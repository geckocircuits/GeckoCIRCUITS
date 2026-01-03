# Java Block Functionality

## Overview
The Java Block feature in GeckoCIRCUITS allows users to write and execute custom Java code within circuit simulations. This enables complex control algorithms, mathematical operations, and custom logic to be implemented directly in the simulation.

## Architecture

### Core Package Structure
All Java block functionality is located in `src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/`

### Main Components

#### 1. Control Block Classes

**ReglerJavaFunction** (`ReglerJavaFunction.java:51`)
- Main controller class that represents the Java block in the circuit
- Manages terminal configuration (input/output terminals)
- Handles GUI interactions (double-click to open code editor)
- Contains `JavaBlockCalculator` inner class for simulation execution
- Implements `VariableTerminalNumber` and `SpecialNameVisible` interfaces

**AbstractJavaBlock** (`AbstractJavaBlock.java:26`)
- Base class for all Java block implementations
- Manages compilation workflow
- Handles source code storage and retrieval
- Provides methods for code compilation and execution

**JavaBlockVector** (`JavaBlockVector.java:20`)
- Implements vector-based operations
- Default Java block type

**JavaBlockMatrix** (`JavaBlockMatrix.java:21`)
- Implements matrix-based operations
- Used when `_variableBusWidth._useMatrix` is enabled

#### 2. Source Code Management

**JavaBlockSource** (`JavaBlockSource.java`)
- Stores all source code components:
  - `_importsCode` - Java import statements
  - `_variablesCode` - Variable declarations
  - `_initCode` - Initialization code (runs at simulation start)
  - `_sourceCode` - Main calculation code (runs each timestep)
- Uses Builder pattern for construction

**SourceFileGenerator** (`SourceFileGenerator.java:29`)
- Generates complete Java source code from individual components
- Creates properly formatted Java classes
- Handles both vector and matrix modes

#### 3. Compilation System

**CompileObject** / `AbstractCompileObject`
- Manages compilation process
- Stores compilation status and messages
- Contains compiled bytecode

**JavaBlockClassLoader** (`JavaBlockClassLoader.java`)
- Custom class loader for dynamically compiled classes
- Enables runtime compilation and execution

**RamJavaFileObject** (`RamJavaFileObject.java`)
- Represents Java source files in memory
- Used by Java Compiler API

**GeckoForwardingFileManager** (`GeckoForwardingFileManager.java`)
- Custom file manager for Java Compiler API
- Handles in-memory file operations during compilation

**CompileStatus** (`CompileStatus.java`)
- Enum defining compilation states:
  - `NOT_COMPILED`
  - `COMPILED_SUCCESSFULL`
  - `COMPILE_ERROR`

**CompiledClassContainer** (`CompiledClassContainer.java`)
- Container for compiled Java classes
- Manages class bytecode and metadata

#### 4. GUI Components

**CodeWindow** (`CodeWindow.java:59`)
- Main editor window for Java code
- Provides tabs for:
  - Source code
  - Imports
  - Variables
  - Init code
  - Compiled source view
  - Compiler messages
  - Output/Console
- Uses SyntaxPane library for syntax highlighting

**ExtraFilesWindow** (`ExtraFilesWindow.java`)
- Manages additional Java source files
- Allows inclusion of multiple files in compilation

**ReglerJavaTriangles** (`ReglerJavaTriangles.java`)
- UI triangles for terminal adjustment
- Handles click events to add/remove terminals

#### 5. Support Classes

**VariableBusWidth** (`VariableBusWidth.java`)
- Manages vector/matrix mode switching
- Configures bus width parameters

**CompileObjectNull** (`CompileObjectNull.java`)
- Null object pattern implementation
- Used when compilation tools are unavailable

**CompileObjectSavedFile** (`CompileObjectSavedFile.java`)
- Handles loading previously compiled objects from files

## How It Works

### Initialization Flow
1. User adds a Java block to the circuit
2. `ReglerJavaFunction` creates an `AbstractJavaBlock` instance (default: `JavaBlockVector`)
3. Source code is loaded (either from circuit file or defaults)

### Code Editing Flow
1. User double-clicks the Java block
2. `ReglerJavaFunction.openDialogWindow()` is called
3. `CodeWindow` is created and displayed
4. User edits code in various tabs

### Compilation Flow
1. User clicks "Compile" button in CodeWindow
2. `AbstractJavaBlock.compileNewBlockSource()` is called
3. `SourceFileGenerator.createSourceCode()` generates complete Java class
4. Java Compiler API compiles the source using custom `JavaBlockClassLoader`
5. Compilation status is updated

### Simulation Execution Flow
1. Simulation starts
2. `JavaBlockCalculator.initializeAtSimulationStart()` calls init code
3. Each timestep: `JavaBlockCalculator.berechneYOUT()` executes main code
4. Input signals are passed as parameters
5. Output signals are calculated and returned

## Terminal Configuration
- Input terminals: Configured via red triangles on left side
- Output terminals: Configured via red triangles on right side
- Terminal numbering: Automatically managed
- Support for both single-value and bus signals

## Code Structure

### Template Structure
```java
// Imports section
<importsCode>

// Class declaration
public class GeneratedClassName {
    
    // Variables section
    <variablesCode>
    
    // Initialization method (runs once at start)
    public void init(double[] input, double[] output) {
        <initCode>
    }
    
    // Main calculation method (runs each timestep)
    public void calculate(double time, double deltaTime, 
                         double[] input, double[] output) {
        <sourceCode>
    }
}
```

### Available Variables in Calculation Method
- `time` - Current simulation time
- `deltaTime` - Time step size
- `input[]` - Array of input signal values
- `output[]` - Array to store output values

## External File Support
- Multiple Java source files can be added
- Files are stored in `GeckoFile` system
- Hash keys used for file identification
- Files are compiled together with main source

## Integration with Control System
- Extends `RegelBlock` base class
- Implements `Operationable` interface
- Supports terminal number adjustment
- Compatible with GeckoCIRCUITS file format

## Error Handling
- Compilation errors are displayed in Compiler Messages tab
- Runtime exceptions are caught and logged
- Console output can be redirected to Output tab

## Performance Considerations
- Dynamic compilation happens at simulation start
- Compiled code executes efficiently
- Support for both vector and matrix operations
- Console output can be disabled for performance

## Known Issues
- Double-click to open code window may not work (current problem)
- Simulations with Java blocks may fail (current problem)

## Dependencies
- **Java Compiler API** (javax.tools) - Required for dynamic compilation
- **SyntaxPane** 1.3.0 - Code editor with syntax highlighting
- Must have JDK (not just JRE) for compiler tools

## Testing
- Example circuits available in `resources/JAVA_Block/`
- Test file: `src/test/java/ch/technokrat/systemtests/ModelResultsTest.java:77`
- Tests currently disabled due to Java script engine issues

## Example Use Cases
1. **Buck-Boost Converter** (`BuckBoost_thermal_with_java.ipes`) - Thermal modeling
2. **Three-Phase VSR** (`ThreePhase-VSR_10kW_thermal_with_java.ipes`) - PMSM control
3. **Sparse Matrix Converter** (`sparseMatrix_java_with_PMSM_control.ipes`) - Advanced control
4. **Indirect Matrix Converter** (`indirect_matrix_java_with_PMSM_control.ipes`) - Matrix operations
