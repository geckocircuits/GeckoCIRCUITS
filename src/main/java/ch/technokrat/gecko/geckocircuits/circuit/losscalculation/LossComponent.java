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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

/**
 * This is an enum to differentiate between different loss components being calculated:
 * To allow the user to see only switching, or only conduction losses, or both.
 * @author anstupar
 */
public enum LossComponent {
    TOTAL, CONDUCTION, SWITCHING;
    
    private final static String saveStringTotal = "total";
    private final static String saveStringConduction = "conduction";
    private final static String saveStringSwitching = "switching";
    
    @Override
    public String toString() {
        String description;
        switch(this) {
            case TOTAL:
                description = "Total losses";
                break;
            case CONDUCTION:
                description = "Conduction losses";
                break;
            case SWITCHING:
                description = "Switching losses";
                break;
            default:
                description = "";
                break;
        }
        return description;
    }
    
    public String getSaveString() {
        String saveString;
         switch(this) {
            case CONDUCTION:
                saveString = saveStringConduction;
                break;
            case SWITCHING:
                saveString = saveStringSwitching;
                break;
            case TOTAL:
            default:
                saveString = saveStringTotal;
                break;

        }
        return saveString;
    }
    
    
    public static LossComponent getEnumFromSaveString(final String saveString) {
        if (saveStringConduction.equals(saveString)) {
            return CONDUCTION;
        }
        else if (saveStringSwitching.equals(saveString)) {
            return SWITCHING;          
        }
        else {
            return TOTAL;
        }
    }
    
}
