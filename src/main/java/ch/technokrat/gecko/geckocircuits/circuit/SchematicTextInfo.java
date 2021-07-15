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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.TechFormat;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Nonlinearable;
import ch.technokrat.gecko.geckocircuits.control.ControlInputTwoTerminalStateable;
import ch.technokrat.gecko.geckocircuits.control.Point;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;

/**
 *
 * @author andreas
 */
public final class SchematicTextInfo {

    private static final TechFormat tcf = new TechFormat();
    private int _xTxtKlickMin;
    private int _xTxtKlickMax;
    private int _yTxtKlickMin;
    private int _yTxtKlickMax;
    private Point _txtKlickPoint;  // zum Starten des relativen Text-Verschiebens wurd initial auf diesen Punkt geklickt
    private double _dxTxt = 2.0;
    private double _dyTxt = 2.0;
    private double _maxLengthText = 4.0;
    private double _lyTxt = 2.0;
    private final AbstractBlockInterface _element;
    private final List<PrintParameter> _printParameter = new ArrayList<PrintParameter>();
    // for some blocks, the textfield just does not make sense, e.g. textField Block, save date-Block...
    private boolean _neverVisible = false;
    private double _dyTxtBeforeMove;
    private double _dxTxtBeforeMove;
    private boolean _doShowParameters = true;

    public SchematicTextInfo(final AbstractBlockInterface element) {
        _element = element;
    }

    public void initPositionRelative(final double initDx, final double initDy) {
        _dxTxt = initDx;
        _dyTxt = initDy;
    }

    public void copyProperties(final SchematicTextInfo copyFrom) {
        _dxTxt = copyFrom._dxTxt;
        _dyTxt = copyFrom._dyTxt;

    }

    public boolean isAngeklickt(final int mouseX, final int mouseY) {
        return (_xTxtKlickMin <= mouseX) && (mouseX <= _xTxtKlickMax) && (_yTxtKlickMin <= mouseY) && (mouseY <= _yTxtKlickMax);
    }

    public void zeichneLinie(final Graphics graphics, final boolean showLine) {

        if (_neverVisible) {
            return;
        }

        final Color origColor = graphics.getColor();

        final int dpix = AbstractCircuitSheetComponent.dpix;

        final int xStart = _element.getSheetPosition().x;
        final int yStart = _element.getSheetPosition().y;

        final int xStop = (int) (_xTxtKlickMin + (_xTxtKlickMax - _xTxtKlickMin) / 3);
        final int yStop = _yTxtKlickMin + (_yTxtKlickMax - _yTxtKlickMin) / 3;


        if (showLine && !_printParameter.isEmpty()) {
            graphics.setColor(GlobalColors.farbeTextLinie);
            if (xStop != 0 && yStop != 0) {
                graphics.drawLine((int) (dpix * xStart), (int) (dpix * yStart), xStop, yStop);
            }
        }

        updateRanges(dpix, xStart, yStart);
        graphics.setColor(origColor);
    }

    public void importASCII(final TokenMap tokenMap) {
        _dxTxt = tokenMap.readDataLine("dxTxt", _dxTxt);
        _dyTxt = tokenMap.readDataLine("dyTxt", _dyTxt);       
        
        _dxTxtBeforeMove = _dxTxt;
        _dyTxtBeforeMove = _dyTxt;        
    }

    public void exportASCII(final StringBuffer ascii) {        
        DatenSpeicher.appendAsString(ascii.append("\ndxTxt"), _dxTxt);
        DatenSpeicher.appendAsString(ascii.append("\ndyTxt"), _dyTxt);
    }

    void absetzenText(final Point position) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        _dxTxt = (_txtKlickPoint.x - _element.getSheetPosition().x) + position.x * 1.0 / dpix - _txtKlickPoint.x;
        _dyTxt = (_txtKlickPoint.y - _element.getSheetPosition().y) + position.y * 1.0 / dpix - _txtKlickPoint.y;
        if(_dxTxt != _dxTxtBeforeMove && _dxTxt != _dxTxtBeforeMove) {
            final MoveTextFieldUndoAction undoAction = new MoveTextFieldUndoAction(_dxTxtBeforeMove, _dyTxtBeforeMove, _dxTxt, _dyTxt);
            AbstractUndoGenericModel.undoManager.addEdit(undoAction);
        }
        
        _dxTxtBeforeMove = _dxTxt;
        _dyTxtBeforeMove = _dyTxt;
    }
    
    public void setNewRelativePosition(final Point relPosition) {        
        final int dpix = AbstractCircuitSheetComponent.dpix;                
        _dxTxt = (_txtKlickPoint.x - _element.getSheetPosition().x) + relPosition.x * 1.0 / dpix - _txtKlickPoint.x;
        _dyTxt = (_txtKlickPoint.y - _element.getSheetPosition().y) + relPosition.y * 1.0 / dpix - _txtKlickPoint.y;        
        
    }        

    private void updateRanges(final int dpix, final int xPos, final int yPos) {
        _xTxtKlickMin = (int) (dpix * (xPos + _dxTxt));
        _xTxtKlickMax = (int) (dpix * (xPos + _dxTxt + _maxLengthText));
        _yTxtKlickMax = (int) (dpix * (yPos + _dyTxt));
        _yTxtKlickMax = (int) (dpix * (yPos + _dyTxt + _lyTxt));
    }
    
    public void setPositionTextClickPointInitial(final int xTxtK1, final int yTxtK1) {        
        _dxTxtBeforeMove = _dxTxt;
        _dyTxtBeforeMove = _dyTxt;
        _txtKlickPoint = new Point(xTxtK1, yTxtK1);
    }

    public void addErrorValue(final String errorText) {
        _printParameter.add(new ErrorParameter(errorText));
    }

    public void paint(final Graphics2D graphics) {        
        if (_neverVisible) {
            return;
        }                

        final int xPos = _element.getSheetPosition().x;
        final int yPos = _element.getSheetPosition().y;

        final FontRenderContext frc = graphics.getFontRenderContext();
        final int dpix = AbstractCircuitSheetComponent.dpix;
        if (SchematischeEingabe2._thermDisplayMode.showName) {  // falls zusaetzlich auch der Name angezeigt werden soll
            _yTxtKlickMin = _yTxtKlickMin - SchematischeEingabe2.DY_ZEILENABSTAND_TXT;
        }

        int counter = 0;
        final double textHeight = graphics.getFont().getSize() * 0.8;
        _yTxtKlickMin = (int) (dpix * (yPos + _dyTxt - textHeight / dpix));        
                
        for (PrintParameter par : _printParameter) {
            final String toDraw = par._parameterString;
            final int stringYPos = (int) (_yTxtKlickMin + textHeight + counter * graphics.getFont().getSize());
            par.drawString(graphics, toDraw, _xTxtKlickMin, stringYPos);            
            final double thisLength = graphics.getFont().getStringBounds(toDraw, frc).getWidth() * 1.0 / dpix;
            _maxLengthText = Math.max(_maxLengthText, thisLength);
            _lyTxt = Math.max(_lyTxt, stringYPos * 1.0 / dpix - (yPos + _dyTxt));
            counter++;
        }

        _yTxtKlickMax = (int) (dpix * (yPos + _dyTxt + _lyTxt));
        _xTxtKlickMax = (int) (dpix * (xPos + _dxTxt + _maxLengthText));

        final int rectHeight = _yTxtKlickMax - _yTxtKlickMin;
        final int rectWidth = _xTxtKlickMax - _xTxtKlickMin;
        //g.drawRect(_xTxtKlickMin, getYTxtKlickMin(), rectWidth, rectHeight);
    }

    public void clearParameters() {
        _printParameter.clear();
        _maxLengthText = 0.1;
        _lyTxt = 0.03;
    }

    public void addParameter(final String value) {
        _printParameter.add(new PrintParameter(value));
    }

    public void setTextNeverVisible() {
        _neverVisible = true;
    }

    public void addParameters(List<UserParameter<? extends Object>> parameters, ElementDisplayProperties properties) {
        if (parameters.isEmpty()) {
            return;
        }

        if (!_doShowParameters) {
            return;
        }

        for (UserParameter<? extends Object> par : parameters) {
            
            if(!par.isShowTypeInfoConditionFromEnum()) {
                return;
            }
            
            if(_element instanceof Nonlinearable) {
                UserParameter<Double> replacedParameter = ((Nonlinearable) _element).getNonlinearReplacedParameter();
                AbstractNonLinearCircuitComponent component = (AbstractNonLinearCircuitComponent) _element;
                if(par == replacedParameter && !par.getNameOpt().isEmpty() && component._isNonlinear.getValue()) {
                    continue; // don't print dollar parameters when nonlinear setting is used!
                }
            }
            final PrintParameterDollar dollarPrinter = PrintParameterDollar.fabric(par);
            if (dollarPrinter != null) {
                _printParameter.add(dollarPrinter);
            } else {
                switch (par.getTextInfoType()) {
                    case SHOW_WHEN_NON_EXTERNAL:
                        assert _element instanceof ControlInputTwoTerminalStateable;
                        if(((ControlInputTwoTerminalStateable) _element).isExternalSet()) {
                            break;
                        }
                    case SHOW_WHEN_DISPLAYPARAMETERS:
                        if (properties.showParameter) {
                            addUserParameter(par);
                        }
                        break;
                    case SHOW_ALWAYS:
                        addUserParameter(par);
                        break;
                    case SHOW_NON_NULL:
                        if (properties.showParameter) {
                            addNonNullUserParameter(par);
                        }
                    case SHOW_NEVER:
                        break;
                    default:
                        assert false;
                }
            }
        }
    }

    void setPositionDeselect() {        
        _dxTxt = _dxTxtBeforeMove;
        _dyTxt = _dyTxtBeforeMove;
    }

    private void addUserParameter(final UserParameter<? extends Object> userParameter) {        
        String uTxt = "";
        
        Object value = userParameter.getValue();
        if(value instanceof Enum<?>) {
            uTxt += userParameter.getShortName() + "= " + value.toString();
        } else {
            uTxt += userParameter.getShortName() + "= " + tcf.formatENG(userParameter.getDoubleValue(), 3);
        }
        
        addParameter(uTxt);
    }

    private void addNonNullUserParameter(final UserParameter<? extends Object> userParameter) {
        if (userParameter.getDoubleValue() != 0) {
            addUserParameter(userParameter);
        }
    }

    public void doShowParameters(final boolean value) {
        _doShowParameters = value;
    }
    

    public static class PrintParameter {

        final String _parameterString;

        public PrintParameter(final String value) {
            _parameterString = value;
        }

        void drawString(final Graphics2D graphics, final String toDraw, final int xPos, final int yPos) {
            graphics.drawString(toDraw, xPos, yPos);
        }
    }

    static class PrintParameterDollar extends PrintParameter {

        private static PrintParameterDollar fabric(final UserParameter<? extends Object> parameter) {
            if (parameter.getNameOpt().isEmpty()) {
                return null;
            }
            if ("non-accessible".equals(parameter.getShortName())) {
                return null;
            }
            return new PrintParameterDollar(parameter.getNameOpt() + "= " + parameter.getDoubleValue());
        }

        PrintParameterDollar(final String value) {
            super(value);
        }

        @Override
        void drawString(final Graphics2D graphics, final String toDraw, final int xPos, final int yPos) {
            final Color oldColor = graphics.getColor();
            graphics.setColor(GlobalColors.farbeOPT);
            super.drawString(graphics, toDraw, xPos, yPos);
            graphics.setColor(oldColor);
        }
    }

    static class ErrorParameter extends PrintParameter {

        public ErrorParameter(final String value) {
            super(value);
        }

        @Override
        void drawString(final Graphics2D graphics, final String toDraw, final int xPos, final int yPos) {
            final Color oldColor = graphics.getColor();
            graphics.setColor(Color.red);
            super.drawString(graphics, toDraw, xPos, yPos);
            graphics.setColor(oldColor);
        }
    }

    private final class MoveTextFieldUndoAction implements UndoableEdit {

        private final double _oldPosX;
        private final double _oldPosY;
        private final double _newPosX;
        private final double _newPosY;

        private MoveTextFieldUndoAction(final double oldPosX, final double oldPosY, final double newPosX, final double newPosY) {
            _oldPosX = oldPosX;
            _oldPosY = oldPosY;
            _newPosX = newPosX;
            _newPosY = newPosY;            
        }

        @Override
        public void undo() {
            _dxTxt = _oldPosX;
            _dyTxt = _oldPosY;
            _dxTxtBeforeMove = _oldPosX;
            _dyTxtBeforeMove = _oldPosY;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() {
            _dxTxt = _newPosX;
            _dyTxt = _newPosY;
            _dxTxtBeforeMove = _newPosX;
            _dyTxtBeforeMove = _newPosY;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            // nothing todo here
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Move text description";
        }

        @Override
        public String getUndoPresentationName() {
            return "Move text desciption of " + _element.getStringID();
        }

        @Override
        public String getRedoPresentationName() {
            return "Move text desciption of " + _element.getStringID();
        }
    }
}
