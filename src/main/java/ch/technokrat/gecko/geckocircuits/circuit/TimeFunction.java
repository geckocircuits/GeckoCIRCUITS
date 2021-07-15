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
package ch.technokrat.gecko.geckocircuits.circuit;

public abstract class TimeFunction {

    protected int _steps_saved;
    protected double[][] var_history;
    protected boolean stepped_back = false;
    protected int steps_reversed = 0;
    public static boolean saveHistory = false;

    public abstract double calculate(double t, double dt);

    protected void historyForward()
    {
      for (int j = var_history.length - 1; j > 0; j--)
        for (int i = 0; i < var_history[0].length; i++)
            var_history[j][i] = var_history[j-1][i];
    }

    protected void historyBackward()
    {
      for (int j = var_history.length - 1; j > 0; j--)
        for (int i = 0; i < var_history[0].length; i++)
            var_history[j-1][i] = var_history[j][i];
    }

    public abstract void stepBack();

}
