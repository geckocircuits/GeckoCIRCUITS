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

import java.awt.Color;

/**
 *
 * @author andy
 */
public class GlobalColors {
    public static final Color LAB_COLOR_DIALOG_1 = Color.black;
    public static final Color farbeGecko = Color.decode("0x99bb33");
    
    public static Color farbeTextLinie = Color.lightGray;  // Verbindungslinie vom Textfeld zum zugehoerigen Element
    public static Color farbeInBearbeitungLK = Color.gray;
    public static Color farbeFertigElementLK = Color.decode("0x00008b");
    public static Color farbeFertigVerbindungLK = Color.blue;
    public static Color farbeLabelLK = Color.decode("0x00008b");
    public static Color farbeParallelLK = Color.decode("0xadd8e6");  // falls eine parallele Linie in Verbindung.zeichne() gezogen wird zur besseren Visualisierung
    public static Color farbeElementLKHintergrund = Color.decode("0xccccff");
    public static Color farbeElementRELFOREGROUND = Color.decode("#886A08");    
    public static Color farbeElementRELBACKGROUND = Color.decode("#f5f5dc");
    public static Color farbeInBearbeitungCONTROL = Color.gray;
    public static Color farbeFertigElementCONTROL = Color.decode("0x006400");
    public static Color farbeFertigVerbindungCONTROL = Color.green;
    public static Color farbeLabelCONTROL = Color.decode("0x006400");
    public static Color farbeParallelCONTROL = Color.decode("0x90ee90");
    public static Color farbeElementCONTROLHintergrund = Color.decode("0xaaffaa");
    public static Color farbeEXTERNAL_TERMINAL = Color.magenta;
    public static Color farbeInBearbeitungTHERM = Color.gray;
    public static Color farbeFertigElementTHERM = Color.decode("0x8b0000");
    public static Color farbeFertigElementRELUCTANCE = farbeElementRELFOREGROUND;
    public static Color farbeFertigVerbindungTHERM = Color.red;
    public static Color farbeLabelTHERM = Color.red;
    public static Color farbeParallelTHERM = Color.decode("0xffa07a");
    public static Color farbeElementTHERMHintergrund = Color.decode("0xffd7d7");
    public static Color farbeZoomRechteck = Color.red;
    public static Color farbeOPT = Color.magenta;
    public static Color farbeConnectorTestMode = Color.magenta;
    public static Color farbeConnectorTestModeInternal = Color.yellow;
    
    
}
