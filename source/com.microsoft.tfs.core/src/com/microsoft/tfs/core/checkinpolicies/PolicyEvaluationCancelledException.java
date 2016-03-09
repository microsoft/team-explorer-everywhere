// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * <p>
 * Thrown when check-in policy validation fails because the user cancelled the
 * evaluation. Thrown by {@link PolicyInstance} implementations to users of core
 * by methods like
 * {@link Workspace#evaluateCheckIn(com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationOptions, com.microsoft.tfs.core.pendingcheckin.PendingCheckin, PolicyContext)}
 * .
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class PolicyEvaluationCancelledException extends VersionControlException {
    public PolicyEvaluationCancelledException() {
        super();
    }
}
