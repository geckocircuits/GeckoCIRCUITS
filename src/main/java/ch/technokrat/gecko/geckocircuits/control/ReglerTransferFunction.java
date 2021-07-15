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

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.control.calculators.InitializableAtSimulationStart;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.StateSpaceCalculator.StateVariables;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ReglerTransferFunction extends AbstractReglerSingleInputSingleOutput
        implements Operationable {

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerTransferFunction.class, "TF", I18nKeys.TRANSFER_FUNKTION_H_S, I18nKeys.DEFINES_A_TRANSFER_FUNCTION);
    private double[] _zeros = new double[MAX_ARRAY_SIZE];
    private double[] _poles = new double[MAX_ARRAY_SIZE];
    private double[] _numeratorPolynom = new double[ReglerTransferFunction.MAX_ARRAY_SIZE];
    private double[] _denomPolynom = new double[ReglerTransferFunction.MAX_ARRAY_SIZE];
    private StateSpaceCalculator _stateSpaceCalc;
    public static final int MAX_ARRAY_SIZE = 20;
    private StateVariables _savedState;

    public ReglerTransferFunction() {
        super();

        _numeratorPolynom[0] = 1;
        _denomPolynom[0] = 1;
        _zeros[0] = 1;
        _poles[0] = 2;
    }
    final UserParameter<Boolean> _inPolynomMode = UserParameter.Builder.
            <Boolean>start("polynomMode", false).
            longName(I18nKeys.IF_TRUE_THEN_POLYNOM).
            shortName("usePolynoms").
            arrayIndex(this, -1).
            build();
    final UserParameter<Boolean> _useInitialState = UserParameter.Builder.
            <Boolean>start("useInitState", false).
            longName(I18nKeys.IF_TRUE_THEN_INITIAL).
            arrayIndex(this, -1).
            shortName("useInitState").
            build();
    final UserParameter<Double> _constantFactor = UserParameter.Builder.
            <Double>start("constant", 1.0).
            longName(I18nKeys.GAIN).
            shortName("gain").
            arrayIndex(this, -1).
            build();

    @Override
    public String[] getOutputNames() {
        return new String[]{"y"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.TRANSFER_FUNCTION_OUTPUT_SIGNAL};
    }

    private class TransferFunctionCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart, IsDtChangeSensitive {

        public TransferFunctionCalculator() {
            super(1, 1);
        }

        @Override
        public void initializeAtSimulationStart(final double deltaT) {
            _stateSpaceCalc = new StateSpaceCalculator(deltaT, _numeratorPolynom, _denomPolynom);
            if (_useInitialState.getValue() && _savedState != null) {
                _stateSpaceCalc.setInitialState(_savedState);
            }
        }

        @Override
        public void berechneYOUT(final double deltaT) {
            _stateSpaceCalc.calculateTimeStep(_inputSignal, deltaT, _outputSignal, _time);
        }

        @Override
        public void initWithNewDt(final double dt) {
            _stateSpaceCalc.initializeWithNewDt(dt);
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new TransferFunctionCalculator();
    }

    @Override
    protected String getCenteredDrawString() {
        return "H(s)";
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        ascii.append("\nnominatorPoles ");
        for (int i = 0; i < _poles.length; i++) {
            ascii.append(_poles[i]);
            ascii.append(' ');
        }

        ascii.append("\ndenominatorZeros ");
        for (int i = 0; i < _zeros.length; i++) {
            ascii.append(_zeros[i]);
            ascii.append(' ');
        }

        ascii.append("\nnominatorPolynom ");
        for (int i = 0; i < _numeratorPolynom.length; i++) {
            ascii.append(_numeratorPolynom[i]);
            ascii.append(' ');
        }

        ascii.append("\ndenominatorPolynom ");
        for (int i = 0; i < _denomPolynom.length; i++) {
            ascii.append(_denomPolynom[i]);
            ascii.append(' ');
        }

        if (_savedState != null) {
            _savedState.exportIndividualCONTROL(ascii);
        }

    }

    public double[] getPoles() {
        final double[] returnValue = new double[_poles.length];
        System.arraycopy(_poles, 0, returnValue, 0, _poles.length);
        return returnValue;
    }

    public void setPole(final double value, final int index) {
        _poles[index] = value;
    }

    public double[] getZeros() {
        final double[] returnValue = new double[_zeros.length];
        System.arraycopy(_zeros, 0, returnValue, 0, _zeros.length);
        return returnValue;
    }

    public void setZero(final double value, final int index) {
        _zeros[index] = value;
    }

    public void setNumeratorPolynom(final List<Double> values) {
        for (int i = 0; i < _numeratorPolynom.length; i++) {
            _numeratorPolynom[i] = 0;
        }

        for (int i = 0; i < Math.min(values.size(), _numeratorPolynom.length); i++) {
            _numeratorPolynom[i] = values.get(i);           
        }
    }

    public void setDeNominatorPolynom(final List<Double> values) {
        for (int i = 0; i < _denomPolynom.length; i++) {
            _denomPolynom[i] = 0;
        }

        for (int i = 0; i < Math.min(values.size(), _denomPolynom.length); i++) {
            _denomPolynom[i] = values.get(i);
        }
    }

    public double getNumeratorCoefficient(final int index) {
        return _numeratorPolynom[index];
    }

    public int getNumeratorSize() {
        return PolynomTools.getMaxPolynomialDegree(_numeratorPolynom) + 1;
    }

    public double getDenominatorCoefficients(final int index) {
        return _denomPolynom[index];
    }

    public int getDenominatorSize() {
        return PolynomTools.getMaxPolynomialDegree(_denomPolynom) + 1;
    }

    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);
        final ReglerTransferFunction other = (ReglerTransferFunction) originalBlock;

        this._poles = new double[other._poles.length];
        System.arraycopy(other._poles, 0, this._poles, 0, _poles.length);

        this._zeros = new double[other._zeros.length];
        System.arraycopy(other._zeros, 0, this._zeros, 0, _zeros.length);

        this._numeratorPolynom = new double[_numeratorPolynom.length];
        System.arraycopy(other._numeratorPolynom, 0, this._numeratorPolynom, 0, _numeratorPolynom.length);

        this._denomPolynom = new double[_denomPolynom.length];
        System.arraycopy(other._denomPolynom, 0, this._denomPolynom, 0, _denomPolynom.length);

        this._savedState = other._savedState;

    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {

        _poles = tokenMap.readDataLine("nominatorPoles", _poles);
        _zeros = tokenMap.readDataLine("denominatorZeros", _zeros);
        _numeratorPolynom = tokenMap.readDataLine("nominatorPolynom", _numeratorPolynom);
        _denomPolynom = tokenMap.readDataLine("denominatorPolynom", _denomPolynom);


        try {
            _savedState = new StateVariables(tokenMap);
        } catch (Throwable ex) {
            System.err.println("could not read transfer function initial state.");
        }
    }

    public void clearPolesAndZeros() {
        for (int i = 0; i < _poles.length; i++) {
            _poles[i] = 0;
            setZero(0, i);
        }

        for (int i = 0; i < _denomPolynom.length; i++) {
            _denomPolynom[i] = 0;
            _numeratorPolynom[i] = 0;
        }
    }

    void saveState() {
        _savedState = _stateSpaceCalc.getStateVariables();
    }

    @Override
    protected final Window openDialogWindow() {        
        return new DialogTransferFunction((ReglerTransferFunction) this, this);        
        
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {
        final List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.add(new OperationInterface("setNumeratorPolynom", I18nKeys.SET_NUMERATOR_POLYNOM) {
            @Override
            public Object doOperation(final Object parameterValue) {
                final Double[] parameterArray = checkParameterType(parameterValue);
                setNumeratorPolynom(Arrays.asList(parameterArray));
                return null;
            }
        });


        returnValue.add(new OperationInterface("setDenominatorPolynom", I18nKeys.SET_DENOMINATOR_POLYNOM) {
            @Override
            public Object doOperation(final Object parameterValue) {                
                final Double[] parameterArray = checkParameterType(parameterValue);
                setDeNominatorPolynom(Arrays.asList(parameterArray));
                return null;
            }
        });

        returnValue.add(new OperationInterface("setPoles", I18nKeys.SET_POLE_COEFF) {
            @Override
            public Object doOperation(final Object parameterValue) {
                Double[] parameters = checkParameterType(parameterValue);
                for(int i = 0; i < parameters.length; i++) {
                    setPole(parameters[i], i);
                }
                for(int i = parameters.length; i < MAX_ARRAY_SIZE; i++) {
                    _poles[i] = 0;
                }
                return null;
            }
        });

        returnValue.add(new OperationInterface("setZeros", I18nKeys.SET_ZERO_COEFF) {
            @Override
            public Object doOperation(final Object parameterValue) {
                Double[] parameters = checkParameterType(parameterValue);
                for(int i = 0; i < parameters.length; i++) {
                    setZero(parameters[i], i);
                }
                for(int i = parameters.length; i < MAX_ARRAY_SIZE; i++) {
                    _zeros[i] = 0;
                }
                return null;            }
        });


        return Collections.unmodifiableList(returnValue);
    }

    private static Double[] checkParameterType(Object parameterValue) {

        if (parameterValue instanceof double[]) {
            double[] orig = (double[]) parameterValue;
            Double[] returnValue = new Double[orig.length];
            for (int i = 0; i < orig.length; i++) {
                returnValue[i] = orig[i];
            }
            return returnValue;
        }
        if (parameterValue instanceof Double[]) {
            return (Double[]) parameterValue;
        }

        throw new IllegalArgumentException("Error: parameter must be a double[] array, but is"
                + " of type " + parameterValue.getClass());
    }
}
