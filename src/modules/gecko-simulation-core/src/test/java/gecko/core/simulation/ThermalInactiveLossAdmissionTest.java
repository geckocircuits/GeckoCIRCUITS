package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * An active loss source needs an active coupled component. If the target is disabled, or sits on a disabled sheet,
 * the core cannot compute the heat, so the circuit must keep the legacy kernel instead of failing in the session.
 */
class ThermalInactiveLossAdmissionTest {
    private static final char BACKSLASH = 92;
    private static final long TARGET = 77, SHEET = 101;
    private static final String ZERO = "0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
    private static final String DC_SOURCE = "401.0 10.0 50.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.0 1.0 -1.7976931348623157E308 1.7976931348623157E308 0.0";
    private static final Set<String> MASKING = Set.of("UNSUPPORTED_COMPONENT", "MISSING_CONTROL_INPUT", "INVALID_TOPOLOGY",
            "UNSUPPORTED_THERMAL_COMPONENT", "UNSUPPORTED_CONTROL_COMPONENT", "INVALID_THERMAL_PROBE");

    private static String electrical(int type, String name, long id, long sheet, int enabled, String from, String to, String parameters) {
        return "e\n<ElementLK>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier " + sheet + "\nenabledShorted " + enabled
                + "\nparameter[] " + parameters + "\nparameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0\nlabelAnfangsKnoten[] /" + from
                + "\nlabelEndKnoten[] /" + to + "\n<" + BACKSLASH + "ElementLK>\n";
    }

    private static String thermal(int type, String name, long id, String from, String to, String parameters, String extra) {
        return "eTH\n<ElementTHERM>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\nparameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0\nlabelAnfangsKnoten[] /" + from
                + "\nlabelEndKnoten[] /" + to + "\n" + extra + "<" + BACKSLASH + "ElementTHERM>\n";
    }

    private static String sheet(long id, int enabled) {
        return "sp\n<ElementSPECIAL>\ntyp 27\nidStringDialog sub" + id + "\nuniqueObjectIdentifier " + id
                + "\nparentSheetIdentifier 0\nenabledShorted " + enabled + "\n<" + BACKSLASH + "ElementSPECIAL>\n";
    }

    /**
     * Source U and a second resistor stay active, so the electrical circuit is valid whatever happens to the target.
     * The loss source is active and couples to the target resistor by its exact saved identifier.
     */
    private static CircuitModel circuit(long targetSheet, int targetEnabled, boolean withSheet, int sheetEnabled) throws Exception {
        String text = (withSheet ? sheet(SHEET, sheetEnabled) : "")
                + electrical(4, "U.1", 10, 0, 1, "in", "0", DC_SOURCE)
                + electrical(1, "R.other", 78, 0, 1, "in", "0", "20.0 " + ZERO)
                + electrical(1, "R.target", TARGET, targetSheet, targetEnabled, "in", "0", "5.0 " + ZERO)
                + thermal(41, "Pv.1", 20, "", "tj", "", "coupledReferenceID[] " + TARGET + " \n")
                + thermal(46, "Rth.1", 21, "tj", "0", "2.0 " + ZERO, "")
                + thermal(47, "Cth.1", 22, "tj", "0", "0.5 0.0 0.0 0.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0", "")
                + thermal(48, "TREF.1", 23, "0", "0", "0.0 " + ZERO, "");
        return new CircuitFileParser().parse(new BufferedReader(new StringReader("dt 0.001\ntDURATION 0.01\n" + text)), "inactive-loss.ipes");
    }

    private static PreparationResult prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
    }

    private static SimulationSession open(CircuitModel model) {
        return SimulationSession.fromModel(model, new SolverSettingsCore(SolverType.SOLVER_BE, 1e-3, 1e-2), ExecutionOptions.defaults());
    }

    /** The only thing that may keep the circuit off the core is the loss coupling itself. */
    private static void assertOnlyTheLossCouplingBlocks(ReadinessReport report) {
        var others = report.diagnostics().stream().filter(item -> MASKING.contains(item.code())).toList();
        assertTrue(others.isEmpty(), "another diagnostic would mask the admission result: " + others);
    }

    private static void assertFallsBackForTheLossSource(PreparationResult result) {
        var report = result.report();
        assertOnlyTheLossCouplingBlocks(report);
        assertNotEquals(ReadinessReport.Outcome.READY, report.outcome(), "READY would open a session that throws: " + report.diagnostics());
        var item = report.diagnostics().stream().filter(d -> d.code().equals("UNSUPPORTED_LOSS_COMPONENT")).findFirst()
                .orElseThrow(() -> new AssertionError(report.diagnostics().toString()));
        assertEquals(Diagnostic.Category.CAPABILITY, item.category());
        assertEquals("THERMAL", item.domain());
        assertEquals("Pv.1", item.component());
        assertFalse(result instanceof PreparationResult.Ready);
    }

    @Test
    void theActiveEquivalentIsReadyAndOpensASession() throws Exception {
        // Positive control first: with every part active, the same fixture must run.
        var model = circuit(0, 1, false, 1);
        var result = prepare(model);
        assertInstanceOf(PreparationResult.Ready.class, result, result.report().diagnostics().toString());
        assertEquals(ReadinessReport.Outcome.READY, result.report().outcome());
        try (var session = open(model)) {
            assertEquals(0, session.status().committedSteps(), "the session constructs and has not stepped");
            session.step();
            assertEquals(1, session.thermalSolves(), "the thermal network runs with the active target");
        }
        var activeSheet = circuit(SHEET, 1, true, 1);
        assertInstanceOf(PreparationResult.Ready.class, prepare(activeSheet), prepare(activeSheet).report().diagnostics().toString());
    }

    @Test
    void aDisabledCoupledComponentKeepsTheLegacyKernel() throws Exception {
        assertFallsBackForTheLossSource(prepare(circuit(0, 0, false, 1)));
    }

    @Test
    void aCoupledComponentOnADisabledSheetKeepsTheLegacyKernel() throws Exception {
        assertFallsBackForTheLossSource(prepare(circuit(SHEET, 1, true, 0)));
    }

    @Test
    void theRawSessionStillRefusesAnInactiveCoupledComponentWithASpecificMessage() throws Exception {
        // This is the failure that readiness must keep a caller from reaching.
        for (var model : new CircuitModel[]{circuit(0, 0, false, 1), circuit(SHEET, 1, true, 0)}) {
            var error = assertThrows(IllegalArgumentException.class, () -> open(model));
            assertTrue(error.getMessage().contains("Pv.1") && error.getMessage().contains("not part of the electrical circuit"),
                    error.getMessage());
        }
    }
}
