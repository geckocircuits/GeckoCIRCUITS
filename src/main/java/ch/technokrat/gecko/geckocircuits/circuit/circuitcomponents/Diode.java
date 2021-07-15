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

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculationDetail;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossProperties;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public final class Diode extends AbstractSemiconductor implements ForwardVoltageDropable {
    static final AbstractTypeInfo TYPE_INFO = 
            new CircuitTypeInfo(Diode.class, "D", I18nKeys.DIODE, I18nKeys.IDEAL_DIODE_WITH_ON_RESISTANCE);
    
    private static final double WIDTH = 0.5;
    private static final double HEIGHT = 0.6;
    private static final int NUMBER_POLYGON_POINTS = 3;
    
    final UserParameter<Double> _forwardVoltageDrop = UserParameter.Builder.
            <Double>start("forwardVoltageDrop", AbstractSwitch.UF_DEFAULT).
            longName(I18nKeys.FORWARD_VOLTAGE_DROP).
            shortName("uF").
            unit("V").
            arrayIndex(this, 1).
            build();
    
    public final UserParameter<Boolean> useNonlinearChar = UserParameter.Builder.
            <Boolean>start("useNonlinearChar", false).                       
            longName(I18nKeys.IF_TRUE_USE_NONLINEAR_CHARACTERISTIC).
            shortName("useNonlinearChar").
            arrayIndex(this, -1).
            build();                               
        
    
    private final LossProperties _losses = new LossProperties(this);
    public DiodeCharacteristic _diodeChar;

    @Override
    public LossProperties getVerlustBerechnung() {
        return _losses;
    }

    @Override
    public void initExtraFiles() {
        _losses.getDetailedLosses().initLossFile();
    }

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        parameter[0] = parameter[3];
        parameter[4] = 0;
        parameter[5] = 0;
        
        if(_losses._lossType.getValue() == LossCalculationDetail.DETAILED
                && useNonlinearChar.getValue()) {
            initializeDiodeCharacteristic();
        } else {
            _diodeChar = null;
        }        
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        graphics.drawPolygon(new int[]{0, (int) (-dpix * WIDTH), (int) (dpix * WIDTH)},
                new int[]{(int) (dpix * HEIGHT), (int) (-dpix * HEIGHT), (int) (-dpix * HEIGHT)}, NUMBER_POLYGON_POINTS);
        graphics.fillRect((int) (-dpix * WIDTH), (int) (dpix * HEIGHT - 2), (int) (dpix * 2 * WIDTH), 2);
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        graphics.fillPolygon(new int[]{0, (int) (-dpix * WIDTH), (int) (dpix * WIDTH)}, new int[]{(int) (dpix * HEIGHT),
            (int) (-dpix * HEIGHT), (int) (-dpix * HEIGHT)}, NUMBER_POLYGON_POINTS);
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        _losses.addTextInfoValue(_textInfo);
    }

    @Override
    protected Window openDialogWindow() {
        return new DiodeDialog(this);
    }

    @Override
    public UserParameter<Double> getForwardVoltageDropParameter() {        
        return _forwardVoltageDrop;
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new DiodeCalculator(this));
    }

    private void initializeDiodeCharacteristic() {
        _diodeChar = new DiodeCharacteristic(_losses._lossCalculationDetailed);
    }
}
