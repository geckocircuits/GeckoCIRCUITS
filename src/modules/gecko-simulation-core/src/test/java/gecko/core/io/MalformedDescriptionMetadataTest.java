package gecko.core.io;

import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MalformedDescriptionMetadataTest {
    @Test
    void malformedControlIdentifierRetainsRawInputAndProducesAnInspectableDiagnostic() throws Exception {
        String text = """
                c
                <ElementCONTROL>
                typ 3
                idStringDialog constant
                uniqueObjectIdentifier abc
                parameter[] 2
                labelEndKnoten[] /drive
                <\\ElementCONTROL>
                dt 0.1
                tDURATION 0.2
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "malformed-uid.ipes");
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var saved = snapshot.toModel().getControlComponents().getFirst().getSavedBlock();
        assertEquals("abc", saved.tokens().get("uniqueObjectIdentifier"));
        assertTrue(saved.serializedText().contains("uniqueObjectIdentifier abc"));
        assertEquals(0, saved.uniqueObjectIdentifier(), "An invalid saved identifier cannot become a valid identity");
        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertTrue(result.report().diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("MALFORMED_COMPONENT") && "CONTROL".equals(diagnostic.domain())
                        && "constant".equals(diagnostic.component()) && diagnostic.sourceLocation().contains("malformed-uid.ipes")));
    }
}
