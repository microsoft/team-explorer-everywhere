// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.codemarker;

/**
 * The extension point mechanism allowing plugins to contribute
 * {@link CodeMarkerListener}s.
 */
public interface CodeMarkerListenerProvider {
    public CodeMarkerListener getCodeMarkerListener();
}
