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



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;
import ch.technokrat.modelviewcontrol.IGenericMVCView;
import ch.technokrat.modelviewcontrol.ModelMVC;

/**
 *
 * @param <M>
 * @author andy
 */
public class DelegateIntSpinner<M extends ModelMVC<Integer>> extends JSpinner
        implements IGenericMVCView<M>, ActionListener {

    private static final long serialVersionUID = 759473276284147L;
    private ModelMVC<Integer> _model;
    private ChangeListener _changeListener;

    /**
     *
     * @param integer Model
     */
    @Override
    public void registerModel(M integerModel, String undoRedoText) {
        assert integerModel != null;
        this.setValue(integerModel.getValue());
        _model = integerModel;
        _model.addModelListener(this);


        _changeListener = new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                if(_model != null) {
                    _model.setValue(getIntegerValue());
                }
            }
        };
        addChangeListener(_changeListener);
    }

    @Override
    public void unregisterModel() {
        if(_model!= null) {
            _model.removeModelListener(this);
            _model = null;
        }
    }

    public Integer getIntegerValue() {
        return (Integer) (super.getValue());
    }

    /**
     *
     * @param evt
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        this.setValue(_model.getValue());
    }
}