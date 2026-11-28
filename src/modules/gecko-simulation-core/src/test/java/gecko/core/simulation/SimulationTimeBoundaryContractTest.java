package gecko.core.simulation;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class SimulationTimeBoundaryContractTest {
    static Path resistor(String dt,String duration) throws Exception {
        Path file=Files.createTempFile("solver-boundary-",".ipes");
        Files.writeString(file,"tDURATION "+duration+"\ndt "+dt+"\n"+"""
            e
            <ElementLK>
            typ 1
            idStringDialog R1
            parameter[] 10
            labelAnfangsKnoten[] /n
            labelEndKnoten[] /0
            <\\ElementLK>
            """);return file;
    }
    @ParameterizedTest @CsvSource({"0.125,0.5,5,0.5","0.125,0.3,3,0.25","0.5,0.125,1,0"})
    void successfulResultReportsTheLastSolvedSampleInsteadOfTheNextUnsolvedTime(
            double dt,double duration,int steps,double last) throws Exception {
        var engine=new HeadlessSimulationEngine();List<Double> times=new ArrayList<>();
        engine.setStepTraceListener((t,p,i,n)->times.add(t));
        var result=engine.runSimulation(SimulationConfig.builder().circuitFile(resistor(""+dt,""+duration).toString())
            .stepWidth(dt).simulationDuration(duration).build());
        assertTrue(result.isSuccess(),result.getErrorMessage());assertEquals(steps,result.getTotalTimeSteps());
        assertEquals(last,times.getLast(),1e-12);assertEquals(last,result.getSimulatedTime(),1e-12);
        assertEquals(last,engine.getCurrentTime(),1e-12);assertTrue(result.getSimulatedTime()<=duration);
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","NaN","Infinity"})
    void invalidSavedTimeStepIsNotSilentlyReplacedByDefaults(String dt) throws Exception {
        var result=new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
            .circuitFile(resistor(dt,"0.01").toString()).build());
        assertEquals(SimulationResult.Status.FAILED,result.getStatus());assertEquals(0,result.getTotalTimeSteps());
    }
    @Test void anExplicitValidOverrideCanReplaceInvalidSavedTiming() throws Exception {
        var result=new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
            .circuitFile(resistor("NaN","-1").toString()).stepWidth(0.125).simulationDuration(0.25).build());
        assertTrue(result.isSuccess(),result.getErrorMessage());assertEquals(3,result.getTotalTimeSteps());
    }
    @Test void failedRunResetsProgressFromThePreviousSimulation() throws Exception {
        var engine=new HeadlessSimulationEngine();
        assertTrue(engine.runSimulation(SimulationConfig.builder().stepWidth(0.125).simulationDuration(0.25).build()).isSuccess());
        var failed=engine.runSimulation(SimulationConfig.builder().circuitFile("/missing/solver-lifecycle.ipes").build());
        assertEquals(SimulationResult.Status.FAILED,failed.getStatus());
        assertEquals(0,engine.getCurrentTime());assertEquals(0,engine.getCurrentStep());assertEquals(0,engine.getEndTime());
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE,engine.getState());
    }
    @Test
    void cancellationReportsOnlyCommittedSamplesAndAllowsReuse() throws Exception {
        var engine = new HeadlessSimulationEngine();
        List<Double> times = new ArrayList<>();
        engine.setStepTraceListener((t, p, i, n) -> {
            times.add(t);
            if (times.size() == 3) engine.cancel();
        });
        var config = SimulationConfig.builder().circuitFile(resistor("0.125", "1").toString()).build();
        var result = engine.runSimulation(config);
        assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
        assertEquals(3, result.getTotalTimeSteps());
        assertEquals(0.25, result.getSimulatedTime());
        assertEquals(times.getLast(), engine.getCurrentTime());
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
        engine.setStepTraceListener(null);
        assertTrue(engine.runSimulation(config).isSuccess());
    }

    @Test
    void initialProgressHasAFiniteExpectedStepCount() throws Exception {
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((t, p, i, n) -> {
            var progress = engine.getDetailedProgress();
            assertNotNull(progress);
            assertEquals(5, progress.getTotalSteps());
            assertTrue(Double.isFinite(engine.getProgress()));
        });
        assertTrue(engine.runSimulation(SimulationConfig.builder()
                .circuitFile(resistor("0.125", "0.5").toString()).build()).isSuccess());
        assertNull(engine.getDetailedProgress());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "NaN", "Infinity"})
    void invalidSavedDurationIsRejected(String duration) throws Exception {
        var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .circuitFile(resistor("0.125", duration).toString()).build());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void loggingIntervalAndDisabledLoggingDoNotChangeSolvedTime() throws Exception {
        for (boolean enabled : new boolean[]{true, false}) {
            var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                    .circuitFile(resistor("0.125", "0.5").toString())
                    .enableDataLogging(enabled).dataLoggingInterval(2).build());
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertEquals(5, result.getTotalTimeSteps());
            assertEquals(0.5, result.getSimulatedTime());
            assertEquals(enabled ? 3 : 0, result.getTimeArray().length);
        }
    }

}
