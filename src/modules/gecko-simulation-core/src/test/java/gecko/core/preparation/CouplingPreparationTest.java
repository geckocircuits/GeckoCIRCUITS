package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class CouplingPreparationTest {
    static CircuitModel transformer() {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        model.addCircuitComponent(CircuitPreparationTest.component(4, "source", new double[]{401, 10}, "p", "0"));
        var primary = CircuitPreparationTest.component(12, "primary", new double[]{4, 0}, "p", "0");
        primary.setParameter("uniqueObjectIdentifier", 9007199254740993L);
        model.addCircuitComponent(primary);
        var secondary = CircuitPreparationTest.component(12, "secondary", new double[]{1, 0}, "s", "0");
        secondary.setParameter("uniqueObjectIdentifier", 9007199254740995L);
        model.addCircuitComponent(secondary);
        model.addCircuitComponent(CircuitPreparationTest.component(1, "load", new double[]{100}, "s", "0"));
        var coupling = CircuitPreparationTest.component(9, "coupling", new double[]{1}, "", "");
        coupling.setParameterStrings(new String[]{"stale-primary-name", "stale-secondary-name"});
        coupling.setParameter("coupledReferenceID0", 9007199254740993L);
        coupling.setParameter("coupledReferenceID1", 9007199254740995L);
        model.addCircuitComponent(coupling);
        return model;
    }

    static List<Double> secondaryVoltages(PreparedCircuit circuit) {
        List<Double> values = new ArrayList<>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) ->
                values.add(volts[netlist.getNodeX(netlist.getElementIndexByName("load"))]));
        var result = engine.runSimulation(circuit, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return values;
    }

    static void assertVoltages(PreparedCircuit circuit, double... expected) {
        var actual = secondaryVoltages(circuit);
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) assertEquals(expected[index], actual.get(index), 1e-8);
    }

    @Test
    void signedCouplingAndWindingOverridesPreserveTheResolvedLongReferences() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(transformer());
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var reversed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("coupling.couplingCoefficient", -1.0))));
        var winding = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(reversed.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("secondary.inductance", 4.0))));
        assertVoltages(original.execution(), 5, 5, 5);
        assertVoltages(reversed.execution(), -5, -5, -5);
        assertVoltages(winding.execution(), -10, -10, -10);
        assertVoltages(original.execution(), 5, 5, 5);
        var effective = winding.circuit().toModel();
        assertEquals(9007199254740993L, effective.getCircuitComponents().get(1).getParameters().get("uniqueObjectIdentifier"));
        assertEquals(9007199254740995L, effective.getCircuitComponents().get(2).getParameters().get("uniqueObjectIdentifier"));
        var coupling = effective.getCircuitComponents().get(4);
        assertEquals(9007199254740993L, coupling.getParameters().get("coupledReferenceID0"));
        assertEquals(9007199254740995L, coupling.getParameters().get("coupledReferenceID1"));
        assertArrayEquals(new String[]{"stale-primary-name", "stale-secondary-name"}, coupling.getParameterStrings());
    }

    @ParameterizedTest
    @CsvSource({"coupling.k,-1,-5", "coupling.param0,0,0", "coupling.couplingCoefficient,1,5",
            "primary.param0,1,10", "secondary.param0,4,10"})
    void namedAndIndexedWindingConfigurationChangesTheSecondaryVoltage(String path, double value, double voltage) {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(transformer());
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of(path, value))));
        assertVoltages(ready.execution(), voltage, voltage, voltage);
        assertVoltages(assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults())).execution(), 5, 5, 5);
    }

    @ParameterizedTest
    @CsvSource({"coupling.k,-1.01", "coupling.param0,1.01", "primary.inductance,0", "secondary.param0,-1"})
    void invalidCouplingOrWindingValuesRejectEveryAssignment(String path, double value) {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(transformer());
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of(path, value, "load.resistance", 200.0))));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item ->
                item.code().equals("INVALID_PARAMETER") && path.equals(item.parameterPath())), rejected.report().toString());
        assertEquals(100.0, snapshot.toModel().getCircuitComponents().get(3).getNumericParameters()[0]);
        assertVoltages(assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults())).execution(), 5, 5, 5);
    }

    @ParameterizedTest
    @CsvSource({"primary.uniqueObjectIdentifier", "secondary.uniqueObjectIdentifier",
            "coupling.coupledReferenceID0", "coupling.coupledReferenceID1", "primary.param2", "secondary.param4"})
    void windingIdentityAssociationsAndRuntimeStateAreReadOnly(String path) {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(transformer());
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of(path, 1.0))));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item ->
                item.code().equals("READ_ONLY_PARAMETER") && path.equals(item.parameterPath())), rejected.report().toString());
        assertVoltages(assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults())).execution(), 5, 5, 5);
    }
}
