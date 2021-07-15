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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.StartupWindow;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlOutput;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public final class ReglerFromEXTERNAL extends RegelBlockSimulink implements VariableTerminalNumber {

    public static List<RegelBlock> fromExternals = new ArrayList<RegelBlock>();
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerFromEXTERNAL.class, "FromEXT", I18nKeys.IMPORT_DATA_FROM_SIMULINK);
    private int _terminalNumber;
    private String _externalName = "name";
    private static final double DA_CONST = 0.5;
    private static final double WIDTH = 0.3;
    private int externalOrderNumber;
    private static final int DPFX = 8, DPFY = 3;
    public double[] dataVector;

    public ReglerFromEXTERNAL() {
        super();
        _terminalNumber = 1;  // default: 1 Anschluss bringen Signale von EXTERNAL
        this.setOutputTerminalNumber(_terminalNumber);
        fromExternals.add(this);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"0", "1", "2", "etc."};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public void setInputTerminalNumber(final int number) {
        // here, we don't have input terminals!
    }

    @Override
    public void setOutputTerminalNumber(final int number) {
        this._terminalNumber = number;

        while (YOUT.size() > number) {
            YOUT.pop();
        }

        while (YOUT.size() < number) {
            YOUT.add(new TerminalControlOutput(this, 1, -YOUT.size()));
        }

        dataVector = new double[_terminalNumber];
    }

    public int getTerminalNumber() {
        return _terminalNumber;
    }

    @SuppressWarnings("PMD")
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (StartupWindow.testDialogOpenSourceVersion("Simulink interface")) {
            return new AbstractControlCalculatable(0, YOUT.size()) {
                @Override
                public void berechneYOUT(double deltaT) {
                    
                }
            };
        } else {
            return new AbstractControlCalculatable(0, _terminalNumber) {
                @Override
                public void berechneYOUT(final double deltaT) {
                    // 'parameter[]' muss vom externen Programm mit den aktuellen Signal-Werten beschrieben werden
                    for (int i = 0; i < _terminalNumber; i++) {
                        _outputSignal[i][0] = dataVector[i];  // Signal-Quelle
                    }
                }
            };
        }
    }

    public void setExternalName(final String name) {
        _externalName = name;
    }

    @SuppressWarnings("PMD")
    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;
        final Color origColor = graphics.getColor();

        graphics.setColor(getBackgroundColor());

        xKlickMin = (int) (dpix * (xPos - WIDTH));
        xKlickMax = (int) (dpix * (xPos + WIDTH));
        yKlickMin = (int) (dpix * (yPos - WIDTH));
        yKlickMax = (int) (dpix * (yPos + 1.0 * _terminalNumber));

        graphics.fillRect((int) (dpix * (xPos - 0.4)), (int) (dpix * (yPos - 0.4)), (int) (dpix * (2 * 0.4)), (int) (dpix * (1.0 * _terminalNumber)));
        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (xPos - 0.4)), (int) (dpix * (yPos - 0.4)), (int) (dpix * (2 * 0.4)), (int) (dpix * (1.0 * _terminalNumber)));
        // Pfeil-Symbol:

        double pf = 3.5;  // Pfeilspitzen-X-Abstand
        double pfym = yPos - 0.4 + (1.0 * _terminalNumber) / 2;  // Pfeil-Y-Koordinate
        graphics.drawPolygon(new int[]{(int) (dpix * (xPos - 0.4)) - DPFX, (int) (dpix * (xPos - 0.4)) - DPFX, (int) (dpix * (xPos - 0.4))}, new int[]{(int) (dpix * pfym) - DPFY, (int) (dpix * pfym) + DPFY, (int) (dpix * pfym)}, 3);
        graphics.drawString("From", (int) (dpix * (xPos - pf)), (int) (dpix * pfym - 1 * graphics.getFont().getSize() / 2));
        graphics.drawString("EXTERN", (int) (dpix * (xPos - pf)), (int) (dpix * pfym + 3 * graphics.getFont().getSize() / 2));


        graphics.drawLine((int) (dpix * (xPos - 0.4)), (int) (dpix * pfym), (int) (dpix * (xPos - pf)), (int) (dpix * pfym));  // zum Pfeil gehoerig
        for (int i1 = 0; i1 < _terminalNumber; i1++) {
            int xi1 = (int) (dpix * (xPos + 2 * DA_CONST)), yi1 = (int) (dpix * (yPos + 1.0 * i1));
            graphics.setColor(GlobalColors.farbeEXTERNAL_TERMINAL);
            graphics.drawString(Integer.valueOf(i1 + 1).toString(), xi1 + 8, yi1 + graphics.getFont().getSize() / 2);
            graphics.setColor(origColor);
        }

        graphics.setColor(Color.black);
        graphics.drawString(_externalName, (int) (dpix * (xPos - pf)),
                (int) (dpix * (1 + pfym) + 3 * graphics.getFont().getSize() / 2));
        graphics.setColor(origColor);
    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    @Override
    public void deleteActionIndividual() {
        super.deleteActionIndividual();
        fromExternals.remove(this);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        _terminalNumber = tokenMap.readDataLine("tn", _terminalNumber);
        int orderNumber = tokenMap.readDataLine("torder", externalOrderNumber);
        insertOrderCorrect(orderNumber);
        setOutputTerminalNumber(_terminalNumber);
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\ntn"), _terminalNumber);
        DatenSpeicher.appendAsString(ascii.append("\ntorder"), fromExternals.indexOf(this));
    }

    @Override
    List<RegelBlock> getOrderList() {
        return fromExternals;
    }

    @Override
    Stack<AbstractTerminal> getVariableTerminals() {
        return YOUT;
    }

    private final class CompareOrder implements Comparator {

        @Override
        public int compare(final Object obj1, final Object obj2) {
            if (obj1 instanceof ReglerFromEXTERNAL && obj2 instanceof ReglerFromEXTERNAL) {
                final ReglerFromEXTERNAL toExtern1 = (ReglerFromEXTERNAL) obj1;
                final ReglerFromEXTERNAL toExtern2 = (ReglerFromEXTERNAL) obj2;
                if (toExtern1.externalOrderNumber == toExtern2.externalOrderNumber) {
                    return 0;
                }
                if (toExtern1.externalOrderNumber < toExtern2.externalOrderNumber) {
                    return -1;
                }
                return 1;
            }

            assert false;
            return 0;
        }
    }

    public void insertOrderCorrect(final int orderNo) {
        externalOrderNumber = orderNo;
        Collections.sort(fromExternals, new CompareOrder());
    }

    @Override
    protected Window openDialogWindow() {
        return new DialogExternal(this);
    }
}
