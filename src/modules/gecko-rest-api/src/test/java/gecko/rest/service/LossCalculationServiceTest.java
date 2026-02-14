package gecko.rest.service;

import gecko.rest.model.loss.ConductionLossRequest;
import gecko.rest.model.loss.DetailedLossRequest;
import gecko.rest.model.loss.LossCurveData;
import gecko.rest.model.loss.LossResponse;
import gecko.rest.model.loss.SwitchingLossRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LossCalculationService.
 * Validates loss calculation logic using gecko-simulation-core classes.
 */
class LossCalculationServiceTest {

    private final LossCalculationService service = new LossCalculationService();

    @Test
    void testCalculateSwitchingLoss_atReferenceVoltage() {
        // At reference voltage, no scaling should occur
        SwitchingLossRequest request = new SwitchingLossRequest(
            10.0,   // current
            600.0,  // voltage
            125.0,  // temperature
            600.0,  // referenceVoltage (same as voltage)
            0.001,  // turnOnEnergy
            0.0015  // turnOffEnergy
        );

        LossResponse response = service.calculateSwitchingLoss(request);

        assertNotNull(response);
        assertEquals(0.0025, response.totalLoss(), 1e-9, "Total = 0.001 + 0.0015");
        assertEquals(0.0025, response.switchingLoss(), 1e-9);
        assertEquals(0.0, response.conductionLoss(), 1e-9);
        assertEquals("simple_switching", response.method());
    }

    @Test
    void testCalculateSwitchingLoss_withVoltageScaling() {
        // At 800V vs 600V reference, should scale by 800/600 = 1.333
        SwitchingLossRequest request = new SwitchingLossRequest(
            10.0,   // current
            800.0,  // voltage (higher than reference)
            125.0,  // temperature
            600.0,  // referenceVoltage
            0.003,  // turnOnEnergy
            0.003   // turnOffEnergy
        );

        LossResponse response = service.calculateSwitchingLoss(request);

        assertNotNull(response);
        // Expected: (0.003 + 0.003) * (800/600) = 0.006 * 1.333... = 0.008
        assertEquals(0.008, response.totalLoss(), 1e-9);
        assertEquals(0.008, response.switchingLoss(), 1e-9);
    }

    @Test
    void testCalculateConductionLoss_resistiveOnly() {
        // With zero threshold, pure resistive loss
        ConductionLossRequest request = new ConductionLossRequest(
            10.0,  // current
            0.1,   // onResistance
            0.0    // thresholdVoltage = 0
        );

        LossResponse response = service.calculateConductionLoss(request);

        assertNotNull(response);
        // P = I * (Vth + I * Ron) = 10 * (0 + 10 * 0.1) = 10 * 1 = 10W
        assertEquals(10.0, response.totalLoss(), 1e-9);
        assertEquals(10.0, response.conductionLoss(), 1e-9);
        assertNull(response.switchingLoss());
        assertEquals("simple_conduction", response.method());
    }

    @Test
    void testCalculateConductionLoss_withThreshold() {
        // Diode-like: threshold + resistance
        ConductionLossRequest request = new ConductionLossRequest(
            10.0,  // current
            0.05,  // onResistance
            0.7    // thresholdVoltage (diode forward drop)
        );

        LossResponse response = service.calculateConductionLoss(request);

        assertNotNull(response);
        // P = I * (Vth + I * Ron) = 10 * (0.7 + 10 * 0.05) = 10 * 1.2 = 12W
        assertEquals(12.0, response.totalLoss(), 1e-9);
        assertEquals(12.0, response.conductionLoss(), 1e-9);
    }

    @Test
    void testCalculateDetailedLoss_singleTemperature() {
        // Create test curves at 25°C
        LossCurveData switchCurve25 = new LossCurveData(
            25.0,  // temperature
            600.0, // blockingVoltage
            new double[][]{
                {0, 10, 20},           // current [A]
                {0, 0.001, 0.002},     // turn-on energy [J]
                {0, 0.0015, 0.003}     // turn-off energy [J]
            }
        );

        LossCurveData condCurve25 = new LossCurveData(
            25.0,  // temperature
            null,  // no blocking voltage for conduction
            new double[][]{
                {0, 0.7, 1.4},  // voltage [V]
                {0, 10, 20}     // current [A]
            }
        );

        DetailedLossRequest request = new DetailedLossRequest(
            10.0,  // current
            25.0,  // temperature (matches curve)
            List.of(switchCurve25),
            List.of(condCurve25)
        );

        LossResponse response = service.calculateDetailedLoss(request);

        assertNotNull(response);
        assertTrue(response.totalLoss() > 0, "Total loss should be positive");
        assertNotNull(response.switchingLoss(), "Should have switching component");
        assertNotNull(response.conductionLoss(), "Should have conduction component");
        assertEquals("detailed_interpolation", response.method());

        // At I=10A, Tj=25C (exact match):
        // Switching: E_on = 0.001J, E_off = 0.0015J, Total = 0.0025J
        // Conduction: V = 0.7V, P = I*V = 10*0.7 = 7W
        assertEquals(0.0025, response.switchingLoss(), 1e-6);
        assertEquals(7.0, response.conductionLoss(), 1e-6);
    }

    @Test
    void testCalculateDetailedLoss_multipleTemperatures() {
        // Curves at two temperatures: 25°C and 125°C
        LossCurveData switchCurve25 = new LossCurveData(
            25.0, 600.0,
            new double[][]{{0, 10}, {0, 0.001}, {0, 0.0015}}
        );

        LossCurveData switchCurve125 = new LossCurveData(
            125.0, 600.0,
            new double[][]{{0, 10}, {0, 0.0015}, {0, 0.002}} // Higher losses at high temp
        );

        LossCurveData condCurve25 = new LossCurveData(
            25.0, null,
            new double[][]{{0, 0.7}, {0, 10}}
        );

        LossCurveData condCurve125 = new LossCurveData(
            125.0, null,
            new double[][]{{0, 0.9}, {0, 10}} // Higher voltage drop at high temp
        );

        // Request at midpoint temperature: 75°C
        DetailedLossRequest request = new DetailedLossRequest(
            10.0,  // current
            75.0,  // temperature (between 25 and 125)
            List.of(switchCurve25, switchCurve125),
            List.of(condCurve25, condCurve125)
        );

        LossResponse response = service.calculateDetailedLoss(request);

        assertNotNull(response);
        assertTrue(response.totalLoss() > 0);
        assertNotNull(response.switchingLoss());
        assertNotNull(response.conductionLoss());

        // At 75°C (halfway between 25 and 125), losses should be interpolated
        // Switching should be between 0.0025J (at 25°C) and 0.0035J (at 125°C)
        assertTrue(response.switchingLoss() > 0.0025);
        assertTrue(response.switchingLoss() < 0.0035);

        // Conduction should be between 7W (at 25°C) and 9W (at 125°C)
        assertTrue(response.conductionLoss() > 7.0);
        assertTrue(response.conductionLoss() < 9.0);
    }
}
