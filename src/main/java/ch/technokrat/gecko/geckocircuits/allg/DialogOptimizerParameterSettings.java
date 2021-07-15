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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.DataTablePanelParameters;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;

public class DialogOptimizerParameterSettings extends GeckoDialog {
    private static final String TXT_HELP =
                "\nParameters (name-value pairs) must match the parameters defined in the simulation model, \ne.g. $Rload. "
                + "\n\nPairs of name-value in the input list must be separated by space or tab. "
                + "\n\nIf a name is defined twice, a warning is given, but the simulation can be performed. "
                + "If names are defined in the input list, that are not used in the simulation model, a warning is given. "
                + "\n\nIf parameter names in the simulation model are not defined in the input list, an error is given. In this case "
                + "the simulation cannot be performed. "
                + "\n";
        
    private final OptimizerParameterData _optData;
    private List<AbstractBlockInterface> _elements;
    private Map<String, Integer> _numberOfUsedParamemters = new HashMap<String, Integer>();
    private JTabbedPane tabber;
    private JTextArea jtaHLP;
    
    private DataTablePanelParameters _dataTable;

    public DialogOptimizerParameterSettings(OptimizerParameterData optData, List<AbstractBlockInterface> e) {
        super(GeckoSim._win, true);
        _optData = optData;
        _elements = e;
        for(AbstractBlockInterface block : e) {
            for(UserParameter<? extends Object> parameter : block.getRegisteredParameters()) {
                String nameOpt = parameter.getNameOpt();
                if(!nameOpt.isEmpty()) {
                    int oldNumber = 0;
                    if(_numberOfUsedParamemters.containsKey(nameOpt)) {
                        oldNumber = _numberOfUsedParamemters.remove(nameOpt);                        
                    }                    
                    _numberOfUsedParamemters.put(nameOpt, oldNumber + 1);
                }
            }
        }
        
        this.init();
        this.setMinimumSize(new Dimension(300, 200));
        this.setVisible(true);
    }

    private void init() {
        tabber = new JTabbedPane();
        tabber.addTab(I18nKeys.PARAMETER.getTranslation(), this.baueGUIInput());
        tabber.addTab(I18nKeys.HELP.getTranslation(), this.baueGUIHelp());
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(tabber, BorderLayout.CENTER);
        con.add(this.baueGUIok(), BorderLayout.SOUTH);
        this.setTitle(I18nKeys.PARAMETER_SET.getTranslation());
        this.pack();
    }

    private JPanel baueGUIInput() {

        _dataTable = new DataTablePanelParameters(new String[]{I18nKeys.NAME.getTranslation(), 
            I18nKeys.VALUE.getTranslation(), I18nKeys.USAGES.getTranslation()}, _numberOfUsedParamemters);

        List<String> nOpt = _optData.getNameOpt();
        List<Double> vOpt = _optData.getValueOpt();
        for (int i1 = 0; i1 < nOpt.size(); i1++) {
            _dataTable.insertDataLine(nOpt.get(i1), vOpt.get(i1));
        }

        _dataTable.setPreferredSize(new Dimension(100, 100));
        return _dataTable;
    }

    private JScrollPane baueGUIHelp() {
        jtaHLP = new JTextArea(TXT_HELP);
        jtaHLP.setLineWrap(true);
        jtaHLP.setWrapStyleWord(true);
        return new JScrollPane(jtaHLP);
    }

    private JPanel baueGUIok() {
        final JPanel returnValue = new JPanel();
        returnValue.setBorder(BorderFactory.createEtchedBorder());
        JButton jbOK = GuiFabric.getJButton(I18nKeys.OK);
        jbOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _optData.reSetValuesWithUndo(_dataTable.getVariableNames(), _dataTable.getVariableValues());
                DialogOptimizerParameterSettings.this.dispose();
            }
        });

        JButton jbCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
        jbCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                DialogOptimizerParameterSettings.this.dispose();
            }
        });
        returnValue.add(jbOK);
        this.getRootPane().setDefaultButton(jbOK);
        returnValue.add(jbCancel);
        return returnValue;
    }
}
