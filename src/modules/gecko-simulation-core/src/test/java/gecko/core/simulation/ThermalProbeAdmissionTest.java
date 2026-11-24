package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TEMP and FLOW need an active thermal network. Without one the circuit keeps the legacy kernel. */
class ThermalProbeAdmissionTest {
    private static final String PROBES = ThermalFixture.control(16, "TEMP.1", 6001, "", "TjS", "/tjS/0/0", "coupledReferenceID[] 0 \n")
            + ThermalFixture.control(17, "FLOW.1", 6002, "", "PS", "/Pv.1/NIX_NIX_NIX/0", "coupledReferenceID[] 5001 \n")
            + ThermalFixture.control(5, "SCOPE.T", 6004, "/TjS/PS", "", "/NIX_NIX_NIX", "");

    private static ReadinessReport report(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), PreparationRequest.defaults()).report();
    }

    private static void assertFallsBackForTheProbes(ReadinessReport report) {
        assertNotEquals(ReadinessReport.Outcome.READY, report.outcome(), report.diagnostics().toString());
        var probes = report.diagnostics().stream().filter(item -> item.code().equals("UNSUPPORTED_CONTROL_COMPONENT")).toList();
        // The build stops at the first probe it cannot resolve.
        assertEquals(1, probes.size(), report.diagnostics().toString());
        probes.forEach(item -> assertEquals(Diagnostic.Category.CAPABILITY, item.category()));
        assertEquals("TEMP.1", probes.get(0).component());
    }

    @Test
    void probesWithoutAnyThermalElementKeepTheLegacyKernel() throws Exception {
        assertFallsBackForTheProbes(report(ThermalFixture.model(PROBES)));
    }

    @Test
    void probesWithOnlyThermalTerminalsKeepTheLegacyKernel() throws Exception {
        var terminal = ThermalFixture.thermal(49, "TERM.1", 5100, "a", "a", "", "");
        assertFallsBackForTheProbes(report(ThermalFixture.model(terminal + PROBES)));
    }

    /** FLOW alone, so that a TEMP failure cannot mask the FLOW branch. */
    private static final String FLOW_ONLY = ThermalFixture.control(17, "FLOW.1", 6002, "", "PS", "/Pv.1/NIX_NIX_NIX/0", "coupledReferenceID[] 5001 \n")
            + ThermalFixture.control(5, "SCOPE.T", 6004, "/PS", "", "/NIX_NIX_NIX", "");

    private static void assertFlowFallsBack(ReadinessReport report) {
        assertNotEquals(ReadinessReport.Outcome.READY, report.outcome(), report.diagnostics().toString());
        var item = report.diagnostics().stream().filter(d -> d.code().equals("UNSUPPORTED_CONTROL_COMPONENT")).findFirst()
                .orElseThrow(() -> new AssertionError(report.diagnostics().toString()));
        assertEquals("FLOW.1", item.component());
        assertEquals(Diagnostic.Category.CAPABILITY, item.category());
        assertTrue(report.diagnostics().stream().noneMatch(d -> d.code().equals("INVALID_THERMAL_PROBE")));
    }

    @Test
    void aFlowProbeWithoutAnyThermalElementKeepsTheLegacyKernel() throws Exception {
        assertFlowFallsBack(report(ThermalFixture.model(FLOW_ONLY)));
    }

    @Test
    void aFlowProbeWithOnlyThermalTerminalsKeepsTheLegacyKernel() throws Exception {
        var terminal = ThermalFixture.thermal(49, "TERM.1", 5100, "a", "a", "", "");
        assertFlowFallsBack(report(ThermalFixture.model(terminal + FLOW_ONLY)));
    }

    @Test
    void probesWithoutAThermalNetworkNeverReachASessionThatThrowsOnThem() throws Exception {
        // The session cannot build these probes. Readiness must have refused them first.
        var model = ThermalFixture.model(PROBES);
        assertThrows(ControlCalculatorFactory.ThermalProbeException.class, () -> SimulationSession.fromModel(model,
                new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 1e-4), ExecutionOptions.defaults()));
        assertNotEquals(ReadinessReport.Outcome.READY, report(model).outcome());
    }

    @Test
    void aBlockedThermalNetworkReportsItsBlockerOnceWithoutADuplicateProbeDiagnostic() throws Exception {
        var module = ThermalFixture.thermal(42, "Modul.1", 5050, "tjS", "0", "1.0", "");
        var report = report(ThermalFixture.model(module + PROBES));
        assertEquals(1, report.diagnostics().stream().filter(item -> item.code().equals("UNSUPPORTED_THERMAL_COMPONENT")).count());
        assertTrue(report.diagnostics().stream().noneMatch(item -> item.code().equals("UNSUPPORTED_CONTROL_COMPONENT")
                || item.code().equals("INVALID_THERMAL_PROBE")), report.diagnostics().toString());
    }

    @Test
    void aValidNetworkStillReportsAMissingProbeReference() throws Exception {
        var model = ThermalFixture.model(ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                + ThermalFixture.control(16, "TEMP.X", 6100, "", "TjX", "/nolabel/0/0", "coupledReferenceID[] 0 \n"));
        var item = report(model).diagnostics().stream().filter(d -> d.code().equals("INVALID_THERMAL_PROBE")).findFirst().orElseThrow();
        assertEquals(Diagnostic.Category.REFERENCES, item.category());
    }

    private static String disabled(String blocks) { return blocks.replace("enabledShorted 1", "enabledShorted 0"); }

    private static ReadinessReport withSearch(CircuitModel model) {
        var search = new PeriodicSteadyStateSettings(1e-4, 1e-6, 10);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(
                new SolverOverrides(null, null, null, new SolverFeatures(0, false, false, search, null)), Map.of()));
        return result.report();
    }

    @Test
    void aDisabledThermalNetworkDoesNotRejectPeriodicSteadyStateButAnActiveOneDoes() throws Exception {
        var inactive = ThermalFixture.model(disabled(ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                .replaceAll("(?s)c\\n<ElementCONTROL>.*", "")));
        assertNull(ThermalNetwork.build(inactive, gecko.core.circuit.netlist.NetlistBuilder.buildFromCircuitModel(inactive), SolverType.SOLVER_BE),
                "a disabled network builds nothing");
        assertTrue(withSearch(inactive).diagnostics().stream().noneMatch(item -> "periodicSteadyState".equals(item.parameterPath())
                && item.message().contains("THERMAL")), withSearch(inactive).diagnostics().toString());
        assertTrue(withSearch(ThermalFixture.model()).diagnostics().stream().anyMatch(item -> "periodicSteadyState".equals(item.parameterPath())
                && item.message().contains("THERMAL")));
    }

    @Test
    void theTimingCheckAgreesWithTheRuntimeAboutWhichThermalElementsAreActive() throws Exception {
        var inactive = ThermalFixture.model(disabled(ThermalFixture.thermalBlocks(ThermalFixture.SWITCH_ID, ThermalFixture.DIODE_ID)
                .replaceAll("(?s)c\\n<ElementCONTROL>.*", "")));
        var search = new PeriodicSteadyStateSettings(1e-4, 1e-6, 10);
        assertTrue(SimulationTiming.periodicSteadyStateCircuitIssues(inactive, 1e-6, 0, search).stream()
                .noneMatch(issue -> issue.message().contains("THERMAL")));
        assertTrue(SimulationTiming.periodicSteadyStateCircuitIssues(ThermalFixture.model(), 1e-6, 0, search).stream()
                .anyMatch(issue -> issue.message().contains("THERMAL")));
    }
}
