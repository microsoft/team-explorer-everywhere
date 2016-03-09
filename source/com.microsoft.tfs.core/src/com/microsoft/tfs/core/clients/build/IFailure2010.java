// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

/**
 * Represents a failure that occurred during a server call.
 *
 * @since TEE-SDK-10.1
 */
public interface IFailure2010 {
    /**
     * The code associated with this failure - typically the type of the
     * exception this Failure represents.
     */
    public String getCode();

    /**
     * The message associated with this failure - typically the message of the
     * exception this Failure represents.
     */
    public String getMessage();

}
