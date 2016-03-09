// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.microsoft.tfs.util.Platform;

/**
 * A simple implementation of a checked-box-enabled Group, similar to the MSFT
 * widget MS uses frequently in VisualStudio.
 *
 * Note: do NOT add widgets directly to this widget. Use getGroup() and add to
 * the group this widget creates. Example:
 *
 * ButtonGroup bg = new ButtonGroup(this, SWT.NONE); Group group =
 * bg.getGroup();
 *
 * Text textBox = new Text(group, SWT.BORDER); ...
 */
public class ButtonGroup extends BaseControl {
    private final Button button;
    private final Group group;

    private final Composite clientComposite;

    private boolean disableGroupWithButton = false;

    public ButtonGroup(final Composite parent, final int style) {
        super(parent, style);
        setLayout(new FormLayout());

        button = new Button(this, SWT.CHECK);
        group = new Group(this, SWT.NONE);

        group.setLayout(new FillLayout());

        clientComposite = new Composite(group, SWT.NONE);

        /*
         * On Windows, the button is part of this control, on the Group where
         * the Text would normally be
         */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final FormData buttonData = new FormData();
            buttonData.left = new FormAttachment(0, 9);
            buttonData.top = new FormAttachment(0, 0);
            button.setLayoutData(buttonData);

            final FormData groupData = new FormData();
            groupData.left = new FormAttachment(0, 0);
            groupData.top = new FormAttachment(0, 1);
            groupData.bottom = new FormAttachment(100, 0);
            groupData.right = new FormAttachment(100, 0);
            group.setLayoutData(groupData);
        }
        /* On Unix, the button is part of this control, above the group */
        else {
            final FormData buttonData = new FormData();
            buttonData.left = new FormAttachment(0, 8);
            buttonData.top = new FormAttachment(0, 0);
            buttonData.right = new FormAttachment(100, 0);
            button.setLayoutData(buttonData);

            final FormData groupData = new FormData();
            groupData.left = new FormAttachment(0, 0);
            groupData.top = new FormAttachment(button, 0, SWT.BOTTOM);
            groupData.bottom = new FormAttachment(100, 0);
            groupData.right = new FormAttachment(100, 0);
            group.setLayoutData(groupData);
        }

        button.addSelectionListener(new ButtonSelectionListener());
        clientComposite.addPaintListener(new ClientCompositePaintListener());
    }

    /**
     * The underlying group. Add all children to this.
     *
     * @return the Group that this widget controls
     */
    public Composite getClientComposite() {
        return clientComposite;
    }

    /**
     * Sets the receiver's text, which is the string that will be displayed as
     * the receiver's title, to the argument, which may not be null.
     *
     * @param text
     *        the receiver's title
     */
    public void setText(final String text) {
        button.setText(text);
    }

    /**
     * Returns the receiver's text, which is the string that the is used as the
     * title. If the text has not previously been set, returns an empty string.
     *
     * @return the receiver's title
     */
    public String getText() {
        return button.getText();
    }

    public Button getButton() {
        return button;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        button.setEnabled(enabled);
        setClientCompositeEnabled(enabled);
    }

    public boolean getSelection() {
        return button.getSelection();
    }

    public void setSelection(final boolean selected) {
        button.setSelection(selected);
    }

    /**
     * Returns the state of group enablement based on button selection. Returns
     * true if the group (and it's children) will be disabled when the button is
     * unselected. false if the two are not correlated.
     *
     * @return true if the group is enabled/disabled on button selection
     */
    public boolean isGroupDisabledWithButton() {
        return disableGroupWithButton;
    }

    /**
     * Sets the group to be enabled and disabled corresponding to button
     * selection.
     *
     * @param disableGroupWithButton
     *        true to enable/disable group according to button selection, false
     *        if the two are independent.
     */
    public void setGroupDisabledWithButton(final boolean disableGroupWithButton) {
        this.disableGroupWithButton = disableGroupWithButton;
    }

    /**
     * Sets the group (and its children) enabled or disabled.
     *
     * @param enabled
     *        true to enable, false to disable
     */
    private void setClientCompositeEnabled(final boolean enabled) {
        clientComposite.setEnabled(enabled);

        final Control[] children = clientComposite.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].setEnabled(enabled);
        }
    }

    /**
     * A simple class to manage group enablement based on button selection.
     */
    private class ButtonSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            if (disableGroupWithButton) {
                setClientCompositeEnabled(button.getSelection());
            }
        }
    }

    /**
     * A class to manage group enablement on paint: this allows callers to not
     * need to worry about group enablement the first time the ButtonGroup is
     * shown.
     */
    private class ClientCompositePaintListener implements PaintListener {
        @Override
        public void paintControl(final PaintEvent e) {
            if (disableGroupWithButton) {
                setClientCompositeEnabled(button.getSelection());
            }
        }
    }
}
