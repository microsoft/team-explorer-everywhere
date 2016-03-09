// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.util.ServerURIUtils;

public class BuildReportEditorInput implements IEditorInput {

    private final IBuildServer buildServer;
    private final String buildUri;
    private final String buildName;

    public BuildReportEditorInput(final IBuildServer buildServer, final String buildUri, final String buildName) {
        super();
        this.buildServer = buildServer;
        this.buildUri = buildUri;
        this.buildName = buildName;
    }

    /**
     * @return the buildServer
     */
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    /**
     * @return the buildUri
     */
    public String getBuildURI() {
        return buildUri;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((buildServer == null) ? 0 : buildServer.hashCode());
        result = prime * result + ((buildUri == null) ? 0 : buildUri.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BuildReportEditorInput)) {
            return false;
        }
        final BuildReportEditorInput other = (BuildReportEditorInput) obj;
        if (buildServer == null) {
            if (other.buildServer != null) {
                return false;
            }
        } else if (!ServerURIUtils.equals(
            buildServer.getConnection().getBaseURI(),
            other.buildServer.getConnection().getBaseURI())) {
            return false;
        }
        if (buildUri == null) {
            if (other.buildUri != null) {
                return false;
            }
        } else if (!buildUri.equals(other.buildUri)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.getMissingImageDescriptor();
    }

    @Override
    public String getName() {
        return buildName;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return buildName;
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

}
