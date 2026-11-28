package gecko.core.io;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParserImportDiagnosticTest {
    private CircuitModel parse(String text) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "diagnostics.ipes");
    }

    @Test
    void missingControlTypeProducesAStableDiagnostic() throws Exception {
        CircuitModel model = parse("""
                c
                <ElementCONTROL>
                idStringDialog broken gain
                parameter[] 2
                <\\ElementCONTROL>
                """);
        assertEquals(1, model.getImportIssues().size());
        ImportIssue issue = model.getImportIssues().getFirst();
        assertEquals("MALFORMED_COMPONENT", issue.code());
        assertEquals("CONTROL", issue.domain());
        assertEquals("broken gain", issue.component());
        assertFalse(issue.unsupported());
        assertTrue(issue.sourceLocation().contains("diagnostics.ipes"));
    }
}
