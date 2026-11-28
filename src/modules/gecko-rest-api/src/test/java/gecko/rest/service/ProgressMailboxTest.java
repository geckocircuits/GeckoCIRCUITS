package gecko.rest.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ProgressMailboxTest {
    @Test
    void pendingUpdatesAreCoalescedAndTheTerminalEventIsOrderedOnce() {
        Queue<Runnable> tasks = new ArrayDeque<>();
        List<Integer> delivered = new ArrayList<>();
        ProgressMailbox mailbox = new ProgressMailbox(tasks::add);
        for (int i = 0; i < 10000; i++) {
            int value = i;
            mailbox.progress(() -> delivered.add(value));
        }
        mailbox.terminal(() -> delivered.add(10000));
        mailbox.terminal(() -> delivered.add(-1));
        mailbox.progress(() -> delivered.add(-2));
        assertEquals(1, tasks.size());
        tasks.remove().run();
        assertEquals(List.of(9999, 10000), delivered);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void aFailedTransportDoesNotPreventTerminalDelivery() {
        Queue<Runnable> tasks = new ArrayDeque<>();
        List<String> delivered = new ArrayList<>();
        ProgressMailbox mailbox = new ProgressMailbox(tasks::add);
        mailbox.progress(() -> { throw new IllegalStateException("disconnected"); });
        mailbox.terminal(() -> delivered.add("terminal"));
        tasks.remove().run();
        assertEquals(List.of("terminal"), delivered);
    }

    @Test
    void shutdownOrOverloadCannotThrowIntoTheSimulationThread() {
        ProgressMailbox mailbox = new ProgressMailbox(task -> { throw new RejectedExecutionException(); });
        assertDoesNotThrow(() -> mailbox.progress(() -> fail("not delivered")));
        assertDoesNotThrow(() -> mailbox.terminal(() -> fail("not delivered")));
    }
}
