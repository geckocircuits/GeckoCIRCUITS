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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;

public class DialogAbout extends JDialog {
    private Image geckoBild;
    private String releaseDate = null;

    public static final String RELEASE_DATE = "2018.02.15";
    public static final String VERSION = "GeckoCIRCUITS - v1.75";
    // changing this number will give a warning message, when
    // someone will open a model file with the old version number!
    public static final int RELEASENUMBER = 175;
    
    // increase the build-number whenever some user gets a a new update of the program.
    // Especially, this should be the case when the version that is downloadable
    // is upgraded due to a minor bug fix.
    public static final int BUILD_NUMBER = 68;
    
    
    public DialogAbout() {
        super.setModal(true);
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());            
            geckoBild = (new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "GeckoSimulationsLogo_50.png"))).getImage();
        } catch (Exception e) {
            
        }
        
        
        try {
            DateFormat dFormat = new SimpleDateFormat("yyyy.MM.dd");
            Date rDate = dFormat.parse(RELEASE_DATE);
            dFormat = new SimpleDateFormat("MMM yyyy", Locale.US);
            releaseDate = dFormat.format(rDate);
        } catch (ParseException pe) {
        }
        //------------------------
        this.setTitle(" About");
        //this.setBackground(Color.decode("0x8cc63f"));
        this.baueGUI();
        this.pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);        
    }

    private void baueGUI() {
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        //------------------
        JButton knOK = GuiFabric.getJButton(I18nKeys.OK);
        knOK.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        JPanel px = new JPanel();
        px.add(knOK);
        JTextArea jtx = new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
                
        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(Color.white);
        jtx.setEditable(false);
        //------------------
        JPanel jpGecko = new JPanel() {

            @Override
            public void paint(Graphics g) {
                g.drawImage(geckoBild, 0, 0, null);
            }
        };
        jpGecko.setPreferredSize(new Dimension(242, 92));  // Bildabmessungen
        //------------------
        JTextArea jtx2 = new JTextArea();
        jtx2.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        
        String professionalOrOpenSource = " professional ";
        if(StartupWindow.testOpenSourceVersion()) {
            professionalOrOpenSource = " open-source ";
        }
    
        
        String licenseString = "No license available.";
                                                
        
        jtx2.setText("\n" + VERSION + professionalOrOpenSource +  
                "\n" + "Build number: " + BUILD_NUMBER +
                "\nrelased " + releaseDate + "\n\n" + 
                licenseString + 
                "\n\nwritten by\nAndreas Müsing\nAndrija Stupar");
        jtx2.setLineWrap(true);
        jtx2.setWrapStyleWord(true);
        jtx2.setBackground(Color.white);
        jtx2.setEditable(false);
        //------------------
        JPanel jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(new EtchedBorder());
        jpM.setBackground(Color.white);
        jpM.add(jtx, BorderLayout.NORTH);
        jpM.add(jpGecko, BorderLayout.CENTER);
        jpM.add(jtx2, BorderLayout.SOUTH);
        con.add(jpM, BorderLayout.CENTER);
        con.add(px, BorderLayout.SOUTH);
        this.pack();
        this.setResizable(false);
    }
}