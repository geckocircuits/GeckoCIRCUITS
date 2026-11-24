package gecko.core.io;

import gecko.core.ac.AcComponent;
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
            case AcComponent.Voltage law -> fields.addAll(List.of("VOLTAGE", law.dc(), law.waveform(),
                    law.magnitude(), law.phaseDegrees()));
            case AcComponent.Controlled law -> fields.addAll(List.of(law.laplace() ? "LAPLACE" : "GAIN", law.expression()));
        }
        fields.addAll(component.aliases());
        return encode(fields);
    }

    public static AcComponent decodeComponent(String text) {
        String[] fields = decode(text);
        require(fields.length >= 5 && (fields[1].equals("1") || fields[1].equals("0")));
        int aliasIndex = fields[3].equals("VOLTAGE") ? 8 : 5;
        require(fields.length >= aliasIndex);
        List<Integer> orders = java.util.Arrays.stream(fields[2].split(",", -1)).map(Integer::valueOf).toList();
        AcComponent.Law law = switch (fields[3]) {
            case "PASSIVE" -> new AcComponent.Passive(fields[4]);
            case "VOLTAGE" -> new AcComponent.Voltage(fields[4], fields[5], fields[6], fields[7]);
            case "GAIN", "LAPLACE" -> new AcComponent.Controlled(fields[4], fields[3].equals("LAPLACE"));
            default -> throw new IllegalArgumentException("Unsupported IPES component law: " + fields[3]);
        };
        return new AcComponent(fields[0], orders, List.of(fields).subList(aliasIndex, fields.length), fields[1].equals("1"), law);
    }

    public static String encode(AcProvenance provenance) {
        var fields = new ArrayList<>(List.of(provenance.sourceSha256(), provenance.importRevision(),
                provenance.importSourceDirty() ? "1" : "0", provenance.importSourceSnapshotSha256(),
                provenance.importRuntimeJarSha256()));
        provenance.dependencySha256().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            fields.add(entry.getKey());
            fields.add(entry.getValue());
        });
        return encode(fields);
    }

    public static AcProvenance decodeProvenance(String text) {
        String[] fields = decode(text);
        require(fields.length >= 5 && fields.length % 2 == 1 && (fields[2].equals("0") || fields[2].equals("1")));
        var dependencies = new LinkedHashMap<String, String>();
        for (int i = 5; i < fields.length; i += 2) require(dependencies.putIfAbsent(fields[i], fields[i + 1]) == null);
        return new AcProvenance(fields[0], fields[1], fields[2].equals("1"), fields[3], fields[4], dependencies);
    }

    /** Derives compatibility values from the active law and current document parameters. */
    public static double[] compatibilityValues(AcComponent component, double[] saved, AcDocumentSettings settings) {
        Map<String, String> parameters = settings == null ? Map.of() : settings.parameterMap();
        double[] values = saved.clone();
        switch (component.law()) {
            case AcComponent.Passive law -> values[0] = SpiceValue.evaluate(law.expression(), parameters);
            case AcComponent.Voltage law -> values[1] = SpiceValue.evaluate(law.dc(), parameters);
            case AcComponent.Controlled law -> {
                if (!law.laplace()) values[0] = SpiceValue.evaluate(law.expression(), parameters);
            }
        }
        return values;
    }

    private static String encode(List<String> fields) {
        var encoded = new StringBuilder(VERSION);
        for (String field : fields) {
            encoded.append('.').append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(field.getBytes(StandardCharsets.UTF_8)));
        }
        return encoded.toString();
    }

    private static String[] decode(String text) {
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
