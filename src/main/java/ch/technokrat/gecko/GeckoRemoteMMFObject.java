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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a wrapper for use in from external programs, e.g. MATLAB or
 * other Java programs. The communication is done via a memory-mapped file (MMF). 
 * This is the alternative, non-network counterpart to GeckoRemoteObject.
 *
 * It is implemented as a child class of GeckoRemoteObject to make inserting the alternative 
 * interface into e.g. GeckoMAGNETICS easier. Ideally both should implement a common interface.
 * 
 * @author andrija s.
 */
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.NullAssignment"})
public final class GeckoRemoteMMFObject extends GeckoRemoteObject {

    public GeckoRemoteMMFObject() {        
        //use factory method instead of constructor
    }
    
    GeckoMemoryMappedFile _mmf = null;
    private final int NO_SESSION_ID = -1;
    private long sessionID = NO_SESSION_ID;
    private static final String ERROR_STRING = "Error with calling remote method. See nested exception for details:\n";
    private static String _pathToJava = "";
    
    

    /**
     * Start GeckoCIRCUITS, set it up to use a given memory-mapped file.
     * The memory-mapped file should NOT exist in order to start a fresh new instance of GeckoCIRCUITS!
     * If the file already exists, this method will treat it as an existing GeckoCIRCUITS instance which is already running,
     * and will try to connect to it - which may fail if the file does not actually correspond to a running GeckoCIRCUITS instance!
     *
     * @param file the file which should memory-mapped to communicate with GeckoCIRCUITS
     * @param size the size of the file (in MB!) to use
     * @return a GeckoRemoteMFFObject connected to this new instance of GeckoCIRCUITS which can be used for controlling it remotely
     */
    public static GeckoRemoteMMFObject startNewRemoteInstance(final String file, final long size) {
        final File mmfile = new File(file);
        if (!mmfile.exists()) { //file does not exist - can be created by a new GeckoCIRCUITS instance
            GeckoSim.operatingmode = OperatingMode.MMF;
            final List<String> argsList = new ArrayList<String>();
            if(!_pathToJava.isEmpty()) {
                argsList.add("-j");
                argsList.add(_pathToJava);
            }
            argsList.add("-mm"); //-mm is argument to enable memory-mapped access on startup
            argsList.add(file);
            argsList.add(Long.toString(size*1024*1024)); //convert MB to bytes
            final String[] args = argsList.toArray(new String[argsList.size()]);
            
            for(String arg : args) {
                System.out.println("arg " + arg);
            }
            GeckoSim.main(args);
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(GeckoRemoteMMFObject.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (GeckoSim.mmfLoaded) {                
                return connectToExistingInstance(file);
            }
        } else {
            System.out.println("The given file " + file + " already exists. Will attempt to treat it as a runnning GeckoCIRCUITS instance\nand connect to it. If this fails, please try a different (non-existing!) file name.");
            return connectToExistingInstance(file);

        }
        assert false;
        return null;
    }

    
    /**
     * Start GeckoCIRCUITS, set it up to use a given memory-mapped file, initialized to the default size.
     * The memory-mapped file should NOT exist in order to start a fresh new instance of GeckoCIRCUITS!
     * If the file already exists, this method will treat it as an existing GeckoCIRCUITS instance which is already running,
     * and will try to connect to it - which may fail if the file does not actually correspond to a running GeckoCIRCUITS instance!
     *
     * @param file the file which should memory-mapped to communicate with GeckoCIRCUITS
     * @return a GeckoRemoteObject connected to this new instance of GeckoCIRCUITS which can be used for controlling it remotely
     */
    public static GeckoRemoteMMFObject startNewRemoteInstance(final String file) {
        return startNewRemoteInstance(file,GeckoMemoryMappedFile._defaultBufferSize);
    }
    
   /**
    * Connect to a running GeckoCIRCUITS instance via a memory-mapped file.
    * For success,
    * 1) GeckoCIRCUITS must be running with MMF access enabled
    * 2) The file must exist and given as the access for MMF
    * 3) GeckoCIRCUITS must not be occupied by another connection
    * @param file the file to use for MMF access. If it does not exist, an error is thrown
    * @return a GeckoRemoteMFFObject connected to this instance of GeckoCIRCUITS which can be used for controlling it remotely
    */
    public static GeckoRemoteMMFObject connectToExistingInstance(final String file) {
        GeckoRemoteMMFObject returnValue = new GeckoRemoteMMFObject();
        final File mmfile = new File(file);
        if (mmfile.exists()) {
            try {
                final GeckoMemoryMappedFile mmf = GeckoMemoryMappedFile.getGeckoMemoryMappedFile(file);
                returnValue.connectToExistingInstance(mmf);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new RuntimeException("Given file for remote access " + file + " does not exist!");
        }
        return returnValue;
    }

   
    /**
     * Connect to GeckoCIRCUITS via a memory-mapped file.
     * @param file the file which to connect through
     */
    private void connectToExistingInstance(final GeckoMemoryMappedFile file) {
        //try {
            if (file.isFree()) { 
                //try {
                    final long newSessionID = file.connect(10000); //wait for 10 s
                    if (newSessionID > 0) {
                        if (_mmf != null) {
                            disconnectFromGecko();
                        }
                        _mmf = file;
                        sessionID = newSessionID;
                        System.out.println("You are now connected to the GeckoCIRCUITS instance via file " + file.getFileName());
                    } else if (newSessionID == 0) {
                        throw new RuntimeException("Connection to GeckoCIRCUITS via file " + file.getFileName() + " rejected by that instance of GeckoCIRCUITS.");
                    } else if (newSessionID == -1) {
                        throw new RuntimeException("The GeckoCIRCUITS instance using file " + file.getFileName()
                                + " is busy with another session. You cannot connect to it now.");
                    } else { // -2
                        throw new RuntimeException("Connection attempt to GeckoCIRCUITS via " + file.getFileName()
                                + " has timed out. Please check whether this file really belongs to a running GeckoCIRCUITS instance.");
                    }                   
                //} catch (Exception e) {
                    //throw new RuntimeException("Error connecting to existing GeckoCIRCUITS instance at file " + file.getFileName(), e);
                //}
            } else {
                if (_mmf != null) {
                    if (_mmf.getConnectionID() == sessionID) {
                        System.out.println("You are already connected to the GeckoCIRCUITS instance available through file " + file.getFileName());
                    } else {
                        System.out.println("The GeckoCIRCUITS instance at file " + file.getFileName()
                                + " is busy with (an)other session(s). You cannot connect to it now.");
                    }
                } else {
                    throw new RuntimeException("The GeckoCIRCUITS session at file " + file.getFileName() + " is already in use by "
                            + "another process.");
                }
            }
        //} catch (Exception ex) {
            //throw new RuntimeException("Error connecting to existing GeckoCIRCUITS instance through file " + file.getFileName() + ":\n" + ex.getMessage(), ex);
        //}

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
    
        
    @Override
    @SuppressWarnings("PMD.NonThreadSafeSingleton")
    public void disconnectFromGecko() {
        try {
            if (_mmf == null) {
                System.out.println("You have no existing connections to GeckoCIRCUITS");
            } else {
                _mmf.disconnect(sessionID);
                sessionID = NO_SESSION_ID;
                System.out.println("You have been disconnected from the GeckoCIRCUITS instance at file" + _mmf.getFileName());
                _mmf = null;
            }

        } catch (Exception e) {
            throw new RuntimeException("Error disconnecting from GeckoCIRCUITS instance at file " + _mmf.getFileName()
                    + "\nTo force a (potentially one-sided) disconnect, please call forceDisconnectFromGecko().\n"+e.getMessage(), e);
        }
    }
    
    @Override
    public void forceDisconnectFromGecko() {
        //disconnects from the session on the client side only (i.e. remote side crashed)
        _mmf = null;
        sessionID = NO_SESSION_ID;
    }

    private boolean checkRemote() {
        if (_mmf == null) {
            System.err.println("You are NOT connected to any instance of GeckoCIRCUITS! Use startNewRemoteInstance(file,size) or"
                    + " connectToExistingInstance(file) to establish a connection.");
            return false;
        } else {
            //check the session ID
            if (_mmf.getConnectionID() == sessionID) {
                return true;
            } else {
                System.out.println("Invalid session ID. Restart your connection by "
                        + "calling disconnectFromGecko() and then reconnecting.");
                return false;
            }
        }
    }

    
    /**
     * NOTE: Ideally, we would for all the remaining methods, like to get the method name by reflection.
     * This is ugly however in terms of compilation, see: http://stackoverflow.com/questions/442747/getting-the-name-of-the-current-executing-method
     * Therefore, in each method, we construct the GeckoRemotePipeObject manually.
     * Annoying, but gives the best performance.
     */
    
    
    /**
     * exit the JVM.
     */
    @Override
    @SuppressWarnings("PMD") //emtpy catch block needed, really
    public void shutdown() {
        try {
            if (_mmf == null) {
                System.out.println("You have no existing connections to GeckoCIRCUITS");
            } else {
                _mmf.shutdown(sessionID);
                sessionID = NO_SESSION_ID;
                System.out.println("GeckoCIRCUITS has been shutdown.");
                _mmf = null;
            }

        } catch (Exception e) {
            throw new RuntimeException("Error disconnecting from GeckoCIRCUITS instance at file " + _mmf.getFileName()
                    + "\nTo force a (potentially one-sided) disconnect, please call forceDisconnectFromGecko().\n"+e.getMessage(), e);
        }

        forceDisconnectFromGecko();

    }

    @Override
    public void simulateStep() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("simulateStep",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method simulateStep!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void simulateSteps(final int steps) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("simulateSteps",new Object[]{steps});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method simulateSteps!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getAccessibleParameters(final String componentName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getAccessibleParameters",new Object[]{componentName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getAccessibleParameters!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getControlElements() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getControlElements",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getControlElements!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public String[] getCircuitElements() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getCircuitElements",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getCircuitElements!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getThermalElements() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getThermalElements",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getThermalElements!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public String[] getSpecialElements() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSpecialElements",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSpecialElements!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getIGBTs() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getIGBTs",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getIGBTs!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public String[] getDiodes() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getDiodes",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getDiodes!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getThyristors() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getThyristors",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getThyristors!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getIdealSwitches() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getIdealSwitches",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getIdealSwitches!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getResistors() {
       try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getResistors",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getResistors!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getInductors() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getInductors",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getInductors!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getCapacitors() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getCapacitors",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getCapacitors!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public Object doOperation(final String elementName, final String operationName, final Object parameterValue) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("doOperation",new Object[]{elementName,operationName,parameterValue});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue() || returnValue.isVoidReturn()) {
                    return returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method doOperation!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void setGlobalParameterValue(final String parameterName, final double value) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setGlobalParameterValue",new Object[]{parameterName,value});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setGlobalParameterValue!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getGlobalParameterValue(final String parameterName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getGlobalParameterValue",new Object[]{parameterName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getGlobalParameterValue!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    

    @Override
    public void setParameter(final String elementName, final String parameterName, final double value) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setParameter",new Object[]{elementName,parameterName,value});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setParameter!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void setParameters(final String elementName, final String[] parameterNames, final double[] values) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setParameters",new Object[]{elementName,parameterNames,values});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setParameters!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getParameter(final String elementName, final String parameterName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getParameter",new Object[]{elementName,parameterName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getParameter!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getOutput(final String elementName, final String outputName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getOutput",new Object[]{elementName,outputName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getOutput!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getOutput(final String elementName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getOutput",new Object[]{elementName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getOutput!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void runSimulation() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("runSimulation",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method runSimulation!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public void initSimulation() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("initSimulation",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method initSimulation!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void initSimulation(final double deltaT, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("initSimulation",new Object[]{deltaT,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method initSimulation!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void continueSimulation() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("continueSimulation",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method continueSimulation!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void simulateTime(final double time) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("simulateTime",new Object[]{time});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method simulateTime!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSimulationTime() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSimulationTime",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSimulationTime!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void endSimulation() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("endSimulation",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method endSimulation!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public void saveFileAs(final String fileName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("saveFileAs",new Object[]{fileName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method saveFileAs!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void openFile(final String fileName) throws FileNotFoundException {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("openFile",new Object[]{fileName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method openFile!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public double get_dt() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("get_dt",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method get_dt!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public double get_dt_pre() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("get_dt_pre",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method get_dt_pre!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public double get_Tend() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("get_Tend",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method get_Tend!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_dt(final double value) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("set_dt",new Object[]{value});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method set_dt!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_Tend(final double value) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("set_Tend",new Object[]{value});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method set_Tend!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF    
    public double get_Tend_pre() {
         try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("get_Tend_pre",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method get_Tend_pre!");
                }
                        
            } else {
                return 0;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_dt_pre(final double value) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("set_dt_pre",new Object[]{value});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method set_dt_pre!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("PMD")
    //CHECKSTYLE:OFF
    public void set_Tend_pre(final double value) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("set_Tend_pre",new Object[]{value});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method set_Tend_pre!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double[][] getFourier(final String scopeName, final int scopePort, final double startTime,
            final double endTime, final int harmonics) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getFourier",new Object[]{scopeName,scopePort,startTime,endTime,harmonics});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (double[][])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getFourier!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double[][] getFourier(final String scopeName, final double startTime, final double endTime,
            final int harmonics) {
        return getFourier(scopeName, 0, startTime, endTime, harmonics);
    }

    
    @Override
    public void setPosition(final String elementName, final int xCoord, final int yCoord) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setPosition",new Object[]{elementName,xCoord,yCoord});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setPosition!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public int[] getPosition(final String elementName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getPosition",new Object[]{elementName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (int[]) returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setPosition!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
        return new int[] {};
    }

    

    @Override
    public void deleteComponent(final String elementName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("deleteComponent",new Object[]{elementName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method deleteComponent!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public void deleteAllComponents(final String subcircuitName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("deleteAllComponents",new Object[]{subcircuitName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method deleteAllComponents!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void createComponent(final String elementType, final String elementName, final int xCoord, final int yCoord) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("createComponent",new Object[]{elementType,elementName,xCoord,yCoord});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method createComponent!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void setOutputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setOutputNodeName",new Object[]{elementName,nodeIndex,nodeName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setOutputNodeName!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void setInputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setInputNodeName",new Object[]{elementName,nodeIndex,nodeName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setInputNodeName!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public String getOutputNodeName(final String elementName, final int nodeIndex) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getOutputNodeName",new Object[]{elementName,nodeIndex});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String) returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setOutputNodeName!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
        return "";
    }

    @Override
    public String getInputNodeName(final String elementName, final int nodeIndex) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getInputNodeName",new Object[]{elementName,nodeIndex});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (String)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getInputNodeName!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
        return "";
    }

    @Override
    public double[] getTimeArray(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getTimeArray",new Object[]{signalName,tStart,tEnd,skipPoints});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (double[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getTimeArray!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public float[] getSignalData(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalData",new Object[]{signalName,tStart,tEnd,skipPoints});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (float[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalData!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
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
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setComponentName",new Object[]{oldName,newName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setComponentName!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void rotate(final String elementName) {
        assert elementName != null;
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("rotate",new Object[]{elementName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method rotate!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void setOrientation(final String elementName, final String direction) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setOrientation",new Object[]{elementName,direction});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setOrientation!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public void importFromFile(final String fileName, final String subCircuitName) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("importFromFile",new Object[]{fileName,subCircuitName});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method importFromFile!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public float[][] getGlobalFloatMatrix() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getGlobalFloatMatrix",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (float[][])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getGlobalFloatMatrix!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double[][] getGlobalDoubleMatrix() {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getGlobalDoubleMatrix",new Object[0]);
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (double[][])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getGlobalDoubleMatrix!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }    
    
    @Override
    public void setGlobalFloatMatrix(final float[][] matrix) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setGlobalFloatMatrix",new Object[]{matrix});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method seGlobalFloatMatrix!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public void setGlobalDoubleMatrix(final double[][] matrix) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("setGlobalDoubleMatrix",new Object[]{matrix});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method setGlobalDoubleMatrix!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalAvg(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalAvg",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalAvg!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalRMS(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalRMS",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalRMS!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalTHD(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalTHD",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalTHD!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalMin(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalMin",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalMin!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalMax(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalMax",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalMax!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalRipple(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalRipple",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalRipple!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalKlirr(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalKlirr",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalKlirr!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double getSignalShape(final String signalName, final double startTime, final double endTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalShape",new Object[]{signalName,startTime,endTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (Double)returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalShape!");
                }
                        
            } else {
                return Double.NaN;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public double[][] getSignalFourier(final String signalName, final double startTime,
            final double endTime, final int harmonics) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalFourier",new Object[]{signalName,startTime,endTime,harmonics});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (double[][])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalFourier!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }

    @Override
    public float[] floatFFT(final float[] timeDomainData) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("floatFFT",new Object[]{timeDomainData});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (float[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method floatFFT!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public void initSteadyStateDetection(String[] stateVariables, double frequency, double deltaT,
            double simulationTime) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("initSteadyStateDetection",new Object[]{stateVariables,frequency,deltaT,simulationTime});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isVoidReturn()) {
                    return;
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method initSteadyStateDetection!");
                }
                        
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
        
    @Override
    public double[] simulateToSteadyState(final boolean supressMessages) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("simulateToSteadyState",new Object[]{supressMessages});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (double[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method simulateToSteadyState!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    @Override
    public double[] simulateToSteadyState(final boolean supressMessages, final double targetCorrelation, final double targetMeanPctDiff) {
        try {
            if (checkRemote()) {
                final GeckoRemotePipeObject method = new GeckoRemotePipeObject("getSignalData",new Object[]{supressMessages,targetCorrelation,targetMeanPctDiff});
                final GeckoRemotePipeObject returnValue = _mmf.callMethod(sessionID, method);
                if (returnValue.isReturnValue()) {
                    return (double[])returnValue.getMethodReturnValue();
                } else if (returnValue.isErrorMessage()) {
                    throw new RuntimeException(returnValue.getErrorMessage());
                } else {
                    throw new RuntimeException("Invalid return value in method getSignalData!");
                }
                        
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ERROR_STRING + ex.getMessage(), ex);
        }
    }
    
    //unsupported methods from supertype
    
    @Override
    public void allowAdditionalClients(final int additionalClients) { 
        throw new UnsupportedOperationException("Not supported yet in GeckoRemoteMMFObject. For multiplce clients, use GeckoRemoteObject.");
    }
    
    
}
