// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

public class DefaultNTCredentials extends Credentials {
    @Override
    public String toString() {
        return "(Default Credentials)";
    }

    @Override
    public int hashCode() {
        return 0xed15ed15;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        return (o instanceof DefaultNTCredentials);
    }
}
