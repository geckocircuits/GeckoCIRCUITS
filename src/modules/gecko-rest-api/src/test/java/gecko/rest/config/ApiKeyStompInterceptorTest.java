package gecko.rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyStompInterceptorTest {
    private ApiKeyProperties properties;
    private ApiKeyStompInterceptor interceptor;

    @BeforeEach
    void configure() {
        properties = new ApiKeyProperties();
        properties.setKeys("valid-key");
        interceptor = new ApiKeyStompInterceptor(properties,
                (id, owner) -> id.equals("owned") && owner.equals(ApiIdentity.keyId("valid-key")));
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void missingAndInvalidConnectKeysAreRejected() {
        for (String key : new String[]{null, "wrong-key"}) {
            StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
            if (key != null) {
                headers.setNativeHeader("X-API-Key", key);
            }
            assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(headers), null));
        }
    }

    @Test
    void aValidConnectionHasAPrincipalWithoutExposingTheKey() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setNativeHeader("X-API-Key", "valid-key");
        Message<?> connected = message(headers);
        assertSame(connected, interceptor.preSend(connected, null));
        assertNotNull(headers.getUser());
        assertNotEquals("valid-key", headers.getUser().getName());
        assertNull(headers.getFirstNativeHeader("X-API-Key"));
        StompHeaderAccessor subscription = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscription.setUser(headers.getUser());
        subscription.setDestination("/topic/simulations/owned");
        assertDoesNotThrow(() -> interceptor.preSend(message(subscription), null));
    }

    @Test
    void foreignUnknownAndWildcardTopicsAreDenied() {
        for (String destination : new String[]{"/topic/simulations/other", "/topic/simulations/*",
                "/topic/**", "/topic/simulations/owned/extra"}) {
            StompHeaderAccessor subscription = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            subscription.setUser(ApiIdentity.forKey("valid-key"));
            subscription.setDestination(destination);
            assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(subscription), null));
        }
    }

    @Test
    void unauthenticatedSubscriptionsAndMessagesAreRejected() {
        for (StompCommand command : new StompCommand[]{StompCommand.SUBSCRIBE, StompCommand.SEND}) {
            StompHeaderAccessor headers = StompHeaderAccessor.create(command);
            headers.setNativeHeader("user", "forged-user");
            assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(headers), null));
        }
    }

    @Test
    void authenticatedClientsCannotPublishBrokerProgress() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setUser(ApiIdentity.forKey("valid-key"));
        headers.setDestination("/topic/simulations/owned");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(headers), null));
    }

    @Test
    void explicitOpenModeStillAllowsConnections() {
        properties.setAuthEnabled(false);
        assertDoesNotThrow(() -> interceptor.preSend(message(StompHeaderAccessor.create(StompCommand.CONNECT)), null));
        properties.setAuthEnabled(true);
        properties.setKeys("");
        assertDoesNotThrow(() -> interceptor.preSend(message(StompHeaderAccessor.create(StompCommand.CONNECT)), null));
    }

    @Test
    void stompConnectionCommandRequiresTheSameCredentials() {
        StompHeaderAccessor missing = StompHeaderAccessor.create(StompCommand.STOMP);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(missing), null));
        StompHeaderAccessor valid = StompHeaderAccessor.create(StompCommand.STOMP);
        valid.setNativeHeader("X-API-Key", "valid-key");
        assertDoesNotThrow(() -> interceptor.preSend(message(valid), null));
        assertNotNull(valid.getUser());
    }
}
