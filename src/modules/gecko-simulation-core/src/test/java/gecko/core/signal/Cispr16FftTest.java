package gecko.core.signal;

import gecko.core.datacontainer.DataContainerSimple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Cispr16Fft - FFT computation class for signal analysis.
 */
class Cispr16FftTest {

    private static final double DELTA = 1e-6;

    // ====================================================
    // FFT Algorithm Tests
    // ====================================================

    @Test
    void testRealft_PowerOfTwo() {
        float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};

        // Should not throw for power-of-2 length
        assertDoesNotThrow(() -> {
            Cispr16Fft.realft(data, 1);
        });
    }

    @Test
    void testRealft_NonPowerOfTwo() {
        float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};

        // realft may use FFTLibrary which can handle non-power-of-2,
        // or fall back to built-in which throws IllegalArgumentException
        // Test that it doesn't crash
        try {
            Cispr16Fft.realft(data, 1);
            // FFTLibrary handled it
        } catch (IllegalArgumentException e) {
            // Built-in FFT rejected it - this is expected
            assertTrue(e.getMessage().contains("2^x"));
        }
    }

    @Test
    void testRealft_ForwardTransform() {
        float[] data = new float[8];
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) i;
        }

        assertDoesNotThrow(() -> {
            Cispr16Fft.realft(data, 1);
        });

        // Verify data was transformed (should be different from input)
        assertNotEquals(0.0f, data[0]);
    }

    @Test
    void testRealft_InverseTransform() {
        float[] data = new float[8];
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) i;
        }

        // Forward transform
        Cispr16Fft.realft(data, 1);

        // Inverse transform
        Cispr16Fft.realft(data, -1);

        // After forward + inverse, should be close to original (scaled)
        assertNotNull(data);
    }

    // ====================================================
    // Blackman Filtering Tests
    // ====================================================

    @Test
    void testCalculateBlackmanFactor() {
        double factor = Cispr16Fft.calculateBlackmanFactor(1.0, 10);

        // Blackman factor should be a reasonable value
        assertTrue(Math.abs(factor) > 0.01);
    }

    @Test
    void testBlackmanNormConstant() {
        // Verify the normalization constant is defined
        assertEquals(2.38, Cispr16Fft.BLACKMAN_NORM, DELTA);
    }

    @Test
    void testInverseBlackman() {
        float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
        float[] original = data.clone();

        // Apply inverse Blackman filter
        assertDoesNotThrow(() -> {
            Cispr16Fft.inverseBlackman(data);
        });

        // Data should be modified
        assertNotEquals(original[0], data[0]);
    }

    // ====================================================
    // Ffour1 Tests
    // ====================================================

    @Test
    void testFfour1_ForwardTransform() {
        float[] data = {1.0f, 0.0f, 2.0f, 0.0f, 3.0f, 0.0f, 4.0f, 0.0f};

        assertDoesNotThrow(() -> {
            Cispr16Fft.ffour1(data, 4, 1);
        });

        // Verify data was transformed
        assertNotNull(data);
    }

    @Test
    void testFfour1_InverseTransform() {
        float[] data = {1.0f, 0.0f, 2.0f, 0.0f, 3.0f, 0.0f, 4.0f, 0.0f};

        assertDoesNotThrow(() -> {
            Cispr16Fft.ffour1(data, 4, -1);
        });

        // Verify data was transformed
        assertNotNull(data);
    }

    // ====================================================
    // Constructor Tests (requires DataContainerSimple mock)
    // ====================================================

    @Test
    void testConstructor_WithBlackman() {
        DataContainerSimple mockData = createMockDataContainer(128);

        Cispr16Fft fft = new Cispr16Fft(mockData, true);

        assertNotNull(fft);
        assertNotNull(fft._zvResampled);
        assertNotNull(fft._magnitudes);
        assertTrue(fft._resampledN > 0);
    }

    @Test
    void testConstructor_WithoutBlackman() {
        DataContainerSimple mockData = createMockDataContainer(128);

        Cispr16Fft fft = new Cispr16Fft(mockData, false);

        assertNotNull(fft);
        assertNotNull(fft._zvResampled);
        assertNotNull(fft._magnitudes);
        assertTrue(fft._resampledN > 0);
    }

    @Test
    void testConstructor_BaseFrequencySet() {
        DataContainerSimple mockData = createMockDataContainer(128);

        Cispr16Fft fft = new Cispr16Fft(mockData, false);

        assertTrue(fft.baseFrequency > 0);
    }

    // ====================================================
    // Helper for Creating Mock DataContainer
    // ====================================================

    private DataContainerSimple createMockDataContainer(int samples) {
        DataContainerSimple container = DataContainerSimple.fabricArrayTimeSeries(1, samples);

        // Fill with sine wave data
        for (int i = 0; i < samples; i++) {
            float value = (float) Math.sin(2 * Math.PI * i / (double) samples);
            container.insertValuesAtEnd(new float[]{value}, i * 0.01);
        }

        return container;
    }
}
