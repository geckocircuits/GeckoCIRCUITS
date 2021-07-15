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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JDialog;

public class PreviewDialogSine extends PreviewDialog {

    public PreviewDialogSine(final JDialog parent) {
        super(parent);
        setTitle(" SINUSOIDAL");
    }

    @Override
    JComponent createComponent() {
        final int b = 160, h = 110, p1 = 10, p2 = 3, rand = 10, rd = 2, q = -1;
        final int x2 = rand, x0 = x2 + 20, x1 = x0 + b, y1 = rand, y0 = y1 + h, y2 = y0 + x2;
        final int[] kordX = new int[]{x0, x0, x0 - p2, x0 + p2, x0, x0, x2, x1 + rand, x1 - p1 + rand, x1 - p1 + rand, x1 + rand};
        final int[] kordY = new int[]{y2, y1, y1 + p1, y1 + p1, y1, y0, y0, y0, y0 - p2, y0 + p2, y0};
        final int[] sinX = new int[b / 2], sinY = new int[sinX.length];
        final int offset = 20, ac = 55, phase = 60;
        for (int i1 = 0; i1 < sinX.length; i1++) {
            sinX[i1] = x0 + (int) (i1 * (b * 1.0) / sinX.length);
            sinY[i1] = y0 - (offset + (int) (ac * Math.sin((i1 * 2 * Math.PI / sinX.length) - phase * Math.PI / 180.0)));
        }
        final int xPh = (int) (phase / 360.0 * b), xMx = xPh + b / 4;
        JComponent jcp = new JComponent() {
            public void paint(Graphics g) {
                g.setFont(GlobalFonts.foAUSWAHL);
                g.setColor(Color.white);
                g.drawRect(0, 0, x1 + 2 * rand, y2 + 4 * rand);
                g.setColor(Color.black);
                g.drawPolyline(kordX, kordY, kordX.length);  // Koord.
                g.setColor(Color.blue);
                g.drawPolyline(sinX, sinY, sinX.length);  // Signalkurve
                g.setColor(Color.lightGray);
                g.drawLine(x2, y0 - offset, x1, y0 - offset);  // offset-Linie (Mittelwert)
                g.setColor(Color.gray);
                g.drawLine(x0 + xPh, y0 - offset - 10, x0 + xPh, y0 + 10);  // Markierungsline fuer Phasenverschiebung
                g.drawLine(x1, y0 - offset / 2, x1, y0 - offset - ac / 2);  // senkrechte Linie bei (2*PI)
                g.drawPolyline(new int[]{x0 + xPh, x0 + xPh, x0 + xPh - p2, x0 + xPh}, new int[]{y0 - offset - 2 * p1, y0 - offset, y0 - offset - p1, y0 - offset - p1}, 4);  // Pfeilspitze senkrecht
                g.drawPolyline(new int[]{x0 + xPh, x0 + xPh, x0 + xPh - p2, x0 + xPh}, new int[]{y0 + 2 * p1, y0, y0 + p1, y0 + p1}, 4);  // Pfeilspitze senkrecht
                g.drawPolyline(new int[]{x0 + xMx, x0 + xMx + p2, x0 + xMx, x0 + xMx, x0 + xMx + p2, x0 + xMx}, new int[]{y0 - offset - ac + p1, y0 - offset - ac + p1, y0 - offset - ac, y0 - offset, y0 - offset - p1, y0 - offset - p1}, 6);  // AC-max-Linie (senkrecht) plus Pfeilspitzen
                g.setColor(Color.white);
                g.fillRect(x0 + xMx + 10, y0 - offset - ac / 2 - 7, 50, 16);
                g.fillRect(x0 - 1, y0 - offset - 30, xMx - 5, 16);
                g.setColor(Color.magenta);
                g.drawPolyline(new int[]{x0 + q, x0 + q, x0 + q, x0 + q + xPh, x0 + q + xPh - p1, x0 + q + xPh - p1, x0 + q + xPh}, new int[]{y0 + q - offset - p2, y0 + q - offset + p2, y0 + q - offset, y0 + q - offset, y0 + q - offset - p2, y0 + q - offset + p2, y0 + q - offset}, 7);  // Phasenverschiebung: Pfeil waagrecht
                g.drawLine(x0, y0 - offset - p2, x0, y0 - offset + p2);
                g.drawString("phase = " + phase + "°", 5, y0 - offset - 17);
                g.drawString("offset", x0 + xPh + 4, y0 + 14);
                g.drawString("amplMAX", x0 + xMx + 4, y0 - offset - ac / 2 + 7);
                g.setColor(Color.black);
                g.fillOval(x0 + xPh - rd, y0 - offset - rd, 2 * rd, 2 * rd);
            }
        };
        jcp.setPreferredSize(new Dimension(x1 + 2 * rand, y2 + 4 * rand));        
        return jcp;
    }
        
}
