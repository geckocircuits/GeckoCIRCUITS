package gecko.core.io;

/**
 * Records source input or normalization evidence that needs a diagnostic.
 *
 * @param code stable diagnostic code
 * @param domain affected circuit domain, or null for a global setting
 * @param component affected component, or null for a global setting
 * @param message diagnostic explanation
 * @param sourceLocation saved input location
 * @param unsupported true when the input needs unsupported execution behavior
 * @param parameterPath affected configuration path, or null when none applies
 */
public record ImportIssue(String code, String domain, String component, String message,
                          String sourceLocation, boolean unsupported, String parameterPath) {
    /**
     * Creates import evidence without a configuration path.
     *
     * @param code stable diagnostic code
     * @param domain affected circuit domain
     * @param component affected component
     * @param message diagnostic explanation
     * @param sourceLocation saved input location
     * @param unsupported true when the input needs unsupported execution behavior
     */
    public ImportIssue(String code, String domain, String component, String message,
                       String sourceLocation, boolean unsupported) {
        this(code, domain, component, message, sourceLocation, unsupported, null);
    }
}