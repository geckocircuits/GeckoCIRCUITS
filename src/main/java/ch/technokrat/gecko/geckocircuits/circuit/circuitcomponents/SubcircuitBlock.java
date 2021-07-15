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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractSpecialBlock;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSheet;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.ElementDisplayProperties;
import ch.technokrat.gecko.geckocircuits.circuit.Enabled;
import ch.technokrat.gecko.geckocircuits.circuit.EnumTerminalLocation;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.SubCircuitTerminable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalInterface;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalSubCircuitBlock;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalToWrap;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.DialogLabelEingeben;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.ReglerTERMINAL;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SubcircuitBlock extends AbstractSpecialBlock {

    public static final AbstractTypeInfo tInfo = new SpecialTypeInfo(SubcircuitBlock.class, "SUBCIRCUIT", I18nKeys.SUBCIRCUIT);
    
    private static final double BORDER_CLICK_OFFSET = 0.5;
    private static final int DEFAULT_BLOCK_WIDTH = 8;
    private static final int DEFAULT_BLOCK_HEIGHT = 5;
    private static final int DEFAULT_SHEET_WIDTH = 50;
    private static final int DEFAULT_SHEET_HEIGHT = 40;
    public final SubCircuitSheet _myCircuitSheet = new SubCircuitSheet(SchematischeEingabe2.Singleton, this);
    
    final UserParameter<Integer> _blockSizeX = UserParameter.Builder.
            <Integer>start("blockSizeX", DEFAULT_BLOCK_WIDTH).
            longName(I18nKeys.X_BLOCK_DIMENSION).
            shortName("BlockSizeX").
            arrayIndex(this, -1).
            build();
    final UserParameter<Integer> _blockSizeY = UserParameter.Builder.
            <Integer>start("blockSizeY", DEFAULT_BLOCK_HEIGHT).
            longName(I18nKeys.Y_BLOCK_DIMENSION).
            shortName("BlockSizeY").
            arrayIndex(this, -1).
            build();
    public final UserParameter<Integer> _sheetSizeX = UserParameter.Builder.
            <Integer>start("worksheetSizeX", DEFAULT_SHEET_WIDTH).
            longName(I18nKeys.X_SHEET_SIZE).
            shortName("SheetSizeX").
            arrayIndex(this, -1).
            build();
    
    public final UserParameter<Integer> _sheetSizeY = UserParameter.Builder.
            <Integer>start("worksheetSizeY", DEFAULT_SHEET_HEIGHT).
            longName(I18nKeys.Y_SHEET_SIZE).
            shortName("SheetSizeY").
            arrayIndex(this, -1).
            build();
    
    public final Set<SubCircuitTerminable> _myTerminals = new LinkedHashSet<SubCircuitTerminable>();

    public SubcircuitBlock() {
        
        super();

        _blockSizeX.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                for (SubCircuitTerminable terminal : _myTerminals) {
                    if (terminal.getTerminalLocation() == EnumTerminalLocation.RIGHT) {
                        final TerminalSubCircuitBlock term = terminal.getBlockTerminal();
                        term.setRelativePosition(_blockSizeX.getValue(), term.getRelativeY());
                    }
                }
            }
        });

        _blockSizeY.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                for (SubCircuitTerminable terminal : _myTerminals) {
                    if (terminal.getTerminalLocation() == EnumTerminalLocation.BOTTOM) {
                        final TerminalSubCircuitBlock term = terminal.getBlockTerminal();
                        term.setRelativePosition(term.getRelativeX(), _blockSizeY.getValue());
                    }
                }
            }
        });                
        
        _myCircuitSheet._worksheetSize._worksheetDimension.addModelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                int newX = _myCircuitSheet._worksheetSize._worksheetDimension.getValue().x;
                if(newX != _sheetSizeX.getValue()) {
                    _sheetSizeX.setUserValue(newX);
                }
                int newY = _myCircuitSheet._worksheetSize._worksheetDimension.getValue().y;
                if(newY != _sheetSizeY.getValue()) {
                    _sheetSizeY.setUserValue(newY);
                }             
            }
        });
        
        _sheetSizeX.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _myCircuitSheet._worksheetSize._worksheetDimension.setValue(new Point(_sheetSizeX.getValue(), _sheetSizeY.getValue()));
            }
        });
        
        _sheetSizeY.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _myCircuitSheet._worksheetSize._worksheetDimension.setValue(new Point(_sheetSizeX.getValue(), _sheetSizeY.getValue()));
            }
        });
        
        getIDStringDialog().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _myCircuitSheet.setNameLabelText();
            }
        });
        
        
        _myCircuitSheet._worksheetSize.setNewWorksheetSize(_sheetSizeX.getValue(), _sheetSizeY.getValue());
    }

    public boolean areTerminalPositionsOK() {
        System.err.println("Warning: Terminal position check disabled!");
        if(1>0) return true;
        for (SubCircuitTerminable terminal1 : _myTerminals) {
            for (SubCircuitTerminable terminal2 : _myTerminals) {
                if (terminal1 != terminal2 && TerminalToWrap.sameBlockPosition(terminal1, terminal2)) {                     
                    return false;
                }
            }
        }
        return true;
    }
    
    
    public Set<SubCircuitTerminable> getTerminalsWithWrongPosition() {
        Set<SubCircuitTerminable> returnValue = new HashSet<SubCircuitTerminable>();
        for (SubCircuitTerminable terminal1 : _myTerminals) {
            for (SubCircuitTerminable terminal2 : _myTerminals) {
                if (terminal1 != terminal2 && TerminalToWrap.sameBlockPosition(terminal1, terminal2)) {                     
                    returnValue.add(terminal1);
                    returnValue.add(terminal2);
                }
            }
        }
        return returnValue;
    }

    /**
     * when deleting a subcircuit component, we first have to delete all
     * sub-components!
     */
    @Override
    public void deleteActionIndividual() {
        final Collection<AbstractCircuitSheetComponent> local = _myCircuitSheet.getLocalSheetComponents();
        final AbstractCircuitSheetComponent[] copy = local.toArray(new AbstractCircuitSheetComponent[local.size()]);
        for (AbstractCircuitSheetComponent subComp : copy) {
            subComp.deleteComponent();
        }
        super.deleteActionIndividual();
    }

    @Override
    public void setToolbarPaintProperties() {
        _blockSizeX.setValueWithoutUndo(6);
        _blockSizeY.setValueWithoutUndo(3);        
        _textInfo.setPositionTextClickPointInitial(0, 0);
        _textInfo.setNewRelativePosition(new Point(-15, -15));
    }        

    public void recalculateTerminalPositions() {        
        for (SubCircuitTerminable term : _myTerminals) {
            switch (term.getTerminalLocation()) {
                case UP:
                    int pos = (int) (_blockSizeX.getValue() * 1.0 * term.getSheetPosition().x / _sheetSizeX.getValue());
                    term.getBlockTerminal().setRelativePosition(pos, -1);
                    break;
                case BOTTOM:
                    pos = (int) (_blockSizeX.getValue() * 1.0 * term.getSheetPosition().x / _sheetSizeX.getValue());
                    term.getBlockTerminal().setRelativePosition(pos, _blockSizeY.getValue());
                    break;
                case LEFT:                    
                    double ratio = 1.0 * term.getSheetPosition().y / _sheetSizeY.getValue();
                    pos = (int) (_blockSizeY.getValue() * ratio);
                    term.getBlockTerminal().setRelativePosition(-1, pos);                    
                    break;
                case RIGHT:
                    pos = (int) (_blockSizeY.getValue() * 1.0 * term.getSheetPosition().y / _sheetSizeY.getValue());
                    term.getBlockTerminal().setRelativePosition(_blockSizeX.getValue(), pos);                    
                    break;
                default:
                    assert false;
            }            
        }
        
    }

    @Override
    public void doDoubleClickAction(final Point clickedPoint) {
        final TerminalInterface clickedTerm = clickedTerminal(clickedPoint);
        if (clickedTerm != null && clickedTerm.getCircuitSheet() == SchematischeEingabe2.Singleton._visibleCircuitSheet) {
            final DialogLabelEingeben labelDialog = new DialogLabelEingeben(clickedTerm);
            labelDialog.setVisible(true);
            return;
        }
        final CircuitSheet newComp = _myCircuitSheet;
        SchematischeEingabe2.Singleton.setNewVisibleCircuitSheet(newComp);
    }

    @Override
    protected Window openDialogWindow() {
        // no dialog window for subcircuit block!
        assert false;
        return null;
    }

    @Override
    public int[] getAussenabmessungenRechteckEckpunkte() {
        return new int[]{dpix * getSheetPosition().x, dpix * getSheetPosition().y,
                    dpix * (getSheetPosition().x + _blockSizeX.getValue() - 1),
                    dpix * (getSheetPosition().y + _blockSizeY.getValue() - 1)};
    }

    public void insertTerminal(final SubCircuitTerminable toInsert) {
        if (!_myTerminals.contains(toInsert)) {
            _myTerminals.add(toInsert);
            XIN.add(toInsert.getBlockTerminal());
        }
        recalculateTerminalPositions();
    }

    @Override
    public void setComponentDirection(final ComponentDirection oritentation) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }

    @Override
    public ComponentDirection getComponentDirection() {
        return ComponentDirection.NORTH_SOUTH;
    }
    

    

    @Override
    public AbstractCircuitSheetComponent copyFabric(final long shiftValue) {
        final SubcircuitBlock returnValue = (SubcircuitBlock) super.copyFabric(shiftValue);
        returnValue._blockSizeX.setValueWithoutUndo(_blockSizeX.getValue());
        returnValue._blockSizeY.setValueWithoutUndo(_blockSizeY.getValue());
        final int newSizeX = _myCircuitSheet._worksheetSize.getSizeX();
        final int newSizeY = _myCircuitSheet._worksheetSize.getSizeY();
        returnValue._myCircuitSheet._worksheetSize.setNewWorksheetSize(newSizeX, newSizeY);

        final List<AbstractCircuitSheetComponent> allNewSubElements = new ArrayList<AbstractCircuitSheetComponent>();

        for (AbstractCircuitSheetComponent subComponent :
                _myCircuitSheet.getLocalSheetComponents().toArray(new AbstractCircuitSheetComponent[0])) {
            final AbstractCircuitSheetComponent subCopy = subComponent.copyFabric(shiftValue);
            subCopy.setParentCircuitSheet(returnValue._myCircuitSheet);
            // enshure that the names of components stays identical for sub-components!
            if (subCopy instanceof AbstractBlockInterface) {
                final String originalName = ((AbstractBlockInterface) subComponent).getStringID();
                ((AbstractBlockInterface) subCopy).getIDStringDialog().setNameUnChecked(originalName);
            }
            allNewSubElements.add(subCopy);
        }

        for (AbstractCircuitSheetComponent searchCoupable : allNewSubElements) {
            if (searchCoupable instanceof ComponentCoupable) {
                ((ComponentCoupable) searchCoupable).getComponentCoupling().trySetCopyReference(allNewSubElements);
            }
        }

        return returnValue;
    }
    

    @Override
    public ElementDisplayProperties getDisplayProperties() {
        return SchematischeEingabe2._lkDisplayMode;
    }

    @Override
    public int istAngeklickt(final int mouseX, final int mouseY) {
        if (dpix * (getSheetPosition().x - BORDER_CLICK_OFFSET) < mouseX
                && mouseX < dpix * (getSheetPosition().x + _blockSizeX.getValue())
                && dpix * (getSheetPosition().y - BORDER_CLICK_OFFSET) < mouseY
                && mouseY < dpix * (getSheetPosition().y + _blockSizeY.getValue())) {
            return 1;
        }
        return 0;
    }

    @Override
    protected void paintIndividualComponent(final Graphics2D graphics) {
        final AffineTransform oldTransform = graphics.getTransform();        
        graphics.translate(getSheetPosition().x * dpix, getSheetPosition().y * dpix);
        for (SubCircuitTerminable term : _myTerminals) {

            final Color terminalColor = getColorForTerminal(term);
            graphics.setColor(terminalColor);

            final int relativeY = term.getBlockTerminal().getRelativeY();
            final int relativeX = term.getBlockTerminal().getRelativeX();
            switch (term.getTerminalLocation()) {
                case LEFT:
                    graphics.drawLine(relativeX * dpix, relativeY * dpix,
                            (1 + relativeX) * dpix, relativeY * dpix);
                    break;
                case RIGHT:
                    graphics.drawLine(relativeX * dpix, relativeY * dpix, (relativeX - 1) * dpix, relativeY * dpix);
                    break;
                case BOTTOM:
                    graphics.drawLine(relativeX * dpix, relativeY * dpix, relativeX * dpix, (relativeY - 1) * dpix);
                    break;
                case UP:
                    graphics.drawLine(relativeX * dpix, relativeY * dpix, relativeX * dpix, (1 + relativeY) * dpix);
                    break;
                default:
                    assert false;
            }
        }

        graphics.setColor(Color.WHITE);
        graphics.fillRect(-dpix / 2, -dpix / 2, dpix * _blockSizeX.getValue(), dpix * _blockSizeY.getValue());

        graphics.setColor(Color.GRAY);
        graphics.drawRect(-dpix / 2, -dpix / 2, dpix * _blockSizeX.getValue(), dpix * _blockSizeY.getValue());
        graphics.setTransform(oldTransform);
    }

    @Override
    public Color getForeGroundColor() {
        return GlobalColors.farbeFertigElementLK;
    }

    /**
     * recursively disable/enable all sub-components. Be careful: shorting a
     * sub-block is not possible!
     */
    @Override
    public void setCircuitEnabled(final Enabled isEnabled) {
        if (isEnabled == Enabled.DISABLED_SHORT) {
            return; // we cannot short-disable the subcircuit block!
        }

        final Collection<AbstractCircuitSheetComponent> subComponents =
                _myCircuitSheet.getLocalSheetComponents();

        for (AbstractCircuitSheetComponent subComp : subComponents) {
            subComp.setCircuitEnabled(isEnabled);
        }
        super.setCircuitEnabled(isEnabled);
    }

    private Color getColorForTerminal(final Object terminal) {
        if (terminal instanceof AbstractCircuitTerminal) {
            final AbstractCircuitTerminal lkTerminal = (AbstractCircuitTerminal) terminal;
            switch (lkTerminal.getSimulationDomain()) {
                case LK:
                    return GlobalColors.farbeFertigElementLK;
                case THERMAL:
                    return GlobalColors.farbeFertigElementTHERM;
                case RELUCTANCE:
                    return GlobalColors.farbeElementRELFOREGROUND;
                case CONTROL:
                    return GlobalColors.farbeFertigElementCONTROL;
                default:
                    assert false;
                    return GlobalColors.farbeFertigElementLK;
            }
        } else if (terminal instanceof ReglerTERMINAL) {
            return GlobalColors.farbeFertigElementCONTROL;
        }
        
        assert false;
        return GlobalColors.farbeFertigElementLK;
    }     

    @Override
    protected void importIndividual(TokenMap tokenMap) {
        super.importIndividual(tokenMap); 
        
        if(tokenMap.containsToken("sheetSizeX")) {
            _sheetSizeX.setValueWithoutUndo(tokenMap.readDataLine("sheetSizeX", _sheetSizeX.getValue()));
            _sheetSizeY.setValueWithoutUndo(tokenMap.readDataLine("sheetSizeY", _sheetSizeY.getValue()));           
        } 
        
        _myCircuitSheet._worksheetSize._worksheetDimension.setValueWithoutUndo(new Point(_sheetSizeX.getValue(), _sheetSizeY.getValue()));
        
    }
    
}
