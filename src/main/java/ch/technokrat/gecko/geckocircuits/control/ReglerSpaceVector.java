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
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.SpaceVectorCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;

public final class ReglerSpaceVector extends RegelBlock {

    private static final int NO_INPUTS = 9;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerSpaceVector.class, "SV", I18nKeys.SPACE_VECTOR_DIAGRAM);
    private final SpaceVectorDisplay svd = new SpaceVectorDisplay(this);
    private String[] header;
    private static final double DA_VALUE = 0.5;
    private static final double WIDTH = 1.4;

    @Override
    protected final Window openDialogWindow() {
        return svd;
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new SpaceVectorCalculator(svd);
    }

    public ReglerSpaceVector() {
        super(NO_INPUTS, 0);
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    public void setTerminalKnotenLabel(final String q, final int knotenIndex) {
        // ... wird von 'setLabelAnfangsKnoten()' in ElementCONTROL aufgerufen, immer wenn die Netzliste aktualisiert wird
        // aktueller Zugriff auf die Knoten-Labels fuer die SCOPE-Header -->
        if (header == null) {
            header = new String[NO_INPUTS + 1];
        }
        header[knotenIndex + 1] = q;
        if (header[knotenIndex + 1].equals(new String(""))) {
            header[knotenIndex + 1] = "sg." + (knotenIndex + 1);
        }
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        int x = getSheetPosition().x;
        int y = getSheetPosition().y;

        xKlickMin = (int) (dpix * (x - DA_VALUE));
        xKlickMax = (int) (dpix * (x + DA_VALUE));
        yKlickMin = (int) (dpix * (y - WIDTH));
        yKlickMax = (int) (dpix * (y - WIDTH + NO_INPUTS));
        Color origColor = graphics.getColor();
        graphics.setColor(getBackgroundColor());  // default        

        graphics.fillRect((int) (dpix * (x - 0.4)), (int) (dpix * (y - WIDTH + 1)), (int) (dpix * (2 * DA_VALUE)), (int) (dpix * NO_INPUTS));
        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y - WIDTH + 1)), (int) (dpix * (2 * DA_VALUE)), (int) (dpix * NO_INPUTS));
        int ds1 = 3, ds2 = 3;
        graphics.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y - 1 + 1)) - ds2, ds1, (int) (dpix * (2)) + 2 * ds2);
        graphics.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y + 2 + 1)) - ds2, ds1, (int) (dpix * (2)) + 2 * ds2);
        graphics.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y + 5 + 1)) - ds2, ds1, (int) (dpix * (2)) + 2 * ds2);

    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\nscale1"), ((Float) svd.jSpinnerLength1.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\nscale2"), ((Float) svd.jSpinnerLength2.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\nscale3"), ((Float) svd.jSpinnerLength3.getValue()).toString());

        DatenSpeicher.appendAsString(ascii.append("\naverage1"), ((Float) svd.jSpinnerAverage1.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\naverage2"), ((Float) svd.jSpinnerAverage2.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\naverage3"), ((Float) svd.jSpinnerAverage3.getValue()).toString());

    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        svd.jSpinnerLength1.setValue(new Float(tokenMap.readDataLine("scale1", 1f)));
        svd.jSpinnerLength2.setValue(new Float(tokenMap.readDataLine("scale2", 1f)));
        svd.jSpinnerLength3.setValue(new Float(tokenMap.readDataLine("scale3", 1f)));
        svd.jSpinnerAverage1.setValue(new Float(tokenMap.readDataLine("average1", 1f)));
        svd.jSpinnerAverage2.setValue(new Float(tokenMap.readDataLine("average2", 1f)));
        svd.jSpinnerAverage3.setValue(new Float(tokenMap.readDataLine("average3", 1f)));
    }
    
}
