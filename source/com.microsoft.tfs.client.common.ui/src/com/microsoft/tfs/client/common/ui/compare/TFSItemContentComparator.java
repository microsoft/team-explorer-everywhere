// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.microsoft.tfs.client.common.ui.framework.compare.ContentComparator;
import com.microsoft.tfs.client.common.ui.framework.compare.ContentComparisonResult;
import com.microsoft.tfs.client.common.util.ProgressMonitorTaskMonitorAdapter;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.util.ArrayUtils;
import com.microsoft.tfs.util.HashUtils;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class TFSItemContentComparator implements ContentComparator {
    private static final Log log = LogFactory.getLog(TFSItemContentComparator.class);

    public static final TFSItemContentComparator INSTANCE = new TFSItemContentComparator();

    @Override
    public ContentComparisonResult contentsEqual(
        final Object input1,
        final Object input2,
        final IProgressMonitor monitor) {
        // server-server compare
        if (input1 instanceof TFSItemNode && input2 instanceof TFSItemNode) {
            return tfsItemNodesCompare((TFSItemNode) input1, (TFSItemNode) input2, monitor);
        }
        // server-local compare
        else if (input1 instanceof TFSItemNode) {
            return tfsItemCompareToLocalFile((TFSItemNode) input1, input2, monitor);
        }
        // local-server compare
        else if (input2 instanceof TFSItemNode) {
            return tfsItemCompareToLocalFile((TFSItemNode) input2, input1, monitor);
        }
        // local-local compare
        else {
            return localFileNodesCompare(input1, input2, monitor);
        }
    }

    /**
     * Compare two tfs item based on MD5 hash
     *
     * @param node1
     * @param node2
     * @param monitor
     * @return
     */
    private ContentComparisonResult tfsItemNodesCompare(
        final TFSItemNode node1,
        final TFSItemNode node2,
        final IProgressMonitor monitor) {
        final Item item1 = node1.getItem();
        final Item item2 = node2.getItem();

        if (item1.getItemType() == ItemType.FOLDER || item2.getItemType() == ItemType.FOLDER) {
            return ContentComparisonResult.EQUAL;
        }

        final byte[] hash1 = item1.getContentHashValue();
        final byte[] hash2 = item2.getContentHashValue();
        return compareByHash(hash1, hash2);
    }

    /**
     * Compare tfsItem with local file based on MD5 hash
     *
     * @param tfsItemNode
     * @param obj
     * @param monitor
     * @return
     */
    private ContentComparisonResult tfsItemCompareToLocalFile(
        final TFSItemNode tfsItemNode,
        final Object obj,
        final IProgressMonitor monitor) {
        final Item item = tfsItemNode.getItem();

        if (item.getItemType() == ItemType.FOLDER) {
            return ContentComparisonResult.EQUAL;
        }

        final byte[] itemHash = item.getContentHashValue();

        final byte[] otherItemHash = getItemHash(obj, monitor);

        return compareByHash(itemHash, otherItemHash);
    }

    /**
     * Compare two local file resource based on MD5 hash
     *
     * @param obj1
     * @param obj2
     * @param monitor
     * @return
     */
    private ContentComparisonResult localFileNodesCompare(
        final Object obj1,
        final Object obj2,
        final IProgressMonitor monitor) {
        final byte[] hash1 = getItemHash(obj1, monitor);
        final byte[] hash2 = getItemHash(obj2, monitor);
        return compareByHash(hash1, hash2);
    }

    /**
     *
     * @param hash1
     * @param hash2
     * @return
     */
    private ContentComparisonResult compareByHash(final byte[] hash1, final byte[] hash2) {
        if (hash1 == null || hash2 == null) {
            return ContentComparisonResult.UNKNOWN;
        }

        return Arrays.equals(hash1, hash2) ? ContentComparisonResult.EQUAL : ContentComparisonResult.NOT_EQUAL;
    }

    private byte[] getItemHash(final Object obj, final IProgressMonitor monitor) {
        if (obj instanceof TFSItemNode) {
            final TFSItemNode tfsItemNode = (TFSItemNode) obj;
            if (tfsItemNode.getItem().getItemType() == ItemType.FOLDER) {
                return null;
            }
            return tfsItemNode.getItem().getContentHashValue();
        }

        if (obj instanceof IStreamContentAccessor) {
            long startTime = 0;
            if (log.isTraceEnabled()) {
                startTime = System.currentTimeMillis();
                final String messageFormat = "hashing InputStream from [{0}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, obj);
                log.trace(message);
            }

            final IStreamContentAccessor streamContentAccessor = (IStreamContentAccessor) obj;
            InputStream inputStream;
            try {
                inputStream = streamContentAccessor.getContents();
            } catch (final CoreException e) {
                if (log.isDebugEnabled()) {
                    log.debug("couldn't access InputStream", e); //$NON-NLS-1$
                }

                return null;
            }
            if (inputStream == null) {
                return null;
            }
            TaskMonitorService.pushTaskMonitor(new ProgressMonitorTaskMonitorAdapter(monitor));
            try {
                final byte[] hash =
                    HashUtils.hashStream(inputStream, HashUtils.ALGORITHM_MD5, TaskMonitorService.getTaskMonitor());

                if (log.isTraceEnabled()) {
                    final long elapsed = System.currentTimeMillis() - startTime;
                    final String messageFormat = "result ({0} ms): {1}"; //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, elapsed, ArrayUtils.byteArrayToHexString(hash));

                    log.trace(message);
                }

                return hash;
            } catch (final CanceledException e) {
                throw new OperationCanceledException();
            } catch (final IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("hashing InputStream failed", e); //$NON-NLS-1$
                }

                return null;
            } finally {
                TaskMonitorService.popTaskMonitor(false);
            }
        }

        return null;
    }
}
