// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account.model;

import java.util.Date;
import java.util.UUID;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public class Profile {

    private CoreAttributes coreAttributes;
    private int coreRevision;
    private Date timeStamp;
    private UUID id;
    private int revision;

    public CoreAttributes getCoreAttributes() {
        return coreAttributes;
    }

    public void setCoreAttributes(final CoreAttributes coreAttributes) {
        this.coreAttributes = coreAttributes;
    }

    public int getCoreRevision() {
        return coreRevision;
    }

    public void setCoreRevision(final int coreRevision) {
        this.coreRevision = coreRevision;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(final Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(final int revision) {
        this.revision = revision;
    }
}
