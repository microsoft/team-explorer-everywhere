// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

import java.io.File;
import java.net.URL;

import org.eclipse.osgi.service.datalocation.Location;

public abstract class LocationProvider extends NonLocalizedDataProvider
    implements DataProvider, AvailableCallback, PopulateCallback {
    protected abstract Location getLocation();

    private File directory;

    @Override
    public final void populate() throws Exception {
        directory = null;

        final Location location = getLocation();
        if (location != null) {
            final URL url = location.getURL();
            if (url != null) {
                final String locationURLString = location.getURL().toExternalForm();
                if (locationURLString.startsWith("file:/")) //$NON-NLS-1$
                {
                    /*
                     * Under windows, the URL string looks like:
                     * file:/C:/home/user/mydir Under linux, it's
                     * file:/home/user/mydir We leave the first forward slash
                     * in, which parses properly as a file under both windows
                     * and linux
                     */
                    directory = new File(locationURLString.substring(5));
                }
            }
        }
    }

    @Override
    public final boolean isAvailable() {
        return directory != null;
    }

    @Override
    public final Object getData() {
        return directory;
    };
}
