package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortArrayCacheTest {

    @Test
    public void testGetCachedArrayReturnsCorrectSize() {
        int size = 7777;
        short[] array = ShortArrayCache.getCachedArray(size);
        assertEquals(size, array.length);
    }

    @Test
    public void testGetCachedArrayIsZeroFilled() {
        short[] array = ShortArrayCache.getCachedArray(100);
        for (int i = 0; i < array.length; i++) {
            assertEquals(0, array[i]);
        }
    }

    @Test
    public void testRecycleAndRetrieve() {
        int size = 8883;
        short[] original = ShortArrayCache.getCachedArray(size);
        ShortArrayCache.recycleArray(original);
        short[] retrieved = ShortArrayCache.getCachedArray(size);
        assertSame(original, retrieved);
    }

    @Test
    public void testRecycledArrayIsZeroCleared() {
        int size = 8885;
        short[] array = ShortArrayCache.getCachedArray(size);
        for (int i = 0; i < array.length; i++) {
            array[i] = Short.MAX_VALUE;
        }
        ShortArrayCache.recycleArray(array);
        short[] cleaned = ShortArrayCache.getCachedArray(size);
        for (int i = 0; i < cleaned.length; i++) {
            assertEquals(0, cleaned[i]);
        }
    }

    @Test
    public void testDifferentSizesAreIndependent() {
        int sizeA = 8887;
        int sizeB = 8889;
        short[] a = ShortArrayCache.getCachedArray(sizeA);
        short[] b = ShortArrayCache.getCachedArray(sizeB);
        ShortArrayCache.recycleArray(a);
        ShortArrayCache.recycleArray(b);
        short[] retrievedA = ShortArrayCache.getCachedArray(sizeA);
        short[] retrievedB = ShortArrayCache.getCachedArray(sizeB);
        assertEquals(sizeA, retrievedA.length);
        assertEquals(sizeB, retrievedB.length);
    }

    @Test
    public void testRecycleSameSizeTwiceKeepsLatest() {
        int size = 8881;
        short[] first = new short[size];
        short[] second = new short[size];
        ShortArrayCache.recycleArray(first);
        ShortArrayCache.recycleArray(second);
        short[] retrieved = ShortArrayCache.getCachedArray(size);
        assertSame(second, retrieved);
    }
}
