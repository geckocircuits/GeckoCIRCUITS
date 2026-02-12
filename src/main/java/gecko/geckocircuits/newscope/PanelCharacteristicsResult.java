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

import gecko.geckocircuits.allg.FormatJTextField;
import gecko.geckocircuits.allg.GlobalColors;
import gecko.geckocircuits.allg.GlobalFonts;
import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 *
 * @author andy
 */
public final class PanelCharacteristicsResult extends JPanel {
    private final JPanel _pERGx = new JPanel();        
    
    private final FormatJTextField[] _ftfAVG;  // Textfelder fuer AVG-Werte
    private final FormatJTextField[] _ftfRMS;  // Textfelder fuer RMS-Werte
    private final FormatJTextField[] _ftfMIN, _ftfMAX;  // Textfelder fuer minimale und maximale Werte
    // Textfelder fuer weitere Kennwerte:
    private final FormatJTextField[] _ftfCREST;
    private final FormatJTextField[] _ftfSHAPE;
    private final FormatJTextField[] _ftfTHD;
    private final FormatJTextField[] _ftfRIPPLE;  
    private final FormatJTextField[] _peakToPeak;  
    
    private final JPanel _pClcq;
    private final JPanel _pClc;
    private static final int NO_VIEW_COLS = 8;
    private static final int FONT_SIZE = 11;
    
    public void setAvgText(final String text, final int index) {
        _ftfAVG[index].setText(text);
    }
    
    public void setRmsText(final String text, final int index) {
        _ftfRMS[index].setText(text);
    }
    
    public void setMinText(final String text, final int index) {
        _ftfMIN[index].setText(text);
    }
    
    public void setMaxText(final String text, final int index) {
        _ftfMAX[index].setText(text);
    }
    
    public void setCRestText(final String text, final int index) {
        _ftfCREST[index].setText(text);
    }
    
    public void setShapeText(final String text, final int index) {
        _ftfSHAPE[index].setText(text);
    }
    
    public void setThdText(final String text, final int index) {
        _ftfTHD[index].setText(text);
    }
    
    public void setPeakPeakText(final String text, final int index) {
        _peakToPeak[index].setText(text);
    }
    
    public void setRippleText(final String text, final int index) {
        _ftfRIPPLE[index].setText(text);
    }
    
    public PanelCharacteristicsResult(final AbstractDataContainer dataContainer) {
        super();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                "Characteristics", TitledBorder.LEFT, TitledBorder.TOP));
        _pClcq = new JPanel();
        _pClcq.setLayout(new GridLayout(dataContainer.getRowLength()+1, 1));
        //
        final JLabel jlH1 = new JLabel("");
        jlH1.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
        jlH1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        _pClcq.add(jlH1);
        for (int i1 = 1; i1 < dataContainer.getRowLength()+1; i1++) {
            final JLabel jlH = new JLabel(dataContainer.getSignalName(i1-1) + "   ");
            jlH.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
            jlH.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            _pClcq.add(jlH);
        }

        _pClc = new JPanel();
        _pClc.setLayout(new GridLayout(dataContainer.getRowLength()+1, NO_VIEW_COLS));
        
        setCharTitleLabels();
        
        final int columnLength = dataContainer.getRowLength();
        final List<FormatJTextField[]> allFormatTxtFlds = new ArrayList<FormatJTextField[]>();

        _ftfAVG = new FormatJTextField[columnLength];
        _ftfRMS = new FormatJTextField[columnLength];
        _ftfTHD = new FormatJTextField[columnLength];
        _ftfMIN = new FormatJTextField[columnLength];
        _ftfMAX = new FormatJTextField[columnLength];
        _ftfRIPPLE = new FormatJTextField[columnLength];
        _ftfCREST = new FormatJTextField[columnLength];
        _ftfSHAPE = new FormatJTextField[columnLength];
        _peakToPeak = new FormatJTextField[columnLength];

        allFormatTxtFlds.add(_ftfAVG);
        allFormatTxtFlds.add(_ftfRMS);
        allFormatTxtFlds.add(_ftfTHD);
        allFormatTxtFlds.add(_ftfMIN);
        allFormatTxtFlds.add(_ftfMAX);
        allFormatTxtFlds.add(_peakToPeak);
        allFormatTxtFlds.add(_ftfRIPPLE);
        allFormatTxtFlds.add(_ftfCREST);
        allFormatTxtFlds.add(_ftfSHAPE);

        for (int i1 = 0; i1 < columnLength; i1++) {
            for (FormatJTextField[] tfarray : allFormatTxtFlds) {
                tfarray[i1] = new FormatJTextField();
                tfarray[i1].setColumns(NO_VIEW_COLS);
                tfarray[i1].setText("-");
                tfarray[i1].setEditable(false);
                tfarray[i1].setFont(new java.awt.Font("Arial", 0, FONT_SIZE));
                _pClc.add(tfarray[i1]);
            }
        }

        _pERGx.setLayout(new BorderLayout());
        _pERGx.add(_pClcq, BorderLayout.WEST);
        _pERGx.add(_pClc, BorderLayout.CENTER);
        this.add(_pERGx, BorderLayout.NORTH);
    }

    private void setCharTitleLabels() {
        final List<JLabel> titleLabels = new ArrayList<JLabel>();
        titleLabels.add(new JLabel("avg"));
        titleLabels.add(new JLabel("rms"));
        titleLabels.add(new JLabel("THD"));
        titleLabels.add(new JLabel("min"));
        titleLabels.add(new JLabel("max"));
        titleLabels.add(new JLabel("peak-peak"));
        titleLabels.add(new JLabel("ripple"));
        titleLabels.add(new JLabel("klirr"));
        titleLabels.add(new JLabel("shape"));

        for(JLabel tmp : titleLabels) {
            tmp.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
            tmp.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            _pClc.add(tmp);
        }
        
    }
}
