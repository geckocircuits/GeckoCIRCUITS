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

import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;


// Leistungskreis Idealer Schalter (hoch- oder niederohmiger Widerstand, daher bidirektional)
public final class IdealSwitch extends AbstractSwitch {
    private static final double WIDTH = 1.6;
    private static final double HEIGHT = 0.8;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(IdealSwitch.class, "S", I18nKeys.IDAL_SWITCH);
        
    /**
     * this "hacks" are just for backwards-compatibility. In the old GeckoCIRCUITS
     * versions, the ideal switch has an on resistance parameter index of 1, in 
     * all other switches it is 2. The same "shift" applies for off-resistance.
     * @return 
     */    
    @Override
    final int getOnResistanceIndex() {
        return 1;
    }
    
    /**
     * this "hacks" are just for backwards-compatibility. In the old GeckoCIRCUITS
     * versions, the ideal switch has an on resistance parameter index of 1, in 
     * all other switches it is 2. The same "shift" applies for off-resistance.
     * @return 
     */
    @Override
    final int getOffResistanceIndex() {
        return 2;
    }
    

    public void setzeParameterZustandswerteAufNULL() {
        parameter[0] = parameter[2];
        parameter[3] = 0;
        parameter[4] = 0;
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        double dd = 0.25, alpha = Math.atan(HEIGHT / WIDTH), ddx = dd * Math.cos(alpha), ddy = dd * Math.sin(alpha);
        graphics.drawLine(0, -2 * dpix, 0, (int) (-dpix * WIDTH / 2));
        graphics.drawLine(0, 2 * dpix, 0, (int) (dpix * WIDTH / 2));
        graphics.fillPolygon(
                new int[]{0, (int) (dpix * (ddx)), (int) (dpix * (HEIGHT + ddx)), (int) (dpix * HEIGHT)},
                new int[]{(int) (-dpix * WIDTH / 2), (int) (-dpix * (WIDTH / 2 + ddy)), 
                    (int) (dpix * (WIDTH / 2 - ddy)), (int) (dpix * WIDTH / 2)}, 4);
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawLine(0, dpix * 2, 0, dpix);        
        graphics.drawLine(0, -dpix * 2, 0, -dpix);        
    }

    
    
    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        verluste.addTextInfoValue(_textInfo);                
        addGateTextInfo();        
    }    
    
    @Override
    protected Window openDialogWindow() {
        return new IdealSwitchDialog(this);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new IdealSwitchCalculator(this));
    }
    
    
}
