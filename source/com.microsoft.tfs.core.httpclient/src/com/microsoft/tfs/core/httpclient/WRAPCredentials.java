// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

public class WRAPCredentials extends Credentials {
    private final String accessToken;

    public WRAPCredentials(final String accessToken) {
        if (accessToken == null) {
            throw new IllegalArgumentException("Access token may not be null");
        }

        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "(OAuth WRAP Credentials)";
    }

    @Override
    public int hashCode() {
        return accessToken.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof WRAPCredentials)) {
            return false;
        }

        final WRAPCredentials other = (WRAPCredentials) o;

        return this.accessToken.equals(other.accessToken);
    }
}
