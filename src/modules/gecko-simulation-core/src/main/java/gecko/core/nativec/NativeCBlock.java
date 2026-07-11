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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Native C/C++ integration block for custom control algorithms.
 *
 * <p>Used to maintain the Classloader and call Native Functions. Provides
 * lifecycle management for native library loading, parameter initialization,
 * and output calculation at each simulation timestep.
 *
 * @author DIEHL Controls Ricardo Richter
 * @since Core Module Extraction Sprint - Phase 2
 */
@SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED",
        justification = "ClassLoader creation is intentional for native C code integration")
public class NativeCBlock {
    NativeCClassLoader _customCClassLoader;
    Class _nativeCWrapperClass;
    InterfaceNativeCWrapper _nativeCWrapperObj;
    private double[] _xINVector;
    private double[] _xOUTVector;

    public NativeCBlock () {
        _customCClassLoader = new NativeCClassLoader();
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidArrayLoops"})
    void calculateYOUT(final double time, final double deltaT, final double[][] inputSignals,
            final double[][] outputSignals) throws Exception {


        if ( _xINVector == null ) {
            _xINVector = new double[inputSignals.length];
        }

        if (_xOUTVector == null) {
            _xOUTVector = new double[outputSignals.length];
        }

        if (time == 0) {
            _nativeCWrapperObj.initParameters();
        }

        for (int i = 0; i < _xINVector.length; i++) {
            _xINVector[i] = inputSignals[i][0];
        }

        _nativeCWrapperObj.calcOutputs(_xINVector, _xOUTVector, outputSignals.length, time, deltaT);


        for (int i = 0; i < _xOUTVector.length; i++) {
            outputSignals[i][0] = _xOUTVector[i];
        }

        checkOutputsForNANorINFValues(outputSignals);
    }

    public boolean loadLibraries (final String name) {
        try {
            _customCClassLoader = new NativeCClassLoader();
            _nativeCWrapperClass = _customCClassLoader.findClass("gecko.core.nativec.NativeCWrapper");
            _nativeCWrapperObj = (InterfaceNativeCWrapper) _nativeCWrapperClass.newInstance();
            _nativeCWrapperObj.loadLibrary(name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return false;
        }
    }

    public void unloadLibraries () {
        try {
            /*ClassLoader clLoader = this.getClass().getClassLoader();
            Field field = ClassLoader.class.getDeclaredField("nativeLibraries");
            field.setAccessible(true);
            Vector libs = (Vector) field.get(clLoader);
            for (Object o : libs) {
                Method finalize = o.getClass().getDeclaredMethod("finalize", new Class[0]);
                finalize.setAccessible(true);
                finalize.invoke(o, new Object[0]);
            }*/
            _nativeCWrapperObj = null;
            _nativeCWrapperClass = null;
            _customCClassLoader = null;
            _xINVector = null;
            _xOUTVector = null;
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkOutputsForNANorINFValues(double[][] signal) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] != signal[i]) {
                throw new ArithmeticException("Output value yOUT[" + i + "] is not a number: " + signal[i]);
            }
        }
    }
}
