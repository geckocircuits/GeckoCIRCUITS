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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.JButton;

public class LastComponentButton extends JButton {

    private AbstractBlockInterface _showComponent;
    private AbstractBlockInterface _tmpShowComponent;
    private AbstractComponentTyp _typ;
    private ComponentDirection _componentDirection = ComponentDirection.NORTH_SOUTH;

    public LastComponentButton() {
    }

    public void setSelectedShowComponent(final AbstractBlockInterface showComponent, final AbstractComponentTyp typ) {
        _showComponent = showComponent;
        _showComponent.setComponentDirection(_componentDirection);
        _typ = typ;
        repaint();
    }

    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g2d = (Graphics2D) graphics;
        setPaintSettings(graphics);
        final AbstractBlockInterface toShow = findComponentToShow();
        if (toShow != null) {
            final AffineTransform oldTrans = g2d.getTransform();
            g2d.translate(2 * getWidth() / 5, 40);
            toShow.paintGeckoComponent(g2d);
            toShow.paintComponentForeGround(g2d);
            g2d.setTransform(oldTrans);
        }
    }

    public void setTempShowComponent(final AbstractBlockInterface newTmpComponent) {
        _tmpShowComponent = newTmpComponent;
        _tmpShowComponent.setComponentDirection(_componentDirection);
        repaint();
    }

    public void removeTempShowComponent(final AbstractBlockInterface tmpComponentToRemove) {
        if (_tmpShowComponent == tmpComponentToRemove) {
            _tmpShowComponent = null;
        }
        repaint();
    }

    private AbstractBlockInterface findComponentToShow() {
        if (_tmpShowComponent != null) {
            return _tmpShowComponent;
        }
        if (_showComponent != null) {
            return _showComponent;
        }
        return null;
    }

    private static void setPaintSettings(final Graphics graphics) {
        final Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public AbstractBlockInterface getSelectedBlock() {
        return _showComponent;
    }

    public AbstractComponentTyp getTyp() {
        return _typ;
    }

    public void setComponentDirection(ComponentDirection _lastRotationDirection) {
        try {
            _componentDirection = _lastRotationDirection;
            if (_showComponent != null) {
                _showComponent.setComponentDirection(_lastRotationDirection);
            }
            if (_tmpShowComponent != null) {
                _tmpShowComponent.setComponentDirection(_lastRotationDirection);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
