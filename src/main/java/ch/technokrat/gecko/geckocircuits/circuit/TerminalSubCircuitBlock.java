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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitTerminal;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.ReglerTERMINAL;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

/**
 * This is the "block"-terminal of the subcuircuit block, which has the terminal
 * name as label.
 *
 * @author andreas
 */
public final class TerminalSubCircuitBlock extends AbstractTerminal implements ControlTerminable {

    private final SubcircuitBlock _subcircuitBlock;
    private SubCircuitTerminable _lkTerminal;
    private int relativeX;
    private int relativeY;
    private int _nodeNumber;

    public TerminalSubCircuitBlock(final SubcircuitBlock relatedComponent, SubCircuitTerminable lkTerminal) {
        super(relatedComponent);
        _subcircuitBlock = relatedComponent;
        _lkTerminal = lkTerminal;        
    }

    @Override
    public Point getPosition() {
        return new Point(_subcircuitBlock.getSheetPosition().x + relativeX, _subcircuitBlock.getSheetPosition().y + relativeY);
    }

    public int getRelativeX() {
        return relativeX;
    }

    public int getRelativeY() {
        return relativeY;
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        Color oldColor = graphics.getColor();
        graphics.setColor(_lkTerminal.getForeGroundColor());        
        super.paintComponent(graphics);
        graphics.setColor(oldColor);
    }

    @Override
    public ConnectorType getCategory() {

        if (_lkTerminal instanceof AbstractCircuitTerminal) {
            AbstractCircuitTerminal lkTerminal = (AbstractCircuitTerminal) _lkTerminal;
            return lkTerminal.getSimulationDomain();
        }

        if (_lkTerminal instanceof ReglerTERMINAL) {
            return ConnectorType.CONTROL;
        }
        assert false;
        return ConnectorType.LK;
    }

    @Override
    public AbstractTerminal createCopy(final AbstractBlockInterface relatedComponent) {        
        final SubCircuitTerminable terminable = (SubCircuitTerminable) relatedComponent;             
        assert terminable.getParentCircuitSheet() instanceof SubCircuitSheet;                
        final SubCircuitSheet subSheet = (SubCircuitSheet) terminable.getParentCircuitSheet();
        final SubcircuitBlock subBlock = subSheet._subBlock;        
        final TerminalSubCircuitBlock returnValue = new TerminalSubCircuitBlock(subBlock, terminable);
        
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        returnValue.relativeX = relativeX;
        returnValue.relativeY = relativeY;
        return returnValue;
    }

    public void setRelativePosition(final int relX, int relY) {
        relativeX = relX;
        relativeY = relY;
    }

    @Override
    public void paintLabelString(final Graphics2D graphics) {        
        final int dpix = AbstractCircuitSheetComponent.dpix;
        Color oldColor = graphics.getColor(); 
        
        graphics.setColor(_lkTerminal.getForeGroundColor());        
        
        if (!_label.getLabelString().isEmpty()) {
            graphics.drawString(_label.getLabelString(), (int) (dpix * getPosition().x) + DX_IN, (int) (dpix * getPosition().y) + DY_TEXT);
        }

        String terminalName = _lkTerminal.getStringID();
        FontRenderContext frc = graphics.getFontRenderContext();
        final int stringHeight = (int) graphics.getFont().getStringBounds(terminalName, frc).getHeight();
        final int stringWidth = (int) graphics.getFont().getStringBounds(terminalName, frc).getWidth();

        AffineTransform oldTrans = graphics.getTransform();
        AffineTransform newTrans = new AffineTransform();

        switch (_lkTerminal.getTerminalLocation()) {
            case LEFT:
                graphics.drawString(_lkTerminal.getStringID(), (int) (dpix * getPosition().x) + dpix / 2 + 2,
                        (int) (dpix * getPosition().y) + stringHeight / 2 - 1);
                break;
            case RIGHT:
                graphics.drawString(_lkTerminal.getStringID(), (int) (dpix * getPosition().x) - dpix / 2 - 2 - stringWidth,
                        (int) (dpix * getPosition().y) + stringHeight / 2 - 1);
                break;
            case BOTTOM:
                newTrans.translate(oldTrans.getTranslateX() + (int) (dpix * getPosition().x),
                        oldTrans.getTranslateY() + (int) (dpix * getPosition().y));
                newTrans.rotate(-Math.PI / 2);
                graphics.setTransform(newTrans);
                graphics.drawString(_lkTerminal.getStringID(), dpix / 2, stringHeight / 2 - 1);

                break;
            case UP:

                newTrans.translate(oldTrans.getTranslateX() + (int) (dpix * getPosition().x),
                        oldTrans.getTranslateY() + (int) (dpix * getPosition().y));
                newTrans.rotate(-Math.PI / 2);
                graphics.setTransform(newTrans);
                graphics.drawString(_lkTerminal.getStringID(), -stringWidth - dpix / 2 - 2, stringHeight / 2 - 1);
                break;
            default:
                assert false;
        }

        graphics.setTransform(oldTrans);
        graphics.setColor(oldColor);

    }

    @Override
    public int getNodeNumber() {
        return _nodeNumber;
    }
    
    @Override
    public void setNodeNumber(final int newValue) {
        _nodeNumber = newValue;
    }
    
    @Override
    public void clearNodeNumber() {
        _nodeNumber = -1;
    }
}
