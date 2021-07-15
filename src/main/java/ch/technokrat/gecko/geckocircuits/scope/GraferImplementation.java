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
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
public final class GraferImplementation extends GraferV3 implements MouseListener, MouseMotionListener {
    // Anzahl der Intervalle auf der x-Achse, in denen Hi- und Lo-Werte zwecks Datenkompression ermittelt werden
    private static final int INTERVALLE_ENTLANG_X = 2000;
    
    private static final long serialVersionUID = 364726123473711L;
    private final Scopable _scope;  // callback 
    // XXX private final ScopeSettings _scopeSettings;
    // gibt an, wieviel der Punkte im Worksheet als Kurve dargestellt werden sollen, siehe Fkt. weiter unten
    private int _zvCounter;
    // kommen die darzustellenden Daten von einer ZV-Simulation oder von einer externen 
    // (statischen) Datei? - default: ZV-Sim.
    public boolean _usesExternalData = false;
    //---------------------------------
    private static final int TXT_DISTANCE_Y = 10;
    public static final int ANZ_DIAGRAM_MAX = 9;  // Zahl der maximal moeglichen Diagramme in einem SCOPE
    public static int DX_IN_LINKS = 60, DX_IN_RECHTS = 70;  // links- u. rechtsseitige x-Einrueckung der Achsen in Pixel
    public static int DY_IN_OBEN = 8, DY_IN_UNTEN = 8;  // y-Einrueckung der Achsen in Pixel von Oben bzw. Unten und y-Abstand zwischen 2 Diagrammen
    public static int ABSTAND_BESCHRIFTUNG_XACHSE = 35;  // soviel Abstand nach ganz unten gibt es zusaetzlich, damit die x-Achsen-Labels gesetzt werden koennen
    private static final int ANZ_AUTO_TICKS = 5;
    private int x1, x2, y1, y2;  // Rechteck-Koordinaten des Zoom-Fensters
    private boolean angeklicktZoom = false;
    //--------
    // Bereichsgrenzen eines Diagramms bezueglich Maus-Klick:
    private int[] xGrfMIN, xGrfMAX, yGrfMIN, yGrfMAX;
    private int indexAngeklickterGraph;
    private boolean controlZoomOn = false;
    private boolean shiftZoomOn = false;
    //==========================================
    //
    // Graph-Properties --> angepasst auf die spezielle SCOPE-Struktur
    public static final int DIAGRAM_TYP_ZV = 91, DIAGRAM_TYP_SGN = 92;
    //
    private int anzGrfVisible;  // Anzahl der aktuell sichtbaren Graphen im Scope
    private int anzDiagram;  // Anzahl der Diagramme
    //
    public String[] nameDiagram;  // Bezeichnungen der Diagramme
    public double[] ySpacingDiagram;  // wieviel 'y-Anteil' hat das jeweilige Diagramm
    public int[] notwendigeHoehePixGRF;  // bei SIGNAL wird die Graph-Hoehe in Pix vorgegeben, eventuell wird die Scope-Groesse angepasst (bei ZV --> '-1')
    public int[] diagramTyp;  // ist das jeweilige Diagramm ein ZV-Typ oder ein Signal-Typ?
    public boolean[] jcbShowLegende;  // sollen die Kurvennamen in Form einer Legende am linken Graph-Rand angezeigt werden?
    public boolean[] showAxisX, showAxisY;  // Anzeigen oder Ausblenden der Achsen
    //
    public double[] minX, maxX, minY, maxY;  // Achsen-Begrenzungen
    public double[] minXOld, maxXOld, minYOld, maxYOld;  // Achsen-Begrenzungen
    public double[] minXOldOld, maxXOldOld, minYOldOld, maxYOldOld;  // Achsen-Begrenzungen
    public boolean[] autoScaleX, autoScaleY;  // sollen die Achsenbegrenzungen automatisch an die Worksheetdaten angepasst werden?
    public int[] xAchsenTyp, yAchsenTyp;  // Linear oder logarithmisch?
    public int[] xAchseFarbe, yAchseFarbe;
    public int[] xAchseStil, yAchseStil;
    // TODO: the following fields hide Grafer fields, check if it would make a 
    // problem just to delete it from here!
    public String[] xAchseBeschriftung, yAchseBeschriftung;
    //
    public int[] gridNormalX_zugeordneteXAchse, gridNormalX_zugeordneteYAchse;
    public int[] gridNormalY_zugeordneteXAchse, gridNormalY_zugeordneteYAchse;
    public int[] farbeGridNormalX, farbeGridNormalXminor, farbeGridNormalY, farbeGridNormalYminor;
    public int[] linStilGridNormalX, linStilGridNormalXminor, linStilGridNormalY, linStilGridNormalYminor;
    public boolean[] xShowGridMaj, xShowGridMin, yShowGridMaj, yShowGridMin;
    //
    public boolean[] xTickAutoSpacing, yTickAutoSpacing;
    public double[] xTickSpacing, yTickSpacing;
    public int[] xAnzTicksMinor, yAnzTicksMinor;
    public int[] xTickLaenge, xTickLaengeMinor, yTickLaenge, yTickLaengeMinor;
    //
    public boolean[] zeigeLabelsXmaj, zeigeLabelsXmin, zeigeLabelsYmaj, zeigeLabelsYmin;
    //
    private boolean[] zeichneDiagrammUmrandung;  // zu zeichnen, wenn der Grid wegen zu kleiner Darstellung (in Pixelpunkten) abgeschaltet ist 
    //-------------------------
    // Graph-Properties --> speziell fuer SIGNAL
    public int[] positionSIGNAL;  // fuer SIGNAL --> enthaelt die y-Postiion (Reihenfolge) der Signal-Kurve innerhalb des einzelnen Graphen
    public int[] positionSIGNAL_ALT;
    public int[] sgnHeight, sgnDistance;
    public double[] sgnSchwelle;
    //
    //-------------------------
    // Graph-Properties zum Merken, wenn man Achsen ein- und ausblendet -->
    // ... wird jeweils direkt aus 'DialogGraphProperties' gelesen und neugesetzt
    public boolean[] ORIGjcbXShowGridMaj, ORIGjcbXShowGridMin;
    public boolean[] ORIGjcbYShowGridMaj, ORIGjcbYShowGridMin;
    public int[] ORIGjcmXlinCol, ORIGjcmYlinCol;
    public int[] ORIGjcmXlinStyl, ORIGjcmYlinStyl;
    public int[] ORIGjtfXtickLengthMaj, ORIGjtfXtickLengthMin;
    public int[] ORIGjtfYtickLengthMaj, ORIGjtfYtickLengthMin;
    public boolean[] ORIGjcbXShowLabelMaj, ORIGjcbXShowLabelMin;
    public boolean[] ORIGjcbYShowLabelMaj, ORIGjcbYShowLabelMin;
    //=========================
    //
    // Verbindung / Zuordnung  Kurve - Diagramm -->
    public static final int ZUORDNUNG_X = 51, ZUORDNUNG_Y = 52, ZUORDNUNG_SIGNAL = 54, ZUORDNUNG_NIX = 55, ZUORDNUNG_MEAN = 56;
    //
    // Signal-Namen bzw. Worksheet-Headers:
    public int anzSignalePlusZeit;  // soviel verschiedene Kolonnen hat das Worksheet
    public String[] signalNamen;
    // Zuordnungen Kurven - Diagramme:
    public int[][] matrixZuordnungKurveDiagram;
    //
    //public int[] zugehoerigkeitX, zugehoerigkeitY;  // Zuordnung der Kurve zur x- und zur y-Achse
    public int[][] indexWsXY;  // Zuordnung Worksheetdaten - Kurven
    //
    //=========================
    //
    // Kurven-Properties
    public int kurvenanzahl;  // soviel verschiedene Kurven werden aktuell im SCOPE dargestellt --> entspricht einer 'Kurven-ID'
    // zur Zuordnung der Kurvenindizes zur Zuordnungsmatrix:
    public int[] indexDerKurveInDerMatrix;  // Abspeichern in folgendem Format: --> 1000*i1 +i2 wobei (i1..Graphenanzahl / i2..Kurvenanzahl)
    public int[] indexDerKurveInDerMatrixALT;  // Speicherung notwendig um Darstellung der SIGNAL-Ordnung korrekt durchfuehren zu koennen
    //
    // jedem Eintrag in der Verknuepfungsmatrix entspricht eine potentielle Kurve -->
    public int[][] crvAchsenTyp;  // wird ueber SET-Methode aktualisiert, damit die Matrix 'matrixZuordnungKurveDiagram' nicht vergessen wird!
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
    // Maus-Aktionen im Diagramm-Fenster:
    public static final int MAUSMODUS_NIX = 546;  // Ruhestellung --> Maus ist deaktiviert
    public static final int MAUSMODUS_ZOOM_AUTOFIT = 547;  // Diagramm passt sich immer an die Datenwerte an
    public static final int MAUSMODUS_ZOOM_FENSTER = 548;  // man kann mit der Maus Zoom-Rechtecke markieren
    public static final int MAUSMODUS_ZEICHNE_LINIE = 550;  // Linien zeichnen (als Objekte!)
    public static final int MAUSMODUS_WERTANZEIGE_SCHIEBER = 554;  // ein Schieber kann ueber alle Diagramme gelegt werden, die entsprechenden y-Werte alle Kurven werden angezeigt
    //
    public static final int MOUSE_CLICKED = 780;
    public static final int MOUSE_PRESSED = 781;
    public static final int MOUSE_RELEASED = 782;
    public static final int MOUSE_DRAGGED = 783;
    private int mausModus = MAUSMODUS_NIX;  // default --> Maus deaktiviert
    private int mausModusALT = MAUSMODUS_NIX;  // damit man in den vorigen Modus zurueckkehren kann zB. nach Druecken von AutoFit
    //
    private boolean simulationLaeuftGerade = false;
    private boolean nochNichtGeZoomt = true;
    private double[][] worksheetDatenTEMP = null;  // vor dem Zoomen werden hier die Simulationsdaten abgelegt
    private int zvCounterTEMP = 0;  // aktueller Zeiger vor dem Zoomen, wird bei AUTO_FIT reaktiviert
    //---------------------------------
    private boolean xSchieberAktiv = false;
    private int xSchieberPix;
    private double[] xSchieberWert = new double[]{-1, -1};  // einem einzelnen Pixelpunkt sind eventuell mehrere Werte zugeordnet
    private double[][] ySchieberWert;  // pro Kurve gibt es zum xSchieberWert-Punktepaar ein entsprechendes ySchieberWert-Punktepaar
    private TechFormat cf = new TechFormat();
    private NumberFormat nf = NumberFormat.getNumberInstance();
    //==========================================
    private ArrayList txtEintraege = new ArrayList();
    private int xSchieberPix2;
    private double[] xSchieberWert2 = new double[]{-1, -1};
    private double[][] ySchieberWert2;
    boolean inDiffMode = false;
    double[][] crvTransparency;

    public final void setAnzahlSichtbarerDiagramme(final int number) {
        this.anzGrfVisible = number;
    }

    public final int getAnzahlSichtbarerDiagramme() {
        return this.anzGrfVisible;
    }

    public final void setAnzahlDiagramme(final int number) {
        this.anzDiagram = number;
    }

    public final int getAnzahlDiagramme() {
        return this.anzDiagram;
    }

    public final double getSlider1Value() {
        return xSchieberWert[0];
    }

    public final double getSlider2Value() {
        return xSchieberWert2[0];
    }

    public final void setSimulationLaeuftGerade(final boolean simIsRunning) {
        this.simulationLaeuftGerade = simIsRunning;
        this.nochNichtGeZoomt = true;
        //-----------------
        if (worksheetDatenTEMP != null) {
            for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                    worksheetDaten.setValue(worksheetDatenTEMP[i1][i2], i1, i2);
                }
            }
            worksheetDatenTEMP = null;
            nochNichtGeZoomt = true;
        }
        //-----------------
    }

    public final void setCrvAchsenTyp(final int im1, final int im2, final int typ) {
        matrixZuordnungKurveDiagram[im1][im2] = typ;
        crvAchsenTyp[im1][im2] = typ;
    }

    public final int getCrvAchsenTyp(final int im1, final int im2) {
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

    // gibt an, wieviel der Punkte im Worksheet als Kurve dargestellt werden sollen
    // Uebergabe als Array damit die Uebergabe als Referenz erfolgen kann
    public final void setZVCounter(final int zvCounter) {
        this._zvCounter = zvCounter;
    }

    // zum Aufrufen externer Daten, die nicht mehr veraendert werden (im Gegensatz zum kontinuierlichen Kurvenaufbau in der Schaltungssimulation) -->
    // bzw. beim Initialisieren/Aendern der Kurvenanzahl fuer die Simulation
    public final void setzeKurvenUndWorksheetDaten(final String[] header, final DataContainer workSheet) {
        this.worksheetDaten = workSheet;

        this.signalNamen = header;
        this.anzSignalePlusZeit = header.length;
        //------------------------
        // XXX _scopeSettings.usesExternalData = this._usesExternalData;
        // XXX _scopeSettings.update_ZVs(anzSignalePlusZeit, signalNamen);  // default-Initialisierung der Kurven bzw. bei Aenderung der Kurven-Anzahl
        // XXX _scopeSettings.loadSettings(this);  // die in 'ScopeSettings' definierten Parameter werden importiert
        //--------------------------
        if (this._usesExternalData) {
            // Daten werden von extern importiert -->
            this.definiereAchsenbegrenzungenImAutoZoom(workSheet);  // minX[],maxX[],minY[],maxY[],minY2[],maxY2[] werden aus 'worksheetDaten' berechnet
            this.initClipping();  // crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] werden berechnet
            this.initAutotickSpacing();  // benoetigt 'minX[],maxX[],minY[],maxY[],...' zur Berechnung
        } else {
            // Daten kommen von der laufenden ZV-Simulation / hier werden SCOPEs initialisiert, bevor Simulationsdaten da sind -->
            // (1) // minX[],maxX[],minY[],maxY[],minY2[],maxY2[] werden willkuerlich initial gesetzt:
            for (int i1 = 0; i1 < minX.length; i1++) {
                // XXX minX[i1] = SimulationsKern.t1SCOPE;
                // XXX maxX[i1] = SimulationsKern.t2SCOPE;
                // XXX minY[i1] = -10;
                // XXX maxY[i1] = +10;
            }
            // (2) crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] werden berechnet
            this.initClipping();
            // (3) Auto-Ticks // benoetigen minX[],maxX[],minY[],maxY[],... zur Berechnung
            this.initAutotickSpacing();
        }
        //--------------------------
        // fuer den Schieber: 
        ySchieberWert = new double[worksheetDaten.getRowLength() - 1][2];
        ySchieberWert2 = new double[worksheetDaten.getRowLength() - 1][2];

        //-------------------------------------
        this.setzeAchsen();  // die default-Werte der Achsen werden definiert und richtig aufbereitet an GraferV3 weitergegeben, die Tick-Parameter wurden in 'initAutotickSpacing()' ermittelt
        this.setzeKurven();  // die default-Werte der Kurven (in 'setDefault_ZVs' definiert) werden richtig aufbereitet an GraferV3 weitergegeben
        //--------------------------
        this.repaint();
    }

    // regelmaessig wird vom Simulator aufgerufen, um die Kurven-Bilder zu aktualisieren -->
    //
    public void akualisiereKurvenUndWorksheetDaten(final double t1, final double t2) {
        //--------------------------
        this.definiereAchsenbegrenzungenNumerischeSimulation(t1, t2);  // minX[],maxX[],minY[],maxY[] werden aus 'worksheetDaten' berechnet
        //--------------------------
        this.setzeAchsen();  // die default-Werte der Achsen werden definiert und richtig aufbereitet an GraferV3 weitergegeben, die Tick-Parameter wurden in 'initAutotickSpacing()' ermittelt
        this.setzeKurven();  // die default-Werte der Kurven (in 'setDefault_ZVs' definiert) werden richtig aufbereitet an GraferV3 weitergegeben
        //--------------------------
        this.blendeEventuellGridLinienAus();
        this.repaint();
    }

    // Ueberschrieben, damit man einfach SIGNAL-Kurven zeichen kann -->
    @Override
    protected void zeichneKurven(final Graphics g) {
        if (worksheetDaten == null) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g;
        int zd = 0;  // Beschriftungs-Nummerierung in y-Richtung
        for (int i1 = 0; i1 < anzahlKurven; i1++) {
            if (matrixZuordnungKurveDiagram[indexDerKurveInDerMatrix[i1] / 1000][indexDerKurveInDerMatrix[i1] % 1000] == this.ZUORDNUNG_SIGNAL) {
                try {
                    zeichneEinzelneSIGNALKurve(g2, i1);
                } catch (Exception e) {
                }  // SIGNAL --> siehe Implementierung gleich unten
            } else {
                int anzKurvenpunkteImWorksheet = worksheetDaten.getColumnLength();
                if (!_usesExternalData) {
                    anzKurvenpunkteImWorksheet = _zvCounter;
                }
                try {
                    zeichneEinzelneKurve(g2, i1, anzKurvenpunkteImWorksheet);
                } catch (Exception e) {
                }  // ZV --> ist Standard in 'GraferV3'
                //----------
                if ((i1 > 0) && (_yAchseY[indexZurKurveGehoerigeYachse[i1]] != _yAchseY[indexZurKurveGehoerigeYachse[i1 - 1]])) {
                    zd = 0;
                } else {
                    if (i1 > 0) {
                        zd++;
                    }
                }
                this.beschrifteNamenDerEinzelnenZVKurve(g2, i1, zd);  // ZV-Kurven-Beschriftung: wird zwecks Allgemeinheit nicht in 'GraferV3' implementiert sondern gleich weiter unten
            }
        }
    }

    // Beschriftung der Kurven-Namen der ZV-Kurven im Graph-->
    // i1 ... KurvenNummer
    private void beschrifteNamenDerEinzelnenZVKurve(final Graphics2D g2D, final int i1, final int zd) {
        //--------------------------------
        int yLinksObenKurve = _yAchseY[indexZurKurveGehoerigeYachse[i1]] - hoehePix[indexZurKurveGehoerigeYachse[i1]];
        String name = signalNamen[indexDerKurveInDerMatrix[i1] % 1000];
        cf.setMaximumDigits(4);
        String wert = cf.formatT(ySchieberWert[indexDerKurveInDerMatrix[i1] % 1000 - 1][0], TechFormat.FORMAT_AUTO);

        if (inDiffMode) {
            int index = indexDerKurveInDerMatrix[i1] % 1000 - 1;
            wert = cf.formatT(ySchieberWert2[index][0] - ySchieberWert[index][0], TechFormat.FORMAT_AUTO);
        }

        // wenn der Schieber aktiviert ist, wird der y-Wert anstatt der Namen angezeigt --> 
        int delta = 16;  // Abstand der Signalnamen untereinander in der Graph-Legende
        g2D.setColor(kurveFarbe[i1]);
        if (xSchieberAktiv) {
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
    protected void zeichneKoordinatenAchsen(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        calculateSliderValues();
        //
        //===============================================
        wertTickX = new double[anzahlAchsenX][];
        tickX = new int[anzahlAchsenX][];
        wertTickXminor = new double[anzahlAchsenX][];
        tickXminor = new int[anzahlAchsenX][];
        wertTickY = new double[anzahlAchsenY][];
        tickY = new int[anzahlAchsenY][];
        wertTickYminor = new double[anzahlAchsenY][];
        tickYminor = new int[anzahlAchsenY][];
        //
        sfX = new double[anzahlAchsenX];
        sfY = new double[anzahlAchsenY];
        //===============================================
        // wenn die Grid-Linien ausgeblendet werden (automatisch, weil Diagramm zu klein in Pixelpunkten), 
        // dann wird eine Umrandungsbox fuer das Diagramm gezeichnet --> 
        //


        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            if (zeichneDiagrammUmrandung[i1]) {
                g2.setColor(Color.lightGray);
                g2.drawRect(_xAchseX[i1], _yAchseX[i1] - hoehePix[i1], breitePix[i1], hoehePix[i1]);
            }
        }
        //===============================================
        // x-Achsen --> da wird nicht herumgepfuscht (ist gleich bei ZV und SIGNAL)
        for (int i1 = 0; i1 < anzahlAchsenX; i1++) {
            zeichneEinzelneKoordinatenAchse_X(g2, i1);
        }
        // y-Achsen --> ist bei SIGNAL anders (Grid und Labels)
        for (int i1 = 0; i1 < anzahlAchsenY; i1++) {
            if (diagramTyp[i1] == GraferImplementation.DIAGRAM_TYP_ZV) {
                zeichneEinzelneKoordinatenAchse_Y(g2, i1);  // hier werden auch die Ticks fuer den Grid berechnet
            } else {
                zeichneEinzelneSIGNALKoordinatenAchse_Y(g2, i1);
            }
        }
        //------------------------
        zeichneGrid_NormalX(g);
        zeichneGrid_NormalY(g);
        //===============================================
        // nachfolgend werden die X- und die Y-Achse noch einmal gezeichnet, damit sie nicht eventuell vom Grid (der eine andere Farbe haben kann)
        // ueberdeckt werden
        // die Methoden 'this.zeichneGrid_NormalX(g)' und 'this.zeichneGrid_NormalY(g)' koennen nicht vor die Schleife zum Zeichnen der Achsen
        // gestellt werden, weil in 'this.zeichneEinzelneKoordinatenAchse_X(g2,i1)' und 'this.zeichneEinzelneKoordinatenAchse_Y(g2,i1)' zuerst einmal
        // der Grid berechnet werden muss
        //
        final GeneralPath grL = new GeneralPath();
        //
        for (int i1 = 0; i1 < anzahlAchsenX; i1++) {
            g2.setColor(farbeAchsenX[i1]);
            // TODO: replace with switch statement
            if (linienStilAchsenX[i1] == SOLID_PLAIN) {
                g2.setStroke(str_SOLID_PLAIN);
            } else if (linienStilAchsenX[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
            } else if (linienStilAchsenX[i1] == SOLID_FAT_1) {
                g2.setStroke(str_SOLID_FAT_1);
            } else if (linienStilAchsenX[i1] == SOLID_FAT_2) {
                g2.setStroke(str_SOLID_FAT_2);
            } else if (linienStilAchsenX[i1] == DOTTED_PLAIN) {
                g2.setStroke(str_DOTTED_PLAIN);
            } else if (linienStilAchsenX[i1] == DOTTED_FAT) {
                g2.setStroke(str_DOTTED_FAT);
            }
            //-----------------------
            // jetzt die Linie ziehen:
            grL.reset();
            grL.moveTo(_xAchseX[i1], _yAchseX[i1]);
            grL.lineTo(_xAchseX[i1] + breitePix[i1], _yAchseX[i1]);
            if (linienStilAchsenX[i1] != GraferV3.INVISIBLE) {
                g2.draw(grL);
                g2.drawString(xAchseBeschriftung[i1], _xAchseX[i1] + breitePix[i1] / 2, _yAchseX[i1] + posXtickLabels[i1]);
            }
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        for (int i1 = 0; i1 < anzahlAchsenY; i1++) {
            g2.setColor(farbeAchsenY[i1]);
            // TODO: replace with switch statement
            if (linienStilAchsenY[i1] == SOLID_PLAIN) {
                g2.setStroke(str_SOLID_PLAIN);
            } else if (linienStilAchsenY[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
            } else if (linienStilAchsenY[i1] == SOLID_FAT_1) {
                g2.setStroke(str_SOLID_FAT_1);
            } else if (linienStilAchsenY[i1] == SOLID_FAT_2) {
                g2.setStroke(str_SOLID_FAT_2);
            } else if (linienStilAchsenY[i1] == DOTTED_PLAIN) {
                g2.setStroke(str_DOTTED_PLAIN);
            } else if (linienStilAchsenY[i1] == DOTTED_FAT) {
                g2.setStroke(str_DOTTED_FAT);
            }
            //-----------------------
            // jetzt die Linie ziehen:
            grL.reset();
            grL.moveTo(_xAchseY[i1], _yAchseY[i1]);
            grL.lineTo(_xAchseY[i1], _yAchseY[i1] - hoehePix[i1]);
            if (linienStilAchsenY[i1] != GraferV3.INVISIBLE) {
                g2.draw(grL);
                g2.drawString(yAchseBeschriftung[i1], _xAchseY[i1] - posYtickLabels[i1], _yAchseY[i1] - hoehePix[i1] / 2);
            }
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        //==================================
    }

    // UEberschreiben fuer SIGNAL:
    protected void zeichneEinzelneSIGNALKoordinatenAchse_Y(final Graphics2D g2, final int i1) {

        GeneralPath grL = new GeneralPath();
        // i1 ... AchsenNummer --> Achtung: pro Graph gibt es je zwei y-Achsen
        // SIGNAL --> y-Achse ist IMMER 'LIN'
        //
        //==================================
        // es gibt einen y-Tick bei '0' und einen bei '1'; und zwar fuer jeden SIGNAL-Verlauf innerhalb des entsprechenden Graphen
        int z = 0;
        for (int i3 = 0; i3 < indexDerKurveInDerMatrix.length; i3++) {
            final int grf = indexDerKurveInDerMatrix[i3] / 1000;
            if (grf == i1 / 2) {
                z++;
            }
        }
        //------------------------
        //
        final int anzTicks = 2 * z;
        wertTickY[i1] = new double[anzTicks];  // zum Tick gehoeriger y-Zahlenwert --> wird hier nicht verwendet
        tickY[i1] = new int[anzTicks];  // Pixel-Position
        tickY[i1][0] = _yAchseY[i1];
        for (int i2 = 0; i2 < anzTicks; i2++) {
            if (i2 % 2 == 0) {
                wertTickY[i1][i2] = 0.0;
                if (i2 > 0) {
                    tickY[i1][i2] = tickY[i1][i2 - 1] - sgnDistance[i1];
                }
            } else {
                wertTickY[i1][i2] = 1.0;
                tickY[i1][i2] = tickY[i1][i2 - 1] - sgnHeight[i1];
            }
        }
        // keine Minor-Ticks bei SIGNAL -->
        final int yMinorTicksAnzahl = 0;
        wertTickYminor[i1] = new double[yMinorTicksAnzahl];
        tickYminor[i1] = new int[yMinorTicksAnzahl];
        //==================================
        if (i1 % 2 != 0) {
            return;  // nur linke y-Achse wird gezeichnet!
        }        //
        g2.setColor(farbeAchsenY[i1]);

        // TODO: replace with switch expression!
        if (linienStilAchsenY[i1] == SOLID_PLAIN) {
            g2.setStroke(str_SOLID_PLAIN);
        } else if (linienStilAchsenY[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
        } else if (linienStilAchsenY[i1] == SOLID_FAT_1) {
            g2.setStroke(str_SOLID_FAT_1);
        } else if (linienStilAchsenY[i1] == SOLID_FAT_2) {
            g2.setStroke(str_SOLID_FAT_2);
        } else if (linienStilAchsenY[i1] == DOTTED_PLAIN) {
            g2.setStroke(str_DOTTED_PLAIN);
        } else if (linienStilAchsenY[i1] == DOTTED_FAT) {
            g2.setStroke(str_DOTTED_FAT);
        } else {
            System.out.println("Fehler: hhqqt5");
        }
        //-----------------------
        // jetzt die Linie ziehen:
        grL.reset();
        grL.moveTo(_xAchseY[i1], _yAchseY[i1]);
        grL.lineTo(_xAchseY[i1], _yAchseY[i1] - hoehePix[i1]);
        if (linienStilAchsenY[i1] != GraferV3.INVISIBLE) {
            g2.draw(grL);
            g2.drawString(yAchseBeschriftung[i1], _xAchseY[i1] - posYtickLabels[i1], _yAchseY[i1] - hoehePix[i1] / 2);
        }
        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        //==================================
    }

    private void reorderLine(final int[] positionSIGNAL, int z1, int z2) {
        // die zu bearbeitende Zeile ist im Array 'positionSIGNAL[]' von z1 nach z2 markiert --> 
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
                for (int i1 = 0; i1 < toBeOrdered.length; i1++) {
                    if (toBeOrdered[i1] == zahl) {
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

    // y-Reihenfolge der SIGNAL-Verlaeufe -->
    // wenn 'kurvenanzahl' veraendert wurde, muessen die unveraenderten SIGNAL-ZVs erhalten bleiben!
    // daher muss der alte Wert von 'indexDerKurveInDerMatrix[]' gespeichert sein, und eine entsprechende Umrechnung erfolgen
    private void setzeYPositionDerSIGNALverlaeufe() {
        //-------------------------------------
        if (positionSIGNAL != null) {
            //------------------------
            // aktualisiert, weil sich die Kurvenanzahl geaendert haben koennte, zuwerst alles mit '-1' markieren --> 
            positionSIGNAL = new int[kurvenanzahl];
            for (int i1 = 0; i1 < kurvenanzahl; i1++) {
                positionSIGNAL[i1] = -1;
            }
            // 
            // alte SIGNAL-Positionen werden in das neue 'positionSIGNAL'-Feld hineinkopiert,
            // die nicht ueberschriebenen Werte sind weiterhin mit negativem Vorzeichen markiert
            for (int i1 = 0; i1 < indexDerKurveInDerMatrix.length; i1++) {
                for (int i2 = 0; i2 < indexDerKurveInDerMatrixALT.length; i2++) {
                    if (indexDerKurveInDerMatrix[i1] == indexDerKurveInDerMatrixALT[i2]) {
                        positionSIGNAL[i1] = positionSIGNAL_ALT[i2];
                    }
                }
            }
            // 
            // jetzt Zeile fuer Zeile die ConnectionMatrix durchgehen:
            // in jeder Zeile die alten Eintraege von 0 aufwaerts durchgehend nummerieren, 
            // dann die '-1'-Eintraege weitergehend aufsteigend nummerieren --> 
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
            // Anm.: die Kurve in der Matrix ganz links hat immer 'positionSIGNAL==0'
            //
            this.speichereALTeWerteFuerPosition();
            //-------------------------------------
        }
        //for (int i1=0;  i1<positionSIGNAL.length;  i1++) System.out.println(i1+"  (1)  "+positionSIGNAL[i1]);  System.out.println("-----------");
        //System.out.println("******************************");
    }

    // fuer Zugriff von 'DialogOrdnungSIGNAL' --> 
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

    public void berechneNotwendigeHoeheSIGNALGraph() {
        //-------------------------------------
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            notwendigeHoehePixGRF[i1] = -1;  // default --> kein SIGNAL-Graph sondern ZV-Graph
            if (diagramTyp[i1] == GraferImplementation.DIAGRAM_TYP_SGN) {
                int anzSGN = 0;  // Anzahl der SIGNAL-Verlaeufe pro SIGNAL-Graph
                for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                    if (crvAchsenTyp[i1][i2] == GraferImplementation.ZUORDNUNG_SIGNAL) {
                        anzSGN++;
                    }
                }
                notwendigeHoehePixGRF[i1] = anzSGN * (sgnHeight[i1] + sgnDistance[i1]);
                notwendigeHoehePixGRF[i1] += (DY_IN_OBEN + DY_IN_UNTEN);
                //System.out.println(notwendigeHoehePixGRF[i1]+"   "+i1+"   "+anzSGN);
            }
        }
    }

    private int getHoeheFuerZVInPixel() {
        // die Hoehe, die fuer die ZVs zur Verfuegung steht, dh. Gesamthoehe minus SIGNAL-Hoehen -->
        int height = this.getHeight() - ABSTAND_BESCHRIFTUNG_XACHSE;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == GraferImplementation.DIAGRAM_TYP_SGN) {
                height -= notwendigeHoehePixGRF[i1];
            }
        }
        return height;
    }

    private void zeichneEinzelneSIGNALKurve(Graphics2D g2, int i1) {

        final GeneralPath grL = new GeneralPath();
        // i1 ... KurvenNummer
        //===============================================
        // nur die berechneten Datenpunkte werden auch gezeichnet --> zvCounter
        //----------------------
        final int[] xPix = new int[_zvCounter];
        final int[] yPix = new int[_zvCounter];
        final int x0Kurve = _xAchseX[indexZurKurveGehoerigeXachse[i1]];  // zugehoerige x-Achse definiert x0 der Kurve
        final int y0Kurve = _yAchseY[indexZurKurveGehoerigeYachse[i1]] - (notwendigeHoehePixGRF[indexZurKurveGehoerigeXachse[i1]] - (DY_IN_OBEN + DY_IN_UNTEN));  // zugehoerige y-Achse definiert y0 der Kurve, 'notwendigeHoehePixGRF[i1]' zur optischen Invertierung (Kurve links ganz oben)
        final int delta = sgnDistance[indexZurKurveGehoerigeXachse[i1]] + sgnHeight[indexZurKurveGehoerigeXachse[i1]];

        //
        for (int i2 = 0; i2 < _zvCounter; i2++) {
            final double xValue = worksheetDaten.getValue(kurve_index_worksheetKolonnen_XY[i1][0], i2);
            if (xAchseTyp[indexZurKurveGehoerigeXachse[i1]] == ACHSE_LIN) {
                xPix[i2] = x0Kurve + (int) (sfX[indexZurKurveGehoerigeXachse[i1]] * (xValue - achseXmin[indexZurKurveGehoerigeXachse[i1]]));
            } else if ((xAchseTyp[indexZurKurveGehoerigeXachse[i1]] == ACHSE_LOG)) {
                xPix[i2] = x0Kurve + (int) (sfX[indexZurKurveGehoerigeXachse[i1]] * this.lg10(xValue / achseXmin[indexZurKurveGehoerigeXachse[i1]]));
            }
            //------------------
            double yValue = worksheetDaten.getValue(kurve_index_worksheetKolonnen_XY[i1][1], i2);
            // Schwelle macht aus dem Analogsignal ein Digital-Signal -->
            try {
                if (yValue < sgnSchwelle[indexZurKurveGehoerigeXachse[i1]]) {
                    yValue = positionSIGNAL[i1] * delta + sgnHeight[indexZurKurveGehoerigeXachse[i1]];
                } else {
                    yValue = (positionSIGNAL[i1] * delta);
                }
                yPix[i2] = (y0Kurve + sgnDistance[indexZurKurveGehoerigeXachse[i1]]) + (int) yValue;
            } catch (Exception e) {
                System.out.println("Fehler: 5z6z4r447 " + e + "    kurvenanzahl= " + kurvenanzahl + "      i1= " + i1 + "     " + positionSIGNAL.length);
            }
        }
        //--------------------------------
        g2.setColor(kurveFarbe[i1]);

        // TODO: replace with switch statement
        if (kurveLinienstil[i1] == SOLID_PLAIN) {
            g2.setStroke(str_SOLID_PLAIN);
        } else if (kurveLinienstil[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
        } else if (kurveLinienstil[i1] == SOLID_FAT_1) {
            g2.setStroke(str_SOLID_FAT_1);
        } else if (kurveLinienstil[i1] == SOLID_FAT_2) {
            g2.setStroke(str_SOLID_FAT_2);
        } else if (kurveLinienstil[i1] == DOTTED_PLAIN) {
            g2.setStroke(str_DOTTED_PLAIN);
        } else if (kurveLinienstil[i1] == DOTTED_FAT) {
            g2.setStroke(str_DOTTED_FAT);
        } else {
            assert false;
        }
        //-----------------------
        // zur Beschriftung der SIGNAL-ZV im Graph -->
        final String name = signalNamen[indexDerKurveInDerMatrix[i1] % 1000];
        if (xSchieberAktiv) {
            g2.drawString(
                    (ySchieberWert[indexDerKurveInDerMatrix[i1] % 1000 - 1][0] < sgnSchwelle[indexZurKurveGehoerigeXachse[i1]] ? "off" : "on"),
                    DX_IN_LINKS - 30,
                    (y0Kurve + sgnDistance[indexZurKurveGehoerigeXachse[i1]]) + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexZurKurveGehoerigeXachse[i1]]));
        }
        g2.drawString(
                name,
                this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y,
                (y0Kurve + sgnDistance[indexZurKurveGehoerigeXachse[i1]]) + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexZurKurveGehoerigeXachse[i1]]));
        //-----------------------
        // jetzt die SIGNAL-Linie ziehen:
        g2.setClip(x0Kurve + 1, 0, breitePix[indexZurKurveGehoerigeYachse[i1]] - 2, 10000);

        grL.reset();
        if (kurveLinienstil[i1] != GraferV3.INVISIBLE) {
            grL.moveTo(xPix[0], yPix[0]);
            for (int i5 = 1; i5 < _zvCounter; i5++) {
                if (yPix[i5] != yPix[i5 - 1]) {  // Umschaltvorgang wird in der Mitte zwischen 2 Datenpunkten realisiert --> optische Verbesserung
                    grL.lineTo((xPix[i5 - 1] + xPix[i5]) / 2, yPix[i5 - 1]);
                    grL.lineTo((xPix[i5 - 1] + xPix[i5]) / 2, yPix[i5]);
                }
                grL.lineTo(xPix[i5], yPix[i5]);
            }
            //---------------
            // optional Farbfuellung der Digital-Signale zur besseren Unterscheidung von '0' und '1' --> 
            final GeneralPath grFill = new GeneralPath();
            grFill.append(grL.getPathIterator(null), false);
            final int nullLinie = (y0Kurve + sgnDistance[indexZurKurveGehoerigeXachse[i1]]) + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexZurKurveGehoerigeXachse[i1]]);
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
                final Color fuellFarbe = GraferV3.selectColor(crvFillingDigitalColor[im1][im2]);
                g2.setColor(fuellFarbe);
                g2.fill(grFill.createTransformedShape(null));
            }
            //---------------
            g2.setColor(kurveFarbe[i1]);

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

    // wird aufgerufen von 'DigitalDialogGraphProperties' und 'DialogGraphProperties' --> 
    public void definiereAchsenbegrenzungenImAutoZoom() {
        this.definiereAchsenbegrenzungenImAutoZoom(worksheetDaten);
    }

    private void definiereAchsenbegrenzungenImAutoZoom(DataContainer ws) {
        //--------------------------
        final double[] tickAbstandY = new double[ANZ_DIAGRAM_MAX], tickAbstandY2 = new double[ANZ_DIAGRAM_MAX];
        //--------------------------
        // zur Effizienz-Steigerung: pro WS-Spalte werden kleinster und groesster Wert bestimmt -->
        final double[] w1 = new double[ws.getRowLength()], w2 = new double[ws.getRowLength()];
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {
            w1[i1] = +1e99;
            w2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {  // geht durch die Spalten
            for (int i2 = 0; i2 < _zvCounter; i2++) {  // geht Zeile fuer Zeile durch die selektierte Spalte
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
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {   // geht durch die Zeilen
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) {
                    minX[i1] = w1[i2];
                    maxX[i1] = w2[i2];
                    // --> ausreichend, weil es nur eine X-Achse pro Matrix-Zeile gibt
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                    // --> Vergleich mit den Begrenzungen der eventuell anderen Y-Achsen
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_SIGNAL) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                    // ist nur dann notwendig, wenn man Umschaltet DIGITAL --> ANALOG
                }
            }
            // wenn Y definiert ist, nicht aber Y2, dann gibt es noch keine Achsenbegrenzung fuer die Y2-Achse (und umgekehrt)
            // --> die Y2-Begrenzungen werden dann gleich den Y-Begrenzungen gesetzt (und umgekehrt) -->
            //if ((minY2[i1]==+1e99)||(maxY2[i1]==-1e99)) { minY2[i1]=minY[i1];   maxY2[i1]=maxY[i1]; }
            //if ((minY[i1] ==+1e99)||(maxY[i1] ==-1e99)) { minY[i1]= minY2[i1];  maxY[i1]= maxY2[i1]; }
            //
            // 'schoenere' Bereichsgrenzen -->
            final double[] autoEmpf = auto_Achsenbegrenzung_Wertempfehlung(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            tickAbstandY[i1] = autoEmpf[4];
        }
        //--------------------------
        double[] xx1 = new double[anzGrfVisible], xx2 = new double[anzGrfVisible];  // X-Achse
        double[] yy1 = new double[anzGrfVisible], yy2 = new double[anzGrfVisible];  // Y-Achse  --> Muss noch individuell angepasst werden!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // ist Auto-Scaling eingeschaltet?
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
        final DataContainer ws = this.worksheetDaten;
        final double[] tickAbstandY = new double[ANZ_DIAGRAM_MAX], tickAbstandY2 = new double[ANZ_DIAGRAM_MAX];
        //--------------------------
        // zur Effizienz-Steigerung:
        // pro WS-Spalte werden kleinster und groesster Wert bestimmt -->
        final double[] w1 = new double[worksheetDaten.getRowLength()], w2 = new double[worksheetDaten.getRowLength()];
        for (int i1 = 0; i1 < w1.length; i1++) {
            w1[i1] = +1e99;
            w2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {  // geht durch die Spalten
            for (int i2 = 0; i2 < _zvCounter + 1; i2++) {  // geht Zeile fuer Zeile durch die selektierte Spalte
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
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {   // geht durch die Zeilen
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) {
                    minX[i1] = t1;
                    maxX[i1] = t2;
                    // --> ausreichend, weil es nur eine X-Achse pro Matrix-Zeile gibt
                    if (minX[i1] == maxX[i1]) {
                        minX[i1] = 0;
                        maxX[i1] = 0.020;
                    }
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                }
            }
            // wenn Y definiert ist, nicht aber Y2, dann gibt es noch keine Achsenbegrenzung fuer die Y2-Achse (und umgekehrt)
            // --> die Y2-Begrenzungen werden dann gleich den Y-Begrenzungen gesetzt (und umgekehrt) -->
            //if ((minY2[i1]==+1e99)||(maxY2[i1]==-1e99)) { minY2[i1]=minY[i1];   maxY2[i1]=maxY[i1]; }
            //if ((minY[i1] ==+1e99)||(maxY[i1] ==-1e99)) { minY[i1]= minY2[i1];  maxY[i1]= maxY2[i1]; }
            //
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
                if ((matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y)) {
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
    public void setzeKurven() {
        if (matrixZuordnungKurveDiagram == null) {
            return;
        }
        //-------------------------------------
        kurvenanzahl = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == GraferImplementation.ZUORDNUNG_Y) || (matrixZuordnungKurveDiagram[i1][i2] == GraferImplementation.ZUORDNUNG_SIGNAL)) {
                    kurvenanzahl++;
                }
            }
        }
        this.setzeKurvenAnzahl(kurvenanzahl);
        //-------------------------------------
        this.speichereALTeWerteFuerPosition();
        //
        indexDerKurveInDerMatrix = new int[kurvenanzahl];  // zur Zuordnung der Kurven-Indizes zur Zuordnungsmatrix
        //
        int[] zugehoerigkeitX = new int[kurvenanzahl];
        int[] zugehoerigkeitY = new int[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            zugehoerigkeitX[i1] = -1;
            zugehoerigkeitY[i1] = -1;
        }

        for (int kurvenIndex = 0; kurvenIndex < kurvenanzahl; kurvenIndex++) {
            for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
                final int zugX = i1;  // weil alle Kurven eines Graphen die gleiche X-Achse sehen
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if ((matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_Y) || (matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_SIGNAL)) {
                        zugehoerigkeitX[kurvenIndex] = zugX;
                        zugehoerigkeitY[kurvenIndex] = i1;
                        indexDerKurveInDerMatrix[kurvenIndex] = 1000 * i1 + i2;
                        kurvenIndex++;
                    }
                }
            }
        }
        this.setzeZugehoerigkeitKurveAchsen(zugehoerigkeitX, zugehoerigkeitY);
        //-------------------------------------
        this.setzeYPositionDerSIGNALverlaeufe();
        //-------------------------------------
        indexWsXY = new int[kurvenanzahl][2];
        //
        for (int kurvenIndex = 0; kurvenIndex < kurvenanzahl; kurvenIndex++) {
            for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
                int zugX = -1;
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if (matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_X) {
                        zugX = i2;
                    }
                }
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if ((matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_Y) || (matrixZuordnungKurveDiagram[i1][i2] == this.ZUORDNUNG_SIGNAL)) {
                        indexWsXY[kurvenIndex][0] = zugX;
                        indexWsXY[kurvenIndex][1] = i2;
                        kurvenIndex++;
                    }
                }
            }
        }
        this.setzeKurveIndexWorksheetKolonnenXY(indexWsXY);
        //
        //=====================================
        int[] crvAchsenTypLok = new int[kurvenanzahl];  // Fuer jeden Matrix-Eintrag gibt es einen eindeutigen Achsen-Typ (X oder Y oder Y2)
        int[] crvLineStyleLok = new int[kurvenanzahl];
        int[] crvLineColorLok = new int[kurvenanzahl];
        final double[] crvTransparencyLok = new double[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / 1000);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % 1000);
            crvAchsenTypLok[i1] = crvAchsenTyp[im1][im2];
            crvLineStyleLok[i1] = crvLineStyle[im1][im2];
            crvLineColorLok[i1] = crvLineColor[im1][im2];
            crvTransparencyLok[i1] = crvTransparency[im1][im2];
        }
        this.setzeKurveLinienstil(crvLineStyleLok);
        //
        Color[] linienFarbe = new Color[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            linienFarbe[i1] = GraferV3.selectColor(crvLineColorLok[i1]);
        }
        this.setzeKurveFarbe(linienFarbe);
        this.setzeKurveTransparenz(crvTransparencyLok);
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
            crvSymbFarbeLok[i1] = GraferV3.selectColor(crvSymbColorLok[i1]);
        }
        this.setzeKurvePunktSymbolAnzeigen(crvSymbShowLok, crvSymbFrequLok, crvSymbShapeLok, crvSymbFarbeLok);
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
        this.setzeKurveClipping(crvClipValXminLok, crvClipValXmaxLok, crvClipValYminLok, crvClipValYmaxLok, crvClipXminLok, crvClipXmaxLok, crvClipYminLok, crvClipYmaxLok);
        //=====================================
    }

    @Override
    public void setzeAchsen() {
        //-------------------------------------
        anzGrfVisible = 0;  // Anzahl der anzuzeigenden Graphen (d.h.  visible==true)
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            anzGrfVisible++;
        }
        this.setzeAchsenAnzahl(anzGrfVisible, anzGrfVisible);
        // 
        zeichneDiagrammUmrandung = new boolean[anzGrfVisible];
        //-----------
        // Bereichsgrenzen fuers Maus-Klicken --> wird hier fuer 2 Diagramme definiert
        final int breite = this.getWidth(), hoehe = this.getHoeheFuerZVInPixel();
        xGrfMIN = new int[anzGrfVisible];
        xGrfMAX = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xGrfMIN.length; i1++) {
            xGrfMIN[i1] = 0;
            xGrfMAX[i1] = this.getWidth();
        }
        yGrfMIN = new int[anzGrfVisible];
        yGrfMAX = new int[anzGrfVisible];
        double ySpGes = 0;   // Gewichtung der y-Achsen
        int iyy = 1;  // Index fuer y-Achse
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
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
        int ix = 0, iy = 0;  // Index fuer x- und y-Achse
        ySpGes = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
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
        this.setzeAchsenBreiteHoeheX0Y0(laenge_xAchse, laenge_yAchse, posX_xAchse, posY_xAchse, posX_yAchse, posY_yAchse);
        //-----------
        double[] x1 = new double[anzGrfVisible], x2 = new double[anzGrfVisible];  // X-Achse
        double[] y1 = new double[anzGrfVisible], y2 = new double[anzGrfVisible];  // Y-Achse  --> Muss noch individuell angepasst werden!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // ist Auto-Scaling eingeschaltet?
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
            xAchseBeschriftungLok[i1] = xAchseBeschriftung[i1];
        }
        for (int i1 = 0; i1 < yAchseBeschriftungLok.length; i1++) {
            yAchseBeschriftungLok[i1] = yAchseBeschriftung[i1];
        }
        this.setzeAchsenBeschriftungen(xAchseBeschriftungLok, yAchseBeschriftungLok);
        //-----------
        int[] xAchseTypLoc = new int[anzGrfVisible], yAchseTypLoc = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseTypLoc.length; i1++) {
            xAchseTypLoc[i1] = xAchsenTyp[i1];
        }
        for (int i1 = 0; i1 < yAchseTypLoc.length; i1++) {
            yAchseTypLoc[i1] = yAchsenTyp[i1];
        }
        this.setzeAchsenTyp(xAchseTypLoc, yAchseTypLoc);
        //-----------
        Color[] xAchseFarbeLok = new Color[anzGrfVisible], yAchseFarbeLok = new Color[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseFarbeLok.length; i1++) {
            xAchseFarbeLok[i1] = GraferV3.selectColor(xAchseFarbe[i1]);
        }
        for (int i1 = 0; i1 < yAchseFarbeLok.length; i1++) {
            yAchseFarbeLok[i1] = GraferV3.selectColor(yAchseFarbe[i1]);
        }
        this.setzeAchsenFarbe(xAchseFarbeLok, yAchseFarbeLok);
        //-----------
        int[] xAchseStilLok = new int[anzGrfVisible], yAchseStilLok = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseStilLok.length; i1++) {
            xAchseStilLok[i1] = xAchseStil[i1];
        }
        for (int i1 = 0; i1 < yAchseStilLok.length; i1++) {
            yAchseStilLok[i1] = yAchseStil[i1];
        }
        this.setzeAchsenLinienStil(xAchseStilLok, yAchseStilLok);
        //-----------
        //
        gridNormalX_zugeordneteXAchse = new int[anzGrfVisible];  // Grid normal auf X-Achse
        gridNormalX_zugeordneteYAchse = new int[anzGrfVisible];  // Grid normal auf X-Achse
        for (int i1 = 0; i1 < gridNormalX_zugeordneteXAchse.length; i1++) {
            gridNormalX_zugeordneteXAchse[i1] = i1;
        }
        for (int i1 = 0; i1 < gridNormalX_zugeordneteYAchse.length; i1++) {
            gridNormalX_zugeordneteYAchse[i1] = i1;
        }
        this.definiereGridNormalX(gridNormalX_zugeordneteXAchse, gridNormalX_zugeordneteYAchse);
        //
        gridNormalY_zugeordneteXAchse = new int[anzGrfVisible];  // Grid normal auf Y-Achse
        gridNormalY_zugeordneteYAchse = new int[anzGrfVisible];  // Grid normal auf Y-Achse
        for (int i1 = 0; i1 < gridNormalY_zugeordneteXAchse.length; i1++) {
            gridNormalY_zugeordneteXAchse[i1] = i1;
        }
        for (int i1 = 0; i1 < gridNormalY_zugeordneteYAchse.length; i1++) {
            gridNormalY_zugeordneteYAchse[i1] = i1;
        }
        this.definiereGridNormalY(gridNormalY_zugeordneteXAchse, gridNormalY_zugeordneteYAchse);
        //
        final Color[] farbeGridNormalXLok = new Color[anzGrfVisible];
        final Color[] farbeGridNormalXminorLok = new Color[farbeGridNormalXLok.length];
        for (int i1 = 0; i1 < farbeGridNormalXLok.length; i1++) {
            farbeGridNormalXLok[i1] = GraferV3.selectColor(farbeGridNormalX[i1]);
            farbeGridNormalXminorLok[i1] = GraferV3.selectColor(farbeGridNormalXminor[i1]);
        }
        final Color[] farbeGridNormalYLok = new Color[anzGrfVisible];
        final Color[] farbeGridNormalYminorLok = new Color[farbeGridNormalYLok.length];
        for (int i1 = 0; i1 < farbeGridNormalYLok.length; i1++) {
            farbeGridNormalYLok[i1] = GraferV3.selectColor(farbeGridNormalY[i1]);
            farbeGridNormalYminorLok[i1] = GraferV3.selectColor(farbeGridNormalYminor[i1]);
        }
        this.setzeGridFarben(farbeGridNormalXLok, farbeGridNormalYLok, farbeGridNormalXminorLok, farbeGridNormalYminorLok);
        //-----------
        int[] linStilGridNormalXLok = new int[anzGrfVisible];
        int[] linStilGridNormalXminorLok = new int[farbeGridNormalXLok.length];
        for (int i1 = 0; i1 < linStilGridNormalXLok.length; i1++) {
            linStilGridNormalXLok[i1] = linStilGridNormalX[i1];
            linStilGridNormalXminorLok[i1] = linStilGridNormalXminor[i1];
        }
        final int[] linStilGridNormalYLok = new int[anzGrfVisible];
        final int[] linStilGridNormalYminorLok = new int[farbeGridNormalYLok.length];
        for (int i1 = 0; i1 < linStilGridNormalYLok.length; i1++) {
            linStilGridNormalYLok[i1] = linStilGridNormalY[i1];
            linStilGridNormalYminorLok[i1] = linStilGridNormalYminor[i1];
        }
        this.setzeGridLinienStil(linStilGridNormalXLok, linStilGridNormalYLok, linStilGridNormalXminorLok, linStilGridNormalYminorLok);
        //-----------
        final int[][] showGridNormalXmajLok = new int[anzGrfVisible][2], showGridNormalXminLok = new int[anzGrfVisible][2];

        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalX_zugeordneteXAchse[i1];
            final int indexAchseY = gridNormalX_zugeordneteYAchse[i1];
            showGridNormalXmajLok[i1][0] = (xShowGridMaj[i1]) ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            showGridNormalXmajLok[i1][1] = (xShowGridMaj[i1]) ? indexAchseY : -1;
            showGridNormalXminLok[i1][0] = (xShowGridMin[i1]) ? indexAchseX : -1;
            showGridNormalXminLok[i1][1] = (xShowGridMin[i1]) ? indexAchseY : -1;
        }
        int[][] yShowGridMajor = new int[anzGrfVisible][2], yShowGridMinor = new int[anzGrfVisible][2];

        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalY_zugeordneteXAchse[i1];
            final int indexAchseY = gridNormalY_zugeordneteYAchse[i1];
            yShowGridMajor[i1][0] = (yShowGridMaj[i1]) ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            yShowGridMajor[i1][1] = (yShowGridMaj[i1]) ? indexAchseY : -1;
            yShowGridMinor[i1][0] = (yShowGridMin[i1]) ? indexAchseX : -1;
            yShowGridMinor[i1][1] = (yShowGridMin[i1]) ? indexAchseY : -1;
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
            xAnzTicksMinorLok[i1] = xAnzTicksMinor[i1];
        }
        for (int i1 = 0; i1 < yAnzTicksMinorLok.length; i1++) {
            yAnzTicksMinorLok[i1] = yAnzTicksMinor[i1];
        }
        this.setzeTickAnzMinor(xAnzTicksMinorLok, yAnzTicksMinorLok);
        //-----------
        final int[] xTickLength = new int[anzGrfVisible], xTickLengthMin = new int[anzGrfVisible];
        final int[] yTickLength = new int[anzGrfVisible], yTickLengthMin = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xTickLength.length; i1++) {
            xTickLength[i1] = xTickLaenge[i1];
            xTickLengthMin[i1] = xTickLaengeMinor[i1];
        }
        for (int i1 = 0; i1 < yTickLength.length; i1++) {
            yTickLength[i1] = yTickLaenge[i1];
            yTickLengthMin[i1] = yTickLaengeMinor[i1];
        }
        this.setzeTickLaenge(xTickLength, yTickLength, xTickLengthMin, yTickLengthMin);
        //-----------
        final boolean[] showLabelsXMaj = new boolean[anzGrfVisible], showLabelsXMin = new boolean[anzGrfVisible];
        final boolean[] showLabelsYMax = new boolean[anzGrfVisible], showLabelsYMin = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < showLabelsXMaj.length; i1++) {
            showLabelsXMaj[i1] = zeigeLabelsXmaj[i1];
            showLabelsXMin[i1] = zeigeLabelsXmin[i1];
        }
        for (int i1 = 0; i1 < showLabelsYMax.length; i1++) {
            showLabelsYMax[i1] = zeigeLabelsYmaj[i1];
            showLabelsYMin[i1] = zeigeLabelsYmin[i1];
        }
        this.setzeTickLabelAnzeige(showLabelsXMaj, showLabelsYMax, showLabelsXMin, showLabelsYMin);
        //-----------
        boolean[] zeigeXticksUnten = new boolean[anzGrfVisible], zeigeYticksLinks = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < zeigeXticksUnten.length; i1++) {
            zeigeXticksUnten[i1] = true;
        }
        for (int i1 = 0; i1 < zeigeYticksLinks.length; i1++) {
            zeigeYticksLinks[i1] = true;
        }
        this.setzeTickAusrichtung(zeigeXticksUnten, zeigeYticksLinks);
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
        // Bereichsgrenzen fuers Maus-Klicken --> wird hier fuer 2 Diagramme definiert
        final int breite = this.getWidth(), hoehe = this.getHoeheFuerZVInPixel();
        xGrfMIN = new int[anzGrfVisible];
        xGrfMAX = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xGrfMIN.length; i1++) {
            xGrfMIN[i1] = 0;
            xGrfMAX[i1] = this.getWidth();
        }
        yGrfMIN = new int[anzGrfVisible];
        yGrfMAX = new int[anzGrfVisible];
        double ySpGes = 0;   // Gewichtung der y-Achsen
        int iyy = 1;  // Index fuer y-Achse
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
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
        int ix = 0, iy = 0;  // Index fuer x- und y-Achse
        ySpGes = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
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
        this.setzeAchsenBreiteHoeheX0Y0(laenge_xAchse, laenge_yAchse, posX_xAchse, posY_xAchse, posX_yAchse, posY_yAchse);
        //-------------
        try {
            this.blendeEventuellGridLinienAus();
        } catch (NullPointerException e) {
            Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "Nullpointer-Exception after resizing.");
        }
    }

    public void blendeEventuellGridLinienAus() {
        //-------------------------------------
        // wenn die Diagramme in einem sehr kleinen Fenster gezeichnet werden, dann muessen eventuell die Grid-Linien ausgeblendet werden, 
        // um eine gewisse Uebersichtlichkeit zu wahren --> 
        // 
        final double px1 = 230, px2 = 100, pxr = 2.5;
        final int[][] showGridXMax = new int[anzGrfVisible][2], showGridXMin = new int[anzGrfVisible][2];
        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalX_zugeordneteXAchse[i1];
            final int indexAchseY = gridNormalX_zugeordneteYAchse[i1];
            showGridXMax[i1][0] = (xShowGridMaj[i1]) ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            showGridXMax[i1][1] = (xShowGridMaj[i1]) ? indexAchseY : -1;
            showGridXMin[i1][0] = (xShowGridMin[i1]) ? indexAchseX : -1;
            showGridXMin[i1][1] = (xShowGridMin[i1]) ? indexAchseY : -1;
            zeichneDiagrammUmrandung[i1] = false;
            if (breitePix[indexAchseX] < px1 * pxr) {
                showGridXMin[i1][0] = -1;
                showGridXMin[i1][1] = -1;
            }
            if (breitePix[indexAchseX] < px2 * pxr) {
                showGridXMax[i1][0] = -1;
                showGridXMax[i1][1] = -1;
                zeichneDiagrammUmrandung[i1] = true;
            }
        }

        final int[][] showGridNormalYmajLok = new int[anzGrfVisible][2], showGridNormalYminLok = new int[anzGrfVisible][2];
        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalY_zugeordneteXAchse[i1];
            final int indexAchseY = gridNormalY_zugeordneteYAchse[i1];
            showGridNormalYmajLok[i1][0] = (yShowGridMaj[i1]) ? indexAchseX : -1;  // '-1' bedeutet: Grid-Linie fuer diese Achsenkombination nicht zeichnen
            showGridNormalYmajLok[i1][1] = (yShowGridMaj[i1]) ? indexAchseY : -1;
            showGridNormalYminLok[i1][0] = (yShowGridMin[i1]) ? indexAchseX : -1;
            showGridNormalYminLok[i1][1] = (yShowGridMin[i1]) ? indexAchseY : -1;
            if (hoehePix[indexAchseY] < px1) {
                showGridNormalYminLok[i1][0] = -1;
                showGridNormalYminLok[i1][1] = -1;
            }
            if (hoehePix[indexAchseY] < px2) {
                showGridNormalYmajLok[i1][0] = -1;
                showGridNormalYmajLok[i1][1] = -1;
                zeichneDiagrammUmrandung[i1] = true;
            }
        }
        this.showGridLines(showGridXMax, showGridXMin, showGridNormalYmajLok, showGridNormalYminLok);
        //-------------------------------------
    }

    public void setMausModus(final int mausModus) {
        this.mausModusALT = this.mausModus;  // alten Zustand abspeichern
        this.mausModus = mausModus;  // in den neuen Zustand gehen
        //--------------------------
        switch (mausModus) {
            case MAUSMODUS_NIX:
                xSchieberAktiv = false;  // aktives Ausschalten des Schiebers
                this.repaint();
                break;
            case MAUSMODUS_ZOOM_AUTOFIT:
                this.mausModus_ZOOM_AUTOFIT();  // xSchieber unveraendert 
                break;
            case MAUSMODUS_ZOOM_FENSTER:
                break;
            case MAUSMODUS_ZEICHNE_LINIE:
                break;
            case MAUSMODUS_WERTANZEIGE_SCHIEBER:
                if (!xSchieberAktiv) {
                    xSchieberAktiv = true;
                    xSchieberPix = _xAchseX[0];  // x-Schieber wird an den Anfang gesetzt: gleich fuer alle Diagramme, in GraferV3 definiert
                    xSchieberPix2 = _xAchseX[0];
                    this.repaint();
                }
                break;
            default:
                Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "message");
        }
        //--------------------------
    }

    //=================================================
    //=================================================
    //=================================================
    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mausModus_ZOOM_FENSTER(mouseX, mouseY, MOUSE_CLICKED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_ZEICHNE_LINIE) {
            mausModus_ZEICHNE_LINIE(mouseX, mouseY, MOUSE_CLICKED);
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mausModus_WERTANZEIGE_SCHIEBER(mouseX, mouseY, MOUSE_CLICKED, me);
        }
    }

    public void mousePressed(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mausModus_ZOOM_FENSTER(mouseX, mouseY, MOUSE_PRESSED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_ZEICHNE_LINIE) {
            mausModus_ZEICHNE_LINIE(mouseX, mouseY, MOUSE_PRESSED);
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mausModus_WERTANZEIGE_SCHIEBER(mouseX, mouseY, MOUSE_PRESSED, me);
        }
    }

    public void mouseReleased(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mausModus_ZOOM_FENSTER(mouseX, mouseY, MOUSE_RELEASED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_ZEICHNE_LINIE) {
            mausModus_ZEICHNE_LINIE(mouseX, mouseY, MOUSE_RELEASED);
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mausModus_WERTANZEIGE_SCHIEBER(mouseX, mouseY, MOUSE_RELEASED, me);
        }
    }

    public void mouseMoved(MouseEvent me) {
    }

    public void mouseDragged(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }


        int mx = me.getX(), my = me.getY();
        if (mausModus == MAUSMODUS_NIX); else if (mausModus == MAUSMODUS_ZOOM_AUTOFIT); else if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mausModus_ZOOM_FENSTER(mx, my, MOUSE_DRAGGED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_ZEICHNE_LINIE) {
            mausModus_ZEICHNE_LINIE(mx, my, MOUSE_DRAGGED);
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mausModus_WERTANZEIGE_SCHIEBER(mx, my, MOUSE_DRAGGED, me);
        }
    }
    //=================================================

    public void mausModus_ZOOM_AUTOFIT() {
        //--------------

        if (worksheetDatenTEMP != null) {
            for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                    worksheetDaten.setValue(worksheetDatenTEMP[i1][i2], i1, i2);
                }
            }
            worksheetDatenTEMP = null;
            _zvCounter = zvCounterTEMP;
            zvCounterTEMP = 0;
            nochNichtGeZoomt = true;
        }
        this.definiereAchsenbegrenzungenImAutoZoom(worksheetDaten);
        mausModus = mausModusALT;
        _scope.aktualisiereMausModus(mausModus);
        //--------------
    }

    void undoZoom() {
        zoomRechteck(true);
    }

    public void mausModus_ZOOM_FENSTER(int mx, int my, int mausAktion, boolean isControlDown, boolean isShiftDown) {



        if (mx < _xAchseX[0]) {
            mx = _xAchseX[0];
        }
        if (mx > _xAchseX[0] + breitePix[0]) {
            mx = _xAchseX[0] + breitePix[0];
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
                System.out.println("Fehler: eorivm3");
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
                System.out.println("Fehler: oweifn03");
                break;
        }
    }

    public void mausModus_WERTANZEIGE_SCHIEBER(int mx, int my, int mausAktion, MouseEvent me) {
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                break;
            //--------------------------
            case MOUSE_RELEASED:
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                if ((mausModus != MAUSMODUS_ZOOM_FENSTER) && (xSchieberAktiv)) {
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
                    if ((me.getModifiers() & me.BUTTON1_MASK) != 0 && !me.isControlDown()) {
                        inDiffMode = false;
                        xSchieberPix = mx;
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
            if (xSchieberPix < _xAchseX[0]) {
                xSchieberPix = _xAchseX[0];
            }
            if (xSchieberPix > _xAchseX[0] + breitePix[0]) {
                xSchieberPix = _xAchseX[0] + breitePix[0];
            }
            try {
                xSchieberWert[0] = getValueFromPixel(xSchieberPix, 0)[0];
            } catch (Exception e) {
            }  // x-Wert der Schieber-Position
            int index = findSliderTimeIndex(xSchieberWert[0]);
            if (index >= 0) {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert[i2][0] = worksheetDaten.getValue(i2 + 1, index);
                }
            } else {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert[i2][0] = 0;
                }
            }
        } else {
            if (xSchieberPix2 < _xAchseX[0]) {
                xSchieberPix2 = _xAchseX[0];
            }
            if (xSchieberPix2 > _xAchseX[0] + breitePix[0]) {
                xSchieberPix2 = _xAchseX[0] + breitePix[0];
            }
            try {
                xSchieberWert2[0] = getValueFromPixel(xSchieberPix2, 0)[0];
            } catch (Exception e) {
            }  // x-Wert der Schieber-Position
            int index = findSliderTimeIndex(xSchieberWert2[0]);
            if (index >= 0) {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert2[i2][0] = worksheetDaten.getValue(i2 + 1, index);
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
                for (int startIndex = 1; startIndex < worksheetDaten.getColumnLength(); startIndex += 10) {
                    if (sliderValue >= worksheetDaten.getValue(0, startIndex - 1)) {
                        i1 = startIndex - 10;
                        break;
                    }
                }
                if (i1 < 1 || i1 > worksheetDaten.getColumnLength() - 1) {
                    i1 = 1;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (i1 = 1; i1 < worksheetDaten.getColumnLength(); i1++) {
                if (sliderValue >= worksheetDaten.getValue(0, i1 - 1)) {
                    if ((sliderValue <= worksheetDaten.getValue(0, i1))) {
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
    }

    public void mausModus_FIBONACCI_LOG(int mx, int my, int mausAktion) {
    }

    // Wenn mit der Maus in das Pixel-Feld geklickt wird -->
    private double[] getValueFromPixel(int xPix, int yPix) {
        //-------------------
        double achseXmin_ = -1, achseYmin_ = -1;
        int xAchseX_ = -1, yAchseY_ = -1;
        double sfX_ = -1, sfY_ = -1;
        int xAchseTyp_ = -1, yAchseTyp_ = -1;
        int indexYAchse = -1;
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
                indexYAchse = indexZurKurveGehoerigeYachse[i1];
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
        return new double[]{xWert, yWert, indexYAchse};
        //-------------------
    }

    // Ermittle (x/y)-Wert in Pixel zu einem Wertepaar -->
    // TODO: why is yWert not final?
    private int[] getPixelFromValue(final double xWert, double yWert, final int index_xAchse, final int index_yAchse) {
        try {
            //-------------------
            final double achseXminLok = achseXmin[index_xAchse];
            final int xAchseXLok = _xAchseX[index_xAchse];
            final double sfX_ = sfX[index_xAchse];
            final int xAchseTyp_ = xAchseTyp[index_xAchse];
            final double achseYmin_ = achseYmin[index_yAchse];
            final int yAchseY_ = _yAchseY[index_yAchse];
            final double sfY_ = sfY[index_yAchse];
            final int yAchseTyp_ = yAchseTyp[index_yAchse];
            //-------------------
            int xPix = -1, yPix = -1;
            if (xAchseTyp_ == ACHSE_LOG) {
                xPix = (int) (sfX_ * Math.log10(xWert / achseXminLok) + xAchseXLok);

            } else if (xAchseTyp_ == ACHSE_LIN) {
                xPix = (int) ((xWert - achseXminLok) * sfX_ + xAchseXLok);
            }
            if (yAchseTyp_ == ACHSE_LOG) {
                yWert = achseYmin_ * Math.pow(10.0, ((yAchseY_ - yPix) / sfY_));
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
    protected void zeichne(final Graphics graphics) {

        //-------------------
        switch (mausModus) {
            case MAUSMODUS_NIX:
                break;
            case MAUSMODUS_ZOOM_AUTOFIT:
                break;
            case MAUSMODUS_ZOOM_FENSTER:
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
            case MAUSMODUS_ZEICHNE_LINIE:
                break;
            case MAUSMODUS_WERTANZEIGE_SCHIEBER:
                // Auch bei einigen anderen MausModus-Einstellungen soll der Schieber sichtbar sein
                // daher: Anzeige abhaengig von 'xSchieberAktiv', siehe unten --> 
                break;
            default:
                Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "Default in case statement reached.");
                break;
        }
        //-------------------
        if (xSchieberAktiv) {

            final int dx = 7, dy = 1, dyFont = 9;
            graphics.setColor(Color.white);
            graphics.fillRect(dx, this.getHeight() - dy - dyFont, 80, dyFont);
            graphics.setColor(Color.red);

            // changed here: don't use the pixel value for the slider, but
            // the x-Value, and re-calculate the pixel from that value
            // otherwise, zooming or changing the window size makes problems/does
            // not update correctly.
            final int xSPix = getPixelFromValue(xSchieberWert[0], 0, 0, 0)[0];
            graphics.drawLine(xSPix, yGrfMIN[0], xSPix, yGrfMAX[anzGrfVisible - 1]);

            cf.setMaximumDigits(6);
            graphics.drawString("t = " + cf.formatT((float) xSchieberWert[0], TechFormat.FORMAT_AUTO), dx, this.getHeight() - dy);

            graphics.setColor(Color.green);
            final int xSPix2 = getPixelFromValue(xSchieberWert2[0], 0, 0, 0)[0];
            graphics.drawLine(xSPix2, yGrfMIN[0], xSPix2, yGrfMAX[anzGrfVisible - 1]);
            graphics.drawString("t = " + cf.formatT((float) xSchieberWert2[0], TechFormat.FORMAT_AUTO), dx + 130, this.getHeight() - dy);

            if (xSchieberWert2[0] >= 0) {
                graphics.setColor(Color.black);
                graphics.drawString("dt = " + cf.formatT((float) (xSchieberWert2[0] - xSchieberWert[0]), TechFormat.FORMAT_AUTO), dx + 260, this.getHeight() - dy);
            }

        }
        
        //-------------------
    }

    private void zoomRechteck(final boolean isUndoZoom) {
        //-------------------
        // (1) fuer eines der Diagramme wird ein Rechteck-Zoom gemacht: 
        //  der x-Bereich gilt auch fuer alle anderen Diagramm, der y-Bereich des entsprechenden Diagramms entspricht dem Zoom-Rechteck 
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
                maxY[indexAxis] = yMAX;  // die ZoomRechteck-Werte fuer Y nur fuer das selektierte Diagramm
            }
            
            
        }

        //-------------------
        // (2) fuer alle anderen Diagramme wird der y-Bereich gefittet --> 
        //
        final double[] value1 = new double[worksheetDaten.getRowLength()], value2 = new double[worksheetDaten.getRowLength()];
        for (int i1 = 0; i1 < worksheetDaten.getRowLength(); i1++) {
            value1[i1] = +1e99;
            value2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < worksheetDaten.getRowLength(); i1++) // geht durch die Spalten
        {
            for (int i2 = 0; i2 < _zvCounter + 1; i2++) {  // geht Zeile fuer Zeile durch die selektierte Spalte
                if (i2 < worksheetDaten.getColumnLength()) {
                    if (worksheetDaten.getValue(i1, i2) < value1[i1]) {
                        value1[i1] = worksheetDaten.getValue(i1, i2);
                    }
                    if (worksheetDaten.getValue(i1, i2) > value2[i1]) {
                        value2[i1] = worksheetDaten.getValue(i1, i2);
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
                if ((i1 != indexAxis) && (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y)) {
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
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // ist Auto-Scaling eingeschaltet?
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

    // In der SCOPE-Darstellung werden die Daten auf einige 1000 (Pixel)punkte in Hi-Lo-Darstellung reduziert, um die Effizient
    // der grafischen Darstellung signifikant zu erhoehen. Wenn man beispielsweise hineinzoomt, dann muessen die Daten mit 
    // deutlich hoeherer Aufloesung nachgeladen und in Hi-Lo-Darstellung uebertragen werden, damit die grafische Darstellung 
    // nicht wichtige Info verliert (zB. 'ausgefranste' Rippelkurven, verschwundene Peaks, Aus Rechtecken werden Dreiecke usw.)
    //
    private void getChangedDataResolution(double x1, double x2) {

        try {
            // vom Simulator gelieferte worksheet[][]-Daten abspeichern solange man nicht weitersimuliert
            if (nochNichtGeZoomt) {
                nochNichtGeZoomt = false;
                worksheetDatenTEMP = new double[worksheetDaten.getRowLength()][worksheetDaten.getColumnLength()];
                for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                    for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                        worksheetDatenTEMP[i1][i2] = worksheetDaten.getValue(i1, i2);
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
            final int lg1 = worksheetDaten.getRowLength(), lg2 = worksheetDaten.getColumnLength();
            final DataContainer wsRAM = _scope.getZVDatenImRAM();  // hochaufloesende Daten im RAM
            int estimatedIndex = (int) (_zvCounter * 1.0 / lg2 * wsRAM.getColumnLength());
            //-------------
            // entsprechende Bereichsgrenzen in RAM-Daten finden -->
            final double xmin = wsRAM.getValue(0, 0);  // exakt
            if (estimatedIndex >= wsRAM.getColumnLength()) {
                estimatedIndex = wsRAM.getColumnLength() - 1;
            }
            double xmax = wsRAM.getValue(0, estimatedIndex);  // estimated

            // falls man mit 'Pause' unvollstaendig simuliert und dann mehr als einmal zoomt:
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
            int zvC = 0;  // lokaler Zaehler in den komprimierten SCOPE-Daten
            final double dtSCOPE = (x2RAM - x1RAM) / INTERVALLE_ENTLANG_X;
            if (dtSCOPE > wsRAM.getTimeIntervalResolution()) {
                int lowerIndex = zeigerX1_RAM;
                int higherIndex = zeigerX2RAM;

                for (int worksheetIndex = 0; worksheetIndex < INTERVALLE_ENTLANG_X + 2; worksheetIndex++) {
                    final double timeValue = x1RAM + worksheetIndex * dtSCOPE;
                    worksheetDaten.setValue(timeValue, 0, 2 * worksheetIndex);
                    worksheetDaten.setValue(timeValue + dtSCOPE, 0, 2 * worksheetIndex + 1);

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
                            oldMeanValue = 0.5 * (worksheetDaten.getValue(i1 + 1, 2 * worksheetIndex - 1) + worksheetDaten.getValue(i1 + 1, 2 * worksheetIndex - 2));
                        } catch (Exception ex) {
                            oldMeanValue = meanValue;
                        }

                        if (meanValue < oldMeanValue) {
                            worksheetDaten.setValue(hiLo.yHi, i1 + 1, 2 * worksheetIndex);
                            worksheetDaten.setValue(hiLo.yLo, i1 + 1, 2 * worksheetIndex + 1);
                        } else {
                            worksheetDaten.setValue(hiLo.yLo, i1 + 1, 2 * worksheetIndex);
                            worksheetDaten.setValue(hiLo.yHi, i1 + 1, 2 * worksheetIndex + 1);
                        }

                    }
                }
                _zvCounter = 2 * INTERVALLE_ENTLANG_X;

            } else { // single point can be resolved:
                for (int i2 = zeigerX1_RAM + 1; i2 < zeigerX2RAM + 1 && i2 < maximumIndex; i2++) {
                    final double time = wsRAM.getValue(0, i2);
                    if (zvC < worksheetDaten.getColumnLength()) {
                        worksheetDaten.setValue(time, 0, zvC);
                        for (int i1 = 0; i1 < lg1 - 1; i1++) {
                            final double value = wsRAM.getValue(i1 + 1, i2);
                            worksheetDaten.setValue(value, i1 + 1, zvC);
                        }
                    }
                    zvC++;
                    _zvCounter = zvC;
                }
            }
            //-------------
            // ZV-Daten in der Worksheet-Anzeige aktualisieren -->
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
                if ((matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X)
                        || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y)) {
                    crvClipValXmin[i1][i2] = this.getXClipAchse(i1, i2)[0];
                    crvClipValXmax[i1][i2] = this.getXClipAchse(i1, i2)[1];
                    crvClipValYmin[i1][i2] = this.getYClipAchse(i1, i2)[0];
                    crvClipValYmax[i1][i2] = this.getYClipAchse(i1, i2)[1];
                }
            }
        }
    }

    public double[] getXClipNo(final int im1, final int im2) {
        // X-Clip --> Worksheet-Daten der zugeordneten X-Achse werden durchsucht
        // CLIP_NO bedeutet: Worksheet-Daten sind begrenzend
        //------------------
        // (1) Aufsuchen der zugeordneten X-Achse:
        int indexX = -1;
        for (int i1 = 0; i1 < worksheetDaten.getRowLength(); i1++) {
            if (matrixZuordnungKurveDiagram[im1][i1] == ZUORDNUNG_X) {
                indexX = i1;
            }
        }
        if (indexX == -1) {
            Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "Index error in plot.");
        }
        // (2) Min- und Max-Werte in dieser Kolonne finden:
        double wsMIN = 1e99, wsMAX = -1e99;
        for (int i1 = 0; i1 < worksheetDaten.getColumnLength(); i1++) {
            if (worksheetDaten.getValue(indexX, i1) < wsMIN) {
                wsMIN = worksheetDaten.getValue(indexX, i1);
            }
            if (worksheetDaten.getValue(indexX, i1) > wsMAX) {
                wsMAX = worksheetDaten.getValue(indexX, i1);
            }
        }
        return new double[]{wsMIN, wsMAX};
    }

    public double[] getYClipNo(final int im1, final int im2) {
        // Y-Clip --> Worksheet-Daten werden durchsucht
        // CLIP_NO bedeutet: Worksheet-Daten sind begrenzend
        double wsMIN = 1e99, wsMAX = -1e99;
        for (int i1 = 0; i1 < worksheetDaten.getColumnLength(); i1++) {
            if (worksheetDaten.getValue(im2, i1) < wsMIN) {
                wsMIN = worksheetDaten.getValue(im2, i1);
            }
            if (worksheetDaten.getValue(im2, i1) > wsMAX) {
                wsMAX = worksheetDaten.getValue(im2, i1);
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
        if (matrixZuordnungKurveDiagram[im1][im2] == ZUORDNUNG_Y) {
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
