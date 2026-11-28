package gecko.core.io.importer;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.CircuitTopology;
import gecko.core.io.CircuitTopology.Side;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PlecsImportContractTest {
    @TempDir Path directory;

    @Test void pulseDrivenRcMatchesTheAnalyticStepResponse() throws Exception {
        var model = fixture("rc");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        assertEquals(3, model.getCircuitComponents().size());
        assertEquals(1, model.getResources().size());

        var trace = voltages(model, "Filter");
        assertTrue(trace.size() >= 6999);
        double largestError = 0;
        for (var sample : trace) {
            double elapsed = Math.max(0, sample.time() - 1e-4);
            double expected = 6 * (1 - Math.exp(-elapsed / 1e-3));
            largestError = Math.max(largestError, Math.abs(expected - sample.voltage()));
        }
        assertEquals(0, largestError, 0.06);
        System.out.printf(java.util.Locale.ROOT, "PLECS RC | %d samples | peak analytic error %.6f V%n", trace.size(), largestError);
    }

    @Test void switchedBuckSettlesAtDutyTimesInputVoltage() throws Exception {
        var model = fixture("buck");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        assertEquals(6, model.getCircuitComponents().size());
        assertEquals(1, model.getResources().size());

        var trace = voltages(model, "Output");
        double previous = average(trace, 0.006, 0.007, model.getTimeStep());
        double settled = average(trace, 0.007, 0.008, model.getTimeStep());
        assertEquals(previous, settled, 0.02, "Check settling across two adjacent 20-period windows.");
        assertEquals(0.37 * 30, settled, 0.02 * 0.37 * 30);
        System.out.printf(java.util.Locale.ROOT, "PLECS buck | 6-7 ms %.6f V | 7-8 ms %.6f V | ideal %.6f V%n",
                previous, settled, 0.37 * 30);
    }

    @ParameterizedTest @ValueSource(strings = {"DC", "AC", "PULSE", "SINE", "SINE_HZ_PU", "SINE_HZ_DEG"})
    void currentSourcesPreserveWaveformAndPolarity(String shape) throws Exception {
        String source = switch (shape) {
            case "DC" -> "Type DCCurrentSource Parameter { Variable \"I\" Value \"1.2\" }";
            case "AC" -> """
                    Type ACCurrentSource
                    Parameter { Variable "I" Value "0.5" }
                    Parameter { Variable "w" Value "2*pi*1000" }
                    Parameter { Variable "phi" Value "0.3" }
                    """;
            default -> "Type CurrentSource";
        };
        String driver = switch (shape) {
            case "PULSE" -> """
                    Component {
                      Name "Drive" Position [30,50] Direction right Type PulseGenerator
                      Parameter { Variable "Hi" Value "1.25" }
                      Parameter { Variable "Lo" Value "-0.25" }
                      Parameter { Variable "f" Value "1000" }
                      Parameter { Variable "DutyCycle" Value "0.5" }
                    }
                    """;
            case "SINE" -> """
                    Component {
                      Name "Drive" Position [30,50] Direction right Type SineGenerator
                      Parameter { Variable "Amplitude" Value "0.5" }
                      Parameter { Variable "Bias" Value "1.2" }
                      Parameter { Variable "Frequency" Value "2*pi*1000" }
                      Parameter { Variable "Phase" Value "0.3" }
                    }
                    """;
            case "SINE_HZ_PU", "SINE_HZ_DEG" -> """
                    Component {
                      Name "Drive" Position [30,50] Direction right Type SineGenerator
                      Parameter { Variable "Amplitude" Value "0.5" }
                      Parameter { Variable "Bias" Value "1.2" }
                      Parameter { Variable "Frequency" Value "750" }
                      Parameter { Variable "Phase" Value "%s" }
                      Parameter { Variable "Units" Value "%s" }
                    }
                    """.formatted(shape.equals("SINE_HZ_PU") ? "0.25" : "90", shape.equals("SINE_HZ_PU") ? "2" : "3");
            default -> "";
        };
        String signalWire = driver.isEmpty() ? ""
                : "Connection { Type Signal SrcComponent \"Drive\" SrcTerminal 1 DstComponent \"I\" DstTerminal 3 }";
        var model = convert("""
                Plecs {
                  StopTime "0.0012"
                  Schematic {
                    Component { Name "I" Position [80,50] Direction up Flipped off %s }
                    Component { Type Resistor Name "Load" Position [140,50] Direction up Parameter { Variable "R" Value "5" } }
                    %s
                    %s
                    Connection { Type Wire SrcComponent "I" SrcTerminal 2 Points [80,20] DstComponent "Load" DstTerminal 1 }
                    Connection { Type Wire SrcComponent "I" SrcTerminal 1 Points [80,85] DstComponent "Load" DstTerminal 2 }
                  }
                }
                """.formatted(source, driver, signalWire));
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        assertEquals(501, component(model, "I").getOrientation());
        assertEquals(shape.equals("DC") ? 0 : 1, model.getResources().size());
        for (var sample : voltages(model, "Load")) {
            if (sample.time() < 10 * model.getTimeStep()) continue;
            double time = sample.time() - 2 * model.getTimeStep();
            double phase = time % 1e-3;
            if (shape.equals("PULSE") && (phase < 5e-6 || Math.abs(phase - 5e-4) < 5e-6 || phase > 995e-6)) continue;
            double expected = switch (shape) {
                case "DC" -> 6;
                case "AC" -> 2.5 * Math.sin(2 * Math.PI * 1000 * time + 0.3);
                case "PULSE" -> phase < 5e-4 ? 6.25 : -1.25;
                case "SINE_HZ_PU", "SINE_HZ_DEG" -> 6 + 2.5 * Math.cos(2 * Math.PI * 750 * time);
                default -> 6 + 2.5 * Math.sin(2 * Math.PI * 1000 * time + 0.3);
            };
            assertEquals(expected, sample.voltage(), 0.01);
        }
    }

    @Test void nativeBraceListsPreserveSupportedComponentsAndUnsupportedBlockReports() throws Exception {
        String input = """
                Plecs {
                  Schematic {
                    Component { Type Scope Name "Plot" Position [14, 28]
                      Axis {
                        Signals {"Voltage, in V", "Sample with } and " "more text"}
                      }
                      Axis { Signals {} }
                    }
                    Component { Type Subsystem Name "Hidden" Position [38, 20]
                      Mask { Parameter {
                        Variable "mode"
                        PossibleValues {"Manual", "Automatic"}
                        Value "2"
                      } }
                      Schematic {}
                    }
                    Component { Type CScript Name "Code" Position [52, 31] }
                    Component { Type Resistor Name "Load" Position [70, 57] Parameter { Variable "R" Value "4" } }
                  }
                }
                """;
        var nativeModel = PlecsText.parse(input);
        var nativeParts = nativeModel.children("Schematic").getFirst().children("Component");
        var axes = nativeParts.getFirst().children("Axis");
        assertEquals(List.of("Voltage, in V", "Sample with } and more text"), axes.getFirst().lists().get("Signals"));
        assertEquals(List.of(), axes.getLast().lists().get("Signals"));
        var maskParameter = nativeParts.get(1).children("Mask").getFirst().children("Parameter").getFirst();
        assertEquals(List.of("Manual", "Automatic"), maskParameter.lists().get("PossibleValues"));
        var model = reopen(convert(input));
        assertEquals("Load", model.getCircuitComponents().getFirst().getName());
        assertEquals(List.of("UNSUPPORTED_PLECS_COMPONENT", "UNSUPPORTED_PLECS_SUBSYSTEM", "UNSUPPORTED_PLECS_C_SCRIPT"),
                model.getConversionIssues().stream().map(issue -> issue.code()).toList());
    }

    @Test void configurableSubsystemAndBase64TextPreserveSiblingComponents() throws Exception {
        String input = """
                Plecs {
                  Schematic {
                    Component { Type ConfigurableSubsystem Name "Selectable" Position [30, 44]
                      MaskDescription base64 "z4"
                        "Q="
                      Configurations {
                        Name "Mode A"
                        Schematic { Component { Type Resistor Name "Hidden A" Position [20,20] } }
                        Name "Mode B"
                        Schematic { Component { Type Resistor Name "Hidden B" Position [40,40] } }
                      }
                    }
                    Component { Type Resistor Name "Kept" Position [100, 73] Parameter { Variable "R" Value "8" } }
                  }
                }
                """;
        var configurable = PlecsText.parse(input).children("Schematic").getFirst().children("Component").getFirst();
        assertEquals("\u03c4", configurable.get("MaskDescription"));
        assertEquals(2, configurable.children("Configurations").getFirst().children("Schematic").size());
        var reopened = reopen(convert(input));
        assertEquals(List.of("Kept"), reopened.getCircuitComponents().stream().map(ComponentData::getName).toList());
        assertEquals(List.of("UNSUPPORTED_PLECS_SUBSYSTEM"),
                reopened.getConversionIssues().stream().map(issue -> issue.code()).toList());
    }

    @ParameterizedTest @ValueSource(strings = {"TimeSpan", "StopTime"})
    void readsNativeTimeSpanAndLegacyStopTime(String field) throws Exception {
        var model = reopen(convert(divider(field + " \"0.003\" MaxStep \"1e-5\"", "", "170, 55")));
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        assertEquals(0.003, model.getSimulationDuration());
        assertEquals(3e-7, model.getTimeStep(), 1e-16);
    }

    @ParameterizedTest @CsvSource({"DisabledOpenCircuit,0,0", "DisabledClosedCircuit,2,12"})
    void preservesNativeCommentModeAndDividerVoltage(String mode, int enabledState, double expected) throws Exception {
        var model = convert(divider("TimeSpan \"0.003\"", "CommentOut " + mode, "170, 55"));
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var reopened = reopen(reopen(model));
        assertEquals(enabledState, component(reopened, "Upper").getSavedBlock().enabledState());
        var topology = CircuitTopology.resolve(reopened);
        Integer input = group(topology, reopened, "Supply", Side.INPUT);
        Integer output = group(topology, reopened, "Load", Side.INPUT);
        assertNotEquals(output, group(topology, reopened, "Load", Side.OUTPUT));
        if (enabledState == 2) {
            assertEquals(input, output);
            assertEquals(output, group(topology, reopened, "Upper", Side.INPUT));
            assertEquals(output, group(topology, reopened, "Upper", Side.OUTPUT));
        } else {
            assertNotEquals(input, output);
            assertNull(group(topology, reopened, "Upper", Side.INPUT));
            assertNull(group(topology, reopened, "Upper", Side.OUTPUT));
        }
        assertEquals(expected, voltages(model, "Load").getLast().voltage(), 1e-8);
    }

    @Test void routesTightLayoutWithoutJoiningDifferentNets() throws Exception {
        var model = convert(divider("TimeSpan \"0.003\"", "", "98, 46"));
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var reopened = reopen(model);
        assertArrayEquals(new int[]{98, 46}, component(reopened, "Load").getPosition());
        var topology = CircuitTopology.resolve(reopened);
        Integer supply = group(topology, reopened, "Supply", Side.INPUT);
        Integer midpoint = group(topology, reopened, "Upper", Side.OUTPUT);
        Integer ground = group(topology, reopened, "Load", Side.OUTPUT);
        assertEquals(supply, group(topology, reopened, "Upper", Side.INPUT));
        assertEquals(midpoint, group(topology, reopened, "Load", Side.INPUT));
        assertEquals(ground, group(topology, reopened, "Supply", Side.OUTPUT));
        assertEquals(3, List.of(supply, midpoint, ground).stream().distinct().count());
        assertEquals(6, voltages(model, "Load").getLast().voltage(), 1e-8);
    }

    @Test void coincidentTerminalsOnDifferentNetsProduceARetainedLayoutIssue() throws Exception {
        var model = convert(divider("TimeSpan \"0.003\"", "", "100, 46"));
        assertTrue(model.getConversionIssues().stream().anyMatch(
                issue -> issue.code().equals("UNSUPPORTED_PLECS_LAYOUT") && issue.parameterPath().equals("Position")));
        assertEquals(model.getConversionIssues(), reopen(model).getConversionIssues());
    }

    @Test void controlledSourcesReportMalformedAndUnsupportedSavedChoices() throws Exception {
        var model = convert("""
                Plecs {
                  Schematic {
                    Component { Type VoltageSource Name "Invalid" Position [20,50]
                      Parameter { Variable "DiscretizationBehavior" Value "unresolvedBehavior" }
                      Parameter { Variable "StateSpaceInlining" Value "9" }
                    }
                    Component { Type CurrentSource Name "Advanced" Position [70,50]
                      Parameter { Variable "DiscretizationBehavior" Value "3" }
                      Parameter { Variable "StateSpaceInlining" Value "2" }
                    }
                    Component { Type Resistor Name "Kept" Position [98,86] Parameter { Variable "R" Value "2" } }
                  }
                }
                """);
        var reopened = reopen(model);
        assertEquals(List.of("Kept"), reopened.getCircuitComponents().stream().map(ComponentData::getName).toList());
        assertEquals(List.of("INVALID_PLECS_PARAMETER", "INVALID_PLECS_PARAMETER",
                "UNSUPPORTED_PLECS_PARAMETER", "UNSUPPORTED_PLECS_PARAMETER"),
                reopened.getConversionIssues().stream().map(issue -> issue.code()).toList());
        assertEquals(List.of("DiscretizationBehavior", "StateSpaceInlining", "DiscretizationBehavior", "StateSpaceInlining"),
                reopened.getConversionIssues().stream().map(issue -> issue.parameterPath()).toList());
    }

    @Test void discreteControlledSourceProducesAnExplicitDiscretizationIssue() {
        var model = convert("""
                Plecs {
                  CircuitModel "DiscStateSpace"
                  Schematic { Component { Type VoltageSource Name "Discrete" Position [20,50] } }
                }
                """);
        assertEquals("UNSUPPORTED_PLECS_PARAMETER", model.getConversionIssues().getFirst().code());
        assertEquals("DiscretizationBehavior", model.getConversionIssues().getFirst().parameterPath());
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void voltageMeasuredSwitchComparesTheThresholdAndHoldsAtEquality(int low) throws Exception {
        var model = convert("""
                Plecs {
                  StopTime "0.00032"
                  Schematic {
                    Component { Type DCVoltageSource Name "Supply" Position [35,60] Direction up Parameter { Variable "V" Value "9" } }
                    Component { Type Switch Name "Switch" Position [85,25] Direction left }
                    Component { Type Resistor Name "Load" Position [135,60] Direction up Parameter { Variable "R" Value "10" } }
                    Component { Type VoltageSource Name "Control" Position [195,60] Direction up }
                    Component { Type Voltmeter Name "Measure" Position [245,60] Direction up }
                    Component { Type Comparator Name "Compare" Position [135,100] Direction right }
                    Component { Type Constant Name "Threshold" Position [60,130] Direction right Parameter { Variable "Value" Value "2" } }
                    Component { Type PulseGenerator Name "Drive" Position [160,60] Direction right
                      Parameter { Variable "Hi" Value "3" }
                      Parameter { Variable "Lo" Value "%d" }
                      Parameter { Variable "f" Value "5000" }
                      Parameter { Variable "DutyCycle" Value "0.4" }
                      Parameter { Variable "Delay" Value "1e-4" }
                    }
                    Connection { Type Wire SrcComponent "Supply" SrcTerminal 1 DstComponent "Switch" DstTerminal 1 }
                    Connection { Type Wire SrcComponent "Switch" SrcTerminal 2 DstComponent "Load" DstTerminal 1 }
                    Connection { Type Wire SrcComponent "Supply" SrcTerminal 2
                      Branch { DstComponent "Load" DstTerminal 2 }
                      Branch { DstComponent "Control" DstTerminal 2 }
                      Branch { DstComponent "Measure" DstTerminal 2 }
                    }
                    Connection { Type Wire SrcComponent "Control" SrcTerminal 1 DstComponent "Measure" DstTerminal 1 }
                    Connection { Type Signal SrcComponent "Drive" SrcTerminal 1 DstComponent "Control" DstTerminal 3 }
                    Connection { Type Signal SrcComponent "Measure" SrcTerminal 3 DstComponent "Compare" DstTerminal 1 }
                    Connection { Type Signal SrcComponent "Threshold" SrcTerminal 1 DstComponent "Compare" DstTerminal 2 }
                    Connection { Type Signal SrcComponent "Compare" SrcTerminal 3 DstComponent "Switch" DstTerminal 3 }
                  }
                }
                """.formatted(low));
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var trace = voltages(model, "Load");
        assertEquals(0, average(trace, 5e-5, 9e-5, model.getTimeStep()), 1e-6);
        assertEquals(9, average(trace, 1.2e-4, 1.6e-4, model.getTimeStep()), 1e-5);
        assertEquals(low == 2 ? 9 : 0, average(trace, 2e-4, 2.4e-4, model.getTimeStep()), 1e-5);
    }

    @Test void preservesNumberedTerminalsDirectionsPositionsAndResourcesAfterTwoWrites() throws Exception {
        var model = fixture("buck");
        var parsed = reopen(reopen(model));
        assertEquals(model.getImportIssues(), parsed.getImportIssues());
        assertEquals(6, parsed.getCircuitComponents().size());
        var main = component(parsed, "Main");
        var diode = component(parsed, "Catch");
        var choke = component(parsed, "Choke");
        var supply = component(parsed, "Supply");
        var output = component(parsed, "Output");
        var load = component(parsed, "Load");
        assertArrayEquals(new int[]{137, 75}, main.getPosition());
        assertEquals(502, main.getOrientation());
        assertArrayEquals(new int[]{201, 124}, diode.getPosition());
        assertEquals(501, diode.getOrientation());
        assertEquals(503, supply.getOrientation());
        assertEquals(first(supply), first(main));
        assertEquals(second(main), second(diode));
        assertEquals(second(main), first(choke));
        assertEquals(second(choke), first(output));
        assertEquals(second(choke), first(load));
        assertEquals("0", first(diode));
        assertEquals(second(supply), second(output));
        assertEquals(second(supply), second(load));
        assertEquals(4, List.of(first(main), second(main), second(choke), second(supply)).stream().distinct().count());
        assertTrue(parsed.getConnections().stream().anyMatch(wire -> wire.getLabel().equals(first(main))
                && wire.getPoints()[0][0] == 135 && wire.getPoints()[0][1] == 75));
        var pwm = parsed.getControlComponents().stream().filter(control -> control.getName().equals("PWM"))
                .findFirst().orElseThrow();
        assertArrayEquals(new int[]{65, 36}, pwm.getPosition());
        model.getResources().forEach((id, resource) -> {
            assertArrayEquals(resource.bytes(), parsed.getResources().get(id).bytes());
            assertEquals(resource.ownerIds(), parsed.getResources().get(id).ownerIds());
        });
    }

    @Test void convertsRemainingComponentsWithSubsystemAndCScriptDiagnostics() throws Exception {
        String source = """
                Plecs {
                  Schematic {
                    /* This comment has unmatched delimiters: } " [ */
                    Component { Type Resistor Name "Kept" Position [30, 40] Parameter { Variable "R" Value "40" } }
                    Component {
                      Type Subsystem
                      Name "Hidden"
                      Position [80, 70]
                      Schematic {
                        Component { Type Inductor Name "Inside" Position [40, 10] Parameter { Variable "L" Value "1" } }
                      }
                    }
                    Component {
                      Type CScript
                      Name "Script " "with \\"quotes\\" and }"
                      Position [125, 72]
                      Parameter { Variable "Declarations" Value "int f() { return 1; } // explanatory string" }
                    }
                    // This comment has a stray opening brace: {
                    Component { Type Capacitor Name "Also kept" Position [155, 40] Parameter { Variable "C" Value "1e-6" } }
                    Component { Type FutureBlock Name "Unknown" Position [170, 70] }
                  }
                }
                DemoSignature "sample"
                """;
        var model = convert("\ufeff" + source);
        assertEquals(List.of("Kept", "Also kept"), model.getCircuitComponents().stream().map(ComponentData::getName).toList());
        assertEquals(List.of("UNSUPPORTED_PLECS_SUBSYSTEM", "UNSUPPORTED_PLECS_C_SCRIPT", "UNSUPPORTED_PLECS_COMPONENT"),
                model.getConversionIssues().stream().map(issue -> issue.code()).toList());
        assertEquals("Script with \"quotes\" and }", model.getConversionIssues().get(1).component());
        assertEquals("fixture.plecs:5", model.getConversionIssues().getFirst().sourceLocation());
        assertTrue(model.getConversionIssues().stream().allMatch(issue -> issue.unsupported()));
        assertEquals(model.getConversionIssues(), reopen(reopen(model)).getConversionIssues());
    }

    @Test void reportsInvalidConnectionsWithoutShortingValidNodes() {
        var model = convert("""
                Plecs {
                  Schematic {
                    Component { Type DCVoltageSource Name "V" Position [30,50] Parameter { Variable "V" Value "4" } }
                    Component { Type Resistor Name "R" Position [65,50] Parameter { Variable "R" Value "8" } }
                    Component { Type Ground Name "G1" Position [30,100] }
                    Component { Type Ground Name "G2" Position [65,100] }
                    Connection {
                      Type Wire SrcComponent "V" SrcTerminal 1 Points [48,75]
                      Branch { DstComponent "R" DstTerminal 1 }
                      Branch { DstComponent "R" DstTerminal 7 }
                    }
                    Connection { Type Wire SrcComponent "V" SrcTerminal 2 Points [48,75] DstComponent "G1" DstTerminal 1 }
                    Connection { Type Wire SrcComponent "R" SrcTerminal 2 DstComponent "G2" DstTerminal 1 }
                    Connection { Type Signal SrcComponent "R" SrcTerminal 1 DstComponent "V" DstTerminal 3 }
                  }
                }
                """);
        var source = component(model, "V");
        var resistor = component(model, "R");
        assertEquals(first(source), first(resistor));
        assertNotEquals(first(source), second(source));
        assertEquals("0", second(source));
        assertEquals("0", second(resistor));
        assertEquals(List.of("INVALID_PLECS_TERMINAL", "INVALID_PLECS_TERMINAL", "INVALID_PLECS_TERMINAL"),
                model.getConversionIssues().stream().map(issue -> issue.code()).toList());
    }

    @ParameterizedTest @ValueSource(strings = {
            "<Plecs><Schematic/></Plecs>",
            "Plecs { Schematic { Component { Name \"unfinished } } }",
            "Plecs { Schematic { Component { Position [1, 2 } } }",
            "Plecs { Schematic { }",
            "/* unfinished }",
            "Plecs { Schematic { } } unknown"
    })
    void malformedOrUnknownFormatsReturnAStructuredIssue(String input) throws Exception {
        var model = convert(input);
        assertTrue(model.getCircuitComponents().isEmpty());
        assertEquals("INVALID_PLECS_FORMAT", model.getConversionIssues().getFirst().code());
        assertEquals(model.getConversionIssues(), reopen(model).getConversionIssues());
    }

    @Test void excessiveNestingReturnsAStructuredIssue() {
        var model = convert("Plecs { Schematic { " + "Deep { ".repeat(300) + " }".repeat(302));
        assertEquals("INVALID_PLECS_FORMAT", model.getConversionIssues().getFirst().code());
    }

    @Test void unsupportedExpressionAndParameterAreReportedWithoutInventingValues() throws Exception {
        var model = convert("""
                Plecs {
                  InitializationCommands "Rval = 4;"
                  Schematic {
                    Component { Type Resistor Name "Variable" Position [12, 34] Parameter { Variable "R" Value "Rval" } }
                    Component { Type Resistor Name "Unit suffix" Position [25, 34] Parameter { Variable "R" Value "3k" } }
                    Component { Type Resistor Name "Extra" Position [40, 34]
                      Parameter { Variable "R" Value "10" }
                      Parameter { Variable "ThermalModel" Value "11" }
                    }
                    Component { Type Resistor Name "Kept" Position [55, 34] Parameter { Variable "R" Value "2*pi" } }
                  }
                }
                """);
        assertEquals(1, model.getCircuitComponents().size());
        assertEquals(2 * Math.PI, component(model, "Kept").getNumericParameters()[0]);
        assertEquals(List.of("UNSUPPORTED_PLECS_SETTING", "INVALID_PLECS_COMPONENT", "INVALID_PLECS_COMPONENT",
                "UNSUPPORTED_PLECS_PARAMETER"), model.getConversionIssues().stream().map(issue -> issue.code()).toList());
        assertEquals("ThermalModel", model.getConversionIssues().getLast().parameterPath());
        assertEquals(model.getConversionIssues(), reopen(model).getConversionIssues());
    }

    @Test void cliDispatchesPlecsAndWritesAPartialFileWithJsonDiagnostics() throws Exception {
        Path input = directory.resolve("case.PLECS");
        Path target = directory.resolve("case.ipes");
        Files.writeString(input, "Plecs { Schematic { Component { Type CScript Name \"Custom\" Position [100, 100] } "
                + "Component { Type Resistor Name \"R\" Position [15,30] Parameter { Variable \"R\" Value \"3\" } } } }");
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        assertEquals(2, Main.run(new String[]{input.toString(), target.toString()}, new PrintStream(output), new PrintStream(errors)));
        assertEquals(0, errors.size());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("\"code\":\"UNSUPPORTED_PLECS_C_SCRIPT\""));
        var parsed = new CircuitFileParser().parse(target.toString());
        assertEquals("R", parsed.getCircuitComponents().getFirst().getName());
        assertEquals("Custom", parsed.getConversionIssues().getFirst().component());
    }

    private record Sample(double time, double voltage) { }

    private List<Sample> voltages(CircuitModel model, String target) throws Exception {
        var file = directory.resolve("simulation.ipes");
        new CircuitFileWriter().write(model, file);
        var engine = new HeadlessSimulationEngine();
        var samples = new ArrayList<Sample>();
        engine.setStepTraceListener((time, volts, amps, netlist) -> {
            int index = netlist.getElementIndexByName(target);
            samples.add(new Sample(time, volts[netlist.getNodeX(index)] - volts[netlist.getNodeY(index)]));
        });
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(file.toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return samples;
    }

    private static double average(List<Sample> samples, double from, double to, double step) {
        long first = Math.round(from / step), last = Math.round(to / step);
        var window = samples.stream().filter(sample -> {
            long index = Math.round(sample.time() / step);
            return index >= first && index < last;
        }).mapToDouble(Sample::voltage).toArray();
        assertEquals(last - first, window.length);
        return java.util.Arrays.stream(window).average().orElseThrow();
    }

    private CircuitModel fixture(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/importer/plecs-" + name + ".plecs")) {
            assertNotNull(stream);
            return convert(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static CircuitModel convert(String source) { return new PlecsImporter().convert(source, "fixture.plecs"); }

    private static CircuitModel reopen(CircuitModel model) throws Exception {
        var data = new CircuitFileWriter().write(model);
        return new CircuitFileParser().parse(new ByteArrayInputStream(data), "reopened.ipes");
    }

    private static ComponentData component(CircuitModel model, String name) {
        return model.getCircuitComponents().stream().filter(comp -> comp.getName().equals(name)).findFirst().orElseThrow();
    }

    private static String first(ComponentData data) { return data.getTerminalXLabels()[0]; }
    private static String second(ComponentData data) { return data.getTerminalYLabels()[0]; }

    private static Integer group(CircuitTopology topology, CircuitModel model, String name, Side side) {
        var terminal = new CircuitTopology.TerminalIdentity(topology.identity(component(model, name)), side, 0);
        return topology.terminalGroups().get(terminal);
    }

    private static String divider(String timing, String upperState, String loadPosition) {
        return """
                Plecs {
                  StartTime "0"
                  %s
                  Schematic {
                    Component { Type DCVoltageSource Name "Supply" Position [20,50] Direction up
                      Parameter { Variable "V" Value "12" }
                    }
                    Component { Type Resistor Name "Upper" Position [100,50] Direction up %s
                      Parameter { Variable "R" Value "1000" }
                    }
                    Component { Type Resistor Name "Load" Position [%s] Direction up
                      Parameter { Variable "R" Value "1000" }
                    }
                    Connection { Type Wire SrcComponent "Supply" SrcTerminal 1 DstComponent "Upper" DstTerminal 1 }
                    Connection { Type Wire SrcComponent "Upper" SrcTerminal 2 DstComponent "Load" DstTerminal 1 }
                    Connection { Type Wire SrcComponent "Load" SrcTerminal 2 DstComponent "Supply" DstTerminal 2 }
                  }
                }
                """.formatted(timing, upperState, loadPosition);
    }
}
