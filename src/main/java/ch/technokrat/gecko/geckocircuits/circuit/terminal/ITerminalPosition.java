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

/**
 * Interface for terminal position management in circuit schematics.
 * 
 * <p>Terminals are connection points on circuit components where wires can attach.
 * This interface provides a standardized way to query and manage terminal positions
 * independent of the underlying component implementation.
 * 
 * <p>Coordinate system:
 * <ul>
 *   <li>X increases to the right</li>
 *   <li>Y increases downward</li>
 *   <li>Coordinates are in grid units (typically 10 pixels per unit)</li>
 * </ul>
 * 
 * @see ConnectionPath
 * @see ConnectionValidator
 */
public interface ITerminalPosition {
    
    /**
     * Gets the X coordinate of the terminal in grid units.
     * 
     * @return X position
     */
    int getX();
    
    /**
     * Gets the Y coordinate of the terminal in grid units.
     * 
     * @return Y position
     */
    int getY();
    
    /**
     * Gets the connector type (domain) of this terminal.
     * 
     * @return the connector type (LK for power, CONTROL for signals, RELUCTANCE for magnetics)
     */
    ConnectorType getConnectorType();
    
    /**
     * Checks if this terminal is at the same position as another terminal.
     * 
     * @param other the other terminal to compare
     * @return true if both terminals are at the same grid position
     */
    default boolean isAtSamePosition(ITerminalPosition other) {
        return this.getX() == other.getX() && this.getY() == other.getY();
    }
    
    /**
     * Calculates the Manhattan distance to another terminal.
     * Used for routing and connection path estimation.
     * 
     * @param other the other terminal
     * @return Manhattan distance in grid units
     */
    default int manhattanDistanceTo(ITerminalPosition other) {
        return Math.abs(this.getX() - other.getX()) + Math.abs(this.getY() - other.getY());
    }
    
    /**
     * Calculates the Euclidean distance to another terminal.
     * 
     * @param other the other terminal
     * @return Euclidean distance in grid units
     */
    default double euclideanDistanceTo(ITerminalPosition other) {
        int dx = this.getX() - other.getX();
        int dy = this.getY() - other.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Checks if this terminal can connect to another terminal.
     * Terminals can only connect if they are of the same connector type.
     * 
     * @param other the other terminal
     * @return true if connection is allowed
     */
    default boolean canConnectTo(ITerminalPosition other) {
        return this.getConnectorType() == other.getConnectorType();
    }
    
    /**
     * Checks if the terminal is on the same horizontal line as another.
     * 
     * @param other the other terminal
     * @return true if Y coordinates match
     */
    default boolean isHorizontallyAligned(ITerminalPosition other) {
        return this.getY() == other.getY();
    }
    
    /**
     * Checks if the terminal is on the same vertical line as another.
     * 
     * @param other the other terminal
     * @return true if X coordinates match
     */
    default boolean isVerticallyAligned(ITerminalPosition other) {
        return this.getX() == other.getX();
    }
    
    /**
     * Creates a simple terminal position from coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param connectorType the connector type
     * @return a new terminal position instance
     */
    static ITerminalPosition of(int x, int y, ConnectorType connectorType) {
        return new SimpleTerminalPosition(x, y, connectorType);
    }
    
    /**
     * Simple immutable implementation of ITerminalPosition.
     */
    final class SimpleTerminalPosition implements ITerminalPosition {
        private final int x;
        private final int y;
        private final ConnectorType connectorType;
        
        public SimpleTerminalPosition(int x, int y, ConnectorType connectorType) {
            this.x = x;
            this.y = y;
            this.connectorType = connectorType;
        }
        
        @Override
        public int getX() {
            return x;
        }
        
        @Override
        public int getY() {
            return y;
        }
        
        @Override
        public ConnectorType getConnectorType() {
            return connectorType;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ITerminalPosition)) return false;
            ITerminalPosition other = (ITerminalPosition) obj;
            return x == other.getX() && y == other.getY() && 
                   connectorType == other.getConnectorType();
        }
        
        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + (connectorType != null ? connectorType.hashCode() : 0);
            return result;
        }
        
        @Override
        public String toString() {
            return String.format("Terminal(%d, %d, %s)", x, y, connectorType);
        }
    }
}
