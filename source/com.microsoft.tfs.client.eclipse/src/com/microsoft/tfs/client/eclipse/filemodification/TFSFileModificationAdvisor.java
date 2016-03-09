// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;

/**
 * Provides configuration information and command resources to help
 * {@link TFSFileModificationValidator} process modification events.
 *
 * @threadsafety unknown
 */
public interface TFSFileModificationAdvisor {
    /**
     * Gets an options provider which supplies options that control how a file
     * modification validation event is processed.
     *
     * @param attemptUi
     *        <code>true</code> if it is safe to use the graphical user
     *        interface to get these options, <code>false</code> if no graphical
     *        user interface should be used
     * @param shell
     *        the org.eclipse.swt.widgets.Shell if available, <code>null</code>
     *        if none is available (see
     *        {@see IFileModificationValidator#validateEdit(org.eclipse.core.resources
     *        .IFile[], Object)}'s second parameter documentation)
     * @return the options provider to use, or <code>null</code> if the
     *         implementation cannot provide one
     */
    TFSFileModificationOptionsProvider getOptionsProvider(boolean attemptUi, Object shell);

    /**
     * Gets a command executor for running commands in response to validation
     * events.
     *
     * @param attemptUi
     *        <code>true</code> if it is safe to use the graphical user
     *        interface to get these options, <code>false</code> if no graphical
     *        user interface should be used
     * @param shell
     *        the org.eclipse.swt.widgets.Shell if available, <code>null</code>
     *        if none is available (see
     *        {@see IFileModificationValidator#validateEdit(org.eclipse.core.resources
     *        .IFile[], Object)}'s second parameter documentation)
     * @return the command executor to use, or <code>null</code> if the
     *         implementation cannot provide one
     */
    CommandExecutor getSynchronousCommandExecutor(boolean attemptUi, Object shell);

    /**
     * Gets a status reporter for reporting errors encountered during file
     * modification processing.
     *
     * @param attemptUi
     *        <code>true</code> if it is safe to use the graphical user
     *        interface to get these options, <code>false</code> if no graphical
     *        user interface should be used
     * @param shell
     *        the org.eclipse.swt.widgets.Shell if available, <code>null</code>
     *        if none is available (see
     *        {@see IFileModificationValidator#validateEdit(org.eclipse.core.resources
     *        .IFile[], Object)}'s second parameter documentation)
     * @return the status reporter to use, or <code>null</code> if the
     *         implementation cannot provide one
     */
    TFSFileModificationStatusReporter getStatusReporter(boolean attemptUi, Object shell);
}
