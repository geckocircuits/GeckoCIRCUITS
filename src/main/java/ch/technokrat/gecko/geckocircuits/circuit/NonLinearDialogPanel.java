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

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileChooser;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFileManagerWindow;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractNonLinearCircuitComponent;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;
import ch.technokrat.gecko.geckocircuits.newscope.GraferV4;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSettings;
import ch.technokrat.gecko.geckocircuits.newscope.SimpleGraferPanel;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class NonLinearDialogPanel extends JPanel {

    private static final int BUTTON_HEIGHT = 30;
    private static final Dimension BUTTON_DIMENSION = new Dimension(80, BUTTON_HEIGHT);
    private final AbstractNonLinearCircuitComponent _nonlinearParent;
    private double[][] data;
    private String _extension = ".txt";
    private String _type = "text file";
    private File savedExternalFile;
    private GeckoFile loadedFile = null;
    private GraferV4 _grafer;
    public JButton _jbOK;
    private final JPanel lowerPanel = new JPanel();
    private DataTablePanel table;
    private final SimpleGraferPanel newScope1;
    private final JDialog _parentDialog;

    public NonLinearDialogPanel(JDialog parentDialog, final AbstractNonLinearCircuitComponent elementLK,
            final boolean isYAxisLog) {
        _nonlinearParent = elementLK;
        _parentDialog = parentDialog;
        this.setLayout(new BorderLayout());
        ScopeSettings settings = new ScopeSettings();

        _grafer = new GraferV4(settings);
        _grafer.createInitialAndSingleDiagram(false, isYAxisLog, 1);

        newScope1 = new SimpleGraferPanel(_grafer);

        _extension = _nonlinearParent.getNonlinearFileExtension();
        _type = "nonlinear " + _nonlinearParent.getNonlinearName() + " characteristic";

        this.initCharacteristic();
        baueGUIok();
        add(this.baueGUIInput(), BorderLayout.CENTER);

        this.add(lowerPanel, BorderLayout.SOUTH);
        _parentDialog.setTitle(_type);
        _parentDialog.getRootPane().setDefaultButton(_jbOK);
        updatePlot();
    }

    private void updatePlot() {
        DataContainerSimple dcs1 = DataContainerSimple.fabricArrayTimeSeries(1, data[0].length);
        dcs1.setSignalName(_nonlinearParent.getNonlinearName(), 0);

        for (int i = 0; i < data[0].length; i++) {
            dcs1.insertValuesAtEnd(new float[]{(float) data[1][i]}, data[0][i]);
        }

        dcs1.setContainerStatus(ContainerStatus.PAUSED);
        _grafer.setDataContainer(dcs1);
    }

    private void initCharacteristic() {
        double[][] ch = null;

        ch = _nonlinearParent.getNonlinearCharacteristic();
        if ((ch == null) || (ch[0].length == 0)) {
            // default-characteristic: 
            data = _nonlinearParent.getInitalNonlinValues();
        } else {
            data = new double[2][];
            data[0] = ch[0];
            data[1] = ch[1];
        }
    }

    private JPanel baueGUIInput() {
        table = new DataTablePanel(new String[]{_nonlinearParent.getIndependentVariableName(),
            _nonlinearParent.getNonlinearNameShort()});
        table.setPreferredSize(new Dimension(150, 100));
        table.setValues(data);

        table.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                data = table.getCheckedData();
                updatePlot();
            }
        });

        JPanel jpCONDdataGes = new JPanel();
        jpCONDdataGes.setLayout(new BorderLayout());
        jpCONDdataGes.add(table, BorderLayout.CENTER);
        JButton jbSaveAsFile = GuiFabric.getJButton(I18nKeys.SAVE);
        jbSaveAsFile.setPreferredSize(BUTTON_DIMENSION);
        jbSaveAsFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                GeckoFileChooser fileChooser = GeckoFileChooser.createSimpleSaveFileChooser(_extension, _parentDialog);
                if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
                    return;
                }
                try {
                    savedExternalFile = AbstractNonLinearCircuitComponent.writeNonLinearCharacteristicToFile(data, fileChooser.getFileWithCheckedEnding());
                } catch (java.io.IOException e) {
                    final String errorMessage = "Error writing non-linear characteristic file " + fileChooser.getFileWithCheckedEnding() + "\n" + e.getMessage();
                    final String errorTitle = "ERROR - I/O exception";
                    JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton jbLoadFromFile = GuiFabric.getJButton(I18nKeys.LOAD);
        jbLoadFromFile.setPreferredSize(BUTTON_DIMENSION);
        jbLoadFromFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                GeckoFileManagerWindow gfm = new GeckoFileManagerWindow(_nonlinearParent, _extension, _type, true);
                gfm.setNonLinearDialog((DialogNonLinearity) _parentDialog);
                gfm.setVisible(true);
            }
        });

        JButton jbCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
        jbCancel.setPreferredSize(BUTTON_DIMENSION);
        jbCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                _parentDialog.dispose();
            }
        });


        lowerPanel.add(jbSaveAsFile);
        lowerPanel.add(jbLoadFromFile);
        lowerPanel.add(jbCancel);

        JPanel jpCOND = new JPanel();
        jpCOND.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        jpCOND.setLayout(new BorderLayout());
        jpCOND.add(newScope1, BorderLayout.CENTER);
        jpCOND.add(jpCONDdataGes, BorderLayout.EAST);
        //==========================        
        return jpCOND;
    }

    private void baueGUIok() {
        lowerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "", TitledBorder.LEFT, TitledBorder.TOP));
        //---------------
        _jbOK = GuiFabric.getJButton(I18nKeys.OK);
        _jbOK.setPreferredSize(BUTTON_DIMENSION);
        _jbOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                if (loadedFile != null) {
                    //here we don't need to do anything, since file is already set, but check anyway
                    if (loadedFile != _nonlinearParent.getNonLinearFile()) {
                        ArrayList<GeckoFile> newFile = new ArrayList<GeckoFile>();
                        newFile.add(loadedFile);
                        _nonlinearParent.addFiles(newFile);
                    }
                } else if (savedExternalFile != null) {
                    //characteristic has been saved to an external file, and we should not attribute this to the circuit element
                    try {
                        GeckoFile newFileFromExternal = new GeckoFile(savedExternalFile, GeckoFile.StorageType.EXTERNAL, Fenster.getOpenFileName());
                        ArrayList<GeckoFile> newFile = new ArrayList<GeckoFile>();
                        newFile.add(newFileFromExternal);
                        _nonlinearParent.addFiles(newFile);
                    } catch (java.io.FileNotFoundException e) {
                        final String errorMessage = e.getMessage();
                        final String errorTitle = "File Not Found";
                        JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    GeckoFile existingNonLinFile = _nonlinearParent.getNonLinearFile();
                    if (existingNonLinFile == null) {
                        String internalFileName = getInternalFileName(_nonlinearParent.getStringID());
                        if (internalFileName != null) {
                            _nonlinearParent.setNonlinearCharacteristic(data, internalFileName);
                        } else {
                            return;
                        }
                    } else {
                        if (existingNonLinFile.noOfUsers() > 1) {
                            switch (overwriteOption(existingNonLinFile.getName())) {
                                case -1: //ESC key
                                    return;
                                case 0: // overwrite data of existing file
                                    _nonlinearParent.setNonlinearCharacteristic(data); //overwrite existing file
                                    break;
                                case 1: // save as new file
                                    String newInternalFileName = getInternalFileName(_nonlinearParent.getStringID() + "new");
                                    if (newInternalFileName != null) {
                                        _nonlinearParent.setNonlinearCharacteristic(data, newInternalFileName);
                                    } else {
                                        return;
                                    }
                                    break;
                            }
                        } else if (existingNonLinFile.getStorageType() == GeckoFile.StorageType.EXTERNAL) {
                            if (originalDataIsChanged(data, _nonlinearParent.nonlinearData)) {

                                int overwrite = overwriteExternal(existingNonLinFile.getName());
                                switch (overwrite) {
                                    case -1: //ESC key
                                        return;
                                    case 0:
                                        _nonlinearParent.setNonlinearCharacteristic(data); //overwrite existing file
                                        break;
                                    case 1:
                                        String newInternalFileName = getInternalFileName(_nonlinearParent.getStringID() + "new");
                                        if (newInternalFileName != null) {
                                            _nonlinearParent.setNonlinearCharacteristic(data, newInternalFileName);
                                        } else {
                                            return;
                                        }
                                        break;
                                }
                            }
                        } else {
                            _nonlinearParent.setNonlinearCharacteristic(data);
                        }
                    }
                }
                _parentDialog.dispose();
            }

            private boolean originalDataIsChanged(double[][] data, double[][] nonlinearData) {
                if(data.length != nonlinearData.length) {
                    return true;
                }
                
                for(int i = 0; i < data.length; i++) {                    
                    for(int j = 0; j < data[0].length; j++) {
                        if(data[i][j] != nonlinearData[i][j]) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        lowerPanel.add(new JSeparator(SwingConstants.VERTICAL));

        lowerPanel.add(_jbOK);

    }

    private String getInternalFileName(String blockName) {
        String internalFileName = JOptionPane.showInputDialog(null, "Please select a non-linear characteristic identifier with extension *" + _extension + " (e.g. " + blockName.replace(".", "") + _extension + "):",
                "Choose file name",
                JOptionPane.PLAIN_MESSAGE);
        if (internalFileName != null) {
            internalFileName = internalFileName.trim();
            if (!internalFileName.endsWith(_extension)) {
                internalFileName += _extension;
            }
        }
        return internalFileName;
    }

    private int overwriteOption(String existingFileName) {
        Object[] options = {"Overwrite existing", "Create New"};

        int selected = JOptionPane.showOptionDialog(null,
                "Non-linear characteristic " + existingFileName + " is also used by other circuit elements.\n"
                + "Would you like to overwrite it (will affect all other elements that use it!)\n"
                + "or create a new one from the input data?",
                "Ovewrite existing characteristic?",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        return selected;
    }

    private int overwriteExternal(String existingFileName) {
        Object[] options = {"Overwrite", "Create New"};

        int selected = JOptionPane.showOptionDialog(null,
                "This element uses the external file " + existingFileName + " for its non-linear characteristic.\n"
                + "Would you like to overwrite it or create a new internal file?",
                "Overwrite external file?",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        return selected;
    }

    public void setCharacteristicLoadedFromFile(final GeckoFile file) {
        //check if file given is file set -> if not, there was an error
        if (_nonlinearParent.getNonLinearFile() == file) {
            double[][] nonLin = _nonlinearParent.getNonlinearCharacteristic();
            data = new double[2][];
            data[0] = nonLin[0];
            data[1] = nonLin[1];
            for(int i = 0; i < data[0].length; i++) {
                System.out.println("iii " + i + " " + data[0][i] + " " + data[1][i] + " " + (data[0][i] * data[1][i]));
            }
            table.setValues(data);
            loadedFile = file;
            updatePlot();
        }
    }
}
