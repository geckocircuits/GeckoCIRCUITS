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
package ch.technokrat.gecko;

import ch.technokrat.gecko.geckocircuits.allg.OperatingMode;
import ch.technokrat.gecko.geckocircuits.allg.StartupWindow;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.control.ReglerFromEXTERNAL;
import ch.technokrat.gecko.geckocircuits.control.ReglerOSZI;
import ch.technokrat.gecko.geckocircuits.control.ReglerToEXTERNAL;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeckoSimulink {

    private long tStartSimulink, tEndSimulink;
    private boolean isRunning = false;

    public GeckoSimulink() {
        if(!setSimulinkOperatingMode()) {
            GeckoSim.main(new String[]{});
        }
        
    }

    public Object external_openFile(Object fileName) {        
        String sFileName = "";
        if (fileName instanceof String) {
            sFileName = (String) fileName;
        }
        try {
            GeckoSim._win.openFile(sFileName);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GeckoSimulink.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "returnValue";
    }

    public GeckoSimulink(String filePath) {        
        ReglerFromEXTERNAL.fromExternals.clear();
        ReglerToEXTERNAL.toExternals.clear();
        if(!setSimulinkOperatingMode()) {
            GeckoSim.main(new String[]{filePath});        
        }                
    }

    //========================================================================
    //========================================================================
    // SIMULINK-KOPPLUNG - direkter externer Zugriff:
    //========================================================================
    //========================================================================
    //
    public double external_init(double tend) {        
        if(isRunning) {
            external_end();
        }
        isRunning = true;
        if (GeckoSim._win == null) {
            System.out.println("initializing GeckoCIRCUITS");
            return -1;
        }
        GeckoSim._win._simRunner.external_init(tend);
        tStartSimulink = System.currentTimeMillis();
        
        for (AbstractBlockInterface block : SchematischeEingabe2.Singleton.getElementCONTROL()) {
                if (block instanceof ReglerOSZI) {
                    ((ReglerOSZI) block).setSimulationTimeBoundaries(0, tend);
                }
            }
        
        return -1;
    }
    
    public void external_step(double t) {
        GeckoSim._geckoSim._win._simRunner.simKern.external_step(t);
    }

    public double external_getdt() {        
        return GeckoSim._geckoSim._win._simRunner.simKern.getdt();
    }

    public void external_end() {        
        GeckoSim._geckoSim._win._simRunner.simKern.external_end();        
        tEndSimulink = System.currentTimeMillis();        
        GeckoSim._win.external_end(tStartSimulink, tEndSimulink);        
        isRunning = false;        
    }

    public int external_getTerminalNumber_TO_EXTERNAL(int portNo) {
        if(portNo < ReglerToEXTERNAL.toExternals.size()) {
            return ((ReglerToEXTERNAL) ReglerToEXTERNAL.toExternals.get(portNo)).XIN.size();
        } else {
            return 0;
        }
        
    }

    public static void external_setInputPortName(int index, String name) {
        if(index < ReglerFromEXTERNAL.fromExternals.size()) {
            ReglerFromEXTERNAL fromExt = (ReglerFromEXTERNAL) ReglerFromEXTERNAL.fromExternals.get(index);
            fromExt.setExternalName(new String(name));
        } 
    }

    public static void external_setOutputPortName(int index, String name) {
        if(index < ReglerToEXTERNAL.toExternals.size()) { 
            ReglerToEXTERNAL fromExt = (ReglerToEXTERNAL) ReglerToEXTERNAL.toExternals.get(index);
            fromExt.setExternalName(new String(name));
        }
    }

    public int external_getTerminalNumber_FROM_EXTERNAL(int portNo) {        
        if(portNo < ReglerFromEXTERNAL.fromExternals.size()) {
            return ((ReglerFromEXTERNAL) ReglerFromEXTERNAL.fromExternals.get(portNo)).getTerminalNumber();
        } else {
            return 0;
        }        
    }

    public void external_setVisible(boolean value) {
        GeckoSim._win.setVisible(value);
    }

    public int getNumOutputPorts() {    
        return ReglerToEXTERNAL.toExternals.size();
    }

    public int getNumInputPorts() {
        int returnValue = ReglerFromEXTERNAL.fromExternals.size();        
        return returnValue;
    }

    static double[] tmpRemove = new double[10];
    
    public double[] external_getValues(int portNumber) {        
        return ((ReglerToEXTERNAL) ReglerToEXTERNAL.toExternals.get(portNumber)).dataVector;
    }

    public void external_setScalarInputValue(double value, int portNo) {
        ReglerFromEXTERNAL reg = (ReglerFromEXTERNAL) ReglerFromEXTERNAL.fromExternals.get(portNo);
        reg.dataVector[0] = value;        
    }

    public void external_setVectorInputValue(double value, int portNo, int index) {        
        ReglerFromEXTERNAL reg = (ReglerFromEXTERNAL) ReglerFromEXTERNAL.fromExternals.get(portNo);
        double[] par = reg.dataVector;
        par[index] = value;                                
    }

    private boolean setSimulinkOperatingMode() {
        GeckoSim.operatingmode = OperatingMode.SIMULINK;
        return StartupWindow.testDialogOpenSourceVersion("Simulink coupling");                    
    }

}
