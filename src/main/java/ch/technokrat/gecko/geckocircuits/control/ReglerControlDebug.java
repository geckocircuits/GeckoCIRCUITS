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

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

public class ReglerControlDebug extends RegelBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerControlDebug.class,"CTRL_DEBUG", I18nKeys.DEBUGGING_STEP);
    private ControlDebugWindow _debugWindow;
    
    public ReglerControlDebug() {
        super(0, 0);        
        XIN.add(new TerminalControlInputWithLabel(this, -4, 0, "1"));
        XIN.add(new TerminalControlInputWithLabel(this, -4, -1, "2"));
        XIN.add(new TerminalControlInputWithLabel(this, -4, -2, "3"));
        XIN.add(new TerminalControlInputWithLabel(this, -4, -3, "Trig"));
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new AbstractControlCalculatable(0, 0) {
            @Override
            public void berechneYOUT(double deltaT) {
                
            }
        };
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    protected Window openDialogWindow() {
        if(_debugWindow == null) {
            _debugWindow = new ControlDebugWindow();
        }        
        _debugWindow.setVisible(true);
        _debugWindow.toFront();
        return _debugWindow;
    }
    
    
    
    @Override
    public int getBlockHeight() {
        return dpix * 4;
    }

    @Override
    public int getBlockWidth() {
        return dpix * 6;
    }       
}
