// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.revision;

/**
 * Represents a change to one work item field in a {@link Revision}.
 *
 * @since TEE-SDK-10.1
 */
public interface RevisionField {
    public Object getOriginalValue();

    public Object getValue();

    public String getName();

    public String getReferenceName();

    public int getID();

    public boolean shouldIgnoreForDeltaTable();
}
