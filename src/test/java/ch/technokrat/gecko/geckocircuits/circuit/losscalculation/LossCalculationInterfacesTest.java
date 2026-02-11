/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for loss calculation interfaces - AbstractLossCalculator,
 * AbstractLossCalculatorFabric, and LossCalculationSplittable.
 * Tests critical paths and edge cases in interface contracts.
 */
public class LossCalculationInterfacesTest {

    private static final double DELTA = 1e-10;

    // ====================================================
    // AbstractLossCalculator Interface Tests
    // ====================================================

    @Test
    public void testAbstractLossCalculator_calcLossesMethod() {
        // Test that implementations must have calcLosses
        AbstractLossCalculator calculator = new MockLossCalculator();

        // Should not throw
        calculator.calcLosses(1.0, 25.0, 1e-6);
    }

    @Test
    public void testAbstractLossCalculator_getTotalLossesMethod() {
        AbstractLossCalculator calculator = new MockLossCalculator();
        calculator.calcLosses(2.0, 25.0, 1e-6);

        double losses = calculator.getTotalLosses();

        assertEquals(4.0, losses, DELTA);  // 2.0 * 2.0 from mock
    }

    @Test
    public void testAbstractLossCalculator_MultipleCalls() {
        AbstractLossCalculator calculator = new MockLossCalculator();

        calculator.calcLosses(1.0, 25.0, 1e-6);
        double losses1 = calculator.getTotalLosses();

        calculator.calcLosses(2.0, 30.0, 1e-6);
        double losses2 = calculator.getTotalLosses();

        assertEquals(1.0, losses1, DELTA);
        assertEquals(4.0, losses2, DELTA);
    }

    @Test
    public void testAbstractLossCalculator_ZeroLosses() {
        AbstractLossCalculator calculator = new MockLossCalculator();
        calculator.calcLosses(0.0, 25.0, 1e-6);

        assertEquals(0.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testAbstractLossCalculator_NegativeLosses() {
        AbstractLossCalculator calculator = new MockLossCalculator();
        calculator.calcLosses(-2.0, 25.0, 1e-6);

        double losses = calculator.getTotalLosses();

        assertEquals(4.0, losses, DELTA);  // (-2.0)^2 = 4.0
    }

    // ====================================================
    // LossCalculationSplittable Interface Tests
    // ====================================================

    @Test
    public void testLossCalculationSplittable_SwitchingLoss() {
        LossCalculationSplittable splittable = new MockSplittable(5.0, 3.0);

        assertEquals(5.0, splittable.getSwitchingLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_ConductionLoss() {
        LossCalculationSplittable splittable = new MockSplittable(5.0, 3.0);

        assertEquals(3.0, splittable.getConductionLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_ZeroSwitching() {
        LossCalculationSplittable splittable = new MockSplittable(0.0, 10.0);

        assertEquals(0.0, splittable.getSwitchingLoss(), DELTA);
        assertEquals(10.0, splittable.getConductionLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_ZeroConduction() {
        LossCalculationSplittable splittable = new MockSplittable(10.0, 0.0);

        assertEquals(10.0, splittable.getSwitchingLoss(), DELTA);
        assertEquals(0.0, splittable.getConductionLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_BothZero() {
        LossCalculationSplittable splittable = new MockSplittable(0.0, 0.0);

        assertEquals(0.0, splittable.getSwitchingLoss(), DELTA);
        assertEquals(0.0, splittable.getConductionLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_NegativeValues() {
        LossCalculationSplittable splittable = new MockSplittable(-5.0, -3.0);

        assertEquals(-5.0, splittable.getSwitchingLoss(), DELTA);
        assertEquals(-3.0, splittable.getConductionLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_LargeValues() {
        LossCalculationSplittable splittable = new MockSplittable(1e10, 1e10);

        assertEquals(1e10, splittable.getSwitchingLoss(), 1e5);
        assertEquals(1e10, splittable.getConductionLoss(), 1e5);
    }

    @Test
    public void testLossCalculationSplittable_SmallValues() {
        LossCalculationSplittable splittable = new MockSplittable(1e-15, 1e-15);

        assertEquals(1e-15, splittable.getSwitchingLoss(), 1e-20);
        assertEquals(1e-15, splittable.getConductionLoss(), 1e-20);
    }

    @Test
    public void testLossCalculationSplittable_InfinityValues() {
        LossCalculationSplittable splittable = new MockSplittable(
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        );

        assertEquals(Double.POSITIVE_INFINITY, splittable.getSwitchingLoss(), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, splittable.getConductionLoss(), DELTA);
    }

    @Test
    public void testLossCalculationSplittable_NaN() {
        LossCalculationSplittable splittable = new MockSplittable(Double.NaN, Double.NaN);

        assertTrue(Double.isNaN(splittable.getSwitchingLoss()));
        assertTrue(Double.isNaN(splittable.getConductionLoss()));
    }

    @Test
    public void testLossCalculationSplittable_MixedValues() {
        LossCalculationSplittable splittable = new MockSplittable(100.0, 0.5);

        assertEquals(100.0, splittable.getSwitchingLoss(), DELTA);
        assertEquals(0.5, splittable.getConductionLoss(), DELTA);
    }

    // ====================================================
    // AbstractLossCalculatorFabric Interface Tests
    // ====================================================

    @Test
    public void testAbstractLossCalculatorFabric_CreatesCalculator() {
        AbstractLossCalculatorFabric fabric = new MockFabric();

        AbstractLossCalculator calculator = fabric.lossCalculatorFabric();

        assertNotNull(calculator);
    }

    @Test
    public void testAbstractLossCalculatorFabric_CreatesWorkingCalculator() {
        AbstractLossCalculatorFabric fabric = new MockFabric();
        AbstractLossCalculator calculator = fabric.lossCalculatorFabric();

        calculator.calcLosses(2.0, 25.0, 1e-6);

        assertEquals(4.0, calculator.getTotalLosses(), DELTA);
    }

    @Test
    public void testAbstractLossCalculatorFabric_MultipleCreations() {
        AbstractLossCalculatorFabric fabric = new MockFabric();

        AbstractLossCalculator calc1 = fabric.lossCalculatorFabric();
        AbstractLossCalculator calc2 = fabric.lossCalculatorFabric();

        // Different instances
        assertNotSame(calc1, calc2);
    }

    @Test
    public void testAbstractLossCalculatorFabric_IndependentInstances() {
        AbstractLossCalculatorFabric fabric = new MockFabric();

        AbstractLossCalculator calc1 = fabric.lossCalculatorFabric();
        AbstractLossCalculator calc2 = fabric.lossCalculatorFabric();

        calc1.calcLosses(1.0, 25.0, 1e-6);
        calc2.calcLosses(3.0, 25.0, 1e-6);

        assertEquals(1.0, calc1.getTotalLosses(), DELTA);
        assertEquals(9.0, calc2.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Combined Interface Tests
    // ====================================================

    @Test
    public void testCombined_CalculatorWithSplittable() {
        // Create a mock that implements both interfaces
        CombinedMock combined = new CombinedMock();

        // Test as AbstractLossCalculator
        combined.calcLosses(5.0, 25.0, 1e-6);
        assertEquals(50.0, combined.getTotalLosses(), DELTA);

        // Test as LossCalculationSplittable
        assertEquals(30.0, combined.getSwitchingLoss(), DELTA);
        assertEquals(20.0, combined.getConductionLoss(), DELTA);
    }

    // ====================================================
    // Mock Implementations
    // ====================================================

    private static class MockLossCalculator implements AbstractLossCalculator {
        private double totalLosses;

        @Override
        public void calcLosses(double current, double temperature, double deltaT) {
            totalLosses = current * current;  // I^2 formula
        }

        @Override
        public double getTotalLosses() {
            return totalLosses;
        }
    }

    private static class MockSplittable implements LossCalculationSplittable {
        private final double switchingLoss;
        private final double conductionLoss;

        MockSplittable(double switching, double conduction) {
            this.switchingLoss = switching;
            this.conductionLoss = conduction;
        }

        @Override
        public double getSwitchingLoss() {
            return switchingLoss;
        }

        @Override
        public double getConductionLoss() {
            return conductionLoss;
        }
    }

    private static class MockFabric implements AbstractLossCalculatorFabric {
        @Override
        public AbstractLossCalculator lossCalculatorFabric() {
            return new MockLossCalculator();
        }
    }

    private static class CombinedMock implements AbstractLossCalculator, LossCalculationSplittable {
        private double switchingLoss;
        private double conductionLoss;

        @Override
        public void calcLosses(double current, double temperature, double deltaT) {
            switchingLoss = current * 6;  // 5 -> 30
            conductionLoss = current * 4;  // 5 -> 20
        }

        @Override
        public double getTotalLosses() {
            return switchingLoss + conductionLoss;
        }

        @Override
        public double getSwitchingLoss() {
            return switchingLoss;
        }

        @Override
        public double getConductionLoss() {
            return conductionLoss;
        }
    }
}
