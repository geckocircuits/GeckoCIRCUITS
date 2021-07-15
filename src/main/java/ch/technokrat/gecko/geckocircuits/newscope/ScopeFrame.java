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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.allg.SaveViewFrame;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.control.ReglerOSZI;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerIntegralCalculatable;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 *
 * @author Tibor Keresztfalvi
 */
public final class ScopeFrame extends javax.swing.JFrame{
  private GraferV4 _grafer;
  private ReglerOSZI _regelBlockOSZI;
  /**
   * Dialog fuer Zuordnungen SignalZV - Graph.
   */
  private DialogConnectSignalsGraphs _diagCON;
  //---------------------------------
  private DialogScopeCharacteristics _diagAvgRms;
  private DialogFourier _diagFourier;
  //---------------------------------
  private int _windowWidth;
  private int _windowHeight;
  private int _loadedScreenPosX;
  private int _loadedScreenPosY;
  private Point _positionPoint = new Point();
  private final PowerAnalysisSettings _powerAnalysisSettings = new PowerAnalysisSettings();
  private static final int DEFAULT_WIDTH = 600;
  private static final int DEFAULT_HEIGHT = 500;

  // <editor-fold defaultstate="collapsed" desc="AbstractAction for moving an external signal">
  private final class MoveExternalSignal extends AbstractAction{
    GraferV4 _grafer;
    int _keyCode;

    MoveExternalSignal(final GraferV4 grafer, final int keyCode){
      this._grafer = grafer;
      this._keyCode = keyCode;
    }

    @Override
    public void actionPerformed(final ActionEvent e){
      double minMaxXDiff = 0, minMaxYDiff = 0;
      int xAxisLength, yAxisLength, tmpIndex;
      ExternalSignal externalSignal; // external signal for which the offset should be modified
      AbstractCurve curve = this._grafer.getSelectedCurve();
      if(this._grafer.getSelectedCurve() == null){
        return;
      }
      tmpIndex = this._grafer.getSelectedCurve().getValueDataIndex();
      if(!(this._grafer._manager.getAllScopeSignals().get(tmpIndex) instanceof ExternalSignal)){
        return;
      }
      externalSignal = (ExternalSignal)this._grafer._manager.getAllScopeSignals().get(tmpIndex);
      minMaxXDiff = curve._xAxis._axisMinMax.getLimits().getIntervalRange();
      minMaxYDiff = curve._yAxis._axisMinMax.getLimits().getIntervalRange();
      xAxisLength = curve._xAxis.getAxisLengthPixel();
      yAxisLength = curve._yAxis.getAxisLengthPixel();

      switch(this._keyCode){
        case KeyEvent.VK_RIGHT:
          externalSignal.moveBy(minMaxXDiff / xAxisLength, 0);
          break;
        case KeyEvent.VK_LEFT:
          externalSignal.moveBy(-minMaxXDiff / xAxisLength, 0);
          break;
        case KeyEvent.VK_UP:
          externalSignal.moveBy(0, minMaxYDiff / yAxisLength);
          break;
        case KeyEvent.VK_DOWN:
          externalSignal.moveBy(0, -minMaxYDiff / yAxisLength);
          break;
        case KeyEvent.VK_DELETE:
          externalSignal.moveTo(0, 0);
          break;
        default:
          break;
      }
      for(AbstractDiagram diag : this._grafer._manager.getDiagrams()){
        if(diag.getCurves().get(tmpIndex).getAxisConnection() != AxisConnection.ZUORDNUNG_NIX){
          diag.loadDataFromContainer(diag._grafer.getDataContainer());
        }
      }
      this._grafer.repaint();
    }
  }
  // </editor-fold>

  public ScopeFrame(final GraferV4 grafer){
    this._grafer = grafer;
    
    // add CTRL+arrow keys to the actionmap to move around selected external signals
    JComponent components = (JComponent)this.getContentPane();
    ActionMap actionMap = components.getActionMap();
    InputMap inputMap = components.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK, false), "moveExternalSignal" + KeyEvent.VK_LEFT);
    actionMap.put("moveExternalSignal" + KeyEvent.VK_LEFT, new MoveExternalSignal(grafer, KeyEvent.VK_LEFT));
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK, false), "moveExternalSignal" + KeyEvent.VK_RIGHT);
    actionMap.put("moveExternalSignal" + KeyEvent.VK_RIGHT, new MoveExternalSignal(grafer, KeyEvent.VK_RIGHT));
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK, false), "moveExternalSignal" + KeyEvent.VK_UP);
    actionMap.put("moveExternalSignal" + KeyEvent.VK_UP, new MoveExternalSignal(grafer, KeyEvent.VK_UP));
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK, false), "moveExternalSignal" + KeyEvent.VK_DOWN);
    actionMap.put("moveExternalSignal" + KeyEvent.VK_DOWN, new MoveExternalSignal(grafer, KeyEvent.VK_DOWN));
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK, true), "moveExternalSignal" + KeyEvent.VK_DELETE);
    actionMap.put("moveExternalSignal" + KeyEvent.VK_DELETE, new MoveExternalSignal(grafer, KeyEvent.VK_DELETE));


    initComponents();

    try{
      this.setIconImage(new ImageIcon(new URL(GlobalFilePathes.PFAD_PICS_URL, "gecko.gif")).getImage());
    }catch(Exception e){
      e.printStackTrace();
    }

    this.addComponentListener(
            new java.awt.event.ComponentAdapter(){
      @Override
      public void componentResized(final ComponentEvent evt){
        _windowWidth = getWidth();
        _windowHeight = getHeight();
      }

      @Override
      public void componentMoved(final ComponentEvent evt){
        if(isVisible()){
          _positionPoint = getLocationOnScreen();
        }
      }
    });

  }

  public GraferV4 getGrafer(){
    return this._grafer;
  }

  public void exportIndividualCONTROL(final StringBuffer ascii){
    _scope.exportInvidualControl(ascii);
    _powerAnalysisSettings.exportIndividualControl(ascii);
    DatenSpeicher.appendAsString(ascii.append("\nwindowWidth"), _windowWidth);
    DatenSpeicher.appendAsString(ascii.append("\nwindowHeight"), _windowHeight);
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    DatenSpeicher.appendAsString(ascii.append("\nsaveScreenWidth"), screenSize.width);
    DatenSpeicher.appendAsString(ascii.append("\nsaveScreenHeight"), screenSize.height);
    DatenSpeicher.appendAsString(ascii.append("\nsaveScreenPosX"), _positionPoint.x);
    DatenSpeicher.appendAsString(ascii.append("\nsaveScreenPosY"), _positionPoint.y);
  }

  public void importIndividualCONTROL(final TokenMap settingsMap){
    _windowWidth = DEFAULT_WIDTH;
    _windowWidth = settingsMap.readDataLine("windowWidth", _windowWidth);
    _windowHeight = DEFAULT_HEIGHT;
    _windowHeight = settingsMap.readDataLine("windowHeight", _windowHeight);

    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final int loadedScreenWidth = settingsMap.readDataLine("saveScreenWidth", 1024);
    final int loadedScreenHeight = settingsMap.readDataLine("saveScreenHeight", 800);

    if(screenSize.height >= _windowHeight && screenSize.width >= _windowWidth){
      setSize(_windowWidth, _windowHeight);

      if(loadedScreenWidth == screenSize.width && loadedScreenHeight == screenSize.height){
        _loadedScreenPosX = settingsMap.readDataLine("saveScreenPosX", 0);
        _loadedScreenPosY = settingsMap.readDataLine("saveScreenPosY", 0);
        this._positionPoint = new Point(_loadedScreenPosX, _loadedScreenPosY);
        this.setLocation(_loadedScreenPosX, _loadedScreenPosY);
      }
    }

    this.setPreferredSize(new Dimension(_windowWidth, _windowHeight));
    _powerAnalysisSettings.importIndividualControl(settingsMap);
    _scope.importIndividualCONTROL(settingsMap);
  }
  
  public void setReferenzAufRegelBlock(final RegelBlock regelBlockOSZI){
    _regelBlockOSZI = (ReglerOSZI)regelBlockOSZI;
  }

  public void setNewTerminalNumber(final int terminalNumber){
    _grafer.doZoomAutoFit();
  }

  public void saveZVData(final String fileName){
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void clearZVDaten(){
    //scope.worksheet.clear();
  }

  /**
   * Wenn die Simulation gerade laeuft, werden alle Menus deaktiviert, damit man
   * dort nicht herumpfuschen kann.
   *
   * @param simStarted true if simulation started, false otherwise
   */
  public void setScopeMenueEnabled(final boolean simStarted){
    if(simStarted){
      jMenuItemInitAndStart.setEnabled(false);
      jMenuItemParameter.setEnabled(false);
      jMenuItemContinue.setEnabled(false);
      jMenuItemPause.setEnabled(true);
      jMenuScopeData.setEnabled(false);
      jMenuGraphs.setEnabled(false);
      jMenuAnalysis.setEnabled(false);
      // Worksheet-Daten waehrend der Simulation nicht zugaenglich:            
    }else{
      jMenuItemInitAndStart.setEnabled(true);
      jMenuItemParameter.setEnabled(true);
      jMenuItemContinue.setEnabled(true);
      jMenuItemPause.setEnabled(false);
      jMenuScopeData.setEnabled(true);
      jMenuGraphs.setEnabled(true);
      jMenuAnalysis.setEnabled(true);
    }
  }

  @Override
  public void setVisible(final boolean b){
    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(_scope, javax.swing.GroupLayout.DEFAULT_SIZE, 739, Short.MAX_VALUE));
    layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(_scope, javax.swing.GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE));

    pack();

    super.setVisible(b);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  //CHECKSTYLE:OFF
  @SuppressWarnings("PMD")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    _scope = new ch.technokrat.gecko.geckocircuits.newscope.NewScope(_grafer);
    jMenuBar = new javax.swing.JMenuBar();
    jMenuScopeData = new javax.swing.JMenu();
    jMenuItemSaveData = new javax.swing.JMenuItem();
    jMenuItemSaveViewAsImage = new javax.swing.JMenuItem();
    jMenuItemScopeSettings = new javax.swing.JMenuItem();
    jMenuItemExit = new javax.swing.JMenuItem();
    jMenuGraphs = new javax.swing.JMenu();
    jMenuItemSignalGraph = new javax.swing.JMenuItem();
    jMenuItemAvgSgn = new javax.swing.JMenuItem();
    jMenuItem1 = new javax.swing.JMenuItem();
    jMenuAnalysis = new javax.swing.JMenu();
    jMenuItemCharacteristics = new javax.swing.JMenuItem();
    jMenuItemFourier = new javax.swing.JMenuItem();
    jMenuSimulation = new javax.swing.JMenu();
    jMenuItemParameter = new javax.swing.JMenuItem();
    jMenuItemInitAndStart = new javax.swing.JMenuItem();
    jMenuItemPause = new javax.swing.JMenuItem();
    jMenuItemContinue = new javax.swing.JMenuItem();

    setLocationByPlatform(true);
    setMinimumSize(new java.awt.Dimension(640, 500));

    jMenuScopeData.setText("Data");
    jMenuScopeData.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuScopeDataActionPerformed(evt);
      }
    });

    jMenuItemSaveData.setText("Save Data");
    jMenuItemSaveData.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemSaveDataActionPerformed(evt);
      }
    });
    jMenuScopeData.add(jMenuItemSaveData);

    jMenuItemSaveViewAsImage.setText("Save View As Image");
    jMenuItemSaveViewAsImage.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemSaveViewAsImageActionPerformed(evt);
      }
    });
    jMenuScopeData.add(jMenuItemSaveViewAsImage);

    jMenuItemScopeSettings.setText("Scope Settings");
    jMenuItemScopeSettings.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemScopeSettingsActionPerformed(evt);
      }
    });
    jMenuScopeData.add(jMenuItemScopeSettings);

    jMenuItemExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemExit.setText("Exit");
    jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemExitActionPerformed(evt);
      }
    });
    jMenuScopeData.add(jMenuItemExit);

    jMenuBar.add(jMenuScopeData);

    jMenuGraphs.setText("Graphs");

    jMenuItemSignalGraph.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemSignalGraph.setText("Signal - Graph");
    jMenuItemSignalGraph.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemSignalGraphActionPerformed(evt);
      }
    });
    jMenuGraphs.add(jMenuItemSignalGraph);

    jMenuItemAvgSgn.setText("Define Average Signal");
    jMenuItemAvgSgn.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemAvgSgnActionPerformed(evt);
      }
    });
    jMenuGraphs.add(jMenuItemAvgSgn);

    jMenuItem1.setText("Define calculation");
    jMenuItem1.setEnabled(false);
    jMenuGraphs.add(jMenuItem1);

    jMenuBar.add(jMenuGraphs);

    jMenuAnalysis.setText("Analysis");

    jMenuItemCharacteristics.setText("Characteristics");
    jMenuItemCharacteristics.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemCharacteristicsActionPerformed(evt);
      }
    });
    jMenuAnalysis.add(jMenuItemCharacteristics);

    jMenuItemFourier.setText("Fourier");
    jMenuItemFourier.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemFourierActionPerformed(evt);
      }
    });
    jMenuAnalysis.add(jMenuItemFourier);

    jMenuBar.add(jMenuAnalysis);

    jMenuSimulation.setText("Simulation");

    jMenuItemParameter.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
    jMenuItemParameter.setText("Parameter");
    jMenuItemParameter.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemParameterActionPerformed(evt);
      }
    });
    jMenuSimulation.add(jMenuItemParameter);

    jMenuItemInitAndStart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
    jMenuItemInitAndStart.setText("Init & Start");
    jMenuItemInitAndStart.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemInitAndStartActionPerformed(evt);
      }
    });
    jMenuSimulation.add(jMenuItemInitAndStart);

    jMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
    jMenuItemPause.setText("Pause");
    jMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemPauseActionPerformed(evt);
      }
    });
    jMenuSimulation.add(jMenuItemPause);

    jMenuItemContinue.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
    jMenuItemContinue.setText("Continue");
    jMenuItemContinue.setEnabled(false);
    jMenuItemContinue.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemContinueActionPerformed(evt);
      }
    });
    jMenuSimulation.add(jMenuItemContinue);

    jMenuBar.add(jMenuSimulation);

    setJMenuBar(jMenuBar);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(_scope, javax.swing.GroupLayout.DEFAULT_SIZE, 739, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(_scope, javax.swing.GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE)
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents
  //CHECKSTYLE:ON

private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemExitActionPerformed
  this.dispose();
}//GEN-LAST:event_jMenuItemExitActionPerformed

private void jMenuItemSaveViewAsImageActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemSaveViewAsImageActionPerformed
    new SaveViewFrame(this, _grafer).setVisible(true);
}//GEN-LAST:event_jMenuItemSaveViewAsImageActionPerformed

private void jMenuItemSignalGraphActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemSignalGraphActionPerformed
  if(_diagCON != null){
    _diagCON.dispose();
  }
  _diagCON = new DialogConnectSignalsGraphs(_scope._grafer.get(0));
  _diagCON.setVisible(true);
}//GEN-LAST:event_jMenuItemSignalGraphActionPerformed

private void jMenuItemCharacteristicsActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemCharacteristicsActionPerformed
  final AbstractDataContainer ramData = _regelBlockOSZI.getZVDatenImRAM();
  if(ramData == null){
    return;
  }

  _diagAvgRms = new DialogScopeCharacteristics(this, ramData,
                                               _powerAnalysisSettings, _grafer._sliderContainer.getSliderXRange());
  _diagAvgRms.setLocationByPlatform(true);
  _diagAvgRms.setVisible(true);

}//GEN-LAST:event_jMenuItemCharacteristicsActionPerformed

private void jMenuItemFourierActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemFourierActionPerformed
  final AbstractDataContainer ramData = _regelBlockOSZI.getZVDatenImRAM();
  if(ramData == null){
    return;
  }
  _diagFourier = new DialogFourier(ramData, _grafer._sliderContainer.getSliderXRange(), _scope);
  _diagFourier.setLocationByPlatform(true);
  _diagFourier.setVisible(true);

}//GEN-LAST:event_jMenuItemFourierActionPerformed

private void jMenuItemParameterActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemParameterActionPerformed
  GeckoSim._win.openParameterMenu(this);
}//GEN-LAST:event_jMenuItemParameterActionPerformed

private void jMenuItemInitAndStartActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemInitAndStartActionPerformed
    GeckoSim._win.initStartWithErrorDialogMessage();
}//GEN-LAST:event_jMenuItemInitAndStartActionPerformed

private void jMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemPauseActionPerformed
  GeckoSim._win.pauseSimulation();
}//GEN-LAST:event_jMenuItemPauseActionPerformed

private void jMenuItemContinueActionPerformed(java.awt.event.ActionEvent evt) {//NOPMD//GEN-FIRST:event_jMenuItemContinueActionPerformed
  try{
        GeckoSim._win.continueCalculationWithPossibleErrorMessage();
  }catch(RuntimeException re){
    re.printStackTrace();
  }
}//GEN-LAST:event_jMenuItemContinueActionPerformed

    private void jMenuItemSaveDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveDataActionPerformed
      JOptionPane.showMessageDialog(this, "This menu option was removed. Please use the 'Export Data To File' \ncontrol"
              + "block, which you can find in the 'Source/ Sink' control menue.", "Information",
                                    JOptionPane.INFORMATION_MESSAGE);

    }//GEN-LAST:event_jMenuItemSaveDataActionPerformed

    private void jMenuItemAvgSgnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAvgSgnActionPerformed
      final DialogDefineAvg dialog = new DialogDefineAvg(this, true,
                                                         (DataContainerIntegralCalculatable)_regelBlockOSZI.getZVDatenImRAM());
      dialog.setVisible(true);
    }//GEN-LAST:event_jMenuItemAvgSgnActionPerformed

    private void jMenuScopeDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuScopeDataActionPerformed
    }//GEN-LAST:event_jMenuScopeDataActionPerformed

    private void jMenuItemScopeSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemScopeSettingsActionPerformed
      DialogScopeSettings dialogSettings = new DialogScopeSettings(this, _regelBlockOSZI, true);
      dialogSettings.setVisible(true);
    }//GEN-LAST:event_jMenuItemScopeSettingsActionPerformed
  // Variables declaration - do not modify//GEN-BEGIN:variables
  public ch.technokrat.gecko.geckocircuits.newscope.NewScope _scope;
  private javax.swing.JMenu jMenuAnalysis;
  private javax.swing.JMenuBar jMenuBar;
  private javax.swing.JMenu jMenuGraphs;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItemAvgSgn;
  private javax.swing.JMenuItem jMenuItemCharacteristics;
  private javax.swing.JMenuItem jMenuItemContinue;
  private javax.swing.JMenuItem jMenuItemExit;
  private javax.swing.JMenuItem jMenuItemFourier;
  private javax.swing.JMenuItem jMenuItemInitAndStart;
  private javax.swing.JMenuItem jMenuItemParameter;
  private javax.swing.JMenuItem jMenuItemPause;
  private javax.swing.JMenuItem jMenuItemSaveData;
  private javax.swing.JMenuItem jMenuItemSaveViewAsImage;
  private javax.swing.JMenuItem jMenuItemScopeSettings;
  private javax.swing.JMenuItem jMenuItemSignalGraph;
  private javax.swing.JMenu jMenuScopeData;
  private javax.swing.JMenu jMenuSimulation;
  // End of variables declaration//GEN-END:variables
}
