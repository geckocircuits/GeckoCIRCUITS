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
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class GeckoSim extends JApplet {

    public static long startTime;
    public static Fenster _win;
    static GeckoSim _geckoSim;
    private static ByteBuffer _systemMatrixBuffer;
    private static DoubleBuffer _sysDB;
    public static boolean _initialShow = true; // this is used from the infineon applet!    
    private String[] datnamExampleApplet;
    private String datnamStartApplet;
    public static URL urlApplet;
    private boolean javaVersionAppletOK = true;
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

    public static void stopTime() {
        long stopTime = System.currentTimeMillis();
        System.out.println("total execution:  " + (stopTime - startTime) / 1000.0);
        System.exit(3);
    }

    public static void main(final String[] args) {
        if (operatingmode != OperatingMode.REMOTE && operatingmode != OperatingMode.MMF) {
            setDefaultFonts();
        }                
        Locale.setDefault(Locale.ENGLISH);
        startTime = System.currentTimeMillis();
        
        /*try {
        for (javax.swing.UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/
        
        //UIManager.put("nimbusBase",Color.getHSBColor(1.33f, 0.9f, 0.8f));
        //UIManager.put("nimbusBlueGrey", Color.getHSBColor(1.33f, 0.4f, 0.9f));
        //UIManager.put("control", Color.getHSBColor(1.33f, 0.3f, 0.8f));

        
        GlobalFilePathes.PFAD_JAR_HOME = GetJarPath.getJarPath();

        if (operatingmode != OperatingMode.REMOTE && operatingmode != OperatingMode.MMF) {
            System.out.println("Starting GeckoCIRCUITS...");
        }

        if (testIfBrandedVersion()) {
            Fenster.IS_BRANDED = true;
            Fenster.IS_APPLET = true;

        } else {
            Fenster.IS_APPLET = false;  // is set to 'true' in its declaration
        }

        Fenster.IS_APPLET = false;  // is set to 'true' in its declaration
        //
        loadApplicationProperties();
        //--------------------
        // -Xmx512m
        // java -Xmx512m -jar GeckoCIRCUITS.jar
        // restart with more memory, if necessary!
        String memorySize = applicationProps.getProperty("MEMORY");
        Integer reqMem = new Integer(memorySize);

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
                && !Fenster.IS_APPLET && !GeckoSim._isTestingMode) {
            if (JavaMemoryRestart.isMemoryRestartRequired(reqMem)
                    && JavaMemoryRestart.startNewGeckoCIRCUITSJVM(reqMem, args, applicationProps.getProperty("JAVACOMMAND"))) {
                System.exit(12);
            }
        }
        _geckoSim = new GeckoSim();
        
        _geckoSim.initialisiere();
        if (!Fenster.IS_APPLET && !Fenster.IS_BRANDED) {
            SystemOutputRedirect.init();
        }

        
        // test if branded version is used:
        if (testIfBrandedVersion()) {
            try {
                InputStream is = GeckoSim.class.getResourceAsStream("/brand.ipes");
                _win.openFile(new BufferedReader(new InputStreamReader(new GZIPInputStream(is))));
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
                                    Fenster._mmf_access = new GeckoCustomMMF(Fenster._scripter);
                                    Fenster._mmf_access.enableAccess(fileName, fileSize);
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

    // JApplet --> 
    @Override
    public void init() {

        Fenster.IS_APPLET = true;
        GeckoSim.urlApplet = this.getCodeBase();  // vom Applet
        setDefaultFonts();
        GlobalFilePathes.PFAD_JAR_HOME = GetJarPath.getJarPath();

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (_win == null) {
                    return;
                }
                _win.setVisible(true);
                try {
                    _win.openFile(datnamStartApplet);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(GeckoSim.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        //---------
        try {
            int anz = (new Integer(this.getParameter("nrExample"))).intValue();
            datnamExampleApplet = new String[anz];
            for (int i1 = 0; i1 < anz; i1++) {
                String dn = "dat";
                if (i1 < 10) {
                    dn += "0";
                }
                dn += ("" + (i1 + 1));
                datnamExampleApplet[i1] = this.getParameter(dn);
            }
            datnamStartApplet = this.getParameter("startFile");
        } catch (Exception e) {
            System.out.println(e);
        }
        operatingmode = OperatingMode.STANDALONE;
        this.initialisiere();
    }

    // JApplet --> 
    @Override
    public void paint(Graphics g) {
        g.setColor(Color.orange);
        g.fillRect(0, 0, 500, 300);
        int x0 = 10, y0 = 0, dy = 17;
        //--------
        if (javaVersionAppletOK) {
            g.setColor(Color.black);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("GeckoCIRCUITS  -  Click Here!", x0, y0 + 1 * dy);
            g.setFont(GlobalFonts.foAUSWAHL);
            g.drawString("Java-Applet Mode: Limited Functionality", x0, y0 + 2 * dy);
        } else {
            g.setColor(Color.black);
            g.setFont(GlobalFonts.foAUSWAHL);
            g.drawString("GeckoCIRCUITS", x0, y0 + 1 * dy);
            g.setFont(GlobalFonts.foAUSWAHL);
            g.drawString("Java-Applet Mode: Limited Functionality", x0, y0 + 2 * dy);
            g.setColor(Color.red);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("Java 1.6 needs to be installed!", x0, y0 + 3 * dy);
        }
    }

    private void initialisiere() {
        //new LangInit(args); 
        LangInit.initEnglish();

        GlobalFilePathes.PFAD_JAR_HOME = GetJarPath.getJarPath();
        //---------
        // Find out if it is Applet or SimulinkObject:
        // Only if parameters are available (from HTML) it is supposed to be Applet
        try {
            int anz = (new Integer(this.getParameter("nrExample"))).intValue();  // throws exception if not Applet
            Fenster.IS_APPLET = true;

        } catch (Exception e) {
            if (Fenster.IS_BRANDED) {
                Fenster.IS_APPLET = true;
            } else {
                Fenster.IS_APPLET = false;
            }

        }
        //=================================
        // this.logErrorsIntoFile();
        //System.setProperty("file.encoding", "shift_jis");          //This doesn't change anything that I can tell
        this.checkJavaVersion();
        if (Fenster.IS_APPLET) {
            if (!javaVersionAppletOK) {
                return;
            }
        } else {
            checkIfOpenedFromZipfile();
        }
        this.checkIfLibraryIsMissing();
        GlobalFilePathes.PFAD_PICS_URL = GetJarPath.getPathPICS();
        //System.out.println("PFAD_JAR_HOME -->   "+GlobalFilePathes.PFAD_JAR_HOME+"\nPFAD_PICS_URL --> "+GlobalFilePathes.PFAD_PICS_URL+"\n--------------");
        loadApplicationProperties();
        this.loadPropertyFile();

        //=================================
        // eigentliches Programm starten und Anpassen an Bildschirmabmessungen -->
        //PiccoloTest test = new PiccoloTest();

        
        if (!Fenster.IS_APPLET && Arrays.asList("gnome-shell", "mate", "other...").contains(System.getenv("DESKTOP_SESSION"))) {
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
       
        _win = new Fenster();


        this.performScreenSettings(_win);
        //=================================
        //boolean zeitLizenzOK= lizenzierung.isLicenceOK(Lizenzierung.LIZENZTYP_ZEITDAUER); 
        //boolean macLizenzOK=  lizenzierung.isLicenceOK(Lizenzierung.LIZENZTYP_MAC_GUELTIG); 
        _win.setActivationOfSimulator(true);  // wird an spaeterer Stelle laufend gecheckt
//        if (zeitLizenzOK && macLizenzOK) win.setActivationOfSimulator(true); else win.setActivationOfSimulator(false);
        //=================================

        if (Fenster.IS_APPLET && !Fenster.IS_BRANDED) {
            _win.setAppletFiles(datnamExampleApplet);
            //win.setVisible(true);  // --> wird erst bei Maus-Klick im Applet-Modus aktiviert! ??
        } else {
            if (_initialShow) {
                _win.setVisible(true);
            }
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
                defaultProps = new Properties();
                defaultProps.load(is);
                is.close();
                applicationProps = new Properties(defaultProps);
            }

            // now load properties from last invocation
            if (!Fenster.IS_APPLET && !Fenster.IS_BRANDED) {
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
                        FileInputStream in = new FileInputStream(APPLICATION_PROPERTY_FILE);
                        applicationProps.load(in);
                        in.close();
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
            if (Fenster.IS_APPLET) {
                String javaVersion = System.getProperty("java.version");
                double jV = (new Double(javaVersion.substring(0, 3))).doubleValue();
                System.out.println("AppletMode --> " + javaVersion + "   " + jV);
                if (jV < 1.6) {
                    javaVersionAppletOK = false;
                    this.repaint();
                }
                return;
            }
            //--------------------
            Properties sysProp = System.getProperties();
            //sysProp.list(System.out);
            String javaVersion = sysProp.getProperty("java.runtime.version");
            double jV = (new Double(javaVersion.replace("+", ".").substring(0, 3))).doubleValue();

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

    private void checkIfLibraryIsMissing() {
        if (Fenster.IS_APPLET) {
            return;
        }

        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            //------------------------------------------
            // Was ist mit 'tools.jar', die der CONTROL-Block 'JAVA-Function' als Compiler braucht? 
            try {
                cl.loadClass("com.sun.tools.javac.Main");
                compiler_toolsjar_missing = false;
            } catch (Exception e) {
                compiler_toolsjar_missing = true;
            }
        } catch (UnsupportedClassVersionError err) {
            JOptionPane.showMessageDialog(null,
                    err.toString() + "\nWrong version of library tools.jar. Please enshure to use a lib/tools.jar file version\n"
                    + "compatible with your Java JVM. GeckoCIRCUITS is currently running with JVM " + System.getProperty("java.version"),
                    "Library Memory error!",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private void logErrorsIntoFile() {
        if (Fenster.IS_APPLET) {
            return;
        }
        try {
            String LOGFILE_NAME = "GeckoCIRCUITS.log";
            PrintStream ps = new PrintStream(new FileOutputStream(LOGFILE_NAME));
            System.setErr(ps);
            System.setOut(ps);
        } catch (Exception e) {
            System.out.println(e + "   erqogfn32");
        }
    }

    private void performScreenSettings(Fenster win) {
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

        // System.out.println("Detected Screen-Size:  "+b+" x "+h);
    }

    public static void saveProperties() {
        FileOutputStream out = null;
        try {

            // bad hack: if property is found in defaultProperties, and not in
            // application properties, this will save the missing property also
            // in the newly created application-property file.
            Enumeration propNames = applicationProps.propertyNames();
            while (propNames.hasMoreElements()) {
                String key = propNames.nextElement().toString();
                applicationProps.setProperty(key, applicationProps.getProperty(key));
            }


            File file = new File(APPLICATION_PROPERTY_FILE);
            Logger.getLogger(GeckoSim.class.getName()).log(Level.CONFIG, "Saving system properties: " + file.getAbsolutePath());
            out = new FileOutputStream(file);
            applicationProps.store(out, "--- GeckoCIRCUITS Property File ---");
            out.close();
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
                    geckoAppData.mkdir();
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
                geckoAppData.mkdir();
            }
            if (geckoAppData.exists()) {
                return geckoAppData;
            }

        }

        return null;
    }

    private static void setDefaultFonts() {
//        UIManager.getLookAndFeelDefaults()
//                .put("defaultFont", );

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
