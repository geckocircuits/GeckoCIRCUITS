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

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import ch.technokrat.modelviewcontrol.ModelMVC;

public class SolverSettings {
    
    public ModelMVC<SolverType> SOLVER_TYPE = new ModelMVC<SolverType>(SolverType.SOLVER_BE ,"solver");
    public final ModelMVC<Double> _T_pre = new ModelMVC<Double>(-100e-3, "pre-calculation time");
    public final ModelMVC<Double> _dt_pre = new ModelMVC<Double>(100e-9, "pre-calculation stepwidth");
    
    public final ModelMVC<Double> dt = new ModelMVC<Double>(0.1e-6, "simulation stepwidth dt");
    public final ModelMVC<Double> _tDURATION = new ModelMVC<Double>(20e-3, "simulation time");
    public final ModelMVC<Double> _tPAUSE = new ModelMVC<Double>(-1.0, "simulation pause time");
    
    public double _dt_ALT;  // Merken der alten Werten nach einer Aenderung in 'DialogSimParameter'
    // flag used for solver start
    public boolean inPreCalculationMode = false;
}
