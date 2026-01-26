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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SolverContext - verifies solver configuration encapsulation.
 */
public class SolverContextTest {

    private static final double TOLERANCE = 1e-12;

    @Test
    public void testConstructor_BackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        assertEquals("dt should match", 1e-6, ctx.getDt(), TOLERANCE);
        assertEquals("solver type should be BE", SolverContext.SOLVER_BE, ctx.getSolverType());
    }

    @Test
    public void testConstructor_Trapezoidal() {
        SolverContext ctx = new SolverContext(1e-5, SolverContext.SOLVER_TRZ);
        assertEquals("dt should match", 1e-5, ctx.getDt(), TOLERANCE);
        assertEquals("solver type should be TRZ", SolverContext.SOLVER_TRZ, ctx.getSolverType());
    }

    @Test
    public void testConstructor_GearShichman() {
        SolverContext ctx = new SolverContext(1e-4, SolverContext.SOLVER_GS);
        assertEquals("dt should match", 1e-4, ctx.getDt(), TOLERANCE);
        assertEquals("solver type should be GS", SolverContext.SOLVER_GS, ctx.getSolverType());
    }

    @Test
    public void testDefaultConstructor_UsesBackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6);
        assertEquals("default solver type should be BE", SolverContext.SOLVER_BE, ctx.getSolverType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_ZeroDt_ThrowsException() {
        new SolverContext(0.0, SolverContext.SOLVER_BE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_NegativeDt_ThrowsException() {
        new SolverContext(-1e-6, SolverContext.SOLVER_BE);
    }

    @Test
    public void testGetTrapezoidalScale_BackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        assertEquals("BE scale should be 1.0", 1.0, ctx.getTrapezoidalScale(), TOLERANCE);
    }

    @Test
    public void testGetTrapezoidalScale_Trapezoidal() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);
        assertEquals("TRZ scale should be 2.0", 2.0, ctx.getTrapezoidalScale(), TOLERANCE);
    }

    @Test
    public void testGetTrapezoidalScale_GearShichman() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_GS);
        assertEquals("GS scale should be 2.0", 2.0, ctx.getTrapezoidalScale(), TOLERANCE);
    }

    @Test
    public void testIsTrapezoidal_BackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        assertFalse("BE should not be trapezoidal", ctx.isTrapezoidal());
    }

    @Test
    public void testIsTrapezoidal_Trapezoidal() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);
        assertTrue("TRZ should be trapezoidal", ctx.isTrapezoidal());
    }

    @Test
    public void testIsTrapezoidal_GearShichman() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_GS);
        assertTrue("GS should be trapezoidal-like", ctx.isTrapezoidal());
    }

    @Test
    public void testIsBackwardEuler() {
        SolverContext ctxBE = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        SolverContext ctxTRZ = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);

        assertTrue("BE should be backward euler", ctxBE.isBackwardEuler());
        assertFalse("TRZ should not be backward euler", ctxTRZ.isBackwardEuler());
    }

    @Test
    public void testGetCapacitorConductance_BackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        double capacitance = 1e-6; // 1 uF

        // BE: G = C/dt = 1e-6 / 1e-6 = 1.0
        double expected = 1.0;
        assertEquals("BE capacitor conductance", expected, ctx.getCapacitorConductance(capacitance), TOLERANCE);
    }

    @Test
    public void testGetCapacitorConductance_Trapezoidal() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);
        double capacitance = 1e-6; // 1 uF

        // TRZ: G = 2C/dt = 2 * 1e-6 / 1e-6 = 2.0
        double expected = 2.0;
        assertEquals("TRZ capacitor conductance", expected, ctx.getCapacitorConductance(capacitance), TOLERANCE);
    }

    @Test
    public void testGetInductorConductance_BackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        double inductance = 1e-3; // 1 mH

        // BE: G = dt/L = 1e-6 / 1e-3 = 1e-3
        double expected = 1e-3;
        assertEquals("BE inductor conductance", expected, ctx.getInductorConductance(inductance), TOLERANCE);
    }

    @Test
    public void testGetInductorConductance_Trapezoidal() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);
        double inductance = 1e-3; // 1 mH

        // TRZ: G = dt/(2L) = 1e-6 / (2 * 1e-3) = 5e-4
        double expected = 5e-4;
        assertEquals("TRZ inductor conductance", expected, ctx.getInductorConductance(inductance), TOLERANCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInductorConductance_ZeroInductance_ThrowsException() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        ctx.getInductorConductance(0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInductorConductance_NegativeInductance_ThrowsException() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        ctx.getInductorConductance(-1e-3);
    }

    @Test
    public void testToString_BackwardEuler() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_BE);
        String str = ctx.toString();
        assertTrue("should contain dt", str.contains("1.0E-6") || str.contains("1E-6"));
        assertTrue("should contain BackwardEuler", str.contains("BackwardEuler"));
    }

    @Test
    public void testToString_Trapezoidal() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);
        String str = ctx.toString();
        assertTrue("should contain Trapezoidal", str.contains("Trapezoidal"));
    }

    @Test
    public void testToString_GearShichman() {
        SolverContext ctx = new SolverContext(1e-6, SolverContext.SOLVER_GS);
        String str = ctx.toString();
        assertTrue("should contain GearShichman", str.contains("GearShichman"));
    }

    @Test
    public void testToString_UnknownType() {
        SolverContext ctx = new SolverContext(1e-6, 99);
        String str = ctx.toString();
        assertTrue("should contain Unknown", str.contains("Unknown"));
    }

    @Test
    public void testSolverConstants() {
        assertEquals("BE should be 0", 0, SolverContext.SOLVER_BE);
        assertEquals("TRZ should be 1", 1, SolverContext.SOLVER_TRZ);
        assertEquals("GS should be 2", 2, SolverContext.SOLVER_GS);
    }
}
