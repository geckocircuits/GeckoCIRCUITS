package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MatrixStamperContractTest {
    private static void assertConductance(IMatrixStamper stamper, double[] parameters, SolverType method, double expected) {
        double[][] matrix = new double[4][4];
        stamper.stampMatrixA(matrix, 1, 2, 3, parameters, 0.001, method);
        assertEquals(expected, matrix[1][1], Math.abs(expected) * 1e-12);
        assertEquals(expected, matrix[2][2], Math.abs(expected) * 1e-12);
        assertEquals(-expected, matrix[1][2], Math.abs(expected) * 1e-12);
        assertEquals(-expected, matrix[2][1], Math.abs(expected) * 1e-12);
        for (double[] row : matrix) {
            assertEquals(0, java.util.Arrays.stream(row).sum(), 1e-12, "KCL conserves terminal current");
        }
    }

    @ParameterizedTest
    @CsvSource({"SOLVER_BE, 2, 0.5", "SOLVER_TRZ, 4, 0.25", "SOLVER_GS, 3, 0.3333333333333333"})
    void reactiveCompanionConductancesRespectTheIntegrationMethod(SolverType method, double capacitorG, double inductorG) {
        assertConductance(new CapacitorStamper(), new double[]{0.002}, method, capacitorG);
        assertConductance(new InductorStamper(), new double[]{0.002}, method, inductorG);
    }

    @Test
    void resistorObeysOhmsLawAndAddsNoIndependentSource() {
        ResistorStamper resistor = new ResistorStamper();
        assertConductance(resistor, new double[]{4}, SolverType.SOLVER_BE, 0.25);
        assertEquals(2, resistor.calculateCurrent(10, 2, new double[]{4}, 0.001, 100));
        assertEquals(-2, resistor.calculateCurrent(2, 10, new double[]{4}, 0.001, 0));
        double[] rhs = {1, 2, 3, 4};
        resistor.stampVectorB(rhs, 1, 2, 3, new double[]{4}, 0.001, 0, null);
        assertArrayEquals(new double[]{1, 2, 3, 4}, rhs);
        assertEquals(1e9, resistor.getAdmittanceWeight(0, 0.001), 1e-5);
    }

    @Test
    void backwardEulerHistorySourcesConserveChargeAndInductorCurrent() {
        CapacitorStamper capacitor = new CapacitorStamper();
        InductorStamper inductor = new InductorStamper();
        double[] rhs = new double[3];
        capacitor.stampVectorB(rhs, 1, 2, 0, new double[]{0.002}, 0.001, 0, new double[]{3});
        assertArrayEquals(new double[]{0, 6, -6}, rhs);
        inductor.stampVectorB(rhs, 1, 2, 0, new double[]{0.002}, 0.001, 0, new double[]{4});
        assertArrayEquals(new double[]{0, 10, -10}, rhs);
        assertEquals(5, inductor.calculateCurrent(3, 1, new double[]{0.002}, 0.001, 4));
        assertEquals(2, capacitor.getAdmittanceWeight(0.002, 0.001));
        assertEquals(0.5, inductor.getAdmittanceWeight(0.002, 0.001));
        for (IMatrixStamper stamper : new IMatrixStamper[]{capacitor, inductor}) {
            rhs = new double[3];
            stamper.stampVectorB(rhs, 1, 2, 0, new double[]{0.002}, 0.001, 0, null);
            assertArrayEquals(new double[3], rhs);
        }
    }

    @Test
    void voltageSourceAddsTheMnaConstraintAndCurrentCoupling() {
        VoltageSourceStamper voltage = new VoltageSourceStamper();
        double[][] matrix = new double[4][4];
        voltage.stampMatrixA(matrix, 1, 2, 3, VoltageSourceStamper.createDCParameters(12), 0.001, SolverType.SOLVER_BE);
        assertArrayEquals(new double[]{0, 0, 0, 1}, matrix[1]);
        assertArrayEquals(new double[]{0, 0, 0, -1}, matrix[2]);
        assertArrayEquals(new double[]{0, 1, -1, 0}, matrix[3]);
        double[] rhs = new double[4];
        voltage.stampVectorB(rhs, 1, 2, 3, VoltageSourceStamper.createDCParameters(12), 0.001, 0, null);
        assertArrayEquals(new double[]{0, 0, 0, 12}, rhs);
        assertEquals(5, voltage.calculateSourceVoltage(VoltageSourceStamper.createACParameters(5, 50, 0), 0.005), 1e-12);
        assertEquals(-5, voltage.calculateSourceVoltage(VoltageSourceStamper.createACParameters(5, 50, Math.PI), 0.005), 1e-12);
    }

    @Test
    void currentSourceHasInfiniteImpedanceAndEqualOppositeInjections() {
        CurrentSourceStamper source = new CurrentSourceStamper();
        double[][] matrix = new double[3][3];
        source.stampMatrixA(matrix, 1, 2, 0, CurrentSourceStamper.createDCParameters(2), 0.001, SolverType.SOLVER_BE);
        assertArrayEquals(new double[3], matrix[1]);
        double[] rhs = new double[3];
        source.stampVectorB(rhs, 1, 2, 0, CurrentSourceStamper.createDCParameters(2), 0.001, 0, null);
        assertArrayEquals(new double[]{0, 2, -2}, rhs);
        assertEquals(3, source.calculateSourceCurrent(CurrentSourceStamper.createACParameters(3, 50, 0), 0.005), 1e-12);
        assertEquals(2, source.calculateCurrent(100, -100, CurrentSourceStamper.createDCParameters(2), 0.001, 0));
    }

    @Test
    void diodeTurnsOnAtForwardThresholdAndBlocksReverseCurrent() {
        DiodeStamper diode = new DiodeStamper(0.1, 1e6, 0.7);
        diode.updateState(0.6, 0, 0, 0);
        assertFalse(diode.isOn());
        assertConductance(diode, null, SolverType.SOLVER_BE, 1e-6);
        diode.updateState(0.7, 0, 0, 0);
        assertTrue(diode.isOn());
        assertTrue(diode.isStateChanged());
        diode.resetStateChange();
        assertFalse(diode.isStateChanged());
        assertConductance(diode, null, SolverType.SOLVER_BE, 10);
        assertEquals(2, diode.calculateCurrent(0.9, 0, null, 0.001, 0), 1e-12);
        double[] rhs = new double[3];
        diode.stampVectorB(rhs, 1, 2, 0, null, 0.001, 0, null);
        assertArrayEquals(new double[]{0, 7, -7}, rhs, 1e-12);
        diode.updateState(0.7, 0, -0.1, 0);
        assertFalse(diode.isOn());
        assertEquals(-1e-6, diode.calculateCurrent(-1, 0, null, 0.001, 0), 1e-15);
        assertConductance(diode, new double[]{2}, SolverType.SOLVER_BE, 0.5);
    }

    @Test
    void idealSwitchFollowsTheGateAndHonorsLegacyCurrentResistance() {
        IdealSwitchStamper device = new IdealSwitchStamper(0.1, 1e6);
        assertConductance(device, null, SolverType.SOLVER_BE, 1e-6);
        device.setGateSignal(1);
        assertTrue(device.isOn());
        assertTrue(device.isStateChanged());
        device.resetStateChange();
        assertFalse(device.isStateChanged());
        assertConductance(device, null, SolverType.SOLVER_BE, 10);
        assertEquals(-20, device.calculateCurrent(0, 2, null, 0.001, 0));
        device.updateState(-100, 100, -10, 0);
        assertTrue(device.isOn(), "voltage and current do not control an ideal switch");
        double[] parameters = IdealSwitchStamper.createParameters(2, 0.1, 1e6, 0);
        device.updateStateFromParameters(parameters);
        assertFalse(device.isOn());
        assertConductance(device, parameters, SolverType.SOLVER_BE, 0.5);
    }
}
