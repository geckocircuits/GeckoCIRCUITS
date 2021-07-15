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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JOptionPane;

public final class OutputWarningStream extends BufferedOutputStream {

    private final PrintStream _ps;
    private boolean _verbosityWarnShown = false;
    private static final long DEFAULT_WARN_SIZE = 50000000;
    private static long warningBytesSize = DEFAULT_WARN_SIZE;
    private static long byteCounter = 0;
    private boolean _isOriginalOutput = true;
    @SuppressWarnings("PMD")
    private StringBuffer _alternativeOutput;
    private static final int MAX_STRING_BUFFER_SIZE = 100000;
    private static String outputDescription;
    private boolean _ignoreFutureMessages = false;
    private static final int BUFFER_FRACTION = 5; // this means, after cleaning the buffer, 1/5th of the original space is left
    private static final int SEARCH_NEWLINE_CHARS = 200;

    public OutputWarningStream(final OutputStream aStream, final PrintStream bufferedWriter) {
        super(aStream);
        _ps = bufferedWriter;
    }

    @Override
    public void write(final byte[] bytes) throws IOException {
        final String aString = new String(bytes);
        if (_isOriginalOutput) {
            _ps.append(aString);
        } else {
            _alternativeOutput.append(aString);
        }

        byteCounter += bytes.length;
        checkLineCount();
    }

    @Override
    public void write(final byte[] bytes, final int off, final int len) throws IOException {
        final String aString = new String(bytes, off, len);
        byteCounter += bytes.length;
        if (_isOriginalOutput) {
            _ps.append(aString);
        } else {
            _alternativeOutput.append(aString);
            checkStringBufferSize();
        }
        checkLineCount();
    }

    public void checkLineCount() {
        if (byteCounter > warningBytesSize) {
            Thread showWarningThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    maybeShowWarning();
                }
            }) {
            };
            showWarningThread.start();

        }
    }

    public void reset() {
        byteCounter = 0;
        _verbosityWarnShown = false;
    }

    private void maybeShowWarning() {
        if (!_verbosityWarnShown && !_ignoreFutureMessages) {
            _verbosityWarnShown = true;
            String destination = "Console output";
            if (!_isOriginalOutput) {
                destination = "Block: " + outputDescription + "  (Text field)";
            }

            final Object[] options = {"Ok",
                "Ignore further messages"};


            //Custom button text
            final int selection = JOptionPane.showOptionDialog(null,
                    "Excessive usage of output messages during simulation detected! This slows down\n"
                    + "your simulation. Please check your simulation model for errors, or consider to\nreduce the"
                    + "verbosity of your custom Java-code!\n\nSource of output message: " + outputDescription + "\n"
                    + "Destination: " + destination,
                    "Performance Warning!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (selection == 1) {
                _ignoreFutureMessages = true;
            }
        }
    }

    void setAlternativeOutput(final StringBuffer stringBuffer, final String description) {
        _isOriginalOutput = false;
        _alternativeOutput = stringBuffer;
        outputDescription = description;
    }

    void setOriginalOutput() {
        _isOriginalOutput = true;
    }

    private void checkStringBufferSize() {
        if (_alternativeOutput.length() > MAX_STRING_BUFFER_SIZE) {
            _alternativeOutput.delete(0, MAX_STRING_BUFFER_SIZE - MAX_STRING_BUFFER_SIZE / BUFFER_FRACTION);
            final int maxSearch = Math.min(_alternativeOutput.length(), SEARCH_NEWLINE_CHARS);
            final char[] searchForNewLine = new char[maxSearch];
            _alternativeOutput.getChars(0, maxSearch, searchForNewLine, 0);
            for (int index = 0; index < maxSearch; index++) {
                if (searchForNewLine[index] == '\n') {
                    _alternativeOutput.delete(0, index + 1);
                    break;
                }
            }
        }
    }

    void setConsoleOutput(final String description) {
        _isOriginalOutput = true;
        outputDescription = description;
    }
}
