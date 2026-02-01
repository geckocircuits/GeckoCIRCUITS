/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

import java.util.*;

/**
 * Handles label conflict detection and resolution during component operations.
 *
 * When components are copied, moved between circuit sheets, or imported from
 * clipboard, label conflicts can occur. This class provides utilities to:
 *
 * 1. Detect label conflicts between source and target sheets
 * 2. Find unused suffix indices for renaming
 * 3. Rename labels with numeric suffixes to avoid conflicts
 * 4. Track potential coupling labels that need updating
 *
 * Label naming convention for conflicts:
 * - Original label: "myLabel"
 * - After conflict resolution: "myLabel.0", "myLabel.1", etc.
 *
 * @author GeckoCIRCUITS Team
 * @see SchematicEditor2
 */
public class LabelConflictResolver {

    /** Default separator between label and suffix */
    public static final String SUFFIX_SEPARATOR = ".";

    /**
     * Detects if any copied labels conflict with existing labels.
     *
     * @param existingLabels labels already present in the target sheet
     * @param copiedLabels labels being copied/imported
     * @return true if at least one copied label conflicts with existing
     */
    public boolean hasLabelConflict(Collection<String> existingLabels, Collection<String> copiedLabels) {
        if (existingLabels == null || copiedLabels == null) {
            return false;
        }
        
        Set<String> existingSet = new HashSet<>(existingLabels);
        for (String copiedLabel : copiedLabels) {
            if (copiedLabel != null && !copiedLabel.isEmpty() && existingSet.contains(copiedLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the smallest suffix index that avoids all conflicts.
     *
     * Given copied labels ["a", "b"] and existing labels ["a.0", "b.1"],
     * this method finds the smallest N where none of the renamed labels
     * "a.N", "b.N" conflict with existing labels.
     *
     * @param copiedLabels labels to be renamed
     * @param existingLabels labels already in the model
     * @return the smallest non-conflicting suffix index (0 or higher)
     */
    public int findNonConflictingSuffixIndex(List<String> copiedLabels, List<String> existingLabels) {
        if (copiedLabels == null || copiedLabels.isEmpty()) {
            return 0;
        }

        Set<String> existingSet = existingLabels != null ? 
                new HashSet<>(existingLabels) : Collections.emptySet();

        int suffixIndex = 0;
        boolean hasConflict;

        do {
            hasConflict = false;
            for (String copiedLabel : copiedLabels) {
                if (copiedLabel == null || copiedLabel.isEmpty()) {
                    continue;
                }
                String renamedLabel = copiedLabel + SUFFIX_SEPARATOR + suffixIndex;
                if (existingSet.contains(renamedLabel)) {
                    hasConflict = true;
                    break;
                }
            }
            if (hasConflict) {
                suffixIndex++;
            }
        } while (hasConflict);

        return suffixIndex;
    }

    /**
     * Renames a label with a suffix to avoid conflicts.
     *
     * @param originalLabel the original label string
     * @param suffixIndex the index to append
     * @return the renamed label, or empty string if original was empty
     */
    public String renameWithSuffix(String originalLabel, int suffixIndex) {
        if (originalLabel == null || originalLabel.isEmpty()) {
            return "";
        }
        return originalLabel + SUFFIX_SEPARATOR + suffixIndex;
    }

    /**
     * Removes duplicate labels from a list, preserving order.
     *
     * @param labels the list to deduplicate (modified in place)
     */
    public void removeDuplicates(List<String> labels) {
        if (labels == null || labels.size() <= 1) {
            return;
        }

        Set<String> seen = new LinkedHashSet<>();
        Iterator<String> iter = labels.iterator();
        while (iter.hasNext()) {
            String label = iter.next();
            if (!seen.add(label)) {
                iter.remove();
            }
        }
    }

    /**
     * Collects all labels from components that have terminals.
     *
     * @param components components to scan
     * @return list of all terminal labels (may contain duplicates and empty strings)
     */
    public List<String> collectLabelsFromComponents(Collection<? extends AbstractCircuitSheetComponent> components) {
        List<String> labels = new ArrayList<>();
        if (components == null) {
            return labels;
        }

        for (AbstractCircuitSheetComponent comp : components) {
            if (comp instanceof ComponentTerminable) {
                labels.addAll(((ComponentTerminable) comp).getAllNodeLabels());
            }
        }
        return labels;
    }

    /**
     * Identifies labels that will be orphaned when components are moved.
     *
     * When moving components between sheets, some labels may become
     * completely absent from the source sheet, breaking potential couplings.
     *
     * @param allComponentsInSheet all components in the source sheet
     * @param componentsBeingMoved components being moved away
     * @return set of labels that will be orphaned
     */
    public Set<String> findOrphanedLabels(
            Collection<? extends AbstractCircuitSheetComponent> allComponentsInSheet,
            Collection<? extends AbstractCircuitSheetComponent> componentsBeingMoved) {

        Set<String> orphanedLabels = new HashSet<>();
        if (allComponentsInSheet == null || componentsBeingMoved == null) {
            return orphanedLabels;
        }

        // Find labels before move
        Set<String> labelsBeforeMove = new HashSet<>();
        for (AbstractCircuitSheetComponent comp : allComponentsInSheet) {
            if (comp instanceof ComponentTerminable) {
                labelsBeforeMove.addAll(((ComponentTerminable) comp).getAllNodeLabels());
            }
        }
        labelsBeforeMove.remove("");

        // Find remaining components
        Set<AbstractCircuitSheetComponent> remaining = new HashSet<>(allComponentsInSheet);
        remaining.removeAll(componentsBeingMoved);

        // Find labels after move
        Set<String> labelsAfterMove = new HashSet<>();
        for (AbstractCircuitSheetComponent comp : remaining) {
            if (comp instanceof ComponentTerminable) {
                labelsAfterMove.addAll(((ComponentTerminable) comp).getAllNodeLabels());
            }
        }
        labelsAfterMove.remove("");

        // Find orphaned: labels that existed before but not after
        for (String label : labelsBeforeMove) {
            if (!labelsAfterMove.contains(label)) {
                orphanedLabels.add(label);
            }
        }

        return orphanedLabels;
    }

    /**
     * Checks if a potential coupling references orphaned labels.
     *
     * @param couplingLabels labels referenced by a potential coupling
     * @param remainingLabels labels still available in the sheet
     * @return true if any coupling label is not in remaining labels
     */
    public boolean hasOrphanedCouplingReference(String[] couplingLabels, Set<String> remainingLabels) {
        if (couplingLabels == null || remainingLabels == null) {
            return false;
        }

        for (String label : couplingLabels) {
            if (label != null && !label.isEmpty() && !remainingLabels.contains(label)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a conflict report for diagnostic purposes.
     *
     * @param existingLabels labels in the target sheet
     * @param copiedLabels labels being imported
     * @return a list of conflicting label names
     */
    public List<String> getConflictingLabels(Collection<String> existingLabels, Collection<String> copiedLabels) {
        List<String> conflicts = new ArrayList<>();
        if (existingLabels == null || copiedLabels == null) {
            return conflicts;
        }

        Set<String> existingSet = new HashSet<>(existingLabels);
        for (String label : copiedLabels) {
            if (label != null && !label.isEmpty() && existingSet.contains(label)) {
                conflicts.add(label);
            }
        }
        return conflicts;
    }

    /**
     * Renames all labels in a collection using the given suffix.
     *
     * @param labels labels to rename (modified in place via CircuitLabel objects)
     * @param suffixIndex the suffix index to use
     */
    public void renameAllLabels(Collection<CircuitLabel> labels, int suffixIndex) {
        if (labels == null) {
            return;
        }

        for (CircuitLabel label : labels) {
            String originalLabel = label.getLabelString();
            if (originalLabel != null && !originalLabel.isEmpty()) {
                label.setLabel(renameWithSuffix(originalLabel, suffixIndex));
            }
        }
    }

    /**
     * Extracts unique CircuitLabel objects from components.
     *
     * Some components share label objects between terminals.
     * This method returns a deduplicated set.
     *
     * @param components components to extract labels from
     * @return set of unique CircuitLabel objects
     */
    public Set<CircuitLabel> extractUniqueLabels(Collection<? extends AbstractCircuitSheetComponent> components) {
        Set<CircuitLabel> uniqueLabels = new HashSet<>();
        if (components == null) {
            return uniqueLabels;
        }

        for (AbstractCircuitSheetComponent comp : components) {
            if (comp instanceof ComponentTerminable) {
                for (TerminalInterface term : ((ComponentTerminable) comp).getAllTerminals()) {
                    uniqueLabels.add(term.getLabelObject());
                }
            }
        }
        return uniqueLabels;
    }

    /**
     * Validates that a label string is well-formed.
     *
     * @param label the label to validate
     * @return true if the label is valid (non-null, may be empty)
     */
    public boolean isValidLabel(String label) {
        return label != null;
    }

    /**
     * Checks if a label has already been renamed with a suffix.
     *
     * @param label the label to check
     * @return true if label contains the suffix separator followed by digits
     */
    public boolean hasSuffix(String label) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        int lastSeparator = label.lastIndexOf(SUFFIX_SEPARATOR);
        if (lastSeparator < 0 || lastSeparator == label.length() - 1) {
            return false;
        }
        String suffix = label.substring(lastSeparator + 1);
        try {
            Integer.parseInt(suffix);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Strips the numeric suffix from a label if present.
     *
     * @param label the label potentially containing a suffix
     * @return the label without suffix, or original if no suffix
     */
    public String stripSuffix(String label) {
        if (!hasSuffix(label)) {
            return label;
        }
        int lastSeparator = label.lastIndexOf(SUFFIX_SEPARATOR);
        return label.substring(0, lastSeparator);
    }
}
