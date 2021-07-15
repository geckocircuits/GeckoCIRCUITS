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

/**
 *
 * @author andreas
 */
public final class PowerAnalysisSettings {    
    int[] _powerAnalVoltageIndices = new int[] {-1, -1, -1};
    int[] _powerAnalCurrentIndices = new int[]{-1, -1, -1};

    void exportIndividualControl(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\npowerAnalCurIndices"), _powerAnalCurrentIndices);
        DatenSpeicher.appendAsString(ascii.append("\npowerAnalVoltIndices"), _powerAnalVoltageIndices);
    }

    void importIndividualControl(final TokenMap settingsMap) {
        _powerAnalCurrentIndices = settingsMap.readDataLine("powerAnalCurIndices[]", _powerAnalCurrentIndices);
        _powerAnalVoltageIndices = settingsMap.readDataLine("powerAnalVoltIndices[]", _powerAnalVoltageIndices);
    }
}
