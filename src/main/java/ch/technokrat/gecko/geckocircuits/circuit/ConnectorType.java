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
import java.awt.Color;

/**
 *
 * @author andreas
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
                return SchematischeEingabe2._lkDisplayMode;
            case CONTROL:
                return SchematischeEingabe2._controlDisplayMode;
            case THERMAL:
                return SchematischeEingabe2._controlDisplayMode;
            default:
                return SchematischeEingabe2._lkDisplayMode;
        }        
    }

    Color getBackgroundColor() {
        switch (this) {
            case THERMAL:
                return GlobalColors.farbeElementTHERMHintergrund;
            case LK:
            case LK_AND_RELUCTANCE:
                return GlobalColors.farbeElementLKHintergrund;
            case RELUCTANCE:
                return GlobalColors.farbeElementRELBACKGROUND;            
            case CONTROL:
                return GlobalColors.farbeElementCONTROLHintergrund;
            case NONE:
            default:
                return Color.lightGray;
        }
    }

    Color getForeGroundColor() {
        switch (this) {
            case THERMAL:
                return GlobalColors.farbeFertigElementTHERM;
            case LK:
            case LK_AND_RELUCTANCE:
                return GlobalColors.farbeFertigElementLK;
            case RELUCTANCE:
                return GlobalColors.farbeFertigElementRELUCTANCE;
            case CONTROL:
                return GlobalColors.farbeFertigElementCONTROL;
            case NONE:
            default:
                return Color.GRAY;            
        }
    }
}
