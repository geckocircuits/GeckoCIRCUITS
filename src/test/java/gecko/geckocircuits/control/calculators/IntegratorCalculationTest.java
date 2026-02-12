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
package gecko.geckocircuits.control.calculators;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author andreas
 */
public class IntegratorCalculationTest {
    /**
     * the deltaT is quite "large" here, possibly this will lead to a test failure in the future.
     * But this should encourage the programmers only to increase the integration accuracy...
     */
    private static final double dt = 2e-5;
    
    private IntegratorCalculation integrator;
    
    @Before
    public void setUp() {
        integrator = new IntegratorCalculation(1, 0, -1000, 1000);
        integrator._inputSignal[0] = new double[]{0};
        integrator._inputSignal[1] = new double[]{0};
    }
    
    @After
    public void tearDown() {
        integrator = null;
    }
    

    @Test
    public void testIntegration() {
        
        integrator.initializeAtSimulationStart(dt);        
        for(double time = 0; time < Math.PI; time+= dt) {
            integrator._inputSignal[0][0] = -Math.sin(time);
            integrator.berechneYOUT(dt);
            // the integral of a sine is the cosine!
            assertEquals(Math.cos(time)-1, integrator._outputSignal[0][0], 1e-9);        
        }
        
        // the integral of a negateive half wave sine is -2!
        assertEquals(-2, integrator._outputSignal[0][0], 1e-9);        
    }
    
    @Test
    public void testReset() {
        integrator._inputSignal[1][0] = 1; // set reset signal
        
        integrator.initializeAtSimulationStart(dt);
        
        for(double time = 0; time < Math.PI; time+= dt) {
            if(time > Math.PI/2) {
                integrator._inputSignal[1][0] = 0; // now, do normal integration!
            }
            integrator._inputSignal[0][0] = -Math.sin(time);
            integrator.berechneYOUT(dt);
        }
        // the integral of a negateive half wave sine is -2!
        assertEquals(-1, integrator._outputSignal[0][0], 1e-5);        
    }

    @Test
    public void testSetA1Val() {
        integrator.setA1Val(-2);
        integrator.initializeAtSimulationStart(dt);
        
        for(double time = 0; time < Math.PI; time+= dt) {
            integrator._inputSignal[0][0] = -Math.sin(time);
            integrator.berechneYOUT(dt);
        }
        // the integral of a negateive half wave sine is -2!
        assertEquals(4, integrator._outputSignal[0][0], 1e-5);        
    }
    
    @Test
    public void testInitialValue() {
        double initValue = -10;        
        integrator = new IntegratorCalculation(1, initValue, -1000, 1000);
        integrator._inputSignal[0] = new double[]{0};
        integrator._inputSignal[1] = new double[]{0};
        
        integrator.initializeAtSimulationStart(dt);
        
        for(double time = 0; time < Math.PI; time+= dt) {
            integrator._inputSignal[0][0] = -Math.sin(time);
            integrator.berechneYOUT(dt);
        }
        // the integral of a negateive half wave sine is -2!
        assertEquals(-12, integrator._outputSignal[0][0], 1e-5);        
    }
               

    @Test
    public void testSetMinMax() {
        integrator.setMinMax(-1, 0);
        integrator.initializeAtSimulationStart(dt);                
        for(double time = 0; time < Math.PI; time+= dt) {
            integrator._inputSignal[0][0] = -Math.sin(time);
            integrator.berechneYOUT(dt);
        }
        assertEquals(-1, integrator._outputSignal[0][0], 1e-5);        
        integrator.setMinMax(-1, 0);
        
        // from the limit -1, we integrate up to zero, the upper
        // limit. 
        for(double time = Math.PI; time < 2*Math.PI; time+= dt) {
            integrator._inputSignal[0][0] = -Math.sin(time);            
            integrator.berechneYOUT(dt);
            if(time < Math.PI * (1.500)) {
                assertTrue(integrator._outputSignal[0][0] < 0);        
            }
        }                        
        assertEquals(0, integrator._outputSignal[0][0], 1e-5);        
    }
}
