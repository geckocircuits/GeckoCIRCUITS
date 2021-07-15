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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JOptionPane;
import ch.technokrat.modelviewcontrol.ModelMVC;

public final class WorksheetSize {

    private static final int DEFAULT_SIZE = 40;
    public ModelMVC<Point> _worksheetDimension = new ModelMVC<Point>(new Point(900, 900), "worksheet size");
    private static final int MIN_SIZE = 10;
    private final CircuitSheet _parent;
    private static final int BORDER_OFFSET = 1;
    /**
     * don't use this stuff in new code. This is just for backwards compatibility with the crappy old .ipes file format.
     */
    private static final String WORKSHEETSIZE_30X30 = "600x600";
    private static final String WORKSHEETSIZE_60X60 = "1000x1000";
    private static final String WORKSHEETSIZE_120X120 = "1500x1500";
    private static final String WORKSHEETSIZE_200X200 = "2000x2000";
    private static final String WORKSHEETSIZE_350X350 = "5000x5000";
    private static final String WORKSHEETSIZE_900X900 = "9000x9000";

    WorksheetSize(final CircuitSheet parent) {
        _parent = parent;
        _worksheetDimension.addModelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int dpix = AbstractCircuitSheetComponent.dpix;
                final int sizeX = _worksheetDimension.getValue().x;
                final int sizeY = _worksheetDimension.getValue().y;
                _parent.setPreferredSize(new Dimension(sizeX * dpix, sizeY * dpix));
                _parent.revalidate();
            }
        });
    }

    public int getSizeX() {
        return _worksheetDimension.getValue().x;
    }

    public int getSizeY() {
        return _worksheetDimension.getValue().y;
    }

    public static int getOldFormatWSSize(final String sizeString) {
        //CHECKSTYLE:OFF
        if (sizeString == null) {
            return 30;
        } else if (sizeString.equals(WORKSHEETSIZE_30X30)) { // default!
            return 30;
        } else if (sizeString.equals(WORKSHEETSIZE_60X60)) {
            return 60;
        } else if (sizeString.equals(WORKSHEETSIZE_120X120)) {
            return 120;
        } else if (sizeString.equals(WORKSHEETSIZE_200X200)) {
            return 200;
        } else if (sizeString.equals(WORKSHEETSIZE_350X350)) {
            return 350;
        } else if (sizeString.equals(WORKSHEETSIZE_900X900)) {
            return 900;
        }
        return 30;
        //CHECKSTYLE:ON
    }

    public void setNewWorksheetSize(final int sizeX, final int sizeY) {        
        try {
            if (isComponentOutsideDrawingArea(sizeX, sizeY)) {
                JOptionPane.showMessageDialog(null, "There are circuit components located outside the selected sheet size "
                        + sizeX + "x" + sizeY + "."
                        + "\nPlease move all components within the target sheet size, before resizing \n"
                        + "the worksheet.", "Error!",
                        JOptionPane.ERROR_MESSAGE);                
                throw new IllegalArgumentException("Cannot resize due to bad component location.");
            } else {                                
                
                if (_parent instanceof SubCircuitSheet) {
                    SubCircuitSheet subSheet = (SubCircuitSheet) _parent;
                    final Collection<SubCircuitTerminable> allSheetTerminals = subSheet._subBlock._myTerminals;                                                            
                    if(allSheetTerminals != null)
                    for (SubCircuitTerminable term : allSheetTerminals) {
                        if (term.getTerminalLocation() == EnumTerminalLocation.RIGHT) {
                            setCheckedTerminalSheetPosition(term, sizeX - 1, term.getSheetPosition().y, allSheetTerminals);
                        }

                        if (term.getTerminalLocation() == EnumTerminalLocation.BOTTOM) {
                            setCheckedTerminalSheetPosition(term, term.getSheetPosition().x, sizeY - 1, allSheetTerminals);
                        }

                        if (term.getSheetPosition().x > sizeX - 1) {
                            setCheckedTerminalSheetPosition(term, sizeX - 1, term.getSheetPosition().y, allSheetTerminals);
                        }

                        if (term.getSheetPosition().y > sizeY - 1) {
                            setCheckedTerminalSheetPosition(term, term.getSheetPosition().x, sizeY - 1, allSheetTerminals);
                        }

                    }
                }
                
                Point newSheetSize = new Point(Math.max(sizeX, MIN_SIZE), Math.max(sizeY, MIN_SIZE));                                                                
                _worksheetDimension.setValue(newSheetSize);
                                
            }            
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setCheckedTerminalSheetPosition(SubCircuitTerminable terminable, int posX, int posY,
            Collection<SubCircuitTerminable> allSheetTerms) {

        for (SubCircuitTerminable term : allSheetTerms) {
            if (term.equals(terminable)) {
                continue;
            }

            if (terminable.getSheetPosition().x != posX) {
                if (term.getSheetPosition().x >= posX) {
                    throw new RuntimeException("Error: cannot move terminal " + terminable.getStringID() + " to new position!");
                }
            }

            if (terminable.getSheetPosition().y != posY) {
                if (term.getSheetPosition().y >= posY) {
                    throw new RuntimeException("Error: cannot move terminal " + terminable.getStringID() + " to new position!");
                }
            }

        }
        terminable.setSheetPositionWithoutUndo(new Point(posX, posY));
    }

    /**
     * Wenn Elemente bzw. Verbindungen ausserhalb des Worksheet sind, also unsichtbar, dann ist das eine potentialle Fehlerquelle
     * --> sollte also dem User nicht ermoeglicht werden --> wird hier grundsaetzlich geprueft
     */
    private boolean isComponentOutsideDrawingArea(final int worksheetSizeX, final int worksheetSizeY) {

        final List<Point> allPoints = new ArrayList<Point>();

        for (AbstractCircuitSheetComponent elem : _parent.getLocalSheetComponents()) {
            if (elem instanceof SubCircuitTerminable) { // don't consider TERMINALS in this check
                continue;
            }

            allPoints.addAll(elem.getAllDimensionPoints());            
        }
                

        for (Point testPt : allPoints) {
            if (testPt.x + BORDER_OFFSET > worksheetSizeX) {
                return true;
            }
            if (testPt.y + BORDER_OFFSET > worksheetSizeY) {
                return true;
            }
        }
        return false;
    }

    public void exportAscii(final StringBuffer strBuf) {                
        strBuf.append("\nworksheetSizeX ").append(_worksheetDimension.getValue().x);
        strBuf.append("\nworksheetSizeY ").append(_worksheetDimension.getValue().y);                
    }        

    public static Point getSize(final TokenMap tokenMap) {
        if (tokenMap.containsToken("worksheetSizeX")) {            
            final int sizeX = tokenMap.readDataLine("worksheetSizeX", DEFAULT_SIZE);
            final int sizeY = tokenMap.readDataLine("worksheetSizeY", DEFAULT_SIZE);                        
            return new Point(sizeX, sizeY);
        } 
        return new Point(DEFAULT_SIZE, DEFAULT_SIZE);
    }

    @Override
    public String toString() {
        return "Subcircuit " + hashCode() + " " + _worksheetDimension.getValue().x + " " + _worksheetDimension.getValue().y;
    }            
    
}
