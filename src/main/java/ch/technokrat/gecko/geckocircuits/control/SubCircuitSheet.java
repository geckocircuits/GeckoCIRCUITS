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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TerminalCircuit;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.DialogSubCktSettings;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author andreas
 */
public class SubCircuitSheet extends CircuitSheet {

    private static final int BUTTON_WIDTH = 120;
    public final SubcircuitBlock _subBlock;
    private final JButton _upButton;
    private final JButton _settingsButton;
    private final JButton _newTerminalButton;
    private final JButton _infoButton;
    private JLabel _nameLabel;

    public SubCircuitSheet(SchematischeEingabe2 se, SubcircuitBlock subBlock) {
        super(se);
        _subBlock = subBlock;
        _upButton = GuiFabric.getJButton(I18nKeys.LEVEL_UP);
        _upButton.setBackground(new Color(0, 0, 0, 0));
        _upButton.setOpaque(false);
        _upButton.setFont(new Font("Arial", Font.PLAIN, 10));
        _upButton.setBounds(0, 0, BUTTON_WIDTH, 20);
        _upButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Fenster win = SchematischeEingabe2.Singleton.win;                
                SchematischeEingabe2.Singleton.setNewVisibleCircuitSheet(_subBlock.getParentCircuitSheet());
            }
        });

        _settingsButton = GuiFabric.getJButton(I18nKeys.SHEET_SETTINGS);
        _settingsButton.setBackground(new Color(0, 0, 0, 0));
        _settingsButton.setOpaque(false);
        _settingsButton.setFont(new Font("Arial", Font.PLAIN, 10));        
        _settingsButton.setBounds(BUTTON_WIDTH, 0, BUTTON_WIDTH, 20);
        _settingsButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                DialogSubCktSettings dialog = new DialogSubCktSettings(GeckoSim._win, false, _subBlock);
                dialog.setVisible(true);
            }
        });

        _newTerminalButton = GuiFabric.getJButton(I18nKeys.NEW_TERMINAL);
        _newTerminalButton.setBackground(new Color(0, 0, 0, 0));
        _newTerminalButton.setOpaque(false);
        _newTerminalButton.setFont(new Font("Arial", Font.PLAIN, 10));
        this.add(_newTerminalButton);
        _newTerminalButton.setBounds(BUTTON_WIDTH*2, 0, BUTTON_WIDTH, 20);
        _newTerminalButton.setToolTipText("<html>Create a new subcircuit terminal. Depending<br>on the component selection tab a circuit,<br>"
                + " control, or thermal terminal will be created.</html>");
        _newTerminalButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractBlockInterface newElement = null;                                
                switch(SchematischeEingabe2.Singleton.wireModeVersteckt) {
                    case SchematischeEingabe2.WIRE_MODE_LK:
                    case SchematischeEingabe2.WIRE_MODE_RELUCTANCE:
                    case SchematischeEingabe2.WIRE_MODE_THERM:
                        newElement = AbstractTypeInfo.fabricNew(AbstractTypeInfo.getTypeInfoFromClass(TerminalCircuit.class));
                        break;
                    case SchematischeEingabe2.WIRE_MODE_CONTROL:
                        newElement = AbstractTypeInfo.fabricNew(AbstractTypeInfo.getTypeInfoFromClass(ReglerTERMINAL.class));
                        break;
                    default:
                        assert false;
                }                
                SchematischeEingabe2.Singleton.insertNewElement(newElement);
            }
        });

        
        _infoButton = GuiFabric.getJButton(I18nKeys.SHOW_INFORMATION);
        _infoButton.setBackground(new Color(0, 0, 0, 0));
        _infoButton.setOpaque(false);
        _infoButton.setFont(new Font("Arial", Font.PLAIN, 10));
        this.add(_infoButton);
        _infoButton.setBounds(BUTTON_WIDTH*3, 0, BUTTON_WIDTH, 20);
        _infoButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(GeckoSim._win, "The subcircuit block can be used to create hierarchical circuit structures.\n"
                        + "The connection of subcircuit-internal circuits to its environment can done \n"
                        + "either via \"subcircuit terminals\" or \"global labels\". We recommend to use\n"
                        + "global labels only rarely - otherwise your simulation model will become \n"
                        + "confusing. Terminals and global labels can be created for all simulation \n"
                        + "domains, i.e. circuit, thermal, control and reluctance.\n"
                        + "By default, the visiblity of labels and components for references are circuit-\n"
                        + "sheet local. You can move components in the model hierarchy by moving\n"
                        + "the components and navigating into the parent circuit sheet or into a sub-\n"
                        + "circuit by clicking on the subcircuit block."
                        , "Subcircuit documentation", JOptionPane.INFORMATION_MESSAGE);
            }
        });    
        
        setNewScaling(AbstractCircuitSheetComponent.dpix);
    }

    @Override
    public void drawCircuitSheet(Graphics2D g2d) {
        super.drawCircuitSheet(g2d);
        g2d.setColor(Color.decode("0xaaaaaa"));  // zwischen GRAY (808080) und LIGHTGREY (d3d3d3)
        int dpix = AbstractCircuitSheetComponent.dpix;        
        g2d.drawRect(0, 0, dpix * (_worksheetSize.getSizeX()), dpix * (_worksheetSize.getSizeY()));
    }       
    
    public void doSetVisibleAction() {
        super.doSetVisibleAction();    
        _nameLabel = new JLabel();
        setNameLabelText();
        Fenster._northPanel.add(_nameLabel);
        Fenster._northPanel.add(_upButton);
        Fenster._northPanel.add(_settingsButton);
        Fenster._northPanel.add(_newTerminalButton);
        Fenster._northPanel.add(_infoButton);
        Fenster._northPanel.revalidate();
        //this.add(upButton);        
    }
    
    public void setNameLabelText() {
        if(_nameLabel != null) {
            _nameLabel.setText(getCircuitSheetName()  + "   ");
        }        
    }
    
    
    public String getCircuitSheetName() {    
        return _subBlock.getStringID();
    }        
}
