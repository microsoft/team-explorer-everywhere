// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

public class Direction {
    public static final Direction UNKNOWN = new Direction(0);
    public static final Direction ASCENDING = new Direction(1);
    public static final Direction DESCENDING = new Direction(2);

    private final int type;

    private Direction(final int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.valueOf(type);
    }

    public int getValue() {
        return type;
    }
}
