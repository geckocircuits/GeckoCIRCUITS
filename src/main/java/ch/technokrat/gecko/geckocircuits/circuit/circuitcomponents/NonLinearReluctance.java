package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

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
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import static ch.technokrat.gecko.geckocircuits.circuit.ConnectorType.LK;
import static ch.technokrat.gecko.geckocircuits.circuit.ConnectorType.RELUCTANCE;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.PostCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalHiddenSubcircuit;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePositionReluctance;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent.NONLIN_IND_X_DEFAULT;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent.NONLIN_IND_Y_DEFAULT;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class NonLinearReluctance extends AbstractNonLinearCircuitComponent implements
        HiddenSubCircuitable, PostCalculatable, CurrentMeasurable, DirectVoltageMeasurable {

    public static final AbstractTypeInfo TYPE_INFO = new ReluctanceTypeInfo(NonLinearReluctance.class, "NLRel", I18nKeys.NONLINEAR_RELUCTANCE);
    private static final int X_TERM_DISTANCE = 2;
    private static final double WIDTH = 0.35;
    private static final double HEIGHT = 1;
    private final AbstractVoltageSource _primarySource
            = (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);
    public final AbstractVoltageSource _secondarySource
            = (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);
    private AbstractBlockInterface[] qLK = new AbstractBlockInterface[]{_primarySource, _secondarySource};

    public NonLinearReluctance() {
        this.setOutputTerminal(0, new TerminalHiddenSubcircuit(this));
        TerminalRelativePosition commonTerminal = new TerminalRelativePositionReluctance(this, 0, X_TERM_DISTANCE);
        setInputTerminal(0, commonTerminal);
        YOUT.add(new TerminalRelativePositionReluctance(this, 0, -X_TERM_DISTANCE) {
            @Override
            public void paintComponent(Graphics graphics) {
                final int dpix = AbstractCircuitSheetComponent.dpix;
                if (_parentElement.getDisplayProperties().showFlowSymbol) {
                    paintFlowSymbol(dpix, graphics);
                }
                super.paintComponent(graphics); //To change body of generated methods, choose Tools | Templates.

            }
        });

        _primarySource.setInputTerminal(0, XIN.get(0));
        _primarySource.setOutputTerminal(0, YOUT.get(0));

        _secondarySource.setInputTerminal(0, XIN.get(0));
        _secondarySource.setOutputTerminal(0, YOUT.get(1));

        _primarySource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_DIDTCURRENTCONTROLLED);
        _secondarySource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY);
        _primarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 1);
        _secondarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 2);

        _primarySource.directPotentialGain.setUserValue(-1.0);
        _secondarySource.directPotentialGain.setValueWithoutUndo(-1.0);
        _primarySource.getIDStringDialog().setNameUnChecked("prim nonlin rel");
        _secondarySource.getIDStringDialog().setNameUnChecked("sec nonlin rel");
        _isNonlinear.setValueWithoutUndo(true);
    }

    @Override
    public AbstractCircuitBlockInterface[] getCurrentMeasurementComponents(final ConnectorType simulationDomain) {
        setSubComponentsName();
        return new AbstractCircuitBlockInterface[]{_secondarySource};
    }

    private void setSubComponentsName() {
        _primarySource.getIDStringDialog().setNameUnChecked(getStringID());
        _secondarySource.getIDStringDialog().setNameUnChecked(getStringID());
    }

    @Override
    public AbstractBlockInterface[] getDirectVoltageMeasurementComponents(final ConnectorType connectorType) {
        setSubComponentsName();
        switch (connectorType) {
            case LK:
                return new AbstractBlockInterface[]{_primarySource};
            case RELUCTANCE:
                return new AbstractBlockInterface[]{_secondarySource};
            default:
                return new AbstractBlockInterface[0];
        }

    }

    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        return Arrays.asList(qLK);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList();
    }

    @Override
    public boolean includeParentInSimulation() {
        return true;
    }

    @Override
    public UserParameter<Double> getNonlinearReplacedParameter() {
        return null;
    }

    @Override
    protected final void drawForeground(final Graphics2D graphics) {
        graphics.drawRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * HEIGHT));
        int length = (int) (dpix * 0.6);
        graphics.drawLine(-length, -length, length, length);
    }

    @Override
    protected final void drawBackground(final Graphics2D graphics) {
        graphics.fillRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * HEIGHT));
    }

    @Override
    protected void importIndividual(TokenMap tokenMap) {
        super.importIndividual(tokenMap); //To change body of generated methods, choose Tools | Templates.
        _primarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 1);
        _secondarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 2);
    }

    @Override
    protected final Window openDialogWindow() {
        return new NonlinearReluctanceDialog(this);
    }

    @Override
    public final String getNonlinearFileExtension() {
        return ".nlr";
    }

    @Override
    public final String getIndependentVariableName() {
        return "i";
    }

    @Override
    public final String getNonlinearName() {
        return "reluctance";
    }

    @Override
    public String getNonlinearNameShort() {
        return "Rel(i)";
    }

    @Override
    public final double[][] getInitalNonlinValues() {
        double[][] returnValue = new double[2][NONLIN_IND_X_DEFAULT.length];
        for (int i = 0; i < NONLIN_IND_X_DEFAULT.length; i++) {
            returnValue[0][i] = NONLIN_REL_X_DEFAULT[i];
            returnValue[1][i] = NONLIN_REL_Y_DEFAULT[i];
        }
        return returnValue;
    }

    public double getStartInductance() {
        return getActualValueLINFromLinearizedCharacteristic(Math.abs(parameter[1]));
    }

    @Override
    public final String getNonlinearFileEnding() {
        return ".nlr";
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
        parameter[0] = getActualValueLINFromLinearizedCharacteristicInverse(Math.abs(parameter[1]));
    }
    
    @Override
    public final void doCalculation(final double deltaT, final double time) {        
        parameter[0] = getActualValueLINFromLinearizedCharacteristicInverse(Math.abs(_secondarySource.parameter[6]));                                                       
    }

    @Override
    public final void setzeParameterZustandswerteAufNULL() {
        parameter[2] = 0;
        parameter[3] = 0;
        parameter[4] = 0;
        initialize();
    }
}
