// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.editors;

import java.text.MessageFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;

public class BuildExplorerEditorInput implements IEditorInput {
    private final IBuildDefinition buildDefinition;
    private TFSServer tfsServer;
    private final Object serverLock = new Object();

    public BuildExplorerEditorInput(final TFSServer server, final IBuildDefinition buildDefinition) {
        tfsServer = server;
        this.buildDefinition = buildDefinition;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    @Override
    public boolean exists() {
        return false;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.getMissingImageDescriptor();
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    @Override
    public String getName() {
        String name = buildDefinition.getName();
        if (name == null || name.equals(BuildPath.RECURSION_OPERATOR)) {
            name = buildDefinition.getTeamProject();
        }

        final String messageFormat = Messages.getString("BuildExplorerEditorInput.InputNameFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, name);
        return message;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /**
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        final String messageFormat = Messages.getString("BuildExplorerEditorInput.InputToolTipFormat"); //$NON-NLS-1$
        final String message =
            MessageFormat.format(messageFormat, buildDefinition.getTeamProject(), buildDefinition.getName());
        return message;
    }

    /**
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    /**
     * @return the buildDefinition
     */
    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return BuildExplorerEditorInput.class.hashCode();
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
        if (!(obj instanceof BuildExplorerEditorInput)) {
            return false;
        }

        return true;
    }

    public TFSServer getServer() {
        return tfsServer;
    }

    public void setServer(final TFSServer server) {
        synchronized (serverLock) {
            tfsServer = server;
        }
    }

}
