package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.preparation.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Every capability row of ADR-0023: what runs in the core, and what keeps the legacy kernel. */
class ThermalReadinessTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path sources;

    private static ReadinessReport report(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), PreparationRequest.defaults()).report();
    }

    private static Diagnostic find(ReadinessReport report, String code) {
        return report.diagnostics().stream().filter(item -> item.code().equals(code)).findFirst()
                .orElseThrow(() -> new AssertionError(code + " missing in " + report.diagnostics()));
    }

    private static CircuitModel withBlocks(String blocks) throws Exception { return ThermalFixture.model(blocks); }

    @Test
    void aSupportedThermalCircuitIsReadyAndNeverReportsTheBlanketDomainDiagnostic() throws Exception {
        var report = report(ThermalFixture.model());
        assertEquals(ReadinessReport.Outcome.READY, report.outcome(), report.diagnostics().toString());
        assertTrue(report.diagnostics().stream().noneMatch(item -> item.code().equals("UNSUPPORTED_DOMAIN")));
    }

    @Test
    void aPowerModuleAndAnUnknownThermalTypeFallBackToLegacy() throws Exception {
        var report = report(withBlocks(ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.thermal(42, "Modul.1", 5050, "tjS", "0", "1.0", "")));
        var item = find(report, "UNSUPPORTED_THERMAL_COMPONENT");
        assertEquals(Diagnostic.Category.CAPABILITY, item.category());
        assertEquals("Modul.1", item.component());
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, report.outcome());
    }

    @Test
    void aNonlinearThermalCapacitanceFallsBackToLegacy() throws Exception {
        var report = report(withBlocks(ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.thermal(47, "Cth.N", 5060, "tjS", "0", "1.0 0.0 0.0 0.0 0.0 0.0 1.0", "isNonlinear true\n")));
        var item = find(report, "UNSUPPORTED_PARAMETER");
        assertEquals("THERMAL", item.domain());
        assertEquals(Diagnostic.Category.CAPABILITY, item.category());
    }

    @Test
    void lossSourcesWithoutAnExactlyOneReferenceFallBackToLegacy() throws Exception {
        // A reference that matches no electrical component.
        assertEquals(Diagnostic.Category.CAPABILITY, find(report(withBlocks(
                ThermalFixture.thermalBlocks(123456789L, ThermalFixture.DIODE_ID))), "UNSUPPORTED_LOSS_COMPONENT").category());
        // A capacitor has no loss model in the core.
        find(report(withBlocks(ThermalFixture.thermalBlocks(-944661538L, ThermalFixture.DIODE_ID))), "UNSUPPORTED_LOSS_COMPONENT");
        // The switch gets the diode's identifier, so two electrical components share it. Both loss sources point at it.
        // The reference is ambiguous and names never decide, so both sources fall back.
        String text = ThermalFixture.buckText().replace("uniqueObjectIdentifier -1014213050", "uniqueObjectIdentifier -1779394816");
        var twin = new CircuitFileParser().parse(new java.io.BufferedReader(new java.io.StringReader(
                text + "\n" + ThermalFixture.thermalBlocks(ThermalFixture.DIODE_ID, ThermalFixture.DIODE_ID))), "twin.ipes");
        var twinReport = report(twin);
        var reported = twinReport.diagnostics().stream().filter(item -> item.code().equals("UNSUPPORTED_LOSS_COMPONENT"))
                .map(Diagnostic::component).collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Set.of("Pv.1", "Pv.2"), reported, twinReport.diagnostics().toString());
    }

    @Test
    void aDetailedLossModelWithoutItsLossFileFallsBackToLegacy() throws Exception {
        String text = ThermalFixture.buckText().replace("verlustTyp 1", "verlustTyp 2");
        var model = new CircuitFileParser().parse(new java.io.BufferedReader(new java.io.StringReader(
                text + "\n" + ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID))), "detailed.ipes");
        var item = find(report(model), "UNSUPPORTED_LOSS_MODEL");
        assertEquals(Diagnostic.Category.CAPABILITY, item.category());
    }

    @Test
    void probesWithAMissingReferenceAreInvalidAndProbesOnAThermalSourceFallBack() throws Exception {
        String missing = ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.control(16, "TEMP.X", 6100, "", "TjX", "/nolabel/0/0", "coupledReferenceID[] 0 \n");
        var invalid = find(report(withBlocks(missing)), "INVALID_THERMAL_PROBE");
        assertEquals(Diagnostic.Category.REFERENCES, invalid.category());
        assertEquals("TEMP.X", invalid.component());
        String flowOnTemperatureSource = ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.thermal(45, "Tsrc", 5070, "tjD", "0", "401.0 10.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "")
                + ThermalFixture.control(17, "FLOW.T", 6101, "", "PT", "/Tsrc/NIX_NIX_NIX/0", "coupledReferenceID[] 5070 \n");
        var unsupported = find(report(withBlocks(flowOnTemperatureSource)), "UNSUPPORTED_CONTROL_COMPONENT");
        assertEquals(Diagnostic.Category.CAPABILITY, unsupported.category());
        // A FLOW probe on a thermal resistor resolves by the unique identifier of that resistor, so it raises no probe error.
        String repeated = ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.control(17, "FLOW.R", 6102, "", "PR", "/Rth.S/NIX_NIX_NIX/0", "coupledReferenceID[] 5010 \n");
        assertTrue(report(withBlocks(repeated)).diagnostics().stream().noneMatch(item -> item.code().equals("INVALID_THERMAL_PROBE")));
    }

    @Test
    void periodicSteadyStateRejectsTheThermalNetworkWithTheSettingPath() throws Exception {
        var search = new gecko.core.allg.PeriodicSteadyStateSettings(1e-4, 1e-6, 10);
        var preparation = new CircuitPreparation();
        var result = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(
                preparation.capture(ThermalFixture.model()), new PreparationRequest(
                        new SolverOverrides(null, null, null, new SolverFeatures(0, false, false, search, null)), java.util.Map.of())));
        var item = find(result.report(), "INVALID_SETTING");
        assertEquals("periodicSteadyState", item.parameterPath());
        assertTrue(item.message().contains("THERMAL"), item.message());
    }

    @Test
    void anAcDocumentWithThermalElementsIsRejectedWithTheThermalDomain() throws Exception {
        var model = ThermalFixture.model();
        model.setAcSettings(importedAcSettings());
        var item = find(report(model), "UNSUPPORTED_DOMAIN");
        assertEquals("THERMAL", item.domain());
    }

    private gecko.core.ac.AcDocumentSettings importedAcSettings() throws Exception {
        return nativeHierarchy().getAcSettings();
    }

    private CircuitModel nativeHierarchy() throws Exception {
        Path sources = Files.createTempDirectory(this.sources, "hierarchy");
        for (String name : List.of("leaf.asc", "leaf.asy", "wrapper.asc", "wrapper.asy", "root.asc", "res.asy", "cap.asy", "voltage.asy")) {
            try (var input = java.util.Objects.requireNonNull(getClass().getResourceAsStream("/ltspice-hierarchy/" + name))) {
                Files.write(sources.resolve(name), input.readAllBytes(), java.nio.file.StandardOpenOption.CREATE_NEW);
            }
        }
        return new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources));
    }

    @Test
    void aNativeHierarchyKeepsItsTransientGateAndRejectsThermalInAc() throws Exception {
        var model = nativeHierarchy();
        var thermal = new CircuitModel.ComponentData(46, "Rth.root");
        thermal.setTerminalXLabels(new String[]{"a"});
        thermal.setTerminalYLabels(new String[]{"0"});
        thermal.setParameter("param0", 1.0);
        model.addThermalComponent(thermal);
        var preparation = new CircuitPreparation();
        // The transient gate still wins for a hierarchy document.
        var saved = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(nativeHierarchy())), "n.ipes");
        saved.setAcSettings(null);
        var gate = find(preparation.prepare(preparation.capture(saved), PreparationRequest.defaults()).report(),
                "UNSUPPORTED_TRANSIENT_LAW");
        assertEquals(Diagnostic.Category.CAPABILITY, gate.category());
        // AC reports the thermal rejection on the hierarchy branch too.
        assertEquals("THERMAL", find(report(model), "UNSUPPORTED_DOMAIN").domain());
    }

    @Test
    void aThermalBlockWithAHierarchyMemberKeyFailsToParse() throws Exception {
        byte[] compressed = new CircuitFileWriter().write(nativeHierarchy());
        String saved;
        try (var gzip = new java.util.zip.GZIPInputStream(new ByteArrayInputStream(compressed))) {
            saved = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Positive control: the base document parses to a hierarchy with its occurrences.
        var base = new CircuitFileParser().parse(new ByteArrayInputStream(saved.getBytes(StandardCharsets.UTF_8)), "base.ipes");
        assertNotNull(base.getHierarchy());
        assertFalse(base.getHierarchy().definitions().isEmpty());
        String member = gecko.core.io.HierarchyMetadataCodec.MEMBER + " "
                + gecko.core.io.HierarchyMetadataCodec.encode(new gecko.core.io.CircuitHierarchy.Member("k"));
        String block = ThermalFixture.thermal(46, "Rth.m", 7001, "a", "0", "1.0", member + "\n");
        var error = assertThrows(Exception.class, () -> new CircuitFileParser().parse(
                new ByteArrayInputStream((saved + "\n" + block).getBytes(StandardCharsets.UTF_8)), "member.ipes"));
        assertTrue(String.valueOf(error.getMessage()).contains("Invalid circuit hierarchy")
                || error.getCause() != null && String.valueOf(error.getCause().getMessage()).contains("Invalid circuit hierarchy"),
                String.valueOf(error));
    }

    @Test
    void aSavedAndReparsedThermalCircuitKeepsItsComponentsCouplingsAndZeroGroup() throws Exception {
        var original = ThermalFixture.model();
        var copy = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(original)), "copy.ipes");
        assertEquals(original.getThermalComponents().size(), copy.getThermalComponents().size());
        for (int index = 0; index < original.getThermalComponents().size(); index++) {
            var before = original.getThermalComponents().get(index);
            var after = copy.getThermalComponents().get(index);
            assertEquals(before.getName(), after.getName());
            assertEquals(before.getImportTokens().get("coupledReferenceID[]"), after.getImportTokens().get("coupledReferenceID[]"));
        }
        assertEquals(ReadinessReport.Outcome.READY, report(copy).outcome(), report(copy).diagnostics().toString());
        var network = ThermalNetwork.build(copy, gecko.core.circuit.netlist.NetlistBuilder.buildFromCircuitModel(copy),
                gecko.core.allg.SolverType.SOLVER_BE);
        assertNotNull(network);
        assertEquals(gecko.core.circuit.netlist.NetlistBuilder.buildThermalFromCircuitModel(original).getElementCount(),
                network.netlist().getElementCount());
    }
}
