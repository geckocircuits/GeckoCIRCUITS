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
package gecko;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.geckocircuits.general.OperatingMode;
import gecko.geckocircuits.general.StartupWindow;
import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.control.ControlFromEXTERNAL;
import gecko.geckocircuits.control.ControlOSZI;
import gecko.geckocircuits.control.ControlToEXTERNAL;
import java.io.FileNotFoundException;
public class GeckoSimulink {
    private static final Logger LOGGER = LogManager.getLogger(GeckoSimulink.class);


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
            LOGGER.error("Failed to open Simulink coupling file: " + sFileName, ex);
        }
        return "returnValue";
    }

    public GeckoSimulink(String filePath) {
        ControlFromEXTERNAL.clearFromExternals();
        ControlToEXTERNAL.clearToExternals();
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
            LOGGER.info("initializing GeckoCIRCUITS");
            return -1;
        }
        GeckoSim._win._simRunner.external_init(tend);
        tStartSimulink = System.currentTimeMillis();

        for (AbstractBlockInterface block : SchematicEditor2.Singleton.getElementCONTROL()) {
                if (block instanceof ControlOSZI) {
                    ((ControlOSZI) block).setSimulationTimeBoundaries(0, tend);
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
        if(portNo < ControlToEXTERNAL.toExternals.size()) {
            return ((ControlToEXTERNAL) ControlToEXTERNAL.toExternals.get(portNo)).XIN.size();
        } else {
            return 0;
        }

    }

    public static void external_setInputPortName(int index, String name) {
        if(index < ControlFromEXTERNAL.fromExternals.size()) {
            ControlFromEXTERNAL fromExt = (ControlFromEXTERNAL) ControlFromEXTERNAL.fromExternals.get(index);
            fromExt.setExternalName(name);
        }
    }

    public static void external_setOutputPortName(int index, String name) {
        if(index < ControlToEXTERNAL.toExternals.size()) {
            ControlToEXTERNAL fromExt = (ControlToEXTERNAL) ControlToEXTERNAL.toExternals.get(index);
            fromExt.setExternalName(name);
        }
    }

    public int external_getTerminalNumber_FROM_EXTERNAL(int portNo) {
        if(portNo < ControlFromEXTERNAL.fromExternals.size()) {
            return ((ControlFromEXTERNAL) ControlFromEXTERNAL.fromExternals.get(portNo)).getTerminalNumber();
        } else {
            return 0;
        }
    }

    public void external_setVisible(boolean value) {
        GeckoSim._win.setVisible(value);
    }

    public int getNumOutputPorts() {
        return ControlToEXTERNAL.toExternals.size();
    }

    public int getNumInputPorts() {
        return ControlFromEXTERNAL.fromExternals.size();
    }

    static double[] tmpRemove = new double[10];

    public double[] external_getValues(int portNumber) {
        return ((ControlToEXTERNAL) ControlToEXTERNAL.toExternals.get(portNumber)).dataVector;
    }

    public void external_setScalarInputValue(double value, int portNo) {
        ControlFromEXTERNAL reg = (ControlFromEXTERNAL) ControlFromEXTERNAL.fromExternals.get(portNo);
        reg.dataVector[0] = value;
    }

    public void external_setVectorInputValue(double value, int portNo, int index) {
        ControlFromEXTERNAL reg = (ControlFromEXTERNAL) ControlFromEXTERNAL.fromExternals.get(portNo);
        double[] par = reg.dataVector;
        par[index] = value;
    }

    private boolean setSimulinkOperatingMode() {
        GeckoSim.operatingmode = OperatingMode.SIMULINK;
        return StartupWindow.testDialogOpenSourceVersion("Simulink coupling");
    }

}
