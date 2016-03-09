// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.ItemProperties;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Pends TFS 2012 property change(s) for one or more items. Don't use with
 * servers pre-TFS 2012.
 */
public class ChangePropertiesCommand extends TFSCommand {
    private final TFSRepository repository;
    private final ItemProperties[] properties;
    private final RecursionType recursion;
    private final LockLevel lockLevel;
    private final PendChangesOptions options;
    private final String[] itemPropertyFilters;

    private final NonFatalCommandHelper nonFatalHelper;

    private int pendedCount;

    /**
     * Pends property changes that set the given values on each given path.
     */
    public ChangePropertiesCommand(final TFSRepository repository, final String[] paths, final PropertyValue[] values) {
        this(repository, paths, values, RecursionType.NONE, LockLevel.UNCHANGED, PendChangesOptions.NONE, null);
    }

    /**
     * Pends property changes that set the given values on each given path.
     */
    public ChangePropertiesCommand(
        final TFSRepository repository,
        final String[] paths,
        final PropertyValue[] values,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final PendChangesOptions options,
        final String[] itemPropertyFilters) {
        this(repository, ItemProperties.fromStrings(paths, values), recursion, lockLevel, options, itemPropertyFilters);
    }

    /**
     * Pends the specified property changes.
     */
    public ChangePropertiesCommand(final TFSRepository repository, final ItemProperties[] properties) {
        this(repository, properties, RecursionType.NONE, LockLevel.UNCHANGED, PendChangesOptions.NONE, null);
    }

    /**
     * Pends the specified property changes.
     */
    public ChangePropertiesCommand(
        final TFSRepository repository,
        final ItemProperties[] properties,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final PendChangesOptions options,
        final String[] itemPropertyFilters) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(properties, "properties"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.repository = repository;
        this.properties = properties;
        this.recursion = recursion;
        this.lockLevel = lockLevel;
        this.options = options;
        this.itemPropertyFilters = itemPropertyFilters;

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    @Override
    public String getName() {
        if (properties.length == 1) {
            return MessageFormat.format(
                Messages.getString("PropertyCommand.SingleItemCommandTextFormat"), //$NON-NLS-1$
                properties[0].getPath());
        } else {
            return MessageFormat.format(
                Messages.getString("PropertyCommand.MultipleItemCommandTextFormat"), //$NON-NLS-1$
                properties.length);
        }
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("PropertyCommand.CommandErrorFormat"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (properties.length == 1) {
            final String messageFormat =
                Messages.getString("PropertyCommand.SingleItemCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, properties[0].getPath());
            return message;
        } else {
            final String messageFormat =
                Messages.getString("PropertyCommand.MultipleItemCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, properties.length);
            return message;
        }
    }

    @Override
    public IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            pendedCount = repository.getWorkspace().pendPropertyChange(
                properties,
                recursion,
                lockLevel,
                options,
                itemPropertyFilters);
        } finally {
            nonFatalHelper.unhookListener();
        }

        if (nonFatalHelper.hasNonFatals()) {
            final int errorCount = properties.length - pendedCount;
            final int severity = (pendedCount > 0) ? IStatus.WARNING : IStatus.ERROR;

            return nonFatalHelper.getBestStatus(
                severity,
                errorCount,
                Messages.getString("PropertyCommand.PropertiesCouldNotBeChangedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    public int getChangesPendedCount() {
        return pendedCount;
    }
}
