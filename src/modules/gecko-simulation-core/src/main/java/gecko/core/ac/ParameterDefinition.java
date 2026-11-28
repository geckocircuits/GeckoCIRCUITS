package gecko.core.ac;

/** A scalar parameter with its original display spelling. */
public record ParameterDefinition(String name, String expression) {
    public ParameterDefinition {
        if (!name.matches("[A-Za-z_][A-Za-z_0-9]*") || expression.isBlank()) {
            throw new IllegalArgumentException("Require a valid parameter name and expression.");
        }
    }
}
