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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a connection path between two terminals in a circuit schematic.
 * 
 * <p>Connection paths are composed of orthogonal segments (horizontal and vertical)
 * that route wires between component terminals. The path follows a Manhattan-style
 * routing where all segments are axis-aligned.
 * 
 * <p>Path structure:
 * <ul>
 *   <li>Start point: first terminal</li>
 *   <li>Intermediate points: corners where direction changes</li>
 *   <li>End point: second terminal</li>
 * </ul>
 * 
 * @see ITerminalPosition
 * @see ConnectionValidator
 */
public final class ConnectionPath {
    
    private final List<PathPoint> points;
    private final ConnectorType connectorType;
    
    /**
     * Creates an empty connection path.
     * 
     * @param connectorType the type of connection (LK, CONTROL, RELUCTANCE)
     */
    public ConnectionPath(ConnectorType connectorType) {
        this.points = new ArrayList<>();
        this.connectorType = Objects.requireNonNull(connectorType, "Connector type cannot be null");
    }
    
    /**
     * Creates a connection path with initial points.
     * 
     * @param connectorType the type of connection
     * @param initialPoints the initial path points
     */
    public ConnectionPath(ConnectorType connectorType, List<PathPoint> initialPoints) {
        this.connectorType = Objects.requireNonNull(connectorType, "Connector type cannot be null");
        this.points = new ArrayList<>(initialPoints);
    }
    
    /**
     * Creates a straight connection path between two terminals.
     * Uses L-shaped routing with one corner point.
     * 
     * @param start the start terminal
     * @param end the end terminal
     * @param horizontalFirst true to go horizontal first, false for vertical first
     * @return the connection path
     */
    public static ConnectionPath createLPath(ITerminalPosition start, ITerminalPosition end, 
                                              boolean horizontalFirst) {
        if (!start.canConnectTo(end)) {
            throw new IllegalArgumentException("Cannot connect terminals of different types: " 
                + start.getConnectorType() + " vs " + end.getConnectorType());
        }
        
        ConnectionPath path = new ConnectionPath(start.getConnectorType());
        path.addPoint(start.getX(), start.getY());
        
        if (horizontalFirst) {
            // Go horizontal first, then vertical
            if (start.getX() != end.getX()) {
                path.addPoint(end.getX(), start.getY());  // Corner point
            }
        } else {
            // Go vertical first, then horizontal
            if (start.getY() != end.getY()) {
                path.addPoint(start.getX(), end.getY());  // Corner point
            }
        }
        
        path.addPoint(end.getX(), end.getY());
        return path;
    }
    
    /**
     * Creates a direct path between two aligned terminals.
     * Only works if terminals are horizontally or vertically aligned.
     * 
     * @param start the start terminal
     * @param end the end terminal
     * @return the connection path
     * @throws IllegalArgumentException if terminals are not aligned
     */
    public static ConnectionPath createDirectPath(ITerminalPosition start, ITerminalPosition end) {
        if (!start.canConnectTo(end)) {
            throw new IllegalArgumentException("Cannot connect terminals of different types");
        }
        if (!start.isHorizontallyAligned(end) && !start.isVerticallyAligned(end)) {
            throw new IllegalArgumentException("Terminals must be aligned for direct path");
        }
        
        ConnectionPath path = new ConnectionPath(start.getConnectorType());
        path.addPoint(start.getX(), start.getY());
        path.addPoint(end.getX(), end.getY());
        return path;
    }
    
    /**
     * Adds a point to the path.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void addPoint(int x, int y) {
        points.add(new PathPoint(x, y));
    }
    
    /**
     * Gets an unmodifiable view of the path points.
     * 
     * @return list of path points
     */
    public List<PathPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }
    
    /**
     * Gets the start point of the path.
     * 
     * @return the first point, or null if path is empty
     */
    public PathPoint getStartPoint() {
        return points.isEmpty() ? null : points.get(0);
    }
    
    /**
     * Gets the end point of the path.
     * 
     * @return the last point, or null if path is empty
     */
    public PathPoint getEndPoint() {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }
    
    /**
     * Gets the number of points in the path.
     * 
     * @return point count
     */
    public int getPointCount() {
        return points.size();
    }
    
    /**
     * Gets the number of segments in the path.
     * A segment connects two consecutive points.
     * 
     * @return segment count (points - 1), or 0 if less than 2 points
     */
    public int getSegmentCount() {
        return Math.max(0, points.size() - 1);
    }
    
    /**
     * Gets the connector type for this path.
     * 
     * @return the connector type
     */
    public ConnectorType getConnectorType() {
        return connectorType;
    }
    
    /**
     * Calculates the total length of the path (sum of all segment lengths).
     * 
     * @return total path length in grid units
     */
    public int getTotalLength() {
        int length = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            PathPoint p1 = points.get(i);
            PathPoint p2 = points.get(i + 1);
            length += Math.abs(p2.x - p1.x) + Math.abs(p2.y - p1.y);
        }
        return length;
    }
    
    /**
     * Gets the number of corners (direction changes) in the path.
     * 
     * @return corner count
     */
    public int getCornerCount() {
        if (points.size() < 3) return 0;
        
        int corners = 0;
        for (int i = 1; i < points.size() - 1; i++) {
            PathPoint prev = points.get(i - 1);
            PathPoint curr = points.get(i);
            PathPoint next = points.get(i + 1);
            
            // Check if direction changes at this point
            boolean prevHorizontal = prev.y == curr.y;
            boolean nextHorizontal = curr.y == next.y;
            
            if (prevHorizontal != nextHorizontal) {
                corners++;
            }
        }
        return corners;
    }
    
    /**
     * Checks if the path is valid (has at least 2 points and all segments are orthogonal).
     * 
     * @return true if path is valid
     */
    public boolean isValid() {
        if (points.size() < 2) return false;
        
        for (int i = 0; i < points.size() - 1; i++) {
            PathPoint p1 = points.get(i);
            PathPoint p2 = points.get(i + 1);
            
            // Each segment must be horizontal or vertical
            if (p1.x != p2.x && p1.y != p2.y) {
                return false;  // Diagonal segment not allowed
            }
        }
        return true;
    }
    
    /**
     * Checks if this path contains a specific point.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if point is on the path
     */
    public boolean containsPoint(int x, int y) {
        // Check each segment
        for (int i = 0; i < points.size() - 1; i++) {
            PathPoint p1 = points.get(i);
            PathPoint p2 = points.get(i + 1);
            
            if (isPointOnSegment(x, y, p1, p2)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isPointOnSegment(int x, int y, PathPoint p1, PathPoint p2) {
        if (p1.x == p2.x) {
            // Vertical segment
            int minY = Math.min(p1.y, p2.y);
            int maxY = Math.max(p1.y, p2.y);
            return x == p1.x && y >= minY && y <= maxY;
        } else if (p1.y == p2.y) {
            // Horizontal segment
            int minX = Math.min(p1.x, p2.x);
            int maxX = Math.max(p1.x, p2.x);
            return y == p1.y && x >= minX && x <= maxX;
        }
        return false;
    }
    
    /**
     * Creates a reversed copy of this path.
     * 
     * @return new path with points in reverse order
     */
    public ConnectionPath reverse() {
        List<PathPoint> reversed = new ArrayList<>(points);
        Collections.reverse(reversed);
        return new ConnectionPath(connectorType, reversed);
    }
    
    /**
     * Trims redundant collinear points from the path.
     * Points that lie on a straight line between their neighbors are removed.
     * 
     * @return new trimmed path
     */
    public ConnectionPath trimmed() {
        if (points.size() <= 2) {
            return new ConnectionPath(connectorType, new ArrayList<>(points));
        }
        
        List<PathPoint> trimmed = new ArrayList<>();
        trimmed.add(points.get(0));  // Always keep first point
        
        for (int i = 1; i < points.size() - 1; i++) {
            PathPoint prev = points.get(i - 1);
            PathPoint curr = points.get(i);
            PathPoint next = points.get(i + 1);
            
            // Keep point only if direction changes
            boolean sameHorizontal = (prev.y == curr.y) && (curr.y == next.y);
            boolean sameVertical = (prev.x == curr.x) && (curr.x == next.x);
            
            if (!sameHorizontal && !sameVertical) {
                trimmed.add(curr);
            }
        }
        
        trimmed.add(points.get(points.size() - 1));  // Always keep last point
        return new ConnectionPath(connectorType, trimmed);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConnectionPath)) return false;
        ConnectionPath other = (ConnectionPath) obj;
        return connectorType == other.connectorType && points.equals(other.points);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(connectorType, points);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConnectionPath[").append(connectorType).append("]: ");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(points.get(i));
        }
        return sb.toString();
    }
    
    /**
     * Represents a point on the connection path.
     */
    public static final class PathPoint {
        public final int x;
        public final int y;
        
        public PathPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PathPoint)) return false;
            PathPoint other = (PathPoint) obj;
            return x == other.x && y == other.y;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + y;
        }
        
        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }
}
