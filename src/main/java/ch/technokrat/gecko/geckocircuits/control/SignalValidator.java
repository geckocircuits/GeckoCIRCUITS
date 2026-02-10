package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import java.util.List;

/**
 * Validates and corrects signal indices for data export.
 * Ensures that signal names match their indices in the data container.
 */
public final class SignalValidator {
    
    /**
     * Validates that signal names match their indices in the data container.
     * Attempts to auto-correct mismatches by finding signals by name.
     * 
     * @param originalNames the expected signal names
     * @param indices the signal indices to validate
     * @param data the data container
     * @return validation result
     */
    public ValidationResult validateSignals(
            final List<String> originalNames,
            final List<Integer> indices,
            final AbstractDataContainer data) {
        
        ValidationResult result = new ValidationResult();
        final int knownIndices = indices.size();
        
        for (int i = 0; i < originalNames.size(); i++) {
            String expectedName = originalNames.get(i);
            int currentIndex = i < knownIndices ? indices.get(i) : -1;
            
            // Check if the index is valid and points to the correct signal
            if (!isValidSignalIndex(currentIndex, expectedName, data)) {
                // Try to find the signal by name
                int correctedIndex = findSignalIndexByName(expectedName, data);
                
                if (correctedIndex >= 0) {
                    result.addCorrection(i, currentIndex, correctedIndex, expectedName);
                } else {
                    result.addMissingSignal(expectedName);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Checks if the index is valid and points to a signal with the expected name.
     * 
     * @param index the signal index
     * @param expectedName the expected signal name
     * @param data the data container
     * @return true if valid, false otherwise
     */
    private boolean isValidSignalIndex(int index, String expectedName, AbstractDataContainer data) {
        if (index < 0 || index >= data.getRowLength()) {
            return false;
        }
        
        String actualName = data.getSignalName(index);
        return expectedName.equals(actualName);
    }
    
    /**
     * Finds the index of a signal by its name.
     * 
     * @param signalName the signal name to find
     * @param data the data container
     * @return the signal index, or -1 if not found
     */
    public int findSignalIndexByName(final String signalName, final AbstractDataContainer data) {
        for (int j = 0; j < data.getRowLength(); j++) {
            if (data.getSignalName(j).equals(signalName)) {
                return j;
            }
        }
        return -1;
    }
    
    /**
     * Result of signal validation.
     */
    public static class ValidationResult {
        private final StringBuilder corrections = new StringBuilder();
        private final StringBuilder missingSignals = new StringBuilder();
        private boolean hasCorrections = false;
        private boolean hasMissingSignals = false;
        
        /**
         * Adds a correction to the result.
         */
        void addCorrection(int position, int oldIndex, int newIndex, String signalName) {
            hasCorrections = true;
            if (corrections.length() > 0) {
                corrections.append(", ");
            }
            corrections.append(String.format("%s: %d->%d", signalName, oldIndex, newIndex));
        }
        
        /**
         * Adds a missing signal to the result.
         */
        void addMissingSignal(String signalName) {
            hasMissingSignals = true;
            if (missingSignals.length() > 0) {
                missingSignals.append(", ");
            }
            missingSignals.append(signalName);
        }
        
        /**
         * Checks if all signals are valid (no missing signals).
         */
        public boolean isValid() {
            return !hasMissingSignals;
        }
        
        /**
         * Checks if any corrections were made.
         */
        public boolean hasCorrections() {
            return hasCorrections;
        }
        
        /**
         * Gets the error message for missing signals.
         */
        public String getErrorMessage() {
            if (!hasMissingSignals) {
                return "";
            }
            return String.format("The following signals are not available: %s", 
                                missingSignals.toString());
        }
        
        /**
         * Gets the corrections message.
         */
        public String getCorrectionsMessage() {
            if (!hasCorrections) {
                return "";
            }
            return String.format("Auto-corrected signal indices: %s", corrections.toString());
        }
    }
}
