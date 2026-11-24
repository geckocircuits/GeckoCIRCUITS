package gecko.core.io;

import gecko.core.ac.ParameterDefinition;
import java.util.ArrayList;
import java.util.List;

/** Versioned hierarchy fields shared by both IPES entry points. */
public final class HierarchyMetadataCodec {
    public static final String DOCUMENT = "geckoHierarchy";
    public static final String MEMBER = "geckoHierarchyMember";
    public static final String OCCURRENCE = "geckoHierarchyOccurrence";
    public static final String BOUNDARY_LOCATION = "geckoHierarchyBoundaryLocation";

    private HierarchyMetadataCodec() { }

    public static String encode(CircuitHierarchy document) {
        var fields = new ArrayList<String>();
        for (var definition : document.definitions()) {
            fields.addAll(List.of(definition.id(), definition.source(), Integer.toString(definition.formals().size())));
            for (var formal : definition.formals()) {
                fields.addAll(List.of(Integer.toString(formal.order()), formal.name(), formal.boundaryMemberKey()));
            }
            appendParameters(fields, definition.defaults());
        }
        return AcMetadataCodec.encode(fields);
    }

    public static CircuitHierarchy decodeDocument(String text) {
        var fields = new Fields(text);
        var definitions = new ArrayList<CircuitHierarchy.Definition>();
        while (fields.remaining() > 0) {
            String id = fields.next(), source = fields.next();
            int count = fields.count(3);
            var formals = new ArrayList<CircuitHierarchy.Formal>();
            for (int i = 0; i < count; i++) {
                formals.add(new CircuitHierarchy.Formal(Integer.parseInt(fields.next()), fields.next(), fields.next()));
            }
            definitions.add(new CircuitHierarchy.Definition(id, source, formals, fields.parameters()));
        }
        return new CircuitHierarchy(definitions);
    }

    public static String encode(CircuitHierarchy.Occurrence occurrence) {
        var fields = new ArrayList<>(List.of(occurrence.definitionId(), occurrence.instance()));
        appendParameters(fields, occurrence.overrides());
        return AcMetadataCodec.encode(fields);
    }

    public static CircuitHierarchy.Occurrence decodeOccurrence(String text) {
        var fields = new Fields(text);
        var occurrence = new CircuitHierarchy.Occurrence(fields.next(), fields.next(), fields.parameters());
        fields.requireEnd();
        return occurrence;
    }

    public static String encode(CircuitHierarchy.Member member) {
        return AcMetadataCodec.encode(List.of(member.key()));
    }

    public static CircuitHierarchy.Member decodeMember(String text) {
        var fields = new Fields(text);
        var member = new CircuitHierarchy.Member(fields.next());
        fields.requireEnd();
        return member;
    }

    public static String encodeBoundaryLocation(int x, int y) {
        return AcMetadataCodec.encode(List.of(Integer.toString(x), Integer.toString(y)));
    }

    public static SpicePorts.Point decodeBoundaryLocation(String text) {
        var fields = new Fields(text);
        var point = new SpicePorts.Point(Integer.parseInt(fields.next()), Integer.parseInt(fields.next()));
        fields.requireEnd();
        return point;
    }

    private static void appendParameters(List<String> fields, List<ParameterDefinition> parameters) {
        fields.add(Integer.toString(parameters.size()));
        for (var parameter : parameters) fields.addAll(List.of(parameter.name(), parameter.expression()));
    }

    private static final class Fields {
        private final String[] fields;
        private int offset;

        Fields(String encoded) { fields = AcMetadataCodec.decode(encoded); }
        int remaining() { return fields.length - offset; }

        String next() {
            if (remaining() == 0) throw invalid();
            return fields[offset++];
        }

        int count(int width) {
            int count = Integer.parseInt(next());
            if (count < 0 || count > remaining() / width) throw invalid();
            return count;
        }

        List<ParameterDefinition> parameters() {
            int count = count(2);
            var result = new ArrayList<ParameterDefinition>(count);
            for (int i = 0; i < count; i++) result.add(new ParameterDefinition(next(), next()));
            return result;
        }

        void requireEnd() {
            if (remaining() != 0) throw invalid();
        }

        private static IllegalArgumentException invalid() {
            return new IllegalArgumentException("Invalid versioned hierarchy field.");
        }
    }
}
