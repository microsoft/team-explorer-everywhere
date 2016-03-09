// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.KeepLocalWritableConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is a writable conflict, occurring when a local file is marked writable
 * but is not checked out.
 *
 * @since TEE-SDK-10.1
 */
public final class WritableConflictDescription extends ConflictDescription {
    protected WritableConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        super(workspace, conflict, conflictItemSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictCategory getConflictCategory() {
        return ConflictCategory.WRITABLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerPath() {
        final Conflict vcConflict = getConflict();

        /*
         * writable file semantics are a bit different. we want to display the
         * base server file, unless it doesn't exist, in which case we want to
         * display their server item.
         */
        if (vcConflict.getBaseServerItem() != null) {
            return vcConflict.getBaseServerItem();
        } else if (vcConflict.getYourServerItem() != null) {
            return vcConflict.getYourServerItem();
        } else {
            return vcConflict.getTheirServerItem();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("WritableConflictDescription.Name"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        final Conflict conflict = getConflict();

        if (conflict != null) {
            // normal writable case
            if (conflict.getBaseServerItem() != null) {
                return Messages.getString("WritableConflictDescription.DescriptionFileBySameNameExists"); //$NON-NLS-1$
            } else if (conflict.getReason() == OperationStatus.TARGET_WRITABLE.getValue()) {
                return MessageFormat.format(
                    Messages.getString("WritableConflictDescription.DescriptionLocalFileIsWritableFormat"), //$NON-NLS-1$
                    conflict.getTargetLocalItem());
            } else if (conflict.getReason() == OperationStatus.SOURCE_WRITABLE.getValue()) {
                return MessageFormat.format(
                    Messages.getString("WritableConflictDescription.DescriptionLocalFileIsWritableFormat"), //$NON-NLS-1$
                    conflict.getSourceLocalItem());
            }
        }

        return Messages.getString("WritableConflictDescription.DescriptionLocalFileIsWritable"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor) {
        final List<ConflictResolution> resolutions = new ArrayList<ConflictResolution>();

        resolutions.add(
            new CoreConflictResolution(
                this,
                ConflictDescriptionStrings.OVERWRITE,
                ConflictDescriptionStrings.OVERWRITE_TOOLTIP,
                ConflictResolutionOptions.NONE,
                Resolution.OVERWRITE_LOCAL));

        final Conflict conflict = getConflict();

        if (conflict != null && conflict.getReason() == OperationStatus.TARGET_WRITABLE.getValue()) {
            resolutions.add(
                new KeepLocalWritableConflictResolution(
                    this,
                    ConflictDescriptionStrings.KEEP_LOCAL,
                    ConflictDescriptionStrings.KEEP_LOCAL_TOOLTIP));
        }

        return resolutions.toArray(new ConflictResolution[resolutions.size()]);
    }
}