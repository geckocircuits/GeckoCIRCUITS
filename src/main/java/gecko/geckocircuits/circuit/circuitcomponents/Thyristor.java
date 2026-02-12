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
import static gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

// Leistungskreis-Thyristor (Knickkennlinie)
public final class Thyristor extends AbstractVoltageDropSwitch {
    private static final double WIDTH = 0.5;
    private static final double HEIGHT = 0.6;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(Thyristor.class, "THYR", I18nKeys.THYRISTOR);
    private static final int REVERSE_RECOV_INDEX = 9;
    private static final int CORRECTED_UK_INDEX = 10;
    private static final int POLYGON_POINTS = 3;
    private static final double GATE_SYMBOL_LENGTH = 1.3;
    private static final int GATE_SYMB_THICKNESS = 3;
    
    private static final int DYNAMIC_RESISTANCE_INDEX = 0; 
    private static final int GATE_SIGNAL_INDEX = 8;
    private static final int LAST_SWITCH_TIME_INDEX = 11;
    
    final UserParameter<Double> _reverseRecoveryDelay = UserParameter.Builder.
	<Double>start("reverserecoverydelay", 0.0).
	longName(I18nKeys.REVERSE_RECOVERY_DELAY).
	shortName("t_rr").
	unit("sec").
	arrayIndex(this,REVERSE_RECOV_INDEX).
	build();

    
    public Thyristor() {
        super();       
        uK.setCorrectedIndex(CORRECTED_UK_INDEX);                 
    }
   

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        parameter[DYNAMIC_RESISTANCE_INDEX] = RD_OFF_DEFAULT;
        parameter[4] = 0;
        parameter[5] = 0;
        parameter[GATE_SIGNAL_INDEX] = 0;
        parameter[LAST_SWITCH_TIME_INDEX] = 0;
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        Color oldColor = graphics.getColor();
        graphics.setColor(getForeGroundColor());        
        
        drawGateSymbol(graphics);
        
        graphics.setColor(oldColor);
        graphics.fillPolygon(new int[]{0, (int) (-dpix * WIDTH), (int) (dpix * WIDTH)}, 
                new int[]{(int) (dpix * HEIGHT), (int) (-dpix * HEIGHT), (int) (-dpix * HEIGHT)}, POLYGON_POINTS);
    }    
    
    private void drawGateSymbol(final Graphics2D graphics) {
        final double gateLength = GATE_SYMBOL_LENGTH * WIDTH;  // Gate-Symbol des THYR --> Laenge
        final int gtd = GATE_SYMB_THICKNESS;  // Gate-Symbol des THYR --> Breite in Pix
        graphics.fillRect((int) (-dpix * gateLength), (int) (-gtd / 2), (int) (dpix * gateLength), gtd);  
    }
    
    
    @Override
    protected void drawForeground(final Graphics2D graphics) {                                                
        graphics.drawPolygon(new int[]{0, (int) (-dpix * WIDTH), (int) (dpix * WIDTH)}, 
                new int[]{(int) (dpix * HEIGHT), (int) (-dpix * HEIGHT), (int) (-dpix * HEIGHT)}, POLYGON_POINTS);
        graphics.fillRect((int) (-dpix * WIDTH), (int) (dpix * HEIGHT - 2), (int) (dpix * (2 * WIDTH)), 2);        
    }
    
   
    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        addGateTextInfo();        
        verluste.addTextInfoValue(_textInfo);        
    }    

    @Override
    public Window openDialogWindow() {
        return new ThyristorDialog(this); 
    }    
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new ThyristorCalculator(this));
    }
    
}
