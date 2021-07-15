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
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCapacitor extends AbstractNonLinearCircuitComponent 
implements CurrentMeasurable, DirectVoltageMeasurable {
    private static final double WIDTH = 0.9;
    private static final double HEIGHT = 0.4;
    
    final UserParameter<Double> _capacitance = UserParameter.Builder.
            <Double>start("capacitance", 100e-9).           
            mapDomains(ConnectorType.LK, ConnectorType.THERMAL).            
            longName(I18nKeys.CAPACITOR_C_F, I18nKeys.CAPACITOR_CTH_JK).
            shortName("C", "Cth").
            unit("F", "J/K").
            showInTextInfo(TextInfoType.SHOW_NEVER). // nonlinearity: custom display!
            arrayIndex(this, 0).
            build();                                    
    
    UserParameter<Double> _initialValue = UserParameter.Builder.
            <Double>start("initVoltage", 0.0).           
            mapDomains(ConnectorType.LK, ConnectorType.THERMAL).            
            longName(I18nKeys.INITIAL_VOLTAGE, I18nKeys.INITIAL_TEMPERATURE).
            shortName("uC(0)", "Temp(0)").
            unit("V", "K").
            showInTextInfo(TextInfoType.SHOW_NON_NULL).            
            arrayIndex(this, 1).
            build();                                            

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        parameter[2] = 0;
        parameter[3] = 0;
        parameter[4] = 0;
        parameter[5] = 0;
        parameter[7] = 0;
        parameter[8] = 0;
        parameter[9] = 0;
        parameter[10] = 0;
        initialize();
    }

    public boolean updateNonlinearCapacitances() {
        // parameter[6] : original starting capacitance
        // parameter[7] : actual value of nonlinear capacitance        
        boolean returnValue = false;
        if (_isNonlinearForCalculationUsage) {
            parameter[7] = getActualValueLOGFromLinearizedCharacteristic(Math.abs(parameter[3]));
            if (parameter[6] == 0) {
                parameter[6] = getActualValueLOGFromLinearizedCharacteristic(Math.abs(parameter[1]));
            }
        } else {
            parameter[7] = parameter[0];
            parameter[6] = parameter[0];
        }


        return returnValue;
    }

    @Override
    protected final void drawForeground(final Graphics2D g2d) {
        double hoi = 0.2;
        g2d.drawLine(0, dpix * 2, 0, -dpix * 2);
        g2d.fillRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT), (int) (dpix * (2 * WIDTH)), (int) (dpix * (2 * HEIGHT)));
        g2d.setColor(Color.white);
        g2d.fillRect((int) (-dpix * WIDTH), (int) (-dpix * hoi), (int) (dpix * (2 * WIDTH)), (int) (dpix * (2 * hoi - 0.05)));
    }
        
    @Override
    public final String getNonlinearFileEnding() {
        return ".nlc";
    }

    @Override
    public final double[] getNonlinXDefault() {
        return NONLIN_CAP_X_DEFAULT;
    }

    @Override
    public final double[] getNonlinYDefault() {
        return NONLIN_CAP_Y_DEFAULT;
    }

    @Override
    protected final Window openDialogWindow() {
        return new CapacitorDialog(this);
    }

    @Override
    public final String getNonlinearFileExtension() {
        return ".nlc";
    }

    @Override
    public final String getIndependentVariableName() {
        return "u";
    }

    @Override
    public final String getNonlinearName() {
        return "capacitance";
    }

    @Override
    public String getNonlinearNameShort() {
        return "C(u(";
    }
    
    
    
    @Override
    public final double[][] getInitalNonlinValues() {
        double[][] returnValue = new double[2][NONLIN_IND_X_DEFAULT.length];
        for(int i = 0; i < NONLIN_IND_X_DEFAULT.length; i++) {
            returnValue[0][i] = NONLIN_CAP_X_DEFAULT[i];
            returnValue[1][i] = NONLIN_CAP_Y_DEFAULT[i];
        }
        return returnValue;
    }

    @Override
    public final UserParameter<Double> getNonlinearReplacedParameter() {
        return _capacitance;
    }                
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new CapacitorCalculator(this));
    }
    
    
}
