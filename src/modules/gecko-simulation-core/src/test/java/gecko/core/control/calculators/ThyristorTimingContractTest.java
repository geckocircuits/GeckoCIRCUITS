package gecko.core.control.calculators;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThyristorTimingContractTest {
    private final double oldTime = AbstractControlCalculatable._time;

    @AfterEach
    void restoreClock() { AbstractControlCalculatable.setTime(oldTime); }

    @ParameterizedTest
    @CsvSource({"25, 25, 15", "50, 50, 30", "60, 60, 90", "50, 100, 15"})
    void synchronizedBridgeHasSixCorrectlyPhasedPulsesWithTheRequestedWidth(double initialHz, double actualHz, double phaseDegrees) {
        double period = 1 / actualHz;
        double dt = period / 1200;
        double pulseWidth = period / 30;
        ThyristorControlCalculator calculator = new ThyristorControlCalculator(0, initialHz, pulseWidth / 2);
        calculator.setOnTime(pulseWidth);
        calculator.setPhaseShift(phaseDegrees - 5);
        calculator._inputSignal[0] = new double[]{5};
        calculator._inputSignal[1] = new double[1];
        List<List<Double>> rising = new ArrayList<>(), falling = new ArrayList<>();
        for (int gate = 0; gate < 6; gate++) {
            rising.add(new ArrayList<>());
            falling.add(new ArrayList<>());
        }
        double[] previous = new double[6];
        // Observe a steady period after initial synchronization and frequency acquisition.
        for (int step = 0; step < 6000; step++) {
            double time = step * dt;
            AbstractControlCalculatable.setTime(time);
            calculator._inputSignal[1][0] = step % 1200 < 600 ? 1 : -1;
            calculator.berechneYOUT(dt);
            for (int gate = 0; gate < 6; gate++) {
                double value = calculator._outputSignal[gate][0];
                assertTrue(value == 0 || value == 1);
                if (step >= 4800 && value != previous[gate]) {
                    (value == 1 ? rising : falling).get(gate).add(time);
                }
                previous[gate] = value;
            }
        }
        // Standard six-pulse bridge order: 1, 6, 2, 4, 3, 5, separated by 60 degrees.
        double[] phaseOffsets = {0, 1.0 / 3, 2.0 / 3, 0.5, 5.0 / 6, 1.0 / 6};
        for (int gate = 0; gate < 6; gate++) {
            assertEquals(1, rising.get(gate).size(), "one rising edge per gate per period");
            assertEquals(1, falling.get(gate).size(), "one falling edge per gate per period");
            double expectedOn = (4 + (phaseDegrees / 360 + phaseOffsets[gate]) % 1) * period;
            assertEquals(expectedOn, rising.get(gate).getFirst(), dt * 1.01);
            assertEquals(pulseWidth, falling.get(gate).getFirst() - rising.get(gate).getFirst(), dt * 1.01);
        }
    }
}
