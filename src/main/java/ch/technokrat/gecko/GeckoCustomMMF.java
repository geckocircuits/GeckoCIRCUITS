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
import ch.technokrat.gecko.geckoscript.AbstractGeckoCustom;
import ch.technokrat.gecko.geckoscript.SimulationAccess;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;

/**
 * The alternative, memory-mapped interface counterpart to the class GeckoCustomRemote.
 * Calls the GeckoRemote functions on the server side via the memory-mapped file approach.
 * @author andrija s.
 */
public class GeckoCustomMMF extends AbstractGeckoCustom {
    
    private GeckoMemoryMappedFile _mmf = null;
    private boolean _accessEnabled = false;
    private long _connectionID = -1;
    
    
    public GeckoCustomMMF(final SimulationAccess access) {
        super(access,null);
    }
    
    @Override
    public void runScript() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * Enable remote access of GeckoCIRCUITS via a memory-mapped file.
     * The file will be reinitialized to a blank disconnected state. Be careful therefore not to pass here files with potentially active connections!
     * This method will initiate a new thread which will poll regularly the contents of the file, to see if a connection should be established.
     * The file is initialized to the default size.
     * @param fileName the name of the file to use
     * @throws FileNotFoundException if the file with the given path cannot be created
     * @throws IOException if there is an error writing to the file
     */
    public void enableAccess(final String fileName) throws FileNotFoundException, IOException {
        _mmf = new GeckoMemoryMappedFile(fileName);
        _accessEnabled = true;
        startMonitoring();
    }
    
    /**
     * Enable remote access of GeckoCIRCUITS via a memory-mapped file.
     * The file will be reinitialized to a blank disconnected state. Be careful therefore not to pass here files with potentially active connections!
     * This method will initiate a new thread which will poll regularly the contents of the file, to see if a connection should be established.
     * The file is initialized to the given size.
     * @param fileName the name of the file to use
     * @param fileSize the size of the memory-mapped file in bytes
     * @throws FileNotFoundException if the file with the given path cannot be created
     * @throws IOException if there is an error writing to the file
     */
    public void enableAccess(final String fileName, final long fileSize) throws FileNotFoundException, IOException {
        _mmf = new GeckoMemoryMappedFile(fileName,fileSize);
        _accessEnabled = true;
        startMonitoring();
    }
    
    /**
     * Disable access to GeckoCIRCUITS via the MMF. The active MMF is deleted!
     */
    public void disableAccess() {
        _accessEnabled = false;
        _connectionID = -1;
        _mmf.forceDisconnect();
        _mmf.deleteFile();
        _mmf = null;
    }
    
    /**
     * Creates a new thread to monitor the MMF for incoming method calls.
     */
    private void startMonitoring() {
        Thread monitoringThread = new Thread(new Runnable() {
           
            @Override
            public void run() {
                monitorMMF();
            }
            
        });
        monitoringThread.start();
    }
    
    private void monitorMMF() {
        GeckoRemotePipeObject methodCall;
        while (_accessEnabled) {
            try {
                if (_mmf.isConnectionAttempt()) { //someone is trying to connect
                    if (_connectionID == -1) { //GeckoCIRCUITS is free currently
                        _connectionID = System.currentTimeMillis();
                        _mmf.acceptConnection(_connectionID);
                    } else { //otherwise reject
                        _mmf.rejectConnection();
                    }
                } else if (_mmf.isDisconnectRequest()) { //existing connection is trying to disconnect
                    _mmf.forceDisconnect();
                    _connectionID = -1;
                } else if (_mmf.isMethodCallPresent()) {
                    methodCall = _mmf.getPipeObject(_connectionID);
                    if (methodCall.isMethodCall()) {
                        callMethod(methodCall);
                    } else {
                        _mmf.setIdle();
                    }
                } else if (_mmf.isShutdownRequest()) {
                    disableAccess();
                    System.exit(0);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                    "Error during memory-mapped remote accces:\n" + ex.getMessage(),
                    "MMF ACCESS ERROR",
                    JOptionPane.ERROR_MESSAGE);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                //nothing to do here
            }
        }
    }
    
    /**
     * Returns the status of the connection.
     * @return a string description for display
     */
    public String getStatus() {
        if (_mmf == null) {
            return "Status: access disabled.";
        } else {
            if (_mmf.isFree()) {
                return "Status: enabled, waiting for incoming connections.";
            } else if (_mmf.getConnectionID() > 0) {
                if (_mmf.isIdle()) {
                    return "Status: connected with session ID: " + _mmf.getConnectionID();
                } else {
                    return "Status: active with session ID: " + _mmf.getConnectionID();
                }
            } else {
                return "Status: (dis)connecting in progress...";
            }
        }                
    }
    
    /**
     * Check whether remote access is enabled.
     * @return true if it is
     */
    public boolean isEnabled() {
        return _accessEnabled;
    }
    
    /**
     * Calls a method indicated by the client via the memory-mapped file and sends the response.
     * @param methodObject
     * @throws IOException if something goes wrong sending the response
     */
    private void callMethod(final GeckoRemotePipeObject methodObject) throws IOException {
        GeckoRemotePipeObject response;
        try {
            //use reflection to get a handle to the right method
            final String name = methodObject.getMethodName();
            final Object[] arguments = methodObject.getMethodArguments();
            final Class[] argumentTypes = new Class[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                argumentTypes[i] = checkForPrimitiveType(arguments[i].getClass()); 
            }
            //special case
            if ("doOperation".equals(name)) {
                argumentTypes[2] = Object.class;
            }
            final Method method = this.getClass().getMethod(name, argumentTypes);
            final Object returnValue = method.invoke(this, arguments);
            if (returnValue == null) { //null return method
                response = new GeckoRemotePipeObject(name);
            } else { //there is a return value to send back
                response = new GeckoRemotePipeObject(name,returnValue);
            }
        } catch (Exception ex) {
            //send back error message
            response = new GeckoRemotePipeObject(methodObject.getMethodName(),ex.getMessage());
        }        
        _mmf.respondToMethodCall(_connectionID, response);
    }
    
    /**
     * Check if there is a connection.
     * 
     * @return true if there is a connection or connection attempt in progress
     */
    public boolean isConnected() {
        if (_mmf == null) {
          return false;  
        } 
        return !(_mmf.isFree());
    }
    
    /**
     * Get the name of the file for remote access.
     * @return the name of the file for access. If access is not enabled, an empty string is returned.
     */
    public String getFile() {
        if (_mmf == null) {
            return "";
        }
        return _mmf.getFileName();
    }
    
    /**
     * Get the memory-mapped file size.
     * 
     * @return the size of the memory-mapped buffer, in bytes
     */
    public long getFileSize() {
        if (_mmf == null) {
            return 0;
        }
        return _mmf.getBufferSize();
    }
    
    /**
     * For transmission, all method arguments are converted to Objects, which means doubles become Doubles, etc.
     * However all the methods take primitive types, therefore we must convert back.
     * This method checks for this and does the conversion.
     * @param type the class of the parameter extracted from the GeckoRemotePipeObject
     * @return the proper class of the parameter
     */
    private Class checkForPrimitiveType(final Class<?> argType) {
        if (argType.equals(Double.class)) {
            return double.class;
        } else if (argType.equals(Integer.class)) {
            return int.class;
        } else if (argType.equals(Boolean.class)) {
            return boolean.class;
        } else if (argType.equals(Float.class)) {
            return float.class;
        } else if (argType.equals(Short.class)) {
            return short.class;
        } else if (argType.equals(Long.class)) {
            return long.class;
        } else if (argType.equals(Byte.class)) {
            return byte.class;
        } else if (argType.equals(Character.class)) {
            return char.class;
        } else {
            return argType;
        }
    }
                
}
