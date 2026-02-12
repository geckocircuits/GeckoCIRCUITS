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

import javax.swing.JLabel;
import javax.swing.Icon;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Dimension;

public class JLabelRot extends JLabel {

    private double _phiDegClockWise = 0;
    private String _txt;
    private static final int X_OFFSET = 5;

    public JLabelRot(final String text, final Icon icon, final int horizAlignment) {
        super(text, icon, horizAlignment);
    }

    public JLabelRot(final String text, final int horizAlignment) {
        super(text, horizAlignment);
    }

    public JLabelRot(final String text) {
        super(text);
    }

    public JLabelRot(final Icon image, final int horizAlignment) {
        super(image, horizAlignment);
    }

    public JLabelRot(final Icon image) {
        super(image);
    }

    public JLabelRot() {
        super();
    }

    public JLabelRot(final String txt, final double phiGradClockWise, final int width, final int height) {
        this(txt);
        this._txt = txt;
        this._phiDegClockWise = phiGradClockWise;
        this.setPreferredSize(new Dimension(width, height));
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        final Graphics2D g2d = (Graphics2D) graphics;
        g2d.transform(AffineTransform.getRotateInstance(_phiDegClockWise * Math.PI / 180.0));
        g2d.drawString(_txt, (-this.getHeight() + X_OFFSET), (this.getWidth() - (this.getWidth() - g2d.getFont().getSize()) / 2));
    }
}
