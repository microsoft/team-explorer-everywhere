// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.vc.CLCTaskMonitor;
import com.microsoft.tfs.client.clc.vc.Main;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionAll;
import com.microsoft.tfs.client.clc.vc.options.OptionForce;
import com.microsoft.tfs.client.clc.vc.options.OptionNoAutoResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionNoSummary;
import com.microsoft.tfs.client.clc.vc.options.OptionOverwrite;
import com.microsoft.tfs.client.clc.vc.options.OptionPreview;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public final class CommandGet extends Command {
    protected static final Log log = LogFactory.getLog(CommandGet.class);

    private final TaskMonitor taskMonitor = new CLCTaskMonitor();

    public CommandGet() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        log.debug("Command Get implementation started"); //$NON-NLS-1$

        /*
         * Recursion defaults to OneLevel so when a directory is specified, its
         * contents will be updated (but not its subdirectories).
         */

        log.debug("Preparing GetOptions"); //$NON-NLS-1$

        final RecursionType r;
        if (findOptionType(OptionRecursive.class) != null) {
            r = RecursionType.FULL;
        } else {
            r = RecursionType.ONE_LEVEL;
        }

        GetOptions options = GetOptions.NONE;
        if (findOptionType(OptionPreview.class) != null) {
            options = options.combine(GetOptions.PREVIEW);
        }

        if (findOptionType(OptionAll.class) != null) {
            options = options.combine(GetOptions.GET_ALL);
        }

        if (findOptionType(OptionOverwrite.class) != null) {
            options = options.combine(GetOptions.OVERWRITE);
        }

        if (findOptionType(OptionForce.class) != null) {
            options = options.combine(GetOptions.OVERWRITE);
            options = options.combine(GetOptions.GET_ALL);
        }

        if (findOptionType(OptionNoAutoResolve.class) != null) {
            options = options.combine(GetOptions.NO_AUTO_RESOLVE);
        }

        if (findOptionType(OptionWorkspace.class) != null) {
            final String messageFormat = Messages.getString("CommandGet.WorkspaceOptionNotValidForCommandFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidOptionException(message);
        }

        final OptionVersion o = (OptionVersion) findOptionType(OptionVersion.class);
        VersionSpec optionVersion = null;
        if (o != null) {
            final VersionSpec[] versions = o.getParsedVersionSpecs();

            if (versions == null || versions.length == 0) {
                throw new InvalidOptionValueException(Messages.getString("CommandGet.ExactlyOneVersionSpecRequired")); //$NON-NLS-1$
            }

            if (versions.length > 1) {
                throw new InvalidOptionValueException(Messages.getString("CommandGet.VersionRangeNotPermitted")); //$NON-NLS-1$
            }

            optionVersion = versions[0];
        }

        /*
         * If no version option was supplied, default to latest.
         */
        if (optionVersion == null) {
            optionVersion = LatestVersionSpec.INSTANCE;
        }

        GetStatus status = null;
        Workspace workspace = null;

        try {
            TaskMonitorService.pushTaskMonitor(taskMonitor);

            if (getFreeArguments().length == 0) {
                log.debug("No free arguments specified"); //$NON-NLS-1$

                /*
                 * No paths were supplied, so we recursivley get the entire
                 * workspace that belongs to our current directory.
                 */

                final TFSTeamProjectCollection connection = createConnection();
                final VersionControlClient client = connection.getVersionControlClient();

                initializeClient(client);
                client.getEventEngine().addGetListener(this);
                client.getEventEngine().addConflictResolvedListener(this);
                workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

                log.debug("Workspace detected: " + workspace.getName()); //$NON-NLS-1$

                /*
                 * A getItems() with null as the requests causes the server to
                 * update all items in this workspace.
                 */
                log.debug("Executing Get requests."); //$NON-NLS-1$
                status = workspace.get(optionVersion, options);
                log.debug("Has executed Get requests."); //$NON-NLS-1$
            } else {
                log.debug(MessageFormat.format("{0} free arguments specified", getFreeArguments().length)); //$NON-NLS-1$
                // Use the default version they specified in the option.
                final QualifiedItem[] items = parseQualifiedItems(optionVersion, false, 0);
                final WorkspaceInfo singleWorkspace = findSingleCachedWorkspace(items);

                log.debug("Workspace detected: " + singleWorkspace.getName()); //$NON-NLS-1$

                if (items.length > 0) {
                    log.debug("Preparing VC client"); //$NON-NLS-1$
                    final TFSTeamProjectCollection connection = createConnection();
                    final VersionControlClient client = connection.getVersionControlClient();

                    initializeClient(client);
                    client.getEventEngine().addGetListener(this);
                    client.getEventEngine().addConflictResolvedListener(this);
                    workspace = realizeCachedWorkspace(singleWorkspace, client);

                    log.debug("Preparing GetRequests."); //$NON-NLS-1$
                    final GetRequest[] requests = new GetRequest[items.length];
                    for (int i = 0; i < items.length; i++) {
                        final QualifiedItem qi = items[i];

                        try {
                            requests[i] = qi.toGetRequest(r);
                        } catch (final ClassNotFoundException e) {
                            final String messageFormat = Messages.getString("CommandGet.ItemCouldNotBeConvertedFormat"); //$NON-NLS-1$
                            final String message = MessageFormat.format(messageFormat, qi.getPath(), Main.VENDOR_NAME);

                            throw new InvalidFreeArgumentException(message);
                        }
                    }

                    log.debug("Executing Get requests."); //$NON-NLS-1$
                    status = workspace.get(requests, options);
                    log.debug("Has executed Get requests."); //$NON-NLS-1$
                }
            }
        } finally {
            TaskMonitorService.popTaskMonitor();
        }

        if (status != null) {
            if (status.isNoActionNeeded()) {
                /*
                 * We downloaded all files without any problems.
                 */
                getDisplay().printLine(Messages.getString("CommandGet.AllFilesUpToDate")); //$NON-NLS-1$
            } else {
                if (findOptionType(OptionNoSummary.class) == null) {
                    displayGetSummary(status);
                }

                // VS opens the conflict UI here if there were conflicts, but we
                // will have already printed them above (possibly again in the
                // summary).
            }
        }

        log.debug("Command Get implementation finished"); //$NON-NLS-1$
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class,
            OptionRecursive.class,
            OptionPreview.class,
            OptionForce.class,
            OptionAll.class,
            OptionOverwrite.class,
            OptionNoAutoResolve.class,
        }, "[<itemSpec>...]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandGet.VCHelpText1"), //$NON-NLS-1$
            Messages.getString("CommandGet.VCHelpText2") //$NON-NLS-1$
        };
    }
}
