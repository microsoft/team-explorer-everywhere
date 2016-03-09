// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

/**
 * Base class for conflict resolution. These objects are generally produced by
 * {@link ConflictDescription} objects. This is an abstract base class.
 *
 * Example: ConflictResolution[] resolutionOptions =
 * conflictDescription.getResolutions(); ConflictResolutionStatus status =
 * resolutionOptions[0].resolveConflict();
 *
 * if(status == ConflictResolutionStatus.Success) Console.out.println("Conflict
 * resolved"); ...
 *
 * or use a callback mechanism, as some conflict resolutions may be
 * long-running: resolutionOptions[0].addListener(new
 * ConflictResolutionStatusListener() { public void
 * StatusChanged(ConflictResolution r, ConflictResolutionStatus status) {
 * if(status == ConflictResolutionStatus.Success) Console.out.println("Conflict
 * resolved"); } }); resolutionOptions[0].resolveConflict();
 *
 * @since TEE-SDK-10.1
 */
public abstract class ConflictResolution {
    private static final Log log = LogFactory.getLog(ConflictResolution.class);

    private final ConflictDescription conflictDescription;
    private final String description;
    private final String helpText;

    private final ConflictResolutionOptions options;

    private ConflictResolutionStatus status = ConflictResolutionStatus.NOT_STARTED;

    private String errorMessage = null;
    private final Object errorMessageLock = new Object();

    private final List<ConflictResolutionStatusListener> listeners = new ArrayList<ConflictResolutionStatusListener>();

    public final static String DefaultErrorMessage = Messages.getString("ConflictResolution.ConflictingContentChanges"); //$NON-NLS-1$

    /**
     * Base class constructor, should only be called by subclasses. Provides a
     * ConflictDescription and a description of this resolution type.
     *
     * @param conflictDescription
     *        the ConflictDescription which contains the conflict which this
     *        class will resolve.
     * @param description
     *        a description of this resolution
     * @param helpText
     *        informative help text for this resolution. may be
     *        <code>null</code>
     */
    protected ConflictResolution(
        final ConflictDescription conflictDescription,
        final String description,
        final String helpText,
        final ConflictResolutionOptions options) {
        Check.notNull(conflictDescription, "conflictDescription"); //$NON-NLS-1$
        Check.notNull(description, "description"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.conflictDescription = conflictDescription;
        this.description = description;
        this.helpText = helpText;
        this.options = options;
    }

    /**
     * Resolve the conflict.
     *
     * @return A ConflictResolutionStatus containing the status of the
     *         resolution (Failed, Success, Running, etc)
     */
    public final ConflictResolutionStatus resolveConflict() {
        Check.notNull(conflictDescription.getConflict(), "conflictDescription.getConflict()"); //$NON-NLS-1$
        Check.notNull(conflictDescription.getWorkspace(), "conflictDescription.getWorkspace()"); //$NON-NLS-1$

        ConflictResolutionStatus workStatus;

        // call the subclasses work() method
        try {
            workStatus = work();
        } catch (final Exception e) {
            log.warn("Could not resolve conflict", e); //$NON-NLS-1$

            workStatus = ConflictResolutionStatus.FAILED;
            errorMessage = e.getMessage();
        }

        setStatus(workStatus);

        return workStatus;
    }

    protected abstract ConflictResolutionStatus work() throws Exception;

    public abstract ConflictResolution newForConflictDescription(ConflictDescription description);

    /**
     * Cancel the conflict resolution. Only makes sense for asynchronous
     * resolutions (ie, ExternalConflictResolution)
     */
    public void cancel() {
    }

    /**
     * Subclasses may set the status of the resolution.
     *
     * @param status
     *        ConflictResolutionStatus describing current resolution state
     */
    protected final synchronized void setStatus(final ConflictResolutionStatus status) {
        this.status = status;

        // notify listeners
        final List<ConflictResolutionStatusListener> listeners =
            new ArrayList<ConflictResolutionStatusListener>(this.listeners);

        for (final Iterator<ConflictResolutionStatusListener> i = listeners.iterator(); i.hasNext();) {
            final ConflictResolutionStatusListener listener = i.next();

            listener.statusChanged(this, status);
        }
    }

    /**
     * Queries the conflict resolver for any new conflicts which arised during
     * the resolution of this conflict. (Typically only used by automerge
     * writable conflicts.)
     *
     * @return A list of new conflicts or null
     */
    public Conflict[] getConflicts() {
        return null;
    }

    /**
     * Gets the ConflictDescription which will be resolved by this resolver.
     *
     * @return A ConflictDescription to resolve
     */
    public ConflictDescription getConflictDescription() {
        return conflictDescription;
    }

    /**
     * Gets the description of this type of resolution (eg "check out and auto
     * merge")
     *
     * @return a String containing a textual representation of what will be done
     *         to resolve this conflict
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the help text for this type of resolution (extended information
     * suitable for displaying in a tooltip.)
     *
     * @return a {@link String} containing a very detailed message to assist the
     *         user. May be <code>null</code>
     */
    public String getHelpText() {
        return helpText;
    }

    /**
     * Determines whether conflict resolution requires a name selection. This
     * will be true for automerges and external merges involving some renames.
     *
     * @return <code>true</code> if the user must choose a name,
     *         <code>false</code> otherwise
     */
    public boolean needsNewPath() {
        return options.contains(ConflictResolutionOptions.SELECT_NAME);
    }

    /**
     * Sets the server name of the resultant merged file. This should be called
     * only if {@link #needsNewPath()} returns true.
     *
     * @param newPath
     *        The new (server) file path
     */
    public abstract void setNewPath(String newPath);

    /**
     * Determines whether conflict resolution requires an explicit encoding
     * selection. This will be true when their file encoding != your file
     * encoding.
     *
     * @return <code>true</code> if the user must choose an encoding,
     *         <code>false</code> otherwise.
     */
    public boolean needsEncodingSelection() {
        return options.contains(ConflictResolutionOptions.SELECT_ENCODING);
    }

    /**
     * Sets the file encoding of the resultant merged file. This should be
     * called only if {@link #needsEncodingSelection()} returns true.
     *
     * @param encoding
     *        The new FileEncoding
     */
    public abstract void setEncoding(FileEncoding encoding);

    /**
     * Gets the current resolution status
     *
     * @return a ConflictResolutionStatus describing the resolution state
     */
    public synchronized final ConflictResolutionStatus getStatus() {
        return status;
    }

    /**
     * Adds an error message that occurred while resolving.
     *
     * @param message
     *        A String representing a resolution error
     */
    protected void setErrorMessage(final String message) {
        synchronized (errorMessageLock) {
            errorMessage = message;
        }
    }

    /**
     * Gets all error messages that occured while resolving this conflict.
     *
     * @return A String array containing all error messages.
     */
    public String getErrorMessage() {
        synchronized (errorMessageLock) {
            if (errorMessage == null) {
                return DefaultErrorMessage;
            }

            return errorMessage;
        }
    }

    /* Listeners and Events */

    /**
     * Add a status listener for this conflict resolution. This listener will be
     * notified every time the conflict goes through a status change (eg,
     * NotStarted, Running, Success, Failure, etc...)
     *
     * @param listener
     *        A ConflictResolutionStatusListener which will be notified of
     *        status changes
     */
    public void addStatusListener(final ConflictResolutionStatusListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes a status listener for this conflict resolution.
     *
     * @param listener
     *        A ConflictResolutionStatusListener which should no longer be
     *        notified of status changes
     */
    public void removeStatusListener(final ConflictResolutionStatusListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}
