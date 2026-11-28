package gecko.core.ac;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Settings owned by the editor document and copied into each AC run. */
public record AcDocumentSettings(AcSweep sweep, List<ParameterDefinition> parameters, String analysisId) {
    public AcDocumentSettings {
        if (sweep == null || analysisId == null || analysisId.isBlank()) {
            throw new IllegalArgumentException("Require an AC sweep and analysis identifier.");
        }
        parameters = List.copyOf(parameters);
        parameterMap(parameters);
    }

    public Map<String, String> parameterMap() { return parameterMap(parameters); }

    private static Map<String, String> parameterMap(List<ParameterDefinition> definitions) {
        var result = new LinkedHashMap<String, String>();
        for (var definition : definitions) {
            if (result.putIfAbsent(definition.name().toLowerCase(Locale.ROOT), definition.expression()) != null) {
                throw new IllegalArgumentException("Duplicate parameter: " + definition.name());
            }
        }
        return result;
    }
}
