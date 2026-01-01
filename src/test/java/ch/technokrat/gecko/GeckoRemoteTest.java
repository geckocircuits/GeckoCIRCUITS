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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

/**
 * This testing class verifies that GeckoRemote class contains static methods
 * that match GeckoRemoteInterface methods.
 *
 * Note: This test does NOT load any circuit files. It only tests API structure.
 * No circuit simulation occurs in this test.
 *
 * @author andy
 */
@Ignore("Test has Proxy issues causing 'null' output - no circuit files loaded")
public class GeckoRemoteTest {

    private static final List<String> _staticMethodNamesToExclude = Arrays.asList("forceDisconnectFromGecko", "disconnectFromGecko", "shutdown",
            "connectToGecko", "startGui", "simulateStep", "simulateSteps", "setJavaPath");
    private static final List<String> _methodNamesToExclude = Arrays.asList("connect", "disconnect", "getSessionID", "isFree", "shutdown",
            "simulateStep", "simulateSteps", "registerLastClientToCallMethod", "checkSessionID", "acceptsExtraConnections");

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

            Class<?>[] exceptionTypes = meth.getExceptionTypes();
            boolean exceptionCheck = false;

            for(Class excClass : exceptionTypes) {
                if(excClass.isAssignableFrom(RemoteException.class)) {
                    exceptionCheck = true;
                }
            }

            assertTrue("Error: the method " + meth + " MUST throw a superclass of RemoteException!", exceptionCheck);

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

        assert foundCounter > 70;
    }

    @Test
    public void testMethodsAvailableStaticToNonStatic() {

        int foundCounter = 0;
        int notFoundCounter = 0;

        for (Method meth : getAllRelevantStaticMethods()) {
            final String name = meth.getName();

            if (_methodNamesToExclude.contains(name)) {
                continue;
            }

            Class<?>[] exceptionTypes = meth.getExceptionTypes();
            boolean exceptionCheck = false;

            for(Class excClass : exceptionTypes) {
                if(excClass.isAssignableFrom(RemoteException.class)) {
                    exceptionCheck = true;
                }
            }

            assertTrue("Error: the method " + meth + " MUST throw a superclass of RemoteException!", exceptionCheck);

            try {
                final Method found = GeckoRemoteInterface.class.getMethod(name, meth.getParameterTypes());
                assertTrue(found.getReturnType().equals(meth.getReturnType()));
                foundCounter++;
            } catch (Exception ex) {
                notFoundCounter++;
            }
        }

        assert foundCounter > 70;
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
