// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit;

import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.options.shared.OptionContinueOnError;
import com.microsoft.tfs.client.clc.options.shared.OptionCopy;
import com.microsoft.tfs.client.clc.options.shared.OptionDelete;
import com.microsoft.tfs.client.clc.options.shared.OptionEdit;
import com.microsoft.tfs.client.clc.options.shared.OptionExitCode;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.options.shared.OptionHelp;
import com.microsoft.tfs.client.clc.options.shared.OptionListExitCodes;
import com.microsoft.tfs.client.clc.options.shared.OptionLogin;
import com.microsoft.tfs.client.clc.options.shared.OptionNew;
import com.microsoft.tfs.client.clc.options.shared.OptionOutputSeparator;
import com.microsoft.tfs.client.clc.options.shared.OptionServer;
import com.microsoft.tfs.client.clc.options.shared.OptionSet;
import com.microsoft.tfs.client.clc.options.shared.OptionTrial;
import com.microsoft.tfs.client.clc.options.shared.OptionUser;
import com.microsoft.tfs.client.clc.options.shared.properties.OptionBooleanProperty;
import com.microsoft.tfs.client.clc.options.shared.properties.OptionNumberProperty;
import com.microsoft.tfs.client.clc.options.shared.properties.OptionStringProperty;
import com.microsoft.tfs.client.clc.vc.options.OptionAccept;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.wit.options.OptionAddAttachment;
import com.microsoft.tfs.client.clc.wit.options.OptionAddExternalLink;
import com.microsoft.tfs.client.clc.wit.options.OptionAddHyperlink;
import com.microsoft.tfs.client.clc.wit.options.OptionAddRelated;
import com.microsoft.tfs.client.clc.wit.options.OptionCreate;
import com.microsoft.tfs.client.clc.wit.options.OptionFileID;
import com.microsoft.tfs.client.clc.wit.options.OptionForce;
import com.microsoft.tfs.client.clc.wit.options.OptionList;
import com.microsoft.tfs.client.clc.wit.options.OptionNoAttachments;
import com.microsoft.tfs.client.clc.wit.options.OptionNoHistory;
import com.microsoft.tfs.client.clc.wit.options.OptionNoLinks;
import com.microsoft.tfs.client.clc.wit.options.OptionOverwrite;
import com.microsoft.tfs.client.clc.wit.options.OptionPrivate;
import com.microsoft.tfs.client.clc.wit.options.OptionProject;
import com.microsoft.tfs.client.clc.wit.options.OptionPublic;
import com.microsoft.tfs.client.clc.wit.options.OptionRemoveAttachment;
import com.microsoft.tfs.client.clc.wit.options.OptionRemoveLink;
import com.microsoft.tfs.client.clc.wit.options.OptionRemoveRelated;
import com.microsoft.tfs.client.clc.wit.options.OptionShowQuery;
import com.microsoft.tfs.client.clc.wit.options.OptionSkipResolveLinks;
import com.microsoft.tfs.client.clc.wit.options.OptionType;
import com.microsoft.tfs.client.clc.wit.options.OptionUpdate;
import com.microsoft.tfs.client.clc.wit.options.OptionWorkItemID;

/**
 * Contains all known options.
 *
 * This class is thread-safe.
 */
public class WorkItemOptions extends OptionsMap {
    public WorkItemOptions() {
        /*
         * IMPORTANT: The first string in the names array is the option's
         * canonical name. Put other aliases or abbreviations after this name,
         * because it will be shown in help text.
         *
         * These are kept in alphabetical order for ease of maintenance.
         */
        putOption(OptionAccept.class, new String[] {
            "accept" //$NON-NLS-1$
        });
        putOption(OptionAddAttachment.class, new String[] {
            "addattachment" //$NON-NLS-1$
        });
        putOption(OptionAddExternalLink.class, new String[] {
            "addexternallink" //$NON-NLS-1$
        });
        putOption(OptionAddHyperlink.class, new String[] {
            "addhyperlink" //$NON-NLS-1$
        });
        putOption(OptionAddRelated.class, new String[] {
            "addrelated" //$NON-NLS-1$
        });
        putOption(OptionBooleanProperty.class, new String[] {
            "boolean" //$NON-NLS-1$
        });
        putOption(OptionCollection.class, new String[] {
            "collection" //$NON-NLS-1$
        });
        putOption(OptionContinueOnError.class, new String[] {
            "continueOnError" //$NON-NLS-1$
        });
        putOption(OptionCopy.class, new String[] {
            "copy" //$NON-NLS-1$
        });
        putOption(OptionCreate.class, new String[] {
            "create" //$NON-NLS-1$
        });
        putOption(OptionDelete.class, new String[] {
            "delete" //$NON-NLS-1$
        });
        putOption(OptionEdit.class, new String[] {
            "edit" //$NON-NLS-1$
        });
        putOption(OptionExitCode.class, new String[] {
            "exitcode" //$NON-NLS-1$
        });
        putOption(OptionFileID.class, new String[] {
            "fileid" //$NON-NLS-1$
        });
        putOption(OptionForce.class, new String[] {
            "force" //$NON-NLS-1$
        });
        putOption(OptionFormat.class, new String[] {
            "format" //$NON-NLS-1$
        });
        putOption(OptionHelp.class, new String[] {
            "help", //$NON-NLS-1$
            "h" //$NON-NLS-1$
        });
        putOption(OptionListExitCodes.class, new String[] {
            "listexitcodes" //$NON-NLS-1$
        });
        putOption(OptionList.class, new String[] {
            "list" //$NON-NLS-1$
        });
        putOption(OptionLogin.class, new String[] {
            "login", //$NON-NLS-1$
            "y", //$NON-NLS-1$
            "jwt" //$NON-NLS-1$
        });
        putOption(OptionNew.class, new String[] {
            "new" //$NON-NLS-1$
        });
        putOption(OptionNoAttachments.class, new String[] {
            "noattachments" //$NON-NLS-1$
        });
        putOption(OptionNoHistory.class, new String[] {
            "nohistory" //$NON-NLS-1$
        });
        putOption(OptionNoLinks.class, new String[] {
            "nolinks" //$NON-NLS-1$
        });
        putOption(OptionNoPrompt.class, new String[] {
            "noprompt" //$NON-NLS-1$
        });
        putOption(OptionNumberProperty.class, new String[] {
            "number" //$NON-NLS-1$
        });
        putOption(OptionOutputSeparator.class, new String[] {
            "outputSeparator" //$NON-NLS-1$
        });
        putOption(OptionOverwrite.class, new String[] {
            "overwrite" //$NON-NLS-1$
        });
        putOption(OptionProject.class, new String[] {
            "project" //$NON-NLS-1$
        });
        putOption(OptionPublic.class, new String[] {
            "public" //$NON-NLS-1$
        });
        putOption(OptionPrivate.class, new String[] {
            "private" //$NON-NLS-1$
        });
        putOption(OptionServer.class, new String[] {
            "server", //$NON-NLS-1$
            "repository", //$NON-NLS-1$
            "s" //$NON-NLS-1$
        });
        putOption(OptionRemoveAttachment.class, new String[] {
            "removeattachment" //$NON-NLS-1$
        });
        putOption(OptionRemoveLink.class, new String[] {
            "removelink" //$NON-NLS-1$
        });
        putOption(OptionRemoveRelated.class, new String[] {
            "removerelated" //$NON-NLS-1$
        });
        putOption(OptionSkipResolveLinks.class, new String[] {
            "skipresolvelinks" //$NON-NLS-1$
        });
        putOption(OptionShowQuery.class, new String[] {
            "showquery" //$NON-NLS-1$
        });
        putOption(OptionSet.class, new String[] {
            "set" //$NON-NLS-1$
        });
        putOption(OptionStringProperty.class, new String[] {
            "string" //$NON-NLS-1$
        });
        putOption(OptionTrial.class, new String[] {
            "trial" //$NON-NLS-1$
        });
        putOption(OptionType.class, new String[] {
            "type" //$NON-NLS-1$
        });
        putOption(OptionUpdate.class, new String[] {
            "update" //$NON-NLS-1$
        });
        putOption(OptionUser.class, new String[] {
            "user" //$NON-NLS-1$
        });
        putOption(OptionWorkItemID.class, new String[] {
            "workitem" //$NON-NLS-1$
        });
    }
}
