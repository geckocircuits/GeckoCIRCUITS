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

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.control.ControlTyp;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractTypeInfo {        
    
    static Map<Class<? extends AbstractBlockInterface>, AbstractComponentTyp> _classEnumMap = new HashMap<Class<? extends AbstractBlockInterface>, AbstractComponentTyp>();
    static Map<Class<? extends AbstractBlockInterface>, AbstractTypeInfo> _classTypeMap = new HashMap<Class<? extends AbstractBlockInterface>, AbstractTypeInfo>();
    static Map<String, AbstractTypeInfo> _stringTypeMap = new HashMap<String, AbstractTypeInfo>();
    static Map<AbstractComponentTyp, AbstractTypeInfo> _enumTypeMap = new HashMap<AbstractComponentTyp, AbstractTypeInfo>();    
    
    public static final Map<String, AbstractComponentTyp> _exportImportEnumMap = new HashMap<String, AbstractComponentTyp>();
    
    static Set<Class<? extends AbstractBlockInterface>> _uniqueClassSet = new HashSet<Class<? extends AbstractBlockInterface>>();
    static Set<String> _uniqueTestSet = new HashSet<String>() {
        @Override
        public boolean add(final String insertTest) {
            assert !this.contains(insertTest) : " Error: ID String is used multiple times: " + insertTest;
            return super.add(insertTest); //To change body of generated methods, choose Tools | Templates.
        }
    };
    static Set<AbstractComponentTyp> _allRegisteredComponentEnums = new HashSet<AbstractComponentTyp>();
    static Set<AbstractTypeInfo> _allRegisteredTypeInfos = new HashSet<AbstractTypeInfo>();            

    static AbstractTypeInfo getTypeFromEnum(final AbstractComponentTyp _typElement) {
        return _enumTypeMap.get(_typElement);
    }

    public static AbstractTypeInfo getFromComponentName(String elementType) {
        if(_stringTypeMap.containsKey(elementType)) {
            return _stringTypeMap.get(elementType);
        } else {
            throw new RuntimeException("Error: a component with type \"" + elementType + "\" does not exist!");
        }
        
    }
    
    public final I18nKeys _typeDescription;
    public final I18nKeys _typeDescriptionVerbose;    
    public final String _fixedIDString;
    public final Class<? extends AbstractBlockInterface> _typeClass;
    public AbstractComponentTyp _parentType;

    public AbstractTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription, final I18nKeys typeDescriptionVerbose) {
        _typeDescription = typeDescription;
        _typeDescriptionVerbose = typeDescriptionVerbose;
        _fixedIDString = idString;
        _typeClass = typeClass;
        _classTypeMap.put(_typeClass, this);
        _stringTypeMap.put(idString, this);                
        _allRegisteredTypeInfos.add(this);
        doConsistencyCheck();
    }

    public AbstractTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription) {
        this(typeClass, idString, typeDescription, typeDescription);
    }
    
    public abstract ConnectorType getSimulationDomain();
    
    public void doConsistencyCheck() {                        
        assert !_fixedIDString.isEmpty();                
        assert !_uniqueTestSet.contains(_fixedIDString) : "Error: ID string is used twice! " + _fixedIDString;
        _uniqueTestSet.add(_fixedIDString);        
        assert !_uniqueClassSet.contains(_typeClass) : "Error: the class is already registered!";
        _uniqueClassSet.add(_typeClass);
    }
    
    public static AbstractTypeInfo getTypeInfoFromClass(Class<? extends AbstractBlockInterface> aClass) {
        return _classTypeMap.get(aClass);
    }
    
    public static AbstractComponentTyp getTypeEnumFromClass(Class<? extends AbstractBlockInterface> aClass) {
        return _classEnumMap.get(aClass);
    }
            
    public void addParentEnum(final AbstractComponentTyp parentType) {
        assert !_allRegisteredComponentEnums.contains(parentType);                
        _allRegisteredComponentEnums.add(parentType);
        _parentType = parentType;
        _classEnumMap.put(_typeClass, parentType);
        _enumTypeMap.put(parentType, this);
        if(!_exportImportEnumMap.containsKey(this.getExportImportCharacters())) {
            _exportImportEnumMap.put(this.getExportImportCharacters(), parentType);        
        }         

    }
    

    public static AbstractTypeInfo getTypeFromString(final String elementType) {
        if(_stringTypeMap.containsKey(elementType)) {
            return _stringTypeMap.get(elementType);
        } else {
            throw new IllegalArgumentException("String type " + elementType + " could not be found!");
        }
    }
        
    public abstract AbstractBlockInterface fabric();
    
    public abstract String getExportImportCharacters();
    public abstract String getSaveIdentifier(); 
    
    public static final AbstractBlockInterface fabricFromFile(final AbstractComponentTyp typ, TokenMap tokenMap) {        
        final AbstractBlockInterface returnValue = typ.getTypeInfo().fabric();
        returnValue.importASCII(tokenMap);        
        return returnValue;
    }
    
    public static final AbstractBlockInterface fabricNew(final AbstractTypeInfo typ) {        
        final AbstractBlockInterface returnValue = typ.fabric();
        returnValue.setParentCircuitSheet(SchematischeEingabe2.Singleton._visibleCircuitSheet);
        returnValue.doOperationAfterNewConstruction();
        return returnValue;
    }
    
    public static AbstractCircuitBlockInterface fabricHiddenSub(final AbstractComponentTyp typ,
            final AbstractCircuitSheetComponent parent) {
        final AbstractCircuitBlockInterface returnValue = (AbstractCircuitBlockInterface) typ.getTypeInfo().fabric();
        returnValue.setParent(parent);
        return returnValue;
    }
    
}
