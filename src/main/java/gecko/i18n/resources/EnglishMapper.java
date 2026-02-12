/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
/*
 * Class used to initialize English key-value pairs.  
 * -No connection to the Wiki Database is needed.
 */
package gecko.i18n.resources;

import gecko.i18n.DoubleMap;

public class EnglishMapper { 

    /**
     * Creates a new map with all single-line English key-value pairs and
     * returns it
     *
     * @return DoubleMap containing all single-line English key-value pairs
     */
    public static DoubleMap initEnglishMap_single() {
        DoubleMap dm = new DoubleMap();

        for(I18nKeys value : I18nKeys.values()) {
            dm.insertPair(value, value._englishTranslation);
        }
        
//        // initialize single-line pairs
//        dm.putPair("File", "File");
//        dm.putPair("New", "New");
//        dm.putPair("Open", "Open");
//        dm.putPair("Save", "Save");
//        dm.putPair("SaveAs", "Save As");
//        dm.putPair("SaveAsApplet", "Save as Applet");
//        dm.putPair("SaveViewAsImage", "Save View as Image");
//        dm.putPair("Edit", "Edit");
//        dm.putPair("Undo", "Undo");
//        dm.putPair("Redo", "Redo");
//        dm.putPair("undonotavailable", "Undo (not available)");
//        dm.putPair("redonotavailable", "Redo (not available)");
//        dm.putPair("MoveElements", "Move Elements");
//        dm.putPair("CopyElements", "Copy Elements");
//        dm.putPair("DeleteElements", "Delete Elements");
//        dm.putPair("Deselect", "Deselect");
//        dm.putPair("SelectAll", "Select all");
//        dm.putPair("SelectAllCommand", "SelectAll");
//        dm.putPair("EnableDisable", "Enable / Disable");
//        dm.putPair("Disable", "Disable");
//        dm.putPair("ShortCircuitComponent", "Short-Circuit Component");
//        dm.putPair("Import", "Import");
//        dm.putPair("Export", "Export");
//        dm.putPair("Simulation", "Simulation");
//        dm.putPair("SelectALicense", "Select a license");
//        dm.putPair("ProvideLicenseFile", "Provide a license file");
//        dm.putPair("View", "View");
//        dm.putPair("WorksheetSize", "Worksheet Size");
//        dm.putPair("Scaling", "Scaling");
//        dm.putPair("FontSize", "Font Size");
//        dm.putPair("useantialiasing", "Use Antialiasing");
//        dm.putPair("Name", "Name");
//        dm.putPair("ShowParameter", "Show Parameter");
//        dm.putPair("ShowTextLine", "Show Text-Line");
//        dm.putPair("FlowDirection", "Flow Direction");
//        dm.putPair("CheckConnections", "Check Connections");
//        dm.putPair("CheckControlModel", "Check control model");
//        dm.putPair("FindInModel", "Find in model");
//        dm.putPair("SetParameters", "Set Parameters");
//        dm.putPair("SetParametersCommand", "setParameters");
//        dm.putPair("SetOrderOfControlBlocks", "Set Order of Control Blocks");
//        dm.putPair("setOrderCommand", "setOrder");
//        dm.putPair("MemorySettings", "Memory Settings");
//        dm.putPair("MemorySettingsCommand", "memorySettings");
//        dm.putPair("RemoteAccessSettings", "Remote Access Settings");
//        dm.putPair("remoteSettings", "remoteSettings");
//        dm.putPair("Help", "Help");
//        dm.putPair("About", "About");
//        dm.putPair("Licensing", "Licensing");
//        dm.putPair("Feedback", "Feedback");
//        dm.putPair("Updates", "Updates");
//        dm.putPair("GECKO", "GECKO");
//        dm.putPair("GECKOSCRIPT", "GeckoSCRIPT");
//        dm.putPair("GECKOOPTIMIZER", "GeckoOPTIMIZER");
//        dm.putPair("GECKOHEAT", "GeckoHEAT");
//        dm.putPair("GECKOMAGNETICS", "GeckoMAGNETICS");
//        dm.putPair("GECKOEMC", "GeckoEMC");
//        dm.putPair("Exit", "Exit");
//        dm.putPair("CIRCUIT", "Circuit");
//        dm.putPair("MOTOR&EMI", "Motor & EMI");
//        dm.putPair("Subcircuit", "Subcircuit");
//        dm.putPair("Thermal", "Thermal");
//        dm.putPair("Reluctance", "Reluctance");
//        dm.putPair("Control", "Control");
//        dm.putPair("Measure", "Measure");
//        dm.putPair("Digital", "Digital");
//        dm.putPair("Math", "Math");
//        dm.putPair("SourceSink", "Source/Sink");
//        dm.putPair("Special", "Special");
//        dm.putPair("ResistorR", "Resistor R [Ohm]");
//        dm.putPair("ResistorRth", "Resistor Rth [K/W]");
//        dm.putPair("VoltageSource", "Voltage Source U[V]");
//        dm.putPair("MMFampturns", "MMF [amp-turns]");
//        dm.putPair("DefinedTemp", "Defined Temperature [\u00b0C]");
//        dm.putPair("CapacitorC", "Capacitor C [F]");
//        dm.putPair("CapacitorCth", "Capacitor Cth [J/K]");
//        dm.putPair("CurrentSourceI", "Current Source I[A]");
//        dm.putPair("HeatSourceW", "Heat Source [W]");
//        dm.putPair("Diode", "Diode");
//        dm.putPair("IdealTransformer", "Ideal Transformer");
//        dm.putPair("OperationalAmplifier", "Operational Amplifier");
//        dm.putPair("BipolarTransistor", "Bipolar Transistor");
//        dm.putPair("Thyristor", "Thyristor");
//        dm.putPair("IGBT", "IGBT");
//        dm.putPair("IdealSwitch", "Ideal Switch");
//        dm.putPair("MagneticCouplingk", "Magnetic Coupling k");
//        dm.putPair("InductorCouplingLc", "Inductor Coupling Lc [H]");
//        dm.putPair("InductorL", "Inductor L[H]");
//        dm.putPair("DCMachine", "DC Machine");
//        dm.putPair("PMSM", "PMSM");
//        dm.putPair("SMSAL", "SM-SAL");
//        dm.putPair("SMRO", "SM-RO");
//        dm.putPair("InductionMachine", "Induction Machine");
//        dm.putPair("IMCAGE", "IM-CAGE");
//        dm.putPair("IMSAT", "IM-SAT");
//        dm.putPair("LISN", "LISN");
//        dm.putPair("ThermalTerminal", "Thermal Terminal");
//        dm.putPair("ControlTerminal", "Control Terminal");
//        dm.putPair("ReluctanceTerminal", "Reluctance Terminal");
//        dm.putPair("GlobalReluctanceTerminal", "Global Reluctance Terminal");
//        dm.putPair("GlobalCircuitTerminal", "Global Circuit Terminal");
//        dm.putPair("CircuitTerminal", "Circuit Terminal");
//        dm.putPair("GlobalThermalTerminal", "Global Thermal Terminal");
//        dm.putPair("ControlMUX", "Control MUX");
//        dm.putPair("ControlDEMUX", "Control DEMUX");
//        dm.putPair("t1", "This Model file was created with an older version of GeckoCIRCUITS.");
//        dm.putPair("t2", "When you save model files, please consider that you cannot open files");
//        dm.putPair("MOSFET", "MOSFET");
//        dm.putPair("t3", "generated by this Version of GeckoCIRCUITS with older releases.");
//        dm.putPair("info", "Info");
//        dm.putPair("aliasingCommand", "aliasingCommand");
//        dm.putPair("10point", "10 point");
//        dm.putPair("12point", "12 point");
//        dm.putPair("14point", "14 point");
//        dm.putPair("16point", "16 point");
//        dm.putPair("18point", "18 point");
//        dm.putPair("6point", "6 point");
//        dm.putPair("8point", "8 point");
//        dm.putPair("Tools", "Tools");
//        dm.putPair("ElectroMagnetic3D", "Electromagnetic 3D-Model");
//        dm.putPair("FilterOptimization", "Filter Optimization");
//        dm.putPair("optimize", "optimize");
//        dm.putPair("TestRthExtractor", "Test Rth/Cth-Extractor");
//        dm.putPair("CircuitSimulationFiles", "Circuit Simulation Files (*.ipes)");
//        dm.putPair("CircuitSimulationFilesipes", "Circuit Simulation Files (*.ipes)");
//        dm.putPair("WarningExclamation", "Warning!");
//        dm.putPair("WarningCheckAutoBackupDisbled", "Warning: Check for auto-backup file disabled!");
//        dm.putPair("SaveAppletCommand", "SaveApplet");
//        dm.putPair("DisableShort", "DisableShort");
//        dm.putPair("jarSimulationAppletText", ".jar Simulation Applet (*.jar)");
//        dm.putPair("ErrorExcl", "Error!");
//        dm.putPair("StoppedAfter", "Stopped after ");
//        dm.putPair("InitTxtStart", "Init TxtI.ti_InitStart Start");
//        dm.putPair("Pause", "Pause");
//        dm.putPair("Continue", "Continue");
//        dm.putPair("t12", "Aborting program exit!");
//        dm.putPair("CannotExitApplet", "Cannot exit applet. Setting invisible!");
//        dm.putPair("Warning", "Warning");
//        dm.putPair("SystemSimulator", "System Simulator");
//        dm.putPair("errorcontrolblock", "Error! Could not create control block with id:");
//        dm.putPair("filenotfound", "ERROR: File not found");
//        dm.putPair("Ok", "Ok");
//        dm.putPair("controlportsconnection", "Control Ports without connection");
//        dm.putPair("DialogControlCheck.jLabel2.text", "Input ports");
//        dm.putPair("DialogControlCheck.jLabel3.text", "Output ports");
//        dm.putPair("DialogControlOrderN.jLabel2.text", "User Defined Order");
//        dm.putPair("DialogControlOrderN.jButtonReset.text", "Reset");
//        dm.putPair("DialogControlOrderN.jLabel1.text", "Optimized Order");
//        dm.putPair("DialogFeedback.jButtonLaunch.text", "Launch Browser");
//        dm.putPair("DialogFeedback.jButtonClose.text", "Close Window");
//        dm.putPair("DialogFindInModel.jCheckBoxExact.text", "Exact match");
//        dm.putPair("DialogFindInModel.jLabel1.text", "Search for String:");
//        dm.putPair("DialogFindInModel.jLabel2.text", "Found items:");
//        dm.putPair("DialogFindInModel.jTextField.text", "Please insert search text");
//        dm.putPair("DialogFindInModel.jCheckBoxIgnore.text", "Ignore case");
//        dm.putPair("DialogFindInModel.jButtonClose.text", "Close window");
//        dm.putPair("DialogLicensing.title", "GeckoCIRCUITS Licensing");
//        dm.putPair("DialogMakeExternal.jLabel1.text", "File path:");
//        dm.putPair("DialogMakeExternal.title", "Convert internal file to external file");
//        dm.putPair("DialogMakeExternal.jPanel1.border.title", "Status");
//        dm.putPair("DialogMakeExternal.jButtonUseExt.text", "Use file from disk");
//        dm.putPair("DialogMakeExternal.jButtonCreate.text", "Create file");
//        dm.putPair("DialogMemory.title", "GeckoCIRCUITS Memory");
//        dm.putPair("DialogMemory.jPanel5.border.title", "Startup memory");
//        dm.putPair("DialogMemory.jLabel7.text", "(see \"Info-tab\" for information)");
//        dm.putPair("DialogMemory.jLabel4.text", "Memory at startup:");
//        dm.putPair("DialogMemory.jLabel3.text", "MB");
//        dm.putPair("DialogMemory.jPanel3.border.title", "JVM memory information");
//        dm.putPair("DialogMemory.jPanel2.TabConstraints.tabTitle", "Settings");
//        dm.putPair("DialogMemory.jLabel6.text", "Lossy compression ratio:");
//        dm.putPair("DialogMemory.jLabel2.text", "Currently used memory:");
//        dm.putPair("DialogMemory.jLabelFree.text", "0 MB");
//        dm.putPair("DialogMemory.jLabel1.text", "Total memory:");
//        dm.putPair("DialogMemory.jLabel5.text", "(changes apply after restart of GeckoCIRCUITS)");
//        dm.putPair("DialogMemory.jPanel4.border.title", "Scope memory handling");
//        dm.putPair("DialogMemory.jPanel1.TabConstraints.tabTitle", "Info");
//        dm.putPair("DialogRemotePort.jLabelRemote.text", "Remote acess of GeckoCIRCUITS is");
//        dm.putPair("DialogRemotePort.jPanel1.border.title", "Connection settings");
//        dm.putPair("DialogRemotePort.jPanelRemoteSettings.TabConstraints.tabTitle", "Settings");
//        dm.putPair("DialogRemotePort.jPanelRemoteInfo.TabConstraints.tabTitle", "Info");
//        dm.putPair("DialogRemotePort.jButtonTest.text", "Connection test");
//        dm.putPair("DialogRemotePort.jButtonApply.text", "Apply");
//        dm.putPair("DialogRemotePort.jRadioButtonEnabled.text", "Enabled");
//        dm.putPair("DialogRemotePort.title", "Remote Access Setup");
//        dm.putPair("DialogRemotePort.jLabelPort.text", "at port:");
//        dm.putPair("DialogRemotePort.jRadioButtonDisabled.text", "Disabled");
//        dm.putPair("DialogSheetSize.jButtonApply.text", "Apply");
//        dm.putPair("DialogSheetSize.jLabel2.text", "Height:");
//        dm.putPair("DialogSheetSize.jLabel1.text", "Width:");
//        dm.putPair("DialogUpdate.jLabel1.text", "Release number:");
//        dm.putPair("DialogUpdate.jPanel1.border.title", "Your GeckoCIRCUITS version");
//        dm.putPair("DialogUpdate.jLabel4.text", "Release number:");
//        dm.putPair("DialogUpdate.jButtonClose.text", "Close window");
//        dm.putPair("DialogUpdate.jButtonGetUpdate.text", "Download update from server (.zip-file)   ");
//        dm.putPair("DialogUpdate.jLabel5.text", "Release date:");
//        dm.putPair("DialogUpdate.jButtonGetInfos.text", "Get update information");
//        dm.putPair("DialogUpdate.jPanel2.border.title", "Available update");
//        dm.putPair("DialogUpdate.jLabel3.text", "New features / changes:");
//        dm.putPair("GeckoFileManagerWindow.jButtonAddSelectedExistingFiles.text", "Add Selected File(s) to local block");
//        dm.putPair("GeckoFileManagerWindow.jPanel2.TabConstraints.tabTitle", "Files already available in model (used by other elements):");
//        dm.putPair("GeckoFileManagerWindow.jLabelFileType2.text", "Java Source File");
//        dm.putPair("GeckoFileManagerWindow.jButtonAddNewFile.text", "Add New File");
//        dm.putPair("GeckoFileManagerWindow.jPanel4.border.title", "File Status");
//        dm.putPair("GeckoFileManagerWindow.jLabelAlreadyAvailable.text", "File type:");
//        dm.putPair("GeckoFileManagerWindow.jLabelFileType.text", "*.java");
//        dm.putPair("GeckoFileManagerWindow.jPanel3.TabConstraints.tabTitle", "Info");
//        dm.putPair("GeckoFileManagerWindow.jRadioButtonIsExternalNew.text", "External");
//        dm.putPair("GeckoFileManagerWindow.jRadioButtonIsInternalNew.text", "Internal");
//        dm.putPair("GeckoFileManagerWindow.jRadioButtonIsInternalExisting.text", "Internal");
//        dm.putPair("GeckoFileManagerWindow.jRadioButtonIsExternalExisting.text", "External");
//        dm.putPair("GeckoFileManagerWindow.jPanel5.border.title", "Modify file type");
//        dm.putPair("GeckoFileManagerWindow.jPanel1.TabConstraints.tabTitle", "Files in this element:");
//        dm.putPair("GeckoFileManagerWindow.jButtonRemove.text", "Remove Selection");
//        dm.putPair("GeckoFileManagerWindow.title", "Gecko File Manager - add external files to your model");
//        dm.putPair("MemoryWarning.jButtonAbort.text", "Abort simulation");
//        dm.putPair("MemoryWarning.jButtonContinue.text", "Try to continue");
//        dm.putPair("SaveViewFrame.jRadioButtonColor.text", "Color");
//        dm.putPair("SaveViewFrame.jPanel1.border.title", "Information");
//        dm.putPair("SaveViewFrame.jRadioButtonGrayScale.text", "Grayscale");
//        dm.putPair("SaveViewFrame.jPanel2.border.title", "Settings");
//        dm.putPair("SaveViewFrame.jRadioButtonPNG.text", "png");
//        dm.putPair("SaveViewFrame.jRadioButtonPDF.text", "pdf");
//        dm.putPair("SaveViewFrame.jRadioButtonSVGZ.text", "svgz");
//        dm.putPair("SaveViewFrame.jButtonCreateImage.text", "Create Image");
//        dm.putPair("SaveViewFrame.jRadioButtonGIF.text", "gif");
//        dm.putPair("SaveViewFrame.jLabelScaling.text", "Scaling:");
//        dm.putPair("SaveViewFrame.jRadioButtonJPG.text", "jpg");
//        dm.putPair("SaveViewFrame.title", "Export Image");
//        dm.putPair("SaveViewFrame.jRadioButtonSVG.text", "svg");
//        dm.putPair("SaveViewFrame.jLabel3.text", "Filename:");
//        dm.putPair("SaveViewFrame.jPanelFileType.border.title", "File format");
//        dm.putPair("numericalinstability", " Numerical Instability");
//        dm.putPair("OK", "OK");
//        dm.putPair("optimizatiocodeprocessor", " Optimization Code Processor");
//        dm.putPair("compilecode", "Compile Code");
//        dm.putPair("runoptimization", "Run Optimization");
//        dm.putPair("closewindow", "Close Window");
//        dm.putPair("compilermessages", "Compiler Messages");
//        dm.putPair("interfacinggeckocircuits", "Interfacing GeckoCIRCUITS: ");
//        dm.putPair("licenceinfo", " Licence Information");
//        dm.putPair("singularmatrix", " Matrix is Singular");
//        dm.putPair("loadcreatewithoutsaving", " Load/Create New Without Saving?");
//        dm.putPair("Yes", "Yes");
//        dm.putPair("No", "No");
//        dm.putPair("Cancel", "Cancel");
//        dm.putPair("Parameter", "Parameter");
//        dm.putPair("Warnings", "Warnings");
//        dm.putPair("Errors", "Errors");
//        dm.putPair("ParameterSet", "Parameter Set");
//        dm.putPair("Parameters", "Parameters");
//        dm.putPair("CheckParameter", "Check Parameter");
//        dm.putPair("SimulationParameters", " Simulation Parameters");
//        dm.putPair("Transient", "Transient");
//        dm.putPair("steadystate", "Steady-State");
//        dm.putPair("Solver", "Solver");
//        dm.putPair("Integrationalgorithmselection", "Integration algorithm selection");
//        dm.putPair("gearshichmantext", "Gear-Shichman: Behavior is in between Trapezoidal and Backward Euler methods");
//        dm.putPair("Info", "Info");
//        dm.putPair("SaveApplet", "SaveApplet");
//        dm.putPair("Untitled", "Untitled");
//        dm.putPair("Readydots", "Ready ...");
//        dm.putPair("appletmodegeckocircuits", "*** Applet-Mode *** GeckoCIRCUITS ***");
//        dm.putPair("novalidgeckolicensefound", "No valid GeckoCIRCUITS license was found!");
//        dm.putPair("Geckolicenseexpired", "GeckoCIRCUITS-license expired!");
//        dm.putPair("nonasciicharsnotresolved", "Probably non-ASCII-Characters are not resolved properly. ");
//        dm.putPair("programexitingnow", "Program is exiting now.");
//        dm.putPair("Error", "Error");
//        dm.putPair("GlobalParametersChanged", "Global parameters changed");
//        dm.putPair("SimulationAborted", "Simulation aborted.");
//        dm.putPair("StartingSimulationDots", "Starting Simulation ... ");
//        dm.putPair("nogeckocircuitsavailable", "No GeckoCIRCUITS license available!");
//        dm.putPair("ensuredongleisattached", "Please ensure that your dongle is attached ");
//        dm.putPair("nolicenseavailable", "No license available.");
//        dm.putPair("donglenotpresent", "Dongle not present, or dongle changed");
//        dm.putPair("couldnotallocatememorysim", "Could not allocate enough java RAM memory for the simulation!");
//        dm.putPair("MemoryError", "Memory error !");
//        dm.putPair("SevereError", "Severe error!");
//        dm.putPair("novalidlicensefound", "no valid GeckoCIRCUITS license found!");
//        dm.putPair("AppletModeSelectEx", " Applet-Mode: Select Example");
//        dm.putPair("ControlOrder", "Control Order");
//        dm.putPair("Example2", "Example 2");
//        dm.putPair("Example1", "Example 1");
//        dm.putPair("Code", "Code");
//        dm.putPair("t4", "This Model file was created with a newer version of GeckoCIRCUITS.");
//        dm.putPair("t5", "Please consider to update your Software to the newest version.");
//        dm.putPair("t6", "You can find update information in the menu Help -> Updates.");
//        dm.putPair("licensefile", "to your computer, or check you license file.");
//        dm.putPair("SelectSimModelFromList", "Select a simulation model from the list. ");
//        dm.putPair("DialogFeedback.jLabel5.text", "<html>We want to improve our software! If you would like to submit a bug-report, <br>give us suggestions about features that we should implement in the software, <br>or you have other issues, then please visit our bugtracking homepage:");
//        dm.putPair("DialogFeedback.jLabelURL.text", "<html><a href\"www.bugs.gecko-research.org\">www.bugs.gecko-research.org</a></html>");
//        dm.putPair("DialogLicensing.jLabel2.text", "Licensing / Terms and Conditions for the Software usage and IT Services of the Gecko-Simulations GmbH, CH-8006 Zurich, Switzerland");
//        dm.putPair("DialogLicensing.jLabel1.text", "GeckoCIRCUITS uses the \"Batik SVG Vector-Graphics libraries\" and \"JSyntaxPane\", which are both licensed under the Apache License (Version 2.0):");
//        dm.putPair("t10", "You attempted to close GeckoCIRCUITS while running in Simulink-mode.");
//        dm.putPair("t11", "Please note that only closing your MATLAB session will close GeckoCIRCUITS.");
//        dm.putPair("t15", "before exiting GeckoCIRCUITS?");
//        dm.putPair("t16", "Warning: Exit GeckoCIRCUITS without saving!");
//        dm.putPair("jarfilenotexist", "Jar-File does not exist:");
//        dm.putPair("couldnotwritetooutput", "Could not write to output file: ");
//        dm.putPair("errormessage", " Error-message: ");
//        dm.putPair("couldnotfindpath", "Could not find path to GeckoCIRCUITS installation: ");
//        dm.putPair("t7", "T_pre, dt_pre: The simulation is executed with the simulation time of T_pre and the stepwidth");
//        dm.putPair("t8", " of dt_pre in advance of the regular simulation. This enables an easy finding of the");
//        dm.putPair("t9", " steady state solution.");
//        dm.putPair("warningnodenumber", " Warning: Node Number");
//        dm.putPair("redundantinput", "*** WARNING: Redundant Input ***");
//        dm.putPair("irrelevantinput", "*** WARNING: Irrelevant Input ***");
//        dm.putPair("proceedwithsimulation", "You can proceed with the simulation with the current parameter set.");
//        dm.putPair("parameterdefinitionmissing", "*** ERROR: Parameter Definition Missing ***");
//        dm.putPair("noerrors", "No Errors.");
//        dm.putPair("simulationcannotbestarted", "The simulation cannot be started. Please set the missing parameters first.");
//        dm.putPair("interfacemethods", "Use the following Interface-methods to control GeckoCIRCUITS:");
//        dm.putPair("backwardeulertext", "Backward-Euler: Very stable, with some numeric damping");
//        dm.putPair("trapezoidaltext", "Trapezoidal: No numeric damping, possibly unstable");

        return dm;
    }

    /**
     * Creates a new map with all multiple-line English key-value pairs and
     * returns it
     *
     * @return DoubleMap containing all multiple-line English key-value pairs
     */
    public static DoubleMap initEnglishMap_multiple() {
        DoubleMap dm = new DoubleMap();

        // initialize multiple-line pairs        
//        dm.putPair("DialogRemotePort.jTextAreaRemoteInfo.text", "Here you can allow the control of a \nGeckoCIRCUITS simulation by another\napplication (e.g. MATLAB) via a network \nport. For details see the GeckoSCRIPT\ntutorial. Currently only connections from\nthe local machine are allowed; network\naccess will be implemented in a future\nversion.");
//        dm.putPair("GeckoFileManagerWindow.jTextArea1.text", "\nFiles specified as \"internal\" are read in by Gecko fully the first time they are added\nto a block and then saved into the Gecko model file (*.ipes). \n\"External\" files are accessed every time a block needs them, and must be distributed \nalong with the model file in order for the simulation to work properly with them.\nYou can use the buttons on the right-hand side to change an existing file's status.");
//        dm.putPair("CautionOverwriteAutoBackupFile", "Caution: you try to overwrite your auto-backup file. \nYou should rename your filename to prevent data-loss!\n Do you want to continue anyway?");
//        dm.putPair("filehaschangedwarning", "\n*** WARNING ***\n\nThe content of the file has changed.\nDo you want to save the changes?\n");
//        dm.putPair("modelerrortext", "\n*** MODEL ERROR ***\n\nThe simulation cannot be performed.\n\nPlease check the circuit model for\n\n- Subcircuits without ground connection\n- Parallel voltage / temperature sources\n- Parallel capacitors with initial conditions set\n- Missing connections between elements\n\n");
//        dm.putPair("MemoryWarning.jTextArea1.text", "Excessive memory usage detected! Your simulation\nrequires more RAM memory than actually available.\nYou can try the following steps before your simulation\nrun:\n- Increase RAM memory of GeckoCIRCUITS at program\n   startup (menu Tools -> Memory)\n- Decrease the number of scope input signals\n- Increase the simulation stepwidth\n- Decrease the total simulation time\n\nWith the current settings, your simulation will first\nslow down due to a reduced memory cache size.\nThen, it will abort with an out-of-memory exception.");
//        dm.putPair("warningproceedingsimulation", "\n*** WARNING ***\n\nThe node number of the model has been changed.Proceeding with the simulation might give incorrect results.\n\n");
//        dm.putPair("help1", "\nParameters (name-value pairs) must match the parameters defined in the simulation model, \ne.g. $Rload. \n\nPairs of name-value in the input list must be separated by space or tab. \n\nIf a name is defined twice, a warning is given, but the simulation can be performed. If names are defined in the input list, that are not used in the simulation model, a warning is given. \n\nIf parameter names in the simulation model are not defined in the input list, an error is given. In this case the simulation cannot be performed. \n");
//        dm.putPair("numericalstabilitytext", "Error:\nNumerical instability of switch.\nSimulation aborted at ");
//        dm.putPair("DialogMemory.jTextArea1.text", "The \"Scope\" element needs to store the simulation data for output/display.\nSmall simulation step widths and long simulation times require a lot of RAM \nmemory. Therefore, you can modify the data storage strategies: \n\n- No lossy compression: The simulation data is stored with its original\n  values in single-precision (float) resolution. This requires probably more\n  RAM memory that available.\n\n- Lossy compression (low, medium, high): The number of significant digits\n  of the simulation data is reduced. The amount of required memory is\n  therefore, also reduced, significantly.\n  Disadvantage: The simulation data is rounded according to the setting:\n  Low: epsilon  1e-5\n  Medium: epsilon  5e-5\n  High: epsilon  2e-4\n  Please note that the timestep-resolution as well as the simulation itself\n  are not affected by the lossy compression. With the compression setting, \n  you can trad-off between  memory requirement and data compression \n  ratio.");
//        dm.putPair("t13", "The model has changed since last save to the model\nfile. Would you like to save your model to the file:\n");


        return dm;
    }
}