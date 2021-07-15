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

import ch.technokrat.gecko.Category;
import ch.technokrat.gecko.Declaration;
import ch.technokrat.gecko.Documentation;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCapacitor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractResistor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Thyristor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.IGBT;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SemiconductorLossCalculatable;
import ch.technokrat.gecko.GeckoCustomRemote;
import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoRemoteInterface;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.MethodCategory;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.allg.OperatingMode;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractInductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.IdealSwitch;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.*;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.CharacteristicsCalculator;
import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.*;
import javax.swing.JTextArea;

public abstract class AbstractGeckoCustom implements GeckoRemoteInterface {

    protected SimulationAccess _circuit;
    private final JTextArea _outputWindow;
    private PrintStream _output;
    private boolean _simInited = false;
    private static float[][] _globalFloatMatrix;
    private static double[][] _globalDoubleMatrix;

    protected AbstractGeckoCustom(final SimulationAccess simaccess, final JTextArea outputFrame) {
        _circuit = simaccess;
        _outputWindow = outputFrame;
        if (outputFrame == null) {
            _output = System.out;
        } else {
            _output = new PrintStream(new TextAreaOutputStream(_outputWindow));
        }
    }

    @Override
    public final void runSimulation() {
        _circuit.startSim();
    }

    @Override
    public final void continueSimulation() {
        _circuit.continueSim();
    }

    @Override
    public final void initSimulation() {
        _circuit.initializeSimulation();
        _simInited = true;
    }

    @Override
    public final void initSimulation(final double deltaT, final double endTime) {
        _circuit.initializeSimulation(deltaT, endTime);
        _simInited = true;
    }

    @Override
    public void setWorksheetSize(int sizeX, int sizeY) {
        _circuit.setWorksheetSize(sizeX, sizeY);
    }

    @Override
    public int[] getWorksheetSize() {
        return _circuit.getWorksheetSize();
    }

    @Override
    public final void simulateStep() {
        if (!_simInited) {
            initSimulation();
            writeOutputLn("Warning: Simulation was not initialized prior to calling simulateStep(). "
                    + "Simulation has been initialized to values specified in GeckoCIRCUITS.");
        }
        try {
            _circuit.simulateOneStep();
        } catch (Exception ex) {
            writerOutputErrorLn("simulateStep(): " + ex.getMessage());
        }
    }

    @Override
    public final void simulateSteps(final int steps) {
        if (!_simInited) {
            initSimulation();
            writeOutputLn("Warning: Simulation was not initialized prior to calling simulateSteps(). "
                    + "Simulation has been initialized to values specified in GeckoCIRCUITS.");
        }

        try {
            for (int i = 0; i < steps; i++) {
                _circuit.simulateOneStep();
            }
        } catch (Exception ex) {
            writerOutputErrorLn("simulateSteps(): " + ex.getMessage());
        }
    }

    @Override
    public final void simulateTime(final double time) {
        if (!_simInited) {
            initSimulation();
            writeOutputLn("Warning: Simulation was not initialized prior to calling simulateTime(). "
                    + "Simulation has been initialized to values specified in GeckoCIRCUITS.");
        }

        try {
            _circuit.simulateSpecifiedTime(time);
        } catch (Exception ex) {
            writerOutputErrorLn("simulateTime(): " + ex.getMessage());
        }
    }

    @Override
    public final void endSimulation() {
        _circuit.endSimulation();
        _simInited = false;
    }

    public final void endScript() {
        if (_simInited) {
            endSimulation();
            writeOutputLn("Warning: Did not call endSimulation() after using simulation control methods! "
                    + "Please call this method at the end of your script after using simulateStep(),"
                    + " simulateSteps(), or simulateTime().");
        }
    }

    @Override
    @SuppressWarnings("PMD") // CHECKSTYLE:OFF I cannot rename this method, since it is already used by Gecko-Users!
    public final double get_dt() {
        return _circuit.get_dt();
    }

    @Override
    public final double get_dt_pre() {
        return _circuit.get_dt_pre();
    }

    @Override
    public double get_Tend() {
        return _circuit.get_Tend();
    }

    @Override
    public double get_Tend_pre() {
        return _circuit.get_Tend_pre();
    }

    // CHECKSTYLE:ON
    public abstract void runScript();

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.WRITE_OUTPUT_DOCUMENTATION)
    @Declaration("void writeOutput(String toBeWritten)")
    public final void writeOutput(final Object toBeWritten) {
        _output.print(toBeWritten.toString());
        _output.flush();
        GeckoCustomRemote.printLn(toBeWritten.toString());
    }

    @Category(MethodCategory.SIGNAL_PROCESSING)
    @Documentation(I18nKeys.WRITE_OUTPUT_LN_DOCUMENTATION)
    @Declaration("void writeOutputLn(String toBeWritten)")
    public final void writeOutputLn(final Object toBeWritten) {
        _output.println(toBeWritten.toString());
        GeckoCustomRemote.printLn(toBeWritten.toString());
    }

    public final void writerOutputErrorLn(final Object toBeWritten) {
        _output.println(toBeWritten.toString());
        GeckoCustomRemote.printErrorLn(toBeWritten.toString());
    }


    /*
     * protected double getNodeValue(String name) { try { return
     * circuit.getNodeValue(name); } catch (Exception e) {
     * System.err.println(e); writeOutput("Node named " + name + " not found in
     * circuit!"); } return 0; }
     */
    @Override
    //gives the user an array of strings - names of all control elements in circuit
    public final String[] getControlElements() {
        final List<RegelBlock> controlElems = _circuit.se.getElementCONTROL();
        final int size = controlElems.size();
        final String[] controlNames = new String[size];
        for (int i = 0; i < size; i++) {
            controlNames[i] = controlElems.get(i).getStringID();
        }
        return controlNames;
    }

    @Override
    //gives the user an array of strings - names of all circuit elements in circuit
    public final String[] getCircuitElements() {
        final List<AbstractCircuitBlockInterface> circuitElems = _circuit.se.getElementLK();
        final int size = circuitElems.size();
        final String[] circuitNames = new String[size];
        for (int i = 0; i < size; i++) {
            circuitNames[i] = circuitElems.get(i).getStringID();
        }
        return circuitNames;
    }

    @Override
    public String[] getThermalElements() {
        final List<? extends AbstractCircuitBlockInterface> thermal_elems = _circuit.se.getElementTHERM();
        final int size = thermal_elems.size();
        final String[] thermalNames = new String[size];
        for (int i = 0; i < size; i++) {
            thermalNames[i] = thermal_elems.get(i).getStringID();
        }
        return thermalNames;
    }

    @Override
    public String[] getSpecialElements() {
        final List<? extends AbstractSpecialBlock> spec_elems = _circuit.se.getElementSpecial();
        final int size = spec_elems.size();
        final String[] specialNames = new String[size];
        for (int i = 0; i < size; i++) {
            specialNames[i] = spec_elems.get(i).getStringID();
        }
        return specialNames;
    }

    @Override
    //gives the user an array of strings - names of all IGBTs in circuit
    public final String[] getIGBTs() {
        return createComponentsList(IGBT.class);
    }

    @Override
    //now the same for diodes, resistors, capacitors, thyristors, switches
    public final String[] getDiodes() {
        return createComponentsList(Diode.class);
    }

    @Override
    public final String[] getThyristors() {
        return createComponentsList(Thyristor.class);
    }

    @Override
    public final String[] getIdealSwitches() {
        return createComponentsList(IdealSwitch.class);
    }

    @Override
    public final String[] getResistors() {
        return createComponentsList(AbstractResistor.class);
    }

    @Override
    public final String[] getInductors() {
        return createComponentsList(AbstractInductor.class);
    }

    @Override
    public final String[] getCapacitors() {
        return createComponentsList(AbstractCapacitor.class);
    }

    private <T extends AbstractCircuitBlockInterface> String[] createComponentsList(Class<T> componentClass) {
        List<T> components = _circuit.getComponentsOfType(componentClass);
        String[] returnValue = new String[components.size()];
        for (int i = 0; i < components.size(); i++) {
            returnValue[i] = components.get(i).getIDStringDialog().toString();
        }
        return returnValue;
    }

    @Override
    public void setComponentName(final String oldName, final String newName) throws Exception {
        AbstractBlockInterface foundBlock = IDStringDialog.getComponentByName(oldName);
        foundBlock.setNewNameChecked(newName);
    }

    /**
     * method which sets the value of field of an element, taking the name of
     * that field as argument
     */
    @Override
    public final void setParameter(final String elementName, final String parameterName, final double value) {
        AbstractBlockInterface circuit_elem = IDStringDialog.getComponentByName(elementName);
        try {
            circuit_elem.setAccessibleParameter(parameterName, value);
        } catch (IllegalAccessException e) {
            writerOutputErrorLn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object doOperation(final String elementName, final String operationName, final Object parameterValue) {
        final AbstractBlockInterface component = IDStringDialog.getComponentByName(elementName);
        try {

            if (operationName.equals("setTextFieldPosition")) {
                if (parameterValue instanceof double[]) {
                    double[] array = (double[]) parameterValue;
                    component._textInfo.initPositionRelative(array[0], array[1]);
                }
                return null;
            }

            if (component instanceof Operationable) {
                Operationable.OperationInterface operationInterface
                        = Operationable.OperationInterface.fabricFromString(operationName, (Operationable) component);
                return operationInterface.doOperation(parameterValue);
            } else {
                throw new IllegalAccessException("Operation " + operationName + " cannot be performed on component!");
            }
        } catch (IllegalAccessException e) {
            writerOutputErrorLn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * method which allows several parameters to be set at once, with names and
     * values given as arrays
     *
     * @param elementName
     * @param parameterNames
     * @param values
     */
    @Override
    public final void setParameters(final String elementName, final String[] parameterNames, final double[] values) {
        if (parameterNames.length == values.length) {
            for (int i = 0; i < values.length; i++) {
                setParameter(elementName, parameterNames[i], values[i]);
            }
        } else {
            writerOutputErrorLn("Error writing values for element " + elementName + ": array of names not equal in length to array of values.");
        }
    }

    /**
     * method which sets detailed loss file for switches
     */
    @Override
    @Deprecated
    public final void setLossFile(final String elementName, final String lossFileName) throws FileNotFoundException {

        AbstractBlockInterface circuit_elem = IDStringDialog.getComponentByName(elementName);

        if (!(circuit_elem instanceof SemiconductorLossCalculatable)) {
            throw new RuntimeException("Selected component does not have a loss file property!");
        } else {
            doOperation(elementName, "setLossFile", lossFileName);
        }
    }

    @Override
    @Deprecated
    public final void setNonLinear(final String elementName, final String characteristicFileName)
            throws FileNotFoundException {
        AbstractBlockInterface circuit_elem = IDStringDialog.getComponentByName(elementName);

        if (!(circuit_elem instanceof AbstractNonLinearCircuitComponent)) {
            throw new RuntimeException("Selected component does not use nonlinear characteristics!");
        } else {
            doOperation(elementName, "setNonLinear", characteristicFileName);
        }
    }

    /**
     * gets the value of field of an element, taking the name of that field as
     * argument
     *
     * @param elementName
     * @param parameterName
     * @return
     */
    @Override
    public final double getParameter(final String elementName, final String parameterName) {
        double paramvalue = 0.0;
        AbstractBlockInterface circuit_elem = IDStringDialog.getComponentByName(elementName);
        try {
            paramvalue = circuit_elem.getAccessibleParameterValue(parameterName);
        } catch (IllegalAccessException e) {
            writerOutputErrorLn(e.getMessage());
            throw new RuntimeException(e);
        }
        return paramvalue;
    }

    /**
     * get the output of a control block by name
     *
     * @param elementName
     * @param outputName
     * @return
     */
    @Override
    public final double getOutput(final String elementName, final String outputName) {
        double outputValue = 0.0;
        AbstractBlockInterface abstractBlock = IDStringDialog.getComponentByName(elementName);
        if (!(abstractBlock instanceof RegelBlock)) {
            writerOutputErrorLn("Error, selected component is not a control block!");
            throw new RuntimeException("Error, selected component is not a control block!");
        }
        RegelBlock control_elem = (RegelBlock) abstractBlock;
        try {
            outputValue = control_elem.getOutput(outputName);
        } catch (IllegalAccessException e) {
            writerOutputErrorLn(e.getMessage());
            throw new RuntimeException(e);
        }
        return outputValue;
    }

    /**
     * get the output of a control block by name, for control elements with a
     * single output (no output name specified)
     *
     * @param elementName
     * @return
     */
    @Override
    public final double getOutput(final String elementName) {
        double outputValue = 0.0;
        AbstractBlockInterface abstractBlock = IDStringDialog.getComponentByName(elementName);
        if (!(abstractBlock instanceof RegelBlock)) {
            writerOutputErrorLn("Error, selected component is not a control block!");
            throw new RuntimeException("Error, selected component is not a control block!");
        }
        RegelBlock control_elem = (RegelBlock) abstractBlock;
        try {
            outputValue = control_elem.getOutput();
        } catch (IllegalAccessException e) {
            writerOutputErrorLn(e.getMessage());
            throw new RuntimeException(e);
        }
        return outputValue;
    }

    @Override
    @SuppressWarnings("PMD") // CHECKSTYLE:OFF I cannot rename this method, since it is already used by Gecko-Users!
    public final void set_Tend(final double Tend) {
        _circuit.set_Tend(Tend);
    }

    @Override
    @SuppressWarnings("PMD") // CHECKSTYLE:OFF I cannot rename this method, since it is already used by Gecko-Users!
    public final void set_Tend_pre(final double Tend) {
        _circuit.set_Tend_pre(Tend);
    }

    //CHECKSTYLE:ON
    @Override
    @SuppressWarnings("PMD") // CHECKSTYLE:OFF I cannot rename this method, since it is already used by Gecko-Users!
    public final void set_dt(final double value) {
        _circuit.set_dt(value);
    }

    @Override
    @SuppressWarnings("PMD") // CHECKSTYLE:OFF I cannot rename this method, since it is already used by Gecko-Users!
    public final void set_dt_pre(final double value) {
        _circuit.set_dt_pre(value);
    }

    //CHECKSTYLE:ON
    @Override
    public final void saveFileAs(final String fileName) {
        _circuit.saveFileAs(fileName);
    }

    @Override
    public void openFile(final String fileName) throws RemoteException, FileNotFoundException {
        GeckoSim.operatingmode = OperatingMode.REMOTE;
        writeOutputLn("Warning: File loaded, autobackup check disabled!");
        try {
            _circuit.openFile(fileName);
        } catch (FileNotFoundException ex) {  // search local file path as next step...
            final File ipesFileName = new File(GlobalFilePathes.datnamAbsLoadIPES);
            if (ipesFileName.exists()) {
                try {
                    _circuit.openFile(ipesFileName.getParent() + "/" + fileName);
                } catch (FileNotFoundException ex1) {
                    writerOutputErrorLn("File not found: " + fileName);
                    throw ex1;
                }
            } else {
                writerOutputErrorLn("File not found: " + fileName);
                throw new FileNotFoundException("File not found: " + fileName);
            }

        }
    }

    @Override
    public void importFromFile(final String fileName, final String importIntoSubcircuit) throws RemoteException, FileNotFoundException {
        try {
            _circuit.importFromFile(fileName, importIntoSubcircuit);
        } catch (FileNotFoundException ex) {  // search local file path as next step...
            final File ipesFileName = new File(GlobalFilePathes.datnamAbsLoadIPES);
            if (ipesFileName.exists()) {
                try {
                    _circuit.openFile(ipesFileName.getParent() + "/" + fileName);
                } catch (FileNotFoundException ex1) {
                    writerOutputErrorLn("File not found: " + fileName);
                    throw ex1;
                }
            } else {
                writerOutputErrorLn("File not found: " + fileName);
                throw new FileNotFoundException("File not found: " + fileName);
            }
        }
    }

    @Override
    public final double[] getSignalCharacteristics(final String scopeName, final int scopePort,
            final double startTime, final double endTime) {
        try {
            return _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return new double[8];
        }
    }

    @Override
    public final double getAvg(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[0];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getRMS(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[1];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public final double getTHD(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[2];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public final double getMin(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[3];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public final double getMax(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[4];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public double getRipple(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[5];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public final double getKlirr(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[6];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public double getShape(final String scopeName, final int scopePort, final double startTime, final double endTime) {
        try {
            final double[] characteristics = _circuit.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
            return characteristics[7];
        } catch (Exception e) {
            writerOutputErrorLn(e.getMessage());
            return 0;
        }
    }

    @Override
    public final double[] getSignalCharacteristics(final String scopeName, final double startTime, final double endTime) {
        return getSignalCharacteristics(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getAvg(final String scopeName, final double startTime, final double endTime) {
        return getAvg(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getRMS(final String scopeName, final double startTime, final double endTime) {
        return getRMS(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getMin(final String scopeName, final double startTime, final double endTime) {
        return getMin(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getMax(final String scopeName, final double startTime, final double endTime) {
        return getMax(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getTHD(final String scopeName, final double startTime, final double endTime) {
        return getTHD(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getShape(final String scopeName, final double startTime, final double endTime) {
        return getShape(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getKlirr(final String scopeName, final double startTime, final double endTime) {
        return getKlirr(scopeName, 0, startTime, endTime);
    }

    @Override
    public final double getRipple(final String scopeName, final double startTime, final double endTime) {
        return getRipple(scopeName, 0, startTime, endTime);
    }

    private final int getSignalRow(String signalName) {
        int signalRow = -1;
        for (int row = 0; row < NetzlisteCONTROL.globalData.getRowLength(); row++) {
            String tmpSignalName = NetzlisteCONTROL.globalData.getSignalName(row);
            String tmpSignalPath = NetzlisteCONTROL.globalData.getSubcircuitSignalPath(row);
            String qualifiedName = tmpSignalPath + tmpSignalName;

            if (qualifiedName.equals(signalName)) {
                signalRow = row;
            }
        }

        if (signalRow == -1) {
            for (int row = 0; row < NetzlisteCONTROL.globalData.getRowLength(); row++) {
                String tmpSignalName = NetzlisteCONTROL.globalData.getSignalName(row);
                if (tmpSignalName.equals(signalName)) {
                    signalRow = row;
                }
            }
        }

        if (signalRow == -1) {
            writerOutputErrorLn("Error, Inputsignal \"" + signalName + "\" could not be found!");
            throw new RuntimeException("Error, Inputsignal \"" + signalName + "\" could not be found!");
        } else {
            return signalRow;
        }
    }

    @Override
    public final double getSignalAvg(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[0];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalRMS(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[1];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalMin(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[3];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalMax(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[4];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalTHD(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[2];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalShape(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[7];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalKlirr(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[6];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double getSignalRipple(final String signalName, final double startTime, final double endTime) {
        int[] row = {getSignalRow(signalName)};
        CharacteristicsCalculator charCalc = CharacteristicsCalculator.calculateFabric(NetzlisteCONTROL.globalData, row, startTime, endTime);
        try {
            double[] characteristics = charCalc.getChannelCharacteristics(0);
            return characteristics[5];
        } catch (GeckoInvalidArgumentException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public final double[][] getSignalFourier(final String signalName, final double startTime, final double endTime,
            final int harmonics) {
        return getFourier(signalName, 0, startTime, endTime, harmonics);
    }
    //functions for steady-state monitoring and detection
    private double[] _periods;
    private String[] _stateVars;
    private double _steadyStateDt;
    private double _steadyStateSimTime;
    private boolean _steadyStateInitialized = false;
    private boolean _steadyStateCrossCorrelationInitialized = false;
    private final static double DEFAULT_TARGET_CORRELATION = 0.998;
    private final static double DEFAULT_TARGET_MEAN_PCT_DIFF = 0.005;

    @Override
    public final void initSteadyStateDetection(final String[] stateVariables, final double[] frequencies,
            final double deltaT, final double simulationTime) {
        _steadyStateDt = deltaT;
        _steadyStateSimTime = simulationTime;

        _periods = new double[frequencies.length];

        for (int i = 0; i < frequencies.length; i++) {
            _periods[i] = 1.0 / frequencies[i];
        }

        try {
            AbstractBlockInterface elem;

            for (int i = 0; i < stateVariables.length; i++) {
                elem = IDStringDialog.getComponentByName(stateVariables[i]);
                if (!((elem instanceof ReglerVOLT) || (elem instanceof ReglerAmperemeter) || (elem instanceof ReglerTEMP))) {
                    writerOutputErrorLn("Supplied element " + stateVariables[i] + " is not a measuring element. Steady state monitoring not initialized!");
                    return;
                }
            }

            if ((_periods.length == 0) || (stateVariables.length == 0)) {
                writerOutputErrorLn("Steady state monitoring not initialized: no supplied frequencies or state variables!");
                return;
            }

            _stateVars = stateVariables;
            _steadyStateInitialized = true;
            _steadyStateCrossCorrelationInitialized = false;

        } catch (Exception e) {
            writerOutputErrorLn("Steady state monitoring not initialized: " + e.getMessage());
        }

    }

    private double[] constructSteadyStateVector() {
        final double[] steadyStateVector = new double[_stateVars.length];

        for (int i = 0; i < _stateVars.length; i++) {
            steadyStateVector[i] = getOutput(_stateVars[i]);
        }

        return steadyStateVector;
    }

    @Override
    public double getSimulationTime() {
        return _circuit.getSimulationTime();
    }

    private double[] compareVectors(final double[] vector1, final double[] vector2) {
        double[] diff = new double[vector1.length];
        double absdiff;

        //calculate % difference for each element, and take the larger % difference
        for (int i = 0; i < vector1.length; i++) {
            absdiff = Math.abs(vector1[i] - vector2[i]);
            diff[i] = Math.max(absdiff / vector1[i], absdiff / vector2[i]);
        }

        return diff;
    }

    /**
     * evaluate difference vector, return true if steady-state condition is
     * satisfied take as argument allowed error (difference between 2 vectors),
     * and also comparison option option 1 - take average of all differences
     * option 2 - make sure every difference is below threshold option 3 - make
     * sure most differences are below threshold, and those that are not are
     * below threshold*2
     *
     * @param diff
     * @param allowed_error
     * @param option
     * @return
     */
    private boolean evaluateVector(final double[] diff, final double allowed_error, final int option) {
        boolean steadystate;

        if (option <= 1) {
            double errorTot = 0;
            double avgError;

            for (int i = 0; i < diff.length; i++) {
                errorTot += diff[i];
            }

            avgError = errorTot / diff.length;

            if (avgError <= allowed_error) {
                steadystate = true;
            } else {
                steadystate = false;
            }
        } else if (option == 2) {
            steadystate = true;

            for (int i = 0; i < diff.length; i++) {
                if (diff[i] > allowed_error) {
                    steadystate = false;
                    break;
                }
            }
        } else {
            int noOfPointsBelowError = 0;
            int noOfPointsBelowTwiceError = 0;
            int noOfPointsAboveTwiceError = 0;

            for (int i = 0; i < diff.length; i++) {
                if (diff[i] <= allowed_error) {
                    noOfPointsBelowError++;
                } else if (diff[i] <= (2.0 * allowed_error)) {
                    noOfPointsBelowTwiceError++;
                } else {
                    noOfPointsAboveTwiceError++;
                }
            }

            //number of points which must be below allowed error
            final int requiredPrecision = (int) Math.round(diff.length * 0.75);

            if ((noOfPointsBelowError >= requiredPrecision) && (noOfPointsBelowTwiceError <= (diff.length - requiredPrecision)) && (noOfPointsAboveTwiceError == 0)) {
                steadystate = true;
            } else {
                steadystate = false;
            }
        }

        return steadystate;
    }

    @Override
    public final double[] simulateUntilSteadyState(final boolean supressMessages) {
        if (!_steadyStateInitialized) {
            writerOutputErrorLn("Steady state detection is not initialized! Please call initSteadyStateDetection() with appropriate arguments first!");
            return null;
        }

        initSimulation(_steadyStateDt, _steadyStateSimTime);

        double time = 0;

        double[] steadyStateVector;
        double[] steadyStateVectorNew;
        double[] vectorDifference;

        //simulate first for a while without checking steady state
        for (int i = 0; i < 100; i++) {
            simulateStep();
            time += _steadyStateDt;
        }

        boolean steadyStateReached = false;
        double potentialCycleStartTime;
        int potentialCyclesPassed = 0;
        double period = 0;

        /*
         * for (int i = 0; i < _periods.length; i++)
         * System.out.println("Potential period: " + _periods[i]);
         */
        steadyStateVector = constructSteadyStateVector();
        potentialCycleStartTime = time;
        while (!steadyStateReached && (time < _steadyStateSimTime)) {
            simulateStep();
            time += _steadyStateDt;
            for (int i = 0; i < _periods.length; i++) {
                if (Math.abs((time - potentialCycleStartTime) - _periods[i]) <= (2.1 * _steadyStateDt)/*
                         * (dt/1000)
                         */) {
                    /*
                     * System.out.println("time - potentialCycleStartTime = " +
                     * (time - potentialCycleStartTime));
                     * System.out.println("checking for steady state at time = "
                     * + time);
                     */
                    potentialCyclesPassed++;
                    steadyStateVectorNew = constructSteadyStateVector();
                    vectorDifference = compareVectors(steadyStateVector, steadyStateVectorNew);
                    /*
                     * for (int j = 0; j < vectorDifference.length; j++)
                     * System.out.println("diff[" + j + "] = " +
                     * vectorDifference[j]);
                     */
                    //allowed error 0.1%
                    steadyStateReached = evaluateVector(vectorDifference, 0.001, 2);
                    if (steadyStateReached) {
                        period = time - potentialCycleStartTime;//_periods[i];
                        break;
                    }
                }
            }
            if (potentialCyclesPassed == _periods.length * 5) {
                steadyStateVector = constructSteadyStateVector();
                potentialCycleStartTime = time;
                potentialCyclesPassed = 0;
            }
        }

        if (!supressMessages) {
            if (steadyStateReached) {
                writeOutputLn("Steady state reached at " + time + " seconds with period of oscillation: " + period + " seconds.");
            } else {
                writerOutputErrorLn("Steady state could not be reached.");
            }
        }
        double[] result = new double[3];
        result[0] = (steadyStateReached) ? 1 : 0; //1 if steady state reached, zero otherwise
        result[1] = time; //time at which steady state was reached
        result[2] = period;
        endSimulation();
        return result;
    }

    @Override
    public final void initSteadyStateDetection(final String[] stateVariables, final double frequency,
            final double deltaT, final double simulationTime) {
        _steadyStateDt = deltaT;
        _steadyStateSimTime = simulationTime;
        _periods = new double[]{1.0 / frequency};

        initSimulation(_steadyStateDt, _steadyStateSimTime); //initialize simulation to find signal names

        try {
            final AbstractDataContainer data = NetzlisteCONTROL.globalData;

            for (int i = 0; i < stateVariables.length; i++) {
                int foundIndex = -1; // search for the signal with the right name.
                for (int j = 0; j < data.getRowLength(); j++) {
                    if (data.getSignalName(j).equals(stateVariables[i])) {
                        foundIndex = j;
                    }
                }

                if (foundIndex < 0) {
                    writerOutputErrorLn("Could not find signal: " + stateVariables[i]);
                    return;
                }
            }

            if ((_periods.length == 0) || (stateVariables.length == 0) || frequency == 0) {
                writerOutputErrorLn("Steady state monitoring not initialized: no supplied frequencies or state variables!");
                return;
            }

            _stateVars = stateVariables;
            _steadyStateInitialized = false;
            _steadyStateCrossCorrelationInitialized = true;

        } catch (Exception e) {
            writerOutputErrorLn("Steady state monitoring not initialized: " + e.getMessage());
        }

    }

    @Override
    public final double[] simulateToSteadyState(final boolean supressMessages) {
        return simulateToSteadyState(supressMessages, DEFAULT_TARGET_CORRELATION, DEFAULT_TARGET_MEAN_PCT_DIFF);
    }

    @Override
    public final double[] simulateToSteadyState(final boolean supressMessages, final double targetCorrelation, final double targetMeanPctDiff) {
        if (!_steadyStateCrossCorrelationInitialized) {
            writerOutputErrorLn("Steady state detection is not initialized! Please call initSteadyStateDetection() with appropriate arguments first!");
            return null;
        }

        double time = 0;
        double start = 0;

        boolean steadyStateReached = false;
        double period = _periods[0];
        List<float[]> stateVarList = new ArrayList<float[]>();
        List<double[]> results;
        //double[] correlations;
        //double[] meanPctDiffs, rmsPctDiffs;

        //simulate one period first
        simulateTime(period);
        time += period;
        while (!steadyStateReached && time < _steadyStateSimTime) {
            //we always simulate 1 period, and then check for cross-correlation between it and the previously simulated period
            simulateTime(period);
            time += period;
            stateVarList.clear();
            for (String variable : _stateVars) {
                stateVarList.add(getSignalData(variable, start, time, 0));
            }
            results = getCrossCorrelation(stateVarList);
            /*correlations = results.get(0);
            meanPctDiffs = results.get(1);
            rmsPctDiffs = results.get(2);
            writeOutputLn("Cross-correlations and mean/rms pct. diff:");
            for (int i = 0; i < correlations.length; i++) {
                writeOutputLn("corr = " + correlations[i] + ", mean pct. diff = " + meanPctDiffs[i] + ", rms pct. diff = " + rmsPctDiffs[i]);
            }*/
            steadyStateReached = evaluateCorrelations(results, targetCorrelation, targetMeanPctDiff);
            start += period;
        }
        endSimulation();
        if (!supressMessages) {
            if (steadyStateReached) {
                writeOutputLn("Steady state reached at " + time + " seconds with period of oscillation: " + period + " seconds.");
            } else {
                writerOutputErrorLn("Steady state could not be reached.");
            }
        }
        double[] result = new double[3];
        result[0] = (steadyStateReached) ? 1 : 0; //1 if steady state reached, zero otherwise
        result[1] = time; //time at which steady state was reached
        result[2] = period;
        endSimulation();
        return result;
    }

    /**
     * Get the cross-correlation between two periods of a simulation.
     *
     * @param waveforms a list of all the waveforms to evaluate (each waveform
     * must have 2 periods)
     * @return a list which contains three arrays - one with the
     * cross-correlations, one with % difference of the means of the two
     * periods, and one with the % difference of the RMS values of the two
     * periods
     */
    private List<double[]> getCrossCorrelation(List<float[]> waveforms) {
        final double[] crossCorrelations;
        final double[] meanRatios, rmsRatios;

        //find index in array which separates two periods
        int size = waveforms.get(0).length;
        int offset = size / 2;
        int endp1;
        if (size % 2 == 0) {
            endp1 = offset - 1;
        } else { //if number of time steps is odd, we "share" the middle point with both periods
            endp1 = offset;
        }

        //convert list of waveforms to 2-D for easier processing
        final int numOfWaveforms = waveforms.size();
        float[][] waveformsAsArray = new float[numOfWaveforms][0];
        for (int i = 0; i < waveformsAsArray.length; i++) {
            waveformsAsArray[i] = waveforms.get(i);
        }

        final double[] t1mean = new double[numOfWaveforms], t2mean = new double[numOfWaveforms];
        final double[] t1rms = new double[numOfWaveforms], t2rms = new double[numOfWaveforms];

        //compute the means and RMS of the two periods
        int pointsAdded = 0;
        for (int i = 1; i <= endp1; i++) {
            for (int j = 0; j < numOfWaveforms; j++) {
                t1mean[j] += waveformsAsArray[j][i];
                t1rms[j] += waveformsAsArray[j][i] * waveformsAsArray[j][i];
                pointsAdded++;
            }
        }
        for (int i = 0; i < t1mean.length; i++) {
            t1mean[i] = t1mean[i] / pointsAdded;
            t1rms[i] = Math.sqrt(t1rms[i] / pointsAdded);
        }

        pointsAdded = 0;
        for (int i = offset; i < size; i++) {
            for (int j = 0; j < numOfWaveforms; j++) {
                t2mean[j] += waveformsAsArray[j][i];
                t2rms[j] += waveformsAsArray[j][i] * waveformsAsArray[j][i];
                pointsAdded++;
            }
        }
        for (int i = 0; i < t2mean.length; i++) {
            t2mean[i] = t2mean[i] / pointsAdded;
            t2rms[i] = Math.sqrt(t2rms[i] / pointsAdded);
        }

        //now compute % differences between the means
        meanRatios = new double[numOfWaveforms];
        rmsRatios = new double[numOfWaveforms];
        double meanDiff, rmsDiff;
        for (int i = 0; i < t1mean.length; i++) {
            //System.out.println("t1mean[" + i + "] = " + t1mean[i] + " t2mean[" + i + "] = " + t2mean[i]);
            meanDiff = Math.abs(t1mean[i] - t2mean[i]);
            meanRatios[i] = Math.max(Math.abs(meanDiff / t1mean[i]), Math.abs(meanDiff / t2mean[i]));
            rmsDiff = Math.abs(t1rms[i] - t2rms[i]);
            rmsRatios[i] = Math.max(Math.abs(rmsDiff / t1rms[i]), Math.abs(rmsDiff / t2rms[i]));
        }

        double[] sumT1, sumT2, sumT1T2;
        sumT1 = new double[numOfWaveforms];
        sumT2 = new double[numOfWaveforms];
        sumT1T2 = new double[numOfWaveforms];

        //compute the series (numerator and denominator of cross-correlation formula)
        for (int i = 0; i <= endp1; i++) {
            for (int j = 0; j < numOfWaveforms; j++) {
                sumT1[j] += (waveformsAsArray[j][i] - t1mean[j]) * (waveformsAsArray[j][i] - t1mean[j]);
                sumT2[j] += (waveformsAsArray[j][i + offset] - t2mean[j]) * (waveformsAsArray[j][i + offset] - t2mean[j]);
                sumT1T2[j] += (waveformsAsArray[j][i] - t1mean[j]) * (waveformsAsArray[j][i + offset] - t2mean[j]);
            }
        }

        //calculate cross correlation for each vector element
        crossCorrelations = new double[numOfWaveforms]; //return cross correlations plus means of the two series

        for (int i = 0; i < crossCorrelations.length; i++) {
            crossCorrelations[i] = sumT1T2[i] / (Math.sqrt(sumT1[i]) * Math.sqrt(sumT2[i]));
        }
        final List<double[]> results = new ArrayList<double[]>();
        results.add(crossCorrelations);
        results.add(meanRatios);
        results.add(rmsRatios);
        return results;
    }

    /**
     * Evaluate the results of the cross-correlation of 2 periods of the
     * simulation, i.e. check if the circuit has reached steady-state.
     *
     * @param correlationResults the results of the evaluation of the
     * cross-correlation between 2 periods of the relevant simulation waveforms,
     * given as a list which contains three arrays - one with the
     * cross-correlations, one with % difference of the means of the two
     * periods, and one with the % difference of the RMS values of the two
     * periods
     * @param targetCorrelation the target correlation. Ideally it is 1, (a
     * number higher than 1 is invalid!), but realistically this is never
     * reached, in most cases this should be in the range 0.99 - 0.999
     * @param targetMeanPctDiff the target percentage difference (0 - 1) between
     * the means or RMS of the two periods. Ideally it is 0 (a value less than
     * zero is invalid!), but realistically it should be 0.001 - 0.005
     * @return true if the correlations and mean/rms percentage differences are
     * equal to or greater (less for means/rms) than their targets, i.e. if the
     * circuit has reached steady state, false otherwise
     */
    private boolean evaluateCorrelations(final List<double[]> correlationResults, final double targetCorrelation, final double targetMeanPctDiff) {

        final double[] correlations = correlationResults.get(0);
        final double[] meanPctDiffs = correlationResults.get(1);
        final double[] rmsPctDiffs = correlationResults.get(2);

        for (int i = 0; i < correlations.length; i++) {
            if (correlations[i] < targetCorrelation) {
                return false;
            }
        }

        //for waveforms with a DC offset, RMS and mean % differences are very close typically
        //for waveforms without a DC offset, the mean % difference is not really relevant, only the RMS
        //therefore we take the minimum of the two to check
        for (int i = 0; i < meanPctDiffs.length; i++) {
            if (Math.min(meanPctDiffs[i], rmsPctDiffs[i]) > targetMeanPctDiff) {
                return false;
            }
        }

        return true;
    }

    @Override
    //fourier analysis of signal stored in a particular scope
    public final double[][] getFourier(final String scopeName, final int scopePort,
            final double startTime, final double endTime, final int harmonics) {
        try {
            return _circuit.doFourierAnalysis(scopeName, scopePort, startTime, endTime, harmonics);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                writerOutputErrorLn(e.getMessage());
            } else {
                e.printStackTrace();
                writerOutputErrorLn(e);
            }

            return null;
        }
    }

    @Override
    public final double[][] getFourier(final String scopeName, final double startTime, final double endTime,
            final int harmonics) {
        return getFourier(scopeName, 0, startTime, endTime, harmonics);
    }

    @Override
    //method which sets the value of field of an element, taking the name of that field as argument
    public final void setPosition(final String elementName, final int xPosition, final int yPosition) {

        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);

        try {
            _circuit.setElementPosition(parentElement, xPosition, yPosition);
        } catch (Exception e) {
            writerOutputErrorLn("ERROR setting position of " + elementName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }

    }
    
    
    
    
    @Override
    //method which sets the value of field of an element, taking the name of that field as argument
    public final int[] getPosition(final String elementName) {

        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);

        try {
            return new int[]{parentElement.getSheetPosition().x, parentElement.getSheetPosition().y};            
        } catch (Exception e) {
            writerOutputErrorLn("ERROR setting position of " + elementName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }        
    }

    @Override
    public final void deleteComponent(final String elementName) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);
        _circuit.deleteElement(parentElement);
    }

    @Override
    public void deleteAllComponents(String subcircuitName) {
        if (subcircuitName == null) {
            _circuit.se.resetCircuitSheetsForNewFile();
        } else {
            final AbstractBlockInterface component = IDStringDialog.getComponentByName(subcircuitName);
            if (component instanceof SubcircuitBlock) {
                SubcircuitBlock scb = (SubcircuitBlock) component;
                _circuit.se.deleteAllComponents(new ArrayList<AbstractCircuitSheetComponent>(scb._myCircuitSheet.allElements));
            } else {
                writerOutputErrorLn("Error: argument cannot be found as subcircuit!");
            }
        }
    }

    @Override
    @Deprecated
    public final void delete(final String elementName) {
        deleteComponent(elementName);
    }

    @Override
    @Deprecated
    public final void create(final String elementType, final String elementName, final int xPosition, final int yPosition) {
        createComponent(elementType, elementName, xPosition, yPosition);
    }

    @Override
    public void createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) {
        try {
            _circuit.createNewConnector(elementName, xStart, yStart, xEnd, yEnd, startHorizontal);
        } catch (Exception e) {
            writerOutputErrorLn("ERROR creating new connector " + elementName + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void createComponent(final String elementType, final String elementName, final int xPosition, final int yPosition) {

        if (IDStringDialog.isNameAlreadyUsed(elementName)) {
            writerOutputErrorLn("The component name is already in use: " + elementName);
            throw new RuntimeException("The component name is already in use: " + elementName);
        }

        try {

            final AbstractTypeInfo elemTyp = AbstractTypeInfo.getFromComponentName(elementType);
            AbstractBlockInterface parentElement = _circuit.createNewElement(elemTyp, elementName, xPosition, yPosition);
        } catch (Exception e) {
            writerOutputErrorLn("ERROR creating new element " + elementName + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void setOutputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);

        if (parentElement != null) {
            final Collection<AbstractTerminal> outTerms = parentElement.YOUT;
            if (nodeIndex < 0 || nodeIndex >= outTerms.size()) {
                String errorMessage = "Invalid output node index " + nodeIndex + "; " + elementName
                        + " has " + outTerms.size() + " output node(s).";
                writerOutputErrorLn(errorMessage);
                throw new ArrayIndexOutOfBoundsException(errorMessage);
            } else {
                try {
                    _circuit.setElementNodeLabel(parentElement, StartOrStopNode.STOP_NODE, nodeIndex, nodeName);
                } catch (Exception e) {
                    writerOutputErrorLn("ERROR setting node label for " + elementName + ": " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public final void setInputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);

        final Collection<AbstractTerminal> inputPorts = parentElement.XIN;
        if (nodeIndex < 0 || nodeIndex >= inputPorts.size()) {
            String errorMessage = "Invalid input node index " + nodeIndex + "; "
                    + elementName + " has " + inputPorts.size() + " input node(s).";
            writerOutputErrorLn(errorMessage);
            throw new ArrayIndexOutOfBoundsException(errorMessage);
        } else {
            try {
                _circuit.setElementNodeLabel(parentElement, StartOrStopNode.START_NODE.START_NODE, nodeIndex, nodeName);
            } catch (Exception e) {
                writerOutputErrorLn("ERROR setting node label for " + elementName + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public final String getOutputNodeName(final String elementName, final int nodeIndex) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);

        if (parentElement != null) {
            final Collection<AbstractTerminal> outTerms = parentElement.YOUT;
            if (nodeIndex < 0 || nodeIndex >= outTerms.size()) {
                String errorMessage = "Invalid output node index " + nodeIndex + "; " + elementName
                        + " has " + outTerms.size() + " output node(s).";
                writerOutputErrorLn(errorMessage);
                throw new ArrayIndexOutOfBoundsException(errorMessage);
            } else {
                try {
                    return _circuit.getElementNodeLabel(parentElement, StartOrStopNode.STOP_NODE, nodeIndex);
                } catch (Exception e) {
                    writerOutputErrorLn("ERROR setting node label for " + elementName + ": " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
        return "";
    }

    @Override
    public final String getInputNodeName(final String elementName, final int nodeIndex) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);

        final Collection<AbstractTerminal> inputPorts = parentElement.XIN;
        if (parentElement != null) {
            if (nodeIndex < 0 || nodeIndex >= inputPorts.size()) {
                String errorMessage = "Invalid input node index " + nodeIndex + "; "
                        + elementName + " has " + inputPorts.size() + " input node(s).";
                writerOutputErrorLn(errorMessage);
                throw new ArrayIndexOutOfBoundsException(errorMessage);
            } else {
                try {
                    return _circuit.getElementNodeLabel(parentElement, StartOrStopNode.START_NODE.START_NODE, nodeIndex);
                } catch (Exception e) {
                    writerOutputErrorLn("ERROR setting node label for " + elementName + ": " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
        return "";
    }

    @Override
    public final void rotate(final String elementName) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);
        parentElement.rotiereSymbol();
        parentElement.absetzenElement();
    }

    /**
     * the direction is based on the flow direction! - i.e. "down" means flow
     * direction arrow is pointing down, etc.
     *
     * @param elementName
     * @param direction DOWN - the default, Typ.NS LEFT - the next in the
     * default rotation, Typ.OW UP - the 2nd in the default rotation, Typ.SN
     * RIGHT - the 3rd in the default rotation, Typ.WO
     */
    @Override
    public final void setOrientation(final String elementName, final String direction) {
        final AbstractBlockInterface parentElement = IDStringDialog.getComponentByName(elementName);
        ComponentDirection orientation = null;

        if ("down".equalsIgnoreCase(direction.trim())) {
            orientation = ComponentDirection.NORTH_SOUTH;
        } else if ("left".equals(direction.trim())) {
            orientation = ComponentDirection.EAST_WEST;
        } else if ("up".equals(direction.trim())) {
            orientation = ComponentDirection.SOUTH_NORTH;
        } else if ("right".equalsIgnoreCase(direction.trim())) {
            orientation = ComponentDirection.WEST_EAST;
        }

        if (orientation == null) {
            writerOutputErrorLn("ERROR setting orientation for " + elementName + ": Invalid orientation! "
                    + "Must be UP, DOWN, LEFT, or RIGHT.");
        } else {
            parentElement.setComponentDirection(orientation);
        }
    }

    /**
     * Helper class for the following two methods (getTimeArray and
     * getSignalData), used to avoid code duplication.
     */
    private static class TimeIntervalData {

        final int _checkedSkipPoints;
        private final AbstractDataContainer _data;
        private final AbstractTimeSerie _timeSerie;
        final int _startIndex;
        final int _stopIndex;
        final int _numberOfSignalPoints;
        final int _indexLimitMaximum;

        TimeIntervalData(final double tStart, final double tEnd, final int skipPoints, final AbstractDataContainer data) {
            _data = data;
            _timeSerie = _data.getTimeSeries(0);
            _checkedSkipPoints = Math.max(skipPoints, 1);
            _startIndex = _timeSerie.findTimeIndex(tStart);
            _stopIndex = _timeSerie.findTimeIndex(tEnd);
            int numberOfSignalPoints = _stopIndex - _startIndex;
            if (numberOfSignalPoints < 0) {
                numberOfSignalPoints = 0;
            }

            numberOfSignalPoints /= _checkedSkipPoints; // yes, Integer division!
            _numberOfSignalPoints = numberOfSignalPoints;
            _indexLimitMaximum = _startIndex + _numberOfSignalPoints * _checkedSkipPoints;
        }
    }

    @Override
    public final float[] getSignalData(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        final AbstractDataContainer data = NetzlisteCONTROL.globalData;
        final TimeIntervalData timeIntervalData = new TimeIntervalData(tStart, tEnd, skipPoints, data);

        int foundIndex = -1; // search for the signal with the right name.
        for (int i = 0; i < data.getRowLength(); i++) {
            if (data.getSignalName(i).equals(signalName)) {
                foundIndex = i;
            }
        }

        if (foundIndex < 0) {
            writerOutputErrorLn("could not find signal: " + signalName);
            return new float[0];
        }

        float[] returnValue = new float[timeIntervalData._numberOfSignalPoints];
        for (int i = timeIntervalData._startIndex, counter = 0; i < timeIntervalData._indexLimitMaximum;
                i += timeIntervalData._checkedSkipPoints, counter++) {
            returnValue[counter] = data.getValue(foundIndex, i);

        }

        return returnValue;
    }

    @Override
    public final double[] getTimeArray(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        final AbstractDataContainer data = NetzlisteCONTROL.globalData;
        final TimeIntervalData timeIntervalData = new TimeIntervalData(tStart, tEnd, skipPoints, data);
        final AbstractTimeSerie timeSerie = data.getTimeSeries(0);

        double[] returnValue = new double[timeIntervalData._numberOfSignalPoints];
        for (int i = timeIntervalData._startIndex, counter = 0; i < timeIntervalData._indexLimitMaximum;
                i += timeIntervalData._checkedSkipPoints, counter++) {
            returnValue[counter] = timeSerie.getValue(i);
        }

        return returnValue;
    }

    @Override
    public void shutdown() throws RemoteException {
        System.exit(0);
    }

    @Override
    public void disconnect(final long sessionID) {
        // only needed for remote interface!
    }

    @Override
    public long connect() {
        // only used for remote connection.
        return 0;
    }

    @Override
    public long getSessionID() {
        // implementation only needed for remote connection, not here!
        return -1; //this is never a session ID - see above
    }

    //these are functions for remote access, and are not needed here
    @Override
    public boolean isFree() {
        return false; //always false since GeckoCustom is not for remote access 
        // unless specifically overwritten by GeckoCustomRemote
    }

    @Override
    public void acceptExtraConnections(int numberOfExtraConnections) {
        //only needed in remote interface
    }

    @Override
    public boolean acceptsExtraConnections() {
        return false; //always false since this class is not for remote access
        //unless specifically overwritten by GeckoCustomRemote
    }

    @Override
    public void registerLastClientToCallMethod(long sessionID) {
        //only needed in remote interface
    }

    @Override
    public boolean checkSessionID(long sessionID) {
        return false; //always false since this class is not for remote access
        //unless specifically overwritten by GeckoCustomRemote
    }

    enum StartOrStopNode {

        START_NODE,
        STOP_NODE;
    }

    @Override
    public float[][] getGlobalFloatMatrix() throws RemoteException {
        return GeckoExternal.getGlobalFloatMatrix();
    }

    @Override
    public double[][] getGlobalDoubleMatrix() throws RemoteException {
        return GeckoExternal.getGlobalDoubleMatrix();
    }

    @Override
    public void setGlobalFloatMatrix(float[][] matrix) throws RemoteException {
        GeckoExternal.setGlobalFloatMatrix(matrix);
    }

    @Override
    public void setGlobalDoubleMatrix(double[][] matrix) throws RemoteException {
        GeckoExternal.setGlobalDoubleMatrix(matrix);
    }

    @Override
    @Deprecated
    public float[] realFFT(final float[] timeValues) {
        return floatFFT(timeValues);
    }

    @Override
    public float[] floatFFT(final float[] timeValues) {
        Cispr16Fft.realft(timeValues, 1);
        return timeValues;
    }

    @Override
    public String[] getAccessibleParameters(String componentName) {
        AbstractBlockInterface circuit_elem = IDStringDialog.getComponentByName(componentName);
        String[] returnValue = circuit_elem.getAccessibleParamterDescription();

        String[] units = circuit_elem.getUnits();

        String[] description = circuit_elem.getAccessibleParameterDescriptionVerbose();
        for (int i = 0; i < returnValue.length; i++) {
            if (i < description.length) {
                returnValue[i] += "\t" + "\t" + units[i] + "\t" + description[i];
            } else {
                returnValue[i] += "\t description missing!!!";
            }
        }

        return returnValue;
    }

    @Override
    public String[] getParametersNames(final String componentName) {
        return getAccessibleParameters(componentName);
    }

    @Override
    public void setGlobalParameterValue(String parameterName, double value) {
        GeckoExternal.setGlobalParameterValue(parameterName, value);
    }

    @Override
    public double getGlobalParameterValue(final String parameterName) {
        return GeckoExternal.getGlobalParameterValue(parameterName);
    }
}
