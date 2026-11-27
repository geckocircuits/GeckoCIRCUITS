package gecko.core.simulation;

import gecko.core.ac.AcComponent;
import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverType;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NativeMosfetTransientTest {
    private static final String DECK = """
            * Source-referenced drain and gate drives
            Vs s 0 3
            Vd d s %s
            Vg g s %s
            M1 d g s s local W=20u L=10u
            .model local %s(Level=1 Vto=%s Kp=2m Lambda=.04 Is=10f)
            .tran 100n 1u
            """;

    @Test
    void keepsGateTerminalAndDrainCurrentAcrossSaveAndReopen() throws Exception {
        assertDrainCurrent(DECK.formatted("5", "2", "NMOS", "1"), SolverType.SOLVER_BE, .0024);
        assertDrainCurrent(DECK.formatted("-5", "-2", "PMOS", "-1"), SolverType.SOLVER_TRZ, -.0024);
    }

    @Test
    void usesTheTriodeAndCutoffLaws() throws Exception {
        assertDrainCurrent(DECK.formatted(".5", "2", "NMOS", "1"), SolverType.SOLVER_BE, .00153);
        assertDrainCurrent(DECK.formatted("5", ".5", "NMOS", "1"), SolverType.SOLVER_TRZ, 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void movingGateAndSourceChangeTheResistorLoadedDrainVoltage(boolean adaptive) throws Exception {
        String deck = DECK.formatted("5", "2", "NMOS", "1")
                .replace("Vs s 0 3", "Vs s 0 PWL(0 0 100n 3 200n 2)")
                .replace("Vg g s 2", "Vg g s PWL(0 .5 100n 2 200n 1.5)")
                .replace("Vd d s 5", "Vd supply s 5\nRload supply d 500");
        var features = adaptive ? new SolverFeatures(0, false, false, null, new AdaptiveStepSettings(1e-3))
                : SolverFeatures.none();
        var ready = prepare(deck, adaptive ? SolverType.SOLVER_TRZ : SolverType.SOLVER_BE, features);
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            session.step((time, voltage, current, netlist) ->
                    assertEquals(0, current[netlist.getElementIndexByName("M1")], 1e-10));
            session.step((time, voltage, current, netlist) -> {
                int mos = netlist.getElementIndexByName("M1");
                double expected = .0024 / 1.04;
                assertEquals(expected, current[mos], 1e-10);
                assertEquals(5 - 500 * expected, voltage[netlist.getNodeX(mos)] - voltage[netlist.getNodeY(mos)], 1e-8);
            });
            if (adaptive) {
                assertTrue(session.adaptiveStepReport().accepted() > 0);
            }
        }
    }

    @Test
    void sourceTiedBodyDiodeConductsWithTheGateOff() throws Exception {
        double thermalVoltage = 1.380649e-23 * 300.15 / 1.602176634e-19;
        double current = 1e-14 * Math.expm1(.55 / thermalVoltage) + .55e-12;
        assertDrainCurrent(DECK.formatted("-.55", "-2", "NMOS", "1"), SolverType.SOLVER_TRZ, -current);
        assertDrainCurrent(DECK.formatted(".55", "2", "PMOS", "-1"), SolverType.SOLVER_BE, current);
    }

    @Test
    void acceptsReferenceAsTheOnlyTerminalInTheGateGroup() throws Exception {
        String deck = DECK.formatted("5", "2", "NMOS", "1")
                .replace("Vs s 0 3\n", "")
                .replace("Vg g s 2\n", "")
                .replace("M1 d g s s", "M1 d 0 s s");
        var imported = new SpiceImporter(false, false, true).convert(deck, "grounded-gate.cir");
        assertTrue(imported.getImportIssues().isEmpty(), imported.getImportIssues().toString());
        var netlist = NetlistBuilder.buildFromCircuitModel(imported);
        assertEquals(0, netlist.getMosfetGateNode(netlist.getElementIndexByName("M1")));
    }

    @Test
    void rejectsOptOutUnsupportedBulkAndVendorModels() {
        String circuit = DECK.formatted("5", "2", "NMOS", "1");
        assertFalse(new SpiceImporter().convert(circuit, "mos.cir").getImportIssues().isEmpty());
        var importer = new SpiceImporter(false, false, true);
        for (String rejected : java.util.List.of(circuit.replace("g s s local", "g s 0 local"),
                circuit.replace("NMOS", "VDMOS"), circuit.replace("Level=1", "Level=3"))) {
            assertTrue(importer.convert(rejected, "mos.cir").getImportIssues().stream()
                    .anyMatch(issue -> issue.component().equals("M1")));
        }
    }

    @Test
    void timestepRejectsUnsupportedSolverAndInitialConditionsWithAMosfet() throws Exception {
        var model = prepare(DECK.formatted("5", "2", "NMOS", "1"), SolverType.SOLVER_BE)
                .execution().newRunModel();
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        var devices = TransientSemiconductors.from(model, netlist);
        assertNotNull(devices);
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricalTimeStep(netlist, SolverType.SOLVER_GS, SolverFeatures.none(), false, devices));
        assertThrows(IllegalArgumentException.class, () -> new ElectricalTimeStep(netlist, SolverType.SOLVER_BE,
                new SolverFeatures(0, false, true), false, devices));
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, SolverFeatures.none(), true, devices));
    }

    private static void assertDrainCurrent(String deck, SolverType type, double expected) throws Exception {
        var ready = prepare(deck, type);
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            session.step((time, voltage, current, netlist) -> {
                int mos = netlist.getElementIndexByName("M1");
                assertEquals(netlist.getNodeX(netlist.getElementIndexByName("Vg")), netlist.getMosfetGateNode(mos));
                assertEquals(expected, current[mos], 1e-10);
            });
        }
    }

    private static PreparationResult.Ready prepare(String deck, SolverType type) throws Exception {
        return prepare(deck, type, SolverFeatures.none());
    }

    private static PreparationResult.Ready prepare(String deck, SolverType type, SolverFeatures features) throws Exception {
        var imported = new SpiceImporter(false, false, true).convert(deck, "mos.cir");
        assertTrue(imported.getImportIssues().isEmpty(), imported.getImportIssues().toString());
        var model = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(imported)), "mos.ipes");
        var law = model.getCircuitComponents().getLast().getAcComponent().law();
        assertInstanceOf(AcComponent.Mosfet.class, law);
        var preparation = new CircuitPreparation();
        var request = new PreparationRequest(new SolverOverrides(type, null, null, features), Map.of());
        var result = preparation.prepare(preparation.capture(model), request);
        return assertInstanceOf(PreparationResult.Ready.class, result, result.report().toString());
    }
}
