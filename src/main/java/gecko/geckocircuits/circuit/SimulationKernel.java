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
package gecko.geckocircuits.circuit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.geckocircuits.api.ISimulationEngine;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractMotor;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractVoltageSource;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCurrentSource;
import gecko.geckocircuits.general.MainWindow;
import gecko.geckocircuits.circuit.circuitcomponents.ReluctanceInductor;
import gecko.core.circuit.SourceType;
import gecko.geckocircuits.control.*;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.geckocircuits.datacontainer.CompressorIntMatrix;
import gecko.geckocircuits.datacontainer.IntegerMatrixCache;
import gecko.geckocircuits.datacontainer.ShortMatrixCache;
import gecko.geckocircuits.newscope.ScopeFrame;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(value = {"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "PA_PUBLIC_PRIMITIVE_ATTRIBUTE"},
        justification = "Static fields staticTSTART/staticTEND are intentionally written for backward compatibility; public simulation status for runtime monitoring")
public class SimulationKernel implements ISimulationEngine {
    private static final Logger LOGGER = LogManager.getLogger(SimulationKernel.class);


    private double dt, t, tPAUSE;
    private double tSTART, tEND;

    // Static fields for backward compatibility with classes that don't have SimulationKernel instance access
    // These are updated whenever instance fields are set. Use getTSTART()/getTEND() methods when possible.
    private static volatile double staticTSTART;
    private static volatile double staticTEND;
    private LKMatrices lkmLK;  // Leistungskreis
    private LKMatrices lkmTHERM;  // thermischer Kreis
    private NetListLK nl;  // Leistungskreis
    private NetzlisteCONTROL controlNL;  // Regelkreis
    private NetListLK thermNL;  // thermischer Kreis
    private boolean simuliereLeistungskreis;  // ist 'false' wenn kein Leistungskreis aufgestellt wurde
    private boolean simuliereRegelkreis;  // ist 'false' wenn kein Regelkreis aufgestellt wurde
    private boolean simuliereThermKreis;  // ist 'false' wenn kein thermischer Kreis aufgestellt wurde
    //-------------------------------
    // Leistungskreis und thermischer Kreis:
    //
    private double[] pLK_ALT, pTHERM_ALT;  // zur Speicherung der Systemmatrizen fuer korrektes Weitermachen nach CONTINUE
    //-------------------------------
    // Kopplung [ Leistungskreis - Regelkreis - thermischer Kreis ]
    //
    private int[] interessanteKnotenLK;   // zum Aufnehmen der ZV-Spannungs-Kurven zwischen LK-Knoten mittels VOLT
    private int[] interessanteKnotenTHERM;   // zum Aufnehmen der ZV-Temperatur-Kurven zwischen THERM-Knoten mittels TEMP
    private int[] zeigerAufControlElement;  // Interaktion LK - CONTROL
    private int[] zeigerAufControlElementTHERM;  // Interaktion THERM - CONTROL
    private int[][] zeiger_VIEWMOT_MaschineLK;  // zur Messung von Maschinen-internen Parametern
    private int jjZeiger;
    private int mult = 20, add = 50;  // Erweiterung der Felder in den Interaktions-Methoden, um Spezialfaelle abzudecken
    //
    private int[][] zuordnung_SchalterLK_SWITCH;  // Welcher Schalter wird durch welchen SWITCH angesteuert?
    private int[][] zuordnung_QuelleLK_signalCONTROL;  // Welcher CONTROL-Knoten steuert welche signalgesteuerte LK-Quelle?
    private int[][] zuordnung_QuelleTHERM_signalCONTROL;  // Welcher CONTROL-Knoten steuert welche signalgesteuerte THERM-Quelle?
    private int[][] zuordnung_MaschineLK_LoadParameterInCONTROL;  // Welcher CONTROL-Knoten definiert welche mechanischen Signale (zB. externes Moment) des Motors?
    //
    private RegelBlock[] c;
    private double[][] controlParameters;
    private int controlANZAHL;
    //-------------------------------
    private AbstractCachedMatrix _lkCachedMatrix;
    LUDecompositionCache _luDecompCache;
    private AbstractCachedMatrix _thCachedMatrix;
    LUDecompositionCache _thLuDecompCache;
    private AbstractControlCalculatable[] sortedCalculators;
    private AbstractControlCalculatable[] unsortedCalculators;

    public enum SimulationStatus {

        NOT_INIT,
        RUNNING,
        PAUSED,
        FINISHED
    }
    public SimulationStatus _simulationStatus = SimulationStatus.NOT_INIT;

    @Override
    public ISimulationEngine.SimulationStatus getSimulationStatus() {
        switch (_simulationStatus) {
            case RUNNING:
                return ISimulationEngine.SimulationStatus.RUNNING;
            case PAUSED:
                return ISimulationEngine.SimulationStatus.PAUSED;
            case FINISHED:
                return ISimulationEngine.SimulationStatus.FINISHED;
            case NOT_INIT:
            default:
                return ISimulationEngine.SimulationStatus.NOT_INIT;
        }
    }

    public SimulationKernel() {
        _thLuDecompCache = new LUDecompositionCache();
        _luDecompCache = new LUDecompositionCache();
    }

    @Override
    public void pauseSimulation() {
        // no-op
    }

    @Override
    public double getCurrentTime() {
        return t;
    }

    @Override
    public double getTEND() {
        return this.tEND;
    }

    @Override
    public double getTSTART() {
        return this.tSTART;
    }

    /**
     * Static accessor for backward compatibility with classes that don't have SimulationKernel instance access.
     * Prefer using instance methods getTSTART()/getTEND() when possible.
     */
    public static double getStaticTSTART() {
        return staticTSTART;
    }

    /**
     * Static accessor for backward compatibility with classes that don't have SimulationKernel instance access.
     * Prefer using instance methods getTSTART()/getTEND() when possible.
     */
    public static double getStaticTEND() {
        return staticTEND;
    }

    @Override
    public double getdt() {
        return dt;
    }
    static int counter = 0;

    private void simulateOneTimeStep() {
        counter++;
        if (simuliereLeistungskreis) {
            nl.updateNonlinearCapacitancesAndResistors();
            final boolean mindestensEineAktiveSchalthandlung = checkForSwitchAction();

            if (mindestensEineAktiveSchalthandlung) {
                lkmLK.schreibeMatrix_A(dt, t, false);
                _lkCachedMatrix = _luDecompCache.getCachedLUDecomposition(lkmLK.a, t);
            }

            lkmLK.schreibeMatrix_B(dt, t, false);
            lkmLK.p = _lkCachedMatrix.solve(lkmLK.bVector);
            doDiodeErrorsRecalculations();
            nl.calculateSubCircuitAsDifferentialEquation(dt, t);  // interne Berechungen in SubCircuits diverser LK-Elemente
            lkmLK.aktualisiereKnotenpotentiale(dt, t);     // pALT=p;
            dataTransferLK_Control();
        }

        if (simuliereThermKreis) {
            lkmTHERM.schreibeMatrix_B(dt, t, false);
            // Solving the matrix equations (power circuit):

            lkmTHERM.p = _thCachedMatrix.solve(lkmTHERM.bVector);
            lkmTHERM.calculateComponentCurrents(-1, dt, t, false, 0);  // stoergroesse '-1' nur fuer die Dioden relevant, hier sind Werte egal
            thermNL.calculateSubCircuitAsDifferentialEquation(dt, t);  // eventuelle analytische Berechungen in SubCircuits der THERM-Elemente
            lkmTHERM.aktualisiereKnotenpotentiale(dt, t);  // pALT_THERM= pTHERM;
            dataTransferTherm();
        }

        //===================================
        // Regler:
        //===================================
        if (simuliereRegelkreis) {
            controlNL.calculateTimeStep(dt, t);
        }

        //lkmLK.schreibeRechendatenNachEinemZeitschritt(t);
        // // Pause is forced at a certain time -->
        if ((t - dt / 2 <= tPAUSE) && (tPAUSE <= t + dt / 2)) {
            _simulationStatus = SimulationStatus.PAUSED;
        }
    }

    private void doDiodeErrorsRecalculations() {
        int switchingErrorCounter = 0;   // 'stoersize<1.0' prevents the algorithm from getting stuck when switching diodes between states
        double stoergroesse = 1;//0.9999999;
        boolean isNewIteration = false;

        while (lkmLK.calculateComponentCurrents(stoergroesse, dt, t, isNewIteration, switchingErrorCounter)) {
            isNewIteration = true;
            if (switchingErrorCounter > 10000) {
                //new DialogDiodenError(switchingErrorCounter, t);
                this.lastUpdateOfScope();  // the final simulation will be updated again to be on the safe side
                throw new Error("Numerical instablity of switch!\nAborting simulation.");
            }

            //this.ausgebenDiodenzustaende(t);
            if ((switchingErrorCounter++) > 2) {
                stoergroesse *= 0.99;
            }

            _lkCachedMatrix = _luDecompCache.getCachedLUDecomposition(lkmLK.a, t);
            lkmLK.p = _lkCachedMatrix.solve(lkmLK.bVector);
        }
    }

    private void setControlledSourcesFromControlValue() {
        // Control of the signal-controlled LK sources using a signal from the control circuit:
        for (int[] mapping : zuordnung_QuelleLK_signalCONTROL) {
            int controlIndex = mapping[1];
            int outputIndex = mapping[2];

            AbstractCircuitBlockInterface e = nl.elements[mapping[0]];
            double[] par = e.parameter;
            double[][] blockOutput = unsortedCalculators[controlIndex]._outputSignal;

            if (blockOutput != null) {  // because yout was not yet defined at the first time step
                par[1] = blockOutput[outputIndex][0];  // for signal-controlled sources, parameter[1] is the signal
            }
        }
    }

    private void setThermalControlledSourcesFromControlValues() {
        // Control of the signal-controlled THERM sources using a signal from the control circuit:
        for (int[] mapping : zuordnung_QuelleTHERM_signalCONTROL) {
            int controlIndex = mapping[1];
            int outputIndex = mapping[2];
            AbstractCircuitBlockInterface e = thermNL.elements[mapping[0]];
            double[] par = e.parameter;
            double[][] blockOutput = unsortedCalculators[controlIndex]._outputSignal;
            if (blockOutput != null) {  // because yout was not yet defined at the first time step
                par[1] = blockOutput[outputIndex][0];  // for signal-controlled sources, parameter[1] is the signal
            }
        }
    }

    private void dataTransferTherm() {
        // Potentialdifferenzen im THERM-Kreis an Temperatur-Sensor TEMP uebergeben:
        for (int i1 = 0; i1 < interessanteKnotenTHERM.length; i1 += 2) {
            double potentialdifferenz = -1;
            if (interessanteKnotenTHERM[i1 + 1] == 0) {
                potentialdifferenz = lkmTHERM.p[interessanteKnotenTHERM[i1]] - 0;
            } else if (interessanteKnotenTHERM[i1] == 0) {
                potentialdifferenz = 0 - lkmTHERM.p[interessanteKnotenTHERM[i1 + 1]];
            } else if ((interessanteKnotenTHERM[i1 + 1] == 0) && (interessanteKnotenTHERM[i1] == 0)) {
                potentialdifferenz = 0 - 0;
            } else {
                potentialdifferenz = lkmTHERM.p[interessanteKnotenTHERM[i1]] - lkmTHERM.p[interessanteKnotenTHERM[i1 + 1]];
            }
            // Signal bei TEMP setzen:
            try {
                sortedCalculators[zeigerAufControlElementTHERM[i1 / 2]]._outputSignal[0][0] = potentialdifferenz;
            } catch (Exception ex) {
                LOGGER.error(controlParameters[zeigerAufControlElementTHERM[i1 / 2]].length + " " + zeigerAufControlElementTHERM[i1 / 2]);
                ex.printStackTrace();
            }

        }
        setThermalControlledSourcesFromControlValues();
    }

    private void dataTransferLK_Control() {

        setControlledSourcesFromControlValue();
        //------------------
        // Control of the external machine values ​​(e.g. load torque) using a signal from the control circuit:
        for (int[] mapping : zuordnung_MaschineLK_LoadParameterInCONTROL) {
            AbstractCircuitBlockInterface e = nl.elements[mapping[0]];
            int controlIndex = mapping[1];
            int outputIndex = mapping[2];
            double[] par = e.parameter;
            double[][] blockOutput = unsortedCalculators[controlIndex]._outputSignal;
            par[((AbstractMotor) e).getIndexForLoadTorque()] = blockOutput[outputIndex][0];  // for signal-controlled sources, parameter[1] is the signal
        }
        //-------------------------------------------------------------------
        // Potentialdifferenzen im LK-Kreis an Regelkreiselement VOLT uebergeben:
        for (int i1 = 0; i1 < interessanteKnotenLK.length; i1 += 2) {
            double potentialdifferenz = -1;
            if (interessanteKnotenLK[i1 + 1] == 0) {
                potentialdifferenz = lkmLK.p[1 + interessanteKnotenLK[i1] - 1];
            } else if (interessanteKnotenLK[i1] == 0) {
                potentialdifferenz = (0 - lkmLK.p[interessanteKnotenLK[i1 + 1]]);
            } else if ((interessanteKnotenLK[i1 + 1] == 0) && (interessanteKnotenLK[i1] == 0)) {
                potentialdifferenz = (0 - 0);
            } else {
                potentialdifferenz = (lkmLK.p[1 + interessanteKnotenLK[i1] - 1] - lkmLK.p[1 + interessanteKnotenLK[i1 + 1] - 1]);
            }

            // Signal bei VOLT setzen:
            // System.out.println("getting voltage: " + potentialdifferenz);
            sortedCalculators[zeigerAufControlElement[i1 / 2]]._outputSignal[0][0] = potentialdifferenz;

        }

        //------------------
        // apply the internal machine parameter selected in VIEWMOT to the CONTROL output of C_VIEWMOT:
        for (int[] mapping : zeiger_VIEWMOT_MaschineLK) {
            AbstractCircuitBlockInterface lkBlock = nl.elements[mapping[1]];
            double interneMaschienenGroesse = lkBlock.parameter[mapping[2]];
            sortedCalculators[mapping[0]]._outputSignal[0][0] = interneMaschienenGroesse;
        }
    }

    private boolean checkForSwitchAction() {

        boolean mindestensEineAktiveSchalthandlung = false;

        for (int[] mapping : zuordnung_SchalterLK_SWITCH) {
            double schaltSignal = sortedCalculators[mapping[0]]._inputSignal[0][0];
            AbstractCircuitBlockInterface e = nl.elements[mapping[1]];
            final double[] par = e.parameter;
            switch (e.getCircuitTyp()) {
                case LK_S:
                    // rDS(t) - rDS,on - rDS,off - i(t) - u(t) - uDSon[V] - k_on[Ws] - k_off[Ws]    -->
                    double rS_vorher = par[0];
                    if (schaltSignal > 0.5) {
                        par[0] = par[1];  // --> on, threshold '0.5' for switching
                    } else {
                        par[0] = par[2];  // --> off
                    }
                    double rS_nachher = par[0];
                    if (rS_vorher != rS_nachher) {
                        mindestensEineAktiveSchalthandlung = true;
                    }
                    break;
                case LK_MOSFET:
                    // rDS(t) - rDS,on - rDS,off - i(t) - u(t) - uDSon[V] - k_on[Ws] - k_off[Ws]    -->
                    rS_vorher = par[0];
                    if (schaltSignal > 0.5) {
                        par[0] = par[2];  // --> on, threshold '0.5' for switching
                    } else {
                        par[0] = par[3];  // --> off
                    }
                    rS_nachher = par[0];
                    if (rS_vorher != rS_nachher) {
                        mindestensEineAktiveSchalthandlung = true;
                    }
                    break;
                case LK_THYR:
                    // THYR is treated like a diode (see below!!), the current gate state is stored here:
                    if (schaltSignal > 0.5) {
                        par[8] = 1;
                    } else {
                        par[8] = 0;  // --> on, threshold '0.5' for switching
                    }
                    break;
                case LK_IGBT:

                    // rD(t) - uf - rON - rOFF - i(t) - u(t) - xxx - xxx - gateStatusOnOff   --> aehnlich wie THYR
                    // rD(t) - uf - rON - rOFF - i(t) - u(t) - xxx - xxx - gateStatusOnOff   --> aehnlich wie THYR
                    if (schaltSignal > 0.5) {
                        par[8] = 1;
                    } else {
                        par[8] = 0;  // --> on, threshold '0.5' for switching
                    }
                    break;
                default:
                    // No switch action needed for other circuit types
                    break;
            }
        }
        return mindestensEineAktiveSchalthandlung;
    }

    @Override
    public void runSimulation() {
        while ((t <= tEND) && (_simulationStatus != SimulationStatus.PAUSED)) {
            simulateOneTimeStep();
            t += dt;
        }

        this.lastUpdateOfScope();

        // Save the last solution in order to be able to continue correctly in the case of CONTINUE -->$
        pLK_ALT = new double[lkmLK.p.length];
        System.arraycopy(lkmLK.p, 0, pLK_ALT, 0, pLK_ALT.length);
        pTHERM_ALT = new double[lkmTHERM.p.length];
        System.arraycopy(lkmTHERM.p, 0, pTHERM_ALT, 0, pTHERM_ALT.length);
    }

    @Override
    public void simulateOneStep() throws Exception {
        if (t + dt > tEND) {
            throw new Exception("Specified end of simulation reached! Cannot simulate another step.");
        }
        simulateOneTimeStep();
        t += dt;
    }

    @Override
    public void simulateTime(double time) throws Exception {
        double simtime = t + time;
        boolean overReach = false;
        if (simtime > tEND) {
            simtime = tEND;
            overReach = true;
        }
        while (t <= simtime) {
            simulateOneTimeStep();
            t += dt;
        }

        if (overReach) {
            throw new Exception("Specified simulation time goes beyond specified simulated end time; simulated only up to end time");
        }
    }

    @Override
    public void endSim() {
        _simulationStatus = SimulationStatus.FINISHED;
        this.lastUpdateOfScope();
        //controlNL.tearDownOnPause();
        //-------------------------------
        // set the initial conditions according to the last calculation when CONTINUE was pressed -->
        pLK_ALT = new double[lkmLK.p.length];
        System.arraycopy(lkmLK.p, 0, pLK_ALT, 0, pLK_ALT.length);
        pTHERM_ALT = new double[lkmTHERM.p.length];
        System.arraycopy(lkmTHERM.p, 0, pTHERM_ALT, 0, pTHERM_ALT.length);
        //-------------------------------
    }

    //System.out.println("Warning: Node-Number has been changed!");
    @Override
    public void setInitialConditionsFromContinue() {
        if ((lkmLK.p.length != pLK_ALT.length) || (lkmTHERM.p.length != pTHERM_ALT.length)) {
            //System.out.println("Warning: Node-Number has been changed!");
            return;
        }
        lkmLK.p = pLK_ALT;
        lkmTHERM.p = pTHERM_ALT;
    }

    @Override
    public void setZeiten(double tSTART, double tEND, double dt) {
        this.tSTART = tSTART;
        this.tEND = tEND;
        this.dt = dt;
        // Update static fields for backward compatibility
        updateStaticTimes(tSTART, tEND);
    }

    private static void updateStaticTimes(double start, double end) {
        staticTSTART = start;
        staticTEND = end;
    }

    public void initSimulation(
            double dt, double tSTART, double tAktuell, double tEND, double tPAUSE,
            boolean getAnfangsbedVomDialogfenster,
            NetListContainer nlContainer, boolean recalculateMatrixFromDifferentDt) {
        _simulationStatus = SimulationStatus.RUNNING;

        this.dt = dt;
        this.tSTART = tSTART;
        this.tEND = tEND;
        this.tPAUSE = tPAUSE;
        this.t = tAktuell;
        // Update static fields for backward compatibility
        updateStaticTimes(tSTART, tEND);
        //
        this.controlNL = nlContainer._nlControl;
        controlNL.doMemorInits(dt);
        if (recalculateMatrixFromDifferentDt) {
            controlNL.doDtChangeInit(dt);
        }

        this.nl = nlContainer._nlLK;
        nl.updateNonlinearCapacitancesAndResistors();
        this.thermNL = nlContainer._nlTH;

        this.c = controlNL._orderedControlBlocks;
        sortedCalculators = controlNL.getSortedControlCalculators();
        unsortedCalculators = controlNL._allUnSortedControlCalculators;

        controlParameters = new double[c.length][];
        for (int i = 0; i < c.length; i++) {
            if (c[i] != null) {
                controlParameters[i] = c[i].parameter;
            }
        }

        this.controlANZAHL = c.length;

        //***************************
        // Leistungskreis:
        //
        definiereInteraktion_VOLT_AMP_LK();  // how do I measure currents and voltages in the LK with CONTROL-AMP or CONTROL-VOLT?
        definiereInteraktion_MaschineLK_VIEWMOT();  // for measuring internal machine parameters
        //
        zuordnung_SchalterLK_SWITCH = defineInteractionSwitchController();  // welches CONTROL-GATE steuert welchen LK_SWITCH (LK_S,LK_IGBT,LK_THYR) an?
        zuordnung_QuelleLK_signalCONTROL = definiereInteraktion_SignalgesteuerteQuelle_Regler(nl);  // welches allg. CONTROL-Signal steuert welche LK_QUELLE (LK_U, LK_I) an?
        zuordnung_QuelleTHERM_signalCONTROL = definiereInteraktion_SignalgesteuerteQuelle_Regler(thermNL);  // welches allg. CONTROL-Signal steuert welche THERM_QUELLE (LK_TEMP, LK_FLOW) an?

        zuordnung_MaschineLK_LoadParameterInCONTROL = definiereInteraktion_MaschineLK_LoadParameterInCONTROL();  // Welcher CONTROL-Knoten definiert welche mechanischen Signale (zB. externes Moment) des Motors?
        definiereInteraktion_TEMP_FLOW_THERM();  // how do I measure thermal flow and temperature differences in THERM with CONTROL-FLOW or CONTROL-TEMP?
        setControlledSourcesFromControlValue();
        setThermalControlledSourcesFromControlValues();

        if (recalculateMatrixFromDifferentDt) {
            lkmLK.schreibeMatrix_A(dt, tAktuell, false);
            lkmTHERM.schreibeMatrix_A(dt, tAktuell, false);
        }
        // Leistungskreis:
        if (getAnfangsbedVomDialogfenster) {
            lkmLK = new LKMatrices(MainWindow._solverSettings.SOLVER_TYPE.getValue());
            lkmLK.initMatrizen(nl, getAnfangsbedVomDialogfenster, true, MainWindow._solverSettings.SOLVER_TYPE.getValue());  // pALT= new double[..];   iALT= new double[..];
            lkmLK.schreibeMatrix_A(dt, tAktuell, false);

            // thermischer Kreis:
            lkmTHERM = new LKMatrices(MainWindow._solverSettings.SOLVER_TYPE.getValue());
            lkmTHERM.initMatrizen(thermNL, getAnfangsbedVomDialogfenster, false, MainWindow._solverSettings.SOLVER_TYPE.getValue());
            lkmTHERM.schreibeMatrix_A(dt, tAktuell, false);
        }
        //=============================
        if (lkmLK.matrixSize < 2) { // if the power circuit does not exist, it is consequently not simulated:
            simuliereLeistungskreis = false;
        } else {
            simuliereLeistungskreis = true;
            _lkCachedMatrix = _luDecompCache.getCachedLUDecomposition(lkmLK.a, t);
        }

        if (lkmTHERM.matrixSize < 2) { // if the thermal circuit does not exist, it is consequently not simulated:
            simuliereThermKreis = false;
        } else {
            simuliereThermKreis = true;
            _thCachedMatrix = _thLuDecompCache.getCachedLUDecomposition(lkmTHERM.a, t);
        }
        if (controlANZAHL < 1) {
            simuliereRegelkreis = false;
        } else {
            simuliereRegelkreis = true;
        }
    }

    public void setScopeMenuesStartStop() {
        // ganz am Anfang sofort einmal auffrischen:
        for (RegelBlock block : c) {
            if (block instanceof ControlOSZI) {
                ControlOSZI oszi = (ControlOSZI) block;
                if (oszi._scopeFrame != null) {
                    oszi._scopeFrame.setScopeMenueEnabled(true);
                }
            }
            if (block instanceof ControlCISPR16) {
                ((ControlCISPR16) block).setTestReceiverCISPR16MenueEnabled(false);
            }
        }
    }

    public void initialisiereCONTROLatSimulationStart(final double dt) {
        controlNL.initializeAtSimulationStart(dt);
        ShortMatrixCache.clearCache();
        IntegerMatrixCache.clearCache();
        CompressorIntMatrix.clearCache();
    }

    private int[][] definiereInteraktion_SignalgesteuerteQuelle_Regler(final NetListLK netlist) {
        //---------------------------------------
        // Welche Signalgesteuerte Quelle im LK (VOLT, AMP, ...) wird von welchem CONTROL-Signal angesteuert? -->
        //
        int[][] zuordnung_QuelleLK_signalCONTROL = new int[mult * controlANZAHL + add][];
        int counter = 0;
        AbstractCircuitBlockInterface[] allElements = netlist.elements; // careful: we DON't use elements including subcircuits. In this case,
        // the direct component value is defined somewhere else!
        for (int i = 0; i < allElements.length; i++) {
            AbstractCircuitBlockInterface element = allElements[i];
            if (element instanceof AbstractVoltageSource || element instanceof AbstractCurrentSource) {
                if (element.getParameter()[0] == SourceType.QUELLE_SIGNALGESTEUERT_NEW || element.getParameter()[0] == SourceType.QUELLE_SIGNALGESTEUERT) {
                    NetzlisteCONTROL.IndexConnection li = controlNL.getIndexConnection(element.getParentCircuitSheet(), element.getParameterString()[0]);
                    if (li != null) { // this can happen when no control signal is selected/assigned!
                        zuordnung_QuelleLK_signalCONTROL[counter]
                                = new int[]{i, li._elementIndex, li._inBlockIndex_outputIndex};
                        counter++;
                    }
                }
            }
        }

        int[][] zuordTEMP = new int[counter][3];
        for (int i1 = 0; i1 < counter; i1++) {
            for (int i2 = 0; i2 < 3; i2++) {
                zuordTEMP[i1][i2] = zuordnung_QuelleLK_signalCONTROL[i1][i2];
            }
        }
        return zuordTEMP;
    }

    private int[][] defineInteractionSwitchController() {
        // Welcher Schalter wird von welchem SWITCH-Regelblock angesteuert? -->
        int[][] zuordnung_SchalterLK_SWITCH = new int[mult * controlANZAHL + add][];
        int zuordnungANZAHL_SchalterLK_SWITCH = 0;
        for (int iC = 0; iC < controlANZAHL; iC++) {
            if (controlNL._orderedControlBlocks[iC] instanceof ControlGate) {
                ControlGate controlGate = (ControlGate) controlNL._orderedControlBlocks[iC];
                AbstractBlockInterface controlledSwitch = controlGate.getComponentCoupling()._coupledElements[0];
                if (controlledSwitch != null) {
                    for (int iLK = 0; iLK < nl.elements.length; iLK++) {
                        AbstractCircuitBlockInterface compareElement = nl.elements[iLK];
                        if (controlledSwitch.equals(compareElement)) {
                            zuordnung_SchalterLK_SWITCH[zuordnungANZAHL_SchalterLK_SWITCH] = new int[]{iC, iLK};
                            zuordnungANZAHL_SchalterLK_SWITCH++;
                        }
                    }
                }
            }
        }
        int[][] zuordTEMP = new int[zuordnungANZAHL_SchalterLK_SWITCH][2];
        for (int i1 = 0; i1 < zuordnungANZAHL_SchalterLK_SWITCH; i1++) {
            for (int i2 = 0; i2 < 2; i2++) {
                zuordTEMP[i1][i2] = zuordnung_SchalterLK_SWITCH[i1][i2];
            }
        }
        return zuordTEMP;
    }

    private int[][] definiereInteraktion_MaschineLK_LoadParameterInCONTROL() {
        //---------------------------------------
        //
        int[][] zuordnung_MaschineLK_LoadParameterInCONTROL = new int[mult * controlANZAHL + add][];
        int zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL = 0;
        for (int iLK = 0; iLK < nl.elements.length; iLK++) {
            AbstractCircuitBlockInterface block = nl.elements[iLK];
            if (block instanceof AbstractMotor) {
                NetzlisteCONTROL.IndexConnection li = controlNL.getIndexConnection(nl.elements[iLK].getParentCircuitSheet(), nl.elements[iLK].getParameterString()[0]);
                if (li != null) {
                    zuordnung_MaschineLK_LoadParameterInCONTROL[zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL] = new int[]{iLK, li._elementIndex, li._inBlockIndex_outputIndex};
                    zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL++;
                }
            }
        }
        int[][] zuordTEMP = new int[zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL][3];
        for (int i1 = 0; i1 < zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL; i1++) {
            for (int i2 = 0; i2 < 3; i2++) {
                zuordTEMP[i1][i2] = zuordnung_MaschineLK_LoadParameterInCONTROL[i1][i2];
            }
        }
        //
        return zuordTEMP;
        //---------------------------------------
    }

    private void definiereInteraktion_MaschineLK_VIEWMOT() {
        //--------
        String iA2 = "";
        int[][] zeiger_VIEWMOT_MaschineLK_temp = new int[controlANZAHL][3];
        jjZeiger = 0;
        for (int i1 = 0; i1 < controlANZAHL; i1++) {
            if (c[i1] instanceof ControlVIEWMOT) {
                ControlVIEWMOT controlVIEWMOT = (ControlVIEWMOT) c[i1];
                AbstractBlockInterface selectedMotor = controlVIEWMOT.getComponentCoupling()._coupledElements[0];
                if (selectedMotor != null) {
                    iA2 = c[i1].getParameterString()[2];  // --> "omega"
                    for (int i2 = 0; i2 < nl.elements.length; i2++) {
                        AbstractCircuitBlockInterface el = nl.elements[i2];
                        if (selectedMotor.equals(el)) {
                            List<String> parameterStringIntern = el.getParameterStringIntern();
                            for (int i3 = 0; i3 < parameterStringIntern.size(); i3++) {
                                if (iA2.equals(parameterStringIntern.get(i3))) {  // --> internen Motor-Parameter gefunden
                                    zeiger_VIEWMOT_MaschineLK_temp[jjZeiger] = new int[]{i1, i2, i3};
                                    jjZeiger++;
                                }
                            }
                        }
                    }
                }
            }
        }
        zeiger_VIEWMOT_MaschineLK = new int[jjZeiger][3];
        System.arraycopy(zeiger_VIEWMOT_MaschineLK_temp, 0, zeiger_VIEWMOT_MaschineLK, 0, jjZeiger);
    }

    private void definiereInteraktion_VOLT_AMP_LK() {
        // interessante Knotenpotentiale (zum Speichern bw. Ausgeben):
        List<Integer> iKn = new ArrayList<Integer>();
        int[] zeigerACE = new int[c.length];

        jjZeiger = 0;
        for (int i1 = 0; i1 < controlANZAHL; i1++) {
            if (c[i1] instanceof ControlVOLT || c[i1] instanceof ControlMMF) {
                AbstractPotentialMeasurement controlVOLT = (AbstractPotentialMeasurement) c[i1];
                AbstractBlockInterface directComponent = controlVOLT.getComponentCoupling()._coupledElements[0];

                String uA = c[i1].getParameterString()[0];
                String uB = c[i1].getParameterString()[1];
                if ((!uA.isEmpty()) && (!uB.isEmpty())) {
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;

                    int firstIndex = nl.findIndexFromLabelInSheet(uA, controlVOLT);
                    iKn.add(firstIndex);

                    int secondIndex = nl.findIndexFromLabelInSheet(uB, controlVOLT);
                    iKn.add(secondIndex);
                } else if (directComponent != null) {
                    if (controlVOLT instanceof ControlMMF
                            && (directComponent instanceof ReluctanceInductor)) {
                        directComponent = ((ReluctanceInductor) directComponent)._secondarySource;
                    }
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;
                    int counter = 0;
                    boolean returnOK = false;
                    for (AbstractCircuitBlockInterface elem : nl.eLKneu) {
                        if (elem.equals(directComponent)) {
                            returnOK = true;
                            iKn.add(nl.knotenX[counter]);
                            iKn.add(nl.knotenY[counter]);
                        }
                        counter++;
                    }
                    if (!returnOK) {
                        throw new ArrayIndexOutOfBoundsException("\nMeasurement of component " + c[i1].getStringID() + "\nhas a missing reference!");
                    }

                }
            }
        }

        interessanteKnotenLK = new int[iKn.size()];
        for (int i = 0; i < iKn.size(); i++) {
            interessanteKnotenLK[i] = iKn.get(i);
        }
        zeigerAufControlElement = new int[jjZeiger];
        System.arraycopy(zeigerACE, 0, zeigerAufControlElement, 0, jjZeiger);
    }

    private void definiereInteraktion_TEMP_FLOW_THERM() {
        // interessante Knotenpotentiale (zum Speichern bw. Ausgeben):
        int[] iKn = new int[thermNL.getElementANZAHLinklusiveSubcircuit() * 2];
        int jj = 0;
        int[] zeigerACE = new int[c.length];
        jjZeiger = 0;
        for (int i1 = 0; i1 < controlANZAHL; i1++) {
            if (c[i1] instanceof ControlTEMP) {
                ControlTEMP controlTEMP = (ControlTEMP) c[i1];
                AbstractBlockInterface directComponent = controlTEMP.getComponentCoupling()._coupledElements[0];

                String uA = c[i1].getParameterString()[0];
                String uB = c[i1].getParameterString()[1];

                if ((!uA.isEmpty()) && (!uB.isEmpty())) {
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;

                    int firstIndex = thermNL.findIndexFromLabelInSheet(uA, controlTEMP);
                    if (firstIndex >= 0) {
                        iKn[jj] = firstIndex;
                        jj++;
                    }

                    int secondIndex = thermNL.findIndexFromLabelInSheet(uB, controlTEMP);
                    if (secondIndex >= 0) {
                        iKn[jj] = secondIndex;
                        jj++;
                    }

                } else if (directComponent != null) {
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;
                    int counter = 0;
                    boolean returnOK = false;

                    for (AbstractCircuitBlockInterface elem : thermNL.eLKneu) {
                        if (elem != null) {
                            if (directComponent.equalsPossibleSubComponent(elem)) {
                                returnOK = true;
                                iKn[jj] = thermNL.knotenX[counter];
                                jj++;
                                iKn[jj] = thermNL.knotenY[counter];
                                jj++;
                            }
                        }
                        counter++;
                    }
                    if (!returnOK) {
                        throw new ArrayIndexOutOfBoundsException("\nMeasurement of component " + c[i1].getStringID() + "\nhas a missing reference!");
                    }
                }
            }
        }
        interessanteKnotenTHERM = new int[jj];
        System.arraycopy(iKn, 0, interessanteKnotenTHERM, 0, jj);

        zeigerAufControlElementTHERM = new int[jjZeiger];
        System.arraycopy(zeigerACE, 0, zeigerAufControlElementTHERM, 0, jjZeiger);
    }

    private void lastUpdateOfScope() {
        this.lastUpdateOfScope(300);
    }

    private void lastUpdateOfScope(long sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            // ignored: interruption acceptable
        }  // damit es nicht zu einer 'RacingCondition' mit einer eventuell noch laufenden Aktualisierung aus takteAuffrischungScope() kommt
        //---------------------------
        for (RegelBlock block : c) {
            if (block instanceof ControlOSZI) {
                ScopeFrame sf = ((ControlOSZI) block)._scopeFrame;
                if (sf != null) {
                    sf.setScopeMenueEnabled(false);
                }
            }
            if (block instanceof ControlCISPR16) {
                ((ControlCISPR16) block).setTestReceiverCISPR16MenueEnabled(true);
            }
        }
    }

    // Externe SIMULINK-KOPPLUNG  -  Zugriff erfolgt ueber 'SimLE.java'
    //
    public void external_step(double time) {
        t = time;
        if (dt == 0) {
            dt = 1e-6;  // passiert beim Start?
        }
        simulateOneTimeStep();
    }

//    public void external_step (double time, double[] input) {
//-------------------------
//        t= time;
//System.out.println("\nexternal ended");
//
//        int counter = 0;
//        for(RegelBlock reg : ControlFromEXTERNAL.fromExternals) {
//            double[] param = reg.getParameter();
//            assert param.length == ((ControlFromEXTERNAL) reg).getTerminalNumber();
//            for(int i = 0; i < param.length; i++) {
//                param[i] = input[counter];
//                counter++;
//            }
//        }
//        //----
//        if (index_FROM_EXTERNAL!=-1) {
//            for (int i1=0;  i1<signals_from_external.length;  i1++) {
//                double x= input[i1];
//                if (x==x) signals_from_external[i1]= x; else signals_from_external[i1]=0;  // NaN, Inf
//                //signals_from_external[i1]= input[i1];
//            }
//            c[index_FROM_EXTERNAL].setParameter(signals_from_external);
//        }
//-------------------------------
//        simulateOneTimeStep();
//    }
    public double getTimeStep() {
        return dt;
    }

    public void external_end() {
        _simulationStatus = SimulationStatus.FINISHED;
        this.lastUpdateOfScope();  // the final simulation will be updated again to be on the safe side
        //-------------------------------
        // Speichern der letzten Loesung, um im Fall von CONTINUE korrekt weitermachen zu koennen -->
        pLK_ALT = new double[lkmLK.p.length];
        System.arraycopy(lkmLK.p, 0, pLK_ALT, 0, pLK_ALT.length);
        pTHERM_ALT = new double[lkmTHERM.p.length];
        System.arraycopy(lkmTHERM.p, 0, pTHERM_ALT, 0, pTHERM_ALT.length);
        //-------------------------------
        //System.out.println("\nexternal ended");
    }

    public void tearDownOnPause() {
        if (_simulationStatus == SimulationStatus.FINISHED) {
            controlNL.tearDownOnPause();
        }
    }
}
