package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

/** The DC operating point through the public prepared-circuit session, per solver. */
class DcOperatingPointTest {
    private static final SolverFeatures DC = new SolverFeatures(0, false, true);

    /** Public node voltages and element currents of every committed step. */
    record Trace(List<double[]> volts, List<double[]> currents) { }

    private static CircuitModel model(String... signals) {
        var model = new CircuitModel();
        model.setTimeStep(0.01);
        model.setSimulationDuration(0.1);
        model.setDataContainerSignals(signals);
        return model;
    }

    private static CircuitModel.ComponentData source(String name, double volts, String plus, String minus) {
        return component(4, name, new double[]{401, volts}, new String[]{plus}, minus);
    }

    private static CircuitModel.ComponentData resistor(String name, double ohms, String x, String y) {
        return component(1, name, new double[]{ohms}, new String[]{x}, y);
    }

    private static CircuitModel.ComponentData capacitor(String name, double farads, double initial, String x, String y) {
        return component(3, name, new double[]{farads, initial}, new String[]{x}, y);
    }

    private static CircuitModel.ComponentData inductor(String name, double henries, double initial, String x, String y) {
        return component(2, name, new double[]{henries, initial}, new String[]{x}, y);
    }

    private static PreparedCircuit prepare(CircuitModel model, SolverType solver, SolverFeatures features) {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(solver, null, null, features), Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result).execution();
    }

    private static Trace run(CircuitModel model, SolverType solver, SolverFeatures features, int steps) {
        var volts = new ArrayList<double[]>();
        var currents = new ArrayList<double[]>();
        try (var session = SimulationSession.open(prepare(model, solver, features), ExecutionOptions.defaults())) {
            for (int step = 0; step < steps; step++) {
                session.step((time, v, i, circuit) -> { volts.add(v.clone()); currents.add(i.clone()); });
            }
        }
        return new Trace(volts, currents);
    }

    private static CircuitModel sourceResistorCapacitor() {
        var model = model("ground", "source", "capacitor");
        model.addCircuitComponent(source("V", 10, "1", "0"));
        model.addCircuitComponent(resistor("R", 1, "1", "2"));
        model.addCircuitComponent(capacitor("C", 1, 0, "2", "0"));
        return model;
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aChargedCapacitorStaysFlatFromTheFirstStep(SolverType solver) {
        var trace = run(sourceResistorCapacitor(), solver, DC, 10);
        for (int step = 0; step < 10; step++) {
            assertEquals(10, trace.volts().get(step)[2], 1e-2, solver + " step " + step);
            assertEquals(0, trace.currents().get(step)[2], 1e-9, "capacitor current, step " + step);
            assertEquals(0, trace.currents().get(step)[1], 1e-9, "resistor current, step " + step);
        }
        var legacy = run(sourceResistorCapacitor(), solver, SolverFeatures.none(), 1);
        assertTrue(legacy.volts().get(0)[2] < 0.5, "without the flag the capacitor starts empty");
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void anInductorStartsAtVoltageOverResistanceAndStaysThere(SolverType solver) {
        var model = model("ground", "source", "junction");
        model.addCircuitComponent(source("V", 10, "1", "0"));
        model.addCircuitComponent(inductor("L", 0.5, 0, "1", "2"));
        model.addCircuitComponent(resistor("R", 2, "2", "0"));
        var trace = run(model, solver, DC, 10);
        for (int step = 0; step < 10; step++) {
            assertEquals(5, trace.currents().get(step)[1], 1e-9, solver + " inductor, step " + step);
            assertEquals(10, trace.volts().get(step)[2], 1e-9, "junction, step " + step);
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aShortedBranchOfADividerCarriesTheWholeCurrent(SolverType solver) {
        var model = model("ground", "source", "junction");
        model.addCircuitComponent(source("V", 12, "1", "0"));
        model.addCircuitComponent(resistor("R1", 4, "1", "2"));
        model.addCircuitComponent(resistor("R2", 4, "2", "0"));
        model.addCircuitComponent(inductor("L", 1e-3, 0, "2", "0"));
        var trace = run(model, solver, DC, 5);
        for (int step = 0; step < 5; step++) {
            assertEquals(3, trace.currents().get(step)[3], 1e-9, "L = V/R1, step " + step);
            assertEquals(0, trace.currents().get(step)[2], 1e-9, "the parallel resistor is bypassed, step " + step);
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void reversedElementsKeepTheirTerminalOrientation(SolverType solver) {
        var model = model("ground", "source", "junction");
        model.addCircuitComponent(source("V", 10, "1", "0"));
        model.addCircuitComponent(resistor("R", 2, "1", "2"));
        model.addCircuitComponent(inductor("L", 0.5, 0, "0", "2"));
        model.addCircuitComponent(capacitor("C", 1, 0, "0", "1"));
        var trace = run(model, solver, DC, 5);
        for (int step = 0; step < 5; step++) {
            assertEquals(-5, trace.currents().get(step)[2], 1e-9, "current runs from X=0 to Y=2, step " + step);
            assertEquals(0, trace.currents().get(step)[3], 1e-9);
            assertEquals(10, trace.volts().get(step)[1], 1e-9);
        }
    }

    /** Sources and reactive elements interleave and use unordered node labels. */
    private static CircuitModel interleaved() {
        var model = model("ground", "a", "b", "c", "d", "e");
        model.addCircuitComponent(source("V2", 6, "3", "0"));
        model.addCircuitComponent(resistor("R1", 2, "7", "5"));
        model.addCircuitComponent(inductor("L1", 1e-3, 3.0, "5", "3"));
        model.addCircuitComponent(capacitor("C1", 1e-3, 1.0, "5", "0"));
        model.addCircuitComponent(source("V1", 12, "7", "0"));
        model.addCircuitComponent(inductor("L2", 2e-3, 0.5, "7", "8"));
        model.addCircuitComponent(resistor("R3", 3, "8", "0"));
        model.addCircuitComponent(capacitor("C2", 2e-3, 2.0, "8", "0"));
        return model;
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void severalSourcesAndReactivesMapBackToTheirOriginalElements(SolverType solver) {
        var trace = run(interleaved(), solver, DC, 10);
        double[] first = trace.currents().get(0);
        assertEquals(3, first[1], 1e-9, "R1");
        assertEquals(3, first[2], 1e-9, "L1");
        assertEquals(0, first[3], 1e-9, "C1");
        assertEquals(4, first[5], 1e-9, "L2");
        assertEquals(4, first[6], 1e-9, "R3");
        assertEquals(0, first[7], 1e-9, "C2");
        for (int step = 1; step < 10; step++) {
            assertArrayEquals(first, trace.currents().get(step), 1e-9, solver + " currents, step " + step);
            assertArrayEquals(trace.volts().get(0), trace.volts().get(step), 1e-9, "voltages, step " + step);
        }
        // Capacitor voltages are the node voltages: C1 across node 5 (= 6 V) and C2 across node 8 (= 12 V).
        double[] volts = trace.volts().get(0);
        assertTrue(java.util.Arrays.stream(volts).anyMatch(v -> Math.abs(v - 6) < 1e-9));
        assertTrue(java.util.Arrays.stream(volts).anyMatch(v -> Math.abs(v - 12) < 1e-9));
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void theDirectFirstStepHistoriesAreTheDcOperatingPoint(SolverType solver) {
        // A wrong capacitor current or inductor history would show in the first step, not only later.
        var trace = run(interleaved(), solver, DC, 2);
        assertArrayEquals(trace.currents().get(0), trace.currents().get(1), 1e-12);
    }

    @Test
    void configuredInitialStatesAreIgnoredByTheSolveAndSurviveInTheModel() {
        var model = interleaved();
        var before = model.getCircuitComponents().stream().map(item -> item.getNumericParameters().clone()).toList();
        run(model, SolverType.SOLVER_TRZ, DC, 3);
        for (int index = 0; index < before.size(); index++) {
            assertArrayEquals(before.get(index), model.getCircuitComponents().get(index).getNumericParameters());
        }
    }

    @Test
    void independentRunsFromOnePreparedCircuitAgree() {
        var prepared = prepare(interleaved(), SolverType.SOLVER_BE, DC);
        List<double[]> first = new ArrayList<>(), second = new ArrayList<>();
        for (var target : List.of(first, second)) {
            try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
                session.step((time, v, i, circuit) -> target.add(i.clone()));
                session.step((time, v, i, circuit) -> target.add(i.clone()));
            }
        }
        for (int step = 0; step < 2; step++) assertArrayEquals(first.get(step), second.get(step));
    }

    @Test
    void singularAndUnsupportedCircuitsFailWithoutAFakeState() {
        var parallel = model("ground", "a");
        parallel.addCircuitComponent(source("V", 5, "1", "0"));
        parallel.addCircuitComponent(inductor("L", 1e-3, 0, "1", "0"));
        var failure = assertThrows(IllegalStateException.class, () -> run(parallel, SolverType.SOLVER_BE, DC, 1));
        assertTrue(failure.getMessage().contains("no unique solution"), failure.getMessage());

        var loop = model("ground", "a", "b");
        loop.addCircuitComponent(source("V", 5, "1", "0"));
        loop.addCircuitComponent(inductor("L1", 1e-3, 0, "1", "2"));
        loop.addCircuitComponent(inductor("L2", 1e-3, 0, "1", "2"));
        loop.addCircuitComponent(resistor("R", 1, "2", "0"));
        assertThrows(IllegalStateException.class, () -> run(loop, SolverType.SOLVER_BE, DC, 1));

        var divider = model("ground", "a", "b");
        divider.addCircuitComponent(source("V", 5, "1", "0"));
        divider.addCircuitComponent(capacitor("C1", 1e-3, 0, "1", "2"));
        divider.addCircuitComponent(capacitor("C2", 1e-3, 0, "2", "0"));
        failure = assertThrows(IllegalStateException.class, () -> run(divider, SolverType.SOLVER_BE, DC, 1));
        assertTrue(failure.getMessage().contains("undetermined"), failure.getMessage());
        // The same circuits still run their transient without the flag.
        assertDoesNotThrow(() -> run(parallel, SolverType.SOLVER_BE, SolverFeatures.none(), 1));
    }

    /** Source into a switch and a load, with a CONTROL constant that drives the switch gate. */
    private static CircuitModel gatedSwitch(double gateLevel) {
        var model = model("ground", "source", "load");
        model.addCircuitComponent(source("V", 10, "1", "0"));
        model.addCircuitComponent(component(7, "S.1", new double[]{0.01, 0.01, 1e9}, new String[]{"1"}, "2"));
        model.addCircuitComponent(resistor("R", 10, "2", "0"));
        model.addCircuitComponent(capacitor("C", 1e-3, 0, "2", "0"));
        model.addControlComponent(component(3, "level", new double[]{gateLevel}, new String[0], "g"));
        var gate = component(6, "GATE.1", new double[0], new String[]{"g"});
        gate.setParameterStrings(new String[]{"S.1"});
        model.addControlComponent(gate);
        return model;
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void theDcPointFreezesTheInitialControlGateBeforeTheFirstStep(SolverType solver) {
        for (double level : new double[]{0, 1}) {
            var trace = run(gatedSwitch(level), solver, DC, 3);
            double expected = level == 0 ? 0 : 100 / 10.01;
            for (int step = 0; step < 3; step++) {
                assertEquals(expected, trace.volts().get(step)[2], 1e-5, solver + " gate " + level + " step " + step);
            }
        }
    }

    @Test
    void settingsReachPreparedCircuitsConfigsAndCopiesIndependently() {
        assertFalse(SimulationConfig.builder().build().getSolverSettings().isDcOperatingPoint(), "off by default");
        assertFalse(SimulationConfig.builder().build().explicitSolverOverrides().features().dcOperatingPoint());
        var config = SimulationConfig.builder().dcOperatingPoint(true).build();
        assertTrue(config.getSolverSettings().isDcOperatingPoint());
        assertTrue(config.explicitSolverOverrides().features().dcOperatingPoint());
        var copy = config.getSolverSettings();
        copy.withDcOperatingPoint(false);
        assertTrue(config.getSolverSettings().isDcOperatingPoint(), "a returned copy cannot change the config");
        assertTrue(config.getSolverSettings().copy().isDcOperatingPoint());
        assertFalse(SolverFeatures.none().dcOperatingPoint());
        assertEquals(new SolverFeatures(1e-5, true, false), new SolverFeatures(1e-5, true));
        assertEquals(new SolverFeatures(1e-5, false, false), new SolverFeatures(1e-5));

        var prepared = prepare(sourceResistorCapacitor(), SolverType.SOLVER_BE, DC);
        assertTrue(prepared.solverSettings().isDcOperatingPoint());
        prepared.solverSettings().withDcOperatingPoint(false);
        assertTrue(prepared.solverSettings().isDcOperatingPoint(), "prepared settings are detached");
        assertFalse(prepare(sourceResistorCapacitor(), SolverType.SOLVER_BE, SolverFeatures.none())
                .solverSettings().isDcOperatingPoint());
    }

    private static double[] device(double current, double gate) {
        double[] values = new double[13];
        values[0] = current;
        values[1] = 0.7;
        values[2] = 0.01;
        values[3] = 1e9;
        values[8] = gate;
        return values;
    }

    /** 5 V, 10 ohm, then a thyristor in parallel with a 1 mF capacitor. */
    private static CircuitNetlist latchedThyristor(double storedResistance, double gate, double volts) {
        double[] source = new double[21], load = {10, 0, 0}, capacitor = new double[13];
        source[0] = gecko.core.circuit.SourceType.QUELLE_DC_NEW;
        source[1] = volts;
        capacitor[0] = 1e-3;
        capacitor[6] = capacitor[7] = 1e-3;
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_THYR,
                        CircuitTypCore.LK_C},
                new int[]{1, 1, 2, 2}, new int[]{0, 2, 0, 0}, new int[]{1, -1, -1, -1},
                new double[][]{source, load, device(storedResistance, gate), capacitor}, 2, 1, 4);
        return netlist;
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aLatchedThyristorStartsTheFirstStepAtItsDcPointWithoutATransient(SolverType solver) {
        double latched = 0.7 + 0.01 * (5 - 0.7) / 10.01;
        var netlist = latchedThyristor(0.01, 0, 5);
        var configured = netlist.getParameter(2).clone();
        var step = new ElectricalTimeStep(netlist, solver, DC);
        assertArrayEquals(configured, netlist.getParameter(2), "the caller's state survives construction");
        for (int index = 0; index < 3; index++) {
            step.advance(1e-6, index * 1e-6, null);
            assertEquals(latched, netlist.getLastNodeVoltages()[2], 1e-6, solver + " step " + index);
            assertEquals(0, netlist.getLastComponentCurrents()[3], 1e-6, "capacitor current, step " + index);
            assertEquals(0.01, netlist.getParameter(2)[0], "still latched");
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aThyristorStoredOffStaysOffWithoutAGateAndConductsWithOne(SolverType solver) {
        var closed = latchedThyristor(1e9, 0, 5);
        var blocking = new ElectricalTimeStep(closed, solver, DC);
        blocking.advance(1e-6, 0, null);
        assertEquals(5, closed.getLastNodeVoltages()[2], 1e-4, "blocking, no gate");
        assertEquals(0, closed.getLastComponentCurrents()[3], 1e-6);
        var gated = latchedThyristor(1e9, 1, 5);
        var conducting = new ElectricalTimeStep(gated, solver, DC);
        conducting.advance(1e-6, 0, null);
        assertEquals(0.7 + 0.01 * (5 - 0.7) / 10.01, gated.getLastNodeVoltages()[2], 1e-6, "gated, forward bias");
        assertEquals(0, gated.getLastComponentCurrents()[3], 1e-6);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aReverseBiasUnlatchesAStoredConductingThyristor(SolverType solver) {
        var netlist = latchedThyristor(0.01, 0, -5);
        var step = new ElectricalTimeStep(netlist, solver, DC);
        step.advance(1e-6, 0, null);
        assertEquals(-5, netlist.getLastNodeVoltages()[2], 1e-4);
        assertEquals(0, netlist.getLastComponentCurrents()[3], 1e-6);
    }

    /** Capacitor voltage and switch node of the first engine step, read from the trace. */
    private static double[] firstBuckStep(SimulationConfig config) {
        double[] first = new double[2];
        boolean[] seen = {false};
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, circuit) -> {
            if (seen[0]) return;
            seen[0] = true;
            first[0] = volts[circuit.getNodeX(4)] - volts[circuit.getNodeY(4)];
            first[1] = volts[circuit.getNodeX(2)] - volts[circuit.getNodeY(2)];
        });
        var result = engine.runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return first;
    }

    @Test
    void theEnginePathStartsTheBuckOutputCapacitorAtItsFrozenGateDcPoint() throws Exception {
        var file = java.nio.file.Path.of(DcOperatingPointTest.class.getResource("/ipes/buck_simple.ipes").toURI()).toString();
        // buck_simple: U.1 = 48 V, S.1 Ron 10 mohm, L.1 short at DC, C.1 open, R.1 = 10 ohm, D.1 blocks.
        double vin = 48, closed = vin * 10 / (10 + 0.01);
        // The frozen gate is the one the transient's first step uses: a closed switch puts the inductor's
        // switch-side node near Vin, an open one lets the diode pull it to ground.
        double[] undisturbed = firstBuckStep(SimulationConfig.builder().circuitFile(file).build());
        double expected = undisturbed[1] > vin / 2 ? closed : 0;
        double[] dc = firstBuckStep(SimulationConfig.builder().circuitFile(file).dcOperatingPoint(true).build());
        assertEquals(expected, dc[0], 1e-3 * vin, "first output-capacitor voltage");
        assertEquals(expected, dc[1], 1e-3 * vin, "the inductor node sits at the same DC level");
    }

    @Test
    void invalidSettingsAreNotAffectedByTheFlag() {
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 1e-4).withDcOperatingPoint(true);
        assertTrue(SimulationTiming.settingsIssues(settings).isEmpty());
    }
}
