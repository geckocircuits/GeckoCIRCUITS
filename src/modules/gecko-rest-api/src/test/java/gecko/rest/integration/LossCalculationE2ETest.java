package gecko.rest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for loss calculation endpoints.
 * Tests full stack: controller → service → gecko-simulation-core.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LossCalculationE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSwitchingLossEndpoint_fullStack() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
            {
                "current": 10.0,
                "voltage": 600.0,
                "temperature": 125.0,
                "referenceVoltage": 600.0,
                "turnOnEnergy": 0.001,
                "turnOffEnergy": 0.0015
            }
            """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/loss/switching",
            request,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("totalLoss"), "Response should contain totalLoss");
        assertTrue(response.getBody().contains("0.0025"), "Total loss should be 0.0025J");
        assertTrue(response.getBody().contains("simple_switching"), "Method should be simple_switching");
    }

    @Test
    void testConductionLossEndpoint_fullStack() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
            {
                "current": 10.0,
                "onResistance": 0.05,
                "thresholdVoltage": 0.7
            }
            """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/loss/conduction",
            request,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("totalLoss"));
        assertTrue(response.getBody().contains("12.0"), "Total loss should be 12W");
        assertTrue(response.getBody().contains("simple_conduction"));
    }

    @Test
    void testDetailedLossEndpoint_fullStack() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Real-world example: IGBT datasheet curves at 25°C and 125°C
        String requestBody = """
            {
                "current": 15.0,
                "temperature": 75.0,
                "switchingCurves": [
                    {
                        "temperature": 25.0,
                        "blockingVoltage": 600.0,
                        "data": [[0, 10, 20, 30], [0, 0.0008, 0.0020, 0.0035], [0, 0.0012, 0.0028, 0.0048]]
                    },
                    {
                        "temperature": 125.0,
                        "blockingVoltage": 600.0,
                        "data": [[0, 10, 20, 30], [0, 0.0012, 0.0028, 0.0048], [0, 0.0018, 0.0038, 0.0065]]
                    }
                ],
                "conductionCurves": [
                    {
                        "temperature": 25.0,
                        "data": [[0, 0.8, 1.2, 1.5], [0, 10, 20, 30]]
                    },
                    {
                        "temperature": 125.0,
                        "data": [[0, 1.0, 1.5, 1.9], [0, 10, 20, 30]]
                    }
                ]
            }
            """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/loss/detailed",
            request,
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("totalLoss"));
        assertTrue(response.getBody().contains("switchingLoss"));
        assertTrue(response.getBody().contains("conductionLoss"));
        assertTrue(response.getBody().contains("detailed_interpolation"));
    }

    @Test
    void testInvalidRequest_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Invalid: negative current
        String requestBody = """
            {
                "current": -10.0,
                "voltage": 600.0,
                "temperature": 125.0,
                "referenceVoltage": 600.0,
                "turnOnEnergy": 0.001,
                "turnOffEnergy": 0.0015
            }
            """;

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/loss/switching",
            request,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
