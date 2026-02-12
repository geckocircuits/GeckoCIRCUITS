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

import gecko.geckocircuits.circuit.losscalculation.LossCalculationDetail;
import static gecko.geckocircuits.circuit.losscalculation.LossCalculationDetail.DETAILED;
import static gecko.geckocircuits.circuit.losscalculation.LossCalculationDetail.SIMPLE;
import gecko.geckocircuits.circuit.losscalculation.LossProperties;
import gecko.geckocircuits.circuit.losscalculation.VerlustBerechnungDetailed;
import gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

public abstract class AbstractDialogPowerSwitch<T extends AbstractSemiconductor> extends DialogElementLK<T> {
    
    private final JPanelHalbleiterDetailButtons _panelHalbleiterDetail;
    private JPanel _jPanelSimpleLosses;
    private final LossProperties _lossCalculation;
    private final JRadioButton _jRadioButtonSimpleLosses = new JRadioButton();
    private final JRadioButton _jRadioButtonDetailedLosses = new JRadioButton();
    private final JPanel _characteristicsPanel = new JPanel();
    private JTabbedPane _tabber;
    private final JPanel _lossPanel = new JPanel();
    
    public AbstractDialogPowerSwitch(final T elementLK) {
        super(elementLK);        
        _lossCalculation = (LossProperties) element.getVerlustBerechnung();
        ButtonGroup bgD = new ButtonGroup();
        bgD.add(_jRadioButtonSimpleLosses);
        bgD.add(_jRadioButtonDetailedLosses);
        
        _jRadioButtonSimpleLosses.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {                
                _lossCalculation.setLossType(LossCalculationDetail.SIMPLE);
                switchSimpleDetailedFieldsEnable(false);
            }
        });
        
        _jRadioButtonDetailedLosses.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {                                
                _lossCalculation.setLossType(LossCalculationDetail.DETAILED);
                switchSimpleDetailedFieldsEnable(true);
            }
        });
        
        final LossProperties lossProps = (LossProperties) element.getVerlustBerechnung();
        final VerlustBerechnungDetailed detailed = lossProps.getDetailedLosses();
        _panelHalbleiterDetail = new JPanelHalbleiterDetailButtons(detailed);
    }
    
    public abstract JPanel createParameterPanel();
    
    @Override
    protected final void baueGUIIndividual() {
        _tabber = new JTabbedPane();        
        
        populateCharacteristicsPanel();        
        populateLossesPanel();        
        _tabber.addTab(I18nKeys.CHARACTERISTIC.getTranslation(), _characteristicsPanel);
        _tabber.addTab("Losses", _lossPanel);
        con.add(_tabber, BorderLayout.CENTER);
    }    
    
    private void populateCharacteristicsPanel() {
        _characteristicsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                I18nKeys.CIRCUIT_MODEL.getTranslation(), TitledBorder.LEFT, TitledBorder.TOP));
        _characteristicsPanel.setLayout(new BorderLayout());
        
        JPanel pS1 = createParameterPanel();        
        _characteristicsPanel.add(pS1, BorderLayout.NORTH);
    }


    private void populateLossesPanel() {    
        _lossPanel.setLayout(new BorderLayout());
        _lossPanel.add(createSimpleOuterPanel(), BorderLayout.NORTH);
        _lossPanel.add(createOuterDetailedLossPanel(), BorderLayout.SOUTH);
                
        setRadioButtonsInitialStates();        
    }    

    
    /**
     * when changing between simple losses and detailled losses, the corresponding panel
     * is enabled, and the other one disabled.
     * @param enabled 
     */
    private void switchSimpleDetailedFieldsEnable(final boolean enabled) {
        if (_panelHalbleiterDetail != null) {
            _panelHalbleiterDetail.setEnabled(enabled);
        }
        for (Component comp : _jPanelSimpleLosses.getComponents()) {            
            comp.setEnabled(!enabled);
        }
    }

    private void setRadioButtonsInitialStates() {
        switch (_lossCalculation.getLossType()) {
            case SIMPLE:
                _jRadioButtonSimpleLosses.setSelected(true);
                switchSimpleDetailedFieldsEnable(false);
                break;
            case DETAILED:
                _jRadioButtonDetailedLosses.setSelected(true);
                switchSimpleDetailedFieldsEnable(true);
                break;
            default:

        }
    }

    private JPanel createSimpleOuterPanel() {
        _jPanelSimpleLosses = createParameterPanel(element.kOn, element.kOff, element.uK);
        _jPanelSimpleLosses.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                I18nKeys.LOSSES_BASED_ON_CIRCUIT_MODEL.getTranslation(), TitledBorder.LEFT, TitledBorder.TOP));
        JPanel simpleOuterPanel = new JPanel();
        simpleOuterPanel.setLayout(new BorderLayout());
        JPanel jpDx1x = new JPanel();
        jpDx1x.setLayout(new BorderLayout());
        jpDx1x.add(_jRadioButtonSimpleLosses, BorderLayout.NORTH);
        jpDx1x.add(new JLabel(" "), BorderLayout.CENTER);
        simpleOuterPanel.add(jpDx1x, BorderLayout.WEST);
        simpleOuterPanel.add(_jPanelSimpleLosses, BorderLayout.CENTER);
        return simpleOuterPanel;
    }

    private JPanel createOuterDetailedLossPanel() {
        JPanel detailedOuterPanel = new JPanel();        
        
        detailedOuterPanel.setLayout(new BorderLayout());
        detailedOuterPanel.add(_jRadioButtonDetailedLosses, BorderLayout.BEFORE_FIRST_LINE);
        JPanel jpDx2x = new JPanel();
        jpDx2x.setLayout(new BorderLayout());
        jpDx2x.add(_jRadioButtonDetailedLosses, BorderLayout.NORTH);
        jpDx2x.add(new JLabel(" "), BorderLayout.CENTER);
        detailedOuterPanel.add(jpDx2x, BorderLayout.WEST);
        detailedOuterPanel.add(_panelHalbleiterDetail, BorderLayout.CENTER);
        return detailedOuterPanel;
    }
        
}
