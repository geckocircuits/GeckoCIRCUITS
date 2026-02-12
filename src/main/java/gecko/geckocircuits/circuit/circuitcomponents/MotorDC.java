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
import gecko.geckocircuits.circuit.AbstractTerminal;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.geckocircuits.circuit.TerminalRelativePosition;
import gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

public final class MotorDC extends AbstractMotorDC { 
    private static final int YPOS_EXCITATION_1 = -2;
    private static final double MID_POSITION = 1.5;
    private static final double LARGE_RESISTOR_VALUE = 1e8;
    private static final double INITIAL_FIELD_INDUCTANCE = 0.005;
    private static final double INITIAL_FIELD_RESISTANCE = 0.02;
    private static final double INITIAL_NUMBER_WINDINGS = 100;
    
    private static final int FIELD_INDUCTANCE_INDEX = 11;
    private static final int FIELD_RESISTANCE_INDEX = 12;
    private static final int FIELD_WINDINGS_INDEX = 13;
    private static final int INITIAL_FIELD_CUR_INDEX = 19;
    private static final int MACHINE_CONSTANT_INDEX = 14;
    
    private static final int EXCITATION_CUR_INDEX = 1;
    private static final int OMEGA_INDEX = 2;
    private static final int DREHZAHL_INDEX = 3;
    private static final int PSI_INDEX = 4;
    private static final int EMK_INDEX = 5;  
    private static final int ELECTRIC_TORQUE_INDEX = 6;
    private static final int THETA_M_INDEX = 7;    
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(MotorDC.class, "M-DC", I18nKeys.DC_MACHINE);
    
    private double _fieldInductance;
    final UserParameter<Double> _fieldInductanceParameter = UserParameter.Builder.
            <Double>start("fieldInductance", INITIAL_FIELD_INDUCTANCE).
            longName(I18nKeys.FIELD_INDUCTANCE).
            shortName("Le").
            unit("H").
            arrayIndex(this, FIELD_INDUCTANCE_INDEX).
            build();
    
    private double _fieldResistance;
    final UserParameter<Double> _fieldResistanceParameter = UserParameter.Builder.
            <Double>start("fieldResistance", INITIAL_FIELD_RESISTANCE).
            longName(I18nKeys.FIELD_RESISTANCE).
            shortName("Re").
            unit("ohm").
            arrayIndex(this, FIELD_RESISTANCE_INDEX).
            build();
    
    private double _fieldWindings;
    UserParameter<Double> _fieldWindingsParameter = UserParameter.Builder.
            <Double>start("fieldWindings", INITIAL_NUMBER_WINDINGS).
            longName(I18nKeys.FIELD_WINDINGS).
            shortName("Ne").
            unit("unitless").
            arrayIndex(this, FIELD_WINDINGS_INDEX).
            build();        
    
    private double _initialFieldCurrent = 0;    
    final UserParameter<Double> _initialFieldCurrentParameter = UserParameter.Builder.
            <Double>start("initialFieldCurrent", 0.0).
            longName(I18nKeys.INITIAL_FIELD_CURRENT).
            shortName("I_Le").
            unit("A").
            arrayIndex(this, INITIAL_FIELD_CUR_INDEX).
            build();              
    
    private double _machineConstant = 1.0;    
    final UserParameter<Double> _machineConstantParam = UserParameter.Builder.
            <Double>start("machineConstant", 1.0).
            longName(I18nKeys.MACHINE_CONSTANT).
            shortName("c").
            unit("unitless").
            arrayIndex(this, MACHINE_CONSTANT_INDEX).
            build();
    
    private AbstractTerminal _internalNodeExcitation;        
    
    private InductorWOCoupling _Le;
    private AbstractResistor _Re;
    private AbstractResistor _LargeResistor;    
    
    double _flux = 0;  // Fluss    
    
    @Override
    void setSubCircuit() {        
        super.setSubCircuit();        
        // Le im Erregerstromkreis --> 
        _Le = (InductorWOCoupling) fabricHiddenSub(CircuitTyp.LK_L, this);
        // Re im Erregerstromkreis --> 
        _Re = (AbstractResistor) fabricHiddenSub(CircuitTyp.LK_R, this);
        // hochohmiger Widerstand zur Anbindung des Rotorkreises --> 
        _LargeResistor = (AbstractResistor) fabricHiddenSub(CircuitTyp.LK_R, this);                
        _Le._inductance.setValueWithoutUndo(_fieldInductance);
        _Re._resistance.setValueWithoutUndo(_fieldResistance);
        _LargeResistor._resistance.setValueWithoutUndo(LARGE_RESISTOR_VALUE);

        _Le.setInputTerminal(0, XIN.get(1));
        _Le.setOutputTerminal(0, _internalNodeExcitation);

        _Re.setInputTerminal(0, _internalNodeExcitation);
        _Re.setOutputTerminal(0, YOUT.get(1));

        _LargeResistor.setInputTerminal(0, YOUT.get(0));
        _LargeResistor.setOutputTerminal(0, YOUT.get(1));       
    }        

    @Override
    void setTerminals() {
        super.setTerminals();
        XIN.add(new TerminalRelativePosition(this, XPOS_LEFT_TERM, -YPOS_EXCITATION_1));        
        YOUT.add(new TerminalRelativePosition(this, XPOS_LEFT_TERM, 1));        
        _internalNodeExcitation = new TerminalRelativePosition(this, 1, 0);
    }                
    
    @Override
    public void setzeParameterZustandswerteAufNULL() {        
        _initialFieldCurrent = _initialFieldCurrentParameter.getValue();
        _fieldWindings = _fieldWindingsParameter.getValue();
        _machineConstant = _machineConstantParam.getValue();        
        super.setzeParameterZustandswerteAufNULL();                                        
        _fieldInductance = _fieldInductanceParameter.getValue();
        
        _Le._inductance.setValueWithoutUndo(_fieldInductance);
        _Le.parameter[1] = _initialFieldCurrent;  // iALT in Le setzen
        _Le.parameter[2] = _initialFieldCurrent;  // iALT in Le setzen        
        
        _fieldResistance = _fieldResistanceParameter.getValue();        
        _Re._resistance.setValueWithoutUndo(_fieldResistance);                                
        if(_fieldWindings <= 0) {
            throw new IllegalArgumentException("Error: number of field windings must be > 0");
        }                
        calculateEMK();                        
    }
    
    @Override
    void calculateEMK() {
        double excitationCurrent = _Le.parameter[2];
        _flux = _fieldInductance / _fieldWindings * excitationCurrent;  // Erregerfluss
        _emk = _machineConstant * _flux * _omegaElectric;  // innere Spannung der Maschine         
    }
                    
    @Override
    public List<String> getParameterStringIntern() {
        return Arrays.asList("ia [A]", "ie [A]", "omega", "n [rpm]", "phi [Vs]", "emf [V]", "Tel [Nm]", "theta [rad]");
    }                    
        
    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        super.drawConnectorLines(graphics);        
        // draw the connector lines
        graphics.drawLine(XPOS_LEFT_TERM * dpix, -dpix, -dpix, -dpix);
        graphics.drawLine(XPOS_LEFT_TERM * dpix, YPOS_EXCITATION_1 * dpix, (int) (dpix * BLOCK_SYMB_Y), YPOS_EXCITATION_1 * dpix);
        graphics.drawLine(-dpix, -dpix, - dpix, (int) (-dpix * MID_POSITION));
        graphics.drawLine( -dpix, (int) (-dpix * MID_POSITION), (int) (dpix * BLOCK_SYMB_Y), (int) (-dpix * MID_POSITION));
        
        // small rectangle for the excitation
        graphics.fillRect((int) (-dpix * BLOCK_SYMB_Y), -dpix *2, (int) (dpix * 2 * BLOCK_SYMB_Y), (int) (dpix /2.0)); 
    }                

    @Override
    protected Window openDialogWindow() {
        return new MotorDCDialog(this);
    }

    @Override
    void updateOldSolverParameters() {
        super.updateOldSolverParameters();        
        parameter[EXCITATION_CUR_INDEX] = _Le.parameter[2];        
        parameter[PSI_INDEX] = _flux;        
    }

    @Override
    double calculateElectricTorque() {
        return _machineConstant * _flux * _anchorCurrent;
    }                

    @Override
    int getEMKIndex() {
        return EMK_INDEX;
    }
    
    @Override
    int getOmegaIndex() {
        return OMEGA_INDEX;
    }
    
    @Override
    int getDrehzahlIndex() {
        return DREHZAHL_INDEX;
    }           

    @Override
    int getElectricTorqueIndex() {
        return ELECTRIC_TORQUE_INDEX;
    }            

    @Override
    int getThetaMIndex() {
        return THETA_M_INDEX;
    }                   
}
