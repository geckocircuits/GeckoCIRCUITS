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

import ch.technokrat.gecko.geckocircuits.newscope.LineSettable;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JTextField;
import java.text.NumberFormat;

public class FormatJTextField extends JTextField {

    public static final double IS_VARIABLE = -1e95;  // GeckoOPTIMIZER
    private boolean _numberOK;
    private String _techFormatPat = TechFormat.FORMAT_AUTO;  // default
    private final TechFormat _cf = new TechFormat();
    private final NumberFormat _nf = NumberFormat.getNumberInstance();
    private static final int OFFSET2 = 5;
    private static final int OFFSET1 = 2;
    private LineSettable _lineSettable;
    
    public FormatJTextField() {
        super();
        
    }

    public FormatJTextField(final String string) {
        super(string);
    }

    public FormatJTextField(final double value) {
        super();
        this.setNumberToField(value);
    }

    public FormatJTextField(final double value, final int maxFracDigits) {
        super();
        this.setNumberToField(value, maxFracDigits);
    }

    public FormatJTextField(final double value, final String pattern) {
        super();
        this._techFormatPat = pattern;
        this.setNumberToField(value);
    }

    public void setTechFormatPattern(final String pattern) {
        this._techFormatPat = pattern;
    }

    public void setMaximumDigits(final int anzDigits) {
        _cf.setMaximumDigits(anzDigits);
    }

    public boolean isNumberOK() {
        return _numberOK;
    }

    public void setState(final boolean isOn) {
        this.setEditable(isOn);
        this.setEnabled(isOn);
    }

    public void setNumberToField(final double value) {        
        final String string = _cf.formatT(value, _techFormatPat);
        this.setText(string);
    }

    public void setNumberToField(final double value, final int maxFractionDigits) {
        _nf.setMaximumFractionDigits(maxFractionDigits);
        final String string = _nf.format(value);
        this.setText(string);
    }

    public double getNumberFromField() {
        String zValue = this.getText();
        if(zValue.isEmpty()) {
            return 0;
        }
        if (zValue.charAt(0) == '$') {
            return FormatJTextField.IS_VARIABLE;  // GeckoOPTIMIZER
        }
        try {
            final double xValue = _cf.parseT(zValue);
            _numberOK = true;
            this.setForeground(Color.black);
            return xValue;
        } catch (Exception e) {
            try {
                int index2 = 0;
                for (int i1 = 0; i1 < zValue.length(); i1++) {
                    if (zValue.charAt(i1) != '\'') {
                        index2++;
                    }
                }
                char[] zChar = new char[index2];
                index2 = 0;
                for (int i1 = 0; i1 < zValue.length(); i1++) {
                    if (zValue.charAt(i1) != '\'') {
                        zChar[index2] = zValue.charAt(i1);
                        index2++;
                    }
                }
                zValue = new String(zChar);
                //----
                final double xValue = _cf.parseT(zValue);
                _numberOK = true;
                this.setForeground(Color.black);
                return xValue;
            } catch (Exception e2) {
                _numberOK = false;
                this.setForeground(Color.red);
                e2.printStackTrace();                
                throw new RuntimeException(e2.getMessage());                                
            }
        }
    }
    

    @Override
    public void paint(final Graphics graphics) {
        final Graphics2D g2d = (Graphics2D) graphics;
        super.paint(g2d);

        if (_lineSettable != null && !getText().equals("-")) {
            if(_lineSettable.getTransparency() != 0) {
                final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        _lineSettable.getTransparency());
                g2d.setComposite(alphaComposite);
            }

            _lineSettable.getStroke().setStrokeStyle(g2d);
            g2d.setColor(_lineSettable.getColor().getJavaColor());
            
            // jetzt die Linie ziehen:
            g2d.drawRect(OFFSET1, OFFSET1, getWidth() - OFFSET2, getHeight() - OFFSET2);
            
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, _lineSettable.getTransparency());
            alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
            g2d.setComposite(alphaComposite);

        }

    }

    public void setLineSettable(final LineSettable lineSettable) {
        _lineSettable = lineSettable;
    }
        
}
