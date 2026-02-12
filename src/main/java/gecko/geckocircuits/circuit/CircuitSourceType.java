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

import gecko.geckocircuits.circuit.circuitcomponents.SourceType;

public enum CircuitSourceType {
    
    QUELLE_DC(SourceType.QUELLE_DC, SourceType.QUELLE_DC_NEW, "DC"),
    QUELLE_SIN(SourceType.QUELLE_SIN, SourceType.QUELLE_SIN_NEW, "SIN"),
    QUELLE_SIGNALGESTEUERT(SourceType.QUELLE_SIGNALGESTEUERT, SourceType.QUELLE_SIGNALGESTEUERT_NEW ,"SIGNAL"),
    QUELLE_CURRENTCONTROLLED_DIRECTLY(SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY, SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW, "CURRENT_CONTROLLED"),
    QUELLE_DIDTCURRENTCONTROLLED(SourceType.QUELLE_DIDTCURRENTCONTROLLED, SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW, "DI_DT_CONTROLLED"),
    QUELLE_VOLTAGECONTROLLED_DIRECTLY(SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY, SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW, "VOLTAGE_CONTROLLED"),
    QUELLE_VOLTAGECONTROLLED_TRANSFORMER(SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER, SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW, "TRANSFORMER");                   
       
    private String _outputString;
    private int _oldGeckoID;
    private int _newGeckoID;
    
    CircuitSourceType(final int oldGeckoID, final int newGeckoID, final String outputString) {
        _oldGeckoID = oldGeckoID;
        _newGeckoID = newGeckoID;
        _outputString = outputString;
    }
    
    public static CircuitSourceType getFromID(final int idValue) {
        for(CircuitSourceType tmp : CircuitSourceType.values()) {
            if(idValue == tmp._oldGeckoID) {
                    return tmp;
            }
        }
        for(CircuitSourceType tmp : CircuitSourceType.values()) {
            if(idValue == tmp._newGeckoID) {
                    return tmp;
            }
        }
        assert false;
        return CircuitSourceType.QUELLE_DC;
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
