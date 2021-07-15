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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import javax.swing.Icon;

/**
 * used for the table view of the data container (up- and down arrows)
 */
class ArrowIcon implements Icon {

    enum IconDirection {

        UP, DOWN;
    };
    private final IconDirection _direction;
    private final Polygon _pagePolygon = new Polygon(new int[]{2, 4, 4, 10, 10, 2},
            new int[]{4, 4, 2, 2, 12, 12}, 6);
    private static final int[] ARROW_X = {4, 9, 6};
    private static final Polygon ARROW_UP_POLY = new Polygon(ARROW_X, new int[]{10, 10, 4}, 3);
    private static final Polygon ARROW_DOWN_POLY = new Polygon(ARROW_X, new int[]{6, 6, 11}, 3);
    private static final int ICON_SIZE = 14;

    public ArrowIcon(final IconDirection direction) {
        _direction = direction;
    }

    @Override
    public int getIconWidth() {
        return ICON_SIZE;
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE;
    }

    @Override
    public void paintIcon(final Component component, final Graphics graphics, final int xPixel, final int yPixel) {
        graphics.setColor(Color.black);
        _pagePolygon.translate(xPixel, yPixel);
        graphics.drawPolygon(_pagePolygon);
        _pagePolygon.translate(-xPixel, -yPixel);
        switch (_direction) {
            case UP:
                ARROW_UP_POLY.translate(xPixel, yPixel);
                graphics.fillPolygon(ARROW_UP_POLY);
                ARROW_UP_POLY.translate(-xPixel, -yPixel);
                break;
            case DOWN:
                ARROW_DOWN_POLY.translate(xPixel, yPixel);
                graphics.fillPolygon(ARROW_DOWN_POLY);
                ARROW_DOWN_POLY.translate(-xPixel, -yPixel);
                break;
            default:
                assert false;
        }
    }
}
