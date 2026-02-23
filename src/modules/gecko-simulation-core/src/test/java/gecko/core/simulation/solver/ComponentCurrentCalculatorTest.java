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
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentCurrentCalculatorTest {

    private ComponentCurrentCalculator calculator;
    private MatrixSolver matrixSolver;

    private static final double EPSILON = 1.0e-12;
    private static final double DT = 1.0e-5;

    @BeforeEach
    void setUp() {
        calculator = new ComponentCurrentCalculator();
        matrixSolver = new MatrixSolver(SolverType.SOLVER_BE);
    }

    @Test
    void testNullMatrixSolverThrowsException() {
        SimpleNetList netlist = new SimpleNetList();
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateComponentCurrents(null, netlist, 0.01, DT, 0.0, true)
        );
    }

    @Test
    void testNullNetlistThrowsException() {
        matrixSolver.initializeMatrices(5, 1, 1);
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateComponentCurrents(matrixSolver, null, 0.01, DT, 0.0, true)
        );
    }

    @Test
    void testResistorCurrentCalculation() {
        matrixSolver.initializeMatrices(5, 1, 1);
        double[] p = matrixSolver.getP();
        p[0] = 10.0;
        p[1] = 2.0;

        SimpleNetList netlist = new SimpleNetList();
        netlist.addComponent(CircuitTypCore.LK_R, 0, 1, new double[4]);
        netlist.getParameter(0)[0] = 2.0;

        calculator.calculateComponentCurrents(matrixSolver, netlist, 0.01, DT, 0.0, true);

        double[] params = netlist.getParameter(0);
        assertEquals(4.0, params[1], EPSILON, "Resistor current should be 4.0 A");
    }

    @Test
    void testInductorCurrentCalculation() {
        matrixSolver.initializeMatrices(5, 1, 1);
        double[] p = matrixSolver.getP();
        double[] iALT = matrixSolver.getIALT();
        p[0] = 10.0;
        p[1] = 0.0;
        iALT[0] = 5.0;

        SimpleNetList netlist = new SimpleNetList();
        netlist.addComponent(CircuitTypCore.LK_L, 0, 1, new double[15]);
        netlist.getParameter(0)[0] = 1.0e-3;

        calculator.calculateComponentCurrents(matrixSolver, netlist, 0.01, DT, 0.0, true);

        double[] params = netlist.getParameter(0);
        assertEquals(5.1, params[1], 1.0e-10, "Inductor current should be 5.1 A");
    }

    @Test
    void testCapacitorCurrentCalculation() {
        matrixSolver.initializeMatrices(5, 1, 1);
        double[] p = matrixSolver.getP();
        double[] pALT = matrixSolver.getPALT();

        p[0] = 100.0;
        p[1] = 0.0;
        pALT[0] = 99.0;
        pALT[1] = 0.0;

        SimpleNetList netlist = new SimpleNetList();
        double[] capParams = new double[15];
        capParams[6] = 1.0e-6;
        capParams[7] = 1.0e-6;
        netlist.addComponent(CircuitTypCore.LK_C, 0, 1, capParams);

        calculator.calculateComponentCurrents(matrixSolver, netlist, 0.01, DT, 0.0, true);

        double[] params = netlist.getParameter(0);
        assertEquals(0.1, params[1], 1.0e-10, "Capacitor current should be 0.1 A");
    }

    @Test
    void testReturnsFalseForSimplifiedVersion() {
        matrixSolver.initializeMatrices(5, 1, 1);
        SimpleNetList netlist = new SimpleNetList();

        boolean needsRecalculation = calculator.calculateComponentCurrents(
                matrixSolver, netlist, 0.01, DT, 0.0, true);

        assertFalse(needsRecalculation, "Simplified version should return false");
    }
}
