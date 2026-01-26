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

import java.awt.Point;
import java.util.*;

/**
 * Handles position calculations for circuit components.
 *
 * This class extracts position-related logic from SchematicEditor2 to provide
 * testable, reusable methods for:
 *
 * 1. Finding anchor points for component groups (upper-left corner)
 * 2. Converting mouse coordinates to grid/raster coordinates
 * 3. Calculating move deltas between positions
 * 4. Computing bounding boxes for component selections
 *
 * All methods are stateless and operate on input parameters, making them
 * easy to test without GUI dependencies.
 *
 * @author GeckoCIRCUITS Team
 * @see SchematicEditor2
 */
public class ComponentPositioner {

    /** Default click radius for snap-to-grid detection (0.0 to 1.0) */
    public static final double DEFAULT_CLICK_RADIUS = 0.5;

    /**
     * Callback interface for getting terminal positions.
     * Used to decouple from concrete terminal implementations.
     */
    public interface PositionProvider {
        /**
         * Returns the position of this object.
         * @return the position point
         */
        Point getPosition();
    }

    /**
     * Finds the anchor point (upper-left corner) for a collection of positions.
     *
     * The anchor point is the minimum X and Y coordinates across all provided
     * positions. This is used as the reference point for move/copy operations.
     *
     * Positions with negative coordinates are ignored (e.g., thermal ambient
     * temperature nodes can have negative coordinates).
     *
     * @param positions collection of position providers
     * @return the upper-left anchor point, or (MAX_VALUE, MAX_VALUE) if no valid positions
     */
    public Point findAnchorPoint(Collection<? extends PositionProvider> positions) {
        if (positions == null || positions.isEmpty()) {
            return new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (PositionProvider provider : positions) {
            Point point = provider.getPosition();
            if (point == null) {
                continue;
            }

            // Skip negative coordinates (special nodes like thermal ambient)
            if (point.x < 0 || point.y < 0) {
                continue;
            }

            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
        }

        return new Point(minX, minY);
    }

    /**
     * Finds the anchor point for a list of raw Point objects.
     *
     * @param points list of position points
     * @return the upper-left anchor point
     */
    public Point findAnchorPointFromPoints(List<Point> points) {
        if (points == null || points.isEmpty()) {
            return new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (Point point : points) {
            if (point == null) {
                continue;
            }

            // Skip negative coordinates
            if (point.x < 0 || point.y < 0) {
                continue;
            }

            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
        }

        return new Point(minX, minY);
    }

    /**
     * Converts pixel coordinates to grid coordinates with snap-to-grid behavior.
     *
     * Uses the default click radius (0.5) for snap detection.
     *
     * @param pixelX the X coordinate in pixels
     * @param pixelY the Y coordinate in pixels
     * @param gridSize the size of each grid cell in pixels
     * @return the grid coordinates
     */
    public Point pixelToGrid(int pixelX, int pixelY, int gridSize) {
        return pixelToGrid(pixelX, pixelY, gridSize, DEFAULT_CLICK_RADIUS);
    }

    /**
     * Converts pixel coordinates to grid coordinates with snap-to-grid behavior.
     *
     * If the click is within the specified radius of the next grid line,
     * the coordinate snaps to that grid line.
     *
     * @param pixelX the X coordinate in pixels
     * @param pixelY the Y coordinate in pixels
     * @param gridSize the size of each grid cell in pixels
     * @param clickRadius the snap radius (0.0 to 1.0, as fraction of grid size)
     * @return the grid coordinates
     */
    public Point pixelToGrid(int pixelX, int pixelY, int gridSize, double clickRadius) {
        if (gridSize <= 0) {
            throw new IllegalArgumentException("Grid size must be positive: " + gridSize);
        }

        int gridX = pixelX / gridSize;
        int gridY = pixelY / gridSize;

        // Calculate fractional position
        double fracX = (double) pixelX / gridSize;
        double fracY = (double) pixelY / gridSize;

        // Snap to next grid line if within click radius
        if (Math.abs((gridX + 1) - fracX) < clickRadius) {
            gridX++;
        }
        if (Math.abs((gridY + 1) - fracY) < clickRadius) {
            gridY++;
        }

        return new Point(gridX, gridY);
    }

    /**
     * Calculates the movement delta between two points.
     *
     * @param currentPosition the current position
     * @param startPosition the starting position (anchor)
     * @return the delta (currentPosition - startPosition)
     */
    public Point calculateMoveDelta(Point currentPosition, Point startPosition) {
        if (currentPosition == null || startPosition == null) {
            return new Point(0, 0);
        }
        return new Point(
                currentPosition.x - startPosition.x,
                currentPosition.y - startPosition.y
        );
    }

    /**
     * Calculates the bounding box for a collection of positions.
     *
     * @param positions collection of position providers
     * @return array of [minX, minY, maxX, maxY], or null if no valid positions
     */
    public int[] calculateBoundingBox(Collection<? extends PositionProvider> positions) {
        if (positions == null || positions.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hasValidPoints = false;

        for (PositionProvider provider : positions) {
            Point point = provider.getPosition();
            if (point == null) {
                continue;
            }

            hasValidPoints = true;
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }

        if (!hasValidPoints) {
            return null;
        }

        return new int[]{minX, minY, maxX, maxY};
    }

    /**
     * Checks if a point is within a rectangular region.
     *
     * The region is defined by two corner points which may be in any order.
     *
     * @param point the point to test
     * @param corner1 first corner of the rectangle
     * @param corner2 opposite corner of the rectangle
     * @return true if the point is within the rectangle (inclusive)
     */
    public boolean isPointInRectangle(Point point, Point corner1, Point corner2) {
        if (point == null || corner1 == null || corner2 == null) {
            return false;
        }

        int minX = Math.min(corner1.x, corner2.x);
        int maxX = Math.max(corner1.x, corner2.x);
        int minY = Math.min(corner1.y, corner2.y);
        int maxY = Math.max(corner1.y, corner2.y);

        return point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY;
    }

    /**
     * Applies a translation to a point.
     *
     * @param original the original point
     * @param deltaX the X translation
     * @param deltaY the Y translation
     * @return a new point with the translation applied
     */
    public Point translatePoint(Point original, int deltaX, int deltaY) {
        if (original == null) {
            return new Point(deltaX, deltaY);
        }
        return new Point(original.x + deltaX, original.y + deltaY);
    }

    /**
     * Finds the center point of a collection of positions.
     *
     * @param positions collection of position providers
     * @return the center point, or null if no valid positions
     */
    public Point findCenterPoint(Collection<? extends PositionProvider> positions) {
        int[] bounds = calculateBoundingBox(positions);
        if (bounds == null) {
            return null;
        }

        return new Point(
                (bounds[0] + bounds[2]) / 2,  // (minX + maxX) / 2
                (bounds[1] + bounds[3]) / 2   // (minY + maxY) / 2
        );
    }

    /**
     * Calculates grid distance between two points.
     *
     * @param point1 first point
     * @param point2 second point
     * @return the Manhattan distance (|dx| + |dy|) in grid units
     */
    public int calculateManhattanDistance(Point point1, Point point2) {
        if (point1 == null || point2 == null) {
            return 0;
        }
        return Math.abs(point1.x - point2.x) + Math.abs(point1.y - point2.y);
    }

    /**
     * Calculates Euclidean distance between two points.
     *
     * @param point1 first point
     * @param point2 second point
     * @return the Euclidean distance
     */
    public double calculateEuclideanDistance(Point point1, Point point2) {
        if (point1 == null || point2 == null) {
            return 0.0;
        }
        int dx = point1.x - point2.x;
        int dy = point1.y - point2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Snaps a point to the nearest grid position.
     *
     * @param point the point to snap
     * @param gridSize the grid size
     * @return the snapped point
     */
    public Point snapToGrid(Point point, int gridSize) {
        if (point == null || gridSize <= 0) {
            return point;
        }

        int snappedX = Math.round((float) point.x / gridSize) * gridSize;
        int snappedY = Math.round((float) point.y / gridSize) * gridSize;

        return new Point(snappedX, snappedY);
    }

    /**
     * Normalizes a rectangle defined by two corners to have corner1 as top-left.
     *
     * @param corner1 first corner (will become top-left)
     * @param corner2 second corner (will become bottom-right)
     * @return array of [topLeft, bottomRight]
     */
    public Point[] normalizeRectangle(Point corner1, Point corner2) {
        if (corner1 == null || corner2 == null) {
            return new Point[]{corner1, corner2};
        }

        Point topLeft = new Point(
                Math.min(corner1.x, corner2.x),
                Math.min(corner1.y, corner2.y)
        );
        Point bottomRight = new Point(
                Math.max(corner1.x, corner2.x),
                Math.max(corner1.y, corner2.y)
        );

        return new Point[]{topLeft, bottomRight};
    }
}
