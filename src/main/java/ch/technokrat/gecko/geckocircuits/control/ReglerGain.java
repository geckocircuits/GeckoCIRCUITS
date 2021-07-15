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
import ch.technokrat.gecko.geckocircuits.control.calculators.GainCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public final class ReglerGain extends AbstractReglerSingleInputSingleOutput {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerGain.class, "GAIN", I18nKeys.GAIN);

    
    public final UserParameter<Double> _gain = UserParameter.Builder.<Double>start("k", 1.0).
            longName(I18nKeys.GAIN).
            shortName("r0").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();                
    
    public ReglerGain() {
        super();
        _gain.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                if(_calculator instanceof GainCalculator) {
                    ((GainCalculator) _calculator).setGain(_gain.getValue());
                    return;
                }
                assert _calculator == null;
            }
        });
    }


    @Override
    public String[] getOutputNames() {
        return new String[]{"p"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.OUTPUT_MULTIPLIED_BY_SPECIFIED_GAIN};
    }            
    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new GainCalculator(_gain.getValue());        
    }    

    @Override
    protected String getCenteredDrawString() {
        return "P";
    }                        

    @Override
    protected Window openDialogWindow() {
        return new ReglerGainDialog(this);        
    }
    
}
