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
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for OptimizerParameterData - global parameter data management.
 * Tests parameter storage, retrieval, and mutation tracking.
 */
public class OptimizerParameterDataTest {

    private OptimizerParameterData parameterData;
    private static final double DELTA = 1e-9;

    @Before
    public void setUp() {
        parameterData = new OptimizerParameterData();
    }

    // ====================================================
    // Initialization Tests
    // ====================================================

    @Test
    public void testClearAndInitialize_EmptyLists() {
        parameterData.clearAndInitializeWithoutUndo(new ArrayList<>(), new ArrayList<>());
        assertEquals(0, parameterData.getNameOpt().size());
        assertEquals(0, parameterData.getValueOpt().size());
    }

    @Test
    public void testClearAndInitialize_WithData() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param1");
        names.add("param2");
        values.add(1.5);
        values.add(2.7);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        assertEquals(2, parameterData.getNameOpt().size());
        assertEquals(2, parameterData.getValueOpt().size());
    }

    @Test
    public void testClearAndInitialize_NullNames() {
        parameterData.clearAndInitializeWithoutUndo(null, new ArrayList<>());
        assertEquals(0, parameterData.getNameOpt().size());
    }

    @Test
    public void testClearAndInitialize_NullValues() {
        List<String> names = new ArrayList<>();
        names.add("param1");
        parameterData.clearAndInitializeWithoutUndo(names, null);
        assertEquals(0, parameterData.getNameOpt().size());
    }

    @Test
    public void testClearAndInitialize_BothNull() {
        parameterData.clearAndInitializeWithoutUndo(null, null);
        assertEquals(0, parameterData.getNameOpt().size());
        assertEquals(0, parameterData.getValueOpt().size());
    }

    @Test
    public void testClearAndInitialize_SizeMismatch_FewerValues() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param1");
        names.add("param2");
        names.add("param3");
        values.add(1.0);
        values.add(2.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        // Should initialize with minimum size (2)
        assertEquals(2, parameterData.getNameOpt().size());
        assertEquals(2, parameterData.getValueOpt().size());
    }

    @Test
    public void testClearAndInitialize_SizeMismatch_FewerNames() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param1");
        values.add(1.0);
        values.add(2.0);
        values.add(3.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        // Should initialize with minimum size (1)
        assertEquals(1, parameterData.getNameOpt().size());
        assertEquals(1, parameterData.getValueOpt().size());
    }

    // ====================================================
    // Get Methods Tests
    // ====================================================

    @Test
    public void testGetNameOpt() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("resistance");
        names.add("capacitance");
        values.add(1000.0);
        values.add(1e-6);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        List<String> retrievedNames = parameterData.getNameOpt();
        assertEquals(2, retrievedNames.size());
        assertTrue(retrievedNames.contains("resistance"));
        assertTrue(retrievedNames.contains("capacitance"));
    }

    @Test
    public void testGetValueOpt() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param1");
        names.add("param2");
        values.add(5.5);
        values.add(10.2);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        List<Double> retrievedValues = parameterData.getValueOpt();
        assertEquals(2, retrievedValues.size());
        assertEquals(5.5, retrievedValues.get(0), DELTA);
        assertEquals(10.2, retrievedValues.get(1), DELTA);
    }

    @Test
    public void testGetNumberFromName_Success() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("voltage");
        values.add(12.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("voltage");
        assertEquals(12.0, value, DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNumberFromName_NotFound() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("voltage");
        values.add(12.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        parameterData.getNumberFromName("current");  // Should throw
    }

    // ====================================================
    // Set Methods Tests
    // ====================================================

    @Test
    public void testSetNumberFromName() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("temp");
        values.add(25.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        parameterData.setNumberFromName("temp", 50.0);

        assertEquals(50.0, parameterData.getNumberFromName("temp"), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNumberFromName_NotFound() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("existing");
        values.add(1.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        parameterData.setNumberFromName("nonexisting", 2.0);  // Should throw
    }

    @Test
    public void testSetNumberFromName_PreservesOtherValues() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param1");
        names.add("param2");
        values.add(1.0);
        values.add(2.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        parameterData.setNumberFromName("param1", 10.0);

        assertEquals(10.0, parameterData.getNumberFromName("param1"), DELTA);
        assertEquals(2.0, parameterData.getNumberFromName("param2"), DELTA);
    }

    // ====================================================
    // Clear Tests
    // ====================================================

    @Test
    public void testClear() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param1");
        names.add("param2");
        values.add(1.0);
        values.add(2.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);
        assertEquals(2, parameterData.getNameOpt().size());

        parameterData.clear();
        assertEquals(0, parameterData.getNameOpt().size());
        assertEquals(0, parameterData.getValueOpt().size());
    }

    // ====================================================
    // Order Preservation Tests
    // ====================================================

    @Test
    public void testOrderPreservation() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("first");
        names.add("second");
        names.add("third");
        values.add(1.0);
        values.add(2.0);
        values.add(3.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        List<String> retrievedNames = parameterData.getNameOpt();
        List<Double> retrievedValues = parameterData.getValueOpt();

        assertEquals("first", retrievedNames.get(0));
        assertEquals("second", retrievedNames.get(1));
        assertEquals("third", retrievedNames.get(2));
        assertEquals(1.0, retrievedValues.get(0), DELTA);
        assertEquals(2.0, retrievedValues.get(1), DELTA);
        assertEquals(3.0, retrievedValues.get(2), DELTA);
    }

    // ====================================================
    // Special Values Tests
    // ====================================================

    @Test
    public void testSetAndGet_ZeroValue() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("zero_param");
        values.add(0.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("zero_param");
        assertEquals(0.0, value, DELTA);
    }

    @Test
    public void testSetAndGet_NegativeValue() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("negative");
        values.add(-100.5);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("negative");
        assertEquals(-100.5, value, DELTA);
    }

    @Test
    public void testSetAndGet_VerySmallValue() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("tiny");
        values.add(1e-12);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("tiny");
        assertEquals(1e-12, value, DELTA);
    }

    @Test
    public void testSetAndGet_VeryLargeValue() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("huge");
        values.add(1e12);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("huge");
        assertEquals(1e12, value, 1e6);
    }

    // ====================================================
    // Multiple Set/Get Sequence Tests
    // ====================================================

    @Test
    public void testMultipleSetOperations() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param");
        values.add(1.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        parameterData.setNumberFromName("param", 2.0);
        assertEquals(2.0, parameterData.getNumberFromName("param"), DELTA);

        parameterData.setNumberFromName("param", 3.0);
        assertEquals(3.0, parameterData.getNumberFromName("param"), DELTA);

        parameterData.setNumberFromName("param", 4.0);
        assertEquals(4.0, parameterData.getNumberFromName("param"), DELTA);
    }

    @Test
    public void testClearFollowedByNewData() {
        List<String> names1 = new ArrayList<>();
        List<Double> values1 = new ArrayList<>();
        names1.add("old");
        values1.add(1.0);

        parameterData.clearAndInitializeWithoutUndo(names1, values1);
        assertEquals(1, parameterData.getNameOpt().size());

        parameterData.clear();
        assertEquals(0, parameterData.getNameOpt().size());

        List<String> names2 = new ArrayList<>();
        List<Double> values2 = new ArrayList<>();
        names2.add("new");
        values2.add(2.0);

        parameterData.clearAndInitializeWithoutUndo(names2, values2);
        assertEquals(1, parameterData.getNameOpt().size());
        assertEquals(2.0, parameterData.getNumberFromName("new"), DELTA);
    }

    // ====================================================
    // Data Type Consistency Tests
    // ====================================================

    @Test
    public void testDataTypeConsistency_AllDoubles() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        // Test with various double formats
        names.add("int_as_double");
        names.add("float_as_double");
        names.add("exp_as_double");

        values.add(42.0);
        values.add(3.14159);
        values.add(2.5e-3);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        assertEquals(42.0, parameterData.getNumberFromName("int_as_double"), DELTA);
        assertEquals(3.14159, parameterData.getNumberFromName("float_as_double"), 0.00001);
        assertEquals(2.5e-3, parameterData.getNumberFromName("exp_as_double"), DELTA);
    }

    // ====================================================
    // Empty String Parameter Name Tests
    // ====================================================

    @Test
    public void testEmptyStringParameterName() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("");
        values.add(1.0);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("");
        assertEquals(1.0, value, DELTA);
    }

    // ====================================================
    // Whitespace in Parameter Names
    // ====================================================

    @Test
    public void testParameterNameWithSpaces() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("my parameter name");
        values.add(5.5);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("my parameter name");
        assertEquals(5.5, value, DELTA);
    }

    @Test
    public void testParameterNameWithSpecialCharacters() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        names.add("param_with-special$chars#");
        values.add(7.7);

        parameterData.clearAndInitializeWithoutUndo(names, values);

        double value = parameterData.getNumberFromName("param_with-special$chars#");
        assertEquals(7.7, value, DELTA);
    }

    // ====================================================
    // Large Dataset Tests
    // ====================================================

    @Test
    public void testLargeDataset() {
        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        int size = 1000;
        for (int i = 0; i < size; i++) {
            names.add("param_" + i);
            values.add((double) i);
        }

        parameterData.clearAndInitializeWithoutUndo(names, values);

        assertEquals(size, parameterData.getNameOpt().size());
        assertEquals(size, parameterData.getValueOpt().size());

        assertEquals(0.0, parameterData.getNumberFromName("param_0"), DELTA);
        assertEquals(500.0, parameterData.getNumberFromName("param_500"), DELTA);
        assertEquals(999.0, parameterData.getNumberFromName("param_999"), DELTA);
    }
}
