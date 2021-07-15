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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public class CircuitLabel {
    private String _label;      
    private LabelPriority _labelPriority = LabelPriority.NORMAL;
    
    public CircuitLabel() {
        _label = "";
    }

    public void setLabel(final String newLabel) {
        if(newLabel.equals(_label)) {
            return;
        }        
        if(!_label.equals(newLabel)) {            
            RenameLabelUndoableEdit undoEdit = new RenameLabelUndoableEdit(_label, newLabel, false);
            AbstractUndoGenericModel.undoManager.addEdit(undoEdit);
        }        
        _label = newLabel;
    }
    
    public void setLabelWithoutUndo(final String newLabel) {
        assert newLabel != null;
        _label = newLabel;
    }
    

    public String getLabelString() {
        return _label;
    }
    
    public LabelPriority getLabelPriority() {
        if (_label.isEmpty() && _labelPriority == LabelPriority.NORMAL) {
            return LabelPriority.EMPTY_STRING;
        } else {
            return _labelPriority;
        }                
    }
    
    public void setLabelPriority(final LabelPriority priority) {
        _labelPriority = priority;
    }

    public void clearPriority() {
        _labelPriority = LabelPriority.NORMAL;
    }

    public void setLabelFromUserDialog(final String newLabel) {
        _labelPriority = LabelPriority.FORCE_NAME;
        if(newLabel.equals(_label)) {
            return;
        }
        RenameLabelUndoableEdit undoEdit = new RenameLabelUndoableEdit(_label, newLabel, true);
        AbstractUndoGenericModel.undoManager.addEdit(undoEdit);
        _label = newLabel;                
    }
    
    private class RenameLabelUndoableEdit implements UndoableEdit {
        final boolean _isSignificant;        
        private final String _oldLabel;
        private final String _newLabel;

        private RenameLabelUndoableEdit(final String oldLabel, final String newLabel,
                final boolean isSignificant) {            
            _oldLabel = oldLabel;
            _newLabel = newLabel;
            _isSignificant = isSignificant;            
        }

        @Override
        public void undo() throws CannotUndoException {
            _label = _oldLabel;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() throws CannotRedoException {
            _label = _newLabel;
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
            return "Label rename";
        }

        @Override
        public String getUndoPresentationName() {
            return "Label rename from \"" + _oldLabel + "\" to \"" + _newLabel + "\"";
        }

        @Override
        public String getRedoPresentationName() {
            return "Label rename from \"" + _newLabel + "\" to \"" + _oldLabel + "\"";
        }
    }
    
    
}
