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
package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.geckocircuits.allg.GlobalFilePathes;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.control.Point;
import gecko.i18n.resources.I18nKeys;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Window;
import java.util.List;

final class ThermMODUL extends AbstractCircuitBlockInterface {
    public static final AbstractTypeInfo TYPE_INFO = 
            new ThermalTypeInfo(ThermMODUL.class, "MOD", I18nKeys.POWER_MODULE_THERMAL_MODEL);

    private final double _height;
    // pro befestigtem Halbleiter(modul) gibt es einen Anschluss
    private double[] _xBef = new double[MAX_INPUT_NO], _yBef = new double[MAX_INPUT_NO];
    private final int _noOfChips;
    private double _xOUT, _yOUT;  // es gibt 1 SignalAusgang (Waermestrom zum Kuehler)
    // zum Zeichnen eines Kuehlers (Pixelpunkt-Daten vom ICEPAK-Screenshot gemessen):
    private final double _scaling;  // Skalierung
    private static final int X_CENTER = 430, Y_CENTER = 250;  // Zentrum des Modul-Bildes in PixelPunkten
    // Dateiname (inkl. Pfad) des Modul-ESBs:
    private String _fileName = GlobalFilePathes.DATNAM_NOT_DEFINED;
    // ID-Strings der SubCircuit-Elemente -->
    private static final int D_E = 5;
    private static final double B_R = 2.5;
    private static final int MAX_INPUT_NO = 99;

    ThermMODUL() {
        super();
        _scaling = B_R / X_CENTER;
        _height = Y_CENTER * _scaling;
        //_elementTherm.setLabelAnfangsKnoten(new String[]{"bli", "bla", "blub"});
        //_elementTherm.setLabelEndKnoten(new String[]{"blo", "blu", "blub"});
        //----------------
        _noOfChips = 2;  // vorerst einmal


        //_xOUT = (int) (sheetPosition.x);
        //_yOUT = (int) (sheetPosition.y + ho + 1);        
    }

    public int getChipAnzahl() {
        return _noOfChips;
    }

    public void setDateiname(final String datnam) {
        _fileName = datnam;
    }

    public String getDateiname() {
        return _fileName;
    }
    

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        for (int i1 = 0; i1 < _noOfChips; i1++) {
            _xBef[i1] = (int) (0 + 1);
            _yBef[i1] = (int) (0 - _noOfChips - 1 + i1);
            graphics.drawLine((int) (dpix * _xBef[i1]), (int) (dpix * _yBef[i1]), (int) (dpix * 0) + D_E,
                    (int) (dpix * _yBef[i1]));
        }
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        _xOUT = 0;
        _yOUT = (int) (_height + 1);
        graphics.drawLine((int) (dpix * _xOUT), (int) (dpix * (0 + 1)), (int) (dpix * _xOUT), (int) (dpix * _yOUT));

        PowerModulePainter.zeichne(graphics, this, graphics.getColor(), dpix);
        if (_noOfChips > 0) {
            drawInputs(graphics, graphics.getColor());
        }
    }

    private void drawInputs(final Graphics graphics, final Color color1) {        
        graphics.setColor(Color.lightGray);
        graphics.fillRect((int) (dpix * getSheetPosition().x) - D_E, (int) (dpix * (_yBef[0] - 1 / 2.0)),
                2 * D_E, (int) (dpix * (_yBef[_noOfChips - 1] - _yBef[0] + 1)));
        if (color1.equals(Color.gray)) {
            graphics.setColor(Color.white);
        } else {
            graphics.setColor(Color.darkGray);
        }
        graphics.drawRect((int) (dpix * getSheetPosition().x) - D_E, (int) (dpix * (_yBef[0] - 1 / 2.0)), 2 * D_E,
                (int) (dpix * (_yBef[_noOfChips - 1] - _yBef[0] + 1)));
        if (color1.equals(Color.gray)) {
            graphics.setColor(Color.lightGray);
        } else {
            graphics.setColor(color1);
        }
        graphics.drawLine((int) (dpix * getSheetPosition().x), (int) (dpix * getSheetPosition().y), 
                (int) (dpix * getSheetPosition().x), (int) (dpix * (_yBef[_noOfChips - 1] + 1 / 2.0)));
    }

    @Override
    public void doDoubleClickAction(final Point clickedPoint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Window openDialogWindow() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
