/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.api;

/**
 * Exception thrown when simulator access operations fail.
 *
 * This exception wraps underlying transport-specific exceptions
 * (e.g., RemoteException for RMI, IOException for MMF) to provide
 * a unified error handling mechanism.
 *
 * @author GeckoCIRCUITS Team
 */
public class SimulatorAccessException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new SimulatorAccessException with a message.
     *
     * @param message the error message
     */
    public SimulatorAccessException(String message) {
        super(message);
    }

    /**
     * Creates a new SimulatorAccessException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public SimulatorAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new SimulatorAccessException with a cause.
     *
     * @param cause the underlying cause
     */
    public SimulatorAccessException(Throwable cause) {
        super(cause);
    }
}
