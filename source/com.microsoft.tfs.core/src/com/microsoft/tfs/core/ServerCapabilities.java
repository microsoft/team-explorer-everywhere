// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import com.microsoft.tfs.util.BitField;

public class ServerCapabilities extends BitField {
    /**
     *
     */
    private static final long serialVersionUID = 5893864270006606463L;

    public static final ServerCapabilities NONE = new ServerCapabilities(0);

    public static final ServerCapabilities HOSTED = new ServerCapabilities(1);
    public static final ServerCapabilities E_MAIL = new ServerCapabilities(2);

    public static final ServerCapabilities ALL = HOSTED.combine(E_MAIL);

    public ServerCapabilities(final int flags) {
        super(flags);
    }

    public boolean contains(final ServerCapabilities other) {
        return containsInternal(other);
    }

    public boolean containsAny(final ServerCapabilities other) {
        return containsAnyInternal(other);
    }

    public ServerCapabilities combine(final ServerCapabilities other) {
        return new ServerCapabilities(combineInternal(other));
    }
}
