package gecko.rest.controller;

import gecko.rest.service.WebSocketProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller providing WebSocket subscription information.
 *
 * <p>The actual WebSocket endpoint is at {@code /ws} (STOMP/SockJS)
 * or {@code /ws-raw} (raw WebSocket).</p>
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulation WebSocket", description = "WebSocket subscription information for real-time streaming")
public class SimulationWebSocketController {

    private final WebSocketProgressService webSocketProgressService;

    @Autowired
    public SimulationWebSocketController(WebSocketProgressService webSocketProgressService) {
        this.webSocketProgressService = webSocketProgressService;
    }

    @GetMapping("/{simulationId}/ws-info")
    @Operation(summary = "Get WebSocket subscription info",
               description = "Returns the STOMP topic destination to subscribe to for real-time updates")
    public ResponseEntity<Map<String, String>> getWebSocketInfo(
            @PathVariable String simulationId) {
        String destination = webSocketProgressService.getTopicDestination(simulationId);
        return ResponseEntity.ok(Map.of(
                "stompEndpoint", "/ws",
                "rawWebSocketEndpoint", "/ws-raw",
                "subscribeDestination", destination,
                "simulationId", simulationId,
                "protocol", "STOMP",
                "note", "Connect to /ws via SockJS then subscribe to the destination"
        ));
    }
}
