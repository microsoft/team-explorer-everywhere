// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcechange;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;

/**
 * Reports to the user about changes to Eclipse resources which are managed by
 * Team Explorer Everywhere.
 *
 * @threadsafety unknown
 */
public interface TFSResourceChangeStatusReporter {
    /**
     * Reports a non-OK status that resulted when Team Explorer Everywhere
     * visited the resource change delta. OK statuses are not reported.
     *
     * @param status
     *        the status (must not be <code>null</code>)
     */
    public void reportNonOKVisitorStatus(IStatus status);

    /**
     * Reports the status and errors that resulted when pending changes that
     * conflicted with "adds" were converted to "edit" changes. These are not
     * proper TFS conflicts, but pending changes that were undone because they
     * would have prevented new content from being added at that path (a pending
     * "delete" is most common).
     *
     * @param status
     *        the status of the conversion to edit operation (must not be
     *        <code>null</code>)
     * @param nonFatals
     *        the non-fatal errors that resulted from the convert to edit
     *        operation (must not be <code>null</code>)
     */
    public void reportConvertToEditStatus(IStatus status, NonFatalErrorEvent[] nonFatals);

    /**
     * Reports the status and errors that resulted when newly-detected resources
     * were pended as "adds" to TFS.
     *
     * @param status
     *        the status of the pend add operation (must not be
     *        <code>null</code>)
     * @param nonFatals
     *        the non-fatal errors that resulted from the pend add operation
     *        (must not be <code>null</code>)
     */
    public void reportAdditionStatus(IStatus status, NonFatalErrorEvent[] nonFatals);

    /**
     * Reports the status and errors that resulted when resources were scanned
     * for edits in TFS.
     *
     * @param status
     *        the status of the scan operation (must not be <code>null</code>)
     * @param nonFatals
     *        the non-fatal errors that resulted from the encoding change
     *        operation (must not be <code>null</code>)
     */
    public void reportScanStatus(IStatus status, NonFatalErrorEvent[] nonFatals);
}
