package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlSparseMatrixExecutionTest {
    private static final double PERIOD = 1.0 / 25000;
    private static final double DT = PERIOD / 20;
    private static final String[] GATES = {"s1+", "s2+", "s3+", "s1-", "s2-", "s3-", "sA", "sB", "sC"};

    @Test
    void savedSparseMatrixExecutesItsNineGateCommandsThroughPreparationAndTheEngine() throws Exception {
        var prepared = prepared(19.5 * DT);
        var result = new HeadlessSimulationEngine().runSimulation(prepared, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(GATES, result.getSignalNames());
        assertEquals(20, result.getSignalData(0).length);

        // Balanced inputs (100, -50, -50) select input sector 1 and equal negative-rail duties.
        // A 40 V reference at pi/2 gives dOUT = (0.2, 0, 0.6, 0, 0.2).
        // The desktop switching table changes the negative rail at T/4 and 3T/4.
        // Its inverter vector is (1, 1, 1) in (T/10, 4T/10) and (6T/10, 9T/10).
        assertSample(result, 0,  new float[]{1,0,0,0,0,1,1,0,0});
        assertSample(result, 3,  new float[]{1,0,0,0,0,1,1,1,1});
        assertSample(result, 7,  new float[]{1,0,0,0,1,0,1,1,1});
        assertSample(result, 11, new float[]{1,0,0,0,1,0,1,0,0});
        assertSample(result, 17, new float[]{1,0,0,0,0,1,1,1,1});
        assertSample(result, 19, new float[]{1,0,0,0,0,1,1,0,0});
    }

    @Test
    void frequencyReferenceAndClockPeakUseTheCurrentSimulationTime() throws Exception {
        var result = new HeadlessSimulationEngine().runSimulation(
                prepared(10.5 * DT, true), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        // The sine clock peaks at 5*DT. The falling sample at 6*DT starts the next period.
        // The reference frequency 1/(24*DT) selects pi/2 at that new period's start.
        assertSample(result, 0, new float[]{1,0,0,0,0,1,0,0,1});
        assertSample(result, 6, new float[]{1,0,0,0,0,1,1,0,0});
        assertSample(result, 7, new float[]{1,0,0,0,0,1,1,1,1});
        assertSample(result, 10, new float[]{1,0,0,0,1,0,1,1,1});
    }

    @Test
    void pauseAndExtensionPreserveTheSwitchingPeriodWhileANewSessionStartsAtZero() throws Exception {
        var prepared = prepared(7.5 * DT);
        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults());
                var independent = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            assertArrayEquals(new float[]{1,0,0,0,0,1,1,0,0}, session.step().values());
            var delivered = new java.util.concurrent.atomic.AtomicInteger();
            var paused = session.run(() -> false, sample -> {
                if (delivered.incrementAndGet() == 3) session.pause();
            });
            assertEquals(SimulationSession.State.PAUSED, paused.state());
            assertEquals(4, paused.committedSteps());
            assertObservationSample(session, 3, new float[]{1,0,0,0,0,1,1,1,1});
            assertEquals(8, session.run().committedSteps());
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());

            session.extend(19.5 * DT);
            assertEquals(20, session.run().committedSteps());
            assertObservationSample(session, 7, new float[]{1,0,0,0,1,0,1,1,1});
            assertObservationSample(session, 11, new float[]{1,0,0,0,1,0,1,0,0});
            assertObservationSample(session, 17, new float[]{1,0,0,0,0,1,1,1,1});
            assertObservationSample(session, 19, new float[]{1,0,0,0,0,1,1,0,0});
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());

            var firstIndependent = independent.step();
            assertEquals(0, firstIndependent.time());
            assertArrayEquals(new float[]{1,0,0,0,0,1,1,0,0}, firstIndependent.values());
            independent.step();
            independent.step();
            assertArrayEquals(new float[]{1,0,0,0,0,1,1,1,1}, independent.step().values());
        }
    }

    private static void assertObservationSample(SimulationSession session, int index, float[] expected) {
        float[][] rows = session.observe(index, 1).values();
        for (int gate = 0; gate < GATES.length; gate++) {
            assertEquals(expected[gate], rows[gate][0], GATES[gate] + " at sample " + index);
        }
    }

    private static void assertSample(SimulationResult result, int index, float[] expected) {
        for (int gate = 0; gate < GATES.length; gate++) {
            assertEquals(expected[gate], result.getSignalData(gate)[index], GATES[gate] + " at sample " + index);
        }
    }

    private static PreparedCircuit prepared(double duration) throws Exception {
        return prepared(duration, false);
    }

    private static PreparedCircuit prepared(double duration, boolean frequencyReference) throws Exception {
        String[] inputs = {"clock", "ur", "us", "ut", "inputPeak", "outputPeak", "outputFrequency", "outputAngle"};
        double[] values = {0, 100, -50, -50, 100, 40, frequencyReference ? 1 / (24 * DT) : 0, Math.PI / 2};
        StringBuilder text = new StringBuilder("dt " + DT + "\ntDURATION " + duration + "\n");
        // Consumers precede producers to exercise the public dependency ordering.
        text.append(block(5, "scope", "", GATES, new String[0]));
        text.append(block(59, "matrix", "", inputs, GATES));
        for (int index = inputs.length - 1; index >= 0; index--) {
            boolean clock = frequencyReference && index == 0;
            text.append(block(clock ? 4 : 3, inputs[index], clock ? "400 1 " + (1 / PERIOD) + " 0 0 0.5"
                    : Double.toString(values[index]), new String[0], new String[]{inputs[index]}));
        }
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(text.toString())), "sparse-matrix.ipes");
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString()).execution();
    }

    private static String block(int type, String name, String parameters, String[] inputs, String[] outputs) {
        return "c\n<ElementCONTROL>\ntyp " + type + "\nidStringDialog " + name
                + "\nparameter[] " + parameters + "\nparameterString[] /\nlabelAnfangsKnoten[] " + labels(inputs)
                + "\nlabelEndKnoten[] " + labels(outputs) + "\n<" + (char) 92 + "ElementCONTROL>\n";
    }

    private static String labels(String[] labels) {
        return labels.length == 0 ? "" : "/" + String.join("/", labels);
    }
}
