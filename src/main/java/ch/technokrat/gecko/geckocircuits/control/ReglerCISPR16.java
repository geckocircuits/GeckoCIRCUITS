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

import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;

public final class ReglerCISPR16 extends RegelBlock implements SpecialNameVisible,
        Operationable {

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerCISPR16.class, "CISPR16", I18nKeys.EMI_TEST_RECEIVER);
    // alle ZV-Daten nicht komprimiert fuer eventuelle Festplattenspeicherung --> Speicherkritisch
    DataContainerSimple _zvDatenRam;
    public final Cispr16Settings _settings = new Cispr16Settings(this);
    private TestReceiverWindow _testReceiverCISPR16;
    /**
     * todo: I don't know a better name for "DA" - this comes from Uwe. Maybe it
     * means "Distanz Aussen"
     */
    private static final double DA_OFFSET = 0.5;
    private static final int DI_OFFSET = 3;  // Innen-Rechteck
    private static final double WIDTH = 1.5;
    private static final int DATA_INDEX_ADD = 5;
    TestReceiverCalculation _testReceiverNew;

    public Cispr16Settings getSettings() {
        return _settings;
    }

    public ReglerCISPR16() {
        super(1, 0);
        try {
            _testReceiverCISPR16 = new TestReceiverWindow(this);
        } catch(Throwable ex) {            
            ex.printStackTrace();
        }
        
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        _testReceiverCISPR16.exportAscii(ascii);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        _testReceiverCISPR16.importAscii(tokenMap);        
    }            

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {
        final List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.add(new OperationInterface("startCalculation", I18nKeys.START_CALCULATION_DOC) {
            @Override                        
            public Object doOperation(final Object parameterValue) {
                _testReceiverCISPR16.startCalculation(false);
                return null;
            }
        });

        returnValue.add(new OperationInterface("saveFourierData", I18nKeys.SAVE_DATA_DOC) {
            @Override            
            public Object doOperation(final Object parameterValue) {                
                _testReceiverCISPR16.saveFourierDataToFile(new File((String) parameterValue));
                return null;
            }
        });
        
        returnValue.add(new OperationInterface("savePeakData", I18nKeys.SAVE_DATA_DOC) {
            @Override            
            public Object doOperation(final Object parameterValue) {                
                _testReceiverCISPR16.savePeakDataToFile(new File((String) parameterValue));
                return null;
            }
        });
        
        returnValue.add(new OperationInterface("saveQuasiPeakData", I18nKeys.SAVE_DATA_DOC) {
            @Override            
            public Object doOperation(final Object parameterValue) {                
                _testReceiverCISPR16.saveQuasiPeakDataToFile(new File((String) parameterValue));
                return null;
            }
        });
        
        returnValue.add(new OperationInterface("saveAveragePeakData", I18nKeys.SAVE_DATA_DOC) {
            @Override            
            public Object doOperation(final Object parameterValue) {                
                _testReceiverCISPR16.saveAverageDataToFile(new File((String) parameterValue));
                return null;
            }
        });

        returnValue.add(new OperationInterface("abortCalculation", I18nKeys.ABORT_SIMULATION) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                _testReceiverCISPR16.abortCalculation();
                return null;
            }
        });
        
        returnValue.add(new OperationInterface("calculateQpAtFrequency", I18nKeys.GET_QUASI_PEAK_AT_FREQ) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                if (!(parameterValue instanceof Number)) {
                    throw new IllegalArgumentException("parameter type must be numeric!");
                }
                checkForValidTestReceiver();
                return _testReceiverNew.calculateQpAtFrequency(((Number) parameterValue).doubleValue());
            }
        });
        returnValue.add(new OperationInterface("calculatePeakAtFrequency", I18nKeys.GET_PEAK_AT_FREQ) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                if (!(parameterValue instanceof Number)) {
                    throw new IllegalArgumentException("parameter type must be numeric!");
                }

                checkForValidTestReceiver();
                return _testReceiverNew.calculatePeakAtFrequency(((Number) parameterValue).doubleValue());
            }
        });

        returnValue.add(new OperationInterface("calculateAvgAtFrequency", I18nKeys.GET_AVG_AT_FREQ) {
            @Override                     
            public Object doOperation(final Object parameterValue) {
                if (!(parameterValue instanceof Number)) {
                    throw new IllegalArgumentException("parameter type must be numeric!");
                }

                checkForValidTestReceiver();
                return _testReceiverNew.calculateAvgAtFrequency(((Number) parameterValue).doubleValue());
            }
        });        
                
        return Collections.unmodifiableList(returnValue);
    }
         

    private void checkForValidTestReceiver() {
        if (_testReceiverNew == null) {
            try {
            _testReceiverNew = new TestReceiverCalculation(_zvDatenRam, _settings);
            } catch (Throwable error) {
                error.printStackTrace();
            }
        }
    }

    private class CisprCalculator extends AbstractControlCalculatable implements MemoryInitializable {

        public CisprCalculator() {
            super(1, 0);
        }

        
        private float[] dataValue = new float[1];
        
        @Override
        public void berechneYOUT(final double deltaT) {
            // be careful, this is complicated by intention! If we would just insert
            // the  inputSignal[0], it would be overwritten in the next simulation step.            
            dataValue[0] = (float) _inputSignal[0][0];
            _zvDatenRam.setContainerStatus(ContainerStatus.RUNNING);
            _zvDatenRam.insertValuesAtEnd(dataValue, _time);

        }

        @Override
        public void doInit(final double deltaT) {
            try {
                _zvDatenRam = DataContainerSimple.fabricConstantDtTimeSeries(1, (int) ((SimulationsKern.tEND - SimulationsKern.tSTART) / deltaT)
                        + DATA_INDEX_ADD);
            } catch (java.lang.OutOfMemoryError err) {
                _zvDatenRam = null;
                JOptionPane.showMessageDialog(null,
                        "Could not allocate enough memory for EMI calculation block!",
                        "Memory error!",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new CisprCalculator();
    }

    public AbstractDataContainer getZVDatenImRAM() {
        assert _zvDatenRam != null;
        return _zvDatenRam;
    }

    public void loescheZVDatenImRAM() {
        _zvDatenRam = null;
    }

    @Override
    public void deleteActionIndividual() {
        super.deleteActionIndividual();
        loescheZVDatenImRAM();
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;

        // Klickbereich SCOPE-Symbol:
        xKlickMin = (int) (dpix * (xPos - WIDTH));
        xKlickMax = (int) (dpix * (xPos + DA_OFFSET));
        yKlickMin = (int) (dpix * (yPos - DA_OFFSET));
        yKlickMax = (int) (dpix * (yPos + WIDTH));

        Color origColor = graphics.getColor();

        graphics.setColor(getBackgroundColor());

        graphics.fillRect((int) (dpix * (xPos - WIDTH)), (int) (dpix * (yPos - DA_OFFSET)), (int) (dpix * (WIDTH + DA_OFFSET)), (int) (dpix * (WIDTH + DA_OFFSET)));
        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (xPos - WIDTH)), (int) (dpix * (yPos - DA_OFFSET)), (int) (dpix * (WIDTH + DA_OFFSET)), (int) (dpix * (WIDTH + DA_OFFSET)));
        graphics.drawRect((int) (dpix * (xPos - WIDTH)) + DI_OFFSET, (int) (dpix * (yPos - DA_OFFSET)) + DI_OFFSET, (int) (dpix * (WIDTH + DA_OFFSET)) - 2 * DI_OFFSET, (int) (dpix * (WIDTH + DA_OFFSET)) - 2 * DI_OFFSET);
        //
        int a1 = DI_OFFSET + 4, a2 = DI_OFFSET + 2;
        graphics.drawLine((int) (dpix * (xPos - WIDTH) + a2), (int) (dpix * (yPos + WIDTH) - a1), (int) (dpix * (xPos + DA_OFFSET) - a2), (int) (dpix * (yPos + WIDTH) - a1));  // waagrechte Achse
        graphics.drawLine((int) (dpix * (xPos - WIDTH) + a1), (int) (dpix * (yPos - DA_OFFSET) + a2), (int) (dpix * (xPos - WIDTH) + a1), (int) (dpix * (yPos + WIDTH) - a2));  // senkrechte Achse
        // Stuetzpunkte der Class A/B-Kurven: 
        final int dx = (int) (dpix * (WIDTH + DA_OFFSET) - a1 - a2), dy = dx;
        final int xk1 = (int) (dpix * (xPos - WIDTH) + a1 + 2);
        final int xk2 = (int) (dpix * (xPos - WIDTH) + a1 + 2 + dx / 3);
        final int xk3 = (int) (dpix * (xPos - WIDTH) + a1 + 2 + 2 * dx / 3);
        final int xk4 = (int) (dpix * (xPos + DA_OFFSET) - a1);
        final int yk1 = (int) (dpix * (yPos - DA_OFFSET) + a1 + 2);
        final int yk2 = (int) (dpix * (yPos - DA_OFFSET) + a1 + 2 + 0.2 * dy);
        final int yk3 = (int) (dpix * (yPos - DA_OFFSET) + a1 + 2 + 0.4 * dy);
        final int yk4 = (int) (dpix * (yPos - DA_OFFSET) + a1 + 2 + 0.6 * dy);
        graphics.drawPolyline(new int[]{xk1, xk2, xk2, xk4}, new int[]{yk1, yk1, yk2, yk2}, 4);
        graphics.drawPolyline(new int[]{xk1, xk2, xk3, xk3, xk4}, new int[]{yk3, yk4, yk4, yk3, yk3}, 5);

    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    public void setTestReceiverCISPR16MenueEnabled(final boolean value) {
        _testReceiverCISPR16.setTestReceiverCISPR16MenueEnabled(value);
        if (value) {
            _zvDatenRam.setContainerStatus(ContainerStatus.FINISHED);
        }

    }

    @Override
    public boolean isNameVisible() {
        return _settings._showName.getValue();
    }

    @Override
    public void setNameVisible(final boolean newValue) {
        _settings._showName.setUserValue(newValue);
    }

    @Override
    protected final Window openDialogWindow() {
        return _testReceiverCISPR16;
    }
}
