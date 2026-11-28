package gecko.core.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ControlNumericalContractTest {
    private static double evaluate(AbstractControlCalculatable calculator, double... inputs) {
        for (int i = 0; i < inputs.length; i++) {
            calculator._inputSignal[i] = new double[]{inputs[i]};
        }
        calculator.berechneYOUT(0.1);
        return calculator._outputSignal[0][0];
    }

    static Stream<Arguments> elementaryFunctions() {
        return Stream.of(
                Arguments.of(new SinCalculator(), Math.PI / 2, 1),
                Arguments.of(new CosCalculator(), Math.PI, -1),
                Arguments.of(new TanCalculator(), Math.PI / 4, 1),
                Arguments.of(new ASinCalculator(), 1, Math.PI / 2),
                Arguments.of(new ACosCalculator(), -1, Math.PI),
                Arguments.of(new ATanCalculator(), 1, Math.PI / 4),
                Arguments.of(new SqrtCalculator(), 9, 3),
                Arguments.of(new ExpCalculator(), 0, 1),
                Arguments.of(new LnCalculator(), Math.E, 1),
                Arguments.of(new AbsCalculator(), -3, 3),
                Arguments.of(new SquareCalculator(), -3, 9),
                Arguments.of(new SignumCalculator(), -3, -1),
                Arguments.of(new SignumCalculator(), 0, 0),
                Arguments.of(new SignumCalculator(), 3, 1),
                Arguments.of(new RoundCalculator(), 1.5, 2),
                Arguments.of(new RoundCalculator(), -1.5, -1));
    }

    @ParameterizedTest
    @MethodSource("elementaryFunctions")
    void elementaryFunctionsMatchKnownValues(AbstractControlCalculatable calculator, double input, double expected) {
        assertEquals(expected, evaluate(calculator, input), 1e-12);
    }

    @Test
    void comparisonsDistinguishEqualityAndBothOrderings() {
        for (double[] values : new double[][]{{1, 2}, {2, 2}, {3, 2}}) {
            double x = values[0], y = values[1];
            assertEquals(x > y ? 1 : 0, evaluate(new GreaterThanCalculator(), x, y));
            assertEquals(x >= y ? 1 : 0, evaluate(new GreaterEqualCalculator(), x, y));
            assertEquals(x == y ? 1 : 0, evaluate(new EqualCalculatorTwoInputs(), x, y));
            assertEquals(x != y ? 1 : 0, evaluate(new NotEqualCalculator(), x, y));
            assertEquals(Math.min(x, y), evaluate(new MinCalculatorTwoInputs(), x, y));
            assertEquals(Math.max(x, y), evaluate(new MaxCalculatorTwoInputs(), x, y));
        }
    }

    @Test
    void divisionHandlesDisconnectedAndZeroDenominatorInputs() {
        assertEquals(3, evaluate(new DivCalculator(), 6, 2));
        assertEquals(0, evaluate(new DivCalculator(), 0, 0));
        assertEquals(1e40, evaluate(new DivCalculator(), 1, 0));
        assertEquals(-1e40, evaluate(new DivCalculator(), -1, 0));
    }

    @Test
    void piControllerIntegratesAConstantInputAndRestartsWithoutOldHistory() {
        PICalculator pi = new PICalculator(0, 0);
        pi.setR0(2);
        pi.setA1(1);
        pi.initializeAtSimulationStart(0.1);
        assertEquals(2.05, evaluate(pi, 1), 1e-12);
        assertEquals(2.15, evaluate(pi, 1), 1e-12);
        pi.initializeAtSimulationStart(0.1);
        assertEquals(2.05, evaluate(pi, 1), 1e-12);
    }

    @Test
    void integratorClampsBothLimitsAndResetDiscardsHistory() {
        IntegratorCalculation integrator = new IntegratorCalculation(1, 0.25, -1, 1);
        integrator.initializeAtSimulationStart(0.1);
        assertEquals(0.25, integrator._outputSignal[0][0]);
        assertEquals(0.35, evaluate(integrator, 2, 0), 1e-12);
        assertEquals(1, evaluate(integrator, 100, 0));
        assertEquals(0.25, evaluate(integrator, 0, 1));
        assertEquals(0.25, evaluate(integrator, 0, 0));
        integrator.setA1Val(2);
        integrator.setMinMax(-0.5, 0.5);
        assertEquals(-0.5, evaluate(integrator, -100, 0));
        assertThrows(IllegalArgumentException.class, () -> integrator.setMinMax(2, 1));
    }

    @Test
    void firstOrderFilterSettlesToItsAnalyticalStepResponse() {
        PT1Calculator filter = new PT1Calculator(1, 1);
        filter.setTimeConstant(0.5);
        filter.setGain(2);
        filter._inputSignal[0] = new double[]{1};
        double last = 0;
        double dt = 0.0001;
        for (int i = 0; i < 10000; i++) {
            filter.berechneYOUT(dt);
            double current = filter._outputSignal[0][0];
            assertTrue(current >= last && current < 2);
            last = current;
        }
        assertEquals(2 * (1 - Math.exp(-2)), last, 0.0001);
    }
}
