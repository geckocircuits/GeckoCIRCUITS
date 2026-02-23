package gecko.core.signal;

import gecko.core.datacontainer.DataContainerSimple;
import gecko.core.datacontainer.DataContainerValuesSettable;
import gecko.core.GeckoInvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FourierGUIless - GUI-less Fourier analysis for GeckoSCRIPT.
 */
class FourierGUIlessTest {

    private DataContainerSimple dataContainer;
    private static final double DELTA = 1e-3;

    @BeforeEach
    void setUp() {
        dataContainer = DataContainerSimple.fabricArrayTimeSeries(1, 200);
    }

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    void testConstructor_ValidParameters() {
        setConstantSignal(dataContainer, 0, 5.0, 128);

        FourierGUIless fourier = new FourierGUIless(dataContainer, 0.0, 1.0, 10);

        assertNotNull(fourier);
    }

    // ====================================================
    // doFourier Tests
    // ====================================================

    @Test
    void testDoFourier_ValidSignal() throws GeckoInvalidArgumentException {
        // Create a simple DC signal
        setConstantSignal(dataContainer, 0, 5.0, 128);

        FourierGUIless fourier = new FourierGUIless(dataContainer, 0.0, 1.27, 5);
        double[][][] result = fourier.doFourier();

        assertNotNull(result);
        assertEquals(4, result.length);  // [anVals, bnVals, cnVals, jnVals]
    }

    @Test
    void testDoFourier_InvalidHarmonics_Negative() {
        setConstantSignal(dataContainer, 0, 5.0, 128);

        // Create with invalid harmonics
        FourierGUIless fourier = new FourierGUIless(dataContainer, 0.0, 1.0, -1);

        assertThrows(GeckoInvalidArgumentException.class, () -> {
            fourier.doFourier();
        });
    }

    @Test
    void testDoFourier_InvalidRange() {
        setConstantSignal(dataContainer, 0, 5.0, 128);

        // Create with invalid range (end before start)
        FourierGUIless fourier = new FourierGUIless(dataContainer, 1.0, 0.0, 5);

        assertThrows(GeckoInvalidArgumentException.class, () -> {
            fourier.doFourier();
        });
    }

    // ====================================================
    // Result Structure Tests
    // ====================================================

    @Test
    void testDoFourier_ResultStructure() throws GeckoInvalidArgumentException {
        setConstantSignal(dataContainer, 0, 5.0, 128);

        FourierGUIless fourier = new FourierGUIless(dataContainer, 0.0, 1.27, 5);
        double[][][] result = fourier.doFourier();

        // Check array dimensions
        assertEquals(4, result.length);

        // Each sub-array should have dimensions [rowLength][harmonics+1]
        for (int i = 0; i < 4; i++) {
            assertNotNull(result[i]);
            assertEquals(1, result[i].length);  // 1 signal
            assertEquals(6, result[i][0].length);  // 0 to 5 harmonics = 6 values
        }
    }

    // ====================================================
    // Helper Methods for Creating Test Data
    // ====================================================

    private void setConstantSignal(DataContainerSimple container, int row, double value, int samples) {
        for (int i = 0; i < samples; i++) {
            container.insertValuesAtEnd(new float[]{(float) value}, i * 0.01);
        }
    }

    private void setSineWave(DataContainerSimple container, int row, double amplitude, double frequency, int samples) {
        for (int i = 0; i < samples; i++) {
            double time = i * 0.01;
            float val = (float) (amplitude * Math.sin(2 * Math.PI * frequency * time));
            container.insertValuesAtEnd(new float[]{val}, time);
        }
    }
}
