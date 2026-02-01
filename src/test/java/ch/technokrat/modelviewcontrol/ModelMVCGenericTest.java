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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for ModelMVCGeneric - base class for MVC model framework with listener support.
 *
 * Tests cover:
 * - Model initialization with various types
 * - Value setting and retrieval
 * - Listener registration and notification
 * - NaN handling for Double values
 * - Listener lifecycle (add/remove)
 */
public class ModelMVCGenericTest {

    // Concrete test implementation of abstract class
    private static class TestModelGeneric<T> extends ModelMVCGeneric<T> {
        public TestModelGeneric(T initValue) {
            super(initValue);
        }
    }

    private TestModelGeneric<String> stringModel;
    private TestModelGeneric<Integer> integerModel;
    private TestModelGeneric<Double> doubleModel;
    private TestModelGeneric<Boolean> booleanModel;

    @Before
    public void setUp() {
        stringModel = new TestModelGeneric<String>("initial");
        integerModel = new TestModelGeneric<Integer>(42);
        doubleModel = new TestModelGeneric<Double>(3.14);
        booleanModel = new TestModelGeneric<Boolean>(true);
    }

    @Test
    public void testInitialValueString() {
        assertEquals("initial", stringModel.getValue());
    }

    @Test
    public void testInitialValueInteger() {
        assertEquals(Integer.valueOf(42), integerModel.getValue());
    }

    @Test
    public void testInitialValueDouble() {
        assertEquals(3.14, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testInitialValueBoolean() {
        assertEquals(true, booleanModel.getValue());
    }

    @Test
    public void testSetValueString() {
        stringModel.setValue("updated");
        assertEquals("updated", stringModel.getValue());
    }

    @Test
    public void testSetValueInteger() {
        integerModel.setValue(100);
        assertEquals(Integer.valueOf(100), integerModel.getValue());
    }

    @Test
    public void testSetValueDouble() {
        doubleModel.setValue(2.71);
        assertEquals(2.71, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testSetValueBoolean() {
        booleanModel.setValue(false);
        assertEquals(false, booleanModel.getValue());
    }

    @Test
    public void testNaNHandlingConvertsToOne() {
        doubleModel.setValue(Double.NaN);
        assertEquals(1.0, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testNegativeInfinityPreserved() {
        doubleModel.setValue(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testPositiveInfinityPreserved() {
        doubleModel.setValue(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testListenerAddition() {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // no-op
            }
        };
        stringModel.addModelListener(listener);
        assertEquals(1, stringModel.listeners.getSize());
    }

    @Test
    public void testMultipleListenerAddition() {
        ActionListener listener1 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        ActionListener listener2 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };

        stringModel.addModelListener(listener1);
        stringModel.addModelListener(listener2);
        assertEquals(2, stringModel.listeners.getSize());
    }

    @Test
    public void testListenerRemoval() {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        stringModel.addModelListener(listener);
        assertEquals(1, stringModel.listeners.getSize());

        stringModel.removeModelListener(listener);
        assertEquals(0, stringModel.listeners.getSize());
    }

    @Test
    public void testListenerNotificationOnValueChange() {
        final boolean[] notified = {false};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notified[0] = true;
                assertEquals("mvc change", e.getActionCommand());
            }
        };

        stringModel.addModelListener(listener);
        stringModel.setValue("new value");
        assertTrue(notified[0]);
    }

    @Test
    public void testListenerNotificationWithCorrectSource() {
        final Object[] source = {null};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                source[0] = e.getSource();
            }
        };

        stringModel.addModelListener(listener);
        stringModel.setValue("new value");
        assertEquals(stringModel, source[0]);
    }

    @Test
    public void testMultipleListenerNotification() {
        final int[] callCount = {0};
        ActionListener listener1 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callCount[0]++;
            }
        };
        ActionListener listener2 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callCount[0]++;
            }
        };

        stringModel.addModelListener(listener1);
        stringModel.addModelListener(listener2);
        stringModel.setValue("new value");
        assertEquals(2, callCount[0]);
    }

    @Test
    public void testListenerNotRemovedWhenNullListeners() {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        stringModel.removeModelListener(listener);
        // Should not throw exception
    }

    @Test
    public void testListenerAdditionWhenNullListenersReinitializes() {
        stringModel.listeners = null;
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        stringModel.addModelListener(listener);
        assertNotNull(stringModel.listeners);
        assertEquals(1, stringModel.listeners.getSize());
    }

    @Test
    public void testNotificationWhenNullListeners() {
        stringModel.listeners = null;
        stringModel.notifyModelListeners(stringModel);
        // Should not throw exception
    }

    @Test
    public void testNotificationWithSpecificListener() {
        final boolean[] notified = {false};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notified[0] = true;
            }
        };
        stringModel.notifyModelListener(listener, stringModel);
        assertTrue(notified[0]);
    }

    @Test
    public void testZeroDoubleValuePreserved() {
        doubleModel.setValue(0.0);
        assertEquals(0.0, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testNegativeDoublePreserved() {
        doubleModel.setValue(-42.5);
        assertEquals(-42.5, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testNullStringValue() {
        stringModel.setValue(null);
        assertNull(stringModel.getValue());
    }
}
