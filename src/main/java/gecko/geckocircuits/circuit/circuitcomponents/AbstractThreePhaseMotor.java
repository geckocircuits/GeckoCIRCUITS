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
import gecko.geckocircuits.circuit.CircuitSourceType;
import gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;

abstract class AbstractThreePhaseMotor extends AbstractMotor {
    private static final int THETA_M_INDEX = 5;
    private static final int ELECTRIC_TORQUE_INDEX = 6;        

    double _statorResistance;
    final UserParameter<Double> _statorResistancePar = UserParameter.Builder.
            <Double>start("statorResistance", 0.5).
            longName(I18nKeys.STATOR_RESISTANCE).
            shortName("Rs").
            unit("ohm").
            arrayIndex(this, getStatorResistanceIndex()).
            build();
    
    final UserParameter<Double> initialStatorCurrentA = UserParameter.Builder.
            <Double>start("initialStatorCurrentA", 0.0).
            longName(I18nKeys.INITIAL_STATOR_CURRENT_A).
            shortName("i_sa").
            unit("A").
            arrayIndex(this, getInitialStatorCurrentIndexA()).
            build();
    
    final UserParameter<Double> initialStatorCurrentB = UserParameter.Builder.
            <Double>start("initialStatorCurrentB", 0.0).
            longName(I18nKeys.INITIAL_STATOR_CURRENT_B).
            shortName("i_sb").
            unit("A").
            arrayIndex(this, getInitialStatorCurrentIndexB()).
            build();
    
    AbstractCurrentSource _controlledAnchorSourceA;
    AbstractCurrentSource _controlledAnchorSourceC;        
    
    double isa, isb, isc, isa0, isb0;  // Anchor-Currents    
    double isd, isd0, isq, isq0;

    double psisd, psisq;
    double psisd0 = 0, psisq0 = 0;
    
    abstract int getStatorResistanceIndex();            

    
    @Override
    public void setzeParameterZustandswerteAufNULL() {
        super.setzeParameterZustandswerteAufNULL();        
        isa = isa0 = initialStatorCurrentA.getValue();
        isb = isb0 = initialStatorCurrentB.getValue();
        _statorResistance = _statorResistancePar.getValue();        
    }

    @Override
    void setSubCircuit() {
        // Statorstromquelle fuer isa(t) --> 
        _controlledAnchorSourceA = (AbstractCurrentSource) fabricHiddenSub(CircuitTyp.LK_I, this);
        _controlledAnchorSourceA.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_SIGNALGESTEUERT);
        
        _controlledAnchorSourceA.setInputTerminal(0, XIN.get(0));
        _controlledAnchorSourceA.setOutputTerminal(0, XIN.get(1));
        
        // Statorstromquelle fuer isc(t) --> 
        _controlledAnchorSourceC = (AbstractCurrentSource) fabricHiddenSub(CircuitTyp.LK_I, this);                    
        _controlledAnchorSourceC.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_SIGNALGESTEUERT);                
        _controlledAnchorSourceC.setInputTerminal(0, XIN.get(2));
        _controlledAnchorSourceC.setOutputTerminal(0, XIN.get(1));
    }        
    
    @Override
    void updateSourceParameters() {                        
        isa = isd * Math.cos(_thetaElectric) - isq * Math.sin(_thetaElectric);        
        isc = isd * Math.cos(_thetaElectric + 2 * Math.PI / 3) - isq * Math.sin(_thetaElectric + 2 * Math.PI / 3);        
        isb = -(isa + isc);
       
        _controlledAnchorSourceA.parameter[1] = isa;
        _controlledAnchorSourceC.parameter[1] = isc;                
    }        
    
    
    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        drawLeftUpperTerminalLine(graphics);        
        drawLeftMidTerminalLine(graphics);                        
        drawLeftLowerTerminalLine(graphics);            
    }
    
    void drawLeftUpperTerminalLine(final Graphics2D graphics) {                        
        graphics.drawLine(-dpix, -dpix, -2 * dpix, -dpix);        
        graphics.drawLine(-dpix, -dpix, 0, 0);
    }

    void drawLeftMidTerminalLine(Graphics2D graphics) {                        
        graphics.drawLine(-dpix, 0, -2*dpix, 0);
        graphics.drawLine(-dpix, 0, 0, 0);
    }

    void drawLeftLowerTerminalLine(Graphics2D graphics) {
        graphics.drawLine(-dpix, dpix, -2 * dpix, dpix);        
        graphics.drawLine(-dpix, dpix, 0, 0);
        
    }
    
    void drawRightUpperTerminalLine(final Graphics2D graphics) {                        
        graphics.drawLine(dpix, -dpix, 0, 0);
        graphics.drawLine(dpix, -dpix, 2 * dpix, -dpix);        
    }

    void drawRightMidTerminalLine(final Graphics2D graphics) {                
        graphics.drawLine(dpix, 0, 2*dpix, 0);
        graphics.drawLine(dpix, 0, 0, 0);
    }

    void drawRightLowerTerminalLine(final Graphics2D graphics) {
        graphics.drawLine(dpix, dpix, 2 * dpix, dpix);
        graphics.drawLine(dpix, dpix, 0, 0);
        
    }
    
    abstract int getInitialStatorCurrentIndexA();
    abstract int getInitialStatorCurrentIndexB();
    
    void updateHistoryVariables() {        
        super.updateHistoryVariables();
        isd0 = isd;
        isq0 = isq;        
    }

    @Override
    void updateOldSolverParameters() {
        super.updateOldSolverParameters();        
        parameter[0] = isa;
        parameter[1] = isb;
        parameter[2] = isc;
    }

    public static int getTHETA_M_INDEX() {
        return THETA_M_INDEX;
    }
    
    
    
    @Override
    final int getOmegaIndex() {
        return 3;
    }    
    
    @Override
    final int getDrehzahlIndex() {
        return 4;
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
