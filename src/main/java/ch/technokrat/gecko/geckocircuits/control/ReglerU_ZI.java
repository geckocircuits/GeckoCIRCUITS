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

import ch.technokrat.gecko.geckocircuits.allg.ProjectData;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;

public final class ReglerU_ZI extends RegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerU_ZI.class, "UZI", I18nKeys.U_ZI_DIAGRAM);

    private UZiDisplay _uziDisplay = new UZiDisplay(this);
    
    private int tnX;  // Nummer der Terminals fuer Signal-Anschluss
    private String[] header;

    private static double br = 1.4, da = 0.4;

    @Override
    protected final Window openDialogWindow() {
        return _uziDisplay;
    }
    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new AbstractControlCalculatable(XIN.size(), 0) {
            @Override
            public void berechneYOUT(final double deltaT) {
                   _uziDisplay.drawVector(_time, _inputSignal);
            }
        };
    }

    public ReglerU_ZI() {
        super(4, 0);
        tnX = 4;        
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    public void setTerminalKnotenLabel(String q, int knotenIndex) {
        // ... wird von 'setLabelAnfangsKnoten()' in ElementCONTROL aufgerufen, immer wenn die Netzliste aktualisiert wird
        // aktueller Zugriff auf die Knoten-Labels fuer die SCOPE-Header -->
        if (header == null) {
            header = new String[tnX + 1];
        }
        header[knotenIndex + 1] = q;
        if (header[knotenIndex + 1].equals("")) {
            header[knotenIndex + 1] = "sg." + (knotenIndex + 1);
        }
    }

    @Override
    public void drawBlockRectangle(final Graphics2D g) {
        int x = getSheetPosition().x;
        int y = getSheetPosition().y;

        // Klickbereich Symbol:
        xKlickMin = (int) (dpix * (x - da));
        xKlickMax = (int) (dpix * (x + da));
        yKlickMin = (int) (dpix * (y - br));
        yKlickMax = (int) (dpix * (y - br + Math.max(tnX, 0)));
        Color origColor = g.getColor();
        g.setColor(getBackgroundColor());        

        g.fillRect((int) (dpix * (x - 0.4)), (int) (dpix * (y - br+1)), (int) (dpix * (2 * da)), (int) (dpix * (1.0 * Math.max(tnX, 0))));
        g.setColor(origColor);
        g.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y - br+1)), (int) (dpix * (2 * da)), (int) (dpix * (1.0 * Math.max(tnX, 0))));

        int ds1 = 3, ds2 = 3;
        g.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y )) - ds2, ds1, (int) (dpix * (1)) + 2 * ds2);
        g.drawRect((int) (dpix * (x - 0.4)), (int) (dpix * (y + 2)) - ds2, ds1, (int) (dpix * (1)) + 2 * ds2);
    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }            

    @Override
    protected void exportAsciiIndividual(StringBuffer ascii) {
        ProjectData.appendAsString(ascii.append("\nscale1"), ((Number) _uziDisplay.jSpinnerLength1.getValue()).toString());
        ProjectData.appendAsString(ascii.append("\nscale2"), ((Number) _uziDisplay.jSpinnerLength2.getValue()).toString());
        ProjectData.appendAsString(ascii.append("\nscale3"), ((Number) _uziDisplay.jSpinnerZ1.getValue()).toString());

        ProjectData.appendAsString(ascii.append("\naverage1"), ((Number) _uziDisplay.jSpinnerAverage1.getValue()).toString());
        ProjectData.appendAsString(ascii.append("\naverage2"), ((Number) _uziDisplay.jSpinnerAverage2.getValue()).toString());
        ProjectData.appendAsString(ascii.append("\naverage3"), ((Number) _uziDisplay.jSpinnerZ2.getValue()).toString());

    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        _uziDisplay.jSpinnerLength1.setValue(tokenMap.readDataLine("scale1", ((Number) _uziDisplay.jSpinnerLength1.getValue()).floatValue()));
        _uziDisplay.jSpinnerLength2.setValue(tokenMap.readDataLine("scale2", ((Number) _uziDisplay.jSpinnerLength2.getValue()).floatValue()));
        _uziDisplay.jSpinnerZ1.setValue(tokenMap.readDataLine("scale3", ((Number) _uziDisplay.jSpinnerZ1.getValue()).floatValue()));
        _uziDisplay.jSpinnerAverage1.setValue(tokenMap.readDataLine("average1", ((Number) _uziDisplay.jSpinnerAverage1.getValue()).floatValue()));
        _uziDisplay.jSpinnerAverage2.setValue(tokenMap.readDataLine("average2", ((Number) _uziDisplay.jSpinnerAverage2.getValue()).floatValue()));
        _uziDisplay.jSpinnerZ2.setValue(tokenMap.readDataLine("average3", ((Number) _uziDisplay.jSpinnerZ2.getValue()).floatValue()));
    }
}
