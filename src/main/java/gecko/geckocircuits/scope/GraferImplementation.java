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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.geckocircuits.general.GlobalColors;
import gecko.geckocircuits.general.TechFormat;
import java.awt.AlphaComposite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.text.NumberFormat;
import java.util.ArrayList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Deprecated
@SuppressFBWarnings(value = {"PA_PUBLIC_PRIMITIVE_ATTRIBUTE", "EI_EXPOSE_REP2"},
        justification = "Legacy graphing class with direct field access; stores worksheet data for scope visualization")
public final class GraferImplementation extends GraferV3 implements MouseListener, MouseMotionListener {
    private static final Logger LOGGER = LogManager.getLogger(GraferImplementation.class);

    // Number of intervals on the x-axis in which Hi and Lo values ​​are determined for data compression
    private static final int INTERVALS_ALONG_X = 2000;

    private static final long serialVersionUID = 364726123473711L;
    private final Scopable _scope;  // callback
    // XXX private final ScopeSettings _scopeSettings;
    // XXX private final ScopeSettings _scopeSettings;
    private int _zvCounter;
    // specifies how many of the points in the worksheet should be displayed as a curve, see function below
    // (statischen) Datei? - default: ZV-Sim.
    public boolean _usesExternalData = false;
    //---------------------------------
    private static final int TXT_DISTANCE_Y = 10;
    public static final int ANZ_DIAGRAM_MAX = 9;  // Number of maximum possible diagrams in a SCOPE
    public static final int DX_IN_LINKS = 60, DX_IN_RECHTS = 70;  // links- u. rechtsseitige x-Einrueckung der Achsen in Pixel
    public static final int DY_IN_OBEN = 8, DY_IN_UNTEN = 8;  // y-Einrueckung der Achsen in Pixel von Oben bzw. Unten und y-Abstand zwischen 2 Diagrammen
    public static final int ABSTAND_BESCHRIFTUNG_XACHSE = 35;  // soviel Abstand nach ganz unten gibt es zusaetzlich, damit die x-Achsen-Labels gesetzt werden koennen
    private static final int ANZ_AUTO_TICKS = 5;
    private int x1, x2, y1, y2;  // Rechteck-Koordinaten des Zoom-Fensters
    private boolean angeklicktZoom = false;
    //--------
    //--------
    private int[] xGrfMIN, xGrfMAX, yGrfMIN, yGrfMAX;
    private int indexAngeklickterGraph;
    private boolean controlZoomOn = false;
    private boolean shiftZoomOn = false;
    //==========================================
    //==========================================
    public static final int DIAGRAM_TYP_ZV = 91, DIAGRAM_TYP_SGN = 92;
    //
    private int anzGrfVisible;  // Number of currently visible graphs in the scope
    private int anzDiagram;  // Number of charts
    //
    public String[] nameDiagram;  // Labels of the diagrams
    public double[] ySpacingDiagram;  // how much 'y-part' does the respective diagram have
    public int[] notwendigeHoehePixGRF;  // with SIGNAL the graph height is specified in pixels, the scope size may be adjusted (for ZV --> '-1')
    public int[] diagramTyp;  // is the respective diagram a ZV type or a signal type?
    public boolean[] jcbShowLegende;  // should the curve names be displayed in the form of a legend on the left edge of the graph?
    public boolean[] showAxisX, showAxisY;  // Show or hide the axes
    //
    public double[] minX, maxX, minY, maxY;  // Achsen-Begrenzungen
    public double[] minXOld, maxXOld, minYOld, maxYOld;  // Achsen-Begrenzungen
    public double[] minXOldOld, maxXOldOld, minYOldOld, maxYOldOld;  // Achsen-Begrenzungen
    public boolean[] autoScaleX, autoScaleY;  // should the axis limits be automatically adjusted to the worksheet data?
    public int[] xAchsenTyp, yAchsenTyp;  // Linear or logarithmic?
    public int[] xAchseFarbe, yAchseFarbe;
    public int[] xAchseStil, yAchseStil;
    // Note: xAxisLabel, yAxisLabel, gridNormal*, linStilGridNormal*,
    // xTickSpacing, yTickSpacing, xNumTicksMinor, yNumTicksMinor, xTickLength*, yTickLength*,
    // zeigeLabels* are inherited from GraferV3
    //
    public int[] colorGridNormalX, colorGridNormalXminor, colorGridNormalY, colorGridNormalYminor;
    public boolean[] xShowGridMaj, xShowGridMin, yShowGridMaj, yShowGridMin;
    //
    public boolean[] xTickAutoSpacing, yTickAutoSpacing;
    //
    private boolean[] zeichneDiagrammUmrandung;  // to draw if the grid is switched off because the display is too small (in pixel points).
    //-------------------------
    //
    public int[] positionSIGNAL;  // for SIGNAL --> contains the y-position (order) of the signal curve within the individual graph
    public int[] positionSIGNAL_ALT;
    public int[] sgnHeight, sgnDistance;
    public double[] sgnSchwelle;
    //-------------------------
    // Graph-Properties zum Merken, wenn man Achsen ein- und ausblendet -->
    //
    public boolean[] ORIGjcbXShowGridMaj, ORIGjcbXShowGridMin;
    public boolean[] ORIGjcbYShowGridMaj, ORIGjcbYShowGridMin;
    public int[] ORIGjcmXlinCol, ORIGjcmYlinCol;
    public int[] ORIGjcmXlinStyl, ORIGjcmYlinStyl;
    public int[] ORIGjtfXtickLengthMaj, ORIGjtfXtickLengthMin;
    public int[] ORIGjtfYtickLengthMaj, ORIGjtfYtickLengthMin;
    public boolean[] ORIGjcbXShowLabelMaj, ORIGjcbXShowLabelMin;
    public boolean[] ORIGjcbYShowLabelMaj, ORIGjcbYShowLabelMin;
    //=========================
    // Connection / Zuordnung  Kurve - Diagramm -->
    public static final int ASSIGNMENT_X = 51, ASSIGNMENT_Y = 52, ZUORDNUNG_SIGNAL = 54, ZUORDNUNG_NIX = 55, ZUORDNUNG_MEAN = 56;
    // Signal-Namen bzw. Worksheet-Headers:
    public int anzSignalePlusZeit;  // This is how many different columns the worksheet has
    public String[] signalNamen;
    // Zuordnungen Kurven - Diagramme:
    public int[][] matrixZuordnungKurveDiagram;
    //=========================
    public int[][] indexWsXY;  // Zuordnung Worksheetdaten - Kurven
    //=========================
    // Kurven-Properties
    public int kurvenanzahl;  // soviel verschiedene Kurven werden aktuell im SCOPE dargestellt --> entspricht einer 'Kurven-ID'
    // Signal-Namen bzw. Worksheet-Headers:
    public int[] indexDerKurveInDerMatrix;  // Abspeichern in folgendem Format: --> 1000*i1 +i2 wobei (i1..Graphenanzahl / i2..Kurvenanzahl)
    public int[] indexDerKurveInDerMatrixALT;  // Storage necessary to display the SIGNAL order correctly
    //
    public int[][] crvAchsenTyp;  // is updated via the SET method so that the matrix 'matrixAssignmentCurveDiagram' is not forgotten!
    public int[][] crvLineStyle, crvLineColor;
    public boolean[][] crvSymbShow;
    public int[][] crvSymbFrequ;
    public int[][] crvSymbShape, crvSymbColor;
    public int[][] crvClipXmin, crvClipXmax, crvClipYmin, crvClipYmax;
    public double[][] crvClipValXmin, crvClipValXmax, crvClipValYmin, crvClipValYmax;
    //
    public boolean[][] crvFillDigitalCurves;
    public int[][] crvFillingDigitalColor;
    //==========================================
    // each entry in the linkage matrix corresponds to a potential curve -->
    public static final int MOUSEMODE_NONE = 546;  // Rest position --> Mouse is deactivated
    public static final int MOUSEMODE_ZOOM_AUTOFIT = 547;  // Chart always adapts to the data values
    public static final int MOUSEMODE_ZOOM_WINDOW = 548;  // you can mark zoom rectangles with the mouse
    public static final int MOUSEMODE_DRAW_LINE = 550;  // Linien zeichnen (als Objekte!)
    public static final int MOUSEMODE_VALUE_DISPLAY_SLIDER = 554;  // a slider can be placed over all diagrams, the corresponding y values ​​and all curves are displayed
    //
    public static final int MOUSE_CLICKED = 780;
    public static final int MOUSE_PRESSED = 781;
    public static final int MOUSE_RELEASED = 782;
    public static final int MOUSE_DRAGGED = 783;
    private int mouseMode = MOUSEMODE_NONE;  // default --> Maus deaktiviert
    private int mausModusALT = MOUSEMODE_NONE;  // so that you can return to the previous mode, e.g. after pressing AutoFit
    //
    private boolean simulationLaeuftGerade = false;
    private boolean nochNichtGeZoomt = true;
    private double[][] worksheetDatenTEMP = null;  // The simulation data is stored here before zooming
    private int zvCounterTEMP = 0;  // current pointer before zooming, is reactivated with AUTO_FIT
    //---------------------------------
    private boolean xSliderActive = false;
    private int xSliderPixels;
    private double[] xSliderValue = new double[]{-1, -1};  // einem einzelnen Pixelpunkt sind eventuell mehrere Werte zugeordnet
    private double[][] ySchieberWert;  // pro Kurve gibt es zum xSliderValue-Punktepaar ein entsprechendes ySchieberWert-Punktepaar
    private TechFormat cf = new TechFormat();
    private NumberFormat nf = NumberFormat.getNumberInstance();
    //==========================================
    private ArrayList<String> txtEintraege = new ArrayList<>();
    private int xSchieberPix2;
    private double[] xSchieberWert2 = new double[]{-1, -1};
    private double[][] ySchieberWert2;
    boolean inDiffMode = false;
    double[][] crvTransparency;

    public void setAnzahlSichtbarerDiagramme(final int number) {
        this.anzGrfVisible = number;
    }

    public int getAnzahlSichtbarerDiagramme() {
        return this.anzGrfVisible;
    }

    public void setAnzahlDiagramme(final int number) {
        this.anzDiagram = number;
    }

    public int getAnzahlDiagramme() {
        return this.anzDiagram;
    }

    public double getSlider1Value() {
        return xSliderValue[0];
    }

    public double getSlider2Value() {
        return xSchieberWert2[0];
    }

    public void setSimulationLaeuftGerade(final boolean simIsRunning) {
        this.simulationLaeuftGerade = simIsRunning;
        this.nochNichtGeZoomt = true;
        //-----------------
        if (worksheetDatenTEMP != null) {
            for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                    worksheetData.setValue(worksheetDatenTEMP[i1][i2], i1, i2);
                }
            }
            worksheetDatenTEMP = null;
            nochNichtGeZoomt = true;
        }
        //-----------------
    }

    public void setCrvAchsenTyp(final int im1, final int im2, final int typ) {
        matrixZuordnungKurveDiagram[im1][im2] = typ;
        crvAchsenTyp[im1][im2] = typ;
    }

    public int getCrvAchsenTyp(final int im1, final int im2) {
        return matrixZuordnungKurveDiagram[im1][im2];
    }

    public GraferImplementation(final Scopable scope) {
        this._scope = scope;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        //------------------------

        // XXX _scopeSettings = scope.getScopeSettings();
        // XXX _scopeSettings.loadSettings(this);  // hier wird 'this' parametrisiert

        //------------------------
    }

    //------------------------
    //------------------------
    public void setZVCounter(final int zvCounter) {
        this._zvCounter = zvCounter;
    }

    // XXX _scopeSettings = scope.getScopeSettings();
    // XXX _scopeSettings.loadSettings(this);  // 'this' is parameterized here
    public void setzeKurvenUndWorksheetDaten(final String[] header, final DataContainer workSheet) {
        this.worksheetData = workSheet;

        this.signalNamen = header;
        this.anzSignalePlusZeit = header.length;
        //------------------------
        // XXX _scopeSettings.usesExternalData = this._usesExternalData;
        // Transfer as an array so that the transfer can be done as a reference
        // for calling external data that is no longer changed (in contrast to the continuous curve buildup in circuit simulation) -->
        //--------------------------
        if (this._usesExternalData) {
            // or when initializing/changing the number of curves for the simulation
            this.definiereAchsenbegrenzungenImAutoZoom(workSheet);  // minX[],maxX[],minY[],maxY[],minY2[],maxY2[] werden aus 'worksheetData' berechnet
            this.initClipping();  // crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] werden berechnet
            this.initAutotickSpacing();  // benoetigt 'minX[],maxX[],minY[],maxY[],...' zur Berechnung
        } else {
            //------------------------
            // (1) // minX[],maxX[],minY[],maxY[],minY2[],maxY2[] werden willkuerlich initial gesetzt:
            // Previously set minX/maxX/minY/maxY from SimulationKernel values (now removed)
            // Values are initialized elsewhere before use
            // (2) crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] werden berechnet
            this.initClipping();
            // (3) Auto-Ticks // benoetigen minX[],maxX[],minY[],maxY[],... zur Berechnung
            this.initAutotickSpacing();
        }
        //--------------------------
        // Data comes from the running ZV simulation / SCOPEs are initialized here before simulation data is available -->
        ySchieberWert = new double[worksheetData.getRowLength() - 1][2];
        ySchieberWert2 = new double[worksheetData.getRowLength() - 1][2];

        //-------------------------------------
        this.setAxes();  // die default-Werte der Achsen werden definiert und richtig aufbereitet an GraferV3 weitergegeben, die Tick-Parameter wurden in 'initAutotickSpacing()' ermittelt
        this.setCurves();  // die default-Werte der Kurven (in 'setDefault_ZVs' definiert) werden richtig aufbereitet an GraferV3 weitergegeben
        //--------------------------
        this.repaint();
    }

    // (2) crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] are calculated
    //
    public void akualisiereKurvenUndWorksheetDaten(final double t1, final double t2) {
        //--------------------------
        this.definiereAchsenbegrenzungenNumerischeSimulation(t1, t2);  // minX[],maxX[],minY[],maxY[] werden aus 'worksheetData' berechnet
        //--------------------------
        this.setAxes();  // die default-Werte der Achsen werden definiert und richtig aufbereitet an GraferV3 weitergegeben, die Tick-Parameter wurden in 'initAutotickSpacing()' ermittelt
        this.setCurves();  // die default-Werte der Kurven (in 'setDefault_ZVs' definiert) werden richtig aufbereitet an GraferV3 weitergegeben
        //--------------------------
        this.possiblyHideGridLines();
        this.repaint();
    }

    // Ueberschrieben, damit man einfach SIGNAL-Kurven zeichen kann -->
    @Override
    protected void drawCurves(final Graphics g) {
        if (worksheetData == null) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g;
        int zd = 0;  // Beschriftungs-Nummerierung in y-Richtung
        for (int i1 = 0; i1 < numCurves; i1++) {
            if (matrixZuordnungKurveDiagram[indexDerKurveInDerMatrix[i1] / 1000][indexDerKurveInDerMatrix[i1] % 1000] == this.ZUORDNUNG_SIGNAL) {
                try {
                    zeichneEinzelneSIGNALKurve(g2, i1);
                } catch (Exception e) {
                    // ignored: best-effort curve rendering
                }  // SIGNAL --> siehe Implementierung gleich unten
            } else {
                int anzKurvenpunkteImWorksheet = worksheetData.getColumnLength();
                if (!_usesExternalData) {
                    anzKurvenpunkteImWorksheet = _zvCounter;
                }
                try {
                    drawSingleCurve(g2, i1, anzKurvenpunkteImWorksheet);
                } catch (Exception e) {
                    // ignored: best-effort curve rendering
                }  // ZV --> ist Standard in 'GraferV3'
                //----------
                if ((i1 > 0) && (_yAxisY[indexCurveAssociatedYAxis[i1]] != _yAxisY[indexCurveAssociatedYAxis[i1 - 1]])) {
                    zd = 0;
                } else {
                    if (i1 > 0) {
                        zd++;
                    }
                }
                this.beschrifteNamenDerEinzelnenZVKurve(g2, i1, zd);  // ZV curve labeling: for the sake of generality, is not implemented in 'GraferV3' but rather below
            }
        }
    }

    //----------
    // i1 ... KurvenNummer
    private void beschrifteNamenDerEinzelnenZVKurve(final Graphics2D g2D, final int i1, final int zd) {
        //--------------------------------
        int yLinksObenKurve = _yAxisY[indexCurveAssociatedYAxis[i1]] - heightPix[indexCurveAssociatedYAxis[i1]];
        String name = signalNamen[indexDerKurveInDerMatrix[i1] % 1000];
        cf.setMaximumDigits(4);
        String wert = cf.formatT(ySchieberWert[indexDerKurveInDerMatrix[i1] % 1000 - 1][0], TechFormat.FORMAT_AUTO);

        if (inDiffMode) {
            int index = indexDerKurveInDerMatrix[i1] % 1000 - 1;
            wert = cf.formatT(ySchieberWert2[index][0] - ySchieberWert[index][0], TechFormat.FORMAT_AUTO);
        }

        // Labeling of the curve names of the ZV curves in the graph-->
        int delta = 16;  // Distance between the signal names in the graph legend
        g2D.setColor(curveColor[i1]);
        if (xSliderActive) {
            Font oldFont = g2D.getFont();
            Font tmpFont = new Font("Arial", Font.PLAIN, 9);
            g2D.setFont(tmpFont);

            g2D.drawString(name + " =", this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y, yLinksObenKurve + g2D.getFont().getSize() + 2 * zd * delta);
            String labelString = "";
            if (inDiffMode) {
                labelString += "diff ";
            }
            labelString += wert;
            g2D.drawString(labelString, this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y, yLinksObenKurve + g2D.getFont().getSize() + 2 * zd * delta + delta);
            g2D.setFont(oldFont);
        } else {
            g2D.drawString(name, this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y, yLinksObenKurve + g2D.getFont().getSize() + zd * delta);
        }
        //--------------------------------
    }

    // Ueberschrieben, damit man einfach einen 'Grid' fuer SIGNAL-Kurven zeichen kann -->
    @Override
    protected void drawCoordinateAxes(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        calculateSliderValues();
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
        //
        //


        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            if (zeichneDiagrammUmrandung[i1]) {
                g2.setColor(Color.lightGray);
                g2.drawRect(_xAxisX[i1], _xAxisY[i1] - heightPix[i1], widthPix[i1], heightPix[i1]);
            }
        }
        //===============================================
        //
        for (int i1 = 0; i1 < numAxesX; i1++) {
            drawSingleCoordinateAxisX(g2, i1);
        }
        // if the grid lines are hidden (automatically because the diagram is too small in pixel points),
        for (int i1 = 0; i1 < numAxesY; i1++) {
            if (diagramTyp[i1] == DIAGRAM_TYP_ZV) {
                drawSingleCoordinateAxisY(g2, i1);  // hier werden auch die Ticks fuer den Grid berechnet
            } else {
                zeichneEinzelneSIGNALKoordinatenAchse_Y(g2, i1);
            }
        }
        //------------------------
        drawGridNormalX(g);
        drawGridNormalY(g);
        //===============================================
        // x-axes --> there is no messing around (it's the same for ZV and SIGNAL)
        // ueberdeckt werden
        // x-axes --> there is no messing around (it's the same for ZV and SIGNAL)
        // gestellt werden, weil in 'this.drawSingleCoordinateAxisX(g2,i1)' und 'this.drawSingleCoordinateAxisY(g2,i1)' zuerst einmal
        // y-axes --> is different for SIGNAL (grid and labels)
        //
        final GeneralPath grL = new GeneralPath();
        //
        for (int i1 = 0; i1 < numAxesX; i1++) {
            if (lineStyleAxesX[i1] == INVISIBLE) {
                continue;
            }
            g2.setColor(colorAxesX[i1]);
            // TODO: replace with switch statement
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
            // TODO: replace with switch statement
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
            // TODO: replace with switch statement
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
            // TODO: replace with switch statement
            grL.reset();
            grL.moveTo(_yAxisX[i1], _yAxisY[i1]);
            grL.lineTo(_yAxisX[i1], _yAxisY[i1] - heightPix[i1]);
            g2.draw(grL);
            g2.drawString(yAxisLabel[i1], _yAxisX[i1] - posYtickLabels[i1], _yAxisY[i1] - heightPix[i1] / 2);
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        //==================================
    }

    //-----------------------
    protected void zeichneEinzelneSIGNALKoordinatenAchse_Y(final Graphics2D g2, final int i1) {

        GeneralPath grL = new GeneralPath();
        // i1 ... AchsenNummer --> Achtung: pro Graph gibt es je zwei y-Achsen
        // now draw the line:
        //==================================
        // now draw the line:
        int z = 0;
        for (int curveIndex : indexDerKurveInDerMatrix) {
            final int grf = curveIndex / 1000;
            if (grf == i1 / 2) {
                z++;
            }
        }
        //------------------------
        //
        final int anzTicks = 2 * z;
        valueTickY[i1] = new double[anzTicks];  // zum Tick gehoeriger y-Zahlenwert --> wird hier nicht verwendet
        tickY[i1] = new int[anzTicks];  // Pixel-Position
        tickY[i1][0] = _yAxisY[i1];
        for (int i2 = 0; i2 < anzTicks; i2++) {
            if (i2 % 2 == 0) {
                valueTickY[i1][i2] = 0.0;
                if (i2 > 0) {
                    tickY[i1][i2] = tickY[i1][i2 - 1] - sgnDistance[i1];
                }
            } else {
                valueTickY[i1][i2] = 1.0;
                tickY[i1][i2] = tickY[i1][i2 - 1] - sgnHeight[i1];
            }
        }
        // keine Minor-Ticks bei SIGNAL -->
        final int yMinorTicksAnzahl = 0;
        valueTickYminor[i1] = new double[yMinorTicksAnzahl];
        tickYminor[i1] = new int[yMinorTicksAnzahl];
        //==================================
        if (i1 % 2 != 0) {
            return;  // only the left y-axis is drawn!
        }        //
        if (lineStyleAxesY[i1] == INVISIBLE) {
            return;
        }
        g2.setColor(colorAxesY[i1]);

        // TODO: replace with switch expression!
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
        } else {
            LOGGER.info("Fehler: hhqqt5");
        }
        //-----------------------
        // TODO: replace with switch expression!
        grL.reset();
        grL.moveTo(_yAxisX[i1], _yAxisY[i1]);
        grL.lineTo(_yAxisX[i1], _yAxisY[i1] - heightPix[i1]);
        g2.draw(grL);
        g2.drawString(yAxisLabel[i1], _yAxisX[i1] - posYtickLabels[i1], _yAxisY[i1] - heightPix[i1] / 2);
        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        //==================================
    }

    private void reorderLine(final int[] positionSIGNAL, int z1, int z2) {
        //-----------------------
        int[] toBeOrdered = new int[z2 - z1];
        int anzAlteEintraege = 0;  // ungleich '-1'
        for (int i1 = z1; i1 <= z2 - 1; i1++) {
            toBeOrdered[i1 - z1] = positionSIGNAL[i1];
            if (positionSIGNAL[i1] != -1) {
                anzAlteEintraege++;
            }
        }
        // zuerst alles ungleich '-1' durchgehend aufsteigend nummerieren -->
        for (int zahl = 0; zahl < anzAlteEintraege; zahl++) {
            boolean noX = true;
            while (noX) {
                boolean lokNoX = true;
                for (int value : toBeOrdered) {
                    if (value == zahl) {
                        lokNoX = false;
                        noX = false;
                    }
                }
                if (lokNoX) {
                    for (int i1 = 0; i1 < toBeOrdered.length; i1++) {
                        if ((toBeOrdered[i1] != -1) && (toBeOrdered[i1] > zahl)) {
                            toBeOrdered[i1]--;
                        }
                    }
                }
            }
        }
        //System.out.print("reorderLine() >>  ");  for (int i1=0;  i1<toBeOrdered.length;  i1++) System.out.print(toBeOrdered[i1]+"  "); System.out.println();
        //-----------
        // jetzt alles '-1' an die alten Werte anschliessend aufsteigend nummerieren -->
        for (int i1 = 0; i1 < toBeOrdered.length; i1++) {
            if (toBeOrdered[i1] == -1) {
                toBeOrdered[i1] = anzAlteEintraege;
                anzAlteEintraege++;
            }
        }
        //System.out.print("reorderLine() >>  ");  for (int i1=0;  i1<toBeOrdered.length;  i1++) System.out.print(toBeOrdered[i1]+"  "); System.out.println();
        //-----------
        for (int i1 = z1; i1 <= z2 - 1; i1++) {
            positionSIGNAL[i1] = toBeOrdered[i1 - z1];
        }
        //-----------
    }

    // now number everything '-1' after the old values ​​in ascending order -->
    //-----------
    //-----------
    private void setzeYPositionDerSIGNALverlaeufe() {
        //-------------------------------------
        if (positionSIGNAL != null) {
            //------------------------
            //-----------
            positionSIGNAL = new int[kurvenanzahl];
            for (int i1 = 0; i1 < kurvenanzahl; i1++) {
                positionSIGNAL[i1] = -1;
            }
            // // therefore the old value of 'indexDerKurveInDerMatrix[]' must be saved and a corresponding conversion must be carried out
            // therefore the old value of 'indexDerKurveInDerMatrix[]' must be saved and a corresponding conversion must be carried out
            for (int i1 = 0; i1 < indexDerKurveInDerMatrix.length; i1++) {
                for (int i2 = 0; i2 < indexDerKurveInDerMatrixALT.length; i2++) {
                    if (indexDerKurveInDerMatrix[i1] == indexDerKurveInDerMatrixALT[i2]) {
                        positionSIGNAL[i1] = positionSIGNAL_ALT[i2];
                    }
                }
            }
            //
            // old SIGNAL positions are copied into the new 'positionSIGNAL' field,
            // the values ​​that have not been overwritten are still marked with a negative sign
            int z1 = 0, z2 = 0;
            while (z2 < kurvenanzahl) {
                while ((z2 < kurvenanzahl) && (indexDerKurveInDerMatrix[z1] / 1000 == indexDerKurveInDerMatrix[z2] / 1000)) {
                    z2++;
                }
                this.reorderLine(positionSIGNAL, z1, z2);
                z1 = z2;
            }

        } else {
            //-------------------------------------
            positionSIGNAL = new int[kurvenanzahl];
            int positionsZaehler = 0;
            positionSIGNAL[0] = positionsZaehler;
            positionsZaehler++;
            for (int i1 = 1; i1 < kurvenanzahl; i1++) {
                if (indexDerKurveInDerMatrix[i1] / 1000 != indexDerKurveInDerMatrix[i1 - 1] / 1000) {
                    positionsZaehler = 0;  // Reset bei neuem Graph
                }
                positionSIGNAL[i1] = positionsZaehler;
                positionsZaehler++;
            }
            //-------------------------------------
            //
            this.speichereALTeWerteFuerPosition();
            //-------------------------------------
        }
        //for (int i1=0;  i1<positionSIGNAL.length;  i1++) System.out.println(i1+"  (1)  "+positionSIGNAL[i1]);  System.out.println("-----------");
        //System.out.println("******************************");
    }

    // Note: the curve in the matrix on the far left always has 'positionSIGNAL==0'
    public int[] getPositionSIGNAL() {
        return positionSIGNAL;
    }

    public void setPositionSIGNAL(final int[] positionSIGNAL) {
        this.positionSIGNAL = positionSIGNAL;
        this.speichereALTeWerteFuerPosition();
    }

    private void speichereALTeWerteFuerPosition() {
        if ((positionSIGNAL == null) || (indexDerKurveInDerMatrix == null)) {
            return;
        }
        //-------------------------------------
        positionSIGNAL_ALT = new int[positionSIGNAL.length];
        System.arraycopy(positionSIGNAL, 0, positionSIGNAL_ALT, 0, positionSIGNAL.length);
        //-------------------------------------
        // eine Kopie von 'indexDerKurveInDerMatrix' aufheben, damit Darstellungs-Abfolge von SIGNAL korrekt erfolgen kann:
        indexDerKurveInDerMatrixALT = new int[indexDerKurveInDerMatrix.length];
        System.arraycopy(indexDerKurveInDerMatrix, 0, indexDerKurveInDerMatrixALT, 0, indexDerKurveInDerMatrix.length);
        //-------------------------------------
    }

    public void calculateRequiredHeightSignalGraph() {
        //-------------------------------------
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            notwendigeHoehePixGRF[i1] = -1;  // default --> kein SIGNAL-Graph sondern ZV-Graph
            if (diagramTyp[i1] == DIAGRAM_TYP_SGN) {
                int anzSGN = 0;  // Number of SIGNAL curves per SIGNAL graph
                for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                    if (crvAchsenTyp[i1][i2] == ZUORDNUNG_SIGNAL) {
                        anzSGN++;
                    }
                }
                notwendigeHoehePixGRF[i1] = anzSGN * (sgnHeight[i1] + sgnDistance[i1]);
                notwendigeHoehePixGRF[i1] += (DY_IN_OBEN + DY_IN_UNTEN);
                //System.out.println(notwendigeHoehePixGRF[i1]+"   "+i1+"   "+anzSGN);
            }
        }
    }

    private int getHeightForZVInPixels() {
        // die Hoehe, die fuer die ZVs zur Verfuegung steht, dh. Gesamthoehe minus SIGNAL-Hoehen -->
        int height = this.getHeight() - ABSTAND_BESCHRIFTUNG_XACHSE;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == DIAGRAM_TYP_SGN) {
                height -= notwendigeHoehePixGRF[i1];
            }
        }
        return height;
    }

    private void zeichneEinzelneSIGNALKurve(Graphics2D g2, int i1) {

        final GeneralPath grL = new GeneralPath();
        // i1 ... KurvenNummer
        //===============================================
        // the height available for the ZVs, ie. Total height minus SIGNAL heights -->
        //----------------------
        final int[] xPix = new int[_zvCounter];
        final int[] yPix = new int[_zvCounter];
        final int x0Kurve = _xAxisX[indexCurveAssociatedXAxis[i1]];  // zugehoerige x-Achse definiert x0 der Kurve
        final int y0Kurve = _yAxisY[indexCurveAssociatedYAxis[i1]] - (notwendigeHoehePixGRF[indexCurveAssociatedXAxis[i1]] - (DY_IN_OBEN + DY_IN_UNTEN));  // zugehoerige y-Achse definiert y0 der Kurve, 'notwendigeHoehePixGRF[i1]' zur optischen Invertierung (Kurve links ganz oben)
        final int delta = sgnDistance[indexCurveAssociatedXAxis[i1]] + sgnHeight[indexCurveAssociatedXAxis[i1]];

        //
        for (int i2 = 0; i2 < _zvCounter; i2++) {
            final double xValue = worksheetData.getValue(curve_index_worksheetColumns_XY[i1][0], i2);
            if (xAxisType[indexCurveAssociatedXAxis[i1]] == ACHSE_LIN) {
                xPix[i2] = x0Kurve + (int) (sfX[indexCurveAssociatedXAxis[i1]] * (xValue - axisXmin[indexCurveAssociatedXAxis[i1]]));
            } else if (xAxisType[indexCurveAssociatedXAxis[i1]] == ACHSE_LOG) {
                xPix[i2] = x0Kurve + (int) (sfX[indexCurveAssociatedXAxis[i1]] * this.lg10(xValue / axisXmin[indexCurveAssociatedXAxis[i1]]));
            }
            //------------------
            double yValue = worksheetData.getValue(curve_index_worksheetColumns_XY[i1][1], i2);
            //
            try {
                if (yValue < sgnSchwelle[indexCurveAssociatedXAxis[i1]]) {
                    yValue = positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]];
                } else {
                    yValue = (positionSIGNAL[i1] * delta);
                }
                yPix[i2] = y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]] + (int) yValue;
            } catch (Exception e) {
                LOGGER.info("Fehler: 5z6z4r447 " + e + "    kurvenanzahl= " + kurvenanzahl + "      i1= " + i1 + "     " + positionSIGNAL.length);
            }
        }
        //--------------------------------
        g2.setColor(curveColor[i1]);

        // TODO: replace with switch statement
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
            assert false;
        }
        //-----------------------
        // TODO: replace with switch statement
        final String name = signalNamen[indexDerKurveInDerMatrix[i1] % 1000];
        if (xSliderActive) {
            g2.drawString(
                    ySchieberWert[indexDerKurveInDerMatrix[i1] % 1000 - 1][0] < sgnSchwelle[indexCurveAssociatedXAxis[i1]] ? "off" : "on",
                    DX_IN_LINKS - 30,
                    y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]] + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]]));
        }
        g2.drawString(
                name,
                this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y,
                y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]] + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]]));
        //-----------------------
        // for labeling the SIGNAL-ZV in the graph -->
        g2.setClip(x0Kurve + 1, 0, widthPix[indexCurveAssociatedYAxis[i1]] - 2, 10000);

        grL.reset();
        if (curveLineStyle[i1] != INVISIBLE) {
            grL.moveTo(xPix[0], yPix[0]);
            for (int i5 = 1; i5 < _zvCounter; i5++) {
                if (yPix[i5] != yPix[i5 - 1]) {  // Switching process is implemented in the middle between 2 data points --> visual improvement
                    grL.lineTo((xPix[i5 - 1] + xPix[i5]) / 2.0, yPix[i5 - 1]);
                    grL.lineTo((xPix[i5 - 1] + xPix[i5]) / 2.0, yPix[i5]);
                }
                grL.lineTo(xPix[i5], yPix[i5]);
            }
            //---------------
            // now draw the SIGNAL line:
            final GeneralPath grFill = new GeneralPath();
            grFill.append(grL.getPathIterator(null), false);
            final int nullLinie = y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]] + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]]);
            grFill.lineTo(xPix[_zvCounter - 1], nullLinie);
            grFill.lineTo(xPix[0], nullLinie);
            if (yPix[0] < nullLinie) {
                grFill.lineTo(xPix[0], yPix[0]);
            }
            //grFill.closePath();
            //---------------
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / 1000);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % 1000);
            if (crvFillDigitalCurves[im1][im2]) {
                final Color fuellFarbe = selectColor(crvFillingDigitalColor[im1][im2]);
                g2.setColor(fuellFarbe);
                g2.fill(grFill.createTransformedShape(null));
            }
            //---------------
            g2.setColor(curveColor[i1]);

            final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
            g2.setComposite(ac);
            g2.draw(grL);
            //---------------
        }
        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        g2.setClip(null);

        final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        g2.setComposite(ac);

        //===============================================
    }

    //---------------
    public void definiereAchsenbegrenzungenImAutoZoom() {
        this.definiereAchsenbegrenzungenImAutoZoom(worksheetData);
    }

    private void definiereAchsenbegrenzungenImAutoZoom(DataContainer ws) {
        //--------------------------
        final double[] tickAbstandY = new double[ANZ_DIAGRAM_MAX];
        //--------------------------
        // zur Effizienz-Steigerung: pro WS-Spalte werden kleinster und groesster Wert bestimmt -->
        final double[] w1 = new double[ws.getRowLength()], w2 = new double[ws.getRowLength()];
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {
            w1[i1] = +1e99;
            w2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {  // geht durch die Spalten
            for (int i2 = 0; i2 < _zvCounter; i2++) {  // goes through the selected column line by line
                if (ws.getValue(i1, i2) < w1[i1]) {
                    w1[i1] = ws.getValue(i1, i2);
                }
                if (ws.getValue(i1, i2) > w2[i1]) {
                    w2[i1] = ws.getValue(i1, i2);
                }
            }
        }
        //--------------------------
        for (int i1 = 0; i1 < minX.length; i1++) {
            minX[i1] = +1e99;
            maxX[i1] = -1e99;
            minY[i1] = +1e99;
            maxY[i1] = -1e99;
        }
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {   // goes through the lines
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_X) {
                    minX[i1] = w1[i2];
                    maxX[i1] = w2[i2];
                    //--------------------------
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_Y) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                    // --> sufficient because there is only one X-axis per matrix row
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_SIGNAL) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                    // --> Comparison with the limitations of any other Y-axes
                }
            }
            // --> Comparison with the limitations of any other Y-axes
            // --> Comparison with the limitations of any other Y-axes
            //if ((minY2[i1]==+1e99)||(maxY2[i1]==-1e99)) { minY2[i1]=minY[i1];   maxY2[i1]=maxY[i1]; }
            //if ((minY[i1] ==+1e99)||(maxY[i1] ==-1e99)) { minY[i1]= minY2[i1];  maxY[i1]= maxY2[i1]; }
            // 'schoenere' Bereichsgrenzen -->
            final double[] autoEmpf = auto_Achsenbegrenzung_Wertempfehlung(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            tickAbstandY[i1] = autoEmpf[4];
        }
        //--------------------------
        double[] xx1 = new double[anzGrfVisible], xx2 = new double[anzGrfVisible];  // X-Achse
        double[] yy1 = new double[anzGrfVisible], yy2 = new double[anzGrfVisible];  // Y-Achse  --> Muss noch individuell angepasst werden!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // is auto-scaling turned on?
        for (int i1 = 0; i1 < xx1.length; i1++) {
            xx1[i1] = minX[i1];
            xx2[i1] = maxX[i1];
            scX[i1] = autoScaleX[i1];
        }
        for (int i1 = 0; i1 < yy1.length; i1++) {
            yy1[i1] = minY[i1];
            yy2[i1] = maxY[i1];
            scY[i1] = autoScaleY[i1];
        }
        this.setzeAchsenBegrenzungen(xx1, xx2, scX, yy1, yy2, scY);
        //-------------------
        // initAutoTickSpacing() -->
        for (int i1 = 0; i1 < ANZ_DIAGRAM_MAX; i1++) {
            xTickSpacing[i1] = this.getAutoTickSpacingX(i1);
            yTickSpacing[i1] = tickAbstandY[i1];
        }
        this.setzeTickSpacing(xTickSpacing, yTickSpacing);
        //
        repaint();
        //--------------------------
    }

    private void definiereAchsenbegrenzungenNumerischeSimulation(double t1, double t2) {
        //--------------------------
        final DataContainer ws = this.worksheetData;
        final double[] tickAbstandY = new double[ANZ_DIAGRAM_MAX];
        //--------------------------
        // zur Effizienz-Steigerung:
        // pro WS-Spalte werden kleinster und groesster Wert bestimmt -->
        final double[] w1 = new double[worksheetData.getRowLength()], w2 = new double[worksheetData.getRowLength()];
        for (int i1 = 0; i1 < w1.length; i1++) {
            w1[i1] = +1e99;
            w2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {  // goes through the columns
            for (int i2 = 0; i2 < _zvCounter + 1; i2++) {  // goes through the selected column line by line
                if (ws.getValue(i1, i2) < w1[i1]) {
                    w1[i1] = ws.getValue(i1, i2);
                }
                if (ws.getValue(i1, i2) > w2[i1]) {
                    w2[i1] = ws.getValue(i1, i2);
                }
            }
        }
        //--------------------------
        for (int i1 = 0; i1 < minX.length; i1++) {
            minY[i1] = +1e99;
            maxY[i1] = -1e99;   // minX[i1]=+1e99;   maxX[i1]=-1e99;
        }
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {   // goes through the lines
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_X) {
                    minX[i1] = t1;
                    maxX[i1] = t2;
                    //--------------------------
                    if (minX[i1] == maxX[i1]) {
                        minX[i1] = 0;
                        maxX[i1] = 0.020;
                    }
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_Y) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                }
            }
            // --> sufficient because there is only one X-axis per matrix row
            // --> sufficient because there is only one X-axis per matrix row
            //if ((minY2[i1]==+1e99)||(maxY2[i1]==-1e99)) { minY2[i1]=minY[i1];   maxY2[i1]=maxY[i1]; }
            //if ((minY[i1] ==+1e99)||(maxY[i1] ==-1e99)) { minY[i1]= minY2[i1];  maxY[i1]= maxY2[i1]; }
            // 'schoenere' Bereichsgrenzen -->
            final double[] autoEmpf = auto_Achsenbegrenzung_Wertempfehlung(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            tickAbstandY[i1] = autoEmpf[4];
        }
        //--------------------------
        // CLIPPING:  Kann erst aufgerufen werden, wenn 'worksheet' und 'minX[],maxX[],...' definiert sind -->
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_X) || (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_Y)) {
                    crvClipValXmin[i1][i2] = this.getXClipAchse(i1, i2)[0];
                    crvClipValXmax[i1][i2] = this.getXClipAchse(i1, i2)[1];
                    crvClipValYmin[i1][i2] = this.getYClipAchse(i1, i2)[0];
                    crvClipValYmax[i1][i2] = this.getYClipAchse(i1, i2)[1];
                }
            }
        }
        //----------------------------------------------
        // initAutoTickSpacing() -->
        //
        for (int i1 = 0; i1 < ANZ_DIAGRAM_MAX; i1++) {
            xTickSpacing[i1] = this.getAutoTickSpacingX(i1);
            yTickSpacing[i1] = tickAbstandY[i1];  // this.getAutoTickSpacingY(i1);
        }
        repaint();
        //--------------------------
    }

    @Override
    public void setCurves() {
        if (matrixZuordnungKurveDiagram == null) {
            return;
        }
        //-------------------------------------
        kurvenanzahl = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_Y) || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_SIGNAL)) {
                    kurvenanzahl++;
                }
            }
        }
        this.setCurvesCount(kurvenanzahl);
        //-------------------------------------
        this.speichereALTeWerteFuerPosition();
        //
        indexDerKurveInDerMatrix = new int[kurvenanzahl];  // to assign the curve indices to the assignment matrix
        //
        int[] zugehoerigkeitX = new int[kurvenanzahl];
        int[] zugehoerigkeitY = new int[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            zugehoerigkeitX[i1] = -1;
            zugehoerigkeitY[i1] = -1;
        }

        for (int kurvenIndex = 0; kurvenIndex < kurvenanzahl; kurvenIndex++) {
            for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
                final int zugX = i1;  // because all curves of a graph see the same x-axis
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if ((matrixZuordnungKurveDiagram[i1][i2] == this.ASSIGNMENT_Y) || (matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_SIGNAL)) {
                        zugehoerigkeitX[kurvenIndex] = zugX;
                        zugehoerigkeitY[kurvenIndex] = i1;
                        indexDerKurveInDerMatrix[kurvenIndex] = 1000 * i1 + i2;
                        kurvenIndex++;
                    }
                }
            }
        }
        this.setCurveAxesAssignment(zugehoerigkeitX, zugehoerigkeitY);
        //-------------------------------------
        this.setzeYPositionDerSIGNALverlaeufe();
        //-------------------------------------
        indexWsXY = new int[kurvenanzahl][2];
        //
        for (int kurvenIndex = 0; kurvenIndex < kurvenanzahl; kurvenIndex++) {
            for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
                int zugX = -1;
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if (matrixZuordnungKurveDiagram[i1][i2] == this.ASSIGNMENT_X) {
                        zugX = i2;
                    }
                }
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if ((matrixZuordnungKurveDiagram[i1][i2] == this.ASSIGNMENT_Y) || (matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_SIGNAL)) {
                        indexWsXY[kurvenIndex][0] = zugX;
                        indexWsXY[kurvenIndex][1] = i2;
                        kurvenIndex++;
                    }
                }
            }
        }
        this.setCurveIndexWorksheetColumnsXY(indexWsXY);
        //=====================================
        int[] crvLineStyleLok = new int[kurvenanzahl];
        int[] crvLineColorLok = new int[kurvenanzahl];
        final double[] crvTransparencyLok = new double[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / 1000);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % 1000);
            crvLineStyleLok[i1] = crvLineStyle[im1][im2];
            crvLineColorLok[i1] = crvLineColor[im1][im2];
            crvTransparencyLok[i1] = crvTransparency[im1][im2];
        }
        this.setCurveLineStyle(crvLineStyleLok);
        //
        Color[] linienFarbe = new Color[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            linienFarbe[i1] = selectColor(crvLineColorLok[i1]);
        }
        this.setCurveColor(linienFarbe);
        this.setCurveTransparency(crvTransparencyLok);
        //=====================================
        boolean[] crvSymbShowLok = new boolean[kurvenanzahl];
        int[] crvSymbFrequLok = new int[kurvenanzahl];
        int[] crvSymbShapeLok = new int[kurvenanzahl];
        int[] crvSymbColorLok = new int[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / 1000);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % 1000);
            crvSymbShowLok[i1] = crvSymbShow[im1][im2];
            crvSymbFrequLok[i1] = crvSymbFrequ[im1][im2];
            crvSymbShapeLok[i1] = crvSymbShape[im1][im2];
            crvSymbColorLok[i1] = crvSymbColor[im1][im2];
        }
        Color[] crvSymbFarbeLok = new Color[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            crvSymbFarbeLok[i1] = selectColor(crvSymbColorLok[i1]);
        }
        this.setCurvePointSymbolVisible(crvSymbShowLok, crvSymbFrequLok, crvSymbShapeLok, crvSymbFarbeLok);
        //=====================================
        // welche Art von Clipping (Achse, kein Clipping, Wert) ?  -->
        int[] crvClipXminLok = new int[kurvenanzahl], crvClipXmaxLok = new int[kurvenanzahl], crvClipYminLok = new int[kurvenanzahl], crvClipYmaxLok = new int[kurvenanzahl];
        // falls Clipping auf Wert, welcher konkrete Zahlenwert ?  -->
        double[] crvClipValXminLok = new double[kurvenanzahl], crvClipValXmaxLok = new double[kurvenanzahl];
        double[] crvClipValYminLok = new double[kurvenanzahl], crvClipValYmaxLok = new double[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / 1000);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % 1000);
            crvClipXminLok[i1] = crvClipXmin[im1][im2];
            crvClipXmaxLok[i1] = crvClipXmax[im1][im2];
            crvClipYminLok[i1] = crvClipYmin[im1][im2];
            crvClipYmaxLok[i1] = crvClipYmax[im1][im2];
            crvClipValXminLok[i1] = crvClipValXmin[im1][im2];
            crvClipValXmaxLok[i1] = crvClipValXmax[im1][im2];
            crvClipValYminLok[i1] = crvClipValYmin[im1][im2];
            crvClipValYmaxLok[i1] = crvClipValYmax[im1][im2];
        }
        this.setCurveClipping(crvClipValXminLok, crvClipValXmaxLok, crvClipValYminLok, crvClipValYmaxLok, crvClipXminLok, crvClipXmaxLok, crvClipYminLok, crvClipYmaxLok);
        //=====================================
    }

    @Override
    public void setAxes() {
        //-------------------------------------
        anzGrfVisible = 0;  // Number of graphs to display (i.e. visible==true)
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            anzGrfVisible++;
        }
        this.setAxesCount(anzGrfVisible, anzGrfVisible);
        //
        zeichneDiagrammUmrandung = new boolean[anzGrfVisible];
        //-----------
        //=====================================
        final int breite = this.getWidth(), hoehe = this.getHeightForZVInPixels();
        xGrfMIN = new int[anzGrfVisible];
        xGrfMAX = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xGrfMIN.length; i1++) {
            xGrfMIN[i1] = 0;
            xGrfMAX[i1] = this.getWidth();
        }
        yGrfMIN = new int[anzGrfVisible];
        yGrfMAX = new int[anzGrfVisible];
        double ySpGes = 0;   // Weighting of the y-axes
        int iyy = 1;  // Index for y-axis
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == DIAGRAM_TYP_ZV) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        yGrfMIN[0] = 0;
        yGrfMAX[0] = (diagramTyp[0] == DIAGRAM_TYP_ZV) ? 0 + (int) (hoehe * (ySpacingDiagram[0] / ySpGes)) : 0 + notwendigeHoehePixGRF[0];
        for (int i1 = 1; i1 < this.getAnzahlDiagramme(); i1++) {
            yGrfMIN[iyy] = yGrfMAX[iyy - 1];
            yGrfMAX[iyy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? yGrfMIN[iyy] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : yGrfMIN[iyy] + notwendigeHoehePixGRF[i1];
            iyy++;
        }
        //-----------
        int[] laenge_xAchse = new int[anzGrfVisible], posX_xAchse = new int[anzGrfVisible], posY_xAchse = new int[anzGrfVisible];
        int[] laenge_yAchse = new int[anzGrfVisible], posX_yAchse = new int[anzGrfVisible], posY_yAchse = new int[anzGrfVisible];
        int ix = 0, iy = 0;  // Index for x and y axes
        ySpGes = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == DIAGRAM_TYP_ZV) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            // X-Achse:
            laenge_xAchse[ix] = breite - (DX_IN_LINKS + DX_IN_RECHTS);
            posX_xAchse[ix] = DX_IN_LINKS;
            if (ix == 0) {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - DY_IN_UNTEN) : notwendigeHoehePixGRF[i1] - DY_IN_UNTEN;
            } else {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? posY_xAchse[ix - 1] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : posY_xAchse[ix - 1] + notwendigeHoehePixGRF[i1];
            }
            ix++;
            // Y-Achse:
            laenge_yAchse[iy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - (DY_IN_OBEN + DY_IN_UNTEN)) : notwendigeHoehePixGRF[i1] - (DY_IN_OBEN + DY_IN_UNTEN);
            posX_yAchse[iy] = posX_xAchse[ix - 1];
            posY_yAchse[iy] = posY_xAchse[ix - 1];
            iy++;
        }
        this.setAxisWidthHeightX0Y0(laenge_xAchse, laenge_yAchse, posX_xAchse, posY_xAchse, posX_yAchse, posY_yAchse);
        //-----------
        double[] x1 = new double[anzGrfVisible], x2 = new double[anzGrfVisible];  // X-Achse
        double[] y1 = new double[anzGrfVisible], y2 = new double[anzGrfVisible];  // Y-Achse  --> Muss noch individuell angepasst werden!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // is auto-scaling turned on?
        for (int i1 = 0; i1 < x1.length; i1++) {
            x1[i1] = minX[i1];
            x2[i1] = maxX[i1];
            scX[i1] = autoScaleX[i1];
        }
        for (int i1 = 0; i1 < y1.length; i1++) {
            y1[i1] = minY[i1];
            y2[i1] = maxY[i1];
            scY[i1] = autoScaleY[i1];
        }
        this.setzeAchsenBegrenzungen(x1, x2, scX, y1, y2, scY);
        //-----------
        final String[] xAchseBeschriftungLok = new String[anzGrfVisible];
        final String[] yAchseBeschriftungLok = new String[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseBeschriftungLok.length; i1++) {
            xAchseBeschriftungLok[i1] = xAxisLabel[i1];
        }
        for (int i1 = 0; i1 < yAchseBeschriftungLok.length; i1++) {
            yAchseBeschriftungLok[i1] = yAxisLabel[i1];
        }
        this.setAxesLabels(xAchseBeschriftungLok, yAchseBeschriftungLok);
        //-----------
        int[] xAchseTypLoc = new int[anzGrfVisible], yAchseTypLoc = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseTypLoc.length; i1++) {
            xAchseTypLoc[i1] = xAchsenTyp[i1];
        }
        for (int i1 = 0; i1 < yAchseTypLoc.length; i1++) {
            yAchseTypLoc[i1] = yAchsenTyp[i1];
        }
        this.setAxesType(xAchseTypLoc, yAchseTypLoc);
        //-----------
        Color[] xAchseFarbeLok = new Color[anzGrfVisible], yAchseFarbeLok = new Color[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseFarbeLok.length; i1++) {
            xAchseFarbeLok[i1] = selectColor(xAchseFarbe[i1]);
        }
        for (int i1 = 0; i1 < yAchseFarbeLok.length; i1++) {
            yAchseFarbeLok[i1] = selectColor(yAchseFarbe[i1]);
        }
        this.setAxisColor(xAchseFarbeLok, yAchseFarbeLok);
        //-----------
        int[] xAchseStilLok = new int[anzGrfVisible], yAchseStilLok = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseStilLok.length; i1++) {
            xAchseStilLok[i1] = xAchseStil[i1];
        }
        for (int i1 = 0; i1 < yAchseStilLok.length; i1++) {
            yAchseStilLok[i1] = yAchseStil[i1];
        }
        this.setAxesLineStyle(xAchseStilLok, yAchseStilLok);
        //-----------
        //
        gridNormalX_associatedXAxis = new int[anzGrfVisible];  // Grid normal auf X-Achse
        gridNormalX_associatedYAxis = new int[anzGrfVisible];  // Grid normal auf X-Achse
        for (int i1 = 0; i1 < gridNormalX_associatedXAxis.length; i1++) {
            gridNormalX_associatedXAxis[i1] = i1;
        }
        for (int i1 = 0; i1 < gridNormalX_associatedYAxis.length; i1++) {
            gridNormalX_associatedYAxis[i1] = i1;
        }
        this.definiereGridNormalX(gridNormalX_associatedXAxis, gridNormalX_associatedYAxis);
        //
        gridNormalY_associatedXAxis = new int[anzGrfVisible];  // Grid normal auf Y-Achse
        gridNormalY_associatedYAxis = new int[anzGrfVisible];  // Grid normal auf Y-Achse
        for (int i1 = 0; i1 < gridNormalY_associatedXAxis.length; i1++) {
            gridNormalY_associatedXAxis[i1] = i1;
        }
        for (int i1 = 0; i1 < gridNormalY_associatedYAxis.length; i1++) {
            gridNormalY_associatedYAxis[i1] = i1;
        }
        this.definiereGridNormalY(gridNormalY_associatedXAxis, gridNormalY_associatedYAxis);
        //
        final Color[] farbeGridNormalXLok = new Color[anzGrfVisible];
        final Color[] farbeGridNormalXminorLok = new Color[farbeGridNormalXLok.length];
        for (int i1 = 0; i1 < farbeGridNormalXLok.length; i1++) {
            farbeGridNormalXLok[i1] = selectColor(colorGridNormalX[i1]);
            farbeGridNormalXminorLok[i1] = selectColor(colorGridNormalXminor[i1]);
        }
        final Color[] farbeGridNormalYLok = new Color[anzGrfVisible];
        final Color[] farbeGridNormalYminorLok = new Color[farbeGridNormalYLok.length];
        for (int i1 = 0; i1 < farbeGridNormalYLok.length; i1++) {
            farbeGridNormalYLok[i1] = selectColor(colorGridNormalY[i1]);
            farbeGridNormalYminorLok[i1] = selectColor(colorGridNormalYminor[i1]);
        }
        this.setGridColors(farbeGridNormalXLok, farbeGridNormalYLok, farbeGridNormalXminorLok, farbeGridNormalYminorLok);
        //-----------
        int[] linStilGridNormalXLok = new int[anzGrfVisible];
        int[] linStilGridNormalXminorLok = new int[farbeGridNormalXLok.length];
        for (int i1 = 0; i1 < linStilGridNormalXLok.length; i1++) {
            linStilGridNormalXLok[i1] = lineStyleGridNormalX[i1];
            linStilGridNormalXminorLok[i1] = lineStyleGridNormalXminor[i1];
        }
        final int[] linStilGridNormalYLok = new int[anzGrfVisible];
        final int[] linStilGridNormalYminorLok = new int[farbeGridNormalYLok.length];
        for (int i1 = 0; i1 < linStilGridNormalYLok.length; i1++) {
            linStilGridNormalYLok[i1] = lineStyleGridNormalY[i1];
            linStilGridNormalYminorLok[i1] = lineStyleGridNormalYminor[i1];
        }
        this.setGridLineStyle(linStilGridNormalXLok, linStilGridNormalYLok, linStilGridNormalXminorLok, linStilGridNormalYminorLok);
        //-----------
        final int[][] showGridNormalXmajLok = new int[anzGrfVisible][2], showGridNormalXminLok = new int[anzGrfVisible][2];

        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalX_associatedXAxis[i1];
            final int indexAchseY = gridNormalX_associatedYAxis[i1];
            showGridNormalXmajLok[i1][0] = xShowGridMaj[i1] ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            showGridNormalXmajLok[i1][1] = xShowGridMaj[i1] ? indexAchseY : -1;
            showGridNormalXminLok[i1][0] = xShowGridMin[i1] ? indexAchseX : -1;
            showGridNormalXminLok[i1][1] = xShowGridMin[i1] ? indexAchseY : -1;
        }
        int[][] yShowGridMajor = new int[anzGrfVisible][2], yShowGridMinor = new int[anzGrfVisible][2];

        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalY_associatedXAxis[i1];
            final int indexAchseY = gridNormalY_associatedYAxis[i1];
            yShowGridMajor[i1][0] = yShowGridMaj[i1] ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            yShowGridMajor[i1][1] = yShowGridMaj[i1] ? indexAchseY : -1;
            yShowGridMinor[i1][0] = yShowGridMin[i1] ? indexAchseX : -1;
            yShowGridMinor[i1][1] = yShowGridMin[i1] ? indexAchseY : -1;
        }
        this.showGridLines(showGridNormalXmajLok, showGridNormalXminLok, yShowGridMajor, yShowGridMinor);
        //-----------
        final boolean[] xTickAutoSpacing = new boolean[anzGrfVisible];
        final boolean[] yTickAutoSpacing = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < xTickAutoSpacing.length; i1++) {
            xTickAutoSpacing[i1] = xTickAutoSpacing[i1];
        }
        for (int i1 = 0; i1 < yTickAutoSpacing.length; i1++) {
            yTickAutoSpacing[i1] = yTickAutoSpacing[i1];
        }
        this.setzeTickAutoSpacing(xTickAutoSpacing, yTickAutoSpacing);
        //-----------
        double[] xTickSpacingLok = new double[anzGrfVisible];
        double[] yTickSpacingLok = new double[anzGrfVisible];
        for (int i1 = 0; i1 < xTickSpacingLok.length; i1++) {
            xTickSpacingLok[i1] = xTickSpacing[i1];
        }
        for (int i1 = 0; i1 < yTickSpacingLok.length; i1++) {
            yTickSpacingLok[i1] = yTickSpacing[i1];
        }
        this.setzeTickSpacing(xTickSpacingLok, yTickSpacingLok);
        //-----------
        int[] xAnzTicksMinorLok = new int[anzGrfVisible];
        int[] yAnzTicksMinorLok = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAnzTicksMinorLok.length; i1++) {
            xAnzTicksMinorLok[i1] = xNumTicksMinor[i1];
        }
        for (int i1 = 0; i1 < yAnzTicksMinorLok.length; i1++) {
            yAnzTicksMinorLok[i1] = yNumTicksMinor[i1];
        }
        this.setTickCountMinor(xAnzTicksMinorLok, yAnzTicksMinorLok);
        //-----------
        final int[] xTickLength = new int[anzGrfVisible], xTickLengthMin = new int[anzGrfVisible];
        final int[] yTickLength = new int[anzGrfVisible], yTickLengthMin = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xTickLength.length; i1++) {
            xTickLength[i1] = xTickLength[i1];
            xTickLengthMin[i1] = xTickLengthMinor[i1];
        }
        for (int i1 = 0; i1 < yTickLength.length; i1++) {
            yTickLength[i1] = yTickLength[i1];
            yTickLengthMin[i1] = yTickLengthMinor[i1];
        }
        this.setTickLength(xTickLength, yTickLength, xTickLengthMin, yTickLengthMin);
        //-----------
        final boolean[] showLabelsXMaj = new boolean[anzGrfVisible], showLabelsXMin = new boolean[anzGrfVisible];
        final boolean[] showLabelsYMax = new boolean[anzGrfVisible], showLabelsYMin = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < showLabelsXMaj.length; i1++) {
            showLabelsXMaj[i1] = showLabelsXmaj[i1];
            showLabelsXMin[i1] = showLabelsXmin[i1];
        }
        for (int i1 = 0; i1 < showLabelsYMax.length; i1++) {
            showLabelsYMax[i1] = showLabelsYmaj[i1];
            showLabelsYMin[i1] = showLabelsYmin[i1];
        }
        this.setTickLabelVisible(showLabelsXMaj, showLabelsYMax, showLabelsXMin, showLabelsYMin);
        //-----------
        boolean[] showXTicksBottom = new boolean[anzGrfVisible], showYTicksLeft = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < showXTicksBottom.length; i1++) {
            showXTicksBottom[i1] = true;
        }
        for (int i1 = 0; i1 < showYTicksLeft.length; i1++) {
            showYTicksLeft[i1] = true;
        }
        this.setzeTickAusrichtung(showXTicksBottom, showYTicksLeft);
        //-----------
        int[] posXtickLabels = new int[anzGrfVisible], posYtickLabels = new int[anzGrfVisible];
        for (int i1 = 0; i1 < posXtickLabels.length; i1++) {
            posXtickLabels[i1] = 30;
        }
        for (int i1 = 0; i1 < posYtickLabels.length; i1++) {
            posYtickLabels[i1] = yTickLength[i1] + 4; //45;
        }
        this.setzeTickLabelPosition(posXtickLabels, posYtickLabels);
        //-----------
        Font[] foX = new Font[anzGrfVisible], foY = new Font[anzGrfVisible];
        for (int i1 = 0; i1 < foX.length; i1++) {
            foX[i1] = new Font("Arial", Font.PLAIN, 11);
        }
        for (int i1 = 0; i1 < foY.length; i1++) {
            foY[i1] = new Font("Arial", Font.PLAIN, 11);
        }
        this.setzeTickLabelFont(foX, foY);
        //-------------------------------------
    }

    public void aktualisiereAchsenNachResizing() {
        //-------------------------------------
        //-----------
        final int breite = this.getWidth(), hoehe = this.getHeightForZVInPixels();
        xGrfMIN = new int[anzGrfVisible];
        xGrfMAX = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xGrfMIN.length; i1++) {
            xGrfMIN[i1] = 0;
            xGrfMAX[i1] = this.getWidth();
        }
        yGrfMIN = new int[anzGrfVisible];
        yGrfMAX = new int[anzGrfVisible];
        double ySpGes = 0;   // Weighting of the y-axes
        int iyy = 1;  // Index for y-axis
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == DIAGRAM_TYP_ZV) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        yGrfMIN[0] = 0;
        yGrfMAX[0] = (diagramTyp[0] == DIAGRAM_TYP_ZV) ? 0 + (int) (hoehe * (ySpacingDiagram[0] / ySpGes)) : 0 + notwendigeHoehePixGRF[0];
        for (int i1 = 1; i1 < this.getAnzahlDiagramme(); i1++) {
            yGrfMIN[iyy] = yGrfMAX[iyy - 1];
            yGrfMAX[iyy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? yGrfMIN[iyy] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : yGrfMIN[iyy] + notwendigeHoehePixGRF[i1];
            iyy++;
        }
        //-------------------------------------
        int[] laenge_xAchse = new int[anzGrfVisible], posX_xAchse = new int[anzGrfVisible], posY_xAchse = new int[anzGrfVisible];
        int[] laenge_yAchse = new int[anzGrfVisible], posX_yAchse = new int[anzGrfVisible], posY_yAchse = new int[anzGrfVisible];
        int ix = 0, iy = 0;  // Index for x and y axes
        ySpGes = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == DIAGRAM_TYP_ZV) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            // X-Achse:
            laenge_xAchse[ix] = breite - (DX_IN_LINKS + DX_IN_RECHTS);
            posX_xAchse[ix] = DX_IN_LINKS;
            if (ix == 0) {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - DY_IN_UNTEN) : notwendigeHoehePixGRF[i1] - DY_IN_UNTEN;
            } else {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? posY_xAchse[ix - 1] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : posY_xAchse[ix - 1] + notwendigeHoehePixGRF[i1];
            }
            ix++;
            // Y-Achse:
            laenge_yAchse[iy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - (DY_IN_OBEN + DY_IN_UNTEN)) : notwendigeHoehePixGRF[i1] - (DY_IN_OBEN + DY_IN_UNTEN);
            posX_yAchse[iy] = posX_xAchse[ix - 1];
            posY_yAchse[iy] = posY_xAchse[ix - 1];
            iy++;
        }
        this.setAxisWidthHeightX0Y0(laenge_xAchse, laenge_yAchse, posX_xAchse, posY_xAchse, posX_yAchse, posY_yAchse);
        //-------------
        // Check for null arrays before calling method that uses them
        if (gridNormalX_associatedXAxis != null && gridNormalX_associatedYAxis != null
                && xShowGridMaj != null && xShowGridMin != null && widthPix != null) {
            this.possiblyHideGridLines();
        }
    }

    public void possiblyHideGridLines() {
        //-------------------------------------
        //-------------
        //-------------
        //
        final double px1 = 230, px2 = 100, pxr = 2.5;
        final int[][] showGridXMax = new int[anzGrfVisible][2], showGridXMin = new int[anzGrfVisible][2];
        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalX_associatedXAxis[i1];
            final int indexAchseY = gridNormalX_associatedYAxis[i1];
            showGridXMax[i1][0] = xShowGridMaj[i1] ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            showGridXMax[i1][1] = xShowGridMaj[i1] ? indexAchseY : -1;
            showGridXMin[i1][0] = xShowGridMin[i1] ? indexAchseX : -1;
            showGridXMin[i1][1] = xShowGridMin[i1] ? indexAchseY : -1;
            zeichneDiagrammUmrandung[i1] = false;
            if (widthPix[indexAchseX] < px1 * pxr) {
                showGridXMin[i1][0] = -1;
                showGridXMin[i1][1] = -1;
            }
            if (widthPix[indexAchseX] < px2 * pxr) {
                showGridXMax[i1][0] = -1;
                showGridXMax[i1][1] = -1;
                zeichneDiagrammUmrandung[i1] = true;
            }
        }

        final int[][] showGridNormalYmajLok = new int[anzGrfVisible][2], showGridNormalYminLok = new int[anzGrfVisible][2];
        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalY_associatedXAxis[i1];
            final int indexAchseY = gridNormalY_associatedYAxis[i1];
            showGridNormalYmajLok[i1][0] = yShowGridMaj[i1] ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            showGridNormalYmajLok[i1][1] = yShowGridMaj[i1] ? indexAchseY : -1;
            showGridNormalYminLok[i1][0] = yShowGridMin[i1] ? indexAchseX : -1;
            showGridNormalYminLok[i1][1] = yShowGridMin[i1] ? indexAchseY : -1;
            if (heightPix[indexAchseY] < px1) {
                showGridNormalYminLok[i1][0] = -1;
                showGridNormalYminLok[i1][1] = -1;
            }
            if (heightPix[indexAchseY] < px2) {
                showGridNormalYmajLok[i1][0] = -1;
                showGridNormalYmajLok[i1][1] = -1;
                zeichneDiagrammUmrandung[i1] = true;
            }
        }
        this.showGridLines(showGridXMax, showGridXMin, showGridNormalYmajLok, showGridNormalYminLok);
        //-------------------------------------
    }

    public void setMouseMode(final int mouseMode) {
        this.mausModusALT = this.mouseMode;  // alten Zustand abspeichern
        this.mouseMode = mouseMode;  // go to the new state
        //--------------------------
        switch (mouseMode) {
            case MOUSEMODE_NONE:
                xSliderActive = false;  // aktives Ausschalten des Schiebers
                this.repaint();
                break;
            case MOUSEMODE_ZOOM_AUTOFIT:
                this.mouseMode_ZOOM_AUTOFIT();  // xSchieber unveraendert
                break;
            case MOUSEMODE_ZOOM_WINDOW:
                break;
            case MOUSEMODE_DRAW_LINE:
                break;
            case MOUSEMODE_VALUE_DISPLAY_SLIDER:
                if (!xSliderActive) {
                    xSliderActive = true;
                    xSliderPixels = _xAxisX[0];  // x-Schieber wird an den Anfang gesetzt: gleich fuer alle Diagramme, in GraferV3 definiert
                    xSchieberPix2 = _xAxisX[0];
                    this.repaint();
                }
                break;
            default:LogManager.getLogger(GraferImplementation.class).error("message");
        }
        //--------------------------
    }

    //=================================================
    //=================================================
    //=================================================
    @Override
    public void mouseEntered(MouseEvent me) {
        // no-op
    }

    @Override
    public void mouseExited(MouseEvent me) {
        // no-op
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mouseMode == MOUSEMODE_ZOOM_WINDOW) {
            mouseMode_ZOOM_WINDOW(mouseX, mouseY, MOUSE_CLICKED, me.isControlDown(), me.isShiftDown());
        } else if (mouseMode == MOUSEMODE_DRAW_LINE) {
            mausModus_ZEICHNE_LINIE(mouseX, mouseY, MOUSE_CLICKED);
        } else if (mouseMode == MOUSEMODE_VALUE_DISPLAY_SLIDER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mouseX, mouseY, MOUSE_CLICKED, me);
        }
    }

    @Override
    public void mousePressed(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mouseMode == MOUSEMODE_ZOOM_WINDOW) {
            mouseMode_ZOOM_WINDOW(mouseX, mouseY, MOUSE_PRESSED, me.isControlDown(), me.isShiftDown());
        } else if (mouseMode == MOUSEMODE_DRAW_LINE) {
            mausModus_ZEICHNE_LINIE(mouseX, mouseY, MOUSE_PRESSED);
        } else if (mouseMode == MOUSEMODE_VALUE_DISPLAY_SLIDER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mouseX, mouseY, MOUSE_PRESSED, me);
        }
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mouseMode == MOUSEMODE_ZOOM_WINDOW) {
            mouseMode_ZOOM_WINDOW(mouseX, mouseY, MOUSE_RELEASED, me.isControlDown(), me.isShiftDown());
        } else if (mouseMode == MOUSEMODE_DRAW_LINE) {
            mausModus_ZEICHNE_LINIE(mouseX, mouseY, MOUSE_RELEASED);
        } else if (mouseMode == MOUSEMODE_VALUE_DISPLAY_SLIDER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mouseX, mouseY, MOUSE_RELEASED, me);
        }
    }

    @Override
    public void mouseMoved(MouseEvent me) {
        // no-op
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }


        if (mouseMode == MOUSEMODE_NONE || mouseMode == MOUSEMODE_ZOOM_AUTOFIT) {
            return;
        }
        int mx = me.getX(), my = me.getY();
        if (mouseMode == MOUSEMODE_ZOOM_WINDOW) {
            mouseMode_ZOOM_WINDOW(mx, my, MOUSE_DRAGGED, me.isControlDown(), me.isShiftDown());
        } else if (mouseMode == MOUSEMODE_DRAW_LINE) {
            mausModus_ZEICHNE_LINIE(mx, my, MOUSE_DRAGGED);
        } else if (mouseMode == MOUSEMODE_VALUE_DISPLAY_SLIDER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mx, my, MOUSE_DRAGGED, me);
        }
    }
    //=================================================

    public void mouseMode_ZOOM_AUTOFIT() {
        //--------------

        if (worksheetDatenTEMP != null) {
            for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                    worksheetData.setValue(worksheetDatenTEMP[i1][i2], i1, i2);
                }
            }
            worksheetDatenTEMP = null;
            _zvCounter = zvCounterTEMP;
            zvCounterTEMP = 0;
            nochNichtGeZoomt = true;
        }
        this.definiereAchsenbegrenzungenImAutoZoom(worksheetData);
        mouseMode = mausModusALT;
        _scope.updateMouseMode(mouseMode);
        //--------------
    }

    void undoZoom() {
        zoomRechteck(true);
    }

    public void mouseMode_ZOOM_WINDOW(int mx, int my, int mausAktion, boolean isControlDown, boolean isShiftDown) {



        if (mx < _xAxisX[0]) {
            mx = _xAxisX[0];
        }
        if (mx > _xAxisX[0] + widthPix[0]) {
            mx = _xAxisX[0] + widthPix[0];
        }
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                angeklicktZoom = true;
                x1 = mx;
                y1 = my;
                try {
                    indexAngeklickterGraph = 0;
                    while (!((xGrfMIN[indexAngeklickterGraph] <= mx) && (mx <= xGrfMAX[indexAngeklickterGraph])
                            && (yGrfMIN[indexAngeklickterGraph] <= my) && (my <= yGrfMAX[indexAngeklickterGraph]))) {
                        indexAngeklickterGraph++;
                    }
                } catch (Exception e) {
                    indexAngeklickterGraph = -1;
                }
                break;
            //--------------------------
            case MOUSE_RELEASED:
                angeklicktZoom = false;
                x2 = mx;
                y2 = my;
                if (Math.abs(x1 - x2) > 1 || Math.abs(y1 - y2) > 1) {
                    this.zoomRechteck(false);
                }
                indexAngeklickterGraph = -1;
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                if (indexAngeklickterGraph == -1) {
                    return;  // kein Graph angeklickt
                }
                int x2old = x2;
                int y2old = y2;
                if (angeklicktZoom) {
                    if (mx < xGrfMIN[indexAngeklickterGraph]) {
                        mx = xGrfMIN[indexAngeklickterGraph];
                    }
                    if (mx > xGrfMAX[indexAngeklickterGraph]) {
                        mx = xGrfMAX[indexAngeklickterGraph];
                    }

                    if (!isControlDown) {
                        if (my < yGrfMIN[indexAngeklickterGraph]) {
                            my = yGrfMIN[indexAngeklickterGraph];
                        }
                        if (my > yGrfMAX[indexAngeklickterGraph]) {
                            my = yGrfMAX[indexAngeklickterGraph];
                        }
                        y2 = my;
                        controlZoomOn = false;
                    } else {
                        y2 = y1 + 1;
                        controlZoomOn = true;
                    }

                    if (isShiftDown) {
                        shiftZoomOn = true;
                        x2 = x1 + 1;
                    } else {
                        shiftZoomOn = false;
                        x2 = mx;
                    }



                    int drawStartx = Math.min(x1, Math.min(x2, x2old)) - 25;
                    int drawStarty = Math.min(y1, Math.min(y2, y2old)) - 25;
                    int drawWidthx = Math.abs(x1 - x2) + 100;
                    int drawWidthy = Math.abs(y1 - y2) + 100;
                    drawWidthx = Math.max(drawWidthx, Math.abs(x2old - x1) + 50);
                    drawWidthy = Math.max(drawWidthy, Math.abs(y2old - y1) + 50);
                    repaint(drawStartx, drawStarty, drawWidthx, drawWidthy);
                }
                break;
            //--------------------------
            default:
                break;
        }
    }

    public void mausModus_ZEICHNE_LINIE(int mx, int my, int mausAktion) {
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                break;
            //--------------------------
            case MOUSE_RELEASED:
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                break;
            //--------------------------
            case MOUSE_CLICKED:
                break;
            //--------------------------
            default:
                LOGGER.info("Fehler: eorivm3");
                break;
        }
    }

    public void mausModus_ZEICHNE_TEXT(int mx, int my, int mausAktion) {
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                break;
            //--------------------------
            case MOUSE_RELEASED:
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                break;
            //--------------------------
            //--------------------------
            default:
                LOGGER.info("Fehler: oweifn03");
                break;
        }
    }

    public void mouseMode_VALUE_DISPLAY_SLIDER(int mx, int my, int mausAktion, MouseEvent me) {
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                break;
            //--------------------------
            case MOUSE_RELEASED:
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                if ((mouseMode != MOUSEMODE_ZOOM_WINDOW) && xSliderActive) {
                    //-------------
                    try {
                        indexAngeklickterGraph = 0;
                        while (!((xGrfMIN[indexAngeklickterGraph] <= mx) && (mx <= xGrfMAX[indexAngeklickterGraph])
                                && (yGrfMIN[indexAngeklickterGraph] <= my) && (my <= yGrfMAX[indexAngeklickterGraph]))) {
                            indexAngeklickterGraph++;
                        }
                    } catch (Exception e) {
                        indexAngeklickterGraph = -1;
                    }
                    //-------------
                    if ((me.getModifiersEx() & me.BUTTON1_DOWN_MASK) != 0 && !me.isControlDown()) {
                        inDiffMode = false;
                        xSliderPixels = mx;
                    } else {
                        inDiffMode = true;
                        xSchieberPix2 = mx;
                    }
                    calculateSliderValues();

                    repaint();

                }
                break;
            //--------------------------
            default:
                break;
        }
        //------------------------------

    }

    private void calculateSliderValues() {
        if (!inDiffMode) {
            if (xSliderPixels < _xAxisX[0]) {
                xSliderPixels = _xAxisX[0];
            }
            if (xSliderPixels > _xAxisX[0] + widthPix[0]) {
                xSliderPixels = _xAxisX[0] + widthPix[0];
            }
            try {
                xSliderValue[0] = getValueFromPixel(xSliderPixels, 0)[0];
            } catch (Exception e) {
                // ignored: best-effort slider value conversion
            }  // x-Wert der Schieber-Position
            int index = findSliderTimeIndex(xSliderValue[0]);
            if (index >= 0) {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert[i2][0] = worksheetData.getValue(i2 + 1, index);
                }
            } else {
                for (double[] sliderValue : ySchieberWert) {
                    sliderValue[0] = 0;
                }
            }
        } else {
            if (xSchieberPix2 < _xAxisX[0]) {
                xSchieberPix2 = _xAxisX[0];
            }
            if (xSchieberPix2 > _xAxisX[0] + widthPix[0]) {
                xSchieberPix2 = _xAxisX[0] + widthPix[0];
            }
            try {
                xSchieberWert2[0] = getValueFromPixel(xSchieberPix2, 0)[0];
            } catch (Exception e) {
                // ignored: best-effort slider value conversion
            }  // x-Wert der Schieber-Position
            int index = findSliderTimeIndex(xSchieberWert2[0]);
            if (index >= 0) {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert2[i2][0] = worksheetData.getValue(i2 + 1, index);
                }
            } else {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert2[i2][0] = 0;
                }
            }
        }

    }

    private int findSliderTimeIndex(double sliderValue) {
        try {

            int i1 = 1;

            try {
                for (int startIndex = 1; startIndex < worksheetData.getColumnLength(); startIndex += 10) {
                    if (sliderValue >= worksheetData.getValue(0, startIndex - 1)) {
                        i1 = startIndex - 10;
                        break;
                    }
                }
                if (i1 < 1 || i1 > worksheetData.getColumnLength() - 1) {
                    i1 = 1;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (i1 = 1; i1 < worksheetData.getColumnLength(); i1++) {
                if (sliderValue >= worksheetData.getValue(0, i1 - 1)) {
                    if (sliderValue <= worksheetData.getValue(0, i1)) {
                        return i1;
                    }

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return -1;
    }

    public void mausModus_FIBONACCI_LIN(int mx, int my, int mausAktion) {
        // no-op
    }

    public void mausModus_FIBONACCI_LOG(int mx, int my, int mausAktion) {
        // no-op
    }

    //-------------------
    private double[] getValueFromPixel(int xPix, int yPix) {
        //-------------------
        double achseXmin_ = -1, achseYmin_ = -1;
        int xAchseX_ = -1, yAchseY_ = -1;
        double sfX_ = -1, sfY_ = -1;
        int xAchseTyp_ = -1, yAchseTyp_ = -1;
        int indexYAchse = -1;
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
                indexYAchse = yAxisIndex;
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
        return new double[]{xWert, yWert, indexYAchse};
        //-------------------
    }

    // Ermittle (x/y)-Wert in Pixel zu einem Wertepaar -->
    // TODO: why is yWert not final?
    private int[] getPixelFromValue(final double xWert, double yWert, final int index_xAchse, final int index_yAchse) {
        try {
            //-------------------
            final double achseXminLok = axisXmin[index_xAchse];
            final int xAchseXLok = _xAxisX[index_xAchse];
            final double sfX_ = sfX[index_xAchse];
            final int xAchseTyp_ = xAxisType[index_xAchse];
            final double achseYmin_ = axisYmin[index_yAchse];
            final int yAchseY_ = _yAxisY[index_yAchse];
            final double sfY_ = sfY[index_yAchse];
            final int yAchseTyp_ = yAxisType[index_yAchse];
            //-------------------
            int xPix = -1, yPix = -1;
            if (xAchseTyp_ == ACHSE_LOG) {
                xPix = (int) (sfX_ * Math.log10(xWert / achseXminLok) + xAchseXLok);

            } else if (xAchseTyp_ == ACHSE_LIN) {
                xPix = (int) ((xWert - achseXminLok) * sfX_ + xAchseXLok);
            }
            if (yAchseTyp_ == ACHSE_LOG) {
                yPix = (int) (yAchseY_ - sfY_ * Math.log10(yWert / achseYmin_));
            } else if (yAchseTyp_ == ACHSE_LIN) {
                yPix = (int) (yAchseY_ - (yWert - achseYmin_) * sfY_);
            }
            return new int[]{xPix, yPix};

        } catch (Exception ex) {
            ex.printStackTrace();
            return new int[]{-1, -1};
        }
        //-------------------
    }

    @Override
    protected void draw(final Graphics graphics) {

        //-------------------
        switch (mouseMode) {
            case MOUSEMODE_NONE:
                break;
            case MOUSEMODE_ZOOM_AUTOFIT:
                break;
            case MOUSEMODE_ZOOM_WINDOW:
                graphics.setColor(GlobalColors.farbeZoomRechteck);
                final int dx = Math.abs(x1 - x2),
                 dy = Math.abs(y1 - y2);

                if ((x1 < x2) && (y1 < y2)) {
                    graphics.drawRect(x1, y1, dx, dy);
                } else if ((x1 < x2) && (y1 > y2)) {
                    graphics.drawRect(x1, y2, dx, dy);
                } else if ((x1 > x2) && (y1 < y2)) {
                    graphics.drawRect(x2, y1, dx, dy);
                } else if ((x1 > x2) && (y1 > y2)) {
                    graphics.drawRect(x2, y2, dx, dy);
                }
                break;
            case MOUSEMODE_DRAW_LINE:
                break;
            case MOUSEMODE_VALUE_DISPLAY_SLIDER:
                // Auch bei einigen anderen MausModus-Einstellungen soll der Schieber sichtbar sein
                // daher: Anzeige abhaengig von 'xSliderActive', siehe unten -->
                break;
            default:LogManager.getLogger(GraferImplementation.class).error("Default in case statement reached.");
                break;
        }
        //-------------------
        if (xSliderActive) {

            final int dx = 7, dy = 1, dyFont = 9;
            graphics.setColor(Color.white);
            graphics.fillRect(dx, this.getHeight() - dy - dyFont, 80, dyFont);
            graphics.setColor(Color.red);

            // changed here: don't use the pixel value for the slider, but
            // the x-Value, and re-calculate the pixel from that value
            // otherwise, zooming or changing the window size makes problems/does
            // not update correctly.
            final int xSPix = getPixelFromValue(xSliderValue[0], 0, 0, 0)[0];
            graphics.drawLine(xSPix, yGrfMIN[0], xSPix, yGrfMAX[anzGrfVisible - 1]);

            cf.setMaximumDigits(6);
            graphics.drawString("t = " + cf.formatT((float) xSliderValue[0], TechFormat.FORMAT_AUTO), dx, this.getHeight() - dy);

            graphics.setColor(Color.green);
            final int xSPix2 = getPixelFromValue(xSchieberWert2[0], 0, 0, 0)[0];
            graphics.drawLine(xSPix2, yGrfMIN[0], xSPix2, yGrfMAX[anzGrfVisible - 1]);
            graphics.drawString("t = " + cf.formatT((float) xSchieberWert2[0], TechFormat.FORMAT_AUTO), dx + 130, this.getHeight() - dy);

            if (xSchieberWert2[0] >= 0) {
                graphics.setColor(Color.black);
                graphics.drawString("dt = " + cf.formatT((float) (xSchieberWert2[0] - xSliderValue[0]), TechFormat.FORMAT_AUTO), dx + 260, this.getHeight() - dy);
            }

        }

        //-------------------
    }

    private void zoomRechteck(final boolean isUndoZoom) {
        //-------------------
        //
        //
        int indexAxis = -1;
        if (isUndoZoom) {
            for (int i = 0; i < minX.length; i++) {
                minX[i] = minXOld[i];
                minY[i] = minYOld[i];
                maxX[i] = maxXOld[i];
                maxY[i] = maxYOld[i];

                minXOld[i] = minXOldOld[i];
                minYOld[i] = minYOldOld[i];
                maxXOld[i] = maxXOldOld[i];
                maxYOld[i] = maxYOldOld[i];
            }

            try {
                this.getChangedDataResolution(minX[0], maxX[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else {
            for (int i = 0; i < minX.length; i++) {

                minXOldOld[i] = minXOld[i];
                minYOldOld[i] = minYOld[i];
                maxXOldOld[i] = maxXOld[i];
                maxYOldOld[i] = maxYOld[i];

                minXOld[i] = minX[i];
                minYOld[i] = minY[i];
                maxXOld[i] = maxX[i];
                maxYOld[i] = maxY[i];
            }


            double[] x1y1 = this.getValueFromPixel(x1, y1);
            double[] x2y2 = this.getValueFromPixel(x2, y2);

            if (shiftZoomOn) {
                x1y1[0] = minX[0];
                x2y2[0] = maxX[0];
            }

            indexAxis = (int) x1y1[2];
            try {
                this.getChangedDataResolution(x1y1[0], x2y2[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            //
            double tMIN = -1, tMAX = -1, yMIN = -1, yMAX = -1;

            if (x1y1[0] < x2y2[0]) {
                tMIN = x1y1[0];
                tMAX = x2y2[0];
            } else {
                tMIN = x2y2[0];
                tMAX = x1y1[0];
            }
            if (x1y1[1] < x2y2[1]) {
                yMIN = x1y1[1];
                yMAX = x2y2[1];
            } else {
                yMIN = x2y2[1];
                yMAX = x1y1[1];
            }

            for (int i1 = 0; i1 < minX.length; i1++) {
                minX[i1] = tMIN;
                maxX[i1] = tMAX;
            }  // neue x-Bereichsgrenze fuer alle Diagramme

            if (!controlZoomOn) {
                minY[indexAxis] = yMIN;
                maxY[indexAxis] = yMAX;  // the ZoomRectangle values ​​for Y only for the selected diagram
            }


        }

        //-------------------
        // (2) fuer alle anderen Diagramme wird der y-Bereich gefittet -->
        //
        final double[] value1 = new double[worksheetData.getRowLength()], value2 = new double[worksheetData.getRowLength()];
        for (int i1 = 0; i1 < worksheetData.getRowLength(); i1++) {
            value1[i1] = +1e99;
            value2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < worksheetData.getRowLength(); i1++) // geht durch die Spalten
        {
            for (int i2 = 0; i2 < _zvCounter + 1; i2++) {  // goes through the selected column line by line
                if (i2 < worksheetData.getColumnLength()) {
                    if (worksheetData.getValue(i1, i2) < value1[i1]) {
                        value1[i1] = worksheetData.getValue(i1, i2);
                    }
                    if (worksheetData.getValue(i1, i2) > value2[i1]) {
                        value2[i1] = worksheetData.getValue(i1, i2);
                    }
                }
            }
        }
        for (int i1 = 0; i1 < minX.length; i1++) {
            if (i1 != indexAxis) {
                minY[i1] = +1e99;
                maxY[i1] = -1e99;
            }
        }
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if ((i1 != indexAxis) && (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_Y)) {
                    if (value1[i2] < minY[i1]) {
                        minY[i1] = value1[i2];
                    }
                    if (value2[i2] > maxY[i1]) {
                        maxY[i1] = value2[i2];
                    }
                }
            }
        }
        // 'schoenere' Bereichsgrenzen -->
        for (int i1 = 0; i1 < minY.length; i1++) {
            final double[] autoEmpf = auto_Achsenbegrenzung_Wertempfehlung(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            //tickAbstandY[i1]= autoEmpf[4];
        }
        //-------------------
        double[] xx1 = new double[anzGrfVisible], xx2 = new double[anzGrfVisible];  // X-Achse
        double[] yy1 = new double[anzGrfVisible], yy2 = new double[anzGrfVisible];  // Y-Achse  --> Muss noch individuell angepasst werden!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // is auto-scaling turned on?
        for (int i1 = 0; i1 < xx1.length; i1++) {
            xx1[i1] = minX[i1];
            xx2[i1] = maxX[i1];
            scX[i1] = autoScaleX[i1];
        }
        for (int i1 = 0; i1 < yy1.length; i1++) {
            yy1[i1] = minY[i1];
            yy2[i1] = maxY[i1];
            scY[i1] = autoScaleY[i1];
        }
        this.setzeAchsenBegrenzungen(xx1, xx2, scX, yy1, yy2, scY);
        //-------------------
        double[] xTickSpacingLok = new double[anzGrfVisible];
        double[] yTickSpacingLok = new double[anzGrfVisible];
        for (int i1 = 0; i1 < xTickSpacingLok.length; i1++) {
            xTickSpacingLok[i1] = this.getAutoTickSpacingX(i1);
            xTickSpacing[i1] = xTickSpacingLok[i1];
        }
        for (int i1 = 0; i1 < yTickSpacingLok.length; i1++) {
            yTickSpacingLok[i1] = this.getAutoTickSpacingY(i1);
            yTickSpacing[i1] = yTickSpacingLok[i1];
        }
        this.setzeTickSpacing(xTickSpacingLok, yTickSpacingLok);
        //-------------------
        x1 = -1;
        x2 = -1;
        y1 = -1;
        y2 = -1;  // --> Ausblenden des Zoom-Rechtecks

        repaint();
        //-------------------
    }

    //
    // deutlich hoeherer Aufloesung nachgeladen und in Hi-Lo-Darstellung uebertragen werden, damit die grafische Darstellung
    // nicht wichtige Info verliert (zB. 'ausgefranste' Rippelkurven, verschwundene Peaks, Aus Rechtecken werden Dreiecke usw.)
    //
    private void getChangedDataResolution(double x1, double x2) {

        try {
            // vom Simulator gelieferte worksheet[][]-Daten abspeichern solange man nicht weitersimuliert
            if (nochNichtGeZoomt) {
                nochNichtGeZoomt = false;
                worksheetDatenTEMP = new double[worksheetData.getRowLength()][worksheetData.getColumnLength()];
                for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                    for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                        worksheetDatenTEMP[i1][i2] = worksheetData.getValue(i1, i2);
                    }
                }
                zvCounterTEMP = _zvCounter;
            }

            // richtige Ordnung von x1 und x2 -->
            if (x1 > x2) {
                final double tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            // x1 und x2 beschreiben die Bereichsgrenzen --> RAM-Daten laden -->
            final int lg1 = worksheetData.getRowLength(), lg2 = worksheetData.getColumnLength();
            final DataContainer wsRAM = _scope.getZVDataInRAM();  // hochaufloesende Daten im RAM
            int estimatedIndex = (int) (_zvCounter * 1.0 / lg2 * wsRAM.getColumnLength());
            //-------------
            // entsprechende Bereichsgrenzen in RAM-Daten finden -->
            final double xmin = wsRAM.getValue(0, 0);  // exakt
            if (estimatedIndex >= wsRAM.getColumnLength()) {
                estimatedIndex = wsRAM.getColumnLength() - 1;
            }
            double xmax = wsRAM.getValue(0, estimatedIndex);  // estimated

            //-------------
            while (xmax == 0) {
                estimatedIndex = (int) (0.8 * estimatedIndex);
                xmax = wsRAM.getValue(0, estimatedIndex);
            }
            int zeigerX1_RAM = (int) ((x1 - xmin) / (xmax - xmin) * estimatedIndex);  // vorerst estimated
            if (zeigerX1_RAM < 0) {
                zeigerX1_RAM = 0;
            }
            int zeigerX2RAM = (int) ((x2 - xmin) / (xmax - xmin) * estimatedIndex);  // vorerst estimated
            //
            double x1RAM = wsRAM.getValue(0, zeigerX1_RAM);  // vorerst estimated
            try {
                if (x1RAM < x1) {
                    while ((x1RAM = wsRAM.getValue(0, zeigerX1_RAM)) < x1) {
                        zeigerX1_RAM++;
                    }
                } else {
                    while ((x1RAM = wsRAM.getValue(0, zeigerX1_RAM)) > x1) {
                        zeigerX1_RAM--;
                    }
                }
            } catch (Exception e) {
                return;
            }  // Zoom in einen Bereich ohne Daten


            double x2RAM = wsRAM.getValue(0, zeigerX2RAM);  // vorerst estimated


            while (x2RAM == 0) {
                zeigerX2RAM--;
                x2RAM = wsRAM.getValue(0, zeigerX2RAM);
            }

            if (x2RAM < x2) {
                for (; wsRAM.getValue(0, zeigerX2RAM) != 0 && x2RAM < x2; x2RAM = wsRAM.getValue(0, zeigerX2RAM)) {
                    zeigerX2RAM++;
                }
            } else {
                for (; x2RAM > x2; x2RAM = wsRAM.getValue(0, zeigerX2RAM)) {
                    zeigerX2RAM--;
                }
            }


            final int maximumIndex = wsRAM.getMaximumTimeIndex();
            zeigerX2RAM = Math.min(maximumIndex, zeigerX2RAM);
            x2RAM = wsRAM.getValue(0, zeigerX2RAM);

            zeigerX1_RAM -= 2;
            if (zeigerX1_RAM < 0) {
                zeigerX1_RAM = 0;
            }


            //-------------
            // RAM-Daten auf Hi-Lo mit SCOPE-Aufloesung reduzieren -->
            //
            int zvC = 0;  // local counter in the compressed SCOPE data
            final double dtSCOPE = (x2RAM - x1RAM) / INTERVALS_ALONG_X;
            if (dtSCOPE > wsRAM.getTimeIntervalResolution()) {
                int lowerIndex = zeigerX1_RAM;
                int higherIndex = zeigerX2RAM;

                for (int worksheetIndex = 0; worksheetIndex < INTERVALS_ALONG_X + 2; worksheetIndex++) {
                    final double timeValue = x1RAM + worksheetIndex * dtSCOPE;
                    worksheetData.setValue(timeValue, 0, 2 * worksheetIndex);
                    worksheetData.setValue(timeValue + dtSCOPE, 0, 2 * worksheetIndex + 1);

                    while (higherIndex < maximumIndex && wsRAM.getEstimatedTimeValue(lowerIndex) < timeValue) {
                        lowerIndex++;
                    }

                    higherIndex = lowerIndex;

                    while (higherIndex < maximumIndex && wsRAM.getEstimatedTimeValue(higherIndex) < timeValue + dtSCOPE) {
                        higherIndex++;
                    }

                    for (int i1 = 0; i1 < lg1 - 1; i1++) {
                        final HiLoData hiLo = wsRAM.getHiLoValue(i1 + 1, lowerIndex, higherIndex);
                        final double meanValue = 0.5 * (hiLo.yHi + hiLo.yLo);
                        double oldMeanValue = 0;
                        try {
                            oldMeanValue = 0.5 * (worksheetData.getValue(i1 + 1, 2 * worksheetIndex - 1) + worksheetData.getValue(i1 + 1, 2 * worksheetIndex - 2));
                        } catch (Exception ex) {
                            oldMeanValue = meanValue;
                        }

                        if (meanValue < oldMeanValue) {
                            worksheetData.setValue(hiLo.yHi, i1 + 1, 2 * worksheetIndex);
                            worksheetData.setValue(hiLo.yLo, i1 + 1, 2 * worksheetIndex + 1);
                        } else {
                            worksheetData.setValue(hiLo.yLo, i1 + 1, 2 * worksheetIndex);
                            worksheetData.setValue(hiLo.yHi, i1 + 1, 2 * worksheetIndex + 1);
                        }

                    }
                }
                _zvCounter = 2 * INTERVALS_ALONG_X;

            } else { // single point can be resolved:
                for (int i2 = zeigerX1_RAM + 1; i2 < zeigerX2RAM + 1 && i2 < maximumIndex; i2++) {
                    final double time = wsRAM.getValue(0, i2);
                    if (zvC < worksheetData.getColumnLength()) {
                        worksheetData.setValue(time, 0, zvC);
                        for (int i1 = 0; i1 < lg1 - 1; i1++) {
                            final double value = wsRAM.getValue(i1 + 1, i2);
                            worksheetData.setValue(value, i1 + 1, zvC);
                        }
                    }
                    zvC++;
                    _zvCounter = zvC;
                }
            }
            //-------------
            // Y-Clip --> Worksheet data is searched
            _scope.ladeWorkSheet();

            //-------------
            //System.out.println("x1RAM= "+x1RAM+"\tx2RAM= "+x2RAM);
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    public void initClipping() {
        // Kann erst aufgerufen werden, wenn 'worksheet' und 'minX[],maxX[],...' definiert sind -->
        //----------------------------------------------
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_X)
                        || (matrixZuordnungKurveDiagram[i1][i2] == ASSIGNMENT_Y)) {
                    crvClipValXmin[i1][i2] = this.getXClipAchse(i1, i2)[0];
                    crvClipValXmax[i1][i2] = this.getXClipAchse(i1, i2)[1];
                    crvClipValYmin[i1][i2] = this.getYClipAchse(i1, i2)[0];
                    crvClipValYmax[i1][i2] = this.getYClipAchse(i1, i2)[1];
                }
            }
        }
    }

    public double[] getXClipNo(final int im1, final int im2) {
        // dh. dort gibt es keine Y-Achsen-Begrenzung
        // CLIP_NO bedeutet: Worksheet-Daten sind begrenzend
        //------------------
        // dh. dort gibt es keine Y-Achsen-Begrenzung
        int indexX = -1;
        for (int i1 = 0; i1 < worksheetData.getRowLength(); i1++) {
            if (matrixZuordnungKurveDiagram[im1][i1] == ASSIGNMENT_X) {
                indexX = i1;
            }
        }
        if (indexX == -1) {LogManager.getLogger(GraferImplementation.class).error("Index error in plot.");
        }
        // (2) Min- und Max-Werte in dieser Kolonne finden:
        double wsMIN = 1e99, wsMAX = -1e99;
        for (int i1 = 0; i1 < worksheetData.getColumnLength(); i1++) {
            if (worksheetData.getValue(indexX, i1) < wsMIN) {
                wsMIN = worksheetData.getValue(indexX, i1);
            }
            if (worksheetData.getValue(indexX, i1) > wsMAX) {
                wsMAX = worksheetData.getValue(indexX, i1);
            }
        }
        return new double[]{wsMIN, wsMAX};
    }

    public double[] getYClipNo(final int im1, final int im2) {
        // Y-Clip --> Worksheet-Daten werden durchsucht
        // CLIP_NO bedeutet: Worksheet-Daten sind begrenzend
        double wsMIN = 1e99, wsMAX = -1e99;
        for (int i1 = 0; i1 < worksheetData.getColumnLength(); i1++) {
            if (worksheetData.getValue(im2, i1) < wsMIN) {
                wsMIN = worksheetData.getValue(im2, i1);
            }
            if (worksheetData.getValue(im2, i1) > wsMAX) {
                wsMAX = worksheetData.getValue(im2, i1);
            }
        }
        return new double[]{wsMIN, wsMAX};
    }

    public double[] getXClipAchse(final int im1, final int im2) {
        // CLIP_ACHSE bedeutet: Achse ist begrenzend
        return new double[]{minX[im1], maxX[im1]};
    }

    public double[] getYClipAchse(final int im1, final int im2) {
        // CLIP_ACHSE bedeutet: Achse ist begrenzend
        // Achtung: Unterscheidung Y-Achse und Y2-Achse -->
        if (matrixZuordnungKurveDiagram[im1][im2] == ASSIGNMENT_Y) {
            return new double[]{minY[im1], maxY[im1]};
        } else {
            // dh. dort gibt es keine Y-Achsen-Begrenzung
            return new double[]{-1, -1};
        }
    }


    public void initAutotickSpacing() {
        for (int i1 = 0; i1 < ANZ_DIAGRAM_MAX; i1++) {
            xTickSpacing[i1] = this.getAutoTickSpacingX(i1);
            yTickSpacing[i1] = this.getAutoTickSpacingY(i1);
        }
    }

    /**
     *
     * @param im1
     * @return
     */
    public double getAutoTickSpacingX(final int im1) {
        return (maxX[im1] - minX[im1]) / ANZ_AUTO_TICKS;
    }

    public double getAutoTickSpacingY(final int im1) {
        return (maxY[im1] - minY[im1]) / ANZ_AUTO_TICKS;
    }
}
