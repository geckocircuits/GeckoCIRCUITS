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

/**
 * GUI-free interface for undoable edits.
 * 
 * This interface mirrors javax.swing.undo.UndoableEdit but without
 * Swing dependencies, allowing undo/redo functionality in headless
 * environments.
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
public interface GeckoUndoableEdit {
    
    /**
     * Undo this edit.
     * @throws IllegalStateException if cannot undo
     */
    void undo() throws IllegalStateException;
    
    /**
     * Redo this edit (undo the undo).
     * @throws IllegalStateException if cannot redo
     */
    void redo() throws IllegalStateException;
    
    /**
     * @return true if this edit can be undone
     */
    boolean canUndo();
    
    /**
     * @return true if this edit can be redone
     */
    boolean canRedo();
    
    /**
     * @return a localized, human-readable description of this edit
     */
    String getPresentationName();
    
    /**
     * @return description for undo menu item (e.g., "Undo Move")
     */
    default String getUndoPresentationName() {
        return "Undo " + getPresentationName();
    }
    
    /**
     * @return description for redo menu item (e.g., "Redo Move")
     */
    default String getRedoPresentationName() {
        return "Redo " + getPresentationName();
    }
    
    /**
     * @return true if this edit is significant (should be undoable by user)
     */
    default boolean isSignificant() {
        return true;
    }
    
    /**
     * Try to absorb another edit into this one.
     * @param edit the edit to absorb
     * @return true if successfully absorbed
     */
    default boolean addEdit(GeckoUndoableEdit edit) {
        return false;
    }
    
    /**
     * Try to replace this edit with another.
     * @param edit the edit to replace with
     * @return true if successfully replaced
     */
    default boolean replaceEdit(GeckoUndoableEdit edit) {
        return false;
    }
    
    /**
     * Notify this edit that it is no longer needed.
     */
    default void die() {
        // Default: no cleanup needed
    }
}
