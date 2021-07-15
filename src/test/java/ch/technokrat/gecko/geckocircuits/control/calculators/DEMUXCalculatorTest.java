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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import ch.technokrat.gecko.geckocircuits.control.ReglerDemux;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andy
 */
public class DEMUXCalculatorTest {
    private static final int NO_OUPUTS = 5;
    
    private DEMUXCalculator _demux;
    private ReglerDemux _reglerDemux;
    
    
    @Before
    public void setUp() {
        _reglerDemux = new ReglerDemux();
        _demux = new DEMUXCalculator(NO_OUPUTS, _reglerDemux);
        _demux._inputSignal[0] = new double[NO_OUPUTS];
    }

    @Test
    public void testBerechneYOUT() {
        Random rand = new Random();
        
        for(int i = 0; i < _demux._outputSignal.length; i++) {
            _demux._inputSignal[0][i] = rand.nextDouble();
        }
                    
        _demux.berechneYOUT(1);
        
        for(int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals(_demux._inputSignal[0][i], _demux._outputSignal[i][0], 1e-9);
        }
    }

    @Test(expected=Exception.class)
    public void testInitializeAtSimulationStart() {       
        DEMUXCalculator demux = new DEMUXCalculator(NO_OUPUTS, _reglerDemux);
        // the +1 should produce an error!
        demux._inputSignal[0] = new double[NO_OUPUTS+1];
        demux.initializeAtSimulationStart(1);        
    }
}
