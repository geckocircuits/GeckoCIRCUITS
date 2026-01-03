# Java Block Code - ThreePhase-VSR_10kW_thermal_with_java.ipes

## Problem

The Java block in this circuit file uses a **test/validation condition**:

```java
if(time == 0.019999500000009673) {  // Exact time check
    // Test code...
}
// No else clause!
return yOUT;  // Returns undefined when test doesn't run
```

**Issue:**
- Simulation time varies continuously (0.0, 0.001, 0.002, ...)
- Time will **never** equal exactly `0.019999500000009673`
- Therefore, test code never executes
- Block returns undefined/empty outputs
- Simulation fails or produces incorrect results

## Solution Options

### Option 1: Remove Test Code (Recommended for Normal Simulation)

Replace the code with:

```java
counter = (int) (xIN[0] + xIN[1] + xIN[2]);

maxLossIGBT = Math.max(xIN[3], maxLossIGBT);
maxLossDiode = Math.max(xIN[4], maxLossDiode);

// Just calculate and return - no test condition
yOUT[0] = maxLossIGBT;
yOUT[1] = maxLossDiode;
yOUT[2] = xIN[0];  // Example: pass through or calculate as needed
yOUT[3] = xIN[1];
// ... adjust for your outputs
```

### Option 2: Modify Test Check (Keep Test Functionality)

Change condition to allow normal simulation after test:

```java
counter = (int) (xIN[0] + xIN[1] + xIN[2]);

maxLossIGBT = Math.max(xIN[3], maxLossIGBT);
maxLossDiode = Math.max(xIN[4], maxLossDiode);

if(time >= 0.0 && counter < COUNTER_COMPARE) {
    // Only run test during first timesteps
    if(counter != COUNTER_COMPARE) {
        return yOUT;
    }

    if(maxLossIGBT != TEST_IGBT) {
        System.out.println("IGBT loss should be: " + TEST_IGBT + " but it is: " + maxLossIGBT);
        return yOUT;
    }
    if(maxLossDiode != TEST_DIODE) {
        System.out.println("DIODE loss should be: " + TEST_DIODE + " but it is: " + maxLossDiode);
        return yOUT;
    }

    gecko.GeckoSim._testSuccessful = true;
}

// Normal simulation code (after test period)
yOUT[0] = maxLossIGBT;
yOUT[1] = maxLossDiode;
// ... add your normal calculation code
```

### Option 3: Use Simpler Example

Try `demo_JAVA_Block.ipes` instead - it has simpler code without test conditions.

## How to Apply the Fix

### Method A: Edit in GeckoCIRCUITS (If Selection Works)

1. Open `ThreePhase-VSR_10kW_thermal_with_java.ipes` in GeckoCIRCUITS
2. Double-click on the Java block
3. In CodeWindow, click "Source Code" tab
4. Copy/paste the fixed code from above
5. Click "Compile" (if available)
6. Run simulation

### Method B: Edit in External Editor (If Selection Doesn't Work)

1. Copy the current code (shown below)
2. Paste into your preferred text editor
3. Edit the code
4. Copy all the modified code
5. **Cannot paste back to GeckoCIRCUITS - GUI limitation**

   **Solution:** Manually edit the .ipes file
   ```bash
   # Extract the .ipes file
   gunzip -c "ThreePhase-VSR_10kW_thermal_with_java.ipes" > temp.ipes

   # Find and replace the sourceCode section
   # (Use your text editor to find: ^<sourceCode>)
   # Replace with your fixed code

   # Recompress (Windows PowerShell)
   gzip -c temp.ipes > "ThreePhase-VSR_10kW_thermal_with_java_fixed.ipes"
   ```

## Current Java Block Code

```java
counter = (int) (xIN[0] + xIN[1] + xIN[2]);

maxLossIGBT = Math.max(xIN[3], maxLossIGBT);
maxLossDiode = Math.max(xIN[4], maxLossDiode);

if(time == TEST_TIME) {
	if(counter != COUNTER_COMPARE) {
		return yOUT;
	}

	if(maxLossIGBT != TEST_IGBT) {
		System.out.println("IGBT loss should be: " + TEST_IGBT + " but it is: " + maxLossIGBT);
		return yOUT;
	}
	if(maxLossDiode != TEST_DIODE) {
		System.out.println("DIODE loss should be: " + TEST_DIODE + " but it is: " + maxLossDiode);
		return yOUT;
	}

	gecko.GeckoSim._testSuccessful = true;
}

return yOUT;
```

## Static Variables Used

```java
TEST_TIME = 0.019999500000009673
COUNTER_COMPARE = 2262
TEST_IGBT = 38.8905763261036
TEST_DIODE = 6582.611049592271
```

## Notes

- The code uses `gecko.GeckoSim._testSuccessful = true;` which is for automated testing
- This requires accessing the GeckoSim object from user code
- For normal simulation, this is not needed
- Remove the test condition to use the block in normal simulation mode

## Debugging

To add more debug output, add `System.out.println()` statements:

```java
System.out.println("Time: " + time);
System.out.println("Input 0: " + xIN[0]);
System.out.println("Input 1: " + xIN[1]);
System.out.println("Counter: " + counter);
```

View output in Java Block CodeWindow → "Output" tab (make sure "Use Java-block textfield output" is selected).
