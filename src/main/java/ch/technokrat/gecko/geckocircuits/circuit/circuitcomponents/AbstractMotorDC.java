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
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;

abstract class AbstractMotorDC extends AbstractMotor {

    static final int XPOS_LEFT_TERM = -2;
    static final int XPOS_RIGHT_TERM = 2;
    private static final int ANCHOR_CURRENT_INDEX = 0;
    private static final int ARM_INDUCTANCE_INDEX = 9;
    private static final int ARMATURE_RESISTANCE_INDEX = 10;
    private static final int FRICTION_COEFF_INDEX = 15;
    private static final int INITIAL_ROTOR_SPEED_INDEX = 20;
    private static final int INERTIA_INDEX = 16;
    private static final int LOAD_TORQUE_INDEX = 17;
    private static final int INIT_ARM_CUR_INDEX = 18;
    private static final double INITIAL_ARM_RESISTANCE = 0.01;
    private static final double INITIAL_ARM_INDUCTANCE = 0.03e-3;
    double _initialArmatureCurrent = 0;
    final UserParameter<Double> _initialArmatureCurrentParam = UserParameter.Builder.
            <Double>start("initialArmaturecurrent", 0.0).
            longName(I18nKeys.INITIAL_ARMATURE_CURRENT).
            shortName("I_La").
            unit("A").
            arrayIndex(this, INIT_ARM_CUR_INDEX).
            build();
    double _armatureInductance;
    final UserParameter<Double> _armatureInductancePar = UserParameter.Builder.
            <Double>start("armatureInductance", INITIAL_ARM_INDUCTANCE).
            longName(I18nKeys.ARMATURE_INDUCTANCE).
            shortName("La").
            unit("H").
            arrayIndex(this, ARM_INDUCTANCE_INDEX).
            build();
    double _armatureResistance;
    final UserParameter<Double> _armatureResistancePar = UserParameter.Builder.
            <Double>start("armatureResistance", INITIAL_ARM_RESISTANCE).
            longName(I18nKeys.ARMATURE_RESISTANCE).
            shortName("Ra").
            unit("ohm").
            arrayIndex(this, ARMATURE_RESISTANCE_INDEX).
            build();
    AbstractTerminal _internalNode1, _internalNode2;
    InductorWOCoupling _LAnker;
    AbstractResistor _RAnker;
    AbstractVoltageSource _uEMK;
    double _emk = 0;
    double _anchorCurrent; // anker strom               

    @Override
    void setTerminals() {
        XIN.add(new TerminalRelativePosition(this, XPOS_LEFT_TERM, 0));
        YOUT.add(new TerminalRelativePosition(this, XPOS_RIGHT_TERM, 0));
        _internalNode1 = new TerminalRelativePosition(this, -1, 0);
        _internalNode2 = new TerminalRelativePosition(this, 0, 0);
    }

    @Override
    public void setzeParameterZustandswerteAufNULL() {        
        super.setzeParameterZustandswerteAufNULL();
        _armatureInductance = _armatureInductancePar.getValue();
        _armatureResistance = _armatureResistancePar.getValue();
        _initialArmatureCurrent = _initialArmatureCurrentParam.getValue();
        _LAnker._inductance.setValueWithoutUndo(_armatureInductance);
        _RAnker._resistance.setValueWithoutUndo(_armatureResistance);
        _LAnker.parameter[1] = _initialArmatureCurrent;  // iALT in La setzen
        _LAnker.parameter[2] = _initialArmatureCurrent;  // iALT in La setzen                
    }

    @Override
    void calculateMotorEquations(final double deltaT, double time) {
        _anchorCurrent = _LAnker.parameter[2];  // Ankerstrom        
        // Motor-Gleichungen durchrechnen -->         
        calculateEMK();
    }

    @Override
    final int getInertiaIndex() {
        return INERTIA_INDEX;
    }

    @Override
    final int getInitialRotationSpeedIndex() {
        return INITIAL_ROTOR_SPEED_INDEX;
    }

    @Override
    final int getFrictionCoefficientIndex() {
        return FRICTION_COEFF_INDEX;
    }

    @Override
    int getInitialRotorPositionIndex() {
        return -1; // in Uwe's old models, the initial position was not given!!!
    }

    @Override
    public final int getIndexForLoadTorque() {
        return LOAD_TORQUE_INDEX;
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.fillRect((int) (-dpix * BLOCK_SYMB_X), (int) (-dpix * BLOCK_SYMB_Y), (int) (dpix * 2 * BLOCK_SYMB_X), (int) (dpix * 2 * BLOCK_SYMB_Y));
        // connector between Anchor clamps
        graphics.drawLine(XPOS_LEFT_TERM * dpix, 0, XPOS_RIGHT_TERM * dpix, 0);
        if (SchematischeEingabe2._lkDisplayMode.showFlowSymbol) {
            this.defineFlowSymbol(getComponentDirection(), graphics);
        }
    }    

    @Override
    void setSubCircuit() {
        // La im Ankerstromkreis --> 
        _LAnker = (InductorWOCoupling) fabricHiddenSub(CircuitTyp.LK_L, this);
        // Ra im Ankerstromkreis --> 
        _RAnker = (AbstractResistor) fabricHiddenSub(CircuitTyp.LK_R, this);
        // EMK im Ankerstromkreis --> 
        _uEMK = (AbstractVoltageSource) fabricHiddenSub(CircuitTyp.LK_U, this);

        _LAnker._inductance.setValueWithoutUndo(_armatureInductance);
        _RAnker._resistance.setValueWithoutUndo(_armatureResistance);
        _uEMK.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_DC);

        _LAnker.setInputTerminal(0, XIN.get(0));
        _LAnker.setOutputTerminal(0, _internalNode1);

        _RAnker.setInputTerminal(0, _internalNode1);
        _RAnker.setOutputTerminal(0, _internalNode2);

        _uEMK.setInputTerminal(0, _internalNode2);
        _uEMK.setOutputTerminal(0, YOUT.get(0));
    }

    @Override
    void updateOldSolverParameters() {
        super.updateOldSolverParameters();
        parameter[ANCHOR_CURRENT_INDEX] = _anchorCurrent;
        parameter[getEMKIndex()] = _emk;
    }

    abstract int getEMKIndex();

    abstract void calculateEMK();

    @Override
    void updateSourceParameters() {
        _uEMK.parameter[1] = _emk;  // DC-Wert der internen WSpg.Quelle                 
    }

    @Override
    int getPolePairIndex() {
        return -1; // old versions don't have a polepair-number
    }

    private void defineFlowSymbol(ComponentDirection componentDirection, Graphics2D g) {
        Color origColor = g.getColor();
        g.setColor(Color.magenta);
        int[] xFl = new int[3];
        int[] yFl = new int[3];
        xFl[0] = (int) (dpix * 2) - 2;
        xFl[1] = xFl[0] - ARROW_LENGTH;
        xFl[2] = xFl[1];
        yFl[0] = 0;
        yFl[1] = yFl[0] - ARROW_WIDTH;
        yFl[2] = yFl[0] + ARROW_WIDTH;

        g.drawPolygon(xFl, yFl, 3);
        g.setColor(origColor);
    }
}
