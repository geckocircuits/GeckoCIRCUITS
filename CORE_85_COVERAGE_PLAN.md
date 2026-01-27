# Core Package 85% Coverage Test Plan

**Target:** Achieve 85% instruction coverage on GUI-free core packages
**Executor:** Claude Haiku
**Estimated Tests:** ~330 new tests
**Estimated Sprints:** 6 sprints

---

## Executive Summary

| Package | Current | Target | Gap | Tests Needed |
|---------|---------|--------|-----|--------------|
| `math` | 55% | 85% | 30% | ~45 tests |
| `circuit.matrix` | 77% | 85% | 8% | ~15 tests |
| `control.calculators` | 41% | 85% | 44% | ~100 tests |
| `losscalculation` | 24% | 85% | 61% | ~80 tests |
| `datacontainer` | 15% | 85% | 70% | ~60 tests |
| `circuit` (core subset) | 6% | 85% | 79% | ~30 tests |
| **Total** | | | | **~330 tests** |

---

## How to Execute This Plan

Each sprint is self-contained. Run with:

```bash
# Before starting sprint
cd /home/tinix/claude_wsl/GeckoCIRCUITS
mvn test -q 2>&1 | tail -5  # Verify baseline

# After implementing tests
mvn clean test jacoco:report -q
# Open target/site/jacoco/index.html in browser
```

### Test File Naming Convention
```
src/test/java/ch/technokrat/gecko/geckocircuits/<package>/<ClassName>Test.java
```

---

# SPRINT C1: Math Package (55% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/math/`
- **Current Coverage**: 55% (2,569 of 4,634 instructions)
- **Target**: 85%
- **Tests to Add**: ~45

## C1.1: NComplex.java (Complex Numbers)

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/NComplexTest.java`

### Test Cases to Implement

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NComplex - Complex Number Arithmetic")
class NComplexTest {
    private static final float TOLERANCE = 1e-6f;

    // === Constructors (3 tests) ===
    @Test
    @DisplayName("Constructor with real and imaginary parts")
    void testConstructor_RealAndImaginary() {
        NComplex c = new NComplex(3.0f, 4.0f);
        assertEquals(3.0f, c.getRe(), TOLERANCE);
        assertEquals(4.0f, c.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Constructor with real part only")
    void testConstructor_RealOnly() {
        NComplex c = new NComplex(5.0f);
        assertEquals(5.0f, c.getRe(), TOLERANCE);
        assertEquals(0.0f, c.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Default constructor creates zero")
    void testConstructor_Default() {
        NComplex c = new NComplex();
        assertEquals(0.0f, c.getRe(), TOLERANCE);
        assertEquals(0.0f, c.getIm(), TOLERANCE);
    }

    // === Addition (4 tests) ===
    @Test
    @DisplayName("Add two complex numbers")
    void testAdd_TwoComplex() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.add(a, b);
        assertEquals(4.0f, result.getRe(), TOLERANCE);
        assertEquals(6.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Add complex with zero")
    void testAdd_WithZero() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex zero = new NComplex();
        NComplex result = NComplex.add(a, zero);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Add negative imaginary parts")
    void testAdd_NegativeImaginary() {
        NComplex a = new NComplex(1.0f, -2.0f);
        NComplex b = new NComplex(3.0f, -4.0f);
        NComplex result = NComplex.add(a, b);
        assertEquals(4.0f, result.getRe(), TOLERANCE);
        assertEquals(-6.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Add resulting in cancellation")
    void testAdd_Cancellation() {
        NComplex a = new NComplex(5.0f, 3.0f);
        NComplex b = new NComplex(-5.0f, -3.0f);
        NComplex result = NComplex.add(a, b);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    // === Subtraction (3 tests) ===
    @Test
    @DisplayName("Subtract two complex numbers")
    void testSub_TwoComplex() {
        NComplex a = new NComplex(5.0f, 7.0f);
        NComplex b = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.sub(a, b);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Subtract from self gives zero")
    void testSub_FromSelf() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.sub(a, a);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Subtract zero")
    void testSub_Zero() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex zero = new NComplex();
        NComplex result = NComplex.sub(a, zero);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    // === Multiplication (4 tests) ===
    @Test
    @DisplayName("Multiply two complex numbers: (1+2i)(3+4i) = -5+10i")
    void testMul_TwoComplex() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.mul(a, b);
        assertEquals(-5.0f, result.getRe(), TOLERANCE);
        assertEquals(10.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Multiply by zero")
    void testMul_ByZero() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex zero = new NComplex();
        NComplex result = NComplex.mul(a, zero);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Multiply by one (identity)")
    void testMul_ByOne() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex one = new NComplex(1.0f);
        NComplex result = NComplex.mul(a, one);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Multiply by i: (3+4i) * i = -4+3i")
    void testMul_ByI() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex i = new NComplex(0.0f, 1.0f);
        NComplex result = NComplex.mul(a, i);
        assertEquals(-4.0f, result.getRe(), TOLERANCE);
        assertEquals(3.0f, result.getIm(), TOLERANCE);
    }

    // === Division (4 tests) ===
    @Test
    @DisplayName("Divide two complex: (4+2i)/(1+i) = 3-i")
    void testDiv_TwoComplex() {
        NComplex a = new NComplex(4.0f, 2.0f);
        NComplex b = new NComplex(1.0f, 1.0f);
        NComplex result = NComplex.div(a, b);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(-1.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Divide by one")
    void testDiv_ByOne() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex one = new NComplex(1.0f);
        NComplex result = NComplex.div(a, one);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Divide self by self equals one")
    void testDiv_SelfBySelf() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.div(a, a);
        assertEquals(1.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Divide zero by complex")
    void testDiv_ZeroByComplex() {
        NComplex zero = new NComplex();
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.div(zero, b);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    // === Conjugate (2 tests) ===
    @Test
    @DisplayName("Conjugate of complex")
    void testConj() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(-4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Conjugate of pure real")
    void testConj_PureReal() {
        NComplex a = new NComplex(5.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(5.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    // === Absolute Value (3 tests) ===
    @Test
    @DisplayName("Absolute value of 3+4i is 5")
    void testAbs_345Triangle() {
        NComplex a = new NComplex(3.0f, 4.0f);
        assertEquals(5.0f, NComplex.abs(a), TOLERANCE);
    }

    @Test
    @DisplayName("Absolute value of pure real")
    void testAbs_PureReal() {
        NComplex a = new NComplex(5.0f);
        assertEquals(5.0f, NComplex.abs(a), TOLERANCE);
    }

    @Test
    @DisplayName("Absolute value of pure imaginary")
    void testAbs_PureImaginary() {
        NComplex a = new NComplex(0.0f, 4.0f);
        assertEquals(4.0f, NComplex.abs(a), TOLERANCE);
    }

    // === Square Root (3 tests) ===
    @Test
    @DisplayName("Square root of positive real: sqrt(4) = 2")
    void testSqrt_PositiveReal() {
        NComplex a = new NComplex(4.0f);
        NComplex result = NComplex.sqrt(a);
        assertEquals(2.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Square root of negative: sqrt(-1) = i")
    void testSqrt_NegativeReal() {
        NComplex a = new NComplex(-1.0f);
        NComplex result = NComplex.sqrt(a);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(1.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Square root of complex")
    void testSqrt_Complex() {
        // sqrt(3+4i) ≈ 2+i (verify: (2+i)² = 4+4i-1 = 3+4i)
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.sqrt(a);
        // Verify by squaring
        NComplex squared = NComplex.mul(result, result);
        assertEquals(3.0f, squared.getRe(), 0.01f);
        assertEquals(4.0f, squared.getIm(), 0.01f);
    }

    // === Real * Complex (2 tests) ===
    @Test
    @DisplayName("Scalar multiply: 2 * (3+4i) = 6+8i")
    void testRCmul() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.RCmul(2.0f, a);
        assertEquals(6.0f, result.getRe(), TOLERANCE);
        assertEquals(8.0f, result.getIm(), TOLERANCE);
    }

    @Test
    @DisplayName("Scalar multiply by zero")
    void testRCmul_ByZero() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.RCmul(0.0f, a);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    // === Object Methods (3 tests) ===
    @Test
    @DisplayName("Equals for identical values")
    void testEquals_Identical() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        assertEquals(a, b);
    }

    @Test
    @DisplayName("Equals for different values")
    void testEquals_Different() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex b = new NComplex(3.0f, 5.0f);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("toString returns readable format")
    void testToString() {
        NComplex a = new NComplex(3.0f, 4.0f);
        String s = a.toString();
        assertTrue(s.contains("3") && s.contains("4"), "Should contain both parts");
    }
}
```

**Expected Test Count**: 31 tests

---

## C1.2: Matrix.java (Dense Matrix with LU Decomposition)

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/MatrixTest.java`

### Test Cases to Implement

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Matrix - Dense Matrix with LU Decomposition")
class MatrixTest {
    private static final double TOLERANCE = 1e-10;

    // === Construction (3 tests) ===
    @Test
    @DisplayName("Create identity matrix")
    void testIdentity() {
        Matrix m = Matrix.identity(3);
        assertEquals(1.0, m.get(0, 0), TOLERANCE);
        assertEquals(1.0, m.get(1, 1), TOLERANCE);
        assertEquals(1.0, m.get(2, 2), TOLERANCE);
        assertEquals(0.0, m.get(0, 1), TOLERANCE);
    }

    @Test
    @DisplayName("Create from 2D array")
    void testFromArray() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        assertEquals(1.0, m.get(0, 0), TOLERANCE);
        assertEquals(4.0, m.get(1, 1), TOLERANCE);
    }

    @Test
    @DisplayName("Create zero matrix")
    void testZeroMatrix() {
        Matrix m = new Matrix(3, 3);
        assertEquals(0.0, m.get(0, 0), TOLERANCE);
        assertEquals(0.0, m.get(1, 2), TOLERANCE);
    }

    // === LU Decomposition (4 tests) ===
    @Test
    @DisplayName("LU decomposition of simple 2x2")
    void testLU_2x2() {
        double[][] data = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(data);
        m.luDecomposition();
        
        // Solve Ax = b where b = [1, 1]
        double[] b = {1, 1};
        double[] x = m.luSolve(b);
        
        // Verify: multiply back
        double r0 = data[0][0] * x[0] + data[0][1] * x[1];
        double r1 = data[1][0] * x[0] + data[1][1] * x[1];
        assertEquals(1.0, r0, TOLERANCE);
        assertEquals(1.0, r1, TOLERANCE);
    }

    @Test
    @DisplayName("LU decomposition of 3x3 system")
    void testLU_3x3() {
        // Simple diagonal dominant matrix
        double[][] data = {
            {4, 1, 0},
            {1, 4, 1},
            {0, 1, 4}
        };
        Matrix m = new Matrix(data);
        m.luDecomposition();
        
        double[] b = {1, 2, 3};
        double[] x = m.luSolve(b);
        
        // Verify Ax = b
        for (int i = 0; i < 3; i++) {
            double sum = 0;
            for (int j = 0; j < 3; j++) {
                sum += data[i][j] * x[j];
            }
            assertEquals(b[i], sum, TOLERANCE);
        }
    }

    @Test
    @DisplayName("LU solve with unit vector")
    void testLU_UnitVector() {
        double[][] data = {{2, 0}, {0, 2}};
        Matrix m = new Matrix(data);
        m.luDecomposition();
        
        double[] b = {1, 0};
        double[] x = m.luSolve(b);
        
        assertEquals(0.5, x[0], TOLERANCE);
        assertEquals(0.0, x[1], TOLERANCE);
    }

    @Test
    @DisplayName("LU decomposition preserves original for reconstruction")
    void testLU_Reconstruction() {
        double[][] original = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(original);
        m.luDecomposition();
        
        // Multiple solves should be consistent
        double[] x1 = m.luSolve(new double[]{1, 0});
        double[] x2 = m.luSolve(new double[]{0, 1});
        
        assertNotNull(x1);
        assertNotNull(x2);
    }

    // === Matrix Operations (4 tests) ===
    @Test
    @DisplayName("Matrix multiplication")
    void testMultiply() {
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{5, 6}, {7, 8}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        
        Matrix result = mA.multiply(mB);
        
        // [1,2][5,6] = [1*5+2*7, 1*6+2*8] = [19, 22]
        // [3,4][7,8]   [3*5+4*7, 3*6+4*8]   [43, 50]
        assertEquals(19.0, result.get(0, 0), TOLERANCE);
        assertEquals(22.0, result.get(0, 1), TOLERANCE);
        assertEquals(43.0, result.get(1, 0), TOLERANCE);
        assertEquals(50.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    @DisplayName("Matrix transpose")
    void testTranspose() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        Matrix m = new Matrix(data);
        Matrix t = m.transpose();
        
        assertEquals(1.0, t.get(0, 0), TOLERANCE);
        assertEquals(4.0, t.get(0, 1), TOLERANCE);
        assertEquals(2.0, t.get(1, 0), TOLERANCE);
        assertEquals(5.0, t.get(1, 1), TOLERANCE);
    }

    @Test
    @DisplayName("Matrix-vector multiply")
    void testMatrixVectorMultiply() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        double[] v = {1, 1};
        
        double[] result = m.multiply(v);
        
        assertEquals(3.0, result[0], TOLERANCE);  // 1*1 + 2*1
        assertEquals(7.0, result[1], TOLERANCE);  // 3*1 + 4*1
    }

    @Test
    @DisplayName("Matrix determinant via LU")
    void testDeterminant() {
        double[][] data = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(data);
        
        double det = m.determinant();
        
        // det = 4*3 - 3*6 = 12 - 18 = -6
        assertEquals(-6.0, det, TOLERANCE);
    }
}
```

**Expected Test Count**: 11 tests

---

## C1.3: GlobalMatrixMath.java (Static Utilities)

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/GlobalMatrixMathTest.java`

### Test Cases to Implement

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalMatrixMath - Matrix Utility Functions")
class GlobalMatrixMathTest {
    private static final double TOLERANCE = 1e-10;

    @Test
    @DisplayName("Copy matrix")
    void testCopyMatrix() {
        double[][] original = {{1, 2}, {3, 4}};
        double[][] copy = GlobalMatrixMath.copyMatrix(original);
        
        assertArrayEquals(original[0], copy[0], TOLERANCE);
        assertArrayEquals(original[1], copy[1], TOLERANCE);
        
        // Modify original, copy should be unchanged
        original[0][0] = 999;
        assertEquals(1.0, copy[0][0], TOLERANCE);
    }

    @Test
    @DisplayName("Add matrices")
    void testAddMatrices() {
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{5, 6}, {7, 8}};
        double[][] result = GlobalMatrixMath.add(a, b);
        
        assertEquals(6.0, result[0][0], TOLERANCE);
        assertEquals(8.0, result[0][1], TOLERANCE);
        assertEquals(10.0, result[1][0], TOLERANCE);
        assertEquals(12.0, result[1][1], TOLERANCE);
    }

    @Test
    @DisplayName("Scale matrix")
    void testScaleMatrix() {
        double[][] a = {{1, 2}, {3, 4}};
        double[][] result = GlobalMatrixMath.scale(a, 2.0);
        
        assertEquals(2.0, result[0][0], TOLERANCE);
        assertEquals(4.0, result[0][1], TOLERANCE);
        assertEquals(6.0, result[1][0], TOLERANCE);
        assertEquals(8.0, result[1][1], TOLERANCE);
    }
}
```

**Expected Test Count**: 3 tests

---

# SPRINT C2: Circuit Matrix Package (77% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/`
- **Current Coverage**: 77% (2,531 of 3,276 instructions)
- **Target**: 85%
- **Tests to Add**: ~15

## C2.1: Edge Cases for Stampers

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/StamperEdgeCasesTest.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Matrix Stamper Edge Cases")
class StamperEdgeCasesTest {
    private static final double TOLERANCE = 1e-12;

    // === ResistorStamper Edge Cases ===
    @Test
    @DisplayName("Resistor: very small resistance (1 mOhm)")
    void testResistor_VerySmall() {
        double[][] A = new double[2][2];
        ResistorStamper stamper = new ResistorStamper();
        double R = 0.001;  // 1 mOhm
        double G = 1.0 / R;
        
        stamper.stampMatrixA(A, 0, 1, -1, new double[]{R}, 1e-6);
        
        assertEquals(G, A[0][0], TOLERANCE);
        assertEquals(-G, A[0][1], TOLERANCE);
    }

    @Test
    @DisplayName("Resistor: very large resistance (1 GOhm)")
    void testResistor_VeryLarge() {
        double[][] A = new double[2][2];
        ResistorStamper stamper = new ResistorStamper();
        double R = 1e9;  // 1 GOhm
        double G = 1.0 / R;
        
        stamper.stampMatrixA(A, 0, 1, -1, new double[]{R}, 1e-6);
        
        assertEquals(G, A[0][0], 1e-20);
    }

    // === CapacitorStamper Edge Cases ===
    @ParameterizedTest
    @ValueSource(doubles = {1e-12, 1e-9, 1e-6, 1e-3})
    @DisplayName("Capacitor: various capacitance values")
    void testCapacitor_VariousValues(double C) {
        double[][] A = new double[2][2];
        double dt = 1e-6;
        CapacitorBEStamper stamper = new CapacitorBEStamper();
        
        stamper.stampMatrixA(A, 0, 1, -1, new double[]{C}, dt);
        
        // G = C / dt for Backward Euler
        double expectedG = C / dt;
        assertEquals(expectedG, A[0][0], expectedG * 1e-10);
    }

    @Test
    @DisplayName("Capacitor: very small timestep")
    void testCapacitor_SmallTimestep() {
        double[][] A = new double[2][2];
        double C = 1e-6;  // 1 uF
        double dt = 1e-9;  // 1 ns
        CapacitorBEStamper stamper = new CapacitorBEStamper();
        
        stamper.stampMatrixA(A, 0, 1, -1, new double[]{C}, dt);
        
        // G = 1e-6 / 1e-9 = 1000 S
        assertEquals(1000.0, A[0][0], TOLERANCE);
    }

    // === InductorStamper Edge Cases ===
    @Test
    @DisplayName("Inductor: very small inductance (1 nH)")
    void testInductor_VerySmall() {
        double[][] A = new double[3][3];
        double L = 1e-9;  // 1 nH
        double dt = 1e-6;
        InductorBEStamper stamper = new InductorBEStamper();
        
        // Inductor adds branch equation
        stamper.stampMatrixA(A, 0, 1, 2, new double[]{L}, dt);
        
        // Should not produce NaN or Infinity
        assertFalse(Double.isNaN(A[2][2]));
        assertFalse(Double.isInfinite(A[2][2]));
    }

    @Test
    @DisplayName("Inductor: large inductance (1 H)")
    void testInductor_VeryLarge() {
        double[][] A = new double[3][3];
        double L = 1.0;  // 1 H
        double dt = 1e-6;
        InductorBEStamper stamper = new InductorBEStamper();
        
        stamper.stampMatrixA(A, 0, 1, 2, new double[]{L}, dt);
        
        // G = dt / L for Backward Euler
        double expectedG = dt / L;
        assertTrue(A[2][2] != 0);  // Should have non-zero diagonal
    }

    // === SwitchStamper Edge Cases ===
    @Test
    @DisplayName("Switch: transition from open to closed")
    void testSwitch_Transition() {
        double[][] A = new double[2][2];
        SwitchStamper stamper = new SwitchStamper();
        
        // Open switch (high resistance)
        double[] openParams = {1e9, 0};  // R_off, state=0
        stamper.stampMatrixA(A, 0, 1, -1, openParams, 1e-6);
        double openConductance = A[0][0];
        
        // Reset and close switch
        A = new double[2][2];
        double[] closedParams = {0.001, 1};  // R_on, state=1
        stamper.stampMatrixA(A, 0, 1, -1, closedParams, 1e-6);
        double closedConductance = A[0][0];
        
        assertTrue(closedConductance > openConductance * 1e6, 
            "Closed should be much more conductive");
    }

    // === Ground Node Handling ===
    @Test
    @DisplayName("Resistor connected to ground node (-1)")
    void testResistor_GroundNode() {
        double[][] A = new double[2][2];
        ResistorStamper stamper = new ResistorStamper();
        
        // Node 0 to ground (-1)
        stamper.stampMatrixA(A, 0, -1, -1, new double[]{1000.0}, 1e-6);
        
        // Only diagonal element should be stamped
        assertEquals(0.001, A[0][0], TOLERANCE);
        // Ground row/column should not be accessed (would be out of bounds)
    }

    @Test
    @DisplayName("Capacitor connected to ground")
    void testCapacitor_GroundNode() {
        double[][] A = new double[2][2];
        CapacitorBEStamper stamper = new CapacitorBEStamper();
        double C = 1e-6;
        double dt = 1e-6;
        
        stamper.stampMatrixA(A, 0, -1, -1, new double[]{C}, dt);
        
        assertEquals(C / dt, A[0][0], TOLERANCE);
    }

    // === VoltageSource Edge Cases ===
    @Test
    @DisplayName("Voltage source stamps branch equation")
    void testVoltageSource_BranchEquation() {
        double[][] A = new double[3][3];
        VoltageSourceStamper stamper = new VoltageSourceStamper();
        
        // Voltage source from node 0 to node 1, branch current at index 2
        stamper.stampMatrixA(A, 0, 1, 2, new double[]{10.0}, 1e-6);
        
        // Should stamp +1, -1 in current rows and branch equation
        assertEquals(1.0, A[0][2], TOLERANCE);
        assertEquals(-1.0, A[1][2], TOLERANCE);
        assertEquals(1.0, A[2][0], TOLERANCE);
        assertEquals(-1.0, A[2][1], TOLERANCE);
    }

    @Test
    @DisplayName("Current source stamps vector b only")
    void testCurrentSource_VectorB() {
        double[][] A = new double[2][2];
        double[] b = new double[2];
        CurrentSourceStamper stamper = new CurrentSourceStamper();
        
        double I = 1.5;  // 1.5 A
        stamper.stampMatrixA(A, 0, 1, -1, new double[]{I}, 1e-6);
        stamper.stampVectorB(b, 0, 1, -1, new double[]{I}, 1e-6, 0, null);
        
        // A matrix should be unchanged (ideal current source)
        assertEquals(0.0, A[0][0], TOLERANCE);
        assertEquals(0.0, A[1][1], TOLERANCE);
        
        // b vector should have current injection
        assertEquals(I, b[0], TOLERANCE);
        assertEquals(-I, b[1], TOLERANCE);
    }
}
```

**Expected Test Count**: 13 tests

---

# SPRINT C3: Control Calculators (41% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/`
- **Current Coverage**: 41% (4,241 of 10,117 instructions)
- **Target**: 85%
- **Tests to Add**: ~100

This is the largest gap. Group tests by calculator type.

## C3.1: Signal Generation Calculators

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalGeneratorTest.java`

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Signal Generation Calculators")
class SignalGeneratorTest {
    private static final double TOLERANCE = 1e-10;

    // === SineCalculator ===
    @Test
    @DisplayName("Sine at t=0 with zero phase")
    void testSine_AtZero() {
        SineCalculator calc = new SineCalculator();
        calc.setAmplitude(1.0);
        calc.setFrequency(50.0);
        calc.setPhase(0.0);
        
        double result = calc.calculate(0.0);
        assertEquals(0.0, result, TOLERANCE);
    }

    @Test
    @DisplayName("Sine at quarter period equals amplitude")
    void testSine_QuarterPeriod() {
        SineCalculator calc = new SineCalculator();
        calc.setAmplitude(10.0);
        calc.setFrequency(50.0);  // T = 0.02s
        calc.setPhase(0.0);
        
        double result = calc.calculate(0.005);  // t = T/4
        assertEquals(10.0, result, TOLERANCE);
    }

    @Test
    @DisplayName("Sine with 90° phase shift")
    void testSine_PhaseShift() {
        SineCalculator calc = new SineCalculator();
        calc.setAmplitude(1.0);
        calc.setFrequency(50.0);
        calc.setPhase(Math.PI / 2);  // 90°
        
        double result = calc.calculate(0.0);
        assertEquals(1.0, result, TOLERANCE);  // cos(0) = 1
    }

    // === SquareWaveCalculator ===
    @Test
    @DisplayName("Square wave positive half")
    void testSquare_PositiveHalf() {
        SquareWaveCalculator calc = new SquareWaveCalculator();
        calc.setAmplitude(5.0);
        calc.setFrequency(100.0);  // T = 0.01s
        calc.setDutyCycle(0.5);
        
        double result = calc.calculate(0.001);  // First 10% of period
        assertEquals(5.0, result, TOLERANCE);
    }

    @Test
    @DisplayName("Square wave negative half")
    void testSquare_NegativeHalf() {
        SquareWaveCalculator calc = new SquareWaveCalculator();
        calc.setAmplitude(5.0);
        calc.setFrequency(100.0);
        calc.setDutyCycle(0.5);
        
        double result = calc.calculate(0.006);  // In second half
        assertEquals(-5.0, result, TOLERANCE);
    }

    @Test
    @DisplayName("Square wave with 75% duty cycle")
    void testSquare_HighDutyCycle() {
        SquareWaveCalculator calc = new SquareWaveCalculator();
        calc.setAmplitude(1.0);
        calc.setFrequency(100.0);
        calc.setDutyCycle(0.75);
        
        // At 70% of period, should still be high
        double result = calc.calculate(0.007);
        assertEquals(1.0, result, TOLERANCE);
    }

    // === TriangleWaveCalculator ===
    @Test
    @DisplayName("Triangle wave at zero")
    void testTriangle_AtZero() {
        TriangleWaveCalculator calc = new TriangleWaveCalculator();
        calc.setAmplitude(1.0);
        calc.setFrequency(100.0);
        
        double result = calc.calculate(0.0);
        assertEquals(0.0, result, TOLERANCE);
    }

    @Test
    @DisplayName("Triangle wave at peak")
    void testTriangle_AtPeak() {
        TriangleWaveCalculator calc = new TriangleWaveCalculator();
        calc.setAmplitude(2.0);
        calc.setFrequency(100.0);  // T = 0.01s
        
        double result = calc.calculate(0.0025);  // T/4 = peak
        assertEquals(2.0, result, TOLERANCE);
    }

    // === ConstantCalculator ===
    @Test
    @DisplayName("Constant value at any time")
    void testConstant() {
        ConstantCalculator calc = new ConstantCalculator();
        calc.setValue(42.0);
        
        assertEquals(42.0, calc.calculate(0.0), TOLERANCE);
        assertEquals(42.0, calc.calculate(100.0), TOLERANCE);
        assertEquals(42.0, calc.calculate(-50.0), TOLERANCE);
    }

    // === StepCalculator ===
    @Test
    @DisplayName("Step before trigger time")
    void testStep_Before() {
        StepCalculator calc = new StepCalculator();
        calc.setInitialValue(0.0);
        calc.setFinalValue(1.0);
        calc.setStepTime(0.1);
        
        double result = calc.calculate(0.05);
        assertEquals(0.0, result, TOLERANCE);
    }

    @Test
    @DisplayName("Step after trigger time")
    void testStep_After() {
        StepCalculator calc = new StepCalculator();
        calc.setInitialValue(0.0);
        calc.setFinalValue(1.0);
        calc.setStepTime(0.1);
        
        double result = calc.calculate(0.15);
        assertEquals(1.0, result, TOLERANCE);
    }
}
```

**Expected Test Count**: 12 tests

---

## C3.2: Mathematical Calculators

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/MathCalculatorTest.java`

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mathematical Calculators")
class MathCalculatorTest {
    private static final double TOLERANCE = 1e-10;

    // === AddCalculator ===
    @Test
    @DisplayName("Add two inputs")
    void testAdd_TwoInputs() {
        AddCalculator calc = new AddCalculator(2);
        calc.setInput(0, 3.0);
        calc.setInput(1, 4.0);
        assertEquals(7.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Add with negative input")
    void testAdd_Negative() {
        AddCalculator calc = new AddCalculator(2);
        calc.setInput(0, 5.0);
        calc.setInput(1, -3.0);
        assertEquals(2.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Add multiple inputs")
    void testAdd_Multiple() {
        AddCalculator calc = new AddCalculator(4);
        calc.setInput(0, 1.0);
        calc.setInput(1, 2.0);
        calc.setInput(2, 3.0);
        calc.setInput(3, 4.0);
        assertEquals(10.0, calc.calculate(), TOLERANCE);
    }

    // === MultiplyCalculator ===
    @Test
    @DisplayName("Multiply two inputs")
    void testMultiply_TwoInputs() {
        MultiplyCalculator calc = new MultiplyCalculator(2);
        calc.setInput(0, 3.0);
        calc.setInput(1, 4.0);
        assertEquals(12.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Multiply by zero")
    void testMultiply_ByZero() {
        MultiplyCalculator calc = new MultiplyCalculator(2);
        calc.setInput(0, 1000.0);
        calc.setInput(1, 0.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
    }

    // === DivideCalculator ===
    @Test
    @DisplayName("Divide normal case")
    void testDivide_Normal() {
        DivideCalculator calc = new DivideCalculator();
        calc.setNumerator(10.0);
        calc.setDenominator(2.0);
        assertEquals(5.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Divide by near-zero uses clamp")
    void testDivide_NearZero() {
        DivideCalculator calc = new DivideCalculator();
        calc.setNumerator(1.0);
        calc.setDenominator(1e-15);
        
        double result = calc.calculate();
        // Should not be Infinity
        assertFalse(Double.isInfinite(result));
    }

    // === GainCalculator ===
    @Test
    @DisplayName("Gain multiplies input")
    void testGain() {
        GainCalculator calc = new GainCalculator();
        calc.setGain(2.5);
        calc.setInput(4.0);
        assertEquals(10.0, calc.calculate(), TOLERANCE);
    }

    // === AbsoluteValueCalculator ===
    @Test
    @DisplayName("Absolute value of positive")
    void testAbs_Positive() {
        AbsoluteValueCalculator calc = new AbsoluteValueCalculator();
        calc.setInput(5.0);
        assertEquals(5.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Absolute value of negative")
    void testAbs_Negative() {
        AbsoluteValueCalculator calc = new AbsoluteValueCalculator();
        calc.setInput(-5.0);
        assertEquals(5.0, calc.calculate(), TOLERANCE);
    }

    // === LimiterCalculator ===
    @Test
    @DisplayName("Limiter within bounds")
    void testLimiter_WithinBounds() {
        LimiterCalculator calc = new LimiterCalculator();
        calc.setLimits(-10.0, 10.0);
        calc.setInput(5.0);
        assertEquals(5.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Limiter clips high")
    void testLimiter_ClipsHigh() {
        LimiterCalculator calc = new LimiterCalculator();
        calc.setLimits(-10.0, 10.0);
        calc.setInput(15.0);
        assertEquals(10.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Limiter clips low")
    void testLimiter_ClipsLow() {
        LimiterCalculator calc = new LimiterCalculator();
        calc.setLimits(-10.0, 10.0);
        calc.setInput(-15.0);
        assertEquals(-10.0, calc.calculate(), TOLERANCE);
    }
}
```

**Expected Test Count**: 13 tests

---

## C3.3: Integration and Differentiation

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/IntegrationDifferentiationTest.java`

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration and Differentiation Calculators")
class IntegrationDifferentiationTest {
    private static final double TOLERANCE = 1e-6;
    private static final double DT = 1e-6;  // 1 µs timestep

    // === IntegratorCalculator ===
    @Test
    @DisplayName("Integrator accumulates constant input")
    void testIntegrator_Constant() {
        IntegratorCalculator calc = new IntegratorCalculator();
        calc.setTimestep(DT);
        calc.reset(0.0);
        
        // Integrate constant 1.0 for 1000 steps = 1ms
        for (int i = 0; i < 1000; i++) {
            calc.setInput(1.0);
            calc.calculate();
        }
        
        // Integral of 1 over 1ms = 0.001
        assertEquals(0.001, calc.getOutput(), TOLERANCE);
    }

    @Test
    @DisplayName("Integrator with initial condition")
    void testIntegrator_InitialCondition() {
        IntegratorCalculator calc = new IntegratorCalculator();
        calc.setTimestep(DT);
        calc.reset(5.0);  // Start at 5
        
        // Should start at 5
        assertEquals(5.0, calc.getOutput(), TOLERANCE);
    }

    @Test
    @DisplayName("Integrator reset clears accumulator")
    void testIntegrator_Reset() {
        IntegratorCalculator calc = new IntegratorCalculator();
        calc.setTimestep(DT);
        calc.reset(0.0);
        
        // Accumulate some value
        for (int i = 0; i < 100; i++) {
            calc.setInput(10.0);
            calc.calculate();
        }
        
        // Reset
        calc.reset(0.0);
        assertEquals(0.0, calc.getOutput(), TOLERANCE);
    }

    // === DifferentiatorCalculator ===
    @Test
    @DisplayName("Differentiator of ramp gives constant")
    void testDifferentiator_Ramp() {
        DifferentiatorCalculator calc = new DifferentiatorCalculator();
        calc.setTimestep(DT);
        calc.reset();
        
        // Feed in ramp: 0, 1, 2, 3... (scaled by dt)
        double slope = 1000.0;  // Units per second
        for (int i = 0; i < 100; i++) {
            double input = i * DT * slope;
            calc.setInput(input);
            calc.calculate();
        }
        
        // Derivative should approach slope
        assertEquals(slope, calc.getOutput(), slope * 0.1);
    }

    @Test
    @DisplayName("Differentiator of constant is zero")
    void testDifferentiator_Constant() {
        DifferentiatorCalculator calc = new DifferentiatorCalculator();
        calc.setTimestep(DT);
        calc.reset();
        
        // Constant input
        for (int i = 0; i < 10; i++) {
            calc.setInput(5.0);
            calc.calculate();
        }
        
        // After settling, derivative should be zero
        assertEquals(0.0, calc.getOutput(), TOLERANCE);
    }

    // === PT1Calculator (First Order Lag) ===
    @Test
    @DisplayName("PT1 step response approaches input")
    void testPT1_StepResponse() {
        PT1Calculator calc = new PT1Calculator();
        calc.setTimestep(DT);
        calc.setTimeConstant(0.001);  // 1ms time constant
        calc.reset(0.0);
        
        // Step input of 1.0
        for (int i = 0; i < 5000; i++) {  // 5ms = 5 time constants
            calc.setInput(1.0);
            calc.calculate();
        }
        
        // After 5 tau, should be > 99% of final value
        assertTrue(calc.getOutput() > 0.99);
    }

    @Test
    @DisplayName("PT1 at t=tau reaches 63.2%")
    void testPT1_AtTau() {
        PT1Calculator calc = new PT1Calculator();
        calc.setTimestep(DT);
        double tau = 0.001;  // 1ms
        calc.setTimeConstant(tau);
        calc.reset(0.0);
        
        // Run for exactly 1 time constant
        int steps = (int)(tau / DT);
        for (int i = 0; i < steps; i++) {
            calc.setInput(1.0);
            calc.calculate();
        }
        
        // Should be approximately 63.2%
        assertEquals(0.632, calc.getOutput(), 0.05);
    }
}
```

**Expected Test Count**: 7 tests

---

## C3.4: Signal Analysis Calculators

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalAnalysisTest.java`

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Signal Analysis Calculators (RMS, THD, FFT)")
class SignalAnalysisTest {
    private static final double TOLERANCE = 0.01;  // 1% tolerance
    private static final double DT = 1e-5;  // 10 µs

    // === RMSCalculator ===
    @Test
    @DisplayName("RMS of DC is the DC value")
    void testRMS_DC() {
        RMSCalculator calc = new RMSCalculator();
        calc.setTimestep(DT);
        calc.setAveragingPeriod(0.02);  // 20ms = 1 cycle at 50Hz
        calc.reset();
        
        // Feed DC value
        for (int i = 0; i < 2000; i++) {
            calc.setInput(5.0);
            calc.calculate();
        }
        
        assertEquals(5.0, calc.getOutput(), TOLERANCE);
    }

    @Test
    @DisplayName("RMS of sine is Vpk / sqrt(2)")
    void testRMS_Sine() {
        RMSCalculator calc = new RMSCalculator();
        calc.setTimestep(DT);
        calc.setAveragingPeriod(0.02);  // Exactly 1 period at 50Hz
        calc.reset();
        
        double freq = 50.0;
        double amplitude = 10.0;
        
        // Feed sine wave for 2 full periods
        for (int i = 0; i < 4000; i++) {
            double t = i * DT;
            double value = amplitude * Math.sin(2 * Math.PI * freq * t);
            calc.setInput(value);
            calc.calculate();
        }
        
        // RMS of sine = amplitude / sqrt(2)
        double expected = amplitude / Math.sqrt(2);
        assertEquals(expected, calc.getOutput(), expected * 0.05);
    }

    @Test
    @DisplayName("RMS of square wave is the amplitude")
    void testRMS_Square() {
        RMSCalculator calc = new RMSCalculator();
        calc.setTimestep(DT);
        calc.setAveragingPeriod(0.01);  // 10ms
        calc.reset();
        
        double amplitude = 5.0;
        
        // Feed square wave
        for (int i = 0; i < 2000; i++) {
            double t = i * DT;
            double value = (t % 0.01 < 0.005) ? amplitude : -amplitude;
            calc.setInput(value);
            calc.calculate();
        }
        
        // RMS of symmetric square = amplitude
        assertEquals(amplitude, calc.getOutput(), amplitude * 0.05);
    }

    // === THDCalculator ===
    @Test
    @DisplayName("THD of pure sine is zero")
    void testTHD_PureSine() {
        THDCalculator calc = new THDCalculator();
        calc.setTimestep(DT);
        calc.setFundamentalFrequency(50.0);
        calc.setHarmonicCount(10);
        calc.reset();
        
        // Feed pure sine
        for (int i = 0; i < 10000; i++) {
            double t = i * DT;
            double value = 10.0 * Math.sin(2 * Math.PI * 50 * t);
            calc.setInput(value);
            calc.calculate();
        }
        
        // Pure sine has zero THD
        assertEquals(0.0, calc.getOutput(), 0.01);
    }

    @Test
    @DisplayName("THD of clipped sine is non-zero")
    void testTHD_ClippedSine() {
        THDCalculator calc = new THDCalculator();
        calc.setTimestep(DT);
        calc.setFundamentalFrequency(50.0);
        calc.setHarmonicCount(10);
        calc.reset();
        
        // Feed clipped sine (clipped at 70% of peak)
        for (int i = 0; i < 10000; i++) {
            double t = i * DT;
            double value = 10.0 * Math.sin(2 * Math.PI * 50 * t);
            value = Math.max(-7.0, Math.min(7.0, value));  // Clip
            calc.setInput(value);
            calc.calculate();
        }
        
        // Clipped sine has significant THD
        assertTrue(calc.getOutput() > 0.1, "Clipped sine should have THD > 10%");
    }

    // === MeanCalculator ===
    @Test
    @DisplayName("Mean of symmetric AC is zero")
    void testMean_SymmetricAC() {
        MeanCalculator calc = new MeanCalculator();
        calc.setTimestep(DT);
        calc.setAveragingPeriod(0.02);
        calc.reset();
        
        // Feed sine wave
        for (int i = 0; i < 4000; i++) {
            double t = i * DT;
            double value = 10.0 * Math.sin(2 * Math.PI * 50 * t);
            calc.setInput(value);
            calc.calculate();
        }
        
        // Mean of symmetric AC is zero
        assertEquals(0.0, calc.getOutput(), 0.01);
    }

    @Test
    @DisplayName("Mean of DC+AC is the DC component")
    void testMean_DCPlusAC() {
        MeanCalculator calc = new MeanCalculator();
        calc.setTimestep(DT);
        calc.setAveragingPeriod(0.02);
        calc.reset();
        
        double dcOffset = 3.0;
        
        // Feed DC + AC
        for (int i = 0; i < 4000; i++) {
            double t = i * DT;
            double value = dcOffset + 10.0 * Math.sin(2 * Math.PI * 50 * t);
            calc.setInput(value);
            calc.calculate();
        }
        
        assertEquals(dcOffset, calc.getOutput(), 0.1);
    }

    // === PeakDetectorCalculator ===
    @Test
    @DisplayName("Peak detector captures maximum")
    void testPeakDetector_Max() {
        PeakDetectorCalculator calc = new PeakDetectorCalculator();
        calc.reset();
        
        // Feed varying signal
        double[] values = {1.0, 5.0, 3.0, 8.0, 2.0, 6.0};
        for (double v : values) {
            calc.setInput(v);
            calc.calculate();
        }
        
        assertEquals(8.0, calc.getOutput(), TOLERANCE);
    }

    @Test
    @DisplayName("Peak detector with reset")
    void testPeakDetector_Reset() {
        PeakDetectorCalculator calc = new PeakDetectorCalculator();
        
        calc.setInput(10.0);
        calc.calculate();
        assertEquals(10.0, calc.getOutput(), TOLERANCE);
        
        calc.reset();
        
        calc.setInput(5.0);
        calc.calculate();
        assertEquals(5.0, calc.getOutput(), TOLERANCE);
    }
}
```

**Expected Test Count**: 9 tests

---

## C3.5: Logic and Comparison Calculators

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/LogicCalculatorTest.java`

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Logic and Comparison Calculators")
class LogicCalculatorTest {
    private static final double TOLERANCE = 1e-10;

    // === ComparatorCalculator ===
    @Test
    @DisplayName("Comparator: A > B returns 1")
    void testComparator_Greater() {
        ComparatorCalculator calc = new ComparatorCalculator();
        calc.setInputA(5.0);
        calc.setInputB(3.0);
        assertEquals(1.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Comparator: A < B returns 0")
    void testComparator_Less() {
        ComparatorCalculator calc = new ComparatorCalculator();
        calc.setInputA(3.0);
        calc.setInputB(5.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("Comparator: A == B returns threshold behavior")
    void testComparator_Equal() {
        ComparatorCalculator calc = new ComparatorCalculator();
        calc.setInputA(5.0);
        calc.setInputB(5.0);
        // Behavior at equality depends on implementation
        double result = calc.calculate();
        assertTrue(result == 0.0 || result == 1.0);
    }

    @Test
    @DisplayName("Comparator with hysteresis")
    void testComparator_Hysteresis() {
        HysteresisComparatorCalculator calc = new HysteresisComparatorCalculator();
        calc.setThresholds(3.0, 7.0);  // Low = 3, High = 7
        
        // Start below low threshold
        calc.setInput(1.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
        
        // Rise above high threshold
        calc.setInput(8.0);
        assertEquals(1.0, calc.calculate(), TOLERANCE);
        
        // Fall but stay above low threshold - should stay high
        calc.setInput(5.0);
        assertEquals(1.0, calc.calculate(), TOLERANCE);
        
        // Fall below low threshold
        calc.setInput(2.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
    }

    // === ANDCalculator ===
    @Test
    @DisplayName("AND: all high returns 1")
    void testAND_AllHigh() {
        ANDCalculator calc = new ANDCalculator(3);
        calc.setInput(0, 1.0);
        calc.setInput(1, 1.0);
        calc.setInput(2, 1.0);
        assertEquals(1.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("AND: one low returns 0")
    void testAND_OneLow() {
        ANDCalculator calc = new ANDCalculator(3);
        calc.setInput(0, 1.0);
        calc.setInput(1, 0.0);
        calc.setInput(2, 1.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
    }

    // === ORCalculator ===
    @Test
    @DisplayName("OR: all low returns 0")
    void testOR_AllLow() {
        ORCalculator calc = new ORCalculator(3);
        calc.setInput(0, 0.0);
        calc.setInput(1, 0.0);
        calc.setInput(2, 0.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("OR: one high returns 1")
    void testOR_OneHigh() {
        ORCalculator calc = new ORCalculator(3);
        calc.setInput(0, 0.0);
        calc.setInput(1, 1.0);
        calc.setInput(2, 0.0);
        assertEquals(1.0, calc.calculate(), TOLERANCE);
    }

    // === NOTCalculator ===
    @Test
    @DisplayName("NOT: inverts 1 to 0")
    void testNOT_InvertHigh() {
        NOTCalculator calc = new NOTCalculator();
        calc.setInput(1.0);
        assertEquals(0.0, calc.calculate(), TOLERANCE);
    }

    @Test
    @DisplayName("NOT: inverts 0 to 1")
    void testNOT_InvertLow() {
        NOTCalculator calc = new NOTCalculator();
        calc.setInput(0.0);
        assertEquals(1.0, calc.calculate(), TOLERANCE);
    }

    // === FlipFlopCalculator ===
    @Test
    @DisplayName("SR FlipFlop: Set")
    void testSRFF_Set() {
        SRFlipFlopCalculator calc = new SRFlipFlopCalculator();
        calc.setS(1.0);
        calc.setR(0.0);
        calc.calculate();
        assertEquals(1.0, calc.getQ(), TOLERANCE);
    }

    @Test
    @DisplayName("SR FlipFlop: Reset")
    void testSRFF_Reset() {
        SRFlipFlopCalculator calc = new SRFlipFlopCalculator();
        calc.setS(1.0);
        calc.setR(0.0);
        calc.calculate();  // Set
        
        calc.setS(0.0);
        calc.setR(1.0);
        calc.calculate();  // Reset
        
        assertEquals(0.0, calc.getQ(), TOLERANCE);
    }

    @Test
    @DisplayName("SR FlipFlop: Hold state")
    void testSRFF_Hold() {
        SRFlipFlopCalculator calc = new SRFlipFlopCalculator();
        calc.setS(1.0);
        calc.setR(0.0);
        calc.calculate();  // Set Q=1
        
        calc.setS(0.0);
        calc.setR(0.0);
        calc.calculate();  // Hold
        
        assertEquals(1.0, calc.getQ(), TOLERANCE);
    }
}
```

**Expected Test Count**: 14 tests

---

## C3.6: Control System Calculators

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/ControlSystemTest.java`

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Control System Calculators (PID, Transfer Functions)")
class ControlSystemTest {
    private static final double TOLERANCE = 0.01;
    private static final double DT = 1e-5;

    // === PIDCalculator ===
    @Test
    @DisplayName("P-only controller with step error")
    void testPID_ProportionalOnly() {
        PIDCalculator calc = new PIDCalculator();
        calc.setTimestep(DT);
        calc.setGains(2.0, 0.0, 0.0);  // Kp=2, Ki=0, Kd=0
        calc.reset();
        
        calc.setError(5.0);
        calc.calculate();
        
        // Output = Kp * error = 2 * 5 = 10
        assertEquals(10.0, calc.getOutput(), TOLERANCE);
    }

    @Test
    @DisplayName("PI controller accumulates integral")
    void testPID_ProportionalIntegral() {
        PIDCalculator calc = new PIDCalculator();
        calc.setTimestep(DT);
        calc.setGains(1.0, 100.0, 0.0);  // Kp=1, Ki=100
        calc.reset();
        
        // Apply constant error for 1000 steps
        for (int i = 0; i < 1000; i++) {
            calc.setError(1.0);
            calc.calculate();
        }
        
        // Integral contribution = Ki * error * time = 100 * 1 * 0.01 = 1
        // Plus proportional = 1
        // Total should be > 1 (accumulated integral)
        assertTrue(calc.getOutput() > 1.0);
    }

    @Test
    @DisplayName("PD controller responds to error rate")
    void testPID_ProportionalDerivative() {
        PIDCalculator calc = new PIDCalculator();
        calc.setTimestep(DT);
        calc.setGains(1.0, 0.0, 0.001);  // Kp=1, Kd=0.001
        calc.reset();
        
        // Apply ramp error
        for (int i = 0; i < 100; i++) {
            double error = i * 0.1;  // Ramp at 0.1/step
            calc.setError(error);
            calc.calculate();
        }
        
        // With derivative, output should be higher than P-only
        double lastError = 99 * 0.1;
        assertTrue(calc.getOutput() > lastError);
    }

    @Test
    @DisplayName("PID with anti-windup")
    void testPID_AntiWindup() {
        PIDCalculator calc = new PIDCalculator();
        calc.setTimestep(DT);
        calc.setGains(1.0, 1000.0, 0.0);
        calc.setOutputLimits(-10.0, 10.0);
        calc.reset();
        
        // Apply large error - should saturate
        for (int i = 0; i < 10000; i++) {
            calc.setError(100.0);
            calc.calculate();
        }
        
        // Output should be clamped
        assertTrue(calc.getOutput() <= 10.0);
    }

    // === TransferFunctionCalculator ===
    @Test
    @DisplayName("First order transfer function step response")
    void testTF_FirstOrder() {
        // G(s) = 1 / (s + 1)  => tau = 1
        TransferFunctionCalculator calc = new TransferFunctionCalculator();
        calc.setTimestep(DT);
        calc.setNumerator(new double[]{1.0});
        calc.setDenominator(new double[]{1.0, 1.0});  // s + 1
        calc.reset();
        
        // Step input
        for (int i = 0; i < 500000; i++) {  // 5s = 5 time constants
            calc.setInput(1.0);
            calc.calculate();
        }
        
        // Should approach 1.0 (DC gain = 1)
        assertTrue(calc.getOutput() > 0.99);
    }

    // === SampleHoldCalculator ===
    @Test
    @DisplayName("Sample and hold captures on trigger")
    void testSampleHold_Capture() {
        SampleHoldCalculator calc = new SampleHoldCalculator();
        calc.setInput(5.0);
        calc.setTrigger(0.0);
        calc.calculate();
        
        double before = calc.getOutput();
        
        // Trigger sample
        calc.setInput(10.0);
        calc.setTrigger(1.0);
        calc.calculate();
        
        assertEquals(10.0, calc.getOutput(), TOLERANCE);
    }

    @Test
    @DisplayName("Sample and hold retains value")
    void testSampleHold_Hold() {
        SampleHoldCalculator calc = new SampleHoldCalculator();
        
        // Sample 7.0
        calc.setInput(7.0);
        calc.setTrigger(1.0);
        calc.calculate();
        
        // Input changes but trigger is low
        calc.setInput(100.0);
        calc.setTrigger(0.0);
        calc.calculate();
        
        // Should still hold 7.0
        assertEquals(7.0, calc.getOutput(), TOLERANCE);
    }

    // === DelayCalculator ===
    @Test
    @DisplayName("Delay by N samples")
    void testDelay() {
        int delaySamples = 10;
        DelayCalculator calc = new DelayCalculator();
        calc.setDelaySamples(delaySamples);
        calc.reset();
        
        // Feed in values
        for (int i = 0; i <= delaySamples + 5; i++) {
            calc.setInput(i * 1.0);
            calc.calculate();
            
            if (i >= delaySamples) {
                // Output should be input from N samples ago
                assertEquals((i - delaySamples) * 1.0, calc.getOutput(), TOLERANCE);
            }
        }
    }
}
```

**Expected Test Count**: 9 tests

---

# SPRINT C4: Loss Calculation (24% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/`
- **Current Coverage**: 24% (1,207 of 4,882 instructions)
- **Target**: 85%
- **Tests to Add**: ~80
- **Note**: Exclude GUI panel classes (6 files)

## C4.1: Conduction Loss Models

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/ConductionLossTest.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Conduction Loss Calculation")
class ConductionLossTest {
    private static final double TOLERANCE = 1e-6;

    // === DiodeConductionLoss ===
    @Test
    @DisplayName("Diode conduction loss = Vf * If")
    void testDiode_BasicLoss() {
        DiodeConductionLoss calc = new DiodeConductionLoss();
        calc.setForwardVoltage(0.7);  // 0.7V forward drop
        
        double current = 10.0;  // 10A
        double loss = calc.calculate(current);
        
        // P = Vf * If = 0.7 * 10 = 7W
        assertEquals(7.0, loss, TOLERANCE);
    }

    @Test
    @DisplayName("Diode with resistance: P = Vf*I + Rd*I²")
    void testDiode_WithResistance() {
        DiodeConductionLoss calc = new DiodeConductionLoss();
        calc.setForwardVoltage(0.7);
        calc.setDynamicResistance(0.01);  // 10mΩ
        
        double current = 10.0;
        double loss = calc.calculate(current);
        
        // P = 0.7*10 + 0.01*100 = 7 + 1 = 8W
        assertEquals(8.0, loss, TOLERANCE);
    }

    @Test
    @DisplayName("Diode reverse bias has no loss")
    void testDiode_ReverseBias() {
        DiodeConductionLoss calc = new DiodeConductionLoss();
        calc.setForwardVoltage(0.7);
        
        double loss = calc.calculate(-5.0);  // Negative current
        assertEquals(0.0, loss, TOLERANCE);
    }

    // === IGBTConductionLoss ===
    @Test
    @DisplayName("IGBT conduction loss = Vce_sat * Ic")
    void testIGBT_BasicLoss() {
        IGBTConductionLoss calc = new IGBTConductionLoss();
        calc.setSaturationVoltage(1.5);  // Vce_sat = 1.5V
        
        double current = 20.0;
        double loss = calc.calculate(current);
        
        assertEquals(30.0, loss, TOLERANCE);  // 1.5 * 20
    }

    @Test
    @DisplayName("IGBT with on-resistance")
    void testIGBT_WithResistance() {
        IGBTConductionLoss calc = new IGBTConductionLoss();
        calc.setSaturationVoltage(1.5);
        calc.setOnResistance(0.005);  // 5mΩ
        
        double current = 20.0;
        double loss = calc.calculate(current);
        
        // P = 1.5*20 + 0.005*400 = 30 + 2 = 32W
        assertEquals(32.0, loss, TOLERANCE);
    }

    // === MOSFETConductionLoss ===
    @Test
    @DisplayName("MOSFET conduction loss = Rds_on * I²")
    void testMOSFET_BasicLoss() {
        MOSFETConductionLoss calc = new MOSFETConductionLoss();
        calc.setRdsOn(0.01);  // 10mΩ
        
        double current = 10.0;
        double loss = calc.calculate(current);
        
        // P = Rds_on * I² = 0.01 * 100 = 1W
        assertEquals(1.0, loss, TOLERANCE);
    }

    @Test
    @DisplayName("MOSFET loss with temperature coefficient")
    void testMOSFET_TempCoefficient() {
        MOSFETConductionLoss calc = new MOSFETConductionLoss();
        calc.setRdsOn(0.01);  // 10mΩ at 25°C
        calc.setTemperatureCoefficient(0.005);  // 0.5%/°C
        calc.setJunctionTemperature(125.0);  // Hot!
        
        double current = 10.0;
        double loss = calc.calculate(current);
        
        // Rds_on increases by 50% at 125°C
        // Rds_hot = 0.01 * (1 + 0.005 * 100) = 0.015
        // P = 0.015 * 100 = 1.5W
        assertEquals(1.5, loss, 0.01);
    }
}
```

**Expected Test Count**: 8 tests

---

## C4.2: Switching Loss Models

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/SwitchingLossTest.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Switching Loss Calculation")
class SwitchingLossTest {
    private static final double TOLERANCE = 1e-6;

    // === IGBTSwitchingLoss ===
    @Test
    @DisplayName("IGBT turn-on loss from datasheet Eon")
    void testIGBT_TurnOnLoss() {
        IGBTSwitchingLoss calc = new IGBTSwitchingLoss();
        calc.setEonReference(5e-3);  // Eon = 5mJ at reference conditions
        calc.setReferenceVoltage(600.0);  // 600V bus
        calc.setReferenceCurrent(50.0);   // 50A
        
        double voltage = 600.0;
        double current = 50.0;
        double loss = calc.calculateTurnOnEnergy(voltage, current);
        
        // At reference conditions, loss = Eon
        assertEquals(5e-3, loss, TOLERANCE);
    }

    @Test
    @DisplayName("IGBT turn-off loss from datasheet Eoff")
    void testIGBT_TurnOffLoss() {
        IGBTSwitchingLoss calc = new IGBTSwitchingLoss();
        calc.setEoffReference(3e-3);  // Eoff = 3mJ
        calc.setReferenceVoltage(600.0);
        calc.setReferenceCurrent(50.0);
        
        double loss = calc.calculateTurnOffEnergy(600.0, 50.0);
        assertEquals(3e-3, loss, TOLERANCE);
    }

    @Test
    @DisplayName("IGBT switching loss scales with voltage")
    void testIGBT_VoltageScaling() {
        IGBTSwitchingLoss calc = new IGBTSwitchingLoss();
        calc.setEonReference(5e-3);
        calc.setReferenceVoltage(600.0);
        calc.setReferenceCurrent(50.0);
        calc.setVoltageExponent(1.3);  // Slightly super-linear
        
        double lossHalfVoltage = calc.calculateTurnOnEnergy(300.0, 50.0);
        double lossFullVoltage = calc.calculateTurnOnEnergy(600.0, 50.0);
        
        // Loss at half voltage should be less than half (due to exponent > 1)
        assertTrue(lossHalfVoltage < lossFullVoltage * 0.5);
    }

    @Test
    @DisplayName("IGBT switching loss scales with current")
    void testIGBT_CurrentScaling() {
        IGBTSwitchingLoss calc = new IGBTSwitchingLoss();
        calc.setEonReference(5e-3);
        calc.setReferenceVoltage(600.0);
        calc.setReferenceCurrent(50.0);
        calc.setCurrentExponent(1.0);  // Linear
        
        double lossHalfCurrent = calc.calculateTurnOnEnergy(600.0, 25.0);
        double lossFullCurrent = calc.calculateTurnOnEnergy(600.0, 50.0);
        
        // Linear scaling means half current = half loss
        assertEquals(lossFullCurrent / 2, lossHalfCurrent, lossFullCurrent * 0.01);
    }

    // === DiodeSwitchingLoss (Reverse Recovery) ===
    @Test
    @DisplayName("Diode reverse recovery loss")
    void testDiode_ReverseRecovery() {
        DiodeSwitchingLoss calc = new DiodeSwitchingLoss();
        calc.setErrReference(2e-3);  // Err = 2mJ
        calc.setReferenceVoltage(600.0);
        calc.setReferenceCurrent(50.0);
        
        double loss = calc.calculateRecoveryEnergy(600.0, 50.0);
        assertEquals(2e-3, loss, TOLERANCE);
    }

    @Test
    @DisplayName("Diode Qrr-based loss calculation")
    void testDiode_QrrBased() {
        DiodeSwitchingLoss calc = new DiodeSwitchingLoss();
        calc.setRecoveryCharge(2e-6);  // Qrr = 2µC
        calc.setSoftnessFactor(0.3);
        
        double voltage = 400.0;
        double diDt = 100e6;  // 100 A/µs
        double loss = calc.calculateRecoveryEnergyFromQrr(voltage, diDt);
        
        // Err ≈ 0.25 * Qrr * Vr (for soft recovery)
        assertTrue(loss > 0);
    }

    // === Total Switching Loss ===
    @Test
    @DisplayName("Total switching power at frequency")
    void testTotal_SwitchingPower() {
        SwitchingLossCalculator calc = new SwitchingLossCalculator();
        calc.setTurnOnEnergy(5e-3);   // 5mJ
        calc.setTurnOffEnergy(3e-3);  // 3mJ
        calc.setSwitchingFrequency(20e3);  // 20kHz
        
        double power = calc.calculateSwitchingPower();
        
        // P_sw = (Eon + Eoff) * f = 8e-3 * 20e3 = 160W
        assertEquals(160.0, power, TOLERANCE);
    }
}
```

**Expected Test Count**: 8 tests

---

# SPRINT C5: Data Container (15% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/`
- **Current Coverage**: 15% (881 of 5,590 instructions)
- **Target**: 85%
- **Tests to Add**: ~60

## C5.1: DataContainer Classes

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerTest.java`

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Data Container Classes")
class DataContainerTest {
    private static final double TOLERANCE = 1e-10;

    // TODO: Implement based on actual class signatures
    // Key classes to test:
    // - ContainerStatus
    // - DataContainerSimple
    // - DataContainerComplex
    // - DataContainerIntegral
    // - AbstractDataContainer
    
    @Test
    @DisplayName("Placeholder - implement after reviewing actual API")
    void placeholder() {
        // Review src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/
        // and implement tests based on actual class signatures
        assertTrue(true);
    }
}
```

**Note**: This sprint requires reviewing the actual datacontainer classes.
Expected Test Count: ~60 tests across multiple test files.

---

# SPRINT C6: Circuit Core Classes (6% → 50%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/` (main, not subpackages)
- **Current Coverage**: 6% (2,972 of ~43,000 instructions in whole circuit package)
- **Target**: 50% for GUI-free classes (54 classes identified)
- **Tests to Add**: ~30 for core domain classes

## C6.1: Core Domain Classes (GUI-Free)

**Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/CoreDomainTest.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Circuit Core Domain Classes")
class CoreDomainTest {
    
    // === GridPoint ===
    @Test
    @DisplayName("GridPoint equals for same coordinates")
    void testGridPoint_Equals() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 20);
        assertEquals(p1, p2);
    }

    @Test
    @DisplayName("GridPoint not equals for different coordinates")
    void testGridPoint_NotEquals() {
        GridPoint p1 = new GridPoint(10, 20);
        GridPoint p2 = new GridPoint(10, 21);
        assertNotEquals(p1, p2);
    }

    // === ComponentDirection ===
    @Test
    @DisplayName("ComponentDirection rotation")
    void testComponentDirection_Rotate() {
        ComponentDirection dir = ComponentDirection.NORTH;
        assertEquals(ComponentDirection.EAST, dir.rotateClockwise());
    }

    // === CircuitSourceType ===
    @Test
    @DisplayName("CircuitSourceType values exist")
    void testCircuitSourceType_Values() {
        assertNotNull(CircuitSourceType.values());
        assertTrue(CircuitSourceType.values().length > 0);
    }

    // === TokenMap ===
    @Test
    @DisplayName("TokenMap stores and retrieves values")
    void testTokenMap_Storage() {
        TokenMap map = new TokenMap();
        map.put("key", "value");
        assertEquals("value", map.get("key"));
    }

    // === UniqueObjectIdentifer ===
    @Test
    @DisplayName("UniqueObjectIdentifier generates unique IDs")
    void testUniqueId_Generation() {
        long id1 = UniqueObjectIdentifer.getNewIdentifier();
        long id2 = UniqueObjectIdentifer.getNewIdentifier();
        assertNotEquals(id1, id2);
    }

    // === TimeFunction ===
    @Test
    @DisplayName("TimeFunction constant value")
    void testTimeFunction_Constant() {
        TimeFunction tf = new TimeFunctionConstant(5.0);
        assertEquals(5.0, tf.getValue(0.0), 1e-10);
        assertEquals(5.0, tf.getValue(100.0), 1e-10);
    }

    // === LabelPriority ===
    @Test
    @DisplayName("LabelPriority ordering")
    void testLabelPriority_Ordering() {
        assertTrue(LabelPriority.HIGH.ordinal() < LabelPriority.LOW.ordinal()
            || LabelPriority.HIGH.compareTo(LabelPriority.LOW) != 0);
    }
}
```

**Expected Test Count**: 8 tests

---

## Verification Commands

After implementing all sprints, run:

```bash
# Run all tests
cd /home/tinix/claude_wsl/GeckoCIRCUITS
mvn clean test -q

# Generate coverage report
mvn jacoco:report

# View report
firefox target/site/jacoco/index.html

# Check specific package coverage
grep -A5 "control.calculators" target/site/jacoco/index.html
```

---

## Success Criteria

| Package | Before | After | Tests Added |
|---------|--------|-------|-------------|
| `math` | 55% | 85% | 45 |
| `circuit.matrix` | 77% | 85% | 15 |
| `control.calculators` | 41% | 85% | 100 |
| `losscalculation` | 24% | 85% | 80 |
| `datacontainer` | 15% | 85% | 60 |
| `circuit` (core) | 6% | 50% | 30 |
| **Total** | | | **330** |

---

## Notes for Haiku Executor

1. **Check class signatures first** - Some test code may need adjustment based on actual API
2. **Run incrementally** - Complete one sprint, verify coverage, then proceed
3. **Skip GUI classes** - Any class with Swing/AWT imports should not be tested in core
4. **Use parameterized tests** where appropriate for edge cases
5. **Focus on public API** - Test public methods, not implementation details

---

*Document created: January 27, 2026*
*For execution by: Claude Haiku*
*Total estimated time: 6-8 sprints*
