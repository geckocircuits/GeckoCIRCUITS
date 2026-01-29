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
package ch.technokrat.gecko;

import ch.technokrat.gecko.geckocircuits.allg.*;
import ch.technokrat.gecko.i18n.LangInit;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Main GeckoCIRCUITS simulation class.
 *
 * This is the entry point for the GeckoCIRCUITS circuit simulator application.
 * The application runs in standalone mode via main().
 *
 * Supports multiple operating modes:
 * - STANDALONE (default) - Normal desktop application
 * - REMOTE - Remote access via RMI (MATLAB integration)
 * - MMF - Memory-mapped file communication
 * - SIMULINK - MATLAB Simulink integration
 * - EXTERNAL - External tool integration
 * - HEADLESS - No GUI, for REST API, CLI, and batch processing
 *
 * To run in HEADLESS mode, use one of:
 * - System property: java -Doperatingmode=HEADLESS -jar gecko.jar
 * - Command line flag: java -jar gecko.jar -headless
 */
public class GeckoSim {

    public static long startTime;
    public static MainWindow _win;
    static GeckoSim _geckoSim;
    public static boolean _initialShow = true;
    public static double xx = 4.67;
    // property stuff
    public static Properties defaultProps;
    public static Properties applicationProps;
    public static final String DEFAULT_PROPERTY_FILE = "/defaultProperties.prp";
    public static String APPLICATION_PROPERTY_FILE;
    public static boolean mainLoaded = false;
    public static boolean remoteLoaded = false;
    public static boolean remoteLoading = true;
    public static boolean mmfLoaded = false;
    public static boolean mmfLoading = true;
    public static boolean compiler_toolsjar_missing = true;
    /**
     * this flag is used for automated testing (JUnit), please don't remove it!
     * Andy.
     */
    public static boolean _testSuccessful = false;
    public static boolean _isTestingMode = false;
    public static OperatingMode operatingmode = OperatingMode.STANDALONE;  // default

    static {
        System.setProperty("polyglot.js.nashorn-compat", "true");
        System.setProperty("polyglot", "true");
        System.setProperty("org.graalvm.polyglot.js.nashorn-compat", "true");
        ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
        
        // Initialize SyntaxPane's script engine if GraalVM JavaScript is available
        if (engine != null) {
            System.setProperty("script.engine", "org.graalvm.polyglot.js.script.JSBindings");
        }
    }

    public static void stopTime() {
        long stopTime = System.currentTimeMillis();
        System.out.println("total execution:  " + (stopTime - startTime) / 1000.0);
        System.exit(3);
    }

    public static void main(final String[] args) {
        // Check for operating mode from system property
        String modeProperty = System.getProperty("operatingmode");
        if (modeProperty != null) {
            try {
                operatingmode = OperatingMode.valueOf(modeProperty.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid operating mode: " + modeProperty);
            }
        }

        // Also check command line args for -headless flag
        for (String arg : args) {
            if ("-headless".equalsIgnoreCase(arg) || "--headless".equalsIgnoreCase(arg)) {
                operatingmode = OperatingMode.HEADLESS;
                break;
            }
        }

        // Skip GUI initialization for non-GUI modes
        if (operatingmode != OperatingMode.REMOTE && operatingmode != OperatingMode.MMF
                && operatingmode != OperatingMode.HEADLESS) {
            setDefaultFonts();
        }
        Locale.setDefault(Locale.ENGLISH);
        startTime = System.currentTimeMillis();

        GlobalFilePathes.PFAD_JAR_HOME = GetJarPath.getJarPath();

        if (operatingmode != OperatingMode.REMOTE && operatingmode != OperatingMode.MMF
                && operatingmode != OperatingMode.HEADLESS) {
            System.out.println("Starting GeckoCIRCUITS...");
        }

        // Handle HEADLESS mode - skip all GUI initialization
        if (operatingmode == OperatingMode.HEADLESS) {
            System.out.println("GeckoCIRCUITS running in HEADLESS mode");
            loadApplicationProperties();
            mainLoaded = true;
            System.out.println("GeckoCIRCUITS HEADLESS is ready");
            // Headless mode exits here - the REST API or CLI should handle further processing
            return;
        }

        if (testIfBrandedVersion()) {
            MainWindow.IS_BRANDED = true;
        }

        //
        loadApplicationProperties();
        //--------------------
        // -Xmx512m
        // java -Xmx512m -jar GeckoCIRCUITS.jar
        // restart with more memory, if necessary!
        String memorySize = applicationProps.getProperty("MEMORY");
        Integer reqMem = Integer.parseInt(memorySize);

        if (operatingmode == OperatingMode.REMOTE) {
            String javaCommand = applicationProps.getProperty("JAVACOMMAND");
            for(int i = 0; i < args.length; i++) {
                if("-j".equals(args[i])) {
                    javaCommand = args[i+1];
                }
            }
                        
            if (JavaMemoryRestart.startNewGeckoCIRCUITSJVM(reqMem, args, javaCommand)) {
                System.out.println("GeckoCIRCUITS should now be running outside of MATLAB at port " + args[1]);
                remoteLoaded = true;
                remoteLoading = false;
                return;//System.exit(12);
            } else {
                System.out.println("ERROR: Could not start GeckoCIRCUITS.");
                remoteLoaded = false;
                remoteLoading = false;
                return;//System.exit(12);
            }

        } else if (operatingmode == OperatingMode.MMF) {
            String javaCommand = applicationProps.getProperty("JAVACOMMAND");
            for(int i = 0; i < args.length; i++) {
                if("-j".equals(args[i])) {
                    javaCommand = args[i+1];
                }
            }
                        
            if (JavaMemoryRestart.startNewGeckoCIRCUITSJVM(reqMem, args, javaCommand)) {
                System.out.println("GeckoCIRCUITS should now be running outside of MATLAB and accessible via file " + args[1] + " of size " + args[2]);
                mmfLoaded = true;
                mmfLoading = false;
                return;//System.exit(12);
            } else {
                System.out.println("ERROR: Could not start GeckoCIRCUITS.");
                mmfLoaded = false;
                mmfLoading = false;
                return;//System.exit(12);
            }
        }

        if (operatingmode != OperatingMode.SIMULINK && operatingmode != OperatingMode.EXTERNAL
                && !GeckoSim._isTestingMode) {
            if (JavaMemoryRestart.isMemoryRestartRequired(reqMem)
                    && JavaMemoryRestart.startNewGeckoCIRCUITSJVM(reqMem, args, applicationProps.getProperty("JAVACOMMAND"))) {
                System.exit(12);
            }
        }
        _geckoSim = new GeckoSim();

        _geckoSim.initialisiere();
        if (!MainWindow.IS_BRANDED) {
            SystemOutputRedirect.init();
        }


        // test if branded version is used:
        if (testIfBrandedVersion()) {
            try {
                InputStream is = GeckoSim.class.getResourceAsStream("/brand.ipes");
                _win.openFile(new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {
            //--------------------
            // load file from command line argument
            if (args.length > 0) {
                if (!(args[0].equals("-p") || args[0].equals("-mm"))) { //first argument could for port number or file for remote access
                    final File file = new File(args[0]);
                    System.out.println(args[0]);

                    loadFileSwingThreadSave(file.getAbsolutePath());
                }

                //set up remote access                
                if (args.length > 0) {
                    //search for argument -p - next argument is port number
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].equals("-p")) {
                            //create GeckoRemote object and add to listener                            
                            try {
                                //if args is -p, without anything following it, it is a mistake, but to catch the error, set to default port number 
                                //otherwise parse the next argument                             

                                if (i != (args.length - 1)) {
                                    GeckoRemoteRegistry.setRemoteAccessPort(Integer.parseInt(args[i + 1]));
                                }
                                GeckoRemoteRegistry.enableRemotePort();
                            } catch (Throwable err) {
                                err.printStackTrace();
                                System.exit(4);
                            }

                            break;
                        } else if (args[i].equals("-mm")) { //memory mapped file access
                            try {
                                //there must be one additional argument - the file name
                                //there should also be a second one - the file size - but if this one is missing we can set the default size
                                if (i != (args.length -1)) {
                                    final String fileName = args[i+1];
                                    final long fileSize;
                                    if ((i+1) != (args.length - 1)) {
                                        fileSize = Long.parseLong(args[i+2]);
                                    } else {
                                        fileSize = GeckoMemoryMappedFile._defaultBufferSize;
                                    }
                                    MainWindow._mmf_access = new GeckoCustomMMF(MainWindow._scripter);
                                    MainWindow._mmf_access.enableAccess(fileName, fileSize);
                                } else {
                                    System.err.println("No file given for memory-mapped access.");
                                    System.exit(4);
                                }
                            } catch (Throwable err) {
                                err.printStackTrace();
                                System.exit(4);
                            }
                        }
                    }
                }
            } else {
                // no command line argument given, try to load .ipes file that was used last time:
                final File lastFile = new File(GlobalFilePathes.RECENT_CIRCUITS_1);
                if (!_isTestingMode && lastFile.exists() && !lastFile.isDirectory()) {

                    loadFileSwingThreadSave(lastFile.getAbsolutePath());
                }
            }
        }
        mainLoaded = true;

        // don't modify this string, since it is used in the Java-Memory-Restart
        System.out.println("GeckoCIRCUITS is ready");
    }

    public static boolean testIfBrandedVersion() {
        URL brandURL = GeckoSim.class.getResource("/brand.ipes");
        return brandURL != null;
    }

    public GeckoSim() {
    }

    private void initialisiere() {
        //new LangInit(args); 
        LangInit.initEnglish();

        GlobalFilePathes.PFAD_JAR_HOME = GetJarPath.getJarPath();
        //---------

        this.checkJavaVersion();
        checkIfOpenedFromZipfile();
        this.checkIfLibraryIsMissing();
        GlobalFilePathes.PFAD_PICS_URL = GetJarPath.getPathPICS();

        loadApplicationProperties();
        this.loadPropertyFile();

        
        if (Arrays.asList("gnome-shell", "mate", "other...").contains(System.getenv("DESKTOP_SESSION"))) {
            try {
                Class<?> xwm = Class.forName("sun.awt.X11.XWM");
                Field awt_wmgr = xwm.getDeclaredField("awt_wmgr");
                awt_wmgr.setAccessible(true);
                Field other_wm = xwm.getDeclaredField("OTHER_WM");
                other_wm.setAccessible(true);
                if (awt_wmgr.get(null).equals(other_wm.get(null))) {
                    Field metacity_wm = xwm.getDeclaredField("METACITY_WM");
                    metacity_wm.setAccessible(true);
                    awt_wmgr.set(null, metacity_wm.get(null));
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
       
        _win = new MainWindow();


        this.performScreenSettings(_win);
        //=================================

        _win.setActivationOfSimulator(true);  // is continuously checked at a later location

        if (_initialShow) {
            _win.setVisible(true);
        }
        _win.setSimulationMenu();
    }

    /**
     * this loads the app properties only once!
     */
    public static void loadApplicationProperties() {

        if (applicationProps != null) {
            return; // just load the properties once!
        }
        forceLoadApplicationProperties();
    }

    public static void forceLoadApplicationProperties() {

        //-----------------------------
        // initialization of property objects. If no applicaton property is found, just use the
        // default property file, which should be within the jar-File
        try {  // create and load default properties
            InputStream is = GeckoSim.class.getResourceAsStream(DEFAULT_PROPERTY_FILE);
            if (is == null) {
                System.err.println("SEVERE: could not find default properties file, exiting!");
                System.exit(-1);
            } else {
                try (InputStream stream = is) {
                    defaultProps = new Properties();
                    defaultProps.load(stream);
                    applicationProps = new Properties(defaultProps);
                }
            }

            // now load properties from last invocation
            if (!MainWindow.IS_BRANDED) {
                try {
                    APPLICATION_PROPERTY_FILE = GetJarPath.getJarPath() + "GeckoProperties.prp";
                    try {
                        File appDataDir = findOrCreateAppDataDirectory();
                        APPLICATION_PROPERTY_FILE = appDataDir.getAbsolutePath() + "/GeckoProperties.prp";
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    File existsText = new File(APPLICATION_PROPERTY_FILE);
                    if (existsText.exists()) {
                        try (FileInputStream in = new FileInputStream(APPLICATION_PROPERTY_FILE)) {
                            applicationProps.load(in);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            Logger.getLogger(GeckoSim.class.getName()).log(Level.SEVERE, "Could not find file: " + e.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(GeckoSim.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void loadPropertyFile() {
        //System.out.println("prop= "+GlobalFilePathes.PFAD_JAR_HOME+"GeckoProperties.prp"); 

        try {
            GlobalFilePathes.RECENT_CIRCUITS_1 = applicationProps.getProperty("RECENT_CIRCUITS_1");
            GlobalFilePathes.RECENT_CIRCUITS_2 = applicationProps.getProperty("RECENT_CIRCUITS_2");
            GlobalFilePathes.RECENT_CIRCUITS_3 = applicationProps.getProperty("RECENT_CIRCUITS_3");
            GlobalFilePathes.RECENT_CIRCUITS_4 = applicationProps.getProperty("RECENT_CIRCUITS_4");

        } catch (Exception e) {
        }
    }

    // funktioniert nur, wenn Java 1.6 installiert ist -> 
    private void checkJavaVersion() {
        try {
            Properties sysProp = System.getProperties();
            String javaVersion = sysProp.getProperty("java.runtime.version");
            double jV = Double.parseDouble(javaVersion.replace("+", ".").substring(0, 3));

            if (jV < 1.6) {
                StringBuffer errorMessage = new StringBuffer();
                errorMessage.append(DialogAbout.VERSION + " needs Java 1.6 (or higher) to be installed\non your computer.");
                errorMessage.append("Currently you employ Java " + javaVersion + " Currently you employ Java ");

                JOptionPane.showMessageDialog(null,
                        errorMessage.toString(),
                        "Memory error!",
                        JOptionPane.ERROR_MESSAGE);

                if (operatingmode == OperatingMode.STANDALONE) {
                    System.exit(0);
                } else if (operatingmode == OperatingMode.SIMULINK || operatingmode == OperatingMode.EXTERNAL) {
                    _win.dispose();
                }

            }
        } catch (Throwable err) {
            System.err.println("Could not check java version.");
        }
    }
    
    public static boolean scriptEngineAvailable = false;
    
    private void checkIfLibraryIsMissing() {
        try {
            javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
            if (compiler != null) {
                compiler_toolsjar_missing = false;
                System.out.println("Java Compiler found: " + compiler.getClass().getName());
            } else {
                compiler_toolsjar_missing = true;
                System.err.println("ERROR: Java Compiler not found. JDK is required (JRE is not sufficient).");
                System.err.println("Current Java version: " + System.getProperty("java.version"));
                System.err.println("Java vendor: " + System.getProperty("java.vendor"));
                System.err.println("Java home: " + System.getProperty("java.home"));
            }
            
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("js");
            if (engine == null) {
                engine = manager.getEngineByExtension("js");
            }
            if (engine == null) {
                engine = manager.getEngineByMimeType("text/javascript");
            }
            if (engine == null) {
                engine = manager.getEngineByMimeType("application/javascript");
            }
            scriptEngineAvailable = (engine != null);
            
        } catch (NoClassDefFoundError | SecurityException err) {
            scriptEngineAvailable = false;
        } catch (Throwable ex) {
            scriptEngineAvailable = false;
        }
    }

    private void performScreenSettings(MainWindow win) {
        GraphicsEnvironment grenv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice grdev = grenv.getDefaultScreenDevice();
        int h = (int) grdev.getDefaultConfiguration().getBounds().getHeight();
        int b = (int) grdev.getDefaultConfiguration().getBounds().getWidth();
        if ((b <= 640) && (h <= 480)) {
            win.setSize(640, 480);
        } else if ((b <= 1000) && (h <= 1000)) {
            win.setSize((int) (0.90 * b), (int) (0.80 * h));
        } else {
            win.setSize(1000, 800);
        }

        win.setLocationByPlatform(true);
    }

    public static void saveProperties() {
        try {

            // bad hack: if property is found in defaultProperties, and not in
            // application properties, this will save the missing property also
            // in the newly created application-property file.
            Enumeration<?> propNames = applicationProps.propertyNames();
            while (propNames.hasMoreElements()) {
                String key = propNames.nextElement().toString();
                applicationProps.setProperty(key, applicationProps.getProperty(key));
            }


            File file = new File(APPLICATION_PROPERTY_FILE);
            Logger.getLogger(GeckoSim.class.getName()).log(Level.CONFIG, "Saving system properties: " + file.getAbsolutePath());
            try (FileOutputStream out = new FileOutputStream(file)) {
                applicationProps.store(out, "--- GeckoCIRCUITS Property File ---");
            }
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger(GeckoSim.class.getName()).log(Level.WARNING, "Could not write property file. Perhaps we do not have file write permissions.", "");
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeckoSim.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static File findOrCreateAppDataDirectory() {
        String dataFolder = System.getenv("LOCALAPPDATA");
        if (dataFolder == null) { // this is the case in linux systems!
            dataFolder = System.getProperty("user.home");
            if (new File(dataFolder).exists()) {
                final File geckoAppData = new File(dataFolder + "/.GeckoCIRCUITS");
                if (!geckoAppData.exists()) {
                    if (!geckoAppData.mkdir()) {
                        System.err.println("Warning: Could not create directory: " + geckoAppData.getAbsolutePath());
                    }
                }
                if (geckoAppData.exists()) {
                    return geckoAppData;
                }
            }

        }
        final File appDataDir = new File(dataFolder);
        if (appDataDir.exists()) {
            final File geckoAppData = new File(dataFolder + "/.GeckoCIRCUITS");
            if (!geckoAppData.exists()) {
                if (!geckoAppData.mkdir()) {
                    System.err.println("Warning: Could not create directory: " + geckoAppData.getAbsolutePath());
                }
            }
            if (geckoAppData.exists()) {
                return geckoAppData;
            }

        }

        return null;
    }

    private static void setDefaultFonts() {

        Font defaultFont = new Font("Arial Unicode MS", Font.PLAIN, 12);

        UIManager.put("Button.font", defaultFont);
        UIManager.put("ToggleButton.font", defaultFont);
        UIManager.put("RadioButton.font", defaultFont);
        UIManager.put("CheckBox.font", defaultFont);
        UIManager.put("ColorChooser.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("List.font", defaultFont);
        UIManager.put("MenuBar.font", defaultFont);
        UIManager.put("MenuItem.font", defaultFont);
        UIManager.put("RadioButtonMenuItem.font", defaultFont);
        UIManager.put("CheckBoxMenuItem.font", defaultFont);
        UIManager.put("Menu.font", defaultFont);
        UIManager.put("PopupMenu.font", defaultFont);
        UIManager.put("OptionPane.font", defaultFont);
        UIManager.put("Panel.font", defaultFont);
        UIManager.put("ProgressBar.font", defaultFont);
        UIManager.put("ScrollPane.font", defaultFont);
        UIManager.put("Viewport.font", defaultFont);
        UIManager.put("TabbedPane.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("PasswordField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("TextPane.font", defaultFont);
        UIManager.put("EditorPane.font", defaultFont);
        UIManager.put("TitledBorder.font", defaultFont);
        UIManager.put("ToolBar.font", defaultFont);
        UIManager.put("ToolTip.font", defaultFont);
        UIManager.put("Tree.font", defaultFont);
    }

    /**
     * when loading the file from the main thread, the gui can throw exceptions,
     * since swing is not thread-safe! Therefore, do this in the event dispatch
     * thread.
     */
    private static void loadFileSwingThreadSave(final String absolutePath) {
        try {
            _win.openFile(absolutePath);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GeckoSim.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void checkIfOpenedFromZipfile() {        
        if (GlobalFilePathes.PFAD_JAR_HOME.contains(".zip")) {
            JOptionPane.showMessageDialog(null,
                    "Warning: Probably GeckoCIRCUITS was executed from within a compressed"
                    + "\narchive (.zip). Please unzip the archive before executing GeckoCIRCUITS.\n",
                    "Error!",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
