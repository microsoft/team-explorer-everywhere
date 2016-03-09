// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteLabelCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String labelName;
    private final String scope;

    private LabelResult[] results;

    public DeleteLabelCommand(final TFSRepository repository, final VersionControlLabel label) {
        this(repository, label.getName(), label.getScope());
    }

    public DeleteLabelCommand(final TFSRepository repository, final String labelName, final String scope) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(labelName, "labelName"); //$NON-NLS-1$
        Check.notNull(scope, "scope"); //$NON-NLS-1$

        this.repository = repository;
        this.labelName = labelName;
        this.scope = scope;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("DeleteLabelCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, labelName);
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DeleteLabelCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("DeleteLabelCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, labelName);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        results = repository.getWorkspace().getClient().deleteLabel(labelName, scope);

        return Status.OK_STATUS;
    }

    public LabelResult[] getResults() {
        return results;
    }
}