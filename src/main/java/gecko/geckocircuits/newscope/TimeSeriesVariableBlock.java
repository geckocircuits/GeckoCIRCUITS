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
package gecko.geckocircuits.newscope;

import gecko.geckocircuits.newscope.DataBlock.IndexLimit;
import gecko.geckocircuits.newscope.DataBlock.TimeLimit;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author xtibi
 */
public final class TimeSeriesVariableBlock extends AbstractTimeSerie {

    private DataBlock _lastBlock;
    private int _overallSize;
    private final SortedMap<IndexLimit, DataBlock> _indexData;
    private final SortedMap<TimeLimit, DataBlock> _timeData;

    public TimeSeriesVariableBlock() {
        super();
        _indexData = new TreeMap<IndexLimit, DataBlock>();
        _timeData = new TreeMap<TimeLimit, DataBlock>();
    }

    public int getNumBlocks() {
        if (_lastBlock != null) {
            return _indexData.size() + 1;
        }
        return 0;
    }

    @Override
    public double getValue(final int index) {
        final IndexLimit ind = new IndexLimit(index, index);
        DataBlock dataBlock = _indexData.get(ind);

        if (dataBlock == null) {
            dataBlock = _lastBlock;
        }
        return dataBlock.getBlockValue(index);
    }

    @Override
    public void setValue(final int index, final double value) {
        // you can only add values to the end!!
        assert index >= _overallSize;

        if (_lastBlock == null) {
            _lastBlock = new DataBlock(value, 0.0, 1, _overallSize);
        } else {
            if (!_lastBlock.setBlockValue(value)) {
                _indexData.put(_lastBlock.getIndexLimit(), _lastBlock);
                _timeData.put(new TimeLimit(_lastBlock.getStartValue(), value), _lastBlock);
                _lastBlock = new DataBlock(value, 0.0, 1, _overallSize);
            }
        }
        _overallSize++;
    }

    @Override
    public int getMaximumIndex() {
        return _overallSize - 1;
    }

    @Override
    public int findTimeIndex(final double time) {
        final TimeLimit timeLim = new TimeLimit(time, time);
        DataBlock dataBlock = _timeData.get(timeLim);

        if (dataBlock == null) {
            dataBlock = _lastBlock;
        }
        return dataBlock.findTimeIndex(time);
    }
//
//    public static void main(String args[]) {
//
//        final TimeSeriesVariableBlock tsb = new TimeSeriesVariableBlock();
//
//        int arraySize = 4000000;
//        double scale = 1e-150;
//        final Random rnd = new Random();
//
//        double startValue = 0.0;
//        double dt = 1e-9;
//        int reps = rnd.nextInt(1000) + 1;
//
//        System.out.println("----------------------------------------------------------------------");
//        System.out.println("Setting values...");
//
//        double value = 0;
//        int index = 0;
//        int blockCount = 0;
//        while (tsb.getMaximumIndex() < arraySize) {
//            for (int i = 0; i < reps; i++) {
//                value = startValue + i * dt;
//                tsb.setValue(index, value);
//                index++;
//            }
//            dt = 1.002 * dt;
//            startValue = value + dt;
//            reps = rnd.nextInt(1000) + 2;
//
//            blockCount++;
//        }
//
//        System.out.println("There should be around " + blockCount + " DataBlocks");
//        System.out.println("There are " + tsb.getNumBlocks() + " DataBlocks");
//        System.out.println("----------------------------------------------------------------------");
//
//        int i1 = rnd.nextInt(arraySize - 1) + 1;
//        int i2 = rnd.nextInt(arraySize - 1) + 1;
//        int i3 = rnd.nextInt(arraySize - 1) + 1;
//        System.out.println("Trying to get values at indices: " + i1 + ", " + i2 + ", " + i3 + ":");
////        System.out.println("TIMESERIESVARIABLEARRAY:");
////        System.out.println(tsa.getValue(i1) + ", " + tsa.getValue(i2) + ", " + tsa.getValue(i3));
//        System.out.println("TIMESERIESVARIABLEBLOCK:");
//        System.out.println(tsb.getValue(i1) + ", " + tsb.getValue(i2) + ", " + tsb.getValue(i3));
//        System.out.println("----------------------------------------------------------------------");
//
//        double time = tsb.getValue(tsb.getMaximumIndex()) * rnd.nextDouble();
//        System.out.println("Trying to find index of time value: " + time);
//        int foundA = tsa.findTimeIndex(time, tsa.getMaximumIndex());
//        int foundB = tsb.findTimeIndex(time, tsb.getMaximumIndex());
//        System.out.println("TIMESERIESVARIABLEARRAY:");
//        System.out.println("Found index: " + foundA + ", with value: " + tsa.getValue(foundA));
//        System.out.println("TIMESERIESVARIABLEBLOCK:");
//        System.out.println("Found index: " + foundB + ", with value: " + tsb.getValue(foundB));
        
//        for (int i=0; i<10000; i++) {
//            time = scale * tsa.getValue(tsa.getMaximumIndex()) * rnd.nextDouble();
//            foundA = tsa.findTimeIndex(time, tsa.getMaximumIndex());
//            foundB = tsb.findTimeIndex(time, tsb.getMaximumIndex());
//            if (Math.abs(foundA - foundB) > 1) {
//                System.out.println("ERROR AT: " + foundA + ", found: " + foundB +
//                        ", with time: " + time);
//            }
//        }
        
//        System.out.println("----------------------------------------------------------------------");
//        System.out.println("Testing the speed...");
//        reps = 10000;
//        System.out.println("Trying to find indices for " + reps + " different time values: ");
//        
//        System.out.println("TIMESERIESVARIABLEARRAY:");
//        time = scale * tsa.getValue(tsa.getMaximumIndex()) * rnd.nextDouble();
//        
//        long startTime = System.currentTimeMillis();
//        for (int i = 0; i < reps; i++) {
//            tsa.findTimeIndex(time, tsa.getMaximumIndex());
//            time = scale * tsa.getValue(tsa.getMaximumIndex()) * rnd.nextDouble();
//        }
//        long endTime = System.currentTimeMillis();
//
//        System.out.println("Indices found in: " + (endTime - startTime) + " milliseconds");
        
//        System.out.println("TIMESERIESVARIABLEBLOCK:");
//        time = scale * tsb.getValue(tsb.getMaximumIndex()) * rnd.nextDouble();
//
//        long startTime = System.currentTimeMillis();
//        for (int i = 0; i < reps; i++) {
//            tsb.findTimeIndex(time, tsb.getMaximumIndex());
//            time = scale * tsb.getValue(tsb.getMaximumIndex()) * rnd.nextDouble();
//        }
//        long endTime = System.currentTimeMillis();
//
//        System.out.println("Indices found in: " + (endTime - startTime) + " milliseconds");

//    }

    @Override
    public double getLastTimeInterval() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
