// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import java.text.MessageFormat;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Represents the type of organization that a given security namespace uses when
 * storing its tokens.
 */
public class SecurityNamespaceStructure extends TypesafeEnum {
    /**
     * Indicates that tokens have no relationship to each other in the security
     * namespace.
     */
    public final static SecurityNamespaceStructure FLAT = new SecurityNamespaceStructure(0);

    /**
     * Indicates that tokens are organized in a hierarchical structure
     */
    public final static SecurityNamespaceStructure HIERARCHICHAL = new SecurityNamespaceStructure(1);

    private SecurityNamespaceStructure(final int value) {
        super(value);
    }

    public static SecurityNamespaceStructure fromInteger(final int value) {
        if (value == FLAT.getValue()) {
            return FLAT;
        } else if (value == HIERARCHICHAL.getValue()) {
            return HIERARCHICHAL;
        }

        throw new RuntimeException(
            MessageFormat.format("Unknown SecurityNamespaceStructure value {0}", Integer.toString(value))); //$NON-NLS-1$
    }
}