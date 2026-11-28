package gecko.core.preparation;

/** A stable finding with an optional circuit location. */
public record Diagnostic(String code, Category category, Severity severity, String message,
        String domain, String component, String parameterPath, String sourceLocation) {
    public enum Category { PARSING, CONFIGURATION, OVERRIDES, REFERENCES, TOPOLOGY, CAPABILITY }
    public enum Severity { ERROR, WARNING }
}
