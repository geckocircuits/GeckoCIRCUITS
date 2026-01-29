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

import ch.technokrat.gecko.geckocircuits.allg.TechFormat;

/** Support for complex numbers in Java */
public class NComplex {
   private final float re;
   private final float im;
   private TechFormat tcf = new TechFormat();
   
   /** create a new complex number with real and imaginary parts */
   public NComplex(float r, float i) {
      re = r;
      im = i;
   }

   /** create a new complex number r + 0i */
   public NComplex(float r) {
      this(r, 0.0f);
   }

   /** create a new complex number 0 + 0i */
   public NComplex() {
      this(0.0f, 0.0f);
   }

   /** returns real value of this */
   public float getRe() {
      return re;
   }

   /** returns imaginary value of this */
   public float getIm() {
      return im;
   }

   /** return a string representation of the complex number */
   public String toString()  { return re + " + " + im + "i"; }

   /** returns a new complex number with value a + b */
   public static NComplex add(NComplex a, NComplex b) {
      float re, im;
      re = a.re + b.re;
      im = a.im + b.im;

      return new NComplex(re, im);
   }

   /** returns a new complex number with value a - b */
   public static NComplex sub(NComplex a, NComplex b) {
      float re, im;
      re = a.re - b.re;
      im = a.im - b.im;

      return new NComplex(re, im);
   }

   /** returns a new complex number with value a*b */
   public static NComplex mul(NComplex a, NComplex b) {
      float re, im;
      re = a.re*b.re - a.im*b.im;
      im = a.im*b.re + a.re*b.im;

      return new NComplex(re, im);
   }

   /** returns a new complex number with the conjugate value of a */
   public static NComplex conj(NComplex a) {
      return new NComplex(a.re, -a.im);
   }

   /** returns a new complex number with value a/b */
   public static NComplex div(NComplex a, NComplex b) {
      float re, im;
      float r,den;

      if (Math.abs(b.re) >= Math.abs(b.im)) {
         r = b.im/b.re;
         den = b.re+r*b.im;
         re = (a.re+r*a.im)/den;
         im = (a.im-r*a.re)/den;
      } else {
         r = b.re/b.im;
         den = b.im+r*b.re;
         re = (a.re*r+a.im)/den;
         im = (a.im*r-a.re)/den;
      }

      return new NComplex(re, im);
   }

   /** returns the absolute value of a */
   public static float abs(NComplex a) {
        final float epsilon = 1e-6f;
        if (Math.abs(a.re) > epsilon || Math.abs(a.im) > epsilon) {
            return (float)Math.sqrt(a.re*a.re + a.im*a.im);
        } else {
            return 0.0f;
        }
   }

   /** returns the sqrt of a */
   public static NComplex sqrt(NComplex a) {
      float im,re;
      float x,y,w,r;
      final float epsilon = 1e-6f;

      if (Math.abs(a.re) < epsilon && Math.abs(a.im) < epsilon) {
         re = 0.0f;
         im = 0.0f;
      } else {
         x = Math.abs(a.re);
         y = Math.abs(a.im);

         if (x >= y) {
            r = y/x;
            w = (float)Math.sqrt(x)*(float)Math.sqrt(0.5f*(1.0f + Math.sqrt(1.0f + r*r)));
         } else {
            r = x/y;
            w = (float)Math.sqrt(y)*(float)Math.sqrt(0.5f*(r + Math.sqrt(1.0f + r*r)));
         }
         if (a.re >= 0.0) {
            re = w;
            im = a.im/(2.0f*w);
         } else {
            im = (a.im >= 0.0f) ? w : -w;
            re = a.im/(2.0f*im);
         }
      }

      return new NComplex(re, im);
   }

   /** returns a new complex number with value a*x */
   public static NComplex RCmul(float x, NComplex a) {
      float im,re;

      re = x*a.re;
      im = x*a.im;

      return new NComplex(re, im);
   }

    public String nicePrint() {
        final float epsilon = 1e-6f;
        if(Math.abs(re) > epsilon && Math.abs(im) > epsilon) {
            if(im > 0) {
                if(Math.abs(im - 1.0f) < epsilon) {
                    return tcf.formatENG(re, 3) + "+i";
                } else
                return tcf.formatENG(re, 3) + "+" + tcf.formatENG(im, 3) + "i";
            } else {
                if(Math.abs(im + 1.0f) < epsilon) {
                    return tcf.formatENG(re, 3) + "-i";
                } else
                return tcf.formatENG(re, 3) + ""  + tcf.formatENG(im, 3) + "i";
            }
        }

        if(Math.abs(re) > epsilon) {
            return "" + tcf.formatENG(re, 3);
        } else {
            if(Math.abs(Math.abs(im) - 1.0f) < epsilon) {
                if(im > 0) return "i";
                else return "-i";
            } else {
                return tcf.formatENG(im, 3) + "i";
            }
        }
    }

    @Override
    public int hashCode() {
        return Double.hashCode(re) - 7 * Double.hashCode(im) + 3;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof NComplex) {
            NComplex compare = (NComplex) o;
            final float epsilon = 1e-6f;
            if(Math.abs(compare.re - this.re) < epsilon && Math.abs(compare.im - this.im) < epsilon) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    
    
    
}
