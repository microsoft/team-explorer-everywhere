// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.io.File;
import java.io.IOException;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>
 * {@link ExternalComparable} is an interface that compare elements can
 * implement to enable extended functionality in the Microsoft compare
 * framework. {@link ExternalComparable}s can be used to perform compares using
 * an external program.
 * </p>
 *
 * <p>
 * This interface is an {@link ITypedElement} extension, and the data available
 * from {@link ITypedElement} is usually used to make the decision whether an
 * external compare should be enabled. Specifically,
 * {@link ITypedElement#getName()} should give the filename and
 * {@link ITypedElement#getType()} should give the extension/type of this
 * compare element.
 * </p>
 *
 * <p>
 * If external compare is chosen, the
 * {@link #getExternalCompareFile(IProgressMonitor)} method is called to
 * download any remote content and get a local file path that can be passed to
 * external programs.
 * </p>
 */
public interface ExternalComparable extends ITypedElement {
    /**
     * <p>
     * Obtains a local file path corresponding to this element that can be
     * passed to an external program. If this compare element represents local
     * content, this method will be trivial to implement. If this compare
     * element represents remote content, this method should perform the
     * long-running task of downloading the remote content locally.
     * </p>
     *
     * @param monitor
     *        an {@link IProgressMonitor} to use for progress reporting and
     *        cancellation checking (must not be <code>null</code>)
     * @return a local file path that corresponds to this compare element (must
     *         not be <code>null</code>)
     * @throws InterruptedException
     * @throws IOException
     */
    File getExternalCompareFile(IProgressMonitor monitor) throws IOException, InterruptedException;
}
