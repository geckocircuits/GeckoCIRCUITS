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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;

import java.net.URI;
import java.awt.Dimension;
import javax.swing.*;

public class DialogNonLinearity extends GeckoDialog {    
    private final NonLinearDialogPanel _content;
    private static final Dimension _windowSize = new Dimension(800, 600);
    public DialogNonLinearity(final AbstractNonLinearCircuitComponent elementLK, final boolean yAxisLog) {
        super(GeckoSim._win, true);
        _content = new NonLinearDialogPanel(this, elementLK, yAxisLog);        
        setContentPane(_content);
        try {
            this.setIconImage((new ImageIcon(URI.create(GlobalFilePathes.PFAD_PICS_URL + "gecko.gif").toURL())).getImage());
        } catch (Exception e) { // NOPMD
            // Exception intentionally ignored: Icon loading is optional - dialog works without it
        }
        
        setPreferredSize(_windowSize);                
        setSize(_windowSize);                                
        pack();
        
    }

    public void setCharacteristicLoadedFromFile(GeckoFile geckoFile) {
        _content.setCharacteristicLoadedFromFile(geckoFile);
    }
    
    
    
}