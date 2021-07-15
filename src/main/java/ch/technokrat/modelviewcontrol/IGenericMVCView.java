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
package ch.technokrat.modelviewcontrol;


import ch.technokrat.modelviewcontrol.ModelMVCGeneric;

/**
 *
 * @param <L>
 * @author andy
 */
public interface IGenericMVCView <L extends ModelMVCGeneric> {
    /**
     * 
     * @param pointModel
     * @param undoRedoText
     */
    abstract public void registerModel(final L pointModel, String undoRedoText);
    /**
     * 
     */
    abstract public void unregisterModel();
}
