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

import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
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
        assertNotNull("ReglerConstant type info should exist", ReglerConstant.tinfo);
        assertNotNull("ReglerGain type info should exist", ReglerGain.tinfo);
        assertNotNull("ReglerAdd type info should exist", ReglerAdd.tinfo);
    }
    
    @Test
    public void testControlTypeInfoDomain() {
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ReglerConstant.tinfo.getSimulationDomain());
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ReglerGain.tinfo.getSimulationDomain());
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ReglerAdd.tinfo.getSimulationDomain());
    }
    
    @Test
    public void testControlTypeInfoExportCharacters() {
        assertEquals("Export char should be 'c'", "c", ReglerConstant.tinfo.getExportImportCharacters());
        assertEquals("Export char should be 'c'", "c", ReglerGain.tinfo.getExportImportCharacters());
    }
    
    @Test
    public void testControlTypeInfoSaveIdentifier() {
        assertEquals("Save ID should be ElementCONTROL", "ElementCONTROL", ReglerConstant.tinfo.getSaveIdentifier());
    }
    
    @Test
    public void testControlTypeInfoIdStrings() {
        assertEquals("CONST id", "CONST", ReglerConstant.tinfo._fixedIDString);
        assertEquals("GAIN id", "GAIN", ReglerGain.tinfo._fixedIDString);
        assertEquals("ADD id", "ADD", ReglerAdd.tinfo._fixedIDString);
        assertEquals("INT id", "INT", ReglerIntegrator.tinfo._fixedIDString);
    }
    
    // ========== Factory Tests ==========
    
    @Test
    public void testControlTypeInfoFabricConstant() {
        Object block = ReglerConstant.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ReglerConstant", block instanceof ReglerConstant);
    }
    
    @Test
    public void testControlTypeInfoFabricGain() {
        Object block = ReglerGain.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ReglerGain", block instanceof ReglerGain);
    }
    
    @Test
    public void testControlTypeInfoFabricAdd() {
        Object block = ReglerAdd.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ReglerAdd", block instanceof ReglerAdd);
    }
    
    @Test
    public void testControlTypeInfoFabricIntegrator() {
        Object block = ReglerIntegrator.tinfo.fabric();
        assertNotNull("Should create block", block);
        assertTrue("Should be ReglerIntegrator", block instanceof ReglerIntegrator);
    }
    
    // ========== Additional Control Block Type Tests ==========
    
    @Test
    public void testReglerSubtractionTypeInfo() {
        assertNotNull("ReglerSubtraction type info should exist", ReglerSubtraction.tinfo);
        assertEquals("SUB id", "SUB", ReglerSubtraction.tinfo._fixedIDString);
        assertEquals("Should be CONTROL domain", ConnectorType.CONTROL, ReglerSubtraction.tinfo.getSimulationDomain());
    }
    
    @Test
    public void testReglerMULTypeInfo() {
        assertNotNull("ReglerMUL type info should exist", ReglerMUL.tinfo);
        assertEquals("MUL id", "MUL", ReglerMUL.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerDivisionTypeInfo() {
        assertNotNull("ReglerDivision type info should exist", ReglerDivision.tinfo);
        assertEquals("DIV id", "DIV", ReglerDivision.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerLimitTypeInfo() {
        assertNotNull("ReglerLimit type info should exist", ReglerLimit.tinfo);
        assertEquals("LIMIT id", "LIMIT", ReglerLimit.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerDelayTypeInfo() {
        assertNotNull("ReglerDelay type info should exist", ReglerDelay.tinfo);
        assertEquals("DELAY id", "DELAY", ReglerDelay.tinfo._fixedIDString);
    }
    
    // ========== Math Function Type Tests ==========
    
    @Test
    public void testReglerSQRTTypeInfo() {
        assertNotNull("ReglerSQRT type info should exist", ReglerSQRT.tinfo);
        assertEquals("SQRT id", "SQRT", ReglerSQRT.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerSQRTypeInfo() {
        assertNotNull("ReglerSQR type info should exist", ReglerSQR.TYPE_INFO);
        assertEquals("SQR id", "SQR", ReglerSQR.TYPE_INFO._fixedIDString);
    }
    
    @Test
    public void testReglerPOWTypeInfo() {
        assertNotNull("ReglerPOW type info should exist", ReglerPOW.tinfo);
        assertEquals("POW id", "POW", ReglerPOW.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerExponentialTypeInfo() {
        assertNotNull("ReglerExponential type info should exist", ReglerExponential.tinfo);
        assertEquals("EXP id", "EXP", ReglerExponential.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerLNTypeInfo() {
        assertNotNull("ReglerLN type info should exist", ReglerLN.tinfo);
        assertEquals("LN id", "LN", ReglerLN.tinfo._fixedIDString);
    }
    
    // ========== Trigonometric Function Type Tests ==========
    
    @Test
    public void testReglerSINTypeInfo() {
        assertNotNull("ReglerSIN type info should exist", ReglerSIN.tinfo);
        assertEquals("SIN id", "SIN", ReglerSIN.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerCosineTypeInfo() {
        assertNotNull("ReglerCosine type info should exist", ReglerCosine.tinfo);
        assertEquals("COS id", "COS", ReglerCosine.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerTANTypeInfo() {
        assertNotNull("ReglerTAN type info should exist", ReglerTAN.tinfo);
        assertEquals("TAN id", "TAN", ReglerTAN.tinfo._fixedIDString);
    }
    
    // ========== Logic Function Type Tests ==========
    
    @Test
    public void testReglerAndTypeInfo() {
        assertNotNull("ReglerAnd type info should exist", ReglerAnd.tinfo);
        assertEquals("AND id", "AND", ReglerAnd.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerOrTypeInfo() {
        assertNotNull("ReglerOr type info should exist", ReglerOr.tinfo);
        assertEquals("OR id", "OR", ReglerOr.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerNOTTypeInfo() {
        assertNotNull("ReglerNOT type info should exist", ReglerNOT.tinfo);
        assertEquals("NOT id", "NOT", ReglerNOT.tinfo._fixedIDString);
    }
    
    // ========== Comparison Function Type Tests ==========
    
    @Test
    public void testReglerGreaterThanTypeInfo() {
        assertNotNull("ReglerGreaterThan type info should exist", ReglerGreaterThan.tinfo);
        assertEquals("GT id", "GT", ReglerGreaterThan.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerGreaterEqualTypeInfo() {
        assertNotNull("ReglerGreaterEqual type info should exist", ReglerGreaterEqual.tinfo);
        assertEquals("GE id", "GE", ReglerGreaterEqual.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerEqualTypeInfo() {
        assertNotNull("ReglerEqual type info should exist", ReglerEqual.tinfo);
        assertEquals("EQ id", "EQ", ReglerEqual.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerNETypeInfo() {
        assertNotNull("ReglerNE type info should exist", ReglerNE.tinfo);
        assertEquals("NE id", "NE", ReglerNE.tinfo._fixedIDString);
    }
    
    // ========== Signal Source Type Tests ==========
    
    @Test
    public void testReglerSignalSourceTypeInfo() {
        assertNotNull("ReglerSignalSource type info should exist", ReglerSignalSource.tinfo);
        assertEquals("SIGNAL id", "SIGNAL", ReglerSignalSource.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerTIMETypeInfo() {
        assertNotNull("ReglerTIME type info should exist", ReglerTIME.tinfo);
        assertEquals("TIME id", "TIME", ReglerTIME.tinfo._fixedIDString);
    }
    
    // ========== Control System Block Tests ==========
    
    @Test
    public void testReglerPITypeInfo() {
        assertNotNull("ReglerPI type info should exist", ReglerPI.tinfo);
        assertEquals("PI id", "PI", ReglerPI.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerPDTypeInfo() {
        assertNotNull("ReglerPD type info should exist", ReglerPD.tinfo);
        assertEquals("PD id", "PD", ReglerPD.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerPT1TypeInfo() {
        assertNotNull("ReglerPT1 type info should exist", ReglerPT1.tinfo);
        assertEquals("PT1 id", "PT1", ReglerPT1.tinfo._fixedIDString);
    }
    
    @Test
    public void testReglerPT2TypeInfo() {
        assertNotNull("ReglerPT2 type info should exist", ReglerPT2.tinfo);
        assertEquals("PT2 id", "PT2", ReglerPT2.tinfo._fixedIDString);
    }
}
