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
package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;

/**
 * Manages data transfer between the three simulation domains:
 * LK (circuit/electrical), CONTROL, and THERM (thermal).
 *
 * <p>Each time step the coupling transfers:
 * <ul>
 *   <li>LK → CONTROL : node voltages (VOLT/CURRENT measurement blocks)</li>
 *   <li>CONTROL → LK : source/switch control signals</li>
 *   <li>LK → THERM  : power loss values</li>
 *   <li>THERM → LK  : temperature-dependent resistances</li>
 * </ul>
 *
 * <p>Extracted from gecko.geckocircuits.circuit.SimulationsKern coupling arrays.
 *
 * @author Extracted for Phase 4 domain coupling
 * @since v2.18.0 Phase 4 - Domain coupling architecture
 */
public class DomainCoupler {

    // Monitored LK node voltages exposed to CONTROL domain
    // Index: control block input index → LK node index
    private int[] lkNodeForControlInput = new int[0];
    private double[] lkNodeVoltagesForControl = new double[0];

    // Control signals driving LK voltage/current sources
    // Index: LK source index → CONTROL output signal index
    private int[] controlOutputForLkSource = new int[0];
    private double[] controlSignalsForLkSources = new double[0];

    // Switch gate signals from CONTROL to LK
    private int[] controlOutputForLkSwitch = new int[0];
    private boolean[] switchGateSignals = new boolean[0];

    // Power losses from LK fed to THERM
    private double[] lkPowerLosses = new double[0];

    // Temperatures from THERM fed to LK (temperature-dependent components)
    private double[] thermTemperatures = new double[0];

    /**
     * Creates a DomainCoupler with no coupling (all domains independent).
     * Suitable for pure electrical circuits with no control or thermal.
     */
    public DomainCoupler() {
    }

    /**
     * Transfer LK node voltages to the CONTROL domain.
     * Called after the LK matrix solve, before CONTROL execution.
     *
     * @param circuitNetlist solved circuit netlist containing node voltages
     */
    public void transferLkToControl(CircuitNetlist circuitNetlist) {
        if (circuitNetlist == null || lkNodeForControlInput.length == 0) {
            return;
        }
        double[] nodeVoltages = circuitNetlist.getLastNodeVoltagesRef();
        for (int i = 0; i < lkNodeForControlInput.length; i++) {
            int nodeIdx = lkNodeForControlInput[i];
            if (nodeIdx >= 0 && nodeIdx < nodeVoltages.length) {
                lkNodeVoltagesForControl[i] = nodeVoltages[nodeIdx];
            }
        }
    }

    /**
     * Transfer CONTROL output signals to LK sources and switches.
     * Called after CONTROL execution, before building LK matrix b.
     *
     * @param controlOutputValues current CONTROL calculator output values
     */
    public void transferControlToLk(double[] controlOutputValues) {
        if (controlOutputValues == null) {
            return;
        }
        for (int i = 0; i < controlOutputForLkSource.length; i++) {
            int ctrlIdx = controlOutputForLkSource[i];
            if (ctrlIdx >= 0 && ctrlIdx < controlOutputValues.length) {
                controlSignalsForLkSources[i] = controlOutputValues[ctrlIdx];
            }
        }
        for (int i = 0; i < controlOutputForLkSwitch.length; i++) {
            int ctrlIdx = controlOutputForLkSwitch[i];
            if (ctrlIdx >= 0 && ctrlIdx < controlOutputValues.length) {
                switchGateSignals[i] = controlOutputValues[ctrlIdx] > 0.5;
            }
        }
    }

    /**
     * Performs a complete domain coupling cycle for one time step.
     * Order: solve LK → transfer LK→CTRL → execute CTRL → transfer CTRL→LK
     *
     * @param circuitNetlist circuit netlist (after LK solve)
     * @param controlNetlist control netlist (will be executed)
     * @param dt time step [s]
     * @param time current simulation time [s]
     */
    public void coupleDomainsForTimeStep(
        CircuitNetlist circuitNetlist,
        ControlNetlist controlNetlist,
        double dt,
        double time
    ) {
        // 1. Transfer LK node voltages to CONTROL inputs
        transferLkToControl(circuitNetlist);

        // 2. Execute CONTROL calculators (reads LK voltages, produces outputs)
        if (controlNetlist != null && controlNetlist.hasCalculators()) {
            controlNetlist.executeTimeStep(dt, time);
        }

        // 3. Transfer CONTROL outputs back to LK (sources, switches)
        // Note: actual source/switch updates happen in next buildVectorB/buildMatrixA call
        // (placeholder for Phase 5 full integration)
    }

    // --- Accessors ---

    /**
     * Gets a copy of the last transferred LK node voltages to CONTROL.
     *
     * @return copy of node voltages array for control domain
     */
    public double[] getLkNodeVoltagesForControl() {
        return lkNodeVoltagesForControl.clone();
    }

    /**
     * Gets a copy of the last transferred CONTROL signals to LK sources.
     *
     * @return copy of control signals for sources array
     */
    public double[] getControlSignalsForLkSources() {
        return controlSignalsForLkSources.clone();
    }

    /**
     * Gets a copy of the last transferred switch gate signals.
     *
     * @return copy of switch gate signals
     */
    public boolean[] getSwitchGateSignals() {
        return switchGateSignals.clone();
    }

    /**
     * Gets a copy of the power loss array (LK → THERM).
     *
     * @return copy of power losses
     */
    public double[] getLkPowerLosses() {
        return lkPowerLosses.clone();
    }

    /**
     * Gets a copy of the temperature array (THERM → LK).
     *
     * @return copy of temperatures
     */
    public double[] getThermTemperatures() {
        return thermTemperatures.clone();
    }

    /**
     * Configure LK→CONTROL voltage monitoring.
     *
     * @param lkNodeIndices array mapping control input index to LK node
     */
    public void configureLkToControlMapping(int[] lkNodeIndices) {
        this.lkNodeForControlInput = lkNodeIndices.clone();
        this.lkNodeVoltagesForControl = new double[lkNodeIndices.length];
    }

    /**
     * Configure CONTROL→LK source signal mapping.
     *
     * @param controlOutputIndices array mapping LK source index to CONTROL output
     */
    public void configureControlToLkSourceMapping(int[] controlOutputIndices) {
        this.controlOutputForLkSource = controlOutputIndices.clone();
        this.controlSignalsForLkSources = new double[controlOutputIndices.length];
    }

    /**
     * Configure CONTROL→LK switch gate signal mapping.
     *
     * @param controlOutputIndices array mapping LK switch index to CONTROL output
     */
    public void configureControlToLkSwitchMapping(int[] controlOutputIndices) {
        this.controlOutputForLkSwitch = controlOutputIndices.clone();
        this.switchGateSignals = new boolean[controlOutputIndices.length];
    }

    /**
     * Configure power loss array size for LK→THERM transfer.
     *
     * @param componentCount number of power-dissipating components
     */
    public void configurePowerLossArray(int componentCount) {
        this.lkPowerLosses = new double[componentCount];
    }

    /**
     * Configure temperature array size for THERM→LK transfer.
     *
     * @param nodeCount number of thermal nodes
     */
    public void configureThermArray(int nodeCount) {
        this.thermTemperatures = new double[nodeCount];
    }
}
