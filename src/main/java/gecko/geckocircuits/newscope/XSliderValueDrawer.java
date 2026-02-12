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
package gecko.geckocircuits.newscope;

import gecko.geckocircuits.allg.TechFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author andy
 */
public final class XSliderValueDrawer extends JPanel {

    private static final TechFormat TECH_FORMAT = new TechFormat();
    private static final int NUM_DIGITS = 6;
    private static final int FONT_SIZE = 9;
    private static final Font DRAW_FONT = new Font("Arial", Font.PLAIN, FONT_SIZE);
    private static final String UNICODE_DELTA = "\u0394";
    private static final String UNICODE_INFINITY = "\u221E";
    private final JLabel _firstTextField = new JLabel();
    private final JLabel _secondTextField = new JLabel();
    private final JLabel _thirdTextField = new JLabel();
    private final JLabel _fourthTextField = new JLabel();
    
    private static final String X_NAME_DEFAULT = "t = ";
    private static final int DRAWER_HEIGHT = 11;    
    private static final String X_NAME_INV_DEFAULT = "f = ";
    
    String xName = X_NAME_DEFAULT;
    String xNameInv = X_NAME_INV_DEFAULT;
    
    private static final int DEF_X_DIM = 100;
    private boolean _isVisible = false;
    
    public XSliderValueDrawer() {
        super();
        this.setLayout(new FlowLayout(FlowLayout.CENTER, FONT_SIZE, 1));
        insertTextField(_firstTextField, Color.red);
        insertTextField(_secondTextField, Color.green);
        insertTextField(_thirdTextField, Color.black);
        insertTextField(_fourthTextField, Color.black);
        setOpaque(false);        
        setMinimumSize(new Dimension(0, DRAWER_HEIGHT));        
    }

    /*
     * values is a 2-array with red and green slider value
     */
    public void setDisplayRanges(final double[] values) {

        _firstTextField.setText(Double.toString(values[0]));
        _secondTextField.setText(Double.toString(values[1]));


        TECH_FORMAT.setMaximumDigits(NUM_DIGITS);
        _firstTextField.setText(xName + TECH_FORMAT.formatT(values[0], TechFormat.FORMAT_AUTO));
        _secondTextField.setText(xName + TECH_FORMAT.formatT(values[1], TechFormat.FORMAT_AUTO));

        if (values[1] > 0) {
            final float diff = (float) (values[1] - values[0]);
            final String diffString = UNICODE_DELTA + xName + TECH_FORMAT.formatT(diff, TechFormat.FORMAT_AUTO);
            _thirdTextField.setText(diffString);


            String fourthString;
            if (diff == 0) {
                fourthString = xNameInv + UNICODE_INFINITY;
            } else {
                final String formatString = TECH_FORMAT.formatT(Math.abs(1 / diff), TechFormat.FORMAT_AUTO);
                fourthString = xNameInv + formatString;
            }

            _fourthTextField.setText(fourthString);
        }
    }

    private void insertTextField(final JLabel jLabel, final Color textColor) {
        jLabel.setFont(DRAW_FONT);
        jLabel.setPreferredSize(new Dimension(DEF_X_DIM, FONT_SIZE + 1));
        jLabel.setForeground(textColor);
        jLabel.setBorder(null);
        this.add(jLabel);
    }
    
    @Override
    public boolean isVisible() {
        return _isVisible;
    }
    
    @Override
    public void setVisible(final boolean value) {
        _isVisible = value;
    }
    
}
