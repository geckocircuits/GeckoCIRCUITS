package ch.technokrat.gecko.rest.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationResponseTest {

    @Test
    void getResults_returnsDefensiveSnapshot() {
        SimulationResponse response = new SimulationResponse("sim-1");
        response.addResult("time", new double[]{0.0, 1.0});

        Map<String, double[]> snapshot = response.getResults();
        snapshot.get("time")[0] = 42.0;

        Map<String, double[]> nextSnapshot = response.getResults();
        assertEquals(0.0, nextSnapshot.get("time")[0], 1e-12);
    }

    @Test
    void getResults_mapIsUnmodifiable() {
        SimulationResponse response = new SimulationResponse("sim-1");
        response.addResult("v", new double[]{1.0});

        Map<String, double[]> snapshot = response.getResults();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("new", new double[]{2.0}));
    }
}
