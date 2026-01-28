/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.core.circuit.netlist;

import java.util.*;

/**
 * Resolves component labels to node indices and element positions.
 * Extracted from NetListLK for better testability and separation of concerns.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Map label strings to potential area indices</li>
 *   <li>Find node indices from terminal labels</li>
 *   <li>Support label-based lookups for measurements</li>
 *   <li>Validate label uniqueness and existence</li>
 * </ul>
 *
 * <p>Labels in GeckoCIRCUITS:
 * <ul>
 *   <li>Each potential area can have an associated label</li>
 *   <li>Labels are used for measurements and cross-references</li>
 *   <li>Terminal labels allow connecting nodes via labels rather than wires</li>
 * </ul>
 *
 * @author Extracted from NetListLK.findIndexFromLabelInSheet
 * @since Sprint 2 - Circuit Refactoring
 */
public class LabelResolver {

    /** Label to index mapping */
    private final Map<String, Integer> labelToIndex;

    /** Index to label mapping (reverse lookup) */
    private final Map<Integer, String> indexToLabel;

    /** Array of labels in order (for compatibility with NetListLK.labelListe) */
    private final String[] labelList;

    /**
     * Creates a LabelResolver with no initial labels.
     */
    public LabelResolver() {
        this.labelToIndex = new HashMap<>();
        this.indexToLabel = new HashMap<>();
        this.labelList = new String[0];
    }

    /**
     * Creates a LabelResolver from an array of labels.
     * Index i maps to labelList[i].
     *
     * @param labels array of label strings (null entries are skipped)
     */
    public LabelResolver(String[] labels) {
        this.labelToIndex = new HashMap<>();
        this.indexToLabel = new HashMap<>();
        this.labelList = labels != null ? labels.clone() : new String[0];

        for (int i = 0; i < labelList.length; i++) {
            if (labelList[i] != null && !labelList[i].isEmpty()) {
                labelToIndex.put(labelList[i], i);
                indexToLabel.put(i, labelList[i]);
            }
        }
    }

    /**
     * Creates a LabelResolver from a map of labels to indices.
     *
     * @param labelMap map of label string to index
     */
    public LabelResolver(Map<String, Integer> labelMap) {
        this.labelToIndex = new HashMap<>(labelMap);
        this.indexToLabel = new HashMap<>();

        // Build reverse mapping and find max index for labelList
        int maxIndex = 0;
        for (Map.Entry<String, Integer> entry : labelMap.entrySet()) {
            indexToLabel.put(entry.getValue(), entry.getKey());
            maxIndex = Math.max(maxIndex, entry.getValue());
        }

        // Build labelList array
        this.labelList = new String[maxIndex + 1];
        for (Map.Entry<Integer, String> entry : indexToLabel.entrySet()) {
            labelList[entry.getKey()] = entry.getValue();
        }
    }

    /**
     * Adds or updates a label mapping.
     *
     * @param label the label string
     * @param index the node/potential index
     * @throws IllegalArgumentException if label is null or empty
     */
    public void addLabel(String label, int index) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }

        // Remove old mapping if label was used elsewhere
        if (labelToIndex.containsKey(label)) {
            int oldIndex = labelToIndex.get(label);
            indexToLabel.remove(oldIndex);
        }

        // Remove old label at this index
        if (indexToLabel.containsKey(index)) {
            String oldLabel = indexToLabel.get(index);
            labelToIndex.remove(oldLabel);
        }

        labelToIndex.put(label, index);
        indexToLabel.put(index, label);
    }

    /**
     * Gets the index for a label.
     *
     * @param label the label to look up
     * @return the index, or -1 if not found
     */
    public int getIndex(String label) {
        Integer index = labelToIndex.get(label);
        return index != null ? index : -1;
    }

    /**
     * Gets the label for an index.
     *
     * @param index the index to look up
     * @return the label, or null if not found
     */
    public String getLabel(int index) {
        return indexToLabel.get(index);
    }

    /**
     * Checks if a label exists.
     *
     * @param label the label to check
     * @return true if the label is mapped
     */
    public boolean hasLabel(String label) {
        return label != null && labelToIndex.containsKey(label);
    }

    /**
     * Checks if an index has a label.
     *
     * @param index the index to check
     * @return true if the index has a label
     */
    public boolean hasLabelAtIndex(int index) {
        return indexToLabel.containsKey(index);
    }

    /**
     * Gets the total number of labels.
     *
     * @return label count
     */
    public int getLabelCount() {
        return labelToIndex.size();
    }

    /**
     * Gets all labels.
     *
     * @return unmodifiable set of all labels
     */
    public Set<String> getAllLabels() {
        return Collections.unmodifiableSet(labelToIndex.keySet());
    }

    /**
     * Gets all mapped indices.
     *
     * @return unmodifiable set of all indices with labels
     */
    public Set<Integer> getAllIndices() {
        return Collections.unmodifiableSet(indexToLabel.keySet());
    }

    /**
     * Gets the label list array (for compatibility with NetListLK).
     *
     * @return copy of label list array
     */
    public String[] getLabelList() {
        return labelList.clone();
    }

    /**
     * Removes a label mapping.
     *
     * @param label the label to remove
     * @return true if the label was removed
     */
    public boolean removeLabel(String label) {
        Integer index = labelToIndex.remove(label);
        if (index != null) {
            indexToLabel.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Removes a label at an index.
     *
     * @param index the index to clear
     * @return true if a label was removed
     */
    public boolean removeLabelAtIndex(int index) {
        String label = indexToLabel.remove(index);
        if (label != null) {
            labelToIndex.remove(label);
            return true;
        }
        return false;
    }

    /**
     * Clears all label mappings.
     */
    public void clear() {
        labelToIndex.clear();
        indexToLabel.clear();
    }

    /**
     * Finds labels matching a pattern.
     *
     * @param pattern regex pattern to match
     * @return list of matching labels
     */
    public List<String> findLabelsMatching(String pattern) {
        List<String> matches = new ArrayList<>();
        for (String label : labelToIndex.keySet()) {
            if (label.matches(pattern)) {
                matches.add(label);
            }
        }
        return matches;
    }

    /**
     * Finds labels starting with a prefix.
     *
     * @param prefix the prefix to match
     * @return list of labels starting with prefix
     */
    public List<String> findLabelsWithPrefix(String prefix) {
        List<String> matches = new ArrayList<>();
        for (String label : labelToIndex.keySet()) {
            if (label.startsWith(prefix)) {
                matches.add(label);
            }
        }
        return matches;
    }

    /**
     * Validates that all required labels exist.
     *
     * @param requiredLabels labels that must exist
     * @return list of missing labels (empty if all present)
     */
    public List<String> validateLabels(Collection<String> requiredLabels) {
        List<String> missing = new ArrayList<>();
        for (String label : requiredLabels) {
            if (!hasLabel(label)) {
                missing.add(label);
            }
        }
        return missing;
    }

    /**
     * Creates a merged resolver combining this and another.
     * In case of conflicts, the other resolver's mappings take precedence.
     *
     * @param other the other resolver to merge
     * @return new merged resolver
     */
    public LabelResolver merge(LabelResolver other) {
        Map<String, Integer> merged = new HashMap<>(this.labelToIndex);
        merged.putAll(other.labelToIndex);
        return new LabelResolver(merged);
    }

    /**
     * Gets the index for a label, throwing if not found.
     * Similar to NetListLK.findIndexFromLabelInSheet behavior.
     *
     * @param label the label to look up
     * @param componentName name of component requesting the lookup (for error message)
     * @return the index
     * @throws RuntimeException if label not found
     */
    public int getIndexOrThrow(String label, String componentName) {
        int index = getIndex(label);
        if (index < 0) {
            throw new RuntimeException(
                "Error in measurement component " + componentName +
                "\nThe label reference \"" + label + "\" was not found.\n" +
                "Please check the label or disable the measurement component."
            );
        }
        return index;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LabelResolver[count=").append(getLabelCount()).append("]\n");
        for (Map.Entry<String, Integer> entry : labelToIndex.entrySet()) {
            sb.append("  \"").append(entry.getKey()).append("\" -> ");
            sb.append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LabelResolver)) return false;
        LabelResolver other = (LabelResolver) obj;
        return labelToIndex.equals(other.labelToIndex);
    }

    @Override
    public int hashCode() {
        return labelToIndex.hashCode();
    }
}
