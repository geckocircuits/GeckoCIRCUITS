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
package ch.technokrat.gecko.geckocircuits.allg;

public enum SolverType {
    SOLVER_BE(0, "backward-euler"),
    SOLVER_TRZ(1, "trapezoidal"),
    SOLVER_GS(2,"gear-shichman");
    
    private int _oldGeckoIndex;
    private String _displayString;
        
    SolverType(final int oldGeckoIndex, final String displayString) {
        _oldGeckoIndex = oldGeckoIndex;
        _displayString = displayString;
    }

    @Override
    public String toString() {
        return _displayString;
    }
    
    public int getOldGeckoIndex() {
        return _oldGeckoIndex;
    }
   
    public static SolverType getFromOldGeckoIndex(final int oldIndex) {
        for(SolverType type : SolverType.values()) {
            if(type._oldGeckoIndex == oldIndex) {
                return type;
            }
        }
        assert false;
        return SolverType.SOLVER_BE;
    }
    
}
