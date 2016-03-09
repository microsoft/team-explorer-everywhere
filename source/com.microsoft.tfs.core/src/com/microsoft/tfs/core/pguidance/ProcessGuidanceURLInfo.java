// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pguidance;

/**
 * Encapsulates a URL string and validity information about a process guidance
 * service resource as validated by {@link IProcessGuidance}.
 *
 * @since TEE-SDK-10.1
 */
public interface ProcessGuidanceURLInfo {
    public boolean isValid();

    public String getURL();

    public String getInvalidMessage();
}
