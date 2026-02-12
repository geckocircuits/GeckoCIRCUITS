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

/**
 *
 * @author andy
 */
public final class SliderValues {
    private final double _xValue1;
    private final double _yValue1;
    private final double _xValue2;
    private final double _yValue2;
    
    public SliderValues(final double xValue1, final double yValue1, final double xValue2, final double yValue2) {
        _xValue1 = xValue1;
        _xValue2 = xValue2;
        _yValue1 = yValue1;
        _yValue2 = yValue2;
    }

    SliderValues() {
        _xValue1 = 0;
        _xValue2 = 0;
        _yValue1 = 0;
        _yValue2 = 0;
    }
    
    public double getXValue1() {        
        return _xValue1;
    }
    
    public double getXValue2() {        
        return _xValue2;
    }
    
    public double getYValue1() {
        return _yValue1;
    }
    
    public double getYValue2() {
        return _yValue2;
    }

    @Override
    public String toString() {
        return super.toString() + " " + _xValue1 + " " + _yValue1 + " " + _xValue2 + " " + _yValue2;
    }
    
    
    
}
