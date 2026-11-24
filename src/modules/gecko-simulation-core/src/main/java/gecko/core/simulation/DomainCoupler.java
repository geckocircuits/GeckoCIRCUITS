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

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.AbstractControlCalculatable;

import java.util.List;

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

    // ControlBlockMapping-based probe/gate coupling
    private ControlBlockMapping controlBlockMapping;

    /** Gate signal parameter index in CircuitNetlist parameter array */
    private static final int PARAM_GATE = 8;

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
            lkNodeVoltagesForControl[i] = nodeIdx >= 0 && nodeIdx < nodeVoltages.length
                    ? nodeVoltages[nodeIdx] : 0.0;
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
            controlSignalsForLkSources[i] = ctrlIdx >= 0 && ctrlIdx < controlOutputValues.length
                    ? controlOutputValues[ctrlIdx] : 0.0;
        }
        for (int i = 0; i < controlOutputForLkSwitch.length; i++) {
            int ctrlIdx = controlOutputForLkSwitch[i];
            switchGateSignals[i] = ctrlIdx >= 0 && ctrlIdx < controlOutputValues.length
                    && controlOutputValues[ctrlIdx] > AbstractControlCalculatable.SIGNAL_THRESHOLD;
        }
    }

    /**
     * Performs a complete domain coupling cycle for one time step.
     * Order: LK→CONTROL probes → execute CONTROL → CONTROL→LK gates
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
        // 1. Transfer LK data to CONTROL probes (VOLT/AMP)
        transferLkToControl(circuitNetlist);
        transferLkToControlProbes(circuitNetlist);

        // 2. Execute CONTROL calculators (reads LK voltages, produces outputs)
        if (controlNetlist != null && controlNetlist.hasCalculators()) {
            controlNetlist.executeTimeStep(dt, time);
        }

        // 3. Transfer CONTROL gate signals back to LK switches
        transferGateSignalsToLk(circuitNetlist);
    }

    /**
     * Configures this coupler with the control block mapping from the factory.
     *
     * @param mapping the control block mapping containing probe/gate/scope info
     */
    public void configureFromMapping(ControlBlockMapping mapping) {
        this.controlBlockMapping = mapping;
    }

    /**
     * No-op kept for backward compatibility. Switch parameters are now read
     * directly from the netlist parameter array each step.
     *
     * @param circuitNetlist unused
     * @deprecated No longer needed — R_ON/R_OFF are read from params per component type.
     */
    @Deprecated
    public void saveInitialSwitchParameters(CircuitNetlist circuitNetlist) {
        // No-op: switch R_ON/R_OFF are now read from params each step in applyGateSignals()
    }

    /**
     * Applies gate signals to switch resistance, matching the proven
     * {@code checkForSwitchAction()} method from SimulationsKern.
     *
     * <p>For each gate mapping, reads the gate calculator's input signal and
     * sets the switch's current resistance {@code params[0]} based on the component type:
     * <ul>
     *   <li>LK_S: {@code params[0] = gate > 0.5 ? params[1] : params[2]}</li>
     *   <li>LK_MOSFET: {@code params[0] = gate > 0.5 ? params[2] : params[3]}</li>
     *   <li>LK_THYR/LK_IGBT: sets {@code params[8]} gate flag only (ON/OFF handled by diode iteration)</li>
     * </ul>
     *
     * <p>Must be called BEFORE building the A matrix so the solve uses the correct resistance.
     *
     * @param circuitNetlist the circuit netlist containing switch parameters
     * @return true if any switch resistance changed (caller should rebuild A matrix)
     */
    public boolean applyGateSignals(CircuitNetlist circuitNetlist) {
        if (controlBlockMapping == null || circuitNetlist == null) {
            return false;
        }

        boolean anyChanged = false;
        List<ControlBlockMapping.GateMapping> gates = controlBlockMapping.getGateMappings();

        for (ControlBlockMapping.GateMapping gate : gates) {
            AbstractControlCalculatable gateCalc = gate.getCalculator();
            double gateValue = readGateValue(gateCalc);

            String switchName = gate.getSwitchName();
            int switchIndex = circuitNetlist.getElementIndexByName(switchName);
            if (switchIndex < 0) continue;

            double[] params = circuitNetlist.getParameter(switchIndex);
            CircuitTypCore type = circuitNetlist.getType(switchIndex);
            int requiredLength = switch (type) {
                case LK_S -> 3;
                case LK_MOSFET -> 4;
                case LK_THYR, LK_IGBT -> PARAM_GATE + 1;
                default -> 1;
            };
            if (params == null || params.length < requiredLength) continue;

            double oldR = params[0];

            switch (type) {
                case LK_S:
                    // Proven: par[0] = schaltSignal > 0.5 ? par[1] : par[2]
                    params[0] = (gateValue > 0.5) ? params[1] : params[2];
                    break;
                case LK_MOSFET:
                    // Proven: par[0] = schaltSignal > 0.5 ? par[2] : par[3]
                    params[0] = (gateValue > 0.5) ? params[2] : params[3];
                    break;
                case LK_THYR:
                case LK_IGBT:
                    // Gate only — actual ON/OFF switching handled by diode iteration
                    params[PARAM_GATE] = (gateValue > 0.5) ? 1.0 : 0.0;
                    break;
                default:
                    break;
            }

            if (params[0] != oldR) {
                anyChanged = true;
            }
        }

        return anyChanged;
    }

    /**
     * Transfers LK node voltages to VOLT probe outputs and
     * LK component currents to AMP probe outputs.
     *
     * <p>VOLT probe: reads {@code parameterString} → resolves to MNA node indices →
     * computes {@code V = p[nodeA] - p[nodeB]} → writes to probe's {@code _outputSignal[0][0]}</p>
     *
     * <p>AMP probe: reads {@code parameterString} → resolves component name to element index →
     * reads current from stored results → writes to probe's {@code _outputSignal[0][0]}</p>
     */
    void transferLkToControlProbes(CircuitNetlist circuitNetlist) {
        if (controlBlockMapping == null || circuitNetlist == null) {
            return;
        }

        double[] nodeVoltages = circuitNetlist.getLastNodeVoltagesRef();
        double[] componentCurrents = circuitNetlist.getLastComponentCurrentsRef();

        // VOLT probes: V = p[nodeA] - p[nodeB]
        for (ControlBlockMapping.VoltProbeMapping probe : controlBlockMapping.getVoltProbeMappings()) {
            int nodeA = probe.getNodeA();
            int nodeB = probe.getNodeB();
            double vA = (nodeA > 0 && nodeA < nodeVoltages.length) ? nodeVoltages[nodeA] : 0.0;
            double vB = (nodeB > 0 && nodeB < nodeVoltages.length) ? nodeVoltages[nodeB] : 0.0;
            probe.getCalculator()._outputSignal[0][0] = vA - vB;
        }

        // AMP probes: I = iALT[elementIndex]
        for (ControlBlockMapping.AmpProbeMapping probe : controlBlockMapping.getAmpProbeMappings()) {
            String compName = probe.getComponentName();
            int elementIndex = circuitNetlist.getElementIndexByName(compName);
            // The legacy LKMatrices current pass does not assign _currentInAmps for LK_U.
            // Keep AMP output parity even though the core solver retains the physical MNA current.
            probe.getCalculator()._outputSignal[0][0] = elementIndex >= 0
                    && elementIndex < componentCurrents.length
                    && circuitNetlist.getType(elementIndex) != CircuitTypCore.LK_U
                            ? componentCurrents[elementIndex] : 0.0;
        }
    }

    /**
     * Stores CONTROL gate signal values into switch parameter[8].
     *
     * <p>This only records the gate value — it does NOT change the switch resistance.
     * The resistance is applied at the start of the next time step by
     * {@link #applyGateSignals(CircuitNetlist)}.
     */
    /**
     * Writes TEMP probes as the thermal potential difference and FLOW probes as the heat flow of this step.
     * The thermal node potentials are differences, so no node counts as ground here.
     */
    void transferThermalToControlProbes(ThermalNetwork thermal) {
        if (controlBlockMapping == null || thermal == null) return;
        CircuitNetlist netlist = thermal.netlist();
        double[] potentials = netlist.getLastNodeVoltagesRef();
        double[] currents = netlist.getLastComponentCurrentsRef();
        for (var probe : controlBlockMapping.getThermalTemperatureProbes()) {
            probe.calculator()._outputSignal[0][0] = potentials[probe.nodeA()] - potentials[probe.nodeB()];
        }
        for (var probe : controlBlockMapping.getThermalFlowProbes()) {
            double value;
            if (probe.lossSource()) {
                var loss = thermal.loss(probe.elementName());
                value = loss == null ? 0 : switch (probe.part()) {
                    case CONDUCTION -> loss.conduction();
                    case SWITCHING -> loss.switching();
                    default -> loss.total();
                };
            } else {
                int index = netlist.getElementIndexByName(probe.elementName());
                value = index >= 0 && index < currents.length ? currents[index] : 0;
            }
            probe.calculator()._outputSignal[0][0] = value;
        }
    }

    /** Writes each signal-controlled thermal source value (parameter 1) from its CONTROL output. */
    void transferControlToThermalSources(ThermalNetwork thermal) {
        if (controlBlockMapping == null || thermal == null) return;
        for (var source : controlBlockMapping.getThermalSourceMappings()) {
            int index = thermal.netlist().getElementIndexByName(source.sourceName());
            if (index < 0) continue;
            thermal.netlist().getParameter(index)[1] = source.producer() == null
                    ? 0 : source.producer()._outputSignal[source.output()][0];
        }
    }

    /**
     * Writes each signal-controlled source value (parameter 1) from its CONTROL output,
     * as desktop {@code SimulationsKern.setControlledSourcesFromControlValue}.
     */
    void transferControlToSources(CircuitNetlist circuitNetlist) {
        if (controlBlockMapping == null || circuitNetlist == null) return;
        for (var source : controlBlockMapping.getSourceMappings()) {
            int index = circuitNetlist.getElementIndexByName(source.sourceName());
            if (index < 0) continue;
            circuitNetlist.getParameter(index)[1] = source.producer() == null
                    ? 0 : source.producer()._outputSignal[source.output()][0];
        }
    }

    /** Exports the held CONTROL value of each signal-controlled source. It only reads. */
    void exportHeldSources(CircuitNetlist circuitNetlist, PeriodicStateMonitor.Sink sink) {
        sink.group(PeriodicStateMonitor.Unit.CONTROL, "held source value");
        if (controlBlockMapping == null || circuitNetlist == null) return;
        for (var source : controlBlockMapping.getSourceMappings()) {
            int index = circuitNetlist.getElementIndexByName(source.sourceName());
            if (index >= 0) sink.value(circuitNetlist.getParameter(index)[1]);
        }
    }

    void transferGateSignalsToLk(CircuitNetlist circuitNetlist) {
        if (controlBlockMapping == null || circuitNetlist == null) {
            return;
        }

        List<ControlBlockMapping.GateMapping> gates = controlBlockMapping.getGateMappings();
        for (ControlBlockMapping.GateMapping gate : gates) {
            AbstractControlCalculatable gateCalc = gate.getCalculator();
            double gateValue = readGateValue(gateCalc);

            String switchName = gate.getSwitchName();
            int switchIndex = circuitNetlist.getElementIndexByName(switchName);
            if (switchIndex >= 0) {
                double[] params = circuitNetlist.getParameter(switchIndex);
                if (params != null && params.length > PARAM_GATE) {
                    params[PARAM_GATE] = gateValue;
                }
            }
        }
    }

    private static double readGateValue(AbstractControlCalculatable calculator) {
        if (calculator._inputSignal.length == 0 || calculator._inputSignal[0] == null
                || calculator._inputSignal[0].length == 0) {
            return 0.0;
        }
        double value = calculator._inputSignal[0][0];
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Gate signal must be finite");
        }
        return value;
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
