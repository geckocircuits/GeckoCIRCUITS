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

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for LabelConflictResolver.
 *
 * Tests the label conflict detection and resolution logic extracted
 * from SchematicEditor2.
 *
 * @author GeckoCIRCUITS Team
 */
public class LabelConflictResolverTest {

    private LabelConflictResolver resolver;

    @Before
    public void setUp() {
        resolver = new LabelConflictResolver();
    }

    // ==================== hasLabelConflict Tests ====================

    @Test
    public void testHasLabelConflict_NoConflict() {
        List<String> existing = Arrays.asList("a", "b", "c");
        List<String> copied = Arrays.asList("d", "e", "f");
        
        assertFalse("Should not detect conflict", resolver.hasLabelConflict(existing, copied));
    }

    @Test
    public void testHasLabelConflict_WithConflict() {
        List<String> existing = Arrays.asList("a", "b", "c");
        List<String> copied = Arrays.asList("b", "d");
        
        assertTrue("Should detect conflict on 'b'", resolver.hasLabelConflict(existing, copied));
    }

    @Test
    public void testHasLabelConflict_EmptyExisting() {
        List<String> existing = Collections.emptyList();
        List<String> copied = Arrays.asList("a", "b");
        
        assertFalse("Empty existing should have no conflicts", resolver.hasLabelConflict(existing, copied));
    }

    @Test
    public void testHasLabelConflict_EmptyCopied() {
        List<String> existing = Arrays.asList("a", "b");
        List<String> copied = Collections.emptyList();
        
        assertFalse("Empty copied should have no conflicts", resolver.hasLabelConflict(existing, copied));
    }

    @Test
    public void testHasLabelConflict_NullExisting() {
        List<String> copied = Arrays.asList("a", "b");
        
        assertFalse("Null existing should have no conflicts", resolver.hasLabelConflict(null, copied));
    }

    @Test
    public void testHasLabelConflict_NullCopied() {
        List<String> existing = Arrays.asList("a", "b");
        
        assertFalse("Null copied should have no conflicts", resolver.hasLabelConflict(existing, null));
    }

    @Test
    public void testHasLabelConflict_IgnoresEmptyLabels() {
        List<String> existing = Arrays.asList("a", "", "b");
        List<String> copied = Arrays.asList("", "c");
        
        assertFalse("Empty labels should not cause conflicts", resolver.hasLabelConflict(existing, copied));
    }

    @Test
    public void testHasLabelConflict_AllSame() {
        List<String> existing = Arrays.asList("a", "b");
        List<String> copied = Arrays.asList("a", "b");
        
        assertTrue("Identical lists should conflict", resolver.hasLabelConflict(existing, copied));
    }

    // ==================== findNonConflictingSuffixIndex Tests ====================

    @Test
    public void testFindNonConflictingSuffixIndex_NoConflicts() {
        List<String> copied = Arrays.asList("a", "b");
        List<String> existing = Arrays.asList("c", "d");
        
        assertEquals("Should return 0 when no conflicts", 0, resolver.findNonConflictingSuffixIndex(copied, existing));
    }

    @Test
    public void testFindNonConflictingSuffixIndex_ConflictAtZero() {
        List<String> copied = Arrays.asList("a", "b");
        List<String> existing = Arrays.asList("a.0", "c");
        
        assertEquals("Should return 1 when .0 conflicts", 1, resolver.findNonConflictingSuffixIndex(copied, existing));
    }

    @Test
    public void testFindNonConflictingSuffixIndex_MultipleConflicts() {
        List<String> copied = Arrays.asList("x");
        List<String> existing = Arrays.asList("x.0", "x.1", "x.2");
        
        assertEquals("Should return 3 to skip all conflicts", 3, resolver.findNonConflictingSuffixIndex(copied, existing));
    }

    @Test
    public void testFindNonConflictingSuffixIndex_EmptyCopied() {
        List<String> copied = Collections.emptyList();
        List<String> existing = Arrays.asList("a", "b");
        
        assertEquals("Empty copied should return 0", 0, resolver.findNonConflictingSuffixIndex(copied, existing));
    }

    @Test
    public void testFindNonConflictingSuffixIndex_NullCopied() {
        List<String> existing = Arrays.asList("a", "b");
        
        assertEquals("Null copied should return 0", 0, resolver.findNonConflictingSuffixIndex(null, existing));
    }

    @Test
    public void testFindNonConflictingSuffixIndex_NullExisting() {
        List<String> copied = Arrays.asList("a", "b");
        
        assertEquals("Null existing should return 0", 0, resolver.findNonConflictingSuffixIndex(copied, null));
    }

    @Test
    public void testFindNonConflictingSuffixIndex_SkipsEmptyLabels() {
        List<String> copied = Arrays.asList("", "a");
        List<String> existing = Arrays.asList("a.0");
        
        assertEquals("Should consider only non-empty labels", 1, resolver.findNonConflictingSuffixIndex(copied, existing));
    }

    // ==================== renameWithSuffix Tests ====================

    @Test
    public void testRenameWithSuffix_NormalLabel() {
        assertEquals("label.0", resolver.renameWithSuffix("label", 0));
        assertEquals("label.5", resolver.renameWithSuffix("label", 5));
        assertEquals("test.123", resolver.renameWithSuffix("test", 123));
    }

    @Test
    public void testRenameWithSuffix_EmptyLabel() {
        assertEquals("Empty label should return empty", "", resolver.renameWithSuffix("", 0));
    }

    @Test
    public void testRenameWithSuffix_NullLabel() {
        assertEquals("Null label should return empty", "", resolver.renameWithSuffix(null, 0));
    }

    @Test
    public void testRenameWithSuffix_LabelWithDot() {
        assertEquals("my.label.0", resolver.renameWithSuffix("my.label", 0));
    }

    // ==================== removeDuplicates Tests ====================

    @Test
    public void testRemoveDuplicates_NoDuplicates() {
        List<String> labels = new ArrayList<>(Arrays.asList("a", "b", "c"));
        resolver.removeDuplicates(labels);
        
        assertEquals(3, labels.size());
        assertEquals(Arrays.asList("a", "b", "c"), labels);
    }

    @Test
    public void testRemoveDuplicates_WithDuplicates() {
        List<String> labels = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b"));
        resolver.removeDuplicates(labels);
        
        assertEquals(3, labels.size());
        assertEquals(Arrays.asList("a", "b", "c"), labels);
    }

    @Test
    public void testRemoveDuplicates_AllSame() {
        List<String> labels = new ArrayList<>(Arrays.asList("x", "x", "x"));
        resolver.removeDuplicates(labels);
        
        assertEquals(1, labels.size());
        assertEquals("x", labels.get(0));
    }

    @Test
    public void testRemoveDuplicates_EmptyList() {
        List<String> labels = new ArrayList<>();
        resolver.removeDuplicates(labels);
        
        assertTrue("Empty list should stay empty", labels.isEmpty());
    }

    @Test
    public void testRemoveDuplicates_NullList() {
        resolver.removeDuplicates(null); // Should not throw
    }

    @Test
    public void testRemoveDuplicates_SingleElement() {
        List<String> labels = new ArrayList<>(Arrays.asList("single"));
        resolver.removeDuplicates(labels);
        
        assertEquals(1, labels.size());
    }

    // ==================== getConflictingLabels Tests ====================

    @Test
    public void testGetConflictingLabels_MultipleConflicts() {
        List<String> existing = Arrays.asList("a", "b", "c", "d");
        List<String> copied = Arrays.asList("b", "c", "e");
        
        List<String> conflicts = resolver.getConflictingLabels(existing, copied);
        
        assertEquals(2, conflicts.size());
        assertTrue(conflicts.contains("b"));
        assertTrue(conflicts.contains("c"));
    }

    @Test
    public void testGetConflictingLabels_NoConflicts() {
        List<String> existing = Arrays.asList("a", "b");
        List<String> copied = Arrays.asList("c", "d");
        
        List<String> conflicts = resolver.getConflictingLabels(existing, copied);
        
        assertTrue("Should be empty when no conflicts", conflicts.isEmpty());
    }

    @Test
    public void testGetConflictingLabels_NullInput() {
        List<String> conflicts1 = resolver.getConflictingLabels(null, Arrays.asList("a"));
        List<String> conflicts2 = resolver.getConflictingLabels(Arrays.asList("a"), null);
        
        assertTrue(conflicts1.isEmpty());
        assertTrue(conflicts2.isEmpty());
    }

    // ==================== hasSuffix Tests ====================

    @Test
    public void testHasSuffix_WithSuffix() {
        assertTrue("label.0 should have suffix", resolver.hasSuffix("label.0"));
        assertTrue("label.123 should have suffix", resolver.hasSuffix("label.123"));
        assertTrue("my.label.5 should have suffix", resolver.hasSuffix("my.label.5"));
    }

    @Test
    public void testHasSuffix_WithoutSuffix() {
        assertFalse("label should not have suffix", resolver.hasSuffix("label"));
        assertFalse("label.abc should not have suffix", resolver.hasSuffix("label.abc"));
        assertFalse("Empty should not have suffix", resolver.hasSuffix(""));
        assertFalse("Null should not have suffix", resolver.hasSuffix(null));
    }

    @Test
    public void testHasSuffix_EdgeCases() {
        assertFalse("Trailing dot should not count", resolver.hasSuffix("label."));
        assertFalse("Dot only should not count", resolver.hasSuffix("."));
        assertTrue("Single digit suffix", resolver.hasSuffix("x.0"));
    }

    // ==================== stripSuffix Tests ====================

    @Test
    public void testStripSuffix_WithSuffix() {
        assertEquals("label", resolver.stripSuffix("label.0"));
        assertEquals("my.label", resolver.stripSuffix("my.label.5"));
    }

    @Test
    public void testStripSuffix_WithoutSuffix() {
        assertEquals("label", resolver.stripSuffix("label"));
        assertEquals("label.abc", resolver.stripSuffix("label.abc"));
    }

    @Test
    public void testStripSuffix_EmptyAndNull() {
        assertEquals("", resolver.stripSuffix(""));
        assertNull(resolver.stripSuffix(null));
    }

    // ==================== isValidLabel Tests ====================

    @Test
    public void testIsValidLabel() {
        assertTrue("Normal label is valid", resolver.isValidLabel("myLabel"));
        assertTrue("Empty label is valid", resolver.isValidLabel(""));
        assertFalse("Null label is invalid", resolver.isValidLabel(null));
    }

    // ==================== findOrphanedLabels Tests ====================

    @Test
    public void testFindOrphanedLabels_NullInputs() {
        Set<String> orphaned = resolver.findOrphanedLabels(null, null);
        assertTrue("Null inputs should return empty set", orphaned.isEmpty());
    }

    // ==================== hasOrphanedCouplingReference Tests ====================

    @Test
    public void testHasOrphanedCouplingReference_NoOrphans() {
        String[] couplingLabels = {"a", "b"};
        Set<String> remaining = new HashSet<>(Arrays.asList("a", "b", "c"));
        
        assertFalse(resolver.hasOrphanedCouplingReference(couplingLabels, remaining));
    }

    @Test
    public void testHasOrphanedCouplingReference_WithOrphans() {
        String[] couplingLabels = {"a", "b"};
        Set<String> remaining = new HashSet<>(Arrays.asList("a", "c"));
        
        assertTrue("'b' is orphaned", resolver.hasOrphanedCouplingReference(couplingLabels, remaining));
    }

    @Test
    public void testHasOrphanedCouplingReference_EmptyCouplingLabels() {
        String[] couplingLabels = {};
        Set<String> remaining = new HashSet<>(Arrays.asList("a", "b"));
        
        assertFalse(resolver.hasOrphanedCouplingReference(couplingLabels, remaining));
    }

    @Test
    public void testHasOrphanedCouplingReference_NullInputs() {
        assertFalse(resolver.hasOrphanedCouplingReference(null, new HashSet<>()));
        assertFalse(resolver.hasOrphanedCouplingReference(new String[]{"a"}, null));
    }

    @Test
    public void testHasOrphanedCouplingReference_IgnoresEmptyLabels() {
        String[] couplingLabels = {"", "a", ""};
        Set<String> remaining = new HashSet<>(Arrays.asList("a"));
        
        assertFalse("Empty labels should be ignored", resolver.hasOrphanedCouplingReference(couplingLabels, remaining));
    }

    // ==================== Integration-like Tests ====================

    @Test
    public void testTypicalCopyScenario() {
        // Simulating a typical copy operation where labels need renaming
        List<String> existingLabels = Arrays.asList("node1", "node2", "gnd");
        List<String> copiedLabels = new ArrayList<>(Arrays.asList("node1", "node2", "out"));
        
        // Check conflict
        assertTrue(resolver.hasLabelConflict(existingLabels, copiedLabels));
        
        // Find non-conflicting suffix
        int suffix = resolver.findNonConflictingSuffixIndex(copiedLabels, existingLabels);
        assertEquals(0, suffix); // .0 doesn't conflict
        
        // Rename
        List<String> renamed = new ArrayList<>();
        for (String label : copiedLabels) {
            if (!label.isEmpty()) {
                renamed.add(resolver.renameWithSuffix(label, suffix));
            }
        }
        
        assertEquals(Arrays.asList("node1.0", "node2.0", "out.0"), renamed);
        
        // Verify no more conflicts
        assertFalse(resolver.hasLabelConflict(existingLabels, renamed));
    }

    @Test
    public void testTypicalCopyScenarioWithExistingSuffixes() {
        // Scenario: user has already copied once, now copying again
        List<String> existingLabels = Arrays.asList("node1", "node1.0", "node1.1");
        List<String> copiedLabels = Arrays.asList("node1");
        
        int suffix = resolver.findNonConflictingSuffixIndex(copiedLabels, existingLabels);
        assertEquals("Should find .2 as first available", 2, suffix);
        
        String renamed = resolver.renameWithSuffix("node1", suffix);
        assertEquals("node1.2", renamed);
    }

    // ==================== extractUniqueLabels Tests ====================

    @Test
    public void testExtractUniqueLabels_NullInput() {
        Set<CircuitLabel> labels = resolver.extractUniqueLabels(null);
        assertTrue(labels.isEmpty());
    }

    @Test
    public void testExtractUniqueLabels_EmptyInput() {
        Set<CircuitLabel> labels = resolver.extractUniqueLabels(Collections.emptyList());
        assertTrue(labels.isEmpty());
    }
}
