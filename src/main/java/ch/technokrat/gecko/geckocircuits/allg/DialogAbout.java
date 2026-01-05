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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.net.MalformedURLException;
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

import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * About dialog for GeckoCIRCUITS application.
 *
 * Displays application version information, build number, release date,
 * license type, and author information. This dialog is modal and provides
 * a brief overview of the current GeckoCIRCUITS installation.
 */
public class DialogAbout extends JDialog {
    private static final long serialVersionUID = 1L;
    private transient Image geckoBild;
    private String releaseDate = null;

    public static final String RELEASE_DATE = "2026.??.??";
    public static final String VERSION = "GeckoCIRCUITS - v?.?";
    public static final int RELEASENUMBER = 202;
    public static final int BUILD_NUMBER = 82;
    

    /**
     * Creates and initializes the About dialog.
     *
     * Loads application icons, formats the release date, and builds the GUI.
     * The dialog is modal and will dispose when closed.
     */
    @SuppressWarnings("this-escape")  // JDialog superclass is fully initialized, setIconImage() call is safe; refactoring would make code more complex without benefit
    public DialogAbout() {
        super.setModal(true);
        try {
            @SuppressWarnings("deprecation")
            URL url1 = new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif");
            this.setIconImage((new ImageIcon(url1)).getImage());
            @SuppressWarnings("deprecation")
            URL url2 = new URL(GlobalFilePathes.PFAD_PICS_URL, "GeckoSimulationsLogo_50.png");
            geckoBild = (new ImageIcon(url2)).getImage();
        } catch (MalformedURLException | RuntimeException e) {

        }


        try {
            DateFormat dFormat = new SimpleDateFormat("yyyy.MM.dd");
            Date rDate = dFormat.parse(RELEASE_DATE);
            dFormat = new SimpleDateFormat("MMM yyyy", Locale.US);
            releaseDate = dFormat.format(rDate);
        } catch (ParseException pe) {
        }

        this.setTitle(" About");
        this.baueGUI();
        this.pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Builds the GUI components for the About dialog.
     *
     * Creates the layout containing the logo, version information,
     * build number, release date, license information, and an OK button.
     */
    private void baueGUI() {
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());

        JButton knOK = GuiFabric.getJButton(I18nKeys.OK);
        knOK.addActionListener(e -> dispose());
        
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

        String licenseType = "GNU GPL 3.0";
        if (StartupWindow.testOpenSourceVersion()) {
            licenseType = "open-source";
        }

        jtx2.setText("\n" + VERSION + " " + licenseType +
                "\n" + "Build number: " + BUILD_NUMBER +
                "\nrelased " + releaseDate + "\n\n" +
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
