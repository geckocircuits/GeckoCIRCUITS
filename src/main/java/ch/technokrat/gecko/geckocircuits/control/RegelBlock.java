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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.*;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.javablock.ReglerJavaFunction;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public abstract class RegelBlock extends AbstractBlockInterface implements Serializable {

    public final static double[] EMPTY_OUTPUT = new double[]{};
    // Abmessungen der einzelnen Elemente:
    private static final double WIDTH = 1.5;
    protected int pFa = 11, pFb = 3;  // Symbol-Pfeil fuer Flussrichtung --> Pfeilspitzenabmessung
    protected int[] xFl = new int[3], yFl = new int[3];  // Symbol-Pfeil, der optional die Stromflussrichtung im Element anzeigt
    protected int xKlickMin, xKlickMax, yKlickMin, yKlickMax;  // Bereich zum Anklicken, Werte werden Reglelblock-spezifisch definiert
    static final TechFormat tcf = new TechFormat();
    public static final double SIGNAL_THRESHOLD = 0.5;
    static final int DISP_DIGITS = 3;
    
    protected ControlTyp _controlTyp;

    
    public AbstractControlCalculatable _calculator;
    private int _priority;

    RegelBlock() {
        // package-private constructor!
    }        

    protected RegelBlock(final int noInputs, final int noOutputs) {                
        if (this instanceof VariableTerminalNumber) {
            ((VariableTerminalNumber) this).setInputTerminalNumber(noInputs);
            ((VariableTerminalNumber) this).setOutputTerminalNumber(noOutputs);
        } else if (noInputs == 1 && noOutputs == 1) {
            setInputTerminalNumber(noInputs);
            setOutputTerminalNumber(noOutputs, 1);
        } else if (noInputs == 2 && noOutputs == 1) {
            setInputTerminalNumber(noInputs);
            setOutputTerminalNumber(noOutputs, 1);
        } else {
            setInputTerminalNumber(noInputs);
            setOutputTerminalNumber(noOutputs, 2);
        }

    }
    
    public int getPriority() {
        return _priority;
    }
    
    public void setPriority(int value) {
        _priority = value;
    }

    public abstract AbstractControlCalculatable getInternalControlCalculatableForSimulationStart();

    @Override
    public void doOperationAfterNewConstruction() {
        if (XIN.size() == 0 && YOUT.size() > 0) {
            _textInfo.initPositionRelative(-5, 0.4);
        }

        if (YOUT.size() == 0 && XIN.size() > 0) {
            _textInfo.initPositionRelative(2, 0.4);
        }
    }

    
    
    protected void setInputTerminalNumber(final int noInputs) {
        try {
            while (XIN.size() > noInputs) {
                XIN.pop();
            }

            while (XIN.size() < noInputs) {
                XIN.add(new TerminalControlInput(this, -2, -XIN.size()));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void setOutputTerminalNumber(int noOutputs, int xPos) {
        try {
            while (YOUT.size() > noOutputs) {
                YOUT.pop();
            }

            while (YOUT.size() < noOutputs) {
                YOUT.add(new TerminalControlOutput(this, xPos, -YOUT.size()));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }        

    public abstract String[] getOutputNames();

    public abstract I18nKeys[] getOutputDescription();

    public ArrayList<String[]> getOutputs() {
        String[] outputs = getOutputNames();
        I18nKeys[] outputkeys = getOutputDescription();
        String[] output_description = new String[outputkeys.length];
        for(int i = 0; i < outputkeys.length; i++) {
            output_description[i] = LangInit.getTranslatedString(outputkeys[i]);
        }

        if (outputs == null || outputs.length == 0) {
            return null;
        }

        ArrayList<String[]> outputList = new ArrayList<String[]>();
        String[] output;
        for (int i = 0; i < outputs.length; i++) {
            output = new String[2];
            output[0] = outputs[i];
            output[1] = output_description[i];
            //System.out.println(idStringDialog+output[0]+output[1]);
            outputList.add(output);
        }
        return outputList;
    }

    public final double getOutput(final String outputName) throws IllegalAccessException {
        String[] outputNames = getOutputNames();
        for (int i = 0; i < outputNames.length; i++) {
            if (outputNames[i].equals(outputName)) {
                try {
                    return _calculator._outputSignal[i][0];
                } catch (Exception ex) {
                    throw new NullPointerException("Error getting output: " + getStringID() + " " + outputName);
                }
            }
        }
        throw new IllegalAccessException("output " + outputName + " in block " + getStringID() + " does not exist");
    }

    public final double getOutput() throws IllegalAccessException {
        if (YOUT.size() > 1) {
            throw new IllegalAccessException(getStringID() + " has more than one output");
        }

        try {
            return _calculator._outputSignal[0][0];
        } catch (Throwable exc) {
            throw new IllegalAccessException(getStringID() + " has no output");
        }

    }

    // zum Ueberschreiben bei PI-Block und aehnlichen:
    public void initAtSimulationStart() {
    }
    

    /**
     * is overwritten in some components for additional functionality, e.g. the
     * scope or the java-block for changing the terminal numbers.
     *
     * @param mousePixelX
     * @param mousePixelY
     * @return
     */
    @Override
    public int istAngeklickt(final int mousePixelX, final int mousePixelY) {
        if (((xKlickMin <= mousePixelX) && (mousePixelX <= xKlickMax)
                && (yKlickMin <= mousePixelY) && (mousePixelY <= yKlickMax))) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public final int[] getAussenabmessungenRechteckEckpunkte() {
        return new int[]{xKlickMin, yKlickMin, xKlickMax, yKlickMax};
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        super.exportAsciiIndividual(ascii);
        boolean[] shiftLabelsIn = new boolean[XIN.size()];
        for (int i = 0; i < XIN.size(); i++) {
            shiftLabelsIn[i] = XIN.get(i).getHasDoubleValue();
        }
        DatenSpeicher.appendAsString(ascii.append("\nshiftLabelsIn"), shiftLabelsIn);

        boolean[] shiftLabelsOut = new boolean[YOUT.size()];
        for (int i = 0; i < YOUT.size(); i++) {
            shiftLabelsOut[i] = YOUT.get(i).getHasDoubleValue();
        }

        DatenSpeicher.appendAsString(ascii.append("\nshiftLabelsOut"), shiftLabelsOut);
    }

    @Override
    public final void importASCII(final TokenMap tokenMap) {
        super.importASCII(tokenMap);

        if (tokenMap.containsToken("shiftLabelsIn[]")) {
            boolean[] shiftLabelsIn = tokenMap.readDataLine("shiftLabelsIn[]", new boolean[0]);
            boolean[] shiftLabelsOut = tokenMap.readDataLine("shiftLabelsOut[]", new boolean[0]);

            for (int i = 0; i < Math.min(XIN.size(), shiftLabelsIn.length); i++) {
                XIN.get(i).setHasDoubleLabel(shiftLabelsIn[i]);
            }

            for (int i = 0; i < Math.min(YOUT.size(), shiftLabelsOut.length); i++) {
                if (YOUT.get(i) instanceof ControlTerminable) {
                    YOUT.get(i).setHasDoubleLabel(shiftLabelsOut[i]);
                }
            }
        }
    }
            
    public final void setTyp(final ControlTyp controTyp) {
        _controlTyp = controTyp;
    }            

    @Override
    public final void rotiereSymbol() {
        // control components cannot be rotated at the moment!
    }

    @Override
    protected abstract Window openDialogWindow();

    @Override
    public final Color getForeGroundColor() {
        return GlobalColors.farbeFertigElementCONTROL;
    }
    

    @Override
    public final void paintGeckoComponent(final Graphics2D g) {
        super.paintGeckoComponent(g);
        drawCenteredString(g);
    }

    @Override
    public void setComponentDirection(final ComponentDirection orientation) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }
    
    

    @Override
    protected void paintIndividualComponent(final Graphics2D graphics) {
        graphics.setColor(GlobalColors.farbeFertigElementCONTROL);
        drawBlockRectangle(graphics);
    }

    public void drawCenteredString(final Graphics2D g2d) {
        String toDraw = getCenteredDrawString();
        final Font oldFont = g2d.getFont();
        
        int newFontSize = Math.min(dpix-3, oldFont.getSize());
        final Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), newFontSize);
        g2d.setFont(newFont);
        String secondLine = "";
        if (toDraw.contains("\n")) {
            secondLine = toDraw.substring(toDraw.indexOf('\n') + 1);
            toDraw = toDraw.substring(0, toDraw.indexOf('\n'));
        }

        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D sb = g2d.getFont().getStringBounds(toDraw, frc);
        int[] points = getAussenabmessungenRechteckEckpunkte();
        int centerX = (int) ((points[0] + points[2]) / 2 - sb.getWidth() / 2);
        int centerY = (int) ((0.6 * points[1] + 0.4 * points[3]) + sb.getHeight() / 2);

        if (secondLine.isEmpty()) {
            g2d.drawString(toDraw, centerX, centerY);
        } else {
            g2d.drawString(toDraw, centerX, centerY - g2d.getFont().getSize() / 2 - 1);

            sb = g2d.getFont().getStringBounds(secondLine, frc);
            points = getAussenabmessungenRechteckEckpunkte();
            centerX = (int) ((points[0] + points[2]) / 2 - sb.getWidth() / 2);
            centerY = (int) ((0.6 * points[1] + 0.4 * points[3]) + sb.getHeight() / 2);
            g2d.drawString(secondLine, centerX, centerY + g2d.getFont().getSize() / 2 + 1);
        }

        g2d.setFont(oldFont);
    }

    ControlTyp getControlTyp() {
        return _controlTyp;
    }

    

    public int getBlockHeight() {
        int maxTerminals = Math.max(XIN.size(), YOUT.size());
        int height = (int) (dpix * (maxTerminals));
        return height;
    }

    public int getBlockWidth() {
        int x = getSheetPosition().x;
        int startx = (int) (dpix * (x - WIDTH));

        int width = (int) (dpix * (1.333 * WIDTH));
        if (!YOUT.isEmpty()) {
            int outXPos = YOUT.get(0).getPosition().x * dpix;

            if (outXPos - startx > width - 0.5 * dpix) {
                width = (int) (outXPos - startx - 0.5 * dpix);
            }
        }
        return width;
    }

    public double getXShift() {
        if (XIN.size() == 0 && YOUT.size() == 1) {
            return 0.5;
        } else {
            return 0;
        }
    }

    public int getYShift() {
        return 0;
    }

    public void drawBlockRectangle(final Graphics2D graphics) {
        final int posX = getSheetPosition().x;
        final int posY = getSheetPosition().y;
        final Color origColor = graphics.getColor();

        graphics.setColor(getBackgroundColor());

        final int height = getBlockHeight();
        final int width = getBlockWidth();

        int startx = (int) (dpix * (posX - 1 / 2.0)) - width / 2;
        int starty = (int) (dpix * (posY - 1 / 2.0));
        startx += getXShift() * dpix;
        starty += getYShift() * dpix;

        graphics.fillRect(startx, starty, width, height);
        graphics.setColor(origColor);
        graphics.drawRect(startx, starty, width, height);

        // Klickbereich:        
        xKlickMin = startx;
        xKlickMax = startx + width;
        yKlickMin = starty;
        yKlickMax = starty + height;
    }            

    protected String getCenteredDrawString() {        
        return getFixedIDString();
    }

    @Override
    public final ElementDisplayProperties getDisplayProperties() {
        return SchematischeEingabe2._controlDisplayMode;
    }

    final void setActiveCalculator(final AbstractControlCalculatable calc) {
        _calculator = calc;
    }
    
    void setInputSignal(final int inputIndex, final RegelBlock outputBlock, final int outputIndex) {
        try {                                    
            _calculator.setInputSignal(inputIndex, outputBlock._calculator, outputIndex);
        } catch (Exception ex) {            
            ex.printStackTrace();
            throw new RuntimeException("Error in control netlist: The output signal of control block\n"
                    + outputBlock.getStringID() + ", no. " + outputIndex + " " + 
                    outputBlock.YOUT.get(outputIndex).getLabelObject().getLabelString()                     
                    +  "\nis already connected to another output signal.\n"
                    + "This is an error in your GeckoCIRCUITS simulation model.\nAborting simulation.");
        }                        
    }

    final AbstractControlCalculatable getControlCalculatable() {
        return _calculator;
    }

    final boolean checkInputWithoutConnectionAndFillInput(final int inputIndex) {
        if(_calculator != null) {
            return _calculator.checkInputWithoutConnectionAndFill(inputIndex);
        } else {
            return true;
        }
    }

    public ControlType getType() {
        if(XIN.size() > 0 && YOUT.size() > 0) {
            return ControlType.TRANSFER;
        }
        
        if(XIN.size() == 0 && YOUT.size() > 0) {
            return ControlType.SOURCE;
        }
        
        if(XIN.size() > 0 && YOUT.size() == 0) {
            return ControlType.SINK;
        }
        
        if(XIN.size() == 0 && YOUT.size() == 0) {
            // data export has no inputs, but should be executed at the end
            return ControlType.SINK;
        }
        
        return ControlType.TRANSFER;
    }
    
    
    void setExpandedParameterListener(final UserParameter<Boolean> useExternal) {
        assert this instanceof ControlInputTwoTerminalStateable;
        useExternal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (useExternal.getValue()) {
                    ((ControlInputTwoTerminalStateable) RegelBlock.this).setExpanded();
                } else {
                    ((ControlInputTwoTerminalStateable) RegelBlock.this).setFolded();                    
                }
            }
        });
    }                    
}
