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

import static org.junit.Assert.*;

/**
 * Unit tests for GridPoint class.
 * Tests point creation, operations, and comparisons.
 */
public class GridPointTest {

    @Test
    public void testConstructorIntCoordinates() {
        GridPoint p = new GridPoint(10, 20);
        assertEquals(10, p.x);
        assertEquals(20, p.y);
    }

    @Test
    public void testConstructorDoubleCoordinates() {
        GridPoint p = new GridPoint(10.5, 20.7);
        assertEquals(10, p.x);
        assertEquals(20, p.y);
    }

    @Test
    public void testConstructorCopy() {
        GridPoint original = new GridPoint(15, 25);
        GridPoint copy = new GridPoint(original);
        assertEquals(original.x, copy.x);
        assertEquals(original.y, copy.y);
        assertNotSame(original, copy);
    }

    @Test
    public void testOriginConstant() {
        assertEquals(0, GridPoint.ORIGIN.x);
        assertEquals(0, GridPoint.ORIGIN.y);
    }

    @Test
    public void testGetX() {
        GridPoint p = new GridPoint(42, 99);
        assertEquals(42, p.getX());
    }

    @Test
    public void testGetY() {
        GridPoint p = new GridPoint(42, 99);
        assertEquals(99, p.getY());
    }

    @Test
    public void testTranslate() {
        GridPoint p = new GridPoint(10, 20);
        GridPoint translated = p.translate(5, -3);
        assertEquals(15, translated.x);
        assertEquals(17, translated.y);
        // Original should be unchanged
        assertEquals(10, p.x);
        assertEquals(20, p.y);
    }

    @Test
    public void testAdd() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(5, 3);
        GridPoint result = p1.add(p2);
        assertEquals(15, result.x);
        assertEquals(23, result.y);
    }

    @Test
    public void testAddNegative() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(-3, -5);
        GridPoint result = p1.add(p2);
        assertEquals(7, result.x);
        assertEquals(15, result.y);
    }

    @Test
    public void testSubtract() {
        GridPoint p1 = new GridPoint(20, 30);
        GridPoint p2 = new GridPoint(5, 10);
        GridPoint result = p1.subtract(p2);
        assertEquals(15, result.x);
        assertEquals(20, result.y);
    }

    @Test
    public void testDistanceTo() {
        GridPoint p1 = new GridPoint(0, 0);
        GridPoint p2 = new GridPoint(3, 4);
        // 3-4-5 triangle
        assertEquals(5.0, p1.distanceTo(p2), 0.001);
    }

    @Test
    public void testDistanceToSamePoint() {
        GridPoint p1 = new GridPoint(5, 5);
        GridPoint p2 = new GridPoint(5, 5);
        assertEquals(0.0, p1.distanceTo(p2), 0.001);
    }

    @Test
    public void testDistanceToIsSymmetric() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(25, 35);
        assertEquals(p1.distanceTo(p2), p2.distanceTo(p1), 0.001);
    }

    @Test
    public void testManhattanDistanceTo() {
        GridPoint p1 = new GridPoint(0, 0);
        GridPoint p2 = new GridPoint(3, 4);
        assertEquals(7, p1.manhattanDistanceTo(p2));
    }

    @Test
    public void testManhattanDistanceToSamePoint() {
        GridPoint p1 = new GridPoint(5, 5);
        GridPoint p2 = new GridPoint(5, 5);
        assertEquals(0, p1.manhattanDistanceTo(p2));
    }

    @Test
    public void testManhattanDistanceIsSymmetric() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(25, 35);
        assertEquals(p1.manhattanDistanceTo(p2), p2.manhattanDistanceTo(p1));
    }

    @Test
    public void testIsWithinTrue() {
        GridPoint p = new GridPoint(5, 5);
        assertTrue(p.isWithin(0, 0, 10, 10));
        assertTrue(p.isWithin(5, 5, 6, 6));
    }

    @Test
    public void testIsWithinFalseOutside() {
        GridPoint p = new GridPoint(15, 15);
        assertFalse(p.isWithin(0, 0, 10, 10));
    }

    @Test
    public void testIsWithinFalseOnBoundary() {
        GridPoint p = new GridPoint(10, 5);
        assertFalse(p.isWithin(0, 0, 10, 10)); // max is exclusive
        assertTrue(p.isWithin(0, 0, 11, 10));
    }

    @Test
    public void testEqualsTrue() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 20);
        assertEquals(p1, p2);
    }

    @Test
    public void testEqualsFalse() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 21);
        assertNotEquals(p1, p2);
    }

    @Test
    public void testEqualsSameObject() {
        GridPoint p = new GridPoint(10, 20);
        assertEquals(p, p);
    }

    @Test
    public void testEqualsWithNull() {
        GridPoint p = new GridPoint(10, 20);
        assertNotEquals(p, null);
    }

    @Test
    public void testHashCode() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 20);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 21);
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testToString() {
        GridPoint p = new GridPoint(10, 20);
        assertEquals("GridPoint[10, 20]", p.toString());
    }

    @Test
    public void testCompareTo() {
        GridPoint p1 = new GridPoint(10, 10);
        GridPoint p2 = new GridPoint(10, 20);
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
    }

    @Test
    public void testCompareToYCoordinatePriority() {
        // Y coordinate takes priority in comparison
        GridPoint p1 = new GridPoint(10, 10);
        GridPoint p2 = new GridPoint(20, 20);
        assertTrue(p1.compareTo(p2) < 0);

        // When Y is same, X determines order
        GridPoint p3 = new GridPoint(10, 15);
        GridPoint p4 = new GridPoint(20, 15);
        assertTrue(p3.compareTo(p4) < 0);
    }

    @Test
    public void testCompareToEqual() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 20);
        assertEquals(0, p1.compareTo(p2));
    }

    @Test
    public void testSerializableInterface() {
        assertTrue(java.io.Serializable.class.isAssignableFrom(GridPoint.class));
    }

    @Test
    public void testComparableInterface() {
        assertTrue(Comparable.class.isAssignableFrom(GridPoint.class));
    }
}
