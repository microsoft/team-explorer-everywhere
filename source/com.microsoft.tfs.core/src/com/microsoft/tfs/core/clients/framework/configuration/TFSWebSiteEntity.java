// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration;

/**
 * @since TEE-SDK-10.1
 */
public interface TFSWebSiteEntity {
    public TFSEntity getReferencedResource();

    public interface TFSAbsoluteWebSiteEntity extends TFSWebSiteEntity {
        public String getBaseURL();
    }

    public interface TFSRelativeWebSiteEntity extends TFSWebSiteEntity {
        public String getRelativePath();
    }
}
