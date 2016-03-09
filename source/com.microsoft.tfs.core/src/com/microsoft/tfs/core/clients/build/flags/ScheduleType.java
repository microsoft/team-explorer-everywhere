// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

/**
 * Describes the schedule type.
 *
 * @since TEE-SDK-10.1
 */
public class ScheduleType {

    /**
     * A weekly schedule type.
     */
    public static final ScheduleType WEEKLY = new ScheduleType(1);

    private final int value;

    /**
     * Create a new instance of ScheduleType (private constructor)
     */
    private ScheduleType(final int value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other instanceof ScheduleType) {
            return value == ((ScheduleType) other).getValue();
        }
        return false;
    }

}
