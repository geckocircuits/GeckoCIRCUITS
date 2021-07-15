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
 * @author andy
 */
final class CurveSettings {
            
    private static final double DEFAULT_TRANSPARENCY = 0.85;
    private static final double DEFAULT_AVG_SPAN = 1e-6;
    
    GeckoColor _curveColor = GeckoColor.getNextColor(); 
    GeckoColor _crvFillingDigColor = GeckoColor.LIGHTGRAY;
    boolean _crvFillDigitalCurves = true;
    GeckoSymbol _crvSymbShape = GeckoSymbol.CROSS;
    GeckoLineStyle _curveLineStyle = GeckoLineStyle.SOLID_PLAIN;
    GeckoColor _crvSymbFarbe = GeckoColor.getNextColor();   
    double _crvTransparency = DEFAULT_TRANSPARENCY;
    double _averageSpan = DEFAULT_AVG_SPAN;    
    boolean _curveShowPtSymbols = false;    
    int _crvSymbFrequ = 1; // number for skipping the symbol shape drawing 
    GeckoLineType _lineType = GeckoLineType.CONNECT_NEIGHBOURS;
    
    CurveSettings() {
        // nothing to do!
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\ncurveColor"), _curveColor.code());
        DatenSpeicher.appendAsString(ascii.append("\ncurveLineStyle"), _curveLineStyle.code());
        DatenSpeicher.appendAsString(ascii.append("\nshowCurveSymbols"), _curveShowPtSymbols);
        DatenSpeicher.appendAsString(ascii.append("\nsymbolColor"), _crvSymbFarbe.code());
        DatenSpeicher.appendAsString(ascii.append("\nsymbolShape"), _crvSymbShape.code());
        DatenSpeicher.appendAsString(ascii.append("\ncrvTransparency"), _crvTransparency);
        DatenSpeicher.appendAsString(ascii.append("\nfillDigitalColor"), _crvFillingDigColor.code());
        DatenSpeicher.appendAsString(ascii.append("\nisFillDigitalCurves"), _crvFillDigitalCurves);
        DatenSpeicher.appendAsString(ascii.append("\nlineType"), _lineType.code());
    }

    void importASCII(final TokenMap curveMap) {
        _curveColor = GeckoColor.getFromCode(curveMap.readDataLine("curveColor", _curveColor.code()));
        _curveLineStyle = GeckoLineStyle.getFromCode(curveMap.readDataLine("curveLineStyle", _curveLineStyle.code()));
        _curveShowPtSymbols = curveMap.readDataLine("showCurveSymbols", _curveShowPtSymbols);
        _crvSymbFarbe = GeckoColor.getFromCode(curveMap.readDataLine("symbolColor", _crvSymbFarbe.code()));
        _crvSymbShape = GeckoSymbol.getFromCode(curveMap.readDataLine("symbolShape", _crvSymbShape.code()));
        _crvTransparency = curveMap.readDataLine("crvTransparency", _crvTransparency);
        _crvFillingDigColor = GeckoColor.getFromCode(curveMap.readDataLine("fillDigitalColor", _crvFillingDigColor.code()));
        _crvFillDigitalCurves = curveMap.readDataLine("isFillDigitalCurves", _crvFillDigitalCurves);
        if(curveMap.containsToken("lineType")) {
            _lineType = GeckoLineType.getFromCode(curveMap.readDataLine("lineType", _lineType.code()));
        }        
    
    }
    
}
