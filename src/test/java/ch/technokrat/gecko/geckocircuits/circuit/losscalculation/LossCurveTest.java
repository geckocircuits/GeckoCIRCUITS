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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the LossCurve base class.
 */
@DisplayName("LossCurve Tests")
class LossCurveTest {
    
    // Concrete test implementation
    private static class TestLossCurve extends LossCurve {
        @Override
        String getXMLTag() {
            return "testCurve";
        }
    }
    
    private TestLossCurve curve;
    
    @BeforeEach
    void setUp() {
        curve = new TestLossCurve();
    }
    
    @Nested
    @DisplayName("Curve Data Management")
    class CurveDataTest {
        
        @Test
        @DisplayName("setCurveData creates defensive copy")
        void setCurveDataDefensiveCopy() {
            double[][] original = {{1.0, 2.0}, {3.0, 4.0}};
            curve.setCurveData(original);
            
            // Modify original
            original[0][0] = 999.0;
            
            // Curve data should not change
            double[][] curveData = curve.getCurveData();
            assertEquals(1.0, curveData[0][0], 1e-10);
        }
        
        @Test
        @DisplayName("getCurveData returns defensive copy")
        void getCurveDataDefensiveCopy() {
            double[][] original = {{1.0, 2.0}, {3.0, 4.0}};
            curve.setCurveData(original);
            
            // Modify returned data
            double[][] returned = curve.getCurveData();
            returned[0][0] = 999.0;
            
            // Internal data should not change
            double[][] curveData = curve.getCurveData();
            assertEquals(1.0, curveData[0][0], 1e-10);
        }
        
        @Test
        @DisplayName("handles empty curve data")
        void handleEmptyCurveData() {
            double[][] empty = {};
            curve.setCurveData(empty);
            
            double[][] result = curve.getCurveData();
            assertEquals(0, result.length);
        }
        
        @Test
        @DisplayName("handles jagged arrays")
        void handleJaggedArrays() {
            double[][] jagged = {{1.0, 2.0, 3.0}, {4.0, 5.0}};
            curve.setCurveData(jagged);
            
            double[][] result = curve.getCurveData();
            assertEquals(3, result[0].length);
            assertEquals(2, result[1].length);
        }
    }
    
    @Nested
    @DisplayName("Temperature Parameter")
    class TemperatureTest {
        
        @Test
        @DisplayName("getName returns temperature with degree symbol")
        void getNameWithTemperature() {
            curve.tj.setValueWithoutUndo(25.0);
            assertEquals("25°C", curve.getName());
        }
        
        @Test
        @DisplayName("getName truncates decimal part")
        void getNameTruncatesDecimal() {
            curve.tj.setValueWithoutUndo(25.7);
            assertEquals("25°C", curve.getName());
        }
        
        @Test
        @DisplayName("getName handles negative temperatures")
        void getNameNegativeTemp() {
            curve.tj.setValueWithoutUndo(-40.0);
            assertEquals("-40°C", curve.getName());
        }
    }
    
    @Nested
    @DisplayName("XML Serialization")
    class XmlSerializationTest {
        
        @Test
        @DisplayName("getXMLTag returns expected tag")
        void getXmlTag() {
            assertEquals("testCurve", curve.getXMLTag());
        }
        
        @Test
        @DisplayName("exportASCII produces valid output")
        void exportAsciiProducesOutput() {
            curve.setCurveData(new double[][]{{1.0, 2.0}, {3.0, 4.0}});
            curve.tj.setValueWithoutUndo(100.0);
            
            StringBuffer ascii = new StringBuffer();
            curve.exportASCII(ascii);
            
            String output = ascii.toString();
            assertTrue(output.contains("<testCurve>"), "Should contain opening tag");
            assertTrue(output.contains("<\\testCurve>"), "Should contain closing tag");
            assertTrue(output.contains("data"), "Should contain data section");
        }
    }
}
