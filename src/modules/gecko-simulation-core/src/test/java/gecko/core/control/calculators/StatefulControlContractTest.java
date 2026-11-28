package gecko.core.control.calculators;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatefulControlContractTest {
    private double step(AbstractControlCalculatable calculator, double dt, double... inputs) {
        for (int i = 0; i < inputs.length; i++) { calculator._inputSignal[i] = new double[]{inputs[i]}; }
        calculator.berechneYOUT(dt);
        return calculator._outputSignal[0][0];
    }

    @Test
    void derivativeRespondsToSlopeAndGainRatherThanAConstantLevel() {
        PDCalculator calculator = new PDCalculator(2);
        assertEquals(0, step(calculator, .125, 0));
        assertEquals(16, step(calculator, .125, 1));
        assertEquals(16, step(calculator, .125, 2));
        assertEquals(-48, step(calculator, .125, -1));
        assertEquals(0, step(calculator, .125, -1));
        calculator.setGain(3);
        assertEquals(24, step(calculator, .125, 0));
    }

    @Test
    void secondOrderFilterMatchesTheAnalyticalUndampedStepResponse() {
        PT2Calculator calculator = new PT2Calculator(1, 1);
        calculator.setTimeConstant(.5);
        calculator.setGain(2);
        double dt = .0001;
        for (int i = 0; i <= 10000; i++) { step(calculator, dt, 1); }
        // G(s)=2/(1+(.5s)^2), zero initial state: y(1)=2*(1-cos(2)).
        assertEquals(2.832293673094285, calculator._outputSignal[0][0], .0003);
    }

    @Test
    void secondOrderImpulsePreservesItsOscillatoryHistory() {
        PT2Calculator calculator = new PT2Calculator(1, 1);
        // Central-difference oscillator with dt=T has a six-sample oscillation period.
        double[] expected = {0, 1, 1, 0, -1, -1, 0, 1, 1, 0};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], step(calculator, 1, i == 0 ? 1 : 0), 1e-12);
        }
    }

    @Test
    void counterCountsOnlyRisingEdgesAndResetDominatesAnEdge() {
        CounterCalculatable calculator = new CounterCalculatable();
        double[][] trace = {{0,0,0},{.49,0,0},{.5,0,1},{1,0,1},{0,0,1},
                {1,1,0},{1,0,0},{0,0,0},{1,0,1}};
        for (double[] point : trace) {
            assertEquals(point[2], step(calculator, .1, point[0], point[1]));
        }
    }

    @Test
    void sampleHoldTracksWhileEnabledAndRetainsItsLastSampleOtherwise() {
        SampleHoldCalculator calculator = new SampleHoldCalculator();
        assertEquals(0, step(calculator, .1, 100, 0));
        assertEquals(7, step(calculator, .1, 7, 1));
        assertEquals(7, step(calculator, .1, 99, 0));
        assertEquals(7, step(calculator, .1, -3, .5));
        assertEquals(-3, step(calculator, .1, -3, Math.nextUp(.5)));
        assertEquals(4, step(calculator, .1, 4, 1));
        assertEquals(5, step(calculator, .1, 5, 1));
    }

    @Test
    void hysteresisRetainsMemoryInsideTheBandWithTheExistingBoundaryConventions() {
        HysteresisCalculatorInternal internal = new HysteresisCalculatorInternal(2);
        HysteresisCalculatorExternal external = new HysteresisCalculatorExternal();
        // Upper equality switches positive; lower equality remains inside the hold band.
        double[][] trace = {{0,0},{3,1},{0,1},{-2,1},{-3,-1},{0,-1},{2,1}};
        for (double[] point : trace) {
            assertEquals(point[1], step(internal, .1, point[0]));
            assertEquals(point[1], step(external, .1, point[0], 2));
        }
        internal.setHValue(.5);
        assertEquals(-1, step(internal, .1, -1));
        assertEquals(-1, step(external, .1, -1, .5));
        internal.setHValue(0);
        assertEquals(0, step(internal, .1, 0));
        assertEquals(0, step(external, .1, 0, 0));
    }

    @Test
    void internalAndExternalLimitsClipBothEndsAndRespondToChangedBounds() {
        LimitCalculatorInternal internal = new LimitCalculatorInternal(-2, 3);
        LimitCalculatorExternal external = new LimitCalculatorExternal();
        double[][] points = {{-10,-2},{-2,-2},{0,0},{3,3},{10,3}};
        for (double[] point : points) {
            assertEquals(point[1], step(internal, .1, point[0]));
            assertEquals(point[1], step(external, .1, point[0], -2, 3));
        }
        internal.setMinMaxValues(1, 1);
        assertEquals(1, step(internal, .1, 100));
        assertEquals(1, step(external, .1, -100, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> internal.setMinMaxValues(3, -2));
    }
}
