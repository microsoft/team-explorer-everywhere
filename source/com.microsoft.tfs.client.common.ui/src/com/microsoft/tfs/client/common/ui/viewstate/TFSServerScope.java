// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.viewstate;

import java.net.URI;

import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

public class TFSServerScope implements ViewStateScope {
    private final String rootNodeName;
    private final URI serverURI;

    public TFSServerScope(final String rootNodeName, final TFSServer server) {
        this(rootNodeName, server.getConnection());
    }

    public TFSServerScope(final String rootNodeName, final TFSTeamProjectCollection connection) {
        this.rootNodeName = rootNodeName;
        this.serverURI = connection.getBaseURI();
    }

    @Override
    public Preferences getNestedPreferences(final Preferences startingNode) {
        return startingNode.node(rootNodeName).node(serverURI.toString());
    }

    @Override
    public IScopeContext getEclipsePreferencesScope() {
        return new InstanceScope();
    }

}
