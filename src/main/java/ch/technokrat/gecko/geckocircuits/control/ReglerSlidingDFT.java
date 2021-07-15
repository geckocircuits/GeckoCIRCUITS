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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.SlidingDFTCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public final class ReglerSlidingDFT extends AbstractReglerVariableInputs {

    private static final double DEFAULT_WIN_SIZE = 1e-5;
    private static final double DEFAULT_FREQENCY = 500;
    public static final ControlTypeInfo T_INFO = new ControlTypeInfo(ReglerSlidingDFT.class,"SDFT", I18nKeys.SDFT);
    
    final UserParameter<Double> _averageSpan = UserParameter.Builder.<Double>start("windowSpan", DEFAULT_WIN_SIZE).
            longName(I18nKeys.AVERAGING_TIME).
            shortName("T").
            unit("sec").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, -1).
            build();
    List<FrequencyData> _data = new ArrayList<FrequencyData>() {

        @Override
        public void clear() {
            for(int i = 0, size = _data.size(); i < size; i++) {
                removeLastFrequencyData();
            }
        }        
    };

    public ReglerSlidingDFT() {
        super(1);
        _data.add(new FrequencyData(DEFAULT_FREQENCY, OutputData.ABS));
    }
    
    public List<FrequencyData> getFrequencyData() {
        return Collections.unmodifiableList(_data);
    }
    
    public void setFrequencyDataWithUndoCheck(final List<FrequencyData> newList) {
        
        if(isUndoRequired(_data, newList)) {
            final FrequencyDataUndoableEdit edit = new FrequencyDataUndoableEdit(_data, newList);
            AbstractUndoGenericModel.undoManager.addEdit(edit);                    
        } 
        
        _data = newList;
        setOutputTerminalNumber(_data.size());
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"DFT"};
    }    

    private boolean isUndoRequired(final List<FrequencyData> oldList, final List<FrequencyData> newList) {
        if(oldList.size() != newList.size()) {
            return true;
        }
        for(int i = 0; i < oldList.size(); i++) {
            if(!oldList.get(i).equals(newList.get(i))) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void copyAdditionalParameters(AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);
        _data.clear();
        for(FrequencyData dat : ((ReglerSlidingDFT) originalBlock)._data) {
            _data.add(new FrequencyData(dat._frequency.getValue(), dat._outputData));
        }        
    }
    

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.SLIDING_FOURIER_TRANSFORM_DESCRIPTION};
    }

    void addDataPoint() {        
        final double newFrequencyValue = _data.get(_data.size()-1)._frequency.getDoubleValue();
        _data.add(new FrequencyData(newFrequencyValue * 2, OutputData.ABS));
    }

    void removeLastFrequencyData() {
        FrequencyData removed = _data.remove(_data.size()-1);
        unregisterParameter(removed._frequency);        
    }
    

    public enum OutputData {

        ABS(1, "Magnitude"),
        REAL(2, "Real"),
        IMAG(3, "Imag"),
        PHASE(4, "Phase");
        private int _integerCode = 1;
        private final String _outputString;
        
        OutputData(final int code, final String outputString) {
            _integerCode = code;
            _outputString = outputString;
        }

        @Override
        public String toString() {
            return _outputString;
        }
        

        static OutputData getFromIntCode(final int code) {
            for (OutputData compare : OutputData.values()) {
                if (compare.getIntegerCode() == code) {
                    return compare;
                }
            }
            assert false;
            return OutputData.ABS;
        }

        private int getIntegerCode() {
            return _integerCode;
        }
    }

    public final class FrequencyData {
        
        public final UserParameter<Double> _frequency;
        public OutputData _outputData;

        public FrequencyData(final double frequency, final OutputData outputData) {
            final int stepNumber = _data.size()+1;
            _outputData = outputData;            
            _frequency = UserParameter.Builder.<Double>start("freq_" + stepNumber, frequency).
            longName(I18nKeys.SDFT_FREQUENCY).
            shortName("F" + stepNumber).
            unit("Hz").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(ReglerSlidingDFT.this, -1).
            build();
        }        

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FrequencyData other = (FrequencyData) obj;
            if (Double.doubleToLongBits(this._frequency.getValue()) != Double.doubleToLongBits(other._frequency.getValue())) {
                return false;
            }
            if (this._outputData != other._outputData) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return (int) (long) Double.doubleToLongBits(_frequency.getValue()) + _outputData.hashCode();
        }

        
        
    }    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new SlidingDFTCalculator(YOUT.size(), _averageSpan.getValue(), _data);
    }    
    

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii);

        double[] outputFreqs = new double[_data.size()];
        String[] outputNameOpts = new String[_data.size()];
        int[] outputTypes = new int[_data.size()];
        for (int i = 0; i < _data.size(); i++) {
            outputFreqs[i] = _data.get(i)._frequency.getValue();
            outputTypes[i] = _data.get(i)._outputData.getIntegerCode();
            outputNameOpts[i] = _data.get(i)._frequency.getNameOpt();
        }
        
        DatenSpeicher.appendAsString(ascii.append("\noutputTypes"), outputTypes);
        DatenSpeicher.appendAsString(ascii.append("\noutputFrequencies"), outputFreqs);
        DatenSpeicher.appendAsString(ascii.append("\nfrequenciesNameOpt"), outputNameOpts);

    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        
        _data.clear();        
        
        final double[] outputFrequencies = tokenMap.readDataLine("outputFrequencies[]", new double[]{DEFAULT_FREQENCY});        
        String[] outputFreqNameOpt = new String[outputFrequencies.length];
        for(int i = 0; i < outputFreqNameOpt.length; i++) {
            outputFreqNameOpt[i] = "";
        }
        final int[] outputTypes = tokenMap.readDataLine("outputTypes[]", new int[]{1});
        if(tokenMap.containsToken("frequenciesNameOpt[]")) {
            outputFreqNameOpt = tokenMap.readDataLine("frequenciesNameOpt[]", outputFreqNameOpt);            
        }
        
        assert outputFrequencies.length == outputTypes.length;

        for (int i = 0; i < outputFrequencies.length; i++) {
            FrequencyData data = new FrequencyData(outputFrequencies[i], OutputData.getFromIntCode(outputTypes[i]));
            if(i < outputFreqNameOpt.length && !outputFreqNameOpt[i].isEmpty()) {
                data._frequency.setNameOpt(outputFreqNameOpt[i]);
            }            
            _data.add(data);
        }
        setOutputTerminalNumber(_data.size());
    }

    
    
    

    @Override
    protected Window openDialogWindow() {
        //return new ReglerSlidingDFTDialog(this);
        return new ReglerSlidingDFTDialog(this);
    }
    
    private class FrequencyDataUndoableEdit implements UndoableEdit {
        private List<FrequencyData> _oldList;
        private List<FrequencyData> _newList;
        public FrequencyDataUndoableEdit(final List<FrequencyData> oldList, final List<FrequencyData> newList) {
            _oldList = oldList;
            _newList = newList;
        }
        
        @Override
        public void undo() {
            _data = _oldList;
            setOutputTerminalNumber(_data.size());
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() {
            _data = _newList;
            setOutputTerminalNumber(_data.size());
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            _newList = Collections.EMPTY_LIST;
            _oldList = Collections.EMPTY_LIST;
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Frequency selection of SFFT";
        }

        @Override
        public String getUndoPresentationName() {
            return "Frequency selection of SFFT";
        }

        @Override
        public String getRedoPresentationName() {
            return "Frequency selection of SFFT";
        }        
    }
    
}
