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
package ch.technokrat.gecko.geckocircuits.newscope;

/**
 * this class is probably the biggest bullshit you have ever seen! This was from Uwe's old Scope implementation, for
 * compatibility reasons, it is still here. It should be refactored or removed, soon!
 *
 */
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScopeSettings implements Serializable {

    public static final int ANZ_DIAGRAM_MAX = 12;
    private int anzDiagram;  // Anzahl der Diagramme
    private List<String> nameDiagram;  // Bezeichnungen der Diagramme
    private List<Double> ySpacingDiagram;  // wieviel 'y-Anteil' hat das jeweilige Diagramm
    private int[] diagramTyp;  // ist das jeweilige Diagramm ein ZV-Typ oder ein Signal-Typ?
    //
    private boolean[] autoScaleX, autoScaleY;  // sollen die Achsenbegrenzungen automatisch an die Worksheetdaten angepasst werden?
    private List<Double> userScaleXMin, userScaleXMax, userScaleYMin, userScaleYMax;
    private List<String> signalNamen = new ArrayList<String>();
    private int[] xAchsenTyp, yAchsenTyp;  // Linear oder logarithmisch?
    private int[] xAchseFarbe, yAchseFarbe;
    private int[] xAchseStil, yAchseStil;
    private List<String> xAchseBeschriftung, yAchseBeschriftung;
    //
    private int[] farbeGridNormalX, farbeGridNormalXminor, farbeGridNormalY, farbeGridNormalYminor;
    private int[] linStilGridNormalX, linStilGridNormalXminor, linStilGridNormalY, linStilGridNormalYminor;
    private boolean[] xShowGridMaj, xShowGridMin, yShowGridMaj, yShowGridMin;
    //
    private int[] xAnzTicksMinor, yAnzTicksMinor;
    private int[] xTickLaenge, xTickLaengeMinor, yTickLaenge, yTickLaengeMinor;
    //
    private boolean[] zeigeLabelsXmaj, zeigeLabelsXmin, zeigeLabelsYmaj, zeigeLabelsYmin;
    private boolean[] ORIGjcbXShowGridMaj, ORIGjcbXShowGridMin;
    private boolean[] ORIGjcbYShowGridMaj, ORIGjcbYShowGridMin;
    private int[] ORIGjcmXlinCol, ORIGjcmYlinCol;
    private int[] ORIGjcmXlinStyl, ORIGjcmYlinStyl;
    private int[] ORIGjtfXtickLengthMaj, ORIGjtfXtickLengthMin;
    private int[] ORIGjtfYtickLengthMaj, ORIGjtfYtickLengthMin;
    private boolean[] ORIGjcbXShowLabelMaj, ORIGjcbXShowLabelMin;
    private boolean[] ORIGjcbYShowLabelMaj, ORIGjcbYShowLabelMin;
    private int[][] matrixZuordnungKurveDiagram;
    private int[][] indexWsXY;  // Zuordnung Worksheetdaten - Kurven
    private int[][] crvAchsenTyp;  // wird ueber SET-Methode aktualisiert, damit die Matrix 'matrixZuordnungKurveDiagram' nicht vergessen wird!
    private int[][] crvLineStyle, crvLineColor;
    private boolean[][] crvSymbShow;
    private int[][] crvSymbFrequ;
    private int[][] crvSymbShape, crvSymbColor;
    //public int[][] crvClipXmin, crvClipXmax, crvClipYmin, crvClipYmax;
    //public double[][] crvClipValXmin, crvClipValXmax, crvClipValYmin, crvClipValYmax;
    private boolean[][] crvFillDigitalCurves;
    private int[][] crvFillingDigitalColor;
    private double[][] crvTransparency;
    public int[] powerAnalysisCurrentIndex = {-1, -1, -1};
    public int[] powerAnalysisVoltageIndex = {-1, -1, -1};
    private final static int DIAGRAM_TYP_ZV = 91, DIAGRAM_TYP_SGN = 92;
    private boolean newFormat = false;
    private int noInputSignals;
    private boolean _isFormatBefore160;

    /**
     * Scope settings is not used anymore. The scope settings are saved internally in grapher.
     * This class is only here for backwards-compatibility, and will be removed, soon.
     * @deprecated
     */
    @Deprecated
    public ScopeSettings() {
        anzDiagram = 1;

        matrixZuordnungKurveDiagram = new int[ANZ_DIAGRAM_MAX][50];
        crvLineColor = new int[ANZ_DIAGRAM_MAX][50];
        crvLineStyle = new int[ANZ_DIAGRAM_MAX][50];
        crvSymbShow = new boolean[ANZ_DIAGRAM_MAX][50];
        crvSymbColor = new int[ANZ_DIAGRAM_MAX][50];

        for (int i = 0; i < crvLineColor.length; i++) {
            for (int j = 0; j < crvLineColor[0].length; j++) {
                crvLineColor[i][j] = GeckoColor.getNextColor().code();
            }
        }

        signalNamen.clear();        


        crvSymbShape = new int[ANZ_DIAGRAM_MAX][50];
        crvSymbFrequ = new int[ANZ_DIAGRAM_MAX][50];
        crvFillingDigitalColor = new int[ANZ_DIAGRAM_MAX][50];
        crvFillDigitalCurves = new boolean[ANZ_DIAGRAM_MAX][50];
        crvAchsenTyp = new int[ANZ_DIAGRAM_MAX][50];

        nameDiagram = new ArrayList<String>();
        for (int i = 0; i < ANZ_DIAGRAM_MAX; i++) {
            nameDiagram.add("GRF " + (1 + i));
        }
        ySpacingDiagram = new ArrayList<Double>();
        diagramTyp = new int[ANZ_DIAGRAM_MAX];

        autoScaleX = new boolean[ANZ_DIAGRAM_MAX];
        autoScaleY = new boolean[ANZ_DIAGRAM_MAX];

        userScaleXMax = new ArrayList<Double>();
        userScaleXMin = new ArrayList<Double>();
        userScaleYMax = new ArrayList<Double>();
        userScaleYMin = new ArrayList<Double>();

        xAchsenTyp = new int[ANZ_DIAGRAM_MAX];
        yAchsenTyp = new int[ANZ_DIAGRAM_MAX];
        xAchseFarbe = new int[ANZ_DIAGRAM_MAX];
        yAchseFarbe = new int[ANZ_DIAGRAM_MAX];
        xAchseStil = new int[ANZ_DIAGRAM_MAX];
        yAchseStil = new int[ANZ_DIAGRAM_MAX];
        xAchseBeschriftung = new ArrayList<String>();

        yAchseBeschriftung = new ArrayList<String>();

        for (int i = 0; i < ANZ_DIAGRAM_MAX; i++) {
            xAchseBeschriftung.add("");
            yAchseBeschriftung.add("");
        }

        //
        farbeGridNormalX = new int[ANZ_DIAGRAM_MAX];
        farbeGridNormalXminor = new int[ANZ_DIAGRAM_MAX];
        farbeGridNormalY = new int[ANZ_DIAGRAM_MAX];
        farbeGridNormalYminor = new int[ANZ_DIAGRAM_MAX];
        linStilGridNormalX = new int[ANZ_DIAGRAM_MAX];
        linStilGridNormalXminor = new int[ANZ_DIAGRAM_MAX];
        linStilGridNormalY = new int[ANZ_DIAGRAM_MAX];
        linStilGridNormalYminor = new int[ANZ_DIAGRAM_MAX];
        xShowGridMaj = new boolean[ANZ_DIAGRAM_MAX];
        xShowGridMin = new boolean[ANZ_DIAGRAM_MAX];
        yShowGridMaj = new boolean[ANZ_DIAGRAM_MAX];
        yShowGridMin = new boolean[ANZ_DIAGRAM_MAX];
        //
        xAnzTicksMinor = new int[ANZ_DIAGRAM_MAX];
        yAnzTicksMinor = new int[ANZ_DIAGRAM_MAX];
        xTickLaenge = new int[ANZ_DIAGRAM_MAX];
        xTickLaengeMinor = new int[ANZ_DIAGRAM_MAX];
        yTickLaenge = new int[ANZ_DIAGRAM_MAX];
        yTickLaengeMinor = new int[ANZ_DIAGRAM_MAX];
        //
        zeigeLabelsXmaj = new boolean[ANZ_DIAGRAM_MAX];
        zeigeLabelsXmin = new boolean[ANZ_DIAGRAM_MAX];
        zeigeLabelsYmaj = new boolean[ANZ_DIAGRAM_MAX];
        zeigeLabelsYmin = new boolean[ANZ_DIAGRAM_MAX];

        ORIGjcbXShowGridMaj = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcbXShowGridMin = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcbYShowGridMaj = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcbYShowGridMin = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcmXlinCol = new int[ANZ_DIAGRAM_MAX];
        ORIGjcmYlinCol = new int[ANZ_DIAGRAM_MAX];
        ORIGjcmXlinStyl = new int[ANZ_DIAGRAM_MAX];
        ORIGjcmYlinStyl = new int[ANZ_DIAGRAM_MAX];
        ORIGjtfXtickLengthMaj = new int[ANZ_DIAGRAM_MAX];
        ORIGjtfXtickLengthMin = new int[ANZ_DIAGRAM_MAX];
        ORIGjtfYtickLengthMaj = new int[ANZ_DIAGRAM_MAX];
        ORIGjtfYtickLengthMin = new int[ANZ_DIAGRAM_MAX];
        ORIGjcbXShowLabelMaj = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcbXShowLabelMin = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcbYShowLabelMaj = new boolean[ANZ_DIAGRAM_MAX];
        ORIGjcbYShowLabelMin = new boolean[ANZ_DIAGRAM_MAX];
        // speziell fuer SIGNAL -->

        crvTransparency = new double[50][50];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                crvTransparency[i][j] = 0.9;
            }
        }

    }        
    
    
    public void loadSettings(GraferV4 impl) {        
        if (_isFormatBefore160) {
            final DiagramCurveSignalManager manager = impl.getManager();
            manager.updateCurveNumber(noInputSignals);
            final int noDiagrams = manager.getNumberDiagrams();
            
            if (noDiagrams != anzDiagram) {
                while (impl.getManager().getDiagrams().size() < anzDiagram) {
                    impl.getManager().addDiagram(new DiagramCurve(impl));
                }
            }

            for (int i = 0; i < anzDiagram; i++) {
                AbstractDiagram diag = manager.getDiagram(i);

                final Axis xAxis = diag._xAxis;
                final Axis yAxis = diag._yAxis1;


                final DiagramSettings diagramSettings = diag._diagramSettings;
                diagramSettings.setNameDiagram(nameDiagram.get(i));
                xAxis._axisTickSettings.setShowLabelsMaj(zeigeLabelsXmaj[i]);
                xAxis._axisTickSettings.setShowLabelsMin(zeigeLabelsXmin[i]);
                yAxis._axisTickSettings.setShowLabelsMaj(zeigeLabelsYmaj[i]);
                yAxis._axisTickSettings.setShowLabelsMin(zeigeLabelsYmin[i]);
                xAxis._axisGridSettings.setLinStyleMaj(GeckoLineStyle.getFromCode(linStilGridNormalX[i]));
                xAxis._axisGridSettings.setLinStyleMin(GeckoLineStyle.getFromCode(linStilGridNormalXminor[i]));
                yAxis._axisGridSettings.setLinStyleMaj(GeckoLineStyle.getFromCode(linStilGridNormalY[i]));
                yAxis._axisGridSettings.setLinStyleMin(GeckoLineStyle.getFromCode(linStilGridNormalYminor[i]));
                xAxis._axisTickSettings.setAnzTicksMinor(xAnzTicksMinor[i]);
                yAxis._axisTickSettings.setAnzTicksMinor(yAnzTicksMinor[i]);
                xAxis._axisTickSettings.setTickLengthMaj(xTickLaenge[i]);
                xAxis._axisTickSettings.setTickLengthMin(xTickLaengeMinor[i]);
                yAxis._axisTickSettings.setTickLengthMaj(yTickLaenge[i]);
                yAxis._axisTickSettings.setTickLengthMin(yTickLaengeMinor[i]);
                diagramSettings.setWeightDiagram(ySpacingDiagram.get(i));
                xAxis._axisGridSettings.setColorGridMaj(GeckoColor.getFromCode(farbeGridNormalX[i]));
                xAxis._axisGridSettings.setColorGridMin(GeckoColor.getFromCode(farbeGridNormalXminor[i]));
                yAxis._axisGridSettings.setColorGridMaj(GeckoColor.getFromCode(farbeGridNormalY[i]));
                yAxis._axisGridSettings.setColorGridMin(GeckoColor.getFromCode(farbeGridNormalYminor[i]));

                xAxis._axisSettings.setColor(GeckoColor.getFromCode(xAchseFarbe[i]));
                yAxis._axisSettings.setColor(GeckoColor.getFromCode(yAchseFarbe[i]));
                xAxis._axisSettings.setStroke(GeckoLineStyle.getFromCode(xAchseStil[i]));
                yAxis._axisSettings.setStroke(GeckoLineStyle.getFromCode(yAchseStil[i]));

                xAxis._axisMinMax.setAutoEnabled(autoScaleX[i]);
                yAxis._axisMinMax.setAutoEnabled(autoScaleY[i]);

                final HiLoData userScaleX = HiLoData.hiLoDataFabric(-1.0f, 1.0f);
                final HiLoData userScaleY = HiLoData.hiLoDataFabric(-1.0f, 1.0f);
                xAxis._axisMinMax.setUserScale(userScaleX);
                yAxis._axisMinMax.setUserScale(userScaleY);

                xAxis.setAxisType(AxisLinLog.getFromCode(xAchsenTyp[i]));
                yAxis.setAxisType(AxisLinLog.getFromCode(yAchsenTyp[i]));


                if (diagramTyp[i] == DIAGRAM_TYP_SGN) {
                    DiagramSignal newDiagram = new DiagramSignal(diag);
                    manager.replaceDiagram(diag, newDiagram);
                    diag = newDiagram;
                }

                final Axis y1Axis = diag._yAxis1;

                if (newFormat) { // in the older verions, these properties were not saved at all (set to false/zero)
                    // therefore, opening old models would not show any ticks... what a hack!
                    xAxis._axisGridSettings.setUserShowGridMaj(ORIGjcbXShowGridMaj[i]);
                    xAxis._axisGridSettings.setUserShowGridMin(ORIGjcbXShowGridMin[i]);
                    y1Axis._axisGridSettings.setUserShowGridMaj(ORIGjcbYShowGridMaj[i]);
                    y1Axis._axisGridSettings.setUserShowGridMin(ORIGjcbYShowGridMin[i]);

                    xAxis._axisTickSettings.setShowLabelsMaj(ORIGjcbXShowLabelMaj[i]);
                    xAxis._axisTickSettings.setShowLabelsMin(ORIGjcbXShowLabelMin[i]);
                    y1Axis._axisTickSettings.setShowLabelsMaj(ORIGjcbYShowLabelMaj[i]);
                    y1Axis._axisTickSettings.setShowLabelsMin(ORIGjcbYShowLabelMin[i]);

                    xAxis._axisTickSettings.setTickLengthMaj(ORIGjtfXtickLengthMaj[i]);
                    xAxis._axisTickSettings.setTickLengthMin(ORIGjtfXtickLengthMin[i]);
                    y1Axis._axisTickSettings.setTickLengthMaj(ORIGjtfYtickLengthMaj[i]);
                    y1Axis._axisTickSettings.setTickLengthMin(ORIGjtfYtickLengthMin[i]);

                    xAxis._axisGridSettings.getColorGridMaj().getFromCode(ORIGjcmXlinCol[i]);
                    y1Axis._axisGridSettings.getColorGridMaj().getFromCode(ORIGjcmYlinCol[i]);
                    xAxis._axisGridSettings.getLinStyleMaj().getFromCode(ORIGjcmXlinStyl[i]);
                    y1Axis._axisGridSettings.getLinStyleMaj().getFromCode(ORIGjcmYlinStyl[i]);
                }

                int diagNo = 0;
                int curveNo = 0;
                manager.updateCurveNumber(noInputSignals);
                for (int jj = 1; jj < noInputSignals + 1; jj++) {
                    AxisConnection axisConnection = AxisConnection.getFromCode(matrixZuordnungKurveDiagram[i][jj]);
                    diag.getCurve(jj - 1).setAxisConnection(axisConnection);
                    if (axisConnection == AxisConnection.ZUORDNUNG_SIGNAL) {
                        diagNo = manager.getDiagrams().indexOf(diag);
                        curveNo = diag.getCurves().indexOf(diag.getCurve(jj - 1));
                    }
                }


                for (int j = 0; j < manager.getNumberInputSignals(); j++) {
                    AbstractCurve curve = diag.getCurve(j);
                    CurveSettings curveSettings = curve.getCurveSettings();
                    curveSettings._curveColor = GeckoColor.getFromCode(crvLineColor[i][j + 1]);
                    curveSettings._curveLineStyle = GeckoLineStyle.getFromCode(crvLineStyle[i][j + 1]);
                    curveSettings._curveShowPtSymbols = crvSymbShow[i][j + 1];
                    curveSettings._crvSymbFarbe = GeckoColor.getFromCode(crvSymbColor[i][j + 1]);
                    curveSettings._crvSymbShape = GeckoSymbol.getFromCode(crvSymbShape[i][j + 1]);
                    curveSettings._crvSymbFrequ = crvSymbFrequ[i][j + 1];
                    curveSettings._crvTransparency = crvTransparency[i][j + 1];
                    curveSettings._crvFillingDigColor = GeckoColor.getFromCode(crvFillingDigitalColor[i][j + 1]);
                    curveSettings._crvFillDigitalCurves = crvFillDigitalCurves[i][j + 1];
                }
            }
        }

        impl.refreshComponentPane();

    }

    // zum Speichern im ASCII-Format (anstatt als Object-Stream) -->
    //
    public void exportASCII(final StringBuffer ascii) {
        ascii.append("\n<scopeSettings>");
        DatenSpeicher.appendAsString(ascii.append("\nnoInputSignals"), noInputSignals);
        ascii.append("\n<\\scopeSettings>");
    }

    public boolean importASCII(final TokenMap tokenMap) {        
        if (tokenMap.containsToken("xAchseStil[]")) {
            _isFormatBefore160 = true;
        } else {
            _isFormatBefore160 = false;
            return true;
        }
        
        if (tokenMap.containsToken("noInputSignals")) {
            noInputSignals = tokenMap.readDataLine("noInputSignals", noInputSignals);
        }

        anzDiagram = tokenMap.readDataLine("anzDiagram", anzDiagram);
        autoScaleX = tokenMap.readDataLine("autoScaleX[]", autoScaleX);
        autoScaleY = tokenMap.readDataLine("autoScaleY[]", autoScaleY);

        if (tokenMap.containsToken("userScaleXMin[]")) {
            userScaleXMin = tokenMap.readDataLineDoubleArray("userScaleXMin[]");
        }

        if (tokenMap.containsToken("userScaleXMax[]")) {
            userScaleXMax = tokenMap.readDataLineDoubleArray("userScaleXMax[]");
        }

        if (tokenMap.containsToken("userScaleYMin[]")) {
            userScaleYMin = tokenMap.readDataLineDoubleArray("userScaleYMin[]");
        }

        if (tokenMap.containsToken("userScaleYMax[]")) {
            userScaleYMax = tokenMap.readDataLineDoubleArray("userScaleYMax[]");
        }

        xShowGridMaj = tokenMap.readDataLine("xShowGridMaj[]", xShowGridMaj);
        xShowGridMin = tokenMap.readDataLine("xShowGridMin[]", xShowGridMin);
        yShowGridMaj = tokenMap.readDataLine("yShowGridMaj[]", yShowGridMaj);
        yShowGridMin = tokenMap.readDataLine("yShowGridMin[]", yShowGridMin);
        zeigeLabelsXmaj = tokenMap.readDataLine("zeigeLabelsXmaj[]", zeigeLabelsXmaj);
        zeigeLabelsXmin = tokenMap.readDataLine("zeigeLabelsXmin[]", zeigeLabelsXmin);
        zeigeLabelsYmaj = tokenMap.readDataLine("zeigeLabelsYmaj[]", zeigeLabelsYmaj);
        zeigeLabelsYmin = tokenMap.readDataLine("zeigeLabelsYmin[]", zeigeLabelsYmin);
        diagramTyp = tokenMap.readDataLine("diagramTyp[]", diagramTyp);
        xAchsenTyp = tokenMap.readDataLine("xAchsenTyp[]", xAchsenTyp);
        yAchsenTyp = tokenMap.readDataLine("yAchsenTyp[]", yAchsenTyp);
        xAchseFarbe = tokenMap.readDataLine("xAchseFarbe[]", xAchseFarbe);
        yAchseFarbe = tokenMap.readDataLine("yAchseFarbe[]", yAchseFarbe);
        xAchseStil = tokenMap.readDataLine("xAchseStil[]", xAchseStil);
        yAchseStil = tokenMap.readDataLine("yAchseStil[]", yAchseStil);
        farbeGridNormalX = tokenMap.readDataLine("farbeGridNormalXminor[]", farbeGridNormalX);
        farbeGridNormalXminor = tokenMap.readDataLine("farbeGridNormalXminor[]", farbeGridNormalXminor);
        farbeGridNormalY = tokenMap.readDataLine("farbeGridNormalY[]", farbeGridNormalY);
        farbeGridNormalYminor = tokenMap.readDataLine("farbeGridNormalYminor[]", farbeGridNormalYminor);
        linStilGridNormalX = tokenMap.readDataLine("linStilGridNormalX[]", linStilGridNormalX);
        linStilGridNormalXminor = tokenMap.readDataLine("linStilGridNormalXminor[]", linStilGridNormalXminor);
        linStilGridNormalY = tokenMap.readDataLine("linStilGridNormalY[]", linStilGridNormalY);
        linStilGridNormalYminor = tokenMap.readDataLine("linStilGridNormalYminor[]", linStilGridNormalYminor);
        xAnzTicksMinor = tokenMap.readDataLine("xAnzTicksMinor[]", xAnzTicksMinor);
        yAnzTicksMinor = tokenMap.readDataLine("yAnzTicksMinor[]", yAnzTicksMinor);
        xTickLaenge = tokenMap.readDataLine("xTickLaenge[]", xTickLaenge);
        xTickLaengeMinor = tokenMap.readDataLine("xTickLaengeMinor[]", xTickLaengeMinor);
        yTickLaenge = tokenMap.readDataLine("yTickLaenge[]", yTickLaenge);

        yTickLaengeMinor = tokenMap.readDataLine("yTickLaengeMinor[]", yTickLaengeMinor);
        ySpacingDiagram = tokenMap.readDataLineDoubleArray("ySpacingDiagram[]");
        nameDiagram = tokenMap.readDataLineStringArray("nameDiagram[]");
        xAchseBeschriftung = tokenMap.readDataLineStringArray("xAchseBeschriftung[]");
        yAchseBeschriftung = tokenMap.readDataLineStringArray("yAchseBeschriftung[]");
        ORIGjcbXShowGridMaj = tokenMap.readDataLine("ORIGjcbXShowGridMaj[]", ORIGjcbXShowGridMaj);
        ORIGjcbXShowGridMin = tokenMap.readDataLine("ORIGjcbXShowGridMin[]", ORIGjcbXShowGridMin);
        ORIGjcbYShowGridMaj = tokenMap.readDataLine("ORIGjcbYShowGridMaj[]", ORIGjcbYShowGridMaj);
        ORIGjcbYShowGridMin = tokenMap.readDataLine("ORIGjcbYShowGridMin[]", ORIGjcbYShowGridMin);
        ORIGjcmXlinCol = tokenMap.readDataLine("ORIGjcmXlinCol[]", ORIGjcmXlinCol);
        ORIGjcmYlinCol = tokenMap.readDataLine("ORIGjcmYlinCol[]", ORIGjcmYlinCol);
        ORIGjcmXlinStyl = tokenMap.readDataLine("ORIGjcmXlinStyl[]", ORIGjcmXlinStyl);
        ORIGjcmYlinStyl = tokenMap.readDataLine("ORIGjcmYlinStyl[]", ORIGjcmYlinStyl);


        if (tokenMap.containsToken("ORIGjtfXtickLengthMajNew[]")) {
            newFormat = true;
            ORIGjtfXtickLengthMaj = tokenMap.readDataLine("ORIGjtfXtickLengthMajNew[]", ORIGjtfXtickLengthMaj);
        }

        if (tokenMap.containsToken("ORIGjtfXtickLengthMinNew[]")) {
            newFormat = true;
            ORIGjtfXtickLengthMin = tokenMap.readDataLine("ORIGjtfXtickLengthMinNew[]", ORIGjtfXtickLengthMin);
        }

        if (tokenMap.containsToken("ORIGjtfYtickLengthMajNew[]")) {
            newFormat = true;
            ORIGjtfYtickLengthMaj = tokenMap.readDataLine("ORIGjtfYtickLengthMajNew[]", ORIGjtfYtickLengthMaj);
        }

        if (tokenMap.containsToken("ORIGjtfYtickLengthMinNew[]")) {
            ORIGjtfYtickLengthMin = tokenMap.readDataLine("ORIGjtfYtickLengthMinNew[]", ORIGjtfYtickLengthMin);
        }

        if (tokenMap.containsToken("ORIGjcbXShowLabelMajNew[]")) {
            ORIGjcbXShowLabelMaj = tokenMap.readDataLine("ORIGjcbXShowLabelMajNew[]", ORIGjcbXShowLabelMaj);
        }

        if (tokenMap.containsToken("ORIGjcbXShowLabelMinNew[]")) {
            ORIGjcbXShowLabelMin = tokenMap.readDataLine("ORIGjcbXShowLabelMinNew[]", ORIGjcbXShowLabelMin);
        }

        if (tokenMap.containsToken("ORIGjcbYShowLabelMajNew[]")) {
            ORIGjcbYShowLabelMaj = tokenMap.readDataLine("ORIGjcbYShowLabelMajNew[]", ORIGjcbYShowLabelMaj);
        }

        if (tokenMap.containsToken("ORIGjcbYShowLabelMinNew[]")) {
            ORIGjcbYShowLabelMin = tokenMap.readDataLine("ORIGjcbYShowLabelMinNew[]", ORIGjcbYShowLabelMin);
        }

        signalNamen = tokenMap.readDataLineStringArray("signalNamen[]");

        noInputSignals = signalNamen.size() - 1;   // -1: compatibility to old save format!                                                     
        matrixZuordnungKurveDiagram = tokenMap.readDataLine("matrixZuordnungKurveDiagram[][]", matrixZuordnungKurveDiagram);


        indexWsXY = tokenMap.readDataLine("indexWsXY[][]", indexWsXY);
        crvAchsenTyp = tokenMap.readDataLine("crvAchsenTyp[][]", crvAchsenTyp);
        crvLineStyle = tokenMap.readDataLine("crvLineStyle[][]", crvLineStyle);
        crvLineColor = tokenMap.readDataLine("crvLineColor[][]", crvLineColor);
        if (tokenMap.containsToken("crvTransparency[][]")) {
            crvTransparency = tokenMap.readDataLine("crvTransparency[][]", crvTransparency);
        }

        crvSymbFrequ = tokenMap.readDataLine("crvSymbFrequ[][]", crvSymbFrequ);
        crvSymbShape = tokenMap.readDataLine("crvSymbShape[][]", crvSymbShape);

        crvSymbColor = tokenMap.readDataLine("crvSymbColor[][]", crvSymbColor);
        crvSymbShow = tokenMap.readDataLine("crvSymbShow[][]", crvSymbShow);
        crvFillDigitalCurves = tokenMap.readDataLine("crvFillDigitalCurves[][]", crvFillDigitalCurves);
        crvFillingDigitalColor = tokenMap.readDataLine("crvFillingDigitalColor[][]", crvFillingDigitalColor);

        if (tokenMap.containsToken("powerAnalysisCurrents[]")) {
            powerAnalysisCurrentIndex = tokenMap.readDataLine("powerAnalysisCurrents[]", powerAnalysisCurrentIndex);
            powerAnalysisVoltageIndex = tokenMap.readDataLine("powerAnalysisVoltages[]", powerAnalysisVoltageIndex);
        }


        // Achtung: in alten Versionen sind diese beiden Parameter nicht gesetzt, daher hier default-Initialisierung: 
        if (crvTransparency == null) {
            crvTransparency = new double[crvLineColor.length][crvLineColor[0].length];
            for (int i = 0; i < crvTransparency.length; i++) {
                for (int j = 0; j < crvTransparency[0].length; j++) {
                    crvTransparency[i][j] = 1.0;
                }
            }
        }


        if (crvFillDigitalCurves == null) {
            crvFillDigitalCurves = new boolean[crvLineColor.length][crvLineColor[0].length];
            for (int i1 = 0; i1 < crvLineColor.length; i1++) {
                for (int i2 = 0; i2 < crvLineColor[0].length; i2++) {
                    crvFillDigitalCurves[i1][i2] = true;
                    crvFillingDigitalColor[i1][i2] = GeckoColor.LIGHTGRAY.code();
                }
            }
        }
        return true;
    }        
    
}
