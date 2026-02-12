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
package gecko.geckocircuits.nativec;

/**
 * In fact, this class represents two red triangles: increase terminal number and decrease!
 * In the future, whe should split this / refactor
 * @author andreas
 */
class ReglerJavaTriangles {
       public int _xKlickMinTerminal, _xKlickMaxTerminal, _yKlickMinTerminalADD, _yKlickMaxTerminalADD,
            _yKlickMinTerminalSUB, _yKlickMaxTerminalSUB;  // Klickbereiche fuer rote Dreiecke --> Aenderung der Terminal-Anzahl

    boolean isIncreaseClicked(final int mouseX, final int mouseY) {
        return _xKlickMinTerminal <= mouseX && mouseX <= _xKlickMaxTerminal
                && _yKlickMinTerminalADD <= mouseY && mouseY <= _yKlickMaxTerminalADD;
    }

    boolean isDecreaseClicked(int mouseX, int mouseY) {
        return _xKlickMinTerminal <= mouseX && mouseX <= _xKlickMaxTerminal
                && _yKlickMinTerminalSUB <= mouseY && mouseY <= _yKlickMaxTerminalSUB;
    }
}
