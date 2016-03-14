// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.vc.options.OptionCloak;
import com.microsoft.tfs.client.clc.vc.options.OptionDecloak;
import com.microsoft.tfs.client.clc.vc.options.OptionMap;
import com.microsoft.tfs.client.clc.vc.options.OptionUnmap;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.client.clc.vc.printers.BasicPrinter;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public final class CommandWorkFold extends Command {
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length > 2) {
            final String messageFormat = Messages.getString("CommandWorkFold.CommandRequiresZeroOneTwoPathsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        boolean mapOption = false;
        boolean unmapOption = false;
        boolean cloakOption = false;
        boolean decloakOption = false;
        int optionCount = 0;

        if (findOptionType(OptionMap.class) != null) {
            mapOption = true;
            ++optionCount;
        }

        if (findOptionType(OptionUnmap.class) != null) {
            unmapOption = true;
            ++optionCount;
        }

        if (findOptionType(OptionCloak.class) != null) {
            cloakOption = true;
            ++optionCount;
        }

        if (findOptionType(OptionDecloak.class) != null) {
            decloakOption = true;
            ++optionCount;
        }

        /*
         * Remove the invalid option combinations right away. Only one of map,
         * unmap, cloak, and decloak can be set.
         */
        if (optionCount > 1) {
            throw new InvalidOptionException(Messages.getString("CommandWorkFold.OneOneOfMapUnmapCloakDecloak")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * If one free argument and no options were supplied, we don't let
         * determineWorkspace() find the workspace, because it will ignore this
         * argument (so we find it ourselves).
         */
        if (getFreeArguments().length == 1 && optionCount == 0) {
            handleSingleWorkingFolder(getFreeArguments()[0], client);
            return;
        }

        /*
         * Now that we've handled the case where no workspace was specified but
         * the current directory should be ignored, we can use the automatic
         * detection. This call will load the appropriate workspace, depending
         * on the server and workspace options' values, and if that's no help,
         * uses the current directory. This call will also handle throwing an
         * exception if the workspace can't be found.
         */
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        /*
         * If zero or one arguments were supplied, and no options were set,
         * we're just in print mode.
         */
        if (getFreeArguments().length <= 1 && optionCount == 0) {
            /*
             * Print the workspace.
             */
            printWorkspace(workspace);
            return;
        }

        /*
         * If map is set and a single map file argument is passed, handle it.
         */
        if (mapOption && getFreeArguments().length == 1 && getFreeArguments()[0].startsWith("@")) //$NON-NLS-1$
        {
            final String filename = getFreeArguments()[0].substring(1);

            updateFromFile(workspace, filename);

            return;
        }

        /*
         * If map is set, or two arguments were specified, we go into map mode.
         */
        if (mapOption || optionCount == 0) {
            if (getFreeArguments().length != 2) {
                throw new InvalidFreeArgumentException(
                    Messages.getString("CommandWorkFold.OneServerPathAndOneLocalPathRequired")); //$NON-NLS-1$
            }

            if (ServerPath.isServerPath(getFreeArguments()[0]) == false) {
                throw new InvalidFreeArgumentException(
                    Messages.getString("CommandWorkFold.FirstFreeArgumentMustBeServerPath")); //$NON-NLS-1$
            }

            workspace.addOrChangeMapping(getFreeArguments()[0], LocalPath.canonicalize(getFreeArguments()[1]));

            return;
        }

        /*
         * If Cloak is set, create a cloak mapping.
         */
        if (cloakOption == true) {
            if (getFreeArguments().length != 1) {
                throw new InvalidFreeArgumentException(Messages.getString("CommandWorkFold.CloakRequiresSinglePath")); //$NON-NLS-1$
            }

            String localOrServerPath = getFreeArguments()[0];

            /*
             * Cloaked mappings work on a server path, with a null local path,
             * so we have to map a given local path to a server path.
             */
            if (ServerPath.isServerPath(localOrServerPath) == false) {
                localOrServerPath = LocalPath.canonicalize(localOrServerPath);

                final PathTranslation translation = workspace.translateLocalPathToServerPath(localOrServerPath);

                if (translation == null) {
                    final String messageFormat =
                        Messages.getString("CommandWorkFold.LocalPathHasNoMappedAncestorsFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, localOrServerPath);

                    throw new InvalidFreeArgumentException(message);
                }

                localOrServerPath = translation.getTranslatedPath();
            }

            workspace.createWorkingFolder(new WorkingFolder(localOrServerPath, null, WorkingFolderType.CLOAK), true);

            return;
        }

        if (unmapOption || decloakOption) {
            if (getFreeArguments().length != 1) {
                if (unmapOption) {
                    throw new InvalidFreeArgumentException(
                        Messages.getString("CommandWorkFold.UnmapRequiresSinglePath")); //$NON-NLS-1$
                } else if (decloakOption) {
                    throw new InvalidFreeArgumentException(
                        Messages.getString("CommandWorkFold.DecloakRequiresSinglePath")); //$NON-NLS-1$
                }
            }

            String path = getFreeArguments()[0];

            /*
             * Find the working folder mapping by local or server path,
             * depending on which they supplied.
             */
            WorkingFolder wf = null;
            if (ServerPath.isServerPath(path) == true) {
                wf = workspace.getExactMappingForServerPath(path);
            } else {
                path = LocalPath.canonicalize(path);
                wf = workspace.getExactMappingForLocalPath(path);
            }

            if (wf == null) {
                final String messageFormat =
                    Messages.getString("CommandWorkFold.NoWorkingFolderAssignedInWorkspaceFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, path, workspace.getName());

                throw new CLCException(message);
            }

            /*
             * Make sure we don't match non-cloak mappings if the decloak option
             * wasn't given.
             */
            if (decloakOption && (wf != null && wf.getType() != WorkingFolderType.CLOAK)) {
                final String messageFormat = Messages.getString("CommandWorkFold.MappingIsNotCloakMappingFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, path);

                throw new CLCException(message);
            }

            try {
                workspace.deleteWorkingFolder(wf);
            } catch (final ServerPathFormatException e) {
                throw new CLCException(
                    Messages.getString("CommandWorkFold.ServerPathFormatException") + e.getLocalizedMessage()); //$NON-NLS-1$
            } catch (final IOException e) {
                throw new CLCException(
                    Messages.getString("CommandWorkFold.ErrorUnmappingFolder") + e.getLocalizedMessage()); //$NON-NLS-1$
            }
        }
    }

    /**
     * Print information about the workspace that contains the given local path.
     *
     * @param localPath
     *        the local path to print workspace info for (not null).
     * @param client
     *        a configured and connected client.
     */
    private void handleSingleWorkingFolder(final String localPath, final VersionControlClient client)
        throws CLCException,
            CannotFindWorkspaceException {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        final WorkspaceInfo w = findCachedWorkspaceForPath(localPath);

        if (w == null) {
            final String messageFormat = Messages.getString("CommandWorkFold.NoWorkspaceFoundContainingMappingFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, localPath);

            throw new CLCException(message);
        }

        /*
         * Refresh the workspace from the server.
         */
        Workspace[] ret = null;
        try {
            ret = client.queryWorkspaces(w.getName(), w.getOwnerName(), w.getComputer());
        } catch (final Exception e) {
            final String messageFormat = Messages.getString("CommandWorkFold.ErrorRefreshingCachedWorkspaceFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, w.toString(), e.getLocalizedMessage());

            throw new CLCException(message);
        }

        Check.isTrue(
            ret != null && ret.length == 1 && ret[0] != null,
            "ret != null && ret.length == 1 && ret[0] != null"); //$NON-NLS-1$

        printWorkspace(ret[0]);
    }

    /**
     * Prints information for a single workspace.
     *
     * @param w
     *        the workspace to print information for (not null).
     */
    private void printWorkspace(final Workspace w) {
        Check.notNull(w, "w"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setHeadingsVisible(false);
        table.setColumns(new Column[] {
            new Column("", Sizing.TIGHT), //$NON-NLS-1$
            new Column("", Sizing.EXPAND) //$NON-NLS-1$
        });

        table.addRow(new String[] {
            Messages.getString("CommandWorkFold.Workspace"), //$NON-NLS-1$
            w.getName()
        });
        table.addRow(new String[] {
            Messages.getString("CommandWorkFold.Collection"), //$NON-NLS-1$
            w.getServerName()
        });

        /*
         * Print the normal info.
         */
        BasicPrinter.printSeparator(getDisplay(), '=');
        table.print(getDisplay().getPrintStream());

        /*
         * Print all the working folder mappings.
         */
        final WorkingFolder[] workingFolders = w.getFolders();
        if (workingFolders != null) {
            for (int i = 0; i < workingFolders.length; i++) {
                final WorkingFolder wf = workingFolders[i];
                Check.notNull(wf, "wf"); //$NON-NLS-1$

                if (wf.getType() == WorkingFolderType.CLOAK) {
                    getDisplay().printLine(" (" //$NON-NLS-1$
                        + Messages.getString("CommandWorkFold.WorkingFolderTypeCloakedLiteral") //$NON-NLS-1$
                        + ") " //$NON-NLS-1$
                        + wf.getDisplayServerItem());
                } else {
                    getDisplay().printLine(" " + wf.getDisplayServerItem() + ": " + wf.getLocalItem()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            getDisplay().printLine(""); //$NON-NLS-1$
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[7];

        optionSets[0] = new AcceptedOptionSet(new Class[] {}, "<localFolder>"); //$NON-NLS-1$

        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionWorkspace.class
        }, ""); //$NON-NLS-1$

        optionSets[2] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class,
            OptionWorkspace.class
        }, "<serverFolder>"); //$NON-NLS-1$

        optionSets[3] = new AcceptedOptionSet(new Class[] {
            OptionMap.class,
            OptionCollection.class,
            OptionWorkspace.class
        }, "<serverFolder> <localFolder>"); //$NON-NLS-1$

        // This optionSet includes a required option (unmap).
        optionSets[4] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class,
            OptionWorkspace.class
        }, "<serverFolder>|<localFolder>", new Class[] //$NON-NLS-1$
        {
            OptionUnmap.class
        });

        // This optionSet includes a required option (cloak).
        optionSets[5] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class,
            OptionWorkspace.class
        }, "<serverFolder>|<localFolder>", new Class[] //$NON-NLS-1$
        {
            OptionCloak.class
        });

        // This optionSet includes a required option (decloak).
        optionSets[6] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class,
            OptionWorkspace.class
        }, "<serverFolder>|<localFolder>", new Class[] //$NON-NLS-1$
        {
            OptionDecloak.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandWorkFold.HelpText1") //$NON-NLS-1$
        };
    }

    private void updateFromFile(final Workspace w, final String filename) throws CLCException {
        BufferedReader reader = null;
        final List<WorkingFolder> mappings = new ArrayList<WorkingFolder>();

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String line;
            int linenum = 0;

            while ((line = reader.readLine()) != null) {
                linenum++;
                line = line.trim();

                if (line.length() == 0 || line.startsWith("#")) //$NON-NLS-1$
                {
                    continue;
                }

                if (StringUtil.startsWithIgnoreCase(line, "(cloaked)")) //$NON-NLS-1$
                {
                    line = line.substring(9);

                    final String[] paths = line.split(":", 2); //$NON-NLS-1$

                    if (paths == null || paths.length < 1 || paths.length > 2) {
                        final String messageFormat = Messages.getString("CommandWorkFold.MappingFileCloakErrorFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, linenum);
                        throw new CLCException(message);
                    }

                    final String serverPath = paths[0].trim();
                    final String localPath = paths.length == 2 ? paths[1].trim() : null;

                    /*
                     * Ensure that server path has leading space such that there
                     * was whitespace separating the (cloaked) statement and the
                     * server path. Ensure local path is omitted.
                     */
                    if (serverPath == paths[0]
                        || !ServerPath.isServerPath(serverPath)
                        || !StringUtil.isNullOrEmpty(localPath)) {
                        final String messageFormat = Messages.getString("CommandWorkFold.MappingFileCloakErrorFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, linenum);
                        throw new CLCException(message);
                    }

                    mappings.add(new WorkingFolder(serverPath, null, WorkingFolderType.CLOAK));
                } else {
                    final String[] paths = line.split(":", 2); //$NON-NLS-1$

                    if (paths == null || paths.length != 2) {
                        final String messageFormat = Messages.getString("CommandWorkFold.MappingFileMapErrorFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, linenum);
                        throw new CLCException(message);
                    }

                    final String serverPath = paths[0].trim();
                    final String localPath = paths[1].trim();

                    if (!ServerPath.isServerPath(serverPath)) {
                        final String messageFormat = Messages.getString("CommandWorkFold.MappingFileMapErrorFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, linenum);
                        throw new CLCException(message);
                    }

                    mappings.add(w.getWorkingFolderFromPaths(serverPath, localPath));
                }
            }
        } catch (final IOException e) {
            final String messageFormat = Messages.getString("CommandWorkFold.ErrorOpeningWorkfoldFileFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, filename, e.getLocalizedMessage());
            throw new CLCException(message);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    /* Ignore */
                }
            }
        }

        w.update(null, null, mappings.toArray(new WorkingFolder[mappings.size()]));
    }
}
