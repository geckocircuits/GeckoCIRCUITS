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
            case AcComponent.IdealDiode law -> fields.addAll(List.of("IDEAL_DIODE_V1", law.modelName(),
                    law.forwardVoltage(), law.onResistance(), law.offResistance(), law.origin().name()));
            case AcComponent.ExponentialDiode law -> {
                if (law.transientLaw()) {
                    fields.addAll(List.of("EXPONENTIAL_DIODE_TRANSIENT_V1", law.modelName(), law.saturationCurrent(),
                            law.emissionCoefficient(), law.seriesResistance(), law.transitTime()));
                } else {
                    fields.addAll(List.of("EXPONENTIAL_DIODE_V2", law.modelName(), law.saturationCurrent(),
                            law.emissionCoefficient(), law.seriesResistance(), law.transitTime(),
                            law.junctionCapacitance(), law.junctionPotential(), law.gradingCoefficient()));
                }
            }
            case AcComponent.Mosfet law -> fields.addAll(List.of("MOSFET_TRANSIENT_V1", law.modelName(),
                    law.pChannel() ? "PMOS" : "NMOS", law.width(), law.length(), encodeLawParameters(law.parameters())));
            case AcComponent.Bipolar law -> {
                fields.addAll(List.of("BIPOLAR_V1", law.modelName(), law.pnp() ? "PNP" : "NPN", law.area(),
                        encodeLawParameters(law.parameters())));
            }
            case AcComponent.Transconductance law -> fields.addAll(List.of("VCCS_V1", law.expression()));
            case AcComponent.Controlled law -> fields.addAll(List.of(law.laplace() ? "LAPLACE" : "GAIN", law.expression()));
        }
        fields.addAll(component.aliases());
        return encode(fields);
    }

    public static AcComponent decodeComponent(String text) {
        String[] fields = decode(text);
        require(fields.length >= 5 && (fields[1].equals("1") || fields[1].equals("0")));
        int aliasIndex = switch (fields[3]) {
            case "VOLTAGE", "BIPOLAR_V1" -> 8;
            case "CURRENT" -> 7;
            case "MOSFET_TRANSIENT_V1" -> 9;
            case "IDEAL_DIODE_V1", "EXPONENTIAL_DIODE_V1", "EXPONENTIAL_DIODE_TRANSIENT_V1" -> 9;
            case "EXPONENTIAL_DIODE_V2" -> 12;
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
            case "IDEAL_DIODE_V1" -> new AcComponent.IdealDiode(fields[4], fields[5], fields[6], fields[7],
                    AcInputOrigin.valueOf(fields[8]));
            case "EXPONENTIAL_DIODE_V1", "EXPONENTIAL_DIODE_TRANSIENT_V1" -> new AcComponent.ExponentialDiode(
                    fields[4], fields[5], fields[6], fields[7], fields[8], fields[3].equals("EXPONENTIAL_DIODE_TRANSIENT_V1"));
            case "EXPONENTIAL_DIODE_V2" -> new AcComponent.ExponentialDiode(fields[4], fields[5], fields[6],
                    fields[7], fields[8], fields[9], fields[10], fields[11]);
            case "BIPOLAR_V1" -> {
                require(fields[5].equals("NPN") || fields[5].equals("PNP"));
                yield new AcComponent.Bipolar(fields[4], fields[5].equals("PNP"), fields[6], decodeLawParameters(fields[7]));
            }
            case "MOSFET_TRANSIENT_V1" -> {
                require(fields[5].equals("NMOS") || fields[5].equals("PMOS"));
                yield new AcComponent.Mosfet(fields[4], fields[5].equals("PMOS"), fields[6], fields[7], decodeLawParameters(fields[8]));
            }
            case "VCCS_V1" -> new AcComponent.Transconductance(fields[4]);
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
        int slot = compatibilitySlot(component.law());
        if (slot >= values.length) {
            throw new IllegalArgumentException("Require compatibility slot " + slot + " on " + component.instance() + ".");
        }
        switch (component.law()) {
            case AcComponent.IdealDiode law -> {
                if (values.length < 4) {
                    throw new IllegalArgumentException("Require compatibility slots 1 to 3 on " + component.instance() + ".");
                }
                var diode = law.evaluate(parameters);
                values[1] = diode.forwardVoltage();
                values[2] = diode.onResistance();
                values[3] = diode.offResistance();
            }
            case AcComponent.ExponentialDiode law -> { }
            case AcComponent.Mosfet law -> { }
            case AcComponent.Bipolar law -> values[slot] = law.pnp() ? 0 : 1;
            case AcComponent.Passive law -> values[slot] = SpiceValue.requirePositivePassive(
                    SpiceValue.evaluate(law.expression(), parameters), component.instance());
            case AcComponent.Inductance law -> values[slot] = law.origin().tested()
                    ? law.evaluate(parameters).henries() : SpiceValue.requirePositivePassive(
                            SpiceValue.evaluate(law.expression(), parameters), component.instance());
            case AcComponent.Voltage law -> values[slot] = SpiceValue.evaluate(law.dc(), parameters);
            case AcComponent.Current law -> values[slot] = SpiceValue.evaluate(law.dc(), parameters);
            case AcComponent.Transconductance law -> values[slot] = SpiceValue.evaluate(law.expression(), parameters);
            case AcComponent.Controlled law -> {
                if (slot >= 0) values[slot] = SpiceValue.evaluate(law.expression(), parameters);
            }
        }
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Require finite compatibility values on " + component.instance() + ".");
            }
        }
        return values;
    }

    /** Returns the native slots derived from the law. */
    public static List<Integer> compatibilitySlots(AcComponent.Law law) {
        if (law instanceof AcComponent.IdealDiode) return List.of(1, 2, 3);
        int slot = compatibilitySlot(law);
        return slot < 0 ? List.of() : List.of(slot);
    }

    private static int compatibilitySlot(AcComponent.Law law) {
        return switch (law) {
            case AcComponent.Passive passive -> 0;
            case AcComponent.Inductance inductance -> 0;
            case AcComponent.Transconductance transconductance -> 0;
            case AcComponent.IdealDiode diode -> -1;
            case AcComponent.ExponentialDiode diode -> -1;
            case AcComponent.Bipolar transistor -> 6;
            case AcComponent.Mosfet transistor -> -1;
            case AcComponent.Voltage voltage -> 1;
            case AcComponent.Current current -> 1;
            case AcComponent.Controlled controlled -> controlled.laplace() ? -1 : 0;
        };
    }

    private static String encodeLawParameters(Map<String, String> values) {
        var fields = new java.util.ArrayList<String>();
        new java.util.TreeMap<>(values).forEach((key, value) -> {
            fields.add(key);
            fields.add(value);
        });
        return encode(fields);
    }

    private static Map<String, String> decodeLawParameters(String text) {
        var fields = decode(text);
        require(fields.length % 2 == 0);
        var parameters = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < fields.length; i += 2) {
            require(parameters.put(fields[i], fields[i + 1]) == null);
        }
        return parameters;
    }

    static String encode(List<String> fields) {
        return encode(VERSION, fields);
    }

    static String encode(String version, List<String> fields) {
        var encoded = new StringBuilder(version);
        for (String field : fields) {
            encoded.append('.').append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(field.getBytes(StandardCharsets.UTF_8)));
        }
        return encoded.toString();
    }

    static String[] decode(String text) {
        return decode(VERSION, text);
    }

    static String[] decode(String version, String text) {
        String[] tokens = text.split("\\.", -1);
        require(tokens[0].equals(version));
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
