---
title: Troubleshooting
---

# Troubleshooting

Common issues and solutions for GeckoCIRCUITS.

## Startup Issues

### Application won't launch

**Symptom:** Nothing happens when running the JAR or launcher script.

**Check Java version:**
```bash
java -version
# Must show 21 or higher
```

If Java is not installed or too old, install [Temurin JDK 21](https://adoptium.net/).

**Check the JAR exists:**
```bash
ls -la target/gecko-1.0-jar-with-dependencies.jar
```

If missing, build it:
```bash
mvn clean package assembly:single -DskipTests
```

### "Unsupported class file major version 65" error

Your Java version is too old. GeckoCIRCUITS requires **Java 21+**.

```bash
# Check version
java -version

# Install Java 21 (Ubuntu/Debian)
sudo apt install openjdk-21-jdk
```

### OutOfMemoryError

Increase the heap size:
```bash
java -Xmx4G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar
```

Default is `-Xmx3G`. Increase to 4G or more for large circuits.

### Blank or black window

**HiDPI scaling issue.** Try:
```bash
java -Dsun.java2d.uiScale=1 -jar target/gecko-1.0-jar-with-dependencies.jar
```

Or use the HiDPI launcher:
```bash
./scripts/run-gecko-linux.sh --hidpi
```

## WSL-Specific Issues

### No display / HeadlessException

GeckoCIRCUITS needs a display server. On WSL2:

**Option 1:** WSLg (Windows 11, automatic):
```bash
# Should work automatically. Verify:
echo $DISPLAY
# Should show something like :0
```

**Option 2:** Install an X server (Windows 10):
```bash
# Install VcXsrv or Xming on Windows
# Then in WSL:
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0
```

**Option 3:** Headless mode with Xvfb:
```bash
sudo apt install xvfb
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99
java -jar target/gecko-1.0-jar-with-dependencies.jar
```

### Slow rendering on WSL

WSL2 GUI forwarding can be slow. Try:

1. Use `--hidpi` flag (reduces pixel count)
2. Minimize scope window refreshes during simulation
3. Close unused scope windows

## Simulation Issues

### Simulation runs but produces incorrect results

| Check | Solution |
|-------|----------|
| Time step too large | Reduce to < 1/(100 x f_switch) |
| Wrong solver | Try Backward Euler for stiff circuits |
| Missing ground | Every circuit needs a ground reference |
| Open nodes | Ensure all nodes are connected |

### Simulation diverges / NaN values

Usually caused by:

1. **Algebraic loops** - Break them with small parasitic elements:
   - Add 1 mOhm series resistance to ideal voltage sources
   - Add 1 nH series inductance to switches

2. **Division by zero** - Check for zero-valued components

3. **Time step too large** for the switching frequency

### Simulation is very slow

- **Reduce simulation duration** - Start with a few switching periods
- **Increase time step** - But not above 1/(100 x f_switch)
- **Simplify the circuit** - Remove unused components
- **Close scope windows** - Real-time plotting slows simulation

## Build Issues

### Maven build fails

**Clear cache and retry:**
```bash
mvn clean
mvn package assembly:single -DskipTests
```

**If dependencies fail to download:**
```bash
mvn dependency:resolve
```

### Tests fail with HeadlessException

Some tests need a display. On Linux CI:
```bash
sudo apt install xvfb
export DISPLAY=:99
Xvfb :99 -screen 0 1024x768x24 &
mvn test
```

### GraalVM/Polyglot errors

Ensure you pass the compatibility flag:
```bash
java -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar
```

## File Issues

### Can't open .ipes file

- Verify the file isn't corrupted (it should be gzip-compressed)
- Try opening from the command line to see error messages:
  ```bash
  java -jar target/gecko-1.0-jar-with-dependencies.jar path/to/file.ipes
  ```

### Circuit looks different after reopening

Component positions are saved in the .ipes file. If positions look wrong:

- Check you're using the same version of GeckoCIRCUITS
- Try **View > Fit to Window** to reset the view

## Still Stuck?

1. Check the [FAQ](faq.md)
2. Search [existing issues](https://github.com/tinix84/GeckoCIRCUITS/issues)
3. Open a [new issue](https://github.com/tinix84/GeckoCIRCUITS/issues/new/choose) with:
   - Your OS and Java version
   - Steps to reproduce
   - Console output / error messages
