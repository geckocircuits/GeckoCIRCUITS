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
import gecko.core.allg.SolverType;
import gecko.core.circuit.losscalculation.SemiconductorLossModel;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.SolverFeatures;
import java.util.ArrayList;
import java.util.List;

/**
 * The saved THERMAL network, solved once per grid step after the electrical step (ADR-0023).
 *
 * <p>It reuses {@link ElectricalTimeStep} on a second netlist. Heat that a loss source computes at step n enters
 * the thermal right-hand side at step n+1, and a loss source reads the junction temperature of the previous step,
 * like the legacy kernel.</p>
 */
final class ThermalNetwork {
    /** One loss source: its heat flow element, its parallel resistor and what it measures. */
    private static final class LossSource {
        final String name;
        final int flow;
        final int resistor;
        final int coupled;
        final SemiconductorLossModel model;
        double temperature;

        LossSource(String name, int flow, int resistor, int coupled, SemiconductorLossModel model) {
            this.name = name;
            this.flow = flow;
            this.resistor = resistor;
            this.coupled = coupled;
            this.model = model;
        }
    }

    private final CircuitNetlist netlist;
    private final ElectricalTimeStep step;
    private final List<LossSource> sources;
    private int committedSteps;

    private ThermalNetwork(CircuitNetlist netlist, ElectricalTimeStep step, List<LossSource> sources) {
        this.netlist = netlist;
        this.step = step;
        this.sources = sources;
    }

    /**
     * @param model the circuit model
     * @param electrical the electrical netlist whose component currents feed the loss sources
     * @param solverType the circuit solver type
     * @return the network, or null when the circuit has no active thermal element
     * @throws IllegalArgumentException if a loss source cannot resolve its coupled component or loss model
     */
    static ThermalNetwork build(CircuitModel model, CircuitNetlist electrical, SolverType solverType) {
        if (model == null || model.getThermalComponents().isEmpty()) return null;
        CircuitNetlist thermal = NetlistBuilder.buildThermalFromCircuitModel(model);
        if (thermal.getElementCount() == 0) return null;
        List<LossSource> sources = new ArrayList<>();
        for (var component : model.getThermalComponents()) {
            if (component.getType() != CircuitTypCore.TH_PvCHIP.getTypeNumber()) continue;
            int flow = thermal.getElementIndexByName(component.getName());
            int resistor = thermal.getElementIndexByName(component.getName() + NetlistBuilder.LOSS_SOURCE_RESISTOR_SUFFIX);
            if (flow < 0 || resistor < 0) continue; // The source is disabled.
            var resolution = ThermalLossSources.resolve(model, component);
            if (resolution.spec() == null) {
                throw new IllegalArgumentException("Loss source '" + component.getName() + "': " + resolution.message());
            }
            int coupled = electrical == null ? -1 : electrical.getElementIndexByName(resolution.spec().coupled().getName());
            if (coupled < 0) {
                throw new IllegalArgumentException("Loss source '" + component.getName()
                        + "' is coupled to a component that is not part of the electrical circuit.");
            }
            sources.add(new LossSource(component.getName(), flow, resistor, coupled, resolution.spec().newModel().get()));
        }
        // The thermal network has no features. It starts from the saved initial conditions, like the legacy kernel.
        return new ThermalNetwork(thermal, new ElectricalTimeStep(thermal, solverType, new SolverFeatures(0, false)), sources);
    }

    /**
     * Solves one grid step, then computes the heat that the next step stamps.
     *
     * @param dt the grid step width
     * @param time the grid time
     * @param electrical the electrical netlist, already holding this step's results
     */
    void advance(double dt, double time, CircuitNetlist electrical) {
        netlist.setSimulationTime(time);
        step.advance(dt, time, null);
        committedSteps++;
        if (sources.isEmpty()) return;
        double[] potentials = electrical.getLastNodeVoltagesRef();
        double[] currents = electrical.getLastComponentCurrentsRef();
        double[] thermalPotentials = netlist.getLastNodeVoltagesRef();
        for (LossSource source : sources) {
            double voltage = potentials[electrical.getNodeX(source.coupled)] - potentials[electrical.getNodeY(source.coupled)];
            source.model.update(currents[source.coupled], voltage, source.temperature, dt);
            netlist.getParameter(source.flow)[1] = source.model.total();
            // The next step reads the junction temperature of this solve, which the matrix just published.
            source.temperature = thermalPotentials[netlist.getNodeY(source.resistor)]
                    - thermalPotentials[netlist.getNodeX(source.resistor)];
        }
    }

    CircuitNetlist netlist() { return netlist; }

    /** @return the number of thermal solves, one per grid step */
    int committedSteps() { return committedSteps; }

    /** @return the loss source with this name, or null */
    LossComponentView loss(String name) {
        for (LossSource source : sources) {
            if (source.name.equals(name)) return new LossComponentView(source.model);
        }
        return null;
    }

    /** Read access to the latest losses of one loss source. */
    record LossComponentView(SemiconductorLossModel model) {
        double total() { return model.total(); }
        double conduction() { return model.conduction(); }
        double switching() { return model.switching(); }
    }
}
