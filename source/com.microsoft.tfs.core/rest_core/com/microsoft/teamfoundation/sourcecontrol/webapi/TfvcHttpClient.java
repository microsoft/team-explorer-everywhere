// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.sourcecontrol.webapi;

import com.microsoft.tfs.core.TFSTeamProjectCollection;

public class TfvcHttpClient extends TfvcHttpClientBase {

    public TfvcHttpClient(final TFSTeamProjectCollection connection) {
        super(connection);
    }

}
