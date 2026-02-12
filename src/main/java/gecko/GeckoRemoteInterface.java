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

import gecko.i18n.resources.I18nKeys;
import java.io.FileNotFoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This an interface for using RMI to control GeckoCIRCUITS from e.g. MATLAB, or
 * remotely from another machine. GeckoCustom implements this interface.
 * Therefore all GeckoSCRIPT functions should be defined here.
 */
public interface GeckoRemoteInterface extends Remote {

    //these are functions for remote access session set up
    boolean isFree() throws RemoteException;
    
    /**
     * Get the session ID for the current GeckoCIRCUITS remote control connection.
     * @return the last active session ID (since there may be multiple clients)
     * @throws RemoteException as any remote method
     * @deprecated this method is meaningless essentially now that multiple clients may be connected to GeckoCIRCUITS. Use @link #checkSessionID(long sessionID) instead.
     */
    @Deprecated
    long getSessionID() throws RemoteException;

    boolean checkSessionID(long sessionID) throws RemoteException;
    
    long connect() throws RemoteException;

    void disconnect(long sessionID) throws RemoteException;

    void acceptExtraConnections(int numberOfExtraConnections) throws RemoteException;
    
    boolean acceptsExtraConnections() throws RemoteException;
    
    void registerLastClientToCallMethod(long sessionID) throws RemoteException;
    
    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_CONTROL_ELEMS_DOC)
    @Declaration("String[] getControlElements()")
    String[] getControlElements() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_CIRCUIT_ELEMS_DOC)
    @Declaration("String[] getCircuitElements()")
    String[] getCircuitElements() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_THERM_ELEMS_DOC)
    @Declaration("String[] getThermalElements()")
    String[] getThermalElements() throws RemoteException;
    
    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_SPEC_ELEMS_DOC)
    @Declaration("String[] getSpecialElements()")
    String[] getSpecialElements() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_IGBTS_DOC)
    @Declaration("String[] getIGBTs()")
    String[] getIGBTs() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_DIODES_DOC)
    @Declaration("String[] getDiodes()")
    String[] getDiodes() throws RemoteException;

    
    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_WORKSHEET_SIZE)
    @Declaration("void setWorksheetSize(int sizeX, int sizeY)")            
    void setWorksheetSize(int sizeX, int sizeY) throws RemoteException;
    
    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GET_WORKSHEET_SIZE)
    @Declaration("int[] getWorksheetSize()")            
    int[] getWorksheetSize() throws RemoteException;
    
    
    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_THYRISTORS_DOC)
    @Declaration("String[] getThyristors()")
    String[] getThyristors() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_IDEAL_SWITCHES_DOC)
    @Declaration("String[] getIdealSwitches()")
    String[] getIdealSwitches() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_RESISTORS_DOC)
    @Declaration("String[] getResistors()")
    String[] getResistors() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_INDUCTORS_DOC)
    @Declaration("String[] getInductors()")
    String[] getInductors() throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.GET_CAPACITORS_DOC)
    @Declaration("String[] getCapacitors()")
    String[] getCapacitors() throws RemoteException;
    
    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.DO_OPERATION_DOC)
    @Declaration("Object doOperation(String elemName, String opName, Object paramValue)")
    Object doOperation(String elementName, String operationName, Object parameterValue) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_PARAMETER_DOCUMENTATION)
    @Declaration("void setParameter(String elementName, String parameterName, double value)")
    void setParameter(String elementName, String parameterName, double value) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_PARAMETERS_DOCUMENTATION)
    @Declaration("void setParameters(String elemName, String[] paramNames, double[] values)")
    void setParameters(String elementName, String[] parameterNames, double[] values) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GET_PARAMETER_DOCUMENTATION)
    @Declaration("double getParameter(String elementName, String parameterName)")
    double getParameter(String elementName, String parameterName) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_OUTPUT_DOCUMENTATION)
    @Declaration("double getOutput(String elementName, String outputName)")
    double getOutput(String elementName, String outputName) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_OUTPUT_SINGLE_PARAMETER)
    @Declaration("double getOutput(String elementName)")
    double getOutput(String elementName) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.INIT_SIMULATION_WO_PARAMETERS)
    @Declaration("void initSimulation()")
    void initSimulation() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.RUNS_THE_SIMULATION_CURRENTLY_LOADED)
    @Declaration("void runSimulation()")
    void runSimulation() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.INIT_SIMULATION_WITH_PARAMETERS)
    @Declaration("void initSimulation(double dt, double endTime)")
    void initSimulation(double deltaT, double endTime) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.CONTINUE_SIMULATION_DOC)
    @Declaration("void continueSimulation()")
    void continueSimulation() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SIMULATE_STEP_DOCUMENTATION)
    @Declaration("void simulateStep()")
    void simulateStep() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SIMULATE_STEPS_DOCUMENTATION)
    @Declaration("void simulateSteps(int steps)")
    void simulateSteps(int steps) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SIMULATE_TIME_DOCUMENTATION)
    @Declaration("void simulateTime(double time)")
    void simulateTime(double time) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.END_SIMULATION_DOCUMENTATION)
    @Declaration("void endSimulation()")
    void endSimulation() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.GET_DT_DOCUMENTATION)
    @Declaration("double get_dt()")
    double get_dt() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.GET_TEND_DOCUMENTATION)
    @Declaration("double get_Tend()")
    double get_Tend() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.GET_DT_PRE_DOCUMENTATION)
    @Declaration("double get_dt_pre()")
    double get_dt_pre() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SET_TEND_PRE_DOCUMENTATION)
    @Declaration("void set_Tend_pre(double Tend)")
    void set_Tend_pre(double value) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.GET_TEND_PRE_DOCUMENTATION)
    @Declaration("double get_Tend_pre()")
    double get_Tend_pre() throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SET_DT_DESCRIPTION)
    @Declaration("void set_dt(double dt)")
    void set_dt(double value) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SET_DT_PRE_DOCUMENTATION)
    @Declaration("void set_dt_pre(double dt)")
    void set_dt_pre(double value) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.GET_SIM_TIME_DOC)
    @Declaration("double getSimulationTime()")
    double getSimulationTime() throws RemoteException;
    
    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SET_THE_TOTAL_SIMULATION_TIME_TO_THE_GIVEN_VALUE_TEND)
    @Declaration("void set_Tend(double value)")
    void set_Tend(double value) throws RemoteException;            

    @Category(MethodCategory.LOAD_SAVE_MODEL)
    @Documentation(I18nKeys.SAVE_FILE_AS_DOCUMENTATION)
    @Declaration("void saveFileAs(String fileName)")
    void saveFileAs(String fileName) throws RemoteException;

    @Category(MethodCategory.LOAD_SAVE_MODEL)
    @Documentation(I18nKeys.OPEN_FILE_DOCUMENTATION)
    @Declaration("void openFile(String fileName) throws FileNotFoundException")
    void openFile(String fileName) throws RemoteException, FileNotFoundException;

    @Category(MethodCategory.LOAD_SAVE_MODEL)
    @Documentation(I18nKeys.IMPORT_FROM_FILE_DOC)
    @Declaration("void importFromFile(String fileName, String insertSubCircuitName)")
    void importFromFile(String fileName, String importIntoSubcircuit) throws RemoteException, FileNotFoundException;
    
    @Category(MethodCategory.LOAD_SAVE_MODEL)
    @Documentation(I18nKeys.SHUTDOWN_DOC)
    @Declaration("void shutdown()")
    void shutdown() throws RemoteException;
    
    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_FOURIER_1_DOC)
    @Declaration("double[][] getFourier(String scope, int port, double tStart, double tStop, int harm)")
    double[][] getFourier(String scopeName, int scopePort, double startTime, double endTime, int harmonics) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_FOURIER_2_DOC)
    @Declaration("double[][] getFourier(String scope, double tStart, double tStop, int harm)")
    double[][] getFourier(String scope, double tStart, double tStop, int harm) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_AVG_DOC)
    @Declaration("double getSignalAvg(String signalName, double startTime, double endTime)")
    double getSignalAvg(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_RMS_DOC)
    @Declaration("double getSignalRMS(String signalName, double startTime, double endTime)")
    double getSignalRMS(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_MIN_DOC)
    @Declaration("double getSignalMin(String signalName, double startTime, double endTime)")
    double getSignalMin(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_MAX_DOC)
    @Declaration("double getSignalMax(String signalName, double startTime, double endTime)")
    double getSignalMax(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_THD_DOC)
    @Declaration("double getSignalTHD(String signalName, double startTime, double endTime)")
    double getSignalTHD(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_SHAPE_DOC)
    @Declaration("double getSignalShape(String signalName, double startTime, double endTime)")
    double getSignalShape(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_KLIRR_DOC)
    @Declaration("double getSignalKlirr(String signalName, double startTime, double endTime)")
    double getSignalKlirr(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIG_RIP_DOC)
    @Declaration("double getSignalRipple(String signalName, double startTime, double endTime)")
    double getSignalRipple(String signalName, double startTime, double endTime) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    double[][] getSignalFourier(String signalName, double startTime, double endTime, int harmonics) throws RemoteException;        

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_POSITION_DOC)
    @Declaration("void setPosition(String elementName, int xPosition, int yPosition)")
    void setPosition(String elementName, int xPosition, int yPosition) throws RemoteException;
    
    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GET_POSITION_DOC)
    @Declaration("int[] getPosition(String elementName)")
    int[] getPosition(String elementName) throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.DELETE_COMPONENT_DOC)
    @Declaration("void deleteComponent(String elementName)")
    void deleteComponent(String elementName) throws RemoteException;

    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.DELETE_ALL_COMPONENTS_DOC)
    @Declaration("void deleteAllComponents(String subcircuitName)")
    void deleteAllComponents(String subcircuitName) throws RemoteException;
    
    
    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.CREATE_COMPONENT_DOC)
    @Declaration("void createComponent(String compType, String newElemName, int sheetPosX, sheetPosY)")
    void createComponent(String elementType, String elementName, int xPosition, int yPosition) throws RemoteException;
    
    @Category(MethodCategory.COMPONENT_CREATION_LISTING)
    @Documentation(I18nKeys.CREATE_CONNECTOR_DOC)
    @Declaration("createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal)")
    void createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) throws RemoteException;
        

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_OUTPUT_NODE_NAME_DOC)
    @Declaration("void setOutputNodeName(String elementName, int nodeIndex, String nodeName)")
    void setOutputNodeName(String elementName, int nodeIndex, String nodeName) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_INPUT_NODE_NAME_DOC)
    @Declaration("void setInputNodeName(String elementName, int nodeIndex, String nodeName)")
    void setInputNodeName(String elementName, int nodeIndex, String nodeName) throws RemoteException;

    
    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GET_OUTPUT_NODE_NAME_DOC)
    @Declaration("String getOutputNodeName(String elementName, int nodeIndex)")
    String getOutputNodeName(String elementName, int nodeIndex) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GET_INPUT_NODE_NAME_DOC)
    @Declaration("String getInputNodeName(String elementName, int nodeIndex)")
    String getInputNodeName(String elementName, int nodeIndex) throws RemoteException;
    
    
    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.ROTATE_DOC)
    @Declaration("void rotate(String elementName)")
    void rotate(String elementName) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_ORIENTATION_DOC)
    @Declaration("void setOrientation(String elementName, String direction)")
    void setOrientation(String elementName, String direction) throws RemoteException;    

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_SIGNAL_DATA_DOCUMENTATION)
    @Declaration("float[] getSignalData(String signalName, double tStart, double tEnd, int skipPoints)")
    float[] getSignalData(String signalName, double tStart, double tEnd, int skipPoints) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.GET_TIME_ARRAY_DOCUMENTATION)
    @Declaration("double[] getTimeArray(String signalName, double tStart, double tEnd, int skipPoints)")
    double[] getTimeArray(String signalName, double tStart, double tEnd, int skipPoints) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.SET_COMPONENT_NAME_DOCUMENTATION)
    @Declaration("void setComponentName(String oldName, String newName) throws Exception")
    void setComponentName(String oldName, String newName) throws Exception;    

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.STORED_MATRIX_DOC)
    @Declaration("float[][] getGlobalFloatMatrix()")
    float[][] getGlobalFloatMatrix() throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.STORED_MATRIX_DOC)
    @Declaration("double[][] getGlobalDoubleMatrix()")
    double[][] getGlobalDoubleMatrix() throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.STORED_MATRIX_DOC)
    @Declaration("void setGlobalFloatMatrix( float[][] matrix)")
    void setGlobalFloatMatrix(float[][] matrix) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.STORED_MATRIX_DOC)
    @Declaration("void setGlobalDoubleMatrix( double[][] matrix)")
    void setGlobalDoubleMatrix(double[][] matrix) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GLOBAL_PAR_DOC)
    @Declaration("void setGlobalParameterValue(String parameterName, double value)")
    void setGlobalParameterValue(String parameterName, double value) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GLOBAL_PAR_DOC)
    @Declaration("double getGlobalParameterValue(String parameterName)")
    double getGlobalParameterValue(String parameterName) throws RemoteException;

    @Category(MethodCategory.COMPONENT_PROPERTIES)
    @Documentation(I18nKeys.GET_ACC_PARAM_DOC)
    @Declaration("String[] getAccessibleParameters(String componentName)")
    String[] getAccessibleParameters(String componentName) throws RemoteException;

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.FLOAT_FFT_DOC)
    @Declaration("float[] floatFFT( float[] timeValues)")
    float[] floatFFT(float[] timeValues) throws RemoteException;

    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.INIT_STEADY_STATE_DETECTION_XC)
    @Declaration("void initSteadyStateDetection(String[] stateVariables, double frequency, double dt, double simulationTime)")
    void initSteadyStateDetection(String[] stateVariables, double frequency, double deltaT,
            double simulationTime) throws RemoteException;
    
    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SIMULATE_TO_STEADY_STATE_DEFAULT)
    @Declaration("double[] simulateToSteadyState(boolean supressMessages)")
    double[] simulateToSteadyState(final boolean supressMessages) throws RemoteException;
    
    @Category(MethodCategory.SIMULATION_START)
    @Documentation(I18nKeys.SIMULATE_TO_STEADY_STATE)
    @Declaration("double[] simulateToSteadyState(boolean supressMessages, double targetCorrelation, double targetMeanPctDiff)")
    double[] simulateToSteadyState(final boolean supressMessages, final double targetCorrelation, final double targetMeanPctDiff) throws RemoteException;
    
    ////////// here, we place the deprecated methods. Don't document them, they are deprecated!
    // in case you declare a new deprecated interface method, please REMOVE its documentation!
    
    //@Documentation(I18nKeys.SET_LOSS_FILE_DOC)
    //@Declaration("void setLossFile(String elementName, String lossFileName) throws FileNotFoundException")
    @Deprecated
    void setLossFile(String elementName, String lossFileName) throws RemoteException, FileNotFoundException;

    //@Documentation(I18nKeys.SET_NONLIN_DOC)
    //@Declaration("void setNonLinear(String elementName, String characteristicFileName) throws FileNotFoundException")
    @Deprecated
    void setNonLinear(String elementName, String characteristicFileName) throws RemoteException, FileNotFoundException;
    
    
    //@Category(MethodCategory.SIMULATION_START)
    //@Documentation(I18nKeys.INIT_STEADY_STATE_DETECTION)
    //@Declaration("void initSteadyStateDetection(String[] stateVariables, double[] frequencies, double dt, double simulationTime)")
    @Deprecated
    void initSteadyStateDetection(String[] stateVariables, double[] frequencies, double deltaT,
            double simulationTime) throws RemoteException;

    //@Category(MethodCategory.SIMULATION_START)
    //@Documentation(I18nKeys.SIMULATE_UNTIL_STEADY_STATE)
    //@Declaration("double[] simulateUntilSteadyState(boolean supressMessages)")
    @Deprecated
    double[] simulateUntilSteadyState(boolean supressMessages) throws RemoteException;
    
    
    @Deprecated
    void create(String elementType, String elementName, int xPosition, int yPosition) throws RemoteException;

    @Deprecated
    void delete(String elementName) throws RemoteException;

    @Deprecated
    float[] realFFT(float[] timeValues) throws RemoteException;

    @Deprecated
    String[] getParametersNames(String componentName) throws RemoteException;

    @Deprecated
    double[] getSignalCharacteristics(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getAvg(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getRMS(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getTHD(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getMin(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getMax(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getRipple(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getKlirr(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getShape(String scopeName, int scopePort, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double[] getSignalCharacteristics(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getAvg(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getRMS(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getMin(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getMax(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getTHD(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getShape(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getKlirr(String scopeName, double startTime, double endTime) throws RemoteException;

    @Deprecated
    double getRipple(String scopeName, double startTime, double endTime) throws RemoteException;
}
