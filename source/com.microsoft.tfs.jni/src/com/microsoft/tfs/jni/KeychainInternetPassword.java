// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 *
 *
 * @threadsafety unknown
 */
public class KeychainInternetPassword {
    private String serverName;
    private String id;
    private String accountName;
    private String path;
    private int port;
    private KeychainProtocol protocol;
    private KeychainAuthenticationType authenticationType;
    private byte[] password;
    private String label;
    private String comment;

    public KeychainInternetPassword() {
    }

    /**
     * Gets the server name.
     *
     * @return the server name (may be <code>null</code>)
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the server name.
     *
     * @param serverName
     *        the server name (or <code>null</code>)
     */
    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    /**
     * Gets the unique id (also called the "security domain")
     *
     * @return the unique id (may be <code>null</code>)
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the unique id (also called the "security domain")
     *
     * @param id
     *        the unique id (or <code>null</code>)
     */
    public void setID(final String id) {
        this.id = id;
    }

    /**
     * Gets the account name used for authentication.
     *
     * @return the account name (may be <code>null</code>)
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Sets the account name used for authentication.
     *
     * @param accountName
     *        the account name (or <code>null</code>)
     */
    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    /**
     * Gets the path to the authenticated resource
     *
     * @return the path (may be <code>null</code>)
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path to the authenticated resource
     *
     * @param path
     *        the path (or <code>null</code>)
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Gets the port to the authenticated resource
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port to the authenticated resource
     *
     * @param port
     *        the port
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Gets the protocol for the authenticated resource
     *
     * @return the protocol (may be <code>null</code>)
     */
    public KeychainProtocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol for the authenticated resource
     *
     * @param protocol
     *        the protocol (or <code>null</code>)
     */
    public void setProtocol(final KeychainProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the authentication type
     *
     * @return the authentication type (may be <code>null</code>)
     */
    public KeychainAuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    /**
     * Sets the authentication type
     *
     * @param authenticationType
     *        the authentication type (or <code>null</code>)
     */
    public void setAuthenticationType(final KeychainAuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    /**
     * Gets the password for authentication
     *
     * @return the password in plaintext (may be <code>null</code>)
     */
    public byte[] getPassword() {
        return password;
    }

    /**
     * Sets the password for authentication
     *
     * @param password
     *        the password in plaintext (or <code>null</code>)
     */
    public void setPassword(final byte[] password) {
        this.password = password;
    }

    /**
     * Gets the label used to name this keychain entry
     *
     * @return the label (may be <code>null</code>)
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label used to name this keychain entry
     *
     * @param label
     *        the label (or <code>null</code>)
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Gets the comment for this keychain entry
     *
     * @return the comment (may be <code>null</code>)
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for this keychain entry
     *
     * @param comment
     *        the comment (or <code>null</code>)
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }
}
