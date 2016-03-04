// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.operations.model;

import java.util.UUID;

/**
 *
 *
 * @threadsafety unknown
 */
public class OperationReference {
    private UUID id;
    private OperationStatus status;
    private String url;

    /**
     * The identifier for this operation.
     */
    public UUID getId() {
        return id;
    }

    /**
     * The identifier for this operation.
     */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
     * The current status of the operation.
     */
    public OperationStatus getStatus() {
        return status;
    }

    /**
     * The current status of the operation.
     */
    public void setStatus(final OperationStatus status) {
        this.status = status;
    }

    /**
     * URL to get the full object.
     */
    public String getUrl() {
        return url;
    }

    /**
     * URL to get the full object.
     */
    public void setUrl(final String url) {
        this.url = url;
    }
}
