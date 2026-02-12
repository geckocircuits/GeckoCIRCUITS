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
package gecko.geckocircuits.allg;

import java.awt.Font;

/**
 *
 * @author andy
 */
public class GlobalFonts {
    private GlobalFonts() {
        // pure static class!
    }
    
    public static final Font foAUSWAHL = new Font("Arial", Font.PLAIN, 12);
    public static final Font foGRAFER = new Font("Arial", Font.PLAIN, 11);    
    // einheitlicher Font fuer die Dialoge -->
    //
    public static final Font LAB_FONT_DIALOG_1 = new Font("Arial", Font.PLAIN, 12);
       
    public static final Font FORMEL_DIALOG_GROSS = new Font("Times New Roman", Font.ITALIC, 16);
    public static final Font FORMEL_DIALOG_KLEIN = new Font("Times New Roman", Font.ITALIC, 12);
    
}
