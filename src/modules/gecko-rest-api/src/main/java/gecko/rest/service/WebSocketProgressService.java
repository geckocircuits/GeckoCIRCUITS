package gecko.rest.service;

import gecko.rest.model.SimulationProgressMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Broadcasts simulation progress updates to WebSocket subscribers.
 *
 * <p>Clients subscribe to {@code /topic/simulations/{simulationId}}
 * to receive real-time progress messages.</p>
 */
@Service
public class WebSocketProgressService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketProgressService.class);
    private static final String TOPIC_PREFIX = "/topic/simulations/";

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketProgressService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts a progress update to all subscribers of the simulation topic.
     *
     * @param simulationId the simulation identifier
     * @param progress     progress percentage 0-100
     * @param currentTime  current simulation time in seconds
     * @param endTime      target end time in seconds
     * @param step         current step number
     * @param totalSteps   total expected steps
     * @param status       current status string
     */
    public void broadcastProgress(String simulationId, double progress,
                                   double currentTime, double endTime,
                                   int step, int totalSteps, String status) {
        SimulationProgressMessage message = new SimulationProgressMessage(
                simulationId, progress, currentTime, endTime, step, totalSteps, status);
        String destination = TOPIC_PREFIX + simulationId;
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.debug("WebSocket broadcast failed for {}: {}", simulationId, e.getMessage());
        }
    }

    /**
     * Broadcasts a completion or failure event.
     *
     * @param simulationId the simulation identifier
     * @param success      true if completed, false if failed
     * @param errorMessage optional error message if failed
     */
    public void broadcastCompletion(String simulationId, boolean success, String errorMessage) {
        String status = success ? "COMPLETED" : "FAILED";
        SimulationProgressMessage message = new SimulationProgressMessage(
                simulationId, success ? 100.0 : 0.0, 0, 0, 0, 0, status);
        message.setErrorMessage(errorMessage);
        String destination = TOPIC_PREFIX + simulationId;
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.debug("WebSocket completion broadcast failed for {}: {}", simulationId, e.getMessage());
        }
    }

    /**
     * Returns the WebSocket topic destination for a simulation.
     * Clients should subscribe to this destination.
     *
     * @param simulationId the simulation identifier
     * @return STOMP topic destination string
     */
    public String getTopicDestination(String simulationId) {
        return TOPIC_PREFIX + simulationId;
    }
}
