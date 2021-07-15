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
import ch.technokrat.gecko.geckocircuits.newscope.TimeSeriesConstantDt;

public class DataContainerFourier extends AbstractDataContainer implements DataContainerValuesSettable {

    // CHECKSTYLE:OFF
    DataContainerCompressable _dataCont;
    
        
    private final TimeSeriesConstantDt _timeSeries;    
    private final double _baseFrequency;

    public DataContainerFourier(final int rows, final int maximumColumns, final double baseFrequency) {
        _timeSeries = new TimeSeriesConstantDt();         
        _dataCont = new DataContainerCompressable(2, _timeSeries, new String[]{"Re", "Im"}, "f");                                
        _baseFrequency = baseFrequency;
    }
    
    public void insertValues(final float[] magnitude, final float[] phase) {
        for(int i = 0; i < magnitude.length; i++) {
            float[] array = _dataCont.getDataArray();
            array[0] = magnitude[i];
            array[1] = phase[i];            
            _dataCont.insertValuesAtEnd(array, _baseFrequency * i);
        }        
        
    }
            

    @Override
    public final float getValue(final int row, final int column) {
        return _dataCont.getValue(row, column);
    }

    public final void setValue(final float value, final int row, final int column) {
        throw new UnsupportedOperationException("this operation is not supported in the data container.");        
    }

    @Override
    public final int getRowLength() {
        return _dataCont.getRowLength();
    }

    @Override
    public final HiLoData getHiLoValue(final int row, final int columnStart, final int columnStop) {
        return _dataCont.getHiLoValue(row, columnStop, columnStop);        
    }

    @Override
    public final double getTimeValue(final int column, final int row) {
        return _timeSeries.getValue(column);
    }

    @Override
    public final int getMaximumTimeIndex(final int row) {        
        return _timeSeries.getMaximumIndex();                
    }

    @SuppressWarnings("PMD")
    @Override
    public void insertValuesAtEnd(final float[] values, final double timeValue) {        
        throw new UnsupportedOperationException("this operation is not supported in the data container.");        
    }    

    @Override
    public final HiLoData getAbsoluteMinMaxValue(final int row) {
        return _dataCont.getAbsoluteMinMaxValue(row);        
    }    

    @Override
    public final int findTimeIndex(final double time, final int row) {
        return _dataCont.findTimeIndex(time, row);        
    }

    @Override
    public final int getUsedRAMSizeInMB() {
        return _dataCont.getUsedRAMSizeInMB();
    }

    @Override
    public final long getCachedRAMSizeInMB() {
        return _dataCont.getCachedRAMSizeInMB();
    }

    @Override
    public Object getDataValueInInterval(double intervalStart, double intervalStop, int columnIndex) {
        return _dataCont.getDataValueInInterval(intervalStart, intervalStop, columnIndex);
    }
    
    
    @Override
    public String getSignalName(final int row) {
        return _dataCont.getSignalName(row);        
    }
    

    @Override
    public String getXDataName() {
        return _dataCont.getXDataName();
    }

    @Override
    public ContainerStatus getContainerStatus() {
        return ContainerStatus.PAUSED;
    }

    @Override
    public void setContainerStatus(ContainerStatus containerStatus) {
        _dataCont.setContainerStatus(containerStatus.PAUSED);
    }

    @Override
    public boolean isInvalidNumbers(int row) {
        return false;
    }

    @Override
    public AbstractTimeSerie getTimeSeries(final int row) {
        return _timeSeries;
    }    

    @Override
    public float[] getDataArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
