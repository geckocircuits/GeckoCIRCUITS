package gecko.core.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SparseMatrixSwitchingContractTest {
    private static final double PERIOD = 1.0 / 25000;
    private static final double DT = PERIOD / 1000;

    private SparseMatrixCalculator calculator(double inputAngle, double outputAngle) {
        SparseMatrixCalculator calculator = new SparseMatrixCalculator();
        for (int i = 0; i < 8; i++) { calculator.checkInputWithoutConnectionAndFill(i); }
        for (int phase = 0; phase < 3; phase++) {
            calculator._inputSignal[phase + 1][0] = 100 * Math.sin(inputAngle - phase * 2 * Math.PI / 3);
        }
        calculator._inputSignal[4][0] = 100;
        calculator._inputSignal[5][0] = 40;
        calculator._inputSignal[7][0] = outputAngle;
        calculator.initializeAtSimulationStart(DT);
        return calculator;
    }

    static Stream<Arguments> sectorPairs() {
        return IntStream.range(0, 12).boxed().flatMap(input -> IntStream.range(0, 12)
                .mapToObj(output -> Arguments.of(15 + 30 * input, 15 + 30 * output)));
    }

    @ParameterizedTest(name = "input {0} degrees, output {1} degrees")
    @MethodSource("sectorPairs")
    void everySectorPairMaintainsAConductingPathWithoutShortingTheInput(double inputDegrees, double outputDegrees) {
        SparseMatrixCalculator calculator = calculator(Math.toRadians(inputDegrees), Math.toRadians(outputDegrees));
        double[] onSamples = new double[6];
        int freewheelingSamples = 0;
        for (int step = 0; step < 1000; step++) {
            calculator.berechneYOUT(DT);
            assertGateInvariants(calculator);
            for (int gate = 0; gate < 6; gate++) { onSamples[gate] += calculator._outputSignal[gate][0]; }
            double[][] gates = calculator._outputSignal;
            if (gates[6][0] == gates[7][0] && gates[7][0] == gates[8][0]) { freewheelingSamples++; }
        }
        assertTrue(freewheelingSamples > 0 && freewheelingSamples < 1000,
                "a nonzero unsaturated reference uses both active vectors and freewheeling");
        // Unity power-factor rectification allocates each rail's duty in proportion to phase voltage.
        double positiveVoltage = 0;
        for (int phase = 0; phase < 3; phase++) {
            positiveVoltage += Math.max(0, calculator._inputSignal[phase + 1][0]);
        }
        for (int phase = 0; phase < 3; phase++) {
            double voltage = calculator._inputSignal[phase + 1][0];
            assertEquals(Math.max(0, voltage) / positiveVoltage, onSamples[phase] / 1000, 0.002);
            assertEquals(Math.max(0, -voltage) / positiveVoltage, onSamples[phase + 3] / 1000, 0.002);
        }
    }

    private void assertGateInvariants(SparseMatrixCalculator calculator) {
        for (double[] gate : calculator._outputSignal) {
            assertTrue(gate[0] == 0 || gate[0] == 1, "switch commands are binary");
        }
        double[][] gates = calculator._outputSignal;
        assertEquals(1, gates[0][0] + gates[1][0] + gates[2][0], "one phase connects to the positive rail");
        assertEquals(1, gates[3][0] + gates[4][0] + gates[5][0], "one phase connects to the negative rail");
        for (int phase = 0; phase < 3; phase++) {
            assertEquals(0, gates[phase][0] * gates[phase + 3][0], "the same phase cannot short the rails");
        }
    }

    @Test
    void restartingReplaysTheSameSwitchingPeriod() {
        SparseMatrixCalculator calculator = calculator(Math.toRadians(15), Math.toRadians(75));
        double[][] expected = new double[1000][9];
        for (int step = 0; step < expected.length; step++) {
            calculator.berechneYOUT(DT);
            for (int gate = 0; gate < 9; gate++) { expected[step][gate] = calculator._outputSignal[gate][0]; }
        }
        calculator.initializeAtSimulationStart(DT);
        for (int step = 0; step < expected.length; step++) {
            calculator.berechneYOUT(DT);
            for (int gate = 0; gate < 9; gate++) {
                assertEquals(expected[step][gate], calculator._outputSignal[gate][0], "restart must reset period history");
            }
        }
    }

    @Test
    void aNewClockPeakStartsAnotherSwitchingPeriod() {
        SparseMatrixCalculator calculator = calculator(Math.toRadians(15), Math.toRadians(75));
        double[][] expected = new double[1000][9];
        calculator._inputSignal[0][0] = 1;
        for (int step = 0; step < 1000; step++) {
            calculator.berechneYOUT(DT);
            for (int gate = 0; gate < 9; gate++) { expected[step][gate] = calculator._outputSignal[gate][0]; }
        }
        calculator._inputSignal[0][0] = 2;
        calculator.berechneYOUT(DT);
        calculator._inputSignal[0][0] = 1;
        // The observed clock period is now 1001 steps. Use its actual duration.
        double newDt = 1001 * DT / 1000;
        for (int step = 0; step < 1000; step++) {
            calculator.berechneYOUT(newDt);
            assertGateInvariants(calculator);
            if (step == 0) {
                for (int gate = 0; gate < 9; gate++) { assertEquals(expected[0][gate], calculator._outputSignal[gate][0]); }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-345, -15, 0, 15, 45, 75, 105, 345, 375})
    void addingAFullTurnToTheReferenceAngleCannotChangeGateCommands(double degrees) {
        SparseMatrixCalculator first = calculator(Math.toRadians(15), Math.toRadians(degrees));
        SparseMatrixCalculator equivalent = calculator(Math.toRadians(15), Math.toRadians(degrees + 360));
        for (int step = 0; step < 1000; step++) {
            first.berechneYOUT(DT);
            equivalent.berechneYOUT(DT);
            for (int gate = 0; gate < 9; gate++) {
                assertEquals(first._outputSignal[gate][0], equivalent._outputSignal[gate][0],
                        "equivalent electrical angles must generate the same switching period");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {15, 45, 75, 105, 135, 165, 195, 225, 255, 285, 315, 345})
    void zeroOutputReferenceCreatesNoLineToLineVoltage(double degrees) {
        SparseMatrixCalculator calculator = calculator(Math.toRadians(15), Math.toRadians(degrees));
        calculator._inputSignal[5][0] = 0;
        for (int step = 0; step < 1000; step++) {
            calculator.berechneYOUT(DT);
            assertGateInvariants(calculator);
            assertEquals(calculator._outputSignal[6][0], calculator._outputSignal[7][0]);
            assertEquals(calculator._outputSignal[7][0], calculator._outputSignal[8][0]);
        }
    }

    @Test
    void zeroElectricalAngleStillProducesAnActiveSwitchingPeriod() {
        SparseMatrixCalculator calculator = calculator(Math.toRadians(15), 0);
        int activeSamples = 0;
        for (int step = 0; step < 1000; step++) {
            calculator.berechneYOUT(DT);
            double[][] gates = calculator._outputSignal;
            if (gates[6][0] != gates[7][0] || gates[7][0] != gates[8][0]) { activeSamples++; }
        }
        assertTrue(activeSamples > 0 && activeSamples < 1000,
                "a zero electrical angle is a valid nonzero voltage reference, not a disabled inverter");
    }

    @ParameterizedTest
    @CsvSource({"25, 15", "50, 75"})
    void frequencyAndExplicitAngleReferencesAgreeAtTheSameElectricalPhase(double frequency, double degrees) {
        double oldTime = AbstractControlCalculatable._time;
        try {
            AbstractControlCalculatable.setTime((3 + degrees / 360) / frequency);
            SparseMatrixCalculator timed = calculator(Math.toRadians(15), 0);
            timed._inputSignal[6][0] = frequency;
            SparseMatrixCalculator angled = calculator(Math.toRadians(15), Math.toRadians(degrees));
            for (int step = 0; step < 1000; step++) {
                timed.berechneYOUT(DT);
                angled.berechneYOUT(DT);
                for (int gate = 0; gate < 9; gate++) {
                    assertEquals(angled._outputSignal[gate][0], timed._outputSignal[gate][0]);
                }
            }
        } finally {
            AbstractControlCalculatable.setTime(oldTime);
        }
    }
}
