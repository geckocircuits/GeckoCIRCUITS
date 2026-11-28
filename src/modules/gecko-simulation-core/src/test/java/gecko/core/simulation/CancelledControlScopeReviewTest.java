package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class CancelledControlScopeReviewTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void cancelledResultsKeepScopePortMappingsAndRecordedTimes(int interval) {
        var model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(1);
        model.addControlComponent(component(3, "source", new double[]{2}, new String[0], "drive"));
        model.addControlComponent(component(5, "first", new double[0], new String[]{"drive", "", "drive"}));
        model.addControlComponent(component(5, "second", new double[0], new String[]{"drive"}));
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        var engine = new HeadlessSimulationEngine();
        var result = engine.runSimulation(ready.execution(), new ExecutionOptions(true, interval),
                () -> engine.getCurrentStep() >= 3);
        assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
        assertEquals(3, result.getTotalTimeSteps());
        assertEquals(0.2, result.getSimulatedTime(), 1e-12);
        assertArrayEquals(interval == 1 ? new double[]{0, 0.1, 0.2} : new double[]{0, 0.2}, result.getTimeArray(), 1e-12);
        assertArrayEquals(interval == 1 ? new float[]{2, 2, 2} : new float[]{2, 2}, result.getSignalData(0));
        assertArrayEquals(interval == 1 ? new float[]{0, 0, 0} : new float[]{0, 0}, result.getSignalData(1));
        @SuppressWarnings("unchecked")
        var channels = (List<ScopeDataExtractor.ScopeChannel>) result.getMetadata().get("scopeChannels");
        assertNotNull(channels, "Partial results must retain SCOPE channel identity.");
        assertEquals(List.of("first", "first", "first", "second"), channels.stream()
                .map(ScopeDataExtractor.ScopeChannel::scopeName).toList());
        assertEquals(List.of(0, 1, 2, 0), channels.stream().map(ScopeDataExtractor.ScopeChannel::channelIndex).toList());
        assertEquals(List.of(0, 1, 0, 0), channels.stream().map(ScopeDataExtractor.ScopeChannel::row).toList());
    }
}
