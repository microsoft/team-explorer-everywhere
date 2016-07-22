// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import org.eclipse.swt.graphics.Image;

import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionReference;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionType;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public class TeamBuildImageHelper extends ImageHelper {

    public TeamBuildImageHelper() {
        super(TFSTeamBuildPlugin.PLUGIN_ID);
    }

    public Image getKeepForeverImage(final boolean isKeepForever) {
        return isKeepForever ? getImage("/icons/KeepForever.gif") : null; //$NON-NLS-1$
    }

    public Image getStatusImage(final QueueStatus status) {
        if (status == null) {
            return null;
        }
        if (status.contains(QueueStatus.QUEUED)) {
            return getImage("/icons/QueueStatusQueued.gif"); //$NON-NLS-1$
        }
        if (status.contains(QueueStatus.IN_PROGRESS)) {
            return getImage("/icons/QueueStatusInProgress.gif"); //$NON-NLS-1$
        }
        if (status.contains(QueueStatus.POSTPONED)) {
            return getImage("/icons/QueueStatusPostponed.gif"); //$NON-NLS-1$
        }
        if (status.contains(QueueStatus.CANCELED)) {
            return getImage("/icons/QueueStatusCanceled.gif"); //$NON-NLS-1$
        }
        return null;
    }

    public Image getStatusImage(final BuildStatus status) {
        if (status == null) {
            return null;
        }

        if (status.contains(BuildStatus.IN_PROGRESS)) {
            return getImage("/icons/BuildStatusInProgress.gif"); //$NON-NLS-1$
        }
        if (status.contains(BuildStatus.SUCCEEDED)) {
            return getImage("/icons/BuildStatusSucceeded.gif"); //$NON-NLS-1$
        }
        if (status.contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
            return getImage("/icons/BuildStatusPartial.gif"); //$NON-NLS-1$
        }
        if (status.contains(BuildStatus.FAILED)) {
            return getImage("/icons/BuildStatusFailed.gif"); //$NON-NLS-1$
        }
        if (status.contains(BuildStatus.STOPPED)) {
            return getImage("/icons/BuildStatusStopped.gif"); //$NON-NLS-1$
        }
        if (status.contains(BuildStatus.NOT_STARTED)) {
            return getImage("/icons/BuildStatusNotStarted.gif"); //$NON-NLS-1$
        }
        return null;
    }

    public Image getBuildReasonImage(final BuildReason reason) {
        if (reason.contains(BuildReason.CHECK_IN_SHELVESET)) {
            return getImage("/icons/BuildReasonGated.gif"); //$NON-NLS-1$
        }
        if (reason.contains(BuildReason.VALIDATE_SHELVESET)) {
            return getImage("/icons/BuildReasonValidate.gif"); //$NON-NLS-1$
        }
        if (reason.contains(BuildReason.INDIVIDUAL_CI)) {
            return getImage("/icons/BuildReasonIndividualCI.gif"); //$NON-NLS-1$
        }
        if (reason.contains(BuildReason.BATCHED_CI)) {
            return getImage("/icons/BuildReasonBatchedCI.gif"); //$NON-NLS-1$
        }
        if (reason.contains(BuildReason.SCHEDULE) || reason.contains(BuildReason.SCHEDULE_FORCED)) {
            return getImage("/icons/BuildReasonScheduled.gif"); //$NON-NLS-1$
        }
        return null;
    }

    public Image getBuildDefinitionImage(final IBuildDefinition definition) {
        if (definition == null) {
            return null;
        }

        if (definition.isEnabled()) {
            return getImage("icons/BuildType.gif"); //$NON-NLS-1$
        } else {
            return getImage("icons/BuildTypeDisabled.gif"); //$NON-NLS-1$
        }
    }

    public Image getBuildDefinitionImage(final BuildDefinitionReference definition) {
        if (definition == null) {
            return null;
        }

        if (definition.getType() == DefinitionType.BUILD) {
            return getImage("icons/vNextBuildDefinition.png"); //$NON-NLS-1$
        } else {
            return getImage("icons/BuildType.gif"); //$NON-NLS-1$
        }
    }
}
