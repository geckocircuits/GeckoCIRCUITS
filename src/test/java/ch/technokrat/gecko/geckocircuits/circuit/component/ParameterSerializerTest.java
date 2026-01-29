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
package ch.technokrat.gecko.geckocircuits.circuit.component;

import ch.technokrat.gecko.geckocircuits.circuit.component.ParameterSerializer.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/**
 * Unit tests for ParameterSerializer.
 * Tests ASCII import/export of parameter values and names.
 */
public class ParameterSerializerTest {
    
    private ParameterSerializer serializer;
    
    @Before
    public void setUp() {
        serializer = new ParameterSerializer();
    }
    
    // ===== Constructor Tests =====
    
    @Test
    public void testDefaultConstructor() {
        assertEquals(ParameterSerializer.DEFAULT_ARRAY_SIZE, serializer.getArraySize());
    }
    
    @Test
    public void testCustomArraySize() {
        ParameterSerializer custom = new ParameterSerializer(100);
        assertEquals(100, custom.getArraySize());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testZeroArraySize() {
        new ParameterSerializer(0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeArraySize() {
        new ParameterSerializer(-5);
    }
    
    // ===== Export Values Tests =====
    
    @Test
    public void testExportValuesEmpty() {
        String result = serializer.exportValuesToASCII(new double[0]);
        assertEquals("0\n\n", result);
    }
    
    @Test
    public void testExportValuesNull() {
        String result = serializer.exportValuesToASCII(null);
        assertEquals("0\n", result);
    }
    
    @Test
    public void testExportValuesSingle() {
        String result = serializer.exportValuesToASCII(new double[]{100.0});
        assertTrue(result.startsWith("1\n"));
        assertTrue(result.contains("100"));
    }
    
    @Test
    public void testExportValuesMultiple() {
        String result = serializer.exportValuesToASCII(new double[]{1.0, 2.5, 3.14159});
        assertTrue(result.startsWith("3\n"));
        assertTrue(result.contains("1.0"));
        assertTrue(result.contains("2.5"));
        assertTrue(result.contains("3.14159"));
    }
    
    @Test
    public void testExportValuesScientific() {
        String result = serializer.exportValuesToASCII(new double[]{1e-9, 1e10});
        assertTrue(result.contains("E") || result.contains("e")); // Scientific notation
    }
    
    // ===== Export Names Tests =====
    
    @Test
    public void testExportNamesNull() {
        String result = serializer.exportNamesToASCII(null);
        assertEquals("0\n", result);
    }
    
    @Test
    public void testExportNamesEmpty() {
        String result = serializer.exportNamesToASCII(new String[0]);
        assertEquals("0\n", result);
    }
    
    @Test
    public void testExportNamesSingle() {
        String result = serializer.exportNamesToASCII(new String[]{"resistance"});
        assertEquals("1\nresistance\n", result);
    }
    
    @Test
    public void testExportNamesMultiple() {
        String result = serializer.exportNamesToASCII(new String[]{"R", "L", "C"});
        assertEquals("3\nR\nL\nC\n", result);
    }
    
    @Test
    public void testExportNamesWithNulls() {
        // Trailing nulls should be trimmed
        String result = serializer.exportNamesToASCII(new String[]{"R", "L", null, null});
        assertEquals("2\nR\nL\n", result);
    }
    
    @Test
    public void testExportNamesWithMiddleNull() {
        // Middle nulls preserved
        String result = serializer.exportNamesToASCII(new String[]{"R", null, "C"});
        assertEquals("3\nR\n\nC\n", result);
    }
    
    // ===== Import Values Tests =====
    
    @Test
    public void testImportValuesEmpty() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("0");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(ParameterSerializer.DEFAULT_ARRAY_SIZE, values.length);
    }
    
    @Test
    public void testImportValuesSingle() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("1\n100.0");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(100.0, values[0], 1e-10);
    }
    
    @Test
    public void testImportValuesMultiple() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\n1.0 2.5 3.14159");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(1.0, values[0], 1e-10);
        assertEquals(2.5, values[1], 1e-10);
        assertEquals(3.14159, values[2], 1e-5);
    }
    
    @Test
    public void testImportValuesMultiLine() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\n1.0\n2.5\n3.14159");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(1.0, values[0], 1e-10);
        assertEquals(2.5, values[1], 1e-10);
    }
    
    @Test
    public void testImportValuesScientific() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("2\n1.5E-9 2.3E10");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(1.5e-9, values[0], 1e-15);
        assertEquals(2.3e10, values[1], 1e5);
    }
    
    @Test(expected = ParseException.class)
    public void testImportValuesInvalidCount() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("abc");
        serializer.importValuesFromASCII(tok);
    }
    
    @Test(expected = ParseException.class)
    public void testImportValuesNegativeCount() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("-5");
        serializer.importValuesFromASCII(tok);
    }
    
    @Test(expected = ParseException.class)
    public void testImportValuesTooManyParameters() throws ParseException {
        ParameterSerializer small = new ParameterSerializer(5);
        TokenizerAdapter tok = small.createTokenizer("10\n1 2 3 4 5 6 7 8 9 10");
        small.importValuesFromASCII(tok);
    }
    
    @Test(expected = ParseException.class)
    public void testImportValuesInvalidValue() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("2\n1.0 xyz");
        serializer.importValuesFromASCII(tok);
    }
    
    @Test(expected = ParseException.class)
    public void testImportValuesMissingValues() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("5\n1.0 2.0");
        serializer.importValuesFromASCII(tok);
    }
    
    // ===== Import Names Tests =====
    
    @Test
    public void testImportNamesEmpty() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("0");
        String[] names = serializer.importNamesFromASCII(tok);
        assertEquals(ParameterSerializer.DEFAULT_ARRAY_SIZE, names.length);
    }
    
    @Test
    public void testImportNamesSingle() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("1\nresistance");
        String[] names = serializer.importNamesFromASCII(tok);
        assertEquals("resistance", names[0]);
    }
    
    @Test
    public void testImportNamesMultiple() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\nR\nL\nC");
        String[] names = serializer.importNamesFromASCII(tok);
        assertEquals("R", names[0]);
        assertEquals("L", names[1]);
        assertEquals("C", names[2]);
    }
    
    @Test
    public void testImportNamesWithEmptyLine() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\nR\n\nC");
        String[] names = serializer.importNamesFromASCII(tok);
        assertEquals("R", names[0]);
        assertNull(names[1]); // Empty line becomes null
        assertEquals("C", names[2]);
    }
    
    @Test(expected = ParseException.class)
    public void testImportNamesTooMany() throws ParseException {
        ParameterSerializer small = new ParameterSerializer(2);
        TokenizerAdapter tok = small.createTokenizer("5\na\nb\nc\nd\ne");
        small.importNamesFromASCII(tok);
    }
    
    // ===== Combined Import/Export Tests =====
    
    @Test
    public void testExportImportRoundTrip() throws ParseException {
        double[] originalValues = {100.0, 0.001, 1e-6};
        String[] originalNames = {"R", "L", "C"};
        
        String exported = serializer.exportToASCII(originalValues, originalNames);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        ImportResult result = serializer.importFromASCII(tok);
        
        assertEquals(100.0, result.getValue(0), 1e-10);
        assertEquals(0.001, result.getValue(1), 1e-10);
        assertEquals(1e-6, result.getValue(2), 1e-12);
        assertEquals("R", result.getName(0));
        assertEquals("L", result.getName(1));
        assertEquals("C", result.getName(2));
    }
    
    @Test
    public void testImportResult() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("2\n1.0 2.0\n2\nA\nB");
        ImportResult result = serializer.importFromASCII(tok);
        
        double[] values = result.getValues();
        String[] names = result.getNames();
        
        assertEquals(1.0, values[0], 1e-10);
        assertEquals(2.0, values[1], 1e-10);
        assertEquals("A", names[0]);
        assertEquals("B", names[1]);
    }
    
    // ===== Tokenizer Tests =====
    
    @Test
    public void testListTokenizerBasic() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("hello world", "foo bar"));
        
        assertEquals("hello", tok.nextToken());
        assertEquals("world", tok.nextToken());
        assertEquals("foo", tok.nextToken());
        assertEquals("bar", tok.nextToken());
        assertNull(tok.nextToken());
    }
    
    @Test
    public void testListTokenizerHasMore() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("a b"));
        
        assertTrue(tok.hasMore());
        tok.nextToken();
        assertTrue(tok.hasMore());
        tok.nextToken();
        assertFalse(tok.hasMore());
    }
    
    @Test
    public void testListTokenizerNextLine() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("first line", "second line"));
        
        assertEquals("first line", tok.nextLine());
        assertEquals("second line", tok.nextLine());
        assertNull(tok.nextLine());
    }
    
    @Test
    public void testListTokenizerMixedUsage() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("3", "1.0 2.0 3.0", "name1", "name2"));
        
        assertEquals("3", tok.nextToken());
        assertEquals("1.0", tok.nextToken());
        assertEquals("2.0", tok.nextToken());
        assertEquals("3.0", tok.nextToken());
        assertEquals("name1", tok.nextLine());
        assertEquals("name2", tok.nextLine());
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalComponentSerialization() throws ParseException {
        // Simulate typical RLC component
        double[] params = new double[40];
        params[0] = 1000.0;  // R
        params[1] = 0.01;    // L
        params[2] = 1e-6;    // C
        
        String[] names = new String[40];
        names[0] = "resistance";
        names[1] = "inductance";
        names[2] = "capacitance";
        
        String exported = serializer.exportToASCII(params, names);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        ImportResult result = serializer.importFromASCII(tok);
        
        assertEquals(1000.0, result.getValue(0), 1e-10);
        assertEquals(0.01, result.getValue(1), 1e-10);
        assertEquals(1e-6, result.getValue(2), 1e-12);
        assertEquals("resistance", result.getName(0));
    }
    
    @Test
    public void testSwitchParameterSerialization() throws ParseException {
        // Simulate switch parameters (thyristor)
        double[] params = {0.01, 1e6, 0.7, 1e-6, 1e-6, 0.0};
        String[] names = {"rOn", "rOff", "uF", "tRec", "tFwd", "unused"};
        
        String exported = serializer.exportToASCII(params, names);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        ImportResult result = serializer.importFromASCII(tok);
        
        assertEquals(0.01, result.getValue(0), 1e-10);
        assertEquals(1e6, result.getValue(1), 1e3);
        assertEquals(0.7, result.getValue(2), 1e-10);
        assertEquals("rOn", result.getName(0));
        assertEquals("tRec", result.getName(3));
    }
    
    @Test
    public void testEmptyComponentSerialization() throws ParseException {
        String exported = serializer.exportToASCII(new double[0], new String[0]);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        ImportResult result = serializer.importFromASCII(tok);
        
        // Should return arrays of default size
        assertEquals(40, result.getValues().length);
        assertEquals(40, result.getNames().length);
    }
    
    @Test
    public void testDoubleFormattingPrecision() throws ParseException {
        double[] original = {1.234567890123, 0.000000001, 123456789.0};

        String exported = serializer.exportValuesToASCII(original);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        double[] imported = serializer.importValuesFromASCII(tok);

        // Values should be close but not necessarily identical due to formatting
        assertEquals(original[0], imported[0], 1e-6);
        assertEquals(original[1], imported[1], 1e-15);
        assertEquals(original[2], imported[2], 1000.0); // Larger tolerance for large numbers with scientific notation
    }

    // ===== Edge Cases and Boundary Tests =====

    @Test
    public void testExportZeroValue() {
        String result = serializer.exportValuesToASCII(new double[]{0.0});
        assertTrue(result.startsWith("1\n"));
        assertTrue(result.contains("0"));
    }

    @Test
    public void testExportNegativeValues() {
        String result = serializer.exportValuesToASCII(new double[]{-1.5, -1e-10});
        assertTrue(result.startsWith("2\n"));
        assertTrue(result.contains("-1.5"));
    }

    @Test
    public void testImportValuesMissingNewline() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("2 1.0 2.0");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(1.0, values[0], 1e-10);
        assertEquals(2.0, values[1], 1e-10);
    }

    @Test(expected = ParseException.class)
    public void testImportValuesInvalidDouble() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("1\n1.0.0");
        serializer.importValuesFromASCII(tok);
    }

    @Test(expected = ParseException.class)
    public void testImportValuesEndOfInput() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("5\n1.0");
        serializer.importValuesFromASCII(tok);
    }

    @Test(expected = ParseException.class)
    public void testImportNamesEndOfInput() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\nA\nB");
        serializer.importNamesFromASCII(tok);
    }

    @Test
    public void testListTokenizerEmpty() {
        TokenizerAdapter tok = new ListTokenizer(new ArrayList<>());
        assertNull(tok.nextToken());
        assertFalse(tok.hasMore());
    }

    @Test
    public void testListTokenizerEmptyLines() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("", "  ", "a"));
        assertEquals("a", tok.nextToken());
    }

    @Test
    public void testListTokenizerMultipleSpaces() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("a    b    c"));
        assertEquals("a", tok.nextToken());
        assertEquals("b", tok.nextToken());
        assertEquals("c", tok.nextToken());
        assertNull(tok.nextToken());
    }

    @Test
    public void testExportNamesAllNull() {
        String result = serializer.exportNamesToASCII(new String[]{null, null});
        assertEquals("0\n", result);
    }

    @Test
    public void testExportNamesSomeNull() {
        String result = serializer.exportNamesToASCII(new String[]{"A", null, null});
        assertEquals("1\nA\n", result);
    }

    @Test
    public void testImportResultGetValue() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\n1.0 2.0 3.0\n0");
        ImportResult result = serializer.importFromASCII(tok);
        assertEquals(1.0, result.getValue(0), 1e-10);
        assertEquals(2.0, result.getValue(1), 1e-10);
        assertEquals(3.0, result.getValue(2), 1e-10);
    }

    @Test
    public void testImportResultGetName() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("0\n3\nA\nB\nC");
        ImportResult result = serializer.importFromASCII(tok);
        assertEquals("A", result.getName(0));
        assertEquals("B", result.getName(1));
        assertEquals("C", result.getName(2));
    }

    @Test
    public void testExportVerySmallNumbers() {
        String result = serializer.exportValuesToASCII(new double[]{1e-20, 1e-30});
        assertTrue(result.contains("E") || result.contains("e"));
    }

    @Test
    public void testExportVeryLargeNumbers() {
        String result = serializer.exportValuesToASCII(new double[]{1e20, 1e30});
        assertTrue(result.contains("E") || result.contains("e"));
    }

    @Test
    public void testImportValuesZero() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("3\n0 0.0 -0.0");
        double[] values = serializer.importValuesFromASCII(tok);
        assertEquals(0.0, values[0], 1e-10);
        assertEquals(0.0, values[1], 1e-10);
        assertEquals(0.0, values[2], 1e-10);
    }

    @Test
    public void testSerializerCustomSize() {
        ParameterSerializer custom = new ParameterSerializer(10);
        assertEquals(10, custom.getArraySize());
    }

    @Test
    public void testListTokenizerBasicUsage() {
        TokenizerAdapter tok = new ListTokenizer(Arrays.asList("a b c", "d e f"));
        assertNotNull("First token should not be null", tok.nextToken());
        // Tokenizer behavior varies - just verify no exceptions
        assertNotNull("Next call should work", tok.nextToken());
    }

    @Test
    public void testTokenizerCreation() {
        TokenizerAdapter tok = serializer.createTokenizer("name1\nname2\n");
        assertNotNull("Tokenizer should be created", tok);
    }

    @Test
    public void testExportImportRoundTripNegativeValues() throws ParseException {
        double[] originalValues = {-100.0, -0.001, -1e-6};
        String exported = serializer.exportValuesToASCII(originalValues);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        double[] imported = serializer.importValuesFromASCII(tok);

        assertEquals(originalValues[0], imported[0], 1e-10);
        assertEquals(originalValues[1], imported[1], 1e-10);
        assertEquals(originalValues[2], imported[2], 1e-12);
    }

    @Test
    public void testExportImportRoundTripWithNullNames() throws ParseException {
        double[] originalValues = {1.0, 2.0};
        String[] originalNames = {null, "B"};

        String exported = serializer.exportToASCII(originalValues, originalNames);
        TokenizerAdapter tok = serializer.createTokenizer(exported);
        ImportResult result = serializer.importFromASCII(tok);

        assertEquals(1.0, result.getValue(0), 1e-10);
        assertNull(result.getName(0));
        assertEquals("B", result.getName(1));
    }

    @Test(expected = ParseException.class)
    public void testImportInvalidCountFormat() throws ParseException {
        TokenizerAdapter tok = serializer.createTokenizer("123abc");
        serializer.importValuesFromASCII(tok);
    }

    @Test
    public void testCreateTokenizerFromString() {
        TokenizerAdapter tok = serializer.createTokenizer("a b c");
        assertEquals("a", tok.nextToken());
        assertEquals("b", tok.nextToken());
        assertEquals("c", tok.nextToken());
    }
}
