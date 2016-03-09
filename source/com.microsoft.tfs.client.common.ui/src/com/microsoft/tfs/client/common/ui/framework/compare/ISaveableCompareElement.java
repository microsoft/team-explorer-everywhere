// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>
 * {@link ISaveableCompareElement} is a interface that compare elements can
 * implement to enable save functionality when using the Microsoft compare
 * framework.
 * </p>
 *
 * <p>
 * {@link ISaveableCompareElement} extends the {@link IContentChangeNotifier}
 * interface. {@link IContentChangeNotifier} implementations can notify
 * listeners of content changes. {@link ISaveableCompareElement} adds the
 * ability to save those content changes.
 * </p>
 */
public interface ISaveableCompareElement extends IContentChangeNotifier {
    /**
     * Saves a content change that this {@link IContentChangeNotifier} has
     * notified listeners of.
     *
     * @param monitor
     *        an {@link IProgressMonitor} to use during the save (must not be
     *        <code>null</code>)
     * @throws CoreException
     *         if any errors occur during save and the save can't be completed
     */
    void save(IProgressMonitor monitor) throws CoreException;
}
