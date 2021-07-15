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

import ch.technokrat.gecko.GeckoRuntimeException;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.JOptionPane;


/**
 * I made this component to be wrapped from ReglerTerminal and LKreisterminal.
 * Otherwise, I cannot avoid code duplication. This is maybe not really
 * "beautiful", however DRY: don't repeat yourself!
 * @author andy
 */
public final class TerminalToWrap {

    private final AbstractBlockInterface _parentComponent;
    private final SubCircuitTerminable _subTerminable;
    private EnumTerminalLocation _terminalLocation = EnumTerminalLocation.LEFT;
    private TerminalSubCircuitBlock _blockTerminal;
    
    public TerminalToWrap(final AbstractBlockInterface parentComponent) {
        _parentComponent = parentComponent;
        assert parentComponent instanceof SubCircuitTerminable;
        _subTerminable = (SubCircuitTerminable) _parentComponent;
    }

    public void reCalculateLocation(final Point moveToPoint) {
        final int wsSizeX = _parentComponent.getParentCircuitSheet()._worksheetSize.getSizeX();
        final int wsSizeY = _parentComponent.getParentCircuitSheet()._worksheetSize.getSizeY();
        final int checkedPointX = Math.min(moveToPoint.x + _parentComponent.getPositionVorVerschieben().x, wsSizeX - 1);
        final int checkedPointY = Math.min(moveToPoint.y + _parentComponent.getPositionVorVerschieben().y, wsSizeY - 1);
        if (checkedPointY > checkedPointX * 1.0 * wsSizeY / wsSizeX) {
            if (checkedPointY < wsSizeY - checkedPointX * 1.0 * wsSizeY / wsSizeX) {
                _parentComponent.setSheetPositionWithoutUndo(new Point(1, checkedPointY));
                _terminalLocation = EnumTerminalLocation.LEFT;
            } else {
                _parentComponent.setSheetPositionWithoutUndo(new Point(checkedPointX, wsSizeY - 1));
                _terminalLocation = EnumTerminalLocation.BOTTOM;
            }

        } else {
            if (checkedPointY < wsSizeY - checkedPointX * 1.0 * wsSizeY / wsSizeX) {
                _parentComponent.setSheetPositionWithoutUndo(new Point(checkedPointX, 2));
                _terminalLocation = EnumTerminalLocation.UP;
            } else {
                _parentComponent.setSheetPositionWithoutUndo(new Point(wsSizeX - 1, checkedPointY));
                _terminalLocation = EnumTerminalLocation.RIGHT;
            }
        }

        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            ((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock.recalculateTerminalPositions();
        }
    }

    public void drawBackground(final Graphics2D graphics) {
        final int diameter = AbstractBlockInterface.dpix;
        graphics.fillOval(-diameter / 2 - 1, -diameter / 2 - 1, diameter + 1, diameter + 1);
    }

    public void absetzenElement() {
        
        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet
                && !((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock.areTerminalPositionsOK()) {
            String problematicList = "";
            
            for(SubCircuitTerminable term : ((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock.getTerminalsWithWrongPosition()) {
                problematicList += term.getStringID() + " position (" + term.getBlockTerminal().getRelativeX() + " " + term.getBlockTerminal().getRelativeY() + ")\n";
            }
            
            JOptionPane.showMessageDialog(null,
                    "The following terminals have an identical position within the subcircuit block:\n"
                    + problematicList
                    + "Please move these terminals to other locations on the subcircuit sheet.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);            
        }

    }

    public void moveComponent(final Point moveToPoint) {
        if (moveToPoint.x == moveToPoint.y) {
            return;
        }
        reCalculateLocation(moveToPoint);
    }

    public void deleteActionIndividual() {
        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            final SubCircuitSheet sub = (SubCircuitSheet) _parentComponent.getParentCircuitSheet();
            sub._subBlock._myTerminals.remove(_subTerminable);
            sub._subBlock.XIN.remove(_subTerminable.getBlockTerminal());
        }
    }

    public void drawForeground(final Graphics2D graphics) {
        final int dpix = AbstractBlockInterface.dpix;
        final int diameter = dpix / 2;
        graphics.fillOval(-diameter / 2 - 1, -diameter / 2 - 1, diameter + 1, diameter + 1);        
        
        if (_parentComponent.getModus() == ComponentState.SELECTED 
                && _parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            final AffineTransform oldTrans = graphics.getTransform();
            final SubcircuitBlock subBlock = ((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock;
            AffineTransform newtransform = new AffineTransform(oldTrans);
            graphics.setTransform(newtransform);
            graphics.translate((- subBlock.getSheetPosition().x - getBlockTerminal().getRelativeX()) * dpix,
                    (- subBlock.getSheetPosition().y - getBlockTerminal().getRelativeY()) * dpix);
            subBlock.paintGeckoComponent(graphics);
            graphics.setTransform(oldTrans);
        }
    }
    

    public void createBlockTerminal() {
        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            final SubCircuitSheet subSheet = (SubCircuitSheet) _parentComponent.getParentCircuitSheet();
            final SubcircuitBlock subBlock = subSheet._subBlock;
            final String oldLabel = _parentComponent.YOUT.pop().getLabelObject().getLabelString();
            if (_subTerminable.getBlockTerminal() == null) {
                _blockTerminal = new TerminalSubCircuitBlock(subBlock, _subTerminable);
            }
            _subTerminable.getBlockTerminal().getLabelObject().setLabel(oldLabel);
            _parentComponent.YOUT.add(_subTerminable.getBlockTerminal());
            subBlock.insertTerminal(_subTerminable);
        }
    }

    public void exportAsciiIndividual(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\nterminalLocation"), _terminalLocation.ordinal());
    }

    public void importIndividual(final TokenMap tokenMap) {
        _terminalLocation = EnumTerminalLocation.getFromOrdinal(tokenMap.readDataLine("terminalLocation",
                _terminalLocation.ordinal()));
    }

    public void copyAdditionalParameters(final TerminalToWrap originalTerminal) {
        _terminalLocation = originalTerminal._terminalLocation;
    }

    public static boolean sameBlockPosition(final SubCircuitTerminable terminable1, final SubCircuitTerminable terminable2) {
        final TerminalSubCircuitBlock termSub1 = (TerminalSubCircuitBlock) terminable1.getBlockTerminal();
        final TerminalSubCircuitBlock termSub2 = (TerminalSubCircuitBlock) terminable2.getBlockTerminal();
        
        final int relX1 = termSub1.getRelativeX();
        final int relX2 = termSub2.getRelativeX();
        final int relY1 = termSub1.getRelativeY();
        final int relY2 = termSub2.getRelativeY();
        return relX1 == relX2 && relY1 == relY2;
    }

    public EnumTerminalLocation getTerminalLocation() {
        return _terminalLocation;
    }

    public TerminalSubCircuitBlock getBlockTerminal() {
        return _blockTerminal;
    }
}
