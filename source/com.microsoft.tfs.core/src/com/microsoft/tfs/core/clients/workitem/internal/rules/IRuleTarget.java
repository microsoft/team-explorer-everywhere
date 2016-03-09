// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

public interface IRuleTarget {
    /**
     * Obtain the ID of the rule target. This method MUST return id 0 if the
     * target is new (does not yet have persistent state on the server).
     *
     * @return the ID of the rule target
     */
    public int getID();

    public int getAreaID();

    /**
     * Called by the rule engine to obtain a field. This lookup should be
     * optimized to be as fast as possible - it is called often by the rule
     * engine.
     *
     * @param fieldId
     *        ID of the field to obtain
     * @return the field
     */
    public IRuleTargetField getRuleTargetField(int fieldId);
}
