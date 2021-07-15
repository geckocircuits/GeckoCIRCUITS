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

/**
 * This an interface for using RMI to control GeckoCIRCUITS from e.g. MATLAB, or
 * remotely from another machine. Warning: the methods of this interface MUST have 
 * identical method names as GeckoRemoteInterface class. I am checking this within
 * a static final Object via assertions. But: this interface does not declare any exception.
 * 
 * @author  andy.
 *
 */
public interface GeckoRemoteIntWithoutExc {
    
    static final MethodNameChecker CHECKER = 
            MethodNameChecker.checkFabric(GeckoRemoteIntWithoutExc.class, GeckoRemoteInterface.class);
    
    
    void runSimulation();

    String[] getControlElements();

    String[] getCircuitElements();

    String[] getThermalElements();
    
    String[] getSpecialElements();

    String[] getIGBTs();

    String[] getDiodes();

    String[] getThyristors();

    String[] getIdealSwitches();

    String[] getResistors();

    String[] getInductors();

    String[] getCapacitors();

    Object doOperation(String elementName, String operationName, Object parameterValue);
    
    void setParameter(String elementName, String parameterName, double value);    
    
    void setParameters(String elementName, String[] parameterNames, double[] values);

    double getParameter(String elementName, String parameterName);

    double getOutput(String elementName, String outputName) ;

    double getOutput(String elementName) ;

    void initSimulation() ;

    void initSimulation(final double deltaT, final double endTime) ;

    void continueSimulation() ;
    
    void simulateTime(double time) ;

    void endSimulation() ;

    void saveFileAs(String fileName) ;

    void openFile(String fileName);

    void importFromFile(String fileName, String importIntoSubcircuit);
    
    double get_dt() ;
    double get_Tend() ;

    double get_dt_pre() ;
    double get_Tend_pre() ;
    
    void set_dt(double value) ;
    void set_dt_pre(double value) ;
    void set_Tend(double value) ;
    void set_Tend_pre(double value) ;
    
    @Deprecated
    double[] getSignalCharacteristics(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getAvg(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getRMS(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getTHD(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getMin(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getMax(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getRipple(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getKlirr(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double getShape(String scopeName, int scopePort, double startTime, double endTime) ;

    @Deprecated
    double[] getSignalCharacteristics(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getAvg(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getRMS(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getMin(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getMax(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getTHD(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getShape(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getKlirr(String scopeName, double startTime, double endTime) ;

    @Deprecated
    double getRipple(String scopeName, double startTime, double endTime) ;

    double[][] getFourier(String scopeName, int scopePort, double startTime, double endTime, int harmonics) ;

    double[][] getFourier(String scopeName, double startTime, double endTime, int harmonics) ;

    double getSignalAvg(String signalName, double startTime, double endTime) ;

    double getSignalRMS(String signalName, double startTime, double endTime) ;

    double getSignalMin(String signalName, double startTime, double endTime) ;

    double getSignalMax(String signalName, double startTime, double endTime) ;

    double getSignalTHD(String signalName, double startTime, double endTime) ;

    double getSignalShape(String signalName, double startTime, double endTime) ;

    double getSignalKlirr(String signalName, double startTime, double endTime) ;

    double getSignalRipple(String signalName, double startTime, double endTime) ;

    double[][] getSignalFourier(String signalName, double startTime, double endTime, int harmonics) ;

    
    void setWorksheetSize(int sizeX, int sizeY);
    int[] getWorksheetSize();    
    
    
    void initSteadyStateDetection(final String[] stateVariables, final double frequency, final double deltaT, 
            final double simulationTime) ;
    
    @Deprecated
    void initSteadyStateDetection(final String[] stateVariables, final double[] frequencies, final double deltaT, 
            final double simulationTime) ;

    double[] simulateToSteadyState(boolean supressMessages) ;
    
    double[] simulateToSteadyState(boolean supressMessages, double targetCorrelation, double targetMeanPctDiff) ;
    
    @Deprecated
    double[] simulateUntilSteadyState(boolean supressMessages) ;

    @Deprecated
    void setLossFile(String elementName, String lossFileName);

    @Deprecated
    void setNonLinear(String elementName, String characteristicFileName);

    void setPosition(String elementName, int xPosition, int yPosition) ;
    int[] getPosition(String elementName) ;

    @Deprecated
    void delete(String elementName) ;
    void deleteComponent(String elementName) ;    
    void deleteAllComponents(String subcircuitName) ;
    
    void createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal);
    
    void createComponent(String elementType, String elementName, int xPosition, int yPosition) ;
    @Deprecated
    void create(String elementType, String elementName, int xPosition, int yPosition) ;

    void setOutputNodeName(String elementName, int nodeIndex, String nodeName) ;
    void setInputNodeName(String elementName, int nodeIndex, String nodeName) ;
    
    String getOutputNodeName(String elementName, int nodeIndex) ;
    String getInputNodeName(String elementName, int nodeIndex) ;

    void rotate(String elementName) ;

    void setOrientation(String elementName, String direction) ;

    void shutdown() ;

    float[] getSignalData(String signalName, double tStart, double tEnd, int skipPoints) ;

    double[] getTimeArray(String signalName, double tStart, double tEnd, int skipPoints) ;

    void setComponentName(final String oldName, final String newName) ;

     double getSimulationTime() ;

     float[][] getGlobalFloatMatrix() ;
     double[][] getGlobalDoubleMatrix() ;
     void setGlobalFloatMatrix(final float[][] matrix) ;
     void setGlobalDoubleMatrix(final double[][] matrix) ;
    float[] floatFFT(final float[] timeValues) ;    
    @Deprecated
    float[] realFFT(final float[] timeValues) ;    
    @Deprecated
    String[] getParametersNames(String componentName) ;
    String[] getAccessibleParameters(String componentName) ;
    
    void setGlobalParameterValue(String parameterName, double value) ;
    double getGlobalParameterValue(String parameterName) ;        
    
    
}
