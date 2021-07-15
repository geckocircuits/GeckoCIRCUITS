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
import javax.swing.JCheckBox;
import ch.technokrat.modelviewcontrol.IGenericMVCView;
import ch.technokrat.modelviewcontrol.ModelMVC;

/**
 *
 * @param <M>
 * @author andy
 */
public class DelegateCheckBox<M extends ModelMVC<Boolean>> extends JCheckBox
    implements IGenericMVCView<M>, ActionListener {
        private static final long serialVersionUID = 159473276254167L;
        private ModelMVC<Boolean> _model;
    private ActionListener _listener;

     /**
     *
     * @param model
     */
    @Override
    public void registerModel(M model, String undoRedoText) {
        this.setSelected(model.getValue());
        _model = model;
        _model.addModelListener(this);

        _listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                _model.setValue(isSelected());
            }
        };

        addActionListener(_listener);

    }

    @Override
    public void unregisterModel() {
        if(_model != null) {
            _model.removeModelListener(this);
            _model = null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        this.setSelected(_model.getValue());
    }



}
