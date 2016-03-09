// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryLabelsCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String label;
    private final String scope;
    private final String owner;
    private final boolean includeItemDetails;
    private final String filterItem;
    private final VersionSpec filterItemVersion;

    private VersionControlLabel[] labels;

    public QueryLabelsCommand(
        final TFSRepository repository,
        final String label,
        final String scope,
        final String owner) {
        this(repository, label, scope, owner, false);
    }

    public QueryLabelsCommand(
        final TFSRepository repository,
        final String label,
        final String scope,
        final String owner,
        final boolean includeItemDetails) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.label = label;
        this.scope = scope;
        this.owner = owner;
        this.includeItemDetails = includeItemDetails;
        filterItem = null;
        filterItemVersion = null;

        addExceptionHandler(new QueryLabelsExceptionHandler());

        setConnection(repository.getConnection());
        setCancellable(true);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryLabelsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryLabelsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryLabelsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        labels = repository.getWorkspace().queryLabels(
            label,
            scope,
            owner,
            includeItemDetails,
            filterItem,
            filterItemVersion);

        return Status.OK_STATUS;
    }

    public VersionControlLabel[] getLabels() {
        return labels;
    }

    private class QueryLabelsExceptionHandler implements ICommandExceptionHandler {
        @Override
        public IStatus onException(final Throwable t) {
            if (t instanceof TECoreException
                && t.getMessage().startsWith("TF14021: ") //$NON-NLS-1$
                && filterItemVersion instanceof DateVersionSpec) {
                final String messageFormat = Messages.getString("QueryLabelsCommand.DateIsBeforeAnyChangesetFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    DateHelper.getDefaultDateTimeFormat().format(
                        ((DateVersionSpec) filterItemVersion).getDate().getTime()));

                return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 14021, message, null);
            }

            return null;
        }
    }
}
