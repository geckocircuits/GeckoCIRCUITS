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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.math.Matrix;
import java.util.Arrays;

/**
 * This class solves a dynamic system, where the transfer function is given via a numerator and denominator polynom. The
 * implementation uses a trapezoidal integration rule. The numerator degree is allowed to be larger by the degree 2, w.r.t. the
 * denominator degree.
 *
 * @author andy
 */
public final class StateSpaceCalculator {

    private static final int MAX_DEGREE_DIFF = 3;
    /**
     * the "real" polynomial part, after doing the polynomial division, i.e. the leading polynomial without any fraction
     * (denominator)
     */
    private final double[] _leadingPolynom;
    /**
     * the numerator polynomial after the polynomial division
     */
    private final double[] _transferNum;
    /**
     * denominator polynomial, as given from the gui
     */
    private final double[] _denomPolynom;
    /**
     * the denominator polynom length minus 1.
     */
    private final int _denominDegree;
    private Matrix _AMatrix;
    private Matrix _AMatrix2;
    private Matrix _rhsVec;
    private StateVariables _stateVariables;
    private final double _deltaT;
    
    static class StateVariables {

        /**
         * these three history values are used for the derivative calculations, when a numerator degree is bigger than the
         * denominator degree
         */
        double _xOLDOLD;
        double _xOLD;
        double _xNEW;
        /**
         * the simulation result (saved for the next trapezoidal step
         */
        Matrix _result;
        /**
         * parentHash is used to determine if this state is appropriate for the parent stateSpaceCalcualtor.
         */
        final int _parentHash;

        StateVariables(final double initialXIn, final int degree, int parentHash) {
            _xNEW = initialXIn;
            _xOLD = _xNEW; // at the first step, I set the history values identical, since otherwise we will
            _xOLDOLD = _xOLD;  // see a glitch at the simulation start!
            _result = new Matrix(degree, 1);
            _parentHash = parentHash;
        }

        StateVariables(final TokenMap tokenMap) {

            _parentHash = tokenMap.readDataLine("parentHash", -1);
            _xNEW = tokenMap.readDataLine("stateXNEW", _xNEW);
            _xOLD = tokenMap.readDataLine("stateXOLD", _xOLD);
            _xOLDOLD = tokenMap.readDataLine("stateXOLDOLD", _xOLDOLD);
            final double[] resultVector = tokenMap.readDataLine("stateResultVector", new double[0]);
            _result = new Matrix(resultVector.length, 1);
            for (int i = 0; i < resultVector.length; i++) {
                _result.set(i, 0, resultVector[i]);
            }
        }
                

        private void saveHistory(final double newXIn) {
            _xOLDOLD = _xOLD;
            _xOLD = _xNEW;
            _xNEW = newXIn;
        }

        void exportIndividualCONTROL(final StringBuffer ascii) {
            ascii.append("\nstateXOLD ");
            ascii.append(_xOLD);

            ascii.append("\nstateXOLDOLD ");
            ascii.append(_xOLDOLD);

            ascii.append("\nparentHash ");
            ascii.append(_parentHash);


            ascii.append("\nstateXNEW ");
            ascii.append(_xNEW);

            ascii.append("\nstateResultVector ");
            for (int i = 0; i < _result.getRowDimension(); i++) {
                ascii.append(_result.get(i, 0));
                ascii.append(' ');
            }
        }
    }

    StateSpaceCalculator(final double deltaT, final double[] numeratorPolynom, final double[] denomPolynom) {
        _denomPolynom = PolynomTools.getCorrectedPolynom(denomPolynom);
        double[] tmpNumPolynom = PolynomTools.getCorrectedPolynom(numeratorPolynom);
        _deltaT = deltaT;
        /**
         * normalize the fraction, so that the highest denominator degree is 1!
         */
        final double maxDenomCoef = _denomPolynom[_denomPolynom.length - 1];
        for (int i = 0; i < _denomPolynom.length; i++) {
            _denomPolynom[i] /= maxDenomCoef;
        }

        for (int i = 0; i < tmpNumPolynom.length; i++) {
            tmpNumPolynom[i] /= maxDenomCoef;
        }


        if (tmpNumPolynom.length - 2 > _denomPolynom.length) {
            throw new ArrayIndexOutOfBoundsException("\nNumerator degree = " + (tmpNumPolynom.length - 1)
                    + " is not allowed to be larger\nthan Denominator degree "
                    + (tmpNumPolynom.length - 1) + " + 2 = " + (_denomPolynom.length + 2));
        }

        final int leadingSize = Math.max(0, tmpNumPolynom.length - _denomPolynom.length + 1);
        _leadingPolynom = new double[leadingSize];
        _transferNum = PolynomTools.polynomialDivision(tmpNumPolynom, _denomPolynom, _leadingPolynom);

        _denominDegree = _denomPolynom.length - 1;
        calculateMatrixA(deltaT);
        _rhsVec = new Matrix(_denominDegree, 1);
    }

    void setInitialState(final StateVariables stateVariables) {
        if (stateVariables._parentHash == this.hashCode()) {
            _stateVariables = stateVariables;
        }
    }
    
    void initializeWithNewDt(final double deltaT) {
         double deltaTOld = _deltaT;
         calculateMatrixA(deltaT);
            
         //TODO: for differentiation (using _stateVariables._xOLD and _stateVariables._xNEW
         // the stepwidth-change is not yet implemented correctly!         
    }

    StateVariables getStateVariables() {
        return _stateVariables;
    }

    public void calculateMatrixA(final double deltaT) {
        if (_denominDegree == 0) {
            return;
        }

        final double[][] aMatrix1 = new double[_denominDegree][_denominDegree];
        final double[][] aMatrix2 = new double[_denominDegree][_denominDegree];

        for (int i = 0; i < _denominDegree; i++) {
            aMatrix1[0][i] = -deltaT * _denomPolynom[_denominDegree - i - 1] / 2;
            aMatrix2[0][i] = deltaT * _denomPolynom[_denominDegree - i - 1] / 2;
        }

        for (int i = 0; i < _denominDegree - 1; i++) {
            aMatrix1[i + 1][i] = deltaT / 2;
            aMatrix2[i + 1][i] = -deltaT / 2;
        }

        for (int i = 0; i < _denominDegree; i++) {
            aMatrix2[i][i] += 1;
        }

        _AMatrix = new Matrix(aMatrix1);
        _AMatrix2 = new Matrix(aMatrix2);
    }

    public void calculateTimeStep(final double[][] xIN, final double deltaT, final double[][] outputSignal, final double time) {

        if (_stateVariables == null) {
            _stateVariables = new StateVariables(xIN[0][0], _denominDegree, this.hashCode());
            _rhsVec = new Matrix(_denominDegree, 1);
        }

        _stateVariables.saveHistory(xIN[0][0]);


        outputSignal[0][0] = 0;

        if (_AMatrix != null) {  
            _rhsVec = _AMatrix.times(_stateVariables._result);
            for (int i = 0; i < _denominDegree; i++) {
                _rhsVec.set(i, 0, _rhsVec.get(i, 0) + _stateVariables._result.get(i, 0));
            }

            _rhsVec.set(0, 0, _rhsVec.get(0, 0) + deltaT * xIN[0][0]);            
            _stateVariables._result = _AMatrix2.solve(_rhsVec);
        }
        double resultValue = 0;
        /**
         * weighting of the result value by the nominator polynom
         */
        for (int i = 0; i < _transferNum.length; i++) {
            resultValue += _transferNum[i] * _stateVariables._result.get(_denominDegree - i - 1, 0);
        }


        // Add the (higher) derivative values, if leading polynom is existent.
        switch (_leadingPolynom.length) {
            case MAX_DEGREE_DIFF:
                resultValue += _leadingPolynom[2] * (_stateVariables._xNEW
                        - 2 * _stateVariables._xOLD + _stateVariables._xOLDOLD) / deltaT; // second derivative
            case 2:
                double toAdd = _leadingPolynom[1] * (_stateVariables._xNEW - _stateVariables._xOLD) / deltaT; // first derivative                
                resultValue += toAdd;
            case 1:
                resultValue += xIN[0][0] * _leadingPolynom[0]; // proportional part
            case 0:
                break;
            default:
                assert false;
        }

        outputSignal[0][0] = resultValue;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (int i = 0; i < _denomPolynom.length; i++) {
            hashCode += i + (new Double(_denomPolynom[i])).hashCode();
        }

        for (int i = 0; i < _leadingPolynom.length; i++) {
            hashCode += i + (new Double(_leadingPolynom[i])).hashCode();
        }

        for (int i = 0; i < _transferNum.length; i++) {
            hashCode += i + (new Double(_transferNum[i])).hashCode();
        }
        hashCode += new Double(_deltaT).hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StateSpaceCalculator other = (StateSpaceCalculator) obj;
        if (!Arrays.equals(this._leadingPolynom, other._leadingPolynom)) {
            return false;
        }
        if (!Arrays.equals(this._transferNum, other._transferNum)) {
            return false;
        }
        if (!Arrays.equals(this._denomPolynom, other._denomPolynom)) {
            return false;
        }
        if (Double.doubleToLongBits(this._deltaT) != Double.doubleToLongBits(other._deltaT)) {
            return false;
        }
        return true;
    }
}
