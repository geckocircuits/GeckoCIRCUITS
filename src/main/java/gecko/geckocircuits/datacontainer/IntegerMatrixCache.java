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
package gecko.geckocircuits.datacontainer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * For the dataContainerCompressable, we use a lot of interger[][] arrays,
 * and "throw" them away shortly afterwards. This utility class helps to keep
 * a cache of inter[] objects. Should be threadsave!
 * @author andreas
 */
public final class IntegerMatrixCache {

    private static ConcurrentHashMap<DimensionKey, int[][]> intMatrixCache = new ConcurrentHashMap<DimensionKey, int[][]>();
    private static final int INIT_HASH = 5;
    private static final int HASH_FACTOR = 53;

    public static void clearCache() {
        intMatrixCache.clear();
    }
    
    private IntegerMatrixCache() {
        // pure utility class!
    }
    
    public static int[][] getCachedIntArray(final int mDim, final int nDim) {
        final DimensionKey searchKey = new DimensionKey(mDim, nDim);
        final int[][] isPresent = intMatrixCache.remove(searchKey);
        if (isPresent == null) {
            return new int[mDim][nDim];
        } else {
            return isPresent;
        }
    }

    public static void recycleIntArray(final int[][] toRecycle) {
        int m = toRecycle.length;
        int n = 0;
        if(m > 0) {
            n = toRecycle[0].length;
        }
        if(intMatrixCache.size() > 50) {
            intMatrixCache.clear();
        }
        intMatrixCache.put(new DimensionKey(m, n), toRecycle);
    }
    
    
    

    static class DimensionKey {

        final int _mDim;
        final int _nDim;

        DimensionKey(final int mDim, final int nDim) {
            _mDim = mDim;
            _nDim = nDim;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof DimensionKey) {
                final DimensionKey compareObj = (DimensionKey) obj;
                return compareObj._mDim == _mDim && compareObj._nDim == _nDim;
            }
            return false;

        }

        @Override
        public int hashCode() {
            int hash = INIT_HASH;
            hash = HASH_FACTOR * hash + this._mDim;
            hash = HASH_FACTOR * hash + this._nDim;
            return hash;
        }
    }
    
}
