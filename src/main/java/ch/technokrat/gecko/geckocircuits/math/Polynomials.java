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

/**
* The Polynomials class contains methods to handle Polynomials and Rational Functions.
*/
public strictfp class Polynomials extends Object {

   /**
   * Given the n+1 coefficients of a polynomial of degree n in u[0..n], and the nv+1 coefficients
   * of another polynomial of degree nv in v[0..nv], divide the polynomial u by the polynomial
   * v giving a quotient polynomial whose coefficients are returned in q[0..n], and a
   * remainder polynomial whose coefficients are returned in r[0..n]. The elements r[nv..n]
   * and q[n-nv+1..n] are returned as zero.
   */
   public static void poldiv(float u[], int n, float v[], int nv, float q[], float r[])
   {
      for(int j = 0; j <= n; j++) {
         r[j] = u[j];
         q[j] = 0.0f;
      }
      for(int k = n-nv; k >= 0; k--) {
         q[k] = r[nv+k]/v[nv];
         for(int j = nv+k-1; j >= k; j--) r[j] -= q[k]*v[j-k];
      }
      for (int j = nv; j <= n; j++) r[j] = 0.0f;
   }
   
}