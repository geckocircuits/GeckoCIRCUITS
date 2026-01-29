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
 * Tests for numerical edge cases in stampers - very small/large values,
 * near-zero conditions, and numerical stability issues.
 */
public class NumericalEdgeCasesTest {

    private static final double TOLERANCE = 1e-12;
    private ResistorStamper resistorStamper;
    private CapacitorStamper capacitorStamper;
    private InductorStamper inductorStamper;
    private VoltageSourceStamper voltageSourceStamper;

    @Before
    public void setUp() {
        resistorStamper = new ResistorStamper();
        capacitorStamper = new CapacitorStamper();
        inductorStamper = new InductorStamper();
        voltageSourceStamper = new VoltageSourceStamper();
    }

    // ========== Resistor Edge Cases ==========

    @Test
    public void testResistor_ExtremelySmallResistance_ClampsToMinimum() {
        // Very small resistance (near numerical precision limit)
        double admittance = resistorStamper.getAdmittanceWeight(1e-15, 1e-6);

        // Should clamp to MIN_RESISTANCE = 1e-9, giving admittance of 1e9
        assertTrue("Very small resistance should be clamped", admittance > 1e8);
        assertEquals("Clamped admittance should be 1e9", 1e9, admittance, 1.0);
    }

    @Test
    public void testResistor_NegativeResistance_ClampsToMinimum() {
        // Negative resistance should clamp to minimum
        double admittance = resistorStamper.getAdmittanceWeight(-1000.0, 1e-6);

        assertTrue("Negative resistance should clamp to minimum", admittance > 1e8);
        assertEquals("Should clamp to 1e9", 1e9, admittance, 1.0);
    }

    @Test
    public void testResistor_ExtremelyLargeResistance() {
        // Very large resistance (open circuit approximation)
        double admittance = resistorStamper.getAdmittanceWeight(1e15, 1e-6);

        // Admittance should be very small
        assertEquals("Large resistance gives small admittance", 1e-15, admittance, 1e-25);
    }

    @Test
    public void testResistor_CurrentWithNearZeroVoltage() {
        double[] parameter = {1000.0};
        double vx = 1e-14; // Near-zero voltage
        double vy = 0.0;

        double current = resistorStamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        // Current should be proportional to voltage
        assertEquals("Near-zero voltage gives near-zero current", 1e-17, current, 1e-20);
    }

    @Test
    public void testResistor_CurrentWithExtremeVoltage() {
        double[] parameter = {1.0};
        double vx = 1e10; // Very large voltage
        double vy = 0.0;

        double current = resistorStamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        // Current should scale with voltage
        assertEquals("Large voltage gives large current", 1e10, current, 1.0);
    }

    @Test
    public void testResistor_StampMatrixA_WithClampedResistance() {
        double[][] a = new double[3][3];
        double[] parameter = {0.0}; // Zero resistance triggers clamping

        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Should stamp with clamped admittance (1e9)
        assertEquals("Clamped admittance should be 1e9", 1e9, a[1][1], 1.0);
    }

    // ========== Capacitor Edge Cases ==========

    @Test
    public void testCapacitor_ExtremelySmallCapacitance_ClampsToMinimum() {
        // Very small capacitance
        double admittance = capacitorStamper.getAdmittanceWeight(1e-20, 1e-6);

        // Should clamp to MIN_CAPACITANCE = 1e-15
        double expected = 1e-15 / 1e-6; // 1e-9
        assertEquals("Very small capacitance should clamp", expected, admittance, 1e-15);
    }

    @Test
    public void testCapacitor_NegativeCapacitance_ClampsToMinimum() {
        // Negative capacitance should clamp
        double admittance = capacitorStamper.getAdmittanceWeight(-1e-6, 1e-6);

        // Should clamp to minimum
        double expected = 1e-15 / 1e-6;
        assertEquals("Negative capacitance should clamp", expected, admittance, 1e-15);
    }

    @Test
    public void testCapacitor_ExtremelyLargeCapacitance() {
        // Very large capacitance
        double admittance = capacitorStamper.getAdmittanceWeight(1e6, 1e-6);

        // G = C/dt = 1e6 / 1e-6 = 1e12
        assertEquals("Large capacitance gives large admittance", 1e12, admittance, 1.0);
    }

    @Test
    public void testCapacitor_HistoryCurrentWithLargeVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {1e10}; // Extreme previous voltage

        capacitorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = (C/dt) * v_prev = 1.0 * 1e10 = 1e10
        assertEquals("Large history voltage gives large current", 1e10, b[1], 1.0);
    }

    @Test
    public void testCapacitor_HistoryCurrentWithNegativeVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {-1e6}; // Large negative voltage

        capacitorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = (C/dt) * v_prev = 1.0 * (-1e6) = -1e6
        assertEquals("Negative previous voltage gives negative current", -1e6, b[1], 1.0);
    }

    @Test
    public void testCapacitor_CurrentCalculationWithLargeVoltageDifference() {
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double vx = 1e8;
        double vy = -1e8;

        double current = capacitorStamper.calculateCurrent(vx, vy, parameter, dt, 0.0);

        // I = G * (Vx - Vy) = 1.0 * 2e8 = 2e8
        assertEquals("Large voltage difference gives large current", 2e8, current, 1.0);
    }

    // ========== Inductor Edge Cases ==========

    @Test
    public void testInductor_ExtremelySmallInductance_ClampsToMinimum() {
        // Very small inductance
        double admittance = inductorStamper.getAdmittanceWeight(1e-20, 1e-6);

        // Should clamp to MIN_INDUCTANCE = 1e-15
        double expected = 1e-6 / 1e-15; // dt / L_min = 1e9
        assertEquals("Very small inductance should clamp", expected, admittance, 1.0);
    }

    @Test
    public void testInductor_NegativeInductance_ClampsToMinimum() {
        // Negative inductance should clamp
        double admittance = inductorStamper.getAdmittanceWeight(-1e-3, 1e-6);

        double expected = 1e-6 / 1e-15; // dt / L_min
        assertEquals("Negative inductance should clamp", expected, admittance, 1.0);
    }

    @Test
    public void testInductor_ExtremelyLargeInductance() {
        // Very large inductance
        double admittance = inductorStamper.getAdmittanceWeight(1e6, 1e-6);

        // G = dt/L = 1e-6 / 1e6 = 1e-12
        assertEquals("Large inductance gives small admittance", 1e-12, admittance, 1e-20);
    }

    @Test
    public void testInductor_HistoryCurrentWithLargePreviousCurrent() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {1e6}; // Large previous current

        inductorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = i_prev = 1e6
        assertEquals("Large previous current stamped", 1e6, b[1], 1.0);
    }

    @Test
    public void testInductor_HistoryCurrentWithNegativePreviousCurrent() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {-1e6}; // Large negative current

        inductorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = i_prev = -1e6
        assertEquals("Negative previous current stamped", -1e6, b[1], 1.0);
    }

    @Test
    public void testInductor_CurrentCalculationWithHistoryAndVoltage() {
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double previousCurrent = 1e6;
        double vx = 10.0;
        double vy = 5.0;

        double current = inductorStamper.calculateCurrent(vx, vy, parameter, dt, previousCurrent);

        // i(t) = i_prev + (dt/L) * v = 1e6 + (1e-6/1e-3) * 5 = 1e6 + 0.005
        double expected = 1e6 + 0.005;
        assertEquals("Current should include history and new voltage", expected, current, 0.01);
    }

    // ========== VoltageSource Edge Cases ==========

    @Test
    public void testVoltageSource_ExtremeAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 1e10};

        double voltage = voltageSourceStamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("Large amplitude DC should be preserved", 1e10, voltage, 1.0);
    }

    @Test
    public void testVoltageSource_NegativeAmplitude() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, -1e6};

        double voltage = voltageSourceStamper.calculateSourceVoltage(parameter, 0.0);

        assertEquals("Negative amplitude DC should be preserved", -1e6, voltage, 1.0);
    }

    @Test
    public void testVoltageSource_ACAtPeakValue() {
        double amplitude = 100.0;
        double frequency = 50.0; // 50 Hz
        double phase = Math.PI / 2; // Phase to hit peak
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 0.0;

        double voltage = voltageSourceStamper.calculateSourceVoltage(parameter, time);

        // At peak: sin(2*pi*f*t + phase) = sin(pi/2) = 1
        assertEquals("Should hit peak at phase pi/2", amplitude, voltage, 1.0);
    }

    @Test
    public void testVoltageSource_ACAtZeroCrossing() {
        double amplitude = 100.0;
        double frequency = 50.0;
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 0.5 / frequency; // Half period later

        double voltage = voltageSourceStamper.calculateSourceVoltage(parameter, time);

        // sin(2*pi*50*0.01 + 0) = sin(pi) = 0
        assertEquals("Should cross zero at half period", 0.0, voltage, 1e-10);
    }

    @Test
    public void testVoltageSource_ACWithVeryLargeFrequency() {
        double amplitude = 100.0;
        double frequency = 1e9; // GHz range
        double phase = 0.0;
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, amplitude, frequency, phase};
        double time = 1e-12; // Very small time step

        double voltage = voltageSourceStamper.calculateSourceVoltage(parameter, time);

        // Should still produce valid sinusoidal output
        assertTrue("Large frequency AC should produce valid output", Math.abs(voltage) <= amplitude);
    }

    // ========== Trapezoidal Integration ==========

    @Test
    public void testCapacitor_TrapezoidalWithExtremeValues() {
        double capacitance = 1e-20; // Very small, will clamp
        double dt = 1e-15; // Very small time step

        double admittance = capacitorStamper.getAdmittanceWeightTrapezoidal(capacitance, dt);

        // Should clamp to minimum and still produce valid result
        assertTrue("Should produce valid admittance", admittance > 0);
        assertTrue("Should be finite", Double.isFinite(admittance));
    }

    @Test
    public void testInductor_TrapezoidalWithExtremeValues() {
        double inductance = 1e-20; // Very small, will clamp
        double dt = 1e-15; // Very small time step

        double admittance = inductorStamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Should clamp to minimum and still produce valid result
        assertTrue("Should produce valid admittance", admittance > 0);
        assertTrue("Should be finite", Double.isFinite(admittance));
    }

    @Test
    public void testInductor_TrapezoidalHistoryCurrent() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {1e6, 10.0}; // i_prev, v_prev

        inductorStamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // I_hist = i_prev + (dt/2L) * v_prev = 1e6 + (1e-6/2e-3) * 10 = 1e6 + 0.005
        double expected = 1e6 + 0.005;
        assertEquals("Trapezoidal history should include voltage term", expected, b[1], 0.01);
    }

    // ========== Array Bounds ==========

    @Test
    public void testStamper_MatrixStampingWithLargeNodeIndices() {
        double[][] a = new double[1000][1000];
        double[] parameter = {1000.0};

        // Stamp at high indices
        resistorStamper.stampMatrixA(a, 500, 501, 0, parameter, 1e-6);

        // Should not throw exception and values should be correct
        double expectedAdmittance = 1.0 / 1000.0;
        assertEquals("Should stamp at high indices", expectedAdmittance, a[500][500], TOLERANCE);
    }

    @Test
    public void testVoltageSourceStamper_GroundedConfiguration() {
        double[][] a = new double[3][3];

        voltageSourceStamper.stampMatrixAGrounded(a, 1, 2);

        // Check grounded configuration stamping
        assertEquals("Voltage equation coefficient should be 1", 1.0, a[2][1], TOLERANCE);
        assertEquals("KCL coefficient should be 1", 1.0, a[1][2], TOLERANCE);
    }

    @Test
    public void testVoltageSourceStamper_AdditionalMatrixSize() {
        int additionalSize = voltageSourceStamper.getAdditionalMatrixSize();

        // Voltage source adds one row/column for current variable
        assertEquals("Should add 1 to matrix size", 1, additionalSize);
    }
}
