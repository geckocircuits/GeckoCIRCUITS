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
 * Tests for the conduction loss curve class (LeitverlusteMesskurve).
 * 
 * The conduction loss curve stores V-I characteristic data at a specific
 * junction temperature, used for calculating semiconductor conduction losses.
 */
@DisplayName("LeitverlusteMesskurve (Conduction Loss Curve) Tests")
class LeitverlusteMesskurveTest {
    
    private LeitverlusteMesskurve curve;
    
    @BeforeEach
    void setUp() {
        curve = new LeitverlusteMesskurve(25.0);  // 25°C junction temp
    }
    
    @Nested
    @DisplayName("Construction")
    class ConstructionTest {
        
        @Test
        @DisplayName("stores junction temperature on construction")
        void storesTemperature() {
            LeitverlusteMesskurve testCurve = new LeitverlusteMesskurve(150.0);
            assertEquals(150.0, testCurve.tj.getValue(), 1e-10);
        }
        
        @Test
        @DisplayName("accepts negative temperature (cryogenic)")
        void acceptsNegativeTemperature() {
            LeitverlusteMesskurve cryoCurve = new LeitverlusteMesskurve(-40.0);
            assertEquals(-40.0, cryoCurve.tj.getValue(), 1e-10);
        }
    }
    
    @Nested
    @DisplayName("Copy Operation")
    class CopyTest {
        
        @Test
        @DisplayName("copy creates independent instance")
        void copyCreatesIndependent() {
            curve.setCurveData(new double[][]{{0.0, 0.0}, {1.0, 10.0}, {2.0, 50.0}});
            
            LeitverlusteMesskurve copy = curve.copy();
            
            // Modify original
            curve.tj.setValueWithoutUndo(999.0);
            curve.data[0][0] = 999.0;
            
            // Copy should be unchanged
            assertEquals(25.0, copy.tj.getValue(), 1e-10);
            assertEquals(0.0, copy.data[0][0], 1e-10);
        }
        
        @Test
        @DisplayName("copy preserves data dimensions")
        void copyPreservesDimensions() {
            double[][] data = {{0.0, 0.0}, {1.0, 10.0}, {2.0, 50.0}};
            curve.setCurveData(data);
            
            LeitverlusteMesskurve copy = curve.copy();
            
            assertEquals(data.length, copy.data.length);
            assertEquals(data[0].length, copy.data[0].length);
        }
        
        @Test
        @DisplayName("copy preserves all data values")
        void copyPreservesValues() {
            double[][] data = {
                {0.0, 0.0},    // V=0V, I=0A
                {0.7, 1.0},    // V=0.7V, I=1A (typical diode forward)
                {0.8, 5.0},    // V=0.8V, I=5A
                {0.9, 10.0}    // V=0.9V, I=10A
            };
            curve.setCurveData(data);
            
            LeitverlusteMesskurve copy = curve.copy();
            
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    assertEquals(data[i][j], copy.data[i][j], 1e-10,
                        String.format("Mismatch at [%d][%d]", i, j));
                }
            }
        }
    }
    
    @Nested
    @DisplayName("XML Serialization")
    class XmlTest {
        
        @Test
        @DisplayName("getXMLTag returns correct tag")
        void xmlTagCorrect() {
            assertEquals("LeitverlusteMesskurve", curve.getXMLTag());
        }
        
        @Test
        @DisplayName("exports to ASCII format")
        void exportsToAscii() {
            curve.setCurveData(new double[][]{{0.7, 1.0}, {0.8, 5.0}});
            
            StringBuffer ascii = new StringBuffer();
            curve.exportASCII(ascii);
            
            String output = ascii.toString();
            assertTrue(output.contains("<LeitverlusteMesskurve>"));
            assertTrue(output.contains("<\\LeitverlusteMesskurve>"));
        }
    }
    
    @Nested
    @DisplayName("V-I Characteristic Data")
    class CharacteristicDataTest {
        
        @Test
        @DisplayName("stores typical diode V-I curve")
        void storesDiodeCurve() {
            // Typical silicon diode forward characteristic at 25°C
            double[][] diodeCurve = {
                {0.0, 0.0},
                {0.5, 0.01},
                {0.6, 0.1},
                {0.7, 1.0},
                {0.8, 10.0},
                {0.9, 50.0}
            };
            
            curve.setCurveData(diodeCurve);
            double[][] retrieved = curve.getCurveData();
            
            assertEquals(6, retrieved.length);
            assertEquals(0.7, retrieved[3][0], 1e-10);  // Voltage at 1A
            assertEquals(1.0, retrieved[3][1], 1e-10);  // Current at 0.7V
        }
        
        @Test
        @DisplayName("getName returns temperature label")
        void nameIncludesTemperature() {
            assertEquals("25°C", curve.getName());
        }
    }
}
