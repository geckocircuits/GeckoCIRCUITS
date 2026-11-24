/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;

/** A winding uses an auxiliary current and a flux constraint, rather than a parallel conductance. */
public final class CoupledInductorStamper extends InductorStamper {
    @Override
    public void stampMatrixA(MatrixCoefficients a, int x, int y, int z,
                             double[] parameters, double dt, SolverType method) {
        double inductance = windingInductance(parameters, dt);
        double alpha = switch (method) {
            case SOLVER_BE -> 1;
            case SOLVER_TRZ -> 2;
            case SOLVER_GS -> 1.5;
        };
        a.add(x, z, 1);
        a.add(y, z, -1);
        a.add(z, x, 1);
        a.add(z, y, -1);
        a.add(z, z, -(alpha * inductance / dt));
        if (parameters.length > 10) {
            parameters[10] = inductance;
        }
    }

    /** Standalone Backward Euler self-history. MatrixSolver adds method-specific mutual history. */
    @Override
    public void stampVectorB(double[] b, int x, int y, int z, double[] parameters,
                              double dt, double time, double[] previousValues) {
        double inductance = windingInductance(parameters, dt);
        double current = previousValues == null || previousValues.length == 0 ? 0 : previousValues[0];
        b[z] -= inductance / dt * current;
    }

    private static double windingInductance(double[] parameters, double dt) {
        if (parameters == null || parameters.length == 0 || !Double.isFinite(parameters[0])
                || parameters[0] <= 0 || !Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Coupled winding requires finite positive inductance and time step");
        }
        return parameters[0];
    }
}
