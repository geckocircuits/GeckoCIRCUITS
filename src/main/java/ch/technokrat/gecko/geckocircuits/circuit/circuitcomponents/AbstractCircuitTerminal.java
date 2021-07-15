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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSheet;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.EnumTerminalLocation;
import ch.technokrat.gecko.geckocircuits.circuit.SubCircuitTerminable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalHiddenSubcircuit;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalInterface;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalSubCircuitBlock;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalToWrap;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalTwoPortComponent;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.Point;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.List;

public abstract class AbstractCircuitTerminal extends AbstractCircuitBlockInterface implements SubCircuitTerminable {

    private final TerminalToWrap _wrapped = new TerminalToWrap(this);

    public AbstractCircuitTerminal() {
        super();
        XIN.add(new TerminalTwoPortComponent(this, 0));
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
    protected void drawForeground(final Graphics2D graphics) {
        _wrapped.drawForeground(graphics);
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        _wrapped.drawBackground(graphics);
    }

    @Override
    public void setParentCircuitSheet(final CircuitSheet parentCircuitSheet) {
        super.setParentCircuitSheet(parentCircuitSheet);
        _wrapped.createBlockTerminal();
    }

    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);
        _wrapped.copyAdditionalParameters(((AbstractCircuitTerminal) originalBlock)._wrapped);
    }

    @Override
    public void findAndSetReferenceToParentSheet(final List<SubcircuitBlock> allSubs, final String rootSubName) {
        super.findAndSetReferenceToParentSheet(allSubs, rootSubName);
        _wrapped.createBlockTerminal();
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        _wrapped.importIndividual(tokenMap);
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii);
        _wrapped.exportAsciiIndividual(ascii);
    }

    @Override
    public TerminalSubCircuitBlock getBlockTerminal() {
        return _wrapped.getBlockTerminal();
    }

    @Override
    public void deleteActionIndividual() {
        super.deleteActionIndividual();
        _wrapped.deleteActionIndividual();
    }

    @Override
    protected Window openDialogWindow() {
        return new TerminalCircuitDialog(this);
    }

    @Override
    public EnumTerminalLocation getTerminalLocation() {
        return _wrapped.getTerminalLocation();
    }

    @Override
    public void setComponentDirection(final ComponentDirection orientation) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }

    @Override
    void drawConnectorLines(final Graphics2D graphics) {
        // no connector lines to draw!
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}