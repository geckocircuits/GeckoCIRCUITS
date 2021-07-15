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

import ch.technokrat.gecko.GeckoCustomRemote;
import ch.technokrat.gecko.GeckoRemoteRegistry;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationsKern;
import ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerValuesSettable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GeckoStatusBar extends JPanel {

    private final JButton _portLabelButton = new JButton();
    private final JLabel _jLabelSimulationStatus = new JLabel();
    private boolean _aliasing;
    //final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.3f);
    //            g2d.setComposite(alphaComposite);
    private long _totalMemMB;
    private static final Color MEM_STRING_COLOR = Color.gray;
    private int _memBarPixelWidth = 0;
    private long _usedMemMB;
    private int _dataMemMB;
    private static final int MBYTE = 1050000;
    private TechFormat cf = new TechFormat();
    private static MemoryWarning _memoryWarning;
    private static boolean _showMemoryWarning = true;
    private double _runningPercentage;
    private final Fenster _fenster;
    private final GeckoProgressPanel _jPanelProgress;
    private Font font = calculateLabelFont();

    GeckoStatusBar(String ti_ReadySim, final Fenster fenster) {
        _fenster = fenster;
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    _portLabelButton.setVisible(GeckoRemoteRegistry.isRemoteEnabled());

                    if (GeckoRemoteRegistry.isRemoteEnabled()) {
                        String pong = GeckoCustomRemote.pingRemoteClient();
                        if (pong != null) {
                            _portLabelButton.setText(I18nKeys.CONNECTED.getTranslation() + " : " + GeckoRemoteRegistry.getRemoteAccessPort());
                            _portLabelButton.setToolTipText(pong);
                            _portLabelButton.setForeground(GlobalColors.farbeGecko);
                        } else {
                            if (GeckoCustomRemote.clients == null || GeckoCustomRemote.clients.isEmpty()) {
                                _portLabelButton.setText(I18nKeys.LISTENING_AT_PORT.getTranslation() + " : " + GeckoRemoteRegistry.getRemoteAccessPort());
                                _portLabelButton.setForeground(Color.ORANGE);
                                _portLabelButton.setToolTipText("GeckoREMOTE " + I18nKeys.LISTENING_AT_PORT + " " + GeckoRemoteRegistry.getRemoteAccessPort());
                            } else {
                                _portLabelButton.setText(I18nKeys.CONNECTION_TEST_FAILED.LISTENING_AT_PORT.getTranslation() + " : " + GeckoRemoteRegistry.getRemoteAccessPort());
                                _portLabelButton.setForeground(Color.RED);
                            }
                        }
                    }

                    repaint();
                } catch (Throwable error) {
                    error.printStackTrace();
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 0, 2000);
        setLayout(new BorderLayout());

        _jLabelSimulationStatus.setText(ti_ReadySim);
        _jLabelSimulationStatus.setMinimumSize(new Dimension(125, 5));
        _jLabelSimulationStatus.setPreferredSize(new Dimension(125, 5));


        this.add(_jLabelSimulationStatus, BorderLayout.WEST);
        _jLabelSimulationStatus.setFont(font);

        _jPanelProgress = new GeckoProgressPanel();
        this.add(_jPanelProgress, BorderLayout.CENTER);

        _portLabelButton.setVisible(false);
        _portLabelButton.setOpaque(false);
        _portLabelButton.setContentAreaFilled(false);
        _portLabelButton.setFocusPainted(false);
        _portLabelButton.setBorder(null);

        _portLabelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DialogRemotePort(GeckoSim._win, true).setVisible(true);
            }
        });
        this.add(_portLabelButton, BorderLayout.EAST);

        setOpaque(false);
    }

    private Font calculateLabelFont() {
        Font oldFont = _jLabelSimulationStatus.getFont();
        font = new Font(_jLabelSimulationStatus.getFont().getFontName(),
                oldFont.getStyle(), oldFont.getSize() - 2);

        return font;
    }

    private class GeckoProgressPanel extends JPanel {

        public GeckoProgressPanel() {
            super();
            setOpaque(true);
        }

        @Override
        public void paint(Graphics graphics) {

            SimulationsKern simKern = _fenster._simRunner.simKern;
            if (simKern != null) {
                switch (simKern._simulationStatus) {
                    case PAUSED:
                    case RUNNING:
                        double prozentFertig = 100 * (simKern.getZeitAktuell()
                                - simKern.getTSTART()) / (simKern.getTEND() - simKern.getTSTART());
                        _runningPercentage = prozentFertig;
                        _jLabelSimulationStatus.setText("Running ...  " + cf.formatT(prozentFertig, TechFormat.FORMAT_AUTO) + " %");
                        break;
                    case FINISHED:
                        break;

                    case NOT_INIT:
                        break;
                }

            }

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.setColor(Color.white);

            //g2d.fillRect(-1, -1, getWidth() + 3, getHeight() + 2);

            if (_aliasing) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            } else {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }

            g2d.setFont(font);
            _memBarPixelWidth = getWidth();

            drawTotalMem(g2d);
            drawUsedMem(g2d);
            drawDataMem(g2d);
            g2d.setColor(Color.BLACK);
        }

        private void drawTotalMem(final Graphics2D g2d) {
            _totalMemMB = Runtime.getRuntime().maxMemory() / MBYTE;
            _usedMemMB = Runtime.getRuntime().totalMemory() / MBYTE;

            if (_usedMemMB * 1.3 > _totalMemMB) {
                return;
            }

            String totalMemoryString = _totalMemMB + "MB Total";
            g2d.setColor(MEM_STRING_COLOR);
            g2d.drawString(totalMemoryString, this.getWidth() - getStringWidth(totalMemoryString) - 5, this.getHeight() - 4);
        }
    }

    void setAliasing(boolean value) {
        _aliasing = value;
    }

    public void setText(final String statusText) {
        _jLabelSimulationStatus.setText(statusText);
    }

    private int getStringWidth(String string) {
        FontMetrics fm = this.getFontMetrics(font);
        return fm.stringWidth(string);
    }

    private void drawDataMem(Graphics2D g2d) {
        DataContainerValuesSettable dc = NetzlisteCONTROL.globalData;
        if (dc == null) {
            return;
        }
        // 20 MB for the program itself!
        try {
            _dataMemMB = (int) (1.35 * dc.getUsedRAMSizeInMB()) + 35;
            final int percentage = (int) ((100 * _dataMemMB) / _usedMemMB);
            double fraction = _dataMemMB * 1.0 / _totalMemMB;
            fraction = Math.min(fraction, 1);

            String usedMemoryString = _dataMemMB + "MB Data, " + percentage + "%";

            if (_dataMemMB < 32) {
                _showMemoryWarning = true;
                if (_memoryWarning != null) {
                    _memoryWarning.setVisible(false);
                }
            }


            if (_showMemoryWarning && fraction > 0.8 && _runningPercentage < 95 && _runningPercentage > 10) {
                _showMemoryWarning = false;
                if (_memoryWarning == null) {
                    _memoryWarning = new MemoryWarning(null, false, _fenster);
                }
                _memoryWarning.setVisible(true);
            }

            g2d.setColor(new Color(240, 255, 240));
            g2d.fillRect(0, 1, (int) (_memBarPixelWidth * fraction), getHeight() - 2);

            g2d.setColor(MEM_STRING_COLOR);
            final int barPixelLength = (int) (_memBarPixelWidth * fraction);
            if (barPixelLength > getStringWidth(usedMemoryString) + 10) {
                if (fraction > 0.85) {
                    g2d.setColor(Color.red);
                }
                g2d.drawString(usedMemoryString, barPixelLength - getStringWidth(usedMemoryString) - 4, this.getHeight() - 6);
            }

            g2d.setColor(MEM_STRING_COLOR);
            final String cachedMemoryString = (_usedMemMB - _dataMemMB) + "MB Cached";
            if (getStringWidth(cachedMemoryString) * 1.5 < barPixelLength && _dataMemMB * 1.3 < _totalMemMB) {
                g2d.drawString(cachedMemoryString, barPixelLength + 4, this.getHeight() - 6);
            }

            g2d.setColor(Color.lightGray);
            g2d.drawRect(0, 0, barPixelLength, getHeight() - 1);
        } catch (ConcurrentModificationException ex) {
            // don't know why this happens :-(
        }
    }

    private void drawUsedMem(Graphics2D g2d) {

        double fraction = _usedMemMB * 1.0 / _totalMemMB;
        String usedMemoryString = _usedMemMB + "MB Used ";
        g2d.setColor(new Color(250, 255, 250));
        g2d.fillRect(0, 1, (int) (_memBarPixelWidth * fraction), getHeight() - 2);

        g2d.setColor(MEM_STRING_COLOR);
        final int barPixelLength = (int) (_memBarPixelWidth * fraction) - 1;

        if (getStringWidth(usedMemoryString) + 10 < barPixelLength && _dataMemMB * 1.2 < _totalMemMB) {
            g2d.drawString(usedMemoryString, barPixelLength - getStringWidth(usedMemoryString) - 4, this.getHeight() - 6);
        }

        g2d.setColor(Color.lightGray);
        g2d.drawRect(0, 0, barPixelLength, getHeight() - 1);
    }

    void setzeStatusRechenzeit(long tsim) {
        String displayText = "Stopped after  ";
        // tsim ... Simulationszeit in [ms]
        double tsec = tsim / 1e3;  // Gesamtzeit in [s] --> [ms] werden nur hinter der Kommastelle angezeigt
        int thour = (int) (tsec / 3600);  // Anzahl Stunden
        tsec -= (thour * 3600);
        int tmin = (int) (tsec / 60);  // Anzahl Minuten
        tsec -= (tmin * 60);
        if (thour > 0) {
            displayText += new String(thour + " [h]  " + tmin + " [min]  " + cf.formatT(tsec, TechFormat.FORMAT_AUTO) + " [s]");
        } else if (tmin > 0) {
            displayText += new String(tmin + " [min]  " + cf.formatT(tsec, TechFormat.FORMAT_AUTO) + " [s]");
        } else {
            displayText += new String(cf.formatT(tsec, TechFormat.FORMAT_AUTO) + " [s]");
        }
        this.setText(displayText);
    }
}
