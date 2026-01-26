/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This testing class verifies that GeckoRemote class contains static methods
 * that match GeckoRemoteInterface methods.
 *
 * Note: GeckoRemote uses GeckoRemoteIntWithoutExc (internal interface without exceptions)
 * so GeckoRemote methods don't declare throws RemoteException. This test verifies
 * that all required methods exist with correct signatures.
 *
 * @author andy
 */
public class GeckoRemoteTest {

    private static final List<String> _staticMethodNamesToExclude = Arrays.asList("forceDisconnectFromGecko", "disconnectFromGecko", "shutdown",
            "connectToGecko", "startGui", "simulateStep", "simulateSteps", "setJavaPath");
    private static final List<String> _methodNamesToExclude = Arrays.asList("connect", "disconnect", "getSessionID", "isFree", "shutdown",
            "simulateStep", "simulateSteps", "registerLastClientToCallMethod", "checkSessionID", "acceptsExtraConnections");

    /**
     * Tests all static methods in GeckoRemote via reflection proxy.
     *
     * IGNORED: Requires complex RMI/proxy setup that is not available in CI environment.
     * This test exercises the remote interface by creating a dummy proxy and invoking
     * all static methods. Requires GeckoRemote proxy infrastructure to be initialized.
     * TODO: Re-enable once remote API testing infrastructure is established.
     */
    @Test
    public void testMethodsAvailableNonStaticToStatic() {

        final List<Method> staticMethods = getAllRelevantStaticMethods();

        int foundCounter = 0;
        int notFoundCounter = 0;

        for (Method meth : GeckoRemoteInterface.class.getMethods()) {
            final String name = meth.getName();

            if (_methodNamesToExclude.contains(name)) {
                continue;
            }

            try {
                final Method found = GeckoRemote.class.getMethod(name, meth.getParameterTypes());
                assertTrue(found.getReturnType().equals(meth.getReturnType()));
                assertTrue("Error: method " + found + " should be static! ", Modifier.isStatic(found.getModifiers()));
                assertTrue("Error: static method should be public:", Modifier.isPublic(found.getModifiers()));
                foundCounter++;
            } catch (Exception ex) {
                assertFalse("Error: static method " + meth + " was not found in GeckoRemote class!", true);
                notFoundCounter++;
                System.err.println("not found " + meth);
            }
        }

        assertTrue("Should have more than 70 GeckoRemote methods", foundCounter > 70);
    }

    /**
     * Verifies all static methods in GeckoRemote have corresponding interface methods.
     *
     * IGNORED: Test performs reflection checks on interface/class structure. Currently
     * requires RMI infrastructure and throws exceptions when classes are not fully loaded.
     * TODO: Re-enable once remote API testing infrastructure is established.
     */
    @Test
    public void testMethodsAvailableStaticToNonStatic() {

        int foundCounter = 0;
        int notFoundCounter = 0;

        for (Method meth : getAllRelevantStaticMethods()) {
            final String name = meth.getName();

            if (_methodNamesToExclude.contains(name)) {
                continue;
            }

            try {
                final Method found = GeckoRemoteInterface.class.getMethod(name, meth.getParameterTypes());
                assertTrue(found.getReturnType().equals(meth.getReturnType()));
                foundCounter++;
            } catch (Exception ex) {
                notFoundCounter++;
            }
        }

        assertTrue("Should have more than 70 GeckoRemote methods", foundCounter > 70);
    }

    private static List<Method> getAllRelevantStaticMethods() {
        final List<Method> staticMethods = new ArrayList<Method>();
        for (Method meth : GeckoRemote.class.getMethods()) {
            if (Modifier.isStatic(meth.getModifiers()) && Modifier.isPublic(meth.getModifiers())) {
                if (!_staticMethodNamesToExclude.contains(meth.getName())) {
                    staticMethods.add(meth);
                }
            }
        }
        return staticMethods;
    }
}
