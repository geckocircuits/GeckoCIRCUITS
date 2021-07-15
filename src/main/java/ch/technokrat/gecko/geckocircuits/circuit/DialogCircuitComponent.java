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

import ch.technokrat.gecko.geckocircuits.allg.FormatJTextField;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFonts;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

abstract public class DialogCircuitComponent<T extends AbstractBlockInterface> extends GeckoDialog
        implements Schliessable, WindowListener {

    final List<UserParameter<? extends Number>> registeredParameters = new ArrayList<UserParameter<? extends Number>>();
    private static final int TEXT_FIELD_LENGTH = 10;
    public static final int NO_TF_COLS = 6;
    private SchematischeEingabe2 _se;  // callback fuer registerChange()        
    public final T element;
    public String _originalName;
    public final List<FormatJTextField> tf = new ArrayList<FormatJTextField>();
    public FormatJTextField tfNam;
    public JPanel jPanelName;
    private JCheckBox checkBoxCompEnabled;
    private JCheckBox checkBoxCompShorted;
    public JButton jButtonOk;
    public final JButton jButtonCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
    public JPanel jPanelButtonOkCancel;
    public Container con;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 25;
    private static final Dimension OK_CANCEL_DIMENSION = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);

    public DialogCircuitComponent(final JFrame parent, final boolean modal, final T element) {
        super(parent, modal);
        this.element = element;
        setGeckoIconImage();
        _se = SchematischeEingabe2.Singleton;
        this.addWindowListener(this);

        JLabel labNam = labelFabric("Name: ");
        tfNam = new FormatJTextField();
        tfNam.setColumns(TEXT_FIELD_LENGTH);
        tfNam.setText(element.getStringID());
        tfNam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                try {
                    setNewElementName();
                } catch (NameAlreadyExistsException ex) {
                    tfNam.setText(_originalName);
                }
            }
        });

        jPanelName = new JPanel();
        jPanelName.setLayout(new GridLayout(getRequiredGridSize(), 2));
        jPanelName.add(labNam);
        jPanelName.add(tfNam);

        for (FormatJTextField textField : tf) {
            if (textField != null) {
                textField.addActionListener(okActionListener);
                textField.setColumns(NO_TF_COLS);
            }
        }

        checkBoxCompEnabled = new JCheckBox("Enabled");

        checkBoxCompShorted = new JCheckBox("Shorted");

        switch (element._isEnabled.getValue()) {
            case ENABLED:
                checkBoxCompEnabled.setSelected(true);
                checkBoxCompShorted.setSelected(false);
                break;
            case DISABLED:
                checkBoxCompEnabled.setSelected(false);
                checkBoxCompShorted.setSelected(false);
                break;
            case DISABLED_SHORT:
                checkBoxCompEnabled.setSelected(true);
                checkBoxCompShorted.setSelected(true);
                break;
            default:
                assert false;
        }

        if (!(element instanceof SubCircuitTerminable)) {
            jPanelName.add(checkBoxCompEnabled);
            if (element.XIN.size() > 0 && element.YOUT.size() > 0) {
                jPanelName.add(checkBoxCompShorted);
            }
        }

        jButtonOk = GuiFabric.getJButton(I18nKeys.OK);
        jButtonOk.setPreferredSize(OK_CANCEL_DIMENSION);
        jButtonOk.addActionListener(okActionListener);
        this.getRootPane().setDefaultButton(jButtonOk);

        jButtonCancel.setPreferredSize(OK_CANCEL_DIMENSION);
        jButtonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                schliesseFenster();
            }
        });
        jPanelButtonOkCancel = new JPanel();
        jPanelButtonOkCancel.add(jButtonOk);
        jPanelButtonOkCancel.add(jButtonCancel);
    }
    public final ActionListener okActionListener = new ActionListener() {

        @Override
        public void actionPerformed(final ActionEvent actionEvent) {
            try {
                setNewElementName();
            } catch (NameAlreadyExistsException ex) {
                return;
            }

            if (checkBoxCompEnabled != null && checkBoxCompEnabled.isSelected()) {
                if (checkBoxCompShorted != null && checkBoxCompShorted.isSelected()) {
                    element._isEnabled.setValue(Enabled.DISABLED_SHORT);
                } else {
                    element._isEnabled.setValue(Enabled.ENABLED);
                }
            } else {
                element._isEnabled.setValue(Enabled.DISABLED);
            }

            try {
                processRegisteredParameters();
                processInputIndividual();
                element.setParameter(element.getParameter());
                schliesseFenster();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };
    

    public void processInputIndividual() {
    }

    @Override
    public void pack() {
        super.pack();
        if (jButtonOk != null) {
            jButtonOk.requestFocus();
        }
    }

    @Override
    public void setVisible(boolean b) {
        this.setTitle(" " + element.getTypeDescription().getTranslation());
        this.setBackground(Color.lightGray);
        this.baueGUI();
        this.pack();        
        this.setResizable(true);
        getRootPane().setDefaultButton(jButtonOk);
        jButtonOk.requestFocus();
        super.setVisible(b);
    }

    private boolean schliesseFensterCalled = false;

    @Override
    public void schliesseFenster() {
        // this function was called several times when closing a window.
        // the boolean flag is only a work-around, do this in a cleaner way
        // in the future!
        if (schliesseFensterCalled) {
            return;
        }
        schliesseFensterCalled = true;
        _se.setDirtyFlag();
        this.dispose();
        _se._visibleCircuitSheet.requestFocus();
    }

    public abstract void baueGUI();

    public final void setNewElementName() throws NameAlreadyExistsException {
        _originalName = element.getStringID();
        if (tfNam.getText().isEmpty() && !(element instanceof SubCircuitTerminable)) {
            JOptionPane.showMessageDialog(this,
                    "Empty object name is not allowed!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (element instanceof SubCircuitTerminable) {
                element.getIDStringDialog().setNameUnChecked(tfNam.getText());
            } else {
                element.setNewNameCheckedUndoable(tfNam.getText());
            }
            SchematischeEingabe2.Singleton.updateComponentCouplings(_originalName, tfNam.getText());
        } catch (NameAlreadyExistsException ex) {
            JOptionPane.showMessageDialog(this,
                    "Object name: " + tfNam.getText() + " is already in use!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            throw ex;
        }

    }

    public int getRequiredGridSize() {
        return 2;
    }

    @Override
    public void windowDeactivated(final WindowEvent windowEvent) {
    }

    @Override
    public void windowActivated(final WindowEvent windowEvent) {
    }

    @Override
    public void windowDeiconified(WindowEvent we) {
    }

    @Override
    public void windowIconified(WindowEvent we) {
    }

    @Override
    public void windowClosed(WindowEvent we) {
    }

    @Override
    public void windowClosing(WindowEvent we) {
        this.schliesseFenster();
    }

    @Override
    public void windowOpened(WindowEvent we) {
    }

    public static FormatJTextField fabricFormatTextField(final UserParameter parameter) {
        final FormatJTextField returnValue = new FormatJTextField(parameter.getDoubleValue());

        if (!parameter.getNameOpt().isEmpty()) {
            returnValue.setForeground(GlobalColors.farbeOPT);
            returnValue.setText(parameter.getNameOpt());
        }

        returnValue.setColumns(NO_TF_COLS);
        return returnValue;
    }

    public static JLabel labelFabric(final String labelTxt) {
        JLabel returnValue = new JLabel(labelTxt);
        returnValue.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        return returnValue;
    }

    public FormatJTextField getRegisteredTextField(final UserParameter<? extends Number> par) {
        FormatJTextField returnValue = new FormatJTextField();
        registeredParameters.add(par);
        returnValue.setColumns(NO_TF_COLS);
        if (par.getNameOpt().isEmpty()) {
            returnValue.setNumberToField(par.getDoubleValue());
        } else {
            returnValue.setText(par.getNameOpt());
            returnValue.setForeground(GlobalColors.farbeOPT);
        }
        tf.add(returnValue);
        returnValue.addActionListener(okActionListener);
        return returnValue;
    }

    public void unregisterTextField(final FormatJTextField toRemove) {
        final int index = tf.indexOf(toRemove);
        tf.remove(toRemove);
        registeredParameters.remove(index);
    }

    public JPanel createParameterPanel(final UserParameter<? extends Number>... parameters) {
        JPanel pPD = new JPanel();
        pPD.setLayout(new GridLayout(parameters.length + 1, 2));

        for (UserParameter<? extends Number> par : parameters) {
            final JLabel labPar1 = new JLabel(par.getShortName() + " =  ");
            labPar1.setFont(GlobalFonts.LAB_FONT_DIALOG_1);
            labPar1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
            pPD.add(labPar1);
            FormatJTextField textField = getRegisteredTextField(par);
            pPD.add(textField);
        }

        pPD.add(new JLabel(" "));  // Abstandhalter
        //        
        return pPD;
    }

    protected void processRegisteredParameters() {
        for (UserParameter<? extends Number> uPar : registeredParameters) {
            int index = registeredParameters.indexOf(uPar);
            Object value = uPar.getValue();
            if (value instanceof Double) {
                setValueFromTextField((UserParameter<Double>) uPar, tf.get(index));
            }
            if (value instanceof Integer) {
                setValueFromIntegerTextField((UserParameter<Integer>) uPar, tf.get(index));
            }
        }
    }

    public static void setValueFromTextField(final UserParameter<Double> userParameter,
            final FormatJTextField formatJTextField) {

        double number = formatJTextField.getNumberFromField();

        if (number == FormatJTextField.IS_VARIABLE) {
            userParameter.setNameOpt(formatJTextField.getText());
        } else {
            if (userParameter.getNameOpt().isEmpty()) {
                userParameter.setUserValue(number);
            } else {
                userParameter.setNameOpt("");
                userParameter.setValueWithoutUndo(number);
            }

        }
    }

    public static void setValueFromIntegerTextField(final UserParameter<Integer> userParameter,
            final FormatJTextField formatJTextField) {
        double number = formatJTextField.getNumberFromField();

        if (number == FormatJTextField.IS_VARIABLE) {
            userParameter.setNameOpt(formatJTextField.getText());
        } else {
            userParameter.setUserValue((int) number);
            userParameter.setNameOpt("");
        }
    }
}
