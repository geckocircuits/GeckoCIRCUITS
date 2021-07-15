/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.ControlSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.SmallSignalCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerSmallSignalAnalysis extends RegelBlock {
    
    private static final double DEFAULT_AMPLITUDE = 0.005;
    private static final double DEFAULT_FREQ_START = 50.0;
    private static final double DEFAULT_FREQ_END = 10000.0;
    
    public static final ControlTypeInfo TYPE_INFO = 
            new ControlTypeInfo(ReglerSmallSignalAnalysis.class, "ANALYSIS", I18nKeys.SMALL_SIGNAL_ANALYIS);
    
    private final int BLOCK_WIDTH = 6;
    
    final UserParameter<Double> _amplitude = UserParameter.Builder.<Double>start("Amplitude", DEFAULT_AMPLITUDE).
            longName(I18nKeys.AMPLITUDE).
            shortName("Ampl").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, -1).
            build();
    
    final UserParameter<Double> _fBase = UserParameter.Builder.<Double>start("StartFreq", DEFAULT_FREQ_START).
            longName(I18nKeys.FREQ_START).
            shortName("fBase").
            unit("Hz").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, -1).
            build();
    
    final UserParameter<Double> _fMax = UserParameter.Builder.<Double>start("EndFreq", DEFAULT_FREQ_END).
            longName(I18nKeys.FREQ_END).
            shortName("fMax").
            unit("Hz").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, -1).
            build();        
        
    
    final UserParameter<SSAShape> _signalType = UserParameter.Builder.<SSAShape>start("signalTypeNew", SSAShape.RECTANGLE).
            longName(I18nKeys.SIGNALTYPE_SSA).
            shortName("shape").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, -1).
            build();
        
    
    public final UserParameter<Boolean> _doAddOutput = UserParameter.Builder.
            <Boolean>start("addOutput", false).
            longName(I18nKeys.IF_TRUE_USE_NONLINEAR_CHARACTERISTIC).
            shortName("addOutput").            
            arrayIndex(this, -1).
            build();                               
    
    
    TerminalControlInputWithLabel externalExcitationTerm = new TerminalControlInputWithLabel(this, -4, -2, "excitation");
    
    public SmallSignalCalculator _lastCalculator;
    
    
    public ReglerSmallSignalAnalysis() {
        super();          
        XIN.add(new TerminalControlInputWithLabel(this, -4, XIN.size(), "measure"));
        XIN.add(new TerminalControlInputWithLabel(this, -4, -XIN.size(), "signal"));        
        YOUT.add(new TerminalControlOutputWithLabel(this, 3, YOUT.size(), "out"));
        _signalType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(_signalType.getValue() == SSAShape.EXTERNAL) {
                    XIN.add(externalExcitationTerm);
                } else {
                    XIN.remove(externalExcitationTerm);
                }
                
            }
        });
    }

    @Override
    public int getBlockWidth() {
        return BLOCK_WIDTH * dpix;
    }                                                
    
    @Override
    // Is called if SSA block is on worksheet and simulation is initiated and running
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        
        SmallSignalCalculator lastCalculator = new SmallSignalCalculator(_amplitude.getValue(), _fBase.getValue(), 
                _fMax.getValue(), _signalType.getValue(), XIN.size(), YOUT.size(), _doAddOutput.getValue());
        _lastCalculator = lastCalculator;
        return lastCalculator;
    }
    
    @Override
    protected String getCenteredDrawString() {
        return "SSA";
    }                

    @Override
    public String[] getOutputNames() {
        return new String[]{"SmallSignal"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[] {I18nKeys.SMALL_SIGNAL_OUTPUT_FOR_ANALYSIS};
    }

    @Override
    // Is called if one drags the SSA block on the worksheet
    protected Window openDialogWindow() {
        return new DialogSmallSignalAnalysis(this);
    }
    
}