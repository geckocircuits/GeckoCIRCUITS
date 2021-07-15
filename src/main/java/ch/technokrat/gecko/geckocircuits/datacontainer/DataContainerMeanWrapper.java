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

import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalMean;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalRegular;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andreas
 */
public final class DataContainerMeanWrapper extends AbstractDataContainer {

    private final AbstractDataContainer _wrapped;
    private final List<ScopeSignalMean> _meanSignals = new ArrayList<ScopeSignalMean>();
    private final ScopeWrapperIndices _scopeIndices;

    public DataContainerMeanWrapper(final AbstractDataContainer wrapped, final ScopeWrapperIndices scopeIndices) {
        super();
        _wrapped = wrapped;
        _scopeIndices = scopeIndices;
        assert _scopeIndices != null;
    }

    @Override
    public HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax) {
        return HiLoData.hiLoDataFabric(0, 10);
    }

    @Override
    public float getValue(final int columnIndex, final int row) {

        if (_wrapped instanceof DataContainerIntegralCalculatable) {
            final double centerTimeValue = _wrapped.getTimeValue(columnIndex, row);
            final double timeInterval = _meanSignals.get(columnIndex).getAveragingTime();
            return ((DataContainerIntegralCalculatable) _wrapped).getAVGValueInInterval(
                    centerTimeValue - timeInterval / 2, centerTimeValue + timeInterval / 2,
                    _meanSignals.get(columnIndex).getConnectedScopeInputIndex());
        } else {
            return 0f;
        }
    }

    @Override
    void setSignalPathName(int containerRowIndex, String subcircuitPath) {        
        _wrapped.setSignalPathName(containerRowIndex, subcircuitPath);
    }

    @Override
    public int getRowLength() {
        return _meanSignals.size();
    }

    @Override
    public double getTimeValue(final int index, final int row) {
        return _wrapped.getTimeValue(index, row);
    }

    @Override
    public int getMaximumTimeIndex(final int row) {
        final int wrappedMaximum = _wrapped.getMaximumTimeIndex(row);
        if (wrappedMaximum < 0) {
            return wrappedMaximum;
        }
        // recalculate maximum index, since we do some averaging
        final double maximumTime = _wrapped.getTimeValue(wrappedMaximum, row);

        double maxAvgTime = 0;
        for (ScopeSignalMean signal : _meanSignals) {
            maxAvgTime = Math.max(signal.getAveragingTime(), maxAvgTime);
        }

        final double maxAvgAllowedTime = maximumTime - maxAvgTime / 2;
        return Math.min(_wrapped.getTimeSeries(0).findTimeIndex(maxAvgAllowedTime), wrappedMaximum);
    }

    @Override
    public Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex) {

        if (_wrapped instanceof DataContainerIntegralCalculatable) {
            final double centerTimeValue = (intervalStart + intervalStop) / 2;
            final double timeInterval = _meanSignals.get(columnIndex).getAveragingTime();
            // falsifies the result, makes little sense???
            // System.out.println("Test: " + (centerTimeValue - timeInterval / 2) + " " + (centerTimeValue + timeInterval / 2) + "::" + (timeInterval) + "::" + (intervalStart) + " " + (intervalStop));
            Object returnValue = ((DataContainerIntegralCalculatable) _wrapped).getAVGValueInInterval(
                    centerTimeValue - timeInterval / 2, centerTimeValue + timeInterval / 2,
                    _meanSignals.get(columnIndex).getConnectedScopeInputIndex());
            return returnValue;
        } else {
            return 0f;
        }

    }

    @Override
    public HiLoData getAbsoluteMinMaxValue(final int row) {
        return _wrapped.getAbsoluteMinMaxValue(_meanSignals.get(row).getConnectedScopeInputIndex());
    }

    @Override
    public int findTimeIndex(final double time, final int row) {
        return _wrapped.findTimeIndex(time, row);
    }

    @Override
    public String getSignalName(final int row) {
        return _wrapped.getSignalName(_meanSignals.get(row).getConnectedScopeInputIndex());
    }

    @Override
    public String getXDataName() {
        return _wrapped.getXDataName();
    }

    @Override
    public ContainerStatus getContainerStatus() {
        return _wrapped.getContainerStatus();
    }

    @Override
    public void setContainerStatus(final ContainerStatus containerStatus) {
        _wrapped.setContainerStatus(containerStatus);
    }

    @Override
    public boolean isInvalidNumbers(final int row) {
        return _wrapped.isInvalidNumbers(_meanSignals.get(row).getConnectedScopeInputIndex());
    }

    public void defineMeanSignals(final List<ScopeSignalMean> meanSignals) {
        _meanSignals.clear();
        _scopeIndices.reset();

        for (ScopeSignalMean meanSignal : meanSignals) {
            final int scopeInputIndex = meanSignal.getConnectedScopeInputIndex();
            final int containerRowIndex = _scopeIndices.getContainerRowIndex(scopeInputIndex);
            _meanSignals.add(new ScopeSignalMean(new ScopeSignalRegular(containerRowIndex, null), meanSignal.getAveragingTime()));
            _scopeIndices.defineAdditionalSignal(this, meanSignal.getConnectedScopeInputIndex() + 1, _meanSignals.size() - 1);
        }

        if (_wrapped instanceof DataContainerIntegralCalculatable) {
            ((DataContainerIntegralCalculatable) _wrapped).defineAvgCalculation(_meanSignals);
        } else {
            assert false;
        }

    }

    @Override
    public AbstractTimeSerie getTimeSeries(final int row) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeSignal(final int row) {
        _scopeIndices.deleteSignal(row);
    }

    @Override
    public float[] getDataArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
