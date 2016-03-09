// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

public class WinCredential {
    private String serverUri;
    private String accountName;
    private String password;

    public WinCredential() {
    }

    /**
     * @param serverUri
     * @param accountName
     * @param password
     */
    public WinCredential(final String serverUri, final String accountName, final String password) {
        this.serverUri = serverUri;
        this.accountName = accountName;
        this.password = password;
    }

    /**
     * @return
     */
    public String getServerUri() {
        return serverUri;
    }

    /**
     * @param serverUri
     */
    public void setServerUri(final String serverUri) {
        this.serverUri = serverUri;
    }

    /**
     * @return
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * @param accountName
     */
    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    /**
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     */
    public void setPassword(final String password) {
        this.password = password;
    }
}
