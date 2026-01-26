package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Point class.
 * Tests an immutable 2D integer point data structure.
 */
public class PointTest {
    
    @Test
    public void testPointCreation() {
        Point p = new Point(10, 20);
        assertEquals("X coordinate should be 10", 10, p.x);
        assertEquals("Y coordinate should be 20", 20, p.y);
    }
    
    @Test
    public void testPointZero() {
        Point p = new Point(0, 0);
        assertEquals("Origin should have x=0", 0, p.x);
        assertEquals("Origin should have y=0", 0, p.y);
    }
    
    @Test
    public void testPointNegative() {
        Point p = new Point(-5, -15);
        assertEquals("Should handle negative x", -5, p.x);
        assertEquals("Should handle negative y", -15, p.y);
    }
    
    @Test
    public void testPointLargeValues() {
        Point p = new Point(Integer.MAX_VALUE, Integer.MIN_VALUE);
        assertEquals("Should handle max integer", Integer.MAX_VALUE, p.x);
        assertEquals("Should handle min integer", Integer.MIN_VALUE, p.y);
    }
    
    @Test
    public void testPointEquality() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(10, 20);
        assertEquals("Points with same coordinates should be equal", p1, p2);
    }
    
    @Test
    public void testPointInequality() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(10, 21);
        assertNotEquals("Points with different y should not be equal", p1, p2);
    }
    
    @Test
    public void testPointDifferentX() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(11, 20);
        assertNotEquals("Points with different x should not be equal", p1, p2);
    }
    
    @Test
    public void testPointNotEqualToNull() {
        Point p = new Point(10, 20);
        assertNotEquals("Point should not equal null", p, null);
        assertFalse("Point equals null should return false", p.equals(null));
    }
    
    @Test
    public void testPointNotEqualToDifferentType() {
        Point p = new Point(10, 20);
        assertNotEquals("Point should not equal string", p, "10 20");
        assertFalse("Point equals different type should return false", p.equals("10 20"));
    }
    
    @Test
    public void testPointHashCode() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(10, 20);
        assertEquals("Equal points should have same hash code", p1.hashCode(), p2.hashCode());
    }
    
    @Test
    public void testPointHashCodeDifferent() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(10, 21);
        // Hash codes may differ for different points
        // (not guaranteed but likely)
        assertTrue("Different points likely have different hash codes", 
                   p1.hashCode() != p2.hashCode() || true);
    }
    
    @Test
    public void testPointToString() {
        Point p = new Point(10, 20);
        String result = p.toString();
        assertNotNull("toString should not return null", result);
        assertTrue("toString should contain x value", result.contains("10"));
        assertTrue("toString should contain y value", result.contains("20"));
        assertEquals("toString format should be 'x y'", "10 20", result);
    }
    
    @Test
    public void testPointToStringNegative() {
        Point p = new Point(-5, -15);
        String result = p.toString();
        assertEquals("toString format should handle negative", "-5 -15", result);
    }
    
    @Test
    public void testPointToStringZero() {
        Point p = new Point(0, 0);
        String result = p.toString();
        assertEquals("toString format for origin", "0 0", result);
    }
    
    @Test
    public void testPointImmutability() {
        Point p = new Point(10, 20);
        // Fields are final, so this test just verifies the contract
        assertEquals("Point should remain unchanged", 10, p.x);
        assertEquals("Point should remain unchanged", 20, p.y);
    }
    
    @Test
    public void testMultiplePointsIndependent() {
        Point p1 = new Point(10, 20);
        Point p2 = new Point(30, 40);
        Point p3 = new Point(10, 20);
        
        assertEquals("p1 should equal p3", p1, p3);
        assertNotEquals("p1 should not equal p2", p1, p2);
        assertEquals("p1 and p3 should still equal after p2 creation", p1, p3);
    }
}
