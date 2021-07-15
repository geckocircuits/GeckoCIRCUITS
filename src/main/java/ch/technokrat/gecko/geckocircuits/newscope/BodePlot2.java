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

import ch.technokrat.gecko.geckocircuits.control.ScopeSignalSimpleName;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;
import java.awt.BorderLayout;
import java.util.Stack;
import javax.swing.JPanel;

/**
 *
 * @author andy
 */
public class BodePlot2 extends JPanel {
    public GraferV4 _graferNew;
    private NewScope _graferPanel;
    private final int NUMBER_SIGNALS = 2;
    private final String[] SIGNAL_NAMES = new String[]{"Magnitude", "Phase"};
    private DialogConnectSignalsGraphs _diagCON;
    private DataContainerSimple _dataContainer;

    public BodePlot2() {                        
        _graferNew = new GraferV4(new ScopeSettings());
        _graferPanel = new NewScope(_graferNew);
        _dataContainer = null;
        this.setLayout(new BorderLayout());
    }
        
    
    public void insertData(double[][]erg) {
        _graferNew = new GraferV4(new ScopeSettings());
        _graferNew.setNewXNames("w [rad / sec] = ", "1/w [sec] = ");        
        _graferNew.setSimulationTimeBoundaries(erg[0][0], erg[0][erg[0].length-1]);
        _graferPanel = new NewScope(_graferNew);
        this.add(_graferPanel);
        _graferNew.createInitialAndSingleDiagram(true, false, 2);
        _graferNew._manager.getDiagram(0)._diagramSettings.setNameDiagram("Magnitude / Gain [dB]");
        _graferNew._manager.getDiagram(0).getCurve(0).setSymbolEnabled(false);
        AbstractDiagram diag = new DiagramCurve(_graferNew);                
        diag._diagramSettings.setNameDiagram("Phase [Degree]");
        diag._xAxis.setAxisType(AxisLinLog.ACHSE_LOG);        
        
        _graferNew.getManager().addDiagram(diag);
        _graferNew._manager.getDiagram(0).getCurve(1).setAxisConnection(AxisConnection.ZUORDNUNG_NIX);
        diag.getCurve(1).setAxisConnection(AxisConnection.ZUORDNUNG_Y);        
        //_graferNew._manager.getDiagrams().get(0).setAllCurvesWithBars(new int[]{0});
        
        _dataContainer = DataContainerSimple.fabricArrayTimeSeries(2, erg[0].length);
        addSignalNames();
        
        
        for (int i = 0; i < erg[0].length; i++) {            
            float[] data = new float[]{(float) erg[1][i], (float) erg[2][i]};
            _dataContainer.insertValuesAtEnd(data, 2 * Math.PI * erg[0][i]);
        }                
        for(int i = 0; i < SIGNAL_NAMES.length; i++) {
            _dataContainer.setSignalName(SIGNAL_NAMES[i], i);
        }
        
        _dataContainer.setContainerStatus(ContainerStatus.PAUSED);
        _graferPanel.setDataContainer(_dataContainer);
        //new DialogConnectSignalsGraphs(_graferNew).setVisible(true);
    }
    
    private void addSignalNames() {
        final Stack<AbstractScopeSignal> inputSignals = new Stack<AbstractScopeSignal>();
        for (int i = 0; i < NUMBER_SIGNALS; i++) {
            inputSignals.add(new ScopeSignalSimpleName(SIGNAL_NAMES[i]));
        }
        _graferNew._manager.setInputSignals(inputSignals);
    }

}
