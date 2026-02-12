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
public final class ShortArrayCache {

    private static ConcurrentHashMap<Integer, short[]> shortMatrixCache = new ConcurrentHashMap<Integer, short[]>();
    
    private ShortArrayCache() {
        // pure utility class!
    }
    
    static int counter1;
    static int counter2;
    
    public static short[] getCachedArray(final int mDim) {        
        final short[] isPresent = shortMatrixCache.remove(mDim);
        //if(counter1 % 100 == 0) {
        //    System.out.println("counters:  "+ counter1 + " " + counter2);
        //}
        if (isPresent == null) {
            counter1++;
            return new short[mDim];            
        } else {
            for(int i = 0; i < isPresent.length; i++) {
                isPresent[i] = 0;
            }
            counter2++;
            return isPresent;
        }
    }

    
    public static void recycleArray(final short[] toRecycle) {        
        shortMatrixCache.put(toRecycle.length, toRecycle);
    }                
}
