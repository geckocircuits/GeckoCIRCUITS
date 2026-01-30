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
package ch.technokrat.gecko.geckocircuits.circuit;

import java.util.Comparator;
import java.util.List;

/**
 *
 * @author andy
 */
class MyTableComparator implements Comparator<List<Double>> {

    public MyTableComparator() {
    }

    @Override
    public int compare(List<Double> o1, List<Double> o2) {
        assert o1.size() == o2.size();
        Double d1 = o1.get(0);
        Double d2 = o2.get(0);

        if(d1 == null && d2 == null) {
            return 0;
        }
        if(d1 == null) {
            return 1;
        }
        if(d2 == null) {
            return -1;
        }

        // Use Double.compare() to correctly handle NaN and infinity
        return Double.compare(d1, d2);
    }

    
    
}
