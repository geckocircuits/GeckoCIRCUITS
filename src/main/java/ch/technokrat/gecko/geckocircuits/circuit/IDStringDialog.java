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

import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

/**
 *
 * @author andreas
 */
public final class IDStringDialog {

    private final AbstractBlockInterface _parent;
    private String _idString;
    private static Map<String, ArrayList<AbstractBlockInterface>> _allIDStrings = new HashMap<String, ArrayList<AbstractBlockInterface>>();
    public static Random rand = new Random(System.currentTimeMillis());
    public static final int MAX_SEARCH_NUMBER = 10000;
    final List<ActionListener> actionListeners = new ArrayList<ActionListener>();

    private IDStringDialog(AbstractBlockInterface parent, final String newName) {
        _idString = newName;
        _parent = parent;
    }

    public static void clearAllNames() {
        _allIDStrings.clear();
    }

    /**
     * use this fabric when you would like to construct an ID string, that is
     * not considered in the program as "name already used". Only know usage for
     * now: for creating the initial components for the SchematischeEingabe
     * component display / search
     *
     * @return
     */
    public static IDStringDialog fabricDummyObjectWithoutEffect(AbstractBlockInterface parent) {
        return new IDStringDialog(parent, parent.getFixedIDString() + ".1");
    }

    /**
     * use this fabric, if the name is a suggestion. If it already exists,
     * iterate a counter number and give the modified value as return value.
     *
     * @param newName
     * @return
     */
    public static IDStringDialog fabricVariableName(final AbstractBlockInterface parent, final String newName) {
        
        IDStringDialog returnValue = null;        
        if (_allIDStrings.containsKey(newName) && !_allIDStrings.get(newName).equals(parent)) {
            returnValue = new IDStringDialog(parent, findUnusedName(newName));            
        } else {
            returnValue = new IDStringDialog(parent, newName);
        }

        final ArrayList<AbstractBlockInterface> newList = new ArrayList<AbstractBlockInterface>();
        newList.add(parent);
        _allIDStrings.put(returnValue.toString(), newList);
        return returnValue;
    }

    public void setNameUnChecked(final String newName) {
        if (newName.equals(_idString)) {
            return;
        }
        _idString = newName;
        notifyListeners();
    }

    private void notifyListeners() {
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(null);
        }
    }

    public void setNewNameCheckedUndoable(final String newName) throws NameAlreadyExistsException {
        final String oldName = _idString;
        if (oldName.equals(newName)) {
            return;
        }
        setNewNameChecked(newName);

        AbstractUndoGenericModel.undoManager.addEdit(new UndoableEdit() {
            @Override
            public void undo() throws CannotUndoException {
                deleteIDString();
                try {
                    setNewNameChecked(oldName);
                } catch (NameAlreadyExistsException ex) {
                    Logger.getLogger(IDStringDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
                notifyListeners();
            }

            @Override
            public boolean canUndo() {
                return true;
            }

            @Override
            public void redo() throws CannotRedoException {
                deleteIDString();
                try {
                    setNewNameChecked(newName);
                } catch (NameAlreadyExistsException ex) {
                    Logger.getLogger(IDStringDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
                notifyListeners();
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
                return "Component rename";
            }

            @Override
            public String getUndoPresentationName() {
                return "Rename from " + oldName + " to " + newName;
            }

            @Override
            public String getRedoPresentationName() {
                return "Rename from " + newName + " to " + oldName;
            }
        });

    }

    public void setNewNameChecked(final String newName) throws NameAlreadyExistsException {        
            if (newName.equals(_idString)) {
                return;
            }
            ArrayList<AbstractBlockInterface> insertList = null;
            if (_allIDStrings.containsKey(newName)) {
                ArrayList<AbstractBlockInterface> possibleConflicts = _allIDStrings.get(newName);
                insertList = possibleConflicts;
                for (AbstractBlockInterface candidate : possibleConflicts) {
                    if (candidate.getParentCircuitSheet() == null && _parent.getParentCircuitSheet() == null
                            && candidate.getParentSheetIdentifier() != 0 && _parent.getParentSheetIdentifier() != 0
                            && candidate.getParentSheetIdentifier() == _parent.getParentSheetIdentifier()) {
                        throw new NameAlreadyExistsException("The name: \"" + newName + "\" is already in use in the circuit sheet!");
                    }

                    if (candidate.getParentCircuitSheet() != null && _parent.getParentCircuitSheet() != null) {
                        if (candidate.getParentCircuitSheet().equals(_parent.getParentCircuitSheet())) {
                            throw new NameAlreadyExistsException("The name: \"" + newName + "\" is already in use in the circuit sheet!");
                        }
                    }
                }
            } else {
                insertList = new ArrayList<AbstractBlockInterface>();
            }
            deleteIDString(); // remove the old name reference            
            _idString = newName;
            notifyListeners();

            insertList.add(_parent);
            if (!_allIDStrings.containsKey(newName)) {
                _allIDStrings.put(newName, insertList);
            }        
    }

    public void deleteIDString() {
        if (_allIDStrings.containsKey(this.toString())) {
            List<AbstractBlockInterface> possibleComponents = _allIDStrings.get(this.toString());
            for (AbstractBlockInterface deleteSearch : possibleComponents.toArray(new AbstractBlockInterface[0])) {
                if (_parent == deleteSearch) {
                    possibleComponents.remove(_parent);
                }
            }
            if (possibleComponents.size() == 0) {
                _allIDStrings.remove(this.toString());
            }
        }
    }

    @Override
    public String toString() {
        return _idString;
    }

    public static boolean isNameAlreadyUsed(final String testName) {
        return _allIDStrings.containsKey(testName);
    }

    public void refreshNameList() {

        ArrayList<AbstractBlockInterface> insertList = new ArrayList<AbstractBlockInterface>();
        if (!_allIDStrings.containsKey(_idString)) {
            _allIDStrings.put(_idString, insertList);
        } else {
            insertList = _allIDStrings.get(_idString);
        }

        if (!insertList.contains(_parent)) {
            insertList.add(_parent);
        }

    }

    public static String findUnusedName(final String newName) {
        final int numericIndex = getNumericEndIndex(newName);

        if (numericIndex < 0) {
            return newName + "1";
        }


        final String nameWithoutNumericEnding = newName.substring(0, numericIndex);

        for (int i = 1; i < MAX_SEARCH_NUMBER; i++) {
            final String testName = nameWithoutNumericEnding + i;
            if (!_allIDStrings.containsKey(testName)) {
                return testName;
            }
        }

        assert false;
        return "invalid name!";
    }

    private static int getNumericEndIndex(final String newName) {
        int digitIndex = -1;

        for (int i = newName.length() - 1; i >= 0; i--) {
            if (Character.isDigit(newName.charAt(i))) {
                digitIndex = i;
                continue;
            } else {
                break;
            }
        }
        return digitIndex;
    }

    public void setRandomStringID() {
        try {
            setNewNameChecked(this + "_" + rand.nextInt());
        } catch (NameAlreadyExistsException ex) {
            try {
                setNewNameChecked(this + "_" + rand.nextInt());
            } catch (NameAlreadyExistsException ex1) {
                Logger.getLogger(AbstractBlockInterface.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    public static AbstractBlockInterface getComponentByName(final String searchName) {

        if (_allIDStrings.containsKey(searchName)) {
            List<AbstractBlockInterface> possibleComponents = _allIDStrings.get(searchName);
            if (possibleComponents.size() == 1) {
                return possibleComponents.get(0);
            } else if (possibleComponents.isEmpty()) {
                throw new NoSuchElementException("A component with the name " + searchName + " could not be found!");
            } else if (possibleComponents.size() > 1) {
                throw new RuntimeException("Name ambiguousness error! The model contains several components with the name: " + searchName + ".\n"
                        + "Hint: you can select a specific component by indexing with its subcircuit sheet names.\n"
                        + "For instance: SUBCIRCUIT.1#R.1 returns the component R.1 from SUBCIRCUIT.1.");
            }
        }

        CircuitSheet parent = SchematischeEingabe2.Singleton._circuitSheet.findSubCircuit(searchName);
        if (parent == null) {
            throw new RuntimeException("Could not find subcircuit component with name: " + searchName);
        }

        String truncatedElementName = searchName.substring(searchName.lastIndexOf('#') + 1, searchName.length());

        for (AbstractBlockInterface toTest : parent.allElements.getClassFromContainer(AbstractBlockInterface.class)) {
            if (toTest.getStringID().equals(truncatedElementName)) {
                return toTest;
            }
        }

        throw new RuntimeException("Could not find component with name: " + searchName);
    }

    public void addActionListener(final ActionListener actionListener) {
        actionListeners.add(actionListener);
    }
}
