package gecko.rest.service;

import gecko.rest.model.SimulationProgressMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketProgressServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketProgressService service;

    @BeforeEach
    void setUp() {
        service = new WebSocketProgressService(messagingTemplate);
    }

    @Test
    void testBroadcastProgressSendsToCorrectTopicDestination() {
        String simulationId = "test-sim-123";
        
        service.broadcastProgress(simulationId, 50.0, 0.01, 0.02, 5000, 10000, "RUNNING");

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), any(SimulationProgressMessage.class));
        
        assertEquals("/topic/simulations/test-sim-123", destinationCaptor.getValue());
    }

    @Test
    void testBroadcastProgressCreatesMessageWithCorrectProgress() {
        String simulationId = "test-sim-456";
        double expectedProgress = 75.5;
        
        service.broadcastProgress(simulationId, expectedProgress, 0.015, 0.02, 7500, 10000, "RUNNING");

        ArgumentCaptor<SimulationProgressMessage> messageCaptor = ArgumentCaptor.forClass(SimulationProgressMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), messageCaptor.capture());
        
        SimulationProgressMessage message = messageCaptor.getValue();
        assertEquals(expectedProgress, message.getProgress());
    }

    @Test
    void testBroadcastProgressCreatesMessageWithCorrectSimulationId() {
        String simulationId = "test-sim-789";
        
        service.broadcastProgress(simulationId, 25.0, 0.005, 0.02, 2500, 10000, "RUNNING");

        ArgumentCaptor<SimulationProgressMessage> messageCaptor = ArgumentCaptor.forClass(SimulationProgressMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), messageCaptor.capture());
        
        SimulationProgressMessage message = messageCaptor.getValue();
        assertEquals(simulationId, message.getSimulationId());
    }

    @Test
    void testBroadcastCompletionSendsCompletedStatusWhenSuccess() {
        String simulationId = "test-sim-complete";
        
        service.broadcastCompletion(simulationId, true, null);

        ArgumentCaptor<SimulationProgressMessage> messageCaptor = ArgumentCaptor.forClass(SimulationProgressMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), messageCaptor.capture());
        
        SimulationProgressMessage message = messageCaptor.getValue();
        assertEquals("COMPLETED", message.getStatus());
        assertEquals(100.0, message.getProgress());
    }

    @Test
    void testBroadcastCompletionSendsFailedStatusWhenNotSuccess() {
        String simulationId = "test-sim-failed";
        
        service.broadcastCompletion(simulationId, false, "Simulation error");

        ArgumentCaptor<SimulationProgressMessage> messageCaptor = ArgumentCaptor.forClass(SimulationProgressMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), messageCaptor.capture());
        
        SimulationProgressMessage message = messageCaptor.getValue();
        assertEquals("FAILED", message.getStatus());
    }

    @Test
    void testBroadcastCompletionSetsErrorMessageWhenFailed() {
        String simulationId = "test-sim-error";
        String expectedError = "Circuit parsing failed";
        
        service.broadcastCompletion(simulationId, false, expectedError);

        ArgumentCaptor<SimulationProgressMessage> messageCaptor = ArgumentCaptor.forClass(SimulationProgressMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), messageCaptor.capture());
        
        SimulationProgressMessage message = messageCaptor.getValue();
        assertEquals(expectedError, message.getErrorMessage());
    }

    @Test
    void testGetTopicDestinationReturnsCorrectFormat() {
        String simulationId = "test-sim-topic";
        
        String destination = service.getTopicDestination(simulationId);

        assertEquals("/topic/simulations/test-sim-topic", destination);
    }

    @Test
    void testBroadcastProgressHandlesMessagingExceptionGracefully() {
        String simulationId = "test-sim-exception";
        doThrow(new RuntimeException("Messaging failed"))
                .when(messagingTemplate).convertAndSend(anyString(), any(SimulationProgressMessage.class));
        
        // Should not throw
        assertDoesNotThrow(() -> service.broadcastProgress(simulationId, 50.0, 0.01, 0.02, 5000, 10000, "RUNNING"));
        
        verify(messagingTemplate).convertAndSend(anyString(), any(SimulationProgressMessage.class));
    }
}
