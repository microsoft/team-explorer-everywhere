// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

/**
 * Base class for the specific compare actions.
 *
 * @threadsafety thread-compatible
 */
public abstract class CompareAction extends ExtendedAction {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (ActionHelpers.linkSelected(selection)) {
            action.setEnabled(false);
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        /*
         * Use the standard filter instead of the "in repository" filter,
         * because the resource may be a pending add.
         */
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.STANDARD_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource resource = selectionInfo.getResources()[0];
        final TFSRepository repository = selectionInfo.getRepositories()[0];

        final boolean shouldContinue = prepare(resource, repository);

        if (!shouldContinue) {
            return;
        }

        final Compare compare = new Compare();

        compare.setModified(getModifiedCompareElement(resource, repository));

        compare.setOriginal(getOriginalCompareElement(resource, repository));

        compare.setAncestor(getAncestorCompareElement(resource, repository));

        compare.addComparator(TFSItemContentComparator.INSTANCE);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(getShell()));
        compare.open();
    }

    /**
     * Prepare this compare action. This is the place to show a dialog for user
     * input if needed.
     *
     * @param resource
     *        the resource that is selected for this compare action
     * @param repository
     *        the repository that the resource corresponds to
     * @return <code>true</code> to continue, or <code>false</code> if the
     *         compare action is canceled
     */
    protected abstract boolean prepare(IResource resource, TFSRepository repository);

    /**
     * @param resource
     *        the resource that is selected for this compare action
     * @param repository
     *        the repository that the resource corresponds to
     * @return the left compare element to use
     */
    protected abstract Object getModifiedCompareElement(IResource resource, TFSRepository repository);

    /**
     * @param resource
     *        the resource that is selected for this compare action
     * @param repository
     *        the repository that the resource corresponds to
     * @return the right compare element to use
     */
    protected abstract Object getOriginalCompareElement(IResource resource, TFSRepository repository);

    /**
     * @param resource
     *        the resource that is selected for this compare action
     * @param repository
     *        the repository that the resource corresponds to
     * @return the ancestor compare element to use, or <code>null</code> for no
     *         ancestor
     */
    protected abstract Object getAncestorCompareElement(IResource resource, TFSRepository repository);
}
