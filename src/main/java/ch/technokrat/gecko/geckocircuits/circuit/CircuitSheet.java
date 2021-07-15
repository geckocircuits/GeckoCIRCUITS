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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.OperatingMode;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2.MouseMoveMode;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.control.TextFieldBlock;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoGraphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.*;
import javax.swing.JPanel;
import javax.swing.JViewport;
import org.apache.batik.svggen.SVGGraphics2D;

public class CircuitSheet extends JPanel {

    private static final int RAD_CTM = 7;  // radius of the oval node-marker in connectorTestMode
    public final MapList allElements = new MapList() {
        @Override
        public boolean add(AbstractCircuitSheetComponent toAdd) {
            return super.add(toAdd);
        }
    };
    public final SchematischeEingabe2 _se;
    public final WorksheetSize _worksheetSize;
    /**
     * the nodes which should be highlighted due to a string search
     */
    public static final Set<Point> _findNodes = new HashSet<Point>();
    /**
     * the nodes shown by the connector-Test.
     */
    public static Set<Point> _showNodes = new HashSet<Point>();

    public CircuitSheet(final SchematischeEingabe2 se) {
        _se = se;
        _worksheetSize = new WorksheetSize(this);
        setLayout(null);
        setOpaque(false);
    }

    /**
     * draws the raster points visible in the background of the sheet
     */
    public void drawCircuitSheet(java.awt.Graphics2D g2d) {
        final JViewport viewport = ((JViewport) this.getParent().getParent());        
        // as applet, the SVG jar is not available!
        if(!Fenster.IS_APPLET && g2d instanceof SVGGraphics2D) {
            return; // don't paint the pixels points for exporting to images!
        }
        Rectangle visibleRect = viewport.getViewRect();
        g2d.setColor(Color.white);
        g2d.fillRect(visibleRect.x, visibleRect.y, visibleRect.width, visibleRect.height);

        // Punktraster:
        g2d.setColor(Color.decode("0xaaaaaa"));  // zwischen GRAY (808080) und LIGHTGREY (d3d3d3)
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);


        final int dpix = AbstractCircuitSheetComponent.dpix;
        int startX = visibleRect.x / dpix - 1;
        int startY = visibleRect.y / dpix - 1;
        int stopX = startX + visibleRect.width / dpix + 2;
        int stopY = startY + visibleRect.height / dpix + 2;
        int noPoints = 0;

        for (int ix = Math.max(0, startX); ix < Math.min(_worksheetSize.getSizeX(), stopX); ix++) {
            for (int iy = Math.max(0, startY); iy < Math.min(_worksheetSize.getSizeY(), stopY); iy++) {
                int xPos = dpix * ix;
                int yPos = dpix * iy;
                g2d.drawLine(xPos, yPos, xPos, yPos);
                noPoints++;
            }
        }
    }

    void paintConnectorTestBackground(final Graphics2D graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        graphics.setColor(GlobalColors.farbeConnectorTestModeInternal);
        for (Point point : CircuitSheet._showNodes) {
            graphics.fillOval((int) (point.x * dpix) - RAD_CTM,
                    (int) (point.y * dpix) - RAD_CTM, 2 * RAD_CTM, 2 * RAD_CTM);
        }
    }

    void paintConnectorTestForeGround(final Graphics2D graphics) {
        graphics.setColor(GlobalColors.farbeConnectorTestMode);
        final int dpix = AbstractCircuitSheetComponent.dpix;
        for (Point point : CircuitSheet._showNodes) {
            graphics.drawOval((int) (point.x * dpix) - RAD_CTM,
                    (int) (point.y * dpix) - RAD_CTM, 2 * RAD_CTM, 2 * RAD_CTM);
        }
    }        
    

    @Override
    public void paintComponent(final Graphics graphics) {
        try {
            int dpix = AbstractCircuitSheetComponent.dpix;
            Graphics2D g2d = (Graphics2D) graphics;
            drawCircuitSheet(g2d);

            Graphics2D g2dDisabled = new GeckoGraphics2D(g2d);
            
            GeckoGraphics2D g2dSelected = new GeckoGraphics2D(g2d);
            g2dSelected.setColorStrategySelected();

            if (_se.connectorTestMode) {
                paintConnectorTestBackground(g2d);
            }


            if (_se.antialiasing) {
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            } else {
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
            }


            // first paint all textfields
            for (AbstractCircuitSheetComponent comp : getLocalSheetComponents()) {
                if (comp instanceof TextFieldBlock) {
                    comp.paintGeckoComponent(_se.findGraphicsForComponent(comp, g2d, g2dSelected, g2dDisabled));
                }
            }

            for (Point pt : _findNodes) {
                g2d.setColor(Color.YELLOW);
                graphics.fillOval((int) (pt.x * dpix) - 2 * RAD_CTM, (int) (pt.y * dpix) - 2 * RAD_CTM, 4 * RAD_CTM, 4 * RAD_CTM);
            }


            try {
                for (AbstractCircuitSheetComponent elem : getLocalSheetComponents()) {
                    if (elem instanceof TextFieldBlock) {
                        continue; // this was already painted above!
                    }
                    elem.paintGeckoComponent(_se.findGraphicsForComponent(elem, g2d, g2dSelected, g2dDisabled));
                }

                for (AbstractCircuitSheetComponent elem : getLocalSheetComponents()) {
                    if (elem instanceof TextFieldBlock) {
                        continue; // this was already painted above!
                    }
                    elem.paintComponentForeGround(_se.findGraphicsForComponent(elem, g2d, g2dSelected, g2dDisabled));
                }

            } catch (ConcurrentModificationException ex) {
                // this happens sometimes, when the painting thread tries to paint and a new component is inserted
                // at the same time. Just ignore the exception, since a repaint will be done anyway, soon.
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            // Symbol Zeichenstift beim Zeichnen der Verbindungen:
            if (_se.wirePenVisible) {
                g2d.setColor(Color.lightGray);
                g2d.fillPolygon(_se.xStift, _se.yStift, 4);
                g2d.setColor(Color.BLACK);
                g2d.drawPolygon(_se.xStift, _se.yStift, 4);
            }

            if (_se.connectorTestMode) {
                g2d.setColor(Color.magenta);
                g2d.drawPolygon(_se.xStift, _se.yStift, 4);
            }
            //---------------------------
            // Markierungsrechteck fuer Drag & Drop:
            if (_se._mouseMoveMode == MouseMoveMode.SELECT_WINDOW) {
                g2d.setColor(Color.orange);
                if (_se.x1markRe < _se.x2markRe) {
                    if (_se.y1markRe < _se.y2markRe) {
                        g2d.drawRect(_se.x1markRe, _se.y1markRe, (_se.x2markRe - _se.x1markRe), (_se.y2markRe - _se.y1markRe));
                    } else {
                        g2d.drawRect(_se.x1markRe, _se.y2markRe, (_se.x2markRe - _se.x1markRe), (-_se.y2markRe + _se.y1markRe));
                    }
                } else {
                    if (_se.y1markRe < _se.y2markRe) {
                        g2d.drawRect(_se.x2markRe, _se.y1markRe, (-_se.x2markRe + _se.x1markRe), (_se.y2markRe - _se.y1markRe));
                    } else {
                        g2d.drawRect(_se.x2markRe, _se.y2markRe, (-_se.x2markRe + _se.x1markRe), (-_se.y2markRe + _se.y1markRe));
                    }
                }
            }

            if (_se.connectorTestMode) {
                paintConnectorTestForeGround(g2d);
            }

            java.awt.Stroke oldStroke = g2d.getStroke();
            for (Point pt : _findNodes) {
                g2d.setColor(Color.MAGENTA);
                g2d.setStroke(new java.awt.BasicStroke(2F));

                graphics.drawOval((int) (pt.x * dpix) - 2 * RAD_CTM, (int) (pt.y * dpix) - 2 * RAD_CTM, 4 * RAD_CTM, 4 * RAD_CTM);
            }
            g2d.setStroke(oldStroke);
            super.paintComponent(g2d);            
        } catch (ConcurrentModificationException ex) {
            System.err.println("Concurrent modification in paint: " + ex.getMessage());
        }
    }

    public Collection<AbstractCircuitSheetComponent> getLocalSheetComponents() {
        return Collections.unmodifiableCollection(allElements);
    }

    public Set<String> findString(final String searchString, final boolean ignoreCase, final boolean startsWith) {
        Set<String> foundStrings = new HashSet<String>();
        _findNodes.clear();
        foundStrings.addAll(findElementInterface(getLocalSheetComponents(), searchString, ignoreCase, startsWith));
        repaint();
        return foundStrings;
    }

    public void updateNetListForLabels(ConnectorType connectorType) {
        // zur Aktualisierung eventuell neu angeschlossener Labels:
        switch(connectorType) {
            case CONTROL:
                NetzlisteAllg.fabricNetzlistComplete(getConnection(ConnectorType.CONTROL), allElements.getClassFromContainer(RegelBlock.class));
                break;
            case THERMAL:
                NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.THERMAL), getLocalComponents(ConnectorType.THERMAL));
                break;
            default:
                NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.LK_AND_RELUCTANCE), getLocalComponents(ConnectorType.LK));
                break;                
        }
        
        
        
    }

    private Set<String> findElementInterface(Collection<? extends AbstractCircuitSheetComponent> elements, final String searchString, final boolean ignoreCase,
            final boolean startsWith) {
        Set<String> foundStrings = new HashSet<String>();
        for (AbstractCircuitSheetComponent elem : elements) {
            if (elem != null) {
                if (elem instanceof AbstractBlockInterface) {
                    AbstractBlockInterface block = (AbstractBlockInterface) elem;
                    if (findFromString(searchString, block.getStringID(), ignoreCase, startsWith)) {
                        Point sheetPos = block.getSheetPosition();
                        _findNodes.add(sheetPos);
                        foundStrings.add("Component: " + block.getStringID() + " x=" + sheetPos.x + " y=" + sheetPos.y);
                    }
                }

                if (elem instanceof ComponentTerminable) {
                    for (TerminalInterface endTerm : ((ComponentTerminable) elem).getAllTerminals()) {
                        final String labelString = endTerm.getLabelObject().getLabelString();
                        if (findFromString(searchString, labelString, ignoreCase, startsWith)) {
                            Point sheetPos = endTerm.getPosition();
                            _findNodes.add(sheetPos);
                            if (elem instanceof AbstractBlockInterface) {
                                foundStrings.add("Label of: " + ((AbstractBlockInterface) elem).getStringID() + " x=" + sheetPos.x + " y=" + sheetPos.y);
                            } else if (elem instanceof Verbindung) {
                                foundStrings.add("Connection label: " + ((Verbindung) elem).getLabel());
                            }

                        }
                    }
                }
            }
        }
        return foundStrings;
    }

    private boolean findFromString(final String searchString, final String componentString, final boolean ignoreCase,
            final boolean startsWith) {
        String newCompString = componentString;
        String newSearchString = searchString;

        if (ignoreCase) {
            newCompString = componentString.toLowerCase();
            newSearchString = searchString.toLowerCase();
        }

        if (startsWith) {
            if (newCompString.startsWith(newSearchString)) {
                return true;
            }
        }


        if (newCompString.equals(newSearchString)) {
            return true;
        }

        return false;
    }

    public static void clearFind() {
        _findNodes.clear();
    }

    private boolean selectPotentialNodesToShow(PotentialArea[] pot, Point clickPoint,
            final List<? extends AbstractBlockInterface> elements) {        
        for (PotentialArea potArea : pot) {
            if (potArea.isPointOnPotential(clickPoint)) {
                for (Point pt : potArea.getAllElementKnotenXY(elements, this)) {
                    CircuitSheet._showNodes.add(pt);
                }
                for (Verbindung verb : potArea.getAllConnections()) {
                    for (TerminalInterface term : verb.getAllTerminals()) {
                        CircuitSheet._showNodes.add(term.getPosition());
                    }

                }                                            
                return true;
            }
        }                
        return false;
    }

    public void maus_connectorTest(final Point clickPoint) {
        // damit man nicht (wie unten) beim 'return' vorzeitig aussteigt und eine Verbindung versehentlich
        // im Bearbeitungs-Modus laesst, die folgende kleine Schleife:        
        CircuitSheet._showNodes.clear();

        List<AbstractBlockInterface> localElementsLK = getLocalComponents(ConnectorType.LK_AND_RELUCTANCE);
        List<AbstractBlockInterface> localElementsTHERM = getLocalComponents(ConnectorType.THERMAL);
        List<AbstractBlockInterface> localElementsCONTROL = getLocalComponents(ConnectorType.CONTROL);

        for (AbstractCircuitSheetComponent comp : this.getLocalSheetComponents()) {
            if (comp instanceof SubcircuitBlock) {
                localElementsLK.add((AbstractBlockInterface) comp);
            }

            if (comp instanceof SubcircuitBlock) {
                localElementsTHERM.add((AbstractBlockInterface) comp);
            }

            if (comp instanceof SubcircuitBlock) {
                localElementsCONTROL.add((AbstractBlockInterface) comp);
            }

        }

        // (1) LK-Check --> 
        PotentialArea[] pot = (NetzlisteAllg.fabricNetzlistComplete(getConnection(ConnectorType.LK_AND_RELUCTANCE), localElementsLK)).getPotentiale();

        if (selectPotentialNodesToShow(pot, clickPoint, localElementsLK)) {
            return;
        }

        // (2) CONTROL-Check --> 

        PotentialArea[] pot2 = (NetzlisteAllg.fabricNetzlistComplete(getConnection(ConnectorType.CONTROL), localElementsCONTROL)).getPotentiale();
        if (selectPotentialNodesToShow(pot2, clickPoint, localElementsCONTROL)) {
            return;
        }

        // (3) THERM-Check --> 
        PotentialArea[] pot3 = (NetzlisteAllg.fabricNetzlistComplete(getConnection(ConnectorType.THERMAL),
                localElementsTHERM)).getPotentiale();
        if (selectPotentialNodesToShow(pot3, clickPoint, localElementsTHERM)) {
            return;
        }
        // no potential found on point!
        CircuitSheet._showNodes.clear();
    }

    public Set<Verbindung> getConnection(final ConnectorType connectorType) {
        Collection<AbstractCircuitSheetComponent> allElements = getLocalSheetComponents();
        Set<Verbindung> allConnectors = new LinkedHashSet<Verbindung>();
        for (AbstractCircuitSheetComponent comp : allElements) {
            if (comp instanceof Verbindung) {
                allConnectors.add((Verbindung) comp);
            }
        }

        if (connectorType == ConnectorType.LK_AND_RELUCTANCE) {
            Set<Verbindung> returnValue = new LinkedHashSet<Verbindung>();
            returnValue.addAll(getConnection(ConnectorType.LK));
            returnValue.addAll(getConnection(ConnectorType.RELUCTANCE));
            return Collections.unmodifiableSet(returnValue);
        }

        Set<Verbindung> returnValue = new LinkedHashSet<Verbindung>();
        for (AbstractCircuitSheetComponent verbCand : allConnectors) {
            if (verbCand instanceof Verbindung) {
                Verbindung verb = (Verbindung) verbCand;
                if (verb.getSimulationDomain() == connectorType) {
                    returnValue.add(verb);
                }
            }

        }
        return Collections.unmodifiableSet(returnValue);
    }

    public final Set<AbstractCircuitSheetComponent> getElementsInRectangle(final int x1markRe, final int y1markRe,
            final int x2markRe, final int y2markRe) {
        final Set<AbstractCircuitSheetComponent> returnValue = new LinkedHashSet<AbstractCircuitSheetComponent>();
        for (AbstractCircuitSheetComponent elem : getLocalSheetComponents()) {
            if (componentIsIncludedInRectangle(x1markRe, y1markRe, x2markRe, y2markRe, elem)) {
                returnValue.add(elem);
            }
        }
        return returnValue;
    }

    // Welche Bauteile werden vom MarkierungsRechteck umfasst? -->
    private static boolean componentIsIncludedInRectangle(int x1MR, int y1MR, int x2MR, int y2MR, AbstractCircuitSheetComponent elem) {
        int[] ausenabmessungen = elem.getAussenabmessungenRechteckEckpunkte();
        int x1 = ausenabmessungen[0];
        int y1 = ausenabmessungen[1];
        int x2 = ausenabmessungen[2];
        int y2 = ausenabmessungen[3];

        if (((x1MR <= x1) && (x1 <= x2MR) && (x1MR <= x2) && (x2 <= x2MR) && (y1MR <= y1) && (y1 <= y2MR) && (y1MR <= y2) && (y2 <= y2MR))
                || ((x1MR >= x1) && (x1 >= x2MR) && (x1MR >= x2) && (x2 >= x2MR) && (y1MR <= y1) && (y1 <= y2MR) && (y1MR <= y2) && (y2 <= y2MR))
                || ((x1MR <= x1) && (x1 <= x2MR) && (x1MR <= x2) && (x2 <= x2MR) && (y1MR >= y1) && (y1 >= y2MR) && (y1MR >= y2) && (y2 >= y2MR))
                || ((x1MR >= x1) && (x1 >= x2MR) && (x1MR >= x2) && (x2 >= x2MR) && (y1MR >= y1) && (y1 >= y2MR) && (y1MR >= y2) && (y2 >= y2MR))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param elementName this is the full path of a component, for which we
     * search the circuit sheet. E.g. Sub.1#inductor.3 would return the circuit
     * sheet of Sub.1
     * @return
     */
    public CircuitSheet findSubCircuit(String elementName) {
        int subCharIndex = elementName.indexOf('#');
        if (subCharIndex < 1) {
            return this;
        }
        String searchSub = elementName.substring(0, subCharIndex);
        String searchComponentName = elementName.substring(subCharIndex + 1);
        for (AbstractCircuitSheetComponent searchComp : allElements) {
            if (searchComp instanceof SubcircuitBlock) {
                SubcircuitBlock subBlock = (SubcircuitBlock) searchComp;
                if (subBlock.getStringID().equals(searchSub)) {
                    return subBlock._myCircuitSheet.findSubCircuit(searchComponentName);
                }
            }
        }
        return this;
    }

    public MapList getAllElements() {
        MapList returnValue = new MapList();
        for (AbstractCircuitSheetComponent comp : allElements) {
            returnValue.add(comp);
            if (comp instanceof SubcircuitBlock) {
                returnValue.addAll(((SubcircuitBlock) comp)._myCircuitSheet.getAllElements());
            }
        }
        return returnValue;
    }

    public List<String> getAllLocalSheetLabels() {

        List<String> allOriginalModelLabels = new ArrayList<String>();
        for (AbstractCircuitSheetComponent comp : allElements) {

            if (comp instanceof ComponentTerminable) {
                allOriginalModelLabels.addAll(((ComponentTerminable) comp).getAllNodeLabels());
            }
        }

        return allOriginalModelLabels;
    }

    public void updateRenamedLabel(final String originalLabel, String neuerLabel, final ConnectorType renamedLabelType) {
        updateNetListForLabels(renamedLabelType);

        if (testRenameLabelWhereOldStillExists(originalLabel, getLocalComponents(renamedLabelType))) {
            return; // if the new label is already present in the model, don't do any update!
        }

        for (AbstractCircuitSheetComponent search : getAllElements()) {
            if (search instanceof PotentialCoupable) {
                final PotentialCoupling potCoup = ((PotentialCoupable) search).getPotentialCoupling();
                if (potCoup.getLinkType() == renamedLabelType || renamedLabelType == ConnectorType.LK && potCoup.getLinkType() == ConnectorType.LK_AND_RELUCTANCE
                        || renamedLabelType == ConnectorType.LK_AND_RELUCTANCE && potCoup.getLinkType() == ConnectorType.LK) {
                    potCoup.renameUpdate(originalLabel, neuerLabel);
                }
            }
        }
    }

    private boolean testRenameLabelWhereOldStillExists(final String originalLabelBeforeRename,
            final List<? extends AbstractBlockInterface> elements) {
        for (AbstractBlockInterface elem : elements) {
            for (TerminalInterface term : elem.getAllTerminals()) {
                String existingCompareLabel = term.getLabelObject().getLabelString();

                if ((!originalLabelBeforeRename.equals("")) && (existingCompareLabel.equals(originalLabelBeforeRename))) {
                    /**
                     * special case: control input terminal. This terminal does
                     * not generate "output" values, therefore don't consider
                     * this for the link detection!
                     */
                    if (!(term instanceof TerminalControlInput)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    public List<AbstractBlockInterface> getLocalComponents(final ConnectorType connectorType) {
        final LinkedList<AbstractBlockInterface> returnValue = new LinkedList<AbstractBlockInterface>();
        final Collection<AbstractBlockInterface> allComponents = allElements.getClassFromContainer(AbstractBlockInterface.class);
        returnValue.addAll(allComponents);
        for (AbstractBlockInterface remove : allComponents) {
            if (connectorType == connectorType.RELUCTANCE || connectorType == ConnectorType.LK || connectorType == ConnectorType.LK_AND_RELUCTANCE) {
                if (remove.getSimulationDomain() != ConnectorType.LK && remove.getSimulationDomain() != ConnectorType.LK_AND_RELUCTANCE && remove.getSimulationDomain() != ConnectorType.RELUCTANCE) {
                    returnValue.remove(remove);
                }
            } else {
                if (remove.getSimulationDomain() != connectorType) {
                    returnValue.remove(remove);
                }
            }
        }
        return returnValue;
    }

    public List<String> getLocalControlLabels() {

        final Set<String> allControlLabels = new LinkedHashSet<String>();
        final List<AbstractBlockInterface> allControlComponents = getLocalComponents(ConnectorType.CONTROL);
        allControlLabels.add("");
        for (AbstractBlockInterface block : allControlComponents) {
            if (block instanceof RegelBlock) {
                final RegelBlock regelBlock = (RegelBlock) block;
                for (String label : regelBlock.getAllNodeLabels()) {
                    if (!label.isEmpty()) {
                        allControlLabels.add(label);
                    }
                }
            }
        }
        final List<String> sortList = new ArrayList<String>(allControlLabels);
        Collections.sort(sortList);
        return Collections.unmodifiableList(sortList);
    }

    public List<String> getLocalLabels(final ConnectorType connectorType) {

        final Set<String> allControlLabels = new LinkedHashSet<String>();
        final List<AbstractBlockInterface> allControlComponents = getLocalComponents(connectorType);
        allControlLabels.add("");
        for (AbstractBlockInterface block : allControlComponents) {
            for (TerminalInterface term : block.getAllTerminals()) {
                if (term.getCategory() == connectorType) {
                    allControlLabels.add(term.getLabelObject().getLabelString());
                }
            }
        }
        final List<String> sortList = new ArrayList<String>(allControlLabels);
        Collections.sort(sortList);
        return Collections.unmodifiableList(sortList);
    }

    /**
     * when componentes / connectors are deleted, whe have to enshure that the
     * link to e.g. voltage measurement components is deleted!
     */
    public void updateAllPotentialCoupables() {

        for (AbstractCircuitSheetComponent comp : getLocalSheetComponents()) {
            if (comp instanceof PotentialCoupable) {
                PotentialCoupling coupling = ((PotentialCoupable) comp).getPotentialCoupling();
                for (int i = 0; i < coupling.getLabels().length; i++) {
                    String coupledLabel = coupling.getLabels()[i];
                    if (coupledLabel == null || coupledLabel.isEmpty()) {
                        continue;
                    }
                    if (!getLocalLabels(coupling.getLinkType()).contains(coupledLabel)) {
                        coupling.setNewCouplingLabel(i, "");
                    }
                }
            }
            if (comp instanceof SubcircuitBlock) { // recursively, check all subsheets.
                ((SubcircuitBlock) comp)._myCircuitSheet.updateAllPotentialCoupables();
            }
        }
    }

    /**
     * return all components that are sub-components of this circuit sheet
     * (recursively)
     */
    public Collection<AbstractCircuitSheetComponent> getLocalRecursiveSubComponents() {
        Set<AbstractCircuitSheetComponent> returnValue = new LinkedHashSet<AbstractCircuitSheetComponent>();
        returnValue.addAll(allElements);
        for (AbstractCircuitSheetComponent subTest : allElements) {
            if (subTest instanceof SubcircuitBlock) {
                SubcircuitBlock subBlock = (SubcircuitBlock) subTest;
                returnValue.addAll(subBlock._myCircuitSheet.getLocalRecursiveSubComponents());
            }
        }
        return Collections.unmodifiableCollection(returnValue);
    }

    public void setNewScaling(final int dpix) {
        setPreferredSize(new Dimension(_worksheetSize.getSizeX() * dpix, _worksheetSize.getSizeY() * dpix));
    }

    public void doSetVisibleAction() {
        Fenster._northPanel.removeAll();
        Fenster._northPanel.revalidate();
    }
}
