// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions.EncodingStrategy;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.engines.MergeEngine;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.diffmerge.ThreeWayMerge;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.process.ProcessRunner.ProcessRunnerState;

/**
 * An {@link ExternalConflictResolution} knows how to spawn an external merge
 * tool to do a three-way merge to attempt to resolve the conflict.
 *
 * @since TEE-SDK-10.1
 */
public class ExternalConflictResolution extends ConflictResolution implements ProcessFinishedHandler {
    private static final Log log = LogFactory.getLog(ExternalConflictResolution.class);

    private final MergeEngine mergeEngine;

    // we need to create a copy of the conflict as external resolution can
    // leave the object wedged when it fails
    private Conflict conflict;

    // may need to rename the target of the merge
    private String newPath;

    // may need a target encoding
    private FileEncoding newEncoding;

    private final Object lock = new Object();

    private ProcessRunner mergeRunner;
    private ThreeWayMerge threeWayMerge;

    /*
     * Clients can configure an conflict resolver. This allows then to, for
     * example, run conflict resolution inside a lock of the Eclipse workspace
     * and/or disable resource change notifications.
     */
    private ExternalConflictResolver conflictResolver = new ExternalConflictResolver();

    public ExternalConflictResolution(
        final ConflictDescription conflictDescription,
        final String description,
        final String helpText,
        final ConflictResolutionOptions options,
        final ExternalToolset configuredMergeTools) {
        super(conflictDescription, description, helpText, options);

        mergeEngine = new MergeEngine(
            conflictDescription.getWorkspace(),
            conflictDescription.getWorkspace().getClient(),
            configuredMergeTools);
    }

    @Override
    public ConflictResolution newForConflictDescription(final ConflictDescription conflictDescription) {
        /*
         * Only called by dummy methods for fancy UI, this doesn't need to have
         * merge tools and thus be accurate.
         */
        return new ExternalConflictResolution(
            conflictDescription,
            getDescription(),
            getHelpText(),
            ConflictResolutionOptions.NONE,
            null);
    }

    /**
     * Sets the new server path of the resolved item.
     *
     * @param newPath
     *        The server path to the resolved item.
     */
    @Override
    public void setNewPath(final String newPath) {
        synchronized (lock) {
            this.newPath = newPath;
        }
    }

    @Override
    public void setEncoding(final FileEncoding newEncoding) {
        synchronized (lock) {
            this.newEncoding = newEncoding;
        }
    }

    /**
     * Sets the conflict resolver. This must, at minimum, call
     * {@link Workspace#resolveConflict(Conflict)}, but may do more, like lock
     * the Eclipse workspace or set up the resource change listener to ignore
     * updates.
     *
     * @param resolver
     */
    public void setConflictResolver(final ExternalConflictResolver resolver) {
        Check.notNull(resolver, "resolver"); //$NON-NLS-1$

        conflictResolver = resolver;
    }

    @Override
    public ConflictResolutionStatus work() throws Exception {
        synchronized (lock) {
            if (mergeRunner != null) {
                throw new Exception(
                    Messages.getString("ExternalConflictResolution.AnotherExternalProcessIsResolvingThisConflict")); //$NON-NLS-1$
            }

            /*
             * we need to create a copy of the conflict as external resolution
             * can leave the object wedged when it fails
             */
            conflict = new Conflict(getConflictDescription().getConflict());

            threeWayMerge = new ThreeWayMerge();

            if (newEncoding != null) {
                conflict.getResolutionOptions().setEncodingStrategy(EncodingStrategy.OVERRIDE_EXPLICIT, newEncoding);
                conflict.getResolutionOptions().setAcceptMergeEncoding(newEncoding);
            }

            mergeRunner = mergeEngine.beginExternalMerge(conflict, threeWayMerge, this, null, null);

            return ConflictResolutionStatus.RUNNING;
        }
    }

    @Override
    public void cancel() {
        synchronized (lock) {
            if (mergeRunner == null || getStatus() != ConflictResolutionStatus.RUNNING) {
                return;
            }

            mergeRunner.interrupt();
        }

        setStatus(ConflictResolutionStatus.CANCELLED);
    }

    @Override
    public void processCompleted(final ProcessRunner runner) {
        synchronized (lock) {
            /*
             * The merge tool has exited. We don't know if it's a successful
             * merge, so we ask the merge engine to check the results of the
             * runner.
             */
            if (mergeEngine.endExternalMerge(runner, conflict, threeWayMerge)) {
                // resolve the real conflict object
                final Workspace workspace = getConflictDescription().getWorkspace();
                final Conflict actualConflict = getConflictDescription().getConflict();

                // accept our (merged) conflict
                actualConflict.setResolution(Resolution.ACCEPT_MERGE);
                actualConflict.setMergedFileName(conflict.getMergedFileName());

                if (newPath != null) {
                    actualConflict.getResolutionOptions().setNewPath(newPath);
                }

                if (conflictResolver.resolveConflict(workspace, actualConflict)) {
                    setStatus(ConflictResolutionStatus.SUCCESS);
                } else {
                    /*
                     * Set status to cancellation because our conflict resolver
                     * should have reported the error.
                     */
                    actualConflict.getResolutionOptions().setNewPath(null);
                    setStatus(ConflictResolutionStatus.CANCELLED);
                }
            } else {
                // this is actually a cancellation
                setStatus(ConflictResolutionStatus.CANCELLED);
            }

            // clean up vars used to resolve
            conflict = null;
            mergeRunner = null;
            threeWayMerge = null;
        }
    }

    @Override
    public void processExecFailed(final ProcessRunner runner) {
        synchronized (lock) {
            String message;
            if (runner.getState() == ProcessRunnerState.EXEC_FAILED) {
                message = MessageFormat.format(
                    Messages.getString("ExternalConflictResolution.ExecutionErrorCheckToolConfigurationFormat"), //$NON-NLS-1$
                    runner.getExecutionError().getMessage());
            } else {
                message = Messages.getString("ExternalConflictResolution.UnknownErrorCheckToolConfiguration"); //$NON-NLS-1$
            }

            setErrorMessage(message);
            setStatus(ConflictResolutionStatus.FAILED);

            // clean up vars used to resolve
            conflict = null;
            mergeRunner = null;
            threeWayMerge = null;
        }
    }

    @Override
    public void processInterrupted(final ProcessRunner runner) {
        synchronized (lock) {
            // don't do anything: there have been no changes made to the
            // underlying conflict object
            setErrorMessage(Messages.getString("ExternalConflictResolution.TheExternalMergeProcessWasInterrupted")); //$NON-NLS-1$
            setStatus(ConflictResolutionStatus.FAILED);
        }
    }

    public static class ExternalConflictResolver {
        public boolean resolveConflict(final Workspace workspace, final Conflict conflict) {
            try {
                workspace.resolveConflict(conflict);
            } catch (final Exception e) {
                log.error("Could not resolve conflict", e); //$NON-NLS-1$
            }

            return false;
        }
    }
}
