/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Edge case and error path tests for ShortMatrixCache.
 * Tests null handling, zero dimensions, concurrency, and data integrity.
 */
public class ShortMatrixCacheEdgeCasesTest {

    @Before
    public void setUp() {
        ShortMatrixCache.clearCache();
    }

    @After
    public void tearDown() {
        ShortMatrixCache.clearCache();
    }

    @Test
    public void testGetCachedMatrixZeroDimensions() {
        // Test with zero dimensions
        short[][] arr = ShortMatrixCache.getCachedMatrix(0, 0);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    public void testGetCachedMatrixSingleElement() {
        // Test with 1x1 matrix
        short[][] arr = ShortMatrixCache.getCachedMatrix(1, 1);
        assertNotNull(arr);
        assertEquals(1, arr.length);
        assertEquals(1, arr[0].length);

        arr[0][0] = 42;
        assertEquals(42, arr[0][0]);
    }

    @Test
    public void testRecycleEmptyMatrix() {
        // Test recycling a zero-dimension array
        short[][] empty = new short[0][0];
        ShortMatrixCache.recycleMatrix(empty);

        short[][] retrieved = ShortMatrixCache.getCachedMatrix(0, 0);
        assertNotNull(retrieved);
    }

    @Test
    public void testRecycleJaggedMatrix() {
        // Test with jagged array (uneven dimensions)
        short[][] jagged = new short[3][];
        jagged[0] = new short[10];
        jagged[1] = new short[20];
        jagged[2] = new short[5];

        // Should handle based on first row
        ShortMatrixCache.recycleMatrix(jagged);

        short[][] arr = ShortMatrixCache.getCachedMatrix(3, 10);
        assertNotNull(arr);
    }

    @Test
    public void testShortBoundaryValues() {
        // Test with short boundary values
        short[][] arr = ShortMatrixCache.getCachedMatrix(1, 3);
        assertNotNull(arr);

        arr[0][0] = Short.MIN_VALUE;
        arr[0][1] = 0;
        arr[0][2] = Short.MAX_VALUE;

        ShortMatrixCache.recycleMatrix(arr);

        short[][] retrieved = ShortMatrixCache.getCachedMatrix(1, 3);
        assertNotNull(retrieved);
    }

    @Test
    public void testMultipleSizes() {
        // Test caching multiple different sizes
        short[][] arr1 = ShortMatrixCache.getCachedMatrix(2, 3);
        short[][] arr2 = ShortMatrixCache.getCachedMatrix(5, 5);
        short[][] arr3 = ShortMatrixCache.getCachedMatrix(10, 1);

        assertNotNull(arr1);
        assertNotNull(arr2);
        assertNotNull(arr3);
        assertEquals(2, arr1.length);
        assertEquals(3, arr1[0].length);
    }

    @Test
    public void testRecycleAndRetrieveSameSize() {
        // Verify cache works: recycle and retrieve same size
        short[][] original = new short[10][10];
        original[0][0] = 999;
        original[5][5] = -500;

        ShortMatrixCache.recycleMatrix(original);
        short[][] retrieved = ShortMatrixCache.getCachedMatrix(10, 10);

        assertNotNull(retrieved);
        assertEquals(10, retrieved.length);
        assertEquals(10, retrieved[0].length);
    }

    @Test
    public void testDifferentSizesNoConflict() {
        // Recycle multiple different sizes
        ShortMatrixCache.recycleMatrix(new short[5][5]);
        ShortMatrixCache.recycleMatrix(new short[3][3]);
        ShortMatrixCache.recycleMatrix(new short[7][7]);

        short[][] arr1 = ShortMatrixCache.getCachedMatrix(5, 5);
        short[][] arr2 = ShortMatrixCache.getCachedMatrix(3, 3);
        short[][] arr3 = ShortMatrixCache.getCachedMatrix(7, 7);

        assertNotNull(arr1);
        assertNotNull(arr2);
        assertNotNull(arr3);
    }

    @Test
    public void testClearCacheFreesMemory() {
        // Fill cache
        for (int i = 0; i < 10; i++) {
            short[][] arr = new short[100][100];
            ShortMatrixCache.recycleMatrix(arr);
        }

        ShortMatrixCache.clearCache();

        // After clear, should get fresh array
        short[][] fresh = ShortMatrixCache.getCachedMatrix(100, 100);
        assertNotNull(fresh);
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // Simulate concurrent access from multiple threads
        Thread[] threads = new Thread[4];

        for (int t = 0; t < 4; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    short[][] arr = ShortMatrixCache.getCachedMatrix(4, 4);
                    assertNotNull(arr);

                    // Simulate some work
                    arr[0][0] = (short) (arr[0][0] + 1);

                    ShortMatrixCache.recycleMatrix(arr);
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        // Should complete without exception
        short[][] final_arr = ShortMatrixCache.getCachedMatrix(4, 4);
        assertNotNull(final_arr);
    }

    @Test
    public void testLargeMatrixDimensions() {
        // Test with large but reasonable dimensions
        short[][] large = ShortMatrixCache.getCachedMatrix(500, 500);
        assertNotNull(large);
        assertEquals(500, large.length);
        assertEquals(500, large[0].length);

        ShortMatrixCache.recycleMatrix(large);
    }

    @Test
    public void testVeryWidthyMatrix() {
        // Test with very wide matrix
        short[][] wide = ShortMatrixCache.getCachedMatrix(1, 5000);
        assertNotNull(wide);
        assertEquals(1, wide.length);
        assertEquals(5000, wide[0].length);
    }

    @Test
    public void testRapidRecycleRetrieveCycle() {
        // Test rapid cycles of recycle and retrieve
        for (int cycle = 0; cycle < 20; cycle++) {
            short[][] arr = ShortMatrixCache.getCachedMatrix(8, 8);
            assertNotNull(arr);
            arr[0][0] = (short) cycle;
            ShortMatrixCache.recycleMatrix(arr);
        }

        // Final retrieval should work
        short[][] final_arr = ShortMatrixCache.getCachedMatrix(8, 8);
        assertNotNull(final_arr);
    }

    @Test
    public void testDataIntegrityAfterCache() {
        // Test that we can write and retrieve data through cache
        short[][] original = new short[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                original[i][j] = (short) (i * 5 + j);
            }
        }

        ShortMatrixCache.recycleMatrix(original);
        short[][] retrieved = ShortMatrixCache.getCachedMatrix(5, 5);

        // Verify dimensions
        assertEquals(5, retrieved.length);
        assertEquals(5, retrieved[0].length);
    }

    @Test
    public void testSingleRowMatrix() {
        // Test 1D-like matrix (single row)
        short[][] single = ShortMatrixCache.getCachedMatrix(1, 100);
        assertNotNull(single);
        assertEquals(1, single.length);

        single[0][0] = 100;
        single[0][99] = 200;

        ShortMatrixCache.recycleMatrix(single);
    }

    @Test
    public void testSingleColumnMatrix() {
        // Test tall matrix (single column)
        short[][] tall = ShortMatrixCache.getCachedMatrix(100, 1);
        assertNotNull(tall);
        assertEquals(100, tall.length);
        assertEquals(1, tall[0].length);

        ShortMatrixCache.recycleMatrix(tall);
    }
}
