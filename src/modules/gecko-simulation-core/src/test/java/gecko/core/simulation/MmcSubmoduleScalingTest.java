package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverType;
import gecko.core.allg.SparseSolverSettings;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.preparation.SolverFeatures;
import gecko.core.simulation.solver.MatrixSolver.Backend;
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One MMC arm of 100 half-bridge submodules (switch plus antiparallel diode, order 204, 503 elements), charged through
 * the diodes. It is a topology fixture for the solver, not a three-phase converter and not a model of MMC control or
 * balancing, so it does not prove behavior of a full MMC. The RC ladders prove sparse scaling only.
 */
class MmcSubmoduleScalingTest {
    private static final int SUBMODULES = 100;
    private static final double GRID = 1e-5;
    private static final double ADAPTIVE_TRACE_TOLERANCE = 1e-6;

    private static CircuitNetlist arm() {
        return SyntheticCircuitBuilder.mmcArm(SUBMODULES, 10.0, 1e-3, 100.0);
    }

    @Test
    void aHundredSubmoduleArmRunsOnTheSparseBackendAndMatchesDenseThroughDiodeSwitching() {
        CircuitNetlist sparseNet = arm();
        CircuitNetlist denseNet = arm();
        assertEquals(2 * SUBMODULES + 2, sparseNet.getNodeMax());
        var sparse = new ElectricalTimeStep(sparseNet, SolverType.SOLVER_BE,
                new SolverFeatures(0, false, false, null, null, new SparseSolverSettings()));
        var dense = new ElectricalTimeStep(denseNet, SolverType.SOLVER_BE);
        assertEquals(Backend.SPARSE, sparse.linearSolverReport().backend(), "AUTO selects sparse above 100 nodes");
        assertEquals(Backend.DENSE, dense.linearSolverReport().backend());

        double[] initialStates = PhysicalTrace.switchStates(denseNet);
        PhysicalTrace denseTrace = new PhysicalTrace();
        PhysicalTrace sparseTrace = new PhysicalTrace();
        int steps = 400;
        long sparseNanos = 0;
        long denseNanos = 0;
        for (int index = 0; index < steps; index++) {
            long t0 = System.nanoTime();
            sparse.advance(GRID, index * GRID, null);
            long t1 = System.nanoTime();
            dense.advance(GRID, index * GRID, null);
            long t2 = System.nanoTime();
            sparseNanos += t1 - t0;
            denseNanos += t2 - t1;
            denseTrace.add(index * GRID, denseNet.getLastNodeVoltages(), denseNet.getLastComponentCurrents(), denseNet);
            sparseTrace.add(index * GRID, sparseNet.getLastNodeVoltages(), sparseNet.getLastComponentCurrents(), sparseNet);
        }
        PhysicalTrace.assertEquivalent(denseTrace, sparseTrace, "MMC arm", 1e-9);
        int transitions = denseTrace.transitions();
        int switchedInFirstStep = 0;
        for (int i = 0; i < initialStates.length; i++) {
            if (initialStates[i] != denseTrace.switchStates.get(0)[i]) switchedInFirstStep++;
        }
        assertTrue(switchedInFirstStep > 20, "diodes must switch while the arm first charges: " + switchedInFirstStep);
        var report = sparse.linearSolverReport();
        long order = report.matrixOrder();
        assertTrue(report.storedCoefficients() < 12 * order, "stored " + report.storedCoefficients() + " for order " + order);
        assertTrue(report.factorizations() > 1, "diode state changes refactor");
        System.out.printf("MMC-100 order=%d elements=%d steps=%d switchedInFirstStep=%d laterTransitionSteps=%d sparseMs=%.1f denseMs=%.1f "
                + "sparseFactorizations=%d storedCoefficients=%d numericStorageEstimateBytes=%d (dense %d)%n",
                order, sparseNet.getElementCount(), steps, switchedInFirstStep, transitions, sparseNanos / 1e6, denseNanos / 1e6,
                report.factorizations(), report.storedCoefficients(), report.storageBytes(),
                dense.linearSolverReport().storageBytes());
    }

    @Test
    void theArmRunsWithEventLocationAndTheAdaptiveStepOnSparseCompanionSolvers() {
        var adaptive = new AdaptiveStepSettings(1e-3, 1e-8, Double.NaN);
        var features = new SolverFeatures(0, true, false, null, adaptive, new SparseSolverSettings());
        CircuitNetlist netlist = arm();
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, features);
        CircuitNetlist denseNet = arm();
        var dense = new ElectricalTimeStep(denseNet, SolverType.SOLVER_BE, new SolverFeatures(0, true, false, null, adaptive));
        PhysicalTrace denseTrace = new PhysicalTrace();
        PhysicalTrace sparseTrace = new PhysicalTrace();
        for (int index = 0; index < 20; index++) {
            step.advanceInterval(GRID, index * GRID, null);
            dense.advanceInterval(GRID, index * GRID, null);
            denseTrace.add(index * GRID, denseNet.getLastNodeVoltages(), denseNet.getLastComponentCurrents(), denseNet);
            sparseTrace.add(index * GRID, netlist.getLastNodeVoltages(), netlist.getLastComponentCurrents(), netlist);
        }
        // Event location bisects to a tolerance and the adaptive step compares error estimates with a threshold, so a
        // rounding difference between the LU variants can move an accepted sub-step. 1e-6 bounds that; 1e-9 does not.
        PhysicalTrace.assertEquivalent(denseTrace, sparseTrace, "MMC arm, events and adaptive step", ADAPTIVE_TRACE_TOLERANCE);
        var report = step.linearSolverReport();
        assertEquals(Backend.SPARSE, report.backend());
        assertEquals(Backend.SPARSE, report.companionBackend());
        long order = report.matrixOrder();
        assertTrue(report.companionStoredCoefficients() < 12 * order);
        assertTrue(step.adaptiveReport().accepted() >= 20);
    }
}
