package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class SemiconductorStampContractTest {
    enum Device { THYRISTOR, IGBT, MOSFET }

    static IStatefulStamper device(Device type, double on, double off, double drop) {
        return switch (type) {
            case THYRISTOR -> new ThyristorStamper(on, off, drop, 0.1);
            case IGBT -> new IGBTStamper(on, off, drop);
            case MOSFET -> new MOSFETStamper(on, off);
        };
    }

    static double[] parameters(Device type, boolean on) {
        double resistance = on ? 0.25 : 1e6;
        return switch (type) {
            case THYRISTOR -> ThyristorStamper.createParameters(
                    resistance, 1.5, 0.25, 1e6, on ? 1 : 0, 0.1, Double.NEGATIVE_INFINITY);
            case IGBT -> IGBTStamper.createParameters(resistance, 1.5, 0.25, 1e6, on ? 1 : 0);
            case MOSFET -> MOSFETStamper.createParameters(resistance, 0.25, 1e6, on ? 1 : 0);
        };
    }

    static Stream<Arguments> operatingPoints() {
        return Stream.of(Device.values()).flatMap(type ->
                Stream.of(false, true).flatMap(on ->
                    Stream.of("internal", "empty", "single", "explicit").flatMap(mode ->
                        Stream.of(SolverType.values()).map(method -> Arguments.of(type, on, mode, method)))));
    }

    static void assertStamp(IStatefulStamper stamper, double[] parameters, SolverType method,
                            double conductance, double injection, double current) {
        double[][] a = {{7, 0, 0, 0}, {0, 2, 0, 1}, {0, 0, 11, 0}, {0, 1, 0, 3}};
        double[] b = {13, 17, 19, 23};
        stamper.stampMatrixA(a, 1, 3, 2, parameters, 0.001, method);
        stamper.stampVectorB(b, 1, 3, 2, parameters, 0.001, 0.5, null);
        assertArrayEquals(new double[]{7, 0, 0, 0}, a[0]);
        assertArrayEquals(new double[]{0, 2 + conductance, 0, 1 - conductance}, a[1], 1e-10);
        assertArrayEquals(new double[]{0, 0, 11, 0}, a[2]);
        assertArrayEquals(new double[]{0, 1 - conductance, 0, 3 + conductance}, a[3], 1e-10);
        assertArrayEquals(new double[]{13, 17 + injection, 19, 23 - injection}, b, 1e-10);
        assertEquals(72, java.util.Arrays.stream(b).sum(), 1e-10, "Two-terminal source conserves current");
        assertEquals(current, stamper.calculateCurrent(5, 2, parameters, 0.001, 99),
                Math.max(1e-15, Math.abs(current) * 1e-12));
    }

    @ParameterizedTest
    @MethodSource("operatingPoints")
    void stampsAndCurrentAgreeForEveryStateAndIntegrationMethod(
            Device type, boolean on, String mode, SolverType method) {
        IStatefulStamper stamper = device(type, 0.25, 1e6, 1.5);
        boolean explicit = mode.equals("explicit") || mode.equals("single");
        stamper.setState(explicit ? !on : on);
        double[] params = mode.equals("single") ? new double[]{on ? 0.25 : 1e6}
                : explicit ? parameters(type, on) : mode.equals("empty") ? new double[0] : null;
        double[] before = params == null ? null : params.clone();
        // At 3 V: ON is (3 - 1.5)/0.25 = 6 A, or 12 A for a MOSFET.
        double injection = on && type != Device.MOSFET ? 6 : 0;
        double current = on ? (type == Device.MOSFET ? 12 : 6) : 3e-6;
        assertStamp(stamper, params, method, on ? 4 : 1e-6, injection, current);
        assertArrayEquals(before, params, "Stamping must not mutate a component's parameters");
    }

    @ParameterizedTest
    @CsvSource({"THYRISTOR,false", "THYRISTOR,true", "IGBT,false", "IGBT,true", "MOSFET,false", "MOSFET,true"})
    void admittanceWeightUsesTheSuppliedResistance(Device type, boolean on) {
        IStatefulStamper stamper = device(type, 0.25, 1e6, 1.5);
        stamper.setState(on);
        assertEquals(0.5, stamper.getAdmittanceWeight(2, 0.001));
        assertEquals(1e12, stamper.getAdmittanceWeight(0, 0.001));
    }

    @ParameterizedTest
    @CsvSource({
        "IGBT,false,0.25,100,0.01,0,0.03",
        "IGBT,true,20000,1e9,0.00005,0.000075,0.000075",
        "THYRISTOR,true,0.75,1,1.3333333333333333,2,2"
    })
    void internalStateIsIndependentOfLegacyResistanceThresholds(
            Device type, boolean on, double rOn, double rOff, double g, double source, double current) {
        IStatefulStamper stamper = device(type, rOn, rOff, 1.5);
        stamper.setState(on);
        assertStamp(stamper, null, SolverType.SOLVER_BE, g, source, current);
    }

    @ParameterizedTest
    @CsvSource({"THYRISTOR,0", "IGBT,0", "MOSFET,0", "THYRISTOR,-2", "IGBT,-2", "MOSFET,-2"})
    void nonpositiveForwardDropProducesAPureResistiveStamp(Device type, double drop) {
        IStatefulStamper stamper = device(type, 0.25, 1e6, drop);
        stamper.setState(true);
        assertStamp(stamper, null, SolverType.SOLVER_BE, 4, 0, 12);
    }

    @ParameterizedTest
    @CsvSource({"THYRISTOR", "IGBT", "MOSFET"})
    void liveResistanceChangesAffectTheNextStamp(Device type) {
        IStatefulStamper stamper = device(type, 0.25, 1e6, 1.5);
        switch (type) {
            case THYRISTOR -> { ((ThyristorStamper) stamper).setROn(2); ((ThyristorStamper) stamper).setROff(4e6); }
            case IGBT -> { ((IGBTStamper) stamper).setROn(2); ((IGBTStamper) stamper).setROff(4e6); }
            case MOSFET -> { ((MOSFETStamper) stamper).setROn(2); ((MOSFETStamper) stamper).setROff(4e6); }
        }
        stamper.setState(true);
        boolean drop = type != Device.MOSFET;
        assertStamp(stamper, null, SolverType.SOLVER_TRZ, 0.5, drop ? 0.75 : 0, drop ? 0.75 : 1.5);
        stamper.setState(false);
        assertStamp(stamper, null, SolverType.SOLVER_GS, 2.5e-7, 0, 7.5e-7);
    }

    @ParameterizedTest
    @CsvSource({"THYRISTOR,0", "THYRISTOR,1", "IGBT,0", "IGBT,1", "MOSFET,0", "MOSFET,1"})
    void factoryParametersSelectTheOperatingResistance(Device type, int gate) {
        IStatefulStamper stamper = switch (type) {
            case THYRISTOR -> new ThyristorStamper();
            case IGBT -> new IGBTStamper();
            case MOSFET -> new MOSFETStamper();
        };
        double[] params = switch (type) {
            case THYRISTOR -> ThyristorStamper.createDefaultParameters(gate);
            case IGBT -> IGBTStamper.createDefaultParameters(gate);
            case MOSFET -> MOSFETStamper.createDefaultParameters(gate);
        };
        boolean on = gate == 1;
        double source = on && type != Device.MOSFET ? 1500 : 0;
        double current = on ? (type == Device.MOSFET ? 3000 : 1500) : 3e-9;
        assertStamp(stamper, params, SolverType.SOLVER_BE, on ? 1000 : 1e-9, source, current);
    }
    @ParameterizedTest
    @CsvSource({"THYRISTOR", "IGBT"})
    void liveForwardVoltageAffectsBothTriggeringAndTheNextStamp(Device type) {
        IStatefulStamper stamper = device(type, 0.25, 1e6, 1.5);
        if (stamper instanceof ThyristorStamper thyristor) {
            thyristor.setUForward(2.5);
            thyristor.fireTrigger(2, 0, 0);
            assertFalse(thyristor.isOn());
            thyristor.fireTrigger(3, 0, 0);
        } else {
            IGBTStamper igbt = (IGBTStamper) stamper;
            igbt.setUForward(2.5);
            igbt.updateStateWithGate(1, 2, 0);
            assertFalse(igbt.isOn());
            igbt.updateStateWithGate(1, 3, 0);
        }
        assertTrue(stamper.isOn());
        assertStamp(stamper, null, SolverType.SOLVER_BE, 4, 10, 2);
    }

    @ParameterizedTest
    @CsvSource({"THYRISTOR", "IGBT", "MOSFET"})
    void resistanceClampsKeepConductanceFiniteAndBounded(Device type) {
        IStatefulStamper stamper = device(type, -1, Double.POSITIVE_INFINITY, 1.5);
        for (boolean on : new boolean[]{false, true}) {
            stamper.setState(on);
            double[][] a = new double[2][2];
            stamper.stampMatrixA(a, 0, 1, 0, null, 1e-6, SolverType.SOLVER_BE);
            assertEquals(on ? 1e12 : 1e-15, a[0][0], on ? 1e-3 : 1e-27);
            assertTrue(Double.isFinite(stamper.calculateCurrent(3, 0, null, 1e-6, 0)));
        }
    }

}
