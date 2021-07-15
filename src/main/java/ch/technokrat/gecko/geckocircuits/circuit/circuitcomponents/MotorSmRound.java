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

public final class MotorSmRound extends AbstractMotorSM {

    private static final int SAT_MAG_INDUCTANCE_INDEX = 20;
    private static final int DAMPER_RES_INDEX = 27;
    private static final int DAMPER_LEAKAGE_IND_D_IND = 28;
    private static final int DAMPER_LEAKAGE_IND_Q_IND = 29;
    private static final int DAMPER_LEAKAGE_IND_Q2 = 30;
    private static final int INIT_ROT_SPEED_INDEX = 31;
    private static final int INIT_ROT_POS_INDEX = 32;
    private static final int INIT_STAT_CUR_INDEX = 33;
    private static final int INIT_STAT_CUR_INDEX_B = 34;
    private static final int INIT_FIELD_CUR_INDEX = 35;
    private static final int INIT_DAMP_CURRENT_INDEX = 36;
    private static final int INIT_STAT_FLUX_D_INDEX = 37;
    private static final int INIT_STAT_FLUX_Q_INDEX = 38;
    private static final double INIT_DAMPER_RES_Q2_VALUE = 0.2;
    private static final double INIT_LEAKAGE_INDUCTANCE_Q2 = 0.002;
    public static AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(MotorSmRound.class, "SM-RO", I18nKeys.SM_RO, I18nKeys.ROUND_ROTOR_SYNCHRONOUS_MACHINE);
    private double _damperResistanceQ2;
    
    final UserParameter<Double> _damperResistanceQ2_par = UserParameter.Builder.
            <Double>start("damperResistanceQ2", INIT_DAMPER_RES_Q2_VALUE).
            longName(I18nKeys.DAMPER_RESISTANCE).
            shortName("Rdamp_q2").
            unit("ohm").
            arrayIndex(this, DAMPER_RES_INDEX).
            build();

    private double _damperLeakageIndQ2;
    final UserParameter<Double> _damperLeakageIndQ2Par = UserParameter.Builder.
            <Double>start("damperLeakageInductanceQ2", INIT_LEAKAGE_INDUCTANCE_Q2).
            longName(I18nKeys.DAMPER_LEAKAGE_INDUCTANCE_Q).
            shortName("Ldamp_leak_q2").
            unit("H").
            arrayIndex(this, DAMPER_LEAKAGE_IND_Q2).
            build();
    private double _ikq1 = 0, _ikq1Old = 0;
    final UserParameter<Double> _initDampCurrentPar = UserParameter.Builder.
            <Double>start("initialDamperCurrent", 0.0).
            longName(I18nKeys.INITIAL_DAMPER_CURRENT).
            shortName("ikq1").
            unit("A").
            arrayIndex(this, INIT_DAMP_CURRENT_INDEX).
            build();

    // Initialisiereung nach INIT&START --> 
    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();
        _damperResistanceQ2 = _damperResistanceQ2_par.getValue();
        _damperLeakageIndQ2 = _damperLeakageIndQ2Par.getValue();
        _ikq1Old = _initDampCurrentPar.getValue();
        _ikq1 = _ikq1Old;
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
        double phimX1 = Math.sqrt(psimd * psimd + psimq * psimq), phimX2 = psiT / 1e3;
        double phim = (phimX1 > phimX2) ? phimX1 : phimX2;
        double inv_Lm = (Mf - Mi) / Math.PI * (((phim - psiT) * Math.atan(tauT * (phim - psiT)) - psiT * Math.atan(tauT * psiT))
                + 0.5 / tauT * (Math.log(1 + (tauT * psiT) * (tauT * psiT)) - Math.log(1 + tauT * tauT * (phim - psiT) * (phim - psiT)))) / phim
                + 0.5 * (Mf + Mi);

        double ddt_psimd = (isd * (Rkd / Llkd - _statorResistance / Lls)
                + isq * _omegaElectric + ifd * (Rkd / Llkd - Rf / Llf)
                - Rkd * inv_Lm / Llkd * psimd + _omegaElectric / Lls * psimq
                + ud / Lls + uf / Llf) / (inv_Lm + 1 / Lls + 1 / Llf + 1 / Llkd);
        double ddt_psimq = (-_omegaElectric * isd + isq * (_damperResistanceQ2 / _damperLeakageIndQ2 - _statorResistance / Lls) + (_damperResistanceQ2 / _damperLeakageIndQ2 - Rkq / Llkq1) * _ikq1
                - _omegaElectric / Lls * psimd - _damperResistanceQ2 * inv_Lm / _damperLeakageIndQ2 * psimq + uq / Lls) / (inv_Lm + 1 / Lls + 1 / Llkq1 + 1 / _damperLeakageIndQ2);
        
        psimd = psimd0 + ddt_psimd * deltaT;
        psimq = psimq0 + ddt_psimq * deltaT;
        
        double ddt_ikq1 = (-Rkq * _ikq1 - ddt_psimq) / Llkq1;
        _ikq1 = _ikq1Old + ddt_ikq1 * deltaT;

        updateIsIfPsis(deltaT, ud, uq, uf, ddt_psimd, ddt_psimq);
    }

    @Override
    void updateHistoryVariables() {
        super.updateHistoryVariables();
        _ikq1Old = _ikq1;
    }            

    @Override
    int getSaturatedMagnetizingInductanceIndex() {
        return SAT_MAG_INDUCTANCE_INDEX;
    }

    @Override
    protected Window openDialogWindow() {
        return new MotorSmRoundDialog(this);
    }

    @Override
    int getDamperLeakageInductanceDIndex() {
        return DAMPER_LEAKAGE_IND_D_IND;
    }

    @Override
    int getDamperLeakageInductanceQIndex() {
        return DAMPER_LEAKAGE_IND_Q_IND;
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
        return INIT_STAT_CUR_INDEX;
    }

    @Override
    int getInitialStatorCurrentIndexB() {
        return INIT_STAT_CUR_INDEX_B;
    }

    @Override
    int getInitialFieldCurrentIndex() {
        return INIT_FIELD_CUR_INDEX;
    }

    @Override
    int getInitialStatorFluxIndexD() {
        return INIT_STAT_FLUX_D_INDEX;
    }

    @Override
    int getInitialStatorFluxIndexQ() {
        return INIT_STAT_FLUX_Q_INDEX;
    }
}
