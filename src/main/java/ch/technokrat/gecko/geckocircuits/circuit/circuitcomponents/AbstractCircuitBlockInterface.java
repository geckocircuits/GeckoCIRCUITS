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

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import static ch.technokrat.gecko.geckocircuits.circuit.ConnectorType.CONTROL;
import ch.technokrat.gecko.geckocircuits.circuit.ElementDisplayProperties;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCircuitBlockInterface extends AbstractBlockInterface {

    static final TechFormat tcf = new TechFormat();
    static final int ARROW_LENGTH = 11, ARROW_WIDTH = 3;  // Symbol-Pfeil fuer Flussrichtung --> Pfeilspitzenabmessung
    
    public double _currentInAmps;
    public double _voltage;        

    AbstractCircuitBlockInterface() {
        // package-private constructor!!!
    }    
    
    /**
     * Careful: this is the default behavior of CurrentMeasurable interface.
     * However, some components don't implement this interface (E.g. Motors,
     * Terminals, ...);
     *
     * @return
     */
    public AbstractCircuitBlockInterface[] getCurrentMeasurementComponents(final ConnectorType connectorType) {
        if (getSimulationDomain() == connectorType) {
            return new AbstractCircuitBlockInterface[]{this};
        } else {
            return new AbstractCircuitBlockInterface[]{};
        }


    }

    @Override
    protected void paintIndividualComponent(final Graphics2D graphics) {
        setTranslationRotation(graphics);
        graphics.setColor(getForeGroundColor());
        drawConnectorLines(graphics);
        graphics.setColor(getBackgroundColor());
        drawBackground(graphics);
        graphics.setColor(getForeGroundColor());
        drawForeground(graphics);
        restoreOrigTransformation(graphics);
        graphics.setColor(getForeGroundColor());
    }

    public AbstractBlockInterface[] getDirectVoltageMeasurementComponents(final ConnectorType connectorType) {
        if (getSimulationDomain() == connectorType) {
            return new AbstractBlockInterface[]{this};
        } else {
            return new AbstractBlockInterface[]{};
        }

    }

    @Override
    public final int istAngeklickt(final int mouseX, final int mouseY) {
        if ((getAussenabmessungenRechteckEckpunkte()[0] <= mouseX)
                && (mouseX <= getAussenabmessungenRechteckEckpunkte()[2])
                && (getAussenabmessungenRechteckEckpunkte()[1] <= mouseY)
                && (mouseY <= getAussenabmessungenRechteckEckpunkte()[3])) {
            return 1;
        } else {
            return 00;
        }
    }

    // fuer das MarkierungsRechteck - Aussenabmessungen sind Element-umfassendes Rechteck
    // --> new int[]{xLinksOben, yLinksOben, xRechtsUnten, yRechtsUnten} in 'echten' PixelPunkt-Koord.
    @Override
    public final int[] getAussenabmessungenRechteckEckpunkte() {
        return new int[]{dpix * (getSheetPosition().x - 1), dpix * (getSheetPosition().y - 1), dpix * (getSheetPosition().x + 1),
            dpix * (getSheetPosition().y + 1)};
    }  // muss individuell implementiert werden        

    public final CircuitTyp getCircuitTyp() {
        return (CircuitTyp) getTypeEnum();
    }

    /**
     * the background of all components is drawn first, so that it does not
     * overlap foreground elements.
     *
     * @param graphics
     */
    @SuppressWarnings("PMD")
    protected void drawBackground(final Graphics2D graphics) {
        // this is not always used... overwrite if required!
    }

    
    
    
    /**
     * the foreground is painted after the background. E.g. when you like to
     * draw text strings, they should not be covered by the background of other
     * elements.
     *
     * @param graphics
     */
    abstract void drawForeground(final Graphics2D graphics);

    /**
     * The lines from the terminal point to somewhere internally
     *
     * @param graphics
     */
    abstract void drawConnectorLines(final Graphics2D graphics);

    @Override
    public final ElementDisplayProperties getDisplayProperties() {
        switch (getSimulationDomain()) {
            case LK:
            case RELUCTANCE:
            case LK_AND_RELUCTANCE:
                return SchematischeEingabe2._lkDisplayMode;
            case THERMAL:
                return SchematischeEingabe2._thermDisplayMode;
            case CONTROL:
                assert false;
                break;
            default:
                assert false : "ciruit domain: " + getSimulationDomain() + " " + this + " " + this.getClass();
                break;
        }
        return null;
    }

    public final String[] getAccessibleParameterUnits() {
        String[] returnValue = new String[parameter.length];
        for (int i = 0; i < returnValue.length; i++) {
            returnValue[i] = "";
        }
        for (UserParameter par : getRegisteredParameters()) {
            returnValue[par.getParameterIndex()] = par.getUnit();
        }
        return returnValue;
    }
        
    public void setzeParameterZustandswerteAufNULL() {
    }
    
    public List<String> getParameterStringIntern() {
        return Collections.EMPTY_LIST;
    }
    
    public abstract List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart();
    
    static List<? extends CircuitComponent> getCalculatorsFromSubComponents(final HiddenSubCircuitable subCircuitable) {
        Collection<? extends AbstractBlockInterface> hiddenSubs = subCircuitable.getHiddenSubCircuitElements();
        List<CircuitComponent> returnValue = new ArrayList<CircuitComponent>();
        for(AbstractBlockInterface block : hiddenSubs) {
            assert block instanceof AbstractCircuitBlockInterface;
            returnValue.addAll(((AbstractCircuitBlockInterface)block).getCircuitCalculatorsForSimulationStart());
        }
        return returnValue;
    }        
}
