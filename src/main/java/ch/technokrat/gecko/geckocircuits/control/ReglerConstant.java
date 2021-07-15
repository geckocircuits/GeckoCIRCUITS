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
import ch.technokrat.gecko.geckocircuits.control.calculators.ConstantCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerConstant extends RegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerConstant.class, "CONST", I18nKeys.CONSTANT_VALUE);
    
    private static final String CONST_STR = "const";
    
    final UserParameter<Double> _constValue = UserParameter.Builder.<Double>start("constantValue", 1.0).
            longName(I18nKeys.CONSTANT_VALUE).
            shortName(CONST_STR).
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();                

    public ReglerConstant() {
        super(0, 1);
        
        _constValue.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                if(_calculator instanceof ConstantCalculator) {
                    ((ConstantCalculator) _calculator).setConst(_constValue.getValue());
                    return;
                }
                assert _calculator == null;
            }
        });
    }

    @Override
    public double getXShift() {
        return 1.0 / 2;
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"c"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.CONSTANT_VALUE};
    }
    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ConstantCalculator(_constValue.getValue());        
    }        
    

    @Override
    protected Window openDialogWindow() {
        return new ReglerConstantDialog(this);        
    }            
}
