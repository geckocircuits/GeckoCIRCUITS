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
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

/**
 * Encapsulates solver configuration for circuit simulation.
 *
 * This class provides context information needed by matrix stampers,
 * including time step size and solver type. It centralizes the logic
 * for determining integration method-specific parameters.
 *
 * Supported solver types:
 * - Backward Euler (BE): First-order implicit method
 * - Trapezoidal Rule (TRZ): Second-order implicit method
 * - Gear-Shichman (GS): Uses trapezoidal-like scaling
 *
 * @author GeckoCIRCUITS Team
 */
public class SolverContext {

    /** Backward Euler integration method */
    public static final int SOLVER_BE = 0;

    /** Trapezoidal rule integration method */
    public static final int SOLVER_TRZ = 1;

    /** Gear-Shichman integration method */
    public static final int SOLVER_GS = 2;

    /** Minimum time step to prevent numerical issues */
    private static final double MIN_DT = 1e-15;

    private final double dt;
    private final int solverType;

    /**
     * Creates a solver context with specified parameters.
     *
     * @param dt time step size in seconds
     * @param solverType solver type (SOLVER_BE, SOLVER_TRZ, or SOLVER_GS)
     * @throws IllegalArgumentException if dt is not positive
     */
    public SolverContext(double dt, int solverType) {
        if (dt <= 0) {
            throw new IllegalArgumentException("Time step must be positive: " + dt);
        }
        this.dt = Math.max(dt, MIN_DT);
        this.solverType = solverType;
    }

    /**
     * Creates a solver context with Backward Euler method.
     *
     * @param dt time step size in seconds
     */
    public SolverContext(double dt) {
        this(dt, SOLVER_BE);
    }

    /**
     * Gets the time step size.
     *
     * @return time step in seconds
     */
    public double getDt() {
        return dt;
    }

    /**
     * Gets the solver type.
     *
     * @return solver type constant
     */
    public int getSolverType() {
        return solverType;
    }

    /**
     * Gets the scale factor for trapezoidal integration.
     *
     * For Backward Euler, the scale is 1.0 (companion model: G = C/dt for capacitor).
     * For Trapezoidal/GS, the scale is 2.0 (companion model: G = 2C/dt for capacitor).
     *
     * @return 2.0 for trapezoidal/GS methods, 1.0 for Backward Euler
     */
    public double getTrapezoidalScale() {
        return (solverType == SOLVER_TRZ || solverType == SOLVER_GS) ? 2.0 : 1.0;
    }

    /**
     * Checks if using trapezoidal or GS integration.
     *
     * @return true if using trapezoidal-like integration
     */
    public boolean isTrapezoidal() {
        return solverType == SOLVER_TRZ || solverType == SOLVER_GS;
    }

    /**
     * Checks if using Backward Euler integration.
     *
     * @return true if using Backward Euler
     */
    public boolean isBackwardEuler() {
        return solverType == SOLVER_BE;
    }

    /**
     * Gets the effective conductance for a capacitor.
     *
     * BE: G = C/dt
     * TRZ/GS: G = 2C/dt
     *
     * @param capacitance capacitance value in Farads
     * @return effective conductance for MNA stamping
     */
    public double getCapacitorConductance(double capacitance) {
        return getTrapezoidalScale() * capacitance / dt;
    }

    /**
     * Gets the effective conductance for an inductor.
     *
     * BE: G = dt/L
     * TRZ/GS: G = dt/(2L)
     *
     * @param inductance inductance value in Henries
     * @return effective conductance for MNA stamping
     */
    public double getInductorConductance(double inductance) {
        if (inductance <= 0) {
            throw new IllegalArgumentException("Inductance must be positive: " + inductance);
        }
        return dt / (getTrapezoidalScale() * inductance);
    }

    @Override
    public String toString() {
        String typeName;
        switch (solverType) {
            case SOLVER_BE:
                typeName = "BackwardEuler";
                break;
            case SOLVER_TRZ:
                typeName = "Trapezoidal";
                break;
            case SOLVER_GS:
                typeName = "GearShichman";
                break;
            default:
                typeName = "Unknown(" + solverType + ")";
        }
        return "SolverContext[dt=" + dt + ", type=" + typeName + "]";
    }
}
