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
package ch.technokrat.gecko.geckocircuits.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.technokrat.gecko.geckocircuits.math.Matrix;
import ch.technokrat.gecko.geckocircuits.math.LUDecomposition;
import ch.technokrat.gecko.geckocircuits.circuit.matrix.SolverContext;
import ch.technokrat.gecko.geckocircuits.circuit.matrix.StamperRegistry;
import ch.technokrat.gecko.geckocircuits.circuit.matrix.IMatrixStamper;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;

/**
 * Tests verifying that the simulation engine can run in headless mode
 * without any GUI dependencies.
 */
@DisplayName("Headless Engine Tests")
class HeadlessEngineTest {

    @Nested
    @DisplayName("Matrix Solver Headless")
    class MatrixSolverHeadless {

        @Test
        @DisplayName("Matrix creation works without display")
        void matrixCreationHeadless() {
            System.setProperty("java.awt.headless", "true");

            Matrix m = new Matrix(3, 3);
            m.set(0, 0, 1.0);
            m.set(1, 1, 2.0);
            m.set(2, 2, 3.0);

            assertEquals(1.0, m.get(0, 0), 1e-10);
        }

        @Test
        @DisplayName("LU decomposition works headless")
        void luDecompositionHeadless() {
            System.setProperty("java.awt.headless", "true");

            double[][] data = {{2, 1}, {1, 3}};
            Matrix A = new Matrix(data);

            LUDecomposition lu = new LUDecomposition(A);
            assertTrue(lu.isNonsingular());
        }

        @Test
        @DisplayName("Large matrix operations headless")
        void largeMatrixHeadless() {
            System.setProperty("java.awt.headless", "true");

            int size = 100;
            Matrix large = Matrix.identity(size, size);

            for (int i = 0; i < size - 1; i++) {
                large.set(i, i + 1, 0.1);
                large.set(i + 1, i, 0.1);
            }

            LUDecomposition lu = new LUDecomposition(large);
            assertTrue(lu.isNonsingular());
        }
    }

    @Nested
    @DisplayName("Stamper Registry Headless")
    class StamperRegistryHeadless {

        @Test
        @DisplayName("Registry creation headless")
        void registryCreationHeadless() {
            System.setProperty("java.awt.headless", "true");

            StamperRegistry registry = StamperRegistry.createDefault();
            assertNotNull(registry);
        }

        @Test
        @DisplayName("All basic stampers available headless")
        void basicStampersHeadless() {
            System.setProperty("java.awt.headless", "true");

            StamperRegistry registry = StamperRegistry.createDefault();

            assertNotNull(registry.getStamper(CircuitTyp.LK_R), "Resistor");
            assertNotNull(registry.getStamper(CircuitTyp.LK_C), "Capacitor");
            assertNotNull(registry.getStamper(CircuitTyp.LK_L), "Inductor");
        }

        @Test
        @DisplayName("Stamping operations headless")
        void stampingHeadless() {
            System.setProperty("java.awt.headless", "true");

            StamperRegistry registry = StamperRegistry.createDefault();
            IMatrixStamper resistor = registry.getStamper(CircuitTyp.LK_R);

            double[][] G = new double[3][3];
            resistor.stampMatrixA(G, 0, 1, 0, new double[]{1000.0}, 1e-6);

            assertTrue(G[0][0] > 0, "Should stamp conductance");
        }
    }

    @Nested
    @DisplayName("Solver Context Headless")
    class SolverContextHeadless {

        @Test
        @DisplayName("Solver context creation headless")
        void solverContextHeadless() {
            System.setProperty("java.awt.headless", "true");

            SolverContext solver = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);
            assertNotNull(solver);
        }

        @Test
        @DisplayName("Component conductance calculations headless")
        void conductanceCalculationsHeadless() {
            System.setProperty("java.awt.headless", "true");

            SolverContext solver = new SolverContext(1e-6, SolverContext.SOLVER_TRZ);

            double gC = solver.getCapacitorConductance(1e-6);
            double gL = solver.getInductorConductance(1e-3);

            assertTrue(gC > 0, "Capacitor conductance");
            assertTrue(gL > 0, "Inductor conductance");
        }
    }

    @Nested
    @DisplayName("Full Simulation Headless")
    class FullSimulationHeadless {

        @Test
        @DisplayName("Complete RC simulation headless")
        void rcSimulationHeadless() {
            System.setProperty("java.awt.headless", "true");

            // Build and solve RC circuit
            double R = 1000.0;
            double C = 1e-6;
            double Vs = 10.0;
            double dt = 1e-5;

            double gR = 1.0 / R;
            double gC = C / dt;

            double[][] G = new double[3][3];
            G[0][0] = gR; G[0][1] = -gR; G[0][2] = 1;
            G[1][0] = -gR; G[1][1] = gR + gC;
            G[2][0] = 1;

            Matrix GMat = new Matrix(G);
            LUDecomposition lu = new LUDecomposition(GMat);

            assertTrue(lu.isNonsingular(), "Should solve RC circuit headless");

            // Simulate
            double vC = 0;
            for (int step = 0; step < 100; step++) {
                double[] rhs = {0, gC * vC, Vs};
                Matrix rhsMat = new Matrix(new double[][]{{rhs[0]}, {rhs[1]}, {rhs[2]}});
                Matrix solution = lu.solve(rhsMat);
                vC = solution.get(1, 0);
            }

            assertTrue(vC > 0, "Capacitor should charge");
        }
    }
}
