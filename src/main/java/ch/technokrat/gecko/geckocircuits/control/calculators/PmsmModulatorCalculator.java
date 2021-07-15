/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control.calculators;

/**
 * TODO: beautify this mess!
 * @author andreas
 */
@SuppressWarnings("PMD")
public final class PmsmModulatorCalculator extends AbstractControlCalculatable {

    private static final int NO_INPUTS = 4;
    private static final int NO_OUTPUTS = 3;

    public PmsmModulatorCalculator() {
        super(NO_INPUTS, NO_OUTPUTS);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        final double valpha = _inputSignal[0][0];
        final double vbeta = _inputSignal[1][0];
        final double triangle = _inputSignal[2][0];
        final double vdc = _inputSignal[3][0];

        int sector = 0;

        double vabs = Math.sqrt(valpha * valpha + vbeta * vbeta);
        double ang = Math.atan2(vbeta, valpha);

        if (vabs >= vdc) {
            vabs = vdc;
        }

        double M_u = 2 * vabs / (Math.sqrt(3) * vdc); //modulation index

        double ang_add = 0;

        if (ang >= 0 && ang < Math.PI / 3) {
            sector = 0;
            ang_add = 0;
        }

        if (ang >= Math.PI / 3 && ang < 2 * Math.PI / 3) {
            sector = 1;
            ang_add = -Math.PI / 3;
        }

        if (ang >= 2 * Math.PI / 3 && ang < Math.PI) {
            sector = 2;
            ang_add = -2 * Math.PI / 3;
        }

        if (ang >= -Math.PI && ang < -2 * Math.PI / 3) {
            sector = 3;
            ang_add = Math.PI;
        }

        if (ang >= -2 * Math.PI / 3 && ang < -Math.PI / 3) {
            sector = 4;
            ang_add = 2 * Math.PI / 3;
        }

        if (ang >= -Math.PI / 3 && ang < 0) {
            sector = 5;
            ang_add = Math.PI / 3;
        }

        double ang_rel = ang + ang_add;

//relative times a vector is applied in a switching half-period:
        double delta_vector1 = Math.sqrt(3) / 2 * M_u * Math.sin(Math.PI / 3 - ang_rel);
        double delta_vector2 = Math.sqrt(3) / 2 * M_u * Math.sin(ang_rel);
        double delta_fw = 0.5 * (1 - delta_vector1 - delta_vector2); //assuming symmetrical fw

        double comp0 = delta_fw;
        double comp1a = delta_fw + delta_vector1;
        double comp1b = delta_fw + delta_vector2;
        double comp2 = delta_fw + delta_vector1 + delta_vector2;

        double pwm0 = (triangle >= comp0) ? 0 : 1;
        double pwm1a = (triangle >= comp1a) ? 0 : 1;
        double pwm1b = (triangle >= comp1b) ? 0 : 1;
        double pwm2 = (triangle >= comp2) ? 0 : 1;

        double U = 0;
        double V = 0;
        double W = 0;


        switch (sector) {

            case 0:
                U = pwm0;
                V = pwm1a;
                W = pwm2;
                break;
            case 1:
                U = pwm1b;
                V = pwm0;
                W = pwm2;
                break;
            case 2:
                U = pwm2;
                V = pwm0;
                W = pwm1a;
                break;
            case 3:
                U = pwm2;
                V = pwm1b;
                W = pwm0;
                break;
            case 4:
                U = pwm1a;
                V = pwm2;
                W = pwm0;
                break;
            case 5:
                U = pwm0;
                V = pwm2;
                W = pwm1b;
                break;
        }

        _outputSignal[0][0] = U;
        _outputSignal[1][0] = V;
        _outputSignal[2][0] = W;
    }
}
