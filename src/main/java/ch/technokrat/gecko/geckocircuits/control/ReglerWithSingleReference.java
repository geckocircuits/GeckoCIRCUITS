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

import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.List;

public abstract class ReglerWithSingleReference extends RegelBlock implements ComponentCoupable {

    final ComponentCoupling _coupling = new ComponentCoupling(1, this, new int[]{0});

    public ReglerWithSingleReference(final int noInputs, final int noOutputs) {
        super(noInputs, noOutputs);
    }

    @Override
    public final ComponentCoupling getComponentCoupling() {
        return _coupling;
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();

        if (!SchematischeEingabe2._controlDisplayMode.showParameter) {
            return;
        }

        if (_coupling._coupledElements[0] == null) {
            _textInfo.addErrorValue(I18nKeys.NOT_DEFINED.getTranslation());
        } else {
            _textInfo.addParameter(getDisplayValueWithoutError());
        }
    }

    abstract String getDisplayValueWithoutError();
    
    
    @Override 
    public List<OperationInterface> getOperationEnumInterfaces() {
        return getComponentCoupling().getOperationInterfaces();
    }
}
