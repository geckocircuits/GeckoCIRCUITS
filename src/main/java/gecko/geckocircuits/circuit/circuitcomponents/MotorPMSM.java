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

import gecko.geckocircuits.allg.UserParameter;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.geckocircuits.circuit.TerminalRelativePosition;
import gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import static java.lang.Math.*;
import java.util.Arrays;
import java.util.List;

public final class MotorPMSM extends AbstractThreePhaseMotor {
    
    private static final int FLUX_LINKAGE_INDEX = 7;
    private static final int FRICTION_INDEX = 8;
    private static final int INERTIA_INDEX = 9;
    private static final int LOAD_TORQUE_INDEX = 10;
    private static final int POLE_PAIR_INDEX = 11;
    private static final int STATOR_RES_INDEX = 12;
    private static final int INIT_ROT_SPEED_INDEX = 17;
    private static final int INIT_ROT_POS_INDEX = 18;        
    private static final int STATOR_INDUCTANCE_D_INDEX = 13;
    private static final int STATOR_INDUCTANCE_Q_INDEX = 14;
    private static final int INI_CURRENT_A_INDEX = 15;
    private static final int INI_CURRENT_B_INDEX = 16;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(MotorPMSM.class, "PMSM", I18nKeys.PMSM, I18nKeys.PERMANENT_MAGNET_SYNCHRONOUS_MACHINE);
    
    private double _fluxLinkage;    
    final UserParameter<Double> _fluxLinkagePar = UserParameter.Builder.
            <Double>start("inducedbythepermanentMagnet", 0.15).
            longName(I18nKeys.FLUX_LINKAGE).
            shortName("Psi").
            addAlternativeShortName("Flux").
            unit("Wb").
            arrayIndex(this, FLUX_LINKAGE_INDEX).
            build();

    private double _statorInductanceD;
    final UserParameter<Double> _statorInductanceDPar = UserParameter.Builder.
            <Double>start("statorInductanceD", 0.0020).
            longName(I18nKeys.STATOR_INDUCTANCE_D).
            shortName("Ld").
            unit("H").
            arrayIndex(this, STATOR_INDUCTANCE_D_INDEX).
            build();
    
    private double _statorInductanceQ;
    final UserParameter<Double> _statorInductanceQPar = UserParameter.Builder.
            <Double>start("statorInductanceQ", 0.0020).
            longName(I18nKeys.STATOR_INDUCTANCE_Q).
            shortName("Lq").
            unit("H").
            arrayIndex(this, STATOR_INDUCTANCE_Q_INDEX).
            build();
                
    
    
    @Override
    void setTerminals() {
        XIN.add(new TerminalRelativePosition(this, -2, 1));
        XIN.add(new TerminalRelativePosition(this, -2, 0));
        XIN.add(new TerminalRelativePosition(this, -2, -1));        
    }    
                        

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();
        _fluxLinkage = _fluxLinkagePar.getValue();
        _statorInductanceD = _statorInductanceDPar.getValue();
        _statorInductanceQ = _statorInductanceQPar.getValue();
        

        _controlledAnchorSourceA.parameter[7] = 0;
        _controlledAnchorSourceC.parameter[7] = 0;
                
        isd0 = 2.0 / 3.0 * (isa0 * cos(_thetaElectric) + isb0 * cos(_thetaElectric - 2 * PI / 3) - (isa0 + isb0) * cos(_thetaElectric + 2 * PI / 3));
        isq0 = -2.0 / 3.0 * (isa0 * sin(_thetaElectric) + isb0 * sin(_thetaElectric - 2 * PI / 3) - (isa0 + isb0) * sin(_thetaElectric + 2 * PI / 3));

        _controlledAnchorSourceA.parameter[1] = isa0;
        _controlledAnchorSourceC.parameter[1] = -(isa0 + isb0);
    }
            
    @Override
    public List<String> getParameterStringIntern() {
        return Arrays.asList("isa [A]", "isb [A]", "isc [A]", "omega", "n [rpm]", "theta [rad]", "Tel [Nm]");
    }            

    
    @Override
    void calculateMotorEquations(final double deltaT, double time) {
        double uab = _controlledAnchorSourceA.parameter[7];
        double ubc = -_controlledAnchorSourceC.parameter[7];
        
        double ud = (2 * uab * cos(_thetaElectric) + ubc * (cos(_thetaElectric) + sqrt(3) * sin(_thetaElectric))) / 3;
        double uq = -(2 * uab * sin(_thetaElectric) + ubc * (sin(_thetaElectric) - sqrt(3) * cos(_thetaElectric))) / 3;

        double udi = _omegaElectric * _statorInductanceQ * isq;
        double uqi = _omegaElectric * (_statorInductanceD * isd + _fluxLinkage);

        isd = (ud + udi + _statorInductanceD * isd0 / deltaT) / (_statorResistance + _statorInductanceD / deltaT);
        isq = (uq - uqi + _statorInductanceQ * isq0 / deltaT) / (_statorResistance + _statorInductanceQ / deltaT);                                            
    }                                        

    @Override
    double calculateElectricTorque() {        
        return 1.5 * isq * (_fluxLinkage + (_statorInductanceD - _statorInductanceQ) * isd);
    }                 
            
    @Override
    protected Window openDialogWindow() {
        return new MotorPMSMDialog(this);
    }
        
    @Override
    public int getIndexForLoadTorque() {
        return LOAD_TORQUE_INDEX;
    }
    
    @Override
    int getFrictionCoefficientIndex() {
        return FRICTION_INDEX;
    }

    @Override
    int getInertiaIndex() {
        return INERTIA_INDEX;
    }
    
    @Override
    int getPolePairIndex() {
        return POLE_PAIR_INDEX;
    }

    @Override
    int getStatorResistanceIndex() {
        return STATOR_RES_INDEX;
    }
    
    @Override
    int getInitialRotationSpeedIndex() {
        return INIT_ROT_SPEED_INDEX;
    }
    
    @Override
    int getInitialRotorPositionIndex() {
        return INIT_ROT_POS_INDEX;
    }

    @Override
    int getInitialStatorCurrentIndexA() {
        return INI_CURRENT_A_INDEX;
    }

    @Override
    int getInitialStatorCurrentIndexB() {
        return INI_CURRENT_B_INDEX;
    }           
}
