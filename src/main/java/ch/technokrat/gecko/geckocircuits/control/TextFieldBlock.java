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

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractSpecialBlock;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitSheet;
import ch.technokrat.gecko.geckocircuits.circuit.ElementDisplayProperties;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextFieldBlock extends AbstractSpecialBlock implements Operationable {

    public static final ControlTypeInfo tInfo = new ControlTypeInfo(TextFieldBlock.class, "TEXT", I18nKeys.TEXT_FIELD);
    
    private static final double DEFAULT_HEIGHT = 2;
    private static final double DEFAULT_WIDTH = 5;
    private static final int DEFAULT_COLOR_INT = 245;
    private static final int FONT_COLOR_INT = 80;
    private static final int TEXT_DX = 1;
    private static final int SHOW_POSX = 3;
    private static final int SHOW_POSY = 2;
    private static final Point SHOW_POS = new Point(SHOW_POSX, SHOW_POSY);    
    
    final UserParameter<String> _titleText = UserParameter.Builder.
            <String>start("title_string", "Title").
            longName(I18nKeys.TITLE_TEXT).
            shortName("title").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    final UserParameter<String> _contentsText = UserParameter.Builder.
            <String>start("contents_string", "").
            longName(I18nKeys.CONTENTS_TEXT).
            shortName("contents").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    final UserParameter<Double> _height = UserParameter.Builder.
            <Double>start("hoehe", DEFAULT_HEIGHT).
            longName(I18nKeys.X_BLOCK_DIMENSION).
            shortName("height").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    final UserParameter<Double> _width = UserParameter.Builder.
            <Double>start("box_breite", DEFAULT_WIDTH).
            longName(I18nKeys.Y_BLOCK_DIMENSION).
            shortName("width").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    final UserParameter<Color> _backGroundColor = UserParameter.Builder.
            <Color>start("back_color", new Color(DEFAULT_COLOR_INT, DEFAULT_COLOR_INT, DEFAULT_COLOR_INT)).
            longName(I18nKeys.BACKGROUND_COLOR).
            shortName("backColor").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    final UserParameter<Color> _fontColor = UserParameter.Builder.
            <Color>start("font_color", new Color(FONT_COLOR_INT, FONT_COLOR_INT, FONT_COLOR_INT)).
            longName(I18nKeys.FOREGROUND_COLOR).
            shortName("foColor").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    private int _xKlickMin, _yKlickMin, _xKlickMax, _yKlickMax;

    public TextFieldBlock() {
        super();
        _textInfo.setTextNeverVisible();
        _titleText.addActionListener(_repaintParentAction);
        _contentsText.addActionListener(_repaintParentAction);
        _height.addActionListener(_repaintParentAction);
        _width.addActionListener(_repaintParentAction);
        _backGroundColor.addActionListener(_repaintParentAction);
        _fontColor.addActionListener(_repaintParentAction);        
    }
    private final ActionListener _repaintParentAction = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            final CircuitSheet parent = getParentCircuitSheet();
            if (parent != null) {
                parent.repaint();
            }
        }
    };

    @Override
    protected void importIndividual(TokenMap tokenMap) {
        super.importIndividual(tokenMap);
        String oldstring = _contentsText.getValue();
        String newString = oldstring.replaceAll("\\\\n", "\n");
        _contentsText.setValueWithoutUndo(newString);
    }
    
    

    @Override
    public void setToolbarPaintProperties() {
        super.setToolbarPaintProperties();
        setSheetPositionWithoutUndo(SHOW_POS);
        _titleText.setValueWithoutUndo("Textfield");
        _contentsText.setValueWithoutUndo(" Textfields are\n useful for model\n documentation.");
        _height.setValueWithoutUndo(2.0);
    }   

    @Override
    public int[] getAussenabmessungenRechteckEckpunkte() {
        return new int[]{_xKlickMin, _yKlickMin, _xKlickMax, _yKlickMax};
    }

    @Override
    public Color getForeGroundColor() {
        return _fontColor.getValue();
    }                

    @Override
    public Color getBackgroundColor() {
        return _backGroundColor.getValue();
    }

    @Override
    public ElementDisplayProperties getDisplayProperties() {
        return SchematischeEingabe2._controlDisplayMode;
    }

    @Override
    public int istAngeklickt(final int mouseX, final int mouseY) {
        if (_xKlickMin < mouseX && mouseX < _xKlickMax
                && _yKlickMin - 1 < mouseY && mouseY < _yKlickMax) {
            return 1;
        }
        return 0;
    }

    @Override
    protected Window openDialogWindow() {
        return new TextFieldDialog(null, this);
    }

    //CHECKSTYLE:OFF
    @Override
    protected void paintIndividualComponent(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;
        final double height = _height.getValue();
        final double width = this._width.getValue();
        try {
            // Klickbereich:
            _xKlickMin = (int) (dpix * (xPos - width));
            _xKlickMax = (int) (dpix * (xPos - width + 1.333 * width));
            _yKlickMin = (int) (dpix * (yPos - height));
            _yKlickMax = (int) (dpix * (1 + yPos - height + 3.6 * height));

            final int rectPosX = (int) (dpix * (xPos - width));
            final int rectPosY = (int) (dpix * (yPos - height));
            final Color origColor = graphics.getColor();
            graphics.setColor(_backGroundColor.getValue());
            graphics.fillRect(rectPosX, rectPosY, (int) (dpix * (1.333 * width)), (int) (dpix * (3.6 * height)));
            graphics.setColor(_fontColor.getValue());
            graphics.drawRect(rectPosX, rectPosY, (int) (dpix * (1.333 * width)), (int) (dpix * (3.6 * height)));
            graphics.drawString(_titleText.getValue(), rectPosX + TEXT_DX, (int) (rectPosY - 0.2));
            String[] split = _contentsText.getValue().split("\\n");            
            int lineCounter = 0;
            for (String lineString : split) {
                final int stringPosY = lineCounter * (graphics.getFont().getSize()+1)
                        + (int) (rectPosY + dpix);
                final int stringPosX = rectPosX + TEXT_DX;                
                graphics.drawString(lineString, stringPosX, stringPosY);
                lineCounter++;
            }
            graphics.setColor(origColor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    //CHECKSTYLE:ON
    
    @Override
    public List<Operationable.OperationInterface> getOperationEnumInterfaces() {
        final List<Operationable.OperationInterface> returnValue = new ArrayList<Operationable.OperationInterface>();
        
        returnValue.add(new Operationable.OperationInterface("setTitle", I18nKeys.SET_TITLE) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                _titleText.setUserValue(parameterValue.toString());
                return null;
            }
        });
        
        returnValue.add(new Operationable.OperationInterface("getTitle", I18nKeys.GET_TITLE) {
            @Override            
            public Object doOperation(final Object parameterValue) {                
                return _titleText.getValue();                
            }
        });
        
        returnValue.add(new Operationable.OperationInterface("setText", I18nKeys.SET_TEXT) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                _contentsText.setUserValue(parameterValue.toString());
                return null;
            }
        });
        
        returnValue.add(new Operationable.OperationInterface("getText", I18nKeys.GET_TEXT) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                return _contentsText.getValue();                
            }
        });
        
        return Collections.unmodifiableList(returnValue);
    }
    
}
