/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ThermPvChip;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossComponent;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.Arrays;
import java.util.List;


public final class ReglerFlowMeter extends AbstractCurrentMeasurement {        
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerFlowMeter.class, "FLOW", I18nKeys.HEATFLOW_MEASUREMENT_W);
    
    private LossComponent _measurementType = LossComponent.TOTAL;
    
    @Override
    public String[] getOutputNames() {
        return new String[]{"Pmeas"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MEASURED_HEAT_FLOW_W};
    }

    @Override
    String getVariableForDisplay() {
        final AbstractBlockInterface coupled = (AbstractCircuitBlockInterface) _coupling._coupledElements[0];
        String display;
        if (coupled instanceof ThermPvChip) {
            switch (_measurementType) {
                case CONDUCTION:
                    display = "pv-cond";
                    break;
                case SWITCHING:
                    display = "pv-swtch";
                    break;
                case TOTAL:
                default:
                    display = "pv";
            }
        } else {
            display = "pv";
        }
        return display;
    }            

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.SELECT_THERMAL_COMPONENT;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_THERMAL_COMPONENTS_DETECTED;        
    }
    
    @Override
    public void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {                
        if(testObject instanceof CurrentMeasurable) {
            final CurrentMeasurable curMeas = (CurrentMeasurable) testObject;
            if(((AbstractCircuitBlockInterface) curMeas).getSimulationDomain() != ConnectorType.THERMAL) {
                return;
            }
            insertList.addAll(Arrays.asList(curMeas.getCurrentMeasurementComponents(ConnectorType.THERMAL)));
        }
        
    }
    
    public LossComponent getLossComponentBeingMeasured() {
        return _measurementType;
    }
    
    public void setLossComponentBeingMeasured(final LossComponent lossComponentToMeasure) {
        _measurementType = lossComponentToMeasure;
    }
    
    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii);
        
        DatenSpeicher.appendAsString(ascii.append("\nlosscomp"), _measurementType.getSaveString());
    }
    
    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        if (tokenMap.containsToken("losscomp")) {
            String measurementComponent = "";
            measurementComponent = tokenMap.readDataLine("losscomp", measurementComponent);
            _measurementType = LossComponent.getEnumFromSaveString(measurementComponent);
        } else {
            _measurementType = LossComponent.TOTAL;
        }
    }
}
