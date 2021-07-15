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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.math.Matrix;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractSwitch;
import ch.technokrat.gecko.geckocircuits.allg.SolverType;

import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractResistor;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp.LK_LKOP2;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp.REL_RELUCTANCE;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.DiodeCharacteristic;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SourceType;
import java.text.NumberFormat;

public class LKMatrices {

    private static final double FAST_NULL_R = 1e-9;
    private static final double FAST_NULL_L = 1e-12;
    public int matrixSize;  // Matrix-Ordnung;
    public double[][] a;
    public double[] bVector, p, pALT, pALTALT, pALTALTALT;  // b ... Stoervektor, p und pALT ... Knotenpotentiale PLUS SpgQuellen-Stroeme
    public double[] iALT, iALTALT, iALTALTALT;  // Stroeme in allen Bauteilen (inklusive SpgQuellen-Stroeme)
    private NetListLK netzliste;
    private int elementANZAHL;
    // zur Implementierung der magnetischen Kopplungen -->
    public double[][] zuLKOP2gehoerigeM_spgQnr, zuLKOP2gehoerigeM_kWerte;
    private SolverType _solverType;

    public LKMatrices(final SolverType solverType) {
        _solverType = solverType;
    }

    public void initMatrizen(NetListLK netzliste, boolean typLK, final SolverType solverType) {
        this.initMatrizen(netzliste, false, typLK, solverType);
    }

    public void initMatrizen(NetListLK netzliste, boolean getAnfangsbedVomDialogfenster, boolean typLK,
            final SolverType solverType) {
        double[][][] kop2 = null;

        zuLKOP2gehoerigeM_spgQnr = null;
        zuLKOP2gehoerigeM_kWerte = null;
        // nur bei LK sind magnetische Kopplungen moeglich, bei THERM nicht vorhanden -->
        if (typLK) {
            kop2 = netzliste.getAlleKopplungenM();
            zuLKOP2gehoerigeM_spgQnr = kop2[0];
            zuLKOP2gehoerigeM_kWerte = kop2[1];
        }
        this.netzliste = netzliste;

        this.elementANZAHL = netzliste.getElementANZAHLinklusiveSubcircuit();

        matrixSize = netzliste.knotenMAX + netzliste.spgQuelleMAX + 1;  // 'plus Eins' weil Null-Potential (Bezug fuer alle Knoten) vorhanden
        //System.out.println("netzliste.knotenMAX= "+netzliste.knotenMAX+"\tnetzliste.spgQuelleMAX= "+netzliste.spgQuelleMAX);
        a = new double[matrixSize][matrixSize];
        bVector = new double[matrixSize];
        p = new double[matrixSize];
        pALT = new double[matrixSize];
        pALTALT = new double[matrixSize];
        pALTALTALT = new double[matrixSize];

        iALT = new double[elementANZAHL];
        iALTALT = new double[elementANZAHL];
        iALTALTALT = new double[elementANZAHL];

        //------------------------------------
        try {
            this.setzeAnfangsbedingungen(getAnfangsbedVomDialogfenster, solverType);  // pALT und iALT werden gesetzt, zB. u(0) bei C oder i(0) bei L
        } catch (java.lang.RuntimeException ex) {
            // Calculate difference in days      
            ex.printStackTrace();
            throw new RuntimeException("The following error occured at the circuit initialization:\n"
                    + ex.getMessage()
                    + "\nPlease check your circuit model for nonphysical conditions.");
        }
    }

    // Setzen der Anfangsbedingungen in 'setzeAnfangsbedingungen()' -->
    // wird nur von innerhalb dieser Klassse aufgerufen
    //
    private void initMatrizen(NetListLK netzliste) {
        this.netzliste = netzliste;
        this.elementANZAHL = netzliste.getElementANZAHLinklusiveSubcircuit();
        //------------------------------------
        // netzliste.knotenMAX ... Zahl der Knoten minus Einer (eben der 'Ground'-Knoten) --> daher '+1' in der Bestimmungsgleichung fuer k -->
        // Ordnung der Matrix a[][] ist  gesamtknotenzahl minus ground plus alleSpgQuellen plus alleLKOP2elemente -->
        matrixSize = netzliste.knotenMAX + netzliste.spgQuelleMAX + 1;  // 'plus Eins' weil Null-Potential (Bezug fuer alle Knoten) vorhanden
        a = new double[matrixSize][matrixSize];
        bVector = new double[matrixSize];
        p = new double[matrixSize];
        pALT = new double[matrixSize];
        pALTALT = new double[matrixSize];
        pALTALTALT = new double[matrixSize];

        iALT = new double[elementANZAHL];
        iALTALT = new double[elementANZAHL];
        iALTALTALT = new double[elementANZAHL];

    }

    public void schreibeMatrix_A(double dt, double time, boolean capError) {

        if (netzliste.elements != null) {
            for (int i1 = 0; i1 < matrixSize; i1++) {
                for (int i2 = 0; i2 < matrixSize; i2++) {
                    a[i1][i2] = 0;
                }
            }
        }

        for (int index : netzliste._singularityEntries) {
            a[index][index] = 1;
        }

        double aW = -1;
        for (int i1 = 0; i1 < elementANZAHL; i1++) {

            int x = netzliste.knotenX[i1];
            int y = netzliste.knotenY[i1];
            int z = netzliste.knotenMAX + netzliste.spgQuelleNr[i1];

            //System.out.println("xyz: " + i1 + " " + x + " " + y + " " + z);
            switch (netzliste.typ[i1]) {
                case REL_RELUCTANCE:
                case LK_R:
                case TH_RTH:
                    if (netzliste.parameter[i1][0] < FAST_NULL_R) {
                        aW = 1.0 / FAST_NULL_R;  // falls R==0
                    } else {
                        aW = 1.0 / netzliste.parameter[i1][0];  //  +1/R
                    }
                    a[x][x] += (+aW);
                    a[y][y] += (+aW);
                    a[x][y] += (-aW);
                    a[y][x] += (-aW);

                    break;
                case TH_AMBIENT:
                case LK_S:  // verhaelt sich exakt wie ein hoch- bzw. niederohmiger Widerstand
                case LK_MOSFET:
                    if (netzliste.parameter[i1][0] < FAST_NULL_R) {
                        aW = 1.0 / FAST_NULL_R;  // falls R==0
                    } else {
                        aW = 1.0 / netzliste.parameter[i1][0];  //  +1/R
                    }

                    a[x][x] += (+aW);
                    a[y][y] += (+aW);
                    a[x][y] += (-aW);
                    a[y][x] += (-aW);

                    break;
                case LK_L:                    
                case NONLIN_REL:
                    aW = getAWForInductance(netzliste.parameter[i1][0], netzliste.parameter[i1], dt);
                    netzliste.parameter[i1][11] = aW;
                    a[x][x] += (+aW);
                    a[y][y] += (+aW);
                    a[x][y] += (-aW);
                    a[y][x] += (-aW);
                    break;
                case LK_LKOP2:
                    a[x][z] += (+1.0);
                    a[y][z] += (-1.0);
                    a[z][x] += (+1.0);
                    a[z][y] += (-1.0);
                    //aW = -netzliste.parameter[i1][0] / dt;  //  +L/dt
                    final double inductanceInAMatrix = netzliste.parameter[i1][0];
                    netzliste.parameter[i1][10] = inductanceInAMatrix;
                    if (_solverType == SolverType.SOLVER_BE) {
                        aW = -inductanceInAMatrix / dt;  //  +L/dt
                    } else if (_solverType == SolverType.SOLVER_TRZ) {
                        aW = -2 * inductanceInAMatrix / dt;  //  +2L/dt
                    } else if (_solverType == SolverType.SOLVER_GS) {
                        aW = -1.5 * inductanceInAMatrix / dt;
                    }

                    a[z][z] += (+aW);

                    int anzahlKoppelPartner = 0;
                    if ((zuLKOP2gehoerigeM_spgQnr != null) && (zuLKOP2gehoerigeM_spgQnr[i1] != null)) {
                        anzahlKoppelPartner = zuLKOP2gehoerigeM_spgQnr[i1].length;
                        for (int i7 = 0; i7 < anzahlKoppelPartner; i7++) {
                            if (_solverType == SolverType.SOLVER_BE) {
                                a[z][netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]] += (-zuLKOP2gehoerigeM_kWerte[i1][i7] / dt);  //  +M/dt
                            } else if (_solverType == SolverType.SOLVER_TRZ) {
                                a[z][netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]] += (-2 * zuLKOP2gehoerigeM_kWerte[i1][i7] / dt);  //  +2M/dt
                            } else if (_solverType == SolverType.SOLVER_GS) {
                                a[z][netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]] += (-1.5 * zuLKOP2gehoerigeM_kWerte[i1][i7] / dt); //+1.5M/dt
                            }
                        }
                    }
                    break;
                case TH_CTH:
                    // bug fix after introducing nonlinear capacitors.
                    // for thermal capacitance, set the parameters correctly.
                    netzliste.parameter[i1][6] = netzliste.parameter[i1][0];
                    netzliste.parameter[i1][7] = netzliste.parameter[i1][0];
                case LK_C:
                    // aW = netzliste.parameter[i1][0] / dt;  //  +C/dt
                    //aW = 2 * netzliste.parameter[i1][6] / dt;  //  +C/dt
                    if (_solverType == SolverType.SOLVER_BE) {
                        aW = netzliste.parameter[i1][6] / dt;  //  +C/dt
                    } else if (_solverType == SolverType.SOLVER_TRZ) {
                        aW = 2 * netzliste.parameter[i1][6] / dt;
                    } else if (_solverType == SolverType.SOLVER_GS) {
                        aW = 1.5 * netzliste.parameter[i1][6] / dt;
                    }
                    a[x][x] += (+aW);
                    a[y][y] += (+aW);
                    a[x][y] += (-aW);
                    a[y][x] += (-aW);
                    break;
                case LK_IGBT:
                case LK_D:
                case LK_THYR:
                    aW = 1.0 / netzliste.parameter[i1][0];  //  +1/r
                    a[x][x] += (+aW);
                    a[y][y] += (+aW);
                    a[x][y] += (-aW);
                    a[y][x] += (-aW);
                    break;
                case LK_I:
                    if ((int) netzliste.parameter[i1][0] == SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW
                            || (int) netzliste.parameter[i1][0] == SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY) {
                        double gain = netzliste.parameter[i1][11];
                        int[][] nodePairDVC = netzliste.nodePairDirVoltContSrc;
                        int x1 = nodePairDVC[i1][0], y1 = nodePairDVC[i1][1];
                        a[x][x1] += gain;
                        a[y][y1] += gain;
                        a[x][y1] -= gain;
                        a[y][x1] -= gain;
                    }
                case TH_FLOW:
                    // kein Beitrag
                    break;
                case LK_U:
                case REL_MMF:
                case TH_TEMP:  //  +1                    
                    a[x][z] += (+1.0);
                    a[y][z] += (-1.0);
                    if (!((int) netzliste.parameter[i1][0] == SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW)
                            && !((int) netzliste.parameter[i1][0] == SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER)) {
                        a[z][x] += (+1.0);
                        a[z][y] += (-1.0);
                    }

                    try {
                        switch ((int) netzliste.parameter[i1][0]) {
                            case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                            case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:

                                if (netzliste.parameter[i1][14] == 0) {
                                    // voltage is function of node-potentials and, therefore, calculated here in matrix A
                                    double gain = netzliste.parameter[i1][11];
                                    int[][] nodePairDVC = netzliste.nodePairDirVoltContSrc;
                                    int x1 = nodePairDVC[i1][0], y1 = nodePairDVC[i1][1];
                                    a[z][x1] += (-gain);
                                    a[z][y1] += (+gain);
                                }

                                break;
                            case SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW:
                            case SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER:
                                double gain2 = netzliste.parameter[i1][11];
                                a[z][z] = 1.0 / gain2;
                                a[z][z - 1] = 1;
                                break;
                            case SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW:
                            case SourceType.QUELLE_DIDTCURRENTCONTROLLED:
                                double gain3 = netzliste.parameter[i1][11];
                                a[z][z + 1] += gain3 / dt;
                                break;
                            case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW:
                            case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY:
                                double gain4 = netzliste.parameter[i1][11];
                                a[z][z - 1] -= gain4;
                                break;
                        }

                    } catch (NullPointerException npe) {
                    }  // at initialization nodePairDirectVoltageControlledSource[][] is not defined --> all according values set to zero
                    break;
                case LK_M:
                    // wird in LK_LKOP2 abgehandelt
                    System.out.println("Fehler a0'wetj2423443");
                    break;

                case LK_TERMINAL:
                case TH_TERMINAL:
                case REL_TERMINAL:
                case LK_GLOBAL_TERMINAL:
                case TH_GLOBAL_TERMINAL:
                case REL_GLOBAL_TERMINAL:
                    break;
                default:
                    System.out.println("Fehler : Bauteil nicht definiert!");
                    break;
            }

        }

//        if(a.length > 2) {
//            System.out.println("printing a matrix: ");
//            for(int i = 0; i < a.length; i++) {
//                for(int j = 0; j < a.length;j++) {
//                    System.out.print(a[i][j] + "\t");
//                }
//                System.out.println("");
//            }
//                
//        }
//        if(a.length > 1) {
//        long hashValue = -7;
//            for (int i = 0; i < a.length; i++) {
//                for (int j = 0; j < a[0].length; j++) {
//                    System.out.print(a[i][j] + " ");
//                    hashValue += (i + 7) * (j + 13) * Double.doubleToLongBits(a[i][j]);
//                }
//                System.out.println("");
//            }
//            System.out.println("hash:  " + hashValue);
//        }
//        if (a.length > 3 && time > 0) {
////            System.out.println("a length:  " + a.length);
////            for (int k = 1; k < a.length; k++) {
////                long hash = 1;
////                for (int i = 0; i < k; i++) {
////                    for (int j = 0; j < k; j++) {
////                        hash += Math.abs(1 + i + 3 * j + Double.doubleToRawLongBits(a[i][j]));
////                    }
////                }
////                System.out.println(k + " xxx: " + time + " " + hash);
////            }
////            
////            for(int l = 0; l <= 21; l++) {
////                System.out.print(a[21][l] + " ");
////            }
////            System.out.println("");
////            for(int l = 0; l <= 21; l++) {
////                System.out.print(a[l][21] + " ");
////            }
//
//            System.exit(3);
//        }
    }

    public void schreibeMatrix_B(double dt, double t, boolean capError) {

        for (int i1 = 0; i1 < matrixSize; i1++) {
            bVector[i1] = 0;
        }

        double bW = 0;
        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            int x = netzliste.knotenX[i1];
            int y = netzliste.knotenY[i1];
            int z = netzliste.knotenMAX + netzliste.spgQuelleNr[i1];

            //------------------------------
            switch (netzliste.typ[i1]) {
                case LK_R:
                case REL_RELUCTANCE:
                case TH_RTH:
                case TH_AMBIENT:
                case LK_S:  // verhaelt sich exakt wie ein hoch- bzw. niederohmiger Widerstand
                case LK_MOSFET:
                    // kein Beitrag
                    break;
                case LK_L:
                case NONLIN_REL:
                    if (netzliste.parameter[i1][0] < FAST_NULL_L) {
                        if (_solverType == SolverType.SOLVER_BE) {
                            bW = -iALT[i1];
                        } else if (_solverType == SolverType.SOLVER_TRZ) {
                            bW = -iALT[i1] - dt * (pALT[x] - pALT[y]) / (2 * FAST_NULL_L);
                        } else if (_solverType == SolverType.SOLVER_GS) {
                            bW = (-4.0 / 3.0) * iALT[i1] + (1.0 / 3.0) * iALTALT[i1];
                        }                        
                    } else {//bW = -iALT[i1] - dt * (pALT[x] - pALT[y]) / (2 * netzliste.parameter[i1][0]);                        
                        if (_solverType == SolverType.SOLVER_BE) {
                            bW = -iALT[i1];
                        } else if (_solverType == SolverType.SOLVER_TRZ) {
                            bW = -iALT[i1] - dt * (pALT[x] - pALT[y]) / (2 * netzliste.parameter[i1][0]);
                        } else if (_solverType == SolverType.SOLVER_GS) {
                            bW = (-4.0 / 3.0) * iALT[i1] + (1.0 / 3.0) * iALTALT[i1];
                        }                        
                    }
                    bVector[x] += (+bW);
                    bVector[y] += (-bW);
                    break;
                case LK_LKOP2:
                    final double inductanceInAMatrix = netzliste.parameter[i1][10];

                    if (_solverType == SolverType.SOLVER_BE) {
                        bW = -iALT[i1] * (inductanceInAMatrix / dt);
                    } else if (_solverType == SolverType.SOLVER_TRZ) {
                        bW = -(pALT[x] - pALT[y]) - iALT[i1] * (2 * inductanceInAMatrix / dt);
                    } else if (_solverType == SolverType.SOLVER_GS) {
                        bW = (-2.0 * iALT[i1] + 0.5 * iALTALT[i1]) * (inductanceInAMatrix / dt);
                    }

                    bVector[z] += (+bW);
                    int anzahlKoppelPartner = 0;
                    if ((zuLKOP2gehoerigeM_spgQnr != null) && (zuLKOP2gehoerigeM_spgQnr[i1] != null)) {
                        anzahlKoppelPartner = zuLKOP2gehoerigeM_spgQnr[i1].length;
                        if (_solverType == SolverType.SOLVER_BE) {
                            for (int i7 = 0; i7 < anzahlKoppelPartner; i7++) {
                                bVector[z] += (-p[netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]] * (zuLKOP2gehoerigeM_kWerte[i1][i7] / dt));  //  +iALT*(M)/dt                                
                                // weil bei Typ.LK_LKOP2 --> i[i1]= p[netzliste.knotenMAX +netzliste.spgQuelleNr[i1]];  (siehe in berechneBauteilStroeme()!)
                            }
                        } else if (_solverType == SolverType.SOLVER_TRZ) {
                            for (int i7 = 0; i7 < anzahlKoppelPartner; i7++) {
                                bVector[z] += (-p[netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]] * (2 * zuLKOP2gehoerigeM_kWerte[i1][i7] / dt));  //  +iALT*(2*M)/dt                                
                            }
                        } else if (_solverType == SolverType.SOLVER_GS) {
                            for (int i7 = 0; i7 < anzahlKoppelPartner; i7++) {
                                bVector[z] += ((-2.0 * p[netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]] + 0.5 * pALTALT[netzliste.knotenMAX + (int) zuLKOP2gehoerigeM_spgQnr[i1][i7]]) * (zuLKOP2gehoerigeM_kWerte[i1][i7] / dt));  //  (+iALT*2 - iALTALT*0.5)*(M/dt)
                            }
                        }
                    }
                    break;

                case TH_CTH:
                    // bug fix after introducing nonlinear capacitors.
                    // for thermal capacitance, set the parameters correctly.
                    netzliste.parameter[i1][6] = netzliste.parameter[i1][0];
                    netzliste.parameter[i1][7] = netzliste.parameter[i1][0];
                case LK_C:
                    double fac = (1 - netzliste.parameter[i1][7] / netzliste.parameter[i1][6]);
                    //bW = (2 * netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + iALT[i1] + fac * netzliste.parameter[i1][10];
                    if (_solverType == SolverType.SOLVER_BE) {
                        bW = (netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + fac * netzliste.parameter[i1][10];
                    } else if (_solverType == SolverType.SOLVER_TRZ) {
                        bW = (2 * netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + iALT[i1] + fac * netzliste.parameter[i1][10];
                    } else if (_solverType == SolverType.SOLVER_GS) {
                        //bW = (1.5 * netzliste.parameter[i1][6] / dt) * ((4/3)*(pALT[x] - pALT[y]) - (1/3)*(pALTALT[x] - pALTALT[y])) + fac * netzliste.parameter[i1][10]; simplifies to:
                        bW = (netzliste.parameter[i1][6] / dt) * (2 * (pALT[x] - pALT[y]) - 0.5 * (pALTALT[x] - pALTALT[y])) + fac * netzliste.parameter[i1][10];
                    }
                    // bW = (netzliste.parameter[i1][0] / dt) * (pALT[x] - pALT[y]);
                    bVector[x] += (+bW);
                    bVector[y] += (-bW);
                    break;
                case LK_IGBT:
                case LK_D:
                case LK_THYR:
                    if (netzliste.parameter[i1][0] < 10000) {
                        bW = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        bVector[x] += (+bW);
                        bVector[y] += (-bW);
                    }
                    break;
                case LK_I:
                case TH_FLOW:
                    switch ((int) netzliste.parameter[i1][0]) {
                        case SourceType.QUELLE_DC_NEW:
                        case SourceType.QUELLE_DC:
                            bW = -netzliste.parameter[i1][1];
                            break;
                        case SourceType.QUELLE_SIGNALGESTEUERT_NEW:
                        case SourceType.QUELLE_SIGNALGESTEUERT:
                            bW = -netzliste.parameter[i1][1];
                            break;
                        case SourceType.QUELLE_SIN_NEW:
                        case SourceType.QUELLE_SIN:
                            bW = -netzliste.parameter[i1][20] * Math.sin(2 * Math.PI * netzliste.parameter[i1][2] * t - Math.toRadians(netzliste.parameter[i1][4])) + netzliste.parameter[i1][3];
                            break;
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                            bW = 0;
                            break;
                        default:
                            System.out.println("Fehler: Strom-Quelle nicht spezifiziert serzw45 w46 " + (int) netzliste.parameter[i1][0]);
                            break;
                    }

                    //
                    //bW= -i[i1];                    
                    bVector[x] += (+bW);
                    bVector[y] += (-bW);

                    break;
                case LK_U:
                case REL_MMF:
                case TH_TEMP:
                    switch ((int) netzliste.parameter[i1][0]) {
                        case SourceType.QUELLE_DC_NEW:
                        case SourceType.QUELLE_DC:
                            bVector[z] += (netzliste.parameter[i1][1]);
                            break;
                        case SourceType.QUELLE_SIGNALGESTEUERT_NEW:
                        case SourceType.QUELLE_SIGNALGESTEUERT:
                            bVector[z] += (netzliste.parameter[i1][1]);
                            break;
                        case SourceType.QUELLE_SIN_NEW:
                        case SourceType.QUELLE_SIN:
                            bVector[z] += (netzliste.parameter[i1][20] * Math.sin(2 * Math.PI * netzliste.parameter[i1][2] * t - Math.toRadians(netzliste.parameter[i1][4])) + netzliste.parameter[i1][3]);
                            break;
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                            if (netzliste.parameter[i1][14] == -1) {
                                bVector[z] += netzliste.parameter[i1][12];
                            }
                            if (netzliste.parameter[i1][14] == 1) {
                                bVector[z] += netzliste.parameter[i1][13];
                            }
                            break;
                        case SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER:
                        case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY:
                            // does not add a voltage --> voltage is function of node-potentials and, therefore, calculated in matrix A
                            //b[z] += (netzliste.parameter[i1][1]);                                                        
                            break;
                        case SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW:
                        case SourceType.QUELLE_DIDTCURRENTCONTROLLED:
                            double gain = netzliste.parameter[i1][11];
                            if (t <= 0) {
                                bVector[z] += 2 * gain * netzliste.parameter[i1][15] / dt;
                            } else {
                                bVector[z] += gain * pALT[z + 1] / dt;
                            }
                            break;
                        default:
                            System.out.println("Fehler: Spannungs-Quelle nicht spezifiziert");
                            break;
                    }
                    break;
                case LK_M:
                    // wird in LK_LKOP2 abgehandelt
                    System.out.println("Fehler 0ierg030303333");
                    break;
                case LK_TERMINAL:
                case TH_TERMINAL:
                case REL_TERMINAL:
                case LK_GLOBAL_TERMINAL:
                case TH_GLOBAL_TERMINAL:
                case REL_GLOBAL_TERMINAL:
                    break;
                default:
                    System.out.println("3 Fehler: Bauteil nicht definiert! " + netzliste.typ[i1]);
                    break;
            }

        }

//        if(t <= 0) {
//            System.out.println("-------------b vector    ...");
//            for(int i = 0; i < bVector.length; i++) {
//                System.out.println(bVector[i]);
//            }
//        }
//        System.out.println("+++++++");
//        for(int i = 0; i < bVector.length; i++) {
//            System.out.println(i + " " + bVector[i]);
//        }
//        System.out.println("-------");
//        
//        long bHash = 0;
//        for(int i = 0; i < bVector.length; i++) {
//            bHash+= Math.abs(Double.doubleToRawLongBits(bVector[i]) + i);
//        }
//        System.out.println(bHash);
//        System.exit(2);
//        
//        if(t == 2e-6) {
//            for(int i = 0; i < bVector.length; i++) {
//                System.out.println(i + " " + bVector[i]);
//            }
//            System.exit(3);
//        }
//        System.out.println("time: " + t + " " + bHash);
    }

    public boolean berechneBauteilStroeme(double stoergroesse, double dt, double t, boolean isNewIteration,
            int errorCounter) {

        boolean einSchrittZurueck = false;

//        if(t <= 0) {
//            System.out.println("ppppppppppppppppppppppp");
//            for(int i = 0; i < p.length; i++) {
//                System.out.println(p[i]);
//            }
//        }
        double acceptanceThreshold = 0;
        if (errorCounter > 300) {
            acceptanceThreshold = 0.1;
            if (errorCounter > 600) {
                acceptanceThreshold = 0.2;
            }
        }

        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            int x = netzliste.knotenX[i1];
            int y = netzliste.knotenY[i1];
            int z = netzliste.knotenMAX + netzliste.spgQuelleNr[i1];
            switch (netzliste.typ[i1]) {
                case REL_RELUCTANCE:
                case LK_R:
                case TH_RTH:
                    if (netzliste.parameter[i1][0] < FAST_NULL_R) {
                        netzliste.eLKneu[i1]._currentInAmps = (p[x] - p[y]) / this.FAST_NULL_R;
                    } else {
                        netzliste.eLKneu[i1]._currentInAmps = (p[x] - p[y]) / netzliste.parameter[i1][0];
                    }
                    break;
                case TH_AMBIENT:
                    if (netzliste.parameter[i1][0] < FAST_NULL_R) {
                        netzliste.eLKneu[i1]._currentInAmps = (p[x] - p[y]) / this.FAST_NULL_R;
                    } else {
                        netzliste.eLKneu[i1]._currentInAmps = (p[x] - p[y]) / netzliste.parameter[i1][0];
                    }
                    break;
                case LK_S:  // verhaelt sich exakt wie ein hoch- bzw. niederohmiger Widerstand
                case LK_MOSFET:
                    if (netzliste.parameter[i1][0] < FAST_NULL_R) {
                        netzliste.eLKneu[i1]._currentInAmps = (p[x] - p[y]) / FAST_NULL_R;
                    } else {
                        netzliste.eLKneu[i1]._currentInAmps = (p[x] - p[y]) / netzliste.parameter[i1][0];
                    }

                    break;
                case LK_L:
                case NONLIN_REL:

                    AbstractNonLinearCircuitComponent element = (AbstractNonLinearCircuitComponent) netzliste.eLKneu[i1];                    
                    if (netzliste.parameter[i1][0] < FAST_NULL_L) {
                        if (_solverType == SolverType.SOLVER_BE) {
                            netzliste.eLKneu[i1]._currentInAmps = +iALT[i1] + dt / FAST_NULL_L * (p[x] - p[y]);                            
                        } else if (_solverType == SolverType.SOLVER_TRZ) {
                            netzliste.eLKneu[i1]._currentInAmps = +iALT[i1] + dt / (2 * FAST_NULL_L) * ((p[x] - p[y]) + (pALT[x] - pALT[y]));
                        } else if (_solverType == SolverType.SOLVER_GS) {
                            netzliste.eLKneu[i1]._currentInAmps = (2.0 / 3.0) * dt / FAST_NULL_L * (p[x] - p[y]) + (4.0 / 3.0) * iALT[i1] - (1.0 / 3.0) * iALTALT[i1];
                        }
                    } else if (_solverType == SolverType.SOLVER_BE) {
                        netzliste.eLKneu[i1]._currentInAmps = +iALT[i1] + dt / netzliste.parameter[i1][0] * (p[x] - p[y]);
                    } else if (_solverType == SolverType.SOLVER_TRZ) {
                        netzliste.eLKneu[i1]._currentInAmps = +iALT[i1] + dt / (2 * netzliste.parameter[i1][0]) * ((p[x] - p[y]) + (pALT[x] - pALT[y]));
                    } else if (_solverType == SolverType.SOLVER_GS) {
                        netzliste.eLKneu[i1]._currentInAmps = (2.0 / 3.0) * dt / netzliste.parameter[i1][0] * (p[x] - p[y]) + (4.0 / 3.0) * iALT[i1] - (1.0 / 3.0) * iALTALT[i1];
                    }

                    if (!isNewIteration && element._isNonlinearForCalculationUsage) {
                        double inductanceInAMatrix = netzliste.parameter[i1][10];
                        double correctionInductance = netzliste.parameter[i1][0];
                        double errorEstimator = Math.abs((correctionInductance - inductanceInAMatrix) / (inductanceInAMatrix + correctionInductance));

                        if (errorEstimator > 0.025) {
                            // AAAA System.out.println("doing correction of inductance point");
                            einSchrittZurueck = true;
                            double oldAW = netzliste.parameter[i1][11];
                            a[x][x] -= oldAW;
                            a[y][y] -= oldAW;
                            a[x][y] += oldAW;
                            a[y][x] += oldAW;
                            double newAW = getAWForInductance(netzliste.parameter[i1][0], netzliste.parameter[i1], dt);
                            a[x][x] += newAW;
                            a[y][y] += newAW;
                            a[x][y] -= newAW;
                            a[y][x] -= newAW;
                            // AAAA System.out.println("new inductance value " + netzliste.parameter[i1][10] + " " + dt / oldAW + " " + dt/newAW);
                            netzliste.parameter[i1][12] = netzliste.parameter[i1][11];
                            netzliste.parameter[i1][11] = newAW;

//                            double correctionCurrent = (netzliste.parameter[i1][10] - inductanceInAMatrix) / netzliste.parameter[i1][10] * (bVector[x] - bVector[y]) / 2;
//                            bVector[x] -= correctionCurrent;
//                            bVector[y] += correctionCurrent;
//                            
//                            //netzliste.eLKneu[i1]._currentLosses *= correctionFactor;
//                            System.out.println("compare " + correctionCurrent + " " + iALT[i1]);
//                            netzliste.eLKneu[i1]._currentLosses += correctionCurrent;
//                            iALT[i1] += correctionCurrent;
//                            iALTALT[i1] += correctionCurrent;                            
//                            iALTALTALT[i1] += correctionCurrent;                            
                        }
                    }

                    break;
                case TH_CTH:
                    netzliste.parameter[i1][6] = netzliste.parameter[i1][0];
                case LK_C:
                    double fac = 1 - netzliste.parameter[i1][7] / netzliste.parameter[i1][6];
                    double nonLinearCorrectionCurrent = -fac * netzliste.parameter[i1][10];
                    if (isNewIteration) {
                        if (_solverType == SolverType.SOLVER_BE) {
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][6] / dt * ((p[x] - p[y]) - (pALT[x] - pALT[y]));
                        } else if (_solverType == SolverType.SOLVER_TRZ) {
                            netzliste.eLKneu[i1]._currentInAmps = 2 * netzliste.parameter[i1][6] / dt * ((p[x] - p[y]) - (pALT[x] - pALT[y])) - iALT[i1];
                        } else if (_solverType == SolverType.SOLVER_GS) {
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][6] / dt * (1.5 * (p[x] - p[y]) - 2 * (pALT[x] - pALT[y]) + 0.5 * (pALTALT[x] - pALTALT[y]));
                        }
                        netzliste.parameter[i1][10] = netzliste.eLKneu[i1]._currentInAmps;
                        netzliste.eLKneu[i1]._currentInAmps += nonLinearCorrectionCurrent;
                    } else {
                        if (_solverType == SolverType.SOLVER_BE) {
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][6] / dt * ((p[x] - p[y]) - (pALT[x] - pALT[y]));
                        } else if (_solverType == SolverType.SOLVER_TRZ) {
                            netzliste.eLKneu[i1]._currentInAmps = 2 * netzliste.parameter[i1][6] / dt * ((p[x] - p[y]) - (pALT[x] - pALT[y])) - iALT[i1];
                        } else if (_solverType == SolverType.SOLVER_GS) {
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][6] / dt * (1.5 * (p[x] - p[y]) - 2 * (pALT[x] - pALT[y]) + 0.5 * (pALTALT[x] - pALTALT[y]));
                        }
                        boolean capCorrection = false;
                        if (netzliste.parameter[i1][2] * (netzliste.eLKneu[i1]._currentInAmps + nonLinearCorrectionCurrent) < 0) {
                            capCorrection = true;
                        }
                        if (Math.abs((netzliste.parameter[i1][6] - netzliste.parameter[i1][7]) / (netzliste.parameter[i1][6] + netzliste.parameter[i1][7])) > 0.1) {
                            capCorrection = true;
                        }

                        if (capCorrection) {
                            einSchrittZurueck = true;
                            double facOld = (1 - netzliste.parameter[i1][7] / netzliste.parameter[i1][6]);
                            double bWOld = 0;
                            if (_solverType == SolverType.SOLVER_BE) {
                                bWOld = (netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + facOld * netzliste.parameter[i1][10];
                            } else if (_solverType == SolverType.SOLVER_TRZ) {
                                bWOld = (2 * netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + iALT[i1] + facOld * netzliste.parameter[i1][10];
                            } else if (_solverType == SolverType.SOLVER_GS) {
                                bWOld = (netzliste.parameter[i1][6] / dt) * (2 * (pALT[x] - pALT[y]) - 0.5 * (pALTALT[x] - pALTALT[y])) + facOld * netzliste.parameter[i1][10];
                            }
                            //double bWOld = (netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + facOld * netzliste.parameter[i1][10];
                            //double aWOld = netzliste.parameter[i1][6] / dt;  //  +C/dt
                            double aWOld = 0;
                            if (_solverType == SolverType.SOLVER_BE) {
                                aWOld = netzliste.parameter[i1][6] / dt;  //  +C/dt
                            } else if (_solverType == SolverType.SOLVER_TRZ) {
                                aWOld = 2 * netzliste.parameter[i1][6] / dt;
                            } else if (_solverType == SolverType.SOLVER_GS) {
                                aWOld = 1.5 * netzliste.parameter[i1][6] / dt;
                            }
                            a[x][x] -= (+aWOld);
                            a[y][y] -= (+aWOld);
                            a[x][y] -= (-aWOld);
                            a[y][x] -= (-aWOld);
                            //------
                            // correction of the capacitance value
                            netzliste.parameter[i1][6] = netzliste.parameter[i1][7];
                            netzliste.parameter[i1][10] = netzliste.eLKneu[i1]._currentInAmps;
                            bVector[x] -= bWOld;
                            bVector[y] += bWOld;
                            double facNew = (1 - netzliste.parameter[i1][7] / netzliste.parameter[i1][6]);
                            double bWNew = 0;
                            //double bWNew = (netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + facNew * netzliste.parameter[i1][10];
                            if (_solverType == SolverType.SOLVER_BE) {
                                bWNew = (netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + facNew * netzliste.parameter[i1][10];
                            } else if (_solverType == SolverType.SOLVER_TRZ) {
                                bWNew = (2 * netzliste.parameter[i1][6] / dt) * (pALT[x] - pALT[y]) + iALT[i1] + facNew * netzliste.parameter[i1][10];
                            } else if (_solverType == SolverType.SOLVER_GS) {
                                bWNew = (netzliste.parameter[i1][6] / dt) * (2 * (pALT[x] - pALT[y]) - 0.5 * (pALTALT[x] - pALTALT[y])) + facNew * netzliste.parameter[i1][10];;
                            }
                            bVector[x] += bWNew;
                            bVector[y] -= bWNew;
                            //double aWNew = netzliste.parameter[i1][6] / dt;  //  +C/dt
                            double aWNew = 0;
                            if (_solverType == SolverType.SOLVER_BE) {
                                aWNew = netzliste.parameter[i1][6] / dt;  //  +C/dt
                            } else if (_solverType == SolverType.SOLVER_TRZ) {
                                aWNew = 2 * netzliste.parameter[i1][6] / dt;
                            } else if (_solverType == SolverType.SOLVER_GS) {
                                aWNew = 1.5 * netzliste.parameter[i1][6] / dt;
                            }
                            a[x][x] += (+aWNew);
                            a[y][y] += (+aWNew);
                            a[x][y] += (-aWNew);
                            a[y][x] += (-aWNew);
                        }
                        netzliste.parameter[i1][10] = netzliste.eLKneu[i1]._currentInAmps;
                        netzliste.eLKneu[i1]._currentInAmps += nonLinearCorrectionCurrent;
                        //System.out.println("" + t + " " + netzliste.parameter[i1][6] + " " + netzliste.parameter[i1][7]);
                    }
                    break;
                case LK_D:

                    final Diode diode = (Diode) netzliste.eLKneu[i1];

                    //--------------------------
                    double rD = netzliste.parameter[i1][0];
                    double uf = netzliste.parameter[i1][1];
                    double diodeVoltage = p[x] - p[y];
                    netzliste.eLKneu[i1]._currentInAmps = ((p[x] - p[y]) - uf) / rD;

                    if (diode._diodeChar != null) {
                        DiodeCharacteristic dc = diode._diodeChar;
                        if (dc.testIfWrongSegment(t, diodeVoltage, stoergroesse, acceptanceThreshold)) {
                            einSchrittZurueck = true;

                            double aALT = 1.0 / netzliste.parameter[i1][0];  // (1/rD)
                            double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];  // (Uf/rD)
                            netzliste.parameter[i1][1] = dc.activeSegment._uF;
                            netzliste.parameter[i1][0] = dc.activeSegment._RDiff;
                            double aNEU = 1.0 / netzliste.parameter[i1][0];
                            double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];

                            //System.out.println("Diode t " + errorCounter + " " +  netzliste.eLKneu[i1].getIDStringDialog() + " " + netzliste.parameter[i1][0] + " " +  netzliste.parameter[i1][2] + "  "+ netzliste.parameter[i1][3] + " " + netzliste.parameter[i1][4]); 
                            // Korrektur Matrix a:
                            a[x][x] += (-aALT + aNEU);
                            a[y][y] += (-aALT + aNEU);
                            a[x][y] += (+aALT - aNEU);
                            a[y][x] += (+aALT - aNEU);
                            // Korrektur Matrix b:
                            bVector[x] += (-bALT + bNEU);
                            bVector[y] += (+bALT - bNEU);

                        }
                    } else {
                        if (((diodeVoltage /*
                                 * + i[i1] * rD
                                 */) < (stoergroesse * uf) + acceptanceThreshold) && (rD < 10000/*
                                 * Typ.rDoffDEFAULT
                                 */)) {  // (uD < uf) und Diode "ON"
                            double aALT = 1.0 / netzliste.parameter[i1][0];  // (1/rD)
                            double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];  // (Uf/rD)
                            netzliste.parameter[i1][0] = netzliste.parameter[i1][3];  // Diode auf "OFF" setzen
                            double aNEU = 1.0 / netzliste.parameter[i1][0];
                            double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                            einSchrittZurueck = true;
//                        for(AbstractCircuitBlockInterface block : netzliste.eLKneu) {
//                            if(block instanceof MOSFET) {
//                                if(((MOSFET) block).getAntiParallelDiode() == netzliste.eLKneu[i1]) {
//                                    System.out.println("mosfet " + block.getStringID());
//                                }
//                            }
//                        }
                            //System.out.println("Diode t " + errorCounter + " " +  netzliste.eLKneu[i1].getIDStringDialog() + " " + netzliste.parameter[i1][0] + " " +  netzliste.parameter[i1][2] + "  "+ netzliste.parameter[i1][3] + " " + netzliste.parameter[i1][4]); 
                            // Korrektur Matrix a:
                            a[x][x] += (-aALT + aNEU);
                            a[y][y] += (-aALT + aNEU);
                            a[x][y] += (+aALT - aNEU);
                            a[y][x] += (+aALT - aNEU);
                            // Korrektur Matrix b:
                            bVector[x] += (-bALT + bNEU);
                            bVector[y] += (+bALT - bNEU);
                        }
                        if ((((diodeVoltage) > (stoergroesse * uf - acceptanceThreshold)) && (rD > 10000/*
                                 * == Typ.rDoffDEFAULT
                                 */))) {  // (uD > uf) und Diode "OFF"

                            double aALT = 1.0 / netzliste.parameter[i1][0];
                            double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                            netzliste.parameter[i1][0] = netzliste.parameter[i1][2];  // Diode auf "ON" setzen
                            double aNEU = 1.0 / netzliste.parameter[i1][0];
                            double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                            einSchrittZurueck = true;
                            // Korrektur Matrix a:
                            a[x][x] += (-aALT + aNEU);
                            a[y][y] += (-aALT + aNEU);
                            a[x][y] += (+aALT - aNEU);
                            a[y][x] += (+aALT - aNEU);
                            // Korrektur Matrix b:
                            bVector[x] += (-bALT + bNEU);
                            bVector[y] += (+bALT - bNEU);
                        }
                    }

//                    if (((p[x] - p[y] /*
//                             * + i[i1] * rD
//                             */) < (stoergroesse * uf) + acceptanceThreshold) && (rD < 10000/*
//                             * Typ.rDoffDEFAULT
//                             */)) {  // (uD < uf) und Diode "ON"
//                        double aALT = 1.0 / netzliste.parameter[i1][0];  // (1/rD)
//                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];  // (Uf/rD)
//                        netzliste.parameter[i1][0] = netzliste.parameter[i1][3];  // Diode auf "OFF" setzen
//                        double aNEU = 1.0 / netzliste.parameter[i1][0];
//                        double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
//                        einSchrittZurueck = true;
////                        for(AbstractCircuitBlockInterface block : netzliste.eLKneu) {
////                            if(block instanceof MOSFET) {
////                                if(((MOSFET) block).getAntiParallelDiode() == netzliste.eLKneu[i1]) {
////                                    System.out.println("mosfet " + block.getStringID());
////                                }
////                            }
////                        }
//                        //System.out.println("Diode t " + errorCounter + " " +  netzliste.eLKneu[i1].getIDStringDialog() + " " + netzliste.parameter[i1][0] + " " +  netzliste.parameter[i1][2] + "  "+ netzliste.parameter[i1][3] + " " + netzliste.parameter[i1][4]); 
//                        // Korrektur Matrix a:
//                        a[x][x] += (-aALT + aNEU);
//                        a[y][y] += (-aALT + aNEU);
//                        a[x][y] += (+aALT - aNEU);
//                        a[y][x] += (+aALT - aNEU);
//                        // Korrektur Matrix b:
//                        bVector[x] += (-bALT + bNEU);
//                        bVector[y] += (+bALT - bNEU);
//                    }
//                    if ((((p[x] - p[y]) > (stoergroesse * uf - acceptanceThreshold)) && (rD > 10000/*
//                             * == Typ.rDoffDEFAULT
//                             */))) {  // (uD > uf) und Diode "OFF"
//
//                        double aALT = 1.0 / netzliste.parameter[i1][0];
//                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
//                        netzliste.parameter[i1][0] = netzliste.parameter[i1][2];  // Diode auf "ON" setzen
//                        double aNEU = 1.0 / netzliste.parameter[i1][0];
//                        double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
//                        einSchrittZurueck = true;
//                        // Korrektur Matrix a:
//                        a[x][x] += (-aALT + aNEU);
//                        a[y][y] += (-aALT + aNEU);
//                        a[x][y] += (+aALT - aNEU);
//                        a[y][x] += (+aALT - aNEU);
//                        // Korrektur Matrix b:
//                        bVector[x] += (-bALT + bNEU);
//                        bVector[y] += (+bALT - bNEU);
//                    }
                    break;
                case LK_THYR:
                    rD = netzliste.parameter[i1][0];
                    uf = netzliste.parameter[i1][1];
                    netzliste.eLKneu[i1]._currentInAmps = ((p[x] - p[y]) - uf) / rD;

                    //--------------------------
                    // Logik fuer THYR-Abfrage:
                    // bei 'stoergroesse=1.0' bleibt die Simulation oft haengen!!
                    if (((p[x] - p[y]) < (stoergroesse * uf + acceptanceThreshold)) && (rD < 0.5 * netzliste.parameter[i1][3])) {  // (uD < uf) und Thyristor "ON"                        
                        double aALT = 1.0 / netzliste.parameter[i1][0];  // (1/rD)
                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];  // (Uf/rD)
                        if (t - netzliste.parameter[i1][11] > 3 * netzliste.parameter[i1][9]) {
                            netzliste.parameter[i1][11] = t;
                        }

                        if (t - netzliste.parameter[i1][11] >= netzliste.parameter[i1][9]) {
                            netzliste.parameter[i1][0] = netzliste.parameter[i1][3];  // Thyristor auf "OFF" setzen                            
                            double aNEU = 1.0 / netzliste.parameter[i1][0];
                            double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                            einSchrittZurueck = true;
                            // Korrektur Matrix a:
                            a[x][x] += (-aALT + aNEU);
                            a[y][y] += (-aALT + aNEU);
                            a[x][y] += (+aALT - aNEU);
                            a[y][x] += (+aALT - aNEU);
                            // Korrektur Matrix b:
                            bVector[x] += (-bALT + bNEU);
                            bVector[y] += (+bALT - bNEU);
                        }
                    }
                    // in 'parameter[8]' wird beim Thyristor das aktuelle Gate-Signal hineingeschrieben (siehe 'Simulationskern.runSimulation()')
                    if ((netzliste.parameter[i1][8] == 1) && (((p[x] - p[y]) > (stoergroesse * uf - acceptanceThreshold)) && (rD == AbstractSwitch.RD_OFF_DEFAULT))) {  // gate==1  und  (uD > uf) und Thyristor "OFF"                        
                        double aALT = 1.0 / netzliste.parameter[i1][0];
                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        netzliste.parameter[i1][0] = netzliste.parameter[i1][2];  // Thyristor auf "ON" setzen
                        double aNEU = 1.0 / netzliste.parameter[i1][0];
                        double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        einSchrittZurueck = true;
                        // Korrektur Matrix a:
                        a[x][x] += (-aALT + aNEU);
                        a[y][y] += (-aALT + aNEU);
                        a[x][y] += (+aALT - aNEU);
                        a[y][x] += (+aALT - aNEU);
                        // Korrektur Matrix b:
                        bVector[x] += (-bALT + bNEU);
                        bVector[y] += (+bALT - bNEU);
                    }

                    break;
                case LK_IGBT:
                    rD = netzliste.parameter[i1][0];
                    uf = netzliste.parameter[i1][1];

                    netzliste.eLKneu[i1]._currentInAmps = ((p[x] - p[y]) - uf) / rD;
                    //--------------------------
                    // Logik fuer IGBT-Abfrage:
                    // rD(t) - uf - rON - rOFF - i(t) - u(t) - xxx - xxx - gateStatusOnOff   --> aehnlich wie THYR
                    // Diode seriell zu Schalter kann nur aktiv werden, wenn das Schalter-Gate 'ON' ist (dh. gateStatusOnOff==1)
                    // Wenn ploetzlich 'gateStatusOnOff==0' gesetzt wird, dann wird der Schalter hochohmig
                    //
                    // ... bei 'stoergroesse=1.0' bleibt die Simulation oft haengen!!
                    if ((((p[x] - p[y] /*
                             * + i[i1] * rD
                             */) < (stoergroesse * uf + acceptanceThreshold)) && (rD < 10000/*
                             * Typ.rDoffDEFAULT
                             */)) && (netzliste.parameter[i1][8] == 1)) {  // (uD < uf) und IGBT-Serien-Diode "ON" und gateStatusOnOff==1
                        double aALT = 1.0 / netzliste.parameter[i1][0];  // (1/rD)
                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];  // (Uf/rD)
                        netzliste.parameter[i1][0] = netzliste.parameter[i1][3];  // --> IGBT auf "OFF" setzen
                        double aNEU = 1.0 / netzliste.parameter[i1][0];
                        double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        einSchrittZurueck = true;
                        // Korrektur Matrix a:
                        a[x][x] += (-aALT + aNEU);
                        a[y][y] += (-aALT + aNEU);
                        a[x][y] += (+aALT - aNEU);
                        a[y][x] += (+aALT - aNEU);
                        // Korrektur Matrix b:
                        bVector[x] += (-bALT + bNEU);
                        bVector[y] += (+bALT - bNEU);
                    }
                    // in 'parameter[8]' wird beim IGBT das aktuelle Gate-Signal hineingeschrieben (siehe 'Simulationskern.runSimulation()')
                    if ((netzliste.parameter[i1][8] == 1) && (((p[x] - p[y]) > (stoergroesse * uf - acceptanceThreshold)) && (rD > 10000/*
                             * == Typ.rDoffDEFAULT
                             */))) {  // gateStatusOnOff==1  und  (uD > uf) und Thyristor "OFF"
                        double aALT = 1.0 / netzliste.parameter[i1][0];
                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        netzliste.parameter[i1][0] = netzliste.parameter[i1][2];  // --> IGBT auf "ON" setzen
                        double aNEU = 1.0 / netzliste.parameter[i1][0];
                        double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        einSchrittZurueck = true;
                        // Korrektur Matrix a:
                        a[x][x] += (-aALT + aNEU);
                        a[y][y] += (-aALT + aNEU);
                        a[x][y] += (+aALT - aNEU);
                        a[y][x] += (+aALT - aNEU);
                        // Korrektur Matrix b:
                        bVector[x] += (-bALT + bNEU);
                        bVector[y] += (+bALT - bNEU);
                    }
                    if ((netzliste.parameter[i1][8] == 0) && (netzliste.parameter[i1][0] == netzliste.parameter[i1][2])) {  // bis jetzt 'ON', aber das Gate wurde soeben auf 'OFF' gesetzt
                        // rD(t) - uf - rON - rOFF - i(t) - u(t) - xxx - xxx - gateStatusOnOff   --> aehnlich wie THYR
                        double aALT = 1.0 / netzliste.parameter[i1][0];
                        double bALT = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        netzliste.parameter[i1][0] = netzliste.parameter[i1][3];  // --> IGBT auf "OFF" setzen
                        double aNEU = 1.0 / netzliste.parameter[i1][0];
                        double bNEU = netzliste.parameter[i1][1] / netzliste.parameter[i1][0];
                        einSchrittZurueck = true;
                        // Korrektur Matrix a:
                        a[x][x] += (-aALT + aNEU);
                        a[y][y] += (-aALT + aNEU);
                        a[x][y] += (+aALT - aNEU);
                        a[y][x] += (+aALT - aNEU);
                        // Korrektur Matrix b:
                        bVector[x] += (-bALT + bNEU);
                        bVector[y] += (+bALT - bNEU);
                    }
                    break;
                case LK_I:
                case TH_FLOW:
                    switch ((int) netzliste.parameter[i1][0]) {
                        case SourceType.QUELLE_DC_NEW:
                        case SourceType.QUELLE_DC:
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][1];
                            break;
                        case SourceType.QUELLE_SIGNALGESTEUERT_NEW:
                        case SourceType.QUELLE_SIGNALGESTEUERT:
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][1];
                            break;
                        case SourceType.QUELLE_SIN_NEW:
                        case SourceType.QUELLE_SIN:
                            netzliste.eLKneu[i1]._currentInAmps = netzliste.parameter[i1][20] * Math.sin(2 * Math.PI * netzliste.parameter[i1][2] * t - Math.toRadians(netzliste.parameter[i1][4])) + netzliste.parameter[i1][3];
                            break;
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                            double gain = netzliste.parameter[i1][11];
                            int[][] nodePairDVC = netzliste.nodePairDirVoltContSrc;
                            int x1 = nodePairDVC[i1][0],
                             y1 = nodePairDVC[i1][1];
                            netzliste.eLKneu[i1]._currentInAmps = 0;//gain * (p[x1] - p[y1]);                            
                            //System.out.println("cur: " + (gain * (p[x1] - p[y1])) + " " + x1 + " " + y1);
                            break;
                        default:
                            System.out.println("Fehler: Strom-Quelle nicht spezifiziert");
                            break;
                    }

                    break;
                case LK_U:
                    if (netzliste.parameter[i1][0] == SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW
                            || netzliste.parameter[i1][0] == SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY && t > 0) {
                        double gain = netzliste.parameter[i1][11];
                        int[][] nodePairDVC = netzliste.nodePairDirVoltContSrc;
                        if (nodePairDVC == null) {
                            continue;
                        }

                        int x1 = nodePairDVC[i1][0];
                        int y1 = nodePairDVC[i1][1];

                        double voltage = gain * (p[x1] - p[y1]);
                        if (netzliste.parameter[i1][14] == 0) { // old state: normal
                            if (voltage < netzliste.parameter[i1][12]) {
                                einSchrittZurueck = true;
                                netzliste.parameter[i1][14] = -1;

                                a[z][x1] -= (-gain);
                                a[z][y1] -= (+gain);

                                bVector[z] += netzliste.parameter[i1][12];
                            }

                            if (voltage > netzliste.parameter[i1][13]) {
                                einSchrittZurueck = true;
                                netzliste.parameter[i1][14] = 1;

                                a[z][x1] -= (-gain);
                                a[z][y1] -= (+gain);
                                bVector[z] += netzliste.parameter[i1][13];
                            }
                        } else {
                            if (netzliste.parameter[i1][14] == -1 && voltage >= netzliste.parameter[i1][12]) {
                                einSchrittZurueck = true;
                                netzliste.parameter[i1][14] = 0;
                                bVector[z] -= netzliste.parameter[i1][12];
                                a[z][x1] += (-gain);
                                a[z][y1] += (+gain);
                            }
                            if (netzliste.parameter[i1][14] == 1 && voltage <= netzliste.parameter[i1][13]) {
                                einSchrittZurueck = true;
                                netzliste.parameter[i1][14] = 0;
                                bVector[z] -= netzliste.parameter[i1][13];
                                a[z][x1] += (-gain);
                                a[z][y1] += (+gain);
                            }
                        }
                    }
                case REL_MMF:
                case TH_TEMP:
                case LK_LKOP2:
                    netzliste.eLKneu[i1]._currentInAmps = p[netzliste.knotenMAX + netzliste.spgQuelleNr[i1]];  // SpgQuellen-Stroeme stehen als Unbekannte ausnahmsweise auch im Knotenpotetial-Vektor                    
                    break;
                case LK_M:
                    // wird in LK_LKOP2 abgehandelt
                    // in diesem 'Element' fliessen keine Stroeme, di/dt-Berechung erfolgt in aktualisiereKnotenpotentiale()
                    break;
                case LK_TERMINAL:
                case TH_TERMINAL:
                case REL_TERMINAL:
                case LK_GLOBAL_TERMINAL:
                case TH_GLOBAL_TERMINAL:
                case REL_GLOBAL_TERMINAL:
                    break;
                default:
                    System.out.println("1 Fehler: Bauteil nicht definiert!");
                    break;
            }
        }

        return einSchrittZurueck;
    }

    public void aktualisiereKnotenpotentiale(double dt, double time) {
        //------------------------------
        // Spannungen aktualisieren:
        //assert false;
        for (int i1 = 0; i1 < matrixSize; i1++) {
            pALTALTALT[i1] = pALTALT[i1];
            pALTALT[i1] = pALT[i1];
            pALT[i1] = p[i1];
        }

        //------------------------------
        // Stroeme aktualisieren:
        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            iALTALTALT[i1] = iALTALT[i1];
            iALTALT[i1] = iALT[i1];
            iALT[i1] = netzliste.eLKneu[i1]._currentInAmps;
        }
        //------------------------------
        // aktuelle Strom- und Spg.-Werte in 'parameter[]' ablegen zwecks einfachem und direktem Zugriff:
        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            int x = netzliste.knotenX[i1];
            int y = netzliste.knotenY[i1];
            netzliste.eLKneu[i1]._voltage = p[x] - p[y];
            switch (netzliste.typ[i1]) {
                case LK_R:
                case REL_RELUCTANCE:
                case TH_RTH:
                case TH_AMBIENT:
                    netzliste.parameter[i1][1] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][2] = p[x] - p[y];
                    break;
                case LK_S:  // verhaelt sich exakt wie ein hoch- bzw. niederohmiger Widerstand
                    netzliste.parameter[i1][3] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][4] = p[x] - p[y];
                    // ACHTUNG: bei Diodenschaltfehlern wird parameter[][3] und parameter[4] mit den aktualisierten korrekten Werten
                    // ueberschrieben --> die Abfrage von parameter[][3] und parameter[][4] zur Schaltverlustberechnung darf daher erst
                    // NACH Abschluss der Diodenzustands-Aktualisierung in der Simulation-Schleife in 'SimulationsKern' erfolgen
                    break;
                case LK_L:
                case NONLIN_REL:
                case LK_LKOP2:
                    // di/dt (fuer induktive Kopplungen M) -->
                    netzliste.parameter[i1][4] = (netzliste.eLKneu[i1]._currentInAmps - netzliste.parameter[i1][2]) / dt;
                    // jetzt i(t) und u(t) aktualisieren -->
                    netzliste.parameter[i1][2] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][3] = p[x] - p[y];
                    break;
                case LK_C:
                case TH_CTH:
                    netzliste.parameter[i1][2] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][3] = p[x] - p[y];
                    netzliste.parameter[i1][4] = p[x];
                    netzliste.parameter[i1][5] = p[y];
                    break;
                case LK_MOSFET:
                case LK_IGBT:
                case LK_D:
                case LK_THYR:
                    netzliste.parameter[i1][4] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][5] = p[x] - p[y];
                    break;
                case LK_I:
                case TH_FLOW:
                    netzliste.parameter[i1][6] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][7] = p[x] - p[y];
                    break;
                case LK_U:
                case REL_MMF:
                case TH_TEMP:
                    netzliste.parameter[i1][6] = netzliste.eLKneu[i1]._currentInAmps;
                    netzliste.parameter[i1][7] = p[x] - p[y];
                    netzliste.parameter[i1][8] = p[x];
                    netzliste.parameter[i1][9] = p[y];
                    break;
                case LK_M:
                    // wird in LK_LKOP2 abgehandelt
                    break;
                case LK_TERMINAL:
                case TH_TERMINAL:
                case REL_TERMINAL:
                case LK_GLOBAL_TERMINAL:
                case TH_GLOBAL_TERMINAL:
                case REL_GLOBAL_TERMINAL:
                    break;
                default:
                    System.out.println("2 Fehler: Bauteil nicht definiert!");
                    break;
            }
        }
    }

    public void schreibeRechendatenNachEinemZeitschritt(double t) {
        //------------------
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(6);
        nf.setMinimumFractionDigits(6);
        String[] txt = new String[elementANZAHL];
        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            int x = netzliste.knotenX[i1];
            int y = netzliste.knotenY[i1];
            txt[i1] = new String(
                    "(id) " + i1 + "\t" + netzliste.eLKneu[i1].getStringID()
                    + "\t(" + netzliste.labelListe[netzliste.knotenX[i1]] + " [" + nf.format(p[x])
                    + "]  -  " + netzliste.labelListe[netzliste.knotenY[i1]] + " [" + nf.format(p[y]) + "]");
        }
        // sortieren:
        String[] txtSub = new String[txt.length];
        for (int i1 = 0; i1 < txt.length; i1++) {
            txtSub[i1] = txt[i1].substring(7).trim();
        }
        boolean flip = true;
        while (flip) {
            flip = false;
            for (int i1 = 1; i1 < txt.length; i1++) {
                if (txtSub[i1 - 1].compareTo(txtSub[i1]) > 0) {
                    String q = txt[i1 - 1];
                    txt[i1 - 1] = txt[i1];
                    txt[i1] = q;
                    String qSub = txtSub[i1 - 1];
                    txtSub[i1 - 1] = txtSub[i1];
                    txtSub[i1] = qSub;
                    flip = true;
                }
            }
        }
        System.out.println("t [us] = " + nf.format(t * 1e6) + "\t---------");
        this.schreibeMatrizenGleichung();
        for (int i1 = 0; i1 < txt.length; i1++) {
            System.out.println(txt[i1]);
        }
    }

    public void schreibeMatrizenGleichung() {
        TechFormat cf = new TechFormat();
        for (int i1 = 0; i1 < a.length; i1++) {
            for (int i2 = 0; i2 < a[0].length; i2++) {
                System.out.print(cf.formatT(a[i1][i2], TechFormat.FORMAT_AUTO) + "\t\t");
            }
            System.out.println("\tB= " + cf.formatT(bVector[i1], TechFormat.FORMAT_AUTO));
        }
    }

    //===================================================================================================
    // i(0)- und u(0)-Werte werden aus netzliste.parameter[][] herausgelesen,
    // dadurch kann 'setzeAnfangsbedingungen()' sowohl fuer die DialogEingabewerte bei t0==0 als auch bei CONTINUE nach PAUSE eingesetzt werden -->
    //
    private void setzeAnfangsbedingungen(boolean getAnfangsbedVomDialogfenster, final SolverType solverType) {
        //
//        for (int i1=0;  i1<pALT.length;  i1++)  pALT[i1]= 0;  // default --> alle Knotenpotentiale auf Null
        //=======================================
        // Anfangsbedingung, im Dialogfenster gesetzt, zB. bei INIT/START -->
        //        
        if ((getAnfangsbedVomDialogfenster) && (matrixSize > 1)) {
            for (int i1 = 0; i1 < pALT.length; i1++) {
                pALT[i1] = 0;  // default --> alle Knotenpotentiale auf Null
                pALTALT[i1] = 0;
                pALTALTALT[i1] = 0;
            }            //
            // hier sind alle C durch Udc ersetzt, und es wird ein Zeitschritt simuliert -->
            // damit stellen sich die richtigen Knotenpotentiale ein, wenn Anfangsbedingungen uC(0) gesetzt sind
            //---------
            // initialisieren:
            LKMatrices lkmInit = new LKMatrices(solverType);

            lkmInit.initMatrizen(NetListLK.ersetze_C_durch_Udc_Fuer_init(netzliste));
            lkmInit.zuLKOP2gehoerigeM_kWerte = this.zuLKOP2gehoerigeM_kWerte;
            lkmInit.zuLKOP2gehoerigeM_spgQnr = this.zuLKOP2gehoerigeM_spgQnr;
            lkmInit.schreibeMatrix_A(1e-9, -1e-9, false);
            // 1. Zeitschritt:
            lkmInit.schreibeMatrix_B(1e-9, -1e-9, false);
            double[] bInit = new double[lkmInit.matrixSize - 1];
            double[] pInit = new double[lkmInit.matrixSize - 1];
            double[][] aInit = new double[lkmInit.matrixSize - 1][lkmInit.matrixSize - 1];
            for (int i1 = 1; i1 < lkmInit.matrixSize; i1++) {
                for (int i2 = 1; i2 < lkmInit.matrixSize; i2++) {
                    aInit[i1 - 1][i2 - 1] = lkmInit.a[i1][i2];
                }
            }
            Matrix aMInit = new Matrix(aInit);
            // Loesen der Matrizen-Gleichungen in der Zeitschleife:
            System.arraycopy(lkmInit.bVector, 1, bInit, 0, lkmInit.matrixSize - 1);
            Matrix bMInit = new Matrix(bInit, bInit.length);
            
             
            Matrix pMInit = aMInit.solve(bMInit);                        
            
            pInit = pMInit.getColumnPackedCopy();

            System.arraycopy(pInit, 0, lkmInit.p, 1, pInit.length);
            lkmInit.p[0] = 0;

            boolean isNewIteration = false;

            double stoergroesse = 0.99999;
            int switchingErrorCounter = 0;
            while (berechneBauteilStroeme(stoergroesse, 1e-9, -1e-9, isNewIteration, 0)) {
                isNewIteration = true;
                if (switchingErrorCounter > 100) {
                    continue;
                }

                //this.ausgebenDiodenzustaende(t);
                if ((switchingErrorCounter++) > 2) {
                    stoergroesse *= 0.99;
                }

                lkmInit = new LKMatrices(solverType);
                lkmInit.initMatrizen(NetListLK.ersetze_C_durch_Udc_Fuer_init(netzliste));
                lkmInit.zuLKOP2gehoerigeM_kWerte = this.zuLKOP2gehoerigeM_kWerte;
                lkmInit.zuLKOP2gehoerigeM_spgQnr = this.zuLKOP2gehoerigeM_spgQnr;
                lkmInit.schreibeMatrix_A(1e-9, -1e-9, false);
                // 1. Zeitschritt:
                lkmInit.schreibeMatrix_B(1e-9, -1e-9, false);
                bInit = new double[lkmInit.matrixSize - 1];
                pInit = new double[lkmInit.matrixSize - 1];
                aInit = new double[lkmInit.matrixSize - 1][lkmInit.matrixSize - 1];
                for (int i1 = 1; i1 < lkmInit.matrixSize; i1++) {
                    for (int i2 = 1; i2 < lkmInit.matrixSize; i2++) {
                        aInit[i1 - 1][i2 - 1] = lkmInit.a[i1][i2];
                    }
                }
                aMInit = new Matrix(aInit);
                // Loesen der Matrizen-Gleichungen in der Zeitschleife:
                System.arraycopy(lkmInit.bVector, 1, bInit, 0, lkmInit.matrixSize - 1);

                bMInit = new Matrix(bInit, bInit.length);
                pMInit = aMInit.solve(bMInit);
                pInit = pMInit.getColumnPackedCopy();

                System.arraycopy(pInit, 0, lkmInit.p, 1, pInit.length);
                lkmInit.p[0] = 0;

            }

            // the following code was originally writte by Andi
            // uwe removed it because it is just a single time-step to check the initial capacitor values
            // very often instabilities occored with this code block (especially OpAmp-implementation)
            // not a clean solution yet but much more stability >> to be improved/fixed ni the future ... 
            // .........
            /*
             * // Diodenschaltfehler? --> int switchingErrorCounter = 0; // 'stoergroesse<1.0' verhindert, dass der Algorithmus
             * beim Dioden-Umschalten zwischen Zustaenden haengen bleibt double stoergroesse = 0.99999; while
             * (lkmInit.berechneBauteilStroeme(stoergroesse, 1e-9, 0, false)) { if (switchingErrorCounter > 1000) { new
             * DialogDiodenError(switchingErrorCounter,t); //System.out.println("schaltfehler ... "); } if
             * ((switchingErrorCounter++) > 2) { stoergroesse *= 0.99; } for (int i1 = 1; i1 < lkmInit.k; i1++) { for (int i2 =
             * 1; i2 < lkmInit.k; i2++) { aInit[i1 - 1][i2 - 1] = lkmInit.a[i1][i2]; } } aMInit = new Matrix(aInit);
             * System.arraycopy(lkmInit.b, 1, bInit, 0, lkmInit.k - 1);
             *
             * bMInit = new Matrix(bInit, bInit.length); pMInit = aMInit.solve(bMInit); pInit = pMInit.getColumnPackedCopy();
             * System.arraycopy(pInit, 0, lkmInit.p, 1, pInit.length); lkmInit.p[0] = 0; }
             */
            //-------------
            // Abschluss:
            lkmInit.aktualisiereKnotenpotentiale(1e-9, 0);     // pALT=p;

            for (int i1 = 0; i1 < pALT.length; i1++) {
                pALT[i1] = lkmInit.pALT[i1];
                pALTALT[i1] = lkmInit.pALT[i1];
                pALTALTALT[i1] = lkmInit.pALT[i1];
            }
        }
        //=======================================
        // CONTINUE -->
        //
        // ACHTUNG: Funktioniert nur, wenn die Schaltungsstruktur dh. die Knotenanzahl nicht veraendert wird!!!!
        //
        if (!getAnfangsbedVomDialogfenster) {  // gespeicherte Spannung zb. bei CONTINUE            
            for (int i1 = 0; i1 < elementANZAHL; i1++) {
                int x = netzliste.knotenX[i1];
                int y = netzliste.knotenY[i1];
                switch (netzliste.typ[i1]) {
                    case LK_C:
                    case TH_CTH:
                        pALT[x] = netzliste.parameter[i1][4];
                        pALT[y] = netzliste.parameter[i1][5];
                        break;
                    case REL_MMF:
                    case TH_TEMP:
                        pALT[x] = netzliste.parameter[i1][8];
                        pALT[y] = netzliste.parameter[i1][9];
                        break;
                    case LK_LKOP2:
                        // SpgQuellen-Stroeme stehen als Unbekannte ausnahmsweise auch im Knotenpotetial-Vektor
                        pALT[netzliste.knotenMAX + netzliste.spgQuelleNr[i1]] = netzliste.parameter[i1][2];
                        break;
                    default:
                        break;
                }
            }
        }
        //
        //------------------------------
        //
        // Stroeme initialisieren:
        for (int i1 = 0; i1 < iALT.length; i1++) {
            iALT[i1] = 0;
            iALTALT[i1] = 0;
            iALTALTALT[i1] = 0;
        }
        //

        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            switch (netzliste.typ[i1]) {
                case LK_C:
                case TH_CTH:
                    if (!getAnfangsbedVomDialogfenster) {
                        iALT[i1] = netzliste.parameter[i1][2];
                    }
                    break;
                case NONLIN_REL:
                case LK_L:
                    if (getAnfangsbedVomDialogfenster) {
                        iALT[i1] = netzliste.parameter[i1][1];
                    } else {
                        iALT[i1] = netzliste.parameter[i1][2];
                    }
                    iALTALT[i1] = iALT[i1];
                    break;
                case LK_LKOP2:
                    if (getAnfangsbedVomDialogfenster) {
                        iALT[i1] = netzliste.parameter[i1][1];
                    } else {
                        iALT[i1] = netzliste.parameter[i1][2];
                    }
                    if ((zuLKOP2gehoerigeM_spgQnr != null) && (zuLKOP2gehoerigeM_spgQnr[i1] != null)) {
                        int anzahlKoppelPartner = zuLKOP2gehoerigeM_spgQnr[i1].length;
                        for (int i7 = 0; i7 < anzahlKoppelPartner; i7++) {
                            p[netzliste.knotenMAX + netzliste.spgQuelleNr[i1]] = iALT[i1];  // SpgQuellen-Stroeme stehen als Unbekannte ausnahmsweise auch im Knotenpotetial-Vektor
                        }
                    }
                    iALTALT[i1] = iALT[i1];
                    break;
                case LK_I:
                case TH_FLOW:
                    if (getAnfangsbedVomDialogfenster) {
                        iALT[i1] = netzliste.parameter[i1][1];
                    } else {
                        iALT[i1] = netzliste.parameter[i1][6];
                    }
                    break;
                case LK_U:
                case REL_MMF:
                    switch ((int) (netzliste.parameter[i1][0])) {
                        case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY:
                            pALT[netzliste.knotenMAX + netzliste.spgQuelleNr[i1]] = netzliste.parameter[i1][10];
                            break;
                    }
                    //pALT[netzliste.knotenMAX + netzliste.spgQuelleNr[i1] + 1] = 2000;
                    //p[netzliste.knotenMAX + netzliste.spgQuelleNr[i1]] = 2000;                    
                    break;
                default:
                    break;
            }
        }
    }

    private double getAWForInductance(double usedInductance, double[] parameter, double dt) {
        double aW = 0;
        if (usedInductance < FAST_NULL_L) {
            if (_solverType == SolverType.SOLVER_BE) {
                aW = dt / FAST_NULL_L;  // falls L==0
            } else if (_solverType == SolverType.SOLVER_TRZ) {
                aW = 0.5 * dt / FAST_NULL_L;
            } else if (_solverType == SolverType.SOLVER_GS) {
                aW = (2.0 / 3.0) * (dt / FAST_NULL_L);
            }
            parameter[10] = FAST_NULL_L;
        } else {
            //aW = 0.5 * dt / netzliste.parameter[i1][0];  //  +dt/L
            if (_solverType == SolverType.SOLVER_BE) {
                aW = dt / usedInductance;  //  +dt/L
            } else if (_solverType == SolverType.SOLVER_TRZ) {
                aW = 0.5 * dt / usedInductance;
            } else if (_solverType == SolverType.SOLVER_GS) {
                aW = (2.0 / 3.0) * (dt / usedInductance);
            }
            parameter[10] = usedInductance;

        }
        return aW;
    }
}
