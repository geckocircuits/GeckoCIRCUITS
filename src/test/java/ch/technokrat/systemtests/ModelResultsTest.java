/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.systemtests;

import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoSim;
import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.*;
import org.junit.BeforeClass;

public final class ModelResultsTest{
  private static final String MODELS_PATH = "TestModels/";

  @BeforeClass
  public static void setUpClass(){
    GeckoSim._isTestingMode = true;
    GeckoSim.main(new String[]{});
  }

  @Before
  public void setUp(){
    GeckoSim._testSuccessful = false;
  }

  @Test
  @Ignore
  public void threePhaseVSRTest(){
    openRunAssert("ThreePhase-VSR_10kW_thermal.ipes");
  }

  @Test
  @Ignore
  public void buckBoostThermal(){
    openRunAssert("BuckBoost_thermal.ipes");
  }

  @Test
  @Ignore
  public void thyristorControlAndParameters(){
    openRunAssert("ThyristorControlBlock.ipes");
  }

  @Test
  @Ignore
  public void opAmp(){
    openRunAssert("OpAmp.ipes");
  }

  @Test
  @Ignore
  public void thyristorCoupling(){
    openRunAssert("ThyristorCoupling.ipes");
  }

  public static void openRunAssert(String fileName){
    try{
            Thread.sleep(10);
      File file = new File(MODELS_PATH + fileName);
      if(!file.exists()){
        System.err.println("could not open file:  " + file.getAbsolutePath());
      }
      GeckoExternal.openFile(file.getAbsolutePath());
      GeckoExternal.runSimulation();
      assertTrue(GeckoSim._testSuccessful);
    }catch(InterruptedException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
    }catch(RemoteException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
    }catch(FileNotFoundException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @After
  public final void tearDown(){
  }
}
