package gecko.rest.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

/** Authenticates STOMP before a client can subscribe to simulation progress. */
public final class ApiKeyStompInterceptor implements ChannelInterceptor {
    private final ApiKeyProperties properties;
    private final java.util.function.BiPredicate<String, String> ownsSimulation;

    public ApiKeyStompInterceptor(ApiKeyProperties properties) {
        this(properties, (id, owner) -> false);
    }

    public ApiKeyStompInterceptor(ApiKeyProperties properties,
            java.util.function.BiPredicate<String, String> ownsSimulation) {
        this.properties = properties;
        this.ownsSimulation = ownsSimulation;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (accessor.getCommand() == StompCommand.CONNECT || accessor.getCommand() == StompCommand.STOMP) {
            String key = accessor.getFirstNativeHeader(ApiKeyAuthFilter.API_KEY_HEADER);
            if (!properties.isValidKey(key)) {
                throw new AccessDeniedException("Valid X-API-Key CONNECT header required");
            }
            accessor.removeNativeHeader(ApiKeyAuthFilter.API_KEY_HEADER);
            // Never expose the credential as a principal name.
            accessor.setUser(ApiIdentity.forKey(key));
        } else if (accessor.getCommand() != StompCommand.DISCONNECT
                && properties.isAuthEnabled() && !properties.getKeys().isEmpty()
                && accessor.getUser() == null) {
            throw new AccessDeniedException("Authenticated STOMP session required");
        }
        if (properties.isAuthEnabled() && !properties.getKeys().isEmpty()) {
            String destination = accessor.getDestination();
            if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
                String prefix = "/topic/simulations/";
                if (destination == null || !destination.startsWith(prefix)
                        || accessor.getUser() == null
                        || !ownsSimulation.test(destination.substring(prefix.length()), accessor.getUser().getName())) {
                    throw new AccessDeniedException("Subscribe to your own simulation topic.");
                }
            }
            if (accessor.getCommand() == StompCommand.SEND
                    && destination != null && destination.startsWith("/topic/")) {
                throw new AccessDeniedException("Clients cannot publish simulation progress.");
            }
        }
        return message;
    }
}
