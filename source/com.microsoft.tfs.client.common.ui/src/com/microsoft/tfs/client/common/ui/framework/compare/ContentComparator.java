// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * {@link ContentComparator} implementations are used by a
 * {@link CustomDifferencer} to compare leaf nodes in a tree of differences. The
 * default {@link Differencer} implementation performs a byte-wise comparison of
 * leaf nodes that implement {@link IStreamContentAccessor}. When leaf nodes do
 * not implement {@link IStreamContentAccessor}, or when a more efficient
 * content comparison is possible, {@link ContentComparator} can be implemented
 * to perform a custom comparison.
 */
public interface ContentComparator {
    /**
     * <p>
     * Compare the content of two differencer input objects. If this comparator
     * is not valid for the input objects, or if this comparator doesn't have
     * enough information to tell whether the objects are different, return
     * {@link ContentComparisonResult#UNKNOWN}. Otherwise, return either
     * {@link ContentComparisonResult#EQUAL} or
     * {@link ContentComparisonResult#NOT_EQUAL}.
     * </p>
     *
     * <p>
     * <b>Important</b>: this method is passed an {@link IProgressMonitor}.
     * Unlike most methods that are passed a monitor, this method does not "own"
     * the monitor and should not call lifecycle methods (like
     * {@link IProgressMonitor#beginTask(String, int)} and
     * {@link IProgressMonitor#done()}) or work reporting methods (like
     * {@link IProgressMonitor#worked(int)} on the monitor. The monitor must be
     * used only to check whether cancelation has been requested during the
     * duration of the call to the {@link ContentComparator} (by calling
     * {@link IProgressMonitor#isCanceled()}. If cancelation is detected, the
     * implementation should throw an {@link OperationCanceledException} to exit
     * early. The rationale for this is that the common case use of
     * {@link ContentComparator} is to compare relatively small items, for which
     * the overhead of creating monitors for each call outweighs the small
     * advantage of providing progress reporting on each comparison.
     * </p>
     *
     * @throws OperationCanceledException
     *         if a cancelation request is detected (by polling the
     *         {@link IProgressMonitor})
     *
     * @param input1
     *        the first differencer input object (must not be <code>null</code>)
     * @param input2
     *        the second differencer input object (must not be <code>null</code>
     *        )
     * @param monitor
     *        an {@link IProgressMonitor} to check for cancelation (see above),
     *        or <code>null</code> if no cancelation checking is requested
     * @return a {@link ContentComparisonResult} that describes the relationship
     *         of the content of the objects (must not be <code>null</code>)
     */
    ContentComparisonResult contentsEqual(Object input1, Object input2, IProgressMonitor monitor)
        throws OperationCanceledException;
}
