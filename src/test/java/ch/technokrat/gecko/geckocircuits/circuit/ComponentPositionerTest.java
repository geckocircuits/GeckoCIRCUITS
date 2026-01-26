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

import java.awt.Point;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for ComponentPositioner.
 *
 * Tests cover:
 * - Anchor point finding for component groups
 * - Pixel to grid coordinate conversion with snapping
 * - Move delta calculations
 * - Bounding box calculations
 * - Point-in-rectangle tests
 * - Translation and distance calculations
 * - Edge cases and null handling
 *
 * @author GeckoCIRCUITS Team
 */
public class ComponentPositionerTest {

    private ComponentPositioner positioner;

    @Before
    public void setUp() {
        positioner = new ComponentPositioner();
    }

    // ========== PositionProvider implementation for testing ==========

    private static class TestPositionProvider implements ComponentPositioner.PositionProvider {
        private final Point position;

        TestPositionProvider(int x, int y) {
            this.position = new Point(x, y);
        }

        TestPositionProvider(Point p) {
            this.position = p;
        }

        @Override
        public Point getPosition() {
            return position;
        }
    }

    // ========== findAnchorPoint tests ==========

    @Test
    public void testFindAnchorPoint_SinglePosition() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(10, 20)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(10, anchor.x);
        assertEquals(20, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_MultiplePositions() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(15, 25),
                new TestPositionProvider(5, 30),
                new TestPositionProvider(20, 10)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(5, anchor.x);
        assertEquals(10, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_IgnoresNegativeCoordinates() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(-5, -10),  // Should be ignored
                new TestPositionProvider(15, 25),
                new TestPositionProvider(10, 20)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(10, anchor.x);
        assertEquals(20, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_IgnoresNegativeXOnly() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(-5, 10),  // Should be ignored (negative X)
                new TestPositionProvider(15, 25)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(15, anchor.x);
        assertEquals(25, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_IgnoresNegativeYOnly() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(5, -10),  // Should be ignored (negative Y)
                new TestPositionProvider(15, 25)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(15, anchor.x);
        assertEquals(25, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_AllNegativeReturnsMaxValue() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(-5, -10),
                new TestPositionProvider(-15, -25)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(Integer.MAX_VALUE, anchor.x);
        assertEquals(Integer.MAX_VALUE, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_EmptyList() {
        List<TestPositionProvider> positions = new ArrayList<>();

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(Integer.MAX_VALUE, anchor.x);
        assertEquals(Integer.MAX_VALUE, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_NullList() {
        Point anchor = positioner.findAnchorPoint(null);

        assertEquals(Integer.MAX_VALUE, anchor.x);
        assertEquals(Integer.MAX_VALUE, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_ZeroCoordinates() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(0, 0),
                new TestPositionProvider(10, 10)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(0, anchor.x);
        assertEquals(0, anchor.y);
    }

    @Test
    public void testFindAnchorPoint_NullPositionInList() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(10, 20),
                new TestPositionProvider((Point) null),
                new TestPositionProvider(5, 15)
        );

        Point anchor = positioner.findAnchorPoint(positions);

        assertEquals(5, anchor.x);
        assertEquals(15, anchor.y);
    }

    // ========== findAnchorPointFromPoints tests ==========

    @Test
    public void testFindAnchorPointFromPoints_Basic() {
        List<Point> points = Arrays.asList(
                new Point(15, 25),
                new Point(5, 30),
                new Point(20, 10)
        );

        Point anchor = positioner.findAnchorPointFromPoints(points);

        assertEquals(5, anchor.x);
        assertEquals(10, anchor.y);
    }

    @Test
    public void testFindAnchorPointFromPoints_EmptyList() {
        Point anchor = positioner.findAnchorPointFromPoints(new ArrayList<>());

        assertEquals(Integer.MAX_VALUE, anchor.x);
        assertEquals(Integer.MAX_VALUE, anchor.y);
    }

    @Test
    public void testFindAnchorPointFromPoints_Null() {
        Point anchor = positioner.findAnchorPointFromPoints(null);

        assertEquals(Integer.MAX_VALUE, anchor.x);
        assertEquals(Integer.MAX_VALUE, anchor.y);
    }

    // ========== pixelToGrid tests ==========

    @Test
    public void testPixelToGrid_ExactGridPoint() {
        Point grid = positioner.pixelToGrid(100, 200, 10);

        assertEquals(10, grid.x);
        assertEquals(20, grid.y);
    }

    @Test
    public void testPixelToGrid_NoSnap() {
        // 95 / 10 = 9.5, which doesn't snap to 10
        Point grid = positioner.pixelToGrid(95, 195, 10);

        assertEquals(9, grid.x);
        assertEquals(19, grid.y);
    }

    @Test
    public void testPixelToGrid_SnapToNextLine() {
        // 96 / 10 = 9.6, which is within 0.5 of 10, so snaps
        Point grid = positioner.pixelToGrid(96, 196, 10, 0.5);

        assertEquals(10, grid.x);
        assertEquals(20, grid.y);
    }

    @Test
    public void testPixelToGrid_CustomClickRadius() {
        // With radius 0.4, 97/10=9.7 should snap (distance to 10 is 0.3 < 0.4)
        Point grid = positioner.pixelToGrid(97, 197, 10, 0.4);

        assertEquals(10, grid.x);
        assertEquals(20, grid.y);
    }

    @Test
    public void testPixelToGrid_CustomClickRadius_NoSnap() {
        // With radius 0.2, 97/10=9.7 should NOT snap (0.3 > 0.2)
        Point grid = positioner.pixelToGrid(97, 197, 10, 0.2);

        assertEquals(9, grid.x);
        assertEquals(19, grid.y);
    }

    @Test
    public void testPixelToGrid_ZeroPixels() {
        Point grid = positioner.pixelToGrid(0, 0, 10);

        assertEquals(0, grid.x);
        assertEquals(0, grid.y);
    }

    @Test
    public void testPixelToGrid_LargeValues() {
        Point grid = positioner.pixelToGrid(1000000, 2000000, 100);

        assertEquals(10000, grid.x);
        assertEquals(20000, grid.y);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPixelToGrid_ZeroGridSize() {
        positioner.pixelToGrid(100, 100, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPixelToGrid_NegativeGridSize() {
        positioner.pixelToGrid(100, 100, -10);
    }

    @Test
    public void testPixelToGrid_DefaultClickRadius() {
        // Should use DEFAULT_CLICK_RADIUS (0.5)
        // 96/10 = 9.6, distance to 10 is 0.4 < 0.5, so snaps
        Point grid = positioner.pixelToGrid(96, 96, 10);

        assertEquals(10, grid.x);
        assertEquals(10, grid.y);
    }

    // ========== calculateMoveDelta tests ==========

    @Test
    public void testCalculateMoveDelta_Basic() {
        Point delta = positioner.calculateMoveDelta(new Point(15, 25), new Point(10, 20));

        assertEquals(5, delta.x);
        assertEquals(5, delta.y);
    }

    @Test
    public void testCalculateMoveDelta_Negative() {
        Point delta = positioner.calculateMoveDelta(new Point(5, 10), new Point(15, 25));

        assertEquals(-10, delta.x);
        assertEquals(-15, delta.y);
    }

    @Test
    public void testCalculateMoveDelta_Zero() {
        Point delta = positioner.calculateMoveDelta(new Point(10, 20), new Point(10, 20));

        assertEquals(0, delta.x);
        assertEquals(0, delta.y);
    }

    @Test
    public void testCalculateMoveDelta_NullCurrent() {
        Point delta = positioner.calculateMoveDelta(null, new Point(10, 20));

        assertEquals(0, delta.x);
        assertEquals(0, delta.y);
    }

    @Test
    public void testCalculateMoveDelta_NullStart() {
        Point delta = positioner.calculateMoveDelta(new Point(10, 20), null);

        assertEquals(0, delta.x);
        assertEquals(0, delta.y);
    }

    @Test
    public void testCalculateMoveDelta_BothNull() {
        Point delta = positioner.calculateMoveDelta(null, null);

        assertEquals(0, delta.x);
        assertEquals(0, delta.y);
    }

    // ========== calculateBoundingBox tests ==========

    @Test
    public void testCalculateBoundingBox_SinglePoint() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(10, 20)
        );

        int[] bounds = positioner.calculateBoundingBox(positions);

        assertNotNull(bounds);
        assertEquals(4, bounds.length);
        assertEquals(10, bounds[0]);  // minX
        assertEquals(20, bounds[1]);  // minY
        assertEquals(10, bounds[2]);  // maxX
        assertEquals(20, bounds[3]);  // maxY
    }

    @Test
    public void testCalculateBoundingBox_MultiplePoints() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(5, 10),
                new TestPositionProvider(25, 30),
                new TestPositionProvider(15, 20)
        );

        int[] bounds = positioner.calculateBoundingBox(positions);

        assertNotNull(bounds);
        assertEquals(5, bounds[0]);   // minX
        assertEquals(10, bounds[1]);  // minY
        assertEquals(25, bounds[2]);  // maxX
        assertEquals(30, bounds[3]);  // maxY
    }

    @Test
    public void testCalculateBoundingBox_IncludesNegativeCoordinates() {
        // Unlike findAnchorPoint, bounding box includes negative coordinates
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(-5, -10),
                new TestPositionProvider(15, 25)
        );

        int[] bounds = positioner.calculateBoundingBox(positions);

        assertNotNull(bounds);
        assertEquals(-5, bounds[0]);   // minX
        assertEquals(-10, bounds[1]);  // minY
        assertEquals(15, bounds[2]);   // maxX
        assertEquals(25, bounds[3]);   // maxY
    }

    @Test
    public void testCalculateBoundingBox_EmptyList() {
        assertNull(positioner.calculateBoundingBox(new ArrayList<>()));
    }

    @Test
    public void testCalculateBoundingBox_NullList() {
        assertNull(positioner.calculateBoundingBox(null));
    }

    @Test
    public void testCalculateBoundingBox_AllNullPositions() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider((Point) null),
                new TestPositionProvider((Point) null)
        );

        assertNull(positioner.calculateBoundingBox(positions));
    }

    // ========== isPointInRectangle tests ==========

    @Test
    public void testIsPointInRectangle_Inside() {
        assertTrue(positioner.isPointInRectangle(
                new Point(15, 25),
                new Point(10, 20),
                new Point(30, 40)
        ));
    }

    @Test
    public void testIsPointInRectangle_OnBoundary() {
        assertTrue(positioner.isPointInRectangle(
                new Point(10, 20),  // On corner
                new Point(10, 20),
                new Point(30, 40)
        ));
    }

    @Test
    public void testIsPointInRectangle_OnEdge() {
        assertTrue(positioner.isPointInRectangle(
                new Point(20, 20),  // On top edge
                new Point(10, 20),
                new Point(30, 40)
        ));
    }

    @Test
    public void testIsPointInRectangle_Outside() {
        assertFalse(positioner.isPointInRectangle(
                new Point(5, 25),  // Outside to the left
                new Point(10, 20),
                new Point(30, 40)
        ));
    }

    @Test
    public void testIsPointInRectangle_ReversedCorners() {
        // Should work even if corners are specified in different order
        assertTrue(positioner.isPointInRectangle(
                new Point(15, 25),
                new Point(30, 40),  // Bottom-right first
                new Point(10, 20)   // Top-left second
        ));
    }

    @Test
    public void testIsPointInRectangle_NullPoint() {
        assertFalse(positioner.isPointInRectangle(
                null,
                new Point(10, 20),
                new Point(30, 40)
        ));
    }

    @Test
    public void testIsPointInRectangle_NullCorner() {
        assertFalse(positioner.isPointInRectangle(
                new Point(15, 25),
                null,
                new Point(30, 40)
        ));
    }

    // ========== translatePoint tests ==========

    @Test
    public void testTranslatePoint_Basic() {
        Point result = positioner.translatePoint(new Point(10, 20), 5, 10);

        assertEquals(15, result.x);
        assertEquals(30, result.y);
    }

    @Test
    public void testTranslatePoint_NegativeDelta() {
        Point result = positioner.translatePoint(new Point(10, 20), -5, -10);

        assertEquals(5, result.x);
        assertEquals(10, result.y);
    }

    @Test
    public void testTranslatePoint_ZeroDelta() {
        Point result = positioner.translatePoint(new Point(10, 20), 0, 0);

        assertEquals(10, result.x);
        assertEquals(20, result.y);
    }

    @Test
    public void testTranslatePoint_NullOriginal() {
        Point result = positioner.translatePoint(null, 5, 10);

        assertEquals(5, result.x);
        assertEquals(10, result.y);
    }

    // ========== findCenterPoint tests ==========

    @Test
    public void testFindCenterPoint_SinglePoint() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(10, 20)
        );

        Point center = positioner.findCenterPoint(positions);

        assertNotNull(center);
        assertEquals(10, center.x);
        assertEquals(20, center.y);
    }

    @Test
    public void testFindCenterPoint_MultiplePoints() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(0, 0),
                new TestPositionProvider(20, 40)
        );

        Point center = positioner.findCenterPoint(positions);

        assertNotNull(center);
        assertEquals(10, center.x);
        assertEquals(20, center.y);
    }

    @Test
    public void testFindCenterPoint_EmptyList() {
        assertNull(positioner.findCenterPoint(new ArrayList<>()));
    }

    @Test
    public void testFindCenterPoint_NullList() {
        assertNull(positioner.findCenterPoint(null));
    }

    // ========== calculateManhattanDistance tests ==========

    @Test
    public void testCalculateManhattanDistance_Basic() {
        int distance = positioner.calculateManhattanDistance(
                new Point(0, 0),
                new Point(3, 4)
        );

        assertEquals(7, distance);  // |3| + |4|
    }

    @Test
    public void testCalculateManhattanDistance_SamePoint() {
        int distance = positioner.calculateManhattanDistance(
                new Point(10, 20),
                new Point(10, 20)
        );

        assertEquals(0, distance);
    }

    @Test
    public void testCalculateManhattanDistance_NegativeCoordinates() {
        int distance = positioner.calculateManhattanDistance(
                new Point(-5, -10),
                new Point(5, 10)
        );

        assertEquals(30, distance);  // |10| + |20|
    }

    @Test
    public void testCalculateManhattanDistance_NullPoint() {
        assertEquals(0, positioner.calculateManhattanDistance(null, new Point(5, 10)));
        assertEquals(0, positioner.calculateManhattanDistance(new Point(5, 10), null));
    }

    // ========== calculateEuclideanDistance tests ==========

    @Test
    public void testCalculateEuclideanDistance_345Triangle() {
        double distance = positioner.calculateEuclideanDistance(
                new Point(0, 0),
                new Point(3, 4)
        );

        assertEquals(5.0, distance, 0.0001);
    }

    @Test
    public void testCalculateEuclideanDistance_SamePoint() {
        double distance = positioner.calculateEuclideanDistance(
                new Point(10, 20),
                new Point(10, 20)
        );

        assertEquals(0.0, distance, 0.0001);
    }

    @Test
    public void testCalculateEuclideanDistance_NullPoint() {
        assertEquals(0.0, positioner.calculateEuclideanDistance(null, new Point(5, 10)), 0.0001);
        assertEquals(0.0, positioner.calculateEuclideanDistance(new Point(5, 10), null), 0.0001);
    }

    // ========== snapToGrid tests ==========

    @Test
    public void testSnapToGrid_ExactGridPoint() {
        Point snapped = positioner.snapToGrid(new Point(20, 30), 10);

        assertEquals(20, snapped.x);
        assertEquals(30, snapped.y);
    }

    @Test
    public void testSnapToGrid_RoundDown() {
        Point snapped = positioner.snapToGrid(new Point(24, 34), 10);

        assertEquals(20, snapped.x);
        assertEquals(30, snapped.y);
    }

    @Test
    public void testSnapToGrid_RoundUp() {
        Point snapped = positioner.snapToGrid(new Point(26, 36), 10);

        assertEquals(30, snapped.x);
        assertEquals(40, snapped.y);
    }

    @Test
    public void testSnapToGrid_NullPoint() {
        assertNull(positioner.snapToGrid(null, 10));
    }

    @Test
    public void testSnapToGrid_ZeroGridSize() {
        Point original = new Point(25, 35);
        Point result = positioner.snapToGrid(original, 0);
        assertEquals(original, result);
    }

    // ========== normalizeRectangle tests ==========

    @Test
    public void testNormalizeRectangle_AlreadyNormalized() {
        Point[] result = positioner.normalizeRectangle(
                new Point(10, 20),
                new Point(30, 40)
        );

        assertEquals(10, result[0].x);
        assertEquals(20, result[0].y);
        assertEquals(30, result[1].x);
        assertEquals(40, result[1].y);
    }

    @Test
    public void testNormalizeRectangle_Reversed() {
        Point[] result = positioner.normalizeRectangle(
                new Point(30, 40),
                new Point(10, 20)
        );

        assertEquals(10, result[0].x);
        assertEquals(20, result[0].y);
        assertEquals(30, result[1].x);
        assertEquals(40, result[1].y);
    }

    @Test
    public void testNormalizeRectangle_MixedCorners() {
        // Top-right and bottom-left corners
        Point[] result = positioner.normalizeRectangle(
                new Point(30, 20),
                new Point(10, 40)
        );

        assertEquals(10, result[0].x);
        assertEquals(20, result[0].y);
        assertEquals(30, result[1].x);
        assertEquals(40, result[1].y);
    }

    @Test
    public void testNormalizeRectangle_NullCorner() {
        Point[] result = positioner.normalizeRectangle(
                null,
                new Point(10, 20)
        );

        assertNull(result[0]);
        assertEquals(10, result[1].x);
    }

    // ========== Edge case: Large coordinate values ==========

    @Test
    public void testLargeCoordinates() {
        List<TestPositionProvider> positions = Arrays.asList(
                new TestPositionProvider(Integer.MAX_VALUE - 100, Integer.MAX_VALUE - 200),
                new TestPositionProvider(100, 200)
        );

        Point anchor = positioner.findAnchorPoint(positions);
        assertEquals(100, anchor.x);
        assertEquals(200, anchor.y);

        int[] bounds = positioner.calculateBoundingBox(positions);
        assertNotNull(bounds);
        assertEquals(100, bounds[0]);
        assertEquals(200, bounds[1]);
        assertEquals(Integer.MAX_VALUE - 100, bounds[2]);
        assertEquals(Integer.MAX_VALUE - 200, bounds[3]);
    }
}
