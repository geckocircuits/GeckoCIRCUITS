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
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Collections;
import java.util.List;

/**
 * Magnetic coupling between coupable inductors
 *
 * @author andreas
 */
public final class MutualInductance extends AbstractCircuitBlockInterface implements ComponentCoupable {
    static final AbstractTypeInfo TYPE_INFO = 
            new CircuitTypeInfo(MutualInductance.class, "K", I18nKeys.MAGNETIC_COUPLING_K, I18nKeys.MAGNETIC_COUPLING_DESCRIPTION);
    
    private static final double WIDTH = 0.5;
    private static final double HEIGHT = 0.5;    
    private static final int STRING_K_Y_FRAC = 3;
    private static final double DEFAULT_COEFFICIENT = 0.98;
    private static final String K_STRING = "k";
    
    final UserParameter<Double> _couplingCoefficient = UserParameter.Builder.
            <Double>start("couplingCoefficient", DEFAULT_COEFFICIENT).
            longName(I18nKeys.MAGNETIC_COUPLING_K).
            shortName("k").
            unit("unitless").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).
            build();
        
    
    final UserParameter<Boolean> _showLines = UserParameter.Builder.
            <Boolean>start("showInductorLine", false).                                   
            longName(I18nKeys.SHOW_LINES).
            shortName("showLines").                        
            arrayIndex(this, -1).
            build();                               
    
    private final ComponentCoupling _componentCoupling = new ComponentCoupling(2, this, new int[]{0, 1});


    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        // Verbindungslinien zu den verkoppelten Induktivitaeten:        
        if (_showLines.getValue()) {
            Color origColor = graphics.getColor();
            graphics.setColor(Color.green);
            if (_componentCoupling._coupledElements[0] != null) {
                final InductorCoupable lkop = (InductorCoupable) _componentCoupling._coupledElements[0];
                graphics.drawLine(0, 0,
                        (int) (dpix * (lkop.getSheetPosition().x - getSheetPosition().x)),
                        (int) (dpix * (lkop.getSheetPosition().y - getSheetPosition().y)));  // Verbindungslinie zu L1
            }

            if (_componentCoupling._coupledElements[1] != null) {
                final InductorCoupable lkop = (InductorCoupable) (_componentCoupling._coupledElements[1]);
                graphics.drawLine(0, 0,
                        (int) (dpix * (lkop.getSheetPosition().x - getSheetPosition().x)),
                        (int) (dpix * (lkop.getSheetPosition().y - getSheetPosition().y)));  // Verbindungslinie zu L1
            }
            graphics.setColor(origColor);
        }
    }

    @Override // never rotate this component!
    public void setComponentDirection(final ComponentDirection orientierung) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }

    @Override
    public ComponentDirection getComponentDirection() {
        return ComponentDirection.NORTH_SOUTH;
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        graphics.fillRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT),
                (int) (dpix * (2 * WIDTH)), (int) (dpix * (2 * HEIGHT)));
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        graphics.drawRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * HEIGHT));
        
        final FontMetrics fontMetrics = graphics.getFontMetrics();
        final int stringWidth = fontMetrics.stringWidth(K_STRING);
        final int stringHeight = fontMetrics.getHeight();
        graphics.drawString("k", -stringWidth/2, stringHeight/STRING_K_Y_FRAC);
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();

        
        AbstractBlockInterface firstInductor = getComponentCoupling()._coupledElements[0];
        AbstractBlockInterface secondInductor = getComponentCoupling()._coupledElements[1];        
        
        if (firstInductor == null && secondInductor != null) {
            final String parStr = I18nKeys.NOT_DEFINED.getTranslation() + " @ " + secondInductor.getStringID();
            _textInfo.addErrorValue(parStr);
        }

        if (firstInductor != null && secondInductor == null) {
            final String parStr = firstInductor.getStringID() + " @ " + I18nKeys.NOT_DEFINED.getTranslation();
            _textInfo.addErrorValue(parStr);
        }        
        
        if (firstInductor == null && secondInductor == null) {            
            _textInfo.addErrorValue(I18nKeys.NOT_DEFINED.getTranslation());
        }
        
        if(firstInductor != null && secondInductor != null) {
            String parStr = firstInductor.getStringID() + " @ " + secondInductor.getStringID();
            _textInfo.addParameter(parStr);
        }                                
    }

    @Override
    public ComponentCoupling getComponentCoupling() {
        return _componentCoupling;
    }        

    
    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.LC_INDUCTOR_SELECTION;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_LC_FOUND;
    }

    @Override
    public void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {
        if (testObject instanceof InductorCoupable) {
            insertList.add((AbstractBlockInterface) testObject);
        }
    }

    @Override
    protected Window openDialogWindow() {
        return new MutualInductanceDialog(this);
    }                
    
    @Override
    public void setToolbarPaintProperties() {
        _componentCoupling.setNewCouplingElement(0, this);
        _componentCoupling.setNewCouplingElement(1, this);
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        // The mutual inductance does not have a calculator! The values are calculated via
        // the coupled inductors themselves!
        return Collections.EMPTY_LIST;
    }
    
    
    @Override 
    public List<OperationInterface> getOperationEnumInterfaces() {        
        return getComponentCoupling().getOperationInterfaces();
    }
    
}
