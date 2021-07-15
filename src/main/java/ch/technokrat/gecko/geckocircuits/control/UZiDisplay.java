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
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;

import javax.swing.ImageIcon;
import java.net.URL;

/*
 * SpaceVectorDisplay.java
 *
 * Created on 27.03.2009, 23:30:02
 */
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JSpinner;

public class UZiDisplay extends GeckoDialog {

    static long counter = 0;
    static final int ORIGINX = 180;
    static final int ORIGINY = 200;
    final double SQRT3 = Math.sqrt(3);
    private double _time;
    private double _old_time;
    private double _timeStep;
    private BufferedImage doubleBufferImage;
    private final GraphicsConfiguration gfxConf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    Graphics2D offGraph;
    DrawVector sv1;
    DrawVector sv2;
    

    private class DrawVector {

        private final JSpinner _zSpinner;

        public DrawVector(Color arrowColor, Color paintColor, JSpinner length, JSpinner average, JSpinner zSpinner) {
            _arrowColor = arrowColor;
            _paintColor = paintColor;
            _length = length;
            _zSpinner = zSpinner;
            _average = average;
        }
        final int HISTORY_BUFFER_SIZE = 10000;
        float[] averageHistoryRe = new float[HISTORY_BUFFER_SIZE];
        float[] averageHistoryIm = new float[HISTORY_BUFFER_SIZE];
        private Color _arrowColor;
        private Color _paintColor;
        private double spaceVectorReal;
        private int spaceVectorRealPos = 0;
        private int old_spaceVectorRealPos = 0;
        private double spaceVectorImag;
        private int spaceVectorImagPos = 0;
        private int old_spaceVectorImagPos = 0;
        private JSpinner _length;
        private JSpinner _average;
        private int averageSpan = 1;

        private void setPlotVector(double u, double current) {

            u *= 140;
            current *= -140;
            float average = (Float) _average.getValue();
            if (average > 0) {
                averageSpan = (int) (average * 1E-6 / _timeStep);
                averageHistoryRe[(int) counter % HISTORY_BUFFER_SIZE] = (float) u;
                averageHistoryIm[(int) counter % HISTORY_BUFFER_SIZE] = (float) current;

                spaceVectorReal = 0;
                spaceVectorImag = 0;

                for (int i = 0; i < averageSpan; i++) {
                    spaceVectorReal += averageHistoryRe[ ((int) (counter + HISTORY_BUFFER_SIZE - i)) % HISTORY_BUFFER_SIZE];
                    spaceVectorImag += averageHistoryIm[ ((int) (counter + HISTORY_BUFFER_SIZE - i)) % HISTORY_BUFFER_SIZE];
                }

                spaceVectorReal /= averageSpan;
                spaceVectorImag /= averageSpan;

            } else {
                spaceVectorImag = current;
                spaceVectorReal = u;
            }

            old_spaceVectorRealPos = spaceVectorRealPos;
            old_spaceVectorImagPos = spaceVectorImagPos;

            spaceVectorRealPos = (int) ((Float) _length.getValue() * spaceVectorReal);
            spaceVectorImagPos = (int) ((Float) _length.getValue() * spaceVectorImag * (Float) _zSpinner.getValue());

            if (old_spaceVectorImagPos != spaceVectorImagPos || old_spaceVectorRealPos != spaceVectorRealPos) {
                offGraph.setColor(_paintColor);

                if (jRadioButtonLine.isSelected()) {
                    offGraph.drawLine(spaceVectorRealPos, spaceVectorImagPos, old_spaceVectorRealPos, old_spaceVectorImagPos);
                }
                if (jRadioButtonPoint.isSelected()) {
                    offGraph.drawRect(spaceVectorRealPos, spaceVectorImagPos, 1, 1);
                }
            }

            jPanelDisplay.repaint();
        }

        public void paintArrow(final Graphics2D g2d) {
            double angle = Math.atan2(spaceVectorImag, spaceVectorReal);
            g2d.setColor(_arrowColor);

            g2d.translate(ORIGINX, ORIGINY);
            Polygon arrowHead = new Polygon(new int[]{
                spaceVectorRealPos,
                (int) (-3 * Math.sin(angle) - 5 * Math.cos(angle) + spaceVectorRealPos),
                (int) (3 * Math.sin(angle) - 5 * Math.cos(angle) + spaceVectorRealPos)},
                    new int[]{
                spaceVectorImagPos,
                (int) (-5 * Math.sin(angle) + 3 * Math.cos(angle) + spaceVectorImagPos),
                (int) (-5 * Math.sin(angle) - 3 * Math.cos(angle) + spaceVectorImagPos)
            }, 3);

            g2d.drawLine(0, 0, spaceVectorRealPos, spaceVectorImagPos);

            g2d.fillPolygon(arrowHead);
            g2d.setColor(Color.black);
            g2d.drawPolygon(arrowHead);

            g2d.translate(-ORIGINX, -ORIGINY);
        }
    }

    /**
     * Creates new form SpaceVectorDisplay
     */
    public UZiDisplay(final ReglerU_ZI regelBlock) {
        super(GeckoSim._win, false);
                
        initComponents();
        

        doubleBufferImage = gfxConf.createCompatibleImage(getWidth(), getHeight());
        offGraph = doubleBufferImage.createGraphics();

        AlphaComposite alphaComposite =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
        offGraph.setComposite(alphaComposite);

        offGraph.translate(ORIGINX, ORIGINY);

        clearDisplay();
        sv1 = new DrawVector(Color.red, Color.red, jSpinnerLength1, jSpinnerAverage1, jSpinnerZ1);
        sv2 = new DrawVector(Color.blue, Color.blue, jSpinnerLength2, jSpinnerAverage2, jSpinnerZ2);

    }

    private void clearDisplay() {

        Color oldColor = offGraph.getColor();
        for (int i = 0; i < 50; i++) {
            offGraph.setColor(Color.white);
            offGraph.fillRect(-ORIGINX, -ORIGINY, 500, 500);
        }
        offGraph.setColor(oldColor);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanelDisplay = new javax.swing.JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                SpaceVectorPaint(g);
            }
        }; ;
        jPanel2 = new javax.swing.JPanel();
        jSpinnerLength1 = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jSpinnerAverage1 = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jSpinnerZ1 = new javax.swing.JSpinner();
        jButtonClear = new javax.swing.JButton();
        jToggleButtonClose = new javax.swing.JToggleButton();
        jLabel2 = new javax.swing.JLabel();
        jSpinnerPauseTime = new javax.swing.JSpinner();
        jPanel3 = new javax.swing.JPanel();
        jSpinnerLength2 = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jSpinnerAverage2 = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jSpinnerZ2 = new javax.swing.JSpinner();
        jRadioButtonLine = new javax.swing.JRadioButton();
        jRadioButtonPoint = new javax.swing.JRadioButton();

        setTitle("\"U-Z*i Plotter\"");
        setLocationByPlatform(true);
        setResizable(false);

        jPanelDisplay.setBackground(new java.awt.Color(255, 255, 255));
        jPanelDisplay.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanelDisplayLayout = new javax.swing.GroupLayout(jPanelDisplay);
        jPanelDisplay.setLayout(jPanelDisplayLayout);
        jPanelDisplayLayout.setHorizontalGroup(
            jPanelDisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 382, Short.MAX_VALUE)
        );
        jPanelDisplayLayout.setVerticalGroup(
            jPanelDisplayLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 444, Short.MAX_VALUE)
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder(null, new java.awt.Color(254, 1, 1)));

        jSpinnerLength1.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(1.0f)));

        jLabel1.setText("Scaling");

        jSpinnerAverage1.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), null, Float.valueOf(0.1f)));

        jLabel3.setFont(jLabel3.getFont());
        jLabel3.setText("Average time [us]");

        jLabel6.setText("Impedance Z");

        jSpinnerZ1.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(0.1f)));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jSpinnerLength1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(jSpinnerAverage1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(jSpinnerZ1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerLength1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerAverage1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerZ1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jToggleButtonClose.setText("Close");
        jToggleButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonCloseActionPerformed(evt);
            }
        });

        jLabel2.setText("Pause time [us]");

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder(null, new java.awt.Color(51, 51, 255)));

        jSpinnerLength2.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(1.0f)));

        jLabel4.setText("Scaling");

        jSpinnerAverage2.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), null, Float.valueOf(0.1f)));

        jLabel5.setText("Average time [us]");

        jLabel7.setText("Impedance Z");

        jSpinnerZ2.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(0.1f)));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jSpinnerLength2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(jSpinnerAverage2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSpinnerZ2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerLength2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerAverage2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerZ2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonGroup1.add(jRadioButtonLine);
        jRadioButtonLine.setSelected(true);
        jRadioButtonLine.setText("Lines");

        buttonGroup1.add(jRadioButtonPoint);
        jRadioButtonPoint.setText("Points");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButtonClose)
                        .addGap(16, 16, 16)
                        .addComponent(jRadioButtonLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonPoint)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 85, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSpinnerPauseTime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                        .addComponent(jLabel2))
                    .addComponent(jPanelDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonClear)
                    .addComponent(jToggleButtonClose)
                    .addComponent(jSpinnerPauseTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRadioButtonLine)
                    .addComponent(jRadioButtonPoint))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jButtonClearActionPerformed
        clearDisplay();
        repaint();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private void jToggleButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jToggleButtonCloseActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_jToggleButtonCloseActionPerformed


    private void SpaceVectorPaint(final Graphics graphics) {

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(doubleBufferImage, 0, 0, this);

        g2d.setColor(Color.GRAY);
        // draw outer circle of diagram
        g2d.drawOval(40, 60, 280, 280);

        // draw re and im axis
        // re axis
        g2d.drawLine(ORIGINX, ORIGINY, 340, ORIGINY);
        g2d.drawPolygon(new int[]{340, 340, 345}, new int[]{ORIGINY - 5, ORIGINY + 5, ORIGINY}, 3);

        // im axis
        g2d.drawLine(ORIGINX, ORIGINY, ORIGINX, 30);
        g2d.drawPolygon(new int[]{ORIGINX - 5, ORIGINX + 5, ORIGINX}, new int[]{30, 30, 25}, 3);


        g2d.setFont(new Font(Font.SERIF, 2, 10));
        g2d.drawString("U", 2 * ORIGINX - 20, ORIGINY + 20);
        g2d.drawString("Z*i", ORIGINX + 10, 40);

        sv1.paintArrow(g2d);
        sv2.paintArrow(g2d);

    }

    void drawVector(final double time, final double[][] xIN) {
        counter++;
        _old_time = _time;
        _time = time;
        _timeStep = _time - _old_time;

        int pauseValue = (Integer) jSpinnerPauseTime.getValue();
        if (pauseValue >= 1) {
            try {
                Thread.sleep(pauseValue / 1000, pauseValue % 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpaceVectorDisplay.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        sv1.setPlotVector(xIN[0][0], xIN[1][0]);
        sv2.setPlotVector(xIN[2][0], xIN[3][0]);

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelDisplay;
    private javax.swing.JRadioButton jRadioButtonLine;
    private javax.swing.JRadioButton jRadioButtonPoint;
    public javax.swing.JSpinner jSpinnerAverage1;
    public javax.swing.JSpinner jSpinnerAverage2;
    public javax.swing.JSpinner jSpinnerLength1;
    public javax.swing.JSpinner jSpinnerLength2;
    public javax.swing.JSpinner jSpinnerPauseTime;
    public javax.swing.JSpinner jSpinnerZ1;
    public javax.swing.JSpinner jSpinnerZ2;
    private javax.swing.JToggleButton jToggleButtonClose;
    // End of variables declaration//GEN-END:variables
}
