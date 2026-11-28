package gecko.core.io;

import gecko.core.ac.AcComponent;
import gecko.core.io.CircuitModel.ComponentData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Resolves saved hierarchy keys against the native component and wire records. */
public final class HierarchyIndex {
    public record Sheet(long id, long parent, ComponentData owner, CircuitHierarchy.Occurrence call,
                        CircuitHierarchy.Definition definition) { }
    public record Boundary(Sheet sheet, CircuitHierarchy.Formal formal, ComponentData terminal) { }

    private final Map<Long, Sheet> sheets = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> signatures = new HashMap<>();
    private final Map<Long, List<String>> paths = new HashMap<>();
    private final List<Boundary> boundaries = new ArrayList<>();

    public HierarchyIndex(CircuitModel model) {
        paths.put(0L, List.of());
        var hierarchy = model.getHierarchy();
        for (var saved : model.getSavedBlocks()) {
            boolean typed = token(saved, HierarchyMetadataCodec.OCCURRENCE) != null
                    || token(saved, HierarchyMetadataCodec.BOUNDARY_LOCATION) != null;
            validateFieldPlacement(saved, typed ? Integer.parseInt(saved.tokens().getOrDefault("typ", "-1")) : -1);
        }
        for (var owner : model.getSpecialComponents()) {
            String token = token(owner.getSavedBlock(), HierarchyMetadataCodec.OCCURRENCE);
            if (token == null) continue;
            if (hierarchy == null || owner.getType() != 27) throw invalid("Missing native sheet owner.");
            long id = owner.getSavedBlock().uniqueObjectIdentifier();
            var call = HierarchyMetadataCodec.decodeOccurrence(token);
            var sheet = new Sheet(id, parent(owner.getSavedBlock()), owner, call, hierarchy.definition(call.definitionId()));
            if (id == 0 || sheets.putIfAbsent(id, sheet) != null) throw invalid("Duplicate native sheet identifier.");
            signatures.put(id, new LinkedHashMap<>());
        }
        if (hierarchy != null && sheets.isEmpty()) throw invalid("Missing hierarchy occurrences.");
        var siblingNames = new HashMap<Long, HashSet<String>>();
        for (var sheet : sheets.values()) {
            if (!siblingNames.computeIfAbsent(sheet.parent(), ignored -> new HashSet<>())
                    .add(sheet.call().instance().toLowerCase(Locale.ROOT))) throw invalid("Duplicate sibling occurrence.");
            path(sheet.id(), new HashSet<>());
        }
        var terminalMembers = new HashMap<Long, Map<String, ComponentData>>();
        var diodeModels = new HashMap<Long, Map<String, AcComponent.IdealDiode>>();
        for (var components : List.of(model.getCircuitComponents(), model.getSpecialComponents(),
                model.getControlComponents(), model.getThermalComponents())) {
            for (var component : components) {
                var saved = component.getSavedBlock();
                validateFieldPlacement(saved, component.getType());
                long parent = parent(saved);
                if (component.getAcComponent() != null && component.getAcComponent().law() instanceof AcComponent.IdealDiode diode) {
                    var previous = diodeModels.computeIfAbsent(parent, ignored -> new HashMap<>())
                            .putIfAbsent(diode.modelName(), diode);
                    if (previous != null && !previous.equals(diode)) {
                        throw invalid("Conflicting ideal diode model " + diode.modelName() + " in " + path(parent));
                    }
                }
                String key = key(saved);
                var signature = signatures.get(parent);
                if (!component.getScalarBindings().isEmpty()) {
                    if (parent != 0 && signature == null || components != model.getCircuitComponents()
                            || component.getAcComponent() != null) {
                        throw invalid("Native scalar bindings require an electrical component.");
                    }
                    ScalarParameterBindings.validateSlots(component.getType(), component.getNumericParameters(),
                            component.getScalarBindings());
                }
                if (token(saved, HierarchyMetadataCodec.BOUNDARY_LOCATION) != null && (signature == null || key == null)) {
                    throw invalid("A boundary field requires a child member association.");
                }
                if (signature == null && key == null) continue;
                if (signature == null || key == null) throw invalid("Missing component member association.");
                String call = token(saved, HierarchyMetadataCodec.OCCURRENCE);
                String definition = call == null ? "" : HierarchyMetadataCodec.decodeOccurrence(call).definitionId();
                var body = Arrays.asList(component.getType(), Arrays.toString(component.getPosition()),
                        component.getOrientation(), saved.enabledState(), Arrays.toString(component.getTerminalXLabels()),
                        Arrays.toString(component.getTerminalYLabels()), component.getAcComponent() == null
                                ? scalarSignature(component)
                                : AcMetadataCodec.encode(component.getAcComponent()), definition);
                if (signature.putIfAbsent(key, body) != null) throw invalid("Duplicate component member " + key);
                if (component.getType() == 29 && components == model.getCircuitComponents()) {
                    terminalMembers.computeIfAbsent(parent, ignored -> new HashMap<>()).put(key, component);
                }
            }
        }
        for (var wire : model.getConnections()) {
            validateFieldPlacement(wire.getSavedBlock(), -1);
            long parent = parent(wire.getSavedBlock());
            String key = key(wire.getSavedBlock());
            var signature = signatures.get(parent);
            if (signature == null && key == null) continue;
            if (signature == null || key == null) throw invalid("Missing wire member association.");
            if (signature.putIfAbsent(key, List.of(wire.getType(), wire.getLabel(), Arrays.deepToString(wire.getPoints()),
                    wire.getSavedBlock().enabledState())) != null) throw invalid("Duplicate wire member " + key);
        }
        var definitionBodies = new HashMap<String, Map<String, Object>>();
        for (var sheet : sheets.values()) {
            var signature = signatures.get(sheet.id());
            var previous = definitionBodies.putIfAbsent(sheet.definition().id(), signature);
            if (previous != null && !previous.equals(signature)) {
                throw invalid("Divergent copies of " + sheet.definition().source() + " at " + path(sheet.id()));
            }
            var terminals = terminalMembers.getOrDefault(sheet.id(), Map.of());
            if (terminals.size() != sheet.definition().formals().size()) {
                throw invalid("Wrong boundary arity for " + path(sheet.id()));
            }
            var externalPins = new HashSet<SpicePorts.Point>();
            for (var formal : sheet.definition().formals()) {
                var terminal = terminals.get(formal.boundaryMemberKey());
                String location = terminal == null ? null : token(terminal.getSavedBlock(), HierarchyMetadataCodec.BOUNDARY_LOCATION);
                if (terminal == null || location == null || !formal.name().equalsIgnoreCase(terminal.getName())) {
                    throw invalid("Missing " + formal.name() + " boundary in " + path(sheet.id()));
                }
                if (!externalPins.add(HierarchyMetadataCodec.decodeBoundaryLocation(location))) {
                    throw invalid("Duplicate parent pin position in " + path(sheet.id()));
                }
                boundaries.add(new Boundary(sheet, formal, terminal));
            }
        }
    }

    public List<Boundary> boundaries() { return List.copyOf(boundaries); }

    private static List<Object> scalarSignature(ComponentData component) {
        double[] values = component.getNumericParameters();
        var bindings = component.getScalarBindings();
        var signature = new java.util.ArrayList<Object>(values.length);
        for (int i = 0; i < values.length; i++) {
            signature.add(bindings.containsKey(i) ? bindings.get(i) : values[i]);
        }
        return signature;
    }
    public Sheet sheet(long id) { return sheets.get(id); }
    public List<String> path(long id) { return paths.getOrDefault(id, List.of()); }

    /** Names an execution row without altering the component's saved local name. */
    public String executionName(ComponentData component) {
        var occurrence = path(parent(component.getSavedBlock()));
        String name = component.getName();
        return occurrence.isEmpty() || name == null || name.isEmpty()
                ? name : String.join(":", occurrence) + ":" + name;
    }

    public Map<Long, Map<String, String>> parameterScopes(Map<String, String> root) {
        var scopes = new LinkedHashMap<Long, Map<String, String>>();
        scopes.put(0L, Map.copyOf(root));
        for (long id : sheets.keySet()) bind(id, scopes);
        return Map.copyOf(scopes);
    }

    private Map<String, String> bind(long id, Map<Long, Map<String, String>> scopes) {
        var scope = scopes.get(id);
        if (scope != null) return scope;
        var sheet = sheets.get(id);
        var caller = bind(sheet.parent(), scopes);
        try {
            scope = sheet.definition().bind(caller, sheet.call());
            scopes.put(id, scope);
            return scope;
        } catch (IllegalArgumentException error) {
            throw invalid(sheet.definition().source() + " " + path(id) + " " + error.getMessage());
        }
    }

    private List<String> path(long id, HashSet<String> active) {
        if (id == 0) return List.of();
        var sheet = sheets.get(id);
        if (sheet == null) throw invalid("Missing parent occurrence " + id);
        if (!active.add(sheet.definition().id())) throw invalid("Recursive definition " + sheet.definition().source());
        var result = new ArrayList<>(path(sheet.parent(), active));
        result.add(sheet.call().instance());
        var immutable = List.copyOf(result);
        paths.put(id, immutable);
        return immutable;
    }

    public static long parent(SavedCircuitBlock saved) { return saved == null ? 0 : saved.parentSheetIdentifier(); }

    public static String key(SavedCircuitBlock saved) {
        String token = token(saved, HierarchyMetadataCodec.MEMBER);
        return token == null ? null : HierarchyMetadataCodec.decodeMember(token).key();
    }

    static String token(SavedCircuitBlock saved, String key) { return saved == null ? null : saved.tokens().get(key); }

    private static void validateFieldPlacement(SavedCircuitBlock saved, int type) {
        if (saved == null) return;
        if (token(saved, HierarchyMetadataCodec.DOCUMENT) != null
                || token(saved, HierarchyMetadataCodec.ROOT_PARAMETERS) != null
                || token(saved, HierarchyMetadataCodec.SCALAR_BINDINGS) != null && !saved.tag().equals("ElementLK")
                || token(saved, HierarchyMetadataCodec.OCCURRENCE) != null && (!saved.tag().equals("ElementSPECIAL") || type != 27)
                || token(saved, HierarchyMetadataCodec.BOUNDARY_LOCATION) != null && (!saved.tag().equals("ElementLK") || type != 29)
                || token(saved, HierarchyMetadataCodec.MEMBER) != null && !List.of("ElementLK", "ElementSPECIAL", "Verbindung").contains(saved.tag())) {
            throw invalid("Unsupported hierarchy field on " + saved.tag());
        }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid circuit hierarchy. " + message);
    }
}
