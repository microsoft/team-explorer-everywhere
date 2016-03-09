// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.activation.ActivationException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.diff.DiffFolderItem;
import com.microsoft.tfs.client.clc.vc.diff.DiffItem;
import com.microsoft.tfs.client.clc.vc.diff.DiffItemPair;
import com.microsoft.tfs.client.clc.vc.diff.launch.DiffLaunchItem;
import com.microsoft.tfs.client.clc.vc.diff.launch.DiffLauncher;
import com.microsoft.tfs.client.clc.vc.diff.launch.LocalFileDiffLaunchItem;
import com.microsoft.tfs.client.clc.vc.diff.launch.PendingChangeDiffLaunchItem;
import com.microsoft.tfs.client.clc.vc.diff.launch.ShelvedChangeDiffLaunchItem;
import com.microsoft.tfs.client.clc.vc.diff.launch.VersionedFileDiffLaunchItem;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionShelveset;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChangeComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChangeComparatorType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.FileEncodingDetector;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * <p>
 * This is a very complicated command. The run method validates arguments and
 * hands off control to one of the following methods:
 * </p>
 * <ul>
 * <li>{@link #diffTwoArguments(boolean)}</li>
 * <li>{@link #diffShelvesetChanges()}</li>
 * <li>{@link #diffWorkspaceAllChanges()}</li>
 * <li>{@link #diffWorkspaceSpecificChanges(QualifiedItem, VersionSpec)}</li>
 * </ul>
 * <p>
 * These secondary methods may call other more-specific methods to handle
 * subsets of their work, but ultimately {@link #showDiff(DiffItem, DiffItem)}
 * is called for individual items that need to be shown to the user.
 * </p>
 */
public final class CommandDifference extends Command {
    private final static String AMBIGUOUS_VERSION_MESSAGE =
        Messages.getString("CommandDifference.AmbiguousVersionToCompare"); //$NON-NLS-1$

    private final static String DIFF_SEPARATOR = "===================================================================="; //$NON-NLS-1$

    private VersionControlClient client = null;

    /**
     * Does the external process launching for us.
     */
    private final DiffLauncher launcher = new DiffLauncher();

    /**
     * A {@link FilenameFilter} that passes all directories.
     */
    static class DirectoriesOnlyFilter implements FilenameFilter {
        @Override
        public boolean accept(final File dir, final String name) {
            return new File(dir, name).isDirectory();
        }
    }

    /**
     * A {@link FilenameFilter} that passes all files.
     */
    static class FilesOnlyFilter implements FilenameFilter {
        @Override
        public boolean accept(final File dir, final String name) {
            return new File(dir, name).isFile();
        }
    }

    public CommandDifference() {
        super();
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandDifference.HelpText1"), //$NON-NLS-1$
            MessageFormat.format(
                Messages.getString("CommandDifference.HelpText2Format"), //$NON-NLS-1$
                EnvironmentVariables.EXTERNAL_DIFF_COMMAND),
            Messages.getString("CommandDifference.HelpText3"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText4"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText5"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText6"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText7"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText8"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText9"), //$NON-NLS-1$
            Messages.getString("CommandDifference.HelpText10"), //$NON-NLS-1$
            "  (GNU diff, unified)     diff -u --label \"%6 / %7\" \"%1\" \"%2\"", //$NON-NLS-1$
            "  (SourceGear DiffMerge)  diffmerge --title1=\"%6\" --title2=\"%7\" \"%1\" \"%2\"", //$NON-NLS-1$
        };
    }

    /**
     * Creates a {@link TFSTeamProjectCollection} and a
     * {@link VersionControlClient} (which is is set in {@link #client}) and
     * hooks up event listeners.
     *
     * @throws ActivationException
     * @throws MalformedURLException
     * @throws ArgumentException
     * @throws CLCException
     * @throws LicenseException
     */
    private void doConnection() throws MalformedURLException, ArgumentException, CLCException, LicenseException {
        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Save the workspace for other methods to use.
         */
        this.client = client;
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length > 2) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandDifference.TooManyItemsSpecified")); //$NON-NLS-1$
        }

        final boolean recursive = findOptionType(OptionRecursive.class) != null;

        Option o = null;

        if (getFreeArguments().length == 0) {
            if (recursive) {
                throw new InvalidFreeArgumentException(
                    Messages.getString("CommandDifference.RecursiveRequiresAtLeastOneItem")); //$NON-NLS-1$
            }

            if (findOptionType(OptionVersion.class) != null) {
                throw new InvalidOptionException(Messages.getString("CommandDifference.VersionOptionNotAllowed")); //$NON-NLS-1$
            }
        }

        if (findOptionType(OptionShelveset.class) != null) {
            if (findOptionType(OptionWorkspace.class) != null) {
                throw new InvalidOptionException(
                    Messages.getString("CommandDifference.ShelvesetCannotBeUsedWithWorkspace")); //$NON-NLS-1$
            }

            if (findOptionType(OptionVersion.class) != null) {
                throw new InvalidOptionException(
                    Messages.getString("CommandDifference.ShelvesetCannotBeUsedWithVersion")); //$NON-NLS-1$
            }

            diffShelvesetChanges();
            return;
        } else {
            final QualifiedItem[] qualifiedItems = parseQualifiedItems(null, true, 0);
            VersionSpec[] versionsFromOption = new VersionSpec[0];

            if ((o = findOptionType(OptionVersion.class)) != null) {
                versionsFromOption = ((OptionVersion) o).getParsedVersionSpecs();
                Check.notNull(versionsFromOption, "versionsFromOption"); //$NON-NLS-1$
            }

            if (getFreeArguments().length == 0) {
                diffWorkspaceAllChanges(recursive);
                return;
            } else if (qualifiedItems.length == 1
                && versionsFromOption.length < 2
                && (qualifiedItems[0].getVersions() == null || qualifiedItems[0].getVersions().length < 2)) {
                /*
                 * If there's a single version specified on the qualified item,
                 * we use that version, otherwise we use the single version
                 * given via the version option, otherwise null (no range
                 * support).
                 */
                final VersionSpec sourceVersion =
                    (qualifiedItems[0].getVersions() != null && qualifiedItems[0].getVersions().length == 1)
                        ? qualifiedItems[0].getVersions()[0]
                        : ((versionsFromOption.length == 1) ? versionsFromOption[0] : null);

                diffWorkspaceSpecificChanges(qualifiedItems[0], sourceVersion, recursive);
                return;
            } else {
                diffTwoArguments(recursive);
                return;
            }
        }
    }

    /**
     * Compares two items (parsed from the command line by the method) at the
     * versions specified with the items (if they were versioned item specs) or
     * at the version(s) given as option values. The items can be files or
     * folders.
     * <p>
     * If more than two versions are specified using versioned item specs or the
     * version option, an exception is thrown.
     *
     * @param recursive
     *        if the recursive option was set on the command line.
     * @throws CLCException
     * @throws ArgumentException
     * @throws LicenseException
     * @throws MalformedURLException
     */
    private void diffTwoArguments(final boolean recursive)
        throws ArgumentException,
            CLCException,
            LicenseException,
            MalformedURLException {
        /*
         * Parse the first argument ("source") as a versioned file spec.
         */
        final VersionedFileSpec sourceItem =
            VersionedFileSpec.parse(getFreeArguments()[0], VersionControlConstants.AUTHENTICATED_USER, true);

        /*
         * Parse the second item ("target"), if specified. Otherwise set the
         * second item to the same as the first item and we'll verify later
         * there were multiple versions specified to diff.
         */
        VersionedFileSpec targetItem = null;
        if (getFreeArguments().length == 2) {
            targetItem =
                VersionedFileSpec.parse(getFreeArguments()[1], VersionControlConstants.AUTHENTICATED_USER, true);
        } else {
            targetItem = sourceItem;
        }

        /*
         * Parse the version(s) specified from the option.
         */
        VersionSpec[] versionsFromOption = new VersionSpec[0];

        Option o = null;
        if ((o = findOptionType(OptionVersion.class)) != null) {
            versionsFromOption = ((OptionVersion) o).getParsedVersionSpecs();
        }

        /*
         * Ambiguous if two arguments and the version option given (in this case
         * each argument must have its own version in the spec).
         */
        if (getFreeArguments().length > 1 && versionsFromOption.length > 0) {
            throw new InvalidOptionException(AMBIGUOUS_VERSION_MESSAGE);

        }

        /*
         * Ambiguous if two arguments and a version range on either
         */
        if (getFreeArguments().length > 1
            && (sourceItem.getVersions().length > 1 || targetItem.getVersions().length > 1)) {
            throw new InvalidOptionException(AMBIGUOUS_VERSION_MESSAGE);
        }

        /*
         * Ambiguous if the sum of the count of versions in the first item plus
         * the count of versions from the option are greater than 2.
         */
        if (sourceItem.getVersions().length + versionsFromOption.length > 2) {
            throw new InvalidOptionException(AMBIGUOUS_VERSION_MESSAGE);
        }

        /*
         * Assign the versions to compare from the available specifiers. We have
         * already counted all the versions in specifiers and options (and
         * errored if more than two were specified), so we can just use the
         * first two we parse.
         */
        VersionSpec sourceVersion = null;
        VersionSpec targetVersion = null;

        /*
         * If the source item has a version with it, use it for the source, and
         * if it was a range, use the second one for the target, otherwise use
         * the option value for the target version.
         */
        if (sourceItem.getVersions().length > 0) {
            sourceVersion = sourceItem.getVersions()[0];

            if (sourceItem.getVersions().length > 1) {
                targetVersion = sourceItem.getVersions()[1];
            } else if (versionsFromOption.length > 0) {
                targetVersion = versionsFromOption[0];
            }

        } else if (versionsFromOption.length > 0) {
            /*
             * First item had no versions, so use one or two from the option
             * value.
             */
            sourceVersion = versionsFromOption[0];

            if (versionsFromOption.length > 1) {
                targetVersion = versionsFromOption[1];
            }
        }

        /*
         * Ambiguous if the target had a version range.
         */
        if (targetItem.getVersions().length > 1) {
            throw new InvalidOptionException(AMBIGUOUS_VERSION_MESSAGE);
        }

        /*
         * If the target has a value, use that value.
         */
        if (targetItem.getVersions().length == 1) {
            targetVersion = targetItem.getVersions()[0];
        }

        /*
         * The two versions have been determined, now determine which workspaces
         * are appropriate for the two items.
         */

        final WorkspaceSpec sourceWorkspaceSpec = getWorkspaceForServerOrLocalPath(sourceItem.getItem(), sourceVersion);
        final WorkspaceSpec targetWorkspaceSpec = getWorkspaceForServerOrLocalPath(targetItem.getItem(), targetVersion);

        Check.notNull(sourceWorkspaceSpec, "sourceWorkspaceSpec"); //$NON-NLS-1$
        Check.notNull(targetWorkspaceSpec, "targetWorkspaceSpec"); //$NON-NLS-1$

        /*
         * If we don't have any versions yet for the source or target version,
         * and there are 0 or 1 free arguments, then use a workspace version
         * spec from the second item. The second item will be a reference to the
         * first item in this case.
         */
        if (sourceVersion == null && targetVersion == null && getFreeArguments().length <= 1) {
            sourceVersion = new WorkspaceVersionSpec(targetWorkspaceSpec);
        }

        doConnection();

        /*
         * Connected, query the workspaces.
         */
        final Workspace sourceWorkspace = getWorkspace(sourceWorkspaceSpec.getName(), sourceWorkspaceSpec.getOwner());
        final Workspace targetWorkspace = getWorkspace(targetWorkspaceSpec.getName(), targetWorkspaceSpec.getOwner());

        File sourceTempDirectory = null;
        File targetTempDirectory = null;

        try {
            try {
                sourceTempDirectory = TempStorageService.getInstance().createTempDirectory();
                targetTempDirectory = TempStorageService.getInstance().createTempDirectory();
            } catch (final IOException e) {
                throw new CLCException(e);
            }

            /*
             * These items will be at the root of the diff item graph for the
             * source and target sides of the diff.
             */
            final DiffItem sourceRootItem =
                generateRootDiffItem(sourceItem.getItem(), sourceVersion, sourceTempDirectory, sourceWorkspace);

            final DiffItem targetRootItem =
                generateRootDiffItem(targetItem.getItem(), targetVersion, targetTempDirectory, targetWorkspace);

            final boolean sourceIsDirectory = sourceRootItem.getItemType() == ItemType.FOLDER;
            final boolean targetIsDirectory = targetRootItem.getItemType() == ItemType.FOLDER;

            List<DiffItem> sourceList = null;
            List<DiffItem> targetList = null;

            if (sourceIsDirectory != targetIsDirectory) {
                throw new InvalidFreeArgumentException(Messages.getString("CommandDifference.CannotDiffFolderAndFile")); //$NON-NLS-1$
            }

            if (recursive && sourceIsDirectory == false) {
                throw new InvalidFreeArgumentException(
                    Messages.getString("CommandDifference.RecursiveCannotBeUsedWithFiles")); //$NON-NLS-1$
            }

            if (sourceIsDirectory) {
                sourceList = generateChildDiffItems(
                    sourceRootItem,
                    sourceWorkspace,
                    recursive,
                    sourceVersion,
                    sourceTempDirectory);
            }

            if (targetIsDirectory) {
                targetList = generateChildDiffItems(
                    targetRootItem,
                    targetWorkspace,
                    recursive,
                    targetVersion,
                    targetTempDirectory);
            }

            if (sourceIsDirectory == false) {
                showDiff(sourceRootItem, targetRootItem);
                return;
            } else {
                /*
                 * Pair up the directories from the first to the second list, if
                 * the item is in both lists.
                 */
                final List<DiffItemPair> directoryPairs = new ArrayList<DiffItemPair>(sourceList.size());
                for (int i = 0; i < sourceList.size(); i++) {
                    final int indexInSecond = targetList.indexOf(sourceList.get(i));
                    if (indexInSecond >= 0) {
                        directoryPairs.add(new DiffItemPair(sourceList.get(i), targetList.get(indexInSecond)));
                    }
                }

                /*
                 * Update the pairs with all the files on disk.
                 */
                generateLocalFileDiffItems(directoryPairs, sourceVersion, targetVersion);

                diffFolder(recursive, sourceRootItem, targetRootItem, directoryPairs);
                return;
            }
        } finally {
            if (sourceTempDirectory != null) {
                TempStorageService.getInstance().cleanUpItem(sourceTempDirectory);
            }

            if (targetTempDirectory != null) {
                TempStorageService.getInstance().cleanUpItem(targetTempDirectory);
            }
        }
    }

    private WorkspaceSpec getWorkspaceForServerOrLocalPath(final String serverOrLocalPath, final VersionSpec version)
        throws CannotFindWorkspaceException,
            InvalidOptionValueException,
            InvalidOptionException {
        Check.notNull(serverOrLocalPath, "serverOrLocalPath"); //$NON-NLS-1$

        String workspaceName;
        String workspaceOwner;

        if (version instanceof WorkspaceVersionSpec && ((WorkspaceVersionSpec) version).getName() != null) {
            workspaceName = ((WorkspaceVersionSpec) version).getName();
            workspaceOwner = ((WorkspaceVersionSpec) version).getOwner();
        } else {
            /*
             * If it's a local path and mapped, just use that cached workspace.
             */
            if (ServerPath.isServerPath(serverOrLocalPath) == false
                && Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).isMapped(serverOrLocalPath)) {
                final WorkspaceInfo cw =
                    Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).getLocalWorkspaceInfo(serverOrLocalPath);
                workspaceName = cw.getName();
                workspaceOwner = cw.getOwnerName();
            } else {
                /*
                 * If we still don't have a workspace, use the default one for
                 * our arguments, working directory, etc.
                 */
                final WorkspaceInfo cw = determineCachedWorkspace(getFreeArguments());

                if (cw == null) {
                    final String messageFormat =
                        Messages.getString("CommandDifference.CouldNotDetermineWorkspaceFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, serverOrLocalPath);

                    throw new CannotFindWorkspaceException(message);
                }

                workspaceName = cw.getName();
                workspaceOwner = cw.getOwnerName();
            }
        }

        return new WorkspaceSpec(workspaceName, workspaceOwner);
    }

    private void diffFolder(
        final boolean recursive,
        final DiffItem sourceRootItem,
        final DiffItem targetRootItem,
        final List<DiffItemPair> directoryPairs) {
        /*
         * An easy performance optimization. If there's just a pair which have
         * no child files or directories, then there's nothing to do.
         */
        if (directoryPairs.size() == 1) {
            final DiffItemPair pair = directoryPairs.get(0);
            final DiffFolderItem sourceItem = (DiffFolderItem) pair.getSourceItem();
            final DiffFolderItem targetItem = (DiffFolderItem) pair.getTargetItem();

            if (sourceItem.getFiles().size() == 0
                && sourceItem.getDirectories().size() == 0
                && targetItem.getFiles().size() == 0
                && targetItem.getDirectories().size() == 0) {
                return;
            }
        }

        final boolean multipleDirectories = directoryPairs.size() > 1;
        boolean firstItem = true;

        for (final Iterator<DiffItemPair> iterator = directoryPairs.iterator(); iterator.hasNext();) {
            final DiffItemPair pair = iterator.next();
            final DiffFolderItem sourceItem = (DiffFolderItem) pair.getSourceItem();
            final DiffFolderItem targetItem = (DiffFolderItem) pair.getTargetItem();

            if (multipleDirectories) {
                if (firstItem == false) {
                    getDisplay().printLine(""); //$NON-NLS-1$
                } else {
                    firstItem = false;
                }

                getDisplay().printLine(
                    MessageFormat.format(
                        Messages.getString("CommandDifference.DiffFolderPaddedFormat"), //$NON-NLS-1$
                        getFullyQualifiedName(pair.getSourceItem())));
                getDisplay().printLine(
                    MessageFormat.format(
                        Messages.getString("CommandDifference.AgainstFolderPaddedFormat"), //$NON-NLS-1$
                        getFullyQualifiedName(pair.getTargetItem())));
                getDisplay().printLine(DIFF_SEPARATOR);
            }

            /*
             * Test if each source item's child directories are common to the
             * target, print if they are not.
             */
            for (final Iterator directoryIterator =
                sourceItem.getDirectories().iterator(); directoryIterator.hasNext();) {
                final DiffFolderItem sourceSubDirectory = (DiffFolderItem) directoryIterator.next();

                final int indexOfSubDirectoryInTarget = targetItem.getDirectories().indexOf(sourceSubDirectory);
                if (indexOfSubDirectoryInTarget >= 0) {
                    targetItem.getDirectories().remove(indexOfSubDirectoryInTarget);
                } else {
                    final String messageFormat = Messages.getString("CommandDifference.FolderFoundOnlyUnderFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        LocalPath.getFileName(sourceSubDirectory.getTempFile()),
                        getFullyQualifiedName(sourceItem));

                    getDisplay().printLine(message);
                }
            }

            /*
             * Do the same for the target side.
             */
            for (final Iterator directoryIterator =
                targetItem.getDirectories().iterator(); directoryIterator.hasNext();) {
                final DiffFolderItem targetSubDirectory = (DiffFolderItem) directoryIterator.next();

                final String messageFormat = Messages.getString("CommandDifference.FolderFoundOnlyUnderFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    LocalPath.getFileName(targetSubDirectory.getTempFile()),
                    getFullyQualifiedName(targetItem));

                getDisplay().printLine(message);
            }

            /*
             * Collect all the files to diff in this directory from the source
             * side, print the files that only exist in the source side.
             */
            final List<DiffItemPair> filesToDiff = new ArrayList<DiffItemPair>();

            for (final Iterator fileIterator = sourceItem.getFiles().iterator(); fileIterator.hasNext();) {
                final DiffItem sourceFile = (DiffItem) fileIterator.next();

                /*
                 * Test if each source item's child files are common to the
                 * target, print if they are not.
                 */
                final int indexOfFileInTarget = targetItem.getFiles().indexOf(sourceFile);
                if (indexOfFileInTarget >= 0) {
                    final DiffItemPair filePair =
                        new DiffItemPair(sourceFile, (DiffItem) targetItem.getFiles().get(indexOfFileInTarget));

                    // Diff this one later.
                    filesToDiff.add(filePair);

                    targetItem.getFiles().remove(indexOfFileInTarget);
                } else {
                    final String messageFormat = Messages.getString("CommandDifference.FileFoundOnlyUnderFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        LocalPath.getFileName(sourceFile.getTempFile()),
                        getFullyQualifiedName(sourceItem));

                    getDisplay().printLine(message);
                }
            }

            /*
             * Print the files that exist only in the target side.
             */
            for (final Iterator fileIterator = targetItem.getFiles().iterator(); fileIterator.hasNext();) {
                final DiffItem targetFile = (DiffItem) fileIterator.next();

                final String messageFormat = Messages.getString("CommandDifference.FileFoundOnlyUnderFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    LocalPath.getFileName(targetFile.getTempFile()),
                    getFullyQualifiedName(targetItem));

                getDisplay().printLine(message);
            }

            /*
             * Diff all the files.
             */
            boolean doneAtLeastOne = false;
            for (final Iterator<DiffItemPair> filePairIterator = filesToDiff.iterator(); filePairIterator.hasNext();) {
                final DiffItemPair filePair = filePairIterator.next();

                /*
                 * Show the file diff. Show any errors but continue.
                 */
                try {
                    showDiff(filePair.getSourceItem(), filePair.getTargetItem());
                    doneAtLeastOne = true;
                } catch (final Throwable t) {
                    setExitCode(ExitCode.FAILURE);

                    final String messageFormat = Messages.getString("CommandDifference.ErrorDiffingFileWithFileFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        filePair.getSourceItem().getTempFile(),
                        filePair.getTargetItem().getTempFile());

                    log.warn(message, t);

                    getDisplay().printErrorLine(t.getMessage());
                }
            }

            if (getExitCode() == ExitCode.FAILURE && doneAtLeastOne) {
                setExitCode(ExitCode.PARTIAL_SUCCESS);
            }
        }
    }

    private void showDiff(final DiffItem sourceDiffItem, final DiffItem targetDiffItem) throws CLCException {
        DiffLaunchItem sourceLaunchItem = null;
        DiffLaunchItem targetLaunchItem = null;

        try {
            /*
             * Prepare the source launch item.
             */
            if (sourceDiffItem.getVersion() != null) {
                Item sourceItem = sourceDiffItem.getItem();
                if (sourceItem == null) {
                    sourceItem = client.getItem(
                        sourceDiffItem.getServerPath(),
                        sourceDiffItem.getVersion(),
                        DeletedState.NON_DELETED,
                        true);
                }

                sourceLaunchItem = new VersionedFileDiffLaunchItem(client, sourceItem, sourceDiffItem.getVersion());
                sourceLaunchItem.setLabel(makeLabel(sourceDiffItem));
            } else {

                sourceLaunchItem = new LocalFileDiffLaunchItem(
                    sourceDiffItem.getLocalPath(),
                    sourceDiffItem.getCodePage(),
                    sourceDiffItem.getLastModified(),
                    false);
            }

            /*
             * Prepare the target launch item.
             */
            if (targetDiffItem.getVersion() != null) {
                Item targetItem = targetDiffItem.getItem();
                if (targetItem == null) {
                    targetItem = client.getItem(
                        targetDiffItem.getServerPath(),
                        targetDiffItem.getVersion(),
                        DeletedState.NON_DELETED,
                        true);
                }

                targetLaunchItem = new VersionedFileDiffLaunchItem(client, targetItem, targetDiffItem.getVersion());
                targetLaunchItem.setLabel(makeLabel(targetDiffItem));
            } else {
                try {
                    targetLaunchItem = new LocalFileDiffLaunchItem(
                        targetDiffItem.getLocalPath(),
                        targetDiffItem.getCodePage(),
                        targetDiffItem.getLastModified(),
                        false);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (final IOException e) {
            final String messageFormat = Messages.getString("CommandDifference.ErrorPreparingLaunchItemForDiffFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.toString());

            log.error(message, e);
            throw new CLCException(message);
        }

        /*
         * VS's implementation computes a "header" string here, but we skip it
         * because most cross-platform diff tools won't take one.
         */

        launcher.launchDiff(sourceLaunchItem, targetLaunchItem);
    }

    private String makeLabel(final DiffItem diffItem) {
        Check.notNull(diffItem, "diffItem"); //$NON-NLS-1$
        Check.notNull(diffItem.getTempFile(), "diffItem.getTempFile()"); //$NON-NLS-1$

        if (diffItem.getLocalPath() != null && diffItem.getTempFile().equals(new File(diffItem.getLocalPath()))) {
            final String messageFormat = Messages.getString("CommandDifference.LocalLabelFormat"); //$NON-NLS-1$
            return MessageFormat.format(
                messageFormat,
                diffItem.getRelativeLocalPath(),
                DiffLaunchItem.SHORT_DATE_TIME_FORMATTER.format(new Date(diffItem.getLastModified())));
        }

        final String messageFormat = Messages.getString("CommandDifference.ServerLabelFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            diffItem.getRelativeLocalPath(),
            diffItem.getChangesetVersion(),
            DiffLaunchItem.SHORT_DATE_TIME_FORMATTER.format(new Date(diffItem.getLastModified())));
    }

    private String getFullyQualifiedName(final DiffItem diffItem) {
        Check.notNull(diffItem, "diffItem"); //$NON-NLS-1$

        if (diffItem.getVersion() == null) {
            if (diffItem.getLocalPath() != null) {
                return diffItem.getLocalPath();
            }

            return diffItem.getServerPath();
        }

        if (diffItem.getServerPath() == null) {
            return VersionedFileSpec.formatForPath(diffItem.getLocalPath(), diffItem.getVersion());
        }

        return VersionedFileSpec.formatForPath(diffItem.getServerPath(), diffItem.getVersion());
    }

    /**
     * Updates the source and target {@link DiffFolderItem}s in each given
     * directory pair with the child files they have on disk, if a corresponding
     * version is given for the side of the target.
     *
     * @param directoryPairs
     *        the pairs to update.
     * @param sourceVersion
     *        the version to use for the source side of the directory pairs. If
     *        null, the source side is not updated.
     * @param targetVersion
     *        the version to use for the target side of the directory pairs. If
     *        null, the target side is not updated.
     */
    private void generateLocalFileDiffItems(
        final List<DiffItemPair> directoryPairs,
        final VersionSpec sourceVersion,
        final VersionSpec targetVersion) {
        Check.notNull(directoryPairs, "directoryPairs"); //$NON-NLS-1$

        if (sourceVersion != null || targetVersion != null) {
            for (int i = 0; i < directoryPairs.size(); i++) {
                final DiffItemPair pair = directoryPairs.get(i);

                final DiffFolderItem sourceItem = (DiffFolderItem) pair.getSourceItem();
                final DiffFolderItem targetItem = (DiffFolderItem) pair.getTargetItem();

                /*
                 * If the source version is null, this is a workspace diff so
                 * look for files that only exist in the source on disk.
                 */
                if (sourceVersion == null && sourceItem != null) {
                    if (sourceItem.getTempFile() != null && new File(sourceItem.getTempFile()).exists()) {
                        final File sourceItemTempFile = new File(sourceItem.getTempFile());

                        final String files[] = sourceItemTempFile.list(new FilesOnlyFilter());
                        makePathsAbsolute(files, sourceItemTempFile);

                        Arrays.sort(files, LocalPath.TOP_DOWN_COMPARATOR);

                        for (int j = 0; j < files.length; j++) {
                            final String fullPath = files[j];
                            final FileEncoding encoding =
                                FileEncodingDetector.detectEncoding(fullPath, FileEncoding.AUTOMATICALLY_DETECT);

                            final DiffItem diffItem = new DiffItem(
                                null,
                                fullPath,
                                fullPath,
                                encoding.getCodePage(),
                                sourceItem.getRootItem(),
                                ItemType.FILE,
                                new File(fullPath).lastModified(),
                                false,
                                sourceVersion);

                            sourceItem.addFile(diffItem);
                        }
                    }
                }

                /*
                 * If the target version is null, this is a workspace diff so
                 * look for files that only exist in the target on disk.
                 */
                if (targetVersion == null && targetItem != null) {
                    if (targetItem.getTempFile() != null && new File(targetItem.getTempFile()).exists()) {
                        final File targetItemTempFile = new File(targetItem.getTempFile());

                        final String files[] = targetItemTempFile.list(new FilesOnlyFilter());
                        makePathsAbsolute(files, targetItemTempFile);

                        Arrays.sort(files, LocalPath.TOP_DOWN_COMPARATOR);

                        for (int j = 0; j < files.length; j++) {
                            final String fullPath = files[j];
                            final FileEncoding encoding =
                                FileEncodingDetector.detectEncoding(fullPath, FileEncoding.AUTOMATICALLY_DETECT);

                            final DiffItem diffItem = new DiffItem(
                                null,
                                fullPath,
                                fullPath,
                                encoding.getCodePage(),
                                targetItem.getRootItem(),
                                ItemType.FILE,
                                new File(fullPath).lastModified(),
                                false,
                                targetVersion);

                            targetItem.addFile(diffItem);
                        }
                    }
                }
            }
        }
    }

    /**
     * Takes a {@link DiffItem} and returns a list of qualifying child diff
     * items (which includes the original diff item), optionally recursing
     * fully. The server is queried for some paths, and the local disk is
     * examined for items not in the repository.
     *
     * @param rootItem
     *        the item to examine for child changes (not null).
     * @param workspace
     *        a workspace that can be used for server queries and working folder
     *        mapping queries (not null).
     * @param recursive
     *        if true, both server and filesystem directories are fully
     *        recursed.
     * @param version
     *        the version of the root item to generate items for. May be null.
     * @param tempDirectory
     *        a temporary directory to be used for new {@link DiffItem}s (not
     *        null).
     * @return a list of {@link DiffFolderItem}s.
     */
    private List<DiffItem> generateChildDiffItems(
        final DiffItem rootItem,
        final Workspace workspace,
        final boolean recursive,
        final VersionSpec version,
        final File tempDirectory) {
        Check.notNull(rootItem, "rootItem"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(tempDirectory, "tempDirectory"); //$NON-NLS-1$

        final List<DiffItem> directoryList = new ArrayList<DiffItem>();
        final boolean isWorkspaceVersionSpec = (version instanceof WorkspaceVersionSpec);

        /*
         * Query all the items from the server that match the given root item
         * (at the given version spec). These items come back sorted so related
         * items are contiguous for faster searching.
         */
        Item[] itemArray = null;

        if (version != null) {
            itemArray = getFilteredServerItemList(rootItem.getServerPath(), version, false, recursive, false, true);
        } else if (rootItem.isInRepository()) {
            itemArray = getFilteredServerItemList(
                rootItem.getServerPath(),
                new WorkspaceVersionSpec(workspace),
                false,
                recursive,
                false,
                true);
        }

        /*
         * Start with the root item.
         */
        directoryList.add(rootItem);

        /*
         * If there are server items, iterate through them, and for each item
         * keep walking in an inner loop to find items in the same directory.
         * Then continue iterating after passing the related items.
         */
        if (itemArray != null && itemArray.length > 0) {
            /*
             * Query the pending changes for edit changes and keep a map so we
             * can look them up during iteration.
             */

            final Map<String, PendingChange> serverPathToExistingAPendingChanges = new HashMap<String, PendingChange>();

            if (isWorkspaceVersionSpec) {
                final PendingSet pendingChangeSet = workspace.getPendingChanges(new String[] {
                    rootItem.getServerPath()
                }, recursive ? RecursionType.FULL : RecursionType.NONE, false);

                if (pendingChangeSet != null
                    && pendingChangeSet.getPendingChanges() != null
                    && pendingChangeSet.getPendingChanges().length > 0) {
                    for (int i = 0; i < pendingChangeSet.getPendingChanges().length; i++) {
                        final PendingChange change = pendingChangeSet.getPendingChanges()[i];

                        if (change != null && change.getChangeType().contains(ChangeType.EDIT)) {
                            serverPathToExistingAPendingChanges.put(change.getServerItem(), change);
                        }
                    }
                }
            }

            /*
             * Build the list of directory items (each item can have children).
             *
             * Iterate over the items from the starting index, and iterate over
             * the remainder in a second loop inside to collapse contiguous
             * items into one diff item (the items list is sorted).
             *
             * The variables in the outer loop start with "first" and the
             * variables in the inner start with "second".
             */

            String localPath = null;
            String serverPath = null;
            String path = null;

            int index = 1;
            while (index < itemArray.length) {
                final Item firstItem = itemArray[index];

                final String firstFolderName = ServerPath.getParent(firstItem.getServerItem());
                final DiffFolderItem firstDiffFolderItem =
                    new DiffFolderItem(firstFolderName, null, null, rootItem, false, version);

                /*
                 * If no version was given, see if the directory really exists
                 * on disk.
                 */
                boolean firstLocalFolderExists = true;
                if (version == null) {
                    final PathTranslation firstTranslation = workspace.translateServerPathToLocalPath(firstFolderName);

                    if (firstTranslation != null && firstTranslation.isCloaked() == false) {
                        firstLocalFolderExists = new File(firstTranslation.getTranslatedPath()).exists();
                    } else {
                        firstLocalFolderExists = false;
                    }
                }

                /*
                 * If the server path is the root folder "$/", chop the length
                 * to just "$".
                 */
                int firstPathOffset = firstFolderName.length();
                if (firstPathOffset == 2) {
                    firstPathOffset = 1;
                }

                /*
                 * Save the starting index and loop over the remaining items
                 * until we match one in a different directory.
                 */
                final int startingIndex = index;
                while (index < itemArray.length) {
                    final Item secondItem = itemArray[index];

                    /*
                     * Assign the second item's server path to the outer loop's
                     * serverPath variable.
                     */
                    serverPath = secondItem.getServerItem();

                    /*
                     * If the second item is not a child of the first item, or
                     * if it has a path separator after our first path's offset
                     * (which mean it's a whole directory underneath the first
                     * item), then we stop iterating because it can't be a
                     * direct child and needs its own diff item later.
                     */
                    if (ServerPath.isChild(firstFolderName, serverPath) == false
                        || serverPath.indexOf(ServerPath.PREFERRED_SEPARATOR_CHARACTER, firstPathOffset + 1) >= 0) {
                        break;
                    }

                    /*
                     * If the item is a folder and we were given a version to
                     * fetch, or it exists locally, process its children.
                     */
                    if ((version != null || firstLocalFolderExists) && secondItem.getItemType() == ItemType.FOLDER) {
                        /*
                         * See if it's mapped. It may not be mapped
                         */
                        final PathTranslation secondTranslation = workspace.translateServerPathToLocalPath(serverPath);

                        if (secondTranslation != null && secondTranslation.isCloaked() == false) {
                            /*
                             * Assign to the outer loop's local path.
                             */
                            localPath = secondTranslation.getTranslatedPath();
                        }

                        /*
                         * If there was a version given, use a temp file for it,
                         * otherwise use the actual local file.
                         */
                        if (version != null) {
                            path = constructTempFile(tempDirectory, serverPath);
                        } else {
                            path = localPath;
                        }

                        Check.notNull(path, "path"); //$NON-NLS-1$

                        try {
                            if (version != null || new File(path).exists()) {
                                final DiffFolderItem secondDiffFolderItem =
                                    new DiffFolderItem(serverPath, localPath, path, rootItem, true, version);

                                /*
                                 * If the first item has already been added to
                                 * the list, add the second item to its
                                 * children.
                                 */
                                final int indexOfFirstItem = directoryList.indexOf(firstDiffFolderItem);
                                if (indexOfFirstItem >= 0) {
                                    final DiffFolderItem parent = (DiffFolderItem) directoryList.get(indexOfFirstItem);
                                    parent.addDirectory(secondDiffFolderItem);
                                }

                                /*
                                 * Add it to the end of the list so it will be
                                 * processed on further iteration in the outer
                                 * loop.
                                 */
                                if (recursive) {
                                    directoryList.add(secondDiffFolderItem);
                                }
                            }
                        } catch (final Throwable t) {
                            log.error("Inner exception evaluating diff items", t); //$NON-NLS-1$
                        }
                    }

                    /*
                     * Move on to the next list item in the inner loop.
                     */
                    index++;
                }

                /*
                 * The inner loop has stopped moving because it found a
                 * different directory, so process all the files between the
                 * startingIndex to the current index.
                 */

                if (version != null || firstLocalFolderExists) {
                    for (int i = startingIndex; i < index; i++) {
                        if (itemArray[i].getItemType() != ItemType.FOLDER) {
                            serverPath = itemArray[i].getServerItem();

                            final PathTranslation thirdTranslation =
                                workspace.translateServerPathToLocalPath(serverPath);

                            if (thirdTranslation != null && thirdTranslation.isCloaked() == false) {
                                localPath = thirdTranslation.getTranslatedPath();
                            }

                            if (version != null) {
                                path = constructTempFile(tempDirectory, serverPath);
                            } else {
                                path = localPath;
                            }

                            final DiffItem fileDiffItem =
                                new DiffItem(itemArray[i], localPath, path, rootItem, version);

                            if (serverPathToExistingAPendingChanges.containsKey((itemArray[i].getServerItem()))) {
                                fileDiffItem.setIsPendingChange(true);
                            }

                            if (isWorkspaceVersionSpec && localPath != null) {
                                final FileSystemAttributes localFileAttributes =
                                    FileSystemUtils.getInstance().getAttributes(localPath);

                                if (localFileAttributes.exists() && localFileAttributes.isReadOnly() == false) {
                                    fileDiffItem.setWritable(true);
                                }
                            }

                            if (version != null || new File(path).exists()) {
                                /*
                                 * Find the parent to put this file into.
                                 */
                                final int indexOfFirstItem = directoryList.indexOf(firstDiffFolderItem);
                                if (indexOfFirstItem >= 0) {
                                    ((DiffFolderItem) directoryList.get(indexOfFirstItem)).addFile(fileDiffItem);
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
         * Walk down from the root item to find any filesystem items that don't
         * have a corresponding server item.
         */
        if (version == null) {
            String[] onDiskSubdirectories = null;

            /*
             * If the root item is on disk, and exists, list the subdirectories
             * in it.
             */
            if (rootItem.getLocalPath() != null) {
                final File rootFile = new File(rootItem.getLocalPath());

                if (rootFile.isDirectory() && rootFile.exists()) {
                    /*
                     * List all the subdirectories.
                     */
                    onDiskSubdirectories = rootFile.list(new DirectoriesOnlyFilter());

                    makePathsAbsolute(onDiskSubdirectories, rootFile);

                    /*
                     * Sort them.
                     */
                    Arrays.sort(onDiskSubdirectories, LocalPath.TOP_DOWN_COMPARATOR);
                }
            }

            if (onDiskSubdirectories != null) {
                for (int i = 0; i < onDiskSubdirectories.length; i++) {
                    final String subDirectoryName = onDiskSubdirectories[i];

                    final DiffFolderItem subDirectoryDiffFolderItem =
                        new DiffFolderItem(null, subDirectoryName, subDirectoryName, rootItem, false, version);

                    ((DiffFolderItem) rootItem).addDirectory(subDirectoryDiffFolderItem);
                }
            }

            if (recursive) {
                final List<DiffFolderItem> subdirectoryList = new ArrayList<DiffFolderItem>();

                /*
                 * Iterate over all the directories we've added so far and
                 * expand them.
                 */
                for (int i = 0; i < directoryList.size(); i++) {
                    getSubDirectoryDiffItems((DiffFolderItem) directoryList.get(i), directoryList, subdirectoryList);
                }

                directoryList.addAll(subdirectoryList);
            }
        }

        return directoryList;
    }

    private String constructTempFile(final File tempDirectory, final String serverPathFile) {
        Check.notNull(tempDirectory, "tempDirectory"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverPathFile, "serverPath"); //$NON-NLS-1$

        return new File(tempDirectory, ServerPath.getFileName(serverPathFile)).getAbsolutePath();
    }

    /**
     * Queries the server for items under the given server path, returning only
     * the items whose changeset IDs are > 0.
     *
     * @param serverPath
     *        the server path to query (not null).
     * @param version
     *        the verion of the server path to query (not null).
     * @param includeDeleted
     *        if true, deleted items are included, otherwise they are not.
     * @param recursive
     *        if true, fully recurse into subdirectories.
     * @param foldersOnly
     *        if true, only folders are returned. If false, all item types are
     *        returned.
     * @param includeDownloadInfo
     *        if true, download information is returned.
     * @return the items that matched.
     */
    private Item[] getFilteredServerItemList(
        final String serverPath,
        final VersionSpec version,
        final boolean includeDeleted,
        final boolean recursive,
        final boolean foldersOnly,
        final boolean includeDownloadInfo) {
        Check.notNull(client, "this.client"); //$NON-NLS-1$

        final Item[] items = client.getItems(
            serverPath,
            version,
            recursive ? RecursionType.FULL : RecursionType.NONE,
            includeDeleted ? DeletedState.ANY : DeletedState.NON_DELETED,
            foldersOnly ? ItemType.FOLDER : ItemType.ANY,
            includeDownloadInfo).getItems();

        final ArrayList<Item> ret = new ArrayList<Item>();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getChangeSetID() > 0) {
                ret.add(items[i]);
            }
        }

        return ret.toArray(new Item[ret.size()]);
    }

    /**
     * Looks at the given item's local folder, lists its subdirectories, and
     * adds the subdirectories to the item's directories, and to the given
     * subdirectoryList only if they are not already in the given directoryList.
     * Recurses into each subdirectory.
     *
     * @param item
     *        the item to examine for subdirectories (not null).
     * @param exclusionDiffFolderItemList
     *        if a found subdirectory has a diff item in this list, it is not
     *        added to the childDiffFolderItems list, but is still added as the
     *        item's child via
     *        {@link DiffFolderItem#addDirectory(DiffFolderItem)}. Not null.
     * @param childDiffFolderItems
     *        a list to collect the found folder items that were not excluded
     *        (not null).
     */
    private void getSubDirectoryDiffItems(
        final DiffFolderItem item,
        final List<DiffItem> exclusionDiffFolderItemList,
        final List<DiffFolderItem> childDiffFolderItems) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(exclusionDiffFolderItemList, "directoryList"); //$NON-NLS-1$
        Check.notNull(childDiffFolderItems, "childDiffFolderItems"); //$NON-NLS-1$

        if (item.getLocalPath() != null) {
            final File file = new File(item.getLocalPath());

            String[] subdirectories = null;

            if (file.isDirectory() && file.exists()) {
                subdirectories = file.list(new DirectoriesOnlyFilter());
                makePathsAbsolute(subdirectories, file);
                Arrays.sort(subdirectories, LocalPath.TOP_DOWN_COMPARATOR);
            }

            if (subdirectories != null) {
                for (int i = 0; i < subdirectories.length; i++) {
                    final DiffFolderItem subItem =
                        new DiffFolderItem(null, subdirectories[i], subdirectories[i], item.getRootItem(), false, null);

                    item.addDirectory(subItem);

                    if (exclusionDiffFolderItemList.contains(subItem) == false) {
                        childDiffFolderItems.add(subItem);
                        getSubDirectoryDiffItems(subItem, exclusionDiffFolderItemList, childDiffFolderItems);
                    }
                }
            }
        }
    }

    private Workspace getWorkspace(final String workspaceName, final String workspaceOwner)
        throws CannotFindWorkspaceException {
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNull(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        final Workspace ret = client.queryWorkspace(workspaceName, workspaceOwner);

        if (ret == null) {
            final String messageFormat = Messages.getString("CommandDifference.WorkspaceDoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, workspaceName, workspaceOwner);

            throw new CannotFindWorkspaceException(message);
        }

        return ret;
    }

    private void diffWorkspaceSpecificChanges(
        final QualifiedItem qualifiedItem,
        final VersionSpec sourceVersion,
        final boolean recursive) throws MalformedURLException, ArgumentException, CLCException, LicenseException {
        doConnection();

        final WorkspaceInfo cw = determineCachedWorkspace(getFreeArguments());
        final Workspace workspace = realizeCachedWorkspace(cw, client);

        final PendingSet set = workspace.getPendingChanges(new String[] {
            qualifiedItem.getPath()
        }, recursive ? RecursionType.FULL : RecursionType.NONE, true);

        if (set != null && set.getPendingChanges() != null && set.getPendingChanges().length > 0) {
            diffPendingChanges(set.getPendingChanges(), sourceVersion, recursive);
            return;
        }

        final String messageFormat = Messages.getString("CommandDifference.ThereIsNoPendingChangeFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getFreeArguments()[0]);

        getDisplay().printLine(message);
        setExitCode(ExitCode.FAILURE);
    }

    private void diffWorkspaceAllChanges(final boolean recursive)
        throws MalformedURLException,
            ArgumentException,
            CLCException,
            LicenseException {
        doConnection();

        final WorkspaceInfo cw = determineCachedWorkspace(getFreeArguments());
        final Workspace workspace = realizeCachedWorkspace(cw, client);

        /*
         * Recursion type is always true for the query, because we want to know
         * all the pending changes, even if we don't diff them recursively.
         */
        final PendingSet set = workspace.getPendingChanges(new String[] {
            ServerPath.ROOT
        }, RecursionType.FULL, true);

        if (set != null && set.getPendingChanges() != null && set.getPendingChanges().length > 0) {
            diffPendingChanges(set.getPendingChanges(), null, recursive);
            return;
        }

        getDisplay().printLine(Messages.getString("CommandDifference.ThereAreNoPendingChanges")); //$NON-NLS-1$
    }

    private void diffPendingChanges(
        final PendingChange[] pendingChanges,
        final VersionSpec sourceVersion,
        final boolean recursive) {
        Arrays.sort(pendingChanges, new PendingChangeComparator(PendingChangeComparatorType.SERVER_ITEM));
        final boolean doneAtLeastOne = false;

        for (int i = 0; i < pendingChanges.length; i++) {
            final PendingChange change = pendingChanges[i];

            getDisplay().printLine(MessageFormat.format(
                Messages.getString("CommandDifference.AColonBFormat"), //$NON-NLS-1$
                change.getChangeType().toUIString(false, change),
                change.getLocalItem()));

            /*
             * Show contents for edits or deletes. Continue on failure.
             */
            if (change.getChangeType().contains(ChangeType.EDIT)
                || change.getChangeType().contains(ChangeType.DELETE)) {
                try {
                    /*
                     * Setup the source item to be nothing (for adds), a pending
                     * change item, or the base version.
                     */

                    DiffLaunchItem sourceLaunchItem;

                    if (change.getChangeType().contains(ChangeType.ADD)) {
                        sourceLaunchItem = new LocalFileDiffLaunchItem(
                            null,
                            change.getEncoding(),
                            change.getCreationDate().getTimeInMillis(),
                            true);
                        sourceLaunchItem.setLabel(Messages.getString("CommandDifference.NoSourceFile")); //$NON-NLS-1$
                    } else if (sourceVersion == null) {
                        sourceLaunchItem = new PendingChangeDiffLaunchItem(change, client);
                    } else {
                        final Item baseItem =
                            client.getItem(change.getServerItem(), sourceVersion, DeletedState.NON_DELETED, true);

                        sourceLaunchItem = new VersionedFileDiffLaunchItem(client, baseItem, sourceVersion);

                        final String relativePath =
                            LocalPath.makeRelative(change.getLocalItem(), LocalPath.getCurrentWorkingDirectory());

                        final String messageFormat = Messages.getString("CommandDifference.ServerLabelFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            relativePath,
                            sourceVersion.toString(),
                            DiffLaunchItem.SHORT_DATE_TIME_FORMATTER.format(baseItem.getCheckinDate().getTime()));

                        sourceLaunchItem.setLabel(message);
                    }

                    /*
                     * Setup the target item, which just points to the local
                     * item.
                     */

                    final DiffLaunchItem targetLaunchItem = new LocalFileDiffLaunchItem(
                        change.getLocalItem(),
                        change.getEncoding(),
                        change.getCreationDate().getTimeInMillis(),
                        false);

                    if (change.getLocalItem() != null && new File(change.getLocalItem()).exists() == false) {
                        targetLaunchItem.setLabel(Messages.getString("CommandDifference.NoTargetFile")); //$NON-NLS-1$
                    }

                    launcher.launchDiff(sourceLaunchItem, targetLaunchItem);
                } catch (final ExternalToolException e) {
                    /*
                     * Rethrow this, because it's fatal.
                     */
                    throw e;
                } catch (final Throwable t) {
                    /*
                     * Don't rethrow, because we have more changes to process.
                     */

                    setExitCode(ExitCode.FAILURE);

                    final String messageFormat =
                        Messages.getString("CommandDifference.ErrorDiffingPendingChangeFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, change.getLocalItem(), t.toString());

                    log.warn(message, t);

                    getDisplay().printErrorLine(message);
                }
            }
        }

        if (doneAtLeastOne && getExitCode() == ExitCode.FAILURE) {
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }
    }

    private void diffShelvesetChanges()
        throws MalformedURLException,
            ArgumentException,
            CLCException,
            LicenseException {
        final OptionShelveset optionShelveset = (OptionShelveset) findOptionType(OptionShelveset.class);
        Check.notNull(optionShelveset, "optionshelveSet"); //$NON-NLS-1$

        /*
         * Determine which shelveset to use.
         */
        final WorkspaceSpec shelvesetSpec =
            WorkspaceSpec.parse(optionShelveset.getValue(), VersionControlConstants.AUTHENTICATED_USER);

        if (shelvesetSpec.getName() == null
            || shelvesetSpec.getName().length() == 0
            || shelvesetSpec.getName().equals("*")) //$NON-NLS-1$
        {
            final String messageFormat = Messages.getString("CommandDifference.InvalidShelvesetSpecificationFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, optionShelveset.getUserText());

            throw new InvalidOptionValueException(message);
        }

        /*
         * Prepare the item specs from the arguments.
         */
        ItemSpec[] itemSpecs = null;
        if (getFreeArguments().length == 0) {
            /*
             * Use full recursion if the user gave no arguments.
             */
            itemSpecs = new ItemSpec[] {
                new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
            };
        } else {
            final RecursionType recursionType =
                (findOptionType(OptionRecursive.class) != null) ? RecursionType.FULL : RecursionType.ONE_LEVEL;

            /*
             * Parse all the arguments as qualified items but warn if the user
             * specified any versions (we're always getting the shelved
             * version).
             */
            final QualifiedItem[] qualifiedItems = parseQualifiedItems(null, false, 0);
            itemSpecs = new ItemSpec[qualifiedItems.length];

            for (int i = 0; i < qualifiedItems.length; i++) {
                if (qualifiedItems[i].getVersions() != null) {
                    final String messageFormat =
                        Messages.getString("CommandDifference.WarningIgnoringVersionSpecFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, qualifiedItems[i].getPath());

                    getDisplay().printErrorLine(message);
                }

                itemSpecs[i] = qualifiedItems[i].toItemSpec(recursionType);
            }
        }

        doConnection();

        final WorkspaceInfo cw = determineCachedWorkspace(getFreeArguments());
        final Workspace workspace = realizeCachedWorkspace(cw, client);

        final PendingChange[] uniqueSortedChanges = getUniqueSortedChanges(
            workspace.queryShelvedChanges(shelvesetSpec.getName(), shelvesetSpec.getOwner(), itemSpecs, true));

        if (uniqueSortedChanges.length == 0) {
            final StringBuffer sb = new StringBuffer();
            for (int i = 0; i < itemSpecs.length; i++) {
                if (i > 0) {
                    sb.append(" "); //$NON-NLS-1$
                }
                sb.append(itemSpecs[i].getItem());
            }

            final String messageFormat =
                Messages.getString("CommandDifference.ShelvesetHasNoPendingChangesMatchingPatternFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, optionShelveset.getUserText(), sb.toString());

            throw new InvalidFreeArgumentException(message);
        }

        final boolean doneAtLeastOne = false;

        for (int i = 0; i < uniqueSortedChanges.length; i++) {
            final PendingChange pendingChange = uniqueSortedChanges[i];
            Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$

            String displayServerItem;

            /*
             * If a version was specified use it for the display string,
             * otherwise just the server path.
             */
            if (pendingChange.getVersion() > 0) {
                displayServerItem = VersionedFileSpec.formatForPath(
                    pendingChange.getServerItem(),
                    new ChangesetVersionSpec(pendingChange.getVersion()));
            } else {
                displayServerItem = pendingChange.getServerItem();
            }

            getDisplay().printLine(MessageFormat.format(
                Messages.getString("CommandDifference.AColonBFormat"), //$NON-NLS-1$
                pendingChange.getChangeType().toUIString(true, pendingChange),
                displayServerItem));

            try {
                final DiffLaunchItem sourceLaunchItem = new PendingChangeDiffLaunchItem(pendingChange, client);

                if (pendingChange.getChangeType().contains(ChangeType.ADD)) {
                    sourceLaunchItem.setLabel(Messages.getString("CommandDifference.NoSourceFile")); //$NON-NLS-1$
                }

                final DiffLaunchItem targetLaunchItem =
                    new ShelvedChangeDiffLaunchItem(shelvesetSpec.getName(), pendingChange, client);

                if (pendingChange.getChangeType().contains(ChangeType.DELETE)) {
                    targetLaunchItem.setLabel(Messages.getString("CommandDifference.NoTargetFile")); //$NON-NLS-1$
                }

                launcher.launchDiff(sourceLaunchItem, targetLaunchItem);
            } catch (final ExternalToolException e) {
                /*
                 * Rethrow this, because it's fatal.
                 */
                throw e;
            } catch (final Throwable t) {
                /*
                 * Don't rethrow, because we have more changes to process.
                 */

                setExitCode(ExitCode.FAILURE);

                final String messageFormat = Messages.getString("CommandDifference.ErrorDiffingShelvedChangeFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, pendingChange.getServerItem(), t.toString());

                log.warn(message, t);
                getDisplay().printErrorLine(message);
            }
        }

        if (doneAtLeastOne && getExitCode() == ExitCode.FAILURE) {
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }
    }

    private PendingChange[] getUniqueSortedChanges(final PendingSet[] pendingSets) {
        final Map<String, PendingChange> changeIDToChangeMap = new HashMap<String, PendingChange>();

        /*
         * Map all changes across all sets by pending change server path.
         */
        for (int i = 0; i < pendingSets.length; i++) {
            final PendingSet set = pendingSets[i];
            Check.notNull(set, "set"); //$NON-NLS-1$

            final PendingChange[] pendingChanges = set.getPendingChanges();

            for (int j = 0; j < pendingChanges.length; j++) {
                final PendingChange change = pendingChanges[j];

                if (changeIDToChangeMap.containsKey(change.getServerItem()) == false) {
                    changeIDToChangeMap.put(change.getServerItem(), change);
                }
            }
        }

        /*
         * Sort them.
         */
        final PendingChange[] changeArray =
            changeIDToChangeMap.values().toArray(new PendingChange[changeIDToChangeMap.values().size()]);

        Arrays.sort(changeArray, new PendingChangeComparator(PendingChangeComparatorType.SERVER_ITEM));

        return changeArray;
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[3];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class,
            OptionRecursive.class
        }, "<itemSpec>"); //$NON-NLS-1$

        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class
        }, "<itemSpec> <itemSpec2>"); //$NON-NLS-1$

        optionSets[2] = new AcceptedOptionSet(new Class[] {
            OptionShelveset.class,
            OptionRecursive.class
        }, "<shelvesetItemSpec>"); //$NON-NLS-1$

        return optionSets;
    }

    private DiffItem generateRootDiffItem(
        final String item,
        VersionSpec version,
        final File tempDirectory,
        final Workspace workspace) throws CLCException {
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(tempDirectory, "tempDirectory"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        boolean isWorkspaceVersion = version instanceof WorkspaceVersionSpec;

        String serverPath = null;
        String localPath = null;

        /*
         * Attempt to match the given item to a working folder mapping. If this
         * fails we leave the localPath or serverPath elements null and deal
         * with those later.
         */
        if (ServerPath.isServerPath(item)) {
            serverPath = ServerPath.canonicalize(item);

            final PathTranslation translation = workspace.translateServerPathToLocalPath(serverPath);
            if (translation != null) {
                localPath = translation.getTranslatedPath();
            }
        } else {
            localPath = LocalPath.canonicalize(item);

            final PathTranslation translation = workspace.translateLocalPathToServerPath(localPath);
            if (translation != null) {
                serverPath = translation.getTranslatedPath();
            }
        }

        /*
         * Use the latest version if an unmapped server path was given.
         */
        if (version == null && localPath == null) {
            version = LatestVersionSpec.INSTANCE;
        }

        /*
         * If version is null, this is for a local item.
         */
        if (version == null) {
            ItemType itemType;
            boolean inRepository = false;
            boolean isFolder = false;
            long lastModified = 0;

            if (localPath == null) {
                final String messageFormat = Messages.getString("CommandDifference.NoWorkingFolderMappingFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, item);

                throw new CLCException(message);
            }

            final File localFile = new File(localPath);
            final FileSystemAttributes localFileAttributes = FileSystemUtils.getInstance().getAttributes(localPath);

            if (localFile.isDirectory() && localFile.exists()) {
                itemType = ItemType.FOLDER;
                isFolder = true;
            } else if (localFile.exists()) {
                itemType = ItemType.FILE;
                lastModified = localFile.lastModified();
            } else {
                final String messageFormat = Messages.getString("CommandDifference.FileOrFolderDoesNotExistFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, localPath);

                throw new CLCException(message);
            }

            /*
             * File or folder exists, cross-check the data with the server
             * information.
             */
            final VersionSpec checkVersionSpec = new WorkspaceVersionSpec(workspace);

            isWorkspaceVersion = true;

            Item[] items = null;

            if (serverPath != null) {
                items = getFilteredServerItemList(serverPath, checkVersionSpec, false, false, false, true);
            }

            int codePage = 0;

            if (items != null && items.length > 0 && ServerPath.equals(items[0].getServerItem(), serverPath)) {
                if (isFolder == false && items[0].getItemType() != ItemType.FOLDER) {
                    itemType = ItemType.FILE;
                    codePage = items[0].getEncoding().getCodePage();
                }

                inRepository = true;
            }

            /*
             * Folders can be returned with no further investigation.
             */
            if (itemType == ItemType.FOLDER) {
                return new DiffFolderItem(serverPath, localPath, localPath, null, inRepository, version);
            } else {
                /*
                 * Determine if the file is a pending change in the current
                 * workspace.
                 */
                boolean isPendingChange = false;
                if (inRepository && isWorkspaceVersion) {
                    final PendingSet pendingSet = workspace.getPendingChanges(new String[] {
                        serverPath
                    }, RecursionType.NONE, false);

                    if (pendingSet != null
                        && pendingSet.getPendingChanges() != null
                        && pendingSet.getPendingChanges().length > 0) {
                        Check.isTrue(
                            pendingSet.getPendingChanges().length == 1,
                            "pendingSet.getPendingChanges().length == 1"); //$NON-NLS-1$

                        if (pendingSet.getPendingChanges()[0].getChangeType().contains(ChangeType.EDIT)) {
                            isPendingChange = true;

                            /*
                             * Use this code page instead of the item we queried
                             * from the server, beacuse this one might include a
                             * pending encoding change.
                             */
                            codePage = pendingSet.getPendingChanges()[0].getEncoding();
                        }
                    }
                }

                if (codePage == 0) {
                    codePage =
                        FileEncodingDetector.detectEncoding(localPath, FileEncoding.AUTOMATICALLY_DETECT).getCodePage();
                }

                final DiffItem ret = new DiffItem(
                    serverPath,
                    localPath,
                    localPath,
                    codePage,
                    null,
                    itemType,
                    lastModified,
                    inRepository,
                    version);

                ret.setWritable(
                    inRepository
                        && isWorkspaceVersion
                        && localFile.exists()
                        && localFileAttributes.isReadOnly() == false);

                ret.setIsPendingChange(isPendingChange);

                return ret;
            }
        } else if (serverPath == null) {
            /*
             * Could not fully qualify the server path with the working folder
             * mappings.
             */
            final String messageFormat = Messages.getString("CommandDifference.FileOrFolderDoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, item);

            throw new CLCException(message);
        } else {
            /*
             * Fully qualified server path, ensure it exists.
             */
            final Item[] items = getFilteredServerItemList(serverPath, version, false, false, false, true);

            if (items == null || items.length < 1 || ServerPath.equals(items[0].getServerItem(), serverPath) == false) {
                if (version != null && version instanceof WorkspaceVersionSpec) {
                    final WorkspaceVersionSpec workspaceSpec = (WorkspaceVersionSpec) version;

                    if (Workspace.matchName(workspaceSpec.getName(), workspace.getName())
                        && workspace.ownerNameMatches(workspaceSpec.getOwner())) {
                        final String messageFormat =
                            Messages.getString("CommandDifference.ItemNotFoundInCurrentWorkspaceFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, serverPath);

                        throw new CLCException(message);
                    }
                } else {
                    final String messageFormat =
                        Messages.getString("CommandDifference.ItemNotFoundInTheRepositoryFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, serverPath);

                    throw new CLCException(message);
                }
            }

            /*
             * Create an intermediate path entry in the temp folder this method
             * was given so a diff tool doesn't sense a top-level rename. This
             * works for files and folders.
             */
            final String newTempPath = new File(tempDirectory, ServerPath.getFileName(serverPath)).getAbsolutePath();

            if (items[0].getItemType() == ItemType.FOLDER) {
                return new DiffFolderItem(serverPath, localPath, newTempPath, null, true, version);
            } else {
                int codePage = items[0].getEncoding().getCodePage();
                boolean isPendingChange = false;

                if (isWorkspaceVersion) {
                    final PendingSet pendingSet = workspace.getPendingChanges(new String[] {
                        serverPath
                    }, RecursionType.NONE, false);

                    if (pendingSet != null
                        && pendingSet.getPendingChanges() != null
                        && pendingSet.getPendingChanges().length > 0) {
                        Check.isTrue(
                            pendingSet.getPendingChanges().length == 1,
                            "pendingSet.getPendingChanges().length == 1"); //$NON-NLS-1$

                        if (pendingSet.getPendingChanges()[0].getChangeType().contains(ChangeType.EDIT)) {
                            isPendingChange = true;
                            /*
                             * Use this code page instead of the item we queried
                             * from the server, beacuse this one might include a
                             * pending encoding change.
                             */
                            codePage = pendingSet.getPendingChanges()[0].getEncoding();
                        }
                    }
                }

                final DiffItem ret = new DiffItem(items[0], localPath, newTempPath, null, version);
                ret.setCodePage(codePage);

                if (isWorkspaceVersion && localPath != null) {
                    final FileSystemAttributes localFileAttributes =
                        FileSystemUtils.getInstance().getAttributes(localPath);

                    if (localFileAttributes.exists() && localFileAttributes.isReadOnly() == false) {
                        ret.setWritable(true);
                    }
                } else {
                    ret.setWritable(false);
                }

                ret.setIsPendingChange(isPendingChange);

                return ret;
            }
        }
    }

    /**
     * Takes an array of short path names ("folder", "a.txt") and a parent
     * {@link File} (the paths are usually existing directories or files inside
     * that parent), and changes the elements in the given array to be full,
     * absolute paths. Each item is made relative to the given parent using the
     * {@link File#File(File, String)} routine..
     *
     * @param shortPaths
     *        the short paths to make relative to the parent (not null, and
     *        elements should not be null)
     * @param parent
     *        the parent file (not null)
     */
    private void makePathsAbsolute(final String[] shortPaths, final File parent) {
        Check.notNull(shortPaths, "shortPaths"); //$NON-NLS-1$
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        for (int i = 0; i < shortPaths.length; i++) {
            Check.notNull(shortPaths[i], "shortPaths[i]"); //$NON-NLS-1$
            shortPaths[i] = new File(parent, shortPaths[i]).getAbsolutePath();
        }
    }
}
