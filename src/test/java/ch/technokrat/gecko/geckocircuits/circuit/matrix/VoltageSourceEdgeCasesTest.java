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
 * Tests for VoltageSourceStamper edge cases and various source types.
 */
public class VoltageSourceEdgeCasesTest {

    private static final double TOLERANCE = 1e-10;
    private VoltageSourceStamper stamper;

    @Before
    public void setUp() {
        stamper = new VoltageSourceStamper();
    }

    // ========== DC Source Edge Cases ==========

    @Test
    public void testCalculateSourceVoltage_DC_ZeroAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 0.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("DC zero should give zero", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_DC_PositiveAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 100.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("DC positive should be constant", 100.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_DC_NegativeAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, -50.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("DC negative should be constant", -50.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_DC_VeryLargeAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 1e10};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("DC very large should be preserved", 1e10, voltage, 1.0);
    }

    @Test
    public void testCalculateSourceVoltage_DC_TimeIndependent() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 50.0};

        double v1 = stamper.calculateSourceVoltage(parameter, 0.0);
        double v2 = stamper.calculateSourceVoltage(parameter, 1e-6);
        double v3 = stamper.calculateSourceVoltage(parameter, 100.0);

        assertEquals("DC at t=0", 50.0, v1, TOLERANCE);
        assertEquals("DC at t=1e-6", 50.0, v2, TOLERANCE);
        assertEquals("DC at t=100", 50.0, v3, TOLERANCE);
    }

    // ========== AC Source Edge Cases ==========

    @Test
    public void testCalculateSourceVoltage_AC_ZeroAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, 0.0, 50.0, 0.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("AC with zero amplitude", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_AtTime0() {
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, 100.0, 50.0, 0.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        // sin(0) = 0
        assertEquals("AC at t=0 should be zero", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_AtQuarterPeriod() {
        double amplitude = 100.0;
        double frequency = 50.0;
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double quarterPeriod = 1.0 / (4 * frequency); // 0.005 seconds

        double voltage = stamper.calculateSourceVoltage(parameter, quarterPeriod);

        // sin(2*pi*50*0.005) = sin(pi/2) = 1
        assertEquals("AC at quarter period should be peak", amplitude, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_AtHalfPeriod() {
        double amplitude = 100.0;
        double frequency = 50.0;
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double halfPeriod = 1.0 / (2 * frequency); // 0.01 seconds

        double voltage = stamper.calculateSourceVoltage(parameter, halfPeriod);

        // sin(2*pi*50*0.01) = sin(pi) = 0
        assertEquals("AC at half period should be zero", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_AtThreeQuarterPeriod() {
        double amplitude = 100.0;
        double frequency = 50.0;
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double threeQuarterPeriod = 3.0 / (4 * frequency);

        double voltage = stamper.calculateSourceVoltage(parameter, threeQuarterPeriod);

        // sin(2*pi*50*0.015) = sin(3*pi/2) = -1
        assertEquals("AC at three-quarter period", -amplitude, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_WithPhaseShift() {
        double amplitude = 100.0;
        double frequency = 50.0;
        double phase = Math.PI / 4; // 45 degrees
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 0.0;

        double voltage = stamper.calculateSourceVoltage(parameter, time);

        // sin(pi/4) = sqrt(2)/2
        double expected = amplitude * Math.sin(phase);
        assertEquals("AC with phase shift at t=0", expected, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_HighFrequency() {
        double amplitude = 50.0;
        double frequency = 1e6; // 1 MHz
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 1e-7; // Very small time

        double voltage = stamper.calculateSourceVoltage(parameter, time);

        // Should produce valid sinusoidal output
        assertTrue("Should be within amplitude bounds", Math.abs(voltage) <= amplitude);
    }

    @Test
    public void testCalculateSourceVoltage_AC_VeryLowFrequency() {
        double amplitude = 100.0;
        double frequency = 0.001; // 1 mHz
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 250.0; // 250 seconds = 1/4 period

        double voltage = stamper.calculateSourceVoltage(parameter, time);

        // Should be near peak
        assertTrue("Should be near peak", voltage > amplitude * 0.9);
    }

    @Test
    public void testCalculateSourceVoltage_AC_NegativeAmplitude() {
        double amplitude = -100.0; // Negative amplitude
        double frequency = 50.0;
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double quarterPeriod = 1.0 / (4 * frequency);

        double voltage = stamper.calculateSourceVoltage(parameter, quarterPeriod);

        // sin(pi/2) = 1, so -100 * 1 = -100
        assertEquals("Negative amplitude at peak", amplitude, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_DefaultFrequency() {
        double amplitude = 100.0;
        // Only 2 parameters (type and amplitude), no frequency
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude};
        double quarterPeriod = 0.25; // 1/4 second (default 1 Hz)

        double voltage = stamper.calculateSourceVoltage(parameter, quarterPeriod);

        // Default frequency = 1 Hz, so sin(2*pi*1*0.25) = sin(pi/2) = 1
        assertEquals("Default 1 Hz frequency", amplitude, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_MissingPhase() {
        double amplitude = 100.0;
        double frequency = 50.0;
        // Only 3 parameters (type, amplitude, frequency), no phase
        // This will use default 1 Hz frequency instead of parameter frequency
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency};
        double quarterPeriod = 0.25; // 1/4 second for 1 Hz default

        double voltage = stamper.calculateSourceVoltage(parameter, quarterPeriod);

        // When parameter.length < 4, it uses default 1 Hz
        // sin(2*pi*1*0.25) = sin(pi/2) = 1
        assertEquals("Should use default 1 Hz", amplitude, voltage, TOLERANCE);
    }

    // ========== Invalid/Edge Parameter Cases ==========

    @Test
    public void testCalculateSourceVoltage_NullParameter() {
        double voltage = stamper.calculateSourceVoltage(null, 0.0);

        assertEquals("Null parameter should return 0", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_EmptyParameter() {
        double[] parameter = {};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("Empty parameter should return 0", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_OneParameterOnly() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("Only type parameter should return 0", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_UnknownSourceType() {
        double[] parameter = {999, 100.0}; // Unknown type

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        // Should use amplitude directly
        assertEquals("Unknown type should use amplitude", 100.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_NegativeSourceType() {
        double[] parameter = {-1, 100.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);

        // Should handle gracefully
        assertEquals("Negative type should use amplitude", 100.0, voltage, TOLERANCE);
    }

    // ========== Matrix Stamping Edge Cases ==========

    @Test
    public void testStampMatrixA_StandardConfiguration() {
        double[][] a = new double[4][4];

        stamper.stampMatrixA(a, 1, 2, 3, new double[]{}, 1e-6);

        // Check standard stamping pattern
        assertEquals("a[3][1] should be +1", 1.0, a[3][1], TOLERANCE);
        assertEquals("a[3][2] should be -1", -1.0, a[3][2], TOLERANCE);
        assertEquals("a[1][3] should be +1", 1.0, a[1][3], TOLERANCE);
        assertEquals("a[2][3] should be -1", -1.0, a[2][3], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_HighNodeIndices() {
        double[][] a = new double[1000][1000];

        stamper.stampMatrixA(a, 500, 501, 999, new double[]{}, 1e-6);

        assertEquals("Should work with high indices", 1.0, a[999][500], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExisting() {
        double[][] a = new double[4][4];
        a[3][1] = 0.5;
        a[3][2] = -0.3;
        a[1][3] = 0.2;
        a[2][3] = -0.1;

        stamper.stampMatrixA(a, 1, 2, 3, new double[]{}, 1e-6);

        // Should add to existing values
        assertEquals("Should add +1", 1.5, a[3][1], TOLERANCE);
        assertEquals("Should add -1", -1.3, a[3][2], TOLERANCE);
        assertEquals("Should add +1", 1.2, a[1][3], TOLERANCE);
        assertEquals("Should add -1", -1.1, a[2][3], TOLERANCE);
    }

    @Test
    public void testStampVectorB_DC() {
        double[] b = new double[4];
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 50.0};

        stamper.stampVectorB(b, 1, 2, 3, parameter, 1e-6, 0.0, null);

        assertEquals("DC should stamp voltage at nodeZ", 50.0, b[3], TOLERANCE);
    }

    @Test
    public void testStampVectorB_AC() {
        double[] b = new double[4];
        double amplitude = 100.0;
        double frequency = 50.0;
        double phase = Math.PI / 2;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 0.0;

        stamper.stampVectorB(b, 1, 2, 3, parameter, 1e-6, time, null);

        // At phase pi/2, sin(pi/2) = 1
        assertEquals("AC at peak should stamp amplitude", amplitude, b[3], TOLERANCE);
    }

    @Test
    public void testStampVectorB_AddsToExisting() {
        double[] b = new double[4];
        b[3] = 10.0; // Pre-existing value
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 50.0};

        stamper.stampVectorB(b, 1, 2, 3, parameter, 1e-6, 0.0, null);

        assertEquals("Should add to existing b value", 60.0, b[3], TOLERANCE);
    }

    // ========== Calculate Current ==========

    @Test
    public void testCalculateCurrent_ReturnsPreviousCurrent() {
        double vx = 10.0;
        double vy = 5.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 100.0};
        double previousCurrent = 42.0;

        double current = stamper.calculateCurrent(vx, vy, parameter, 1e-6, previousCurrent);

        // For voltage source, should return previousCurrent
        assertEquals("Should return previous current", previousCurrent, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ReturnsNegativePreviousCurrent() {
        double previousCurrent = -10.5;

        double current = stamper.calculateCurrent(0, 0, new double[]{}, 1e-6, previousCurrent);

        assertEquals("Should return negative previous current", previousCurrent, current, TOLERANCE);
    }

    // ========== Admittance Weight ==========

    @Test
    public void testGetAdmittanceWeight() {
        double admittance = stamper.getAdmittanceWeight(100.0, 1e-6);

        // Voltage sources should return 0
        assertEquals("Voltage source has no simple admittance", 0.0, admittance, TOLERANCE);
    }

    // ========== Grounded Configuration ==========

    @Test
    public void testStampMatrixAGrounded_Basic() {
        double[][] a = new double[3][3];

        stamper.stampMatrixAGrounded(a, 1, 2);

        // Voltage equation: Vx = Vsource
        assertEquals("Voltage equation coefficient", 1.0, a[2][1], TOLERANCE);
        // KCL at node 1: include source current
        assertEquals("KCL coefficient", 1.0, a[1][2], TOLERANCE);
    }

    @Test
    public void testStampMatrixAGrounded_HighIndices() {
        double[][] a = new double[1000][1000];

        stamper.stampMatrixAGrounded(a, 500, 999);

        assertEquals("Should work at high indices", 1.0, a[999][500], TOLERANCE);
    }

    @Test
    public void testStampMatrixAGrounded_AddsToExisting() {
        double[][] a = new double[3][3];
        a[2][1] = 0.5;
        a[1][2] = 0.3;

        stamper.stampMatrixAGrounded(a, 1, 2);

        assertEquals("Should add to existing", 1.5, a[2][1], TOLERANCE);
        assertEquals("Should add to existing", 1.3, a[1][2], TOLERANCE);
    }

    // ========== Additional Matrix Size ==========

    @Test
    public void testGetAdditionalMatrixSize() {
        int size = stamper.getAdditionalMatrixSize();

        assertEquals("Should return 1", 1, size);
    }
}
