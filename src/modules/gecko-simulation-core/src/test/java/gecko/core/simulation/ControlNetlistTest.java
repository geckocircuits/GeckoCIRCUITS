/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.simulation;

import gecko.core.control.calculators.AbstractControlCalculatable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ControlNetlist.
 */
class ControlNetlistTest {

    private ControlNetlist netlist;

    @BeforeEach
    void setUp() {
        netlist = new ControlNetlist();
    }

    // ========== Tests for createEmpty() factory method ==========

    @Test
    void createEmpty_returnsNetlistWith0Calculators() {
        ControlNetlist empty = ControlNetlist.createEmpty();

        assertEquals(0, empty.getCalculatorCount());
        assertFalse(empty.hasCalculators());
    }

    @Test
    void createEmpty_returnsNewInstance() {
        ControlNetlist empty1 = ControlNetlist.createEmpty();
        ControlNetlist empty2 = ControlNetlist.createEmpty();

        assertNotSame(empty1, empty2);
        assertEquals(empty1.getCalculatorCount(), empty2.getCalculatorCount());
    }

    // ========== Tests for default constructor ==========

    @Test
    void constructor_createsEmptyNetlist() {
        assertEquals(0, netlist.getCalculatorCount());
        assertEquals(0.0, netlist.getSimulationTime(), 0.0001);
        assertFalse(netlist.hasCalculators());
    }

    // ========== Tests for setSortedCalculators() ==========

    @Test
    void setSortedCalculators_storesCalculators() {
        List<AbstractControlCalculatable> calculators = new ArrayList<>();
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        calculators.add(calc1);
        calculators.add(calc2);

        netlist.setSortedCalculators(calculators);

        assertEquals(2, netlist.getCalculatorCount());
        assertTrue(netlist.hasCalculators());
    }

    @Test
    void setSortedCalculators_emptyList() {
        List<AbstractControlCalculatable> emptyList = new ArrayList<>();

        netlist.setSortedCalculators(emptyList);

        assertEquals(0, netlist.getCalculatorCount());
        assertFalse(netlist.hasCalculators());
    }

    @Test
    void setSortedCalculators_nullThrowsException() {
        assertThrows(NullPointerException.class, () -> netlist.setSortedCalculators(null));
    }

    @Test
    void setSortedCalculators_canBeCalledMultipleTimes() {
        List<AbstractControlCalculatable> first = new ArrayList<>();
        first.add(new MockCalculator(1, 1));
        netlist.setSortedCalculators(first);
        assertEquals(1, netlist.getCalculatorCount());

        List<AbstractControlCalculatable> second = new ArrayList<>();
        second.add(new MockCalculator(1, 1));
        second.add(new MockCalculator(1, 1));
        netlist.setSortedCalculators(second);
        assertEquals(2, netlist.getCalculatorCount());
    }

    // ========== Tests for executeTimeStep() ==========

    @Test
    void executeTimeStep_doesNotThrowWhenEmpty() {
        assertDoesNotThrow(() -> netlist.executeTimeStep(1e-6, 0.0));
    }

    @Test
    void executeTimeStep_updatesSimulationTime() {
        netlist = ControlNetlist.createEmpty();

        netlist.executeTimeStep(1e-6, 0.005);

        assertEquals(0.005, netlist.getSimulationTime(), 0.00001);
    }

    @Test
    void executeTimeStep_callsCalculators() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        List<AbstractControlCalculatable> calculators = List.of(calc1, calc2);
        netlist.setSortedCalculators(calculators);

        netlist.executeTimeStep(1e-6, 0.001);

        assertEquals(1, calc1.callCount);
        assertEquals(1, calc2.callCount);
    }

    @Test
    void executeTimeStep_setsAbstractControlCalculatableStaticTime() {
        MockCalculator calc = new MockCalculator(1, 1);
        List<AbstractControlCalculatable> calculators = List.of(calc);
        netlist.setSortedCalculators(calculators);

        netlist.executeTimeStep(1e-6, 0.002);

        assertEquals(0.002, AbstractControlCalculatable._time, 0.00001);
    }

    @Test
    void executeTimeStep_callsCalculatorsInOrder() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        List<AbstractControlCalculatable> calculators = List.of(calc1, calc2);
        netlist.setSortedCalculators(calculators);

        netlist.executeTimeStep(1e-6, 0.001);

        assertTrue(calc1.callCount > 0);
        assertTrue(calc2.callCount > 0);
    }

    @Test
    void executeTimeStep_multipleTimeSteps() {
        MockCalculator calc = new MockCalculator(1, 1);
        List<AbstractControlCalculatable> calculators = List.of(calc);
        netlist.setSortedCalculators(calculators);

        for (int i = 0; i < 5; i++) {
            netlist.executeTimeStep(1e-6, i * 1e-6);
        }

        assertEquals(5, calc.callCount);
    }

    // ========== Tests for getCalculatorCount() ==========

    @Test
    void getCalculatorCount_emptyNetlist() {
        assertEquals(0, netlist.getCalculatorCount());
    }

    @Test
    void getCalculatorCount_multipleCalculators() {
        List<AbstractControlCalculatable> calculators = List.of(
            new MockCalculator(1, 1),
            new MockCalculator(1, 1),
            new MockCalculator(1, 1)
        );
        netlist.setSortedCalculators(calculators);

        assertEquals(3, netlist.getCalculatorCount());
    }

    // ========== Tests for hasCalculators() ==========

    @Test
    void hasCalculators_emptyNetlist() {
        assertFalse(netlist.hasCalculators());
    }

    @Test
    void hasCalculators_withCalculators() {
        netlist.setSortedCalculators(List.of(new MockCalculator(1, 1)));

        assertTrue(netlist.hasCalculators());
    }

    @Test
    void hasCalculators_afterClearingCalculators() {
        netlist.setSortedCalculators(List.of(new MockCalculator(1, 1)));
        assertTrue(netlist.hasCalculators());

        netlist.setSortedCalculators(new ArrayList<>());
        assertFalse(netlist.hasCalculators());
    }

    // ========== Tests for getSortedCalculators() ==========

    @Test
    void getSortedCalculators_returnsArray() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        netlist.setSortedCalculators(List.of(calc1, calc2));

        AbstractControlCalculatable[] result = netlist.getSortedCalculators();

        assertEquals(2, result.length);
        assertSame(calc1, result[0]);
        assertSame(calc2, result[1]);
    }

    @Test
    void getSortedCalculators_returnsCopy() {
        MockCalculator calc = new MockCalculator(1, 1);
        netlist.setSortedCalculators(List.of(calc));

        AbstractControlCalculatable[] array1 = netlist.getSortedCalculators();
        AbstractControlCalculatable[] array2 = netlist.getSortedCalculators();

        assertNotSame(array1, array2);
        assertSame(array1[0], array2[0]);
    }

    @Test
    void getSortedCalculators_emptyArray() {
        AbstractControlCalculatable[] result = netlist.getSortedCalculators();

        assertEquals(0, result.length);
    }

    // ========== Tests for getCalculatorsList() ==========

    @Test
    void getCalculatorsList_returnsUnmodifiableList() {
        MockCalculator calc = new MockCalculator(1, 1);
        netlist.setSortedCalculators(List.of(calc));

        List<AbstractControlCalculatable> list = netlist.getCalculatorsList();

        assertEquals(1, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add(new MockCalculator(1, 1)));
    }

    @Test
    void getCalculatorsList_containsCorrectCalculators() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        netlist.setSortedCalculators(List.of(calc1, calc2));

        List<AbstractControlCalculatable> list = netlist.getCalculatorsList();

        assertEquals(2, list.size());
        assertSame(calc1, list.get(0));
        assertSame(calc2, list.get(1));
    }

    // ========== Tests for fromOrderedCalculators() with List ==========

    @Test
    void fromOrderedCalculators_withList_storesCalculators() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        List<AbstractControlCalculatable> calculators = List.of(calc1, calc2);

        ControlNetlist result = ControlNetlist.fromOrderedCalculators(calculators);

        assertEquals(2, result.getCalculatorCount());
        assertSame(calc1, result.getSortedCalculators()[0]);
        assertSame(calc2, result.getSortedCalculators()[1]);
    }

    @Test
    void fromOrderedCalculators_withList_nullThrowsException() {
        assertThrows(NullPointerException.class, () -> ControlNetlist.fromOrderedCalculators((List<AbstractControlCalculatable>) null));
    }

    @Test
    void fromOrderedCalculators_withList_emptyList() {
        List<AbstractControlCalculatable> empty = new ArrayList<>();

        ControlNetlist result = ControlNetlist.fromOrderedCalculators(empty);

        assertEquals(0, result.getCalculatorCount());
        assertFalse(result.hasCalculators());
    }

    @Test
    void fromOrderedCalculators_withList_createsNewInstance() {
        List<AbstractControlCalculatable> calculators = List.of(new MockCalculator(1, 1));

        ControlNetlist result1 = ControlNetlist.fromOrderedCalculators(calculators);
        ControlNetlist result2 = ControlNetlist.fromOrderedCalculators(calculators);

        assertNotSame(result1, result2);
    }

    // ========== Tests for fromOrderedCalculators() with Array ==========

    @Test
    void fromOrderedCalculators_withArray_storesCalculators() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        MockCalculator calc2 = new MockCalculator(1, 1);
        AbstractControlCalculatable[] calculators = {calc1, calc2};

        ControlNetlist result = ControlNetlist.fromOrderedCalculators(calculators);

        assertEquals(2, result.getCalculatorCount());
        assertSame(calc1, result.getSortedCalculators()[0]);
        assertSame(calc2, result.getSortedCalculators()[1]);
    }

    @Test
    void fromOrderedCalculators_withArray_nullThrowsException() {
        assertThrows(NullPointerException.class, () -> ControlNetlist.fromOrderedCalculators((AbstractControlCalculatable[]) null));
    }

    @Test
    void fromOrderedCalculators_withArray_emptyArray() {
        AbstractControlCalculatable[] empty = new AbstractControlCalculatable[0];

        ControlNetlist result = ControlNetlist.fromOrderedCalculators(empty);

        assertEquals(0, result.getCalculatorCount());
        assertFalse(result.hasCalculators());
    }

    @Test
    void fromOrderedCalculators_withArray_createsCopy() {
        MockCalculator calc1 = new MockCalculator(1, 1);
        AbstractControlCalculatable[] original = {calc1};

        ControlNetlist result = ControlNetlist.fromOrderedCalculators(original);
        // Modifying original array should not affect netlist
        original[0] = null;

        assertSame(calc1, result.getSortedCalculators()[0]);
    }

    // ========== Mock Calculator for testing ==========

    /**
     * Mock calculator for testing that tracks call count and time steps.
     */
    private static class MockCalculator extends AbstractControlCalculatable {
        int callCount = 0;
        double lastDt = 0;

        MockCalculator(int inputs, int outputs) {
            super(inputs, outputs);
        }

        @Override
        public void berechneYOUT(double deltaT) {
            callCount++;
            lastDt = deltaT;
        }
    }
}
