// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalCommandHelper;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class AddCommand extends TFSCommand {
    private final TFSRepository repository;
    private final String[] localPaths;
    private final boolean recursive;
    private final LockLevel lockLevel;
    private final GetOptions getOptions;
    private final PendChangesOptions pendChangesOptions;
    private final Map<String, FileEncoding> encodingHints;
    private final FileEncoding defaultEncoding;

    private boolean ignoreNonFatals = false;

    private final NonFatalCommandHelper nonFatalHelper;

    private int addCount;
    private NonFatalErrorEvent[] nonFatalErrors = new NonFatalErrorEvent[0];

    public AddCommand(final TFSRepository repository, final String localPath) {
        this(repository, new String[] {
            localPath
        });

        Check.notNull(localPath, "localPath"); //$NON-NLS-1$
    }

    public AddCommand(final TFSRepository repository, final String[] localPaths) {
        this(repository, localPaths, false, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
    }

    public AddCommand(
        final TFSRepository repository,
        final String[] localPaths,
        final boolean recursive,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendChangesOptions) {
        this(repository, localPaths, recursive, lockLevel, getOptions, pendChangesOptions, null, null);
    }

    /**
     * See
     * {@link Workspace#pendAdd(String[], boolean, LockLevel, GetOptions, PendChangesOptions, Map, FileEncoding)
     * for details about these parameters.
     */
    public AddCommand(
        final TFSRepository repository,
        final String[] localPaths,
        final boolean recursive,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendChangesOptions,
        final Map<String, FileEncoding> encodingHints,
        final FileEncoding defaultEncoding) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(localPaths, "localPaths"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendChangesOptions, "pendChangesOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.localPaths = localPaths;
        this.recursive = recursive;
        this.lockLevel = lockLevel;
        this.getOptions = getOptions;
        this.pendChangesOptions = pendChangesOptions;
        this.encodingHints = encodingHints;
        this.defaultEncoding = defaultEncoding;

        nonFatalHelper = new NonFatalCommandHelper(repository);
    }

    /**
     * Sets the option to ignore the non-fatals produced by an add when
     * reporting the {@link IStatus} of this method. If this option is true,
     * this command will return a severity of {@link IStatus#OK} even when
     * non-failures occur. If this option is false (the default), non-failures
     * will create a severity of {@link IStatus#WARNING} or
     * {@link IStatus#ERROR}, depending on whether some or all files failed,
     * respectively.
     *
     * @param ignoreNonFatals
     *        true to ignore the results of an add, false to report them
     */
    public void setIgnoreNonFatals(final boolean ignoreNonFatals) {
        this.ignoreNonFatals = ignoreNonFatals;
    }

    @Override
    public String getName() {
        if (localPaths.length == 1) {
            final String messageFormat = Messages.getString("AddCommand.SingleCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, localPaths[0]);
        } else {
            final String messageFormat = Messages.getString("AddCommand.MultiCommandTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, localPaths.length);
        }
    }

    @Override
    public String getErrorDescription() {
        if (localPaths.length == 1) {
            final String messageFormat = Messages.getString("AddCommand.SingleErrorTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, localPaths[0]);
        } else {
            final String messageFormat = Messages.getString("AddCommand.MultiErrorTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, localPaths.length);
        }
    }

    @Override
    public String getLoggingDescription() {
        if (localPaths.length == 1) {
            final String messageFormat = Messages.getString("AddCommand.SingleCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, localPaths[0]);
        } else {
            final String messageFormat = Messages.getString("AddCommand.MultiCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, localPaths.length);
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        nonFatalHelper.hookupListener();

        try {
            addCount = repository.getWorkspace().pendAdd(
                localPaths,
                recursive,
                lockLevel,
                getOptions,
                pendChangesOptions,
                encodingHints,
                defaultEncoding);
        } finally {
            nonFatalHelper.unhookListener();
        }

        /*
         * Store the items that failed for callers, if possible.
         */
        if (nonFatalHelper.hasNonFatals()) {
            nonFatalErrors = nonFatalHelper.getNonFatalErrors();
        }

        /*
         * Clients may set the "ignore non-fatals" option so that they may do
         * more advanced processing themselves. Eg, TFSResourceChangeListener
         * will make a second pass on failures to determine if the file already
         * exists in the workspace because another client pended an add.
         */
        if (!ignoreNonFatals && addCount < localPaths.length) {
            final int errorCount = localPaths.length - addCount;
            final int severity = (addCount > 0) ? IStatus.WARNING : IStatus.ERROR;

            return nonFatalHelper.getBestStatus(
                severity,
                errorCount,
                Messages.getString("AddCommand.FilesCouldNotBeAddedFormat")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    public int getAddCount() {
        return addCount;
    }

    public NonFatalErrorEvent[] getNonFatalErrors() {
        return nonFatalErrors;
    }
}
