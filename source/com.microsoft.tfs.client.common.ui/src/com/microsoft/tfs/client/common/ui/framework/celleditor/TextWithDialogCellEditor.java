// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.celleditor;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.sizing.MeasureItemHeightListener;

public abstract class TextWithDialogCellEditor extends CellEditor {
    private Text text;
    private Button button;
    private boolean enableFocusLostBehavior = true;

    protected abstract String openDialog(Shell shell, String currentValue);

    public TextWithDialogCellEditor(final Composite parent, final int style) {
        super(parent, style);

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            final Table table = (Table) parent;
            table.addListener(/* SWT.MeasureItem */41, new MeasureItemHeightListener(5));
        }
    }

    @Override
    protected Control createControl(final Composite parent) {
        final Font font = parent.getFont();
        final Color bg = parent.getBackground();

        final Composite composite = new Composite(parent, getStyle());
        composite.setFont(font);
        composite.setBackground(bg);
        composite.setLayout(new CellEditorLayout());

        text = new Text(composite, SWT.NONE);
        text.setFont(font);
        text.setBackground(bg);

        text.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                }
            }
        });

        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                fireApplyEditorValue();
                deactivate();
            }
        });

        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.character == '\u001b') {
                    // Escape character
                    fireCancelEditor();
                }
            }
        });

        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final boolean oldValidState = isValueValid();
                final boolean newValidState = isCorrect(getNewValue());
                valueChanged(oldValidState, newValidState);
            }
        });

        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                if (!enableFocusLostBehavior) {
                    return;
                }
                TextWithDialogCellEditor.this.focusLost();
            }
        });

        /*
         * Don't use round buttons on Mac OS Cocoa, use flat-style (boxy)
         * buttons
         */
        final int buttonStyle = (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) ? SWT.FLAT : SWT.NONE;

        button = new Button(composite, buttonStyle);
        button.setText(Messages.getString("TextWithDialogCellEditor.ButtonText")); //$NON-NLS-1$
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                enableFocusLostBehavior = false;
                final String currentValue = (String) doGetValue();
                final String newValue = openDialog(button.getShell(), currentValue);
                doSetValue(newValue);
                text.selectAll();
                text.setFocus();
                enableFocusLostBehavior = true;
            }
        });

        /*
         * Mouse track listener around buttons in cell editors it totally
         * screwed up on OS X. It can't measure things properly at all. So, we
         * use the focus lost behavior switching in widgetSelected() above for
         * Mac.
         */
        if (!WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            button.addMouseTrackListener(new MouseTrackAdapter() {
                @Override
                public void mouseEnter(final MouseEvent e) {
                    enableFocusLostBehavior = false;
                }

                @Override
                public void mouseExit(final MouseEvent e) {
                    enableFocusLostBehavior = true;
                }
            });
        }

        return composite;
    }

    @Override
    protected Object doGetValue() {
        String s = text.getText();
        if (s.trim().length() == 0) {
            s = null;
        }
        return s;
    }

    @Override
    protected void doSetFocus() {
        if (text != null) {
            text.selectAll();
            text.setFocus();
        }
    }

    @Override
    protected void doSetValue(Object value) {
        if (value == null) {
            value = ""; //$NON-NLS-1$
        }

        Assert.isTrue(text != null && (value instanceof String));

        text.setText((String) value);
    }

    private Object getNewValue() {
        final String newValue = text.getText();
        if (newValue == null) {
            return ""; //$NON-NLS-1$
        } else {
            return newValue;
        }
    }

    private class CellEditorLayout extends Layout {
        @Override
        public void layout(final Composite editor, final boolean force) {
            final Rectangle bounds = editor.getClientArea();

            if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
                text.setBounds(0, 0, bounds.width - 40, bounds.height);
                button.setBounds(bounds.width - 40, -5, 40, bounds.height);
            } else {
                final Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
                text.setBounds(0, 0, bounds.width - size.x, bounds.height);
                button.setBounds(bounds.width - size.x, 0, size.x, bounds.height);
            }
        }

        @Override
        public Point computeSize(final Composite editor, final int wHint, final int hHint, final boolean force) {
            if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
                return new Point(wHint, hHint);
            }

            final Point textSize = text.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
            final Point buttonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
            return new Point(buttonSize.x, Math.max(textSize.y, buttonSize.y));
        }
    }
}
