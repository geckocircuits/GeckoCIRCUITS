/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.DQABCDCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

public final class ReglerDQABC extends RegelBlock {
    
    private static final int PORT_1_POS = 0;
    private static final int PORT_2_POS = -1;
    private static final int PORT_3_POS = -2;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerDQABC.class, "DQABC", I18nKeys.DQ_ABC_TRANSFORMATION);
    
    public ReglerDQABC() {
        super();
        
        XIN.add(new TerminalControlInputWithLabel(this, -2, PORT_1_POS, "d"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, PORT_2_POS, "q"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, PORT_3_POS, "th"));                
        
        YOUT.add(new TerminalControlOutputWithLabel(this, 1, PORT_1_POS, "a"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 1, PORT_2_POS, "b"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 1, PORT_3_POS, "c"));                
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"a", "b", "c"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.PHASE_A, I18nKeys.PHASE_B, I18nKeys.PHASE_C};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new DQABCDCalculator();
        
    }        

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    @Override
    protected Window openDialogWindow() {
        return new DialogWindowWithoutInput(this);        
    }       
}
