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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author andreas
 */
public final class ScopeWrapperIndices {

    private final List<Integer> _originalGlobalIndices;
    private final List<Integer> _globalIndices = new ArrayList<Integer>();
    private final List<AbstractDataContainer> _indexedDataContainers = new ArrayList<AbstractDataContainer>();
    private final DataContainerGlobal _globalDataContainer;
    
    
    public ScopeWrapperIndices(final List<Integer> globalIndices, final DataContainerGlobal globalDataContainer) {        
        _originalGlobalIndices = Collections.unmodifiableList(globalIndices);
        _globalDataContainer = globalDataContainer;
        for (int index = 0; index < globalIndices.size(); index++) {
            _indexedDataContainers.add(globalDataContainer);
        }
    }

    public int getContainerRowIndex(final int row) {
        if(row < _globalIndices.size()) {
            return _globalIndices.get(row);
        } else {
            return 0;
        }
        
    }

    public void reset() {
        _globalIndices.clear();
        _globalIndices.addAll(_originalGlobalIndices);
        _indexedDataContainers.clear();
        for (int index = 0; index < _globalIndices.size(); index++) {
            _indexedDataContainers.add(_globalDataContainer);
        }
    }

    public AbstractDataContainer getDataContainer(final int row) {
            return _indexedDataContainers.get(row);
    }

    int getTotalSignalNumber() {
        return _globalIndices.size();
    }

    public void defineAdditionalSignal(final AbstractDataContainer newContainer, final int rowInsertPositon,
            final int containerRowIndex) {
        _indexedDataContainers.add(rowInsertPositon, newContainer);
        _globalIndices.add(rowInsertPositon, containerRowIndex);
    }

    public void deleteSignal(final int row) {
        _indexedDataContainers.remove(row);
        _globalIndices.remove(row);
    }
}
