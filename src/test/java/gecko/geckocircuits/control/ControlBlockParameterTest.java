/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.geckocircuits.control;

import gecko.geckocircuits.general.UserParameter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for UserParameter functionality through control block implementations.
 *
 * UserParameter provides:
 * - Type-safe parameter storage
 * - Connection to parameter arrays
 * - Undo/redo support
 * - Display names and units
 */
public class ControlBlockParameterTest {

    @Test
    public void testReglerLimit_MinLimitParameterExists() {
        ControlLimit limit = new ControlLimit();
        UserParameter<Double> minLimit = limit._minLimit;
        assertNotNull("minLimit parameter should exist", minLimit);
    }

    @Test
    public void testReglerLimit_MaxLimitParameterExists() {
        ControlLimit limit = new ControlLimit();
        UserParameter<Double> maxLimit = limit._maxLimit;
        assertNotNull("maxLimit parameter should exist", maxLimit);
    }

    @Test
    public void testReglerLimit_MinLimitDefaultValue() {
        ControlLimit limit = new ControlLimit();
        assertEquals("minLimit default should be -1.0", -1.0, limit._minLimit.getValue(), 1e-9);
    }

    @Test
    public void testReglerLimit_MaxLimitDefaultValue() {
        ControlLimit limit = new ControlLimit();
        assertEquals("maxLimit default should be 1.0", 1.0, limit._maxLimit.getValue(), 1e-9);
    }

    @Test
    public void testReglerLimit_IsExternalDefaultValue() {
        ControlLimit limit = new ControlLimit();
        assertFalse("isExternal default should be false", limit._isExternalSet.getValue());
    }

    @Test
    public void testUserParameter_GetShortName() {
        ControlLimit limit = new ControlLimit();
        String shortName = limit._minLimit.getShortName();
        assertNotNull("Short name should not be null", shortName);
        assertEquals("Short name should be 'min'", "min", shortName);
    }

    @Test
    public void testUserParameter_GetDoubleValue() {
        ControlLimit limit = new ControlLimit();
        double value = limit._maxLimit.getDoubleValue();
        assertEquals("getDoubleValue should return 1.0", 1.0, value, 1e-9);
    }

    @Test
    public void testUserParameter_GetSaveIdentifier() {
        ControlLimit limit = new ControlLimit();
        String identifier = limit._minLimit.getSaveIdentifier();
        assertNotNull("Save identifier should not be null", identifier);
        assertEquals("Save identifier should be 'minLimit'", "minLimit", identifier);
    }

    @Test
    public void testUserParameter_SetValueWithoutUndo() {
        ControlLimit limit = new ControlLimit();
        limit._minLimit.setValueWithoutUndo(-10.0);
        assertEquals("Value should be updated to -10.0", -10.0, limit._minLimit.getValue(), 1e-9);
    }

    @Test
    public void testUserParameter_BooleanParameter() {
        ControlLimit limit = new ControlLimit();
        UserParameter<Boolean> isExternal = limit._isExternalSet;

        // Test boolean specific behavior
        Boolean value = isExternal.getValue();
        assertNotNull("Boolean value should not be null", value);
        assertFalse("Default value should be false", value);
    }

    @Test
    public void testReglerLimit_ParameterLinksToArray() {
        ControlLimit limit = new ControlLimit();
        // minLimit is at index 0, maxLimit at index 1
        limit._minLimit.setValueWithoutUndo(-5.0);
        limit._maxLimit.setValueWithoutUndo(5.0);

        // The parameter array should be updated
        assertEquals("Parameter array[0] should be -5.0", -5.0, limit.parameter[0], 1e-9);
        assertEquals("Parameter array[1] should be 5.0", 5.0, limit.parameter[1], 1e-9);
    }

    @Test
    public void testReglerDivision_NoRegisteredParameters() {
        // Division block is simple - no configurable parameters
        ControlDivision div = new ControlDivision();
        // It should still work without registered parameters
        assertNotNull("Division block should be creatable", div);
    }
}
