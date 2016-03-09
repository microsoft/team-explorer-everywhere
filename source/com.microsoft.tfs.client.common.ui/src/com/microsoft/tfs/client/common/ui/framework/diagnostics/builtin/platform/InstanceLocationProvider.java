// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocationProvider;

public class InstanceLocationProvider extends LocationProvider {
    @Override
    protected Location getLocation() {
        return Platform.getInstanceLocation();
    }
}
