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
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativeFixedDirection;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

abstract class AbstractMotorSM extends AbstractThreePhaseMotor {

    AbstractCurrentSource _controlledSource3;
    AbstractResistor _resistor;
    
    double Lls;
    final UserParameter<Double> statorLeakageInductance = UserParameter.Builder.
            <Double>start("statorLeakageInductance", 0.0010).
            longName(I18nKeys.STATOR_LEAKAGE_INDUCTANCE).
            shortName("Ls_leak").
            unit("H").
            arrayIndex(this, 18).
            build();
    double psiT;
    final UserParameter<Double> fluxAtSaturationTransition = UserParameter.Builder.
            <Double>start("fluxAtSaturationTransition", 0.25).
            longName(I18nKeys.FLUX_AT_SATURATION_TRANSITION).
            shortName("flux_st").
            unit("H").
            arrayIndex(this, 21).
            build();
    double fT;
    final UserParameter<Double> tightnessOfSaturationTransition = UserParameter.Builder.
            <Double>start("tightnessOfSaturationTransition", 0.9).
            longName(I18nKeys.TIGHTNESS_OF_SATURATION_TRANSITION).
            shortName("t_st").
            unit("unitless").
            arrayIndex(this, 22).
            build();
    double Rf;
    final UserParameter<Double> fieldResistance = UserParameter.Builder.
            <Double>start("fieldResistance", 0.1).
            longName(I18nKeys.FIELD_RESISTANCE).
            shortName("Rf").
            unit("ohm").
            arrayIndex(this, 23).
            build();
    double Llf;
    final UserParameter<Double> fieldLeakageInductance = UserParameter.Builder.
            <Double>start("fieldLeakageInductance", 5.0E-4).
            longName(I18nKeys.FIELD_LEAKAGE_INDUCTANCE).
            shortName("Lf_leak").
            unit("H").
            arrayIndex(this, 24).
            build();
    double Rkd;
    final UserParameter<Double> damperResistanceD = UserParameter.Builder.
            <Double>start("damperResistanceD", 0.1).
            longName(I18nKeys.DAMPER_RESISTANCE).
            shortName("Rdamp_d").
            unit("ohm").
            arrayIndex(this, 25).
            build();
    final UserParameter<Double> initialStatorFluxD = UserParameter.Builder.
            <Double>start("initialStatorFluxD", 0.0).
            longName(I18nKeys.INITIAL_STATOR_FLUX_D).
            shortName("fluxS_d").
            unit("A").
            arrayIndex(this, getInitialStatorFluxIndexD()).
            build();
    final UserParameter<Double> initialStatorFluxQ = UserParameter.Builder.
            <Double>start("initialStatorFluxQ", 0.0).
            longName(I18nKeys.INITIAL_STATOR_FLUX_Q).
            shortName("fluxS_q").
            unit("A").
            arrayIndex(this, getInitialStatorFluxIndexQ()).
            build();
    double ifd, ifd0;
    final UserParameter<Double> initialFieldCurrent = UserParameter.Builder.
            <Double>start("initialFieldCurrent", 0.0).
            longName(I18nKeys.INITIAL_FIELD_CURRENT).
            shortName("if").
            unit("A").
            arrayIndex(this, getInitialFieldCurrentIndex()).
            build();
    
    double Llkd;
    final UserParameter<Double> damperLeakageInductanceD = UserParameter.Builder.
            <Double>start("damperLeakageInductanceD", 0.0015).
            longName(I18nKeys.DAMPER_LEAKAGE_INDUCTANCE_D).
            shortName("Ldamp_leak_d").
            unit("H").
            arrayIndex(this, getDamperLeakageInductanceDIndex()).
            build();
    double Llkq1;
    final UserParameter<Double> damperLeakageInductanceQ = UserParameter.Builder.
            <Double>start("damperLeakageInductanceQ", 0.0030).
            longName(I18nKeys.DAMPER_LEAKAGE_INDUCTANCE_Q).
            shortName("Ldamp_leak_q").
            unit("H").
            arrayIndex(this, getDamperLeakageInductanceQIndex()).
            build();
    double Rkq;
    public final UserParameter<Double> damperResistanceQ = UserParameter.Builder.
            <Double>start("damperResistanceQ", 0.2).
            longName(I18nKeys.DAMPER_RESISTANCE).
            shortName("Rdamp_q").
            unit("ohm").
            arrayIndex(this, 26).
            build();
    double Lmd0;
    public final UserParameter<Double> unsaturatedMagnetizingInductanceD = UserParameter.Builder.
            <Double>start("unsaturatedMagnetizingInductance", 0.01).
            longName(I18nKeys.UNSATURATED_MAGNETIZING_INDUCTANCE).
            shortName("Lm0").
            unit("H").
            arrayIndex(this, 19).
            build();
    double Lmdsat;
    public final UserParameter<Double> saturatedMagnetizingInductance = UserParameter.Builder.
            <Double>start("saturatedMagnetizingInductance", 0.0020).
            longName(I18nKeys.SATURATED_MAGNETIZING_INDUCTANCE).
            shortName("Lm_sat").
            unit("H").
            arrayIndex(this, getSaturatedMagnetizingInductanceIndex()).
            build();
            
    double psimd0 = 0, psimq0 = 0, psimd = 0, psimq = 0;
    // weitere interne Parameter: 
    double Mf, Mi, tauT;

    @Override
    final void setTerminals() {
        XIN.add(new TerminalRelativePosition(this, -2, 1));
        XIN.add(new TerminalRelativePosition(this, -2, 0));
        XIN.add(new TerminalRelativePosition(this, -2, -1));

        YOUT.add(new TerminalRelativePosition(this, 2, 1));
        YOUT.add(new TerminalRelativePosition(this, 2, -1));
    }

    @Override
    void setSubCircuit() {
        super.setSubCircuit();
        // Erregerstromquelle fuer ifx(t) -->
        _controlledSource3 = (AbstractCurrentSource) fabricHiddenSub(CircuitTyp.LK_I, this);
        // hochohmiger Widerstand zur Anbindung des Rotorkreises --> 
        _resistor = (AbstractResistor) fabricHiddenSub(CircuitTyp.LK_R, this);

        _controlledSource3.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_SIGNALGESTEUERT);
        _resistor._resistance.setValueWithoutUndo(1e8);
        
        _controlledSource3.setInputTerminal(0, YOUT.get(0));
        _controlledSource3.setOutputTerminal(0, YOUT.get(1));

        _resistor.setInputTerminal(0, XIN.get(1));
        _resistor.setOutputTerminal(0, YOUT.get(1));
    }

    @Override
    int getInertiaIndex() {
        return 14;
    }

    @Override
    int getFrictionCoefficientIndex() {
        return 13;
    }

    @Override
    int getPolePairIndex() {
        return 16;
    }

    @Override
    int getStatorResistanceIndex() {
        return 17;
    }

    public final int getIndexForLoadTorque() {
        return 15;
    }
    
    abstract int getInitialStatorFluxIndexD();

    abstract int getInitialStatorFluxIndexQ();

    abstract int getInitialFieldCurrentIndex();

    abstract int getDamperLeakageInductanceDIndex();

    abstract int getDamperLeakageInductanceQIndex();

    abstract int getSaturatedMagnetizingInductanceIndex();

    @Override
    final double calculateElectricTorque() {
        return 1.5 * (isq * psisd - isd * psisq);
    }

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();        
        Rkd = damperResistanceD.getValue();
        Rkq = damperResistanceQ.getValue();
        Llkd = damperLeakageInductanceD.getValue();
        Llkq1 = damperLeakageInductanceQ.getValue();
        Llf = fieldLeakageInductance.getValue();
        
        isa = isa0 = initialStatorCurrentA.getValue();
        isb = isb0 = initialStatorCurrentB.getValue();

        ifd = ifd0 = initialFieldCurrent.getValue();

        psisd = psisd0 = initialStatorFluxD.getValue();
        psisq = psisq0 = initialStatorFluxQ.getValue();

        Rf = fieldResistance.getValue();
        Lls = statorLeakageInductance.getValue();
        Lmdsat = saturatedMagnetizingInductance.getValue();
        Mf = 1.0 / Lmdsat;
        Lmd0 = unsaturatedMagnetizingInductanceD.getValue();
        fT = tightnessOfSaturationTransition.getValue();
        psiT = fluxAtSaturationTransition.getValue();
        Llf = fieldLeakageInductance.getValue();
        psimd = psimd0 = psisd0 - Lls * isd0;
        psimq = psimq0 = psisq0 - Lls * isq0;

        isd = isd0 = 2.0 / 3.0 * (isa0 * Math.cos(_thetaElectric) + isb0 * Math.cos(_thetaElectric - 2.0 / 3 * Math.PI) - (isa0 + isb0) * Math.cos(_thetaElectric + 2.0 / 3 * Math.PI));
        isq = isq0 = -2.0 / 3.0 * (isa0 * Math.sin(_thetaElectric) + isb0 * Math.sin(_thetaElectric - 2.0 / 3.0 * Math.PI) - (isa0 + isb0) * Math.sin(_thetaElectric + 2.0 / 3.0 * Math.PI));

        Mi = (1.0 / Lmd0 - 1.0 / Lmdsat * (0.5 - Math.atan(tauT * psiT) / Math.PI)) / (0.5 + Math.atan(tauT * psiT) / Math.PI);
        tauT = fT / psiT * Lmd0 / Lmdsat;
        _controlledAnchorSourceA.parameter[1] = isa0;
        _controlledAnchorSourceC.parameter[1] = -(isa0 + isb0);
        _controlledSource3.parameter[1] = ifd0;        
    }        
    
    final void updateSourceParameters() {
        super.updateSourceParameters();
        _controlledSource3.parameter[1] = ifd;   
    }

    void updateHistoryVariables() {
        super.updateHistoryVariables();
        psimd0 = psimd;
        psimq0 = psimq;
        ifd0 = ifd;        
    }            

    void updateIsIfPsis(double deltaT, double ud, double uq, double uf, double ddt_psimd, double ddt_psimq) {                
        double ddt_isd = (-_statorResistance * isd + Lls * _omegaElectric * isq + _omegaElectric * psimq + ud - ddt_psimd) / Lls;                                        
        double ddt_isq = (-Lls * _omegaElectric * isd - _statorResistance * isq - psimd * _omegaElectric + uq - ddt_psimq) / Lls;
        double ddt_ifd = (-Rf * ifd + uf - ddt_psimd) / Llf;
        isd = isd0 + ddt_isd * deltaT;
                
                
        
        isq = isq0 + ddt_isq * deltaT;
        ifd = ifd0 + ddt_ifd * deltaT;
        psisd = Lls * isd + psimd;
        psisq = Lls * isq + psimq;                                                        
    }
    

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        super.drawConnectorLines(graphics);
        super.drawRightLowerTerminalLine(graphics);
        super.drawRightUpperTerminalLine(graphics);
    }

    @Override
    void updateOldSolverParameters() {
        super.updateOldSolverParameters();        
        parameter[7] = ifd;
        parameter[8] = psisd;
        parameter[9] = psisq;
    }    
    

    @Override
    public final List<String> getParameterStringIntern() {
        return Arrays.asList("isa [A]", "isb [A]", "isc [A]", "omega", "n [rpm]",
                "theta [rad]", "Tel [Nm]");
    }    
    
    

    double calculate_ud(final double uab, final double ubc) {
        return (2 * uab * Math.cos(_thetaElectric) + ubc * (Math.cos(_thetaElectric) + Math.sqrt(3) * Math.sin(_thetaElectric))) / 3;
    }

    double calculate_uq(final double uab, final double ubc) {
        return -(2 * uab * Math.sin(_thetaElectric) + ubc * (Math.sin(_thetaElectric) - Math.sqrt(3) * Math.cos(_thetaElectric))) / 3;        
    }
}
