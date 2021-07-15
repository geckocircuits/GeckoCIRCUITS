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


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class CheckBoxList extends JList
    implements ListSelectionListener {

    static Color listForeground, listBackground,
        listSelectionForeground,
        listSelectionBackground;
    static {
        UIDefaults uid = UIManager.getLookAndFeel().getDefaults();
        listForeground =  uid.getColor ("List.foreground");
        listBackground =  uid.getColor ("List.background");
        listSelectionForeground =  uid.getColor ("List.selectionForeground");
        listSelectionBackground =  uid.getColor ("List.selectionBackground");
    }

    HashSet selectionCache = new HashSet();
    int toggleIndex = -1;
    boolean toggleWasSelected;

    public CheckBoxList() {
        super();
        setCellRenderer (new CheckBoxListCellRenderer());
        addListSelectionListener (this);
    }

    // ListSelectionListener implementation
    public void valueChanged (ListSelectionEvent lse) {
        System.out.println (lse);
        if (! lse.getValueIsAdjusting()) {
            removeListSelectionListener (this);

            // remember everything selected as a result of this action
            HashSet newSelections = new HashSet();
            int size = getModel().getSize();
            for (int i=0; i<size; i++) {
                if (getSelectionModel().isSelectedIndex(i)) {
                    newSelections.add (new Integer(i));
                }
            }

            // turn on everything that was previously selected
            Iterator it = selectionCache.iterator();
            while (it.hasNext()) {
                int index = ((Integer) it.next()).intValue();
                System.out.println ("adding " + index);
                getSelectionModel().addSelectionInterval(index, index);
            }

            // add or remove the delta
            it = newSelections.iterator();
            while (it.hasNext()) {
                Integer nextInt = (Integer) it.next();
                int index = nextInt.intValue();
                if (selectionCache.contains (nextInt))
                    getSelectionModel().removeSelectionInterval (index, index);
                else
                    getSelectionModel().addSelectionInterval (index, index);
            }

            // save selections for next time
            selectionCache.clear();
            for (int i=0; i<size; i++) {
                if (getSelectionModel().isSelectedIndex(i)) {
                    System.out.println ("caching " + i);
                    selectionCache.add (new Integer(i));
                }
            }

            addListSelectionListener (this);

        }
    }




    public static void main (String[] args) {
        JList list = new CheckBoxList ();
        DefaultListModel defModel = new DefaultListModel();
        list.setModel (defModel);
        String[] listItems = {
            "Chris", "Joshua", "Daniel", "Michael",
            "Don", "Kimi", "Kelly", "Keagan"
        };
        Iterator it = Arrays.asList(listItems).iterator();
        while (it.hasNext())
            defModel.addElement (it.next());
        // show list
        JScrollPane scroller =
            new JScrollPane (list,
                            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JFrame frame = new JFrame ("Checkbox JList");
        frame.getContentPane().add (scroller);
        frame.pack();
        frame.setVisible(true);
    }


    class CheckBoxListCellRenderer extends JComponent
        implements ListCellRenderer {
        DefaultListCellRenderer defaultComp;
        JCheckBox checkbox;
        public CheckBoxListCellRenderer() {
            setLayout (new BorderLayout());
            defaultComp = new DefaultListCellRenderer();
            checkbox = new JCheckBox();
            add (checkbox, BorderLayout.WEST);
            add (defaultComp, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object  value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus){
            defaultComp.getListCellRendererComponent (list, value, index,
                                                      isSelected, cellHasFocus);
            /*
            checkbox.setSelected (isSelected);
            checkbox.setForeground (isSelected ?
                                    listSelectionForeground :
                                    listForeground);
            checkbox.setBackground (isSelected ?
                                    listSelectionBackground :
                                    listBackground);
            */
            checkbox.setSelected (isSelected);
            Component[] comps = getComponents();
            for (int i=0; i<comps.length; i++) {
                comps[i].setForeground (listForeground);
                comps[i].setBackground (listBackground);
            }
            return this;
        }
    }
}