/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.HierarchyIndex;
import gecko.core.simulation.SimulationTiming.SettingIssue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Assigns each CONTROL block to a sample-clock group. This class is the only place that knows the sheet path rule.
 *
 * <p>Group 0 is the default group at the global period. Groups 1 to n are the configured sheet paths in sorted
 * order. A block takes the group of its nearest enclosing configured sheet. A sheet path joins the segments from the
 * root with "/". A native sheet contributes its occurrence instance and a legacy sheet its saved name. Segments
 * compare without regard to case. Inheritance walks sheet identifiers, never strings.</p>
 */
final class ControlSubsystems {
    static final String PATH = "controlSubsystemPeriods";

    /** The global period and the configured periods that a run request carries. */
    record Spec(double globalPeriod, Map<String, Double> periods) {
        Spec { periods = periods == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(periods)); }
        boolean grouped() { return !periods.isEmpty(); }
    }

    /**
     * The resolved assignment for one model.
     *
     * @param periods the sample period of each group, with the global period first
     * @param names the configured sheet path of each group, with an empty name first
     * @param groupOf the group of every CONTROL block of the model that was inspected, by identity
     * @param issues the findings, empty when the assignment can execute
     */
    record Plan(double[] periods, String[] names, IdentityHashMap<ComponentData, Integer> groupOf,
            List<SettingIssue> issues) {
        int groups() { return periods.length; }

        /** @return the controller step of each group for the electrical step: the period, or the step if shorter */
        double[] steps(double electricalStep) {
            double[] steps = new double[periods.length];
            for (int group = 0; group < steps.length; group++) steps[group] = Math.max(periods[group], electricalStep);
            return steps;
        }
    }

    private record Sheet(long id, long parent, String segment) { }

    private ControlSubsystems() { }

    private static boolean badKey(String key) {
        return key.isBlank() || key.startsWith("/") || key.endsWith("/") || key.contains("//");
    }

    private static boolean badValue(Double value) { return value == null || !Double.isFinite(value) || value < 0; }

    /** Checks keys and values without a circuit. A key is not blank, has no empty segment and no edge slash. */
    static List<SettingIssue> keyIssues(Map<String, Double> periods) {
        var issues = new ArrayList<SettingIssue>();
        for (var entry : periods.entrySet()) {
            String path = PATH + "." + entry.getKey();
            if (badKey(entry.getKey())) {
                issues.add(new SettingIssue(path, "A sheet path has no empty segment and no leading or trailing '/'."));
            }
            if (badValue(entry.getValue())) {
                issues.add(new SettingIssue(path, "CONTROL subsystem sample period must be finite and non-negative."));
            }
        }
        return issues;
    }

    /**
     * Resolves every configured path against the model and assigns its CONTROL blocks.
     * Blocks keep their own identity, so call this on the model whose components you will wire.
     *
     * @param model the resolved model whose CONTROL components are wired
     * @param spec the global period and the periods by sheet path
     * @return the assignment with its findings. An empty map assigns every block to group 0.
     */
    static Plan plan(CircuitModel model, Spec spec) {
        var names = new ArrayList<String>();
        names.add("");
        var periods = new ArrayList<Double>();
        periods.add(spec.globalPeriod());
        var groupOf = new IdentityHashMap<ComponentData, Integer>();
        var issues = new ArrayList<>(keyIssues(spec.periods()));
        var sheets = sheets(model);
        var byPath = new HashMap<String, List<Long>>();
        for (var sheet : sheets.values()) {
            String path = path(sheets, sheet);
            if (path != null) byPath.computeIfAbsent(path.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(sheet.id());
        }
        var groupOfSheet = new HashMap<Long, Integer>();
        var claimed = new HashMap<Long, String>();
        for (var entry : spec.periods().entrySet()) {
            String key = entry.getKey();
            if (badKey(key) || badValue(entry.getValue())) continue;
            var matches = byPath.getOrDefault(key.toLowerCase(Locale.ROOT), List.of());
            String path = PATH + "." + key;
            if (matches.isEmpty()) {
                issues.add(new SettingIssue(path, "No subcircuit sheet has the path '" + key + "'."));
            } else if (matches.size() > 1) {
                issues.add(new SettingIssue(path, "The sheet path '" + key + "' is ambiguous. More than one sheet matches."));
            } else if (claimed.putIfAbsent(matches.get(0), key) != null) {
                issues.add(new SettingIssue(path, "The sheet path '" + key + "' names the same sheet as '"
                        + claimed.get(matches.get(0)) + "'."));
            } else {
                groupOfSheet.put(matches.get(0), names.size());
                names.add(key);
                periods.add(entry.getValue());
            }
        }
        var populated = new HashSet<Long>();
        boolean broken = false;
        for (var component : model.getControlComponents()) {
            if (ControlBlockDescriptor.find(component.getType()) == null) continue;
            int group = 0;
            boolean found = false;
            var seen = new HashSet<Long>();
            long id = HierarchyIndex.parent(component.getSavedBlock());
            while (id != 0) {
                var sheet = sheets.get(id);
                if (sheet == null || !seen.add(id)) {
                    if (!spec.periods().isEmpty()) broken = true;
                    break;
                }
                populated.add(id);
                if (!found && groupOfSheet.containsKey(id)) {
                    group = groupOfSheet.get(id);
                    found = true;
                }
                id = sheet.parent();
            }
            groupOf.put(component, group);
        }
        if (broken) {
            issues.add(new SettingIssue(PATH, "A CONTROL block names a parent sheet that is missing or circular."));
        }
        for (var entry : groupOfSheet.entrySet()) {
            if (!populated.contains(entry.getKey())) {
                issues.add(new SettingIssue(PATH + "." + names.get(entry.getValue()),
                        "The sheet '" + names.get(entry.getValue()) + "' has no CONTROL block."));
            }
        }
        double[] array = new double[periods.size()];
        for (int index = 0; index < array.length; index++) array[index] = periods.get(index);
        return new Plan(array, names.toArray(String[]::new), groupOf, List.copyOf(issues));
    }

    /** Collects sheets by identifier. A native sheet uses its occurrence instance. A legacy sheet uses its saved name. */
    private static Map<Long, Sheet> sheets(CircuitModel model) {
        var sheets = new LinkedHashMap<Long, Sheet>();
        HierarchyIndex hierarchy = null;
        for (var component : model.getSpecialComponents()) {
            if (component.getType() != 27 || component.getSavedBlock() == null) continue;
            if (hierarchy == null) hierarchy = new HierarchyIndex(model);
            var saved = component.getSavedBlock();
            var occurrence = hierarchy.sheet(saved.uniqueObjectIdentifier());
            String segment = occurrence != null ? occurrence.call().instance() : component.getName();
            sheets.put(saved.uniqueObjectIdentifier(), new Sheet(saved.uniqueObjectIdentifier(),
                    HierarchyIndex.parent(saved), segment == null ? "" : segment));
        }
        return sheets;
    }

    /** @return the path from the root, or null when the chain is circular, broken or has a segment that cannot be addressed */
    private static String path(Map<Long, Sheet> sheets, Sheet sheet) {
        var segments = new ArrayList<String>();
        var seen = new HashSet<Long>();
        Sheet cursor = sheet;
        while (cursor != null) {
            if (!seen.add(cursor.id()) || cursor.segment().isBlank() || cursor.segment().contains("/")) return null;
            segments.add(0, cursor.segment());
            if (cursor.parent() == 0) break;
            cursor = sheets.get(cursor.parent());
            if (cursor == null) return null;
        }
        return String.join("/", segments);
    }

    /** @return the group of a block, which is 0 for a block the plan did not inspect */
    static int groupOf(Plan plan, ComponentData component) {
        Integer group = plan.groupOf().get(component);
        return group == null ? 0 : group;
    }
}
