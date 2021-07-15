/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * This testing class is a bit "different" from normal ones. First, we check if GeckoRemote contains ALL methods 
 * of the interface GeckoRemoteInterface with the same method name, but a "static" modifier.
 * Then, we exercise the GeckoRemote-Interface. In fact, I just check if the correct exception is thrown 
 * (UndefinedOperationException) form a dummy proxy wrapper.
 * 
 * @author andy
 */
public class GeckoRemoteTest {

    private static GeckoRemoteInterface _oldWrapped;
    private static final List<String> _staticMethodNamesToExclude = Arrays.asList("forceDisconnectFromGecko", "disconnectFromGecko", "shutdown",
            "connectToGecko", "startGui", "simulateStep", "simulateSteps");
    private static final List<String> _methodNamesToExclude = Arrays.asList("connect", "disconnect", "getSessionID", "isFree", "shutdown",
            "simulateStep", "simulateSteps");

   

    @Test
    @Ignore
    public void testCallAllGeckoRemoteStaticMethods() {
        final GeckoRemoteTestingDummy testingDummy = new GeckoRemoteTestingDummy();   
        
        InvocationHandler handler = null;
        try {
            handler = new GeckoRemote.RemoteInvocationHandler(testingDummy);
        } catch (Throwable ex) {
            Logger.getLogger(GeckoRemoteTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ClassLoader loader = testingDummy.getClass().getClassLoader();
        Class[] interfaces = new Class[]{GeckoRemoteIntWithoutExc.class};

        GeckoRemote._proxy = (GeckoRemoteIntWithoutExc) Proxy.newProxyInstance(loader, interfaces, handler);
        GeckoRemote.doProxyCheck = false;
        for (Method meth : getAllRelevantStaticMethods()) {
            Class<?>[] parameterTypes = meth.getParameterTypes();
            Object[] dummyParameters = new Object[parameterTypes.length];
            Class currentClass = null;            
            try {
                for (int i = 0; i < dummyParameters.length; i++) {
                    currentClass = parameterTypes[i];
                    if(currentClass.equals(String.class)) {
                        dummyParameters[i] = "abc";
                    } else if(currentClass.equals(int.class)) {
                        dummyParameters[i] = 1;
                    } else if(currentClass.equals(double.class)) {
                        dummyParameters[i] = 1;
                    } else if(currentClass.equals(float.class)) {
                        dummyParameters[i] = 1;
                    } else if(currentClass.equals(boolean.class)) {
                        dummyParameters[i] = true;
                    } else if(currentClass.equals(double[].class)) {
                        dummyParameters[i] = new double[]{1.0};
                    } else if(currentClass.equals(int[].class)) {
                        dummyParameters[i] = new int[]{1};
                    } else if(currentClass.equals(float[].class)) {
                        dummyParameters[i] = new float[]{1f};
                    } else if(currentClass.equals(float[][].class)) {
                        dummyParameters[i] = new float[0][0];
                    } else if(currentClass.equals(double[][].class)) {
                        dummyParameters[i] = new double[0][0];
                    } else if(currentClass.equals(String[].class)) {
                        dummyParameters[i] = new String[]{"1"};
                    } else if(currentClass.equals(Object.class)) {
                        dummyParameters[i] = new Object();
                    } else {
                        dummyParameters[i] = currentClass.getConstructor().newInstance((Object) null);
                    }
                    
                }
                
                try {
                    meth.invoke(null, dummyParameters);
                } catch (Throwable ex) {
                    //ex.printStackTrace();
                    assertTrue(ex.getCause() instanceof UnsupportedOperationException);                    
                }          
            } catch (Exception ex) {                                
                System.err.println("current class " + currentClass);
                Logger.getLogger(GeckoRemoteTest.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue("Unexpected exception in calling method " + meth.getName() + " exception type is " + ex, false);                    
            }            
        }
    }

    @Test
    @Ignore
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
        //System.out.println(" found " + foundCounter + " not found " + notFoundCounter);
    }

    @Test
    @Ignore
    public void testMethodsAvailableStaticToNonStatic() {

        int foundCounter = 0;
        int notFoundCounter = 0;
        for (Method meth : getAllRelevantStaticMethods()) {
            final String name = meth.getName();

            try {
                final Method found = GeckoRemoteInterface.class.getMethod(name, meth.getParameterTypes());
                assertTrue(found.getReturnType().equals(meth.getReturnType()));
                foundCounter++;
            } catch (Exception ex) {
                assertFalse("Error: method " + meth + " was not found in GeckoRemoteInterface", true);
                notFoundCounter++;
                System.err.println("not found " + meth);
            }
        }
        assert foundCounter > 70;
        //System.out.println(" found " + foundCounter + " not found " + notFoundCounter);
    }

    private List<Method> getAllRelevantStaticMethods() {
        final List<Method> staticMethods = new ArrayList<Method>();
        for (Method staticSearch : GeckoRemote.class.getMethods()) {
            if (Modifier.isStatic(staticSearch.getModifiers())) {
                if (!_staticMethodNamesToExclude.contains(staticSearch.getName())) {
                    staticMethods.add(staticSearch);
                }

            }
        }
        return staticMethods;
    }
}