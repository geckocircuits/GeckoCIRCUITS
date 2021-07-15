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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class SuggestionField extends JTextField {

    private static final long serialVersionUID = 1756202080423312153L;
    private final JDialog _dialog;
    private JList _list;
    private List<String> _data = new ArrayList<String>();
    private final List<String> _suggestions = new ArrayList<String>();
    private InterruptableMatcher _matcher;
    private Font _busy;
    private Font _regular;
    private String _lastWord = "";
    private String _lastChosenExistingVariable;
    private String _hint;
    private final List<ActionListener> _listeners = new ArrayList<ActionListener>();
    private SuggestMatcher _suggestMatcher = new ContainsMatcher();
    private boolean _caseSensitive = false;
    private final JScrollPane _scrollPane;

    public SuggestionField(final Frame owner) {
        super();                        
        
        owner.addComponentListener(new ComponentListener() {
            @Override
            public void componentShown(final ComponentEvent event) {
                SuggestionField.this.relocate();
            }

            @Override
            public void componentResized(final ComponentEvent event) {
                SuggestionField.this.relocate();
            }

            @Override
            public void componentMoved(final ComponentEvent event) {
                SuggestionField.this.relocate();
            }

            @Override
            public void componentHidden(final ComponentEvent event) {
                SuggestionField.this.relocate();
            }
        });
        owner.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(final WindowEvent event) {
                // nothing todo
            }

            @Override
            public void windowIconified(final WindowEvent event) {
                SuggestionField.this._dialog.setVisible(false);
            }

            @Override
            public void windowDeiconified(final WindowEvent event) {
                // nothing todo
            }

            @Override
            public void windowDeactivated(final WindowEvent event) {
                // nothing todo
            }

            @Override
            public void windowClosing(final WindowEvent event) {
                SuggestionField.this._dialog.dispose();
            }

            public void windowClosed(final WindowEvent event) {
                SuggestionField.this._dialog.dispose();
            }

            public void windowActivated(final WindowEvent event) {
                // nothing todo
            }
        });
        addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                SuggestionField.this._dialog.setVisible(false);

                if ((SuggestionField.this.getText().equals("")) && (e.getOppositeComponent() != null) && (e.getOppositeComponent().getName() != null)) {
                    if (!e.getOppositeComponent().getName().equals("suggestFieldDropdownButton")) {
                        SuggestionField.this.setText(SuggestionField.this._hint);
                    }
                } else if (SuggestionField.this.getText().equals("")) {
                    SuggestionField.this.setText(SuggestionField.this._hint);
                }
            }

            public void focusGained(FocusEvent e) {
                if (SuggestionField.this.getText().equals(SuggestionField.this._hint)) {
                    SuggestionField.this.setText("");
                }

                SuggestionField.this.showSuggest();
            }
        });
        this._dialog = new JDialog(owner);
        this._dialog.setUndecorated(true);
        this._dialog.setFocusableWindowState(false);
        this._dialog.setFocusable(false);
        this._list = new JList();
        _list.setFixedCellWidth(160);
        this._list.addMouseListener(new MouseListener() {
            private int selected;

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                if (this.selected == SuggestionField.this._list.getSelectedIndex()) {
                    SuggestionField.this.setText((String) SuggestionField.this._list.getSelectedValue());
                    SuggestionField.this._lastChosenExistingVariable = SuggestionField.this._list.getSelectedValue().toString();
                    SuggestionField.this.fireActionEvent();
                    SuggestionField.this._dialog.setVisible(false);
                }
                this.selected = SuggestionField.this._list.getSelectedIndex();                
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {                
                fireActionEvent();
            }
        });
        _scrollPane = new JScrollPane(this._list, 20,
                31);
        this._dialog.add(_scrollPane);
        this._dialog.pack();
        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                SuggestionField.this.relocate();
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 27) {
                    SuggestionField.this._dialog.setVisible(false);
                    return;
                }
                if (e.getKeyCode() == 40) {
                    if (SuggestionField.this._dialog.isVisible()) {
                        SuggestionField.this._list.setSelectedIndex(SuggestionField.this._list.getSelectedIndex() + 1);
                        SuggestionField.this._list.ensureIndexIsVisible(SuggestionField.this._list.getSelectedIndex() + 1);
                        return;
                    }
                    SuggestionField.this.showSuggest();
                } else {
                    if (e.getKeyCode() == 38) {
                        SuggestionField.this._list.setSelectedIndex(SuggestionField.this._list.getSelectedIndex() - 1);
                        SuggestionField.this._list.ensureIndexIsVisible(SuggestionField.this._list.getSelectedIndex() - 1);
                        return;
                    }
                    if (((e.getKeyCode() == 10 ? 1 : 0) & (SuggestionField.this._list.getSelectedIndex() != -1 ? 1 : 0) & (SuggestionField.this._suggestions.size() > 0 ? 1 : 0)) != 0) {
                        SuggestionField.this.setText((String) SuggestionField.this._list.getSelectedValue());
                        SuggestionField.this._lastChosenExistingVariable = SuggestionField.this._list.getSelectedValue().toString();
                        SuggestionField.this.fireActionEvent();
                        SuggestionField.this._dialog.setVisible(false);
                        return;
                    }
                }
                SuggestionField.this.showSuggest();
            }
        });
        this._regular = getFont();
        this._busy = new Font(getFont().getName(), 2, getFont().getSize());
    }

    public SuggestionField(final Frame owner, final List<String> data) {
        this(owner);
        setSuggestData(data);
    }

    public boolean setSuggestData(final List<String> data) {
        if (data == null) {
            return false;
        }
        Collections.sort(data);
        this._data = data;
        this._list.setListData(data.toArray());
        return true;
    }

    public List<String> getSuggestData() {
        return Collections.unmodifiableList(this._data);
    }

    public void setPreferredSuggestSize(final Dimension size) {
        this._dialog.setPreferredSize(size);
    }

    public void setMinimumSuggestSize(final Dimension size) {
        this._dialog.setMinimumSize(size);
    }

    public void setMaximumSuggestSize(final Dimension size) {
        this._dialog.setMaximumSize(size);
    }

    public void showSuggest() {
        if (!getText().toLowerCase().contains(this._lastWord.toLowerCase())) {
            this._suggestions.clear();
        }
        if (this._suggestions.isEmpty()) {
            this._suggestions.addAll(this._data);
        }
        if (this._matcher != null) {
            this._matcher.stop = true;
        }
        this._matcher = new InterruptableMatcher();

        SwingUtilities.invokeLater(this._matcher);
        this._lastWord = getText();
        relocate();
    }

    public void hideSuggest() {
        this._dialog.setVisible(false);
    }

    public boolean isSuggestVisible() {
        return this._dialog.isVisible();
    }

    private synchronized void relocate() {
        try {            
            Point tmpLocation = getLocationOnScreen();
            _list.revalidate();                        
            final int listHeight = _scrollPane.getHeight();
            tmpLocation.y += - listHeight;
            this._dialog.setLocation(tmpLocation);                        
        } catch (IllegalComponentStateException exc) {
            // exc.printStackTrace();
        }
    }

    public void addSelectionListener(final ActionListener listener) {
        if (listener != null) {
            this._listeners.add(listener);
        }
    }

    public void removeSelectionListener(final ActionListener listener) {
        this._listeners.remove(listener);
    }

    private void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, 0, getText());        
        for (ActionListener listener : this._listeners) {
            listener.actionPerformed(event);
        }
    }

    public String getLastChosenExistingVariable() {
        return this._lastChosenExistingVariable;
    }

    public String getHint() {
        return this._hint;
    }

    public void setHint(String hint) {
        this._hint = hint;
    }

    public void setSuggestMatcher(final SuggestMatcher suggestMatcher) {
        this._suggestMatcher = suggestMatcher;
    }

    public boolean isCaseSensitive() {
        return this._caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this._caseSensitive = caseSensitive;
    }

    private class InterruptableMatcher extends Thread {

        private volatile boolean stop;

        private InterruptableMatcher() {
        }

        public void run() {
            try {
                SuggestionField.this.setFont(SuggestionField.this._busy);
                Iterator it = SuggestionField.this._suggestions.iterator();
                String word = SuggestionField.this.getText();
                while (it.hasNext()) {
                    if (this.stop) {
                        return;
                    }

                    if (SuggestionField.this._caseSensitive) {
                        if (!SuggestionField.this._suggestMatcher.matches((String) it.next(), word)) {
                            it.remove();
                        }
                    } else if (!SuggestionField.this._suggestMatcher.matches(
                            ((String) it.next()).toLowerCase(), word.toLowerCase())) {
                        it.remove();
                    }
                }

                SuggestionField.this.setFont(SuggestionField.this._regular);
                if (SuggestionField.this._suggestions.size() > 0) {
                    SuggestionField.this._list.setListData(SuggestionField.this._suggestions.toArray());
                    SuggestionField.this._list.setSelectedIndex(0);
                    SuggestionField.this._list.ensureIndexIsVisible(0);
                    SuggestionField.this._dialog.setVisible(true);
                } else {
                    SuggestionField.this._dialog.setVisible(false);
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }
}