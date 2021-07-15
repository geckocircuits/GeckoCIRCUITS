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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.Operationable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

// TODO: this is a mess, please clean anybody up!

public abstract class AbstractNonLinearCircuitComponent extends AbstractTwoPortLKreisBlock 
implements Operationable, Nonlinearable {       
    
    public final UserParameter<Boolean> _isNonlinear = UserParameter.Builder.
            <Boolean>start("isNonlinear", false).                       
            longName(I18nKeys.IF_TRUE_USE_NONLINEAR_CHARACTERISTIC).
            shortName("isNonlinear").            
            arrayIndex(this, -1).
            build();                               

    // if we would use the UserParameter _isNonlinear, many Boolean object would 
    // be autoboxed every simulation-step!
    public boolean _isNonlinearForCalculationUsage = false;

    
    // nonlinX --> {u1, u2, u3 ... un} ... u-Points on the nonlinear characteristic C=C(u)
    // nonlinY --> {C1, C2, C3 ... Cn} ... C-Points on the nonlinear characteristic C=C(u)
    public double[][] nonlinearData = new double[0][0];
    public GeckoFile nonLinearChar = null;
    /**
     * this field is only used for init: at the object creation, we just know the hash value of the object, the link
     * will then be set later.
     */
    public long nonLinearCharHashValueForInit = 0;
    public boolean initNonLinFromFile = false;
    public long nonLinearLastModified = -2;
    public static final double[] NONLIN_CAP_X_DEFAULT = new double[]{0, 100, 300, 400};
    public static final double[] NONLIN_CAP_Y_DEFAULT = new double[]{1e-7, 0.8e-7, 1.2e-9, 1e-9};    
    
    public static final double[] NONLIN_IND_X_DEFAULT = new double[]{0, 10, 15, 30};
    public static final double[] NONLIN_IND_Y_DEFAULT = new double[]{500e-6, 500e-6, 150e-6, 100e-6};
    
    public static final double[] NONLIN_REL_X_DEFAULT = new double[]{0, 100, 300, 400};
    public static final double[] NONLIN_REL_Y_DEFAULT = new double[]{2, 2, 4, 5};    
    
    
    
    public AbstractNonLinearCircuitComponent() {
        super();
        nonlinearData = getInitalNonlinValues();  
        _isNonlinear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _isNonlinearForCalculationUsage = _isNonlinear.getValue();
            }
        });
    }               
    
    public void setNonLinearFile(File file) throws IllegalAccessException {        
        if (!(file.getName().endsWith(getNonlinearFileEnding()))) {
            throw new IllegalAccessException("Non-linear characteristic file must end with extension " + getNonlinearFileEnding());
        } else {            
            try {                
                GeckoFile newFile = new GeckoFile(file, GeckoFile.StorageType.EXTERNAL, Fenster.getOpenFileName());                
                ArrayList<GeckoFile> newFileList = new ArrayList<GeckoFile>();                
                newFileList.add(newFile);                
                addFiles(newFileList);
                _isNonlinear.setValueWithoutUndo(true);
            } catch (Exception e) {
                e.printStackTrace();
                final String errorMessage = "Error writing non-linear characteristic file in " + getStringID() + "\n" + e.getMessage();
                final String errorTitle = getStringID() + ": ERROR";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    @Deprecated // this is really ugly! Use an interface instead for external files!
    public void removeLocalComponentFiles(final List<GeckoFile> filesToRemove) {
        //in this case there should only be one file to remove
        if (filesToRemove.size() > 0 && nonLinearChar != null) {
            nonLinearChar.removeUser(getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(nonLinearChar);
            nonLinearChar = null;
        }
    }    
    
    @Override
    public void addFiles(final List<GeckoFile> newFiles) {
        if (newFiles.size() > 0) {
            //should be only one file, therefore get first one on list
            GeckoFile newNonLin = newFiles.get(0);
            try {
                double[][] nonLin = readNonLinearCharacteristicFromFile(newNonLin);
                if (nonLinearChar != null) {
                    nonLinearChar.removeUser(getUniqueObjectIdentifier());
                    Fenster._fileManager.maintain(nonLinearChar);
                }
                nonLinearChar = newNonLin;
                nonlinearData = nonLin;
                nonLinearChar.setUser(getUniqueObjectIdentifier());
                Fenster._fileManager.addFile(nonLinearChar);
                nonLinearLastModified = nonLinearChar.checkModificationTimeStamp();
            } catch (NumberFormatException e) {
                final String errorMessage = "Non-linear characteristic file format error: \n" + e.getMessage();
                final String errorTitle = getStringID() + ": ERROR - Number format exception";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            } catch (java.io.IOException e) {
                final String errorMessage = "Error reading non-linear characteristic file in " + getStringID() + "\n" + e.getMessage();
                final String errorTitle = getStringID() + ": ERROR - I/O exception";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public AbstractCircuitSheetComponent copyFabric(long shiftValue) {
        AbstractNonLinearCircuitComponent copy = (AbstractNonLinearCircuitComponent) super.copyFabric(shiftValue);        
        
        if(nonLinearChar != null) {
            copy.nonLinearCharHashValueForInit = nonLinearChar.getHashValue();        
        } else {
            copy.nonLinearCharHashValueForInit = 0;        
        }
        
        copy.initNonLinFromFile = true;
        copy.initExternalFile();
                
        copy.nonlinearData = new double[2][nonlinearData[0].length];
        
        System.arraycopy(nonlinearData[0], 0, copy.nonlinearData[0], 0, nonlinearData[0].length);
        System.arraycopy(nonlinearData[1], 0, copy.nonlinearData[1], 0, nonlinearData[1].length);
        
        return copy;
    }

    @Override
    public List<GeckoFile> getFiles() {
        ArrayList<GeckoFile> nonLinearFile = new ArrayList<GeckoFile>();
        nonLinearFile.add(nonLinearChar);
        return nonLinearFile;
    }

    @Override
    protected void addTextInfoParameters() {
        if (_isNonlinear.getValue() && nonLinearChar == null) {
            _textInfo.addErrorValue("Nonlinear file not found!");
        }

        if (SchematischeEingabe2._lkDisplayMode.showParameter) {

            double displayValue = 0;
            if (this instanceof AbstractCapacitor) {
                displayValue = ((AbstractCapacitor) this)._capacitance.getValue();
            }

            if (this instanceof AbstractInductor) {
                displayValue = ((AbstractInductor) this)._inductance.getValue();
            }
                        
            String uTxt = getFixedIDString() + "=" + (_isNonlinear.getValue() ? "nonlin" : tcf.formatENG(displayValue, 3));
            if (getNonlinearReplacedParameter() != null && getNonlinearReplacedParameter().getNameOpt().isEmpty()) {
                _textInfo.addParameter(uTxt);
            } else {
                
            }
        }
    }

    @Override
    public void initExtraFiles() {
        initExternalFile();
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {

        //void (String elementName, String characteristicFileName) throws RemoteException, FileNotFoundException;

        final List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.add(new OperationInterface("setNonLinear", I18nKeys.SET_NONLINEAR_OPERATION_DOC) {
            @Override
            public Object doOperation(final Object parameterValue) {
                if(!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Argument must be a file path as String type.");
                }                
                String characteristicFileName = (String) parameterValue;                
                File nonLinFile = new File(characteristicFileName);                
                //if it doesn't exist, try first to see if it is in the same directory as the currently open model file
                if (!nonLinFile.exists()) {
                    final File modelFile = new File(ch.technokrat.gecko.geckocircuits.allg.Fenster.getOpenFileName());
                    final String currentModelDirectory = modelFile.getParent();
                    final String nonLinFileName = currentModelDirectory + System.getProperty("file.separator") + characteristicFileName;
                    nonLinFile = new File(nonLinFileName);                    
                }
                if (nonLinFile.exists() && !nonLinFile.isDirectory()) {
                    try {
                        AbstractNonLinearCircuitComponent.this.setNonLinearFile(nonLinFile);
                    } catch (IllegalAccessException ex) {
                        System.out.println("exception " + ex);
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                } else {                    
                    throw new RuntimeException("Specified non-linear characteristic file: " + characteristicFileName + " does not exist or is a directory.");
                }                
                return null;
            }
        });

        return Collections.unmodifiableList(returnValue);
    }                
    

    public double[][] getNonlinearCharacteristic() {
        if ((nonLinearChar != null) && (nonLinearLastModified != nonLinearChar.checkModificationTimeStamp())) {
            updateNonLinearCharacteristic();
        }
        return nonlinearData;
    }

    public void updateNonLinearCharacteristic() {
        try {
            nonlinearData = readNonLinearCharacteristicFromFile(nonLinearChar);             
            nonLinearLastModified = nonLinearChar.checkModificationTimeStamp();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            final String errorMessage = "Non-linear characteristic file format error: \n" + e.getMessage();
            final String errorTitle = getStringID() + ": ERROR - Number format exception";
            JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException e) {
            final String errorMessage = "Error reading non-linear characteristic file in " + getStringID() + "\n" + e.getMessage();
            final String errorTitle = getStringID() + ": ERROR - I/O exception";
            JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    //reads a non linear characteristic from a file, returns an array which contains two array - non-linear x, non-linear y
    public static double[][] readNonLinearCharacteristicFromFile(GeckoFile file) throws NumberFormatException, java.io.IOException {
        double[] nonLinX, nonLinY;
        ArrayList<String> lines = new ArrayList<String>();
        BufferedReader in = file.getBufferedReader();
        String inLine;
        while ((inLine = in.readLine()) != null) {
            lines.add(inLine);
        }
        in.close();
        int noOfLines = lines.size();
        if (noOfLines < 2) {
            throw new NumberFormatException("File contains less than 2 data points!");
        }
        nonLinX = new double[noOfLines];
        nonLinY = new double[noOfLines];
        StringTokenizer tokens;
        String currentLine;
        for (int i = 0; i < noOfLines; i++) {
            currentLine = lines.get(i);
            tokens = new StringTokenizer(currentLine, " ");
            if (tokens.countTokens() != 2) {
                throw new NumberFormatException("Impromer data point in file: " + currentLine);
            }            
            nonLinX[i] = (new Double(tokens.nextToken())).doubleValue();
            nonLinY[i] = (new Double(tokens.nextToken())).doubleValue();
        }

        return new double[][]{nonLinX, nonLinY};
    }
    

    

    public void initialize() {                
        if (_isNonlinear.getValue() && (nonLinearChar != null)) {
            if (nonLinearLastModified != nonLinearChar.checkModificationTimeStamp()) {
                updateNonLinearCharacteristic();
            }
        }
    }

    public void setNonlinearCharacteristic(double[][] data) {        
        if (nonLinearChar == null) {
            String newFileName = getStringID() + "NonLinearity" + ((int) 100 * Math.random()) + 
                    getNonlinearFileEnding();
            setNonlinearCharacteristic(data, newFileName);
        } else {
            nonlinearData = data;
            try {
                String fileName;
                if (nonLinearChar.getStorageType() == GeckoFile.StorageType.EXTERNAL) {
                    fileName = nonLinearChar.getCurrentAbsolutePath();
                } else {
                    fileName = nonLinearChar.getName();
                }
                File newData = writeNonLinearCharacteristicToFile(data, new File(fileName));
                nonLinearChar.update(newData);
                nonLinearLastModified = nonLinearChar.checkModificationTimeStamp();
            } catch (java.io.IOException e) {
                final String errorMessage = "Error writing non-linear characteristic file in " + getStringID() + "\n" + e.getMessage();
                final String errorTitle = getStringID() + ": ERROR - I/O exception";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setNonlinearCharacteristic(double[][] data, String newInternalFileName) {
        nonlinearData = data;
        if (nonLinearChar != null) {
            nonLinearChar.removeUser(getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(nonLinearChar);
        }
        try {
            File newFile = writeNonLinearCharacteristicToFile(nonlinearData, new File(newInternalFileName));
            nonLinearChar = new GeckoFile(newFile, GeckoFile.StorageType.INTERNAL, Fenster.getOpenFileName());
            nonLinearChar.setUser(getUniqueObjectIdentifier());
            Fenster._fileManager.addFile(nonLinearChar);
            nonLinearLastModified = nonLinearChar.checkModificationTimeStamp();
        } catch (Exception e) {
            final String errorMessage = "Error writing non-linear characteristic file in " + getStringID() + "\n" + e.getMessage();
            final String errorTitle = getStringID() + ": ERROR";
            JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    public GeckoFile getNonLinearFile() {
        return nonLinearChar;
    }
    
    //===================================
    // ASSUMPTION: x- and y-axis both linear
    // also employed with LKOP2
    // nlX,nlY define the picewise non-linear characteristic, x is the actual value 
    public double getActualValueLINFromLinearizedCharacteristic(double x) {        
        int i1 = 0;
        int pkt = nonlinearData[0].length;
        while ((i1 < pkt) && (nonlinearData[0][i1] < x)) {
            i1++;
        }
        if (i1 == 0) {
            return nonlinearData[1][i1];  // left boarder
        }
        if (i1 >= pkt) {
            return nonlinearData[1][pkt - 1];  // right boarder
        }
        double x1 = nonlinearData[0][i1 - 1];
        double x2 = nonlinearData[0][i1];
        double interval = x2 - x1;
        return (nonlinearData[1][i1 - 1] * (x2 - x) + nonlinearData[1][i1] * (x - x1)) / interval;
    }
    
    
    //===================================
    // ASSUMPTION: x- and y-axis both linear
    // also employed with LKOP2
    // nlX,nlY define the picewise non-linear characteristic, x is the actual value 
    public double getActualValueLINFromLinearizedCharacteristicInverse(double x) {        
        int i1 = 0;
        boolean debug = false;
        if( x > -23.513443326778713) {
            debug = true;
        }
        int pkt = nonlinearData[0].length;
        while ((i1 < pkt) && (nonlinearData[0][i1] < x)) {
            i1++;
        }        
        
        
//        if(debug) {
//            System.out.println("compare end value " + nonlinearData[0][pkt-1] + " " + x);
//        }
        
        if (i1 == 0) {            
            return 1.0 / nonlinearData[1][i1];  // left boarder
        }
        if (i1 >= pkt) {            
            return 1.0 / nonlinearData[1][pkt - 1];  // right boarder
        }
        double x1 = nonlinearData[0][i1 - 1];
        double x2 = nonlinearData[0][i1];
        double interval = x2 - x1;        
        return (1.0 / nonlinearData[1][i1 - 1] * (x2 - x) + 1.0 / nonlinearData[1][i1] * (x - x1)) / interval;
    }
    

    //===================================
    // ASSUMPTION: x-axis is linear, but y-axis is logarithmic
    // Coss(u) in S, D, THYR
    // nlX,nlY define the picewise non-linear characteristic, x is the actual value (e.g. x=u in case of C with y=C(u)) -->
    public double getActualValueLOGFromLinearizedCharacteristic(double x) {
        int i1 = 0;
        int pkt = nonlinearData[0].length;
        while ((i1 < pkt) && (nonlinearData[0][i1] < x)) {
            i1++;
        }
        if (i1 == 0) {
            return nonlinearData[1][0];
        }
        if (i1 >= pkt) {
            return nonlinearData[1][pkt - 1];
        }
        double x1 = nonlinearData[0][i1 - 1];
        double x2 = nonlinearData[0][i1];
        double lg10_y1 = Math.log10(nonlinearData[1][i1 - 1]);
        double lg10_y2 = Math.log10(nonlinearData[1][i1]);
        double lg10_y = lg10_y1 + (lg10_y2 - lg10_y1) * (x - x1) / (x2 - x1);
        return Math.pow(10, lg10_y);
    }

    @Override
    public void exportAsciiIndividual(final StringBuffer ascii) {        
        DatenSpeicher.appendAsString(ascii.append("\nnonlinX"), nonlinearData[0]);
        DatenSpeicher.appendAsString(ascii.append("\nnonlinY"), nonlinearData[1]);
        if (nonLinearChar != null) {
            DatenSpeicher.appendAsString(ascii.append("\nnonLinearCharHashValue"), nonLinearChar.getHashValue());
        } else {
            DatenSpeicher.appendAsString(ascii.append("\nnonLinearCharHashValue"), 0);
        }
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        if (tokenMap.containsToken("isNonlinear")) {            
            double[] nonlinX = tokenMap.readDataLine("nonlinX[]", new double[0]);
            double[] nonlinY = tokenMap.readDataLine("nonlinY[]", new double[0]);
            nonlinearData = new double[][] {nonlinX, nonlinY};
        }

        if (nonlinearData.length != 2 || (nonlinearData[0].length < 2) || (nonlinearData[1].length < 2)) {
            nonlinearData = getInitalNonlinValues();            
        }
        
        if (tokenMap.containsToken("nonLinearCharHashValue")) {
            nonLinearCharHashValueForInit = tokenMap.readDataLine("nonLinearCharHashValue", nonLinearCharHashValueForInit);
            initNonLinFromFile = true;
        }
    }
    
    public void initExternalFile() {        
        if (_isNonlinear.getValue()) {
            if (initNonLinFromFile) {
                try {
                    nonLinearChar = Fenster._fileManager.getFile(nonLinearCharHashValueForInit);
                    updateNonLinearCharacteristic();
                } catch (FileNotFoundException e) {
                    createNewInitialInternalFile();                    
                }
            } else {
                createNewInitialInternalFile();
            }
        }
    }
    
    //writes a non-linear characteristic to file
    public static File writeNonLinearCharacteristicToFile(double[][] data, File nonLinFile) throws java.io.IOException {
        
        BufferedWriter out = new BufferedWriter(new java.io.FileWriter(nonLinFile));
        for (int i = 0; i < data[0].length; i++) {
            out.write(data[0][i] + " " + data[1][i] + "\n");
        }
        out.flush();
        out.close();
        return nonLinFile;
    }
    
    
    
    /**
     * this is for backwards-compatibility from older versions. Since the 
     * file object was not yet created, we create an internal file that
     * contains the nonlinear characterisitc.
     */    
    private void createNewInitialInternalFile() {
        Random rand = new Random();
        byte randByte = (byte) rand.nextInt();
        File tmpFile = new File(getStringID() + "_" + randByte + getNonlinearFileEnding());
        try {
            byte[] fileContents = writeNonLinearCharacteristicToBytes();
            GeckoFile newFile = new GeckoFile(tmpFile, Fenster.getOpenFileName(), fileContents);
            ArrayList<GeckoFile> newFiles = new ArrayList<GeckoFile>();
            newFiles.add(newFile);
            addFiles(newFiles);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractNonLinearCircuitComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public byte[] writeNonLinearCharacteristicToBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(baos));
            for (int i = 0; i < nonlinearData[0].length; i++) {
                out.write(nonlinearData[0][i] + " " + nonlinearData[1][i] + "\n");
            }
            out.flush();
            byte[] returnValue = baos.toByteArray();
            out.close();
            return returnValue;
        } catch (IOException ex) {
            Logger.getLogger(AbstractNonLinearCircuitComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new byte[0];
    }

    
    
    
}
