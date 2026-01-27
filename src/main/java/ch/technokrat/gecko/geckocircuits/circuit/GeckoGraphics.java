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

/**
 * GUI-free abstraction for graphics operations.
 * 
 * Classes can define drawing behavior using this interface without
 * depending on java.awt.Graphics or javax.swing.
 * 
 * Colors are represented as RGB integers:
 * - RGB format: 0xRRGGBB (e.g., 0xFF0000 = red, 0x00FF00 = green)
 * - Use int2rgb() and rgb2int() utilities for conversions
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
public interface GeckoGraphics {
    
    // ============== Color operations ===============
    
    /**
     * Set the current drawing color.
     * @param rgb RGB color value (0xRRGGBB)
     */
    void setColor(int rgb);
    
    /**
     * Get the current drawing color.
     * @return RGB color value (0xRRGGBB)
     */
    int getColor();
    
    // ============== Line drawing ===============
    
    /**
     * Draw a line from (x1, y1) to (x2, y2).
     */
    void drawLine(int x1, int y1, int x2, int y2);
    
    /**
     * Draw a polyline (connected line segments).
     */
    void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);
    
    // ============== Shape drawing ===============
    
    /**
     * Draw an unfilled rectangle.
     */
    void drawRect(int x, int y, int width, int height);
    
    /**
     * Draw a filled rectangle.
     */
    void fillRect(int x, int y, int width, int height);
    
    /**
     * Draw an unfilled oval/ellipse.
     */
    void drawOval(int x, int y, int width, int height);
    
    /**
     * Draw a filled oval/ellipse.
     */
    void fillOval(int x, int y, int width, int height);
    
    /**
     * Draw an unfilled polygon.
     */
    void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);
    
    /**
     * Draw a filled polygon.
     */
    void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);
    
    /**
     * Draw an unfilled arc.
     * @param startAngle starting angle in degrees
     * @param arcAngle angular extent in degrees
     */
    void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);
    
    /**
     * Draw a filled arc.
     * @param startAngle starting angle in degrees
     * @param arcAngle angular extent in degrees
     */
    void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);
    
    // ============== Text ===============
    
    /**
     * Draw a text string.
     */
    void drawString(String str, int x, int y);
    
    // ============== Transformations ===============
    
    /**
     * Translate the coordinate system.
     */
    void translate(int x, int y);
    
    /**
     * Rotate the coordinate system by the given angle.
     * @param theta angle in radians
     */
    void rotate(double theta);
    
    /**
     * Scale the coordinate system.
     */
    void scale(double sx, double sy);
    
    // ============== State management ===============
    
    /**
     * Save the current graphics state (transforms, color, etc.).
     */
    void save();
    
    /**
     * Restore the previously saved graphics state.
     */
    void restore();
    
    // ============== Stroke/line style ===============
    
    /**
     * Set the line width/stroke width.
     */
    void setStrokeWidth(float width);
}
