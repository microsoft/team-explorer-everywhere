// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Describes how a check-in note failed to pass the evaluation checks.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety conditionally thread-safe
 */
public class CheckinNoteFailure {
    private final CheckinNoteFieldDefinition definition;
    private final String message;

    /**
     * @param definition
     *        the checkin note definition that failed (must not be
     *        <code>null</code>)
     * @param message
     *        the error message (must not be <code>null</code> or empty).
     */
    public CheckinNoteFailure(final CheckinNoteFieldDefinition definition, final String message) {
        Check.notNull(definition, "definition"); //$NON-NLS-1$
        Check.notNullOrEmpty(message, "message"); //$NON-NLS-1$

        this.definition = definition;
        this.message = message;
    }

    public CheckinNoteFieldDefinition getDefinition() {
        return definition;
    }

    public String getMessage() {
        return message;
    }
}
