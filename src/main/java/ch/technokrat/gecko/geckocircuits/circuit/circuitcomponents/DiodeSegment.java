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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

public class DiodeSegment {
    double _startVoltage, _endVoltage;
    final double _startCurrent, _endCurrent;
    public final double _RDiff;
    public final double _uF;
    
    public DiodeSegment(double startVoltage, double endVoltage, double startCurrent, double endCurrent) {
        _startVoltage = startVoltage;
        _endVoltage = endVoltage;
        _startCurrent = startCurrent;
        _endCurrent = endCurrent;
        _RDiff = (_endVoltage - _startVoltage) / (_endCurrent - _startCurrent);
        _uF = calculateUF(endCurrent, startCurrent, endVoltage, startVoltage);
    }
    
    int testIfInInterval(double time, double testVoltage, double stoerGroesse, double acceptanceThreshold) {                
        if(testVoltage < _startVoltage * stoerGroesse + acceptanceThreshold) return -1;
        if(testVoltage > _endVoltage * stoerGroesse  - acceptanceThreshold) return 1;
        return 0;
    }
    
    double calculateUF(double y1, double y2, double x1, double x2) {
        return y1 * (x1 - x2) / (y2 - y1) + x1;
    }

    @Override
    public String toString() {
        return "DiodeSegment " + _uF + " " + _RDiff + " " + _startVoltage + " " + _endVoltage + " " + _startCurrent + " " + _endCurrent;
    }

    void setLargeLowerVoltage() {
        _startVoltage = -Double.MAX_VALUE;
    }
    
    void setLargeUpperVoltage() {
        _endVoltage = Double.MAX_VALUE;
    }            
}