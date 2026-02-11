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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Validates connections between terminals and connection paths.
 * 
 * <p>This class provides validation logic for circuit connections including:
 * <ul>
 *   <li>Type compatibility (LK, CONTROL, RELUCTANCE)</li>
 *   <li>Path validity (orthogonal segments only)</li>
 *   <li>Connection rules enforcement</li>
 *   <li>Collision detection</li>
 * </ul>
 * 
 * @see ITerminalPosition
 * @see ConnectionPath
 */
public final class ConnectionValidator {
    
    /** Maximum allowed path length in grid units */
    public static final int MAX_PATH_LENGTH = 1000;
    
    /** Maximum number of corners allowed in a path */
    public static final int MAX_CORNERS = 10;
    
    /** Minimum distance between parallel paths */
    public static final int MIN_PATH_SPACING = 1;
    
    private ConnectionValidator() {
        // Utility class - no instantiation
    }
    
    // ===========================================
    // Terminal Validation
    // ===========================================
    
    /**
     * Validates that two terminals can be connected.
     * 
     * @param terminal1 first terminal
     * @param terminal2 second terminal
     * @return validation result
     */
    public static ValidationResult validateConnection(ITerminalPosition terminal1, 
                                                       ITerminalPosition terminal2) {
        Objects.requireNonNull(terminal1, "Terminal 1 cannot be null");
        Objects.requireNonNull(terminal2, "Terminal 2 cannot be null");
        
        // Check connector type compatibility
        if (terminal1.getConnectorType() != terminal2.getConnectorType()) {
            return ValidationResult.failure("Incompatible connector types: " 
                + terminal1.getConnectorType() + " vs " + terminal2.getConnectorType());
        }
        
        // Check for same position (short circuit warning)
        if (terminal1.isAtSamePosition(terminal2)) {
            return ValidationResult.warning("Terminals are at the same position");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Checks if a terminal position is valid (within reasonable bounds).
     * 
     * @param terminal the terminal to validate
     * @param minX minimum X bound
     * @param maxX maximum X bound
     * @param minY minimum Y bound
     * @param maxY maximum Y bound
     * @return validation result
     */
    public static ValidationResult validateTerminalBounds(ITerminalPosition terminal,
                                                           int minX, int maxX, 
                                                           int minY, int maxY) {
        if (terminal.getX() < minX || terminal.getX() > maxX) {
            return ValidationResult.failure("Terminal X position out of bounds: " + terminal.getX());
        }
        if (terminal.getY() < minY || terminal.getY() > maxY) {
            return ValidationResult.failure("Terminal Y position out of bounds: " + terminal.getY());
        }
        return ValidationResult.success();
    }
    
    // ===========================================
    // Path Validation
    // ===========================================
    
    /**
     * Validates a connection path.
     * 
     * @param path the path to validate
     * @return validation result
     */
    public static ValidationResult validatePath(ConnectionPath path) {
        Objects.requireNonNull(path, "Path cannot be null");
        
        // Check minimum points
        if (path.getPointCount() < 2) {
            return ValidationResult.failure("Path must have at least 2 points");
        }
        
        // Check path validity (orthogonal segments)
        if (!path.isValid()) {
            return ValidationResult.failure("Path contains diagonal segments");
        }
        
        // Check path length
        if (path.getTotalLength() > MAX_PATH_LENGTH) {
            return ValidationResult.failure("Path length exceeds maximum: " 
                + path.getTotalLength() + " > " + MAX_PATH_LENGTH);
        }
        
        // Check corner count
        if (path.getCornerCount() > MAX_CORNERS) {
            return ValidationResult.warning("Path has many corners: " + path.getCornerCount());
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates that a path connects two specific terminals.
     * 
     * @param path the path
     * @param start expected start terminal
     * @param end expected end terminal
     * @return validation result
     */
    public static ValidationResult validatePathEndpoints(ConnectionPath path,
                                                          ITerminalPosition start,
                                                          ITerminalPosition end) {
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(start, "Start terminal cannot be null");
        Objects.requireNonNull(end, "End terminal cannot be null");
        
        ConnectionPath.PathPoint pathStart = path.getStartPoint();
        ConnectionPath.PathPoint pathEnd = path.getEndPoint();
        
        if (pathStart == null || pathEnd == null) {
            return ValidationResult.failure("Path has no endpoints");
        }
        
        boolean startMatches = pathStart.x == start.getX() && pathStart.y == start.getY();
        boolean endMatches = pathEnd.x == end.getX() && pathEnd.y == end.getY();
        
        if (!startMatches && !endMatches) {
            return ValidationResult.failure("Path endpoints do not match terminals");
        }
        
        // Check if path is reversed
        if (!startMatches && endMatches) {
            return ValidationResult.warning("Path appears to be reversed");
        }
        
        return ValidationResult.success();
    }
    
    // ===========================================
    // Collision Detection
    // ===========================================
    
    /**
     * Checks if two paths intersect (cross each other).
     * 
     * @param path1 first path
     * @param path2 second path
     * @return true if paths intersect
     */
    public static boolean pathsIntersect(ConnectionPath path1, ConnectionPath path2) {
        List<ConnectionPath.PathPoint> points1 = path1.getPoints();
        List<ConnectionPath.PathPoint> points2 = path2.getPoints();
        
        // Check each segment of path1 against each segment of path2
        for (int i = 0; i < points1.size() - 1; i++) {
            ConnectionPath.PathPoint p1a = points1.get(i);
            ConnectionPath.PathPoint p1b = points1.get(i + 1);
            
            for (int j = 0; j < points2.size() - 1; j++) {
                ConnectionPath.PathPoint p2a = points2.get(j);
                ConnectionPath.PathPoint p2b = points2.get(j + 1);
                
                if (segmentsIntersect(p1a, p1b, p2a, p2b)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean segmentsIntersect(ConnectionPath.PathPoint a1, ConnectionPath.PathPoint a2,
                                              ConnectionPath.PathPoint b1, ConnectionPath.PathPoint b2) {
        // Check if segments are both horizontal or both vertical
        boolean aHorizontal = a1.y == a2.y;
        boolean bHorizontal = b1.y == b2.y;
        
        if (aHorizontal == bHorizontal) {
            // Parallel segments - check for overlap
            if (aHorizontal) {
                // Both horizontal
                if (a1.y != b1.y) return false;  // Different rows
                int aMin = Math.min(a1.x, a2.x);
                int aMax = Math.max(a1.x, a2.x);
                int bMin = Math.min(b1.x, b2.x);
                int bMax = Math.max(b1.x, b2.x);
                return aMin <= bMax && bMin <= aMax;
            } else {
                // Both vertical
                if (a1.x != b1.x) return false;  // Different columns
                int aMin = Math.min(a1.y, a2.y);
                int aMax = Math.max(a1.y, a2.y);
                int bMin = Math.min(b1.y, b2.y);
                int bMax = Math.max(b1.y, b2.y);
                return aMin <= bMax && bMin <= aMax;
            }
        }
        
        // Perpendicular segments - check for crossing
        ConnectionPath.PathPoint horStart, horEnd, verStart, verEnd;
        if (aHorizontal) {
            horStart = a1; horEnd = a2;
            verStart = b1; verEnd = b2;
        } else {
            horStart = b1; horEnd = b2;
            verStart = a1; verEnd = a2;
        }
        
        int horY = horStart.y;
        int horMinX = Math.min(horStart.x, horEnd.x);
        int horMaxX = Math.max(horStart.x, horEnd.x);
        int verX = verStart.x;
        int verMinY = Math.min(verStart.y, verEnd.y);
        int verMaxY = Math.max(verStart.y, verEnd.y);
        
        return verX >= horMinX && verX <= horMaxX && horY >= verMinY && horY <= verMaxY;
    }
    
    /**
     * Checks if a point lies on any existing path.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param existingPaths collection of existing paths
     * @return true if point is on any path
     */
    public static boolean isPointOnAnyPath(int x, int y, Collection<ConnectionPath> existingPaths) {
        for (ConnectionPath path : existingPaths) {
            if (path.containsPoint(x, y)) {
                return true;
            }
        }
        return false;
    }
    
    // ===========================================
    // Domain-Specific Validation
    // ===========================================
    
    /**
     * Validates that a power (LK) connection follows power circuit rules.
     * 
     * @param path the path to validate
     * @return validation result
     */
    public static ValidationResult validatePowerConnection(ConnectionPath path) {
        if (path.getConnectorType() != ConnectorType.LK) {
            return ValidationResult.failure("Not a power connection");
        }
        return validatePath(path);
    }
    
    /**
     * Validates that a control connection follows signal routing rules.
     * 
     * @param path the path to validate
     * @return validation result
     */
    public static ValidationResult validateControlConnection(ConnectionPath path) {
        if (path.getConnectorType() != ConnectorType.CONTROL) {
            return ValidationResult.failure("Not a control connection");
        }
        return validatePath(path);
    }
    
    /**
     * Validates that a reluctance (magnetic) connection is valid.
     * 
     * @param path the path to validate
     * @return validation result
     */
    public static ValidationResult validateReluctanceConnection(ConnectionPath path) {
        if (path.getConnectorType() != ConnectorType.RELUCTANCE) {
            return ValidationResult.failure("Not a reluctance connection");
        }
        return validatePath(path);
    }
    
    // ===========================================
    // Validation Result
    // ===========================================
    
    /**
     * Result of a validation operation.
     */
    public static final class ValidationResult {
        
        public enum Status {
            SUCCESS,
            WARNING,
            FAILURE
        }
        
        private final Status status;
        private final String message;
        private final List<String> details;
        
        private ValidationResult(Status status, String message) {
            this.status = status;
            this.message = message;
            this.details = new ArrayList<>();
        }
        
        public static ValidationResult success() {
            return new ValidationResult(Status.SUCCESS, "Validation passed");
        }
        
        public static ValidationResult warning(String message) {
            return new ValidationResult(Status.WARNING, message);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(Status.FAILURE, message);
        }
        
        public boolean isValid() {
            return status == Status.SUCCESS || status == Status.WARNING;
        }
        
        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
        
        public boolean isWarning() {
            return status == Status.WARNING;
        }
        
        public boolean isFailure() {
            return status == Status.FAILURE;
        }
        
        public Status getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ValidationResult addDetail(String detail) {
            details.add(detail);
            return this;
        }
        
        public List<String> getDetails() {
            return List.copyOf(details);
        }
        
        @Override
        public String toString() {
            return status + ": " + message;
        }
    }
}
