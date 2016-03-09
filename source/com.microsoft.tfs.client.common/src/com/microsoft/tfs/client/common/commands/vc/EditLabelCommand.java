// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/*
 * Editing a label is (potentially) a two-stage operation: removing old elements
 * and adding new ones, this class handles both cases. Note that this is not
 * atomic.
 */
public class EditLabelCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final VersionControlLabel originalLabel;
    private final VersionControlLabel newLabel;
    private final ItemSpec[] removes;
    private final LabelItemSpec[] adds;

    public EditLabelCommand(
        final TFSRepository repository,
        final VersionControlLabel originalLabel,
        final VersionControlLabel newLabel,
        final ItemSpec[] removes,
        final LabelItemSpec[] adds) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(originalLabel, "originalLabel"); //$NON-NLS-1$
        Check.notNull(newLabel, "newLabel"); //$NON-NLS-1$

        this.repository = repository;
        this.originalLabel = originalLabel;
        this.newLabel = newLabel;
        this.removes = removes;
        this.adds = adds;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("EditLabelCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, newLabel.getName());
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("EditLabelCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("EditLabelCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, newLabel.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final int work = (removes != null && removes.length > 0 && adds != null && adds.length > 0) ? 2 : 1;

        final String messageFormat = Messages.getString("EditLabelCommand.ProgressTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, newLabel.getName());
        progressMonitor.beginTask(message, work);

        if (removes != null && removes.length > 0) {
            final SubProgressMonitor unlabelMonitor = new SubProgressMonitor(progressMonitor, 1);
            final UnlabelCommand unlabelCommand = new UnlabelCommand(repository, originalLabel, removes);

            try {
                final IStatus unlabelStatus = unlabelCommand.run(unlabelMonitor);

                if (!unlabelStatus.isOK()) {
                    return unlabelStatus;
                }
            } finally {
                unlabelMonitor.done();
            }
        }

        if ((adds != null && adds.length > 0) || !originalLabel.getComment().equals(newLabel.getComment())) {
            final SubProgressMonitor createMonitor = new SubProgressMonitor(progressMonitor, 1);
            final CreateLabelCommand createCommand = new CreateLabelCommand(repository, newLabel, adds);

            try {
                final IStatus createStatus = createCommand.run(createMonitor);

                if (!createStatus.isOK()) {
                    return createStatus;
                }
            } finally {
                createMonitor.done();
            }
        }

        return Status.OK_STATUS;
    }
}