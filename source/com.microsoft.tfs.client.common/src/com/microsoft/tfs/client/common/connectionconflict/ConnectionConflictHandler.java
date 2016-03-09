// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.connectionconflict;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;

/**
 * Handles "connection conflicts" - when another repository is trying to come
 * online, but another repository is already configured as the default
 * repository. Clients can override to provide prompting, closing of existing
 * connections, etc.
 *
 * @threadsafety unknown
 */
public interface ConnectionConflictHandler {
    /**
     * Notifies the user the he/she is trying to connect to a {@link TFSServer}
     * that is not currently the default TFSServer. Implementations may attempt
     * to reconcile this error (eg, by closing the other projects and removing
     * the server from server manager).
     *
     * @return <code>true</code> if action was taken to resolve this issue and
     *         the configuration should be retried, <code>false</code> otherwise
     */
    boolean resolveServerConflict();

    /**
     * Notifies the user the he/she is trying to connect to a
     * {@link TFSRepository} that is not currently the default TFSRepository.
     * Implementations may attempt to reconcile this error (eg, by closing the
     * other projects and removing the repository from repository manager).
     *
     * @return <code>true</code> if action was taken to resolve this issue and
     *         the configuration should be retried, <code>false</code> otherwise
     */
    boolean resolveRepositoryConflict();

    /**
     * Notifies the user that he/she is trying to connect to {@link TFSServer}
     * that is not currently the default TFSServer.
     */
    public void notifyServerConflict();

    /**
     * Notifies the user that he/she is trying to connect to
     * {@link TFSRepository} that is not currently the default TFSRepository.
     */
    public void notifyRepositoryConflict();
}
