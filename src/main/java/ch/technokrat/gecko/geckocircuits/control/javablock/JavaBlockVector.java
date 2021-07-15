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
        _compiledInstance.init();
    }

    @Override
    public void findAndLoadClass() {
        try {
            _classNameFileMap = _compileObject.getClassNameFileMap();

            final ClassLoader classLoader = new JavaBlockClassLoader(_classNameFileMap);
            final Class clazz = Class.forName(_compileObject.getClassName(), false, classLoader);

            try {
                _compiledInstance = (ControlCalculatable) clazz.newInstance();
            } catch (NoClassDefFoundError err) {
                err.printStackTrace();
            } catch (InstantiationException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
}


