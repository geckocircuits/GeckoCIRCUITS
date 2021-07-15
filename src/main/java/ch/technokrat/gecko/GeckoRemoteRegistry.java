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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class GeckoRemoteRegistry {

    private static final String INTERFACE_NAME = "GeckoRemoteInterface";
    private static Registry _rmiRegistry = null;
    private static final int DEFAULT_ACCESSPORT = 43035;
    private static GeckoCustomRemote _remote = null;
    private static final String PROPERTIES_KEY = "REMOTE_ACCESS_PORT";
    private static GeckoCustomRemote remoteAccess;
    private static final String _ipQuerySite = "http://icanhazip.com/";
    private static String _ipAddress = "127.0.0.1";
    

    private GeckoRemoteRegistry() {
        // this is a "static" behavior class! -> private constructor
    }

    public static int getRemoteAccessPort() {
        GeckoSim.loadApplicationProperties();
        int returnValue = DEFAULT_ACCESSPORT;
            try {                
                if (GeckoSim.applicationProps.containsKey(PROPERTIES_KEY)) {                                     
                    String property = GeckoSim.applicationProps.getProperty(PROPERTIES_KEY);                    
                    returnValue = Integer.parseInt(property);                    
                } 
            } catch (Throwable error) {
                error.printStackTrace();
            }

        return returnValue;
    }

    /*
     * Set a new port number for the remote access. Be careful: when a connection already
     * exists, this will be "unbound" and opened then with the new port number.
     */
    public static void setRemoteAccessPort(final int newPortNumber) throws Exception {
        int oldPortNumber = getRemoteAccessPort();                
        
        if (isRemoteEnabled() && oldPortNumber != newPortNumber) {
            unbindExistingRegistry();
            GeckoSim.applicationProps.setProperty(PROPERTIES_KEY, ((Integer) newPortNumber).toString());    
            enableRemotePort();
        } else {
            GeckoSim.applicationProps.setProperty(PROPERTIES_KEY, ((Integer) newPortNumber).toString());                        
        }        
        if(oldPortNumber != newPortNumber) {                        
            GeckoSim.saveProperties();
        }        
    }
    
    public static String getIPAddress() {
        return _ipAddress;
        //add here the code for putting this in the properties file if necessary - although I wouldn't because the IP address
        //will probably vary from run to run of the application for most users (DHCP, etc.)
    }
    
    public static void setIPAddress(final String ip) {
        _ipAddress = ip;
    }

    public static boolean isRemoteEnabled() {
        return _remote != null;
    }

    public static void disableRemotePort() throws RemoteException {
        if (_rmiRegistry == null || _remote == null) {
            return; // nothing todo, already disabled
        }
        unbindExistingRegistry();
    }

    private static void unbindExistingRegistry() throws RemoteException {
        try {
            _remote.disconnectAll();
            _rmiRegistry.unbind(INTERFACE_NAME);
            UnicastRemoteObject.unexportObject(remoteAccess, true);
            System.out.println("GeckoCIRCUITS disconnect from client.");
            System.out.flush();
            System.err.flush();
            _remote = null;
        } catch (Exception e) {
            throw new RemoteException("Error disabling remote access.\n" + e.getMessage(), e);
        }
    }

    public static void enableRemotePort() throws Exception {
        if (isRemoteEnabled()) {
            return; // nothing todo - port already enabled!
        }
        try {            
            remoteAccess = new GeckoCustomRemote(Fenster._scripter);            
            
            GeckoRemoteInterface stub = (GeckoRemoteInterface) UnicastRemoteObject.exportObject(remoteAccess, 0);            
            System.setProperty("java.rmi.server.hostname",_ipAddress);
            //if (_rmiRegistry == null) {
            // Problem! This is a JAVA bug. Once a registry is opened, we cannot close it 
            // (see http://stackoverflow.com/questions/14265232/java-rmi-registry-port-change-issue).
            // properly anymore. Therefore, when we change the port, we just keep the
            // old ones, open, too. This is ugly. there is a workaround, http://stackoverflow.com/questions/1087163/closing-rmi-registry
            // but I dont't like this workaround, since it is platform dependent.
            // with my solution, at least the user can change to another port, and 
            // finally can change back to the original port...
            try {
                _rmiRegistry = LocateRegistry.getRegistry(getRemoteAccessPort());
                _rmiRegistry.rebind(INTERFACE_NAME, stub);            
            } catch(Throwable throwable) {
                _rmiRegistry = LocateRegistry.createRegistry(getRemoteAccessPort());
                _rmiRegistry.bind(INTERFACE_NAME, stub);            
            }
            
            _remote = remoteAccess;
            System.out.println("GeckoCIRCUITS configured for remote access at port " + GeckoRemoteRegistry.getRemoteAccessPort() + ".");
            System.out.println("Using IP address: " + _ipAddress);
        } catch (Exception ex) {
            throw new Exception("Error in enabling new remote port!\n" + ex.getMessage(), ex);
        }
    }
    
    /**Get IP numbers to choose from for REAL network access.
     * Returns the local machine IP addresses (of the machine's network interfaces) - not necessarily the "external" IP
     * if the machine is behind a NAT.
     * 
     * @return an array of local IP addresses, excluding the localhost (127.0.0.1) and inactive interfaces at the time the method is called.
     * @throws SocketException if something goes wrong and the IP addresses cannot be retrieved.
     */
    public static String[] getMachineIPNumbers() throws SocketException {
        final List<String> machineIPs = new ArrayList<String>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface interFace = interfaces.nextElement();
            //System.out.println("interface: " + interFace.getName() + " is loopback: " + interFace.isLoopback() + " is up: " + interFace.isUp());
            //filters out 127.0.0.1 and inactive interfaces
            if (interFace.isLoopback() /*|| !interFace.isUp()*/) {
                continue;
            }

            Enumeration<InetAddress> addresses = interFace.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                //System.out.println(interFace.getName() + ": " + address.getHostAddress());
                machineIPs.add(address.getHostAddress());
            }
        }
        final String[] localIPs = machineIPs.toArray(new String[0]);
        return localIPs;
    }
    
    /**
     * If the machine is behind a NAT of some sort (router, etc.) the above method does not return the IP address
     * via which the machine is reachable from the internet. This method queries a website to get this address.
     * 
     * @return the IP address as a String
     * @throws MalformedURLException if the URL of the website to be checked is invalid in some way
     * @throws IOException if the website to be checked cannot be read from properly
     */
    public static String getExternalIPAddress() throws MalformedURLException, IOException {
        //ask a site to get the IP seen on the internet
        URL getMyIP = new URL(_ipQuerySite);
        BufferedReader in = new BufferedReader(new InputStreamReader(getMyIP.openStream()));
        final String ip = in.readLine(); //the IP is the first line of the page
        return ip;
    }
    
    
}
