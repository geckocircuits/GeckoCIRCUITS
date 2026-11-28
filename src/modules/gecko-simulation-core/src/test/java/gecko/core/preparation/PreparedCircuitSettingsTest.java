package gecko.core.preparation;

import gecko.core.allg.SolverType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class PreparedCircuitSettingsTest {
    @ParameterizedTest
    @CsvSource({"0.5, 0.025, 0.15", "-0.75, 0.0, -1.0"})
    void capturedPreCalculationAndPauseSettingsStayDetached(double preTime, double preStep, double pause) {
        var source = CircuitPreparationTest.resistorCircuit();
        source.setSolverType(SolverType.SOLVER_GS);
        source.setPreSimulationTime(preTime);
        source.setPreSimulationTimeStep(preStep);
        source.setPauseTime(pause);
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(source);
        source.setPreSimulationTime(99);
        source.setPreSimulationTimeStep(98);
        source.setPauseTime(97);
        var prepared = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults())).execution();
        var settings = prepared.solverSettings();
        assertAll(
                () -> assertEquals(SolverType.SOLVER_GS, settings.getSolverType()),
                () -> assertEquals(0.1, settings.getStepWidth()),
                () -> assertEquals(0.2, settings.getSimulationDuration()),
                () -> assertEquals(preTime, settings.getPreCalculationTime()),
                () -> assertEquals(preStep, settings.getPreCalculationStepWidth()),
                () -> assertEquals(pause, settings.getPauseTime()));
        settings.withPreCalculation(96, 95).withPauseTime(94).withStepWidth(93);
        var next = prepared.solverSettings();
        assertNotSame(settings, next);
        assertAll(
                () -> assertEquals(preTime, next.getPreCalculationTime()),
                () -> assertEquals(preStep, next.getPreCalculationStepWidth()),
                () -> assertEquals(pause, next.getPauseTime()),
                () -> assertEquals(0.1, next.getStepWidth()));
    }
}
