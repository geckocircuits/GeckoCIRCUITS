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
package gecko.geckocircuits.newscope;

import gecko.geckocircuits.allg.GlobalColors;
import gecko.geckocircuits.newscope.GraferV4.MausModus;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author andy
 */
final class ZoomWindow {

    private boolean _zoomWinVisible = false;
    private int _startClickX, _startClickY;  // Koordinates of zoom window
    private int _stopDrawWindowX, _stopDrawWindowY;
    private boolean _controlZoomOn = false;
    private boolean _shiftZoomOn = false;
    private boolean _xZoomOn = false;
    private boolean _yZoomOn = false;
    private final AbstractDiagram _clickedDiagram;
    private static final Rectangle NULL_RECTANGLE = new Rectangle();
    private Rectangle _drawRectangle = NULL_RECTANGLE;
    // needed for efficient drawing / undrawing of the old rectangle
    private Rectangle _lastDrawnRect = new Rectangle();
    private Point _panningStartPoint;
    private boolean _zoomModusEnabled = true;
    final MouseMotionListener _mouseMotionLsnr = new MouseMotionListener() {
        @Override
        public void mouseMoved(final MouseEvent mouseEvent) {
            // nothing to do here!
        }

        @Override
        public void mouseDragged(final MouseEvent mouseEvent) {
            if (!_zoomModusEnabled) {
                return;
            }

            if (mouseEvent.isAltDown() || (mouseEvent.isControlDown() && mouseEvent.isShiftDown())
                    || mouseEvent.isAltGraphDown()) {
                doPanning(mouseEvent);
            } else {
                updateZoomWindow(mouseEvent);
            }


        }

        private void updateZoomWindow(final MouseEvent mouseEvent) {
            _zoomWinVisible = true;
            determineBoundaries(mouseEvent);

            final int minX = Math.min(_startClickX, _stopDrawWindowX);
            final int minY = Math.min(_startClickY, _stopDrawWindowY);
            final int maxX = Math.max(_startClickX, _stopDrawWindowX);
            final int maxY = Math.max(_startClickY, _stopDrawWindowY);
            final int width = maxX - minX;
            final int height = maxY - minY;

            _drawRectangle = new Rectangle(minX, minY, width, height);


            // this seems to be unnecessary, however, we want to "undraw" the old rectangle
            if (_lastDrawnRect == null) {
                _clickedDiagram.repaint(_drawRectangle.x, _drawRectangle.y, _drawRectangle.width + 1, _drawRectangle.height + 1);
            } else {
                final Rectangle union = _drawRectangle.union(_lastDrawnRect);
                _clickedDiagram.repaint(union.x, union.y, union.width + 1, union.height + 1);

            }
        }

        private void doPanning(final MouseEvent mouseEvent) {
            final Set<AbstractDiagram> diagramsForXUpate = _clickedDiagram._grafer.getDiagramsForXUpdate(mouseEvent, _clickedDiagram);
            final Axis yAxis = _clickedDiagram._yAxis1;
            yAxis.doPanning(mouseEvent.getY(), _panningStartPoint.y);

            for (AbstractDiagram diagram : diagramsForXUpate) {
                final Axis xAxis = diagram._xAxis;
                xAxis.doPanning(mouseEvent.getX(), _panningStartPoint.x);
                diagram.loadDataFromContainer(_clickedDiagram._grafer.getDataContainer());
                diagram.repaint();
            }

        }
    };
    final MouseListener _mouseListener = new MouseListener() {
        @Override
        public void mouseEntered(final MouseEvent mouseEvent) {
            // nothing to do here!
        }

        @Override
        public void mouseExited(final MouseEvent mouseEvent) {
        }

        @Override
        public void mouseClicked(final MouseEvent mouseEvent) {
        }

        @Override
        public void mousePressed(final MouseEvent mouseEvent) {
            if (!_zoomModusEnabled) {
                return;
            }
            _startClickX = Math.max(mouseEvent.getX(), _clickedDiagram._xAxis._axisOriginPixel.x);
            _startClickY = Math.min(mouseEvent.getY(), _clickedDiagram._xAxis._axisOriginPixel.y);
            _panningStartPoint = mouseEvent.getPoint();
            for (AbstractDiagram diag : _clickedDiagram._grafer._manager.getDiagrams()) {
                final Axis xAxis = diag._xAxis;
                xAxis.setPanningOriginLimits();
            }

            _clickedDiagram._yAxis1.setPanningOriginLimits();


        }

        @Override
        public void mouseReleased(final MouseEvent mouseEvent) {
            if (!_zoomModusEnabled) {
                return;
            }
            _zoomWinVisible = false;
            setDiagramRange(_clickedDiagram._grafer);
            _drawRectangle = NULL_RECTANGLE;
            _clickedDiagram.repaint();
        }
    };

    ZoomWindow(final AbstractDiagram parentComponent) {
        _clickedDiagram = parentComponent;
    }

    void setMausModus(final MausModus mausModus) {
        if (mausModus == MausModus.ZOOM_WINDOW) {
            _zoomModusEnabled = true;
        } else {
            _zoomModusEnabled = false;
        }
    }

    /**
     * Zoom only in x-Direction
     *
     * @param isOn
     */
    public void setXZoomOn(final boolean isOn) {
        _xZoomOn = isOn;
    }

    /**
     * zoom only in y-direction
     *
     * @param isOn
     */
    public void setYZoomOn(final boolean isOn) {
        _yZoomOn = isOn;
    }

    private void determineBoundaries(final MouseEvent mouseEvent) {
        int xValue = mouseEvent.getX();
        int yValue = mouseEvent.getY();

        if (mouseEvent.isControlDown() || _xZoomOn) {
            yValue = _startClickY + 1;
            _controlZoomOn = true;
        } else {
            _controlZoomOn = false;
        }

        if (mouseEvent.isShiftDown() || _yZoomOn) {
            _shiftZoomOn = true;
            xValue = _startClickX + 1;
        } else {
            _shiftZoomOn = false;
        }

        _stopDrawWindowX = xValue;
        _stopDrawWindowY = yValue;

        final Axis xAxis = _clickedDiagram._xAxis;
        if (_stopDrawWindowX < xAxis._axisOriginPixel.x) {
            _stopDrawWindowX = xAxis._axisOriginPixel.x;
        }

        final int maxX = xAxis._axisOriginPixel.x + xAxis.getAxisLengthPixel();
        if (_stopDrawWindowX > maxX) {
            _stopDrawWindowX = maxX;
        }

        final Axis y1Axis = _clickedDiagram._yAxis1;
        if (_stopDrawWindowY > y1Axis._axisOriginPixel.y) {
            _stopDrawWindowY = y1Axis._axisOriginPixel.y;

        }

        final int minY = y1Axis._axisOriginPixel.y - y1Axis.getAxisLengthPixel();
        if (_stopDrawWindowY < minY) {
            _stopDrawWindowY = minY;
        }
    }

    private void setDiagramRange(final GraferV4 grafer) {

        // neue x-Bereichsgrenze fuer alle Diagramme
        final Axis zoomedXAxis = _clickedDiagram._xAxis;

        // if user just clicked, without draggin a "real" window, just exit
        if (Math.abs(_drawRectangle.width) < 2 && Math.abs(_drawRectangle.height) < 2) {
            return;
        }

        final HiLoData newValueMinMax = HiLoData.hiLoDataFabric((float) zoomedXAxis.getValueFromPixel(_drawRectangle.x),
                (float) zoomedXAxis.getValueFromPixel(_drawRectangle.x + _drawRectangle.width));

        final List<Axis> axisToAdapt = new ArrayList<Axis>();

        for (AbstractDiagram diag : grafer.getManager().getDiagrams()) {
            if (!_shiftZoomOn) {
                final Axis axis = diag._xAxis;
                if (axis.hasIdenticalSettings(zoomedXAxis)) {
                    axisToAdapt.add(axis);
                }
            }
        }

        for (Axis axis : axisToAdapt) {
            axis._axisMinMax.setZoomValues(newValueMinMax, false);
        }

        if (!_controlZoomOn) {
            final Axis y1Axis = _clickedDiagram._yAxis1;
            final Axis y2Axis = _clickedDiagram._yAxis2;

            final HiLoData newMinMax1 = HiLoData.hiLoDataFabric(
                    (float) y1Axis.getValueFromPixel(_drawRectangle.y + _drawRectangle.height),
                    (float) y1Axis.getValueFromPixel(_drawRectangle.y));
            y1Axis._axisMinMax.setZoomValues(newMinMax1, false);

            final HiLoData newMinMax2 = HiLoData.hiLoDataFabric(
                    (float) y2Axis.getValueFromPixel(_drawRectangle.y + _drawRectangle.height),
                    (float) y2Axis.getValueFromPixel(_drawRectangle.y));
            y2Axis._axisMinMax.setZoomValues(newMinMax2, false);
        }


        for (AbstractDiagram diag : grafer.getManager().getDiagrams()) {
            diag.loadDataFromContainer(grafer.getDataContainer());
            diag.repaint();
        }



//        _zoomWindowVisible = false;
    }

    void paintComponent(final Graphics2D graphics) {
        if (!_zoomWinVisible) {
            return;
        }

        final AlphaComposite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        graphics.setComposite(alphaComp);
        graphics.setColor(GlobalColors.farbeZoomRechteck);
        graphics.draw(_drawRectangle);
        if (!_drawRectangle.equals(_lastDrawnRect)) {
            _lastDrawnRect = _drawRectangle;
        }

    }
}
