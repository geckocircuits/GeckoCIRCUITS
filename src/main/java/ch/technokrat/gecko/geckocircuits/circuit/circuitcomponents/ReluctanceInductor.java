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

import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.NameAlreadyExistsException;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePositionReluctance;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ReluctanceInductor extends AbstractCircuitBlockInterface
        implements HiddenSubCircuitable, CurrentMeasurable, 
        DirectVoltageMeasurable {

    static final AbstractTypeInfo TYPE_INFO =
            new ReluctanceAndCircuitTypeInfo(ReluctanceInductor.class, "LRel",
            I18nKeys.INDUCTOR_RELUCTANCE, I18nKeys.INDUCTOR_RELUCTANCE_COUPLING_COMPONENT);
    private static final int ARC_CONST_1 = 110;
    private static final int ARC_CONST_2 = 270;
    private static final double ARC_CONST_3 = 7;
    private static final int POLYGON_CORNERS = 3;
    private static final double HEIGHT = 0.8;
    private static final int X_TERM_DISTANCE = 2;
    private static final int WINDINGS_INDEX = 0;
    final UserParameter<Double> _windings = UserParameter.Builder.
            <Double>start("turns", 2.0).
            longName(I18nKeys.WINDING_NUMBER).
            shortName("n").
            addAlternativeShortName("turns").
            unit("turns").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, WINDINGS_INDEX).
            build();
    private static final int INIT_CUR_INDEX = 1;
    final UserParameter<Double> _initCurrent = UserParameter.Builder.
            <Double>start("initCurrent", 0.0).
            longName(I18nKeys.INITIAL_CURRENT).
            shortName("i0").
            showInTextInfo(TextInfoType.SHOW_NON_NULL).
            unit("A").
            arrayIndex(this, INIT_CUR_INDEX).
            build();
    private static final int INPUT_REV_INDEX = 3;
    final UserParameter<Boolean> _inputReversed = UserParameter.Builder.
            <Boolean>start("inputReversed", false).
            longName(I18nKeys.INPUT_REVERSED).
            shortName("ir").
            arrayIndex(this, INPUT_REV_INDEX).
            build();
    private static final int OUTPUT_REV_INDEX = 4;
    final UserParameter<Boolean> _outputReversed = UserParameter.Builder.
            <Boolean>start("outputReversed", false).
            longName(I18nKeys.OUTPUT_REVERSED).
            shortName("or").
            arrayIndex(this, OUTPUT_REV_INDEX).
            build();
    private final AbstractBlockInterface[] _qLK = new AbstractCircuitBlockInterface[2];
    ;
    
    private final AbstractVoltageSource _primarySource =
            (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);
    public final AbstractVoltageSource _secondarySource =
            (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);

    public ReluctanceInductor() {
        super();

        XIN.add(new TerminalRelativePosition(this, -1, X_TERM_DISTANCE));
        XIN.add(new TerminalRelativePosition(this, -1, -X_TERM_DISTANCE));

        YOUT.add(new TerminalRelativePositionReluctance(this, 1, X_TERM_DISTANCE));
        YOUT.add(new TerminalRelativePositionReluctance(this, 1, -X_TERM_DISTANCE));

        
        
        _qLK[0] = _primarySource;
        _qLK[1] = _secondarySource;
        _primarySource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_DIDTCURRENTCONTROLLED);
        _secondarySource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY);
        _primarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 1);
        _secondarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 2);

        defineTerminals();
        _inputReversed.addActionListener(_reversedListener);
        _outputReversed.addActionListener(_reversedListener);

        setSourcePotentialGains();
        
        _windings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                setSourcePotentialGains();                
            }
        });

        _initCurrent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                _primarySource._didtInitialCurrent.setValueWithoutUndo(_initCurrent.getValue());
            }
        });        
        
        
    }
    private final ActionListener _reversedListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            defineTerminals();
        }
    };
    
    private void setSourcePotentialGains() {
        _primarySource.directPotentialGain.setUserValue(-_windings.getValue());
        _secondarySource.directPotentialGain.setValueWithoutUndo(-_windings.getValue());
    }

    //CHECKSTYLE:OFF
    @Override
    public void setNewNameCheckedUndoable(final String newName) throws NameAlreadyExistsException {
        super.setNewNameCheckedUndoable(newName);
        setSubComponentsName();
    }

    @Override
    public void setNewNameChecked(final String newName) throws NameAlreadyExistsException {
        super.setNewNameChecked(newName);
        setSubComponentsName();
    }
    //CHECKSTYLE:ON

    private void setSubComponentsName() {
        _primarySource.getIDStringDialog().setNameUnChecked(getStringID());
        _secondarySource.getIDStringDialog().setNameUnChecked(getStringID());        
    }

    @Override
    public AbstractCircuitBlockInterface[] getCurrentMeasurementComponents(final ConnectorType simulationDomain) {
        setSubComponentsName();

        switch (simulationDomain) {
            case LK:
                return new AbstractCircuitBlockInterface[]{_primarySource};
            case RELUCTANCE:
                return new AbstractCircuitBlockInterface[]{_secondarySource};
            default:
                assert false;
                return null;
        }
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
        return Arrays.asList(_qLK);
    }

    protected void defineFlowSymbol(final ComponentDirection orientierung, final Graphics graphics) {

        final int[] xFl = new int[POLYGON_CORNERS];
        final int[] yFl = new int[POLYGON_CORNERS];


        for (int i = -1; i < 2; i += 2) {

            int reversed;
            if (i == 1) {
                if (_inputReversed.getValue()) {
                    reversed = - 1;
                } else {
                    reversed = 1;
                }
            } else {
                if (_outputReversed.getValue()) {
                    reversed = -1;
                } else {
                    reversed = 1;
                }
            }

            xFl[0] = (int) (dpix * (-i));
            xFl[1] = xFl[0] - ARROW_WIDTH;
            xFl[2] = xFl[0] + ARROW_WIDTH;
            yFl[0] = (int) reversed * ((int) (-dpix * 2) + 2);
            yFl[1] = (int) (yFl[0] + reversed * ARROW_LENGTH);
            yFl[2] = (int) (yFl[1]);

            graphics.setColor(Color.magenta);
            graphics.drawPolygon(xFl, yFl, POLYGON_CORNERS);
        }
    }

    private double getRadiusQ() {
        return Math.round(dpix * (2 * HEIGHT) / ARC_CONST_3);
    }

    @Override
    protected void drawForeground(final Graphics2D g2d) {
        final Color origColor = g2d.getColor();
        final double radiusQ = getRadiusQ();
        if (SchematischeEingabe2._lkDisplayMode.showFlowSymbol) {
            this.defineFlowSymbol(getComponentDirection(), g2d);
        }

        g2d.setStroke(new BasicStroke((float) 2.0));
        g2d.setColor(GlobalColors.farbeFertigElementRELUCTANCE);
        g2d.drawLine(0, (int) (dpix * (+HEIGHT) + radiusQ), 0, (int) (dpix * (-HEIGHT) - radiusQ));

        g2d.setColor(origColor);
        for (int yq = (int) (dpix * (-HEIGHT)); yq <= (int) (dpix * (+HEIGHT)); yq += (int) (2 * radiusQ)) {
            g2d.drawArc((int) (-radiusQ * 2.0), (int) (yq - radiusQ), (int) (2 * radiusQ * 2),
                    (int) (2 * radiusQ), ARC_CONST_1, ARC_CONST_2);
        }

        g2d.setStroke(new BasicStroke((float) 1.0));
        g2d.drawLine((int) (dpix * (- 1)), (int) (dpix * (+HEIGHT) + radiusQ), (int) (dpix * (- 1)), (int) (dpix * (+2)));
        g2d.drawLine((int) -dpix, (int) (-dpix * HEIGHT - radiusQ), (int) -dpix, (int) (-dpix * 2));

    }

    @Override
    void drawConnectorLines(final Graphics2D graphics) {
        final double radiusQ = getRadiusQ();

        graphics.drawLine((int) -dpix, (int) (dpix * HEIGHT + radiusQ), (int) (-dpix / 2), (int) (dpix * HEIGHT + radiusQ));
        graphics.drawLine((int) -dpix, (int) (-dpix * HEIGHT - radiusQ), (int) (-dpix / 2), (int) (-dpix * HEIGHT - radiusQ));
        graphics.drawLine((int) (-dpix / 2), (int) (-dpix * HEIGHT - radiusQ), (int) (-dpix / 2), (int) (dpix * HEIGHT + radiusQ));


        final Color oldColor = graphics.getColor();
        graphics.setColor(GlobalColors.farbeFertigElementRELUCTANCE);
        graphics.drawLine((int) dpix, (int) (dpix * HEIGHT + radiusQ), (int) dpix, (int) (dpix * 2));
        graphics.drawLine((int) dpix, (int) (-dpix * HEIGHT - radiusQ), (int) dpix, (int) (-dpix * 2));
        graphics.drawLine(0, (int) (dpix * (+HEIGHT) + radiusQ), (int) (dpix * (+1)), (int) (dpix * (+HEIGHT) + radiusQ));
        graphics.drawLine(0, (int) (dpix * (-HEIGHT) - radiusQ), (int) (dpix * (+1)), (int) (dpix * (-HEIGHT) - radiusQ));
        graphics.setColor(oldColor);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        _primarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 1);
        _secondarySource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 2);
    }

    private void defineTerminals() {
        if (_inputReversed.getValue()) {
            ((AbstractCircuitBlockInterface) _primarySource).setInputTerminal(0, XIN.get(0));
            ((AbstractCircuitBlockInterface) _primarySource).setOutputTerminal(0, XIN.get(1));
        } else {
            ((AbstractCircuitBlockInterface) _primarySource).setInputTerminal(0, XIN.get(1));
            ((AbstractCircuitBlockInterface) _primarySource).setOutputTerminal(0, XIN.get(0));
        }

        if (_outputReversed.getValue()) {
            ((AbstractCircuitBlockInterface) _secondarySource).setInputTerminal(0, YOUT.get(0));
            ((AbstractCircuitBlockInterface) _secondarySource).setOutputTerminal(0, YOUT.get(1));
        } else {
            ((AbstractCircuitBlockInterface) _secondarySource).setInputTerminal(0, YOUT.get(1));
            ((AbstractCircuitBlockInterface) _secondarySource).setOutputTerminal(0, YOUT.get(0));
        }

    }

    @Override
    public boolean includeParentInSimulation() {
        return false;
    }

    @Override
    protected Window openDialogWindow() {
        return new ReluctanceInductorDialog(this);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
