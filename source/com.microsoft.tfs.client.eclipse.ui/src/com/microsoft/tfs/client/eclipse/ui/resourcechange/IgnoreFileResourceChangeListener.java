// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.resourcechange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreDocument;
import com.microsoft.tfs.client.eclipse.ui.decorators.TFSLabelDecorator;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalItemExclusionEvaluator;

/**
 * Listens for changes to .tpipgnore and .tfignore resources and refreshes label
 * decorations to reflect the new contents.
 */
public class IgnoreFileResourceChangeListener implements IResourceChangeListener {
    private static final Path TPIGNORE_PROJECT_RELATIVE_PATH = new Path(TPIgnoreDocument.DEFAULT_FILENAME);

    private static final Log log = LogFactory.getLog(IgnoreFileResourceChangeListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        /*
         * See TFSResourceChangeListener for why we do this check.
         */
        if (!TFSEclipseClientPlugin.getDefault().getProjectManager().isStarted()) {
            log.warn("Resource change event called before workbench has started, ignoring resource changes"); //$NON-NLS-1$
            return;
        }

        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(final IResourceDelta delta) throws CoreException {
                    final IResource resource = delta.getResource();

                    if (resource.getType() == IResource.FILE) {
                        if (resource.getProjectRelativePath().equals(TPIGNORE_PROJECT_RELATIVE_PATH)) {
                            TFSLabelDecorator.refreshTFSLabelDecorator();
                            return false;
                        }

                        if (resource.getName().equals(LocalItemExclusionEvaluator.IGNORE_FILE_NAME)) {
                            // Must clear the cached evaluators before
                            // redecorating
                            PluginResourceFilters.TFS_IGNORE_FILTER.clearCachedEvaluators();
                            TFSLabelDecorator.refreshTFSLabelDecorator();
                            return false;
                        }
                    }

                    // Continue visiting children
                    return true;
                }
            });
        } catch (final CoreException e) {
            TFSEclipseClientPlugin.getDefault().getLog().log(e.getStatus());
            return;
        }
    }

}
