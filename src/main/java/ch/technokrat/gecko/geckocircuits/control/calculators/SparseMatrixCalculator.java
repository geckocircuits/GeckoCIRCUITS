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
 * TODO: this is the biggest mess I have ever seen. Pleas clean anybody up!
 * @author andreas
 */
public final class SparseMatrixCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {

    
    // Dedektion eines Pulsperioden-Beginns zur Berechnung --> 
    private double fDRaltalt = 0, fDRalt = 0;
    private boolean neuePulsperiodeBeginnt = true;
    private double tLokal = 0;  // Lokalzeit, wird bei Pulsperiodenbeginn auf Null gesetzt 
    // Globale Rechengroessen --> 
    private int seIN = -1, seOUT = -1;  // Sektorinfo des Matrix-Konverters
    private double Tp0 = 1 / 25e3, Tp = Tp0;  // initiale Annahme fuer die Schaltfrequenz (muss erst ermittelt werden und kann sich aendern) 
    private double[] dIN = new double[2], dOUT = new double[5];  // relative Einschaltdauern 
    private double sRp, sSp, sTp, sRm, sSm, sTm, s1, s2, s3;  // Schaltsignale --> 0 oder 1 
    

    public SparseMatrixCalculator() {
        super(8, 9);
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        fDRaltalt = 0;
        fDRalt = 0;
        Tp = Tp0;
        tLokal = 0;
        neuePulsperiodeBeginnt = true;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        double ur = _inputSignal[1][0], us = _inputSignal[2][0], ut = _inputSignal[3][0];  // Eingangs- bzw. Netzseitig 
        double uNmax = _inputSignal[4][0], uOUTmax = _inputSignal[5][0], fOUT = _inputSignal[6][0];  // Amplituden und Ausgangsfrequenz  
        double fDR = _inputSignal[0][0];  // Taktfrequenz fuer die Pulsperiode 
        double phi2 = _inputSignal[7][0];  // output-side angle for creating uaOUT*, ubOUT*, ucOUT* for PMSM-control; reliable alternative to fOUT 
        if ((fDRaltalt < fDRalt) && (fDRalt > fDR)) {
            neuePulsperiodeBeginnt = true;
        }
        fDRaltalt = fDRalt;
        fDRalt = fDR;
        //-------------
        if (neuePulsperiodeBeginnt) {
            if (tLokal != 0) {
                Tp = tLokal;
            }
            tLokal = 0;
            sectorDetection(ur, us, ut, fOUT, phi2);  // Sektorindizes seIN, seOUT werden bestimmt 
            calculateSwitchingTimes(ur, us, ut, uNmax, uOUTmax, fOUT, phi2);  // Einschaltdauern dOUT=[d1..d5] und dIN=[da,db] werden berechnet 
            neuePulsperiodeBeginnt = false;
        }
        setPulseWidths(dOUT[0], dOUT[1], dOUT[2], dOUT[3], dOUT[4], dIN[0], dIN[1], (1.0 / Tp));  // alle 9 Schaltsignale werden generiert 
        tLokal += deltaT;

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
        int LG = 1000;  // maximale zeitliche Aufloesung innerhalb der Pulsperiode 
        int y1, y2, y3, y4, y5, y6, y7, y8, y9, dh, x1, x2, xm, dxh;
        int x1a = -1, x1b = -1, x1c, x1d, x2a = -1, x2b = -1, x2c, x2d, x3a = -1, x3b = -1, x3c, x3d, x4a = -1, x4b = -1, x4c, x4d, x5a = -1, x5b = -1, x5c, x5d;
        int x6a = -1, x6b = -1, x6c, x6d, x7a = -1, x7b = -1, x7c, x7d, x8a = -1, x8b = -1, x8c, x8d, x9a = -1, x9b = -1, x9c, x9d;
        int d = 12;
        x1 = 0;
        x2 = LG;
        xm = (x2 + x1) / 2;
        dxh = xm - x1;
        y1 = 0 + d;
        y2 = y1 + d;
        y3 = y2 + d;
        y4 = y3 + d;
        y5 = y4 + d;
        y6 = y5 + d;
        y7 = y6 + 2 * d;
        y8 = y7 + d;
        y9 = y8 + d;
        double xLokal = LG * fDR * tLokal;
        switch (seIN) {
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
        if (seIN == 1) {
            sRp = 1;
            sSp = 0;
            sTp = 0;
            sRm = 0;
            if (xLokal < x5a) {
                sSm = 0;
            } else if (xLokal < x5d) {
                sSm = 1;
            } else {
                sSm = 0;
            }
            if (xLokal < x6b) {
                sTm = 1;
            } else if (xLokal < x6c) {
                sTm = 0;
            } else {
                sTm = 1;
            }
        } else if (seIN == 2) {
            if (xLokal < x1b) {
                sRp = 1;
            } else if (xLokal < x1c) {
                sRp = 0;
            } else {
                sRp = 1;
            }
            if (xLokal < x2a) {
                sSp = 0;
            } else if (xLokal < x2d) {
                sSp = 1;
            } else {
                sSp = 0;
            }
            sTp = 0;
            sRm = 0;
            sSm = 0;
            sTm = 1;
        } else if (seIN == 3) {
            if (xLokal < x1a) {
                sRp = 0;
            } else if (xLokal < x1d) {
                sRp = 1;
            } else {
                sRp = 0;
            }
            if (xLokal < x2b) {
                sSp = 1;
            } else if (xLokal < x2c) {
                sSp = 0;
            } else {
                sSp = 1;
            }
            sTp = 0;
            sRm = 0;
            sSm = 0;
            sTm = 1;
        } else if (seIN == 4) {
            sRp = 0;
            sSp = 1;
            sTp = 0;
            if (xLokal < x4a) {
                sRm = 0;
            } else if (xLokal < x4d) {
                sRm = 1;
            } else {
                sRm = 0;
            }
            sSm = 0;
            if (xLokal < x6b) {
                sTm = 1;
            } else if (xLokal < x6c) {
                sTm = 0;
            } else {
                sTm = 1;
            }
        } else if (seIN == 5) {
            sRp = 0;
            sSp = 1;
            sTp = 0;
            if (xLokal < x4b) {
                sRm = 1;
            } else if (xLokal < x4c) {
                sRm = 0;
            } else {
                sRm = 1;
            }
            sSm = 0;
            if (xLokal < x6a) {
                sTm = 0;
            } else if (xLokal < x6d) {
                sTm = 1;
            } else {
                sTm = 0;
            }
        } else if (seIN == 6) {
            sRp = 0;
            if (xLokal < x2b) {
                sSp = 1;
            } else if (xLokal < x2c) {
                sSp = 0;
            } else {
                sSp = 1;
            }
            if (xLokal < x3a) {
                sTp = 0;
            } else if (xLokal < x3d) {
                sTp = 1;
            } else {
                sTp = 0;
            }
            sRm = 1;
            sSm = 0;
            sTm = 0;
        } else if (seIN == 7) {
            sRp = 0;
            if (xLokal < x2a) {
                sSp = 0;
            } else if (xLokal < x2d) {
                sSp = 1;
            } else {
                sSp = 0;
            }
            if (xLokal < x3b) {
                sTp = 1;
            } else if (xLokal < x3c) {
                sTp = 0;
            } else {
                sTp = 1;
            }
            sRm = 1;
            sSm = 0;
            sTm = 0;
        } else if (seIN == 8) {
            sRp = 0;
            sSp = 0;
            sTp = 1;
            if (xLokal < x4b) {
                sRm = 1;
            } else if (xLokal < x4c) {
                sRm = 0;
            } else {
                sRm = 1;
            }
            if (xLokal < x5a) {
                sSm = 0;
            } else if (xLokal < x5d) {
                sSm = 1;
            } else {
                sSm = 0;
            }
            sTm = 0;
        } else if (seIN == 9) {
            sRp = 0;
            sSp = 0;
            sTp = 1;
            if (xLokal < x4a) {
                sRm = 0;
            } else if (xLokal < x4d) {
                sRm = 1;
            } else {
                sRm = 0;
            }
            if (xLokal < x5b) {
                sSm = 1;
            } else if (xLokal < x5c) {
                sSm = 0;
            } else {
                sSm = 1;
            }
            sTm = 0;
        } else if (seIN == 10) {
            if (xLokal < x1a) {
                sRp = 0;
            } else if (xLokal < x1d) {
                sRp = 1;
            } else {
                sRp = 0;
            }
            sSp = 0;
            if (xLokal < x3b) {
                sTp = 1;
            } else if (xLokal < x3c) {
                sTp = 0;
            } else {
                sTp = 1;
            }
            sRm = 0;
            sSm = 1;
            sTm = 0;
        } else if (seIN == 11) {
            if (xLokal < x1b) {
                sRp = 1;
            } else if (xLokal < x1c) {
                sRp = 0;
            } else {
                sRp = 1;
            }
            sSp = 0;
            if (xLokal < x3a) {
                sTp = 0;
            } else if (xLokal < x3d) {
                sTp = 1;
            } else {
                sTp = 0;
            }
            sRm = 0;
            sSm = 1;
            sTm = 0;
        } else if (seIN == 12) {
            sRp = 1;
            sSp = 0;
            sTp = 0;
            sRm = 0;
            if (xLokal < x5b) {
                sSm = 1;
            } else if (xLokal < x5c) {
                sSm = 0;
            } else {
                sSm = 1;
            }
            if (xLokal < x6a) {
                sTm = 0;
            } else if (xLokal < x6d) {
                sTm = 1;
            } else {
                sTm = 0;
            }
        }
        switch (seOUT) {
            case 1:  //inverseKurve=false;
                x7a = x1;
                x7b = xm;
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 2:  //inverseKurve=true;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = -1;
                x9b = -1;
                break;
            case 3:  //inverseKurve=true;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = -1;
                x9b = -1;
                break;
            case 4:  //inverseKurve=false;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = x1;
                x8b = xm;
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 5:  //inverseKurve=false;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = x1;
                x8b = xm;
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            case 6:  //inverseKurve=true;
                x7a = -1;
                x7b = -1;
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            case 7:  //inverseKurve=true;
                x7a = -1;
                x7b = -1;
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 8:  //inverseKurve=false;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = x1 + (int) (d1 * dxh);
                x8b = x1 + (int) ((1 - d5) * dxh);
                x9a = x1;
                x9b = xm;
                break;
            case 9:  //inverseKurve=false;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = x1 + (int) ((d1 + d2) * dxh);
                x8b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x9a = x1;
                x9b = xm;
                break;
            case 10:  //inverseKurve=true;
                x7a = x1 + (int) (d1 * dxh);
                x7b = x1 + (int) ((1 - d5) * dxh);
                x8a = -1;
                x8b = -1;
                x9a = x1 + (int) ((d1 + d2) * dxh);
                x9b = x1 + (int) ((d1 + d2 + d3) * dxh);
                break;
            case 11:  //inverseKurve=true;
                x7a = x1 + (int) ((d1 + d2) * dxh);
                x7b = x1 + (int) ((d1 + d2 + d3) * dxh);
                x8a = -1;
                x8b = -1;
                x9a = x1 + (int) (d1 * dxh);
                x9b = x1 + (int) ((1 - d5) * dxh);
                break;
            case 12:  //inverseKurve=false;
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
        if ((seOUT == 1) || (seOUT == 12)) {
            s1 = 1;
            if (xLokal < x8a) {
                s2 = 0;
            } else if (xLokal < x8b) {
                s2 = 1;
            } else if (xLokal < x8c) {
                s2 = 0;
            } else if (xLokal < x8d) {
                s2 = 1;
            } else {
                s2 = 0;
            }
            if (xLokal < x9a) {
                s3 = 0;
            } else if (xLokal < x9b) {
                s3 = 1;
            } else if (xLokal < x9c) {
                s3 = 0;
            } else if (xLokal < x9d) {
                s3 = 1;
            } else {
                s3 = 0;
            }
        } else if ((seOUT == 2) || (seOUT == 3)) {
            if (xLokal < x7a) {
                s1 = 1;
            } else if (xLokal < x7b) {
                s1 = 0;
            } else if (xLokal < x7c) {
                s1 = 1;
            } else if (xLokal < x7d) {
                s1 = 0;
            } else {
                s1 = 1;
            }
            if (xLokal < x8a) {
                s2 = 1;
            } else if (xLokal < x8b) {
                s2 = 0;
            } else if (xLokal < x8c) {
                s2 = 1;
            } else if (xLokal < x8d) {
                s2 = 0;
            } else {
                s2 = 1;
            }
            s3 = 0;
        } else if ((seOUT == 4) || (seOUT == 5)) {
            if (xLokal < x7a) {
                s1 = 0;
            } else if (xLokal < x7b) {
                s1 = 1;
            } else if (xLokal < x7c) {
                s1 = 0;
            } else if (xLokal < x7d) {
                s1 = 1;
            } else {
                s1 = 0;
            }
            s2 = 1;
            if (xLokal < x9a) {
                s3 = 0;
            } else if (xLokal < x9b) {
                s3 = 1;
            } else if (xLokal < x9c) {
                s3 = 0;
            } else if (xLokal < x9d) {
                s3 = 1;
            } else {
                s3 = 0;
            }
        } else if ((seOUT == 6) || (seOUT == 7)) {
            s1 = 0;
            if (xLokal < x8a) {
                s2 = 1;
            } else if (xLokal < x8b) {
                s2 = 0;
            } else if (xLokal < x8c) {
                s2 = 1;
            } else if (xLokal < x8d) {
                s2 = 0;
            } else {
                s2 = 1;
            }
            if (xLokal < x9a) {
                s3 = 1;
            } else if (xLokal < x9b) {
                s3 = 0;
            } else if (xLokal < x9c) {
                s3 = 1;
            } else if (xLokal < x9d) {
                s3 = 0;
            } else {
                s3 = 1;
            }
        } else if ((seOUT == 8) || (seOUT == 9)) {
            if (xLokal < x7a) {
                s1 = 0;
            } else if (xLokal < x7b) {
                s1 = 1;
            } else if (xLokal < x7c) {
                s1 = 0;
            } else if (xLokal < x7d) {
                s1 = 1;
            } else {
                s1 = 0;
            }
            if (xLokal < x8a) {
                s2 = 0;
            } else if (xLokal < x8b) {
                s2 = 1;
            } else if (xLokal < x8c) {
                s2 = 0;
            } else if (xLokal < x8d) {
                s2 = 1;
            } else {
                s2 = 0;
            }
            s3 = 1;
        } else if ((seOUT == 10) || (seOUT == 11)) {
            if (xLokal < x7a) {
                s1 = 1;
            } else if (xLokal < x7b) {
                s1 = 0;
            } else if (xLokal < x7c) {
                s1 = 1;
            } else if (xLokal < x7d) {
                s1 = 0;
            } else {
                s1 = 1;
            }
            s2 = 0;
            if (xLokal < x9a) {
                s3 = 1;
            } else if (xLokal < x9b) {
                s3 = 0;
            } else if (xLokal < x9c) {
                s3 = 1;
            } else if (xLokal < x9d) {
                s3 = 0;
            } else {
                s3 = 1;
            }
        }
    }

    private void calculateSwitchingTimes(double ur, double us, double ut, double uNmax, double uOUTmax, double fOUT, double phi2) {
        switch (seIN) {
            case 1:
                dIN[0] = -ut / ur;
                break;
            case 2:
                dIN[0] = -ur / ut;
                break;
            case 3:
                dIN[0] = -us / ut;
                break;
            case 4:
                dIN[0] = -ut / us;
                break;
            case 5:
                dIN[0] = -ur / us;
                break;
            case 6:
                dIN[0] = -us / ur;
                break;
            case 7:
                dIN[0] = -ut / ur;
                break;
            case 8:
                dIN[0] = -ur / ut;
                break;
            case 9:
                dIN[0] = -us / ut;
                break;
            case 10:
                dIN[0] = -ut / us;
                break;
            case 11:
                dIN[0] = -ur / us;
                break;
            case 12:
                dIN[0] = -us / ur;
                break;
            default:
                break;
        }
        dIN[1] = 1 - dIN[0];

        // Ausgang:
        double k = 1 / Math.sqrt(3) * uOUTmax / (uNmax * uNmax);  // Ann.: Ideales 3-ph. Spannungsnetz am Eingang
        double phiOUT = 2 * Math.PI * fOUT * _time - Math.PI / 2;  // old version, does not work for PMSM-control 
        if (fOUT <= 0) {
            phiOUT = phi2 - Math.PI / 2;  // phiOUT= thetaEl +dPhiEl -Math.PI/2;  --> improvement EPE 2009
        }
        while (phiOUT >= Math.PI / 3) {
            phiOUT -= Math.PI / 3;
        }
        double ua = k * Math.cos(phiOUT + Math.PI / 6);
        double ub = k * Math.sin(phiOUT);
        double x1 = 0, x2 = 0;
        switch (seIN) {
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
                break;
        }
        switch (seOUT) {
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
    }

    private void sectorDetection(double ur, double us, double ut, double fOUT, double phi2) {
        // Sektor der Eingangsspannungen:
        if ((us <= 0) && (ut <= us)) {
            seIN = 1;
        } else if ((us >= 0) && (ur >= us)) {
            seIN = 2;
        } else if ((ur >= 0) && (us >= ur)) {
            seIN = 3;
        } else if ((ur <= 0) && (ut <= ur)) {
            seIN = 4;
        } else if ((ut <= 0) && (ur <= ut)) {
            seIN = 5;
        } else if ((ut >= 0) && (us >= ut)) {
            seIN = 6;
        } else if ((us >= 0) && (ut >= us)) {
            seIN = 7;
        } else if ((us <= 0) && (ur <= us)) {
            seIN = 8;
        } else if ((ur <= 0) && (us <= ur)) {
            seIN = 9;
        } else if ((ur >= 0) && (ut >= ur)) {
            seIN = 10;
        } else if ((ut >= 0) && (ur >= ut)) {
            seIN = 11;
        } else if ((ut <= 0) && (us <= ut)) {
            seIN = 12;
        }
        // Sektor der Ausgangsspannungen:
        double phiOUT = 2 * Math.PI * fOUT * _time;  // old version, does not work for PMSM-control 
        if (fOUT <= 0) {
            phiOUT = phi2;  // phiOUT= thetaEl +dPhiEl; --> improvement EPE 2009
        }
        double u1 = Math.sin(phiOUT);
        double u2 = Math.sin(phiOUT - 2 * Math.PI / 3);
        double u3 = Math.sin(phiOUT - 4 * Math.PI / 3);
        if ((u2 < 0) && (u3 < u2)) {
            seOUT = 1;
        } else if ((u2 > 0) && (u1 > u2)) {
            seOUT = 2;
        } else if ((u1 > 0) && (u2 > u1)) {
            seOUT = 3;
        } else if ((u1 < 0) && (u3 < u1)) {
            seOUT = 4;
        } else if ((u3 < 0) && (u1 < u3)) {
            seOUT = 5;
        } else if ((u3 > 0) && (u2 > u3)) {
            seOUT = 6;
        } else if ((u2 > 0) && (u3 > u2)) {
            seOUT = 7;
        } else if ((u2 < 0) && (u1 < u2)) {
            seOUT = 8;
        } else if ((u1 < 0) && (u2 < u1)) {
            seOUT = 9;
        } else if ((u1 > 0) && (u3 > u1)) {
            seOUT = 10;
        } else if ((u3 > 0) && (u1 > u3)) {
            seOUT = 11;
        } else if ((u3 < 0) && (u2 < u3)) {
            seOUT = 12;
        }
    }
}