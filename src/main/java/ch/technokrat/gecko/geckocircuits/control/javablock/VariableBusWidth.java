/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control.javablock;

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class VariableBusWidth {
    Map<Integer, Integer> busMap = new HashMap<Integer, Integer>();

    final UserParameter<Boolean> _fixedOutputBusEnabled;
    final UserParameter<Integer> _fixedOutputBusWidth;
    final UserParameter<Boolean> _useMatrix;
    
    public VariableBusWidth(final ReglerJavaFunction parent) {
        
         _fixedOutputBusWidth = UserParameter.Builder.
            <Integer>start("outputBusWidth", 1).
            longName(I18nKeys.OUTPUT_BUS_WIDTH).
            shortName("outputBusWidth").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(parent, -1).
            build();
    
        
        _fixedOutputBusEnabled = UserParameter.Builder.
            <Boolean>start("fixedBusEnabled", true).
            longName(I18nKeys.FIXED_BUS_ENABLED).
            shortName("fixedBusEnabled").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(parent, -1).
            build();
    
        _useMatrix = UserParameter.Builder.
            <Boolean>start("useMatrix", false).
            longName(I18nKeys.DISPLAY_COMPONENT_NAME_IN_CIRCUIT_SHEET).
            shortName("useMatrix").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(parent, -1).
            build();
    
    }
    
    
            
    final int getBusWidth(final int outputNumber) {
        if(busMap.containsKey(outputNumber)) {
            return busMap.get(outputNumber);
        } else {
            return 1;
        }
        
    }
    
    final void setBusWidth(final int outputNumber, final int busWidth) {
        if(busMap.containsKey(outputNumber)) {
            busMap.remove(outputNumber);
        }
        
        busMap.put(outputNumber, busWidth);
    }

    void exportAsciiIndividual(final StringBuffer ascii) {
        
        int[] indicesList = new int[busMap.size()];
        int[] busWidth = new int[busMap.size()];
        int counter = 0;
        for(Entry<Integer, Integer> entry : busMap.entrySet()) {
            indicesList[counter] = entry.getKey();            
            busWidth[counter] = entry.getValue();
            counter++;
        }
        DatenSpeicher.appendAsString(ascii.append("\nvaluesBusWidth"),busWidth);
        DatenSpeicher.appendAsString(ascii.append("\nindicesBusWidth"),indicesList);
        
    }
    
    void importAscii(final TokenMap tokenMap) {
        int[] indices = new int[0];
        int[] values = new int[0];
        
        if(tokenMap.containsToken("indicesBusWidth[]")) {
            indices = tokenMap.readDataLine("indicesBusWidth[]", indices);
            values = tokenMap.readDataLine("valuesBusWidth[]", values);
        }
        
        for(int i = 0; i < indices.length; i++) {
            busMap.put(indices[i], values[i]);
            
        }
    }
    
}
