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
package ch.technokrat.gecko.geckocircuits.circuit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Adapter that wraps java.awt.Graphics as GeckoGraphics.
 *
 * Use this in GUI code to provide the GeckoGraphics interface to
 * GUI-free drawing classes.
 *
 * This is the ONLY class that imports java.awt, acting as a bridge
 * between GUI-free drawing code and Swing rendering.
 *
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Adapter stores Graphics reference for drawing operations and exposes it for advanced usage")
public class AwtGraphicsAdapter implements GeckoGraphics {
    
    private final Graphics2D g;
    private final Deque<AffineTransform> transformStack = new ArrayDeque<>();
    private int currentColorRgb = 0x000000;
    
    public AwtGraphicsAdapter(Graphics graphics) {
        this.g = (Graphics2D) graphics;
    }
    
    @Override
    public void setColor(int rgb) {
        currentColorRgb = rgb;
        g.setColor(new Color(rgb));
    }
    
    @Override
    public int getColor() {
        return currentColorRgb;
    }
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }
    
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        g.drawPolyline(xPoints, yPoints, nPoints);
    }
    
    @Override
    public void drawRect(int x, int y, int width, int height) {
        g.drawRect(x, y, width, height);
    }
    
    @Override
    public void fillRect(int x, int y, int width, int height) {
        g.fillRect(x, y, width, height);
    }
    
    @Override
    public void drawOval(int x, int y, int width, int height) {
        g.drawOval(x, y, width, height);
    }
    
    @Override
    public void fillOval(int x, int y, int width, int height) {
        g.fillOval(x, y, width, height);
    }
    
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g.drawPolygon(xPoints, yPoints, nPoints);
    }
    
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g.fillPolygon(xPoints, yPoints, nPoints);
    }
    
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.drawArc(x, y, width, height, startAngle, arcAngle);
    }
    
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.fillArc(x, y, width, height, startAngle, arcAngle);
    }
    
    @Override
    public void drawString(String str, int x, int y) {
        g.drawString(str, x, y);
    }
    
    @Override
    public void translate(int x, int y) {
        g.translate(x, y);
    }
    
    @Override
    public void rotate(double theta) {
        g.rotate(theta);
    }
    
    @Override
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }
    
    @Override
    public void save() {
        transformStack.push(g.getTransform());
    }
    
    @Override
    public void restore() {
        if (!transformStack.isEmpty()) {
            g.setTransform(transformStack.pop());
        }
    }
    
    @Override
    public void setStrokeWidth(float width) {
        g.setStroke(new BasicStroke(width));
    }
    
    /**
     * Get the underlying Graphics2D for advanced operations not covered by GeckoGraphics.
     * Use sparingly - prefer adding methods to GeckoGraphics instead.
     */
    public Graphics2D getGraphics2D() {
        return g;
    }
}
