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

import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitSource.WIDTH;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCurrentSource extends AbstractCircuitSource {

    private static final double I_ARROW_LENGTH = 0.7;
    private static final double ARROW_HEAD_WIDTH = 0.4;
    private static final double ARROW_HEAD_LENGTH = 0.8;
    private static final double ARROW_LINE_WIDTH = 0.1;
    private static final int NO_TRI_POINTS = 3;
    private static final double DEFAULT_AMPLITUDE = 1.0;

    @Override
    protected final void drawForeground(final Graphics2D graphics) {
        super.drawForeground(graphics);

        graphics.fillPolygon(new int[]{0, (int) (ARROW_HEAD_WIDTH * WIDTH * dpix), (int) (-ARROW_HEAD_WIDTH * WIDTH * dpix)},
                new int[]{(int) (ARROW_HEAD_LENGTH * WIDTH * dpix), 0, 0}, NO_TRI_POINTS);
        graphics.fillRect((int) (-ARROW_LINE_WIDTH * WIDTH * dpix),
                (int) (dpix * (-I_ARROW_LENGTH * WIDTH) + 1),
                (int) (dpix * 2 * ARROW_LINE_WIDTH * WIDTH), (int) (dpix * I_ARROW_LENGTH * WIDTH));
    }
    

    @Override
    final String getUnitForDomain(final ConnectorType connectorType) {
        switch (connectorType) {
            case LK:
            case LK_AND_RELUCTANCE:
                return "A";
            case THERMAL:
                return "W";
            case RELUCTANCE:
                return "N/D";
            default:
                assert false;
                return "A";
        }
    }

    @Override
    final ConnectorType[] getDomains() {
        return new ConnectorType[]{ConnectorType.LK, ConnectorType.THERMAL};
    }

    @Override
    final String[] getShortNamesForAmplitude() {
        return new String[]{"ampl"};
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
                return "iMAX";
            case THERMAL:
                return "Pv";
            default:
                assert false;
                return "iMAX";
        }
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new CurrentSourceCalculator(this));
    }
    
}
