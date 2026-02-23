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

package gecko.core.nativec;

/**
 * Interface for native C/C++ library integration.
 *
 * <p>This interface provides prototype functions for native code integration.
 * The functions init() and calcOutputs(...) are intended to be overwritten
 * by native functions. Using the interface makes the method call easier.
 *
 * @author DIEHL Controls Ricardo Richter
 * @since Core Module Extraction Sprint - Phase 2
 */
public interface InterfaceNativeCWrapper {

    void loadLibrary (String name);

    void initParameters ();

    void calcOutputs (double[] xINVector, double[] xOUTVector, int numberOfOuts, double time, double deltaT);
}
