// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionDelete;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionChild;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionOwner;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.printers.LabelResultPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelChildOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public final class CommandLabel extends Command {
    public CommandLabel() {
        super();
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length < 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandLabel.LabelNameCannotBeEmpty")); //$NON-NLS-1$
        }

        final LabelSpec spec = LabelSpec.parse(getFreeArguments()[0], null, false);

        if (spec.getLabel().length() == 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandLabel.PleaseSpecifyALabelName")); //$NON-NLS-1$
        }

        if (findOptionType(OptionDelete.class) != null) {
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionChild.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionVersion.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionOwner.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionComment.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionRecursive.class);

            deleteLabel(spec);
        } else {
            createOrUpdateLabel(spec);
        }
    }

    private void deleteLabel(final LabelSpec spec)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        if (getFreeArguments().length > 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandLabel.DeleteRequiresOneLabelName")); //$NON-NLS-1$
        }

        /*
         * Pass an empty array of local paths because none of the free arguments
         * is a local path for delete.
         */
        final TFSTeamProjectCollection connection = createConnection(new String[0]);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Default to the root folder if scope wasn't supplied.
         */
        final String scope = (spec.getScope() == null) ? ServerPath.ROOT : spec.getScope();

        final LabelResult[] results = client.deleteLabel(spec.getLabel(), scope);

        if (results == null || results.length == 0) {
            setExitCode(ExitCode.FAILURE);
            return;
        }

        LabelResultPrinter.printLabelResults(results, getDisplay());
    }

    private void createOrUpdateLabel(final LabelSpec spec)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            VersionSpecParseException,
            LabelSpecParseException {
        RecursionType recursionType = RecursionType.NONE;
        String owner = VersionControlConstants.AUTHENTICATED_USER;
        String comment = null;
        LabelChildOption childOptions = LabelChildOption.FAIL;

        Option o = null;

        if (findOptionType(OptionRecursive.class) != null) {
            recursionType = RecursionType.FULL;
        }

        if ((o = findOptionType(OptionOwner.class)) != null) {
            owner = ((OptionOwner) o).getValue();
        }

        if ((o = findOptionType(OptionComment.class)) != null) {
            comment = ((OptionComment) o).getValue();
        }

        if ((o = findOptionType(OptionChild.class)) != null) {
            if (((OptionChild) o).getValue().equals(OptionChild.REPLACE)) {
                childOptions = LabelChildOption.REPLACE;
            } else if (((OptionChild) o).getValue().equals(OptionChild.MERGE)) {
                childOptions = LabelChildOption.MERGE;
            }
        }

        final QualifiedItem[] items = parseQualifiedItems(null, true, 1);

        /*
         * If there aren't items and there was no comment or owner update,
         * error.
         */
        if (items.length == 0
            && (owner == null || owner.equals(VersionControlConstants.AUTHENTICATED_USER))
            && comment == null) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandLabel.MustSpecifyItemOrCommentOrOwner")); //$NON-NLS-1$
        }

        /*
         * Prepare the subset of the free arguments that can be paths so
         * createConnection() searches the correct arguments.
         */
        final String[] pathFreeArguments = getLastFreeArguments(1);

        final TFSTeamProjectCollection connection = createConnection(pathFreeArguments, true);

        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Label commands are performed without an AWorkspace, so we just search
         * for a current workspace in order to give hints to the server when we
         * use local paths.
         */
        String workspaceName = null;
        String workspaceOwner = null;
        String workspaceOwnerDisplayName = null;

        try {
            final WorkspaceInfo cachedWorkspace = determineCachedWorkspace(pathFreeArguments);
            workspaceName = cachedWorkspace.getName();
            workspaceOwner = cachedWorkspace.getOwnerName();
            workspaceOwnerDisplayName = cachedWorkspace.getOwnerDisplayName();
        } catch (final CannotFindWorkspaceException e) {
        }

        throwIfContainsUnmappedLocalPath(pathFreeArguments);

        /*
         * Create the label we're going to send to the server.
         */
        final VersionControlLabel label =
            new VersionControlLabel(spec.getLabel(), owner, owner, spec.getScope(), comment);

        /*
         * Add all the qualified items.
         */
        final LabelItemSpec[] labelItemSpecs = new LabelItemSpec[items.length];
        for (int i = 0; i < items.length; i++) {
            VersionSpec defaultVersion;

            /*
             * If the version option was specified, use that as the default,
             * otherwise use latest.
             */
            if ((o = findOptionType(OptionVersion.class)) != null) {
                defaultVersion = VersionSpec.parseSingleVersionFromSpec(((OptionVersion) o).getValue(), owner);
            } else {
                defaultVersion = determineVersionFromPathType(
                    items[i].getPath(),
                    workspaceName,
                    workspaceOwner,
                    workspaceOwnerDisplayName);
            }

            /*
             * If the qualified item is missing a version and there was no
             * version option specified, we apply the default.
             */
            if (items[i].getVersions() == null || items[i].getVersions().length == 0) {
                items[i].setVersions(new VersionSpec[] {
                    defaultVersion
                });
            }

            final ItemSpec itemSpec = new ItemSpec(items[i].getPath(), recursionType, items[i].getDeletionID());

            /*
             * Create and add the new label item spec from the qualified item.
             */
            labelItemSpecs[i] = new LabelItemSpec(itemSpec, items[i].getVersions()[0], false);
        }

        final LabelResult[] results = client.createLabel(label, labelItemSpecs, childOptions);

        if (results == null || results.length == 0) {
            setExitCode(ExitCode.FAILURE);
            return;
        }

        LabelResultPrinter.printLabelResults(results, getDisplay());
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[2];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionOwner.class,
            OptionVersion.class,
            OptionComment.class,
            OptionChild.class,
            OptionRecursive.class
        }, "<labelName>[@<scope>] <itemSpec>..."); //$NON-NLS-1$

        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionOwner.class,
            OptionVersion.class
        }, "<labelName>[@<scope>]", new Class[] //$NON-NLS-1$
        {
            OptionDelete.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandLabel.HelpText1") //$NON-NLS-1$
        };
    }

    private VersionSpec determineVersionFromPathType(
        final String path,
        final String workspaceName,
        final String workspaceOwner,
        final String workspaceOwnerDisplayName) {
        if (path != null
            && workspaceName != null
            && workspaceOwner != null
            && workspaceOwnerDisplayName != null
            && !ServerPath.isServerPath(path)) {
            return new WorkspaceVersionSpec(workspaceName, workspaceOwner, workspaceOwnerDisplayName);
        }

        return LatestVersionSpec.INSTANCE;
    }
}
