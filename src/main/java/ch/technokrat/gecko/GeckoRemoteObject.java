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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a wrapper for use in from external programs, e.g. MATLAB or
 * other Java programs. The communication is done via a network socket. Since
 * the method count of this class is ever-increasing, I wrote some reflection
 * code which is doing the wrapper operations. PLEASE (WARNING!) Whenever you
 * modify something here, then execute the corresponding JUnit test. This test
 * ensures that nothing goes wrong with reflection and all the methods (method
 * names, argument types, ...).
 *
 */
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.NullAssignment"})
public class GeckoRemoteObject {

    public GeckoRemoteObject() {        
        //use factory method instead of constructor
    }
    
    private int portNumber;
    GeckoRemoteInterface _wrapped = null;
    GeckoRemoteIntWithoutExc _proxy;
    private final int NO_SESSION_ID = -2;
    private long sessionID = NO_SESSION_ID;
    private static final String REGISTRY_NAME = "GeckoRemoteInterface";
    private static final String ERROR_STRING = "Error with calling remote method. See nested exception for details.";
    private RemoteInvocationHandler _invocationHandler;
    private boolean doProxyCheck = true;
    private static String _pathToJava = "";
    
    public class RemoteInvocationHandler implements InvocationHandler {

        private final Object object;
        private final Map<Method, Method> _methodMap = new HashMap<Method, Method>();

        public RemoteInvocationHandler(final Object object) throws NoSuchMethodException {
            this.object = object;
            for (Method meth : GeckoRemoteIntWithoutExc.class.getMethods()) {
                Method toMap;
                toMap = object.getClass().getMethod(meth.getName(), meth.getParameterTypes());
                _methodMap.put(meth, toMap);
            }
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (doProxyCheck) {
                checkRemoteWithException();
            }
            try {
                return _methodMap.get(method).invoke(this.object, args);
            } catch (Throwable ex) {
                throw ex.getCause();
            }

        }
    }

    /**
     * start GeckoCIRCUITS, set it up to use that port
     *
     * @param port the port at which GeckoCIRCUITS should listen for incoming connections
     * @return a GeckoRemoteObject connected to this new instance of GeckoCIRCUITS which can be used for controlling it remotely
     */
    public static GeckoRemoteObject startNewRemoteInstance(final int port) {
        if (portFree(port)) {
            GeckoSim.operatingmode = OperatingMode.REMOTE;
            final List<String> argsList = new ArrayList<String>();
            if(!_pathToJava.isEmpty()) {
                argsList.add("-j");
                argsList.add(_pathToJava);
            }
            argsList.add("-p");
            argsList.add(Integer.toString(port));
            final String[] args = argsList.toArray(new String[argsList.size()]);
            
            for(String arg : args) {
                System.out.println("arg " + arg);
            }
            GeckoSim.main(args);
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(GeckoRemoteObject.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (GeckoSim.remoteLoaded) {                
                return connectToExistingInstance(port);
            }
        } else {
            throw new RuntimeException("Error, port: " + port + " is blocked/occupied.");
        }
        assert false;
        return null;
    }

    public static GeckoRemoteObject connectToExistingInstance(final String host, final int port) {
        GeckoRemoteObject returnValue = new GeckoRemoteObject();
        try {
            final Registry registry = LocateRegistry.getRegistry(host, port);
            final GeckoRemoteInterface geckoInstance = (GeckoRemoteInterface) registry.lookup(REGISTRY_NAME);
            returnValue.connectToExistingInstance(geckoInstance, port);

            CallbackClientInterface callbackObj = new CallbackClientImpl();
            // register for callback
            if (geckoInstance instanceof CallbackServerInterface) {
                ((CallbackServerInterface) geckoInstance).registerForCallback(callbackObj);
            } else {
                System.err.println("Could not establish connection for callback. \nOutput messages from GeckoCIRCUITS"
                        + "may be dropped to nowhere!");
            }
        } catch (RemoteException ex) {
            throw new RuntimeException("There is no GeckoCIRCUITS instance at port " + port, ex);
        } catch (NotBoundException ex) {
            throw new RuntimeException("There is no GeckoCIRCUITS instance at port " + port + " or it is not enabled for remote access.", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Error connecting to GeckoCIRCUITS at port " + port, ex);
        }
        return returnValue;
    }

    public static GeckoRemoteObject connectToExistingInstance(final int port) {
        return connectToExistingInstance("localhost", port);
    }

    public static GeckoRemoteObject connectToGecko() {
        Fenster.IS_APPLET = false; // this is needed - otherwise we cannot read the properties
        GeckoSim.forceLoadApplicationProperties();
        return connectToExistingInstance(GeckoRemoteRegistry.getRemoteAccessPort());
    }

    public static GeckoRemoteObject startNewRemoteInstance() {
        return startNewRemoteInstance(GeckoRemoteRegistry.getRemoteAccessPort());
    }

    /**
     * check whether this port is already used or if it's free and if it's used,
     * is already a GeckoCIRCUITS instance running or something else
     *
     * @param port
     * @return
     */
    @SuppressWarnings({"PMD.DoNotThrowExceptionInFinally", "PMD.PreserveStackTrace"})
    private static boolean portFree(final int port) {
        boolean portFree = true;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
        } catch (IOException ex) {
            portFree = false;
            //this means that port is used - maybe already by another GeckoCIRCUITS instance - check if this is so
            GeckoRemoteInterface existingInstance = null;

            try {
                final Registry registry = LocateRegistry.getRegistry("localhost", port);
                existingInstance = (GeckoRemoteInterface) registry.lookup(REGISTRY_NAME);
            } catch (Exception originalException) { //this means there is no GeckoCIRCUITS already running there
                final String message = "Port " + port + " is already occupied by another process or is blocked. Please try another port number.";
                throw new RuntimeException(message, originalException);
            } finally { //this port is already occupied by a GeckoCIRCUITS instance - attempt to connect to it
                if (existingInstance != null) {
                    System.out.println("Port " + port + " is already occupied by GeckoCIRCUITS.");
                }
            }
        } finally {
            // Clean up
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    portFree = false;
                    throw new RuntimeException("Error with using port " + port + ". Try another port number.", e);
                }
            }
        }

        return portFree;
    }

    //try to connect to an existing instance of GeckoCIRCUITS
    private void connectToExistingInstance(final GeckoRemoteInterface existingInstance, final int port) {
        try {
            if (existingInstance.isFree() && !existingInstance.checkSessionID(sessionID)) { //with multiple connections, GeckoCIRCUITS may be free AND this client may be already connected!
                try {
                    final long newSessionID = existingInstance.connect();
                    if (_wrapped != null) {
                        disconnectFromGecko();
                    }
                    _wrapped = existingInstance;

                    ClassLoader loader = _wrapped.getClass().getClassLoader();
                    Class[] interfaces = new Class[]{GeckoRemoteIntWithoutExc.class};

                    _invocationHandler = new RemoteInvocationHandler(_wrapped);
                    _proxy = (GeckoRemoteIntWithoutExc) Proxy.newProxyInstance(loader, interfaces, _invocationHandler);

                    sessionID = newSessionID;
                    portNumber = port;
                    _wrapped.registerLastClientToCallMethod(sessionID);
                    System.out.println("You are now connected to the GeckoCIRCUITS instance at port " + port);
                } catch (Exception e) {
                    throw new RuntimeException("Error connecting to existing GeckoCIRCUITS instance at port " + port, e);
                }
            } else {
                if (_wrapped != null) {
                    if (_wrapped.checkSessionID(sessionID)) {
                        System.out.println("You are already connected to the GeckoCIRCUITS instance available at port " + port);
                        _wrapped.registerLastClientToCallMethod(sessionID);
                    } else {
                        System.out.println("The GeckoCIRCUITS instance at port " + port
                                + " is busy with (an)other session(s). You cannot connect to it now.");
                    }
                } else {
                    throw new RuntimeException("The GeckoCIRCUITS session at port " + port + " is already in use by "
                            + "another process.");
                }
            }
        } catch (RemoteException ex) {
            throw new RuntimeException("Error connecting to existing GeckoCIRCUITS instance at port " + port, ex);
        }

    }
    
    /**
     * Set the path of the java executable to use (needed when starting GeckoCIRCUITS from a Netbeans platform application)
     * @param pathToJavaExe the path to the java executable
     */
    public static void setJavaPath(final String pathToJavaExe) {
        if(new File(pathToJavaExe).exists()) {
            _pathToJava = pathToJavaExe;
        } else {
            throw new RuntimeException("Error: Path to Java executable: " + pathToJavaExe + " does not exist!");
        }        
    }
    
    /**
     * Allow additional clients to simultaneously connect to the connected-to instance of GeckoCIRCUITS.
     * @param additionalClients the number of additional (!) clients (beside the original one) that can be allowed to connect remotely to GeckoCIRCUITS
     *                          if this number is set to 0 or less, only one client will be allowed to connect at a time.
     *                          if this number is set to e.g. 2, 3 clients in total will be allowed to connect a a time.
     * NOTE: disabling extra client connections (e.g. passing 0) does NOT disconnect any currently connected clients; it only affects clients which attempt
     *       to connect in the future.
     */
    public void allowAdditionalClients(final int additionalClients) {
        try {
            if (checkRemote()) {
                _wrapped.acceptExtraConnections(additionalClients);
            } 
        } catch (RemoteException e) {
            throw new RuntimeException("Error trying to enable additional client connections!",e);
        }
    }
    
    
    @SuppressWarnings("PMD.NonThreadSafeSingleton")
    public void disconnectFromGecko() {
        try {
            if (_wrapped == null) {
                System.out.println("You have no existing connections to GeckoCIRCUITS");
            } else {
                _wrapped.disconnect(sessionID);
                sessionID = NO_SESSION_ID;
                _wrapped = null;
                System.out.println("You have been disconnected from the GeckoCIRCUITS instance at port " + portNumber);
            }

        } catch (RemoteException e) {
            throw new RuntimeException("Error disconnecting from GeckoCIRCUITS instance at port " + portNumber
                    + "\nTo force a (potentially one-sided) disconnect, please call forceDisconnectFromGecko().", e);
        }
    }

    public void forceDisconnectFromGecko() {
        //disconnects from the session on the client side only (i.e. remote side crashed)
        _wrapped = null;
        sessionID = NO_SESSION_ID;
    }

    private void checkRemoteWithException() {
        if (_wrapped == null) {
            throw new RuntimeException("You are NOT connected to any instance of GeckoCIRCUITS! Use startGui(port) or"
                    + " connectToGecko(port) to establish a connection.");
        } else {
            //check the session ID
            try {
                if (!_wrapped.checkSessionID(sessionID)) {
                    throw new RuntimeException("Invalid session ID. Please restart your connection by "
                            + "calling disconnectFromGecko() and then reconnecting.");
                }
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private boolean checkRemote() throws RemoteException {
        if (_wrapped == null) {
            System.err.println("You are NOT connected to any instance of GeckoCIRCUITS! Use startGui(port) or"
                    + " connectToGecko(port) to establish a connection.");
            return false;
        } else {
            //check the session ID
            if (_wrapped.checkSessionID(sessionID)) {
                return true;
            } else {
                System.out.println("Invalid session ID. Restart your connection by "
                        + "calling disconnectFromGecko() and then reconnecting.");
                return false;
            }
        }
    }

    /**
     * exit the JVM.
     */
    @SuppressWarnings("PMD") // here, we really want an empty catch block!
    public void shutdown() {
        try {
            if (_wrapped != null) {
                _wrapped.shutdown();
            }
            //CHECKSTYLE:OFF
        } catch (Exception exc) {
            // CHECKSTYLE:ON
            // yes, do nothing here! This exception is thrown, since the other side is closed.
        }

        forceDisconnectFromGecko();

    }

    public void simulateStep() {
        // careful: here, i don't call _proxy.(), since this method is 
        //critical to simulation runtime (possibly called very often).
        try {
            if (checkRemote()) {
                _wrapped.simulateStep();
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING, ex);
        }
    }

    public void simulateSteps(final int steps) {
        // careful: here, i don't call _proxy.(), since this method is 
        //critical to simulation runtime (possibly called very often).
        try {
            if (checkRemote()) {
                _wrapped.simulateSteps(steps);
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING, ex);
        }
    }

    public String[] getAccessibleParameters(final String componentName) {
        return _proxy.getAccessibleParameters(componentName);
    }

    public String[] getControlElements() {
        return _proxy.getControlElements();
    }

    public String[] getCircuitElements() {
        return _proxy.getCircuitElements();
    }

    public String[] getThermalElements() {
        return _proxy.getThermalElements();
    }
    
    public String[] getSpecialElements() {
        return _proxy.getSpecialElements();
    }

    public String[] getIGBTs() {
        return _proxy.getIGBTs();
    }

    public String[] getDiodes() {
        return _proxy.getDiodes();
    }

    public String[] getThyristors() {
        return _proxy.getThyristors();
    }

    public String[] getIdealSwitches() {
        return _proxy.getIdealSwitches();
    }

    public String[] getResistors() {
        return _proxy.getResistors();
    }

    public String[] getInductors() {
        return _proxy.getInductors();
    }

    public String[] getCapacitors() {
        return _proxy.getCapacitors();
    }

    public Object doOperation(final String elementName, final String operationName, final Object parameterValue) {
        return _proxy.doOperation(elementName, operationName, parameterValue);
    }

    public void setGlobalParameterValue(final String parameterName, final double value) {
        _proxy.setGlobalParameterValue(parameterName, value);
    }

    public double getGlobalParameterValue(final String parameterName) {
        return _proxy.getGlobalParameterValue(parameterName);
    }

    public void setParameter(final String elementName, final String parameterName, final double value) {
        _proxy.setParameter(elementName, parameterName, value);
    }

    public void setParameters(final String elementName, final String[] parameterNames, final double[] values) {
        _proxy.setParameters(elementName, parameterNames, values);
    }

    public double getParameter(final String elementName, final String parameterName) {
        return _proxy.getParameter(elementName, parameterName);
    }

    public double getOutput(final String elementName, final String outputName) {
        return _proxy.getOutput(elementName, outputName);
    }

    public double getOutput(final String elementName) {
        return _proxy.getOutput(elementName);
    }

    public void runSimulation() {
        _proxy.runSimulation();
    }

    public void initSimulation() {
        _proxy.initSimulation();
    }

    public void initSimulation(final double deltaT, final double endTime) {
        _proxy.initSimulation(deltaT, endTime);
    }

    public void continueSimulation() {
        _proxy.continueSimulation();
    }

    public void simulateTime(final double time) {
        _proxy.simulateTime(time);
    }

    public double getSimulationTime() {
        return _proxy.getSimulationTime();
    }

    public void endSimulation() {
        _proxy.endSimulation();
    }

    public void saveFileAs(final String fileName) {
        _proxy.saveFileAs(fileName);
    }

    public void openFile(final String fileName) throws FileNotFoundException {
        _proxy.openFile(fileName);
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public double get_dt() {
        return _proxy.get_dt();
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public double get_dt_pre() {
        return _proxy.get_dt_pre();
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public double get_Tend() {
        return _proxy.get_Tend();
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_dt(final double value) {
        _proxy.set_dt(value);
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_Tend(final double value) {
        _proxy.set_Tend(value);
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF    
    public double get_Tend_pre() {
        return _proxy.get_Tend_pre();
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_dt_pre(final double value) {
        _proxy.set_dt_pre(value);
    }

    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_Tend_pre(final double value) {
        _proxy.set_Tend_pre(value);
    }
    
    @Deprecated
    public double[][] getFourier(final String scopeName, final int scopePort, final double startTime,
            final double endTime, final int harmonics) {
        return _proxy.getFourier(scopeName, scopePort, startTime, endTime, harmonics);
    }

    @Deprecated
    public double[][] getFourier(final String scopeName, final double startTime, final double endTime,
            final int harmonics) {
        return getFourier(scopeName, 0, startTime, endTime, harmonics);
    }
    
    @Deprecated
    public void initSteadyStateDetection(final String[] stateVariables, final double[] frequencies,
            final double deltaT, final double simulationTime) {
        _proxy.initSteadyStateDetection(stateVariables, frequencies, deltaT, simulationTime);
    }
    
    public void initSteadyStateDetection(String[] stateVariables, double frequency, double deltaT,
            double simulationTime) {
        _proxy.initSteadyStateDetection(stateVariables, frequency, deltaT, simulationTime);
    }
    
    @Deprecated
    public double[] simulateUntilSteadyState(final boolean supressMessages) {
        return _proxy.simulateUntilSteadyState(supressMessages);
    }
    
    public double[] simulateToSteadyState(final boolean supressMessages) {
        return _proxy.simulateToSteadyState(supressMessages);
    }
    
    public double[] simulateToSteadyState(final boolean supressMessages, final double targetCorrelation, final double targetMeanPctDiff) {
        return _proxy.simulateToSteadyState(supressMessages, targetCorrelation, targetMeanPctDiff);
    }

    public void setPosition(final String elementName, final int xCoord, final int yCoord) {
        _proxy.setPosition(elementName, xCoord, yCoord);
    }
    
    public int[] getPosition(final String elementName) {
        return _proxy.getPosition(elementName);
    }

    public void deleteComponent(final String elementName) {
        _proxy.deleteComponent(elementName);
    }
    
    public void deleteAllComponents(final String subcircuitName) {
        _proxy.deleteAllComponents(subcircuitName);
    }
   

    public void createComponent(final String elementType, final String elementName, final int xCoord, final int yCoord) {
        _proxy.createComponent(elementType, elementName, xCoord, yCoord);
    }

    public void setOutputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        _proxy.setOutputNodeName(elementName, nodeIndex, nodeName);
    }

    public void setInputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        _proxy.setInputNodeName(elementName, nodeIndex, nodeName);
    }
    
    public String getOutputNodeName(final String elementName, final int nodeIndex) {
        return _proxy.getOutputNodeName(elementName, nodeIndex);
    }

    public String getInputNodeName(final String elementName, final int nodeIndex) {
        return _proxy.getInputNodeName(elementName, nodeIndex);
    }

    public double[] getTimeArray(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        return _proxy.getTimeArray(signalName, tStart, tEnd, skipPoints);
    }

    public float[] getSignalData(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        return _proxy.getSignalData(signalName, tStart, tEnd, skipPoints);
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
    public void setComponentName(final String oldName, final String newName) {
        _proxy.setComponentName(oldName, newName);
    }

    public void rotate(final String elementName) {
        assert elementName != null;
        _proxy.rotate(elementName);
    }

    public void setOrientation(final String elementName, final String direction) {
        _proxy.setOrientation(elementName, direction);
    }

    public void importFromFile(final String fileName, final String subCircuitName) throws FileNotFoundException {
        _proxy.importFromFile(fileName, subCircuitName);
    }

    public float[][] getGlobalFloatMatrix() {
        return _proxy.getGlobalFloatMatrix();
    }

    public double[][] getGlobalDoubleMatrix() {
        return _proxy.getGlobalDoubleMatrix();
    }

    public void setGlobalFloatMatrix(final float[][] matrix) {
        _proxy.setGlobalFloatMatrix(matrix);
    }

    public void setGlobalDoubleMatrix(final double[][] matrix) {
        _proxy.setGlobalDoubleMatrix(matrix);
    }

    public double getSignalAvg(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalAvg(signalName, startTime, endTime);
    }

    public double getSignalRMS(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalRMS(signalName, startTime, endTime);
    }

    public double getSignalTHD(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalTHD(signalName, startTime, endTime);
    }

    public double getSignalMin(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalMin(signalName, startTime, endTime);
    }

    public double getSignalMax(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalMax(signalName, startTime, endTime);
    }

    public double getSignalRipple(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalRipple(signalName, startTime, endTime);
    }

    public double getSignalKlirr(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalKlirr(signalName, startTime, endTime);
    }

    public double getSignalShape(final String signalName, final double startTime, final double endTime) {
        return _proxy.getSignalShape(signalName, startTime, endTime);
    }

    public double[][] getSignalFourier(final String signalName, final double startTime,
            final double endTime, final int harmonics) {
        return _proxy.getSignalFourier(signalName, startTime, endTime, harmonics);
    }

    public float[] floatFFT(final float[] timeDomainData) {
        return _proxy.floatFFT(timeDomainData);
    }
}
