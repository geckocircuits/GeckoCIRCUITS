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
package ch.technokrat.gecko.geckocircuits.allg;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Locale;
import java.io.Serializable;

public class TechFormat implements Serializable {

    //------------------------------
    /*
    public static final String FORMAT_SCIENCE_K1= "#.#E0";
    public static final String FORMAT_SCIENCE_K2= "#.##E0";
    public static final String FORMAT_SCIENCE_K3= "#.###E0";
    public static final String FORMAT_ENG_K1= "###.#E0";
    public static final String FORMAT_ENG_K2= "###.##E0";
    public static final String FORMAT_ENG_K3= "###.###E0";
    public static final String FORMAT_NOEXP_K0= "#.";
    public static final String FORMAT_NOEXP_K1= "#.#";
    public static final String FORMAT_NOEXP_K2= "#.##";
    public static final String FORMAT_NOEXP_K3= "#.###";
     */
    public static final String FORMAT_AUTO = "AUTO";  // automatische Anpassung an die Groesse der zu formatierenden Zahl
    //
    private int anzDigits = 4;  // default
    //
    private DecimalFormat df;
    //------------------------------
    private String[] abk = new String[]{"p", "n", "u", "m", "k", "M"};  // moegliche technische "Zahlen"eingaben
    private int[] hoch = new int[]{-12, -9, -6, -3, +3, +6};   // zugehoerige Exponenten
    //------------------------------
    private static final double ln10 = Math.log(10);
    private static final double DUMMY = -1.111222333444555e-77;
    //------------------------------

    public TechFormat() {
        df = (DecimalFormat) (NumberFormat.getNumberInstance(new Locale("en", "US")));
        //this.testAusgabe2();
    }

    public void setMaximumDigits(int anzDigits) {
        this.anzDigits = anzDigits;
    }
    private double xfmax = 0, fmax = 0;  // zum Testen des Algorithmus in formatENG()

    
    
    public String formatENG(double x, int anzDigits) {
            
        if (Math.abs(x) > Double.MAX_VALUE - 1) {
            return "" + x;
        }

        if (x != x) {
            return "" + x;
        }
        //-----------------------
        double z = Math.abs(x);
        int anz3erPakete = 0;
        //-----------------------
        if (z > 1) {
            while (z > 1e3) {
                z /= 1e3;
                anz3erPakete++;
            }
        } else if ((z < 1) && (z != 0)) {
            while (z < 1) {
                z *= 1e3;
                anz3erPakete++;
            }
        }
        int z1 = (int) z;  // vor der Kommastelle
        double hz = this.exp10(anzDigits);
        int z2 = (int) Math.round((z - z1) * hz);  // nach der Kommastelle
        if (z2 / hz == 1) {
            z1 += 1;
            z2 = 0;
        }  // numerischer Ueberlauf
        int anzVorgestellteNullen = anzDigits - (int) (Math.log(z2) / ln10) - 1;
        if (z2 == 1000) {
            anzVorgestellteNullen--;  // notwendige Korrektur wegen Rundungsfehler bei  Math.log(z2)/ln10
        }
        if (z2 == 0) {
            anzVorgestellteNullen = anzDigits;
        }
        StringBuffer erg = new StringBuffer();
        if (x < 0) {
            erg.append("-");
        }
        erg.append(z1);
        if (z2 != 0) {
            erg.append(".");
            for (int i1 = 0; i1 < anzVorgestellteNullen; i1++) {
                erg.append("0");
            }
            while (z2 % 10 == 0) {
                z2 /= 10;
            }
            erg.append(z2);
        }
        if (anz3erPakete != 0) {
            erg.append("e");
            if (Math.abs(x) < 1) {
                erg.append("-" + (3 * anz3erPakete));
            } else {
                erg.append("" + (3 * anz3erPakete));
            }
        }
        //-----------------------
        double fehler = (this.parseT(erg.toString()) - x) / x * 1e2;  // in [%]
        if (x == 0) {
            fehler = this.parseT(erg.toString()) * 1e2;
        }
        if (Math.abs(fehler) > fmax) {
            fmax = Math.abs(fehler);
            xfmax = x;
        }
        //System.out.println("x="+x+"    anzDigits="+anzDigits+"    --> "+erg+"     "+fehler+"%");
        return erg.toString();
    }

    // (1) Zahl (double) wird in einen String zur GUI-Ausgabe umgewandelt
    //
    public String formatT(double x, String pattern) {
        if (pattern.equals(TechFormat.FORMAT_AUTO)) {
            /*
            // AUTO-Strategie: Zahlen mit Endung 'E-3', 'E0', 'E3' werden nicht in exponetieller Schreibweise dargestellt,
            // alle anderen in Ingenieur-maessiger Darstellung (3er-Packete)
            df.applyPattern("#E0");
            String s= df.format(x);
            System.out.println("x= "+x+"  /  s= "+s);
            if (s.endsWith("E0"))       df.applyPattern("#.#####");
            else if (s.endsWith("E3"))  df.applyPattern("#.##");
            else if (s.endsWith("E-3")) df.applyPattern("#.########");
            else df.applyPattern("0.###E0");
            s= df.format(x);
            System.out.println("sneu= "+s);
            return s;
             */
            return this.formatENG(x, anzDigits);
        } else {
            df.applyPattern(pattern);
            StringBuffer sb = new StringBuffer(df.format(x));
            for (int i1 = 0; i1 < sb.length(); i1++) {
                if (sb.charAt(i1) == 'E') {
                    sb.setCharAt(i1, 'e');  // 'e' ist optisch ansprechender als 'E'
                }
            }
            return sb.toString();
        }
    }

    // (2) String wird eingelesen und in eine Zahl (double) umgewandelt
    // optional fuer die Eingabe:
    // 2k --> 2000;  2k2 --> 2200;  2m --> 0.002;  2m2 --> 2.2e-3;  2u --> 2e-6;  2u4 --> 2.4e-6;  7n --> 7e-9;  usw.
    //
    public double parseT(String s) {
        try {
            //-----------------------
            // (1) Repraesentiert der Eingabe-String eine korrekt eingegebene 'double'-Zahl? -->
            double x = (new Double(s)).doubleValue();
            return x;
        } catch (Exception e1) {
            //-----------------------
            // (2) Enthaelt der Eingabe-String ungueltige Eingabe-Zeichen? -->
            String s2 = s.trim();
            char[] sc = s2.toCharArray();
            int anzTechZeichen = 0, anzKomma = 0;
            for (int i1 = 0; i1 < sc.length; i1++) {
                if ((sc[i1] != '0') && (sc[i1] != '1') && (sc[i1] != '2') && (sc[i1] != '3') && (sc[i1] != '4') && (sc[i1] != '5') && (sc[i1] != '6') && (sc[i1] != '7') && (sc[i1] != '8') && (sc[i1] != '9')
                        && (sc[i1] != 'e') && (sc[i1] != 'E') && (sc[i1] != '+') && (sc[i1] != '-') && (sc[i1] != '.')
                        && (sc[i1] != 'p') && (sc[i1] != 'n') && (sc[i1] != 'u') && (sc[i1] != 'm') && (sc[i1] != 'k') && (sc[i1] != 'M')) {
                    throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'  --> unzulaessiges Zeichen");
                }
                if (((sc[i1] == '+') || (sc[i1] == '-')) && (i1 != 0)) {
                    // '+' und '-' stehen nicht am Beginn --> stehen sie vor dem Exponenten? (waere OK) -->
                    if ((sc[i1 - 1] != 'e') && (sc[i1 - 1] != 'E')) {
                        throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'  --> '+' bzw '-' an unzulaessiger Stelle");
                    }
                }
                if ((sc[i1] == 'e') || (sc[i1] == 'E') || (sc[i1] == 'p') || (sc[i1] == 'n') || (sc[i1] == 'u') || (sc[i1] == 'm') || (sc[i1] == 'k') || (sc[i1] == 'M')) {
                    anzTechZeichen++;
                }
                if (sc[i1] == '.') {
                    anzKomma++;
                }
            }
            if ((sc[sc.length - 1] == 'e') || (sc[sc.length - 1] == 'E')) {
                throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'  --> kein Exponent definiert");
            }
            if (anzTechZeichen > 1) {
                throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'  --> mehr als ein 'Sonder'zeichen");
            }
            if (anzKomma > 1) {
                throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'  --> mehr als ein Komma");
            }
            //
            // Annahme: alle Falscheingaben wurden eliminiert - ab jetzt keine Exception mehr moeglich
            //-----------------------
            // (3) Zeichen sind OK, wir parsen entsprechend den technischen Eingaben definiert in abk[]  zB. -->
            // 2k --> 2000;  2k2 --> 2200;  2m --> 0.002;  2m2 --> 2.2e-3;  2u --> 2e-6;  2u4 --> 2.4e-6;  7n --> 7e-9;  usw.
            for (int i1 = 0; i1 < abk.length; i1++) {
                int pos = s2.indexOf(abk[i1]);
                if (pos != -1) {
                    String sub1 = s2.substring(0, pos);
                    String sub2 = s2.substring(pos + 1);
                    double z1 = 0, z2 = 0;
                    try {
                        z1 = (new Double(sub1)).doubleValue();
                    } catch (Exception ex1) {
                        break;
                    }
                    try {
                        z2 = (new Double(sub2)).doubleValue();
                    } catch (Exception ex1) {
                        if (sub2.length() != 0) {
                            break;
                        }
                    }
                    double x = (z1 + 0.1 * z2) * this.exp10(hoch[i1]);
                    return x;
                }
            }
            //-----------------------
            // (4) hier sollten wir eigentlich gar nicht hingelangen -->
            throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'  --> unbekanntes Problem [9324ubf902]");
        }
    }

    //===========================================================================
    public String format(String eingabe, int maxNachKomma) {
        double erg = this.fmt(eingabe);
        if (erg == DUMMY) {
            throw new NumberFormatException("Ungueltiges Zahlenformat in 'TechFormat'");
        }
        return this.fmt(erg, maxNachKomma);
    }

    public String fmt(double w, int maxNachKomma) {
        maxNachKomma += 2;
        String st = "" + w;
        if (st.length() > maxNachKomma) {
            int pos1 = st.indexOf(".");
            String sub1 = st.substring(0, pos1);
            int pos3;
            int pos2 = st.indexOf("E");
            if ((pos2 == -1) || (pos2 > maxNachKomma)) {
                pos3 = maxNachKomma;
            } else {
                pos3 = pos2;
            }
            String sub2 = st.substring(pos1 + 1, pos3);
            String sub3 = "";
            if (pos2 != -1) {
                sub3 = "e" + st.substring(pos2 + 1);
            }
            st = sub1 + "." + sub2 + sub3;
        }
        return st;
    }

    public double fmt(String st) {
        // 2k --> 2000;  2k2 --> 2200;  2m --> 0.002;  2m2 --> 2.2e-3;  2u --> 2e-6;  2u4 --> 2.4e-6;  7n --> 7e-9;  usw.
        double erg = DUMMY;
        try {
            erg = (new Double(st)).doubleValue();
        } catch (Exception e) {
            for (int i1 = 0; i1 < abk.length; i1++) {
                int pos = st.indexOf(abk[i1]);
                if (pos != -1) {
                    String sub1 = st.substring(0, pos);
                    String sub2 = st.substring(pos + 1);
                    double z1 = 0, z2 = 0;
                    try {
                        z1 = (new Double(sub1)).doubleValue();
                    } catch (Exception e1) {
                        break;
                    }
                    try {
                        z2 = (new Double(sub2)).doubleValue();
                    } catch (Exception e1) {
                        if (sub2.length() != 0) {
                            break;
                        }
                    }
                    erg = (z1 + 0.1 * z2) * this.exp10(hoch[i1]);
                    return erg;
                }
            }
        }
        if (erg == DUMMY) {
            throw new NumberFormatException();
        }
        return erg;
    }

    private double exp10(int n) {
        if (n == 0) {
            return 1;
        }
        double erg = 1;
        if (n > 0) {
            erg = 10;
            for (int i1 = 1; i1 < n; i1++) {
                erg *= 10;
            }
        } else if (n < 0) {
            for (int i1 = 0; i1 > n; i1--) {
                erg /= 10;
            }
        }
        return erg;
    }

    public void testAusgabe() {
        //----------------------------
        /*
        this.formatENG(12.999999, 5);
        this.formatENG(12.99, 1);
        this.formatENG(12.99, 2);
        this.formatENG(12.99, 3);
        this.formatENG(12.9, 2);
         */
        /*
        this.formatENG(1010003.0516670677, anzDigits);
        for (int i1=0;  i1<-2000000;  i1++) {
        double basis= 2*(Math.random()-0.5);
        int potenz= (int)(2*(Math.random()-0.5) *15);
        this.formatENG(Math.pow(10,potenz)*basis, 5);
        }
        System.out.println();  System.out.println("fmax="+fmax+"   @ x="+xfmax);
         */
        //----------------------------
        // Testen 'parseT()' -->
        String[] s = new String[]{
            "1234.676", "1,234.676", "1'234.676", "12'3'4.676", "1234.6m76", "12e4", "-98.55e45", "0.67E-67", "123n4.676", "1234n.676", "1234.676e", "1234.676EE",
            "2e.", "2e+.", "2e+0.67", "e3434", ".e3", "2k34", "2k2", "13m", "300", "7.1"
        };
        System.out.println("Testen 'parseT()' -->");
        for (int i1 = 0; i1 < s.length; i1++) {
            try {
                System.out.println(i1 + "\t" + this.parseT(s[i1]) + "\t\t" + s[i1]);
            } catch (Exception e) {
                System.out.println(e + "   " + s[i1]);
            }
        }
        System.out.println();
        //----------------------------
        // Teste 'formatT()' -->
        System.out.println("Testen 'formatT()' -->");
        /*
        String[] test= new String[]{"12m", "12m2m", "22i", "192.565656mu"};
        for (int i1=0;  i1<test.length;  i1++)
        System.out.println(test[i1]+"\t\t"+this.fmt(test[i1])+"\t\t"+this.fmt(this.fmt(test[i1]),4));
        System.out.println("-------------------------");
        System.out.println(this.format("1.5um", 3));
        this.applyPattern("##0.##E0");
         */
        double[] data = new double[]{
            0.000000000000123456, 0.00000000000123456, 0.0000000000123456, 0.000000000123456,
            0.00000000123456, 0.0000000123456, 0.000000123456, 0.00000123456, 0.0000123456,
            0.000123456, 0.00123456, 0.0123456, 0.123456, 1.23456, 12.3456, 123.456, 1234.56,
            12345.6, 123456, 1234560, 12345600, 123456000, 1234560000, 300, 7.1, 7.11, 7.12, 0.071
        };
        for (int i1 = 0; i1 < data.length; i1++) {
            System.out.println(this.formatT(data[i1], TechFormat.FORMAT_AUTO) + "\t\t" + data[i1]);
        }
        //----------------------------
    }

    public void testAusgabe2() {
        //----------------------------
        // Teste 'formatT()' -->
        System.out.println("Random-Testen 'formatT()' -->");
        for (int i1 = 0; i1 < 999999; i1++) {
            int e1 = (int) (1 + 8 * Math.random());  // [-1 ... +8] --> Anzahl Stellen
            int e2 = (int) (Math.pow(10, e1) * Math.random());  // ganzzahlig, maximal 8-stellig
            int e3 = (int) (31 * (Math.random() - 0.5));  // [-15 ... +15]
            double data = e2 * Math.pow(10, e3);
            double q1 = (new Double(this.formatT(data, TechFormat.FORMAT_AUTO))).doubleValue();
            double ratio = q1 / data;
            if (ratio > 1) {
                ratio = data / q1;
            }
            if (ratio < 0.9999) {
                System.out.println(data + "\t\t" + this.formatT(data, TechFormat.FORMAT_AUTO) + "\t\tratio= " + ratio);
            }
        }
        //----------------------------
    }
}
