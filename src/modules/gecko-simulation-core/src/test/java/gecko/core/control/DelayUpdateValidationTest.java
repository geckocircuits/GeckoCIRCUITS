package gecko.core.control;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelayUpdateValidationTest {
    @Test
    void invalidCapacityAndRepeatedCommitPreservePublishedHistory() {
        for (double delay : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            assertThrows(IllegalArgumentException.class, () -> DelayHistory.validateHistorySize(delay, 0.1));
        }
        var history = new DelayHistory(0.2);
        var update = history.prepareRestart(0.1);
        update.commit();
        history.capture(4);
        assertThrows(IllegalStateException.class, update::commit);
        history.capture(6);
        assertEquals(4, history.publish());
    }
}
