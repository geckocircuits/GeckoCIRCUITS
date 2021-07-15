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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.DataTablePanel;
import ch.technokrat.gecko.geckocircuits.control.calculators.SmallSignalCalculator;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import ch.technokrat.gecko.geckocircuits.newscope.GraferV4;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSettings;
import ch.technokrat.gecko.geckocircuits.newscope.SimpleGraferPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 *
 * @author Raffael2
 */
public final class DialogSSAPlot extends GeckoDialog {
    private static final int BUTTON_HEIGHT = 30;
    //private static final Dimension BUTTON_DIMENSION = new Dimension(80, BUTTON_HEIGHT);
    
    private double[][] _data;
    //private String _extension = ".txt";
    private String _type = "text file";
    private GraferV4 _grafer;
    private final JPanel lowerPanel = new JPanel();
    private DataTablePanel table;
    private final SimpleGraferPanel newScope1;
    
    public DialogSSAPlot(final double[][] data){
        super(GeckoSim._win, true);
        _data = data;
        
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
        
        ScopeSettings settings = new ScopeSettings();
        Dimension windowSize = new Dimension(800, 600);
        setPreferredSize(windowSize);
        setSize(windowSize);
        
        _grafer = new GraferV4(settings);
        _grafer.createInitialAndSingleDiagram(true, true, 1);
        
        newScope1 = new SimpleGraferPanel(_grafer);
        
        //_extension = ...;
        _type = "SSA Bode Plot";
        
        //this.initMagnitude();
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(this.createGUIInput(), BorderLayout.CENTER);
        con.add(lowerPanel,BorderLayout.SOUTH);
        this.setTitle(_type);
        //getRootPane().setDefaultButton(...);
        this.pack();
        updatePlot();
    }
    
    /*
    private void initMagnitude(){       
        data = _parent.getMagnitude();
    }
    */
    private JPanel createGUIInput(){
        table = new DataTablePanel(new String[]{"omega [rad/s]","|G| [dB]"});
        table.setPreferredSize(new Dimension(150,100));
        table.setValues(_data);
        
        table.addTableModelListener(new TableModelListener(){
            @Override
            public void tableChanged(TableModelEvent e){
                _data = table.getCheckedData();
                updatePlot();
            }
            
        });  
        
        JPanel jpCONDdataGes = new JPanel();
        jpCONDdataGes.setLayout(new BorderLayout());
        jpCONDdataGes.add(table, BorderLayout.CENTER);
        
        final DialogSSAPlot dialogParent = this;
        
        //========================
        JPanel jpCOND = new JPanel();
        jpCOND.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        jpCOND.setLayout(new BorderLayout());
        jpCOND.add(newScope1, BorderLayout.CENTER);
        jpCOND.add(jpCONDdataGes, BorderLayout.EAST);
        //========================
        return jpCOND;
    }
    
    private void updatePlot() {
        DataContainerSimple dcs1 = DataContainerSimple.fabricArrayTimeSeries(1, _data[0].length);
        dcs1.setSignalName("TF of ...", 0);

        for (int i = 0; i < _data[0].length; i++) {
            dcs1.insertValuesAtEnd(new float[]{(float) _data[1][i]}, _data[0][i]);
        }

        dcs1.setContainerStatus(ContainerStatus.PAUSED);
        _grafer.setDataContainer(dcs1);
    }
    
}