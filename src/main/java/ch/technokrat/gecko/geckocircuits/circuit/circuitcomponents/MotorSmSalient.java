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
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.util.List;

public final class MotorSmSalient extends AbstractMotorSM {
    private static final double INITIAL_UNSAT_INDUCTANCE = 0.003;
    
    private static final int UNSAT_MAGNETIZING_INDUC_INDEX = 20;
    private static final int DAMPER_LEAKAGE_IND_D_INDEX = 27;        
    private static final int DAMPER_LEAKAGE_IND_Q_INDEX = 28;        
    private static final int INIT_ROT_SPEED_INDEX = 29;
    private static final int INIT_ROT_POS_INDEX = 30;          
    private static final int INIT_STATOR_CUR_A_INDEX = 31;
    private static final int INIT_STATOR_CUR_B_INDEX = 32;        
    private static final int INIT_FIELD_CUR_INDEX = 33;
    private static final int INIT_STATOR_FLUX_D_INDEX = 34;
    private static final int INIT_STATOR_FLUX_Q_INDEX = 35;                
    private static final int SAT_IND_INDEX = 36;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(MotorSmSalient.class, "SM-SAL", I18nKeys.SM_SAL, I18nKeys.SALIENT_POLE_SYNCHRONOUS_MACHINE);
        
    
    private double _Lmq0;
    final UserParameter<Double> _unsatMagnetizingInductQPar = UserParameter.Builder.
            <Double>start("unsaturatedMagnetizingInductanceQ", INITIAL_UNSAT_INDUCTANCE).
            longName(I18nKeys.UNSATURATED_MAGNETIZING_INDUCTANCE).
            shortName("Lm0_q").
            unit("H").
            arrayIndex(this, UNSAT_MAGNETIZING_INDUC_INDEX).
            build();

    private double _m2;
    
    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();
        _Lmq0 = _unsatMagnetizingInductQPar.getValue();
        _m2 = _Lmq0 / Lmd0;
    }    
    
    @Override
    protected Window openDialogWindow() {
        return new MotorSmSalientDialog(this);
    }

    @Override
    void calculateMotorEquations(final double deltaT, double time) {
        double uab = _controlledAnchorSourceA.parameter[7];
        double ubc = -_controlledAnchorSourceC.parameter[7];
        double uf = _controlledSource3.parameter[7];
                
        // Berechnung der Maschinen-Diff.Gl: 
        // Block 'vdq': 
        double ud = calculate_ud(uab, ubc);
        double uq = calculate_uq(uab, ubc);
        // Block 'isdq,psimdq': 
        double phimuX1 = Math.sqrt(psimd * psimd + psimq * psimq / _m2), phimuX2 = psiT / 1e3;
        double phimu = (phimuX1 > phimuX2) ? phimuX1 : phimuX2;                
        
        double inv_Lmd = (Mf - Mi) / Math.PI * (((phimu - psiT) * Math.atan(tauT * (phimu - psiT)) - psiT * Math.atan(tauT * psiT))
                + 0.5 / tauT * (Math.log(1 + (tauT * psiT) * (tauT * psiT)) - Math.log(1 + tauT * tauT * (phimu - psiT) * (phimu - psiT)))) / phimu
                + 0.5 * (Mf + Mi);
        double inv_Lmq = inv_Lmd / _m2;
        
        double ddt_psimd = (isd * (Rkd / Llkd - _statorResistance / Lls) + isq * _omegaElectric 
                + ifd * (Rkd / Llkd - Rf / Llf) - Rkd * inv_Lmd / Llkd * psimd + _omegaElectric / Lls * psimq
                + ud / Lls + uf / Llf) / (inv_Lmd + 1 / Lls + 1 / Llf + 1 / Llkd);
        double ddt_psimq = (-_omegaElectric * isd + isq * (Rkq / Llkq1 - _statorResistance / Lls) - _omegaElectric / Lls * psimd - Rkq * inv_Lmq / Llkq1 * psimq + uq / Lls) / (inv_Lmq + 1 / Lls + 1 / Llkq1);
        psimd = psimd0 + ddt_psimd * deltaT;
        psimq = psimq0 + ddt_psimq * deltaT;                                
        
        
        updateIsIfPsis(deltaT, ud, uq, uf, ddt_psimd, ddt_psimq);        
        
    }
    
    static int counter = 0;                            
    
    int getDamperLeakageInductanceDIndex() {
        return DAMPER_LEAKAGE_IND_D_INDEX;
    }
    
    int getDamperLeakageInductanceQIndex() {
        return DAMPER_LEAKAGE_IND_Q_INDEX;
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
        return INIT_STATOR_CUR_A_INDEX;
    }

    @Override
    int getInitialStatorCurrentIndexB() {
        return INIT_STATOR_CUR_B_INDEX;
    }
    
    @Override
    int getInitialFieldCurrentIndex() {
        return INIT_FIELD_CUR_INDEX;
    }
    
    @Override
    int getInitialStatorFluxIndexD() {
        return INIT_STATOR_FLUX_D_INDEX;
    }

    @Override
    int getInitialStatorFluxIndexQ() {
        return INIT_STATOR_FLUX_Q_INDEX;
    }
    
    @Override
    int getSaturatedMagnetizingInductanceIndex() {
        return SAT_IND_INDEX;
    }            
}
