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

import java.awt.Graphics2D;
import static java.lang.Math.round;
/**
 *
 * @author andy
 */
public enum GeckoSymbol {

    CIRCLE(-838300),
    CIRCLE_FILLED(-838301),
    CROSS(-838302),
    RECT(-838303),
    RECT_FILLED(-838304),
    TRIANG(-838305),
    TRIANG_FILLED(-838306);

    private static final int DMCIRCLE = 8, H_CROSS = 4, A_RECT = 6, A_TRIANG = 8;
    private static final float TRIANGLE_SIZE = 0.29f;
    
    private final int _code;

    GeckoSymbol(final int code) {
        _code = code;
    }

    public int code() {
        return _code;
    }
    

    static GeckoSymbol getFromCode(final int code) {
        for(GeckoSymbol val : GeckoSymbol.values()) {
            if(val._code == code) {
                return val;
            }
        }
        
        return GeckoSymbol.CROSS;
    }
    
    static GeckoSymbol getFromOrdinal(final int ordinal) {
        for(GeckoSymbol val : GeckoSymbol.values()) {
            if(val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }
        
    void drawSymbol(final Graphics2D g2d, final float xPix, final float yPix) {
        
        // I am using round instead of a simple (int) cast, since for the export to vectorgraphics
        // formats, this is more accurate!
        switch (this) {
            case CIRCLE:
                g2d.drawOval(round(xPix - DMCIRCLE / 2), round(yPix - DMCIRCLE / 2), DMCIRCLE, DMCIRCLE);
                break;
            case CIRCLE_FILLED:
                g2d.fillOval(round(xPix - DMCIRCLE / 2), round(yPix - DMCIRCLE / 2), DMCIRCLE, DMCIRCLE);
                break;
            case CROSS:
                g2d.drawLine(round(xPix - H_CROSS), round(yPix), round(xPix + H_CROSS), round(yPix));
                g2d.drawLine(round(xPix), round(yPix - H_CROSS), round(xPix), round(yPix + H_CROSS));
                break;
            case RECT:
                g2d.drawRect(round(xPix - A_RECT / 2), round(yPix - A_RECT / 2), A_RECT, A_RECT);
                break;
            case RECT_FILLED:
                g2d.fillRect(round(xPix - A_RECT / 2), round(yPix - A_RECT / 2), round(A_RECT), round(A_RECT));
                break;
            case TRIANG:
                int[] xPoints = new int[]{round(xPix - A_TRIANG / 2), round(xPix + A_TRIANG / 2), round(xPix)};
                int[] yPoints = new int[]{round(yPix + (int) (TRIANGLE_SIZE * A_TRIANG)), round(yPix 
                        + (int) (TRIANGLE_SIZE * A_TRIANG)), round(yPix - (int) (2 * TRIANGLE_SIZE * A_TRIANG))};
                g2d.drawPolygon(xPoints, yPoints, xPoints.length);
                break;
            case TRIANG_FILLED:
                xPoints = new int[]{round(xPix - A_TRIANG / 2), round(xPix + A_TRIANG / 2), round(xPix)};
                yPoints = new int[]{round(yPix + (int) (TRIANGLE_SIZE * A_TRIANG)),
                    round(yPix + (int) (TRIANGLE_SIZE * A_TRIANG)), round(yPix - (int) (2 * TRIANGLE_SIZE * A_TRIANG))};
                g2d.fillPolygon(xPoints, yPoints, xPoints.length);
                break;
            default:
                assert false;
        }
    }
}
