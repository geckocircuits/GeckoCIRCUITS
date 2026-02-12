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
package gecko;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andy
 */
public class GeckoRemoteObjectTest {

    public static void main(String[] args) {                
        GeckoRemoteObject tester = GeckoRemoteObject.connectToExistingInstance(43035);
        tester.allowAdditionalClients(1);                

        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(GeckoRemoteObjectTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("getting from remote " + tester.getDiodes());            
        }

    }
}
