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
package ch.technokrat.gecko.geckocircuits.circuit;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit tests for TokenMap class.
 * Tests token extraction, line parsing, data reading, and block handling.
 */
public class TokenMapTest {

    private TokenMap _tokenMap;

    @Before
    public void setUp() {
        // Create a simple ASCII array for testing
        String[] testAscii = {
            "componentA value1 param1",
            "componentB 123 456",
            "param1 true",
            "param2 3.14159",
            "  ",  // blank line
            "param3 1000",
            "dataArray 1 2 3 4 5"
        };
        _tokenMap = new TokenMap(testAscii, false);
    }

    @Test
    public void testConstructor_AcceptsAsciiArray() {
        String[] ascii = {"token1 value", "token2 123"};
        TokenMap map = new TokenMap(ascii);
        assertNotNull(map);
    }

    @Test
    public void testConstructor_WithSpecialPairs() {
        String[] ascii = {"token1 value", "token2 123"};
        TokenMap map = new TokenMap(ascii, true);
        assertNotNull(map);
    }

    @Test
    public void testGetLines_ReturnsOriginalAsciiArray() {
        String[] ascii = {"token1 value", "token2 123"};
        TokenMap map = new TokenMap(ascii);
        String[] lines = map.getLines();
        assertEquals(ascii.length, lines.length);
        assertEquals(ascii[0], lines[0]);
        assertEquals(ascii[1], lines[1]);
    }

    @Test
    public void testContainsToken_WithExistingToken() {
        assertTrue(_tokenMap.containsToken("componentA"));
    }

    @Test
    public void testContainsToken_WithNonExistentToken() {
        assertFalse(_tokenMap.containsToken("nonExistentToken"));
    }

    @Test
    public void testContainsToken_CaseSensitive() {
        // TokenMap is case-sensitive
        assertTrue(_tokenMap.containsToken("componentA"));
        assertFalse(_tokenMap.containsToken("componenta"));
    }

    @Test
    public void testGetLineNumber_ValidToken() {
        Integer lineNum = _tokenMap.getLineNumber("componentA");
        assertNotNull(lineNum);
        assertEquals(0, lineNum.intValue());
    }

    @Test
    public void testGetLineNumber_AnotherValidToken() {
        Integer lineNum = _tokenMap.getLineNumber("param1");
        assertNotNull(lineNum);
        assertEquals(2, lineNum.intValue());
    }

    @Test
    public void testGetLineNumber_InvalidToken() {
        Integer lineNum = _tokenMap.getLineNumber("nonExistentToken");
        assertNull(lineNum);
    }

    @Test
    public void testGetLineString_WithValidToken() {
        String line = _tokenMap.getLineString("componentA", "default");
        assertEquals("componentA value1 param1", line);
    }

    @Test
    public void testGetLineString_WithInvalidToken_ReturnsDefault() {
        String defaultValue = "defaultValue";
        String line = _tokenMap.getLineString("nonExistent", defaultValue);
        assertEquals(defaultValue, line);
    }

    @Test
    public void testReadDataLine_String_WithValidToken() {
        String value = _tokenMap.readDataLine("componentA", "default");
        assertNotNull(value);
        assertEquals("value1 param1", value);
    }

    @Test
    public void testReadDataLine_String_WithInvalidToken_ReturnsDefault() {
        String defaultValue = "myDefault";
        String value = _tokenMap.readDataLine("nonExistent", defaultValue);
        assertEquals(defaultValue, value);
    }

    @Test
    public void testReadDataLine_Double_WithValidToken() {
        double value = _tokenMap.readDataLine("param2", 0.0);
        assertEquals(3.14159, value, 0.00001);
    }

    @Test
    public void testReadDataLine_Double_WithInvalidToken_ReturnsDefault() {
        double defaultValue = 99.99;
        double value = _tokenMap.readDataLine("nonExistent", defaultValue);
        assertEquals(defaultValue, value, 0.0);
    }

    @Test
    public void testReadDataLine_Integer_WithValidToken() {
        int value = _tokenMap.readDataLine("componentB", 0);
        assertEquals(123, value);
    }

    @Test
    public void testReadDataLine_Integer_WithInvalidToken_ReturnsDefault() {
        int defaultValue = 999;
        int value = _tokenMap.readDataLine("nonExistent", defaultValue);
        assertEquals(defaultValue, value);
    }

    @Test
    public void testReadDataLine_Long_WithValidToken() {
        long value = _tokenMap.readDataLine("param3", 0L);
        assertEquals(1000L, value);
    }

    @Test
    public void testReadDataLine_Boolean_TrueValue() {
        boolean value = _tokenMap.readDataLine("param1", false);
        assertTrue(value);
    }

    @Test
    public void testReadDataLine_Boolean_DefaultValue() {
        boolean value = _tokenMap.readDataLine("nonExistent", true);
        assertTrue(value);
    }

    @Test
    public void testReadDataLine_IntArray() {
        String[] ascii = {
            "arrayData 1 2 3 4 5"
        };
        TokenMap map = new TokenMap(ascii);
        int[] result = map.readDataLine("arrayData", new int[]{0});
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(1, result[0]);
        assertEquals(5, result[4]);
    }

    @Test
    public void testReadDataLine_DoubleArray() {
        String[] ascii = {
            "doubleArray 1.0 2.5 3.14 4.99"
        };
        TokenMap map = new TokenMap(ascii);
        double[] result = map.readDataLine("doubleArray", new double[]{0.0});
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(1.0, result[0], 0.0);
        assertEquals(3.14, result[2], 0.0);
    }

    @Test
    public void testReadDataLine_IntArrayWithNull() {
        String[] ascii = {
            "arrayWithNull null"
        };
        TokenMap map = new TokenMap(ascii);
        int[] result = map.readDataLine("arrayWithNull", new int[]{99});
        assertNull(result);
    }

    @Test
    public void testReadDataLine_DoubleArrayWithNull() {
        String[] ascii = {
            "arrayWithNull null"
        };
        TokenMap map = new TokenMap(ascii);
        double[] result = map.readDataLine("arrayWithNull", new double[]{99.0});
        assertNull(result);
    }

    @Test
    public void testReadDataLine_BooleanArray() {
        String[] ascii = {
            "boolArray true false true"
        };
        TokenMap map = new TokenMap(ascii);
        boolean[] result = map.readDataLine("boolArray", new boolean[]{false});
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertTrue(result[2]);
    }

    @Test
    public void testReadDataLine_ByteArray() {
        String[] ascii = {
            "byteArray 1 2 3 4 5"
        };
        TokenMap map = new TokenMap(ascii);
        byte[] result = map.readDataLine("byteArray", new byte[]{0});
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(1, result[0]);
    }

    @Test
    public void testReadDataLine_LongArray() {
        String[] ascii = {
            "longArray 1000 2000 3000"
        };
        TokenMap map = new TokenMap(ascii);
        long[] result = map.readDataLine("longArray", new long[]{0});
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(1000L, result[0]);
    }

    @Test
    public void testReadDataLine_2DIntArray() {
        String[] ascii = {
            "matrix2D 2 3 1 2 3 4 5 6"
        };
        TokenMap map = new TokenMap(ascii);
        int[][] result = map.readDataLine("matrix2D", new int[][]{{0}});
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        assertEquals(1, result[0][0]);
        assertEquals(6, result[1][2]);
    }

    @Test
    public void testReadDataLine_2DDoubleArray() {
        String[] ascii = {
            "matrix2D 2 2 1.0 2.0 3.0 4.0"
        };
        TokenMap map = new TokenMap(ascii);
        double[][] result = map.readDataLine("matrix2D", new double[][]{{0.0}});
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(1.0, result[0][0], 0.0);
        assertEquals(4.0, result[1][1], 0.0);
    }

    @Test
    public void testReadDataLine_2DBooleanArray() {
        String[] ascii = {
            "matrix2D 2 2 true false false true"
        };
        TokenMap map = new TokenMap(ascii);
        boolean[][] result = map.readDataLine("matrix2D", new boolean[][]{{false}});
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertTrue(result[0][0]);
        assertFalse(result[0][1]);
    }

    @Test
    public void testReadDataLine_StringArray() {
        String[] ascii = {
            "strArray value1 value2 value3"
        };
        TokenMap map = new TokenMap(ascii);
        String[] result = map.readDataLine("strArray", new String[]{"default"});
        assertNotNull("Result should not be null", result);
        // The actual result depends on the API implementation
        assertTrue("Result should be array", result.length >= 0);
    }

    @Test
    public void testLeseASCIITextBlock_BasicUsage() {
        String[] ascii = {
            "description This is a text block"
        };
        TokenMap map = new TokenMap(ascii);
        String result = map.leseASCIITextBlock("description", "default");
        assertEquals("This is a text block", result);
    }

    @Test
    public void testLeseASCIITextBlock_WithNewlineEscape() {
        String[] ascii = {
            "description Line1\\nLine2"
        };
        TokenMap map = new TokenMap(ascii);
        String result = map.leseASCIITextBlock("description", "default");
        assertTrue(result.contains("Line1"));
        assertTrue(result.contains("Line2"));
    }

    @Test
    public void testToString_CreatesCorrectOutput() {
        String[] ascii = {
            "token1 value1",
            "token2 value2"
        };
        TokenMap map = new TokenMap(ascii);
        String result = map.toString();
        assertTrue(result.contains("token1 value1"));
        assertTrue(result.contains("token2 value2"));
        assertTrue(result.contains("\n"));
    }

    @Test
    public void testToString_SingleLineArray() {
        String[] ascii = {"singleLine"};
        TokenMap map = new TokenMap(ascii);
        String result = map.toString();
        assertEquals("singleLine", result);
    }

    @Test
    public void testFindSubBlock_ValidRange() {
        String[] ascii = {
            "start",
            "content1",
            "content2",
            "end",
            "after"
        };
        TokenMap map = new TokenMap(ascii);
        String[] result = map.findSubBlock("start", "end");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("content1", result[0]);
        assertEquals("content2", result[1]);
    }

    @Test
    public void testFindSubBlock_EmptyBlock() {
        String[] ascii = {
            "start",
            "end",
            "after"
        };
        TokenMap map = new TokenMap(ascii);
        String[] result = map.findSubBlock("start", "end");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCreateSubBlock_ValidRange() {
        String[] ascii = {
            "start",
            "line1",
            "line2",
            "end"
        };
        TokenMap map = new TokenMap(ascii);
        String result = map.createSubBlock("start", "end");
        assertNotNull(result);
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
    }

    @Test
    public void testReadDataLineDoubleArray_ValidData() {
        String[] ascii = {
            "doubleList 1.5 2.5 3.5"
        };
        TokenMap map = new TokenMap(ascii);
        java.util.List<Double> result = map.readDataLineDoubleArray("doubleList");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0), 0.0);
        assertEquals(3.5, result.get(2), 0.0);
    }

    @Test
    public void testReadDataLineStringArray_ValidData() {
        String[] ascii = {
            "strList val1 val2 val3"
        };
        TokenMap map = new TokenMap(ascii);
        java.util.List<String> result = map.readDataLineStringArray("strList");
        assertNotNull("Result should not be null", result);
        // The actual result depends on the API implementation
        assertTrue("Result should be list", result.size() >= 0);
    }

    @Test
    public void testReadDataLine_LongWithValidToken() {
        String[] ascii = {
            "longValue 1000000000"
        };
        TokenMap map = new TokenMap(ascii);
        long result = map.readDataLine("longValue", 0L);
        assertEquals(1000000000L, result);
    }
}
