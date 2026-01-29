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
package ch.technokrat.gecko.geckocircuits.circuit.terminal;

import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ITerminalPosition interface and its default implementations.
 * Tests position calculations, distance metrics, and connection validation.
 */
public class ITerminalPositionTest {

    private static final double TOLERANCE = 1e-10;

    // ===========================================
    // Factory Method Tests
    // ===========================================

    @Test
    public void testOf_CreatesTerminalPosition_LK() {
        ITerminalPosition terminal = ITerminalPosition.of(5, 10, ConnectorType.LK);

        assertEquals(5, terminal.getX());
        assertEquals(10, terminal.getY());
        assertEquals(ConnectorType.LK, terminal.getConnectorType());
    }

    @Test
    public void testOf_CreatesTerminalPosition_CONTROL() {
        ITerminalPosition terminal = ITerminalPosition.of(0, 0, ConnectorType.CONTROL);

        assertEquals(0, terminal.getX());
        assertEquals(0, terminal.getY());
        assertEquals(ConnectorType.CONTROL, terminal.getConnectorType());
    }

    @Test
    public void testOf_CreatesTerminalPosition_RELUCTANCE() {
        ITerminalPosition terminal = ITerminalPosition.of(100, 200, ConnectorType.RELUCTANCE);

        assertEquals(100, terminal.getX());
        assertEquals(200, terminal.getY());
        assertEquals(ConnectorType.RELUCTANCE, terminal.getConnectorType());
    }

    @Test
    public void testOf_NegativeCoordinates() {
        ITerminalPosition terminal = ITerminalPosition.of(-10, -5, ConnectorType.LK);

        assertEquals(-10, terminal.getX());
        assertEquals(-5, terminal.getY());
    }

    @Test
    public void testOf_LargeCoordinates() {
        ITerminalPosition terminal = ITerminalPosition.of(10000, 20000, ConnectorType.THERMAL);

        assertEquals(10000, terminal.getX());
        assertEquals(20000, terminal.getY());
    }

    // ===========================================
    // Same Position Tests
    // ===========================================

    @Test
    public void testIsAtSamePosition_True() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 10, ConnectorType.LK);

        assertTrue(t1.isAtSamePosition(t2));
        assertTrue(t2.isAtSamePosition(t1));
    }

    @Test
    public void testIsAtSamePosition_True_DifferentTypes() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 10, ConnectorType.CONTROL);

        // Same position check is position-only, not type-dependent
        assertTrue(t1.isAtSamePosition(t2));
    }

    @Test
    public void testIsAtSamePosition_False_DifferentX() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(6, 10, ConnectorType.LK);

        assertFalse(t1.isAtSamePosition(t2));
    }

    @Test
    public void testIsAtSamePosition_False_DifferentY() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 11, ConnectorType.LK);

        assertFalse(t1.isAtSamePosition(t2));
    }

    @Test
    public void testIsAtSamePosition_False_DifferentBoth() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 20, ConnectorType.LK);

        assertFalse(t1.isAtSamePosition(t2));
    }

    // ===========================================
    // Manhattan Distance Tests
    // ===========================================

    @Test
    public void testManhattanDistanceTo_Horizontal() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 5, ConnectorType.LK);

        assertEquals(10, t1.manhattanDistanceTo(t2));
        assertEquals(10, t2.manhattanDistanceTo(t1));
    }

    @Test
    public void testManhattanDistanceTo_Vertical() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 10, ConnectorType.LK);

        assertEquals(10, t1.manhattanDistanceTo(t2));
    }

    @Test
    public void testManhattanDistanceTo_Diagonal() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(3, 4, ConnectorType.LK);

        assertEquals(7, t1.manhattanDistanceTo(t2));  // |3-0| + |4-0| = 7
    }

    @Test
    public void testManhattanDistanceTo_SamePoint() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 5, ConnectorType.LK);

        assertEquals(0, t1.manhattanDistanceTo(t2));
    }

    @Test
    public void testManhattanDistanceTo_NegativeCoordinates() {
        ITerminalPosition t1 = ITerminalPosition.of(-5, -5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 5, ConnectorType.LK);

        assertEquals(20, t1.manhattanDistanceTo(t2));  // |5-(-5)| + |5-(-5)| = 20
    }

    @Test
    public void testManhattanDistanceTo_LargeDistances() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(1000, 2000, ConnectorType.LK);

        assertEquals(3000, t1.manhattanDistanceTo(t2));
    }

    // ===========================================
    // Euclidean Distance Tests
    // ===========================================

    @Test
    public void testEuclideanDistanceTo_Horizontal() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 0, ConnectorType.LK);

        assertEquals(10.0, t1.euclideanDistanceTo(t2), TOLERANCE);
    }

    @Test
    public void testEuclideanDistanceTo_Vertical() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(0, 10, ConnectorType.LK);

        assertEquals(10.0, t1.euclideanDistanceTo(t2), TOLERANCE);
    }

    @Test
    public void testEuclideanDistanceTo_3_4_5Triangle() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(3, 4, ConnectorType.LK);

        assertEquals(5.0, t1.euclideanDistanceTo(t2), TOLERANCE);
    }

    @Test
    public void testEuclideanDistanceTo_SamePoint() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 5, ConnectorType.LK);

        assertEquals(0.0, t1.euclideanDistanceTo(t2), TOLERANCE);
    }

    @Test
    public void testEuclideanDistanceTo_Symmetric() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.LK);

        double dist1 = t1.euclideanDistanceTo(t2);
        double dist2 = t2.euclideanDistanceTo(t1);

        assertEquals(dist1, dist2, TOLERANCE);
    }

    @Test
    public void testEuclideanDistanceTo_NegativeCoordinates() {
        ITerminalPosition t1 = ITerminalPosition.of(-3, -4, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(0, 0, ConnectorType.LK);

        assertEquals(5.0, t1.euclideanDistanceTo(t2), TOLERANCE);
    }

    // ===========================================
    // Connection Capability Tests
    // ===========================================

    @Test
    public void testCanConnectTo_SameType_LK() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.LK);

        assertTrue(t1.canConnectTo(t2));
        assertTrue(t2.canConnectTo(t1));
    }

    @Test
    public void testCanConnectTo_SameType_CONTROL() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.CONTROL);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.CONTROL);

        assertTrue(t1.canConnectTo(t2));
    }

    @Test
    public void testCanConnectTo_SameType_RELUCTANCE() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.RELUCTANCE);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.RELUCTANCE);

        assertTrue(t1.canConnectTo(t2));
    }

    @Test
    public void testCanConnectTo_SameType_THERMAL() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.THERMAL);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.THERMAL);

        assertTrue(t1.canConnectTo(t2));
    }

    @Test
    public void testCanConnectTo_IncompatibleTypes_LK_CONTROL() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.CONTROL);

        assertFalse(t1.canConnectTo(t2));
        assertFalse(t2.canConnectTo(t1));
    }

    @Test
    public void testCanConnectTo_IncompatibleTypes_LK_RELUCTANCE() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.RELUCTANCE);

        assertFalse(t1.canConnectTo(t2));
    }

    @Test
    public void testCanConnectTo_IncompatibleTypes_CONTROL_THERMAL() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.CONTROL);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.THERMAL);

        assertFalse(t1.canConnectTo(t2));
    }

    @Test
    public void testCanConnectTo_WithNONE() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 10, ConnectorType.NONE);

        assertFalse(t1.canConnectTo(t2));
    }

    // ===========================================
    // Horizontal Alignment Tests
    // ===========================================

    @Test
    public void testIsHorizontallyAligned_True() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(20, 5, ConnectorType.LK);

        assertTrue(t1.isHorizontallyAligned(t2));
        assertTrue(t2.isHorizontallyAligned(t1));
    }

    @Test
    public void testIsHorizontallyAligned_False() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(20, 10, ConnectorType.LK);

        assertFalse(t1.isHorizontallyAligned(t2));
    }

    @Test
    public void testIsHorizontallyAligned_SamePoint() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 5, ConnectorType.LK);

        assertTrue(t1.isHorizontallyAligned(t2));
    }

    @Test
    public void testIsHorizontallyAligned_NegativeCoordinates() {
        ITerminalPosition t1 = ITerminalPosition.of(-10, -5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, -5, ConnectorType.LK);

        assertTrue(t1.isHorizontallyAligned(t2));
    }

    // ===========================================
    // Vertical Alignment Tests
    // ===========================================

    @Test
    public void testIsVerticallyAligned_True() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 20, ConnectorType.LK);

        assertTrue(t1.isVerticallyAligned(t2));
        assertTrue(t2.isVerticallyAligned(t1));
    }

    @Test
    public void testIsVerticallyAligned_False() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, 20, ConnectorType.LK);

        assertFalse(t1.isVerticallyAligned(t2));
    }

    @Test
    public void testIsVerticallyAligned_SamePoint() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 5, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 5, ConnectorType.LK);

        assertTrue(t1.isVerticallyAligned(t2));
    }

    @Test
    public void testIsVerticallyAligned_NegativeCoordinates() {
        ITerminalPosition t1 = ITerminalPosition.of(-5, -10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(-5, 10, ConnectorType.LK);

        assertTrue(t1.isVerticallyAligned(t2));
    }

    // ===========================================
    // SimpleTerminalPosition Implementation Tests
    // ===========================================

    @Test
    public void testSimpleTerminalPosition_Equality() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t3 = ITerminalPosition.of(5, 10, ConnectorType.CONTROL);

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);  // Different connector type
    }

    @Test
    public void testSimpleTerminalPosition_HashCode() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 10, ConnectorType.LK);

        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testSimpleTerminalPosition_ToString() {
        ITerminalPosition t = ITerminalPosition.of(5, 10, ConnectorType.LK);
        String str = t.toString();

        assertTrue(str.contains("5"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("LK"));
    }

    @Test
    public void testSimpleTerminalPosition_NotEqual_DifferentX() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(6, 10, ConnectorType.LK);

        assertNotEquals(t1, t2);
    }

    @Test
    public void testSimpleTerminalPosition_NotEqual_DifferentY() {
        ITerminalPosition t1 = ITerminalPosition.of(5, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(5, 11, ConnectorType.LK);

        assertNotEquals(t1, t2);
    }

    @Test
    public void testSimpleTerminalPosition_EqualsNull() {
        ITerminalPosition t = ITerminalPosition.of(5, 10, ConnectorType.LK);

        assertNotEquals(t, null);
    }

    @Test
    public void testSimpleTerminalPosition_EqualsSelf() {
        ITerminalPosition t = ITerminalPosition.of(5, 10, ConnectorType.LK);

        assertEquals(t, t);
    }

    // ===========================================
    // All Connector Types Test Coverage
    // ===========================================

    @Test
    public void testAllConnectorTypes_LK() {
        ITerminalPosition t = ITerminalPosition.of(0, 0, ConnectorType.LK);
        assertEquals(ConnectorType.LK, t.getConnectorType());
    }

    @Test
    public void testAllConnectorTypes_CONTROL() {
        ITerminalPosition t = ITerminalPosition.of(0, 0, ConnectorType.CONTROL);
        assertEquals(ConnectorType.CONTROL, t.getConnectorType());
    }

    @Test
    public void testAllConnectorTypes_RELUCTANCE() {
        ITerminalPosition t = ITerminalPosition.of(0, 0, ConnectorType.RELUCTANCE);
        assertEquals(ConnectorType.RELUCTANCE, t.getConnectorType());
    }

    @Test
    public void testAllConnectorTypes_THERMAL() {
        ITerminalPosition t = ITerminalPosition.of(0, 0, ConnectorType.THERMAL);
        assertEquals(ConnectorType.THERMAL, t.getConnectorType());
    }

    @Test
    public void testAllConnectorTypes_LK_AND_RELUCTANCE() {
        ITerminalPosition t = ITerminalPosition.of(0, 0, ConnectorType.LK_AND_RELUCTANCE);
        assertEquals(ConnectorType.LK_AND_RELUCTANCE, t.getConnectorType());
    }

    @Test
    public void testAllConnectorTypes_NONE() {
        ITerminalPosition t = ITerminalPosition.of(0, 0, ConnectorType.NONE);
        assertEquals(ConnectorType.NONE, t.getConnectorType());
    }

    // ===========================================
    // Edge Cases and Boundary Tests
    // ===========================================

    @Test
    public void testBoundaryValues_ZeroCoordinates() {
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(0, 1, ConnectorType.LK);

        assertEquals(1, t1.manhattanDistanceTo(t2));
    }

    @Test
    public void testBoundaryValues_MaxIntegerCoordinates() {
        // Use a reasonable large value instead of Integer.MAX_VALUE for practical use
        int largeVal = 100000;
        ITerminalPosition t1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(largeVal, largeVal, ConnectorType.LK);

        assertEquals(2 * largeVal, t1.manhattanDistanceTo(t2));
    }

    @Test
    public void testMixedCoordinateSigns() {
        ITerminalPosition t1 = ITerminalPosition.of(-10, 10, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(10, -10, ConnectorType.LK);

        int expectedManhattan = 20 + 20;  // |10-(-10)| + |-10-10|
        assertEquals(expectedManhattan, t1.manhattanDistanceTo(t2));
    }

    @Test
    public void testPositionIndependenceOfAlignment() {
        ITerminalPosition t1 = ITerminalPosition.of(-100, 50, ConnectorType.LK);
        ITerminalPosition t2 = ITerminalPosition.of(100, 50, ConnectorType.LK);

        assertTrue(t1.isHorizontallyAligned(t2));
    }
}
