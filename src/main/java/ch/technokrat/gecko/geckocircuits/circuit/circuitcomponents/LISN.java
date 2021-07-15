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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativeFixedDirection;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// LISN-Netzwerk fuer EMV-Messungen 
// Leistungskreis-Induktivitaet koppelbar ueber M (im Gegensatz dazu kann L nicht gekoppelt werden)
// dieses Element ist in Wirklichkeit ein Subcircuit, der ein L in Serie zu einer Spannungsquelle enthaelt
public class LISN extends AbstractCircuitBlockInterface implements HiddenSubCircuitable {
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(LISN.class, "LISN", I18nKeys.LISN, I18nKeys.LINE_IMPEDANCE_STABILIZATION_NETWORK);
        
    private AbstractBlockInterface[] qLK;
    private AbstractTerminal intern1, intern2, intern3, intern7, intern8, intern9;
    
    private double Lin = 50e-6, Lout = 300e-9, Rout = 20e-3, Cgnd = 250e-9, Rgnd = 50;
    private InductorWOCoupling _Lin1;
    private InductorWOCoupling _Lin2;
    private InductorWOCoupling _Lin3;
    private InductorWOCoupling _Lout1;
    private InductorWOCoupling _Lout2;
    private InductorWOCoupling _Lout3;
    private AbstractResistor _Rout1;
    private AbstractResistor _Rout2;
    private AbstractResistor _Rout3;
    private AbstractResistor _RGnd1;
    private AbstractResistor _RGnd2;
    private AbstractResistor _RGnd3;
    private AbstractCapacitor _CGnd1;
    private AbstractCapacitor _CGnd2;
    private AbstractCapacitor _CGnd3;
    
    public LISN() {
        super();
        setComponentDirection(ComponentDirection.NORTH_SOUTH);
        XIN.add(new TerminalRelativeFixedDirection(this, -3, 1));
        XIN.add(new TerminalRelativeFixedDirection(this, -3, 0));
        XIN.add(new TerminalRelativeFixedDirection(this, -3, -1));
        
        YOUT.add(new TerminalRelativeFixedDirection(this, 2, 1));
        YOUT.add(new TerminalRelativeFixedDirection(this, 2, 0));
        YOUT.add(new TerminalRelativeFixedDirection(this, 2, -1));
        
        YOUT.add(new TerminalRelativeFixedDirection(this, -2, -3));
        YOUT.add(new TerminalRelativeFixedDirection(this, 1, -3));
        YOUT.add(new TerminalRelativeFixedDirection(this, 0, -3));
        YOUT.add(new TerminalRelativeFixedDirection(this, -1, -3));
        
        intern1 = new TerminalRelativeFixedDirection(this, -1, -1);
        intern2 = new TerminalRelativeFixedDirection(this, -1, 0);
        intern3 = new TerminalRelativeFixedDirection(this, -1, 1);
        
        intern7 = new TerminalRelativeFixedDirection(this, 0, -1);
        intern8 = new TerminalRelativeFixedDirection(this, 0, 0);
        intern9 = new TerminalRelativeFixedDirection(this, 0, 1);
              
        this.setzeSubcircuit(false);
    }        

    public void setzeParameterZustandswerteAufNULL() {
        // set all u and i to zero >>
        _Lin1.parameter[2] = 0;
        _Lin1.parameter[3] = 0;
        _Lin2.parameter[2] = 0;
        _Lin2.parameter[3] = 0;
        _Lin3.parameter[2] = 0;
        _Lin3.parameter[3] = 0;
        _Lout1.parameter[2] = 0;
        _Lout1.parameter[3] = 0;
        _Lout2.parameter[2] = 0;
        _Lout2.parameter[3] = 0;
        _Lout3.parameter[2] = 0;
        _Lout3.parameter[3] = 0;
        qLK[6].parameter[1] = 0;
        qLK[6].parameter[2] = 0;
        qLK[7].parameter[1] = 0;
        qLK[7].parameter[2] = 0;
        qLK[8].parameter[1] = 0;
        qLK[8].parameter[2] = 0;
        qLK[ 9].parameter[1] = 0;
        qLK[9].parameter[2] = 0;
        qLK[10].parameter[1] = 0;
        qLK[10].parameter[2] = 0;
        qLK[11].parameter[1] = 0;
        qLK[11].parameter[2] = 0;
        qLK[12].parameter[2] = 0;
        qLK[12].parameter[3] = 0;
        qLK[13].parameter[2] = 0;
        qLK[13].parameter[3] = 0;
        qLK[14].parameter[2] = 0;
        qLK[14].parameter[3] = 0;
    }

    
    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        return Arrays.asList(qLK);
    }                       
    
    
    private void setzeSubcircuit(boolean elementVonDateiGeladen) {
        qLK = new AbstractBlockInterface[15];
        //----------------
        // 3x Eingangsinduktivitaet (netzseitig) --> 
        _Lin1 = (InductorWOCoupling) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_L, this);
        _Lin2 = (InductorWOCoupling) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_L, this);
        _Lin3 = (InductorWOCoupling) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_L, this);
        
        qLK[0] = _Lin1;
        qLK[1] = _Lin2;
        qLK[2] = _Lin3;
        
        _Lin1._inductance.setValueWithoutUndo(Lin);
        _Lin2._inductance.setValueWithoutUndo(Lin);
        _Lin3._inductance.setValueWithoutUndo(Lin);
        
        
        _Lin1.setInputTerminal(0, XIN.get(0));
        _Lin2.setInputTerminal(0, XIN.get(1));
        _Lin3.setInputTerminal(0, XIN.get(2));
        
        _Lin1.setOutputTerminal(0, intern1);
        _Lin2.setOutputTerminal(0, intern2);
        _Lin3.setOutputTerminal(0, intern3);
        
        // 3x Ausgangsinduktivitaet (Konverter-seitig) --> 
        _Lout1 = (InductorWOCoupling) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_L, this);
        _Lout2 = (InductorWOCoupling) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_L, this);
        _Lout3 = (InductorWOCoupling) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_L, this);
                
        qLK[3] = _Lout1;
        qLK[4] = _Lout2;
        qLK[5] = _Lout3;
        _Lout1._inductance.setValueWithoutUndo(Lout);
        _Lout2._inductance.setValueWithoutUndo(Lout);
        _Lout3._inductance.setValueWithoutUndo(Lout);                
        
        _Lout1.setInputTerminal(0, intern1);
        _Lout2.setInputTerminal(0, intern2);
        _Lout3.setInputTerminal(0, intern3);
        
        _Lout1.setOutputTerminal(0, intern7);
        _Lout2.setOutputTerminal(0, intern8);
        _Lout3.setOutputTerminal(0, intern9);
        
        // 3x Ausgangswiderstand (Konverter-seitig) --> 
        //if (!elementVonDateiGeladen) {
        //    SchematischeEingabe2.staticZaehlerLKelementeINIT[CircuitTyp.LK_R]++;
        //}
        _Rout1 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _Rout2 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _Rout3 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        
        qLK[6] = _Rout1;
        qLK[7] = _Rout2;
        qLK[8] = _Rout3;
        
        _Rout1._resistance.setValueWithoutUndo(Rout);
        _Rout2._resistance.setValueWithoutUndo(Rout);
        _Rout3._resistance.setValueWithoutUndo(Rout);
        
        _Rout1.setOutputTerminal(0, YOUT.get(0));
        _Rout2.setOutputTerminal(0, YOUT.get(1));
        _Rout3.setOutputTerminal(0, YOUT.get(2));
        
        _Rout1.setInputTerminal(0, intern7);
        _Rout2.setInputTerminal(0, intern8);
        _Rout3.setInputTerminal(0, intern9);
        
        // 3x GND-widerstand (Signalmessbezug) --> 
        _RGnd1 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _RGnd2 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _RGnd3 = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        
        qLK[ 9] = _RGnd1;
        qLK[10] = _RGnd2;        
        qLK[11] = _RGnd3;
                                
        _RGnd1._resistance.setValueWithoutUndo(Rgnd);
        _RGnd2._resistance.setValueWithoutUndo(Rgnd);
        _RGnd3._resistance.setValueWithoutUndo(Rgnd);
        
        _RGnd1.setOutputTerminal(0, YOUT.get(3));
        _RGnd2.setOutputTerminal(0, YOUT.get(3));
        _RGnd3.setOutputTerminal(0, YOUT.get(3));
        
        _RGnd1.setInputTerminal(0, YOUT.get(4));
        _RGnd2.setInputTerminal(0, YOUT.get(5));
        _RGnd3.setInputTerminal(0, YOUT.get(6));
        
        // 3x GND-kapazitaet (Signalmessung) --> 
        _CGnd1 = (AbstractCapacitor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_C, this);
        _CGnd2 = (AbstractCapacitor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_C, this);
        _CGnd3 = (AbstractCapacitor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_C, this);
        
        qLK[12] = _CGnd1;
        qLK[13] = _CGnd2;
        qLK[14] = _CGnd3;
        
        for(AbstractBlockInterface elem : qLK) {
            elem.getIDStringDialog().setRandomStringID();
        }
        
        _CGnd1._capacitance.setValueWithoutUndo(Cgnd);
        _CGnd2._capacitance.setValueWithoutUndo(Cgnd);
        _CGnd3._capacitance.setValueWithoutUndo(Cgnd);
        
        _CGnd1.setOutputTerminal(0, YOUT.get(4));
        _CGnd2.setOutputTerminal(0, YOUT.get(5));
        _CGnd3.setOutputTerminal(0, YOUT.get(6));
        
        _CGnd1.setInputTerminal(0, intern1);
        _CGnd2.setInputTerminal(0, intern2);
        _CGnd3.setInputTerminal(0, intern3);                       
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        restoreOrigTransformation(graphics);
        int x = getSheetPosition().x;
        int y = getSheetPosition().y;

        int x1 = XIN.get(0).getPosition().x;
        int y1 = XIN.get(0).getPosition().y;
        int x2 = XIN.get(1).getPosition().x;
        int y2 = XIN.get(1).getPosition().y;        
        int x3 = XIN.get(2).getPosition().x;
        int y3 = XIN.get(2).getPosition().y;
        
        int x4 = YOUT.get(0).getPosition().x;
        int y4 = YOUT.get(0).getPosition().y;
        int x5 = YOUT.get(1).getPosition().x;
        int y5 = YOUT.get(1).getPosition().y;
        int x6 = YOUT.get(2).getPosition().x;
        int y6 = YOUT.get(2).getPosition().y;
        
        int x7 = YOUT.get(3).getPosition().x;
        int y7 = YOUT.get(3).getPosition().y;
        int x8 = YOUT.get(4).getPosition().x;
        int y8 = YOUT.get(4).getPosition().y;
        int x9 = YOUT.get(5).getPosition().x;
        int y9 = YOUT.get(5).getPosition().y;
        int x10 = YOUT.get(6).getPosition().x;
        int y10 = YOUT.get(6).getPosition().y;
        
        graphics.drawLine((int) (dpix * x1), (int) (dpix * y1), (int) (dpix * x4), (int) (dpix * y4));
        graphics.drawLine((int) (dpix * x2), (int) (dpix * y2), (int) (dpix * x5), (int) (dpix * y5));
        graphics.drawLine((int) (dpix * x3), (int) (dpix * y3), (int) (dpix * x6), (int) (dpix * y6));
        graphics.drawLine((int) (dpix * x7), (int) (dpix * y7), (int) (dpix * x7), (int) (dpix * y));
        graphics.drawLine((int) (dpix * x8), (int) (dpix * y8), (int) (dpix * x8), (int) (dpix * y));
        graphics.drawLine((int) (dpix * x9), (int) (dpix * y9), (int) (dpix * x9), (int) (dpix * y));
        graphics.drawLine((int) (dpix * x10), (int) (dpix * y10), (int) (dpix * x10), (int) (dpix * y));
        setTranslationRotation(graphics);
    }
    
        

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        graphics.fillRect((int) (-dpix * 2.5), (int) (-dpix * 1.5), (int) (dpix * 4.0), (int) (dpix * 4.0));
    }        

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        graphics.drawRect((int) (-dpix * 2.5), (int) (-dpix * 1.5), (int) (dpix * 4.0), (int) (dpix * 4.0));
        restoreOrigTransformation(graphics);
        //g.drawString("LISN", (int)(dpix*x)-10, (int)(dpix*(y-0.0))); 
        graphics.setColor(Color.magenta);
        int x = getSheetPosition().x;
        int y = getSheetPosition().y;

        int x1 = XIN.get(0).getPosition().x;
        int y1 = XIN.get(0).getPosition().y;
        int x2 = XIN.get(1).getPosition().x;
        int y2 = XIN.get(1).getPosition().y;        
        int x3 = XIN.get(2).getPosition().x;
        int y3 = XIN.get(2).getPosition().y;
        
        int x4 = YOUT.get(0).getPosition().x;
        int y4 = YOUT.get(0).getPosition().y;
        int x5 = YOUT.get(1).getPosition().x;
        int y5 = YOUT.get(1).getPosition().y;
        int x6 = YOUT.get(2).getPosition().x;
        int y6 = YOUT.get(2).getPosition().y;
        
        int x7 = YOUT.get(3).getPosition().x;
        int y7 = YOUT.get(3).getPosition().y;
        int x8 = YOUT.get(4).getPosition().x;
        int y8 = YOUT.get(4).getPosition().y;
        int x9 = YOUT.get(5).getPosition().x;
        int y9 = YOUT.get(5).getPosition().y;
        int x10 = YOUT.get(6).getPosition().x;
        int y10 = YOUT.get(6).getPosition().y;
        
        graphics.drawString("R", (int) (dpix * (x - 2.5)) + 2, (int) (dpix * y1) + 5);
        graphics.drawString("S", (int) (dpix * (x - 2.5)) + 2, (int) (dpix * y2) + 5);
        graphics.drawString("T", (int) (dpix * (x - 2.5)) + 2, (int) (dpix * y3) + 5);
        graphics.drawString("U", (int) (dpix * (x + 1.5)) - 12, (int) (dpix * y1) + 5);
        graphics.drawString("V", (int) (dpix * (x + 1.5)) - 12, (int) (dpix * y2) + 5);
        graphics.drawString("W", (int) (dpix * (x + 1.5)) - 12, (int) (dpix * y3) + 5);
        graphics.drawString("n", (int) (dpix * x7) - 5, (int) (dpix * (y + 2.5)) - 3);
        graphics.drawString("a", (int) (dpix * x8) - 5, (int) (dpix * (y + 2.5)) - 3);
        graphics.drawString("b", (int) (dpix * x9) - 5, (int) (dpix * (y + 2.5)) - 3);
        graphics.drawString("c", (int) (dpix * x10) - 5, (int) (dpix * (y + 2.5)) - 3);
        setTranslationRotation(graphics);
    }

    @Override
    public ComponentDirection getComponentDirection() {
        return ComponentDirection.NORTH_SOUTH;
    }

    @Override
    public void setComponentDirection(ComponentDirection oritentation) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);                
    }
    
    
    
    @Override
    public boolean includeParentInSimulation() {
        return false;
    }
    
    @Override
    protected final Window openDialogWindow() {
        return new LISNDialog(this);        
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return AbstractCircuitBlockInterface.getCalculatorsFromSubComponents(this);        
    }
}
