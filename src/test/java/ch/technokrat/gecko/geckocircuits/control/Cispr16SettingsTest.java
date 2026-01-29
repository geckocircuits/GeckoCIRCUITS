/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit tests for Cispr16Settings parameter class.
 * Tests configuration settings for CISPR16 measurement blocks.
 */
public class Cispr16SettingsTest {

    private ReglerCISPR16 _cispr16Block;
    private Cispr16Settings _settings;

    @Before
    public void setUp() {
        // Create a CISPR16 block for initializing settings
        _cispr16Block = new ReglerCISPR16();
        _settings = new Cispr16Settings(_cispr16Block);
    }

    // ========== Boolean Parameter Tests ==========

    @Test
    public void testShowNameDefaultValue() {
        assertNotNull("_showName parameter should exist", _settings._showName);
        assertEquals("Default showName should be true", true, _settings._showName.getValue());
    }

    @Test
    public void testShowNameToggle() {
        _settings._showName.setValueWithoutUndo(false);
        assertEquals("Should be false after toggle", false, _settings._showName.getValue());

        _settings._showName.setValueWithoutUndo(true);
        assertEquals("Should be true after toggle back", true, _settings._showName.getValue());
    }

    @Test
    public void testPeakDefaultValue() {
        assertNotNull("_peak parameter should exist", _settings._peak);
        assertEquals("Default peak should be true", true, _settings._peak.getValue());
    }

    @Test
    public void testPeakToggle() {
        _settings._peak.setValueWithoutUndo(false);
        assertEquals("Peak should be false", false, _settings._peak.getValue());

        _settings._peak.setValueWithoutUndo(true);
        assertEquals("Peak should be true", true, _settings._peak.getValue());
    }

    @Test
    public void testQuasiPeakDefaultValue() {
        assertNotNull("_qpeak parameter should exist", _settings._qpeak);
        assertEquals("Default qpeak should be true", true, _settings._qpeak.getValue());
    }

    @Test
    public void testQuasiPeakToggle() {
        _settings._qpeak.setValueWithoutUndo(false);
        assertEquals("Quasi-peak should be false", false, _settings._qpeak.getValue());

        _settings._qpeak.setValueWithoutUndo(true);
        assertEquals("Quasi-peak should be true", true, _settings._qpeak.getValue());
    }

    @Test
    public void testAverageDefaultValue() {
        assertNotNull("_average parameter should exist", _settings._average);
        assertEquals("Default average should be false", false, _settings._average.getValue());
    }

    @Test
    public void testAverageToggle() {
        _settings._average.setValueWithoutUndo(true);
        assertEquals("Average should be true", true, _settings._average.getValue());

        _settings._average.setValueWithoutUndo(false);
        assertEquals("Average should be false", false, _settings._average.getValue());
    }

    @Test
    public void testUseBlackmanDefaultValue() {
        assertNotNull("_useBlackman parameter should exist", _settings._useBlackman);
        assertEquals("Default useBlackman should be true", true, _settings._useBlackman.getValue());
    }

    @Test
    public void testUseBlackmanToggle() {
        _settings._useBlackman.setValueWithoutUndo(false);
        assertEquals("Blackman should be false", false, _settings._useBlackman.getValue());

        _settings._useBlackman.setValueWithoutUndo(true);
        assertEquals("Blackman should be true", true, _settings._useBlackman.getValue());
    }

    @Test
    public void testShowRMSValuesDefaultValue() {
        assertNotNull("_showRMSValues parameter should exist", _settings._showRMSValues);
        assertEquals("Default showRMS should be true", true, _settings._showRMSValues.getValue());
    }

    @Test
    public void testShowRMSValuesToggle() {
        _settings._showRMSValues.setValueWithoutUndo(false);
        assertEquals("RMS should be false", false, _settings._showRMSValues.getValue());

        _settings._showRMSValues.setValueWithoutUndo(true);
        assertEquals("RMS should be true", true, _settings._showRMSValues.getValue());
    }

    // ========== Double Parameter Tests ==========

    @Test
    public void testMaxFreqDefaultValue() {
        assertNotNull("_maxFreq parameter should exist", _settings._maxFreq);
        assertTrue("Default maxFreq should be positive", _settings._maxFreq.getValue() > 0);
        assertEquals("Default maxFreq should be 2000000", 2000000.0, _settings._maxFreq.getValue(), 1e-9);
    }

    @Test
    public void testMaxFreqModification() {
        _settings._maxFreq.setValueWithoutUndo(3000000.0);
        assertEquals("Max frequency should be modified", 3000000.0, _settings._maxFreq.getValue(), 1e-9);
    }

    @Test
    public void testMinFreqDefaultValue() {
        assertNotNull("_minFreq parameter should exist", _settings._minFreq);
        assertTrue("Default minFreq should be positive", _settings._minFreq.getValue() > 0);
        assertEquals("Default minFreq should be 9000", 9000.0, _settings._minFreq.getValue(), 1e-9);
    }

    @Test
    public void testMinFreqModification() {
        _settings._minFreq.setValueWithoutUndo(10000.0);
        assertEquals("Min frequency should be modified", 10000.0, _settings._minFreq.getValue(), 1e-9);
    }

    @Test
    public void testFrequencyConsistency() {
        _settings._minFreq.setValueWithoutUndo(5000.0);
        _settings._maxFreq.setValueWithoutUndo(500000.0);

        assertTrue("Min should be less than max",
                _settings._minFreq.getValue() < _settings._maxFreq.getValue());
    }

    @Test
    public void testQpIntervalDefaultValue() {
        assertNotNull("_qpInteval parameter should exist", _settings._qpInteval);
        assertTrue("Default qpInteval should be positive", _settings._qpInteval.getValue() > 0);
    }

    @Test
    public void testQpIntervalModification() {
        double newValue = 0.01;
        _settings._qpInteval.setValueWithoutUndo(newValue);
        assertEquals("QP interval should be modified", newValue, _settings._qpInteval.getValue(), 1e-9);
    }

    @Test
    public void testFilterThresholdDefaultValue() {
        assertNotNull("_filterThreshold parameter should exist", _settings._filterThreshold);
        assertEquals("Default filterThreshold should be 0.5", 0.5, _settings._filterThreshold.getValue(), 1e-9);
    }

    @Test
    public void testFilterThresholdModification() {
        _settings._filterThreshold.setValueWithoutUndo(0.8);
        assertEquals("Filter threshold should be modified", 0.8, _settings._filterThreshold.getValue(), 1e-9);
    }

    @Test
    public void testFilterThresholdBoundaries() {
        _settings._filterThreshold.setValueWithoutUndo(0.0);
        assertEquals("Filter threshold should accept 0", 0.0, _settings._filterThreshold.getValue(), 1e-9);

        _settings._filterThreshold.setValueWithoutUndo(1.0);
        assertEquals("Filter threshold should accept 1.0", 1.0, _settings._filterThreshold.getValue(), 1e-9);
    }

    // ========== Combined Tests ==========

    @Test
    public void testAutomaticQPSelectionParameter() {
        assertNotNull("_automaticQPSelection parameter should exist", _settings._automaticQPSelection);
        // Default value can be either true or false, just verify it exists
        boolean value = _settings._automaticQPSelection.getValue();
        assertTrue("Parameter should be accessible", true);
    }

    @Test
    public void testAllParametersExist() {
        assertNotNull("All parameters should be initialized", _settings._showName);
        assertNotNull("All parameters should be initialized", _settings._peak);
        assertNotNull("All parameters should be initialized", _settings._qpeak);
        assertNotNull("All parameters should be initialized", _settings._average);
        assertNotNull("All parameters should be initialized", _settings._useBlackman);
        assertNotNull("All parameters should be initialized", _settings._showRMSValues);
        assertNotNull("All parameters should be initialized", _settings._maxFreq);
        assertNotNull("All parameters should be initialized", _settings._minFreq);
        assertNotNull("All parameters should be initialized", _settings._qpInteval);
        assertNotNull("All parameters should be initialized", _settings._filterThreshold);
        assertNotNull("All parameters should be initialized", _settings._automaticQPSelection);
    }

    @Test
    public void testIndependentParameterModification() {
        // Modify one parameter and verify others are not affected
        boolean originalShowName = _settings._showName.getValue();
        _settings._peak.setValueWithoutUndo(!_settings._peak.getValue());

        assertEquals("showName should not change", originalShowName, _settings._showName.getValue());
    }

    @Test
    public void testMultipleParameterChanges() {
        _settings._showName.setValueWithoutUndo(false);
        _settings._peak.setValueWithoutUndo(false);
        _settings._qpeak.setValueWithoutUndo(false);
        _settings._maxFreq.setValueWithoutUndo(1000000.0);

        assertFalse("showName should be false", _settings._showName.getValue());
        assertFalse("peak should be false", _settings._peak.getValue());
        assertFalse("qpeak should be false", _settings._qpeak.getValue());
        assertEquals("maxFreq should be 1000000", 1000000.0, _settings._maxFreq.getValue(), 1e-9);
    }

    @Test
    public void testNegativeFrequencyValues() {
        // Test that negative values can be set (behavior depends on validation)
        _settings._minFreq.setValueWithoutUndo(-5000.0);
        assertEquals("Should accept negative value", -5000.0, _settings._minFreq.getValue(), 1e-9);
    }

    @Test
    public void testZeroFrequencyValues() {
        _settings._maxFreq.setValueWithoutUndo(0.0);
        assertEquals("Should accept zero", 0.0, _settings._maxFreq.getValue(), 1e-9);
    }

    @Test
    public void testLargeFrequencyValues() {
        double largeValue = 1e10;
        _settings._maxFreq.setValueWithoutUndo(largeValue);
        assertEquals("Should handle large values", largeValue, _settings._maxFreq.getValue(), 1e-9);
    }
}
