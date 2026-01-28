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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.NetListContainer;
import ch.technokrat.gecko.geckocircuits.circuit.SchematicEditor2;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern.SimulationStatus;
import ch.technokrat.gecko.geckocircuits.circuit.SolverSettings;
import ch.technokrat.gecko.geckocircuits.control.DataSaver;
import ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL;
import ch.technokrat.gecko.geckocircuits.control.ReglerOSZI;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckoscript.SimulationAccess;


public final class SimulationRunner {

    final MainWindow _mainwindow;
    final SchematicEditor2 _se;
    public SimulationsKern simKern;
    private NetListContainer nlContainer;

    public SimulationRunner(final MainWindow mainwindow, final SchematicEditor2 schematicEntry) {
        _mainwindow = mainwindow;
        _se = schematicEntry;
    }

    public void startCalculation(boolean createNewSimThread, SolverSettings solverSettings) throws Exception {
        boolean getAnfangsbedVomDialogfenster = true;
        _mainwindow.setMenuDuringSimulation(true, false);

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
        

// Stub: SimulationRunner has been migrated to gecko-simulation-core.
package ch.technokrat.gecko.geckocircuits.allg;

public final class SimulationRunner {
    public SimulationRunner(Object a, Object b) {
        throw new UnsupportedOperationException("SimulationRunner has been migrated to gecko-simulation-core.");
    }
}
            _mainwindow.jtfStatus.setzeStatusRechenzeit(q2 - q1);
            waitForDataSavers();
            _mainwindow.setMenuDuringSimulation(false, true);
        }
    }

    //========================================================================
    // SIMULINK-KOPPLUNG  -  Zugriff erfolgt ueber 'GeckoSim.java'
    //
    public void external_init(double tEnd) {
        _mainwindow.jtfStatus.setText("Starting Simulation ... ");
        boolean getAnfangsbedVomDialogfenster = true;  // AB von Simulink abholen!
        simKern = new SimulationsKern();
        double tSTART = 0, tAktuell = tSTART;
        nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);
        simKern.initSimulation(
                MainWindow._solverSettings.dt.getValue(), tSTART, tAktuell, tEnd, MainWindow._solverSettings._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        simKern.initialisiereCONTROLatSimulationStart(MainWindow._solverSettings.dt.getValue());
    }
}
