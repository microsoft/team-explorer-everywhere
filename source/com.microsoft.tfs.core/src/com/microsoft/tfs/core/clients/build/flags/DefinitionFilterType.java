// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

/**
 * Type safe enum representing type type of build definition filter (spec or uri
 * based).
 *
 * @since TEE-SDK-10.1
 */
public class DefinitionFilterType {

    public static final DefinitionFilterType DEFINITION_SPEC = new DefinitionFilterType(0);
    public static final DefinitionFilterType DEFINITION_URIS = new DefinitionFilterType(1);

    private final int value;

    /**
     * Create a new instance of ScheduleType (private constructor)
     */
    private DefinitionFilterType(final int value) {
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
