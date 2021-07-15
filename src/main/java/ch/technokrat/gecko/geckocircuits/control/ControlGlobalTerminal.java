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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.DialogGlobalTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.GlobalTerminable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlBidirectional;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalInterface;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.NothingToDoCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.HashSet;
import java.util.Set;

public final class ControlGlobalTerminal extends RegelBlock implements GlobalTerminable {

    public static final Set<ControlGlobalTerminal> ALL_GLOBALS = new HashSet<ControlGlobalTerminal>();
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlGlobalTerminal.class,"GLOBAL_CONTROL", I18nKeys.GLOBAL_CONTROL_TERMINAL);

    public ControlGlobalTerminal() {
        super();
        XIN.add(new TerminalControlBidirectional(this, 0, 0));
        ALL_GLOBALS.add(this);
    }

    @Override
    public void doDoubleClickAction(Point clickedPoint) {
        DialogGlobalTerminal dialog = new DialogGlobalTerminal(GeckoSim._win, this);
        dialog.setVisible(true);
    }

    @Override
    public int istAngeklickt(final int mouseX, final int mouseY) {
        if (((getSheetPosition().x * dpix - dpix / 2 <= mouseX) && mouseX <= (getSheetPosition().x * dpix + dpix / 2)
                && (getSheetPosition().y * dpix - dpix / 2 <= mouseY) && (mouseY <= getSheetPosition().y * dpix + dpix / 2))) {
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
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new NothingToDoCalculator(1, 1);
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

        int diameter = dpix;
        graphics.setColor(getBackgroundColor());

        graphics.fillRect(dpix * getSheetPosition().x - diameter / 2,
                dpix * getSheetPosition().y - diameter / 2, diameter + 1, diameter + 1);

        super.paintIndividualComponent(graphics);

        diameter = dpix / 2;
        graphics.setColor(getForeGroundColor());

        graphics.fillOval(-diameter / 2 + dpix * getSheetPosition().x,
                -diameter / 2 + dpix * getSheetPosition().y, diameter + 1, diameter + 1);


    }
    

    @Override
    protected String getCenteredDrawString() {
        // don't draw a string here!
        return "";
    }
    

    @Override
    public void deleteActionIndividual() {
        ALL_GLOBALS.remove(this);
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public Set<? extends GlobalTerminable> getAllGlobalTerminals() {
        return ALL_GLOBALS;
    }

    @Override
    protected Window openDialogWindow() {
        // nothing todo, the dialog is opened in "doDoubleClickAction"
        assert false;
        return null;
    }
}
