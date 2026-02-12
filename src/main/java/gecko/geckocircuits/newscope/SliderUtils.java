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

import gecko.GeckoSim;
import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import javax.swing.JOptionPane;

/**
 *
 * @author Tibor Keresztfalvi
 */
public final class SliderUtils {

    private static final String MESSAGE_HEAD = "Info";

    private SliderUtils() {
        // this is a static utility class, don't create instances
    }

    public enum IterationDirection {

        FORWARD,
        BACKWARD;
    }

    public enum ExtremumType {

        MAXIMUM,
        MINIMUM;
    }

    public enum FlankType {

        ASCEND,
        DESCEND;
    }

    private static class DataContainerIterator {

        private final AbstractDataContainer _container;
        private final IterationDirection _direction;
        private int _index;
        private final int _crvIndex;

        DataContainerIterator(final AbstractDataContainer container, final IterationDirection direction,
                final int initialIndex, final int crvIndex) {
            _container = container;
            _direction = direction;
            _index = initialIndex;
            _crvIndex = crvIndex;
        }

        int getIndex() {
            return _index;
        }

        float getDataValue() {
            return _container.getValue(_crvIndex, _index);
        }

        float iterateAndGetValue() {
            iterate();
            return getDataValue();
        }

        private void iterate() {
            switch (_direction) {
                case FORWARD:
                    _index++;
                    break;
                case BACKWARD:
                    _index--;
                    break;
                default:
                    assert false;
            }

            if (_index < 0 || _index > _container.getMaximumTimeIndex(0)) {
                throw new ArrayIndexOutOfBoundsException("Iteration out of bounds!");
            }

        }
    }

    private static boolean extremaCondition(final ExtremumType maxMin, final double before,
            final double value, final double after) {
        switch (maxMin) {
            case MAXIMUM:
                return (before < value && value > after);
            case MINIMUM:
                return (before > value && value < after);
            default:
                assert false;
                return false;
        }
    }

    @SuppressWarnings("PMD") // supress switch statement break warning, which is useless here!
    private static boolean signalFlankCondition(final FlankType flankType, final IterationDirection direction,
            final double currVal, final double nextVal) {

        switch (flankType) {
            case ASCEND:
                if (direction == IterationDirection.FORWARD) {
                    return nextVal >= currVal;
                } else {
                    return nextVal <= currVal;
                }                
            case DESCEND:
                if (direction == IterationDirection.FORWARD) {
                    return nextVal <= currVal;
                } else {
                    return nextVal >= currVal;
                }
            default:
                assert false;
                return false;
        }
    }

    /**
     * Returns the x-value of the next or previous maximum or minimum in the given curve
     *
     * @param xValue x-value from where to start
     * @param crvIndex index of the curve
     * @param fwd if true, the next extrema will be returned, the previous else
     * @param minMax if true, a maximum will be returned, a minimum else
     * @return x-value of the next maximum
     */
    static double nextExtrema(final double xValue, final AbstractCurve crvIndex, final IterationDirection direction,
            final ExtremumType minMax, final AbstractDataContainer wsRAM) {

        try {
            final DataContainerIterator iterator = new DataContainerIterator(wsRAM, direction, wsRAM.findTimeIndex(xValue, 0),
                    crvIndex.getValueDataIndex());


            double before = iterator.iterateAndGetValue();
            int beginIndex = iterator.getIndex();
            
            double value = iterator.iterateAndGetValue();
            
            double after = iterator.iterateAndGetValue();
            int endIndex = iterator.getIndex();                                    

            while (true) {
                if (extremaCondition(minMax, before, value, after)) {
                    return wsRAM.getTimeValue(((beginIndex + endIndex) / 2), 0);                                        
                } else {
                    before = value;
                    value = after;
                    beginIndex = endIndex;
                    while (value == after) {
                        iterator.iterate();
                        after = iterator.getDataValue();                                                
                    }
                    endIndex = iterator.getIndex();
                }
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(GeckoSim._win, "No extrema found!", MESSAGE_HEAD,
                    JOptionPane.INFORMATION_MESSAGE);
            return xValue;
        }
    }

    /**
     * Returns the x-value of the next or previous ascending or descending flank in a signal curve
     *
     * @param xValue x-value from where to start
     * @param crvIndex index of signal curve
     * @param fwd if true, the next flank will be returned, the previous else
     * @param ascDescOrig if true, an ascned will be returned, a descend else
     * @return x-value of the next ascend
     */
    static double nextSIGNALFlank(final double xValue, final AbstractCurve crvIndex, final IterationDirection direction,
            final FlankType flankType, final AbstractDataContainer wsRAM) {
        try {
            final DataContainerIterator iterator = new DataContainerIterator(wsRAM, direction, wsRAM.findTimeIndex(xValue, 0),
                    crvIndex.getValueDataIndex());


            double currentValue = iterator.iterateAndGetValue();
            double nextValue = iterator.iterateAndGetValue();
            
            while (signalFlankCondition(flankType, direction, currentValue, nextValue)) {
                currentValue = nextValue;
                nextValue = iterator.iterateAndGetValue();
            }


            iterator.iterate();
            return wsRAM.getTimeValue(iterator.getIndex(), 0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "No flank found!", MESSAGE_HEAD,
                    JOptionPane.INFORMATION_MESSAGE);
            return xValue;
        }
    }

    /**
     * Returns the next or previous x-value where the function is zero or equal to the current value
     *
     * @param xValue x-value from where to start
     * @param crvIndex index of the curve
     * @param fwd if true, the next zero or equal value will be returned, the previous else
     * @param zero if true, a zero will be returned, an equal value else
     * @return x-value of the next zero
     */
    static double nextZeroOrEqual(final double xValue, final AbstractCurve crvIndex, final IterationDirection direction,
            final AbstractDataContainer wsRAM, final double comparisonValue) {

        final DataContainerIterator iterator = new DataContainerIterator(wsRAM, direction, wsRAM.findTimeIndex(xValue, 0),
                crvIndex.getValueDataIndex());

        iterator.iterate();

        double currentValue = iterator.iterateAndGetValue();
        iterator.iterate();
        double nextValue = iterator.iterateAndGetValue();
        try {
            // difference in signum
            while ((currentValue - comparisonValue) * (nextValue - comparisonValue) > 0 
                    || ((currentValue - comparisonValue) == 0 && (nextValue - comparisonValue) == 0)) {
                currentValue = nextValue;
                nextValue = iterator.iterateAndGetValue();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "No zero or equal value found!", MESSAGE_HEAD,
                    JOptionPane.INFORMATION_MESSAGE);
            return xValue;
        }
        
        return wsRAM.getTimeValue(iterator.getIndex(), 0);
    }
}
