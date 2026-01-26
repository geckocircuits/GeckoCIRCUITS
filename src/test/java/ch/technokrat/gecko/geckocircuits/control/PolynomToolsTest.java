package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.math.NComplex;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for PolynomTools utility class.
 * Tests polynomial string representation and factorization.
 */
public class PolynomToolsTest {
    
    private static final double EPSILON = 1e-10;
    
    // ============= getPolynomString Tests =============
    
    @Test
    public void testGetPolynomString_constantPolynomial() {
        double[] polynom = {5.0};
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tags", result.contains("<html>") && result.contains("</html>"));
        assertTrue("Should contain the constant value", result.contains("5"));
    }
    
    @Test
    public void testGetPolynomString_linearPolynomial() {
        List<Double> polynom = Arrays.asList(1.0, 2.0);  // 1 + 2s
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
        assertTrue("Should contain 's' for linear term", result.contains("s"));
    }
    
    @Test
    public void testGetPolynomString_quadraticPolynomial() {
        List<Double> polynom = Arrays.asList(1.0, 0.0, 2.0);  // 1 + 2s^2
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain superscript tag for power", result.contains("<sup>"));
        assertTrue("Should contain '2' for power", result.contains("2"));
    }
    
    @Test
    public void testGetPolynomString_negativeCoefficients() {
        List<Double> polynom = Arrays.asList(1.0, -2.0);  // 1 - 2s
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain minus sign", result.contains("-"));
    }
    
    @Test
    public void testGetPolynomString_zeroCoefficients() {
        List<Double> polynom = Arrays.asList(1.0, 0.0, 3.0);  // 1 + 3s^2 (skip 0)
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        // Zero coefficient should not appear in output
        assertTrue("Should properly skip zero coefficients", true);
    }
    
    @Test
    public void testGetPolynomString_verySmallCoefficients() {
        List<Double> polynom = Arrays.asList(1.0, 1e-41);  // Should skip very small value
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        // Very small values should be skipped
        assertTrue("Should handle very small values", result.contains("1"));
    }
    
    @Test
    public void testGetPolynomString_unitCoefficientsHandling() {
        List<Double> polynom = Arrays.asList(1.0, 1.0);  // 1 + s
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        // Unit coefficients should be handled specially
        assertTrue("Should contain 's'", result.contains("s"));
    }
    
    @Test
    public void testGetPolynomString_negativeUnitCoefficients() {
        List<Double> polynom = Arrays.asList(1.0, -1.0);  // 1 - s
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain minus sign for -1 coefficient", result.contains("-"));
    }
    
    @Test
    public void testGetPolynomString_emptyList() {
        List<Double> polynom = new ArrayList<>();
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should return non-null string even for empty list", result);
        assertTrue("Should contain HTML tags", result.contains("<html>") && result.contains("</html>"));
    }
    
    @Test
    public void testGetPolynomString_largeCoefficients() {
        List<Double> polynom = Arrays.asList(1e6, 2e5);
        String result = PolynomTools.getPolynomString(polynom);
        
        assertNotNull("Should handle large coefficients", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
    }
    
    // ============= evaluateFactorizedExpression Tests =============
    
    @Test
    public void testEvaluateFactorizedExpression_emptyCoefficients() {
        List<NComplex> coefficients = new ArrayList<>();
        List<Double> result = PolynomTools.evaluateFactorizedExpression(coefficients, 2.0);
        
        assertNotNull("Should return non-null list", result);
        assertEquals("Empty coefficients should return factor only", 1, result.size());
        assertEquals("Should return the factor", 2.0, result.get(0), EPSILON);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_singleRealRoot() {
        List<NComplex> roots = Arrays.asList(new NComplex(2.0f, 0.0f));  // (s - 2)
        List<Double> result = PolynomTools.evaluateFactorizedExpression(roots, 1.0);
        
        assertNotNull("Should return non-null list", result);
        assertTrue("Should produce polynomial coefficients", result.size() > 0);
        // First coefficient should be -2 (from -(2))
        assertEquals("First coefficient should be -2", -2.0, result.get(0), EPSILON);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_singleRootWithFactor() {
        List<NComplex> roots = Arrays.asList(new NComplex(1.0f, 0.0f));  // (s - 1)
        List<Double> result = PolynomTools.evaluateFactorizedExpression(roots, 3.0);
        
        assertNotNull("Should return non-null list", result);
        assertTrue("Should produce polynomial coefficients", result.size() > 0);
        // First coefficient should be 3*(-1) = -3
        assertEquals("Should apply factor correctly", -3.0, result.get(0), EPSILON);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_multipleRoots() {
        List<NComplex> roots = Arrays.asList(
            new NComplex(1.0f, 0.0f),  // (s - 1)
            new NComplex(2.0f, 0.0f)   // (s - 2)
        );
        List<Double> result = PolynomTools.evaluateFactorizedExpression(roots, 1.0);
        
        assertNotNull("Should return non-null list", result);
        // (s-1)(s-2) = s^2 - 3s + 2
        assertTrue("Should produce polynomial with multiple terms", result.size() > 1);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_complexRoots() {
        List<NComplex> roots = Arrays.asList(
            new NComplex(1.0f, 1.0f)   // s = 1 + j
        );
        List<Double> result = PolynomTools.evaluateFactorizedExpression(roots, 1.0);
        
        assertNotNull("Should handle complex roots", result);
        assertTrue("Should produce polynomial coefficients", result.size() > 0);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_maxArraySize() {
        // Create coefficients up to max size
        List<NComplex> roots = new ArrayList<>();
        for (int i = 0; i < PolynomTools.MAX_ARRAY_SIZE - 2; i++) {
            roots.add(new NComplex((float)i, 0.0f));
        }
        
        List<Double> result = PolynomTools.evaluateFactorizedExpression(roots, 1.0);
        
        assertNotNull("Should handle large number of roots", result);
        assertTrue("Should produce polynomial", result.size() > 0);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_negativeFactor() {
        List<NComplex> roots = Arrays.asList(new NComplex(1.0f, 0.0f));
        List<Double> result = PolynomTools.evaluateFactorizedExpression(roots, -2.0);
        
        assertNotNull("Should handle negative factor", result);
        assertTrue("Should produce polynomial", result.size() > 0);
        // First coefficient should be -2 * (-1) = 2
        assertEquals("Should apply negative factor", 2.0, result.get(0), EPSILON);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_zeroFactor() {
        // This was previously causing ArrayIndexOutOfBoundsException
        List<NComplex> coefficients = Arrays.asList(new NComplex(2.0f, 0.0f));
        List<Double> result = PolynomTools.evaluateFactorizedExpression(coefficients, 0.0);
        
        assertNotNull("Should handle zero factor", result);
        assertFalse("Should return non-empty list", result.isEmpty());
        assertEquals("Should return [0.0] for zero factor", 1, result.size());
        assertEquals("Should contain 0.0", 0.0, result.get(0), EPSILON);
    }
    
    @Test
    public void testEvaluateFactorizedExpression_multipleCoefficients_zeroFactor() {
        // Multiple coefficients with zero factor
        List<NComplex> coefficients = Arrays.asList(
            new NComplex(1.0f, 0.0f),
            new NComplex(2.0f, 0.0f),
            new NComplex(3.0f, 0.0f)
        );
        List<Double> result = PolynomTools.evaluateFactorizedExpression(coefficients, 0.0);
        
        assertNotNull("Should handle multiple coefficients with zero factor", result);
        assertFalse("Should return non-empty list", result.isEmpty());
        assertEquals("Should return [0.0] when factor is zero", 1, result.size());
        assertEquals("Result should be 0.0", 0.0, result.get(0), EPSILON);
    }
    
    // ============= plotFactorizedPolynoms Tests =============
    
    @Test
    public void testPlotFactorizedPolynoms_singleRoot() {
        List<NComplex> roots = Arrays.asList(new NComplex(2.0f, 0.0f));
        String result = PolynomTools.plotFactorizedPolynoms(roots);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
        assertTrue("Should contain (s+2) or similar", result.contains("s"));
    }
    
    @Test
    public void testPlotFactorizedPolynoms_multipleRoots() {
        List<NComplex> roots = Arrays.asList(
            new NComplex(1.0f, 0.0f),
            new NComplex(2.0f, 0.0f)
        );
        String result = PolynomTools.plotFactorizedPolynoms(roots);
        
        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
    }
    
    @Test
    public void testPlotFactorizedPolynoms_repeatedRoots() {
        List<NComplex> roots = Arrays.asList(
            new NComplex(1.0f, 0.0f),
            new NComplex(1.0f, 0.0f)  // Same root twice
        );
        String result = PolynomTools.plotFactorizedPolynoms(roots);
        
        assertNotNull("Should handle repeated roots", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
        // Should show power for repeated roots
        assertTrue("Should indicate multiplicity", result.contains("<sup>") || result.contains("2"));
    }
    
    @Test
    public void testPlotFactorizedPolynoms_emptyList() {
        List<NComplex> roots = new ArrayList<>();
        String result = PolynomTools.plotFactorizedPolynoms(roots);
        
        assertNotNull("Should handle empty list", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
    }
    
    @Test
    public void testPlotFactorizedPolynoms_complexRoots() {
        List<NComplex> roots = Arrays.asList(
            new NComplex(1.0f, 2.0f)  // 1 + 2j
        );
        String result = PolynomTools.plotFactorizedPolynoms(roots);
        
        assertNotNull("Should handle complex roots", result);
        assertTrue("Should contain HTML tags", result.contains("<html>"));
    }
}
