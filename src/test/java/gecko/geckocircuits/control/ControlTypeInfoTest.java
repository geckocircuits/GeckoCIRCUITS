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
package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.ConnectorType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for ControlTypeInfo class.
 * Sprint 9: Control Package Core
 */
public class ControlTypeInfoTest {

    // ========== Basic Type Info Tests ==========

    @Test
    public void testControlTypeInfoCreation() {
        assertNotNull("ControlConstant type info should exist", ControlConstant.tinfo);
        assertNotNull("ControlGain type info should exist", ControlGain.tinfo);
        assertNotNull("ControlAdd type info should exist", ControlAdd.tinfo);
    }

    @Test
    public void testControlTypeInfoDomain() {
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ControlConstant.tinfo.getSimulationDomain());
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ControlGain.tinfo.getSimulationDomain());
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ControlAdd.tinfo.getSimulationDomain());
    }

    @Test
    public void testControlTypeInfoExportCharacters() {
        assertEquals("Export char should be 'c'", "c", ControlConstant.tinfo.getExportImportCharacters());
        assertEquals("Export char should be 'c'", "c", ControlGain.tinfo.getExportImportCharacters());
    }

    @Test
    public void testControlTypeInfoSaveIdentifier() {
        assertEquals("Save ID should be ElementCONTROL", "ElementCONTROL", ControlConstant.tinfo.getSaveIdentifier());
    }

    @Test
    public void testControlTypeInfoIdStrings() {
        assertEquals("CONST id", "CONST", ControlConstant.tinfo._fixedIDString);
        assertEquals("GAIN id", "GAIN", ControlGain.tinfo._fixedIDString);
        assertEquals("ADD id", "ADD", ControlAdd.tinfo._fixedIDString);
        assertEquals("INT id", "INT", ControlIntegrator.tinfo._fixedIDString);
    }

    // ========== Factory Tests ==========

    @Test
    public void testControlTypeInfoFabricConstant() {
        Object block = ControlConstant.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ControlConstant", block instanceof ControlConstant);
    }

    @Test
    public void testControlTypeInfoFabricGain() {
        Object block = ControlGain.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ControlGain", block instanceof ControlGain);
    }

    @Test
    public void testControlTypeInfoFabricAdd() {
        Object block = ControlAdd.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ControlAdd", block instanceof ControlAdd);
    }

    @Test
    public void testControlTypeInfoFabricIntegrator() {
        Object block = ControlIntegrator.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ControlIntegrator", block instanceof ControlIntegrator);
    }

    // ========== Additional Control Block Type Tests ==========

    @Test
    public void testReglerSubtractionTypeInfo() {
        assertNotNull("ControlSubtraction type info should exist", ControlSubtraction.tinfo);
        assertEquals("SUB id", "SUB", ControlSubtraction.tinfo._fixedIDString);
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ControlSubtraction.tinfo.getSimulationDomain());
    }

    @Test
    public void testReglerMULTypeInfo() {
        assertNotNull("ControlMUL type info should exist", ControlMUL.tinfo);
        assertEquals("MUL id", "MUL", ControlMUL.tinfo._fixedIDString);
    }

    @Test
    public void testReglerDivisionTypeInfo() {
        assertNotNull("ControlDivision type info should exist", ControlDivision.tinfo);
        assertEquals("DIV id", "DIV", ControlDivision.tinfo._fixedIDString);
    }

    @Test
    public void testReglerLimitTypeInfo() {
        assertNotNull("ControlLimit type info should exist", ControlLimit.tinfo);
        assertEquals("LIMIT id", "LIMIT", ControlLimit.tinfo._fixedIDString);
    }

    @Test
    public void testReglerDelayTypeInfo() {
        assertNotNull("ControlDelay type info should exist", ControlDelay.tinfo);
        assertEquals("DELAY id", "DELAY", ControlDelay.tinfo._fixedIDString);
    }

    // ========== Math Function Type Tests ==========

    @Test
    public void testReglerSQRTTypeInfo() {
        assertNotNull("ControlSQRT type info should exist", ControlSQRT.tinfo);
        assertEquals("SQRT id", "SQRT", ControlSQRT.tinfo._fixedIDString);
    }

    @Test
    public void testReglerSQRTypeInfo() {
        assertNotNull("ControlSQR type info should exist", ControlSQR.TYPE_INFO);
        assertEquals("SQR id", "SQR", ControlSQR.TYPE_INFO._fixedIDString);
    }

    @Test
    public void testReglerPOWTypeInfo() {
        assertNotNull("ControlPOW type info should exist", ControlPOW.tinfo);
        assertEquals("POW id", "POW", ControlPOW.tinfo._fixedIDString);
    }

    @Test
    public void testReglerExponentialTypeInfo() {
        assertNotNull("ControlExponential type info should exist", ControlExponential.tinfo);
        assertEquals("EXP id", "EXP", ControlExponential.tinfo._fixedIDString);
    }

    @Test
    public void testReglerLNTypeInfo() {
        assertNotNull("ControlLN type info should exist", ControlLN.tinfo);
        assertEquals("LN id", "LN", ControlLN.tinfo._fixedIDString);
    }

    // ========== Trigonometric Function Type Tests ==========

    @Test
    public void testReglerSINTypeInfo() {
        assertNotNull("ControlSIN type info should exist", ControlSIN.tinfo);
        assertEquals("SIN id", "SIN", ControlSIN.tinfo._fixedIDString);
    }

    @Test
    public void testReglerCosineTypeInfo() {
        assertNotNull("ControlCosine type info should exist", ControlCosine.tinfo);
        assertEquals("COS id", "COS", ControlCosine.tinfo._fixedIDString);
    }

    @Test
    public void testReglerTANTypeInfo() {
        assertNotNull("ControlTAN type info should exist", ControlTAN.tinfo);
        assertEquals("TAN id", "TAN", ControlTAN.tinfo._fixedIDString);
    }

    // ========== Logic Function Type Tests ==========

    @Test
    public void testReglerAndTypeInfo() {
        assertNotNull("ControlAnd type info should exist", ControlAnd.tinfo);
        assertEquals("AND id", "AND", ControlAnd.tinfo._fixedIDString);
    }

    @Test
    public void testReglerOrTypeInfo() {
        assertNotNull("ControlOr type info should exist", ControlOr.tinfo);
        assertEquals("OR id", "OR", ControlOr.tinfo._fixedIDString);
    }

    @Test
    public void testReglerNOTTypeInfo() {
        assertNotNull("ControlNOT type info should exist", ControlNOT.tinfo);
        assertEquals("NOT id", "NOT", ControlNOT.tinfo._fixedIDString);
    }

    // ========== Comparison Function Type Tests ==========

    @Test
    public void testReglerGreaterThanTypeInfo() {
        assertNotNull("ControlGreaterThan type info should exist", ControlGreaterThan.tinfo);
        assertEquals("GT id", "GT", ControlGreaterThan.tinfo._fixedIDString);
    }

    @Test
    public void testReglerGreaterEqualTypeInfo() {
        assertNotNull("ControlGreaterEqual type info should exist", ControlGreaterEqual.tinfo);
        assertEquals("GE id", "GE", ControlGreaterEqual.tinfo._fixedIDString);
    }

    @Test
    public void testReglerEqualTypeInfo() {
        assertNotNull("ControlEqual type info should exist", ControlEqual.tinfo);
        assertEquals("EQ id", "EQ", ControlEqual.tinfo._fixedIDString);
    }

    @Test
    public void testReglerNETypeInfo() {
        assertNotNull("ControlNE type info should exist", ControlNE.tinfo);
        assertEquals("NE id", "NE", ControlNE.tinfo._fixedIDString);
    }

    // ========== Signal Source Type Tests ==========

    @Test
    public void testReglerSignalSourceTypeInfo() {
        assertNotNull("ControlSignalSource type info should exist", ControlSignalSource.tinfo);
        assertEquals("SIGNAL id", "SIGNAL", ControlSignalSource.tinfo._fixedIDString);
    }

    @Test
    public void testReglerTIMETypeInfo() {
        assertNotNull("ControlTIME type info should exist", ControlTIME.tinfo);
        assertEquals("TIME id", "TIME", ControlTIME.tinfo._fixedIDString);
    }

    // ========== Control System Block Tests ==========

    @Test
    public void testReglerPITypeInfo() {
        assertNotNull("ControlPI type info should exist", ControlPI.tinfo);
        assertEquals("PI id", "PI", ControlPI.tinfo._fixedIDString);
    }

    @Test
    public void testReglerPDTypeInfo() {
        assertNotNull("ControlPD type info should exist", ControlPD.tinfo);
        assertEquals("PD id", "PD", ControlPD.tinfo._fixedIDString);
    }

    @Test
    public void testReglerPT1TypeInfo() {
        assertNotNull("ControlPT1 type info should exist", ControlPT1.tinfo);
        assertEquals("PT1 id", "PT1", ControlPT1.tinfo._fixedIDString);
    }

    @Test
    public void testReglerPT2TypeInfo() {
        assertNotNull("ControlPT2 type info should exist", ControlPT2.tinfo);
        assertEquals("PT2 id", "PT2", ControlPT2.tinfo._fixedIDString);
    }
}
