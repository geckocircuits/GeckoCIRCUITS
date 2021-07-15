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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;

public final class DiagramSettings {
    private static final double DEFAULT_SPACING = 0.2;
    
    private String _nameDiagram = "";        
    private double _yWeightDiagram = DEFAULT_SPACING;    
    
    DiagramSettings() {
        // nothing to do, pure data object
    }
    
    public void setNameDiagram(final String newName) {
        assert newName != null;
        _nameDiagram = newName;
    }        
    
    String getNameDiagram() {
        return _nameDiagram;
    }
    
    public void setWeightDiagram(final double weight) {
        assert weight >= 0;
        assert weight <= 1;
        _yWeightDiagram = weight;
    }
    
    public double getWeightDiagram() {
        return _yWeightDiagram;
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\nnameDiagram"), _nameDiagram);
        DatenSpeicher.appendAsString(ascii.append("\nyWeightDiagram"), _yWeightDiagram);        
    }

    void importASCII(final TokenMap diagramSettingsMap) {
        _nameDiagram = diagramSettingsMap.readDataLine("nameDiagram", _nameDiagram);        
        _yWeightDiagram = diagramSettingsMap.readDataLine("yWeightDiagram", _yWeightDiagram);        
    }
    
    
}
