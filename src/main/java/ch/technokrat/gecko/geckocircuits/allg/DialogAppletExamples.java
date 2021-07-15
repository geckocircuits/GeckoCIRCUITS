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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.border.EtchedBorder;

public class DialogAppletExamples extends JDialog implements WindowListener, ActionListener {

    //---------------------------------
    private String[] datnamExampleApplet;
    private Fenster callback;
    private JList list;
    //---------------------------------

    public DialogAppletExamples(String[] datnamExampleApplet, Fenster callback) {
        super.setModal(true);
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
        this.addWindowListener(this);
        this.datnamExampleApplet = datnamExampleApplet;
        this.callback = callback;
        //------------------------
        this.setTitle(" Applet-Mode: Select Example");
        this.baueGUI();
        this.pack();
        //this.setResizable(false);
        this.setVisible(true);
        //------------------------
    }

    private void baueGUI() {
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        //------------------
        JButton knOK = GuiFabric.getJButton(I18nKeys.OK);
        knOK.setActionCommand("OK");
        knOK.addActionListener(this);
        JButton knCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
        knCancel.setActionCommand("Cancel");
        knCancel.addActionListener(this);
        JPanel px = new JPanel();
        px.add(knOK);
        px.add(knCancel);
        //------------------
        JTextArea jtx = new JTextArea();        
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx.setText("Select a simulation model from the list. \n\n");
        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(Color.white);
        jtx.setEditable(false);
        //------------------
        list = new JList(datnamExampleApplet);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //------------------
        JPanel jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(new EtchedBorder());
        jpM.setBackground(Color.white);
        jpM.add(jtx, BorderLayout.NORTH);
        jpM.add(new JScrollPane(list), BorderLayout.CENTER);
        //
        con.add(jpM, BorderLayout.CENTER);
        con.add(px, BorderLayout.SOUTH);
        //------------------
    }

    public void actionPerformed(ActionEvent ae) {
        String ak = ae.getActionCommand();
        if (ak.equals("OK")) {
            int index = list.getSelectedIndex();
            if (index == -1) {
                return;
            }
            String sel = datnamExampleApplet[index];
            try {
                callback.openFile(sel);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(DialogAppletExamples.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.dispose();
        } else if (ak.equals("Cancel")) {
            this.dispose();
        }
    }

    //------------------------------------------------
    public void windowDeactivated(WindowEvent we) {
    }

    public void windowActivated(WindowEvent we) {
    }

    public void windowDeiconified(WindowEvent we) {
    }

    public void windowIconified(WindowEvent we) {
    }

    public void windowClosed(WindowEvent we) {
    }

    public void windowClosing(WindowEvent we) {
        this.dispose();
    }

    public void windowOpened(WindowEvent we) {
    }
    //------------------------------------------------
}
