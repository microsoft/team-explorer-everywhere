// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.core.Messages;

/**
 * @since TEE-SDK-10.1
 */
public class ConflictDescriptionStrings {
    public final static String VERSION_ACCEPT_THEIRS =
        Messages.getString("ConflictDescriptionStrings.VersionAcceptTheirs"); //$NON-NLS-1$
    public final static String VERSION_ACCEPT_THEIRS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.VersionAcceptTheirsTooltip"); //$NON-NLS-1$

    public final static String VERSION_ACCEPT_YOURS =
        Messages.getString("ConflictDescriptionStrings.VersionAcceptYours"); //$NON-NLS-1$
    public final static String VERSION_ACCEPT_YOURS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.VersionAcceptYoursTooltip"); //$NON-NLS-1$

    public final static String ROLLBACK_LOCAL_ACCEPT_THEIRS =
        Messages.getString("ConflictDescriptionStrings.RollbackLocalAcceptTheirs"); //$NON-NLS-1$
    public final static String ROLLBACK_LOCAL_ACCEPT_THEIRS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.RollbackLocalAcceptTheirsTooltip"); //$NON-NLS-1$

    public final static String ROLLBACK_LOCAL_ACCEPT_YOURS =
        Messages.getString("ConflictDescriptionStrings.RollbackLocalAcceptYours"); //$NON-NLS-1$
    public final static String ROLLBACK_LOCAL_ACCEPT_YOURS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.RollbackLocalAcceptYoursTooltip"); //$NON-NLS-1$

    public final static String MERGE_ACCEPT_THEIRS = Messages.getString("ConflictDescriptionStrings.MergeAcceptTheirs"); //$NON-NLS-1$
    public final static String MERGE_ACCEPT_THEIRS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.MergeAcceptTheirsTooltip"); //$NON-NLS-1$

    public final static String MERGE_ACCEPT_YOURS = Messages.getString("ConflictDescriptionStrings.MergeAcceptYours"); //$NON-NLS-1$
    public final static String MERGE_ACCEPT_YOURS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.MergeAcceptYoursTooltip"); //$NON-NLS-1$

    public final static String VERSION_RENAME_SERVER =
        Messages.getString("ConflictDescriptionStrings.VersionRenameServer"); //$NON-NLS-1$
    public final static String VERSION_RENAME_SERVER_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.VersionRenameServerTooltip"); //$NON-NLS-1$

    public final static String VERSION_RENAME_LOCAL =
        Messages.getString("ConflictDescriptionStrings.VersionRenameLocal"); //$NON-NLS-1$
    public final static String VERSION_RENAME_LOCAL_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.VersionRenameLocalTooltip"); //$NON-NLS-1$

    public final static String AUTOMERGE = Messages.getString("ConflictDescriptionStrings.Automerge"); //$NON-NLS-1$
    public final static String AUTOMERGE_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.AutomergeTooltipFormat"); //$NON-NLS-1$

    public final static String RENAME_AND_AUTOMERGE =
        Messages.getString("ConflictDescriptionStrings.RenameAndAutomerge"); //$NON-NLS-1$
    public final static String RENAME_AND_AUTOMERGE_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.RenameAndAutomergeTooltip"); //$NON-NLS-1$

    public static final String RENAME_ENCODING_AND_AUTOMERGE =
        Messages.getString("ConflictDescriptionStrings.RenameEncodingAndAutomerge"); //$NON-NLS-1$
    public static final String RENAME_ENCODING_AND_AUTOMERGE_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.RenameEncodingAndAutomergeTooltip"); //$NON-NLS-1$

    public final static String RENAME = Messages.getString("ConflictDescriptionStrings.Rename"); //$NON-NLS-1$
    public final static String RENAME_TOOLTIP = Messages.getString("ConflictDescriptionStrings.RenameTooltip"); //$NON-NLS-1$

    public final static String SELECT_ENCODING_AND_AUTOMERGE =
        Messages.getString("ConflictDescriptionStrings.SelectEncodingAndAutomerge"); //$NON-NLS-1$
    public final static String SELECT_ENCODING_AND_AUTOMERGE_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.SelectEncodingAndAutomergeTooltip"); //$NON-NLS-1$

    public final static String WRITABLE_AUTOMERGE = Messages.getString("ConflictDescriptionStrings.WritableAutomerge"); //$NON-NLS-1$
    public final static String WRITABLE_AUTOMERGE_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.WritableAutomergeTooltip"); //$NON-NLS-1$

    public final static String OVERWRITE = Messages.getString("ConflictDescriptionStrings.Overwrite"); //$NON-NLS-1$
    public final static String OVERWRITE_TOOLTIP = Messages.getString("ConflictDescriptionStrings.OverwriteTooltip"); //$NON-NLS-1$

    public final static String KEEP_LOCAL = Messages.getString("ConflictDescriptionStrings.KeepLocalContents"); //$NON-NLS-1$
    public final static String KEEP_LOCAL_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.KeepLocalContentsTooltip"); //$NON-NLS-1$

    /* Merge target deleted */
    public final static String MERGE_TARGET_DELETED_AUTOMERGE =
        Messages.getString("ConflictDescriptionStrings.MergeTargetDeletedAutomerge"); //$NON-NLS-1$
    public final static String MERGE_TARGET_DELETED_AUTOMERGE_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.MergeTargetDeletedAutomergeTooltip"); //$NON-NLS-1$

    public final static String MERGE_TARGET_DELETED_MERGE_IN_EXTERNAL_EDITOR =
        Messages.getString("ConflictDescriptionStrings.MergeTargetDeletedMergeInExternalEditor"); //$NON-NLS-1$
    public final static String MERGE_TARGET_DELETED_MERGE_IN_EXTERNAL_EDITOR_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.MergeTargetDeletedMergeInExternalEditorTooltip"); //$NON-NLS-1$

    /* Version control deleted */
    public final static String VERSION_DELETED_ACCEPT_YOURS =
        Messages.getString("ConflictDescriptionStrings.VersionDeletedAcceptYours"); //$NON-NLS-1$
    public final static String VERSION_DELETED_ACCEPT_YOURS_TOOLTIP =
        Messages.getString("ConflictDescriptionStrings.VersionDeletedAcceptYoursTooltip"); //$NON-NLS-1$

    /*
     * Content summaries (eg, "1 local, 2 server, 3 common, 4 conflicts")
     */
    public final static String SUMMARY_BOTH_CHANGED =
        Messages.getString("ConflictDescriptionStrings.SummaryBothChangedFormat"); //$NON-NLS-1$
    public final static String BASELESS_MERGE_CONFLICT =
        Messages.getString("ConflictDescriptionStrings.BaselessMergeConflict"); //$NON-NLS-1$
    public final static String SUMMARY_CONTENT_CONFLICT =
        Messages.getString("ConflictDescriptionStrings.SummaryContentConflictFormat"); //$NON-NLS-1$
    public final static String SUMMARY_CONTENT_AND_ENCODING_CHANGED =
        Messages.getString("ConflictDescriptionStrings.SummaryContentAndEncoding"); //$NON-NLS-1$
    public final static String SUMMARY_RENAMED = Messages.getString("ConflictDescriptionStrings.SummaryRenamed"); //$NON-NLS-1$
    public final static String SUMMARY_BINARY = Messages.getString("ConflictDescriptionStrings.SummaryBinary"); //$NON-NLS-1$
    public final static String SUMMARY_NO_MERGE_AVAILABLE =
        Messages.getString("ConflictDescriptionStrings.SummaryNoMergeAvailable"); //$NON-NLS-1$
    public final static String SUMMARY_UNMERGEABLE =
        Messages.getString("ConflictDescriptionStrings.SummaryUnmergeable"); //$NON-NLS-1$

    private ConflictDescriptionStrings() {
    }
}
