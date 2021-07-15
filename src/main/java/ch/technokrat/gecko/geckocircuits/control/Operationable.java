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

import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.List;

public interface Operationable {

    List<OperationInterface> getOperationEnumInterfaces();

    public abstract class OperationInterface {

        final String _operationName;
        private final I18nKeys _documentation;

        public OperationInterface(final String operationName, final I18nKeys documentation) {
            assert operationName != null;
            assert !operationName.isEmpty();
            _operationName = operationName;
            _documentation = documentation;
        }

        public abstract Object doOperation(final Object parameterValue);

        public String getDocumentationString() {
            return _documentation.getTranslation();
        }

        public static OperationInterface fabricFromString(final String operationName, final Operationable parent) {
            List<OperationInterface> allPossibleOperations = parent.getOperationEnumInterfaces();
            final StringBuilder listOfValidOperations = new StringBuilder();
            
            
            for (OperationInterface testOperation : allPossibleOperations) {
                if (testOperation._operationName.equalsIgnoreCase(operationName)) {
                    return testOperation;
                } else {
                    listOfValidOperations.append("\t" + testOperation._operationName + "\n");
                }
            }
            throw new IllegalArgumentException("Error: tried to execute " + operationName + ". Permitted operations are: \n"
                    + listOfValidOperations.toString());
        }

        @Override
        public String toString() {
            return _operationName;
        }
        
        
    }
}
