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
package ch.technokrat.gecko.geckocircuits.circuit.component;

import ch.technokrat.gecko.geckocircuits.circuit.component.ComponentPositionManager.Direction;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.Point;

/**
 * Unit tests for ComponentPositionManager.
 * Tests position, rotation, mirroring, and coordinate transformations.
 */
public class ComponentPositionManagerTest {
    
    private ComponentPositionManager manager;
    
    @Before
    public void setUp() {
        manager = new ComponentPositionManager();
    }
    
    // ===== Constructor Tests =====
    
    @Test
    public void testDefaultConstructor() {
        assertEquals(0, manager.getX());
        assertEquals(0, manager.getY());
        assertEquals(Direction.NORTH, manager.getDirection());
        assertFalse(manager.isMirrored());
    }
    
    @Test
    public void testPositionConstructor() {
        ComponentPositionManager m = new ComponentPositionManager(10, 20);
        assertEquals(10, m.getX());
        assertEquals(20, m.getY());
        assertEquals(Direction.NORTH, m.getDirection());
    }
    
    @Test
    public void testFullConstructor() {
        ComponentPositionManager m = new ComponentPositionManager(5, 10, Direction.EAST, true);
        assertEquals(5, m.getX());
        assertEquals(10, m.getY());
        assertEquals(Direction.EAST, m.getDirection());
        assertTrue(m.isMirrored());
    }
    
    @Test
    public void testConstructorNullDirection() {
        ComponentPositionManager m = new ComponentPositionManager(0, 0, null, false);
        assertEquals(Direction.NORTH, m.getDirection());
    }
    
    // ===== Position Tests =====
    
    @Test
    public void testSetPosition() {
        manager.setPosition(15, 25);
        assertEquals(15, manager.getX());
        assertEquals(25, manager.getY());
    }
    
    @Test
    public void testSetPositionFromPoint() {
        manager.setPosition(new Point(30, 40));
        assertEquals(new Point(30, 40), manager.getPosition());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetPositionNullPoint() {
        manager.setPosition(null);
    }
    
    @Test
    public void testTranslate() {
        manager.setPosition(10, 20);
        manager.translate(5, -3);
        assertEquals(15, manager.getX());
        assertEquals(17, manager.getY());
    }
    
    @Test
    public void testSetXY() {
        manager.setX(100);
        manager.setY(200);
        assertEquals(100, manager.getX());
        assertEquals(200, manager.getY());
    }
    
    // ===== Direction Tests =====
    
    @Test
    public void testDirectionValues() {
        assertEquals(0, Direction.NORTH.getDegrees());
        assertEquals(90, Direction.EAST.getDegrees());
        assertEquals(180, Direction.SOUTH.getDegrees());
        assertEquals(270, Direction.WEST.getDegrees());
    }
    
    @Test
    public void testRotateClockwise() {
        assertEquals(Direction.EAST, Direction.NORTH.rotateClockwise());
        assertEquals(Direction.SOUTH, Direction.EAST.rotateClockwise());
        assertEquals(Direction.WEST, Direction.SOUTH.rotateClockwise());
        assertEquals(Direction.NORTH, Direction.WEST.rotateClockwise());
    }
    
    @Test
    public void testRotateCounterClockwise() {
        assertEquals(Direction.WEST, Direction.NORTH.rotateCounterClockwise());
        assertEquals(Direction.NORTH, Direction.EAST.rotateCounterClockwise());
        assertEquals(Direction.EAST, Direction.SOUTH.rotateCounterClockwise());
        assertEquals(Direction.SOUTH, Direction.WEST.rotateCounterClockwise());
    }
    
    @Test
    public void testOpposite() {
        assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
        assertEquals(Direction.NORTH, Direction.SOUTH.opposite());
        assertEquals(Direction.WEST, Direction.EAST.opposite());
        assertEquals(Direction.EAST, Direction.WEST.opposite());
    }
    
    @Test
    public void testFromDegrees() {
        assertEquals(Direction.NORTH, Direction.fromDegrees(0));
        assertEquals(Direction.EAST, Direction.fromDegrees(90));
        assertEquals(Direction.SOUTH, Direction.fromDegrees(180));
        assertEquals(Direction.WEST, Direction.fromDegrees(270));
    }
    
    @Test
    public void testFromDegreesNormalization() {
        assertEquals(Direction.NORTH, Direction.fromDegrees(360));
        assertEquals(Direction.NORTH, Direction.fromDegrees(-360));
        assertEquals(Direction.EAST, Direction.fromDegrees(450)); // 450 = 90
        assertEquals(Direction.WEST, Direction.fromDegrees(-90)); // -90 = 270
    }
    
    @Test
    public void testFromDegreesRounding() {
        // Round to nearest 90°
        assertEquals(Direction.NORTH, Direction.fromDegrees(30));
        assertEquals(Direction.EAST, Direction.fromDegrees(80));
        assertEquals(Direction.SOUTH, Direction.fromDegrees(200));
        assertEquals(Direction.WEST, Direction.fromDegrees(300));
    }
    
    @Test
    public void testFromOrdinal() {
        assertEquals(Direction.NORTH, Direction.fromOrdinal(0));
        assertEquals(Direction.EAST, Direction.fromOrdinal(1));
        assertEquals(Direction.SOUTH, Direction.fromOrdinal(2));
        assertEquals(Direction.WEST, Direction.fromOrdinal(3));
        assertEquals(Direction.NORTH, Direction.fromOrdinal(4)); // Wraps
    }
    
    @Test
    public void testIsHorizontalVertical() {
        assertTrue(Direction.EAST.isHorizontal());
        assertTrue(Direction.WEST.isHorizontal());
        assertFalse(Direction.NORTH.isHorizontal());
        assertFalse(Direction.SOUTH.isHorizontal());
        
        assertTrue(Direction.NORTH.isVertical());
        assertTrue(Direction.SOUTH.isVertical());
        assertFalse(Direction.EAST.isVertical());
        assertFalse(Direction.WEST.isVertical());
    }
    
    @Test
    public void testSetDirection() {
        manager.setDirection(Direction.SOUTH);
        assertEquals(Direction.SOUTH, manager.getDirection());
    }
    
    @Test
    public void testSetDirectionNull() {
        manager.setDirection(null);
        assertEquals(Direction.NORTH, manager.getDirection());
    }
    
    @Test
    public void testSetDirectionDegrees() {
        manager.setDirectionDegrees(180);
        assertEquals(Direction.SOUTH, manager.getDirection());
    }
    
    @Test
    public void testSetDirectionOrdinal() {
        manager.setDirectionOrdinal(2);
        assertEquals(Direction.SOUTH, manager.getDirection());
    }
    
    @Test
    public void testRotateClockwiseMethod() {
        manager.setDirection(Direction.NORTH);
        manager.rotateClockwise();
        assertEquals(Direction.EAST, manager.getDirection());
        manager.rotateClockwise();
        assertEquals(Direction.SOUTH, manager.getDirection());
    }
    
    @Test
    public void testRotateCounterClockwiseMethod() {
        manager.setDirection(Direction.NORTH);
        manager.rotateCounterClockwise();
        assertEquals(Direction.WEST, manager.getDirection());
    }
    
    @Test
    public void testRotate180() {
        manager.setDirection(Direction.NORTH);
        manager.rotate180();
        assertEquals(Direction.SOUTH, manager.getDirection());
    }
    
    // ===== Mirroring Tests =====
    
    @Test
    public void testMirrored() {
        assertFalse(manager.isMirrored());
        manager.setMirrored(true);
        assertTrue(manager.isMirrored());
    }
    
    @Test
    public void testToggleMirror() {
        assertFalse(manager.isMirrored());
        manager.toggleMirror();
        assertTrue(manager.isMirrored());
        manager.toggleMirror();
        assertFalse(manager.isMirrored());
    }
    
    // ===== Saved Position Tests =====
    
    @Test
    public void testSaveRestorePosition() {
        manager.setPosition(10, 20);
        manager.savePosition();
        manager.setPosition(50, 60);
        
        assertEquals(10, manager.getSavedX());
        assertEquals(20, manager.getSavedY());
        assertEquals(new Point(10, 20), manager.getSavedPosition());
        
        manager.restorePosition();
        assertEquals(10, manager.getX());
        assertEquals(20, manager.getY());
    }
    
    @Test
    public void testDeltaFromSaved() {
        manager.setPosition(10, 20);
        manager.savePosition();
        manager.setPosition(15, 25);
        
        Point delta = manager.getDeltaFromSaved();
        assertEquals(5, delta.x);
        assertEquals(5, delta.y);
    }
    
    // ===== Bounding Box Tests =====
    
    @Test
    public void testDefaultDimensions() {
        assertEquals(1, manager.getWidth());
        assertEquals(1, manager.getHeight());
    }
    
    @Test
    public void testSetDimensions() {
        manager.setDimensions(3, 5);
        assertEquals(3, manager.getWidth());
        assertEquals(5, manager.getHeight());
    }
    
    @Test
    public void testSetDimensionsMinimum() {
        manager.setDimensions(0, -5);
        assertEquals(1, manager.getWidth()); // Minimum 1
        assertEquals(1, manager.getHeight());
    }
    
    @Test
    public void testEffectiveDimensionsNorth() {
        manager.setDimensions(3, 5);
        manager.setDirection(Direction.NORTH);
        assertEquals(3, manager.getEffectiveWidth());
        assertEquals(5, manager.getEffectiveHeight());
    }
    
    @Test
    public void testEffectiveDimensionsEast() {
        manager.setDimensions(3, 5);
        manager.setDirection(Direction.EAST);
        assertEquals(5, manager.getEffectiveWidth()); // Swapped
        assertEquals(3, manager.getEffectiveHeight());
    }
    
    @Test
    public void testCenter() {
        manager.setPosition(10, 20);
        manager.setDimensions(4, 6);
        
        assertEquals(12, manager.getCenterX());
        assertEquals(23, manager.getCenterY());
        assertEquals(new Point(12, 23), manager.getCenter());
    }
    
    @Test
    public void testEdges() {
        manager.setPosition(10, 20);
        manager.setDimensions(4, 6);
        
        assertEquals(10, manager.getLeft());
        assertEquals(14, manager.getRight());
        assertEquals(20, manager.getTop());
        assertEquals(26, manager.getBottom());
    }
    
    // ===== Hit Testing Tests =====
    
    @Test
    public void testContains() {
        manager.setPosition(10, 20);
        manager.setDimensions(5, 5);
        
        assertTrue(manager.contains(10, 20)); // Top-left corner
        assertTrue(manager.contains(12, 22)); // Inside
        assertFalse(manager.contains(15, 20)); // Right edge (exclusive)
        assertFalse(manager.contains(10, 25)); // Bottom edge (exclusive)
        assertFalse(manager.contains(5, 22)); // Left of bounds
    }
    
    @Test
    public void testContainsPoint() {
        manager.setPosition(0, 0);
        manager.setDimensions(10, 10);
        
        assertTrue(manager.contains(new Point(5, 5)));
        assertFalse(manager.contains(new Point(15, 5)));
        assertFalse(manager.contains((Point) null));
    }
    
    @Test
    public void testIntersects() {
        manager.setPosition(0, 0);
        manager.setDimensions(10, 10);
        
        ComponentPositionManager other = new ComponentPositionManager(5, 5);
        other.setDimensions(10, 10);
        
        assertTrue(manager.intersects(other));
        
        ComponentPositionManager disjoint = new ComponentPositionManager(20, 20);
        disjoint.setDimensions(5, 5);
        assertFalse(manager.intersects(disjoint));
        
        assertFalse(manager.intersects(null));
    }
    
    // ===== Coordinate Transformation Tests =====
    
    @Test
    public void testLocalToSheetNorth() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.NORTH);
        
        Point result = manager.localToSheet(2, 3);
        assertEquals(new Point(12, 23), result);
    }
    
    @Test
    public void testLocalToSheetEast() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.EAST);
        
        Point result = manager.localToSheet(2, 3);
        assertEquals(new Point(7, 22), result); // Rotated 90° CW
    }
    
    @Test
    public void testLocalToSheetMirrored() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.NORTH);
        manager.setMirrored(true);
        
        Point result = manager.localToSheet(2, 3);
        assertEquals(new Point(8, 23), result); // X flipped
    }
    
    @Test
    public void testSheetToLocalNorth() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.NORTH);
        
        Point result = manager.sheetToLocal(12, 23);
        assertEquals(new Point(2, 3), result);
    }
    
    @Test
    public void testLocalToSheetRoundTrip() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.EAST);
        manager.setMirrored(true);
        
        Point local = new Point(2, 3);
        Point sheet = manager.localToSheet(local.x, local.y);
        Point backToLocal = manager.sheetToLocal(sheet.x, sheet.y);
        
        assertEquals(local, backToLocal);
    }
    
    // ===== Serialization Tests =====
    
    @Test
    public void testToIntArray() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.SOUTH);
        manager.setMirrored(true);
        
        int[] data = manager.toIntArray();
        assertEquals(4, data.length);
        assertEquals(10, data[0]);
        assertEquals(20, data[1]);
        assertEquals(2, data[2]); // SOUTH ordinal
        assertEquals(1, data[3]); // mirrored
    }
    
    @Test
    public void testFromIntArray() {
        manager.fromIntArray(new int[]{30, 40, 1, 0});
        
        assertEquals(30, manager.getX());
        assertEquals(40, manager.getY());
        assertEquals(Direction.EAST, manager.getDirection());
        assertFalse(manager.isMirrored());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testFromIntArrayInvalid() {
        manager.fromIntArray(new int[]{1, 2}); // Too short
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testFromIntArrayNull() {
        manager.fromIntArray(null);
    }
    
    // ===== Copy Tests =====
    
    @Test
    public void testCopy() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.WEST);
        manager.setMirrored(true);
        manager.setDimensions(3, 5);
        manager.savePosition();
        
        ComponentPositionManager copy = manager.copy();
        
        assertEquals(manager.getX(), copy.getX());
        assertEquals(manager.getY(), copy.getY());
        assertEquals(manager.getDirection(), copy.getDirection());
        assertEquals(manager.isMirrored(), copy.isMirrored());
        assertEquals(manager.getWidth(), copy.getWidth());
        assertEquals(manager.getHeight(), copy.getHeight());
        assertEquals(manager.getSavedX(), copy.getSavedX());
        
        // Ensure independence
        copy.setPosition(99, 99);
        assertEquals(10, manager.getX());
    }
    
    // ===== Equals/HashCode Tests =====
    
    @Test
    public void testEquals() {
        ComponentPositionManager m1 = new ComponentPositionManager(10, 20, Direction.EAST, true);
        ComponentPositionManager m2 = new ComponentPositionManager(10, 20, Direction.EAST, true);
        ComponentPositionManager m3 = new ComponentPositionManager(10, 20, Direction.WEST, true);
        
        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
        assertNotEquals(m1, null);
        assertNotEquals(m1, "string");
    }
    
    @Test
    public void testHashCode() {
        ComponentPositionManager m1 = new ComponentPositionManager(10, 20, Direction.EAST, true);
        ComponentPositionManager m2 = new ComponentPositionManager(10, 20, Direction.EAST, true);
        
        assertEquals(m1.hashCode(), m2.hashCode());
    }
    
    // ===== ToString Tests =====
    
    @Test
    public void testToString() {
        manager.setPosition(10, 20);
        manager.setDirection(Direction.EAST);
        manager.setDimensions(3, 5);
        
        String str = manager.toString();
        assertTrue(str.contains("10"));
        assertTrue(str.contains("20"));
        assertTrue(str.contains("EAST"));
        assertTrue(str.contains("3x5") || str.contains("3") && str.contains("5"));
    }
    
    @Test
    public void testToStringMirrored() {
        manager.setMirrored(true);
        String str = manager.toString();
        assertTrue(str.contains("mirror"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalComponentWorkflow() {
        // Create component at position
        ComponentPositionManager comp = new ComponentPositionManager(100, 200);
        comp.setDimensions(2, 4);
        
        // Rotate 90° for horizontal orientation
        comp.rotateClockwise();
        assertEquals(Direction.EAST, comp.getDirection());
        assertEquals(4, comp.getEffectiveWidth()); // Swapped due to rotation
        assertEquals(2, comp.getEffectiveHeight());
        
        // Move component
        comp.savePosition();
        comp.translate(10, 5);
        assertEquals(110, comp.getX());
        assertEquals(205, comp.getY());
        
        // Undo move
        comp.restorePosition();
        assertEquals(100, comp.getX());
        assertEquals(200, comp.getY());
    }
    
    @Test
    public void testFullRotationCycle() {
        manager.setDirection(Direction.NORTH);
        
        manager.rotateClockwise();
        assertEquals(Direction.EAST, manager.getDirection());
        
        manager.rotateClockwise();
        assertEquals(Direction.SOUTH, manager.getDirection());
        
        manager.rotateClockwise();
        assertEquals(Direction.WEST, manager.getDirection());
        
        manager.rotateClockwise();
        assertEquals(Direction.NORTH, manager.getDirection());
    }
}
