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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.SolverSettings;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import java.net.URL;
import javax.swing.JTabbedPane;

public class DialogSimParameter extends JDialog implements ActionListener {

    private double _dt, _tDuration, _tPause;
    private SolverSettings _solverSettings;
    private FormatJTextField[] _tf;
    private static final TechFormat TECH_FORMAT = new TechFormat();
    private static final int COLS = 6;
    private double _tPre = -1;
    private double _dtPre = -1;
    private String[] _solverOptions = {"Backward Euler", "Trapezoidal", "Gear-Shichman"};
    private int _solvertype;
    private JComboBox _dropdownSolvSel = new JComboBox(_solverOptions);

    public DialogSimParameter(final Frame owner, final SolverSettings callback) {
        super(owner, true);
        
        try {
            this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif"))).getImage());
        } catch (Exception e) {
        }
        this._solverSettings = callback;
        _dt = callback.dt.getValue();
        _tDuration = callback._tDURATION.getValue();
        _tPause = callback._tPAUSE.getValue();
        _tPre = callback._T_pre.getValue();
        _dtPre = callback._dt_pre.getValue();
        _solvertype = Fenster._solverSettings.SOLVER_TYPE.getValue().getOldGeckoIndex();
        _tf = new FormatJTextField[9];
        for (int i1 = 0; i1 < _tf.length; i1++) {
            _tf[i1] = new FormatJTextField();
        }
        //

        if (GeckoSim.operatingmode == OperatingMode.SIMULINK) {
            _tf[1].setEnabled(false);
            _tf[2].setEnabled(false);
            _tf[3].setEnabled(false);
            _tf[4].setEnabled(false);
        }

        this.setTitle(" Simulation Parameters");
        this.baueGUI();
        //this.pack();
        this.setSize(260, 340);
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void baueGUI() {
        //------------------------
        JButton knOK = GuiFabric.getJButton(I18nKeys.OK);

        knOK.setActionCommand("OK");
        knOK.addActionListener(this);
        knOK.setDefaultCapable(true);

        this.getRootPane().setDefaultButton(knOK);

        JButton knX = GuiFabric.getJButton(I18nKeys.CANCEL);
        knX.setActionCommand("Cancel");
        knX.addActionListener(this);
        JPanel px = new JPanel();
        px.add(knOK);
        px.add(knX);
        //------------------
        JTabbedPane tabber = new JTabbedPane();
        tabber.addTab("Transient", this.getSimParameterPanel());
        tabber.addTab("Steady-State", this.getSteadyStatePanel());
        tabber.addTab("Solver", this.getIntegrationPanel());
        //------------------
        Container con = this.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(tabber, BorderLayout.CENTER);
        con.add(px, BorderLayout.SOUTH);
        this.setLocationByPlatform(true);
        this.setLocationRelativeTo(GeckoSim._win);
    }

    private JPanel getIntegrationPanel() {
//      
        //------------------
        JPanel jpAllg = new JPanel();
        jpAllg.setLayout(new GridLayout(1, 1));
        jpAllg.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Integration algorithm selection", TitledBorder.LEFT, TitledBorder.TOP));

        _dropdownSolvSel.setSelectedIndex(_solvertype);
        jpAllg.add(_dropdownSolvSel); //jpM.add(new JLabel(" "));


        JTextArea jtx = new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx.setText("Backward-Euler: Very stable, with some numeric damping\n"
                + "Trapezoidal: No numeric damping, possibly unstable\n"
                + "Gear-Shichman: Behavior is in between Trapezoidal and Backward Euler methods");

        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(this.getBackground());
        jtx.setEditable(false);
        //
        //
        JPanel jpTxt = new JPanel();
        jpTxt.setLayout(new BorderLayout());
        jpTxt.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Info", TitledBorder.LEFT, TitledBorder.TOP));
        jpTxt.add(jtx, BorderLayout.CENTER);
        JPanel jpMM = new JPanel();
        jpMM.setLayout(new BorderLayout());
        jpMM.add(jpAllg, BorderLayout.NORTH);
        jpMM.add(jpTxt, BorderLayout.CENTER);
        return jpMM;

    }

    private JPanel getSimParameterPanel() {
        //------------------
        JPanel jpM = new JPanel();
        jpM.setLayout(new GridLayout(3, 2));
        jpM.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Parameters", TitledBorder.LEFT, TitledBorder.TOP));
        //
        JLabel labDT = new JLabel("dt =  ");
        labDT.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        JLabel labT1 = new JLabel("t_SIM =  ");
        labT1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        JLabel labTstop = new JLabel("t_BR =  ");
        labTstop.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        //
        _tf[0].setNumberToField(_dt);
        _tf[0].setColumns(COLS);
        _tf[0].addActionListener(this);
        _tf[1].setNumberToField(_tDuration);
        _tf[1].setColumns(COLS);
        _tf[1].addActionListener(this);
        _tf[2].setNumberToField(_tPause);
        _tf[2].setColumns(COLS);
        _tf[2].addActionListener(this);
        //
        jpM.add(labDT);
        jpM.add(_tf[0]);   //jpM.add(new JLabel(" "));
        jpM.add(labT1);
        jpM.add(_tf[1]);  //jpM.add(new JLabel(" "));
        jpM.add(labTstop);
        jpM.add(_tf[2]);   //jpM.add(new JLabel(" "));

        //------------------
        // Erklaerender Text zu 'tPAUSE':
        JPanel jpTxt = new JPanel();
        jpTxt.setLayout(new BorderLayout());
        jpTxt.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "t_BR", TitledBorder.LEFT, TitledBorder.TOP));
        //
        JTextArea jtx = new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx.setText("Set 't_BR' to stop the simulation at a defined time before 't_END'. Set 't_BR= -1' to disable this feature.");
        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(this.getBackground());
        jtx.setEditable(false);
        //
        jpTxt.add(jtx, BorderLayout.CENTER);
        //
        JPanel jpMM = new JPanel();
        jpMM.setLayout(new BorderLayout());
        jpMM.add(jpM, BorderLayout.NORTH);
        jpMM.add(jpTxt, BorderLayout.CENTER);
        jpMM.setPreferredSize(new Dimension((int) jpM.getPreferredSize().getWidth(), 300));
        //------------------
        return jpMM;
    }

    private JPanel getSteadyStatePanel() {
        // T_ss, newton-bisect, newton:iterNmax, newton:iter%
        //------------------
        JPanel jpAllg = new JPanel();
        jpAllg.setLayout(new GridLayout(3, 2));
        jpAllg.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Parameters", TitledBorder.LEFT, TitledBorder.TOP));
        //
        JLabel lab1 = new JLabel("T_pre =  ");
        lab1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        JLabel lab2 = new JLabel("dt_pre =  ");
        lab2.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        //
        _tf[3].setNumberToField(_tPre);
        _tf[3].setColumns(COLS);
        _tf[3].addActionListener(this);
        _tf[4].setNumberToField(_dtPre);
        _tf[4].setColumns(COLS);
        _tf[4].addActionListener(this);
        //tf[5].setNumberToField(maxErrorNewton);       tf[5].setColumns(cols);    tf[5].addActionListener(this);
        //
        jpAllg.add(lab2);
        jpAllg.add(_tf[4]);
        jpAllg.add(lab1);
        jpAllg.add(_tf[3]);
        //------------------
        JTextArea jtx = new JTextArea();
        jtx.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        jtx.setText("T_pre, dt_pre: The simulation is executed with the simulation time of T_pre and the stepwidth"
                + " of dt_pre in advance of the regular simulation. This enables an easy finding of the"
                + " steady state solution.\n");

        jtx.setLineWrap(true);
        jtx.setWrapStyleWord(true);
        jtx.setBackground(this.getBackground());
        jtx.setEditable(false);
        //
        //
        JPanel jpTxt = new JPanel();
        jpTxt.setLayout(new BorderLayout());
        jpTxt.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Info", TitledBorder.LEFT, TitledBorder.TOP));
        jpTxt.add(jtx, BorderLayout.CENTER);
        JPanel jpMM = new JPanel();
        jpMM.setLayout(new BorderLayout());
        jpMM.add(jpAllg, BorderLayout.NORTH);
        jpMM.add(jpTxt, BorderLayout.CENTER);
        return jpMM;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("Cancel")) {
            this.dispose();
        } else {
            // 'OK'-Knopf oder 'Return'-Taste gedrueckt
            boolean allesOK = true;
            try {
                _dt = _tf[0].getNumberFromField();
            } catch (Exception nfe) {
                allesOK = false;
            }
            try {
                _tDuration = _tf[1].getNumberFromField();
            } catch (Exception nfe) {
                allesOK = false;
            }
            try {
                _tPause = _tf[2].getNumberFromField();
            } catch (Exception nfe) {
                allesOK = false;
            }
            try {
                _tPre = _tf[3].getNumberFromField();
            } catch (Exception nfe) {
                allesOK = false;
            }

            //try { Tss=       tf[3].getNumberFromField(); } catch (Exception nfe) { allesOK=false; }
            //try { maxIterationsNewton= (int)tf[4].getNumberFromField(); } catch (Exception nfe) { allesOK=false; }
            //try { maxErrorNewton= tf[5].getNumberFromField(); } catch (Exception nfe) { allesOK=false; }
            try {
                _dtPre = _tf[4].getNumberFromField();
            } catch (Exception nfe) {
                allesOK = false;
            }
            _solvertype = _dropdownSolvSel.getSelectedIndex();
            if (allesOK) {
                _solverSettings.dt.setValue(_dt);
                _solverSettings._tDURATION.setValue(_tDuration);
                _solverSettings._tPAUSE.setValue(_tPause);
                _solverSettings.SOLVER_TYPE.setValue(SolverType.getFromOldGeckoIndex(_solvertype));
                _solverSettings._dt_pre.setValue(_dtPre);
                _solverSettings._T_pre.setValue(_tPre);
                this.dispose();
            }
        }
    }
}
