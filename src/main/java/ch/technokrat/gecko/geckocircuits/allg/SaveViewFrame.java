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

import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public final class SaveViewFrame extends GeckoDialog {

    private static final Float JPG_QUALITY = 0.8f;
    private static final float GREY_RED = 0.212671f;
    private static final float GREY_GREEN = 0.715160f;
    private static final float GREY_BLUE = 0.072169f;
    private static final int GREY_DIVISOR = 256;
    private static final double SVG_DEFAULT_SCALE = 700;
    private static final String SVG_URL = "http://www.w3.org/2000/svg";
    private final JComponent _viewPanel;
    private static AvailableTypes selectedFileType = AvailableTypes.SVG;
    private static String lastUsedFileName = "";

    private enum AvailableTypes {

        SVG("svg"), SVGZ("svgz"), PDF("pdf"), JPG("jpg"), GIF("gif"), PNG("png");
        private String _displayString;

        AvailableTypes(final String displayString) {
            _displayString = displayString;
        }

        @Override
        public String toString() {
            return _displayString;
        }
    };

    /**
     * Creates new form SaveViewFrame
     */
    public SaveViewFrame(final JFrame parent, final JComponent saveViewPanel) {
        super(parent, true);
        initComponents();
        _viewPanel = saveViewPanel;

        synchronized (this) {

            if (lastUsedFileName.isEmpty()) {
                lastUsedFileName = GlobalFilePathes.DATNAM;
                final int pointIndex = lastUsedFileName.lastIndexOf('.');
                if (pointIndex > 0) {
                    lastUsedFileName = lastUsedFileName.substring(0, pointIndex);
                } else {
                    lastUsedFileName += "image";
                }
                lastUsedFileName += ".svg";
            }
        }

        jTextFieldFileName.setText(lastUsedFileName);


        final ActionListener updateTypeListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                setImageType();
                setCorrectEndingToTextField();
            }
        };

        jRadioButtonPDF.addActionListener(updateTypeListener);
        jRadioButtonSVG.addActionListener(updateTypeListener);
        jRadioButtonSVGZ.addActionListener(updateTypeListener);
        jRadioButtonJPG.addActionListener(updateTypeListener);
        jRadioButtonGIF.addActionListener(updateTypeListener);
        jRadioButtonPNG.addActionListener(updateTypeListener);

        setInitButtonSelected();

        jButtonFileChooser.setIcon(UIManager.getIcon("FileView.directoryIcon"));

        setImageType();
    }

    private void setInitButtonSelected() {
        switch (selectedFileType) {
            case SVG:
                jRadioButtonSVG.setSelected(true);
                break;
            case SVGZ:
                jRadioButtonSVGZ.setSelected(true);
                break;
            case PDF:
                jRadioButtonPDF.setSelected(true);
                break;
            case JPG:
                jRadioButtonJPG.setSelected(true);
                break;
            case GIF:
                jRadioButtonGIF.setSelected(true);
                break;
            case PNG:
                jRadioButtonPNG.setSelected(true);
                break;
            default:
                jRadioButtonSVG.setSelected(true);
        }
    }
    private static final String JPG_TEXT = "\n JPEG - Joint Photographic Experts Group Image:"
            + "\n\n - Rastered Image format with lossy compression"
            + "\n - Very small image size with best compression"
            + "\n - Could produce compression artifacts";
    private static final String PNG_TEXT = "\n PNG - Portable Network Graphics"
            + "\n\n - Rastered Image format with lossless compression"
            + "\n - Acceptable image size";
    private static final String GIF_TEXT = "\n GIF - Graphics Interchange Format"
            + "\n\n - Rastered Image format with lossless compression"
            + "\n - Acceptable image size";
    private static final String SVG_TEXT = "\n SVG - Scalable Vector Graphics"
            + "\n\n - Vector-graphics Image, no compression"
            + "\n - Editable vector-graphics"
            + "\n - Programs for image post-processing: "
            + "\n    - Windows: e.g. Adobe Illustrator"
            + "\n    - Linux: e.g. Inkscape, Gimp";
    private static final String SVGZ_TEXT = "\n SVGZ - Compressed Scalable Vector Graphics"
            + "\n\n - Vector-graphics Image, GZIP compression"
            + "\n - Editable vector-graphics"
            + "\n - Programs for image post-processing: "
            + "\n    - Windows: e.g. Adobe Illustrator"
            + "\n    - Linux: e.g. Inkscape, Gimp"
            + "\n - Advantage to SVG: smaller file size"
            + "\n - Disadvantage: no preview image in browser";
    private static final String PDF_TEXT = "\n PDF - Adobe Portable Document Format"
            + "\n\n - Compressed vector graphics format";

    //CHECKSTYLE:OFF
    @SuppressWarnings("PMD")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jPanelFileType = new javax.swing.JPanel();
        jRadioButtonSVG = new javax.swing.JRadioButton();
        jRadioButtonJPG = new javax.swing.JRadioButton();
        jRadioButtonSVGZ = new javax.swing.JRadioButton();
        jRadioButtonGIF = new javax.swing.JRadioButton();
        jRadioButtonPNG = new javax.swing.JRadioButton();
        jRadioButtonPDF = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        jRadioButtonColor = new javax.swing.JRadioButton();
        jRadioButtonGrayScale = new javax.swing.JRadioButton();
        jLabelScaling = new javax.swing.JLabel();
        jSpinnerScaling = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldFileName = new javax.swing.JTextField();
        jButtonFileChooser = new javax.swing.JButton();
        jButtonCreateImage = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaInfo = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Export Image");
        setLocationByPlatform(true);
        setResizable(false);

        jPanelFileType.setBorder(javax.swing.BorderFactory.createTitledBorder("File format"));
        jPanelFileType.setMinimumSize(new java.awt.Dimension(128, 200));
        jPanelFileType.setLayout(new java.awt.GridLayout(4, 2, 10, 0));

        buttonGroup1.add(jRadioButtonSVG);
        jRadioButtonSVG.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonSVG.setSelected(true);
        jRadioButtonSVG.setText("svg");
        jPanelFileType.add(jRadioButtonSVG);

        buttonGroup1.add(jRadioButtonJPG);
        jRadioButtonJPG.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonJPG.setText("jpg");
        jPanelFileType.add(jRadioButtonJPG);

        buttonGroup1.add(jRadioButtonSVGZ);
        jRadioButtonSVGZ.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonSVGZ.setText("svgz");
        jPanelFileType.add(jRadioButtonSVGZ);

        buttonGroup1.add(jRadioButtonGIF);
        jRadioButtonGIF.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonGIF.setText("gif");
        jPanelFileType.add(jRadioButtonGIF);

        buttonGroup1.add(jRadioButtonPNG);
        jRadioButtonPNG.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonPNG.setText("png");
        jPanelFileType.add(jRadioButtonPNG);

        buttonGroup1.add(jRadioButtonPDF);
        jRadioButtonPDF.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonPDF.setText("pdf");
        jPanelFileType.add(jRadioButtonPDF);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        buttonGroup2.add(jRadioButtonColor);
        jRadioButtonColor.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonColor.setSelected(true);
        jRadioButtonColor.setText("Color");

        buttonGroup2.add(jRadioButtonGrayScale);
        jRadioButtonGrayScale.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jRadioButtonGrayScale.setText("Grayscale");

        jLabelScaling.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelScaling.setText("Scaling:");

        jSpinnerScaling.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jSpinnerScaling.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), Float.valueOf(10.0f), Float.valueOf(0.1f)));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadioButtonGrayScale)
                    .addComponent(jRadioButtonColor))
                .addGap(0, 20, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSpinnerScaling, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelScaling))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jRadioButtonColor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonGrayScale)
                .addGap(28, 28, 28)
                .addComponent(jLabelScaling)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerScaling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel3.setText("Filename:");

        jTextFieldFileName.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTextFieldFileName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextFieldFileNameKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextFieldFileNameKeyTyped(evt);
            }
        });

        jButtonFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFileChooserActionPerformed(evt);
            }
        });

        jButtonCreateImage.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonCreateImage.setText("Create Image");
        jButtonCreateImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCreateImageActionPerformed(evt);
            }
        });

        jButtonCancel.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Information"));

        jScrollPane1.setBorder(null);

        jTextAreaInfo.setEditable(false);
        jTextAreaInfo.setColumns(20);
        jTextAreaInfo.setRows(5);
        jTextAreaInfo.setBorder(null);
        jTextAreaInfo.setCaretColor(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        jTextAreaInfo.setOpaque(false);
        jScrollPane1.setViewportView(jTextAreaInfo);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelFileType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldFileName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonFileChooser)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(143, 143, 143)
                .addComponent(jButtonCreateImage, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelFileType, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonFileChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTextFieldFileName, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCreateImage)
                    .addComponent(jButtonCancel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //CHECKSTYLE:ON

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonFileChooserActionPerformed
        int lastSlashIndex = lastUsedFileName.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            lastSlashIndex = lastUsedFileName.lastIndexOf('\\');
        }

        File parentDirectory = null;
        
        if (lastSlashIndex > 0) {
            final String parentPath = lastUsedFileName.substring(0, lastSlashIndex);
            parentDirectory = new File(parentPath);
        }


        JFileChooser chooser;
        if (parentDirectory != null && parentDirectory.exists()) {
            chooser = new JFileChooser(parentDirectory);
        } else {
            chooser = new JFileChooser();
        }

        final List<String> allEndings = new ArrayList<String>();

        for (AvailableTypes type : AvailableTypes.values()) {
            allEndings.add(type.toString());
        }

        final javax.swing.filechooser.FileFilter fileFilter =
                new ImageFileFilter(allEndings.toArray(new String[allEndings.size()]));
        chooser.setFileFilter(fileFilter);
        final int result = chooser.showSaveDialog(_viewPanel);
        if (result == JFileChooser.CANCEL_OPTION) {
            return;
        }

        final File selectedFile = chooser.getSelectedFile();
        jTextFieldFileName.setText(selectedFile.getAbsolutePath());
        lastUsedFileName = selectedFile.getAbsolutePath();
        setFileTypeFromTextField();
    }//GEN-LAST:event_jButtonFileChooserActionPerformed

    private void jTextFieldFileNameKeyTyped(java.awt.event.KeyEvent evt) {//NOPMD//GEN-FIRST:event_jTextFieldFileNameKeyTyped
    }//GEN-LAST:event_jTextFieldFileNameKeyTyped

    private void jTextFieldFileNameKeyReleased(java.awt.event.KeyEvent evt) {//NOPMD//GEN-FIRST:event_jTextFieldFileNameKeyReleased
        setFileTypeFromTextField();
    }//GEN-LAST:event_jTextFieldFileNameKeyReleased

    private void jButtonCreateImageActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonCreateImageActionPerformed
        final Runnable createRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    jButtonCreateImage.setEnabled(false);
                    jButtonCreateImage.setText("Calculating...");
                    createAndSaveImage();
                } catch (Throwable ex) {
                    JOptionPane.showMessageDialog(null,
                            ex.getMessage(),
                            "Error!",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                } finally {
                    jButtonCreateImage.setEnabled(true);
                    jButtonCreateImage.setText("Create image");
                }
            }
        };

        final Thread thread = new Thread(createRunnable);
        thread.start();

    }//GEN-LAST:event_jButtonCreateImageActionPerformed

    private void setFileTypeFromTextField() {
        lastUsedFileName = jTextFieldFileName.getText();
        setButtonSelectedFromFileType();
        setImageType();
    }

    private void setButtonSelectedFromFileType() {
        switch (selectedFileType) {
            case JPG:
                jRadioButtonJPG.setSelected(true);
                break;
            case SVG:
                jRadioButtonSVG.setSelected(true);
                break;
            case SVGZ:
                jRadioButtonSVGZ.setSelected(true);
                break;
            case PNG:
                jRadioButtonPNG.setSelected(true);
                break;
            case GIF:
                jRadioButtonPNG.setSelected(true);
                break;
            case PDF:
                jRadioButtonPDF.setSelected(true);
                break;
            default:
                assert false;
        }
    }

    private void setCorrectEndingToTextField() {
        final String oldText = jTextFieldFileName.getText();
        StringBuffer newText = new StringBuffer(oldText);

        if (!oldText.endsWith(selectedFileType.toString())) {
            final int pointIndex = oldText.lastIndexOf('.');
            if (pointIndex > 0) {
                newText = new StringBuffer(oldText.substring(0, pointIndex));
            }
            newText.append('.');
            newText.append(selectedFileType);
        }
        jTextFieldFileName.setText(newText.toString());
    }
    //CHECKSTYLE:OFF
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonCreateImage;
    private javax.swing.JButton jButtonFileChooser;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelScaling;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelFileType;
    private javax.swing.JRadioButton jRadioButtonColor;
    private javax.swing.JRadioButton jRadioButtonGIF;
    private javax.swing.JRadioButton jRadioButtonGrayScale;
    private javax.swing.JRadioButton jRadioButtonJPG;
    private javax.swing.JRadioButton jRadioButtonPDF;
    private javax.swing.JRadioButton jRadioButtonPNG;
    private javax.swing.JRadioButton jRadioButtonSVG;
    private javax.swing.JRadioButton jRadioButtonSVGZ;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinnerScaling;
    private javax.swing.JTextArea jTextAreaInfo;
    private javax.swing.JTextField jTextFieldFileName;
    // End of variables declaration//GEN-END:variables
    //CHECKSTYLE:ON

    private void setImageType() {

        if (jRadioButtonSVG.isSelected()) {
            jTextAreaInfo.setText(SVG_TEXT);
            jLabelScaling.setVisible(false);
            jSpinnerScaling.setVisible(false);
            selectedFileType = AvailableTypes.SVG;
        }

        if (jRadioButtonSVGZ.isSelected()) {
            jTextAreaInfo.setText(SVGZ_TEXT);
            jLabelScaling.setVisible(false);
            jSpinnerScaling.setVisible(false);
            selectedFileType = AvailableTypes.SVGZ;
        }



        if (jRadioButtonPDF.isSelected()) {
            jTextAreaInfo.setText(PDF_TEXT);
            jLabelScaling.setVisible(false);
            jSpinnerScaling.setVisible(false);
            selectedFileType = AvailableTypes.PDF;
        }

        if (jRadioButtonJPG.isSelected()) {
            jTextAreaInfo.setText(JPG_TEXT);
            jLabelScaling.setVisible(true);
            jSpinnerScaling.setVisible(true);
            selectedFileType = AvailableTypes.JPG;
        }

        if (jRadioButtonGIF.isSelected()) {
            jTextAreaInfo.setText(GIF_TEXT);
            jLabelScaling.setVisible(true);
            jSpinnerScaling.setVisible(true);
            selectedFileType = AvailableTypes.GIF;
        }

        if (jRadioButtonPNG.isSelected()) {
            jTextAreaInfo.setText(PNG_TEXT);
            jLabelScaling.setVisible(true);
            jSpinnerScaling.setVisible(true);
            selectedFileType = AvailableTypes.PNG;
        }

    }

    private static class ImageFileFilter extends javax.swing.filechooser.FileFilter {

        final List<String> _endings;

        public ImageFileFilter(final String[] initEndings) {
            super();
            _endings = Arrays.asList(initEndings);
        }

        @Override
        public boolean accept(final File file) {
            // Auch Unterverzeichnisse anzeigen
            if (file.isDirectory()) {
                return true;
            }
            for (String end : _endings) {
                if (file.getName().toLowerCase().endsWith(end)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            final StringBuffer returnStrings = new StringBuffer();
            for (String tmp : _endings) {
                returnStrings.append(tmp);
                returnStrings.append(", ");
            }
            return returnStrings.append("  files").toString();
        }
    }

    private void createAndSaveImage() {

        setCorrectEndingToTextField();

        // Get a DOMImplementation.
        final DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();


        // Create an instance of org.w3c.dom.Document.        
        final Document document = domImpl.createDocument(SVG_URL, AvailableTypes.SVG.toString(), null);
        final SVGGraphics2D svgGenerator = createSVGGenerator(document);


        try {

            scaleSvgImageIfNecessary(svgGenerator);

            // not only svg export requires an svg file, the otheres (pdf, ...)
            // are created from an svg, too.
            File svgFile;

            switch (selectedFileType) {
                case GIF:
                    createGifImage();
                    return;
                case SVG:
                    svgFile = new File(jTextFieldFileName.getText());
                    break;
                default:
                    svgFile = File.createTempFile("tmp", "." + AvailableTypes.SVG);
                    svgFile.deleteOnExit();
            }

            // create the svg image:
            _viewPanel.paint(svgGenerator);



            // Finally, stream out SVG to the standard output using
            // UTF-8 encoding.

            if (selectedFileType == AvailableTypes.SVGZ) {
                final OutputStream outStream = new BufferedOutputStream(new FileOutputStream(jTextFieldFileName.getText()));
                createZippedSVGImage(outStream, svgGenerator);
                return;
            }

            streamSVGImageToTempFile(svgFile, svgGenerator);

            if (selectedFileType == AvailableTypes.SVG) {
                return; // here, we don't need to continue anymore!
            }

            // Set the transcoding hints.
            // Create the transcoder input.
            final String svgURI = svgFile.toURL().toString();
            final TranscoderInput input = new TranscoderInput(svgURI);

            // Create the transcoder output.            
            final File transcoderOutputFile = new File(jTextFieldFileName.getText());
            final OutputStream ostream = new BufferedOutputStream(new FileOutputStream(transcoderOutputFile));
            final TranscoderOutput output = new TranscoderOutput(ostream);
            final SVGAbstractTranscoder transcoder = getTranscoder();
            transcoder.transcode(input, output);

            // Flush and close the stream.
            ostream.flush();
            ostream.close();
        } catch (java.lang.OutOfMemoryError err) {
            throw new GeckoRuntimeException("Java out-of-memory. Reducing the worksheet\n"
                    + "size may help to solve the problem.", err);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * for exporting to gray images, we generate our custom SVG generator which
     * overrides the setColor method
     */
    private SVGGraphics2D createSVGGenerator(final Document document) {
        return new SVGGraphics2D(document) {
            @Override
            public void setColor(final Color color) {
                if (jRadioButtonGrayScale.isSelected()) {
                    final float grey = (GREY_RED * color.getRed()
                            + GREY_GREEN * color.getGreen()
                            + GREY_BLUE * color.getBlue())
                            / GREY_DIVISOR;
                    final Color newColor = new Color(grey, grey, grey);
                    super.setColor(newColor);
                } else {
                    super.setColor(color);
                }
            }
        };
    }

    private void scaleSvgImageIfNecessary(final SVGGraphics2D svgGenerator) {
        final float scaling = (Float) jSpinnerScaling.getValue();
        switch (selectedFileType) {
            case PDF:
                break;
            case SVG:
            case SVGZ:
                svgGenerator.scale(SVG_DEFAULT_SCALE / _viewPanel.getWidth(), SVG_DEFAULT_SCALE / _viewPanel.getWidth());
                break;
            default:
                svgGenerator.scale(scaling, scaling);
        }
    }

    private void createGifImage() throws FileNotFoundException, IOException {
        final int height = _viewPanel.getHeight();
        final int width = _viewPanel.getWidth();
        final float scaling = (Float) jSpinnerScaling.getValue();
        final BufferedImage img = new BufferedImage((int) (scaling * width),
                (int) (scaling * height), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = (Graphics2D) img.getGraphics();
        g2d.scale(scaling, scaling);
        // paint white background - otherwise background is set to default black:
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);
        _viewPanel.paint(g2d);
        final FileOutputStream fileOutStream = new FileOutputStream(new File(jTextFieldFileName.getText()));
        ImageIO.write(img, AvailableTypes.GIF.toString(), new BufferedOutputStream(fileOutStream));
        fileOutStream.close();
    }

    private void createZippedSVGImage(final OutputStream outStream, final SVGGraphics2D svgGenerator)
            throws IOException {
        final GZIPOutputStream finalsvgImage = new GZIPOutputStream(outStream);
        final Writer out = new OutputStreamWriter(finalsvgImage, "UTF-8");
        svgGenerator.stream(out, true);
        out.flush();
        out.close();
    }

    private void streamSVGImageToTempFile(final File svgFile, final SVGGraphics2D svgGenerator)
            throws IOException {
        final OutputStream svgOutputStream = new BufferedOutputStream(new FileOutputStream(svgFile));
        final Writer out = new OutputStreamWriter(svgOutputStream, "UTF-8");
        svgGenerator.stream(out, true);
        out.flush();
        out.close();
    }

    private SVGAbstractTranscoder getTranscoder() {
        SVGAbstractTranscoder returnValue;

        final int height = _viewPanel.getHeight();
        final int width = _viewPanel.getWidth();

        final float scaling = (Float) jSpinnerScaling.getValue();
        switch (selectedFileType) {
            case PDF:
                returnValue = new PDFTranscoder();
                break;
            case JPG:
                returnValue = new JPEGTranscoder();
                returnValue.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, JPG_QUALITY);
                break;
            case PNG:
                returnValue = new PNGTranscoder();
                break;
            default:
                assert false : "no transcoder available:  " + selectedFileType;
                return null;
        }
        returnValue.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(scaling * width));
        returnValue.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(scaling * height));
        return returnValue;
    }
}
