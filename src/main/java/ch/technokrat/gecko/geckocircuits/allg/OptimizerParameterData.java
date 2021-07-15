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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public final class OptimizerParameterData {

    private Map<String, Double> _dataMap = new LinkedHashMap<String, Double>();

    public List<String> getNameOpt() {
        return new ArrayList<String>(_dataMap.keySet());
    }

    public List<Double> getValueOpt() {
        List<Double> returnValue = new ArrayList<Double>();
        Set<Entry<String, Double>> entrySet = _dataMap.entrySet();

        for (Entry<String, Double> entry : entrySet) {
            returnValue.add(entry.getValue());
        }
        return returnValue;
    }

    public void clearAndInitializeWithoutUndo(final List<String> nameOpt, final List<Double> valueOpt) {
        if (nameOpt == null || valueOpt == null) { // for backwards compatibility with very old .ipes files
            _dataMap = new LinkedHashMap<String, Double>();
            return;
        }

        //assert nameOpt.size() == valueOpt.size() : nameOpt.size() + " " + valueOpt.size();
        Map<String, Double> newMap = new LinkedHashMap<String, Double>();
        for (int i = 0; i < Math.min(nameOpt.size(), valueOpt.size()); i++) {
            newMap.put(nameOpt.get(i), valueOpt.get(i));            
        }
        _dataMap = newMap;
    }

    public void reSetValuesWithUndo(final List<String> nameOpt, final List<Double> valueOpt) {
        Map<String, Double> oldMap = _dataMap;
        clearAndInitializeWithoutUndo(nameOpt, valueOpt);

        if (checkUndoAction(oldMap, _dataMap)) {
            setUndoAction(oldMap, _dataMap);
        }
    }

    public void clear() {
        _dataMap.clear();
    }

    double getNumberFromNameWOException(final String nameOpt) {
        if (_dataMap.containsKey(nameOpt)) {
            return _dataMap.get(nameOpt);
        } else {            
            return Double.NaN;
        }
    }

    public double getNumberFromName(final String nameOpt) {
        if (_dataMap.containsKey(nameOpt)) {
            return _dataMap.get(nameOpt);
        } else {
            throw new IllegalArgumentException("Global parameter with name: " + nameOpt + " is not available!");
        }
    }

    private boolean checkUndoAction(final Map<String, Double> oldMap, final Map<String, Double> newMap) {
        for (Entry<String, Double> entry1 : oldMap.entrySet()) {
            String key = entry1.getKey();
            if (newMap.containsKey(key)) {
                if (!oldMap.get(key).equals(newMap.get(key))) {
                    return true;
                }
            } else {
                return true;
            }
        }

        for (Entry<String, Double> entry1 : newMap.entrySet()) {
            String key = entry1.getKey();
            if (oldMap.containsKey(key)) {
                if (!oldMap.get(key).equals(newMap.get(key))) {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    private void setUndoAction(final Map<String, Double> oldMap, final Map<String, Double> newMap) {
        UndoableEdit undoEdit = new ParameterUndo(oldMap, newMap);
        AbstractUndoGenericModel.undoManager.addEdit(undoEdit);
    }

    public void setNumberFromName(String parameterName, double value) {
        if (!_dataMap.containsKey(parameterName)) {
            throw new IllegalArgumentException("Parameter with name: " + parameterName
                    + " does not exist in simulatin model!");
        }
        _dataMap.put(parameterName, value);

    }

    private final class ParameterUndo implements UndoableEdit {

        private final Map<String, Double> _oldMap;
        private final Map<String, Double> _newMap;

        public ParameterUndo(final Map<String, Double> oldMap, final Map<String, Double> newMap) {
            _oldMap = oldMap;
            _newMap = newMap;
        }

        @Override
        public void undo() {
            _dataMap = _oldMap;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() {
            _dataMap = _newMap;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
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
            return "Global parameters changed";
        }

        @Override
        public String getUndoPresentationName() {
            return "Global parameters changed";
        }

        @Override
        public String getRedoPresentationName() {
            return "Global parameters changed";
        }
    }
}
