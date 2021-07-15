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
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 *
 * @author andreas
 */
public class Label {
    final ConnectorType _connectorType;
    final String _labelString;
    
    public Label(final String labelString, final ConnectorType connectorType) {
        _connectorType = connectorType;
        _labelString = labelString;
    }

   
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this._connectorType != null ? this._connectorType.hashCode() : 0);
        hash = 89 * hash + (this._labelString != null ? this._labelString.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Label other = (Label) obj;
        if (this._connectorType != other._connectorType) {
            return false;
        }
        if ((this._labelString == null) ? (other._labelString != null) : !this._labelString.equals(other._labelString)) {
            return false;
        }
        return true;
    }
    
    
            
}
