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

import java.io.Serializable;

/**
 * This class contains the description of a GeckoRemote i.e. AbstractGeckoCustom method being called. It is used for the alternative,
 * memory-mapped remote interface for GeckoCIRCUITS. Inside this object all the relevant information is packed, and then the object
 * is serialized and written to a memory-mapped file.
 * @author andrija s.
 */
public class GeckoRemotePipeObject implements Serializable {
    
    //internal enum which defines which type of action this objects represents - a method call, a return variable, successful void return, or an error message
    public static enum GeckoRemotePipeObjectType { METHOD_CALL, METHOD_RETURN_VALUE, METHOD_VOID_RETURN, ERROR_MESSAGE };
    
    private final GeckoRemotePipeObjectType _type; //the type of action this object represents
    private final String _methodName; //the name of the method - must be set for all types (method calls and returns and errors)
    private final Object[] _methodArguments; //method arguments for method call - null in other cases
    private final Object _methodReturnValue; //the return value of the method - null if void or if not a return action
    private final String _errorMessage; //the error message - empty string if none required
    
    /**
     * Constructor to create an object representing a method call.
     * @param methodName the name of the method
     * @param methodArguments the arguments of the method, IN THE ORDER they appear in the method definition!
     */
    public GeckoRemotePipeObject(final String methodName, final Object[] methodArguments) {
        _type = GeckoRemotePipeObjectType.METHOD_CALL;
        _methodName = methodName;
        _methodArguments = methodArguments;
        _methodReturnValue = null;
        _errorMessage = "";
    }
    
    /**
     * Constructor to create an object representing a method return value.
     * @param methodName the name of the method
     * @param returnValue the return value (object) of the method
     */
    public GeckoRemotePipeObject(final String methodName, final Object returnValue) {
        _type = GeckoRemotePipeObjectType.METHOD_RETURN_VALUE;
        _methodName = methodName;
        _methodArguments = null;
        _methodReturnValue = returnValue;
        _errorMessage = "";
    }
    
    /**
     * Constructor to create an object representing a successful void return from a method.
     * @param methodName the name of the method
     */
    public GeckoRemotePipeObject(final String methodName) {
        _type = GeckoRemotePipeObjectType.METHOD_VOID_RETURN;
        _methodName = methodName;
        _methodArguments = null;
        _methodReturnValue = null;
        _errorMessage = "";
    }
    
    /**
     * Constructor to create an object representing an error message (e.g. exception thrown).
     * @param methodName the name of the method
     * @param errorMessage the error message to transmit
     */
    public GeckoRemotePipeObject(final String methodName, final String errorMessage) {
        _type = GeckoRemotePipeObjectType.ERROR_MESSAGE;
        _methodName = methodName;
        _methodArguments = null;
        _methodReturnValue = null;
        _errorMessage = errorMessage;
    }
    
    /**
     * Check if this object represents a method call.
     * @return true if it is a method call
     */
    public boolean isMethodCall() {
        return _type == GeckoRemotePipeObjectType.METHOD_CALL;
    }
    
    /**
     * Check if this object represents a successful void return of a method.
     * @return true if it is a successful void return of a method
     */
    public boolean isVoidReturn() {
        return _type == GeckoRemotePipeObjectType.METHOD_VOID_RETURN;
    }
    
    /**
     * Check if this object represents a successful return value of a method.
     * @return true if it is a return value
     */
    public boolean isReturnValue() {
        return _type == GeckoRemotePipeObjectType.METHOD_RETURN_VALUE;
    }
    
    /**
     * Check if this object represents an error message.
     * @return true if it is an error message
     */
    public boolean isErrorMessage() {
        return _type == GeckoRemotePipeObjectType.ERROR_MESSAGE;
    }
    
    /**
     * Check the name of this method.
     * @return the name of the method associated with this object
     */
    public String getMethodName() {
        return _methodName;
    }
    
    /**
     * Get the arguments of this method.
     * @return the arguments of the method associated with this object
     */
    public Object[] getMethodArguments() {
        return _methodArguments;
    }
    
    /**
     * Get the return value of this method.
     * @return the return value of the method associated with this object
     */
    public Object getMethodReturnValue() {
        return _methodReturnValue;
    }
    
    /**
     * Get the error message.
     * @return the error message contained within this object
     */
    public String getErrorMessage() {
        return _errorMessage;
    }
}
