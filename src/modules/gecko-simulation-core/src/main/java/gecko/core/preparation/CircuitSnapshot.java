package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.SimulationBlockClassification;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Objects;

/** Owns a circuit. Every inspection returns a detached model. */
public final class CircuitSnapshot {
    private final CircuitModel model;
    CircuitSnapshot(CircuitModel source) {
        model = copy(Objects.requireNonNull(source, "Circuit is required"));
        SimulationBlockClassification.retainSpecialComponentIssues(model);
    }
    /**
     * Returns a detached inspection model, including detached arrays, maps, and connections.
     * Changes to this model cannot change the snapshot or another inspection view.
     *
     * @return a writable copy of the captured circuit
     */
    public CircuitModel toModel() { return copy(model); }

    private record Owner(String domain, String name) { }

    /** Creates the common active view for readiness and one numerical run. */
    CircuitModel executionModel() {
        var execution = toModel();
        var topology = CircuitTopology.resolve(execution);
        execution.getConnections().removeIf(connection -> !topology.isActiveConnection(connection));
        var active = new LinkedHashSet<>(topology.activeComponents());
        var structural = new LinkedHashSet<>(topology.structuralComponents());
        var removedOwners = new LinkedHashSet<Owner>();
        var activeOwners = new LinkedHashSet<Owner>();
        retainActive("LK", execution.getCircuitComponents(), topology, active, structural, execution, removedOwners, activeOwners);
        retainActive("CONTROL", execution.getControlComponents(), topology, active, structural, execution, removedOwners, activeOwners);
        retainActive("THERMAL", execution.getThermalComponents(), topology, active, structural, execution, removedOwners, activeOwners);
        retainActive("SPECIAL", execution.getSpecialComponents(), topology, active, structural, execution, removedOwners, activeOwners);
        var subcircuitLocations = new LinkedHashSet<String>();
        execution.getSpecialComponents().stream().filter(component -> component.getType() == 27)
                .map(CircuitModel.ComponentData::getSourceLocation).filter(Objects::nonNull)
                .forEach(subcircuitLocations::add);
        execution.getImportIssues().removeIf(issue -> {
            var owner = new Owner(issue.domain(), issue.component());
            return removedOwners.contains(owner) && !activeOwners.contains(owner)
                    || issue.code().equals("UNSUPPORTED_SIMULATION_BLOCK") && "SPECIAL".equals(issue.domain())
                        && subcircuitLocations.contains(issue.sourceLocation());
        });
        return execution;
    }

    private static void retainActive(String domain, List<CircuitModel.ComponentData> components,
            CircuitTopology topology, Set<CircuitTopology.ComponentIdentity> active,
            Set<CircuitTopology.ComponentIdentity> structural, CircuitModel execution,
            Set<Owner> removedOwners, Set<Owner> activeOwners) {
        components.removeIf(component -> {
            var owner = new Owner(domain, component.getName());
            if (active.contains(topology.identity(component))) {
                activeOwners.add(owner);
                return false;
            }
            if (structural.contains(topology.identity(component))) {
                execution.getStructuralComponents().add(new CircuitModel.StructuralComponent(domain, component));
                activeOwners.add(owner);
                return true;
            }
            removedOwners.add(owner);
            return true;
        });
    }

    private static CircuitModel copy(CircuitModel source) {
        var target = new CircuitModel();
        target.setAcSettings(source.getAcSettings());
        target.setRootParameterDefinitions(source.getRootParameterDefinitions());
        target.setHierarchy(source.getHierarchy());
        target.setAcProvenance(source.getAcProvenance());
        target.setSimulationDuration(source.getSimulationDuration());
        target.setTimeStep(source.getTimeStep());
        target.setPreSimulationTime(source.getPreSimulationTime());
        target.setPreSimulationTimeStep(source.getPreSimulationTimeStep());
        target.setPauseTime(source.getPauseTime());
        target.setSolverType(source.getSolverType());
        target.setFilePath(source.getFilePath());
        target.setFileVersion(source.getFileVersion());
        target.setUniqueFileId(source.getUniqueFileId());
        target.setCreationDate(source.getCreationDate());
        target.setDisplayPixels(source.getDisplayPixels());
        target.setFontSize(source.getFontSize());
        target.setFontType(source.getFontType());
        target.setWindowWidth(source.getWindowWidth());
        target.setWindowHeight(source.getWindowHeight());
        target.setScripterCode(source.getScripterCode());
        target.setScripterImports(source.getScripterImports());
        target.setScripterDeclarations(source.getScripterDeclarations());
        target.getOptimizerParameters().putAll(source.getOptimizerParameters());
        target.getImportIssues().addAll(source.getImportIssues());
        source.getSavedTransientSettingIssues().forEach(target::addSavedTransientSettingIssue);
        source.getConversionIssues().forEach(target::addConversionIssue);
        target.getSavedBlocks().addAll(source.getSavedBlocks());
        target.getSavedSettings().putAll(source.getSavedSettings());
        target.getSavedSections().addAll(source.getSavedSections());
        target.getResources().putAll(source.getResources());
        target.getResourceFailures().putAll(source.getResourceFailures());
        if (source.getDataContainerSignals() != null) target.setDataContainerSignals(source.getDataContainerSignals().clone());
        copyComponents(source.getCircuitComponents(), target.getCircuitComponents());
        copyComponents(source.getControlComponents(), target.getControlComponents());
        copyComponents(source.getThermalComponents(), target.getThermalComponents());
        copyComponents(source.getSpecialComponents(), target.getSpecialComponents());
        for (var structural : source.getStructuralComponents()) {
            var components = new ArrayList<CircuitModel.ComponentData>();
            copyComponents(List.of(structural.component()), components);
            target.getStructuralComponents().add(new CircuitModel.StructuralComponent(structural.domain(), components.getFirst()));
        }
        for (var connection : source.getConnections()) {
            var copy = new CircuitModel.ConnectionData(connection.getType(),
                    (int[][]) copyValue(connection.getPoints()), connection.getLabel());
            copy.setSavedBlock(connection.getSavedBlock());
            target.addConnection(copy);
        }
        return target;
    }

    private static void copyComponents(List<CircuitModel.ComponentData> source, List<CircuitModel.ComponentData> target) {
        for (var component : source) {
            int[] position = component.getPosition();
            var copy = new CircuitModel.ComponentData(component.getType(), component.getName(), position[0], position[1], component.getOrientation());
            copy.setAcComponent(component.getAcComponent());
            component.getParameters().forEach((key, value) -> copy.setParameter(key, copyValue(value)));
            copy.setTerminalXLabels(component.getTerminalXLabels().clone());
            copy.setTerminalYLabels(component.getTerminalYLabels().clone());
            copy.setParameterStrings(component.getParameterStrings().clone());
            copy.setNumericParameters(component.getNumericParameters().clone());
            copy.setScalarBindings(component.getScalarBindings());
            copy.setParsedInput(component.isParsedInput());
            copy.getImportTokens().putAll(component.getImportTokens());
            copy.setConfigurationNormalized(component.isConfigurationNormalized());
            copy.setImportedParameterCount(component.getImportedParameterCount());
            copy.setSourceLocation(component.getSourceLocation());
            copy.setSavedBlock(component.getSavedBlock());
            copy.setSourceOrdinal(component.getSourceOrdinal() < 0 ? target.size() : component.getSourceOrdinal());
            copy.getTerminalData().addAll(component.getTerminalData());
            target.add(copy);
        }
    }

    private static Object copyValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Character
                || value instanceof Enum<?> || value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof Float || value instanceof Double
                || value instanceof BigInteger || value instanceof BigDecimal) return value;
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object copy = Array.newInstance(value.getClass().getComponentType(), length);
            for (int i = 0; i < length; i++) Array.set(copy, i, copyValue(Array.get(value, i)));
            return copy;
        }
        if (value instanceof Map<?, ?> values) {
            var copy = new LinkedHashMap<>();
            values.forEach((key, item) -> copy.put(copyValue(key), copyValue(item)));
            return copy;
        }
        if (value instanceof List<?> values) {
            var copy = new ArrayList<>();
            values.forEach(item -> copy.add(copyValue(item)));
            return copy;
        }
        if (value instanceof Set<?> values) {
            var copy = new LinkedHashSet<>();
            values.forEach(item -> copy.add(copyValue(item)));
            return copy;
        }
        throw new IllegalArgumentException("Unsupported mutable circuit value: " + value.getClass().getName());
    }
}
