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
 * Tests for StartsWithMatcher - matches strings that begin with the search word.
 */
public class StartsWithMatcherTest {

    private StartsWithMatcher matcher;

    @Before
    public void setUp() {
        matcher = new StartsWithMatcher();
    }

    // ====================================================
    // Basic Match Tests
    // ====================================================

    @Test
    public void testMatches_ExactString() {
        assertTrue("Should match exact string", matcher.matches("hello", "hello"));
    }

    @Test
    public void testMatches_StartsWithAtBeginning() {
        assertTrue("Should match string at beginning", matcher.matches("hello world", "hello"));
    }

    @Test
    public void testMatches_SingleCharacter() {
        assertTrue("Should match single character at start", matcher.matches("hello", "h"));
    }

    @Test
    public void testMatches_FirstWord() {
        assertTrue("Should match first word", matcher.matches("hello world", "hel"));
    }

    // ====================================================
    // Non-Match Tests
    // ====================================================

    @Test
    public void testMatches_NoMatch_InMiddle() {
        assertFalse("Should not match string in middle", matcher.matches("hello world", "lo wo"));
    }

    @Test
    public void testMatches_NoMatch_AtEnd() {
        assertFalse("Should not match string at end", matcher.matches("hello world", "world"));
    }

    @Test
    public void testMatches_NoMatch_Different() {
        assertFalse("Should not match different string", matcher.matches("hello", "xyz"));
    }

    // ====================================================
    // Case Sensitivity Tests
    // ====================================================

    @Test
    public void testMatches_CaseSensitive_Lowercase() {
        assertTrue("Should match lowercase start", matcher.matches("Hello", "Hello"));
    }

    @Test
    public void testMatches_CaseSensitive_MixedCase() {
        assertTrue("Should match mixed case start", matcher.matches("HelloWorld", "Hell"));
    }

    // ====================================================
    // Edge Cases - Empty Strings
    // ====================================================

    @Test
    public void testMatches_EmptySearchWord() {
        assertTrue("Empty string should match any string (starts with semantics)", matcher.matches("hello", ""));
    }

    @Test
    public void testMatches_EmptyDataWord() {
        assertFalse("Non-empty search word should not match empty data", matcher.matches("", "hello"));
    }

    @Test
    public void testMatches_BothEmpty() {
        assertTrue("Empty should start with empty string", matcher.matches("", ""));
    }

    // ====================================================
    // Whitespace Tests
    // ====================================================

    @Test
    public void testMatches_WithLeadingWhitespace() {
        assertTrue("Should match with leading whitespace", matcher.matches("  hello", "  "));
    }

    @Test
    public void testMatches_WithTrailingWhitespace_InData() {
        assertTrue("Should match start even with trailing whitespace in data", matcher.matches("hello  ", "hello"));
    }

    @Test
    public void testMatches_StartWithSpace() {
        assertFalse("Should not start with space if data doesn't", matcher.matches("hello", " "));
    }

    // ====================================================
    // Special Characters Tests
    // ====================================================

    @Test
    public void testMatches_WithSpecialCharacters_Start() {
        assertTrue("Should match special character at start", matcher.matches("@hello", "@"));
    }

    @Test
    public void testMatches_WithNumbers_Start() {
        assertTrue("Should match numbers at start", matcher.matches("123hello", "123"));
    }

    @Test
    public void testMatches_Dots_Start() {
        assertTrue("Should match dots at start", matcher.matches(".txt", "."));
    }

    @Test
    public void testMatches_SpecialCharacters_MiddleNotMatch() {
        assertFalse("Should not match special char if not at start", matcher.matches("hello@world", "@"));
    }

    // ====================================================
    // Long String Tests
    // ====================================================

    @Test
    public void testMatches_LongDataWord() {
        String longString = "The quick brown fox jumps over the lazy dog";
        assertTrue("Should find beginning substring in long string", matcher.matches(longString, "The quick"));
    }

    @Test
    public void testMatches_LongSearchTerm() {
        String dataWord = "The quick brown fox jumps over the lazy dog";
        String searchWord = "The quick brown fox";
        assertTrue("Should find long beginning substring", matcher.matches(dataWord, searchWord));
    }

    @Test
    public void testMatches_SearchTermLongerThanData() {
        assertFalse("Search term longer than data should not match", matcher.matches("hi", "hello world"));
    }

    // ====================================================
    // Component Naming Tests
    // ====================================================

    @Test
    public void testMatches_ResistorPrefix() {
        assertTrue("Should match 'R' at start of resistor", matcher.matches("R100k", "R"));
    }

    @Test
    public void testMatches_CapacitorPrefix() {
        assertTrue("Should match 'C' at start of capacitor", matcher.matches("C10u", "C"));
    }

    @Test
    public void testMatches_LibraryPrefix() {
        assertTrue("Should match library prefix", matcher.matches("LIB_COMPONENT", "LIB"));
    }

    @Test
    public void testMatches_ComponentPrefixFilter() {
        assertTrue("Should match component prefix for filtering", matcher.matches("GND_1", "GND"));
    }

    // ====================================================
    // Interface Implementation Tests
    // ====================================================

    @Test
    public void testImplementsSuggestMatcher() {
        assertTrue("StartsWithMatcher should implement SuggestMatcher", matcher instanceof SuggestMatcher);
    }

    @Test
    public void testMatches_ImplementedFromInterface() {
        SuggestMatcher matcherInterface = new StartsWithMatcher();
        assertTrue("Should work through interface reference", matcherInterface.matches("hello", "hel"));
    }

    // ====================================================
    // Ordering/Prefix Tests
    // ====================================================

    @Test
    public void testMatches_AlphabeticalPrefixes() {
        assertTrue("A prefix should match", matcher.matches("ABC", "A"));
        assertTrue("AB prefix should match", matcher.matches("ABC", "AB"));
        assertTrue("ABC prefix should match", matcher.matches("ABC", "ABC"));
        assertFalse("B prefix should not match A start", matcher.matches("ABC", "B"));
    }

    // ====================================================
    // Multiple Matches at Start
    // ====================================================

    @Test
    public void testMatches_RepeatedStartCharacters() {
        assertTrue("Should match repeated start character", matcher.matches("aaabbb", "aa"));
    }

    @Test
    public void testMatches_FullWordStart() {
        assertTrue("Should match complete starting word", matcher.matches("hello-world", "hello"));
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testMatches_EntireStringAsSearch() {
        assertTrue("Entire string should match itself as search", matcher.matches("hello", "hello"));
    }

    @Test
    public void testMatches_ExceedingLength() {
        assertFalse("Search word exceeding data length should not match", matcher.matches("hi", "hello"));
    }
}
