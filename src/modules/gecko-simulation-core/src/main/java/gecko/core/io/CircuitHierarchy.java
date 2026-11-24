package gecko.core.io;

import gecko.core.ac.ParameterDefinition;
import gecko.core.io.importer.SpiceValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Definition metadata of a native circuit document. */
public record CircuitHierarchy(List<Definition> definitions) {
    public CircuitHierarchy {
        definitions = List.copyOf(definitions);
        var keys = new java.util.HashSet<String>();
        for (Definition definition : definitions) {
            if (!keys.add(definition.id())) throw new IllegalArgumentException("Duplicate definition " + definition.id());
        }
        if (definitions.isEmpty()) throw new IllegalArgumentException("Add a hierarchy definition.");
    }

    public Definition definition(String id) {
        return definitions.stream().filter(definition -> definition.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown definition " + id));
    }

    public record Definition(String id, String source, List<Formal> formals, List<ParameterDefinition> defaults) {
        public Definition {
            requireText(id);
            requireText(source);
            formals = List.copyOf(formals);
            defaults = List.copyOf(defaults);
            parameters(defaults);
            var names = new java.util.HashSet<String>();
            var keys = new java.util.HashSet<String>();
            for (int i = 0; i < formals.size(); i++) {
                var formal = formals.get(i);
                if (formal.order() != i + 1 || !names.add(formal.name().toLowerCase(Locale.ROOT))
                        || !keys.add(formal.boundaryMemberKey())) {
                    throw new IllegalArgumentException("Invalid formal order or name in " + source);
                }
            }
            if (formals.isEmpty()) throw new IllegalArgumentException("Add a formal port in " + source);
        }

        public Map<String, String> bind(Map<String, String> caller, Occurrence occurrence) {
            var scope = parameters(defaults);
            for (var override : occurrence.overrides()) {
                String name = override.name().toLowerCase(Locale.ROOT);
                if (!scope.containsKey(name)) {
                    throw new IllegalArgumentException("Unknown override " + override.name() + " in " + source);
                }
                scope.put(name, Double.toString(SpiceValue.evaluate(override.expression(), caller)));
            }
            scope.values().forEach(expression -> SpiceValue.evaluate(expression, scope));
            return Map.copyOf(scope);
        }
    }

    public record Formal(int order, String name, String boundaryMemberKey) {
        public Formal {
            requireText(name);
            requireText(boundaryMemberKey);
        }
    }

    /** Call metadata on the native block that owns the child sheet. */
    public record Occurrence(String definitionId, String instance, List<ParameterDefinition> overrides) {
        public Occurrence {
            requireText(definitionId);
            requireText(instance);
            overrides = List.copyOf(overrides);
            parameters(overrides);
        }
    }

    /** Stable metadata on a native sheet member. */
    public record Member(String key) {
        public Member { requireText(key); }
    }

    private static LinkedHashMap<String, String> parameters(List<ParameterDefinition> parameters) {
        var map = new LinkedHashMap<String, String>();
        for (var parameter : parameters) {
            if (map.putIfAbsent(parameter.name().toLowerCase(Locale.ROOT), parameter.expression()) != null) {
                throw new IllegalArgumentException("Duplicate parameter " + parameter.name());
            }
        }
        return map;
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Require a hierarchy identifier.");
    }
}
