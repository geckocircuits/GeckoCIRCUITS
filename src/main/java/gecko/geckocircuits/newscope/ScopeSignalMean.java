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
package gecko.geckocircuits.newscope;

public final class ScopeSignalMean extends AbstractScopeSignal {
    private double _averagingTime;
    final ScopeSignalRegular _connectedScopeSignal;        
    
    public ScopeSignalMean(final AbstractScopeSignal connectedScopeSignal, final double averagingTime) {
        super();
        _connectedScopeSignal = (ScopeSignalRegular) connectedScopeSignal;
        _averagingTime = averagingTime;
    }

    
    public int getConnectedScopeInputIndex() {
        return _connectedScopeSignal.getSignalIndex();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ScopeSignalMean other = (ScopeSignalMean) obj;
        if (this._connectedScopeSignal.getSignalIndex() != other.getConnectedScopeInputIndex()) {
            return false;
        }        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this.getConnectedScopeInputIndex();
        hash = 67 * hash + (int) (Double.doubleToLongBits(this._averagingTime) ^ (Double.doubleToLongBits(this._averagingTime) >>> 32));
        hash = 67 * hash + (this._connectedScopeSignal != null ? this._connectedScopeSignal.hashCode() : 0);
        return hash;
    }

    public void setAverageTime(final double value) {
        _averagingTime = value;
    }

    public double getAveragingTime() {
        return _averagingTime;
    }

    @Override
    public String getSignalName() {
        return "<" + _connectedScopeSignal.getSignalName() + ">";
    }                
}
