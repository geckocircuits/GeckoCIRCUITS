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

	public void continueCalculation(final boolean createNewSimThread, final SolverSettings solverSettings) throws Exception {
		boolean getAnfangsbedVomDialogfenster = false;
		_mainwindow.setMenuDuringSimulation(true, false);

		double tAktuell = simKern.getZeitAktuell();
		double tSTART = simKern.getTSTART();
		double tEND = simKern.getTEND();

		if (tAktuell >= tEND) {
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
			simKern.setInitialConditionsFromContinue();

		} else {
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

	public void pauseSimulation() {
		try {
			if (simKern != null) {
				simKern._simulationStatus = SimulationStatus.PAUSED;
			} else {
				return;
			}

			_mainwindow.setMenuDuringSimulation(false, true);
			NetzlisteCONTROL.globalData.setContainerStatus(ContainerStatus.PAUSED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initSim(double dtLoc, double tEND) {
		// Ganz am Anfang: t0=0
		_mainwindow.setMenuDuringSimulation(true, false);
		boolean getAnfangsbedVomDialogfenster = true;

		simKern = new SimulationsKern();
		double tSTART = 0, tAktuell = tSTART;
		MainWindow._solverSettings._tDURATION.setValueWithoutUndo(tEND);
		MainWindow._solverSettings.dt.setValueWithoutUndo(dtLoc);

		if (MainWindow._solverSettings.inPreCalculationMode) {
			tEND = MainWindow._solverSettings._T_pre.getValue();
			dtLoc = MainWindow._solverSettings._dt_pre.getValue();
		}

		nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);

		simKern.initSimulation(
				dtLoc, tSTART, tAktuell, tEND, MainWindow._solverSettings._tPAUSE.getValue(),
				getAnfangsbedVomDialogfenster, nlContainer, false);
		MainWindow._solverSettings._dt_ALT = dtLoc;
		simKern.initialisiereCONTROLatSimulationStart(dtLoc);
		_mainwindow.jtfStatus.setText("Starting Simulation ... ");

		for (AbstractBlockInterface block : _se.getElementCONTROL()) {
			if (block instanceof ReglerOSZI) {
				((ReglerOSZI) block).setSimulationTimeBoundaries(simKern.getTSTART(), simKern.getTEND());
			}
		}
	}

	public void initSim() {
		this.initSim(0, 0); // dt and tEND should be set appropriately
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
						((ReglerOSZI) block).setSimulationTimeBoundaries(simKern.getTSTART(), simKern.getTEND());
					}
				}

				q1 = System.currentTimeMillis();
				q2 = 0;
				_mainwindow.jtfStatus.setText("Starting Simulation ... ");

				try {
					simKern.runSimulation();
					// GUI: update menu items
				} catch (java.lang.OutOfMemoryError err) {
					throw new OutOfMemoryError("Could not allocate enough java RAM memory for the simulation!");
				} finally {
					if (MainWindow._solverSettings.inPreCalculationMode) {
						endRun();
					} else {
						MainWindow._solverSettings.inPreCalculationMode = false;
						_mainwindow.continueCalculation(false);
					}
				}
			} catch (Throwable error) {
				_mainwindow.pauseSimulation();
				simKern._simulationStatus = SimulationStatus.FINISHED;
				_mainwindow.jtfStatus.setText("Simulation aborted.");
				if (!_runWithoutThread) {
					error.printStackTrace();
				} else {
					throw new RuntimeException(error);
				}
			}
		}

		public void endRun() {
			q2 = System.currentTimeMillis();
			_mainwindow.pauseSimulation();
			simKern._simulationStatus = SimulationStatus.FINISHED;
			simKern.tearDownOnPause();
			_mainwindow.jtfStatus.setzeStatusRechenzeit(q2 - q1);
			waitForDataSavers();
			_mainwindow.setMenuDuringSimulation(false, true);
		}
	}

	public void external_init(double tEnd) {
		_mainwindow.jtfStatus.setText("Starting Simulation ... ");
		boolean getAnfangsbedVomDialogfenster = true;
		simKern = new SimulationsKern();
		double tSTART = 0, tAktuell = tSTART;
		nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);
		simKern.initSimulation(
				MainWindow._solverSettings.dt.getValue(), tSTART, tAktuell, tEnd, MainWindow._solverSettings._tPAUSE.getValue(),
				getAnfangsbedVomDialogfenster, nlContainer, false);
		simKern.initialisiereCONTROLatSimulationStart(MainWindow._solverSettings.dt.getValue());
	}
}


