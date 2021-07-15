/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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

//package ch.technokrat.expressionscripting;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.regex.Pattern;
//import javax.script.Compilable;
//import javax.script.CompiledScript;
//import javax.script.ScriptException;
//
//class VariableExpression extends AbstractExpression {
//    
//    private CompiledScript _script;
//    private final Compilable _compileable;
//    final String _evaluationString;
//    private boolean _thisExpressionHasNoVariableReferences = true;
//    public static final Map<String, Double> variablesMappedToNumbers = new LinkedHashMap<String, Double>();
//    
//    private static final Pattern FIND_DOLLAR_REGEXP = Pattern.compile(
//            "\\$     # word boundary\n"
//            + "[A-Za-z]# 1 ASCII letter\n"
//            + "[\\w\\.]*    # 0+ alnums\n"
//            + "\\b     # word boundary\n"
//            + "(?!     # Lookahead assertion: Make sure there is no...\n"
//            + " \\s*   # optional whitespace\n"
//            + " \\(    # opening parenthesis\n"
//            + ")       # ...at this position in the string",
//            Pattern.COMMENTS);
//
//    VariableExpression(final Object nameable, final String expression) {
//        super(nameable, expression);
//        
//        _compileable = (Compilable) JavaScriptTest.engine;
//
//        final String withString = " with(Math)  { " + expression + " }";
//        _evaluationString = replaceVariablesWithFunctionCall(withString);
//        if (!withString.equals(_evaluationString)) {
//            _thisExpressionHasNoVariableReferences = false;
//        }
//    }
//
//    private static String replaceVariablesWithFunctionCall(final String evaluationString) {
//        return FIND_DOLLAR_REGEXP.matcher(evaluationString).replaceAll("callBack.invoke(\"$0\")");
//    }
//
//    @Override
//    public Double evaluate() throws ScriptException {
//        try {
//            if (_script == null) {
//                _script = _compileable.compile(_evaluationString);
//            }
//
//            Object doubleValue = _script.eval();
//            Double returnValue = (Double) doubleValue;
//            variablesMappedToNumbers.put(_nameable.toString(), returnValue);
//            return returnValue;
//        } catch (final ScriptException ex) {                              
//            throw new ScriptException("Error in evaluation of expression: " + VariableExpression.this + "\n" + ex.getMessage());
//        }        
//    }        
//
//    public boolean hasNoVariable() {
//        return _thisExpressionHasNoVariableReferences;
//    } 
//        
//}