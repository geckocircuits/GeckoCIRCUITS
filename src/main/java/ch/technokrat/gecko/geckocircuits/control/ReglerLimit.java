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
import ch.technokrat.gecko.geckocircuits.control.calculators.LimitCalculatorExternal;
import ch.technokrat.gecko.geckocircuits.control.calculators.LimitCalculatorInternal;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;

public final class ReglerLimit extends RegelBlock implements ControlInputTwoTerminalStateable {    
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerLimit.class, "LIMIT", I18nKeys.LIMITER);
    
    private static final double Y_STRING_DRAW_OFFSET = 2.2;
    private static final int X_DRAW_OFFSET = 3;
    private static final int Y_DRAW_OFFSET = 3;
    private static final int IN_TERMS_WITH_EXT = 3;    
    private static final int X_EXTERNAL_TERMINAL = -1;
    private static final String MAX = "max";
    private static final String MIN = "min";
    
    
    final UserParameter<Double> _minLimit = UserParameter.
            Builder.<Double>start("minLimit", -1.0).
            longName(I18nKeys.LOWER_LIMIT).
            shortName(MIN).
            showInTextInfo(TextInfoType.SHOW_WHEN_NON_EXTERNAL).
            arrayIndex(this, 0).
            build();
    final UserParameter<Double> _maxLimit = UserParameter.
            Builder.<Double>start("maxLimit", 1.0).
            longName(I18nKeys.UPPER_LIMIT).
            shortName(MAX).
            showInTextInfo(TextInfoType.SHOW_WHEN_NON_EXTERNAL).
            arrayIndex(this, 1).
            build();
    
    final UserParameter<Boolean> _isExternalSet = UserParameter.Builder.
            <Boolean>start("useExternal", false).
            longName(I18nKeys.IF_TRUE_EXTERNAL_TERMINALS).
            shortName("useExternal").
            arrayIndex(this, 2).
            build();

    private final Stack<TerminalControlInput> _stashedTerminals = new Stack<TerminalControlInput>();
    
    public ReglerLimit() {
        super(1, 1);
        
        final ActionListener minMaxAction = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {                
                if (_calculator instanceof LimitCalculatorInternal) {
                    final double min = _minLimit.getValue();
                    final double max = _maxLimit.getValue();
                    final LimitCalculatorInternal internal = (LimitCalculatorInternal) _calculator;
                    internal.setMinMaxValues(min, max);
                    return;
                }
                assert _calculator == null || _calculator instanceof LimitCalculatorExternal: "Calculator not known!";
            }
        };
        
        _minLimit.addActionListener(minMaxAction);
        _maxLimit.addActionListener(minMaxAction);

        setExpandedParameterListener(_isExternalSet);        
    }
            

    @Override
    public int getBlockHeight() {
        return dpix;
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"lim"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.OUTPUT_LIMITED_BY_SPECIFIED_BOUNDS};
    }    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (_isExternalSet.getValue()) {
            return new LimitCalculatorExternal();
        } else {
            return new LimitCalculatorInternal(_minLimit.getValue(), _maxLimit.getValue());
        }
    }


    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;

        final Color origColor = graphics.getColor();
        super.drawBlockRectangle(graphics);

        if (_isExternalSet.getValue()) {
            graphics.drawLine(xPos * dpix, (int) ((yPos + 1/2.0) * dpix), xPos * dpix, (yPos + Y_DRAW_OFFSET) * dpix);
            graphics.setColor(GlobalColors.farbeInBearbeitungCONTROL);
            graphics.drawString(MIN, (int) (dpix * xPos) + X_DRAW_OFFSET, (int) (dpix * (yPos + Y_STRING_DRAW_OFFSET)));
            graphics.drawString(MAX, (int) (dpix * xPos) + X_DRAW_OFFSET, (int) (dpix * (yPos + Y_STRING_DRAW_OFFSET+1)));
            graphics.setColor(origColor);
        }
    }

    @Override
    protected String getCenteredDrawString() {
        return "LIM";
    }    
                
    
    @Override
    protected Window openDialogWindow() {
        return new ReglerLimitDialog(this);        
    }    

    @Override
    public void setFolded() {
        while (XIN.size() > 1) {
            _stashedTerminals.push((TerminalControlInput) XIN.pop());   
        }
        assert XIN.size() == 1;
    }

    
    @Override
    public void setExpanded() {
        for(int i = 0, popSize = _stashedTerminals.size(); i < popSize; i++) {
            XIN.add(_stashedTerminals.pop());
        }
        while (XIN.size() < IN_TERMS_WITH_EXT) {
            XIN.add(new TerminalControlInput(this, X_EXTERNAL_TERMINAL, -XIN.size() - 1));
        }
    }        

    @Override
    public boolean isExternalSet() {
        return _isExternalSet.getValue();
    }
    
    
    @Override
    public void setExternalUsed(final boolean value) {
        _isExternalSet.setUserValue(value);
    }
            
}
