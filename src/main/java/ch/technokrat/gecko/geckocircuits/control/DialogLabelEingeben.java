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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.*;

public final class DialogLabelEingeben extends JDialog {

    private final Verbindung _connector;
    private final FormatJTextField _textField = new FormatJTextField();
    private final String _originalLabel;
    private JCheckBox jCheckBoxEnabled;
    private static final int COLS = 12;
    private final ConnectorType _conType;
    private final TerminalInterface _clickedTerminal;
    private final CircuitLabel _label;

    public DialogLabelEingeben(final TerminalInterface terminal) {
        super(GeckoSim._win, true);
        try {
            this.setIconImage(new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif")).getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        _clickedTerminal = terminal;
        _label = terminal.getLabelObject();
        _conType = _clickedTerminal.getCategory();
        if (terminal instanceof TerminalVerbindung) {
            _connector = ((TerminalVerbindung) terminal).getParentConnection();
        } else {
            _connector = null;
        }        
        
        _originalLabel = _label.getLabelString();               
        setTitle(" Label");
        baueGUI();

    }

    private void baueGUI() {
        final Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        this.setLocationRelativeTo(GeckoSim._win);

        final JButton knOK = GuiFabric.getJButton(I18nKeys.OK);


        final JButton knX = GuiFabric.getJButton(I18nKeys.CANCEL);
        knX.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                dispose();
            }
        });
        final JPanel panelX = new JPanel();
        panelX.add(knOK);
        panelX.add(knX);
        
        _textField.setText(_originalLabel);
        _textField.setColumns(COLS);
        _textField.selectAll();        

        final JPanel jpN = new JPanel();
        jpN.setLayout(new GridLayout(2, 1));
        jpN.add(_textField);

        if (_connector != null) {
            jCheckBoxEnabled = new JCheckBox("Enabled");
            if (_connector.isCircuitEnabled() == Enabled.ENABLED) {
                jCheckBoxEnabled.setSelected(true);
            } else {
                jCheckBoxEnabled.setSelected(false);
            }


            jpN.add(jCheckBoxEnabled);
        }
        con.add(jpN);
        //------------------
        con.add(panelX, BorderLayout.SOUTH);

        knOK.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {

                if (_connector != null) {
                    if (jCheckBoxEnabled.isSelected()) {
                        _connector._isEnabled.setValue(Enabled.ENABLED);
                    } else {
                        _connector._isEnabled.setValue(Enabled.DISABLED);
                    }

                }

                // 'OK'-Knopf oder 'Return'-Taste gedrueckt --> entsprechender Label des Elements wird aktualisiert
                _label.setLabelFromUserDialog(_textField.getText());                
                _clickedTerminal.getCircuitSheet().updateRenamedLabel(_originalLabel, _textField.getText(), _conType);
                SchematischeEingabe2.Singleton.registerChangeWithNetlistUpdate();
                dispose();
            }
        });

        this.getRootPane().setDefaultButton(knOK);
        this.pack();
        this.setResizable(false);

    }

    @Override
    protected JRootPane createRootPane() {
        final JRootPane myRootPane = super.createRootPane();
        final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        myRootPane.registerKeyboardAction(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                // The action to be executed when Escape is pressed  
                DialogLabelEingeben.this.dispose();
            }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return myRootPane;
    }
}
