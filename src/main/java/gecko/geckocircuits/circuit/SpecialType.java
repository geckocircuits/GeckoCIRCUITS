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
package gecko.geckocircuits.circuit;

import gecko.geckocircuits.general.AbstractComponentType;
import gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import gecko.geckocircuits.control.TextFieldBlock;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Enum exposes type info for component configuration")
public enum SpecialType implements AbstractComponentType {
    SUBCIRCUIT(27, SubcircuitBlock.tInfo),
    TEXTFIELD(70, TextFieldBlock.tInfo);

    public static SpecialType getFromIntNumber(final int intNumber) {
        for(SpecialType compare : values()) {
            if(compare.getTypeNumber() == intNumber) {
                return compare;
            }
        }
        throw new IllegalArgumentException("Type with identifier: " + intNumber + " is not known!");
    }

    private int _intValue;
    private AbstractTypeInfo _typeInfo;

    SpecialType(final int initValue, final AbstractTypeInfo typeInfo) {
        _intValue = initValue;
        _typeInfo = typeInfo;
        _typeInfo.addParentEnum(this);
    }

    @Override
    public AbstractTypeInfo getTypeInfo() {
        return _typeInfo;
    }

    @Override
    public int getTypeNumber() {
        return _intValue;
    }


}
