package gecko.geckocircuits.circuit;

import gecko.geckocircuits.allg.SolverType;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.circuitcomponents.CapacitorCircuit;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import gecko.geckocircuits.circuit.circuitcomponents.ResistorCircuit;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.DataContainerGlobal;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationKernelIsolationContractTest {
    @Test
    void aFailedNumericalFinishIsNotRepeated() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var failure = new IllegalStateException("Test teardown failure");
        var kernel = new SimulationsKern(SolverType.SOLVER_BE, active -> { }) {
            @Override
            public void tearDownOnPause() {
                calls.incrementAndGet();
                throw failure;
            }
        };
        kernel.initSimulation(0.25, 0, 0, 0.5, 1, true, netlist(List.of()), false);
        kernel.initialisiereCONTROLatSimulationStart(0.25);
        var ticket = kernel.reserveExecution(() -> true);
        ticket.bindCompletion();
        ticket.run();

        assertSame(failure, assertThrows(IllegalStateException.class, ticket::finish));
        assertFalse(ticket.finish());
        assertEquals(1, calls.get());
        assertEquals(SimulationsKern.SimulationStatus.FINISHED, kernel._simulationStatus);
    }

    private enum NaNBoundary { DURATION, END, CURRENT }

    @ParameterizedTest
    @EnumSource(NaNBoundary.class)
    void aNaNUpperBoundOrClockDoesNotExecuteANumericalStep(NaNBoundary boundary) throws Exception {
        var kernel = new SimulationsKern(SolverType.SOLVER_BE, active -> { });
        kernel.initSimulation(1, 0, boundary == NaNBoundary.CURRENT ? Double.NaN : 0,
                1, 2, true, dischargeNetlist(), false);
        kernel.initialisiereCONTROLatSimulationStart(1);
        if (boundary == NaNBoundary.END) {
            kernel.setZeiten(0, Double.NaN, 1);
        }
        Path trace = Path.of(System.getProperty("gecko.test.auditDirectory", System.getProperty("java.io.tmpdir")),
                "gecko-nan-contract-" + java.util.UUID.randomUUID() + ".csv");
        try (var logger = new SimulationTraceLogger(trace.toFile()) {
            @Override
            public void logStep(double time, LKMatrices matrices, NetListLK netlist) {
                throw new AssertionError("A NaN bound must not execute a numerical step");
            }
        }) {
            kernel.setTraceLogger(logger);
            if (boundary == NaNBoundary.DURATION) {
                kernel.simulateTime(Double.NaN);
            } else {
                kernel.runSimulation();
            }
        }
        if (boundary == NaNBoundary.CURRENT) {
            assertTrue(Double.isNaN(kernel.getZeitAktuell()));
        } else {
            assertEquals(0.0, kernel.getZeitAktuell());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aCapturedStatusWriterCannotChangeAReplacementRecording(boolean discardFirst) {
        var data = new DataContainerGlobal();
        data.init(1, new String[]{"first"}, "time");
        var first = data.captureStatusUpdater();
        data.setContainerStatus(ContainerStatus.RUNNING);
        if (discardFirst) {
            data.clear();
        }
        data.init(1, new String[]{"replacement"}, "time");
        data.setContainerStatus(ContainerStatus.RUNNING);

        first.accept(ContainerStatus.PAUSED);

        assertEquals(ContainerStatus.RUNNING, data.getContainerStatus());
        assertEquals("replacement", data.getSignalName(0));
    }

    @Test
    void anEmptyRunUsesExplicitConfigurationAndReportsPresentationActivity() {
        List<Boolean> activity = new ArrayList<>();
        SimulationsKern kernel = new SimulationsKern(SolverType.SOLVER_TRZ, activity::add);
        var netlist = netlist(List.of());

        kernel.initSimulation(0.25, 0, 0, 0.5, 1, true, netlist, false);
        kernel.initialisiereCONTROLatSimulationStart(0.25);
        kernel.setScopeMenuesStartStop();
        kernel.runSimulation();

        assertEquals(0.75, kernel.getZeitAktuell());
        assertEquals(List.of(true, false), activity);
    }

    @Test
    void aTimedPauseReportsInactiveAfterTheCommittedStep() {
        List<Boolean> activity = new ArrayList<>();
        SimulationsKern kernel = new SimulationsKern(SolverType.SOLVER_BE, activity::add);
        var netlist = netlist(List.of());

        kernel.initSimulation(0.25, 0, 0, 1, 0.25, true, netlist, false);
        kernel.initialisiereCONTROLatSimulationStart(0.25);
        kernel.setScopeMenuesStartStop();
        kernel.runSimulation();

        assertEquals(0.5, kernel.getZeitAktuell());
        assertEquals(gecko.geckocircuits.api.ISimulationEngine.SimulationStatus.PAUSED, kernel.getSimulationStatus());
        assertEquals(List.of(true, false), activity);
    }

    @Test
    void electricalRunsUseEachKernelsSolverConfiguration() throws Exception {
        var backwardEuler = new SimulationsKern(SolverType.SOLVER_BE, active -> { });
        var trapezoidal = new SimulationsKern(SolverType.SOLVER_TRZ, active -> { });
        // R = C = dt = 1. Legacy initialization starts capacitor current history at zero.
        // The first BE sample is 10 / 2. The first TRZ sample is 20 / 3.
        assertEquals(5.0, dischargeAfterOneStep(backwardEuler), 1e-9);
        assertEquals(20.0 / 3, dischargeAfterOneStep(trapezoidal), 1e-9);
    }

    @Test
    void legacyKernelReadsTheSelectedSolverAtInitialization() throws Exception {
        SolverType previous = MainWindow._solverSettings.SOLVER_TYPE.getValue();
        try {
            MainWindow._solverSettings.SOLVER_TYPE.setValueWithoutUndo(SolverType.SOLVER_BE);
            var kernel = new SimulationsKern();
            MainWindow._solverSettings.SOLVER_TYPE.setValueWithoutUndo(SolverType.SOLVER_TRZ);
            assertEquals(20.0 / 3, dischargeAfterOneStep(kernel), 1e-9);
        } finally {
            MainWindow._solverSettings.SOLVER_TYPE.setValueWithoutUndo(previous);
        }
    }

    @Test
    void completionPublishesContinuationHistoryBeforeReportingPresentationFailure() throws Exception {
        var owner = new AtomicReference<SimulationsKern>();
        var historyAvailable = new AtomicBoolean();
        var presentationError = new IllegalStateException("Test presentation failure");
        var kernel = new SimulationsKern(SolverType.SOLVER_BE, active -> {
            if (!active) {
                owner.get().setInitialConditionsFromContinue();
                historyAvailable.set(true);
            }
            throw presentationError;
        });
        owner.set(kernel);
        var netlist = dischargeNetlist();
        kernel.initSimulation(1, 0, 0, 0, 2, true, netlist, false);
        kernel.initialisiereCONTROLatSimulationStart(1);
        List<Throwable> reports = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord report) { reports.add(report.getThrown()); }
            @Override
            public void flush() { }
            @Override
            public void close() { }
        };
        Logger logger = Logger.getLogger(SimulationsKern.class.getName());
        logger.addHandler(handler);
        try {
            kernel.setScopeMenuesStartStop();
            kernel.runSimulation();
        } finally {
            logger.removeHandler(handler);
        }

        assertTrue(historyAvailable.get());
        assertEquals(List.of(presentationError, presentationError), reports);
        kernel.initSimulation(1, 0, kernel.getZeitAktuell(), 2, 3, false, netlist, false);
        kernel.setInitialConditionsFromContinue();
        assertEquals(2.5, recordNextVoltage(kernel), 1e-9);
    }

    private static NetListContainer dischargeNetlist() throws Exception {
        var resistor = (ResistorCircuit) CircuitTyp.LK_R.getTypeInfo().fabric();
        resistor.setAccessibleParameter("R", 1);
        var capacitor = (CapacitorCircuit) CircuitTyp.LK_C.getTypeInfo().fabric();
        capacitor.setAccessibleParameter("C", 1);
        capacitor.setAccessibleParameter("uC(0)", 10);
        return netlist(List.of(resistor, capacitor));
    }

    private static NetListContainer netlist(List<? extends AbstractBlockInterface> elements) {
        var electrical = NetListLK.fabricIncludingSubcircuits(Set.of(), elements);
        var thermal = NetListLK.fabricIncludingSubcircuits(Set.of(), List.of());
        var control = NetzlisteCONTROL.fabricRunSimulation(
                NetzlisteAllg.fabricNetzlistComplete(List.of(), List.of()));
        return NetListContainer.fabricGuiUpdate(electrical, thermal, control);
    }

    private static double dischargeAfterOneStep(SimulationsKern kernel) throws Exception {
        kernel.initSimulation(1, 0, 0, 1, 2, true, dischargeNetlist(), false);
        kernel.initialisiereCONTROLatSimulationStart(1);
        return recordNextVoltage(kernel);
    }

    private static double recordNextVoltage(SimulationsKern kernel) throws Exception {
        Path trace = Path.of(System.getProperty("gecko.test.auditDirectory", System.getProperty("java.io.tmpdir")),
                "gecko-kernel-discharge-" + java.util.UUID.randomUUID() + ".csv");
        try (var logger = new SimulationTraceLogger(trace.toFile())) {
            kernel.setTraceLogger(logger);
            kernel.simulateOneStep();
        }
        String[] row = Files.readAllLines(trace).get(1).split(",");
        return Math.abs(Double.parseDouble(row[2]) - Double.parseDouble(row[1]));
    }
}
