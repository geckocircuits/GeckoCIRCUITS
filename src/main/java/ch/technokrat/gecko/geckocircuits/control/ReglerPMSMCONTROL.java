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
import ch.technokrat.gecko.geckocircuits.control.calculators.PmsmControlCalculator;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

public final class ReglerPMSMCONTROL extends RegelBlock {

    private static final int BLOCK_WIDTH = 5;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerPMSMCONTROL.class, "PMSM_CTRL", I18nKeys.PMSM_CONTROLLER);

    public ReglerPMSMCONTROL() {
        super();
        
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "i_r"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "i_s"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "omega"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "phi"));        
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "n*"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "pi_nk"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "pi_nt"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "pi_n_lim"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "pi_ik"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "pi_it"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "pi_i_lim"));
        XIN.add(new TerminalControlInputWithLabel(this, -3, -XIN.size(), "fP"));
        
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "v_alpha"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "v_beta"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "v_q*"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "v_d*"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "i_q*"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "i_d*"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "i_q"));
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, -YOUT.size(), "i_d"));
    }
       
        

    @Override
    public int getBlockWidth() {
        return BLOCK_WIDTH * dpix;
    }
    
    
    @Override
    public String[] getOutputNames() {
        return new String[]{"v_alpha", "v_beta", "v_q*", "v_d*", "i_q*", "i_d*", "i_q", "i_d"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{ 
            I18nKeys.VOLTAGE_ALPHA_IN_ALPHA_BETA_COORDINATES, 
            I18nKeys.VOLTAGE_BETA_IN_ALPHA_BETA_COORDINATES, 
            I18nKeys.VOLTAGE_D_STAR, 
            I18nKeys.VOLTAGE_Q_STAR, 
            I18nKeys.CURRENT_D_STAR, 
            I18nKeys.CURRENT_Q_STAR, 
            I18nKeys.CURRENT_D, 
            I18nKeys.CURRENT_Q 
            };
    }
    

    @Override
    public double getXShift() {
        return 1 / 2.0;
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new PmsmControlCalculator();
    }

    @Override
    protected String getCenteredDrawString() {
        return "PMSM\nCTRL";
    }
               

    @Override
    protected final Window openDialogWindow() {
        return new DialogWindowWithoutInput(this);
    }
    
}
