package gecko.core.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ParserFormatCompatibilityTest {
    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void plainAndGzipInputsRetainEquivalentWhitespaceAndSlashNumericLayouts(boolean gzip, boolean slash)
            throws Exception {
        String values = slash ? "/10/2/0" : "10 2 0";
        String text = "e\n<ElementLK>\ntyp 1\nidStringDialog R.1\nx 0\ny 0\norientierung 0\n"
                + "parameter[] " + values + "\nlabelAnfangsKnoten[] /n\nlabelEndKnoten[] /0\n"
                + "parameterString[] /saved data\n<\\ElementLK>\n";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (gzip) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream stream = new GZIPOutputStream(compressed)) {
                stream.write(bytes);
            }
            bytes = compressed.toByteArray();
        }
        CircuitModel model = new CircuitFileParser().parse(new ByteArrayInputStream(bytes), "format.ipes");
        CircuitModel.ComponentData resistor = model.getCircuitComponents().getFirst();
        assertArrayEquals(new double[]{10, 2, 0}, resistor.getNumericParameters());
        assertEquals(3, resistor.getImportedParameterCount());
        assertEquals(10.0, resistor.getParameters().get("param0"));
        assertEquals(10.0, resistor.getParameters().get("resistance"));
        assertEquals(values, resistor.getImportTokens().get("parameter[]"));
        assertTrue(model.getImportIssues().isEmpty());
    }
}