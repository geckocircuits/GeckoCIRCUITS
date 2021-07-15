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
import ch.technokrat.gecko.geckocircuits.control.calculators.PmsmModulatorCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;


public final class ReglerPMSM_Modulator extends RegelBlock {
    private static final int BLOCK_WIDTH = 3;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerPMSM_Modulator.class, "PMSM_MOD", I18nKeys.PMSM_MODULATOR);

    public ReglerPMSM_Modulator() {
        super();
        
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "Ua"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "Ub"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "Uc"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "Uref"));
        
        YOUT.add(new TerminalControlOutputWithLabel(this, 2, -YOUT.size(), "gRm"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 2, -YOUT.size(), "gSm"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 2, -YOUT.size(), "gTm"));
        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"gRm", "gSm", "gTm"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL,I18nKeys.GATE_SIGNAL};
    }

    @Override
    public int getBlockWidth() {
        return BLOCK_WIDTH * dpix;
    }

    @Override
    public double getXShift() {
        return 1/2.0;
    }
    
    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new PmsmModulatorCalculator();        
    }        

    @Override
    protected String getCenteredDrawString() {
        return "PMSM\nMOD";
    }    

    @Override
    protected final Window openDialogWindow() {
        return new DialogWindowWithoutInput(this);
    }
}
