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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.control.ControlTypeInfo;
import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractSwitch;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SemiconductorLossCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ThermAmbient;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossProperties;
import ch.technokrat.gecko.geckocircuits.control.*;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;
import ch.technokrat.modelviewcontrol.ModelMVC;

public abstract class AbstractBlockInterface extends AbstractCircuitSheetComponent
        implements ComponentTerminable {

    public List<UserParameter<? extends Object>> registeredParameters = new ArrayList<UserParameter<? extends Object>>();
    private ComponentDirection orientationBeforeMove = ComponentDirection.NORTH_SOUTH;
    private ch.technokrat.gecko.geckocircuits.control.Point _sheetPosBeforeMove = new ch.technokrat.gecko.geckocircuits.control.Point(0, 0);
    /**
     * I use "lazy initialization" for the idstring, since the string is built
     * from the "type number", and the type is not available within the
     * constructor.
     */
    private IDStringDialog idStringDialog;
    private Point _sheetPosition = new Point(0, 0);
    protected String[] parameterString = new String[]{"", "", "0"};
    public final double[] parameter = new double[40];
    public final Stack<AbstractTerminal> XIN = new Stack<AbstractTerminal>();
    public final Stack<AbstractTerminal> YOUT = new Stack<AbstractTerminal>();
    private AffineTransform _origTransform;
    private ModelMVC<ComponentDirection> _componentDirection = new ModelMVC<ComponentDirection>(ComponentDirection.NORTH_SOUTH, "Rotation/orientation of sheet component");
    public final String[] nameOpt = new String[40]; // GeckoOPTIMIZER --> names of variable parameters
    public final SchematicTextInfo _textInfo = new SchematicTextInfo(this);
    public final Set<ComponentCoupling> _isReferencedBy = new HashSet<ComponentCoupling>() {
        @Override
        public boolean add(ComponentCoupling e) {
            boolean returnValue = super.add(e);
            doReferenceAddAction(e);
            return returnValue;
        }

        @Override
        public boolean remove(Object o) {
            assert o instanceof ComponentCoupling;
            boolean returnValue = super.remove(o);
            doReferenceRemoveAction((ComponentCoupling) o);
            return returnValue;
        }
    };

    @Override
    public final ConnectorType getSimulationDomain() {
        return getTypeInfo().getSimulationDomain();
    }

    public AbstractBlockInterface() {
        for (int i = 0; i < nameOpt.length; i++) {
            nameOpt[i] = "";
        }
    }

    public final String getFixedIDString() {
        return getTypeInfo()._fixedIDString;
    }

    public final I18nKeys getTypeDescription() {
        return getTypeInfo()._typeDescription;
    }

    public void doReferenceRemoveAction(final ComponentCoupling removed) {
    }

    public void doReferenceAddAction(final ComponentCoupling added) {
    }

    public final void setAccessibleParameter(final String paramname, final double paramValue) throws IllegalAccessException {
        for (UserParameter par : getRegisteredParameters()) {
            if (paramname.equalsIgnoreCase(par.getShortName())) {                
                par.setFromDoubleValue(paramValue);
                return;
            }
        }

        for (UserParameter par : getRegisteredParameters()) {
            if (paramname.equalsIgnoreCase(par.getAlternativeShortName())) {
                par.setFromDoubleValue(paramValue);
                // no return here! Search all possible names.
            }
        }
        
        if(paramname.equalsIgnoreCase("enabled")) { // this is only for forward compatibility.
            // the next release > 1.70 will have the same command, but implemented in a clean way!
            int intValue = (int) paramValue;
            if(intValue == 0) {
                setCircuitEnabled(Enabled.DISABLED);
                return;
            }
            if(intValue == 1) {
                setCircuitEnabled(Enabled.ENABLED);
                return;
            }
            if(intValue == 2) {
                setCircuitEnabled(Enabled.DISABLED_SHORT);
                return;
            }
            
        }

        throw new IllegalAccessException("Parameter with name " + paramname + " not found in " + getStringID() + ".");
    }

    public final double getAccessibleParameterValue(final String paramname) throws IllegalAccessException {

        for (UserParameter par : getRegisteredParameters()) {
            if (paramname.equalsIgnoreCase(par.getShortName())) {
                return par.getDoubleValue();

            }
        }

        for (UserParameter par : getRegisteredParameters()) {
            if (paramname.equalsIgnoreCase(par.getAlternativeShortName())) {
                return par.getDoubleValue();
            }
        }

        throw new IllegalAccessException("Parameter with name " + paramname + " not found in " + getStringID() + ".");
    }

    public final String[] getAccessibleParameterDescriptionVerbose() {
        final List<String> returnList = new ArrayList<String>();
        for (UserParameter par : getRegisteredParameters()) {
            returnList.add(par.getLongName());
        }
        return returnList.toArray(new String[returnList.size()]);
    }

    public final String[] getAccessibleParamterDescription() {
        final List<String> returnList = new ArrayList<String>();
        for (UserParameter par : getRegisteredParameters()) {
            returnList.add(par.getShortName());
        }
        return returnList.toArray(new String[returnList.size()]);
    }

    public final String[] getUnits() {
        final List<String> returnList = new ArrayList<String>();
        for (UserParameter par : getRegisteredParameters()) {
            returnList.add(par.getUnit());
        }
        return returnList.toArray(new String[returnList.size()]);
    }

    public Color getForeGroundColor() {
        return getSimulationDomain().getForeGroundColor();
    }

    public Color getBackgroundColor() {
        return getSimulationDomain().getBackgroundColor();
    }

    public void setDummyIDStringDialog() {
        idStringDialog = IDStringDialog.fabricDummyObjectWithoutEffect(this);
    }

    public IDStringDialog getIDStringDialog() {
        if (idStringDialog == null) {
            idStringDialog = IDStringDialog.fabricVariableName(this, getFixedIDString() + ".1");
        }
        return idStringDialog;
    }

    @Override
    public AbstractCircuitSheetComponent copyFabric(final long shiftValue) {
        final AbstractBlockInterface returnValue = getTypeInfo().fabric();
        returnValue.getIdentifier().createNewIdentifier(this.getUniqueObjectIdentifier() + shiftValue);
        if (this instanceof ComponentCoupable) {
            final ComponentCoupling oldCoupling = ((ComponentCoupable) this).getComponentCoupling();
            ((ComponentCoupable) returnValue).getComponentCoupling().shiftCopyCouplingIDsFrom(oldCoupling, shiftValue);
        }

        returnValue._parentCircuitSheet = this._parentCircuitSheet;
        this.copyLKBlockPars(returnValue);
        returnValue._isEnabled.setValueWithoutUndo(_isEnabled.getValue());
        return returnValue;
    }

    abstract public ElementDisplayProperties getDisplayProperties();

    public void setNewNameChecked(final String newName) throws NameAlreadyExistsException {

        getIDStringDialog().setNewNameChecked(newName);
    }

    public void setNewNameCheckedUndoable(final String newName) throws NameAlreadyExistsException {
        getIDStringDialog().setNewNameCheckedUndoable(newName);
    }

    private void readNameFromFile(final TokenMap tokenMap) {

        getIDStringDialog().setRandomStringID();
        try {
            final String nameFromFile = tokenMap.readDataLine("idStringDialog", "");
            setNewNameChecked(nameFromFile);
        } catch (NameAlreadyExistsException ex) {
            try {
                final String oldName = getStringID();
                setNewNameChecked(oldName);
            } catch (NameAlreadyExistsException ex1) {
                try {
                    final String oldName = getStringID();
                    setNewNameChecked(IDStringDialog.findUnusedName(oldName));
                    Logger.getLogger(AbstractBlockInterface.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (NameAlreadyExistsException ex2) {
                    Logger.getLogger(AbstractBlockInterface.class.getName()).log(Level.SEVERE, null, ex2);
                }
            }
            System.out.println(ex.getMessage() + " renamed to: " + getStringID());
        }

    }

    @Override
    public void importASCII(final TokenMap tokenMap) {
        if (this instanceof SemiconductorLossCalculatable) {
            TokenMap subBlock = tokenMap.getBlockTokenMap("<Verluste>");
            if (subBlock != null) {
                LossProperties verluste = (LossProperties) ((SemiconductorLossCalculatable) this).getVerlustBerechnung();
                verluste.importASCII(subBlock);  // Laden der korrekten Parameter
            }
        }

        readNameFromFile(tokenMap);

        int x = tokenMap.readDataLine("x", 5);
        int y = tokenMap.readDataLine("y", 5);
        setSheetPositionWithoutUndo(new Point(x, y));

        parameterString = tokenMap.readDataLine("parameterString[]", parameterString);
        List<Double> parameterTmp = tokenMap.readDataLineDoubleArray("parameter[]");
        for (int i = 0; i < parameterTmp.size(); i++) {
            parameter[i] = parameterTmp.get(i);
        }


        if (tokenMap.containsToken("nameOpt[]")) {
            String[] nameOptNew = tokenMap.readDataLine("nameOpt[]", nameOpt);
            // this step is for backwards-compatibility. E.g. thermal components
            // did not include parameters before release 1.63.
            System.arraycopy(nameOptNew, 0, nameOpt, 0, Math.min(nameOptNew.length, nameOpt.length));
        }

        if (tokenMap.containsToken("orientierung")) {
            setComponentDirection(ComponentDirection.getFromCode(
                    tokenMap.readDataLine("orientierung", getComponentDirection().code())));
        }

        String[] labelAnfangsKnoten = tokenMap.readDataLine("labelAnfangsKnoten[]", new String[0]);

        if (this instanceof VariableTerminalNumber) {
            ((VariableTerminalNumber) this).setInputTerminalNumber(labelAnfangsKnoten.length);
        }

        if (this instanceof ControlInputTwoTerminalStateable) {
            ControlInputTwoTerminalStateable inputStateable = (ControlInputTwoTerminalStateable) this;
            inputStateable.setFolded();
            if (XIN.size() < labelAnfangsKnoten.length) {
                inputStateable.setExpanded();
            }
        }


        for (int i = 0; i < Math.min(labelAnfangsKnoten.length, XIN.size()); i++) {
            XIN.get(i).getLabelObject().setLabelWithoutUndo(labelAnfangsKnoten[i]);
        }

        String[] labelEndKnoten = tokenMap.readDataLine("labelEndKnoten[]", new String[0]);

        if (this instanceof VariableTerminalNumber) {
            ((VariableTerminalNumber) this).setOutputTerminalNumber(labelEndKnoten.length);
        }

        for (int i = 0; i < Math.min(labelEndKnoten.length, YOUT.size()); i++) {
            YOUT.get(i).getLabelObject().setLabelWithoutUndo(labelEndKnoten[i]);
        }


        setPositionVorVerschieben(getSheetPosition());
        setOrientationBeforeMove(getComponentDirection());


        super.importASCII(tokenMap);

        _textInfo.importASCII(tokenMap);

        if (this instanceof ComponentCoupable) {
            ((ComponentCoupable) this).getComponentCoupling().importASCII(tokenMap);
        }


        for (UserParameter par : getRegisteredParameters()) {
            par.readFromParameterArray(parameter);
            par.readFromNameOptArray(nameOpt);
            par.readFromTokenMap(tokenMap);
        }

        this.importIndividual(tokenMap);
        if (this instanceof AbstractCircuitBlockInterface) {
            ((AbstractCircuitBlockInterface) this).setzeParameterZustandswerteAufNULL();
        }

        if (this instanceof RegelBlock) {
            setParameterWithoutUnDo(getParameter());
        }

    }

    public void checkNameOptParameter() {
        for (UserParameter par : getRegisteredParameters()) {
            if (!par.getNameOpt().isEmpty()) {
                double number = Fenster.optimizerParameterData.getNumberFromName(par.getNameOpt());
            }

        }
    }

    /*
     * overwrite this mehtod for additional parameters to load! (Template method pattern)
     *
     */
    protected void importIndividual(final TokenMap tokenMap) {
    }

    public final String getStringID() {
        return getIDStringDialog().toString();
    }

    protected void addTextInfoParameters() {
    }

    @Override
    public void deleteActionIndividual() {
        //remove all extra files (loss files, extra java files, nonlinearity files, etc.)
        LossProperties lossDescription = null;
        if (this instanceof AbstractSwitch) {
            lossDescription = ((AbstractSwitch) (this)).getVerlustBerechnung();
        } else if (this instanceof Diode) {
            lossDescription = ((Diode) (this)).getVerlustBerechnung();
        }
        if (lossDescription != null) {
            lossDescription._lossCalculationDetailed.removeLossFile();
        }


        getIDStringDialog().deleteIDString();

        for (ComponentCoupling coup : _isReferencedBy.toArray(new ComponentCoupling[0])) {
            coup.elementDeleted(this);
        }

        if (this instanceof HiddenSubCircuitable) {
            HiddenSubCircuitable hidden = (HiddenSubCircuitable) this;
            for (AbstractBlockInterface subComponent : hidden.getHiddenSubCircuitElements()) {
                subComponent.deleteActionIndividual();
            }

        }

        if (this instanceof ComponentCoupable) {
            ComponentCoupling coup = ((ComponentCoupable) this).getComponentCoupling();
            for (int i = 0; i < coup._coupledIdentifiers.length; i++) {
                coup.setNewCouplingElementInvisibleUndoable(i, null);
            }
        }
    }

    @Override
    public final void exportASCII(final StringBuffer ascii) {
        final String saveIdentifierStartString = "<" + getTypeInfo().getSaveIdentifier() + ">";
        ascii.append(saveIdentifierStartString);

        String[] labelAnfangsKnoten = new String[XIN.size()];
        for (int i = 0; i < labelAnfangsKnoten.length; i++) {
            labelAnfangsKnoten[i] = XIN.get(i).getLabelObject().getLabelString();
        }

        String[] labelEndKnoten = new String[YOUT.size()];
        for (int i = 0; i < labelEndKnoten.length; i++) {
            labelEndKnoten[i] = YOUT.get(i).getLabelObject().getLabelString();
        }


        DatenSpeicher.appendAsString(ascii.append("\nlabelAnfangsKnoten"), labelAnfangsKnoten);
        DatenSpeicher.appendAsString(ascii.append("\nlabelEndKnoten"), labelEndKnoten);
        super.exportASCII(ascii);
        DatenSpeicher.appendAsString(ascii.append("\ntyp"), getTypeEnum().getTypeNumber());
        getIdentifier().exportASCII(ascii);

        DatenSpeicher.appendAsString(ascii.append("\nx"), getSheetPosition().x);
        DatenSpeicher.appendAsString(ascii.append("\ny"), getSheetPosition().y);
        DatenSpeicher.appendAsString(ascii.append("\nparameter"), parameter);

        DatenSpeicher.appendAsString(ascii.append("\nparameterString"), parameterString);
        updateNameOptArray();
        DatenSpeicher.appendAsString(ascii.append("\nnameOpt"), nameOpt);

        DatenSpeicher.appendAsString(ascii.append("\norientierung"), getComponentDirection().code());
        DatenSpeicher.appendAsString(ascii.append("\nidStringDialog"), getStringID());

        if (this instanceof ComponentCoupable) {
            ((ComponentCoupable) this).getComponentCoupling().exportASCII(ascii);
        }

        for (UserParameter par : getRegisteredParameters()) {
            par.writeXMLToFile(ascii);
        }

        exportAsciiIndividual(ascii);

        if (this instanceof SemiconductorLossCalculatable) {
            ((LossProperties) ((SemiconductorLossCalculatable) this).getVerlustBerechnung()).exportASCII(ascii);
        }

        _textInfo.exportASCII(ascii);
        // Daten der individuellen ReglerBloecke:
        ascii.append("\n");
        final String saveIdentifierEndString = "\n<\\" + getTypeInfo().getSaveIdentifier() + ">\n";
        ascii.append(saveIdentifierEndString);
    }

    public final void setParameter(final double[] blockParameter) {
        for (int i = 0; i < blockParameter.length; i++) {
            parameter[i] = blockParameter[i];
        }

        for (UserParameter par : getRegisteredParameters()) {
            par.readFromParameterArray(blockParameter);
        }
    }

    public final void setParameterWithoutUnDo(final double[] blockParameter) {
        for (int i = 0; i < blockParameter.length; i++) {
            parameter[i] = blockParameter[i];
        }

        for (UserParameter par : getRegisteredParameters()) {
            par.readFromParamterArrayWithoutUndo(blockParameter);
        }
    }

   
    public final double[] getParameter() {
        List<UserParameter<? extends Object>> parameterList = getRegisteredParameters();
        if (!parameterList.isEmpty()) {
            for (UserParameter par : parameterList) {
                par.writeToParamterArray(parameter);
            }
        }
        return this.parameter;
    }

    public void updateNameOptArray() {
        List<UserParameter<? extends Object>> parameterList = getRegisteredParameters();
        if (!parameterList.isEmpty()) {
            for (UserParameter par : parameterList) {
                par.writeNameOptArray(nameOpt);
            }
        }
    }

    public void restoreOrigTransformation(Graphics2D g) {
        g.setTransform(_origTransform);
    }

    public void setTranslationRotation(Graphics2D g) {
        _origTransform = g.getTransform();

        g.translate(getSheetPosition().x * dpix, getSheetPosition().y * dpix);
        switch (getComponentDirection()) {
            case NORTH_SOUTH:
                break;
            case EAST_WEST:
                g.rotate(Math.PI / 2);
                break;
            case SOUTH_NORTH:
                g.rotate(Math.PI);
                break;
            case WEST_EAST:
                g.rotate(Math.PI * 3.0 / 2);
                break;
            default:
                assert false;
        }
    }

    public String[] getParameterString() {
        return parameterString;
    }

    // GeckoOPTIMIZER --> 
    public final String[] getNameOpt() {
        return nameOpt;
    }

    public abstract int istAngeklickt(final int mouseX, final int mouseY);

    public final void setInputTerminal(final int termIndex, final AbstractTerminal terminal) {
        XIN.set(termIndex, terminal);
    }

    public final void setOutputTerminal(final int termIndex, final AbstractTerminal terminal) {
        YOUT.set(termIndex, terminal);
    }

    public List<Verbindung> getShortConnectors() {
        final List<Verbindung> returnValue = new ArrayList<Verbindung>();

        for (int i = 0; i < Math.min(XIN.size(), YOUT.size()); i++) {
            final Verbindung verb = new VerbindungShortConnector(ConnectorType.LK, this.getParentCircuitSheet());
            Point startPoint = XIN.get(i).getPosition();
            Point stopPoint = YOUT.get(i).getPosition();

            verb.setzeStartKnoten(startPoint);

            int distX = startPoint.x - stopPoint.x;
            int distY = startPoint.y - stopPoint.y;
            int xPos = startPoint.x;
            int yPos = startPoint.y;

            if (distX != 0) {
                for (int j = 0; j <= Math.abs(distX); j++) {
                    verb.setzeAktuellenPunktAufVerbindung(new Point(xPos, yPos));
                    xPos += distX / distX;
                }
            }

            if (distY != 0) {
                for (int j = 0; j <= Math.abs(distY); j++) {
                    verb.setzeAktuellenPunktAufVerbindung(new Point(xPos, yPos));
                    yPos += distY / distY;
                }
            }

            verb.setzeEndKnoten(stopPoint.x, stopPoint.y);
            returnValue.add(verb);
        }
        return returnValue;
    }

    /**
     * return null if no terminal was clicked!
     *
     * @param px screen coordinates in (dpix-scaled!) pixel
     * @param py
     * @return
     */
    public TerminalInterface clickedTerminal(final Point clickPoint) {
        for (TerminalInterface testTerm : getAllTerminals()) {
            if (testTerm.getPosition().equals(clickPoint) && !(testTerm instanceof TerminalHiddenSubcircuit)) {
                return testTerm;
            }
        }
        return null;
    }

    @Override
    public void moveComponent(final Point moveToPoint) {
        if (this instanceof ThermAmbient) {
            // nix machen!
        } else {
            for (AbstractTerminal term : XIN) {
                term.getLabelObject().setLabelPriority(LabelPriority.LOW);
            }
            for (AbstractTerminal term : YOUT) {
                term.getLabelObject().setLabelPriority(LabelPriority.LOW);
            }
        }

        setSheetPositionWithoutUndo(new ch.technokrat.gecko.geckocircuits.control.Point(moveToPoint.x + _sheetPosBeforeMove.x,
                moveToPoint.y + _sheetPosBeforeMove.y));
    }

    @Override
    public void absetzenElement() {
        setPositionVorVerschieben(getSheetPosition());
        setOrientationBeforeMove(getComponentDirection());
        setModus(ComponentState.FINISHED);
    }

    @Override
    public Collection<? extends TerminalInterface> getAllTerminals() {
        List<TerminalInterface> returnValue = new ArrayList<TerminalInterface>();
        returnValue.addAll(XIN);
        returnValue.addAll(YOUT);
        return returnValue;
    }

    public void setPositionWithoutUndo(final int posX, final int posY) {
        setSheetPositionWithoutUndo(new ch.technokrat.gecko.geckocircuits.control.Point(posX, posY));
        this.absetzenElement();
    }

    @Override
    public final void setPositionWithUndo() {
        final Point oldPosition = _sheetPosBeforeMove;
        this.absetzenElement();
        final Point newPosition = _sheetPosition;
        if (!oldPosition.equals(newPosition)) {
            MoveComponentUndoEdit undoEdit = new MoveComponentUndoEdit(oldPosition, newPosition);
            AbstractUndoGenericModel.undoManager.addEdit(undoEdit);
        }
    }

    @Override
    public void deselectViaESCAPE() {
        setComponentDirection(getOrientationBeforeMove());
        setPositionWithoutUndo(getPositionVorVerschieben().x, getPositionVorVerschieben().y);
    }

    public void rotiereSymbol() {
        for (AbstractTerminal term : XIN) {
            term.getLabelObject().setLabelPriority(LabelPriority.LOW);
        }
        for (AbstractTerminal term : YOUT) {
            term.getLabelObject().setLabelPriority(LabelPriority.LOW);
        }
        setComponentDirection(getComponentDirection().nextOrientation());
        setModus(ComponentState.SELECTED);
    }

    @Override
    public Collection<String> getAllNodeLabels() {
        List<String> vecAllNodes = new ArrayList<String>();
        for (AbstractTerminal term : XIN) {
            if (!term.getLabelObject().getLabelString().isEmpty()) {
                vecAllNodes.add(term.getLabelObject().getLabelString());
            }
        }
        for (AbstractTerminal term : YOUT) {
            if (!term.getLabelObject().getLabelString().isEmpty()) {
                vecAllNodes.add(term.getLabelObject().getLabelString());
            }
        }
        return vecAllNodes;
    }

    public void copyLKBlockPars(final AbstractBlockInterface copy) {
        copy.setSheetPositionWithoutUndo(getSheetPosition());
        copy.setPositionVorVerschieben(getSheetPosition());
        System.arraycopy(parameter, 0, copy.parameter, 0, parameter.length);
        copy.parameterString = new String[this.parameterString.length];
        System.arraycopy(this.parameterString, 0, copy.parameterString, 0, this.parameterString.length);
        copy._textInfo.copyProperties(_textInfo);
        copy.setComponentDirection(this.getComponentDirection());
        copy.setParameter(copy.getParameter());

        copy.XIN.clear();
        for (AbstractTerminal term : XIN) {
            if (!(term instanceof TerminalSubCircuitBlock)) {
                copy.XIN.add(term.createCopy(copy));
            }
        }

        copy.YOUT.clear();
        for (AbstractTerminal term : YOUT) {
            copy.YOUT.add(term.createCopy(copy));
        }

        System.arraycopy(this.nameOpt, 0, copy.nameOpt, 0, this.nameOpt.length);



        if (this instanceof SemiconductorLossCalculatable) {
            LossProperties origLosses = (LossProperties) ((SemiconductorLossCalculatable) this).getVerlustBerechnung();
            ((LossProperties) ((SemiconductorLossCalculatable) copy).getVerlustBerechnung()).copyPropertiesFrom(origLosses);
        }


        for (UserParameter par : getRegisteredParameters()) {
            for (UserParameter insertCopySearch : copy.getRegisteredParameters()) {
                if (insertCopySearch.getSaveIdentifier().equals(par.getSaveIdentifier())
                        && insertCopySearch.getValue().getClass().equals(par.getValue().getClass())) {
                    insertCopySearch.setValueWithoutUndo(par.getValue());
                    if (!par.getNameOpt().isEmpty()) {
                        insertCopySearch.setNameOpt(par.getNameOpt());
                    }
                }
            }
        }

        copy.copyAdditionalParameters(this);
    }

    @Override
    public void paintComponentForeGround(final Graphics2D graphics) {
        graphics.setFont(SchematischeEingabe2.circuitFont);
        graphics.setColor(getForeGroundColor());

        for (AbstractTerminal term : XIN) {
            if (term.getCircuitSheet() == _parentCircuitSheet) {
                term.paintLabelString(graphics);
                //if(term instanceof TerminalControl) {
                //    ((TerminalControl) term).paintControlState(graphics);
                //}
            }
        }

        for (AbstractTerminal term : YOUT) {
            if (term.getCircuitSheet() == _parentCircuitSheet) {
                term.paintLabelString(graphics);
                //if(term instanceof TerminalControl) {
                //    ((TerminalControl) term).paintControlState(graphics);
                //}
            }
        }

        _textInfo.paint(graphics);

        paintShortCircuitConnections(graphics);


    }

    @Override
    public void paintGeckoComponent(final Graphics2D graphics) {
        graphics.setFont(SchematischeEingabe2.circuitFont);
        graphics.setColor(getForeGroundColor());

        _textInfo.clearParameters();

        if (getDisplayProperties().showName || (this instanceof SpecialNameVisible
                && ((SpecialNameVisible) this).isNameVisible())) {  // falls zusaetzlich auch der Name angezeigt werden soll            
            _textInfo.addParameter(getStringID());
        }

        this.addTextInfoParameters();
        _textInfo.addParameters(getRegisteredParameters(), getDisplayProperties());

        _textInfo.zeichneLinie(graphics, getDisplayProperties().showTextLine);

        for (TerminalInterface term : getAllTerminals()) {
            if (term.getCircuitSheet() == _parentCircuitSheet) {
                term.paintComponent(graphics);
            }
        }

        paintIndividualComponent(graphics);
    }

    /**
     * @return the orientationBeforeMove
     */
    public ComponentDirection getOrientationBeforeMove() {
        return orientationBeforeMove;
    }

    /**
     * @param orientationBeforeMove the orientationBeforeMove to set
     */
    public void setOrientationBeforeMove(final ComponentDirection orientationBeforeMove) {
        this.orientationBeforeMove = orientationBeforeMove;
    }

    @Override
    public int elementAngeklickt(final Point clickPoint) {
        return istAngeklickt((int) (dpix * clickPoint.x),
                (int) (dpix * clickPoint.y));
    }

    @Override
    public boolean testDoDoubleClickAction(final Point clickPoint) {
        int clickTester = elementAngeklickt(clickPoint);
        if (clickTester > 0) {
            return true;
        }

        if (clickedTerminal(clickPoint) != null) {
            return true;
        }

        return false;
    }

    /**
     * @return the orientierung
     */
    public ComponentDirection getComponentDirection() {
        return _componentDirection.getValue();
    }

    public void setComponentDirection(final ComponentDirection orientation) {
        if (_componentDirection.getValue() != orientation) {
            _componentDirection.setValueWithoutUndo(orientation);
        }
    }

    /**
     * @param orientation the orientierung to set
     */
    public void setComponentDirectionUndo() {
        final ComponentDirection newValue = _componentDirection.getValue();
        if (newValue != orientationBeforeMove) {
            _componentDirection.setValueWithoutUndo(orientationBeforeMove);
            _componentDirection.setValue(newValue);
        }
    }

    /**
     * @return the positionVorVerschieben
     */
    public ch.technokrat.gecko.geckocircuits.control.Point getPositionVorVerschieben() {
        return _sheetPosBeforeMove;
    }

    /**
     * @param positionVorVerschieben the positionVorVerschieben to set
     */
    public void setPositionVorVerschieben(ch.technokrat.gecko.geckocircuits.control.Point positionVorVerschieben) {
        this._sheetPosBeforeMove = positionVorVerschieben;
    }

    /**
     * @return the sheetPosition
     */
    public Point getSheetPosition() {
        return _sheetPosition;
    }

    private Point getCheckedSheetPosition(final Point sheetPosition) {
        Point checkedSheetPosition = sheetPosition;
        if (checkedSheetPosition.x < 1) {
            checkedSheetPosition = new Point(1, checkedSheetPosition.y);
        }

        if (checkedSheetPosition.y < 1) {
            checkedSheetPosition = new Point(checkedSheetPosition.x, 1);
        }

        if (getParentCircuitSheet() == null) {
            return checkedSheetPosition;
        }

        int maxX = getParentCircuitSheet()._worksheetSize.getSizeX() - 1;
        int maxY = getParentCircuitSheet()._worksheetSize.getSizeY() - 1;

        if (checkedSheetPosition.x > maxX) {
            checkedSheetPosition = new Point(maxX, checkedSheetPosition.y);
        }

        if (checkedSheetPosition.y > maxY) {
            checkedSheetPosition = new Point(checkedSheetPosition.x, maxY);
        }
        return checkedSheetPosition;
    }

    /**
     * @param sheetPosition the sheetPosition to set
     */
    public void setSheetPositionWithoutUndo(final Point sheetPosition) {
        if (_sheetPosition == sheetPosition) {
            return;
        }
        _sheetPosition = getCheckedSheetPosition(sheetPosition);
    }

    public final boolean elementTEXTAngeklickt(final int mx, final int my) {
        return _textInfo.isAngeklickt(mx, my);
    }

    protected void exportAsciiIndividual(final StringBuffer ascii) {
    }

    protected abstract void paintIndividualComponent(final Graphics2D graphics);

    /**
     * override this method, when additional parameters have to be copied!
     *
     * @param originalBlock
     */
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
    }

    @Override
    public final Collection<? extends Point> getAllDimensionPoints() {
        final List<Point> returnValue = new ArrayList<Point>();
        for (AbstractTerminal term : XIN) {
            returnValue.add(term.getPosition());
        }

        for (AbstractTerminal term : YOUT) {
            returnValue.add(term.getPosition());
        }
        returnValue.add(getSheetPosition());
        return returnValue;
    }

    public final void registerParameter(final UserParameter toRegister) {
        registeredParameters.add(toRegister);
    }

    public final void unregisterParameter(final UserParameter toUnRegister) {
        registeredParameters.remove(toUnRegister);
    }

    public final List<UserParameter<? extends Object>> getRegisteredParameters() {
        return Collections.unmodifiableList(registeredParameters);
    }

    /**
     * when components are short-circuited, we paint a bold line through the
     * component. Usually, this is an error, and the line helps the user to see
     * where he has short-components. However, do this only with two-port
     * components, but not with op-amps, transformers, ...
     *
     * @param graphics
     */
    private void paintShortCircuitConnections(final Graphics2D graphics) {
        if (this instanceof AbstractCircuitBlockInterface && (XIN.size() != 1 || YOUT.size() != 1)) {
            // no multi-port non-control-components!
            return;
        }
        final Stroke origStroke = graphics.getStroke();
        for (int i = 0; i < Math.min(XIN.size(), YOUT.size()); i++) {
            final boolean isLabelConnection = XIN.get(i).getLabelObject().getLabelString().equals(
                    YOUT.get(i).getLabelObject().getLabelString())
                    && !XIN.get(i).getLabelObject().getLabelString().isEmpty();
            if (_isEnabled.getValue() == Enabled.DISABLED_SHORT || isLabelConnection) {
                if (this instanceof ThermAmbient || this instanceof SubCircuitTerminable) {
                    continue;
                }
                final int x1pix = XIN.get(i).getPosition().x * dpix;
                final int y1pix = XIN.get(i).getPosition().y * dpix;
                final int x2pix = YOUT.get(i).getPosition().x * dpix;
                final int y2pix = YOUT.get(i).getPosition().y * dpix;
                graphics.setColor(getForeGroundColor());
                graphics.setStroke(new BasicStroke(3));
                graphics.drawLine(x1pix, y1pix, x2pix, y2pix);
                graphics.setStroke(origStroke);
            }

        }
    }

    @Override
    public void doDoubleClickAction(final Point clickedPoint) {
        final TerminalInterface clickedTerm = clickedTerminal(clickedPoint);
        if (clickedTerm != null && clickedTerm.getCircuitSheet() == SchematischeEingabe2.Singleton._visibleCircuitSheet) {
            final DialogLabelEingeben labelDialog = new DialogLabelEingeben(clickedTerm);
            labelDialog.setVisible(true);
            return;
        }
        final Window dialog = openDialogWindow();
        dialog.setVisible(true);
        if (dialog instanceof Frame) {
            Frame frame = (Frame) dialog;                        
            int state = frame.getExtendedState();
            state &= ~JFrame.ICONIFIED;
            frame.setExtendedState(state);            
        }
    }

    protected abstract Window openDialogWindow();

    @Override
    public String toString() {
        return getStringID();
    }

    public void setToolbarPaintProperties() {
    }

    public final I18nKeys getTypeDescriptionVerbose() {
        return getTypeInfo()._typeDescriptionVerbose;
    }

    /**
     *
     * @return
     */
    public final AbstractComponentTyp getTypeEnum() {
        return ControlTypeInfo.getTypeEnumFromClass(this.getClass());
    }

    public AbstractTypeInfo getTypeInfo() {
        return AbstractTypeInfo.getTypeInfoFromClass(this.getClass());
    }

    public void doOperationAfterNewConstruction() {
    }

    private final class MoveComponentUndoEdit implements UndoableEdit {

        private final Point _oldPosition;
        private final Point _newPosition;

        private MoveComponentUndoEdit(final Point oldPosition, final Point newPosition) {
            _oldPosition = oldPosition;
            _newPosition = newPosition;
        }

        @Override
        public void undo() throws CannotUndoException {
            _sheetPosition = _oldPosition;
            _sheetPosBeforeMove = _sheetPosition;
            absetzenElement();
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() throws CannotRedoException {
            _sheetPosition = _newPosition;
            _sheetPosBeforeMove = _sheetPosition;
            absetzenElement();
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            // nothing todo!
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Component moved";
        }

        @Override
        public String getUndoPresentationName() {
            return "Move component " + getIDStringDialog();
        }

        @Override
        public String getRedoPresentationName() {
            return "Move component " + getIDStringDialog();
        }
    }

    public boolean equalsPossibleSubComponent(final Object toCompare) {
        return this.equals(toCompare);
    }

    @Override
    public String getExportImportCharacters() {
        return getTypeInfo().getExportImportCharacters();
    }
        
}
