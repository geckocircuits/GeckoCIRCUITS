package gecko.rest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP over SockJS.
 *
 * <p>Clients connect to /ws and subscribe to /topic/simulations/{id}
 * to receive real-time simulation progress events.</p>
 *
 * <p>Message format (JSON):</p>
 * <pre>
 * {
 *   "simulationId": "uuid",
 *   "progress": 45.0,
 *   "currentTime": 0.009,
 *   "endTime": 0.02,
 *   "step": 9000,
 *   "totalSteps": 20000,
 *   "status": "RUNNING"
 * }
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable in-memory message broker for /topic destinations
        registry.enableSimpleBroker("/topic");
        // Application destinations prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // Raw WebSocket endpoint (for non-browser clients)
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*");
    }
}
