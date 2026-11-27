package gecko.geckocircuits.allg;

import gecko.GeckoSim;
import gecko.geckoscript.SimulationAccess;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.circuit.SimulationTraceLogger;
import gecko.geckocircuits.circuit.SolverSettings;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.circuit.SimulationsKern;
import gecko.geckocircuits.circuit.LKMatrices;
import gecko.geckocircuits.circuit.NetListLK;
import gecko.geckocircuits.circuit.circuitcomponents.CapacitorCircuit;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import gecko.geckocircuits.circuit.circuitcomponents.ResistorCircuit;
import gecko.geckocircuits.control.ControlTyp;
import gecko.geckocircuits.control.ReglerCISPR16;
import gecko.geckocircuits.control.ReglerConstant;
import gecko.geckocircuits.control.RegelBlock;
import gecko.geckocircuits.control.calculators.ConstantCalculator;
import gecko.geckocircuits.control.ReglerOSZI;
import gecko.geckocircuits.control.TestReceiverWindow;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import gecko.i18n.DoubleMap;
import gecko.i18n.LangInit;
import gecko.i18n.SelectableLanguages;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JMenu;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.JDialog;
import javax.swing.Timer;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SimulationRunnerPresentationContractTest {
    private MainWindow window;
    private SchematicEditor2 editor;
    private ReglerOSZI scope;
    private ReglerCISPR16 receiver;
    private TestReceiverWindow receiverWindow;
    private List<Window> previousWindows;
    private List<Window> ownedWindows = List.of();
    private Properties previousProperties;
    private String previousBackend;
    private boolean previousBranded;
    private boolean previousTesting;
    private String previousFilename;
    private SchematicEditor2 previousEditor;
    private SchematicEditor2 previousSingleton;
    private SolverType previousSolver;
    private double previousDt;
    private double previousDuration;
    private double previousPause;
    private double previousPreTime;
    private boolean previousPreMode;
    private URL previousPictures;
    private SelectableLanguages previousLanguage;
    private DoubleMap[] previousTranslations;
    private SimulationAccess previousScripter;
    private GeckoFileManager previousFileManager;
    private double previousDtAlt;
    private double previousPreDt;

    @BeforeEach
    void openDesktopFixture() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Desktop presentation requires a display");
        previousWindows = Arrays.asList(Window.getWindows());
        previousBackend = System.getProperty("gecko.backend");
        System.setProperty("gecko.backend", "legacy");
        previousProperties = GeckoSim.applicationProps;
        previousBranded = MainWindow.IS_BRANDED;
        previousTesting = GeckoSim._isTestingMode;
        previousFilename = GlobalFilePathes.DATNAM;
        previousEditor = MainWindow._se;
        previousSingleton = SchematicEditor2.Singleton;
        previousScripter = MainWindow._scripter;
        previousFileManager = MainWindow._fileManager;
        previousPictures = GlobalFilePathes.PFAD_PICS_URL;
        previousLanguage = LangInit.language;
        previousTranslations = new DoubleMap[]{LangInit.englishMap_single, LangInit.englishMap_multiple,
            LangInit.transMap_single, LangInit.transMap_multiple};
        var settings = MainWindow._solverSettings;
        previousSolver = settings.SOLVER_TYPE.getValue();
        previousDt = settings.dt.getValue();
        previousDuration = settings._tDURATION.getValue();
        previousPause = settings._tPAUSE.getValue();
        previousPreTime = settings._T_pre.getValue();
        previousPreMode = settings.inPreCalculationMode;
        previousDtAlt = settings._dt_ALT;
        previousPreDt = settings._dt_pre.getValue();
        LangInit.initEnglish();
        GlobalFilePathes.PFAD_PICS_URL = GetJarPath.getPathPICS();
        GeckoSim.applicationProps = new Properties();
        try (var defaults = GeckoSim.class.getResourceAsStream("/defaultProperties.prp")) {
            GeckoSim.applicationProps.load(defaults);
        }
        GeckoSim.applicationProps.setProperty("UPDATE_FREQ", "NEVER");
        MainWindow.IS_BRANDED = true;
        GeckoSim._isTestingMode = true;
        settings.SOLVER_TYPE.setValueWithoutUndo(SolverType.SOLVER_BE);
        settings.dt.setValueWithoutUndo(1.0);
        settings._tDURATION.setValueWithoutUndo(1.0);
        settings._tPAUSE.setValueWithoutUndo(-1.0);
        settings._T_pre.setValueWithoutUndo(-1.0);
        settings.inPreCalculationMode = false;
        try {
            SwingUtilities.invokeAndWait(() -> {
            window = new MainWindow();
            window.setSimulationMenu();
            editor = MainWindow._se;
            try {
                var resistor = (ResistorCircuit) CircuitTyp.LK_R.getTypeInfo().fabric();
                resistor.setAccessibleParameter("R", 1);
                var capacitor = (CapacitorCircuit) CircuitTyp.LK_C.getTypeInfo().fabric();
                capacitor.setAccessibleParameter("C", 1);
                capacitor.setAccessibleParameter("uC(0)", 10);
                add(resistor, 0);
                add(capacitor, 0);
                var constant = (ReglerConstant) ControlTyp.C_CONST.getTypeInfo().fabric();
                scope = (ReglerOSZI) ControlTyp.C_SCOPE.getTypeInfo().fabric();
                scope.setInputTerminalNumber(1);
                List<Window> beforeReceiver = Arrays.asList(Window.getWindows());
                receiver = (ReglerCISPR16) ControlTyp.C_CISPR16.getTypeInfo().fabric();
                receiverWindow = Arrays.stream(Window.getWindows())
                        .filter(candidate -> !beforeReceiver.contains(candidate))
                        .filter(TestReceiverWindow.class::isInstance)
                        .map(TestReceiverWindow.class::cast).findFirst().orElseThrow();
                add(constant, 10);
                add(scope, 20);
                add(receiver, 30);
                constant.YOUT.get(0).getLabelObject().setLabelWithoutUndo("sample");
                scope.XIN.get(0).getLabelObject().setLabelWithoutUndo("sample");
                receiver.XIN.get(0).getLabelObject().setLabelWithoutUndo("sample");
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
            });
        } finally {
            ownedWindows = Arrays.stream(Window.getWindows()).filter(candidate -> !previousWindows.contains(candidate)).toList();
        }
    }

    private void add(AbstractBlockInterface block, int x) {
        block.setParentCircuitSheet(editor._circuitSheet);
        block.setPositionWithoutUndo(x, 0);
        block.doOperationAfterNewConstruction();
    }

    @AfterEach
    void closeDesktopFixture() throws Exception {
        if (previousWindows == null) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (Window candidate : ownedWindows) {
                    candidate.dispose();
                }
            });
        } finally {
            restoreDesktopState();
        }
    }

    private void restoreDesktopState() {
        GeckoSim.applicationProps = previousProperties;
        if (previousBackend == null) System.clearProperty("gecko.backend");
        else System.setProperty("gecko.backend", previousBackend);
        MainWindow.IS_BRANDED = previousBranded;
        GeckoSim._isTestingMode = previousTesting;
        GlobalFilePathes.DATNAM = previousFilename;
        MainWindow._se = previousEditor;
        SchematicEditor2.Singleton = previousSingleton;
        MainWindow._scripter = previousScripter;
        MainWindow._fileManager = previousFileManager;
        GlobalFilePathes.PFAD_PICS_URL = previousPictures;
        LangInit.language = previousLanguage;
        LangInit.englishMap_single = previousTranslations[0];
        LangInit.englishMap_multiple = previousTranslations[1];
        LangInit.transMap_single = previousTranslations[2];
        LangInit.transMap_multiple = previousTranslations[3];
        var settings = MainWindow._solverSettings;
        settings.SOLVER_TYPE.setValueWithoutUndo(previousSolver);
        settings.dt.setValueWithoutUndo(previousDt);
        settings._tDURATION.setValueWithoutUndo(previousDuration);
        settings._tPAUSE.setValueWithoutUndo(previousPause);
        settings._T_pre.setValueWithoutUndo(previousPreTime);
        settings.inPreCalculationMode = previousPreMode;
        settings._dt_ALT = previousDtAlt;
        settings._dt_pre.setValueWithoutUndo(previousPreDt);
    }

    @Test
    void legacyScriptRetainsOnlySamplesAtOrAfterTheAbsoluteRecordingStart() throws Exception {
        var settings = MainWindow._solverSettings;
        double previousOutputStart = settings.outputStartTime.getValue();
        try {
            settings.outputStartTime.setValueWithoutUndo(0.5);
            var runner = window._simRunner;
            runner.initSim(0.25, 1);
            var kernel = runner.simKern;
            kernel.setScopeMenuesStartStop();
            kernel.simulateOneStep();
            kernel.simulateOneStep();
            var data = NetzlisteCONTROL.globalData;
            assertEquals(-1, data.getMaximumTimeIndex(0));
            kernel.simulateOneStep();
            assertEquals(0, data.getMaximumTimeIndex(0));
            assertEquals(0.5, data.getTimeValue(0, 0));
            assertEquals(0.75, kernel.getZeitAktuell());
        } finally {
            settings.outputStartTime.setValueWithoutUndo(previousOutputStart);
        }
    }

    @Test
    void repeatedInitializationRetainsOnlyTheCurrentKernelOwner() throws Exception {
        for (int run = 0; run < 4; run++) {
            window._simRunner.initSim(1, 1);
        }

        assertEquals(1, retainedKernelOwners(window._simRunner));
    }

    @Test
    void aFirstInvalidStartReportsAbortWithoutDereferencingAnUnpublishedKernel() throws Exception {
        editor.getElementLK().get(0).getRegisteredParameters().get(0)
                .setNameOpt("missing_parameter_" + java.util.UUID.randomUUID());
        var dialogClosed = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            Timer dismiss = new Timer(25, event -> {
                for (Window candidate : Window.getWindows()) {
                    if (candidate instanceof JDialog dialog && dialog.isShowing()
                            && dialog.getOwner() == window && "Error".equals(dialog.getTitle())) {
                        dialogClosed.set(true);
                        dialog.dispose();
                    }
                }
            });
            dismiss.start();
            try {
                window.initStartWithErrorDialogMessage();
            } finally {
                dismiss.stop();
            }
        });
        assertTrue(dialogClosed.get());
        assertNull(window._simRunner.simKern);
        assertFalse(window.isSimulationRunning());
        assertEquals("Simulation aborted.", statusLabel().getText());
    }

    @Test
    void aControlFailureRemainsPrimaryWhenTeardownAlsoFails() throws Exception {
        var primary = new ArithmeticException("Test calculation failure");
        var secondary = new IllegalStateException("Test teardown failure");
        var teardownCalls = new AtomicInteger();
        TestControl extension = replaceConstant();
        var calculator = new AbstractControlCalculatable(0, 1) {
            @Override
            public void berechneYOUT(double dt) { throw primary; }
            @Override
            public void tearDownOnPause() {
                teardownCalls.incrementAndGet();
                throw secondary;
            }
        };
        extension.calculator = () -> calculator;
        var settings = new SolverSettings();
        settings.dt.setValueWithoutUndo(1.0);
        settings._tDURATION.setValueWithoutUndo(1.0);

        RuntimeException reported = assertThrows(RuntimeException.class,
                () -> window._simRunner.startCalculation(false, settings));

        assertSame(primary, reported.getCause());
        assertEquals(List.of(secondary), List.of(primary.getSuppressed()));
        assertEquals(1, teardownCalls.get());
        assertEquals(SimulationsKern.SimulationStatus.FINISHED, window._simRunner.simKern._simulationStatus);
        assertEquals("Simulation aborted.", statusLabel().getText());
    }

    @Test
    void concurrentContinuationCannotObserveAPartialNumericalPreparation() throws Exception {
        TestControl input = replaceConstant();
        var runner = new SimulationRunner(window, editor);
        var holdNext = new AtomicBoolean();
        var reached = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        input.validate = () -> {
            if (holdNext.compareAndSet(true, false)) {
                reached.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for preparation release");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }
        };
        var baseline = new SolverSettings();
        baseline.dt.setValueWithoutUndo(0.25);
        baseline._tDURATION.setValueWithoutUndo(1.0);
        runner.startCalculation(false, baseline);
        var first = new SolverSettings();
        first.dt.setValueWithoutUndo(0.125);
        first._tDURATION.setValueWithoutUndo(1.0);
        var second = new SolverSettings();
        second.dt.setValueWithoutUndo(0.0625);
        second._tDURATION.setValueWithoutUndo(1.0);
        holdNext.set(true);
        Path trace = newTrace();
        try (var logger = new SimulationTraceLogger(trace.toFile())) {
            runner.simKern.setTraceLogger(logger);
            var preparing = CompletableFuture.runAsync(() -> {
                try { runner.continueCalculation(false, first); }
                catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
            });
            assertTrue(reached.await(5, TimeUnit.SECONDS));
            var entered = new CountDownLatch(1);
            var following = CompletableFuture.runAsync(() -> {
                entered.countDown();
                try { runner.continueCalculation(false, second); }
                catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
            });
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> following.get(5, TimeUnit.SECONDS));
                assertEquals(1.25, runner.simKern.getZeitAktuell());
            } finally {
                release.countDown();
                preparing.get(10, TimeUnit.SECONDS);
                following.get(10, TimeUnit.SECONDS);
            }
        }
        // Five steps at dt=0.25 leave 3.2768 V. The next dt=0.125 step uses 8/9 of that voltage.
        assertEquals(3.2768 * 8 / 9, firstVoltage(trace), 1e-9);
    }

    private JLabel statusLabel() {
        return Arrays.stream(window.jtfStatus.getComponents()).filter(JLabel.class::isInstance)
                .map(JLabel.class::cast).findFirst().orElseThrow();
    }

    private TestControl replaceConstant() {
        RegelBlock original = editor.getElementCONTROL().stream().filter(ReglerConstant.class::isInstance)
                .findFirst().orElseThrow();
        editor._circuitSheet.allElements.remove(original);
        var extension = new TestControl();
        add(extension, 10);
        extension.YOUT.get(0).getLabelObject().setLabelWithoutUndo("sample");
        return extension;
    }

    private static final class TestControl extends RegelBlock {
        private Runnable validate = () -> { };
        private java.util.function.Supplier<AbstractControlCalculatable> calculator = () -> new ConstantCalculator(1);

        private TestControl() { super(0, 1); setTyp(ControlTyp.C_CONST); }
        @Override
        public void checkNameOptParameter() { validate.run(); super.checkNameOptParameter(); }
        @Override
        public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() { return calculator.get(); }
        @Override
        public gecko.geckocircuits.circuit.AbstractTypeInfo getTypeInfo() { return ReglerConstant.tinfo; }
        @Override
        public String[] getOutputNames() { return new String[]{"c"}; }
        @Override
        public gecko.i18n.resources.I18nKeys[] getOutputDescription() {
            return new gecko.i18n.resources.I18nKeys[]{gecko.i18n.resources.I18nKeys.CONSTANT_VALUE};
        }
        @Override
        protected Window openDialogWindow() { return null; }
    }

    private static int retainedKernelOwners(SimulationRunner runner) throws Exception {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<SimulationsKern> kernels = Collections.newSetFromMap(new IdentityHashMap<>());
        var pending = new ArrayDeque<Object>();
        pending.add(runner);
        List<String> ownershipModules = List.of(SimulationRunner.class.getName(),
                LegacySimulationFrontend.class.getName(), LatestOwnedPresentation.class.getName(),
                SimulationsKern.class.getName());
        while (!pending.isEmpty()) {
            Object owner = pending.removeFirst();
            if (!visited.add(owner)) {
                continue;
            }
            if (owner instanceof SimulationsKern kernel) {
                kernels.add(kernel);
            }
            if (owner instanceof Iterable<?> values) {
                values.forEach(value -> { if (value != null) pending.add(value); });
            } else if (owner instanceof Object[] values) {
                Arrays.stream(values).filter(java.util.Objects::nonNull).forEach(pending::add);
            } else if (ownershipModules.stream().anyMatch(owner.getClass().getName()::startsWith)) {
                for (var field : owner.getClass().getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        Object value = field.get(owner);
                        if (value != null) {
                            pending.add(value);
                        }
                    }
                }
            }
        }
        return kernels.size();
    }

    @Test
    void startUsesSuppliedSolverAndFinalizesPresentationOnTheEventThread() throws Exception {
        List<Boolean> menuStates = new ArrayList<>();
        List<Boolean> eventThreads = new ArrayList<>();
        JMenu dataMenu = scope._scopeFrame.getJMenuBar().getMenu(0);
        dataMenu.addPropertyChangeListener("enabled", event -> {
            menuStates.add((Boolean) event.getNewValue());
            eventThreads.add(SwingUtilities.isEventDispatchThread());
        });
        var supplied = new SolverSettings();
        supplied.SOLVER_TYPE.setValueWithoutUndo(SolverType.SOLVER_TRZ);
        supplied.dt.setValueWithoutUndo(1.0);
        supplied._tDURATION.setValueWithoutUndo(1.0);
        Path trace = newTrace();
        try (var logger = new SimulationTraceLogger(trace.toFile())) {
            window._simRunner.setPendingTraceLogger(logger);
            window._simRunner.startCalculation(false, supplied);
        }

        assertEquals(20.0 / 3, firstVoltage(trace), 1e-9);
        assertEquals(List.of(false, true), menuStates);
        assertEquals(List.of(true, true), eventThreads);
        assertTrue(dataMenu.isEnabled());
        assertEquals(ContainerStatus.FINISHED, receiver.getZVDatenImRAM().getContainerStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void scriptInitializationKeepsSelectedSolverAndPresentation(boolean external) throws Exception {
        MainWindow._solverSettings.SOLVER_TYPE.setValueWithoutUndo(SolverType.SOLVER_TRZ);
        if (external) {
            window._simRunner.external_init(1);
        } else {
            window._simRunner.initSim(1, 1);
        }
        var kernel = window._simRunner.simKern;
        Path trace = newTrace();
        try (var logger = new SimulationTraceLogger(trace.toFile())) {
            kernel.setTraceLogger(logger);
            kernel.setScopeMenuesStartStop();
            assertFalse(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
            if (external) {
                kernel.external_step(0);
                kernel.external_end();
            } else {
                kernel.simulateOneStep();
                kernel.endSim();
            }
        }

        assertEquals(20.0 / 3, firstVoltage(trace), 1e-9);
        assertTrue(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
        assertEquals(ContainerStatus.FINISHED, receiver.getZVDatenImRAM().getContainerStatus());
    }

    @Test
    void legacyKernelStillUpdatesExistingScopeAndReceiver() throws Exception {
        var kernel = new SimulationsKern();
        var netlist = NetListContainer.fabricStartSimulation(editor, kernel);
        kernel.initSimulation(1, 0, 0, 1, -1, true, netlist, false);
        kernel.initialisiereCONTROLatSimulationStart(1);
        kernel.setScopeMenuesStartStop();
        assertFalse(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
        kernel.simulateOneStep();
        kernel.endSim();

        assertTrue(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
        assertEquals(ContainerStatus.FINISHED, receiver.getZVDatenImRAM().getContainerStatus());
    }

    @Test
    void anEarlierRunsQueuedStopCannotFinalizeReusedPresentationTargets() throws Exception {
        window._simRunner.initSim(1, 1);
        var previousKernel = window._simRunner.simKern;
        var previousData = receiver.getZVDatenImRAM();
        previousKernel.setScopeMenuesStartStop();
        previousKernel.simulateOneStep();
        assertEquals(ContainerStatus.RUNNING, previousData.getContainerStatus());
        var lateStop = new AtomicReference<CompletableFuture<Void>>();
        SwingUtilities.invokeAndWait(() -> {
            lateStop.set(CompletableFuture.runAsync(previousKernel::endSim));
            window._simRunner.initSim(1, 1);
            window._simRunner.simKern.setScopeMenuesStartStop();
            try {
                window._simRunner.simKern.simulateOneStep();
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        });
        lateStop.get().get(10, TimeUnit.SECONDS);

        assertNotSame(previousData, receiver.getZVDatenImRAM());
        assertEquals(ContainerStatus.FINISHED, previousData.getContainerStatus());
        assertFalse(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
        assertEquals(ContainerStatus.RUNNING, receiver.getZVDatenImRAM().getContainerStatus());
        window._simRunner.simKern.endSim();
        assertTrue(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
        assertEquals(ContainerStatus.FINISHED, receiver.getZVDatenImRAM().getContainerStatus());
    }

    @Test
    void aReceiverWidgetFailureCannotSkipRecordingFinalization() throws Exception {
        window._simRunner.initSim(1, 1);
        var kernel = window._simRunner.simKern;
        kernel.setScopeMenuesStartStop();
        kernel.simulateOneStep();
        var data = receiver.getZVDatenImRAM();
        JButton calculate = findCalculateButton(receiverWindow);
        calculate.addPropertyChangeListener("enabled", event -> {
            if (Boolean.TRUE.equals(event.getNewValue())) {
                throw new IllegalStateException("Test receiver widget failure");
            }
        });

        kernel.endSim();

        assertEquals(ContainerStatus.FINISHED, data.getContainerStatus());
    }

    @Test
    void legacyWidgetFacadesKeepTheirStartAndStopStates() throws Exception {
        window._simRunner.initSim(1, 1);
        SwingUtilities.invokeAndWait(() -> {
            scope._scopeFrame.setScopeMenueEnabled(true);
            receiver.setTestReceiverCISPR16MenueEnabled(false);
        });
        assertRunningScopeMenus();
        assertFalse(findCalculateButton(receiverWindow).isEnabled());
        assertEquals("Simulation running. Waiting for data...", findSimulationStatus(receiverWindow));

        SwingUtilities.invokeAndWait(() -> {
            scope._scopeFrame.setScopeMenueEnabled(false);
            receiver.setTestReceiverCISPR16MenueEnabled(true);
        });
        var menus = scope._scopeFrame.getJMenuBar();
        assertTrue(menus.getMenu(0).isEnabled());
        assertTrue(menus.getMenu(1).isEnabled());
        assertTrue(menus.getMenu(2).isEnabled());
        assertTrue(menus.getMenu(3).getItem(0).isEnabled());
        assertTrue(menus.getMenu(3).getItem(1).isEnabled());
        assertFalse(menus.getMenu(3).getItem(2).isEnabled());
        assertTrue(menus.getMenu(3).getItem(3).isEnabled());
        assertTrue(findCalculateButton(receiverWindow).isEnabled());
        assertEquals("Simulation finished, data available.", findSimulationStatus(receiverWindow));
        assertEquals(ContainerStatus.FINISHED, receiver.getZVDatenImRAM().getContainerStatus());
    }

    @Test
    void aReceiverWidgetListenerCanReplaceTheRunWithoutFinishingTheNewRecording() throws Exception {
        window._simRunner.initSim(1, 1);
        var origin = window._simRunner.simKern;
        origin.setScopeMenuesStartStop();
        origin.simulateOneStep();
        var oldData = receiver.getZVDatenImRAM();
        var replaced = new AtomicBoolean();
        var fixtureError = new AtomicReference<Throwable>();
        findCalculateButton(receiverWindow).addPropertyChangeListener("enabled", event -> {
            if (Boolean.TRUE.equals(event.getNewValue()) && replaced.compareAndSet(false, true)) {
                try {
                    window._simRunner.initSim(1, 1);
                    window._simRunner.simKern.setScopeMenuesStartStop();
                    window._simRunner.simKern.simulateOneStep();
                } catch (Throwable error) {
                    fixtureError.set(error);
                }
            }
        });
        try {
            origin.endSim();

            assertNull(fixtureError.get());
            assertTrue(replaced.get());
            assertNotSame(oldData, receiver.getZVDatenImRAM());
            assertEquals(ContainerStatus.FINISHED, oldData.getContainerStatus());
            assertEquals(ContainerStatus.RUNNING, receiver.getZVDatenImRAM().getContainerStatus());
            assertFalse(findCalculateButton(receiverWindow).isEnabled());
            assertEquals("Simulation running. Waiting for data...", findSimulationStatus(receiverWindow));
        } finally {
            window._simRunner.simKern.endSim();
        }
    }

    @Test
    void publicScriptStopCannotPauseTheRunStartedByItsCompletionListener() throws Exception {
        window._simRunner.initSim(1, 1);
        var origin = window._simRunner.simKern;
        origin.setScopeMenuesStartStop();
        origin.simulateOneStep();
        var oldData = receiver.getZVDatenImRAM();
        var replacement = new AtomicReference<SimulationsKern>();
        var replaced = new AtomicBoolean();
        var fixtureError = new AtomicReference<Throwable>();
        findCalculateButton(receiverWindow).addPropertyChangeListener("enabled", event -> {
            if (Boolean.TRUE.equals(event.getNewValue()) && replaced.compareAndSet(false, true)) {
                try {
                    window._simRunner.initSim(0.125, 2);
                    replacement.set(window._simRunner.simKern);
                    replacement.get().setScopeMenuesStartStop();
                    replacement.get().simulateOneStep();
                } catch (Throwable error) {
                    fixtureError.set(error);
                }
            }
        });
        try {
            MainWindow._scripter.endSimulation();

            assertNull(fixtureError.get());
            assertTrue(replaced.get());
            assertNotSame(origin, replacement.get());
            assertSame(replacement.get(), window._simRunner.simKern);
            assertNotSame(oldData, receiver.getZVDatenImRAM());
            assertEquals(ContainerStatus.FINISHED, oldData.getContainerStatus());
            assertEquals(ContainerStatus.RUNNING, receiver.getZVDatenImRAM().getContainerStatus());
            assertEquals(SimulationsKern.SimulationStatus.RUNNING, replacement.get()._simulationStatus);
            assertEquals(2.0, replacement.get().getTEND());
            assertEquals(0.125, replacement.get().getdt());
            assertTrue(window.isSimulationRunning());
            assertFalse(findMainMenuItem(window.getJMenuBar(), "Save").isEnabled());
            assertTrue(findMainMenuItem(window.getJMenuBar(), "Pause").isEnabled());
            assertFalse(findMainMenuItem(window.getJMenuBar(), "Continue").isEnabled());
            assertFalse(findCalculateButton(receiverWindow).isEnabled());
            assertEquals("Simulation running. Waiting for data...", findSimulationStatus(receiverWindow));
            assertEquals("Starting Simulation ... ", statusLabel().getText());
        } finally {
            if (replacement.get() != null) replacement.get().endSim();
        }
    }

    @Test
    void timedPauseKeepsLegacyRunnerStatusAndContinuesTheSameRecording() throws Exception {
        var settings = new SolverSettings();
        settings.dt.setValueWithoutUndo(0.25);
        settings._tDURATION.setValueWithoutUndo(1.0);
        settings._tPAUSE.setValueWithoutUndo(0.25);
        window._simRunner.startCalculation(false, settings);
        var kernel = window._simRunner.simKern;
        var data = receiver.getZVDatenImRAM();
        assertEquals(0.5, kernel.getZeitAktuell());
        assertEquals(SimulationsKern.SimulationStatus.FINISHED, kernel._simulationStatus);
        assertEquals(ContainerStatus.FINISHED, data.getContainerStatus());

        window._simRunner.continueCalculation(false, settings);

        assertSame(kernel, window._simRunner.simKern);
        assertSame(data, receiver.getZVDatenImRAM());
        assertEquals(1.25, kernel.getZeitAktuell());
        assertEquals(SimulationsKern.SimulationStatus.FINISHED, kernel._simulationStatus);
        assertEquals(ContainerStatus.FINISHED, data.getContainerStatus());
    }

    @Test
    void aSupersededQueuedWorkerCannotAdvanceTheResumedKernel() throws Exception {
        var staleWorker = new AtomicReference<Thread>();
        var resumedWorker = new AtomicReference<Thread>();
        var staleSamples = new AtomicInteger();
        var resumed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        List<Double> samples = Collections.synchronizedList(new ArrayList<>());
        var settings = new SolverSettings();
        settings.dt.setValueWithoutUndo(0.25);
        settings._tDURATION.setValueWithoutUndo(1.0);
        try (var logger = new SimulationTraceLogger(newTrace().toFile()) {
            @Override
            public void logStep(double time, LKMatrices matrices, NetListLK netlist) {
                if (Thread.currentThread() == staleWorker.get()) {
                    staleSamples.incrementAndGet();
                    return;
                }
                resumedWorker.set(Thread.currentThread());
                samples.add(time);
                resumed.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for the resumed worker release");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }
        }) {
            window._simRunner.setPendingTraceLogger(logger);
            Set<Thread> before = Thread.getAllStackTraces().keySet();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        window._simRunner.startCalculation(true, settings);
                        staleWorker.set(Thread.getAllStackTraces().keySet().stream()
                                .filter(thread -> !before.contains(thread))
                                .filter(thread -> "simulationThread".equals(thread.getName()))
                                .findFirst().orElseThrow());
                        awaitQueuedPresentation(staleWorker.get());
                        window._simRunner.pauseSimulation();
                        window._simRunner.continueCalculation(true, settings);
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                });
                staleWorker.get().join(5000);
                assertFalse(staleWorker.get().isAlive());
                assertEquals(0, staleSamples.get());
                assertTrue(resumed.await(5, TimeUnit.SECONDS));
            } finally {
                release.countDown();
                if (resumedWorker.get() != null) {
                    resumedWorker.get().join(5000);
                    assertFalse(resumedWorker.get().isAlive());
                }
            }
        }
        assertEquals(List.of(0.0, 0.25, 0.5, 0.75, 1.0), samples);
        assertEquals(1.25, window._simRunner.simKern.getZeitAktuell());
    }

    @Test
    void aFailedPreCalculationCannotPoisonTheNextOrdinaryRun() throws Exception {
        var primary = new ArithmeticException("Test pre-calculation failure");
        TestControl extension = replaceConstant();
        installPreCalculationFailure(extension, false, primary, () -> { });
        var settings = new SolverSettings();
        configurePreCalculation(settings, 0.02);
        RuntimeException reported = assertThrows(RuntimeException.class,
                () -> window._simRunner.startCalculation(false, settings));
        assertSame(primary, reported.getCause());
        assertOrdinaryRetry(extension, settings, settings.inPreCalculationMode);
    }

    @Test
    void aFailedPreCalculationInitializationClearsItsModeBeforeRetry() throws Exception {
        var primary = new ArithmeticException("Test pre-calculation initialization failure");
        TestControl extension = replaceConstant();
        installPreCalculationFailure(extension, true, primary, () -> { });
        var settings = new SolverSettings();
        configurePreCalculation(settings, 0.02);
        assertSame(primary, assertThrows(ArithmeticException.class,
                () -> window._simRunner.startCalculation(false, settings)));
        assertOrdinaryRetry(extension, settings, settings.inPreCalculationMode);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void failedPreCalculationCannotClearItsReplacementMode(boolean failDuringInitialization,
            boolean separateSettings) throws Exception {
        var primary = new ArithmeticException("Test replaced pre-calculation failure");
        TestControl extension = replaceConstant();
        var original = MainWindow._solverSettings;
        configurePreCalculation(original, 0.2);
        var incoming = separateSettings ? new SolverSettings() : original;
        configurePreCalculation(incoming, 0.2);
        boolean originalModeAfterFailure;
        try (var replacement = new PreCalculationReplacement(extension, incoming)) {
            installPreCalculationFailure(extension, failDuringInitialization, primary, replacement::start);
            if (!failDuringInitialization) {
                findCalculateButton(receiverWindow).addPropertyChangeListener("enabled", event -> {
                    if (Boolean.TRUE.equals(event.getNewValue())) replacement.start();
                });
            }
            RuntimeException reported = assertThrows(RuntimeException.class,
                    () -> window._simRunner.startCalculation(false, original));
            assertSame(primary, failDuringInitialization ? reported : reported.getCause());
            replacement.assertRunning();
            originalModeAfterFailure = original.inPreCalculationMode;
            if (!separateSettings) assertTrue(originalModeAfterFailure);
        }
        if (separateSettings) {
            assertAll(() -> assertFalse(originalModeAfterFailure), () -> assertScriptRetry(extension, original));
        }
        assertFalse(incoming.inPreCalculationMode);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedWarmupRespectsSettingsOwnedByAPendingReplacement(boolean separateSettings) throws Exception {
        var primary = new ArithmeticException("Test pending replacement failure");
        TestControl extension = replaceConstant();
        var original = MainWindow._solverSettings;
        configurePreCalculation(original, 0.2);
        var incoming = separateSettings ? new SolverSettings() : original;
        configurePreCalculation(incoming, 0.2);
        try (var replacement = new PreCalculationReplacement(extension, incoming)) {
            installPreCalculationFailure(extension, false, primary, () -> { });
            findCalculateButton(receiverWindow).addPropertyChangeListener("enabled", event -> {
                if (Boolean.TRUE.equals(event.getNewValue())) replacement.startPending();
            });
            RuntimeException reported = assertThrows(RuntimeException.class,
                    () -> window._simRunner.startCalculation(false, original));
            assertSame(primary, reported.getCause());
            replacement.assertPending();
            assertEquals(!separateSettings, original.inPreCalculationMode);
            replacement.continuePreparation();
            replacement.assertRunning();
        }
        if (separateSettings) assertScriptRetry(extension, original);
        assertFalse(incoming.inPreCalculationMode);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void supersededWarmupInitializationReleasesOnlyItsOwnSettings(boolean separateSettings) throws Exception {
        TestControl extension = replaceConstant();
        var original = MainWindow._solverSettings;
        configurePreCalculation(original, 0.2);
        var incoming = separateSettings ? new SolverSettings() : original;
        configurePreCalculation(incoming, 0.2);
        var cancelledSteps = new AtomicInteger();
        boolean originalModeAfterCancellation;
        try (var replacement = new PreCalculationReplacement(extension, incoming)) {
            installPreCalculationCancellation(extension, replacement::start, cancelledSteps);
            window._simRunner.startCalculation(false, original);
            replacement.assertRunning();
            assertEquals(0, cancelledSteps.get());
            originalModeAfterCancellation = original.inPreCalculationMode;
            if (!separateSettings) assertTrue(originalModeAfterCancellation);
        }
        if (separateSettings) {
            assertAll(() -> assertFalse(originalModeAfterCancellation), () -> assertScriptRetry(extension, original));
        }
        assertFalse(incoming.inPreCalculationMode);
    }

    private static void installPreCalculationCancellation(TestControl extension, Runnable replacement,
            AtomicInteger cancelledSteps) {
        class CancelledPreCalculation extends AbstractControlCalculatable
                implements gecko.geckocircuits.control.calculators.InitializableAtSimulationStart {
            CancelledPreCalculation() { super(0, 1); }
            @Override
            public void initializeAtSimulationStart(double dt) { replacement.run(); }
            @Override
            public void berechneYOUT(double dt) { cancelledSteps.incrementAndGet(); }
        }
        extension.calculator = CancelledPreCalculation::new;
    }

    private static void configurePreCalculation(SolverSettings settings, double duration) {
        settings._T_pre.setValueWithoutUndo(0.05);
        settings._dt_pre.setValueWithoutUndo(0.025);
        settings.dt.setValueWithoutUndo(0.01);
        settings._tDURATION.setValueWithoutUndo(duration);
    }

    private static void installPreCalculationFailure(TestControl extension, boolean initialize,
            ArithmeticException primary, Runnable replacement) {
        class FailingPreCalculation extends AbstractControlCalculatable
                implements gecko.geckocircuits.control.calculators.InitializableAtSimulationStart {
            FailingPreCalculation() { super(0, 1); }
            @Override
            public void initializeAtSimulationStart(double dt) {
                if (initialize) {
                    replacement.run();
                    throw primary;
                }
            }
            @Override
            public void berechneYOUT(double dt) { throw primary; }
        }
        extension.calculator = FailingPreCalculation::new;
    }

    private void assertOrdinaryRetry(TestControl extension, SolverSettings settings, boolean modeAfterFailure)
            throws Exception {
        extension.calculator = () -> new ConstantCalculator(1);
        settings._T_pre.setValueWithoutUndo(-0.1);
        Path trace = newTrace();
        try (var logger = new SimulationTraceLogger(trace.toFile())) {
            window._simRunner.setPendingTraceLogger(logger);
            window._simRunner.startCalculation(false, settings);
        }
        var times = Files.readAllLines(trace).stream().skip(1)
                .map(row -> Double.parseDouble(row.split(",")[0])).toList();
        assertAll(() -> assertFalse(modeAfterFailure),
                () -> assertEquals(List.of(0.0, 0.01, 0.02), times),
                () -> assertEquals(0.02, window._simRunner.simKern.getTEND()),
                () -> assertEquals(0.01, window._simRunner.simKern.getdt()),
                () -> assertEquals(0.01, settings._dt_ALT),
                () -> assertFalse(settings.inPreCalculationMode));
    }

    private void assertScriptRetry(TestControl extension, SolverSettings original) throws Exception {
        extension.calculator = () -> new ConstantCalculator(1);
        original._T_pre.setValueWithoutUndo(-0.1);
        MainWindow._scripter.initializeSimulation(0.01, 0.02);
        try {
            assertEquals(0.01, window._simRunner.simKern.getdt());
            assertEquals(0.02, window._simRunner.simKern.getTEND());
            MainWindow._scripter.simulateOneStep();
            assertEquals(0.01, window._simRunner.simKern.getZeitAktuell());
        } finally {
            MainWindow._scripter.endSimulation();
        }
    }

    private final class PreCalculationReplacement implements AutoCloseable {
        private final TestControl extension;
        private final SolverSettings settings;
        private final AtomicReference<SimulationsKern> kernel = new AtomicReference<>();
        private final AtomicReference<Thread> worker = new AtomicReference<>();
        private final CountDownLatch reached = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger steps = new AtomicInteger();
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicBoolean requested = new AtomicBoolean();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final CountDownLatch preparing = new CountDownLatch(1);
        private final CountDownLatch releasePreparation = new CountDownLatch(1);
        private final AtomicReference<CompletableFuture<Void>> pending = new AtomicReference<>();

        private PreCalculationReplacement(TestControl extension, SolverSettings settings) {
            this.extension = extension;
            this.settings = settings;
        }

        private void start() {
            if (!requested.compareAndSet(false, true)) return;
            installGate();
            startWorker();
        }

        private void startPending() {
            if (!requested.compareAndSet(false, true)) return;
            installGate();
            extension.validate = () -> {
                preparing.countDown();
                try {
                    if (!releasePreparation.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out releasing replacement preparation");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            };
            pending.set(CompletableFuture.runAsync(this::startWorker));
            try {
                if (!preparing.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Replacement preparation did not begin");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                failure.set(error);
            }
        }

        private void assertPending() {
            assertNull(failure.get());
            assertEquals(0, preparing.getCount());
            assertFalse(started.get());
            assertTrue(settings.inPreCalculationMode);
        }

        private void continuePreparation() throws Exception {
            releasePreparation.countDown();
            pending.get().get(10, TimeUnit.SECONDS);
            assertNull(failure.get());
        }

        private void startWorker() {
            try {
                window._simRunner.startCalculation(true, settings);
                kernel.set(window._simRunner.simKern);
                started.set(true);
            } catch (Throwable error) {
                failure.set(error);
            }
        }

        private void installGate() {
            extension.calculator = () -> new AbstractControlCalculatable(0, 1) {
                @Override
                public void berechneYOUT(double dt) {
                    worker.set(Thread.currentThread());
                    if (steps.incrementAndGet() != 2) return;
                    reached.countDown();
                    try {
                        if (!release.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out releasing replacement pre-calculation");
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                }
            };
        }

        private void assertRunning() throws Exception {
            assertNull(failure.get());
            assertTrue(started.get());
            assertTrue(reached.await(5, TimeUnit.SECONDS));
            assertSame(kernel.get(), window._simRunner.simKern);
            assertTrue(settings.inPreCalculationMode);
            assertEquals(0.025, kernel.get().getdt());
            assertEquals(0.05, kernel.get().getTEND());
            assertEquals(SimulationsKern.SimulationStatus.RUNNING, kernel.get()._simulationStatus);
            assertEquals(ContainerStatus.RUNNING, receiver.getZVDatenImRAM().getContainerStatus());
            assertTrue(window.isSimulationRunning());
            assertFalse(findMainMenuItem(window.getJMenuBar(), "Save").isEnabled());
            assertEquals("Starting Simulation ... ", statusLabel().getText());
        }

        @Override
        public void close() throws Exception {
            releasePreparation.countDown();
            release.countDown();
            assertAll(() -> { if (pending.get() != null) pending.get().get(10, TimeUnit.SECONDS); },
                    () -> { if (started.get()) assertTrue(reached.await(10, TimeUnit.SECONDS)); },
                    () -> {
                        Thread captured = worker.get();
                        if (captured != null) {
                            captured.join(10000);
                            assertFalse(captured.isAlive());
                        }
                    });
        }
    }

    @Test
    void suppliedPreCalculationSettingsContinueWithTheirOwnTimeStepAndDuration() throws Exception {
        var settings = new SolverSettings();
        settings._T_pre.setValueWithoutUndo(0.5);
        settings._dt_pre.setValueWithoutUndo(0.25);
        settings.dt.setValueWithoutUndo(0.125);
        settings._tDURATION.setValueWithoutUndo(1.0);
        Path trace = newTrace();
        try (var logger = new SimulationTraceLogger(trace.toFile())) {
            window._simRunner.setPendingTraceLogger(logger);
            window._simRunner.startCalculation(false, settings);
        }

        assertEquals(1.625, window._simRunner.simKern.getZeitAktuell());
        assertEquals(1.5, window._simRunner.simKern.getTEND());
        assertEquals(0.125, window._simRunner.simKern.getdt());
        assertEquals(0.125, settings._dt_ALT);
        assertFalse(settings.inPreCalculationMode);
        assertEquals(1.0, MainWindow._solverSettings.dt.getValue());
        var times = Files.readAllLines(trace).stream().skip(1)
                .map(row -> Double.parseDouble(row.split(",")[0])).toList();
        assertEquals(List.of(0.0, 0.25, 0.5, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.5), times);
    }

    @Test
    void scriptInitializationDeliversStatusOnTheEventThread() throws Exception {
        JLabel status = Arrays.stream(window.jtfStatus.getComponents())
                .filter(JLabel.class::isInstance).map(JLabel.class::cast).findFirst().orElseThrow();
        List<Boolean> threads = new ArrayList<>();
        status.addPropertyChangeListener("text", event -> {
            if (String.valueOf(event.getNewValue()).startsWith("Starting Simulation")) {
                threads.add(SwingUtilities.isEventDispatchThread());
            }
        });

        window._simRunner.initSim(0.25, 1);

        assertEquals(List.of(true), threads);
    }

    @Test
    void aPauseMenuListenerCannotOverwriteTheReplacementRunOrItsRecording() throws Exception {
        window._simRunner.initSim(0.25, 1);
        var origin = window._simRunner.simKern;
        origin.setScopeMenuesStartStop();
        origin.simulateOneStep();
        var replacement = new AtomicReference<SimulationsKern>();
        var fixtureError = new AtomicReference<Throwable>();
        var replaced = new AtomicBoolean();
        JMenuItem save = findMainMenuItem(window.getJMenuBar(), "Save");
        save.addPropertyChangeListener("enabled", event -> {
            if (Boolean.TRUE.equals(event.getNewValue()) && replaced.compareAndSet(false, true)) {
                try {
                    window._simRunner.initSim(0.125, 2);
                    replacement.set(window._simRunner.simKern);
                    replacement.get().setScopeMenuesStartStop();
                    replacement.get().simulateOneStep();
                } catch (Throwable error) {
                    fixtureError.set(error);
                }
            }
        });
        try {
            window._simRunner.pauseSimulation();

            assertNull(fixtureError.get());
            assertTrue(replaced.get());
            assertNotSame(origin, window._simRunner.simKern);
            assertSame(replacement.get(), window._simRunner.simKern);
            assertEquals(2.0, replacement.get().getTEND());
            assertEquals(0.125, replacement.get().getdt());
            assertEquals(SimulationsKern.SimulationStatus.RUNNING, replacement.get()._simulationStatus);
            assertEquals(ContainerStatus.RUNNING, NetzlisteCONTROL.globalData.getContainerStatus());
            assertTrue(window.isSimulationRunning());
            assertFalse(save.isEnabled());
            assertTrue(findMainMenuItem(window.getJMenuBar(), "Pause").isEnabled());
            assertFalse(findMainMenuItem(window.getJMenuBar(), "Continue").isEnabled());
        } finally {
            if (replacement.get() != null) {
                replacement.get().endSim();
            }
        }
    }

    private static JMenuItem findMainMenuItem(MenuElement parent, String action) {
        if (parent instanceof JMenuItem item && action.equals(item.getActionCommand())) {
            return item;
        }
        for (MenuElement child : parent.getSubElements()) {
            JMenuItem match = findMainMenuItem(child, action);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    @Test
    void publicScriptStopCannotFinishAConcurrentContinuationOfTheSameKernel() throws Exception {
        window._simRunner.initSim(0.25, 1);
        var kernel = window._simRunner.simKern;
        kernel.setScopeMenuesStartStop();
        kernel.simulateOneStep();
        window._simRunner.pauseSimulation();
        var data = receiver.getZVDatenImRAM();
        var reached = new CountDownLatch(1);
        var continueNow = new CountDownLatch(1);
        var continued = new CountDownLatch(1);
        var releaseStep = new CountDownLatch(1);
        var eventThreadHeld = new CountDownLatch(1);
        var releaseEventThread = new CountDownLatch(1);
        var armEventGate = new AtomicBoolean();
        var held = new AtomicBoolean();
        var callbackFailure = new AtomicReference<Throwable>();
        var originalWorker = new AtomicReference<Thread>();
        var resumedWorker = new AtomicReference<Thread>();
        var resumedEntered = new CountDownLatch(1);
        var continuationStarted = new AtomicBoolean();
        Throwable primaryFailure = null;
        var stopper = new AtomicReference<Thread>();
        var stop = new AtomicReference<CompletableFuture<Void>>();
        findCalculateButton(receiverWindow).addPropertyChangeListener("enabled", event -> {
            if (Boolean.FALSE.equals(event.getNewValue()) && armEventGate.compareAndSet(true, false)) {
                SwingUtilities.invokeLater(() -> {
                    eventThreadHeld.countDown();
                    try {
                        if (!releaseEventThread.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out releasing the continuation presentation gate");
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                });
            }
        });
        try (var logger = new SimulationTraceLogger(newTrace().toFile()) {
            @Override
            public void logStep(double time, LKMatrices matrices, NetListLK netlist) {
                if (!held.compareAndSet(false, true)) {
                    resumedWorker.compareAndSet(null, Thread.currentThread());
                    resumedEntered.countDown();
                    return;
                }
                originalWorker.set(Thread.currentThread());
                reached.countDown();
                try {
                    if (!continueNow.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Continuation was not requested");
                    SwingUtilities.invokeAndWait(() -> findCalculateButton(receiverWindow).setEnabled(true));
                    armEventGate.set(true);
                    window._simRunner.continueCalculation(true, MainWindow._solverSettings);
                    continuationStarted.set(true);
                    continued.countDown();
                    if (!releaseStep.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Original step was not released");
                } catch (Throwable error) {
                    callbackFailure.set(error);
                    continued.countDown();
                    throw new IllegalStateException(error);
                }
            }
        }) {
            kernel.setTraceLogger(logger);
            try {
                window._simRunner.continueCalculation(true, MainWindow._solverSettings);
                assertTrue(reached.await(5, TimeUnit.SECONDS));
                stop.set(CompletableFuture.runAsync(() -> {
                    stopper.set(Thread.currentThread());
                    MainWindow._scripter.endSimulation();
                }));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while ((stopper.get() == null || stopper.get().getState() != Thread.State.BLOCKED)
                        && System.nanoTime() < deadline) Thread.onSpinWait();
                assertEquals(Thread.State.BLOCKED, stopper.get().getState());
                continueNow.countDown();
                assertTrue(continued.await(5, TimeUnit.SECONDS));
                assertNull(callbackFailure.get());
                assertTrue(eventThreadHeld.await(5, TimeUnit.SECONDS));
                releaseStep.countDown();
                deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!stop.get().isDone() && stopper.get().getState() != Thread.State.WAITING
                        && System.nanoTime() < deadline) Thread.onSpinWait();
                assertTrue(stop.get().isDone() || stopper.get().getState() == Thread.State.WAITING);
                assertSame(kernel, window._simRunner.simKern);
                assertSame(data, receiver.getZVDatenImRAM());
                assertEquals(SimulationsKern.SimulationStatus.RUNNING, kernel._simulationStatus);
                assertEquals(ContainerStatus.RUNNING, data.getContainerStatus());
                assertTrue(window.isSimulationRunning());
                assertEquals("Simulation running. Waiting for data...", findSimulationStatus(receiverWindow));
                assertEquals("Starting Simulation ... ", statusLabel().getText());
            } catch (Exception | Error failure) {
                primaryFailure = failure;
                throw failure;
            } finally {
                continueNow.countDown();
                releaseStep.countDown();
                releaseEventThread.countDown();
                try {
                    assertAll(
                            () -> { if (stop.get() != null) stop.get().get(10, TimeUnit.SECONDS); },
                            () -> {
                                if (originalWorker.get() != null) {
                                    originalWorker.get().join(10000);
                                    assertFalse(originalWorker.get().isAlive());
                                }
                            },
                            () -> {
                                if (continuationStarted.get()) assertTrue(resumedEntered.await(10, TimeUnit.SECONDS));
                                if (resumedWorker.get() != null) {
                                    resumedWorker.get().join(10000);
                                    assertFalse(resumedWorker.get().isAlive());
                                }
                            });
                } catch (Exception | Error cleanupFailure) {
                    if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
                    else throw cleanupFailure;
                }
            }
        }
    }

    @Test
    void aQueuedStopCannotFinishTheRecordingOfTheSameKernelsResumedSegment() throws Exception {
        window._simRunner.initSim(0.25, 1);
        var kernel = window._simRunner.simKern;
        kernel.setScopeMenuesStartStop();
        kernel.simulateOneStep();
        window._simRunner.pauseSimulation();
        var data = receiver.getZVDatenImRAM();
        var lateStop = new AtomicReference<CompletableFuture<Void>>();
        var stopper = new AtomicReference<Thread>();
        var entered = new CountDownLatch(1);
        try (var gate = new StepGate(0.25)) {
            kernel.setTraceLogger(gate);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    lateStop.set(CompletableFuture.runAsync(() -> {
                        stopper.set(Thread.currentThread());
                        entered.countDown();
                        kernel.endSim();
                    }));
                    try {
                        assertTrue(entered.await(5, TimeUnit.SECONDS));
                        awaitQueuedPresentation(stopper.get());
                        window._simRunner.continueCalculation(true, MainWindow._solverSettings);
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                });
                assertTrue(gate.reached.await(5, TimeUnit.SECONDS));
                lateStop.get().get(5, TimeUnit.SECONDS);
                assertSame(data, receiver.getZVDatenImRAM());
                assertFalse(scope._scopeFrame.getJMenuBar().getMenu(0).isEnabled());
                assertEquals(ContainerStatus.RUNNING, data.getContainerStatus());
            } finally {
                gate.release.countDown();
                if (gate.worker != null) {
                    gate.worker.join(5000);
                    assertFalse(gate.worker.isAlive());
                }
            }
        }
    }

    @Test
    void aCompletedWorkerCannotFinishOrPauseAReplacementKernel() throws Exception {
        var replaced = new AtomicBoolean();
        var origin = new AtomicReference<SimulationsKern>();
        var replacement = new AtomicReference<SimulationsKern>();
        var fixtureError = new AtomicReference<Throwable>();
        var worker = new AtomicReference<Thread>();
        var sampled = new CountDownLatch(1);
        scope._scopeFrame.getJMenuBar().getMenu(0).addPropertyChangeListener("enabled", event -> {
            if (Boolean.TRUE.equals(event.getNewValue()) && replaced.compareAndSet(false, true)) {
                try {
                    origin.set(window._simRunner.simKern);
                    window._simRunner.initSim(1, 1);
                    replacement.set(window._simRunner.simKern);
                    replacement.get().setScopeMenuesStartStop();
                    replacement.get().simulateOneStep();
                } catch (Throwable error) {
                    fixtureError.set(error);
                }
            }
        });
        var settings = new SolverSettings();
        settings.dt.setValueWithoutUndo(1.0);
        settings._tDURATION.setValueWithoutUndo(0.0);
        try (var logger = new SimulationTraceLogger(newTrace().toFile()) {
            @Override
            public void logStep(double time, LKMatrices matrices, NetListLK netlist) {
                worker.set(Thread.currentThread());
                sampled.countDown();
            }
        }) {
            window._simRunner.setPendingTraceLogger(logger);
            window._simRunner.startCalculation(true, settings);
            assertTrue(sampled.await(5, TimeUnit.SECONDS));
            worker.get().join(5000);
            assertFalse(worker.get().isAlive());
            SwingUtilities.invokeAndWait(() -> { });

            assertNull(fixtureError.get());
            assertTrue(replaced.get());
            assertNotSame(origin.get(), replacement.get());
            assertEquals(SimulationsKern.SimulationStatus.FINISHED, origin.get()._simulationStatus);
            assertEquals(SimulationsKern.SimulationStatus.RUNNING, replacement.get()._simulationStatus);
            assertTrue(window.isSimulationRunning());
            assertRunningScopeMenus();
            assertEquals(ContainerStatus.RUNNING, receiver.getZVDatenImRAM().getContainerStatus());
        } finally {
            if (replacement.get() != null) {
                replacement.get().endSim();
            }
        }
    }

    private void assertRunningScopeMenus() {
        var menus = scope._scopeFrame.getJMenuBar();
        assertFalse(menus.getMenu(0).isEnabled());
        assertFalse(menus.getMenu(1).isEnabled());
        assertFalse(menus.getMenu(2).isEnabled());
        JMenu simulation = menus.getMenu(3);
        assertFalse(simulation.getItem(0).isEnabled());
        assertFalse(simulation.getItem(1).isEnabled());
        assertTrue(simulation.getItem(2).isEnabled());
        assertFalse(simulation.getItem(3).isEnabled());
    }

    private static void awaitQueuedPresentation(Thread worker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (worker.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(Thread.State.WAITING, worker.getState());
    }

    private static final class StepGate extends SimulationTraceLogger {
        private final double time;
        private final AtomicBoolean held = new AtomicBoolean();
        private final CountDownLatch reached = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile Thread worker;

        private StepGate(double time) throws Exception {
            super(newTrace().toFile());
            this.time = time;
        }

        @Override
        public void logStep(double sampleTime, LKMatrices matrices, NetListLK netlist) {
            if (sampleTime == time && held.compareAndSet(false, true)) {
                worker = Thread.currentThread();
                reached.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for the test segment release");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }
        }
    }

    private static JButton findCalculateButton(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button && "Calculate".equals(button.getText())) {
                return button;
            }
            if (child instanceof Container container) {
                JButton button = findCalculateButton(container);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static String findSimulationStatus(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel label && label.getText() != null
                    && label.getText().startsWith("Simulation ")) {
                return label.getText();
            }
            if (child instanceof Container container) {
                String status = findSimulationStatus(container);
                if (status != null) {
                    return status;
                }
            }
        }
        return null;
    }

    private static Path newTrace() {
        return Path.of(System.getProperty("gecko.test.auditDirectory", System.getProperty("java.io.tmpdir")),
                "gecko-runner-contract-" + java.util.UUID.randomUUID() + ".csv");
    }

    private static double firstVoltage(Path trace) throws Exception {
        String[] row = Files.readAllLines(trace).get(1).split(",");
        return Math.abs(Double.parseDouble(row[2]) - Double.parseDouble(row[1]));
    }
}
