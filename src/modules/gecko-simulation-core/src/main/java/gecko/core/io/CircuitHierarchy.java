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

    public record Definition(String id, String source, List<Formal> formals, List<ParameterDefinition> defaults,
                             List<RequiredInput> requiredInputs) {
        public Definition(String id, String source, List<Formal> formals, List<ParameterDefinition> defaults) {
            this(id, source, formals, defaults, List.of());
        }

        public Definition {
            requireText(id);
            requireText(source);
            formals = List.copyOf(formals);
            defaults = List.copyOf(defaults);
            requiredInputs = List.copyOf(requiredInputs);
            var scalarNames = new java.util.HashSet<>(parameters(defaults).keySet());
            for (var input : requiredInputs) {
                if (!scalarNames.add(input.name().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Duplicate scalar input " + input.name() + " in " + source);
                }
            }
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
            var explicit = parameters(occurrence.overrides());
            var allowed = new java.util.HashSet<>(scope.keySet());
            requiredInputs.forEach(input -> allowed.add(input.name().toLowerCase(Locale.ROOT)));
            for (var override : occurrence.overrides()) {
                String name = override.name().toLowerCase(Locale.ROOT);
                if (!allowed.contains(name)) {
                    throw new IllegalArgumentException("Unknown override " + override.name() + " in " + source);
                }
            }
            for (var input : requiredInputs) {
                if (!explicit.containsKey(input.name().toLowerCase(Locale.ROOT))) {
                    var use = input.uses().getFirst();
                    throw new IllegalArgumentException("Missing required input " + input.name() + " at "
                            + use.source() + ":" + use.line() + " " + use.member() + " " + use.attribute());
                }
            }
            explicit.replaceAll((name, expression) -> Double.toString(SpiceValue.evaluate(expression, caller)));
            scope.putAll(explicit);
            scope.values().forEach(expression -> SpiceValue.evaluate(expression, scope));
            return Map.copyOf(scope);
        }
    }

    public record RequiredInput(String name, List<ScalarUse> uses) {
        public RequiredInput {
            if (name == null || !name.matches("[A-Za-z_][A-Za-z_0-9]*")) {
                throw new IllegalArgumentException("Require a valid scalar input name.");
            }
            uses = List.copyOf(uses);
            if (uses.isEmpty()) throw new IllegalArgumentException("Require a scalar input source use.");
        }
    }

    public record ScalarUse(String source, String member, String attribute, int line) {
        public ScalarUse {
            requireText(source);
            requireText(member);
            requireText(attribute);
            if (line < 1) throw new IllegalArgumentException("Require a scalar input source line.");
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

    public static LinkedHashMap<String, String> parameters(List<ParameterDefinition> parameters) {
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
