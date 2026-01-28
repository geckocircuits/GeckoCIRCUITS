package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class IntegerMatrixCacheTest {
    @Test
    public void testGetAndRecycleIntArray() {
        int[][] arr = IntegerMatrixCache.getCachedIntArray(3, 4);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(4, arr[0].length);
        // Fill and recycle
        arr[0][0] = 42;
        IntegerMatrixCache.recycleIntArray(arr);
        // Should be able to get a cached array of same size
        int[][] arr2 = IntegerMatrixCache.getCachedIntArray(3, 4);
        assertNotNull(arr2);
        // Not guaranteed to be same object, but should be correct size
        assertEquals(3, arr2.length);
        assertEquals(4, arr2[0].length);
    }

    @Test
    public void testClearCache() {
        int[][] arr = IntegerMatrixCache.getCachedIntArray(2, 2);
        IntegerMatrixCache.recycleIntArray(arr);
        IntegerMatrixCache.clearCache();
        // After clear, should get a new array
        int[][] arr2 = IntegerMatrixCache.getCachedIntArray(2, 2);
        assertNotNull(arr2);
        assertEquals(2, arr2.length);
        assertEquals(2, arr2[0].length);
    }

    @Test
    public void testCacheEviction() {
        // Fill cache with many arrays to trigger eviction
        for (int i = 0; i < 60; i++) {
            int[][] arr = new int[2][2];
            IntegerMatrixCache.recycleIntArray(arr);
        }
        // Should not throw
        int[][] arr2 = IntegerMatrixCache.getCachedIntArray(2, 2);
        assertNotNull(arr2);
    }
}
