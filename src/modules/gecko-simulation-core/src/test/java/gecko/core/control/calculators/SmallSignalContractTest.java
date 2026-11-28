package gecko.core.control.calculators;

import gecko.core.control.SSAShape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class SmallSignalContractTest {
    private double oldTime;
    private double[][] oldBode;

    @BeforeEach
    void saveSharedState() {
        oldTime = AbstractControlCalculatable._time;
        oldBode = SmallSignalCalculator._bode;
        SmallSignalCalculator._bode = new double[3][];
    }

    @AfterEach
    void restoreSharedState() {
        AbstractControlCalculatable.setTime(oldTime);
        SmallSignalCalculator._bode = oldBode;
    }

    private SmallSignalCalculator external() {
        SmallSignalCalculator calculator = new SmallSignalCalculator(1, 8, 80, SSAShape.EXTERNAL, 3, 1, false);
        for (int i = 0; i < 3; i++) { calculator.checkInputWithoutConnectionAndFill(i); }
        calculator.initializeAtSimulationStart(1.0 / 512);
        return calculator;
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "0, -45", "90, -45", "30, 90"})
    void knownTransferGainAndPhaseAreIndependentOfExcitationPhaseAndRingPosition(double excitationPhase, double responsePhase) {
        SmallSignalCalculator calculator = external();
        // H = 2 * exp(j * responsePhase). One complete 8 Hz period contains 64 samples.
        for (int i = 0; i < 77; i++) {
            double angle = 2 * Math.PI * i / 64 + Math.toRadians(excitationPhase);
            calculator._inputSignal[2][0] = Math.sin(angle);
            calculator._inputSignal[0][0] = 2 * Math.sin(angle + Math.toRadians(responsePhase));
            calculator.berechneYOUT(1.0 / 512);
        }
        calculator.tearDownOnPause();
        assertEquals(8, SmallSignalCalculator._bode[0][0]);
        assertEquals(6.020599913, SmallSignalCalculator._bode[1][0], 1e-5);
        assertEquals(responsePhase, SmallSignalCalculator._bode[2][0], 1e-8);
    }

    @Test
    void restartingDoesNotPublishAnIncompleteNewPeriod() {
        SmallSignalCalculator calculator = external();
        calculator._inputSignal[2][0] = 1;
        for (int i = 0; i < 48; i++) { calculator.berechneYOUT(1.0 / 512); }
        calculator.initializeAtSimulationStart(1.0 / 512);
        for (int i = 0; i < 16; i++) { calculator.berechneYOUT(1.0 / 512); }
        calculator.tearDownOnPause();
        assertNull(SmallSignalCalculator._bode[1], "a new run must collect an entire new period");
        assertEquals(16, calculator._numberSamples);
    }

    @Test
    void externalExcitationCanBeAddedWithoutChangingTheMeasuredTransferInput() {
        SmallSignalCalculator calculator = new SmallSignalCalculator(1, 8, 80, SSAShape.EXTERNAL, 3, 1, true);
        for (int i = 0; i < 3; i++) { calculator.checkInputWithoutConnectionAndFill(i); }
        calculator.initializeAtSimulationStart(1.0 / 512);
        calculator._inputSignal[0][0] = 7;
        calculator._inputSignal[1][0] = 10;
        calculator._inputSignal[2][0] = 2;
        calculator.berechneYOUT(1.0 / 512);
        assertEquals(12, calculator._outputSignal[0][0]);
        assertEquals(2, calculator._smallSignalValues[0]);
        assertEquals(7, calculator._measuredValues[0]);
    }

    @Test
    void anotherCalculatorCannotChangeAnExistingExcitationFrequencyGrid() {
        SmallSignalCalculator first = new SmallSignalCalculator(1, 8, 80, SSAShape.SINE, 1, 1, false);
        first.externalSetTime(1.0 / 32);
        // The five odd harmonics at this instant alternate +1, -1, +1, -1, +1.
        assertEquals(1, first.calculateSmallSignal(1.0 / 512), 1e-12);
        new SmallSignalCalculator(1, 16, 32, SSAShape.SINE, 1, 1, false);
        assertEquals(1, first.calculateSmallSignal(1.0 / 512), 1e-12);
    }

    @Test
    void publishedBodeFrequenciesBelongToTheCalculatorThatProducedTheMeasurements() {
        SmallSignalCalculator first = external();
        new SmallSignalCalculator(1, 16, 32, SSAShape.SINE, 1, 1, false);
        for (int i = 0; i < 64; i++) {
            first._inputSignal[2][0] = Math.sin(2 * Math.PI * i / 64);
            first._inputSignal[0][0] = 2 * first._inputSignal[2][0];
            first.berechneYOUT(1.0 / 512);
        }
        first.tearDownOnPause();
        assertArrayEquals(new double[]{8, 24, 40, 56, 72}, SmallSignalCalculator._bode[0]);
        assertEquals(6.020599913, SmallSignalCalculator._bode[1][0], 1e-5);
    }

    @Test
    void timestepChangeRequiresACompleteNewPeriod() {
        SmallSignalCalculator calculator = external();
        for (int i = 0; i < 48; i++) { calculator.berechneYOUT(1.0 / 512); }
        calculator.initWithNewDt(1.0 / 256);
        for (int i = 0; i < 31; i++) { calculator.berechneYOUT(1.0 / 256); }
        calculator.tearDownOnPause();
        assertNull(SmallSignalCalculator._bode[1]);
        calculator.berechneYOUT(1.0 / 256);
        calculator.tearDownOnPause();
        assertNotNull(SmallSignalCalculator._bode[1]);
        assertEquals(32, calculator._numberSamples);
    }

    @Test
    void unexcitedHarmonicsHaveUndefinedGainAndUnresolvableHarmonicsHaveNoPhase() {
        SmallSignalCalculator calculator = external();
        for (int i = 0; i < 64; i++) {
            calculator._inputSignal[2][0] = Math.sin(2 * Math.PI * i / 64);
            calculator._inputSignal[0][0] = calculator._inputSignal[2][0];
            calculator.berechneYOUT(1.0 / 512);
        }
        calculator.tearDownOnPause();
        assertEquals(0, SmallSignalCalculator._bode[1][0], 1e-5);
        for (int i = 1; i < SmallSignalCalculator._bode[0].length; i++) {
            assertTrue(Double.isNaN(SmallSignalCalculator._bode[1][i]));
        }
        calculator.initWithNewDt(1);
        calculator.berechneYOUT(1);
        calculator.berechneYOUT(1);
        calculator.tearDownOnPause();
        for (int i = 0; i < SmallSignalCalculator._bode[0].length; i++) {
            assertTrue(Double.isNaN(SmallSignalCalculator._bode[1][i]));
            assertTrue(Double.isNaN(SmallSignalCalculator._bode[2][i]));
        }
    }

    @ParameterizedTest
    @EnumSource(value = SSAShape.class, names = {"RECTANGLE", "TRIANGLE"})
    void generatedExcitationHasTheSpecifiedAmplitudeAndHalfPeriodAntisymmetry(SSAShape shape) {
        SmallSignalCalculator calculator = new SmallSignalCalculator(2, 8, 80, shape, 1, 1, false);
        calculator.checkInputWithoutConnectionAndFill(0);
        calculator.initializeAtSimulationStart(1.0 / 512);
        double[] period = new double[64];
        for (int i = 0; i < 128; i++) {
            calculator.externalSetTime(i / 512.0);
            calculator.berechneYOUT(1.0 / 512);
            if (i >= 64) { period[i - 64] = calculator._outputSignal[0][0]; }
        }
        assertEquals(-2, java.util.Arrays.stream(period).min().orElseThrow(), 1e-12);
        assertEquals(2, java.util.Arrays.stream(period).max().orElseThrow(), 1e-12);
        for (int i = 0; i < 32; i++) {
            if (shape == SSAShape.RECTANGLE && i == 31) {
                // At exact zero crossings, the strict carrier comparator selects the low level.
                assertEquals(-2, period[i]);
                assertEquals(-2, period[i + 32]);
            } else {
                assertEquals(-period[i], period[i + 32], 1e-12);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = SSAShape.class, names = {"RECTANGLE", "TRIANGLE"})
    void restartReplaysGeneratedExcitationFromItsInitialPhase(SSAShape shape) {
        SmallSignalCalculator calculator = new SmallSignalCalculator(2, 8, 80, shape, 1, 1, false);
        calculator.checkInputWithoutConnectionAndFill(0);
        calculator.initializeAtSimulationStart(1.0 / 512);
        double[] first = new double[37];
        for (int i = 0; i < first.length; i++) {
            calculator.berechneYOUT(1.0 / 512);
            first[i] = calculator._outputSignal[0][0];
        }
        calculator.initializeAtSimulationStart(1.0 / 512);
        for (double expected : first) {
            calculator.berechneYOUT(1.0 / 512);
            assertEquals(expected, calculator._outputSignal[0][0], 1e-12);
        }
    }

    @ParameterizedTest
    @EnumSource(value = SSAShape.class, names = {"RECTANGLE", "TRIANGLE"})
    void timestepChangePreservesExcitationPhase(SSAShape shape) {
        SmallSignalCalculator changed = new SmallSignalCalculator(2, 8, 80, shape, 1, 1, false);
        SmallSignalCalculator reference = new SmallSignalCalculator(2, 8, 80, shape, 1, 1, false);
        for (SmallSignalCalculator calculator : new SmallSignalCalculator[]{changed, reference}) {
            calculator.checkInputWithoutConnectionAndFill(0);
            calculator.initializeAtSimulationStart(1.0 / 512);
            for (int i = 0; i < 5; i++) { calculator.berechneYOUT(1.0 / 512); }
        }
        changed.initWithNewDt(1.0 / 256);
        for (int i = 0; i < 16; i++) {
            changed.berechneYOUT(1.0 / 256);
            reference.berechneYOUT(1.0 / 256);
            assertEquals(reference._outputSignal[0][0], changed._outputSignal[0][0], 1e-12);
        }
    }
}
