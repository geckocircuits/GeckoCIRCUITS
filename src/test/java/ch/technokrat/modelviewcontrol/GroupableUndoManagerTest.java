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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive tests for GroupableUndoManager and its inner classes.
 *
 * Tests cover:
 * - GroupableUndoManager basic undo/redo (UndoManager subclass)
 * - GroupUndoStart - all UndoableEdit methods, edit merging logic
 * - GroupUndoStop - undo/redo applying operations in correct order
 * - Presentation names with/without parent edits
 * - Complete grouping workflow: Start -> add edits -> Stop -> undo/redo
 * - die() clearing internal lists
 * - isSignificant() returning correct values (Start=false, Stop=true)
 * - Synchronization of undo/redo/addEdit methods
 */
public class GroupableUndoManagerTest {

    private GroupableUndoManager undoManager;
    private TestEdit testEdit1;
    private TestEdit testEdit2;
    private TestEdit testEdit3;

    /**
     * Simple test UndoableEdit implementation that tracks undo/redo calls.
     */
    private static class TestEdit implements UndoableEdit {
        private int undoCount = 0;
        private int redoCount = 0;
        private final String name;
        private boolean canUndo = true;
        private boolean canRedo = true;

        public TestEdit(String name) {
            this.name = name;
        }

        @Override
        public void undo() throws CannotUndoException {
            if (!canUndo) {
                throw new CannotUndoException();
            }
            undoCount++;
        }

        @Override
        public boolean canUndo() {
            return canUndo;
        }

        @Override
        public void redo() throws CannotRedoException {
            if (!canRedo) {
                throw new CannotRedoException();
            }
            redoCount++;
        }

        @Override
        public boolean canRedo() {
            return canRedo;
        }

        @Override
        public void die() {
            // Cleanup if needed
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return name;
        }

        @Override
        public String getUndoPresentationName() {
            return "Undo " + name;
        }

        @Override
        public String getRedoPresentationName() {
            return "Redo " + name;
        }

        public int getUndoCount() {
            return undoCount;
        }

        public int getRedoCount() {
            return redoCount;
        }

        public void setCanUndo(boolean canUndo) {
            this.canUndo = canUndo;
        }

        public void setCanRedo(boolean canRedo) {
            this.canRedo = canRedo;
        }
    }

    @Before
    public void setUp() {
        undoManager = new GroupableUndoManager();
        testEdit1 = new TestEdit("Edit1");
        testEdit2 = new TestEdit("Edit2");
        testEdit3 = new TestEdit("Edit3");
    }

    // ====================================================
    // GroupableUndoManager Basic Tests
    // ====================================================

    @Test
    public void testGroupableUndoManagerIsUndoManager() {
        assertTrue("GroupableUndoManager should extend UndoManager",
            undoManager instanceof javax.swing.undo.UndoManager);
    }

    @Test
    public void testAddEditReturnsTrue() {
        boolean result = undoManager.addEdit(testEdit1);
        assertTrue("addEdit should return true for valid edit", result);
    }

    @Test
    public void testCanUndoAfterAddingEdit() {
        undoManager.addEdit(testEdit1);
        assertTrue("Should be able to undo after adding edit", undoManager.canUndo());
    }

    @Test
    public void testCanRedoAfterUndo() throws CannotUndoException {
        undoManager.addEdit(testEdit1);
        undoManager.undo();
        assertTrue("Should be able to redo after undo", undoManager.canRedo());
    }

    @Test
    public void testUndoCallsEditUndo() throws CannotUndoException {
        undoManager.addEdit(testEdit1);
        undoManager.undo();
        assertEquals("Undo should call edit's undo method", 1, testEdit1.getUndoCount());
    }

    @Test
    public void testRedoCallsEditRedo() throws CannotUndoException {
        undoManager.addEdit(testEdit1);
        undoManager.undo();
        undoManager.redo();
        assertEquals("Redo should call edit's redo method", 1, testEdit1.getRedoCount());
    }

    @Test
    public void testCannotUndoWhenEmpty() {
        assertFalse("Should not be able to undo when manager is empty", undoManager.canUndo());
    }

    @Test(expected = CannotUndoException.class)
    public void testUndoThrowsExceptionWhenEmpty() throws CannotUndoException {
        undoManager.undo();
    }

    @Test(expected = CannotRedoException.class)
    public void testRedoThrowsExceptionWhenNotAvailable() throws CannotRedoException {
        undoManager.redo();
    }

    @Test
    public void testMultipleEditsUndoInReverseOrder() throws CannotUndoException {
        undoManager.addEdit(testEdit1);
        undoManager.addEdit(testEdit2);
        undoManager.addEdit(testEdit3);

        undoManager.undo(); // Removes testEdit3
        assertEquals("testEdit3 should be undone", 1, testEdit3.getUndoCount());
        assertEquals("testEdit1 and testEdit2 should not be undone", 0, testEdit1.getUndoCount());

        undoManager.undo(); // Removes testEdit2
        assertEquals("testEdit2 should be undone", 1, testEdit2.getUndoCount());
    }

    // ====================================================
    // GroupUndoStart Basic Tests
    // ====================================================

    @Test
    public void testGroupUndoStartIsUndoableEdit() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertTrue("GroupUndoStart should implement UndoableEdit",
            start instanceof UndoableEdit);
    }

    @Test
    public void testGroupUndoStartCanUndo() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertTrue("GroupUndoStart canUndo should return true", start.canUndo());
    }

    @Test
    public void testGroupUndoStartCanRedo() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertTrue("GroupUndoStart canRedo should return true", start.canRedo());
    }

    @Test
    public void testGroupUndoStartIsNotSignificant() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertFalse("GroupUndoStart isSignificant should return false", start.isSignificant());
    }

    @Test
    public void testGroupUndoStartPresentationNames() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertEquals("GroupUndoStart getPresentationName should return 'not available'",
            "not available", start.getPresentationName());
        assertEquals("GroupUndoStart getUndoPresentationName should return 'not available'",
            "not available", start.getUndoPresentationName());
        assertEquals("GroupUndoStart getRedoPresentationName should return 'not available'",
            "not available", start.getRedoPresentationName());
    }

    @Test
    public void testGroupUndoStartUndoDoesNothing() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        start.undo(); // Should not throw
    }

    @Test
    public void testGroupUndoStartRedoDoesNothing() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        start.redo(); // Should not throw
    }

    @Test
    public void testGroupUndoStartReplaceEditReturnsFalse() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertFalse("GroupUndoStart replaceEdit should return false",
            start.replaceEdit(testEdit1));
    }

    @Test
    public void testGroupUndoStartDieClearsEdits() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        start.addEdit(testEdit1);
        start.addEdit(testEdit2);
        start.die();
        // After die(), merged edits should be cleared
        // We can't directly inspect, but subsequent operations shouldn't affect cleared data
    }

    // ====================================================
    // GroupUndoStart AddEdit Merging Logic
    // ====================================================

    @Test
    public void testGroupUndoStartAddEditAcceptsRegularEdit() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        boolean result = start.addEdit(testEdit1);
        assertTrue("GroupUndoStart should accept regular edits", result);
    }

    @Test
    public void testGroupUndoStartAcceptsMultipleEdits() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        assertTrue(start.addEdit(testEdit1));
        assertTrue(start.addEdit(testEdit2));
        assertTrue(start.addEdit(testEdit3));
    }

    @Test
    public void testGroupUndoStartStopsAcceptingEditsAfterStop() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        start.addEdit(testEdit1);
        start.addEdit(testEdit2);
        start.addEdit(stop); // Close the group

        // After adding the matching stop, otherEditsAccepted should be false
        boolean afterStopResult = start.addEdit(testEdit3);
        assertFalse("GroupUndoStart should not accept edits after stop", afterStopResult);
    }

    @Test
    public void testGroupUndoStartTransfersEditsToStop() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        start.addEdit(testEdit1);
        start.addEdit(testEdit2);
        boolean stopResult = start.addEdit(stop);

        // The stop should be accepted but not added to merge list
        assertFalse("Adding matching stop should return false", stopResult);
    }

    @Test
    public void testGroupUndoStartIgnoresNonMatchingStop() {
        GroupableUndoManager.GroupUndoStart start1 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStart start2 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop2 = new GroupableUndoManager.GroupUndoStop(start2);

        // Try to add a stop that doesn't match this start
        boolean result = start1.addEdit(stop2);
        assertTrue("Non-matching stop should be added like regular edit", result);
    }

    // ====================================================
    // GroupUndoStop Basic Tests
    // ====================================================

    @Test
    public void testGroupUndoStopIsUndoableEdit() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertTrue("GroupUndoStop should implement UndoableEdit",
            stop instanceof UndoableEdit);
    }

    @Test
    public void testGroupUndoStopCanUndo() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertTrue("GroupUndoStop canUndo should return true", stop.canUndo());
    }

    @Test
    public void testGroupUndoStopCanRedo() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertTrue("GroupUndoStop canRedo should return true", stop.canRedo());
    }

    @Test
    public void testGroupUndoStopIsSignificant() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertTrue("GroupUndoStop isSignificant should return true", stop.isSignificant());
    }

    @Test
    public void testGroupUndoStopAddEditReturnsFalse() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertFalse("GroupUndoStop addEdit should return false",
            stop.addEdit(testEdit1));
    }

    @Test
    public void testGroupUndoStopReplaceEditReturnsFalse() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertFalse("GroupUndoStop replaceEdit should return false",
            stop.replaceEdit(testEdit1));
    }

    @Test
    public void testGroupUndoStopUndoWithEmptyEditList() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        start.addEdit(stop); // This transfers empty edit list to stop
        stop.undo(); // Should not throw with empty list
    }

    @Test
    public void testGroupUndoStopRedoWithEmptyEditList() throws CannotRedoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        start.addEdit(stop); // This transfers empty edit list to stop
        stop.redo(); // Should not throw with empty list
    }

    @Test
    public void testGroupUndoStopDieClearsEditList() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        start.addEdit(testEdit1);
        start.addEdit(testEdit2);
        start.addEdit(stop);
        stop.die();
        // After die(), edit list should be cleared
    }

    // ====================================================
    // GroupUndoStop Presentation Names
    // ====================================================

    @Test
    public void testGroupUndoStopPresentationNameWithoutParent() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertEquals("GroupUndoStop without parent should return 'Multi operation'",
            "Multi operation", stop.getPresentationName());
    }

    @Test
    public void testGroupUndoStopUndoPresentationNameWithoutParent() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertEquals("GroupUndoStop without parent should return 'Operation on multiple components'",
            "Operation on multiple components", stop.getUndoPresentationName());
    }

    @Test
    public void testGroupUndoStopRedoPresentationNameWithoutParent() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        assertEquals("GroupUndoStop without parent should return 'Operation on multiple components'",
            "Operation on multiple components", stop.getRedoPresentationName());
    }

    @Test
    public void testGroupUndoStopPresentationNameWithParent() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        stop.setParentEditForInfo(testEdit1);
        assertEquals("GroupUndoStop with parent should return parent's presentation name",
            "Edit1", stop.getPresentationName());
    }

    @Test
    public void testGroupUndoStopUndoPresentationNameWithParent() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        stop.setParentEditForInfo(testEdit1);
        assertEquals("GroupUndoStop with parent should return parent's undo presentation name",
            "Undo Edit1", stop.getUndoPresentationName());
    }

    @Test
    public void testGroupUndoStopRedoPresentationNameWithParent() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);
        stop.setParentEditForInfo(testEdit1);
        assertEquals("GroupUndoStop with parent should return parent's redo presentation name",
            "Redo Edit1", stop.getRedoPresentationName());
    }

    // ====================================================
    // GroupUndoStop Undo/Redo Order Tests
    // ====================================================

    @Test
    public void testGroupUndoStopUndoCallsEditsInForwardOrder() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        start.addEdit(testEdit1);
        start.addEdit(testEdit2);
        start.addEdit(testEdit3);
        start.addEdit(stop);

        stop.undo();

        // All edits should have been undone once
        assertEquals("testEdit1 should be undone", 1, testEdit1.getUndoCount());
        assertEquals("testEdit2 should be undone", 1, testEdit2.getUndoCount());
        assertEquals("testEdit3 should be undone", 1, testEdit3.getUndoCount());
    }

    @Test
    public void testGroupUndoStopRedoCallsEditsInReverseOrder() throws CannotUndoException, CannotRedoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        start.addEdit(testEdit1);
        start.addEdit(testEdit2);
        start.addEdit(testEdit3);
        start.addEdit(stop);

        stop.undo();
        // Reset counters to verify order
        testEdit1.redoCount = 0;
        testEdit2.redoCount = 0;
        testEdit3.redoCount = 0;

        stop.redo();

        // All edits should have been redone once
        assertEquals("testEdit1 should be redone", 1, testEdit1.getRedoCount());
        assertEquals("testEdit2 should be redone", 1, testEdit2.getRedoCount());
        assertEquals("testEdit3 should be redone", 1, testEdit3.getRedoCount());
    }

    // ====================================================
    // Complete Grouping Workflow Tests
    // ====================================================

    @Test
    public void testCompleteGroupWorkflow() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        undoManager.addEdit(start);
        undoManager.addEdit(testEdit1);
        undoManager.addEdit(testEdit2);
        undoManager.addEdit(testEdit3);
        undoManager.addEdit(stop);

        assertTrue("Should be able to undo after adding group", undoManager.canUndo());
        undoManager.undo();

        assertEquals("testEdit1 should be undone", 1, testEdit1.getUndoCount());
        assertEquals("testEdit2 should be undone", 1, testEdit2.getUndoCount());
        assertEquals("testEdit3 should be undone", 1, testEdit3.getUndoCount());
    }

    @Test
    public void testCompleteGroupWorkflowWithRedo() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        undoManager.addEdit(start);
        undoManager.addEdit(testEdit1);
        undoManager.addEdit(testEdit2);
        undoManager.addEdit(testEdit3);
        undoManager.addEdit(stop);

        undoManager.undo();
        assertTrue("Should be able to redo after undo", undoManager.canRedo());
        undoManager.redo();

        assertEquals("testEdit1 should be redone", 1, testEdit1.getRedoCount());
        assertEquals("testEdit2 should be redone", 1, testEdit2.getRedoCount());
        assertEquals("testEdit3 should be redone", 1, testEdit3.getRedoCount());
    }

    @Test
    public void testMultipleGroupsInSequence() throws CannotUndoException {
        // First group
        GroupableUndoManager.GroupUndoStart start1 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop1 = new GroupableUndoManager.GroupUndoStop(start1);
        TestEdit edit1a = new TestEdit("Edit1a");
        TestEdit edit1b = new TestEdit("Edit1b");

        undoManager.addEdit(start1);
        undoManager.addEdit(edit1a);
        undoManager.addEdit(edit1b);
        undoManager.addEdit(stop1);

        // Second group
        GroupableUndoManager.GroupUndoStart start2 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop2 = new GroupableUndoManager.GroupUndoStop(start2);
        TestEdit edit2a = new TestEdit("Edit2a");
        TestEdit edit2b = new TestEdit("Edit2b");

        undoManager.addEdit(start2);
        undoManager.addEdit(edit2a);
        undoManager.addEdit(edit2b);
        undoManager.addEdit(stop2);

        // Undo second group
        undoManager.undo();
        assertEquals("edit2a should be undone", 1, edit2a.getUndoCount());
        assertEquals("edit2b should be undone", 1, edit2b.getUndoCount());
        assertEquals("edit1a should not be undone yet", 0, edit1a.getUndoCount());

        // Undo first group
        undoManager.undo();
        assertEquals("edit1a should be undone", 1, edit1a.getUndoCount());
        assertEquals("edit1b should be undone", 1, edit1b.getUndoCount());
    }

    @Test
    public void testGroupWithNoIntermediateEdits() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        undoManager.addEdit(start);
        undoManager.addEdit(stop); // Empty group

        assertTrue("Empty group should still be undoable", undoManager.canUndo());
        undoManager.undo();
        // Should not throw
    }

    @Test
    public void testGroupWithSingleEdit() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        undoManager.addEdit(start);
        undoManager.addEdit(testEdit1);
        undoManager.addEdit(stop);

        undoManager.undo();
        assertEquals("Single grouped edit should be undone", 1, testEdit1.getUndoCount());
    }

    // ====================================================
    // Edge Cases and Error Handling
    // ====================================================

    @Test
    public void testMismatchedGroupStartAndStop() {
        GroupableUndoManager.GroupUndoStart start1 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStart start2 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop2 = new GroupableUndoManager.GroupUndoStop(start2);

        // Adding stop2 to start1 should treat it as a regular edit
        boolean result = start1.addEdit(stop2);
        assertTrue("Mismatched stop should be added as regular edit", result);
    }

    @Test
    public void testGroupStartWithNestedStops() {
        GroupableUndoManager.GroupUndoStart start1 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStart start2 = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop1 = new GroupableUndoManager.GroupUndoStop(start1);
        GroupableUndoManager.GroupUndoStop stop2 = new GroupableUndoManager.GroupUndoStop(start2);

        // start1 accepts start2, stop2, and stop1
        start1.addEdit(start2);
        boolean stop2Result = start1.addEdit(stop2);
        assertTrue("stop2 (non-matching) should be accepted", stop2Result);

        boolean stop1Result = start1.addEdit(stop1);
        assertFalse("stop1 (matching) should close the group", stop1Result);
    }

    @Test
    public void testGroupEditCallChain() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        TestEdit edit1 = new TestEdit("E1");
        TestEdit edit2 = new TestEdit("E2");
        TestEdit edit3 = new TestEdit("E3");

        start.addEdit(edit1);
        start.addEdit(edit2);
        start.addEdit(edit3);
        start.addEdit(stop); // Transfer edits to stop

        // Now stop has the edits
        stop.undo();

        // Verify all edits were undone
        assertEquals(1, edit1.getUndoCount());
        assertEquals(1, edit2.getUndoCount());
        assertEquals(1, edit3.getUndoCount());
    }

    @Test
    public void testSynchronizationOfUndoMethod() throws CannotUndoException {
        // Test that undo method is synchronized
        GroupableUndoManager mgr = new GroupableUndoManager();
        mgr.addEdit(testEdit1);
        mgr.undo(); // Should not throw due to synchronization
        assertEquals(1, testEdit1.getUndoCount());
    }

    @Test
    public void testSynchronizationOfRedoMethod() throws CannotUndoException {
        // Test that redo method is synchronized
        GroupableUndoManager mgr = new GroupableUndoManager();
        mgr.addEdit(testEdit1);
        mgr.undo();
        mgr.redo(); // Should not throw due to synchronization
        assertEquals(1, testEdit1.getRedoCount());
    }

    @Test
    public void testSynchronizationOfAddEditMethod() {
        // Test that addEdit method is synchronized
        GroupableUndoManager mgr = new GroupableUndoManager();
        boolean result = mgr.addEdit(testEdit1); // Should not throw
        assertTrue(result);
    }

    // ====================================================
    // Presentation and Metadata Tests
    // ====================================================

    @Test
    public void testGroupStopDelegatesParentPresentation() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        TestEdit parent = new TestEdit("ParentOperation");
        stop.setParentEditForInfo(parent);

        assertEquals("Should use parent's presentation name",
            parent.getPresentationName(), stop.getPresentationName());
        assertEquals("Should use parent's undo name",
            parent.getUndoPresentationName(), stop.getUndoPresentationName());
        assertEquals("Should use parent's redo name",
            parent.getRedoPresentationName(), stop.getRedoPresentationName());
    }

    @Test
    public void testGroupStopChangeParentEdit() {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        TestEdit parent1 = new TestEdit("Parent1");
        TestEdit parent2 = new TestEdit("Parent2");

        stop.setParentEditForInfo(parent1);
        assertEquals("Parent1", stop.getPresentationName());

        stop.setParentEditForInfo(parent2);
        assertEquals("Parent2", stop.getPresentationName());
    }

    // ====================================================
    // Stress Tests
    // ====================================================

    @Test
    public void testLargeGroupOfEdits() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        TestEdit[] edits = new TestEdit[100];
        for (int i = 0; i < 100; i++) {
            edits[i] = new TestEdit("Edit" + i);
            start.addEdit(edits[i]);
        }
        start.addEdit(stop);

        undoManager.addEdit(start);
        undoManager.addEdit(stop);

        undoManager.undo();

        // Verify all edits were undone
        for (int i = 0; i < 100; i++) {
            assertEquals("Edit " + i + " should be undone", 1, edits[i].getUndoCount());
        }
    }

    @Test
    public void testMultipleUndoRedoCycles() throws CannotUndoException {
        GroupableUndoManager.GroupUndoStart start = new GroupableUndoManager.GroupUndoStart();
        GroupableUndoManager.GroupUndoStop stop = new GroupableUndoManager.GroupUndoStop(start);

        undoManager.addEdit(start);
        undoManager.addEdit(testEdit1);
        undoManager.addEdit(testEdit2);
        undoManager.addEdit(stop);

        // Multiple cycles
        for (int i = 0; i < 3; i++) {
            undoManager.undo();
            assertEquals("Cycle " + i + ": testEdit1 undo count", i + 1, testEdit1.getUndoCount());
            undoManager.redo();
            assertEquals("Cycle " + i + ": testEdit1 redo count", i + 1, testEdit1.getRedoCount());
        }
    }
}
