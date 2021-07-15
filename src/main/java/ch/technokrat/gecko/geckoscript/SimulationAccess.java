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
package ch.technokrat.gecko.geckoscript;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.*;
import ch.technokrat.gecko.geckocircuits.control.DataSaver;
import ch.technokrat.gecko.geckocircuits.control.javablock.ExtraFilesWindow;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class SimulationAccess implements GeckoFileable {

    final static long DUMMY_BLOCK_ID = -1231231987;
    final List<GeckoFile> _additionalSourceFiles = new ArrayList<GeckoFile>();
    private boolean _populateFileList = false;
    private final Set<String> _additionalFilesHashKeys = new TreeSet();

    private ScriptWindow scriptwindow;
    public SchematischeEingabe2 se;
    private Fenster mainWindow;

    public SimulationAccess(final Fenster fenster) {
        se = SchematischeEingabe2.Singleton;
        mainWindow = fenster;
        assert mainWindow != null;
        try {
            scriptwindow = new ScriptWindow(this);

        } catch (Throwable ex) {
            System.out.println("Could not find editor library jsyntaxpane.jar. Scripting tool disabled.");
            // ex.printStackTrace();
        }

    }

    public boolean isScripterEnabled() {
        return scriptwindow != null;
    }

    public void startSim() {
        try {
            mainWindow._simRunner.startCalculation(false, mainWindow._solverSettings);
            waitForDataSavers();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void continueSim() {
        try {
            mainWindow.continueCalculation(false);
            waitForDataSavers();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private void waitForDataSavers() {
        int counter = 0;
        while (DataSaver.WAIT_COUNTER.get() != 0 && counter < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimulationAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
            counter++;
        }
    }

    public void initializeSimulation() {
        try {
            mainWindow._simRunner.initSim();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initializeSimulation(double dt, double endTime) {
        try {
            mainWindow._simRunner.initSim(dt, endTime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void simulateOneStep() throws Exception {
        mainWindow._simRunner.simKern.simulateOneStep();
    }

    public void simulateSpecifiedTime(double time) throws Exception {
        mainWindow._simRunner.simKern.simulateTime(time);
    }

    public void endSimulation() {
        mainWindow.endSim();
    }

    public void makeVisible() {
        if (scriptwindow != null) {
            scriptwindow.setVisible(true);
        }
    }

    public <T extends AbstractCircuitBlockInterface> List<T> getComponentsOfType(Class<T> searchClass) {
        List<T> returnValue = new ArrayList<T>();
        for (AbstractCircuitBlockInterface elem : se.getElementLK()) {
            AbstractCircuitBlockInterface block = elem;
            if (searchClass.isAssignableFrom(block.getClass())) {
                returnValue.add((T) block);
            }
        }
        return returnValue;
    }

    public List<List<AbstractBlockInterface>> getElementsSorted() {
        List<List<AbstractBlockInterface>> returnValue = doListSort(se.getElementLK());
        returnValue.addAll(doListSort(se.getElementCONTROL()));
        returnValue.addAll(doListSort(se.getElementTHERM()));
        returnValue.addAll(doListSort(se.getElementSpecial()));
        return returnValue;
    }

    private static List<List<AbstractBlockInterface>> doListSort(final List<? extends AbstractBlockInterface> unsorted) {
        List<List<AbstractBlockInterface>> sortedListofLists = new ArrayList<List<AbstractBlockInterface>>();
        List<Class> types = new ArrayList<Class>();
        HashMap<Class, List<AbstractBlockInterface>> listsByType = new HashMap<Class, List<AbstractBlockInterface>>();
        List<AbstractBlockInterface> currentList;

        for (AbstractBlockInterface elem : unsorted) {
            Class elementClass = elem.getClass();
            currentList = listsByType.get(elementClass);
            if (currentList == null) {
                currentList = new ArrayList<AbstractBlockInterface>();
                currentList.add(elem);
                listsByType.put(elementClass, currentList);
                types.add(elementClass);
            } else {
                currentList.add(elem);
            }
        }

        for (Class elemType : types) {
            sortedListofLists.add(listsByType.get(elemType));
        }

        return sortedListofLists;
    }

    public void setScriptCode(String scripterCode) {
        if (scriptwindow != null) {
            scriptwindow.setScripterCode(scripterCode);
        }
    }

    public String getDeclarationCode() {
        if (scriptwindow != null) {
            return scriptwindow.getDeclarationCode();
        } else {
            return "";
        }

    }

    public String getImportCode() {
        if (scriptwindow != null) {
            return scriptwindow.getImportCode();
        } else {
            return "";
        }

    }

    public void setDeclarationCode(String code) {
        if (scriptwindow != null) {
            scriptwindow.setDeclarationCode(code);
        }

    }

    public void setImportCode(String code) {
        if (scriptwindow != null) {
            scriptwindow.setImportCode(code);
        }

    }

    public String getScriptCode() {
        if (scriptwindow != null) {
            return scriptwindow.getSourceCode();
        } else {
            return "";
        }

    }

    void set_Tend(double Tend) {
        mainWindow._solverSettings._tDURATION.setValueWithoutUndo(Tend);
    }

    void set_Tend_pre(double Tend) {
        mainWindow._solverSettings._T_pre.setValueWithoutUndo(Tend);
    }

    void set_dt(double value) {
        mainWindow._solverSettings.dt.setValue(value);
    }

    void set_dt_pre(double value) {
        mainWindow._solverSettings._dt_pre.setValueWithoutUndo(value);
    }

    void saveFileAs(String fileName) {
        mainWindow.rawSaveFile(new File(fileName));
    }

    void openFile(String fileName) throws FileNotFoundException {
        mainWindow.openFile(fileName);
    }

    public final void importFromFile(final String fileName, final String importIntoSubcircuit)
            throws FileNotFoundException {
        Fenster.importComponentsFromFile(fileName, importIntoSubcircuit);
    }

    File getCurrentModelFile() {
        return new File(mainWindow.getOpenFileName());
    }

    public double[] getSignalCharacteristics(String scopename, int port, double start_time, double end_time)
            throws Exception {
        AbstractBlockInterface block = IDStringDialog.getComponentByName(scopename);

        if (!(block instanceof ReglerOSZI)) {
            throw new Exception("Supplied element " + scopename + "to getSignalCharacteristics function is not a SCOPE");
        } else {
            ReglerOSZI scope = (ReglerOSZI) block;
            return scope.getChannelCharacteristics(port, start_time, end_time);
        }

    }

    public double[][] doFourierAnalysis(String scopename, int port, double start_time, double end_time, int harmonics) throws Exception {
        AbstractBlockInterface block = IDStringDialog.getComponentByName(scopename);

        if (!(block instanceof ReglerOSZI)) {
            throw new Exception("Supplied element " + scopename + "to getSignalCharacteristics function is not a SCOPE");
        } else {
            ReglerOSZI scope = (ReglerOSZI) block;
            return scope.doFourierAnalysis(port, start_time, end_time, harmonics);
        }
    }

    public double get_dt() {
        return mainWindow._solverSettings.dt.getValue();
    }

    double get_dt_pre() {
        return mainWindow._solverSettings._dt_pre.getValue();
    }

    double get_Tend_pre() {
        return mainWindow._solverSettings._T_pre.getValue();
    }

    double get_Tend() {
        return mainWindow._solverSettings._tDURATION.getValue();
    }

    //to clear the GeckoCustom object after opening a new file
    public void clearData() {
        if (scriptwindow != null) {
            scriptwindow.clearObject();
        }

    }

    public void setElementPosition(AbstractBlockInterface element, int x, int y) throws Exception {
        boolean positionOK = isPositionValid(/*
                 * element,
                 */x, y);

        if (positionOK) {
            Point originalPoint = element.getPositionVorVerschieben();
            element.moveComponent(new Point(x - originalPoint.x, y - originalPoint.y));
            element.absetzenElement();
        }
    }
        

    private boolean isPositionValid(/*
             * ElementInterface element,
             */int x, int y) throws Exception {
        boolean valid = false;
        int worksheetSizeX = Fenster._se._circuitSheet._worksheetSize.getSizeX();
        int worksheetSizeY = Fenster._se._circuitSheet._worksheetSize.getSizeY();
        if (x >= worksheetSizeX || y >= worksheetSizeY) {
            throw new Exception("Given position is outside defined drawing area! Sheet size is " + worksheetSizeX + "x" + worksheetSizeY + " and given new position is " + x + "x" + y + ".");
        } else {
            //here ideally we should check full size of component
            //this we can to by calling the component's getAnfangsKnoten() and getEndKnoten() methods
            //BUT - to update this, we first have to set the new coordinates and then re-paint the component
            //this is too complicated for now, so just give a "buffer zone" around the component and check if position is far enough away from the sides
            //of the sheet
            int bufferSpace = 3;
            if (y < bufferSpace || x < bufferSpace || (worksheetSizeY - y) < bufferSpace || (worksheetSizeX - x) < bufferSpace) {
                valid = true;
                //throw new Exception("Given position is too close to worksheet sides. Sheet size is " + worksheetSizeX + "x" + worksheetSizeY + " and given new position is " + x + "x" + y + ".");
            } else {
                valid = true;
            }
        }
        return valid;
    }

    public void deleteElement(AbstractBlockInterface element) {
        se.deleteComponent(element);
    }

    void createNewConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) throws Exception {
        boolean startPositionOK = isPositionValid(xStart, yStart);
        boolean endPositionOK = isPositionValid(xEnd, yEnd);
        AbstractCircuitSheetComponent newElement = null;
        if (startPositionOK && endPositionOK) {
            newElement = se.externalCreateAndPlaceNewConnector(elementName, xStart, yStart, xEnd, yEnd, startHorizontal);
            if (newElement == null) {
                throw new Exception("New element not created - error!");
            }
        }
    }

    public AbstractBlockInterface createNewElement(final AbstractTypeInfo elementCategory,
            final String elementName, final int x, final int y) throws Exception {

        boolean positionOK = isPositionValid(x, y);
        AbstractBlockInterface newElement = null;
        if (positionOK) {
            newElement = se.externalCreateAndPlaceNewElement(elementName, elementCategory, x, y);
            if (newElement == null) {
                throw new Exception("New element not created - error!");
            }
        }
        return newElement;
    }

    //returns true if element is renamed successfully, false otherwise
    public void renameElement(AbstractBlockInterface element, String newName) throws NameAlreadyExistsException {

        if (newName.isEmpty()) {
            throw new IllegalArgumentException("Error: cannot insert emtyp name!");
        }
        String oldName = element.getStringID();
        element.setNewNameChecked(newName);
        se.updateComponentCouplings(oldName, newName);
    }

    //set the label for a model element
    public void setElementNodeLabel(final AbstractBlockInterface element, final AbstractGeckoCustom.StartOrStopNode labelType,
            final int nodeIndex, final String labelName) throws Exception {

        AbstractTerminal terminal = null;
        switch (labelType) {
            case START_NODE:
                terminal = element.XIN.get(nodeIndex);
                break;
            case STOP_NODE:
                terminal = element.YOUT.get(nodeIndex);
                break;
            default:
                throw new Exception("Invalid label type: neither output nor input node!");
        }

        final ConnectorType terminalType = terminal.getCategory();
        
               
        final CircuitLabel label = terminal.getLabelObject();
        final String oldLabel = label.getLabelString();        
        label.setLabelFromUserDialog(labelName);
                
        final NetzlisteAllg netzlisteAllg1 = NetzlisteAllg.fabricNetzlistComponentLabelUpdate(element, terminalType);        
                                        
        se.updateRenamedLabel(oldLabel, labelName, terminalType);                
        se.setDirtyFlag();
    }
    
    
    //set the label for a model element
    public String getElementNodeLabel(final AbstractBlockInterface element, final AbstractGeckoCustom.StartOrStopNode labelType,
            final int nodeIndex) throws Exception {

        AbstractTerminal terminal = null;
        switch (labelType) {
            case START_NODE:
                terminal = element.XIN.get(nodeIndex);
                break;
            case STOP_NODE:
                terminal = element.YOUT.get(nodeIndex);
                break;
            default:
                throw new Exception("Invalid label type: neither output nor input node!");
        }
        
                       
        final CircuitLabel label = terminal.getLabelObject();        
        return label.getLabelString();        
    }

    public double getSimulationTime() {        
        return GeckoSim._win._simRunner.simKern.getZeitAktuell();
    }

    @Override
    public void addFiles(List<GeckoFile> newFiles) {
        for (GeckoFile newFile : newFiles) {
            _additionalSourceFiles.add(newFile);
            newFile.setUser(DUMMY_BLOCK_ID);
            Fenster._fileManager.addFile(newFile);
        }
        scriptwindow._extSourceWindow.addNewFiles(newFiles);
    }

    @Override
    public List<GeckoFile> getFiles() {
        return _additionalSourceFiles;
    }

    @Override
    public void removeLocalComponentFiles(List<GeckoFile> filesToRemove) {
        for (GeckoFile removedFile : filesToRemove) {
            _additionalSourceFiles.remove(removedFile);
            removedFile.removeUser(DUMMY_BLOCK_ID);
            Fenster._fileManager.maintain(removedFile);
        }

        scriptwindow._extSourceWindow.removeFilesFromList(filesToRemove);
    }

    public String getExtraFilesHashes() {
        String returnValue = "";
        for (GeckoFile gFile : _additionalSourceFiles) {
            returnValue += gFile.getHashValue();
        }
        return returnValue;
    }

    public void setExtraFilesHashBlock(final String extraFilesHashString) {
        _additionalFilesHashKeys.clear();
        _additionalFilesHashKeys.addAll(Arrays.asList(extraFilesHashString.split("\\r?\\n")));
        _populateFileList = true;
    }

    @Override
    public void initExtraFiles() {
        if (scriptwindow != null) {
            scriptwindow._extSourceWindow.removeFilesFromList(_additionalSourceFiles);
            _additionalSourceFiles.clear();
        }

        if (_additionalFilesHashKeys.isEmpty()) {
            return;
        }

        if (_populateFileList) {
            long hashValue;
            GeckoFile file;
            boolean fileMissing = false;
            int filesMissing = 0;

            for (String hash : _additionalFilesHashKeys) {
                if (hash.trim().isEmpty()) {
                    continue;
                }
                hashValue = Long.valueOf(hash.trim());
                try {
                    file = Fenster._fileManager.getFile(hashValue);
                    _additionalSourceFiles.add(file);
                } catch (Exception e) {
                    fileMissing = true;
                    filesMissing++;
                }
            }
            if (fileMissing) {
                final String errorMessage = filesMissing + " additional source files missing in GeckoSCRIPT code";
                final String errorTitle = "ERROR - File(s) not found";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
        if (scriptwindow != null) {
            scriptwindow._extSourceWindow.addNewFiles(_additionalSourceFiles);
        }
    }

    void setWorksheetSize(int sizeX, int sizeY) {
        Fenster._se._circuitSheet._worksheetSize.setNewWorksheetSize(sizeX, sizeY);
    }

    int[] getWorksheetSize() {
        int sizeX = Fenster._se._circuitSheet._worksheetSize.getSizeX();
        int sizeY = Fenster._se._circuitSheet._worksheetSize.getSizeY();
        return new int[]{sizeX, sizeY};
    }

}
