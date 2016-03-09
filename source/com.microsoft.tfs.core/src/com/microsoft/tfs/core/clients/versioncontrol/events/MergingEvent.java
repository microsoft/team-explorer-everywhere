// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;

/**
 * Event fired when a file is being merged.
 *
 * @since TEE-SDK-10.1
 */
public class MergingEvent extends CoreClientEvent {
    private final Conflict conflict;
    private final Workspace workspace;
    private final boolean isLatest;
    private final PendingChange pendingChange;
    private final OperationStatus status;
    private final ChangeType targetLocalChangeType;
    private final boolean diskUpdateAttempted;
    private final PropertyValue[] properties;

    public MergingEvent(
        final EventSource source,
        final Conflict conflict,
        final Workspace workspace,
        final boolean isLatest,
        final PendingChange pendingChange,
        final OperationStatus status,
        final ChangeType targetLocalChangeType,
        final boolean diskUpdateAttempted,
        final PropertyValue[] properties) {
        super(source);

        this.conflict = conflict;
        this.workspace = workspace;
        this.isLatest = isLatest;
        this.pendingChange = pendingChange;
        this.status = status;
        this.targetLocalChangeType = targetLocalChangeType;
        this.diskUpdateAttempted = diskUpdateAttempted;
        this.properties = properties;
    }

    public boolean isDiskUpdateAttempted() {
        return diskUpdateAttempted;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public PendingChange getPendingChange() {
        return pendingChange;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public ChangeType getTargetLocalPendingChange() {
        return targetLocalChangeType;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public ChangeType getChangeType() {
        return conflict.getBaseChangeType();
    }

    public String getSourceServerItem() {
        return conflict.getTheirServerItem();
    }

    public String getSourceLocalItem() {
        return conflict.getSourceLocalItem();
    }

    public ChangesetVersionSpec getSourceVersionFrom() {
        return new ChangesetVersionSpec(conflict.getBaseVersion());
    }

    public ChangesetVersionSpec getSourceVersionTo() {
        return new ChangesetVersionSpec(conflict.getTheirVersion());
    }

    public String getTargetServerItem() {
        return conflict.getYourServerItem();
    }

    public String getTargetLocalItem() {
        return conflict.getTargetLocalItem();
    }

    public ChangesetVersionSpec getTargetVersionPended() {
        return new ChangesetVersionSpec(conflict.getYourVersion());
    }

    public boolean isConflict() {
        return conflict.getResolution() == Resolution.NONE;
    }

    @Override
    public String toString() {
        String source;
        String target;
        String message;

        final ChangeType changeType = getChangeType();
        final OperationStatus status = getStatus();

        if (changeType.contains(ChangeType.MERGE) == false) {
            /*
             * Non-merge changes have no server items. Things like rename cause
             * this case, and the target file just needs to be moved because the
             * parent moved on the source side.
             */
            source = null;
        } else if (changeType.contains(ChangeType.BRANCH)) {
            /*
             * Branches will have no meaningful from version.
             */
            source = VersionedFileSpec.formatForPath(getSourceServerItem(), getSourceVersionTo());
        } else {
            /*
             * We can use both versions.
             */
            source =
                VersionedFileSpec.formatForPath(getSourceServerItem(), getSourceVersionFrom(), getSourceVersionTo());
        }

        if (changeType.contains(ChangeType.BRANCH)) {
            /*
             * Branches also have no meaningful to (or "target pended") version.
             */
            target = getTargetServerItem();
        } else {
            target = VersionedFileSpec.formatForPath(getTargetServerItem(), getTargetVersionPended());
        }

        if (status == OperationStatus.CONFLICT) {
            // TODO get correct string
            message =
                MessageFormat.format(
                    Messages.getString("MergingEvent.MergeConflictOfTypeBetweenSourceAndTargetFormat"), //$NON-NLS-1$
                    changeType.toUIString(false),
                    source,
                    target);
        } else if (source == null) {
            // TODO get correct string
            message = MessageFormat.format(
                Messages.getString("MergingEvent.MergeOfAssociatedPendingChangeTypeForTargetFormat"), //$NON-NLS-1$
                changeType.toUIString(false),
                target);
        } else {
            // TODO get correct string
            message =
                MessageFormat.format(
                    Messages.getString("MergingEvent.MergePendedForChangeTypeForSourceToTargetFormat"), //$NON-NLS-1$
                    changeType.toUIString(false),
                    source,
                    target);
        }

        return message;
    }

    /**
     * Converts this event into a displayable message.
     *
     * @param error
     *        the error message, if any, that is also a result of this event
     * @return the displayable message for pending a merge
     */
    public String getMessage(final AtomicReference<String> error) {
        error.set(null);

        if (status == OperationStatus.GETTING
            || status == OperationStatus.REPLACING
            || status == OperationStatus.DELETING
            || status == OperationStatus.CONFLICT) {
            // Nothing to do.
        } else if (status == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY) {
            error.set(MessageFormat.format(
                Messages.getString("MergingEvent.CantDeleteNonEmptyDirPathFormat"), //$NON-NLS-1$
                getSourceLocalItem()));
        } else if (status == OperationStatus.SOURCE_WRITABLE) {
            error.set(MessageFormat.format(
                Messages.getString("MergingEvent.FileIsWritablePathFormat"), //$NON-NLS-1$
                getSourceLocalItem()));
        } else if (status == OperationStatus.TARGET_IS_DIRECTORY) {
            error.set(MessageFormat.format(
                Messages.getString("MergingEvent.FoundDirectoryExpectedFilePathFormat"), //$NON-NLS-1$
                getTargetLocalItem()));
        } else if (status == OperationStatus.TARGET_LOCAL_PENDING) {
            error.set(MessageFormat.format(
                Messages.getString("MergingEvent.TargetLocalPendingPathFormat"), //$NON-NLS-1$
                getSourceLocalItem(),
                getTargetLocalItem(),
                getTargetLocalPendingChange().toUIString(false)));
        } else if (status == OperationStatus.TARGET_WRITABLE) {
            error.set(MessageFormat.format(
                Messages.getString("MergingEvent.FileIsWritablePathFormat"), //$NON-NLS-1$
                getTargetLocalItem()));
        }

        return conflict.getDetailedMessage(getStatus() == OperationStatus.CONFLICT);
    }

    public PropertyValue[] getPropertyValues() {
        return this.properties;
    }
}
