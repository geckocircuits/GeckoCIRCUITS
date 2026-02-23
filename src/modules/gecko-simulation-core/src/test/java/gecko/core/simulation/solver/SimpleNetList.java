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

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.INetList;

/**
 * Simple in-memory netlist implementation for testing.
 */
public class SimpleNetList implements INetList {
    private CircuitTypCore[] types;
    private int[] nodeX;
    private int[] nodeY;
    private double[][] parameters;
    private int elementCount = 0;

    public SimpleNetList() {
        types = new CircuitTypCore[10];
        nodeX = new int[10];
        nodeY = new int[10];
        parameters = new double[10][];
    }

    public void addComponent(CircuitTypCore type, int x, int y, double[] params) {
        if (elementCount < types.length) {
            types[elementCount] = type;
            nodeX[elementCount] = x;
            nodeY[elementCount] = y;
            parameters[elementCount] = params;
            elementCount++;
        }
    }

    @Override
    public int getNodeMax() { return 5; }

    @Override
    public int getVoltageSourceMax() { return 1; }

    @Override
    public int getElementCount() { return elementCount; }

    @Override
    public CircuitTypCore getType(int index) { return types[index]; }

    @Override
    public int getNodeX(int index) { return nodeX[index]; }

    @Override
    public int getNodeY(int index) { return nodeY[index]; }

    @Override
    public double[] getParameter(int index) { return parameters[index]; }

    @Override
    public int getVoltageSourceNumber(int index) { return -1; }

    @Override
    public double getSimulationTime() { return 0.0; }
}
