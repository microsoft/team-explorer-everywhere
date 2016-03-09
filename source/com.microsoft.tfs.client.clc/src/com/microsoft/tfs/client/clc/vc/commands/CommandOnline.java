// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.vc.options.OptionAdds;
import com.microsoft.tfs.client.clc.vc.options.OptionDeletes;
import com.microsoft.tfs.client.clc.vc.options.OptionDiff;
import com.microsoft.tfs.client.clc.vc.options.OptionExclude;
import com.microsoft.tfs.client.clc.vc.options.OptionPreview;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChange;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflinePender;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizer;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerFilter;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerMethod;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerPathProvider;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.FileHelpers;

public class CommandOnline extends Command {
    public CommandOnline() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        String[] paths;

        final boolean preview = (findOptionType(OptionPreview.class) != null);
        final boolean detectAdds = (findOptionType(OptionAdds.class) != null);
        final boolean detectDeletes = (findOptionType(OptionDeletes.class) != null);

        final OptionExclude optionExclude = (OptionExclude) findOptionType(OptionExclude.class);
        final String[] excludes = (optionExclude != null) ? optionExclude.getValues() : null;

        // default to one-level recursion, allow full recursion with -recursive
        final RecursionType recurType =
            (findOptionType(OptionRecursive.class) != null) ? RecursionType.FULL : RecursionType.ONE_LEVEL;

        // default to writable file detection, use md5 hash detection if
        // detected
        final OfflineSynchronizerMethod method = (findOptionType(OptionDiff.class) != null)
            ? OfflineSynchronizerMethod.MD5_HASH : OfflineSynchronizerMethod.WRITABLE_FILES;

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        // get paths to return online for as free arguments, or use all
        // mapped workspace paths if there were none
        paths = getFreeArguments();
        if (paths == null || paths.length == 0) {
            paths = getWorkspacePaths(workspace);
        } else {
            paths = canonicalizePaths(paths);
        }

        // NOTE: incompatibility with tfpt.exe: Microsoft only displays paths
        // if you've passed the /deletes option.
        for (int i = 0; i < paths.length; i++) {
            final String messageFormat = Messages.getString("CommandOnline.CheckingStatusOfItemsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, paths[i]);

            getDisplay().printLine(message);
        }

        final OfflineSynchronizerPathProvider provider = new OfflineSynchronizerPathProvider(paths);

        final OfflineSynchronizer synchronizer = new OfflineSynchronizer(workspace, provider);
        synchronizer.setMethod(method);
        synchronizer.setRecursionType(recurType);

        if (excludes != null) {
            synchronizer.setFilter(new WildcardExclusionFilter(excludes));
        }

        // turn on add and delete detection modes
        synchronizer.setDetectAdded(detectAdds);
        synchronizer.setDetectDeleted(detectDeletes);
        // synchronizer.setProgressReporter(new ChangeTypeReporter());

        try {
            synchronizer.detectChanges();
        } catch (final Exception e) {
            getDisplay().printErrorLine(e.getMessage());
            setExitCode(ExitCode.FAILURE);
            return;
        }

        final OfflineChange[] changes = synchronizer.getChanges();

        if (changes.length == 0) {
            getDisplay().printLine(Messages.getString("CommandOnline.NoChangesFoundToPend")); //$NON-NLS-1$
            return;
        }

        if (preview) {
            getDisplay().printLine(Messages.getString("CommandOnline.ShowingPotentitalChangesOnly")); //$NON-NLS-1$
        }

        int errors = 0;

        final OfflineChangeType[] changeTypes = new OfflineChangeType[] {
            OfflineChangeType.UNDO,
            OfflineChangeType.ADD,
            OfflineChangeType.EDIT,
            OfflineChangeType.DELETE
        };
        for (int i = 0; i < changeTypes.length; i++) {
            final OfflineChange[] changesByType = OfflineChange.getChangesByType(changes, changeTypes[i]);

            if (preview) {
                showChanges(changesByType, changeTypes[i]);
            } else {
                errors += pendChanges(workspace, changesByType, changeTypes[i]);
            }
        }

        if (preview) {
            getDisplay().printLine(Messages.getString("CommandOnline.ShowingPotentitalChangesOnly")); //$NON-NLS-1$
        }

        if (errors > 0) {
            getDisplay().printErrorLine(Messages.getString("CommandOnline.SomeChangesCouldNotBePended")); //$NON-NLS-1$
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }
    }

    private String[] getWorkspacePaths(final Workspace workspace) {
        final WorkingFolder[] workFolds = workspace.getFolders();

        final String[] paths = new String[workFolds.length];

        for (int i = 0; i < workFolds.length; i++) {
            paths[i] = workFolds[i].getLocalItem();
        }

        return paths;
    }

    /**
     * Canonicalize local paths.
     *
     * @param paths
     * @return
     * @throws IOException
     */
    private String[] canonicalizePaths(final String[] paths) {
        final String[] canonical = new String[paths.length];

        for (int i = 0; i < paths.length; i++) {
            canonical[i] = LocalPath.canonicalize(paths[i]);
        }

        return canonical;
    }

    @Override
    public void onNewPendingChange(final PendingChangeEvent e) {
        final ChangeType changeType = e.getPendingChange().getChangeType();

        String changeName;
        if (changeType.contains(ChangeType.ADD)) {
            changeName = "add"; //$NON-NLS-1$
        } else if (changeType.contains(ChangeType.DELETE)) {
            changeName = "delete"; //$NON-NLS-1$
        } else if (changeType.contains(ChangeType.EDIT)) {
            changeName = "edit"; //$NON-NLS-1$
        } else {
            changeName = "unknown"; //$NON-NLS-1$
        }

        getDisplay().printLine(" " //$NON-NLS-1$
            + changeName
            + ": " //$NON-NLS-1$
            + ((e.getPendingChange().getLocalItem() != null) ? e.getPendingChange().getLocalItem()
                : e.getPendingChange().getServerItem()));
    }

    /**
     * Display (to stdout) the list(s) of changes that are occuring.
     *
     * @param synchronizer
     *        an OfflineSynchronizer containing changes
     * @param type
     *        the type of changes to display
     */
    private void showChanges(final OfflineChange[] typedChanges, final OfflineChangeType type) {
        if (typedChanges.length == 0) {
            return;
        }

        final String typeString = type.toString().toLowerCase();
        getDisplay().printLine(type.toString() + "s:"); //$NON-NLS-1$

        for (int i = 0; i < typedChanges.length; i++) {
            getDisplay().printLine(" " + typeString + ": " + typedChanges[i].getLocalPath()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Actually pend changes to the server.
     *
     * @param typedChanges
     *        a list of changes to pend
     * @param type
     *        the type of changes to pend
     * @return number of errors
     */
    private int pendChanges(
        final Workspace workspace,
        final OfflineChange[] typedChanges,
        final OfflineChangeType type) {
        if (typedChanges.length == 0) {
            return 0;
        }

        getDisplay().printLine(type.toString() + "s:"); //$NON-NLS-1$

        final OfflinePender pender = new OfflinePender(workspace, typedChanges);
        return pender.pendChanges();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.commands.Command#getCommandHelpText()
     */
    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandOnline.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandOnline.HelpText2") //$NON-NLS-1$
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.clc.commands.Command#getSupportedOptionSets()
     */
    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionAdds.class,
            OptionDeletes.class,
            OptionDiff.class,
            OptionExclude.class,
            OptionRecursive.class,
            OptionPreview.class
        }, "[<itemSpec>...]"); //$NON-NLS-1$
        return optionSets;
    }

    private class WildcardExclusionFilter extends OfflineSynchronizerFilter {
        private final String[] excludes;

        public WildcardExclusionFilter(final String[] excludes) {
            this.excludes = excludes;
        }

        @Override
        public boolean shouldPend(final File file, final OfflineChangeType changeType, final ItemType serverItemType) {
            try {
                return filenameIncluded(file.getCanonicalPath());
            } catch (final Exception e) {
                return false;
            }
        }

        @Override
        public boolean shouldRecurse(final File file) {
            try {
                return filenameIncluded(file.getCanonicalPath());
            } catch (final Exception e) {
                return false;
            }
        }

        private boolean filenameIncluded(final String path) {
            // compare the filename (last element) of the path with the
            // exclusion list
            final String filename = LocalPath.getFileName(path);

            for (int i = 0; i < excludes.length; i++) {
                if (FileHelpers.filenameMatches(filename, excludes[i])) {
                    return false;
                }
            }

            return true;
        }
    }
}