// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>
 * {@link DifferencerInputGenerator} generates an input {@link Object} that will
 * be passed to the Eclipse compare Differencer engine. The input object could
 * be the left, right, or ancestor object.
 * </p>
 *
 * <p>
 * Implementing {@link DifferencerInputGenerator} is preferable to doing
 * long-running tasks to generate differencer input objects directly in client
 * code. {@link DifferencerInputGenerator} runs as part of the overall compare
 * operation and runs under {@link IProgressMonitor} control (can report
 * progress and respond to cancellation requests).
 * </p>
 */
public interface DifferencerInputGenerator {
    /**
     * A description of the differencer input, suitable for logging. This value
     * should NOT be localized.
     *
     * @return A description of the differencer input, suitable for logging.
     */
    String getLoggingDescription();

    /**
     * <p>
     * Computes a differencer input object. The supplied
     * {@link IProgressMonitor} should be used to report progress and check for
     * cancellation.
     * </p>
     *
     * <p>
     * The input {@link Object} should implement:
     * <ul>
     * <li>{@link ITypedElement}</li>
     * <li>{@link IStructureComparator} if it represents a non-leaf node in the
     * tree of differences</li>
     * <li>{@link IEncodedStreamContentAccessor} if it represents a leaf node in
     * the tree of differences</li>
     * <li>{@link ILabeledCompareElement} to provide a descriptive label for the
     * element</li>
     * <li>{@link IEditableContent} and {@link ISaveableCompareElement} if the
     * element should be editable</li>
     * </ul>
     * </p>
     *
     * @return a differencer input object as described above
     */
    Object getInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException;
}
