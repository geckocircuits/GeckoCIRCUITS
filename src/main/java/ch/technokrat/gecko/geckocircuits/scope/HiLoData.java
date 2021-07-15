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

package ch.technokrat.gecko.geckocircuits.scope;

/**
 * This is for backward-compatibility with old scope. In future, replace with "newscope":
 * @author andy
 */
@Deprecated
public class HiLoData {
    public float yLo = 1E30f;
    public float yHi = -1E30f;

    void insertCompare(float y) {
        yLo = Math.min(yLo, y);
        yHi = Math.max(yHi, y);

    }

    void insertCompare(HiLoData data) {
        if(data.yLo < 1E30f)
            yLo = Math.min(yLo, data.yLo);
        if(data.yHi > -1E30)
            yHi = Math.max(yHi, data.yHi);

    }

}
