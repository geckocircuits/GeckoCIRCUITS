package gecko.core.circuit.netlist;

/** A topology error with a stable code and component location. */
public final class TopologyValidationException extends IllegalArgumentException {
    private final String code;
    private final String component;
    public TopologyValidationException(String code, String component, String message) {
        super(message);
        this.code = code;
        this.component = component;
    }
    public String code() { return code; }
    public String component() { return component; }
}
