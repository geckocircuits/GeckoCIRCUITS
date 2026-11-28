package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import gecko.core.io.ImportIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static gecko.core.preparation.CircuitPreparationTest.component;
import static gecko.core.preparation.CircuitPreparationTest.resistorCircuit;
import static org.junit.jupiter.api.Assertions.*;

class CircuitReadinessTest {
    static PreparationResult prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
    }

    @ParameterizedTest
    @CsvSource({"8,9,0.001", "10,10,1", "7,12,2"})
    void activeSavedFeaturesOutsideTheExecutionPathAreUnsupported(int type, int slot, double value) {
        var model = resistorCircuit();
        double[] parameters = new double[13];
        parameters[0] = 1000;
        parameters[1] = 1;
        parameters[2] = 1;
        parameters[3] = 1000;
        if (type == 7) parameters[2] = 1000;
        parameters[slot] = value;
        model.addCircuitComponent(component(type, "device", parameters, "1", "0"));
        var result = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model));
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_PARAMETER")
                && d.category() == Diagnostic.Category.CAPABILITY && d.component().equals("device")));
    }

    @ParameterizedTest
    @CsvSource({"3,nonlinear", "6,useNonlinearChar"})
    void explicitNonlinearSavedTokensCannotBeSilentlyConvertedToLinearInput(int type, String token) {
        var model = resistorCircuit();
        var device = component(type, "nonlinear", new double[]{1, 1, 1, 1000}, "1", "0");
        device.setParsedInput(true);
        device.setImportedParameterCount(4);
        device.getImportTokens().put(token, "true");
        model.addCircuitComponent(device);
        var result = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model));
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_PARAMETER")
                && d.component().equals("nonlinear")));
    }
    @Test
    void importIssuesRetainBothCategoriesAndExactProvenance() {
        var model = resistorCircuit();
        model.getImportIssues().add(new ImportIssue("MALFORMED_SIMULATION_BLOCK", "CONTROL", "broken",
                "The closing tag is missing.", "fixture.ipes: line 14", false));
        model.getImportIssues().add(new ImportIssue("UNSUPPORTED_SIMULATION_BLOCK", "SPECIAL", "subcircuit",
                "The saved block is unsupported.", "fixture.ipes: line 25", true));
        var result = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model));
        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("MALFORMED_SIMULATION_BLOCK")
                && d.category() == Diagnostic.Category.PARSING && d.sourceLocation().equals("fixture.ipes: line 14")));
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_SIMULATION_BLOCK")
                && d.category() == Diagnostic.Category.CAPABILITY && d.component().equals("subcircuit")));
        assertEquals(result.report(), prepare(model).report());
        assertEquals(2, model.getImportIssues().size());
    }

    @Test
    void anUnknownThermalTypeStaysUnsupportedSoTheLegacyKernelRunsIt() {
        var model = resistorCircuit();
        model.addThermalComponent(component(101, "heat", new double[]{10}, "hot", "0"));
        var result = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model));
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_THERMAL_COMPONENT")
                && d.category() == Diagnostic.Category.CAPABILITY && d.domain().equals("THERMAL")
                && d.component().equals("heat")));
        assertFalse(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_DOMAIN")));
    }

    @Test
    void unsupportedGroundConnectionDoesNotCreateAnInvalidTopologyByOmission() {
        var model = resistorCircuit();
        model.getCircuitComponents().clear();
        model.addCircuitComponent(component(1, "resistor", new double[]{10}, "a", "b"));
        model.addCircuitComponent(component(777, "unknown", new double[]{1}, "b", "0"));
        var result = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model));
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, result.report().outcome());
        assertFalse(result.report().diagnostics().stream().anyMatch(d -> d.category() == Diagnostic.Category.TOPOLOGY));
    }

    @Test
    void independentFloatingIslandRetainsItsErrorBesideUnsupportedGroundedContent() {
        var model = resistorCircuit();
        model.addCircuitComponent(component(1, "floating", new double[]{10}, "x", "y"));
        model.addCircuitComponent(component(777, "unknown", new double[]{1}, "0", "0"));
        var result = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model));
        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNCONNECTED_GROUND")
                && d.component().equals("floating")));
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_COMPONENT")));
    }
}