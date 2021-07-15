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

import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

final class OperationalAmplifierDialog extends DialogElementLK<OperationalAmplifier> {

    public OperationalAmplifierDialog(final OperationalAmplifier parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {
        JTabbedPane tabberOPV1 = this.createGUI_OPV1();
        con.add(tabberOPV1, BorderLayout.CENTER);

    }

    protected JTabbedPane createGUI_OPV1() {   
        JPanel mainParameter = createParameterPanel(element.gainValue, element.inputResistance, element.outputResistance, element.frequencyDependency);
        mainParameter.setBorder(new TitledBorder("Main Parameters"));
        
        JPanel voltDivPanel = createParameterPanel(element.voltageDividerRa, element.voltageDividerRb);
        voltDivPanel.setBorder(new TitledBorder("Voltage Divider"));
        
        JPanel limitPanel = createParameterPanel(element.voltageLimitationMax, element.voltageLimitationMin);
        limitPanel.setBorder(new TitledBorder("Voltage-Limitation"));
                
        JPanel jpOx = new JPanel();
        //jpOx.setLayout(new BorderLayout());
        jpOx.add(mainParameter, BorderLayout.WEST);
        jpOx.add(voltDivPanel, BorderLayout.EAST);
        JPanel jpOy = new JPanel();
        jpOy.setLayout(new BorderLayout());
        jpOy.add(jpOx, BorderLayout.WEST);
        JPanel jpOz = new JPanel();
        jpOz.setLayout(new BorderLayout());
        jpOz.add(jpOy, BorderLayout.NORTH);
        //================
        JPanel jpDefOPV1 = new JPanel();
        jpDefOPV1.setLayout(new BorderLayout());
        final JComponent jcOPV1 = new JComponent() {
            public void paint(final Graphics graphics) {
                try {
                    graphics.setColor(Color.white);
                    graphics.fillRect(0, 0, 999, 999);
                    Image img = (new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "dialog_opv_model.png"))).getImage();
                    graphics.drawImage(img, 10, 10, new JFrame());
                } catch (Exception e) {
                    System.out.println(e + "   er4b");
                }
            }
        };
        jcOPV1.setPreferredSize(new Dimension(442 + 20, 130));
        jpDefOPV1.add(jcOPV1, BorderLayout.CENTER);
        //================
        JPanel jpOPVadvx1 = new JPanel();
        jpOPVadvx1.setLayout(new BorderLayout());
        jpOx.add(limitPanel);
        //jpOPVadvx1.add(jpOPVadv2, BorderLayout.CENTER);
        JPanel jpOPVadvx2 = new JPanel();
        jpOPVadvx2.setLayout(new BorderLayout());
        jpOPVadvx2.add(jpOPVadvx1, BorderLayout.SOUTH);
        JTabbedPane tabberOPV1 = new JTabbedPane();
        tabberOPV1.addTab("Parameter", jpOz);
        tabberOPV1.addTab("Model", jpDefOPV1);
        return tabberOPV1;
    }            
}
