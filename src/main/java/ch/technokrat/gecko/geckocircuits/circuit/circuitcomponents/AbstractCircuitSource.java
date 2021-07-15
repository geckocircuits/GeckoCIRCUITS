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
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import static ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType.QUELLE_DC;
import static ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType.QUELLE_SIGNALGESTEUERT;
import static ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType.QUELLE_SIN;
import static ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface.tcf;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCircuitSource extends AbstractTwoPortLKreisBlock implements DirectVoltageMeasurable,
        ComponentCoupable, PotentialCoupable, CurrentMeasurable {

    static final double WIDTH = 0.9;
    static final double HEIGHT = 0.6;
    final UserParameter<Double> _dcValue = UserParameter.Builder.
            <Double>start("dcValue", 10.0).
            mapDomains(getDomains()).
            longName(I18nKeys.DC_VALUE_OF_SOURCE).
            shortName(getSortedDCValueShortNames()).
            addAlternativeShortName(getAlternativeDCValueShortNames()).
            unit(getSortedDomainUnits()).
            arrayIndex(this, 1).
            build();
    final UserParameter<CircuitSourceType> sourceType = UserParameter.Builder.
            <CircuitSourceType>start("sourceType", CircuitSourceType.QUELLE_DC).
            mapDomains(ConnectorType.LK, ConnectorType.RELUCTANCE, ConnectorType.THERMAL).
            longName(I18nKeys.TYPE_OF_SOURCE).
            shortName("type", "type", "type").
            unit("", "", "").
            arrayIndex(this, 0).
            build();
    final UserParameter<Double> frequency = UserParameter.Builder.
            <Double>start("frequency", 50.0).
            mapDomains(ConnectorType.LK, ConnectorType.RELUCTANCE, ConnectorType.THERMAL).
            longName(I18nKeys.FREQUENCY).
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            showWhenEnumValueIsSet(sourceType, CircuitSourceType.QUELLE_SIN).
            shortName("f", "f", "f").
            unit("Hz", "Hz", "Hz").
            arrayIndex(this, 2).
            build();
    final UserParameter<Double> phase = UserParameter.Builder.
            <Double>start("phaseDegrees", 0.0).
            mapDomains(ConnectorType.LK, ConnectorType.RELUCTANCE, ConnectorType.THERMAL).
            longName(I18nKeys.PHASE_SHIFT_OF_WAVEFORM).
            showInTextInfo(TextInfoType.SHOW_NON_NULL).
            showWhenEnumValueIsSet(sourceType, CircuitSourceType.QUELLE_SIN).
            shortName("phase", "phase", "phase").
            unit("degrees", "degrees", "degrees").
            arrayIndex(this, 4).
            build();
    final UserParameter<Double> directPotentialGain = UserParameter.Builder.
            <Double>start("directPotentialGain", 1.0).
            mapDomains(ConnectorType.LK, ConnectorType.RELUCTANCE, ConnectorType.THERMAL).
            longName(I18nKeys.GAIN_FOR_DIRECT_POTENTIAL_CONTROL).
            shortName("gain").
            showWhenEnumValueIsSet(sourceType, CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY).
            unit("", "", "").
            arrayIndex(this, 11).
            build();
    final UserParameter<Double> _didtInitialCurrent = UserParameter.Builder.
            <Double>start("didtInitialCurrent", 0.0).
            longName(I18nKeys.NON_ACCESSIBLE).
            shortName("non-accessibe").
            arrayIndex(this, 15).
            build();
    final UserParameter<Double> _offset = UserParameter.Builder.
            <Double>start("offset", 0.0).
            mapDomains(getDomains()).
            longName(I18nKeys.OFFSET_OF_WAVEFORM_FROM_ZERO).
            showInTextInfo(TextInfoType.SHOW_NON_NULL).
            showWhenEnumValueIsSet(sourceType, CircuitSourceType.QUELLE_SIN).
            shortName("offset").
            unit(getSortedDomainUnits()).
            arrayIndex(this, 3).
            build();
    final UserParameter<Double> _amplitude = UserParameter.Builder.
            <Double>start("amplitude", getDefaultAmplitudeValue()).
            mapDomains(getDomains()).
            longName(I18nKeys.PEAK_AMPLITUDE).
            shortName(getShortNamesForAmplitude()).
            unit(getSortedDomainUnits()).
            arrayIndex(this, 20).
            build();
    private final ComponentCoupling _compCoupling = new ComponentCoupling(1, this, new int[]{1});
    private final PotentialCoupling _potCoupling = new PotentialCoupling(this, new int[]{0}, ConnectorType.CONTROL);

    @Override
    public final void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {

        if (testObject == this) {
            return;
        }
        if (testObject instanceof DirectVoltageMeasurable
                && testObject instanceof AbstractCircuitBlockInterface) {

            final DirectVoltageMeasurable measurable = (DirectVoltageMeasurable) testObject;
            insertList.addAll(Arrays.asList(measurable.getDirectVoltageMeasurementComponents(getSimulationDomain())));

            if (getSimulationDomain() == ConnectorType.LK) {
                insertList.addAll(Arrays.asList(measurable.getDirectVoltageMeasurementComponents(ConnectorType.RELUCTANCE)));
            }

            if (getSimulationDomain() == ConnectorType.RELUCTANCE) {
                insertList.addAll(Arrays.asList(measurable.getDirectVoltageMeasurementComponents(ConnectorType.LK)));
            }
        }
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        if (!tokenMap.containsToken("phaseDegrees")) {
            phase.setValueWithoutUndo(Math.toDegrees(phase.getValue()));
        }

        if (!tokenMap.containsToken("amplitude")) { // file version <= 1.62
            _amplitude.setValueWithoutUndo(parameter[1]);
            if (!getNameOpt()[1].isEmpty()) {
                _amplitude.setNameOpt(getNameOpt()[1]);
            }
        }
    }

    @Override
    public final I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_USABLE_CIRCUIT_COMPONENT;
    }

    @Override
    public final I18nKeys getCouplingTitle() {
        return I18nKeys.DIRECT_POTENTIAL_CONTROL;
    }

    @Override
    public final ComponentCoupling getComponentCoupling() {
        return _compCoupling;
    }

    @Override
    public final PotentialCoupling getPotentialCoupling() {
        return _potCoupling;
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        graphics.drawOval((int) (-dpix * WIDTH), (int) (-dpix * WIDTH), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * WIDTH));
    }

    @Override
    protected final void drawBackground(final Graphics2D graphics) {
        graphics.fillOval((int) (-dpix * WIDTH), (int) (-dpix * WIDTH), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * WIDTH));
    }

    @Override
    protected final Window openDialogWindow() {
        return new AbstractCircuitSourceDialog(this);
    }

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        parameter[6] = 0;
        parameter[7] = 0;
        parameter[8] = 0;
        parameter[9] = 0;
        if (sourceType.getValue() == CircuitSourceType.QUELLE_SIGNALGESTEUERT) {
            parameter[1] = 0;
        }
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters(); //To change body of generated methods, choose Tools | Templates.
        addTextInfoErrorReference();

        if (!SchematischeEingabe2._lkDisplayMode.showParameter) {
            return;
        }

        switch (sourceType.getValue()) {
            case QUELLE_DC:
                String uTxt = getFixedIDString() + " dc = " + tcf.formatENG(_dcValue.getValue(), 2);
                if (nameOpt[1].isEmpty()) {
                    _textInfo.addParameter(uTxt);
                }
                break;
            case QUELLE_SIGNALGESTEUERT:
                _textInfo.addParameter("Sgn.-Contr.");
                break;
            case QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                _textInfo.addParameter("Dir.Pot.-Contr.");
                break;
            case QUELLE_SIN:
                if (nameOpt[1].isEmpty()) {
                    _textInfo.addParameter(" ac = " + tcf.formatENG(_amplitude.getValue(), 2));
                }
                break;
            default:
                assert false;
        }

        addDirectOrSignalTextInfo();

    }

    public void addTextInfoErrorReference() {
        if ((sourceType.getValue() == CircuitSourceType.QUELLE_SIGNALGESTEUERT)
                || (sourceType.getValue() == CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY)) {
            int index = -1;
            if (sourceType.getValue() == CircuitSourceType.QUELLE_SIGNALGESTEUERT) {
                final String couplingLabel = getPotentialCoupling().getLabels()[0];
                if (couplingLabel == null || couplingLabel.isEmpty()) {
                    _textInfo.addErrorValue("no control-sgn");
                }
            } else if (sourceType.getValue() == CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY) {
                if (getComponentCoupling()._coupledElements[0] == null) {
                    _textInfo.addErrorValue("no control-sgn");
                }
            }


        }
    }

    void addDirectOrSignalTextInfo() {
        String parStr = "";
        switch (sourceType.getValue()) {
            case QUELLE_SIGNALGESTEUERT:
                String potentialLabel = getPotentialCoupling().getLabels()[0];
                if (potentialLabel != null && !potentialLabel.isEmpty()) {
                    parStr = potentialLabel + " >>";
                }
                break;
            case QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                AbstractCircuitBlockInterface coupledComponent = (AbstractCircuitBlockInterface) getComponentCoupling()._coupledElements[0];

                if (coupledComponent != null) {                    
                    if (directPotentialGain.getNameOpt().isEmpty()) {                        
                        parStr = tcf.formatENG(directPotentialGain.getValue(), 2) 
                                + " * v(" + coupledComponent.getStringID() + ")";
                    } else {
                        parStr = nameOpt[11] + " * v(" + coupledComponent.getStringID() + ")";
                    }
                }
                break;
            default:
                return;
        }

        if (!parStr.isEmpty()) {
            _textInfo.addParameter(parStr);
        }

    }

    abstract String getUnitForDomain(ConnectorType connectorType);

    abstract ConnectorType[] getDomains();

    abstract String[] getShortNamesForAmplitude();

    abstract double getDefaultAmplitudeValue();

    abstract String getDCValueShortNameFromDomain(ConnectorType connectorType);

    //CHECKSTYLE:OFF
    protected String[] getAlternativeDCValueShortNames() {
        return new String[0];
    }
    //CHECKSTYLE:ON

    final String[] getSortedDCValueShortNames() {
        ConnectorType[] domains = getDomains();
        String[] returnValue = new String[domains.length];
        for (int i = 0; i < domains.length; i++) {
            returnValue[i] = getDCValueShortNameFromDomain(domains[i]);
        }
        return returnValue;
    }

    final String[] getSortedDomainUnits() {
        ConnectorType[] domains = getDomains();
        String[] returnValue = new String[domains.length];
        for (int i = 0; i < domains.length; i++) {
            returnValue[i] = getUnitForDomain(domains[i]);
        }
        return returnValue;
    }
    
    @Override 
    public List<OperationInterface> getOperationEnumInterfaces() {
        List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.addAll(getComponentCoupling().getOperationInterfaces());
        returnValue.addAll(_potCoupling.getOperationInterfaces());
        return returnValue;
    }
}
