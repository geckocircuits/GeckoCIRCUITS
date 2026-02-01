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
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for ContainsMatcher - matches strings that contain the search word anywhere.
 */
public class ContainsMatcherTest {

    private ContainsMatcher matcher;

    @Before
    public void setUp() {
        matcher = new ContainsMatcher();
    }

    // ====================================================
    // Basic Match Tests
    // ====================================================

    @Test
    public void testMatches_ExactString() {
        assertTrue("Should match exact string", matcher.matches("hello", "hello"));
    }

    @Test
    public void testMatches_ContainsAtBeginning() {
        assertTrue("Should match string at beginning", matcher.matches("hello world", "hello"));
    }

    @Test
    public void testMatches_ContainsInMiddle() {
        assertTrue("Should match string in middle", matcher.matches("hello world", "lo wo"));
    }

    @Test
    public void testMatches_ContainsAtEnd() {
        assertTrue("Should match string at end", matcher.matches("hello world", "world"));
    }

    @Test
    public void testMatches_SingleCharacter() {
        assertTrue("Should match single character", matcher.matches("hello", "e"));
    }

    // ====================================================
    // Case Sensitivity Tests
    // ====================================================

    @Test
    public void testMatches_CaseSensitive_MatchingCase() {
        assertTrue("Should match when case matches", matcher.matches("Hello", "Hello"));
    }

    @Test
    public void testMatches_CaseSensitive_DifferentCase() {
        assertFalse("Should NOT match with different case (case-sensitive)", matcher.matches("Hello", "hello"));
    }

    @Test
    public void testMatches_MixedCase_Matching() {
        assertTrue("Should match mixed case when it matches", matcher.matches("HelloWorld", "loWo"));
    }

    // ====================================================
    // Non-Match Tests
    // ====================================================

    @Test
    public void testMatches_NoMatch_Different() {
        assertFalse("Should not match different string", matcher.matches("hello", "xyz"));
    }

    @Test
    public void testMatches_NoMatch_Substring_NotContained() {
        assertFalse("Should not match if substring not contained", matcher.matches("hello", "xyz"));
    }

    @Test
    public void testMatches_NoMatch_Partial() {
        assertFalse("Should not match partial substring not in data", matcher.matches("hello", "world"));
    }

    // ====================================================
    // Edge Cases - Empty Strings
    // ====================================================

    @Test
    public void testMatches_EmptySearchWord() {
        assertTrue("Empty string should match any string (substring semantics)", matcher.matches("hello", ""));
    }

    @Test
    public void testMatches_EmptyDataWord() {
        assertFalse("Non-empty search word should not match empty data", matcher.matches("", "hello"));
    }

    @Test
    public void testMatches_BothEmpty() {
        assertTrue("Empty should contain empty string", matcher.matches("", ""));
    }

    // ====================================================
    // Whitespace Tests
    // ====================================================

    @Test
    public void testMatches_WithWhitespace_Beginning() {
        assertTrue("Should match with leading whitespace", matcher.matches("  hello", "hello"));
    }

    @Test
    public void testMatches_WithWhitespace_End() {
        assertTrue("Should match with trailing whitespace", matcher.matches("hello  ", "hello"));
    }

    @Test
    public void testMatches_WithWhitespace_Both() {
        assertTrue("Should match with surrounding whitespace", matcher.matches("  hello  ", "hello"));
    }

    @Test
    public void testMatches_WhitespaceInSearch() {
        assertTrue("Should match whitespace in search", matcher.matches("hello world", " "));
    }

    @Test
    public void testMatches_MultipleSpaces() {
        assertTrue("Should match multiple spaces", matcher.matches("hello  world", "  "));
    }

    // ====================================================
    // Special Characters Tests
    // ====================================================

    @Test
    public void testMatches_WithSpecialCharacters() {
        assertTrue("Should match special characters", matcher.matches("hello@world", "@"));
    }

    @Test
    public void testMatches_WithNumbers() {
        assertTrue("Should match numbers", matcher.matches("hello123world", "123"));
    }

    @Test
    public void testMatches_WithSymbols() {
        assertTrue("Should match symbols", matcher.matches("hello#world", "#"));
    }

    @Test
    public void testMatches_Dots() {
        assertTrue("Should match dots", matcher.matches("file.txt", ".txt"));
    }

    // ====================================================
    // Long String Tests
    // ====================================================

    @Test
    public void testMatches_LongString() {
        String longString = "The quick brown fox jumps over the lazy dog";
        assertTrue("Should find substring in long string", matcher.matches(longString, "quick brown"));
    }

    @Test
    public void testMatches_LongSearchTerm() {
        String dataWord = "The quick brown fox jumps over the lazy dog";
        String searchWord = "quick brown fox jumps over";
        assertTrue("Should find long substring", matcher.matches(dataWord, searchWord));
    }

    @Test
    public void testMatches_SearchTermLongerThanData() {
        assertFalse("Search term longer than data should not match", matcher.matches("hi", "hello"));
    }

    // ====================================================
    // Interface Implementation Tests
    // ====================================================

    @Test
    public void testImplementsSuggestMatcher() {
        assertTrue("ContainsMatcher should implement SuggestMatcher", matcher instanceof SuggestMatcher);
    }

    @Test
    public void testMatches_ImplementedFromInterface() {
        // Verify the method is callable through interface reference
        SuggestMatcher matcherInterface = new ContainsMatcher();
        assertTrue("Should work through interface reference", matcherInterface.matches("hello", "ell"));
    }

    // ====================================================
    // Component Value Tests
    // ====================================================

    @Test
    public void testMatches_Resistor() {
        assertTrue("Should find 'R' in resistor value", matcher.matches("R100k", "100"));
    }

    @Test
    public void testMatches_Capacitor() {
        assertTrue("Should find 'C' in capacitor value", matcher.matches("C10u", "10u"));
    }

    @Test
    public void testMatches_SIPrefix() {
        assertTrue("Should find SI prefix", matcher.matches("100k", "k"));
    }

    // ====================================================
    // Multiple Matches Tests
    // ====================================================

    @Test
    public void testMatches_MultipleOccurrences() {
        assertTrue("Should match even if word appears multiple times", matcher.matches("hello hello hello", "hello"));
    }

    @Test
    public void testMatches_OverlappingMatches() {
        assertTrue("Should match overlapping pattern", matcher.matches("aaa", "aa"));
    }
}
