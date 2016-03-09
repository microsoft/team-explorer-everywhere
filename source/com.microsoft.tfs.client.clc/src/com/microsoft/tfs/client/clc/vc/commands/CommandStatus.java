// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.options.shared.OptionUser;
import com.microsoft.tfs.client.clc.vc.options.OptionNoDetect;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionShelveset;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.client.clc.vc.printers.BasicPrinter;
import com.microsoft.tfs.client.clc.xml.CommonXMLNames;
import com.microsoft.tfs.client.clc.xml.SimpleXMLWriter;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChangeComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChangeComparatorType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSetComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSetType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.TFSUser;
import com.microsoft.tfs.core.util.TFSUsernameParseException;
import com.microsoft.tfs.util.Check;

public final class CommandStatus extends Command {
    private final static String STATUS_ELEMENT_NAME = "status"; //$NON-NLS-1$
    private final static String PENDING_CHANGES_ELEMENT_NAME = "pending-changes"; //$NON-NLS-1$
    private final static String CANDIDATE_PENDING_CHANGES_ELEMENT_NAME = "candidate-pending-changes"; //$NON-NLS-1$
    private final static String EMPTY = ""; //$NON-NLS-1$
    private final static String CDATA = "CDATA"; //$NON-NLS-1$

    public CommandStatus() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run()
        throws ArgumentException,

            MalformedURLException,
            CLCException,
            LicenseException {
        /*
         * Workspace and shelveset specs can have owner (user) parts, but the
         * user option always overrides these.
         */
        String workspaceName = null;
        String workspaceOwner = null;
        String shelvesetSpec = null;
        String format = OptionFormat.BRIEF;
        boolean recursive = false;
        boolean isWorkspaceExplicit = false;
        boolean isUserExplicit = false;
        boolean noDetect = false;

        try {
            final WorkspaceInfo cachedWorkspace = determineCachedWorkspace(getFreeArguments());
            workspaceName = cachedWorkspace.getName();
            workspaceOwner = cachedWorkspace.getOwnerName();
        } catch (final CannotFindWorkspaceException e) {
            // Ignore.
        }

        Option o = null;

        if ((o = findOptionType(OptionWorkspace.class)) != null) {
            try {
                final WorkspaceSpec spec = WorkspaceSpec.parse(
                    ((OptionWorkspace) o).getValue(),
                    VersionControlConstants.AUTHENTICATED_USER,
                    true);

                workspaceName = spec.getName();
                workspaceOwner = spec.getOwner();
            } catch (final WorkspaceSpecParseException e) {
                throw new InvalidOptionValueException(e.getMessage());
            }
            isWorkspaceExplicit = true;
        }

        if ((o = findOptionType(OptionUser.class)) != null) {
            workspaceOwner = ((OptionUser) o).getValue();
            isUserExplicit = true;
        }

        if ((o = findOptionType(OptionShelveset.class)) != null) {
            shelvesetSpec = ((OptionShelveset) o).getValue();
        }

        if ((o = findOptionType(OptionFormat.class)) != null) {
            format = ((OptionFormat) o).getValue();
        }

        if ((o = findOptionType(OptionRecursive.class)) != null) {
            recursive = true;
        }

        if ((o = findOptionType(OptionNoDetect.class)) != null) {
            noDetect = true;
        }

        /*
         * Use the workspace option value to try to find the server to connect
         * to, but don't throw if it can't be found, because the workspace may
         * be a remote one (used only to scope our status query below).
         */
        final TFSTeamProjectCollection connection = createConnection(true, false);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * If the user supplied no free arguments, we'll display the status for
         * all files in the appropriate workspace and force recursion on.
         */
        String[] items = null;
        if (getFreeArguments().length == 0) {
            items = new String[] {
                ServerPath.ROOT
            };
            recursive = true;
        } else {
            /*
             * Canonicalize any local paths in the free arguments.
             */
            items = getFreeArguments();

            for (int i = 0; i < items.length; i++) {
                if (ServerPath.isServerPath(items[i]) == false) {
                    items[i] = LocalPath.canonicalize(items[i]);
                }
            }
        }

        /*
         * An owner username was given.
         */
        if (isUserExplicit) {
            if (workspaceOwner == null) {
                throw new InvalidOptionException(Messages.getString("CommandStatus.UserWasExplicitButNoUserRead")); //$NON-NLS-1$
            }

            // Handle the user wildcard.
            if (workspaceOwner != null && workspaceOwner.equalsIgnoreCase("*")) //$NON-NLS-1$
            {
                workspaceOwner = null;
            }

            if (isWorkspaceExplicit == false) {
                workspaceName = null;
            } else if (workspaceOwner == null) {
                /*
                 * We can't have a wildcard user and a non-wildcard workspace
                 * (doesn't make sense, because a workspace only has one user).
                 */
                throw new InvalidOptionException(
                    Messages.getString("CommandStatus.UserOptionCannotTakeWildcardWhenWorkspaceSupplied")); //$NON-NLS-1$
            }
        } else if (workspaceOwner == null) {
            workspaceOwner = VersionControlConstants.AUTHENTICATED_USER;
        }

        if (workspaceName != null && workspaceName.equals("*")) //$NON-NLS-1$
        {
            workspaceName = null;
        }

        /*
         * Get the sets.
         */
        PendingSet[] pendingSets = null;
        if (shelvesetSpec != null) {
            /*
             * The workspace and shelveset options can't both be specified.
             */
            if (isWorkspaceExplicit) {
                throw new InvalidOptionException(
                    Messages.getString("CommandStatus.CannotUseBothShelvesetAndWorkspace")); //$NON-NLS-1$
            }

            // WorkspaceSpec can parse shelveset specs
            WorkspaceSpec spec;
            try {
                spec = WorkspaceSpec.parse(shelvesetSpec, VersionControlConstants.AUTHENTICATED_USER, true);
            } catch (final WorkspaceSpecParseException e) {
                throw new InvalidOptionValueException(e.getMessage());
            }

            // An empty or null name or * means wildcard.
            String queryShelvesetName = spec.getName();
            if (queryShelvesetName == null
                || queryShelvesetName.length() == 0
                || queryShelvesetName.equalsIgnoreCase("*")) //$NON-NLS-1$
            {
                queryShelvesetName = null;
            }

            final String queryShelvesetOwner = spec.getOwner();

            /*
             * localWorkspaceName and localWorkspaceOwner will be null if the
             * paths are all server paths, otherwise they will be some detected
             * local workspace.
             */
            pendingSets = client.queryShelvedChanges(
                queryShelvesetName,
                queryShelvesetOwner,
                ItemSpec.fromStrings(items, (recursive) ? RecursionType.FULL : RecursionType.NONE),
                false);
        } else {
            // Verify there are mappings for local paths
            throwIfContainsUnmappedLocalPath(getFreeArguments());

            final ItemSpec[] specs = ItemSpec.fromStrings(items, recursive ? RecursionType.FULL : RecursionType.NONE);
            pendingSets = client.queryPendingSets(specs, false, workspaceName, workspaceOwner, !noDetect);
        }

        if (OptionFormat.XML.equalsIgnoreCase(format)) {
            try {
                printXML(pendingSets, client);
            } catch (final TransformerConfigurationException e) {
                throw new CLCException(e);
            } catch (final SAXException e) {
                throw new CLCException(e);
            }
        } else {
            if (pendingSets == null || pendingSets.length == 0) {
                getDisplay().printLine(Messages.getString("CommandStatus.ThereAreNoMatchingPendingChanges")); //$NON-NLS-1$
            } else {
                if (OptionFormat.DETAILED.equalsIgnoreCase(format)) {
                    printDetailed(pendingSets, client);
                } else if (OptionFormat.BRIEF.equalsIgnoreCase(format)) {
                    printBrief(pendingSets, client, getDisplay(), isUserExplicit);
                }
            }
        }
    }

    private void printXML(final PendingSet[] pendingSets, final VersionControlClient client)
        throws TransformerConfigurationException,
            SAXException {
        Check.notNull(client, "client"); //$NON-NLS-1$

        final SimpleXMLWriter xmlWriter = new SimpleXMLWriter(getDisplay());

        xmlWriter.startDocument();
        xmlWriter.startElement(EMPTY, EMPTY, STATUS_ELEMENT_NAME, new AttributesImpl());

        if (pendingSets != null && pendingSets.length > 0) {
            // Sort the sets based on the specialized comparator.
            Arrays.sort(pendingSets, new PendingSetComparator());

            xmlWriter.startElement(EMPTY, EMPTY, PENDING_CHANGES_ELEMENT_NAME, new AttributesImpl());

            // Generate the PendingChanges section.
            for (final PendingSet set : pendingSets) {
                if (set != null) {
                    final PendingChange[] changes = set.getPendingChanges();
                    if (changes != null) {
                        addPendingChangeElements(xmlWriter, set, changes);
                    }
                }
            }

            xmlWriter.endElement(EMPTY, EMPTY, PENDING_CHANGES_ELEMENT_NAME);
            xmlWriter.startElement(EMPTY, EMPTY, CANDIDATE_PENDING_CHANGES_ELEMENT_NAME, new AttributesImpl());

            // Generate the CandidatePendingChanges section.
            for (final PendingSet set : pendingSets) {
                if (set != null) {
                    final PendingChange[] changes = set.getCandidatePendingChanges();
                    if (changes != null) {
                        addPendingChangeElements(xmlWriter, set, changes);
                    }
                }
            }

            xmlWriter.endElement(EMPTY, EMPTY, CANDIDATE_PENDING_CHANGES_ELEMENT_NAME);
        }

        xmlWriter.endElement(EMPTY, EMPTY, STATUS_ELEMENT_NAME);
        xmlWriter.endDocument();
    }

    private void addPendingChangeElements(
        final SimpleXMLWriter xmlWriter,
        final PendingSet set,
        final PendingChange[] changes) throws SAXException {
        final boolean shelveSet = set.getType() == PendingSetType.SHELVESET;

        // Sort the changes based on the specialized comparator.
        Arrays.sort(changes, new PendingChangeComparator(PendingChangeComparatorType.SERVER_ITEM));

        // Loop over the changes in this pending set.
        for (int j = 0; j < changes.length; j++) {
            final PendingChange change = changes[j];
            if (change == null) {
                continue;
            }

            String owner = null;
            try {
                owner = new TFSUser(set.getOwnerName()).toString();
            } catch (final TFSUsernameParseException e) {
                owner = set.getOwnerName();
            }

            final AttributesImpl changeAttributes = new AttributesImpl();

            changeAttributes.addAttribute(EMPTY, EMPTY, CommonXMLNames.SERVER_ITEM, CDATA, change.getServerItem());
            changeAttributes.addAttribute(
                EMPTY,
                EMPTY,
                CommonXMLNames.VERSION,
                CDATA,
                Integer.toString(change.getVersion()));
            changeAttributes.addAttribute(EMPTY, EMPTY, CommonXMLNames.OWNER, CDATA, owner);
            changeAttributes.addAttribute(
                EMPTY,
                EMPTY,
                CommonXMLNames.DATE,
                CDATA,
                SimpleXMLWriter.ISO_DATE_FORMAT.format(change.getCreationDate().getTime()));
            changeAttributes.addAttribute(EMPTY, EMPTY, CommonXMLNames.LOCK, CDATA, change.getLockLevelName());
            changeAttributes.addAttribute(
                EMPTY,
                EMPTY,
                CommonXMLNames.CHANGE_TYPE,
                CDATA,
                change.getChangeType().toUIString(false, change));

            changeAttributes.addAttribute(
                EMPTY,
                EMPTY,
                ((shelveSet) ? CommonXMLNames.SHELVESET : CommonXMLNames.WORKSPACE),
                CDATA,
                (set.getName() != null) ? set.getName() : "<unknown>"); //$NON-NLS-1$

            /*
             * If the source server item and server item paths differ, we have a
             * rename, move, etc., and want to print the source item.
             */
            if (change.getSourceServerItem() != null
                && ServerPath.equals(change.getSourceServerItem(), change.getServerItem()) == false) {
                changeAttributes.addAttribute(
                    EMPTY,
                    EMPTY,
                    CommonXMLNames.SOURCE_ITEM,
                    CDATA,
                    change.getSourceServerItem());
            }

            if (shelveSet == false) {
                if (set.getComputer() != null) {
                    changeAttributes.addAttribute(EMPTY, EMPTY, CommonXMLNames.COMPUTER, CDATA, set.getComputer());
                }

                if (change.getLocalItem() != null) {
                    changeAttributes.addAttribute(
                        EMPTY,
                        EMPTY,
                        CommonXMLNames.LOCAL_ITEM,
                        CDATA,
                        change.getLocalItem());
                }
            }

            if (change.getItemType() == ItemType.FILE
                && change.getEncoding() != VersionControlConstants.ENCODING_UNCHANGED) {
                changeAttributes.addAttribute(
                    EMPTY,
                    EMPTY,
                    CommonXMLNames.FILE_TYPE,
                    CDATA,
                    new FileEncoding(change.getEncoding()).getName());
            }

            if (change.getDeletionID() != 0) {
                changeAttributes.addAttribute(
                    EMPTY,
                    EMPTY,
                    CommonXMLNames.DELETION_ID,
                    CDATA,
                    Integer.toString(change.getDeletionID()));
            }

            xmlWriter.startElement(EMPTY, EMPTY, CommonXMLNames.PENDING_CHANGE, changeAttributes);
            xmlWriter.endElement(EMPTY, EMPTY, CommonXMLNames.PENDING_CHANGE);
        }
    }

    private int printDetailed(final PendingSet[] pendingSets, final VersionControlClient client) {
        int changes = 0;
        int candidates = 0;

        if (hasPendingChange(pendingSets)) {
            changes = printDetailedSection(pendingSets, client, false);
        }

        if (hasCandidatePendingChange(pendingSets)) {
            displayDectedCandidatesHeader(getDisplay());
            candidates = printDetailedSection(pendingSets, client, true);
        }

        displayCounts(getDisplay(), changes, candidates);
        return changes + candidates;
    }

    private int printDetailedSection(
        final PendingSet[] pendingSets,
        final VersionControlClient client,
        final boolean showCandidateSection) {
        if (pendingSets.length == 0) {
            return 0;
        }

        Check.notNull(pendingSets[0], "pendingSets[0]"); //$NON-NLS-1$

        final boolean shelveSet = pendingSets[0].getType() == PendingSetType.SHELVESET;

        // Sort the sets based on the specialized comparator.
        Arrays.sort(pendingSets, new PendingSetComparator());

        int totalChanges = 0;

        /*
         * This table can be re-used for each item with the same column
         * settings.
         */
        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setHeadingsVisible(false);
        table.setColumns(new Column[] {
            new Column(EMPTY, Sizing.TIGHT),
            new Column(EMPTY, Sizing.EXPAND)
        });

        // Loop over all the pending sets.
        for (int i = 0; i < pendingSets.length; i++) {
            final PendingSet set = pendingSets[i];
            if (set == null) {
                continue;
            }

            final PendingChange[] changes =
                showCandidateSection ? set.getCandidatePendingChanges() : set.getPendingChanges();
            if (changes == null) {
                continue;
            }

            // Sort the changes based on the specialized comparator.
            Arrays.sort(changes, new PendingChangeComparator(PendingChangeComparatorType.SERVER_ITEM));

            // Loop over the changes in this pending set.
            for (int j = 0; j < changes.length; j++) {
                final PendingChange change = changes[j];
                if (change == null) {
                    continue;
                }

                // Print a blank line to separate changes.
                if (totalChanges > 0) {
                    getDisplay().printLine(EMPTY);
                }

                totalChanges++;

                /*
                 * Figure out which path and version to use. For a branch
                 * change, we use the source version.
                 */
                String itemPath = null;
                if (change.getVersion() > 0 && change.getChangeType().contains(ChangeType.BRANCH) == false) {
                    // AVersionedFileSpec builds a pretty string for us.
                    itemPath = VersionedFileSpec.formatForPath(
                        change.getServerItem(),
                        new ChangesetVersionSpec(change.getVersion()));
                } else {
                    // Branch case, so just use item.
                    itemPath = change.getServerItem();
                }

                final String owner = set.getOwnerDisplayName();

                // TODO : Handle deletion IDs in this string too.
                getDisplay().printLine(itemPath);
                table.clearRows();
                table.addRow(new String[] {
                    Messages.getString("CommandStatus.IndentedUser"), //$NON-NLS-1$
                    owner
                });
                table.addRow(new String[] {
                    Messages.getString("CommandStatus.IndentedDate"), //$NON-NLS-1$
                    SimpleDateFormat.getDateTimeInstance().format(change.getCreationDate().getTime())
                });
                table.addRow(new String[] {
                    Messages.getString("CommandStatus.IndentedLock"), //$NON-NLS-1$
                    change.getLockLevelName()
                });
                table.addRow(new String[] {
                    Messages.getString("CommandStatus.IndentedChange"), //$NON-NLS-1$
                    change.getChangeType().toUIString(false, change)
                });
                table.addRow(new String[] {
                    ((shelveSet) ? Messages.getString("CommandStatus.IndentedShelveset") //$NON-NLS-1$
                        : Messages.getString("CommandStatus.IndentedWorkspace")), //$NON-NLS-1$
                    (set.getName() != null) ? set.getName()
                        : "<" + Messages.getString("CommandStatus.ShelvesetNameUnknownLiteral") + ">" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                });

                /*
                 * If the source server item and server item paths differ, we
                 * have a rename, move, etc., and want to print the source item.
                 */
                if (change.getSourceServerItem() != null
                    && ServerPath.equals(change.getSourceServerItem(), change.getServerItem()) == false) {
                    table.addRow(new String[] {
                        Messages.getString("CommandStatus.IndentedSourceItem"), //$NON-NLS-1$
                        change.getSourceServerItem()
                    });
                }

                if (shelveSet == false) {
                    // Print local item like: [COMPUTER] /path/to/change
                    final String computer = (set.getComputer() != null) ? "[" + set.getComputer() + "]" //$NON-NLS-1$ //$NON-NLS-2$
                        : "[<" //$NON-NLS-1$
                            + Messages.getString("CommandStatus.ComputerNameUnknownLiteral") //$NON-NLS-1$
                            + ">]"; //$NON-NLS-1$
                    final String localItem = (change.getLocalItem() != null) ? change.getLocalItem()
                        : "(" //$NON-NLS-1$
                            + Messages.getString("CommandStatus.LocalPathUnavailableLiteral") //$NON-NLS-1$
                            + ")"; //$NON-NLS-1$

                    table.addRow(new String[] {
                        Messages.getString("CommandStatus.IndentedLocalItem"), //$NON-NLS-1$
                        computer + " " + localItem //$NON-NLS-1$
                    });
                }

                if (change.getItemType() == ItemType.FILE
                    && change.getEncoding() != VersionControlConstants.ENCODING_UNCHANGED) {
                    table.addRow(new String[] {
                        Messages.getString("CommandStatus.IndentedFileType"), //$NON-NLS-1$
                        new FileEncoding(change.getEncoding()).getName()
                    });
                }

                table.print(getDisplay().getPrintStream());
            }
        }

        return totalChanges;
    }

    static int printBrief(
        final List<PendingSet> pendingSets,
        final VersionControlClient client,
        final Display display,
        final boolean showUserColumn) throws ArgumentException {
        return printBrief(pendingSets.toArray(new PendingSet[pendingSets.size()]), client, display, showUserColumn);
    }

    static int printBrief(
        final PendingSet[] pendingSets,
        final VersionControlClient client,
        final Display display,
        final boolean showUserColumn) throws ArgumentException {
        int changes = 0;
        int candidates = 0;

        if (hasPendingChange(pendingSets)) {
            changes = printBriefSection(pendingSets, client, display, showUserColumn, false);
        }

        if (hasCandidatePendingChange(pendingSets)) {
            displayDectedCandidatesHeader(display);
            candidates = printBriefSection(pendingSets, client, display, showUserColumn, true);
        }

        displayCounts(display, changes, candidates);
        return changes + candidates;
    }

    static private int printBriefSection(
        final PendingSet[] pendingSets,
        final VersionControlClient client,
        final Display display,
        final boolean showUserColumn,
        final boolean showCandidateSection) throws ArgumentException {
        if (pendingSets.length == 0) {
            return 0;
        }

        /*
         * We need to print some different text if it's a shelveset.
         */
        Check.notNull(pendingSets[0], "pendingSets[0]"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setHeadingsVisible(true);

        final ArrayList<Column> columns = new ArrayList<Column>();
        columns.add(new Column(Messages.getString("CommandStatus.FileName"), Sizing.TIGHT)); //$NON-NLS-1$
        columns.add(new Column(Messages.getString("CommandStatus.Change"), Sizing.TIGHT)); //$NON-NLS-1$
        if (showUserColumn) {
            columns.add(new Column(Messages.getString("CommandStatus.User"), Sizing.TIGHT)); //$NON-NLS-1$
        }
        columns.add(new Column(Messages.getString("CommandStatus.LocalPath"), Sizing.EXPAND)); //$NON-NLS-1$

        table.setColumns(columns.toArray(new Column[0]));

        /*
         * First determine if any files are locked so we can hide the lock
         * column if none are.
         */
        boolean someFilesLocked = false;
        for (int i = 0; i < pendingSets.length; i++) {
            final PendingSet set = pendingSets[i];
            if (set == null) {
                continue;
            }

            final PendingChange[] changes =
                showCandidateSection ? set.getCandidatePendingChanges() : set.getPendingChanges();

            for (int j = 0; j < changes.length; j++) {
                final PendingChange change = changes[j];
                if (change == null) {
                    continue;
                }

                if (change.getLockLevel() != LockLevel.NONE && change.getLockLevel() != LockLevel.UNCHANGED) {
                    someFilesLocked = true;
                    break;
                }
            }

            if (someFilesLocked == true) {
                break;
            }
        }

        int totalChanges = 0;

        String lastServerPath = null;
        for (int i = 0; i < pendingSets.length; i++) {
            final PendingSet set = pendingSets[i];
            Check.notNull(set, "set"); //$NON-NLS-1$

            final PendingChange[] changes =
                showCandidateSection ? set.getCandidatePendingChanges() : set.getPendingChanges();

            if (changes == null) {
                continue;
            }

            for (int j = 0; j < changes.length; j++) {
                final PendingChange change = changes[j];
                if (change == null) {
                    continue;
                }

                totalChanges++;

                final String serverPath = change.getServerItem();
                final String fileName = change.getLocalItem();

                /*
                 * Only print the server path when it changes through the loop.
                 * This gives us a nice tabular/hierarchical display.
                 */
                final String justChangedToServerPath = ServerPath.getParent(serverPath);
                if (lastServerPath == null || ServerPath.equals(justChangedToServerPath, lastServerPath) == false) {
                    /*
                     * Put in a blank line before starting a new server path,
                     * but before the first.
                     */
                    if (lastServerPath != null) {
                        table.addRow(new String[] {
                            "" //$NON-NLS-1$
                        });
                    }

                    lastServerPath = justChangedToServerPath;

                    // Just add the server path.
                    table.addRow(new String[] {
                        justChangedToServerPath
                    });
                }

                String changeString = null;
                if (someFilesLocked) {
                    changeString =
                        change.getLockLevelShortName() + " " + change.getChangeType().toUIString(false, change); //$NON-NLS-1$
                } else {
                    changeString = change.getChangeType().toUIString(false, change);
                }

                final List<String> values = new ArrayList<String>();
                values.add(ServerPath.getFileName(serverPath));
                values.add(changeString);
                if (showUserColumn) {
                    values.add(set.getOwnerDisplayName());
                }
                values.add(
                    (fileName != null) ? fileName
                        : "(" + Messages.getString("CommandStatus.FileNameUnavailableLiteral") + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                table.addRow(values.toArray(new String[0]));
            }
        }

        table.print(display.getPrintStream());

        return totalChanges;
    }

    private static boolean hasPendingChange(final PendingSet[] pendingSets) {
        for (final PendingSet pendingSet : pendingSets) {
            final PendingChange[] changes = pendingSet.getPendingChanges();
            if (changes != null && changes.length > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCandidatePendingChange(final PendingSet[] pendingSets) {
        for (final PendingSet pendingSet : pendingSets) {
            final PendingChange[] changes = pendingSet.getCandidatePendingChanges();
            if (changes != null && changes.length > 0) {
                return true;
            }
        }
        return false;
    }

    private static void displayDectedCandidatesHeader(final Display display) {
        display.printLine(EMPTY);
        BasicPrinter.printSeparator(display, '-');
        display.printLine(Messages.getString("CommandStatus.DetectedChangesHeaderText")); //$NON-NLS-1$
        BasicPrinter.printSeparator(display, '-');
    }

    private static void displayCounts(final Display display, final int changeCount, final int candidateCount) {
        display.printLine(EMPTY);
        if (candidateCount > 0) {
            final String format = Messages.getString("CommandStatus.NumChangesAndDetectedFormat"); //$NON-NLS-1$
            display.printLine(MessageFormat.format(format, changeCount, candidateCount));
        } else {
            final String format = Messages.getString("CommandStatus.NumChangesFormat"); //$NON-NLS-1$
            display.printLine(MessageFormat.format(format, changeCount));
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionWorkspace.class,
            OptionShelveset.class,
            OptionFormat.class,
            OptionRecursive.class,
            OptionUser.class,
            OptionNoDetect.class,
        }, "[<itemSpec>...]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandStatus.HelpText1") //$NON-NLS-1$
        };
    }
}
