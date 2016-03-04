// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi;

import com.microsoft.tfs.core.TFSTeamProjectCollection;

public class TaskAgentHttpClient extends TaskAgentHttpClientBase {

    public TaskAgentHttpClient(final TFSTeamProjectCollection connection) {
        super(connection);
    }
}
