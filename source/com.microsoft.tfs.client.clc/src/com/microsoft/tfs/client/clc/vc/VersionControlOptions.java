// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.options.shared.OptionBuildName;
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
import com.microsoft.tfs.client.clc.options.shared.OptionTeamProject;
import com.microsoft.tfs.client.clc.options.shared.OptionTrial;
import com.microsoft.tfs.client.clc.options.shared.OptionUser;
import com.microsoft.tfs.client.clc.options.shared.properties.OptionBooleanProperty;
import com.microsoft.tfs.client.clc.options.shared.properties.OptionNumberProperty;
import com.microsoft.tfs.client.clc.options.shared.properties.OptionStringProperty;
import com.microsoft.tfs.client.clc.vc.options.OptionAccept;
import com.microsoft.tfs.client.clc.vc.options.OptionAdds;
import com.microsoft.tfs.client.clc.vc.options.OptionAll;
import com.microsoft.tfs.client.clc.vc.options.OptionAssociate;
import com.microsoft.tfs.client.clc.vc.options.OptionAuthor;
import com.microsoft.tfs.client.clc.vc.options.OptionAuto;
import com.microsoft.tfs.client.clc.vc.options.OptionBaseless;
import com.microsoft.tfs.client.clc.vc.options.OptionBypass;
import com.microsoft.tfs.client.clc.vc.options.OptionCandidate;
import com.microsoft.tfs.client.clc.vc.options.OptionChangeset;
import com.microsoft.tfs.client.clc.vc.options.OptionCheckin;
import com.microsoft.tfs.client.clc.vc.options.OptionChild;
import com.microsoft.tfs.client.clc.vc.options.OptionCloak;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionComputer;
import com.microsoft.tfs.client.clc.vc.options.OptionConvertToType;
import com.microsoft.tfs.client.clc.vc.options.OptionDecloak;
import com.microsoft.tfs.client.clc.vc.options.OptionDeleteAll;
import com.microsoft.tfs.client.clc.vc.options.OptionDeleteValues;
import com.microsoft.tfs.client.clc.vc.options.OptionDeleted;
import com.microsoft.tfs.client.clc.vc.options.OptionDeletes;
import com.microsoft.tfs.client.clc.vc.options.OptionDetect;
import com.microsoft.tfs.client.clc.vc.options.OptionDiff;
import com.microsoft.tfs.client.clc.vc.options.OptionDiscard;
import com.microsoft.tfs.client.clc.vc.options.OptionExclude;
import com.microsoft.tfs.client.clc.vc.options.OptionFileTime;
import com.microsoft.tfs.client.clc.vc.options.OptionFolders;
import com.microsoft.tfs.client.clc.vc.options.OptionForce;
import com.microsoft.tfs.client.clc.vc.options.OptionForgetBuild;
import com.microsoft.tfs.client.clc.vc.options.OptionItemMode;
import com.microsoft.tfs.client.clc.vc.options.OptionKeepHistory;
import com.microsoft.tfs.client.clc.vc.options.OptionKeepMergeHistory;
import com.microsoft.tfs.client.clc.vc.options.OptionLatest;
import com.microsoft.tfs.client.clc.vc.options.OptionLocation;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionMap;
import com.microsoft.tfs.client.clc.vc.options.OptionMove;
import com.microsoft.tfs.client.clc.vc.options.OptionNewName;
import com.microsoft.tfs.client.clc.vc.options.OptionNoAutoResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionNoDetect;
import com.microsoft.tfs.client.clc.vc.options.OptionNoGet;
import com.microsoft.tfs.client.clc.vc.options.OptionNoIgnore;
import com.microsoft.tfs.client.clc.vc.options.OptionNoImplicitBaseless;
import com.microsoft.tfs.client.clc.vc.options.OptionNoMerge;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionNoSummary;
import com.microsoft.tfs.client.clc.vc.options.OptionNotes;
import com.microsoft.tfs.client.clc.vc.options.OptionOutput;
import com.microsoft.tfs.client.clc.vc.options.OptionOverride;
import com.microsoft.tfs.client.clc.vc.options.OptionOverrideType;
import com.microsoft.tfs.client.clc.vc.options.OptionOverwrite;
import com.microsoft.tfs.client.clc.vc.options.OptionOwner;
import com.microsoft.tfs.client.clc.vc.options.OptionPermission;
import com.microsoft.tfs.client.clc.vc.options.OptionPreview;
import com.microsoft.tfs.client.clc.vc.options.OptionProxy;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionRemove;
import com.microsoft.tfs.client.clc.vc.options.OptionReplace;
import com.microsoft.tfs.client.clc.vc.options.OptionResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionSaved;
import com.microsoft.tfs.client.clc.vc.options.OptionSetValues;
import com.microsoft.tfs.client.clc.vc.options.OptionShelveset;
import com.microsoft.tfs.client.clc.vc.options.OptionSilent;
import com.microsoft.tfs.client.clc.vc.options.OptionSlotMode;
import com.microsoft.tfs.client.clc.vc.options.OptionStartCleanup;
import com.microsoft.tfs.client.clc.vc.options.OptionStopAfter;
import com.microsoft.tfs.client.clc.vc.options.OptionStopAt;
import com.microsoft.tfs.client.clc.vc.options.OptionTemplate;
import com.microsoft.tfs.client.clc.vc.options.OptionToVersion;
import com.microsoft.tfs.client.clc.vc.options.OptionType;
import com.microsoft.tfs.client.clc.vc.options.OptionUnmap;
import com.microsoft.tfs.client.clc.vc.options.OptionUpdateComputerName;
import com.microsoft.tfs.client.clc.vc.options.OptionUpdateUserName;
import com.microsoft.tfs.client.clc.vc.options.OptionValidate;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;

/**
 * Contains all known options.
 *
 * This class is thread-safe.
 */
public class VersionControlOptions extends OptionsMap {
    public VersionControlOptions() {
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
        putOption(OptionAdds.class, new String[] {
            "adds" //$NON-NLS-1$
        });
        putOption(OptionAll.class, new String[] {
            "all" //$NON-NLS-1$
        });
        putOption(OptionAssociate.class, new String[] {
            "associate" //$NON-NLS-1$
        });
        putOption(OptionAuthor.class, new String[] {
            "author" //$NON-NLS-1$
        });
        putOption(OptionAuto.class, new String[] {
            "auto" //$NON-NLS-1$
        });
        putOption(OptionBaseless.class, new String[] {
            "baseless" //$NON-NLS-1$
        });
        putOption(OptionBooleanProperty.class, new String[] {
            "boolean" //$NON-NLS-1$
        });
        putOption(OptionBuildName.class, new String[] {
            "buildName" //$NON-NLS-1$
        });
        putOption(OptionBypass.class, new String[] {
            "bypass" //$NON-NLS-1$
        });
        putOption(OptionCandidate.class, new String[] {
            "candidate" //$NON-NLS-1$
        });
        putOption(OptionChangeset.class, new String[] {
            "changeset" //$NON-NLS-1$
        });
        putOption(OptionCheckin.class, new String[] {
            "checkin" //$NON-NLS-1$
        });
        putOption(OptionChild.class, new String[] {
            "child" //$NON-NLS-1$
        });
        putOption(OptionCloak.class, new String[] {
            "cloak" //$NON-NLS-1$
        });
        putOption(OptionCollection.class, new String[] {
            "collection" //$NON-NLS-1$
        });
        putOption(OptionComment.class, new String[] {
            "comment" //$NON-NLS-1$
        });
        putOption(OptionComputer.class, new String[] {
            "computer" //$NON-NLS-1$
        });
        putOption(OptionContinueOnError.class, new String[] {
            "continueOnError" //$NON-NLS-1$
        });
        putOption(OptionConvertToType.class, new String[] {
            "converttotype" //$NON-NLS-1$
        });
        putOption(OptionCopy.class, new String[] {
            "copy" //$NON-NLS-1$
        });
        putOption(OptionDecloak.class, new String[] {
            "decloak" //$NON-NLS-1$
        });
        putOption(OptionDelete.class, new String[] {
            "delete" //$NON-NLS-1$
        });
        putOption(OptionDeleteAll.class, new String[] {
            "deleteall" //$NON-NLS-1$
        });
        putOption(OptionDeleted.class, new String[] {
            "deleted" //$NON-NLS-1$
        });
        putOption(OptionDeletes.class, new String[] {
            "deletes" //$NON-NLS-1$
        });
        putOption(OptionDeleteValues.class, new String[] {
            "deletevalues" //$NON-NLS-1$
        });
        putOption(OptionDetect.class, new String[] {
            "detect" //$NON-NLS-1$
        });
        putOption(OptionDiff.class, new String[] {
            "diff" //$NON-NLS-1$
        });
        putOption(OptionDiscard.class, new String[] {
            "discard" //$NON-NLS-1$
        });
        putOption(OptionEdit.class, new String[] {
            "edit" //$NON-NLS-1$
        });
        putOption(OptionExclude.class, new String[] {
            "exclude" //$NON-NLS-1$
        });
        putOption(OptionExitCode.class, new String[] {
            "exitcode" //$NON-NLS-1$
        });
        putOption(OptionFileTime.class, new String[] {
            "filetime" //$NON-NLS-1$
        });
        putOption(OptionFolders.class, new String[] {
            "folders" //$NON-NLS-1$
        });
        putOption(OptionForce.class, new String[] {
            "force", //$NON-NLS-1$
            "f" //$NON-NLS-1$
        });
        putOption(OptionForgetBuild.class, new String[] {
            "forgetBuild" //$NON-NLS-1$
        });
        putOption(OptionFormat.class, new String[] {
            "format" //$NON-NLS-1$
        });
        putOption(OptionHelp.class, new String[] {
            "help", //$NON-NLS-1$
            "h", //$NON-NLS-1$
            "?" //$NON-NLS-1$
        });
        putOption(OptionItemMode.class, new String[] {
            "itemmode" //$NON-NLS-1$
        });
        putOption(OptionKeepHistory.class, new String[] {
            "keephistory" //$NON-NLS-1$
        });
        putOption(OptionKeepMergeHistory.class, new String[] {
            "keepmergehistory" //$NON-NLS-1$
        });
        putOption(OptionNew.class, new String[] {
            "new" //$NON-NLS-1$
        });
        putOption(OptionNoAutoResolve.class, new String[] {
            "noautoresolve" //$NON-NLS-1$
        });
        putOption(OptionListExitCodes.class, new String[] {
            "listexitcodes" //$NON-NLS-1$
        });
        putOption(OptionLatest.class, new String[] {
            "latest" //$NON-NLS-1$
        });
        putOption(OptionLocation.class, new String[] {
            "location" //$NON-NLS-1$
        });
        putOption(OptionLock.class, new String[] {
            "lock", //$NON-NLS-1$
            "k" //$NON-NLS-1$
        });
        putOption(OptionLogin.class, new String[] {
            "login", //$NON-NLS-1$
            "y", //$NON-NLS-1$
            "jwt" //$NON-NLS-1$
        });
        putOption(OptionMap.class, new String[] {
            "map" //$NON-NLS-1$
        });
        putOption(OptionMove.class, new String[] {
            "move" //$NON-NLS-1$
        });
        putOption(OptionNew.class, new String[] {
            "new" //$NON-NLS-1$
        });
        putOption(OptionNewName.class, new String[] {
            "newname" //$NON-NLS-1$
        });
        putOption(OptionNoDetect.class, new String[] {
            "nodetect" //$NON-NLS-1$
        });
        putOption(OptionNoGet.class, new String[] {
            "noget" //$NON-NLS-1$
        });
        putOption(OptionNoIgnore.class, new String[] {
            "noignore" //$NON-NLS-1$
        });
        putOption(OptionNoImplicitBaseless.class, new String[] {
            "noimplicitbaseless" //$NON-NLS-1$
        });
        putOption(OptionNoMerge.class, new String[] {
            "nomerge" //$NON-NLS-1$
        });
        putOption(OptionNoPrompt.class, new String[] {
            "noprompt" //$NON-NLS-1$
        });
        putOption(OptionNoSummary.class, new String[] {
            "nosummary" //$NON-NLS-1$
        });
        putOption(OptionNotes.class, new String[] {
            "notes" //$NON-NLS-1$
        });
        putOption(OptionNumberProperty.class, new String[] {
            "number" //$NON-NLS-1$
        });
        putOption(OptionOutput.class, new String[] {
            "output" //$NON-NLS-1$
        });
        putOption(OptionOutputSeparator.class, new String[] {
            "outputSeparator" //$NON-NLS-1$
        });
        putOption(OptionOverwrite.class, new String[] {
            "overwrite", //$NON-NLS-1$
            "o" //$NON-NLS-1$
        });
        putOption(OptionOverride.class, new String[] {
            "override" //$NON-NLS-1$
        });
        putOption(OptionOverrideType.class, new String[] {
            "overridetype" //$NON-NLS-1$
        });
        putOption(OptionOwner.class, new String[] {
            "owner" //$NON-NLS-1$
        });
        putOption(OptionPermission.class, new String[] {
            "permission" //$NON-NLS-1$
        });
        putOption(OptionPreview.class, new String[] {
            "preview" //$NON-NLS-1$
        });
        putOption(OptionProxy.class, new String[] {
            "proxy" //$NON-NLS-1$
        });
        putOption(OptionRecursive.class, new String[] {
            "recursive", //$NON-NLS-1$
            "r" //$NON-NLS-1$
        });
        putOption(OptionRemove.class, new String[] {
            "remove" //$NON-NLS-1$
        });
        putOption(OptionReplace.class, new String[] {
            "replace" //$NON-NLS-1$
        });
        putOption(OptionResolve.class, new String[] {
            "resolve" //$NON-NLS-1$
        });
        putOption(OptionSaved.class, new String[] {
            "saved", //$NON-NLS-1$
        });
        putOption(OptionServer.class, new String[] {
            "server", //$NON-NLS-1$
            "repository", //$NON-NLS-1$
            "s" //$NON-NLS-1$
        });
        putOption(OptionSet.class, new String[] {
            "set" //$NON-NLS-1$
        });
        putOption(OptionSetValues.class, new String[] {
            "setvalues" //$NON-NLS-1$
        });
        putOption(OptionShelveset.class, new String[] {
            "shelveset" //$NON-NLS-1$
        });
        putOption(OptionSilent.class, new String[] {
            "silent" //$NON-NLS-1$
        });
        putOption(OptionSlotMode.class, new String[] {
            "slotmode" //$NON-NLS-1$
        });
        putOption(OptionStartCleanup.class, new String[] {
            "startcleanup" //$NON-NLS-1$
        });
        putOption(OptionStopAfter.class, new String[] {
            "stopafter", //$NON-NLS-1$
            "stop" //$NON-NLS-1$
        });
        putOption(OptionStopAt.class, new String[] {
            "stopat" //$NON-NLS-1$
        });
        putOption(OptionStringProperty.class, new String[] {
            "string" //$NON-NLS-1$
        });
        putOption(OptionTeamProject.class, new String[] {
            "teamProject" //$NON-NLS-1$
        });
        putOption(OptionTemplate.class, new String[] {
            "template" //$NON-NLS-1$
        });
        putOption(OptionToVersion.class, new String[] {
            "toversion" //$NON-NLS-1$
        });
        putOption(OptionTrial.class, new String[] {
            "trial" //$NON-NLS-1$
        });
        putOption(OptionType.class, new String[] {
            "type" //$NON-NLS-1$
        });
        putOption(OptionUnmap.class, new String[] {
            "unmap" //$NON-NLS-1$
        });
        putOption(OptionUpdateUserName.class, new String[] {
            "updateUserName" //$NON-NLS-1$
        });
        putOption(OptionUpdateComputerName.class, new String[] {
            "updateComputerName" //$NON-NLS-1$
        });
        putOption(OptionUser.class, new String[] {
            "user" //$NON-NLS-1$
        });
        putOption(OptionValidate.class, new String[] {
            "validate" //$NON-NLS-1$
        });
        putOption(OptionVersion.class, new String[] {
            "version" //$NON-NLS-1$
        });
        putOption(OptionWorkspace.class, new String[] {
            "workspace", //$NON-NLS-1$
            "w" //$NON-NLS-1$
        });
    }
}
