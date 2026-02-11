package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortMatrixCacheTest {
    @Test
    public void testGetAndRecycleShortArray() {
        short[][] arr = ShortMatrixCache.getCachedMatrix(3, 4);
        assertNotNull(arr);
        assertEquals(3, arr.length);
        assertEquals(4, arr[0].length);
        // Fill and recycle
        arr[0][0] = 42;
        ShortMatrixCache.recycleMatrix(arr);
        // Should be able to get a cached array of same size
        short[][] arr2 = ShortMatrixCache.getCachedMatrix(3, 4);
        assertNotNull(arr2);
        assertEquals(3, arr2.length);
        assertEquals(4, arr2[0].length);
    }

    @Test
    public void testClearCache() {
        short[][] arr = ShortMatrixCache.getCachedMatrix(2, 2);
        ShortMatrixCache.recycleMatrix(arr);
        ShortMatrixCache.clearCache();
        // After clear, should get a new array
        short[][] arr2 = ShortMatrixCache.getCachedMatrix(2, 2);
        assertNotNull(arr2);
        assertEquals(2, arr2.length);
        assertEquals(2, arr2[0].length);
    }
}
