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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.NothingToDoCalculator;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPotentialMeasurement extends RegelBlock implements ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupable, ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable {

    final PotentialCoupling _potentialLabelCoupling;
    final ComponentCoupling _componentCoupling = new ComponentCoupling(1, this, new int[]{2});
    final ConnectorType _measureConnectorType;

    public AbstractPotentialMeasurement(final ConnectorType connectorType) {
        super(0, 1);
        _measureConnectorType = connectorType;
        _potentialLabelCoupling = new PotentialCoupling(this, new int[]{0, 1}, connectorType);
    }

    @Override
    public final ComponentCoupling getComponentCoupling() {
        return _componentCoupling;
    }

    @Override
    public final PotentialCoupling getPotentialCoupling() {
        return _potentialLabelCoupling;
    }    

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {        
        return new NothingToDoCalculator(0, 1);
    }

    @Override
    protected final void addTextInfoParameters() {
        super.addTextInfoParameters();
        
        final AbstractCircuitBlockInterface coupledElement = (AbstractCircuitBlockInterface) getComponentCoupling()._coupledElements[0];
        final String label1 = getPotentialCoupling().getLabels()[0];
        final String label2 = getPotentialCoupling().getLabels()[1];
        
        if (SchematischeEingabe2._controlDisplayMode.showParameter) {
            String parStr = label1 + " @ " + label2;
            if ((label1.isEmpty() || label2.isEmpty())
                    && (coupledElement == null)) {
                parStr = "not defined";
                _textInfo.addErrorValue(parStr);
            } else {
                if (coupledElement != null) {
                    parStr = coupledElement.getStringID();
                }
                _textInfo.addParameter(parStr);
            }
        }
    }

    @Override
    protected final Window openDialogWindow() {
        return new ReglerVOLTDialog(this);
    }
    
    public final void checkComponentCompatibility(Object testObject, List<AbstractBlockInterface> insertList) {
        if(testObject instanceof DirectVoltageMeasurable) {
            DirectVoltageMeasurable voltMeas = (DirectVoltageMeasurable) testObject;
            for(AbstractBlockInterface comp : voltMeas.getDirectVoltageMeasurementComponents(_measureConnectorType)) {
                insertList.add(comp);
            }            
        }
    }
    
    @Override 
    public List<OperationInterface> getOperationEnumInterfaces() {
        List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.addAll(getComponentCoupling().getOperationInterfaces());
        returnValue.addAll(_potentialLabelCoupling.getOperationInterfaces());
        return returnValue;
    }
}
