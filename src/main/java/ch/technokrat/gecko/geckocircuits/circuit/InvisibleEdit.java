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

import javax.swing.undo.UndoableEdit;

public abstract class InvisibleEdit implements UndoableEdit {

    @Override
    public final boolean canUndo() {
        return true;
    }
    

    @Override
    public final boolean canRedo() {
        return true;
    }

    @Override
    public final void die() {
        // nothing todo
    }

    @Override
    public final boolean addEdit(final UndoableEdit anEdit) {
        return false;
    }

    @Override
    public final boolean replaceEdit(final UndoableEdit anEdit) {
        return false;
    }

    @Override
    public final boolean isSignificant() {
        return false;
    }

    @Override
    public final String getPresentationName() {
        return "invisible edit!";
    }

    @Override
    public final String getUndoPresentationName() {
        return "invisible edit!";
    }

    @Override
    public final String getRedoPresentationName() {
        return "invisible edit!";
    }
    
}
