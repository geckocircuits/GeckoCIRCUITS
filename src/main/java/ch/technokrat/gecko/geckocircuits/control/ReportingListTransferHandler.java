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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public final class ReportingListTransferHandler extends TransferHandler {

    private DataFlavor _locArrayLstFlvr;
    private final DataFlavor _serArrayLstFlvr;
    private final String _locArrayListType = DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList";
    private JList _source = null;
    private int[] _indices = null;
    private int _addIndex = -1; //Location where items were added
    private int _addCount = 0;  //Number of items added

    public ReportingListTransferHandler() {
        super();
        try {
            _locArrayLstFlvr = new DataFlavor(_locArrayListType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        _serArrayLstFlvr = new DataFlavor(List.class, "ArrayList");
    }

    @Override
    public boolean importData(final JComponent comp, final Transferable transf) {

        if (!canImport(comp, transf.getTransferDataFlavors())) {
            return false;
        }

        final JList target = (JList) comp;
        final List alist = getArrayList(transf);
        
        if (alist == null) {
            return false;
        }


        //At this point we use the same code to retrieve the data
        //locally or serially.

        //We'll drop at the current selected index.
        int index = target.getSelectedIndex();
        //Prevent the user from dropping data back on itself.
        //For example, if the user is moving items #4,#5,#6 and #7 and
        //attempts to insert the items after item #5, this would
        //be problematic when removing the original items.
        //This is interpreted as dropping the same data on itself
        //and has no effect.

        if (abortDueToSelfDrop(target, index)) {
            return true;
        }


        final DefaultListModel listModel = (DefaultListModel) target.getModel();
        final int max = listModel.getSize();

        if (index < 0) {
            index = max;
        }

        index = Math.min(max, index);

        if (_source.equals(target)) {
            _addIndex = index;
        }

        _addCount = alist.size();

        for (int i = 0; i < alist.size(); i++) {
            listModel.add(index++, alist.get(i));
        }
        return true;
    }

    @Override
    protected void exportDone(final JComponent comp, final Transferable data, final int action) {
        if (_addCount < 1) {
            return;
        }

        if ((action == MOVE) && (_indices != null)) {
            final DefaultListModel model = (DefaultListModel) _source.getModel();

            //If we are moving items around in the same list, we
            //need to adjust the indices accordingly since those
            //after the insertion point have moved.            
            for (int i = 0; i < _indices.length; i++) {
                if ((_indices[i] > _addIndex) && (_addIndex != -1)) {
                    _indices[i] += _addCount;
                }
            }
            for (int i = _indices.length - 1; i >= 0; i--) {
                model.remove(_indices[i]);
            }

        }
        _addIndex = -1;
        _addCount = 0;
    }

    private boolean hasLocalArrayListFlavor(final DataFlavor[] flavors) {
        if (_locArrayLstFlvr == null) {
            return false;
        }

        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].equals(_locArrayLstFlvr)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSerialArrayListFlavor(final DataFlavor[] flavors) {
        if (_serArrayLstFlvr == null) {
            return false;
        }

        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].equals(_serArrayLstFlvr)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canImport(final JComponent comp, final DataFlavor[] flavors) {
        if (hasLocalArrayListFlavor(flavors)) {
            return true;
        }
        if (hasSerialArrayListFlavor(flavors)) {
            return true;
        }
        return false;
    }

    @Override
    protected Transferable createTransferable(final JComponent comp) {
        if (comp instanceof JList) {
            _source = (JList) comp;
            _indices = _source.getSelectedIndices();

            final Object[] values = _source.getSelectedValues();
            if (values == null || values.length == 0) {
                return null;
            }

            final List<Object> alist = new ArrayList<Object>(values.length);
            for (int i = 0; i < values.length; i++) {
                final Object obj = values[i];
                alist.add(obj);
            }
            return new ReportingListTransferable(alist);
        }
        return null;
    }

    @Override
    public int getSourceActions(final JComponent comp) {
        return COPY_OR_MOVE;
    }

    private List getArrayList(final Transferable transf) {
        List returnValue;
        try {
            if (hasLocalArrayListFlavor(transf.getTransferDataFlavors())) {
                returnValue = (ArrayList) transf.getTransferData(_locArrayLstFlvr);
            } else if (hasSerialArrayListFlavor(transf.getTransferDataFlavors())) {
                returnValue = (ArrayList) transf.getTransferData(_serArrayLstFlvr);
            } else {
                return null;
            }
        } catch (UnsupportedFlavorException ufe) {
            return null;
        } catch (IOException ioe) {
            return null;
        }
        return returnValue;
    }

    private boolean abortDueToSelfDrop(final JList target, final int index) {
        return _source.equals(target)
                && _indices != null && index > _indices[0]
                && index <= _indices[_indices.length - 1];
    }

    private final class ReportingListTransferable implements Transferable {

        final List _data;

        public ReportingListTransferable(final List alist) {
            _data = alist;
        }

        @Override
        public Object getTransferData(final DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return _data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{_locArrayLstFlvr,
                        _serArrayLstFlvr};
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            if (_locArrayLstFlvr.equals(flavor)) {
                return true;
            }
            if (_serArrayLstFlvr.equals(flavor)) {
                return true;
            }
            return false;
        }
    }
}
