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
 * Unit tests for ConnectionPath.
 * Tests path creation, manipulation, and validation.
 */
public class ConnectionPathTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // ===========================================
    // Path Creation Tests
    // ===========================================
    
    @Test
    public void testEmptyPathCreation() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        
        assertEquals(0, path.getPointCount());
        assertEquals(0, path.getSegmentCount());
        assertEquals(ConnectorType.LK, path.getConnectorType());
    }
    
    @Test
    public void testAddPoints() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        assertEquals(3, path.getPointCount());
        assertEquals(2, path.getSegmentCount());
    }
    
    @Test
    public void testGetStartPoint() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(5, 10);
        path.addPoint(15, 10);
        
        ConnectionPath.PathPoint start = path.getStartPoint();
        assertEquals(5, start.x);
        assertEquals(10, start.y);
    }
    
    @Test
    public void testGetEndPoint() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(5, 10);
        path.addPoint(15, 20);
        
        ConnectionPath.PathPoint end = path.getEndPoint();
        assertEquals(15, end.x);
        assertEquals(20, end.y);
    }
    
    @Test
    public void testGetStartEndPoint_EmptyPath() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        
        assertNull(path.getStartPoint());
        assertNull(path.getEndPoint());
    }
    
    // ===========================================
    // L-Path Creation Tests
    // ===========================================
    
    @Test
    public void testCreateLPath_HorizontalFirst() {
        ITerminalPosition start = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition end = ITerminalPosition.of(10, 5, ConnectorType.LK);
        
        ConnectionPath path = ConnectionPath.createLPath(start, end, true);
        
        assertEquals(3, path.getPointCount());  // Start, corner, end
        assertEquals(0, path.getStartPoint().x);
        assertEquals(0, path.getStartPoint().y);
        assertEquals(10, path.getEndPoint().x);
        assertEquals(5, path.getEndPoint().y);
    }
    
    @Test
    public void testCreateLPath_VerticalFirst() {
        ITerminalPosition start = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition end = ITerminalPosition.of(10, 5, ConnectorType.LK);
        
        ConnectionPath path = ConnectionPath.createLPath(start, end, false);
        
        assertEquals(3, path.getPointCount());
        // Middle point should be (0, 5) for vertical-first
        ConnectionPath.PathPoint middle = path.getPoints().get(1);
        assertEquals(0, middle.x);
        assertEquals(5, middle.y);
    }
    
    @Test
    public void testCreateLPath_AlignedHorizontally() {
        ITerminalPosition start = ITerminalPosition.of(0, 5, ConnectorType.LK);
        ITerminalPosition end = ITerminalPosition.of(10, 5, ConnectorType.LK);
        
        ConnectionPath path = ConnectionPath.createLPath(start, end, true);
        
        // L-path always creates 3 points (start, corner, end) even if aligned
        // The corner just coincides with one of the endpoints
        assertTrue(path.getPointCount() >= 2);
    }
    
    @Test
    public void testCreateLPath_AlignedVertically() {
        ITerminalPosition start = ITerminalPosition.of(5, 0, ConnectorType.LK);
        ITerminalPosition end = ITerminalPosition.of(5, 10, ConnectorType.LK);
        
        ConnectionPath path = ConnectionPath.createLPath(start, end, false);
        
        // L-path always creates 3 points (start, corner, end) even if aligned
        assertTrue(path.getPointCount() >= 2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateLPath_IncompatibleTypes() {
        ITerminalPosition start = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition end = ITerminalPosition.of(10, 5, ConnectorType.CONTROL);
        
        ConnectionPath.createLPath(start, end, true);
    }
    
    // ===========================================
    // Direct Path Creation Tests
    // ===========================================
    
    @Test
    public void testCreateDirectPath_Horizontal() {
        ITerminalPosition start = ITerminalPosition.of(0, 5, ConnectorType.CONTROL);
        ITerminalPosition end = ITerminalPosition.of(10, 5, ConnectorType.CONTROL);
        
        ConnectionPath path = ConnectionPath.createDirectPath(start, end);
        
        assertEquals(2, path.getPointCount());
        assertEquals(ConnectorType.CONTROL, path.getConnectorType());
    }
    
    @Test
    public void testCreateDirectPath_Vertical() {
        ITerminalPosition start = ITerminalPosition.of(5, 0, ConnectorType.CONTROL);
        ITerminalPosition end = ITerminalPosition.of(5, 10, ConnectorType.CONTROL);
        
        ConnectionPath path = ConnectionPath.createDirectPath(start, end);
        
        assertEquals(2, path.getPointCount());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDirectPath_NotAligned() {
        ITerminalPosition start = ITerminalPosition.of(0, 0, ConnectorType.CONTROL);
        ITerminalPosition end = ITerminalPosition.of(10, 5, ConnectorType.CONTROL);
        
        ConnectionPath.createDirectPath(start, end);
    }
    
    // ===========================================
    // Path Length Tests
    // ===========================================
    
    @Test
    public void testGetTotalLength_Horizontal() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        
        assertEquals(10, path.getTotalLength());
    }
    
    @Test
    public void testGetTotalLength_Vertical() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(0, 5);
        
        assertEquals(5, path.getTotalLength());
    }
    
    @Test
    public void testGetTotalLength_LPath() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        assertEquals(15, path.getTotalLength());  // 10 + 5
    }
    
    @Test
    public void testGetTotalLength_Empty() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        
        assertEquals(0, path.getTotalLength());
    }
    
    // ===========================================
    // Corner Count Tests
    // ===========================================
    
    @Test
    public void testGetCornerCount_Straight() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        
        assertEquals(0, path.getCornerCount());
    }
    
    @Test
    public void testGetCornerCount_OneCorner() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        assertEquals(1, path.getCornerCount());
    }
    
    @Test
    public void testGetCornerCount_TwoCorners() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(5, 0);
        path.addPoint(5, 5);
        path.addPoint(10, 5);
        
        assertEquals(2, path.getCornerCount());
    }
    
    // ===========================================
    // Path Validity Tests
    // ===========================================
    
    @Test
    public void testIsValid_ValidPath() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        assertTrue(path.isValid());
    }
    
    @Test
    public void testIsValid_SinglePoint() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        
        assertFalse(path.isValid());  // Less than 2 points
    }
    
    @Test
    public void testIsValid_DiagonalSegment() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(5, 5);  // Diagonal!
        
        assertFalse(path.isValid());
    }
    
    // ===========================================
    // Contains Point Tests
    // ===========================================
    
    @Test
    public void testContainsPoint_OnHorizontalSegment() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        
        assertTrue(path.containsPoint(5, 0));
        assertTrue(path.containsPoint(0, 0));
        assertTrue(path.containsPoint(10, 0));
    }
    
    @Test
    public void testContainsPoint_OnVerticalSegment() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(0, 10);
        
        assertTrue(path.containsPoint(0, 5));
    }
    
    @Test
    public void testContainsPoint_NotOnPath() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        
        assertFalse(path.containsPoint(5, 5));
        assertFalse(path.containsPoint(-1, 0));
        assertFalse(path.containsPoint(11, 0));
    }
    
    @Test
    public void testContainsPoint_AtCorner() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        assertTrue(path.containsPoint(10, 0));  // Corner point
    }
    
    // ===========================================
    // Reverse Tests
    // ===========================================
    
    @Test
    public void testReverse() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        ConnectionPath reversed = path.reverse();
        
        assertEquals(3, reversed.getPointCount());
        assertEquals(10, reversed.getStartPoint().x);
        assertEquals(5, reversed.getStartPoint().y);
        assertEquals(0, reversed.getEndPoint().x);
        assertEquals(0, reversed.getEndPoint().y);
    }
    
    @Test
    public void testReverse_PreservesConnectorType() {
        ConnectionPath path = new ConnectionPath(ConnectorType.CONTROL);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        
        ConnectionPath reversed = path.reverse();
        
        assertEquals(ConnectorType.CONTROL, reversed.getConnectorType());
    }
    
    // ===========================================
    // Trim Tests
    // ===========================================
    
    @Test
    public void testTrimmed_RemovesCollinearPoints() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(5, 0);   // Collinear - should be removed
        path.addPoint(10, 0);
        
        ConnectionPath trimmed = path.trimmed();
        
        assertEquals(2, trimmed.getPointCount());
        assertEquals(0, trimmed.getStartPoint().x);
        assertEquals(10, trimmed.getEndPoint().x);
    }
    
    @Test
    public void testTrimmed_PreservesCorners() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        path.addPoint(10, 5);
        
        ConnectionPath trimmed = path.trimmed();
        
        assertEquals(3, trimmed.getPointCount());  // All points needed
    }
    
    @Test
    public void testTrimmed_ShortPath() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(10, 0);
        
        ConnectionPath trimmed = path.trimmed();
        
        assertEquals(2, trimmed.getPointCount());
    }
    
    // ===========================================
    // PathPoint Tests
    // ===========================================
    
    @Test
    public void testPathPoint_Equality() {
        ConnectionPath.PathPoint p1 = new ConnectionPath.PathPoint(5, 10);
        ConnectionPath.PathPoint p2 = new ConnectionPath.PathPoint(5, 10);
        ConnectionPath.PathPoint p3 = new ConnectionPath.PathPoint(5, 11);
        
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
    }
    
    @Test
    public void testPathPoint_HashCode() {
        ConnectionPath.PathPoint p1 = new ConnectionPath.PathPoint(5, 10);
        ConnectionPath.PathPoint p2 = new ConnectionPath.PathPoint(5, 10);
        
        assertEquals(p1.hashCode(), p2.hashCode());
    }
    
    @Test
    public void testPathPoint_ToString() {
        ConnectionPath.PathPoint p = new ConnectionPath.PathPoint(5, 10);
        
        assertEquals("(5,10)", p.toString());
    }
    
    // ===========================================
    // Equals/HashCode Tests
    // ===========================================
    
    @Test
    public void testPath_Equality() {
        ConnectionPath path1 = new ConnectionPath(ConnectorType.LK);
        path1.addPoint(0, 0);
        path1.addPoint(10, 0);
        
        ConnectionPath path2 = new ConnectionPath(ConnectorType.LK);
        path2.addPoint(0, 0);
        path2.addPoint(10, 0);
        
        assertEquals(path1, path2);
    }
    
    @Test
    public void testPath_Inequality_DifferentType() {
        ConnectionPath path1 = new ConnectionPath(ConnectorType.LK);
        path1.addPoint(0, 0);
        
        ConnectionPath path2 = new ConnectionPath(ConnectorType.CONTROL);
        path2.addPoint(0, 0);
        
        assertNotEquals(path1, path2);
    }
    
    @Test
    public void testPath_Inequality_DifferentPoints() {
        ConnectionPath path1 = new ConnectionPath(ConnectorType.LK);
        path1.addPoint(0, 0);
        path1.addPoint(10, 0);
        
        ConnectionPath path2 = new ConnectionPath(ConnectorType.LK);
        path2.addPoint(0, 0);
        path2.addPoint(5, 0);
        
        assertNotEquals(path1, path2);
    }
    
    // ===========================================
    // Edge Cases
    // ===========================================
    
    @Test
    public void testNegativeCoordinates() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(-10, -5);
        path.addPoint(10, -5);
        
        assertEquals(20, path.getTotalLength());
        assertTrue(path.containsPoint(0, -5));
    }
    
    @Test
    public void testLargeCoordinates() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(0, 0);
        path.addPoint(1000, 0);
        path.addPoint(1000, 500);
        
        assertEquals(1500, path.getTotalLength());
        assertTrue(path.isValid());
    }
    
    @Test
    public void testZeroLengthPath() {
        ConnectionPath path = new ConnectionPath(ConnectorType.LK);
        path.addPoint(5, 5);
        path.addPoint(5, 5);  // Same point
        
        assertEquals(0, path.getTotalLength());
    }
}
