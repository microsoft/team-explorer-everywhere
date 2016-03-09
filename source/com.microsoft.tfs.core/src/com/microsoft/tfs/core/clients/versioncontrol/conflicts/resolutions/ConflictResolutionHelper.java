// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;

public final class ConflictResolutionHelper {
    private ConflictResolutionHelper() {
    }

    public static String getResolutionString(final Resolution resolution) {
        if (Resolution.NONE.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringNone"); //$NON-NLS-1$
        } else if (Resolution.ACCEPT_MERGE.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringAcceptMerge"); //$NON-NLS-1$
        } else if (Resolution.ACCEPT_YOURS.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringAcceptYours"); //$NON-NLS-1$
        } else if (Resolution.ACCEPT_THEIRS.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringAcceptTheirs"); //$NON-NLS-1$
        } else if (Resolution.DELETE_CONFLICT.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringDeleteConflict"); //$NON-NLS-1$
        } else if (Resolution.ACCEPT_YOURS_RENAME_THEIRS.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringAcceptYoursRenameTheirs"); //$NON-NLS-1$
        } else if (Resolution.OVERWRITE_LOCAL.equals(resolution)) {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringOverwriteLocal"); //$NON-NLS-1$
        } else {
            return Messages.getString("ConflictResolutionHelper.ResolutionStringUnknown"); //$NON-NLS-1$
        }
    }
}
