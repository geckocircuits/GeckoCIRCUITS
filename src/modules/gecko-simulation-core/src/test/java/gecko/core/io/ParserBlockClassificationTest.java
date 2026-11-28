package gecko.core.io;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ParserBlockClassificationTest {
    @ParameterizedTest
    @CsvSource({"x,ElementMAGNETIC,999,1", "sp,ElementSPECIAL,27,1",
                "sp,ElementSPECIAL,999,1", "sp,ElementSPECIAL,70,0"})
    void classifiesUnknownSimulationAndSpecialContent(String marker, String tag, int type, int issueCount)
            throws Exception {
        String text = marker + "\n<" + tag + ">\ntyp " + type
                + "\nidStringDialog block.1\n<\\" + tag + ">\n";
        CircuitModel model = new CircuitFileParser().parse(
                new BufferedReader(new StringReader(text)), "classified.ipes");
        assertEquals(issueCount, model.getImportIssues().size());
        if (issueCount > 0) {
            ImportIssue issue = model.getImportIssues().getFirst();
            assertEquals("UNSUPPORTED_SIMULATION_BLOCK", issue.code());
            assertEquals("block.1", issue.component());
            assertTrue(issue.unsupported());
            assertTrue(issue.sourceLocation().contains("classified.ipes"));
        }
    }
}
