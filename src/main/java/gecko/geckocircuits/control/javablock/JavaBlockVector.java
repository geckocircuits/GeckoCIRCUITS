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
package gecko.geckocircuits.control.javablock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.ControlCalculatable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
public class JavaBlockVector extends AbstractJavaBlock {
    private static final Logger LOGGER = LogManager.getLogger(JavaBlockVector.class);

    private ControlCalculatable _compiledInstance;
    private double[] _xINVector;

    JavaBlockVector(final ControlJavaFunction control) {
        super(control);
    }

    @Override
    AbstractJavaBlock createOtherBlockTypeCopy() {
        final AbstractJavaBlock returnValue = new JavaBlockMatrix(_controlJavaBlock);
        createNewJavaSourceCopy(returnValue);
        returnValue._additionalSourceFiles.addAll(this._additionalSourceFiles);
        return returnValue;
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidArrayLoops"})
    @Override
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
            if (Double.isNaN(ausgangssignal[i][0])) {
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
    @SuppressWarnings("PMD.CloseResource") // ClassLoader must persist for dynamically loaded class lifecycle
    public void findAndLoadClass() {
        LOGGER.info("JavaBlockVector.findAndLoadClass() - Loading compiled class...");
        LOGGER.info("Compilation status: " + _compileObject.getCompileStatus());
        LOGGER.info("Compiler message: " + _compileObject.getCompilerMessage());
        LOGGER.info("Class name: " + _compileObject.getClassName());

        if (_compileObject.getCompileStatus() != CompileStatus.COMPILED_SUCCESSFULL) {
            LOGGER.error("ERROR: Compilation was not successful! Status: " + _compileObject.getCompileStatus());
            return;
        }

        try {
            _classNameFileMap = _compileObject.getClassNameFileMap();

            final ClassLoader classLoader = new JavaBlockClassLoader(_classNameFileMap);
            final Class<?> clazz = Class.forName(_compileObject.getClassName(), false, classLoader);
            LOGGER.info("Class loaded successfully: " + clazz.getName());

            try {
                _compiledInstance = (ControlCalculatable) clazz.newInstance();
                LOGGER.info("Instance created successfully: " + _compiledInstance.getClass().getName());
            } catch (NoClassDefFoundError err) {
                LOGGER.error("ERROR: NoClassDefFoundError while loading Java block: " + err.getMessage());LogManager.getLogger(ControlJavaFunction.class).error("NoClassDefFoundError while loading Java block: " + err.getMessage(), err);
            } catch (InstantiationException ex) {
                LOGGER.error("ERROR: InstantiationException while creating Java block instance: " + ex.getMessage());LogManager.getLogger(ControlJavaFunction.class).error("InstantiationException while creating Java block instance: " + ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                LOGGER.error("ERROR: IllegalAccessException while creating Java block instance: " + ex.getMessage());LogManager.getLogger(ControlJavaFunction.class).error("IllegalAccessException while creating Java block instance: " + ex.getMessage(), ex);
            } catch (SecurityException ex) {
                LOGGER.error("ERROR: SecurityException while creating Java block instance: " + ex.getMessage());LogManager.getLogger(ControlJavaFunction.class).error("SecurityException while creating Java block instance: " + ex.getMessage(), ex);
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.error("ERROR: ClassNotFoundException while loading Java block class: " + ex.getMessage());LogManager.getLogger(ControlJavaFunction.class).error("ClassNotFoundException while loading Java block class: " + ex.getMessage(), ex);
        }
    }


}


