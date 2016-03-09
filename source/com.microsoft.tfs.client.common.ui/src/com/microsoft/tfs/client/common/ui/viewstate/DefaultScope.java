// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.viewstate;

import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

public class DefaultScope implements ViewStateScope {
    private final String rootNodeName;

    public DefaultScope(final String rootNodeName) {
        this.rootNodeName = rootNodeName;
    }

    @Override
    public Preferences getNestedPreferences(final Preferences startingNode) {
        return startingNode.node(rootNodeName);
    }

    @Override
    public IScopeContext getEclipsePreferencesScope() {
        return new InstanceScope();
    }
}
