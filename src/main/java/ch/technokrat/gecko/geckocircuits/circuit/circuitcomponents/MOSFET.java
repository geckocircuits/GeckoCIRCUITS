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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.swing.JOptionPane;

// Leistungskreis-IGBT (Knickkennlinie, Modifikation des THYR)
public final class MOSFET extends AbstractSwitch implements HiddenSubCircuitable {
    public static final AbstractTypeInfo TYPE_INFO = 
            new CircuitTypeInfo(MOSFET.class, "MOSFET", I18nKeys.MOSFET, I18nKeys.IDEALIZED_METAL_OXIDE_FIELD_EFFECT);
    
    private static final double WIDTH = 0.7;
    private static final double HEIGHT = 0.4;
    private static final double ANTI_DIODE_SIZE = 0.3;
    
    
    final Diode _antiParallelDiode;
    
    UserParameter<Double> _adRon = UserParameter.Builder.
            <Double>start("antiParallelDiodeRon", AbstractSwitch.RD_ON_DEFAULT).
            longName(I18nKeys.ON_RESISTANCE_ANTIPARALLEL).
            shortName("ad_rON").
            unit("Ohm").
            arrayIndex(this, -1).
            build();
    UserParameter<Double> _adRoff = UserParameter.Builder.
            <Double>start("antiParallelDiodeRoff", AbstractSwitch.RD_OFF_DEFAULT).
            longName(I18nKeys.OFF_RESISTANCE_ANTIPARALLEL).
            shortName("ad_rOFF").
            unit("Ohm").
            arrayIndex(this, -1).
            build();
    UserParameter<Double> _adUf = UserParameter.Builder.
            <Double>start("antiParallelDiodeUF", AbstractSwitch.UF_DEFAULT).
            longName(I18nKeys.FORWARD_VOLTAGE_ANTIPARALLEL).
            shortName("ad_uF").
            unit("Ohm").
            arrayIndex(this, -1).
            build();
    private static boolean _show170_22bugfixWarning = false;
    private static boolean _show172_50bugfixWarning = false;

    public MOSFET() {
        super();
        _antiParallelDiode = (Diode) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_D, this);
        double[] diodeParameter = new double[]{AbstractSwitch.RD_OFF_DEFAULT, 550e-3, 3.9e-3, 
            AbstractSwitch.RD_OFF_DEFAULT, 0, 0, 0, 0, -1, -1, 0, -1, 1};
        _antiParallelDiode.setParameter(diodeParameter);
        _antiParallelDiode.setInputTerminal(0, YOUT.get(0));
        _antiParallelDiode.setOutputTerminal(0, XIN.get(0));
        _antiParallelDiode.getIDStringDialog().setRandomStringID();

        _antiParallelDiode.kOn.setValueWithoutUndo(0.0);
        _antiParallelDiode.kOff.setValueWithoutUndo(0.0);        
        
        _adUf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _antiParallelDiode._forwardVoltageDrop.setValueWithoutUndo(_adUf.getValue());
            }
        });

        _adRoff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _antiParallelDiode._offResistance.setValueWithoutUndo(_adRoff.getValue());
            }
        });

        _adRon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _antiParallelDiode._onResistance.setValueWithoutUndo(_adRon.getValue());
            }
        });

        kOn.setValueWithoutUndo(30e-6);
        kOff.setValueWithoutUndo(15e-6);
    }

    public Diode getAntiParallelDiode() {
        return _antiParallelDiode;
    }
    
    

    public void setzeParameterZustandswerteAufNULL() {
        parameter[0] = AbstractSwitch.RD_OFF_DEFAULT;
        parameter[4] = 0;
        parameter[5] = 0;
        parameter[8] = 0;
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        addGateTextInfo();
        verluste.addTextInfoValue(_textInfo);
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        // gate line
        graphics.drawPolyline(
                new int[]{-dpix, -dpix, (int) (-dpix * (1 + HEIGHT))},
                new int[]{-dpix, dpix, dpix}, 3);



        final double lshort = 0.3;
        graphics.drawLine((int) (-dpix * WIDTH), (int) (dpix * lshort), (int) (-dpix * WIDTH), -(int) (dpix * lshort));
        graphics.drawLine((int) (-dpix * WIDTH), (int) (dpix * (lshort + 1)), (int) (-dpix * WIDTH), -(int) (dpix * (lshort - 1)));
        graphics.drawLine((int) (-dpix * WIDTH), (int) (dpix * (lshort - 1)), (int) (-dpix * WIDTH), -(int) (dpix * (lshort + 1)));

        graphics.drawLine(0, 0, 0, dpix);
        graphics.drawLine(0, 0, (int) (-dpix * WIDTH), 0);

        final double triangleWidth = 0.25;
        final double xOffset = 0.15;
        graphics.fillPolygon(
                new int[]{(int) (-dpix * xOffset), (int) (-dpix * xOffset), (int) (-dpix * (WIDTH - xOffset))},
                new int[]{(int) (-dpix * triangleWidth), (int) (dpix * triangleWidth), 0}, 3);

        //Gate Diode:
        graphics.drawPolyline(
                new int[]{0, (int) (dpix * WIDTH), (int) (dpix * WIDTH), 0},
                new int[]{-dpix, -dpix, dpix, dpix}, 4);


        drawAntiParallelDiode(graphics);
    }

    private void drawAntiParallelDiode(final Graphics2D graphics) {

        graphics.drawLine((int) (dpix * (WIDTH - ANTI_DIODE_SIZE)), (int) (-dpix * (ANTI_DIODE_SIZE)),
                (int) (dpix * (WIDTH + ANTI_DIODE_SIZE)), (int) (-dpix * (ANTI_DIODE_SIZE)));
        graphics.fillPolygon(
                new int[]{(int) (dpix * WIDTH), (int) (dpix * (WIDTH - ANTI_DIODE_SIZE)), (int) (dpix * (WIDTH + ANTI_DIODE_SIZE))},
                new int[]{(int) (-dpix * (ANTI_DIODE_SIZE)), (int) (dpix * (ANTI_DIODE_SIZE)),
            (int) (dpix * (ANTI_DIODE_SIZE))}, 3);
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawPolyline(
                new int[]{0, 0, (int) (-dpix * WIDTH)},
                new int[]{-2 * dpix, -dpix, -dpix}, 3);

        graphics.drawPolyline(
                new int[]{(int) (-dpix * WIDTH), 0, 0},
                new int[]{dpix, dpix, dpix * 2}, 3);
    }            

    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        return Arrays.asList(new AbstractBlockInterface[]{_antiParallelDiode});
    }

    @Override
    public boolean includeParentInSimulation() {
        return true;
    }

    @Override
    protected Window openDialogWindow() {
        return new MOSFETDialog(this);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap); //To change body of generated methods, choose Tools | Templates.
        boolean warning1AlreadyShown = false;
        boolean warning2AlreadyShown = false;
        
        if (_show170_22bugfixWarning) {
            warning1AlreadyShown = true;
        }
        if (!tokenMap.containsToken("bugfix_170_22")) {
            _show170_22bugfixWarning = true;
        }
                                
        
        if (_show172_50bugfixWarning) {
            warning2AlreadyShown = true;
        }
        
        if (!tokenMap.containsToken("bugfix_172_50")) {
            _show172_50bugfixWarning = true;
        }
        
        if (_show170_22bugfixWarning) {
            _show172_50bugfixWarning = true;
        }

        if (_show170_22bugfixWarning && !warning1AlreadyShown) {
            JOptionPane.showMessageDialog(GeckoSim._win, "Your model contains MOSEFET components. Beginning from GeckoCIRCUITS release 1.70 build number 23,"
                    + "\n some bugfixes might change your model behavior. The relevant bugfixes are:\n\n"
                    + "    - Loss calculation with simple parameters: An errorneous forward voltage drop was considered in the\n"
                    + "      loss calculation. This additional loss is not present in this release of GeckoCIRCUITS, anymore.\n"
                    + "    - Current measurement of MOSFET: Older GeckoCIRCUITS releases did not consider the antiparallel diode\n"
                    + "      current in the MOSFET current measurement. If your control model depends on the MOSFET current,\n"
                    + "      measurement it might behave different, now.", "MOSFET Bugfix Information", JOptionPane.PLAIN_MESSAGE);
        }
        
        if (_show172_50bugfixWarning && !warning2AlreadyShown) {
            JOptionPane.showMessageDialog(GeckoSim._win, "Your model contains MOSEFET components. Beginning from GeckoCIRCUITS release 1.72 build number 50,"
                    + "\nfrom November 2015, the following bug fix might change your model behavior:\n\n"
                    + "The current measurement of the Mosfet component had the wrong direction / sign during freewheeling state.\n"
                    + "This bug is fixed in GeckoCIRCUITS 1.72, build number 50. In case your control model depends on the MOSFET\n"
                    + "current during freewheeling state, then the simulation could behave different.", "MOSFET Bugfix Information", JOptionPane.PLAIN_MESSAGE);
        }
        
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii); //To change body of generated methods, choose Tools | Templates.
        ascii.append("\nbugfix_170_22 false");
        ascii.append("\nbugfix_172_50 false");
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return AbstractCircuitBlockInterface.getCalculatorsFromSubComponents(this);        
    }
}
