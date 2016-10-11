// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
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
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.vc.options.OptionComputer;
import com.microsoft.tfs.client.clc.vc.options.OptionOwner;
import com.microsoft.tfs.client.clc.vc.options.OptionRemove;
import com.microsoft.tfs.client.clc.vc.options.OptionUpdateComputerName;
import com.microsoft.tfs.client.clc.vc.options.OptionUpdateUserName;
import com.microsoft.tfs.client.clc.vc.printers.BasicPrinter;
import com.microsoft.tfs.client.clc.xml.CommonXMLNames;
import com.microsoft.tfs.client.clc.xml.SimpleXMLWriter;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissions;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalServerInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;

public final class CommandWorkspaces extends Command {
    private final static String WORKSPACES_ELEMENT_NAME = "workspaces"; //$NON-NLS-1$
    private final static String TYPE_ATTRIBUTE_NAME = "type"; //$NON-NLS-1$
    private final static String DEPTH_ATTRIBUTE_NAME = "depth"; //$NON-NLS-1$

    public CommandWorkspaces() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (findOptionType(OptionRemove.class) != null) {
            if (findOptionType(OptionOwner.class) != null
                || findOptionType(OptionComputer.class) != null
                || findOptionType(OptionFormat.class) != null
                || findOptionType(OptionUpdateUserName.class) != null
                || findOptionType(OptionUpdateComputerName.class) != null) {
                throw new InvalidOptionException(
                    //@formatter:off
                    Messages.getString("CommandWorkspaces.RemoveCannotBeUsedWithOwnerComputerFormatUpdateusernameUpdatecomputername")); //$NON-NLS-1$
                    //@formatter:on
            }
            removeCachedWorkspace();
        } else {
            displayAndUpdate();
        }
    }

    private void removeCachedWorkspace()
        throws InvalidOptionValueException,
            InvalidFreeArgumentException,
            CLCException,
            InvalidOptionException {
        if (getFreeArguments().length > 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandWorkspaces.NoWorkspaceNamesWithRemove")); //$NON-NLS-1$
        }

        // TODO allow wildcards for server
        URI serverURI = null;
        String rawServerString = null;

        final OptionCollection collectionOption = getCollectionOption();
        if (collectionOption != null) {
            serverURI = collectionOption.getURI();
            rawServerString = collectionOption.getValue();
        }

        // Split on commas.
        final String[] workspaces = ((OptionRemove) findOptionType(OptionRemove.class)).getValue().split(","); //$NON-NLS-1$

        // Remove the empties.
        final List<String> workspacesArray = new ArrayList<String>();
        for (int i = 0; i < workspaces.length; i++) {
            if (workspaces[i] != null && workspaces[i].trim().length() > 0) {
                workspacesArray.add(workspaces[i].trim());
            }
        }

        final String[] workspacesToRemove = workspacesArray.toArray(new String[workspacesArray.size()]);

        if (workspacesToRemove.length < 1) {
            throw new InvalidOptionValueException(
                Messages.getString("CommandWorkspaces.RemoveRequiresAtLeastOneWorkspaceOrWildcard")); //$NON-NLS-1$
        }

        /*
         * See if the wildcard was specified, throwing an error if it was and
         * more workspaces were given.
         */
        for (int i = 0; i < workspacesToRemove.length; i++) {
            if (workspacesToRemove[i].equalsIgnoreCase("*")) //$NON-NLS-1$
            {
                if (workspacesToRemove.length > 1) {
                    throw new InvalidOptionValueException(
                        Messages.getString("CommandWorkspaces.WildcardCannotBeUsedWithOtherWorkspaceNames")); //$NON-NLS-1$
                }

                // Wildcard was the only workspace to remove.
                break;
            }
        }

        boolean hadError = false;

        for (int i = 0; i < workspacesToRemove.length; i++) {
            final String workspaceDisplayName = workspacesToRemove[i];
            final String workspaceRealName = (workspacesToRemove[i].equalsIgnoreCase("*")) ? null //$NON-NLS-1$
                : workspacesToRemove[i];

            final WorkspaceInfo[] removed = Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).removeCachedWorkspaceInfo(
                serverURI,
                workspaceRealName,
                null);

            if (removed == null || removed.length < 1) {
                hadError = true;

                final String message;

                if (rawServerString != null) {
                    final String messageFormat =
                        Messages.getString("CommandWorkspaces.NoWorkspaceInCacheMatchesForServerFormat"); //$NON-NLS-1$
                    message = MessageFormat.format(messageFormat, workspaceDisplayName, rawServerString);
                } else {
                    final String messageFormat =
                        Messages.getString("CommandWorkspaces.NoWorkspaceInCacheMatchesForAllServersFormat"); //$NON-NLS-1$
                    message = MessageFormat.format(messageFormat, workspaceDisplayName);
                }

                getDisplay().printErrorLine(message);
            } else {
                /*
                 * We may have removed multiple cached workspaces (wildcard), so
                 * print them all.
                 */
                for (int j = 0; j < removed.length; j++) {
                    getDisplay().printLine(
                        new WorkspaceSpec(removed[j].getName(), removed[j].getOwnerDisplayName()).toString());
                }
            }
        }

        Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).saveConfigIfDirty();

        if (hadError) {
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }
    }

    private void displayAndUpdate() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length > 1) {
            final String messageFormat =
                Messages.getString("CommandWorkspaces.CommandRequiresAtMostOneWorkspaceFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        String computer = null;
        OptionFormat formatOption = null;
        String workspaceName = null;
        String workspaceOwner = null;
        URI serverURI = null;
        String updateUserName = null;
        String updateComputerName = null;

        Option o = null;

        /*
         * Parse these up front.
         */

        if ((o = findOptionType(OptionComputer.class)) != null) {
            computer = ((OptionComputer) o).getValue();
        }

        if ((o = findOptionType(OptionFormat.class)) != null) {
            formatOption = (OptionFormat) o;
        }

        if ((o = findOptionType(OptionUpdateUserName.class)) != null) {
            updateUserName = ((OptionUpdateUserName) o).getValue();
        }

        if ((o = findOptionType(OptionUpdateComputerName.class)) != null) {
            updateComputerName = ((OptionUpdateComputerName) o).getValue();
        }

        final OptionCollection collectionOption = getCollectionOption();
        if (collectionOption != null) {
            serverURI = collectionOption.getURI();
        }

        final String format = (formatOption == null) ? OptionFormat.BRIEF : formatOption.getValue();

        if (getFreeArguments().length != 0) {
            final WorkspaceSpec spec = WorkspaceSpec.parse(getFreeArguments()[0], null);
            workspaceName = spec.getName();
            workspaceOwner = spec.getOwner();
        }

        /*
         * If they provided an owner option, let it override the one parsed from
         * the workspace spec.
         */
        if ((o = findOptionType(OptionOwner.class)) != null) {
            workspaceOwner = ((OptionOwner) o).getValue();
        }

        /*
         * Only set the exit code if the user was searching for something.
         */
        final boolean partialSuccessOnFailedQuery =
            (computer != null || workspaceOwner != null || getFreeArguments().length != 0);

        /*
         * Smash wildcards down to null values for easier testing.
         */
        if (workspaceName == null || workspaceName.equals("*")) //$NON-NLS-1$
        {
            workspaceName = null;
        }

        /*
         * Since we support "*" to mean "all" for both computer and owner, we
         * don't allow wildcards inside the strings.
         */
        if (computer != null && computer.equalsIgnoreCase("*") == false && Wildcard.isWildcard(computer)) //$NON-NLS-1$
        {
            throw new InvalidOptionException(Messages.getString("CommandWorkspaces.WildcardsNotAllowedInComputer")); //$NON-NLS-1$
        }

        if (workspaceOwner != null
            && workspaceOwner.equalsIgnoreCase("*") == false //$NON-NLS-1$
            && Wildcard.isWildcard(workspaceOwner)) {
            throw new InvalidOptionException(Messages.getString("CommandWorkspaces.WildcardsNotAllowedInOwner")); //$NON-NLS-1$
        }

        TFSTeamProjectCollection connection = null;
        VersionControlClient client = null;

        if (serverURI != null) {
            /*
             * We need a connection.
             */
            connection = createConnection(true, true);
            client = connection.getVersionControlClient();
            initializeClient(client);

            /*
             * Update the data on the server (and the local caches).
             */
            updateWorkspaces(connection, client, serverURI, workspaceOwner, updateUserName, updateComputerName);
        } else if (updateUserName != null || updateComputerName != null) {
            throw new InvalidOptionException(
                Messages.getString("CommandWorkspaces.ServerRequiredWithUpdatecomputernameUpdateusername")); //$NON-NLS-1$
        }

        Workspace[] workspaces = null;
        WorkspaceInfo[] workspaceInfos = null;

        /*
         * No filters, so add all workspaces to the list.
         */
        if (serverURI == null && computer == null && workspaceName == null && workspaceOwner == null) {
            if (OptionFormat.DETAILED.equalsIgnoreCase(format) || OptionFormat.XML.equalsIgnoreCase(format)) {
                throw new InvalidOptionValueException(
                    Messages.getString("CommandWorkspaces.DetailedAndXMLFormatsRequireCollectionOption")); //$NON-NLS-1$
            }

            workspaceInfos = Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).getAllLocalWorkspaceInfo();

            if (workspaceInfos.length == 0) {
                getDisplay().printLine(Messages.getString("CommandWorkspaces.NoLocalWorkspacesFound")); //$NON-NLS-1$
                setExitCode(ExitCode.PARTIAL_SUCCESS);
                return;
            }
        } else {
            /*
             * Filter based on the criteria the user supplied.
             */

            if (computer == null && workspaceOwner == null) {
                computer = LocalHost.getShortName();
            } else if (computer != null && computer.equalsIgnoreCase("*")) //$NON-NLS-1$
            {
                computer = null;
            }

            if (workspaceOwner == null) {
                workspaceOwner = VersionControlConstants.AUTHENTICATED_USER;
            } else if (workspaceOwner != null && workspaceOwner.equalsIgnoreCase("*")) //$NON-NLS-1$
            {
                workspaceOwner = null;
            }

            if (connection == null || client == null) {
                /*
                 * No connection as of yet. We pretty much require the server
                 * option at this point, because we need to connect to some
                 * server for the query.
                 *
                 * The default "cannot find workspace" exception doesn't make a
                 * lot of sense here, so we rethrow.
                 */
                try {
                    connection = createConnection();
                    client = connection.getVersionControlClient();
                    initializeClient(client);
                } catch (final CannotFindWorkspaceException e) {
                    throw new CannotFindWorkspaceException(
                        Messages.getString("CommandWorkspaces.CouldNotDetermineTheServerToQuery")); //$NON-NLS-1$
                }
            }

            if (workspaceOwner != null && workspaceOwner.equalsIgnoreCase(VersionControlConstants.AUTHENTICATED_USER)) {
                final WorkspacePermissions publicLimitedPermissions =
                    WorkspacePermissions.USE.combine(WorkspacePermissions.READ);
                workspaces = client.queryWorkspaces(workspaceName, workspaceOwner, computer, publicLimitedPermissions);
            } else {
                workspaces = client.queryWorkspaces(workspaceName, workspaceOwner, computer);
            }

            if (workspaces.length == 0) {
                /*
                 * Resolve our TFS names back into meaningful strings for the
                 * error.
                 */

                final String authenticatedUserName = connection.getAuthorizedIdentity().getDisplayName();
                if (workspaceOwner != null && workspaceOwner.equals(VersionControlConstants.AUTHENTICATED_USER)) {
                    workspaceOwner = authenticatedUserName;
                } else if (workspaceOwner == null) {
                    workspaceOwner = "*"; //$NON-NLS-1$
                }

                workspaceName =
                    new WorkspaceSpec((workspaceName == null) ? "*" : workspaceName, workspaceOwner).toString(); //$NON-NLS-1$

                final String messageFormat =
                    Messages.getString("CommandWorkspaces.NoWorkspaceMatchingNameOnComputerFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, workspaceName, (computer == null ? "*" //$NON-NLS-1$
                    : computer), (serverURI == null) ? "*" : URIUtils.decodeForDisplay(serverURI)); //$NON-NLS-1$

                getDisplay().printLine(message);

                if (partialSuccessOnFailedQuery) {
                    setExitCode(ExitCode.PARTIAL_SUCCESS);
                    return;
                }
            }
        }

        /*
         * Time to display what we found.
         */

        if (OptionFormat.BRIEF.equalsIgnoreCase(format)) {
            // If Workspaces were queried, convert to WorkspaceInfo
            if (workspaces != null) {
                workspaceInfos = new WorkspaceInfo[workspaces.length];

                for (int i = 0; i < workspaces.length; i++) {
                    workspaceInfos[i] = new WorkspaceInfo(
                        new InternalServerInfo(workspaces[i].getServerURI(), workspaces[i].getServerGUID()),
                        workspaces[i]);
                }
            }

            Arrays.sort(workspaceInfos);
            printBrief(workspaceInfos);
        } else if (OptionFormat.DETAILED.equalsIgnoreCase(format)) {
            Arrays.sort(workspaces);
            printDetailed(workspaces);
        } else if (OptionFormat.XML.equalsIgnoreCase(format)) {
            Arrays.sort(workspaces);

            try {
                printXML(workspaces);
            } catch (final TransformerConfigurationException e) {
                throw new CLCException(e);
            } catch (final SAXException e) {
                throw new CLCException(e);
            }
        }
    }

    private void printBrief(final WorkspaceInfo[] infos) {
        /*
         * Do the brief, tabular display.
         */
        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());

        table.setColumns(new Column[] {
            new Column(Messages.getString("CommandWorkspaces.Workspace"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("CommandWorkspaces.Owner"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("CommandWorkspaces.Computer"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("CommandWorkspaces.Comment"), Sizing.EXPAND) //$NON-NLS-1$
        });

        String lastServerURIString = null;
        for (int i = 0; i < infos.length; i++) {
            final WorkspaceInfo info = infos[i];

            final String currentNormalizedURIString = (info.getServerURI() != null)
                ? URIUtils.ensurePathHasTrailingSlash(info.getServerURI()).toString() : null;

            /*
             * Print the null case and when the current URI is different from
             * the last one we've printed.
             *
             * TFS is always case-insensitive about server paths (virtual
             * directory, project collection names, etc.), and since schemes and
             * DNS hostnames are case-insensitive, compare the whole URI string
             * case-insensitive so items group correctly in the output.
             */
            if (info.getServerURI() == null
                || (currentNormalizedURIString != null
                    && currentNormalizedURIString.equalsIgnoreCase(lastServerURIString) == false)) {
                /*
                 * Print the table for the previous server if there were rows to
                 * print.
                 */
                if (table.getRowCount() > 0) {
                    table.print(getDisplay().getPrintStream());
                }

                if (lastServerURIString != null) {
                    getDisplay().printLine(""); //$NON-NLS-1$
                }

                // Print this new server's line.
                getDisplay().printLine(Messages.getString("CommandWorkspaces.Collection") //$NON-NLS-1$
                    + ((currentNormalizedURIString == null)
                        ? "<" + Messages.getString("CommandWorkspaces.CollectionURIUnknownLiteral") + ">" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        : URIUtils.decodeForDisplay(currentNormalizedURIString)));

                // Clear the rows to be added for this server.
                table.clearRows();

                lastServerURIString = ((currentNormalizedURIString == null) ? null : currentNormalizedURIString);
            }

            table.addRow(new String[] {
                info.getName(),
                info.getOwnerDisplayName(),
                info.getComputer(),
                info.getComment()
            });
        }

        if (table.getRowCount() > 0) {
            table.print(getDisplay().getPrintStream());
        }
    }

    private void updateWorkspaces(
        final TFSTeamProjectCollection connection,
        final VersionControlClient client,
        final URI serverURI,
        String workspaceOwner,
        String updateUserName,
        final String updateComputerName) throws CLCException {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        if (updateUserName != null) {
            client.updateUserName();
            if (UserNameUtil.isComplete(updateUserName) == false) {
                updateUserName =
                    UserNameUtil.makeComplete(updateUserName, connection.getAuthorizedTFSUser().toString(), true);
            }

            Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).removeCachedWorkspaceInfo(serverURI, null, updateUserName);
        }

        if (workspaceOwner == null || workspaceOwner.equals("*")) //$NON-NLS-1$
        {
            workspaceOwner = connection.getAuthorizedTFSUser().toString();
        }

        if (updateComputerName != null) {
            final Workspace[] workspaces = client.queryWorkspaces(null, workspaceOwner, updateComputerName);

            for (int i = 0; i < workspaces.length; i++) {
                final Workspace workspace = workspaces[i];
                workspace.updateComputerName();
            }
        }

        // Finally, go get the current set of workspaces for the owner that are
        // in the repository and update the local cache. This will also update
        // the cache for any user and computer name updates from above.
        Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).updateWorkspaceInfoCache(client, workspaceOwner);
    }

    private void printXML(final Workspace[] workspaces) throws TransformerConfigurationException, SAXException {
        Check.notNull(workspaces, "workspaces"); //$NON-NLS-1$

        final SimpleXMLWriter xmlWriter = new SimpleXMLWriter(getDisplay());

        xmlWriter.startDocument();
        xmlWriter.startElement("", "", WORKSPACES_ELEMENT_NAME, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$

        for (int i = 0; i < workspaces.length; i++) {
            final Workspace workspace = workspaces[i];

            final AttributesImpl workspaceAttributes = new AttributesImpl();

            workspaceAttributes.addAttribute("", "", CommonXMLNames.NAME, "CDATA", workspace.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            workspaceAttributes.addAttribute("", "", CommonXMLNames.OWNER, "CDATA", workspace.getOwnerName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            workspaceAttributes.addAttribute(
                "", //$NON-NLS-1$
                "", //$NON-NLS-1$
                CommonXMLNames.OWNER_DISPLAY_NAME,
                "CDATA", //$NON-NLS-1$
                workspace.getOwnerDisplayName());
            workspaceAttributes.addAttribute("", "", CommonXMLNames.COMPUTER, "CDATA", workspace.getComputer()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            workspaceAttributes.addAttribute("", "", CommonXMLNames.COMMENT, "CDATA", workspace.getComment()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            workspaceAttributes.addAttribute(
                "", //$NON-NLS-1$
                "", //$NON-NLS-1$
                CommonXMLNames.SERVER,
                "CDATA", //$NON-NLS-1$
                URIUtils.decodeForDisplay(workspace.getServerURI()));

            xmlWriter.startElement("", "", CommonXMLNames.WORKSPACE, workspaceAttributes); //$NON-NLS-1$ //$NON-NLS-2$

            final WorkingFolder[] workingFolders = workspace.getFolders();
            if (workingFolders != null) {
                for (int j = 0; j < workingFolders.length; j++) {
                    final WorkingFolder workingFolder = workingFolders[j];

                    final AttributesImpl workingFolderAttributes = new AttributesImpl();

                    String typeString = "unknown"; //$NON-NLS-1$
                    if (workingFolder.getType() == WorkingFolderType.MAP) {
                        typeString = "map"; //$NON-NLS-1$
                    } else if (workingFolders[j].getType() == WorkingFolderType.CLOAK) {
                        typeString = "cloak"; //$NON-NLS-1$
                    }

                    /*
                     * Use getServerItem() instead of getDisplayServerItem()
                     * because the depth is expressed as an attribute and would
                     * be redundant here.
                     */
                    workingFolderAttributes.addAttribute(
                        "", //$NON-NLS-1$
                        "", //$NON-NLS-1$
                        CommonXMLNames.SERVER_ITEM,
                        "CDATA", //$NON-NLS-1$
                        workingFolder.getServerItem());

                    if (workingFolders[j].getType() != WorkingFolderType.CLOAK) {
                        workingFolderAttributes.addAttribute(
                            "", //$NON-NLS-1$
                            "", //$NON-NLS-1$
                            CommonXMLNames.LOCAL_ITEM,
                            "CDATA", //$NON-NLS-1$
                            workingFolder.getLocalItem());
                    }

                    workingFolderAttributes.addAttribute("", "", TYPE_ATTRIBUTE_NAME, "CDATA", typeString); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                    String depthString = "unknown"; //$NON-NLS-1$
                    if (workingFolder.getDepth() == RecursionType.FULL) {
                        depthString = "full"; //$NON-NLS-1$
                    } else if (workingFolder.getDepth() == RecursionType.NONE) {
                        depthString = "none"; //$NON-NLS-1$
                    } else if (workingFolder.getDepth() == RecursionType.ONE_LEVEL) {
                        depthString = "one-level"; //$NON-NLS-1$
                    }

                    workingFolderAttributes.addAttribute("", "", DEPTH_ATTRIBUTE_NAME, "CDATA", depthString); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                    xmlWriter.startElement("", "", CommonXMLNames.WORKING_FOLDER, workingFolderAttributes); //$NON-NLS-1$ //$NON-NLS-2$
                    xmlWriter.endElement("", "", CommonXMLNames.WORKING_FOLDER); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            xmlWriter.endElement("", "", CommonXMLNames.WORKSPACE); //$NON-NLS-1$ //$NON-NLS-2$
        }

        xmlWriter.endElement("", "", WORKSPACES_ELEMENT_NAME); //$NON-NLS-1$ //$NON-NLS-2$
        xmlWriter.endDocument();
    }

    private void printDetailed(final Workspace[] workspaces) {
        /*
         * This table can be re-used for each item (even working folders) with
         * the same column settings.
         */
        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setHeadingsVisible(false);
        table.setColumns(new Column[] {
            new Column("", Sizing.TIGHT), //$NON-NLS-1$
            new Column("", Sizing.EXPAND) //$NON-NLS-1$
        });

        for (int i = 0; i < workspaces.length; i++) {
            final Workspace workspace = workspaces[i];

            table.clearRows();

            /*
             * Write a separator row.
             */
            BasicPrinter.printSeparator(getDisplay(), '=');

            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.WorkspaceColon"), //$NON-NLS-1$
                workspace.getName()
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.OwnerColon"), //$NON-NLS-1$
                workspace.getOwnerDisplayName()
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.ComputerColon"), //$NON-NLS-1$
                workspace.getComputer()
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.CommentColon"), //$NON-NLS-1$
                workspace.getComment()
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.CollectionColon"), //$NON-NLS-1$
                workspace.getServerName()
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.PermissionProfileColon"), //$NON-NLS-1$
                workspace.getPermissionsProfile().getBuiltinIndex() == WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PRIVATE
                    ? Messages.getString("CommandWorkspaces.PermissionProfilePrivate") //$NON-NLS-1$
                    : workspace.getPermissionsProfile().getBuiltinIndex() == WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC_LIMITED
                        ? Messages.getString("CommandWorkspaces.PermissionProfilePublicLimited") //$NON-NLS-1$
                        : workspace.getPermissionsProfile().getBuiltinIndex() == WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC
                            ? Messages.getString("CommandWorkspaces.PermissionProfilePublic") //$NON-NLS-1$
                            : Messages.getString("CommandWorkspaces.PermissionProfileCustom") //$NON-NLS-1$
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.FileTimeColon"), //$NON-NLS-1$
                workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                    ? Messages.getString("CommandWorkspaces.FileTimeCheckin") //$NON-NLS-1$
                    : Messages.getString("CommandWorkspaces.FileTimeCurrent") //$NON-NLS-1$
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.LocationColon"), //$NON-NLS-1$
                workspace.getLocation() == WorkspaceLocation.SERVER
                    ? Messages.getString("CommandWorkspaces.LocationServer") //$NON-NLS-1$
                    : Messages.getString("CommandWorkspaces.LocationLocal") //$NON-NLS-1$
            });
            table.addRow(new String[] {
                Messages.getString("CommandWorkspaces.FileTimeColon"), //$NON-NLS-1$
                workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                    ? Messages.getString("CommandWorkspaces.FileTimeCheckin") //$NON-NLS-1$
                    : Messages.getString("CommandWorkspaces.FileTimeCurrent") //$NON-NLS-1$
            });

            table.print(getDisplay().getPrintStream());

            table.clearRows();

            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(Messages.getString("CommandWorkspaces.WorkingFoldersColon")); //$NON-NLS-1$
            getDisplay().printLine(""); //$NON-NLS-1$

            final WorkingFolder[] workingFolders = workspace.getFolders();
            if (workingFolders != null) {
                for (int j = 0; j < workingFolders.length; j++) {
                    final WorkingFolder workingFolder = workingFolders[j];

                    String serverItemString = " "; //$NON-NLS-1$

                    if (workingFolder.getType() == WorkingFolderType.CLOAK) {
                        serverItemString =
                            " (" + Messages.getString("CommandWorkspaces.WorkingFolderTypeCloakedLiteral") + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }

                    serverItemString = serverItemString + workingFolder.getDisplayServerItem() + ":"; //$NON-NLS-1$

                    table.addRow(new String[] {
                        serverItemString,
                        workingFolder.getLocalItem()
                    });
                }
            }

            table.print(getDisplay().getPrintStream());
            getDisplay().printLine(""); //$NON-NLS-1$
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[2];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionOwner.class,
            OptionComputer.class,
            OptionCollection.class,
            OptionFormat.class,
            OptionUpdateUserName.class,
            OptionUpdateComputerName.class
        }, "workspaceName"); //$NON-NLS-1$

        // This optionSet includes a required option (remove).
        optionSets[1] = new AcceptedOptionSet(new Class[0], "", new Class[] //$NON-NLS-1$
        {
            OptionRemove.class,
            OptionCollection.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandWorkspaces.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandWorkspaces.HelpText2") //$NON-NLS-1$
        };
    }
}
