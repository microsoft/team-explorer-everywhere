// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.prefs;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.externaltools.ExternalToolAssociation;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolValidator;
import com.microsoft.tfs.core.externaltools.validators.MergeToolValidator;

public class ExternalMergeToolDialog extends ExternalToolDialog {
    public ExternalMergeToolDialog(
        final Shell parentShell,
        final ExternalToolAssociation association,
        final boolean isNew,
        final ExternalToolAssociation[] otherAssociations) {
        super(parentShell, association, isNew, otherAssociations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getExternalToolType() {
        return Messages.getString("ExternalMergeToolDialog.ExternalToolType"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getExplanationText() {
        return Messages.getString("ExternalMergeToolDialog.ExplanationText"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRequiredText() {
        return Messages.getString("ExternalMergeToolDialog.RequiredArguments"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOptionalText() {
        return Messages.getString("ExternalMergeToolDialog.OptionalArguments"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExternalToolValidator getToolValidator() {
        return new MergeToolValidator();
    }
}
