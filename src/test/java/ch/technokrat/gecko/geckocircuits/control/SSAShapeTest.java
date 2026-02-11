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
 * Unit tests for SSAShape enum.
 */
public class SSAShapeTest {

    @Test
    public void testAllValuesExist() {
        SSAShape[] values = SSAShape.values();
        assertEquals(4, values.length);
    }

    @Test
    public void testSineValue() {
        assertNotNull(SSAShape.SINE);
        assertEquals("SINE", SSAShape.SINE.toString());
    }

    @Test
    public void testRectangleValue() {
        assertNotNull(SSAShape.RECTANGLE);
        assertEquals("RECT", SSAShape.RECTANGLE.toString());
    }

    @Test
    public void testTriangleValue() {
        assertNotNull(SSAShape.TRIANGLE);
        assertEquals("TRI", SSAShape.TRIANGLE.toString());
    }

    @Test
    public void testExternalValue() {
        assertNotNull(SSAShape.EXTERNAL);
        assertEquals("EXT", SSAShape.EXTERNAL.toString());
    }

    @Test
    public void testGetFromOrdinalZero() {
        SSAShape shape = SSAShape.getFromOrdinal(0);
        assertEquals(SSAShape.SINE, shape);
    }

    @Test
    public void testGetFromOrdinalOne() {
        SSAShape shape = SSAShape.getFromOrdinal(1);
        assertEquals(SSAShape.RECTANGLE, shape);
    }

    @Test
    public void testGetFromOrdinalTwo() {
        SSAShape shape = SSAShape.getFromOrdinal(2);
        assertEquals(SSAShape.TRIANGLE, shape);
    }

    @Test
    public void testGetFromOrdinalThree() {
        SSAShape shape = SSAShape.getFromOrdinal(3);
        assertEquals(SSAShape.EXTERNAL, shape);
    }

    @Test
    public void testOrdinalValues() {
        assertEquals(0, SSAShape.SINE.ordinal());
        assertEquals(1, SSAShape.RECTANGLE.ordinal());
        assertEquals(2, SSAShape.TRIANGLE.ordinal());
        assertEquals(3, SSAShape.EXTERNAL.ordinal());
    }

    @Test
    public void testValueOf() {
        assertEquals(SSAShape.SINE, SSAShape.valueOf("SINE"));
        assertEquals(SSAShape.RECTANGLE, SSAShape.valueOf("RECTANGLE"));
        assertEquals(SSAShape.TRIANGLE, SSAShape.valueOf("TRIANGLE"));
        assertEquals(SSAShape.EXTERNAL, SSAShape.valueOf("EXTERNAL"));
    }

    @Test
    public void testShapeReadableNames() {
        assertTrue(SSAShape.SINE.toString().equals("SINE"));
        assertTrue(SSAShape.RECTANGLE.toString().equals("RECT"));
        assertTrue(SSAShape.TRIANGLE.toString().equals("TRI"));
        assertTrue(SSAShape.EXTERNAL.toString().equals("EXT"));
    }
}
