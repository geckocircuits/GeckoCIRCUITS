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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

/**
 * Tests for CompressorIntMatrix - data compression/decompression for integer matrices.
 */
public class CompressorIntMatrixTest {

    @Before
    public void setUp() {
        // Clear any cached data before each test
        CompressorIntMatrix.clearCache();
    }

    @After
    public void tearDown() {
        CompressorIntMatrix.clearCache();
    }

    // ====================================================
    // Basic Compression/Decompression Tests
    // ====================================================

    @Test
    public void testCompressDecompress_SmallMatrix() {
        int[][] original = {
            {1, 2, 3},
            {4, 5, 6}
        };
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        assertEquals(original.length, decompressed.length);
        assertEquals(original[0].length, decompressed[0].length);
        
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                assertEquals("Value mismatch at [" + i + "][" + j + "]",
                    original[i][j], decompressed[i][j]);
            }
        }
    }

    @Test
    public void testCompressDecompress_LargeMatrix() {
        int rows = 100;
        int cols = 50;
        int[][] original = new int[rows][cols];
        
        // Fill with deterministic pattern
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                original[i][j] = i * cols + j;
            }
        }
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                assertEquals(original[i][j], decompressed[i][j]);
            }
        }
    }

    @Test
    public void testCompressDecompress_SingleRow() {
        int[][] original = {{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}};
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        assertEquals(1, decompressed.length);
        assertArrayEquals(original[0], decompressed[0]);
    }

    @Test
    public void testCompressDecompress_SingleColumn() {
        int[][] original = {
            {1}, {2}, {3}, {4}, {5}
        };
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i][0], decompressed[i][0]);
        }
    }

    // ====================================================
    // Empty/Edge Case Tests
    // ====================================================

    @Test
    public void testCompressDecompress_EmptyMatrix() {
        int[][] original = new int[0][0];
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        assertEquals(0, decompressed.length);
    }

    @Test
    public void testCompressDecompress_ZeroValues() {
        int[][] original = {
            {0, 0, 0},
            {0, 0, 0}
        };
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < original.length; i++) {
            assertArrayEquals(original[i], decompressed[i]);
        }
    }

    @Test
    public void testCompressDecompress_NegativeValues() {
        int[][] original = {
            {-100, -50, 0},
            {50, 100, -Integer.MAX_VALUE / 2}
        };
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < original.length; i++) {
            assertArrayEquals(original[i], decompressed[i]);
        }
    }

    @Test
    public void testCompressDecompress_LargeIntValues() {
        int[][] original = {
            {Integer.MAX_VALUE, Integer.MIN_VALUE},
            {Integer.MAX_VALUE / 2, Integer.MIN_VALUE / 2}
        };
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < original.length; i++) {
            assertArrayEquals(original[i], decompressed[i]);
        }
    }

    // ====================================================
    // Compression Properties Tests
    // ====================================================

    @Test
    public void testCompressionRatio_Recorded() {
        int[][] original = new int[100][100];
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                original[i][j] = i + j;  // Simple pattern - should compress well
            }
        }
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        
        // Compression ratio should be positive
        assertTrue("Compression ratio should be positive", compressor.compressionRatio > 0);
    }

    @Test
    public void testCompressionTime_Recorded() {
        int[][] original = new int[50][50];
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        
        // Time should be non-negative
        assertTrue("Compression time should be >= 0", compressor.compressionTime >= 0);
    }

    @Test
    public void testGetCompressedMemory() {
        int[][] original = new int[10][10];
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        
        int compressedMemory = compressor.getCompressedMemory();
        assertTrue("Compressed memory should be positive", compressedMemory > 0);
    }

    // ====================================================
    // Data Integrity Tests
    // ====================================================

    @Test
    public void testDataIntegrity_RandomValues() {
        int rows = 20;
        int cols = 30;
        int[][] original = new int[rows][cols];
        
        // Pseudo-random pattern (deterministic)
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                original[i][j] = (i * 17 + j * 31) % 1000 - 500;
            }
        }
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                assertEquals("Data corruption at [" + i + "][" + j + "]",
                    original[i][j], decompressed[i][j]);
            }
        }
    }

    @Test
    public void testDataIntegrity_RepeatedPatterns() {
        // Highly compressible data
        int[][] original = new int[50][50];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                original[i][j] = 42;  // All same value
            }
        }
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                assertEquals(42, decompressed[i][j]);
            }
        }
    }

    @Test
    public void testDataIntegrity_AlternatingValues() {
        int[][] original = new int[20][20];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                original[i][j] = ((i + j) % 2 == 0) ? 1 : -1;
            }
        }
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        int[][] decompressed = compressor.deCompress();
        
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                assertEquals(original[i][j], decompressed[i][j]);
            }
        }
    }

    // ====================================================
    // Multiple Decompression Tests
    // ====================================================

    @Test
    public void testMultipleDecompressions() {
        int[][] original = {
            {1, 2, 3},
            {4, 5, 6}
        };
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(original);
        
        // Decompress multiple times
        int[][] first = compressor.deCompress();
        int[][] second = compressor.deCompress();
        int[][] third = compressor.deCompress();
        
        // All should match original
        for (int i = 0; i < original.length; i++) {
            assertArrayEquals(original[i], first[i]);
            assertArrayEquals(original[i], second[i]);
            assertArrayEquals(original[i], third[i]);
        }
    }

    // ====================================================
    // Cache Clearing Test
    // ====================================================

    @Test
    public void testClearCache() {
        // Clear should not throw
        CompressorIntMatrix.clearCache();
        
        // Compression should still work after clearing - use a larger array to ensure 
        // sufficient data for the compression algorithm
        int[][] data = new int[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                data[i][j] = i * 10 + j;
            }
        }
        
        CompressorIntMatrix compressor = new CompressorIntMatrix(data);
        int[][] decompressed = compressor.deCompress();
        
        // Verify dimensions
        assertEquals(5, decompressed.length);
        assertEquals(5, decompressed[0].length);
        
        // Verify data integrity
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                assertEquals(data[i][j], decompressed[i][j]);
            }
        }
    }
}
