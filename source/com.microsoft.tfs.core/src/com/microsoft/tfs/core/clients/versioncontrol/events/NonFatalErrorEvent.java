// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a non-fatal error is encountered during processing. Fatal
 * errors are thrown as exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class NonFatalErrorEvent extends CoreClientEvent {
    static final long serialVersionUID = 1861259112856825407L;

    private final VersionControlClient vcClient;
    private final Workspace workspace;
    private final Throwable throwable;
    private final Failure failure;

    public NonFatalErrorEvent(
        final EventSource source,
        final VersionControlClient vcClient,
        final Throwable throwable) {
        super(source);

        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(throwable, "throwable"); //$NON-NLS-1$

        this.vcClient = vcClient;
        workspace = null;
        this.throwable = throwable;
        failure = null;
    }

    public NonFatalErrorEvent(final EventSource source, final VersionControlClient vcClient, final Failure failure) {
        super(source);

        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(failure, "failure"); //$NON-NLS-1$

        this.vcClient = vcClient;
        workspace = null;
        throwable = null;
        this.failure = failure;
    }

    public NonFatalErrorEvent(final EventSource source, final Workspace workspace, final Failure failure) {
        super(source);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(failure, "failure"); //$NON-NLS-1$

        vcClient = workspace.getClient();
        this.workspace = workspace;
        throwable = null;
        this.failure = failure;
    }

    public NonFatalErrorEvent(final EventSource source, final Workspace workspace, final Throwable throwable) {
        super(source);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(throwable, "exception"); //$NON-NLS-1$

        vcClient = workspace.getClient();
        this.workspace = workspace;
        this.throwable = throwable;
        failure = null;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Failure getFailure() {
        return failure;
    }

    public VersionControlClient getClient() {
        return vcClient;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getMessage() {
        if (throwable != null) {
            return throwable.getLocalizedMessage();
        }

        if (failure == null) {
            return Messages.getString("NonFatalErrorEvent.NonFatalErrorOfUnknownReason"); //$NON-NLS-1$
        }

        return failure.getFormattedMessage();
    }
}
