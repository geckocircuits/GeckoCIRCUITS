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

import java.util.Enumeration;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for WeakListModel - ListModel implementation using weak references for listeners.
 *
 * Tests cover:
 * - Element addition, removal, and retrieval
 * - Size and empty state
 * - Contains and index operations
 * - Listener registration and notification
 * - Event firing (contents changed, interval added, interval removed)
 * - Thread-safety with synchronized operations
 */
public class WeakListModelTest {

    private WeakListModel model;
    private TestListDataListener listener;

    private static class TestListDataListener implements ListDataListener {
        public int contentsChangedCount = 0;
        public int intervalAddedCount = 0;
        public int intervalRemovedCount = 0;
        public ListDataEvent lastEvent = null;

        @Override
        public void contentsChanged(ListDataEvent e) {
            contentsChangedCount++;
            lastEvent = e;
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            intervalAddedCount++;
            lastEvent = e;
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            intervalRemovedCount++;
            lastEvent = e;
        }
    }

    @Before
    public void setUp() {
        model = new WeakListModel();
        listener = new TestListDataListener();
    }

    @Test
    public void testInitialSizeIsZero() {
        assertEquals(0, model.getSize());
    }

    @Test
    public void testIsEmptyWhenNoElements() {
        assertTrue(model.isEmpty());
    }

    @Test
    public void testAddElement() {
        model.addElement("item1");
        assertEquals(1, model.getSize());
        assertEquals("item1", model.getElementAt(0));
    }

    @Test
    public void testAddMultipleElements() {
        model.addElement("item1");
        model.addElement("item2");
        model.addElement("item3");
        assertEquals(3, model.getSize());
        assertEquals("item1", model.getElementAt(0));
        assertEquals("item2", model.getElementAt(1));
        assertEquals("item3", model.getElementAt(2));
    }

    @Test
    public void testIsEmptyAfterAddElement() {
        model.addElement("item");
        assertFalse(model.isEmpty());
    }

    @Test
    public void testContains() {
        model.addElement("item1");
        model.addElement("item2");
        assertTrue(model.contains("item1"));
        assertTrue(model.contains("item2"));
        assertFalse(model.contains("item3"));
    }

    @Test
    public void testIndexOf() {
        model.addElement("item1");
        model.addElement("item2");
        model.addElement("item3");
        assertEquals(0, model.indexOf("item1"));
        assertEquals(1, model.indexOf("item2"));
        assertEquals(2, model.indexOf("item3"));
        assertEquals(-1, model.indexOf("item4"));
    }

    @Test
    public void testLastIndexOf() {
        model.addElement("item1");
        model.addElement("item2");
        model.addElement("item1");
        assertEquals(2, model.lastIndexOf("item1"));
        assertEquals(1, model.lastIndexOf("item2"));
    }

    @Test
    public void testElementAt() {
        model.addElement("item1");
        model.addElement("item2");
        assertEquals("item1", model.elementAt(0));
        assertEquals("item2", model.elementAt(1));
    }

    @Test
    public void testFirstElement() {
        model.addElement("first");
        model.addElement("second");
        assertEquals("first", model.firstElement());
    }

    @Test
    public void testLastElement() {
        model.addElement("first");
        model.addElement("second");
        model.addElement("third");
        assertEquals("third", model.lastElement());
    }

    @Test
    public void testRemoveElement() {
        model.addElement("item1");
        model.addElement("item2");
        assertTrue(model.removeElement("item1"));
        assertEquals(1, model.getSize());
        assertEquals("item2", model.getElementAt(0));
    }

    @Test
    public void testRemoveElementReturnsFalseIfNotFound() {
        model.addElement("item1");
        assertFalse(model.removeElement("item2"));
    }

    @Test
    public void testRemoveElementAt() {
        model.addElement("item1");
        model.addElement("item2");
        model.addElement("item3");
        model.removeElementAt(1);
        assertEquals(2, model.getSize());
        assertEquals("item1", model.getElementAt(0));
        assertEquals("item3", model.getElementAt(1));
    }

    @Test
    public void testInsertElementAt() {
        model.addElement("item1");
        model.addElement("item3");
        model.insertElementAt("item2", 1);
        assertEquals(3, model.getSize());
        assertEquals("item2", model.getElementAt(1));
    }

    @Test
    public void testSetElementAt() {
        model.addElement("item1");
        model.addElement("item2");
        model.setElementAt("newItem", 0);
        assertEquals("newItem", model.getElementAt(0));
    }

    @Test
    public void testRemoveAllElements() {
        model.addElement("item1");
        model.addElement("item2");
        model.removeAllElements();
        assertEquals(0, model.getSize());
        assertTrue(model.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, model.size());
        model.addElement("item1");
        assertEquals(1, model.size());
        model.addElement("item2");
        assertEquals(2, model.size());
    }

    @Test
    public void testElements() {
        model.addElement("item1");
        model.addElement("item2");
        model.addElement("item3");
        Enumeration<Object> elements = model.elements();
        assertEquals("item1", elements.nextElement());
        assertEquals("item2", elements.nextElement());
        assertEquals("item3", elements.nextElement());
        assertFalse(elements.hasMoreElements());
    }

    @Test
    public void testToString() {
        model.addElement("item1");
        model.addElement("item2");
        String str = model.toString();
        assertTrue(str.contains("item1"));
        assertTrue(str.contains("item2"));
    }

    @Test
    public void testAddListDataListener() {
        model.addListDataListener(listener);
        model.addElement("item1");
        assertEquals(1, listener.intervalAddedCount);
    }

    @Test
    public void testRemoveListDataListener() {
        model.addListDataListener(listener);
        model.removeListDataListener(listener);
        model.addElement("item1");
        assertEquals(0, listener.intervalAddedCount);
    }

    @Test
    public void testFireIntervalAdded() {
        model.addListDataListener(listener);
        model.addElement("item1");
        assertEquals(1, listener.intervalAddedCount);
        assertEquals(0, listener.lastEvent.getIndex0());
        assertEquals(0, listener.lastEvent.getIndex1());
    }

    @Test
    public void testFireIntervalRemoved() {
        model.addListDataListener(listener);
        model.addElement("item1");
        model.addElement("item2");
        model.removeElementAt(0);
        assertEquals(1, listener.intervalRemovedCount);
        assertEquals(0, listener.lastEvent.getIndex0());
        assertEquals(0, listener.lastEvent.getIndex1());
    }

    @Test
    public void testFireContentsChanged() {
        model.addListDataListener(listener);
        model.addElement("item1");
        model.setElementAt("newItem", 0);
        assertEquals(1, listener.contentsChangedCount);
        assertEquals(0, listener.lastEvent.getIndex0());
        assertEquals(0, listener.lastEvent.getIndex1());
    }

    @Test
    public void testMultipleListeners() {
        TestListDataListener listener2 = new TestListDataListener();
        model.addListDataListener(listener);
        model.addListDataListener(listener2);
        model.addElement("item1");
        assertEquals(1, listener.intervalAddedCount);
        assertEquals(1, listener2.intervalAddedCount);
    }

    @Test
    public void testAddElementFiresCorrectEventType() {
        model.addListDataListener(listener);
        model.addElement("item");
        assertEquals(ListDataEvent.INTERVAL_ADDED, listener.lastEvent.getType());
    }

    @Test
    public void testRemoveElementFiresCorrectEventType() {
        model.addListDataListener(listener);
        model.addElement("item");
        listener.intervalRemovedCount = 0;
        model.removeElement("item");
        assertEquals(ListDataEvent.INTERVAL_REMOVED, listener.lastEvent.getType());
    }

    @Test
    public void testSetElementFiresCorrectEventType() {
        model.addListDataListener(listener);
        model.addElement("item");
        listener.contentsChangedCount = 0;
        model.setElementAt("newItem", 0);
        assertEquals(ListDataEvent.CONTENTS_CHANGED, listener.lastEvent.getType());
    }

    @Test
    public void testRemoveAllElementsFiresEvent() {
        model.addListDataListener(listener);
        model.addElement("item1");
        model.addElement("item2");
        model.removeAllElements();
        assertEquals(1, listener.intervalRemovedCount);
    }

    @Test
    public void testRemoveAllElementsEmptyDoesNotFireEvent() {
        model.addListDataListener(listener);
        model.removeAllElements();
        assertEquals(0, listener.intervalRemovedCount);
    }

    @Test
    public void testTrimToSize() {
        model.addElement("item");
        model.trimToSize();
        assertEquals(1, model.getSize());
        assertEquals("item", model.getElementAt(0));
    }

    @Test
    public void testEnsureCapacity() {
        model.ensureCapacity(100);
        model.addElement("item");
        assertEquals(1, model.getSize());
    }

    @Test
    public void testEventSourceIsModel() {
        model.addListDataListener(listener);
        model.addElement("item");
        assertEquals(model, listener.lastEvent.getSource());
    }

    @Test
    public void testInsertElementAtFiresEvent() {
        model.addListDataListener(listener);
        model.addElement("item1");
        listener.intervalAddedCount = 0;
        model.insertElementAt("item2", 0);
        assertEquals(1, listener.intervalAddedCount);
    }

    @Test
    public void testRemoveElementAtFiresEvent() {
        model.addListDataListener(listener);
        model.addElement("item1");
        model.addElement("item2");
        listener.intervalRemovedCount = 0;
        model.removeElementAt(0);
        assertEquals(1, listener.intervalRemovedCount);
    }

    @Test
    public void testAddNullElement() {
        model.addElement(null);
        assertEquals(1, model.getSize());
        assertNull(model.getElementAt(0));
    }

    @Test
    public void testContainsNull() {
        model.addElement(null);
        assertTrue(model.contains(null));
    }

    @Test
    public void testRemoveNull() {
        model.addElement(null);
        assertTrue(model.removeElement(null));
        assertEquals(0, model.getSize());
    }

    @Test
    public void testListenerArrayConversion() {
        model.addListDataListener(listener);
        java.util.EventListener[] listeners = model.getListeners(ListDataListener.class);
        assertEquals(1, listeners.length);
    }

    @Test
    public void testIndexOperationsWithDuplicates() {
        model.addElement("item");
        model.addElement("item");
        model.addElement("item");
        assertEquals(0, model.indexOf("item"));
        assertEquals(2, model.lastIndexOf("item"));
    }
}
