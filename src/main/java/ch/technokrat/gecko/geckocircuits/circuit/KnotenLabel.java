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


import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle; 
import java.awt.Graphics2D;
import java.awt.BasicStroke; 
import java.awt.font.FontRenderContext;
import java.io.Serializable;




public class KnotenLabel implements Serializable {

    //----------------------------------
    private String labelTxt;
    private int xTxt, yTxt;      // Pixel-Koord. vom Text
    private int xAnker, yAnker;  // ScematicEntry-Koord. vom Ankerpunkt, d.h. wohin die graue Linie zielt 
    private int x1click, y1click, x2click, y2click;  // definiert Klickbereich in Pixel 
    private Font font;
    private Color fb; 
    private boolean visible=false; 
    //----------------------------------

    
    public void setKoordTxt (int xT, int yT) { this.xTxt=xT;   this.yTxt=yT; }
    public void setKoordAnker (int xA, int yA) { this.xAnker=xA;   this.yAnker=yA; }
    public void setText (String txt) { this.labelTxt=txt; } 
    public void setVisible (boolean visible) { this.visible=visible; }

    

    public KnotenLabel () {}

    public KnotenLabel (String[] ascii) {
        this.importASCII(ascii);
    }


    public boolean istAngeklickt (int xPix, int yPix) {
        if ((x1click<xPix)&&(xPix<x2click)&&(y1click<yPix)&&(yPix<y2click)) return true; else return false; 
    }

    
    public void zeichne (Graphics g, Color f1, int dpix) {
        if (!visible) return; 
        //-----
        //g.setFont(font);
        g.setColor(f1); 
        g.drawString(labelTxt, xTxt,yTxt); 
        g.setColor(Color.lightGray); 
        g.drawLine((int)(dpix*xAnker), (int)(dpix*yAnker), xTxt, yTxt); 
        //-----
        FontRenderContext frc= ((Graphics2D)g).getFontRenderContext();
        Rectangle rect= g.getFont().getStringBounds(labelTxt, frc).getBounds(); 
        x1click= xTxt+rect.x; 
        y1click= yTxt+rect.y; 
        x2click= x1click +rect.width; 
        y2click= y1click +rect.height; 
        //System.out.println(x1click+"   "+x2click+"   "+y1click+"   "+y2click); 
        g.setColor(Color.magenta); 
        g.drawRect(x1click,y1click, x2click-x1click,y2click-y1click);
        //-----
    }



    // zum Speichern im ASCII-Format (anstatt als Object-Stream) -->
    public void exportASCII (StringBuffer ascii) {
        ascii.append("<Verbindung>");
        //------------------
        /*
        DatenSpeicher.appendAsString(ascii.append("\nlabel"), label);
        DatenSpeicher.appendAsString(ascii.append("\nxLabel"), xLabel);
        DatenSpeicher.appendAsString(ascii.append("\nyLabel"), yLabel);
        */
        //-----------
        ascii.append(new StringBuffer("\n<\\Verbindung>\n"));
        //------------------
    }




    public void importASCII (final String[] ascii) {
        
    }



}





