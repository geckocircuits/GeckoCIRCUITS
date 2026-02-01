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

import ch.technokrat.gecko.geckocircuits.control.Point;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ComponentDirection enum.
 * Tests orientation codes, transitions, and direction calculations.
 */
public class ComponentDirectionTest {

    @Test
    public void testEnumValuesExist() {
        assertEquals(4, ComponentDirection.values().length);
        assertNotNull(ComponentDirection.NORTH_SOUTH);
        assertNotNull(ComponentDirection.EAST_WEST);
        assertNotNull(ComponentDirection.SOUTH_NORTH);
        assertNotNull(ComponentDirection.WEST_EAST);
    }

    @Test
    public void testComponentDirectionCodes() {
        assertEquals(503, ComponentDirection.NORTH_SOUTH.code());
        assertEquals(504, ComponentDirection.EAST_WEST.code());
        assertEquals(501, ComponentDirection.SOUTH_NORTH.code());
        assertEquals(502, ComponentDirection.WEST_EAST.code());
    }

    @Test
    public void testGetFromCodeValidCodes() {
        assertEquals(ComponentDirection.NORTH_SOUTH, ComponentDirection.getFromCode(503));
        assertEquals(ComponentDirection.EAST_WEST, ComponentDirection.getFromCode(504));
        assertEquals(ComponentDirection.SOUTH_NORTH, ComponentDirection.getFromCode(501));
        assertEquals(ComponentDirection.WEST_EAST, ComponentDirection.getFromCode(502));
    }

    @Test
    public void testGetFromCodeInvalidCode() {
        // Invalid codes should return default NORTH_SOUTH
        assertEquals(ComponentDirection.NORTH_SOUTH, ComponentDirection.getFromCode(999));
        assertEquals(ComponentDirection.NORTH_SOUTH, ComponentDirection.getFromCode(0));
        assertEquals(ComponentDirection.NORTH_SOUTH, ComponentDirection.getFromCode(-1));
    }

    @Test
    public void testNextOrientationCycle() {
        ComponentDirection current = ComponentDirection.NORTH_SOUTH;
        current = current.nextOrientation();
        assertEquals(ComponentDirection.EAST_WEST, current);

        current = current.nextOrientation();
        assertEquals(ComponentDirection.SOUTH_NORTH, current);

        current = current.nextOrientation();
        assertEquals(ComponentDirection.WEST_EAST, current);

        current = current.nextOrientation();
        assertEquals(ComponentDirection.NORTH_SOUTH, current);
    }

    @Test
    public void testIsHorizontal() {
        assertFalse(ComponentDirection.NORTH_SOUTH.isHorizontal());
        assertTrue(ComponentDirection.EAST_WEST.isHorizontal());
        assertFalse(ComponentDirection.SOUTH_NORTH.isHorizontal());
        assertTrue(ComponentDirection.WEST_EAST.isHorizontal());
    }

    @Test
    public void testGetDirectionVerticalPoints() {
        // Test SOUTH_NORTH (start above end, decreasing y)
        Point start = new Point(10, 20);
        Point end = new Point(10, 5);
        assertEquals(ComponentDirection.SOUTH_NORTH, ComponentDirection.getDirection(start, end));

        // Test NORTH_SOUTH (start below end, increasing y)
        start = new Point(10, 5);
        end = new Point(10, 20);
        assertEquals(ComponentDirection.NORTH_SOUTH, ComponentDirection.getDirection(start, end));
    }

    @Test
    public void testGetDirectionHorizontalPoints() {
        // Test EAST_WEST (start to the right of end, decreasing x)
        Point start = new Point(20, 10);
        Point end = new Point(5, 10);
        assertEquals(ComponentDirection.EAST_WEST, ComponentDirection.getDirection(start, end));

        // Test WEST_EAST (start to the left of end, increasing x)
        Point start2 = new Point(5, 10);
        Point end2 = new Point(20, 10);
        assertEquals(ComponentDirection.WEST_EAST, ComponentDirection.getDirection(start2, end2));
    }

    @Test
    public void testGetDirectionSamePoint() {
        // Same points should prefer NORTH_SOUTH based on implementation
        Point p = new Point(10, 10);
        assertEquals(ComponentDirection.NORTH_SOUTH, ComponentDirection.getDirection(p, p));
    }

    @Test
    public void testGetDirectionDiagonalPoints() {
        // For diagonal points, x comparison takes precedence
        Point start = new Point(20, 20);
        Point end = new Point(5, 10);
        assertEquals(ComponentDirection.EAST_WEST, ComponentDirection.getDirection(start, end));

        start = new Point(5, 20);
        end = new Point(20, 10);
        assertEquals(ComponentDirection.WEST_EAST, ComponentDirection.getDirection(start, end));
    }

    // Skip Point-related tests to avoid compilation issues in partial builds
    // These methods should work correctly when Point class is available
}
