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

import java.lang.reflect.Method;

/**
 * pure utility class - no constructor. This utility function checks wheter the
 * methods of "checkMethods" are contained with the identical method signature
 * inside "containsMethodSignature".
 *
 * @author andy
 */
final class MethodNameChecker {

    private MethodNameChecker() {
        super();
    }

    static MethodNameChecker checkFabric(final Class checkMethods,
            final Class<GeckoRemoteInterface> containsMethodSignature) {
        try {
            assert false; // immediately return when assertions are turned off
            // we don't want to spend time in this check when users open GeckoCIRCUITS.
            return null;
        } catch (AssertionError err) { // we go here, when the JVM-flag "-ea" is set!
            for (Method toTest : checkMethods.getMethods()) {
                try {
                    assert containsMethodSignature.getMethod(toTest.getName(), toTest.getParameterTypes()) != null;                    
                } catch (Throwable ex) {
                    assert false : "Method in geckoRemoteInterface not found: " + toTest;
                }
            }
        }
        return null;
    }
}
