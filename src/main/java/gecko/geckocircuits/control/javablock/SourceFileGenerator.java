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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class (only static methods) which constructs the javablock
 * sourcecode, when import code, class source code, ... is given.
 * createSourceCode returns the String for the .java-file!
 *
 * @author andreas
 */
public final class SourceFileGenerator {

    private static final String DOUBLE_TAB = "\t\t";
    private static final int MIN_BUFFER_SIZE = 64;

    private SourceFileGenerator() {
        // pure utility class, therefore private constructor!
    }

    /**
     *
     * @return a 2^n value, probably bigger than the total source code length.
     */
    private static int estimateTotalStringSize(final JavaBlockSource javaBlockSource) {
        final int tmpSize = javaBlockSource._importsCode.length() + javaBlockSource._sourceCode.length()
                + javaBlockSource._initCode.length() + javaBlockSource._variablesCode.length();
        int expectedTotalSize = MIN_BUFFER_SIZE;
        while (expectedTotalSize < tmpSize) {
            expectedTotalSize *= 2;
        }
        return expectedTotalSize;
    }

    public static String createSourceCode(final JavaBlockSource source, final String className,
            final int outTerminalNumber, final VariableBusWidth variableBusWidth) {
        try {

            final StringBuilder sourceStringBuilder = new StringBuilder(estimateTotalStringSize(source));

            appendClassImportsAndHeader(sourceStringBuilder, source._importsCode, className, 
                    variableBusWidth._useMatrix.getValue());
            appendVariablesCode(sourceStringBuilder, source._variablesCode, outTerminalNumber, 
                    variableBusWidth);
            appendInitCode(sourceStringBuilder, source._initCode);
            appendFunctionSourceCode(sourceStringBuilder, source._sourceCode, variableBusWidth._useMatrix.getValue());
            appendClassFooter(sourceStringBuilder);
            return sourceStringBuilder.toString();
        } catch (IOException ex) {
            Logger.getLogger(JavaBlockSource.class.getName()).log(Level.SEVERE, null, ex);
        }
        assert false;
        return null;
    }

    private static void appendClassImportsAndHeader(final StringBuilder sourceStringBuilder,
            final String importsCode, final String className, final boolean isMatrix) throws IOException {

        final BufferedReader reader = new BufferedReader(new StringReader(importsCode));
        for (String strLine = reader.readLine(); strLine != null; strLine = reader.readLine()) {
            sourceStringBuilder.append(strLine);
            sourceStringBuilder.append("\n");
        }
        sourceStringBuilder.append("\n");
        sourceStringBuilder.append("public class ");
        sourceStringBuilder.append(className);
        if (isMatrix) {
            sourceStringBuilder.append(" implements java.io.Serializable, gecko.ControlCalculatableMatrix { \n");
        } else {
            sourceStringBuilder.append(" implements java.io.Serializable, gecko.ControlCalculatable { \n");
        }

    }

    private static void appendVariablesCode(final StringBuilder sourceStringBuilder,
            final String variablesCode, final int outTerminalNumber, 
            final VariableBusWidth variableBusWidth) throws IOException {
        sourceStringBuilder.append("// variables: \n");
        final BufferedReader reader = new BufferedReader(new StringReader(variablesCode));

        for (String strLine = reader.readLine(); strLine != null; strLine = reader.readLine()) {
            sourceStringBuilder.append("\t");
            sourceStringBuilder.append(strLine);
            sourceStringBuilder.append("\n");
        }

        if (variableBusWidth._useMatrix.getValue()) {
            if (variableBusWidth._fixedOutputBusEnabled.getValue()) {
                sourceStringBuilder.append("\tprivate final double[][] yOUT = new double[");
                sourceStringBuilder.append(outTerminalNumber);
                sourceStringBuilder.append("][" + variableBusWidth._fixedOutputBusWidth.getValue() + "];\n");

            } else {                
                sourceStringBuilder.append("\tprivate final double[][] yOUT = new double[][]");
                sourceStringBuilder.append("{ ");
                for(int i = 0; i < outTerminalNumber; i++) {
                    sourceStringBuilder.append("new double[" + variableBusWidth.getBusWidth(i) + "]");
                    if(i < outTerminalNumber-1) {
                        sourceStringBuilder.append(", ");
                    }
                }
                sourceStringBuilder.append(" };");
                
            }
            sourceStringBuilder.append("\n\t@Override");
                sourceStringBuilder.append("\n\tpublic double[][] getOutputSignal() {\n"
                        + "\t\treturn yOUT;\n"
                        + "\t}");                

        } else {
            sourceStringBuilder.append("\tprivate double[] yOUT = new double[");
            sourceStringBuilder.append(outTerminalNumber);
            sourceStringBuilder.append("];\n");
        }
    }

    private static void appendInitCode(final StringBuilder sourceStringBuilder, final String initCode) throws IOException {
        sourceStringBuilder.append("\n\t@Override\n");
        sourceStringBuilder.append("\tpublic void init() {\n");
        final BufferedReader reader = new BufferedReader(new StringReader(initCode));
        for (String strLine = reader.readLine(); strLine != null; strLine = reader.readLine()) {
            sourceStringBuilder.append(DOUBLE_TAB);
            sourceStringBuilder.append(strLine);
            sourceStringBuilder.append("\n");
        }
        sourceStringBuilder.append("\t");
    }

    private static void appendFunctionSourceCode(final StringBuilder sourceStringBuilder,
            final String sourceCode, final boolean isMatrix) throws IOException {
        sourceStringBuilder.append("}\n\n    @Override\n");

        if (isMatrix) {
            sourceStringBuilder.append("    public double[][] calculateYOUT(final double[][] xIN, final double time, "
                    + "final double dt) throws Exception {\n");

        } else {
            sourceStringBuilder.append("    public double[] calculateYOUT(final double[] xIN, final double time, "
                    + "final double dt) throws Exception {\n");

        }
        sourceStringBuilder.append("// ****************** your code segment **********************\n");
        final BufferedReader reader = new BufferedReader(new StringReader(sourceCode));

        for (String strLine = reader.readLine(); strLine != null; strLine = reader.readLine()) {
            sourceStringBuilder.append("\t\t");
            sourceStringBuilder.append(strLine);
            sourceStringBuilder.append("\n");
        }

        sourceStringBuilder.append("// ****************** end of code segment **********************\n");
    }

    private static void appendClassFooter(final StringBuilder sourceStringBuilder) {
        sourceStringBuilder.append("    }\n");
        sourceStringBuilder.append("}\n");

    }
}
