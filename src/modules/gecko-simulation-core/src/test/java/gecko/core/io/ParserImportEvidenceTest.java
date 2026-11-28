package gecko.core.io;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParserImportEvidenceTest {
    private CircuitModel parse(String text) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "evidence.ipes");
    }

    @Test
    void electricalInputRetainsRawValuesAndExplicitZeroTokens() throws Exception {
        CircuitModel model = parse("""
                e
                <ElementLK>
                typ 4
                idStringDialog V.1
                parameter[] 402 6 50 2 1.5707963267948966
                amplitude 0
                phaseDegrees 0
                amplitude$ParameterName peak
                nameOpt[] /old peak/old frequency
                version170 true
                useExternal false
                <\\ElementLK>
                """);
        CircuitModel.ComponentData source = model.getCircuitComponents().getFirst();
        assertArrayEquals(new double[]{402, 6, 50, 2, Math.PI / 2}, source.getNumericParameters());
        assertEquals(5, source.getImportedParameterCount());
        assertTrue(source.isParsedInput());
        assertFalse(source.isConfigurationNormalized());
        assertEquals("0", source.getImportTokens().get("amplitude"));
        assertEquals("0", source.getImportTokens().get("phaseDegrees"));
        assertEquals("peak", source.getImportTokens().get("amplitude$ParameterName"));
        assertEquals("/old peak/old frequency", source.getImportTokens().get("nameOpt[]"));
        assertEquals("true", source.getImportTokens().get("version170"));
        assertEquals("false", source.getImportTokens().get("useExternal"));
        assertTrue(source.getSourceLocation().contains("evidence.ipes"));
    }
}
