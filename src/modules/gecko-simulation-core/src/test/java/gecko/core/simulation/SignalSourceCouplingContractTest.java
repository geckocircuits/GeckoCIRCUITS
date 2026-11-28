package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

/** Desktop SimulationsKern.setControlledSourcesFromControlValue: parameter[1] is the signal-controlled source value. */
class SignalSourceCouplingContractTest {
    private static CircuitNetlist source() {
        var netlist = new CircuitNetlist();
        double[] parameters = new double[21];
        parameters[0] = 400;
        parameters[1] = -0.02; // a stale saved value
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U}, new int[]{1}, new int[]{0},
                new int[]{1}, new double[][]{parameters}, 1, 1, 1);
        netlist.setComponentNameToIndex(Map.of("U.2", 0));
        return netlist;
    }

    @Test
    void theControlOutputBecomesTheSourceValue() {
        var netlist = source();
        var producer = new ConstantCalculator(0);
        var mapping = new ControlBlockMapping();
        mapping.addSourceMapping(producer, 0, "U.2");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);

        coupler.transferControlToSources(netlist);
        assertEquals(0, netlist.getParameter(0)[1]);
        producer._outputSignal[0][0] = 3.5;
        coupler.transferControlToSources(netlist);
        assertEquals(3.5, netlist.getParameter(0)[1]);
    }

    @Test
    void anUnassignedSignalHoldsTheSourceAtZero() {
        var netlist = source();
        var mapping = new ControlBlockMapping();
        mapping.addSourceMapping(null, 0, "U.2");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);

        coupler.transferControlToSources(netlist);
        assertEquals(0, netlist.getParameter(0)[1]);
    }

    private static CircuitModel.ComponentData signalSource(String name, double savedValue, String signal,
            String... terminals) {
        var source = component(4, name, new double[]{400, savedValue}, new String[]{terminals[0]}, terminals[1]);
        source.setParameterStrings(new String[]{signal});
        return source;
    }

    @Test
    void aProbeFedSourceTakesThePreviousStepMeasurementLikeTheDesktop() {
        // SimulationsKern.dataTransferLK_Control sets the sources before it publishes this step's VOLT outputs.
        var model = new CircuitModel();
        model.addCircuitComponent(signalSource("U.2", -0.02, "v", "1", "0"));
        var probe = component(1, "VOLT.1", new double[0], new String[0], "v");
        probe.setParameterStrings(new String[]{"1", "0"});
        model.addControlComponent(probe);
        var netlist = source();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6)) {
            runtime.seedSources(netlist);
            assertEquals(0, netlist.getParameter(0)[1]);
            netlist.storeResults(new double[]{0, 5}, new double[]{0});
            runtime.advanceAfterElectricalStep(netlist, 0);
            assertEquals(0, netlist.getParameter(0)[1], "Step 2 solves with the measurement from before step 1");
            netlist.storeResults(new double[]{0, 7}, new double[]{0});
            runtime.advanceAfterElectricalStep(netlist, 1e-6);
            assertEquals(5, netlist.getParameter(0)[1], "Step 3 solves with the step 1 measurement");
        }
    }

    @Test
    void aSourceInACircuitWithoutControlIsHeldAtZero() {
        // NetListContainer zeroes the saved value; no CONTROL output ever replaces it.
        var model = new CircuitModel();
        model.addCircuitComponent(signalSource("U.2", -0.02, "v", "1", "0"));
        var netlist = source();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6)) {
            runtime.seedSources(netlist);
            assertEquals(0, netlist.getParameter(0)[1]);
        }
    }

    @Test
    void theInitialConditionSolveSeesTheSeededSourceNotTheSavedValue() {
        // SimulationsKern sets the sources before initMatrizen solves the dialog initial conditions.
        var staleSignal = run(signalSource("U.1", 100, "missing", "1", "0"));
        var zeroVolts = run(component(4, "U.1", new double[]{401, 0}, new String[]{"1"}, "0"));
        assertArrayEquals(zeroVolts.getSignalNames(), staleSignal.getSignalNames());
        for (int signal = 0; signal < zeroVolts.getSignalNames().length; signal++) {
            assertArrayEquals(zeroVolts.getSignalData(signal), staleSignal.getSignalData(signal), 1e-6f,
                    zeroVolts.getSignalNames()[signal]);
        }
    }

    private static SimulationResult run(CircuitModel.ComponentData source) {
        var model = new CircuitModel();
        model.setSolverType(SolverType.SOLVER_TRZ);
        model.setTimeStep(1e-6);
        model.setSimulationDuration(5e-6);
        model.addCircuitComponent(source);
        model.addCircuitComponent(component(2, "L.1", new double[]{1e-3, 0}, new String[]{"1"}, "2"));
        model.addCircuitComponent(component(3, "C.1", new double[]{1e-6, 1}, new String[]{"2"}, "0"));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result;
    }
}
