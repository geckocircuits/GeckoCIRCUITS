# Circuit Package GUI-Free Refactoring Plan

**Current Status:** 57/96 GUI-free (59%)  
**Target:** 77/96 GUI-free (80%)  
**Date:** January 27, 2026  
**Executor:** Claude Haiku

---

## Overview

This plan documents incremental refactoring tasks to increase the GUI-free percentage
of the `circuit` package. Tasks are ordered by difficulty (easiest first) and each
task is designed to be executed independently.

**Key Files:**
- Source: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/`
- Tests: `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/`
- Validation: `src/test/java/ch/technokrat/gecko/geckocircuits/core/CorePackageValidationTest.java`

**39 GUI-Dependent Classes (Current):**
```
AbstractBlockInterface, AbstractCircuitSheetComponent, AbstractTerminal, CircuitLabel,
CircuitSheet, ComponentCoupling, ConnectorType, DataTablePanel, DataTablePanelParameters,
DialogCircuitComponent, DialogGlobalTerminal, DialogModule, DialogNonLinearity,
IDStringDialog, InvisibleEdit, KnotenLabel, MyTableCellEditor, MyTableCellRenderer,
NonLinearDialogPanel, PotentialCoupling, SchematicComponentSelection2, SchematicEditor2,
SchematicTextInfo, SubCircuitTerminable, TerminalControl, TerminalControlBidirectional,
TerminalControlInput, TerminalControlOutput, TerminalFixedPositionInvisible,
TerminalHiddenSubcircuit, TerminalInterface, TerminalRelativePositionReluctance,
TerminalSubCircuitBlock, TerminalToWrap, TerminalTwoPortComponent, TerminalVerbindung,
ToolBar, Verbindung, WorksheetSize
```

---

## TIER 1: Easy Wins (Color Only) - Expected: +2 classes

### Task 1.1: ConnectorType.java - Replace Color with RGB int

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ConnectorType.java`  
**Current Import:** `import java.awt.Color;`  
**Difficulty:** ⭐ Easy  
**Estimated Time:** 15 min

**Step-by-Step Instructions:**

1. **Read the file:**
   ```bash
   cat src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ConnectorType.java
   ```

2. **Find Color usages:**
   ```bash
   grep -n "Color" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ConnectorType.java
   ```

3. **Replace Color constants with int RGB values:**
   - `Color.RED` → `0xFF0000`
   - `Color.BLUE` → `0x0000FF`
   - `Color.GREEN` → `0x00FF00`
   - `Color.BLACK` → `0x000000`
   - `Color.WHITE` → `0xFFFFFF`
   - `new Color(r, g, b)` → `((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF)`

4. **Change field types:**
   - `private Color fooColor;` → `private int fooColorRgb;`

5. **Add conversion method (if needed by callers):**
   ```java
   /**
    * Convert RGB int to AWT Color for GUI rendering.
    * Call this only in GUI layer.
    */
   public java.awt.Color toAwtColor(int rgb) {
       return new java.awt.Color(rgb);
   }
   ```

6. **Remove the import:**
   - Delete line: `import java.awt.Color;`

7. **Verify:**
   ```bash
   mvn test-compile -q
   grep "import java.awt" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ConnectorType.java
   ```

**Acceptance Criteria:**
- [ ] No `java.awt` imports in file
- [ ] Compiles without errors
- [ ] Color values preserved as int RGB

---

### Task 1.2: SubCircuitTerminable.java - Replace Color with RGB int

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SubCircuitTerminable.java`  
**Current Import:** `import java.awt.Color;`  
**Difficulty:** ⭐ Easy  
**Estimated Time:** 15 min

**Step-by-Step Instructions:**

1. **Read the file:**
   ```bash
   cat src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SubCircuitTerminable.java
   ```

2. **Find Color usages:**
   ```bash
   grep -n "Color" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SubCircuitTerminable.java
   ```

3. **Apply same pattern as Task 1.1:**
   - Replace Color type with int
   - Replace Color constants with hex values
   - Remove import

4. **Verify:**
   ```bash
   mvn test-compile -q
   ```

---

## TIER 2: Undo Framework Extraction - Expected: +4 classes

### Task 2.1: Create GeckoUndoableEdit Interface (PREREQUISITE)

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoUndoableEdit.java`  
**Difficulty:** ⭐⭐ Medium-Easy  
**Estimated Time:** 20 min

**Create this file with exact content:**

```java
/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * GUI-free interface for undoable edits.
 * 
 * This interface mirrors javax.swing.undo.UndoableEdit but without
 * Swing dependencies, allowing undo/redo functionality in headless
 * environments.
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
public interface GeckoUndoableEdit {
    
    /**
     * Undo this edit.
     * @throws IllegalStateException if cannot undo
     */
    void undo() throws IllegalStateException;
    
    /**
     * Redo this edit (undo the undo).
     * @throws IllegalStateException if cannot redo
     */
    void redo() throws IllegalStateException;
    
    /**
     * @return true if this edit can be undone
     */
    boolean canUndo();
    
    /**
     * @return true if this edit can be redone
     */
    boolean canRedo();
    
    /**
     * @return a localized, human-readable description of this edit
     */
    String getPresentationName();
    
    /**
     * @return description for undo menu item (e.g., "Undo Move")
     */
    default String getUndoPresentationName() {
        return "Undo " + getPresentationName();
    }
    
    /**
     * @return description for redo menu item (e.g., "Redo Move")
     */
    default String getRedoPresentationName() {
        return "Redo " + getPresentationName();
    }
    
    /**
     * @return true if this edit is significant (should be undoable by user)
     */
    default boolean isSignificant() {
        return true;
    }
    
    /**
     * Try to absorb another edit into this one.
     * @param edit the edit to absorb
     * @return true if successfully absorbed
     */
    default boolean addEdit(GeckoUndoableEdit edit) {
        return false;
    }
    
    /**
     * Try to replace this edit with another.
     * @param edit the edit to replace with
     * @return true if successfully replaced
     */
    default boolean replaceEdit(GeckoUndoableEdit edit) {
        return false;
    }
    
    /**
     * Notify this edit that it is no longer needed.
     */
    default void die() {
        // Default: no cleanup needed
    }
}
```

**Verify:**
```bash
mvn test-compile -q
```

---

### Task 2.2: Create GeckoUndoableEditAdapter (Bridge to Swing)

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoUndoableEditAdapter.java`  
**Difficulty:** ⭐⭐ Medium-Easy  
**Estimated Time:** 15 min

**Create this file:**

```java
/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Adapter that wraps a GeckoUndoableEdit as a Swing UndoableEdit.
 * 
 * Use this in GUI code to integrate GUI-free edits with Swing's UndoManager.
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15
 */
public class GeckoUndoableEditAdapter implements UndoableEdit {
    
    private final GeckoUndoableEdit delegate;
    
    public GeckoUndoableEditAdapter(GeckoUndoableEdit delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void undo() throws CannotUndoException {
        try {
            delegate.undo();
        } catch (IllegalStateException e) {
            throw new CannotUndoException();
        }
    }
    
    @Override
    public void redo() throws CannotRedoException {
        try {
            delegate.redo();
        } catch (IllegalStateException e) {
            throw new CannotRedoException();
        }
    }
    
    @Override
    public boolean canUndo() {
        return delegate.canUndo();
    }
    
    @Override
    public boolean canRedo() {
        return delegate.canRedo();
    }
    
    @Override
    public void die() {
        delegate.die();
    }
    
    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        if (anEdit instanceof GeckoUndoableEditAdapter) {
            return delegate.addEdit(((GeckoUndoableEditAdapter) anEdit).delegate);
        }
        return false;
    }
    
    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {
        if (anEdit instanceof GeckoUndoableEditAdapter) {
            return delegate.replaceEdit(((GeckoUndoableEditAdapter) anEdit).delegate);
        }
        return false;
    }
    
    @Override
    public boolean isSignificant() {
        return delegate.isSignificant();
    }
    
    @Override
    public String getPresentationName() {
        return delegate.getPresentationName();
    }
    
    @Override
    public String getUndoPresentationName() {
        return delegate.getUndoPresentationName();
    }
    
    @Override
    public String getRedoPresentationName() {
        return delegate.getRedoPresentationName();
    }
    
    /**
     * Get the wrapped GUI-free edit.
     */
    public GeckoUndoableEdit getDelegate() {
        return delegate;
    }
}
```

---

### Task 2.3: Refactor InvisibleEdit.java

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/InvisibleEdit.java`  
**Current Import:** `import javax.swing.undo.UndoableEdit;`  
**Difficulty:** ⭐⭐ Medium-Easy  
**Estimated Time:** 20 min  
**Depends On:** Task 2.1

**Step-by-Step Instructions:**

1. **Read the file:**
   ```bash
   cat src/main/java/ch/technokrat/gecko/geckocircuits/circuit/InvisibleEdit.java
   ```

2. **Change interface implementation:**
   - Replace `implements UndoableEdit` with `implements GeckoUndoableEdit`

3. **Update exception handling:**
   - `throws CannotUndoException` → `throws IllegalStateException`
   - `throws CannotRedoException` → `throws IllegalStateException`

4. **Remove imports:**
   - Delete: `import javax.swing.undo.UndoableEdit;`
   - Delete: `import javax.swing.undo.CannotUndoException;` (if present)
   - Delete: `import javax.swing.undo.CannotRedoException;` (if present)

5. **Verify:**
   ```bash
   mvn test-compile -q
   grep "import javax.swing" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/InvisibleEdit.java
   ```

---

### Task 2.4: Refactor CircuitLabel.java

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitLabel.java`  
**Current Imports:**
```java
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
```
**Difficulty:** ⭐⭐ Medium-Easy  
**Estimated Time:** 25 min  
**Depends On:** Task 2.1

**Step-by-Step Instructions:**

1. **Read the file** to understand structure
2. **Replace UndoableEdit references:**
   - If class implements `UndoableEdit`: change to `GeckoUndoableEdit`
   - If class has methods returning `UndoableEdit`: change return type to `GeckoUndoableEdit`
   - If class has fields of type `UndoableEdit`: change type
3. **Replace exception types:**
   - `CannotUndoException` → `IllegalStateException`
   - `CannotRedoException` → `IllegalStateException`
4. **Remove all javax.swing.undo imports**
5. **Verify compilation**

---

### Task 2.5: Refactor ComponentCoupling.java

**Same pattern as Task 2.4**

---

### Task 2.6: Refactor PotentialCoupling.java

**Same pattern as Task 2.4**

---

## TIER 3: Update Validation Test

### Task 3.1: Update CorePackageValidationTest.java

**File:** `src/test/java/ch/technokrat/gecko/geckocircuits/core/CorePackageValidationTest.java`

**After completing Tier 1 and 2, update CIRCUIT_GUI_CLASSES set:**

Remove these entries (they are now GUI-free):
- `"ConnectorType.java"` (if Task 1.1 done)
- `"SubCircuitTerminable.java"` (if Task 1.2 done - check if it was in list)
- `"InvisibleEdit.java"` (if Task 2.3 done)
- `"CircuitLabel.java"` (if Task 2.4 done)
- `"ComponentCoupling.java"` (if Task 2.5 done)
- `"PotentialCoupling.java"` (if Task 2.6 done)

**Update the test display name:**
- `"circuit main package GUI-free classes remain GUI-free (57/96)"` 
- → `"circuit main package GUI-free classes remain GUI-free (63/96)"` (or actual count)

---

## Classes NOT to Refactor (Drawing/Heavy GUI)

These classes have deep GUI dependencies and are OUT OF SCOPE:

| Class | Reason |
|-------|--------|
| AbstractBlockInterface.java | Graphics2D drawing + Window |
| AbstractCircuitSheetComponent.java | Graphics2D + ActionListener |
| AbstractTerminal.java | Graphics rendering |
| CircuitSheet.java | JPanel + Graphics2D |
| All Terminal*.java (12 classes) | Terminal rendering with Graphics |
| All Dialog*.java (4 classes) | Dialog windows |
| SchematicEditor2.java | Full editor GUI |
| SchematicComponentSelection2.java | Full selection GUI |
| Verbindung.java | Wire rendering |
| ToolBar.java | Swing toolbar |
| DataTablePanel*.java | Swing tables |
| NonLinearDialogPanel.java | Swing panel |
| KnotenLabel.java | Graphics rendering |
| MyTableCell*.java | Swing table cells |
| SchematicTextInfo.java | Graphics + fonts |

---

## TIER 4: Create GeckoGraphics Abstraction - Expected: +10 classes

This tier introduces a GUI-free graphics abstraction layer to decouple drawing logic.

### Task 4.1: Create GeckoGraphics Interface

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoGraphics.java`  
**Difficulty:** ⭐⭐⭐ Medium  
**Estimated Time:** 30 min

**Purpose:** Abstract drawing operations so classes can define what to draw without depending on java.awt.Graphics.

**Create this file:**

```java
/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * GUI-free abstraction for graphics operations.
 * 
 * Classes can define drawing behavior using this interface without
 * depending on java.awt.Graphics or javax.swing.
 * 
 * @author GeckoCIRCUITS Team
 * @since Sprint 15 - GUI-free refactoring
 */
public interface GeckoGraphics {
    
    // Color operations (RGB as int)
    void setColor(int rgb);
    int getColor();
    
    // Line drawing
    void drawLine(int x1, int y1, int x2, int y2);
    void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);
    
    // Shape drawing
    void drawRect(int x, int y, int width, int height);
    void fillRect(int x, int y, int width, int height);
    void drawOval(int x, int y, int width, int height);
    void fillOval(int x, int y, int width, int height);
    void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);
    void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);
    void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);
    void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);
    
    // Text
    void drawString(String str, int x, int y);
    
    // Transform
    void translate(int x, int y);
    void rotate(double theta);
    void scale(double sx, double sy);
    
    // State
    void save();
    void restore();
    
    // Stroke
    void setStrokeWidth(float width);
}
```

---

### Task 4.2: Create AwtGraphicsAdapter (Bridge to AWT)

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AwtGraphicsAdapter.java`  
**Difficulty:** ⭐⭐⭐ Medium  
**Estimated Time:** 30 min

**Create this file:**

```java
/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 */
package ch.technokrat.gecko.geckocircuits.circuit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Adapter that wraps java.awt.Graphics as GeckoGraphics.
 * 
 * Use this in GUI code to provide the GeckoGraphics interface to
 * GUI-free drawing classes.
 */
public class AwtGraphicsAdapter implements GeckoGraphics {
    
    private final Graphics2D g;
    private final Deque<AffineTransform> transformStack = new ArrayDeque<>();
    private int currentColorRgb = 0x000000;
    
    public AwtGraphicsAdapter(Graphics graphics) {
        this.g = (Graphics2D) graphics;
    }
    
    @Override
    public void setColor(int rgb) {
        currentColorRgb = rgb;
        g.setColor(new Color(rgb));
    }
    
    @Override
    public int getColor() {
        return currentColorRgb;
    }
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }
    
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        g.drawPolyline(xPoints, yPoints, nPoints);
    }
    
    @Override
    public void drawRect(int x, int y, int width, int height) {
        g.drawRect(x, y, width, height);
    }
    
    @Override
    public void fillRect(int x, int y, int width, int height) {
        g.fillRect(x, y, width, height);
    }
    
    @Override
    public void drawOval(int x, int y, int width, int height) {
        g.drawOval(x, y, width, height);
    }
    
    @Override
    public void fillOval(int x, int y, int width, int height) {
        g.fillOval(x, y, width, height);
    }
    
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g.drawPolygon(xPoints, yPoints, nPoints);
    }
    
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g.fillPolygon(xPoints, yPoints, nPoints);
    }
    
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.drawArc(x, y, width, height, startAngle, arcAngle);
    }
    
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.fillArc(x, y, width, height, startAngle, arcAngle);
    }
    
    @Override
    public void drawString(String str, int x, int y) {
        g.drawString(str, x, y);
    }
    
    @Override
    public void translate(int x, int y) {
        g.translate(x, y);
    }
    
    @Override
    public void rotate(double theta) {
        g.rotate(theta);
    }
    
    @Override
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }
    
    @Override
    public void save() {
        transformStack.push(g.getTransform());
    }
    
    @Override
    public void restore() {
        if (!transformStack.isEmpty()) {
            g.setTransform(transformStack.pop());
        }
    }
    
    @Override
    public void setStrokeWidth(float width) {
        g.setStroke(new BasicStroke(width));
    }
    
    /**
     * Get the underlying Graphics2D for advanced operations.
     */
    public Graphics2D getGraphics2D() {
        return g;
    }
}
```

---

### Task 4.3: Create Drawable Interface

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Drawable.java`  
**Difficulty:** ⭐ Easy  
**Estimated Time:** 10 min

**Create this file:**

```java
/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 */
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * Interface for objects that can draw themselves to a GeckoGraphics context.
 * 
 * This is the GUI-free version of paintComponent(Graphics).
 */
public interface Drawable {
    
    /**
     * Draw this object using the provided graphics context.
     * @param g the GUI-free graphics context
     */
    void draw(GeckoGraphics g);
}
```

---

### Task 4.4-4.7: Refactor Terminal* Classes (Graphics only)

These Terminal classes ONLY use Graphics for the paintComponent method. They can
be refactored to use the Drawable interface while keeping a thin bridge method.

**Classes to refactor:**
- TerminalHiddenSubcircuit.java (1 import: Graphics)
- TerminalInterface.java (1 import: Graphics)
- TerminalTwoPortComponent.java (1 import: Graphics)
- TerminalVerbindung.java (1 import: Graphics)

**Pattern for each:**

1. **Add implements Drawable** to class declaration
2. **Add draw(GeckoGraphics) method** with GUI-free drawing logic
3. **Update paintComponent(Graphics)** to delegate:
   ```java
   @Override
   public void paintComponent(Graphics graphics) {
       draw(new AwtGraphicsAdapter(graphics));
   }
   ```
4. **Remove the Graphics import** - only AwtGraphicsAdapter imports Graphics

**Example for TerminalHiddenSubcircuit.java:**

```java
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.control.Point;

public class TerminalHiddenSubcircuit extends TerminalRelativePosition implements Drawable {

    public TerminalHiddenSubcircuit(AbstractBlockInterface relatedComponent) {
        super(relatedComponent, 0, 0);
    }

    @Override
    public void draw(GeckoGraphics g) {
        // do not draw this terminal!
    }

    // Keep for backwards compatibility - delegates to draw()
    public void paintComponentLegacy(Object graphics) {
        if (graphics instanceof java.awt.Graphics) {
            draw(new AwtGraphicsAdapter((java.awt.Graphics) graphics));
        }
    }
}
```

**Note:** TerminalInterface.java is an interface - change:
```java
void paintComponent(final Graphics graphics);
```
to:
```java
void draw(GeckoGraphics g);
// Optional: Keep legacy method with Object parameter
```

---

## TIER 5: Extract Dialog Logic - Expected: +3 classes

These classes use JOptionPane for simple message/input. Extract the logic.

### Task 5.1: Refactor DialogGlobalTerminal.java

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogGlobalTerminal.java`  
**Current Import:** `import javax.swing.JOptionPane;`  
**Difficulty:** ⭐⭐⭐ Medium  
**Estimated Time:** 40 min

**Strategy:** Extract business logic into a separate GUI-free class.

1. **Create GlobalTerminalLogic.java** (GUI-free):
   ```java
   package ch.technokrat.gecko.geckocircuits.circuit;
   
   import java.util.HashSet;
   import java.util.Set;
   
   public class GlobalTerminalLogic {
       private final GlobalTerminable _globalTerminable;
       private final Set<GlobalTerminable> _allGlobalTerminals;
       
       public GlobalTerminalLogic(GlobalTerminable globalTerminable) {
           _globalTerminable = globalTerminable;
           _allGlobalTerminals = new HashSet<>(_globalTerminable.getAllGlobalTerminals());
       }
       
       public Set<String> getAllGlobalNames() {
           Set<String> names = new HashSet<>();
           for (GlobalTerminable gt : _allGlobalTerminals) {
               names.add(gt.getStringID());
           }
           return names;
       }
       
       public boolean isValidName(String name) {
           // validation logic
           return name != null && !name.trim().isEmpty();
       }
       
       // More business logic methods...
   }
   ```

2. **Keep DialogGlobalTerminal.java as thin GUI wrapper** that uses GlobalTerminalLogic

---

### Task 5.2: Refactor WorksheetSize.java

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/WorksheetSize.java`  
**Current Imports:**
```java
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
```
**Difficulty:** ⭐⭐⭐ Medium  
**Estimated Time:** 45 min

**Strategy:** 

1. **Replace Dimension with width/height ints or custom Size class**
2. **Extract validation to separate class** (WorksheetSizeValidator)
3. **Replace ActionListener with a GUI-free callback interface**
4. **Remove JOptionPane - use exception or return value for errors**

**Create WorksheetSizeValidator.java:**
```java
package ch.technokrat.gecko.geckocircuits.circuit;

public class WorksheetSizeValidator {
    
    public static class ValidationResult {
        public final boolean valid;
        public final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    public ValidationResult validateResize(int newWidth, int newHeight, 
                                           boolean hasComponentsOutside) {
        if (hasComponentsOutside) {
            return ValidationResult.error(
                "There are circuit components located outside the selected sheet size " +
                newWidth + "x" + newHeight + ".\n" +
                "Please move all components within the target sheet size.");
        }
        return ValidationResult.ok();
    }
}
```

---

### Task 5.3: Refactor IDStringDialog.java

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/IDStringDialog.java`  
**Current Imports:**
```java
import java.awt.event.ActionListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
```
**Difficulty:** ⭐⭐ Medium-Easy  
**Estimated Time:** 30 min

**Steps:**
1. Replace ActionListener with standard Java Consumer/Runnable or custom callback
2. Replace UndoableEdit with GeckoUndoableEdit (from Task 2.1)
3. Replace CannotRedoException/CannotUndoException with IllegalStateException

---

## TIER 6: Create GeckoActionListener - Expected: +2 classes

### Task 6.1: Create GeckoActionCallback Interface

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoActionCallback.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * GUI-free replacement for java.awt.event.ActionListener.
 */
@FunctionalInterface
public interface GeckoActionCallback {
    void onAction(String actionCommand);
}
```

### Task 6.2: Refactor AbstractCircuitSheetComponent.java (Partial)

This class uses ActionListener. Can partially decouple by using GeckoActionCallback
for internal logic while keeping ActionListener at the boundary.

---

## TIER 7: Create GeckoDimension - Expected: +1 class

### Task 7.1: Create GeckoDimension.java

**New File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoDimension.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * GUI-free replacement for java.awt.Dimension.
 */
public final class GeckoDimension {
    public final int width;
    public final int height;
    
    public GeckoDimension(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public java.awt.Dimension toAwtDimension() {
        return new java.awt.Dimension(width, height);
    }
    
    public static GeckoDimension fromAwtDimension(java.awt.Dimension dim) {
        return new GeckoDimension(dim.width, dim.height);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeckoDimension)) return false;
        GeckoDimension that = (GeckoDimension) o;
        return width == that.width && height == that.height;
    }
    
    @Override
    public int hashCode() {
        return 31 * width + height;
    }
    
    @Override
    public String toString() {
        return "GeckoDimension[" + width + "x" + height + "]";
    }
}
```

---

## Verification Commands

```bash
# Count GUI-free classes
cd /home/tinix/claude_wsl/GeckoCIRCUITS
GUI=$(grep -l "import java\.awt\|import javax\.swing" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/*.java | wc -l)
TOTAL=$(ls src/main/java/ch/technokrat/gecko/geckocircuits/circuit/*.java | wc -l)
echo "GUI-free: $((TOTAL - GUI))/$TOTAL ($(( (TOTAL - GUI) * 100 / TOTAL ))%)"

# Compile check
mvn test-compile -q

# Run validation test
mvn test -Dtest=CorePackageValidationTest -q

# Run all tests
mvn test -q
```

---

## Execution Checklist

### Phase 1: Foundation (Tier 1-3) - Target: 63/96 (66%)
```
□ Task 1.1: ConnectorType.java (Color → int RGB)
□ Task 1.2: SubCircuitTerminable.java (Color → int RGB)
□ Compile check

□ Task 2.1: Create GeckoUndoableEdit.java interface
□ Task 2.2: Create GeckoUndoableEditAdapter.java bridge
□ Compile check

□ Task 2.3: InvisibleEdit.java (UndoableEdit → GeckoUndoableEdit)
□ Task 2.4: CircuitLabel.java (UndoableEdit → GeckoUndoableEdit)
□ Task 2.5: ComponentCoupling.java (UndoableEdit → GeckoUndoableEdit)
□ Task 2.6: PotentialCoupling.java (UndoableEdit → GeckoUndoableEdit)
□ Compile check

□ Task 3.1: Update CorePackageValidationTest.java
□ Verification: mvn test -q
```

### Phase 2: Graphics Abstraction (Tier 4) - Target: 71/96 (74%)
```
□ Task 4.1: Create GeckoGraphics.java interface
□ Task 4.2: Create AwtGraphicsAdapter.java bridge
□ Task 4.3: Create Drawable.java interface
□ Compile check

□ Task 4.4: TerminalHiddenSubcircuit.java (Graphics → GeckoGraphics)
□ Task 4.5: TerminalInterface.java (Graphics → GeckoGraphics)
□ Task 4.6: TerminalTwoPortComponent.java (Graphics → GeckoGraphics)
□ Task 4.7: TerminalVerbindung.java (Graphics → GeckoGraphics)
□ Compile check

□ Task 4.8: TerminalFixedPositionInvisible.java (if simple)
□ Task 4.9: Additional Terminal classes (if straightforward)
□ Verification: mvn test -q
```

### Phase 3: Dialog & Listener Extraction (Tier 5-7) - Target: 77/96 (80%)
```
□ Task 5.1: Create GlobalTerminalLogic.java + refactor DialogGlobalTerminal
□ Task 5.2: Create WorksheetSizeValidator.java + refactor WorksheetSize
□ Task 5.3: IDStringDialog.java (ActionListener + Undo)
□ Compile check

□ Task 6.1: Create GeckoActionCallback.java
□ Task 6.2: Partial refactor of classes using ActionListener
□ Compile check

□ Task 7.1: Create GeckoDimension.java
□ Task 7.2: Replace Dimension usages where simple
□ Compile check

□ Final validation: mvn test -q
□ Report final count: 77/96 (80%)
```

---

## Expected Final Result

| Metric | Before | Phase 1 | Phase 2 | Phase 3 |
|--------|--------|---------|---------|---------|
| GUI-free classes | 57 | 63 | 71 | 77 |
| Percentage | 59% | 66% | 74% | 80% |
| New abstractions | 0 | 2 | 5 | 8 |
| Tests | 2426 | 2426+ | 2426+ | 2426+ |

---

## Risk Assessment

| Task | Risk Level | Mitigation |
|------|------------|------------|
| Color → RGB | Low | Well-defined pattern |
| UndoableEdit | Low | Interface compatible |
| Graphics abstraction | Medium | May need additional methods |
| Terminal refactoring | Medium | Inheritance chains |
| Dialog extraction | High | Complex state management |
| ActionListener | Medium | Callback semantics must match |

---

## Rollback Strategy

If any tier breaks the build:
1. `git checkout -- src/` to revert all changes
2. Re-run tests: `mvn test -q`
3. Apply only completed tasks from checklist
