package gecko.core.preparation;

import java.util.List;

/** Describes a supported numeric configuration field. */
public record ConfigurationField(String domain, String component, String name, String indexedName, List<String> aliases) {
    /**
     * Copies the alias list so callers cannot change the field description.
     *
     * @param domain the component domain
     * @param component the complete component name
     * @param name the canonical writable field name
     * @param indexedName the canonical indexed configuration name
     * @param aliases the additional writable names
     */
    public ConfigurationField { aliases = List.copyOf(aliases); }
}
