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
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelChildOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class CreateLabelCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final VersionControlLabel label;
    private final LabelItemSpec[] items;
    private final LabelChildOption options;

    private LabelResult[] labelResults;

    public CreateLabelCommand(
        final TFSRepository repository,
        final VersionControlLabel label,
        final LabelItemSpec[] items) {
        this(repository, label, items, LabelChildOption.FAIL);
    }

    public CreateLabelCommand(
        final TFSRepository repository,
        final VersionControlLabel label,
        final LabelItemSpec[] items,
        final LabelChildOption options) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(label, "label"); //$NON-NLS-1$
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.repository = repository;
        this.label = label;
        this.items = items;
        this.options = options;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("CreateLabelCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, label.getName());
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("CreateLabelCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, label.getName());
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("CreateLabelCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, label.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        labelResults = repository.getWorkspace().createLabel(label, items, options);

        return Status.OK_STATUS;
    }

    public LabelResult[] getLabelResults() {
        return labelResults;
    }
}
