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

public interface CallbackClientInterface 
  extends java.rmi.Remote{
  // This remote method is invoked by a callback
  // server to make a callback to an client which
  // implements this interface.
  // @param message - a string containing information for the
  //                  client to process upon being called back.

    void printSystemMessage(String message) throws java.rmi.RemoteException;           
    void printErrorMessage(String message) throws java.rmi.RemoteException;

    String ping() throws java.rmi.RemoteException;
}
