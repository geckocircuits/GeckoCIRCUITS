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
 * Tests for EndsWithMatcher - matches strings that end with the search word.
 */
public class EndsWithMatcherTest {

    private EndsWithMatcher matcher;

    @Before
    public void setUp() {
        matcher = new EndsWithMatcher();
    }

    // ====================================================
    // Basic Match Tests
    // ====================================================

    @Test
    public void testMatches_ExactString() {
        assertTrue("Should match exact string", matcher.matches("hello", "hello"));
    }

    @Test
    public void testMatches_EndsWithAtEnd() {
        assertTrue("Should match string at end", matcher.matches("hello world", "world"));
    }

    @Test
    public void testMatches_SingleCharacter() {
        assertTrue("Should match single character at end", matcher.matches("hello", "o"));
    }

    @Test
    public void testMatches_LastWord() {
        assertTrue("Should match last word", matcher.matches("hello world", "orld"));
    }

    // ====================================================
    // Non-Match Tests
    // ====================================================

    @Test
    public void testMatches_NoMatch_AtBeginning() {
        assertFalse("Should not match string at beginning", matcher.matches("hello world", "hello"));
    }

    @Test
    public void testMatches_NoMatch_InMiddle() {
        assertFalse("Should not match string in middle", matcher.matches("hello world", "lo wo"));
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
        assertTrue("Should match lowercase end", matcher.matches("HELLO", "HELLO"));
    }

    @Test
    public void testMatches_CaseSensitive_MixedCase() {
        assertTrue("Should match mixed case end", matcher.matches("HelloWorld", "orld"));
    }

    // ====================================================
    // Edge Cases - Empty Strings
    // ====================================================

    @Test
    public void testMatches_EmptySearchWord() {
        assertTrue("Empty string should match any string (ends with semantics)", matcher.matches("hello", ""));
    }

    @Test
    public void testMatches_EmptyDataWord() {
        assertFalse("Non-empty search word should not match empty data", matcher.matches("", "hello"));
    }

    @Test
    public void testMatches_BothEmpty() {
        assertTrue("Empty should end with empty string", matcher.matches("", ""));
    }

    // ====================================================
    // Whitespace Tests
    // ====================================================

    @Test
    public void testMatches_WithTrailingWhitespace() {
        assertTrue("Should match with trailing whitespace", matcher.matches("hello  ", "  "));
    }

    @Test
    public void testMatches_WithLeadingWhitespace_InData() {
        assertTrue("Should match end even with leading whitespace in data", matcher.matches("  hello", "hello"));
    }

    @Test
    public void testMatches_EndWithSpace() {
        assertFalse("Should not end with space if data doesn't", matcher.matches("hello", " "));
    }

    // ====================================================
    // Special Characters Tests
    // ====================================================

    @Test
    public void testMatches_WithSpecialCharacters_End() {
        assertTrue("Should match special character at end", matcher.matches("hello@", "@"));
    }

    @Test
    public void testMatches_WithNumbers_End() {
        assertTrue("Should match numbers at end", matcher.matches("hello123", "123"));
    }

    @Test
    public void testMatches_Dots_End() {
        assertTrue("Should match dots at end", matcher.matches("file.txt", ".txt"));
    }

    @Test
    public void testMatches_SpecialCharacters_BeginningNotMatch() {
        assertFalse("Should not match special char if not at end", matcher.matches("hello@world", "@"));
    }

    // ====================================================
    // File Extension Tests
    // ====================================================

    @Test
    public void testMatches_JavaFileExtension() {
        assertTrue("Should match .java extension", matcher.matches("MyClass.java", ".java"));
    }

    @Test
    public void testMatches_IpesFileExtension() {
        assertTrue("Should match .ipes extension", matcher.matches("circuit.ipes", ".ipes"));
    }

    @Test
    public void testMatches_PropertiesFileExtension() {
        assertTrue("Should match .properties extension", matcher.matches("config.properties", "properties"));
    }

    @Test
    public void testMatches_OnlyExtension() {
        assertTrue("Should match entire extension", matcher.matches("config.properties", ".properties"));
    }

    // ====================================================
    // Long String Tests
    // ====================================================

    @Test
    public void testMatches_LongDataWord() {
        String longString = "The quick brown fox jumps over the lazy dog";
        assertTrue("Should find ending substring in long string", matcher.matches(longString, "lazy dog"));
    }

    @Test
    public void testMatches_LongSearchTerm() {
        String dataWord = "The quick brown fox jumps over the lazy dog";
        String searchWord = "jumps over the lazy dog";
        assertTrue("Should find long ending substring", matcher.matches(dataWord, searchWord));
    }

    @Test
    public void testMatches_SearchTermLongerThanData() {
        assertFalse("Search term longer than data should not match", matcher.matches("hi", "hello world"));
    }

    // ====================================================
    // Component Suffix Tests
    // ====================================================

    @Test
    public void testMatches_SuffixFilters() {
        assertTrue("Should match suffix for filtering", matcher.matches("component_v1", "_v1"));
    }

    @Test
    public void testMatches_NumberSuffix() {
        assertTrue("Should match number suffix", matcher.matches("GND1", "1"));
    }

    // ====================================================
    // Interface Implementation Tests
    // ====================================================

    @Test
    public void testImplementsSuggestMatcher() {
        assertTrue("EndsWithMatcher should implement SuggestMatcher", matcher instanceof SuggestMatcher);
    }

    @Test
    public void testMatches_ImplementedFromInterface() {
        SuggestMatcher matcherInterface = new EndsWithMatcher();
        assertTrue("Should work through interface reference", matcherInterface.matches("hello", "llo"));
    }

    // ====================================================
    // Reverse Patterns Tests
    // ====================================================

    @Test
    public void testMatches_RepeatedEndCharacters() {
        assertTrue("Should match repeated end character", matcher.matches("aaabbb", "bb"));
    }

    @Test
    public void testMatches_FullWordEnd() {
        assertTrue("Should match complete ending word", matcher.matches("hello-world", "world"));
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

    // ====================================================
    // Path Separator Tests
    // ====================================================

    @Test
    public void testMatches_FilePathEnding() {
        assertTrue("Should match file path ending", matcher.matches("/path/to/file.txt", "file.txt"));
    }

    @Test
    public void testMatches_DirectoryPath() {
        assertTrue("Should match directory name at end", matcher.matches("/path/to/directory", "directory"));
    }

    // ====================================================
    // URL Tests
    // ====================================================

    @Test
    public void testMatches_UrlDomain() {
        assertTrue("Should match domain suffix", matcher.matches("www.example.com", ".com"));
    }

    @Test
    public void testMatches_UrlPath() {
        assertTrue("Should match URL path end", matcher.matches("http://example.com/path/file.html", ".html"));
    }
}
