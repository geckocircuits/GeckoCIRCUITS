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


public class PmsmControlCalculator extends AbstractControlCalculatable {

    double time_last = 0;
    double psi_sa_last = 0;
    double psi_sb_last = 0;
    double valpha_last = 0;
    double vbeta_last = 0;
    double phi_last = 0;
    double phi_startup_last = 0;
    double int_n_last = 0;
    double int_iq_last = 0;
    double int_id_last = 0;
    double x_n_last = 0;
    double x_iq_last = 0;
    double x_id_last = 0;
    double w_est_last = 0;
    double w_est_filt_last = 0;
    boolean startup = true;
    double vf_offset = 0;

    public PmsmControlCalculator() {
        super(12, 8);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        final double ia = _inputSignal[0][0];
        final double ib = _inputSignal[1][0];
        final double w = _inputSignal[2][0];
        final double phi = _inputSignal[3][0];
        final double n_ref = _inputSignal[4][0];
        final double Kp_n = _inputSignal[5][0];
        final double T_n = _inputSignal[6][0];
        final double n_limit = _inputSignal[7][0];
        final double Kp_i = _inputSignal[8][0];
        final double T_i = _inputSignal[9][0];
        final double i_limit = _inputSignal[10][0];
        final double fP = _inputSignal[11][0];


        final double a1_n = Kp_n / T_n;
        final double a1_i = Kp_i / T_i;

        final double w_ref = n_ref / 60 * 2 * Math.PI;


        final double delta_t = deltaT;

//*** Abort if sample time not expired
//if (time-time_last < 0.1e-3) return yOUT;

        final double ialpha = ia;
        final double ibeta = 1 / Math.sqrt(3) * (2 * ib + ia);

//Park transformation:
        final double id = ialpha * Math.cos(phi) + ibeta * Math.sin(phi);
        final double iq = -ialpha * Math.sin(phi) + ibeta * Math.cos(phi);

//Speed control:
        final double x_n = w_ref - w;
        double int_n = int_n_last + a1_n * delta_t * 0.5 * (x_n + x_n_last);
        if (int_n > n_limit) {
            int_n = n_limit;
        }
        if (int_n < -n_limit) {
            int_n = -n_limit;
        }
        double iq_ref = Kp_n * x_n + int_n;
        if (iq_ref > n_limit) {
            iq_ref = n_limit;
        }
        if (iq_ref < -n_limit) {
            iq_ref = -n_limit;
        }

//Iq control:
        final double x_iq = iq_ref - iq;
        double int_iq = int_iq_last + a1_i * delta_t * 0.5 * (x_iq + x_iq_last);
        if (int_iq > i_limit) {
            int_iq = i_limit;
        }
        if (int_iq < -i_limit) {
            int_iq = -i_limit;
        }
        double vq_ref = Kp_i * x_iq + int_iq;
        if (vq_ref > i_limit) {
            vq_ref = i_limit;
        }
        if (vq_ref < -i_limit) {
            vq_ref = -i_limit;
        }

//Id control:
        final double id_ref = 0;
        final double x_id = id_ref - id;
        double int_id = int_id_last + a1_i * delta_t * 0.5 * (x_id + x_id_last);
        if (int_id > i_limit) {
            int_id = i_limit;
        }
        if (int_id < -i_limit) {
            int_id = -i_limit;
        }
        double vd_ref = Kp_i * x_id + int_id;
        if (vd_ref > i_limit) {
            vd_ref = i_limit;
        }
        if (vd_ref < -i_limit) {
            vd_ref = -i_limit;
        }

//Inverse park transformation:
        final double valpha = vd_ref * Math.cos(phi) - vq_ref * Math.sin(phi);
        final double vbeta = vd_ref * Math.sin(phi) + vq_ref * Math.cos(phi);       

        if (fP > 999e-3) {
            valpha_last = valpha;
            vbeta_last = vbeta;
        }

        x_n_last = x_n;
        int_n_last = int_n;
        x_iq_last = x_iq;
        int_iq_last = int_iq;
        x_id_last = x_id;
        int_id_last = int_id;

        _outputSignal[0][0] = valpha_last;
        _outputSignal[1][0] = vbeta_last;
        _outputSignal[2][0] = vq_ref;
        _outputSignal[3][0] = vd_ref;
        _outputSignal[4][0] = iq_ref;
        _outputSignal[5][0] = id_ref;
        _outputSignal[6][0] = iq;
        _outputSignal[7][0] = id;
    }
}
