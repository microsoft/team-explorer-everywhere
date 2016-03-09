// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.soapextensions.Agent2008Status;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.util.StringUtil;

public class Build2008Helper extends TFS2008Helper {
    public Build2008Helper(final BuildServer buildServer) {
        super(buildServer);
    }

    public static ControllerStatus convert(final Agent2008Status status) {
        if (status.equals(Agent2008Status.ENABLED)) {
            return ControllerStatus.AVAILABLE;
        } else {
            return ControllerStatus.UNAVAILABLE;
        }
    }

    public static BuildAgentSpec2008 convert(final BuildControllerSpec spec) {
        return convert(spec, StringUtil.EMPTY);
    }
}
