package gecko.core.allg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperatingModeTest {

    @Test
    void testEnumValues() {
        OperatingMode[] modes = OperatingMode.values();
        assertEquals(6, modes.length, "Should have 6 operating modes");
    }

    @Test
    void testStandaloneMode() {
        assertEquals(OperatingMode.STANDALONE, OperatingMode.valueOf("STANDALONE"));
    }

    @Test
    void testSimulinkMode() {
        assertEquals(OperatingMode.SIMULINK, OperatingMode.valueOf("SIMULINK"));
    }

    @Test
    void testExternalMode() {
        assertEquals(OperatingMode.EXTERNAL, OperatingMode.valueOf("EXTERNAL"));
    }

    @Test
    void testRemoteMode() {
        assertEquals(OperatingMode.REMOTE, OperatingMode.valueOf("REMOTE"));
    }

    @Test
    void testMMFMode() {
        assertEquals(OperatingMode.MMF, OperatingMode.valueOf("MMF"));
    }

    @Test
    void testHeadlessMode() {
        assertEquals(OperatingMode.HEADLESS, OperatingMode.valueOf("HEADLESS"));
    }

    @Test
    void testValueOfThrowsExceptionForInvalidMode() {
        assertThrows(IllegalArgumentException.class, () -> {
            OperatingMode.valueOf("INVALID_MODE");
        });
    }

    @Test
    void testEnumOrdering() {
        OperatingMode[] modes = OperatingMode.values();
        assertEquals(OperatingMode.STANDALONE, modes[0]);
        assertEquals(OperatingMode.SIMULINK, modes[1]);
        assertEquals(OperatingMode.EXTERNAL, modes[2]);
        assertEquals(OperatingMode.REMOTE, modes[3]);
        assertEquals(OperatingMode.MMF, modes[4]);
        assertEquals(OperatingMode.HEADLESS, modes[5]);
    }
}
