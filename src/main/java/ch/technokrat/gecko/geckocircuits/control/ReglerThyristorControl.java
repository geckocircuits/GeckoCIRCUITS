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
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.ThyristorControlCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

public final class ReglerThyristorControl extends RegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerThyristorControl.class, "THYRISTOR_CONTROL", I18nKeys.THYRISTOR_CONTROL);

    private DialogThyristorControl dtc;
    private static final int TN_Y = 6;  // Nummer der Terminals fuer Signal-Anschluss    
    final UserParameter<Double> _onTime = UserParameter.Builder.<Double>start("onTime", 4e-3).
            longName(I18nKeys.GATE_ON_TIME_SEC).
            shortName("ontime").
            arrayIndex(this, -1).
            build();
    final UserParameter<Double> _initFreq = UserParameter.Builder.<Double>start("initFreq", 50.0).
            longName(I18nKeys.INITIAL_FREQUENCY_HZ).
            shortName("initf").
            arrayIndex(this, -1).
            build();
    final UserParameter<Double> _phaseShift = UserParameter.Builder.<Double>start("phaseShift", 30.0).
            longName(I18nKeys.PHASE_SHIFT_DEGREES).
            shortName("phase").
            arrayIndex(this, -1).
            build();

    public ReglerThyristorControl() {
        super(0, TN_Y);
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "alpha"));
        XIN.add(new TerminalControlInputWithLabel(this, -2, -XIN.size(), "sync"));
    }

    @Override
    public double getXShift() {
        return 0.5;
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"gt_A1", "gt_A2", "gt_A3", "gt_B1", "gt_B2", "gt_B3"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL, 
            I18nKeys.GATE_SIGNAL,I18nKeys.GATE_SIGNAL,
            I18nKeys.GATE_SIGNAL,I18nKeys.GATE_SIGNAL};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ThyristorControlCalculator(_phaseShift.getValue(), _initFreq.getValue(), _onTime.getValue());
    }
    
    @Override
    protected String getCenteredDrawString() {
        return "THYR\nCTRL";
    }

    public void showWindow() {
        if (dtc == null) {
            dtc = new DialogThyristorControl(this);
        }
        dtc.setVisible(true);
    }

    @Override
    protected Window openDialogWindow() {
        if (dtc == null) {
            dtc = new DialogThyristorControl(this);
        }
        return dtc;
    }
}
