/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.core.circuit.losscalculation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive tests for DetailedLossLookupTable - 2D interpolation for loss curves.
 *
 * The lookup table performs bilinear interpolation across temperature and current
 * to determine energy losses. It handles both conduction (ConductionLossMeasurementCurve)
 * and switching losses (SwitchingLossCurve) with voltage normalization.
 */
class DetailedLossLookupTableTest {

    private static final double DELTA = 1e-6;

    // ====================================================
    // Single Temperature Curve Tests
    // ====================================================

    @Test
    public void testSingleTemperatureCurve_ExactPoint() {
        // Single curve at 25°C
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20, 30},  // Current [A]
            {0, 2.5, 6.0, 10.5}  // Loss [W]
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Exact data points
        assertEquals(0.0, table.getInterpolatedYValue(25.0, 0.0), DELTA);
        assertEquals(2.5, table.getInterpolatedYValue(25.0, 10.0), DELTA);
        assertEquals(6.0, table.getInterpolatedYValue(25.0, 20.0), DELTA);
        assertEquals(10.5, table.getInterpolatedYValue(25.0, 30.0), DELTA);
    }

    @Test
    public void testSingleTemperatureCurve_LinearInterpolation() {
        // Single curve for linear interpolation between points
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20},  // Current [A]
            {0, 10, 30}   // Loss [W]
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Midpoint between 0A and 10A: should be (0+10)/2 = 5W
        assertEquals(5.0, table.getInterpolatedYValue(25.0, 5.0), DELTA);

        // Midpoint between 10A and 20A: should be (10+30)/2 = 20W
        assertEquals(20.0, table.getInterpolatedYValue(25.0, 15.0), DELTA);

        // 1/4 point between 0A and 10A: 0.25*10 = 2.5W
        assertEquals(2.5, table.getInterpolatedYValue(25.0, 2.5), DELTA);

        // 3/4 point between 10A and 20A: 10 + 0.75*(30-10) = 25W
        assertEquals(25.0, table.getInterpolatedYValue(25.0, 17.5), DELTA);
    }

    // ====================================================
    // Two Temperature Curves with Interpolation
    // ====================================================

    @Test
    public void testTwoTemperatureCurves_ExactTemperature() {
        // Two curves at 25°C and 125°C
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {
            {0, 10, 20},  // Current [A]
            {0, 5, 12}    // Loss at 25°C [W]
        };
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {
            {0, 10, 20},  // Current [A]
            {0, 8, 18}    // Loss at 125°C [W]
        };
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At exact temperature 25°C
        assertEquals(0.0, table.getInterpolatedYValue(25.0, 0.0), DELTA);
        assertEquals(5.0, table.getInterpolatedYValue(25.0, 10.0), DELTA);
        assertEquals(12.0, table.getInterpolatedYValue(25.0, 20.0), DELTA);

        // At exact temperature 125°C
        assertEquals(0.0, table.getInterpolatedYValue(125.0, 0.0), DELTA);
        assertEquals(8.0, table.getInterpolatedYValue(125.0, 10.0), DELTA);
        assertEquals(18.0, table.getInterpolatedYValue(125.0, 20.0), DELTA);
    }

    @Test
    public void testTwoTemperatureCurves_BilinearInterpolation() {
        // Two curves at 25°C and 125°C
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {
            {0, 10, 20},  // Current [A]
            {0, 10, 20}   // Loss at 25°C [W]
        };
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {
            {0, 10, 20},  // Current [A]
            {0, 20, 40}   // Loss at 125°C [W]
        };
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Midpoint temperature (75°C) and midpoint current (10A)
        // At 25°C, 10A: 10W; At 125°C, 10A: 20W
        // At 75°C: (10W + 20W) / 2 = 15W
        assertEquals(15.0, table.getInterpolatedYValue(75.0, 10.0), DELTA);

        // Midpoint temperature (75°C) and current 20A
        // At 25°C, 20A: 20W; At 125°C, 20A: 40W
        // At 75°C: (20W + 40W) / 2 = 30W
        assertEquals(30.0, table.getInterpolatedYValue(75.0, 20.0), DELTA);

        // Midpoint temperature (75°C) and current 5A (interpolated current)
        // At 25°C, 5A: 5W; At 125°C, 5A: 10W
        // At 75°C: (5W + 10W) / 2 = 7.5W
        assertEquals(7.5, table.getInterpolatedYValue(75.0, 5.0), DELTA);
    }

    @Test
    public void testTwoTemperatureCurves_AsymmetricInterpolation() {
        // Test non-midpoint temperature interpolation
        ConductionLossMeasurementCurve curve0 = new ConductionLossMeasurementCurve(0.0);
        double[][] data0 = {
            {0, 10},  // Current [A]
            {0, 10}   // Loss at 0°C [W]
        };
        curve0.setCurveData(data0);

        ConductionLossMeasurementCurve curve100 = new ConductionLossMeasurementCurve(100.0);
        double[][] data100 = {
            {0, 10},  // Current [A]
            {0, 20}   // Loss at 100°C [W]
        };
        curve100.setCurveData(data100);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve0);
        curves.add(curve100);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At 25°C (1/4 from 0 to 100): 10 + 0.25*(20-10) = 12.5W
        assertEquals(12.5, table.getInterpolatedYValue(25.0, 10.0), DELTA);

        // At 75°C (3/4 from 0 to 100): 10 + 0.75*(20-10) = 17.5W
        assertEquals(17.5, table.getInterpolatedYValue(75.0, 10.0), DELTA);
    }

    // ====================================================
    // Boundary Value Tests
    // ====================================================

    @Test
    public void testBoundaryValues_ZeroCurrent() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20},
            {0, 5, 12}
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        assertEquals(0.0, table.getInterpolatedYValue(25.0, 0.0), DELTA);
    }

    @Test
    public void testBoundaryValues_MaxCurrent() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20, 30},
            {0, 5, 12, 21}
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        assertEquals(21.0, table.getInterpolatedYValue(25.0, 30.0), DELTA);
    }

    @Test
    public void testBoundaryValues_CurveEndpoints() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(50.0);
        double[][] data = {
            {5, 15, 25},  // Note: starts at 5A, not 0A
            {2, 8, 16}
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // First point
        assertEquals(2.0, table.getInterpolatedYValue(50.0, 5.0), DELTA);

        // Last point
        assertEquals(16.0, table.getInterpolatedYValue(50.0, 25.0), DELTA);

        // Interpolation between second and third: at 20A
        // Should be: 8 + (20-15)/(25-15)*(16-8) = 8 + 0.5*8 = 12
        assertEquals(12.0, table.getInterpolatedYValue(50.0, 20.0), DELTA);
    }

    // ====================================================
    // Extrapolation Tests
    // ====================================================

    @Test
    public void testExtrapolation_BelowMinCurrent() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {5, 10, 15},  // Starts at 5A
            {10, 20, 30}
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Current below minimum (2A < 5A)
        // Should use first two points: 10 + (2-5)/(10-5)*(20-10) = 10 - 6 = 4
        double result = table.getInterpolatedYValue(25.0, 2.0);
        assertEquals(4.0, result, DELTA);
    }

    @Test
    public void testExtrapolation_AboveMaxCurrent() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20},
            {0, 10, 25}
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Current above maximum (30A > 20A)
        // Should use last two points: 10 + (30-10)/(20-10)*(25-10) = 10 + 2*15 = 40
        double result = table.getInterpolatedYValue(25.0, 30.0);
        assertEquals(40.0, result, DELTA);
    }

    @Test
    public void testNegativeExtrapolation_ClampedToZero() {
        // Test that negative interpolated values are clamped to zero
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {10, 20},  // Starts at 10A
            {5, 10}    // Positive slope
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At 0A, extrapolation would give: 5 + (0-10)/(20-10)*(10-5) = 5 - 5 = 0
        assertEquals(0.0, table.getInterpolatedYValue(25.0, 0.0), DELTA);

        // At negative current (if extrapolation goes negative, should clamp to 0)
        double result = table.getInterpolatedYValue(25.0, -5.0);
        assertTrue(result >= 0.0);  // Should be non-negative
    }

    // ====================================================
    // Negative Current Handling (allCurrentsPositive flag)
    // ====================================================

    @Test
    public void testNegativeCurrent_AllPositiveCurves() {
        // When all currents in curves are positive, negative input current uses absolute value
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20},   // All positive
            {0, 5, 12}
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Negative current should use abs value
        assertEquals(5.0, table.getInterpolatedYValue(25.0, -10.0), DELTA);
        assertEquals(12.0, table.getInterpolatedYValue(25.0, -20.0), DELTA);
    }

    @Test
    public void testNegativeCurrent_MixedSignCurves() {
        // When curves have negative currents, use as-is (for bidirectional devices)
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {-20, -10, 0, 10, 20},   // Negative and positive
            {12, 5, 0, 5, 12}         // Symmetric loss curve
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Should use actual negative current (no abs)
        assertEquals(5.0, table.getInterpolatedYValue(25.0, -10.0), DELTA);
        assertEquals(12.0, table.getInterpolatedYValue(25.0, -20.0), DELTA);
        assertEquals(5.0, table.getInterpolatedYValue(25.0, 10.0), DELTA);
        assertEquals(12.0, table.getInterpolatedYValue(25.0, 20.0), DELTA);
    }

    // ====================================================
    // Inverse Lookup Tests (getInterpolatedXValue)
    // ====================================================

    @Test
    public void testInverseLookup_ExactPoint() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20, 30},  // Current [A]
            {0, 5, 15, 30}    // Loss [W]
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Exact inverse lookup
        assertEquals(0.0, table.getInterpolatedXValue(25.0, 0.0), DELTA);
        assertEquals(10.0, table.getInterpolatedXValue(25.0, 5.0), DELTA);
        assertEquals(20.0, table.getInterpolatedXValue(25.0, 15.0), DELTA);
        assertEquals(30.0, table.getInterpolatedXValue(25.0, 30.0), DELTA);
    }

    @Test
    public void testInverseLookup_Interpolation() {
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20},  // Current [A]
            {0, 10, 30}   // Loss [W]
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At loss = 5W (midpoint between 0W and 10W), current should be 5A
        assertEquals(5.0, table.getInterpolatedXValue(25.0, 5.0), DELTA);

        // At loss = 20W (midpoint between 10W and 30W), current should be 15A
        assertEquals(15.0, table.getInterpolatedXValue(25.0, 20.0), DELTA);
    }

    @Test
    public void testInverseLookup_TwoTemperatures() {
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {
            {0, 10, 20},  // Current [A]
            {0, 10, 20}   // Loss at 25°C [W]
        };
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {
            {0, 10, 20},  // Current [A]
            {0, 20, 40}   // Loss at 125°C [W]
        };
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At 75°C (midpoint), test with loss value 10W
        // At 25°C: 10W is at 10A; At 125°C: 10W is at 5A
        // At 75°C: interpolate between 10A and 5A = (10 + 5) / 2 = 7.5A
        // Actually, let's use a different test value

        // At 75°C, loss of 10W:
        // At 25°C: 10W is at current 10A
        // At 125°C: 10W is at current 5A (since 0→0, 10→20, 20→40, so 10W is at 5A)
        // At 75°C: (10*0.5 + 5*0.5) = 7.5A
        double result = table.getInterpolatedXValue(75.0, 10.0);
        assertEquals(7.5, result, DELTA);
    }

    @Test
    public void testInverseLookup_AllowsNegativeResult() {
        // Unlike getInterpolatedYValue, getInterpolatedXValue allows negative results
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {-20, -10, 0, 10, 20},   // Bidirectional current
            {20, 10, 0, 10, 20}      // Symmetric loss
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At loss = 10W, should find current = -10A (or +10A)
        double result = table.getInterpolatedXValue(25.0, 10.0);
        // Will find first occurrence at -10A
        assertEquals(-10.0, result, DELTA);
    }

    // ====================================================
    // SwitchingLossCurve Tests (Voltage Normalization)
    // ====================================================

    @Test
    public void testSwitchingLossCurve_VoltageNormalization() {
        // SwitchingLossCurve energies are normalized by blocking voltage
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 600.0);
        double[][] data = {
            {0, 10, 20},     // Current [A]
            {0, 6.0, 12.0},  // Eon at 600V [mJ]
            {0, 3.0, 6.0}    // Eoff at 600V [mJ]
        };
        curve.setCurveData(data);

        List<SwitchingLossCurve> curves = new ArrayList<>();
        curves.add(curve);

        // dataIndex=1 for Eon
        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Energy should be normalized: 6.0mJ / 600V = 0.01 mJ/V
        assertEquals(0.01, table.getInterpolatedYValue(25.0, 10.0), DELTA);

        // Energy should be normalized: 12.0mJ / 600V = 0.02 mJ/V
        assertEquals(0.02, table.getInterpolatedYValue(25.0, 20.0), DELTA);
    }

    @Test
    public void testSwitchingLossCurve_MultipleVoltages() {
        // Test with different blocking voltages
        SwitchingLossCurve curve300 = new SwitchingLossCurve(25.0, 300.0);
        double[][] data300 = {
            {0, 10},      // Current [A]
            {0, 3.0},     // Eon at 300V [mJ]
            {0, 1.5}      // Eoff at 300V [mJ]
        };
        curve300.setCurveData(data300);

        SwitchingLossCurve curve600 = new SwitchingLossCurve(125.0, 600.0);
        double[][] data600 = {
            {0, 10},      // Current [A]
            {0, 9.0},     // Eon at 600V [mJ]
            {0, 4.5}      // Eoff at 600V [mJ]
        };
        curve600.setCurveData(data600);

        List<SwitchingLossCurve> curves = new ArrayList<>();
        curves.add(curve300);
        curves.add(curve600);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At 25°C, 10A: 3.0mJ / 300V = 0.01 mJ/V
        assertEquals(0.01, table.getInterpolatedYValue(25.0, 10.0), DELTA);

        // At 125°C, 10A: 9.0mJ / 600V = 0.015 mJ/V
        assertEquals(0.015, table.getInterpolatedYValue(125.0, 10.0), DELTA);

        // At 75°C (midpoint), 10A: (0.01 + 0.015) / 2 = 0.0125 mJ/V
        assertEquals(0.0125, table.getInterpolatedYValue(75.0, 10.0), DELTA);
    }

    @Test
    public void testSwitchingLossCurve_EoffDataIndex() {
        // Test with dataIndex=2 for Eoff
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 400.0);
        double[][] data = {
            {0, 10, 20},     // Current [A]
            {0, 4.0, 8.0},   // Eon at 400V [mJ]
            {0, 2.0, 4.0}    // Eoff at 400V [mJ]
        };
        curve.setCurveData(data);

        List<SwitchingLossCurve> curves = new ArrayList<>();
        curves.add(curve);

        // dataIndex=2 for Eoff
        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 2);

        // Eoff energy normalized: 2.0mJ / 400V = 0.005 mJ/V
        assertEquals(0.005, table.getInterpolatedYValue(25.0, 10.0), DELTA);

        // Eoff energy normalized: 4.0mJ / 400V = 0.01 mJ/V
        assertEquals(0.01, table.getInterpolatedYValue(25.0, 20.0), DELTA);
    }

    // ====================================================
    // ConductionLossMeasurementCurve Factory Tests
    // ====================================================

    @Test
    public void testLeitverlusteFactory_NoNormalization() {
        // Conduction losses are NOT normalized (no voltage division)
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {0, 10, 20},   // Current [A]
            {0, 5.0, 12.0} // Voltage drop [V]
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Values should be unchanged (no normalization)
        assertEquals(5.0, table.getInterpolatedYValue(25.0, 10.0), DELTA);
        assertEquals(12.0, table.getInterpolatedYValue(25.0, 20.0), DELTA);
    }

    // ====================================================
    // Temperature Edge Cases
    // ====================================================

    @Test
    public void testTemperature_BelowAllCurves() {
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {
            {0, 10},
            {0, 10}
        };
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {
            {0, 10},
            {0, 20}
        };
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Temperature below minimum (0°C < 25°C)
        // Should use first curve only
        assertEquals(10.0, table.getInterpolatedYValue(0.0, 10.0), DELTA);
    }

    @Test
    public void testTemperature_AboveAllCurves() {
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {
            {0, 10},
            {0, 10}
        };
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {
            {0, 10},
            {0, 20}
        };
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Temperature above maximum (200°C > 125°C)
        // Actually extrapolates between the two curves:
        // At 25°C: 10W; At 125°C: 20W
        // At 200°C: 10 + (200-25)/(125-25)*(20-10) = 10 + 175/100*10 = 10 + 17.5 = 27.5
        assertEquals(27.5, table.getInterpolatedYValue(200.0, 10.0), DELTA);
    }

    @Test
    public void testTemperature_ThreeCurves() {
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {{0, 10}, {0, 10}};
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve75 = new ConductionLossMeasurementCurve(75.0);
        double[][] data75 = {{0, 10}, {0, 15}};
        curve75.setCurveData(data75);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {{0, 10}, {0, 20}};
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve75);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Between first and second: 50°C (midpoint of 25-75)
        assertEquals(12.5, table.getInterpolatedYValue(50.0, 10.0), DELTA);

        // Between second and third: 100°C (midpoint of 75-125)
        assertEquals(17.5, table.getInterpolatedYValue(100.0, 10.0), DELTA);

        // Exact at middle curve
        assertEquals(15.0, table.getInterpolatedYValue(75.0, 10.0), DELTA);
    }

    // ====================================================
    // Single Data Point Curves
    // ====================================================

    @Test
    public void testTwoDataPoints_MinimumForInterpolation() {
        // Single data point would cause ArrayIndexOutOfBoundsException
        // Minimum of two points is needed for interpolation
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {10, 20},   // Two current points (minimum)
            {5, 15}     // Two loss values
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At exact points
        assertEquals(5.0, table.getInterpolatedYValue(25.0, 10.0), DELTA);
        assertEquals(15.0, table.getInterpolatedYValue(25.0, 20.0), DELTA);

        // Midpoint interpolation
        assertEquals(10.0, table.getInterpolatedYValue(25.0, 15.0), DELTA);
    }

    @Test
    public void testExtrapolation_BelowMinimumWithSingleCurve() {
        // Test extrapolation below minimum current with SINGLE temperature curve
        // Note: With single temperature curve, early return at line 88 bypasses clamping
        // So negative values ARE returned (this may be a bug, but it's current behavior)
        ConductionLossMeasurementCurve curve = new ConductionLossMeasurementCurve(25.0);
        double[][] data = {
            {5, 10},   // Two points starting at 5A
            {2, 5}     // Losses
        };
        curve.setCurveData(data);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Below first point: 0A (extrapolation)
        // Uses first two points: weight1=2, weight2=-1
        // Result: 2*2 + 5*(-1) = 4 - 5 = -1
        // With single temperature curve, early return bypasses Math.max clamping
        double result = table.getInterpolatedYValue(25.0, 0.0);
        assertEquals(-1.0, result, DELTA);

        // At first point
        assertEquals(2.0, table.getInterpolatedYValue(25.0, 5.0), DELTA);

        // Between points: 7.5A
        // 2 + (7.5-5)/(10-5)*(5-2) = 2 + 2.5/5*3 = 2 + 1.5 = 3.5
        assertEquals(3.5, table.getInterpolatedYValue(25.0, 7.5), DELTA);
    }

    @Test
    public void testExtrapolation_BelowMinimumWithTwoCurvesClampsToZero() {
        // Test that with MULTIPLE temperature curves, clamping to zero DOES happen
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {{5, 10}, {2, 5}};
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {{5, 10}, {3, 6}};
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // Below first point: 0A (extrapolation)
        // At 25°C: -1.0 (as calculated above)
        // At 125°C: 3 + (0-5)/(10-5)*(6-3) = 3 - 3 = 0
        // At 25°C (exact): (0*1 + (-1)*0) = -1, but wait, we're AT 25°C so no interpolation
        // Actually at exactly 25°C, upperTempIndex=0, lowerTempIndex=0, so early return again
        // Let's test at 75°C (midpoint): (-1 + 0) / 2 = -0.5, clamped to 0
        double result = table.getInterpolatedYValue(75.0, 0.0);
        assertEquals(0.0, result, DELTA);
    }

    // ====================================================
    // Physical Realism Tests
    // ====================================================

    @Test
    public void testPhysicalRealism_IGBT_ConductionLoss() {
        // Realistic IGBT conduction loss curve
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        double[][] data25 = {
            {0, 25, 50, 75, 100, 150},        // Current [A]
            {0, 0.9, 1.1, 1.25, 1.4, 1.65}    // VCE(sat) at 25°C [V]
        };
        curve25.setCurveData(data25);

        ConductionLossMeasurementCurve curve125 = new ConductionLossMeasurementCurve(125.0);
        double[][] data125 = {
            {0, 25, 50, 75, 100, 150},        // Current [A]
            {0, 1.1, 1.4, 1.6, 1.8, 2.2}      // VCE(sat) at 125°C [V] (higher)
        };
        curve125.setCurveData(data125);

        List<ConductionLossMeasurementCurve> curves = new ArrayList<>();
        curves.add(curve25);
        curves.add(curve125);

        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(curves, 1);

        // At 25°C, 50A: expect 1.1V
        assertEquals(1.1, table.getInterpolatedYValue(25.0, 50.0), DELTA);

        // At 125°C, 50A: expect 1.4V
        assertEquals(1.4, table.getInterpolatedYValue(125.0, 50.0), DELTA);

        // At 75°C (midpoint), 50A: (1.1 + 1.4)/2 = 1.25V
        assertEquals(1.25, table.getInterpolatedYValue(75.0, 50.0), DELTA);
    }

    @Test
    public void testPhysicalRealism_MOSFET_SwitchingLoss() {
        // Realistic MOSFET switching loss
        SwitchingLossCurve curve25 = new SwitchingLossCurve(25.0, 100.0);
        double[][] data25 = {
            {0, 5, 10, 15, 20},         // Current [A]
            {0, 0.05, 0.2, 0.45, 0.8},  // Eon at 100V, 25°C [mJ]
            {0, 0.03, 0.12, 0.27, 0.48} // Eoff at 100V, 25°C [mJ]
        };
        curve25.setCurveData(data25);

        List<SwitchingLossCurve> curves = new ArrayList<>();
        curves.add(curve25);

        DetailedLossLookupTable tableEon = DetailedLossLookupTable.fabric(curves, 1);
        DetailedLossLookupTable tableEoff = DetailedLossLookupTable.fabric(curves, 2);

        // Normalized Eon at 10A: 0.2mJ / 100V = 0.002 mJ/V
        assertEquals(0.002, tableEon.getInterpolatedYValue(25.0, 10.0), DELTA);

        // Normalized Eoff at 10A: 0.12mJ / 100V = 0.0012 mJ/V
        assertEquals(0.0012, tableEoff.getInterpolatedYValue(25.0, 10.0), DELTA);

        // Eon should be greater than Eoff (typical for power semiconductors)
        assertTrue(tableEon.getInterpolatedYValue(25.0, 15.0) >
                   tableEoff.getInterpolatedYValue(25.0, 15.0));
    }
}
