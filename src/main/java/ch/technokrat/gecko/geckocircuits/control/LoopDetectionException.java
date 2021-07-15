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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.technokrat.gecko.geckocircuits.control;

import java.util.List;

/**
 *
 * @author andy
 */
class LoopDetectionException extends Exception {

    private final ControlOrderNode _startBlock;
    private final List<ControlOrderNode> _loopList;

    public LoopDetectionException(ControlOrderNode firstComponent, List<ControlOrderNode> loopList) {
        _startBlock = firstComponent;
        _loopList = loopList;
    }

    boolean insertTestLoopFinished(ControlOrderNode nextNode) {
        if (_loopList.contains(nextNode)) {
            nextNode.setLoopCrackTrue();            
            return true;
        } else {
            _loopList.add(nextNode);
            return false;
        }
    }

    void printLoopMessage() {
//        System.err.println("Control loop detected:");
//        for (ControlOrderNode loopNode : _loopList) {
//            System.err.print(" " + loopNode.getElementControl().getStringID());
//        }
//        System.err.println("");
    }
}
