// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * {@link Compare} is used to run a compare operation. This class follows the
 * builder pattern - the compare operation is built up piece by piece. When all
 * desired pieces are in place, the {@link #open()} method is called to open the
 * compare UI.
 * </p>
 */
public class Compare {
    private static final Log log = LogFactory.getLog(Compare.class);

    /**
     * The {@link CompareUIType} we will use when opening. Never
     * <code>null</code>.
     */
    private CompareUIType compareUIType = CompareUIType.EDITOR;

    /**
     * The modified compare object. Initially <code>null</code>.
     */
    private Object modified;

    /**
     * The original compare object. Initially <code>null</code>.
     */
    private Object original;

    /**
     * The ancestor compare object. Initially <code>null</code>.
     */
    private Object ancestor;

    /**
     * Whether the compare should start "dirty" (with editors saveable) or not.
     * This is typically false for normal compares, but may be useful when the
     * user is forced to accept a path (OK / Cancel), for example when doing a
     * three-way merge.
     */
    private boolean alwaysDirty = false;

    /**
     * The {@link CompareConfiguration} we will use when opening. Never
     * <code>null</code>.
     */
    private final CustomCompareConfiguration compareConfiguration = new CustomCompareConfiguration();

    /**
     * The optional {@link ContentComparator}s we will use when opening.
     * Initially an empty list.
     */
    private final List<ContentComparator> comparators = new ArrayList<ContentComparator>();

    /**
     * The optional {@link ExternalCompareHandler} we will use when opening.
     * Initially <code>null</code>.
     */
    private ExternalCompareHandler externalCompareHandler;

    private final SingleListenerFacade saveListeners = new SingleListenerFacade(CompareSaveListener.class);

    /**
     * Sets the UI type of this compare operation, which is used to determine
     * how to display the compare operation when {@link #open()} is called. The
     * default UI type is {@link CompareUIType#EDITOR}.
     *
     * @param compareUIType
     *        the compare UI type (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setUIType(final CompareUIType compareUIType) {
        Check.notNull(compareUIType, "compareUIType"); //$NON-NLS-1$
        this.compareUIType = compareUIType;
        return this;
    }

    /**
     * Sets the modified side of this compare operation to the specified
     * element.
     *
     * @param modified
     *        the modified element (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModified(final Object modified) {
        Check.notNull(modified, "modified"); //$NON-NLS-1$
        this.modified = modified;
        return this;
    }

    /**
     * Sets the modified side of this compare operation to the specified
     * {@link IResource}.
     *
     * @param resource
     *        the modified resource (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedResource(final IResource resource) {
        return setResourceElement(resource, null, CompareElementType.LEFT);
    }

    /**
     * Sets the modified side of this compare operation to the specified
     * {@link IResource}. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param resource
     *        the modified resource (must not be <code>null</code>)
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedResource(final IResource resource, final ResourceFilter resourceFilter) {
        return setResourceElement(resource, resourceFilter, CompareElementType.LEFT);
    }

    /**
     * Sets the modified side of this compare operation to the specified local
     * path.
     *
     * @param localPath
     *        the modified local path (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedLocalPath(final String localPath) {
        return setLocalPathElement(localPath, null, ResourceType.ANY, null, CompareElementType.LEFT);
    }

    /**
     * Sets the modified side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be.
     *
     * @param localPath
     *        the modified local path (must not be <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedLocalPath(final String localPath, final ResourceType resourceType) {
        return setLocalPathElement(localPath, null, resourceType, null, CompareElementType.LEFT);
    }

    /**
     * Sets the modified side of this compare operation to the specified local
     * path. The specified {@link ResourceFilter} is used when enumerating child
     * resources.
     *
     * @param localPath
     *        the modified local path (must not be <code>null</code>)
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedLocalPath(final String localPath, final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, null, ResourceType.ANY, resourceFilter, CompareElementType.LEFT);
    }

    /**
     * Sets the modified side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param localPath
     *        the modified local path (must not be <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedLocalPath(
        final String localPath,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, null, resourceType, resourceFilter, CompareElementType.LEFT);
    }

    /**
     * Sets the modified side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param localPath
     *        the modified local path (must not be <code>null</code>)
     * @param charset
     *        The charset of the ancestor file (may be <code>null</code> to use
     *        the default platform charset). This value will be ignored if the
     *        file is a resource in the Eclipse workspace, and the resource's
     *        charset will be used instead.
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setModifiedLocalPath(
        final String localPath,
        final Charset charset,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, charset, resourceType, resourceFilter, CompareElementType.LEFT);
    }

    /**
     * Sets the original side of this compare operation to the specified
     * element.
     *
     * @param original
     *        the original element (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginal(final Object original) {
        Check.notNull(original, "original"); //$NON-NLS-1$
        this.original = original;
        return this;
    }

    /**
     * Sets the original side of this compare operation to the specified
     * {@link IResource}.
     *
     * @param resource
     *        the original resource (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalResource(final IResource resource) {
        return setResourceElement(resource, null, CompareElementType.RIGHT);
    }

    /**
     * Sets the original side of this compare operation to the specified
     * {@link IResource}. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param resource
     *        the original resource (must not be <code>null</code>)
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalResource(final IResource resource, final ResourceFilter resourceFilter) {
        return setResourceElement(resource, resourceFilter, CompareElementType.RIGHT);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalLocalPath(final String localPath) {
        return setLocalPathElement(localPath, null, ResourceType.ANY, null, CompareElementType.RIGHT);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalLocalPath(final String localPath, final ResourceType resourceType) {
        return setLocalPathElement(localPath, null, resourceType, null, CompareElementType.RIGHT);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The specified {@link ResourceFilter} is used when enumerating child
     * resources.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalLocalPath(final String localPath, final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, null, null, resourceFilter, CompareElementType.RIGHT);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalLocalPath(
        final String localPath,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, null, resourceType, resourceFilter, CompareElementType.RIGHT);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param charset
     *        The charset of the ancestor file (may be <code>null</code> to use
     *        the default platform charset). This value will be ignored if the
     *        file is a resource in the Eclipse workspace, and the resource's
     *        charset will be used instead.
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setOriginalLocalPath(
        final String localPath,
        final Charset charset,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, charset, resourceType, resourceFilter, CompareElementType.RIGHT);
    }

    /**
     * Sets the ancestor side of this compare operation to the specified
     * element.
     *
     * @param ancestor
     *        the ancestor element (can be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestor(final Object ancestor) {
        this.ancestor = ancestor;
        return this;
    }

    /**
     * Sets the original side of this compare operation to the specified
     * {@link IResource}.
     *
     * @param resource
     *        the original resource (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorResource(final IResource resource) {
        return setResourceElement(resource, null, CompareElementType.ANCESTOR);
    }

    /**
     * Sets the original side of this compare operation to the specified
     * {@link IResource}. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param resource
     *        the original resource (must not be <code>null</code>)
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorResource(final IResource resource, final ResourceFilter resourceFilter) {
        return setResourceElement(resource, resourceFilter, CompareElementType.ANCESTOR);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorLocalPath(final String localPath) {
        return setLocalPathElement(localPath, null, ResourceType.ANY, null, CompareElementType.ANCESTOR);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorLocalPath(final String localPath, final ResourceType resourceType) {
        return setLocalPathElement(localPath, null, resourceType, null, CompareElementType.ANCESTOR);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The specified {@link ResourceFilter} is used when enumerating child
     * resources.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorLocalPath(final String localPath, final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, null, null, resourceFilter, CompareElementType.ANCESTOR);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorLocalPath(
        final String localPath,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, null, resourceType, resourceFilter, CompareElementType.ANCESTOR);
    }

    /**
     * Sets the original side of this compare operation to the specified local
     * path. The {@link ResourceType} specifies what type of resource the local
     * path is expected to be. The specified {@link ResourceFilter} is used when
     * enumerating child resources.
     *
     * @param localPath
     *        the original local path (must not be <code>null</code>)
     * @param charset
     *        The charset of the ancestor file (may be <code>null</code> to use
     *        the default platform charset). This value will be ignored if the
     *        file is a resource in the Eclipse workspace, and the resource's
     *        charset will be used instead.
     * @param resourceType
     *        the {@link ResourceType}, or {@link ResourceType#ANY} if the
     *        resource type is not known
     * @param resourceFilter
     *        the filter to use when enumerating child resources, or
     *        <code>null</code> for no filtering
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare setAncestorLocalPath(
        final String localPath,
        final Charset charset,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter) {
        return setLocalPathElement(localPath, charset, resourceType, resourceFilter, CompareElementType.ANCESTOR);
    }

    /**
     * Forces the editor value to be dirty, in which case the the "Save" option
     * will always be enabled. This may be useful for performing three-way
     * merges and allowing both accept and cancel.
     *
     * This value has no effect if no inputs are editable.
     *
     * This value is likely confusing if the UI is not in a dialog.
     *
     * @param dirty
     *        <code>true</code> to always allow saving of editor inputs,
     *        <code>false</code> otherwise.
     */
    public void setAlwaysDirty(final boolean alwaysDirty) {
        this.alwaysDirty = alwaysDirty;
    }

    /**
     * Adds a {@link ContentComparator} to this compare operation.
     *
     * @param comparator
     *        the {@link ContentComparator} to add (must not be
     *        <code>null</code>)
     * @return this {@link Compare} object for method chaining purposes
     */
    public Compare addComparator(final ContentComparator comparator) {
        Check.notNull(comparator, "comparator"); //$NON-NLS-1$
        comparators.add(comparator);
        return this;
    }

    /**
     * @return the {@link CompareConfiguration} used by this compare operation
     */
    public CompareConfiguration getCompareConfiguration() {
        return compareConfiguration;
    }

    /**
     * Sets the {@link CompareLabelProvider} that will be used by this compare
     * operation.
     *
     * @param labelProvider
     *        the {@link CompareLabelProvider}, or <code>null</code> to not use
     *        a {@link CompareLabelProvider}
     */
    public void setCompareLabelProvider(final CompareLabelProvider labelProvider) {
        compareConfiguration.setLabelProvider(labelProvider);
    }

    /**
     * Sets the {@link ExternalCompareHandler} that will be used by this compare
     * operation.
     *
     * @param externalCompareHandler
     *        the {@link ExternalComparable}, or <code>null</code> to not use an
     *        {@link ExternalCompareHandler}
     */
    public void setExternalCompareHandler(final ExternalCompareHandler externalCompareHandler) {
        this.externalCompareHandler = externalCompareHandler;
    }

    public void addSaveListener(final CompareSaveListener listener) {
        saveListeners.addListener(listener);
    }

    public void removeSaveListener(final CompareSaveListener listener) {
        saveListeners.removeListener(listener);
    }

    /**
     * Opens this compare operation, showing the compare UI, returning the
     * result details of the comparison. Note that the compare results only
     * includes saved data from the results of opening the UI - that is, it will
     * only include saved data from a synchronous comparison (ie, in a dialog).
     * If you use an asynchronous compare UI (ie, an editor), you should hook up
     * a save listener.
     *
     * @return The {@link CompareResult} containing details of the results of
     *         this comparison (never <code>null</code>)
     */
    public CompareResult open() {
        final ContentComparator[] comparatorArray = comparators.toArray(new ContentComparator[comparators.size()]);

        final CustomCompareEditorInput input = new CustomCompareEditorInput(
            modified,
            original,
            ancestor,
            comparatorArray,
            compareConfiguration,
            externalCompareHandler);

        if (compareConfiguration.isLeftEditable() || compareConfiguration.isRightEditable()) {
            /*
             * Disable confirm save requirement - this property is required for
             * Eclipse 3.2 dialogs to enable dirty dialogs on open.
             *
             * This is
             * org.eclipse.compare.internal.CompareEditor.CONFIRM_SAVE_PROPERTY
             */
            compareConfiguration.setProperty("org.eclipse.compare.internal.CONFIRM_SAVE_PROPERTY", Boolean.FALSE); //$NON-NLS-1$
            input.setAlwaysDirty(alwaysDirty);
        }

        input.addSaveListener(new ProxyCompareSaveListener());

        log.info(MessageFormat.format("Comparing {0} to {1}", getLabelNOLOC(modified), getLabelNOLOC(original))); //$NON-NLS-1$

        compareUIType.openCompareUI(input);

        final boolean contentsIdentical = (input.getCompareResult() == null);
        final boolean contentsSaved = (input.getSavedContents().length > 0);

        return new CompareResult(contentsIdentical, input.wasOKPressed(), contentsSaved);
    }

    private String getLabelNOLOC(final Object node) {
        if (node == null) {
            return "(null)"; //$NON-NLS-1$
        }

        if (node instanceof ILabeledCompareElement) {
            return ((ILabeledCompareElement) node).getLabelNOLOC();
        }

        if (node instanceof DifferencerInputGenerator) {
            return ((DifferencerInputGenerator) node).getLoggingDescription();
        }

        return "(unknown)"; //$NON-NLS-1$
    }

    private Compare setLocalPathElement(
        final String localPath,
        final Charset charset,
        final ResourceType resourceType,
        final ResourceFilter resourceFilter,
        final CompareElementType type) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        final Object element =
            CompareUtils.createCompareElementForLocalPath(localPath, charset, resourceType, resourceFilter);
        type.setElement(this, element);
        return this;
    }

    private Compare setResourceElement(
        final IResource resource,
        final ResourceFilter resourceFilter,
        final CompareElementType type) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        final Object element = CompareUtils.createCompareElementForResource(resource, resourceFilter);
        type.setElement(this, element);
        return this;
    }

    private class ProxyCompareSaveListener implements CompareSaveListener {
        @Override
        public void onCompareElementSaved(final CompareSaveEvent event) {
            /* Rethrow this event */
            ((CompareSaveListener) saveListeners.getListener()).onCompareElementSaved(event);
        }
    }
}
