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
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.IntegratorCalculation;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerIntegrator extends RegelBlock {

    private static final String MAX = "max";
    private static final String MIN = "min";
    private static final int MIN_INDEX = 2;
    private static final int MAX_INDEX = 3;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerIntegrator.class, "INT", I18nKeys.INTEGRATOR);
    
    public final UserParameter<Double> _a1Val = UserParameter.Builder.<Double>start("a1", 1.0).
            longName(I18nKeys.INTEGRATOR_COEFFICIENT).
            shortName("a1").
            arrayIndex(this, 0).
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            build();
    public final UserParameter<Double> _y0Val = UserParameter.Builder.<Double>start("y0", 0.0).
            longName(I18nKeys.INITIAL_VALUE).
            shortName("y0").
            showInTextInfo(TextInfoType.SHOW_NON_NULL).
            arrayIndex(this, 1).
            build();
    public final UserParameter<Double> _minLimit = UserParameter.Builder.<Double>start("min", -1.0).
            longName(I18nKeys.LOWER_LIMIT).
            shortName(MIN).
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, MIN_INDEX).
            build();
    public final UserParameter<Double> _maxLimit = UserParameter.Builder.<Double>start("max", 1.0).
            longName(I18nKeys.UPPER_LIMIT).
            shortName(MAX).
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, MAX_INDEX).
            build();

    public ReglerIntegrator() {
        super(2, 1);        

        final ActionListener minMaxAction = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                if (_calculator instanceof IntegratorCalculation) {
                    final IntegratorCalculation intCalc = (IntegratorCalculation) _calculator;
                    final double min = _minLimit.getValue();
                    final double max = _maxLimit.getValue();
                    if(min < max) {
                        intCalc.setMinMax(min, max);
                    }                    
                    return;
                }
                assert _calculator == null : "Calculator not known!";
            }
        };
        
        _maxLimit.addActionListener(minMaxAction);
        _minLimit.addActionListener(minMaxAction);        

        _a1Val.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                if (_calculator instanceof IntegratorCalculation) {
                    final IntegratorCalculation intCalc = (IntegratorCalculation) _calculator;
                    intCalc.setA1Val(_a1Val.getValue());
                    return;
                }
                assert _calculator == null : "Calculator not known!";
            }
        });

        // we don't add an actionlistener the y0 value, since the initial value is fixed after simulation start!

    }            

    @Override
    public String[] getOutputNames() {
        return new String[]{"integral"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.INTEGRAL_OUTPUT_LIMITED};
    }
    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new IntegratorCalculation(_a1Val.getValue(), _y0Val.getValue(), _minLimit.getValue(), _maxLimit.getValue());
    }   

    @Override
    protected final Window openDialogWindow() {
        return new ReglerIntegratorDialog(this);
    }    
}
