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
package ch.technokrat.gecko.geckocircuits.circuit;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

public class ToolBar extends JFrame {

    public ToolBar() {
        super("ToolBar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ImageIcon image1 = new ImageIcon("button1.gif");
        JButton button1 = new JButton(image1);
        ImageIcon image2 = new ImageIcon("button2.gif");
        JButton button2 = new JButton(image2);
        ImageIcon image3 = new ImageIcon("button3.gif");
        JButton button3 = new JButton(image3);
        JToolBar bar = new JToolBar();
        bar.add(button1);
        bar.add(button2);
        bar.add(button3);
        JTextArea edit = new JTextArea(8,40);
        JScrollPane scroll = new JScrollPane(edit);
        JPanel pane = new JPanel();
        BorderLayout bord = new BorderLayout();
        pane.setLayout(bord);
        pane.add("North", bar);
        pane.add("Center", scroll);

        setContentPane(pane);
    }

    public static void main(String[] arguments) {
        ToolBar frame = new ToolBar();
        frame.pack();
        frame.setVisible(true);
    }
}