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
package ch.technokrat.gecko.geckocircuits.circuit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Adapter that wraps a GeckoUndoableEdit as a Swing UndoableEdit.
 * 
 * Use this in GUI code to integrate GUI-free edits with Swing's UndoManager.
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
public class GeckoUndoableEditAdapter implements UndoableEdit {
    
    private final GeckoUndoableEdit delegate;
    
    public GeckoUndoableEditAdapter(GeckoUndoableEdit delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void undo() throws CannotUndoException {
        try {
            delegate.undo();
        } catch (IllegalStateException e) {
            throw new CannotUndoException();
        }
    }
    
    @Override
    public void redo() throws CannotRedoException {
        try {
            delegate.redo();
        } catch (IllegalStateException e) {
            throw new CannotRedoException();
        }
    }
    
    @Override
    public boolean canUndo() {
        return delegate.canUndo();
    }
    
    @Override
    public boolean canRedo() {
        return delegate.canRedo();
    }
    
    @Override
    public void die() {
        delegate.die();
    }
    
    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        if (anEdit instanceof GeckoUndoableEditAdapter) {
            return delegate.addEdit(((GeckoUndoableEditAdapter) anEdit).delegate);
        }
        return false;
    }
    
    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {
        if (anEdit instanceof GeckoUndoableEditAdapter) {
            return delegate.replaceEdit(((GeckoUndoableEditAdapter) anEdit).delegate);
        }
        return false;
    }
    
    @Override
    public boolean isSignificant() {
        return delegate.isSignificant();
    }
    
    @Override
    public String getPresentationName() {
        return delegate.getPresentationName();
    }
    
    @Override
    public String getUndoPresentationName() {
        return delegate.getUndoPresentationName();
    }
    
    @Override
    public String getRedoPresentationName() {
        return delegate.getRedoPresentationName();
    }
    
    /**
     * Get the wrapped GUI-free edit.
     */
    public GeckoUndoableEdit getDelegate() {
        return delegate;
    }
}
