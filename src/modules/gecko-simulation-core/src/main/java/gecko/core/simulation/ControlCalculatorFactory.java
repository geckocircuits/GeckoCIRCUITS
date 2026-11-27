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

import gecko.core.circuit.ControlTypCore;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.calculators.*;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.preparation.CircuitConfiguration;

import java.util.*;
import java.util.logging.Logger;

/**
 * Factory that builds a wired control calculator chain from parsed {@code ComponentData}.
 *
 * <p>This class converts the parsed control component data (from .ipes file
 * {@code <ElementCONTROL>} blocks) into a set of {@link AbstractControlCalculatable}
 * instances, wires their signal connections by canonical terminal groups, and returns
 * a ready-to-execute {@link ControlNetlist} along with a {@link ControlBlockMapping}
 * for domain coupling.</p>
 *
 * <p>Mirrors the GUI-side logic in {@code NetzlisteCONTROL} but without any Swing dependencies.</p>
 */
public class ControlCalculatorFactory {

    private static final Logger LOG = Logger.getLogger(ControlCalculatorFactory.class.getName());

    /** Reports a wired CONTROL graph that has an instantaneous dependency cycle. */
    public static final class DependencyCycleException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private DependencyCycleException(List<String> blocked) {
            super("A CONTROL dependency cycle prevents execution of: " + String.join(", ", blocked) + ".");
        }
    }

    /** Reports a TEMP or FLOW probe whose thermal reference is missing or that the core cannot run. */
    public static final class ThermalProbeException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String code;
        private final String component;

        private ThermalProbeException(String code, String component, String message) {
            super("CONTROL block '" + component + "': " + message);
            this.code = code;
            this.component = component;
        }

        /** @return INVALID_THERMAL_PROBE for a missing reference, UNSUPPORTED_CONTROL_COMPONENT for an unsupported one */
        public String code() { return code; }
        public String component() { return component; }
    }

    /** Reports a connected vector width that the selected calculator cannot execute. */
    public static final class InvalidSignalWidthException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String component;

        private InvalidSignalWidthException(String component, IllegalArgumentException cause) {
            super("CONTROL block '" + component + "': " + cause.getMessage(), cause);
            this.component = component;
        }

        /** @return the saved name of the component with an invalid signal width */
        public String component() { return component; }
    }

    /**
     * Result of building the control calculator chain.
     */
    public static class BuildResult {
        private final ControlNetlist controlNetlist;
        private final ControlBlockMapping mapping;
        private final List<FromExternalCalculator> externalInputs;
        private final List<ToExternalCalculator> externalOutputs;

        public BuildResult(ControlNetlist controlNetlist, ControlBlockMapping mapping) {
            this(controlNetlist, mapping, List.of(), List.of());
        }

        BuildResult(ControlNetlist controlNetlist, ControlBlockMapping mapping,
                List<FromExternalCalculator> externalInputs, List<ToExternalCalculator> externalOutputs) {
            this.controlNetlist = controlNetlist;
            this.mapping = mapping;
            this.externalInputs = List.copyOf(externalInputs);
            this.externalOutputs = List.copyOf(externalOutputs);
        }

        public ControlNetlist getControlNetlist() { return controlNetlist; }
        public ControlBlockMapping getMapping() { return mapping; }
        List<FromExternalCalculator> externalInputs() { return externalInputs; }
        List<ToExternalCalculator> externalOutputs() { return externalOutputs; }
    }

    /**
     * Builds and wires CONTROL descriptions with effective numeric settings.
     * Use {@link #buildCircuit(CircuitModel, double)} for saved layouts, named settings, bindings, or resources.
     *
     * @param controlComponents CONTROL descriptions with resolved numeric settings
     * @return a BuildResult containing the wired ControlNetlist and ControlBlockMapping
     * @throws InvalidSignalWidthException if a connected vector has an invalid width
     * @throws DependencyCycleException if the wired blocks contain an instantaneous dependency cycle
     */
    public BuildResult build(List<CircuitModel.ComponentData> controlComponents) {
        return build(controlComponents, 0);
    }

    /**
     * Builds calculators and checks dependencies for the selected step.
     *
     * @param controlComponents CONTROL descriptions with resolved numeric settings
     * @param timeStep the step in seconds, or zero to defer the delay threshold check
     * @return the wired runtime and its coupling and SCOPE mappings
     * @throws IllegalArgumentException if configuration is invalid
     * @throws InvalidSignalWidthException if a connected vector has an invalid width
     * @throws DependencyCycleException if instantaneous dependencies contain a cycle
     */
    public BuildResult build(List<CircuitModel.ComponentData> controlComponents, double timeStep) {
        return build(controlComponents, timeStep, null, null, null);
    }

    /**
     * Builds independent CONTROL calculators from a model and its captured resources.
     * Unresolved saved CONTROL settings use the shared normalizer on a detached model.
     * Prepared overrides and caller data remain unchanged. The method builds the electrical netlist
     * so VOLT terminal labels resolve to their same-sheet node indices.
     *
     * @param model the model with owned resource bytes, or null for an empty runtime
     * @param timeStep the step in seconds, or zero to defer the delay threshold check
     * @return the wired CONTROL runtime and its coupling and SCOPE mappings
     * @throws IllegalArgumentException if configuration or required resources are invalid
     * @throws InvalidSignalWidthException if a connected vector has an invalid width
     * @throws DependencyCycleException if instantaneous dependencies contain a cycle
     */
    public BuildResult buildCircuit(CircuitModel model, double timeStep) {
        CircuitNetlist electricalNetlist = model == null ? null : NetlistBuilder.buildFromCircuitModel(model);
        return buildCircuit(model, timeStep, electricalNetlist);
    }

    public BuildResult buildCircuit(CircuitModel model, double timeStep, CircuitNetlist electricalNetlist) {
        return buildCircuit(model, timeStep, electricalNetlist, null);
    }

    /**
     * Builds the CONTROL runtime with TEMP and FLOW probes resolved against the thermal netlist.
     *
     * @param thermalNetlist the thermal netlist of the circuit, or null when it has no thermal element
     * @throws ThermalProbeException if a TEMP or FLOW block has no usable thermal reference
     */
    public BuildResult buildCircuit(CircuitModel model, double timeStep, CircuitNetlist electricalNetlist,
            CircuitNetlist thermalNetlist) {
        var resolved = model == null ? null : CircuitConfiguration.resolveSavedControl(model);
        return build(resolved == null ? null : resolved.getControlComponents(), timeStep, resolved, electricalNetlist,
                thermalNetlist);
    }

    private BuildResult build(List<CircuitModel.ComponentData> controlComponents, double timeStep, CircuitModel model,
            CircuitNetlist electricalNetlist, CircuitNetlist thermalNetlist) {
        if (controlComponents == null || controlComponents.isEmpty()) {
            var mapping = new ControlBlockMapping();
            addSourceMappings(model, null, Map.of(), mapping);
            return new BuildResult(ControlNetlist.createEmpty(), mapping);
        }

        ControlBlockMapping mapping = new ControlBlockMapping();
        CircuitModel wiringModel = model;
        if (wiringModel == null) {
            wiringModel = new CircuitModel();
            wiringModel.getControlComponents().addAll(controlComponents);
        }
        CircuitTopology topology = CircuitTopology.resolve(wiringModel);

        // Step 1: Create calculator instances for each component
        List<CalculatorEntry> entries = new ArrayList<>();
        for (CircuitModel.ComponentData comp : controlComponents) {
            AbstractControlCalculatable calc = createCalculator(comp, model);
            if (calc != null) {
                entries.add(new CalculatorEntry(comp, calc));
            }
        }

        if (timeStep > 0 && Double.isFinite(timeStep)) {
            for (var entry : entries) {
                if (entry.calculator instanceof DelayCalculator delay) delay.validateTimeStep(timeStep);
            }
        }

        // Step 2: Index output ports by their canonical signal groups.
        Map<Integer, SignalPort> outputMap = new LinkedHashMap<>();
        for (CalculatorEntry entry : entries) {
            String[] yLabels = entry.component.getTerminalYLabels();
            for (int port = 0; port < yLabels.length; port++) {
                var group = topology.group(entry.component, CircuitTopology.Side.OUTPUT, port);
                if (group != null) {
                    outputMap.put(group.id(), new SignalPort(entry.calculator, port));
                }
            }
        }

        addSourceMappings(model, topology, outputMap, mapping);

        // Step 3: Wire input ports through the same canonical signal groups.
        for (CalculatorEntry entry : entries) {
            String[] xLabels = entry.component.getTerminalXLabels();
            for (int inputPort = 0; inputPort < xLabels.length; inputPort++) {
                String label = xLabels[inputPort];
                var group = topology.group(entry.component, CircuitTopology.Side.INPUT, inputPort);
                if (group == null && entry.component.getType() == 22) {
                    group = topology.externalOutputGroup(entry.component, inputPort);
                }
                if (group != null && outputMap.containsKey(group.id())) {
                    SignalPort source = outputMap.get(group.id());
                    try {
                        entry.calculator.setInputSignal(inputPort, source.calculator, source.portIndex);
                    } catch (Exception e) {
                        LOG.warning("Failed to wire signal '" + label + "' to "
                                + entry.component.getName() + " input " + inputPort + ": " + e.getMessage());
                    }
                }
            }
            // Fill unconnected inputs with zero
            for (int i = 0; i < entry.calculator._inputSignal.length; i++) {
                entry.calculator.checkInputWithoutConnectionAndFill(i);
            }
        }

        for (CalculatorEntry entry : entries) {
            try {
                entry.calculator.validateInputSignalWidths();
            } catch (IllegalArgumentException error) {
                throw new InvalidSignalWidthException(entry.component.getName(), error);
            }
        }

        // Step 4: Build probe/gate/scope mappings
        for (CalculatorEntry entry : entries) {
            int type = entry.component.getType();
            if (type == TEMP_TYPE) {
                addTemperatureProbe(model, topology, thermalNetlist, entry, mapping);
                continue;
            }
            if (type == FLOW_TYPE) {
                addFlowProbe(model, thermalNetlist, entry, mapping);
                continue;
            }
            if (!ControlTypCore.isKnown(type)) continue;

            ControlTypCore controlType = ControlTypCore.fromTypeNumber(type);
            String[] paramStrings = entry.component.getParameterStrings();

            switch (controlType) {
                case VOLT -> {
                    // parameterString format: [nodeLabel1, nodeLabel2, flag]
                    int nodeA = 0;
                    int nodeB = 0;
                    if (paramStrings.length >= 1 && !paramStrings[0].isEmpty()) {
                        nodeA = resolveProbeNode(model, electricalNetlist, entry.component, paramStrings[0]);
                    }
                    if (paramStrings.length >= 2 && !paramStrings[1].isEmpty()) {
                        nodeB = resolveProbeNode(model, electricalNetlist, entry.component, paramStrings[1]);
                    }
                    mapping.addVoltProbe(entry.calculator, nodeA, nodeB, entry.component.getName());
                }
                case AMP -> {
                    // parameterString format: [componentName, NIX, flag]
                    String compName = (paramStrings.length >= 1) ? paramStrings[0] : "";
                    mapping.addAmpProbe(entry.calculator, compName, entry.component.getName());
                }
                case GATE -> {
                    // parameterString format: [switchName, NIX, flag]
                    String switchName = (paramStrings.length >= 1) ? paramStrings[0] : "";
                    mapping.addGateMapping(entry.calculator, switchName);
                }
                case SCOPE -> {
                    // Input labels are the signal names
                    String[] xLabels = entry.component.getTerminalXLabels();
                    mapping.addScopeInfo(entry.calculator, xLabels, entry.component.getName(),
                            entry.component.getSavedBlock() == null ? 0 : entry.component.getSavedBlock().uniqueObjectIdentifier());
                }
                default -> { /* SIGNAL, CONST, GAIN — no special mapping needed */ }
            }
        }

        // Execute each signal producer before its consumers.
        ControlNetlist netlist = ControlNetlist.fromOrderedCalculators(dependencyOrder(entries, timeStep, true));
        netlist.setDependencyOrder((step, activatePending) -> dependencyOrder(entries, step, activatePending));
        var externalInputs = orderedExternal(entries, 23, FromExternalCalculator.class);
        var externalOutputs = orderedExternal(entries, 22, ToExternalCalculator.class);
        return new BuildResult(netlist, mapping, externalInputs, externalOutputs);
    }

    private static <T extends AbstractControlCalculatable> List<T> orderedExternal(List<CalculatorEntry> entries,
            int type, Class<T> calculatorType) {
        return entries.stream().filter(entry -> entry.component.getType() == type)
                .sorted(Comparator.comparingInt(entry -> ((Number) entry.component.getParameters()
                        .getOrDefault("externalOrder", entry.component.getSourceOrdinal() < 0
                                ? entries.indexOf(entry) : entry.component.getSourceOrdinal())).intValue()))
                .map(entry -> calculatorType.cast(entry.calculator)).toList();
    }

    private static List<AbstractControlCalculatable> dependencyOrder(List<CalculatorEntry> entries, double dt, boolean activatePending) {
        Map<double[], Integer> producers = new IdentityHashMap<>();
        List<Set<Integer>> consumers = new ArrayList<>();
        int[] dependencies = new int[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            consumers.add(new LinkedHashSet<>());
            for (double[] output : entries.get(index).calculator._outputSignal) {
                producers.put(output, index);
            }
        }
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            if (entry.calculator instanceof PT2Calculator) continue;
            if (entry.calculator instanceof DelayCalculator delay) {
                double delayTime = activatePending ? delay.getConfiguredDelayTime() : delay.getDelayTime();
                if (delayTime > 0 && dt <= delayTime) continue;
            }
            for (double[] input : entries.get(index).calculator._inputSignal) {
                Integer producer = producers.get(input);
                if (producer != null && consumers.get(producer).add(index)) {
                    dependencies[index]++;
                }
            }
        }
        Queue<Integer> ready = new PriorityQueue<>();
        for (int index = 0; index < dependencies.length; index++) {
            if (dependencies[index] == 0) ready.add(index);
        }
        List<AbstractControlCalculatable> sorted = new ArrayList<>();
        while (!ready.isEmpty()) {
            int index = ready.remove();
            sorted.add(entries.get(index).calculator);
            for (int consumer : consumers.get(index)) {
                if (--dependencies[consumer] == 0) ready.add(consumer);
            }
        }
        if (sorted.size() != entries.size()) {
            List<String> blocked = new ArrayList<>();
            for (int index = 0; index < dependencies.length; index++) {
                if (dependencies[index] > 0) blocked.add(entries.get(index).component.getName());
            }
            Collections.sort(blocked);
            throw new DependencyCycleException(blocked);
        }
        return sorted;
    }

    /**
     * Creates the appropriate calculator for a control component.
     */
    private AbstractControlCalculatable createCalculator(CircuitModel.ComponentData comp, CircuitModel model) {
        int type = comp.getType();
        ControlBlockDescriptor descriptor = ControlBlockDescriptor.find(type);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unsupported CONTROL component '" + comp.getName()
                    + "' with type " + type);
        }
        return descriptor.createCalculator(comp, model);
    }

    /**
     * Parses a node label string to an MNA node index.
     * Labels are string-encoded integers (e.g., "1", "2", "3"). "0" = ground.
     */
    static int parseNodeLabel(String label) {
        if (label == null || label.isEmpty()) {
            return 0; // ground
        }
        try {
            return Integer.parseInt(label.trim());
        } catch (NumberFormatException e) {
            return 0; // ground for unrecognized labels
        }
    }

    /**
     * Signal-controlled LK sources read the CONTROL signal named by their first parameter string on their own sheet
     * (desktop SimulationsKern.definiereInteraktion_SignalgesteuerteQuelle_Regler). No such signal holds zero,
     * as does a circuit without CONTROL, where the desktop has zeroed the saved value.
     */
    private static void addSourceMappings(CircuitModel model, CircuitTopology topology,
            Map<Integer, SignalPort> outputMap, ControlBlockMapping mapping) {
        if (model == null) return;
        for (CircuitModel.ComponentData source : model.getCircuitComponents()) {
            double[] numeric = source.getNumericParameters();
            boolean signalMode = numeric.length > 0 && (numeric[0] == 400 || numeric[0] == 2);
            if (!signalMode || (source.getType() != CircuitTypCore.LK_U.getTypeNumber()
                    && source.getType() != CircuitTypCore.LK_I.getTypeNumber())) continue;
            String[] names = source.getParameterStrings();
            String signal = names.length > 0 && names[0] != null ? names[0].trim() : "";
            long sheet = source.getSavedBlock() == null ? 0 : source.getSavedBlock().parentSheetIdentifier();
            var group = topology == null || signal.isEmpty() ? null : topology.labelGroup("CONTROL", sheet, signal);
            SignalPort producer = group == null ? null : outputMap.get(group.id());
            mapping.addSourceMapping(producer == null ? null : producer.calculator,
                    producer == null ? 0 : producer.portIndex, source.getName());
        }
        for (CircuitModel.ComponentData source : model.getThermalComponents()) {
            double[] numeric = source.getNumericParameters();
            boolean signalMode = numeric.length > 0 && (numeric[0] == 400 || numeric[0] == 2);
            if (!signalMode || (source.getType() != CircuitTypCore.TH_FLOW.getTypeNumber()
                    && source.getType() != CircuitTypCore.TH_TEMP.getTypeNumber())) continue;
            String[] names = source.getParameterStrings();
            String signal = names.length > 0 && names[0] != null ? names[0].trim() : "";
            long sheet = source.getSavedBlock() == null ? 0 : source.getSavedBlock().parentSheetIdentifier();
            var group = topology == null || signal.isEmpty() ? null : topology.labelGroup("CONTROL", sheet, signal);
            SignalPort producer = group == null ? null : outputMap.get(group.id());
            mapping.addThermalSourceMapping(producer == null ? null : producer.calculator,
                    producer == null ? 0 : producer.portIndex, source.getName());
        }
    }

    private static final int TEMP_TYPE = 16;
    private static final int FLOW_TYPE = 17;

    private static void addTemperatureProbe(CircuitModel model, CircuitTopology topology, CircuitNetlist thermal,
            CalculatorEntry entry, ControlBlockMapping mapping) {
        var component = entry.component;
        String name = component.getName();
        String[] labels = component.getParameterStrings();
        String first = labels.length > 0 ? labels[0] : "";
        String second = labels.length > 1 ? labels[1] : "";
        if (model == null || thermal == null || thermal.getElementCount() == 0) {
            throw new ThermalProbeException("INVALID_THERMAL_PROBE", name, "The circuit has no thermal network to measure.");
        }
        long sheet = component.getSavedBlock() == null ? 0 : component.getSavedBlock().parentSheetIdentifier();
        if (!first.isEmpty() && !second.isEmpty()) {
            mapping.addThermalTemperatureProbe(entry.calculator, thermalNode(model, topology, thermal, name, sheet, first),
                    thermalNode(model, topology, thermal, name, sheet, second), name);
            return;
        }
        var coupled = coupledThermal(model, component);
        int index = coupled == null || coupled.getType() == CircuitTypCore.TH_PvCHIP.getTypeNumber() ? -1
                : thermal.getElementIndexByName(coupled.getName());
        if (index < 0) {
            throw new ThermalProbeException(coupled != null && coupled.getType() == CircuitTypCore.TH_PvCHIP.getTypeNumber()
                    ? "UNSUPPORTED_CONTROL_COMPONENT" : "INVALID_THERMAL_PROBE", name,
                    "The temperature probe has no supported thermal reference.");
        }
        mapping.addThermalTemperatureProbe(entry.calculator, thermal.getNodeX(index), thermal.getNodeY(index), name);
    }

    private static void addFlowProbe(CircuitModel model, CircuitNetlist thermal, CalculatorEntry entry,
            ControlBlockMapping mapping) {
        var component = entry.component;
        String name = component.getName();
        var coupled = model == null ? null : coupledThermal(model, component);
        if (thermal == null || coupled == null || thermal.getElementIndexByName(coupled.getName()) < 0) {
            throw new ThermalProbeException("INVALID_THERMAL_PROBE", name, "The heat-flow probe has no thermal reference.");
        }
        int type = coupled.getType();
        if (type == CircuitTypCore.TH_PvCHIP.getTypeNumber()) {
            String saved = component.getImportTokens().get("losscomp");
            mapping.addThermalFlowProbe(entry.calculator, coupled.getName(), true,
                    saved == null ? gecko.core.circuit.losscalculation.LossComponent.TOTAL
                            : gecko.core.circuit.losscalculation.LossComponent.getEnumFromSaveString(saved.trim()), name);
        } else if (type == CircuitTypCore.TH_FLOW.getTypeNumber() || type == CircuitTypCore.TH_RTH.getTypeNumber()
                || type == CircuitTypCore.TH_CTH.getTypeNumber() || type == CircuitTypCore.TH_AMBIENT.getTypeNumber()) {
            mapping.addThermalFlowProbe(entry.calculator, coupled.getName(), false, null, name);
        } else {
            throw new ThermalProbeException("UNSUPPORTED_CONTROL_COMPONENT", name,
                    "The core cannot measure the heat flow of thermal component type " + type + ".");
        }
    }

    private static CircuitModel.ComponentData coupledThermal(CircuitModel model, CircuitModel.ComponentData control) {
        String reference = control.getImportTokens().get("coupledReferenceID[]");
        if (reference != null && !reference.isBlank()) {
            try {
                long identifier = Long.parseLong(reference.trim().split("\\s+")[0]);
                for (var thermal : model.getThermalComponents()) {
                    if (thermal.getParameters().get("uniqueObjectIdentifier") instanceof Number number
                            && number.longValue() == identifier) return thermal;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
            return null;
        }
        String[] names = control.getParameterStrings();
        if (names.length > 0) {
            for (var thermal : model.getThermalComponents()) if (names[0].equals(thermal.getName())) return thermal;
        }
        return null;
    }

    /** The thermal netlist node of a label on the probe's sheet, like the legacy findIndexFromLabelInSheet. */
    private static int thermalNode(CircuitModel model, CircuitTopology topology, CircuitNetlist thermal, String probe,
            long sheet, String label) {
        var group = topology.labelGroup("THERMAL", sheet, label);
        if (group != null) {
            for (var component : model.getThermalComponents()) {
                for (var side : CircuitTopology.Side.values()) {
                    var terminal = topology.group(component, side, 0);
                    if (terminal == null || terminal.id() != group.id()) continue;
                    String element = component.getType() == CircuitTypCore.TH_PvCHIP.getTypeNumber()
                            ? component.getName() + NetlistBuilder.LOSS_SOURCE_RESISTOR_SUFFIX : component.getName();
                    int index = thermal.getElementIndexByName(element);
                    if (index >= 0) return side == CircuitTopology.Side.INPUT ? thermal.getNodeX(index) : thermal.getNodeY(index);
                }
            }
        }
        throw new ThermalProbeException("INVALID_THERMAL_PROBE", probe, "The thermal label '" + label + "' is unknown.");
    }

    private static int resolveProbeNode(CircuitModel model, CircuitNetlist electricalNetlist,
            CircuitModel.ComponentData measurement, String label) {
        if (electricalNetlist == null) return parseNodeLabel(label);
        long sheet = measurement.getSavedBlock() == null ? 0 : measurement.getSavedBlock().parentSheetIdentifier();
        for (CircuitModel.ComponentData component : model.getCircuitComponents()) {
            long componentSheet = component.getSavedBlock() == null ? 0 : component.getSavedBlock().parentSheetIdentifier();
            if (componentSheet != sheet) continue;
            int element = electricalNetlist.getElementIndexByName(component.getName());
            if (element < 0) continue;
            for (String terminalLabel : component.getTerminalXLabels()) {
                if (Objects.equals(terminalLabel, label)) return electricalNetlist.getNodeX(element);
            }
            for (String terminalLabel : component.getTerminalYLabels()) {
                if (Objects.equals(terminalLabel, label)) return electricalNetlist.getNodeY(element);
            }
        }
        throw new IllegalArgumentException("Voltage probe '" + measurement.getName()
                + "' references an unknown electrical terminal label '" + label + "'");
    }

    private static class CalculatorEntry {
        final CircuitModel.ComponentData component;
        final AbstractControlCalculatable calculator;

        CalculatorEntry(CircuitModel.ComponentData component, AbstractControlCalculatable calculator) {
            this.component = component;
            this.calculator = calculator;
        }
    }

    private static class SignalPort {
        final AbstractControlCalculatable calculator;
        final int portIndex;

        SignalPort(AbstractControlCalculatable calculator, int portIndex) {
            this.calculator = calculator;
            this.portIndex = portIndex;
        }
    }
}
