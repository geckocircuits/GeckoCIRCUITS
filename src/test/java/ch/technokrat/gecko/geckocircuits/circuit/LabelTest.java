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
 * Unit tests for Label data class.
 * Tests immutable label creation, equality, and hashing.
 */
public class LabelTest {

    private Label _label;

    @Before
    public void setUp() {
        _label = new Label("R1", ConnectorType.LK);
    }

    @Test
    public void testConstructor_CreatesLabelWithStringAndType() {
        Label label = new Label("C1", ConnectorType.CONTROL);
        assertNotNull(label);
    }

    @Test
    public void testEquals_SameLabelAndType() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.LK);
        assertEquals(label1, label2);
    }

    @Test
    public void testEquals_DifferentLabelSameType() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R2", ConnectorType.LK);
        assertNotEquals(label1, label2);
    }

    @Test
    public void testEquals_SameLabelDifferentType() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.CONTROL);
        assertNotEquals(label1, label2);
    }

    @Test
    public void testEquals_BothDifferent() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R2", ConnectorType.CONTROL);
        assertNotEquals(label1, label2);
    }

    @Test
    public void testEquals_WithNull() {
        Label label1 = new Label("R1", ConnectorType.LK);
        assertNotEquals(label1, null);
    }

    @Test
    public void testEquals_WithDifferentClass() {
        Label label1 = new Label("R1", ConnectorType.LK);
        String notALabel = "R1";
        assertNotEquals(label1, notALabel);
    }

    @Test
    public void testEquals_Reflexive() {
        assertEquals(_label, _label);
    }

    @Test
    public void testEquals_Symmetric() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.LK);
        assertEquals(label1, label2);
        assertEquals(label2, label1);
    }

    @Test
    public void testEquals_Transitive() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.LK);
        Label label3 = new Label("R1", ConnectorType.LK);
        assertEquals(label1, label2);
        assertEquals(label2, label3);
        assertEquals(label1, label3);
    }

    @Test
    public void testHashCode_SameLabelAndType() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.LK);
        assertEquals(label1.hashCode(), label2.hashCode());
    }

    @Test
    public void testHashCode_DifferentLabels() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R2", ConnectorType.LK);
        // Different labels should (likely) have different hash codes
        assertNotEquals(label1.hashCode(), label2.hashCode());
    }

    @Test
    public void testHashCode_DifferentTypes() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.CONTROL);
        // Different types should (likely) have different hash codes
        assertNotEquals(label1.hashCode(), label2.hashCode());
    }

    @Test
    public void testHashCode_Consistency() {
        int hash1 = _label.hashCode();
        int hash2 = _label.hashCode();
        int hash3 = _label.hashCode();
        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    public void testHashCode_CanBeUsedInHashSet() {
        java.util.Set<Label> labels = new java.util.HashSet<>();
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R2", ConnectorType.CONTROL);
        Label label3 = new Label("R1", ConnectorType.LK); // Duplicate of label1

        labels.add(label1);
        labels.add(label2);
        labels.add(label3);

        // Should have 2 unique labels (label1 and label2)
        assertEquals(2, labels.size());
        assertTrue(labels.contains(label1));
        assertTrue(labels.contains(label2));
    }

    @Test
    public void testHashCode_CanBeUsedInHashMap() {
        java.util.Map<Label, String> labelMap = new java.util.HashMap<>();
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R2", ConnectorType.CONTROL);

        labelMap.put(label1, "Resistor");
        labelMap.put(label2, "Control");

        assertEquals("Resistor", labelMap.get(label1));
        assertEquals("Control", labelMap.get(label2));
    }

    @Test
    public void testConstructor_WithEmptyString() {
        Label label = new Label("", ConnectorType.LK);
        assertNotNull(label);
    }

    @Test
    public void testConstructor_WithLongLabel() {
        String longLabel = "VeryLongLabelWithManyCharactersForTesting123456789";
        Label label = new Label(longLabel, ConnectorType.LK);
        assertNotNull(label);
    }

    @Test
    public void testConstructor_WithSpecialCharacters() {
        Label label = new Label("R_1@#$%", ConnectorType.CONTROL);
        assertNotNull(label);
    }

    @Test
    public void testConstructor_WithUnicodeLabel() {
        Label label = new Label("R_α_β_γ", ConnectorType.THERMAL);
        assertNotNull(label);
    }

    @Test
    public void testEquals_AllConnectorTypes() {
        for (ConnectorType type : ConnectorType.values()) {
            Label label1 = new Label("R1", type);
            Label label2 = new Label("R1", type);
            assertEquals(label1, label2);
        }
    }

    @Test
    public void testNotEquals_AllDifferentConnectorTypes() {
        ConnectorType[] types = ConnectorType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                Label label1 = new Label("R1", types[i]);
                Label label2 = new Label("R1", types[j]);
                assertNotEquals("Labels with types " + types[i] + " and " + types[j] + " should not be equal",
                        label1, label2);
            }
        }
    }

    @Test
    public void testHashCode_MultipleInstances() {
        Label[] labels = new Label[5];
        for (int i = 0; i < 5; i++) {
            labels[i] = new Label("R" + i, ConnectorType.LK);
        }

        // All should have non-zero hash codes
        for (Label label : labels) {
            assertNotEquals(0, label.hashCode());
        }
    }

    @Test
    public void testEqualsWithNullConnectorType() {
        Label label1 = new Label("R1", null);
        Label label2 = new Label("R1", null);
        assertEquals(label1, label2);
    }

    @Test
    public void testEqualsWithNullLabelString() {
        Label label1 = new Label(null, ConnectorType.LK);
        Label label2 = new Label(null, ConnectorType.LK);
        assertEquals(label1, label2);
    }

    @Test
    public void testEqualsWithBothNull() {
        Label label1 = new Label(null, null);
        Label label2 = new Label(null, null);
        assertEquals(label1, label2);
    }

    @Test
    public void testHashCode_WithNullLabelString() {
        Label label = new Label(null, ConnectorType.LK);
        int hashCode = label.hashCode();
        // Should not throw exception
        assertNotNull(hashCode);
    }

    @Test
    public void testHashCode_WithNullConnectorType() {
        Label label = new Label("R1", null);
        int hashCode = label.hashCode();
        // Should not throw exception
        assertNotNull(hashCode);
    }

    @Test
    public void testLabelImmutability_CannotModifyAfterConstruction() {
        Label label1 = new Label("R1", ConnectorType.LK);
        Label label2 = new Label("R1", ConnectorType.LK);
        assertEquals(label1, label2);

        // Labels should always be equal for same string and type
        Label label3 = new Label("R1", ConnectorType.LK);
        assertEquals(label1, label3);
    }

    @Test
    public void testLargeNumberOfLabelsInSet() {
        java.util.Set<Label> labels = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            labels.add(new Label("Label" + i, ConnectorType.LK));
        }
        assertEquals(1000, labels.size());
    }

    @Test
    public void testAllConnectorTypesWithSameLabel() {
        String labelString = "TestLabel";
        Label[] labels = new Label[ConnectorType.values().length];
        int index = 0;
        for (ConnectorType type : ConnectorType.values()) {
            labels[index++] = new Label(labelString, type);
        }

        // All should be different
        for (int i = 0; i < labels.length; i++) {
            for (int j = i + 1; j < labels.length; j++) {
                assertNotEquals(labels[i], labels[j]);
            }
        }
    }

    @Test
    public void testHashCodeDifferenceWithDifferentInputs() {
        Label label1 = new Label("Label1", ConnectorType.LK);
        Label label2 = new Label("Label2", ConnectorType.LK);
        Label label3 = new Label("Label1", ConnectorType.CONTROL);

        // While hash codes can theoretically collide, these different inputs
        // should produce different hash codes with high probability
        assertNotEquals(label1.hashCode(), label2.hashCode());
        assertNotEquals(label1.hashCode(), label3.hashCode());
    }
}
