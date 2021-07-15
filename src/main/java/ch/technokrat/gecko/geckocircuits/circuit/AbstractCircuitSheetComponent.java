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
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import ch.technokrat.modelviewcontrol.ModelMVC;

public abstract class AbstractCircuitSheetComponent {

    public static int dpix;  // Abstand 2er Rasterpunkte in Pixelpunkten                
    public final static ModelMVC<Integer> dpixValue;
    private ComponentState _modus = ComponentState.FINISHED;

    static {
        dpixValue = new ModelMVC<Integer>(10, "circuit scaling size");
        dpixValue.addModelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dpix = dpixValue.getValue();
                try {
                    SchematischeEingabe2.Singleton.setNewScaling(dpix);
                    SchematischeEingabe2.Singleton._visibleCircuitSheet.revalidate();
                    SchematischeEingabe2.Singleton._visibleCircuitSheet.repaint();
                } catch (NullPointerException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        });
    }
    public final ModelMVC<Enabled> _isEnabled = new ModelMVC<Enabled>(Enabled.ENABLED) {
        @Override
        public String toString() {
            return AbstractCircuitSheetComponent.this.toString();
        }
    };
    protected CircuitSheet _parentCircuitSheet;
    private final UniqueObjectIdentifer _identifier = new UniqueObjectIdentifer();
    /**
     * temp because: at model loading time, the parent sheets are not yet
     * created. This field is used to find the reference after the model is
     * loaded completely. Later, this field should never ever be used, since the
     * componentes could be moved to another subsheet.
     *
     */
    private long _tempParentSheetIdentifier = 0;
    boolean _isHiddenSubCircuitComponent = false;
    private AbstractCircuitSheetComponent _parent;

    public abstract void absetzenElement();

    public abstract void setPositionWithUndo();

    public abstract void deselectViaESCAPE();

    public abstract void moveComponent(final Point moveToPoint);

    public abstract void paintGeckoComponent(final Graphics2D graphics);

    abstract public void paintComponentForeGround(final Graphics2D graphics);    
    
    
    
    /**
     *
     * @param shiftIdentifier used to implement a simple group-copy operation.
     * All Identifiers (component references, component id's and so on) are
     * shifted by this value.
     * @return
     */
    public abstract AbstractCircuitSheetComponent copyFabric(final long shiftIdentifier);

    abstract public int[] getAussenabmessungenRechteckEckpunkte();

    public AbstractCircuitSheetComponent() {
        _identifier.createNewIdentifier();
    }

    public void deleteComponent() {        
        if (this instanceof GeckoFileable) {
            List<GeckoFile> filesList = ((GeckoFileable)this).getFiles();
            if (filesList != null) {
                final ArrayList<GeckoFile> extraFiles = new ArrayList<GeckoFile>(filesList);
                if (extraFiles != null && !extraFiles.isEmpty()) {
                    ((GeckoFileable) this).removeLocalComponentFiles(extraFiles);
                }
            }
        }       

        deleteActionIndividual();
                
        //assert _parentCircuitSheet.allElements.contains(this);       
        if (_parentCircuitSheet != null) {
            _parentCircuitSheet.allElements.remove(this);
        }

    }

    protected void deleteActionIndividual() {
        // override this method, if something has to be done BEFORE the component is deleted!
    }

    /**
     * @return the modus
     */
    public ComponentState getModus() {
        return _modus;
    }

    /**
     * @param modus the modus to set
     */
    public void setModus(final ComponentState modus) {
        this._modus = modus;
    }

    /**
     * @return the isEnabled
     */
    public Enabled isCircuitEnabled() {
        return _isEnabled.getValue();
    }

    /**
     * @param isEnabled the isEnabled to set
     */
    public void setCircuitEnabled(final Enabled isEnabled) {
        _isEnabled.setValue(isEnabled);
    }

    void importASCII(final TokenMap tokenMap) {
        _identifier.importASCII(tokenMap);
        boolean oldEnabled = true;

        if (tokenMap.containsToken("parentSheetIdentifier")) {
            _tempParentSheetIdentifier = tokenMap.readDataLine("parentSheetIdentifier", (long) 0);
        }

        if (tokenMap.containsToken("enabled")) {
            oldEnabled = tokenMap.readDataLine("enabled", oldEnabled);
            if (oldEnabled) {
                _isEnabled.setValueWithoutUndo(Enabled.ENABLED);
            } else {
                _isEnabled.setValueWithoutUndo(Enabled.DISABLED);
            }
        }

        if (tokenMap.containsToken("enabledShorted")) {
            _isEnabled.setValueWithoutUndo(Enabled.getFromOrdinal(tokenMap.readDataLine("enabledShorted", 1)));
        }
    }

    public long getParentSheetIdentifier() {
        return _tempParentSheetIdentifier;
    }

    public void exportASCII(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\nenabledShorted"), _isEnabled.getValue().ordinal());

        if (getParentCircuitSheet() instanceof SubCircuitSheet) {
            DatenSpeicher.appendAsString(ascii.append("\nparentSheetIdentifier"),
                    ((SubCircuitSheet) _parentCircuitSheet)._subBlock.getUniqueObjectIdentifier());
        } else {
            DatenSpeicher.appendAsString(ascii.append("\nparentSheetIdentifier"), (long) 0);
        }

    }

    public CircuitSheet getParentCircuitSheet() {
        if (_parent == null) {
            return _parentCircuitSheet;
        } else {
            return _parent.getParentCircuitSheet();
        }

    }

    public abstract ConnectorType getSimulationDomain();

    /**
     * @param parentCircuitSheet the _parentCircuitSheet to set
     */    
    public void setParentCircuitSheet(final CircuitSheet parentCircuitSheet) {        
        if (_parentCircuitSheet != null && _parentCircuitSheet.allElements.contains(this)) {
            _parentCircuitSheet.allElements.remove(this);
        }
        this._parentCircuitSheet = parentCircuitSheet;
        _parentCircuitSheet.allElements.add(this);                                        
    }
    

    /**
     *
     * @param allSubs the subcircuit blocks from the NEW import (and only them!
     * @param rootSubcircuitName if "null" or "emptyString": root of the import
     * is the main circuit sheet. Else, find the subcircuit block and set this
     * as root for the import.
     */
    public void findAndSetReferenceToParentSheet(final List<SubcircuitBlock> allSubs, final String rootSubcircuitName) {

        for (SubcircuitBlock subBlock : allSubs) {
            if (subBlock.getUniqueObjectIdentifier() == _tempParentSheetIdentifier) {
                setParentCircuitSheet(subBlock._myCircuitSheet);
                return;
            }
        }
        _tempParentSheetIdentifier = 0;


        if (rootSubcircuitName != null && !rootSubcircuitName.isEmpty()) {
            SubcircuitBlock rootBlock = (SubcircuitBlock) IDStringDialog.getComponentByName(rootSubcircuitName);
            setParentCircuitSheet(rootBlock._myCircuitSheet);
        } else {            
            setParentCircuitSheet(SchematischeEingabe2.Singleton._circuitSheet);
        }


    }
    
    /**
     *
     * @param allSubs the subcircuit blocks from the NEW import (and only them!
     * @param rootSubcircuitName if "null" or "emptyString": root of the import
     * is the main circuit sheet. Else, find the subcircuit block and set this
     * as root for the import.
     */
    public void findAndSetReferenceToParentSheet2(final List<SubcircuitBlock> allSubs, final String rootSubcircuitName) {

        for (SubcircuitBlock subBlock : allSubs) {
            if (subBlock.getUniqueObjectIdentifier() == _tempParentSheetIdentifier) {
                setParentCircuitSheet(subBlock._myCircuitSheet);
                return;
            }
        }
        _tempParentSheetIdentifier = 0;


        if (rootSubcircuitName != null && !rootSubcircuitName.isEmpty()) {
            SubcircuitBlock rootBlock = (SubcircuitBlock) IDStringDialog.getComponentByName(rootSubcircuitName);
            setParentCircuitSheet(rootBlock._myCircuitSheet);
        } else {            
            setParentCircuitSheet(SchematischeEingabe2.Singleton._circuitSheet);
        }


    }

    boolean isSubCircuitComponent() {
        return _isHiddenSubCircuitComponent;
    }

    /**
     * @return the _identifier
     */
    public UniqueObjectIdentifer getIdentifier() {
        return _identifier;
    }

    public long getUniqueObjectIdentifier() {
        return _identifier.getIdentifier();
    }
    
    public abstract int elementAngeklickt(final Point clickPoint);

    /**
     * used to determine if component is selected via a window, or for instance
     * to detect if component is outside the sheeet size.
     *
     * @return
     */
    abstract Collection<? extends Point> getAllDimensionPoints();

    public abstract boolean testDoDoubleClickAction(final Point clickPoint);

    abstract public void doDoubleClickAction(final Point clickedPoint);

    public void setParent(final AbstractCircuitSheetComponent parent) {
        _parent = parent;
    }

    public void shiftAllIdentifiers(final long shiftValue) {
        getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + shiftValue);
        if (this instanceof ComponentCoupable) {
            ComponentCoupling coupling = ((ComponentCoupable) this).getComponentCoupling();
            coupling.shiftReferenceIDs(shiftValue);
        }
        if (_tempParentSheetIdentifier != 0) {
            _tempParentSheetIdentifier += shiftValue;
        };
    }    

    public abstract String getExportImportCharacters();
    
    public boolean allParentSubcircuitsEnabled() {        
        if(_parentCircuitSheet != null && _parentCircuitSheet instanceof SubCircuitSheet) {
            SubCircuitSheet subSheet = (SubCircuitSheet) _parentCircuitSheet;
            final SubcircuitBlock subBlock = subSheet._subBlock;
            if(subBlock._isEnabled.getValue() != Enabled.ENABLED) {
                return false;
            }
        } 
        return true;
    }
    
}
