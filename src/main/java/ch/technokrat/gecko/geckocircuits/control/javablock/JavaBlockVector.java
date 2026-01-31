/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control.javablock;

import ch.technokrat.gecko.ControlCalculatable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaBlockVector extends AbstractJavaBlock {
    private ControlCalculatable _compiledInstance;
    private double[] _xINVector;
    
    JavaBlockVector(final ReglerJavaFunction regler) {
        super(regler);
    }

    @Override
    AbstractJavaBlock createOtherBlockTypeCopy() {
        final AbstractJavaBlock returnValue = new JavaBlockMatrix(_reglerJavaBlock);
        createNewJavaSourceCopy(returnValue);
        returnValue._additionalSourceFiles.addAll(this._additionalSourceFiles);
        return returnValue;
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidArrayLoops"})
    void calculateYOUT(final double time, final double deltaT, final double[][] inputSignals,
            final double[][] outputSignals) throws Exception {
        if (_compiledInstance == null) {
            throw new IllegalStateException("Java block compilation failed - cannot simulate. Check error logs for details.");
        }

        for (int i = 0; i < _xINVector.length; i++) {
            _xINVector[i] = inputSignals[i][0];
        }


        final double[] outValue = _compiledInstance.calculateYOUT(_xINVector, time, deltaT);

        final int outLength = Math.min(outputSignals.length, outValue.length);

        for (int i = 0; i < outLength; i++) {
            outputSignals[i][0] = outValue[i];
        }
        //ausgangssignal = (double[]) _externYOUT.invoke(null, new Object[]{xIN, t, dt});

        checkOutputsForNANorINFValues(outputSignals);
    }
    
    private void checkOutputsForNANorINFValues(double[][] ausgangssignal) {
        for (int i = 0; i < ausgangssignal.length; i++) {
            if (ausgangssignal[i] != ausgangssignal[i]) {
                throw new ArithmeticException("Output value yOUT[" + i + "] is not a number: " + ausgangssignal[i]);
            }
        }
    }

    @Override
    void initialize(double[][] inputSignals, double[][] outputSignals) throws Exception {
        _xINVector = new double[inputSignals.length];
        super.initialize(inputSignals, outputSignals); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    @Override
    protected void doInitialize(double[][] xIN, double[][] yOUT) {
        if (_compiledInstance == null) {
            throw new IllegalStateException("Java block compilation failed - cannot initialize. Check error logs for details.");
        }
        _compiledInstance.init();
    }

    @Override
    @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED",
            justification = "ClassLoader creation is intentional for dynamic class loading in scripting code")
    public void findAndLoadClass() {
        System.out.println("JavaBlockVector.findAndLoadClass() - Loading compiled class...");
        System.out.println("Compilation status: " + _compileObject.getCompileStatus());
        System.out.println("Compiler message: " + _compileObject.getCompilerMessage());
        System.out.println("Class name: " + _compileObject.getClassName());

        if (_compileObject.getCompileStatus() != CompileStatus.COMPILED_SUCCESSFULL) {
            System.err.println("ERROR: Compilation was not successful! Status: " + _compileObject.getCompileStatus());
            return;
        }

        try {
            _classNameFileMap = _compileObject.getClassNameFileMap();

            final ClassLoader classLoader = new JavaBlockClassLoader(_classNameFileMap);
            final Class<?> clazz = Class.forName(_compileObject.getClassName(), false, classLoader);
            System.out.println("Class loaded successfully: " + clazz.getName());

            try {
                _compiledInstance = (ControlCalculatable) clazz.newInstance();
                System.out.println("Instance created successfully: " + _compiledInstance.getClass().getName());
            } catch (NoClassDefFoundError err) {
                System.err.println("ERROR: NoClassDefFoundError while loading Java block: " + err.getMessage());
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "NoClassDefFoundError while loading Java block: " + err.getMessage(), err);
            } catch (InstantiationException ex) {
                System.err.println("ERROR: InstantiationException while creating Java block instance: " + ex.getMessage());
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "InstantiationException while creating Java block instance: " + ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                System.err.println("ERROR: IllegalAccessException while creating Java block instance: " + ex.getMessage());
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "IllegalAccessException while creating Java block instance: " + ex.getMessage(), ex);
            } catch (SecurityException ex) {
                System.err.println("ERROR: SecurityException while creating Java block instance: " + ex.getMessage());
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "SecurityException while creating Java block instance: " + ex.getMessage(), ex);
            }
        } catch (ClassNotFoundException ex) {
            System.err.println("ERROR: ClassNotFoundException while loading Java block class: " + ex.getMessage());
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "ClassNotFoundException while loading Java block class: " + ex.getMessage(), ex);
        }
    }

    
}


