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

package ch.technokrat.gecko.geckocircuits.scope;

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class FourierDiagramm extends GraferV3 implements MouseListener, MouseMotionListener {

    //----------------------------
    private double[] cnSG;
    private int nMin;
    private double[] xNeu, yNeu;
    private int bi, hi, X0xi, X0yi, Y0xi, Y0yi;  // Hoehe, Breite, X-u-Y-Koord. des Achsenkreuzes (alles in Pix)
    //-----------------------
    private int mausModus = GraferImplementation.MAUSMODUS_NIX;
    private int x1Zoom, y1Zoom, x2Zoom, y2Zoom;
    private boolean imDragModus = false;
    // Bereichsgrenzen eines Diagramms bezueglich Maus-Klick:
    private int[] xGrfMIN, xGrfMAX, yGrfMIN, yGrfMAX;
    private int indexAngeklickterGraph = 0;
    //-----------------------
    private boolean xSchieberAktiv = false;
    private int xSchieberPix;
    private double[] xSchieberWert = new double[]{-1, -1};  // einem einzelnen Pixelpunkt sind eventuell mehrere Werte zugeordnet
    private double[] yNeuWert = new double[]{-1, -1};
    private TechFormat cf = new TechFormat();
    //-----------------------
    private final double _baseFrequency;

    public FourierDiagramm(double[] cnSG, int nMin, double baseFreqency) {
        bi = 350;
        hi = 300;
        X0xi = 60;
        X0yi = hi + 60;
        Y0xi = X0xi;
        Y0yi = X0yi;
        _baseFrequency = baseFreqency;
        
        this.setPreferredSize(new Dimension(bi + 2 * X0xi, X0yi + (X0yi - hi)));  // fuer pack() im uebergeordneten JFrame
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
        xNeu = new double[4 * cnSG.length];
        yNeu = new double[4 * cnSG.length];
        int i2 = 0;  // Zaehler in 'xNeu'
        double balkenbreite = 0.1;
        double deltaX = 1e-6;
        for (int i1 = 0; i1 < cnSG.length; i1++) {
            xNeu[i2] = (nMin + i1) - balkenbreite - deltaX;
            yNeu[i2] = 0;
            xNeu[i2 + 1] = (nMin + i1) - balkenbreite;
            yNeu[i2 + 1] = cnSG[i1];
            xNeu[i2 + 2] = (nMin + i1) + balkenbreite;
            yNeu[i2 + 2] = cnSG[i1];
            xNeu[i2 + 3] = (nMin + i1) + balkenbreite + deltaX;
            yNeu[i2 + 3] = 0;
            i2 += 4;
        }
        //-----------------------
        DataContainer daten = new DataContainerSimple(2, xNeu.length);
        for (int i1 = 0; i1 < xNeu.length; i1++) {
            daten.setValue(xNeu[i1], 0, i1);
            daten.setValue(yNeu[i1], 1, i1);
        }
        worksheetDaten = daten;
        //-----------------------
        this.setzeAchsen();
        this.setzeKurven();
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
        this.setzeAchsenBreiteHoeheX0Y0(new int[]{bi}, new int[]{hi}, new int[]{X0xi}, new int[]{X0yi}, new int[]{Y0xi}, new int[]{Y0yi});
        //---------------------------------------
        // Bereichsgrenzen fuers Maus-Klicken --> wird hier fuer 2 Diagramme definiert
        xGrfMIN = new int[]{0};
        xGrfMAX = new int[]{this.getWidth()};
        yGrfMIN = new int[]{0};
        yGrfMAX = new int[]{this.getHeight()};
        //---------------------------------------
    }

    public void setMausModus(int mausModus) {
        this.mausModus = mausModus;
        //---------
        if (mausModus == GraferImplementation.MAUSMODUS_NIX) {
            xSchieberAktiv = false;
            repaint();
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
            double ymin = 0, ymax = -1e99;
            for (int i1 = 0; i1 < yNeu.length; i1++) {
                if (yNeu[i1] > ymax) {
                    ymax = yNeu[i1];
                }
            }
            double[] empf = auto_Achsenbegrenzung_Wertempfehlung(ymin, ymax);
            this.setzeAchsenBegrenzungen(new double[]{xNeu[0]}, new double[]{xNeu[xNeu.length - 1]}, new boolean[]{true}, new double[]{ymin}, new double[]{empf[1]}, new boolean[]{true});
            this.setzeTickSpacing(new double[]{(cnSG.length / 10)}, new double[]{empf[4]});
            repaint();
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            //------------------------------------
            xSchieberAktiv = true;
            xSchieberPix = X0xi;  // x-Schieber wird an den Anfang gesetzt
        }
        //---------
    }

    // wird ueberschrieben, um Text dazuschreiben zu koennen -->
    protected void zeichne(Graphics g) {
        if ((mausModus == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) && (imDragModus)) {
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
        if ((mausModus == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) || (xSchieberAktiv)) {
            g.setColor(Color.red);
            g.drawLine(xSchieberPix, X0yi, xSchieberPix, X0yi - hi);
            int x0 = X0xi + bi - 15, y0 = X0yi - hi + 12, dy = 15;
            g.setColor(Color.white);
            g.fillRect(x0, y0 - 12, 25, 12 + dy);
            g.setColor(Color.black);
            g.drawString("x = " + (int) xSchieberWert[0], x0, y0);
            g.drawString("f = " + (int) xSchieberWert[1], x0, y0 + dy);
            
            g.setColor(Color.blue);
            g.drawString("y = " + cf.formatT(yNeuWert[0], TechFormat.FORMAT_AUTO), x0, y0 + 2 * dy);
        }
    }

    @Override
    public void setzeAchsen() {
        //-------------------------------------
        this.setzeAchsenAnzahl(1, 1);
        this.setzeAchsenBreiteHoeheX0Y0(new int[]{bi}, new int[]{hi}, new int[]{X0xi}, new int[]{X0yi}, new int[]{Y0xi}, new int[]{Y0yi});
        this.setzeAchsenFarbe(new Color[]{Color.black}, new Color[]{Color.black});
        this.setzeAchsenTyp(new int[]{ACHSE_LIN}, new int[]{ACHSE_LIN});
        this.setzeAchsenLinienStil(new int[]{SOLID_PLAIN}, new int[]{SOLID_PLAIN});
        this.setzeAchsenBeschriftungen(new String[]{""}, new String[]{""});  // braucht es, damit kein NullPointer-Error
        this.definiereGridNormalX(new int[]{0}, new int[]{0});
        this.definiereGridNormalY(new int[]{0}, new int[]{0});
        this.setzeGridLinienStil(new int[]{INVISIBLE}, new int[]{DOTTED_PLAIN}, new int[]{INVISIBLE}, new int[]{INVISIBLE});
        this.showGridLines(new int[][]{{0, 0}}, new int[][]{{0, 0}}, new int[][]{{0, 0}}, new int[][]{{0, 0}});
        this.setzeGridFarben(new Color[]{Color.lightGray}, new Color[]{Color.lightGray}, new Color[]{Color.lightGray}, new Color[]{Color.lightGray});
        this.setzeTickAnzMinor(new int[]{2}, new int[]{2});
        this.setzeTickLaenge(new int[]{4}, new int[]{4}, new int[]{0}, new int[]{0});
        this.setzeTickAusrichtung(new boolean[]{true}, new boolean[]{true});
        this.setzeTickLabelAnzeige(new boolean[]{true}, new boolean[]{true}, new boolean[]{false}, new boolean[]{false});
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
        this.setzeAchsenBegrenzungen(new double[]{xNeu[0]}, new double[]{xNeu[xNeu.length - 1]}, new boolean[]{true}, new double[]{ymin}, new double[]{empf[1]}, new boolean[]{true});
        this.setzeTickSpacing(new double[]{cnSG.length / 10}, new double[]{empf[4]});
        //-------------------------------------
    }

    @Override
    protected void setzeKurven() {
        //=========================================
        // anhand der Worksheet-Daten zu setzen -->
        //------------------------------------- 
        this.setzeKurvenAnzahl(1);
        this.setzeZugehoerigkeitKurveAchsen(new int[]{0}, new int[]{0});
        this.setzeKurveIndexWorksheetKolonnenXY(new int[][]{{0, 1}});
        this.setzeKurvePunktSymbolAnzeigen(new boolean[]{false}, new int[]{1}, new int[]{SYBM_CIRCLE}, new Color[]{Color.black});
        this.setzeKurveClipping(
                new double[]{0}, new double[]{1}, new double[]{0}, new double[]{1},
                new int[]{CLIP_NO}, new int[]{CLIP_NO}, new int[]{CLIP_NO}, new int[]{CLIP_NO});
        this.setzeKurveLinienstil(new int[]{SOLID_PLAIN});
        this.setzeKurveFarbe(new Color[]{Color.blue});
        //-------------------------------------
    }

    @Override
    public void mouseEntered(final MouseEvent mouseEvent) {
    }

    @Override
    public void mouseExited(final MouseEvent mouseEvent) {
    }

    @Override
    public void mousePressed(final MouseEvent mouseEvent) {
        if (mausModus == GraferImplementation.MAUSMODUS_NIX) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
            x1Zoom = mouseEvent.getX();
            y1Zoom = mouseEvent.getY();
            imDragModus = true;
        } else if (mausModus == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) {
        }
    }

    @Override
    public void mouseReleased(final MouseEvent mouseEvent) {
        //-------------------
        if (mausModus == GraferImplementation.MAUSMODUS_NIX) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
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
            }            //
            // Achsen entsprechend neu setzen -->
            this.setzeAchsenBegrenzungen(
                    new double[]{empfX[0]}, new double[]{empfX[1]}, new boolean[]{true},
                    new double[]{empfY[0]}, new double[]{empfY[1]}, new boolean[]{true});
            this.setzeTickSpacing(new double[]{empfX[4]}, new double[]{empfY[4]});
            repaint();
            //-------------------
        } else if (mausModus == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) {
        }
    }

    @Override
    public void mouseClicked(final MouseEvent mouseEvent) {
    }

    @Override
    public void mouseMoved(final MouseEvent mouseEvent) {
    }

    @Override
    public void mouseDragged(final MouseEvent mouseEvent) {
        if (mausModus == GraferImplementation.MAUSMODUS_NIX) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
        } else if (mausModus == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
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
        } else if (xSchieberAktiv) {
            xSchieberPix = mouseEvent.getX();
            if (xSchieberPix < X0xi) {
                xSchieberPix = X0xi;
            }
            if (xSchieberPix > X0xi + bi) {
                xSchieberPix = X0xi + bi;
            }
            xSchieberWert[0] = Math.round(getValueFromPixel(xSchieberPix, 0)[0]);  // x-Wert der Schieber-Position, nur ganzzahlige n-Werte
            xSchieberWert[1] = xSchieberWert[0] * _baseFrequency;
            
            for (int i1 = 1; i1 < xNeu.length; i1++) {
                if ((xNeu[i1 - 1] <= xSchieberWert[0]) && (xSchieberWert[0] <= xNeu[i1])) {
                    yNeuWert[0] = yNeu[i1];
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
        for (int i1 = 0; i1 < indexZurKurveGehoerigeXachse.length; i1++) {
            if ((_xAchseX[indexZurKurveGehoerigeXachse[i1]] >= xGrfMIN[indexAngeklickterGraph])
                    && (_xAchseX[indexZurKurveGehoerigeXachse[i1]] <= xGrfMAX[indexAngeklickterGraph])) {
                achseXmin_ = achseXmin[indexZurKurveGehoerigeXachse[i1]];
                xAchseX_ = _xAchseX[indexZurKurveGehoerigeXachse[i1]];
                sfX_ = sfX[indexZurKurveGehoerigeXachse[i1]];
                xAchseTyp_ = xAchseTyp[indexZurKurveGehoerigeXachse[i1]];
                break;
            }
        }
        for (int i1 = 0; i1 < indexZurKurveGehoerigeYachse.length; i1++) {
            if ((_yAchseY[indexZurKurveGehoerigeYachse[i1]] >= yGrfMIN[indexAngeklickterGraph])
                    && (_yAchseY[indexZurKurveGehoerigeYachse[i1]] <= yGrfMAX[indexAngeklickterGraph])) {
                achseYmin_ = achseYmin[indexZurKurveGehoerigeYachse[i1]];
                yAchseY_ = _yAchseY[indexZurKurveGehoerigeYachse[i1]];
                sfY_ = sfY[indexZurKurveGehoerigeYachse[i1]];
                yAchseTyp_ = yAchseTyp[indexZurKurveGehoerigeYachse[i1]];
                indexDiagrammYachse = indexZurKurveGehoerigeYachse[i1];
                break;
            }
        }
        //-------------------
        double xWert = -1, yWert = -1;
        if (xAchseTyp_ == ACHSE_LOG) {
            xWert = achseXmin_ * Math.pow(10.0, ((xPix - xAchseX_) / sfX_));
        } else if (xAchseTyp_ == ACHSE_LIN) {
            xWert = achseXmin_ + (xPix - xAchseX_) / sfX_;
        }
        if (yAchseTyp_ == ACHSE_LOG) {
            yWert = achseYmin_ * Math.pow(10.0, ((yAchseY_ - yPix) / sfY_));
        } else if (yAchseTyp_ == ACHSE_LIN) {
            yWert = achseYmin_ + (yAchseY_ - yPix) / sfY_;
        }
        return new double[]{xWert, yWert, indexDiagrammYachse};
        //-------------------
    }

    // Ermittle (x/y)-Wert in Pixel zu einem Wertepaar -->
    private int[] getPixelFromValue(double xWert, double yWert, int index_xAchse, int index_yAchse) {
        //-------------------
        double achseXmin_ = achseXmin[index_xAchse];
        int xAchseX_ = _xAchseX[index_xAchse];
        double sfX_ = sfX[index_xAchse];
        int xAchseTyp_ = xAchseTyp[index_xAchse];
        double achseYmin_ = achseYmin[index_yAchse];
        int yAchseY_ = _yAchseY[index_yAchse];
        double sfY_ = sfY[index_yAchse];
        int yAchseTyp_ = yAchseTyp[index_yAchse];
        //-------------------
        int xPix = -1, yPix = -1;
        if (xAchseTyp_ == ACHSE_LOG) {
            xWert = achseXmin_ * Math.pow(10.0, ((xPix - xAchseX_) / sfX_));
        } else if (xAchseTyp_ == ACHSE_LIN) {
            xWert = achseXmin_ + (xPix - xAchseX_) / sfX_;
        }
        if (yAchseTyp_ == ACHSE_LOG) {
            yWert = achseYmin_ * Math.pow(10.0, ((yAchseY_ - yPix) / sfY_));
        } else if (yAchseTyp_ == ACHSE_LIN) {
            yWert = achseYmin_ + (yAchseY_ - yPix) / sfY_;
        }
        return new int[]{xPix, yPix};
        //-------------------
    }
}