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
package gecko.geckocircuits.newscope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author andy
 */
public final class DiagramCurve extends AbstractDiagram {
    static final String DIAGRAM_TYPE_STRING = "DiagramCurve";

    public DiagramCurve(final GraferV4 grafer) {
        super(grafer, new DiagramSettings());        
    }

    public DiagramCurve(final AbstractDiagram oldDiagram) {
        super(oldDiagram._grafer, new DiagramSettings());        
    }
    
    @Override
    List<AbstractCurve> getCurvesCopy(final List<AbstractCurve> oldCurves) {        
        final List<AbstractCurve> newCurves = new ArrayList<AbstractCurve>();
        for (AbstractCurve curve : oldCurves) {
            newCurves.add(new CurveRegular(curve, this));
        }
        return Collections.unmodifiableList(newCurves);
    }
    
    @Override
    AbstractCurve curveFabric() {
        return new CurveRegular(this);
    }

    @Override
    double getDiagramYWeight() {
        return _diagramSettings.getWeightDiagram();
    }

    @Override
    String getDiagramTypeString() {
        return DIAGRAM_TYPE_STRING;                
    }

    
    
    
}
