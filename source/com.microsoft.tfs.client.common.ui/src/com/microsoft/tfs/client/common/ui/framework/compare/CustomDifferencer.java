// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.microsoft.tfs.client.common.ui.Messages;

/**
 * <p>
 * {@link CustomDifferencer} is an extension of the Eclipse compare framework's
 * {@link Differencer} class.
 * </p>
 *
 * <p>
 * {@link CustomDifferencer} provides support for {@link ContentComparator}s to
 * compare the content of leaf nodes in the difference tree.
 * </p>
 */
public class CustomDifferencer extends Differencer {
    private static final Log log = LogFactory.getLog(CustomDifferencer.class);

    private final ContentComparator[] comparators;

    private IProgressMonitor progressMonitor;

    /**
     * Creates a new {@link CustomDifferencer} that uses the given
     * {@link ContentComparator}s. No reference to the specified
     * {@link ContentComparator} array is kept by this object after this
     * constructor returns.
     *
     * @param comparators
     *        {@link ContentComparator}s to use, or <code>null</code> to not use
     *        any {@link ContentComparator}s
     */
    public CustomDifferencer(final ContentComparator[] comparators) {
        this.comparators = comparators == null ? null : (ContentComparator[]) comparators.clone();
    }

    @Override
    public Object findDifferences(
        final boolean threeWay,
        final IProgressMonitor pm,
        final Object data,
        final Object ancestor,
        final Object left,
        final Object right) {
        progressMonitor = pm == null ? new NullProgressMonitor() : pm;
        progressMonitor.beginTask(Messages.getString("CustomDifferencer.ProgressText"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

        return super.findDifferences(threeWay, progressMonitor, data, ancestor, left, right);
    }

    @Override
    protected void updateProgress(final IProgressMonitor progressMonitor, final Object node) {
        super.updateProgress(progressMonitor, node);
        progressMonitor.worked(1);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.compare.internal.InternalDifferencer
     * #contentsEqual(java.lang.Object, java.lang.Object)
     */
    @Override
    protected boolean contentsEqual(final Object input1, final Object input2) {
        if (comparators != null) {
            for (int i = 0; i < comparators.length; i++) {
                if (progressMonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }

                ContentComparisonResult result;
                try {
                    result = comparators[i].contentsEqual(input1, input2, progressMonitor);
                } catch (final OperationCanceledException e) {
                    throw e;
                } catch (final Exception e) {
                    final String messageFormat = "comparator [{0}] failed"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, comparators[i]);
                    log.warn(message, e);
                    continue;
                }
                if (ContentComparisonResult.EQUAL == result) {
                    return true;
                }
                if (ContentComparisonResult.NOT_EQUAL == result) {
                    return false;
                }
            }
        }

        return super.contentsEqual(input1, input2);
    }
}
