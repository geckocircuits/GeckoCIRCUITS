package gecko.core.io;

import gecko.core.ac.AcComponent;
import gecko.core.ac.AcInputOrigin;
import gecko.core.ac.AcDocumentSettings;
import gecko.core.ac.AcSweep;
import gecko.core.ac.ParameterDefinition;
import gecko.core.io.importer.SpiceValue;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Versioned IPES fields shared by desktop and headless readers and writers. */
public final class AcMetadataCodec {
    public static final String SETTINGS = "geckoAnalysis";
    public static final String COMPONENT = "geckoAcComponent";
    public static final String PROVENANCE = "geckoAcProvenance";
    private static final String VERSION = "1";

    private AcMetadataCodec() { }

    public static String encode(AcDocumentSettings settings) {
        List<String> fields = new ArrayList<>(List.of("AC_DEC", settings.analysisId(),
                Integer.toString(settings.sweep().pointsPerDecade()), Double.toString(settings.sweep().startHz()),
                Double.toString(settings.sweep().stopHz())));
        for (var parameter : settings.parameters()) {
            fields.add(parameter.name());
            fields.add(parameter.expression());
        }
        return encode(fields);
    }

    public static AcDocumentSettings decodeSettings(String text) {
        String[] fields = decode(text);
        require(fields.length >= 5 && (fields.length - 5) % 2 == 0);
        if (!fields[0].equals("AC_DEC")) throw new IllegalArgumentException("Unsupported IPES analysis: " + fields[0]);
        var parameters = new ArrayList<ParameterDefinition>();
        for (int i = 5; i < fields.length; i += 2) parameters.add(new ParameterDefinition(fields[i], fields[i + 1]));
        return new AcDocumentSettings(new AcSweep(Integer.parseInt(fields[2]), Double.parseDouble(fields[3]),
                Double.parseDouble(fields[4])), parameters, fields[1]);
    }

    public static String encode(AcComponent component) {
        var fields = new ArrayList<>(List.of(component.instance(), component.mirrored() ? "1" : "0",
                component.spiceOrders().stream().map(Object::toString).collect(java.util.stream.Collectors.joining(","))));
        switch (component.law()) {
            case AcComponent.Passive law -> fields.addAll(List.of("PASSIVE", law.expression()));
            case AcComponent.Inductance law -> {
                if (law.origin() == AcInputOrigin.LEGACY) {
                    var resistance = (AcComponent.ExplicitResistance) law.resistance();
                    fields.addAll(List.of("INDUCTANCE", law.expression(), resistance.expression()));
                } else {
                    boolean explicit = law.resistance() instanceof AcComponent.ExplicitResistance;
                    fields.addAll(List.of("INDUCTANCE_ORIGIN_V1", law.expression(), law.origin().name(),
                            law.origin().profileId(), law.origin().configurationSha256(), explicit ? "EXPLICIT" : "DEFAULT",
                            explicit ? ((AcComponent.ExplicitResistance) law.resistance()).expression() : ""));
                }
            }
            case AcComponent.Voltage law -> fields.addAll(List.of("VOLTAGE", law.dc(), law.waveform(),
                    law.magnitude(), law.phaseDegrees()));
            case AcComponent.Current law -> fields.addAll(List.of("CURRENT", law.dc(), law.magnitude(), law.phaseDegrees()));
            case AcComponent.Controlled law -> fields.addAll(List.of(law.laplace() ? "LAPLACE" : "GAIN", law.expression()));
        }
        fields.addAll(component.aliases());
        return encode(fields);
    }

    public static AcComponent decodeComponent(String text) {
        String[] fields = decode(text);
        require(fields.length >= 5 && (fields[1].equals("1") || fields[1].equals("0")));
        int aliasIndex = switch (fields[3]) {
            case "VOLTAGE" -> 8;
            case "CURRENT" -> 7;
            case "INDUCTANCE" -> 6;
            case "INDUCTANCE_ORIGIN_V1" -> 10;
            default -> 5;
        };
        require(fields.length >= aliasIndex);
        List<Integer> orders = java.util.Arrays.stream(fields[2].split(",", -1)).map(Integer::valueOf).toList();
        AcComponent.Law law = switch (fields[3]) {
            case "PASSIVE" -> new AcComponent.Passive(fields[4]);
            case "INDUCTANCE" -> new AcComponent.Inductance(fields[4], fields[5]);
            case "INDUCTANCE_ORIGIN_V1" -> {
                require(fields.length == aliasIndex + orders.size());
                var origin = AcInputOrigin.decode(fields[5], fields[6], fields[7]);
                require(origin != AcInputOrigin.LEGACY);
                AcComponent.Resistance resistance = switch (fields[8]) {
                    case "EXPLICIT" -> new AcComponent.ExplicitResistance(fields[9]);
                    case "DEFAULT" -> {
                        require(fields[9].isEmpty());
                        yield new AcComponent.ImportedDefault();
                    }
                    default -> throw new IllegalArgumentException("Unsupported AC resistance setting.");
                };
                yield new AcComponent.Inductance(fields[4], resistance, origin);
            }
            case "VOLTAGE" -> new AcComponent.Voltage(fields[4], fields[5], fields[6], fields[7]);
            case "CURRENT" -> new AcComponent.Current(fields[4], fields[5], fields[6]);
            case "GAIN", "LAPLACE" -> new AcComponent.Controlled(fields[4], fields[3].equals("LAPLACE"));
            default -> throw new IllegalArgumentException("Unsupported IPES component law: " + fields[3]);
        };
        return new AcComponent(fields[0], orders, List.of(fields).subList(aliasIndex, fields.length), fields[1].equals("1"), law);
    }

    public static String encode(AcProvenance provenance) {
        var fields = new ArrayList<>(List.of(provenance.sourceSha256(), provenance.importRevision(),
                provenance.importSourceDirty() ? "1" : "0", provenance.importSourceSnapshotSha256(),
                provenance.importRuntimeJarSha256()));
        if (provenance.origin().tested()) {
            fields.addFirst("ASC_ORIGIN_V1");
            fields.addAll(List.of(provenance.origin().name(), provenance.origin().profileId(),
                    provenance.origin().configurationSha256(), Integer.toString(provenance.dependencySha256().size()),
                    Integer.toString(provenance.definitionSourceSha256().size())));
        }
        provenance.dependencySha256().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            fields.add(entry.getKey());
            fields.add(entry.getValue());
        });
        provenance.definitionSourceSha256().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            fields.add(entry.getKey());
            fields.add(entry.getValue());
        });
        provenance.memberSha256().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            fields.add(entry.getKey().toString());
            fields.add(entry.getValue());
        });
        return encode(fields);
    }

    public static AcProvenance decodeProvenance(String text) {
        String[] fields = decode(text);
        if (fields.length > 0 && fields[0].equals("ASC_ORIGIN_V1")) {
            require(fields.length >= 11 && fields.length % 2 == 1 && (fields[3].equals("0") || fields[3].equals("1")));
            var origin = AcInputOrigin.decode(fields[6], fields[7], fields[8]);
            require(origin.tested());
            int dependencyCount = Integer.parseInt(fields[9]);
            int definitionCount = Integer.parseInt(fields[10]);
            require(dependencyCount >= 0 && definitionCount >= 0
                    && (long) dependencyCount + definitionCount <= (fields.length - 11) / 2);
            int definitionStart = 11 + 2 * dependencyCount;
            int memberStart = definitionStart + 2 * definitionCount;
            var dependencies = new LinkedHashMap<String, String>();
            var definitions = new LinkedHashMap<String, String>();
            var members = new LinkedHashMap<Long, String>();
            for (int i = 11; i < definitionStart; i += 2) require(dependencies.putIfAbsent(fields[i], fields[i + 1]) == null);
            for (int i = definitionStart; i < memberStart; i += 2) require(definitions.putIfAbsent(fields[i], fields[i + 1]) == null);
            for (int i = memberStart; i < fields.length; i += 2) {
                require(members.putIfAbsent(Long.parseLong(fields[i]), fields[i + 1]) == null);
            }
            return new AcProvenance(fields[1], fields[2], fields[3].equals("1"), fields[4], fields[5],
                    dependencies, origin, definitions, members);
        }
        require(fields.length >= 5 && fields.length % 2 == 1 && (fields[2].equals("0") || fields[2].equals("1")));
        var dependencies = new LinkedHashMap<String, String>();
        for (int i = 5; i < fields.length; i += 2) require(dependencies.putIfAbsent(fields[i], fields[i + 1]) == null);
        return new AcProvenance(fields[0], fields[1], fields[2].equals("1"), fields[3], fields[4], dependencies);
    }

    /** Derives compatibility values from the active law and current document parameters. */
    public static double[] compatibilityValues(AcComponent component, double[] saved, AcDocumentSettings settings) {
        Map<String, String> parameters = settings == null ? Map.of() : settings.parameterMap();
        return compatibilityValues(component, saved, parameters);
    }

    public static double[] compatibilityValues(AcComponent component, double[] saved, Map<String, String> parameters) {
        double[] values = saved.clone();
        switch (component.law()) {
            case AcComponent.Passive law -> values[0] = SpiceValue.evaluate(law.expression(), parameters);
            case AcComponent.Inductance law -> values[0] = law.origin().tested()
                    ? law.evaluate(parameters).henries() : SpiceValue.evaluate(law.expression(), parameters);
            case AcComponent.Voltage law -> values[1] = SpiceValue.evaluate(law.dc(), parameters);
            case AcComponent.Current law -> values[1] = SpiceValue.evaluate(law.dc(), parameters);
            case AcComponent.Controlled law -> {
                if (!law.laplace()) values[0] = SpiceValue.evaluate(law.expression(), parameters);
            }
        }
        return values;
    }

    static String encode(List<String> fields) {
        var encoded = new StringBuilder(VERSION);
        for (String field : fields) {
            encoded.append('.').append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(field.getBytes(StandardCharsets.UTF_8)));
        }
        return encoded.toString();
    }

    static String[] decode(String text) {
        String[] tokens = text.split("\\.", -1);
        require(tokens[0].equals(VERSION));
        String[] fields = new String[tokens.length - 1];
        for (int i = 0; i < fields.length; i++) {
            byte[] bytes = Base64.getUrlDecoder().decode(tokens[i + 1]);
            fields[i] = new String(bytes, StandardCharsets.UTF_8);
            require(java.util.Arrays.equals(bytes, fields[i].getBytes(StandardCharsets.UTF_8)));
        }
        return fields;
    }

    private static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException("Invalid or unsupported versioned AC field.");
    }
}
