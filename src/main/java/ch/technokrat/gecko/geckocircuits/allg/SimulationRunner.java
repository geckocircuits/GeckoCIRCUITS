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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern.SimulationStatus;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL;
import ch.technokrat.gecko.geckocircuits.control.ReglerOSZI;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.control.DataSaver;
import ch.technokrat.gecko.geckoscript.SimulationAccess;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;


public final class SimulationRunner {

    final Fenster _fenster;
    final SchematischeEingabe2 _se;
    public SimulationsKern simKern;
    private NetListContainer nlContainer;

    public SimulationRunner(final Fenster fenster, final SchematischeEingabe2 schematicEntry) {
        _fenster = fenster;
        _se = schematicEntry;
    }

    public void startCalculation(boolean createNewSimThread, SolverSettings solverSettings) throws Exception {
        boolean getAnfangsbedVomDialogfenster = true;
        _fenster.setMenuDuringSimulation(true, false);

        simKern = new SimulationsKern();
        double tSTART = 0, tAktuell = tSTART;
        double tEND = solverSettings._tDURATION.getValue();
        double dtLoc = solverSettings.dt.getValue();

        if (solverSettings._T_pre.getValue() > 0) {
            solverSettings.inPreCalculationMode = true;
        }

        if (solverSettings.inPreCalculationMode) {
            tEND = solverSettings._T_pre.getValue();
            dtLoc = solverSettings._dt_pre.getValue();
        }

        nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);
        

        simKern.initSimulation(
                dtLoc, tSTART, tAktuell, tEND, solverSettings._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        simKern.setScopeMenuesStartStop();
        solverSettings._dt_ALT = dtLoc;
        simKern.initialisiereCONTROLatSimulationStart(dtLoc);  // wird nicht gemacht, wenn 'Continue' aktiviert wird                        

        RunThreadRun _runThread = new RunThreadRun();

        if (createNewSimThread) {
            final Thread calc = new Thread(_runThread);
            calc.setName("simulationThread");
            calc.start();
        } else {
            _runThread.setRunWithoutThread();
            _runThread.run();
        }
    }
    
        

    void continueCalculation(final boolean createNewSimThread, final SolverSettings solverSettings) throws Exception {
        boolean getAnfangsbedVomDialogfenster = false;
        _fenster.setMenuDuringSimulation(true, false);

        double tAktuell = simKern.getZeitAktuell();  // da sind wir gerade beim Simulieren
        double tSTART = simKern.getTSTART();
        double tEND = simKern.getTEND();

        if (tAktuell >= tEND) {  // naechstes Simulations-Fenster wird geoeffnet
            tSTART = tEND;
            tEND += solverSettings._tDURATION.getValue();
            simKern.setZeiten(tSTART, tEND, solverSettings.dt.getValue());
            boolean recalculateMatrix = false;
            if (solverSettings.dt.getValue() != solverSettings._dt_ALT) {
                recalculateMatrix = true;
            }
            solverSettings._dt_ALT = solverSettings.dt.getValue();

            nlContainer = NetListContainer.fabricContinueSimulation(_se, simKern, nlContainer);

            simKern.initSimulation(solverSettings.dt.getValue(), tSTART, tAktuell, tEND,
                    solverSettings._tPAUSE.getValue(),
                    getAnfangsbedVomDialogfenster, nlContainer, recalculateMatrix);
            simKern.setScopeMenuesStartStop();
            simKern.setInitialConditionsFromContinue();  // LK_Matrix mit den letztberechneten Werten fuellen

        } else { // Pause-Continue
            simKern._simulationStatus = SimulationStatus.RUNNING;
            simKern.setScopeMenuesStartStop();
            NetzlisteCONTROL.globalData.setContainerStatus(ContainerStatus.RUNNING);
        }

        Thread calc = new Thread(new RunThreadRun());
        if (createNewSimThread) {
            calc.start();
        } else {
            calc.run();
        }
    }

    private void waitForDataSavers() {
        int counter = 0;
        while (DataSaver.WAIT_COUNTER.get() != 0 && counter < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimulationAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
            counter++;
        }
    }

    void pauseSimulation() {
        try {
            if (simKern != null) {
                simKern._simulationStatus = SimulationStatus.PAUSED;
            } else {
                return;
            }

            _fenster.setMenuDuringSimulation(false, true);
            NetzlisteCONTROL.globalData.setContainerStatus(ContainerStatus.PAUSED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initSim(double dtLoc, double tEND) {
        
        // Ganz am Anfang: t0=0
        _fenster.setMenuDuringSimulation(true, false);
        //
        boolean getAnfangsbedVomDialogfenster = true;
        //---------------------

        List<AbstractBlockInterface> allElements = _se.getBlockInterfaceComponents();
        simKern = new SimulationsKern();
        double tSTART = 0, tAktuell = tSTART;
        _fenster._solverSettings._tDURATION.setValueWithoutUndo(tEND);
        _fenster._solverSettings.dt.setValueWithoutUndo(dtLoc);
        //double tEND = tDURATION;
        //double dtLoc = dt;

        if (_fenster._solverSettings.inPreCalculationMode) {
            tEND = _fenster._solverSettings._T_pre.getValue();
            dtLoc = _fenster._solverSettings._dt_pre.getValue();
        }

        nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);

        simKern.initSimulation(
                dtLoc, tSTART, tAktuell, tEND, _fenster._solverSettings._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        _fenster._solverSettings._dt_ALT = dtLoc;
        simKern.initialisiereCONTROLatSimulationStart(dtLoc);  // wird nicht gemacht, wenn 'Continue' aktiviert wird
        _fenster.jtfStatus.setText("Starting Simulation ... ");

        for (AbstractBlockInterface block : _se.getElementCONTROL()) {
            if (block instanceof ReglerOSZI) {
                ((ReglerOSZI) block).setSimulationTimeBoundaries(SimulationsKern.tSTART, SimulationsKern.tEND);
            }
        }

    }

    //for initializing simulation to be controlled step by step from GeckoSCRIPT
    public void initSim() {
        this.initSim(_fenster._solverSettings.dt.getValue(), _fenster._solverSettings._tDURATION.getValue());
    }
    

    private class RunThreadRun implements Runnable {

        long q1;
        long q2;
        private boolean _runWithoutThread = false;

        public void setRunWithoutThread() {
            _runWithoutThread = true;
        }

        public void run() {
            try {
                for (AbstractBlockInterface block : _se.getElementCONTROL()) {
                    if (block instanceof ReglerOSZI) {
                        ((ReglerOSZI) block).setSimulationTimeBoundaries(SimulationsKern.tSTART, SimulationsKern.tEND);
                    }
                }

                q1 = System.currentTimeMillis();
                q2 = 0;
                _fenster.jtfStatus.setText("Starting Simulation ... ");

                
                try {
                    simKern.runSimulation();

                    if (Fenster.IS_BRANDED) {
                        _fenster.mItemNew.setEnabled(false);
                        _fenster.mItemOpen.setEnabled(false);
                    } else {
                        _fenster.mItemNew.setEnabled(true);
                        _fenster.mItemOpen.setEnabled(true);
                    }
                    _fenster.mItemExit.setEnabled(true);
                    if (!Fenster.IS_APPLET) {
                        _fenster.mItemSave.setEnabled(true);
                        _fenster.mItemSaveAs.setEnabled(true);
                        _fenster.mItemSaveView.setEnabled(true);
                    }



                } catch (java.lang.OutOfMemoryError err) {
                    throw new OutOfMemoryError("Could not allocate enough java RAM memory for the simulation!");
                } finally {
                    if (!_fenster._solverSettings.inPreCalculationMode) {
                        endRun();
                    } else {
                        _fenster._solverSettings.inPreCalculationMode = false;
                        try {
                            _fenster.continueCalculation(false);
                        } catch (Throwable error) {                            
                            error.printStackTrace();
                            throw new RuntimeException(error);
                        }
                    }
                }
            } catch (Throwable error) {
                GeckoSim._win.pauseSimulation();
                GeckoSim._win._simRunner.simKern._simulationStatus = SimulationsKern.SimulationStatus.FINISHED;
                GeckoSim._win.jtfStatus.setText("Simulation aborted.");
                if (!_runWithoutThread) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            error.getMessage(),
                            "Severe error!",
                            JOptionPane.ERROR_MESSAGE);
                } else {                    
                    throw new RuntimeException(error);
                }
            }
        }

        public void endRun() {
            q2 = System.currentTimeMillis();
            _fenster.pauseSimulation();
            simKern._simulationStatus = SimulationStatus.FINISHED;
            simKern.tearDownOnPause();
            _fenster.jtfStatus.setzeStatusRechenzeit(q2 - q1);
            waitForDataSavers();
            _fenster.setMenuDuringSimulation(false, true);
        }
    }

    //========================================================================
    // SIMULINK-KOPPLUNG  -  Zugriff erfolgt ueber 'GeckoSim.java'
    //
    public void external_init(double tEnd) {
        _fenster.jtfStatus.setText("Starting Simulation ... ");
        boolean getAnfangsbedVomDialogfenster = true;  // AB von Simulink abholen!
        simKern = new SimulationsKern();
        double tSTART = 0, tAktuell = tSTART;
        nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);
        simKern.initSimulation(
                _fenster._solverSettings.dt.getValue(), tSTART, tAktuell, tEnd, _fenster._solverSettings._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        simKern.initialisiereCONTROLatSimulationStart(_fenster._solverSettings.dt.getValue());
    }
}
