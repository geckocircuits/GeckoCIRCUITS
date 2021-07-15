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

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.OperatingMode;
import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a simple wrapper, so that users can access GeckoCIRCUITS from within
 * matlab. Some specific things have been done, e.g. startGui which creates
 * another thread that lives to support Gui-Actions.
 *
 * Careful (for later versions of GeckoCIRCUITS): here, we do a lot of code
 * duplication. The methods in this wrapper are similar existent in GeckoCustom,
 * GeckoSimulink and SimulationAccess Later, we should do this a bit nicer...!
 *
 * @author andy
 */

/*
 * When porting this to the new version, make sure GeckoExternal accesses
 * DIRECTLY SimulationAccess, rather than going through GeckoCustom.
 *
 * @author anstupar
 */
/**
 * This wrapper is now deprecated for use by GeckoCIRCUITS users. They should
 * now use GeckoRemote. We live these here for 1) backwards compatibility 2) our
 * own internal use (possibly), i.e. with demo programs
 *
 * @author anstupar
 */
public class GeckoExternal {

    protected static ExternalGeckoCustom external;
    private static double[][] _globalDoubleMatrix;
    private static float[][] _globalFloatMatrix;

    public static void startGui() {

        System.out.println("***WARNING: GeckoExternal is a DEPRECATED API***");
        System.out.println("GeckoExternal is deprecated as of GeckoCIRCUITS version 1.6.");
        System.out.println("Please switch to using GeckoRemote (as explained in the Appendix of the GeckoSCRIPT tutorial included with your GeckoCIRCUITS distribution).\n");
        System.out.println("Reasons to switch: ");
        System.out.println("Using GeckoRemote runs GeckoCIRCUITS in its own JVM, not inside MATLAB, this means that");
        System.out.println("1) You can close GeckoCIRCUITS without having to close all of MATLAB;");
        System.out.println("2) You don't have to worry about memory allocation for GeckoCIRCUITS in the MATLAB JVM;");
        System.out.println("3) You will not have problems with compiling Java blocks in your model as was the case with some MATLAB installations.\n");
        System.out.println("GeckoExternal continues to function as before for backwards compatibility. However it is no longer maintained.");
        System.out.println("This means that:");
        System.out.println("1) Any new problems with using GeckoExternal in MATLAB will not be addressed;");
        System.out.println("2) Any new GeckoSCRIPT functions (version 1.6 and later) will not be available through GeckoExternal.");
        System.out.println("***WARNING: GeckoExternal is a DEPRECATED API***");

        GeckoSim.operatingmode = OperatingMode.EXTERNAL;
        if (external == null) {
            Thread guiThread = new Thread() {
                @Override
                public void run() {
                    GeckoSim.main(new String[]{});
                    checkExternal();
                }
            };
            guiThread.setPriority(Thread.MIN_PRIORITY);
            guiThread.start();
        }

        while (!GeckoSim.mainLoaded) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(GeckoExternal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public static void startGui(String filePath) {
        GeckoSim.operatingmode = OperatingMode.EXTERNAL;
        if (external == null) {
            GeckoSim.main(new String[]{filePath});
            checkExternal();
        }
    }

    protected static void checkExternal() {
        if (external == null) {
            external = new ExternalGeckoCustom(Fenster._scripter);
            Fenster._external = external;
        }
    }

    public static void runSimulation() {
        checkExternal();
        external.runSimulation();
    }

    public static String[] getControlElements() {
        checkExternal();
        return external.getControlElements();
    }

    public static String[] getCircuitElements() {
        checkExternal();
        return external.getCircuitElements();
    }

    public static String[] getThermalElements() {
        checkExternal();
        return external.getThermalElements();
    }
    
    public static String[] getSpecialElements() {
        checkExternal();
        return external.getSpecialElements();
    }

    public static String[] getIGBTs() {
        checkExternal();
        return external.getIGBTs();
    }

    public static String[] getDiodes() {
        checkExternal();
        return external.getDiodes();
    }

    public static void runGeckoSCRIPT() {
    }

    public static void setGlobalDoubleMatrix(double[][] matrix) {
        _globalDoubleMatrix = matrix;
    }

    public static void setGlobalFloatMatrix(float[][] matrix) {
        _globalFloatMatrix = matrix;
    }

    public static double[][] getGlobalDoubleMatrix() {
        return _globalDoubleMatrix;
    }

    public static float[][] getGlobalFloatMatrix() {
        return _globalFloatMatrix;
    }
        

    public static void setGlobalParameterValue(final String parameterName, final double value) {
        checkExternal();
        GeckoSim._geckoSim._win.optimizerParameterData.setNumberFromName(parameterName, value);
    }

    public static double getGlobalParameterValue(final String parameterName) {
        checkExternal();
        return GeckoSim._geckoSim._win.optimizerParameterData.getNumberFromName(parameterName);
    }

    public String[] getThyristors() {
        checkExternal();
        return external.getThyristors();
    }

    public static String[] getIdealSwitches() {
        checkExternal();
        return external.getIdealSwitches();
    }

    public static String[] getResistors() {
        checkExternal();
        return external.getResistors();
    }

    public static String[] getInductors() {
        checkExternal();
        return external.getInductors();
    }

    public static String[] getCapacitors() {
        checkExternal();
        return external.getCapacitors();
    }

    public static void setParameter(String elementName, String parameterName, double value) {
        checkExternal();
        external.setParameter(elementName, parameterName, value);
    }

    public static void setParameters(final String elementName, final String[] parameterNames, final double[] values) {
        checkExternal();
        external.setParameters(elementName, parameterNames, values);
    }        
    
    public static Object doOperation(final String elementName, final String operationName, final Object parameterValue) {
        checkExternal();
        return external.doOperation(elementName, operationName, parameterValue);
    }

    public static double getParameter(final String elementName, final String parameterName) {
        checkExternal();
        return external.getParameter(elementName, parameterName);
    }

    public static double getOutput(final String elementName, final String outputName) {
        checkExternal();
        return external.getOutput(elementName, outputName);
    }

    public static double getOutput(String elementName) {
        checkExternal();
        return external.getOutput(elementName);
    }

    public static void initSimulation() {
        checkExternal();
        external.initSimulation();
    }

    public static void initSimulation(double dt, double endTime) {
        checkExternal();
        external.initSimulation(dt, endTime);
    }
    
    public static double getSimulationTime() {
        checkExternal();
        return external.getSimulationTime();
    }

    public static void continueSimulation() {
        checkExternal();
        external.continueSimulation();
    }

    public static void simulateStep() {
        checkExternal();
        external.simulateStep();
    }

    public static void simulateSteps(int steps) {
        checkExternal();
        external.simulateSteps(steps);
    }

    public static void simulateTime(double time) {
        checkExternal();
        external.simulateTime(time);
    }
    
    public static void setWorksheetSize(int sizeX, int sizeY) {
        checkExternal();
        external.setWorksheetSize(sizeX, sizeY);
    }
    
    public static int[] getWorksheetSize() {
        checkExternal();
        return external.getWorksheetSize();
    }
        

    public static void endSimulation() {
        checkExternal();
        external.endSimulation();
    }

    public static void saveFileAs(String fileName) {
        checkExternal();
        external.saveFileAs(fileName);
    }

    public static void openFile(final String fileName) throws RemoteException, FileNotFoundException {
        checkExternal();
        external.openFile(fileName);
    }

    public static double get_dt() {
        checkExternal();
        return external.get_dt();
    }

    public static void set_dt(double value) {
        checkExternal();
        external.set_dt(value);
    }

    public static void set_Tend(double value) {
        checkExternal();
        external.set_Tend(value);
    }

    public static double get_Tend() {
        checkExternal();
        return external.get_Tend();
    }

    public static double get_dt_pre() {
        checkExternal();
        return external.get_dt_pre();
    }

    public static void set_dt_pre(double value) {
        checkExternal();
        external.set_dt_pre(value);
    }

    public static void set_Tend_pre(double value) {
        checkExternal();
        external.set_Tend_pre(value);
    }

    public static double get_Tend_pre() {
        checkExternal();
        return external.get_Tend_pre();
    }

    public static double[] getSignalCharacteristics(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
    }

    public static double getAvg(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getAvg(scopeName, scopePort, startTime, endTime);
    }

    public static double getRMS(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getRMS(scopeName, scopePort, startTime, endTime);
    }

    public static double getTHD(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getTHD(scopeName, scopePort, startTime, endTime);
    }

    public static double getMin(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getMin(scopeName, scopePort, startTime, endTime);
    }

    public static double getMax(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getMax(scopeName, scopePort, startTime, endTime);
    }

    public static double getRipple(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getRipple(scopeName, scopePort, startTime, endTime);
    }

    public static double getKlirr(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getKlirr(scopeName, scopePort, startTime, endTime);
    }

    public static double getShape(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getShape(scopeName, scopePort, startTime, endTime);
    }

    public static double[] getSignalCharacteristics(String scopeName, double startTime, double endTime) {
        return getSignalCharacteristics(scopeName, 0, startTime, endTime);
    }

    public static double getAvg(String scopeName, double startTime, double endTime) {
        return getAvg(scopeName, 0, startTime, endTime);
    }

    public static double getRMS(String scopeName, double startTime, double endTime) {
        return getRMS(scopeName, 0, startTime, endTime);
    }

    public static double getMin(String scopeName, double startTime, double endTime) {
        return getMin(scopeName, 0, startTime, endTime);
    }

    public static double getMax(String scopeName, double startTime, double endTime) {
        return getMax(scopeName, 0, startTime, endTime);
    }

    public static double getTHD(String scopeName, double startTime, double endTime) {
        return getTHD(scopeName, 0, startTime, endTime);
    }

    public static double getShape(String scopeName, double startTime, double endTime) {
        return getShape(scopeName, 0, startTime, endTime);
    }

    public static double getKlirr(String scopeName, double startTime, double endTime) {
        return getKlirr(scopeName, 0, startTime, endTime);
    }

    public static double getRipple(String scopeName, double startTime, double endTime) {
        return getRipple(scopeName, 0, startTime, endTime);
    }

    public static double[][] getFourier(String scopeName, int scopePort, double startTime, double endTime, int harmonics) {
        checkExternal();
        return external.getFourier(scopeName, scopePort, startTime, endTime, harmonics);
    }

    public static double[][] getFourier(final String scopeName, final double startTime, final double endTime,
            final int harmonics) {
        return getFourier(scopeName, 0, startTime, endTime, harmonics);
    }
    
    public static void initSteadyStateDetection(final String[] stateVariables, final double[] frequencies,
            final double deltaT, final double simulationTime) {
        checkExternal();
        external.initSteadyStateDetection(stateVariables, frequencies, deltaT, simulationTime);
    }

    public static double[] simulateUntilSteadyState(final boolean supressMessages) {
        checkExternal();
        return external.simulateUntilSteadyState(supressMessages);
    }

    public static void setLossFile(final String elementName, final String lossFileName) throws FileNotFoundException {
        checkExternal();
        external.setLossFile(elementName, lossFileName);
    }

    public static double[] getTimeArray(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        checkExternal();
        return external.getTimeArray(signalName, tStart, tEnd, skipPoints);
    }

    public static float[] getSignalData(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        checkExternal();
        return external.getSignalData(signalName, tStart, tEnd, skipPoints);
    }

    public static void importFromFile(final String fileName, final String subCircuitName) throws FileNotFoundException {
        checkExternal();
        try {
            external.importFromFile(fileName, subCircuitName);
        } catch (RemoteException ex) {
            Logger.getLogger(GeckoExternal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public final void createComponent(final String elementType, final String elementName, final int xPosition, final int yPosition) {        
        checkExternal();
        external.createComponent(elementType, elementName, xPosition, yPosition);                
    }
        
    public void createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) {
        checkExternal();
        external.createConnector(elementName, xStart, yStart, xEnd, yEnd, startHorizontal);
    }
    
    
    public static void deleteComponent(final String elementName) {
        checkExternal();
        external.deleteComponent(elementName);
    }
    
    
    public static void deleteAllComponents(final String subcircuitName) {
        checkExternal();
        external.deleteAllComponents(subcircuitName);
    }
    

    /**
     * Rename a component with a given name.
     *
     * @param oldName for selection of component
     * @param newName the new name that should be given. Throws an Exception, if
     * name is already in use!
     * @throws Exception Setting the name was not possible, since another
     * component uses this name already.
     */
    public static void setComponentName(final String oldName, final String newName) throws Exception {
        checkExternal();
        try {
            external.setComponentName(oldName, newName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void setPosition(final String elementName, final int xCoord, final int yCoord) {
        checkExternal();
        external.setPosition(elementName, xCoord, yCoord);
    }
    
    public static int[] getPosition(final String elementName) {
        checkExternal();
        return external.getPosition(elementName);
    }
    
    public static void setOutputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        checkExternal();
        external.setOutputNodeName(elementName, nodeIndex, nodeName);
    }

    public static void setInputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        checkExternal();
        external.setInputNodeName(elementName, nodeIndex, nodeName);
    }
    
    public static String getOutputNodeName(final String elementName, final int nodeIndex) {
        checkExternal();
        return external.getOutputNodeName(elementName, nodeIndex);
    }

    public static String getInputNodeName(final String elementName, final int nodeIndex) {
        checkExternal();
        return external.getInputNodeName(elementName, nodeIndex);
    }
    
    /*
     * do a in-place fourier transform. Attention: the length of timeValues must
     * be a number of 2^N!
     */
    public static float[] realFFT(final float[] timeValues) {
        Cispr16Fft.realft(timeValues, 1);
        return timeValues;
    }
}
