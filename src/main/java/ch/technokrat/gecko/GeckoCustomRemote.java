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
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This is an implementation of GeckoCustom, that is to be used for remote
 * method invocation with GeckoRemote.
 *
 * @author anstupar
 */
public final class GeckoCustomRemote extends AbstractGeckoCustom implements GeckoRemoteInterface, CallbackServerInterface {

    private boolean _free = true; //denotes if this instance of GeckoCIRCUITS is free for a remote connection
    private static long _lastSessionIDActive = 0;
    public static Map<Long,CallbackClientInterface> clients;
    
    private boolean _acceptsExtraConnections = false; //denotes if this instance of GeckoCIRCUITS allows more than one client to connect
    private int _numberOfExtraConnectionsAccepted = 0; //denotes how many additional clients (besides the first one) this instance of GeckoCIRCUITS will accept

    public GeckoCustomRemote(final SimulationAccess access) {
        super(access, null);
        clients = new HashMap<Long,CallbackClientInterface>();        
    }

    public static void printErrorLn(final String message) {
        if (clients != null && !clients.isEmpty()) {
            try {
                final CallbackClientInterface lastClient = clients.get(_lastSessionIDActive);
                if (lastClient != null) {
                    lastClient.printErrorMessage(message);
                }
            } catch (RemoteException ex) {
                Logger.getLogger(GeckoCustomRemote.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void printLn(final String message) {
        if (clients != null && !clients.isEmpty()) {
            try {
                final CallbackClientInterface lastClient = clients.get(_lastSessionIDActive);
                if (lastClient != null) {
                    lastClient.printSystemMessage(message);
                }
            } catch (RemoteException ex) {
                Logger.getLogger(GeckoCustomRemote.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void runScript() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isFree() {
        return _free;
    }

    @Override
    @Deprecated
    public long getSessionID() {
        return _lastSessionIDActive;
    }

    @Override
    public boolean checkSessionID(final long sessionID) {
        if (clients != null && !clients.isEmpty()) {
            return clients.containsKey(sessionID);
        } else {
            return false;
        }
    }
    
    @Override
    //here we generate a session ID and return it to the sender
    public long connect() {
        final long connectionID = System.currentTimeMillis();
        //add session ID to clients map with null value - method registerClientForCallback will place an object there
        clients.put(connectionID,null);
        _free = (_acceptsExtraConnections && clients.size() < (_numberOfExtraConnectionsAccepted+1));
        _lastSessionIDActive = connectionID;
        return connectionID;
    }

    @Override
    public void disconnect(final long remoteSessionID) {
        
        if (clients != null && !clients.isEmpty() && clients.containsKey(remoteSessionID)) {
            try {
                final CallbackClientInterface disconnectingClient = clients.get(remoteSessionID);
                if(disconnectingClient != null) {
                    disconnectingClient.printSystemMessage("GeckoREMOTE session closed.");                
                }                
            } catch (RemoteException ex) {
                Logger.getLogger(GeckoCustomRemote.class.getName()).log(Level.SEVERE, null, ex);
            }
            clients.remove(remoteSessionID);
            _free = (clients.isEmpty() || (_acceptsExtraConnections && clients.size() < (_numberOfExtraConnectionsAccepted+1))); 
        }
    }
    
    /**
     * Used when unbinding the registry.
     */
    public void disconnectAll() {
        if (clients != null && !clients.isEmpty()) {
            final Set<Entry<Long,CallbackClientInterface>> clientSet = clients.entrySet();
            for (Entry<Long,CallbackClientInterface> clientEntry : clientSet) {
                disconnect(clientEntry.getKey());
            }
        }
    }
    
    @Override
    public void registerForCallback(
            final CallbackClientInterface callbackClientObject)
            throws java.rmi.RemoteException {
        clients.put(_lastSessionIDActive, callbackClientObject);
    }

    
    @Override
    public void acceptExtraConnections(int numberOfExtraConnections) {
        _acceptsExtraConnections = (numberOfExtraConnections > 0);
        _numberOfExtraConnectionsAccepted = numberOfExtraConnections;
        _free = (clients.isEmpty() || (_acceptsExtraConnections && clients.size() < (_numberOfExtraConnectionsAccepted+1)));
    }
    
    @Override
    public boolean acceptsExtraConnections() {
        return _acceptsExtraConnections;
    }
    
    @Override
    public void registerLastClientToCallMethod(long sessionID) {
        if (clients.containsKey(sessionID)) {
            _lastSessionIDActive = sessionID;
        }            
    }
    
    public static String pingRemoteClient() {
        if(clients == null || clients.isEmpty()) {
            return null;
        }
        String pongs = null;
        Set<Entry<Long,CallbackClientInterface>> clientSet = clients.entrySet();
        CallbackClientInterface oneClient;
        String pong;
        for (Entry<Long,CallbackClientInterface> clientEntry : clientSet) {
            oneClient = clientEntry.getValue();
            if (oneClient != null) {
                try {
                    pong = oneClient.ping();
                    if (pongs == null) {
                        pongs = pong;
                    } else {
                        pongs = pongs.concat("\nAND\n" + pong);
                    }
                } catch (RemoteException ex) {
                    System.err.println(I18nKeys.CONNECTION_TEST_FAILED.getTranslation());
                }
            } else {
                return null;
            }
        }
        
        return pongs;
    }

    public static String getClientInfo() {
        if (clients == null || clients.isEmpty()) {
            return "\n  " + I18nKeys.NO_CONNECTION_ESTABLISHED.getTranslation();
        } else {
            final StringBuilder returnValue = new StringBuilder();
            String pong = pingRemoteClient();
            if (pong == null) {
                returnValue.append(I18nKeys.ERROR_CLIENT_IS_REGISTERED_BUT_CANNOT_BE_CONTACTED.getTranslation());
                Set<Entry<Long,CallbackClientInterface>> clientSet = clients.entrySet();
                CallbackClientInterface oneClient;
                for (Entry<Long,CallbackClientInterface> clientEntry : clientSet) {
                    oneClient = clientEntry.getValue();
                    if (oneClient != null) {
                        returnValue.append("\n");
                        returnValue.append(oneClient.toString());
                    }
                }
            } else {
                returnValue.append(I18nKeys.STATUS_OF_CONNECTION_OK.getTranslation() + "\n");
                returnValue.append(pong);
            }
            return returnValue.toString();
        }
    }
}
