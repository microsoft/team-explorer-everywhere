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
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UnlabelCommand extends TFSConnectedCommand {
    private final TFSRepository repository;

    private final String labelName;
    private final String labelScope;
    private final ItemSpec[] items;
    private final VersionSpec versionSpec;

    private LabelResult[] results;

    public UnlabelCommand(final TFSRepository repository, final VersionControlLabel label, final ItemSpec[] items) {
        this(
            repository,
            label.getName(),
            label.getScope(),
            items,
            new LabelVersionSpec(new LabelSpec(label.getName(), label.getScope())));
    }

    public UnlabelCommand(
        final TFSRepository repository,
        final VersionControlLabel label,
        final ItemSpec[] items,
        final VersionSpec versionSpec) {
        this(repository, label.getName(), label.getScope(), items, versionSpec);
    }

    public UnlabelCommand(
        final TFSRepository repository,
        final String labelName,
        final String labelScope,
        final ItemSpec[] items,
        final VersionSpec versionSpec) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(labelName, "labelName"); //$NON-NLS-1$
        Check.notNull(labelScope, "labelScope"); //$NON-NLS-1$
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        this.repository = repository;
        this.labelName = labelName;
        this.labelScope = labelScope;
        this.items = items;
        this.versionSpec = versionSpec;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        if (items.length == 1) {
            final String messageFormat = Messages.getString("UnlabelCommand.CommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, labelName);
        } else {
            return (Messages.getString("UnlabelCommand.MultiItemRemoveText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (items.length == 1) {
            return (Messages.getString("UnlabelCommand.SingleItemErrorText")); //$NON-NLS-1$
        } else {
            return (Messages.getString("UnlabelCommand.MultiItemErrorText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (items.length == 1) {
            final String messageFormat = Messages.getString("UnlabelCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, labelName);
        } else {
            return (Messages.getString("UnlabelCommand.MultiItemRemoveText", LocaleUtil.ROOT)); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        results = repository.getWorkspace().unlabelItem(labelName, labelScope, items, versionSpec);

        return Status.OK_STATUS;
    }

    public LabelResult[] getResults() {
        return results;
    }
}
