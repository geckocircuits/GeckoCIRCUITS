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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.control.AbstractPotentialMeasurement;
import ch.technokrat.gecko.geckocircuits.control.Operationable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public class PotentialCoupling {

    private final AbstractBlockInterface _parent;
    private final int[] _stringIDIndices;
    private final ConnectorType _potentialType;
    private String[] _labelsBeforeCopyRename;

    public PotentialCoupling(AbstractBlockInterface parent, int[] stringIDIndices, ConnectorType potentialType) {
        _parent = parent;
        _stringIDIndices = stringIDIndices;
        _potentialType = potentialType;
    }

    public void setNewCouplingLabel(int i, String newLabel) {
        final String oldLabel = _parent.getParameterString()[_stringIDIndices[i]];
        if (oldLabel.equals(newLabel)) {
            return;
        }
        ReferencedLabelChange undoEdit = new ReferencedLabelChange(oldLabel, newLabel, i, false);
        AbstractUndoGenericModel.undoManager.addEdit(undoEdit);
        _parent.getParameterString()[_stringIDIndices[i]] = newLabel;
    }

    public void setNewCouplingLabelUserDialog(int i, String newLabel) {
        final String oldLabel = _parent.getParameterString()[_stringIDIndices[i]];
        if (oldLabel.equals(newLabel)) {
            return;
        }
        ReferencedLabelChange undoEdit = new ReferencedLabelChange(oldLabel, newLabel, i, true);
        AbstractUndoGenericModel.undoManager.addEdit(undoEdit);
        _parent.getParameterString()[_stringIDIndices[i]] = newLabel;
    }

    ConnectorType getLinkType() {
        return _potentialType;
    }

    void renameUpdate(final String oldLabel, final String newLabel) {
        for (int i = 0; i < _stringIDIndices.length; i++) {
            String[] parameterString = _parent.getParameterString();
            final String savedString = parameterString[_stringIDIndices[i]];

            if (!oldLabel.isEmpty() && savedString.equals(oldLabel)) {
                setNewCouplingLabel(_stringIDIndices[i], newLabel);
            }
        }
    }

    public AbstractBlockInterface getParent() {
        return _parent;
    }

    void saveLabelsBeforeCopyRename() {
        _labelsBeforeCopyRename = new String[_stringIDIndices.length];
        for (int i = 0; i < _stringIDIndices.length; i++) {
            _labelsBeforeCopyRename[i] = _parent.getParameterString()[_stringIDIndices[i]];
        }
    }

    void tryFindChangedLabels(Collection<? extends AbstractCircuitSheetComponent> exchangeNew, int endIndex) {
        for (int i = 0; i < _stringIDIndices.length; i++) {
            try {
                String myOrigLabel = _labelsBeforeCopyRename[i];

                for (AbstractCircuitSheetComponent search : exchangeNew) {
                    if (search instanceof ComponentTerminable) {
                        for (TerminalInterface term : ((ComponentTerminable) search).getAllTerminals()) {
                            String termLabel = term.getLabelObject().getLabelString();
                            if (termLabel.isEmpty()) {
                                continue;
                            }
                            int endingStringIndex = termLabel.lastIndexOf("." + endIndex);
                            String termLabelBeforeCopy = termLabel.substring(0, endingStringIndex);
                            if (myOrigLabel.equals(termLabelBeforeCopy)) {
                                setNewCouplingLabel(i, termLabel);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                setNewCouplingLabel(i, "");
                ex.printStackTrace();
            }
        }
    }

    public String[] getLabels() {
        String[] returnValue = new String[_stringIDIndices.length];
        for (int i = 0; i < _stringIDIndices.length; i++) {
            returnValue[i] = _parent.getParameterString()[_stringIDIndices[i]];
        }
        return returnValue;
    }

    public ConnectorType getType() {
        return _potentialType;
    }

    private class ReferencedLabelChange implements UndoableEdit {

        final boolean _isSignificant;
        private final String _oldLabel;
        private final String _newLabel;
        private final int _index;

        private ReferencedLabelChange(final String oldLabel, final String newLabel, final int index,
                final boolean isSignificant) {
            _index = index;
            _oldLabel = oldLabel;
            _newLabel = newLabel;
            _isSignificant = isSignificant;
        }

        @Override
        public void undo() throws CannotUndoException {
            _parent.getParameterString()[_stringIDIndices[_index]] = _oldLabel;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() throws CannotRedoException {
            _parent.getParameterString()[_stringIDIndices[_index]] = _newLabel;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            // nothing todo!
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
            return _isSignificant;
        }

        @Override
        public String getPresentationName() {
            return "Label reference change";
        }

        @Override
        public String getUndoPresentationName() {
            return "Label reference from \"" + _oldLabel + "\" to \"" + _newLabel + "\"";
        }

        @Override
        public String getRedoPresentationName() {
            return "Label reference from \"" + _newLabel + "\" to \"" + _oldLabel + "\"";
        }
    }
    
    
    class SetOperation extends Operationable.OperationInterface {
        final int _index;
        SetOperation(final int index) {            
            super(((index == 0) ? "setCouplingLabel" : "setSecondCouplingLabel"), I18nKeys.SET_COUPLING_LABEL);
            _index = index;
        }
        @Override
            public Object doOperation(final Object parameterValue) {
                if (!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Parameter type must be a String!");
                }
                                                                                                    
                setNewCouplingLabel(_index, (String) parameterValue);
                if(_parent instanceof AbstractPotentialMeasurement) {
                    ((AbstractPotentialMeasurement) _parent).getComponentCoupling().setNewCouplingElement(0, null);
                }
                return null;                                                                                
            }
        
    }    

    public List<Operationable.OperationInterface> getOperationInterfaces() {
        List<Operationable.OperationInterface> returnValue = new ArrayList<Operationable.OperationInterface>();
        
        Operationable.OperationInterface op0 = new SetOperation(0);
        returnValue.add(op0);
        
        if(_stringIDIndices.length == 2) {
            Operationable.OperationInterface op1 = new SetOperation(1);
            returnValue.add(op1);
        }
        
       return returnValue;
    }
    
}
