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
package gecko.geckocircuits.control;

import gecko.geckocircuits.control.calculators.ABCDQCalculator;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.i18n.resources.I18nKeys;
import java.awt.Window;

public final class ReglerABCDQ extends RegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerABCDQ.class, "ABCDQ", I18nKeys.ABC_DQ_TRANSFORM);
    
    public ReglerABCDQ() {
        super();
        
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "a"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "b"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "c"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "th"));
        
        YOUT.add(new TerminalControlOutputWithLabel(this, 1, -YOUT.size(), "d"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 1, -YOUT.size(), "q"));        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"d", "q"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.D_ROTATING_AXIS_COORDINATE, 
            I18nKeys.Q_ROTATING_AXIS_COORDINATE};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ABCDQCalculator();        
    }

    @Override
    public String getCenteredDrawString() {
        return "";
    }        

    @Override
    protected final Window openDialogWindow() {
        return new DialogWindowWithoutInput(this);        
    }
    
}
