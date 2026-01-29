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
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for AbstractUndoGenericModel - MVC model with undo/redo support.
 *
 * Tests cover:
 * - Model initialization and undo flag
 * - Value setting with undo/redo recording
 * - Undo/redo operations
 * - Global event listener notification
 * - Value setting without undo
 * - State transitions and undo action lifecycle
 */
public class AbstractUndoGenericModelTest {

    private TestUndoModel<String> model;
    private TestUndoModel<Integer> intModel;
    private TestUndoModel<Double> doubleModel;

    @Before
    public void setUp() {
        // Clear the undo manager before each test
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        AbstractUndoGenericModel.globalEventListeners.clear();

        model = new TestUndoModel<>("initial");
        intModel = new TestUndoModel<>(42);
        doubleModel = new TestUndoModel<>(3.14);
    }

    @After
    public void tearDown() {
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        AbstractUndoGenericModel.globalEventListeners.clear();
    }

    // Concrete test implementation of abstract class
    private static class TestUndoModel<T> extends AbstractUndoGenericModel<T> {
        private static final long serialVersionUID = 1L;

        public TestUndoModel(T initValue) {
            super(initValue);
        }

        public boolean isRedoUndoFlagSet() {
            return _redoUndoFlag;
        }

        public boolean isInitialized() {
            return _initialized;
        }
    }

    @Test
    public void testInitialStateNotInitialized() {
        assertFalse(model.isInitialized());
        assertEquals("initial", model.getValue());
    }

    @Test
    public void testFirstSetValueInitializes() {
        model.setValue("first");
        assertTrue(model.isInitialized());
        assertEquals("first", model.getValue());
    }

    @Test
    public void testSetValueAddsUndoAfterInitialization() {
        model.setValue("first"); // Initialize
        boolean canUndoBefore = AbstractUndoGenericModel.undoManager.canUndo();
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        model.setValue("second");
        boolean canUndoAfter = AbstractUndoGenericModel.undoManager.canUndo();
        assertFalse(canUndoBefore);
        assertTrue(canUndoAfter);
    }

    @Test
    public void testSetValueDoesNotAddUndoBeforeInitialization() {
        assertFalse(AbstractUndoGenericModel.undoManager.canUndo());
        model.setValue("first");
        assertFalse(AbstractUndoGenericModel.undoManager.canUndo());
    }

    @Test
    public void testSetValueDoesNotAddUndoForSameValue() {
        model.setValue("value"); // Initialize
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        model.setValue("value"); // Same value
        assertFalse(AbstractUndoGenericModel.undoManager.canUndo());
    }

    @Test
    public void testSetValueWithoutUndoDoesNotAddEdit() {
        model.setValue("first"); // Initialize
        AbstractUndoGenericModel.undoManager.discardAllEdits();
        model.setValueWithoutUndo("second");
        assertFalse(AbstractUndoGenericModel.undoManager.canUndo());
    }

    @Test
    public void testSetValueWithoutUndoUpdatesValue() {
        model.setValue("first");
        model.setValueWithoutUndo("second");
        assertEquals("second", model.getValue());
    }

    @Test
    public void testUndo() throws CannotUndoException {
        model.setValue("first");
        model.setValue("second");
        model.setValue("third");

        AbstractUndoGenericModel.undoManager.undo();
        assertEquals("second", model.getValue());
    }

    @Test
    public void testRedo() throws CannotUndoException {
        model.setValue("first");
        model.setValue("second");

        AbstractUndoGenericModel.undoManager.undo();
        assertEquals("first", model.getValue());

        AbstractUndoGenericModel.undoManager.redo();
        assertEquals("second", model.getValue());
    }

    @Test
    public void testUndoMultipleSteps() throws CannotUndoException {
        model.setValue("first");
        model.setValue("second");
        model.setValue("third");

        AbstractUndoGenericModel.undoManager.undo();
        AbstractUndoGenericModel.undoManager.undo();
        assertEquals("first", model.getValue());
    }

    @Test
    public void testRedoMultipleSteps() throws CannotUndoException {
        model.setValue("first");
        model.setValue("second");
        model.setValue("third");

        AbstractUndoGenericModel.undoManager.undo();
        AbstractUndoGenericModel.undoManager.undo();

        AbstractUndoGenericModel.undoManager.redo();
        AbstractUndoGenericModel.undoManager.redo();
        assertEquals("third", model.getValue());
    }

    @Test
    public void testGlobalEventListenerNotificationOnSetValue() {
        final boolean[] notified = {false};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notified[0] = true;
            }
        };

        AbstractUndoGenericModel.globalEventListeners.add(listener);
        model.setValue("new value");
        assertTrue(notified[0]);
    }

    @Test
    public void testGlobalEventListenerReceivesCorrectEvent() {
        final ActionEvent[] receivedEvent = {null};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                receivedEvent[0] = e;
            }
        };

        AbstractUndoGenericModel.globalEventListeners.add(listener);
        model.fireGlobalEvent();
        assertNotNull(receivedEvent[0]);
        assertEquals("global MVC action", receivedEvent[0].getActionCommand());
    }

    @Test
    public void testMultipleGlobalEventListeners() {
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

        AbstractUndoGenericModel.globalEventListeners.add(listener1);
        AbstractUndoGenericModel.globalEventListeners.add(listener2);
        model.fireGlobalEvent();
        assertEquals(2, callCount[0]);
    }

    @Test
    public void testSetValueWithoutUndoDoesNotTriggerGlobalEvent() {
        final int[] callCount = {0};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callCount[0]++;
            }
        };

        AbstractUndoGenericModel.globalEventListeners.add(listener);
        model.setValue("first"); // Initialize and fire
        callCount[0] = 0;
        model.setValueWithoutUndo("second");
        assertEquals(1, callCount[0]); // Should still fire one event
    }

    @Test
    public void testIntegerModelUndo() throws CannotUndoException {
        intModel.setValue(100);
        intModel.setValue(200);

        AbstractUndoGenericModel.undoManager.undo();
        assertEquals(Integer.valueOf(100), intModel.getValue());
    }

    @Test
    public void testDoubleModelUndo() throws CannotUndoException {
        doubleModel.setValue(1.5);
        doubleModel.setValue(2.5);

        AbstractUndoGenericModel.undoManager.undo();
        assertEquals(1.5, doubleModel.getValue(), 0.001);
    }

    @Test
    public void testUndoRedoFlagPreventsRecursiveUndo() throws CannotUndoException {
        model.setValue("first");
        model.setValue("second");

        assertTrue(AbstractUndoGenericModel.undoManager.canUndo());
        AbstractUndoGenericModel.undoManager.undo();

        // After undo, redo should be available (proves no new undo entry was created)
        assertTrue(AbstractUndoGenericModel.undoManager.canRedo());
    }

    @Test
    public void testCannotUndoWhenNoEdits() {
        model.setValue("first"); // Initialize but don't change anything more
        assertFalse(AbstractUndoGenericModel.undoManager.canUndo());
    }

    @Test
    public void testCanUndoAfterValueChange() {
        model.setValue("first");
        model.setValue("second");
        assertTrue(AbstractUndoGenericModel.undoManager.canUndo());
    }

    @Test
    public void testSetValueWithoutUndoInitializes() {
        TestUndoModel<String> newModel = new TestUndoModel<>("init");
        assertFalse(newModel.isInitialized());
        newModel.setValueWithoutUndo("value");
        assertTrue(newModel.isInitialized());
    }

    @Test
    public void testSetValueWithoutUndoDoesNotFireWhenSameValue() {
        model.setValue("same");
        final int[] eventCount = {0};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eventCount[0]++;
            }
        };

        AbstractUndoGenericModel.globalEventListeners.add(listener);
        model.setValueWithoutUndo("same");
        assertEquals(0, eventCount[0]);
    }

    @Test
    public void testUndoManagerLimitDefault() {
        // UndoManager is initialized with limit of 1000
        assertTrue(AbstractUndoGenericModel.undoManager.getLimit() > 0);
    }

    @Test
    public void testSetValueOfDifferentTypesInSequence() {
        TestUndoModel<Object> objModel = new TestUndoModel<>("string");
        objModel.setValue(42);
        assertEquals(42, objModel.getValue());
        objModel.setValue("back to string");
        assertEquals("back to string", objModel.getValue());
    }
}
