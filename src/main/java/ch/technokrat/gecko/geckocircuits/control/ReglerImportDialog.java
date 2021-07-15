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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileManagerWindow;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author andy
 */
class ReglerImportDialog extends DialogElementCONTROL<ReglerSignalSource>{

    private FormatJTextField jtfImportStatus;  // fuer das Importieren externer Zeitverlaeufe, die dann vom SIGNAL-Block ausgegeben werden    
    
    public ReglerImportDialog(ReglerSignalSource element) {
        super(element);
    }

    @Override
    void baueGuiIndividual() {     
        JTextArea jtx = new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx.setText("Data Format (Space-Separator)[ time  -  value ]");
        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(Color.white);
        jtx.setEditable(false);
        JPanel jpIMPORT = new JPanel();
        jpIMPORT.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import ASCII File", TitledBorder.LEFT, TitledBorder.TOP));
        jpIMPORT.setLayout(new BorderLayout());
        
        //         
        /*
         * JLabel jlIN1= new JLabel(TxtI.ti_ctrSIGd1_DialogElementCONTROL); jlIN1.setFont(TxtI.ti_Font_A);
         * jlIN1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1); jpIMPORT.add(jlIN1); JLabel jlIN2= new
         * JLabel(TxtI.ti_ctrSIGd2_DialogElementCONTROL); jlIN2.setFont(TxtI.ti_Font_A);
         * jlIN2.setForeground(GlobalColors.LAB_COLOR_DIALOG_1); jpIMPORT.add(jlIN2); JLabel jlIN3= new JLabel("");
         * jlIN3.setFont(TxtI.ti_Font_A); jlIN3.setForeground(GlobalColors.LAB_COLOR_DIALOG_1); //jpIMPORT.add(jlIN3);
         */
        JButton jbImport = GuiFabric.getJButton(I18nKeys.IMPORT_DATA);
        final DialogElementCONTROL ich = this;
        final FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                if ((f.getName().endsWith(".txt")) || (f.getName().endsWith(".dat"))) {
                    return true;
                } else {
                    return false;
                }
            }

            public String getDescription() {
                return new String("Data File, Space-Spr. (*.dat, *.txt)");
            }
        };
        //-----------
        jbImport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                GeckoFileManagerWindow fileManager = new GeckoFileManagerWindow(element, ".dat", "Data file, space-separated", true);
                fileManager.setVisible(true);
                //------------
                String datnam = ((ReglerSignalSource) element).getDatnam();
                if (datnam == null) {
                    datnam = GlobalFilePathes.DATNAM_NOT_DEFINED;
                }
                if (datnam.equals(GlobalFilePathes.DATNAM_NOT_DEFINED)) {
                    jtfImportStatus.setText("No external data file specified");
                    jtfImportStatus.setForeground(Color.red);
                } else {
                    jtfImportStatus.setText(datnam);
                    jtfImportStatus.setForeground(Color.decode("0x006400"));
                }
                //-----------
            }
        });
        JPanel jpCheck = new JPanel();
        jpCheck.setLayout(new BorderLayout());
        jtfImportStatus = new FormatJTextField();
        jtfImportStatus.setColumns(25);
        String datnam = ((ReglerSignalSource) element).getDatnam();
        if (datnam == null) {
            datnam = GlobalFilePathes.DATNAM_NOT_DEFINED;
        }
        if (datnam.equals(GlobalFilePathes.DATNAM_NOT_DEFINED)) {
            jtfImportStatus.setText("No external data file specified");
            jtfImportStatus.setForeground(Color.red);
        } else {
            jtfImportStatus.setText(datnam);
            jtfImportStatus.setForeground(Color.decode("0x006400"));
        }
        
        //
        
        JPanel jpSL4 = new JPanel();
        jpSL4.setLayout(new BorderLayout());
        JPanel jpSL5 = new JPanel();
        jpSL5.add(jbImport);
        jpSL4.add(jpSL5, BorderLayout.NORTH);
        jpSL4.add(jtfImportStatus, BorderLayout.CENTER);
        jpIMPORT.add(jtx, BorderLayout.CENTER);
        jpIMPORT.add(jpSL4, BorderLayout.SOUTH);
        
        jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.add(jpIMPORT, BorderLayout.CENTER);        
        
    }            
}
