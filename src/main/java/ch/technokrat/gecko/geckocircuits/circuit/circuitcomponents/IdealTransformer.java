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
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.NameAlreadyExistsException;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IdealTransformer extends AbstractCircuitBlockInterface implements HiddenSubCircuitable, CurrentMeasurable, DirectVoltageMeasurable {

    private static final double HEIGHT = 0.8;
    public static AbstractTypeInfo TYPE_INFO = new CircuitTypeInfo(IdealTransformer.class, "Trans", I18nKeys.IDEAL_TRANSFORMER);
    final UserParameter<Double> _windingsRatio = UserParameter.Builder.
            <Double>start("windingsRatio", 5.0).
            longName(I18nKeys.RATIO_OF_WINDINGS).
            shortName("ratio").
            unit("unit-less").
            arrayIndex(this, -1).
            build();

    final UserParameter<Double> _windings1 = UserParameter.Builder.
            <Double>start("windings1", 10.0).
            longName(I18nKeys.WINDING_NUMBER_PRIMARY_SIDE).
            shortName("n1").
            unit("turns").
            arrayIndex(this, 0).
            build();
    final UserParameter<Double> _windings2 = UserParameter.Builder.
            <Double>start("windings2", 2.0).
            longName(I18nKeys.WINDING_NUMBER_SECONDARY_SIDE).
            shortName("n2").
            unit("turns").
            arrayIndex(this, 1).
            build();
    final UserParameter<Double> _reversed = UserParameter.Builder.
            <Double>start("reversed", -1.0).
            longName(I18nKeys.REVERSE_MAGNETIC_COUPLING).
            shortName("reversed").
            arrayIndex(this, 2).
            build();

    private final AbstractVoltageSource primaryVoltageSource = (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);
    private final AbstractVoltageSource secondaryVoltageSource = (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);

    public IdealTransformer() {
        super();

        XIN.add(new TerminalRelativePosition(this, -1, 2));
        XIN.add(new TerminalRelativePosition(this, -1, -2));

        YOUT.add(new TerminalRelativePosition(this, 1, 2));
        YOUT.add(new TerminalRelativePosition(this, 1, -2));
        _windings1.addActionListener(ratioListener);
        _windings2.addActionListener(ratioListener);        

        this.setzeSubcircuit();
    }
    private ActionListener ratioListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {                        
            _windingsRatio.setValueWithoutUndo(_windings1.getValue() / _windings2.getValue());
        }
    };

    public void initPar() {

        primaryVoltageSource.directPotentialGain.setValueWithoutUndo(-_reversed.getValue() * _windings1.getValue() / _windings2.getValue());
        secondaryVoltageSource.directPotentialGain.setValueWithoutUndo(-_reversed.getValue() * _windings1.getValue() / _windings2.getValue());
        primaryVoltageSource.getComponentCoupling().setNewCouplingElement(0, secondaryVoltageSource);
        primaryVoltageSource._lowerLimit.setValueWithoutUndo(-Double.MAX_VALUE);
        primaryVoltageSource._upperLimit.setValueWithoutUndo(Double.MAX_VALUE);

        secondaryVoltageSource._lowerLimit.setValueWithoutUndo(-Double.MAX_VALUE);
        secondaryVoltageSource._upperLimit.setValueWithoutUndo(Double.MAX_VALUE);
    }

    // Initialisiereung nach INIT&START --> 
    @Override
    public void setzeParameterZustandswerteAufNULL() {
        this.initPar();
    }

    @Override
    public void setNewNameCheckedUndoable(String newName) throws NameAlreadyExistsException {
        super.setNewNameCheckedUndoable(newName);
        setSubComponentsName();
    }

    @Override
    public void setNewNameChecked(String newName) throws NameAlreadyExistsException {
        super.setNewNameChecked(newName);
        setSubComponentsName();
    }

    @Override
    public AbstractBlockInterface[] getDirectVoltageMeasurementComponents(final ConnectorType connectorType) {
        setSubComponentsName();
        if (connectorType == ConnectorType.LK) {
            return new AbstractBlockInterface[]{primaryVoltageSource, secondaryVoltageSource};
        } else {
            return new AbstractBlockInterface[0];
        }

    }

    private void setSubComponentsName() {
        try {
            primaryVoltageSource.setNewNameChecked(getStringID() + " prim");
            secondaryVoltageSource.setNewNameChecked(getStringID() + " sec");
        } catch (NameAlreadyExistsException ex) {
            Logger.getLogger(IdealTransformer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public AbstractCircuitBlockInterface[] getCurrentMeasurementComponents(final ConnectorType connectorType) {
        setSubComponentsName();
        return new AbstractCircuitBlockInterface[]{primaryVoltageSource, secondaryVoltageSource};
    }

    @Override
    public Collection<? extends AbstractBlockInterface> getHiddenSubCircuitElements() {
        initPar();
        return Arrays.asList(primaryVoltageSource, secondaryVoltageSource);
    }

    private void setzeSubcircuit() {

        primaryVoltageSource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 1);
        secondaryVoltageSource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 2);

        primaryVoltageSource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
        primaryVoltageSource.directPotentialGain.setValueWithoutUndo(-_reversed.getValue() * _windings1.getValue() / _windings2.getValue());
        secondaryVoltageSource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER);
        secondaryVoltageSource.directPotentialGain.setValueWithoutUndo(-_reversed.getValue() * _windings1.getValue() / _windings2.getValue());

        primaryVoltageSource.getComponentCoupling().setNewCouplingElement(0, secondaryVoltageSource);

        setSubComponentsName();

        primaryVoltageSource.XIN.set(0, XIN.get(0));
        primaryVoltageSource.YOUT.set(0, XIN.get(1));

        secondaryVoltageSource.XIN.set(0, YOUT.get(0));
        secondaryVoltageSource.YOUT.set(0, YOUT.get(1));

    }

    @Override
    protected void importIndividual(TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        primaryVoltageSource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 1);
        secondaryVoltageSource.getIdentifier().createNewIdentifier(getUniqueObjectIdentifier() + 2);
    }

    private void defineFlowSymbol(ComponentDirection orientierung, Graphics g) {

        Color origColor = g.getColor();
        g.setColor(Color.magenta);
        int x = getSheetPosition().x;
        int y = getSheetPosition().y;
        int[] xFl = new int[3];
        int[] yFl = new int[3];

        for (int i = -1; i < 2; i += 2) {
            switch (orientierung) {
                case NORTH_SOUTH:
                    xFl[0] = (int) (dpix * (x - i));
                    xFl[1] = xFl[0] - ARROW_WIDTH;
                    xFl[2] = xFl[0] + ARROW_WIDTH;
                    yFl[0] = (int) (dpix * (y + 2)) - 2;
                    yFl[1] = yFl[0] - ARROW_LENGTH;
                    yFl[2] = yFl[1];
                    break;
                case SOUTH_NORTH:
                    xFl[0] = (int) (dpix * (x - i));
                    xFl[1] = xFl[0] - ARROW_WIDTH;
                    xFl[2] = xFl[0] + ARROW_WIDTH;
                    yFl[0] = (int) (dpix * (y - 2)) + 2;
                    yFl[1] = yFl[0] + ARROW_LENGTH;
                    yFl[2] = yFl[1];
                    break;
                case WEST_EAST:
                    xFl[0] = (int) (dpix * (x + 2)) - 2;
                    xFl[1] = xFl[0] - ARROW_LENGTH;
                    xFl[2] = xFl[1];
                    yFl[0] = (int) (dpix * (y - i));
                    yFl[1] = yFl[0] - ARROW_WIDTH;
                    yFl[2] = yFl[0] + ARROW_WIDTH;
                    break;
                case EAST_WEST:
                    xFl[0] = (int) (dpix * (x - 2)) + 2;
                    xFl[1] = xFl[0] + ARROW_LENGTH;
                    xFl[2] = xFl[1];
                    yFl[0] = (int) (dpix * (y + i));
                    yFl[1] = yFl[0] - ARROW_WIDTH;
                    yFl[2] = yFl[0] + ARROW_WIDTH;
                    break;
                default:
                    assert false;
            }

            g.drawPolygon(xFl, yFl, 3);
        }
        g.setColor(origColor);
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        int x = getSheetPosition().x;
        int y = getSheetPosition().y;
        //-----------------
        // Klickbereich:        
        double rq = Math.round(dpix * (2 * HEIGHT) / 7.0);

        g2.setStroke(new BasicStroke((float) 2.0));
        for (int yq = (int) (dpix * (0 - HEIGHT)); yq <= (int) (dpix * (0 + HEIGHT)); yq += (int) (2 * rq)) {
            g2.drawArc((int) (dpix * (0 + 1) - 2.0 * rq), (int) (yq - rq), (int) (4 * rq), (int) (2 * rq), 90, 180);
        }

        for (int yq = (int) (dpix * (0 - HEIGHT)); yq <= (int) (dpix * (0 + HEIGHT)); yq += (int) (2 * rq)) {
            g2.drawArc((int) (dpix * (0 - 1) - 2.0 * rq), (int) (yq - rq), (int) (4 * rq), (int) (2 * rq), 90, -180);
        }

        g2.setStroke(new BasicStroke((float) 0.2 * dpix));
        g2.drawLine((int) (dpix * 0), (int) (dpix * (0 + HEIGHT)), (int) (dpix * 0), (int) (dpix * (0 - HEIGHT) - rq));

        g2.setStroke(new BasicStroke((float) 1.0));
        g2.drawLine((int) (dpix * (0 - 1)), (int) (dpix * (0 + HEIGHT) + rq), (int) (dpix * (0 - 1)), (int) (dpix * (0 + 2)));
        g2.drawLine((int) (dpix * (0 - 1)), (int) (dpix * (0 - HEIGHT) - rq), (int) (dpix * (0 - 1)), (int) (dpix * (0 - 2)));

        g2.drawLine((int) (dpix * (0 + 1)), (int) (dpix * (0 + HEIGHT) + rq), (int) (dpix * (0 + 1)), (int) (dpix * (0 + 2)));
        g2.drawLine((int) (dpix * (0 + 1)), (int) (dpix * (0 - HEIGHT) - rq), (int) (dpix * (0 + 1)), (int) (dpix * (0 - 2)));

        // Ansichten -->
        FontRenderContext frc = ((Graphics2D) graphics).getFontRenderContext();
        restoreOrigTransformation(graphics);

        if (SchematischeEingabe2._lkDisplayMode.showFlowSymbol) {
            this.defineFlowSymbol(getComponentDirection(), graphics);
        }

        if (SchematischeEingabe2._lkDisplayMode.showParameter) {
            Font origFont = graphics.getFont();
            graphics.setFont(SchematischeEingabe2.foLKSmall);
            switch (getComponentDirection()) {
                case EAST_WEST:
                    graphics.drawString("" + _windings1.getValue().floatValue(), dpix * x - graphics.getFontMetrics().stringWidth("" + _windings1.getValue().floatValue()) / 2, (int) (dpix * (y - 1.3)));
                    graphics.drawString("" + _windings2.getValue().floatValue(), dpix * x - graphics.getFontMetrics().stringWidth("" + _windings1.getValue().floatValue()) / 2, (int) (dpix * (y + 1.3)) + graphics.getFont().getSize());
                    break;
                case NORTH_SOUTH:
                    graphics.drawString("" + _windings1.getValue().floatValue(), (int) (dpix * (x - 1.5)) - graphics.getFontMetrics().stringWidth("" + _windings1.getValue().floatValue()), dpix * y + graphics.getFont().getSize() / 2 - 1);
                    graphics.drawString("" + _windings2.getValue().floatValue(), (int) (dpix * (x + 1.5)), dpix * y + graphics.getFont().getSize() / 2 - 1);
                    break;
                case WEST_EAST:
                    graphics.drawString("" + _windings2.getValue().floatValue(), dpix * x - graphics.getFontMetrics().stringWidth("" + _windings1.getValue().floatValue()) / 2, (int) (dpix * (y - 1.3)));
                    graphics.drawString("" + _windings1.getValue().floatValue(), dpix * x - graphics.getFontMetrics().stringWidth("" + _windings1.getValue().floatValue()) / 2, (int) (dpix * (y + 1.3)) + graphics.getFont().getSize());
                    break;
                case SOUTH_NORTH:
                    graphics.drawString("" + _windings2.getValue().floatValue(), (int) (dpix * (x - 1.5)) - graphics.getFontMetrics().stringWidth("" + _windings2.getValue().floatValue()), dpix * y + graphics.getFont().getSize() / 2 - 1);
                    graphics.drawString("" + _windings1.getValue().floatValue(), (int) (dpix * (x + 1.5)), dpix * y + graphics.getFont().getSize() / 2 - 1);
                    break;
            }

            graphics.setFont(origFont);
        }
        setTranslationRotation(graphics);
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        int pkd = (int) (0.4 * dpix);  // Durchmesser des Kopplungspunktes in Pixel
        double pk1 = 1.0, pk2 = -0.48;  // Punkt zur Markierung der eventuellen Kopplung mit anderen Spulen        
        graphics.fillOval((int) (dpix * (+pk2) - pkd / 2), (int) (dpix * (-1.4) - pkd / 2), pkd, pkd);
        graphics.fillOval((int) (dpix * (-pk2) - pkd / 2), (int) (dpix * (+_reversed.getValue() * 1.4) - pkd / 2), pkd, pkd);
    }

    @Override
    public boolean includeParentInSimulation() {
        return false;
    }

    @Override
    protected final Window openDialogWindow() {
        return new IdealTransformerDialog(this);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return AbstractCircuitBlockInterface.getCalculatorsFromSubComponents(this);
    }
}
