/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  terms of the GNU General Public License as published by the Free Software
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

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoSim;

/**
 * Integration tests for real circuit models.
 * Tests verify that example circuit files can be loaded and simulated successfully.
 */
public final class ModelResultsTest{
  private static final String MODELS_PATH = "resources/Topologies/";
  private static final String OPAMP_PATH = "resources/OpAmp/";

  @BeforeClass
  public static void setUpClass(){
    GeckoSim._isTestingMode = true;
    GeckoSim.main(new String[]{});
  }

  @Before
  public void setUp(){
    // Reset state between tests
    GeckoSim._testSuccessful = false;
  }

  @After
  public final void tearDown(){
    // Cleanup between tests - allow time for state to reset
    try {
      Thread.sleep(100);
    } catch (InterruptedException ex) {
      // Ignore
    }
  }

  @Test
  public void threePhaseVSRTest(){
    openRunAssert("ThreePhase-VSR_10kW_thermal.ipes");
  }

  @Test
  public void buckBoostThermal(){
    openRunAssert("BuckBoost_thermal.ipes");
  }

  @Test
  public void thyristorControlAndParameters(){
    openRunAssert("ThyristorControlBlock.ipes");
  }

  @Test
  public void opAmp(){
    openRunAssert(OPAMP_PATH + "OpAmp.ipes");
  }

  @Test
  public void thyristorCoupling(){
    openRunAssert("ThyristorCoupling.ipes");
  }

  public static void openRunAssert(String fileName){
    try{
      Thread.sleep(10);
      String filePath = fileName.startsWith("resources/") ? fileName : MODELS_PATH + fileName;
      File file = new File(filePath);
      if(!file.exists()){
        System.err.println("could not open file:  " + file.getAbsolutePath());
      }
      GeckoExternal.openFile(file.getAbsolutePath());
      GeckoExternal.runSimulation();

      // Verify simulation completed successfully
      double simTime = GeckoExternal.getSimulationTime();
      assertTrue("Simulation time should be positive", simTime > 0);

      // Verify we can get results
      String[] circuitElements = GeckoExternal.getCircuitElements();
      assertNotNull("Circuit elements should not be null", circuitElements);
      assertTrue("Circuit should have elements", circuitElements.length > 0);

      System.out.println("Successfully simulated: " + fileName);
    }catch(InterruptedException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("Test interrupted: " + ex.getMessage());
    }catch(RemoteException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("Remote exception: " + ex.getMessage());
    }catch(FileNotFoundException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("File not found: " + ex.getMessage());
    } catch(Exception ex) {
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("Unexpected exception: " + ex.getMessage());
    }
  }
}
