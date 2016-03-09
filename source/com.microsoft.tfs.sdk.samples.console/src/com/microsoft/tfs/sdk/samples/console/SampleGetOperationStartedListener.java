// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import com.microsoft.tfs.core.clients.versioncontrol.events.GetOperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;

public class SampleGetOperationStartedListener implements OperationStartedListener {
    public void onGetOperationStarted(final GetOperationStartedEvent e) {
        for (final GetRequest request : e.getRequests()) {
            if (request.getItemSpec() != null) {
                System.out.println("Started getting: " + request.getItemSpec().toString()); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void onOperationStarted(final OperationStartedEvent e) {
        if (e instanceof GetOperationStartedEvent) {
            onGetOperationStarted((GetOperationStartedEvent) e);
        }
    }
}
