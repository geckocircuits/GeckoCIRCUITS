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
package ch.technokrat.gecko.geckocircuits.circuit;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit tests for CircuitLabel class.
 * Tests label management, priority handling, and basic state operations.
 */
public class CircuitLabelTest {

    private CircuitLabel _label;

    @Before
    public void setUp() {
        _label = new CircuitLabel();
    }

    @Test
    public void testConstructor_InitializesEmptyLabel() {
        assertEquals("", _label.getLabelString());
    }

    @Test
    public void testGetLabelString_InitiallyEmpty() {
        assertTrue(_label.getLabelString().isEmpty());
    }

    @Test
    public void testSetLabelWithoutUndo_SetsLabel() {
        _label.setLabelWithoutUndo("R1");
        assertEquals("R1", _label.getLabelString());
    }

    @Test
    public void testSetLabelWithoutUndo_OverwritesPreviousLabel() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelWithoutUndo("R2");
        assertEquals("R2", _label.getLabelString());
    }

    @Test
    public void testSetLabelWithoutUndo_NullAssertion() {
        try {
            _label.setLabelWithoutUndo(null);
            fail("Should throw AssertionError for null label");
        } catch (AssertionError e) {
            // Expected
        }
    }

    @Test
    public void testSetLabelWithoutUndo_EmptyString() {
        _label.setLabelWithoutUndo("");
        assertEquals("", _label.getLabelString());
    }

    @Test
    public void testSetLabelWithoutUndo_LongLabel() {
        String longLabel = "VeryLongLabelWithManyCharactersForTesting123456";
        _label.setLabelWithoutUndo(longLabel);
        assertEquals(longLabel, _label.getLabelString());
    }

    @Test
    public void testSetLabelWithoutUndo_SpecialCharacters() {
        String specialLabel = "R1_@#$%";
        _label.setLabelWithoutUndo(specialLabel);
        assertEquals(specialLabel, _label.getLabelString());
    }

    @Test
    public void testGetLabelPriority_InitiallyEmptyString() {
        assertEquals(LabelPriority.EMPTY_STRING, _label.getLabelPriority());
    }

    @Test
    public void testGetLabelPriority_EmptyStringReturnsEmptyStringPriority() {
        _label.setLabelWithoutUndo("");
        assertEquals(LabelPriority.EMPTY_STRING, _label.getLabelPriority());
    }

    @Test
    public void testGetLabelPriority_NonEmptyStringReturnsSetPriority() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelPriority(LabelPriority.LOW);
        assertEquals(LabelPriority.LOW, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelPriority_ChangesFromNormalToLow() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelPriority(LabelPriority.LOW);
        assertEquals(LabelPriority.LOW, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelPriority_ChangesFromNormalToForceName() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelPriority(LabelPriority.FORCE_NAME);
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelPriority_CanChangeMultipleTimes() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelPriority(LabelPriority.LOW);
        assertEquals(LabelPriority.LOW, _label.getLabelPriority());
        _label.setLabelPriority(LabelPriority.FORCE_NAME);
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());
    }

    @Test
    public void testClearPriority_ResetsToNormal() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelPriority(LabelPriority.FORCE_NAME);
        _label.clearPriority();
        assertEquals(LabelPriority.NORMAL, _label.getLabelPriority());
    }

    @Test
    public void testClearPriority_WhenEmptyStringIsSet() {
        _label.setLabelWithoutUndo("");
        _label.setLabelPriority(LabelPriority.NORMAL);
        _label.clearPriority();
        // When cleared and label is empty, should return EMPTY_STRING
        assertEquals(LabelPriority.EMPTY_STRING, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelFromUserDialog_SetsPriority() {
        _label.setLabelFromUserDialog("R1");
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelFromUserDialog_SetsLabelString() {
        _label.setLabelFromUserDialog("R1");
        assertEquals("R1", _label.getLabelString());
    }

    @Test
    public void testSetLabelFromUserDialog_SameLabelDoesNotChange() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelFromUserDialog("R1");
        assertEquals("R1", _label.getLabelString());
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelFromUserDialog_ChangesLabel() {
        _label.setLabelWithoutUndo("R1");
        _label.setLabelFromUserDialog("R2");
        assertEquals("R2", _label.getLabelString());
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());
    }

    @Test
    public void testMultipleOperationsSequence() {
        // Start empty
        assertEquals("", _label.getLabelString());

        // Set label without undo
        _label.setLabelWithoutUndo("L1");
        assertEquals("L1", _label.getLabelString());
        assertEquals(LabelPriority.NORMAL, _label.getLabelPriority());

        // Set priority
        _label.setLabelPriority(LabelPriority.LOW);
        assertEquals(LabelPriority.LOW, _label.getLabelPriority());

        // Change label via user dialog
        _label.setLabelFromUserDialog("L2");
        assertEquals("L2", _label.getLabelString());
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());

        // Clear priority
        _label.clearPriority();
        assertEquals("L2", _label.getLabelString());
        assertEquals(LabelPriority.NORMAL, _label.getLabelPriority());
    }

    @Test
    public void testLabelPriorityTransitions_EmptyToNonEmpty() {
        // Start with empty label
        _label.setLabelWithoutUndo("");
        assertEquals(LabelPriority.EMPTY_STRING, _label.getLabelPriority());

        // Set a non-empty label
        _label.setLabelWithoutUndo("R1");
        assertEquals(LabelPriority.NORMAL, _label.getLabelPriority());
    }

    @Test
    public void testSetLabelPriority_WithDifferentPriorities() {
        _label.setLabelWithoutUndo("R1");
        for (LabelPriority priority : LabelPriority.values()) {
            _label.setLabelPriority(priority);
            assertEquals(priority, _label.getLabelPriority());
        }
    }

    @Test
    public void testSetLabelFromUserDialog_WithEmptyString() {
        _label.setLabelFromUserDialog("");
        assertEquals("", _label.getLabelString());
        assertEquals(LabelPriority.FORCE_NAME, _label.getLabelPriority());
    }

    @Test
    public void testLabelWithNumbers() {
        String labelWithNumbers = "R123";
        _label.setLabelWithoutUndo(labelWithNumbers);
        assertEquals(labelWithNumbers, _label.getLabelString());
    }

    @Test
    public void testLabelWithUnderscores() {
        String labelWithUnderscores = "R_1_2_3";
        _label.setLabelWithoutUndo(labelWithUnderscores);
        assertEquals(labelWithUnderscores, _label.getLabelString());
    }

    @Test
    public void testMultipleObjectInstances_AreIndependent() {
        CircuitLabel label1 = new CircuitLabel();
        CircuitLabel label2 = new CircuitLabel();

        label1.setLabelWithoutUndo("L1");
        label2.setLabelWithoutUndo("L2");

        assertEquals("L1", label1.getLabelString());
        assertEquals("L2", label2.getLabelString());
    }

    @Test
    public void testEdgeCaseUnicodeCharacters() {
        String unicodeLabel = "R_α_β_γ";
        _label.setLabelWithoutUndo(unicodeLabel);
        assertEquals(unicodeLabel, _label.getLabelString());
    }

    @Test
    public void testLabelConsistency_AfterMultipleSetOperations() {
        String[] labels = {"R1", "C1", "L1", "V1", "I1"};
        for (String label : labels) {
            _label.setLabelWithoutUndo(label);
            assertEquals(label, _label.getLabelString());
        }
    }
}
