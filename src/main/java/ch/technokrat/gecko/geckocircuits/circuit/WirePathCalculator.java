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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates wire/connection paths between two points on a circuit schematic.
 *
 * This class extracts wire routing logic from SchematicEditor2 to provide
 * testable, reusable methods for computing L-shaped paths between points.
 *
 * Wire paths in GeckoCIRCUITS are orthogonal (horizontal/vertical only) and
 * follow an L-shaped or staircase pattern. This class computes the intermediate
 * points needed to draw such paths.
 *
 * Supported path types:
 * - Horizontal-first: Goes horizontal first, then vertical
 * - Vertical-first: Goes vertical first, then horizontal
 * - Direct line: When start and end are aligned (same X or Y)
 *
 * All methods are stateless and operate on input parameters, making them
 * easy to test without GUI dependencies.
 * 
 * Note: This class is GUI-free - it uses GridPoint instead of java.awt.Point.
 *
 * @author GeckoCIRCUITS Team
 * @see SchematicEditor2
 * @see Verbindung
 */
public class WirePathCalculator {

    /**
     * Represents a calculated wire path between two points.
     */
    public static class WirePath {
        private final GridPoint start;
        private final GridPoint end;
        private final List<GridPoint> intermediatePoints;
        private final boolean horizontalFirst;

        /**
         * Creates a new wire path.
         *
         * @param start the starting point
         * @param end the ending point
         * @param intermediatePoints list of points between start and end
         * @param horizontalFirst true if path goes horizontal first
         */
        public WirePath(GridPoint start, GridPoint end, List<GridPoint> intermediatePoints, boolean horizontalFirst) {
            this.start = new GridPoint(start);
            this.end = new GridPoint(end);
            this.intermediatePoints = new ArrayList<>(intermediatePoints);
            this.horizontalFirst = horizontalFirst;
        }

        public GridPoint getStart() {
            return new GridPoint(start);
        }

        public GridPoint getEnd() {
            return new GridPoint(end);
        }

        /**
         * Returns all points in order: start, intermediate points, end.
         */
        public List<GridPoint> getAllPoints() {
            List<GridPoint> allPoints = new ArrayList<>();
            allPoints.add(new GridPoint(start));
            for (GridPoint p : intermediatePoints) {
                allPoints.add(new GridPoint(p));
            }
            allPoints.add(new GridPoint(end));
            return allPoints;
        }

        /**
         * Returns only intermediate points (excluding start and end).
         */
        public List<GridPoint> getIntermediatePoints() {
            List<GridPoint> points = new ArrayList<>();
            for (GridPoint p : intermediatePoints) {
                points.add(new GridPoint(p));
            }
            return points;
        }

        public boolean isHorizontalFirst() {
            return horizontalFirst;
        }

        /**
         * Returns the number of segments in this path.
         * A direct line has 1 segment, L-shape has 2 segments.
         */
        public int getSegmentCount() {
            if (start.x == end.x || start.y == end.y) {
                return 1;  // Direct horizontal or vertical line
            }
            return 2;  // L-shaped path
        }

        /**
         * Returns the total Manhattan distance of this path.
         */
        public int getTotalDistance() {
            return Math.abs(end.x - start.x) + Math.abs(end.y - start.y);
        }

        /**
         * Returns the corner point of an L-shaped path, or null for direct lines.
         */
        public GridPoint getCornerPoint() {
            if (start.x == end.x || start.y == end.y) {
                return null;  // No corner for direct lines
            }
            if (horizontalFirst) {
                return new GridPoint(end.x, start.y);
            } else {
                return new GridPoint(start.x, end.y);
            }
        }
    }

    /**
     * Calculates an L-shaped wire path between two points.
     *
     * @param xStart X coordinate of start point
     * @param yStart Y coordinate of start point
     * @param xEnd X coordinate of end point
     * @param yEnd Y coordinate of end point
     * @param horizontalFirst if true, path goes horizontal first; otherwise vertical first
     * @return the calculated wire path
     */
    public WirePath calculatePath(int xStart, int yStart, int xEnd, int yEnd, boolean horizontalFirst) {
        return calculatePath(new GridPoint(xStart, yStart), new GridPoint(xEnd, yEnd), horizontalFirst);
    }

    /**
     * Calculates an L-shaped wire path between two points.
     *
     * @param start the starting point
     * @param end the ending point
     * @param horizontalFirst if true, path goes horizontal first; otherwise vertical first
     * @return the calculated wire path
     */
    public WirePath calculatePath(GridPoint start, GridPoint end, boolean horizontalFirst) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end points cannot be null");
        }

        List<GridPoint> intermediatePoints = new ArrayList<>();

        int xLength = end.x - start.x;
        int yLength = end.y - start.y;

        int xDir = (xLength >= 0) ? 1 : -1;
        int yDir = (yLength >= 0) ? 1 : -1;

        int i = 0, j = 0;

        if (horizontalFirst) {
            // Move horizontally first
            while (Math.abs(i - xLength) > 0) {
                intermediatePoints.add(new GridPoint(start.x + i, start.y + j));
                i += xDir;
            }
            // Then move vertically
            while (Math.abs(j - yLength) > 0) {
                intermediatePoints.add(new GridPoint(start.x + i, start.y + j));
                j += yDir;
            }
        } else {
            // Move vertically first
            while (Math.abs(j - yLength) > 0) {
                intermediatePoints.add(new GridPoint(start.x + i, start.y + j));
                j += yDir;
            }
            // Then move horizontally
            while (Math.abs(i - xLength) > 0) {
                intermediatePoints.add(new GridPoint(start.x + i, start.y + j));
                i += xDir;
            }
        }

        return new WirePath(start, end, intermediatePoints, horizontalFirst);
    }

    /**
     * Calculates path points as a simple list (for compatibility with existing code).
     *
     * Returns the sequence of points that form the wire path, excluding the
     * start point but including intermediate points. The end point is NOT included
     * (caller typically sets it separately via setzeEndKnoten).
     *
     * @param xStart X coordinate of start point
     * @param yStart Y coordinate of start point
     * @param xEnd X coordinate of end point
     * @param yEnd Y coordinate of end point
     * @param horizontalFirst if true, path goes horizontal first
     * @return list of intermediate points forming the path
     */
    public List<GridPoint> calculatePathPoints(int xStart, int yStart, int xEnd, int yEnd, boolean horizontalFirst) {
        WirePath path = calculatePath(xStart, yStart, xEnd, yEnd, horizontalFirst);
        return path.getIntermediatePoints();
    }

    /**
     * Calculates a direct path (no intermediate points) when start and end are aligned.
     *
     * @param start the starting point
     * @param end the ending point
     * @return the wire path (with empty intermediate points if aligned)
     */
    public WirePath calculateDirectPath(GridPoint start, GridPoint end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end points cannot be null");
        }

        // If aligned horizontally or vertically, no intermediate points needed
        if (start.x == end.x || start.y == end.y) {
            return new WirePath(start, end, Collections.emptyList(), start.y == end.y);
        }

        // Not aligned - default to horizontal first
        return calculatePath(start, end, true);
    }

    /**
     * Determines if a path between two points would be a direct line.
     *
     * @param start the starting point
     * @param end the ending point
     * @return true if points are aligned (same X or Y coordinate)
     */
    public boolean isDirectPath(GridPoint start, GridPoint end) {
        if (start == null || end == null) {
            return false;
        }
        return start.x == end.x || start.y == end.y;
    }

    /**
     * Calculates the wire pen cursor position for drawing wires.
     *
     * The wire pen is a small arrow/triangle shape that indicates the
     * current drawing position when creating connections.
     *
     * @param gridX the X grid coordinate
     * @param gridY the Y grid coordinate
     * @param gridSize the size of each grid cell in pixels (dpix)
     * @return array of points forming the pen shape [tip, upper, right, lower]
     */
    public GridPoint[] calculateWirePenShape(int gridX, int gridY, int gridSize) {
        GridPoint[] penPoints = new GridPoint[4];

        // Pen tip at grid position
        penPoints[0] = new GridPoint(gridSize * gridX, gridSize * gridY);

        // Upper point (above and to the right)
        penPoints[1] = new GridPoint((int)(gridSize * (gridX + 0.4)), (int)(gridSize * (gridY - 1.5)));

        // Right point (further right)
        penPoints[2] = new GridPoint((int)(gridSize * (gridX + 0.7)), (int)(gridSize * (gridY - 1.5)));

        // Lower point (slightly right of tip)
        penPoints[3] = new GridPoint((int)(gridSize * (gridX + 0.1)), gridSize * gridY);

        return penPoints;
    }

    /**
     * Calculates the wire pen cursor as separate X and Y arrays (for Polygon drawing).
     *
     * @param gridX the X grid coordinate
     * @param gridY the Y grid coordinate
     * @param gridSize the size of each grid cell in pixels
     * @return array of two int arrays: [xPoints, yPoints]
     */
    public int[][] calculateWirePenArrays(int gridX, int gridY, int gridSize) {
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        xPoints[0] = gridSize * gridX;
        yPoints[0] = gridSize * gridY;

        xPoints[1] = (int)(gridSize * (gridX + 0.4));
        yPoints[1] = (int)(gridSize * (gridY - 1.5));

        xPoints[2] = (int)(gridSize * (gridX + 0.7));
        yPoints[2] = (int)(gridSize * (gridY - 1.5));

        xPoints[3] = (int)(gridSize * (gridX + 0.1));
        yPoints[3] = gridSize * gridY;

        return new int[][] { xPoints, yPoints };
    }

    /**
     * Validates that a wire path is orthogonal (only horizontal/vertical segments).
     *
     * @param points the list of points forming the path
     * @return true if all segments are orthogonal
     */
    public boolean isOrthogonalPath(List<GridPoint> points) {
        if (points == null || points.size() < 2) {
            return true;  // Empty or single point is considered orthogonal
        }

        for (int i = 0; i < points.size() - 1; i++) {
            GridPoint current = points.get(i);
            GridPoint next = points.get(i + 1);

            // Each segment must be either horizontal or vertical
            if (current.x != next.x && current.y != next.y) {
                return false;  // Diagonal segment found
            }
        }

        return true;
    }

    /**
     * Counts the number of direction changes (corners) in a wire path.
     *
     * @param points the list of points forming the path
     * @return the number of corners/turns in the path
     */
    public int countCorners(List<GridPoint> points) {
        if (points == null || points.size() < 3) {
            return 0;
        }

        int corners = 0;
        for (int i = 1; i < points.size() - 1; i++) {
            GridPoint prev = points.get(i - 1);
            GridPoint current = points.get(i);
            GridPoint next = points.get(i + 1);

            // Check if direction changes at this point
            boolean prevHorizontal = (prev.y == current.y);
            boolean nextHorizontal = (current.y == next.y);

            if (prevHorizontal != nextHorizontal) {
                corners++;
            }
        }

        return corners;
    }

    /**
     * Simplifies a wire path by removing redundant intermediate points.
     *
     * Points are redundant if they lie on a straight line between neighbors.
     *
     * @param points the original list of points
     * @return simplified list with redundant points removed
     */
    public List<GridPoint> simplifyPath(List<GridPoint> points) {
        if (points == null || points.size() <= 2) {
            return points == null ? Collections.emptyList() : new ArrayList<>(points);
        }

        List<GridPoint> simplified = new ArrayList<>();
        simplified.add(new GridPoint(points.get(0)));

        for (int i = 1; i < points.size() - 1; i++) {
            GridPoint prev = points.get(i - 1);
            GridPoint current = points.get(i);
            GridPoint next = points.get(i + 1);

            // Keep point if it's a corner (direction changes)
            boolean prevHorizontal = (prev.y == current.y);
            boolean nextHorizontal = (current.y == next.y);

            if (prevHorizontal != nextHorizontal) {
                simplified.add(new GridPoint(current));
            }
        }

        simplified.add(new GridPoint(points.get(points.size() - 1)));
        return simplified;
    }

    /**
     * Calculates the total wire length of a path.
     *
     * @param points the list of points forming the path
     * @return total Manhattan distance of all segments
     */
    public int calculatePathLength(List<GridPoint> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }

        int totalLength = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            GridPoint current = points.get(i);
            GridPoint next = points.get(i + 1);
            totalLength += Math.abs(next.x - current.x) + Math.abs(next.y - current.y);
        }

        return totalLength;
    }
}
