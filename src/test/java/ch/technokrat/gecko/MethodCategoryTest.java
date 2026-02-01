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
package ch.technokrat.gecko;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for MethodCategory enum.
 * Tests enum values and string representation.
 */
public class MethodCategoryTest {

    @Test
    public void testMethodCategoryValues() {
        MethodCategory[] values = MethodCategory.values();
        assertEquals(6, values.length);
    }

    @Test
    public void testSimulationStartCategory() {
        MethodCategory cat = MethodCategory.SIMULATION_START;
        assertNotNull(cat);
        assertEquals(MethodCategory.SIMULATION_START, cat);
    }

    @Test
    public void testLoadSaveModelCategory() {
        MethodCategory cat = MethodCategory.LOAD_SAVE_MODEL;
        assertNotNull(cat);
        assertEquals(MethodCategory.LOAD_SAVE_MODEL, cat);
    }

    @Test
    public void testSignalProcessingCategory() {
        MethodCategory cat = MethodCategory.SIGNAL_PROCESSING;
        assertNotNull(cat);
        assertEquals(MethodCategory.SIGNAL_PROCESSING, cat);
    }

    @Test
    public void testComponentPropertiesCategory() {
        MethodCategory cat = MethodCategory.COMPONENT_PROPERTIES;
        assertNotNull(cat);
        assertEquals(MethodCategory.COMPONENT_PROPERTIES, cat);
    }

    @Test
    public void testComponentCreationListingCategory() {
        MethodCategory cat = MethodCategory.COMPONENT_CREATION_LISTING;
        assertNotNull(cat);
        assertEquals(MethodCategory.COMPONENT_CREATION_LISTING, cat);
    }

    @Test
    public void testAllCategoriesCategory() {
        MethodCategory cat = MethodCategory.ALL_CATEGORIES;
        assertNotNull(cat);
        assertEquals(MethodCategory.ALL_CATEGORIES, cat);
    }

    @Test
    public void testMethodCategoryToString() {
        MethodCategory cat = MethodCategory.SIMULATION_START;
        String str = cat.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void testAllCategoriesHaveTranslation() {
        for (MethodCategory cat : MethodCategory.values()) {
            String translation = cat.toString();
            assertNotNull("Category " + cat + " should have a translation", translation);
            assertFalse("Category " + cat + " translation should not be empty", translation.isEmpty());
        }
    }

    @Test
    public void testEnumValueOf() {
        MethodCategory cat = MethodCategory.valueOf("SIMULATION_START");
        assertEquals(MethodCategory.SIMULATION_START, cat);
    }

    @Test
    public void testEnumOrdinal() {
        MethodCategory cat = MethodCategory.SIMULATION_START;
        assertTrue(cat.ordinal() >= 0);
        assertTrue(cat.ordinal() < MethodCategory.values().length);
    }

    @Test
    public void testCategoryComparison() {
        MethodCategory cat1 = MethodCategory.SIMULATION_START;
        MethodCategory cat2 = MethodCategory.LOAD_SAVE_MODEL;
        assertNotEquals(cat1, cat2);
    }

    @Test
    public void testCategorySelfEquality() {
        MethodCategory cat = MethodCategory.COMPONENT_PROPERTIES;
        assertEquals(cat, cat);
    }

    @Test
    public void testCategoryHashCode() {
        MethodCategory cat1 = MethodCategory.SIMULATION_START;
        MethodCategory cat2 = MethodCategory.SIMULATION_START;
        assertEquals(cat1.hashCode(), cat2.hashCode());
    }
}
