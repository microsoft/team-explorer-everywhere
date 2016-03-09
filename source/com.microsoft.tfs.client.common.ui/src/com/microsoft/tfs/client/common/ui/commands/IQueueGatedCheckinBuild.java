// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;

/**
 * Interface for the QueueGatedCheckinBuild extension point.
 */
public interface IQueueGatedCheckinBuild {
    /**
     *
     * @param shell
     *        A shell for a progress indicator.
     * @param buildDefinition
     *        The target build definition.
     * @param shelvesetName
     *        The shelveset name (with user spec).
     * @param gatedCheckinTicket
     *        The gated check-in ticket.
     * @return
     */
    public boolean queueBuild(
        Shell shell,
        IBuildDefinition buildDefinition,
        String shelvesetName,
        String gatedCheckinTicket);

    /**
     *
     * @param shell
     *        A shell for a progress indicator.
     * @param buildDefinition
     *        The target build definition.
     * @param buildDefinitionURI
     *        The target build definition URI.
     * @param shelvesetName
     *        The shelveset name (with user spec).
     * @param gatedCheckinTicket
     *        The gated check-in ticket.
     * @return
     */
    public boolean queueBuild(
        Shell shell,
        IBuildDefinition buildDefinition,
        String buildDefinitionURI,
        String shelvesetName,
        String gatedCheckinTicket);

    /**
     * Returns the build queued by this request.
     *
     *
     * @return
     */
    public IQueuedBuild getQueuedBuild();
}
