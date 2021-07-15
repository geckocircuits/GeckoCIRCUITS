/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.ControlSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SourceType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.SSAShape;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;
import ch.technokrat.modelviewcontrol.ModelMVC;

/**
 *
 * @author andreas
 */
public final class UserParameter<T> {

    private final List<String> _unit;
    private int _index = -1;
    private ModelMVC<T> _value;
    private final String _identifier;
    private final List<ConnectorType> _typeMap;
    private final List<String> _shortNames;
    // this field can be used to maintain backward-compatibility with older versions.
    private final List<String> _alternativeShortName;
    private final List<String> _longName;
    private AbstractBlockInterface _parameterableObject;
    private String _nameOpt = "";
    private final TextInfoType _textInfoType;
    private final UserParameter<? extends Enum> _enumConditionParameter;
    private final Enum _enumConditionValue;
    private static final String NAME_OPT_EXTENSION = "$ParameterName";
    private final String _identifierNameOpt;
    
    private UserParameter(final Builder<T> builder) {
        _unit = builder._units;
        _index = builder._index;
        _shortNames = builder._shortNames;
        if (builder._alternativeShortNames.isEmpty()) {
            _alternativeShortName = builder._shortNames;
        } else {
            _alternativeShortName = builder._alternativeShortNames;
        }

        _longName = builder._longNames;
        _identifier = builder._identifier;
        _identifierNameOpt = _identifier + NAME_OPT_EXTENSION;
        _typeMap = builder._connectorTypeMap;
        _parameterableObject = builder._paramterableObject;

        Object description = getShortName();
        if(_parameterableObject != null) {
            description = new Object() {

                @Override
                public String toString() {
                    return _parameterableObject.getIDStringDialog() + ", value " + getShortName();
                }                                
            };
        }
        
        _value = new ModelMVC<T>(builder._initialValue, description);
        _textInfoType = builder._textInfoType;
        _enumConditionParameter = builder._enumConditionParameter;
        _enumConditionValue = builder._enumConditionValue;
        
        if (_index >= 0) {
            _value.addModelListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    _parameterableObject.parameter[_index] = getDoubleValue();
                }
            });
        }
    }
    
    public boolean isShowTypeInfoConditionFromEnum() {
        if(_enumConditionParameter == null || _enumConditionValue == null) {
            return true;
        }
        return _enumConditionParameter.getValue() == _enumConditionValue;
    }

    public TextInfoType getTextInfoType() {
        return _textInfoType;
    }
        

    public String getNameOpt() {
        return _nameOpt;
    }

    public void setNameOpt(String newParameterName) {
        if (_nameOpt.equals(newParameterName)) {
            return;
        }
        insertNameOptUndoableEdit(_nameOpt, newParameterName);

        _nameOpt = newParameterName;
    }

    public String getUnit() {
        if (_unit == null || _unit.isEmpty()) {
            return "";
        }
        return getFromMultipleValues(_unit);
    }

    public String getShortName() {
        return getFromMultipleValues(_shortNames);
    }

    public String getAlternativeShortName() {
        if (_alternativeShortName == null) {
            return "";
        }
        return getFromMultipleValues(_alternativeShortName);
    }

    public int getParameterIndex() {
        return _index;
    }

    public String getLongName() {
        return getFromMultipleValues(_longName);
    }

    private <T> T getFromMultipleValues(List<T> list) {
        try {
            if (_typeMap == null || _typeMap.isEmpty() || list.size() == 1) {
                return list.get(0);
            } else {
                int index = _typeMap.indexOf(_parameterableObject.getSimulationDomain());
                return list.get(index);
            }
        } catch (Throwable ex) {
            System.err.println("could not find domain " + " " + _parameterableObject.getSimulationDomain()
                    + " " + _parameterableObject.getStringID() + " from component\n"
                    + " " + _parameterableObject.getClass() + " \n parameter: " + _longName);
            System.err.println("available domains are: ");
            System.out.println("typemap: " + _typeMap);
            for (ConnectorType type : _typeMap) {
                System.out.println(type);
            }
            ex.printStackTrace();
            return null;
        }
    }

    public T getValue() {
        if (!_nameOpt.isEmpty() && _value.getValue() instanceof Number) {            
            Double returnValue = (Double) Fenster.optimizerParameterData.getNumberFromNameWOException(_nameOpt);
            if(_value.getValue() instanceof Integer) {
                return (T) (Integer) returnValue.intValue();
            } else {
                return (T) returnValue;
            }
        }
        return _value.getValue();
    }

    public void setUserValue(T newValue) {
        _value.setValue(newValue);
    }

    public void setValueWithoutUndo(final T newValue) {
        _value.setValueWithoutUndo(newValue);
    }

    public void setFromDoubleValue(double newValue) {        
        if (_value.getValue() instanceof Double) {
            _value.setValue((T) (Double) newValue);
            return;
        }

        if (_value.getValue() instanceof Integer) {
            _value.setValue((T) (Integer) (int) newValue);
            return;
        }
        if (_value.getValue() instanceof Boolean) {
            if (newValue > 0.5) {
                _value.setValue((T) (Boolean) true);
            } else {
                _value.setValue((T) (Boolean) false);
            }
            return;
        }

        if (_value.getValue() instanceof ControlSourceType) {
            _value.setValue((T) ControlSourceType.getFromID((int) newValue));
            return;
        }

        if (_value.getValue() instanceof CircuitSourceType) {
            _value.setValue((T) CircuitSourceType.getFromID((int) newValue));
            return;
        }

        assert false : _value.getValue().getClass() + " for component " + _parameterableObject;
    }

    public String getSaveIdentifier() {
        return _identifier;
    }

    public void setCorrectedIndex(final int newIndex) {
        _index = newIndex;
    }

    public double getDoubleValue() {
        if (!_nameOpt.isEmpty()) {
                return Fenster.optimizerParameterData.getNumberFromNameWOException(_nameOpt);
        }

        if (_value.getValue() instanceof Number) {
            return ((Number) _value.getValue()).doubleValue();
        }

        if (_value.getValue() instanceof Boolean) {
            boolean bool = (Boolean) _value.getValue();
            if (bool) {
                return 1;
            } else {
                return 0;
            }
        }

        if (_value.getValue() instanceof ControlSourceType) {
            ControlSourceType type = (ControlSourceType) _value.getValue();
            return type.getOldGeckoID();
        }

        if (_value.getValue() instanceof CircuitSourceType) {
            CircuitSourceType type = (CircuitSourceType) _value.getValue();
            return type.getOldGeckoID();
        }
        
        if(_value.getValue() instanceof SSAShape) {            
            return ((SSAShape) _value.getValue()).ordinal();
        }

        assert false : _value.getClass();
        return 0;
    }

    public void addActionListener(final ActionListener actionListener) {
        _value.addModelListener(actionListener);
    }

    public void readFromTokenMap(final TokenMap tokenMap) {
        final T oldValue = _value.getValue();                                
        
        if (tokenMap.containsToken(_identifier)) {                        
            
            if(tokenMap.containsToken(_identifierNameOpt)) {
                _nameOpt = tokenMap.readDataLine(_identifierNameOpt, "");            
            }
            
            if (oldValue instanceof Double) {
                final Double newValue = tokenMap.readDataLine(_identifier, (Double) _value.getValue());
                _value.setValueWithoutUndo((T) newValue);                                                
                return;
            }
            if (oldValue instanceof Boolean) {
                final Boolean newValue = tokenMap.readDataLine(_identifier, (Boolean) _value.getValue());
                _value.setValueWithoutUndo((T) newValue);
                return;
            }

            if (oldValue instanceof Integer) {
                final Integer newValue = tokenMap.readDataLine(_identifier, (Integer) _value.getValue());
                _value.setValueWithoutUndo((T) newValue);
                return;
            }

            if (oldValue instanceof ControlSourceType) {
                final ControlSourceType newValue = ControlSourceType.getFromID(tokenMap.readDataLine(_identifier, ControlSourceType.QUELLE_RECHTECK.getOldGeckoID()));
                _value.setValueWithoutUndo((T) newValue);
                return;
            }
            
            if(oldValue instanceof SSAShape) {                
                final SSAShape newValue = SSAShape.getFromOrdinal(tokenMap.readDataLine(_identifier, SSAShape.RECTANGLE.ordinal()));
                _value.setValueWithoutUndo((T) newValue);
                return;
            }

            if (oldValue instanceof CircuitSourceType) {
                final CircuitSourceType newValue = CircuitSourceType.getFromID(tokenMap.readDataLine(_identifier, SourceType.QUELLE_DC_NEW));
                _value.setValueWithoutUndo((T) newValue);
                return;
            }

            if (oldValue instanceof String) {
                final String newValue = tokenMap.readDataLine(_identifier, (String) _value.getValue());
                _value.setValueWithoutUndo((T) newValue);
                return;
            }

            if (oldValue instanceof Color) {
                final Color newValue = new Color(tokenMap.readDataLine(_identifier, ((Color) _value.getValue()).getRGB()));
                _value.setValueWithoutUndo((T) newValue);
                return;
            }                        

            assert false;
        } else {
            //System.err.println("Value not found: " + _identifier);
        }

    }

    public void readFromNameOptArray(String[] nameOpt) {       
        if (_index < 0) {
            return;
        }
        if (nameOpt.length <= _index) {
            return;
        }

        if (nameOpt[_index].isEmpty()) {
            return;
        }
        _nameOpt = nameOpt[_index];
    }

    public void readFromParameterArray(final double[] parameters) {
        if (_index < 0) {
            return;
        }

        if (_index >= parameters.length) {
            return;
        }

        T oldValue = getValue();

        if (oldValue instanceof Double) {
            _value.setValue((T) (Double) parameters[_index]);
            return;
        }

        if (oldValue instanceof Integer) {
            _value.setValue((T) (Integer) (int) parameters[_index]);
            return;
        }

        if (oldValue instanceof Boolean) {
            if (parameters[_index] > 0.5) {
                _value.setValue((T) (Boolean) true);
            } else {
                _value.setValue((T) (Boolean) false);
            }
            return;
        }

        if (oldValue instanceof ControlSourceType) {
            _value.setValue((T) ControlSourceType.getFromID((int) parameters[_index]));
            return;
        }

        if (oldValue instanceof CircuitSourceType) {
            _value.setValue((T) CircuitSourceType.getFromID((int) parameters[_index]));
            return;
        }


        assert false;
    }

    public void readFromParamterArrayWithoutUndo(final double[] tmpArray) {

        T oldValue = getValue();
        if (_index < 0) {
            return;
        }
        if (oldValue instanceof Double) {
            _value.setValueWithoutUndo((T) (Double) tmpArray[_index]);
            return;
        }

        if (oldValue instanceof Integer) {
            _value.setValueWithoutUndo((T) (Integer) (int) tmpArray[_index]);
            return;
        }

        if (oldValue instanceof Boolean) {
            if (tmpArray[_index] > 0.5) {
                _value.setValueWithoutUndo((T) (Boolean) true);
            } else {
                _value.setValueWithoutUndo((T) (Boolean) false);
            }
            return;
        }

        if (oldValue instanceof ControlSourceType) {
            _value.setValueWithoutUndo((T) ControlSourceType.getFromID((int) tmpArray[_index]));
            return;
        }

        if (oldValue instanceof CircuitSourceType) {
            _value.setValueWithoutUndo((T) CircuitSourceType.getFromID((int) tmpArray[_index]));
            return;
        }

        assert false : oldValue.getClass();
    }

    public void writeNameOptArray(String[] nameOpt) {
        if (_index < 0) {
            return;
        }
        if (nameOpt.length <= _index) {
            return;
        }
        nameOpt[_index] = _nameOpt;
    }

    public void writeToParamterArray(final double[] tmpArray) {
        T oldValue = getValue();

        if (_index < 0) {
            return;
        }

        if (oldValue instanceof Double && _index >= 0) {
            tmpArray[_index] = (Double) getValue();
            return;
        }

        if (oldValue instanceof Integer && _index >= 0) {
            tmpArray[_index] = (Integer) getValue();
            return;
        }

        if (oldValue instanceof Boolean) {
            Boolean value = (Boolean) getValue();

            if (value) {
                tmpArray[_index] = 1;
            } else {
                tmpArray[_index] = 0;
            }
            return;
        }

        if (oldValue instanceof ControlSourceType) {
            ControlSourceType value = (ControlSourceType) getValue();
            tmpArray[_index] = value.getOldGeckoID();
            return;
        }

        if (oldValue instanceof CircuitSourceType) {
            CircuitSourceType value = (CircuitSourceType) getValue();
            tmpArray[_index] = value.getOldGeckoID();
            return;
        }

        assert _index < 0;
    }

    public void writeXMLToFile(final StringBuffer ascii) {
        T value = getValue();
        
        final String nameOptString = getNameOpt();
        if(!getNameOpt().isEmpty()) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifierNameOpt), nameOptString);
        }
        
        if (value instanceof Double) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), (Double) value);
            return;
        }
        if (value instanceof Boolean) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), (Boolean) value);
            return;
        }

        if (value instanceof Integer) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), (Integer) value);
            return;
        }

        if (value instanceof ControlSourceType) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), ((ControlSourceType) value).getOldGeckoID());
            return;
        }

        if (value instanceof CircuitSourceType) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), ((CircuitSourceType) value).getOldGeckoID());
            return;
        }
        
        if (value instanceof SSAShape) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), ((SSAShape) value).ordinal());
            return;
        }
        
        

        if (value instanceof String) {
            String writeString = (String) value;
            if(writeString.contains("\n")) {                
                writeString = writeString.replaceAll("\n", "\\\\n");                                
            }
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), writeString);
            return;
        }

        if (value instanceof Color) {
            DatenSpeicher.appendAsString(ascii.append("\n" + _identifier), ((Color) value).getRGB());
            return;
        }                

        try {
            assert false;
        } catch (AssertionError err) {
            err.printStackTrace();
        }

    }

    private void insertNameOptUndoableEdit(final String oldName, final String newName) {
        UndoableEdit edit = new GlobalParameterUndoable(oldName, newName);
        AbstractUndoGenericModel.undoManager.addEdit(edit);
    }

    private class GlobalParameterUndoable implements UndoableEdit {

        private final String _oldValue;
        private final String _newValue;

        public GlobalParameterUndoable(final String oldValue, final String newValue) {
            _oldValue = oldValue;
            _newValue = newValue;
        }

        @Override
        public void undo() throws CannotUndoException {
            _nameOpt = _oldValue;
        }

        @Override
        public void redo() throws CannotRedoException {
            _nameOpt = _newValue;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            // nothing todo
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Paramter change";
        }

        @Override
        public String getUndoPresentationName() {
            return "Parameter change: " + _oldValue + " >> " + _newValue;
        }

        @Override
        public String getRedoPresentationName() {
            return "Parameter change: " + _newValue + " >> " + _oldValue;
        }
    }

    public static class Builder<T> {

        private int _index = -1;
        private String _identifier;
        private final T _initialValue;
        private List<ConnectorType> _connectorTypeMap;
        private ConnectorType _activeDomain;
        private final List<String> _units = new ArrayList<String>();
        private final List<String> _shortNames = new ArrayList<String>();
        private final List<String> _alternativeShortNames = new ArrayList<String>();
        private final List<String> _longNames = new ArrayList<String>();

        public static <T> Builder<T> start(final String saveIdentifier, final T initValue) {
            return new Builder<T>(saveIdentifier, initValue);
        }
                
        
        private AbstractBlockInterface _paramterableObject;
        private TextInfoType _textInfoType = TextInfoType.SHOW_NEVER;
        private UserParameter<? extends Enum> _enumConditionParameter;
        private Enum _enumConditionValue;

        private Builder(final String saveIdentifier, T initialValue) {
            _identifier = saveIdentifier;
            _initialValue = initialValue;
        }

        public UserParameter.Builder showWhenEnumValueIsSet(UserParameter<? extends Enum> enumParameter, Enum enumValue) {
            _enumConditionParameter = enumParameter;
            _enumConditionValue = enumValue;
            return this;
        }
        
        public UserParameter.Builder showInTextInfo(final TextInfoType textInfoType) {
            _textInfoType = textInfoType;
            return this;
        }

        public UserParameter.Builder unit(final String... units) {
            assert units.length > 0;
            Collections.addAll(_units, units);
            return this;
        }

        public UserParameter.Builder arrayIndex(final AbstractBlockInterface parameterBlock, final int index) {
            _index = index;
            _paramterableObject = parameterBlock;
            return this;
        }

        public UserParameter.Builder longName(final I18nKeys name, final I18nKeys... additionalNames) {
            _longNames.add(name.getTranslation());
            for(I18nKeys additionalName : additionalNames) {
                _longNames.add(additionalName.getTranslation());
            }            
            return this;
        }

        public UserParameter.Builder shortName(final String... shortNames) {
            assert shortNames.length > 0;
            Collections.addAll(_shortNames, shortNames);
            return this;
        }

        /**
         * this method can be used for backwards-compatiblity with old names.
         * Reason: Existing GeckoSCRIPT-Code may rely on old "short names"
         * @param alternativeName
         * @param additionalAlternativeNames
         * @return 
         */
        public UserParameter.Builder addAlternativeShortName(final String... additionalAlternativeNames) {            
            Collections.addAll(_alternativeShortNames, additionalAlternativeNames);
            return this;
        }

        public UserParameter.Builder mapDomains(final ConnectorType... connectorTypes) {
            assert connectorTypes.length > 0;
            this._connectorTypeMap = new ArrayList<ConnectorType>();            
            Collections.addAll(_connectorTypeMap, connectorTypes);
            return this;
        }

        public UserParameter<T> build() {
            UserParameter returnValue = new UserParameter<T>(this);
            if (returnValue._parameterableObject != null) {
                returnValue._parameterableObject.registerParameter(returnValue);
            }
            return returnValue;
        }

        
    }
}
