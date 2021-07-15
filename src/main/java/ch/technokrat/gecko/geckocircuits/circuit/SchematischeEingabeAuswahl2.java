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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.allg.LastComponentButton;
import ch.technokrat.gecko.geckocircuits.allg.SuggestionField;
import ch.technokrat.gecko.geckocircuits.control.ControlTyp;
import static ch.technokrat.gecko.geckocircuits.control.ControlTyp.*;
import ch.technokrat.gecko.i18n.LangInit;
import ch.technokrat.gecko.i18n.translationtoolbox.PopupListener;
import java.awt.AWTException;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class SchematischeEingabeAuswahl2 extends JTabbedPane {

    private static final int MIN_GRID_SIZE = 12;
    private static final int LOWER_BOUND_OFFSET = -4;
    private final List<AbstractBlockInterface> _showBlocks;
    private final List<AbstractComponentTyp> _showBlocksType = new ArrayList<AbstractComponentTyp>();
    private final Map<AbstractComponentTyp, AbstractBlockInterface> _map = new HashMap<AbstractComponentTyp, AbstractBlockInterface>();
    private AbstractBlockInterface _paintBlock = null;
    public AbstractComponentTyp _typElement = null;
    private SchematischeEingabe2 se;
    private CircuitTyp[] _typLK = new CircuitTyp[]{
        CircuitTyp.LK_U, CircuitTyp.LK_I, CircuitTyp.LK_R, CircuitTyp.LK_C, CircuitTyp.LK_L,
        CircuitTyp.LK_LKOP2, CircuitTyp.LK_M, CircuitTyp.LK_S, CircuitTyp.LK_IGBT, CircuitTyp.LK_MOSFET,
        CircuitTyp.LK_D, CircuitTyp.LK_THYR, CircuitTyp.LK_BJT, CircuitTyp.LK_OPV1, CircuitTyp.LK_TRANS// Typ.LK_LKOP
    };
    private CircuitTyp[] _typMotor = new CircuitTyp[]{
        CircuitTyp.LK_MOTOR, CircuitTyp.LK_MOTOR_PERM, CircuitTyp.LK_MOTOR_PMSM, CircuitTyp.LK_MOTOR_SMSALIENT, CircuitTyp.LK_MOTOR_SMROUND,
        CircuitTyp.LK_MOTOR_IMC, CircuitTyp.LK_MOTOR_IMA, CircuitTyp.LK_MOTOR_IMSAT, /*
         * Typ.LK_MOTOR_IMB,
         */ CircuitTyp.LK_LISN,};
    
    private AbstractComponentTyp[] _typSubcircuit = new AbstractComponentTyp[]{
        SpecialTyp.SUBCIRCUIT, CircuitTyp.LK_TERMINAL, CircuitTyp.TH_TERMINAL,
        ControlTyp.C_TERMINAL, CircuitTyp.REL_TERMINAL, CircuitTyp.LK_GLOBAL_TERMINAL,
        CircuitTyp.TH_GLOBAL_TERMINAL, ControlTyp.C_GLOBAL_TERMINAL,
        CircuitTyp.REL_GLOBAL_TERMINAL, ControlTyp.C_MUX, ControlTyp.C_DEMUX};
    
    private ControlTyp[] _typCONTROL = new ControlTyp[]{
        C_GAIN, C_PT1, C_PT2, C_INT, C_PI, C_PD, C_HYS, C_LIMIT, C_ADD, C_SUB, C_MUL, C_DIV,
        C_MIN, C_MAX, C_SIGN, C_TF
    };
    private ControlTyp[] _typMeasure = new ControlTyp[]{
        C_SCOPE, C_VOLTMETER, C_AMPMETER, C_MMFMETER, C_FLUXMETER,
        C_TEMP, C_FLOW, C_VIEWMOT, C_CISPR16, C_SPACE_VECTOR,
        C_U_ZI//, C_SMALL_SIG
    };
    
    private ControlTyp[] _typDigital = new ControlTyp[]{
        C_NOT, C_AND, C_OR, C_XOR,
        C_DELAY, C_SAMPLEHOLD, C_COUNTER,
        C_GE, C_GT, C_EQ, C_NE};
    private ControlTyp[] _typMath = new ControlTyp[]{
        C_ADD, C_SUB, C_MUL, C_DIV,
        C_ABS, C_SIGN, C_ROUND, C_SQR, C_SQRT, C_POW, C_EXP, C_LN,
        C_SIN, C_COS, C_TAN, C_ASIN, C_ACOS, C_ATAN, C_SDFT};
    private ControlTyp[] _typSourceSink = new ControlTyp[]{
        C_SIGNALSOURCE, C_CONST, C_SWITCH,
        C_TO_EXTERNAL, C_FROM_EXTERNAL, C_DATA_EXPORT, C_SOURCE_IMPORT_DATA, C_SOURCE_RANDOM
    };
    
    
    private AbstractComponentTyp[] _typSpecial = new AbstractComponentTyp[]{
        ControlTyp.C_JAVA_FUNCTION, ControlTyp.C_NATIVE_C_FUNCTION, ControlTyp.C_SMALL_SIG, ControlTyp.C_ABCDQ, ControlTyp.C_DQABC,
        ControlTyp.C_TIME, ControlTyp.C_SPARSEMATRIX, ControlTyp.C_PMSM_CONTROL,
        ControlTyp.C_PMSM_MODULATOR, ControlTyp.C_THYR_CTRL, 
        /*ControlTyp.C_DEBUG,*/ SpecialTyp.TEXTFIELD
    };
    private AbstractComponentTyp[] _typTherm = new AbstractComponentTyp[]{
        CircuitTyp.TH_TEMP, CircuitTyp.TH_FLOW, CircuitTyp.TH_PvCHIP,
        CircuitTyp.TH_RTH, CircuitTyp.TH_CTH,
        CircuitTyp.TH_AMBIENT
    };
    private CircuitTyp[] _typReluctance = new CircuitTyp[]{
        CircuitTyp.REL_RELUCTANCE, CircuitTyp.NONLIN_REL, CircuitTyp.REL_MMF, CircuitTyp.REL_INDUCTOR
    };
    private LastComponentButton _lastComponentButton;

    public void anmeldenSchematischeEingabe(SchematischeEingabe2 se) {
        this.se = se;
    }

    public SchematischeEingabeAuswahl2() {

        List<AbstractBlockInterface> showAllBlocks = new ArrayList<AbstractBlockInterface>();
        
        for (AbstractComponentTyp typ : AbstractTypeInfo._allRegisteredComponentEnums) {
            AbstractBlockInterface newComponent = typ.getTypeInfo().fabric();
            if (newComponent != null) {
                newComponent.setDummyIDStringDialog();
                showAllBlocks.add(newComponent);
                _map.put(typ, newComponent);
                _showBlocksType.add(typ);
            }
        }


        IDStringDialog.clearAllNames();

        _showBlocks = Collections.unmodifiableList(showAllBlocks);
        for (AbstractBlockInterface block : _showBlocks) {
            block.setToolbarPaintProperties();
        }

        this.initPanels();
    }

    private void createButtonsForPanel(AbstractComponentTyp[] types, JPanel compCircuit) {
        for (int i = 0; i < types.length; i++) {
            AbstractBlockInterface block = _map.get(types[i]);
            JButton testButton = new SchematischeEingabeAuswahl2.ComponentSelectionButton(types[i], block);
            compCircuit.add(testButton);
        }
    }

    private JPanel createJPanelForTypes(final AbstractComponentTyp[] types, final String tabTitle) {
        final JPanel returnValue = new JPanel();
        final GridLayout gridLayout = new GridLayout(Math.max(types.length, MIN_GRID_SIZE), 1);
        gridLayout.setVgap(-1);
        returnValue.setLayout(gridLayout);
        returnValue.setBorder(new EmptyBorder(0, 0, LOWER_BOUND_OFFSET, -1));
        createButtonsForPanel(types, returnValue);
        final JScrollPane scrollPane = new JScrollPane(returnValue);
        scrollPane.revalidate();
        this.addTab(tabTitle, scrollPane);
        this.setForegroundAt(getTabCount() - 1, returnValue.getComponent(0).getForeground());
        return returnValue;
    }

    public void registerSearchField(final SuggestionField searchTestField) {
        List<String> searchVector = new ArrayList<String>();
        for (AbstractBlockInterface showBlock : _showBlocks) {
            searchVector.add(showBlock.getTypeDescription().getEnglishString());
        }
        searchTestField.setSuggestData(searchVector);

        searchTestField.addSelectionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (AbstractBlockInterface block : _showBlocks) {                    
                    if (block.getTypeDescription().getTranslation().equals(searchTestField.getText())) {
                        _lastComponentButton.setSelectedShowComponent(block, _showBlocksType.get(_showBlocks.indexOf(block)));
                    }
                }
            }
        });
    }

    public void registerLastComponentButton(LastComponentButton lastComponentButton) {
        _lastComponentButton = lastComponentButton;

        _lastComponentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (_lastComponentButton.getSelectedBlock() != null) {
                    se.deselect();
                    _typElement = _lastComponentButton.getTyp();                    
                }

            }
        });

        SchematischeEingabeAuswahl2.MouseDraggedOutsideListener listener = new SchematischeEingabeAuswahl2.MouseDraggedOutsideListener(_lastComponentButton);
        _lastComponentButton.addFocusListener(listener);
        _lastComponentButton.addMouseMotionListener(listener);
    }

    class ComponentSelectionButton extends JButton {

        private final AbstractComponentTyp _typ;

        public ComponentSelectionButton(final AbstractComponentTyp typ, final AbstractBlockInterface exampleBlock) {
            super(LangInit.getTranslatedString(exampleBlock.getTypeDescription()));                        
            addMouseListener(new PopupListener(exampleBlock.getTypeDescription()));
            _typ = typ;
            final String description = getText();
            final String descriptionVerbose = LangInit.getTranslatedString(exampleBlock.getTypeDescriptionVerbose());

            // show tooltips only, when information is different from shown
            // component name!
            if (!description.equals(descriptionVerbose)) {
                setToolTipText("<html><body width='250'>" + descriptionVerbose + "</html>");
            }

            setHorizontalAlignment(SwingConstants.LEADING);
            setForeground(exampleBlock.getForeGroundColor());            
            setContentAreaFilled(false);
            setFocusPainted(false);


            addMouseListener(new MouseListener() {
                private boolean _mouseInComponent;
                @Override
                public void mouseClicked(final MouseEvent event) {
                    // nothing todo
                }

                @Override
                public void mousePressed(final MouseEvent event) {
                    // nothing todo
                }

                @Override
                public void mouseReleased(final MouseEvent event) {
                    if(_mouseInComponent) {                        
                        se.deselect();
                        _typElement = _typ;                        
                        _lastComponentButton.setSelectedShowComponent(_map.get(_typ), _typ);                    
                    }
                }

                @Override
                public void mouseEntered(final MouseEvent event) {
                    _mouseInComponent = true;
                    _lastComponentButton.setTempShowComponent(_map.get(_typ));
                }

                @Override
                public void mouseExited(final MouseEvent event) {
                    _mouseInComponent = false;
                    _lastComponentButton.removeTempShowComponent(_map.get(_typ));
                }
            });

            SchematischeEingabeAuswahl2.MouseDraggedOutsideListener dragListener = new SchematischeEingabeAuswahl2.MouseDraggedOutsideListener(_typ);
            addMouseMotionListener(dragListener);
            addFocusListener(dragListener);
        }
    }

    private class MouseDraggedOutsideListener implements MouseMotionListener, FocusListener {

        long _lastDraggedMillis;
        private AbstractComponentTyp _typ;
        private LastComponentButton _button;

        public MouseDraggedOutsideListener(final AbstractComponentTyp typ) {
            _typ = typ;
        }

        public MouseDraggedOutsideListener(final LastComponentButton button) {
            _button = button;
        }

        @Override
        public void mouseDragged(final MouseEvent event) {
            _lastDraggedMillis = System.currentTimeMillis();            
        }

        @Override
        public void mouseMoved(final MouseEvent event) {
            // nothing todo
        }

        @Override
        public void focusGained(final FocusEvent event) {
            // nothing todo!
        }

        @Override
        public void focusLost(final FocusEvent event) {
            long timeInterval = _lastDraggedMillis - System.currentTimeMillis();
            if (Math.abs(timeInterval) < 2) {
                se.deselect();
                if (_typ != null) {
                    _typElement = _typ;
                } else {
                    assert _button != null;
                    _typElement = _button.getTyp();
                    if (_typElement == null) {
                        return;
                    }
                }

                _lastComponentButton.setSelectedShowComponent(_map.get(_typElement), _typElement);
                se.testCreateNewComponent();
                se._singleComponentMouseDrag = true;
                try {
                    Robot robot;
                    robot = new Robot();
                    final java.awt.Point mousePoint = MouseInfo.getPointerInfo().getLocation();
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    robot.mouseMove(mousePoint.x - 10, mousePoint.y);
                    //robot.mousePress(InputEvent.BUTTON1_MASK);


                } catch (AWTException ex) {
                    Logger.getLogger(SchematischeEingabeAuswahl2.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void initPanels() {
        createJPanelForTypes(_typLK, "Circuit");
        createJPanelForTypes(_typMotor, "Motor & EMI");
        createJPanelForTypes(_typSubcircuit, "Subcircuit");
        createJPanelForTypes(_typTherm, "Thermal");
        createJPanelForTypes(_typReluctance, "Reluctance");
        createJPanelForTypes(_typCONTROL, "Control");
        createJPanelForTypes(_typMeasure, "Measure");
        createJPanelForTypes(_typDigital, "Digital");
        createJPanelForTypes(_typMath, "Math");
        createJPanelForTypes(_typSourceSink, "Source/Sink");
        createJPanelForTypes(_typSpecial, "Special");
    }
}
