/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import com.sun.tools.javac.util.Pair;
import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCapacitor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractInductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractResistor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.InductorCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.MutualInductance;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SourceType;
import ch.technokrat.gecko.geckocircuits.control.AbstractPotentialMeasurement;
import java.util.*;

public class NetListLK {

    public int knotenMAX, spgQuelleMAX;  // knotenMAX ... Gesamt-Knotenanzahl minus 'Ground';  spgQuelleMAX ... Summe der SpgQuellen plus LKOP2-Elemente
    public CircuitTyp[] typ;
    public int[] knotenX, knotenY;
    public int[][] nodePairDirVoltContSrc;
    public double[][] parameter;  // Bauteilwert; Typus 'sinus', 'dreieck', ... bei Strom/Spg-Quelle; init-Werte iL_ALT und uC_ALT; ...
    public int[] spgQuelleNr;  // zaehlt nicht nur die SpgQuellen von 1 beginnend aufsteigen, sondern auch die LKOP2-Elemente, wobei Durchmischung mit den SpgQuellen moeglich ist
    protected int[][] gemeinsameKnoten;  // Element-Knoten plus alle Verbindungen, die auf dem gleichen Potential liegen
    protected int gesamtzahlKnotenNr;
    public Verbindung[] v;
    public int verbindungANZAHL;
    public AbstractCircuitBlockInterface[] elements, eLKneu, eLK_M;
    public int elementANZAHL, elementANZAHLneu;
    public String[] labelListe;
    //
    public PotentialArea[] potLab;
    public double t;  // Aktuelle Zeit in der Simulation
    // zur Beschreibung magnetischer Kopplungen -->
    private AbstractCircuitBlockInterface[] alleGekoppeltenLc;  // eine Auflistung aller verschiedenen gekoppelten Lc-Elemente
    private AbstractCircuitBlockInterface[][] partnerLc;  // jedem gelisteten Lc-Element werden die Kopplungspartner-Lc zugeteilt
    private double[][] kopplungen;    // das sind die zugehoerigen Kopplungs-Werte
    private PostCalculatable[] _postCalculatables = new PostCalculatable[0];
    public int[] _singularityEntries = new int[0];

    
    public double getSimulationsZeit() {
        return t;
    }
    

    public int[][] getGemeinsameKnoten() {
        return gemeinsameKnoten;
    }

    public int getGesamtzahlKnotenNr() {
        return gesamtzahlKnotenNr;
    }

    public Verbindung[] getVerbindungen() {
        return v;
    }

    public int getVerbindungANZAHL() {
        return verbindungANZAHL;
    }

    public int getElementANZAHL() {
        return elementANZAHL;
    }

    public int getElementANZAHLinklusiveSubcircuit() {
        return elementANZAHLneu;
    }  //  Kopplungen M werden nicht mitgezaehlt!

    // die Nummer eines ElementLK in der Netzliste ist oft nicht ident mit der ID-Nummer,
    // zB. wenn Kopplung k eingebaut wird, und anschliessend weitere ElementLK hinzugefuegt werden -->
    // fuer die Kopplung von Induktivitaeten braucht man aber die NUmmer in der aktuellen Netzliste zur Zuordnung -->
    private int getNetzlistenNummer(AbstractCircuitBlockInterface search) {
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            if (search.equals(eLKneu[i1])) {
                return i1;
            }
        }
        System.out.println("Fehler qer^08gj03qhg4");
        return -1;
    }

    // alle Kopplungen M, die im Algorithmus in LKMatrizen ausgewertet werden -->
    public double[][][] getAlleKopplungenM() {
        double[][] zuLKOP2gehoerigeM_spgQnr = new double[elementANZAHLneu][];
        double[][] zuLKOP2gehoerigeM_kWerte = new double[elementANZAHLneu][];
        this.definiere_magnetischeKopplungen_im_LK();  // was sind die Koppelpartner (und Koppelwerte) der einzelnen Induktivitaeten? -->
        //--------------------
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            if (eLKneu[i1] instanceof InductorCoupable) {
                for (int i2 = 0; i2 < alleGekoppeltenLc.length; i2++) {
                    if (alleGekoppeltenLc[i2] == eLKneu[i1]) {
                        int anzahlKoppelPartner = partnerLc[i2].length;
                        double[] temp_spgQnr = new double[anzahlKoppelPartner];
                        double[] temp_kWerte = new double[anzahlKoppelPartner];
                        for (int i3 = 0; i3 < anzahlKoppelPartner; i3++) {
                            int koppelPartnerID = this.getNetzlistenNummer(partnerLc[i2][i3]);
                            for (int i4 = 0; i4 < elementANZAHLneu; i4++) {
                                if (koppelPartnerID == this.getNetzlistenNummer(eLKneu[i4])) {
                                    temp_spgQnr[i3] = spgQuelleNr[i4];
                                }
                            }
                            temp_kWerte[i3] = kopplungen[i2][i3];
                        }
                        zuLKOP2gehoerigeM_spgQnr[i1] = temp_spgQnr;
                        zuLKOP2gehoerigeM_kWerte[i1] = temp_kWerte;
                    }
                }
            }
        }

        return new double[][][]{zuLKOP2gehoerigeM_spgQnr, zuLKOP2gehoerigeM_kWerte};
    }

    /**
     * this fabric exists, since the constructor name makes it difficult to
     * distinguish between the full netlist and the netlist without subcircuits
     *
     * @param v
     * @param e
     * @return
     */
    public static NetListLK fabricIncludingSubcircuits(final Set<Verbindung> v, List<? extends AbstractBlockInterface> e) {                        
        NetzlisteAllg nl = NetzlisteAllg.fabricNetzlistDisabledParentSubsRemoved(v, e);
        nl.deSingularizeIsolatedPotentials();
        return new NetListLK(nl, true);
    }

    public static NetListLK fabricExcludingSubcircuits(final Collection<Verbindung> v, List<? extends AbstractBlockInterface> e) {
        return new NetListLK(NetzlisteAllg.fabricNetzlistLabelUpdate(v, e), false);
    }

    private NetListLK(NetzlisteAllg nlLK, boolean includeSubCircuits) {
        _singularityEntries = nlLK._singularityIndices;
                
        this.v = nlLK._connections.toArray(new Verbindung[0]);
        this.verbindungANZAHL = v.length;
        this.potLab = nlLK.getPotentiale();

        List<AbstractBlockInterface> elements = new ArrayList<AbstractBlockInterface>(nlLK.getElemente());

        this.elements = new AbstractCircuitBlockInterface[elements.size()];
        List<PostCalculatable> tmpPostCalculatables = new ArrayList<PostCalculatable>();
        for (int i = 0; i < this.elements.length; i++) {
            this.elements[i] = (AbstractCircuitBlockInterface) elements.get(i);
            if (this.elements[i] instanceof PostCalculatable) {
                tmpPostCalculatables.add((PostCalculatable) this.elements[i]);
            }
        }        
                
        this.elementANZAHL = this.elements.length;
        this.elementANZAHLneu = this.elementANZAHL;  // wird weiter unten im Fall eventueller Subcircuits korregiert
        if (includeSubCircuits) {
            this.initialisiereMitSubcircuit();
            this.defineNodePairDirVoltContSrc();
        }
        
        this._postCalculatables = tmpPostCalculatables.toArray(new PostCalculatable[tmpPostCalculatables.size()]);
        for (PostCalculatable calc : _postCalculatables) {
            calc.doInitialization();
        }
        
        labelListe = new String[potLab.length];
        for (int i1 = 0; i1 < potLab.length; i1++) {
            String label = potLab[i1].getLabel();
            labelListe[i1] = label;
        } 
        
        

    }

    /**
     * this version is only for the initalization/ internal replacement of
     * capacitors with voltage sources.
     */
    private NetListLK(CircuitTyp[] typ, int[] knotenX, int[] knotenY, double[][] parameter, int[] spgQuelleNr) {
        // Voraussetzung 1: Knoten sind durchgehend von Null weg aufsteigend numeriert
        //------------------------------
        this.typ = typ;
        this.knotenX = knotenX;
        this.knotenY = knotenY;
        this.parameter = parameter;
        
        this.spgQuelleNr = spgQuelleNr;
        this.elementANZAHL = typ.length;
        this.elementANZAHLneu = this.elementANZAHL;
        //------------------------------
        // Voraussetzung 2: Spannungsquellen-Nummern sind von Eins weg durchgehend und aufsteigend numeriert
        // -->
        knotenMAX = 0;  // Anzahl der (verschiedenen) Knoten
        spgQuelleMAX = 0;  // Anzahl der (verschiedenen) Spannungsquellen
        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            if (knotenX[i1] > knotenMAX) {
                knotenMAX = knotenX[i1];
            }
            if (knotenY[i1] > knotenMAX) {
                knotenMAX = knotenY[i1];
            }
            if (spgQuelleNr[i1] > spgQuelleMAX) {
                spgQuelleMAX = spgQuelleNr[i1];
            }
        }
    }

    // in case of direct-voltage-control of sources the nodes of the element, where the voltage is measured,
    // have to be found in the following methode;
    // this is employed in LKMatrizen() to set up matrix A >>
    private void defineNodePairDirVoltContSrc() {
        nodePairDirVoltContSrc = new int[elementANZAHLneu][2];
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            AbstractComponentTyp circuitTyp = eLKneu[i1].getTypeEnum();
            if (circuitTyp == CircuitTyp.LK_U || circuitTyp == CircuitTyp.LK_I || circuitTyp == CircuitTyp.REL_MMF) {
                ComponentCoupable compCoupable = (ComponentCoupable) eLKneu[i1];
                ComponentCoupling coupling = compCoupable.getComponentCoupling();
                for (int i2 = 0; i2 < elementANZAHLneu; i2++) {
                    if (eLKneu[i2] == coupling._coupledElements[0]) {
                        nodePairDirVoltContSrc[i1][0] = knotenX[i2];
                        nodePairDirVoltContSrc[i1][1] = knotenY[i2];
                    }
                }
            }
        }
    }

    public boolean updateNonlinearCapacitancesAndResistors() {
        boolean returnValue = false;
        for (AbstractCircuitBlockInterface elem : eLKneu) {
            if (elem instanceof AbstractCapacitor) {
                if (((AbstractCapacitor) elem).updateNonlinearCapacitances()) {
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    // bei jedem Zeitschritt in der Simulationsschleife in 'SimulationsKern' werden analytische Komponenten des SubCircuit berechnet,
    // (dh. nicht als Netzliste um Rechenaufwand zu reduzieren)
    public void berechneSubCircuitAlsDifferentialgleichung(double dt, double t) {
        this.t = t;
        for (PostCalculatable calc : _postCalculatables) {                        
            calc.doCalculation(dt, t);
        }
    }

    // zB. die im SubCircuit definierten ElementeLK werden in die LK-Netzliste integriert -->
    public final void integriereSubCircuits() {
        Set<AbstractBlockInterface> eLKneuSet = new LinkedHashSet<AbstractBlockInterface>();
        ArrayList<AbstractCircuitBlockInterface> eLK_M_vec = new ArrayList<AbstractCircuitBlockInterface>();

        for (AbstractCircuitBlockInterface elem : elements) {
            if (elem instanceof HiddenSubCircuitable) {
                // Element wird aufgeloest in seine einzelnen LK-Elemente -->
                
                HiddenSubCircuitable subCircuitable = (HiddenSubCircuitable) elem;             
                if (subCircuitable.includeParentInSimulation()) {
                    eLKneuSet.add(elem);
                }
                eLKneuSet.addAll(subCircuitable.getHiddenSubCircuitElements());                
                
            } else {
                if (elem instanceof MutualInductance) {
                    eLK_M_vec.add(elem);
                } else {
                    eLKneuSet.add(elem);
                }
            }
        }

        elementANZAHLneu = eLKneuSet.size();
        eLKneu = eLKneuSet.toArray(new AbstractCircuitBlockInterface[eLKneuSet.size()]);
        eLK_M = eLK_M_vec.toArray(new AbstractCircuitBlockInterface[eLK_M_vec.size()]);
    }

    protected void initialisiereMitSubcircuit() {
        this.integriereSubCircuits();
        Set<Verbindung> connections = new LinkedHashSet<Verbindung>();
        for (Verbindung verb : v) {
            connections.add(verb);
        }

        List<AbstractBlockInterface> eLKneuList = new ArrayList<AbstractBlockInterface>();
        for (int i = 0; i < eLKneu.length; i++) {
            if (eLKneu[i] != null) {
                eLKneuList.add(eLKneu[i]);
            }
        }
        NetzlisteAllg netList = NetzlisteAllg.fabricNetzlistComplete(connections, eLKneuList);
        netList.deSingularizeIsolatedPotentials();
        _singularityEntries = netList._singularityIndices;                
        
        this.potLab = netList.getPotentiale();
        
        //***********************************************************
        // LK-Knotenliste -->
        typ = new CircuitTyp[elementANZAHLneu];
        knotenX = new int[elementANZAHLneu];
        knotenY = new int[elementANZAHLneu];
        parameter = new double[elementANZAHLneu][];
        spgQuelleNr = new int[elementANZAHLneu];
        int spgQuelleZaehler = 1;
        //***********************************************************
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            AbstractCircuitBlockInterface elem = eLKneu[i1];
            //------------------
            // allgemein:
            CircuitTyp circuitTyp = (CircuitTyp) elem.getTypeEnum();
            typ[i1] = circuitTyp;
            parameter[i1] = elem.getParameter();
            spgQuelleNr[i1] = (circuitTyp == CircuitTyp.REL_MMF || circuitTyp == CircuitTyp.LK_U
                    || circuitTyp == CircuitTyp.LK_LKOP2) || circuitTyp == CircuitTyp.TH_TEMP ? (spgQuelleZaehler++) : -1;
            //------------------
            // Anfangsknoten:            
            List<AbstractTerminal> startTerminals = elem.XIN;
            int[] nrAnfangsKn = new int[startTerminals.size()];  // dieses Array muss mit Knotennummern gefuellt werden
            for (int i2 = 0; i2 < startTerminals.size(); i2++) {
                for (int i3 = 0; i3 < potLab.length; i3++) {
                    if (potLab[i3].isTerminalOnPotential(startTerminals.get(i2))) {
                        nrAnfangsKn[i2] = i3;
                    }
                }
            }

            // Endknoten:
            List<AbstractTerminal> endTerminals = elem.YOUT;
            int[] nrEndKn = new int[endTerminals.size()];  // dieses Array muss mit Knotennummern gefuellt werden
            for (int i2 = 0; i2 < endTerminals.size(); i2++) {
                for (int i3 = 0; i3 < potLab.length; i3++) {
                    if (potLab[i3].isTerminalOnPotential(endTerminals.get(i2))) {
                        nrEndKn[i2] = i3;
                    }
                }
            }

            // Zuweisung der KnotenNummern fuer LK-Netzliste (es gibt bei den LK-Elementen nur je einen Anfangs- u. End-Knoten):
            
            try {
                knotenX[i1] = nrAnfangsKn[0];
                knotenY[i1] = nrEndKn[0];
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }                
        
//        System.out.println("yyyyyyyyyyyyyyyyyyyyyyy repair!!!");
//        int[] saveKnotenX = new int[]{5, 2, 3, 4, 0, 5, 1};
//        int[] saveKnotenY = new int[]{6, 3, 2, 6, 1, 4, 0};
//        
//        knotenX = saveKnotenX;
//        knotenY = saveKnotenY;
        //------------------
        // Voraussetzung 1: Knoten sind durchgehend von Null weg aufsteigend numeriert
        // Voraussetzung 2: Spannungsquellen-Nummern sind von Eins weg durchgehend und aufsteigend numeriert
        // -->
        knotenMAX = 0;  // Anzahl der (verschiedenen) Knoten
        spgQuelleMAX = 0;  // Anzahl der (verschiedenen) Spannungsquellen
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            knotenMAX = Math.max(knotenMAX, knotenX[i1]);
            knotenMAX = Math.max(knotenMAX, knotenY[i1]);

            if (spgQuelleNr[i1] > spgQuelleMAX) {
                spgQuelleMAX = spgQuelleNr[i1];
            }
        }
    }

    // die hier verwendete Netzliste darf keine Subcircuits enthalten, diese muessen bereits in die NetzlisteLK integriert sein
    public static NetListLK ersetze_C_durch_Udc_Fuer_init(NetListLK nl) {
        // fuer die neue Netzliste:
        CircuitTyp[] i_typ = new CircuitTyp[nl.typ.length];
        int[] i_knotenX = new int[nl.knotenX.length];
        int[] i_knotenY = new int[nl.knotenY.length];
        double[][] i_parameter = new double[nl.parameter.length][];
        int[] i_spgQuelleNr = new int[nl.spgQuelleNr.length];
        int spgQuelleZaehler = 0;
        //--------------
        // vorhandene Spannungsquellen werden registriert, der spgQuelleZaehler laeuft dann von da weg fuer die C-Bauteile -->
        for (int i1 = 0; i1 < nl.typ.length; i1++) {
            if (nl.spgQuelleNr[i1] > spgQuelleZaehler) {
                spgQuelleZaehler = nl.spgQuelleNr[i1];
            }
        }
        spgQuelleZaehler++;
                
        //--------------
        for (int i1 = 0; i1 < nl.typ.length; i1++) {
            switch (nl.typ[i1]) {
                case LK_C:
                    // wird fuer die Initialisierung durch DC-SpgQuelle ersetzt -->
                    // aber nur, wenn die Anfangsbedingung uC(0) ungleich Null gesetzt wurde
                    if (nl.parameter[i1][1] != 0) {
                        i_typ[i1] = CircuitTyp.LK_U;
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[]{SourceType.QUELLE_DC_NEW, nl.parameter[i1][1], -1, -1, -1, -1, 0, 0, 0, 0};   // TypQuelle - uNmax - frequ - offset - phase - tastverh.
                        i_spgQuelleNr[i1] = spgQuelleZaehler;
                        spgQuelleZaehler++;
                    } else {
                        // bleibt unveraendert -->
                        i_typ[i1] = nl.typ[i1];
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[nl.parameter[i1].length];
                        for (int i2 = 0; i2 < nl.parameter[i1].length; i2++) {
                            i_parameter[i1][i2] = nl.parameter[i1][i2];
                        }
                        i_spgQuelleNr[i1] = nl.spgQuelleNr[i1];
                    }
                    break;
                case TH_CTH:
                    // wird fuer die Initialisierung durch DC-SpgQuelle ersetzt -->
                    // aber nur, wenn die Anfangsbedingung uC(0) ungleich Null gesetzt wurde
                    if (nl.parameter[i1][1] != 0) {
                        i_typ[i1] = CircuitTyp.TH_TEMP;
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[]{SourceType.QUELLE_DC_NEW, nl.parameter[i1][1], -1, -1, -1, -1, 0, 0, 0, 0};   // TypQuelle - uNmax - frequ - offset - phase - tastverh.
                        i_spgQuelleNr[i1] = spgQuelleZaehler;
                        spgQuelleZaehler++;
                    } else {
                        // bleibt unveraendert -->
                        i_typ[i1] = nl.typ[i1];
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[nl.parameter[i1].length];
                        for (int i2 = 0; i2 < nl.parameter[i1].length; i2++) {
                            i_parameter[i1][i2] = nl.parameter[i1][i2];
                        }
                        i_spgQuelleNr[i1] = nl.spgQuelleNr[i1];
                    }
                    break;
                default:
                    // bleibt unveraendert -->
                    i_typ[i1] = nl.typ[i1];
                    i_knotenX[i1] = nl.knotenX[i1];
                    i_knotenY[i1] = nl.knotenY[i1];
                    i_parameter[i1] = new double[nl.parameter[i1].length];
                    for (int i2 = 0; i2 < nl.parameter[i1].length; i2++) {
                        i_parameter[i1][i2] = nl.parameter[i1][i2];
                    }
                    i_spgQuelleNr[i1] = nl.spgQuelleNr[i1];
                    break;
            }
        }
        //--------------
        NetListLK nl_C_ersetzt = new NetListLK(i_typ, i_knotenX, i_knotenY, i_parameter, i_spgQuelleNr);
        nl_C_ersetzt._singularityEntries = nl._singularityEntries;
        
        nl_C_ersetzt.elements = nl.elements;
        nl_C_ersetzt.eLKneu = nl.eLKneu;
        nl_C_ersetzt.nodePairDirVoltContSrc = nl.nodePairDirVoltContSrc;
        //nl_C_ersetzt.ausgebenTest();
        //
        return nl_C_ersetzt;
    }

    private void definiere_magnetischeKopplungen_im_LK() {

        // M -->   []{ k - xL1(Koord.) - yL1(Koord.) - xL2(Koord.) - yL2(Koord.) - ID-Nr_L1 - ID-Nr_L2 {
        int maxLc = elementANZAHL;
        List<AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface>> kLc = new ArrayList<AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface>>();
        List<Double> kM = new ArrayList<Double>();

        // (1) Einlesen aller gekoppelten LK-Lc-Paare -->
        for (AbstractCircuitBlockInterface search : elements) {
            if (search instanceof MutualInductance) {
                double[] parM = search.getParameter();
                
                for (int i2 = 0; i2 < this.getElementANZAHL(); i2++) {
                    if (((ComponentCoupable) search).getComponentCoupling()._coupledElements[0].equals(elements[i2])) {
                        parM[5] = i2;
                    }
                    if (((ComponentCoupable) search).getComponentCoupling()._coupledElements[1].equals(elements[i2])) {
                        parM[6] = i2;
                    }
                }

                AbstractInductor inductor1 = (AbstractInductor) elements[(int) parM[5]];
                AbstractInductor inductor2 = (AbstractInductor) elements[(int) parM[6]];
                if (inductor1 != null && inductor2 != null) {
                    double kValue = parM[0];
                    double M = kValue * Math.sqrt(inductor1.getStartInductance() * inductor2.getStartInductance());
                    kM.add(M);
                    kLc.add(new AbstractMap.SimpleEntry(inductor1, inductor2));
                } //else one or two couplings are not defined
            }
        }

        //-----------------------------
        // (3) Aufbereitung: Welche gekoppelten LK-Lc-Elemente sieht das jeweilige LK-Lc-Element ?  -->
        // (a) zuerst eine Auflistung aller verschiedenen gekoppelten Lc-Elemente -->

        Set<AbstractCircuitBlockInterface> alleGekoppeltenLcSet = new LinkedHashSet<AbstractCircuitBlockInterface>();
        for (AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface> pair : kLc) {
            alleGekoppeltenLcSet.add(pair.getKey());
            alleGekoppeltenLcSet.add(pair.getValue());
        }
        alleGekoppeltenLc = alleGekoppeltenLcSet.toArray(new AbstractCircuitBlockInterface[0]);
        //
        // (b) jedem gelisteten Lc-Element werden die Kopplungspartner-Lc zugeteilt -->
        partnerLc = new AbstractCircuitBlockInterface[alleGekoppeltenLc.length][alleGekoppeltenLc.length];
        kopplungen = new double[alleGekoppeltenLc.length][alleGekoppeltenLc.length];
        int ix = 0;
        for (int i1 = 0; i1 < alleGekoppeltenLc.length; i1++) {
            ix = 0;
            AbstractCircuitBlockInterface[] partnerTemp = new AbstractCircuitBlockInterface[alleGekoppeltenLc.length];
            double[] kpTemp = new double[alleGekoppeltenLc.length];
            for (AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface> pair : kLc) {
                if (alleGekoppeltenLc[i1].equals(pair.getKey())) {
                    partnerTemp[ix] = pair.getValue();
                    kpTemp[ix] = kM.get(kLc.indexOf(pair));
                    ix++;

                }
                if (alleGekoppeltenLc[i1].equals(pair.getValue())) {
                    partnerTemp[ix] = pair.getKey();
                    kpTemp[ix] = kM.get(kLc.indexOf(pair));
                    ix++;
                }
            }

            AbstractCircuitBlockInterface[] partner = new AbstractCircuitBlockInterface[ix];
            for (int i2 = 0; i2 < ix; i2++) {
                partner[i2] = partnerTemp[i2];
            }
            partnerLc[i1] = partner;
            double[] kp = new double[ix];
            System.arraycopy(kpTemp, 0, kp, 0, kp.length);
            kopplungen[i1] = kp;
        }
    }

    public int findIndexFromLabelInSheet(final String searchLabel, final AbstractPotentialMeasurement measurement) {
        final CircuitSheet parentCircuitSheet = measurement.getParentCircuitSheet();

        for (int i = 0; i < eLKneu.length; i++) {
            AbstractCircuitBlockInterface block = eLKneu[i];

            if (block.getParentCircuitSheet() != parentCircuitSheet) {
                continue;
            }

            for (AbstractTerminal termIn : block.XIN) {
                if (termIn.getLabelObject().getLabelString().equals(searchLabel)) {
                    if (block._isEnabled.getValue() == Enabled.DISABLED) {
                        continue;
                    }
                    return knotenX[i];
                }
            }

            for (AbstractTerminal termOut : block.YOUT) {
                if (termOut.getLabelObject().getLabelString().equals(searchLabel)) {
                    if (block._isEnabled.getValue() == Enabled.DISABLED) {
                        continue;
                    }
                    return knotenY[i];
                }
            }
        }


        throw new RuntimeException("Error in measurement component " + measurement.getStringID()
                + "\nThe label reference \"" + searchLabel + "\" references to a disabled component.\n"
                + "Please disable the measurement component " + measurement.getStringID() + " to run\n"
                + "the simulation. Aborting.");

    }
}
