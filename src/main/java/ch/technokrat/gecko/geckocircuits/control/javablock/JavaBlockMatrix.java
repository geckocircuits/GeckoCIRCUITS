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

        final double[][] outValue = _compiledInstance.calculateYOUT(inputSignals, time, deltaT);
        checkOutputsForNANorINFValues(outputSignals);
    }
    
    public double[][] getOutputVectorFromBlock() {
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
        _compiledInstance.init();
    }

    @Override
    public void findAndLoadClass() {
        try {
            _classNameFileMap = _compileObject.getClassNameFileMap();

            final ClassLoader classLoader = new JavaBlockClassLoader(_classNameFileMap);
            final Class clazz = Class.forName(_compileObject.getClassName(), false, classLoader);

            try {
                _compiledInstance = (ControlCalculatableMatrix) clazz.newInstance();
                
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


