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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
//import java.util.WeakHashMap;

/**
 * Main-Function: getCachedLUDecomposition. For switched converters, one and the
 * same LU decomposition is typically re-computed very often. Therefore, this
 * class provides a cache, which speeds up the calculation for larger matrices.
 *
 * @author andy
 */
public class LUDecompositionCache {

    private static final int MAX_CACHE_SIZE = 1000;
    private static int varMaxCacheSize = MAX_CACHE_SIZE;
    private final Map<Integer, AbstractCachedMatrix> _cachedMatrices = new HashMap<Integer, AbstractCachedMatrix>();
    private int _cacheHitCounter = 0;
    private int _cacheMissCounter = 0;
    private static final boolean USE_CACHE = true;
    private static long memoryBytes = 0;

    private static final long maxJVMMemory = Runtime.getRuntime().maxMemory();

    /**
     *
     * @param matrix the matrix to search in the LU-Cache
     * @param time actual simulationtime, needed for the cache overflow removal
     * algorithm
     * @return the matrix in the cache, including the LU-Decomposition
     */
    public AbstractCachedMatrix getCachedLUDecomposition(final double[][] matrix, final double time) {

        final AbstractCachedMatrix newMatrix = new CachedMatrix(matrix);
        final AbstractCachedMatrix fromCache = _cachedMatrices.get(newMatrix.hashCode());
        if (fromCache == null) {
            _cacheMissCounter++;
            newMatrix.setAccess(time);
            testForCacheShrink(time);
            newMatrix.initLUDecomp();
            if (USE_CACHE) {
                _cachedMatrices.put(newMatrix.hashCode(), newMatrix);
                memoryBytes += newMatrix.calculateMemoryRequirement();
            }
            return newMatrix;

        } else {
            if (fromCache.secondHashCode() != newMatrix.secondHashCode()) {
                // this is in case something goes really wrong (by accident same hash
                // code of two actually different matrices
                newMatrix.setAccess(time);
                testForCacheShrink(time);
                newMatrix.initLUDecomp();
                if (USE_CACHE) {
                    _cachedMatrices.put(newMatrix.hashCode(), newMatrix);
                    memoryBytes += newMatrix.calculateMemoryRequirement();
                }
                return newMatrix;
            }

            fromCache.setAccess(time);
            _cacheHitCounter++;
            //System.out.println("matrix size : " + matrix.length);
//             if(_cacheHitCounter%1000 == 0) {
//                 printDebugMessages(time);
//             }

            return fromCache;
        }
    }

    private void printDebugMessages(final double time) {
        _cacheHitCounter++;

        System.out.println("cache size: " + _cachedMatrices.size());
        //for (int key : _cachedMatrices.keySet()) {
        //    final AbstractCachedMatrix mat = _cachedMatrices.get(key);
        //    System.out.println("cache: " + mat + " " + (time - mat.getLatestAccessTime()) + " " + mat.getAccessCounter());
        //}
        System.out.println("cache hits: " + _cacheHitCounter + " " + _cacheMissCounter + " " + (100.0 * _cacheHitCounter / (_cacheHitCounter + _cacheMissCounter)) + "%");
        System.out.println("memory requirement in MB: " + memoryBytes / 1024 / 1024);
        long size = 0;
        for (Entry<Integer, AbstractCachedMatrix> entry : _cachedMatrices.entrySet()) {
            size += entry.getValue().calculateMemoryRequirement();
        }
    }

    /*
     * if cache is larger than maximum cache size, then remove
     * the oldest and the two least accessed matrix entry.
     * careful with the least accessed: put a threshold, so that 
     * a quite new matrix is not removed immediately!
     */
    private void testForCacheShrink(double time) {
//        if(1>0) {
//            return; // i disabled this, since we use a weakHashMap for the cache, now!
//        }
        if (_cachedMatrices.size() > varMaxCacheSize) {
            Integer oldestKey = -1;
            double oldestTime = 1e99;

            for (Map.Entry<Integer, AbstractCachedMatrix> entry : _cachedMatrices.entrySet()) {
                AbstractCachedMatrix tmp = entry.getValue();
                if (tmp.getLatestAccessTime() < oldestTime) {
                    oldestKey = entry.getKey();
                    oldestTime = tmp.getLatestAccessTime();
                }
            }

            AbstractCachedMatrix removed = _cachedMatrices.remove(oldestKey);
            memoryBytes -= removed.calculateMemoryRequirement();

            if (removed != null) {
                removed.deleteCache();
            }

            double accessMinimumAge = (oldestTime + time) / 2;
            removeLeastAccessedMatrices(accessMinimumAge);
        }

        calculateNewVarMaxCacheSize();
    }

    private void removeLeastAccessedMatrices(double accessMinimumAge) {
        Integer leastAccessKey = -1;
        Integer secondLeastAccessKey = -1;
        Integer thirdLeastAccessKey = -1;

        int leastAccessCounter = Integer.MAX_VALUE;
        int secondLeastAccessCounter = Integer.MAX_VALUE;
        int thirdLeastAccessCounter = Integer.MAX_VALUE;

        for (Map.Entry<Integer, AbstractCachedMatrix> entry : _cachedMatrices.entrySet()) {
            AbstractCachedMatrix tmp = entry.getValue();
            if (tmp.getLatestAccessTime() < accessMinimumAge) {

                int accessCounter = tmp.getAccessCounter();

                if (accessCounter < thirdLeastAccessCounter) {
                    thirdLeastAccessCounter = accessCounter;
                    thirdLeastAccessKey = entry.getKey();
                } else if (accessCounter < secondLeastAccessCounter) {
                    secondLeastAccessCounter = accessCounter;
                    secondLeastAccessKey = entry.getKey();
                } else if (accessCounter < leastAccessCounter) {
                    leastAccessCounter = accessCounter;
                    leastAccessKey = entry.getKey();
                }
            }
        }

        if (leastAccessKey != -1) {
            AbstractCachedMatrix remove = _cachedMatrices.remove(leastAccessKey);
            memoryBytes -= remove.calculateMemoryRequirement();
        }

        if (secondLeastAccessKey != -1) {
            AbstractCachedMatrix remove = _cachedMatrices.remove(secondLeastAccessKey);
            memoryBytes -= remove.calculateMemoryRequirement();
        }

        if (thirdLeastAccessKey != -1) {
            AbstractCachedMatrix remove = _cachedMatrices.remove(thirdLeastAccessKey);
            memoryBytes -= remove.calculateMemoryRequirement();
        }
    }

    private void calculateNewVarMaxCacheSize() {
        if (memoryBytes > maxJVMMemory / 3) {
            // enshure that the cache size is not too big.
            varMaxCacheSize = _cachedMatrices.size();
        }

        if (memoryBytes < maxJVMMemory / 10) {
            varMaxCacheSize = MAX_CACHE_SIZE;
        }
    }
}
