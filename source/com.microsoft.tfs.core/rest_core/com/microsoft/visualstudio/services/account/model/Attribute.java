// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account.model;

import java.util.Date;

public class Attribute {
    private Descriptor descriptor;
    private String value;
    private Date timeStamp;
    private int revision;

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(final Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(final Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(final int revision) {
        this.revision = revision;
    }
}
