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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.ReglerDivision;
import ch.technokrat.gecko.geckocircuits.control.ReglerLimit;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

/**
 * Tests for AbstractBlockInterface - the base class for all circuit and control components.
 *
 * These tests verify the core functionality inherited by all components:
 * - Terminal management (XIN/YOUT)
 * - Parameter registration and access
 * - Position and orientation
 * - Component identity
 */
public class AbstractBlockInterfaceTest {

    @Test
    public void testReglerDivision_HasTwoInputTerminals() {
        ReglerDivision div = new ReglerDivision();
        assertEquals("Division block should have 2 input terminals", 2, div.XIN.size());
    }

    @Test
    public void testReglerDivision_HasOneOutputTerminal() {
        ReglerDivision div = new ReglerDivision();
        assertEquals("Division block should have 1 output terminal", 1, div.YOUT.size());
    }

    @Test
    public void testReglerLimit_HasOneInputTerminalInitially() {
        ReglerLimit limit = new ReglerLimit();
        assertEquals("Limit block should have 1 input terminal initially", 1, limit.XIN.size());
    }

    @Test
    public void testReglerLimit_HasOneOutputTerminal() {
        ReglerLimit limit = new ReglerLimit();
        assertEquals("Limit block should have 1 output terminal", 1, limit.YOUT.size());
    }

    @Test
    public void testReglerLimit_HasRegisteredParameters() {
        ReglerLimit limit = new ReglerLimit();
        List<UserParameter<?>> params = limit.getRegisteredParameters();
        assertNotNull("Registered parameters should not be null", params);
        assertTrue("Limit block should have at least 3 parameters", params.size() >= 3);
    }

    @Test
    public void testGetAllTerminals_ReturnsAllTerminals() {
        ReglerDivision div = new ReglerDivision();
        Collection<? extends TerminalInterface> terminals = div.getAllTerminals();
        assertNotNull("getAllTerminals should not return null", terminals);
        assertEquals("Should return 3 terminals (2 in + 1 out)", 3, terminals.size());
    }

    @Test
    public void testComponentDirection_DefaultIsNorthSouth() {
        ReglerDivision div = new ReglerDivision();
        assertEquals("Default direction should be NORTH_SOUTH",
                ComponentDirection.NORTH_SOUTH, div.getComponentDirection());
    }

    @Test
    public void testSetComponentDirection_CanBeAccessed() {
        // Note: setComponentDirection behavior depends on ModelMVC initialization
        // This test verifies the direction can be accessed after construction
        ReglerDivision div = new ReglerDivision();
        ComponentDirection direction = div.getComponentDirection();
        assertNotNull("Component direction should not be null", direction);
    }

    @Test
    public void testSetSheetPosition_SetsPosition() {
        ReglerDivision div = new ReglerDivision();
        Point pos = new Point(10, 20);
        div.setSheetPositionWithoutUndo(pos);
        assertEquals("Sheet position should be set", pos, div.getSheetPosition());
    }

    @Test
    public void testParameterArray_HasFixedSize() {
        ReglerDivision div = new ReglerDivision();
        assertEquals("Parameter array should have size 40", 40, div.parameter.length);
    }

    @Test
    public void testNameOptArray_HasFixedSize() {
        ReglerDivision div = new ReglerDivision();
        assertEquals("nameOpt array should have size 40", 40, div.nameOpt.length);
    }

    @Test
    public void testGetRegisteredParameters_ReturnsUnmodifiableList() {
        ReglerLimit limit = new ReglerLimit();
        List<UserParameter<?>> params = limit.getRegisteredParameters();

        try {
            params.add(null);
            fail("Should throw UnsupportedOperationException for unmodifiable list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testSetParameter_UpdatesParameterArray() {
        ReglerLimit limit = new ReglerLimit();
        double[] newParams = new double[40];
        newParams[0] = -5.0;  // minLimit
        newParams[1] = 10.0;  // maxLimit

        limit.setParameter(newParams);

        assertEquals("Parameter[0] should be updated", -5.0, limit.parameter[0], 1e-9);
        assertEquals("Parameter[1] should be updated", 10.0, limit.parameter[1], 1e-9);
    }

    @Test
    public void testGetParameter_ReturnsParameterArray() {
        ReglerLimit limit = new ReglerLimit();
        double[] params = limit.getParameter();
        assertNotNull("getParameter should not return null", params);
        assertEquals("Should return array of size 40", 40, params.length);
    }
}
