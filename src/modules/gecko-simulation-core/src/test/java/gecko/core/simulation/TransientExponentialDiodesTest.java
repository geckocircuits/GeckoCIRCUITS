package gecko.core.simulation;

import gecko.core.ac.AcComponent;
import gecko.core.allg.SolverType;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
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

import static org.junit.jupiter.api.Assertions.*;

class TransientExponentialDiodesTest {
    private static final double STEP = 1e-8;
    private static final double VT = 1.380649e-23 * 300.15 / 1.602176634e-19;
    private static final String DECK = """
            * Driven forward diode
            V1 anode 0 .6
            D1 anode 0 dtest
            .model dtest D(Is=1e-14 Tt=20n)
            .tran 10n 1u
            """;

    @Test
    void savedModelRunsWithExplicitSelectionAndRejectsIncompatibleStartModes() throws Exception {
        var legacy = new SpiceImporter().convert(DECK, "legacy-diode.cir");
        assertNull(legacy.getCircuitComponents().get(1).getAcComponent());
        var model = selected(DECK);
        var saved = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "diode.ipes");
        var diode = assertInstanceOf(AcComponent.ExponentialDiode.class, saved.getCircuitComponents().get(1).getAcComponent().law());
        assertTrue(diode.transientLaw());
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(saved), PreparationRequest.defaults()));
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            assertNotNull(session.step());
        }
        var dc = new PreparationRequest(new SolverOverrides(null, null, null, new SolverFeatures(0, false, true)), Map.of());
        assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(preparation.capture(saved), dc));
    }

    @Test
    void backwardEulerPublishesConductionAndDiffusionCurrent() throws Exception {
        var model = executionModel(DECK);
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, SolverFeatures.none(), false,
                TransientSemiconductors.from(model, netlist));
        int diode = netlist.getElementIndexByName("D1");
        double conduction = 1e-14 * Math.expm1(.6 / VT);
        step.advance(STEP, 0, null);
        assertEquals(3 * conduction + .6e-12, step.solverForTest().getIALT()[diode], 1e-11);
        step.advance(STEP, STEP, null);
        assertEquals(conduction + .6e-12, step.solverForTest().getIALT()[diode], 1e-11);
    }

    @Test
    void discardingATrialKeepsTheSameChargeHistoryAsAnUntouchedTwin() throws Exception {
        var model = executionModel(DECK.replace(".6", "PWL(0 0 100n .6)").replace("Tt=20n", "Rs=1k Tt=20n"));
        var twinModel = executionModel(DECK.replace(".6", "PWL(0 0 100n .6)").replace("Tt=20n", "Rs=1k Tt=20n"));
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        var twinNetlist = NetlistBuilder.buildFromCircuitModel(twinModel);
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, new SolverFeatures(0, true), false,
                TransientSemiconductors.from(model, netlist));
        var twin = new ElectricalTimeStep(twinNetlist, SolverType.SOLVER_TRZ, new SolverFeatures(0, true), false,
                TransientSemiconductors.from(twinModel, twinNetlist));
        step.subStepForTest(STEP, 50e-9, -1);
        twin.subStepForTest(STEP, 50e-9, -1);
        step.trialAndRestoreForTest(3 * STEP, 80e-9);
        step.subStepForTest(STEP, 60e-9, -1);
        twin.subStepForTest(STEP, 60e-9, -1);
        assertArrayEquals(twin.solverForTest().getIALT(), step.solverForTest().getIALT(), 1e-15);
        assertEquals(0, step.eventReport().located());
    }

    @Test
    void convergenceChecksNodesOutsideTheDiodeBranch() throws Exception {
        var model = executionModel(DECK.replace("V1 anode 0 .6", "V1 anode 0 0\nI1 0 sense .5p\nR1 sense 0 1T"));
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, SolverFeatures.none(), false,
                TransientSemiconductors.from(model, netlist));
        step.advance(STEP, 0, null);
        int resistor = netlist.getElementIndexByName("R1");
        double[] voltages = netlist.getLastNodeVoltagesRef();
        assertEquals(.5, voltages[netlist.getNodeX(resistor)] - voltages[netlist.getNodeY(resistor)], 1e-6);
    }

    @Test
    void adaptiveEstimatesDiffusionCurrentEvenWhenTheJunctionVoltageIsForced() throws Exception {
        var model = executionModel(DECK.replace(".6", "PWL(0 0 100n .55 160n .62)"));
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        var features = new SolverFeatures(0, false, false, null,
                new gecko.core.allg.AdaptiveStepSettings(1e-3));
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ, features, false,
                TransientSemiconductors.from(model, netlist));
        for (int grid = 0; grid <= 16; grid++) step.advanceAdaptiveForTest(STEP, grid * STEP);
        assertTrue(step.adaptiveReport().rejected() > 0);
    }

    @Test
    void sparseResidualUsesTheSamePhysicalSolutionAsDense() throws Exception {
        var denseModel = executionModel(DECK);
        var sparseModel = executionModel(DECK);
        var dense = NetlistBuilder.buildFromCircuitModel(denseModel);
        var sparse = NetlistBuilder.buildFromCircuitModel(sparseModel);
        var sparseSettings = new gecko.core.allg.SparseSolverSettings(gecko.core.allg.SparseSolverSettings.Mode.SPARSE);
        var features = new SolverFeatures(0, false, false, null, null, sparseSettings);
        var reference = new ElectricalTimeStep(dense, SolverType.SOLVER_TRZ, SolverFeatures.none(), false,
                TransientSemiconductors.from(denseModel, dense));
        var candidate = new ElectricalTimeStep(sparse, SolverType.SOLVER_TRZ, features, false,
                TransientSemiconductors.from(sparseModel, sparse));
        for (int i = 0; i < 4; i++) {
            reference.advance(STEP, i * STEP, null);
            candidate.advance(STEP, i * STEP, null);
            assertArrayEquals(reference.solverForTest().getIALT(), candidate.solverForTest().getIALT(), 1e-13);
        }
        assertEquals(gecko.core.simulation.solver.MatrixSolver.Backend.SPARSE, candidate.solverForTest().getBackend());
    }

    private static CircuitModel executionModel(String deck) throws Exception {
        var model = selected(deck);
        var saved = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "diode.ipes");
        var preparation = new CircuitPreparation();
        return assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(saved),
                PreparationRequest.defaults())).execution().newRunModel();
    }

    private static CircuitModel selected(String deck) {
        var model = new SpiceImporter(true).convert(deck, "diode.cir");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        return model;
    }
}
