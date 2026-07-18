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
package gecko.geckocircuits.control.calculators;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * TODO: this is the biggest mess I have ever seen. Pleas clean anybody up!
 * @author andreas
 */
@SuppressWarnings({"PMD.AvoidLiteralsInIfConditions", "PMD.AssignmentInOperand"}) // Complex PWM/matrix converter control logic
@SuppressFBWarnings(value = "FL_FLOATS_AS_LOOP_COUNTERS",
        justification = "Phase normalization uses double for precision; bounded while loop prevents infinite iteration")
public final class SparseMatrixCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {

    private static final double NUMERIC_EPSILON = 1e-12;
    private static final double DEFAULT_DUTY_RATIO = 0.5;


    // Detection of the start of a pulse period for calculation -->
    private double fDRPreviousOld = 0, fDRPrevious = 0;
    private boolean newPulsePeriodBegins = true;
    private double tLocal = 0;  // Local time, reset to zero at the start of a pulse period
    // Global calculation variables -->
    private int sectorIn = -1, sectorOut = -1;  // Sector info of the matrix converter
    private double Tp0 = 1 / 25e3, Tp = Tp0;  // initial assumption for the switching frequency (must be determined first and can change)
    private double[] dIN = new double[2], dOUT = new double[5];  // relative duty ratios / turn-on durations
    private double sRp, sSp, sTp, sRm, sSm, sTm, s1, s2, s3;  // Switching signals --> 0 or 1


    public SparseMatrixCalculator() {
        super(8, 9);
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        fDRPreviousOld = 0;
        fDRPrevious = 0;
        Tp = Tp0;
        tLocal = 0;
        newPulsePeriodBegins = true;
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        double ur = _inputSignal[1][0], us = _inputSignal[2][0], ut = _inputSignal[3][0];  // input / grid-side
        double uNmax = _inputSignal[4][0], uOUTmax = _inputSignal[5][0], fOUT = _inputSignal[6][0];  // amplitudes and output frequency
        double fDR = _inputSignal[0][0];  // Clock frequency for the pulse period
        double phi2 = _inputSignal[7][0];  // output-side angle for creating uaOUT*, ubOUT*, ucOUT* for PMSM-control; reliable alternative to fOUT
        if ((fDRPreviousOld < fDRPrevious) && (fDRPrevious > fDR)) {
            newPulsePeriodBegins = true;
        }
        fDRPreviousOld = fDRPrevious;
        fDRPrevious = fDR;
        //-------------
        if (newPulsePeriodBegins) {
            if (tLocal != 0) {
                Tp = tLocal;
            }
            tLocal = 0;
            sectorDetection(ur, us, ut, fOUT, phi2);  // Sector indices sectorIn, sectorOut are determined
            calculateSwitchingTimes(ur, us, ut, uNmax, uOUTmax, fOUT, phi2);  // Duty ratios dOUT=[d1..d5] and dIN=[da,db] are calculated
            newPulsePeriodBegins = false;
        }
        double switchingFrequency = safeDivide(1.0, Tp, safeDivide(1.0, Tp0, 0.0));
        setPulseWidths(dOUT[0], dOUT[1], dOUT[2], dOUT[3], dOUT[4], dIN[0], dIN[1], switchingFrequency);  // all 9 switching signals are generated
        tLocal += deltaT;

        _outputSignal[0][0] = sRp;
        _outputSignal[1][0] = sSp;
        _outputSignal[2][0] = sTp;
        _outputSignal[3][0] = sRm;
        _outputSignal[4][0] = sSm;
        _outputSignal[5][0] = sTm;
        _outputSignal[6][0] = s1;
        _outputSignal[7][0] = s2;
        _outputSignal[8][0] = s3;
    }

    public void setPulseWidths(double d1, double d2, double d3, double d4, double d5, double da, double db, double fDR) {
        int LG = 1000;  // maximum temporal resolution within the pulse period
        int x1, x2, xm, dxh;
        int x1a = -1, x1b = -1, x1c, x1d, x2a = -1, x2b = -1, x2c, x2d, x3a = -1, x3b = -1, x3c, x3d, x4a = -1, x4b = -1, x4c, x4d, x5a = -1, x5b = -1, x5c, x5d;
        int x6a = -1, x6b = -1, x6c, x6d, x7a = -1, x7b = -1, x7c, x7d, x8a = -1, x8b = -1, x8c, x8d, x9a = -1, x9b = -1, x9c, x9d;
        x1 = 0;
        x2 = LG;
        xm = (x2 + x1) / 2;
        dxh = xm - x1;
        double xLocal = LG * fDR * tLocal;
        switch (sectorIn) {
            case 1:
                x1a = x1;
                x1b = xm;
                x2a = -1;
                x2b = -1;
                x3a = -1;
                x3b = -1;
                x4a = -1;
                x4b = -1;
                x5a = x1 + (int) (da * dxh);
                x5b = xm;
                x6a = x1;
                x6b = x5a;
                break;
            case 2:
                x1a = x1;
                x1b = x1 + (int) (da * dxh);
                x2a = x1b;
                x2b = xm;
                x3a = -1;
                x3b = -1;
                x4a = -1;
                x4b = -1;
                x5a = -1;
                x5b = -1;
                x6a = x1;
                x6b = xm;
                break;
            case 3:
                x1a = x1 + (int) (da * dxh);
                x1b = xm;
                x2a = x1;
                x2b = x1a;
                x3a = -1;
                x3b = -1;
                x4a = -1;
                x4b = -1;
                x5a = -1;
                x5b = -1;
                x6a = x1;
                x6b = xm;
                break;
            case 4:
                x1a = -1;
                x1b = -1;
                x2a = x1;
                x2b = xm;
                x3a = -1;
                x3b = -1;
                x4a = x1 + (int) (da * dxh);
                x4b = xm;
                x5a = -1;
                x5b = -1;
                x6a = x1;
                x6b = x4a;
                break;
            case 5:
                x1a = -1;
                x1b = -1;
                x2a = x1;
                x2b = xm;
                x3a = -1;
                x3b = -1;
                x4a = x1;
                x4b = x1 + (int) (da * dxh);
                x5a = -1;
                x5b = -1;
                x6a = x4b;
                x6b = xm;
                break;
            case 6:
                x1a = -1;
                x1b = -1;
                x2a = x1;
                x2b = x1 + (int) (da * dxh);
                x3a = x2b;
                x3b = xm;
                x4a = x1;
                x4b = xm;
                x5a = -1;
                x5b = -1;
                x6a = -1;
                x6b = -1;
                break;
            case 7:
                x1a = -1;
                x1b = -1;
                x2a = x1 + (int) (da * dxh);
                x2b = xm;
                x3a = x1;
                x3b = x2a;
                x4a = x1;
                x4b = xm;
                x5a = -1;
                x5b = -1;
                x6a = -1;
                x6b = -1;
                break;
            case 8:
                x1a = -1;
                x1b = -1;
                x2a = -1;
                x2b = -1;
                x3a = x1;
                x3b = xm;
                x4a = x1;
                x4b = x1 + (int) (da * dxh);
                x5a = x4b;
                x5b = xm;
                x6a = -1;
                x6b = -1;
                break;
            case 9:
                x1a = -1;
                x1b = -1;
                x2a = -1;
                x2b = -1;
                x3a = x1;
                x3b = xm;
                x4a = x1 + (int) (da * dxh);
                x4b = xm;
                x5a = x1;
                x5b = x4a;
                x6a = -1;
                x6b = -1;
                break;
            case 10:
                x1a = x1 + (int) (da * dxh);
                x1b = xm;
                x2a = -1;
                x2b = -1;
                x3a = x1;
                x3b = x1a;
                x4a = -1;
                x4b = -1;
                x5a = x1;
                x5b = xm;
                x6a = -1;
                x6b = -1;
                break;
            case 11:
                x1a = x1;
                x1b = x1 + (int) (da * dxh);
                x2a = -1;
                x2b = -1;
                x3a = x1b;
                x3b = xm;
                x4a = -1;
                x4b = -1;
                x5a = x1;
                x5b = xm;
                x6a = -1;
                x6b = -1;
                break;
            case 12:
                x1a = x1;
                x1b = xm;
                x2a = -1;
                x2b = -1;
                x3a = -1;
                x3b = -1;
                x4a = -1;
                x4b = -1;
                x5a = x1;
                x5b = x1 + (int) (da * dxh);
                x6a = x5b;
                x6b = xm;
                break;
            default:
                break;
        }
        x1c = 2 * xm - x1b;
        x1d = x1 + x2 - x1a;
        x2c = 2 * xm - x2b;
        x2d = x1 + x2 - x2a;
        x3c = 2 * xm - x3b;
        x3d = x1 + x2 - x3a;
        x4c = 2 * xm - x4b;
        x4d = x1 + x2 - x4a;
        x5c = 2 * xm - x5b;
        x5d = x1 + x2 - x5a;
        x6c = 2 * xm - x6b;
        x6d = x1 + x2 - x6a;
        if (sectorIn == 1) {
            sRp = 1;
            sSp = 0;
            sTp = 0;
            sRm = 0;
            if (xLocal < x5a) {
                sSm = 0;
            } else if (xLocal < x5d) {
                sSm = 1;
            } else {
                sSm = 0;
            }
            if (xLocal < x6b) {
                sTm = 1;
            } else if (xLocal < x6c) {
                sTm = 0;
            } else {
                sTm = 1;
            }
        } else if (sectorIn == 2) {
            if (xLocal < x1b) {
                sRp = 1;
            } else if (xLocal < x1c) {
                sRp = 0;
            } else {
                sRp = 1;
            }
            if (xLocal < x2a) {
                sSp = 0;
            } else if (xLocal < x2d) {
                sSp = 1;
            } else {
                sSp = 0;
            }
            sTp = 0;
            sRm = 0;
            sSm = 0;
            sTm = 1;
        } else if (sectorIn == 3) {
            if (xLocal < x1a) {
                sRp = 0;
            } else if (xLocal < x1d) {
                sRp = 1;
            } else {
                sRp = 0;
            }
            if (xLocal < x2b) {
                sSp = 1;
            } else if (xLocal < x2c) {
                sSp = 0;
            } else {
                sSp = 1;
            }
            sTp = 0;
            sRm = 0;
            sSm = 0;
            sTm = 1;
        } else if (sectorIn == 4) {
            sRp = 0;
            sSp = 1;
            sTp = 0;
            if (xLocal < x4a) {
                sRm = 0;
            } else if (xLocal < x4d) {
                sRm = 1;
            } else {
                sRm = 0;
            }
            sSm = 0;
            if (xLocal < x6b) {
                sTm = 1;
            } else if (xLocal < x6c) {
                sTm = 0;
            } else {
                sTm = 1;
            }
        } else if (sectorIn == 5) {
            sRp = 0;
            sSp = 1;
            sTp = 0;
            if (xLocal < x4b) {
                sRm = 1;
            } else if (xLocal < x4c) {
                sRm = 0;
            } else {
                sRm = 1;
            }
            sSm = 0;
            if (xLocal < x6a) {
                sTm = 0;
            } else if (xLocal < x6d) {
                sTm = 1;
            } else {
                sTm = 0;
            }
        } else if (sectorIn == 6) {
            sRp = 0;
            if (xLocal < x2b) {
                sSp = 1;
            } else if (xLocal < x2c) {
                sSp = 0;
            } else {
                sSp = 1;
            }
            if (xLocal < x3a) {
                sTp = 0;
            } else if (xLocal < x3d) {
                sTp = 1;
            } else {
                sTp = 0;
            }
            sRm = 1;
            sSm = 0;
            sTm = 0;
        } else if (sectorIn == 7) {
            sRp = 0;
            if (xLocal < x2a) {
                sSp = 0;
            } else if (xLocal < x2d) {
                sSp = 1;
            } else {
                sSp = 0;
            }
            if (xLocal < x3b) {
                sTp = 1;
            } else if (xLocal < x3c) {
                sTp = 0;
            } else {
                sTp = 1;
            }
            sRm = 1;
            sSm = 0;
            sTm = 0;
        } else if (sectorIn == 8) {
            sRp = 0;
            sSp = 0;
            sTp = 1;
            if (xLocal < x4b) {
                sRm = 1;
            } else if (xLocal < x4c) {
                sRm = 0;
            } else {
                sRm = 1;
            }
            if (xLocal < x5a) {
                sSm = 0;
            } else if (xLocal < x5d) {
                sSm = 1;
            } else {
                sSm = 0;
            }
            sTm = 0;
        } else if (sectorIn == 9) {
            sRp = 0;
            sSp = 0;
            sTp = 1;
            if (xLocal < x4a) {
                sRm = 0;
            } else if (xLocal < x4d) {
                sRm = 1;
            } else {
                sRm = 0;
            }
            if (xLocal < x5b) {
                sSm = 1;
            } else if (xLocal < x5c) {
                sSm = 0;
            } else {
                sSm = 1;
            }
            sTm = 0;
        } else if (sectorIn == 10) {
            if (xLocal < x1a) {
                sRp = 0;
            } else if (xLocal < x1d) {
                sRp = 1;
            } else {
                sRp = 0;
            }
            sSp = 0;
            if (xLocal < x3b) {
                sTp = 1;
            } else if (xLocal < x3c) {
                sTp = 0;
            } else {
                sTp = 1;
            }
            sRm = 0;
            sSm = 1;
            sTm = 0;
        } else if (sectorIn == 11) {
            if (xLocal < x1b) {
                sRp = 1;
            } else if (xLocal < x1c) {
                sRp = 0;
            } else {
                sRp = 1;
            }
            sSp = 0;
            if (xLocal < x3a) {
                sTp = 0;
            } else if (xLocal < x3d) {
                sTp = 1;
            } else {
                sTp = 0;
            }
            sRm = 0;
            sSm = 1;
            sTm = 0;
        } else if (sectorIn == 12) {
            sRp = 1;
            sSp = 0;
            sTp = 0;
            sRm = 0;
            if (xLocal < x5b) {
                sSm = 1;
            } else if (xLocal < x5c) {
                sSm = 0;
            } else {
                sSm = 1;
            }
            if (xLocal < x6a) {
                sTm = 0;
            } else if (xLocal < x6d) {
                sTm = 1;
            } else {
                sTm = 0;
            }
        }
        switch (sectorOut) {
            case 1:  //inverseCurve=false;
                x7a = x1;
                x7b = xm;
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 2:  //inverseCurve=true;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = -1;
                x9b = -1;
                break;
            case 3:  //inverseCurve=true;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = -1;
                x9b = -1;
                break;
            case 4:  //inverseCurve=false;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = x1;
                x8b = xm;
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 5:  //inverseCurve=false;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = x1;
                x8b = xm;
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            case 6:  //inverseCurve=true;
                x7a = -1;
                x7b = -1;
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            case 7:  //inverseCurve=true;
                x7a = -1;
                x7b = -1;
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 8:  //inverseCurve=false;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = x1;
                x9b = xm;
                break;
            case 9:  //inverseCurve=false;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = x1;
                x9b = xm;
                break;
            case 10:  //inverseCurve=true;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = -1;
                x8b = -1;
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 11:  //inverseCurve=true;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = -1;
                x8b = -1;
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            case 12:  //inverseCurve=false;
                x7a = x1;
                x7b = xm;
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            default:
                break;
        }
        x7c = 2 * xm - x7b;
        x7d = x1 + x2 - x7a;
        x8c = 2 * xm - x8b;
        x8d = x1 + x2 - x8a;
        x9c = 2 * xm - x9b;
        x9d = x1 + x2 - x9a;
        if ((sectorOut == 1) || (sectorOut == 12)) {
            s1 = 1;
            if (xLocal < x8a) {
                s2 = 0;
            } else if (xLocal < x8b) {
                s2 = 1;
            } else if (xLocal < x8c) {
                s2 = 0;
            } else if (xLocal < x8d) {
                s2 = 1;
            } else {
                s2 = 0;
            }
            if (xLocal < x9a) {
                s3 = 0;
            } else if (xLocal < x9b) {
                s3 = 1;
            } else if (xLocal < x9c) {
                s3 = 0;
            } else if (xLocal < x9d) {
                s3 = 1;
            } else {
                s3 = 0;
            }
        } else if ((sectorOut == 2) || (sectorOut == 3)) {
            if (xLocal < x7a) {
                s1 = 1;
            } else if (xLocal < x7b) {
                s1 = 0;
            } else if (xLocal < x7c) {
                s1 = 1;
            } else if (xLocal < x7d) {
                s1 = 0;
            } else {
                s1 = 1;
            }
            if (xLocal < x8a) {
                s2 = 1;
            } else if (xLocal < x8b) {
                s2 = 0;
            } else if (xLocal < x8c) {
                s2 = 1;
            } else if (xLocal < x8d) {
                s2 = 0;
            } else {
                s2 = 1;
            }
            s3 = 0;
        } else if ((sectorOut == 4) || (sectorOut == 5)) {
            if (xLocal < x7a) {
                s1 = 0;
            } else if (xLocal < x7b) {
                s1 = 1;
            } else if (xLocal < x7c) {
                s1 = 0;
            } else if (xLocal < x7d) {
                s1 = 1;
            } else {
                s1 = 0;
            }
            s2 = 1;
            if (xLocal < x9a) {
                s3 = 0;
            } else if (xLocal < x9b) {
                s3 = 1;
            } else if (xLocal < x9c) {
                s3 = 0;
            } else if (xLocal < x9d) {
                s3 = 1;
            } else {
                s3 = 0;
            }
        } else if ((sectorOut == 6) || (sectorOut == 7)) {
            s1 = 0;
            if (xLocal < x8a) {
                s2 = 1;
            } else if (xLocal < x8b) {
                s2 = 0;
            } else if (xLocal < x8c) {
                s2 = 1;
            } else if (xLocal < x8d) {
                s2 = 0;
            } else {
                s2 = 1;
            }
            if (xLocal < x9a) {
                s3 = 1;
            } else if (xLocal < x9b) {
                s3 = 0;
            } else if (xLocal < x9c) {
                s3 = 1;
            } else if (xLocal < x9d) {
                s3 = 0;
            } else {
                s3 = 1;
            }
        } else if ((sectorOut == 8) || (sectorOut == 9)) {
            if (xLocal < x7a) {
                s1 = 0;
            } else if (xLocal < x7b) {
                s1 = 1;
            } else if (xLocal < x7c) {
                s1 = 0;
            } else if (xLocal < x7d) {
                s1 = 1;
            } else {
                s1 = 0;
            }
            if (xLocal < x8a) {
                s2 = 0;
            } else if (xLocal < x8b) {
                s2 = 1;
            } else if (xLocal < x8c) {
                s2 = 0;
            } else if (xLocal < x8d) {
                s2 = 1;
            } else {
                s2 = 0;
            }
            s3 = 1;
        } else if ((sectorOut == 10) || (sectorOut == 11)) {
            if (xLocal < x7a) {
                s1 = 1;
            } else if (xLocal < x7b) {
                s1 = 0;
            } else if (xLocal < x7c) {
                s1 = 1;
            } else if (xLocal < x7d) {
                s1 = 0;
            } else {
                s1 = 1;
            }
            s2 = 0;
            if (xLocal < x9a) {
                s3 = 1;
            } else if (xLocal < x9b) {
                s3 = 0;
            } else if (xLocal < x9c) {
                s3 = 1;
            } else if (xLocal < x9d) {
                s3 = 0;
            } else {
                s3 = 1;
            }
        }
    }

    private void calculateSwitchingTimes(double ur, double us, double ut, double uNmax, double uOUTmax, double fOUT, double phi2) {
        switch (sectorIn) {
            case 1:
                dIN[0] = safeDutyRatio(-ut, ur);
                break;
            case 2:
                dIN[0] = safeDutyRatio(-ur, ut);
                break;
            case 3:
                dIN[0] = safeDutyRatio(-us, ut);
                break;
            case 4:
                dIN[0] = safeDutyRatio(-ut, us);
                break;
            case 5:
                dIN[0] = safeDutyRatio(-ur, us);
                break;
            case 6:
                dIN[0] = safeDutyRatio(-us, ur);
                break;
            case 7:
                dIN[0] = safeDutyRatio(-ut, ur);
                break;
            case 8:
                dIN[0] = safeDutyRatio(-ur, ut);
                break;
            case 9:
                dIN[0] = safeDutyRatio(-us, ut);
                break;
            case 10:
                dIN[0] = safeDutyRatio(-ut, us);
                break;
            case 11:
                dIN[0] = safeDutyRatio(-ur, us);
                break;
            case 12:
                dIN[0] = safeDutyRatio(-us, ur);
                break;
            default:
                break;
        }
        dIN[0] = clampDutyRatio(dIN[0]);
        dIN[1] = 1 - dIN[0];

        // Output:
        double k = safeDivide(uOUTmax, uNmax * uNmax, 0.0) / Math.sqrt(3);  // Assumption: Ideal 3-phase voltage grid at input
        double phiOUT = 2 * Math.PI * fOUT * _time - Math.PI / 2;  // old version, does not work for PMSM-control
        if (fOUT <= 0) {
            phiOUT = phi2 - Math.PI / 2;  // phiOUT= thetaEl +dPhiEl -Math.PI/2;  --> improvement EPE 2009
        }
        while (phiOUT >= Math.PI / 3) {
            phiOUT -= Math.PI / 3;
        }
        double ua = sanitizeFinite(k * Math.cos(phiOUT + Math.PI / 6), 0.0);
        double ub = sanitizeFinite(k * Math.sin(phiOUT), 0.0);
        double x1 = 0;
        double x2 = 0;
        switch (sectorIn) {
            case 1:
                x1 = (-2 * ut);
                x2 = (-2 * us);
                break;
            case 2:
                x1 = (2 * ur);
                x2 = (2 * us);
                break;
            case 3:
                x1 = (2 * us);
                x2 = (2 * ur);
                break;
            case 4:
                x1 = (-2 * ut);
                x2 = (-2 * ur);
                break;
            case 5:
                x1 = (-2 * ur);
                x2 = (-2 * ut);
                break;
            case 6:
                x1 = (2 * us);
                x2 = (2 * ut);
                break;
            case 7:
                x1 = (2 * ut);
                x2 = (2 * us);
                break;
            case 8:
                x1 = (-2 * ur);
                x2 = (-2 * us);
                break;
            case 9:
                x1 = (-2 * us);
                x2 = (-2 * ur);
                break;
            case 10:
                x1 = (2 * ut);
                x2 = (2 * ur);
                break;
            case 11:
                x1 = (2 * ur);
                x2 = (2 * ut);
                break;
            case 12:
                x1 = (-2 * us);
                x2 = (-2 * ut);
                break;
            default:
                throw new IllegalArgumentException("Invalid input sector: " + sectorIn);
        }
        switch (sectorOut) {
            case 1:
                dOUT[0] = ua * x1;
                dOUT[1] = ub * x1;
                dOUT[3] = ub * x2;
                dOUT[4] = ua * x2;
                dOUT[2] = 1 - (dOUT[0] + dOUT[1] + dOUT[3] + dOUT[4]);
                break;
            case 2:
                dOUT[1] = ua * x1;
                dOUT[0] = ub * x1;
                dOUT[4] = ub * x2;
                dOUT[3] = ua * x2;
                dOUT[2] = 1 - (dOUT[1] + dOUT[0] + dOUT[4] + dOUT[3]);
                break;
            case 3:
                dOUT[0] = ua * x1;
                dOUT[1] = ub * x1;
                dOUT[3] = ub * x2;
                dOUT[4] = ua * x2;
                dOUT[2] = 1 - (dOUT[0] + dOUT[1] + dOUT[3] + dOUT[4]);
                break;
            case 4:
                dOUT[1] = ua * x1;
                dOUT[0] = ub * x1;
                dOUT[4] = ub * x2;
                dOUT[3] = ua * x2;
                dOUT[2] = 1 - (dOUT[1] + dOUT[0] + dOUT[4] + dOUT[3]);
                break;
            case 5:
                dOUT[0] = ua * x1;
                dOUT[1] = ub * x1;
                dOUT[3] = ub * x2;
                dOUT[4] = ua * x2;
                dOUT[2] = 1 - (dOUT[0] + dOUT[1] + dOUT[3] + dOUT[4]);
                break;
            case 6:
                dOUT[1] = ua * x1;
                dOUT[0] = ub * x1;
                dOUT[4] = ub * x2;
                dOUT[3] = ua * x2;
                dOUT[2] = 1 - (dOUT[1] + dOUT[0] + dOUT[4] + dOUT[3]);
                break;
            case 7:
                dOUT[0] = ua * x1;
                dOUT[1] = ub * x1;
                dOUT[3] = ub * x2;
                dOUT[4] = ua * x2;
                dOUT[2] = 1 - (dOUT[0] + dOUT[1] + dOUT[3] + dOUT[4]);
                break;
            case 8:
                dOUT[1] = ua * x1;
                dOUT[0] = ub * x1;
                dOUT[4] = ub * x2;
                dOUT[3] = ua * x2;
                dOUT[2] = 1 - (dOUT[1] + dOUT[0] + dOUT[4] + dOUT[3]);
                break;
            case 9:
                dOUT[0] = ua * x1;
                dOUT[1] = ub * x1;
                dOUT[3] = ub * x2;
                dOUT[4] = ua * x2;
                dOUT[2] = 1 - (dOUT[0] + dOUT[1] + dOUT[3] + dOUT[4]);
                break;
            case 10:
                dOUT[1] = ua * x1;
                dOUT[0] = ub * x1;
                dOUT[4] = ub * x2;
                dOUT[3] = ua * x2;
                dOUT[2] = 1 - (dOUT[1] + dOUT[0] + dOUT[4] + dOUT[3]);
                break;
            case 11:
                dOUT[0] = ua * x1;
                dOUT[1] = ub * x1;
                dOUT[3] = ub * x2;
                dOUT[4] = ua * x2;
                dOUT[2] = 1 - (dOUT[0] + dOUT[1] + dOUT[3] + dOUT[4]);
                break;
            case 12:
                dOUT[1] = ua * x1;
                dOUT[0] = ub * x1;
                dOUT[4] = ub * x2;
                dOUT[3] = ua * x2;
                dOUT[2] = 1 - (dOUT[1] + dOUT[0] + dOUT[4] + dOUT[3]);
                break;
            default:
                break;
        }
        sanitizeDutyArray(dOUT);
    }

    private void sectorDetection(double ur, double us, double ut, double fOUT, double phi2) {
        // Sector of input voltages:
        if ((us <= 0) && (ut <= us)) {
            sectorIn = 1;
        } else if ((us >= 0) && (ur >= us)) {
            sectorIn = 2;
        } else if ((ur >= 0) && (us >= ur)) {
            sectorIn = 3;
        } else if ((ur <= 0) && (ut <= ur)) {
            sectorIn = 4;
        } else if ((ut <= 0) && (ur <= ut)) {
            sectorIn = 5;
        } else if ((ut >= 0) && (us >= ut)) {
            sectorIn = 6;
        } else if ((us >= 0) && (ut >= us)) {
            sectorIn = 7;
        } else if ((us <= 0) && (ur <= us)) {
            sectorIn = 8;
        } else if ((ur <= 0) && (us <= ur)) {
            sectorIn = 9;
        } else if ((ur >= 0) && (ut >= ur)) {
            sectorIn = 10;
        } else if ((ut >= 0) && (ur >= ut)) {
            sectorIn = 11;
        } else if ((ut <= 0) && (us <= ut)) {
            sectorIn = 12;
        }
        // Output voltage sector:
        double phiOUT = 2 * Math.PI * fOUT * _time;  // old version, does not work for PMSM-control
        if (fOUT <= 0) {
            phiOUT = phi2;  // phiOUT= thetaEl +dPhiEl; --> improvement EPE 2009
        }
        double u1 = Math.sin(phiOUT);
        double u2 = Math.sin(phiOUT - 2 * Math.PI / 3);
        double u3 = Math.sin(phiOUT - 4 * Math.PI / 3);
        if ((u2 < 0) && (u3 < u2)) {
            sectorOut = 1;
        } else if ((u2 > 0) && (u1 > u2)) {
            sectorOut = 2;
        } else if ((u1 > 0) && (u2 > u1)) {
            sectorOut = 3;
        } else if ((u1 < 0) && (u3 < u1)) {
            sectorOut = 4;
        } else if ((u3 < 0) && (u1 < u3)) {
            sectorOut = 5;
        } else if ((u3 > 0) && (u2 > u3)) {
            sectorOut = 6;
        } else if ((u2 > 0) && (u3 > u2)) {
            sectorOut = 7;
        } else if ((u2 < 0) && (u1 < u2)) {
            sectorOut = 8;
        } else if ((u1 < 0) && (u2 < u1)) {
            sectorOut = 9;
        } else if ((u1 > 0) && (u3 > u1)) {
            sectorOut = 10;
        } else if ((u3 > 0) && (u1 > u3)) {
            sectorOut = 11;
        } else if ((u3 < 0) && (u2 < u3)) {
            sectorOut = 12;
        }
    }

    private double safeDutyRatio(double numerator, double denominator) {
        return clampDutyRatio(safeDivide(numerator, denominator, DEFAULT_DUTY_RATIO));
    }

    private double clampDutyRatio(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_DUTY_RATIO;
        }
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    private double safeDivide(double numerator, double denominator, double fallback) {
        if (!Double.isFinite(numerator) || !Double.isFinite(denominator) || Math.abs(denominator) <= NUMERIC_EPSILON) {
            return fallback;
        }
        return sanitizeFinite(numerator / denominator, fallback);
    }

    private double sanitizeFinite(double value, double fallback) {
        if (Double.isFinite(value)) {
            return value;
        }
        return fallback;
    }

    private void sanitizeDutyArray(double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = sanitizeFinite(values[i], 0.0);
        }
    }
}
