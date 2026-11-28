package gecko.core.circuit.netlist;

import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class SavedNetlistValidationContractTest {
    @ParameterizedTest
    @CsvSource({"floating-a,floating-b","NIX_NIX_NIX,NIX_NIX_NIX"})
    void invalidSavedTopologyFailsTheEngineBeforePublishingResults(String x,String y) throws Exception {
        String content = """
                tDURATION 0.002
                dt 0.001
                e
                <ElementLK>
                typ 1
                idStringDialog bad-R
                parameter[] /2.0
                labelAnfangsKnoten[] /%s
                labelEndKnoten[] /%s
                <\\ElementLK>
                """.formatted(x,y);
        // Preserve this diagnostic input instead of deleting generated files.
        Path file=Files.createTempFile("gecko-invalid-topology-",".ipes");
        Files.writeString(file,content);
        SimulationResult result=new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder().circuitFile(file.toString()).build());
        assertEquals(SimulationResult.Status.FAILED,result.getStatus());
        assertTrue(result.getErrorMessage().contains("bad-R"),result.getErrorMessage());
        assertEquals(0,result.getTotalTimeSteps());
    }
}
