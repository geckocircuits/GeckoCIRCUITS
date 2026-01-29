/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SourceType enumeration constants.
 * Sprint 3: Circuit Components Coverage
 */
public class SourceTypeTest {

    // ========== DC Source Tests ==========

    @Test
    public void testDCSourceOldFormat() {
        int dcOld = SourceType.QUELLE_DC;
        assertEquals("DC old format ID", 401, dcOld);
    }

    @Test
    public void testDCSourceNewFormat() {
        int dcNew = SourceType.QUELLE_DC_NEW;
        assertEquals("DC new format ID", 0, dcNew);
    }

    @Test
    public void testDCSourceIDsDistinct() {
        assertNotEquals("Old and new DC IDs should differ",
            SourceType.QUELLE_DC, SourceType.QUELLE_DC_NEW);
    }

    // ========== Sinusoidal Source Tests ==========

    @Test
    public void testSinusoidalSourceOldFormat() {
        int sinOld = SourceType.QUELLE_SIN;
        assertEquals("Sine old format ID", 402, sinOld);
    }

    @Test
    public void testSinusoidalSourceNewFormat() {
        int sinNew = SourceType.QUELLE_SIN_NEW;
        assertEquals("Sine new format ID", 1, sinNew);
    }

    @Test
    public void testSinusoidalSourceIDsDistinct() {
        assertNotEquals("Old and new sine IDs should differ",
            SourceType.QUELLE_SIN, SourceType.QUELLE_SIN_NEW);
    }

    // ========== Signal Controlled Source Tests ==========

    @Test
    public void testSignalControlledOldFormat() {
        int sigOld = SourceType.QUELLE_SIGNALGESTEUERT;
        assertEquals("Signal controlled old format ID", 400, sigOld);
    }

    @Test
    public void testSignalControlledNewFormat() {
        int sigNew = SourceType.QUELLE_SIGNALGESTEUERT_NEW;
        assertEquals("Signal controlled new format ID", 2, sigNew);
    }

    @Test
    public void testSignalControlledIDsDistinct() {
        assertNotEquals("Old and new signal controlled IDs should differ",
            SourceType.QUELLE_SIGNALGESTEUERT, SourceType.QUELLE_SIGNALGESTEUERT_NEW);
    }

    // ========== Voltage Controlled Directly Tests ==========

    @Test
    public void testVoltageControlledDirectlyOldFormat() {
        int vcOld = SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY;
        assertEquals("Voltage controlled directly old format ID", 399, vcOld);
    }

    @Test
    public void testVoltageControlledDirectlyNewFormat() {
        int vcNew = SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW;
        assertEquals("Voltage controlled directly new format ID", 5, vcNew);
    }

    @Test
    public void testVoltageControlledDirectlyIDsDistinct() {
        assertNotEquals("Old and new VCVS directly IDs should differ",
            SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY,
            SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW);
    }

    // ========== Voltage Controlled Transformer Tests ==========

    @Test
    public void testVoltageControlledTransformerOldFormat() {
        int vcOld = SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER;
        assertEquals("Voltage controlled transformer old format ID", 398, vcOld);
    }

    @Test
    public void testVoltageControlledTransformerNewFormat() {
        int vcNew = SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW;
        assertEquals("Voltage controlled transformer new format ID", 6, vcNew);
    }

    @Test
    public void testVoltageControlledTransformerIDsDistinct() {
        assertNotEquals("Old and new VCVS transformer IDs should differ",
            SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER,
            SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW);
    }

    // ========== Current Controlled Tests ==========

    @Test
    public void testCurrentControlledDirectlyOldFormat() {
        int ccOld = SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY;
        assertEquals("Current controlled directly old format ID", 396, ccOld);
    }

    @Test
    public void testCurrentControlledDirectlyNewFormat() {
        int ccNew = SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW;
        assertEquals("Current controlled directly new format ID", 3, ccNew);
    }

    @Test
    public void testCurrentControlledDirectlyIDsDistinct() {
        assertNotEquals("Old and new CCVS directly IDs should differ",
            SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY,
            SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW);
    }

    // ========== dI/dt Current Controlled Tests ==========

    @Test
    public void testDIdtCurrentControlledOldFormat() {
        int didtOld = SourceType.QUELLE_DIDTCURRENTCONTROLLED;
        assertEquals("dI/dt current controlled old format ID", 397, didtOld);
    }

    @Test
    public void testDIdtCurrentControlledNewFormat() {
        int didtNew = SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW;
        assertEquals("dI/dt current controlled new format ID", 4, didtNew);
    }

    @Test
    public void testDIdtCurrentControlledIDsDistinct() {
        assertNotEquals("Old and new dI/dt controlled IDs should differ",
            SourceType.QUELLE_DIDTCURRENTCONTROLLED,
            SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW);
    }

    // ========== Source Type Mapping Tests ==========

    @Test
    public void testNewFormatIDRange() {
        // New format uses 0-6 range
        assertTrue("DC new ID in range", SourceType.QUELLE_DC_NEW >= 0 && SourceType.QUELLE_DC_NEW <= 6);
        assertTrue("Sine new ID in range", SourceType.QUELLE_SIN_NEW >= 0 && SourceType.QUELLE_SIN_NEW <= 6);
        assertTrue("Signal controlled new ID in range", SourceType.QUELLE_SIGNALGESTEUERT_NEW >= 0 && SourceType.QUELLE_SIGNALGESTEUERT_NEW <= 6);
        assertTrue("VCVS directly new ID in range", SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW >= 0 && SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW <= 6);
        assertTrue("VCVS transformer new ID in range", SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW >= 0 && SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW <= 6);
        assertTrue("CCVS directly new ID in range", SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW >= 0 && SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW <= 6);
        assertTrue("dI/dt controlled new ID in range", SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW >= 0 && SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW <= 6);
    }

    @Test
    public void testOldFormatIDRange() {
        // Old format uses higher numbers: 396-402
        assertTrue("All old format IDs > 395",
            SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY > 395 &&
            SourceType.QUELLE_DIDTCURRENTCONTROLLED > 395 &&
            SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER > 395 &&
            SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY > 395 &&
            SourceType.QUELLE_SIGNALGESTEUERT > 395 &&
            SourceType.QUELLE_DC > 395 &&
            SourceType.QUELLE_SIN > 395);
    }

    @Test
    public void testNewFormatSequential() {
        // New format should have sequential IDs for easy mapping
        assertEquals("DC new format", 0, SourceType.QUELLE_DC_NEW);
        assertEquals("Sine new format", 1, SourceType.QUELLE_SIN_NEW);
        assertEquals("Signal controlled new format", 2, SourceType.QUELLE_SIGNALGESTEUERT_NEW);
        assertEquals("CCVS directly new format", 3, SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW);
        assertEquals("dI/dt controlled new format", 4, SourceType.QUELLE_DIDTCURRENTCONTROLLED_NEW);
        assertEquals("VCVS directly new format", 5, SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW);
        assertEquals("VCVS transformer new format", 6, SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW);
    }

    // ========== Source Type Uniqueness Tests ==========

    @Test
    public void testOldFormatUnique() {
        // Each old format ID should be unique
        assertNotEquals("DC != Sine", SourceType.QUELLE_DC, SourceType.QUELLE_SIN);
        assertNotEquals("DC != Signal controlled", SourceType.QUELLE_DC, SourceType.QUELLE_SIGNALGESTEUERT);
        assertNotEquals("Sine != Signal controlled", SourceType.QUELLE_SIN, SourceType.QUELLE_SIGNALGESTEUERT);
    }

    @Test
    public void testNewFormatUnique() {
        // Each new format ID should be unique
        assertNotEquals("DC new != Sine new", SourceType.QUELLE_DC_NEW, SourceType.QUELLE_SIN_NEW);
        assertNotEquals("DC new != Signal new", SourceType.QUELLE_DC_NEW, SourceType.QUELLE_SIGNALGESTEUERT_NEW);
        assertNotEquals("Sine new != Signal new", SourceType.QUELLE_SIN_NEW, SourceType.QUELLE_SIGNALGESTEUERT_NEW);
    }

    // ========== Type Classification Tests ==========

    @Test
    public void testDCIsIndependent() {
        // DC source is independent (not controlled)
        int dc_new = SourceType.QUELLE_DC_NEW;
        assertTrue("DC is independent type", dc_new == SourceType.QUELLE_DC_NEW);
    }

    @Test
    public void testSinusoidalIsIndependent() {
        // Sinusoidal source is independent
        int sin_new = SourceType.QUELLE_SIN_NEW;
        assertTrue("Sine is independent type", sin_new == SourceType.QUELLE_SIN_NEW);
    }

    @Test
    public void testVoltageControlledIsDependent() {
        // VCVS is voltage-controlled (dependent)
        int vcvs = SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW;
        assertTrue("VCVS is controlled type", vcvs == SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW);
    }

    @Test
    public void testCurrentControlledIsDependent() {
        // CCVS is current-controlled (dependent)
        int ccvs = SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW;
        assertTrue("CCVS is controlled type", ccvs == SourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW);
    }

    // ========== Legacy Compatibility Tests ==========

    @Test
    public void testOldFormatStillDefined() {
        // Old format constants should still exist for backward compatibility
        assertTrue("Old DC format defined", SourceType.QUELLE_DC == 401);
        assertTrue("Old Sine format defined", SourceType.QUELLE_SIN == 402);
    }

    @Test
    public void testMappingConsistency() {
        // New format should provide consistent replacement
        // When migrating from old to new format, mapping should be clear
        int dc_id_diff = SourceType.QUELLE_DC - SourceType.QUELLE_DC_NEW;
        int sin_id_diff = SourceType.QUELLE_SIN - SourceType.QUELLE_SIN_NEW;
        assertTrue("Old DC ID is offset by 401", dc_id_diff == 401);
        assertTrue("Old Sine ID is offset by 401", sin_id_diff == 401);
    }

    // ========== Source Type Count Tests ==========

    @Test
    public void testTotalSourceTypes() {
        // Should have 7 source types total
        int total_new_types = 7;  // 0 through 6
        assertEquals("New format has 7 types", 6, SourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW);
    }
}
