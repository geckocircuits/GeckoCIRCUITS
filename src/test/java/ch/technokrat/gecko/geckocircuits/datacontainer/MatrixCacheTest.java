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
 * Tests for IntegerMatrixCache and ShortMatrixCache - array object pooling.
 * These caches help reduce GC pressure by recycling array allocations.
 */
public class MatrixCacheTest {

    @Before
    public void setUp() {
        IntegerMatrixCache.clearCache();
        ShortMatrixCache.clearCache();
    }

    @After
    public void tearDown() {
        IntegerMatrixCache.clearCache();
        ShortMatrixCache.clearCache();
    }

    // ====================================================
    // IntegerMatrixCache Tests
    // ====================================================

    @Test
    public void testIntegerCache_GetFromEmptyCache() {
        // First call creates new array
        int[][] array = IntegerMatrixCache.getCachedIntArray(5, 10);
        
        assertNotNull(array);
        assertEquals(5, array.length);
        assertEquals(10, array[0].length);
    }

    @Test
    public void testIntegerCache_RecycleAndReuse() {
        int[][] original = IntegerMatrixCache.getCachedIntArray(5, 10);
        
        // Put values in
        original[0][0] = 42;
        original[4][9] = 100;
        
        // Recycle
        IntegerMatrixCache.recycleIntArray(original);
        
        // Get same dimensions - should return recycled array
        int[][] recycled = IntegerMatrixCache.getCachedIntArray(5, 10);
        
        assertNotNull(recycled);
        assertEquals(5, recycled.length);
        assertEquals(10, recycled[0].length);
    }

    @Test
    public void testIntegerCache_DifferentDimensions() {
        // Get various sizes
        int[][] arr1 = IntegerMatrixCache.getCachedIntArray(2, 3);
        int[][] arr2 = IntegerMatrixCache.getCachedIntArray(4, 5);
        int[][] arr3 = IntegerMatrixCache.getCachedIntArray(6, 7);
        
        // Verify dimensions
        assertEquals(2, arr1.length);
        assertEquals(3, arr1[0].length);
        
        assertEquals(4, arr2.length);
        assertEquals(5, arr2[0].length);
        
        assertEquals(6, arr3.length);
        assertEquals(7, arr3[0].length);
    }

    @Test
    public void testIntegerCache_MultipleRecycles() {
        // Recycle multiple arrays
        int[][] arr1 = IntegerMatrixCache.getCachedIntArray(2, 3);
        int[][] arr2 = IntegerMatrixCache.getCachedIntArray(4, 5);
        
        IntegerMatrixCache.recycleIntArray(arr1);
        IntegerMatrixCache.recycleIntArray(arr2);
        
        // Get them back
        int[][] get1 = IntegerMatrixCache.getCachedIntArray(2, 3);
        int[][] get2 = IntegerMatrixCache.getCachedIntArray(4, 5);
        
        assertEquals(2, get1.length);
        assertEquals(4, get2.length);
    }

    @Test
    public void testIntegerCache_EmptyArray() {
        int[][] empty = IntegerMatrixCache.getCachedIntArray(0, 0);
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testIntegerCache_SingleRow() {
        int[][] single = IntegerMatrixCache.getCachedIntArray(1, 100);
        assertEquals(1, single.length);
        assertEquals(100, single[0].length);
    }

    @Test
    public void testIntegerCache_SingleColumn() {
        int[][] single = IntegerMatrixCache.getCachedIntArray(100, 1);
        assertEquals(100, single.length);
        assertEquals(1, single[0].length);
    }

    @Test
    public void testIntegerCache_ClearCache() {
        // Add to cache
        int[][] arr = IntegerMatrixCache.getCachedIntArray(5, 5);
        IntegerMatrixCache.recycleIntArray(arr);
        
        // Clear
        IntegerMatrixCache.clearCache();
        
        // Get should create new (cache is empty)
        int[][] newArr = IntegerMatrixCache.getCachedIntArray(5, 5);
        assertNotNull(newArr);
    }

    // ====================================================
    // ShortMatrixCache Tests
    // ====================================================

    @Test
    public void testShortCache_GetFromEmptyCache() {
        short[][] array = ShortMatrixCache.getCachedMatrix(5, 10);
        
        assertNotNull(array);
        assertEquals(5, array.length);
        assertEquals(10, array[0].length);
    }

    @Test
    public void testShortCache_RecycleAndReuse() {
        short[][] original = ShortMatrixCache.getCachedMatrix(5, 10);
        
        // Put values in
        original[0][0] = 42;
        original[4][9] = 100;
        
        // Recycle
        ShortMatrixCache.recycleMatrix(original);
        
        // Get same dimensions
        short[][] recycled = ShortMatrixCache.getCachedMatrix(5, 10);
        
        assertNotNull(recycled);
        assertEquals(5, recycled.length);
        assertEquals(10, recycled[0].length);
    }

    @Test
    public void testShortCache_DifferentDimensions() {
        short[][] arr1 = ShortMatrixCache.getCachedMatrix(3, 4);
        short[][] arr2 = ShortMatrixCache.getCachedMatrix(5, 6);
        
        assertEquals(3, arr1.length);
        assertEquals(4, arr1[0].length);
        
        assertEquals(5, arr2.length);
        assertEquals(6, arr2[0].length);
    }

    @Test
    public void testShortCache_EmptyMatrix() {
        short[][] empty = ShortMatrixCache.getCachedMatrix(0, 0);
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testShortCache_ClearCache() {
        short[][] arr = ShortMatrixCache.getCachedMatrix(5, 5);
        ShortMatrixCache.recycleMatrix(arr);
        
        ShortMatrixCache.clearCache();
        
        short[][] newArr = ShortMatrixCache.getCachedMatrix(5, 5);
        assertNotNull(newArr);
    }

    // ====================================================
    // DimensionKey Tests (via IntegerMatrixCache)
    // ====================================================

    @Test
    public void testDimensionKey_SameDimensions() {
        // Same dimensions should match in cache
        int[][] arr1 = IntegerMatrixCache.getCachedIntArray(10, 20);
        IntegerMatrixCache.recycleIntArray(arr1);
        
        int[][] arr2 = IntegerMatrixCache.getCachedIntArray(10, 20);
        assertNotNull(arr2);
        assertEquals(10, arr2.length);
        assertEquals(20, arr2[0].length);
    }

    @Test
    public void testDimensionKey_DifferentDimensions() {
        // Different dimensions should not match
        int[][] arr5x10 = IntegerMatrixCache.getCachedIntArray(5, 10);
        IntegerMatrixCache.recycleIntArray(arr5x10);
        
        // Request 10x5 - different dimensions, should be new array
        int[][] arr10x5 = IntegerMatrixCache.getCachedIntArray(10, 5);
        assertEquals(10, arr10x5.length);
        assertEquals(5, arr10x5[0].length);
    }

    // ====================================================
    // Stress Tests
    // ====================================================

    @Test
    public void testCacheStress_ManyAllocations() {
        // Allocate many arrays
        for (int i = 0; i < 100; i++) {
            int[][] arr = IntegerMatrixCache.getCachedIntArray(i % 10 + 1, (i + 5) % 10 + 1);
            IntegerMatrixCache.recycleIntArray(arr);
        }
        
        // Should not throw
        IntegerMatrixCache.clearCache();
    }

    @Test
    public void testCacheStress_ShortMatrix() {
        for (int i = 0; i < 100; i++) {
            short[][] arr = ShortMatrixCache.getCachedMatrix(i % 10 + 1, (i + 5) % 10 + 1);
            ShortMatrixCache.recycleMatrix(arr);
        }
        
        ShortMatrixCache.clearCache();
    }

    // ====================================================
    // Data Preservation Tests
    // ====================================================

    @Test
    public void testIntegerCache_DataInitialization() {
        // New arrays should be zero-initialized by Java
        int[][] arr = IntegerMatrixCache.getCachedIntArray(3, 3);
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(0, arr[i][j]);
            }
        }
    }

    @Test
    public void testShortCache_DataInitialization() {
        short[][] arr = ShortMatrixCache.getCachedMatrix(3, 3);
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(0, arr[i][j]);
            }
        }
    }
}
