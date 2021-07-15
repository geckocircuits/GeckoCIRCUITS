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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 *
 * @author andy
 */
public class GeckoDialog extends JDialog {
    

    private final Component _parent;

    public GeckoDialog(final Window parent, final boolean modal) {       
        super(parent);        
        setModal(modal);
        _parent = parent;
        init();        
    }
    
    public GeckoDialog(final Dialog parent, final boolean modal) {       
        super(parent);        
        setModal(modal);
        _parent = parent;
        init();        
    }
        

    private void init() {
        try {
            this.setIconImage(new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif")).getImage());
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        this.setLocationRelativeTo(_parent);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * close dialog on escape!
     */
    @Override
    protected final JRootPane createRootPane() {
        final KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        final Action actionListener = new AbstractAction() {

            public void actionPerformed(final ActionEvent actionEvent) {
                setVisible(false);
            }
        };

        if(rootPane == null) {
            rootPane = new JRootPane();
        }
        
        final InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        
        rootPane.getActionMap().put("ESCAPE", actionListener);
        return rootPane;
    }        
    
    public void setGeckoIconImage() {
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
    }
    
}
