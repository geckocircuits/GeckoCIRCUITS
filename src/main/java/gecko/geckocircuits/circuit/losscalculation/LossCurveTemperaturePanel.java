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
package gecko.geckocircuits.circuit.losscalculation;

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

public final class LossCurveTemperaturePanel extends JPanel {

    private final List<JRadioButton> _radioButtons = new ArrayList<JRadioButton>();    
    private final ButtonGroup _buttonGroup = new ButtonGroup();
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();
    
    public LossCurveTemperaturePanel(final List<? extends LossCurve> curveList) {                        
        super();
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Curves", TitledBorder.LEFT, TitledBorder.TOP));
                        
        setGuiButtonsFromList(curveList);                
        setSelectedButton(0);   
        
    }

    int getSelectedIndex() {
        for (JRadioButton button : _radioButtons) {
            if (button.isSelected()) {
                return _radioButtons.indexOf(button);
            }
        }
        assert false;
        return -1;
    }
    
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    private void sendActionEvent(final ActionEvent event) {
        for(ActionListener listener : _listeners) {
            listener.actionPerformed(event);
        }
    }    

    void setSelectedButton(final int toSelect) {
        _radioButtons.get(toSelect).setSelected(true);
    }           

    public void setGuiButtonsFromList(final List<? extends LossCurve> curveList) {
        setLayout(new GridLayout(curveList.size(), 1));                
                
        final GridBagConstraints _gbc = new GridBagConstraints();
        _radioButtons.clear();        
        this.removeAll();
        for (int i1 = 0; i1 < curveList.size(); i1++) {            
            _gbc.gridx = 0;
            _gbc.gridy = i1;
            final JRadioButton newButton = new JRadioButton(curveList.get(i1).getName());            
            _radioButtons.add(newButton);
            newButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    if(newButton.isSelected()) {
                        sendActionEvent(actionEvent);
                    }
                    
                }
                
            });
                        
            _buttonGroup.add(newButton);
            this.add(newButton, _gbc);                                                
        }           
        this.updateUI();
    }
    
}
