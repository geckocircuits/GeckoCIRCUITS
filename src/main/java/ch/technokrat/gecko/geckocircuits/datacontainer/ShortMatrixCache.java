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


import java.util.concurrent.ConcurrentHashMap;

/**
 * For the dataContainerCompressable, we use a lot of interger[][] arrays,
 * and "throw" them away shortly afterwards. This utility class helps to keep
 * a cache of inter[] objects. Should be threadsave!
 * @author andreas
 */
public final class ShortMatrixCache {

    private static ConcurrentHashMap<IntegerMatrixCache.DimensionKey, short[][]> shortMatrixCache = new ConcurrentHashMap<IntegerMatrixCache.DimensionKey, short[][]>();
    private static final int INIT_HASH = 5;
    private static final int HASH_FACTOR = 53;

    public static void clearCache() {
        shortMatrixCache.clear();
    }
    
    private ShortMatrixCache() {
        // pure utility class!
    }
    
    public static short[][] getCachedMatrix(final int mDim, final int nDim) {
        final IntegerMatrixCache.DimensionKey searchKey = new IntegerMatrixCache.DimensionKey(mDim, nDim);
        final short[][] isPresent = shortMatrixCache.remove(searchKey);
        if (isPresent == null) {
            return new short[mDim][nDim];
        } else {
            return isPresent;
        }
    }

    public static void recycleMatrix(final short[][] toRecycle) {
        int m = toRecycle.length;
        int n = 0;
        if(m > 0) {
            n = toRecycle[0].length;
        }
        shortMatrixCache.put(new IntegerMatrixCache.DimensionKey(m, n), toRecycle);
    }                
}
