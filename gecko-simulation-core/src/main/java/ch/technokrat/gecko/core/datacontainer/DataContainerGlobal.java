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
package ch.technokrat.gecko.core.datacontainer;

import java.util.Observable;
import java.util.Observer;

/**
 * Global data container that wraps another data container and provides
 * a unified interface for accessing simulation results.
 *
 * This is a simplified GUI-free version for use in headless simulation core.
 * It uses DataContainerSimple as the underlying storage.
 */
public final class DataContainerGlobal extends AbstractDataContainer implements Observer, DataContainerValuesSettable {

    private AbstractDataContainer _data;
    private DataContainerValuesSettable _settable;
    private int _lastDataIndex = -1;
    private String[] _signalNames;
    private String _xDataName = "time [s]";

    public DataContainerGlobal() {
        super();
        // Initialize with null data container
        _data = null;
        _settable = null;
    }

    /**
     * Initializes the data container with the specified dimensions.
     *
     * @param rows number of signal rows
     * @param columns number of time steps
     * @param signalNames names for each signal row
     * @param xDataName name for the time axis
     */
    public void init(final int rows, final int columns, final String[] signalNames, final String xDataName) {
        DataContainerSimple data = DataContainerSimple.fabricConstantDtTimeSeries(rows, columns);
        _signalNames = signalNames;
        _xDataName = xDataName;

        // Set signal names
        if (signalNames != null) {
            for (int i = 0; i < Math.min(rows, signalNames.length); i++) {
                data.setSignalName(signalNames[i], i);
            }
        }

        _settable = data;
        _data = data;
        _data.addObserver(this);
    }

    /**
     * Simplified init method compatible with legacy code.
     *
     * @param rows number of signal rows
     * @param signalNames names for each signal row
     * @param xDataName name for the time axis
     */
    public void init(final int rows, final String[] signalNames, final String xDataName) {
        // Default to a reasonable number of columns (time steps)
        // This will be expanded as needed during simulation
        init(rows, 100000, signalNames, xDataName);
    }

    @Override
    public HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax) {
        if (_data == null) {
            return HiLoData.hiLoDataFabric(0, 0);
        }
        return _data.getHiLoValue(row, columnMin, columnMax);
    }

    @Override
    public float getValue(final int row, final int column) {
        if (_data == null) {
            return 0;
        }
        return _data.getValue(row, column);
    }

    @Override
    public int getRowLength() {
        if (_data == null) {
            return 0;
        }
        return _data.getRowLength();
    }

    @Override
    public double getTimeValue(final int index, final int row) {
        if (_data == null) {
            return 0;
        }
        return _data.getTimeValue(index, row);
    }

    @Override
    public int getMaximumTimeIndex(final int row) {
        if (_data == null) {
            return -1;
        }
        return _data.getMaximumTimeIndex(row);
    }

    @Override
    public void insertValuesAtEnd(final float[] values, final double timeValue) {
        if (_settable != null) {
            _settable.insertValuesAtEnd(values, timeValue);
        }
    }

    @Override
    public HiLoData getAbsoluteMinMaxValue(final int row) {
        if (_data == null) {
            return HiLoData.hiLoDataFabric(0, 1);
        }
        try {
            return _data.getAbsoluteMinMaxValue(row);
        } catch (ArithmeticException ex) {
            return HiLoData.hiLoDataFabric(0, 1);
        }
    }

    /**
     * Clears the data container and releases resources.
     */
    public void clear() {
        if (_data != null) {
            _data.setContainerStatus(ContainerStatus.DELETED);
            _data.deleteObservers();
        }
        _settable = null;
        _data = null;
    }

    @Override
    public int findTimeIndex(final double time, final int row) {
        if (_data == null) {
            return 0;
        }
        return _data.findTimeIndex(time, row);
    }

    @Override
    public int getUsedRAMSizeInMB() {
        if (_settable == null) {
            return 0;
        }
        return _settable.getUsedRAMSizeInMB();
    }

    @Override
    public long getCachedRAMSizeInMB() {
        if (_settable == null) {
            return 0;
        }
        return _settable.getCachedRAMSizeInMB();
    }

    @Override
    public Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex) {
        if (_data == null) {
            return null;
        }
        return _data.getDataValueInInterval(intervalStart, intervalStop, columnIndex);
    }

    @Override
    public String getSignalName(final int row) {
        if (_data == null) {
            return "";
        }
        return _data.getSignalName(row);
    }

    @Override
    public String getXDataName() {
        if (_data == null) {
            return _xDataName;
        }
        return _data.getXDataName();
    }

    @Override
    public ContainerStatus getContainerStatus() {
        if (_data == null) {
            return ContainerStatus.NOT_INITIALIZED;
        }
        return _data.getContainerStatus();
    }

    @Override
    public void setContainerStatus(final ContainerStatus containerStatus) {
        if (_data != null) {
            _data.setContainerStatus(containerStatus);
        }
    }

    @Override
    public boolean isInvalidNumbers(final int row) {
        if (_data == null) {
            return false;
        }
        return _data.isInvalidNumbers(row);
    }

    @Override
    public void update(final Observable observable, final Object arg) {
        if (_data != null && getMaximumTimeIndex(0) != _lastDataIndex) {
            this.setChanged();
            this.notifyObservers();
            _lastDataIndex = getMaximumTimeIndex(0);
        }
    }

    @Override
    public AbstractTimeSerie getTimeSeries(int row) {
        if (_data == null) {
            return null;
        }
        return _data.getTimeSeries(row);
    }

    @Override
    public int hashCode() {
        if (_data == null) {
            return 0;
        }
        return _data.hashCode();
    }

    @Override
    public float[] getDataArray() {
        if (_data == null) {
            return new float[0];
        }
        return _data.getDataArray();
    }

    @Override
    void setSignalPathName(int containerRowIndex, String subcircuitPath) {
        if (_data != null) {
            _data.setSignalPathName(containerRowIndex, subcircuitPath);
        }
    }

    @Override
    public String getSubcircuitSignalPath(final int row) {
        if (_data == null) {
            return "";
        }
        return _data.getSubcircuitSignalPath(row);
    }

    /**
     * Checks if this container has been initialized with data.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return _data != null;
    }
}
