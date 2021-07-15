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
package ch.technokrat.gecko.geckocircuits.math;


//package Jama;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.math.BigDecimal;
//import Jama.util.*;

/**
   Jama = Java Matrix class.
<P>
   The Java Matrix Class provides the fundamental operations of numerical
   linear algebra.  Various constructors create Matrices from two dimensional
   arrays of double precision floating point numbers.  Various "gets" and
   "sets" provide access to submatrices and matrix elements.  Several methods
   implement basic matrix arithmetic, including matrix addition and
   multiplication, matrix norms, and element-by-element array operations.
   Methods for reading and printing matrices are also included.  All the
   operations in this version of the Matrix Class involve real matrices.
   Complex matrices may be handled in a future version.
<P>
   Five fundamental matrix decompositions, which consist of pairs or triples
   of matrices, permutation vectors, and the like, produce results in five
   decomposition classes.  These decompositions are accessed by the Matrix
   class to compute solutions of simultaneous linear equations, determinants,
   inverses and other matrix functions.  The five decompositions are:
<P><UL>
   <LI>Cholesky Decomposition of symmetric, positive definite matrices.
   <LI>LU Decomposition of rectangular matrices.
   <LI>QR Decomposition of rectangular matrices.
   <LI>Singular Value Decomposition of rectangular matrices.
   <LI>Eigenvalue Decomposition of both symmetric and nonsymmetric square matrices.
</UL>
<DL>
<DT><B>Example of use:</B></DT>
<P>
<DD>Solve a linear system A x = b and compute the residual norm, ||b - A x||.
<P><PRE>
      double[][] vals = {{1.,2.,3},{4.,5.,6.},{7.,8.,10.}};
      Matrix A = new Matrix(vals);
      Matrix b = Matrix.random(3,1);
      Matrix x = A.solve(b);
      Matrix r = A.times(x).minus(b);
      double rnorm = r.normInf();
</PRE></DD>
</DL>

@author The MathWorks, Inc. and the National Institute of Standards and Technology.
@version 5 August 1998
*/

public class BigMatrix implements java.io.Serializable {

/* ------------------------
   Class variables
 * ------------------------ */

   /** Array for internal storage of elements.
   @serial internal array storage.
   */
   private BigDecimal[][] A;
   private BigLUDecomposition luDecomp;
   /** Row and column dimensions.
   @serial row dimension.
   @serial column dimension.
   */
   private int m, n;

/* ------------------------
   Constructors
 * ------------------------ */

   /** Construct an m-by-n matrix of zeros.
   @param m    Number of rows.
   @param n    Number of colums.
   */

   public BigMatrix (int m, int n) {
      this.m = m;
      this.n = n;
      A = new BigDecimal[m][n];
      for(int i = 0; i < m; i++) {
          for(int j = 0; j < n; j++) {
              A[i][j] = new BigDecimal(0);
          }
      }
      luDecomp = null;
   }

   

   /** Construct a matrix from a 2-D array.
   @param initA    Two-dimensional array of doubles.
   @exception  IllegalArgumentException All rows must have the same length
   @see        #constructWithCopy
   */
   public BigMatrix (BigDecimal[][] initA) {
      m = initA.length;
      n = initA[0].length;
      this.A = new BigDecimal[m][n];
      for (int i = 0; i < m; i++) {
         if (initA[i].length != n) {
            throw new IllegalArgumentException("All rows must have the same length.");
         }
         for(int j = 0; j < n; j++) {
             BigDecimal tmpNumber = new BigDecimal(0);
             this.A[i][j] = tmpNumber.add(initA[i][j]);
         }
      }
   }

   /** Construct a matrix from a one-dimensional packed array
   @param vals One-dimensional array of doubles, packed by columns (ala Fortran).
   @param m    Number of rows.
   @exception  IllegalArgumentException Array length must be a multiple of m.
   */

   public BigMatrix (double vals[], int m) {
      this.m = m;
      n = (m != 0 ? vals.length/m : 0);
      if (m*n != vals.length) {
         throw new IllegalArgumentException("Array length must be a multiple of m.");
      }
      A = new BigDecimal[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = new BigDecimal(vals[i+j*m]);
         }
      }
   }

/* ------------------------
   Public Methods
 * ------------------------ */

 
   /** Access the internal two-dimensional array.
   @return     Pointer to the two-dimensional array of matrix elements.
   */

   public BigDecimal[][] getArray () {
      return A;
   }

   /** Copy the internal two-dimensional array.
   @return     Two-dimensional array copy of matrix elements.
   */

   public BigDecimal[][] getArrayCopy () {
      BigDecimal[][] C = new BigDecimal[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = new BigDecimal(0);
            C[i][j] = C[i][j].add(A[i][j]);
         }
      }

      return C;
   }

   /** Make a one-dimensional column packed copy of the internal array.
   @return     Matrix elements packed in a one-dimensional array by columns.
   */

   public double[] getColumnPackedCopy () {
      double[] vals = new double[m*n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            vals[i+j*m] = A[i][j].doubleValue();
         }
      }
      return vals;
   }


   /** Get row dimension.
   @return     m, the number of rows.
   */

   public int getRowDimension () {
      return m;
   }

   /** Get column dimension.
   @return     n, the number of columns.
   */

   public int getColumnDimension () {
      return n;
   }

   

  

   /** Get a submatrix.
   @param r    Array of row indices.
   @param i0   Initial column index
   @param i1   Final column index
   @return     A(r(:),j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public BigMatrix getMatrix (int[] r, int j0, int j1) {
      BigMatrix X = new BigMatrix(r.length,j1-j0+1);
      BigDecimal[][] B = X.getArray();
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = j0; j <= j1; j++) {
               B[i][j-j0] = A[r[i]][j];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** One norm
   @return    maximum column sum.
   */

   public double norm1 () {
      double f = 0;
      for (int j = 0; j < n; j++) {
         double s = 0;
         for (int i = 0; i < m; i++) {
            s += A[i][j].abs().doubleValue();
         }
         f = Math.max(f,s);
      }
      return f;
   }


   /** Infinity norm
   @return    maximum row sum.
   */

   public double normInf () {
      double f = 0;
      for (int i = 0; i < m; i++) {
         double s = 0;
         for (int j = 0; j < n; j++) {
            s += A[i][j].abs().doubleValue();
         }
         f = Math.max(f,s);
      }
      return f;
   }


   /** C = A + B
   @param B    another matrix
   @return     A + B
   */
//
//   public Matrix plus (BigMatrix B) {
//      checkMatrixDimensions(B);
//      Matrix X = new Matrix(m,n);
//      double[][] C = X.getArray();
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            C[i][j] = A[i][j] + B.A[i][j];
//         }
//      }
//      return X;
//   }

   /** A = A + B
   @param B    another matrix
   @return     A + B
   */
//
//   public Matrix plusEquals (Matrix B) {
//      checkMatrixDimensions(B);
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            A[i][j] = A[i][j] + B.A[i][j];
//         }
//      }
//      return this;
//   }

   /** C = A - B
   @param B    another matrix
   @return     A - B
   */

//   public Matrix minus (Matrix B) {
//      checkMatrixDimensions(B);
//      Matrix X = new Matrix(m,n);
//      double[][] C = X.getArray();
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            C[i][j] = A[i][j] - B.A[i][j];
//         }
//      }
//      return X;
//   }

   /** A = A - B
   @param B    another matrix
   @return     A - B
   */

//   public Matrix minusEquals (Matrix B) {
//      checkMatrixDimensions(B);
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            A[i][j] = A[i][j] - B.A[i][j];
//         }
//      }
//      return this;
//   }

   /** Element-by-element multiplication, C = A.*B
   @param B    another matrix
   @return     A.*B
   */

//   public Matrix arrayTimes (Matrix B) {
//      checkMatrixDimensions(B);
//      Matrix X = new Matrix(m,n);
//      double[][] C = X.getArray();
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            C[i][j] = A[i][j] * B.A[i][j];
//         }
//      }
//      return X;
//   }

   /** Element-by-element multiplication in place, A = A.*B
   @param B    another matrix
   @return     A.*B
   */

//   public Matrix arrayTimesEquals (Matrix B) {
//      checkMatrixDimensions(B);
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            A[i][j] = A[i][j] * B.A[i][j];
//         }
//      }
//      return this;
//   }

   /** Element-by-element right division, C = A./B
   @param B    another matrix
   @return     A./B
   */

//   public Matrix arrayRightDivide (Matrix B) {
//      checkMatrixDimensions(B);
//      Matrix X = new Matrix(m,n);
//      double[][] C = X.getArray();
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            C[i][j] = A[i][j] / B.A[i][j];
//         }
//      }
//      return X;
//   }

   /** Element-by-element right division in place, A = A./B
   @param B    another matrix
   @return     A./B
   */

//   public Matrix arrayRightDivideEquals (Matrix B) {
//      checkMatrixDimensions(B);
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            A[i][j] = A[i][j] / B.A[i][j];
//         }
//      }
//      return this;
//   }

   /** Element-by-element left division, C = A.\B
   @param B    another matrix
   @return     A.\B
   */

//   public Matrix arrayLeftDivide (Matrix B) {
//      checkMatrixDimensions(B);
//      Matrix X = new Matrix(m,n);
//      double[][] C = X.getArray();
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            C[i][j] = B.A[i][j] / A[i][j];
//         }
//      }
//      return X;
//   }

   /** Element-by-element left division in place, A = A.\B
   @param B    another matrix
   @return     A.\B
   */

//   public Matrix arrayLeftDivideEquals (Matrix B) {
//      checkMatrixDimensions(B);
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            A[i][j] = B.A[i][j] / A[i][j];
//         }
//      }
//      return this;
//   }

   /** Multiply a matrix by a scalar, C = s*A
   @param s    scalar
   @return     s*A
   */

//   public Matrix times (double s) {
//      Matrix X = new Matrix(m,n);
//      double[][] C = X.getArray();
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            C[i][j] = s*A[i][j];
//         }
//      }
//      return X;
//   }
//
//   /** Multiply a matrix by a scalar in place, A = s*A
//   @param s    scalar
//   @return     replace A by s*A
//   */
//
//   public Matrix timesEquals (double s) {
//      for (int i = 0; i < m; i++) {
//         for (int j = 0; j < n; j++) {
//            A[i][j] = s*A[i][j];
//         }
//      }
//      return this;
//   }
//
//   /** Linear algebraic matrix multiplication, A * B
//   @param B    another matrix
//   @return     Matrix product, A * B
//   @exception  IllegalArgumentException Matrix inner dimensions must agree.
//   */
//
//   public Matrix times (Matrix B) {
//      if (B.m != n) {
//         throw new IllegalArgumentException("Matrix inner dimensions must agree.");
//      }
//      Matrix X = new Matrix(m,B.n);
//      double[][] C = X.getArray();
//      double[] Bcolj = new double[n];
//      for (int j = 0; j < B.n; j++) {
//         for (int k = 0; k < n; k++) {
//            Bcolj[k] = B.A[k][j];
//         }
//         for (int i = 0; i < m; i++) {
//            double[] Arowi = A[i];
//            double s = 0;
//            for (int k = 0; k < n; k++) {
//               s += Arowi[k]*Bcolj[k];
//            }
//            C[i][j] = s;
//         }
//      }
//      return X;
//   }

   /** LU Decomposition
   @return     LUDecomposition
   @see LUDecomposition
   */

   public BigLUDecomposition lu () {
      return new BigLUDecomposition(this);
   }

   /** QR Decomposition
   @return     QRDecomposition
   @see QRDecomposition
   */

//   public QRDecomposition qr () {
//      return new QRDecomposition(this);
//   }

   /** Cholesky Decomposition
   @return     CholeskyDecomposition
   @see CholeskyDecomposition
   */

//   public CholeskyDecomposition chol () {
//      return new CholeskyDecomposition(this);
//   }
//
//   /** Singular Value Decomposition
//   @return     SingularValueDecomposition
//   @see SingularValueDecomposition
//   */
//
//   public SingularValueDecomposition svd () {
//      return new SingularValueDecomposition(this);
//   }

   /** Eigenvalue Decomposition
   @return     EigenvalueDecomposition
   @see EigenvalueDecomposition
   */

//   public EigenvalueDecomposition eig () {
//      return new EigenvalueDecomposition(this);
//   }

   /** Solve A*X = B
   @param B    right hand side
   @return     solution if A is square, least squares solution otherwise
   */
   public BigMatrix solve (BigMatrix B) {
      if(m == n) {
          if(luDecomp == null) {
              luDecomp = new BigLUDecomposition(this);
          }
          return luDecomp.solve(B);
      } else {
          return null;
        //return (new QRDecomposition(this)).solve(B);
      }
   }


   public void ResetLUDecomp() {
    luDecomp = null;
   }


   /** Print the matrix to stdout.   Line the elements up in columns
     * with a Fortran-like 'Fw.d' style format.
   @param w    Column width.
   @param d    Number of digits after the decimal.
   */

   public void print (int w, int d) {
      print(new PrintWriter(System.out,true),w,d); }

   /** Print the matrix to the output stream.   Line the elements up in
     * columns with a Fortran-like 'Fw.d' style format.
   @param output Output stream.
   @param w      Column width.
   @param d      Number of digits after the decimal.
   */

   public void print (PrintWriter output, int w, int d) {
      DecimalFormat format = new DecimalFormat();
      format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
      format.setMinimumIntegerDigits(1);
      format.setMaximumFractionDigits(d);
      format.setMinimumFractionDigits(d);
      format.setGroupingUsed(false);
      print(output,format,w+2);
   }

   /** Print the matrix to stdout.  Line the elements up in columns.
     * Use the format object, and right justify within columns of width
     * characters.
     * Note that is the matrix is to be read back in, you probably will want
     * to use a NumberFormat that is set to US Locale.
   @param format A  Formatting object for individual elements.
   @param width     Field width for each column.
   @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

   public void print (NumberFormat format, int width) {
      print(new PrintWriter(System.out,true),format,width); }

   // DecimalFormat is a little disappointing coming from Fortran or C's printf.
   // Since it doesn't pad on the left, the elements will come out different
   // widths.  Consequently, we'll pass the desired column width in as an
   // argument and do the extra padding ourselves.

   /** Print the matrix to the output stream.  Line the elements up in columns.
     * Use the format object, and right justify within columns of width
     * characters.
     * Note that is the matrix is to be read back in, you probably will want
     * to use a NumberFormat that is set to US Locale.
   @param output the output stream.
   @param format A formatting object to format the matrix elements
   @param width  Column width.
   @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

   public void print (PrintWriter output, NumberFormat format, int width) {
      output.println();  // start on new line.
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            String s = format.format(A[i][j]); // format the number
            int padding = Math.max(1,width-s.length()); // At _least_ 1 space
            for (int k = 0; k < padding; k++)
               output.print(' ');
            output.print(s);
         }
         output.println();
      }
      output.println();   // end with blank line.
   }

   /** Read a matrix from a stream.  The format is the same the print method,
     * so printed matrices can be read back in (provided they were printed using
     * US Locale).  Elements are separated by
     * whitespace, all the elements for each row appear on a single line,
     * the last row is followed by a blank line.
   @param input the input stream.
   */

   public static Matrix read (BufferedReader input) throws java.io.IOException {
      StreamTokenizer tokenizer= new StreamTokenizer(input);

      // Although StreamTokenizer will parse numbers, it doesn't recognize
      // scientific notation (E or D); however, Double.valueOf does.
      // The strategy here is to disable StreamTokenizer's number parsing.
      // We'll only get whitespace delimited words, EOL's and EOF's.
      // These words should all be numbers, for Double.valueOf to parse.

      tokenizer.resetSyntax();
      tokenizer.wordChars(0,255);
      tokenizer.whitespaceChars(0, ' ');
      tokenizer.eolIsSignificant(true);
      java.util.Vector v = new java.util.Vector();

      // Ignore initial empty lines
      while (tokenizer.nextToken() == StreamTokenizer.TT_EOL);
      if (tokenizer.ttype == StreamTokenizer.TT_EOF)
	throw new java.io.IOException("Unexpected EOF on matrix read.");
      do {
         v.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st row.
      } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);

      int n = v.size();  // Now we've got the number of columns!
      double row[] = new double[n];
      for (int j=0; j<n; j++)  // extract the elements of the 1st row.
         row[j]=((Double)v.elementAt(j)).doubleValue();
      v.removeAllElements();
      v.addElement(row);  // Start storing rows instead of columns.
      while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
         // While non-empty lines
         v.addElement(row = new double[n]);
         int j = 0;
         do {
            if (j >= n) throw new java.io.IOException
               ("Row " + v.size() + " is too long.");
            row[j++] = Double.valueOf(tokenizer.sval).doubleValue();
         } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
         if (j < n) throw new java.io.IOException
            ("Row " + v.size() + " is too short.");
      }
      int m = v.size();  // Now we've got the number of rows.
      double[][] A = new double[m][];
      v.copyInto(A);  // copy the rows out of the vector
      return new Matrix(A);
   }


/* ------------------------
   Private Methods
 * ------------------------ */

   /** Check if size(A) == size(B) **/

   private void checkMatrixDimensions (BigMatrix B) {
      if (B.m != m || B.n != n) {
         throw new IllegalArgumentException("Matrix dimensions must agree.");
      }
   }

}
