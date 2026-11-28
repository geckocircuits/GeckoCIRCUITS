package gecko.geckocircuits.circuit;

import gecko.geckocircuits.allg.SolverType;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTraceLoggerTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    Path directory;

    @Test
    void writesGzipCsvWithStableColumnsAndDeviceStatesAcrossSteps() throws Exception {
        CircuitTyp[] types = {CircuitTyp.LK_S, CircuitTyp.LK_MOSFET, CircuitTyp.LK_D};
        double[][] parameters = {{0.1, 0.1, 1000.0}, {0.2, 5.0, 0.2, 2000.0}, {0.3}};
        NetListLK netlist = netlist(types, parameters);
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        matrices.matrixSize = 2;
        matrices.p = new double[]{0.0, 12.5};
        matrices.iALT = new double[]{1.0, 2.0, 3.0};
        Path output = directory.resolve("trace.csv.gz");

        try (SimulationTraceLogger logger = new SimulationTraceLogger(output.toFile())) {
            logger.logStep(0.001, matrices, netlist);
            parameters[0][0] = 1000.0;
            parameters[1][0] = 2000.0;
            logger.logStep(0.002, matrices, netlist);
        }

        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(output)), StandardCharsets.UTF_8))) {
            lines = reader.lines().toList();
        }
        assertEquals(3, lines.size());
        assertEquals("time,p[0],p[1],i[0],i[1],i[2],R_sw_elem0,gate_elem0,R_sw_elem1,gate_elem1,R_d_elem2", lines.get(0));
        String[] onValues = lines.get(1).split(",");
        String[] offValues = lines.get(2).split(",");
        assertEquals(11, onValues.length);
        assertEquals(0.001, Double.parseDouble(onValues[0]));
        assertEquals(12.5, Double.parseDouble(onValues[2]));
        assertEquals(3.0, Double.parseDouble(onValues[5]));
        assertEquals(1.0, Double.parseDouble(onValues[7]));
        assertEquals(1.0, Double.parseDouble(onValues[9]));
        assertEquals(0.3, Double.parseDouble(onValues[10]));
        assertEquals(0.0, Double.parseDouble(offValues[7]));
        assertEquals(0.0, Double.parseDouble(offValues[9]));
    }

    @Test
    void writesDecimalPointsWhenDefaultLocaleUsesDecimalCommas() throws Exception {
        Locale previous = Locale.getDefault();
        Path output = directory.resolve("trace.csv");
        try {
            Locale.setDefault(Locale.GERMANY);
            LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
            matrices.matrixSize = 1;
            matrices.p = new double[]{1.25};
            matrices.iALT = new double[]{2.5};
            try (SimulationTraceLogger logger = new SimulationTraceLogger(output.toFile())) {
                logger.logStep(0.5, matrices, netlist(new CircuitTyp[]{CircuitTyp.LK_R}, new double[][]{{2.0}}));
            }
        } finally {
            Locale.setDefault(previous);
        }
        String[] values = Files.readAllLines(output).get(1).split(",");
        assertEquals(3, values.length);
        assertEquals(0.5, Double.parseDouble(values[0]));
        assertEquals(1.25, Double.parseDouble(values[1]));
        assertEquals(2.5, Double.parseDouble(values[2]));
    }

    @Test
    void exportsSolvedVoltageSourceCurrentInsteadOfUnusedHistorySlot() throws Exception {
        NetListLK netlist = netlist(new CircuitTyp[]{CircuitTyp.LK_U}, new double[][]{{401, 12}});
        netlist.knotenMAX = 1;
        netlist.spgQuelleNr[0] = 1;
        LKMatrices matrices = new LKMatrices(SolverType.SOLVER_BE);
        matrices.matrixSize = 3;
        matrices.p = new double[]{0, 12, -0.25};
        matrices.iALT = new double[]{0};
        Path output = directory.resolve("source-current.csv");

        try (SimulationTraceLogger logger = new SimulationTraceLogger(output.toFile())) {
            logger.logStep(0, matrices, netlist);
        }

        assertEquals(-0.25, Double.parseDouble(Files.readAllLines(output).get(1).split(",")[4]));
    }

    @Test
    void refusesToReplaceAnExistingTrace() throws Exception {
        Path output = directory.resolve("existing.csv");
        Files.writeString(output, "preserved trace");
        assertThrows(FileAlreadyExistsException.class, () -> new SimulationTraceLogger(output.toFile()));
        assertEquals("preserved trace", Files.readString(output));
    }

    private static NetListLK netlist(CircuitTyp[] types, double[][] parameters) throws Exception {
        Constructor<NetListLK> constructor = NetListLK.class.getDeclaredConstructor(
                CircuitTyp[].class, int[].class, int[].class, double[][].class, int[].class);
        constructor.setAccessible(true);
        return constructor.newInstance(types, new int[types.length], new int[types.length],
                parameters, new int[types.length]);
    }
}
