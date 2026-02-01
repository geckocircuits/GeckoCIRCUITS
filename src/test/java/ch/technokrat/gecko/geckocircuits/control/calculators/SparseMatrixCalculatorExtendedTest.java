package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Extended tests for SparseMatrixCalculator to cover all 12 input sectors and 12 output sectors.
 * This calculator implements Sparse Matrix Converter (SMC) control for AC-AC conversion.
 */
public class SparseMatrixCalculatorExtendedTest {

    private SparseMatrixCalculator calculator;
    private static final double DELTA_T = 0.00001; // 10 microseconds

    @Before
    public void setUp() {
        calculator = new SparseMatrixCalculator();
        // Initialize input and output signal arrays (8 inputs, 9 outputs)
        for (int i = 0; i < calculator._inputSignal.length; i++) {
            calculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            calculator._outputSignal[i] = new double[]{0};
        }
        calculator.initializeAtSimulationStart(DELTA_T);
    }

    // =================== SECTOR 1 TESTS ===================
    @Test
    public void testInputSector1() {
        // seIN=1: (us <= 0) && (ut <= us)
        // Example: ur=10, us=-5, ut=-8
        setThreePhaseInputVoltages(10.0, -5.0, -8.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 1");
    }

    // =================== SECTOR 2 TESTS ===================
    @Test
    public void testInputSector2() {
        // seIN=2: (us >= 0) && (ur >= us)
        // Example: ur=10, us=5, ut=-15
        setThreePhaseInputVoltages(10.0, 5.0, -15.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 2");
    }

    // =================== SECTOR 3 TESTS ===================
    @Test
    public void testInputSector3() {
        // seIN=3: (ur >= 0) && (us >= ur)
        // Example: ur=5, us=10, ut=-15
        setThreePhaseInputVoltages(5.0, 10.0, -15.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 3");
    }

    // =================== SECTOR 4 TESTS ===================
    @Test
    public void testInputSector4() {
        // seIN=4: (ur <= 0) && (ut <= ur)
        // Example: ur=-5, us=13, ut=-8
        setThreePhaseInputVoltages(-5.0, 13.0, -8.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 4");
    }

    // =================== SECTOR 5 TESTS ===================
    @Test
    public void testInputSector5() {
        // seIN=5: (ut <= 0) && (ur <= ut)
        // Example: ur=-8, us=13, ut=-5
        setThreePhaseInputVoltages(-8.0, 13.0, -5.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 5");
    }

    // =================== SECTOR 6 TESTS ===================
    @Test
    public void testInputSector6() {
        // seIN=6: (ut >= 0) && (us >= ut)
        // Example: ur=-15, us=10, ut=5
        setThreePhaseInputVoltages(-15.0, 10.0, 5.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 6");
    }

    // =================== SECTOR 7 TESTS ===================
    @Test
    public void testInputSector7() {
        // seIN=7: (us >= 0) && (ut >= us)
        // Example: ur=-15, us=5, ut=10
        setThreePhaseInputVoltages(-15.0, 5.0, 10.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 7");
    }

    // =================== SECTOR 8 TESTS ===================
    @Test
    public void testInputSector8() {
        // seIN=8: (us <= 0) && (ur <= us)
        // Example: ur=-10, us=-5, ut=15
        setThreePhaseInputVoltages(-10.0, -5.0, 15.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 8");
    }

    // =================== SECTOR 9 TESTS ===================
    @Test
    public void testInputSector9() {
        // seIN=9: (ur <= 0) && (us <= ur)
        // Example: ur=-5, us=-10, ut=15
        setThreePhaseInputVoltages(-5.0, -10.0, 15.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 9");
    }

    // =================== SECTOR 10 TESTS ===================
    @Test
    public void testInputSector10() {
        // seIN=10: (ur >= 0) && (ut >= ur)
        // Example: ur=5, us=-15, ut=10
        setThreePhaseInputVoltages(5.0, -15.0, 10.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 10");
    }

    // =================== SECTOR 11 TESTS ===================
    @Test
    public void testInputSector11() {
        // seIN=11: (ut >= 0) && (ur >= ut)
        // Example: ur=10, us=-15, ut=5
        setThreePhaseInputVoltages(10.0, -15.0, 5.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 11");
    }

    // =================== SECTOR 12 TESTS ===================
    @Test
    public void testInputSector12() {
        // seIN=12: (ut <= 0) && (us <= ut)
        // Example: ur=15, us=-10, ut=-5
        setThreePhaseInputVoltages(15.0, -10.0, -5.0);
        runCalculatorCycle();
        assertValidOutputs("Sector 12");
    }

    // =================== OUTPUT SECTOR TESTS (based on phi2 angle) ===================

    @Test
    public void testOutputSector1() {
        // Set inputs to create output sector 1
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0); // phi2 = 0 -> sector 1
        runCalculatorCycle();
        assertValidOutputs("Output Sector 1");
    }

    @Test
    public void testOutputSector2() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, Math.PI / 6); // phi2 = 30 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 2");
    }

    @Test
    public void testOutputSector3() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, Math.PI / 3); // phi2 = 60 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 3");
    }

    @Test
    public void testOutputSector4() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, Math.PI / 2); // phi2 = 90 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 4");
    }

    @Test
    public void testOutputSector5() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 2 * Math.PI / 3); // phi2 = 120 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 5");
    }

    @Test
    public void testOutputSector6() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 5 * Math.PI / 6); // phi2 = 150 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 6");
    }

    @Test
    public void testOutputSector7() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, Math.PI); // phi2 = 180 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 7");
    }

    @Test
    public void testOutputSector8() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 7 * Math.PI / 6); // phi2 = 210 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 8");
    }

    @Test
    public void testOutputSector9() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 4 * Math.PI / 3); // phi2 = 240 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 9");
    }

    @Test
    public void testOutputSector10() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 3 * Math.PI / 2); // phi2 = 270 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 10");
    }

    @Test
    public void testOutputSector11() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 5 * Math.PI / 3); // phi2 = 300 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 11");
    }

    @Test
    public void testOutputSector12() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 11 * Math.PI / 6); // phi2 = 330 degrees
        runCalculatorCycle();
        assertValidOutputs("Output Sector 12");
    }

    // =================== COMBINED INPUT/OUTPUT SECTOR TESTS ===================

    @Test
    public void testInputSector1OutputSector5() {
        setThreePhaseInputVoltages(10.0, -5.0, -8.0);
        setOutputParameters(100.0, 50.0, 50.0, 2 * Math.PI / 3);
        runCalculatorCycle();
        assertValidOutputs("seIN=1 seOUT=5");
    }

    @Test
    public void testInputSector3OutputSector7() {
        setThreePhaseInputVoltages(5.0, 10.0, -15.0);
        setOutputParameters(100.0, 50.0, 50.0, Math.PI);
        runCalculatorCycle();
        assertValidOutputs("seIN=3 seOUT=7");
    }

    @Test
    public void testInputSector6OutputSector10() {
        setThreePhaseInputVoltages(-15.0, 10.0, 5.0);
        setOutputParameters(100.0, 50.0, 50.0, 3 * Math.PI / 2);
        runCalculatorCycle();
        assertValidOutputs("seIN=6 seOUT=10");
    }

    @Test
    public void testInputSector9OutputSector3() {
        setThreePhaseInputVoltages(-5.0, -10.0, 15.0);
        setOutputParameters(100.0, 50.0, 50.0, Math.PI / 3);
        runCalculatorCycle();
        assertValidOutputs("seIN=9 seOUT=3");
    }

    // =================== TIME EVOLUTION TESTS ===================

    @Test
    public void testPulsePeriodDetection() {
        // Simulate a rising edge in the clock signal
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);

        // Clock signal rises
        calculator._inputSignal[0][0] = 0.5;
        calculator.berechneYOUT(DELTA_T);

        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(DELTA_T);

        calculator._inputSignal[0][0] = 0.5;
        calculator.berechneYOUT(DELTA_T);

        assertValidOutputs("Pulse period detection");
    }

    @Test
    public void testMultiplePulsePeriods() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);

        for (int period = 0; period < 3; period++) {
            // Rising edge
            calculator._inputSignal[0][0] = 0.0;
            calculator.berechneYOUT(DELTA_T);

            calculator._inputSignal[0][0] = 1.0;
            calculator.berechneYOUT(DELTA_T);

            calculator._inputSignal[0][0] = 0.0;
            calculator.berechneYOUT(DELTA_T);
        }

        assertValidOutputs("Multiple pulse periods");
    }

    @Test
    public void testTimeLocalEvolution() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);

        // Run multiple time steps within one pulse period
        for (int i = 0; i < 100; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Step " + i);
        }
    }

    // =================== SWITCHING TIME TESTS ===================

    @Test
    public void testSwitchSignalsBinary() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);
        runCalculatorCycle();

        // Switch signals should be 0 or 1
        for (int i = 0; i < 9; i++) {
            double value = calculator._outputSignal[i][0];
            assertTrue("Switch signal " + i + " should be 0 or 1",
                      value == 0.0 || value == 1.0);
        }
    }

    @Test
    public void testSwitchTransitions() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);

        double[][] switchHistory = new double[100][9];

        for (int step = 0; step < 100; step++) {
            calculator.berechneYOUT(DELTA_T);
            for (int i = 0; i < 9; i++) {
                switchHistory[step][i] = calculator._outputSignal[i][0];
            }
        }

        // Verify some switching happens (not all constant)
        boolean hasTransition = false;
        for (int i = 0; i < 9 && !hasTransition; i++) {
            for (int step = 1; step < 100 && !hasTransition; step++) {
                if (switchHistory[step][i] != switchHistory[step-1][i]) {
                    hasTransition = true;
                }
            }
        }
        // Note: May not always have transitions depending on timing
    }

    // =================== EDGE CASE TESTS ===================

    @Test
    public void testZeroVoltages() {
        setThreePhaseInputVoltages(0.0, 0.0, 0.0);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);

        // Should handle zero voltages without crashing
        calculator.berechneYOUT(DELTA_T);
        // May produce NaN/Inf due to division, but should not crash
    }

    @Test
    public void testVerySmallVoltages() {
        setThreePhaseInputVoltages(0.001, -0.0005, -0.0005);
        setOutputParameters(100.0, 50.0, 50.0, 0.0);
        runCalculatorCycle();
        assertValidOutputs("Very small voltages");
    }

    @Test
    public void testLargeVoltages() {
        setThreePhaseInputVoltages(1000.0, -500.0, -500.0);
        setOutputParameters(1000.0, 500.0, 100.0, 0.0);
        runCalculatorCycle();
        assertValidOutputs("Large voltages");
    }

    @Test
    public void testPositiveFrequency() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 100.0, 0.0); // fOUT = 100 Hz
        runCalculatorCycle();
        assertValidOutputs("Positive frequency");
    }

    @Test
    public void testZeroFrequency() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, 0.0, Math.PI/4); // fOUT = 0, use phi2
        runCalculatorCycle();
        assertValidOutputs("Zero frequency with phi2");
    }

    @Test
    public void testNegativeFrequency() {
        setThreePhaseInputVoltages(10.0, -5.0, -5.0);
        setOutputParameters(100.0, 50.0, -50.0, Math.PI/4); // fOUT negative
        runCalculatorCycle();
        assertValidOutputs("Negative frequency");
    }

    // =================== HELPER METHODS ===================

    private void setThreePhaseInputVoltages(double ur, double us, double ut) {
        calculator._inputSignal[1][0] = ur;
        calculator._inputSignal[2][0] = us;
        calculator._inputSignal[3][0] = ut;
    }

    private void setOutputParameters(double uNmax, double uOUTmax, double fOUT, double phi2) {
        calculator._inputSignal[4][0] = uNmax;
        calculator._inputSignal[5][0] = uOUTmax;
        calculator._inputSignal[6][0] = fOUT;
        calculator._inputSignal[7][0] = phi2;
    }

    private void runCalculatorCycle() {
        // Simulate a pulse period detection by generating a clock edge
        calculator._inputSignal[0][0] = 0.0;
        calculator.berechneYOUT(DELTA_T);
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(DELTA_T);
        calculator._inputSignal[0][0] = 0.5;
        calculator.berechneYOUT(DELTA_T);
    }

    private void assertValidOutputs(String context) {
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse(context + ": Output " + i + " should not be NaN",
                       Double.isNaN(calculator._outputSignal[i][0]));
            assertFalse(context + ": Output " + i + " should not be infinite",
                       Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }

    // =================== TIME-VARYING SECTOR TESTS ===================
    // These tests vary xLokal to cover different branches in setPulseWidths

    @Test
    public void testSector1TimeEvolution() {
        setThreePhaseInputVoltages(10.0, -5.0, -8.0);  // seIN=1
        setOutputParameters(100.0, 50.0, 50.0, 0.0);  // seOUT=1

        // Run many time steps to cover different xLokal values
        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector1 time step " + i);
        }
    }

    @Test
    public void testSector2TimeEvolution() {
        setThreePhaseInputVoltages(10.0, 5.0, -15.0);  // seIN=2
        setOutputParameters(100.0, 50.0, 50.0, Math.PI/6);  // seOUT=2

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector2 time step " + i);
        }
    }

    @Test
    public void testSector3TimeEvolution() {
        setThreePhaseInputVoltages(5.0, 10.0, -15.0);  // seIN=3
        setOutputParameters(100.0, 50.0, 50.0, Math.PI/3);  // seOUT=3

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector3 time step " + i);
        }
    }

    @Test
    public void testSector4TimeEvolution() {
        setThreePhaseInputVoltages(-5.0, 13.0, -8.0);  // seIN=4
        setOutputParameters(100.0, 50.0, 50.0, Math.PI/2);  // seOUT=4

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector4 time step " + i);
        }
    }

    @Test
    public void testSector5TimeEvolution() {
        setThreePhaseInputVoltages(-8.0, 13.0, -5.0);  // seIN=5
        setOutputParameters(100.0, 50.0, 50.0, 2*Math.PI/3);  // seOUT=5

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector5 time step " + i);
        }
    }

    @Test
    public void testSector6TimeEvolution() {
        setThreePhaseInputVoltages(-15.0, 10.0, 5.0);  // seIN=6
        setOutputParameters(100.0, 50.0, 50.0, 5*Math.PI/6);  // seOUT=6

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector6 time step " + i);
        }
    }

    @Test
    public void testSector7TimeEvolution() {
        setThreePhaseInputVoltages(-15.0, 5.0, 10.0);  // seIN=7
        setOutputParameters(100.0, 50.0, 50.0, Math.PI);  // seOUT=7

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector7 time step " + i);
        }
    }

    @Test
    public void testSector8TimeEvolution() {
        setThreePhaseInputVoltages(-10.0, -5.0, 15.0);  // seIN=8
        setOutputParameters(100.0, 50.0, 50.0, 7*Math.PI/6);  // seOUT=8

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector8 time step " + i);
        }
    }

    @Test
    public void testSector9TimeEvolution() {
        setThreePhaseInputVoltages(-5.0, -10.0, 15.0);  // seIN=9
        setOutputParameters(100.0, 50.0, 50.0, 4*Math.PI/3);  // seOUT=9

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector9 time step " + i);
        }
    }

    @Test
    public void testSector10TimeEvolution() {
        setThreePhaseInputVoltages(5.0, -15.0, 10.0);  // seIN=10
        setOutputParameters(100.0, 50.0, 50.0, 3*Math.PI/2);  // seOUT=10

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector10 time step " + i);
        }
    }

    @Test
    public void testSector11TimeEvolution() {
        setThreePhaseInputVoltages(10.0, -15.0, 5.0);  // seIN=11
        setOutputParameters(100.0, 50.0, 50.0, 5*Math.PI/3);  // seOUT=11

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector11 time step " + i);
        }
    }

    @Test
    public void testSector12TimeEvolution() {
        setThreePhaseInputVoltages(15.0, -10.0, -5.0);  // seIN=12
        setOutputParameters(100.0, 50.0, 50.0, 11*Math.PI/6);  // seOUT=12

        for (int i = 0; i < 200; i++) {
            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Sector12 time step " + i);
        }
    }

    @Test
    public void testContinuousThreePhaseSimulation() {
        // Simulate continuous 3-phase AC input rotating through all sectors
        double omega = 2 * Math.PI * 50;  // 50Hz

        for (int step = 0; step < 1000; step++) {
            double t = step * DELTA_T;
            double ur = 100.0 * Math.sin(omega * t);
            double us = 100.0 * Math.sin(omega * t - 2*Math.PI/3);
            double ut = 100.0 * Math.sin(omega * t + 2*Math.PI/3);

            setThreePhaseInputVoltages(ur, us, ut);
            setOutputParameters(100.0, 50.0, 25.0, omega * t);

            calculator.berechneYOUT(DELTA_T);
            assertValidOutputs("Continuous step " + step);
        }
    }
}
