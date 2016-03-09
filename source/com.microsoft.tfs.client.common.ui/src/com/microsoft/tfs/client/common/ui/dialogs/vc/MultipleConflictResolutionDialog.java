// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.MultipleConflictResolutionControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictCategory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;

/**
 * MultipleConflictResolutionDialog offers resolution of multiple conflicts
 * (which may be multiple types of conflicts: eg, version, writable, etc.)
 */
public class MultipleConflictResolutionDialog extends BaseDialog {
    private final Map descriptionsByCategory = new TreeMap();
    private final Map controlsByCategory = new HashMap();

    private final Map resolutionsByCategory = new HashMap();

    public MultipleConflictResolutionDialog(final Shell parentShell, final ConflictDescription[] descriptions) {
        super(parentShell);

        setOptionResizable(false);
        setOptionPersistGeometry(false);

        setConflictDescriptions(descriptions);
    }

    public void setConflictDescriptions(final ConflictDescription[] descriptions) {
        descriptionsByCategory.clear();

        for (int i = 0; i < descriptions.length; i++) {
            final ConflictCategory conflictCategory = descriptions[i].getConflictCategory();
            List descriptionList = (List) descriptionsByCategory.get(conflictCategory);

            if (descriptionList == null) {
                descriptionList = new ArrayList();
                descriptionsByCategory.put(conflictCategory, descriptionList);
            }

            descriptionList.add(descriptions[i]);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAddToDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(1, true);
        dialogLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        dialogLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        dialogLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        dialogArea.setLayout(dialogLayout);

        controlsByCategory.clear();

        final Label explanationLabel = new Label(dialogArea, SWT.WRAP);
        explanationLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        explanationLabel.setText(Messages.getString("MultipleConflictResolutionDialog.ExplainLabelText")); //$NON-NLS-1$

        /* Add some additional spacing */
        final Composite spacer = new Composite(dialogArea, SWT.NONE);
        spacer.setLayoutData(new GridData(1, 1));

        // get the conflict types...
        final Iterator i = descriptionsByCategory.keySet().iterator();
        while (i.hasNext()) {
            final ConflictCategory conflictCategory = (ConflictCategory) i.next();
            final List descriptions = (List) descriptionsByCategory.get(conflictCategory);

            final ConflictCategory category = ((ConflictDescription) descriptions.get(0)).getConflictCategory();

            final MultipleConflictResolutionControl resolveControl =
                new MultipleConflictResolutionControl(dialogArea, SWT.NONE);

            resolveControl.setConflictCategory(category);
            resolveControl.setConflictCount(descriptions.size());

            resolveControl.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    updateSelection();
                }
            });

            resolveControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            controlsByCategory.put(conflictCategory, resolveControl);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookDialogAboutToClose()
     */
    @Override
    protected void hookDialogAboutToClose() {
        final Iterator i = controlsByCategory.keySet().iterator();
        while (i.hasNext()) {
            final ConflictCategory conflictCategory = (ConflictCategory) i.next();

            final MultipleConflictResolutionControl resolveControl =
                (MultipleConflictResolutionControl) controlsByCategory.get(conflictCategory);

            resolutionsByCategory.put(conflictCategory, resolveControl.getResolution());
        }
    }

    public ConflictResolution getResolution(final ConflictDescription description) {
        final ConflictCategory conflictCategory = description.getConflictCategory();
        final ConflictResolution dummyResolution = (ConflictResolution) resolutionsByCategory.get(conflictCategory);

        if (dummyResolution == null) {
            return null;
        }

        return dummyResolution.newForConflictDescription(description);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("MultipleConflictResolutionDialog.DialogTitle"); //$NON-NLS-1$
    }

    private void updateSelection() {
        boolean okEnabled = false;

        for (final Iterator i = controlsByCategory.keySet().iterator(); i.hasNext();) {
            final MultipleConflictResolutionControl control =
                (MultipleConflictResolutionControl) controlsByCategory.get(i.next());

            if (control.getSelection()) {
                okEnabled = true;
                break;
            }
        }

        getButton(IDialogConstants.OK_ID).setEnabled(okEnabled);
    }
}
