// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.revision;

import java.util.Date;

/**
 * Represents a work item revision state.
 *
 * @since TEE-SDK-10.1
 */
public interface Revision {
    public RevisionField[] getFields();

    public String getTagLine();

    public RevisionField getField(int id);

    public RevisionField getField(String referenceName);

    public Date getRevisionDate();
}
