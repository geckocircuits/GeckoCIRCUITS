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
import java.util.*;
import javax.swing.*;
 
public class ListDnD {
    ReportingListTransferHandler arrayListHandler =
                         new ReportingListTransferHandler();
 
    private JPanel getContent() {
        JPanel panel = new JPanel(new GridLayout(1,0));
        panel.add(getListComponent("left"));
        panel.add(getListComponent("right"));
        return panel;
    }
 
    private JScrollPane getListComponent(String s) {
        DefaultListModel model = new DefaultListModel();
        for(int j = 0; j < 5; j++)
            model.addElement(s + " " + (j+1));
        JList list = new JList(model);
        list.setName(s);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setTransferHandler(arrayListHandler);
        list.setDragEnabled(true);
        return new JScrollPane(list);
    }
 
    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(new ListDnD().getContent());
        f.setSize(400,200);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}