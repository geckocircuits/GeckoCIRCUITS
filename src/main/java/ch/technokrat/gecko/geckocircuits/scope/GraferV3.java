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

import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoGraphics2D;
import java.awt.AlphaComposite;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.Font;
import java.awt.Color;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import java.text.NumberFormat;

/*
 * this is the "old scope", in future, replace with "newScope"
 */
@SuppressWarnings("serial")
@Deprecated
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
    public final static Stroke str_SOLID_PLAIN = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);
    public final static Stroke str_INVISIBLE = new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);  // eigentlich unsichtbar
    public final static Stroke str_SOLID_FAT_1 = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);
    public final static Stroke str_SOLID_FAT_2 = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f);
    public final static Stroke str_DOTTED_PLAIN = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f, new float[]{4, 4}, 0);
    public final static Stroke str_DOTTED_FAT = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f, new float[]{4, 4}, 0);
    //
    public static final String[] FARBEN = /*
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
    //
    //-------------------------------------
    // (1) Achsen:
    protected int anzahlAchsenX, anzahlAchsenY;  // Anzahl der x- u. y-Achsen
    protected int[] breitePix, hoehePix;  // Breite und Hoehe der jeweiligen Diagramm-Achsen
    protected int[] _xAchseX, _yAchseX, _xAchseY, _yAchseY;  // Achsen-Koordinaten
    protected int[] xAchseTyp, yAchseTyp;  // Achse linear oder logarithmisch
    protected Color[] farbeAchsenX, farbeAchsenY;  // Farben der einzelnen Achsen, zB. unsichtbar --> weiss
    protected int[] linienStilAchsenX, linienStilAchsenY;  // durchgezogen, gepunktet, fett(???)
    //
    protected double[] achseXmin, achseXmax, achseYmin, achseYmax;  // min- u. max-Zahlenwerte
    protected boolean[] autoAchseXmin, autoAchseXmax, autoAchseYmin, autoAchseYmax;  // sind diese Werte auf AUTO gesetzt??
    protected String[] xAchseBeschriftung, yAchseBeschriftung;
    //-------------
    protected Color[] farbeGridNormalX, farbeGridNormalY, farbeGridNormalXminor, farbeGridNormalYminor;
    protected int[] linStilGridNormalX, linStilGridNormalY, linStilGridNormalXminor, linStilGridNormalYminor;
    protected int[] gridNormalX_zugeordneteXAchse, gridNormalX_zugeordneteYAchse;
    protected int[] gridNormalY_zugeordneteXAchse, gridNormalY_zugeordneteYAchse;
    protected int[][] showGridNormalXmajor, showGridNormalXminor, showGridNormalYmajor, showGridNormalYminor;  // welche Grid-Linien sollen dargestellt werden?
    // --> zB. showGridNormalXmajor[i1]= {index_xAchse, index_yAchse}
    //-------------
    protected boolean[] xTickAutoSpacing, yTickAutoSpacing;  // sollen die Tick-Abstaende automatisch bestimmt werden?
    protected double[] xTickSpacing, yTickSpacing;  // Abstand zwischen 2 Ticks, ausgehend von Null
    protected int[] xAnzTicksMinor, yAnzTicksMinor;  // Zahl der Minor-Ticks zwischen zwei regulaeren Ticks
    protected int[] xTickLaenge, yTickLaenge, xTickLaengeMinor, yTickLaengeMinor;
    protected boolean[] zeigeXticksUnten, zeigeYticksLinks;  // Ticks auf der x-Achse koennen nach unten oder nach oben zeigen, analog die der y-Achse
    //
    protected boolean[] zeigeLabelsXmaj, zeigeLabelsXmin, zeigeLabelsYmaj, zeigeLabelsYmin;  // sollen die entsprechenden Labels bei den jeweiligen Ticks angezeigt werden?
    protected int[] posXtickLabels, posYtickLabels;  // wie weit sind die Tick-Beschriftungen von der jeweiligen Achse entfernt?
    protected Font[] foTickLabelX, foTickLabelY;
    //-------------
    protected int[][] tickX, tickY, tickXminor, tickYminor;  // Pixelpunkt-Position der Ticks
    protected double[][] wertTickX, wertTickY, wertTickXminor, wertTickYminor;  // Zahlenwerte der einzelnen Ticks
    protected double[] sfX, sfY;  // Streckfaktoren fuer Umrechnung Pixel <--> Werte
    //-------------------------------------
    // (2) Kurven:
    protected DataContainer worksheetDaten;  // hier stehen die Punkte aller Kurven drinnen  --> derzeit nur EIN Worksheet implementiert
    //-------------
    protected int[] indexZurKurveGehoerigeXachse, indexZurKurveGehoerigeYachse;  // Zuordnung Kurve <--> Achsen
    protected int anzahlKurven;  // alle in diesem JPanel gezeichneten Kurven
    protected int[][] kurve_index_worksheetKolonnen_XY;  // int[][]{{index_x_kolonne,index_y_kolonne}}
    protected boolean[] kurvenPunktSymbolAnzeigen;  // Sollen die Punkte der Kurve als Punkte dargestellt werden
    protected int[] crvSymbFrequ, crvSymbShape;     // Details zum Zeichnen der Symbole auf den Kurven-Datenpunkten
    protected Color[] crvSymbFarbe;                 // Details zum Zeichnen der Symbole auf den Kurven-Datenpunkten
    protected double[] kurveClippling_xmin, kurveClippling_xmax, kurveClippling_ymin, kurveClippling_ymax;  // definierter Zahlenwert fuers Clipping
    protected int[] clipXmin, clipXmax, clipYmin, clipYmax;  // Art des Cilpping --> "AXIS", "NO CLIP", "VALUE"
    protected int[] kurveLinienstil;
    protected Color[] kurveFarbe;
    //-------------------------------------
    // sonstiges:
    protected NumberFormat nf = NumberFormat.getNumberInstance();
    protected TechFormat tcf = new TechFormat();
    protected int digitsX = 3, digitsY = 3;  // Nachkomma-Stellen bei der Tick-Beschriftung
    //-------------------------------------
    //-------------------------------------
    // speziell (eigentlich gepfuscht):
    // Unterscheidung ZV - Signal -->
    protected int[] kurvenTypZVvsSIGNAL;  // wird direkt ohne 'get'-Fkt. von der abgeleiteten Klasse angeprochen
    //-------------------------------------
    public boolean _antialiasing = true;
    private double[] kurveTransparenz;

    public boolean ladeWorksheetDaten(DataContainer daten) {
        //-----------------------
        this.worksheetDaten = daten;
        // Daten-Konsistenz (grob) pruefen:
        if (daten.getRowLength() < 2) {
            return false;  // nur eine Kolonne --> mindestens 2 erforderlich fuer y=y(x) - Kurve
        }

        this.autoSettingsAnpassen();  // macht erst Sinn, wenn die Kurven-Daten da sind!
        return true;
        //-----------------------
    }

    //-------------------------------------
    // (1) setze Achsen:
    //
    public void setzeAchsenAnzahl(int anzX, int anzY) {
        this.anzahlAchsenX = anzX;
        this.anzahlAchsenY = anzY;
    }

    public void setzeAchsenBreiteHoeheX0Y0(int[] b, int[] h, int[] xX, int[] yX, int[] xY, int[] yY) {
        this.breitePix = b;
        this.hoehePix = h;
        this._xAchseX = xX;
        this._yAchseX = yX;
        this._xAchseY = xY;
        this._yAchseY = yY;
    }

    public void setzeAchsenBegrenzungen(double[] xMin, double[] xMax, boolean[] autoScaleX, double[] yMin, double[] yMax, boolean[] autoScaleY) {
        this.achseXmin = xMin;
        this.autoAchseXmin = autoScaleX;
        this.achseXmax = xMax;
        this.autoAchseXmax = autoScaleX;
        this.achseYmin = yMin;        
        this.autoAchseYmin = autoScaleY;
        this.achseYmax = yMax;
        this.autoAchseYmax = autoScaleY;
    }

    public void setzeAchsenTyp(int[] x, int[] y) {
        this.xAchseTyp = x;
        this.yAchseTyp = y;
    }

    public void setzeAchsenFarbe(Color[] fX, Color[] fY) {
        this.farbeAchsenX = fX;
        this.farbeAchsenY = fY;
    }

    public void setzeAchsenLinienStil(int[] stilX, int[] stilY) {
        this.linienStilAchsenX = stilX;
        this.linienStilAchsenY = stilY;
    }
    //-----------

    public void definiereGridNormalX(int[] zugeordneteXAchse, int[] zugeordneteYAchse) {
        this.gridNormalX_zugeordneteXAchse = zugeordneteXAchse;
        this.gridNormalX_zugeordneteYAchse = zugeordneteYAchse;
        if (zugeordneteXAchse.length != zugeordneteYAchse.length) {
            System.out.println("Fehler 45763425n");
        }
    }

    public void definiereGridNormalY(int[] zugeordneteXAchse, int[] zugeordneteYAchse) {
        this.gridNormalY_zugeordneteXAchse = zugeordneteXAchse;
        this.gridNormalY_zugeordneteYAchse = zugeordneteYAchse;
        if (zugeordneteXAchse.length != zugeordneteYAchse.length) {
            System.out.println("Fehler 908hj4gw4");
        }
    }

    public void setzeGridFarben(Color[] farbeGridNormalX, Color[] farbeGridNormalY, Color[] farbeGridNormalXminor, Color[] farbeGridNormalYminor) {
        this.farbeGridNormalX = farbeGridNormalX;
        this.farbeGridNormalY = farbeGridNormalY;
        this.farbeGridNormalXminor = farbeGridNormalXminor;
        this.farbeGridNormalYminor = farbeGridNormalYminor;
    }

    public void setzeGridLinienStil(int[] linStilGridNormalX, int[] linStilGridNormalY, int[] linStilGridNormalXminor, int[] linStilGridNormalYminor) {
        this.linStilGridNormalX = linStilGridNormalX;
        this.linStilGridNormalY = linStilGridNormalY;
        this.linStilGridNormalXminor = linStilGridNormalXminor;
        this.linStilGridNormalYminor = linStilGridNormalYminor;
    }

    public void showGridLines(int[][] showGridNormalXmajor, int[][] showGridNormalXminor, int[][] showGridNormalYmajor, int[][] showGridNormalYminor) {
        this.showGridNormalXmajor = showGridNormalXmajor;
        this.showGridNormalXminor = showGridNormalXminor;
        this.showGridNormalYmajor = showGridNormalYmajor;
        this.showGridNormalYminor = showGridNormalYminor;
    }
    //-----------

    public void setzeTickAutoSpacing(boolean[] xTickAutoSpacing, boolean[] yTickAutoSpacing) {
        this.xTickAutoSpacing = xTickAutoSpacing;
        this.yTickAutoSpacing = yTickAutoSpacing;
    }

    public void setzeTickSpacing(double[] x, double[] y) {
        this.xTickSpacing = x;
        this.yTickSpacing = y;
    }

    public void setzeTickAnzMinor(int[] x, int[] y) {
        this.xAnzTicksMinor = x;
        this.yAnzTicksMinor = y;
    }

    public void setzeTickLabelAnzeige(boolean[] xMaj, boolean[] yMaj, boolean[] xMin, boolean[] yMin) {
        this.zeigeLabelsXmaj = xMaj;
        this.zeigeLabelsYmaj = yMaj;
        this.zeigeLabelsXmin = xMin;
        this.zeigeLabelsYmin = yMin;
    }

    public void setzeTickLaenge(int[] x, int[] y, int[] xMinor, int[] yMinor) {
        this.xTickLaenge = x;
        this.yTickLaenge = y;
        this.xTickLaengeMinor = xMinor;
        this.yTickLaengeMinor = yMinor;
    }
    // - - - - - - - - - - - - -

    public void setzeTickAusrichtung(boolean[] x, boolean[] y) {
        this.zeigeXticksUnten = x;
        this.zeigeYticksLinks = y;
    }

    public void setzeTickLabelPosition(int[] x, int[] y) {
        this.posXtickLabels = x;
        this.posYtickLabels = y;
    }

    public void setzeTickLabelFont(Font[] foX, Font[] foY) {
        this.foTickLabelX = foX;
        this.foTickLabelY = foY;
    }

    public void setzeAchsenBeschriftungen(String[] x, String[] y) {
        this.xAchseBeschriftung = x;
        this.yAchseBeschriftung = y;
    }
    //-------------------------------------
    // (2) setze Kurven:
    //

    public void setzeKurvenAnzahl(int anz) {
        this.anzahlKurven = anz;
    }

    public void setzeZugehoerigkeitKurveAchsen(int[] indexXachse, int[] indexYachse) {
        this.indexZurKurveGehoerigeXachse = indexXachse;
        this.indexZurKurveGehoerigeYachse = indexYachse;
    }

    public void setzeKurveIndexWorksheetKolonnenXY(int[][] iwkXY) {
        this.kurve_index_worksheetKolonnen_XY = iwkXY;
    }

    public void setzeKurvePunktSymbolAnzeigen(boolean[] sym, int[] crvSymbFrequ, int[] crvSymbShape, Color[] crvSymbFarbe) {
        this.kurvenPunktSymbolAnzeigen = sym;
        this.crvSymbFrequ = crvSymbFrequ;
        this.crvSymbShape = crvSymbShape;
        this.crvSymbFarbe = crvSymbFarbe;
    }

    public void setzeKurveClipping(double[] xmin, double[] xmax, double[] ymin, double[] ymax, int[] clipXmin, int[] clipXmax, int[] clipYmin, int[] clipYmax) {
        this.kurveClippling_xmin = xmin;
        this.kurveClippling_xmax = xmax;
        this.kurveClippling_ymin = ymin;
        this.kurveClippling_ymax = ymax;  // eventuell manuell definierte konkrete Zahlenwerte
        this.clipXmin = clipXmin;
        this.clipXmax = clipXmax;
        this.clipYmin = clipYmin;
        this.clipYmax = clipYmax;         // Art des Cilpping --> "AXIS", "NO CLIP", "VALUE"
    }

    public void setzeKurveLinienstil(int[] kurveLinienstil) {
        this.kurveLinienstil = kurveLinienstil;
    }

    public void setzeKurveFarbe(Color[] f) {
        this.kurveFarbe = f;
    }

    public void setzeKurveTransparenz(double[] trans) {
        this.kurveTransparenz = trans;
    }
    //-------------------------------------

    public void autoSettingsAnpassen() {
        // AUTO-Settings koennen erst abschliessend erfolgen, wenn alle relevanten Grafer-Daten vorhanden sind
        // --> expliziter Aufruf
        this.auto_BereichsgrenzenDerAchsen();  // falls "AUTO"-Grenze --> automatisch setzen
    }
    //-------------------------------------

    public void setzeAchsen() {
    }

    protected void setzeKurven() {
    }

    public GraferV3() {
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
            this.zeichneKoordinatenAchsen(g);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e + " in zeichneKoordinatenAchsen()");
        }
        try {
            this.zeichneKurven(g);
        } catch (Exception e) {
            System.out.println(e + " in zeichneKurven()");
            e.printStackTrace();
        }
        try {
            this.zeichne(g);
        } catch (Exception e) {
            System.out.println(e + " in zeichne()");
        }
        //--------------------------
    }

    // zum Ueberschreiben
    protected void zeichne(Graphics g) {
    }

    public static double lg10(double x) {
        return (Math.log(x) / Math.log(10.0));
    }

    protected int berechne_x_PixLinear(double wert, int index) {
        return (_xAchseX[index] + (int) (sfX[index] * (wert - achseXmin[index])));
    }

    protected int berechne_x_PixLogarithmisch(double wert, int index) {
        return (_xAchseX[index] + (int) (sfX[index] * this.lg10(wert / achseXmin[index])));
    }

    protected int berechne_y_PixLinear(double wert, int index) {
        return (_yAchseY[index] - (int) (sfY[index] * (wert - achseYmin[index])));
    }

    protected int berechne_y_PixLogarithmisch(double wert, int index) {
        return (_yAchseY[index] - (int) (sfY[index] * this.lg10(wert / achseYmin[index])));
    }

    protected void zeichneKurven(Graphics g) {
        if (worksheetDaten == null) {
            return;
        }
        GeckoGraphics2D g2 = new GeckoGraphics2D((Graphics2D) g);
        for (int i1 = 0; i1 < anzahlKurven; i1++) {
            this.zeichneEinzelneKurve(g2, i1, worksheetDaten.getColumnLength());
        }
    }

    protected void zeichneKoordinatenAchsen(Graphics g) {
        GeneralPath grL = new GeneralPath();
        Graphics2D g2 = (Graphics2D) g;
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
        // x-Achsen:
        for (int i1 = 0; i1 < anzahlAchsenX; i1++) {
            this.zeichneEinzelneKoordinatenAchse_X(g2, i1);
        }
        // y-Achsen:
        for (int i1 = 0; i1 < anzahlAchsenY; i1++) {
            this.zeichneEinzelneKoordinatenAchse_Y(g2, i1);
        }
        //===============================================
        this.zeichneGrid_NormalX(g);
        this.zeichneGrid_NormalY(g);
        //===============================================
        // nachfolgend werden die X- und die Y-Achse noch einmal gezeichnet, damit sie nicht eventuell vom Grid (der eine andere Farbe haben kann)
        // ueberdeckt werden
        // die Methoden 'this.zeichneGrid_NormalX(g)' und 'this.zeichneGrid_NormalY(g)' koennen nicht vor die Schleife zum Zeichnen der Achsen
        // gestellt werden, weil in 'this.zeichneEinzelneKoordinatenAchse_X(g2,i1)' und 'this.zeichneEinzelneKoordinatenAchse_Y(g2,i1)' zuerst einmal
        // der Grid berechnet werden muss
        //
        for (int i1 = 0; i1 < anzahlAchsenX; i1++) {
            g2.setColor(farbeAchsenX[i1]);
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

    protected void zeichneEinzelneKurve(Graphics2D g2, int i1, int anzKurvenpunkteImWorksheet) {


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
        int x0Kurve = _xAchseX[indexZurKurveGehoerigeXachse[i1]];  // zugehoerige x-Achse definiert x0 der Kurve
        int y0Kurve = _yAchseY[indexZurKurveGehoerigeYachse[i1]];  // zugehoerige y-Achse definiert y0 der Kurve


        for (int i2 = 0; i2 < anzKurvenpunkteImWorksheet; i2++) {
            double x = worksheetDaten.getValue(kurve_index_worksheetKolonnen_XY[i1][0], i2);
            if (xAchseTyp[indexZurKurveGehoerigeXachse[i1]] == ACHSE_LIN) {
                xPix[i2] = (float) (x0Kurve + (sfX[indexZurKurveGehoerigeXachse[i1]] * (x - achseXmin[indexZurKurveGehoerigeXachse[i1]])));
            } else if ((xAchseTyp[indexZurKurveGehoerigeXachse[i1]] == ACHSE_LOG)) {
                xPix[i2] = (float) (x0Kurve + (sfX[indexZurKurveGehoerigeXachse[i1]] * this.lg10(x / achseXmin[indexZurKurveGehoerigeXachse[i1]])));
            }

            double y = worksheetDaten.getValue(kurve_index_worksheetKolonnen_XY[i1][1], i2);
            if (yAchseTyp[indexZurKurveGehoerigeYachse[i1]] == ACHSE_LIN) {
                yPix[i2] = (float) (y0Kurve - (sfY[indexZurKurveGehoerigeYachse[i1]] * (y - achseYmin[indexZurKurveGehoerigeYachse[i1]])));
//                if(y == 20.15) {
//                    System.out.println(y + " " + yPix[i2]);
//                }
                //System.out.println(yPix[i2]);
            } else if ((yAchseTyp[indexZurKurveGehoerigeYachse[i1]] == ACHSE_LOG)) {
                if (y <= 0) {
                    y = 1e-99;  //y=achseYmin[indexZurKurveGehoerigeYachse[i1]];
                }
                yPix[i2] = (float) (y0Kurve - (sfY[indexZurKurveGehoerigeYachse[i1]] * this.lg10(y / achseYmin[indexZurKurveGehoerigeYachse[i1]])));
            }
        }



        g2.setClip(x0Kurve + 1, y0Kurve - hoehePix[indexZurKurveGehoerigeYachse[i1]] - 1, breitePix[indexZurKurveGehoerigeYachse[i1]] + 2, hoehePix[indexZurKurveGehoerigeYachse[i1]] + 3);
        //--------------------------------
        g2.setColor(kurveFarbe[i1]);
        //
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
            System.out.println("Fehler: rhjw5z65");
        }
        
        //-----------------------
        // jetzt die Linie ziehen:
        grL.reset();
        if (kurveLinienstil[i1] != GraferV3.INVISIBLE) {            
            grL.moveTo(xPix[0], yPix[0]);
            for (int i5 = 1; i5 < anzKurvenpunkteImWorksheet; i5++) {
                if(xPix[i5] < 2000 && yPix[i5] < 2000) { // old uwe-bug!
                    grL.lineTo(xPix[i5], yPix[i5]);  
                }
            }

            g2.draw(grL);
        }
        
        
        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        //
        //-----------------------
        int dmCIRCLE = 8, hCROSS = 4, aRECT = 6, aTRIANG = 8;

        if (kurvenPunktSymbolAnzeigen[i1]) {
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

    protected void zeichneEinzelneKoordinatenAchse_X(Graphics2D g2, int i1) {
        //==================================
        if (xAchseTyp[i1] == ACHSE_LIN) {
            sfX[i1] = breitePix[i1] / (achseXmax[i1] - achseXmin[i1]);
            int anzTicks = (int) (achseXmax[i1] / xTickSpacing[i1]) - (int) (achseXmin[i1] / xTickSpacing[i1]) + 1;
            double[] wertTickX_temp = new double[anzTicks];
            int[] tickX_temp = new int[anzTicks];
            int j = 0;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = xTickSpacing[i1] * (int) (achseXmin[i1] / xTickSpacing[i1]) + i2 * xTickSpacing[i1];
                int tick = this.berechne_x_PixLinear(wert, i1);
                if ((achseXmin[i1] <= wert) && (wert <= achseXmax[i1])) {
                    wertTickX_temp[j] = wert;
                    tickX_temp[j] = tick;
                    j++;
                }
            }
            anzTicks = j;  // Korrektur der Tick-Anzahl -->
            double maxXtic = -100;
            int maxIndex = -1;
            for (int i3 = 0; i3 < _yAchseX.length; i3++) {
                maxXtic = Math.max(_yAchseX[i3] + posXtickLabels[i3], maxXtic);
                if (maxXtic == _yAchseX[i3] + posXtickLabels[i3]) {
                    maxIndex = i3;
                }

            }
            zeigeLabelsXmaj[maxIndex] = true;

            //
            // hier wird geprueft, ob ueberhaupt Ticks eingetragen werden sollen -->
            if (j != 0) {
                //-----------------------------------------------------------------------------------------------------------------------
                wertTickX[i1] = new double[anzTicks];
                System.arraycopy(wertTickX_temp, 0, wertTickX[i1], 0, anzTicks);
                tickX[i1] = new int[anzTicks];
                System.arraycopy(tickX_temp, 0, tickX[i1], 0, anzTicks);
                //
                int d = xTickLaenge[i1];
                if (zeigeXticksUnten[i1]) {
                    d = -d;
                }


                for (int i2 = 0; i2 < anzTicks; i2++) {
                    g2.setColor(farbeAchsenX[i1]);
                    g2.drawLine(tickX[i1][i2], _yAchseX[i1], tickX[i1][i2], _yAchseX[i1] - d);  // Ticks auf der x-Achse


                    if (zeigeLabelsXmaj[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatENG(wertTickX[i1][i2], digitsX);  //nf.format(wertTickX[i1][i2]);
                        g2.drawString(label, -3 + tickX[i1][i2], _yAchseX[i1] + posXtickLabels[i1]);  // Labels auf der x-Achse
                    }
                }
                //
                double xTickSpacingMinor = xTickSpacing[i1] / xAnzTicksMinor[i1];  // Wert zwischen zwei Minor-Ticks auf der x-Achse
                int xMinorTicksAnzahl = (int) ((achseXmax[i1] - achseXmin[i1]) / xTickSpacingMinor) + 2;
                double[] wertTickMinorX_temp = new double[xMinorTicksAnzahl];
                int[] tickMinorX_temp = new int[xMinorTicksAnzahl];
                double wertMinor = wertTickX[i1][0] - xTickSpacing[i1];
                j = 0;
                while (wertMinor < wertTickX[i1][0] - 1.5 * xTickSpacingMinor) {
                    wertMinor += xTickSpacingMinor;
                    if (wertMinor >= achseXmin[i1]) {
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                for (int i2 = 0; i2 < wertTickX[i1].length - 1; i2++) {
                    wertMinor = wertTickX[i1][i2] + xTickSpacingMinor;
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                    while (wertMinor < wertTickX[i1][i2 + 1] - 1.5 * xTickSpacingMinor) {
                        wertMinor += xTickSpacingMinor;
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                wertMinor = wertTickX[i1][wertTickX[i1].length - 1];
                while (wertMinor < achseXmax[i1] - 0.99 * xTickSpacingMinor) {
                    wertMinor += xTickSpacingMinor;
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                }
                xMinorTicksAnzahl = j;  // Korrektur der Minor-Tick Anzahl
                wertTickXminor[i1] = new double[xMinorTicksAnzahl];
                tickXminor[i1] = new int[xMinorTicksAnzahl];
                System.arraycopy(wertTickMinorX_temp, 0, wertTickXminor[i1], 0, xMinorTicksAnzahl);
                d = xTickLaengeMinor[i1];
                if (zeigeXticksUnten[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < xMinorTicksAnzahl; i2++) {
                    tickXminor[i1][i2] = this.berechne_x_PixLinear(wertTickXminor[i1][i2], i1);
                    g2.drawLine(tickXminor[i1][i2], _yAchseX[i1], tickXminor[i1][i2], _yAchseX[i1] - d);  // Minor-Ticks auf der x-Achse
                    if (zeigeLabelsXmin[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatENG(wertTickXminor[i1][i2], digitsX);  // nf.format(wertTickXminor[i1][i2]);
                        g2.drawString(label, tickXminor[i1][i2], _yAchseX[i1] + posXtickLabels[i1]);  // Minor-Labels auf der x-Achse
                    }
                    //-----------------------------------------------------------------------------------------------------------------------
                }
            }
            //==================================
        } else if (xAchseTyp[i1] == ACHSE_LOG) {
            // zwingend -->  xTickSpacing[i1]=AUTO  weil nur sinnvollerweise die Zehner-Dekaden mit Ticks versehen werden
            sfX[i1] = breitePix[i1] / this.lg10(achseXmax[i1] / achseXmin[i1]);
            int anzTicks = (int) Math.round(this.lg10(achseXmax[i1] / achseXmin[i1])) + 3;
            double[] wertTickX_temp = new double[anzTicks];
            int[] tickX_temp = new int[anzTicks];
            int j = 0;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = Math.pow(10, ((int) this.lg10(achseXmin[i1]) - 1 + i2));
                int tick = this.berechne_x_PixLogarithmisch(wert, i1);
                if ((achseXmin[i1] <= wert) && (wert <= achseXmax[i1])) {
                    wertTickX_temp[j] = wert;
                    tickX_temp[j] = tick;
                    j++;
                }
            }
            // hier wird abgecheckt, ob ueberhaupt Ticks eingetragen werden sollen -->
            if (j != 0) {
                //-----------------------------------------------------------------------------------------------------------------------
                anzTicks = j;  // Korrektur der Tick-Anzahl -->
                wertTickX[i1] = new double[anzTicks];
                System.arraycopy(wertTickX_temp, 0, wertTickX[i1], 0, anzTicks);
                tickX[i1] = new int[anzTicks];
                System.arraycopy(tickX_temp, 0, tickX[i1], 0, anzTicks);
                //
                int d = xTickLaenge[i1];
                if (zeigeXticksUnten[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < anzTicks; i2++) {
                    g2.setColor(farbeAchsenX[i1]);
                    g2.drawLine(tickX[i1][i2], _yAchseX[i1], tickX[i1][i2], _yAchseX[i1] - d);  // Ticks auf der x-Achse
                    if (zeigeLabelsXmaj[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatT(wertTickX[i1][i2], "#.E0");  // tcf.formatENG(wertTickX[i1][i2],digitsX);
                        g2.drawString(label, tickX[i1][i2], _yAchseX[i1] + posXtickLabels[i1]);  // Labels auf der x-Achse
                    }
                }
                //
                int xMinorTicksAnzahl = 10 * (anzTicks + 1);
                double[] wertTickMinorX_temp = new double[xMinorTicksAnzahl];
                //int[] tickMinorX_temp= new int[xMinorTicksAnzahl];
                double wertMinor = wertTickX[i1][0] / ((int) (wertTickX[i1][0] / achseXmin[i1]));
                wertTickMinorX_temp[0] = wertMinor;
                j = 1;
                while (wertMinor < 0.85 * wertTickX[i1][0]) {
                    wertMinor += (0.1 * wertTickX[i1][0]);
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                }
                for (int i2 = 0; i2 < wertTickX[i1].length - 1; i2++) {
                    wertMinor = wertTickX[i1][i2] + (0.1 * wertTickX[i1][i2 + 1]);
                    wertTickMinorX_temp[j] = wertMinor;
                    j++;
                    while (wertMinor < 0.85 * wertTickX[i1][i2 + 1]) {
                        wertMinor += (0.1 * wertTickX[i1][i2 + 1]);
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                wertMinor = wertTickX[i1][wertTickX[i1].length - 1];
                while (wertMinor < achseXmax[i1]) {
                    wertMinor += wertTickX[i1][wertTickX[i1].length - 1];
                    if (wertMinor <= achseXmax[i1]) {
                        wertTickMinorX_temp[j] = wertMinor;
                        j++;
                    }
                }
                xMinorTicksAnzahl = j;  // Korrektur der Minor-Tick Anzahl
                wertTickXminor[i1] = new double[xMinorTicksAnzahl];
                tickXminor[i1] = new int[xMinorTicksAnzahl];
                System.arraycopy(wertTickMinorX_temp, 0, wertTickXminor[i1], 0, xMinorTicksAnzahl);
                d = xTickLaengeMinor[i1];
                if (zeigeXticksUnten[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < xMinorTicksAnzahl; i2++) {
                    tickXminor[i1][i2] = this.berechne_x_PixLogarithmisch(wertTickXminor[i1][i2], i1);
                    g2.drawLine(tickXminor[i1][i2], _yAchseX[i1], tickXminor[i1][i2], _yAchseX[i1] - d);  // Minor-Ticks auf der x-Achse
                    if (zeigeLabelsXmin[i1]) {
                        g2.setFont(foTickLabelX[i1]);
                        String label = tcf.formatT(wertTickXminor[i1][i2], "#.E0");  // tcf.formatENG(wertTickXminor[i1][i2],digitsX);
                        g2.drawString(label, tickXminor[i1][i2], _yAchseX[i1] + posXtickLabels[i1]);  // Minor-Labels auf der x-Achse
                    }
                }
                //-----------------------------------------------------------------------------------------------------------------------
            }
        }

    }

    protected void zeichneEinzelneKoordinatenAchse_Y(Graphics2D g2, int i1) {

        if (yAchseTyp[i1] == ACHSE_LIN) {
            sfY[i1] = hoehePix[i1] / (achseYmax[i1] - achseYmin[i1]);
            int anzTicks = (int) (achseYmax[i1] / yTickSpacing[i1]) - (int) (achseYmin[i1] / yTickSpacing[i1]) + 1;
            anzTicks = Math.max(anzTicks, 2);
            double[] wertTickY_temp = new double[anzTicks];
            int[] tickY_temp = new int[anzTicks];
            int j = 0;
            
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = yTickSpacing[i1] * (int) (achseYmin[i1] / yTickSpacing[i1]) + i2 * yTickSpacing[i1];
                int tick = this.berechne_y_PixLinear(wert, i1);                
                if ((achseYmin[i1] <= wert) && (wert <= achseYmax[i1])) {
                    wertTickY_temp[j] = wert;                    
                    tickY_temp[j] = tick;
                    j++;
                }
            }
            anzTicks = j;  // Korrektur der Tick-Anzahl -->            
            wertTickY[i1] = new double[anzTicks];            
            System.arraycopy(wertTickY_temp, 0, wertTickY[i1], 0, anzTicks);
            tickY[i1] = new int[anzTicks];
            System.arraycopy(tickY_temp, 0, tickY[i1], 0, anzTicks);
            //
            int d = yTickLaenge[i1];
            if (zeigeYticksLinks[i1]) {
                d = -d;
            }
            for (int i2 = 0; i2 < anzTicks; i2++) {
                g2.setColor(farbeAchsenY[i1]);
                g2.drawLine(_xAchseY[i1], tickY[i1][i2], _xAchseY[i1] + d, tickY[i1][i2]);  // Ticks auf der y-Achse
                if (zeigeLabelsYmaj[i1]) {
                    g2.setFont(foTickLabelY[i1]);
                    String label = tcf.formatENG(wertTickY[i1][i2], digitsY);  // nf.format(wertTickY[i1][i2]);
                    //if ((label.equals("0"))&&(wertTickY[i1][i2]!=0)) label= ""+wertTickY[i1][i2];
                    g2.drawString(label, _xAchseY[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickY[i1][i2] + foTickLabelY[i1].getSize() / 2 - 1);  // Labels auf der y-Achse
                }
            }
            //
            double yTickSpacingMinor = yTickSpacing[i1] / yAnzTicksMinor[i1];  // Wert zwischen zwei Minor-Ticks auf der y-Achse
            int yMinorTicksAnzahl = (int) ((achseYmax[i1] - achseYmin[i1]) / yTickSpacingMinor) + 2;
            double[] wertTickMinorY_temp = new double[yMinorTicksAnzahl];
            int[] tickMinorY_temp = new int[yMinorTicksAnzahl];
            double wertMinor = wertTickY[i1][0] - yTickSpacing[i1];
            j = 0;
            while (wertMinor < wertTickY[i1][0] - 1.5 * yTickSpacingMinor) {
                wertMinor += yTickSpacingMinor;
                if (wertMinor >= achseYmin[i1]) {
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                }
            }
            for (int i2 = 0; i2 < wertTickY[i1].length - 1; i2++) {
                wertMinor = wertTickY[i1][i2] + yTickSpacingMinor;
                wertTickMinorY_temp[j] = wertMinor;
                j++;
                while (wertMinor < wertTickY[i1][i2 + 1] - 1.5 * yTickSpacingMinor) {
                    wertMinor += yTickSpacingMinor;
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                }
            }
            wertMinor = wertTickY[i1][wertTickY[i1].length - 1];
            while (wertMinor < achseYmax[i1] - 0.99 * yTickSpacingMinor) {
                wertMinor += yTickSpacingMinor;
                wertTickMinorY_temp[j] = wertMinor;
                j++;
            }
            yMinorTicksAnzahl = j;  // Korrektur der Minor-Tick Anzahl
            wertTickYminor[i1] = new double[yMinorTicksAnzahl];
            tickYminor[i1] = new int[yMinorTicksAnzahl];
            System.arraycopy(wertTickMinorY_temp, 0, wertTickYminor[i1], 0, yMinorTicksAnzahl);
            d = yTickLaengeMinor[i1];
            if (zeigeYticksLinks[i1]) {
                d = -d;
            }
            for (int i2 = 0; i2 < yMinorTicksAnzahl; i2++) {
                tickYminor[i1][i2] = this.berechne_y_PixLinear(wertTickYminor[i1][i2], i1);
                g2.drawLine(_xAchseY[i1], tickYminor[i1][i2], _xAchseY[i1] + d, tickYminor[i1][i2]);  // Minor-Ticks auf der y-Achse
                if (zeigeLabelsYmin[i1]) {
                    g2.setFont(foTickLabelY[i1]);
                    String label = tcf.formatENG(wertTickYminor[i1][i2], digitsY);  // nf.format(wertTickYminor[i1][i2]);
                    g2.drawString(label, _xAchseY[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickYminor[i1][i2] + foTickLabelY[i1].getSize() / 2);  // Minor-Labels auf der y-Achse
                }
            }
            //==================================
        } else if (yAchseTyp[i1] == ACHSE_LOG) {
            if (achseYmin[i1] <= 0) {
                achseYmin[i1] = achseYmax[i1] / 1e4;
            }
            // zwingend -->  yTickSpacing[i1]=AUTO  weil nur sinnvollerweise die Zehner-Dekaden mit Ticks versehen werden
            sfY[i1] = hoehePix[i1] / this.lg10(achseYmax[i1] / achseYmin[i1]);
            int anzTicks = (int) (this.lg10(achseYmax[i1] / achseYmin[i1])) + 3;
            double[] wertTickY_temp = new double[anzTicks];
            int[] tickY_temp = new int[anzTicks];
            int j = 0;
            for (int i2 = 0; i2 < anzTicks; i2++) {
                double wert = Math.pow(10, ((int) this.lg10(achseYmin[i1]) - 1 + i2));
                int tick = this.berechne_y_PixLogarithmisch(wert, i1);
                if ((achseYmin[i1] <= wert) && (wert <= achseYmax[i1])) {
                    wertTickY_temp[j] = wert;
                    tickY_temp[j] = tick;
                    j++;
                }
            }
            // hier wird abgecheckt, ob ueberhaupt Ticks eingetragen werden sollen -->
            if (j != 0) {
                //-----------------------------------------------------------------------------------------------------------------------
                anzTicks = j;  // Korrektur der Tick-Anzahl -->
                wertTickY[i1] = new double[anzTicks];
                System.arraycopy(wertTickY_temp, 0, wertTickY[i1], 0, anzTicks);
                tickY[i1] = new int[anzTicks];
                System.arraycopy(tickY_temp, 0, tickY[i1], 0, anzTicks);
                //
                int d = yTickLaenge[i1];
                if (zeigeYticksLinks[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < anzTicks; i2++) {
                    g2.setColor(farbeAchsenY[i1]);
                    g2.drawLine(_xAchseY[i1], tickY[i1][i2], _xAchseY[i1] + d, tickY[i1][i2]);  // Ticks auf der y-Achse
                    if (zeigeLabelsYmaj[i1]) {
                        g2.setFont(foTickLabelY[i1]);
                        String label = tcf.formatT(wertTickY[i1][i2], "#.E0");  // tcf.formatENG(wertTickY[i1][i2],digitsY);
                        g2.drawString(label, _xAchseY[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickY[i1][i2] + foTickLabelY[i1].getSize() / 2);  // Labels auf der y-Achse
                    }
                }
                //
                int yMinorTicksAnzahl = 10 * (anzTicks + 1);
                double[] wertTickMinorY_temp = new double[yMinorTicksAnzahl];
                int[] tickMinorY_temp = new int[yMinorTicksAnzahl];
                double wertMinor = wertTickY[i1][0] / ((int) (wertTickY[i1][0] / achseYmin[i1]));
                wertTickMinorY_temp[0] = wertMinor;
                j = 1;
                while (wertMinor < 0.85 * wertTickY[i1][0]) {
                    wertMinor += (0.1 * wertTickY[i1][0]);
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                }
                for (int i2 = 0; i2 < wertTickY[i1].length - 1; i2++) {
                    wertMinor = wertTickY[i1][i2] + (0.1 * wertTickY[i1][i2 + 1]);
                    wertTickMinorY_temp[j] = wertMinor;
                    j++;
                    while (wertMinor < 0.85 * wertTickY[i1][i2 + 1]) {
                        wertMinor += (0.1 * wertTickY[i1][i2 + 1]);
                        wertTickMinorY_temp[j] = wertMinor;
                        j++;
                    }
                }
                wertMinor = wertTickY[i1][wertTickY[i1].length - 1];
                while (wertMinor < achseYmax[i1]) {
                    wertMinor += wertTickY[i1][wertTickY[i1].length - 1];
                    if (wertMinor <= achseYmax[i1]) {
                        wertTickMinorY_temp[j] = wertMinor;
                        j++;
                    }
                }
                yMinorTicksAnzahl = j;  // Korrektur der Minor-Tick Anzahl
                wertTickYminor[i1] = new double[yMinorTicksAnzahl];
                tickYminor[i1] = new int[yMinorTicksAnzahl];
                System.arraycopy(wertTickMinorY_temp, 0, wertTickYminor[i1], 0, yMinorTicksAnzahl);
                d = yTickLaengeMinor[i1];
                if (zeigeYticksLinks[i1]) {
                    d = -d;
                }
                for (int i2 = 0; i2 < yMinorTicksAnzahl; i2++) {
                    tickYminor[i1][i2] = this.berechne_y_PixLogarithmisch(wertTickYminor[i1][i2], i1);
                    g2.drawLine(_xAchseY[i1], tickYminor[i1][i2], _xAchseY[i1] + d, tickYminor[i1][i2]);  // Minor-Ticks auf der y-Achse
                    if (zeigeLabelsYmin[i1]) {
                        g2.setFont(foTickLabelY[i1]);
                        String label = tcf.formatT(wertTickYminor[i1][i2], "#.E0");  // tcf.formatENG(wertTickYminor[i1][i2],digitsY);
                        g2.drawString(label, _xAchseY[i1] - posYtickLabels[i1] - (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth(), tickYminor[i1][i2] + foTickLabelY[i1].getSize() / 2);  // Minor-Labels auf der y-Achse
                    }
                }
                //-----------------------------------------------------------------------------------------------------------------------
            }
        }
        //==================================
        /*
         * g2.setColor(farbeAchsenY[i1]); if (linienStilAchsenY[i1]==SOLID_PLAIN) { g2.setStroke(str_SOLID_PLAIN); } else if
         * (linienStilAchsenY[i1]==INVISIBLE) { // nix machen, weil unsichtbar } else if (linienStilAchsenY[i1]==SOLID_FAT_1) {
         * g2.setStroke(str_SOLID_FAT_1); } else if (linienStilAchsenY[i1]==SOLID_FAT_2) { g2.setStroke(str_SOLID_FAT_2); } else
         * if (linienStilAchsenY[i1]==DOTTED_PLAIN) { g2.setStroke(str_DOTTED_PLAIN); } else if
         * (linienStilAchsenY[i1]==DOTTED_FAT) { g2.setStroke(str_DOTTED_FAT); } else System.out.println("Fehler: hhqqt5");
         * //----------------------- // jetzt die Linie ziehen: grL.reset(); grL.moveTo(xAchseY[i1], yAchseY[i1]);
         * grL.lineTo(xAchseY[i1], yAchseY[i1]-hoehePix[i1]); if (linienStilAchsenY[i1]!=GraferV3.INVISIBLE) { g2.draw(grL);
         * g2.drawString(yAchseBeschriftung[i1], xAchseY[i1]-posYtickLabels[i1], yAchseY[i1]-hoehePix[i1]/2); }
         * g2.setStroke(str_SOLID_PLAIN); // wieder auf 'default' setzen
         */
        //==================================
    }

    protected void zeichneGrid_NormalX(Graphics g) {
        GeneralPath grL = new GeneralPath();
        Graphics2D g2 = (Graphics2D) g;
        //
        if ((gridNormalX_zugeordneteXAchse == null) || (gridNormalX_zugeordneteYAchse == null)) {
            return;  // um Fehler beim ersten Aufruf zu vermeiden
        }        //------------------------------------------------------------
        // Grid-Linien parallel der y-Achse (dh. normal auf die x-Achse)  -->
        for (int i1 = 0; i1 < gridNormalX_zugeordneteXAchse.length; i1++) {
            int indexAchseX = gridNormalX_zugeordneteXAchse[i1];
            int indexAchseY = gridNormalX_zugeordneteYAchse[i1];
            if ((indexAchseX != -1) && (indexAchseY != -1)) {
                // Minor-Grids -->
                for (int i3 = 0; i3 < showGridNormalXminor.length; i3++) {
                    if ((showGridNormalXminor[i3][0] == indexAchseX) && ((showGridNormalXminor[i3][1] == indexAchseY)) && (tickXminor[indexAchseX] != null)) {
                        for (int i2 = 0; i2 < tickXminor[indexAchseX].length; i2++) {
                            g.setColor(farbeGridNormalXminor[i1]);
                            if (linStilGridNormalXminor[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (linStilGridNormalXminor[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
                            } else if (linStilGridNormalXminor[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (linStilGridNormalXminor[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (linStilGridNormalXminor[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (linStilGridNormalXminor[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: p05kgh9");
                            }
                            //-----------------------
                            // jetzt die Linie ziehen:
                            grL.reset();
                            grL.moveTo(tickXminor[indexAchseX][i2], _yAchseY[indexAchseY]);
                            grL.lineTo(tickXminor[indexAchseX][i2], _yAchseY[indexAchseY] - hoehePix[indexAchseY]);
                            if (linStilGridNormalXminor[i1] != GraferV3.INVISIBLE) {
                                g2.draw(grL);
                            }
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
                // Major-Ticks -->
                for (int i3 = 0; i3 < showGridNormalXmajor.length; i3++) {
                    if ((showGridNormalXmajor[i3][0] == indexAchseX) && ((showGridNormalXmajor[i3][1] == indexAchseY)) && (tickX[indexAchseX] != null)) {
                        for (int i2 = 0; i2 < tickX[indexAchseX].length; i2++) {
                            g.setColor(farbeGridNormalX[i1]);
                            if (linStilGridNormalX[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (linStilGridNormalX[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
                            } else if (linStilGridNormalX[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (linStilGridNormalX[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (linStilGridNormalX[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (linStilGridNormalX[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: hrtrjww5j");
                            }
                            //-----------------------
                            // jetzt die Linie ziehen:
                            grL.reset();
                            grL.moveTo(tickX[indexAchseX][i2], _yAchseY[indexAchseY]);
                            grL.lineTo(tickX[indexAchseX][i2], _yAchseY[indexAchseY] - hoehePix[indexAchseY]);
                            if (linStilGridNormalX[i1] != GraferV3.INVISIBLE) {
                                g2.draw(grL);
                            }
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
            }
        }
        //------------------------------------------------------------
    }

    protected void zeichneGrid_NormalY(Graphics g) {
        GeneralPath grL = new GeneralPath();
        Graphics2D g2 = (Graphics2D) g;
        //
        if ((gridNormalY_zugeordneteXAchse == null) || (gridNormalY_zugeordneteYAchse == null)) {
            return;  // um Fehler beim ersten Aufruf zu vermeiden
        }        //------------------------------------------------------------
        // Grid-Linien parallel der x-Achse (dh. normal auf die y-Achse)  -->
        for (int i1 = 0; i1 < gridNormalY_zugeordneteXAchse.length; i1++) {
            int indexAchseX = gridNormalY_zugeordneteXAchse[i1];
            int indexAchseY = gridNormalY_zugeordneteYAchse[i1];
            if ((indexAchseX != -1) && (indexAchseY != -1)) {
                // Minor-Grids -->
                for (int i3 = 0; i3 < showGridNormalYminor.length; i3++) {
                    if ((showGridNormalYminor[i3][0] == indexAchseX) && ((showGridNormalYminor[i3][1] == indexAchseY)) && (tickYminor[indexAchseY] != null)) {
                        for (int i2 = 0; i2 < tickYminor[indexAchseY].length; i2++) {
                            g.setColor(farbeGridNormalYminor[i1]);
                            if (linStilGridNormalYminor[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (linStilGridNormalYminor[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
                            } else if (linStilGridNormalYminor[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (linStilGridNormalYminor[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (linStilGridNormalYminor[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (linStilGridNormalYminor[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: hezwhwh6");
                            }
                            //-----------------------
                            // jetzt die Linie ziehen:
                            grL.reset();
                            grL.moveTo(_xAchseX[indexAchseX], tickYminor[indexAchseY][i2]);
                            grL.lineTo(_xAchseX[indexAchseX] + breitePix[indexAchseX], tickYminor[indexAchseY][i2]);
                            if (linStilGridNormalYminor[i1] != GraferV3.INVISIBLE) {
                                g2.draw(grL);
                            }
                            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
                            //-----------------------
                        }
                    }
                }
                // Major-Ticks -->
                for (int i3 = 0; i3 < showGridNormalYmajor.length; i3++) {
                    if ((showGridNormalYmajor[i3][0] == indexAchseX) && ((showGridNormalYmajor[i3][1] == indexAchseY)) && (tickY[indexAchseY] != null)) {
                        for (int i2 = 0; i2 < tickY[indexAchseY].length; i2++) {
                            g.setColor(farbeGridNormalY[i1]);
                            if (linStilGridNormalY[i1] == SOLID_PLAIN) {
                                g2.setStroke(str_SOLID_PLAIN);
                            } else if (linStilGridNormalY[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
                            } else if (linStilGridNormalY[i1] == SOLID_FAT_1) {
                                g2.setStroke(str_SOLID_FAT_1);
                            } else if (linStilGridNormalY[i1] == SOLID_FAT_2) {
                                g2.setStroke(str_SOLID_FAT_2);
                            } else if (linStilGridNormalY[i1] == DOTTED_PLAIN) {
                                g2.setStroke(str_DOTTED_PLAIN);
                            } else if (linStilGridNormalY[i1] == DOTTED_FAT) {
                                g2.setStroke(str_DOTTED_FAT);
                            } else {
                                System.out.println("Fehler: gjigrije");
                            }
                            //-----------------------
                            // jetzt die Linie ziehen:
                            grL.reset();
                            grL.moveTo(_xAchseX[indexAchseX], tickY[indexAchseY][i2]);
                            grL.lineTo(_xAchseX[indexAchseX] + breitePix[indexAchseX], tickY[indexAchseY][i2]);
                            if (linStilGridNormalY[i1] != GraferV3.INVISIBLE) {
                                g2.draw(grL);
                            }
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
        // wenn nur Achsen gezeichnet werden, aber (noch?) keine Kurvendaten vorhanden sind
        if (worksheetDaten == null) {
            return;
        }
        //
        //----------------------
        // zuerst alle Minima und Maxima der Kurvendaten suchen:
        int laenge = worksheetDaten.getRowLength();
        double[] minEmpfehlungLIN = new double[laenge];  // empfohlene Achsen-Min-Werte bei AUTO / ACHSE_LIN
        double[] maxEmpfehlungLIN = new double[laenge];  // empfohlene Achsen-Max-Werte bei AUTO / ACHSE_LIN
        double[] minEmpfehlungLOG = new double[laenge];  // empfohlene Achsen-Min-Werte bei AUTO / ACHSE_LOG
        double[] maxEmpfehlungLOG = new double[laenge];  // empfohlene Achsen-Max-Werte bei AUTO / ACHSE_LOG
        double[] min = new double[laenge];  // Min- u. Max-Werte der einzelnen Kolonnen von 'worksheetDaten'
        double[] max = new double[laenge];
        //
        for (int i1 = 0; i1 < laenge; i1++) {
            min[i1] = 1e99;
            max[i1] = -1e99;  // initial
            for (int i2 = 0; i2 < worksheetDaten.getColumnLength(); i2++) {
                if (worksheetDaten.getValue(i1, i2) < min[i1]) {
                    min[i1] = worksheetDaten.getValue(i1, i2);
                }
                if (worksheetDaten.getValue(i1, i2) > max[i1]) {
                    max[i1] = worksheetDaten.getValue(i1, i2);
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
        for (int achsenNr = 0; achsenNr < achseXmin.length; achsenNr++) {
            if (autoAchseXmin[achsenNr]) {
                // jetzt den Achsen, die von Kurven besetzt sind, die entsprechenden Minima und Maxima zuteilen:
                achseXmin[achsenNr] = 1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < anzahlKurven; nrKurve++) {
                    if (achsenNr == indexZurKurveGehoerigeXachse[nrKurve]) {
                        if ((xAchseTyp[achsenNr] == ACHSE_LIN)
                                && (achseXmin[achsenNr] > minEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][0]])) {
                            achseXmin[achsenNr] = minEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][0]];
                        } else if ((xAchseTyp[achsenNr] == ACHSE_LOG)
                                && (achseXmin[achsenNr] > minEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][0]])) {
                            achseXmin[achsenNr] = minEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][0]];
                        }
                    }
                }
            }
        }
        for (int achsenNr = 0; achsenNr < achseXmax.length; achsenNr++) {
            if (autoAchseXmax[achsenNr]) {
                // jetzt den Achsen, die von Kurven besetzt sind, die entsprechenden Minima und Maxima zuteilen:
                achseXmax[achsenNr] = -1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < anzahlKurven; nrKurve++) {
                    if (achsenNr == indexZurKurveGehoerigeXachse[nrKurve]) {
                        if ((xAchseTyp[achsenNr] == ACHSE_LIN)
                                && (achseXmax[achsenNr] < maxEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][0]])) {
                            achseXmax[achsenNr] = maxEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][0]];
                        } else if ((xAchseTyp[achsenNr] == ACHSE_LOG)
                                && (achseXmax[achsenNr] < maxEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][0]])) {
                            achseXmax[achsenNr] = maxEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][0]];
                        }
                    }
                }
            }
        }
        for (int achsenNr = 0; achsenNr < achseYmin.length; achsenNr++) {
            if (autoAchseYmin[achsenNr]) {
                // jetzt den Achsen, die von Kurven besetzt sind, die entsprechenden Minima und Maxima zuteilen:
                achseYmin[achsenNr] = 1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < anzahlKurven; nrKurve++) {
                    if (achsenNr == indexZurKurveGehoerigeYachse[nrKurve]) {
                        if ((yAchseTyp[achsenNr] == ACHSE_LIN)
                                && (achseYmin[achsenNr] > minEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][1]])) {
                            achseYmin[achsenNr] = minEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][1]];
                        } else if ((yAchseTyp[achsenNr] == ACHSE_LOG)
                                && (achseYmin[achsenNr] > minEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][1]])) {
                            achseYmin[achsenNr] = minEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][1]];
                        }
                    }
                }
            }
        }
        for (int achsenNr = 0; achsenNr < achseYmax.length; achsenNr++) {
            if (autoAchseYmax[achsenNr]) {
                // jetzt den Achsen, die von Kurven besetzt sind, die entsprechenden Minima und Maxima zuteilen:
                achseYmax[achsenNr] = -1e99;  // default, falls keine Kurve zugeordnet ist
                for (int nrKurve = 0; nrKurve < anzahlKurven; nrKurve++) {
                    if (achsenNr == indexZurKurveGehoerigeYachse[nrKurve]) {
                        if ((yAchseTyp[achsenNr] == ACHSE_LIN)
                                && (achseYmax[achsenNr] < maxEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][1]])) {
                            achseYmax[achsenNr] = maxEmpfehlungLIN[kurve_index_worksheetKolonnen_XY[nrKurve][1]];
                        } else if ((yAchseTyp[achsenNr] == ACHSE_LOG)
                                && (achseYmax[achsenNr] < maxEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][1]])) {
                            achseYmax[achsenNr] = maxEmpfehlungLOG[kurve_index_worksheetKolonnen_XY[nrKurve][1]];
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
        // ACHTUNG: LIN- und LOG-Skalierung wird unterschiedliche behandelt!!
        double z1empfLIN, z2empfLIN, z1empfLOG, z2empfLOG;
        double schrittEmpfLIN = 1.0, schrittEmpfLOG = 1.0;
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
                    if (unten[i1] == alt) {
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
                    if (oben[i1] == alt) {
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
            case GraferV3.BLACK:
                return Color.black;
            case GraferV3.RED:
                return Color.red;
            case GraferV3.GREEN:
                return Color.green;
            case GraferV3.BLUE:
                return Color.blue;
            case GraferV3.DARKGRAY:
                return Color.darkGray;
            case GraferV3.GRAY:
                return Color.gray;
            case GraferV3.LIGTHGRAY:
                return Color.lightGray;
            case GraferV3.WHITE:
                return Color.white;
            case GraferV3.MAGENTA:
                return Color.magenta;
            case GraferV3.CYAN:
                return Color.cyan;
            case GraferV3.ORANGE:
                return Color.orange;
            case GraferV3.YELLOW:
                return Color.yellow;
            case GraferV3.DARKGREEN:
                return Color.decode("0x006400");
            default:
                System.out.println("Fehler: dtcjjztdm " + selector);
                return Color.black;
        }
    }

    public static int getIndexForColorSelector(String fx) {
        for (int i1 = 0; i1 < GraferV3.FARBEN.length; i1++) {
            if (GraferV3.FARBEN[i1].equals(fx)) {
                return i1;
            }
        }
        return -1;
    }

    public static Color getSelectedColor(String col) {
        if (col.equals(GraferV3.FARBEN[ 0])) {
            return Color.black;
        }
        if (col.equals(GraferV3.FARBEN[ 1])) {
            return Color.red;
        }
        if (col.equals(GraferV3.FARBEN[ 2])) {
            return Color.green;
        }
        if (col.equals(GraferV3.FARBEN[ 3])) {
            return Color.blue;
        }
        if (col.equals(GraferV3.FARBEN[ 4])) {
            return Color.darkGray;
        }
        if (col.equals(GraferV3.FARBEN[ 5])) {
            return Color.gray;
        }
        if (col.equals(GraferV3.FARBEN[ 6])) {
            return Color.lightGray;
        }
        if (col.equals(GraferV3.FARBEN[ 7])) {
            return Color.white;
        }
        if (col.equals(GraferV3.FARBEN[ 8])) {
            return Color.magenta;
        }
        if (col.equals(GraferV3.FARBEN[ 9])) {
            return Color.cyan;
        }
        if (col.equals(GraferV3.FARBEN[10])) {
            return Color.orange;
        }
        if (col.equals(GraferV3.FARBEN[11])) {
            return Color.yellow;
        }
        if (col.equals(GraferV3.FARBEN[12])) {
            return Color.decode("0x006400");
        }
        return null;
    }
}
