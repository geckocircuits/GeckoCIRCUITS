/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

import org.junit.Before;
import org.junit.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for WirePathCalculator.
 *
 * Tests cover:
 * - L-shaped path calculation (horizontal-first and vertical-first)
 * - Direct path detection for aligned points
 * - Wire pen shape calculation
 * - Path validation (orthogonality)
 * - Corner counting
 * - Path simplification
 * - Path length calculation
 * - Edge cases and null handling
 *
 * @author GeckoCIRCUITS Team
 */
public class WirePathCalculatorTest {

    private WirePathCalculator calculator;

    @Before
    public void setUp() {
        calculator = new WirePathCalculator();
    }

    // ========== WirePath class tests ==========

    @Test
    public void testWirePath_GetAllPoints() {
        List<GridPoint> intermediate = Arrays.asList(new GridPoint(5, 0), new GridPoint(10, 0));
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 5), intermediate, true);

        List<GridPoint> allPoints = path.getAllPoints();

        assertEquals(4, allPoints.size());
        assertEquals(new GridPoint(0, 0), allPoints.get(0));
        assertEquals(new GridPoint(5, 0), allPoints.get(1));
        assertEquals(new GridPoint(10, 0), allPoints.get(2));
        assertEquals(new GridPoint(10, 5), allPoints.get(3));
    }

    @Test
    public void testWirePath_GetCornerPoint_HorizontalFirst() {
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 5), new ArrayList<>(), true);

        GridPoint corner = path.getCornerPoint();

        assertNotNull(corner);
        assertEquals(10, corner.x);  // end.x
        assertEquals(0, corner.y);   // start.y
    }

    @Test
    public void testWirePath_GetCornerPoint_VerticalFirst() {
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 5), new ArrayList<>(), false);

        GridPoint corner = path.getCornerPoint();

        assertNotNull(corner);
        assertEquals(0, corner.x);   // start.x
        assertEquals(5, corner.y);   // end.y
    }

    @Test
    public void testWirePath_GetCornerPoint_DirectLine() {
        // Horizontal line - no corner
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 0), new ArrayList<>(), true);

        assertNull(path.getCornerPoint());
    }

    @Test
    public void testWirePath_GetSegmentCount_DirectLine() {
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 0), new ArrayList<>(), true);

        assertEquals(1, path.getSegmentCount());
    }

    @Test
    public void testWirePath_GetSegmentCount_LShape() {
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 5), new ArrayList<>(), true);

        assertEquals(2, path.getSegmentCount());
    }

    @Test
    public void testWirePath_GetTotalDistance() {
        WirePathCalculator.WirePath path = new WirePathCalculator.WirePath(
                new GridPoint(0, 0), new GridPoint(10, 5), new ArrayList<>(), true);

        assertEquals(15, path.getTotalDistance());  // |10-0| + |5-0|
    }

    // ========== calculatePath tests ==========

    @Test
    public void testCalculatePath_HorizontalFirst_PositiveDirection() {
        WirePathCalculator.WirePath path = calculator.calculatePath(0, 0, 3, 2, true);

        assertEquals(new GridPoint(0, 0), path.getStart());
        assertEquals(new GridPoint(3, 2), path.getEnd());
        assertTrue(path.isHorizontalFirst());

        // Check intermediate points: should go right first, then down
        List<GridPoint> intermediate = path.getIntermediatePoints();
        assertTrue(intermediate.size() > 0);

        // Verify path goes horizontal first
        GridPoint firstIntermediate = intermediate.get(0);
        assertEquals(0, firstIntermediate.y);  // Y should stay at start.y initially
    }

    @Test
    public void testCalculatePath_VerticalFirst_PositiveDirection() {
        WirePathCalculator.WirePath path = calculator.calculatePath(0, 0, 3, 2, false);

        assertEquals(new GridPoint(0, 0), path.getStart());
        assertEquals(new GridPoint(3, 2), path.getEnd());
        assertFalse(path.isHorizontalFirst());

        // Check intermediate points: should go down first, then right
        List<GridPoint> intermediate = path.getIntermediatePoints();
        assertTrue(intermediate.size() > 0);

        // Verify path goes vertical first
        GridPoint firstIntermediate = intermediate.get(0);
        assertEquals(0, firstIntermediate.x);  // X should stay at start.x initially
    }

    @Test
    public void testCalculatePath_NegativeXDirection() {
        WirePathCalculator.WirePath path = calculator.calculatePath(5, 0, 2, 3, true);

        assertEquals(new GridPoint(5, 0), path.getStart());
        assertEquals(new GridPoint(2, 3), path.getEnd());

        // All points should progress from x=5 toward x=2
        List<GridPoint> all = path.getAllPoints();
        for (int i = 0; i < all.size() - 1; i++) {
            // X should never increase
            assertTrue(all.get(i).x >= all.get(i + 1).x || all.get(i).y != all.get(i + 1).y);
        }
    }

    @Test
    public void testCalculatePath_NegativeYDirection() {
        WirePathCalculator.WirePath path = calculator.calculatePath(0, 5, 3, 2, false);

        assertEquals(new GridPoint(0, 5), path.getStart());
        assertEquals(new GridPoint(3, 2), path.getEnd());

        // Vertical first means Y changes first (going up since end.y < start.y)
        List<GridPoint> intermediate = path.getIntermediatePoints();
        if (!intermediate.isEmpty()) {
            GridPoint first = intermediate.get(0);
            assertEquals(0, first.x);  // X stays at start initially
        }
    }

    @Test
    public void testCalculatePath_SamePoint() {
        WirePathCalculator.WirePath path = calculator.calculatePath(5, 5, 5, 5, true);

        assertEquals(new GridPoint(5, 5), path.getStart());
        assertEquals(new GridPoint(5, 5), path.getEnd());
        assertEquals(0, path.getTotalDistance());
        assertTrue(path.getIntermediatePoints().isEmpty());
    }

    @Test
    public void testCalculatePath_HorizontalLine() {
        WirePathCalculator.WirePath path = calculator.calculatePath(0, 5, 10, 5, true);

        assertEquals(new GridPoint(0, 5), path.getStart());
        assertEquals(new GridPoint(10, 5), path.getEnd());
        assertEquals(1, path.getSegmentCount());

        // All intermediate points should be on the same Y
        for (GridPoint p : path.getIntermediatePoints()) {
            assertEquals(5, p.y);
        }
    }

    @Test
    public void testCalculatePath_VerticalLine() {
        WirePathCalculator.WirePath path = calculator.calculatePath(5, 0, 5, 10, true);

        assertEquals(new GridPoint(5, 0), path.getStart());
        assertEquals(new GridPoint(5, 10), path.getEnd());
        assertEquals(1, path.getSegmentCount());

        // All intermediate points should be on the same X
        for (GridPoint p : path.getIntermediatePoints()) {
            assertEquals(5, p.x);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculatePath_NullStart() {
        calculator.calculatePath(null, new GridPoint(5, 5), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculatePath_NullEnd() {
        calculator.calculatePath(new GridPoint(0, 0), null, true);
    }

    // ========== calculatePathPoints tests ==========

    @Test
    public void testCalculatePathPoints_Basic() {
        List<GridPoint> points = calculator.calculatePathPoints(0, 0, 3, 2, true);

        assertNotNull(points);
        // Points should form path from (0,0) toward (3,2)
    }

    // ========== calculateDirectPath tests ==========

    @Test
    public void testCalculateDirectPath_Horizontal() {
        WirePathCalculator.WirePath path = calculator.calculateDirectPath(
                new GridPoint(0, 5), new GridPoint(10, 5));

        assertEquals(1, path.getSegmentCount());
        assertTrue(path.getIntermediatePoints().isEmpty() || 
                   path.getIntermediatePoints().stream().allMatch(p -> p.y == 5));
    }

    @Test
    public void testCalculateDirectPath_Vertical() {
        WirePathCalculator.WirePath path = calculator.calculateDirectPath(
                new GridPoint(5, 0), new GridPoint(5, 10));

        assertEquals(1, path.getSegmentCount());
    }

    @Test
    public void testCalculateDirectPath_NotAligned() {
        WirePathCalculator.WirePath path = calculator.calculateDirectPath(
                new GridPoint(0, 0), new GridPoint(5, 5));

        assertEquals(2, path.getSegmentCount());  // Will be L-shaped
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateDirectPath_NullStart() {
        calculator.calculateDirectPath(null, new GridPoint(5, 5));
    }

    // ========== isDirectPath tests ==========

    @Test
    public void testIsDirectPath_Horizontal() {
        assertTrue(calculator.isDirectPath(new GridPoint(0, 5), new GridPoint(10, 5)));
    }

    @Test
    public void testIsDirectPath_Vertical() {
        assertTrue(calculator.isDirectPath(new GridPoint(5, 0), new GridPoint(5, 10)));
    }

    @Test
    public void testIsDirectPath_NotAligned() {
        assertFalse(calculator.isDirectPath(new GridPoint(0, 0), new GridPoint(5, 5)));
    }

    @Test
    public void testIsDirectPath_SamePoint() {
        assertTrue(calculator.isDirectPath(new GridPoint(5, 5), new GridPoint(5, 5)));
    }

    @Test
    public void testIsDirectPath_NullStart() {
        assertFalse(calculator.isDirectPath(null, new GridPoint(5, 5)));
    }

    @Test
    public void testIsDirectPath_NullEnd() {
        assertFalse(calculator.isDirectPath(new GridPoint(0, 0), null));
    }

    // ========== calculateWirePenShape tests ==========

    @Test
    public void testCalculateWirePenShape_Basic() {
        GridPoint[] pen = calculator.calculateWirePenShape(10, 20, 16);

        assertNotNull(pen);
        assertEquals(4, pen.length);

        // Point 0 should be at grid position
        assertEquals(160, pen[0].x);  // 10 * 16
        assertEquals(320, pen[0].y);  // 20 * 16
    }

    @Test
    public void testCalculateWirePenShape_Origin() {
        GridPoint[] pen = calculator.calculateWirePenShape(0, 0, 16);

        assertEquals(0, pen[0].x);
        assertEquals(0, pen[0].y);
    }

    @Test
    public void testCalculateWirePenArrays_Basic() {
        int[][] arrays = calculator.calculateWirePenArrays(10, 20, 16);

        assertNotNull(arrays);
        assertEquals(2, arrays.length);
        assertEquals(4, arrays[0].length);  // xPoints
        assertEquals(4, arrays[1].length);  // yPoints

        // Check first point (tip)
        assertEquals(160, arrays[0][0]);  // 10 * 16
        assertEquals(320, arrays[1][0]);  // 20 * 16
    }

    // ========== isOrthogonalPath tests ==========

    @Test
    public void testIsOrthogonalPath_Horizontal() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(10, 0)
        );

        assertTrue(calculator.isOrthogonalPath(points));
    }

    @Test
    public void testIsOrthogonalPath_LShape() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(5, 5)
        );

        assertTrue(calculator.isOrthogonalPath(points));
    }

    @Test
    public void testIsOrthogonalPath_Diagonal() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 5),  // Diagonal!
                new GridPoint(10, 5)
        );

        assertFalse(calculator.isOrthogonalPath(points));
    }

    @Test
    public void testIsOrthogonalPath_Empty() {
        assertTrue(calculator.isOrthogonalPath(new ArrayList<>()));
    }

    @Test
    public void testIsOrthogonalPath_SinglePoint() {
        assertTrue(calculator.isOrthogonalPath(Arrays.asList(new GridPoint(5, 5))));
    }

    @Test
    public void testIsOrthogonalPath_Null() {
        assertTrue(calculator.isOrthogonalPath(null));
    }

    // ========== countCorners tests ==========

    @Test
    public void testCountCorners_StraightLine() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(10, 0)
        );

        assertEquals(0, calculator.countCorners(points));
    }

    @Test
    public void testCountCorners_OneCorner() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(5, 5)
        );

        assertEquals(1, calculator.countCorners(points));
    }

    @Test
    public void testCountCorners_TwoCorners() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(5, 5),
                new GridPoint(10, 5)
        );

        assertEquals(2, calculator.countCorners(points));
    }

    @Test
    public void testCountCorners_Staircase() {
        // Staircase pattern: right, up, right, up
        // Corners are at (2,0), (2,2), (4,2) - 3 corners
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(2, 0),
                new GridPoint(2, 2),
                new GridPoint(4, 2),
                new GridPoint(4, 4)
        );

        assertEquals(3, calculator.countCorners(points));
    }

    @Test
    public void testCountCorners_TwoPoints() {
        assertEquals(0, calculator.countCorners(Arrays.asList(
                new GridPoint(0, 0), new GridPoint(5, 0))));
    }

    @Test
    public void testCountCorners_Null() {
        assertEquals(0, calculator.countCorners(null));
    }

    // ========== simplifyPath tests ==========

    @Test
    public void testSimplifyPath_NoRedundant() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(5, 5)
        );

        List<GridPoint> simplified = calculator.simplifyPath(points);

        assertEquals(3, simplified.size());
    }

    @Test
    public void testSimplifyPath_RemoveRedundant() {
        // Points (1,0), (2,0), (3,0), (4,0) are all on same horizontal line
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(1, 0),
                new GridPoint(2, 0),
                new GridPoint(3, 0),
                new GridPoint(4, 0),
                new GridPoint(5, 0),
                new GridPoint(5, 5)
        );

        List<GridPoint> simplified = calculator.simplifyPath(points);

        // Should keep start, corner at (5,0), and end
        assertEquals(3, simplified.size());
        assertEquals(new GridPoint(0, 0), simplified.get(0));
        assertEquals(new GridPoint(5, 0), simplified.get(1));
        assertEquals(new GridPoint(5, 5), simplified.get(2));
    }

    @Test
    public void testSimplifyPath_TwoPoints() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 5)
        );

        List<GridPoint> simplified = calculator.simplifyPath(points);

        assertEquals(2, simplified.size());
    }

    @Test
    public void testSimplifyPath_Null() {
        List<GridPoint> simplified = calculator.simplifyPath(null);

        assertTrue(simplified.isEmpty());
    }

    // ========== calculatePathLength tests ==========

    @Test
    public void testCalculatePathLength_Horizontal() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(10, 0)
        );

        assertEquals(10, calculator.calculatePathLength(points));
    }

    @Test
    public void testCalculatePathLength_Vertical() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(0, 10)
        );

        assertEquals(10, calculator.calculatePathLength(points));
    }

    @Test
    public void testCalculatePathLength_LShape() {
        List<GridPoint> points = Arrays.asList(
                new GridPoint(0, 0),
                new GridPoint(5, 0),
                new GridPoint(5, 5)
        );

        assertEquals(10, calculator.calculatePathLength(points));  // 5 + 5
    }

    @Test
    public void testCalculatePathLength_SinglePoint() {
        List<GridPoint> points = Arrays.asList(new GridPoint(5, 5));

        assertEquals(0, calculator.calculatePathLength(points));
    }

    @Test
    public void testCalculatePathLength_Empty() {
        assertEquals(0, calculator.calculatePathLength(new ArrayList<>()));
    }

    @Test
    public void testCalculatePathLength_Null() {
        assertEquals(0, calculator.calculatePathLength(null));
    }

    // ========== Integration tests ==========

    @Test
    public void testCalculatePath_ThenValidate() {
        WirePathCalculator.WirePath path = calculator.calculatePath(0, 0, 10, 5, true);
        List<GridPoint> allPoints = path.getAllPoints();

        // Path should be orthogonal
        assertTrue(calculator.isOrthogonalPath(allPoints));

        // Total length should match Manhattan distance
        assertEquals(15, calculator.calculatePathLength(allPoints));
    }

    @Test
    public void testCalculatePath_LargeDistance() {
        WirePathCalculator.WirePath path = calculator.calculatePath(0, 0, 100, 50, true);

        assertEquals(150, path.getTotalDistance());
        assertTrue(calculator.isOrthogonalPath(path.getAllPoints()));
    }

    @Test
    public void testCalculatePath_NegativeCoordinates() {
        WirePathCalculator.WirePath path = calculator.calculatePath(-5, -10, 5, 10, true);

        assertEquals(new GridPoint(-5, -10), path.getStart());
        assertEquals(new GridPoint(5, 10), path.getEnd());
        assertEquals(30, path.getTotalDistance());  // |10| + |20|
    }
}
