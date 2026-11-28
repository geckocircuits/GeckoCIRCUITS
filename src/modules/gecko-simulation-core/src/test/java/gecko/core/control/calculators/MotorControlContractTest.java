package gecko.core.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MotorControlContractTest {
    private PmsmControlCalculator controller() {
        PmsmControlCalculator calculator = new PmsmControlCalculator();
        for (int i = 0; i < 12; i++) { calculator.checkInputWithoutConnectionAndFill(i); }
        calculator._inputSignal[5][0] = 1;   // speed PI gain
        calculator._inputSignal[6][0] = 1;   // speed integration time
        calculator._inputSignal[7][0] = 100; // speed-loop output limit
        calculator._inputSignal[8][0] = 2;   // current PI gain
        calculator._inputSignal[9][0] = 1;   // current integration time
        calculator._inputSignal[10][0] = 100;
        calculator._inputSignal[11][0] = 1;  // update stationary-frame voltage
        return calculator;
    }

    @Test
    void zeroErrorProducesZeroVoltageAndCurrentReferences() {
        PmsmControlCalculator calculator = controller();
        for (int step = 0; step < 10; step++) { calculator.berechneYOUT(0.1); }
        for (double[] output : calculator._outputSignal) { assertEquals(0, output[0]); }
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 1", "90, -1, 0", "180, 0, -1", "-90, 1, 0"})
    void measuredBalancedCurrentRotatesIntoTheExpectedDqAxes(double degrees, double iq, double id) {
        PmsmControlCalculator calculator = controller();
        calculator._inputSignal[0][0] = 1;
        calculator._inputSignal[1][0] = -0.5;
        calculator._inputSignal[3][0] = Math.toRadians(degrees);
        calculator.berechneYOUT(0.1);
        assertEquals(iq, calculator._outputSignal[6][0], 1e-12);
        assertEquals(id, calculator._outputSignal[7][0], 1e-12);
        assertEquals(0, calculator._outputSignal[5][0]);
    }

    @Test
    void cascadedPiLoopsFollowTheWorkedConstantSpeedErrorResponse() {
        PmsmControlCalculator calculator = controller();
        calculator._inputSignal[2][0] = -2;
        calculator.berechneYOUT(0.1);
        assertEquals(2.1, calculator._outputSignal[4][0], 1e-12);
        assertEquals(4.41, calculator._outputSignal[2][0], 1e-12);
        assertEquals(4.41, calculator._outputSignal[1][0], 1e-12);
        calculator.berechneYOUT(0.1);
        assertEquals(2.3, calculator._outputSignal[4][0], 1e-12);
        assertEquals(5.25, calculator._outputSignal[2][0], 1e-12);
    }

    @Test
    void saturationRemainsBoundedAndRecoversWhenBothErrorsReverse() {
        PmsmControlCalculator calculator = controller();
        calculator._inputSignal[7][0] = 2;
        calculator._inputSignal[10][0] = 3;
        for (int sign : new int[]{1, -1}) {
            calculator._inputSignal[0][0] = sign * 100;
            calculator._inputSignal[1][0] = -sign * 50;
            calculator._inputSignal[2][0] = -sign * 100;
            for (int step = 0; step < 100; step++) {
                calculator.berechneYOUT(1);
                assertTrue(Math.abs(calculator._outputSignal[2][0]) <= 3);
                assertTrue(Math.abs(calculator._outputSignal[3][0]) <= 3);
                assertTrue(Math.abs(calculator._outputSignal[4][0]) <= 2);
            }
            assertEquals(sign * 3, calculator._outputSignal[2][0]);
            assertEquals(-sign * 3, calculator._outputSignal[3][0]);
            assertEquals(sign * 2, calculator._outputSignal[4][0]);
        }
    }

    @Test
    void stationaryFrameVoltageIsHeldUntilTheUpdateSignalExceedsItsThreshold() {
        PmsmControlCalculator calculator = controller();
        calculator._inputSignal[2][0] = -2;
        calculator._inputSignal[11][0] = 0;
        calculator.berechneYOUT(0.1);
        assertEquals(0, calculator._outputSignal[1][0]);
        assertEquals(4.41, calculator._outputSignal[2][0], 1e-12);
        calculator._inputSignal[11][0] = 1;
        calculator.berechneYOUT(0.1);
        assertEquals(5.25, calculator._outputSignal[1][0], 1e-12);
        calculator._inputSignal[2][0] = 10;
        calculator._inputSignal[11][0] = 0.999;
        calculator.berechneYOUT(0.1);
        assertEquals(5.25, calculator._outputSignal[1][0], 1e-12);
        assertTrue(calculator._outputSignal[2][0] < 0);
        calculator._inputSignal[11][0] = 1;
        calculator.berechneYOUT(0.1);
        assertEquals(calculator._outputSignal[2][0], calculator._outputSignal[1][0]);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.3, 1.2, 3.14, -1.5})
    void abcDqRoundTripPreservesBalancedPhasesAndRejectsCommonMode(double angle) {
        ABCDQCalculator forward = new ABCDQCalculator();
        double[] phases = {3, -1, -2};
        for (int i = 0; i < 3; i++) { forward._inputSignal[i] = new double[]{phases[i]}; }
        forward._inputSignal[3] = new double[]{angle};
        forward.berechneYOUT(0.1);
        double d = forward._outputSignal[0][0], q = forward._outputSignal[1][0];
        for (int i = 0; i < 3; i++) { forward._inputSignal[i][0] += 10; }
        forward.berechneYOUT(0.1);
        assertEquals(d, forward._outputSignal[0][0], 1e-12);
        assertEquals(q, forward._outputSignal[1][0], 1e-12);
        DQABCDCalculator inverse = new DQABCDCalculator();
        inverse._inputSignal[0] = new double[]{d};
        inverse._inputSignal[1] = new double[]{q};
        inverse._inputSignal[2] = new double[]{angle};
        inverse.berechneYOUT(0.1);
        for (int i = 0; i < 3; i++) { assertEquals(phases[i], inverse._outputSignal[i][0], 1e-10); }
    }
}
