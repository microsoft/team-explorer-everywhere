// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeConflictDescription;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

public class ConflictEncodingSelectionControl extends BaseControl {
    private final Combo encodingCombo;

    private ConflictEncodingDescription[] availableEncodings;

    private FileEncoding selectedEncoding;

    public ConflictEncodingSelectionControl(final Composite parent, final int style) {
        super(parent, style);

        final FillLayout layout = new FillLayout();
        layout.spacing = getSpacing();
        setLayout(layout);

        encodingCombo = new Combo(this, SWT.READ_ONLY);
    }

    public void setConflictDescription(final ConflictDescription conflictDescription) {
        Check.notNull(conflictDescription, "conflictDescription"); //$NON-NLS-1$

        if (conflictDescription instanceof MergeConflictDescription) {
            availableEncodings = new ConflictEncodingDescription[3];

            final FileEncoding sourceEncoding = conflictDescription.getConflict().getTheirEncoding();
            availableEncodings[0] = new ConflictEncodingDescription(
                MessageFormat.format(
                    Messages.getString("ConflictEncodingSelectionControl.TakeSourceEncodingDescriptionFormat"), //$NON-NLS-1$
                    sourceEncoding.getName()),
                sourceEncoding);

            final FileEncoding targetEncoding = conflictDescription.getConflict().getYourEncoding();
            availableEncodings[1] = new ConflictEncodingDescription(
                MessageFormat.format(
                    Messages.getString("ConflictEncodingSelectionControl.KeepTargetEncodingDescriptionFormat"), //$NON-NLS-1$
                    targetEncoding.getName()),
                targetEncoding);

            final FileEncoding baseEncoding = conflictDescription.getConflict().getBaseEncoding();
            availableEncodings[2] = new ConflictEncodingDescription(
                MessageFormat.format(
                    Messages.getString("ConflictEncodingSelectionControl.TakeBaseEncodingDescriptionFormat"), //$NON-NLS-1$
                    baseEncoding.getName()),
                baseEncoding);
        } else {
            availableEncodings = new ConflictEncodingDescription[2];

            final FileEncoding localEncoding = conflictDescription.getConflict().getYourEncoding();
            availableEncodings[0] = new ConflictEncodingDescription(
                MessageFormat.format(
                    Messages.getString("ConflictEncodingSelectionControl.LocalEncodingDescriptionFormat"), //$NON-NLS-1$
                    localEncoding.getName()),
                localEncoding);

            final FileEncoding serverEncoding = conflictDescription.getConflict().getTheirEncoding();
            availableEncodings[1] = new ConflictEncodingDescription(
                MessageFormat.format(
                    Messages.getString("ConflictEncodingSelectionControl.ServerEncodingDescriptionFormat"), //$NON-NLS-1$
                    serverEncoding.getName()),
                serverEncoding);
        }

        for (int i = 0; i < availableEncodings.length; i++) {
            encodingCombo.add(availableEncodings[i].getDescription());
        }

        selectedEncoding = availableEncodings[0].getFileEncoding();
        encodingCombo.select(0);

        encodingCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedEncoding = availableEncodings[encodingCombo.getSelectionIndex()].getFileEncoding();
            }
        });
    }

    public FileEncoding getFileEncoding() {
        return selectedEncoding;
    }

    private final static class ConflictEncodingDescription {
        private final FileEncoding fileEncoding;
        private final String description;

        public ConflictEncodingDescription(final String description, final FileEncoding fileEncoding) {
            Check.notNull(description, "description"); //$NON-NLS-1$
            Check.notNull(fileEncoding, "fileEncoding"); //$NON-NLS-1$

            this.description = description;
            this.fileEncoding = fileEncoding;
        }

        public String getDescription() {
            return description;
        }

        public FileEncoding getFileEncoding() {
            return fileEncoding;
        }
    }
}
