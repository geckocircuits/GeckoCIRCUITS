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
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.PostCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Window;

public abstract class AbstractInductor extends AbstractNonLinearCircuitComponent
        implements PostCalculatable, CurrentMeasurable, DirectVoltageMeasurable {

    private static final double HEIGHT = 0.8;
    final UserParameter<Double> _inductance = UserParameter.Builder.
            <Double>start("inductance", 3.0E-4).
            longName(I18nKeys.INDUCTANCE).
            shortName("L").
            unit("H").
            showInTextInfo(TextInfoType.SHOW_NEVER). // noninearity: custom display
            arrayIndex(this, 0).
            build();
    final UserParameter<Double> _initialCurrent = UserParameter.Builder.
            <Double>start("initialCurrent", 0.0).
            longName(I18nKeys.INITIAL_CURRENT).
            shortName("iL(0)").
            unit("A").
            showInTextInfo(TextInfoType.SHOW_NON_NULL).
            arrayIndex(this, 1).
            build();

    @Override
    public final String getNonlinearFileExtension() {
        return ".nll";
    }

    @Override
    public final String getIndependentVariableName() {
        return "i";
    }

    @Override
    public final String getNonlinearName() {
        return "inductance";
    }

    @Override
    public String getNonlinearNameShort() {
        return "L(i)";
    }

    @Override
    public final double[][] getInitalNonlinValues() {
        double[][] returnValue = new double[2][NONLIN_IND_X_DEFAULT.length];
        for (int i = 0; i < NONLIN_IND_X_DEFAULT.length; i++) {
            returnValue[0][i] = NONLIN_IND_X_DEFAULT[i];
            returnValue[1][i] = NONLIN_IND_Y_DEFAULT[i];
        }
        return returnValue;
    }

    private double getRQ() {
        return Math.round(dpix * (2 * HEIGHT) / 7.0);
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        int yq = -1;
        double rq = getRQ();
        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke((float) 2.0));
        for (yq = (int) (dpix * (0 - HEIGHT)); yq <= (int) (dpix * (0 + HEIGHT)); yq += (int) (2 * rq)) {
            graphics.drawArc((int) (-2.0 * rq), (int) (yq - rq), (int) (4 * rq), (int) (2 * rq), 90, 180);
        }
        graphics.setStroke(oldStroke);
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        final double rq = getRQ();
        graphics.setStroke(new BasicStroke((float) 1.0));
        graphics.drawLine(0, (int) (-dpix * 2), 0, (int) (-dpix * HEIGHT - rq));
        graphics.drawLine(0, (int) (dpix * 2), 0, (int) (dpix * HEIGHT + rq));
    }

    public double getStartInductance() {
        if (_isNonlinear.getValue()) {
            return getActualValueLINFromLinearizedCharacteristic(Math.abs(parameter[1]));
        } else {
            return _inductance.getValue();
        }
    }

    @Override
    public UserParameter<Double> getNonlinearReplacedParameter() {
        return _inductance;
    }

    @Override
    public final String getNonlinearFileEnding() {
        return ".nll";
    }

    @Override
    public final double[] getNonlinXDefault() {
        return NONLIN_IND_X_DEFAULT;
    }

    @Override
    public final double[] getNonlinYDefault() {
        return NONLIN_IND_Y_DEFAULT;
    }

    @Override
    public final void doInitialization() {
        if (_isNonlinear.getValue()) {
            parameter[0] = getActualValueLINFromLinearizedCharacteristic(Math.abs(parameter[1]));
        }
    }

    @Override
    protected Window openDialogWindow() {
        return new InductorDialog(this);
    }

    @Override
    public final void doCalculation(final double deltaT, final double time) {
        if (_isNonlinearForCalculationUsage) {
            // continuous update of L(i) based on the non-linear characteristic -->             
            parameter[0] = getActualValueLINFromLinearizedCharacteristic(Math.abs(parameter[2]));            
            //System.out.println("ind ind " + parameter[0] + "\t" + time + " " + parameter[2]);
        }
    }

    @Override
    public final void setzeParameterZustandswerteAufNULL() {
        parameter[2] = 0;
        parameter[3] = 0;
        parameter[4] = 0;
        initialize();
    }
}
