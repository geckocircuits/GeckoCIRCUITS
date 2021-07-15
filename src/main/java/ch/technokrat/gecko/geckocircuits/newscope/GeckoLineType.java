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

public enum GeckoLineType {
    /**
     * WARNING: Don't change the order of the enumeration constants, ordinal() is used!
     */
    CONNECT_NEIGHBOURS(0, "Connect neigbours"),
    BAR(1, "Bar to x-Axis");    

    static GeckoLineStyle getFromOrdinal(final int ordinal) {
        for (GeckoLineStyle val : GeckoLineStyle.values()) {
            if (val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }
    private final int _code;
    private final String _description;
    
    GeckoLineType(final int code, final String description) {
        this._code = code;
        _description = description;
    }

    public int code() {
        return _code;
    }        

    public static GeckoLineStyle setzeLinienstilSelektiert(final int ordinal) {
        for (GeckoLineStyle val : GeckoLineStyle.values()) {
            if (val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }
    

    public static GeckoLineType getFromCode(final int gLSCode) {
        for (GeckoLineType val : GeckoLineType.values()) {
            if (val._code == gLSCode) {
                return val;
            }
        }
        
        // default:
        return GeckoLineType.CONNECT_NEIGHBOURS;
    }

    @Override
    public String toString() {
        return _description;
    }
    
    
}
