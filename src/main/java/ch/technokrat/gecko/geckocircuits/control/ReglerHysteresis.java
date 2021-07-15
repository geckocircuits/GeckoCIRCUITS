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
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlInput;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.HysteresisCalculatorExternal;
import ch.technokrat.gecko.geckocircuits.control.calculators.HysteresisCalculatorInternal;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerHysteresis extends RegelBlock implements ControlInputTwoTerminalStateable {

    /**
     * yes, -1 and not +1! with this setting, the output gets +1 when the input is -1, and vice versa.
     */
    private static final double DEF_HYS_THRES = -1;
    
    private static final int X_EXTERNAL = -1;
    private static final int Y_EXTERNAL = -2;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerHysteresis.class, "HYS", I18nKeys.HYSTERESIS);
    
    public final UserParameter<Double> _hysteresisThreshold = UserParameter.Builder.<Double>start("h", DEF_HYS_THRES).
            longName(I18nKeys.HYSTERESIS_THRESHOLD).
            shortName("h").
            showInTextInfo(TextInfoType.SHOW_WHEN_NON_EXTERNAL).
            arrayIndex(this, 0).
            build();
    public final UserParameter<Boolean> _useExternal = UserParameter.Builder.<Boolean>start("useExternal", false).
            longName(I18nKeys.IF_TRUE_EXTERNAL_TERMINALS).
            shortName("external").
            arrayIndex(this, 1).
            build();
    
    private TerminalControlInput _stashedTerminal;
    
    public ReglerHysteresis() {
        super(1, 1);
        
        setExpandedParameterListener(_useExternal);
                
        _hysteresisThreshold.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                if(_calculator instanceof HysteresisCalculatorInternal) {
                    ((HysteresisCalculatorInternal) _calculator).setHValue(_hysteresisThreshold.getValue());
                    return;
                }
                assert _calculator == null || _calculator instanceof HysteresisCalculatorExternal : "Calculator not know!";
            }
        });
    }


    @Override
    public String[] getOutputNames() {
        return new String[]{"hys"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.HYTERESIS_OUTPUT};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (_useExternal.getValue()) {
            return new HysteresisCalculatorExternal();
        } else {
            return new HysteresisCalculatorInternal(_hysteresisThreshold.getValue());
        }
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        super.drawBlockRectangle(graphics);
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;
        final Color origColor = graphics.getColor();
        if (_useExternal.getValue()) {
            graphics.drawLine(xPos * dpix, (int) ((yPos + 1 / 2.0) * dpix), xPos * dpix, (yPos + 2) * dpix);
            //int dy = (int) (SchematischeEingabe2.foCONTROL.getStringBounds("xxx", frc).getHeight() * 0.25);
            graphics.setColor(GlobalColors.farbeInBearbeitungCONTROL);
            //XIN[1] = new Point((int) xe1, (int) ye1);
            graphics.drawString("h", (int) (dpix * xPos) + 1 + 2, (int) (dpix * (yPos + 2)) + 1 + 2);
            graphics.setColor(origColor);

        }
    }
    

    @Override
    public int getBlockHeight() {
        return dpix;
    }        


    @Override
    protected final Window openDialogWindow() {
        return new ReglerHysteresisDialog(this);
    }

    @Override
    public void setFolded() {
        if(XIN.size() == 2) {            
            _stashedTerminal = (TerminalControlInput) XIN.pop();            
        }
        if(XIN.size() == 1) {
            return;
        }
        assert false : "Impossible input terminal number!";
    }

    @Override
    public void setExpanded() {
        
        if(XIN.size() == 2) {            
            return;
        }
        
        if(_stashedTerminal != null) {
            XIN.push(_stashedTerminal);
        } else {
            XIN.add(new TerminalControlInput(this, X_EXTERNAL, Y_EXTERNAL));
        }                                                            
    }

    @Override
    public boolean isExternalSet() {
        return _useExternal.getValue();
    }
    
    @Override
    public void setExternalUsed(final boolean value) {
        _useExternal.setUserValue(value);
    }
    
}
