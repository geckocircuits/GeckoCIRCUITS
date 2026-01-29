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
package ch.technokrat.modelviewcontrol;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for ModelMVC - Generic MVC model with optional description and undo/redo support.
 *
 * Tests cover:
 * - Model initialization with and without description
 * - Value getting and setting with undo tracking
 * - toString() representation with/without description
 * - Serialization support
 */
public class ModelMVCTest {

    private ModelMVC<String> stringModel;
    private ModelMVC<Integer> intModel;
    private ModelMVC<Double> doubleModel;
    private ModelMVC<Boolean> boolModel;

    @Before
    public void setUp() {
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        AbstractUndoGenericModel.globalEventListeners.clear();

        stringModel = new ModelMVC<>("test");
        intModel = new ModelMVC<>(42);
        doubleModel = new ModelMVC<>(3.14);
        boolModel = new ModelMVC<>(true);
    }

    @After
    public void tearDown() {
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        AbstractUndoGenericModel.globalEventListeners.clear();
    }

    @Test
    public void testInitializationWithValue() {
        assertEquals("test", stringModel.getValue());
    }

    @Test
    public void testInitializationWithIntegerValue() {
        assertEquals(Integer.valueOf(42), intModel.getValue());
    }

    @Test
    public void testInitializationWithDoubleValue() {
        assertEquals(3.14, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testInitializationWithBooleanValue() {
        assertEquals(true, boolModel.getValue());
    }

    @Test
    public void testInitializationWithDescription() {
        ModelMVC<String> model = new ModelMVC<>("value", "My Description");
        assertEquals("value", model.getValue());
        assertEquals("My Description", model.toString());
    }

    @Test
    public void testInitializationWithDescriptionObject() {
        Object desc = new Object() {
            @Override
            public String toString() {
                return "CustomDescription";
            }
        };
        ModelMVC<String> model = new ModelMVC<>("value", desc);
        assertEquals("CustomDescription", model.toString());
    }

    @Test
    public void testToStringWithoutDescription() {
        String toStr = stringModel.toString();
        assertTrue(toStr.contains(stringModel.getClass().getName()) || toStr.contains("ModelMVC"));
    }

    @Test
    public void testToStringWithDescription() {
        ModelMVC<String> model = new ModelMVC<>("value", "TestDescription");
        assertEquals("TestDescription", model.toString());
    }

    @Test
    public void testToStringWithNullDescription() {
        ModelMVC<String> model = new ModelMVC<>("value", null);
        String toStr = model.toString();
        assertTrue(toStr.contains("ModelMVC") || toStr.contains("test"));
    }

    @Test
    public void testSetValue() {
        stringModel.setValue("new value");
        assertEquals("new value", stringModel.getValue());
    }

    @Test
    public void testSetValueInteger() {
        intModel.setValue(100);
        assertEquals(Integer.valueOf(100), intModel.getValue());
    }

    @Test
    public void testSetValueDouble() {
        doubleModel.setValue(2.71);
        assertEquals(2.71, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testSetValueBoolean() {
        boolModel.setValue(false);
        assertEquals(false, boolModel.getValue());
    }

    @Test
    public void testSetValueInitializes() {
        ModelMVC<String> model = new ModelMVC<>("init");
        model.setValue("first");
        model.setValue("second");
        // Should be able to undo after initialization
        assertEquals("second", model.getValue());
    }

    @Test
    public void testDescriptionWithStringValue() {
        ModelMVC<String> model = new ModelMVC<>("myValue", "String Parameter");
        assertEquals("myValue", model.getValue());
        assertEquals("String Parameter", model.toString());
    }

    @Test
    public void testDescriptionWithNumericValue() {
        ModelMVC<Double> model = new ModelMVC<>(3.14, "Pi Constant");
        assertEquals(3.14, model.getValue(), 0.001);
        assertEquals("Pi Constant", model.toString());
    }

    @Test
    public void testDescriptionPreservedAfterValueChange() {
        ModelMVC<String> model = new ModelMVC<>("initial", "MyDescription");
        model.setValue("changed");
        assertEquals("MyDescription", model.toString());
    }

    @Test
    public void testModelIsUndoable() {
        stringModel.setValue("first");
        stringModel.setValue("second");
        assertTrue(AbstractUndoGenericModel.undoManager.canUndo());
    }

    @Test
    public void testMultipleModelsWithDifferentDescriptions() {
        ModelMVC<String> model1 = new ModelMVC<>("val1", "First");
        ModelMVC<String> model2 = new ModelMVC<>("val2", "Second");

        assertEquals("First", model1.toString());
        assertEquals("Second", model2.toString());
    }

    @Test
    public void testDescriptionObjectWithComplexToString() {
        Object customObj = new Object() {
            private String name = "CustomName";

            @Override
            public String toString() {
                return "Custom[" + name + "]";
            }
        };
        ModelMVC<Integer> model = new ModelMVC<>(42, customObj);
        assertEquals("Custom[CustomName]", model.toString());
    }

    @Test
    public void testNullValueWithDescription() {
        ModelMVC<String> model = new ModelMVC<>(null, "Nullable");
        assertNull(model.getValue());
        assertEquals("Nullable", model.toString());
    }

    @Test
    public void testToStringHashCodeWhenNoDescription() {
        ModelMVC<String> model = new ModelMVC<>("value", null);
        String toStr = model.toString();
        // Should contain hashcode
        assertTrue(toStr.contains(String.valueOf(model.hashCode())));
    }

    @Test
    public void testListenerNotification() {
        final boolean[] notified = {false};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notified[0] = true;
            }
        };

        stringModel.addModelListener(listener);
        stringModel.setValue("new");
        assertTrue(notified[0]);
    }

    @Test
    public void testGlobalEventListenerNotification() {
        final boolean[] notified = {false};
        ActionListener globalListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notified[0] = true;
            }
        };

        AbstractUndoGenericModel.globalEventListeners.add(globalListener);
        stringModel.setValue("new");
        assertTrue(notified[0]);
    }

    @Test
    public void testDescriptionIsObjectNotString() {
        Integer descriptionObject = 123;
        ModelMVC<String> model = new ModelMVC<>("value", descriptionObject);
        assertEquals("123", model.toString());
    }

    @Test
    public void testMultipleValuesWithSameDescription() {
        ModelMVC<Integer> model1 = new ModelMVC<>(10, "Count");
        ModelMVC<Integer> model2 = new ModelMVC<>(20, "Count");

        assertEquals("Count", model1.toString());
        assertEquals("Count", model2.toString());
        assertNotEquals(model1.getValue(), model2.getValue());
    }

    @Test
    public void testDescriptionEmptyString() {
        ModelMVC<String> model = new ModelMVC<>("value", "");
        assertEquals("", model.toString());
    }

    @Test
    public void testValueChangeDoesNotAffectDescription() {
        ModelMVC<Double> model = new ModelMVC<>(1.0, "Resistance");
        model.setValue(2.0);
        model.setValue(3.0);
        model.setValue(4.0);
        assertEquals("Resistance", model.toString());
        assertEquals(4.0, model.getValue(), 0.001);
    }

    @Test
    public void testSerializationIsSupported() {
        // Class implements Serializable, verify it can be cast to Serializable
        assertTrue(stringModel instanceof java.io.Serializable);
    }

    @Test
    public void testBooleanModelWithDescription() {
        ModelMVC<Boolean> model = new ModelMVC<>(true, "EnableFlag");
        assertEquals(true, model.getValue());
        assertEquals("EnableFlag", model.toString());

        model.setValue(false);
        assertEquals(false, model.getValue());
        assertEquals("EnableFlag", model.toString());
    }
}
