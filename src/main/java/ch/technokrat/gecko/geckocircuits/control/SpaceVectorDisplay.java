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

import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JSpinner;

/**
 *
 * @author andy
 */
public class SpaceVectorDisplay extends javax.swing.JFrame {
    static long counter = 0;
    final int ORIGINX = 180;
    final int ORIGINY = 200;
    final double SQRT3 = Math.sqrt(3);
    private double _time;
    private double _old_time;
    private double _timeStep;
    
    
    private BufferedImage doubleBufferImage;
    private final GraphicsConfiguration gfxConf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    Graphics2D offGraph;
    SpaceVector sv1;
    SpaceVector sv2;
    SpaceVector sv3;
    private ReglerSpaceVector reglerSpaceVector;
   

    private class SpaceVector {

        public SpaceVector(Color arrowColor, Color paintColor, JSpinner length, JSpinner average) {
            _arrowColor = arrowColor;
            _paintColor = paintColor;
            _length = length;
            _average = average;
        }
        int HISTORY_BUFFER_SIZE = 100000;
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
        
        private void setSpaceVector(final double r, final double s, double t) {            
            
            double re = 2 / 3.0 * (r - 0.5 * s - 0.5 * t);
            double im = -2 / 3.0 * (+0.5 * SQRT3 * s - 0.5 * SQRT3 * t);
            
            float average = (Float) _average.getValue();
            if( average > 0) {
                averageSpan = (int) (average * 1E-6 / _timeStep);
                if(averageSpan > HISTORY_BUFFER_SIZE) {
                    averageSpan = HISTORY_BUFFER_SIZE;
                }

                averageHistoryRe[(int) counter % HISTORY_BUFFER_SIZE] = (float) re;
                averageHistoryIm[(int) counter % HISTORY_BUFFER_SIZE] = (float) im;
                
                spaceVectorReal = 0;
                spaceVectorImag = 0;

                for(int i = 0; i < averageSpan; i++) {
                    int index = ((int) (counter  + 2 * HISTORY_BUFFER_SIZE - i )) % HISTORY_BUFFER_SIZE;
                    spaceVectorReal += averageHistoryRe[index];
                    spaceVectorImag += averageHistoryIm[index];
                }

                spaceVectorReal /= averageSpan;
                spaceVectorImag /= averageSpan;
                
            } else {
                spaceVectorImag = im;
                spaceVectorReal = re;            
            }
            
            old_spaceVectorRealPos = spaceVectorRealPos;
            old_spaceVectorImagPos = spaceVectorImagPos;

            spaceVectorRealPos = (int) ((Float) _length.getValue() * spaceVectorReal);
            spaceVectorImagPos = (int) ((Float) _length.getValue() * spaceVectorImag);

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

        public void paintArrow(Graphics2D g2d) {
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


    /** Creates new form SpaceVectorDisplay */
    public SpaceVectorDisplay(RegelBlock regelBlock) {
        try { this.setIconImage((new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL,"gecko.gif"))).getImage()); } catch (Exception ex) {}
        initComponents();

        if(regelBlock instanceof ReglerSpaceVector) {
            reglerSpaceVector = (ReglerSpaceVector) regelBlock;
        }

        doubleBufferImage = gfxConf.createCompatibleImage(getWidth(), getHeight());
        offGraph = doubleBufferImage.createGraphics();

        AlphaComposite ac =
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.1f);
        offGraph.setComposite(ac);
        
        offGraph.translate(ORIGINX, ORIGINY);

        clearDisplay();
        sv1 = new SpaceVector(Color.red, Color.red, jSpinnerLength1, jSpinnerAverage1);
        sv2 = new SpaceVector(Color.blue, Color.blue, jSpinnerLength2, jSpinnerAverage2);
        sv3 = new SpaceVector(Color.green, Color.green, jSpinnerLength3, jSpinnerAverage3);
        jRadioButtonLine.setSelected(true);
        
    }        
    

    private void clearDisplay() {

        Color oldColor = offGraph.getColor();        
        for(int i = 0; i < 50; i++) {
            offGraph.setColor(Color.white);
            offGraph.fillRect(-ORIGINX, -ORIGINY, 500, 500);
        }
        offGraph.setColor(oldColor);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
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
        jButtonClear = new javax.swing.JButton();
        jToggleButtonClose = new javax.swing.JToggleButton();
        jLabel2 = new javax.swing.JLabel();
        jSpinnerPauseTime = new javax.swing.JSpinner();
        jPanel3 = new javax.swing.JPanel();
        jSpinnerLength2 = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jSpinnerAverage2 = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jSpinnerLength3 = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jSpinnerAverage3 = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jRadioButtonLine = new javax.swing.JRadioButton();
        jRadioButtonPoint = new javax.swing.JRadioButton();

        setTitle("\" Space Vector Plotter\"");
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
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder(null, new java.awt.Color(254, 1, 1)));

        jSpinnerLength1.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(1.0f)));

        jLabel1.setText("Scaling");

        jSpinnerAverage1.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), null, Float.valueOf(0.1f)));

        jLabel3.setText("Average time [us]");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jSpinnerLength1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSpinnerAverage1, javax.swing.GroupLayout.Alignment.TRAILING))
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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jSpinnerLength2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSpinnerAverage2, javax.swing.GroupLayout.Alignment.TRAILING))
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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder(null, new java.awt.Color(0, 204, 51)));

        jSpinnerLength3.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(1.0f)));

        jLabel6.setText("Scaling");

        jSpinnerAverage3.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), null, Float.valueOf(0.1f)));

        jLabel7.setText("Average time [us]");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jSpinnerLength3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSpinnerAverage3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerLength3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinnerAverage3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                        .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButtonClose)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioButtonLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonPoint)))
                .addGap(79, 79, 79)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSpinnerPauseTime))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE))
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

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//
//        SpaceVectorDisplay svd = new SpaceVectorDisplay();
//        svd.setVisible(true);
//
//        for (int i = 0; i < 4000; i++) {
//            try {
//                double time = i / 10000.0;
//                double r = 20 * Math.sin(100 * time);
//                double s = 20 * Math.sin(100 * time - 2 * Math.PI / 3);
//                double t = 20 * Math.sin(100 * time - 4 * Math.PI / 3);
//                svd.drawVector(time, r,s,t,r,s,t, r, s, t);
//                Thread.sleep(10);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(SpaceVectorDisplay.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        System.exit(-1);
//    }

    private void SpaceVectorPaint(Graphics g) {        

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(doubleBufferImage, 0, 0, this);

        g2d.setColor(Color.GRAY);
        // draw outer circle of diagram
        g2d.drawOval(40, 60, 280, 280);

        // draw re and im axis
        // re axis
        g2d.drawLine(ORIGINX, ORIGINY, 340, ORIGINY);
        g2d.drawPolygon(new int[]{340, 340, 345}, new int[]{ORIGINY-5, ORIGINY+5, ORIGINY}, 3);
        
        // im axis
        g2d.drawLine(ORIGINX, ORIGINY, ORIGINX, 30);
        g2d.drawPolygon(new int[]{ORIGINX-5, ORIGINX+5, ORIGINX}, new int[]{30, 30, 25}, 3);
        
        
        g2d.setFont(new Font(Font.SERIF, 2, 10));
        g2d.drawString("Re", 2 * ORIGINX-20, ORIGINY+20);
        g2d.drawString("Im", ORIGINX+10, 40);

        sv1.paintArrow(g2d);
        sv2.paintArrow(g2d);
        sv3.paintArrow(g2d);

    }

    public void drawVector(double time, double[][] xIN, double dt) {
        counter++;
        _old_time = _time;
        _time = time;
        _timeStep = dt;
        
        int pauseValue = (Integer) jSpinnerPauseTime.getValue();
        if (pauseValue >= 1) {
            try {
                Thread.sleep(pauseValue / 1000, pauseValue % 1000 );
            } catch (InterruptedException ex) {
                Logger.getLogger(SpaceVectorDisplay.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        sv1.setSpaceVector(xIN[0][0], xIN[1][0], xIN[2][0]);
        sv2.setSpaceVector(xIN[3][0], xIN[4][0], xIN[5][0]);
        sv3.setSpaceVector(xIN[6][0], xIN[7][0], xIN[8][0]);

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
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelDisplay;
    private javax.swing.JRadioButton jRadioButtonLine;
    private javax.swing.JRadioButton jRadioButtonPoint;
    public javax.swing.JSpinner jSpinnerAverage1;
    public javax.swing.JSpinner jSpinnerAverage2;
    public javax.swing.JSpinner jSpinnerAverage3;
    public javax.swing.JSpinner jSpinnerLength1;
    public javax.swing.JSpinner jSpinnerLength2;
    public javax.swing.JSpinner jSpinnerLength3;
    public javax.swing.JSpinner jSpinnerPauseTime;
    private javax.swing.JToggleButton jToggleButtonClose;
    // End of variables declaration//GEN-END:variables
}
