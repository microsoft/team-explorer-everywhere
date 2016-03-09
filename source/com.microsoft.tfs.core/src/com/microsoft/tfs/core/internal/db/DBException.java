// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import com.microsoft.tfs.core.exceptions.TECoreException;

public class DBException extends TECoreException {
    public DBException(final Throwable t, final String sql) {
        super((t != null ? t.toString() : "") + " sql [" + sql + "]", t); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public DBException(final Throwable t) {
        super(t);
    }

    public DBException(final String s) {
        super(s);
    }

    public DBException(final String s, final Throwable t) {
        super(s, t);
    }
}
