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

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.calculators.PT1Calculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

abstract class AbstractReglerPT extends AbstractReglerSingleInputSingleOutput {

    final UserParameter<Double> _TVal = UserParameter.Builder.<Double>start("T", 1.0).
            longName(I18nKeys.TIME_CONSTANT).
            shortName("T").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();
    final UserParameter<Double> _a1Val = UserParameter.Builder.<Double>start("a1", 1.0).
            longName(I18nKeys.GAIN).
            shortName("a1").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 1).
            build();

    public AbstractReglerPT() {
        super();
        _TVal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {                
                if (_calculator instanceof PT1Calculator) {
                    ((PT1Calculator) _calculator).setTimeConstant(_TVal.getValue());
                }
            }
        });

        _a1Val.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {                
                if (_calculator instanceof PT1Calculator) {
                    ((PT1Calculator) _calculator).setGain(_a1Val.getValue());
                }
            }
        });
    }      
    
    @Override
    protected final Window openDialogWindow() {
        return new ReglerPTDialog(this);
    }
}
