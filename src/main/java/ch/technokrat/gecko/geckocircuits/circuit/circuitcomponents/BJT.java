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
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalHiddenSubcircuit;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// Leistungskreis Idealer Schalter (hoch- oder niederohmiger Widerstand, daher bidirektional)
// BJT is NOT an AbstractSwitch, since it does not connect to a "control gate"!
public final class BJT extends AbstractTwoPortLKreisBlock implements HiddenSubCircuitable {
    private static final double HEIGHT = 0.8;
    
    private static final double DEF_FORWARD_BETA = 100;
    private static final double DEF_BACKWARD_BETA = 60;
    private static final double DEF_BASE_RES = 0.1;
    private static final double DEF_EM_RES = 0.01;
    private static final double DEF_COL_RES = 0.01;
    private static final double DEF_uF = 0.6;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(BJT.class, "BJT", I18nKeys.BIPOLAR_TRANSISTOR);
    
    
    final UserParameter<Double> _forwardBeta = UserParameter.Builder.
            <Double>start("forwardBeta", DEF_FORWARD_BETA).                       
            longName(I18nKeys.FORWARD_AMP_FACTOR).
            shortName("beta1").            
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();                                    
    
    final UserParameter<Double> _backwardBeta = UserParameter.Builder.
            <Double>start("backwardBeta", DEF_BACKWARD_BETA).                       
            longName(I18nKeys.BACKWARD_AMP_FACTOR).
            shortName("beta2").            
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 1).
            build();                                    
    
    final UserParameter<Double> _baseResistance = UserParameter.Builder.
            <Double>start("baseResistance", DEF_BASE_RES).                       
            longName(I18nKeys.INTERNAL_BASE_RES).
            shortName("rB").
            unit("ohm").            
            arrayIndex(this, 2).
            build();                                    

    final UserParameter<Double> _emitterResistance = UserParameter.Builder.
            <Double>start("emitterResistance", DEF_EM_RES).                       
            longName(I18nKeys.INTERNAL_EM_RES).
            shortName("rE").
            unit("ohm").            
            arrayIndex(this, -1).
            build();                                    
    
    final UserParameter<Double> _collectorResistance = UserParameter.Builder.
            <Double>start("collectorResistance", DEF_COL_RES).                       
            longName(I18nKeys.INTERNAL_COL_RES).
            shortName("rC").
            unit("ohm").            
            arrayIndex(this, -1).
            build();                                    
    
    
    final UserParameter<Double> _forwardVoltage = UserParameter.Builder.
            <Double>start("forwardVoltage", DEF_uF).                       
            longName(I18nKeys.SEMICONDUCTOR_FORWARD_VOLTAGE).
            shortName("uF").
            unit("V").            
            arrayIndex(this, 5).
            build();                                    
    
    final UserParameter<Boolean> _isNpn = UserParameter.Builder.
            <Boolean>start("isNpn", true).                       
            longName(I18nKeys.IF_TRUE_THEN_NPN).
            shortName("NPN").            
            arrayIndex(this, 6).
            build();                               
    
    private final TerminalRelativePosition _baseTerminal;    
    private final AbstractTerminal _emitterTerminal;
    private final AbstractTerminal _collectorTerminal;
    private final AbstractTerminal _midTerminal = new TerminalHiddenSubcircuit(this);
        
    
    private final Diode _diode1;
    private final Diode _diode2;
    private final AbstractCurrentSource controlledSource1;
    private final AbstractCurrentSource controlledSource2;
    private final AbstractResistor _resistor1;
    

    public BJT() {
        super();        
        _collectorTerminal = XIN.get(0);
        _baseTerminal = new TerminalRelativePosition(this, -2, 0);        
        _emitterTerminal = YOUT.get(0);        
        XIN.add(_baseTerminal);
        //YOUT.add(_baseMidTerminal);
        double i = 0, u = 0;    // Strom und Spannung eines Zweipols --> wird laufend in 'LKMatrizen' in parameter[] hineingeschrieben
                
        _diode1 = (Diode) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_D, this);
        _diode1.getIDStringDialog().setRandomStringID();        
        _diode1.setOutputTerminal(0, _collectorTerminal);
        _diode1.setInputTerminal(0, _midTerminal);

        _diode2 = (Diode) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_D, this);
        _diode2.getIDStringDialog().setRandomStringID();        
        _diode2.setOutputTerminal(0, _emitterTerminal);
        _diode2.setInputTerminal(0, _midTerminal);


        controlledSource1 = (AbstractCurrentSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_I, this);        
        controlledSource1.setOutputTerminal(0, _midTerminal);
        controlledSource1.setInputTerminal(0, _collectorTerminal);
        controlledSource1.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
        controlledSource1.directPotentialGain.setValueWithoutUndo(DEF_FORWARD_BETA / DEF_BASE_RES);        


        controlledSource2 = (AbstractCurrentSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_I, this);        
        controlledSource2.setOutputTerminal(0, _midTerminal);
        controlledSource2.setInputTerminal(0, _emitterTerminal);
        controlledSource2.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
        controlledSource2.directPotentialGain.setValueWithoutUndo(DEF_BACKWARD_BETA / DEF_BASE_RES);
        

        _resistor1 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);        
        _resistor1._resistance.setValueWithoutUndo(DEF_BASE_RES);
        _resistor1.setOutputTerminal(0, _midTerminal);
        _resistor1.setInputTerminal(0, _baseTerminal);

        controlledSource1.getComponentCoupling().setNewCouplingElement(0, _resistor1);
        controlledSource2.getComponentCoupling().setNewCouplingElement(0, _resistor1);
    }

    private void setNPNTerminals() {

        // output emitter diode
        _diode1.setOutputTerminal(0, _collectorTerminal);
        _diode1.setInputTerminal(0, _midTerminal);

        // output collector diode
        _diode2.setOutputTerminal(0, _emitterTerminal);
        _diode2.setInputTerminal(0, _midTerminal);       
        
        // emitter clamping current source
        controlledSource1.setOutputTerminal(0, _midTerminal);
        controlledSource1.setInputTerminal(0, _collectorTerminal);

        // collector clamping current source
        controlledSource2.setOutputTerminal(0, _midTerminal);
        controlledSource2.setInputTerminal(0, _emitterTerminal);               

        // base resistance
        _resistor1.setOutputTerminal(0, _midTerminal);
        _resistor1.setInputTerminal(0, _baseTerminal);
        //_baseMidTerminal.getLabelObject().setLabel("base mid");
        //_midTerminal.getLabelObject().setLabel("Mid mid");
    }
    
    private void setPNPTerminals() {
        // output emitter diode
        _diode1.setOutputTerminal(0, _midTerminal);
        _diode1.setInputTerminal(0, _collectorTerminal);

        // output collector diode
        _diode2.setOutputTerminal(0, _midTerminal);
        _diode2.setInputTerminal(0, _emitterTerminal);

        // emitter clamping current source
        controlledSource1.setOutputTerminal(0, _collectorTerminal);
        controlledSource1.setInputTerminal(0, _midTerminal);

        // collector clamping current source
        controlledSource2.setOutputTerminal(0, _emitterTerminal);
        controlledSource2.setInputTerminal(0, _midTerminal);
        
        // base resistance
        _resistor1.setOutputTerminal(0, _baseTerminal);
        _resistor1.setInputTerminal(0, _midTerminal);        
        
    }
    
    
    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {

        controlledSource1.directPotentialGain.setValueWithoutUndo(_forwardBeta.getValue() / (_baseResistance.getValue()));
        controlledSource2.directPotentialGain.setValueWithoutUndo(_backwardBeta.getValue() / (_baseResistance.getValue()));

        _resistor1._resistance.setValueWithoutUndo(_baseResistance.getValue());

        // set the diode parameters:
        _diode1._forwardVoltageDrop.setValueWithoutUndo(_forwardVoltage.getValue());
        _diode1._onResistance.setValueWithoutUndo(_collectorResistance.getValue());

        _diode2._forwardVoltageDrop.setValueWithoutUndo(_forwardVoltage.getValue());
        _diode2._onResistance.setValueWithoutUndo(_emitterResistance.getValue());
        

        if (_isNpn.getValue()) {
            setNPNTerminals();
        } else {
            setPNPTerminals();
        }
        return Arrays.asList( new AbstractBlockInterface[]{_diode1, _diode2, controlledSource1, _resistor1});
    }

    
    


    @Override
    protected void drawForeground(final Graphics2D graphics) {
        double dd = 0.25, alpha = Math.atan(HEIGHT), ddx = dd * Math.cos(alpha), ddy = dd * Math.sin(alpha);
        
        int[] xPoints = new int[]{(int) -(dpix * 0.5), -dpix / 3, 0};
        int[] yPoints = new int[]{(int) (dpix * 0.8), (int) (dpix * 0.5), dpix};

        if (!_isNpn.getValue()) { // draw pnp triangle
            xPoints = new int[]{(int) -(dpix * 0.6), 0, (int) -(dpix * 0.3)};
            yPoints = new int[]{(int) (dpix * 0.5), (int) (dpix * 0.75), (int) (1.1 * dpix)};
        }

        graphics.fillPolygon(xPoints, yPoints, 3);
        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke((float) 3.0));
        graphics.drawLine(-(int) (dpix * 0.8), (int) (dpix * 0.8), -(int) (dpix * 0.8), (int) (-dpix * 0.8));
        graphics.setStroke(oldStroke);        
    }

    @Override
    protected void drawConnectorLines(Graphics2D graphics) {
        graphics.drawLine(0, -dpix * 2, 0, -dpix);
        graphics.drawLine(0, dpix * 2, 0, dpix);
        graphics.drawLine(-(int) (dpix * 0.8), (int) (dpix * 0.3), 0, dpix );
        graphics.drawLine(-(int) (dpix * 0.8), (int) (-dpix * 0.3), 0, -dpix );
        graphics.drawLine(-2 * dpix, 0, -(int) (dpix * 0.8), 0);
    }
    
    
    @Override
    public boolean includeParentInSimulation() {
        return false;
    }

    @Override
    protected Window openDialogWindow() {
        return new BJTDialog(this);
    }    
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return AbstractCircuitBlockInterface.getCalculatorsFromSubComponents(this);        
    }
}
