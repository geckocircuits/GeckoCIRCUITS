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

/**
 *
 * @author andreas
 */
public enum ContainerStatus {
    /**
     * used as initial value
     */
    NOT_INITIALIZED,
    /**
     * data is inserted from a simulation/fourier calculatioin, etc.
     */
    RUNNING, 
    /**
     * no more simulation data will be inserted into the container.
     */
    FINISHED, 
    /**
     * 
     */
    PAUSED, 
    /**
     * used in global data-container, when old simulation results are invalidated/deleted. 
     */
    DELETED; 
}
