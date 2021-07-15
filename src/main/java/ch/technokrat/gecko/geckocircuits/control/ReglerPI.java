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
import ch.technokrat.gecko.geckocircuits.control.calculators.PICalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerPI extends AbstractReglerSingleInputSingleOutput {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerPI.class, "PI", I18nKeys.PI_CONTROL);

    public final UserParameter<Double> _r0 = UserParameter.Builder.<Double>start("r0", -1.0).
            longName(I18nKeys.GAIN).
            shortName("r0").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();
    public final UserParameter<Double> _a1 = UserParameter.Builder.<Double>start("a1", -1.0).
            longName(I18nKeys.INTEGRATOR_COEFFICIENT).
            shortName("a1").
            unit("1/sec").
            arrayIndex(this, 1).
            build();
    public final UserParameter<Double> _TimeConstant = UserParameter.Builder.<Double>start("T_Value", 1.0).
            longName(I18nKeys.TIME_CONSTANT).
            unit("sec").
            shortName("T").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, -1).
            build();

    public ReglerPI() {
        super();
        _a1.addActionListener(_updateTValue);
        _a1.addActionListener(_updateA1Value);

        _r0.addActionListener(_updateTValue);
        _r0.addActionListener(_updateR0Value);
    }
    private final ActionListener _updateA1Value = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            if (_calculator instanceof PICalculator) {
                ((PICalculator) _calculator).setA1(_a1.getValue());
                return;
            }

            assert _calculator == null : "Calculator not known!";

        }
    };
    private final ActionListener _updateR0Value = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            if (_calculator instanceof PICalculator) {
                ((PICalculator) _calculator).setR0(_r0.getValue());
                return;
            }
            assert _calculator == null : "Calculator not known!";

        }
    };
    private final ActionListener _updateTValue = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {

            if (_calculator instanceof PICalculator) {
                ((PICalculator) _calculator).setA1(_a1.getValue());
                ((PICalculator) _calculator).setR0(_r0.getValue());
            } else {
                assert _calculator == null : "Calculator not known!";
            }

            final double a1Value = _a1.getValue();
            if (a1Value != 0) {
                _TimeConstant.setUserValue(_r0.getValue() / a1Value);                
            }
        }
    };

    @Override
    public String[] getOutputNames() {
        return new String[]{"pi"};
    }        

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new PICalculator(_r0.getValue(), _a1.getValue());
    }    
    

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.PROPORTIONAL_INTEGRAL_CONTROL_DESCRIPTION};
    }    
        

    @Override
    protected final Window openDialogWindow() {
        return new ReglerPIDialog(this);
    }
    
}
