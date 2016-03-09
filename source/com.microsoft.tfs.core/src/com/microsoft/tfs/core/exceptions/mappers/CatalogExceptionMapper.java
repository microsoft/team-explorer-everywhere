// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions.mappers;

/**
 * Mapper for TFS catalog service exceptions.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class CatalogExceptionMapper extends TECoreExceptionMapper {
    public static RuntimeException map(final RuntimeException e) {
        return TECoreExceptionMapper.map(e);
    }
}
