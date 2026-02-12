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

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the remote interface CallbackClientInterface, which we use for
 * propagating System.out and System.err messages to Matlab.
 *
 *
 */
public final class CallbackClientImpl extends UnicastRemoteObject implements CallbackClientInterface {

    private final String _clientHostname;
    private final String _clientUserID;
    private final String _connectionDate;
    
    public CallbackClientImpl() throws RemoteException {
        super();

        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (java.net.UnknownHostException ex) {
            Logger.getLogger(CallbackClientImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(addr == null) {
            _clientHostname = "unknown";            
        } else {
            _clientHostname = addr.getHostName();
        }
        
        _clientUserID = System.getProperty("user.name");
        final GregorianCalendar now = new GregorianCalendar();
        final Date date = now.getTime();        
        _connectionDate = date.toString();
    }
        

    @Override
    public void printSystemMessage(final String message) {
        System.out.println(message);        
    }

    @Override
    public void printErrorMessage(final String message) {
        System.err.println(message);
    }
    

    @Override
    public String ping() {          
        final StringBuffer returnValue = new StringBuffer(0x64);
        returnValue.append("User: ");
        returnValue.append(_clientUserID);
        returnValue.append("\nHostname: ");
        returnValue.append(_clientHostname);
        returnValue.append("\nConnection date: ");
        returnValue.append(_connectionDate);
        return returnValue.toString();
    }
    
    
}
