/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package ch.technokrat.gecko.core.io;

import ch.technokrat.gecko.core.allg.SolverType;
import ch.technokrat.gecko.core.circuit.netlist.LabelResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitFileParser.
 */
class CircuitFileParserTest {

    private CircuitFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new CircuitFileParser();
    }

    @Test
    void parse_validContent_extractsSimulationParameters() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            tPAUSE 0.01
            T_pre 0.001
            dt_pre 1e-7
            solverType 1
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(0.02, model.getSimulationDuration(), 1e-10);
        assertEquals(1e-6, model.getTimeStep(), 1e-15);
        assertEquals(0.01, model.getPauseTime(), 1e-10);
        assertEquals(0.001, model.getPreSimulationTime(), 1e-10);
        assertEquals(1e-7, model.getPreSimulationTimeStep(), 1e-15);
        assertEquals(SolverType.SOLVER_TRZ, model.getSolverType());
    }

    @Test
    void parse_backwardEulerSolver_correctType() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            solverType 0
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(SolverType.SOLVER_BE, model.getSolverType());
    }

    @Test
    void parse_gearShichmanSolver_correctType() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            solverType 2
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(SolverType.SOLVER_GS, model.getSolverType());
    }

    @Test
    void parse_displaySettings_extractsCorrectly() throws Exception {
        String content = """
            dpix 20
            fontSize 14
            fontTyp Arial Unicode MS
            fensterWidth 1200
            fensterHeight 900
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(20, model.getDisplayPixels());
        assertEquals(14, model.getFontSize());
        assertEquals("Arial Unicode MS", model.getFontType());
        assertEquals(1200, model.getWindowWidth());
        assertEquals(900, model.getWindowHeight());
    }

    @Test
    void parse_fileMetadata_extractsCorrectly() throws Exception {
        String content = """
            FileVersion 175
            UniqueFileId 12345
            DtStor 2024-01-15
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(175, model.getFileVersion());
        assertEquals(12345, model.getUniqueFileId());
        assertEquals("2024-01-15", model.getCreationDate());
    }

    @Test
    void parse_missingFields_usesDefaults() throws Exception {
        String content = """
            tDURATION 0.05
            dt 2e-6
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(0.05, model.getSimulationDuration(), 1e-10);
        assertEquals(2e-6, model.getTimeStep(), 1e-15);
        assertEquals(-1, model.getPauseTime()); // default
        assertEquals(SolverType.SOLVER_BE, model.getSolverType()); // default
    }

    @Test
    void parse_missingPreSimTimeStep_usesMainTimeStep() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            dt_pre 0
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        // When dt_pre is 0 or negative, it should default to dt
        assertEquals(model.getTimeStep(), model.getPreSimulationTimeStep());
    }

    @Test
    void parse_scripterBlocks_extractsContent() throws Exception {
        String content = """
            <scripterCode>
            double x = 5.0;
            return x * 2;
            <\\scripterCode>
            <scripterImports>
            import java.util.List;
            <\\scripterImports>
            <scripterDeclarations>
            private double value;
            <\\scripterDeclarations>
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertTrue(model.getScripterCode().contains("double x = 5.0"));
        assertTrue(model.getScripterImports().contains("import java.util.List"));
        assertTrue(model.getScripterDeclarations().contains("private double value"));
    }

    @Test
    void parse_scripterBlockTokenWithIndentAndTab_isDiscovered() throws Exception {
        String content = """
            \t<scripterCode>\t
            double x = 7.0;
            \t<\\scripterCode>
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertTrue(model.getScripterCode().contains("double x = 7.0"));
    }

    @Test
    void parse_dataContainerSignals_preservesEmptySlotsAndMapsNix() throws Exception {
        String content = """
            dataContainerSignals[] //sigA/NIX_NIX_NIX//sigB/
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertArrayEquals(new String[]{"", "", "sigA", "", "", "sigB", ""}, model.getDataContainerSignals());
    }

    @Test
    void parse_optimizerValuesWithSlashSeparators_keepsNameValueAlignment() throws Exception {
        String content = """
            optimizerName[] /alpha/NIX_NIX_NIX/beta/gamma
            optimizerValue[] /1.5/99.0/2.5/3.5
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(3, model.getOptimizerParameters().size());
        assertEquals(1.5, model.getOptimizerParameters().get("alpha"), 1e-10);
        assertEquals(2.5, model.getOptimizerParameters().get("beta"), 1e-10);
        assertEquals(3.5, model.getOptimizerParameters().get("gamma"), 1e-10);
    }

    @Test
    void parse_optimizerInvalidValueToken_doesNotShiftLaterAssignments() throws Exception {
        String content = """
            optimizerName[] /alpha/beta/gamma
            optimizerValue[] 1.0 bad 3.0
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(2, model.getOptimizerParameters().size());
        assertEquals(1.0, model.getOptimizerParameters().get("alpha"), 1e-10);
        assertFalse(model.getOptimizerParameters().containsKey("beta"));
        assertEquals(3.0, model.getOptimizerParameters().get("gamma"), 1e-10);
    }

    @Test
    void parse_emptyContent_returnsModelWithDefaults() throws Exception {
        String content = "";

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertNotNull(model);
        assertEquals(SolverType.SOLVER_BE, model.getSolverType());
    }

    @Test
    void parse_fromInputStream_works() throws Exception {
        String content = """
            tDURATION 0.03
            dt 5e-7
            solverType 1
            """;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8));

        CircuitModel model = parser.parse(inputStream, "test.ipes");

        assertEquals(0.03, model.getSimulationDuration(), 1e-10);
        assertEquals(5e-7, model.getTimeStep(), 1e-15);
        assertEquals(SolverType.SOLVER_TRZ, model.getSolverType());
    }

    @Test
    void parse_nonExistentFile_throwsException() {
        assertThrows(FileNotFoundException.class, () -> {
            parser.parse("/nonexistent/path/to/file.ipes");
        });
    }

    @Test
    void hasValidSimulationParameters_valid_returnsTrue() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertTrue(model.hasValidSimulationParameters());
    }

    @Test
    void hasValidSimulationParameters_zeroDuration_returnsFalse() throws Exception {
        String content = """
            tDURATION 0
            dt 1e-6
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertFalse(model.hasValidSimulationParameters());
    }

    @Test
    void hasValidSimulationParameters_dtGreaterThanDuration_returnsFalse() throws Exception {
        String content = """
            tDURATION 1e-6
            dt 1e-5
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertFalse(model.hasValidSimulationParameters());
    }

    @Test
    void getTotalComponentCount_emptyModel_returnsZero() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");

        assertEquals(0, model.getTotalComponentCount());
    }

    @Test
    void circuitModel_addComponents_updatesCount() throws Exception {
        CircuitModel model = new CircuitModel();

        model.addCircuitComponent(new CircuitModel.ComponentData(1, "R1"));
        model.addCircuitComponent(new CircuitModel.ComponentData(2, "C1"));
        model.addControlComponent(new CircuitModel.ComponentData(10, "PI1"));

        assertEquals(3, model.getTotalComponentCount());
        assertEquals(2, model.getCircuitComponents().size());
        assertEquals(1, model.getControlComponents().size());
    }

    @Test
    void circuitModel_toString_includesKeyInfo() throws Exception {
        String content = """
            tDURATION 0.02
            dt 1e-6
            solverType 0
            """;

        CircuitModel model = parser.parse(new BufferedReader(new StringReader(content)), "test.ipes");
        String str = model.toString();

        assertTrue(str.contains("simulationDuration=0.02"));
        assertTrue(str.contains("timeStep=1.0E-6"));
        assertTrue(str.contains("solverType=backward-euler"));
    }

    @Test
    void componentData_storesParametersCorrectly() {
        CircuitModel.ComponentData component = new CircuitModel.ComponentData(1, "R1", 10, 20, 90);

        component.setParameter("resistance", 1000.0);
        component.setParameter("tolerance", 0.05);

        assertEquals(1, component.getType());
        assertEquals("R1", component.getName());
        assertArrayEquals(new int[]{10, 20}, component.getPosition());
        assertEquals(90, component.getOrientation());
        assertEquals(1000.0, component.getParameters().get("resistance"));
        assertEquals(0.05, component.getParameters().get("tolerance"));
    }

    @Test
    void connectionData_storesDataCorrectly() {
        int[][] points = {{0, 0}, {10, 0}, {10, 10}};
        CircuitModel.ConnectionData connection = new CircuitModel.ConnectionData("LK", points);

        assertEquals("LK", connection.getType());
        assertArrayEquals(points, connection.getPoints());
    }

    @Test
    void labelResolver_labelListStaysInSyncAcrossMutations() {
        LabelResolver resolver = new LabelResolver(new String[]{"A", "B"});

        resolver.addLabel("C", 2);
        resolver.removeLabel("B");
        resolver.addLabel("A", 3);
        resolver.removeLabelAtIndex(2);

        String[] labels = resolver.getLabelList();
        assertEquals(4, labels.length);
        assertNull(labels[0]);
        assertNull(labels[1]);
        assertNull(labels[2]);
        assertEquals("A", labels[3]);
    }

    @Test
    void labelResolver_clearResetsLabelListAndMaps() {
        LabelResolver resolver = new LabelResolver(new String[]{"A", "B"});
        resolver.addLabel("C", 3);

        resolver.clear();

        assertEquals(0, resolver.getLabelCount());
        assertEquals(0, resolver.getLabelList().length);
        assertFalse(resolver.hasLabel("A"));
        assertFalse(resolver.hasLabelAtIndex(3));
    }
}
