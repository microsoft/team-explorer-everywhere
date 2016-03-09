// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.vc.options.OptionMove;
import com.microsoft.tfs.client.clc.vc.options.OptionNoAutoResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionNoMerge;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.printers.ConflictPrinter;
import com.microsoft.tfs.client.clc.xml.SimpleXMLWriter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.UnshelveResult;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.UnshelveException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.Check;

public final class CommandUnshelve extends Command {
    private final static String UNSHELVED_CHANGES_ELEMENT_NAME = "unshelved-changes"; //$NON-NLS-1$

    public CommandUnshelve() {
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
            LicenseException,
            InputValidationException {
        final String[] freeArguments = getFreeArguments();

        if (freeArguments.length == 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandUnshelve.PleaseSpecifyShelvesetName")); //$NON-NLS-1$
        }

        final String shelvesetArgument = freeArguments[0];

        // Save off the rest of the free arguments so we can ignore the
        // shelveset name.
        final String[] fileSpecs = new String[freeArguments.length - 1];
        for (int i = 1; i < freeArguments.length; i++) {
            fileSpecs[i - 1] = freeArguments[i];
        }

        /*
         * Determine the workspace from the file specs, if available, else use
         * normal default.
         */
        final WorkspaceInfo cachedWorkspace;
        if (fileSpecs.length == 0) {
            /*
             * Even though fileSpecs is empty, passing it makes sure
             * determineCachedWorkspace() doesn't use the shelveset name as a
             * path.
             */
            cachedWorkspace = determineCachedWorkspace(fileSpecs);
        } else {
            cachedWorkspace = findSingleCachedWorkspace(fileSpecs);
            Check.notNull(cachedWorkspace, "Workspace should have been found because there was at least one filespec."); //$NON-NLS-1$
        }

        /*
         * Parse out the shelveset name and options.
         */
        final WorkspaceSpec shelvesetSpec =
            WorkspaceSpec.parse(shelvesetArgument, VersionControlConstants.AUTHENTICATED_USER);

        boolean move = false;
        if (findOptionType(OptionMove.class) != null) {
            move = true;
        }

        boolean recursive = false;
        if (findOptionType(OptionRecursive.class) != null) {
            recursive = true;
        }

        boolean noMerge = false;
        if (findOptionType(OptionNoMerge.class) != null) {
            noMerge = true;
        }

        boolean noAutoResolve = false;
        if (findOptionType(OptionNoAutoResolve.class) != null) {
            noAutoResolve = true;
        }

        final String format;
        final OptionFormat optionFormat = (OptionFormat) findOptionType(OptionFormat.class);
        if (optionFormat != null) {
            format = optionFormat.getValue();
        } else {
            format = OptionFormat.BRIEF;
        }

        final TFSTeamProjectCollection connection = createConnection(fileSpecs, true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * If XML output is enabled, we configure a writer, start the document,
         * and tell the base command to use it for its output (from new pending
         * change events).
         */
        final boolean xmlOutput = OptionFormat.XML.equalsIgnoreCase(format);
        SimpleXMLWriter xmlWriter = null;

        if (xmlOutput) {
            try {
                xmlWriter = new SimpleXMLWriter(getDisplay());
                setXMLWriter(xmlWriter);

                xmlWriter.startDocument();
                xmlWriter.startElement("", "", UNSHELVED_CHANGES_ELEMENT_NAME, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (final SAXException e) {
                throw new CLCException(e);
            } catch (final TransformerConfigurationException e) {
                throw new CLCException(e);
            }
        }

        final Workspace workspace = realizeCachedWorkspace(cachedWorkspace, client);

        client.getEventEngine().addConflictResolvedListener(this);

        final ItemSpec[] itemSpecs = (fileSpecs.length == 0) ? null
            : ItemSpec.fromStrings(fileSpecs, (recursive) ? RecursionType.FULL : RecursionType.NONE);

        final UnshelveResult result;
        try {
            // If no filespecs were given, use null to mean all items in the
            // shelveset.
            result = workspace.unshelve(
                shelvesetSpec.getName(),
                shelvesetSpec.getOwner(),
                itemSpecs,
                null,
                null,
                !noMerge,
                noAutoResolve);
        } catch (final UnshelveException e) {
            getDisplay().printErrorLine(Messages.getString("CommandUnshelve.NoChangesUnshelved")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        /*
         * Warnings/errors will have been printed during unshelve.
         */
        if (result == null) {
            setExitCode(ExitCode.PARTIAL_SUCCESS);
            return;
        }

        if (result.getStatus().getNumConflicts() > 0) {
            final Conflict[] conflicts = workspace.queryConflicts(itemSpecs);

            if (conflicts != null) {
                final String messageFormat = Messages.getString("CommandUnshelve.ConflictsFoundFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, conflicts.length);

                getDisplay().printErrorLine(message);

                for (int i = 0; i < conflicts.length; i++) {
                    ConflictPrinter.printConflict(conflicts[i], getDisplay(), false);
                }
            }

            setExitCode(ExitCode.PARTIAL_SUCCESS);
            return;
        }

        if (move && (getExitCode() == ExitCode.SUCCESS || getExitCode() == ExitCode.UNKNOWN)) {
            client.deleteShelveset(shelvesetSpec.getName(), shelvesetSpec.getOwner());

            final String messageFormat = Messages.getString("CommandUnshelve.DeletedShelvesetFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, shelvesetSpec.getName());

            getDisplay().printLine(message);
        }

        // Save the shelveset information in the last attempted checkin.
        workspace.updateLastSavedCheckin(result.getShelveset());

        /*
         * Finish the XML document and detach the writer from the base class.
         */
        if (xmlOutput) {
            try {
                xmlWriter.endElement("", "", UNSHELVED_CHANGES_ELEMENT_NAME); //$NON-NLS-1$ //$NON-NLS-2$
                xmlWriter.endDocument();
            } catch (final SAXException e) {
                throw new CLCException(e);
            }

            setXMLWriter(null);
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionMove.class,
            OptionRecursive.class,
            OptionNoMerge.class,
            OptionNoAutoResolve.class,
            OptionFormat.class
        }, "<shelvesetName[;owner]> [<itemSpec>...]"); //$NON-NLS-1$

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandUnshelve.HelpText1") //$NON-NLS-1$
        };
    }
}
