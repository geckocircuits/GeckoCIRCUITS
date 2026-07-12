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

import gecko.geckocircuits.general.ProjectData;
import gecko.core.circuit.TokenMap;

/**
 *
 * @author andy
 */
final class AxisTickSettings {
    private boolean _autoTickSpacing = true;
    private boolean _showLabelsMaj = true;
    private boolean _showLabelsMin = false;
    private int _tickLengthMaj = DEF_LENGTH_MAJ;
    private int _tickLengthMin = DEF_LENGTH_MIN;
    private int _numTicksMinor = 2;  // Zahl der Minor-Ticks zwischen zwei regulaeren Ticks

    private static final int DEF_LENGTH_MAJ = 8;
    private static final int DEF_LENGTH_MIN = 5;

    public void setNumTicksMinor(final int value) {
        _numTicksMinor = value;
    }

    public int getNumTicksMinor() {
        return _numTicksMinor;
    }


    public int getTickLengthMin() {
        return _tickLengthMin;
    }

    public int getTickLengthMaj() {
        return _tickLengthMaj;
    }

    public boolean isAutoTickSpacing() {
        return _autoTickSpacing;
    }

    public void setAutoTickSpacing(final boolean value) {
        _autoTickSpacing = value;
    }

    public boolean isShowLabelsMaj() {
        return _showLabelsMaj;
    }

    public boolean isShowLabelsMin() {
        return _showLabelsMin;
    }

    public void setShowLabelsMaj(final boolean showLabelsMaj) {
        _showLabelsMaj = showLabelsMaj;
    }

    public void setShowLabelsMin(final boolean showLabelsMin) {
        _showLabelsMin = showLabelsMin;
    }

    void setTickLengthMaj(final int tickLengthMaj) {
        _tickLengthMaj = tickLengthMaj;
    }

    void setTickLengthMin(final int tickLengthMin) {
        _tickLengthMin = tickLengthMin;
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        ProjectData.appendAsString(ascii.append("\nnoMinorTicks"), _numTicksMinor);
        ProjectData.appendAsString(ascii.append("\nisShowLabelsMaj"), _showLabelsMaj);
        ProjectData.appendAsString(ascii.append("\nisShowLabelsMin"), _showLabelsMin);
        ProjectData.appendAsString(ascii.append("\ntickLengthMaj"), _tickLengthMaj);
        ProjectData.appendAsString(ascii.append("\ntickLengthMin"), _tickLengthMin);
    }

    void importASCII(final TokenMap axisMap) {
        _numTicksMinor = axisMap.readDataLine("noMinorTicks", _numTicksMinor);
        _showLabelsMaj = axisMap.readDataLine("isShowLabelsMaj", _showLabelsMaj);
        _showLabelsMin = axisMap.readDataLine("isShowLabelsMin", _showLabelsMin);
        _tickLengthMaj = axisMap.readDataLine("tickLengthMaj", _tickLengthMaj);
        _tickLengthMin = axisMap.readDataLine("tickLengthMin", _tickLengthMin);
    }

}
