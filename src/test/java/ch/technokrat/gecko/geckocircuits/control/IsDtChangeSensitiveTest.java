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
package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for IsDtChangeSensitive interface.
 * Tests various implementations and the interface contract.
 */
public class IsDtChangeSensitiveTest {

    @Test
    public void testInterfaceExistence() {
        assertNotNull(IsDtChangeSensitive.class);
    }

    @Test
    public void testInterfaceHasInitWithNewDtMethod() {
        try {
            IsDtChangeSensitive.class.getMethod("initWithNewDt", double.class);
        } catch (NoSuchMethodException e) {
            fail("Interface should have initWithNewDt(double dt) method");
        }
    }

    @Test
    public void testSimpleImplementation() {
        IsDtChangeSensitive impl = createSimpleImplementation();
        assertNotNull(impl);
    }

    @Test
    public void testImplementationCallsInitWithNewDt() {
        MockDtSensitive mock = new MockDtSensitive();
        mock.initWithNewDt(0.001);
        
        assertEquals(0.001, mock.lastDt, 1e-9);
        assertTrue(mock.initCalled);
    }

    @Test
    public void testImplementationWithDifferentDtValues() {
        MockDtSensitive mock = new MockDtSensitive();
        
        mock.initWithNewDt(0.001);
        assertEquals(0.001, mock.lastDt, 1e-9);
        
        mock.initWithNewDt(0.0001);
        assertEquals(0.0001, mock.lastDt, 1e-12);
        
        mock.initWithNewDt(1.0);
        assertEquals(1.0, mock.lastDt, 1e-9);
    }

    @Test
    public void testImplementationWithVerySmallDt() {
        MockDtSensitive mock = new MockDtSensitive();
        double smallDt = 1e-9;
        
        mock.initWithNewDt(smallDt);
        assertEquals(smallDt, mock.lastDt, 1e-18);
    }

    @Test
    public void testImplementationWithZeroDt() {
        MockDtSensitive mock = new MockDtSensitive();
        mock.initWithNewDt(0.0);
        
        assertEquals(0.0, mock.lastDt, 1e-9);
    }

    @Test
    public void testImplementationWithNegativeDt() {
        MockDtSensitive mock = new MockDtSensitive();
        mock.initWithNewDt(-0.001);
        
        assertEquals(-0.001, mock.lastDt, 1e-9);
    }

    @Test
    public void testImplementationWithLargeDt() {
        MockDtSensitive mock = new MockDtSensitive();
        double largeDt = 1000.0;
        
        mock.initWithNewDt(largeDt);
        assertEquals(largeDt, mock.lastDt, 1e-9);
    }

    @Test
    public void testMultipleImplementationsIndependent() {
        MockDtSensitive impl1 = new MockDtSensitive();
        MockDtSensitive impl2 = new MockDtSensitive();
        
        impl1.initWithNewDt(0.001);
        impl2.initWithNewDt(0.01);
        
        assertEquals(0.001, impl1.lastDt, 1e-9);
        assertEquals(0.01, impl2.lastDt, 1e-9);
    }

    private IsDtChangeSensitive createSimpleImplementation() {
        return dt -> {
            // Simple no-op implementation
        };
    }

    /**
     * Mock implementation for testing the interface.
     */
    static class MockDtSensitive implements IsDtChangeSensitive {
        double lastDt;
        boolean initCalled = false;

        @Override
        public void initWithNewDt(final double dt) {
            this.lastDt = dt;
            this.initCalled = true;
        }
    }
}
