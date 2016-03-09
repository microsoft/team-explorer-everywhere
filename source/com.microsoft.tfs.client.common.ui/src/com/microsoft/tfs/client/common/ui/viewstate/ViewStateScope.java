// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.viewstate;

import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.Preferences;

public interface ViewStateScope {
    public Preferences getNestedPreferences(Preferences startingNode);

    public IScopeContext getEclipsePreferencesScope();
}
