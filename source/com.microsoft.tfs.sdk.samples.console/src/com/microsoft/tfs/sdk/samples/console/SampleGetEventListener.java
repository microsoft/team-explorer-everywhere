// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.console;

import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;

public class SampleGetEventListener implements GetListener {
    @Override
    public void onGet(final GetEvent e) {
        final String item = e.getTargetLocalItem() != null ? e.getTargetLocalItem() : e.getServerItem();

        System.out.println("getting: " + item); //$NON-NLS-1$
    }
}
