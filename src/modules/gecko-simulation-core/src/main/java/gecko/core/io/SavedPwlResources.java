package gecko.core.io;

import gecko.core.circuit.SourceType;
import gecko.core.circuit.waveform.PiecewiseLinearWaveform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks the raw declarations for source-owned PWL data before resource capture. */
public final class SavedPwlResources {
    private static final String REFERENCE = PiecewiseLinearWaveform.RESOURCE_ID_TOKEN;
    private static final Set<String> LEGACY_RESOURCE_KEYS = Set.of("usageList", "<usageList>", "<usageList[]>",
            "fileContents", "<fileContents>", "<fileContents[]>");

    private SavedPwlResources() { }

    public record Source(long resourceId, long ownerId) { }
    public record Embedded(long resourceId, long ownerId, byte[] bytes) {
        public Embedded { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }

    public static long identifier(String text) {
        long id = Long.parseLong(text);
        if (id == 0 || !Long.toString(id).equals(text)) {
            throw new IllegalArgumentException("Use a canonical nonzero PWL identifier.");
        }
        return id;
    }

    /** A source can retain its table when temporarily switched to a different mode. */
    public static Source source(String[] lines, boolean electricalSource) {
        var pointers = rows(lines, REFERENCE);
        var modes = rows(lines, "sourceType");
        var legacyModes = electricalSource ? rows(lines, "parameter[]") : List.<String>of();
        boolean pwlMode = electricalSource && (modes.stream().anyMatch(SavedPwlResources::pwlMode)
                || modes.isEmpty() && legacyModes.stream().anyMatch(SavedPwlResources::pwlMode));
        if (pointers.isEmpty() && !pwlMode) return null;
        if (!electricalSource) {
            throw new IllegalArgumentException("Only electrical voltage and current sources can own PWL data.");
        }
        if (modes.size() > 1 || modes.isEmpty() && legacyModes.size() > 1 || rows(lines, "typ").size() > 1) {
            throw new IllegalArgumentException("Use a single PWL source definition.");
        }
        return new Source(identifier(single(pointers, REFERENCE)),
                identifier(single(rows(lines, "uniqueObjectIdentifier"), "uniqueObjectIdentifier")));
    }

    private static boolean pwlMode(String text) {
        try {
            double mode = Double.parseDouble(text.split("\\s+", 2)[0]);
            return mode == SourceType.PWL || mode == SourceType.PWL_NEW;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    /** Returns embedded PWL records by their position in the raw file block list. */
    public static Map<Integer, Embedded> preflight(List<Source> sources, List<String[]> blocks) {
        var expected = new LinkedHashMap<Long, Long>();
        for (Source source : sources) {
            if (expected.putIfAbsent(source.resourceId(), source.ownerId()) != null) {
                throw new IllegalArgumentException("A PWL resource must belong to exactly one source.");
            }
        }

        var captured = new LinkedHashMap<Integer, Embedded>();
        if (expected.isEmpty()) return captured;
        var seen = new HashSet<Long>();
        for (int i = 0; i < blocks.size(); i++) {
            String[] lines = blocks.get(i);
            for (String hash : rows(lines, "hashValue")) {
                long id;
                try {
                    // Include rows the legacy scalar reader could interpret as this ID.
                    id = Long.parseLong(hash.split("\\s+", 2)[0]);
                } catch (NumberFormatException error) {
                    continue;
                }
                Long owner = expected.get(id);
                if (owner == null) continue;
                if (!seen.add(id)) {
                    throw new IllegalArgumentException("Duplicate PWL resource identifier " + id);
                }
                Embedded record = embedded(lines);
                if (record.ownerId() != owner) {
                    throw new IllegalArgumentException("Use the source's numeric owner for PWL resource " + id);
                }
                captured.put(i, record);
                break;
            }
        }

        var missing = new LinkedHashSet<>(expected.keySet());
        missing.removeAll(seen);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing embedded PWL resource " + missing);
        }
        return captured;
    }

    public static Embedded embedded(String[] lines) {
        long id = identifier(single(rows(lines, "hashValue"), "hashValue"));
        var storage = rows(lines, "isExternal");
        if (!storage.equals(List.of("0"))) {
            throw new IllegalArgumentException("Declare the PWL resource internal and embed its data.");
        }
        for (String row : lines) {
            String key = row.strip().split("\\s+", 2)[0];
            if (LEGACY_RESOURCE_KEYS.contains(key)) {
                throw new IllegalArgumentException("Remove legacy PWL payload or owner declarations.");
            }
        }
        String owner = single(rows(lines, "usageList[]"), "usageList[]");
        if (owner.startsWith("/")) owner = owner.substring(1);
        long ownerId = identifier(owner);
        String payload = single(rows(lines, "fileContents[]"), "fileContents[]");
        String[] values = payload.split("\\s+");
        byte[] bytes = new byte[values.length];
        try {
            for (int i = 0; i < values.length; i++) bytes[i] = Byte.parseByte(values[i]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Embed valid PWL data in the circuit document.", error);
        }
        PiecewiseLinearWaveform.fromBytes(bytes);
        return new Embedded(id, ownerId, bytes);
    }

    private static String single(List<String> values, String field) {
        if (values.size() != 1) {
            throw new IllegalArgumentException("Use exactly one PWL " + field + " declaration.");
        }
        return values.getFirst();
    }

    private static List<String> rows(String[] lines, String key) {
        var values = new ArrayList<String>();
        for (String row : lines) {
            String[] pair = row.strip().split("\\s+", 2);
            if (key.equals(pair[0])) values.add(pair.length == 2 ? pair[1] : "");
        }
        return values;
    }
}
