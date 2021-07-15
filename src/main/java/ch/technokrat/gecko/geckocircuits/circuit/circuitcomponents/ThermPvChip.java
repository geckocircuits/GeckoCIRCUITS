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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.PostCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalFixedPositionInvisible;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalTwoPortRelativeFixedDirection;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.AbstractLossCalculator;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.AbstractLossCalculatorFabric;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculationSplittable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// Waermequelle: Leit- und Schaltverluste von Leistungshalbleitern
// --> Messunsg von Strom und Spannung durch LK_D oder LK_S
// --> Ermittlung der Verluste ueber Datenblattwerte (parameter[] von LK_D und LK_S)
// --> Realisierung mittels einer signalgesteuerten Stromquelle
public final class ThermPvChip extends AbstractCircuitBlockInterface implements ComponentCoupable, CurrentMeasurable,
        HiddenSubCircuitable, PostCalculatable, DirectVoltageMeasurable {

    public static final AbstractTypeInfo TYPE_INFO  = new ThermalTypeInfo(ThermPvChip.class, "Pv", I18nKeys.THERMAL_LOSS_W);
    
    private static final double PARALLEL_RESISTANCE = 10000.0;
    private static final double EARTH_X = 0.5;
    private static final int EARTH_Y = 3;
    private static final int POLYGON_POINTS = 5;
    private static final double WIDTH = 1.2;
    private static final double HEIGHT = 0.6;
    private static final int DIAMETER = 4;
    
    
    // FLOW-Quelle und paralleler hochohmiger Innenwiderstand Rth:
    private final AbstractBlockInterface[] _qTH = new AbstractCircuitBlockInterface[2]; 
    private final ComponentCoupling _componentCoupling = new ComponentCoupling(1, this, new int[]{0});
    private AbstractCurrentSource _thFlow;
    private AbstractResistor _parallelRes;
    private AbstractCircuitBlockInterface _lossComponent;
    private AbstractLossCalculator _lossCalculator;
    
    private static final double DIAMETER_A = 0.8;
    private double _conductionLosses;
    private double _switchingLosses;
    
    ThermPvChip() {
        super();
        XIN.add(new TerminalFixedPositionInvisible(this, ThermAmbient.THERMAL_ZERO));
        //XIN.add(new TerminalTwoPortRelativeFixedDirection(this, -2, ComponentDirection.WEST_EAST));
        YOUT.add(new TerminalTwoPortRelativeFixedDirection(this, 2, ComponentDirection.WEST_EAST));        
        this.setzeSubcircuit();
    }

    @Override
    public Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        _thFlow.getIDStringDialog().setNameUnChecked(getStringID());
        return Arrays.asList(_qTH);
    }

    @Override
    public boolean includeParentInSimulation() {
        return false;
    }

    // beim Laden von Datei muessen die SubCircuit-IDstrings geladen und aktualisiert werden -->
    public void initialisiereSubcircuit() {
        _thFlow.getIDStringDialog().setNameUnChecked(getStringID());
        _parallelRes.getIDStringDialog().setRandomStringID();  // RTH
    }
    

    private void setzeSubcircuit() {                
        // FLOW-Quelle:
        _thFlow = (AbstractCurrentSource) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.TH_FLOW, this);
        _qTH[0] = _thFlow;

        _thFlow.setInputTerminal(0, XIN.get(0));
        _thFlow.setOutputTerminal(0, YOUT.get(0));

        _thFlow.sourceType.setValueWithoutUndo(CircuitSourceType.QUELLE_SIGNALGESTEUERT);

        //----------------
        // parallel zur FLOW-Quelle ein hochohmiger Innenwiderstand RTH:
        _parallelRes = (AbstractResistor) AbstractTypeInfo.fabricHiddenSub(CircuitTyp.TH_RTH, this);
        _qTH[1] = _parallelRes;
        _parallelRes._resistance.setValueWithoutUndo(PARALLEL_RESISTANCE);

        _parallelRes.setInputTerminal(0, XIN.get(0));
        _parallelRes.setOutputTerminal(0, YOUT.get(0));
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        // nachtraegliche Parametrisierung des Blocks durchfuehren -->        
        initialisiereSubcircuit();
    }
    
    private static final AbstractLossCalculator DUMMY_LOSS_CALC = new AbstractLossCalculator() {        

        @Override
        public void calcLosses(double current, double temperature, double deltaT) {
            // nothing todo... dummy class
            // do nothing... just return 0 Watts as output power!
        }

        @Override
        public double getTotalLosses() {
            return 0;
        }
    };

    @Override
    public void doInitialization() {
        _lossCalculator = DUMMY_LOSS_CALC;
        _lossComponent = (AbstractCircuitBlockInterface) getComponentCoupling()._coupledElements[0];
        if (_lossComponent == null) {
            return;
        }

        if (_lossComponent instanceof LossCalculatable) {
            final AbstractLossCalculatorFabric verluste = ((LossCalculatable) _lossComponent).getVerlustBerechnung();
            _lossCalculator = verluste.lossCalculatorFabric();
        }        
    }

    @Override
    public void doCalculation(final double deltaT, final double time) {
        // Junction-Temperatur --> Temperaturdifferenz an Rth, wobei Bezugspunkt das Null-Niveau ist ('TH_NULLBEZUG_KNOTEN')
        final double temperature = -_parallelRes.parameter[2];  
        if(_lossComponent == null) {
            return;
        }
        
        _lossCalculator.calcLosses(_lossComponent._currentInAmps, temperature, deltaT);
        
        _currentInAmps = _lossCalculator.getTotalLosses();        
        
        if(_lossCalculator instanceof LossCalculationSplittable) {            
            _conductionLosses = ((LossCalculationSplittable) _lossCalculator).getConductionLoss();            
            _switchingLosses = ((LossCalculationSplittable) _lossCalculator).getSwitchingLoss();
        }
        
        _thFlow.parameter[1] = _currentInAmps;  // Verluste repraesentieren Waermestrom [W/m2] in der gesteuerten FLOW-Quelle        
    }

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawLine((int) (dpix * (- WIDTH - DIAMETER_A)), 0, (int) (dpix * (WIDTH + DIAMETER_A)), 0);
        graphics.drawLine((int) (dpix * (- WIDTH - DIAMETER_A)), 0, (int) (dpix * (- WIDTH - DIAMETER_A)), (int) (dpix * HEIGHT));
    }

    @Override
    protected void drawBackground(final Graphics2D graphics) {
        final double dax = 0.5 * DIAMETER_A;
        graphics.fillPolygon(
                new int[]{(int) (-dpix * WIDTH), (int) (dpix * WIDTH), (int) (dpix * (+ WIDTH + dax)), 
                    (int) (dpix * WIDTH), (int) (-dpix * WIDTH)},
                new int[]{(int) (dpix * HEIGHT), (int) (dpix * HEIGHT), 0, 
                    (int) (-dpix * HEIGHT), (int) (-dpix * HEIGHT)}, POLYGON_POINTS); 
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        final double dax = 0.5 * DIAMETER_A;
        graphics.drawPolygon(
                new int[]{(int) (-dpix * WIDTH), (int) (dpix * WIDTH), (int) (dpix * (WIDTH + dax)), 
                    (int) (dpix * WIDTH), (int) (-dpix * WIDTH)},
                new int[]{(int) (dpix * (0 + HEIGHT)), (int) (dpix * HEIGHT), 0, 
                    (int) (-dpix * HEIGHT), (int) (-dpix * HEIGHT)}, POLYGON_POINTS);
        graphics.drawString("Loss", (int) (-dpix * WIDTH + DIAMETER), (int) (graphics.getFont().getSize() / 2 - 2));
        // 'Erde'  -->

        graphics.fillRect((int) (dpix * (- WIDTH - DIAMETER_A - EARTH_X)), 
                (int) (dpix * HEIGHT), (int) (dpix * (2 * EARTH_X)), EARTH_Y);
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        if (SchematischeEingabe2._thermDisplayMode.showParameter) {

            final AbstractCircuitBlockInterface coupledElement =
                    (AbstractCircuitBlockInterface) getComponentCoupling()._coupledElements[0];
            if (coupledElement == null) {                
                _textInfo.addErrorValue(I18nKeys.NOT_DEFINED.getTranslation());
            } else {
                _textInfo.addParameter(coupledElement.getStringID());
            }
        }
    }

    @Override
    public void setComponentDirection(final ComponentDirection orientierung) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }
    

    @Override
    public ComponentCoupling getComponentCoupling() {
        return _componentCoupling;
    }

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.SELECT_LOSS_COMPONENT;
    }    

    @Override
    public void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {

        if (testObject instanceof AbstractResistor && ((AbstractResistor) testObject).getSimulationDomain() != ConnectorType.LK) {
            return;
        }

        if (testObject instanceof LossCalculatable) {
            insertList.add((AbstractBlockInterface) testObject);
        }
    }

    @Override
    public Window openDialogWindow() {
        return new ThermPvChipDialog(this);
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_LOSS_COMPONENT_DEFINED_IN_CIRCUIT_SHEET;
    }

    @Override
    public boolean equalsPossibleSubComponent(final Object toCompare) {
        if(toCompare.equals(this)) {
            return true;
        }
        if(toCompare.equals(_thFlow)) {
            return true;
        }
        return false;
    }
    
    public boolean isSplittableLossCalculation() {
        if(_lossCalculator == null) {
            return false;
        }    
        return _lossCalculator instanceof LossCalculationSplittable;
    }
    
    public double getTotalLosses() {
        return _currentInAmps;
    }
    
    public double getConductionLosses() {
        return _conductionLosses;
    }
    
    public double getSwitchngLosses() {
        return _switchingLosses;
    }

    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override 
    public List<OperationInterface> getOperationEnumInterfaces() {
        return getComponentCoupling().getOperationInterfaces();
    }
    
}
