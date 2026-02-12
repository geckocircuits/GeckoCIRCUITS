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
package gecko.geckocircuits.circuit.circuitcomponents;


public final class VoltageSourceDIDTControlledCalculator extends AbstractVoltageSourceControlledCalculator
    implements HistoryUpdatable, BStampable {

    public VoltageSourceDIDTControlledCalculator(final AbstractVoltageSource parent) {
        super(parent);
    }

    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        assert _z > 0;
        super.stampMatrixA(matrix, deltaT);
        matrix[_z][_currentControl.getZValue()] += _gain / deltaT;

    }

    @Override
    public void stampVectorB(final double[] bVector, final double time, final double deltaT) {
            double value = _gain * _currentControl.getCurrent() / deltaT;
            bVector[_z] += value;//_function.calculate(t, dt);
    }

    @Override
    public boolean isBasisStampable() {
        return false;
    }

    @Override
    public void registerBVector(final BVector bvector) {
        // nothing todo??? 
    }

}
