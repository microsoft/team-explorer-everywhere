// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * {@link ExternalCompareHandler} is a callback interface that gets called back
 * during a compare operation implemented by the
 * {@link CustomCompareEditorInput} class. Implementations have a chance to
 * launch an external compare and cancel the pending internal compare, or allow
 * the internal compare to proceed.
 */
public interface ExternalCompareHandler {
    /**
     * <p>
     * Called at the beginning of an internal compare operation. Implementations
     * can examine the compare elements and launch an external compare program
     * if desired. If an external compare program is launched, the
     * implementation should return <code>true</code> to indicate that an
     * external compare was done and no internal compare is necessary.
     * </p>
     *
     * <p>
     * Typically, implementations will check if the compare elements implement
     * the {@link ExternalComparable} interface, and then use information
     * available through that interface to make the external compare decision.
     * </p>
     *
     * @param threeWay
     *        <code>true</code> if this compare is a three-way compare
     * @param monitor
     *        an {@link IProgressMonitor} to use for reporting progress and
     *        checking for cancellation
     * @param modified
     *        the modified (local) compare element
     * @param original
     *        the original (server) compare element
     * @param ancestor
     *        the ancestor compare element
     * @return <code>true</code> if an external compare was done and the
     *         internal compare should be skipped, or <code>false</code> to
     *         proceed with the internal compare
     */
    public boolean onCompare(
        boolean threeWay,
        IProgressMonitor monitor,
        Object modified,
        Object original,
        Object ancestor);
}
