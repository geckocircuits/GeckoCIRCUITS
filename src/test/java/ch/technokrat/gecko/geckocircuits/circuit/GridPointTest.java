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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the GUI-free GridPoint class.
 */
@DisplayName("GridPoint Tests")
class GridPointTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTest {
        
        @Test
        @DisplayName("Creates point from int coordinates")
        void constructFromInts() {
            GridPoint p = new GridPoint(10, 20);
            assertEquals(10, p.x);
            assertEquals(20, p.y);
            assertEquals(10, p.getX());
            assertEquals(20, p.getY());
        }
        
        @Test
        @DisplayName("Creates point from double coordinates (truncates)")
        void constructFromDoubles() {
            GridPoint p = new GridPoint(10.7, 20.9);
            assertEquals(10, p.x);
            assertEquals(20, p.y);
        }
        
        @Test
        @DisplayName("Creates copy of existing point")
        void constructFromPoint() {
            GridPoint original = new GridPoint(15, 25);
            GridPoint copy = new GridPoint(original);
            assertEquals(original, copy);
            assertNotSame(original, copy);
        }
        
        @Test
        @DisplayName("Origin constant is at (0,0)")
        void originConstant() {
            assertEquals(0, GridPoint.ORIGIN.x);
            assertEquals(0, GridPoint.ORIGIN.y);
        }
    }
    
    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticTest {
        
        @Test
        @DisplayName("translate creates new point with offset")
        void translateCreatesNewPoint() {
            GridPoint p = new GridPoint(10, 20);
            GridPoint translated = p.translate(5, -3);
            
            assertEquals(15, translated.x);
            assertEquals(17, translated.y);
            // Original unchanged
            assertEquals(10, p.x);
            assertEquals(20, p.y);
        }
        
        @Test
        @DisplayName("add combines two points")
        void addPoints() {
            GridPoint p1 = new GridPoint(10, 20);
            GridPoint p2 = new GridPoint(5, 15);
            GridPoint sum = p1.add(p2);
            
            assertEquals(15, sum.x);
            assertEquals(35, sum.y);
        }
        
        @Test
        @DisplayName("subtract finds difference of two points")
        void subtractPoints() {
            GridPoint p1 = new GridPoint(10, 20);
            GridPoint p2 = new GridPoint(3, 8);
            GridPoint diff = p1.subtract(p2);
            
            assertEquals(7, diff.x);
            assertEquals(12, diff.y);
        }
    }
    
    @Nested
    @DisplayName("Distance Calculations")
    class DistanceTest {
        
        @Test
        @DisplayName("distanceTo calculates Euclidean distance")
        void euclideanDistance() {
            GridPoint p1 = new GridPoint(0, 0);
            GridPoint p2 = new GridPoint(3, 4);
            
            assertEquals(5.0, p1.distanceTo(p2), 1e-10);
            assertEquals(5.0, p2.distanceTo(p1), 1e-10);
        }
        
        @Test
        @DisplayName("manhattanDistanceTo calculates taxicab distance")
        void manhattanDistance() {
            GridPoint p1 = new GridPoint(0, 0);
            GridPoint p2 = new GridPoint(3, 4);
            
            assertEquals(7, p1.manhattanDistanceTo(p2));
            assertEquals(7, p2.manhattanDistanceTo(p1));
        }
        
        @Test
        @DisplayName("distance to self is zero")
        void distanceToSelf() {
            GridPoint p = new GridPoint(10, 20);
            assertEquals(0.0, p.distanceTo(p), 1e-10);
            assertEquals(0, p.manhattanDistanceTo(p));
        }
    }
    
    @Nested
    @DisplayName("Region Checking")
    class RegionTest {
        
        @Test
        @DisplayName("isWithin returns true for point inside region")
        void pointInsideRegion() {
            GridPoint p = new GridPoint(5, 5);
            assertTrue(p.isWithin(0, 0, 10, 10));
        }
        
        @Test
        @DisplayName("isWithin returns false for point outside region")
        void pointOutsideRegion() {
            GridPoint p = new GridPoint(15, 5);
            assertFalse(p.isWithin(0, 0, 10, 10));
        }
        
        @Test
        @DisplayName("isWithin boundary is inclusive on min, exclusive on max")
        void boundaryBehavior() {
            GridPoint onMin = new GridPoint(0, 0);
            GridPoint onMax = new GridPoint(10, 10);
            
            assertTrue(onMin.isWithin(0, 0, 10, 10));  // Min inclusive
            assertFalse(onMax.isWithin(0, 0, 10, 10)); // Max exclusive
        }
    }
    
    @Nested
    @DisplayName("Equality and Hashing")
    class EqualityTest {
        
        @Test
        @DisplayName("equals returns true for same coordinates")
        void equalPoints() {
            GridPoint p1 = new GridPoint(10, 20);
            GridPoint p2 = new GridPoint(10, 20);
            
            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }
        
        @Test
        @DisplayName("equals returns false for different coordinates")
        void differentPoints() {
            GridPoint p1 = new GridPoint(10, 20);
            GridPoint p2 = new GridPoint(10, 21);
            
            assertNotEquals(p1, p2);
        }
        
        @Test
        @DisplayName("equals handles null and other types")
        void equalsEdgeCases() {
            GridPoint p = new GridPoint(10, 20);
            
            assertNotEquals(p, null);
            assertNotEquals(p, "not a point");
        }
    }
    
    @Nested
    @DisplayName("Comparison and Sorting")
    class ComparisonTest {
        
        @Test
        @DisplayName("compareTo orders by Y then X")
        void compareToOrdering() {
            GridPoint p1 = new GridPoint(5, 10);
            GridPoint p2 = new GridPoint(10, 5);
            GridPoint p3 = new GridPoint(15, 10);
            
            assertTrue(p2.compareTo(p1) < 0);  // p2.y < p1.y
            assertTrue(p1.compareTo(p3) < 0);  // same y, p1.x < p3.x
            assertEquals(0, p1.compareTo(new GridPoint(5, 10)));
        }
    }
    
    @Nested
    @DisplayName("AWT Compatibility")
    class AwtCompatibilityTest {
        
        @Test
        @DisplayName("toAwtPoint converts to java.awt.Point")
        void convertToAwt() {
            GridPoint gp = new GridPoint(10, 20);
            java.awt.Point awtp = gp.toAwtPoint();
            
            assertEquals(10, awtp.x);
            assertEquals(20, awtp.y);
        }
        
        @Test
        @DisplayName("fromAwtPoint converts from java.awt.Point")
        void convertFromAwt() {
            java.awt.Point awtp = new java.awt.Point(15, 25);
            GridPoint gp = GridPoint.fromAwtPoint(awtp);
            
            assertEquals(15, gp.x);
            assertEquals(25, gp.y);
        }
        
        @Test
        @DisplayName("round-trip conversion preserves values")
        void roundTripConversion() {
            GridPoint original = new GridPoint(42, 73);
            GridPoint roundTrip = GridPoint.fromAwtPoint(original.toAwtPoint());
            
            assertEquals(original, roundTrip);
        }
    }
    
    @Test
    @DisplayName("toString returns readable format")
    void toStringFormat() {
        GridPoint p = new GridPoint(10, 20);
        assertEquals("GridPoint[10, 20]", p.toString());
    }
}
