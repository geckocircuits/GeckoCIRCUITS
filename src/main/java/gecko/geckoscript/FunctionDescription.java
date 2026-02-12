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
package gecko.geckoscript;

public final class FunctionDescription {

    final String _declarationString;
    final String _detailsString;   

    public FunctionDescription(final String declarationString, final String detailsString) {
        _declarationString = declarationString;
        _detailsString = "<html><body width='600'>" + "<font face=\"Courier New\" size=\"3\"><b>" + _declarationString + " </b></font><br><br>"
                + detailsString + "</html>";
    }

    @Override
    public String toString() {
        return _declarationString;
    }
}
