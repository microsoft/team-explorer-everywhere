// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.prefs;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
    @Override
    public void initializeDefaultPreferences() {
        // TODO use a non-deprecated API for preferences at the non-UI client
        // layer

        final Preferences prefs = TFSCommonClientPlugin.getDefault().getPluginPreferences();

        prefs.setDefault(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL, 60000);
    }
}
