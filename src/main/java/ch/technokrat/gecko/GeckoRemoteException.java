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
 * An exception for when something goes wrong in the GeckoRemoteObject class.
 * @author anstupar
 */
public class GeckoRemoteException extends Exception {

    /**
     * Creates a new instance of
     * <code>GeckoRemoteObjectException</code> without detail message.
     */
    public GeckoRemoteException() {
    }

    /**
     * Constructs an instance of
     * <code>GeckoRemoteObjectException</code> with the specified detail
     * message.
     *
     * @param msg the detail message.
     */
    public GeckoRemoteException(String msg) {
        super(msg);
    }
    
    /**
     * Create a new GeckoRemoteException which contains a reference to the exception (e.g. IOException, or other) which cause the 
     * GeckoRemote exception.
     * @param message the message of the GeckoRemoteException
     * @param preecedingException the exception which caused a GeckoRemoteException to be thrown
     */
    public GeckoRemoteException(final String message, final Throwable preecedingException) {
        super(message,preecedingException);
    }
}
