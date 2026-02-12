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

package gecko.geckocircuits.newscope;

import gecko.geckocircuits.circuit.losscalculation.LossCurve;
import gecko.geckocircuits.circuit.losscalculation.SwitchingLossCurve;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public class LossCurvePlotPanel extends SimpleGraferPanel {
    private int _moreDataFactor;
    
    

    private LossCurvePlotPanel(GraferV4 grafer) {
        super(grafer);
    }

    public void highlightMessKurve(int index) {        
        AbstractDiagram diag = _grafer._manager.getDiagram(0);
        for(AbstractCurve curve : diag.getCurves()) {
            if(curve.getStroke() != GeckoLineStyle.DOTTED_FAT) {
                curve.setStroke(GeckoLineStyle.SOLID_PLAIN);
            }            
        }
        diag.getCurves().get(index).setStroke(GeckoLineStyle.SOLID_FAT_2);
        if(_moreDataFactor > 1) {
            diag.getCurves().get(index + diag.getCurves().size()/2).setStroke(GeckoLineStyle.SOLID_FAT_2);
        }
        repaint();
    }
    
    public static LossCurvePlotPanel fabric(List<? extends LossCurve> lossCurvesWOTest, final LossCurve testCurve) {
        List<LossCurve> lossCurves = new ArrayList<LossCurve>();
        lossCurves.addAll(lossCurvesWOTest);
        
        if(testCurve != null) {
            lossCurves.add(testCurve);            
        }
        
        
        int moreDataFactor = 1;
        if(lossCurves.get(0) instanceof SwitchingLossCurve) {
            moreDataFactor = 2;
        }                        
        
        ScopeSettings settings = new ScopeSettings();
        GraferV4 _grafer = new GraferV4(settings);        
        _grafer.setUpdateSleep(50);
        _grafer.createInitialAndSingleDiagram(false, false, lossCurves.size() * moreDataFactor);                                
        Dimension windowSize = new Dimension(550, 500);
        LossCurvePlotPanel returnValue =  new LossCurvePlotPanel(_grafer);        
        returnValue.setPreferredSize(windowSize);                
        returnValue.setSize(windowSize);                
        
        DataContainerManyTimeSeries dcs1 = new DataContainerManyTimeSeries(moreDataFactor * lossCurves.size(), 1000);        
        
        for(LossCurve curve : lossCurves) {            
            dcs1.setSignalName(curve.getName(), lossCurves.indexOf(curve));                                    
            try {
            for(int i = 0; i < curve.data[0].length; i++) {
                float yValue = (float) (curve.data[1][i]);
                double xValue = curve.data[0][i];                
                dcs1.insertValueAtEnd(yValue , xValue , lossCurves.indexOf(curve));
                
                if(curve.data.length > 2) {
                    float yValue2 = (float) (curve.data[2][i]);                    
                    dcs1.insertValueAtEnd(yValue2 , xValue , lossCurves.size() + lossCurves.indexOf(curve));
                }                                
            }
            } catch (Exception ex) {
                System.err.println(curve + " " + curve.data );
                ex.printStackTrace();
            }
        }
        
        if(moreDataFactor == 2) {
            List<AbstractCurve> curves = _grafer._manager.getDiagrams().get(0).getCurves();
            for(int i = curves.size()/2; i < curves.size(); i++) {
                curves.get(i).setColor(curves.get(i - curves.size()/2).getColor());
            }
        }
        
        dcs1.setContainerStatus(ContainerStatus.PAUSED);        
        returnValue.setDataContainer(dcs1);
        returnValue._moreDataFactor = moreDataFactor;
        _grafer.setSimulationTimeBoundaries(0, dcs1.getNiceMaximumXValue());  
        
        if(testCurve != null) {
            AbstractDiagram diag = _grafer._manager.getDiagram(0);
            AbstractCurve lastCurve = diag.getCurves().get(diag.getCurves().size()-1);
            lastCurve.setStroke(GeckoLineStyle.DOTTED_FAT);
            lastCurve.setSymbolEnabled(false);
            if(returnValue._moreDataFactor > 1) {
                AbstractCurve beforeLastCurve = diag.getCurves().get(diag.getCurves().size()/2-1);
                beforeLastCurve.setStroke(GeckoLineStyle.DOTTED_FAT);                
                beforeLastCurve.setSymbolEnabled(false);
            }    
        }        
        
        return returnValue;        
    }        
}
