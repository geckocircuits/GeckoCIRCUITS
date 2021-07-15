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
import ch.technokrat.gecko.geckocircuits.control.calculators.CounterCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public final class ReglerCounter extends SimpleRegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerCounter.class, "COUNTER", I18nKeys.COUNTER);
    
    public ReglerCounter() {
        super(2, 1);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"cnt"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.COUNT_NUMBER_OF_INPUTS_RISING};
    }
    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new CounterCalculatable();
    }

    @Override
    protected String getCenteredDrawString() {
        return "CNT";
    }            

    @Override
    String getDialogMessage() {
        return "<html>if (x1 &gt 0.5) && (x1_old &lt 0.5)  ...  c = c + 1;   y1= c"
            + "<br>if (z &gt 0.5)  ...  c = 0  (reset)</html>";
    }
    
}
