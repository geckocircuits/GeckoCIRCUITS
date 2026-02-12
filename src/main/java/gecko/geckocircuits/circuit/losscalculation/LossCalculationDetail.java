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
package gecko.geckocircuits.circuit.losscalculation;

public enum LossCalculationDetail {

    SIMPLE("simple"),
    DETAILED("detailed");
    private final String _displayString;

    LossCalculationDetail(String diplayString) {
        _displayString = diplayString;
    }

    @Override
    public String toString() {
        return _displayString;
    }

    public int getOldGeckoCIRCUITSOrdinal() {
        switch (this) {
            case DETAILED:
                return 2;
            case SIMPLE:
                return 1;
            default:
                assert false;
                return 1;
        }
    }
    
    public static LossCalculationDetail getFromDeprecatedFileVersion(final int number) {
        for(LossCalculationDetail val : LossCalculationDetail.values()) {
            if(val.getOldGeckoCIRCUITSOrdinal() == number) {
                return val;
            }
        }
        assert false;
        return DETAILED;
    }
    
}
