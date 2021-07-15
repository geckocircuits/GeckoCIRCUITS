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
//import javax.script.ScriptException;
//
//abstract class AbstractExpression {
//
//    protected final Object _nameable;
//    final String _scriptingCodeGivenFromUser;
//
//    abstract Double evaluate() throws ScriptException;
//
//    public static AbstractExpression newInstance(final Object nameable, final String expression) 
//    throws ScriptException {
//        VariableExpression possibleReturnValue = new VariableExpression(nameable, expression);
//        if (possibleReturnValue.hasNoVariable()) {
//            return new ConstantExpression(nameable, possibleReturnValue);
//        }
//        return possibleReturnValue;
//    }
//
//    AbstractExpression(final Object nameable, final String expression) {
//        _nameable = nameable;
//        _scriptingCodeGivenFromUser = expression;
//    }
//
//    public boolean nameMatchesFirstTest(final String testName) {
//        return _nameable.toString().equals(testName);
//    }    
//    
//    @Override
//    public final String toString() {
//        return _nameable.toString() + " = " + _scriptingCodeGivenFromUser;
//    }
//}