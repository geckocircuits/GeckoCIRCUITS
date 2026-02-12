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
package gecko.geckocircuits.circuit;

public enum ControlSourceType {            
    QUELLE_SIN(402, 0, "SINE"),
    QUELLE_DREIECK(403, 1, "TRIANGLE"),    
    QUELLE_RECHTECK(404, 2, "RECTANGLE"),
    QUELLE_RANDOM(405, 3, "RANDOM"),
    QUELLE_IMPORT(406,4, "IMPORT");
    
    private final String _outputString;
    private final int _oldGeckoID;
    private final int _newGeckoID;
    
    ControlSourceType(final int oldGeckoID, final int newGeckoID, final String outputString) {
        _oldGeckoID = oldGeckoID;
        _newGeckoID = newGeckoID;
        _outputString = outputString;
    }
    
    public static ControlSourceType getFromID(final int idValue) {
        for(ControlSourceType tmp : ControlSourceType.values()) {
            if(idValue == tmp._oldGeckoID) {
                    return tmp;
            }
        }
        for(ControlSourceType tmp : ControlSourceType.values()) {
            if(idValue == tmp._newGeckoID) {
                    return tmp;
            }
        }
        assert false;
        return ControlSourceType.QUELLE_RECHTECK;
    }
    
    public int getOldGeckoID() {
        return _oldGeckoID;
    }       
    
    @Override
    public String toString() {
        return _outputString;
    }

    public double getNewID() {
        return _newGeckoID;
    }
}
