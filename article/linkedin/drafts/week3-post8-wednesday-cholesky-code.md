# LinkedIn Post #8 (Wednesday) - Week 3

**Publishing Date:** Week 3, Wednesday 8am
**Topic:** Cholesky Decomposition in 20 Lines
**Target:** Engineers ready for solver algorithms
**Goal:** Show actual solver code, explain why Cholesky beats LU
**Word Count:** 493 words

---

## Post Content

Cholesky decomposition solves circuit matrices 2x faster than LU. Here's the algorithm in 20 lines of Java.

Monday I explained why circuit matrices are sparse. Today: how to solve them fast.

**Why Not Gaussian Elimination?**

Standard Gaussian elimination (LU decomposition):
- Factorizes A = L·U (lower × upper triangular)
- Works for any matrix
- O(N³) operations

But circuit matrices have special properties:
- **Symmetric:** A = A^T (because resistor from i→j is same as j→i)
- **Positive definite:** All eigenvalues > 0 (physical systems)

We can exploit this!

**Cholesky Decomposition:**

For symmetric positive definite matrices:
A = L·L^T (lower triangular × its transpose)

Advantages:
- Half the storage (only need L, not L and U)
- Half the operations (~N³/6 vs ~N³/3 for LU)
- More numerically stable
- Perfect for sparse matrices

**The Algorithm:**

```java
// From: CholeskyDecomposition.java (simplified)
public class CholeskyDecomposition {

    // Decompose A = L * L^T
    public static double[][] decompose(double[][] a) {
        int n = a.length;
        double[][] L = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0.0;

                if (i == j) {
                    // Diagonal element
                    for (int k = 0; k < j; k++) {
                        sum += L[j][k] * L[j][k];
                    }
                    L[j][j] = Math.sqrt(a[j][j] - sum);
                } else {
                    // Off-diagonal element
                    for (int k = 0; k < j; k++) {
                        sum += L[i][k] * L[j][k];
                    }
                    L[i][j] = (a[i][j] - sum) / L[j][j];
                }
            }
        }
        return L;
    }

    // Solve L * L^T * x = b
    public static double[] solve(double[][] L, double[] b) {
        int n = L.length;

        // Forward substitution: L * y = b
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int k = 0; k < i; k++) {
                sum += L[i][k] * y[k];
            }
            y[i] = (b[i] - sum) / L[i][i];
        }

        // Backward substitution: L^T * x = y
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int k = i + 1; k < n; k++) {
                sum += L[k][i] * x[k];
            }
            x[i] = (y[i] - sum) / L[i][i];
        }

        return x;
    }
}
```

**How It Works:**

1. **Decomposition:** Compute L such that A = L·L^T
   - Loop through rows and columns
   - Diagonal: sqrt(A[j][j] - sum)
   - Off-diagonal: (A[i][j] - sum) / L[j][j]

2. **Forward Substitution:** Solve L·y = b (lower triangular)
   - Start from top, work down
   - Each y[i] computed from previous y values

3. **Backward Substitution:** Solve L^T·x = y (upper triangular)
   - Start from bottom, work up
   - Each x[i] computed from later x values

**Why This Is Fast:**

- No pivoting required (positive definite matrices)
- Half the multiplications of LU
- Cache-friendly (sequential memory access)
- Parallelizable (columns independent after diagonal)

**Sparse Version:**

The algorithm above is dense (O(N³)). GeckoCIRCUITS uses sparse Cholesky:
- Only operate on non-zero entries
- Complexity: O(nnz · N) where nnz = number of non-zeros
- For circuit matrices: ~100x faster than dense

We'll cover this in the weekend article.

**Friday Preview:**

Weekend article hype: "Sparse Matrices & Cholesky: The Speed Behind SPICE"
- Sparse storage formats
- Symbolic factorization (compute sparsity pattern once)
- Numerical factorization (reuse pattern every time step)
- Fill-in reduction (ordering matters!)

Subscribe now for the full deep-dive Saturday.

Source: github.com/geckocircuits/GeckoCIRCUITS
File: CholeskyDecomposition.java:45-150

---

**Hashtags:**
#CholeskyDecomposition #LinearAlgebra #CircuitSimulation #NumericalMethods #PerformanceOptimization

**CTA:** Subscribe to Substack for weekend article

**Source File References:**
- CholeskyDecomposition.java:45-150
- Matrix.java:200-250

**Metrics:**
- Word count: 493
- Reading time: 2.6 min
- Code snippets: 1 (complete decompose + solve)
- Algorithm: Cholesky (symmetric positive definite)
