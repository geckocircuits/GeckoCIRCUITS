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

import gecko.geckocircuits.circuit.TokenMap;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Immutable class, which holds the java control block source code. Imutability makes comparisons easier for equality. Builder
 * pattern is used for object construction, see Joshua Bloch's book "effective java".
 *
 * @author andreas
 */
public final class JavaBlockSource {
    // fields contain source code, that is also saved in the JAVA-object .ipes stuff
    // since it is final, we don't need the private modifier

    final String _sourceCode;
    final String _importsCode;
    final String _initCode;
    final String _variablesCode;

    public static final class Builder {

        private String _sourceCode = "return yOUT;";
        private String _importsCode = "";
        private String _initCode = "";
        private String _variablesCode = "";

        public Builder sourceCode(final String sourceCode) {
            _sourceCode = sourceCode;
            return this;
        }

        public Builder importsCode(final String importsCode) {
            _importsCode = importsCode;
            return this;
        }

        public Builder initCode(final String initCode) {
            _initCode = initCode;
            return this;
        }

        public Builder variablesCode(final String variablesCode) {
            _variablesCode = variablesCode;
            return this;
        }
        

        JavaBlockSource build() {
            return new JavaBlockSource(this);
        }
    }

    private JavaBlockSource(final Builder builder) {
        _sourceCode = builder._sourceCode;
        _importsCode = builder._importsCode;
        _initCode = builder._initCode;
        _variablesCode = builder._variablesCode;
    }

    public JavaBlockSource(final TokenMap tokenMap) {
        _sourceCode = tokenMap.createSubBlock("<sourceCode>", "<\\sourceCode>");
        _initCode = tokenMap.createSubBlock("<staticCode>", "<\\staticCode>");
        _importsCode = tokenMap.createSubBlock("<importCode>", "<\\importCode>");
        _variablesCode = tokenMap.createSubBlock("<staticVariables>", "<\\staticVariables>");
    }

    private Iterable<String> getAllCodeBlocks() {
        return Arrays.asList(_sourceCode, _importsCode, _initCode, _variablesCode);
    }

    @SuppressWarnings("PMD.ConsecutiveLiteralAppends") // this makes the code easier to read
    public void exportIndividualCONTROL(final StringBuffer ascii) {
        ascii.append("\n<sourceCode>\n");
        ascii.append(_sourceCode);
        ascii.append("\n<\\sourceCode>");
        ascii.append("\n<staticCode>\n");
        ascii.append(_initCode);
        ascii.append("\n<\\staticCode>");
        ascii.append("\n<importCode>\n");
        ascii.append(_importsCode);
        ascii.append("\n<\\importCode>");
        ascii.append("\n<staticVariables>\n");
        ascii.append(_variablesCode);
        ascii.append("\n<\\staticVariables>");
    }        

    

    @Override
    public int hashCode() {
        int returnValue = 0;
        for (String codeBlock : getAllCodeBlocks()) {
            returnValue += codeBlock.hashCode();
        }
        return returnValue;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JavaBlockSource other = (JavaBlockSource) obj;

        final Iterable<String> otherBlocks = other.getAllCodeBlocks();
        final Iterable<String> thisBlocks = getAllCodeBlocks();
        for (final Iterator<String> it1 = otherBlocks.iterator(), it2 = thisBlocks.iterator();
                it1.hasNext() && it2.hasNext(); it1.next(), it2.next()) {
            if (!it1.equals(it2)) {
                return false;
            }
        }

        return true;
    }
}
