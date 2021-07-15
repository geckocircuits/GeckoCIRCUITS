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
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
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
    
    private static SchematischeEingabe2 _se;
    private static double br = 1.4, da = 0.4;
    
    public static void setReferenzAufSchematischEingabe(SchematischeEingabe2 se) {
        _se = se;
    }

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
        if (header[knotenIndex + 1].equals(new String(""))) {
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
        DatenSpeicher.appendAsString(ascii.append("\nscale1"), ((Float) _uziDisplay.jSpinnerLength1.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\nscale2"), ((Float) _uziDisplay.jSpinnerLength2.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\nscale3"), ((Float) _uziDisplay.jSpinnerZ1.getValue()).toString());

        DatenSpeicher.appendAsString(ascii.append("\naverage1"), ((Float) _uziDisplay.jSpinnerAverage1.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\naverage2"), ((Float) _uziDisplay.jSpinnerAverage2.getValue()).toString());
        DatenSpeicher.appendAsString(ascii.append("\naverage3"), ((Float) _uziDisplay.jSpinnerZ2.getValue()).toString());
        
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        _uziDisplay.jSpinnerLength1.setValue(new Float(tokenMap.readDataLine("scale1", (Float) _uziDisplay.jSpinnerLength1.getValue())));
        _uziDisplay.jSpinnerLength2.setValue(new Float(tokenMap.readDataLine("scale2", (Float) _uziDisplay.jSpinnerLength2.getValue())));
        _uziDisplay.jSpinnerZ1.setValue(new Float(tokenMap.readDataLine("scale3", (Float) _uziDisplay.jSpinnerZ1.getValue())));
        _uziDisplay.jSpinnerAverage1.setValue(new Float(tokenMap.readDataLine("average1", (Float) _uziDisplay.jSpinnerAverage1.getValue())));
        _uziDisplay.jSpinnerAverage2.setValue(new Float(tokenMap.readDataLine("average2", (Float) _uziDisplay.jSpinnerAverage2.getValue())));
        _uziDisplay.jSpinnerZ2.setValue(new Float(tokenMap.readDataLine("average3", (Float) _uziDisplay.jSpinnerZ2.getValue())));
    }
}
