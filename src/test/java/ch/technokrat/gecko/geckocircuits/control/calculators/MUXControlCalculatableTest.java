/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public final class MUXControlCalculatableTest {
    private MUXControlCalculatable _muxCalculator;
    private static final int NO_INPUTS = 6;        
    private static final double DELTA_T = 1e-9;
    private static final double TOL = 1e-23;
    
    @Before
    public void setUp() {
        _muxCalculator = new MUXControlCalculatable(NO_INPUTS);
        for(int i = 0; i < NO_INPUTS; i++) {
            _muxCalculator._inputSignal[i] = new double[1];    
        }
        
    }

    @Test
    public void testBerechneYOUT() {
        Random rand = new Random();
        
        // enshure that the correct number of outputs/inputs is set up!
        assertEquals(_muxCalculator._inputSignal.length, _muxCalculator._outputSignal[0].length);
        
        for(int i = 0; i < _muxCalculator._inputSignal.length; i++) {
            _muxCalculator._inputSignal[i][0] = rand.nextDouble();
        }
                    
        _muxCalculator.berechneYOUT(DELTA_T);
        
        for(int i = 0; i < _muxCalculator._inputSignal.length; i++) {
            assertEquals(_muxCalculator._inputSignal[i][0], _muxCalculator._outputSignal[0][i], TOL);
        }        
    }
}
