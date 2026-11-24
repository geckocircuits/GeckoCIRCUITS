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
package gecko.core.io;

import gecko.core.allg.SolverType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI-free representation of a parsed GeckoCIRCUITS circuit model.
 * Contains all simulation parameters and component data extracted from .ipes files.
 *
 * <p>This class is designed for headless operation and contains no Swing/AWT dependencies.</p>
 */
public class CircuitModel {
    /** Captured grid geometry. Hidden proxy terminals do not have physical junctions. */
    public record TerminalData(String domain, long sheetIdentifier, CircuitTopology.Side side, int port,
                               int x, int y, boolean geometric) { }
    /** Retains a nonexecuting block that still defines terminal connectivity. */
    public record StructuralComponent(String domain, ComponentData component) { }

    // Simulation parameters
    private double simulationDuration;
    private double timeStep;
    private double preSimulationTime;
    private double preSimulationTimeStep;
    private double pauseTime = -1;
    private SolverType solverType = SolverType.SOLVER_BE;

    // File metadata
    private String filePath;
    private int fileVersion;
    private int uniqueFileId;
    private String creationDate;

    // Display settings (for reference, not used in headless mode)
    private int displayPixels = 16;
    private int fontSize = 12;
    private String fontType = "Arial";
    private int windowWidth = -1;
    private int windowHeight = -1;

    // Components
    private final List<ComponentData> circuitComponents = new ArrayList<>();
    private final List<ComponentData> controlComponents = new ArrayList<>();
    private final List<ComponentData> thermalComponents = new ArrayList<>();
    private final List<ConnectionData> connections = new ArrayList<>();
    private final List<StructuralComponent> structuralComponents = new ArrayList<>();
    private final List<ComponentData> specialComponents = new ArrayList<>();
    private final List<SavedCircuitBlock> savedBlocks = new ArrayList<>();
    private final Map<String, String> savedSettings = new HashMap<>();
    private final List<SavedCircuitBlock.Section> savedSections = new ArrayList<>();
    private final Map<Long, CircuitResource> resources = new HashMap<>();
    private final Map<Long, CircuitResourceFailure> resourceFailures = new HashMap<>();

    public List<ComponentData> getSpecialComponents() { return specialComponents; }
    public List<StructuralComponent> getStructuralComponents() { return structuralComponents; }
    public List<SavedCircuitBlock> getSavedBlocks() { return savedBlocks; }
    public Map<String, String> getSavedSettings() { return savedSettings; }
    public List<SavedCircuitBlock.Section> getSavedSections() { return savedSections; }
    public Map<Long, CircuitResource> getResources() { return resources; }
    public Map<Long, CircuitResourceFailure> getResourceFailures() { return resourceFailures; }

    private final List<ImportIssue> importIssues = new ArrayList<>();
    private final List<ImportIssue> conversionIssues = new ArrayList<>();

    // Optimizer parameters
    private final Map<String, Double> optimizerParameters = new HashMap<>();

    // Signal names
    private String[] dataContainerSignals;

    // Scripting (if present)
    private String scripterCode = "";
    private String scripterImports = "";
    private String scripterDeclarations = "";

    // Constructor
    public CircuitModel() {
    }

    // Getters and setters for simulation parameters

    public double getSimulationDuration() {
        return simulationDuration;
    }

    public void setSimulationDuration(double simulationDuration) {
        this.simulationDuration = simulationDuration;
    }

    public double getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }

    public double getPreSimulationTime() {
        return preSimulationTime;
    }

    public void setPreSimulationTime(double preSimulationTime) {
        this.preSimulationTime = preSimulationTime;
    }

    public double getPreSimulationTimeStep() {
        return preSimulationTimeStep;
    }

    public void setPreSimulationTimeStep(double preSimulationTimeStep) {
        this.preSimulationTimeStep = preSimulationTimeStep;
    }

    public double getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(double pauseTime) {
        this.pauseTime = pauseTime;
    }

    public SolverType getSolverType() {
        return solverType;
    }

    public void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    // File metadata getters/setters

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(int fileVersion) {
        this.fileVersion = fileVersion;
    }

    public int getUniqueFileId() {
        return uniqueFileId;
    }

    public void setUniqueFileId(int uniqueFileId) {
        this.uniqueFileId = uniqueFileId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    // Display settings

    public int getDisplayPixels() {
        return displayPixels;
    }

    public void setDisplayPixels(int displayPixels) {
        this.displayPixels = displayPixels;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontType() {
        return fontType;
    }

    public void setFontType(String fontType) {
        this.fontType = fontType;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    // Component access

    public List<ComponentData> getCircuitComponents() {
        return circuitComponents;
    }

    public List<ImportIssue> getImportIssues() {
        return importIssues;
    }

    /** Records a diagnostic from a source format for serialization with the circuit. */
    public void addConversionIssue(ImportIssue issue) {
        if (!importIssues.contains(issue)) importIssues.add(issue);
        if (!conversionIssues.contains(issue)) conversionIssues.add(issue);
    }

    /** Returns source-format diagnostics. Native parser diagnostics are generated on each read. */
    public List<ImportIssue> getConversionIssues() {
        return conversionIssues.stream().filter(importIssues::contains).toList();
    }

    public void addCircuitComponent(ComponentData component) {
        circuitComponents.add(component);
    }

    public List<ComponentData> getControlComponents() {
        return controlComponents;
    }

    public void addControlComponent(ComponentData component) {
        controlComponents.add(component);
    }

    public List<ComponentData> getThermalComponents() {
        return thermalComponents;
    }

    public void addThermalComponent(ComponentData component) {
        thermalComponents.add(component);
    }

    public List<ConnectionData> getConnections() {
        return connections;
    }

    public void addConnection(ConnectionData connection) {
        connections.add(connection);
    }



    // Optimizer parameters

    public Map<String, Double> getOptimizerParameters() {
        return optimizerParameters;
    }

    public void setOptimizerParameter(String name, double value) {
        optimizerParameters.put(name, value);
    }

    // Signal names

    public String[] getDataContainerSignals() {
        return dataContainerSignals;
    }

    public void setDataContainerSignals(String[] dataContainerSignals) {
        this.dataContainerSignals = dataContainerSignals;
    }

    // Scripting

    public String getScripterCode() {
        return scripterCode;
    }

    public void setScripterCode(String scripterCode) {
        this.scripterCode = scripterCode;
    }

    public String getScripterImports() {
        return scripterImports;
    }

    public void setScripterImports(String scripterImports) {
        this.scripterImports = scripterImports;
    }

    public String getScripterDeclarations() {
        return scripterDeclarations;
    }

    public void setScripterDeclarations(String scripterDeclarations) {
        this.scripterDeclarations = scripterDeclarations;
    }

    // Utility methods

    /**
     * Gets the total number of components in the circuit.
     *
     * @return total component count
     */
    public int getTotalComponentCount() {
        return circuitComponents.size() + controlComponents.size() + thermalComponents.size();
    }

    /**
     * Checks if the circuit model has valid simulation parameters.
     *
     * @return true if parameters are valid
     */
    public boolean hasValidSimulationParameters() {
        return simulationDuration > 0 && timeStep > 0 && timeStep < simulationDuration;
    }

    @Override
    public String toString() {
        return "CircuitModel{" +
                "simulationDuration=" + simulationDuration +
                ", timeStep=" + timeStep +
                ", solverType=" + solverType +
                ", circuitComponents=" + circuitComponents.size() +
                ", controlComponents=" + controlComponents.size() +
                ", thermalComponents=" + thermalComponents.size() +
                ", connections=" + connections.size() +
                '}';
    }

    /**
     * Represents a circuit component's data.
     */
    public static class ComponentData {
        private final int type;
        private final String name;
        private final Map<String, Object> parameters;
        private final int[] position; // x, y coordinates
        private final int orientation;
        private String[] terminalXLabels = new String[0];  // labelAnfangsKnoten[]
        private String[] terminalYLabels = new String[0];  // labelEndKnoten[]
        private String[] parameterStrings = new String[0]; // parameterString[]
        private double[] numericParameters = new double[0]; // parameter[] raw values

        private boolean parsedInput;
        private boolean configurationNormalized;
        private final Map<String, String> importTokens = new HashMap<>();
        private int importedParameterCount = -1;
        private String sourceLocation;
        private SavedCircuitBlock savedBlock;
        private int sourceOrdinal = -1;
        private final List<TerminalData> terminalData = new ArrayList<>();

        public List<TerminalData> getTerminalData() { return terminalData; }
        /** Keeps the original domain-list order when an execution view excludes components. */
        public int getSourceOrdinal() { return sourceOrdinal; }
        public void setSourceOrdinal(int ordinal) { sourceOrdinal = ordinal; }

        /** @return the shared immutable saved configuration, or null when absent */
        public SavedCircuitBlock getSavedBlock() { return savedBlock; }
        /**
         * Retains immutable saved configuration without copying it.
         * @param block the saved configuration, or null to clear it
         */
        public void setSavedBlock(SavedCircuitBlock block) { savedBlock = block; }

        public boolean isParsedInput() { return parsedInput; }
        public void setParsedInput(boolean parsedInput) { this.parsedInput = parsedInput; }
        public boolean isConfigurationNormalized() { return configurationNormalized; }
        public void setConfigurationNormalized(boolean normalized) { this.configurationNormalized = normalized; }
        public Map<String, String> getImportTokens() { return importTokens; }
        public int getImportedParameterCount() { return importedParameterCount; }
        public void setImportedParameterCount(int count) { this.importedParameterCount = count; }
        public String getSourceLocation() { return sourceLocation; }
        public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

        public ComponentData(int type, String name) {
            this.type = type;
            this.name = name;
            this.parameters = new HashMap<>();
            this.position = new int[2];
            this.orientation = 0;
        }

        public ComponentData(int type, String name, int x, int y, int orientation) {
            this.type = type;
            this.name = name;
            this.parameters = new HashMap<>();
            this.position = new int[]{x, y};
            this.orientation = orientation;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameter(String key, Object value) {
            parameters.put(key, value);
        }

        public int[] getPosition() {
            return position;
        }

        public int getOrientation() {
            return orientation;
        }

        public String[] getTerminalXLabels() {
            return terminalXLabels;
        }

        public void setTerminalXLabels(String[] terminalXLabels) {
            this.terminalXLabels = terminalXLabels != null ? terminalXLabels : new String[0];
        }

        public String[] getTerminalYLabels() {
            return terminalYLabels;
        }

        public void setTerminalYLabels(String[] terminalYLabels) {
            this.terminalYLabels = terminalYLabels != null ? terminalYLabels : new String[0];
        }

        public String[] getParameterStrings() {
            return parameterStrings;
        }

        public void setParameterStrings(String[] parameterStrings) {
            this.parameterStrings = parameterStrings != null ? parameterStrings : new String[0];
        }

        public double[] getNumericParameters() {
            return numericParameters;
        }

        public void setNumericParameters(double[] numericParameters) {
            this.numericParameters = numericParameters != null ? numericParameters : new double[0];
        }

        @Override
        public String toString() {
            return "ComponentData{type=" + type + ", name='" + name + "'}";
        }
    }

    /**
     * Represents a connection between components.
     */
    public static class ConnectionData {
        private final String type; // LK, CONTROL, THERMAL
        private final String label;
        private final int[][] points;
        private SavedCircuitBlock savedBlock;

        /** @return the shared immutable saved configuration, or null when absent */
        public SavedCircuitBlock getSavedBlock() { return savedBlock; }
        /**
         * Retains immutable saved configuration without copying it.
         * @param block the saved configuration, or null to clear it
         */
        public void setSavedBlock(SavedCircuitBlock block) { savedBlock = block; }

        public ConnectionData(String type, int[][] points) {
            this(type, points, "");
        }

        public ConnectionData(String type, int[][] points, String label) {
            this.type = type;
            this.points = points;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getType() {
            return type;
        }

        public int[][] getPoints() {
            return points;
        }

        @Override
        public String toString() {
            return "ConnectionData{type='" + type + "', points=" + points.length + "}";
        }
    }
}
