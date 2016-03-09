// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeConflictDescription;
import com.microsoft.tfs.util.Check;

public class ConflictNameSelectionControl extends BaseControl {
    private final Button yourNameButton;
    private final Button theirNameButton;
    private final Button newNameButton;

    private final Text yourNameText;
    private final Text theirNameText;
    private final Text newNameText;

    private String yourPath;
    private String theirPath;

    private boolean insideStateUpdate = false;
    private String lastFilename = null;

    public ConflictNameSelectionControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        yourNameButton = new Button(this, SWT.RADIO);
        yourNameButton.setText(Messages.getString("ConflictNameSelectionControl.YourNameButtonText")); //$NON-NLS-1$
        yourNameButton.setSelection(true);

        yourNameText = new Text(this, SWT.READ_ONLY);
        yourNameText.setText(""); //$NON-NLS-1$
        yourNameText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(yourNameText);

        theirNameButton = new Button(this, SWT.RADIO);
        theirNameButton.setText(Messages.getString("ConflictNameSelectionControl.TheirNameButtonText")); //$NON-NLS-1$

        theirNameText = new Text(this, SWT.READ_ONLY);
        theirNameText.setText(""); //$NON-NLS-1$
        theirNameText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(theirNameText);

        newNameButton = new Button(this, SWT.RADIO);
        newNameButton.setText(Messages.getString("ConflictNameSelectionControl.NewNameButtonText")); //$NON-NLS-1$

        newNameText = new Text(this, SWT.BORDER);
        newNameText.setEnabled(false);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(newNameText);
        ControlSize.setCharWidthHint(newNameText, 50);

        final SelectionListener buttonSelectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                updateState();
            }
        };

        yourNameButton.addSelectionListener(buttonSelectionListener);
        theirNameButton.addSelectionListener(buttonSelectionListener);
        newNameButton.addSelectionListener(buttonSelectionListener);

        final ModifyListener textModifyListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                notifyListeners();
            }
        };

        newNameText.addModifyListener(textModifyListener);

        /*
         * We do not want either of our read only text controls in the tab list.
         * Strangely, Buttons are not in the tab list, only Text fields. Thus,
         * the only Control we want in the tab list is the newNameText. The
         * buttons will still be tab-able, even though they are not in this
         * list.
         */
        setTabList(new Control[] {
            newNameText
        });
    }

    private void updateState() {
        /* Avoid recursion */
        if (insideStateUpdate) {
            return;
        }

        insideStateUpdate = true;

        try {
            if (yourNameButton.getSelection()) {
                newNameText.setText(yourNameText.getText());
                newNameText.setEnabled(false);
            } else if (theirNameButton.getSelection()) {
                newNameText.setText(theirNameText.getText());
                newNameText.setEnabled(false);
            } else if (newNameButton.getSelection()) {
                newNameText.setEnabled(true);
            }
        } finally {
            insideStateUpdate = false;
        }

        notifyListeners();
    }

    private void notifyListeners() {
        final String newFilename = getFilename();

        if ((newFilename == null && lastFilename != null)
            || (newFilename != null && !newFilename.equals(lastFilename))) {
            final Event modifyEvent = new Event();
            modifyEvent.display = getDisplay();
            modifyEvent.widget = this;

            notifyListeners(SWT.Modify, modifyEvent);
        }

        lastFilename = newFilename;
    }

    public void setConflictDescription(final ConflictDescription conflictDescription) {
        Check.notNull(conflictDescription, "conflictDescription"); //$NON-NLS-1$

        /*
         * Merge renames need yourServerItemSource. Version renames need their
         * server item.
         */

        String yourPrompt = Messages.getString("ConflictNameSelectionControl.Use"); //$NON-NLS-1$
        String theirPrompt = Messages.getString("ConflictNameSelectionControl.Use"); //$NON-NLS-1$

        /*
         * In a merge conflict, we need to reparent the names based on the merge
         * target path (ie, YourServerItem.)
         */
        if (conflictDescription instanceof MergeConflictDescription) {
            yourPrompt = Messages.getString("ConflictNameSelectionControl.Take"); //$NON-NLS-1$
            yourPath = conflictDescription.getConflict().getYourServerItem();

            theirPrompt = Messages.getString("ConflictNameSelectionControl.Keep"); //$NON-NLS-1$
            theirPath = conflictDescription.getConflict().getYourServerItemSource();
        }
        /* Version Conflicts: simpler */
        else {
            yourPrompt = Messages.getString("ConflictNameSelectionControl.Keep"); //$NON-NLS-1$
            yourPath = conflictDescription.getConflict().getYourServerItem();

            theirPrompt = Messages.getString("ConflictNameSelectionControl.Take"); //$NON-NLS-1$
            theirPath = conflictDescription.getConflict().getTheirServerItem();
        }

        final String nullFormat = Messages.getString("ConflictNameSelectionControl.NullPathFormat"); //$NON-NLS-1$
        final String nonNullFormat = Messages.getString("ConflictNameSelectionControl.NonNullPathFormat"); //$NON-NLS-1$
        final String localDescription = conflictDescription.getLocalFileDescription();
        final String serverDescription = conflictDescription.getRemoteFileDescription();

        if (yourPath == null) {
            yourNameButton.setText(MessageFormat.format(nullFormat, yourPrompt, localDescription));
            yourNameButton.setEnabled(false);
            yourNameText.setText(""); //$NON-NLS-1$
        } else {
            yourNameButton.setText(MessageFormat.format(nonNullFormat, yourPrompt, localDescription));
            yourNameButton.setEnabled(true);
            yourNameText.setText(yourPath);
            newNameText.setText(yourPath);
        }

        if (theirPath == null) {
            theirNameButton.setText(MessageFormat.format(nullFormat, theirPrompt, serverDescription));
            theirNameButton.setEnabled(false);
            theirNameText.setText(""); //$NON-NLS-1$
        } else {
            theirNameButton.setText(MessageFormat.format(nonNullFormat, theirPrompt, serverDescription));
            theirNameButton.setEnabled(true);
            theirNameText.setText(theirPath);
        }
    }

    public String getFilename() {
        String filename = null;

        if (yourNameButton.getSelection() && yourPath != null) {
            filename = yourPath;
        } else if (theirNameButton.getSelection() && theirPath != null) {
            filename = theirPath;
        } else if (newNameButton.getSelection() && newNameText.getText().length() > 0) {
            filename = newNameText.getText();
        }

        return filename;
    }

    public void addModifyListener(final ModifyListener modifyListener) {
        addListener(SWT.Modify, new TypedListener(modifyListener));
    }
}
