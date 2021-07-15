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
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

// Saettigbare Asynchronmaschine 
public final class MotorImSat extends AbstractMotorIMCommon {
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(MotorImSat.class, "IM-SAT", I18nKeys.IM_SAT, I18nKeys.SATURABLE_INDUCTION_MACHINE);

    final UserParameter<Double> _unsaturatedMagnetizingInductance = UserParameter.Builder.
            <Double>start("unsaturatedMagnetizingInductance", 0.05).
            longName(I18nKeys.UNSATURATED_MAGNETIZING_INDUCTANCE).
            shortName("Lm0").
            unit("H").
            arrayIndex(this, 21).
            build();
    final UserParameter<Double> _saturatedMagnetizingInductance = UserParameter.Builder.
            <Double>start("saturatedMagnetizingInductance", 0.05).
            longName(I18nKeys.SATURATED_MAGNETIZING_INDUCTANCE).
            shortName("Lm_sat").
            unit("H").
            arrayIndex(this, 22).
            build();
    final UserParameter<Double> _fluxSaturationTransition = UserParameter.Builder.
            <Double>start("fluxatSaturationTransition", 0.25).
            longName(I18nKeys.FLUX_AT_SATURATION_TRANSITION).
            shortName("flux_st").
            unit("H").
            arrayIndex(this, 23).
            build();
    final UserParameter<Double> _tightnessOfSaturationTransition = UserParameter.Builder.
            <Double>start("tightnessofSaturationTransition", 0.9).
            longName(I18nKeys.TIGHTNESS_OF_SATURATION_TRANSITION).
            shortName("t_st").
            unit("unitless").
            arrayIndex(this, 24).
            build();
    private int drMpix = 3;
    private double Lls = 1e-3;
    private double Rr = 1.0, Llr = 1.8e-3, Lm0 = 50e-3, Lmsat = 50e-3, psiT = 0.25, fT = 0.9;
    // interne Variablen: 
    private double isd = 0, isd0 = 0, isq = 0, isq0 = 0;
    private double Mf, Mi, tauT, psimd, psimq;
    private double psimd0 = 0, psimq0 = 0;
    private double ira = 0, irb = 0, ira0 = 0, irb0 = 0;  // Rotor-Phasenstroeme
    private AbstractCurrentSource _controlledSource3;
    private AbstractCurrentSource _controlledSource4;
    private AbstractResistor _resistor;

    @Override
    void setTerminals() {

        XIN.add(new TerminalRelativePosition(this, -2, 1));
        XIN.add(new TerminalRelativePosition(this, -2, 0));
        XIN.add(new TerminalRelativePosition(this, -2, -1));

        YOUT.add(new TerminalRelativePosition(this, 2, 1));
        YOUT.add(new TerminalRelativePosition(this, 2, 0));
        YOUT.add(new TerminalRelativePosition(this, 2, -1));
    }

    @Override
    void setSubCircuit() {
        super.setSubCircuit();
        // Eingangsstromquelle fuer iax(t) --> 
        _controlledSource3 = (AbstractCurrentSource) fabricHiddenSub(CircuitTyp.LK_I, this);
        // Eingangsstromquelle fuer icx(t) --> 
        _controlledSource4 = (AbstractCurrentSource) fabricHiddenSub(CircuitTyp.LK_I, this);
        // hochohmiger Widerstand zur Anbindung des Rotorkreises --> 
        _resistor = (AbstractResistor) fabricHiddenSub(CircuitTyp.LK_R, this);


        _controlledSource3.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_SIGNALGESTEUERT);
        _controlledSource4.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_SIGNALGESTEUERT);
        _resistor._resistance.setValueWithoutUndo(1e8);

        _controlledSource3.setInputTerminal(0, YOUT.get(0));
        _controlledSource3.setOutputTerminal(0, YOUT.get(1));

        _controlledSource4.setInputTerminal(0, YOUT.get(2));
        _controlledSource4.setOutputTerminal(0, YOUT.get(1));

        _resistor.setInputTerminal(0, XIN.get(1));
        _resistor.setOutputTerminal(0, YOUT.get(1));
    }

    public int getIndexForLoadTorque() {
        return 15;
    }

    // Initialisiereung nach INIT&START --> 
    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();
        _statorResistance = parameter[17];
        Lls = parameter[18];
        Rr = parameter[19];
        Llr = parameter[20];
        Lm0 = parameter[21];
        Lmsat = parameter[22];
        psiT = parameter[23];
        fT = parameter[24];

        isa0 = parameter[25];
        isb0 = parameter[26];
        //
        tauT = fT / psiT * Lm0 / Lmsat;
        Mf = 1 / Lmsat;
        Mi = (1 / Lm0 - 1 / Lmsat * (0.5 - Math.atan(tauT * psiT) / Math.PI)) / (0.5 + Math.atan(tauT * psiT) / Math.PI);
        isd0 = isa0;
        isq0 = 1 / Math.sqrt(3) * (isa0 + 2 * isb0);
        psimd0 = psisd0 - Lls * isd0;
        psimq0 = psisq0 - Lls * isq0;
        ira0 = 0;
        irb0 = 0;
        //
        _controlledAnchorSourceA.parameter[1] = isa0;
        _controlledAnchorSourceC.parameter[1] = -(isa0 + isb0);
        _controlledSource3.parameter[1] = ira0;
        _controlledSource4.parameter[1] = -(ira0 + irb0);

    }

    @Override
    public List<String> getParameterStringIntern() {
        return Arrays.asList("isa [A]", "isb [A]", "isc [A]", "omega", "n [rpm]", "theta [rad]", "Tel [Nm]");
    }

    @Override
    void calculateMotorEquations(final double deltaT, double time) {
        double usab = _controlledAnchorSourceA.parameter[7];
        double usbc = -_controlledAnchorSourceC.parameter[7];

        double urab = _controlledSource3.parameter[7];
        double urbc = -_controlledSource4.parameter[7];
        //------
        // Block 'usab,usbc -> udq': 
        double usd = 2 * usab / 3 + usbc / 3;
        double usq = usbc / Math.sqrt(3);
        double psim = Math.sqrt(psimd * psimd + psimq * psimq);
        if (psim < 0.001 * psiT) {
            psim = 0.001 * psiT;
        }
        double inv_Lm = (Mf - Mi) / Math.PI * ((psim - psiT) * Math.atan(tauT * (psim - psiT)) - psiT * Math.atan(tauT * psiT)
                + 0.5 / tauT * (Math.log(1 + (tauT * psiT) * (tauT * psiT)) - Math.log(1 + tauT * tauT * (psim - psiT) * (psim - psiT)))) / psim + 0.5 * (Mf + Mi);
        // Block 'vrdq': 
        double urd = 2.0 / 3.0 * (urab * Math.cos(_thetaElectric) - urbc * Math.cos(_thetaElectric - 2 * Math.PI / 3));
        double urq = 2.0 / 3.0 * (urab * Math.sin(_thetaElectric) - urbc * Math.sin(_thetaElectric - 2 * Math.PI / 3));
        // Block 'isdq,psimdq': 
        double ddt_psimd = ((Rr / Llr - _statorResistance / Lls) * isd + isq * _omegaElectric - Rr * inv_Lm / Llr * psimd - _omegaElectric * psimq * (inv_Lm + 1 / Llr)
                + usd / Lls + urd / Llr) / (inv_Lm + 1 / Lls + 1 / Llr);
        double ddt_psimq = (-_omegaElectric * isd + (Rr / Llr - _statorResistance / Lls) * isq + _omegaElectric * (inv_Lm + 1 / Llr) * psimd - Rr * inv_Lm / Llr * psimq
                + usq / Lls + urq / Llr) / (inv_Lm + 1 / Lls + 1 / Llr);
        double ddt_isd = (-_statorResistance * isd + usd - ddt_psimd) / Lls;
        double ddt_isq = (-_statorResistance * isq + usq - ddt_psimq) / Lls;
        psimd = psimd0 + ddt_psimd * deltaT;
        psimq = psimq0 + ddt_psimq * deltaT;
        isd = isd0 + ddt_isd * deltaT;
        isq = isq0 + ddt_isq * deltaT;
        // Block 'psisdq': 
        psisd = Lls * isd + psimd;
        psisq = Lls * isq + psimq;
        // Block 'irdq':
        double ird = inv_Lm * psimd - isd;
        double irq = inv_Lm * psimq - isq;
        // Block 'psirdq' --> nicht weiter verwendet? 
        double psird = Llr * ird + psimd;  // nicht weiter verwendet 
        double psirq = Llr * irq + psimq;  // nicht weiter verwendet 
        // Block 'ira,irb': 
        ira = ird * Math.cos(_thetaElectric) + irq * Math.sin(_thetaElectric);
        irb = ird * Math.cos(_thetaElectric + 2 * Math.PI / 3) + irq * Math.sin(_thetaElectric + 2 * Math.PI / 3);
    }

    @Override
    void updateSourceParameters() {
        // Block 'isa,isb': 
        isa = isd;
        isb = 0.5 * (-isd + Math.sqrt(3) * isq);
        isc = 0.5 * (-isd - Math.sqrt(3) * isq);
        _controlledAnchorSourceA.parameter[1] = isa;
        _controlledAnchorSourceC.parameter[1] = -(isa + isb);  // isc
        _controlledSource3.parameter[1] = ira;
        _controlledSource4.parameter[1] = -(ira + irb);  // irc         
    }

    @Override
    void updateHistoryVariables() {
        super.updateHistoryVariables();
        isd0 = isd;
        isq0 = isq;
        psimd0 = psimd;
        psimq0 = psimq;
    }

    @Override
    void updateOldSolverParameters() {
        super.updateOldSolverParameters();
        parameter[7] = psisd;
        parameter[8] = psisq;
    }

    @Override
    protected void drawOnTop(final Graphics2D graphics) {
        // Saturation-Symbol: 
        graphics.drawOval((int) (-dpix * RADIUS_MOTOR_SYMBOL) + drMpix, (int) (-dpix * RADIUS_MOTOR_SYMBOL) + drMpix,
                (int) (dpix * 2 * RADIUS_MOTOR_SYMBOL) - 2 * drMpix, (int) (dpix * 2 * RADIUS_MOTOR_SYMBOL) - 2 * drMpix);
        double h1 = 0.1, h2 = 0.3, h3 = 0.4;
        graphics.drawLine((int) (dpix * (-h1 - h2)), (int) (dpix * h3), (int) (-dpix * h1), (int) (dpix * h3));
        graphics.drawLine((int) (dpix * (h1 + h2)), (int) (-dpix * h3), (int) (dpix * h1), (int) (-dpix * h3));
        graphics.drawLine((int) (-dpix * h1), (int) (dpix * h3), (int) (dpix * h1), (int) (-dpix * h3));
    }    

    @Override
    protected Window openDialogWindow() {
        return new MotorImSatDialog(this);
    }

    @Override // im-sat
    final double calculateElectricTorque() {
        return 1.5 * (isq * psisd - isd * psisq);
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        super.drawConnectorLines(graphics);
        drawRightUpperTerminalLine(graphics);
        drawRightMidTerminalLine(graphics);
        drawRightLowerTerminalLine(graphics);
    }

    @Override
    int getInitialRotationSpeedIndex() {
        return 27;
    }

    @Override
    int getInitialRotorPositionIndex() {
        return 28;
    }

    @Override
    int getInitialStatorCurrentIndexA() {
        return 25;
    }

    @Override
    int getInitialStatorCurrentIndexB() {
        return 26;
    }

    @Override
    int getInitialStatorFluxIndexD() {
        return 29;
    }

    @Override
    int getInitialStatorFluxIndexQ() {
        return 30;
    }        
}
