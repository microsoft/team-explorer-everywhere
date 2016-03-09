// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;

public interface IRetentionPolicy {
    /**
     * The build definition that owns the retention policy.
     *
     *
     * @return
     */
    public IBuildDefinition getBuildDefinition();

    /**
     * The reason(s) of builds to which the retention policy applies.
     *
     *
     * @return
     */
    public BuildReason getBuildReason();

    public void setBuildReason(BuildReason value);

    /**
     * The status(es) of builds to which the retention policy applies.
     *
     *
     * @return
     */
    public BuildStatus getBuildStatus();

    public void setBuildStatus(BuildStatus value);

    /**
     * The number of builds to keep.
     *
     *
     * @return
     */
    public int getNumberToKeep();

    public void setNumberToKeep(int value);

    /**
     * The parts of the build to delete.
     *
     *
     * @return
     */
    public DeleteOptions getDeleteOptions();

    public void setDeleteOptions(DeleteOptions value);
}
