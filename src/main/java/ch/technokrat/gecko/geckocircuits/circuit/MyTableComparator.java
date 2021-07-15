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
        if(o1.get(0) == null) {
            return 1;
        }
        if(o2.get(0) == null) {
            return -1;            
        }
        if(o1.get(0) == null && o2.get(0) == 0) {
            return 0;
        }
        if(o1.get(0) < o2.get(0)) {
            return -1;
        }
        
        if(o1.get(0) > o2.get(0)) {
            return 1;
        }
        
        if((double) o1.get(0) == (double) o2.get(0)) {
            return 0;
        }
                
        assert false : o1;
        return -1;
    }

    
    
}
