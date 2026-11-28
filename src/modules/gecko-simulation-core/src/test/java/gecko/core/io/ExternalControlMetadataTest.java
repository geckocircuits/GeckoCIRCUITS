package gecko.core.io;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExternalControlMetadataTest {
    @Test
    void retainsToExternalWidthWhenSavedInputLabelsAreAbsent() throws Exception {
        String text = "c\n<ElementCONTROL>\ntyp 22\nidStringDialog ToEXT.3\ntorder 2\n"
                + "anzXIN 3\nknotenNrXIN[] 0 1 2\n<"
                + (char) 92 + "ElementCONTROL>\n";
        var model = parse(text);
        var component = model.getControlComponents().getFirst();
        assertEquals(0, component.getTerminalXLabels().length);
        assertEquals(3, component.getParameters().get("externalWidth"));
        var descriptor = gecko.core.simulation.ControlBlockDescriptor.find(22);
        assertEquals(3, descriptor.inputs(component));
        assertEquals(3, descriptor.createCalculator(component, model)._inputSignal.length);
    }

    @Test
    void retainsSavedTerminalWidthAndExternalOrder() throws Exception {
        String text = "c\n<ElementCONTROL>\ntyp 23\nidStringDialog from\ntn 2\ntorder 7\n"
                + "labelAnfangsKnoten[] /\nlabelEndKnoten[] /a/b\n<" + (char) 92 + "ElementCONTROL>\n";
        var component = parse(text).getControlComponents().getFirst();
        assertEquals(2, component.getParameters().get("externalWidth"));
        assertEquals(7, component.getParameters().get("externalOrder"));
    }

    @Test
    void paddedToExternalLabelsKeepDataWidthAndLimitTopologyToActiveTerminals() throws Exception {
        var model = new CircuitFileParser().parse(fixturePath().toString());
        var toExternal = model.getControlComponents().stream()
                .filter(component -> component.getType() == 22 && component.getName().equals("ToEXT.4"))
                .findFirst().orElseThrow();
        var descriptor = gecko.core.simulation.ControlBlockDescriptor.find(22);

        assertEquals(28, descriptor.inputs(toExternal));
        assertEquals(28, descriptor.createCalculator(toExternal, model)._inputSignal.length);
        assertEquals(28, toExternal.getTerminalXLabels().length);
        assertEquals(3, toExternal.getParameters().get("activeInputWidth"));

        var topology = CircuitTopology.resolve(model);
        for (int port = 0; port < 3; port++) {
            assertNotNull(topology.group(toExternal, CircuitTopology.Side.INPUT, port), "active input " + port);
        }
        for (int port = 3; port < 28; port++) {
            assertNull(topology.group(toExternal, CircuitTopology.Side.INPUT, port), "inactive input " + port);
        }

        var readiness = new gecko.core.preparation.CircuitPreparation()
                .prepare(new gecko.core.preparation.CircuitPreparation().capture(model),
                        gecko.core.preparation.PreparationRequest.defaults()).report();
        assertFalse(readiness.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.component() != null && diagnostic.component().equals(toExternal.getName())
                        && (diagnostic.code().equals("INVALID_CONTROL_PORT")
                                || diagnostic.code().equals("MISSING_CONTROL_INPUT")
                                        && inactiveInputPort(diagnostic.parameterPath(), 3))));
    }

    private static boolean inactiveInputPort(String path, int firstInactivePort) {
        if (path == null || !path.matches(".*\\.input\\[\\d+\\]")) return false;
        int start = path.lastIndexOf('[') + 1;
        int end = path.length() - 1;
        return Integer.parseInt(path.substring(start, end)) >= firstInactivePort;
    }

    private static CircuitModel parse(String text) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "external.ipes");
    }

    private static Path fixturePath() {
        Path relative = Path.of("resources/tutorials/7xx_scripting_automation/"
                + "703_simulink_cosimulation/vr1_simulink.ipes");
        for (Path root = Path.of("").toAbsolutePath(); root != null; root = root.getParent()) {
            Path candidate = root.resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        throw new AssertionError("Simulink fixture was not found from the test working directory");
    }
}
