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
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.SparseMatrixCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

import java.awt.Window;

public final class ReglerSPARSEMATRIX extends RegelBlock {

    private static final int X_POS_IN = -2;
    private static final int X_POS_OUT = 2;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerSPARSEMATRIX.class, "SPARSEMATRIX", I18nKeys.SPARSE_MATRIX_CONVERTER_CONTROL);
    
    public ReglerSPARSEMATRIX() {
        super();
        
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "fp"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "uN1"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "uN2"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "uN2"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "uNmax"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "uLmax"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "ufL"));
        XIN.add(new TerminalControlInputWithLabel(this, X_POS_IN, -XIN.size(), "phi2"));                                
        
        
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "s1+"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "s2+"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "s3+"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "s1-"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "s2-"));        
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "s3-"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "sA"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "sB"));
        YOUT.add(new TerminalControlOutputWithLabel(this, X_POS_OUT, -YOUT.size(), "sC"));                
        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"s1+", "s2+", "s3+", "s1-", "s2-", "s3-", "sA", "sB", "sC"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL,
            I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL, I18nKeys.GATE_SIGNAL};
    }

    @Override
    public int getBlockWidth() {
        return 3 * dpix;
    }

    @Override
    public double getXShift() {
        return 0.5;
    }
    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new SparseMatrixCalculator();
    }
    

    @Override
    protected String getCenteredDrawString() {
        return "";
    }            

    @Override
    protected final Window openDialogWindow() {
        return new DialogWindowWithoutInput(this);
    }
    
}
