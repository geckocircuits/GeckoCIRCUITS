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

/**
 * IMMUTABLE Point class!
 * @author andreas
 */
public class Point {
    public final int x;
    public final int y;
    public Point(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Point other = (Point) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.x;
        hash = 89 * hash + this.y;
        return hash;
    }

    @Override
    public String toString() {
        return x + " " + y;
    }        
    
    public double distance(Point otherPoint) {
        return Math.sqrt((x - otherPoint.x) * (x - otherPoint.x) + (y - otherPoint.y) * (y - otherPoint.y));
    }
    
}
