package gecko.core.signal;

import gecko.core.datacontainer.DataContainerSimple;
import gecko.core.datacontainer.DataContainerValuesSettable;
import gecko.core.GeckoInvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CharacteristicsCalculator - signal analysis for RMS, THD, AVG, MIN/MAX.
 */
class CharacteristicsCalculatorTest {

    private DataContainerSimple dataContainer;
    private static final double DELTA = 1e-6;

    @BeforeEach
    void setUp() {
        dataContainer = DataContainerSimple.fabricArrayTimeSeries(1, 200);
    }

    // ====================================================
    // Basic Characteristics Tests
    // ====================================================

    @Test
    void testCalculateFabric_BasicSignal() {
        // Create a simple DC signal at 5V
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertNotNull(calc);
        assertTrue(calc.isValid());
    }

    @Test
    void testGetAVGValue_DCSignal() {
        // DC signal at 5V
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertEquals(5.0, calc.getAVGValue(0), DELTA);
    }

    @Test
    void testGetRMS2Value_DCSignal() {
        // DC signal at 5V - RMS should equal DC value
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertEquals(5.0, calc.getRMS2Value(0), DELTA);
    }

    @Test
    void testGetMinMaxValue_VariableSignal() {
        // Signal varying from -10 to +10
        setRampSignal(dataContainer, 0, -10.0, 10.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertEquals(-10.0, calc.getMinValue(0), 0.3);
        assertEquals(10.0, calc.getMaxValue(0), 0.3);
    }

    @Test
    void testGetPeakToPeakValue() {
        // Signal varying from -10 to +10, peak-to-peak = 20
        setRampSignal(dataContainer, 0, -10.0, 10.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertEquals(20.0, calc.getPeakToPeakValue(0), 0.3);
    }

    // ====================================================
    // Caching Tests
    // ====================================================

    @Test
    void testCalculateFabric_Caching() {
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc1 = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);
        CharacteristicsCalculator calc2 = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        // Should return cached instance
        assertSame(calc1, calc2);
    }

    @Test
    void testCalculateFabric_CachingInvalidatedByDifferentRange() {
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc1 = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);
        CharacteristicsCalculator calc2 = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.5, 0.99);

        // Different range - should return new instance
        assertNotSame(calc1, calc2);
    }

    // ====================================================
    // Exception Tests
    // ====================================================

    @Test
    void testGetChannelCharacteristics_InvalidChannel() {
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertThrows(GeckoInvalidArgumentException.class, () -> {
            calc.getChannelCharacteristics(999);
        });
    }

    @Test
    void testGetChannelCharacteristics_ValidChannel() throws GeckoInvalidArgumentException {
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        double[] characteristics = calc.getChannelCharacteristics(0);

        assertNotNull(characteristics);
        assertEquals(9, characteristics.length);  // 9 total channels
        assertEquals(5.0, characteristics[0], DELTA);  // AVG
        assertEquals(5.0, characteristics[1], DELTA);  // RMS
    }

    // ====================================================
    // Validity Tests
    // ====================================================

    @Test
    void testIsValid_AfterCalculation() {
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        assertTrue(calc.isValid());
    }

    @Test
    void testSetInvalid() {
        setConstantSignal(dataContainer, 0, 5.0, 100);

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(
            dataContainer, 0.0, 0.99);

        calc.setInvalid();
        assertFalse(calc.isValid());
    }

    // ====================================================
    // Helper Methods for Creating Test Data
    // ====================================================

    private void setConstantSignal(DataContainerSimple container, int row, double value, int samples) {
        for (int i = 0; i < samples; i++) {
            container.insertValuesAtEnd(new float[]{(float) value}, i * 0.01);
        }
    }

    private void setRampSignal(DataContainerSimple container, int row, double min, double max, int samples) {
        for (int i = 0; i < samples; i++) {
            double fraction = (double) i / (samples - 1);
            float val = (float) (min + (max - min) * fraction);
            container.insertValuesAtEnd(new float[]{val}, i * 0.01);
        }
    }
}
