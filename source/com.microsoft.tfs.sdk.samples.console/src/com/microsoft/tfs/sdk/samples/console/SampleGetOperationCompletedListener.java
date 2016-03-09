// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import com.microsoft.tfs.core.clients.versioncontrol.events.GetOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;

public class SampleGetOperationCompletedListener implements OperationCompletedListener {
    public void onGetOperationCompleted(final GetOperationCompletedEvent e) {
        for (final GetRequest request : e.getRequests()) {
            if (request.getItemSpec() != null) {
                System.out.println("Completed getting: " + request.getItemSpec().toString()); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void onOperationCompleted(final OperationCompletedEvent e) {
        if (e instanceof GetOperationCompletedEvent) {
            onGetOperationCompleted((GetOperationCompletedEvent) e);
        }
    }
}
