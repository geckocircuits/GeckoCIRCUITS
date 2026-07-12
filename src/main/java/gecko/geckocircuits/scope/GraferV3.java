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

import gecko.geckocircuits.general.TechFormat;
import gecko.geckocircuits.newscope.GeckoGraphics2D;
import java.awt.AlphaComposite;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.Font;
import java.awt.Color;
import java.awt.RenderingHints;
import java.util.Arrays;
import javax.swing.JPanel;
import java.text.NumberFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/*
 * this is the "old scope", in future, replace with "newScope"
 */
@SuppressWarnings("serial")
@Deprecated
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Legacy scope class stores worksheet data for graph visualization")
public class GraferV3 extends JPanel {

    //-------------------------------------
    // Konstanten:
    public static final int AUTO = -111111111;
    public static final int DEAKTIVIERT = -111111112;
    public static final int ACHSE_LIN = -111111114;
    public static final int ACHSE_LOG = -111111115;
    //
    public static final String[] CLIPPING = new String[]{"AXIS", "DATA", "VALUE"};
    public static final int CLIP_ACHSE = -111111116;
    public static final int CLIP_NO = -111111117;
    public static final int CLIP_VALUE = -111111118;
    //
    public static final String[] LINIEN_STIL = /*
             * TxtI.ti_linStil_GraferV3;
             */ new String[]{"SOLID_PLAIN", "INVISIBLE", "SOLID_FAT_1", "SOLID_FAT_2", "DOTTED_PLAIN", "DOTTED_FAT"};
    public static final int SOLID_PLAIN = -3333330;
    public static final int INVISIBLE = -3333331;
    public static final int SOLID_FAT_1 = -3333332;
    public static final int SOLID_FAT_2 = -3333333;
    public static final int DOTTED_PLAIN = -3333334;
    public static final int DOTTED_FAT = -3333335;
    //
    public static final Stroke str_SOLID_PLAIN = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);
    public static final Stroke str_INVISIBLE = new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);  // eigentlich unsichtbar
    public static final Stroke str_SOLID_FAT_1 = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);
    public static final Stroke str_SOLID_FAT_2 = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);
    public static final Stroke str_DOTTED_PLAIN = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f, new float[]{4, 4}, 0);
    public static final Stroke str_DOTTED_FAT = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f, new float[]{4, 4}, 0);
    //
    static final String[] FARBEN = /*  // MS_PKGPROTECT: only used within package
             * TxtI.ti_farbe_GraferV3;
             */ new String[]{"black", "red", "green", "blue", "darkgray", "gray", "ligthgray", "white", "magenta", "cyan", "orange", "yellow", "darkgreen"};
    public static final int BLACK = -3444440;
    public static final int RED = -3444441;
    public static final int GREEN = -3444442;
    public static final int BLUE = -3444443;
    public static final int DARKGRAY = -3444444;
    public static final int GRAY = -3444445;
    public static final int LIGTHGRAY = -3444446;
    public static final int WHITE = -3444447;
    public static final int MAGENTA = -3444448;
    public static final int CYAN = -3444449;
    public static final int ORANGE = -3444450;
    public static final int YELLOW = -3444451;
    public static final int DARKGREEN = -3444452;
    //
    public static final String[] SYMBOLSHAPE = /*
             * TxtI.ti_formSymb_GraferV3;
             */ new String[]{"CIRCLE", "CIRCLE_FILLED", "CROSS", "RECT", "RECT_FILLED", "TRIANG", "TRIANG_FILLED"};
    public static final int SYBM_CIRCLE = -838300;
    public static final int SYBM_CIRCLE_FILLED = -838301;
    public static final int SYBM_CROSS = -838302;
    public static final int SYBM_RECT = -838303;
    public static final int SYBM_RECT_FILLED = -838304;
    public static final int SYBM_TRIANG = -838305;
    public static final int SYBM_TRIANG_FILLED = -838306;
    //-------------------------------------
    // (1) Achsen:
    protected int numAxesX, numAxesY;  // Anzahl der x- u. y-Achsen
    protected int[] widthPix, heightPix;  // Breite und Hoehe der jeweiligen Diagramm-Achsen
    protected int[] _xAxisX, _xAxisY, _yAxisX, _yAxisY;  // Achsen-Koordinaten
    protected int[] xAxisType, yAxisType;  // Achse linear oder logarithmisch
    protected Color[] colorAxesX, colorAxesY;  // Farben der einzelnen Achsen, zB. unsichtbar --> weiss
    protected int[] lineStyleAxesX, lineStyleAxesY;  // durchgezogen, gepunktet, fett(???)
    //
    protected double[] axisXmin, axisXmax, axisYmin, axisYmax;  // min- u. max-Zahlenwerte
    protected boolean[] autoAxisXmin, autoAxisXmax, autoAxisYmin, autoAxisYmax;  // sind diese Werte auf AUTO gesetzt??
    protected String[] xAxisLabel, yAxisLabel;
    //-------------
    protected Color[] colorGridNormalX, colorGridNormalY, colorGridNormalXminor, colorGridNormalYminor;
    protected int[] lineStyleGridNormalX, lineStyleGridNormalY, lineStyleGridNormalXminor, lineStyleGridNormalYminor;
    protected int[] gridNormalX_associatedXAxis, gridNormalX_associatedYAxis;
    protected int[] gridNormalY_associatedXAxis, gridNormalY_associatedYAxis;
    protected int[][] showGridNormalXmajor, showGridNormalXminor, showGridNormalYmajor, showGridNormalYminor;  // welche Grid-Linien sollen dargestellt werden?
    // --> zB. showGridNormalXmajor[i1]= {index_xAchse, index_yAchse}
    //-------------
    protected boolean[] xTickAutoSpacing, yTickAutoSpacing;  // should the tick spacing be determined automatically?
    protected double[] xTickSpacing, yTickSpacing;  // Distance between 2 ticks, starting from zero
    protected int[] xNumTicksMinor, yNumTicksMinor;  // Zahl der Minor-Ticks zwischen zwei regulaeren Ticks
    protected int[] xTickLength, yTickLength, xTickLengthMinor, yTickLengthMinor;
    protected boolean[] showXTicksBottom, showYTicksLeft;  // Ticks auf der x-Achse koennen nach unten oder nach oben zeigen, analog die der y-Achse
    //
    protected boolean[] showLabelsXmaj, showLabelsXmin, showLabelsYmaj, showLabelsYmin;  // sollen die entsprechenden Labels bei den jeweiligen Ticks angezeigt werden?
    protected int[] posXtickLabels, posYtickLabels;  // how far away are the tick labels from each axis?
    protected Font[] foTickLabelX, foTickLabelY;
    //-------------
    protected int[][] tickX, tickY, tickXminor, tickYminor;  // Pixel point position of the ticks
    protected double[][] valueTickX, valueTickY, valueTickXminor, valueTickYminor;  // Zahlenwerte der einzelnen Ticks
    protected double[] sfX, sfY;  // Stretch factors for converting pixels <--> values
    //-------------------------------------
    // (2) Kurven:
    protected DataContainer worksheetData;  // hier stehen die Punkte aller Kurven drinnen  --> derzeit nur EIN Worksheet implementiert
    //-------------
    protected int[] indexCurveAssociatedXAxis, indexCurveAssociatedYAxis;  // Zuordnung Kurve <--> Achsen
    protected int numCurves;  // alle in diesem JPanel gezeichneten Kurven
    protected int[][] curve_index_worksheetColumns_XY;  // int[][]{{index_x_kolonne,index_y_kolonne}}
    protected boolean[] showCurvePointSymbol;  // Sollen die Punkte der Kurve als Punkte dargestellt werden
    protected int[] crvSymbFrequ, crvSymbShape;     // Details about drawing the symbols on the curve data points
    protected Color[] crvSymbFarbe;                 // Details about drawing the symbols on the curve data points
    protected double[] curveClipping_xmin, curveClipping_xmax, curveClipping_ymin, curveClipping_ymax;  // definierter Zahlenwert fuers Clipping
    protected int[] clipXmin, clipXmax, clipYmin, clipYmax;  // Art des Cilpping --> "AXIS", "NO CLIP", "VALUE"
    protected int[] curveLineStyle;
    protected Color[] curveColor;
    //-------------------------------------
    // sonstiges:
    protected NumberFormat nf = NumberFormat.getNumberInstance();
    protected TechFormat tcf = new TechFormat();
    protected int digitsX = 3, digitsY = 3;  // Decimal places in the tick label
    //-------------------------------------
    //-------------------------------------
    // speziell (eigentlich gepfuscht):
    // Unterscheidung ZV - Signal -->
    protected int[] curveTypeZVvsSIGNAL;  // wird direkt ohne 'get'-Fkt. von der abgeleiteten Klasse angeprochen
    //-------------------------------------
    public boolean _antialiasing = true;
    private double[] kurveTransparenz;

    public boolean loadWorksheetData(DataContainer data) {
        //-----------------------
        this.worksheetData = data;
        // Daten-Konsistenz (grob) pruefen:
        if (data.getRowLength() < 2) {
            return false;  // only one column --> at least 2 required for y=y(x) - curve
        }

        this.autoSettingsAnpassen();  // macht erst Sinn, wenn die Kurven-Daten da sind!
        return true;
        //-----------------------
    }

    //-------------------------------------
    // (1) setze Achsen:
    //
    public void setAxesCount(int anzX, int anzY) {
        this.numAxesX = anzX;
        this.numAxesY = anzY;
    }

    public void setAxisWidthHeightX0Y0(int[] b, int[] h, int[] xX, int[] yX, int[] xY, int[] yY) {
        this.widthPix = Arrays.copyOf(b, b.length);
        this.heightPix = Arrays.copyOf(h, h.length);
        this._xAxisX = Arrays.copyOf(xX, xX.length);
        this._xAxisY = Arrays.copyOf(yX, yX.length);
        this._yAxisX = Arrays.copyOf(xY, xY.length);
        this._yAxisY = Arrays.copyOf(yY, yY.length);
    }

    public void setzeAchsenBegrenzungen(double[] xMin, double[] xMax, boolean[] autoScaleX, double[] yMin, double[] yMax, boolean[] autoScaleY) {
        this.axisXmin = Arrays.copyOf(xMin, xMin.length);
        this.autoAxisXmin = Arrays.copyOf(autoScaleX, autoScaleX.length);
        this.axisXmax = Arrays.copyOf(xMax, xMax.length);
        this.autoAxisXmax = Arrays.copyOf(autoScaleX, autoScaleX.length);
        this.axisYmin = Arrays.copyOf(yMin, yMin.length);
        this.autoAxisYmin = Arrays.copyOf(autoScaleY, autoScaleY.length);
        this.axisYmax = Arrays.copyOf(yMax, yMax.length);
        this.autoAxisYmax = Arrays.copyOf(autoScaleY, autoScaleY.length);
    }

    public void setAxesType(int[] x, int[] y) {
        this.xAxisType = Arrays.copyOf(x, x.length);
        this.yAxisType = Arrays.copyOf(y, y.length);
    }

    public void setAxisColor(Color[] fX, Color[] fY) {
        this.colorAxesX = Arrays.copyOf(fX, fX.length);
        this.colorAxesY = Arrays.copyOf(fY, fY.length);
    }

    public void setAxesLineStyle(int[] stilX, int[] stilY) {
        this.lineStyleAxesX = Arrays.copyOf(stilX, stilX.length);
        this.lineStyleAxesY = Arrays.copyOf(stilY, stilY.length);
    }
    //-----------

    public void definiereGridNormalX(int[] zugeordneteXAchse, int[] zugeordneteYAchse) {
        this.gridNormalX_associatedXAxis = Arrays.copyOf(zugeordneteXAchse, zugeordneteXAchse.length);
        this.gridNormalX_associatedYAxis = Arrays.copyOf(zugeordneteYAchse, zugeordneteYAchse.length);
        if (zugeordneteXAchse.length != zugeordneteYAchse.length) {
            System.out.println("Fehler 45763425n");
        }
    }

    public void definiereGridNormalY(int[] zugeordneteXAchse, int[] zugeordneteYAchse) {
        this.gridNormalY_associatedXAxis = Arrays.copyOf(zugeordneteXAchse, zugeordneteXAchse.length);
        this.gridNormalY_associatedYAxis = Arrays.copyOf(zugeordneteYAchse, zugeordneteYAchse.length);
        if (zugeordneteXAchse.length != zugeordneteYAchse.length) {
            System.out.println("Fehler 908hj4gw4");
        }
    }

    public void setGridColors(Color[] colorGridNormalX, Color[] colorGridNormalY, Color[] colorGridNormalXminor, Color[] colorGridNormalYminor) {
        this.colorGridNormalX = Arrays.copyOf(colorGridNormalX, colorGridNormalX.length);
        this.colorGridNormalY = Arrays.copyOf(colorGridNormalY, colorGridNormalY.length);
        this.colorGridNormalXminor = Arrays.copyOf(colorGridNormalXminor, colorGridNormalXminor.length);
        this.colorGridNormalYminor = Arrays.copyOf(colorGridNormalYminor, colorGridNormalYminor.length);
    }

    public void setGridLineStyle(int[] lineStyleGridNormalX, int[] lineStyleGridNormalY, int[] lineStyleGridNormalXminor, int[] lineStyleGridNormalYminor) {
        this.lineStyleGridNormalX = Arrays.copyOf(lineStyleGridNormalX, lineStyleGridNormalX.length);
        this.lineStyleGridNormalY = Arrays.copyOf(lineStyleGridNormalY, lineStyleGridNormalY.length);
        this.lineStyleGridNormalXminor = Arrays.copyOf(lineStyleGridNormalXminor, lineStyleGridNormalXminor.length);
        this.lineStyleGridNormalYminor = Arrays.copyOf(lineStyleGridNormalYminor, lineStyleGridNormalYminor.length);
    }

    public void showGridLines(int[][] showGridNormalXmajor, int[][] showGridNormalXminor, int[][] showGridNormalYmajor, int[][] showGridNormalYminor) {
        this.showGridNormalXmajor = deepCopy2D(showGridNormalXmajor);
        this.showGridNormalXminor = deepCopy2D(showGridNormalXminor);
        this.showGridNormalYmajor = deepCopy2D(showGridNormalYmajor);
        this.showGridNormalYminor = deepCopy2D(showGridNormalYminor);
    }

    private static int[][] deepCopy2D(int[][] src) {
        if (src == null) {
            return new int[0][];
        }
        int[][] copy = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }
    //-----------

    public void setzeTickAutoSpacing(boolean[] xTickAutoSpacing, boolean[] yTickAutoSpacing) {
        this.xTickAutoSpacing = Arrays.copyOf(xTickAutoSpacing, xTickAutoSpacing.length);
        this.yTickAutoSpacing = Arrays.copyOf(yTickAutoSpacing, yTickAutoSpacing.length);
    }

    public void setzeTickSpacing(double[] x, double[] y) {
        this.xTickSpacing = Arrays.copyOf(x, x.length);
        this.yTickSpacing = Arrays.copyOf(y, y.length);
    }

    public void setTickCountMinor(int[] x, int[] y) {
        this.xNumTicksMinor = Arrays.copyOf(x, x.length);
        this.yNumTicksMinor = Arrays.copyOf(y, y.length);
    }

    public void setTickLabelVisible(boolean[] xMaj, boolean[] yMaj, boolean[] xMin, boolean[] yMin) {
        this.showLabelsXmaj = Arrays.copyOf(xMaj, xMaj.length);
        this.showLabelsYmaj = Arrays.copyOf(yMaj, yMaj.length);
        this.showLabelsXmin = Arrays.copyOf(xMin, xMin.length);
        this.showLabelsYmin = Arrays.copyOf(yMin, yMin.length);
    }

    public void setTickLength(int[] x, int[] y, int[] xMinor, int[] yMinor) {
        this.xTickLength = Arrays.copyOf(x, x.length);
        this.yTickLength = Arrays.copyOf(y, y.length);
        this.xTickLengthMinor = Arrays.copyOf(xMinor, xMinor.length);
        this.yTickLengthMinor = Arrays.copyOf(yMinor, yMinor.length);
    }
    // - - - - - - - - - - - - -

    public void setzeTickAusrichtung(boolean[] x, boolean[] y) {
        this.showXTicksBottom = Arrays.copyOf(x, x.length);
        this.showYTicksLeft = Arrays.copyOf(y, y.length);
    }

    public void setzeTickLabelPosition(int[] x, int[] y) {
        this.posXtickLabels = Arrays.copyOf(x, x.length);
        this.posYtickLabels = Arrays.copyOf(y, y.length);
    }

    public void setzeTickLabelFont(Font[] foX, Font[] foY) {
        this.foTickLabelX = Arrays.copyOf(foX, foX.length);
        this.foTickLabelY = Arrays.copyOf(foY, foY.length);
    }

    public void setAxesLabels(String[] x, String[] y) {
        this.xAxisLabel = Arrays.copyOf(x, x.length);
        this.yAxisLabel = Arrays.copyOf(y, y.length);
    }
    //-------------------------------------
    // (2) setze Kurven:
    //

    public void setCurvesCount(int anz) {
        this.numCurves = anz;
    }

    public void setCurveAxesAssignment(int[] indexXachse, int[] indexYachse) {
        this.indexCurveAssociatedXAxis = Arrays.copyOf(indexXachse, indexXachse.length);
        this.indexCurveAssociatedYAxis = Arrays.copyOf(indexYachse, indexYachse.length);
    }

    public void setCurveIndexWorksheetColumnsXY(int[][] iwkXY) {
        this.curve_index_worksheetColumns_XY = deepCopy2D(iwkXY);
    }

    public void setCurvePointSymbolVisible(boolean[] sym, int[] crvSymbFrequ, int[] crvSymbShape, Color[] crvSymbFarbe) {
        this.showCurvePointSymbol = Arrays.copyOf(sym, sym.length);
        this.crvSymbFrequ = Arrays.copyOf(crvSymbFrequ, crvSymbFrequ.length);
        this.crvSymbShape = Arrays.copyOf(crvSymbShape, crvSymbShape.length);
        this.crvSymbFarbe = Arrays.copyOf(crvSymbFarbe, crvSymbFarbe.length);
    }

    public void setCurveClipping(double[] xmin, double[] xmax, double[] ymin, double[] ymax, int[] clipXmin, int[] clipXmax, int[] clipYmin, int[] clipYmax) {
        this.curveClipping_xmin = Arrays.copyOf(xmin, xmin.length);
        this.curveClipping_xmax = Arrays.copyOf(xmax, xmax.length);
        this.curveClipping_ymin = Arrays.copyOf(ymin, ymin.length);
        this.curveClipping_ymax = Arrays.copyOf(ymax, ymax.length);  // eventuell manuell definierte konkrete Zahlenwerte
        this.clipXmin = Arrays.copyOf(clipXmin, clipXmin.length);
        this.clipXmax = Arrays.copyOf(clipXmax, clipXmax.length);
        this.clipYmin = Arrays.copyOf(clipYmin, clipYmin.length);
        this.clipYmax = Arrays.copyOf(clipYmax, clipYmax.length);         // Art des Cilpping --> "AXIS", "NO CLIP", "VALUE"
    }

    public void setCurveLineStyle(int[] curveLineStyle) {
        this.curveLineStyle = Arrays.copyOf(curveLineStyle, curveLineStyle.length);
    }

    public void setCurveColor(Color[] f) {
        this.curveColor = Arrays.copyOf(f, f.length);
    }

    public void setCurveTransparency(double[] trans) {
        this.kurveTransparenz = Arrays.copyOf(trans, trans.length);
    }
    //-------------------------------------

    public void autoSettingsAnpassen() {
        // AUTO-Settings koennen erst abschliessend erfolgen, wenn alle relevanten Grafer-Daten vorhanden sind
        // --> expliziter Aufruf
        this.auto_BereichsgrenzenDerAchsen();  // falls "AUTO"-Grenze --> automatisch setzen
    }
    //-------------------------------------

    public void setAxes() {
        // no-op
    }

    protected void setCurves() {
        // no-op
    }


    @Override
    public void paint(Graphics g) {

        if (_antialiasing) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        //--------------------------
        g.setColor(Color.white);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());  // --> weisser Hintergrund
        //--------------------------
        try {
            this.drawCoordinateAxes(g);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e + " in drawCoordinateAxes()");
        }
        try {
            this.drawCurves(g);
        } catch (Exception e) {
            System.out.println(e + " in drawCurves()");
            e.printStackTrace();
        }
        try {
            this.draw(g);
        } catch (Exception e) {
            System.out.println(e + " in draw()");
        }
        //--------------------------
    }

    // zum Ueberschreiben
    protected void draw(Graphics g) {
        // no-op
    }

    public static double lg10(double x) {
        return Math.log(x) / Math.log(10.0);
    }

    protected int calculateXPixLinear(double wert, int index) {
        return _xAxisX[index] + (int) (sfX[index] * (wert - axisXmin[index]));
    }

    protected int calculateXPixLogarithmic(double wert, int index) {
        return _xAxisX[index] + (int) (sfX[index] * this.lg10(wert / axisXmin[index]));
    }

    protected int calculateYPixLinear(double wert, int index) {
        return _yAxisY[index] - (int) (sfY[index] * (wert - axisYmin[index]));
    }

    protected int calculateYPixLogarithmic(double wert, int index) {
        return _yAxisY[index] - (int) (sfY[index] * this.lg10(wert / axisYmin[index]));
    }

    protected void drawCurves(Graphics g) {
        if (worksheetData == null) {
            return;
        }
        GeckoGraphics2D g2 = new GeckoGraphics2D((Graphics2D) g);
        for (int i1 = 0; i1 < numCurves; i1++) {
            this.drawSingleCurve(g2, i1, worksheetData.getColumnLength());
        }
    }

    protected void drawCoordinateAxes(Graphics g) {
        GeneralPath grL = new GeneralPath();
        Graphics2D g2 = (Graphics2D) g;
        //===============================================
        valueTickX = new double[numAxesX][];
        tickX = new int[numAxesX][];
        valueTickXminor = new double[numAxesX][];
        tickXminor = new int[numAxesX][];
        valueTickY = new double[numAxesY][];
        tickY = new int[numAxesY][];
        valueTickYminor = new double[numAxesY][];
        tickYminor = new int[numAxesY][];
        //
        sfX = new double[numAxesX];
        sfY = new double[numAxesY];
        //===============================================
        // x-Achsen:
        for (int i1 = 0; i1 < numAxesX; i1++) {
            this.drawSingleCoordinateAxisX(g2, i1);
        }
        // y-Achsen:
        for (int i1 = 0; i1 < numAxesY; i1++) {
            this.drawSingleCoordinateAxisY(g2, i1);
        }
        //===============================================
        this.drawGridNormalX(g);
        this.drawGridNormalY(g);
        //===============================================
        // ueberdeckt werden
        // die Methoden 'this.drawGridNormalX(g)' und 'this.drawGridNormalY(g)' koennen nicht vor die Schleife zum Zeichnen der Achsen
        // gestellt werden, weil in 'this.drawSingleCoordinateAxisX(g2,i1)' und 'this.drawSingleCoordinateAxisY(g2,i1)' zuerst einmal
        // der Grid berechnet werden muss
        //
        for (int i1 = 0; i1 < numAxesX; i1++) {
            if (lineStyleAxesX[i1] == INVISIBLE) {
                continue;
            }
            g2.setColor(colorAxesX[i1]);
            if (lineStyleAxesX[i1] == SOLID_PLAIN) {
                g2.setStroke(str_SOLID_PLAIN);
            } else if (lineStyleAxesX[i1] == SOLID_FAT_1) {
                g2.setStroke(str_SOLID_FAT_1);
            } else if (lineStyleAxesX[i1] == SOLID_FAT_2) {
                g2.setStroke(str_SOLID_FAT_2);
            } else if (lineStyleAxesX[i1] == DOTTED_PLAIN) {
                g2.setStroke(str_DOTTED_PLAIN);
            } else if (lineStyleAxesX[i1] == DOTTED_FAT) {
                g2.setStroke(str_DOTTED_FAT);
            }
            //-----------------------
            // jetzt die Linie ziehen:
            grL.reset();
            grL.moveTo(_xAxisX[i1], _xAxisY[i1]);
            grL.lineTo(_xAxisX[i1] + widthPix[i1], _xAxisY[i1]);
            g2.draw(grL);
            g2.drawString(xAxisLabel[i1], _xAxisX[i1] + widthPix[i1] / 2, _xAxisY[i1] + posXtickLabels[i1]);
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        for (int i1 = 0; i1 < numAxesY; i1++) {
            if (lineStyleAxesY[i1] == INVISIBLE) {
                continue;
            }
            g2.setColor(colorAxesY[i1]);
            if (lineStyleAxesY[i1] == SOLID_PLAIN) {
                g2.setStroke(str_SOLID_PLAIN);
            } else if (lineStyleAxesY[i1] == SOLID_FAT_1) {
                g2.setStroke(str_SOLID_FAT_1);
            } else if (lineStyleAxesY[i1] == SOLID_FAT_2) {
                g2.setStroke(str_SOLID_FAT_2);
            } else if (lineStyleAxesY[i1] == DOTTED_PLAIN) {
                g2.setStroke(str_DOTTED_PLAIN);
            } else if (lineStyleAxesY[i1] == DOTTED_FAT) {
                g2.setStroke(str_DOTTED_FAT);
            }
            //-----------------------
            //==================================
            grL.reset();
            grL.moveTo(_yAxisX[i1], _yAxisY[i1]);
            grL.lineTo(_yAxisX[i1], _yAxisY[i1] - heightPix[i1]);
            g2.draw(grL);
            g2.drawString(yAxisLabel[i1], _yAxisX[i1] - posYtickLabels[i1], _yAxisY[i1] - heightPix[i1] / 2);
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        //==================================
    }

    protected void drawSingleCurve(Graphics2D g2, int i1, int anzKurvenpunkteImWorksheet) {


        if (kurveTransparenz != null) {
            float transparenz = (float) kurveTransparenz[i1];
            if (transparenz < 0.1) {
                transparenz = 1;
            }
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparenz);
            g2.setComposite(ac);
        } else {
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);
            g2.setComposite(ac);
        }

        GeneralPath grL = new GeneralPath();
        //===============================================
        float[] xPix = new float[anzKurvenpunkteImWorksheet];
        float[] yPix = new float[anzKurvenpunkteImWorksheet];
        int x0Kurve = _xAxisX[indexCurveAssociatedXAxis[i1]];  // zugehoerige x-Achse definiert x0 der Kurve
        int y0Kurve = _yAxisY[indexCurveAssociatedYAxis[i1]];  // zugehoerige y-Achse definiert y0 der Kurve


        for (int i2 = 0; i2 < anzKurvenpunkteImWorksheet; i2++) {
            double x = worksheetData.getValue(curve_index_worksheetColumns_XY[i1][0], i2);
            if (xAxisType[indexCurveAssociatedXAxis[i1]] == ACHSE_LIN) {
                xPix[i2] = (float) (x0Kurve + (sfX[indexCurveAssociatedXAxis[i1]] * (x - axisXmin[indexCurveAssociatedXAxis[i1]])));
            } else if (xAxisType[indexCurveAssociatedXAxis[i1]] == ACHSE_LOG) {
                xPix[i2] = (float) (x0Kurve + (sfX[indexCurveAssociatedXAxis[i1]] * this.lg10(x / axisXmin[indexCurveAssociatedXAxis[i1]])));
            }

            double y = worksheetData.getValue(curve_index_worksheetColumns_XY[i1][1], i2);
            if (yAxisType[indexCurveAssociatedYAxis[i1]] == ACHSE_LIN) {
                yPix[i2] = (float) (y0Kurve - (sfY[indexCurveAssociatedYAxis[i1]] * (y - axisYmin[indexCurveAssociatedYAxis[i1]])));
//                if(y == 20.15) {
//                    System.out.println(y + " " + yPix[i2]);
//                }
                //System.out.println(yPix[i2]);
            } else if (yAxisType[indexCurveAssociatedYAxis[i1]] == ACHSE_LOG) {
                if (y <= 0) {
                    y = 1e-99;  //y=axisYmin[indexCurveAssociatedYAxis[i1]];
                }
                yPix[i2] = (float) (y0Kurve - (sfY[indexCurveAssociatedYAxis[i1]] * this.lg10(y / axisYmin[indexCurveAssociatedYAxis[i1]])));
            }
        }



        g2.setClip(x0Kurve + 1, y0Kurve - heightPix[indexCurveAssociatedYAxis[i1]] - 1, widthPix[indexCurveAssociatedYAxis[i1]] + 2, heightPix[indexCurveAssociatedYAxis[i1]] + 3);
        //--------------------------------
        g2.setColor(curveColor[i1]);
        //
        if (curveLineStyle[i1] == SOLID_PLAIN) {
            g2.setStroke(str_SOLID_PLAIN);
        } else if (curveLineStyle[i1] == SOLID_FAT_1) {
            g2.setStroke(str_SOLID_FAT_1);
        } else if (curveLineStyle[i1] == SOLID_FAT_2) {
            g2.setStroke(str_SOLID_FAT_2);
        } else if (curveLineStyle[i1] == DOTTED_PLAIN) {
            g2.setStroke(str_DOTTED_PLAIN);
        } else if (curveLineStyle[i1] == DOTTED_FAT) {
            g2.setStroke(str_DOTTED_FAT);
        } else if (curveLineStyle[i1] != INVISIBLE) {
            System.out.println("Fehler: rhjw5z65");
        }

        //-----------------------
        //
        grL.reset();
        if (curveLineStyle[i1] != INVISIBLE) {
            grL.moveTo(xPix[0], yPix[0]);
            for (int i5 = 1; i5 < anzKurvenpunkteImWorksheet; i5++) {
                if(xPix[i5] < 2000 && yPix[i5] < 2000) { // old uwe-bug!
                    grL.lineTo(xPix[i5], yPix[i5]);
                }
            }

            g2.draw(grL);
        }


        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        //-----------------------
        int dmCIRCLE = 8, hCROSS = 4, aRECT = 6, aTRIANG = 8;

        if (showCurvePointSymbol[i1]) {
            g2.setColor(crvSymbFarbe[i1]);
            for (int i2 = 0; i2 < anzKurvenpunkteImWorksheet; i2++) {

                if (i2 % crvSymbFrequ[i1] == 0) {
                    switch (crvSymbShape[i1]) {
                        case SYBM_CIRCLE:
                            g2.drawOval(Math.round(xPix[i2]) - dmCIRCLE / 2, (int) Math.round(yPix[i2]) - dmCIRCLE / 2, dmCIRCLE, dmCIRCLE);
                            break;
                        case SYBM_CIRCLE_FILLED:
                            g2.fillOval(Math.round(xPix[i2]) - dmCIRCLE / 2, Math.round(yPix[i2]) - dmCIRCLE / 2, dmCIRCLE, dmCIRCLE);
                            break;
                        case SYBM_CROSS:
                            g2.drawLine(Math.round(xPix[i2]) - hCROSS, Math.round(yPix[i2]), Math.round(xPix[i2]) + hCROSS, Math.round(yPix[i2]));
                            g2.drawLine(Math.round(xPix[i2]), Math.round(yPix[i2]) - hCROSS, Math.round(xPix[i2]), Math.round(yPix[i2]) + hCROSS);
                            break;
                        case SYBM_RECT:
                            g2.drawRect(Math.round(xPix[i2]) - aRECT / 2, Math.round(yPix[i2]) - aRECT / 2, aRECT, aRECT);
                            break;
                        case SYBM_RECT_FILLED:
                            g2.fillRect(Math.round(xPix[i2]) - aRECT / 2, Math.round(yPix[i2]) - aRECT / 2, aRECT, aRECT);
                            break;
                        case SYBM_TRIANG:
                            g2.drawPolygon(new int[]{Math.round(xPix[i2]) - aTRIANG / 2, Math.round(xPix[i2]) + aTRIANG / 2, Math.round(xPix[i2])},
                                    new int[]{Math.round(yPix[i2]) + (int) (0.29 * aTRIANG), Math.round(yPix[i2]) + (int) (0.29 * aTRIANG), Math.round(yPix[i2]) - (int) (0.58 * aTRIANG)}, 3);
                            break;
                        case SYBM_TRIANG_FILLED:
                            g2.fillPolygon(new int[]{Math.round(xPix[i2]) - aTRIANG / 2, Math.round(xPix[i2]) + aTRIANG / 2, Math.round(xPix[i2])},
                                    new int[]{Math.round(yPix[i2]) + (int) (0.29 * aTRIANG), Math.round(yPix[i2]) + (int) (0.29 * aTRIANG), Math.round(yPix[i2]) - (int) (0.58 * aTRIANG)}, 3);
                            break;
                        default:
                            System.out.println("Fehler: q09gj023");
                            break;
                    }
                }
            }
        }

        g2.setClip(null);
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        g2.setComposite(ac);
        //===============================================
    }

    protected void drawSingleCoordinateAxisX(Graphics2D g2, int i1) {
        //==================================
        if (xAxisType[i1] == ACHSE_LIN) {
            sfX[i1] = widthPix[i1] / (axisXmax[i1] - axisXmin[i1]);
            int anzTicks = (int) (axisXmax[i1] / xTickSpacing[i1]) - (int) (axisXmin[i1] / xTickSpacing[i1]) + 1;
            double[] wertTickX_temp = new double[anzTicks];
            int[] tickX_temp = new int[anzTicks];
            int j = 0;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = xTickSpacing[i1] * (int) (axisXmin[i1] / xTickSpacing[i1]) + i2 * xTickSpacing[i1];
                int tick = this.calculateXPixLinear(wert, i1);
                if ((axisXmin[i1] <= wert) && (wert <= axisXmax[i1])) {
                    wertTickX_temp[j] = wert;
                    tickX_temp[j] = tick;
                    j++;
                }
            }
            anzTicks = j;  // Correction of the number of ticks -->
            double maxXtic = -100;
            int maxIndex = -1;
            for (int i3 = 0; i3 < _xAxisY.length; i3++) {
                maxXtic = Math.max(_xAxisY[i3] + posXtickLabels[i3], maxXtic);
                if (maxXtic == _xAxisY[i3] + posXtickLabels[i3]) {
                    maxIndex = i3;
                }

            }
            showLabelsXmaj[maxIndex] = true;

            // hier wird geprueft, ob ueberhaupt Ticks eingetragen werden sollen -->
            if (j != 0) {
                //-----------------------------------------------------------------------------------------------------------------------
                valueTickX[i1] = new double[anzTicks];
                System.arraycopy(wertTickX_temp, 0, valueTickX[i1], 0, anzTicks);
                tickX[i1] = new int[anzTicks];
                System.arraycopy(tickX_temp, 0, tickX[i1], 0, anzTicks);
                //
                int d = xTickLength[i1];
                if (showXTicksBottom[i1]) {
                    d = -d;
                }


                for (int i2 = 0; i2 < anzTicks; i2++) {
                    g2.setColor(colorAxesX[i1]);
                    g2.drawLine(tickX[i1][i2], _xAxisY[i1], tickX[i1][i2], _xAxisY[i1] - d);  // Ticks auf der x-Achse


                    if (showLabelsXmaj[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatENG(valueTickX[i1][i2], digitsX);  //nf.format(valueTickX[i1][i2]);
                        g2.drawString(label, -3 + tickX[i1][i2], _xAxisY[i1] + posXtickLabels[i1]);  // Labels auf der x-Achse
                    }
                }
                //
                double xTickSpacingMinor = xTickSpacing[i1] / xNumTicksMinor[i1];  // Wert zwischen zwei Minor-Ticks auf der x-Achse
                int xMinorTicksAnzahl = (int) ((axisXmax[i1] - axisXmin[i1]) / xTickSpacingMinor) + 2;
                double[] wertTickMinorX_temp = new double[xMinorTicksAnzahl];
                double wertMinor = valueTickX[i1][0] - xTickSpacing[i1];
                j = 0;
                while (wertMinor < valueTickX[i1][0] - 1.5 * xTickSpacingMinor) {
                    wertMinor += xTickSpacingMinor;
                    if (wertMinor >= axisXmin[i1]) {
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                for (int i2 = 0; i2 < valueTickX[i1].length - 1; i2++) {
                    wertMinor = valueTickX[i1][i2] + xTickSpacingMinor;
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                    while (wertMinor < valueTickX[i1][i2 + 1] - 1.5 * xTickSpacingMinor) {
                        wertMinor += xTickSpacingMinor;
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                wertMinor = valueTickX[i1][valueTickX[i1].length - 1];
                while (wertMinor < axisXmax[i1] - 0.99 * xTickSpacingMinor) {
                    wertMinor += xTickSpacingMinor;
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                }
                xMinorTicksAnzahl = j;  // Correction of the minor tick number
                valueTickXminor[i1] = new double[xMinorTicksAnzahl];
                tickXminor[i1] = new int[xMinorTicksAnzahl];
                System.arraycopy(wertTickMinorX_temp, 0, valueTickXminor[i1], 0, xMinorTicksAnzahl);
                d = xTickLengthMinor[i1];
                if (showXTicksBottom[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < xMinorTicksAnzahl; i2++) {
                    tickXminor[i1][i2] = this.calculateXPixLinear(valueTickXminor[i1][i2], i1);
                    g2.drawLine(tickXminor[i1][i2], _xAxisY[i1], tickXminor[i1][i2], _xAxisY[i1] - d);  // Minor-Ticks auf der x-Achse
                    if (showLabelsXmin[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatENG(valueTickXminor[i1][i2], digitsX);  // nf.format(valueTickXminor[i1][i2]);
                        g2.drawString(label, tickXminor[i1][i2], _xAxisY[i1] + posXtickLabels[i1]);  // Minor-Labels auf der x-Achse
                    }
                    //-----------------------------------------------------------------------------------------------------------------------
                }
            }
            //==================================
        } else if (xAxisType[i1] == ACHSE_LOG) {
            // Here it is checked whether ticks should be entered at all -->
            sfX[i1] = widthPix[i1] / this.lg10(axisXmax[i1] / axisXmin[i1]);
            int anzTicks = (int) Math.round(this.lg10(axisXmax[i1] / axisXmin[i1])) + 3;
            double[] wertTickX_temp = new double[anzTicks];
            int[] tickX_temp = new int[anzTicks];
            int j = 0;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = Math.pow(10, (int) this.lg10(axisXmin[i1]) - 1 + i2);
                int tick = this.calculateXPixLogarithmic(wert, i1);
                if ((axisXmin[i1] <= wert) && (wert <= axisXmax[i1])) {
                    wertTickX_temp[j] = wert;
                    tickX_temp[j] = tick;
                    j++;
                }
            }
            //
            if (j != 0) {
                //-----------------------------------------------------------------------------------------------------------------------
                anzTicks = j;  // Correction of the number of ticks -->
                valueTickX[i1] = new double[anzTicks];
                System.arraycopy(wertTickX_temp, 0, valueTickX[i1], 0, anzTicks);
                tickX[i1] = new int[anzTicks];
                System.arraycopy(tickX_temp, 0, tickX[i1], 0, anzTicks);
                //
                int d = xTickLength[i1];
                if (showXTicksBottom[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < anzTicks; i2++) {
                    g2.setColor(colorAxesX[i1]);
                    g2.drawLine(tickX[i1][i2], _xAxisY[i1], tickX[i1][i2], _xAxisY[i1] - d);  // Ticks auf der x-Achse
                    if (showLabelsXmaj[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatT(valueTickX[i1][i2], "#.E0");  // tcf.formatENG(valueTickX[i1][i2],digitsX);
                        g2.drawString(label, tickX[i1][i2], _xAxisY[i1] + posXtickLabels[i1]);  // Labels auf der x-Achse
                    }
                }
                //
                int xMinorTicksAnzahl = 10 * (anzTicks + 1);
                double[] wertTickMinorX_temp = new double[xMinorTicksAnzahl];
                //int[] tickMinorX_temp= new int[xMinorTicksAnzahl];
                double wertMinor = valueTickX[i1][0] / ((int) (valueTickX[i1][0] / axisXmin[i1]));
                wertTickMinorX_temp[0] = wertMinor;
                j = 1;
                while (wertMinor < 0.85 * valueTickX[i1][0]) {
                    wertMinor += (0.1 * valueTickX[i1][0]);
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                }
                for (int i2 = 0; i2 < valueTickX[i1].length - 1; i2++) {
                    wertMinor = valueTickX[i1][i2] + (0.1 * valueTickX[i1][i2 + 1]);
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                    while (wertMinor < 0.85 * valueTickX[i1][i2 + 1]) {
                        wertMinor += (0.1 * valueTickX[i1][i2 + 1]);
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                wertMinor = valueTickX[i1][valueTickX[i1].length - 1];
                while (wertMinor < axisXmax[i1]) {
                    wertMinor += valueTickX[i1][valueTickX[i1].length - 1];
                    if (wertMinor <= axisXmax[i1]) {
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                xMinorTicksAnzahl = j;  // Correction of the minor tick number
                valueTickXminor[i1] = new double[xMinorTicksAnzahl];
                tickXminor[i1] = new int[xMinorTicksAnzahl];
                System.arraycopy(wertTickMinorX_temp, 0, valueTickXminor[i1], 0, xMinorTicksAnzahl);
                d = xTickLengthMinor[i1];
                if (showXTicksBottom[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < xMinorTicksAnzahl; i2++) {
                    tickXminor[i1][i2] = this.calculateXPixLogarithmic(valueTickXminor[i1][i2], i1);
                    g2.drawLine(tickXminor[i1][i2], _xAxisY[i1], tickXminor[i1][i2], _xAxisY[i1] - d);  // Minor-Ticks auf der x-Achse
                    if (showLabelsXmin[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatT(valueTickXminor[i1][i2], "#.E0");  // tcf.formatENG(valueTickXminor[i1][i2],digitsX);
                        g2.drawString(label, tickXminor[i1][i2], _xAxisY[i1] + posXtickLabels[i1]);  // Minor-Labels auf der x-Achse
                    }
                }
                //-----------------------------------------------------------------------------------------------------------------------
            }
        }

    }

    protected void drawSingleCoordinateAxisY(Graphics2D g2, int i1) {

        if (yAxisType[i1] == ACHSE_LIN) {
            sfY[i1] = heightPix[i1] / (axisYmax[i1] - axisYmin[i1]);
            int anzTicks = (int) (axisYmax[i1] / yTickSpacing[i1]) - (int) (axisYmin[i1] / yTickSpacing[i1]) + 1;
            anzTicks = Math.max(anzTicks, 2);
            double[] wertTickY_temp = new double[anzTicks];
            int[] tickY_temp = new int[anzTicks];
            int j = 0;

            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = yTickSpacing[i1] * (int) (axisYmin[i1] / yTickSpacing[i1]) + i2 * yTickSpacing[i1];
                int tick = this.calculateYPixLinear(wert, i1);
                if ((axisYmin[i1] <= wert) && (wert <= axisYmax[i1])) {
                    wertTickY_temp[j] = wert;
                    tickY_temp[j] = tick;
                    j++;
                }
            }
            anzTicks = j;  // Correction of the number of ticks -->
            valueTickY[i1] = new double[anzTicks];
            System.arraycopy(wertTickY_temp, 0, valueTickY[i1], 0, anzTicks);
            tickY[i1] = new int[anzTicks];
            System.arraycopy(tickY_temp, 0, tickY[i1], 0, anzTicks);
            //
            int d = yTickLength[i1];
            if (showYTicksLeft[i1]) {
                d = -d;
            }
            for (int i2 = 0; i2 < anzTicks; i2++) {
                g2.setColor(colorAxesY[i1]);
                g2.drawLine(_yAxisX[i1], tickY[i1][i2], _yAxisX[i1] + d, tickY[i1][i2]);  // Ticks auf der y-Achse
                if (showLabelsYmaj[i1]) {
                    g2.setFont(foTickLabelY[i1]);
                    String label = tcf.formatENG(valueTickY[i1][i2], digitsY);  // nf.format(valueTickY[i1][i2]);
                    //if ((label.equals("0"))&&(valueTickY[i1][i2]!=0)) label= ""+valueTickY[i1][i2];
                    g2.drawString(label, _yAxisX[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickY[i1][i2] + foTickLabelY[i1].getSize() / 2 - 1);  // Labels auf der y-Achse
                }
            }
            //
            double yTickSpacingMinor = yTickSpacing[i1] / yNumTicksMinor[i1];  // Wert zwischen zwei Minor-Ticks auf der y-Achse
            int yMinorTicksAnzahl = (int) ((axisYmax[i1] - axisYmin[i1]) / yTickSpacingMinor) + 2;
            double[] wertTickMinorY_temp = new double[yMinorTicksAnzahl];
            double wertMinor = valueTickY[i1][0] - yTickSpacing[i1];
            j = 0;
            while (wertMinor < valueTickY[i1][0] - 1.5 * yTickSpacingMinor) {
                wertMinor += yTickSpacingMinor;
                if (wertMinor >= axisYmin[i1]) {
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                }
            }
            for (int i2 = 0; i2 < valueTickY[i1].length - 1; i2++) {
                wertMinor = valueTickY[i1][i2] + yTickSpacingMinor;
                wertTickMinorY_temp[j] = wertMinor;
                j++;
                while (wertMinor < valueTickY[i1][i2 + 1] - 1.5 * yTickSpacingMinor) {
                    wertMinor += yTickSpacingMinor;
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                }
            }
            wertMinor = valueTickY[i1][valueTickY[i1].length - 1];
            while (wertMinor < axisYmax[i1] - 0.99 * yTickSpacingMinor) {
                wertMinor += yTickSpacingMinor;
                wertTickMinorY_temp[j] = wertMinor;
                j++;
            }
            yMinorTicksAnzahl = j;  // Correction of the minor tick number
            valueTickYminor[i1] = new double[yMinorTicksAnzahl];
            tickYminor[i1] = new int[yMinorTicksAnzahl];
            System.arraycopy(wertTickMinorY_temp, 0, valueTickYminor[i1], 0, yMinorTicksAnzahl);
            d = yTickLengthMinor[i1];
            if (showYTicksLeft[i1]) {
                d = -d;
            }
            for (int i2 = 0; i2 < yMinorTicksAnzahl; i2++) {
                tickYminor[i1][i2] = this.calculateYPixLinear(valueTickYminor[i1][i2], i1);
                g2.drawLine(_yAxisX[i1], tickYminor[i1][i2], _yAxisX[i1] + d, tickYminor[i1][i2]);  // Minor-Ticks auf der y-Achse
                if (showLabelsYmin[i1]) {
                    g2.setFont(foTickLabelY[i1]);
                    String label = tcf.formatENG(valueTickYminor[i1][i2], digitsY);  // nf.format(valueTickYminor[i1][i2]);
                    g2.drawString(label, _yAxisX[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickYminor[i1][i2] + foTickLabelY[i1].getSize() / 2);  // Minor-Labels auf der y-Achse
                }
            }
            //==================================
        } else if (yAxisType[i1] == ACHSE_LOG) {
            if (axisYmin[i1] <= 0) {
                axisYmin[i1] = axisYmax[i1] / 1e4;
            }
            // Here it is checked whether ticks should be entered at all -->
            sfY[i1] = heightPix[i1] / this.lg10(axisYmax[i1] / axisYmin[i1]);
            int anzTicks = (int) this.lg10(axisYmax[i1] / axisYmin[i1]) + 3;
            double[] wertTickY_temp = new double[anzTicks];
            int[] tickY_temp = new int[anzTicks];
            int j = 0;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = Math.pow(10, (int) this.lg10(axisYmin[i1]) - 1 + i2);
                int tick = this.calculateYPixLogarithmic(wert, i1);
                if ((axisYmin[i1] <= wert) && (wert <= axisYmax[i1])) {
                    wertTickY_temp[j] = wert;
                    tickY_temp[j] = tick;
                    j++;
                }
            }
            //
            if (j != 0) {
                //-----------------------------------------------------------------------------------------------------------------------
                anzTicks = j;  // Correction of the number of ticks -->
                valueTickY[i1] = new double[anzTicks];
                System.arraycopy(wertTickY_temp, 0, valueTickY[i1], 0, anzTicks);
                tickY[i1] = new int[anzTicks];
                System.arraycopy(tickY_temp, 0, tickY[i1], 0, anzTicks);
                //
                int d = yTickLength[i1];
                if (showYTicksLeft[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < anzTicks; i2++) {
                    g2.setColor(colorAxesY[i1]);
                    g2.drawLine(_yAxisX[i1], tickY[i1][i2], _yAxisX[i1] + d, tickY[i1][i2]);  // Ticks auf der y-Achse
                    if (showLabelsYmaj[i1]) {
                        g2.setFont(foTickLabelY[i1]);
                        String label = tcf.formatT(valueTickY[i1][i2], "#.E0");  // tcf.formatENG(valueTickY[i1][i2],digitsY);
                        g2.drawString(label, _yAxisX[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickY[i1][i2] + foTickLabelY[i1].getSize() / 2);  // Labels auf der y-Achse
                    }
                }
                //
                int yMinorTicksAnzahl = 10 * (anzTicks + 1);
                double[] wertTickMinorY_temp = new double[yMinorTicksAnzahl];
                double wertMinor = valueTickY[i1][0] / ((int) (valueTickY[i1][0] / axisYmin[i1]));
                wertTickMinorY_temp[0] = wertMinor;
                j = 1;
                while (wertMinor < 0.85 * valueTickY[i1][0]) {
                    wertMinor += (0.1 * valueTickY[i1][0]);
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                }
                for (int i2 = 0; i2 < valueTickY[i1].length - 1; i2++) {
                    wertMinor = valueTickY[i1][i2] + (0.1 * valueTickY[i1][i2 + 1]);
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                    while (wertMinor < 0.85 * valueTickY[i1][i2 + 1]) {
                        wertMinor += (0.1 * valueTickY[i1][i2 + 1]);
                        wertTickMinorY_temp[j] = wertMinor;
                        j++;
                    }
                }
                wertMinor = valueTickY[i1][valueTickY[i1].length - 1];
                while (wertMinor < axisYmax[i1]) {
                    wertMinor += valueTickY[i1][valueTickY[i1].length - 1];
                    if (wertMinor <= axisYmax[i1]) {
                        wertTickMinorY_temp[j] = wertMinor;
                        j++;
                    }
                }
                yMinorTicksAnzahl = j;  // Correction of the minor tick number
                valueTickYminor[i1] = new double[yMinorTicksAnzahl];
                tickYminor[i1] = new int[yMinorTicksAnzahl];
                System.arraycopy(wertTickMinorY_temp, 0, valueTickYminor[i1], 0, yMinorTicksAnzahl);
                d = yTickLengthMinor[i1];
                if (showYTicksLeft[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < yMinorTicksAnzahl; i2++) {
                    tickYminor[i1][i2] = this.calculateYPixLogarithmic(valueTickYminor[i1][i2], i1);
                    g2.drawLine(_yAxisX[i1], tickYminor[i1][i2], _yAxisX[i1] + d, tickYminor[i1][i2]);  // Minor-Ticks auf der y-Achse
                    if (showLabelsYmin[i1]) {
                        g2.setFont(foTickLabelY[i1]);
                        String label = tcf.formatT(valueTickYminor[i1][i2], "#.E0");  // tcf.formatENG(valueTickYminor[i1][i2],digitsY);
                        g2.drawString(label, _yAxisX[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickYminor[i1][i2] + foTickLabelY[i1].getSize() / 2);  // Minor-Labels auf der y-Achse
                    }
                }
                //-----------------------------------------------------------------------------------------------------------------------
            }
        }
        //==================================
        /*
         * g2.setColor(colorAxesY[i1]); if (lineStyleAxesY[i1]==SOLID_PLAIN) { g2.setStroke(str_SOLID_PLAIN); } else if
         * (lineStyleAxesY[i1]==INVISIBLE) { // nix machen, weil unsichtbar } else if (lineStyleAxesY[i1]==SOLID_FAT_1) {
         * g2.setStroke(str_SOLID_FAT_1); } else if (lineStyleAxesY[i1]==SOLID_FAT_2) { g2.setStroke(str_SOLID_FAT_2); } else
         * if (lineStyleAxesY[i1]==DOTTED_PLAIN) { g2.setStroke(str_DOTTED_PLAIN); } else if
         * (lineStyleAxesY[i1]==DOTTED_FAT) { g2.setStroke(str_DOTTED_FAT); } else System.out.println("Fehler: hhqqt5");
         * //----------------------- // jetzt die Linie ziehen: grL.reset(); grL.moveTo(xAchseY[i1], yAchseY[i1]);
         * grL.lineTo(xAchseY[i1], yAchseY[i1]-heightPix[i1]); if (lineStyleAxesY[i1]!=GraferV3.INVISIBLE) { g2.draw(grL);
         * g2.drawString(yAxisLabel[i1], xAchseY[i1]-posYtickLabels[i1], yAchseY[i1]-heightPix[i1]/2); }
         * g2.setStroke(str_SOLID_PLAIN); // wieder auf 'default' setzen
         */
        //==================================
    }

    protected void drawGridNormalX(Graphics g) {
        GeneralPath grL = new GeneralPath();
        Graphics2D g2 = (Graphics2D) g;
        //
        if ((gridNormalX_associatedXAxis == null) || (gridNormalX_associatedYAxis == null)) {
            return;  // um Fehler beim ersten Aufruf zu vermeiden
        }        //------------------------------------------------------------
        //-----------------------
        for (int i1 = 0; i1 < gridNormalX_associatedXAxis.length; i1++) {
            int indexAchseX = gridNormalX_associatedXAxis[i1];
            int indexAchseY = gridNormalX_associatedYAxis[i1];
            if ((indexAchseX != -1) && (indexAchseY != -1)) {
                // Minor-Grids -->
                for (int[] gridConfig : showGridNormalXminor) {
                    if ((gridConfig[0] == indexAchseX) && (gridConfig[1] == indexAchseY) && (tickXminor[indexAchseX] != null)) {
                        for (int i2 = 0; i2 < tickXminor[indexAchseX].length; i2++) {
                            if (lineStyleGridNormalXminor[i1] == INVISIBLE) {
                                continue;
                            }
                            g.setColor(colorGridNormalXminor[i1]);
                            if (lineStyleGridNormalXminor[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (lineStyleGridNormalXminor[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (lineStyleGridNormalXminor[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (lineStyleGridNormalXminor[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (lineStyleGridNormalXminor[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: p05kgh9");
                            }
                            //-----------------------
                            //-----------------------
                            grL.reset();
                            grL.moveTo(tickXminor[indexAchseX][i2], _yAxisY[indexAchseY]);
                            grL.lineTo(tickXminor[indexAchseX][i2], _yAxisY[indexAchseY] - heightPix[indexAchseY]);
                            g2.draw(grL);
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
                // Major-Ticks -->
                for (int[] gridConfig : showGridNormalXmajor) {
                    if ((gridConfig[0] == indexAchseX) && (gridConfig[1] == indexAchseY) && (tickX[indexAchseX] != null)) {
                        for (int i2 = 0; i2 < tickX[indexAchseX].length; i2++) {
                            if (lineStyleGridNormalX[i1] == INVISIBLE) {
                                continue;
                            }
                            g.setColor(colorGridNormalX[i1]);
                            if (lineStyleGridNormalX[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (lineStyleGridNormalX[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (lineStyleGridNormalX[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (lineStyleGridNormalX[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (lineStyleGridNormalX[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: hrtrjww5j");
                            }
                            //-----------------------
                            // Minor-Grids -->
                            grL.reset();
                            grL.moveTo(tickX[indexAchseX][i2], _yAxisY[indexAchseY]);
                            grL.lineTo(tickX[indexAchseX][i2], _yAxisY[indexAchseY] - heightPix[indexAchseY]);
                            g2.draw(grL);
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
            }
        }
        //------------------------------------------------------------
    }

    protected void drawGridNormalY(Graphics g) {
        GeneralPath grL = new GeneralPath();
        Graphics2D g2 = (Graphics2D) g;
        //
        if ((gridNormalY_associatedXAxis == null) || (gridNormalY_associatedYAxis == null)) {
            return;  // um Fehler beim ersten Aufruf zu vermeiden
        }        //------------------------------------------------------------
        // now draw the line:
        for (int i1 = 0; i1 < gridNormalY_associatedXAxis.length; i1++) {
            int indexAchseX = gridNormalY_associatedXAxis[i1];
            int indexAchseY = gridNormalY_associatedYAxis[i1];
            if ((indexAchseX != -1) && (indexAchseY != -1)) {
                // Minor-Grids -->
                for (int[] gridConfig : showGridNormalYminor) {
                    if ((gridConfig[0] == indexAchseX) && (gridConfig[1] == indexAchseY) && (tickYminor[indexAchseY] != null)) {
                        for (int i2 = 0; i2 < tickYminor[indexAchseY].length; i2++) {
                            if (lineStyleGridNormalYminor[i1] == INVISIBLE) {
                                continue;
                            }
                            g.setColor(colorGridNormalYminor[i1]);
                            if (lineStyleGridNormalYminor[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (lineStyleGridNormalYminor[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (lineStyleGridNormalYminor[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (lineStyleGridNormalYminor[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (lineStyleGridNormalYminor[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: hezwhwh6");
                            }
                            //-----------------------
                            //-----------------------
                            grL.reset();
                            grL.moveTo(_xAxisX[indexAchseX], tickYminor[indexAchseY][i2]);
                            grL.lineTo(_xAxisX[indexAchseX] + widthPix[indexAchseX], tickYminor[indexAchseY][i2]);
                            g2.draw(grL);
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
                // Major-Ticks -->
                for (int[] gridConfig : showGridNormalYmajor) {
                    if ((gridConfig[0] == indexAchseX) && (gridConfig[1] == indexAchseY) && (tickY[indexAchseY] != null)) {
                        for (int i2 = 0; i2 < tickY[indexAchseY].length; i2++) {
                            if (lineStyleGridNormalY[i1] == INVISIBLE) {
                                continue;
                            }
                            g.setColor(colorGridNormalY[i1]);
                            if (lineStyleGridNormalY[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (lineStyleGridNormalY[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (lineStyleGridNormalY[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (lineStyleGridNormalY[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (lineStyleGridNormalY[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: gjigrije");
                            }
                            //-----------------------
                            //
                            grL.reset();
                            grL.moveTo(_xAxisX[indexAchseX], tickY[indexAchseY][i2]);
                            grL.lineTo(_xAxisX[indexAchseX] + widthPix[indexAchseX], tickY[indexAchseY][i2]);
                            g2.draw(grL);
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
            }
        }
        //------------------------------------------------------------
    }

    protected void auto_BereichsgrenzenDerAchsen() {
        //----------------------
        //----------------------
        if (worksheetData == null) {
            return;
        }
        //----------------------
        //
        int laenge = worksheetData.getRowLength();
        double[] minEmpfehlungLIN = new double[laenge];  // empfohlene Achsen-Min-Werte bei AUTO / ACHSE_LIN
        double[] maxEmpfehlungLIN = new double[laenge];  // empfohlene Achsen-Max-Werte bei AUTO / ACHSE_LIN
        double[] minEmpfehlungLOG = new double[laenge];  // empfohlene Achsen-Min-Werte bei AUTO / ACHSE_LOG
        double[] maxEmpfehlungLOG = new double[laenge];  // empfohlene Achsen-Max-Werte bei AUTO / ACHSE_LOG
        double[] min = new double[laenge];  // Min and max values ​​of the individual columns of 'worksheet data'
        double[] max = new double[laenge];
        //
        for (int i1 = 0; i1 < laenge; i1++) {
            min[i1] = 1e99;
            max[i1] = -1e99;  // initial
            for (int i2 = 0; i2 < worksheetData.getColumnLength(); i2++) {
                if (worksheetData.getValue(i1, i2) < min[i1]) {
                    min[i1] = worksheetData.getValue(i1, i2);
                }
                if (worksheetData.getValue(i1, i2) > max[i1]) {
                    max[i1] = worksheetData.getValue(i1, i2);
                }
            }
            double[] autoEmpf = this.auto_Achsenbegrenzung_Wertempfehlung(min[i1], max[i1]);
            minEmpfehlungLIN[i1] = autoEmpf[0];
            maxEmpfehlungLIN[i1] = autoEmpf[1];
            minEmpfehlungLOG[i1] = autoEmpf[2];
            maxEmpfehlungLOG[i1] = autoEmpf[3];
        }
        //----------------------
        // nur die mit 'AUTO' bezeichneten Daten werden nachfolgend automatisch gesetzt:
        //
        for (int achsenNr = 0; achsenNr < axisXmin.length; achsenNr++) {
            if (autoAxisXmin[achsenNr]) {
                // now assign the corresponding minimums and maximums to the axes occupied by curves:
                axisXmin[achsenNr] = 1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < numCurves; nrKurve++) {
                    if (achsenNr == indexCurveAssociatedXAxis[nrKurve]) {
                        if ((xAxisType[achsenNr] == ACHSE_LIN)
                                && (axisXmin[achsenNr] > minEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][0]])) {
                            axisXmin[achsenNr] = minEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][0]];
                        } else if ((xAxisType[achsenNr] == ACHSE_LOG)
                                && (axisXmin[achsenNr] > minEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][0]])) {
                            axisXmin[achsenNr] = minEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][0]];
                        }
                    }
                }
            }
        }
        for (int achsenNr = 0; achsenNr < axisXmax.length; achsenNr++) {
            if (autoAxisXmax[achsenNr]) {
                // now assign the corresponding minimums and maximums to the axes occupied by curves:
                axisXmax[achsenNr] = -1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < numCurves; nrKurve++) {
                    if (achsenNr == indexCurveAssociatedXAxis[nrKurve]) {
                        if ((xAxisType[achsenNr] == ACHSE_LIN)
                                && (axisXmax[achsenNr] < maxEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][0]])) {
                            axisXmax[achsenNr] = maxEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][0]];
                        } else if ((xAxisType[achsenNr] == ACHSE_LOG)
                                && (axisXmax[achsenNr] < maxEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][0]])) {
                            axisXmax[achsenNr] = maxEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][0]];
                        }
                    }
                }
            }
        }
        for (int achsenNr = 0; achsenNr < axisYmin.length; achsenNr++) {
            if (autoAxisYmin[achsenNr]) {
                //----------------------
                axisYmin[achsenNr] = 1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < numCurves; nrKurve++) {
                    if (achsenNr == indexCurveAssociatedYAxis[nrKurve]) {
                        if ((yAxisType[achsenNr] == ACHSE_LIN)
                                && (axisYmin[achsenNr] > minEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][1]])) {
                            axisYmin[achsenNr] = minEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][1]];
                        } else if ((yAxisType[achsenNr] == ACHSE_LOG)
                                && (axisYmin[achsenNr] > minEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][1]])) {
                            axisYmin[achsenNr] = minEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][1]];
                        }
                    }
                }
            }
        }
        for (int achsenNr = 0; achsenNr < axisYmax.length; achsenNr++) {
            if (autoAxisYmax[achsenNr]) {
                //
                axisYmax[achsenNr] = -1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < numCurves; nrKurve++) {
                    if (achsenNr == indexCurveAssociatedYAxis[nrKurve]) {
                        if ((yAxisType[achsenNr] == ACHSE_LIN)
                                && (axisYmax[achsenNr] < maxEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][1]])) {
                            axisYmax[achsenNr] = maxEmpfehlungLIN[curve_index_worksheetColumns_XY[nrKurve][1]];
                        } else if ((yAxisType[achsenNr] == ACHSE_LOG)
                                && (axisYmax[achsenNr] < maxEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][1]])) {
                            axisYmax[achsenNr] = maxEmpfehlungLOG[curve_index_worksheetColumns_XY[nrKurve][1]];
                        }
                    }
                }
            }
        }
        //----------------------
    }

    protected double[] auto_Achsenbegrenzung_Wertempfehlung(double z1, double z2) {
        //----------------------
        // achse= [z1 .. z2] --> gesuchte Werte sollen 'rund'zahlig und eventuell ein bischen groesser als z1, z2 sein
        // different bases can be examined in parallel e.g. 2 and 5, and then a final base is selected
        double z1empfLIN, z2empfLIN, z1empfLOG, z2empfLOG;
        double schrittEmpfLIN = 1.0;
        double z1lg = z1, z2lg = z2;
        //----------------
        // LIN -->
        //
        if ((z1 == z2) || (Math.abs(z1 - z2) / (Math.abs(z1) + Math.abs(z2)) < 1e-12)) {
            double d = 0.2, z12 = z1;
            if (z1 == 0) {
                z1 -= 5;
                z2 += 5;
            } else if (z1 > 0) {
                z1 *= (1 - d);
                z2 *= (1 + d);
            } else {
                z1 *= (1 + d);
                z2 *= (1 - d);
            }
            z1empfLIN = z1;
            z2empfLIN = z2;
            schrittEmpfLIN = Math.abs(z1 - z12) * d;
        } else {
            double dz = z2 - z1;  // Ann. z2 > z1
            int potenz = (int) (Math.log(dz) / Math.log(10.0));
            double[] defaultSchritt = new double[]{1, 2, 5};  // default (AUTO)  -->
            // d.h. Skalierung zB. in Schritten 2e-7 oder 2.0 oder 200 oder ...
            // verschiedene Basis kann parallel untersucht werden zB. 2 und 5, und davon dann ein endgueltige Basis ausgewaehlt werden
            double[] schritt = new double[defaultSchritt.length];
            for (int i1 = 0; i1 < defaultSchritt.length; i1++) {
                schritt[i1] = defaultSchritt[i1] * Math.pow(10, potenz - 1);  // schritt[] > 0 gilt immer
                if ((dz / schritt[i1]) > 10) {
                    schritt[i1] = defaultSchritt[i1] * Math.pow(10, potenz);
                }
            }
            double[] unten = new double[defaultSchritt.length];
            double alt = -1;
            for (int i1 = 0; i1 < defaultSchritt.length; i1++) {
                unten[i1] = schritt[i1] * Math.round(z1 / schritt[i1]);
                alt = unten[i1] - 1;
                while (z1 < unten[i1]) {
                    unten[i1] -= schritt[i1];
                    if (Math.abs(unten[i1] - alt) < 1e-15) {
                        break;
                    } else {
                        alt = unten[i1];
                    }
                }
            }
            double[] oben = new double[defaultSchritt.length];
            for (int i1 = 0; i1 < defaultSchritt.length; i1++) {
                alt = oben[i1] - 1;
                oben[i1] = schritt[i1] * Math.round(z2 / schritt[i1]);
                while (z2 > oben[i1]) {
                    oben[i1] += schritt[i1];
                    if (Math.abs(oben[i1] - alt) < 1e-15) {
                        break;
                    } else {
                        alt = oben[i1];
                    }
                }
            }
            int k = 2;
            int[] anzSchritte = new int[defaultSchritt.length];
            for (int i1 = 0; i1 < defaultSchritt.length; i1++) {
                anzSchritte[i1] = (int) Math.round(dz / schritt[i1]);
                if ((3 < anzSchritte[i1]) && (anzSchritte[i1] < 6)) {
                    k = i1;
                    i1 = defaultSchritt.length;
                }
            }
            z1empfLIN = unten[k];
            z2empfLIN = oben[k];
            schrittEmpfLIN = schritt[k];
        }
        //System.out.println(z1+"   "+z1empfLIN+"  //  "+z2+"   "+z2empfLIN+"   "+schrittEmpfLIN);
        //----------------
        // LOG -->
        z1empfLOG = 0.7 * z1lg;  // min
        z2empfLOG = 1.3 * z2lg;  // max
        //----------------
        return new double[]{z1empfLIN, z2empfLIN, z1empfLOG, z2empfLOG, schrittEmpfLIN};
        //----------------------
    }

    public static Color selectColor(int selector) {
        switch (selector) {
            case BLACK:
                return Color.black;
            case RED:
                return Color.red;
            case GREEN:
                return Color.green;
            case BLUE:
                return Color.blue;
            case DARKGRAY:
                return Color.darkGray;
            case GRAY:
                return Color.gray;
            case LIGTHGRAY:
                return Color.lightGray;
            case WHITE:
                return Color.white;
            case MAGENTA:
                return Color.magenta;
            case CYAN:
                return Color.cyan;
            case ORANGE:
                return Color.orange;
            case YELLOW:
                return Color.yellow;
            case DARKGREEN:
                return Color.decode("0x006400");
            default:
                System.out.println("Fehler: dtcjjztdm " + selector);
                return Color.black;
        }
    }

    public static int getIndexForColorSelector(String fx) {
        for (int i1 = 0; i1 < FARBEN.length; i1++) {
            if (FARBEN[i1].equals(fx)) {
                return i1;
            }
        }
        return -1;
    }

    public static Color getSelectedColor(String col) {
        if (col.equals(FARBEN[ 0])) {
            return Color.black;
        }
        if (col.equals(FARBEN[ 1])) {
            return Color.red;
        }
        if (col.equals(FARBEN[ 2])) {
            return Color.green;
        }
        if (col.equals(FARBEN[ 3])) {
            return Color.blue;
        }
        if (col.equals(FARBEN[ 4])) {
            return Color.darkGray;
        }
        if (col.equals(FARBEN[ 5])) {
            return Color.gray;
        }
        if (col.equals(FARBEN[ 6])) {
            return Color.lightGray;
        }
        if (col.equals(FARBEN[ 7])) {
            return Color.white;
        }
        if (col.equals(FARBEN[ 8])) {
            return Color.magenta;
        }
        if (col.equals(FARBEN[ 9])) {
            return Color.cyan;
        }
        if (col.equals(FARBEN[10])) {
            return Color.orange;
        }
        if (col.equals(FARBEN[11])) {
            return Color.yellow;
        }
        if (col.equals(FARBEN[12])) {
            return Color.decode("0x006400");
        }
        return null;
    }
}
