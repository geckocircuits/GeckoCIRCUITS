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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.control.ReglerSaveData;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import java.io.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * This class is responsible for saving the simulation data to a data file.
 *
 */
public final class DataSaver extends Observable implements Observer {

    private final AbstractDataContainer _data;
    private AbstractLinePrinter _linePrinter;
    private final Runnable _saveRunnable = new SaveRunnable();
    private boolean _abortSignal = false;
    private int _percentage = 0;
    private int _lastSavedDataIndex = -1;
    public static final AtomicInteger WAIT_COUNTER = new AtomicInteger(0);
    private static final int SLEEP_TIMER = 200;
    private static final double PERCENT_CONST = 100;
    private boolean _hasCounterValue = false;
    private static final int MAX_FILE_COUNTER = 1000;
    private final ReglerSaveData _regler;

    public DataSaver(final AbstractDataContainer data, ReglerSaveData regler) {
        super();
        _regler = regler;
        _data = data;
        initSettings();
    }

    public void doManualSave() {
        final Thread runThread = new Thread(_saveRunnable);
        runThread.start();
    }
    
    public void doManualSaveBlocking() {
        if(_data.getContainerStatus() == ContainerStatus.RUNNING) {
            throw new RuntimeException("Error: blocking save can only be initiated when simulation has stopped.");
        }
        
        if(_regler._saveModus != ReglerSaveData.SaveModus.MANUAL) {
            throw new RuntimeException("Error: Data export block must be set to \"Save manually.\"");
        }
        _saveRunnable.run();
    }

    void abortSave() {
        _abortSignal = true;
    }

    @Override
    public void update(final Observable obs, final Object arg) {
        if (_regler._saveModus == ReglerSaveData.SaveModus.SIMULATION_END) {
            if (_data.getContainerStatus() == ContainerStatus.PAUSED) {
                final Thread runThread = new Thread(_saveRunnable);
                runThread.start();
            } else {
                if (!_hasCounterValue) {
                    _hasCounterValue = true;
                    WAIT_COUNTER.incrementAndGet();
                }
            }
        }
    }

    private String findFreeFile(final String origFile) {
        if (!new File(origFile).exists()) {
            return origFile;
        }

        int dotIndex = origFile.lastIndexOf('.');
        if (dotIndex < 1) {
            dotIndex = origFile.length() - 1;
        }
        int underscoreIndex = origFile.lastIndexOf('_');

        if (underscoreIndex < 1) {
            underscoreIndex = dotIndex;
        }

        for (int counter = 0; counter < MAX_FILE_COUNTER; counter++) {
            final String newFileName = origFile.substring(0, underscoreIndex) + "_" + counter + origFile.substring(dotIndex);
            final File newFile = new File(newFileName);
            if (!newFile.exists()) {
                return newFileName;
            }
        }
        return origFile;
    }

    private void initSettings() {
        switch (_regler._saveModus) {
            case SIMULATION_END:
                _data.addObserver(this);
                break;
            case DURING_SIMULATION:
                final Thread runThread = new Thread(_saveRunnable);
                runThread.start();
                break;
            case MANUAL:
                break;
            default:
                assert false;
        }
    }

    private class SaveRunnable implements Runnable {

        @Override
        public void run() {
            try {
                if (_regler._saveModus == ReglerSaveData.SaveModus.SIMULATION_END) {
                    initSave(_data);
                    doFullSave(_data);
                    try {
                        _linePrinter.closeStream();
                    } catch (IOException ex) {
                        Logger.getLogger(DataSaver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    WAIT_COUNTER.decrementAndGet();
                    if (WAIT_COUNTER.get() < 0) {
                        WAIT_COUNTER.set(0);
                    }
                    _hasCounterValue = false;
                } else {
                    initSave(_data);
                    while (_data.getContainerStatus() == ContainerStatus.RUNNING) {
                        doFullSave(_data);
                        try {
                            Thread.sleep(SLEEP_TIMER);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(DataSaver.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    // do one final save at simulation end!
                    doFullSave(_data);
                    try {
                        _linePrinter.closeStream();
                    } catch (IOException ex) {
                        Logger.getLogger(DataSaver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (SignalMissingException exc) {
                JOptionPane.showMessageDialog(null,
                        exc.getMessage(),
                        "Error!",
                        JOptionPane.ERROR_MESSAGE);
            }

            _data.deleteObserver(DataSaver.this);

        }
    };

    public int getPercentage() {
        return _percentage;
    }

    private void initSave(final AbstractDataContainer data) throws SignalMissingException {

        if (_linePrinter != null) {
            try {
                _linePrinter.closeStream();
                _abortSignal = true;
                Thread.sleep(SLEEP_TIMER);
            } catch (InterruptedException ex) {
                Logger.getLogger(DataSaver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (_regler._fileOverwrite.equals(ReglerSaveData.FileOverwrite.DO_NUMBERING) && new File(_regler._file.getValue()).exists()) {
            _regler._file.setValueWithoutUndo(findFreeFile(_regler._file.getValue()));
        }

        switch (_regler._outputType) {
            case TEXT:
                _linePrinter = new TxtLinePrinter(new File(_regler._file.getValue()), data, _regler);
                break;
            case BINARY:
                _linePrinter = new BinaryLinePrinter(new File(_regler._file.getValue()), data, _regler);
                break;
            default:
                assert false;
        }
        try {
            _linePrinter.initStream();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error while saving data to file,\n"
                    + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doFullSave(final AbstractDataContainer data) {
        final int maxIndex = data.getMaximumTimeIndex(0);
        int savePercentage = 0;
        final int skipDataPoints = _regler._skipDataPoints.getValue();
        try {

            if (_regler._transposeData.getValue()) {
                _linePrinter.printTransposedData();
                _percentage = (int) PERCENT_CONST;
                setChanged();
                notifyObservers();
                return;
            }

            for (int line = _lastSavedDataIndex + 1; line <= maxIndex; line += skipDataPoints) {
                if (_abortSignal) {
                    _abortSignal = false;
                    return;
                }


                _linePrinter.printLine(line);
                final int newPercentage = (int) Math.round((PERCENT_CONST * line) / maxIndex);
                if (newPercentage != savePercentage) {
                    savePercentage = newPercentage;
                    _percentage = savePercentage;
                    setChanged();
                    notifyObservers();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            _lastSavedDataIndex = maxIndex;
        } catch (IOException ex) {
            Logger.getLogger(DialogDataExport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    abstract class AbstractLinePrinter {

        final AbstractDataContainer _data;        
        final File _file;
        final int[] _selectedIndices;
        final ReglerSaveData _settings;

        public AbstractLinePrinter(final File file, final AbstractDataContainer data, final ReglerSaveData settings)
                throws SignalMissingException {
            _file = file;
            _data = data;
            _settings = settings;
            compareAndCorrectSignalNamesIndices();
            _selectedIndices = new int[_settings.getSelectedSignalIndices().size()];
            for (int i = 0; i < _selectedIndices.length; i++) {
                _selectedIndices[i] = _settings.getSelectedSignalIndices().get(i);
            }
        }

        abstract void printLine(final int lineNumber) throws IOException;

        abstract void initStream() throws IOException;

        abstract void closeStream() throws IOException;

        abstract void printTransposedData() throws IOException;

        /**
         * Sometimes, when renaming signals, the "connection" to the right index
         * is lost. Here, we search for a signal with the right name. If it is
         * not found, we output a warning signal and continue with the old
         * index.
         *
         * @param _settings
         * @param _data
         */
        private void compareAndCorrectSignalNamesIndices() throws SignalMissingException {
            final List<String> originalNames = _settings.getSelectedNames();

            // loop: check if wrong signals are connected
            for (int i = 0; i < originalNames.size(); i++) {
                final String origName = originalNames.get(i);

                String dataSignalName = "";
                int signalIndex = _settings.getSelectedSignalIndices().get(i);
                if (signalIndex < _data.getRowLength()) {
                    dataSignalName = _data.getSignalName(signalIndex);
                }

                if (!origName.equals(dataSignalName)) {
                    for (int j = 0; j < _data.getRowLength(); j++) {
                        final String realName = _data.getSignalName(j);
                        if (realName.equals(origName)) { // if the original name is found at another position, just
                            // repair the wroing index/connection.                                                        
                            _settings.setSelectedSignal(j, i);
                        }
                    }
                }

                int newSignalIndex = _settings.getSelectedSignalIndices().get(i);

                if (newSignalIndex >= _data.getRowLength()
                        || !_data.getSignalName(newSignalIndex).equals(origName)) {
                    _settings.removeSignal(i);
                    throw new SignalMissingException("The signal \"" + origName + "\" is not available as scope input in the\n"
                            + "simulation model. The signal \"" + origName + "\" was removed from\n"
                            + "the selection.");

                }
            }
        }
    }

    private class TxtLinePrinter extends AbstractLinePrinter {

        private DecimalFormat _sciFormat = new DecimalFormat("0.#####E0");
        private final DecimalFormat _decFormat = new DecimalFormat("###.###");
        private static final double SMALL_THRESHOLD = 0.1;
        private static final double LARGE_THRESHOLD = 100;
        private BufferedWriter _bufferedWriter;
        private final String _separator;

        TxtLinePrinter(final File file, final AbstractDataContainer data,
                final ReglerSaveData settings) throws SignalMissingException {
            super(file, data, settings);
            _separator = settings._itemSeparator.stringValue();
            setFormatters();
        }

        @Override
        void printLine(final int lineNumber) throws IOException {
            _bufferedWriter.write(Double.toString(_data.getTimeValue(lineNumber, 0)));
            _bufferedWriter.write(_separator);

            for (int i = 0; i < _selectedIndices.length; i++) {
                int column = _selectedIndices[i];
                final float value = _data.getValue(column, lineNumber);
                final String numberString = getFormatter(value).format(value);
                _bufferedWriter.write(numberString);
                if (i < _selectedIndices.length - 1) { // don't write the separator at the line end!
                    _bufferedWriter.write(_separator);
                }
            }
            _bufferedWriter.newLine();
        }

        @Override
        void printTransposedData() throws IOException {
            final int maxIndex = _data.getMaximumTimeIndex(0);

            if (_settings._printHeader.getValue()) {
                _bufferedWriter.write(_data.getXDataName());
                _bufferedWriter.write(_separator);
            }

            for (int i = 0; i < maxIndex; i++) {
                final double timeValue = _data.getTimeValue(i, 0);
                _bufferedWriter.write(Double.toString(timeValue));
                if (i < maxIndex - 1) {
                    _bufferedWriter.write(_separator);
                }

            }
            _bufferedWriter.newLine();



            for (int column : _settings.getSelectedSignalIndices()) {
                if (_settings._printHeader.getValue()) {
                    _bufferedWriter.write(_data.getSignalName(column));
                    _bufferedWriter.write(_separator);
                }

                for (int i = 0; i < maxIndex; i++) {
                    final float value = _data.getValue(column, i);
                    final String numberString = getFormatter(value).format(value);
                    _bufferedWriter.write(numberString);
                    if (i < maxIndex - 1) {
                        _bufferedWriter.write(_separator);
                    }
                }
                _bufferedWriter.newLine();
            }


        }

        @Override
        void initStream() throws IOException {
            _bufferedWriter = new BufferedWriter(new FileWriter(_file));
            if (_settings._printHeader.getValue() && !_settings._transposeData.getValue()) {
                printHeader(_data);
            }
        }

        @Override
        void closeStream() throws IOException {
            if (_bufferedWriter != null) {
                _bufferedWriter.flush();
                _bufferedWriter.close();
            }
        }

        private void printHeader(final AbstractDataContainer data) throws IOException {
            // write header symbol
            _bufferedWriter.write(_settings._headerSymbol.toString() + " ");
            // write x-axis symbol:
            _bufferedWriter.write(data.getXDataName() + _separator);

            // write signal header names:
            for (int i : _selectedIndices) {
                _bufferedWriter.write(data.getSignalName(i));
                _bufferedWriter.write(_separator);
            }
            _bufferedWriter.newLine();
        }

        private DecimalFormat getFormatter(final float number) {
            if (number == 0) {
                return _decFormat;
            }

            if (Math.abs(number) < SMALL_THRESHOLD) {
                return _sciFormat;
            }
            if (Math.abs(number) > LARGE_THRESHOLD) {
                return _sciFormat;
            }
            return _decFormat;
        }

        private void setFormatters() {
            final StringBuilder sciFormatString = new StringBuilder("0.");
            for (int i = 1, maxDigits = _settings._significDigits.getValue(); i < maxDigits; i++) {
                sciFormatString.append('#');
            }

            sciFormatString.append("E0");
            _sciFormat = new DecimalFormat(sciFormatString.toString());
        }
    }

    private class BinaryLinePrinter extends AbstractLinePrinter {

        private DataOutputStream _outputStream;

        BinaryLinePrinter(final File file, final AbstractDataContainer data, final ReglerSaveData settings) throws SignalMissingException {
            super(file, data, settings);

        }

        @Override
        void initStream() throws FileNotFoundException {
            _outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_file)));
        }

        @Override
        void closeStream() throws IOException {
            if (_outputStream != null) {
                _outputStream.close();
            }
        }

        @Override
        void printLine(final int lineNumber) throws IOException {
            _outputStream.writeDouble(_data.getTimeValue(lineNumber, 0));
            for (int index : _selectedIndices) {
                _outputStream.writeFloat(_data.getValue(index, lineNumber));
            }
        }

        @Override
        void printTransposedData() throws IOException {
            final int maxIndex = _data.getMaximumTimeIndex(0);
            for (int i = 0; i < maxIndex; i++) {
                _outputStream.writeDouble(_data.getTimeValue(i, 0));
            }

            for (int index : _selectedIndices) {
                for (int i = 0; i < maxIndex; i++) {
                    _outputStream.writeFloat(_data.getValue(index, i));
                }
            }
        }
    }

    class SignalMissingException extends Exception {

        public SignalMissingException(String message) {
            super(message);
        }
    }
}
