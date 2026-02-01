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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.control.Point;

/**
 * Interface for objects that can be terminated in a subcircuit.
 * 
 * This interface is GUI-free: colors are returned as RGB integers.
 * 
 * @since Sprint 15 - GUI-free refactoring
 */
public interface SubCircuitTerminable {
    public TerminalSubCircuitBlock getBlockTerminal();
    public EnumTerminalLocation getTerminalLocation();
    public Point getSheetPosition();
    public void setSheetPositionWithoutUndo(Point sheetPosition);
    public String getStringID();
    public CircuitSheet getParentCircuitSheet();
    
    /**
     * Get foreground color as RGB integer.
     * To convert to AWT Color: {@code new java.awt.Color(getForeGroundColorRgb())}
     */
    int getForeGroundColorRgb();
    
    /**
     * @deprecated Use getForeGroundColorRgb() for GUI-free code.
     * For GUI layer only - returns AWT Color.
     */
    @Deprecated(since = "Sprint 15", forRemoval = true)
    default java.awt.Color getForeGroundColor() {
        return new java.awt.Color(getForeGroundColorRgb());
    }
}
