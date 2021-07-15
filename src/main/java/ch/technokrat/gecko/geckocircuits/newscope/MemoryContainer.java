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
package ch.technokrat.gecko.geckocircuits.newscope;

import java.util.ArrayList;
import java.util.List;

/**
 * Instead of creating a new float[] at every simulation step, we try to obtain
 * a cached one. This class is "pseudo-threadsafe". One could do it quite clean
 * with some effort and synchronization of the static methods. However, the
 * occurence of returnValue == null is only rare. Therefore, we just detect
 * this nullpointer and generate a float[] in this case. This is much simpler
 * and more efficient than a correct thread synchronization.
 * @author andreas
 */
public class MemoryContainer {

    private final float[][] _memCache;
    private long _inUsePointer;
    private long _availablePointer;
    private static final int CACHE_SIZE = 10000;
    private static final int CLEAR_SIZE = 10;
    private final int _numRows;    
        
    private static List<MemoryContainer> _containerSingletons = new ArrayList<MemoryContainer>();
    
    public static MemoryContainer getMemoryContainer(final int numRows) {
        if(_containerSingletons.size() > CLEAR_SIZE) {
            _containerSingletons.clear();
        }
        for(MemoryContainer mem : _containerSingletons) {
            if(mem._numRows == numRows) {
                return mem;
            }
        }
        
        MemoryContainer newContainer = new MemoryContainer(numRows);        
        _containerSingletons.add(newContainer);
        return newContainer;
    }
    
    
    public MemoryContainer(final int numRows) {
        _memCache = new float[CACHE_SIZE][numRows];
        _numRows = numRows;
        _inUsePointer = 0;
        _availablePointer = CACHE_SIZE - 1;    
    }

    private float[] createNewArray() {
        return new float[_numRows];
    }
    
    public float[] getArray() {
        assert _memCache != null : "Memory cache not yet initialized!";
        if (_inUsePointer < _availablePointer - 10) {
            final int arrayIndex = (int) (_inUsePointer % CACHE_SIZE);
            final float[] returnValue = _memCache[arrayIndex];
            _memCache[arrayIndex] = null;
            _inUsePointer++;
//            if(_availablePointer % 1000 == 0) {
//            System.out.println("Cache contains " + 
//                    (_availablePointer - _inUsePointer) * 100.0 / CACHE_SIZE 
//                    + "% valid arrays.");
//            }
            assert _inUsePointer < _availablePointer;
            if(returnValue == null) {
                return createNewArray();
            }
            return returnValue;
        } else {
            return createNewArray();
        }
    }
    
    public float[] getArrayInitializedWithNaN() {
        float[] returnValue = getArray();
        for(int i = 0; i < returnValue.length; i++) {
            returnValue[i] = Float.NaN;
        }
        return returnValue;
    }

    public void recycleArray(final float[] value) {
        assert value.length == _numRows;
        _memCache[(int) (_availablePointer % CACHE_SIZE)] = value;
        _availablePointer++;
    }

    
    
}
