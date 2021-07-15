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
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlInput;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlOutput;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.DEMUXCalculator;
import ch.technokrat.gecko.geckocircuits.control.javablock.ReglerJavaFunction;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class ReglerDemux extends RegelBlock implements VariableTerminalNumber {

    private static final double DA_CONST = 0.5;
    private static final double WIDTH = 0.3;
    private static final int DEFAULT_TERM_NUMBER = 3;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerDemux.class, "DEMUX", I18nKeys.CONTROL_DEMUX);
    public ReglerJavaFunction _connectedJavaBlock;
    public int _connectedJavaOutputIndex;

    final UserParameter<Integer> _outputTerminalNumber = UserParameter.Builder.
            <Integer>start("tn", 3).
            longName(I18nKeys.NO_OUTPUT_TERMINALS).
            shortName("numberOutputTerminals").
            arrayIndex(this, -1).
            build();
    
    public ReglerDemux() {
        super();
        this.setOutputTerminalNumber(DEFAULT_TERM_NUMBER);
        XIN.add(new TerminalControlInput(this, -2, -1));
        
        _outputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOutputTerminalNumber(_outputTerminalNumber.getValue());
            }
        });
        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"0", "1", "2", "etc."};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.DEMUXED_VECTOR_SIGNAL, I18nKeys.DEMUXED_VECTOR_SIGNAL, I18nKeys.DEMUXED_VECTOR_SIGNAL, I18nKeys.DEMUXED_VECTOR_SIGNAL};
    }

    @Override
    void setInputSignal(int inputIndex, RegelBlock outputBlock, int outputIndex) {        
        super.setInputSignal(inputIndex, outputBlock, outputIndex); //To change body of generated methods, choose Tools | Templates.
        if(outputBlock instanceof ReglerJavaFunction) {
            _connectedJavaBlock = (ReglerJavaFunction) outputBlock;
            _connectedJavaOutputIndex = outputIndex;
        } else {
            _connectedJavaBlock = null;
        }
    }
    
    

    @Override
    public void setInputTerminalNumber(final int number) {
        // here, we don't have input terminals!
    }
    
    

    @Override
    public void setOutputTerminalNumber(final int number) {
               
        while (YOUT.size() > number) {
            YOUT.pop();
        }

        while (YOUT.size() < number) {
            YOUT.add(new TerminalControlOutput(this, 1, -YOUT.size()));
        }
        
        if(_outputTerminalNumber != null) {
            int newsize = YOUT.size();
            if(_outputTerminalNumber.getValue() != newsize) {
                _outputTerminalNumber.setUserValue(newsize);
            }                        
        }
    }

    @SuppressWarnings("PMD")
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new DEMUXCalculator(YOUT.size(), this);                
    }

    @SuppressWarnings("PMD")
    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;
        final Color origColor = graphics.getColor();

        graphics.setColor(getBackgroundColor());
        int termNumber = YOUT.size();
        xKlickMin = (int) (dpix * (xPos - WIDTH));
        xKlickMax = (int) (dpix * (xPos + WIDTH));
        yKlickMin = (int) (dpix * (yPos - WIDTH));
        yKlickMax = (int) (dpix * (yPos + 1.0 * termNumber));

        graphics.fillRect((int) (dpix * (xPos - 0.4)), (int) (dpix * (yPos - 0.4)), (int) (dpix * (2 * 0.4)), (int) (dpix * termNumber));
        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (xPos - 0.4)), (int) (dpix * (yPos - 0.4)), (int) (dpix * (2 * 0.4)), (int) (dpix * termNumber));
        graphics.setColor(origColor);
    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }        

    @Override
    protected Window openDialogWindow() {
        return new DialogMuxDemux(this);
    }

}
