// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class FindInSourceControlQuery {
    private final String serverPath;
    private final String wildcard;
    private boolean recursive;
    private boolean checkedOut;
    private boolean showStatus;
    private String checkedOutUser;

    private final String RECURSIVE_OPTION_NAME = "isRecursive"; //$NON-NLS-1$
    private final String CHECKEDOUT_OPTION_NAME = "isCheckout"; //$NON-NLS-1$
    private final String CHECKOUT_USER_OPTION_NAME = "isChechoutByUser"; //$NON-NLS-1$
    private final String SHOW_STATUS_OPTION_NAME = "showStatus"; //$NON-NLS-1$

    /**
     * Constructs a basic source control query for the root server path with no
     * wildcards for files in any checkout state.
     */
    public FindInSourceControlQuery() {
        this(ServerPath.ROOT, null, true, false, false, null);
    }

    /**
     * Constructs a source control query for the given wildcard beneath the
     * given server path (at any checkout state.)
     *
     * @param serverPath
     *        The server path to query
     * @param wildcard
     *        The wildcard to query for (or <code>null</code> for all files)
     */
    public FindInSourceControlQuery(final String serverPath, final String wildcard) {
        this(serverPath, wildcard, true, false, false, null);
    }

    /**
     * Constructs a source control query for the given wildcard beneath the
     * given server path, optionally matching only files that are checked out,
     * optionally to a given user.
     *
     * @param serverPath
     *        The server path to query
     * @param wildcard
     *        The wildcard to query for (or <code>null</code> for all files)
     * @param checkedOut
     *        <code>true</code> to only match items that are checked out,
     *        <code>false</code> to match all items
     * @param checkedOutUser
     *        The user to display checked out items for (or <code>null</code>
     *        for all users). This parameter has no effect if {@link checkedOut}
     *        is false.
     */
    public FindInSourceControlQuery(
        final String serverPath,
        final String wildcard,
        final boolean recursive,
        final boolean checkedOut,
        final boolean showStatus,
        final String checkedOutUser) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        this.serverPath = serverPath;
        this.wildcard = wildcard;
        this.recursive = recursive;
        this.checkedOut = checkedOut;
        this.checkedOutUser = checkedOutUser;
        this.showStatus = showStatus;
    }

    /**
     * @return The server path to query on (never <code>null</code>)
     */
    public String getServerPath() {
        return serverPath;
    }

    /**
     * @return The wildcard to filter items by or <code>null</code> for no
     *         wildcard
     */
    public String getWildcard() {
        return wildcard;
    }

    /**
     * @return <code>true</code> if the query applies to the server path
     *         recursively, <code>false</code> otherwise
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * @return <code>true</code> to filter by checked out files,
     *         <code>false</code> to include all files
     */
    public boolean isCheckedOut() {
        return checkedOut;
    }

    /**
     * @return <code>true</code> to retrieve status information ,
     *         <code>false</code> not to retrieve status information
     */
    public boolean showStatus() {
        return showStatus;
    }

    /**
     * @return The user (in DOMAIN\\username format) to filter checked out files
     *         by or <code>null</code> to show all checked out files
     */
    public String getCheckedOutUser() {
        return checkedOutUser;
    }

    public void loadFromMemento(final Memento memento) {
        if (memento == null) {
            return;
        }

        /*
         * Read all the options.
         */
        final Memento[] options = memento.getChildren(FindInSCEQueryOptionsPersistence.OPTION_MEMENTO_NAME);

        // no preferences saved before
        if (options == null) {
            return;
        }

        for (final Memento option : options) {
            final String optionName = option.getString(FindInSCEQueryOptionsPersistence.OPTION_NAME);
            if (!StringUtil.isNullOrEmpty(optionName)) {
                if (optionName.equals(RECURSIVE_OPTION_NAME)) {
                    final boolean value = option.getBoolean(FindInSCEQueryOptionsPersistence.VALUE_NAME).booleanValue();
                    recursive = value;
                } else if (optionName.equals(CHECKEDOUT_OPTION_NAME)) {
                    final boolean value = option.getBoolean(FindInSCEQueryOptionsPersistence.VALUE_NAME).booleanValue();
                    checkedOut = value;
                } else if (optionName.equals(CHECKOUT_USER_OPTION_NAME)) {
                    final String value = option.getString(FindInSCEQueryOptionsPersistence.VALUE_NAME);
                    checkedOutUser = StringUtil.isNullOrEmpty(value) ? null : value;
                } else if (optionName.equals(SHOW_STATUS_OPTION_NAME)) {
                    final boolean value = option.getBoolean(FindInSCEQueryOptionsPersistence.VALUE_NAME).booleanValue();
                    showStatus = value;
                }
            }
        }

    }

    public void saveToMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        /*
         * Write all the options.
         */

        Memento child = memento.createChild(FindInSCEQueryOptionsPersistence.OPTION_MEMENTO_NAME);
        child.putString(FindInSCEQueryOptionsPersistence.OPTION_NAME, RECURSIVE_OPTION_NAME);
        child.putBoolean(FindInSCEQueryOptionsPersistence.VALUE_NAME, isRecursive());

        child = memento.createChild(FindInSCEQueryOptionsPersistence.OPTION_MEMENTO_NAME);
        child.putString(FindInSCEQueryOptionsPersistence.OPTION_NAME, CHECKEDOUT_OPTION_NAME);
        child.putBoolean(FindInSCEQueryOptionsPersistence.VALUE_NAME, isCheckedOut());

        child = memento.createChild(FindInSCEQueryOptionsPersistence.OPTION_MEMENTO_NAME);
        child.putString(FindInSCEQueryOptionsPersistence.OPTION_NAME, CHECKOUT_USER_OPTION_NAME);
        child.putString(
            FindInSCEQueryOptionsPersistence.VALUE_NAME,
            getCheckedOutUser() == null ? "" : getCheckedOutUser()); //$NON-NLS-1$

        child = memento.createChild(FindInSCEQueryOptionsPersistence.OPTION_MEMENTO_NAME);
        child.putString(FindInSCEQueryOptionsPersistence.OPTION_NAME, SHOW_STATUS_OPTION_NAME);
        child.putBoolean(FindInSCEQueryOptionsPersistence.VALUE_NAME, showStatus());

    }

}
