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

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculationSimple;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossProperties;
import ch.technokrat.gecko.geckocircuits.control.Operationable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author andy diodes and switches are abstractSemiconductors.
 */
public abstract class AbstractSemiconductor extends AbstractTwoPortLKreisBlock implements SemiconductorLossCalculatable, CurrentMeasurable,
        DirectVoltageMeasurable, Operationable {

    public final UserParameter<Double> _onResistance = UserParameter.Builder.
            <Double>start("onResistance", AbstractSwitch.RD_ON_DEFAULT).
            longName(I18nKeys.ON_RESISTANCE).
            shortName("rON").
            unit("Ohm").
            arrayIndex(this, getOnResistanceIndex()).
            build();
    public final UserParameter<Double> _offResistance = UserParameter.Builder.
            <Double>start("offResistance", AbstractSwitch.RD_OFF_DEFAULT).
            longName(I18nKeys.OFF_RESISTANCE).
            shortName("rOFF").
            unit("Ohm").
            arrayIndex(this, getOffResistanceIndex()).
            build();
    public final UserParameter<Double> kOn = UserParameter.Builder.
            <Double>start("kON", 20e-6).
            longName(I18nKeys.CURRENT_DEPENDENT_COEFFICIENT_ON).
            shortName("k_on").
            arrayIndex(this, 6).
            unit("Ws/A").
            build();
    public final UserParameter<Double> kOff = UserParameter.Builder.
            <Double>start("kOFF", 30e-6).
            longName(I18nKeys.CURRENT_DEPENDENT_COEFFICIENT_OFF).
            shortName("k_off").
            unit("Ws/A").
            arrayIndex(this, 7).
            build();
    public final UserParameter<Integer> numberParalleled = UserParameter.Builder.
            <Integer>start("numberParalleled", 1).
            longName(I18nKeys.NUMBER_OF_DEVICES_PARALLEL).
            shortName("paralleled").
            unit("unitless").
            arrayIndex(this, 12).
            build();
    public final UserParameter<Double> uK = UserParameter.Builder.
            <Double>start("uSWnorm", LossCalculationSimple.UK_DEFAULT_VALUE).
            longName(I18nKeys.BLOCKING_VOLTAGE_FOR_SWITCHING).
            shortName("uK").
            arrayIndex(this, -1).
            unit("V").
            build();

    public AbstractSemiconductor() {                               
    }

    /**
     * this "hacks" are just for backwards-compatibility. In the old
     * GeckoCIRCUITS versions, the ideal switch has an on resistance parameter
     * index of 1, in all other switches it is 2. The same "shift" applies for
     * off-resistance.
     *
     * @return
     */
    int getOnResistanceIndex() {
        return 2;
    }

    /**
     * this "hacks" are just for backwards-compatibility. In the old
     * GeckoCIRCUITS versions, the ideal switch has an on resistance parameter
     * index of 1, in all other switches it is 2. The same "shift" applies for
     * off-resistance.
     *
     * @return
     */
    int getOffResistanceIndex() {
        return 3;
    }

    public void addFiles(List<GeckoFile> _newFilesToAdd) {
    }

    public List<GeckoFile> getFiles() {
        if (this instanceof LossCalculatable) {
            List<GeckoFile> returnValue = new ArrayList<GeckoFile>();
            GeckoFile lossFile = ((LossProperties) ((LossCalculatable) this).getVerlustBerechnung()).getDetailedLosses().lossFile;
            returnValue.add(lossFile);
            return returnValue;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public void removeLocalComponentFiles(List<GeckoFile> filesToRemove) {
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {
        List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.add(new OperationInterface("setLossFile", I18nKeys.SET_LOSS_FILE_DOC) {
            @Override
            public Object doOperation(final Object parameterValue) {
                if(!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Error: argument must be a String containing a file name");
                }
                File lossFile = new File((String) parameterValue);

                //if it doesn't exist, try first to see if it is in the same directory as the currently open model file
                if (!lossFile.exists()) {
                    final File modelFile = new File(Fenster.getOpenFileName());
                    final String currentModelDirectory = modelFile.getParent();
                    final String correctedFileName = currentModelDirectory + System.getProperty("file.separator") + parameterValue;
                    lossFile = new File(correctedFileName);
                }

                if (lossFile.exists() && !lossFile.isDirectory()) {
                    final String foundLossFileName = lossFile.getAbsolutePath();
                    if (foundLossFileName.endsWith(".scl")) {                                                
                        ((LossProperties) getVerlustBerechnung()).getDetailedLosses().readLossesFromFileAndSetDetailedLossType(foundLossFileName);                        
                    } else {                        
                        throw new RuntimeException("Invalid loss file " + foundLossFileName);
                    }
                } else {
                    throw new RuntimeException("Specified loss file: " + parameterValue + " does not exist.");
                }
                return null;
            }
        });

        return Collections.unmodifiableList(returnValue);
    }
}
