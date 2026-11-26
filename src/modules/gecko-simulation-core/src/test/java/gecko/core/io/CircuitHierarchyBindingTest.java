package gecko.core.io;

import gecko.core.ac.ParameterDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitHierarchyBindingTest {
    private static final List<CircuitHierarchy.Formal> PINS = List.of(new CircuitHierarchy.Formal(1, "NodeOnly", "pin"));
    private static final List<CircuitHierarchy.RequiredInput> INPUTS = List.of(
            new CircuitHierarchy.RequiredInput("VHIGH",
                    List.of(new CircuitHierarchy.ScalarUse("leaf.asc", "V2", "Value", 42))),
            new CircuitHierarchy.RequiredInput("VLOW",
                    List.of(new CircuitHierarchy.ScalarUse("leaf.asc", "V1", "Value", 17))));

    @Test void resolvesEveryAssignmentInItsParentBeforeBindingChildDefaults() {
        var definition = definition(parameter("alias", "{VHIGH+offset}"), parameter("offset", "1"), parameter("number", "40"));
        var caller = Map.of("VHIGH", "12", "VLOW", "-5", "ParentAlias", "{number}", "number", "1");
        var call = List.of(parameter("Vhigh", "{VHIGH}"), parameter("Vlow", "{VhIgH+VLOW+ParentAlias-1}"));
        var expected = Map.of("vhigh", "12.0", "vlow", "7.0", "alias", "{VHIGH+offset}", "offset", "1", "number", "40");
        assertEquals(expected, definition.bind(caller, occurrence(call)));
        assertEquals(expected, definition.bind(caller, occurrence(call.reversed())));
        assertEquals("13.0", definition.bind(expected,
                occurrence(List.of(parameter("VHIGH", "{alias}"), parameter("VLOW", "0")))).get("vhigh"));
        assertTrue(assertThrows(IllegalArgumentException.class, () ->
                definition(parameter("alias", "{ParentAlias}")).bind(caller, occurrence(call)))
                .getMessage().contains("Unknown scalar parameter: parentalias"));
    }

    @Test void rejectsInvalidCallsWithoutChangingSiblingBindings() {
        var definition = definition(parameter("defaultValue", "3"));
        var caller = Map.of("VHIGH", "12", "VLOW", "-5", "Foo", "3", "NodeOnly", "1", "ModelOnly", "2");
        var valid = List.of(parameter("vhigh", "{VHIGH}"), parameter("vlow", "{VLOW}"));
        var expected = definition.bind(caller, occurrence(valid));
        for (String unknown : List.of("Vhihg", "Foo", "NodeOnly", "ModelOnly")) {
            var overrides = List.of(parameter("vhigh", "{undefined}"), valid.getLast(), parameter(unknown, "1"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> definition.bind(caller, occurrence(overrides)))
                    .getMessage().contains("Unknown override " + unknown));
        }
        assertTrue(assertThrows(IllegalArgumentException.class, () ->
                definition.bind(caller, occurrence(List.of(valid.getLast())))).getMessage()
                .contains("Missing required input VHIGH at leaf.asc:42 V2 Value"));
        assertTrue(assertThrows(IllegalArgumentException.class, () ->
                definition.bind(caller, occurrence(List.of(parameter("Vhigh", "{unknown}"), valid.getLast()))))
                .getMessage().contains("Unknown scalar parameter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () ->
                occurrence(List.of(valid.getFirst(), parameter("VHIGH", "2"), valid.getLast())))
                .getMessage().contains("Duplicate parameter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () ->
                definition(parameter("a", "{b}"), parameter("b", "{a}")).bind(caller, occurrence(valid)))
                .getMessage().contains("Circular scalar parameter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () ->
                definition(parameter("a", "{1/0}")).bind(caller, occurrence(valid)))
                .getMessage().contains("nonfinite"));
        assertEquals(expected, definition.bind(caller, occurrence(valid)));
    }

    @Test void keepsLegacyDocumentBoundariesAndVersion() {
        var legacy = new CircuitHierarchy.Definition("old", "old.asc", PINS, List.of(parameter("R", "2k")));
        var second = new CircuitHierarchy.Definition("other", "other.asc", PINS, List.of());
        var oldDocument = new CircuitHierarchy(List.of(legacy, second));
        String oldEncoding = HierarchyMetadataCodec.encode(oldDocument);
        assertTrue(oldEncoding.startsWith("1."));
        assertEquals(oldDocument, HierarchyMetadataCodec.decodeDocument(oldEncoding));
        assertEquals(Map.of("r", "500.0"), legacy.bind(Map.of("base", "500"),
                new CircuitHierarchy.Occurrence("old", "Xold", List.of(parameter("r", "{base}")))));
        var newDocument = new CircuitHierarchy(List.of(second, definition(), legacy));
        String newEncoding = HierarchyMetadataCodec.encode(newDocument);
        assertTrue(newEncoding.startsWith("2."));
        assertEquals(newDocument, HierarchyMetadataCodec.decodeDocument(newEncoding));
        assertThrows(IllegalArgumentException.class, () ->
                HierarchyMetadataCodec.decodeDocument(newEncoding.substring(0, newEncoding.lastIndexOf('.'))));
    }

    private static CircuitHierarchy.Definition definition(ParameterDefinition... defaults) {
        return new CircuitHierarchy.Definition("leaf", "leaf.asc", PINS, List.of(defaults), INPUTS);
    }

    private static CircuitHierarchy.Occurrence occurrence(List<ParameterDefinition> overrides) {
        return new CircuitHierarchy.Occurrence("leaf", "Xleaf", overrides);
    }

    private static ParameterDefinition parameter(String name, String expression) {
        return new ParameterDefinition(name, expression);
    }
}
