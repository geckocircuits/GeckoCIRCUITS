package gecko.geckocircuits.control.calculators;

import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class SparseMatrixParityContractTest {
    private static final double PERIOD = 1.0 / 25000;
    private static final double DT = PERIOD / 20;
    private final double previousDesktopTime = AbstractControlCalculatable._time;
    enum RuntimePath { DESKTOP, CORE }

    private record Probe(double[][] input, double[][] output, DoubleConsumer start, BiConsumer<Double, Double> step) { }

    @AfterEach
    void restoreDesktopTime() { AbstractControlCalculatable.setTime(previousDesktopTime); }

    static Stream<Arguments> workedVectors() {
        return Stream.of(RuntimePath.values()).flatMap(path -> Stream.of(
                Arguments.of(path, 0, 0, new double[]{1,0,0,0,0,1,0,0,1}),
                Arguments.of(path, 45, 2, new double[]{1,0,0,0,0,1,1,0,0})));
    }

    @ParameterizedTest
    @MethodSource("workedVectors")
    void bothRuntimeAdaptersPublishTheWorkedSectorVectors(
            RuntimePath path, double angleDegrees, int sample, double[] expected) {
        var probe = probe(path);
        probe.input[7][0] = Math.toRadians(angleDegrees);
        probe.start.accept(DT);
        for (int index = 0; index <= sample; index++) probe.step.accept(DT, index * DT);
        // At zero phase, the inverter selects (0,0,1). At pi/4 and T/10 it selects (1,0,0).
        // The balanced (100,-50,-50) input connects the positive rail to R and negative rail to T.
        assertOutputs(probe, expected);
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void frequencyReferenceUsesTheCurrentAdaptersClockAtTheNextPeak(RuntimePath path) {
        var probe = probe(path);
        probe.input[6][0] = 1 / (24 * DT);
        probe.start.accept(DT);
        probe.step.accept(DT, 0.0);
        assertOutputs(probe, new double[]{1,0,0,0,0,1,0,0,1});
        for (int index = 1; index <= 6; index++) {
            probe.input[0][0] = Math.sin(2 * Math.PI * index / 20);
            probe.step.accept(DT, index * DT);
        }
        // The falling clock sample starts a period at 6*DT, where the reference phase is pi/2.
        assertOutputs(probe, new double[]{1,0,0,0,0,1,1,0,0});
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void replacementInputRowsReachTheNextClockPeak(RuntimePath path) {
        var probe = probe(path);
        probe.start.accept(DT);
        probe.step.accept(DT, 0.0);
        assertOutputs(probe, new double[]{1,0,0,0,0,1,1,0,0});
        probe.input[7] = new double[]{0};
        probe.input[0] = new double[]{1};
        probe.step.accept(DT, DT);
        probe.input[0] = new double[]{0};
        probe.step.accept(DT, 2 * DT);
        assertOutputs(probe, new double[]{1,0,0,0,0,1,0,0,1});
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void reinitializationReplaysTheFirstSwitchingPeriod(RuntimePath path) {
        var probe = probe(path);
        probe.start.accept(DT);
        for (int index = 0; index < 20; index++) probe.step.accept(DT, index * DT);
        probe.start.accept(DT);
        probe.step.accept(DT, 0.0);
        assertOutputs(probe, new double[]{1,0,0,0,0,1,1,0,0});
        for (int index = 1; index <= 3; index++) probe.step.accept(DT, index * DT);
        assertOutputs(probe, new double[]{1,0,0,0,0,1,1,1,1});
    }

    @Test
    void desktopWiresRetainTheirOutputRowsAcrossCalculationAndRestart() throws Exception {
        var calculator = new SparseMatrixCalculator();
        configureInputs(calculator._inputSignal);
        var consumers = new GainCalculator[9];
        var originalRows = calculator._outputSignal.clone();
        for (int port = 0; port < consumers.length; port++) {
            consumers[port] = new GainCalculator(1);
            consumers[port].setInputSignal(0, calculator, port);
        }
        for (int run = 0; run < 2; run++) {
            calculator.initializeAtSimulationStart(DT);
            for (int index = 0; index <= 3; index++) {
                AbstractControlCalculatable.setTime(index * DT);
                calculator.berechneYOUT(DT);
            }
            // Keep the existing public pulse-width call available between normal calculations.
            calculator.setPulseWidths(0.2, 0, 0.6, 0, 0.2, 0.5, 0.5, 25000);
            double[] expected = {1,0,0,0,0,1,1,1,1};
            for (int port = 0; port < consumers.length; port++) {
                assertSame(originalRows[port], calculator._outputSignal[port]);
                assertSame(originalRows[port], consumers[port]._inputSignal[0]);
                consumers[port].berechneYOUT(DT);
                assertEquals(expected[port], consumers[port]._outputSignal[0][0], "consumer " + port);
            }
        }
    }

    private static Probe probe(RuntimePath path) {
        Probe result;
        if (path == RuntimePath.DESKTOP) {
            var calculator = new SparseMatrixCalculator();
            result = new Probe(calculator._inputSignal, calculator._outputSignal,
                    calculator::initializeAtSimulationStart, (dt, time) -> {
                        AbstractControlCalculatable.setTime(time);
                        calculator.berechneYOUT(dt);
                    });
        } else {
            var calculator = new gecko.core.control.calculators.SparseMatrixCalculator();
            result = new Probe(calculator._inputSignal, calculator._outputSignal,
                    calculator::initializeAtSimulationStart, (dt, time) -> {
                        calculator.setSimulationTime(time);
                        calculator.berechneYOUT(dt);
                    });
        }
        assertEquals(8, result.input.length);
        assertEquals(9, result.output.length);
        configureInputs(result.input);
        return result;
    }

    private static void configureInputs(double[][] input) {
        double[] values = {0, 100, -50, -50, 100, 40, 0, Math.PI / 2};
        for (int port = 0; port < values.length; port++) input[port] = new double[]{values[port]};
    }

    private static void assertOutputs(Probe probe, double[] expected) {
        for (int port = 0; port < expected.length; port++) {
            assertEquals(expected[port], probe.output[port][0], "output port " + port);
        }
    }
}
