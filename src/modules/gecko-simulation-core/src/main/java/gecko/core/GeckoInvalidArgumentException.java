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
package gecko.core;

/**
 * Exception thrown when invalid arguments are provided to GeckoCIRCUITS methods.
 *
 * <p>Used throughout the core simulation engine for parameter validation and
 * argument checking in signal analysis, scripting, and simulation methods.
 *
 * @author andreas
 * @since Core Module Extraction Sprint - Phase 3
 */
public class GeckoInvalidArgumentException extends Exception {

    public GeckoInvalidArgumentException(final String message) {
        super(message);
    }

}
