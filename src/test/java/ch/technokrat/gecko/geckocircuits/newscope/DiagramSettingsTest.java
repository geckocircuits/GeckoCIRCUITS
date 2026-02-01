/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.newscope;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for DiagramSettings - diagram display configuration.
 */
public class DiagramSettingsTest {

    private DiagramSettings settings;
    private static final double DELTA = 1e-9;

    @Before
    public void setUp() {
        settings = new DiagramSettings();
    }

    // ====================================================
    // Default Values Tests
    // ====================================================

    @Test
    public void testDefaultNameDiagram_IsEmpty() {
        assertNotNull("Default diagram name should not be null", settings.getNameDiagram());
        assertEquals("Default diagram name should be empty", "", settings.getNameDiagram());
    }

    @Test
    public void testDefaultWeightDiagram_Is0Point2() {
        assertEquals("Default diagram weight should be 0.2", 0.2, settings.getWeightDiagram(), DELTA);
    }

    // ====================================================
    // Diagram Name Tests
    // ====================================================

    @Test
    public void testSetNameDiagram_SimpleName() {
        settings.setNameDiagram("Test Diagram");
        assertEquals("Diagram name should be 'Test Diagram'", "Test Diagram", settings.getNameDiagram());
    }

    @Test
    public void testSetNameDiagram_EmptyString() {
        settings.setNameDiagram("Initial");
        settings.setNameDiagram("");
        assertEquals("Diagram name should be empty after reset", "", settings.getNameDiagram());
    }

    @Test
    public void testSetNameDiagram_LongName() {
        String longName = "This is a very long diagram name with many characters";
        settings.setNameDiagram(longName);
        assertEquals("Diagram name should handle long strings", longName, settings.getNameDiagram());
    }

    @Test
    public void testSetNameDiagram_SpecialCharacters() {
        String specialName = "Diagram_1-Test@#$%";
        settings.setNameDiagram(specialName);
        assertEquals("Diagram name should handle special characters", specialName, settings.getNameDiagram());
    }

    @Test
    public void testSetNameDiagram_WithSpaces() {
        String nameWithSpaces = "Diagram with multiple spaces";
        settings.setNameDiagram(nameWithSpaces);
        assertEquals("Diagram name should preserve spaces", nameWithSpaces, settings.getNameDiagram());
    }

    @Test
    public void testSetNameDiagram_NumericString() {
        settings.setNameDiagram("12345");
        assertEquals("Diagram name should accept numeric strings", "12345", settings.getNameDiagram());
    }

    @Test
    public void testSetNameDiagram_MultipleUpdates() {
        settings.setNameDiagram("First");
        assertEquals("First update", "First", settings.getNameDiagram());

        settings.setNameDiagram("Second");
        assertEquals("Second update", "Second", settings.getNameDiagram());

        settings.setNameDiagram("Third");
        assertEquals("Third update", "Third", settings.getNameDiagram());
    }

    // ====================================================
    // Diagram Weight Tests
    // ====================================================

    @Test
    public void testSetWeightDiagram_Minimum() {
        settings.setWeightDiagram(0.0);
        assertEquals("Weight should be 0.0", 0.0, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_Maximum() {
        settings.setWeightDiagram(1.0);
        assertEquals("Weight should be 1.0", 1.0, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_Quarter() {
        settings.setWeightDiagram(0.25);
        assertEquals("Weight should be 0.25", 0.25, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_Half() {
        settings.setWeightDiagram(0.5);
        assertEquals("Weight should be 0.5", 0.5, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_ThreeQuarters() {
        settings.setWeightDiagram(0.75);
        assertEquals("Weight should be 0.75", 0.75, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_DefaultAgain() {
        settings.setWeightDiagram(0.5);
        settings.setWeightDiagram(0.2);
        assertEquals("Weight should return to default 0.2", 0.2, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_PrecisionValue() {
        settings.setWeightDiagram(0.333333);
        assertEquals("Weight should handle precision values", 0.333333, settings.getWeightDiagram(), DELTA);
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test(expected = AssertionError.class)
    public void testSetWeightDiagram_BelowZero_Fails() {
        settings.setWeightDiagram(-0.1);
    }

    @Test(expected = AssertionError.class)
    public void testSetWeightDiagram_AboveOne_Fails() {
        settings.setWeightDiagram(1.1);
    }

    @Test
    public void testSetWeightDiagram_VerySmall() {
        settings.setWeightDiagram(1e-10);
        assertEquals("Weight should handle very small positive values", 1e-10, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testSetWeightDiagram_VeryCloseTo1() {
        settings.setWeightDiagram(0.9999999999);
        assertEquals("Weight should handle values very close to 1", 0.9999999999, settings.getWeightDiagram(), DELTA);
    }

    // ====================================================
    // State Persistence Tests
    // ====================================================

    @Test
    public void testSetNameDiagram_NullAssertion() {
        // This should fail with assertion since the javadoc asserts newName != null
        try {
            settings.setNameDiagram(null);
            fail("Should throw AssertionError for null name");
        } catch (AssertionError e) {
            // Expected
        }
    }

    @Test
    public void testMultipleStateChanges_Independent() {
        settings.setNameDiagram("MyDiagram");
        settings.setWeightDiagram(0.35);

        assertEquals("Name should be 'MyDiagram'", "MyDiagram", settings.getNameDiagram());
        assertEquals("Weight should be 0.35", 0.35, settings.getWeightDiagram(), DELTA);

        // Change one and verify other unchanged
        settings.setNameDiagram("NewName");
        assertEquals("New name should update", "NewName", settings.getNameDiagram());
        assertEquals("Weight should remain unchanged", 0.35, settings.getWeightDiagram(), DELTA);

        // Change weight and verify name unchanged
        settings.setWeightDiagram(0.6);
        assertEquals("Name should remain unchanged", "NewName", settings.getNameDiagram());
        assertEquals("Weight should update", 0.6, settings.getWeightDiagram(), DELTA);
    }

    @Test
    public void testMultipleInstances_Independent() {
        DiagramSettings settings2 = new DiagramSettings();

        settings.setNameDiagram("Diagram1");
        settings.setWeightDiagram(0.3);

        settings2.setNameDiagram("Diagram2");
        settings2.setWeightDiagram(0.7);

        assertEquals("First instance name", "Diagram1", settings.getNameDiagram());
        assertEquals("First instance weight", 0.3, settings.getWeightDiagram(), DELTA);
        assertEquals("Second instance name", "Diagram2", settings2.getNameDiagram());
        assertEquals("Second instance weight", 0.7, settings2.getWeightDiagram(), DELTA);
    }

    // ====================================================
    // Use Case Scenarios Tests
    // ====================================================

    @Test
    public void testScenario_EquallySizedDiagrams() {
        // Distribute 3 diagrams equally
        DiagramSettings d1 = new DiagramSettings();
        DiagramSettings d2 = new DiagramSettings();
        DiagramSettings d3 = new DiagramSettings();

        double weight = 1.0 / 3;
        d1.setWeightDiagram(weight);
        d2.setWeightDiagram(weight);
        d3.setWeightDiagram(weight);

        assertEquals("D1 weight", weight, d1.getWeightDiagram(), DELTA);
        assertEquals("D2 weight", weight, d2.getWeightDiagram(), DELTA);
        assertEquals("D3 weight", weight, d3.getWeightDiagram(), DELTA);
    }

    @Test
    public void testScenario_VariableSizedDiagrams() {
        // Create diagrams with proportional weights: 1:2:3 ratio
        DiagramSettings d1 = new DiagramSettings();
        DiagramSettings d2 = new DiagramSettings();
        DiagramSettings d3 = new DiagramSettings();

        d1.setNameDiagram("Small");
        d1.setWeightDiagram(1.0 / 6);

        d2.setNameDiagram("Medium");
        d2.setWeightDiagram(2.0 / 6);

        d3.setNameDiagram("Large");
        d3.setWeightDiagram(3.0 / 6);

        assertEquals("Small weight", 1.0 / 6, d1.getWeightDiagram(), DELTA);
        assertEquals("Medium weight", 2.0 / 6, d2.getWeightDiagram(), DELTA);
        assertEquals("Large weight", 3.0 / 6, d3.getWeightDiagram(), DELTA);

        // Verify names
        assertEquals("Small name", "Small", d1.getNameDiagram());
        assertEquals("Medium name", "Medium", d2.getNameDiagram());
        assertEquals("Large name", "Large", d3.getNameDiagram());
    }

    @Test
    public void testScenario_SwapDiagramNames() {
        DiagramSettings d1 = new DiagramSettings();
        DiagramSettings d2 = new DiagramSettings();

        d1.setNameDiagram("Voltage");
        d2.setNameDiagram("Current");

        // Swap names
        String temp = d1.getNameDiagram();
        d1.setNameDiagram(d2.getNameDiagram());
        d2.setNameDiagram(temp);

        assertEquals("D1 should have Current", "Current", d1.getNameDiagram());
        assertEquals("D2 should have Voltage", "Voltage", d2.getNameDiagram());
    }

    // ====================================================
    // Getter/Setter Consistency Tests
    // ====================================================

    @Test
    public void testGetSetConsistency_Name() {
        String[] testNames = {"A", "B", "Diagram1", "Test Diagram", ""};
        for (String name : testNames) {
            settings.setNameDiagram(name);
            assertEquals("Name consistency for '" + name + "'", name, settings.getNameDiagram());
        }
    }

    @Test
    public void testGetSetConsistency_Weight() {
        double[] testWeights = {0.0, 0.1, 0.2, 0.5, 0.9, 1.0};
        for (double weight : testWeights) {
            settings.setWeightDiagram(weight);
            assertEquals("Weight consistency for " + weight, weight, settings.getWeightDiagram(), DELTA);
        }
    }
}
