/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for CurrentSourceStamper - verifies correct MNA matrix stamping for current sources.
 */
public class CurrentSourceStamperTest {

    private static final double TOLERANCE = 1e-12;
    private CurrentSourceStamper stamper;

    @Before
    public void setUp() {
        stamper = new CurrentSourceStamper();
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("CurrentSourceStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testStampMatrixA_NoContribution() {
        double[][] a = new double[3][3];
        a[1][1] = 0.005; // Pre-existing value
        double[] parameter = CurrentSourceStamper.createDCParameters(1.0);

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Ideal current source should not modify A matrix
        assertEquals("a[1][1] should be unchanged", 0.005, a[1][1], TOLERANCE);
        assertEquals("a[2][2] should be unchanged", 0.0, a[2][2], TOLERANCE);
        assertEquals("a[1][2] should be unchanged", 0.0, a[1][2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_DCSource_Positive() {
        double[] b = new double[3];
        double[] parameter = CurrentSourceStamper.createDCParameters(1.5); // 1.5 A DC
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // Current enters at nodeX (1.5A), leaves at nodeY (-1.5A)
        assertEquals("b[1] should be +I", 1.5, b[1], TOLERANCE);
        assertEquals("b[2] should be -I", -1.5, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_DCSource_Negative() {
        double[] b = new double[3];
        double[] parameter = CurrentSourceStamper.createDCParameters(-2.0); // -2.0 A DC
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        assertEquals("b[1] should be -2.0", -2.0, b[1], TOLERANCE);
        assertEquals("b[2] should be +2.0", 2.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_AddsToExistingValues() {
        double[] b = new double[3];
        b[1] = 1.0; // Pre-existing value
        b[2] = 0.5;
        double[] parameter = CurrentSourceStamper.createDCParameters(1.0);
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        assertEquals("b[1] should be 1.0 + 1.0 = 2.0", 2.0, b[1], TOLERANCE);
        assertEquals("b[2] should be 0.5 - 1.0 = -0.5", -0.5, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_ACSource_AtZero() {
        double[] b = new double[3];
        double[] parameter = CurrentSourceStamper.createACParameters(2.0, 1000.0, 0.0);
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // sin(0) = 0, so current should be 0
        assertEquals("AC source at t=0, phase=0 should give 0 current", 0.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_ACSource_AtQuarterPeriod() {
        double[] b = new double[3];
        double frequency = 1000.0; // 1 kHz
        double period = 1.0 / frequency;
        double time = period / 4.0; // Quarter period
        double[] parameter = CurrentSourceStamper.createACParameters(2.0, frequency, 0.0);
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, time, previousValues);

        // sin(2*pi*f*T/4) = sin(pi/2) = 1.0, so current should be amplitude
        assertEquals("AC source at quarter period should give peak current", 2.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_ACSource_WithPhase() {
        double[] b = new double[3];
        double phase = Math.PI / 2; // 90 degree phase shift
        double[] parameter = CurrentSourceStamper.createACParameters(3.0, 1000.0, phase);
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // sin(phase) = sin(pi/2) = 1.0, so current = 3.0
        assertEquals("AC source with 90 deg phase at t=0", 3.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_ACSource_AtHalfPeriod() {
        double[] b = new double[3];
        double frequency = 1000.0;
        double period = 1.0 / frequency;
        double time = period / 2.0;
        double[] parameter = CurrentSourceStamper.createACParameters(2.0, frequency, 0.0);
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, time, previousValues);

        // sin(2*pi*f*T/2) = sin(pi) = 0
        assertEquals("AC source at half period should be ~0", 0.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_ACSource_NegativePeak() {
        double[] b = new double[3];
        double frequency = 1000.0;
        double period = 1.0 / frequency;
        double time = 3.0 * period / 4.0; // 3/4 period
        double[] parameter = CurrentSourceStamper.createACParameters(2.0, frequency, 0.0);
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, time, previousValues);

        // sin(2*pi*f*3T/4) = sin(3*pi/2) = -1.0
        assertEquals("AC source at 3/4 period should give negative peak", -2.0, b[1], TOLERANCE);
    }

    @Test
    public void testCalculateSourceCurrent_DC() {
        double[] parameter = CurrentSourceStamper.createDCParameters(5.0);
        double current = stamper.calculateSourceCurrent(parameter, 1.0);
        assertEquals("DC current should be constant", 5.0, current, TOLERANCE);
    }

    @Test
    public void testCalculateSourceCurrent_DC_TimeIndependent() {
        double[] parameter = CurrentSourceStamper.createDCParameters(5.0);

        double current1 = stamper.calculateSourceCurrent(parameter, 0.0);
        double current2 = stamper.calculateSourceCurrent(parameter, 1.0);
        double current3 = stamper.calculateSourceCurrent(parameter, 100.0);

        assertEquals("DC current at t=0", 5.0, current1, TOLERANCE);
        assertEquals("DC current at t=1", 5.0, current2, TOLERANCE);
        assertEquals("DC current at t=100", 5.0, current3, TOLERANCE);
    }

    @Test
    public void testCalculateSourceCurrent_NullParameter() {
        double current = stamper.calculateSourceCurrent(null, 0.0);
        assertEquals("null parameter should return 0", 0.0, current, TOLERANCE);
    }

    @Test
    public void testCalculateSourceCurrent_EmptyParameter() {
        double[] parameter = new double[0];
        double current = stamper.calculateSourceCurrent(parameter, 0.0);
        assertEquals("empty parameter should return 0", 0.0, current, TOLERANCE);
    }

    @Test
    public void testCalculateSourceCurrent_SingleParameter() {
        double[] parameter = new double[]{0}; // Only source type, no amplitude
        double current = stamper.calculateSourceCurrent(parameter, 0.0);
        assertEquals("single parameter should return 0", 0.0, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent() {
        double[] parameter = CurrentSourceStamper.createDCParameters(3.5);
        double current = stamper.calculateCurrent(10.0, 5.0, parameter, 1e-6, 0.0);

        // For ideal current source, current is the source value (voltage independent)
        assertEquals("current should be source value", 3.5, current, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight() {
        double admittance = stamper.getAdmittanceWeight(1.0, 1e-6);
        assertEquals("ideal current source has zero admittance", 0.0, admittance, TOLERANCE);
    }

    @Test
    public void testStampVectorBGrounded() {
        double[] b = new double[3];
        double[] parameter = CurrentSourceStamper.createDCParameters(2.5);

        stamper.stampVectorBGrounded(b, 1, parameter, 0.0);

        assertEquals("b[1] should have current", 2.5, b[1], TOLERANCE);
        assertEquals("b[0] should be unchanged", 0.0, b[0], TOLERANCE);
    }

    @Test
    public void testCreateDCParameters() {
        double[] params = CurrentSourceStamper.createDCParameters(1.5);

        assertEquals("should have 4 elements", 4, params.length);
        assertEquals("source type should be DC", CurrentSourceStamper.SOURCE_DC, (int) params[0]);
        assertEquals("current should be 1.5", 1.5, params[1], TOLERANCE);
    }

    @Test
    public void testCreateACParameters() {
        double[] params = CurrentSourceStamper.createACParameters(2.0, 50.0, 0.5);

        assertEquals("should have 4 elements", 4, params.length);
        assertEquals("source type should be AC", CurrentSourceStamper.SOURCE_AC, (int) params[0]);
        assertEquals("amplitude should be 2.0", 2.0, params[1], TOLERANCE);
        assertEquals("frequency should be 50.0", 50.0, params[2], TOLERANCE);
        assertEquals("phase should be 0.5", 0.5, params[3], TOLERANCE);
    }

    @Test
    public void testSourceTypeConstants() {
        assertEquals("DC should be 0", 0, CurrentSourceStamper.SOURCE_DC);
        assertEquals("AC should be 1", 1, CurrentSourceStamper.SOURCE_AC);
    }

    @Test
    public void testACSource_WithMinimalParameters() {
        // AC source with only type and amplitude (no freq/phase)
        double[] parameter = new double[]{CurrentSourceStamper.SOURCE_AC, 1.0};
        double current = stamper.calculateSourceCurrent(parameter, 0.25); // Quarter of default period

        // With default 1Hz and no phase, at t=0.25s, sin(2*pi*0.25) = 1.0
        assertEquals("AC with minimal params at t=0.25", 1.0, current, TOLERANCE);
    }

    @Test
    public void testUnknownSourceType_UsesAmplitude() {
        double[] parameter = new double[]{99, 7.5}; // Unknown type
        double current = stamper.calculateSourceCurrent(parameter, 0.0);

        assertEquals("Unknown type should use amplitude directly", 7.5, current, TOLERANCE);
    }
}
