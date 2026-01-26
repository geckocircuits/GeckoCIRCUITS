# NetBeans GUI Designer - Usage Guide

## Overview

**NetBeans GUI Designer** (also called WindowBuilder) is a WYSIWYG editor for Swing GUI components, built into NetBeans. It's the Swing equivalent of SceneBuilder for JavaFX.

---

## Installation & Setup

### Step 1: Install NetBeans

**Windows:**
1. Download NetBeans: https://www.netbeans.org/downloads/
2. Run installer
3. Complete installation
4. Note installation path

**macOS:**
1. Download NetBeans: https://www.netbeans.org/downloads/
2. Open .dmg file
3. Drag NetBeans to Applications folder
4. Complete installation

**Ubuntu/Linux:**
```bash
# Option A: From repositories
sudo apt-get install netbeans

# Option B: From download
1. Download .sh installer from: https://www.netbeans.org/downloads/
2. Make executable: chmod +x netbeans-*.sh
3. Run: ./netbeans-*.sh
```

### Step 2: Find NetBeans Installation

**Windows:**
- Check default: `C:\Program Files\Apache NetBeans\`
- Or custom during installation

**macOS:**
- Check: `/Applications/NetBeans/`
- Or custom during installation

**Ubuntu/Linux:**
```bash
# Common locations
/usr/local/netbeans/
/opt/netbeans/
~/netbeans/

# Find with:
which netbeans
whereis netbeans
```

**Universal method (works on all OS):**
- In terminal/command prompt, run: `netbeans`
- Opens NetBeans if in PATH
- Or create desktop shortcut

---

## Opening Project in NetBeans

### Method 1: From NetBeans Menu

1. Launch NetBeans
2. File → Open Project
3. Navigate to project directory
4. Select `pom.xml`
5. Click "Open Project"

**Method 2: Command Line**

```bash
# Navigate to your project directory first
cd /path/to/your/project

# Then open NetBeans
netbeans

# Or specify file:
netbeans pom.xml
```

**Method 3: Drag and Drop**

1. Open file explorer/finder
2. Navigate to project directory
3. Drag project folder to NetProjects panel in NetBeans
4. Release

---

## Working with .form Files

### Opening .form in GUI Designer

**Method 1: Right-click (Windows/Linux)**
1. In NetBeans Projects panel (left), navigate to: `Your Project → Source Packages → [package path]`
2. Find `.form` file (e.g., `DialogSheetSize.form`)
3. Right-click → Open

**Method 2: Double-click**
1. Double-click `.form` file in Projects panel
2. Automatically opens in GUI Designer tab

**Method 3: Shift-double-click (Best Method)**
1. Hold Shift and double-click `.form` file
2. Opens in GUI Designer tab instead of Source code
3. Avoids opening Source tab first

---

## NetBeans GUI Designer Features

### What You Can Do

**Drag and Drop Components (Palette - Right Panel)**
- Swing Containers: JPanel, JFrame, JDialog, etc.
- Swing Controls: JButton, JLabel, JSpinner, etc.
- Swing Menus: JMenu, JMenuItem
- Layout Managers: BorderLayout, GridLayout, FlowLayout, etc.

**Edit Properties (Properties - Right Panel)**
- Name: Component name (e.g., jButtonOk)
- Text: Button/label text
- Font: Size, style, family
- Colors: Background, foreground
- Size: Width, height, preferred size
- Position: X, Y coordinates
- Events: ActionPerformed, mouseClicked, etc.

**Resize and Position (Mouse)**
- Click component to select
- Drag corners/edges to resize
- Drag to move component
- Align multiple components (dashed alignment guides appear)

**Layout Management (Auto and Manual)**
- Right-click → Set Layout → [Choose layout manager]
- Auto-layout: Components snap to grid
- Manual: Free positioning

---

## Switching Between Views

When .form file is open, you'll see tabs at bottom:

```
[Design] [Source] [History]
```

- **[Design]**: GUI designer (drag/drop, properties)
- **[Source]**: Java code editor (business logic)
- **[History]**: Undo/redo changes
- **[Navigator]**: Component hierarchy (optional, View → Navigator)

**Hotkeys:**
- Ctrl+S: Save file
- Ctrl+Shift+Up: Previous method
- Ctrl+Shift+Down: Next method
- Tab: Switch between Design and Source
- Alt+Shift+F: Format code
- Ctrl+Space: Code completion

---

## Daily Workflow

### Editing a Dialog (Example: DialogSheetSize)

**Step 1: Open .form in Designer**
```bash
Shift+double-click DialogSheetSize.form
# Opens in [Design] tab automatically
```

**Step 2: Design GUI**
```bash
1. Drag components from Palette (right panel)
   - JButton, JLabel, JSpinner, etc.

2. Drop on form
   - Position with mouse
   - Resize by dragging corners

3. Set properties (right panel)
   - Click button
   - Find "text" property
   - Type value (e.g., "OK")
   - Press Enter
```

**Step 3: Save**
```bash
Ctrl+S
# Or File → Save
```

**Step 4: Edit Business Logic (Optional)**
```bash
1. Click [Source] tab
2. Edit Java code
3. NetBeans auto-updates form references
4. Press Ctrl+S to save
```

**Step 5: Test**
```bash
Method A: From NetBeans
- Right-click project → Run
- Press F6

Method B: From command line
- cd /path/to/project
- mvn exec:java -Dexec.mainClass="ch.technokrat.gecko.GeckoSim"

Method C: Run JAR
- java -jar target/gecko-1.0-jar-with-dependencies.jar
```

---

## Common Tasks

### Changing Button Text
1. Click button in Designer
2. Find "text" property (right panel)
3. Type new text
4. Press Enter
5. Save (Ctrl+S)

### Adding New Component
1. Drag component from Palette
2. Drop on form
3. Adjust position/size
4. Set properties
5. Save

### Deleting Component
1. Click component in Designer
2. Press Delete key
3. Save

### Renaming Component
1. Click component in Designer
2. Right-click → Rename
3. Type new name
4. Press Enter
5. Save

### Changing Component Size
1. Click component in Designer
2. Find "preferredSize" or "size" property
3. Edit value (e.g., "[100, 30]")
4. Press Enter
5. Save

---

## Advanced Features

### Property Editor (Right Panel)

Full list of properties available:
- **Name**: Component identifier
- **Text**: Label/caption text
- **Font**: Size (e.g., "12"), family, style
- **Foreground/Background**: Colors (RGB values)
- **Opaque**: Component transparency
- **Enabled**: Enable/disable component
- **Visible**: Show/hide component
- **Tool Tip Text**: Hover text
- **Minimum/Maximum/Preferred Sizes**: Dimension constraints
- **Layout**: Alignment, anchors, weights
- **Events**: actionPerformed, mouseClicked, keyPressed, etc.

### Palette Customization

You can:
- Filter components by type (containers, controls, menus)
- Search for specific components
- Pin frequently used components
- Customize palette display (large icons, list view)

### Code Generation

NetBeans automatically generates:
- Component initialization code
- Layout setup code
- Event handler stubs
- Variable declarations

**You only write:**
- Business logic in event handlers
- Component property access
- Data model code

---

## Comparison: Hand-coded vs NetBeans Designer

| Task | Hand-coded Swing | NetBeans GUI Designer |
|------|------------------|----------------------|
| Create button | 5-10 lines of code | Drag + drop |
| Position components | Trial & error | Click + drag |
| Set properties | 1-2 lines per property | Click + select |
| Preview | Must run app | Instant in designer |
| Total time | 50-100 lines | 5 minutes |

---

## Benefits Over Hand-coding

✅ **Visual Design**: See what you get instantly
✅ **Faster**: 10x faster for UI work
✅ **Fewer Errors**: No syntax errors in GUI code
✅ **Auto-generated**: NetBeans writes UI code automatically
✅ **Preview**: Instant visual feedback
✅ **Maintainable**: .form file is version-controllable
✅ **Native Support**: Built into NetBeans
✅ **Zero Migration**: Works with existing .form files
✅ **Zero Risk**: Doesn't break any existing code

---

## Platform-Specific Notes

### Windows
- Default install: `C:\Program Files\Apache NetBeans\`
- Command: `C:\Program Files\Apache NetBeans\bin\netbeans64.exe`
- Desktop shortcut: Usually created during install

### macOS
- Default install: `/Applications/NetBeans/`
- Command: `open -a NetBeans`
- Spotlight: Search for "NetBeans"

### Ubuntu/Linux
**Find NetBeans:**
```bash
# Check if in PATH
which netbeans

# Or check common locations
ls /usr/local/netbeans/
ls /opt/netbeans/
ls ~/netbeans/

# Install if missing
sudo apt-get install netbeans

# Or download and run .sh installer
wget https://downloads.apache.org/netbeans/netbeans/latest/bundles/netbeans-latest.sh
chmod +x netbeans-latest.sh
./netbeans-latest.sh
```

**Run NetBeans on Ubuntu:**
```bash
# From install location
/opt/netbeans/bin/netbeans

# Or if in PATH
netbeans
```

**Note for Ubuntu:**
- Ensure Java is installed (`java -version`)
- NetBeans includes JDK by default
- May need to set JAVA_HOME if using custom JDK

---

## Resources

### Official Documentation
- NetBeans Documentation: https://netbeans.apache.org/kb/docs/
- GUI Designer Guide: https://netbeans.apache.org/kb/docs/java/swing-gui-designer.html
- NetBeans Wiki: https://cwiki.apache.org/confluence/display/NETBEANS/

### Tutorials
- Search YouTube: "NetBeans GUI Designer tutorial"
- Search: "NetBeans WindowBuilder tutorial"
- Search: "NetBeans Swing GUI design"

### Community
- NetBeans Forums: https://community.netbeans.org/
- Stack Overflow: Tag questions with "netbeans" "swing"

---

## Troubleshooting

### .form file doesn't open in Designer
**Solution:**
- Right-click → Open
- Shift+double-click (best method)
- File → Open File → select .form

### Can't find components in Palette
**Solution:**
- Check Palette panel is visible (right side)
- Try Window → Reset Windows
- Check View → Palette (make sure it's checked)

### Project won't build in NetBeans
**Solutions:**
- Right-click project → Clean and Build
- Check Maven settings (Preferences → Settings → Maven)
- Ensure JDK version matches project (Java 21)

### Changes in Source not appearing
**Solutions:**
- Save .form file (Ctrl+S)
- Close and reopen .form file
- Check that Source tab shows generated code

---

## Integration with Other IDEs

### Option A: Use NetBeans for GUI, VSCode for Code

**Workflow:**
1. Open .form in NetBeans for visual design
2. Drag/drop, set properties, save
3. Edit Java code in VSCode (Source tab)
4. Both work with same project files
5. VSCode watches for changes, NetBeans updates on file save

**Benefits:**
- Use VSCode for code editing (familiar)
- Use NetBeans for GUI design (visual)
- Both tools work with same project
- No need to choose one over the other

### Option B: Use NetBeans Full-Time

**When to switch:**
- Need GUI design frequently
- Prefer NetBeans code editor
- Want integrated experience

**Migration:**
1. Install NetBeans plugins you use (Git, SonarLint, etc.)
2. Import VSCode settings (if available)
3. Learn NetBeans shortcuts
4. Import Maven configuration

---

## Summary

**NetBeans GUI Designer provides:**
- WYSIWYG editor for Swing dialogs
- Works with existing .form files directly
- Zero migration effort
- Zero compatibility issues
- Maintains your code and .form files

**Advantages over JavaFX:**
- No toolkit conflicts (Swing only)
- No complex configuration needed
- Works immediately
- No migration time (use .form as-is)

**Recommended workflow:**
1. Open .form in NetBeans (Shift+double-click)
2. Design GUI in Designer tab
3. Set properties in Properties panel
4. Save (Ctrl+S)
5. Test application

---

**Status: Ready to use NetBeans GUI Designer for maintainable Swing GUI!**

