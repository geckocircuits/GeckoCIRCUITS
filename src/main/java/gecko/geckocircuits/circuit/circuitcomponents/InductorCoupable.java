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

import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

// koppelbare Leistungskreis-Induktivitaet [H]
// dahinter verbirgt sich nicht ein Subcircuit, sondern die linearisierte Martixgleichung der Topologie wird
// um die Stroeme in diesen gekoppelten Induktivitaeten erweitert --> verbesserte numerische Stabilitaet 
public final class InductorCoupable extends AbstractInductor  {    
    private static final int DOT_DIAMETER = 5;
    private static final double DOT_SPACING_X = 0.4;
    private static final double DOT_SPACING_Y = 1.1;           
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(InductorCoupable.class, "Lc", I18nKeys.INDUCTOR_COUPLING_LC_H, I18nKeys.INDUCTOR_THAT_CAN_BE_COUPLED);

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        super.drawForeground(graphics);        
        graphics.fillOval((int) (DOT_SPACING_X * dpix), (int) (dpix * DOT_SPACING_Y), DOT_DIAMETER, DOT_DIAMETER);
    }                                   

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new InductorCouplingCalculator(this));
    }
}
