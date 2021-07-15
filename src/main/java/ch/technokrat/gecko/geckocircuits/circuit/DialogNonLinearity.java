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
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileChooser;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileManagerWindow;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import ch.technokrat.gecko.geckocircuits.newscope.GraferV4;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSettings;
import ch.technokrat.gecko.geckocircuits.newscope.SimpleGraferPanel;

import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.File;
import java.util.ArrayList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class DialogNonLinearity extends GeckoDialog {    
    private final NonLinearDialogPanel _content;
    private static final Dimension _windowSize = new Dimension(800, 600);
    public DialogNonLinearity(final AbstractNonLinearCircuitComponent elementLK, final boolean yAxisLog) {
        super(GeckoSim._win, true);
        _content = new NonLinearDialogPanel(this, elementLK, yAxisLog);        
        setContentPane(_content);
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
        
        setPreferredSize(_windowSize);                
        setSize(_windowSize);                                
        pack();
        
    }

    public void setCharacteristicLoadedFromFile(GeckoFile geckoFile) {
        _content.setCharacteristicLoadedFromFile(geckoFile);
    }
    
    
    
}