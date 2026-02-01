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
package ch.technokrat.gecko.geckocircuits.circuit.component;

import java.awt.Point;

/**
 * Manages component position, rotation, and mirroring on the schematic sheet.
 * Extracted from AbstractBlockInterface to separate position/orientation concerns.
 * 
 * <p>Coordinate System:
 * <ul>
 *   <li>X increases to the right</li>
 *   <li>Y increases downward (screen coordinates)</li>
 *   <li>Grid alignment is typical (positions snap to grid)</li>
 * </ul>
 * 
 * <p>Rotation/Direction:
 * <ul>
 *   <li>0° = North (terminals at top/bottom)</li>
 *   <li>90° = East (terminals at left/right)</li>
 *   <li>180° = South (flipped from North)</li>
 *   <li>270° = West (flipped from East)</li>
 * </ul>
 * 
 * <p>Thread-safety: Not thread-safe. External synchronization required.
 * 
 * @author Sprint 3 refactoring
 */
public class ComponentPositionManager {
    
    /** Component direction/rotation enumeration. */
    public enum Direction {
        NORTH(0),
        EAST(90),
        SOUTH(180),
        WEST(270);
        
        private final int degrees;
        
        Direction(int degrees) {
            this.degrees = degrees;
        }
        
        public int getDegrees() {
            return degrees;
        }
        
        /**
         * Gets direction rotated clockwise by 90 degrees.
         */
        public Direction rotateClockwise() {
            switch (this) {
                case NORTH: return EAST;
                case EAST: return SOUTH;
                case SOUTH: return WEST;
                case WEST: return NORTH;
                default: throw new IllegalStateException();
            }
        }
        
        /**
         * Gets direction rotated counter-clockwise by 90 degrees.
         */
        public Direction rotateCounterClockwise() {
            switch (this) {
                case NORTH: return WEST;
                case WEST: return SOUTH;
                case SOUTH: return EAST;
                case EAST: return NORTH;
                default: throw new IllegalStateException();
            }
        }
        
        /**
         * Gets opposite direction (180° rotation).
         */
        public Direction opposite() {
            switch (this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                case WEST: return EAST;
                default: throw new IllegalStateException();
            }
        }
        
        /**
         * Gets direction from degrees (0, 90, 180, 270).
         */
        public static Direction fromDegrees(int degrees) {
            int normalized = ((degrees % 360) + 360) % 360;
            switch (normalized) {
                case 0: return NORTH;
                case 90: return EAST;
                case 180: return SOUTH;
                case 270: return WEST;
                default:
                    // Round to nearest 90°
                    if (normalized < 45 || normalized >= 315) return NORTH;
                    if (normalized < 135) return EAST;
                    if (normalized < 225) return SOUTH;
                    return WEST;
            }
        }
        
        /**
         * Gets direction from ordinal (0-3).
         */
        public static Direction fromOrdinal(int ordinal) {
            switch (ordinal % 4) {
                case 0: return NORTH;
                case 1: return EAST;
                case 2: return SOUTH;
                case 3: return WEST;
                default: return NORTH;
            }
        }
        
        /**
         * Checks if direction is horizontal (East or West).
         */
        public boolean isHorizontal() {
            return this == EAST || this == WEST;
        }
        
        /**
         * Checks if direction is vertical (North or South).
         */
        public boolean isVertical() {
            return this == NORTH || this == SOUTH;
        }
    }
    
    // Position state
    private int x;
    private int y;
    private Direction direction;
    private boolean mirrored;
    
    // Saved position for move operations
    private int savedX;
    private int savedY;
    
    // Bounding box (component-specific dimensions)
    private int width;
    private int height;
    
    /**
     * Creates a position manager at origin with default orientation.
     */
    public ComponentPositionManager() {
        this(0, 0);
    }
    
    /**
     * Creates a position manager at specified position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     */
    public ComponentPositionManager(int x, int y) {
        this(x, y, Direction.NORTH, false);
    }
    
    /**
     * Creates a position manager with full specification.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param direction Component direction
     * @param mirrored Whether component is mirrored
     */
    public ComponentPositionManager(int x, int y, Direction direction, boolean mirrored) {
        this.x = x;
        this.y = y;
        this.direction = direction != null ? direction : Direction.NORTH;
        this.mirrored = mirrored;
        this.savedX = x;
        this.savedY = y;
        this.width = 1;
        this.height = 1;
    }
    
    // ===== Position Access =====
    
    /**
     * Gets X coordinate.
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets Y coordinate.
     */
    public int getY() {
        return y;
    }
    
    /**
     * Gets position as Point.
     */
    public Point getPosition() {
        return new Point(x, y);
    }
    
    /**
     * Sets X coordinate.
     */
    public void setX(int x) {
        this.x = x;
    }
    
    /**
     * Sets Y coordinate.
     */
    public void setY(int y) {
        this.y = y;
    }
    
    /**
     * Sets position.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Sets position from Point.
     */
    public void setPosition(Point p) {
        if (p == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        this.x = p.x;
        this.y = p.y;
    }
    
    /**
     * Moves position by offset.
     */
    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }
    
    // ===== Direction/Rotation =====
    
    /**
     * Gets component direction.
     */
    public Direction getDirection() {
        return direction;
    }
    
    /**
     * Gets direction as degrees (0, 90, 180, 270).
     */
    public int getDirectionDegrees() {
        return direction.getDegrees();
    }
    
    /**
     * Gets direction as ordinal (0-3) for legacy compatibility.
     */
    public int getDirectionOrdinal() {
        return direction.ordinal();
    }
    
    /**
     * Sets component direction.
     */
    public void setDirection(Direction direction) {
        this.direction = direction != null ? direction : Direction.NORTH;
    }
    
    /**
     * Sets direction from degrees.
     */
    public void setDirectionDegrees(int degrees) {
        this.direction = Direction.fromDegrees(degrees);
    }
    
    /**
     * Sets direction from ordinal (0-3).
     */
    public void setDirectionOrdinal(int ordinal) {
        this.direction = Direction.fromOrdinal(ordinal);
    }
    
    /**
     * Rotates component 90° clockwise.
     */
    public void rotateClockwise() {
        this.direction = this.direction.rotateClockwise();
    }
    
    /**
     * Rotates component 90° counter-clockwise.
     */
    public void rotateCounterClockwise() {
        this.direction = this.direction.rotateCounterClockwise();
    }
    
    /**
     * Rotates component 180°.
     */
    public void rotate180() {
        this.direction = this.direction.opposite();
    }
    
    // ===== Mirroring =====
    
    /**
     * Checks if component is mirrored.
     */
    public boolean isMirrored() {
        return mirrored;
    }
    
    /**
     * Sets mirrored state.
     */
    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }
    
    /**
     * Toggles mirrored state.
     */
    public void toggleMirror() {
        this.mirrored = !this.mirrored;
    }
    
    // ===== Saved Position (for move operations) =====
    
    /**
     * Saves current position for later restore.
     */
    public void savePosition() {
        this.savedX = this.x;
        this.savedY = this.y;
    }
    
    /**
     * Restores previously saved position.
     */
    public void restorePosition() {
        this.x = this.savedX;
        this.y = this.savedY;
    }
    
    /**
     * Gets saved X coordinate.
     */
    public int getSavedX() {
        return savedX;
    }
    
    /**
     * Gets saved Y coordinate.
     */
    public int getSavedY() {
        return savedY;
    }
    
    /**
     * Gets saved position as Point.
     */
    public Point getSavedPosition() {
        return new Point(savedX, savedY);
    }
    
    /**
     * Calculates delta from saved position.
     */
    public Point getDeltaFromSaved() {
        return new Point(x - savedX, y - savedY);
    }
    
    // ===== Bounding Box =====
    
    /**
     * Gets component width (in grid units).
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets component height (in grid units).
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Sets component dimensions.
     */
    public void setDimensions(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }
    
    /**
     * Gets effective width considering rotation.
     */
    public int getEffectiveWidth() {
        return direction.isHorizontal() ? height : width;
    }
    
    /**
     * Gets effective height considering rotation.
     */
    public int getEffectiveHeight() {
        return direction.isHorizontal() ? width : height;
    }
    
    /**
     * Calculates center X coordinate.
     */
    public int getCenterX() {
        return x + getEffectiveWidth() / 2;
    }
    
    /**
     * Calculates center Y coordinate.
     */
    public int getCenterY() {
        return y + getEffectiveHeight() / 2;
    }
    
    /**
     * Gets center as Point.
     */
    public Point getCenter() {
        return new Point(getCenterX(), getCenterY());
    }
    
    /**
     * Gets the left edge X coordinate.
     */
    public int getLeft() {
        return x;
    }
    
    /**
     * Gets the right edge X coordinate.
     */
    public int getRight() {
        return x + getEffectiveWidth();
    }
    
    /**
     * Gets the top edge Y coordinate.
     */
    public int getTop() {
        return y;
    }
    
    /**
     * Gets the bottom edge Y coordinate.
     */
    public int getBottom() {
        return y + getEffectiveHeight();
    }
    
    // ===== Hit Testing =====
    
    /**
     * Checks if a point is within the component bounds.
     * 
     * @param px X coordinate to test
     * @param py Y coordinate to test
     * @return true if point is within bounds
     */
    public boolean contains(int px, int py) {
        return px >= getLeft() && px < getRight() &&
               py >= getTop() && py < getBottom();
    }
    
    /**
     * Checks if a point is within the component bounds.
     */
    public boolean contains(Point p) {
        return p != null && contains(p.x, p.y);
    }
    
    /**
     * Checks if this component intersects with another.
     */
    public boolean intersects(ComponentPositionManager other) {
        if (other == null) return false;
        
        return getLeft() < other.getRight() &&
               getRight() > other.getLeft() &&
               getTop() < other.getBottom() &&
               getBottom() > other.getTop();
    }
    
    // ===== Coordinate Transformation =====
    
    /**
     * Transforms a local coordinate to sheet coordinate.
     * 
     * @param localX Local X (relative to component)
     * @param localY Local Y (relative to component)
     * @return Sheet coordinate
     */
    public Point localToSheet(int localX, int localY) {
        int tx, ty;
        
        switch (direction) {
            case NORTH:
                tx = mirrored ? -localX : localX;
                ty = localY;
                break;
            case EAST:
                tx = mirrored ? localY : -localY;
                ty = localX;
                break;
            case SOUTH:
                tx = mirrored ? localX : -localX;
                ty = -localY;
                break;
            case WEST:
                tx = mirrored ? -localY : localY;
                ty = -localX;
                break;
            default:
                tx = localX;
                ty = localY;
        }
        
        return new Point(x + tx, y + ty);
    }
    
    /**
     * Transforms a sheet coordinate to local coordinate.
     * 
     * @param sheetX Sheet X coordinate
     * @param sheetY Sheet Y coordinate
     * @return Local coordinate (relative to component)
     */
    public Point sheetToLocal(int sheetX, int sheetY) {
        int dx = sheetX - x;
        int dy = sheetY - y;
        int lx, ly;
        
        switch (direction) {
            case NORTH:
                lx = mirrored ? -dx : dx;
                ly = dy;
                break;
            case EAST:
                lx = dy;
                ly = mirrored ? dx : -dx;
                break;
            case SOUTH:
                lx = mirrored ? dx : -dx;
                ly = -dy;
                break;
            case WEST:
                lx = -dy;
                ly = mirrored ? -dx : dx;
                break;
            default:
                lx = dx;
                ly = dy;
        }
        
        return new Point(lx, ly);
    }
    
    // ===== Serialization Support =====
    
    /**
     * Exports position state to array for serialization.
     * 
     * @return Array: [x, y, directionOrdinal, mirrored(0/1)]
     */
    public int[] toIntArray() {
        return new int[]{x, y, direction.ordinal(), mirrored ? 1 : 0};
    }
    
    /**
     * Imports position state from array.
     * 
     * @param data Array: [x, y, directionOrdinal, mirrored(0/1)]
     */
    public void fromIntArray(int[] data) {
        if (data == null || data.length < 4) {
            throw new IllegalArgumentException("Invalid position data array");
        }
        this.x = data[0];
        this.y = data[1];
        this.direction = Direction.fromOrdinal(data[2]);
        this.mirrored = data[3] != 0;
    }
    
    /**
     * Creates a copy of this position manager.
     */
    public ComponentPositionManager copy() {
        ComponentPositionManager copy = new ComponentPositionManager(x, y, direction, mirrored);
        copy.savedX = this.savedX;
        copy.savedY = this.savedY;
        copy.width = this.width;
        copy.height = this.height;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("ComponentPositionManager[pos=(%d,%d), dir=%s%s, size=%dx%d]",
            x, y, direction, mirrored ? ", mirrored" : "", width, height);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ComponentPositionManager)) return false;
        
        ComponentPositionManager other = (ComponentPositionManager) obj;
        return x == other.x && y == other.y &&
               direction == other.direction && mirrored == other.mirrored;
    }
    
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + direction.hashCode();
        result = 31 * result + (mirrored ? 1 : 0);
        return result;
    }
}
