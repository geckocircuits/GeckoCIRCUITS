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
package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for immutable Point class.
 */
public class PointTest {

    @Test
    public void testPointConstruction() {
        Point p = new Point(5, 10);
        assertEquals(5, p.x);
        assertEquals(10, p.y);
    }

    @Test
    public void testPointWithNegativeCoordinates() {
        Point p = new Point(-3, -7);
        assertEquals(-3, p.x);
        assertEquals(-7, p.y);
    }

    @Test
    public void testPointWithZeroCoordinates() {
        Point p = new Point(0, 0);
        assertEquals(0, p.x);
        assertEquals(0, p.y);
    }

    @Test
    public void testPointEqualsWithSameCoordinates() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(5, 10);
        assertEquals(p1, p2);
        assertTrue(p1.equals(p2));
    }

    @Test
    public void testPointNotEqualsWithDifferentX() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(6, 10);
        assertNotEquals(p1, p2);
        assertFalse(p1.equals(p2));
    }

    @Test
    public void testPointNotEqualsWithDifferentY() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(5, 11);
        assertNotEquals(p1, p2);
        assertFalse(p1.equals(p2));
    }

    @Test
    public void testPointNotEqualsWithNull() {
        Point p = new Point(5, 10);
        assertFalse(p.equals(null));
    }

    @Test
    public void testPointNotEqualsWithDifferentType() {
        Point p = new Point(5, 10);
        assertFalse(p.equals("not a point"));
        assertFalse(p.equals(5));
    }

    @Test
    public void testPointHashCodeConsistency() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(5, 10);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testPointHashCodeDifference() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(6, 10);
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testPointToString() {
        Point p = new Point(5, 10);
        assertEquals("5 10", p.toString());
    }

    @Test
    public void testPointToStringWithNegatives() {
        Point p = new Point(-3, -7);
        assertEquals("-3 -7", p.toString());
    }

    @Test
    public void testDistanceZero() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(5, 10);
        assertEquals(0.0, p1.distance(p2), 1e-9);
    }

    @Test
    public void testDistanceSimple() {
        Point p1 = new Point(0, 0);
        Point p2 = new Point(3, 4);
        // 3-4-5 right triangle
        assertEquals(5.0, p1.distance(p2), 1e-9);
    }

    @Test
    public void testDistanceNegativeCoordinates() {
        Point p1 = new Point(-1, -1);
        Point p2 = new Point(2, 3);
        // distance = sqrt((2-(-1))^2 + (3-(-1))^2) = sqrt(9 + 16) = sqrt(25) = 5
        assertEquals(5.0, p1.distance(p2), 1e-9);
    }

    @Test
    public void testDistanceSymmetric() {
        Point p1 = new Point(1, 2);
        Point p2 = new Point(4, 6);
        assertEquals(p1.distance(p2), p2.distance(p1), 1e-9);
    }

    @Test
    public void testPointImmutability() {
        Point p = new Point(5, 10);
        int originalX = p.x;
        int originalY = p.y;
        assertEquals(originalX, p.x);
        assertEquals(originalY, p.y);
    }

    @Test
    public void testLargeCoordinates() {
        Point p = new Point(Integer.MAX_VALUE, Integer.MIN_VALUE);
        assertEquals(Integer.MAX_VALUE, p.x);
        assertEquals(Integer.MIN_VALUE, p.y);
    }
}
