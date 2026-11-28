package gecko.core.io;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class ParserStructureEvidenceTest {
    static Stream<String> malformedControlBlocks() {
        return Stream.of(
                "<ElementCONTROL>\ntyp 105\nidStringDialog gain\nparameter[] 2\n<\\ElementCONTROL>\n",
                "c\n<ElementCONTROL>\ntyp 105\nidStringDialog gain\nparameter[] 2\n",
                "c\n",
                "<\\ElementCONTROL>\n");
    }

    @ParameterizedTest
    @MethodSource("opaqueTags")
    void savedTextDoesNotCreateSimulationBlocks(String tag) {
        String content = "c\n<ElementCONTROL>\ntyp 105\n<\\ElementCONTROL>\n"
                + "e\n<ElementLK>\ntyp 1\n<\\ElementLK>\n"
                + "x\n<ElementMAGNETIC>\ntyp 999\n<\\ElementMAGNETIC>\nc\n";
        String text = "<" + tag + ">\n" + content + "<\\" + tag + ">\n";
        CircuitModel model = assertDoesNotThrow(() -> new CircuitFileParser().parse(
                new BufferedReader(new StringReader(text)), "saved-text.ipes"));
        assertTrue(model.getImportIssues().isEmpty());
        assertTrue(model.getCircuitComponents().isEmpty());
        assertTrue(model.getControlComponents().isEmpty());
        if (tag.equals("scripterCode")) {
            assertEquals(content.trim(), model.getScripterCode());
        }
    }

    static Stream<String> opaqueTags() {
        return Stream.of("scripterCode", "scripterImports", "scripterDeclarations", "sourceCode",
                "staticCode", "importCode", "staticVariables", "GeckoFileManager");
    }

    @ParameterizedTest
    @MethodSource("malformedControlBlocks")
    void malformedControlStructureRemainsInspectableWithAParsingDiagnostic(String text) {
        CircuitModel model = assertDoesNotThrow(() -> new CircuitFileParser().parse(
                new BufferedReader(new StringReader(text)), "structure.ipes"));
        assertTrue(model.getImportIssues().stream().anyMatch(issue ->
                issue.code().equals("MALFORMED_SIMULATION_BLOCK") && issue.domain().equals("CONTROL")
                        && !issue.unsupported() && issue.sourceLocation().contains("structure.ipes")));
    }
}
