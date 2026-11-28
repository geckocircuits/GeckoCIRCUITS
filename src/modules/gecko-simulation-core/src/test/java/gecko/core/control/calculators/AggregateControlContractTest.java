package gecko.core.control.calculators;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class AggregateControlContractTest {
    private final double oldTime = AbstractControlCalculatable._time;
    @AfterEach void restoreClock() { AbstractControlCalculatable.setTime(oldTime); }

    private double evaluate(AbstractControlCalculatable calculator, double... inputs) {
        for (int i = 0; i < inputs.length; i++) { calculator._inputSignal[i] = new double[]{inputs[i]}; }
        calculator.berechneYOUT(.1);
        return calculator._outputSignal[0][0];
    }

    @ParameterizedTest
    @CsvSource({"0,0,0,0,0","0,1,0,1,1","1,0,0,1,1","1,1,1,1,0"})
    void binaryBlocksFollowTheirTruthTables(double a, double b, double and, double or, double xor) {
        assertEquals(and, evaluate(new AndTwoPortCalculator(), a, b));
        assertEquals(or, evaluate(new OrCalculatorTwoInputs(), a, b));
        assertEquals(xor, evaluate(new XORCalculator(), a, b));
        assertEquals(a == 0 ? 1 : 0, evaluate(new NotCalculator(), a));
    }

    @ParameterizedTest
    @CsvSource({"0,0,0,0,0,1","0,0,1,0,1,0","0,1,0,0,1,0","0,1,1,0,1,0",
            "1,0,0,0,1,0","1,0,1,0,1,0","1,1,0,0,1,0","1,1,1,1,1,1"})
    void threeInputBlocksConsiderEveryPort(double a, double b, double c, double and, double or, double equal) {
        assertEquals(and, evaluate(new AndMultiInputCalculator(3), a, b, c));
        assertEquals(or, evaluate(new OrCalculatorMultipleInputs(3), a, b, c));
        assertEquals(equal, evaluate(new EqualCalculatorMultiInput(3), a, b, c));
    }

    @ParameterizedTest
    @CsvSource({"-2,7,3,-2,7,-12","7,-2,3,-2,7,6","3,7,-2,-2,7,-2","5,5,5,5,5,-5"})
    void aggregatesPreserveSignAndInputOrder(double a, double b, double c, double min, double max, double difference) {
        assertEquals(min, evaluate(new MinCalculatorMultiInputs(3), a, b, c));
        assertEquals(max, evaluate(new MaxCalculatorMultiInputs(3), a, b, c));
        assertEquals(difference, evaluate(new SubtractionMoreParameter(3), a, b, c));
    }

    @Test
    void subtractionAndEqualityHandleSignedAndNonbinaryValues() {
        assertEquals(6, evaluate(new SubtractionTwoParameter(), 10, 4));
        assertEquals(-6, evaluate(new SubtractionTwoParameter(), 4, 10));
        assertEquals(1, evaluate(new EqualCalculatorMultiInput(3), -2, -2, -2));
        assertEquals(0, evaluate(new EqualCalculatorMultiInput(3), -2, -2, 2));
    }

    @Test
    void muxKeepsPortOrderAndCopiesValuesUntilTheNextCalculation() {
        MUXControlCalculatable calculator = new MUXControlCalculatable(3);
        evaluate(calculator, 1, -2, 3);
        assertArrayEquals(new double[]{1,-2,3}, calculator._outputSignal[0]);
        calculator._inputSignal[0][0] = 99;
        assertEquals(1, calculator._outputSignal[0][0]);
        calculator.berechneYOUT(.1);
        assertArrayEquals(new double[]{99,-2,3}, calculator._outputSignal[0]);
    }

    @Test
    void timeSourcePublishesTheSimulationClock() {
        TimeCalculator calculator = new TimeCalculator();
        for (double time : new double[]{0, .25, 100.5}) {
            AbstractControlCalculatable.setTime(time);
            assertEquals(time, evaluate(calculator));
        }
    }
}
