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

import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import gecko.geckocircuits.allg.FormatJTextField;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

/**
 *
 * @author andy
 */
public final class JPanelDialogRange extends JPanel {

    private final JRadioButton jRadButScopeRange, jRadButtDefRange, jrb3;
    private final FormatJTextField _rngSc1, _rngSc2, _rngDf1, _rngDf2, _rngSl1, _rngSl2;  // Angaben Zeitbereiche
    private final double _xScope1, _xScope2;
    private double _xSlider1, _xSlider2;  // vom Slider-Paar definierter Rechenbereich
    private double _xDef1, _xDef2;  // von Hand definierter Rechenbereich
    private static final int MAX_FRACT_DIGITS = 9;
    private static final int NUM_COLS = 9;
    private static final int GRID_ROWS = 6;
    private final transient KeyListener _xDef1KeyListener = new KeyListener() {

        @Override
        public void keyTyped(final KeyEvent keyEvent) {
            // nothing to do here!
        }

        @Override
        public void keyPressed(final KeyEvent keyEvent) {
            // nothing to do here!
        }

        @Override
        public void keyReleased(final KeyEvent keyEvent) {
            try {
                _xDef1 = _rngDf1.getNumberFromField(); 
                notifyActionListener();
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
    };
    private ActionListener _listener;
    
    void registerActionListener(ActionListener listener) {
        _listener = listener;
    }
    
    private void notifyActionListener() {
        if(_listener != null) {
            _listener.actionPerformed(null);
        }
    }
    
    private final transient KeyListener _xDef2KeyListener = new KeyListener() {

        @Override
        public void keyTyped(final KeyEvent keyEvent) {
            // nothing to do here
        }

        @Override
        public void keyPressed(final KeyEvent keyEvent) {
            // nothing to do here
        }

        @Override
        public void keyReleased(final KeyEvent keyEvent) {
            try {
                _xDef2 = _rngDf2.getNumberFromField(); 
                notifyActionListener();
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
    };

    public JPanelDialogRange(final AbstractDataContainer worksheet, final double[] sliderValues) {
        super();
        setLayout(new GridLayout(GRID_ROWS, 1));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Set range",
                TitledBorder.LEFT, TitledBorder.TOP));

        // Default-Bereichsgrenzen:
        _xScope1 = worksheet.getTimeValue(0, 0);  
        int index2 = worksheet.getMaximumTimeIndex(0) - 1;
        double xScope2value = worksheet.getTimeValue(index2, 0);
        while ((xScope2value == 0) && (index2 > 0)) {
            xScope2value = worksheet.getTimeValue(index2, 0);
            index2--;
        }

        _xScope2 = xScope2value;

        // die anderen beiden Einstellungen werden vorerst genauso gesetzt:
        _xDef1 = _xScope1;
        _xDef2 = _xScope2;
        _xSlider1 = _xScope1;
        _xSlider2 = _xScope2;

        final ButtonGroup group = new ButtonGroup();
        jRadButScopeRange = new JRadioButton("Data-Range");
        jRadButtDefRange = new JRadioButton("Define Range");
        jrb3 = new JRadioButton("Slider-Range");

        group.add(jRadButScopeRange);
        group.add(jRadButtDefRange);
        group.add(jrb3);
        //
        this.add(jRadButScopeRange);
        final JPanel jpR1 = new JPanel();

        _rngSc1 = new FormatJTextField(_xScope1, MAX_FRACT_DIGITS);
        _rngSc1.setColumns(NUM_COLS);
        _rngSc2 = new FormatJTextField(_xScope2, MAX_FRACT_DIGITS);
        _rngSc2.setColumns(NUM_COLS);
        jpR1.add(_rngSc1);
        jpR1.add(_rngSc2);
        this.add(jpR1);
        this.add(jRadButtDefRange);

        final JPanel jpR2 = new JPanel();
        _rngDf1 = new FormatJTextField(_xDef1);
        _rngDf1.setColumns(NUM_COLS);

        _rngDf2 = new FormatJTextField(_xDef2);
        _rngDf2.setColumns(NUM_COLS);

        jpR2.add(_rngDf1);
        jpR2.add(_rngDf2);
        this.add(jpR2);

        setSliderValues(sliderValues);

        this.add(jrb3);
        final JPanel jpR3 = new JPanel();
        _rngSl1 = new FormatJTextField(_xSlider1);
        _rngSl1.setColumns(NUM_COLS);
        _rngSl2 = new FormatJTextField(_xSlider2);
        _rngSl2.setColumns(NUM_COLS);
        jpR3.add(_rngSl1);
        jpR3.add(_rngSl2);
        this.add(jpR3);

        jRadButScopeRange.setSelected(true);
        _rngSc1.setEditable(false);
        _rngSc2.setEditable(false);
        _rngDf1.setEditable(true);
        _rngDf2.setEditable(true);
        _rngSl1.setEditable(false);
        _rngSl2.setEditable(false);
        //
        _rngSc1.setEnabled(true);
        _rngSc2.setEnabled(true);
        _rngDf1.setEnabled(false);
        _rngDf2.setEnabled(false);
        _rngSl1.setEnabled(false);
        _rngSl2.setEnabled(false);


        setActionListeners();
    }


    private void setActionListeners() {

        jRadButScopeRange.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _rngSc1.setEnabled(true);
                _rngSc2.setEnabled(true);
                _rngSl1.setEnabled(false);
                _rngSl2.setEnabled(false);
                _rngDf1.setEnabled(false);
                _rngDf2.setEnabled(false);
                notifyActionListener();

            }
        });

        jRadButtDefRange.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _rngSc1.setEnabled(false);
                _rngSc2.setEnabled(false);
                _rngSl1.setEnabled(false);
                _rngSl2.setEnabled(false);
                _rngDf1.setEnabled(true);
                _rngDf2.setEnabled(true);
                notifyActionListener();

            }
        });

        jrb3.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                _rngSc1.setEnabled(false);
                _rngSc2.setEnabled(false);
                _rngSl1.setEnabled(true);
                _rngSl2.setEnabled(true);
                _rngDf1.setEnabled(false);
                _rngDf2.setEnabled(false);
                notifyActionListener();

            }
        });

        _rngDf1.addKeyListener(_xDef1KeyListener);
        _rngDf2.addKeyListener(_xDef2KeyListener);

    }

    public double getXScope1() {
        return _xScope1;
    }

    public double getXScope2() {
        return _xScope2;
    }

    protected double getStopTimeValue() {
        if (jRadButScopeRange.isSelected()) {
            return _xScope2;
        } else if (jRadButtDefRange.isSelected()) {
            return _xDef2;
        } else {
            return _xSlider2;
        }
    }

    protected double getStartTimeValue() {
        if (jRadButScopeRange.isSelected()) {
            return _xScope1;
        } else if (jRadButtDefRange.isSelected()) {
            return _xDef1;
        } else {
            return _xSlider1;
        }
    }

    private void setSliderValues(final double[] sliderValues) {
        _xSlider1 = Math.min(sliderValues[0], sliderValues[1]);
        _xSlider2 = Math.max(sliderValues[0], sliderValues[1]);
    }
}
