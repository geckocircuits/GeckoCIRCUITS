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
    
    public static final Color farbeTextLinie = Color.lightGray;  // Verbindungslinie vom Textfeld zum zugehoerigen Element
    public static final Color farbeInBearbeitungLK = Color.gray;
    public static final Color farbeFertigElementLK = Color.decode("0x00008b");
    public static final Color farbeFertigVerbindungLK = Color.blue;
    public static final Color farbeLabelLK = Color.decode("0x00008b");
    public static final Color farbeParallelLK = Color.decode("0xadd8e6");  // falls eine parallele Linie in Verbindung.zeichne() gezogen wird zur besseren Visualisierung
    public static final Color farbeElementLKHintergrund = Color.decode("0xccccff");
    public static final Color farbeElementRELFOREGROUND = Color.decode("#886A08");
    public static final Color farbeElementRELBACKGROUND = Color.decode("#f5f5dc");
    public static final Color farbeInBearbeitungCONTROL = Color.gray;
    public static final Color farbeFertigElementCONTROL = Color.decode("0x006400");
    public static final Color farbeFertigVerbindungCONTROL = Color.green;
    public static final Color farbeLabelCONTROL = Color.decode("0x006400");
    public static final Color farbeParallelCONTROL = Color.decode("0x90ee90");
    public static final Color farbeElementCONTROLHintergrund = Color.decode("0xaaffaa");
    public static final Color farbeEXTERNAL_TERMINAL = Color.magenta;
    public static final Color farbeInBearbeitungTHERM = Color.gray;
    public static final Color farbeFertigElementTHERM = Color.decode("0x8b0000");
    public static final Color farbeFertigElementRELUCTANCE = farbeElementRELFOREGROUND;
    public static final Color farbeFertigVerbindungTHERM = Color.red;
    public static final Color farbeLabelTHERM = Color.red;
    public static final Color farbeParallelTHERM = Color.decode("0xffa07a");
    public static final Color farbeElementTHERMHintergrund = Color.decode("0xffd7d7");
    public static final Color farbeZoomRechteck = Color.red;
    public static final Color farbeOPT = Color.magenta;
    public static final Color farbeConnectorTestMode = Color.magenta;
    public static final Color farbeConnectorTestModeInternal = Color.yellow;
    
    
}
