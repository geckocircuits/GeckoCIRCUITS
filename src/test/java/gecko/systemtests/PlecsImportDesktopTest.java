package gecko.systemtests;

import gecko.core.io.importer.Main;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.ConnectorType;
import gecko.geckocircuits.circuit.circuitcomponents.IdealSwitch;
import gecko.geckocircuits.control.ReglerGate;
import gecko.geckocircuits.control.ReglerOSZI;
import gecko.geckocircuits.newscope.ScopeViewport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class PlecsImportDesktopTest {
    @ParameterizedTest
    @ValueSource(strings = {"plecs-rc", "plecs-buck"})
    void runsImportedCircuit(String fixture) throws Exception {
        ImportedDesktopRun.launch(fixture, DesktopRun.class);
    }

    @Test
    void checksPaintedScopeAfterBackgroundViewportReset() throws Exception {
        ImportedDesktopRun.launch("plecs-buck-viewport-reset", DesktopRun.class);
    }

    public static final class DesktopRun {
        public static void main(String[] args) {
            try {
                run(args[0], Path.of(args[1]));
                System.exit(0);
            } catch (Throwable error) {
                error.printStackTrace();
                System.exit(1);
            }
        }

        private static void run(String scenario, Path evidence) throws Exception {
            boolean resetViewport = scenario.equals("plecs-buck-viewport-reset");
            String fixture = resetViewport ? "plecs-buck" : scenario;
            boolean buck = fixture.equals("plecs-buck");
            assertTrue(buck || fixture.equals("plecs-rc"), "Unknown fixture " + fixture);
            Path source = Path.of("src/modules/gecko-simulation-core/src/test/resources/importer", fixture + ".plecs");
            Path circuit = evidence.resolve(fixture + ".ipes");
            assertEquals(0, Main.run(new String[]{source.toString(), circuit.toString()}, System.out, System.err));
            double duration = buck ? .008 : .007;
            double step = buck ? 2e-7 : 7e-7;
            ImportedDesktopRun.open(circuit, duration, step, () -> checkSchematic(buck), (scope, steps) -> {
                var electrical = blocks(MainWindow._se.getElementLK());
                var data = scope.getZVDatenImRAM();
                var output = electrical.get(buck ? "Output" : "Filter");
                int outputRow = scopeRow(scope, "V_" + pin(output, true));
                int samples = data.getMaximumTimeIndex(outputRow) + 1;
                assertEquals(steps, samples);
                assertNearTimeGrid(scope, outputRow, samples, step);
                String measurements = buck ? checkBuck(scope, electrical.get("Main"), outputRow, samples)
                        : checkRc(scope, outputRow, samples);
                ImportedDesktopRun.captureSchematic(evidence.resolve(fixture + "-schematic.png"));
                ImportedDesktopRun.captureScope(scope, 0, duration, evidence.resolve(fixture + "-scope.png"), !buck);
                if (buck) {
                    Path detail = evidence.resolve(fixture + "-pwm-detail.png");
                    if (resetViewport) {
                        ImportedDesktopRun.captureScope(scope, .0078, .008, detail, true, () -> {
                            resetViewportOnBackgroundThread(scope, duration);
                            return null;
                        });
                    } else {
                        ImportedDesktopRun.captureScope(scope, .0078, .008, detail, true);
                    }
                }
                Files.writeString(evidence.resolve("receipt.txt"), String.format(Locale.ROOT, """
                        source=%s
                        circuit=%s
                        backend=core
                        state=COMPLETED
                        components=%s
                        rendered_traces=%d
                        samples=%d
                        step_seconds=%.9f
                        %s
                        """, source, circuit.getFileName(), electrical.keySet().stream().sorted().toList(),
                        data.getRowLength(), samples, step, measurements));
            });
        }
    }

    private static void resetViewportOnBackgroundThread(ReglerOSZI scope, double end) throws Exception {
        var graph = scope._scopeFrame.getGrafer();
        var reset = new FutureTask<Void>(() -> {
            graph.setSimulationTimeBoundaries(0, end);
            return null;
        });
        Thread.ofVirtual().name("scope-viewport-reset").start(reset);
        reset.get(5, TimeUnit.SECONDS);
        assertTrue(ScopeViewport.read(graph.getManager().getDiagrams().getFirst(), 0, end)
                .matchesTimeRange(0, end),
                "The background thread must restore the full time range");
    }

    private static ReglerOSZI checkSchematic(boolean buck) {
        var editor = MainWindow._se;
        assertTrue(editor.getConversionIssues().isEmpty(), "The desktop must load a complete conversion");
        var electrical = blocks(editor.getElementLK());
        var control = blocks(editor.getElementCONTROL());
        var wires = editor.getConnection(ConnectorType.LK_AND_RELUCTANCE);
        assertEquals(electrical.size() * 2, wires.size());
        for (var block : electrical.values()) {
            for (var terminal : Set.of(block.XIN.getFirst(), block.YOUT.getFirst())) {
                assertEquals(1, wires.stream().filter(wire -> wire.getAllPointCoordinates().contains(terminal.getPosition())
                        && wire.getLabel().equals(terminal.getLabelObject().getLabelString())).count(),
                        "Labeled wire at " + block.getStringID() + " terminal " + terminal.getPosition());
            }
        }
        Set<String> nodes;
        if (buck) {
            assertEquals(Set.of("Supply", "Main", "Catch", "Choke", "Output", "Load"), electrical.keySet());
            nodes = Set.of(pin(electrical.get("Supply"), true), pin(electrical.get("Main"), false),
                    pin(electrical.get("Output"), true), "0");
            assertEquals(pin(electrical.get("Supply"), true), pin(electrical.get("Main"), true));
            assertEquals(pin(electrical.get("Main"), false), pin(electrical.get("Catch"), false));
            assertEquals(pin(electrical.get("Main"), false), pin(electrical.get("Choke"), true));
            assertEquals(pin(electrical.get("Choke"), false), pin(electrical.get("Output"), true));
            assertEquals(pin(electrical.get("Output"), true), pin(electrical.get("Load"), true));
            for (String id : Set.of("Supply", "Output", "Load")) assertEquals("0", pin(electrical.get(id), false));
            assertEquals("0", pin(electrical.get("Catch"), true));
            assertInstanceOf(IdealSwitch.class, electrical.get("Main"));
            var gate = assertInstanceOf(ReglerGate.class, control.get("PLECS.GATE.Main"));
            assertSame(electrical.get("Main"), gate.getComponentCoupling()._coupledElements[0]);
            assertLeftToRight(electrical, "Supply", "Main", "Catch", "Choke", "Output", "Load");
            assertTrue(electrical.get("Main").getSheetPosition().y < electrical.get("Catch").getSheetPosition().y);
        } else {
            assertEquals(Set.of("Input", "Series", "Filter"), electrical.keySet());
            nodes = Set.of(pin(electrical.get("Input"), true), pin(electrical.get("Filter"), true), "0");
            assertEquals(pin(electrical.get("Input"), true), pin(electrical.get("Series"), true));
            assertEquals(pin(electrical.get("Series"), false), pin(electrical.get("Filter"), true));
            assertEquals("0", pin(electrical.get("Filter"), false));
            assertEquals("0", pin(electrical.get("Input"), false));
            assertLeftToRight(electrical, "Input", "Series", "Filter");
            assertTrue(electrical.get("Series").getSheetPosition().y < electrical.get("Filter").getSheetPosition().y);
        }
        assertEquals(nodes, wires.stream().map(wire -> wire.getLabel()).collect(Collectors.toSet()));
        var scope = assertInstanceOf(ReglerOSZI.class, control.get("SCOPE.IMPORT"));
        var diagrams = scope._scopeFrame.getGrafer().getManager().getDiagrams();
        assertEquals(1, diagrams.size());
        assertEquals(nodes.size() - 1, diagrams.getFirst().getCurves().size());
        return scope;
    }

    private static String checkRc(ReglerOSZI scope, int row, int samples) {
        var data = scope.getZVDatenImRAM();
        double peakError = 0;
        for (int i = 0; i < samples; i++) {
            double elapsed = Math.max(0, data.getTimeValue(i, row) - 100e-6);
            peakError = Math.max(peakError, Math.abs(data.getValue(row, i) + 6 * Math.expm1(-elapsed / .001)));
        }
        assertTrue(peakError <= .06, "Maximum capacitor voltage error in V " + peakError);
        return String.format(Locale.ROOT, "peak_rc_error_v=%.9f%nrc_limit_v=0.06", peakError);
    }

    private static String checkBuck(ReglerOSZI scope, AbstractBlockInterface main, int row, int samples) {
        var data = scope.getZVDatenImRAM();
        int switchedNode = scopeRow(scope, "V_" + pin(main, false));
        double previous = 0, settled = 0;
        int previousCount = 0, settledCount = 0, on = 0, off = 0;
        for (int i = 0; i < samples; i++) {
            double time = data.getTimeValue(i, row);
            if (time < .006 - 1e-11 || time >= .008 - 1e-11) continue;
            double output = data.getValue(row, i);
            if (time < .007 - 1e-11) {
                previous += output;
                previousCount++;
                continue;
            }
            settled += output;
            settledCount++;
            double phase = time % 50e-6;
            if (phase > 5e-6 && phase < 15e-6) {
                assertEquals(30, data.getValue(switchedNode, i), .1, "Negative PWM level must close the switch");
                on++;
            } else if (phase > 25e-6 && phase < 45e-6) {
                assertEquals(0, data.getValue(switchedNode, i), .1, "Zero PWM level must open the switch");
                off++;
            }
        }
        assertEquals(5000, previousCount);
        assertEquals(5000, settledCount);
        assertTrue(on >= 950 && off >= 1950);
        previous /= previousCount;
        settled /= settledCount;
        double ideal = .37 * 30;
        assertEquals(previous, settled, .02, "Check settling across adjacent 20-period windows");
        assertEquals(ideal, settled, .02 * ideal);
        return String.format(Locale.ROOT, "buck_6_to_7_ms_v=%.9f%nbuck_7_to_8_ms_v=%.9f%nideal_v=%.3f%n"
                + "relative_error_percent=%.6f%nnegative_pwm_on_samples=%d%nzero_pwm_off_samples=%d",
                previous, settled, ideal, 100 * Math.abs(settled - ideal) / ideal, on, off);
    }

    private static int scopeRow(ReglerOSZI scope, String signal) {
        var data = scope.getZVDatenImRAM();
        for (int row = 0; row < data.getRowLength(); row++) {
            if (signal.equals(data.getSignalName(row))) return row;
        }
        throw new AssertionError("Scope signal missing " + signal);
    }

    private static void assertNearTimeGrid(ReglerOSZI scope, int row, int samples, double step) {
        var data = scope.getZVDatenImRAM();
        for (int i = 0; i < samples; i++) assertEquals(i * step, data.getTimeValue(i, row), 1e-10);
    }

    private static void assertLeftToRight(Map<String, AbstractBlockInterface> blocks, String... ids) {
        for (int i = 1; i < ids.length; i++) {
            assertTrue(blocks.get(ids[i - 1]).getSheetPosition().x < blocks.get(ids[i]).getSheetPosition().x,
                    ids[i - 1] + " must be left of " + ids[i]);
        }
    }

    private static String pin(AbstractBlockInterface block, boolean input) {
        return (input ? block.XIN : block.YOUT).getFirst().getLabelObject().getLabelString();
    }

    private static Map<String, AbstractBlockInterface> blocks(Iterable<? extends AbstractBlockInterface> blocks) {
        var stream = java.util.stream.StreamSupport.stream(blocks.spliterator(), false);
        return stream.collect(Collectors.toMap(AbstractBlockInterface::getStringID, Function.identity()));
    }
}
