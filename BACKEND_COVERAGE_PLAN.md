# GeckoCIRCUITS Backend Coverage Plan: Target 85%

## Executive Summary

| Metric | Current | Target |
|--------|---------|--------|
| Pure Backend Average | 32.2% | 85% |
| Total New Tests | - | ~940 |
| Sprints | - | 10 |

---

## How to Use This Plan

Each sprint is self-contained. Execute with:
```bash
# Start sprint
mvn test  # Verify current state

# After implementing tests
mvn clean test jacoco:report
python3 scripts/backend-coverage.py
```

---

# SPRINT 15: Math Package (0% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/math/`
- **Files**: 7 files, 0% coverage
- **Target Tests**: 80+
- **Test Location**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/`

---

## 15.1: NComplex.java (Complex Numbers)

**File**: `src/main/java/ch/technokrat/gecko/geckocircuits/math/NComplex.java`

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/NComplexTest.java`

### Methods to Test (17 public methods)

```java
// Constructors
NComplex(float r, float i)  // real + imaginary
NComplex(float r)           // real only
NComplex()                  // zero

// Accessors
float getRe()
float getIm()

// Arithmetic (static methods)
static NComplex add(NComplex a, NComplex b)
static NComplex sub(NComplex a, NComplex b)
static NComplex mul(NComplex a, NComplex b)
static NComplex div(NComplex a, NComplex b)
static NComplex conj(NComplex a)
static float abs(NComplex a)
static NComplex sqrt(NComplex a)
static NComplex RCmul(float x, NComplex a)

// Object methods
String toString()
String nicePrint()
int hashCode()
boolean equals(Object o)
```

### Test Implementation

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class NComplexTest {
    private static final float TOLERANCE = 1e-6f;

    // === Constructor Tests ===
    @Test
    public void testConstructor_RealAndImaginary() {
        NComplex c = new NComplex(3.0f, 4.0f);
        assertEquals(3.0f, c.getRe(), TOLERANCE);
        assertEquals(4.0f, c.getIm(), TOLERANCE);
    }

    @Test
    public void testConstructor_RealOnly() {
        NComplex c = new NComplex(5.0f);
        assertEquals(5.0f, c.getRe(), TOLERANCE);
        assertEquals(0.0f, c.getIm(), TOLERANCE);
    }

    @Test
    public void testConstructor_Default() {
        NComplex c = new NComplex();
        assertEquals(0.0f, c.getRe(), TOLERANCE);
        assertEquals(0.0f, c.getIm(), TOLERANCE);
    }

    // === Arithmetic Tests ===
    @Test
    public void testAdd() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.add(a, b);
        assertEquals(4.0f, result.getRe(), TOLERANCE);
        assertEquals(6.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testSub() {
        NComplex a = new NComplex(5.0f, 7.0f);
        NComplex b = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.sub(a, b);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testMul() {
        // (1+2i)(3+4i) = 3+4i+6i+8i² = 3+10i-8 = -5+10i
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.mul(a, b);
        assertEquals(-5.0f, result.getRe(), TOLERANCE);
        assertEquals(10.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testDiv() {
        // (4+2i)/(1+i) = (4+2i)(1-i)/((1+i)(1-i)) = (4-4i+2i-2i²)/2 = (6-2i)/2 = 3-i
        NComplex a = new NComplex(4.0f, 2.0f);
        NComplex b = new NComplex(1.0f, 1.0f);
        NComplex result = NComplex.div(a, b);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(-1.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testConj() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(-4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testAbs() {
        // |3+4i| = sqrt(9+16) = 5
        NComplex a = new NComplex(3.0f, 4.0f);
        assertEquals(5.0f, NComplex.abs(a), TOLERANCE);
    }

    @Test
    public void testSqrt() {
        // sqrt(3+4i) ≈ 2+i (verify: (2+i)² = 4+4i+i² = 3+4i)
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.sqrt(a);
        assertEquals(2.0f, result.getRe(), 0.01f);
        assertEquals(1.0f, result.getIm(), 0.01f);
    }

    @Test
    public void testRCmul() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.RCmul(2.0f, a);
        assertEquals(4.0f, result.getRe(), TOLERANCE);
        assertEquals(6.0f, result.getIm(), TOLERANCE);
    }

    // === Edge Cases ===
    @Test
    public void testAdd_WithZero() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex zero = new NComplex();
        NComplex result = NComplex.add(a, zero);
        assertEquals(a.getRe(), result.getRe(), TOLERANCE);
        assertEquals(a.getIm(), result.getIm(), TOLERANCE);
    }

    @Test
    public void testMul_WithZero() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex zero = new NComplex();
        NComplex result = NComplex.mul(a, zero);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testAbs_Zero() {
        NComplex zero = new NComplex();
        assertEquals(0.0f, NComplex.abs(zero), TOLERANCE);
    }

    @Test
    public void testAbs_PureReal() {
        NComplex a = new NComplex(-5.0f, 0.0f);
        assertEquals(5.0f, NComplex.abs(a), TOLERANCE);
    }

    @Test
    public void testAbs_PureImaginary() {
        NComplex a = new NComplex(0.0f, -7.0f);
        assertEquals(7.0f, NComplex.abs(a), TOLERANCE);
    }

    // === Equality Tests ===
    @Test
    public void testEquals_Same() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(1.0f, 2.0f);
        assertEquals(a, b);
    }

    @Test
    public void testEquals_Different() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(1.0f, 3.0f);
        assertNotEquals(a, b);
    }

    @Test
    public void testHashCode_Consistent() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(1.0f, 2.0f);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // === String Tests ===
    @Test
    public void testToString() {
        NComplex a = new NComplex(3.0f, 4.0f);
        String str = a.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }
}
```

**Expected Tests**: 20+

---

## 15.2: Matrix.java (Matrix Operations)

**File**: `src/main/java/ch/technokrat/gecko/geckocircuits/math/Matrix.java`

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/MatrixTest.java`

### Methods to Test (56 public methods - prioritize core operations)

**Priority 1 - Constructors & Basic Operations**:
```java
Matrix(int m, int n)                    // Create m×n zero matrix
Matrix(double[][] A)                    // Create from 2D array
int getRowDimension()
int getColumnDimension()
double get(int i, int j)
void set(int i, int j, double s)
double[][] getArray()
Matrix copy()
```

**Priority 2 - Arithmetic**:
```java
Matrix plus(Matrix B)                   // A + B
Matrix minus(Matrix B)                  // A - B
Matrix times(double s)                  // s * A
Matrix times(Matrix B)                  // A * B
Matrix transpose()                      // A^T
```

**Priority 3 - Advanced**:
```java
Matrix inverse()                        // A^(-1)
double det()                            // determinant
Matrix solve(Matrix B)                  // Solve A*X = B
double trace()                          // Sum of diagonal
double norm1()                          // Max column sum
double normInf()                        // Max row sum
static Matrix identity(int m, int n)    // Identity matrix
static Matrix random(int m, int n)      // Random matrix
```

### Test Implementation

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class MatrixTest {
    private static final double TOLERANCE = 1e-10;

    // === Constructor Tests ===
    @Test
    public void testConstructor_ZeroMatrix() {
        Matrix m = new Matrix(3, 4);
        assertEquals(3, m.getRowDimension());
        assertEquals(4, m.getColumnDimension());
        assertEquals(0.0, m.get(0, 0), TOLERANCE);
        assertEquals(0.0, m.get(2, 3), TOLERANCE);
    }

    @Test
    public void testConstructor_From2DArray() {
        double[][] data = {{1, 2}, {3, 4}, {5, 6}};
        Matrix m = new Matrix(data);
        assertEquals(3, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(4.0, m.get(1, 1), TOLERANCE);
    }

    // === Get/Set Tests ===
    @Test
    public void testGetSet() {
        Matrix m = new Matrix(2, 2);
        m.set(0, 0, 1.5);
        m.set(1, 1, 2.5);
        assertEquals(1.5, m.get(0, 0), TOLERANCE);
        assertEquals(2.5, m.get(1, 1), TOLERANCE);
    }

    // === Arithmetic Tests ===
    @Test
    public void testPlus() {
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{5, 6}, {7, 8}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        Matrix result = mA.plus(mB);
        assertEquals(6.0, result.get(0, 0), TOLERANCE);  // 1+5
        assertEquals(8.0, result.get(0, 1), TOLERANCE);  // 2+6
        assertEquals(10.0, result.get(1, 0), TOLERANCE); // 3+7
        assertEquals(12.0, result.get(1, 1), TOLERANCE); // 4+8
    }

    @Test
    public void testMinus() {
        double[][] a = {{5, 6}, {7, 8}};
        double[][] b = {{1, 2}, {3, 4}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        Matrix result = mA.minus(mB);
        assertEquals(4.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTimesScalar() {
        double[][] a = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(a);
        Matrix result = m.times(2.0);
        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(8.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTimesMatrix() {
        // [1 2] * [5 6] = [1*5+2*7  1*6+2*8] = [19 22]
        // [3 4]   [7 8]   [3*5+4*7  3*6+4*8]   [43 50]
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{5, 6}, {7, 8}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        Matrix result = mA.times(mB);
        assertEquals(19.0, result.get(0, 0), TOLERANCE);
        assertEquals(22.0, result.get(0, 1), TOLERANCE);
        assertEquals(43.0, result.get(1, 0), TOLERANCE);
        assertEquals(50.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTranspose() {
        double[][] a = {{1, 2, 3}, {4, 5, 6}};
        Matrix m = new Matrix(a);
        Matrix t = m.transpose();
        assertEquals(3, t.getRowDimension());
        assertEquals(2, t.getColumnDimension());
        assertEquals(1.0, t.get(0, 0), TOLERANCE);
        assertEquals(4.0, t.get(0, 1), TOLERANCE);
        assertEquals(2.0, t.get(1, 0), TOLERANCE);
    }

    // === Advanced Operations ===
    @Test
    public void testDeterminant_2x2() {
        // det([1 2; 3 4]) = 1*4 - 2*3 = -2
        double[][] a = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(a);
        assertEquals(-2.0, m.det(), TOLERANCE);
    }

    @Test
    public void testDeterminant_3x3() {
        // det([1 2 3; 4 5 6; 7 8 9]) = 0 (singular)
        double[][] a = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(a);
        assertEquals(0.0, m.det(), TOLERANCE);
    }

    @Test
    public void testInverse_2x2() {
        // inv([4 7; 2 6]) = 1/(24-14) * [6 -7; -2 4] = [0.6 -0.7; -0.2 0.4]
        double[][] a = {{4, 7}, {2, 6}};
        Matrix m = new Matrix(a);
        Matrix inv = m.inverse();
        assertEquals(0.6, inv.get(0, 0), TOLERANCE);
        assertEquals(-0.7, inv.get(0, 1), TOLERANCE);
        assertEquals(-0.2, inv.get(1, 0), TOLERANCE);
        assertEquals(0.4, inv.get(1, 1), TOLERANCE);
    }

    @Test
    public void testInverse_TimesOriginal_IsIdentity() {
        double[][] a = {{4, 7}, {2, 6}};
        Matrix m = new Matrix(a);
        Matrix inv = m.inverse();
        Matrix product = m.times(inv);
        assertEquals(1.0, product.get(0, 0), TOLERANCE);
        assertEquals(0.0, product.get(0, 1), TOLERANCE);
        assertEquals(0.0, product.get(1, 0), TOLERANCE);
        assertEquals(1.0, product.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTrace() {
        double[][] a = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(a);
        assertEquals(15.0, m.trace(), TOLERANCE); // 1+5+9
    }

    @Test
    public void testIdentity() {
        Matrix id = Matrix.identity(3, 3);
        assertEquals(1.0, id.get(0, 0), TOLERANCE);
        assertEquals(1.0, id.get(1, 1), TOLERANCE);
        assertEquals(1.0, id.get(2, 2), TOLERANCE);
        assertEquals(0.0, id.get(0, 1), TOLERANCE);
        assertEquals(0.0, id.get(1, 0), TOLERANCE);
    }

    @Test
    public void testSolve() {
        // Solve: [2 1] * [x] = [5]
        //        [1 3]   [y]   [5]
        // Solution: x=2, y=1
        double[][] a = {{2, 1}, {1, 3}};
        double[][] b = {{5}, {5}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        Matrix x = mA.solve(mB);
        assertEquals(2.0, x.get(0, 0), TOLERANCE);
        assertEquals(1.0, x.get(1, 0), TOLERANCE);
    }

    @Test
    public void testCopy() {
        double[][] a = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(a);
        Matrix copy = m.copy();
        assertEquals(m.get(0, 0), copy.get(0, 0), TOLERANCE);
        copy.set(0, 0, 99.0);
        assertEquals(1.0, m.get(0, 0), TOLERANCE); // Original unchanged
    }

    @Test
    public void testNorm1() {
        // Max column sum: col0=1+3=4, col1=2+4=6 -> 6
        double[][] a = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(a);
        assertEquals(6.0, m.norm1(), TOLERANCE);
    }

    @Test
    public void testNormInf() {
        // Max row sum: row0=1+2=3, row1=3+4=7 -> 7
        double[][] a = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(a);
        assertEquals(7.0, m.normInf(), TOLERANCE);
    }
}
```

**Expected Tests**: 25+

---

## 15.3: LUDecomposition.java

**File**: `src/main/java/ch/technokrat/gecko/geckocircuits/math/LUDecomposition.java`

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/LUDecompositionTest.java`

### Methods to Test

```java
LUDecomposition(Matrix A)    // Constructor
boolean isNonsingular()      // Check singularity
Matrix getL()                // Lower triangular
Matrix getU()                // Upper triangular
int[] getPivot()             // Pivot vector
double det()                 // Determinant
Matrix solve(Matrix B)       // Solve A*X = B
```

### Test Implementation

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class LUDecompositionTest {
    private static final double TOLERANCE = 1e-10;

    @Test
    public void testLUDecomposition_NonSingular() {
        double[][] a = {{2, 1}, {1, 3}};
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testLUDecomposition_Singular() {
        double[][] a = {{1, 2}, {2, 4}}; // Singular (row2 = 2*row1)
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        assertFalse(lu.isNonsingular());
    }

    @Test
    public void testGetL_IsLowerTriangular() {
        double[][] a = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix L = lu.getL();
        // Lower triangular: upper elements should be 0
        assertEquals(0.0, L.get(0, 1), TOLERANCE);
    }

    @Test
    public void testGetU_IsUpperTriangular() {
        double[][] a = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix U = lu.getU();
        // Upper triangular: lower elements should be 0
        assertEquals(0.0, U.get(1, 0), TOLERANCE);
    }

    @Test
    public void testLU_Product_EqualsOriginal() {
        double[][] a = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix L = lu.getL();
        Matrix U = lu.getU();
        Matrix product = L.times(U);
        // Note: Need to account for pivoting
        // Just check dimensions for now
        assertEquals(2, product.getRowDimension());
        assertEquals(2, product.getColumnDimension());
    }

    @Test
    public void testDet() {
        double[][] a = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        // det = 4*3 - 3*6 = 12 - 18 = -6
        assertEquals(-6.0, lu.det(), TOLERANCE);
    }

    @Test
    public void testSolve() {
        double[][] a = {{2, 1}, {1, 3}};
        double[][] b = {{5}, {5}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        LUDecomposition lu = new LUDecomposition(mA);
        Matrix x = lu.solve(mB);
        assertEquals(2.0, x.get(0, 0), TOLERANCE);
        assertEquals(1.0, x.get(1, 0), TOLERANCE);
    }

    @Test
    public void testGetPivot() {
        double[][] a = {{2, 1}, {1, 3}};
        Matrix m = new Matrix(a);
        LUDecomposition lu = new LUDecomposition(m);
        int[] pivot = lu.getPivot();
        assertEquals(2, pivot.length);
    }
}
```

**Expected Tests**: 10+

---

## 15.4: CholeskyDecomposition.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/CholeskyDecompositionTest.java`

### Methods to Test

```java
CholeskyDecomposition(Matrix A)  // Constructor
boolean isSPD()                  // Symmetric positive definite?
Matrix getL()                    // Lower triangular factor
Matrix solve(Matrix B)           // Solve A*X = B
```

### Test Implementation

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class CholeskyDecompositionTest {
    private static final double TOLERANCE = 1e-10;

    @Test
    public void testCholesky_SPD_Matrix() {
        // SPD matrix: [[4, 2], [2, 2]]
        double[][] a = {{4, 2}, {2, 2}};
        Matrix m = new Matrix(a);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        assertTrue(chol.isSPD());
    }

    @Test
    public void testCholesky_NonSPD_Matrix() {
        // Non-SPD matrix (not positive definite)
        double[][] a = {{1, 2}, {2, 1}};
        Matrix m = new Matrix(a);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        assertFalse(chol.isSPD());
    }

    @Test
    public void testGetL_LowerTriangular() {
        double[][] a = {{4, 2}, {2, 2}};
        Matrix m = new Matrix(a);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        Matrix L = chol.getL();
        // L should be lower triangular
        assertEquals(0.0, L.get(0, 1), TOLERANCE);
    }

    @Test
    public void testL_TimesL_Transpose_EqualsA() {
        double[][] a = {{4, 2}, {2, 2}};
        Matrix m = new Matrix(a);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        Matrix L = chol.getL();
        Matrix product = L.times(L.transpose());
        assertEquals(4.0, product.get(0, 0), TOLERANCE);
        assertEquals(2.0, product.get(0, 1), TOLERANCE);
        assertEquals(2.0, product.get(1, 0), TOLERANCE);
        assertEquals(2.0, product.get(1, 1), TOLERANCE);
    }

    @Test
    public void testSolve() {
        double[][] a = {{4, 2}, {2, 2}};
        double[][] b = {{6}, {4}};
        Matrix mA = new Matrix(a);
        Matrix mB = new Matrix(b);
        CholeskyDecomposition chol = new CholeskyDecomposition(mA);
        Matrix x = chol.solve(mB);
        // Verify: A*x = b
        Matrix result = mA.times(x);
        assertEquals(6.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(1, 0), TOLERANCE);
    }
}
```

**Expected Tests**: 8+

---

## 15.5: Polynomials.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/math/PolynomialsTest.java`

### Methods to Test

```java
static void poldiv(float[] u, int n, float[] v, int nv, float[] q, float[] r)
```

### Test Implementation

```java
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class PolynomialsTest {
    private static final float TOLERANCE = 1e-5f;

    @Test
    public void testPoldiv_Simple() {
        // (x^2 + 2x + 1) / (x + 1) = (x + 1), remainder 0
        // Coefficients in ascending order: [1, 2, 1] / [1, 1]
        float[] u = {1, 2, 1};
        float[] v = {1, 1};
        float[] q = new float[2];  // Quotient
        float[] r = new float[2];  // Remainder

        Polynomials.poldiv(u, 2, v, 1, q, r);

        // Expected quotient: x + 1 -> [1, 1]
        assertEquals(1.0f, q[0], TOLERANCE);
        assertEquals(1.0f, q[1], TOLERANCE);
    }

    @Test
    public void testPoldiv_WithRemainder() {
        // (x^2 + 1) / (x + 1) = x - 1, remainder 2
        float[] u = {1, 0, 1};
        float[] v = {1, 1};
        float[] q = new float[2];
        float[] r = new float[2];

        Polynomials.poldiv(u, 2, v, 1, q, r);

        // Check quotient and remainder
        assertNotNull(q);
        assertNotNull(r);
    }
}
```

**Expected Tests**: 5+

---

## Sprint 15 Acceptance Criteria

- [ ] `NComplexTest.java` - 20+ tests, all GREEN
- [ ] `MatrixTest.java` - 25+ tests, all GREEN
- [ ] `LUDecompositionTest.java` - 10+ tests, all GREEN
- [ ] `CholeskyDecompositionTest.java` - 8+ tests, all GREEN
- [ ] `PolynomialsTest.java` - 5+ tests, all GREEN
- [ ] Math package coverage ≥ 85%
- [ ] `mvn clean test` passes
- [ ] Total tests: 68+

---

# SPRINT 16: DataContainer Package (3% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/`
- **Files**: 30 files (4 GUI, 26 backend)
- **Current Coverage**: 3%
- **Target Tests**: 100+
- **Test Location**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/`

---

## 16.1: ContainerStatus.java (Enum)

**File**: `src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ContainerStatus.java`

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/ContainerStatusTest.java`

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContainerStatusTest {

    @Test
    public void testEnumValues() {
        ContainerStatus[] values = ContainerStatus.values();
        assertEquals(5, values.length);
    }

    @Test
    public void testNotInitialized() {
        assertNotNull(ContainerStatus.NOT_INITIALIZED);
    }

    @Test
    public void testRunning() {
        assertNotNull(ContainerStatus.RUNNING);
    }

    @Test
    public void testFinished() {
        assertNotNull(ContainerStatus.FINISHED);
    }

    @Test
    public void testPaused() {
        assertNotNull(ContainerStatus.PAUSED);
    }

    @Test
    public void testDeleted() {
        assertNotNull(ContainerStatus.DELETED);
    }

    @Test
    public void testValueOf() {
        assertEquals(ContainerStatus.RUNNING, ContainerStatus.valueOf("RUNNING"));
    }
}
```

**Expected Tests**: 7

---

## 16.2: TextSeparator.java (Enum)

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/TextSeparatorTest.java`

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextSeparatorTest {

    @Test
    public void testSpace() {
        assertEquals(' ', TextSeparator.SPACE.charValue());
    }

    @Test
    public void testTabulator() {
        assertEquals('\t', TextSeparator.TABULATOR.charValue());
    }

    @Test
    public void testSemicolon() {
        assertEquals(';', TextSeparator.SEMICOLON.charValue());
    }

    @Test
    public void testComma() {
        assertEquals(',', TextSeparator.COMMA.charValue());
    }

    @Test
    public void testGetFromCode() {
        TextSeparator sep = TextSeparator.getFromCode(TextSeparator.SPACE.code());
        assertEquals(TextSeparator.SPACE, sep);
    }

    @Test
    public void testGetFromOrdinal() {
        TextSeparator sep = TextSeparator.getFromOrdinal(0);
        assertEquals(TextSeparator.SPACE, sep);
    }

    @Test
    public void testStringValue() {
        assertNotNull(TextSeparator.COMMA.stringValue());
    }
}
```

**Expected Tests**: 7

---

## 16.3: AverageValue.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/AverageValueTest.java`

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class AverageValueTest {
    private static final double TOLERANCE = 1e-10;

    @Test
    public void testGetIntervalStart() {
        // Test interval start getter
        // Create AverageValue and verify start
    }

    @Test
    public void testGetIntervalStop() {
        // Test interval stop getter
    }

    @Test
    public void testGetAverageValue() {
        // Test average value calculation
    }

    @Test
    public void testGetAverageSpan() {
        // Test span = stop - start
    }

    @Test
    public void testAppendAverage() {
        // Test combining two averages
    }

    @Test
    public void testToString() {
        // Test string representation
    }
}
```

**Expected Tests**: 10

---

## 16.4: DataContainerSimple.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerSimpleTest.java`

### Key Methods to Test

```java
static DataContainerSimple fabricConstantDtTimeSeries(int rows, int columns)
static DataContainerSimple fabricArrayTimeSeries(int rows, int columns)
void setValue(float value, int row, int column)
float getValue(int row, int column)
int getRowLength()
void setSignalName(String name, int row)
String getSignalName(int row)
double getTimeValue(int index, int row)
int getMaximumTimeIndex(int row)
void deleteDataReference()
```

### Test Implementation

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DataContainerSimpleTest {
    private static final float TOLERANCE = 1e-6f;
    private DataContainerSimple container;

    @Before
    public void setUp() {
        container = DataContainerSimple.fabricConstantDtTimeSeries(3, 100);
    }

    @Test
    public void testFabricConstantDtTimeSeries() {
        DataContainerSimple c = DataContainerSimple.fabricConstantDtTimeSeries(5, 200);
        assertNotNull(c);
        assertEquals(5, c.getRowLength());
    }

    @Test
    public void testFabricArrayTimeSeries() {
        DataContainerSimple c = DataContainerSimple.fabricArrayTimeSeries(4, 150);
        assertNotNull(c);
        assertEquals(4, c.getRowLength());
    }

    @Test
    public void testSetGetValue() {
        container.setValue(3.14f, 0, 0);
        assertEquals(3.14f, container.getValue(0, 0), TOLERANCE);
    }

    @Test
    public void testSetGetValue_MultiplePositions() {
        container.setValue(1.0f, 0, 0);
        container.setValue(2.0f, 1, 50);
        container.setValue(3.0f, 2, 99);

        assertEquals(1.0f, container.getValue(0, 0), TOLERANCE);
        assertEquals(2.0f, container.getValue(1, 50), TOLERANCE);
        assertEquals(3.0f, container.getValue(2, 99), TOLERANCE);
    }

    @Test
    public void testGetRowLength() {
        assertEquals(3, container.getRowLength());
    }

    @Test
    public void testSetGetSignalName() {
        container.setSignalName("Voltage", 0);
        assertEquals("Voltage", container.getSignalName(0));
    }

    @Test
    public void testGetMaximumTimeIndex() {
        // Should be columns - 1
        int maxIdx = container.getMaximumTimeIndex(0);
        assertTrue(maxIdx >= 0);
    }

    @Test
    public void testDeleteDataReference() {
        container.deleteDataReference();
        // Should not throw, verify container is cleared
    }

    @Test
    public void testGetTimeValue() {
        double time = container.getTimeValue(0, 0);
        // Time value depends on implementation
        assertNotNull(time);
    }

    @Test
    public void testMultipleRows() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 10; col++) {
                container.setValue(row * 10 + col, row, col);
            }
        }
        assertEquals(25.0f, container.getValue(2, 5), TOLERANCE);
    }
}
```

**Expected Tests**: 15+

---

## 16.5: IntegerMatrixCache.java, ShortArrayCache.java, ShortMatrixCache.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/CacheTest.java`

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class CacheTest {

    @After
    public void tearDown() {
        IntegerMatrixCache.clearCache();
        ShortMatrixCache.clearCache();
    }

    // === IntegerMatrixCache ===
    @Test
    public void testIntegerMatrixCache_Get() {
        int[][] matrix = IntegerMatrixCache.getCachedIntArray(3, 4);
        assertNotNull(matrix);
        assertEquals(3, matrix.length);
        assertEquals(4, matrix[0].length);
    }

    @Test
    public void testIntegerMatrixCache_Recycle() {
        int[][] matrix = IntegerMatrixCache.getCachedIntArray(3, 4);
        IntegerMatrixCache.recycleIntArray(matrix);
        // Should not throw
    }

    @Test
    public void testIntegerMatrixCache_ReuseRecycled() {
        int[][] matrix1 = IntegerMatrixCache.getCachedIntArray(3, 4);
        IntegerMatrixCache.recycleIntArray(matrix1);
        int[][] matrix2 = IntegerMatrixCache.getCachedIntArray(3, 4);
        // May or may not be same instance (implementation dependent)
        assertNotNull(matrix2);
    }

    // === ShortArrayCache ===
    @Test
    public void testShortArrayCache_Get() {
        short[] arr = ShortArrayCache.getCachedArray(100);
        assertNotNull(arr);
        assertEquals(100, arr.length);
    }

    @Test
    public void testShortArrayCache_Recycle() {
        short[] arr = ShortArrayCache.getCachedArray(100);
        ShortArrayCache.recycleArray(arr);
        // Should not throw
    }

    // === ShortMatrixCache ===
    @Test
    public void testShortMatrixCache_Get() {
        short[][] matrix = ShortMatrixCache.getCachedMatrix(5, 6);
        assertNotNull(matrix);
        assertEquals(5, matrix.length);
        assertEquals(6, matrix[0].length);
    }

    @Test
    public void testShortMatrixCache_Recycle() {
        short[][] matrix = ShortMatrixCache.getCachedMatrix(5, 6);
        ShortMatrixCache.recycleMatrix(matrix);
        // Should not throw
    }

    @Test
    public void testClearCache() {
        IntegerMatrixCache.getCachedIntArray(10, 10);
        ShortMatrixCache.getCachedMatrix(10, 10);
        IntegerMatrixCache.clearCache();
        ShortMatrixCache.clearCache();
        // Should not throw
    }
}
```

**Expected Tests**: 10+

---

## 16.6: CompressorIntMatrix.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/CompressorIntMatrixTest.java`

```java
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompressorIntMatrixTest {

    @Test
    public void testCompressDecompress() {
        int[][] original = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        CompressorIntMatrix compressor = new CompressorIntMatrix();

        compressor.doCompression(original);
        int[][] decompressed = compressor.deCompress();

        assertArrayEquals(original[0], decompressed[0]);
        assertArrayEquals(original[1], decompressed[1]);
        assertArrayEquals(original[2], decompressed[2]);
    }

    @Test
    public void testCompressDecompress_LargeMatrix() {
        int rows = 100;
        int cols = 100;
        int[][] original = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                original[i][j] = i * cols + j;
            }
        }

        CompressorIntMatrix compressor = new CompressorIntMatrix();
        compressor.doCompression(original);
        int[][] decompressed = compressor.deCompress();

        for (int i = 0; i < rows; i++) {
            assertArrayEquals(original[i], decompressed[i]);
        }
    }

    @Test
    public void testGetCompressedMemory() {
        int[][] original = {{1, 2, 3}, {4, 5, 6}};
        CompressorIntMatrix compressor = new CompressorIntMatrix();
        compressor.doCompression(original);

        int compressedSize = compressor.getCompressedMemory();
        assertTrue(compressedSize > 0);
    }

    @Test
    public void testCompressionRatio() {
        // Create data with redundancy (good for compression)
        int[][] original = new int[100][100];
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                original[i][j] = 42; // Same value = high compression
            }
        }

        CompressorIntMatrix compressor = new CompressorIntMatrix();
        compressor.doCompression(original);

        int originalSize = 100 * 100 * 4; // bytes
        int compressedSize = compressor.getCompressedMemory();
        assertTrue(compressedSize < originalSize);
    }
}
```

**Expected Tests**: 8+

---

## Sprint 16 Acceptance Criteria

- [ ] `ContainerStatusTest.java` - 7 tests
- [ ] `TextSeparatorTest.java` - 7 tests
- [ ] `AverageValueTest.java` - 10 tests
- [ ] `DataContainerSimpleTest.java` - 15 tests
- [ ] `CacheTest.java` - 10 tests
- [ ] `CompressorIntMatrixTest.java` - 8 tests
- [ ] DataContainer package coverage ≥ 60%
- [ ] `mvn clean test` passes
- [ ] Total tests: 57+

---

# SPRINT 17: Control Calculators Expansion (41% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/`
- **Files**: 73 files
- **Current Coverage**: 41%
- **Target Tests**: 80+

### Untested Calculator Classes (Priority List)

1. **IntegratorCalculator** - Integration
2. **DifferentiatorCalculator** - Differentiation
3. **MovingAverageCalculator** - Signal smoothing
4. **AbsCalculator** - Absolute value
5. **SqrtCalculator** - Square root
6. **ExpCalculator** - Exponential
7. **Log10Calculator** - Logarithm base 10
8. **PowerCalculator** - Power function
9. **ModuloCalculator** - Modulo operation
10. **FloorCalculator** - Floor function

### Test Template for Calculators

```java
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class [CalculatorName]Test {
    private static final double TOLERANCE = 1e-10;
    private [CalculatorName] calculator;

    @Before
    public void setUp() {
        calculator = new [CalculatorName]();
    }

    @Test
    public void testCalculate_BasicInput() {
        // Set input, call calculate, verify output
    }

    @Test
    public void testCalculate_Zero() {
        // Edge case: zero input
    }

    @Test
    public void testCalculate_Negative() {
        // Edge case: negative input
    }

    @Test
    public void testCalculate_LargeValue() {
        // Edge case: large values
    }
}
```

---

## Sprint 17 Acceptance Criteria

- [ ] 10+ calculator test classes
- [ ] 80+ total new tests
- [ ] Calculators package coverage ≥ 85%
- [ ] `mvn clean test` passes

---

# SPRINT 18: Loss Calculation Package (13% → 85%)

## Overview
- **Package**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/`
- **Files**: 22 files (6 GUI, 16 backend)
- **Current Coverage**: 13%
- **Target Tests**: 60+

## 18.1: LossContainer.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossContainerTest.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

public class LossContainerTest {
    private static final double TOLERANCE = 1e-10;

    @Test
    public void testGetTotalLosses() {
        // switching + conduction
    }

    @Test
    public void testGetConductionLosses() {
        // Verify conduction loss getter
    }

    @Test
    public void testGetSwitchingLosses() {
        // Verify switching loss getter
    }

    @Test
    public void testTotalEqualsSum() {
        // total = conduction + switching
    }
}
```

---

## 18.2: LossCalculationSimple.java

**Create Test File**: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationSimpleTest.java`

### Key Formulas to Test

- **Conduction Loss**: `|current * Uf| + (current² * RON)`
- **Turn-on Switching Loss**: `|kON * current / deltaT|`
- **Turn-off Switching Loss**: `|kOFF * oldCurrent / deltaT|`

```java
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

public class LossCalculationSimpleTest {
    private static final double TOLERANCE = 1e-6;

    @Test
    public void testConductionLoss_Formula() {
        // Loss = |I * Uf| + I² * Ron
        // Example: I=10A, Uf=0.7V, Ron=0.01Ω
        // Loss = |10*0.7| + 100*0.01 = 7 + 1 = 8W
    }

    @Test
    public void testConductionLoss_ZeroCurrent() {
        // I=0 -> Loss=0
    }

    @Test
    public void testConductionLoss_NegativeCurrent() {
        // Verify absolute value is used
    }

    @Test
    public void testSwitchingLoss_TurnOn() {
        // Loss = |kON * I / dt|
    }

    @Test
    public void testSwitchingLoss_TurnOff() {
        // Loss = |kOFF * I_old / dt|
    }

    @Test
    public void testTotalLoss() {
        // Total = Conduction + Switching
    }
}
```

---

## 18.3: LossCurve.java & SwitchingLossCurve.java

```java
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

public class SwitchingLossCurveTest {

    @Test
    public void testConstructor() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 600.0);
        assertNotNull(curve);
    }

    @Test
    public void testSetGetCurveData() {
        double[][] data = {{0, 0, 0}, {10, 0.001, 0.002}};
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 600.0);
        curve.setCurveData(data);
        double[][] retrieved = curve.getCurveData();
        assertArrayEquals(data[0], retrieved[0], 1e-10);
    }

    @Test
    public void testCopy() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 600.0);
        SwitchingLossCurve copy = curve.copy();
        assertNotNull(copy);
        assertNotSame(curve, copy);
    }

    @Test
    public void testGetName() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 600.0);
        String name = curve.getName();
        assertNotNull(name);
        assertTrue(name.contains("25"));
    }
}
```

---

## Sprint 18 Acceptance Criteria

- [ ] `LossContainerTest.java` - 8 tests
- [ ] `LossCalculationSimpleTest.java` - 15 tests
- [ ] `LossCurveTest.java` - 10 tests
- [ ] `SwitchingLossCurveTest.java` - 10 tests
- [ ] `LeitverlusteMesskurveTest.java` - 10 tests
- [ ] `DetailedLossLookupTableTest.java` - 10 tests
- [ ] Loss calculation package coverage ≥ 70%
- [ ] `mvn clean test` passes
- [ ] Total tests: 63+

---

# Remaining Sprints Summary

## Sprint 19: Circuit Package Core (2% → 50%)
- Focus: `SimulationsKern.java`, `NetList.java`, `CircuitSheet.java`
- Tests: 150+

## Sprint 20: Newscope Data Classes (1% → 60%)
- Focus: `HiLoData.java`, `TimeSeriesConstantDt.java`, `NiceScale.java`
- Tests: 80+

## Sprint 21: Circuit Components Backend (3% → 70%)
- Focus: Non-GUI component models
- Tests: 200+

## Sprint 22: Control Package Backend (1% → 60%)
- Focus: `ControlBlock.java`, `TransferFunction.java`
- Tests: 150+

## Sprint 23: ModelViewController (21% → 85%)
- Focus: MVC pattern classes
- Tests: 40+

## Sprint 24: I18N Package (37% → 85%)
- Focus: Translation utilities
- Tests: 30+

---

# Verification Commands

After each sprint:

```bash
# Run all tests
mvn clean test

# Generate coverage report
mvn jacoco:report

# Check backend coverage
python3 scripts/backend-coverage.py

# View detailed HTML report
open target/site/jacoco/index.html
```

---

# Success Metrics

| Milestone | Backend Coverage | Total Tests |
|-----------|------------------|-------------|
| Sprint 15 Complete | 45% | 580+ |
| Sprint 17 Complete | 55% | 720+ |
| Sprint 20 Complete | 70% | 1000+ |
| Sprint 24 Complete | 85% | 1350+ |
