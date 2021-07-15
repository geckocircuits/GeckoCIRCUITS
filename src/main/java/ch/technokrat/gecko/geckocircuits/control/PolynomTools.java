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

import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.math.NComplex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.DefaultListModel;

/**
 *
 * @author andy
 */
public final class PolynomTools {

    public static final int MAX_ARRAY_SIZE = 20;
    private static final String HTML_TAG = "<html>";
    private static final double SMALL_VALUE = 1e-40;
    public static final TechFormat TECH_FORMATTER = new TechFormat();

    private PolynomTools() {
        // private static - make this a utility-class  
    }

    ;
    
    
    public static String getPolynomString(double[] polynom) {
        List<Double> tmpList = new ArrayList<Double>();
        for (double value : polynom) {
            tmpList.add(value);
        }
        return getPolynomString(tmpList);
    }
    
    public static void main(String[] args) {
        List<Double> testPoly = Arrays.asList(-2.0, -2.0);
        System.out.println(getPolynomString(testPoly));
    }
    
    
    /*
     * return a html-String for the given polynom. Format: 1 + s - 2s^2 -> [1, 1, -2]
     *
     */
    public static String getPolynomString(final List<Double> polynom) {
        final StringBuffer returnValue = new StringBuffer(HTML_TAG);

        boolean firstFlag = true;
        for (int power = 0; power < polynom.size(); power++) {

            final Double value = polynom.get(power);
            final String niceFormValue = TECH_FORMATTER.formatENG(value, 3);
            // if nearly-zero power is detected, do not print anything
            if (Math.abs(polynom.get(power)) < SMALL_VALUE) {                
                continue;
            }

            if (firstFlag) {                
                if(value == -1) {
                    returnValue.append("-");
                } else if(value != 1){
                    returnValue.append(niceFormValue);
                }           
                if(power == 0 && Math.abs(value) == 1) {
                    returnValue.append('1');
                }
                firstFlag = false;
            } else {
                returnValue.append(printCoeffWithSign(value, niceFormValue));
            }
            returnValue.append(printSPower(power));
        }

        returnValue.append("</html>");
        return returnValue.toString();
    }

    /**
     *
     * @param value numeric value
     * @param niceFormValue nicely formatted value
     * @return 3 -> "+3" 1 -> "" -1 -> "-" -5 -> "-5"
     */
    private static String printCoeffWithSign(final double value, final String niceFormValue) {
        final StringBuffer returnValue = new StringBuffer();
        if (value > 0) {
            returnValue.append('+');
        }

        // if value == 0, don't plot anything, else return regular string
        if (Math.abs(value) > SMALL_VALUE) {
            if (Math.abs(Math.abs(value) - 1) > SMALL_VALUE) {
                returnValue.append(niceFormValue);
            } else { // if 1 or -1, just print the sign:
                if (value < 0) { // the positive sign was handled before!                   
                    returnValue.append('-');
                }
            }
        }

        return returnValue.toString();
    }

    /**
     * @param power power to print
     * @return return s^i in html format. If i == 0, empty string is returned.
     */
    private static String printSPower(final int power) {
        final StringBuffer returnValue = new StringBuffer();
        if (power > 0) {
            returnValue.append('s');
            if (power > 1) {
                returnValue.append("<sup>");
                returnValue.append(power);
                returnValue.append("</sup>");
            }
        }
        return returnValue.toString();
    }

    /**
     *
     * @param coefficients if poles or zeros are give, the factorized polynom will be returned
     * @param factor multiplication factor for the result
     * @return coefficients of a polynom: [1 0 3] == 1 + 3s^2
     */
    public static List<Double> evaluateFactorizedExpression(final List<NComplex> coefficients, final double factor) {
        double[] polynomReal = new double[MAX_ARRAY_SIZE];
        double[] polynomImag = new double[MAX_ARRAY_SIZE];
        final List<Double> returnValue = new ArrayList<Double>();

        if (coefficients.isEmpty()) {
            returnValue.add(factor);
            return returnValue;
        }

        polynomReal[0] = -coefficients.get(0).getRe();
        polynomImag[0] = -coefficients.get(0).getIm();
        polynomReal[1] = 1f;

        for (int i = 1; i < coefficients.size(); i++) {
            final double[] tmpReal = new double[MAX_ARRAY_SIZE];
            final double[] tmpImag = new double[MAX_ARRAY_SIZE];

            System.arraycopy(polynomReal, 0, tmpReal, 0, tmpReal.length);
            System.arraycopy(polynomImag, 0, tmpImag, 0, tmpImag.length);

            for (int k = tmpReal.length - 2; k >= 0; k--) {
                final double real = -tmpReal[k] * coefficients.get(i).getRe() + tmpImag[k] * coefficients.get(i).getIm();
                final double imag = -tmpImag[k] * coefficients.get(i).getRe() - tmpReal[k] * coefficients.get(i).getIm();
                polynomReal[k] = real;
                polynomImag[k] = imag;
                polynomReal[1 + k] = polynomReal[1 + k] + tmpReal[k];
                polynomImag[1 + k] = polynomImag[1 + k] + tmpReal[k];
            }
        }

        for (int k = 0; k < polynomReal.length; k++) {
            polynomReal[k] *= factor;
        }

        int maxIndex = MAX_ARRAY_SIZE - 1;
        while (polynomReal[maxIndex] < SMALL_VALUE) {
            maxIndex--;
        }

        for (int i = 0; i <= maxIndex; i++) {
            returnValue.add((double) polynomReal[i]);
        }
        return returnValue;
    }

    public static String plotFactorizedPolynoms(final List<NComplex> factorizedList) {
        final Map<NComplex, Integer> map = getPowersMap(factorizedList);
        final StringBuilder returnValue = new StringBuilder(HTML_TAG);


        int counter = 0;
        for (Entry<NComplex, Integer> entry : map.entrySet()) {
            if (counter > 0) {
                returnValue.append("&middot");
            }
            returnValue.append("(s");
            final NComplex value = new NComplex(-entry.getKey().getRe(), -entry.getKey().getIm());
            if (value.getRe() > 0) {
                returnValue.append('+');
            }

            if (value.getRe() == 0 && value.getIm() > 0) {
                returnValue.append('+');
            }

            returnValue.append(value.nicePrint());
            returnValue.append(')');
            if (entry.getValue() > 1) {
                returnValue.append("<sup>");
                returnValue.append(entry.getValue());
                returnValue.append("</sup>");
            }

            counter++;
        }

        if (counter == 0) {
            returnValue.append('1');
        }
        returnValue.append("</html>");
        return returnValue.toString();
    }

    /**
     *
     * @param polynomList list of polynome coefficient [1 2 0 4] == 1 + 2s + 4s^4
     * @return map between polynom powers and coefficients
     */
    private static Map<NComplex, Integer> getPowersMap(final List<NComplex> polynomList) {

        final Map<NComplex, Integer> nomPowers = new HashMap<NComplex, Integer>();
        for (int i = 0; i < polynomList.size(); i++) {
            int oldPower = 0;
            final NComplex value = polynomList.get(i);
            if (nomPowers.containsKey(value)) {
                oldPower = nomPowers.get(value);
            }
            nomPowers.put(value, oldPower + 1);
        }
        return nomPowers;
    }

    /**
     * extract a List of complex polynomial coefficient from a JList
     *
     * @param nomModel
     * @return
     */
    public static List<NComplex> getPolesOrZeros(final DefaultListModel nomModel) {
        final List<NComplex> zeros = new ArrayList<NComplex>();
        for (int i = 0; i < nomModel.getSize(); i++) {
            final NComplex value = ((ComplexPrinter) nomModel.getElementAt(i))._value;
            zeros.add(value);
            if (value.getIm() != 0) {
                zeros.add(new NComplex(value.getRe(), -value.getIm()));
            }
        }
        return zeros;
    }

    public static int getMaxPolynomialDegree(final double[] polynom) {
        int returnValue = -1;

        for (int i = 0; i < polynom.length; i++) {
            if (polynom[i] != 0) {
                returnValue = i;
            }
        }
        return returnValue;
    }

//    public static void main(String[] args) {
//        double [] num = new double[]{ 1, 1};
//        double [] den = new double[]{ 1, 2, 1};
//        double[] lead = new double[0];
//        double[] remainder = polynomialDivision(num, den, lead);
//        
//        System.out.println(PolynomTools.getPolynomString(remainder));
//    }
    /**
     * calculate the polynomial division between numerator and denominator.
     *
     * @param numerator [1,2,3]-> 1 + 2x + 3 x^2. The maximum degree value (3x^2) is not allowed to be zero
     * @param denominator [1,2,3]-> 1 + 2x + 3 x^2. The maximum degree value (3x^2) is not allowed to be zero
     * @param leading Polynomial part as RETURN value (given as argument, since the degree of the leading polynom is not a priori
     * known
     * @return the remainder of the division
     */
    public static double[] polynomialDivision(final double[] numerator, final double[] denominator,
            final double[] leading) {

        final double[] tmpStorage = new double[numerator.length];
        System.arraycopy(numerator, 0, tmpStorage, 0, tmpStorage.length);

        final int leadingIndex = numerator.length - denominator.length;
        final int numeratorIndex = numerator.length - 1;
        final int denominatorIndex = denominator.length - 1;

        assert numerator[numeratorIndex] != 0;
        assert denominator[denominatorIndex] != 0;
        assert Math.max(0, numerator.length - denominator.length + 1) == leading.length;

        for (int l = leadingIndex; l >= 0; l--) {
            leading[l] = tmpStorage[numeratorIndex - leadingIndex + l] / denominator[denominatorIndex];
            for (int i = 0; i < denominator.length; i++) {
                tmpStorage[i + l] -= leading[l] * denominator[i];
            }
        }

        final double[] remainder = new double[PolynomTools.getMaxPolynomialDegree(tmpStorage) + 1];
        System.arraycopy(tmpStorage, 0, remainder, 0, remainder.length);
        return remainder;
    }

    public static double[] getCorrectedPolynom(final double[] denomPolynom) {
        final int size = PolynomTools.getMaxPolynomialDegree(denomPolynom) + 1;
        final double[] returnValue = new double[size];
        System.arraycopy(denomPolynom, 0, returnValue, 0, returnValue.length);
        return returnValue;
    }
}
