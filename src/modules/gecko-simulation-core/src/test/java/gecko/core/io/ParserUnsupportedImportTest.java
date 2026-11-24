package gecko.core.io;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParserUnsupportedImportTest {
    private CircuitModel parse(String text) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "unsupported.ipes");
    }

    @Test
    void savedThermalElementRemainsInspectableWithUnsupportedEvidence() throws Exception {
        CircuitModel model = parse("""
                eTH
                <ElementTHERM>
                typ 101
                idStringDialog Heat.1
                x 3
                y 4
                orientierung 503
                parameter[] 10 20
                labelAnfangsKnoten[] /hot
                labelEndKnoten[] /ambient
                parameterString[] /thermal model
                nonlinear true
                <\\ElementTHERM>
                """);
        assertEquals(1, model.getThermalComponents().size());
        CircuitModel.ComponentData thermal = model.getThermalComponents().getFirst();
        assertEquals("Heat.1", thermal.getName());
        assertEquals(101, thermal.getType());
        assertArrayEquals(new double[]{10, 20}, thermal.getNumericParameters());
        assertArrayEquals(new String[]{"hot"}, thermal.getTerminalXLabels());
        assertEquals("true", thermal.getImportTokens().get("nonlinear"));
        assertTrue(thermal.isParsedInput());
        // The parser only reads the block. Readiness decides which thermal types the core can run (ADR-0023).
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
    }
}
