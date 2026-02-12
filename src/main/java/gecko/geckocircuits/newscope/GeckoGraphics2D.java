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
package gecko.geckocircuits.newscope;


import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;


public class GeckoGraphics2D extends Graphics2D {
        public final Graphics2D origGraphics;
        private ColorSettable colorStrategy = new ColorStragegyDisabledComponent();
        
        public GeckoGraphics2D(final Graphics2D g2d) {
            super();
            origGraphics = g2d;
        }
        
        @Override
        public void draw(final Shape shape) {
            origGraphics.draw(shape);
        }

        @Override
        public boolean drawImage(final Image image, final AffineTransform xForm, final ImageObserver obs) {
            return origGraphics.drawImage(image, xForm, obs);
        }

        @Override
        public void drawImage(final BufferedImage bufferedImage, final BufferedImageOp bImop, 
            final int xValue, final int yValue) {
            origGraphics.drawImage(bufferedImage, bImop, xValue, yValue);
        }

        @Override
        public void drawRenderedImage(final RenderedImage renderedImage, final AffineTransform affineTransform) {
            origGraphics.drawRenderedImage(renderedImage, affineTransform);
        }

        @Override
        public void drawRenderableImage(final RenderableImage renderableImage, final AffineTransform affineTransform) {
            origGraphics.drawRenderableImage(renderableImage, affineTransform);
        }

        @Override
        public void drawString(final String drawString, final int xPos, final int yPos) {
            origGraphics.drawString(drawString, xPos, yPos);
        }

        @Override
        public void drawString(String string, float f, float f1) {
            origGraphics.drawString(string, f, f1);
        }

        @Override
        public void drawString(AttributedCharacterIterator aci, int i, int i1) {
            origGraphics.drawString(aci, java.awt.Component.TOP_ALIGNMENT, java.awt.Component.TOP_ALIGNMENT);
        }

        @Override
        public void drawString(AttributedCharacterIterator aci, float f, float f1) {
            origGraphics.drawString(aci, f, f1);
        }

        @Override
        public void drawGlyphVector(GlyphVector gv, float f, float f1) {
            origGraphics.drawGlyphVector(gv, f, f1);
        }

        @Override
        public void fill(Shape shape) {
            origGraphics.fill(shape);
        }

        @Override
        public boolean hit(Rectangle rctngl, Shape shape, boolean bln) {
            return origGraphics.hit(rctngl, shape, bln);
        }

        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return origGraphics.getDeviceConfiguration();
        }

        @Override
        public void setComposite(Composite cmpst) {
            origGraphics.setComposite(cmpst);
        }

        @Override
        public void setPaint(Paint paint) {
            origGraphics.setPaint(paint);
        }

        @Override
        public void setStroke(Stroke stroke) {
            origGraphics.setStroke(stroke);
        }

        @Override
        public void setRenderingHint(Key key, Object o) {
            origGraphics.setRenderingHint(key, o);
        }

        @Override
        public Object getRenderingHint(Key key) {
            return origGraphics.getRenderingHint(key);
        }

        @Override
        public void setRenderingHints(Map<?, ?> map) {
            origGraphics.setRenderingHints(map);
        }

        @Override
        public void addRenderingHints(Map<?, ?> map) {
            origGraphics.addRenderingHints(map);
        }

        @Override
        public RenderingHints getRenderingHints() {
            return origGraphics.getRenderingHints();
        }

        @Override
        public void translate(int i, int i1) {
            origGraphics.translate(i, i1);
        }

        @Override
        public void translate(double d, double d1) {
            origGraphics.translate(d, d1);
        }

        @Override
        public void rotate(double d) {
            origGraphics.rotate(d);
        }

        @Override
        public void rotate(double d, double d1, double d2) {
            origGraphics.rotate(d, d1, d2);
        }

        @Override
        public void scale(double d, double d1) {
            origGraphics.scale(d, d1);
        }

        @Override
        public void shear(double d, double d1) {
            origGraphics.shear(d, d1);
        }

        @Override
        public void transform(AffineTransform at) {
            origGraphics.transform(at);
        }

        @Override
        public void setTransform(AffineTransform at) {
            origGraphics.setTransform(at);
        }

        @Override
        public AffineTransform getTransform() {
            return origGraphics.getTransform();
        }

        @Override
        public Paint getPaint() {
            return origGraphics.getPaint();
        }

        @Override
        public Composite getComposite() {
            return origGraphics.getComposite();
        }

        @Override
        public void setBackground(Color color) {
            origGraphics.setBackground(color);
        }

        @Override
        public Color getBackground() {
            return origGraphics.getBackground();
        }

        @Override
        public Stroke getStroke() {
            return origGraphics.getStroke();
        }

        @Override
        public void clip(Shape shape) {
            origGraphics.clip(shape);
        }
        
        public void setColorStrategySelected() {
            colorStrategy = new ColorStrategySelected();
        }

        @Override
        public FontRenderContext getFontRenderContext() {
            return origGraphics.getFontRenderContext();
        }

        @Override
        public Graphics create() {
            return origGraphics.create();
        }

        @Override
        public Color getColor() {
            return origGraphics.getColor();
        }

        @Override
        public void setColor(Color color) {                        
            colorStrategy.setColor(color, origGraphics);            
        }

        @Override
        public void setPaintMode() {
            origGraphics.setPaintMode();
        }

        @Override
        public void setXORMode(Color color) {
            origGraphics.setXORMode(color);
        }

        @Override
        public Font getFont() {
            return origGraphics.getFont();
        }

        @Override
        public void setFont(Font font) {
            origGraphics.setFont(font);
        }

        @Override
        public FontMetrics getFontMetrics(Font font) {
            return origGraphics.getFontMetrics();
        }

        @Override
        public Rectangle getClipBounds() {
            return origGraphics.getClipBounds();
        }

        @Override
        public void clipRect(int i, int i1, int i2, int i3) {
            origGraphics.clearRect(i, i1, i2, i3);
        }

        @Override
        public void setClip(int i, int i1, int i2, int i3) {
            origGraphics.setClip(i, i1, i2, i3);
        }

        @Override
        public Shape getClip() {
            return origGraphics.getClip();
        }

        @Override
        public void setClip(Shape shape) {
            origGraphics.setClip(shape);
        }

        @Override
        public void copyArea(int i, int i1, int i2, int i3, int i4, int i5) {
            origGraphics.copyArea(i, i1, i2, i3, i4, i5);
        }

        @Override
        public void drawLine(int i, int i1, int i2, int i3) {
            origGraphics.drawLine(i, i1, i2, i3);
        }

        @Override
        public void fillRect(int i, int i1, int i2, int i3) {
            origGraphics.fillRect(i, i1, i2, i3);
        }

        @Override
        public void clearRect(int i, int i1, int i2, int i3) {
            origGraphics.clearRect(i, i1, i2, i3);
        }

        @Override
        public void drawRoundRect(int i, int i1, int i2, int i3, int i4, int i5) {
            origGraphics.drawRect(i, i1, i2, i3);
        }

        @Override
        public void fillRoundRect(int i, int i1, int i2, int i3, int i4, int i5) {
            origGraphics.fillRoundRect(i, i1, i2, i3, i4, i5);
        }

        @Override
        public void drawOval(int i, int i1, int i2, int i3) {
            origGraphics.drawOval(i, i1, i2, i3);
            
        }

        @Override
        public void fillOval(int i, int i1, int i2, int i3) {
            origGraphics.fillOval(i, i1, i2, i3);
        }

        @Override
        public void drawArc(int i, int i1, int i2, int i3, int i4, int i5) {
            origGraphics.drawArc(i, i1, i2, i3, i4, i5);
        }

        @Override
        public void fillArc(int i, int i1, int i2, int i3, int i4, int i5) {
            origGraphics.fillArc(i, i1, i2, i3, i4, i5);
        }

        @Override
        public void drawPolyline(int[] ints, int[] ints1, int i) {
            origGraphics.drawPolyline(ints, ints1, i);
        }

        @Override
        public void drawPolygon(int[] ints, int[] ints1, int i) {
            origGraphics.drawPolygon(ints, ints1, i);
        }

        @Override
        public void fillPolygon(int[] ints, int[] ints1, int i) {
            origGraphics.fillPolygon(ints, ints1, i);
        }

        @Override
        public boolean drawImage(Image image, int i, int i1, ImageObserver io) {
            return origGraphics.drawImage(image, null, io);
        }

        @Override
        public boolean drawImage(Image image, int i, int i1, int i2, int i3, ImageObserver io) {
            return origGraphics.drawImage(image, i, i1, io);
        }

        @Override
        public boolean drawImage(Image image, int i, int i1, Color color, ImageObserver io) {
            return origGraphics.drawImage(image, i, i1, color, io);
        }

        @Override
        public boolean drawImage(Image image, int i, int i1, int i2, int i3, Color color, ImageObserver io) {
            return origGraphics.drawImage(image, i, i1, i2, i3, io);
        }

        @Override
        public boolean drawImage(Image image, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7, ImageObserver io) {
            return origGraphics.drawImage(image, i, i1, i2, i3, i4, i5, i6, i7, io);
        }

        @Override
        public boolean drawImage(final Image image, final int i, final int i1, final int i2, final int i3, 
        final int i4, final int i5, final int i6, final int i7, final Color color, ImageObserver io) {
                return origGraphics.drawImage(image, i, i1, i2, i3, i4, i5, i6, i7, color, io);
        }

        @Override
        public void dispose() {
            origGraphics.dispose();
        }
        
    }
