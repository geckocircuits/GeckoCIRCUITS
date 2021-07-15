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

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSheet;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

class MutualInductanceDialog extends DialogElementLK<MutualInductance> {
    private AbstractBlockInterface _selectedCoupling1;
    private AbstractBlockInterface _selectedCoupling2;
    private final JCheckBox _jcbM = new JCheckBox(I18nKeys.SHOW_LINES.getTranslation());
    
    public MutualInductanceDialog(final MutualInductance parent) {
        super(parent);
    }

    @Override
    protected void baueGUIIndividual() {        

        JPanel pIN = createParameterPanel(element._couplingCoefficient);                        
        _jcbM.setSelected(element._showLines.getValue());
        
        pIN.add(_jcbM);
        
        JPanel jpR = new JPanel();
        jpR.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                I18nKeys.MAGNETICALLY_COUPLED.getTranslation(), TitledBorder.LEFT, TitledBorder.TOP));
        //
        CircuitSheet parent = element.getParentCircuitSheet();
        final List<AbstractBlockInterface> alleElementLK = parent.getLocalComponents(ConnectorType.LK);
        String[] labelListeInduktTemp = new String[alleElementLK.size()];
        int[] idListeInduktTemp = new int[alleElementLK.size()];
        int anzL = 0;
        for (AbstractBlockInterface elem : alleElementLK) {
            if (elem instanceof InductorCoupable) {
                labelListeInduktTemp[anzL] = elem.getStringID();
                idListeInduktTemp[anzL] = alleElementLK.indexOf(elem);
                anzL++;
            }
        }
        String[] labelListeInduktLK = new String[anzL];
        final int[] idListeInduktLK = new int[anzL];
        System.arraycopy(labelListeInduktTemp, 0, labelListeInduktLK, 0, anzL);
        System.arraycopy(idListeInduktTemp, 0, idListeInduktLK, 0, anzL);
        //
        if (anzL >= 2) {
            final JComboBox combo = new JComboBox(labelListeInduktLK);
            combo.setForeground(GlobalColors.farbeFertigElementLK);
            int indexCombo = -1;
            for (int i1 = 0; i1 < labelListeInduktLK.length; i1++) {
                if (element.getParameterString()[0].equals(labelListeInduktLK[i1])) {
                    indexCombo = i1;
                    _selectedCoupling1 = element.getComponentCoupling()._coupledElements[0];
                }
            }
            combo.setSelectedIndex(indexCombo);
            combo.addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent ae) {
                    for (AbstractBlockInterface search : alleElementLK) {
                        if (search.getStringID().equals(combo.getSelectedItem().toString())) {
                            _selectedCoupling1 = search;                            
                        }
                    }

                    double[] par = element.getParameter();
                    par[5] = idListeInduktLK[combo.getSelectedIndex()];
                    element.setParameter(par);
                }
            });
            final JComboBox combo2 = new JComboBox(labelListeInduktLK);
            indexCombo = -1;
            for (int i1 = 0; i1 < labelListeInduktLK.length; i1++) {
                if (element.getParameterString()[1].equals(labelListeInduktLK[i1])) {
                    indexCombo = i1;
                    _selectedCoupling2 = element.getComponentCoupling()._coupledElements[1];
                }
            }
            combo2.setSelectedIndex(indexCombo);
            combo2.setForeground(GlobalColors.farbeFertigElementLK);
            combo2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {

                    for (AbstractBlockInterface search : alleElementLK) {
                        if (search.getStringID().equals(combo2.getSelectedItem().toString())) {
                            _selectedCoupling2 = search;                            
                        }
                    }

                    double[] par = element.getParameter();
                    par[6] = idListeInduktLK[combo2.getSelectedIndex()];
                    element.setParameter(par);
                }
            });
            jpR.setLayout(new GridLayout(1, 2));
            jpR.add(combo);
            jpR.add(combo2);
        } else {
            jpR.setLayout(new BorderLayout());
            JLabel jl2M = labelFabric(I18nKeys.NO_LC_FOUND.getTranslation());
            jpR.add(jl2M, BorderLayout.CENTER);
        }

        JPanel jpR2 = new JPanel();
        jpR2.setLayout(new BorderLayout());
        jpR2.add(pIN, BorderLayout.NORTH);
        jpR2.add(jpR, BorderLayout.CENTER);
        JPanel platzhalter = new JPanel();
        platzhalter.setPreferredSize(new Dimension(20, 80));
        jpR2.add(platzhalter, BorderLayout.SOUTH);
        //
        JTabbedPane tabberM = new JTabbedPane();
        tabberM.addTab("Parameter", jpR2);
        tabberM.addTab("Equations", this.schreibeFormeln_M());
        //
        con.add(tabberM, BorderLayout.CENTER);

    }
    

    // eine kleine Auflistung der Trafo-Gleichungen im Dialogfenster der magnetischen Kopplung
    protected JComponent schreibeFormeln_M() {
        final int bG = 230, hG = 180, x1 = 10, y1 = 30, y2 = y1 + 30, y3 = y2 + 30, y4 = y3 + 35, y5 = y4 + 30;
        JComponent comp = new JComponent() {
            public void paint(Graphics g) {
                g.setColor(Color.white);
                g.fillRect(0, 0, 999, 999);
                //g.fillRect(0,0,bG,hG);
                g.setColor(Color.black);
                //g.drawRect(0,0,bG-1,hG-1);
                //------------
                // (1) u1/u2= i2/i1= N1/N2
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("u   / u   =  i   / i   =  N   / N  ", x1, y1);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("   1       2          2     1            1         2", x1, y1 + 5);
                //------------
                // (2) L1/L2= (N1/N2)^2
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("L   / L   =  (N   / N  )", x1, y2);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("   1       2              1        2", x1, y2 + 5);
                g.drawString("2", x1 + 132, y2 - 5);
                //------------
                // (3) M= k*Sqrt[L1*L2]
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("M  =  k   (L  L   )", x1, y3);
                g.drawString(".", x1 + 53, y3 - 4);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("                        1     2", x1, y3 + 5);
                g.drawString("0.5", x1 + 110, y3 - 5);
                //------------
                // (4) Ls= (1-k)*L
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("L     = (1 - k)  L", x1, y4);
                g.drawString(".", x1 + 87, y4 - 4);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("    s,i                          i", x1, y4 + 5);
                //------------
                // (5) Lh= k*L
                g.setFont(GlobalFonts.FORMEL_DIALOG_GROSS);
                g.drawString("L     = k  L", x1, y5);
                g.drawString(".", x1 + 55, y5 - 4);
                g.setFont(GlobalFonts.FORMEL_DIALOG_KLEIN);
                g.drawString("    h,i                i", x1, y5 + 5);
                //------------
                g.setColor(Color.lightGray);
                g.drawLine(x1, y3 + 12, bG - x1, y3 + 12);
            }
        };
        comp.setPreferredSize(new Dimension(bG, hG));
        return comp;
    }

    @Override
    public void processInputIndividual() {
        element._showLines.setUserValue(_jcbM.isSelected());    
        ((ComponentCoupable) element).getComponentCoupling().setNewCouplingElementUndoable(0, _selectedCoupling1);
        ((ComponentCoupable) element).getComponentCoupling().setNewCouplingElementUndoable(1, _selectedCoupling2);
    }        
}
