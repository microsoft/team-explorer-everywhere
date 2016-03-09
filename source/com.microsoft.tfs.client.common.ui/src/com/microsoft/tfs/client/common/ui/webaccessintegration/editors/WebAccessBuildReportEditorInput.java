// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.server.TFSServer;

public class WebAccessBuildReportEditorInput implements IEditorInput {
    private final String url;
    private final String buildNumber;
    private final TFSServer server;

    public WebAccessBuildReportEditorInput(final TFSServer server, final String url, final String buildNumber) {
        this.url = url;
        this.buildNumber = buildNumber;
        this.server = server;
    }

    public String getURL() {
        return url;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public TFSServer getServer() {
        return server;
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getName() {
        return "name"; //$NON-NLS-1$
    }

    @Override
    public String getToolTipText() {
        return "tooltip"; //$NON-NLS-1$
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WebAccessBuildReportEditorInput) {
            final WebAccessBuildReportEditorInput other = (WebAccessBuildReportEditorInput) obj;
            return url.equals(other.url);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
