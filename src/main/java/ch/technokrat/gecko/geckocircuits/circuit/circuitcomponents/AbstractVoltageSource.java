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
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.TimeFunctionConstant;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractVoltageSource extends AbstractCircuitSource {

    public static final String[] SHORT_NAMES_FOR_AMPLITUDE = new String[]{"uMAX", "MMF_MAX", "T_MAX"};
    private static final double DEFAULT_AMPLITUDE = 325;
    private static final int LOWER_LIMIT_INDEX = 12;
    private static final int UPPER_LIMIT_INDEX = 13;
    private static final int PLUS_MINUS_X_OFFSET = -12;
    private static final double PLUSMINUS_SEPARATION = 1.5;
    
    final UserParameter<Double> _lowerLimit = UserParameter.Builder.
            <Double>start("lowerLimit", Double.MIN_VALUE).
            mapDomains(getDomains()).
            longName(I18nKeys.VOLTAGE_LIMITATION_MINIMUM).
            shortName("lowerLimit").
            unit(getSortedDomainUnits()).
            arrayIndex(this, LOWER_LIMIT_INDEX).
            build();
    final UserParameter<Double> _upperLimit = UserParameter.Builder.
            <Double>start("upperLimit", Double.MAX_VALUE).
            mapDomains(getDomains()).
            longName(I18nKeys.VOLTAGE_LIMITATION_MAXIMUM).
            shortName("upperLimit").
            unit(getSortedDomainUnits()).
            arrayIndex(this, UPPER_LIMIT_INDEX).
            build();

    @Override
    protected final void drawForeground(final Graphics2D graphics) {
        super.drawForeground(graphics);
        int lgq = (int) (dpix * WIDTH / 2) + 2;
        drawPlusSymbol(graphics, lgq);
        drawMinusSymbol(graphics, lgq);
    }

    private void drawPlusSymbol(final Graphics2D graphics, final int length) {
        final int yHeight = (int) (-dpix * PLUSMINUS_SEPARATION);
        graphics.drawLine((int) PLUS_MINUS_X_OFFSET - length / 2, yHeight, (int) PLUS_MINUS_X_OFFSET + length / 2, yHeight);
        graphics.drawLine(PLUS_MINUS_X_OFFSET, (int) yHeight + length / 2, PLUS_MINUS_X_OFFSET, (int) yHeight - length / 2);
    }

    private void drawMinusSymbol(final Graphics2D graphics, final int length) {
        final int yHeight = (int) (PLUSMINUS_SEPARATION * dpix);
        graphics.drawLine((int) PLUS_MINUS_X_OFFSET - length / 2, yHeight, (int) PLUS_MINUS_X_OFFSET + length / 2, yHeight);
    }
    
    @Override
    protected final void importIndividual(final TokenMap tokenMap) {
        // careful! if the limits are set (the user usually doesnt
        // do this, only used in operational amplifier), then the
        // voltage controlled source is doing nonsense!
        _upperLimit.setValueWithoutUndo(Double.MAX_VALUE);
        _lowerLimit.setValueWithoutUndo(-Double.MAX_VALUE);
        parameter[UPPER_LIMIT_INDEX] = Double.MAX_VALUE;
        parameter[LOWER_LIMIT_INDEX] = Double.MIN_VALUE;

        super.importIndividual(tokenMap);
    }

    @Override
    final String getUnitForDomain(final ConnectorType connectorType) {
        switch (connectorType) {
            case LK:
            case LK_AND_RELUCTANCE:
                return "V";
            case THERMAL:
                return "K";
            case RELUCTANCE:
                return "A";
            case CONTROL:
            default:
                //assert false;
                return "V";
        }
    }

    @Override
    final ConnectorType[] getDomains() {
        return new ConnectorType[]{ConnectorType.LK, ConnectorType.RELUCTANCE, ConnectorType.THERMAL};
    }

    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    final String[] getShortNamesForAmplitude() {
        return SHORT_NAMES_FOR_AMPLITUDE;
    }

    @Override
    final double getDefaultAmplitudeValue() {
        return DEFAULT_AMPLITUDE;
    }

    @Override
    final String getDCValueShortNameFromDomain(final ConnectorType connectorType) {

        switch (connectorType) {
            case LK:
            case LK_AND_RELUCTANCE:
                return "uDC";
            case THERMAL:
                return "T_DC";
            case RELUCTANCE:
                return "MMF_DC";
            default:
                assert false;
                return "iMAX";
        }
    }

    @Override
    protected final String[] getAlternativeDCValueShortNames() {
        return new String[]{"uMAX", "MMF_MAX", "T_MAX"};
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new VoltageSourceCalculator(new TimeFunctionConstant(12),this));
    }
    
}