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

import gecko.geckocircuits.allg.FormatJTextField;
import gecko.geckocircuits.allg.GlobalColors;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.ComponentCoupable;
import gecko.geckocircuits.circuit.CircuitSourceType;
import gecko.geckocircuits.control.DialogElementCONTROL;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

class AbstractCircuitSourceDialog extends DialogElementLK<AbstractCircuitSource> {
    private static final int SIN_TAB_INDEX = 0;
    private static final int DC_SOURCE_TAB_INDEX = 1;
    private static final int SIGNAL_CTRL_TAB_INDEX = 2;
    private static final int DIRECT_VOLTAGE_TAB_INDEX = 3;
    
    private static final int DIALOG_SIZE_X = 100;
    private static final int DIALOG_SIZE_Y = 110;
    
    private final JTabbedPane _tabberU = new JTabbedPane();    
    
    private FormatJTextField _dcValueField;
    private FormatJTextField _amplitudeField;
    private FormatJTextField _frequencyField;
    private FormatJTextField _offsetField;
    private FormatJTextField _phaseField;
    private FormatJTextField _gainField;
    
    public AbstractCircuitSourceDialog(final AbstractCircuitSource parent) {
        super(parent);
    }

    @Override
    void baueGUIIndividual() {
        
        String labelText1 = "";        
        
        if(element instanceof AbstractVoltageSource) {
            _dcValueField = getRegisteredTextField( ((AbstractVoltageSource) element)._dcValue);
            _amplitudeField = getRegisteredTextField( ((AbstractVoltageSource) element)._amplitude);            
            _offsetField = getRegisteredTextField(((AbstractVoltageSource) element)._offset);                        
            labelText1 = ((AbstractVoltageSource) element)._amplitude.getShortName();            
        } else if(element instanceof AbstractCurrentSource) {
            _dcValueField = getRegisteredTextField(((AbstractCurrentSource) element)._dcValue);
            _amplitudeField = getRegisteredTextField( ((AbstractCurrentSource) element)._amplitude);            
            _offsetField = getRegisteredTextField(((AbstractCurrentSource) element)._offset);                        
            labelText1 = ((AbstractCurrentSource) element)._amplitude.getShortName();
        }
        
        _frequencyField = getRegisteredTextField(element.frequency);                
        _phaseField = getRegisteredTextField(element.phase);                
                
        JPanel puAC2 = new JPanel();
        puAC2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "AC Sinusoidal", TitledBorder.LEFT, TitledBorder.TOP));
        puAC2.setLayout(new GridLayout(5, 2));
        //
        JLabel jl11 = labelFabric(labelText1 +  " =  ");
        puAC2.add(jl11);
        puAC2.add(_amplitudeField);
        JLabel jl12 = labelFabric("f [Hz] = ");
        puAC2.add(jl12);
        puAC2.add(_frequencyField);
        JLabel jl13 = labelFabric("offset = ");
        puAC2.add(jl13);
        puAC2.add(_offsetField);
        JLabel jl14 = labelFabric("phase [°] = ");
        puAC2.add(jl14);
        puAC2.add(_phaseField);

        JPanel puAC = new JPanel();
        _tabberU.addTab("AC", puAC);
        //puAC.setLayout(new GridLayout(1, 1));
        puAC.setLayout(new BorderLayout());
        puAC.add(puAC2, BorderLayout.NORTH);
        //
        //---------------------------------------
        JPanel puDC2 = new JPanel();
        puDC2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "DC Constant", TitledBorder.LEFT, TitledBorder.TOP));
        puDC2.setLayout(new GridLayout(1, 2));
        String labelText = "iDC =  ";

        if (element instanceof AbstractVoltageSource) {
            labelText = "uDC  ";
        }

        JLabel jl20 = labelFabric(labelText);

        puDC2.add(jl20);
        puDC2.add(_dcValueField);
        //
        JPanel puDC = new JPanel();
        _tabberU.addTab("DC", puDC);
        puDC.setLayout(new BorderLayout());
        puDC.add(puDC2, BorderLayout.NORTH);
        //
        //---------------------------------------
        JPanel puSGN2 = new JPanel();
        puSGN2.setBorder(new TitledBorder("Signal-Controlled"));
        puSGN2.setLayout(new BorderLayout());
        Component controlLabelComponent = createControlLabelCombo(element);
        puSGN2.add(controlLabelComponent);
        //
        JPanel puSGN = new JPanel();
        _tabberU.addTab("Sign", puSGN);
        puSGN.setLayout(new BorderLayout());
        puSGN.add(puSGN2, BorderLayout.NORTH);
        //
        //------------
        JPanel puSGNdir = new JPanel();
        puSGNdir.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                "Direct Voltage Control", TitledBorder.LEFT, TitledBorder.TOP));
        puSGNdir.setLayout(new BorderLayout());
        JPanel jpM = null;
        if (element instanceof ComponentCoupable) {
            jpM = DialogElementCONTROL.createComponentCouplingPanel((AbstractBlockInterface) element);
        }
        puSGNdir.add(jpM);

        JTextArea jtx5 = new JTextArea();
        jtx5.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx5.setText("INFORMATION: \nDirect Voltage Control allows to employ voltage across a"
                + " power circuit component without time-step delay. Visualize flow direction "
                + "to specify the voltage. Use this feature in case of stability problems with controlled sources.");
        jtx5.setLineWrap(true);
        jtx5.setWrapStyleWord(true);
        jtx5.setBackground(Color.white);
        jtx5.setEditable(false);
        jtx5.setPreferredSize(new Dimension(DIALOG_SIZE_X, DIALOG_SIZE_Y));
        //-------
        JLabel jl30x = labelFabric("Gain = ");
        _gainField = getRegisteredTextField(element.directPotentialGain);
        
        JPanel puSdirX = new JPanel();
        puSdirX.setLayout(new BorderLayout());
        puSdirX.add(puSGNdir, BorderLayout.NORTH);
        puSdirX.add(jl30x, BorderLayout.WEST);
        puSdirX.add(_gainField, BorderLayout.CENTER);
        puSdirX.add(new JLabel("                     "), BorderLayout.EAST);  // Abstandhalter
        puSdirX.add(new JLabel("                     "), BorderLayout.SOUTH);  // Abstandhalter
        JPanel puSdir = new JPanel();
        puSdir.setLayout(new BorderLayout());
        puSdir.add(puSdirX, BorderLayout.NORTH);
        puSdir.add(new JScrollPane(jtx5), BorderLayout.CENTER);
        //
        _tabberU.addTab("Dir.Volt.", puSdir);
        
        
        
        JPanel jpMM = new JPanel();
        jpMM.setLayout(new BorderLayout());
        jpMM.add(_tabberU, BorderLayout.CENTER);
        
        switch (element.sourceType.getValue()) {            
            case QUELLE_SIN:
                _tabberU.setSelectedIndex(SIN_TAB_INDEX);
                break;
            case QUELLE_DC:
                _tabberU.setSelectedIndex(DC_SOURCE_TAB_INDEX);
                break;
            case QUELLE_SIGNALGESTEUERT:
                _tabberU.setSelectedIndex(SIGNAL_CTRL_TAB_INDEX);
                break;
            case QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                _tabberU.setSelectedIndex(DIRECT_VOLTAGE_TAB_INDEX);
                break;
            default:
                break;
        }
        con.add(jpMM, BorderLayout.CENTER);
    }
    
    @Override
    public void processInputIndividual() {                                        
        switch (_tabberU.getSelectedIndex()) {
            case SIN_TAB_INDEX:
                element.sourceType.setUserValue(CircuitSourceType.QUELLE_SIN);                
                break;
            case DC_SOURCE_TAB_INDEX:
                element.sourceType.setUserValue(CircuitSourceType.QUELLE_DC);                                
                break;
            case SIGNAL_CTRL_TAB_INDEX:
                element.sourceType.setUserValue(CircuitSourceType.QUELLE_SIGNALGESTEUERT);
                break;
            case DIRECT_VOLTAGE_TAB_INDEX:
                element.sourceType.setUserValue(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
                break;
            default:
                break;
        }
    }

}
