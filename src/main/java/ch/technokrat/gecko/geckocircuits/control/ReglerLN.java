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
import ch.technokrat.gecko.geckocircuits.control.calculators.LnCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

public final class ReglerLN extends AbstractReglerSingleInputSingleOutput {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerLN.class, "LN", I18nKeys.LN);

    @Override
    public String[] getOutputNames() {
        return new String[]{"ln"};
    }    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new LnCalculator();        
    }

    @Override
    protected Window openDialogWindow() {
        return new DialogSimpleInfoMessage(this, "y1 = ln (x1)");
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.NATURAL_LOGARITHM_LN};
    }
}
