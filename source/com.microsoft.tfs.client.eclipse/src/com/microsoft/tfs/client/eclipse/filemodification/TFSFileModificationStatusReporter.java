// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.repository.TFSRepository;

/**
 * Reports file modification errors via the program log files. Clients may
 * override to provide (for example) UI based error reporting when not working
 * in headless mode.
 *
 * @threadsafety unknown
 */
public class TFSFileModificationStatusReporter {
    private final Log log = LogFactory.getLog(TFSFileModificationValidator.class);

    public void reportError(final String title, final IStatus status) {
        log.error(MessageFormat.format("Could not pend edit: {0}", status.getMessage())); //$NON-NLS-1$
    }

    public void reportStatus(
        final TFSRepository repository,
        final TFSFileModificationStatusData[] statusData,
        final IStatus status) {
        log.error(MessageFormat.format("Could not pend edit: {0}", status.getMessage()), status.getException()); //$NON-NLS-1$
    }
}
