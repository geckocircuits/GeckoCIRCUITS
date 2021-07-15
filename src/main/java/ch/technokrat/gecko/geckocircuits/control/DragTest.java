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

import java.awt.BorderLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class DragTest extends JFrame implements DragSourceListener,
    DragGestureListener {

  DragSource ds;

  JList jl;

  StringSelection transferable;

  String[] items = { "Java", "C", "C++", "Lisp", "Perl", "Python" };

  public DragTest() {
    super("Drag Test");
    setSize(200, 150);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        System.exit(0);
      }
    });
    jl = new JList(items);
    jl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    getContentPane().add(new JScrollPane(jl), BorderLayout.CENTER);

    ds = new DragSource();
    DragGestureRecognizer dgr = ds.createDefaultDragGestureRecognizer(jl,
        DnDConstants.ACTION_COPY, this);
    setVisible(true);
  }

  public void dragGestureRecognized(DragGestureEvent dge) {
    System.out.println("Drag Gesture Recognized!");
    transferable = new StringSelection(jl.getSelectedValue().toString());
    ds.startDrag(dge, DragSource.DefaultCopyDrop, transferable, this);
  }

  public void dragEnter(DragSourceDragEvent dsde) {
    System.out.println("Drag Enter");
  }

  public void dragExit(DragSourceEvent dse) {
    System.out.println("Drag Exit");
  }

  public void dragOver(DragSourceDragEvent dsde) {
    System.out.println("Drag Over");
  }

  public void dragDropEnd(DragSourceDropEvent dsde) {
    System.out.print("Drag Drop End: ");
    if (dsde.getDropSuccess()) {
      System.out.println("Succeeded");
    } else {
      System.out.println("Failed");
    }
  }

  public void dropActionChanged(DragSourceDragEvent dsde) {
    System.out.println("Drop Action Changed");
  }

  public static void main(String args[]) {
    new DragTest();
  }
}
