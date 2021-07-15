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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

/**
 * this ugly class is just (and still) used from the "old" geckocircuits solver.
 * Remove this, when the new solver is finished. CircuitSourceType should be a
 * complemte replacement of this one.
 *
 * @author andy
 */
public class SourceType {    
    
    public static final int QUELLE_CURRENTCONTROLLED_DIRECTLY = 396;
    public static final int QUELLE_CURRENTCONTROLLED_DIRECTLY_NEW = 3;
    
    public static final int QUELLE_DIDTCURRENTCONTROLLED = 397;
    public static final int QUELLE_DIDTCURRENTCONTROLLED_NEW = 4;
    
    public static final int QUELLE_VOLTAGECONTROLLED_DIRECTLY = 399;
    public static final int QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW = 5;
    
    public static final int QUELLE_VOLTAGECONTROLLED_TRANSFORMER = 398;
    public static final int QUELLE_VOLTAGECONTROLLED_TRANSFORMER_NEW = 6;
    
    public static final int QUELLE_SIGNALGESTEUERT = 400;
    public static final int QUELLE_SIGNALGESTEUERT_NEW = 2;
    
    public static final int QUELLE_DC = 401;
    public static final int QUELLE_DC_NEW = 0;
    
    public static final int QUELLE_SIN = 402;
    public static final int QUELLE_SIN_NEW = 1;
}
