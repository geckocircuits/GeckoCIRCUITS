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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalRelativePosition;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class OperationalAmplifier extends AbstractCircuitBlockInterface implements HiddenSubCircuitable {

    public static final AbstractTypeInfo TYPE_INFO =
            new CircuitTypeInfo(OperationalAmplifier.class, "OP-AMP", I18nKeys.OPERATIONAL_AMPLIFIER);
    
    UserParameter<Double> inputResistance = UserParameter.Builder.
            <Double>start("inputResistance", 90000.0).
            longName(I18nKeys.INPUT_RESISTANCE).
            shortName("rIN").
            unit("ohm").
            arrayIndex(this, 0).
            build();
    UserParameter<Double> outputResistance = UserParameter.Builder.
            <Double>start("outputResistance", 0.12).
            longName(I18nKeys.OUTPUT_RESISTANCE).
            shortName("rOUT").
            unit("ohm").
            arrayIndex(this, 1).
            build();
    final UserParameter<Double> frequencyDependency = UserParameter.Builder.
            <Double>start("frequencyDependency", 1E5).
            longName(I18nKeys.FREQUENCY_DEPENDENCY).
            shortName("fp").
            addAlternativeShortName("fb").
            showInTextInfo(TextInfoType.SHOW_NON_NULL).
            unit("1/(ohmF)").
            arrayIndex(this, 4).
            build();
    UserParameter<Double> gainValue = UserParameter.Builder.
            <Double>start("gainValue", 10000.0).
            longName(I18nKeys.GAIN).
            shortName("gain").
            unit("V/V").
            arrayIndex(this, 5).
            build();
    UserParameter<Double> voltageLimitationMax = UserParameter.Builder.
            <Double>start("voltageLimitMax.", 12.0).
            longName(I18nKeys.VOLTAGE_LIMITATION_MAXIMUM).
            shortName("U_max").
            unit("V").
            arrayIndex(this, 7).
            build();
    UserParameter<Double> voltageLimitationMin = UserParameter.Builder.
            <Double>start("voltageLimitatMin.", -12.0).
            longName(I18nKeys.VOLTAGE_LIMITATION_MINIMUM).
            shortName("U_min").
            unit("V").
            arrayIndex(this, 8).
            build();
    UserParameter<Double> voltageDividerRa = UserParameter.Builder.
            <Double>start("voltageDividerRa", 1.0).
            longName(I18nKeys.VOLTAGE_DIVIDER).
            shortName("Ra").
            unit("ohm").
            arrayIndex(this, 13).
            build();
    UserParameter<Double> voltageDividerRb = UserParameter.Builder.
            <Double>start("voltageDividerRb", 100.0).
            longName(I18nKeys.VOLTAGE_DIVIDER).
            shortName("Rb").
            unit("ohm").
            arrayIndex(this, 14).
            build();
    private AbstractBlockInterface[] _qLK;
    private final AbstractTerminal intern0, intern1, intern2, intern3, intern4, intern5;
    private static final double R_ISOLATION = 10e6;  // isolation, defined as high resistance
    private static final double R_F = 1;  // set always to one, define time-constant Tp=Rp*Cp via Cp
    private final AbstractVoltageSource _internalVoltageSource;
    private final AbstractResistor _rIN;
    private final AbstractVoltageSource _outputVoltageSource;
    private final AbstractCapacitor _outputCapacitor;
    private final AbstractResistor _rOUT;
    private final AbstractResistor _rIsolation;
    private final AbstractResistor _Rf;
    private final AbstractResistor _Ra;
    private final AbstractResistor _Rb;

    public OperationalAmplifier() {
        super();
        XIN.add(new TerminalRelativePosition(this, 1, 2));
        XIN.add(new TerminalRelativePosition(this, -1, 2));

        YOUT.add(new TerminalRelativePosition(this, 0, -3));
        YOUT.add(new TerminalRelativePosition(this, -2, -1));

        intern0 = new TerminalRelativePosition(this, -1, -1);
        intern1 = new TerminalRelativePosition(this, -1, 0);
        intern2 = new TerminalRelativePosition(this, -1, 1);
        intern3 = new TerminalRelativePosition(this, 0, -1);
        intern4 = new TerminalRelativePosition(this, 0, 0);
        intern5 = new TerminalRelativePosition(this, 0, 1);

        setComponentDirection(ComponentDirection.WEST_EAST);
        _rIN = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _rIN.getIDStringDialog().setRandomStringID();

        _rOUT = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _rOUT.getIDStringDialog().setRandomStringID();

        _rIsolation = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _rIsolation.getIDStringDialog().setRandomStringID();

        _Rf = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _Rf._resistance.setValueWithoutUndo(R_F);
        _Rf.getIDStringDialog().setRandomStringID();

        _Ra = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _Ra.getIDStringDialog().setRandomStringID();

        _Rb = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_R, this);
        _Rb.getIDStringDialog().setRandomStringID();

        _internalVoltageSource = (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);
        _internalVoltageSource.getIDStringDialog().setRandomStringID();
        _outputCapacitor = (AbstractCapacitor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_C, this);
        _outputCapacitor.getIDStringDialog().setRandomStringID();

        _outputVoltageSource = (AbstractVoltageSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.LK_U, this);
        _outputVoltageSource.getIDStringDialog().setRandomStringID();
        _outputVoltageSource._lowerLimit.setValueWithoutUndo(-Double.MAX_VALUE / 2);
        _outputVoltageSource._upperLimit.setValueWithoutUndo(Double.MAX_VALUE / 2);
        this.setzeSubcircuit();
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii);
        DatenSpeicher.appendAsString(ascii.append("\nnewFormat160"), true);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        if (!tokenMap.containsToken("newFormat160")) { // for backwards compatibility...
            setComponentDirection(ComponentDirection.WEST_EAST);
        }

        if (!tokenMap.containsToken("frequencyDependency")) { // old file format, version <= 1.62
            frequencyDependency.setValueWithoutUndo(1.0 / frequencyDependency.getValue());
        }
    }

    private void initPar() {
        _rIN._resistance.setValueWithoutUndo(inputResistance.getValue());
        _rOUT._resistance.setValueWithoutUndo(outputResistance.getValue());
        _rIsolation._resistance.setValueWithoutUndo(R_ISOLATION);
        _internalVoltageSource.directPotentialGain.setValueWithoutUndo(gainValue.getValue());
        _internalVoltageSource._lowerLimit.setValueWithoutUndo(voltageLimitationMin.getValue());
        _internalVoltageSource._upperLimit.setValueWithoutUndo(voltageLimitationMax.getValue());
        double freqValue = frequencyDependency.getValue();
        if (freqValue != 0) {
            _outputCapacitor._capacitance.setValueWithoutUndo(1.0 / freqValue);
        }
        _Ra._resistance.setValueWithoutUndo(voltageDividerRa.getValue());
        _Rb._resistance.setValueWithoutUndo(voltageDividerRb.getValue());
    }

    // Initialisiereung nach INIT&START --> 
    public void setzeParameterZustandswerteAufNULL() {
        this.initPar();
        _outputCapacitor.parameter[1] = 0;
        _outputCapacitor.parameter[2] = 0;
        _outputCapacitor.parameter[3] = 0;
    }

    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        initPar();
        return Arrays.asList(_qLK);
    }

    private void setzeSubcircuit() {
        _qLK = new AbstractBlockInterface[9];
        _qLK[0] = _rIN;
        // Rout -->
        _qLK[1] = _rOUT;
        // Riso, internal isolation -->
        _qLK[2] = _rIsolation;
        _qLK[3] = _internalVoltageSource;
        //
        // R14 (voltage-divider for voltage-clamping) -->
        _qLK[4] = _Ra;
        // R15 (voltage-divider for voltage-clamping) -->
        _qLK[5] = _Rb;
        // Rf (defining pole) -->
        _qLK[6] = _Rf;
        // Cp (defining pole) -->
        _qLK[7] = _outputCapacitor;
        // internal voltage-source at output side -->        
        _qLK[8] = _outputVoltageSource;

        _rIN.setInputTerminal(0, XIN.get(0)); // RIN
        _rIN.setOutputTerminal(0, XIN.get(1));

        _rOUT.setInputTerminal(0, intern3); // ROUT
        _rOUT.setOutputTerminal(0, YOUT.get(0));

        _rIsolation.setInputTerminal(0, XIN.get(1)); // Riso
        _rIsolation.setOutputTerminal(0, YOUT.get(1));

        _internalVoltageSource.setInputTerminal(0, intern0); // Ux = gain * Uin
        _internalVoltageSource.setOutputTerminal(0, XIN.get(1));

        _Ra.setInputTerminal(0, intern0); // R14
        _Ra.setOutputTerminal(0, intern1);

        _Rb.setInputTerminal(0, intern1); // Rp
        _Rb.setOutputTerminal(0, XIN.get(1));

        _Rf.setInputTerminal(0, intern1); // Cp
        _Rf.setOutputTerminal(0, intern2);

        _outputCapacitor.setInputTerminal(0, intern2);
        _outputCapacitor.setOutputTerminal(0, XIN.get(1));

        _outputVoltageSource.setInputTerminal(0, intern3);
        _outputVoltageSource.setOutputTerminal(0, YOUT.get(1));

        for (AbstractBlockInterface elem : _qLK) {
            elem.getIDStringDialog().setRandomStringID();
        }

        _internalVoltageSource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
        _internalVoltageSource.getComponentCoupling().setNewCouplingElement(0, _rIN);
        _outputVoltageSource.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
        _outputVoltageSource.getComponentCoupling().setNewCouplingElement(0, _outputCapacitor);
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawLine((int) (-dpix), (int) (-dpix * 2), (int) (-dpix), (int) (-dpix));
        graphics.drawLine((int) (dpix), (int) (-dpix * 2), (int) (dpix), (int) (-dpix));
        graphics.drawLine(0, 0, (int) (dpix * 0), (int) (dpix * 3));
        graphics.drawLine(0, (int) (dpix), -(int) (dpix * 2), (int) (dpix * 1));
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        graphics.fillPolygon(new int[]{(int) (-dpix * 1.5), (int) (dpix * 1.5), 0},
                new int[]{(int) (-dpix * 1), (int) (-dpix * 1), (int) (dpix * 2)}, 3);
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        graphics.drawPolygon(new int[]{(int) (-dpix * 1.5), (int) (dpix * 1.5), 0},
                new int[]{(int) (-dpix * 1), (int) (-dpix * 1), (int) (dpix * 2)}, 3);

        int ss = 3;  // pix
        // "+", "-":
        int xp = (int) (dpix * (+0.5));
        int yp = (int) (dpix * (-0.5));
        int xm = (int) (dpix * (-0.5));
        int ym = (int) (dpix * (-0.5));
        graphics.drawLine(xp - ss, yp, xp + ss, yp);
        graphics.drawLine(xp, yp - ss, xp, yp + ss);
        graphics.drawLine(xm, ym - ss, xm, ym + ss);

    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        if (!SchematischeEingabe2._lkDisplayMode.showParameter) {
            return;
        }

        if ((voltageLimitationMin.getNameOpt().isEmpty() && voltageLimitationMax.getNameOpt().isEmpty())) {
            _textInfo.addParameter("u=[" + voltageLimitationMin.getValue() + ".." + voltageLimitationMax.getValue() + "]");
        }
    }

    @Override
    public boolean includeParentInSimulation() {
        return false;
    }

    @Override
    protected final Window openDialogWindow() {
        return new OperationalAmplifierDialog(this);
    }
    
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return AbstractCircuitBlockInterface.getCalculatorsFromSubComponents(this);        
    }
}
