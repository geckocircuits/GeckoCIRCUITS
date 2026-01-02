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
import ch.technokrat.gecko.ControlCalculatableMatrix;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaBlockMatrix extends AbstractJavaBlock {
    private ControlCalculatableMatrix _compiledInstance;
    JavaBlockMatrix(final ReglerJavaFunction regler) {
        super(regler);
    }

    @Override
    AbstractJavaBlock createOtherBlockTypeCopy() {
        final AbstractJavaBlock returnValue = new JavaBlockVector(_reglerJavaBlock);
        returnValue._javaBlockSource = this._javaBlockSource;
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

        final double[][] outValue = _compiledInstance.calculateYOUT(inputSignals, time, deltaT);
        checkOutputsForNANorINFValues(outputSignals);
    }
    
    public double[][] getOutputVectorFromBlock() {
        if (_compiledInstance == null) {
            throw new IllegalStateException("Java block compilation failed - cannot get output. Check error logs for details.");
        }
        return _compiledInstance.getOutputSignal();
    }

    private void checkOutputsForNANorINFValues(double[][] ausgangssignal) {
        for (int i = 0; i < ausgangssignal.length; i++) {
            if (ausgangssignal[i] != ausgangssignal[i]) {
                throw new ArithmeticException("Output value yOUT[" + i + "] is not a number: " + ausgangssignal[i]);
            }
        }
    }

    @Override
    protected void doInitialize(double[][] xIN, double[][] yOUT) {
        if (_compiledInstance == null) {
            throw new IllegalStateException("Java block compilation failed - cannot initialize. Check error logs for details.");
        }
        _compiledInstance.init();
    }

    @Override
    public void findAndLoadClass() {
        try {
            _classNameFileMap = _compileObject.getClassNameFileMap();

            final ClassLoader classLoader = new JavaBlockClassLoader(_classNameFileMap);
            final Class<?> clazz = Class.forName(_compileObject.getClassName(), false, classLoader);

            try {
                _compiledInstance = (ControlCalculatableMatrix) clazz.newInstance();

            } catch (NoClassDefFoundError err) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "NoClassDefFoundError while loading Java block: " + err.getMessage(), err);
            } catch (InstantiationException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "InstantiationException while creating Java block instance: " + ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "IllegalAccessException while creating Java block instance: " + ex.getMessage(), ex);
            } catch (SecurityException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "SecurityException while creating Java block instance: " + ex.getMessage(), ex);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "ClassNotFoundException while loading Java block class: " + ex.getMessage(), ex);
        }
    }

}


