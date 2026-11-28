package gecko.core.io.importer;

import gecko.core.ac.AcComponent;
import gecko.core.ac.AcResult;
import gecko.core.ac.Complex;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** LTspice B elements and the stock bv and bi symbols import to the opt-in behavioural law (ADR-0033). */
class BehaviouralImportTest {
    /**
     * Archive id 438 (Tof/Test files/Transformer coupling.asc) with its XFMR subcircuit flattened by hand, and the
     * derived 1 ohm load that puts B1 in the loop.
     */
    private static final String TRANSFORMER = """
            * Transformer coupling, X1 = XFMR flattened
            .param k=0.995
            .param L1=606u
            .param L2=37.5u
            .param Lf={L1*(1-k**2)}
            .param Lm={L1*k**2}
            .param N={(1/k)*sqrt(L2/L1)}
            V2 in 0 SINE(0 1 100k)
            VIV1 in N001 0
            R2 N002 N001 10m
            Lf N002 N003 {Lf}
            Lm N003 0 {Lm}
            R1 out1 0 1
            BX1 N003 0 I=I(VX1)*{N}
            BX2 X1N001 0 V=V(N003,0)*{N}
            VX1 X1N001 X1N002 0
            RX1 out1 X1N002 1u
            RX2 N003 0 1Meg
            .tran 0 100u 0 100n
            """;

    @TempDir Path directory;

    @Test
    void bElementsStayUnsupportedWithoutTheOption() {
        var model = new SpiceImporter().convert(TRANSFORMER, "transformer.cir");
        assertEquals(List.of("BX1", "BX2"), model.getConversionIssues().stream()
                .filter(issue -> issue.code().equals("UNSUPPORTED_ELEMENT")).map(issue -> issue.component()).toList());
    }

    @Test
    void flatTransientTransformerKeepsTheSameStepAndTheLoadedAmplitude() throws Exception {
        var model = new SpiceImporter().withBehaviouralSources().convert(TRANSFORMER, "transformer.cir");
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var law = (AcComponent.Behavioural) component(model, "BX2").getAcComponent().law();
        String n003 = component(model, "Lm").getTerminalXLabels()[0];
        assertEquals("V(" + n003 + ",0)*{N}", law.expression());
        model.setSolverType(SolverType.SOLVER_TRZ);
        model.setSimulationDuration(model.getSimulationDuration() + model.getTimeStep());
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        var names = Arrays.asList(result.getSignalNames());
        float[] out1 = result.getSignalData(names.indexOf("V_" + component(model, "R1").getTerminalXLabels()[0]));
        float[] node = result.getSignalData(names.indexOf("V_" + n003));
        double[] time = result.getTimeArray();
        double ratio = (1 / 0.995) * Math.sqrt(37.5 / 606), peak = 0;
        for (int i = 0; i < time.length; i++) {
            // B2 in the same step: V(out1) = N V(N003) R1 / (R1 + 1u).
            assertEquals(ratio * node[i] / (1 + 1e-6), out1[i], 1e-6 + 1e-5 * Math.abs(out1[i]), "t=" + time[i]);
            if (time[i] >= 80e-6) peak = Math.max(peak, Math.abs(out1[i]));
        }
        // The imported SINE source runs through CONTROL and lags by its own steps, so compare the loaded amplitude.
        List<String> reference;
        try (InputStream stream = getClass().getResourceAsStream("/behavioural/438r1-ltspice.csv")) {
            reference = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().skip(1).toList();
        }
        double expected = reference.stream().map(line -> line.split(","))
                .filter(fields -> Double.parseDouble(fields[0]) >= 80e-6)
                .mapToDouble(fields -> Math.abs(Double.parseDouble(fields[1]))).max().orElseThrow();
        assertEquals(expected, peak, 0.01 * expected);
    }

    @Test
    void stockBvAndBiSymbolsSolveTheirLawsInAc() throws Exception {
        for (String name : List.of("voltage", "current", "res", "bv", "bi")) {
            try (var resource = getClass().getResourceAsStream("/ltspice-hierarchy/" + name + ".asy")) {
                Files.copy(java.util.Objects.requireNonNull(resource), directory.resolve(name + ".asy"));
            }
        }
        var input = Files.writeString(directory.resolve("behavioural.asc"), """
                Version 4
                SHEET 1 880 680
                FLAG 0 16 in
                FLAG 0 96 0
                FLAG 160 16 out
                FLAG 160 96 0
                FLAG 256 16 out
                FLAG 256 96 0
                FLAG 400 0 cur
                FLAG 400 80 0
                SYMBOL voltage 0 0 R0
                SYMATTR InstName V1
                SYMATTR Value AC 1
                SYMBOL bv 160 0 R0
                SYMATTR InstName B1
                SYMATTR Value V=V(IN)*2
                SYMBOL res 240 0 R0
                SYMATTR InstName R1
                SYMATTR Value 1k
                SYMBOL bi 400 0 R0
                SYMATTR InstName B2
                SYMATTR Value I=V(in)*1m
                SYMBOL res 384 -16 R0
                SYMATTR InstName R2
                SYMATTR Value 1k
                TEXT 0 200 Left 2 !.ac dec 1 1k 1k
                """);
        var plain = new LtspiceImporter().convert(input, directory);
        assertTrue(plain.getConversionIssues().stream().anyMatch(issue -> issue.code().equals("UNSUPPORTED_SYMBOL")));
        var model = new LtspiceImporter().withBehaviouralSources().convert(input, directory);
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var result = ready.execution().acPlan().execute();
        assertEquals(new Complex(2, 0), voltage(result, "out"));
        assertEquals(-1, voltage(result, "cur").real(), 1e-12);
    }

    private static Complex voltage(AcResult result, String label) {
        return result.groups().stream().filter(group -> group.aliases().stream().anyMatch(label::equalsIgnoreCase))
                .findFirst().orElseThrow(() -> new AssertionError("no node " + label)).voltageV().getFirst();
    }

    private static CircuitModel.ComponentData component(CircuitModel model, String name) {
        return model.getCircuitComponents().stream().filter(data -> data.getName().equals(name)).findFirst().orElseThrow();
    }
}
