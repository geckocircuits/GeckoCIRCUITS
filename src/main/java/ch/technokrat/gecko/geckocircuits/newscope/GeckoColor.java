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

import java.awt.Color;

/**
 *
 * @author Tibor Keresztfalvi
 */
public enum GeckoColor {

    
    /**
     * WARNING: Don't change the order of the enumeration constants, ordinal() is used!
     */
    BLACK (-3444440, Color.BLACK),
    RED (-3444441, Color.RED),
    GREEN (-3444442, Color.GREEN),
    BLUE (-3444443, Color.BLUE),
    DARKGRAY (-3444444, Color.DARK_GRAY),
    GRAY (-3444445, Color.GRAY),
    LIGHTGRAY (-3444446, Color.LIGHT_GRAY),
    WHITE (-3444447, Color.WHITE),
    MAGENTA (-3444448, Color.MAGENTA),
    CYAN (-3444449, Color.CYAN),
    ORANGE (-3444450, Color.ORANGE),
    YELLOW (-3444451, Color.YELLOW),
    DARKGREEN (-3444452, Color.decode("0x006400"));

    
    private static int counter = 0;
    
    static GeckoColor getNextColor() {
        counter++;
        counter = counter % values().length;
        final GeckoColor returnValue = getFromOrdinal(counter);
        
        // don't select these colors, since they are nealy invisible!
        if(returnValue == GeckoColor.WHITE || returnValue == GeckoColor.YELLOW) {
            return GeckoColor.getNextColor();
        }
        return returnValue;
    }
    
    static GeckoColor getNextColor(GeckoColor previous) {
        counter = previous.ordinal()+1;
        counter = counter % values().length;
        final GeckoColor returnValue = getFromOrdinal(counter);
        
        // don't select these colors, since they are nealy invisible!
        if(returnValue == GeckoColor.WHITE || returnValue == GeckoColor.YELLOW) {
            return GeckoColor.getNextColor(returnValue);
        }
        return returnValue;
    }
    

    private final int _code;
    private final Color _color;
    
    GeckoColor(final int code, final Color color) {
        _code = code;
        _color = color;
    }
    
    
        
    
    static GeckoColor getFromOrdinal(final int ordinal) {
        for(GeckoColor val : GeckoColor.values()) {
            if(val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }
            
    public Color getJavaColor() {
        return _color;
    }
    
    public int code() { return _code; }                
    
    static GeckoColor getFromCode(final int code) {
        for(GeckoColor val : GeckoColor.values()) {
            if(val._code == code) {
                return val;
            }
        }
        return GeckoColor.BLACK;
    }
}
