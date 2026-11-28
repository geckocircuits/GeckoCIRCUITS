package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class ConverterScopePortSixGoldenParityTest {
    private record Case(String circuit, double expectedAverage) { }

    static Stream<Case> converters() {
        return Stream.of(new Case("cuk_simple", goldenAverage("cuk_simple")),
                new Case("sepic_simple", goldenAverage("sepic_simple")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void scopePortSixAverageMatchesLegacyGolden(Case testCase) throws Exception {
        var file = new File("../../test/resources/ipes/education/" + testCase.circuit() + ".ipes");
        assertTrue(file.isFile(), "Missing circuit: " + file.getAbsolutePath());
        var model = new CircuitFileParser().parse(file.getAbsolutePath());
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, result,
                testCase.circuit() + " must remain Ready: " + result.report().diagnostics());

        String scopeSignal;
        try (var metadata = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            scopeSignal = metadata.scopeChannels().stream()
                    .filter(candidate -> candidate.scopeName().equals("SCOPE.1") && candidate.channelIndex() == 6)
                    .findFirst().orElseThrow(() -> new AssertionError("Missing SCOPE.1 port 6 in " + testCase.circuit()))
                    .signalName();
        }

        List<SimulationSession.Sample> samples = new ArrayList<>();
        String[] signalNames;
        try (var host = SessionHost.open(ready.execution(), ExecutionOptions.defaults())) {
            var status = host.run().get();
            assertEquals(SimulationSession.State.COMPLETED, status.state());
            assertEquals(1000, status.committedSteps());
            signalNames = host.signalNames();
            assertEquals(1000, host.drainSamples(samples::add));
        }
        int signalRow = java.util.Arrays.asList(signalNames).indexOf(scopeSignal);
        assertTrue(signalRow >= 0, "Core samples do not contain scope signal " + scopeSignal);
        assertEquals(1000, samples.size());

        int startIndex = 0;
        while (startIndex < samples.size() && samples.get(startIndex).time() < 0.8e-3) startIndex++;
        double weightedSum = 0;
        for (int index = startIndex; index < samples.size() - 1 && samples.get(index).time() < 1.0e-3; index++) {
            double deltaTime = samples.get(index + 1).time() - samples.get(index).time();
            weightedSum += samples.get(index).values()[signalRow] * deltaTime;
        }
        assertTrue(startIndex < samples.size(), testCase.circuit() + " has no samples in the golden window");
        // Mirrors CharacteristicsCalculator: left-sample integration, excludes the final recorded index,
        // and divides by the exact requested window duration.
        double actualAverage = weightedSum / (1.0e-3 - 0.8e-3);
        double relativeDifference = Math.abs(actualAverage - testCase.expectedAverage())
                / Math.abs(testCase.expectedAverage());
        assertTrue(relativeDifference <= 0.01,
                () -> String.format(Locale.ROOT,
                        "%s SCOPE.1 port 6 (%s, mapped core row %d) average: golden=%.12g core=%.12g (%.3f%%)",
                        testCase.circuit(), scopeSignal, signalRow, testCase.expectedAverage(),
                        actualAverage, relativeDifference * 100));
    }

    private static double goldenAverage(String circuit) {
        var file = new File("../../test/resources/golden/" + circuit + ".golden.csv");
        try {
            assertTrue(file.isFile(), "Missing legacy golden reference: " + file.getAbsolutePath());
            return Files.readAllLines(file.toPath()).stream()
                    .filter(line -> line.startsWith("SCOPE.1,6,"))
                    .map(line -> line.split(","))
                    .mapToDouble(parts -> Double.parseDouble(parts[2]))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing SCOPE.1 port 6 golden row: " + circuit));
        } catch (java.io.IOException error) {
            throw new AssertionError("Cannot read legacy golden for " + circuit, error);
        }
    }
}
