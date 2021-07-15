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

//CHECKSTYLE:OFF
import ch.technokrat.gecko.geckocircuits.newscope.*;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

//CHECKSTYLE:ON
/**
 * Be careful: in the scope, the number of input ports can be
 * increased/decreased. Therefore, check if we are out of bounds, and give zer0
 * as return value, if so.
 *
 * @author andy
 */
public final class DataContainerScopeWrapper extends AbstractDataContainer implements DataContainerIntegralCalculatable {

    private final AbstractDataContainer _globalContainer;
    private final ScopeWrapperIndices _scopeIndices;
    private final DataContainerMeanWrapper _meanWrapper;
    private final DefinedMeanSignals _avgIndexTimeMap;
    private final List<AbstractScopeSignal> _signalsList;
    private final Observer _observer;

    public DataContainerScopeWrapper(final AbstractDataContainer globalContainer, final ScopeWrapperIndices indices,
            final DefinedMeanSignals meanSignals, final List<AbstractScopeSignal> signalsList) {
        this._signalsList = signalsList;
        _globalContainer = globalContainer;
        _observer = new Observer() {
            @Override
            public void update(final Observable observable, final Object arg) {
                setChanged();
                notifyObservers();
            }
        };

        _globalContainer.addObserver(_observer);

        _scopeIndices = indices;
        _meanWrapper = new DataContainerMeanWrapper(globalContainer, _scopeIndices);
        _avgIndexTimeMap = meanSignals;
        _avgIndexTimeMap.registerIndices(_meanWrapper);

        for (int i = 0; i < _scopeIndices.getTotalSignalNumber(); i++) {
            AbstractScopeSignal signal = _signalsList.get(i);
            if (!(signal instanceof ScopeSignalMean)) {
                AbstractDataContainer container = _scopeIndices.getDataContainer(i);
                String subPath = _signalsList.get(i).getSubcircuitPath();
                container.setSignalPathName(_scopeIndices.getContainerRowIndex(i), subPath);
            }
        }
    }

    @Override
    public HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax) {
        final AbstractDataContainer container = _scopeIndices.getDataContainer(row);
        return container.getHiLoValue(_scopeIndices.getContainerRowIndex(row), columnMin, columnMax);
    }

    @Override
    public float getValue(final int row, final int column) {
        final AbstractDataContainer container = _scopeIndices.getDataContainer(row);
        return container.getValue(_scopeIndices.getContainerRowIndex(row), column);
    }

    @Override
    public int getRowLength() {
        return _scopeIndices.getTotalSignalNumber();
    }

    @Override
    public double getTimeValue(final int index, final int row) {
        return _globalContainer.getTimeValue(index, row);
    }

    @Override
    public int getMaximumTimeIndex(final int row) {
        if (_meanWrapper == null) {
            return _globalContainer.getMaximumTimeIndex(row);
        } else {
            return Math.min(_globalContainer.getMaximumTimeIndex(row), _meanWrapper.getMaximumTimeIndex(row));
        }
    }

    @Override
    public HiLoData getAbsoluteMinMaxValue(final int row) {
        if (row < _scopeIndices.getTotalSignalNumber()) {
            final AbstractDataContainer container = _scopeIndices.getDataContainer(row);
            return container.getAbsoluteMinMaxValue(_scopeIndices.getContainerRowIndex(row));
        } else {
            return HiLoData.hiLoDataFabric(0, 1);
        }
    }

    @Override
    public int findTimeIndex(final double time, final int row) {
        return _globalContainer.findTimeIndex(time, row);
    }

    @Override
    public Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int row) {
        AbstractDataContainer container = null;
        Object returnValue = new Float(0.0);
        if (row < this._scopeIndices.getTotalSignalNumber()) {
            container = this._scopeIndices.getDataContainer(row);
            if (container != null) {
                returnValue = container.getDataValueInInterval(intervalStart, intervalStop, _scopeIndices.getContainerRowIndex(row));
            }
        }
        return returnValue;
    }

    @Override
    public String getSignalName(final int row) {
        String returnValue = "";
        if (row < this._signalsList.size()) {
            returnValue = _signalsList.get(row).getSignalName();
        } else {
            returnValue = "TestSubj";
        }
        return returnValue;
    }

    @Override
    public String getXDataName() {
        return _globalContainer.getXDataName();
    }

    @Override
    public ContainerStatus getContainerStatus() {
        return _globalContainer.getContainerStatus();
    }

    @Override
    public void setContainerStatus(final ContainerStatus containerStatus) {
        _globalContainer.setContainerStatus(containerStatus);
        setChanged();
        notifyObservers();
    }

    @Override
    public boolean isInvalidNumbers(final int row) {
        final AbstractDataContainer container = _scopeIndices.getDataContainer(row);
        return container.isInvalidNumbers(_scopeIndices.getContainerRowIndex(row));
    }

    @Override
    public AbstractTimeSerie getTimeSeries(final int row) {
        return _globalContainer.getTimeSeries(row);
    }

    @Override
    public float getAVGValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void defineAvgCalculation(final List<ScopeSignalMean> meanSignals) {
        ((DataContainerIntegralCalculatable) _globalContainer).defineAvgCalculation(meanSignals);
    }

    @Override
    public DefinedMeanSignals getDefinedMeanSignals() {
        return _avgIndexTimeMap;
    }

    public void deregisterObserver() {
        _globalContainer.deleteObserver(_observer);
    }

    @Override
    public float[] getDataArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
