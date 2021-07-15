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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileManagerWindow;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.DialogVerlusteDetail;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.VerlustBerechnungDetailed;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

final class JPanelHalbleiterDetailButtons extends JPanel {
    private static final int BUTTON_WIDTH = 500;
    
    private final FormatJTextField _jlS2 = new FormatJTextField("-");
    private final FormatJTextField _jlS2b = new FormatJTextField();
    private final VerlustBerechnungDetailed _detailedLosses;
    private final JButton jbS2edit = GuiFabric.getJButton(I18nKeys.EDIT_SEMICONDUCTOR);
    
    JPanelHalbleiterDetailButtons(final VerlustBerechnungDetailed detailedLosses) {
        super();
        _detailedLosses = detailedLosses;
        setBorder(BorderFactory.createTitledBorder("Detailed Loss Model"));
        setLayout(new GridLayout(5, 1));
        
        checkFileLink();                
        
        JButton jbS2load = GuiFabric.getJButton(I18nKeys.LOAD_SEMICONDUCTOR);
        jbS2load.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        if (Fenster.IS_APPLET) {
            jbS2load.setEnabled(false);
        }
        
        jbS2load.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {                                
                GeckoFileManagerWindow dialog = new GeckoFileManagerWindow(_detailedLosses, ".scl", "Semicond. losses", _jlS2, _jlS2b, jbS2edit);
                dialog.setVisible(true);                                
                checkFileLink();
            }
        });
        //--------------        
        final Window parentWindow = SwingUtilities.getWindowAncestor(JPanelHalbleiterDetailButtons.this);
        jbS2edit.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));        
        jbS2edit.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                //focusIstFreigegeben= true;  // damit kann der Focus an das nun zu Oeffnende Fenster weitergegeben werden                                
                final DialogVerlusteDetail dialog = DialogVerlusteDetail.fabricCreateExisiting(_detailedLosses, parentWindow);
                dialog.setVisible(true);
                checkFileLink();
            }
        });

        //--------------
        JButton jbS2new = GuiFabric.getJButton(I18nKeys.CREATE_NEW);
        jbS2new.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));

        jbS2new.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                final DialogVerlusteDetail dialog = DialogVerlusteDetail.fabricCreateNew(_detailedLosses, parentWindow);
                dialog.setVisible(true);
                checkFileLink();
            }
        });        
        
        add(_jlS2b);
        add(_jlS2);
        add(jbS2load);
        add(jbS2edit);
        add(jbS2new);
        setEnabled(false);
        
        
    }

    
    
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled); //To change body of generated methods, choose Tools | Templates.
        for (Component comp : getComponents()) {
            comp.setEnabled(enabled);
        }
    }

    private void checkFileLink() {
        final boolean isFileLinkOK = _detailedLosses.pruefeLinkAufHalbleiterDatei();  // ist die Datei mit der Verlustbeschreibung ueberhaupt vorhanden?
        //
        if (isFileLinkOK) {  // Datei vorhanden und OK

            _jlS2b.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
            final String absolutePath = _detailedLosses.lossFile.getCurrentAbsolutePath();
            _jlS2b.setText(_detailedLosses.lossFile.getName());  // Datei

            String separator = System.getProperty("file.separator");
            if (absolutePath.contains(separator)) {
                _jlS2.setText(absolutePath.substring(0, absolutePath.lastIndexOf(separator)));  // Pfad
            } else { // when using file from windows on linux computers, the upper approach generates a problem.
                _jlS2.setText(absolutePath.substring(0, absolutePath.lastIndexOf('\\')));
            }

        } else {  // Datei nicht gefunden oder fehlerhaft            
            //jlS2.setVisible(false);
            _jlS2.setText("-");
            _jlS2b.setText("Loss file not found!");
        }
        Color fS2 = (isFileLinkOK) ? Color.decode("0x006400") : Color.red;
        if (Fenster.IS_APPLET) {
            fS2 = Color.decode("0x006400");  // weil im Applet-Modus keine Datei verwendet wird, ist die Farbe hier nie 'rot'
        }
        int cls = 20;
        _jlS2.setColumns(cls);
        //jlS2.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        _jlS2b.setColumns(cls);
        _jlS2.setForeground(fS2);
        _jlS2b.setForeground(fS2);
    }
    
}
