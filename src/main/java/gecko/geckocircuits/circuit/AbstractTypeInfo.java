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

package gecko.geckocircuits.circuit;
import gecko.core.circuit.TokenMap;

import gecko.geckocircuits.general.AbstractComponentType;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.i18n.resources.I18nKeys;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Type information with GUI/I18n support.
 * Extends TypeInfoCore to add internationalization and description fields.
 * Maintains registry of all component types.
 */
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
        justification = "Abstract class - subclasses are final or properly designed to handle constructor exceptions")
public abstract class AbstractTypeInfo extends TypeInfoCore {

    static Map<Class<? extends AbstractBlockInterface>, AbstractComponentType> _classEnumMap = new HashMap<Class<? extends AbstractBlockInterface>, AbstractComponentType>();
    static Map<Class<? extends AbstractBlockInterface>, AbstractTypeInfo> _classTypeMap = new HashMap<Class<? extends AbstractBlockInterface>, AbstractTypeInfo>();
    static Map<String, AbstractTypeInfo> _stringTypeMap = new HashMap<String, AbstractTypeInfo>();
    static Map<AbstractComponentType, AbstractTypeInfo> _enumTypeMap = new HashMap<AbstractComponentType, AbstractTypeInfo>();

    @SuppressFBWarnings(value = "MS_MUTABLE_COLLECTION_PKGPROTECT",
            justification = "_exportImportEnumMap is intentionally public for component type registration across packages")
    public static final Map<String, AbstractComponentType> _exportImportEnumMap = new HashMap<String, AbstractComponentType>();

    static Set<Class<? extends AbstractBlockInterface>> _uniqueClassSet = new HashSet<Class<? extends AbstractBlockInterface>>();
    static Set<String> _uniqueTestSet = new HashSet<String>() {
        @Override
        public boolean add(final String insertTest) {
            assert !this.contains(insertTest) : " Error: ID String is used multiple times: " + insertTest;
            return super.add(insertTest);
        }
    };
    static Set<AbstractComponentType> _allRegisteredComponentEnums = new HashSet<AbstractComponentType>();
    static Set<AbstractTypeInfo> _allRegisteredTypeInfos = new HashSet<AbstractTypeInfo>();

    static AbstractTypeInfo getTypeFromEnum(final AbstractComponentType _typElement) {
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

    public AbstractTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription, final I18nKeys typeDescriptionVerbose) {
        super(typeClass, idString);
        _typeDescription = typeDescription;
        _typeDescriptionVerbose = typeDescriptionVerbose;
        _classTypeMap.put(_typeClass, this);
        _stringTypeMap.put(idString, this);
        _allRegisteredTypeInfos.add(this);
        doConsistencyCheck();
    }

    public AbstractTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription) {
        this(typeClass, idString, typeDescription, typeDescription);
    }

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

    public static AbstractComponentType getTypeEnumFromClass(Class<? extends AbstractBlockInterface> aClass) {
        return _classEnumMap.get(aClass);
    }

    public void addParentEnum(final AbstractComponentType parentType) {
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

    @Override
    public abstract ConnectorType getSimulationDomain();

    @Override
    public abstract AbstractBlockInterface fabric();

    @Override
    public abstract String getExportImportCharacters();
    @Override
    public abstract String getSaveIdentifier();

    /**
     * Factory method to create components from file with deserialization
     */
    public static final AbstractBlockInterface fabricFromFile(final AbstractComponentType typ, TokenMap tokenMap) {
        final AbstractBlockInterface returnValue = typ.getTypeInfo().fabric();
        returnValue.importASCII(tokenMap);
        return returnValue;
    }

    /**
     * Factory method to create new component instance and initialize
     */
    public static final AbstractBlockInterface fabricNew(final AbstractTypeInfo typ) {
        final AbstractBlockInterface returnValue = typ.fabric();
        returnValue.setParentCircuitSheet(SchematicEditor2.Singleton._visibleCircuitSheet);
        returnValue.doOperationAfterNewConstruction();
        return returnValue;
    }

    /**
     * Factory method for hidden sub-components
     */
    public static AbstractCircuitBlockInterface fabricHiddenSub(final AbstractComponentType typ,
            final AbstractCircuitSheetComponent parent) {
        final AbstractCircuitBlockInterface returnValue = (AbstractCircuitBlockInterface) typ.getTypeInfo().fabric();
        returnValue.setParent(parent);
        return returnValue;
    }
}

