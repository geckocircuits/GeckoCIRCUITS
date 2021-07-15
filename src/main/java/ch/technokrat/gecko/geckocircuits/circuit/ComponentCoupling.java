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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.control.Operationable;
import ch.technokrat.gecko.geckocircuits.control.ReglerGate;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

public final class ComponentCoupling {

    public final AbstractBlockInterface[] _coupledElements;
    public final long[] _coupledIdentifiers;
    private long[] _coupledIdentifiersBeforeCopy;
    final AbstractBlockInterface _parentElement;
    final int[] _parStringIndices;
    private int _internalStringIndex = -1;
    private String _internalString = "";

    public ComponentCoupling(final int maxNumberCouplings, AbstractBlockInterface parent, int[] parStringIndices) {
        _coupledElements = new AbstractBlockInterface[maxNumberCouplings];
        _coupledIdentifiers = new long[maxNumberCouplings];
        _parentElement = parent;
        _parStringIndices = new int[parStringIndices.length];
        System.arraycopy(parStringIndices, 0, _parStringIndices, 0, parStringIndices.length);
    }
    
    public void setNewCouplingElement(final int index, final AbstractBlockInterface element) {        
        _internalStringIndex = -1;
        _internalString = "";
        if (element == null) {
            _coupledIdentifiers[index] = 0;
            if (_coupledElements[index] != null) {
                _coupledElements[index]._isReferencedBy.remove(this);
            }
            _coupledElements[index] = null;
        } else {                                    
            _coupledIdentifiers[index] = element.getUniqueObjectIdentifier();
            if (_coupledElements[index] != null) {                                
                _coupledElements[index]._isReferencedBy.remove(this);
            }

            _coupledElements[index] = element;
            _coupledElements[index]._isReferencedBy.add(this);
        }

        updateCouplingParameterStrings();
    }

    public void setNewCouplingElement(int index, AbstractBlockInterface element, String is) {        
        if (element == null) {
            _coupledIdentifiers[index] = 0;
            if (_coupledElements[index] != null) {
                _coupledElements[index]._isReferencedBy.remove(this);
            }
            _coupledElements[index] = null;
        } else {                        
            _coupledIdentifiers[index] = element.getUniqueObjectIdentifier();

            if (_coupledElements[index] != null) {
                _coupledElements[index]._isReferencedBy.remove(this);
            }

            _coupledElements[index] = element;
            _coupledElements[index]._isReferencedBy.add(this);
        }

        
        
        
        
        updateCouplingParameterStrings();
        if (!(element instanceof AbstractCircuitBlockInterface)) {
            return;
        }

        List<String> internalStrings = ((AbstractCircuitBlockInterface) element).getParameterStringIntern();
        if (internalStrings == null) {
            return;
        }

        for (int searchIndex = 0; searchIndex < internalStrings.size(); searchIndex++) {
            if (is.equals(internalStrings.get(searchIndex))) {
                _internalStringIndex = searchIndex;
                _internalString = is;
            }
        }

    }

    public void setNewCouplingElementUndoable(final int index, final AbstractBlockInterface element) {
        if (element == _coupledElements[index]) {
            return;
        }
        
        if(element != null) {
            removeWithSingleReference(element, index); // this is for removing Gate connections that
            // already exist. Could be done in a nicer way, e.g. creating a sub-class of ComponentCouling...!
        }
        
        final CouplingUndoableEdit edit = new CouplingUndoableEdit(_coupledElements[index], element, index, true);
        AbstractUndoGenericModel.undoManager.addEdit(edit);
        setNewCouplingElement(index, element);
        
    }

    public void setNewCouplingElementInvisibleUndoable(final int index, final AbstractBlockInterface element) {
        if (element == _coupledElements[index]) {
            return;
        }                

        final CouplingUndoableEdit edit = new CouplingUndoableEdit(_coupledElements[index], element, index, false);
        AbstractUndoGenericModel.undoManager.addEdit(edit);
        setNewCouplingElement(index, element);
    }

    

    public AbstractBlockInterface getParent() {
        return _parentElement;
    }

    public void updateCouplingParameterStrings() {        
        for (int i = 0; i < _coupledElements.length; i++) {
            if (_coupledElements[i] == null) {
                _parentElement.getParameterString()[_parStringIndices[i]] = "";
            } else {
                String setString = _coupledElements[i].getStringID();

                if (_internalStringIndex >= 0) {
                    setString += _internalString;
                }                
                _parentElement.getParameterString()[_parStringIndices[i]] = setString;
            }
        }

    }

    public List<Long> getCouplingIdentifiers() {
        List<Long> returnValue = new ArrayList<Long>();
        for (long value : _coupledIdentifiers) {
            returnValue.add(value);
        }
        return returnValue;
    }

    public void importASCII(final TokenMap tokenMap) {        
                
                        
        if (tokenMap.containsToken("coupledReferenceID[]")) {
            long[] coupledIdentifiers = tokenMap.readDataLine("coupledReferenceID[]", _coupledIdentifiers);                                                
            System.arraycopy(coupledIdentifiers, 0, _coupledIdentifiers, 0, coupledIdentifiers.length);            
        }

        if (tokenMap.containsToken("copyCoupledReferenceID[]")) {
            _coupledIdentifiersBeforeCopy = tokenMap.readDataLine("copyCoupledReferenceID[]", new long[0]);
        }

        if (tokenMap.containsToken("internalIndex")) {
            _internalStringIndex = tokenMap.readDataLine("internalIndex", -1);
            _internalString = tokenMap.readDataLine("internalString", "");
        }

    }

    public void exportASCII(StringBuffer ascii) {        
        DatenSpeicher.appendAsString(ascii.append("\ncoupledReferenceID"), _coupledIdentifiers);
        /**
         * careful: this is used fore restoring connections, when copy export ->
         * import is used!
         */
        DatenSpeicher.appendAsString(ascii.append("\ncopyCoupledReferenceID"), _coupledIdentifiers);
        DatenSpeicher.appendAsString(ascii.append("\ninternalIndex"), _internalStringIndex);
        DatenSpeicher.appendAsString(ascii.append("\ninternalString"), _internalString);
    }

    public void refreshCoupledReferences(final List<? extends AbstractCircuitSheetComponent> allSheetElements) {
                
        
        String[] parameterString = _parentElement.getParameterString();
        List<AbstractBlockInterface> allElements = new ArrayList<AbstractBlockInterface>();
        for(AbstractCircuitSheetComponent comp : allSheetElements) {
            if(comp instanceof AbstractBlockInterface) {
                allElements.add((AbstractBlockInterface) comp);
            }
        }                
        for (AbstractCircuitSheetComponent elem : allSheetElements) {
            if (elem instanceof HiddenSubCircuitable) {
                for (AbstractBlockInterface sub : ((HiddenSubCircuitable) elem).getHiddenSubCircuitElements()) {
                    allElements.add(sub);
                }
            }
        }
                
        
        for (int i = 0; i < _coupledElements.length; i++) {
            int parStringIndex = _parStringIndices[i];            
            if (parameterString[parStringIndex].isEmpty() && _coupledElements[i] == null) {                             
                return;
            }
            
            if ((!parameterString[parStringIndex].isEmpty() && _coupledElements[i] == null)
                    || _coupledElements[i].getUniqueObjectIdentifier() != _coupledIdentifiers[i]) {                                
                for (AbstractBlockInterface elem : allElements) {
                    boolean internStringAdded = false;
                    //System.out.println("searching: " + elem.getStringID() + " " + elem.getUniqueObjectIdentifier());
                    if (elem instanceof AbstractCircuitBlockInterface) {
                        AbstractCircuitBlockInterface lkblock = (AbstractCircuitBlockInterface) elem;
                        List<String> internStrings = lkblock.getParameterStringIntern();
                        if (internStrings != null && !internStrings.isEmpty()) {                                                        
                            
                            internStringAdded = true;
                            for (int index = 0; index < internStrings.size(); index++) {
                                final String internString = parameterString[parStringIndex];
                                if (parameterString[parStringIndex].startsWith(elem.getStringID())
                                        && parameterString[parStringIndex].endsWith(internString)) {
                                    _internalString = internString;
                                    _internalStringIndex = index;
                                    _coupledIdentifiers[i] = elem.getUniqueObjectIdentifier();
                                    if (_coupledElements[i] != null && _coupledElements[i] != elem) {
                                        _coupledElements[i]._isReferencedBy.remove(this);
                                    }
                                    _coupledElements[i] = elem;                                    
                                    elem._isReferencedBy.add(this);                                    
                                }
                            }
                        }
                    } 
                    if (!internStringAdded && elem.getUniqueObjectIdentifier() == _coupledIdentifiers[i]
                            || _coupledIdentifiers[i] == 0 && elem.getStringID().equals(parameterString[parStringIndex])) {
                        _coupledIdentifiers[i] = elem.getUniqueObjectIdentifier();
                        if (_coupledElements[i] != null) {
                            _coupledElements[i]._isReferencedBy.remove(this);
                        }                        
                        _coupledElements[i] = elem;
                        elem._isReferencedBy.add(this);
                    }
                }

            }

            if (_coupledElements[i] != null) {
                assert _coupledIdentifiers[i] == _coupledElements[i].getUniqueObjectIdentifier();
            }
        }
        
        updateCouplingParameterStrings();
        
    }

    /**
     * try to find the new coupling partner, when a group of elements was
     * copied.
     *
     * @param origElement
     * @param allNewElements
     * @param vecIndexExchangeAllNew
     * @param originElement
     */
    public void trySetCopyReference(final List<AbstractCircuitSheetComponent> allNewElements) {   
                
        for (int i = 0; i < _coupledElements.length; i++) {
            long searchCouplingIdentifier = _coupledIdentifiers[i];            
            setNewCouplingElement(i, null, _internalString);
            for (AbstractCircuitSheetComponent elem : allNewElements) {
                if (elem instanceof AbstractBlockInterface) {
                    long elemIdentifier = elem.getIdentifier().getIdentifier();                    
                    if (elemIdentifier != 0 && elemIdentifier == searchCouplingIdentifier) {                                                
                        this.setNewCouplingElement(i, (AbstractBlockInterface) elem, _internalString);
                    }
                }
            }
        }
    }

    public void elementDeleted(AbstractBlockInterface deletedElement) {
        for (int i = 0; i < _coupledElements.length; i++) {
            if (deletedElement.equals(_coupledElements[i])) {
                setNewCouplingElementInvisibleUndoable(i, null);
            }
        }
    }

    void setOldCopyIdentifiers(final ComponentCoupling oldCoupling) {
        long[] coupledIdentifiers = oldCoupling._coupledIdentifiers;
        this._internalString = oldCoupling._internalString;
        this._internalStringIndex = oldCoupling._internalStringIndex;
        _coupledIdentifiersBeforeCopy = new long[coupledIdentifiers.length];
        System.arraycopy(coupledIdentifiers, 0, _coupledIdentifiersBeforeCopy, 0, coupledIdentifiers.length);

    }

    void shiftCopyCouplingIDsFrom(final ComponentCoupling oldCoupling, final long shiftValue) {
        for(int i = 0; i < oldCoupling._coupledIdentifiers.length; i++) {            
            _coupledIdentifiers[i] = oldCoupling._coupledIdentifiers[i] + shiftValue;            
        }        
    }

    void refreshCouplingReferences() {
        for(int i = 0; i < _coupledElements.length; i++) {
            setNewCouplingElement(i, _coupledElements[i]);
        }
    }

    private void removeWithSingleReference(final AbstractBlockInterface partner, final int index) {                
        for(ComponentCoupling otherCoup : partner._isReferencedBy.toArray(new ComponentCoupling[partner._isReferencedBy.size()])) {                        
            if(otherCoup._parentElement instanceof ReglerGate 
                    && _parentElement instanceof ReglerGate) {  
                otherCoup.setNewCouplingElementUndoable(index, null);
            }            
        }
    }
    
    class SetOperation extends Operationable.OperationInterface {
        final int _index;
        SetOperation(final int index) {            
            super(((index == 0) ? "setComponentCoupling" : "setSecondComponentCoupling"), I18nKeys.SET_COMPONENT_COUPLING);
            _index = index;
        }
        @Override
            public Object doOperation(final Object parameterValue) {
                if (!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Parameter type must be a String!");
                }
                
                final AbstractBlockInterface toCouple = IDStringDialog.getComponentByName((String) parameterValue);
                
                ComponentCoupable coupable = (ComponentCoupable) _parentElement;                
                    
                    List<AbstractBlockInterface> insertList = new ArrayList<AbstractBlockInterface>();
                    coupable.checkComponentCompatibility(toCouple, insertList);


                    if (insertList.contains(toCouple)) {
                        setNewCouplingElementUndoable(_index, toCouple);
                        return null;
                    }


                    if (toCouple instanceof HiddenSubCircuitable) {
                        Collection<? extends AbstractBlockInterface> subs = ((HiddenSubCircuitable) toCouple).getHiddenSubCircuitElements();
                        for (AbstractBlockInterface sub : subs) {
                            if (insertList.contains(sub)) {
                                setNewCouplingElementUndoable(_index, sub);
                                return null;
                            }
                        }
                    }

                    throw new RuntimeException("Component " + _parentElement.getStringID()
                            + " cannot be coupled to component " + parameterValue);
                                                
            }
        
    }    

    public List<Operationable.OperationInterface> getOperationInterfaces() {
        List<Operationable.OperationInterface> returnValue = new ArrayList<Operationable.OperationInterface>();
        
        Operationable.OperationInterface op0 = new SetOperation(0);
        returnValue.add(op0);
        
        if(_coupledElements.length == 2) {
            Operationable.OperationInterface op1 = new SetOperation(1);
            returnValue.add(op1);
        }        
        
       return returnValue;
    }

    private class CouplingUndoableEdit implements UndoableEdit {

        final boolean _isSignificant;
        private final int _index;
        private final AbstractBlockInterface _oldReference;
        private final AbstractBlockInterface _newReference;

        private CouplingUndoableEdit(final AbstractBlockInterface oldReference, final AbstractBlockInterface newReference, final int index,
                final boolean isSignificant) {            
            _index = index;
            _oldReference = oldReference;
            _newReference = newReference;
            _isSignificant = isSignificant;
        }

        @Override
        public void undo() throws CannotUndoException {            
            setNewCouplingElement(_index, _oldReference);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() throws CannotRedoException {
            setNewCouplingElement(_index, _newReference);
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
            return "component reference";
        }

        @Override
        public String getUndoPresentationName() {
            String oldRefString = "null";
            String newRefString = "null";
            if (_oldReference != null) {
                oldRefString = _oldReference.getStringID();
            }
            if (_newReference != null) {
                newRefString = _newReference.getStringID();
            }
            return "Component reference from \"" + oldRefString + "\" to \"" + newRefString + "\"";
        }

        @Override
        public String getRedoPresentationName() {
            String oldRefString = "null";
            String newRefString = "null";
            if (_oldReference != null) {
                oldRefString = _oldReference.getStringID();
            }
            if (_newReference != null) {
                newRefString = _newReference.getStringID();
            }
            return "Component reference from \"" + newRefString + "\" to \"" + oldRefString + "\"";
        }
    }
    
    void shiftReferenceIDs(final long shiftValue) {
        for(int i = 0; i < _coupledIdentifiers.length; i++) {
            if(_coupledIdentifiers[i] != 0) {                                
                _coupledIdentifiers[i] += shiftValue;
            }            
        }        
    }
}
