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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import java.net.URL;
import java.text.NumberFormat;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;

abstract class PreviewDialog extends JDialog {
    final NumberFormat nf = NumberFormat.getNumberInstance();
    
    protected PreviewDialog(final JDialog parent) {
        super(parent);        
        try {
            setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        getContentPane().add(createComponent());
        pack();
        setResizable(false);        
        setLocation(parent.getLocationOnScreen().x + parent.getWidth(), parent.getLocationOnScreen().y);        
    }           
    
    abstract JComponent createComponent();
    
}
