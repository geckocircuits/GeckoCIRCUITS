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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalHiddenSubcircuit;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// Leistungskreis-IGBT (Knickkennlinie, Modifikation des THYR)
public final class IGBT extends AbstractVoltageDropSwitch implements HiddenSubCircuitable {

    private static final double WIDTH = 0.7;
    private static final double HEIGHT = 0.4;
    private static final int TRIANGLE_WIDTH = 5;
    public static final AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(IGBT.class, "IGBT", I18nKeys.IGBT, I18nKeys.INSULATED_GATE_BIPOLAR_TRANSISTOR);
    
    final UserParameter<Boolean> _isSatCurEnabled = UserParameter.Builder.
            <Boolean>start("satCurEnabled", false).
            longName(I18nKeys.IF_ZERO_ISAT_DISABLED).
            shortName("iSAT_enabled").
            unit("").
            arrayIndex(this, 10).
            build();
    final UserParameter<Double> _saturationCurrent = UserParameter.Builder.
            <Double>start("saturationCurrent", 10.0).
            longName(I18nKeys.IGBT_SATURATION_CURRENT_VALUE).
            shortName("iSAT").
            unit("A").
            arrayIndex(this, 11).
            build();
    private final AbstractTerminal outTerminal;
    private final AbstractTerminal inTerminal;
    

    public IGBT() {
        super();
        outTerminal = YOUT.get(0);
        inTerminal = XIN.get(0);
        kOn.setValueWithoutUndo(30e-6);
        kOff.setValueWithoutUndo(15e-6);
    }

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        parameter[0] = parameter[3];
        parameter[4] = 0;
        parameter[5] = 0;
        parameter[8] = 0;
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {        
        graphics.drawPolyline(
                new int[]{-dpix, -dpix, (int) (-dpix * (1 + HEIGHT))},
                new int[]{-dpix, dpix, dpix}, 3);
        graphics.fillPolygon(
                new int[]{(int) (-dpix * WIDTH), (int) (-dpix * WIDTH), 0},
                new int[]{(int) (dpix - TRIANGLE_WIDTH), dpix, dpix}, 3);

    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawPolyline(
                new int[]{0, 0, (int) (-dpix * WIDTH), (int) (-dpix * WIDTH), 0, 0},
                new int[]{- 2 * dpix, -dpix, -dpix,
            dpix, dpix, 2 * dpix}, 6);

    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        addGateTextInfo();
        verluste.addTextInfoValue(_textInfo);
    }    

    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        if (_isSatCurEnabled.getValue()) {
            Diode saturationDiode = (Diode) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_D, this);
            double rONSmall = _onResistance.getValue();
            double uForwardSmall = -_onResistance.getValue() * _saturationCurrent.getValue();

            double[] diodeParameter = new double[]{RD_OFF_DEFAULT, uForwardSmall, rONSmall, RD_OFF_DEFAULT, 0, 0, 0, 0, -1, -1, 0, -1, 1};
            saturationDiode.setParameter(diodeParameter);
            saturationDiode.getIDStringDialog().setRandomStringID();

            AbstractTerminal internalTerminal = new TerminalHiddenSubcircuit(this);
            saturationDiode.YOUT.set(0, internalTerminal);
            saturationDiode.XIN.set(0, outTerminal);
            this.YOUT.set(0, internalTerminal);

            if (YOUT.size() > 1) {
                this.YOUT.set(1, outTerminal);
            } else {
                this.YOUT.add(outTerminal);
            }

            AbstractCurrentSource saturationCurrentSource = (AbstractCurrentSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_I, this);
            // TypQuelle - iNmax - frequ - offset - phase - tastverh. - Strom - Spannung    -->
            double[] currentParameter = new double[]{SourceType.QUELLE_DC_NEW, _saturationCurrent.getValue(), 0, 0, 0, 0, 0, 0, -1, -1, 0, 0};
            saturationCurrentSource.setParameter(currentParameter);
            saturationCurrentSource.XIN.set(0, internalTerminal);
            saturationCurrentSource.YOUT.set(0, outTerminal);
            saturationCurrentSource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_DC);
            final List<AbstractBlockInterface> returnValue = new ArrayList<AbstractBlockInterface>();
            returnValue.add(saturationDiode);
            returnValue.add(saturationCurrentSource);
            return Collections.unmodifiableCollection(returnValue);
        } else {
            XIN.set(0, inTerminal);
            YOUT.set(0, outTerminal);
            if (YOUT.size() > 1) {
                YOUT.pop();
            }
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public boolean includeParentInSimulation() {
        return true;
    }

    @Override
    protected Window openDialogWindow() {
        return new IGBTDialog(this);
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new IGBTCalculator(this));
    }
}
