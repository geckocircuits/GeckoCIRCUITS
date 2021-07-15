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
import ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.HiddenSubCircuitable;
import ch.technokrat.gecko.geckocircuits.circuit.PostCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.PotentialCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractMotor extends AbstractCircuitBlockInterface implements PotentialCoupable, HiddenSubCircuitable, PostCalculatable {    
    
    private static final double SIXTY_OVER_TWOPI = 60.0 / (2 * Math.PI);
    static final double RADIUS_MOTOR_SYMBOL = 1.0, BLOCK_SYMB_X = 1.3,
            BLOCK_SYMB_Y = 0.3;            
    
    private final PotentialCoupling _potCoupling = new PotentialCoupling(this, new int[]{0}, ConnectorType.CONTROL);

    
        
    double _omegaElectric, _thetaElectric;    
    public double _omegaMechanic, _omegaMechanicOld, _thetaMechanic, _thetaMechanicOld;    
    private double _polePairs, _drehzahl, _torqueEl;    
    private double _torqueMech = 0; // wird vorgegeben (zB. als Signal) --> 
    
    final UserParameter<Double> _polePairsParameter = UserParameter.Builder.
            <Double>start("polePairs", 1.0).
            longName(I18nKeys.POLE_PAIRS).
            shortName("p").
            unit("unitless").
            arrayIndex(this, getPolePairIndex()).
            build();
    
    private double _inertia;
    final UserParameter<Double> _inertiaParameter = UserParameter.Builder.
            <Double>start("inertia", 1.0).
            longName(I18nKeys.INERTIA).
            shortName("J").
            unit("Nms2").
            arrayIndex(this, getInertiaIndex()).
            build();
    
    final UserParameter<Double> _initialRotationalSpeed = UserParameter.Builder.
	<Double>start("initialRotationalSpeed", 0.0).
	longName(I18nKeys.INITIAL_ROTATION_SPEED).
	shortName("omega").
	unit("1/s").
	arrayIndex(this,getInitialRotationSpeedIndex()).
	build();
    
    private double _frictionMech;
    final UserParameter<Double> _frictionParameter = UserParameter.Builder.
	<Double>start("frictionCoefficient", 0.1).
	longName(I18nKeys.FRICTION_COEFFICIENT).
	shortName("fr").
	unit("Nms").
	arrayIndex(this,getFrictionCoefficientIndex()).
	build();         
    
    final UserParameter<Double> _initialRotorPosition = UserParameter.Builder.
	<Double>start("initialRotorPosition", 0.0).
	longName(I18nKeys.INITIAL_ROTOR_POSITION).
	shortName("theta").
	unit("rad").
	arrayIndex(this,getInitialRotorPositionIndex()).
	build();        
    
    private final List<AbstractBlockInterface> _qLK = new ArrayList<AbstractBlockInterface>();
    
    AbstractMotor() {
        super();
        initializeComponent();
    }
    
    final void initializeComponent() {
        setTerminals();
        setSubCircuit();
    }
    
    abstract void setTerminals();
    abstract void setSubCircuit();
    
    abstract int getInertiaIndex();
    abstract int getInitialRotationSpeedIndex();
    abstract int getFrictionCoefficientIndex(); 
    abstract int getInitialRotorPositionIndex();
    abstract int getElectricTorqueIndex();
    abstract void calculateMotorEquations(final double deltaT, final double time);
    abstract double calculateElectricTorque();
    public abstract int getIndexForLoadTorque();
    
    @Override
    public PotentialCoupling getPotentialCoupling() {
        return _potCoupling;
    }     

    @Override
    public void deleteActionIndividual() {
        super.deleteActionIndividual();
        for(ComponentCoupling coup : _isReferencedBy) {
            coup.elementDeleted(this);
        }
        
    }
        
    
    @Override
    public final boolean includeParentInSimulation() {
        return false;
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        if (!SchematischeEingabe2._lkDisplayMode.showParameter) {
            return;
        }

        final String torqueString = getPotentialCoupling().getLabels()[0];        
        if (torqueString.isEmpty()) {
            _textInfo.addParameter("no torque defined");            
        } else {
            _textInfo.addParameter(torqueString + " >>");            
        }                
    }            
    
    @Override
    public void doInitialization() {        
    }
    
    @Override
    public final Collection<AbstractBlockInterface> getHiddenSubCircuitElements() {
        return Collections.unmodifiableList(_qLK);
    }
    
    @Override
    protected final void drawBackground(final Graphics2D graphics) {
        graphics.fillOval((int) (-dpix * RADIUS_MOTOR_SYMBOL), (int) (-dpix * RADIUS_MOTOR_SYMBOL), (int) (dpix * 2 * RADIUS_MOTOR_SYMBOL), (int) (dpix * 2 * RADIUS_MOTOR_SYMBOL));        
    }
        
    @Override
    protected final void drawForeground(final Graphics2D graphics) {
        graphics.drawOval((int) (-dpix * RADIUS_MOTOR_SYMBOL), (int) (-dpix * RADIUS_MOTOR_SYMBOL), (int) (dpix * 2 * RADIUS_MOTOR_SYMBOL), (int) (dpix * 2 * RADIUS_MOTOR_SYMBOL));
        drawOnTop(graphics);
    }
    
    protected void drawOnTop(final Graphics2D graphics) {
        // extend if necessary.
    }
    
    public static AbstractCircuitBlockInterface fabricHiddenSub(final CircuitTyp typ, 
            final AbstractCircuitSheetComponent parent) {
        final AbstractCircuitBlockInterface returnValue = AbstractTypeInfo.fabricHiddenSub(typ, parent);
        assert parent instanceof AbstractMotor: "Invalid circuit type:  " + typ;
        final AbstractMotor parentMotor = (AbstractMotor) parent;
        returnValue.getIDStringDialog().setRandomStringID();
        parentMotor._qLK.add(returnValue);
        return returnValue;
    }
    
    @Override
    public final void doCalculation(final double deltaT, final double time) {
        _torqueMech = parameter[getIndexForLoadTorque()];
        calculateMotorEquations(deltaT, time);        
        calculateMechanicalParameters(deltaT);
        updateSourceParameters();
        updateOldSolverParameters();
        updateHistoryVariables();        
    }
    
    @Override
    public void setzeParameterZustandswerteAufNULL() {        
        for (int i1 = 0; i1 < getParameterStringIntern().size(); i1++) {
            parameter[i1] = 0;
        }
        
        _polePairs = _polePairsParameter.getValue();                
        _frictionMech = _frictionParameter.getValue();
        _inertia = _inertiaParameter.getValue();
        _omegaMechanicOld = _initialRotationalSpeed.getValue();
        _omegaMechanic = _omegaMechanicOld;
        _omegaElectric = _omegaMechanic * _polePairs;
        _thetaMechanicOld = _initialRotorPosition.getValue();
        _thetaMechanic = _thetaMechanicOld;
        _thetaElectric = _polePairs * _thetaMechanic;                
    }
    
    void updateHistoryVariables() {        
        _thetaMechanicOld = _thetaMechanic;
        _omegaMechanicOld = _omegaMechanic;
    }

    final void calculateMechanicalParameters(final double deltaT) {
        _torqueEl = _polePairs * calculateElectricTorque();
        _omegaMechanic = (_torqueEl - _torqueMech + _inertia * _omegaMechanicOld / deltaT) / (_inertia / deltaT + _frictionMech);                        
        _omegaElectric = _omegaMechanic * _polePairs;
        _drehzahl = SIXTY_OVER_TWOPI * _omegaMechanic;     
        _thetaMechanic = _thetaMechanicOld + _omegaMechanic * deltaT;
        _thetaElectric = _polePairs * _thetaMechanic;
    }
        
    void updateOldSolverParameters() {    
        parameter[getOmegaIndex()] = _omegaMechanic;
        parameter[getDrehzahlIndex()] = _drehzahl;
        parameter[getElectricTorqueIndex()] = _torqueEl;
        parameter[getThetaMIndex()] = _thetaMechanic;
    }

    @Override
    protected void importIndividual(TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        if(DatenSpeicher.readFileVersion < 170) { 
            // backwards compatibility: before version 1.70, the 
            // machines could not be rotated!
            setComponentDirection(ComponentDirection.NORTH_SOUTH);            
        }
    }
    
    
    
    abstract int getPolePairIndex();        
    abstract int getOmegaIndex();
    abstract int getThetaMIndex();
    abstract int getDrehzahlIndex();
    abstract void updateSourceParameters();
    
    @Override
    final public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return AbstractCircuitBlockInterface.getCalculatorsFromSubComponents(this);        
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {
        return _potCoupling.getOperationInterfaces();
    }
    
    
    
}
