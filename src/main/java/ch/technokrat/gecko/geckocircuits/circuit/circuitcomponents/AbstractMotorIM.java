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
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativeFixedDirection;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;

public abstract class AbstractMotorIM extends AbstractMotorIMCommon {    
    double _magnetizingInductance;
    public final UserParameter<Double> _magnetizingInductancePar = UserParameter.Builder.
            <Double>start("magnetizingInductance", 0.05).
            longName(I18nKeys.MAGNETIZING_INDUCTANCE).
            shortName("Lm").
            unit("H").
            arrayIndex(this, 21).
            build();        

    double isd0, isq0 = 0, ird0 = 0, irq0 = 0;
    double isd, isq = 0, ird = 0, irq = 0;                       
    
    @Override
    final int getInitialRotationSpeedIndex() {
        return 24;
    }
    
    @Override
    int getInitialRotorPositionIndex() {
        return 25;
    }
        
    @Override
    int getInitialStatorCurrentIndexA() {
        return 22;
    }

    @Override
    int getInitialStatorCurrentIndexB() {
        return 23;
    }
    
    @Override
    int getInitialStatorFluxIndexD() {
        return 26;
    }
    
    @Override
    int getInitialStatorFluxIndexQ() {
        return 27;
    }

    @Override
    final double calculateElectricTorque() {
        return 1.5 * _magnetizingInductance * (isq * ird - isd * irq);
    }            

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();
        _magnetizingInductance = _magnetizingInductancePar.getValue();
        psisd0 = initialStatorFluxD.getValue();
        psisd = psisd;
        psisq0 = initialStatorFluxQ.getValue();
        psisq = psisq0;        
        isd0 = isa;        
    }    

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        super.drawConnectorLines(graphics);
    }
    
    
    
}
