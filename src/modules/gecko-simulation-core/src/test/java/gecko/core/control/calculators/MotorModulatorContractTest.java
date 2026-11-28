package gecko.core.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MotorModulatorContractTest {
    private PmsmModulatorCalculator calculator(double alpha, double beta, double dc) {
        PmsmModulatorCalculator calculator = new PmsmModulatorCalculator();
        for (int i = 0; i < 4; i++) { calculator.checkInputWithoutConnectionAndFill(i); }
        calculator._inputSignal[0][0] = alpha;
        calculator._inputSignal[1][0] = beta;
        calculator._inputSignal[3][0] = dc;
        return calculator;
    }

    @ParameterizedTest
    @CsvSource({"30, .25, .50, .75", "90, .50, .25, .75", "150, .75, .25, .50",
            "-150, .75, .50, .25", "-90, .50, .75, .25", "-30, .25, .75, .50"})
    void sixSectorCentersProduceTheKnownSymmetricSpaceVectorDuties(double degrees, double u, double v, double w) {
        PmsmModulatorCalculator calculator = calculator(50 * Math.cos(Math.toRadians(degrees)),
                50 * Math.sin(Math.toRadians(degrees)), 100);
        double[] expected = {u, v, w};
        // Avoid rounding ambiguity at trigonometric boundaries while checking both sides of each switch.
        for (double carrier : new double[]{0, .25 - 1e-12, .25 + 1e-12, .5 - 1e-12, .5 + 1e-12,
                .75 - 1e-12, .75 + 1e-12, 1}) {
            calculator._inputSignal[2][0] = carrier;
            calculator.berechneYOUT(0.001);
            for (int phase = 0; phase < 3; phase++) {
                assertEquals(carrier < expected[phase] ? 1 : 0, calculator._outputSignal[phase][0]);
            }
        }
    }

    @Test
    void zeroVoltageUsesEqualHalfDutyAndTheCarrierComparisonHasAnExactBoundary() {
        PmsmModulatorCalculator calculator = calculator(0, 0, 100);
        calculator._inputSignal[2][0] = Math.nextDown(0.5);
        calculator.berechneYOUT(0.001);
        for (double[] output : calculator._outputSignal) { assertEquals(1, output[0]); }
        calculator._inputSignal[2][0] = 0.5;
        calculator.berechneYOUT(0.001);
        for (double[] output : calculator._outputSignal) { assertEquals(0, output[0]); }
    }

    @Test
    void voltageMagnitudeClampingAndNegativeAxisRepresentationAreConsistent() {
        PmsmModulatorCalculator limited = calculator(-100, 0, 100);
        PmsmModulatorCalculator overdriven = calculator(-1000, 0, 100);
        PmsmModulatorCalculator adjacent = calculator(-100, -1e-10, 100);
        for (double carrier : new double[]{.1, .3, .5, .7, .9}) {
            for (PmsmModulatorCalculator calculator : new PmsmModulatorCalculator[]{limited, overdriven, adjacent}) {
                calculator._inputSignal[2][0] = carrier;
                calculator.berechneYOUT(0.001);
            }
            for (int phase = 0; phase < 3; phase++) {
                assertEquals(limited._outputSignal[phase][0], overdriven._outputSignal[phase][0]);
                assertEquals(limited._outputSignal[phase][0], adjacent._outputSignal[phase][0]);
            }
        }
    }
}
