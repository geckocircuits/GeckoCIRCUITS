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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import java.util.Random;

public class UniqueObjectIdentifer {

    private static Random generator = new Random();    
    private long identifier = 0;    

    public UniqueObjectIdentifer() {
        
    }
    
    public long getIdentifier() {
        //assert identifier != 0;
        return identifier;
    }
    
    public void createNewIdentifier() {
        assert identifier == 0;
        identifier = this.hashCode() + generator.nextInt();
    }
    
    public void createNewIdentifier(final long value) {        
        identifier = value;
    }

    public void importASCII(TokenMap tokenMap) {        
        if(tokenMap.containsToken("uniqueObjectIdentifier")) {
            identifier = tokenMap.readDataLine("uniqueObjectIdentifier", identifier);            
            if(identifier == 0) {
                identifier = this.hashCode() + generator.nextInt();
            }                                    
        } else {
            identifier = this.hashCode() + generator.nextInt();
        }
        
    }

    public void exportASCII(final StringBuffer ascii) {        
        DatenSpeicher.appendAsString(ascii.append("\nuniqueObjectIdentifier"), identifier);        
    }                
}
