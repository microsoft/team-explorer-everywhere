// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.license;

public interface LicenseListener {
    public void eulaAcceptanceChanged(boolean eulaAccepted);
}
