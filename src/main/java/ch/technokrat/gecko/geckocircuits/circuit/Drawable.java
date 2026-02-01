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
 * Interface for objects that can draw themselves to a GeckoGraphics context.
 * 
 * This is the GUI-free version of paintComponent(Graphics).
 * 
 * Instead of:
 * ```java
 * public void paintComponent(Graphics g) {
 *     // drawing code
 * }
 * ```
 * 
 * Use:
 * ```java
 * @Override
 * public void draw(GeckoGraphics g) {
 *     // GUI-free drawing code
 * }
 * ```
 * 
 * In GUI code that needs to render Drawable objects:
 * ```java
 * drawable.draw(new AwtGraphicsAdapter(graphics));
 * ```
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
public interface Drawable {
    
    /**
     * Draw this object using the provided GUI-free graphics context.
     * 
     * @param g the graphics context (abstracted, no java.awt imports needed)
     */
    void draw(GeckoGraphics g);
}
