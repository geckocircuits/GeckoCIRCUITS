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
//import java.applet.Applet;
//import java.util.ArrayList;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.script.Bindings;
//import javax.script.ScriptContext;
//import javax.script.ScriptEngine;
//import javax.script.ScriptEngineManager;
//import javax.script.ScriptException;
//
//public class JavaScriptTest extends Applet {
//
//    public static List<AbstractExpression> allExpressions;
//    private static ScriptEngineManager mgr = new ScriptEngineManager();
//    public static ScriptEngine engine = mgr.getEngineByName("JavaScript");
//    public static int evaluationCounter = 0;
//    private static Bindings _bindings;
//    final String Digits = "(\\p{Digit}+)";
//    final String HexDigits = "(\\p{XDigit}+)";
//
//    public static void main(String[] args) {
//        try {
//            engine.getContext().setAttribute("callBack", new JavaScriptTest(),
//                    ScriptContext.ENGINE_SCOPE);
//
//
//            allExpressions = new ArrayList<AbstractExpression>();
//            final int NN = 9;
//            for (int i = 0; i < NN; i++) {
//                if (i == 0) {
//                    allExpressions.add(AbstractExpression.newInstance("$R." + i, "$R.5"));
//                    //allExpressions.add(AbstractExpression.newInstance("$R.0", "$R." + (NN - 1)));
//                } else {
//                    String expression = "1 + $R." + ((i - 1));
//                    allExpressions.add(AbstractExpression.newInstance("$R." + i, expression));
//                }
//
//            }
//
//            long tick = System.currentTimeMillis();
//            for (int i = NN - 1; i >= 0; i--) {
//                System.out.print("evaluating " + allExpressions.get(i));
//                System.out.println("\t" + allExpressions.get(i).evaluate());
//            }
//
//            System.out.println("evaluation counter " + evaluationCounter);
//            long tock = System.currentTimeMillis();
//            System.out.println("111 time millis " + (tock - tick));
//
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(JavaScriptTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        } catch (ScriptException ex) {
//            String niceMessage = ex.getMessage().replaceAll("sun.org.mozilla.javascript.internal.WrappedException: Wrapped javax.script.ScriptException:", "Wrapped: ");
//            if(niceMessage.contains(LOOP_STRING)) {
//                niceMessage = niceMessage.substring(0, niceMessage.indexOf(LOOP_STRING) + LOOP_STRING.length());
//            }            
//            
//            System.err.println("ex message " + niceMessage);
//        }
//    }
//    private LinkedHashSet<AbstractExpression> _expressionStack;
//    private static final String LOOP_STRING = "Error: loop detected in expression stack!";
//
//    public double invoke(final String test) throws ScriptException {
//        evaluationCounter++;
//        if (VariableExpression.variablesMappedToNumbers.containsKey(test)) {
//            return VariableExpression.variablesMappedToNumbers.get(test);
//        } else {
//            if (_expressionStack == null) {
//                _expressionStack = new LinkedHashSet<AbstractExpression>();
//            }
//
//            Double newVariable = searchForUnknownVariable(test);
//            if (newVariable == null) {
//                throw new IllegalArgumentException("Error: Variable " + test + " is not known within this context!");
//            }
//            assert newVariable instanceof Double;
//            _expressionStack = null;
//            return newVariable;
//        }
//
//    }
//
//    private Double searchForUnknownVariable(final String test) throws ScriptException {
//        for (AbstractExpression expression : JavaScriptTest.allExpressions) {
//            if (expression.nameMatchesFirstTest(test)) {
//                if (_expressionStack != null) {
//                    if (_expressionStack.contains(expression)) {
//                        throw new ScriptException(LOOP_STRING);
//                    } else {
//                        _expressionStack.add(expression);
//                    }
//                }
//                //System.out.println("searching for variable " + test + " " + _expressionStack.size());
//                return expression.evaluate();
//            }
//        }
//        return 0.0;
//    }
//}
