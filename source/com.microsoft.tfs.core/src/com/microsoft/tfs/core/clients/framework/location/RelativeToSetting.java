// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import java.text.MessageFormat;

/**
 * Enumeration indicating what a given {@link ServiceDefinition} is relative to.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class RelativeToSetting {
    /**
     * Indicates that a service definition is relative to the current context.
     * The current context refers to the relative path of the collection or
     * application that the ServiceDefinition belongs to. For a
     * ServiceDefinition have a RelativeToSetting of Context its RelativePath
     * property must not null or empty.
     */
    public static final RelativeToSetting CONTEXT = new RelativeToSetting(0, "Context"); //$NON-NLS-1$

    /**
     * Indicates that a service definition is relative to the authority of the
     * access point. For a ServiceDefinition to have a RelativeToSetting of
     * Authority its RelativePath property must not be null or empty.
     */
    public static final RelativeToSetting AUTHORITY = new RelativeToSetting(1, "Authority"); //$NON-NLS-1$

    /**
     * Indicates that a ServiceDefinition is relative to the web application.
     * For a ServiceDefinition to have a RelativeToSetting of WebApplication its
     * RelativePath property must not be null or empty.
     */
    public static final RelativeToSetting WEB_APPLICATION = new RelativeToSetting(2, "WebApplication"); //$NON-NLS-1$

    /**
     * Indicates that a ServiceDefinition isn't relative to anything. For a
     * ServiceDefinition to have a RelativeToSetting of FullyQualified its
     * RelativePath property must be null.
     */
    public static final RelativeToSetting FULLY_QUALIFIED = new RelativeToSetting(3, "FullyQualified"); //$NON-NLS-1$

    /**
     * Indicates that a ServiceDefinition is relative to the authority and port
     * of the access point. For a ServiceDefinition to have a RelativeToSetting
     * of Port its RelativePath property must not be null or empty.
     */
    public static final RelativeToSetting PORT = new RelativeToSetting(4, "Port"); //$NON-NLS-1$

    /**
     * All enumeration values in order.
     */
    private static RelativeToSetting[] settings = new RelativeToSetting[] {
        CONTEXT,
        AUTHORITY,
        WEB_APPLICATION,
        FULLY_QUALIFIED,
        PORT,
    };

    private final int value;
    private final String name;

    private RelativeToSetting(final int value, final String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public String toString() {
        return MessageFormat.format("RelativeToSetting [name={0}, value={1}]", name, value); //$NON-NLS-1$
    }

    public int toInt() {
        return value;
    }

    public static RelativeToSetting intToRelativeToSetting(final int value) {
        if (value >= 0 && value <= 4) {
            return settings[value];
        } else {
            throw new IllegalArgumentException("value"); //$NON-NLS-1$
        }
    }
}
