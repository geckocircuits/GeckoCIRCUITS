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
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.DelayCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerDelay extends AbstractReglerSingleInputSingleOutput {
    private static final double DEFAULT_DELAY = 10e-6;    
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerDelay.class, "DELAY", I18nKeys.DELAY);
    
    public final UserParameter<Double> _tDelay = UserParameter.Builder.<Double>start("tDelay", DEFAULT_DELAY).
            longName(I18nKeys.DELAY_INPUT).
            unit("sec").
            shortName("T").
            addAlternativeShortName("T_delay").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();                
        

    public ReglerDelay() {
        super();
        _tDelay.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                if(_calculator instanceof DelayCalculator) {
                    ((DelayCalculator) _calculator).setDelayTime(_tDelay.getValue());
                    return;
                }
                assert _calculator == null;
            }
        });
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"delayed"};
    }        

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new DelayCalculator(_tDelay.getValue());
    }


    @Override
    protected String getCenteredDrawString() {
        return "DEL";
    }            

    @Override
    protected final Window openDialogWindow() {
        return new ReglerDelayDialog(this);        
    }
                
    
    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.OUTPUT_DELEAYED_BY_TIME};
    }
    
}
