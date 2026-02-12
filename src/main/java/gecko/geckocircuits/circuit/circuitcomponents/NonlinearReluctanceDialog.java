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
package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.geckocircuits.circuit.NonLinearDialogPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;

class NonlinearReluctanceDialog extends DialogElementLK<NonLinearReluctance> {
    private NonLinearDialogPanel _nonLinearDialogPanel;
    
    public NonlinearReluctanceDialog(final NonLinearReluctance parent) {
        super(parent);
        
        setPreferredSize(new Dimension(700, 500));
        this.pack();
    }

    @Override
    protected void baueGUIIndividual() {        
        JPanel pINy = new JPanel();
        _nonLinearDialogPanel = new NonLinearDialogPanel(this, element, false);
        pINy.setLayout(new BorderLayout());
        pINy.add(_nonLinearDialogPanel, BorderLayout.CENTER);
        con.add(pINy, BorderLayout.CENTER);
        _nonLinearDialogPanel._jbOK.addActionListener(okActionListener);
        jPanelButtonOkCancel.setVisible(false);
    }  
    
}
