package gecko.core.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParameterOverrideApplicator.
 */
class ParameterOverrideApplicatorTest {

    private CircuitModel model;

    @BeforeEach
    void setUp() {
        model = new CircuitModel();

        CircuitModel.ComponentData r1 = new CircuitModel.ComponentData(1, "R1");
        r1.setParameter("resistance", 10.0);
        r1.setParameter("temperature", 25.0);
        model.addCircuitComponent(r1);

        CircuitModel.ComponentData c1 = new CircuitModel.ComponentData(2, "C1");
        c1.setParameter("capacitance", 1e-6);
        model.addCircuitComponent(c1);

        CircuitModel.ComponentData pi1 = new CircuitModel.ComponentData(3, "PI1");
        pi1.setParameter("gain", 1.0);
        pi1.setParameter("timeConstant", 0.01);
        model.addControlComponent(pi1);

        CircuitModel.ComponentData th1 = new CircuitModel.ComponentData(4, "TH1");
        th1.setParameter("thermalResistance", 5.0);
        model.addThermalComponent(th1);
    }

    @Test
    void applyOverrides_nullModel_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> ParameterOverrideApplicator.applyOverrides(null, Map.of("R1.resistance", 20.0)));
    }

    @Test
    void applyOverrides_nullOverrides_returnsEmptyResult() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, null);
        assertEquals(0, result.appliedCount());
        assertEquals(0, result.failedCount());
        assertTrue(result.allApplied());
    }

    @Test
    void applyOverrides_emptyOverrides_returnsEmptyResult() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of());
        assertEquals(0, result.appliedCount());
        assertEquals(0, result.failedCount());
    }

    @Test
    void applyOverrides_singleCircuitComponent_appliesValue() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("R1.resistance", 100.0));
        assertEquals(1, result.appliedCount());
        assertEquals(0, result.failedCount());
        assertTrue(result.allApplied());
        assertEquals(100.0, (Double) model.getCircuitComponents().get(0).getParameters().get("resistance"), 1e-12);
    }

    @Test
    void applyOverrides_controlComponent_appliesValue() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("PI1.gain", 2.5));
        assertEquals(1, result.appliedCount());
        assertEquals(0, result.failedCount());
        assertEquals(2.5, (Double) model.getControlComponents().get(0).getParameters().get("gain"), 1e-12);
    }

    @Test
    void applyOverrides_thermalComponent_appliesValue() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("TH1.thermalResistance", 3.0));
        assertEquals(1, result.appliedCount());
        assertEquals(0, result.failedCount());
        assertEquals(3.0, (Double) model.getThermalComponents().get(0).getParameters().get("thermalResistance"), 1e-12);
    }

    @Test
    void applyOverrides_multipleComponents_appliesAll() {
        Map<String, Double> overrides = new HashMap<>();
        overrides.put("R1.resistance", 50.0);
        overrides.put("C1.capacitance", 2e-6);
        overrides.put("PI1.gain", 5.0);

        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, overrides);
        assertEquals(3, result.appliedCount());
        assertEquals(0, result.failedCount());
        assertTrue(result.allApplied());
    }

    @Test
    void applyOverrides_unknownComponent_recordsUnmatched() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("UNKNOWN.resistance", 10.0));
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.unmatchedPaths().contains("UNKNOWN.resistance"));
    }

    @Test
    void applyOverrides_unknownParameter_recordsUnmatched() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("R1.unknownParam", 99.0));
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.unmatchedPaths().contains("R1.unknownParam"));
    }

    @Test
    void applyOverrides_noDotInPath_recordsUnmatched() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("resistance", 10.0));
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.unmatchedPaths().contains("resistance"));
    }

    @Test
    void applyOverrides_nullPath_recordsUnmatched() {
        Map<String, Double> overrides = new HashMap<>();
        overrides.put(null, 10.0);
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, overrides);
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
    }

    @Test
    void applyOverrides_emptyComponentName_recordsUnmatched() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of(".resistance", 10.0));
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
    }

    @Test
    void applyOverrides_emptyParameterKey_recordsUnmatched() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("R1.", 10.0));
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
    }

    @Test
    void applyOverrides_doesNotModifyUnchangedComponents() {
        ParameterOverrideApplicator.applyOverrides(model, Map.of("R1.resistance", 50.0));
        // C1 and PI1 should be unchanged
        assertEquals(1e-6, (Double) model.getCircuitComponents().get(1).getParameters().get("capacitance"), 1e-12);
        assertEquals(1.0, (Double) model.getControlComponents().get(0).getParameters().get("gain"), 1e-12);
    }

    @Test
    void applyOverrides_appliedPathsListPopulated() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, Map.of("R1.resistance", 20.0));
        assertTrue(result.appliedPaths().contains("R1.resistance"));
        assertTrue(result.unmatchedPaths().isEmpty());
    }

    @Test
    void applyOverridesForce_nullModel_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> ParameterOverrideApplicator.applyOverridesForce(null, Map.of("R1.resistance", 20.0)));
    }

    @Test
    void applyOverridesForce_newParameterKey_isCreated() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverridesForce(model, Map.of("R1.newParam", 42.0));
        assertEquals(1, result.appliedCount());
        assertEquals(0, result.failedCount());
        assertEquals(42.0, (Double) model.getCircuitComponents().get(0).getParameters().get("newParam"), 1e-12);
    }

    @Test
    void applyOverridesForce_unknownComponent_recordsUnmatched() {
        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverridesForce(model, Map.of("MISSING.param", 1.0));
        assertEquals(0, result.appliedCount());
        assertEquals(1, result.failedCount());
    }

    @Test
    void overrideResult_allApplied_trueWhenNoFailures() {
        ParameterOverrideApplicator.OverrideResult ok =
                new ParameterOverrideApplicator.OverrideResult(3, 0, java.util.List.of(), java.util.List.of());
        assertTrue(ok.allApplied());

        ParameterOverrideApplicator.OverrideResult partial =
                new ParameterOverrideApplicator.OverrideResult(2, 1, java.util.List.of(), java.util.List.of("x"));
        assertFalse(partial.allApplied());
    }

    @Test
    void applyOverrides_mixedSuccessAndFailure_correctCounts() {
        Map<String, Double> overrides = new HashMap<>();
        overrides.put("R1.resistance", 20.0);      // valid
        overrides.put("R1.unknownKey", 1.0);        // bad key
        overrides.put("MISSING.resistance", 1.0);   // bad component

        ParameterOverrideApplicator.OverrideResult result =
                ParameterOverrideApplicator.applyOverrides(model, overrides);
        assertEquals(1, result.appliedCount());
        assertEquals(2, result.failedCount());
    }
}
