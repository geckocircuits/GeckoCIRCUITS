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
package ch.technokrat.gecko.core.circuit;

import java.io.Serializable;
import java.util.Objects;

/**
 * A GUI-free replacement for java.awt.Point for schematic grid coordinates.
 * 
 * This class represents a point on the circuit schematic grid and is designed
 * to be used in headless environments without AWT/Swing dependencies.
 * 
 * The grid coordinates are integers representing discrete positions on the
 * schematic canvas, typically in units of grid spacing (e.g., 10 pixels).
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15
 */
public final class GridPoint implements Serializable, Comparable<GridPoint> {
    
    private static final long serialVersionUID = 1L;
    
    /** Origin point (0, 0) */
    public static final GridPoint ORIGIN = new GridPoint(0, 0);
    
    /** X coordinate (horizontal position) */
    public final int x;
    
    /** Y coordinate (vertical position) */
    public final int y;
    
    /**
     * Creates a grid point at the specified coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public GridPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Creates a grid point from double coordinates (truncates to int).
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public GridPoint(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }
    
    /**
     * Creates a copy of another grid point.
     *
     * @param other the point to copy
     */
    public GridPoint(GridPoint other) {
        this.x = other.x;
        this.y = other.y;
    }
    
    /**
     * Returns a new point translated by the given offsets.
     *
     * @param dx horizontal offset
     * @param dy vertical offset
     * @return new translated point
     */
    public GridPoint translate(int dx, int dy) {
        return new GridPoint(x + dx, y + dy);
    }
    
    /**
     * Returns a new point at the sum of this and another point.
     *
     * @param other the point to add
     * @return new point representing vector addition
     */
    public GridPoint add(GridPoint other) {
        return new GridPoint(x + other.x, y + other.y);
    }
    
    /**
     * Returns a new point at the difference of this and another point.
     *
     * @param other the point to subtract
     * @return new point representing vector subtraction
     */
    public GridPoint subtract(GridPoint other) {
        return new GridPoint(x - other.x, y - other.y);
    }
    
    /**
     * Returns the Euclidean distance to another point.
     *
     * @param other the other point
     * @return distance in grid units
     */
    public double distanceTo(GridPoint other) {
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Returns the Manhattan (taxicab) distance to another point.
     *
     * @param other the other point
     * @return Manhattan distance in grid units
     */
    public int manhattanDistanceTo(GridPoint other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
    
    /**
     * Checks if this point is within a rectangular region.
     *
     * @param minX minimum x coordinate (inclusive)
     * @param minY minimum y coordinate (inclusive)
     * @param maxX maximum x coordinate (exclusive)
     * @param maxY maximum y coordinate (exclusive)
     * @return true if point is within the region
     */
    public boolean isWithin(int minX, int minY, int maxX, int maxY) {
        return x >= minX && x < maxX && y >= minY && y < maxY;
    }
    
    /**
     * Returns the x coordinate.
     * @return x coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Returns the y coordinate.
     * @return y coordinate
     */
    public int getY() {
        return y;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GridPoint)) return false;
        GridPoint other = (GridPoint) obj;
        return x == other.x && y == other.y;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
    
    @Override
    public String toString() {
        return "GridPoint[" + x + ", " + y + "]";
    }
    
    @Override
    public int compareTo(GridPoint other) {
        int cmpY = Integer.compare(this.y, other.y);
        return cmpY != 0 ? cmpY : Integer.compare(this.x, other.x);
    }
    
    /**
     * Converts to java.awt.Point for GUI compatibility.
     * 
     * @return equivalent AWT Point
     */
    public java.awt.Point toAwtPoint() {
        return new java.awt.Point(x, y);
    }
    
    /**
     * Creates a GridPoint from a java.awt.Point.
     *
     * @param awtPoint the AWT point to convert
     * @return equivalent GridPoint
     */
    public static GridPoint fromAwtPoint(java.awt.Point awtPoint) {
        return new GridPoint(awtPoint.x, awtPoint.y);
    }
}
