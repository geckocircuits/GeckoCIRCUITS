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

package gecko.geckocircuits.scope;

import gecko.geckocircuits.general.GlobalColors;
import gecko.geckocircuits.general.TechFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class FourierDiagram extends GraferV3 implements MouseListener, MouseMotionListener {

    //----------------------------
    private double[] cnSG;
    private int nMin;
    private double[] xNew, yNew;
    private int bi, hi, X0xi, X0yi, Y0xi, Y0yi;  // Hoehe, Breite, X-u-Y-Koord. des Achsenkreuzes (alles in Pix)
    //-----------------------
    private int mouseMode = GraferImplementation.MOUSEMODE_NONE;
    private int x1Zoom, y1Zoom, x2Zoom, y2Zoom;
    private boolean imDragModus = false;
    // Bereichsgrenzen eines Diagramms bezueglich Maus-Klick:
    private int[] xGrfMIN, xGrfMAX, yGrfMIN, yGrfMAX;
    private int indexAngeklickterGraph = 0;
    //-----------------------
    private boolean xSliderActive = false;
    private int xSliderPixels;
    private double[] xSliderValue = new double[]{-1, -1};  // einem einzelnen Pixelpunkt sind eventuell mehrere Werte zugeordnet
    private double[] yNewValue = new double[]{-1, -1};
    private TechFormat cf = new TechFormat();
    //-----------------------
    private final double _baseFrequency;

    public FourierDiagram(double[] cnSG, int nMin, double baseFreqency) {
        bi = 350;
        hi = 300;
        X0xi = 60;
        X0yi = hi + 60;
        Y0xi = X0xi;
        Y0yi = X0yi;
        _baseFrequency = baseFreqency;

        this.setPreferredSize(new Dimension(bi + 2 * X0xi, X0yi + X0yi - hi));  // fuer pack() im uebergeordneten JFrame
        // Bereichsgrenzen fuers Maus-Klicken:
        xGrfMIN = new int[]{0};
        xGrfMAX = new int[]{this.getWidth()};
        yGrfMIN = new int[]{0};
        yGrfMAX = new int[]{this.getHeight()};
        //---------------------------------------
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        //
        this.cnSG = cnSG;
        this.nMin = nMin;
        //-----------------------
        // Kurve 'aufbereiten'  --> Umwandlung der Balkendaten
        xNew = new double[4 * cnSG.length];
        yNew = new double[4 * cnSG.length];
        int i2 = 0;  // Zaehler in 'xNew'
        double balkenbreite = 0.1;
        double deltaX = 1e-6;
        for (int i1 = 0; i1 < cnSG.length; i1++) {
            xNew[i2] = nMin + i1 - balkenbreite - deltaX;
            yNew[i2] = 0;
            xNew[i2 + 1] = nMin + i1 - balkenbreite;
            yNew[i2 + 1] = cnSG[i1];
            xNew[i2 + 2] = nMin + i1 + balkenbreite;
            yNew[i2 + 2] = cnSG[i1];
            xNew[i2 + 3] = nMin + i1 + balkenbreite + deltaX;
            yNew[i2 + 3] = 0;
            i2 += 4;
        }
        //-----------------------
        DataContainer data = new DataContainerSimple(2, xNew.length);
        for (int i1 = 0; i1 < xNew.length; i1++) {
            data.setValue(xNew[i1], 0, i1);
            data.setValue(yNew[i1], 1, i1);
        }
        worksheetData = data;
        //-----------------------
        this.setAxes();
        this.setCurves();
        this.resize();  // damit's auch wirklich passt
    }

    // Neuskalierung des Diagramms, wenn die Fenster-Abmessungen geaendert werden -->
    public void resize() {
        //---------------------------------------
        bi = this.getWidth() - 2 * X0xi;
        hi = this.getHeight() - (2 * 35);
        X0xi = 75;
        X0yi = hi + 30;
        Y0xi = X0xi;
        Y0yi = X0yi;
        this.setAxisWidthHeightX0Y0(new int[]{bi}, new int[]{hi}, new int[]{X0xi}, new int[]{X0yi}, new int[]{Y0xi}, new int[]{Y0yi});
        //---------------------------------------
        // Bereichsgrenzen fuers Maus-Klicken --> wird hier fuer 2 Diagramme definiert
        xGrfMIN = new int[]{0};
        xGrfMAX = new int[]{this.getWidth()};
        yGrfMIN = new int[]{0};
        yGrfMAX = new int[]{this.getHeight()};
        //---------------------------------------
    }

    public void setMouseMode(int mouseMode) {
        this.mouseMode = mouseMode;
        //---------
        if (mouseMode == GraferImplementation.MOUSEMODE_NONE) {
            xSliderActive = false;
            repaint();
        } else if (mouseMode == GraferImplementation.MOUSEMODE_ZOOM_AUTOFIT) {
            double ymin = 0, ymax = -1e99;
            for (double value : yNew) {
                if (value > ymax) {
                    ymax = value;
                }
            }
            double[] empf = auto_Achsenbegrenzung_Wertempfehlung(ymin, ymax);
            this.setzeAchsenBegrenzungen(new double[]{xNew[0]}, new double[]{xNew[xNew.length - 1]}, new boolean[]{true}, new double[]{ymin}, new double[]{empf[1]}, new boolean[]{true});
            this.setzeTickSpacing(new double[]{(cnSG.length / 10.0)}, new double[]{empf[4]});
            repaint();
        } else if (mouseMode == GraferImplementation.MOUSEMODE_VALUE_DISPLAY_SLIDER) {
            //------------------------------------
            xSliderActive = true;
            xSliderPixels = X0xi;  // x-Schieber wird an den Anfang gesetzt
        }
        //---------
    }

    // wird ueberschrieben, um Text dazuschreiben zu koennen -->
    @Override
    protected void draw(Graphics g) {
        if ((mouseMode == GraferImplementation.MOUSEMODE_ZOOM_WINDOW) && imDragModus) {
            g.setColor(GlobalColors.farbeZoomRechteck);
            int b = Math.abs(x2Zoom - x1Zoom), h = Math.abs(y2Zoom - y1Zoom);
            if ((x1Zoom > x2Zoom) && (y1Zoom > y2Zoom)) {
                g.drawRect(x2Zoom, y2Zoom, b, h);
            } else if ((x1Zoom > x2Zoom) && (y2Zoom > y1Zoom)) {
                g.drawRect(x2Zoom, y1Zoom, b, h);
            } else if ((x1Zoom < x2Zoom) && (y1Zoom > y2Zoom)) {
                g.drawRect(x1Zoom, y2Zoom, b, h);
            } else if ((x1Zoom < x2Zoom) && (y2Zoom > y1Zoom)) {
                g.drawRect(x1Zoom, y1Zoom, b, h);
            }
        }
        if ((mouseMode == GraferImplementation.MOUSEMODE_VALUE_DISPLAY_SLIDER) || xSliderActive) {
            g.setColor(Color.red);
            g.drawLine(xSliderPixels, X0yi, xSliderPixels, X0yi - hi);
            int x0 = X0xi + bi - 15, y0 = X0yi - hi + 12, dy = 15;
            g.setColor(Color.white);
            g.fillRect(x0, y0 - 12, 25, 12 + dy);
            g.setColor(Color.black);
            g.drawString("x = " + (int) xSliderValue[0], x0, y0);
            g.drawString("f = " + (int) xSliderValue[1], x0, y0 + dy);

            g.setColor(Color.blue);
            g.drawString("y = " + cf.formatT(yNewValue[0], TechFormat.FORMAT_AUTO), x0, y0 + 2 * dy);
        }
    }

    @Override
    public void setAxes() {
        //-------------------------------------
        this.setAxesCount(1, 1);
        this.setAxisWidthHeightX0Y0(new int[]{bi}, new int[]{hi}, new int[]{X0xi}, new int[]{X0yi}, new int[]{Y0xi}, new int[]{Y0yi});
        this.setAxisColor(new Color[]{Color.black}, new Color[]{Color.black});
        this.setAxesType(new int[]{ACHSE_LIN}, new int[]{ACHSE_LIN});
        this.setAxesLineStyle(new int[]{SOLID_PLAIN}, new int[]{SOLID_PLAIN});
        this.setAxesLabels(new String[]{""}, new String[]{""});  // braucht es, damit kein NullPointer-Error
        this.definiereGridNormalX(new int[]{0}, new int[]{0});
        this.definiereGridNormalY(new int[]{0}, new int[]{0});
        this.setGridLineStyle(new int[]{INVISIBLE}, new int[]{DOTTED_PLAIN}, new int[]{INVISIBLE}, new int[]{INVISIBLE});
        this.showGridLines(new int[][]{{0, 0}}, new int[][]{{0, 0}}, new int[][]{{0, 0}}, new int[][]{{0, 0}});
        this.setGridColors(new Color[]{Color.lightGray}, new Color[]{Color.lightGray}, new Color[]{Color.lightGray}, new Color[]{Color.lightGray});
        this.setTickCountMinor(new int[]{2}, new int[]{2});
        this.setTickLength(new int[]{4}, new int[]{4}, new int[]{0}, new int[]{0});
        this.setzeTickAusrichtung(new boolean[]{true}, new boolean[]{true});
        this.setTickLabelVisible(new boolean[]{true}, new boolean[]{true}, new boolean[]{false}, new boolean[]{false});
        this.setzeTickLabelPosition(new int[]{20}, new int[]{16});
        this.setzeTickLabelFont(new Font[]{new Font("Arial", Font.PLAIN, 12)}, new Font[]{new Font("Arial", Font.PLAIN, 12)});
        //=========================================
//        this.setzeAchsenBegrenzungen(new double[]{0.02}, new double[]{0.06}, new boolean[]{true}, new double[]{-4}, new double[]{4}, new boolean[]{true});
//        this.setzeTickSpacing(new double[]{0.01}, new double[]{2});
        double ymin = 0, ymax = -1;
        for (int n = nMin; n < nMin + cnSG.length; n++) {
            if (cnSG[n] > ymax) {
                ymax = cnSG[n];
            }
        }
        double[] empf = auto_Achsenbegrenzung_Wertempfehlung(ymin, ymax);
        while (empf[4] > 0.5 * (ymax - ymin)) {
            empf[4] *= 0.5;
        }
        //
        this.setzeAchsenBegrenzungen(new double[]{xNew[0]}, new double[]{xNew[xNew.length - 1]}, new boolean[]{true}, new double[]{ymin}, new double[]{empf[1]}, new boolean[]{true});
        this.setzeTickSpacing(new double[]{cnSG.length / 10.0}, new double[]{empf[4]});
        //-------------------------------------
    }

    @Override
    protected void setCurves() {
        //=========================================
        // anhand der Worksheet-Daten zu setzen -->
        //-------------------------------------
        this.setCurvesCount(1);
        this.setCurveAxesAssignment(new int[]{0}, new int[]{0});
        this.setCurveIndexWorksheetColumnsXY(new int[][]{{0, 1}});
        this.setCurvePointSymbolVisible(new boolean[]{false}, new int[]{1}, new int[]{SYBM_CIRCLE}, new Color[]{Color.black});
        this.setCurveClipping(
                new double[]{0}, new double[]{1}, new double[]{0}, new double[]{1},
                new int[]{CLIP_NO}, new int[]{CLIP_NO}, new int[]{CLIP_NO}, new int[]{CLIP_NO});
        this.setCurveLineStyle(new int[]{SOLID_PLAIN});
        this.setCurveColor(new Color[]{Color.blue});
        //-------------------------------------
    }

    @Override
    public void mouseEntered(final MouseEvent mouseEvent) {
        // no-op
    }

    @Override
    public void mouseExited(final MouseEvent mouseEvent) {
        // no-op
    }

    @Override
    public void mousePressed(final MouseEvent mouseEvent) {
        if (mouseMode == GraferImplementation.MOUSEMODE_ZOOM_WINDOW) {
            x1Zoom = mouseEvent.getX();
            y1Zoom = mouseEvent.getY();
            imDragModus = true;
        }
    }

    @Override
    public void mouseReleased(final MouseEvent mouseEvent) {
        //-------------------
        if (mouseMode == GraferImplementation.MOUSEMODE_ZOOM_WINDOW) {
            imDragModus = false;
            x2Zoom = mouseEvent.getX();
            y2Zoom = mouseEvent.getY();
            // Umrechnung der Zoom-Koordinaten von Pixelpunkten in Werte des Zomm-definierenden Rechtecks -->
            double[] xy1 = getValueFromPixel(x1Zoom, y1Zoom);
            double[] xy2 = getValueFromPixel(x2Zoom, y2Zoom);
            if (xy1[0] > xy2[0]) {  // flip x-values
                double q = xy1[0];
                xy1[0] = xy2[0];
                xy2[0] = q;
            }
            if (xy1[1] > xy2[1]) {  // flip y-values
                double q = xy1[1];
                xy1[1] = xy2[1];
                xy2[1] = q;
            }
            double[] empfX = new double[]{xy1[0], xy2[0], -1, -1, (0.2 * (xy2[0] - xy1[0]))};
            empfX[4] = Math.round(empfX[4]);
            if (empfX[4] < 1) {
                empfX[4] = 1;
            }
            double[] empfY = new double[]{xy1[1], xy2[1], -1, -1, (0.2 * (xy2[1] - xy1[1]))};
            while (empfY[4] > 0.5 * (xy2[1] - xy1[1])) {
                empfY[4] *= 0.5;
            }
            if (empfY[0] < 0) {
                empfY[0] = 0;  // damit keine negativen y-Werte
            }            // Achsen entsprechend neu setzen -->
            this.setzeAchsenBegrenzungen(
                    new double[]{empfX[0]}, new double[]{empfX[1]}, new boolean[]{true},
                    new double[]{empfY[0]}, new double[]{empfY[1]}, new boolean[]{true});
            this.setzeTickSpacing(new double[]{empfX[4]}, new double[]{empfY[4]});
            repaint();
            //-------------------
        }
    }

    @Override
    public void mouseClicked(final MouseEvent mouseEvent) {
        // no-op
    }

    @Override
    public void mouseMoved(final MouseEvent mouseEvent) {
        // no-op
    }

    @Override
    public void mouseDragged(final MouseEvent mouseEvent) {
        if (mouseMode == GraferImplementation.MOUSEMODE_NONE
                || mouseMode == GraferImplementation.MOUSEMODE_ZOOM_AUTOFIT) {
            return;
        }
        if (mouseMode == GraferImplementation.MOUSEMODE_ZOOM_WINDOW) {
            if (!imDragModus) {
                return;
            }
            int mx = mouseEvent.getX(), my = mouseEvent.getY();
            if (mx < X0xi) {
                mx = X0xi;
            }
            if (mx > X0xi + bi) {
                mx = X0xi + bi;
            }
            if (my > X0yi) {
                my = X0yi;
            }
            if (my < X0yi - hi) {
                my = X0yi - hi;
            }
            x2Zoom = mx;
            y2Zoom = my;
            repaint();
        } else if (xSliderActive) {
            xSliderPixels = mouseEvent.getX();
            if (xSliderPixels < X0xi) {
                xSliderPixels = X0xi;
            }
            if (xSliderPixels > X0xi + bi) {
                xSliderPixels = X0xi + bi;
            }
            xSliderValue[0] = Math.round(getValueFromPixel(xSliderPixels, 0)[0]);  // x-Wert der Schieber-Position, nur ganzzahlige n-Werte
            xSliderValue[1] = xSliderValue[0] * _baseFrequency;

            for (int i1 = 1; i1 < xNew.length; i1++) {
                if ((xNew[i1 - 1] <= xSliderValue[0]) && (xSliderValue[0] <= xNew[i1])) {
                    yNewValue[0] = yNew[i1];
                    break;
                }
            }
            repaint();
        }
        //-------------------
    }
    //================================================

    // Wenn mit der Maus in das Pixel-Feld geklickt wird -->
    private double[] getValueFromPixel(int xPix, int yPix) {
        //-------------------
        double achseXmin_ = -1, achseYmin_ = -1;
        int xAchseX_ = -1, yAchseY_ = -1;
        double sfX_ = -1, sfY_ = -1;
        int xAchseTyp_ = -1, yAchseTyp_ = -1;
        int indexDiagrammYachse = -1;
        for (int xAxisIndex : indexCurveAssociatedXAxis) {
            if ((_xAxisX[xAxisIndex] >= xGrfMIN[indexAngeklickterGraph])
                    && (_xAxisX[xAxisIndex] <= xGrfMAX[indexAngeklickterGraph])) {
                achseXmin_ = axisXmin[xAxisIndex];
                xAchseX_ = _xAxisX[xAxisIndex];
                sfX_ = sfX[xAxisIndex];
                xAchseTyp_ = xAxisType[xAxisIndex];
                break;
            }
        }
        for (int yAxisIndex : indexCurveAssociatedYAxis) {
            if ((_yAxisY[yAxisIndex] >= yGrfMIN[indexAngeklickterGraph])
                    && (_yAxisY[yAxisIndex] <= yGrfMAX[indexAngeklickterGraph])) {
                achseYmin_ = axisYmin[yAxisIndex];
                yAchseY_ = _yAxisY[yAxisIndex];
                sfY_ = sfY[yAxisIndex];
                yAchseTyp_ = yAxisType[yAxisIndex];
                indexDiagrammYachse = yAxisIndex;
                break;
            }
        }
        //-------------------
        double xWert = -1, yWert = -1;
        if (xAchseTyp_ == ACHSE_LOG) {
            xWert = achseXmin_ * Math.pow(10.0, (xPix - xAchseX_) / sfX_);
        } else if (xAchseTyp_ == ACHSE_LIN) {
            xWert = achseXmin_ + (xPix - xAchseX_) / sfX_;
        }
        if (yAchseTyp_ == ACHSE_LOG) {
            yWert = achseYmin_ * Math.pow(10.0, (yAchseY_ - yPix) / sfY_);
        } else if (yAchseTyp_ == ACHSE_LIN) {
            yWert = achseYmin_ + (yAchseY_ - yPix) / sfY_;
        }
        return new double[]{xWert, yWert, indexDiagrammYachse};
        //-------------------
    }
}