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
package ch.technokrat.gecko.geckocircuits.newscope;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for AxisLimits - axis range and zoom management.
 */
public class AxisLimitsTest {

    private AxisLimits limits;
    private static final float DELTA = 1e-6f;

    @Before
    public void setUp() {
        limits = new AxisLimits();
    }

    // ====================================================
    // Default State Tests
    // ====================================================

    @Test
    public void testDefaultAutoEnabled_IsTrue() {
        assertTrue("Default auto scaling should be enabled", limits.isAutoEnabled());
    }

    @Test
    public void testDefaultClipping_IsGlobalAuto() {
        assertEquals("Default clipping should be GLOBAL_AUTO",
            Clipping.GLOBAL_AUTO, limits.getClipping());
    }

    @Test
    public void testDefaultLimits_NotNull() {
        assertNotNull("Default limits should not be null", limits.getLimits());
    }

    // ====================================================
    // Auto Scale Enable/Disable Tests
    // ====================================================

    @Test
    public void testSetAutoEnabled_False() {
        limits.setAutoEnabled(false);
        assertFalse("Auto enabled should be false", limits.isAutoEnabled());
    }

    @Test
    public void testSetAutoEnabled_True() {
        limits.setAutoEnabled(false);
        limits.setAutoEnabled(true);
        assertTrue("Auto enabled should be true", limits.isAutoEnabled());
    }

    @Test
    public void testSetAutoEnabled_Toggle() {
        assertTrue("Initial auto should be enabled", limits.isAutoEnabled());
        limits.setAutoEnabled(false);
        assertFalse("After toggle to false", limits.isAutoEnabled());
        limits.setAutoEnabled(true);
        assertTrue("After toggle back to true", limits.isAutoEnabled());
    }

    @Test
    public void testDisableAutoScale_SavesCurrentLimits() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(-10, 50);
        limits.setGlobalAutoScaleValues(globalScale);

        limits.setAutoEnabled(false);

        HiLoData retrievedLimits = limits.getLimits();
        assertNotNull("Retrieved limits should not be null", retrievedLimits);
        assertEquals("Retrieved min should match", -10, retrievedLimits._yLo, DELTA);
        assertEquals("Retrieved max should match", 50, retrievedLimits._yHi, DELTA);
    }

    // ====================================================
    // Global Auto Scale Tests
    // ====================================================

    @Test
    public void testSetGlobalAutoScaleValues() {
        HiLoData newGlobalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(newGlobalScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        assertEquals("Global scale min should match", 0, retrieved._yLo, DELTA);
        assertEquals("Global scale max should match", 100, retrieved._yHi, DELTA);
    }

    @Test
    public void testSetGlobalAutoScaleValues_Negative() {
        HiLoData newGlobalScale = HiLoData.hiLoDataFabric(-50, -10);
        limits.setGlobalAutoScaleValues(newGlobalScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        assertEquals("Global scale min should match", -50, retrieved._yLo, DELTA);
        assertEquals("Global scale max should match", -10, retrieved._yHi, DELTA);
    }

    @Test
    public void testSetGlobalAutoScaleValues_CrossingZero() {
        HiLoData newGlobalScale = HiLoData.hiLoDataFabric(-30, 70);
        limits.setGlobalAutoScaleValues(newGlobalScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        assertEquals("Global scale min", -30, retrieved._yLo, DELTA);
        assertEquals("Global scale max", 70, retrieved._yHi, DELTA);
    }

    // ====================================================
    // Local Auto Scale Tests
    // ====================================================

    @Test
    public void testSetLocalAutoScaleValues() {
        HiLoData localScale = HiLoData.hiLoDataFabric(10, 40);
        limits.setLocalAutoScaleValues(localScale);

        // Switch to local auto scale
        limits.setLocalFit();
        HiLoData retrieved = limits.getLimits();

        assertEquals("Local scale min should match", 10, retrieved._yLo, DELTA);
        assertEquals("Local scale max should match", 40, retrieved._yHi, DELTA);
    }

    @Test
    public void testSetLocalFit() {
        HiLoData localScale = HiLoData.hiLoDataFabric(5, 95);
        limits.setLocalAutoScaleValues(localScale);

        limits.setLocalFit();
        assertEquals("Clipping should be LOCAL_AUTO", Clipping.LOCAL_AUTO, limits.getClipping());
    }

    // ====================================================
    // Global Fit Tests
    // ====================================================

    @Test
    public void testGlobalFit() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        limits.globalFit();
        assertEquals("Clipping should be GLOBAL_AUTO", Clipping.GLOBAL_AUTO, limits.getClipping());
    }

    @Test
    public void testGlobalFit_FromZoomed() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData zoomedData = HiLoData.hiLoDataFabric(20, 80);
        limits.setZoomValues(zoomedData, false);

        assertEquals("Should be in ZOOMED mode", Clipping.ZOOMED, limits.getClipping());

        limits.globalFit();
        assertEquals("Clipping should be back to GLOBAL_AUTO", Clipping.GLOBAL_AUTO, limits.getClipping());
    }

    // ====================================================
    // Zoom Tests
    // ====================================================

    @Test
    public void testSetZoomValues() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData zoomData = HiLoData.hiLoDataFabric(25, 75);
        limits.setZoomValues(zoomData, false);

        assertEquals("Clipping should be ZOOMED", Clipping.ZOOMED, limits.getClipping());

        HiLoData retrieved = limits.getLimits();
        assertEquals("Zoomed min should match", 25, retrieved._yLo, DELTA);
        assertEquals("Zoomed max should match", 75, retrieved._yHi, DELTA);
    }

    @Test
    public void testSetZoomValues_WithNiceScale() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData zoomData = HiLoData.hiLoDataFabric(25, 75);
        limits.setZoomValues(zoomData, true);

        assertEquals("Clipping should be ZOOMED", Clipping.ZOOMED, limits.getClipping());
        HiLoData retrieved = limits.getLimits();
        assertNotNull("Retrieved limits should not be null", retrieved);
    }

    @Test
    public void testSetZoomValues_MultipleZooms() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        // First zoom
        HiLoData zoom1 = HiLoData.hiLoDataFabric(20, 80);
        limits.setZoomValues(zoom1, false);
        HiLoData retrieved1 = limits.getLimits();
        assertEquals("First zoom max", 80, retrieved1._yHi, DELTA);

        // Second zoom
        HiLoData zoom2 = HiLoData.hiLoDataFabric(30, 70);
        limits.setZoomValues(zoom2, false);
        HiLoData retrieved2 = limits.getLimits();
        assertEquals("Second zoom max", 70, retrieved2._yHi, DELTA);
    }

    // ====================================================
    // History/Undo Tests
    // ====================================================

    @Test
    public void testPopHistoryStack_SingleZoom() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData zoomData = HiLoData.hiLoDataFabric(25, 75);
        limits.setZoomValues(zoomData, false);

        // Pop history should go back to previous state
        limits.popHistoryStack();

        // After pop, should go back (note: this also zooms)
        HiLoData retrieved = limits.getLimits();
        assertNotNull("Retrieved limits should not be null", retrieved);
    }

    @Test
    public void testPopHistoryStack_EmptyHistory() {
        // Pop on empty history should do nothing
        limits.popHistoryStack();
        assertEquals("Clipping should still be GLOBAL_AUTO", Clipping.GLOBAL_AUTO, limits.getClipping());
    }

    // ====================================================
    // Save/Load Tests
    // ====================================================

    @Test
    public void testSaveValues() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        // Should not throw
        limits.saveValues();
    }

    @Test
    public void testLoadFromSaved() {
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        limits.saveValues();

        // Zoom to something else
        HiLoData zoomData = HiLoData.hiLoDataFabric(25, 75);
        limits.setZoomValues(zoomData, false);

        // Load saved should restore
        limits.loadFromSaved();

        HiLoData retrieved = limits.getLimits();
        assertNotNull("Retrieved limits should not be null", retrieved);
    }

    // ====================================================
    // Nice Scale Tests
    // ====================================================

    @Test
    public void testSetNiceScale_True() {
        limits.setNiceScale(true);

        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData retrieved = limits.getLimits();
        assertNotNull("Retrieved limits should not be null", retrieved);
    }

    @Test
    public void testSetNiceScale_False() {
        limits.setNiceScale(false);

        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData retrieved = limits.getLimits();
        assertEquals("Retrieved min", 0, retrieved._yLo, DELTA);
        assertEquals("Retrieved max", 100, retrieved._yHi, DELTA);
    }

    // ====================================================
    // Symmetric Zero Tests
    // ====================================================

    @Test
    public void testSetCommonZero_True() {
        limits.setCommonZero(true);

        HiLoData globalScale = HiLoData.hiLoDataFabric(-30, 70);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        // With common zero, both should be symmetric
        assertEquals("Min should be symmetric", Math.abs(retrieved._yLo), Math.abs(retrieved._yHi), DELTA);
    }

    @Test
    public void testSetCommonZero_False() {
        limits.setCommonZero(false);

        HiLoData globalScale = HiLoData.hiLoDataFabric(-30, 70);
        limits.setGlobalAutoScaleValues(globalScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        // Should not be symmetric
        assertNotEquals("Min and max should not be symmetric", Math.abs(retrieved._yLo), Math.abs(retrieved._yHi), DELTA);
    }

    @Test
    public void testIsCommonZero_Default() {
        assertFalse("Default common zero should be false", limits.isCommonZero());
    }

    // ====================================================
    // User Scale Tests
    // ====================================================

    @Test
    public void testSetValueScaleLocal() {
        HiLoData userScale = HiLoData.hiLoDataFabric(10, 90);
        limits.setValueScaleLocal(userScale);

        limits.setAutoEnabled(false);
        HiLoData retrieved = limits.getLimits();

        assertEquals("Value scale min", 10, retrieved._yLo, DELTA);
        assertEquals("Value scale max", 90, retrieved._yHi, DELTA);
    }

    @Test
    public void testSetUserScale() {
        HiLoData userScale = HiLoData.hiLoDataFabric(15, 85);
        limits.setUserScale(userScale);

        limits.setAutoEnabled(false);
        HiLoData retrieved = limits.getLimits();

        assertEquals("User scale min", 15, retrieved._yLo, DELTA);
        assertEquals("User scale max", 85, retrieved._yHi, DELTA);
    }

    // ====================================================
    // Integration Tests
    // ====================================================

    @Test
    public void testWorkflow_AutoToManualZoom() {
        // Start with auto scale
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 100);
        limits.setGlobalAutoScaleValues(globalScale);
        assertTrue("Should start with auto enabled", limits.isAutoEnabled());

        // Zoom
        HiLoData zoomData = HiLoData.hiLoDataFabric(30, 70);
        limits.setZoomValues(zoomData, false);

        // Switch to manual
        HiLoData manualScale = HiLoData.hiLoDataFabric(40, 60);
        limits.setAutoEnabled(false);
        limits.setValueScaleLocal(manualScale);

        HiLoData retrieved = limits.getLimits();
        assertEquals("Manual scale min", 40, retrieved._yLo, DELTA);
        assertEquals("Manual scale max", 60, retrieved._yHi, DELTA);
    }

    @Test
    public void testWorkflow_ComplexSequence() {
        // Global fit
        HiLoData globalScale = HiLoData.hiLoDataFabric(0, 1000);
        limits.setGlobalAutoScaleValues(globalScale);

        // Zoom in
        limits.setZoomValues(HiLoData.hiLoDataFabric(200, 800), false);

        // Save current state
        limits.saveValues();

        // Zoom in more
        limits.setZoomValues(HiLoData.hiLoDataFabric(300, 700), false);

        // Load saved (should restore to previous zoom)
        limits.loadFromSaved();

        HiLoData retrieved = limits.getLimits();
        assertNotNull("Should have valid limits after load", retrieved);
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testSetGlobalAutoScaleValues_VeryLargeRange() {
        HiLoData largeScale = HiLoData.hiLoDataFabric(-1e10f, 1e10f);
        limits.setGlobalAutoScaleValues(largeScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        assertEquals("Large min", -1e10f, retrieved._yLo, DELTA);
        assertEquals("Large max", 1e10f, retrieved._yHi, DELTA);
    }

    @Test
    public void testSetGlobalAutoScaleValues_VerySmallRange() {
        HiLoData smallScale = HiLoData.hiLoDataFabric(-1e-10f, 1e-10f);
        limits.setGlobalAutoScaleValues(smallScale);

        HiLoData retrieved = limits.getAutoScaleGlobal();
        assertEquals("Small min", -1e-10f, retrieved._yLo, 1e-15f);
        assertEquals("Small max", 1e-10f, retrieved._yHi, 1e-15f);
    }
}
