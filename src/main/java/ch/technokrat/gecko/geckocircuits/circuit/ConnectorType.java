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

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;

/**
 * Enumeration of connector types with associated display properties.
 * 
 * This class is GUI-free: it returns color values as RGB integers rather than
 * java.awt.Color objects. Convert to Color in GUI code if needed.
 * 
 * @author andreas
 * @since Sprint 15 - GUI-free refactoring
 */
public enum ConnectorType {

    LK,
    CONTROL,
    RELUCTANCE,
    LK_AND_RELUCTANCE,
    THERMAL,
    NONE;    

    public static ConnectorType fromOrdinal(final int ord) {
        for (ConnectorType tmp : ConnectorType.values()) {
            if (tmp.ordinal() == ord) {
                return tmp;
            }
        }
        assert false;
        return LK;
    }

    public ElementDisplayProperties getDisplayMode() {
        switch(this) {
            case LK:
            case RELUCTANCE:
            case LK_AND_RELUCTANCE:
                return SchematicEditor2._lkDisplayMode;
            case CONTROL:
                return SchematicEditor2._controlDisplayMode;
            case THERMAL:
                return SchematicEditor2._controlDisplayMode;
            default:
                return SchematicEditor2._lkDisplayMode;
        }        
    }

    /**
     * Get background color as RGB integer value.
     * To convert to AWT Color: {@code new java.awt.Color(getBackgroundColorRgb())}
     */
    int getBackgroundColorRgb() {
        switch (this) {
            case THERMAL:
                return colorToRgb(GlobalColors.farbeElementTHERMHintergrund);
            case LK:
            case LK_AND_RELUCTANCE:
                return colorToRgb(GlobalColors.farbeElementLKHintergrund);
            case RELUCTANCE:
                return colorToRgb(GlobalColors.farbeElementRELBACKGROUND);            
            case CONTROL:
                return colorToRgb(GlobalColors.farbeElementCONTROLHintergrund);
            case NONE:
            default:
                return 0xD3D3D3; // Color.lightGray as RGB
        }
    }
    
    /**
     * @deprecated Use getBackgroundColorRgb() for GUI-free code.
     * For GUI layer only - returns AWT Color.
     */
    @Deprecated(since = "Sprint 15", forRemoval = true)
    java.awt.Color getBackgroundColor() {
        return new java.awt.Color(getBackgroundColorRgb());
    }

    /**
     * Get foreground color as RGB integer value.
     * To convert to AWT Color: {@code new java.awt.Color(getForeGroundColorRgb())}
     */
    int getForeGroundColorRgb() {
        switch (this) {
            case THERMAL:
                return colorToRgb(GlobalColors.farbeFertigElementTHERM);
            case LK:
            case LK_AND_RELUCTANCE:
                return colorToRgb(GlobalColors.farbeFertigElementLK);
            case RELUCTANCE:
                return colorToRgb(GlobalColors.farbeFertigElementRELUCTANCE);
            case CONTROL:
                return colorToRgb(GlobalColors.farbeFertigElementCONTROL);
            case NONE:
            default:
                return 0x808080; // Color.GRAY as RGB           
        }
    }
    
    /**
     * @deprecated Use getForeGroundColorRgb() for GUI-free code.
     * For GUI layer only - returns AWT Color.
     */
    @Deprecated(since = "Sprint 15", forRemoval = true)
    java.awt.Color getForeGroundColor() {
        return new java.awt.Color(getForeGroundColorRgb());
    }
    
    /**
     * Convert java.awt.Color to RGB integer.
     * Only used during transition; remove this method once GlobalColors is refactored.
     */
    private static int colorToRgb(java.awt.Color color) {
        if (color == null) {
            return 0x000000;
        }
        return (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
}
