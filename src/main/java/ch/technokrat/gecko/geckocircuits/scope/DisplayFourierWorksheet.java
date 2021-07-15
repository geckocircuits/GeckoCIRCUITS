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
package ch.technokrat.gecko.geckocircuits.scope;

import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

@Deprecated
class DisplayFourierWorksheet extends JPanel {

    //----------------------------
    private JTable table;
    private JScrollPane jsp;
    private Object[][] wsObj;
    private TechFormat cf = new TechFormat();
    //----------------------------
    private double[] cnSG, jnSG;
    private int nMin;
    //----------------------------

    public DisplayFourierWorksheet(double[] cnSG, double[] jnSG, int nMin) {
        this.cnSG = cnSG;
        this.jnSG = jnSG;
        this.nMin = nMin;
        this.schreibeData();
        this.baueGUI();
    }

    private void baueGUI() {
        this.removeAll();
        this.setLayout(new BorderLayout());
        jsp = new JScrollPane(table);
        this.add(jsp, BorderLayout.CENTER);
    }

    private void schreibeData() {
        final String[] header = new String[]{"n", "c_n", "phi_n [rad]"};
        final String[][] wsObj = new String[cnSG.length][header.length];
        for (int i1 = 0; i1 < wsObj.length; i1++) {
            for (int i2 = 0; i2 < wsObj[0].length; i2++) {
                if (i2 == 0) {
                    wsObj[i1][i2] = new String(cf.formatT(i1 + nMin, TechFormat.FORMAT_AUTO));
                } else if (i2 == 1) {
                    wsObj[i1][i2] = new String(cf.formatT(cnSG[i1], TechFormat.FORMAT_AUTO));
                } else if (i2 == 2) {
                    wsObj[i1][i2] = new String(cf.formatT(jnSG[i1], TechFormat.FORMAT_AUTO));
                }
            }
        }
        table = new JTable(wsObj, header) {
            // ueberschreiben, damit man nicht in den Daten herumeditieren kann -->

            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }
}