// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ISelectionProvider;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.action.SelectionProviderAction;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.jni.FileSystemUtils;

public abstract class RepositoryAction extends SelectionProviderAction {
    private TFSRepository repository;

    protected RepositoryAction(final ISelectionProvider selectionProvider, final TFSRepository repository) {
        super(selectionProvider);
        this.repository = repository;
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;

        /*
         * When there is no repository, disable these actions. Standard
         * selection events (or other mechanisms) can re-enable the action
         * later.
         */
        if (repository == null) {
            setEnabled(false);
        }
    }

    @Override
    public final void doRun() {
        if (repository == null) {
            final String messageFormat = "repository not set on action [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getClass().getName());
            throw new IllegalStateException(message);
        }

        ClientTelemetryHelper.sendRunActionEvent(this);

        doRun(repository);
    }

    protected final TFSRepository getRepository() {
        if (repository == null) {
            final String messageFormat = "repository not set on action [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getClass().getName());
            throw new IllegalStateException(message);
        }

        return repository;
    }

    protected abstract void doRun(TFSRepository repository);

    public boolean containsSymlinkChange(final Change change) {
        if (change == null || change.getItem() == null) {
            return false;
        }

        return PropertyConstants.IS_SYMLINK.equals(
            PropertyUtils.selectMatching(change.getItem().getPropertyValues(), PropertyConstants.SYMBOLIC_KEY));
    }

    public boolean isSymbolicLink(final String path) {
        if (path == null) {
            return false;
        }

        return FileSystemUtils.getInstance().getAttributes(path).isSymbolicLink();
    }
}
