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
import ch.technokrat.gecko.geckocircuits.control.calculators.GreaterThanCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public final class ReglerGreaterThan extends SimpleRegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerGreaterThan.class, "GT", I18nKeys.GREATER_THAN);

    public ReglerGreaterThan() {
        super(2, 1);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"gt"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.GREATER_THAN_OUTPUT};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new GreaterThanCalculator();        
    }

    @Override
    String getDialogMessage() {
        return "<html>x1 >  x2  ...  y1 = 1<br>"
                + "x1 <= x2  ...  y1 = 0</html>";
    }
}
