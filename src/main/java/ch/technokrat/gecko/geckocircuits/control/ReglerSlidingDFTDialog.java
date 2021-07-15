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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.FrequencyData;
import ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.OutputData;
import static ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.OutputData.ABS;
import static ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.OutputData.IMAG;
import static ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.OutputData.PHASE;
import static ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.OutputData.REAL;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

class ReglerSlidingDFTDialog extends DialogElementCONTROL<ReglerSlidingDFT> {

    private final GridLayout _grid;
    private final Stack<JComboBox> _frequencySelectionList = new Stack<JComboBox>();
    private final Stack<FormatJTextField> _freqFields = new Stack<FormatJTextField>();
    private final Stack<JLabel> _labels = new Stack<JLabel>();
    private final List<ReglerSlidingDFT.FrequencyData> _originalData;
    private final JPanel jPanelFreqs;
    private JButton _jButtonAddFreq;
    private JButton _jButtonRemoveFreq;

    public ReglerSlidingDFTDialog(final ReglerSlidingDFT regler) {
        super(regler);
        jPanelFreqs = new JPanel();
        jPanelFreqs.setBorder(new TitledBorder("Fourier Transform Frequencies"));
        _originalData = regler.getFrequencyData();
        this.setTitle(LangInit.getTranslatedString(regler.getTypeDescription()));        
        _grid = new GridLayout(_originalData.size(), 2);
        for (ReglerSlidingDFT.FrequencyData data : _originalData) {
            addFreqData(data);
        }
        jPanelFreqs.revalidate();
        this.pack();
        setResizable(false);
    }

    @Override
    void baueGuiIndividual() {
        jpM = new JPanel();
        jpM.setLayout(new BorderLayout());
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Parameter", TitledBorder.LEFT, TitledBorder.TOP));

        final JPanel pPT1a = createParameterPanel(element._averageSpan);
        jpM.add(pPT1a, BorderLayout.NORTH);
        jpM.add(jPanelFreqs, BorderLayout.CENTER);

        final JPanel buttonAddRemovePanel = new JPanel();
        _jButtonAddFreq = GuiFabric.getJButton(I18nKeys.ADD_FREQUENCY);
        _jButtonAddFreq.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                element.addDataPoint();
                _grid.setRows(_freqFields.size() + 1);
                setResizable(true);
                addParameterPanel();
                //addFormatJTextField(newFreq);
                addComboBox(ReglerSlidingDFT.OutputData.ABS);
                jPanelFreqs.revalidate();
                ReglerSlidingDFTDialog.this.pack();
                setResizable(false);                                
            }
        });
        buttonAddRemovePanel.add(_jButtonAddFreq);
        _jButtonRemoveFreq = GuiFabric.getJButton(I18nKeys.REMOVE_FREQUENCY);
        _jButtonRemoveFreq.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (_frequencySelectionList.size() == 1) {
                    return;
                }                                

                jPanelFreqs.remove(_frequencySelectionList.pop());
                
                FormatJTextField toRemoveTextField = _freqFields.pop();
                unregisterTextField(toRemoveTextField);
                
                jPanelFreqs.remove(toRemoveTextField);
                jPanelFreqs.remove(_labels.pop());                

                _grid.setRows(_freqFields.size());
                _grid.setColumns(5);
                jPanelFreqs.setLayout(_grid);
                setResizable(true);
                jPanelFreqs.revalidate();
                ReglerSlidingDFTDialog.this.pack();
                setResizable(false);                
            }
        });
        buttonAddRemovePanel.add(_jButtonRemoveFreq);
        jpM.add(buttonAddRemovePanel, BorderLayout.SOUTH);


    }

    private void addParameterPanel() {
        FrequencyData data = element.getFrequencyData().get(_freqFields.size());
        final JLabel labPar1 = new JLabel(data._frequency.getShortName() + " [Hz] =  ");
        _labels.add(labPar1);
        labPar1.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        labPar1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jPanelFreqs.add(labPar1);        
        FormatJTextField textField = getRegisteredTextField(data._frequency);
        _freqFields.add(textField);
        jPanelFreqs.add(textField);
        jPanelFreqs.setLayout(_grid);
    }

    private void addComboBox(final OutputData data) {
        final JComboBox frequencySelectionBox = new JComboBox();
        Dimension comboDimension = new Dimension(20, 3);
        frequencySelectionBox.setPreferredSize(comboDimension);
        frequencySelectionBox.setMaximumSize(comboDimension);
        frequencySelectionBox.setMinimumSize(comboDimension);
        frequencySelectionBox.setSize(comboDimension);

        frequencySelectionBox.addItem(ReglerSlidingDFT.OutputData.ABS);
        frequencySelectionBox.addItem(ReglerSlidingDFT.OutputData.REAL);
        frequencySelectionBox.addItem(ReglerSlidingDFT.OutputData.IMAG);
        frequencySelectionBox.addItem(ReglerSlidingDFT.OutputData.PHASE);
        _frequencySelectionList.add(frequencySelectionBox);
        jPanelFreqs.add(frequencySelectionBox);

        switch (data) {
            case ABS:
                frequencySelectionBox.setSelectedIndex(0);
                break;
            case REAL:
                frequencySelectionBox.setSelectedIndex(1);
                break;
            case IMAG:
                frequencySelectionBox.setSelectedIndex(2);
                break;
            case PHASE:
                frequencySelectionBox.setSelectedIndex(3);
                break;
            default:
                assert false;
                break;
        }

    }

    private void addFreqData(final ReglerSlidingDFT.FrequencyData data) {
        //addFormatJTextField(data._frequency.getValue());
        addParameterPanel();
        addComboBox(data._outputData);
    }

    @Override
    public void processInputIndividual() {
        super.processInputIndividual(); //To change body of generated methods, choose Tools | Templates.
        element.setOutputTerminalNumber(_freqFields.size());
                
        for(JComboBox combo : _frequencySelectionList) {
            int index = _frequencySelectionList.indexOf(combo);            
            _originalData.get(index)._outputData = (OutputData) combo.getSelectedItem();
        }
        int makeSmaller = 0;
        if(_originalData.size() > _freqFields.size()) {
            makeSmaller = _originalData.size() - _freqFields.size();
        }
        
        for(int i = 0; i < makeSmaller; i++) {
            element.removeLastFrequencyData();
        }                        
    }
    
    

}
