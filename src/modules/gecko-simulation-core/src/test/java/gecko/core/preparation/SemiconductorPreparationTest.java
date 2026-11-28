package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import gecko.core.allg.SolverType;
import static org.junit.jupiter.api.Assertions.*;

class SemiconductorPreparationTest {
    static CircuitModel gatedSwitch(int type, double[] parameters, double gateValue) {
        var model = CircuitPreparationTest.resistorCircuit();
        var resistor = model.getCircuitComponents().get(1);
        resistor.setParameter("param0", 1.0);
        resistor.setNumericParameters(new double[]{1});
        resistor.setTerminalYLabels(new String[]{"2"});
        model.addCircuitComponent(CircuitPreparationTest.component(type, "device", parameters, "2", "0"));
        model.addControlComponent(CircuitPreparationTest.component(4, "command", new double[]{404, gateValue, 1, 0, 0, 0.5}, "", "gate"));
        var gate = CircuitPreparationTest.component(6, "gate", new double[0], "gate", "");
        gate.setParameterStrings(new String[]{"device"});
        model.addControlComponent(gate);
        return model;
    }

    static void assertCurrents(PreparedCircuit circuit, double... expected) {
        var actual = CircuitPreparationTest.currents(circuit);
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) assertEquals(expected[index], actual.get(index), 1e-10);
    }

    @Test
    void idealSwitchOverridesChangeTheOffAndFollowingGatedElectricalSteps() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(gatedSwitch(7, new double[]{5, 1, 5}, 1));
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var changed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(),
                        Map.of("device.onResistance", 2.0, "device.offResistance", 11.0))));
        assertCurrents(original.execution(), 1, 3, 3);
        assertCurrents(changed.execution(), 0.5, 2, 2);
        assertCurrents(original.execution(), 1, 3, 3);
    }
    @Test
    void mosfetOverridesUseItsSavedSlotsAndChangeBlockingAndConductingCurrent() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(gatedSwitch(28, new double[]{5, 0, 1, 5}, 1));
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var changed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(),
                        Map.of("device.onResistance", 2.0, "device.offResistance", 11.0))));
        assertCurrents(original.execution(), 1, 3, 3);
        assertCurrents(changed.execution(), 0.5, 2, 2);
        assertCurrents(original.execution(), 1, 3, 3);
    }
    @ParameterizedTest
    @ValueSource(ints = {6, 8, 10})
    void forwardDropFamilyOverridesChangeTheirOwnElectricalConfiguration(int type) {
        var model = gatedSwitch(type, new double[]{type == 6 ? 1e12 : 19999, 1, 1, type == 6 ? 1e12 : 19999}, 1);
        if (type == 6) model.getControlComponents().clear();
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var drop = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("device.forwardVoltageDrop", 2.0))));
        var on = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("device.onResistance", 3.0))));
        var off = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("device.offResistance", type == 6 ? 2e12 : 39999.0))));
        assertCurrents(original.execution(), type == 6 ? 2.5 : 0.0003, 2.5, 2.5);
        assertCurrents(drop.execution(), type == 6 ? 2 : 0.0003, 2, 2);
        assertCurrents(on.execution(), type == 6 ? 1.25 : 0.0003, 1.25, 1.25);
        assertCurrents(off.execution(), type == 6 ? 2.5 : 0.00015, 2.5, 2.5);
        assertCurrents(original.execution(), type == 6 ? 2.5 : 0.0003, 2.5, 2.5);
    }

    @Test
    void diodeOffResistanceOverrideChangesTheReverseBiasLeakage() {
        var model = gatedSwitch(6, new double[]{19999, 0, 1, 19999}, 0);
        model.getControlComponents().clear();
        var source = model.getCircuitComponents().get(0);
        source.setNumericParameters(new double[]{401, -6});
        source.setParameter("param1", -6.0);
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var changed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("device.param3", 39999.0))));
        assertCurrents(original.execution(), -0.0003, -0.0003, -0.0003);
        assertCurrents(changed.execution(), -0.00015, -0.00015, -0.00015);
    }
    @ParameterizedTest
    @CsvSource({"7,kON,0.00002", "7,param6,0.00002", "7,kOFF,0.00003", "7,param7,0.00003",
            "7,numberParalleled,2", "7,param12,2", "7,uSWnorm,400",
            "6,kON,0.00002", "6,kOFF,0.00003", "6,numberParalleled,2", "6,uSWnorm,400",
            "8,kON,0.00002", "8,kOFF,0.00003", "8,numberParalleled,2", "8,uSWnorm,400",
            "10,kON,0.00002", "10,kOFF,0.00003", "10,numberParalleled,2", "10,uSWnorm,400",
            "28,kON,0.00002", "28,kOFF,0.00003", "28,numberParalleled,2", "28,uSWnorm,400",
            "28,antiParallelDiodeRon,0.01", "28,antiParallelDiodeRoff,1000000", "28,antiParallelDiodeUF,1",
            "6,useNonlinearChar,1", "8,reverserecoverydelay,0.1", "8,param9,0.1",
            "10,satCurEnabled,1", "10,param10,1", "10,saturationCurrent,20", "10,param11,20"})
    void unimplementedNumericConfigurationCannotPretendToBeAnEffectiveOverride(int type, String field, double value) {
        double[] parameters = type == 7 ? new double[]{19999, 1, 19999} : new double[]{19999, 0, 1, 19999};
        var model = gatedSwitch(type, parameters, 1);
        if (type == 6) model.getControlComponents().clear();
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        String path = "device." + field;
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of(path, value))));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item ->
                item.code().equals("UNSUPPORTED_PARAMETER") && path.equals(item.parameterPath())),
                rejected.report().diagnostics().toString());
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertCurrents(original.execution(), type == 6 ? 3 : 0.0003, 3, 3);
    }
    static String savedDevice(int type, String extra) {
        String parameters = type == 7 ? "99999 1 19999 0 77 88 0.00002 0.00003 1 0 0 0 1"
                : "99999 0 1 19999 77 88 0.00002 0.00003 1 0 0 " + (type == 10 ? "10" : "0") + " 1";
        String text = SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/1", "/0", "")
                + SourcePreparationTest.element("LK", 1, "R.1", "1", "/1", "/2", "")
                + SourcePreparationTest.element("LK", type, "device", parameters, "/2", "/0",
                        "uniqueObjectIdentifier 9007199254740993\nkON 0.00002\nkOFF 0.00003\nuSWnorm 400\n" + extra);
        if (type != 6) {
            text += SourcePreparationTest.element("CONTROL", 4, "command", "404 1 1 0 0 0.5", "/", "/gate", "");
            text += SourcePreparationTest.element("CONTROL", 6, "gate", "", "/gate", "/", "")
                    .replace("parameterString[] /\n", "parameterString[] /device\n");
        }
        return text;
    }

    static Stream<Arguments> savedDevices() {
        return Stream.of(6, 7, 8, 10, 28).flatMap(type -> Stream.of(SolverType.values())
                .map(solver -> Arguments.of(type, solver)));
    }

    @ParameterizedTest
    @MethodSource("savedDevices")
    void savedDesktopDefaultsRemainExecutableAndPreserveLossConfiguration(int type, SolverType solver) throws Exception {
        var model = SourcePreparationTest.parse(savedDevice(type, ""));
        model.setSolverType(solver);
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertCurrents(ready.execution(), type == 6 ? 3 : 0.0003, 3, 3);
        var device = ready.circuit().toModel().getCircuitComponents().get(2);
        assertEquals(9007199254740993L, device.getParameters().get("uniqueObjectIdentifier"));
        assertEquals(0.00002, device.getNumericParameters()[6]);
        assertEquals(0.00003, device.getNumericParameters()[7]);
        assertEquals(1, device.getNumericParameters()[12]);
        assertEquals("400", device.getImportTokens().get("uSWnorm"));
        assertEquals(type == 10 ? 10 : 0, device.getNumericParameters()[11]);
        assertEquals(0, device.getNumericParameters()[4]);
        assertEquals(0, device.getNumericParameters()[5]);
        assertEquals(0, device.getNumericParameters()[8]);
        int onSlot = type == 7 ? 1 : 2;
        var changed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(ready.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("device.param" + onSlot, 2.0))));
        assertCurrents(changed.execution(), type == 6 ? 2 : 0.0003, 2, 2);
        assertCurrents(ready.execution(), type == 6 ? 3 : 0.0003, 3, 3);
    }

    @ParameterizedTest
    @CsvSource({"6,numberParalleled,2", "7,numberParalleled,2", "8,numberParalleled,2",
            "10,numberParalleled,2", "28,numberParalleled,2", "8,reverserecoverydelay,0.1",
            "10,satCurEnabled,true", "6,useNonlinearChar,true"})
    void unsupportedSavedDeviceModesRemainInspectableWithTheirConfiguration(int type, String field, String value) throws Exception {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(SourcePreparationTest.parse(savedDevice(type, field + " " + value)));
        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().anyMatch(item -> item.code().equals("UNSUPPORTED_PARAMETER")
                && item.category() == Diagnostic.Category.CAPABILITY && "device".equals(item.component())
                && ("device." + field).equals(item.parameterPath())), result.report().toString());
        assertEquals(value, result.circuit().toModel().getCircuitComponents().get(2).getImportTokens().get(field));
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 10, 28})
    void deviceRuntimeAndIdentityFieldsCannotBeOverridden(int type) {
        var model = gatedSwitch(type, type == 7 ? new double[]{19999, 1, 19999}
                : new double[]{19999, 0, 1, 19999}, 1);
        if (type == 6) model.getControlComponents().clear();
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        for (String field : new String[]{"param0", "param4", "param5", "param8", "uniqueObjectIdentifier"}) {
            String path = "device." + field;
            var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                    new PreparationRequest(SolverOverrides.none(), Map.of(path, 1.0))));
            assertTrue(rejected.report().diagnostics().stream().anyMatch(item -> item.code().equals("READ_ONLY_PARAMETER")
                    && path.equals(item.parameterPath())), rejected.report().toString());
        }
    }
}
