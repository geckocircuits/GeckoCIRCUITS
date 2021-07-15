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
import javax.swing.JTextField;

public class DelegateNumericTextField <M extends ModelMVC<Double>> extends JTextField
        implements IGenericMVCView<M>, ActionListener {
        private static final long serialVersionUID = 759956473825447L;
        private ActionListener _listener;
        private ModelMVC<Double> _model;


        public DelegateNumericTextField() {
            this.setText("0.0");
        }

    @Override
    public void registerModel(M textModel, String undoRedoText) {
        assert textModel != null;
        setText(textModel.getValue().toString());

        textModel.addModelListener(this);
        _model = textModel;

        _listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _model.setValue(Double.parseDouble(getText()));
            }
        };

        addActionListener(_listener);


    }

    @Override
    public void unregisterModel() {
        removeActionListener(_listener);
        if(_model != null) {
            _model.removeModelListener(this);
            _model = null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        setText(_model.getValue().toString());
    }
    
    public void saveValue() {
        _model.setValue(Double.parseDouble(getText()));
    }




}