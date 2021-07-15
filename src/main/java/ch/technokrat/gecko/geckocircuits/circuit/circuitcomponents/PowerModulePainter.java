/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;


import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ThermMODUL;
import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author andy
 */
public class PowerModulePainter {

    private static int[] xKB1 = new int[]{168, 141, 141, 152, 832, 831, 815}, yKB1 = new int[]{421, 439, 550, 555, 366, 249, 242};
    private static int[] xKB2 = new int[]{168, 129, 238, 164, 55, 26, 40, 664, 698, 591, 664, 774, 812}, yKB2 = new int[]{421, 366, 335, 232, 265, 218, 199, 29, 86, 117, 219, 185, 245};
    private static int[] xKB3 = new int[]{168, 142, 142, 11, 11, 24, 56, 34, 34, 106, 108, 130}, yKB3 = new int[]{421, 437, 550, 370, 258, 219, 265, 287, 378, 475, 385, 366};
    private static int[] xKB4 = new int[]{722, 655, 591, 700, 721}, yKB4 = new int[]{192, 205, 118, 86, 95};
    private static int[] xKB41 = new int[]{722, 655, 664, 734}, yKB41 = new int[]{192, 205, 219, 199};
    private static int[] xKB42 = new int[]{719, 700, 663, 676}, yKB42 = new int[]{90, 86, 31, 31};
    private static int[] xKB43 = new int[]{814, 832, 791, 773}, yKB43 = new int[]{244, 249, 189, 187};
    private static int[] xKB5 = new int[]{104, 35, 164, 173, 130, 109}, yKB5 = new int[]{476, 377, 342, 353, 365, 387};
    private static int[] xKB6 = new int[]{34, 166, 164, 42, 34}, yKB6 = new int[]{377, 342, 231, 269, 290};
    private static int[] xKB7 = new int[]{166, 166, 175, 238}, yKB7 = new int[]{233, 343, 351, 335};
    private static int[] xkb1 = new int[xKB1.length], ykb1 = new int[xKB1.length], xkb2 = new int[xKB2.length], ykb2 = new int[xKB2.length], xkb3 = new int[xKB3.length], ykb3 = new int[xKB3.length], xkb4 = new int[xKB4.length], ykb4 = new int[xKB4.length];
    private static int[] xkb41 = new int[xKB41.length], ykb41 = new int[xKB41.length], xkb42 = new int[xKB42.length], ykb42 = new int[xKB42.length], xkb43 = new int[xKB43.length], ykb43 = new int[xKB43.length];
    private static int[] xkb5 = new int[xKB5.length], ykb5 = new int[xKB5.length], xkb6 = new int[xKB6.length], ykb6 = new int[xKB6.length], xkb7 = new int[xKB7.length], ykb7 = new int[xKB7.length];
    private static int xC = 430, yC = 250;  // Zentrum des Modul-Bildes in PixelPunkten
    private static double br = 2.5;
    
    static void zeichne(Graphics graphics, ThermMODUL aThis, Color color1, int dpix) {
    
        double sk = br / xC;
    
        
        for (int i1 = 0; i1 < xKB1.length; i1++) {
            xkb1[i1] = (int) (dpix * (0 - (xKB1[i1] - xC) * sk));
            ykb1[i1] = (int) (dpix * (0 - (yC - yKB1[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB2.length; i1++) {
            xkb2[i1] = (int) (dpix * (0 - (xKB2[i1] - xC) * sk));
            ykb2[i1] = (int) (dpix * (0 - (yC - yKB2[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB3.length; i1++) {
            xkb3[i1] = (int) (dpix * (0 - (xKB3[i1] - xC) * sk));
            ykb3[i1] = (int) (dpix * (0 - (yC - yKB3[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB4.length; i1++) {
            xkb4[i1] = (int) (dpix * (0 - (xKB4[i1] - xC) * sk));
            ykb4[i1] = (int) (dpix * (0 - (yC - yKB4[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB41.length; i1++) {
            xkb41[i1] = (int) (dpix * (0 - (xKB41[i1] - xC) * sk));
            ykb41[i1] = (int) (dpix * (0 - (yC - yKB41[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB42.length; i1++) {
            xkb42[i1] = (int) (dpix * (0 - (xKB42[i1] - xC) * sk));
            ykb42[i1] = (int) (dpix * (0 - (yC - yKB42[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB43.length; i1++) {
            xkb43[i1] = (int) (dpix * (0 - (xKB43[i1] - xC) * sk));
            ykb43[i1] = (int) (dpix * (0 - (yC - yKB43[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB5.length; i1++) {
            xkb5[i1] = (int) (dpix * (0 - (xKB5[i1] - xC) * sk));
            ykb5[i1] = (int) (dpix * (0 - (yC - yKB5[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB6.length; i1++) {
            xkb6[i1] = (int) (dpix * (0 - (xKB6[i1] - xC) * sk));
            ykb6[i1] = (int) (dpix * (0 - (yC - yKB6[i1]) * sk));
        }
        for (int i1 = 0; i1 < xKB7.length; i1++) {
            xkb7[i1] = (int) (dpix * (0 - (xKB7[i1] - xC) * sk));
            ykb7[i1] = (int) (dpix * (0 - (yC - yKB7[i1]) * sk));
        }
        
        // Modul zeichnen:
        graphics.setColor(Color.darkGray);
        graphics.fillPolygon(xkb41, ykb41, xkb41.length);
        graphics.fillPolygon(xkb4, ykb4, xkb4.length);
        graphics.fillPolygon(xkb5, ykb5, xkb5.length);
        graphics.fillPolygon(xkb6, ykb6, xkb6.length);
        graphics.fillPolygon(xkb7, ykb7, xkb7.length);
        graphics.setColor(Color.gray);
        graphics.fillPolygon(xkb1, ykb1, xkb1.length);
        graphics.fillPolygon(xkb2, ykb2, xkb2.length);
        graphics.fillPolygon(xkb3, ykb3, xkb3.length);
        graphics.fillPolygon(xkb41, ykb41, xkb41.length);
        graphics.fillPolygon(xkb43, ykb43, xkb43.length);
        if (color1.equals(Color.gray)) {
            graphics.setColor(Color.lightGray);
        } else {
            graphics.setColor(color1);
        }
        graphics.drawPolygon(xkb5, ykb5, xkb5.length);
        graphics.drawPolygon(xkb6, ykb6, xkb6.length);
        graphics.drawPolygon(xkb7, ykb7, xkb7.length);
        graphics.drawPolygon(xkb4, ykb4, xkb4.length);
        graphics.drawPolygon(xkb41, ykb41, xkb41.length);
        graphics.drawPolygon(xkb42, ykb42, xkb42.length);
        graphics.drawPolygon(xkb43, ykb43, xkb43.length);
        graphics.drawPolygon(xkb2, ykb2, xkb2.length);
        graphics.drawPolygon(xkb3, ykb3, xkb3.length);
        graphics.drawPolygon(xkb1, ykb1, xkb1.length);
    }
   
    
    private PowerModulePainter() {
        
    }
    
    
}
