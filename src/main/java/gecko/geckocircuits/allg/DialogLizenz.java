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
package gecko.geckocircuits.allg;

import gecko.i18n.GuiFabric;
import gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import java.net.URL;
import javax.swing.border.EtchedBorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;




@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Dialog stores license text buffer for display")
public class DialogLizenz extends JDialog implements WindowListener, ActionListener {
    private JButton knOK;
    private StringBuffer sbInfoTxt;

    public DialogLizenz (StringBuffer sbInfoTxt) {
        super.setModal(true);
        try {
            @SuppressWarnings("deprecation")
            URL url = new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif");
            this.setIconImage(new ImageIcon(url).getImage());
        } catch (Exception e) {
            // ignored: icon loading is optional
        }
        this.sbInfoTxt= sbInfoTxt;
        this.addWindowListener(this);
        this.setTitle(" Licence Information");
        this.baueGUI();
        this.setSize(240,200);
        this.setResizable(false);
        //------------------------
    }

    private void baueGUI () {
        Container con= this.getContentPane();
        con.setLayout(new BorderLayout());
        //------------------
        knOK= GuiFabric.getJButton(I18nKeys.OK);
        knOK.setActionCommand("OK");
        knOK.addActionListener(this);
        JPanel px= new JPanel();
        px.add(knOK);
        //------------------
        JPanel jpM= new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(new EtchedBorder());
        //
        JTextArea jtx= new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        //
        jtx.setText(sbInfoTxt.toString());
        //
        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(this.getBackground());
        jtx.setEditable(false);
        //
        jpM.add(jtx, BorderLayout.CENTER);
        //------------------
        con.add(jpM, BorderLayout.CENTER);
        con.add(px, BorderLayout.SOUTH);
    }





    @Override
    public void actionPerformed (ActionEvent ae) {
        // 'OK'-Knopf gedrueckt --> Dialog kann verlassen werden
        this.dispose();
    }



    //------------------------------------------------
    @Override
    public void windowDeactivated (WindowEvent we) { this.requestFocus(); }
    @Override
    public void windowActivated (WindowEvent we) { /* no-op */ }
    @Override
    public void windowDeiconified (WindowEvent we) { /* no-op */ }
    @Override
    public void windowIconified (WindowEvent we) { /* no-op */ }
    @Override
    public void windowClosed (WindowEvent we) { /* no-op */ }
    @Override
    public void windowClosing (WindowEvent we) { /* no-op */ }
    @Override
    public void windowOpened (WindowEvent we) { /* no-op */ }
    //------------------------------------------------

}

