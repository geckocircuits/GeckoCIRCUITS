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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.NothingToDoCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.util.List;

public final class ReglerTERMINAL extends RegelBlock implements SubCircuitTerminable {                
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerTERMINAL.class, "CONTROL_TERMINAL", I18nKeys.CONTROL_TERMINAL);
    private final TerminalToWrap _wrapped = new TerminalToWrap(this);
    
    public ReglerTERMINAL() {
        super();
        XIN.add(new TerminalControlBidirectional(this, 0, 0));             
        // this is only a dummy terminal, so that we get the correct label name 
        // when loading from file. It will be replaced when the references are
        // set correctly.
        YOUT.add(new TerminalHiddenSubcircuit(this));
    }
    

    @Override
    public void absetzenElement() {
        super.absetzenElement();
        _wrapped.absetzenElement();
    }
    
    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);
        _wrapped.copyAdditionalParameters(((ReglerTERMINAL) originalBlock)._wrapped);        
    }
        
        
    @Override
    public int istAngeklickt(final int mouseX, final int mouseY) {        
        if (((getSheetPosition().x*dpix - dpix/2 <= mouseX) && mouseX <= (getSheetPosition().x*dpix + dpix/2) 
                && (getSheetPosition().y*dpix - dpix/2 <= mouseY) && (mouseY <= getSheetPosition().y*dpix + dpix/2 ))) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * return null if no terminal was clicked!
     *
     * @param px screen coordinates in (dpix-scaled!) pixel
     * @param py
     * @return
     */
    @Override
    public TerminalInterface clickedTerminal(final Point clickPoint) {
        // the label dialog should never apear!
        return null;
    }
    
    @Override
    public void moveComponent(final Point moveToPoint) {                
        _wrapped.moveComponent(moveToPoint);        
    }        
    
    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        // don't draw a block rectangle for this component!
        xKlickMin = getSheetPosition().x * dpix - dpix / 2;
        xKlickMax = getSheetPosition().x * dpix + dpix / 2;
        yKlickMin = getSheetPosition().y * dpix - dpix / 2;
        yKlickMax = getSheetPosition().y * dpix + dpix / 2; 
    }            

    @Override
    protected void paintIndividualComponent(final Graphics2D graphics) {
        final AffineTransform origTranform = graphics.getTransform();
        graphics.translate(getSheetPosition().x * dpix, getSheetPosition().y * dpix);
        final Color origColor = graphics.getColor();
        graphics.setColor(GlobalColors.farbeElementCONTROLHintergrund);
        _wrapped.drawBackground(graphics);
        graphics.setColor(origColor);
        _wrapped.drawForeground(graphics);   
        graphics.setTransform(origTranform);
        drawBlockRectangle(graphics);
    }            

    @Override
    protected String getCenteredDrawString() {
        // don't draw a string here!
        return "";
    }        
                

    @Override
    public void setParentCircuitSheet(final CircuitSheet parentCircuitSheet) {
        super.setParentCircuitSheet(parentCircuitSheet);
        _wrapped.createBlockTerminal();

    }
    
    @Override
    public void findAndSetReferenceToParentSheet(final List<SubcircuitBlock> allSubs, final String rootSubName) {
        super.findAndSetReferenceToParentSheet(allSubs, rootSubName);
        _wrapped.createBlockTerminal();        
    }
    
    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        _wrapped.importIndividual(tokenMap);            
    }
    
    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii);
        _wrapped.exportAsciiIndividual(ascii);        
    }                

    @Override
    public void deleteActionIndividual() {        
        super.deleteActionIndividual();
        _wrapped.deleteActionIndividual();        
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }    
    class ReglerTerminalCalculator extends AbstractControlCalculatable {

        public ReglerTerminalCalculator() {
            super(1, 1);
        }

        
        @Override
        public void berechneYOUT(double deltaT) {
            _outputSignal[0][0] = 3.14;
        }
        
    }
    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ReglerTerminalCalculator();
    }    

    @Override
    protected Window openDialogWindow() {
        return new ReglerTerminalDialog(this);
    }                
    
    @Override
    public TerminalSubCircuitBlock getBlockTerminal() {
        return _wrapped.getBlockTerminal();
    }

    @Override
    public EnumTerminalLocation getTerminalLocation() {
        return _wrapped.getTerminalLocation();
    }
}
