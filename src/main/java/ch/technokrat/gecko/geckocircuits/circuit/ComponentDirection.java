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

import ch.technokrat.gecko.geckocircuits.control.Point;

public enum ComponentDirection {

    NORTH_SOUTH(503),
    EAST_WEST(504),
    SOUTH_NORTH(501),
    WEST_EAST(502);
    int _oldOrdinal;

    ComponentDirection(int oldOrdinal) {
        _oldOrdinal = oldOrdinal;
    }

    public int code() {
        return _oldOrdinal;
    }

    static ComponentDirection getFromCode(final int code) {
        for (ComponentDirection val : ComponentDirection.values()) {
            if (val._oldOrdinal == code) {
                return val;
            }
        }
        return ComponentDirection.NORTH_SOUTH;
    }

    ComponentDirection nextOrientation() {
        switch (this) {
            case NORTH_SOUTH:
                return EAST_WEST;
            case EAST_WEST:
                return SOUTH_NORTH;
            case SOUTH_NORTH:
                return WEST_EAST;
            case WEST_EAST:
                return NORTH_SOUTH;
            default:
                assert false;

        }
        return ComponentDirection.NORTH_SOUTH;
    }
    
    public static ComponentDirection getDirection(final Point start, final Point end) {
        if (start.x == end.x) {
            if (start.y > end.y) {
                return SOUTH_NORTH;
            } else {
                return NORTH_SOUTH;
            }

        } else {
            if (start.x > end.x) {
                return EAST_WEST;
            } else {
                return WEST_EAST;
            }
        }
    }
    
    public boolean isHorizontal() {
        return this == WEST_EAST || this == EAST_WEST;
    }
}
