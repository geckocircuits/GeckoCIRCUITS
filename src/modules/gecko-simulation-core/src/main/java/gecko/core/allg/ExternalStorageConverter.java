package gecko.core.allg;

/**
 * Interface to abstract GUI interaction when converting files from internal to external storage.
 * Allows GeckoFile to work in GUI-free environments (tests, REST API).
 *
 * @since Sprint 4a - GeckoFile Migration
 */
public interface ExternalStorageConverter {
    /**
     * Prompts for path to store external file.
     *
     * @param geckoFile the file being converted
     * @param originalContents the internal file contents to potentially write
     * @return absolute path to external file, or null if operation cancelled
     */
    String promptForExternalPath(GeckoFile geckoFile, byte[] originalContents);
}
