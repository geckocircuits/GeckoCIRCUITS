package gecko.core.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Saved configuration, including ordered extension sections and circuit hierarchy. */
public final class SavedCircuitBlock {
    public record Section(String tag, String text) { }

    private final String tag;
    private final Map<String, String> tokens;
    private final List<Section> orderedSections;
    private final Map<String, String> sections;
    private final String serializedText;

    private SavedCircuitBlock(String tag, Map<String, String> tokens, List<Section> sections, String text) {
        this.tag = tag;
        this.tokens = Map.copyOf(tokens);
        this.orderedSections = List.copyOf(sections);
        var indexed = new LinkedHashMap<String, String>();
        sections.forEach(section -> indexed.putIfAbsent(section.tag(), section.text()));
        this.sections = Map.copyOf(indexed);
        this.serializedText = text;
    }

    /**
     * Captures saved tokens and ordered sections without retaining scope display settings.
     * @param tag the enclosing saved block tag
     * @param lines the block contents without the enclosing tag lines
     * @return immutable saved configuration with duplicate section order preserved
     */
    public static SavedCircuitBlock parse(String tag, String[] lines) {
        var tokens = new LinkedHashMap<String, String>();
        var sections = new ArrayList<Section>();
        var retained = new ArrayList<String>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("<") && line.endsWith(">") && !line.startsWith("<\\")) {
                String sectionTag = line.substring(1, line.length() - 1);
                int end = closingLine(lines, i, sectionTag);
                // These sections configure windows and plots, not simulation input.
                if (!sectionTag.equals("ScopeSettings") && !sectionTag.equals("ScopeWindowSettings")) {
                    sections.add(new Section(sectionTag, String.join("\n", java.util.Arrays.copyOfRange(lines, i + 1, end))));
                    for (int j = i; j <= end && j < lines.length; j++) retained.add(lines[j]);
                }
                i = end;
            } else {
                retained.add(lines[i]);
                if (!line.isEmpty() && !line.startsWith("<")) {
                    String[] pair = line.split("\\s+", 2);
                    tokens.putIfAbsent(pair[0], pair.length == 2 ? pair[1] : "");
                }
            }
        }
        return new SavedCircuitBlock(tag, tokens, sections, String.join("\n", retained));
    }

    static int closingLine(String[] lines, int opening, String tag) {
        int depth = 1;
        for (int i = opening + 1; i < lines.length; i++) {
            if (lines[i].trim().equals("<" + tag + ">")) depth++;
            if (lines[i].trim().equals("<\\" + tag + ">") && --depth == 0) return i;
        }
        return lines.length;
    }

    public String tag() { return tag; }
    public Map<String, String> tokens() { return tokens; }
    /** Returns the first section for each tag. Use orderedSections for duplicate tags. */
    public Map<String, String> sections() { return sections; }
    public List<Section> orderedSections() { return orderedSections; }
    public String serializedText() { return serializedText; }
    /** Returns zero for an absent or invalid identifier. Parser evidence retains invalid input. */
    public long uniqueObjectIdentifier() {
        try { return longToken("uniqueObjectIdentifier", 0); }
        catch (NumberFormatException error) { return 0; }
    }
    public long parentSheetIdentifier() { return longToken("parentSheetIdentifier", 0); }
    /** Uses desktop ordinals: disabled 0, enabled 1, shorted 2. */
    public int enabledState() {
        return (int) longToken("enabledShorted", Boolean.parseBoolean(tokens.getOrDefault("enabled", "true")) ? 1 : 0);
    }
    private long longToken(String name, long fallback) {
        String value = tokens.get(name);
        return value == null ? fallback : Long.parseLong(value);
    }
}
